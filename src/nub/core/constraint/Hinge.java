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
 * A Hinge constraint allows 1-DOF rotational motion and by default no translation is
 * allowed (0-DOF), however, translation motion could be modified using an {@link AxisPlaneConstraint}.
 *
 * To define a Hinge constraint you must provide a Reference Quaternion, an Axis of rotation, an orthogonal Vector to this axis
 * (in order to draw the constraint properly @see {@link nub.processing.Scene#drawConstraint(PGraphics, Node)}) and the maximum and
 * minimum angle bounds (between 0 and PI).
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
 * Hence, if the idle rotation corresponds to the initial rotation of a Node to constraint the following equation must be satisfied at
 * the very moment in which the constraint is created:
 *    idle = node.rotation()
 *
 * Furthermore, if a rotation rot is applied to the node rotation the following equations are satisfied:
 *    (1)  node.rotation() * rot = idle * alpha
 *    (2)  idle * alpha * rest = idle * rest * beta
 *    (3)  beta = rest^-1 * alpha * rest
 *    (4)  beta = rest^-1 * idle^-1 * node.rotation() * rot * rest
 *    (5)  beta = orientation^-1 * node.rotation() * rot * rest
 *
 * Equation (5) is required to compare if the rotation applied to the node satisfies the constraint, and if that is not the case
 * a clamping action must be performed.
 */
public class Hinge extends Constraint {
  protected float _max;
  protected float _min;
  protected Quaternion _restRotation = new Quaternion();
  protected Quaternion _idleRotation = new Quaternion();
  protected Quaternion _orientation = new Quaternion();

  protected AxisPlaneConstraint.Type transConstraintType = AxisPlaneConstraint.Type.FORBIDDEN;
  ;
  protected Vector transConstraintDir = new Vector();


  public Hinge(float min, float max) {
    _min = min;
    _max = max;
  }

  public Hinge(float min, float max, Quaternion rotation, Vector up, Vector twist) {
    _min = min;
    _max = max;
    setRestRotation(rotation, up, twist);
  }

  public Quaternion restRotation() {
    return _restRotation;
  }

  public Quaternion idleRotation() {
    return _idleRotation;
  }

  /**
   * Defines the orientational parameters of this constraint:
   * @param reference Is the reference rotation in which the Up and Twist vectors will be defined. Usually this quaternion
   *                  is the same as the (@see {@link Node#rotation()}) to constraint.
   * @param up  Represents a Orthogonal vector to the axis of rotation useful to draw properly a Hinge constraint in a
   *            scene (@see {@link nub.processing.Scene#drawConstraint(PGraphics, Node)}).
   * @param twist Represents the Axis of rotation of this 1-DOF rotational constraint.
   */
  public void setRestRotation(Quaternion reference, Vector up, Vector twist) {
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
    _orientation.compose(delta); // orientation = idle * rest
    _restRotation = delta;
  }

  /**
   * Defines explicitly the reference and rest rotation of this constraint (this method should not be used by the user).
   * @param reference
   * @param rest
   */
  public void setRotations(Quaternion reference, Quaternion rest) {
    _idleRotation = reference.get();
    _restRotation = rest.get();
    _orientation = Quaternion.compose(reference, rest);
  }

  public Quaternion orientation() {
    return _orientation;
  }

  public float maxAngle() {
    return _max;
  }

  public void setMaxAngle(float max) {
    this._max = max;
  }

  public float minAngle() {
    return _min;
  }

  public void setMinAngle(float min) {
    this._min = min;
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    Quaternion desired = Quaternion.compose(node.rotation(), rotation); //w.r.t reference
    desired.normalize();
    desired = Quaternion.compose(_orientation.inverse(), desired);
    desired.normalize();
    desired = Quaternion.compose(desired, _restRotation);
    desired.normalize();

    Vector rotationAxis = new Vector(desired._quaternion[0], desired._quaternion[1], desired._quaternion[2]);
    rotationAxis = Vector.projectVectorOnAxis(rotationAxis, new Vector(0, 0, 1));
    //Get rotation component on Axis direction w.r.t reference
    Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), desired.w());
    float deltaAngle = rotationTwist.angle();
    if (rotationAxis.dot(new Vector(0, 0, 1)) < 0) deltaAngle *= -1;
    float change = deltaAngle;
    if (-_min > change || change > _max) {
      change = change < 0 ? (float) (change + 2 * Math.PI) : change;
      change = change - _max < (float) (-_min + 2 * Math.PI) - change ? _max : -_min;
    }

    //apply constrained rotation
    Quaternion rot = Quaternion.compose(node.rotation().inverse(), _orientation);
    rot.normalize();
    rot.compose(new Quaternion(new Vector(0, 0, 1), change));
    rot.normalize();
    rot.compose(_restRotation.inverse());
    rot.normalize();
    return rot;
  }

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
}
