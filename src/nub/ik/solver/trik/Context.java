package nub.ik.solver.trik;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.FABRIKSolver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.List;
import java.util.Random;

public class Context {
    /**
     * A TRIK solver is a set of Heuristics that works on the same Context in order to solve the IK
     * problem on a kinematic chain.
     *
     * A Context contains the sufficient information that a heuristic must use in order to find
     * a new chain configuration.
     * */

    /**
     * Chain is the kinematic chain to modify. Since we expect to return at each iteration a better
     * solution that the previously found, this solver must be steady state. Hence, we perform any action
     * on a usableChain (a copy of the original chain) and only when the new configuration results in a
     * better solution we modify the kinematic chain.
     *
    * */
    protected List<? extends Node> _chain; //the kinematic chain to modify
    protected List<Node> _usableChain; //a copy of the kinematic chain.


    protected boolean _enableDelegation = false;
    protected float[] _delegationAtJoint;
    protected float _delegationFactor = 5f; //current joint will work at least "delegation factor" times the remaining ones

    protected int _deadlockCounter = 0;

    //This structures allows to find the world position /orientation of a Node using the sufficient operations
    protected List<NodeInformation> _chainInformation, _usableChainInformation; //Keep position / orientation information
    protected Node _target, _worldTarget, _previousTarget; //Target to reach

    protected boolean _direction = false;
    protected boolean _topToBottom = true;

    protected boolean _enableWeight, _explore;
    protected int _lockTimes = 0, _lockTimesCriteria = 4;

    //Error attributes
    protected float _maxLength = 0, _avgLength = 0;
    protected Solver _solver;


    protected boolean _debug = false;
    protected float _weightRatio = 3f, _weightRatioNear = 1.2f; //how many times is position more important than orientation
    protected float _weightThreshold = 0.1f; //change error measurement when the chain is near the target
    protected int _last = -1;

    protected boolean _singleStep = false;

    protected float _positionWeight;

    public float positionWeight(){
        return _positionWeight;
    }

    public void setPositionWeight(float positionWeight){
        _positionWeight = positionWeight;
    }

    public void setDirection(boolean direction){
        _direction = direction;
    }


    public int deadlockCounter(){
        return _deadlockCounter;
    }

    public void incrementDeadlockCounter(){
        _deadlockCounter++;
    }

    public void resetDeadlockCounter(){
        _deadlockCounter = 0;
    }

    public Context(List<? extends Node> chain, Node target){
        this(chain, target, false);
    }

    public Context(List<? extends Node> chain, Node target, boolean debug){
        this._chain = chain;
        this._debug = debug;
        if(_debug && _chain.get(0).graph() instanceof Scene) {
            this._usableChain = FABRIKSolver._copy(chain, null, (Scene) _chain.get(0).graph());
        }
        else {
            this._usableChain = FABRIKSolver._copy(chain);
        }

        //create info list
        _chainInformation = NodeInformation._createInformationList(_chain, true);
        _usableChainInformation = NodeInformation._createInformationList(_usableChain, true);
        this._last = _chain.size() - 1;

        this._target = target;
        this._previousTarget =
                target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);

        this._worldTarget = target == null ? new Node() : new Node(_target.position(), _target.orientation(), 1);
        this._last = _chain.size() - 1;
        _delegationAtJoint = new float[chain.size() - 1];
    }

    public void setSolver(Solver solver){
        _solver = solver;
    }

    public Solver solver(){
        return _solver;
    }
    //Getters and setters
    public List<? extends Node> chain(){
        return _chain;
    }

    public List<Node> usableChain(){
        return _usableChain;
    }

    public List<NodeInformation> chainInformation(){
        return _chainInformation;
    }

    public List<NodeInformation> usableChainInformation(){
        return _usableChainInformation;
    }

    public int last(){
        return _last;
    }

    public boolean direction(){
        return _direction;
    }

    public Node target(){
        return _target;
    }
    public Node worldTarget(){
        return _worldTarget;
    }


    public boolean debug(){
        return _debug;
    }

    public void setDebug(boolean debug){
        _debug = debug;
    }

    public boolean enableWeight(){
        return _enableWeight;
    }

    public boolean singleStep(){ //TODO : REMOVE!
        return _singleStep;
    }

    public void setSingleStep(boolean singleStep){
        _singleStep = singleStep;
    }

    public NodeInformation endEffectorInformation(){
        return _usableChainInformation.get(_last);
    }

    public float weightRatio(){
        return _weightRatio;
    }

    public Node previousTarget(){
        return _previousTarget;
    }

    public void setPreviousTarget(Node previousTarget){
        _previousTarget = previousTarget;
    }

    public float avgLength(){
        return _avgLength;
    }

    public float maxLength(){
        return _maxLength;
    }

    public void setMaxLength(float maxLength){
        _maxLength = maxLength;
    }

    public void setAvgLength(float avgLength){
        _avgLength = avgLength;
    }

    public void setTarget(Node target){
        _target = target;
    }

    public void copyChainState(List<NodeInformation> origin, List<NodeInformation> dest){
        //Copy the content of the origin chain into dest
        Node refDest = dest.get(0).node().reference();
        if(refDest != null){
            Constraint constraint = refDest.constraint();
            refDest.setConstraint(null);
            refDest.set(origin.get(0).node().reference());
            refDest.setConstraint(constraint);
        }

        for(int i = 0; i < origin.size(); i++) {
            Node node = origin.get(i).node();
            Constraint constraint = dest.get(i).node().constraint();
            Quaternion rotation = node.rotation().get();
            Vector translation = node.translation().get();

            dest.get(i).node().setConstraint(null);
            dest.get(i).node().setRotation(rotation);
            dest.get(i).node().setTranslation(translation);
            dest.get(i).node().setScaling(node.scaling());
            dest.get(i).node().setConstraint(constraint);
        }
    }

    public int _currentIteration(){
        return _solver.iteration();
    }

    public float delegationAtJoint(int i){
        return _delegationAtJoint[i];
    }

    public void setDelegationAtJoint(int i, float value){
        _delegationAtJoint[i] = value;
    }

    //TODO : Define various default ways for delegation distribution
    public void setKDistributionDelegation(float k){//work done by current joint is k times greater than done by remaining ones
        for(int i = 0; i < chain().size() - 1; i++){
            int n = chain().size() - 1 - i;
            int idx = i; //int idx = _topToBottom ? i : chain().size() - 2 - i;
            _delegationAtJoint[idx] = k / (k + n - 1);
        }
    }


    public void setDelegationFactor(float k){
        _enableDelegation = true;
        _delegationFactor = k;
        setKDistributionDelegation(_delegationFactor);
    }

    public void enableDelegation(boolean enableDelegation){
        _enableDelegation = enableDelegation;
    }

    public boolean enableDelegation(){
        return _enableDelegation;
    }

    public void setTopToBottom(boolean topToBottom){
        if(_topToBottom != topToBottom) {
            _topToBottom = topToBottom;
            //swap delegation per joint
            //_swapDelegationPerJoint();
        }
    }

    public void _swapDelegationPerJoint(){
        System.out.println("swap!!");
        int last  = _delegationAtJoint.length - 1;
        for(int i =0; i <= last; i++){
            System.out.print(_delegationAtJoint[i] + " , ");
        }
        System.out.println();
        for(int i =0; i <= last / 2 ; i++){
            float aux = _delegationAtJoint[i];
            _delegationAtJoint[i] = _delegationAtJoint[last - i];
            _delegationAtJoint[last - i] = aux;
        }
        for(int i =0; i <= last; i++){
            System.out.print(_delegationAtJoint[i] + " , ");
        }
        System.out.println();
    }


    public boolean topToBottom(){
        return  _topToBottom;
    }


    /*Error measures*/
    public static float positionError(Vector eff, Vector target){
        return Vector.distance(eff, target);
    }

    public static float positionError(NodeInformation eff, Node target){
        return positionError(eff.positionCache(), target.position());
    }

    public static float orientationError(Quaternion eff, Quaternion target, boolean angles){
        float s1 = 1, s2 = 1;
        if(eff.w() < 0) s1 = -1;
        if(target.w() < 0) s2 = - 1;
        float dot = s1 * eff._quaternion[0] * s2 * target._quaternion[0] +
                s1 * eff._quaternion[1] * s2 * target._quaternion[1] +
                s1 * eff._quaternion[2] * s2 * target._quaternion[2] +
                s1 * eff._quaternion[3] * s2 * target._quaternion[3];

        //clamp dot product
        dot = Math.min(Math.max(dot, -1), 1);
        if(angles) return (float) Math.toDegrees(Math.acos(Math.min(Math.max(2 * dot * dot - 1, -1), 1)));
        return  (float) (1 - dot * dot);
    }


    public static float quaternionDistance(Quaternion a, Quaternion b){
        float s1 = 1, s2 = 1;
        if(a.w() < 0) s1 = -1;
        if(b.w() < 0) s2 = - 1;
        float dot = s1 * a._quaternion[0] * s2 * b._quaternion[0] + s1 * a._quaternion[1] * s2 * b._quaternion[1] + s1 * a._quaternion[2] * s2 * b._quaternion[2] + s1 * a._quaternion[3] * s2 * b._quaternion[3];
        return (float) (1 - Math.pow(dot, 2));
    }

    public float error(NodeInformation eff, Node target, float w1, float w2){
        return error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation(), w1, w2);
    }

    public float error(NodeInformation eff, Node target){
        return error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation(), 1, 1);
    }

    public float error(Vector effPosition, Vector targetPosition, Quaternion effRotation, Quaternion targetRotation, float w1, float w2){
        float error = positionError(effPosition, targetPosition);
        if(_direction){
            float orientationError = orientationError(effRotation, targetRotation, false);
            float weighted_error = error / _positionWeight;
            float c_k = (float) Math.floor(weighted_error);
            error =  c_k + 0.2f * orientationError + 0.8f * (weighted_error - c_k);
        }
        return error;
    }



}
