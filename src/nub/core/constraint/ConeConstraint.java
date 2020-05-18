/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Sebastian Chaparro Cuevas, https://github.com/VisualComputing
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core.constraint;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PGraphics;

/**
 * A Cone constraint allows 3-DOF rotational motion and by default no translation is
 * allowed (0-DOF), however, translation motion could be modified using an {@link AxisPlaneConstraint}.
 *
 * To define a Cone constraint you must provide a Reference Quaternion, a Twist axis of rotation, an Up Vector orthogonal to this axis
 * (in order to draw the constraint properly @see {@link nub.processing.Scene#drawConstraint(PGraphics, Node)}) and the boundaries of the
 * constraint.
 *
 * As the reference rotation give us the transformation against which any further rotation must be compared, we will call it the
 * idle rotation.
 *
 * The Up and Twist vector defines a unique rotation w.r.t the idle rotation that we named rest rotation i.e. the transformation
 * required to align the Z and Y Axes of the idle rotation with the Twist and Up vector respectively:
 *    Twist = rest^-1 * Z * rest
 *    Up = rest^-1 * Y * rest
 *
 * The composition transformation of the idle rotation and the rest rotation gives the orientation of the constraint:
 *    orientation = idle * rest
 *
 * It is possible to include an Offset rotation in cases in which the initial rotation to compare does not corresponds with the
 * idle rotation.
 *
 * If a rotation rot is applied to the node rotation the following equations are satisfied:
 *    (1)  node.rotation() * rot * offset = idle * alpha
 *    (2)  idle * alpha * rest = idle * rest * beta
 *    (3)  beta = rest^-1 * alpha * rest
 *    (4)  beta = rest^-1 * idle^-1 * node.rotation() * rot * offset * rest
 *    (5)  beta = orientation^-1 * node.rotation() * rot * offset * rest
 *
 * Equation (5) is required to compare if the rotation applied to the node satisfies the constraint, and if that is not the case
 * a clamping action must be performed.
 */

public abstract class ConeConstraint extends Constraint {
  protected Quaternion _restRotation = new Quaternion();
  protected Quaternion _idleRotation = new Quaternion();
  protected Quaternion _orientation = new Quaternion();
  protected Quaternion _offset = new Quaternion();
  protected float _min = (float) Math.PI, _max = (float) Math.PI;

  protected AxisPlaneConstraint.Type transConstraintType = AxisPlaneConstraint.Type.FORBIDDEN;
  ;
  protected Vector transConstraintDir = new Vector();


  public Quaternion restRotation() {
    return _restRotation;
  }

  public Quaternion idleRotation() {
    return _idleRotation;
  }

  public Quaternion offset() {
    return _offset;
  }

  public Quaternion orientation() {
    return _orientation;
  }

  public void setRestRotation(Quaternion restRotation) {
    this._restRotation = restRotation.get();
  }

  /**
   * Defines the orientational parameters of this constraint:
   * @param reference Is the reference rotation in which the Up and Twist vectors will be defined. Usually this quaternion
   *                  is the same as the (@see {@link Node#rotation()}) to constraint.
   * @param up  Represents a Orthogonal vector to the axis of rotation useful to draw properly a Hinge constraint in a
   *            scene (@see {@link nub.processing.Scene#drawConstraint(PGraphics, Node)}).
   * @param twist Represents the Axis of rotation of this 1-DOF rotational constraint.
   */
  public void setRestRotation(Quaternion reference, Vector up, Vector twist, Vector offset) {
    _orientation = reference.get();
    _idleRotation = reference.get();
    //Align z-Axis with twist vector around new up Vector
    Quaternion delta = new Quaternion(new Vector(0, 0, 1), twist);
    Vector tw = delta.inverseRotate(up);
    //Align y-Axis with up vector
    //Assume that up and twist are orthogonal
    float angle = Vector.angleBetween(tw, new Vector(0, 1, 0));
    if (Vector.cross(new Vector(0, 1, 0), tw, null).dot(new Vector(0, 0, 1)) < 0)
      angle *= -1;
    delta.compose(new Quaternion(new Vector(0, 0, 1), angle));
    delta.normalize();
    _orientation.compose(delta); // orientation = idle * rest
    _offset = new Quaternion(twist, offset); // TODO : check offset
    _restRotation = delta;
  }

  public void setRotations(Quaternion reference, Quaternion rest) {
    _idleRotation = reference.get();
    _restRotation = rest.get();
    _orientation = Quaternion.compose(reference, rest);
  }


  public void setTwistLimits(float min, float max) {
    _min = min;
    _max = max;
  }

  public float minTwistAngle() {
    return _min;
  }

  public float maxTwistAngle() {
    return _max;
  }

  public void setRestRotation(Quaternion reference, Vector up, Vector twist) {
    setRestRotation(reference, up, twist, twist);
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    //Define how much idle rotation must change according to this rules:
    //(1) idle * idle_change = node * rotation * offset
    //(2) idle * idle_change * rest_rotation = idle * rest_rotation * rest_change
    //(3) offset is applied w.r.t idle space
    Quaternion delta_idle = Quaternion.compose(_idleRotation.inverse(), node.rotation());
    delta_idle.normalize();
    delta_idle.compose(rotation);
    delta_idle.normalize();
    delta_idle.compose(_offset);
    Quaternion delta_rest = Quaternion.compose(_restRotation.inverse(), delta_idle);
    delta_rest.normalize();
    delta_rest.compose(_restRotation);
    delta_rest.normalize();

    //work w.r.t rest space
    //Decompose delta in terms of twist and swing (twist vector w.r.t rest)
    Vector tw = new Vector(0, 0, 1); // w.r.t idle
    Vector rotationAxis = new Vector(delta_rest._quaternion[0], delta_rest._quaternion[1], delta_rest._quaternion[2]);
    rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t idle
    //Get rotation component on Axis direction
    Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), delta_rest.w()); //w.r.t rest
    Quaternion rotationSwing = Quaternion.compose(delta_rest, rotationTwist.inverse()); //w.r.t rest
    //Constraint swing
    Vector new_pos = rotationSwing.rotate(new Vector(0, 0, 1)); //get twist desired target position
    Vector constrained = apply(new_pos); // constraint target position
    rotationSwing = new Quaternion(tw, constrained); // get constrained swing rotation
    //Constraint twist
    //Find idle twist
    //compare angles
    float twistAngle = rotationTwist.angle();
    if (rotationAxis.dot(tw) < 0) twistAngle = -twistAngle;
    //Check that twist angle is in range [-min, max]
    if ((twistAngle < 0 && -twistAngle > _min) || (twistAngle > 0 && twistAngle > _max)) {

      twistAngle = twistAngle < 0 ? (float) (twistAngle + 2 * Math.PI) : twistAngle;
      twistAngle = twistAngle - _max < (float) (-_min + 2 * Math.PI) - twistAngle ? _max : -_min;
    }
    rotationTwist = new Quaternion(tw, twistAngle); //w.r.t rest

    //constrained change
    Quaternion constrained_change = Quaternion.compose(rotationSwing, rotationTwist);
    //find change in terms of frame rot
    //_idle * constrained_change = frame * rot
    Quaternion rot = Quaternion.compose(node.rotation().inverse(), _idleRotation);
    rot.normalize();
    rot.compose(_restRotation);
    rot.normalize();
    rot.compose(constrained_change);
    rot.normalize();
    rot.compose(_restRotation.inverse());
    rot.normalize();
    rot.compose(_offset.inverse());
    rot.normalize();
    return rot;
  }

  public abstract Vector apply(Vector target);

  @Override
  public Vector constrainTranslation(Vector translation, Node node) {
    Vector res = new Vector(translation._vector[0], translation._vector[1], translation._vector[2]);
    Vector proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        proj = node.rotation().rotate(translationConstraintDirection());
        // proj = node._localInverseTransformOf(translationConstraintDirection());
        res = Vector.projectVectorOnPlane(translation, proj);
        break;
      case AXIS:
        proj = node.rotation().rotate(translationConstraintDirection());
        // proj = node._localInverseTransformOf(translationConstraintDirection());
        res = Vector.projectVectorOnAxis(translation, proj);
        break;
      case FORBIDDEN:
        res = new Vector(0.0f, 0.0f, 0.0f);
        break;
    }
    return res;
  }

  public AxisPlaneConstraint.Type translationConstraintType() {
    return transConstraintType;
  }

  public Vector translationConstraintDirection() {
    return transConstraintDir;
  }

  public void setTranslationConstraint(AxisPlaneConstraint.Type type, Vector direction) {
    setTranslationConstraintType(type);
    setTranslationConstraintDirection(direction);
  }

  public void setTranslationConstraintType(AxisPlaneConstraint.Type type) {
    transConstraintType = type;
  }


  public void setTranslationConstraintDirection(Vector direction) {
    if ((translationConstraintType() != AxisPlaneConstraint.Type.FREE) && (translationConstraintType()
        != AxisPlaneConstraint.Type.FORBIDDEN)) {
      float norm = direction.magnitude();
      if (norm == 0) {
        System.out
            .println("Warning: AxisPlaneConstraint.setTranslationConstraintDir: null vector for translation constraint");
        transConstraintType = AxisPlaneConstraint.Type.FREE;
      } else
        transConstraintDir = Vector.multiply(direction, (1.0f / norm));
    }
  }

  //Convinient methods to apply stereographic projection.
  /*more info at http://www.ams.org/publicoutreach/feature-column/fc-2014-02*/
  protected Vector _stereographicProjection(Vector v){float d = (1 + v.z());
    float X = 2 * v.x() / d;
    float Y = 2 * v.y() / d;
    return new Vector(X, Y, 0);
  }

  protected Vector _inverseStereographicProjection(Vector v){
    float s = 4 / (v.x() * v.x() + v.y() * v.y() + 4);
    return new Vector(s * v.x(), s * v.y(), 2 * s - 1);
  }
}

