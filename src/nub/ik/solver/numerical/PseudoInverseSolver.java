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

package nub.ik.solver.numerical;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.primitives.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;

public class PseudoInverseSolver extends Solver {
  //look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/iksurvey.pdf
  protected List<? extends Node> _chain;
  protected Node _target;
  protected Node _previousTarget;
  protected SimpleMatrix _J;
  protected Vector[] _axes;
  protected SimpleMatrix _delta;
  protected float _d_max;

  public PseudoInverseSolver(List<? extends Node> chain) {
    super();
    this._chain = chain;
    for (Node f : _chain) {
      float d = Vector.distance(f.position(), f.reference() != null ? f.reference().position() : new Vector(0, 0, 0));
      _d_max = _d_max < d ? d : _d_max;
    }
    _axes = new Vector[_chain.size() - 1];
  }

  public PseudoInverseSolver(ArrayList<? extends Node> chain, Node target) {
    super();
    this._chain = chain;
    this._target = target;
    this._previousTarget =
        target == null ? null : Node.detach(target.position().get(), target.orientation().get(), 1);
    _axes = new Vector[_chain.size() - 1];
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

  @Override
  protected boolean _iterate() {
    //As no target is specified there is no need to perform an iteration
    if (_target == null || _chain.size() < 2) return true;
    //Clamp _maxError
    Vector e = Vector.subtract(_target.position(), endEffector().position());
    if (e.magnitude() > _d_max) {
      e.normalize();
      e.multiply(_d_max);
    }

    SimpleMatrix error = SimpleMatrix.wrap(
        Util.vectorToMatrix(e, true));

    _J = SimpleMatrix.wrap(Util.jacobian(_chain, endEffector(), _target.position(), _axes));
    _delta = SimpleMatrix.wrap(Util.solvePseudoinverse(_J.getDDRM(), error.getDDRM()));
    double max = 0;
    for (int i = 0; i < _delta.numRows(); i++) {
      max = Math.abs(_delta.get(i, 0)) > max ? Math.abs(_delta.get(i, 0)) : max;
    }
    if (max > Math.toRadians(10))
      _delta = _delta.scale(Math.toRadians(10) / max); //TODO: check for a better scaling value

    Util.updateChain(_chain, _delta, _axes);
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(endEffector().position(), _target.position()) <= super._maxError) {
      return true;
    }
    //Check total rotation change
    //if (change <= _minDistance) return true;
    return false;
  }

  //Update must be done at each iteration step (see line 83)
  @Override
  protected void _update() {
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
    _previousTarget = _target == null ? null : Node.detach(_target.position().get(), _target.orientation().get(), 1);
    _axes = new Vector[_chain.size() - 1];
    _iterations = 0;
  }

  @Override
  public float error() {
    return Vector.distance(_target.position(), _chain.get(_chain.size() - 1).position());
  }
}