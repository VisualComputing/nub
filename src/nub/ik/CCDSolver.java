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

package nub.ik;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

//TODO : Enable / Disable iteration Hist

public class CCDSolver extends Solver {
  protected List<? extends Node> _chain;
  protected Node _target;
  protected Node _previousTarget;

  //Animation Stuff
  protected ArrayList<ArrayList<Vector>> _iterationsHistory;

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

  public CCDSolver(List<? extends Node> chain, Node target) {
    super();
    this._chain = chain;
    this._target = target;
    this._previousTarget =
        target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
  }

  /*
   * Performs a CCD ITERATION
   * For further info please look at (https://sites.google.com/site/auraliusproject/ccd-algorithm)
   * */
  @Override
  protected boolean _iterate() {
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

    ArrayList<Vector> positions = new ArrayList<Vector>();

    for (int i = _chain.size() - 2; i >= 0; i--) {
      Quaternion delta = null;
      Quaternion initial = _chain.get(i).rotation().get();
      delta = new Quaternion(endLocalPosition, targetLocalPosition);
      //update target local position
      if(_chain.get(i).reference() == null){
        targetLocalPosition = _chain.get(i).worldLocation(targetLocalPosition);
      } else {
        targetLocalPosition = _chain.get(i).reference().location(targetLocalPosition, _chain.get(i));
      }
      _chain.get(i).rotate(delta);
      //update end effector local position
      if(_chain.get(i).reference() == null){
        endLocalPosition = _chain.get(i).worldLocation(endLocalPosition);
      } else {
        endLocalPosition = _chain.get(i).reference().location(endLocalPosition, _chain.get(i));
      }
      initial.compose(_chain.get(i).rotation().get());
      change += Math.abs(initial.angle());
      positions.add(0,_chain.get(i+1).position().get());
    }
    positions.add(0,_chain.get(0).position().get());

    addIterationRecord(positions);
    //Check total rotation change
    if (change <= _minDistance) return true;
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
    _iterationsHistory = new ArrayList<>();
    ArrayList<Vector> positions = new ArrayList<Vector>();
    for(Node node : chain()){
      positions.add(node.position().get());
    }
    addIterationRecord(positions);
  }

  @Override
  public float error() {
    return Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
  }

  //Animation Stuff
  public ArrayList<ArrayList<Vector>> iterationsHistory(){
    return _iterationsHistory;
  }

  public void addIterationRecord(ArrayList<Vector> iteration){
    _iterationsHistory.add(new ArrayList<>(iteration));
  }


}
