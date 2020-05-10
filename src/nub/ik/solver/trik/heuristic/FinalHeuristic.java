package nub.ik.solver.trik.heuristic;

import javafx.util.Pair;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.NodeState;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.HashMap;

public class FinalHeuristic extends Heuristic {
  /**
   * The idea of this heuristics is to apply and interchange popular CCD Step along with Triangulation step. Here most of the work is done by the first joints,
   * hence the obtained solution could not seem natural when working with unconstrained chains. For this purposes a smoothing stage is required in which each
   * joint will try to do and delegate work.
   */

  HashMap<String, Pair<Vector, Vector>> vectors = new HashMap<>();
  protected int _smoothingIterations = 30;
  protected Quaternion[] _initialRotations;
  protected boolean _checkHinge = true;

  protected boolean _enableWeights = false;
  protected float maxAngle = 111f * (float) Math.toRadians(40);


  public void checkHinge(boolean check) {
    _checkHinge = check;
  }

  public FinalHeuristic(Context context) {
    super(context);
    _initialRotations = new Quaternion[_context.chain().size()];
    _log = false;
  }


  @Override
  public void prepare() {
    //Update cache of usable chain
    NodeInformation._updateCache(_context.usableChainInformation());
    for (int i = 0; i < _initialRotations.length; i++) {
      _initialRotations[i] = _context.usableChainInformation().get(i).node().rotation().get();
    }
  }

  protected boolean _log;
  protected int _times = 2;

  @Override
  public void applyActions(int i) {
    if (_context.debug()) vectors.clear();
    Vector target = _context.worldTarget().position();
    if (_smooth || _context.enableDelegation()) {
      _context.usableChainInformation().get(i + 1).updateCacheUsingReference();
      int currentIteration = _context._currentIteration();
      if (_context.singleStep()) {
        currentIteration = currentIteration / _context.chain().size();
      }

      if (_log) {
        System.out.println("t : " + target + "mag" + target.magnitude());
      }

      //FABRIK - LIKE HEURISTIC
      if (currentIteration < _smoothingIterations) {
        Vector eff_to_j_i1 = Vector.subtract(_context.endEffectorInformation().positionCache(), _context.usableChainInformation().get(i + 1).positionCache());
        Vector j_i1_proj = Vector.subtract(_context.worldTarget().position(), eff_to_j_i1);
        Vector j_i_proj = Vector.subtract(j_i1_proj, _context.usableChainInformation().get(i).positionCache());
        j_i_proj.normalize();
        j_i_proj.multiply(Vector.distance(_context.usableChainInformation().get(i).positionCache(), _context.usableChainInformation().get(i + 1).positionCache()));
        j_i_proj.add(_context.usableChainInformation().get(i).positionCache());
        target = Vector.add(j_i_proj, eff_to_j_i1);
      }

      if (_log) {
        System.out.println(" t : " + target + "mag" + target.magnitude());
        System.out.println("smooth t : " + target + "mag" + target.magnitude());
        System.out.println("J_i pos : " + _context.usableChainInformation().get(i).positionCache() + " vs " + _context.usableChainInformation().get(i).node().position());
        System.out.println("J_i1 pos : " + _context.usableChainInformation().get(i + 1).positionCache() + " vs " + _context.usableChainInformation().get(i + 1).node().position());
      }

      if (_log) {
        System.out.println("smooth t : " + target + "mag" + target.magnitude());
      }

      if (_context.debug()) {
        vectors.put("st ", new Pair<>(target, _context.endEffectorInformation().positionCache()));
        vectors.put("t_des ", new Pair<>(_context.usableChainInformation().get(0).positionCache(), target));
      }
    }

    NodeInformation j_i = _context.usableChainInformation().get(i);
    Vector eff_wrt_j_i = j_i.locationWithCache(_context.endEffectorInformation().positionCache());
    Vector target_wrt_j_i = j_i.locationWithCache(target);

    if (_log) {
      System.out.println("*-*-*-*-*-*-*-*-*-*");
      System.out.println("On i : " + i);
    }

    if (i == _context.last() - 1) {
      Quaternion q_i = applyCCD(i, j_i, eff_wrt_j_i, target_wrt_j_i, true);
      j_i.rotateAndUpdateCache(q_i, false, _context.endEffectorInformation()); //Apply local rotation
      if (_context.direction()) {
        float max_dist = _context.searchingAreaRadius();
        float radius = Vector.distance(_context.endEffectorInformation().positionCache(), j_i.positionCache());
        //find max theta allowed
        float max_theta = (float) Math.acos(Math.max(Math.min(1 - (max_dist * max_dist) / (2 * radius * radius), 1), -1));
        j_i.rotateAndUpdateCache(applyOrientationalCCD(i, max_theta), false, _context.endEffectorInformation());
      }
      return;
    }

    NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
    j_i1.updateCacheUsingReference();
    Vector eff_wrt_j_i1 = j_i1.locationWithCache(_context.endEffectorInformation().positionCache());
    Vector target_wrt_j_i1 = j_i1.locationWithCache(target);
    //Find the two solutions of the triangulation problem on joint j_i1
    Solution[] solutions;
    solutions = applyTriangulation(i, j_i, j_i1, eff_wrt_j_i1, target_wrt_j_i1, _checkHinge);

    //Keep original State of J_i and J_i1
    NodeInformation endEffector = _context.endEffectorInformation();

    NodeState initial_j_i = new NodeState(j_i);
    NodeState initial_j_i1 = new NodeState(j_i1);
    NodeState initial_eff = new NodeState(endEffector);

    NodeState[] final_j_i = new NodeState[solutions.length];
    NodeState[] final_j_i1 = new NodeState[solutions.length];
    NodeState[] final_eff = new NodeState[solutions.length];

    int best = 0; // keep track of best solution
    float best_dist = Float.MAX_VALUE, best_angle = Float.MAX_VALUE;
    for (int s = 0; s < solutions.length; s++) {
      float a; //amount or rotation applied
      j_i1.updateCacheUsingReference();
      //Apply solution find by triangulation
      Quaternion q;
      q = solutions[s].quaternion();
      j_i1.rotateAndUpdateCache(q, false, endEffector);
      //Apply CCD t times (best local action if joint rotation constraints are quite different)
      j_i.rotateAndUpdateCache(applyCCD(i, j_i, j_i.locationWithCache(endEffector.positionCache()), j_i.locationWithCache(target), true), true, endEffector);
      j_i1.updateCacheUsingReference();

      for (int t = 0; t < _times; t++) {
        j_i1.updateCacheUsingReference();
        Quaternion q_i1 = applyCCD(i + 1, j_i1, j_i1.locationWithCache(endEffector.positionCache()), j_i1.locationWithCache(target), true);
        j_i1.rotateAndUpdateCache(q_i1, false, endEffector);
        Quaternion q_i = applyCCD(i, j_i, j_i.locationWithCache(endEffector.positionCache()), j_i.locationWithCache(target), true);
        j_i.rotateAndUpdateCache(q_i, false, endEffector);
      }
      j_i1.updateCacheUsingReference();

      if (_context.debug()) {
        vectors.put("zi " + (s + 1), new Pair<>(j_i.positionCache().get(), j_i1.positionCache().get()));
        vectors.put("zf " + (s + 1), new Pair<>(j_i1.positionCache().get(), endEffector.positionCache().get()));
      }

      if (_context.direction()) {
        float max_dist = _context.searchingAreaRadius();
        float radius = Vector.distance(endEffector.positionCache(), j_i1.positionCache());
        //find max theta allowed
        float max_theta = (float) Math.acos(Math.max(Math.min(1 - (max_dist * max_dist) / (2 * radius * radius), 1), -1));
        j_i1.rotateAndUpdateCache(applyOrientationalCCD(i + 1, max_theta), false, endEffector);
      }

      //store state in final vector
      final_j_i[s] = new NodeState(j_i);
      final_j_i1[s] = new NodeState(j_i1);
      final_eff[s] = new NodeState(endEffector);

      a = Math.abs(_context.quaternionDistance(initial_j_i.rotation(), j_i.node().rotation()) + _context.quaternionDistance(initial_j_i1.rotation(), j_i1.node().rotation()));
      //a = Math.abs(_context.quaternionDistance(initial_j_i.rotation(), j_i.node().rotation()));
      //a = Math.abs(_context.quaternionDistance(initial_j_i1.orientation(), j_i1.node().orientation()));
      //a += 0.5f * Math.abs(_context.quaternionDistance(_initialRotations[i], j_i.node().rotation()) - _context.quaternionDistance(_initialRotations[i + 1], j_i1.node().rotation()));
      if (_log) {
        System.out.println("---> a : " + a);
        System.out.println("initial j_i :" + _initialRotations[i].axis() + "a " + _initialRotations[i].angle() + "final " + final_j_i[s].rotation().axis() + " a " + final_j_i[s].rotation().angle());
        System.out.println("initial j_i :" + _initialRotations[i]._quaternion[0] + " , " + _initialRotations[i]._quaternion[1] + " , " + _initialRotations[i]._quaternion[2] + " , " + _initialRotations[i]._quaternion[3]);

        System.out.println("initial j_i1 :" + _initialRotations[i + 1].axis() + "a " + _initialRotations[i + 1].angle() + "final " + final_j_i1[s].rotation().axis() + " a " + final_j_i1[s].rotation().angle());

        System.out.println("---> sol : " + (s + 1) + "work by angle 1 " + _context.quaternionDistance(_initialRotations[i], final_j_i[s].rotation()));
        System.out.println("---> sol : " + (s + 1) + "work by angle 2 " + _context.quaternionDistance(_initialRotations[i + 1], final_j_i1[s].rotation()));
      }

      float dist;
      if (!_context.enableDelegation() && !_enableWeights) {
        dist = _context.error(endEffector, _context.worldTarget());
        dist /= _context.searchingAreaRadius();
      } else {
        float error = _context.positionError(endEffector.positionCache(), target);
        if (_context.direction()) {
          float orientationError = _context.orientationError(endEffector.orientationCache(), _context.worldTarget().orientation(), false);
          float weighted_error = error / _context.searchingAreaRadius();
          float c_k = (float) Math.floor(weighted_error);
          error = c_k + _context.orientationWeight() * orientationError + (1 - _context.orientationWeight()) * (weighted_error - c_k);
        }
        dist = error;

        //Handiness
        if (i + 3 < _context.usableChainInformation().size()) {
          NodeInformation j_i2 = _context.usableChainInformation().get(i + 2);
          NodeInformation j_i3 = _context.usableChainInformation().get(i + 3);
          Vector b1 = Vector.subtract(j_i1.node().position(), j_i.node().position());
          Vector b2 = Vector.subtract(j_i2.node().position(), j_i1.node().position());
          Vector b3 = Vector.subtract(j_i3.node().position(), j_i2.node().position());
          b1.normalize();
          b2.normalize();
          b3.normalize();

          Vector c1 = Vector.cross(b1, b2, null);
          Vector c2 = Vector.cross(b2, b3, null);
          if (c1.magnitude() > 0.1f || c2.magnitude() > 0.1f) {
            //System.out.println("Handiness : " + c1 + " " + c2);
            //System.out.println("Handiness : " + c1.magnitude() + " " + c2.magnitude());

            //System.out.println("---> dist : " + dist);
            if (Vector.dot(c1, c2) < 0) {
              //dist += 1f;
              //System.out.println("---> new dist : " + dist);
            }
          }
        }
      }

      if (_log) {
        System.out.println("---> dist : " + dist);
        System.out.println("---> constraint error : " + solutions[s].value());
      }

      dist = dist + 0.1f * a;// + solutions[s].value();// + length_distance * _lengthWeight;
      if (dist < best_dist) {
        best_dist = dist;
        best = s;
      }

      //reset state
      j_i.setCache(initial_j_i.position().get(), initial_j_i.orientation().get());
      j_i.node().setRotation(initial_j_i.rotation().get());
      j_i1.setCache(initial_j_i1.position().get(), initial_j_i1.orientation().get());
      j_i1.node().setRotation(initial_j_i1.rotation().get());
      endEffector.setCache(initial_eff.position().get(), initial_eff.orientation().get());

      if (_context.debug()) {
        vectors.put("sol " + (s + 1), new Pair<>(final_j_i[s].position().get(), final_j_i1[s].position().get()));
        vectors.put("sol " + (s + 1) + " d : " + String.format("%.3f", dist), new Pair<>(final_j_i1[s].position().get(), final_eff[s].position().get()));
      }
    }

    if (_log) {
      System.out.println("best i :" + final_j_i[best].rotation().axis() + " a : " + final_j_i[best].rotation().angle());
      System.out.println("best i + 1 :" + final_j_i1[best].rotation().axis() + " a : " + final_j_i1[best].rotation().angle());
      System.out.println("best i :" + j_i.node().rotation().axis() + " a : " + j_i.node().rotation().angle());
      System.out.println("best i + 1 :" + j_i1.node().rotation().axis() + " a : " + j_i1.node().rotation().angle());
    }

    //Apply best solution
    j_i.setCache(final_j_i[best].position().get(), final_j_i[best].orientation().get());
    Constraint c_i = j_i.node().constraint();
    j_i.node().setConstraint(null);
    j_i.node().setRotation(final_j_i[best].rotation().get());
    j_i.node().setConstraint(c_i);
    j_i1.setCache(final_j_i1[best].position().get(), final_j_i1[best].orientation().get());
    Constraint c_i1 = j_i1.node().constraint();
    j_i1.node().setConstraint(null);
    j_i1.node().setRotation(final_j_i1[best].rotation().get());
    j_i1.node().setConstraint(c_i1);
    endEffector.setCache(final_eff[best].position().get(), final_eff[best].orientation().get());
  }


  protected class Solution {
    protected Quaternion _quaternion;
    protected float _value;

    protected Solution(Quaternion q, float v) {
      _quaternion = q;
      _value = v;
    }

    protected void setQuaternion(Quaternion q) {
      _quaternion = q;
    }

    protected void setValue(float v) {
      _value = v;
    }

    protected Quaternion quaternion() {
      return _quaternion;
    }

    protected float value() {
      return _value;
    }
  }

  protected Solution[] applyTriangulation(int i, NodeInformation j_i, NodeInformation j_i1, Vector endEffector, Vector target, boolean checkHinge) {
    //checkHinge = true;
    Hinge h_i1 = null;
    Vector v_i = j_i1.locationWithCache(j_i.positionCache());
    Vector normal;
    //In this case we apply triangulation over j_i1
    if (j_i1.node().constraint() instanceof Hinge) {
      //Project endEffector to lie on the plane defined by the axis of rotation
      h_i1 = ((Hinge) j_i1.node().constraint());
      //1. find rotation axis
      normal = h_i1.orientation().rotate(new Vector(0, 0, 1));
      if (_log) System.out.println("normal " + normal);
      normal = j_i1.node().rotation().inverse().rotate(normal);
      normal.normalize();
      //2. project target and end effector
      v_i = Vector.projectVectorOnPlane(v_i, normal);
      endEffector = Vector.projectVectorOnPlane(endEffector, normal);
      target = Vector.projectVectorOnPlane(target, normal);
    } else {
      normal = Vector.cross(endEffector, target, null);
      if (normal.squaredNorm() < 0.0001f) {
        normal = Vector.cross(target, v_i, null);
      }
      //pick any vector if all are collinear
      if (normal.squaredNorm() < 0.0001f) {
        normal = target.orthogonalVector();
      }
      normal.normalize();
    }
    //Find the two solutions of the triangulation problem assuming no constraints
    Vector a = v_i;
    Vector a_neg = Vector.multiply(a, -1);
    Vector b = endEffector;
    Vector c = Vector.subtract(target, a);
    float a_mag = a.magnitude(), b_mag = b.magnitude(), c_mag = c.magnitude();

    if (_log) {
      System.out.println("Vec a : " + a + "mag" + a.magnitude());
      System.out.println("Vec b : " + b + "mag" + b.magnitude());
      System.out.println("Vec c : " + c + "mag" + c.magnitude());
    }

    float angle = Math.min(Math.max(Vector.dot(a, b) / (a_mag * b_mag), -1), 1);
    angle = (float) (Math.acos(angle));
    if (_log) {
      System.out.println("dot a ,b  : " + Vector.dot(a, b));
      System.out.println("a mag * b mag : " + (a_mag * b_mag));
      System.out.println("result : " + Vector.dot(a, b) / (a_mag * b_mag));
      System.out.println("angle : " + Math.toDegrees(angle));
      System.out.println("cross : " + Vector.cross(b, a_neg, null));
      System.out.println("Normal : " + normal);
      System.out.println("Dot : " + Vector.dot(Vector.cross(b, a_neg, null), normal));

    }

    float angle_1, angle_2;

    if (Vector.dot(Vector.cross(b, a_neg, null), normal) < 0) {
      angle = -angle;
    }

    //Find limits centered at angle
    float max = (float) Math.PI;
    float min = -max;
    if (_log) {
      System.out.println("--- max limit : " + Math.toDegrees(max));
      System.out.println("--- min limit : " + Math.toDegrees(min));
    }

    if (a_mag + b_mag <= c_mag) {
      if (_log) System.out.println("---case1 : extend chain!!! ");
      //Chain must be extended as much as possible
      if (_log) {
        System.out.println("current : " + (Math.toDegrees(angle)));
        System.out.println("expected : " + (Math.toDegrees(Math.PI)));
      }

      if (angle == 0) {
        angle_1 = (float) (Math.PI);
        angle_2 = -(float) (Math.PI);
      } else {
        angle_1 = Math.signum(angle) * (float) (Math.PI) - angle;
        angle_2 = (float) (-Math.signum(angle_1) * 2 * Math.PI + angle_1);
      }
    } else if (c_mag < Math.abs(a_mag - b_mag)) {
      if (_log) System.out.println("---case2 : contract chain!!! on node : ");
      //Chain must be contracted as much as possible
      if (_log) {
        System.out.println("current : " + (Math.toDegrees(angle)));
        System.out.println("expected : " + 0);
      }

      angle_1 = -angle;
      angle_2 = (float) (-Math.signum(angle_1) * 2 * Math.PI + angle_1);
    } else {
      //Apply law of cosines
      float current = angle;

      float expected = Math.min(Math.max(-(c_mag * c_mag - a_mag * a_mag - b_mag * b_mag) / (2f * a_mag * b_mag), -1), 1);
      expected = (float) (Math.acos(expected));

      if (_log) {
        System.out.println("current : " + Math.toDegrees(current));
        System.out.println("expected : " + Math.toDegrees(expected));
      }

      float sign = current != 0 ? Math.signum(current) : 1;
      angle_1 = sign * expected - current;
      if (Math.abs(current) < Math.abs(expected)) {
        angle_2 = current - Math.signum(current) * expected;
      } else {
        angle_2 = sign * (float) (2 * Math.PI - expected) - current;
      }

      if (_log) {
        System.out.println("--> angle 1 : " + Math.toDegrees(angle_1));
        System.out.println("--> angle 2: " + Math.toDegrees(angle_2));
      }
    }
    if (_log) {
      System.out.println("--> angle 1 : " + Math.toDegrees(angle_1));
      System.out.println("--> angle 2 : " + Math.toDegrees(angle_2));
    }

    //Constraint the angles according to the joint limits (prefer the solution with least damping)
    float constrained_angle_1 = Math.min(max, Math.max(min, angle_1));
    float constrained_angle_2 = Math.min(max, Math.max(min, angle_2));
    ;

    if (_log) {
      System.out.println("--> constrained angle 1 : " + Math.toDegrees(constrained_angle_1));
      System.out.println("--> constrained angle 2 : " + Math.toDegrees(constrained_angle_2));
      System.out.println("--> ratio angle 1 : " + (Math.abs(constrained_angle_1) + 1) / (Math.abs(angle_1) + 1));
      System.out.println("--> ration angle 2 : " + (Math.abs(constrained_angle_2) + 1) / (Math.abs(angle_2) + 1));

    }

    Solution[] deltas = new Solution[2];
    deltas[0] = new Solution(new Quaternion(normal, constrained_angle_1), 1 - (Math.abs(constrained_angle_1) + 1) / (Math.abs(angle_1) + 1));
    deltas[1] = new Solution(new Quaternion(normal, constrained_angle_2), 1 - (Math.abs(constrained_angle_2) + 1) / (Math.abs(angle_2) + 1));
    for (Solution delta : deltas) {
      if (_enableWeights) {
        //smooth angle
        delta.setQuaternion(new Quaternion(delta.quaternion().axis(), delta.quaternion().angle() * _context.delegationAtJoint(i + 1)));
        //clamp rotation if required
        delta.setQuaternion(_clampRotation(j_i1.node().rotation(), _initialRotations[i + 1], Quaternion.compose(j_i1.node().rotation(), delta.quaternion()), maxAngle));
      }
      if (j_i1.node().constraint() != null) {
        delta.setQuaternion(j_i1.node().constraint().constrainRotation(delta.quaternion(), j_i1.node()));
      }
      if (_log) {
        System.out.println("--> delta : " + delta.quaternion().axis() + " angle : " + Math.toDegrees(delta.quaternion().angle()));
      }
      delta.quaternion().normalize();
    }
    return deltas;
  }

  //Try to approach to target final position by means of twisting
  protected Quaternion applyCCDTwist(NodeInformation j_i, NodeInformation j_i1, Vector endEffector, Vector target, float maxAngle) {
    Vector j_i_to_eff_proj, j_i_to_target_proj;
    Vector tw = j_i1.node().translation(); // w.r.t j_i
    //Project the given vectors in the plane given by twist axis
    try {
      j_i_to_target_proj = Vector.projectVectorOnPlane(target, tw);
      j_i_to_eff_proj = Vector.projectVectorOnPlane(endEffector, tw);
    } catch (Exception e) {
      return new Quaternion(tw, 0);
    }

    //Perform this operation only when Projected Vectors have not a despicable length
    if (j_i_to_target_proj.magnitude() < 0.3 * target.magnitude() && j_i_to_eff_proj.magnitude() < 0.3 * endEffector.magnitude()) {
      return new Quaternion(tw, 0);
    }

    //Find the angle between projected vectors
    float angle = Vector.angleBetween(j_i_to_eff_proj, j_i_to_target_proj);
    //clamp angle
    angle = Math.min(angle, maxAngle);
    if (Vector.cross(j_i_to_eff_proj, j_i_to_target_proj, null).dot(tw) < 0)
      angle *= -1;

    Quaternion twist = new Quaternion(tw, angle);
    if (_smooth) {
      twist = _clampRotation(twist, _smoothAngle);
    }
    twist.normalize();
    return twist;
  }


  protected Quaternion applyCCD(int i, NodeInformation j_i, Vector endEffector, Vector target, boolean checkHinge) {
    Vector p = endEffector;
    Vector q = target;
    if (checkHinge && j_i.node().constraint() != null && j_i.node().constraint() instanceof Hinge) {
      Hinge h = (Hinge) j_i.node().constraint();
      Quaternion quat = Quaternion.compose(j_i.node().rotation().inverse(), h.idleRotation());
      Vector tw = h.restRotation().rotate(new Vector(0, 0, 1));
      tw = quat.rotate(tw);
      //Project b & c on the plane of rot
      p = Vector.projectVectorOnPlane(p, tw);
      q = Vector.projectVectorOnPlane(q, tw);
    }
    //Apply desired rotation removing twist component
    Quaternion delta = new Quaternion(p, q);
    if (_enableWeights) {
      //smooth angle
      delta = new Quaternion(delta.axis(), delta.angle() * _context.delegationAtJoint(i));
      delta = _clampRotation(j_i.node().rotation(), _initialRotations[i], Quaternion.compose(j_i.node().rotation(), delta), maxAngle);
    }
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
    }
    delta.normalize();
    return delta;
  }

  protected Quaternion applyOrientationalCCD(int i, float maxAngle) {
    NodeInformation j_i = _context.usableChainInformation().get(i);
    Quaternion O_i = j_i.orientationCache();
    Quaternion O_i_inv = O_i.inverse();
    Quaternion O_eff = _context.usableChainInformation().get(_context.last()).orientationCache();
    Quaternion target = _context.worldTarget().orientation();
    Quaternion O_i1_to_eff = Quaternion.compose(O_i.inverse(), O_eff);
    O_i1_to_eff.normalize();
    Quaternion delta = Quaternion.compose(O_i_inv, target);
    delta.normalize();
    delta.compose(O_i1_to_eff.inverse());
    delta.normalize();
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
      delta.normalize();
    }
    //clamp rotation
    delta = _clampRotation(delta, maxAngle);
    return delta;
  }


  @Override
  public NodeInformation[] nodesToModify(int i) {
    //return new NodeInformation[]{_context.usableChainInformation().get(i - 1), _context.usableChainInformation().get(i)};
    return null;
  }

  protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
    float angle = rotation.angle();
    float angleVal = Math.abs(angle);
    float angleSign = Math.signum(angle);
    Vector axis = rotation.axis();
    if (Math.abs(angle) > Math.PI) {
      axis.multiply(-1);
      angle = angleSign * (float) (2 * Math.PI - angleVal);
    }
    if (Math.abs(angle) > maxAngle) {
      rotation = new Quaternion(axis, angleSign * maxAngle);
    }
    return rotation;
  }

  protected static Quaternion _clampRotation(Quaternion q_cur, Quaternion q_i, Quaternion q_f, float maxAngle) {
    Quaternion diff = Quaternion.compose(q_i.inverse(), q_f);
    diff.normalize();

    float angle = diff.angle();
    float angleVal = Math.abs(angle);
    float angleSign = Math.signum(angle);
    Vector axis = diff.axis();

    if (Math.abs(angle) > Math.PI) {
      axis.multiply(-1);
      angle = angleSign * (float) (2 * Math.PI - angleVal);
    }

    if (Math.abs(angle) > maxAngle) {
      diff = new Quaternion(axis, angleSign * maxAngle);
    }

    Quaternion delta = Quaternion.compose(q_cur.inverse(), q_i);
    delta.compose(diff);
    return delta;
  }


  public void drawVectors(Scene scene) {
    if (!_context.debug()) return;
    PGraphics pg = scene.context();
    int i = 0;
    for (String key : vectors.keySet()) {
      Pair<Vector, Vector> pair = vectors.get(key);
      if (key.equals("normal")) {
        Vector n = Vector.subtract(pair.getValue(), pair.getKey());
        Vector a1 = Vector.orthogonalVector(n);
        a1.normalize();
        a1.multiply(n.magnitude() * 9);
        Vector a2 = Vector.cross(n, a1, null);
        a2.normalize();
        a2.multiply(n.magnitude() * 8);
        pg.fill(255, 255, 255, 50);
        pg.noStroke();
        pg.beginShape();
        pg.vertex(a1.x() + a2.x() + pair.getKey().x(), a1.y() + a2.y() + pair.getKey().y(), a1.z() + a2.z() + pair.getKey().z());
        pg.vertex(a1.x() - a2.x() + pair.getKey().x(), a1.y() - a2.y() + pair.getKey().y(), a1.z() - a2.z() + pair.getKey().z());
        pg.vertex(-a1.x() - a2.x() + pair.getKey().x(), -a1.y() - a2.y() + pair.getKey().y(), -a1.z() - a2.z() + pair.getKey().z());
        pg.vertex(-a1.x() + a2.x() + pair.getKey().x(), -a1.y() + a2.y() + pair.getKey().y(), -a1.z() + a2.z() + pair.getKey().z());
        pg.endShape();
      }


      Vector t = scene.screenLocation(pair.getValue());
      pg.noStroke();
      pg.fill(scene.pApplet().noise(1000 + 10 * i) * 255, scene.pApplet().noise(80 + 10 * i) * 255, scene.pApplet().noise(235 + 10 * i) * 255);
      if (Vector.distance(pair.getKey(), pair.getValue()) > 1) {
        scene.drawArrow(pair.getKey(), pair.getValue(), 1.5f);
      }
      pg.noLights();
      scene.beginHUD();
      pg.fill(255, 255, 255);
      pg.text(key, t.x(), t.y());
      scene.endHUD();
      pg.lights();
      i++;

    }
  }

  public void drawPositionContourMap(Scene scene) {
    PGraphics pg = scene.context();
    //Draw as much contours as the number of bones of the structure
    for (int i = 1; i < 5; i++) {
      float r = _context.searchingAreaRadius() * i;
      pg.pushStyle();
      pg.noStroke();
      pg.noLights();

      pg.pushMatrix();
      scene.applyWorldTransformation(_context.worldTarget());
      pg.fill(255, 0, 0, PApplet.map(i, 5, 1, 100, 30));
      pg.sphere(r);
      pg.popMatrix();
      pg.popStyle();
    }
  }
}
