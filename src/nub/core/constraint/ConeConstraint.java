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

package nub.core.constraint;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

/**
 * A Frame is constrained to disable translation and
 * allow 2-DOF rotation limiting Rotation in a Sphere to
 * laid inside an Ellipse.
 */

//TODO : Add Twist limit & check for unnecessary transformations
public abstract class ConeConstraint extends Constraint {
  protected Quaternion _restRotation = new Quaternion();
  protected Quaternion _idleRotation = new Quaternion();
  protected Quaternion _orientation = new Quaternion();
  protected Quaternion _offset = new Quaternion();
  protected float _min = (float) Math.PI, _max = (float) Math.PI;

  protected AxisPlaneConstraint.Type transConstraintType = AxisPlaneConstraint.Type.FORBIDDEN;;
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
   * reference is a Quaternion that will be aligned to point to the given Basis Vectors
   * result will be stored on restRotation.
   * twist and up axis are defined locally on reference rotation
   */
  public void setRestRotation(Quaternion reference, Vector up, Vector twist, Vector offset) {
    _orientation = reference.get();
    _idleRotation = reference.get();
    //Align z-Axis with twist vector around new up Vector
    Quaternion delta = new Quaternion(new Vector(0, 0, 1), twist);
    Vector tw = delta.inverseRotate(up);
    //Align y-Axis with up vector
    //Assume that up and twist are orthogonal
    float angle = Vector.angleBetween(tw, new Vector(0,1,0));
    if(Vector.cross(new Vector(0,1,0),tw, null).dot(new Vector(0,0,1)) < 0)
      angle *= -1;
    delta.compose(new Quaternion(new Vector(0, 0, 1), angle));
    delta.normalize();
    _orientation.compose(delta); // orientation = idle * rest
    _offset = new Quaternion(twist, offset); // TODO : check offset
    _restRotation = delta;
  }

  public void setRotations(Quaternion reference, Quaternion rest){
    _idleRotation = reference.get();
    _restRotation = rest.get();
    _orientation = Quaternion.compose(reference, rest);
  }


  public void setTwistLimits(float min, float max) {
    _min = min;
    _max = max;
  }

  public float minTwistAngle(){
    return _min;
  }

  public float maxTwistAngle(){
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
    Quaternion delta_rest =  Quaternion.compose(_restRotation.inverse(), delta_idle);
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
}

