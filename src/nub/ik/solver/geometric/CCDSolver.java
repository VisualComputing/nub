/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.ik.solver.geometric;

import nub.core.Node;
import nub.ik.animation.IKAnimation;
import nub.ik.animation.SolverAnimation;
import nub.ik.solver.KinematicStructure;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.List;

//TODO : Enable / Disable iteration Hist

public class CCDSolver extends Solver {
  protected List<? extends Node> _chain;
  //TODO: This is just for debug purposes
  public boolean _enable_kinematic_structure = true;
  //Using a Kinematic Structure decreases the number of transformations
  protected List<KinematicStructure.KNode> _structure;
  protected Node _target;
  protected Node _previousTarget;

  //Animation Stuff
  protected boolean _enableHistory;
  protected IKAnimation.NodeStates _history;

  //TODO: Refactor, perhaps move to Solver class
  protected SolverAnimation _solverAnimation;

  public void enableHistory(boolean enableHistory) {
    _enableHistory = enableHistory;
  }

  public List<? extends Node> chain() {
    return _chain;
  }

  public Node target() {
    return _target;
  }

  public void setTarget(Node endEffector, Node target) {
    this._target = target;
  }

  public void setTarget(Node target) {
    this._target = target;
  }

  public Node head() {
    return _chain.get(0);
  }

  public Node endEffector() {
    return _chain.get(_chain.size() - 1);
  }

  public CCDSolver(List<? extends Node> chain) {
    this(chain, null);
  }

  public CCDSolver(List<? extends Node> chain, boolean enable_kinematic_structure) {
        this(chain, null, enable_kinematic_structure);
    }

  public CCDSolver(List<? extends Node> chain, Node target) {
    this(chain, target, true);
  }

  public CCDSolver(List<? extends Node> chain, Node target, boolean enable_kinematic_structure) {
    super();
    _enable_kinematic_structure = enable_kinematic_structure;
    if(_enable_kinematic_structure) this._structure = KinematicStructure.generateKChain(chain);
    else this._chain = chain;
    this._target = target;
    this._previousTarget =
        target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
    if (_enableHistory) _history = new IKAnimation.NodeStates();
  }

  /*
   * Performs a CCD ITERATION
   * For further info please look at (https://sites.google.com/site/auraliusproject/ccd-algorithm)
   * */
  @Override
  protected boolean _iterate() {
    if(_enable_kinematic_structure) return iterateWithStructure();
    return iterateWithChain();
  }

  //TODO: this must be removed
  protected boolean iterateWithChain(){
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _chain.size() < 2) return true;
    Node end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= _maxError) {
      return true;
    }
    float change = 0.0f;
    Vector endLocalPosition = _chain.get(_chain.size() - 2).location(end.position());
    Vector targetLocalPosition = _chain.get(_chain.size() - 2).location(target);

    for (int i = _chain.size() - 2; i >= 0; i--) {
      Quaternion initial = _chain.get(i).rotation().get();
      Quaternion delta = new Quaternion(endLocalPosition, targetLocalPosition);
      //update target local position
      if (_chain.get(i).reference() == null) {
        targetLocalPosition = _chain.get(i).worldLocation(targetLocalPosition);
      } else {
        targetLocalPosition = _chain.get(i).reference().location(targetLocalPosition, _chain.get(i));
      }
      _chain.get(i).rotate(delta);
      //update end effector local position
      if (_chain.get(i).reference() == null) {
        endLocalPosition = _chain.get(i).worldLocation(endLocalPosition);
      } else {
        endLocalPosition = _chain.get(i).reference().location(endLocalPosition, _chain.get(i));
      }
      initial.compose(_chain.get(i).rotation().inverse());
      change += Math.abs(initial.angle());
      if (_enableHistory) {
        history().addNodeState("step", _chain.get(i), _chain.get(i).reference(), null, _chain.get(i).rotation().get());
        history().incrementStep();
      }
    }
    if (_enableHistory) history().incrementIteration();
    //Check total rotation change
    if (change <= _minDistance) return true;
    return false;
  }

  protected boolean iterateWithStructure(){
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _structure.size() < 2) return true;
    KinematicStructure.KNode end = _structure.get(_structure.size() - 1);
    Vector target = this._target.position().get();
    //update whole chain position/orientation
    Vector endPosition = end.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= _maxError) {
      return true;
    }

    float change = 0.0f;

    //Traverse the structure backwards
    for (int i = _structure.size() - 2; i >= 0; i--) {
      Vector targetLocalPosition = _structure.get(i).location(target, true);
      Vector endLocalPosition = _structure.get(i).location(end.position(), true);
      if(_solverAnimation != null) {
        _solverAnimation.enableSequential(false);
        _solverAnimation.addTrajectory(null, _structure.get(i).position(), end.position());
        _solverAnimation.addTrajectory(_structure.get(i).node(), new Vector(), endLocalPosition);
        _solverAnimation.addMessage("Step 1: Find the segment line defined by Joint " + i + " and End Effector " + (_structure.size() -1), 32);
        _solverAnimation.enableSequential(true);
        _solverAnimation.clearStep(1);
        _solverAnimation.addTrajectory(null, _structure.get(i).position(), target);
        _solverAnimation.enableSequential(false);
        _solverAnimation.addMessage("Step 2: Find the segment Line defined by Joint " + i + " and Target", 32);
        _solverAnimation.enableSequential(true);
        _solverAnimation.clearStep(1);
      }

      Quaternion delta = new Quaternion(endLocalPosition, targetLocalPosition);
      _structure.get(i).rotate(delta);

      if(_solverAnimation != null){
        _solverAnimation.addRotateNode(_structure.get(i).node(), delta);
        _solverAnimation.enableSequential(false);
        _solverAnimation.addMessage("Step 3: Rotate Joint " + i + " to reduce the distance from End Effector " + i + " to Target (T)", 32);
        _solverAnimation.enableSequential(true);
        _solverAnimation.clearStep(9);
      }

      //update end effector local position
      if (_enableHistory) {
        history().addNodeState("step", _structure.get(i).node(), _structure.get(i).node().reference(), null, _structure.get(i).rotation().get());
        history().incrementStep();
      }
    }
    if (_enableHistory) history().incrementIteration();
    //Check total change
    if (Vector.distance(endPosition, end.position()) <= _minDistance) return true;
    return false;
  }


  @Override
  protected void _update() {
    /*Not required, since chain is updated inside iterate step*/
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
    _previousTarget = _target == null ? null : new Node(_target.position().get(), _target.orientation().get(), 1);
    _iterations = 0;
    if (_enableHistory) {
      if (_history == null) _history = new IKAnimation.NodeStates();
      else _history.clear();
    }
    if(_enable_kinematic_structure){
        //TODO: Remove this update!
        _structure.get(_structure.size()-1).updatePath(null);
      for (KinematicStructure.KNode node : _structure) {
        if (_enableHistory)
          _history.addNodeState("initialization", node.node(), node.node().reference(), node.translation().get(), node.rotation().get());
      }
    }
    else {
      for (Node node : chain()) {
        if (_enableHistory)
          _history.addNodeState("initialization", node, node.reference(), node.translation().get(), node.rotation().get());
      }
    }
    if (_enableHistory) {
      _history.incrementStep();
      _history.incrementIteration();
    }
  }

  @Override
  public float error() {
    if(_enable_kinematic_structure) return Vector.distance(_structure.get(_structure.size() - 1).position(), _target.position());
    return Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
  }

  //Animation Stuff
  public IKAnimation.NodeStates history() {
    return _history;
  }

  //TODO: Refactor, perhaps move to Solver class
  public void attachSolverAnimation(SolverAnimation solverAnimation){
      _solverAnimation = solverAnimation;
      registerStructures();
  }

  public void registerStructures(){
      if(_enable_kinematic_structure){
          List<Node> chain = new ArrayList<Node>();
          for(KinematicStructure.KNode knode : _structure){
              chain.add(knode.node());
          }
          _solverAnimation.registerStructure("Chain", chain);
      }else {
          _solverAnimation.registerStructure("Chain", _chain);
      }
  }
}
