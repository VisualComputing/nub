/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.ik;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;

public class TreeSolver extends FABRIKSolver {
  //TODO : How to apply proposed heuristics to this solver?
  //TODO : Update - check copy of chains and references
  /*Convenient Class to store ChainSolvers in a Tree Structure*/
  protected static class TreeNode {
    protected TreeNode _parent;
    protected ArrayList<TreeNode> _children;
    protected ChainSolver _solver;
    protected boolean _modified;
    protected float _weight = 1.f;

    public TreeNode() {
      _children = new ArrayList<TreeNode>();
    }

    public TreeNode(ChainSolver solver) {
      this._solver = solver;
      _children = new ArrayList<TreeNode>();
    }

    protected TreeNode(TreeNode parent, ChainSolver solver) {
      this._parent = parent;
      this._solver = solver;
      if (parent != null) {
        parent._addChild(this);
      }
      _children = new ArrayList<TreeNode>();
    }

    protected boolean _addChild(TreeNode n) {
      return _children.add(n);
    }

    protected ArrayList<TreeNode> _children() {
      return _children;
    }

    protected float _weight() {
      return _weight;
    }

    protected ChainSolver _solver() {
      return _solver;
    }
  }

  //TODO Relate weights with End Effectors not with chains
  /*Tree structure that contains a list of Solvers that must be accessed in a BFS way*/
  protected TreeNode root;
  protected float _current = 10e10f, _best = 10e10f;
  /*Heuristic Parameters*/
  protected boolean _fixTwisting = true;
  protected boolean _keepDirection = true;

  public Frame head() {
    return (Frame) root._solver().head();
  }

  protected void _setup(TreeNode parent, Frame frame, ArrayList<Frame> list) {
    if (frame == null) return;
    if (frame.children().isEmpty()) {
      list.add(frame);
      ChainSolver solver = new ChainSolver(list, _copyChain(parent, list), null);
      new TreeNode(parent, solver);
      return;
    }
    if (frame.children().size() > 1) {
      list.add(frame);
      ChainSolver solver = new ChainSolver(list, _copyChain(parent, list), null);
      TreeNode treeNode = new TreeNode(parent, solver);
      for (Frame child : frame.children()) {
        ArrayList<Frame> newList = new ArrayList<Frame>();
        newList.add(frame);
        _setup(treeNode, child, newList);
      }
    } else {
      list.add(frame);
      _setup(parent, frame.children().get(0), list);
    }
  }

  protected boolean _addTarget(TreeNode treeNode, Frame endEffector, Frame target) {
    if (treeNode == null) return false;
    if (treeNode._solver().endEffector() == endEffector) {
      treeNode._solver().setTarget(target);
      return true;
    }
    for (TreeNode child : treeNode._children()) {
      _addTarget(child, endEffector, target);
    }
    return false;
  }

  protected boolean _addTargetDirection(TreeNode treeNode, Frame endEffector, Vector direction) {
    if (treeNode == null) return false;
    if (treeNode._solver().endEffector() == endEffector) {
      treeNode._solver().setTargetDirection(direction);
      return true;
    }
    for (TreeNode child : treeNode._children()) {
      _addTargetDirection(child, endEffector, direction);
    }
    return false;
  }

  public boolean addTarget(Frame endEffector, Frame target) {
    return _addTarget(root, endEffector, target);
  }

  public boolean addTargetDirection(Frame endEffector, Vector direction) {
    return _addTargetDirection(root, endEffector, direction);
  }

  public TreeSolver(Frame frame) {
    super();
    TreeNode dummy = new TreeNode(); //Dummy TreeNode to Keep Reference
    _setup(dummy, frame, new ArrayList<Frame>());
    //dummy must have only a child,
    this.root = dummy._children().get(0);
    this.root._parent = null;
  }

  protected int _forwardReaching(TreeNode treeNode) {
    ArrayList<Vector> list_a = new ArrayList<>();
    for(Frame f : treeNode._solver._chain) list_a.add(f.worldLocation(new Vector()));
    aux_prev.add(list_a);
    float totalWeight = 0;
    int chains = 0;

    /*int n = treeNode._children().size();
    int idx = n > 0 ? (int) Math.round(Math.random() * (n - 1)) : 0; //TODO: Randomization not important
    for (int c = 0 ; c < n ; c++, idx = (idx + 1) % n){*/
    for (TreeNode child : treeNode._children()) {
      chains += _forwardReaching(child);
      if (child._solver().target() != null) totalWeight += child._weight();
    }
    //Stage 1: Forward Reaching
    ChainSolver solver = treeNode._solver();
    //TODO: add embedded target and enable to give it some weight - Weight/Target as an attribute of Chain or as TreeNode attribute?
    //Update Target according to children Head new Position
    Vector newTarget = new Vector();
    for (TreeNode child : treeNode._children()) {
      //If Child Chain Joints new positions doesn't matter
      if (child._solver().target() == null) continue;
      newTarget.add(Vector.multiply(child._solver()._positions().get(0), 1.f / totalWeight));
    }
    if (newTarget.magnitude() > 0) {
      solver.setTarget(new Frame(newTarget, solver._chain.get(solver._chain.size() - 1).orientation().get(), 1));
    }

    //Execute Until the distance between the end effector and the target is below a threshold
    if (solver.target() == null) {
      treeNode._modified = false;
      return chains;
    }
    if (Vector.distance(solver._chain.get(solver._chain.size() - 1).position(), solver.target().position()) <= error) {
      treeNode._modified = false;
      return chains;
    }


    //TODO: Check blocked and oscillation decisions based on "e"
    /*solver._lastError = iterations % 2 == 0 ?
            Vector.distance(solver._chain.get(solver._chain.size()-1).position(), solver._target.position()) :
            Math.max(Vector.distance(solver._chain.get(solver._chain.size()-1).position(), solver._target.position()), solver._lastError);*/

    //Apply Fix Twisting before applying a Full Fabrik iteration to whole chain
    //TODO: 1. is there a better condition ? 2. How to consider properly twisting of a Node with 2 or more children
    //TODO : Twisting to favour region of movement
    if(_fixTwisting && solver._chain.size() > 2 && iterations % 3 == 0){
      if(treeNode._parent != null && treeNode._parent._children.size() > 1){
        _applyTwistRotation(solver._chain.subList(1, solver._chain.size()), solver._target.position());
      } else{
        _applyTwistRotation(solver._chain, solver._target.position());
      }
      //TODO : Do it efficiently
      for(int i = 0; i < solver._chain.size(); i++){
        solver._positions.set(i, solver._chain.get(i).position());
      }
    }

    /* TODO: Check if it's better for convergence to try more times with local regions */
    Vector o = solver._chain.get(0).position();
    Vector p = null;
    if(solver._positions().size() > 1) p = solver._chain.get(1).position();

    for(int i = 0; i < 3 && solver._positions().size() > 1; i++) {
      solver._positions().set(solver._chain.size() - 1, solver._target.position().get());
      if(solver._targetDirection != null){
        solver._applyTargetdirection();
      }
      solver._forwardReaching();
      solver._positions().set(0, o);
      solver._positions().set(1, p);
      solver._backwardReaching(o);
    }

    solver._positions().set(solver._chain.size() - 1, solver._target.position().get());
    solver._forwardReaching();
    ArrayList<Vector> list = new ArrayList<>();
    for(Vector v : solver._positions()){
      list.add(v.get());
    }
    aux_p.add(list);

    treeNode._modified = true;
    for (TreeNode child : treeNode._children()){
      if(!child._modified) chains += 1;
      child._modified = true;
    }
    return chains + 1;
  }

  public static ArrayList<ArrayList<Vector>> aux_p = new ArrayList<>();
  public static ArrayList<ArrayList<Vector>> aux_prev = new ArrayList<>();

  protected float _backwardReaching(TreeNode treeNode) {
    float change = minDistance;
    if (treeNode._modified) {
      ChainSolver solver = treeNode._solver();
      Vector o = solver._positions().get(0);
      solver._positions().set(0, solver._chain.get(0).position().get());
      change = solver._backwardReaching(o);
      //Get error
      /*When executing Backward Step, if the Frame is a SubBase (Has more than 1 Child) and
       * it is not a "dummy Frame" (Convenient Frame that constraints position but no orientation of
       * its children) then an additional step must be done: A Weighted Average of Positions to establish
       * new Frame orientation
       * */
      //TODO : CHECK WHY CENTROID GENERATES WORST BEHAVIORS
      // (Last chain modified determines Sub Base orientation)
      //TODO : AVERAGE ROTATION USING SVD (SAME AS FINDING BEST RIGID TRANSFORMATION)
      if (treeNode._children().size() > 1) {
        Vector centroid = new Vector();
        Vector newCentroid = new Vector();
        float totalWeight = 0;
        int amount = 1;
        float[] cumulative = new float[4];
        Quaternion des = new Quaternion();
        Quaternion first = null;
        for (TreeNode child : treeNode._children()) {
          //If target is null, then Joint must not be included
          if (child._solver().target() == null) continue;
          if (child._solver()._chain.size() < 2) continue;
          if (child._solver()._chain.get(1).translation().magnitude() == 0) continue;
          Vector diff = solver._chain.get(solver._chain.size() - 1).location(child._solver()._chain.get(1).position());
          centroid.add(Vector.multiply(diff, child._weight()));
          Vector v1 = solver._chain.get(solver._chain.size() - 1).location(child._solver()._chain.get(1).position());
          Vector v2 = v1;
          if (child._modified) {
            diff = solver._chain.get(solver._chain.size() - 1).location(child._solver()._positions().get(1));
            newCentroid.add(Vector.multiply(diff, child._weight()));
            v2 = solver._chain.get(solver._chain.size() - 1).location(child._solver()._positions().get(1));
          } else {
            newCentroid.add(Vector.multiply(diff, child._weight()));
          }
          Quaternion q = new Quaternion(v1, v2);

          if(amount == 1){
            for(int i = 0; i < 4; i++)
              cumulative[i] = q._quaternion[i];
              first = q;
          }else {
            des = averageQuaternion(cumulative, q, first, amount);
          }
          totalWeight += child._weight();
          amount++;
        }
        //Set only when Centroid and New Centroid varies
        //if (Vector.distance(centroid, newCentroid) > 0.001) {
          centroid.multiply(1.f / totalWeight);
          newCentroid.multiply(1.f / totalWeight);
          Quaternion deltaOrientation = new Quaternion(centroid, newCentroid);
          float ang = deltaOrientation.angle();
          //clamp rotation
          ang = (float)Math.max(Math.min(ang, 10*Math.PI/180), -10*Math.PI/180);
          deltaOrientation = new Quaternion(deltaOrientation.axis(), ang);
          //System.out.println(" ax : " + deltaOrientation.axis() + " ang : " + ang);
          treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).rotate(des);
          treeNode._solver()._positions().set(treeNode._solver()._chain.size() - 1, treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).position());

          for (TreeNode child : treeNode._children()) {
            child._solver()._positions().set(0, child._solver()._chain.get(0).position());
            child._solver()._positions().set(1, child._solver()._chain.get(1).position());
            //child._solver()._chain.get(0).setPosition(treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).position().get());
            //child._solver()._chain.get(0).setOrientation(treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).orientation().get());
            //child._solver()._chain.get(0).setRotation(treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).rotation().get());
            //if (child._solver()._chain.size() < 2) continue;
            /*if (child._solver()._chain.get(1).translation().magnitude() == 0) continue;
            if (child._modified) {
              child._solver()._positions().set(0, solver._chain.get(0).position());
              child._solver()._positions().set(1, child._solver()._chain.get(1).position());
            }*/
          }
        //}
      }
    }

    for (TreeNode child : treeNode._children()) {
      change += _backwardReaching(child);
    }

    if(treeNode._solver().target() != null && treeNode._children.isEmpty())
      _current += Vector.distance(treeNode._solver()._chain.get(treeNode._solver()._chain.size() - 1).position(), treeNode._solver().target().position());

    return change;
  }

  @Override
  protected boolean _iterate() {
    aux_p = new ArrayList<>();
    aux_prev = new ArrayList<>();
    _current = 0;
    int modifiedChains = _forwardReaching(root);
    float change = _backwardReaching(root);
    change = modifiedChains > 0 ? change / (modifiedChains * 1.f) : change;
    //Check total position change
    _update();
    if (change / (modifiedChains * 1.) <= minDistance) return true;
    return false;
  }

  @Override
  protected void _update() {
    if(_current < _best){
      _updateTree(root);
      _best = _current;
    }
  }

  //Update Subtree that have associated Frame as root
  protected void _updateTree(TreeNode treeNode) {
    for(int i = 0; i < treeNode._solver._original.size(); i++){
      treeNode._solver._original.get(i).setRotation(treeNode._solver._chain.get(i).rotation().get());
    }
    for (TreeNode child : treeNode._children()) {
      _updateTree(child);
    }
  }

  protected boolean _changed(TreeNode treeNode) {
    if (treeNode == null) return false;
    if (treeNode._solver()._changed() && treeNode._children().isEmpty()) return true;
    for (TreeNode child : treeNode._children()) {
      if (_changed(child)) return true;
    }
    return false;
  }

  @Override
  protected boolean _changed() {
    return _changed(root);
  }

  protected void _reset(TreeNode treeNode) {
    if (treeNode == null) return;
    //Update Previous Target
    if (treeNode._solver()._changed()) treeNode._solver()._reset();
    for (TreeNode child : treeNode._children()) {
      _reset(child);
    }
    if(treeNode._solver._target != null && treeNode._children.isEmpty()) {
      _best += Vector.distance(treeNode._solver()._original.get(treeNode._solver()._original.size() - 1).position(), treeNode._solver().target().position());
    }
  }

  @Override
  protected void _reset() {
    iterations = 0;
    _best = 0;
    _current = 0;
    _reset(root);
  }

  @Override
  public float error() {
    return _best;
  }

  @Override
  public void setTarget(Frame endEffector, Frame target) {
    addTarget(endEffector,target);
  }


  protected ArrayList<Frame> _copyChain(TreeNode parent, ArrayList<Frame> list){
    if(parent._solver != null) {
      Frame reference = parent._solver._chain.get(parent._solver._chain.size() - 1);
      ArrayList<Frame> copy = _copy(list.subList(1, list.size()), reference);
      copy.add(0, reference);
      return copy;
    }
    return _copy(list);
  }

  //AVERAGING QUATERNIONS AS SUGGESTED IN http://wiki.unity3d.com/index.php/Averaging_Quaternions_and_Vectors
  public static Quaternion averageQuaternion(float[] cumulative, Quaternion newRotation, Quaternion firstRotation, int addAmount){
    float w = 0.0f;
    float x = 0.0f;
    float y = 0.0f;
    float z = 0.0f;
    //Before we add the new rotation to the average (mean), we have to check whether the quaternion has to be inverted. Because
    //q and -q are the same rotation, but cannot be averaged, we have to make sure they are all the same.
    if(!areQuaternionsClose(newRotation, firstRotation)){
      newRotation = inverseSignQuaternion(newRotation);
    }

    //Average the values
    float addDet = 1f/(float)addAmount;
    cumulative[0] += newRotation.x();
    x = cumulative[0] * addDet;
    cumulative[1] += newRotation.y();
    y = cumulative[1] * addDet;
    cumulative[2] += newRotation.z();
    z = cumulative[2] * addDet;
    cumulative[3] += newRotation.w();
    w = cumulative[3] * addDet;

    //note: if speed is an issue, you can skip the normalization step
    return normalizeQuaternion(x, y, z, w);
  }

  public static Quaternion normalizeQuaternion(float x, float y, float z, float w){
    float lengthD = 1.0f / (w*w + x*x + y*y + z*z);
    w *= lengthD;
    x *= lengthD;
    y *= lengthD;
    z *= lengthD;
    return new Quaternion(x, y, z, w);
  }

  //Changes the sign of the quaternion components. This is not the same as the inverse.
  public static Quaternion inverseSignQuaternion(Quaternion q){
    return new Quaternion(-q.x(), -q.y(), -q.z(), -q.w());
  }

  //Returns true if the two input quaternions are close to each other. This can
//be used to check whether or not one of two quaternions which are supposed to
//be very similar but has its component signs reversed (q has the same rotation as
//-q)
  public static boolean areQuaternionsClose(Quaternion q1, Quaternion q2){
    float dot = Quaternion.dot(q1, q2);
    if(dot < 0.0f){
      return false;
    }
    else{
      return true;
    }
  }

  public void setFixTwisting(boolean fixTwisting){
    _fixTwisting = fixTwisting;
  }

  public void setKeepDirection(boolean keepDirection){
    _keepDirection = keepDirection;
    setKeepDirection(root, keepDirection);
  }

  public void setKeepDirection(TreeNode node, boolean keepDirection){
    if(node == null) return;
    node._solver.setKeepDirection(keepDirection);
    for(TreeNode child : node._children){
      setKeepDirection(child, keepDirection);
    }
  }

}

