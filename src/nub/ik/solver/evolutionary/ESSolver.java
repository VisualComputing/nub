package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 28/10/18.
 */
public class ESSolver extends Solver {
  protected Random random = new Random();
  protected Node _target;
  protected Node _previousTarget;
  protected List<? extends Node> _chain, _x_i;
  protected boolean _powerLaw;
  protected double _sigma;
  protected double _alpha = 2;
  protected PrintWriter _printWriter;

  public ESSolver(double sigma, List<? extends Node> chain) {
    this._powerLaw = false;
    this._sigma = sigma;
    this._chain = chain;
    _x_i = _copy(chain);
  }

  public ESSolver(double alpha, double sigma, List<? extends Node> chain) {
    this._powerLaw = true;
    this._alpha = alpha;
    this._sigma = sigma;
    this._chain = chain;
    _x_i = _copy(chain);
  }

  public double sigma() {
    return _sigma;
  }

  public double alpha() {
    return _alpha;
  }

  public boolean powerLaw() {
    return _powerLaw;
  }

  public double[] execute() {
    double[] results = new double[_maxIterations];
    int k = 0;
    _x_i = _copy(_chain);
    while (k < _maxIterations) {
      _iterate();
      results[k] = _distanceToTarget(_x_i);
      k++;
    }
    return results;
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

  protected double _distanceToTarget(List<? extends Node> chain) {
    return Vector.distance(chain.get(chain.size() - 1).position(), _target.position());
  }

  protected double _powerLawGenerator(double x, double alpha) {
    double coarse_alpha = 1.0 / (1.0 - alpha);
    return Math.pow(1.0 - x, coarse_alpha);
  }

  protected ArrayList<Node> _copy(List<? extends Node> chain) {
    ArrayList<Node> copy = new ArrayList<Node>();
    Node reference = chain.get(0).reference();
    if (reference != null) {
      reference = new Node(reference.position().get(), reference.orientation().get(), 1);
    }
    for (Node joint : chain) {
      Node newJoint = new Node();
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }


  @Override
  protected boolean _iterate() {
    ArrayList<Node> x_i1 = _copy(_x_i);
    for (int i = 0; i < _x_i.size(); i++) {
      int invert = random.nextDouble() >= 0.5 ? 1 : -1;
      //rotate
      float roll;
      float pitch;
      float yaw;
      if (_powerLaw) {
        roll = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha) * _sigma);
        pitch = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha) * _sigma);
        yaw = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha) * _sigma);
      } else {
        roll = (float) (random.nextGaussian() * _sigma);
        pitch = (float) (random.nextGaussian() * _sigma);
        yaw = (float) (random.nextGaussian() * _sigma);
      }
      //rotate method consider constraints
      x_i1.get(i).rotate(new Quaternion(roll, pitch, yaw));
    }

    double d1 = _distanceToTarget(x_i1), d2 = _distanceToTarget(_x_i);
    if (d1 < d2) {
      _x_i = x_i1;
      d1 = d2;
    }
    return d1 < _minDistance;
  }


  public Node head() {
    return _chain.get(0);
  }

  public Node endEffector() {
    return _chain.get(_chain.size() - 1);
  }

  @Override
  protected void _update() {
    for (int i = 0; i < _chain.size(); i++) {
      _chain.get(i).setRotation(_x_i.get(i).rotation().get());
    }
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
  }

  @Override
  public float error() {
    return Vector.distance(_target.position(), _chain.get(_chain.size() - 1).position());
  }
}

