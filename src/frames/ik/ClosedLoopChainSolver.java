package frames.ik;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 30/07/18.
 */

public class ClosedLoopChainSolver extends FABRIKSolver {

    protected ArrayList<? extends Frame> _chain;
    protected ArrayList<Frame> _reversedChain;
    protected ArrayList<? extends Frame> _original;
    protected float _distanceHeadToTail;
    protected ArrayList<Vector> _childrenPositions = new ArrayList<Vector>();
    protected ArrayList<Float> _childrenDistances= new ArrayList<Float>();
    protected boolean _hasChanged = false;

    //here we don't consider any kind of constraint
    //chain must not include the head twice
    public ClosedLoopChainSolver(ArrayList<? extends Frame> chain) {
        super();
        this._original = chain;
        //this._chain = _copy(chain);
        this._chain = chain;
        _positions = new ArrayList<Vector>();
        _childrenPositions = new ArrayList<Vector>();
        _childrenDistances = new ArrayList<Float>();
        _distances = new ArrayList<Float>();
        _orientations = new ArrayList<Quaternion>();
        Vector prevPosition = chain.get(0).reference() != null
                ? chain.get(0).reference().position().get() : new Vector(0, 0, 0);
        Quaternion prevOrientation = chain.get(0).reference() != null
                ? chain.get(0).reference().orientation().get() : new Quaternion();
        _reversedChain = new ArrayList<>();
        for (Frame joint : chain) {
            _properties.put(joint, new Properties());
            _reversedChain.add(_reversedChain.size(), joint);
            Vector position = joint.position().get();
            Quaternion orientation = prevOrientation.get();
            orientation.compose(joint.rotation().get());
            _positions.add(position);
            _childrenPositions.add(position.get());
            _childrenDistances.add(0.f);
            _distances.add(Vector.subtract(position, prevPosition).magnitude());
            _orientations.add(orientation);

            //Vector positionChild = joint.children() != null ?
            //        joint.children().get(0).position() : position.get();
            //_childrenPositions.add(positionChild);
            //_distancesChildren.add(Vector.subtract(position, positionChild).magnitude());
            prevPosition = position;
            prevOrientation = orientation.get();
        }

        Vector head = _chain.get(0).position().get();
        Vector tail = _chain.get(_chain.size()-1).position().get();
        _distanceHeadToTail = Vector.subtract(head, tail).magnitude();
    }

    public ArrayList<? extends Frame> chain() {
        return _original;
    }

    public void setUnknown(Frame frame, Vector vector){
        for(int i = 0; i < _original.size(); i++){
            if(frame == _original.get(i)){
                _positions.set(i, vector);
            }
        }
    }

    public void setUnknown(int i, Vector vector){
        _positions.set(i, vector);
        _hasChanged = true;
    }

    public void setChildPosition(int i, Vector vector){
        _childrenPositions.set(i, vector);
    }

    public void setChildDistance(int i, Float distance){
        _childrenDistances.set(i, distance);
    }


    public float fixNoisyJoints(){
        //consider the case when there're 2 unknowns
        //Initial root position
        float change = 0;
        ArrayList<Vector> prev = new ArrayList<Vector>();
        for(Vector v : _positions){
            prev.add(v.get());
        }

        Vector initial = _chain.get(0).position().get();
        //Stage 1
        //step 1
        _positions.set(1, _move(_positions.get(0), _positions.get(1), _distances.get(1)));
        //step 2
        _positions.set(2, _move(_positions.get(1), _positions.get(2), _distances.get(2)));
        //Move root
        //step 3
        _positions.set(0, _move(_positions.get(_positions.size()-1), _positions.get(0), _distanceHeadToTail));
        //step 4
        _positions.set(1, _move(_positions.get(0), _positions.get(1), _distances.get(1)));
        //Stage 2
        //set root to initial position
        _positions.set(0 , initial);
        //Move in the other order
        //step 5
        _positions.set(2, _move(_positions.get(0), _positions.get(2), _distanceHeadToTail));
        //step 6
        _positions.set(1, _move(_positions.get(2), _positions.get(1), _distances.get(2)));


        //Steps 7 - 8 - 9 - 10
        //keep distances with children
        for(int i = 1; i < _chain.size(); i++){
            float dist = _distances.get(i);
            if(i == 2) dist = _distanceHeadToTail;
            _positions.set(i, _move(_positions.get(0), _positions.get(i), dist));
            _positions.set(i,_move(_childrenPositions.get(i), _positions.get(i), _childrenDistances.get(i)));
        }

        //step 11
        _positions.set(1, _move(_positions.get(0), _positions.get(1), _distances.get(1)));
        //step 12
        _positions.set(2, _move(_positions.get(1), _positions.get(2), _distances.get(2)));
        //Move root
        //step 13
        _positions.set(0, _move(_positions.get(_positions.size()-1), _positions.get(0), _distanceHeadToTail));
        //step 14
        _positions.set(1, _move(_positions.get(0), _positions.get(1), _distances.get(1)));
        //set root to initial position
        _positions.set(0 , initial);
        //Move in the other order
        //step 15
        _positions.set(2, _move(_positions.get(0), _positions.get(2), _distanceHeadToTail));
        //step 16
        _positions.set(1, _move(_positions.get(2), _positions.get(1), _distances.get(2)));


        //Move in the other order
        for(int i = 0; i < _positions.size(); i++){
            change += Vector.distance(prev.get(i), _positions.get(i));
        }
        return change;
    }


    @Override
    protected boolean _iterate() {
        //Execute Until the distance between the end effector and the target is below a threshold
        float error = 0;
        for(int i = 0; i < _chain.size(); i++){
            error += Vector.distance(_chain.get(i).position(), _positions.get(i));
        }
        if (error <= this.error){
            return true;
        }

        float change = fixNoisyJoints();
        //Check total position change
        if (change <= minDistance){
            return true;
        }
        return false;
    }

    @Override
    protected void _update() {
        //_backwardReaching(_chain, _original.get(0).position());
        for(int i = 0; i < _original.size() - 1; i++){
            Vector v = _original.get(i).location(positions().get(i));
            _original.get(i).rotate(new Quaternion(_original.get(i+1).translation(), v));
            //_original.get(i).setRotation(_chain.get(i).rotation().get());
            //_original.get(i).setPosition(_positions.get(i));
        }
    }

    @Override
    protected boolean _changed() {
        return _hasChanged;
    }

    @Override
    protected void _reset() {
        iterations = 0;
        _hasChanged = false;
    }
}
