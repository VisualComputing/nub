package frames.ik.jacobian;

import frames.core.Frame;
import frames.ik.Solver;
import frames.primitives.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class TransposeSolver extends Solver{
    //look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/iksurvey.pdf

    protected ArrayList<? extends Frame> _chain;
    protected Frame _target;
    protected Frame _previousTarget;
    protected SimpleMatrix _J;
    protected SimpleMatrix _delta;
    protected Vector[] _axes;
    //protected float _alpha = 0.00001f;
    protected float _d_max;

    //protected List<Statistics> _statistics;
    protected boolean _debug = false;

    public TransposeSolver(ArrayList<? extends Frame> chain){
        super();
        this._chain = chain;
        _axes = new Vector[_chain.size() - 1];
        for(Frame f : _chain){
            float d = Vector.distance(f.position(), f.reference() != null ? f.reference().position() : new Vector(0,0,0));
            _d_max = _d_max < d ? d : _d_max;
        }
    }

    public TransposeSolver(ArrayList<? extends Frame> chain, Frame target) {
        super();
        this._chain = chain;
        this._target = target;
        this._previousTarget =
                target == null ? null : new Frame(target.position().get(), target.orientation().get(), 1);
        _axes = new Vector[_chain.size() - 1];
    }

    /*
    public List<Statistics> statistics(){
        return _statistics;
    }
    */

    public ArrayList<? extends Frame> chain() {
        return _chain;
    }

    public Frame target() {
        return _target;
    }

    public void setTarget(Frame target) {
        this._target = target;
    }

    public Frame head() {
        return _chain.get(0);
    }

    public Frame endEffector() {
        return _chain.get(_chain.size() - 1);
    }

    @Override
    protected boolean _iterate() {
        //As no target is specified there is no need to perform an iteration
        if (_target == null || _chain.size() < 2) return true;
        //Clamp error
        Vector e = Vector.subtract(_target.position() , endEffector().position());
        if(e.magnitude() > _d_max){
            e.normalize();
            e.multiply(_d_max*2);
        }


        SimpleMatrix error = SimpleMatrix.wrap(
                Util.vectorToMatrix(e, head().graph().is3D()));
        _J = SimpleMatrix.wrap( Util.jacobian( _chain, endEffector() , _target.position(), _axes));

        _delta = _J.transpose().mult(error);

        //choosing alpha according to error magnitude
        SimpleMatrix JJTe = _J.mult(_delta);
        double div = JJTe.dot(JJTe);
        if(div > 1E-3)
            _delta = _delta.scale(error.dot(JJTe)/div);
        else
            _delta = _delta.scale(0);

        //Execute Until the distance between the end effector and the target is below a threshold
        if (Vector.distance(endEffector().position(), _target.position()) <= super.error) {
            return true;
        }
        //Check total rotation change
        //if (change <= minDistance) return true;
        return false;
    }

    @Override
    protected void _update() {
        Util.updateChain(_chain, _delta, _axes);
    }


    @Override
    protected boolean _changed() {
        if (_target == null) {
            _previousTarget = null;
            return false;
        } else if (_previousTarget == null) {
            return true;
        }
        return !(_previousTarget.position().matches(_target.position()) && _previousTarget.orientation().matches(_target.orientation()));
    }

    @Override
    protected void _reset() {
        _previousTarget = _target == null ? null : new Frame(_target.position().get(), _target.orientation().get(), 1);
        iterations = 0;
        _axes = new Vector[_chain.size() - 1];
    }

    @Override
    public float error() {
        return Vector.distance(_target.position(), _chain.get(_chain.size()-1).position());
    }
}

