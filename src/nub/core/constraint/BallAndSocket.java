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

import nub.primitives.Quaternion;
import nub.primitives.Vector;

import static java.lang.Math.PI;


/**
 * A Ball and Socket constraint is a {@link ConeConstraint} that defines the boundaries of the cone constraint
 * with 4 parameters corresponding to the dimensions of each semi-axis of an ellipse.
 *
 * Warning: this constraints works properly when the parameters are less than PI / 2 radians, if that not the case
 * you must use a {@link SphericalPolygon} constraint.
 */
public class BallAndSocket extends ConeConstraint {
  protected float _down;
  protected float _up;
  protected float _left;
  protected float _right;

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

  /*
   * Adapted from http://wiki.roblox.com/index.php?title=Inverse_kinematics
   * new_pos: new position defined in terms of local coordinates
   */
  //TODO : remove unnecessary calculations (consider this restriction only from 0 to PI)
  public Vector apply(Vector target) {
    Vector uvec = new Vector(0, 1, 0);
    Vector rvec = new Vector(1, 0, 0);
    Vector line = new Vector(0, 0, 1);

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
      /*float a = (float) (Math.atan2(yaspect, xaspect));
      float cos = (float) (Math.cos(a));
      float sin = (float) (Math.sin(a));
      float rad = 1.f / (float) Math.sqrt(((cos * cos) / (xbound * xbound)) + ((sin * sin) / (ybound * ybound)));
      float x = rad * cos;
      float y = rad * sin;*/

      Vector v = closestPointToEllipse(xbound, ybound, new Vector(xaspect, yaspect));
      f = Vector.add(proj, Vector.multiply(rvec, v.x()));
      f = Vector.add(f, Vector.multiply(uvec, v.y()));

      f.normalize();
      f.multiply(target.magnitude());
    }
    return f;
  }

  //this is an adaptation from https://github.com/0xfaded/ellipse_demo/issues/1
  //more info at: https://wet-robots.ghost.io/simple-method-for-distance-to-ellipse/
  public static Vector closestPointToEllipse(float semi_major, float semi_minor, Vector point) {
    float px = Math.abs(point.x());
    float py = Math.abs(point.y());

    float tx = 0.707f;
    float ty = 0.707f;

    float a = semi_major;
    float b = semi_minor;

    float x, y, ex, ey, rx, ry, qx, qy, r, q, t;

    for (int i = 0; i < 3; i++) {
      x = a * tx;
      y = b * ty;
      ex = (a * a - b * b) * (tx * tx * tx) / a;
      ey = (b * b - a * a) * (ty * ty * ty) / b;
      rx = x - ex;
      ry = y - ey;
      qx = px - ex;
      qy = py - ey;

      r = (float) Math.sqrt(ry * ry + rx * rx);
      q = (float) Math.sqrt(qy * qy + qx * qx);

      tx = Math.min(1, Math.max(0, (qx * r / q + ex) / a));
      ty = Math.min(1, Math.max(0, (qy * r / q + ey) / b));

      t = (float) Math.sqrt(ty * ty + tx * tx);
      tx /= t;
      ty /= t;
    }
    return new Vector(Math.signum(point.x()) * a * tx, Math.signum(point.y()) * b * ty);
  }
}
