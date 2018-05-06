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

public class CCDSolver extends Solver {
  protected ArrayList<? extends Frame> _chain;
  protected Frame _target;
  protected Frame _previousTarget;

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

  public CCDSolver(ArrayList<? extends Frame> chain) {
    this(chain, null);
  }

  public CCDSolver(ArrayList<? extends Frame> chain, Frame target) {
    super();
    this._chain = chain;
    this._target = target;
    //TODO needs testing
    this._previousTarget = target == null ? null : target.detach();
  }

  /*
   * Performs a CCD ITERATION
   * For further info please look at (https://sites.google.com/site/auraliusproject/ccd-algorithm)
   * */
  @Override
  protected boolean _iterate() {
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _chain.size() < 2) return true;
    Frame end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= error) {
      return true;
    }
    float change = 0.0f;
    Vector endLocalPosition = _chain.get(_chain.size() - 2).location(end.position());
    Vector targetLocalPosition = _chain.get(_chain.size() - 2).location(target);
    for (int i = _chain.size() - 2; i >= 0; i--) {
      Quaternion delta = null;
      Quaternion initial = _chain.get(i).rotation().get();
      delta = new Quaternion(endLocalPosition, targetLocalPosition);
      //update target local position
      targetLocalPosition = _chain.get(i).reference().displacement(targetLocalPosition, _chain.get(i));
      _chain.get(i).rotate(delta);
      //update end effector local position
      endLocalPosition = _chain.get(i).reference().displacement(endLocalPosition, _chain.get(i));
      initial.compose(_chain.get(i).rotation().get());
      change += Math.abs(initial.angle());
    }
    //Check total rotation change
    if (change <= minDistance) return true;
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
    //TODO needs testing
    _previousTarget = _target == null ? null : _target.detach();
    iterations = 0;
  }
}
