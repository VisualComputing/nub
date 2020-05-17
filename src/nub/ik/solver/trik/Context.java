package nub.ik.solver.trik;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.animation.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

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
   */
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
  //Important parameters for orientation solution
  protected float _searchingAreaRadius = 0.5f;
  protected boolean _radiusRelativeToBoneAverage = true;
  protected float _orientationWeight = 0.2f;

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


  public float searchingAreaRadius() {
    if (_radiusRelativeToBoneAverage) return _searchingAreaRadius * _avgLength;
    return _searchingAreaRadius;
  }

  public void setSearchingAreaRadius(float searchingAreaRadius, boolean relativeToBoneAverage) {
    _radiusRelativeToBoneAverage = relativeToBoneAverage;
    _searchingAreaRadius = searchingAreaRadius;
  }

  public void setSearchingAreaRadius(float searchingAreaRadius) {
    setSearchingAreaRadius(searchingAreaRadius, false);
  }

  public void setRadiusRelativeToBoneAverage(boolean relativeToBoneAverage) {
    _radiusRelativeToBoneAverage = relativeToBoneAverage;
  }

  public void setOrientationWeight(float orientationWeight) {
    _orientationWeight = orientationWeight;
  }

  public float orientationWeight() {
    return _orientationWeight;
  }

  public void setDirection(boolean direction) {
    _direction = direction;
  }


  public int deadlockCounter() {
    return _deadlockCounter;
  }

  public void incrementDeadlockCounter() {
    _deadlockCounter++;
  }

  public void resetDeadlockCounter() {
    _deadlockCounter = 0;
  }

  public Context(List<? extends Node> chain, Node target) {
    this(chain, target, false);
  }

  public Context(List<? extends Node> chain, Node target, boolean debug) {
    this._chain = chain;
    this._debug = debug;
    if (_debug && Graph.isReachable(_chain.get(0))) {
      this._usableChain = _attachedCopy(chain, null);
    } else {
      this._usableChain = _detachedCopy(chain);
    }

    //create info list
    _chainInformation = NodeInformation._createInformationList(_chain, true);
    _usableChainInformation = NodeInformation._createInformationList(_usableChain, true);
    this._last = _chain.size() - 1;

    this._target = target;
    this._previousTarget =
        target == null ? null : Node.detach(target.position().get(), target.orientation().get(), 1);

    this._worldTarget = target == null ? Node.detach(new Vector(), new Quaternion(), 1) : Node.detach(_target.position(), _target.orientation(), 1);
    this._last = _chain.size() - 1;
    _delegationAtJoint = new float[chain.size() - 1];
    update();
  }

  public void setSolver(Solver solver) {
    _solver = solver;
  }

  public Solver solver() {
    return _solver;
  }

  //Getters and setters
  public List<? extends Node> chain() {
    return _chain;
  }

  public List<Node> usableChain() {
    return _usableChain;
  }

  public List<NodeInformation> chainInformation() {
    return _chainInformation;
  }

  public List<NodeInformation> usableChainInformation() {
    return _usableChainInformation;
  }

  public int last() {
    return _last;
  }

  public boolean direction() {
    return _direction;
  }

  public Node target() {
    return _target;
  }

  public Node worldTarget() {
    return _worldTarget;
  }


  public boolean debug() {
    return _debug;
  }

  public void setDebug(boolean debug) {
    _debug = debug;
  }

  public boolean enableWeight() {
    return _enableWeight;
  }

  public boolean singleStep() { //TODO : REMOVE!
    return _singleStep;
  }

  public void setSingleStep(boolean singleStep) {
    _singleStep = singleStep;
  }

  public NodeInformation endEffectorInformation() {
    return _usableChainInformation.get(_last);
  }

  public float weightRatio() {
    return _weightRatio;
  }

  public Node previousTarget() {
    return _previousTarget;
  }

  public void setPreviousTarget(Node previousTarget) {
    _previousTarget = previousTarget;
  }

  public float avgLength() {
    return _avgLength;
  }

  public float maxLength() {
    return _maxLength;
  }

  public void setMaxLength(float maxLength) {
    _maxLength = maxLength;
  }

  public void setAvgLength(float avgLength) {
    _avgLength = avgLength;
  }

  public void setTarget(Node target) {
    _target = target;
  }

  public void copyChainState(List<NodeInformation> origin, List<NodeInformation> dest) {
    //Copy the content of the origin chain into dest
    Node refDest = dest.get(0).node().reference();
    if (refDest != null) {
      Constraint constraint = refDest.constraint();
      refDest.setConstraint(null);
      refDest.set(origin.get(0).node().reference());
      refDest.setConstraint(constraint);
    }

    for (int i = 0; i < origin.size(); i++) {
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

  public int _currentIteration() {
    return _solver.iteration();
  }

  public float delegationAtJoint(int i) {
    return _delegationAtJoint[i];
  }

  public void setDelegationAtJoint(int i, float value) {
    _delegationAtJoint[i] = value;
  }

  //TODO : Define various default ways for delegation distribution
  public void setKDistributionDelegation(float k) {//work done by current joint is k times greater than done by remaining ones
    for (int i = 0; i < chain().size() - 1; i++) {
      int n = chain().size() - 1 - i;
      int idx = i;
      //idx = _topToBottom ? i : chain().size() - 2 - i;
      //_delegationAtJoint[idx] = k / (k + n - 1);
      _delegationAtJoint[idx] = 0.4f;
    }
  }


  public void setDelegationFactor(float k) {
    //_enableDelegation = true;
    _delegationFactor = k;
    setKDistributionDelegation(_delegationFactor);
  }

  public void enableDelegation(boolean enableDelegation) {
    _enableDelegation = enableDelegation;
  }

  public boolean enableDelegation() {
    return _enableDelegation;
  }

  public void setTopToBottom(boolean topToBottom) {
    if (_topToBottom != topToBottom) {
      _topToBottom = topToBottom;
      //swap delegation per joint
      //_swapDelegationPerJoint();
    }
  }

  public void _swapDelegationPerJoint() {
    System.out.println("swap!!");
    int last = _delegationAtJoint.length - 1;
    for (int i = 0; i <= last; i++) {
      System.out.print(_delegationAtJoint[i] + " , ");
    }
    System.out.println();
    for (int i = 0; i <= last / 2; i++) {
      float aux = _delegationAtJoint[i];
      _delegationAtJoint[i] = _delegationAtJoint[last - i];
      _delegationAtJoint[last - i] = aux;
    }
    for (int i = 0; i <= last; i++) {
      System.out.print(_delegationAtJoint[i] + " , ");
    }
    System.out.println();
  }


  public boolean topToBottom() {
    return _topToBottom;
  }


  /*Error measures*/
  public static float positionError(Vector eff, Vector target) {
    return Vector.distance(eff, target);
  }

  public static float positionError(NodeInformation eff, Node target) {
    return positionError(eff.positionCache(), target.position());
  }

  public static float orientationError(Quaternion eff, Quaternion target, boolean angles) {
    float s1 = 1, s2 = 1;
    if (eff.w() < 0) s1 = -1;
    if (target.w() < 0) s2 = -1;
    float dot = s1 * eff._quaternion[0] * s2 * target._quaternion[0] +
        s1 * eff._quaternion[1] * s2 * target._quaternion[1] +
        s1 * eff._quaternion[2] * s2 * target._quaternion[2] +
        s1 * eff._quaternion[3] * s2 * target._quaternion[3];

    //clamp dot product
    dot = Math.min(Math.max(dot, -1), 1);
    if (angles) return (float) Math.toDegrees(Math.acos(Math.min(Math.max(2 * dot * dot - 1, -1), 1)));
    return (float) (1 - dot * dot);
  }


  public static float quaternionDistance(Quaternion a, Quaternion b) {
    float s1 = 1, s2 = 1;
    if (a.w() < 0) s1 = -1;
    if (b.w() < 0) s2 = -1;
    float dot = s1 * a._quaternion[0] * s2 * b._quaternion[0] + s1 * a._quaternion[1] * s2 * b._quaternion[1] + s1 * a._quaternion[2] * s2 * b._quaternion[2] + s1 * a._quaternion[3] * s2 * b._quaternion[3];
    return (float) (1 - Math.pow(dot, 2));
  }

  public float error(NodeInformation eff, Node target, float w1, float w2) {
    return error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation(), w1, w2);
  }

  public float error(NodeInformation eff, Node target) {
    return error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation(), 1, 1);
  }

  public float error(Vector effPosition, Vector targetPosition, Quaternion effRotation, Quaternion targetRotation, float w1, float w2) {
    float error = positionError(effPosition, targetPosition);
    float radius = _radiusRelativeToBoneAverage ? _searchingAreaRadius * _avgLength : _searchingAreaRadius;
    if (_direction) {
      float orientationError = orientationError(effRotation, targetRotation, false);
      float weighted_error = error / radius;
      float c_k = (float) Math.floor(weighted_error);
      error = c_k + _orientationWeight * orientationError + (1 - _orientationWeight) * (weighted_error - c_k);
    }
    return error;
  }

  public void update() {
    //find maxLength
    float maxLength = 0;
    for (int i = 0; i < chain().size() - 1; i++) {
      maxLength += Vector.distance(chainInformation().get(i).positionCache(), chainInformation().get(i + 1).positionCache());
    }
    _maxLength = maxLength;
    _avgLength = maxLength / chain().size();

    //Set values of worldTarget and worldEndEffector
    if (_target != null) {
      worldTarget().setRotation(target().orientation().get());
      worldTarget().setPosition(target().position().get());
    }
  }

  public static List<Node> _detachedCopy(List<? extends Node> chain, Node reference) {
    return _detachedCopy(chain, reference, true);
  }

  public static List<Node> _detachedCopy(List<? extends Node> chain, Node reference, boolean copy_constraints) {
    List<Node> copy = new ArrayList<Node>();
    for (Node joint : chain) {
      Node newJoint = Node.detach(new Vector(), new Quaternion(), 1);
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      if (copy_constraints) newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }

  public static List<Node> _detachedCopy(List<? extends Node> chain) {
    return _detachedCopy(chain, true);
  }

  public static List<Node> _detachedCopy(List<? extends Node> chain, boolean copy_constraints) {
    Node reference = chain.get(0).reference();
    if (reference != null) {
      reference = Node.detach(reference.position().get(), reference.orientation().get(), 1);
    }
    return _detachedCopy(chain, reference, copy_constraints);
  }

  /*TODO: remove this! (debug purposes)*/
  public static List<Node> _attachedCopy(List<? extends Node> chain, Node reference) {
    return _attachedCopy(chain, reference, true);
  }

  public static List<Node> _attachedCopy(List<? extends Node> chain, Node reference, boolean copy_constraints) {
    Node ref = reference;
    List<Node> copy = new ArrayList<Node>();
    if (ref == null) {
      reference = chain.get(0).reference();
      if (reference != null) {
        ref = new Node();
        ref.setPosition(reference.position().get());
        ref.setOrientation(reference.orientation().get());
        ref.enableTagging(false);
      }
    }

    int r = (int) (Math.random() * 255);
    int g = (int) (Math.random() * 255);
    int b = (int) (Math.random() * 255);
    for (Node joint : chain) {
      Joint newJoint = new Joint(r, g, b, 3);
      if (copy.isEmpty()) {
        newJoint.setRoot(true);
      }
      newJoint.setReference(ref);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      if (copy_constraints) newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      ref = newJoint;
    }
    return copy;
  }
}
