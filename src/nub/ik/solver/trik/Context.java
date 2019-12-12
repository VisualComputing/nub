package nub.ik.solver.trik;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.geometric.FABRIKSolver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.heuristic.Heuristic;
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
    //This structures allows to find the world position /orientation of a Node using the sufficient operations
    protected List<NodeInformation> _chainInformation, _usableChainInformation; //Keep position / orientation information
    protected Node _target, _worldTarget, _previousTarget; //Target to reach

    protected boolean _direction = false;

    protected boolean _enableWeight, _explore;
    protected int _lockTimes = 0, _lockTimesCriteria = 4;

    //Error attributes
    protected float _maxLength = 0, _avgLength = 0;


    protected boolean _debug = false;
    protected Random _random = new Random(0);
    protected float _weightRatio = 3f, _weightRatioNear = 1.2f; //how many times is position more important than orientation
    protected float _weightThreshold = 0.1f; //change error measurement when the chain is near the target
    protected int _last = -1;

    protected boolean _singleStep = false;

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
}
