/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.primitives.constraint;

import frames.primitives.Frame;
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
    Vector new_pos = Quaternion.multiply(desired, new Vector(0, 0, 1));
    Vector constrained = apply(new_pos, _restRotation);
    //Get Quaternion
    return new Quaternion(new Vector(0, 0, 1), Quaternion.multiply(frame.rotation().inverse(), constrained));
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

    xbound = xbound > Math.pow(10, 2) ? (float) Math.pow(10, 2) : xbound;
    ybound = ybound > Math.pow(10, 2) ? (float) Math.pow(10, 2) : ybound;
    xbound = xbound < -Math.pow(10, 2) ? (float) -Math.pow(10, 2) : xbound;
    ybound = ybound < -Math.pow(10, 2) ? (float) -Math.pow(10, 2) : ybound;

    float ellipse = ((xaspect * xaspect) / (xbound * xbound)) + ((yaspect * yaspect) / (ybound * ybound));
    inbounds = inbounds && ellipse <= 1;
    if ((!inbounds && !inv && proj.magnitude() > Float.MIN_VALUE) || (inbounds && inv && proj.magnitude() > Float.MIN_VALUE)) {
      float a = (float) (Math.atan2(yaspect, xaspect));
      float cos = (float) (Math.cos(a));
      if (cos < 0) cos = -cos < Math.pow(10, -4) ? -Float.MIN_VALUE : cos;
      else cos = cos < Math.pow(10, -4) ? Float.MIN_VALUE : cos;
      float sin = (float) (Math.sin(a));
      if (sin < 0) sin = -sin < Math.pow(10, -4) ? -Float.MIN_VALUE : sin;
      else sin = sin < Math.pow(10, -4) ? Float.MIN_VALUE : sin;
      float rad = 1.f / (float) Math.sqrt(((cos * cos) / (xbound * xbound)) + ((sin * sin) / (ybound * ybound)));
      if (Math.abs(cos) <= Float.MIN_VALUE) {
        rad = (float) Math.sqrt((ybound * ybound) / (sin * sin));
      }
      if (Math.abs(sin) <= Float.MIN_VALUE) {
        rad = (float) Math.sqrt((xbound * xbound) / (cos * cos));
      }
      float x = rad * cos;
      float y = rad * sin;

      f = Vector.add(proj, Vector.multiply(rvec, x));
      f = Vector.add(f, Vector.multiply(uvec, y));

      if (Math.abs(f.x()) < Math.pow(10, -4)) f.setX(0);
      if (Math.abs(f.y()) < Math.pow(10, -4)) f.setY(0);
      if (Math.abs(f.z()) < Math.pow(10, -4)) f.setZ(0);
      f.normalize();
      f.multiply(target.magnitude());
    }
    return f;
  }
}
