/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.geom;

import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;
import remixlab.util.Util;

/**
 * A class to describe a two or three dimensional vector. This class API aims to conform
 * that of the great <a href="https://processing.org/reference/PVector.html">Processing
 * PVector</a>.
 * <p>
 * The result of all functions are applied to the vector itself, with the exception of
 * cross(), which returns a new Vec (or writes to a specified 'target' Vec). That is,
 * add() will add the contents of one vector to this one. Using add() with additional
 * parameters allows you to put the result into a new Vec. Functions that act on multiple
 * vectors also include static versions. Because creating new objects can be
 * computationally expensive, most functions include an optional 'target' Vec, so that a
 * new Vec object is not created with each operation.
 * <p>
 * Initially based on the <a href="http://www.processing.org">Processing</a> PVector
 * class.
 */
public class Vec implements Linkable {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(this.vec[0]).append(this.vec[1]).append(this.vec[2]).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    Vec other = (Vec) obj;
    return new EqualsBuilder().append(this.vec[0], other.vec[0]).append(this.vec[1], other.vec[1])
        .append(this.vec[2], other.vec[2]).isEquals();
  }

  /**
   * The x, y and z coordinates of the Vec.
   */
  public float vec[] = new float[3];

  /**
   * Constructor for an empty vector: x, y, and z are set to 0.
   */
  public Vec() {
    reset();
  }

  /**
   * Constructor for a 3D vector.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   * @param z the y coordinate.
   */
  public Vec(float x, float y, float z) {
    this.vec[0] = x;
    this.vec[1] = y;
    this.vec[2] = z;
  }

  protected Vec(Vec other) {
    set(other);
  }

  /**
   * Constructor for a 2D vector: z coordinate is set to 0.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   */
  public Vec(float x, float y) {
    this.vec[0] = x;
    this.vec[1] = y;
    this.vec[2] = 0;
  }

  /**
   * Returns the x component of the Vec.
   */
  public float x() {
    return this.vec[0];
  }

  /**
   * Returns the y component of the Vec.
   */
  public float y() {
    return this.vec[1];
  }

  /**
   * Returns the z component of the Vec.
   */
  public float z() {
    return this.vec[2];
  }

  /**
   * Sets the x component of the Vec.
   */
  public void setX(float x) {
    this.vec[0] = x;
  }

  /**
   * Sets the y component of the Vec.
   */
  public void setY(float y) {
    this.vec[1] = y;
  }

  /**
   * Sets the z component of the Vec.
   */
  public void setZ(float z) {
    this.vec[2] = z;
  }

  /**
   * Same as {@code return projectVectorOnAxis(this, direction)}.
   *
   * @see #projectVectorOnAxis(Vec, Vec)
   */
  public Vec projectVectorOnAxis(Vec direction) {
    return projectVectorOnAxis(this, direction);
  }

  /**
   * Projects the {@code src} Vec on the axis defined by {@code direction} (which does not
   * need to be normalized, but must be non null) that passes through the origin.
   */
  public static Vec projectVectorOnAxis(Vec src, Vec direction) {
    float directionSquaredNorm = squaredNorm(direction);
    if (Util.zero(directionSquaredNorm))
      throw new RuntimeException("Direction squared norm is nearly 0");

    float modulation = src.dot(direction) / directionSquaredNorm;
    return Vec.multiply(direction, modulation);
  }

  /**
   * Same as {@code projectVectorOnPlane(this, normal)}.
   *
   * @see #projectVectorOnPlane(Vec, Vec)
   */
  public Vec projectVectorOnPlane(Vec normal) {
    return projectVectorOnPlane(this, normal);
  }

  /**
   * Projects {@code src} on the plane defined by {@code normal} (which does not need to
   * be normalized, but must be non null) that passes through the origin.
   */
  public static Vec projectVectorOnPlane(Vec src, Vec normal) {
    float normalSquaredNorm = squaredNorm(normal);
    if (Util.zero(normalSquaredNorm))
      throw new RuntimeException("Normal squared norm is nearly 0");

    float modulation = src.dot(normal) / normalSquaredNorm;
    return Vec.subtract(src, Vec.multiply(normal, modulation));
  }

  /**
   * Same as {@code return squaredNorm(this)}.
   *
   * @see #squaredNorm(Vec)
   */
  public float squaredNorm() {
    return squaredNorm(this);
  }

  /**
   * Utility function that returns the squared norm of the Vec.
   */
  public static float squaredNorm(Vec v) {
    return (v.vec[0] * v.vec[0]) + (v.vec[1] * v.vec[1]) + (v.vec[2] * v.vec[2]);
  }

  /**
   * Same as {@code return orthogonalVector(this)}.
   *
   * @see #orthogonalVector(Vec)
   */
  public Vec orthogonalVector() {
    return orthogonalVector(this);
  }

  /**
   * Utility function that returns a Vec orthogonal to {@code v}. Its {@code mag()}
   * depends on the Vec, but is zero only for a {@code null} Vec. Note that the function
   * that associates an {@code orthogonalVector()} to a Vec is not continuous.
   */
  public static Vec orthogonalVector(Vec v) {
    // Find smallest component. Keep equal case for null values.
    if ((Math.abs(v.vec[1]) >= 0.9f * Math.abs(v.vec[0])) && (Math.abs(v.vec[2]) >= 0.9f * Math.abs(v.vec[0])))
      return new Vec(0.0f, -v.vec[2], v.vec[1]);
    else if ((Math.abs(v.vec[0]) >= 0.9f * Math.abs(v.vec[1])) && (Math.abs(v.vec[2]) >= 0.9f * Math.abs(v.vec[1])))
      return new Vec(-v.vec[2], 0.0f, v.vec[0]);
    else
      return new Vec(-v.vec[1], v.vec[0], 0.0f);
  }

  @Override
  public void link(float[] src) {
    vec = src;
  }

  @Override
  public void unLink() {
    float[] data = new float[3];
    get(data);
    set(data);
  }

  /**
   * Sets all Vec components to 0.
   */
  public void reset() {
    vec[0] = vec[1] = vec[2] = 0;
  }

  // end new

  /**
   * Set x, y, and z coordinates.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   * @param z the z coordinate.
   */
  public void set(float x, float y, float z) {
    this.vec[0] = x;
    this.vec[1] = y;
    this.vec[2] = z;
  }

  /**
   * Set x, y, and z coordinates from a Vec object.
   *
   * @param v the Vec object to be copied
   */
  @Override
  public void set(Linkable v) {
    if (!(v instanceof Vec))
      throw new RuntimeException("v should be an instance of Vec");
    set((Vec) v);
  }

  public void set(Vec v) {
    this.vec[0] = v.vec[0];
    this.vec[1] = v.vec[1];
    this.vec[2] = v.vec[2];
  }

  /**
   * Set the x, y (and maybe z) coordinates using a float[] array as the source.
   *
   * @param source array to copy from
   */
  @Override
  public void set(float[] source) {
    if (source.length >= 2) {
      this.vec[0] = source[0];
      this.vec[1] = source[1];
    }
    if (source.length >= 3) {
      this.vec[2] = source[2];
    }
  }

  /**
   * Get a copy of this vector.
   */
  @Override
  public Vec get() {
    return new Vec(this);
  }

  @Override
  public float[] get(float[] target) {
    if (target == null) {
      return new float[]{this.vec[0], this.vec[1], this.vec[2]};
    }
    if (target.length >= 2) {
      target[0] = this.vec[0];
      target[1] = this.vec[1];
    }
    if (target.length >= 3) {
      target[2] = this.vec[2];
    }
    return target;
  }

  /**
   * Calculate the magnitude (length) of the vector
   *
   * @return the magnitude of the vector
   */
  public float magnitude() {
    return (float) Math.sqrt(this.vec[0] * this.vec[0] + this.vec[1] * this.vec[1] + this.vec[2] * this.vec[2]);
  }

  /**
   * Calculate the squared magnitude of the vector Faster if the real length is not
   * required in the case of comparing vectors, etc.
   *
   * @return squared magnitude of the vector
   */
  public float squaredMagnitude() {
    return (this.vec[0] * this.vec[0] + this.vec[1] * this.vec[1] + this.vec[2] * this.vec[2]);
  }

  /**
   * Add a vector to this vector
   *
   * @param v the vector to be added
   */
  public void add(Vec v) {
    this.vec[0] += v.vec[0];
    this.vec[1] += v.vec[1];
    this.vec[2] += v.vec[2];
  }

  /**
   * @param x x component of the vector
   * @param y y component of the vector
   * @param z z component of the vector
   */
  public void add(float x, float y, float z) {
    this.vec[0] += x;
    this.vec[1] += y;
    this.vec[2] += z;
  }

  /**
   * Add two vectors
   *
   * @param v1 a vector
   * @param v2 another vector
   * @return a new vector that is the sum of v1 and v2
   */
  static public Vec add(Vec v1, Vec v2) {
    return add(v1, v2, null);
  }

  /**
   * Add two vectors into a target vector
   *
   * @param v1     a vector
   * @param v2     another vector
   * @param target the target vector (if null, a new vector will be created)
   * @return a new vector that is the sum of v1 and v2
   */
  static public Vec add(Vec v1, Vec v2, Vec target) {
    if (target == null) {
      target = new Vec(v1.vec[0] + v2.vec[0], v1.vec[1] + v2.vec[1], v1.vec[2] + v2.vec[2]);
    } else {
      target.set(v1.vec[0] + v2.vec[0], v1.vec[1] + v2.vec[1], v1.vec[2] + v2.vec[2]);
    }
    return target;
  }

  /**
   * Subtract a vector from this vector
   *
   * @param v the vector to be subtracted
   */
  public void subtract(Vec v) {
    this.vec[0] -= v.vec[0];
    this.vec[1] -= v.vec[1];
    this.vec[2] -= v.vec[2];
  }

  /**
   * @param x the x component of the vector
   * @param y the y component of the vector
   * @param z the z component of the vector
   */
  public void subtract(float x, float y, float z) {
    this.vec[0] -= x;
    this.vec[1] -= y;
    this.vec[2] -= z;
  }

  /**
   * Subtract one vector from another
   *
   * @param v1 a vector
   * @param v2 another vector
   * @return a new vector that is v1 - v2
   */
  static public Vec subtract(Vec v1, Vec v2) {
    return subtract(v1, v2, null);
  }

  /**
   * Subtract one vector from another and store in another vector
   *
   * @param v1     the x, y, and z components of a Vec object
   * @param v2     the x, y, and z components of a Vec object
   * @param target Vec in which to store the result
   */
  static public Vec subtract(Vec v1, Vec v2, Vec target) {
    if (target == null) {
      target = new Vec(v1.vec[0] - v2.vec[0], v1.vec[1] - v2.vec[1], v1.vec[2] - v2.vec[2]);
    } else {
      target.set(v1.vec[0] - v2.vec[0], v1.vec[1] - v2.vec[1], v1.vec[2] - v2.vec[2]);
    }
    return target;
  }

  /**
   * Multiply this vector by a scalar
   *
   * @param n the value to multiply by
   */
  public void multiply(float n) {
    this.vec[0] *= n;
    this.vec[1] *= n;
    this.vec[2] *= n;
  }

  /**
   * Multiply a vector by a scalar
   *
   * @param v a vector
   * @param n scalar
   * @return a new vector that is v1 * n
   */
  static public Vec multiply(Vec v, float n) {
    return multiply(v, n, null);
  }

  /**
   * Multiply a vector by a scalar, and write the result into a target Vec.
   *
   * @param v      a vector
   * @param n      scalar
   * @param target Vec to store the result
   * @return the target vector, now set to v1 * n
   */
  static public Vec multiply(Vec v, float n, Vec target) {
    if (target == null) {
      target = new Vec(v.vec[0] * n, v.vec[1] * n, v.vec[2] * n);
    } else {
      target.set(v.vec[0] * n, v.vec[1] * n, v.vec[2] * n);
    }
    return target;
  }

  /**
   * Divide this vector by a scalar
   *
   * @param n the value to divide by
   */
  public void divide(float n) {
    this.vec[0] /= n;
    this.vec[1] /= n;
    this.vec[2] /= n;
  }

  /**
   * Divide a vector by a scalar and return the result in a new vector.
   *
   * @param v a vector
   * @param n scalar
   * @return a new vector that is v1 / n
   */
  static public Vec divide(Vec v, float n) {
    return divide(v, n, null);
  }

  /**
   * Divide a vector by a scalar and store the result in another vector.
   *
   * @param target Vec in which to store the result
   */
  static public Vec divide(Vec v, float n, Vec target) {
    if (target == null) {
      target = new Vec(v.vec[0] / n, v.vec[1] / n, v.vec[2] / n);
    } else {
      target.set(v.vec[0] / n, v.vec[1] / n, v.vec[2] / n);
    }
    return target;
  }

  /**
   * Calculate the Euclidean distance between two points (considering a point as a vector
   * object)
   *
   * @param v another vector
   * @return the Euclidean distance between
   */
  public float distance(Vec v) {
    float dx = this.vec[0] - v.vec[0];
    float dy = this.vec[1] - v.vec[1];
    float dz = this.vec[2] - v.vec[2];
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Calculate the Euclidean distance between two points (considering a point as a vector
   * object)
   *
   * @param v1 a vector
   * @param v2 another vector
   * @return the Euclidean distance between v1 and v2
   */
  static public float distance(Vec v1, Vec v2) {
    float dx = v1.vec[0] - v2.vec[0];
    float dy = v1.vec[1] - v2.vec[1];
    float dz = v1.vec[2] - v2.vec[2];
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Calculate the dot product with another vector
   *
   * @return the dot product
   */
  public float dot(Vec v) {
    return this.vec[0] * v.vec[0] + this.vec[1] * v.vec[1] + this.vec[2] * v.vec[2];
  }

  /**
   * @param x x component of the vector
   * @param y y component of the vector
   * @param z z component of the vector
   */
  public float dot(float x, float y, float z) {
    return this.vec[0] * x + this.vec[1] * y + this.vec[2] * z;
  }

  /**
   * @param v1 any variable of type Vec
   * @param v2 any variable of type Vec
   */
  static public float dot(Vec v1, Vec v2) {
    return v1.vec[0] * v2.vec[0] + v1.vec[1] * v2.vec[1] + v1.vec[2] * v2.vec[2];
  }

  /**
   * Return a vector composed of the cross product between this and another.
   */
  public Vec cross(Vec v) {
    return cross(v, null);
  }

  /**
   * Perform cross product between this and another vector, and store the result in
   * 'target'. If target is null, a new vector is created.
   */
  public Vec cross(Vec v, Vec target) {
    float crossX = this.vec[1] * v.vec[2] - v.vec[1] * this.vec[2];
    float crossY = this.vec[2] * v.vec[0] - v.vec[2] * this.vec[0];
    float crossZ = this.vec[0] * v.vec[1] - v.vec[0] * this.vec[1];

    if (target == null) {
      target = new Vec(crossX, crossY, crossZ);
    } else {
      target.set(crossX, crossY, crossZ);
    }
    return target;
  }

  /**
   * @param v1     any variable of type Vec
   * @param v2     any variable of type Vec
   * @param target Vec to store the result
   */
  static public Vec cross(Vec v1, Vec v2, Vec target) {
    float crossX = v1.vec[1] * v2.vec[2] - v2.vec[1] * v1.vec[2];
    float crossY = v1.vec[2] * v2.vec[0] - v2.vec[2] * v1.vec[0];
    float crossZ = v1.vec[0] * v2.vec[1] - v2.vec[0] * v1.vec[1];

    if (target == null) {
      target = new Vec(crossX, crossY, crossZ);
    } else {
      target.set(crossX, crossY, crossZ);
    }
    return target;
  }

  /**
   * Normalize the vector to length 1 (make it a unit vector)
   */
  public void normalize() {
    float m = magnitude();
    if (m != 0 && m != 1) {
      divide(m);
    }
  }

  /**
   * Normalize this vector, storing the result in another vector.
   *
   * @param target Set to null to create a new vector
   * @return a new vector (if target was null), or target
   */
  public Vec normalize(Vec target) {
    if (target == null) {
      target = new Vec();
    }
    float m = magnitude();
    if (m > 0) {
      target.set(vec[0] / m, vec[1] / m, vec[02] / m);
    } else {
      target.set(vec[0], vec[1], vec[2]);
    }
    return target;
  }

  /**
   * Limit the magnitude of this vector
   *
   * @param max the maximum length to limit this vector
   */
  public void limit(float max) {
    if (magnitude() > max) {
      normalize();
      multiply(max);
    }
  }

  /**
   * Sets the magnitude of the vector to an arbitrary amount.
   *
   * @param len the new length for this vector
   */
  public void setMagnitude(float len) {
    normalize();
    multiply(len);
  }

  /**
   * Sets the magnitude of this vector, storing the result in another vector.
   *
   * @param target Set to null to create a new vector
   * @param len    the new length for the new vector
   * @return a new vector (if target was null), or target
   */
  public Vec setMagnitude(Vec target, float len) {
    target = normalize(target);
    target.multiply(len);
    return target;
  }

  /**
   * Calculate the angle of rotation for this vector (only 2D vectors)
   *
   * @return the angle of rotation
   */
  public float heading() {
    float angle = (float) Math.atan2(-this.vec[1], this.vec[0]);
    return -1 * angle;
  }

  /**
   * Rotate the vector by an angle (only 2D vectors), magnitude remains the same
   *
   * @param theta the angle of rotation
   */
  public void rotate(float theta) {
    float xTemp = this.vec[0];
    // Might need to check for rounding errors like with angleBetween function?
    this.vec[0] = this.vec[0] * (float) Math.cos(theta) - this.vec[1] * (float) Math.sin(theta);
    this.vec[1] = xTemp * (float) Math.sin(theta) + this.vec[1] * (float) Math.cos(theta);
  }

  /**
   * Linear interpolate the vector to another vector
   *
   * @param v   the vector to lerp to
   * @param amt The amt parameter is the amount to interpolate between the two vectors where
   *            1.0 equal to the new vector 0.1 is very near the new vector, 0.5 is half-way
   *            in between.
   */
  public void lerp(Vec v, float amt) {
    this.vec[0] = Util.lerp(this.vec[0], v.vec[0], amt);
    this.vec[1] = Util.lerp(this.vec[1], v.vec[1], amt);
    this.vec[2] = Util.lerp(this.vec[2], v.vec[2], amt);
  }

  /**
   * Linear interpolate between two vectors (returns a new Vec object).
   *
   * @param v1 the vector to start from
   * @param v2 the vector to lerp to
   */
  public static Vec lerp(Vec v1, Vec v2, float amt) {
    Vec v = v1.get();
    v.lerp(v2, amt);
    return v;
  }

  /**
   * Linear interpolate the vector to x,y,z values.
   *
   * @param x the x component to lerp to
   * @param y the y component to lerp to
   * @param z the z component to lerp to
   */
  public void lerp(float x, float y, float z, float amt) {
    this.vec[0] = Util.lerp(this.x(), x, amt);
    this.vec[1] = Util.lerp(this.y(), y, amt);
    this.vec[2] = Util.lerp(this.z(), z, amt);
  }

  /**
   * Calculate the angle between two vectors, using the dot product.
   *
   * @param v1 a vector
   * @param v2 another vector
   * @return the angle between the vectors
   */
  static public float angleBetween(Vec v1, Vec v2) {
    // We get NaN if we pass in a zero vector which can cause problems
    // Zero seems like a reasonable angle between a 0 length vector and
    // something else
    if (Util.zero(v1.magnitude()))
      return 0.0f;
    if (Util.zero(v2.magnitude()))
      return 0.0f;

    // as in P5:
    // double dot = v1.x() * v2.x() + v1.y() * v2.y() + v1.z() * v2.z();
    // double v1mag = Math.sqrt(v1.x() * v1.x() + v1.y() * v1.y() + v1.z() *
    // v1.z());
    // double v2mag = Math.sqrt(v2.x() * v2.x() + v2.y() * v2.y() + v2.z() *
    // v2.z());
    // double amt = dot / (v1mag * v2mag);
    // if (amt <= -1) return (float) Math.PI; else if (amt >= 1) return 0;
    // return (float) Math.acos(amt);

    // as here:
    // http://stackoverflow.com/questions/10133957/signed-angle-between-two-vectors-without-a-reference-plane
    float s = cross(v1, v2, null).magnitude();
    float c = dot(v1, v2);
    return (float) Math.atan2(s, c);
  }

  /**
   * Make a new 2D unit vector from an angle.
   *
   * @param angle the angle
   * @return the new unit PVec
   */
  static public Vec fromAngle(float angle) {
    return fromAngle(angle, null);
  }

  /**
   * Make a new 2D unit vector from an angle
   *
   * @param angle  the angle
   * @param target the target vector (if null, a new vector will be created)
   * @return the Vec
   */
  static public Vec fromAngle(float angle, Vec target) {
    if (target == null) {
      target = new Vec((float) Math.cos(angle), (float) Math.sin(angle), 0);
    } else {
      target.set((float) Math.cos(angle), (float) Math.sin(angle), 0);
    }
    return target;
  }

  public void print() {
    System.out.println(x() + " " + y() + " " + z() + "\n");
  }

  public String toString() {
    return "[ " + this.vec[0] + ", " + this.vec[1] + ", " + this.vec[2] + " ]";
  }
}
