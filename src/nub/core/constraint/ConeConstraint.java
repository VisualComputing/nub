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
    //Align y-Axis with up vector
    Quaternion delta = new Quaternion(new Vector(0, 1, 0), up);
    Vector tw = delta.inverseRotate(twist);
    //Align z-Axis with twist vector
    delta.compose(new Quaternion(new Vector(0, 0, 1), tw));
    _orientation.compose(delta); // orientation = idle * rest
    _offset = new Quaternion(twist, offset); // TODO : check offset
    _restRotation = delta;
  }

  public void setTwistLimits(float min, float max) {
    _min = min;
    _max = max;
  }


  public void setRestRotation(Quaternion reference, Vector up, Vector twist) {
    setRestRotation(reference, up, twist, twist);
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    //Define how much idle rotation must change : idle * change = frame * rot
    //Identify rotation that must be applied to move twist and up to its  new values
    Quaternion curr = _idleRotation; //w.r.t ref
    Quaternion next = Quaternion.compose(node.rotation(), rotation); // w.r.t ref
    Quaternion change = Quaternion.compose(curr.inverse(), next); // w.r.t idle
    change = Quaternion.compose(change, _offset); // w.r.t idle
    //Decompose change in terms of twist and swing (twist vector w.r.t idle)
    Vector tw = _restRotation.rotate(new Vector(0, 0, 1)); // w.r.t idle
    Vector rotationAxis = new Vector(change._quaternion[0], change._quaternion[1], change._quaternion[2]);
    rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t idle
    //Get rotation component on Axis direction
    Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), change.w()); //w.r.t idle
    Quaternion rotationSwing = Quaternion.compose(change, rotationTwist.inverse()); //w.r.t idle
    //Constraint swing
    Vector new_pos = rotationSwing.rotate(_restRotation.rotate(new Vector(0, 0, 1))); //get twist desired target position
    Vector constrained = apply(new_pos, _restRotation); // constraint target position
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
    rotationTwist = new Quaternion(tw, twistAngle); //w.r.t idle

    //constrained change
    Quaternion constrained_change = Quaternion.compose(rotationSwing, rotationTwist);
    //find change in terms of frame rot
    //_idle * constrained_change = frame * rot
    Quaternion rot = Quaternion.compose(node.rotation().inverse(), Quaternion.compose(_idleRotation, Quaternion.compose(constrained_change, _offset.inverse())));
    return rot;
  }

  @Override
  public Vector constrainTranslation(Vector translation, Node node) {
    return new Vector(0, 0, 0);
  }

  public abstract Vector apply(Vector target, Quaternion restRotation);

}

