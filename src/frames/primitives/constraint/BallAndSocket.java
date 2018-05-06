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

package frames.primitives.constraint;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import static java.lang.Math.PI;


/**
 * A Frame is constrained to disable translation and
 * allow 2-DOF rotation limiting Rotation in a Sphere to
 * laid inside an Ellipse.
 */
public class BallAndSocket extends Constraint {
  /*
   * With this Kind of Constraint no Translation is allowed
   * and the rotation depends on 4 angles. This kind of constraint always
   * look for the reference frame (local constraint), if no initial position is
   * set a Quat() is assumed as rest position.
   *
   * It is important to note that The Twist Axis will be in the same direction as
   * Z-Axis in The rest Rotation Transformation, similarly Up Vector will correspond to
   * Y-Axis and right direction with X-Axis direction.
   * */
  protected float _down;
  protected float _up;
  protected float _left;
  protected float _right;
  protected Quaternion _restRotation = new Quaternion();

  public float down() {
    return _down;
  }

  public void setDown(float down) {
    this._down = down;
  }

  public float up() {
    return _up;
  }

  public void setUp(float up) {
    this._up = up;
  }

  public float left() {
    return _left;
  }

  public void setLeft(float left) {
    this._left = left;
  }

  public float right() {
    return _right;
  }

  public void setRight(float right) {
    this._right = right;
  }

  public Quaternion restRotation() {
    return _restRotation;
  }

  public void setRestRotation(Quaternion restRotation) {
    this._restRotation = restRotation.get();
  }

  /**
   * reference is a Quaternion that will be aligned to point to the given Basis Vectors
   * result will be stored on restRotation.
   * twist and up axis are defined locally on reference rotation
   */
  public void setRestRotation(Quaternion reference, Vector up, Vector twist) {
    _restRotation = reference.get();
    Vector Z = _restRotation.inverse().rotate(twist);
    //Align Y-Axis with Up Axis
    _restRotation.compose(new Quaternion(new Vector(0, 1, 0), up));
    //Align y-Axis with twist vector
    _restRotation.compose(new Quaternion(new Vector(0, 0, 1), twist));
  }

  public BallAndSocket() {
    _down = (float) (PI / 2.f);
    _left = (float) (PI / 2.f);
    _up = (float) (PI / 2.f);
    _right = (float) (PI / 2.f);
    _restRotation = new Quaternion();
  }

  public BallAndSocket(float down, float up, float left, float right, Quaternion restRotation) {
    this(down, up, left, right);
    this._restRotation = restRotation.get();
  }

  public BallAndSocket(float down, float up, float left, float right) {
    this();
    this._down = down;
    this._up = up;
    this._left = left;
    this._right = right;
  }

  public BallAndSocket(float vertical, float horizontal) {
    this(vertical, vertical, horizontal, horizontal);
  }

  public BallAndSocket(float vertical, float horizontal, Quaternion restRotation) {
    this(vertical, vertical, horizontal, horizontal, restRotation);
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
    Quaternion desired = Quaternion.compose(frame.rotation(), rotation);
    //twist to frame
    Vector twist = _restRotation.rotate(new Vector(0, 0, 1));
    Vector new_pos = Quaternion.multiply(desired, twist);
    Vector constrained = apply(new_pos, _restRotation);
    //Get Quaternion
    return new Quaternion(twist, Quaternion.multiply(frame.rotation().inverse(), constrained));
  }


  @Override
  public Vector constrainTranslation(Vector translation, Frame frame) {
    return new Vector(0, 0, 0);
  }


  /*
   * Adapted from http://wiki.roblox.com/index.php?title=Inverse_kinematics
   * new_pos: new position defined in terms of local coordinates
   */

  /*
  public Vector constraint(Vector target) {
    return constraint(target, _restRotation);
  }
  */

  //TODO : rename, discard?
  //TODO : remove unnecessary calculations (consider this restriction only from 0 to PI)
  public Vector apply(Vector target, Quaternion restRotation) {
    Vector uvec = Quaternion.multiply(restRotation, new Vector(0, 1, 0));
    Vector rvec = Quaternion.multiply(restRotation, new Vector(1, 0, 0));
    Vector line = Quaternion.multiply(restRotation, new Vector(0, 0, 1));

    float PI = (float) Math.PI;
    Vector f = target.get();
    float scalar = Vector.dot(target, line) / line.magnitude();
    Vector proj = Vector.multiply(line, scalar);
    Vector adjust = Vector.subtract(target, proj);
    float xaspect = Vector.dot(adjust, rvec);
    float yaspect = Vector.dot(adjust, uvec);
    float clampDown = this._down;
    float clampUp = this._up;
    float clampLeft = this._left;
    float clampRight = this._right;
    boolean inv = false;
    float xbound = xaspect >= 0 ? clampRight : clampLeft;
    float ybound = yaspect >= 0 ? clampUp : clampDown;
    boolean inbounds = true;
    if (scalar < 0) {
      if (xbound > PI / 2. && ybound > PI / 2.) {
        xbound = proj.magnitude() * (float) (Math.tan(PI - xbound));
        ybound = proj.magnitude() * (float) (Math.tan(PI - ybound));
        inv = true;
      } else {
        xbound = proj.magnitude() * (float) (Math.tan(xbound));
        ybound = proj.magnitude() * (float) (Math.tan(ybound));
        proj.multiply(-1.f);
        inbounds = false;
      }
    } else {
      xbound = xbound > PI / 2. ? proj.magnitude() * (float) (Math.tan(PI / 2.f))
          : proj.magnitude() * (float) (Math.tan(xbound));
      ybound = ybound > PI / 2. ? proj.magnitude() * (float) (Math.tan(PI / 2.f))
          : proj.magnitude() * (float) (Math.tan(ybound));
    }

    float ellipse = ((xaspect * xaspect) / (xbound * xbound)) + ((yaspect * yaspect) / (ybound * ybound));
    inbounds = inbounds && ellipse <= 1;
    if ((!inbounds && !inv && proj.magnitude() > Float.MIN_VALUE) || (inbounds && inv && proj.magnitude() > Float.MIN_VALUE)) {
      float a = (float) (Math.atan2(yaspect, xaspect));
      float cos = (float) (Math.cos(a));
      float sin = (float) (Math.sin(a));
      float rad = 1.f / (float) Math.sqrt(((cos * cos) / (xbound * xbound)) + ((sin * sin) / (ybound * ybound)));
      float x = rad * cos;
      float y = rad * sin;

      f = Vector.add(proj, Vector.multiply(rvec, x));
      f = Vector.add(f, Vector.multiply(uvec, y));

      f.normalize();
      f.multiply(target.magnitude());
    }
    return f;
  }
}
