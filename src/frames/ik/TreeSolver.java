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

import frames.core.Node;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;

public class TreeSolver extends FABRIKSolver {
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

  public Node head() {
    return (Node) root._solver().head();
  }

  protected void _setup(TreeNode parent, Node frame, ArrayList<Node> list) {
    if (frame == null) return;
    if (frame.children().isEmpty()) {
      list.add(frame);
      ChainSolver solver = new ChainSolver(list, null);
      new TreeNode(parent, solver);
      return;
    }
    if (frame.children().size() > 1) {
      list.add(frame);
      ChainSolver solver = new ChainSolver(list, null);
      TreeNode treeNode = new TreeNode(parent, solver);
      for (Node child : frame.children()) {
        ArrayList<Node> newList = new ArrayList<Node>();
        newList.add(frame);
        _setup(treeNode, child, newList);
      }
    } else {
      list.add(frame);
      _setup(parent, frame.children().get(0), list);
    }
  }

  protected boolean _addTarget(TreeNode treeNode, Node endEffector, Frame target) {
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

  public boolean addTarget(Node endEffector, Frame target) {
    return _addTarget(root, endEffector, target);
  }

  public TreeSolver(Node genericFrame) {
    super();
    TreeNode dummy = new TreeNode(); //Dummy TreeNode to Keep Reference
    _setup(dummy, genericFrame, new ArrayList<Node>());
    //dummy must have only a child,
    this.root = dummy._children().get(0);
  }

  protected int _forwardReaching(TreeNode treeNode) {
    float totalWeight = 0;
    boolean modified = false;
    int chains = 0;
    for (TreeNode child : treeNode._children()) {
      chains += _forwardReaching(child);
      if (child._solver().target() != null) totalWeight += child._weight();
      modified = modified || child._modified;
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
      solver.setTarget(new Frame(newTarget, solver.endEffector().orientation().get()));
    }

    //Execute Until the distance between the end effector and the target is below a threshold
    if (solver.target() == null) {
      treeNode._modified = false;
      return chains;
    }
    if (Vector.distance(solver.endEffector().position(), solver.target().position()) <= error) {
      treeNode._modified = false;
      return chains;
    }
    solver._positions().set(solver.chain().size() - 1, solver._target.position().get());
    solver._forwardReaching();
    treeNode._modified = true;
    return chains + 1;
  }

  protected float _backwardReaching(TreeNode treeNode) {
    float change = minDistance;
    if (treeNode._modified) {
      ChainSolver solver = treeNode._solver();
      solver._positions().set(0, solver.head().position());
      change = solver._backwardReaching();
      /*When executing Backward Step, if the Frame is a SubBase (Has more than 1 Child) and
       * it is not a "dummy Frame" (Convenient Frame that constraints position but no orientation of
       * its children) then an additional step must be done: A Weighted Average of Positions to establish
       * new Frame orientation
       * */
      //TODO : Perhaps add an option to not execute this step
      // (Last chain modified determines Sub Base orientation)
      if (treeNode._children().size() > 1) {
        Vector centroid = new Vector();
        Vector newCentroid = new Vector();
        float totalWeight = 0;
        for (TreeNode child : treeNode._children()) {
          //If target is null, then Joint must not be included
          if (child._solver().target() == null) continue;
          if (child._solver().chain().size() < 2) continue;
          if (child._solver().chain().get(1).translation().magnitude() == 0) continue;
          Vector diff = solver.endEffector().coordinatesOf(child._solver().chain().get(1).position());
          centroid.add(Vector.multiply(diff, child._weight()));
          if (child._modified) {
            diff = solver.endEffector().coordinatesOf(child._solver()._positions().get(1));
            newCentroid.add(Vector.multiply(diff, child._weight()));
          } else {
            newCentroid.add(Vector.multiply(diff, child._weight()));
          }
          totalWeight += child._weight();
        }
        //Set only when Centroid and New Centroid varies
        if (Vector.distance(centroid, newCentroid) > 0.001) {
          centroid.multiply(1.f / totalWeight);
          newCentroid.multiply(1.f / totalWeight);
          Quaternion deltaOrientation = new Quaternion(centroid, newCentroid);
          treeNode._solver().endEffector().rotate(deltaOrientation);
          for (TreeNode child : treeNode._children()) {
            if (child._solver().chain().size() < 2) continue;
            if (child._solver().chain().get(1).translation().magnitude() == 0) continue;
            if (child._modified) {
              child._solver()._positions().set(1, child._solver().chain().get(1).position());
            }
          }
        }
      }
    }
    for (TreeNode child : treeNode._children()) {
      change += _backwardReaching(child);
    }
    return change;
  }

  @Override
  protected boolean _iterate() {
    int modifiedChains = _forwardReaching(root);
    float change = _backwardReaching(root);
    change = modifiedChains > 0 ? change / (modifiedChains * 1.f) : change;
    //Check total position change
    if (change / (modifiedChains * 1.) <= minDistance) return true;
    return false;
  }

  @Override
  protected void _update() {
    //As BackwardStep modify chains, no update is required
  }

  //Update Subtree that have associated Frame as root
  protected boolean _updateTree(TreeNode treeNode, Node frame) {
    if (treeNode._solver().endEffector() == frame) {
      _setup(treeNode, frame, new ArrayList<Node>());
      return true;
    }
    for (TreeNode child : treeNode._children()) {
      _updateTree(child, frame);
    }
    return false;
  }

  protected boolean _changed(TreeNode treeNode) {
    if (treeNode == null) return false;
    if (treeNode._solver()._changed()) return true;
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
  }

  @Override
  public void _reset() {
    iterations = 0;
    _reset(root);
  }
}