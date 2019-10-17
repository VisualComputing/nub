/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.primitives;

/**
 * A class to describe a two or three dimensional vector. This class API aims to conform
 * that of the great <a href="https://processing.org/reference/PVector.html">Processing
 * PVector</a>.
 * <p>
 * The result of all functions are applied to the vector itself, with the exception of
 * cross(), which returns a new vector (or writes to a specified 'target' vector). That is,
 * add() will add the contents of one vector to this one. Using add() with additional
 * parameters allows you to put the result into a new vector. Functions that act on multiple
 * vectors also include static versions. Because creating new objects can be
 * computationally expensive, most functions include an optional 'target' vector, so that a
 * new vector object is not created with each operation.
 * <p>
 * Initially based on the <a href="http://www.processing.org">Processing</a> PVector
 * class.
 */
public class Vector {
  /**
   * Returns whether or not this vector matches other.
   *
   * @param vector other vector
   */
  public boolean matches(Vector vector) {
    return this._vector[0] == vector._vector[0] && this._vector[1] == vector._vector[1] && this._vector[2] == vector._vector[2];
  }

  /**
   * The x, y and z coordinates of the Vector.
   */
  public float _vector[] = new float[3];

  /**
   * Constructor for an empty vector: x, y, and z are set to 0.
   */
  public Vector() {
    reset();
  }

  /**
   * Constructor for a 3D vector.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   * @param z the y coordinate.
   */
  public Vector(float x, float y, float z) {
    this._vector[0] = x;
    this._vector[1] = y;
    this._vector[2] = z;
  }

  protected Vector(Vector other) {
    set(other);
  }

  /**
   * Constructor for a 2D vector: z coordinate is set to 0.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   */
  public Vector(float x, float y) {
    this._vector[0] = x;
    this._vector[1] = y;
    this._vector[2] = 0;
  }

  /**
   * Macro that returns a number number between {@code lower} and {@code upper}.
   */
  protected static float _random(float lower, float upper) {
    return ((float) Math.random() * (upper - lower)) + lower;
  }

  /**
   * Randomize this vector. The vector is normalized too.
   *
   * @see #random()
   */
  public void randomize() {
    set(Vector.random());
  }

  /**
   * Returns a normalized random vector.
   *
   * @see #randomize()
   */
  public static Vector random() {
    Vector vector = new Vector(_random(-1, 1), _random(-1, 1), _random(-1, 1));
    vector.normalize();
    return vector;
  }

  /**
   * Returns the x component of the vector.
   */
  public float x() {
    return this._vector[0];
  }

  /**
   * Returns the y component of the vector.
   */
  public float y() {
    return this._vector[1];
  }

  /**
   * Returns the z component of the vector.
   */
  public float z() {
    return this._vector[2];
  }

  /**
   * Sets the x component of the vector.
   */
  public void setX(float x) {
    this._vector[0] = x;
  }

  /**
   * Sets the y component of the vector.
   */
  public void setY(float y) {
    this._vector[1] = y;
  }

  /**
   * Sets the z component of the vector.
   */
  public void setZ(float z) {
    this._vector[2] = z;
  }

  /**
   * Same as {@code return projectVectorOnAxis(this, direction)}.
   *
   * @see #projectVectorOnAxis(Vector, Vector)
   */
  public Vector projectVectorOnAxis(Vector direction) {
    return projectVectorOnAxis(this, direction);
  }

  /**
   * Returns the vector projection of a onto b. Vector b should be normalized. See:
   * https://en.wikipedia.org/wiki/Vector_projection
   */
  public static Vector vectorProjection(Vector a, Vector b) {
    return Vector.multiply(b, scalarProjection(a, b));
  }

  /**
   * Returns the scalar projection of a onto b. Vector b should be normalized. See:
   * https://en.wikipedia.org/wiki/Scalar_projection
   */
  public static float scalarProjection(Vector a, Vector b) {
    return Vector.dot(a, b);
  }

  /**
   * Projects the {@code vector} on the axis defined by {@code direction} (which does not
   * need to be normalized, but must be non null) that passes through the origin.
   */
  public static Vector projectVectorOnAxis(Vector vector, Vector direction) {
    Vector b = direction.get();
    b.normalize();
    if (b.magnitude() == 0)
      throw new RuntimeException("Direction squared norm is nearly 0");
    return Vector.vectorProjection(vector, b);
  }

  /**
   * Same as {@code projectVectorOnPlane(this, normal)}.
   *
   * @see #projectVectorOnPlane(Vector, Vector)
   */
  public Vector projectVectorOnPlane(Vector normal) {
    return projectVectorOnPlane(this, normal);
  }

  /**
   * Projects {@code vector} on the plane defined by {@code normal} (which does not need to
   * be normalized, but must be non null) that passes through the origin.
   */
  public static Vector projectVectorOnPlane(Vector vector, Vector normal) {
    float normalSquaredNorm = squaredNorm(normal);
    if (normalSquaredNorm == 0)
      throw new RuntimeException("Normal squared norm is nearly 0");

    float modulation = vector.dot(normal) / normalSquaredNorm;
    return Vector.subtract(vector, Vector.multiply(normal, modulation));
  }

  /**
   * Same as {@code return squaredNorm(this)}.
   *
   * @see #squaredNorm(Vector)
   */
  public float squaredNorm() {
    return squaredNorm(this);
  }

  /**
   * Utility function that returns the squared norm of the vector.
   */
  public static float squaredNorm(Vector vector) {
    return (vector._vector[0] * vector._vector[0]) + (vector._vector[1] * vector._vector[1]) + (vector._vector[2] * vector._vector[2]);
  }

  /**
   * Same as {@code return orthogonalVector(this)}.
   *
   * @see #orthogonalVector(Vector)
   */
  public Vector orthogonalVector() {
    return orthogonalVector(this);
  }

  /**
   * Utility function that returns a vector orthogonal to {@code vector}. Its {@code magnitude()}
   * depends on the vector, but is zero only for a {@code null} vector. Note that the function
   * that associates an {@code orthogonalVector()} to a vector is not continuous.
   */
  public static Vector orthogonalVector(Vector vector) {
    // Find smallest component. Keep equal case for null values.
    if ((Math.abs(vector._vector[1]) >= 0.9f * Math.abs(vector._vector[0])) && (Math.abs(vector._vector[2]) >= 0.9f * Math.abs(vector._vector[0])))
      return new Vector(0.0f, -vector._vector[2], vector._vector[1]);
    else if ((Math.abs(vector._vector[0]) >= 0.9f * Math.abs(vector._vector[1])) && (Math.abs(vector._vector[2]) >= 0.9f * Math.abs(vector._vector[1])))
      return new Vector(-vector._vector[2], 0.0f, vector._vector[0]);
    else
      return new Vector(-vector._vector[1], vector._vector[0], 0.0f);
  }

  /**
   * Sets all vector components to 0.
   */
  public void reset() {
    _vector[0] = _vector[1] = _vector[2] = 0;
  }

  // end new

  /**
   * Set x and y coordinates. The z coordinate is set to 0.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   * @see #set(float, float, float)
   */
  public void set(float x, float y) {
    set(x, y, 0);
  }

  /**
   * Set x, y, and z coordinates.
   *
   * @param x the x coordinate.
   * @param y the y coordinate.
   * @param z the z coordinate.
   * @see #set(float, float)
   */
  public void set(float x, float y, float z) {
    this._vector[0] = x;
    this._vector[1] = y;
    this._vector[2] = z;
  }

  /**
   * Set x, y, and z coordinates from a Vector object.
   *
   * @param vector the Vector object to be copied
   */
  public void set(Vector vector) {
    this._vector[0] = vector._vector[0];
    this._vector[1] = vector._vector[1];
    this._vector[2] = vector._vector[2];
  }

  /**
   * Set the x, y (and maybe z) coordinates using a float[] array as the source.
   *
   * @param source array to copy from
   */
  public void set(float[] source) {
    if (source.length >= 2) {
      this._vector[0] = source[0];
      this._vector[1] = source[1];
    }
    if (source.length >= 3) {
      this._vector[2] = source[2];
    }
  }

  /**
   * Returns a deep copy of this vector.
   */
  public Vector get() {
    return new Vector(this);
  }

  /**
   * Returns this vector as an array.
   */
  public float[] get(float[] target) {
    if (target == null) {
      return new float[]{this._vector[0], this._vector[1], this._vector[2]};
    }
    if (target.length >= 2) {
      target[0] = this._vector[0];
      target[1] = this._vector[1];
    }
    if (target.length >= 3) {
      target[2] = this._vector[2];
    }
    return target;
  }

  /**
   * Calculate the magnitude (length) of the vector
   *
   * @return the magnitude of the vector
   */
  public float magnitude() {
    return (float) Math.sqrt(this._vector[0] * this._vector[0] + this._vector[1] * this._vector[1] + this._vector[2] * this._vector[2]);
  }

  /**
   * Calculate the squared magnitude of the vector.
   *
   * @return squared magnitude of the vector
   */
  public float squaredMagnitude() {
    return (this._vector[0] * this._vector[0] + this._vector[1] * this._vector[1] + this._vector[2] * this._vector[2]);
  }

  /**
   * Add a vector to this vector
   *
   * @param vector the vector to be added
   */
  public void add(Vector vector) {
    this._vector[0] += vector._vector[0];
    this._vector[1] += vector._vector[1];
    this._vector[2] += vector._vector[2];
  }

  /**
   * @param x x component of the vector
   * @param y y component of the vector
   * @param z z component of the vector
   */
  public void add(float x, float y, float z) {
    this._vector[0] += x;
    this._vector[1] += y;
    this._vector[2] += z;
  }

  /**
   * Add two vectors.
   *
   * @param vector1 a vector
   * @param vector2 another vector
   * @return a new vector that is the sum of vector1 and vector2
   */
  static public Vector add(Vector vector1, Vector vector2) {
    return add(vector1, vector2, null);
  }

  /**
   * Add two vectors into a target vector.
   *
   * @param vector1 a vector
   * @param vector2 another vector
   * @param target  the target vector (if null, a new vector will be created)
   * @return a new vector that is the sum of vector1 and vector2
   */
  static public Vector add(Vector vector1, Vector vector2, Vector target) {
    if (target == null) {
      target = new Vector(vector1._vector[0] + vector2._vector[0], vector1._vector[1] + vector2._vector[1], vector1._vector[2] + vector2._vector[2]);
    } else {
      target.set(vector1._vector[0] + vector2._vector[0], vector1._vector[1] + vector2._vector[1], vector1._vector[2] + vector2._vector[2]);
    }
    return target;
  }

  /**
   * Subtract a vector from this vector.
   *
   * @param vector the vector to be subtracted
   */
  public void subtract(Vector vector) {
    this._vector[0] -= vector._vector[0];
    this._vector[1] -= vector._vector[1];
    this._vector[2] -= vector._vector[2];
  }

  /**
   * @param x the x component of the vector
   * @param y the y component of the vector
   * @param z the z component of the vector
   */
  public void subtract(float x, float y, float z) {
    this._vector[0] -= x;
    this._vector[1] -= y;
    this._vector[2] -= z;
  }

  /**
   * Subtract one vector from another.
   *
   * @param vector1 a vector
   * @param vector2 another vector
   * @return a new vector that is vector1 - vector2
   */
  static public Vector subtract(Vector vector1, Vector vector2) {
    return subtract(vector1, vector2, null);
  }

  /**
   * Subtract one vector from another and store in another vector.
   *
   * @param vector1 the x, y, and z components of a Vector object
   * @param vector2 the x, y, and z components of a Vector object
   * @param target  Vector in which to store the result
   */
  static public Vector subtract(Vector vector1, Vector vector2, Vector target) {
    if (target == null) {
      target = new Vector(vector1._vector[0] - vector2._vector[0], vector1._vector[1] - vector2._vector[1], vector1._vector[2] - vector2._vector[2]);
    } else {
      target.set(vector1._vector[0] - vector2._vector[0], vector1._vector[1] - vector2._vector[1], vector1._vector[2] - vector2._vector[2]);
    }
    return target;
  }

  /**
   * Multiply this vector by a scalar.
   *
   * @param n the value to multiply by
   */
  public void multiply(float n) {
    this._vector[0] *= n;
    this._vector[1] *= n;
    this._vector[2] *= n;
  }

  /**
   * Multiply a vector by a scalar.
   *
   * @param vector a vector
   * @param n      scalar
   * @return a new vector that is vector * n
   */
  static public Vector multiply(Vector vector, float n) {
    return multiply(vector, n, null);
  }

  /**
   * Multiply a vector by a scalar, and write the result into a target vector.
   *
   * @param vector a vector
   * @param n      scalar
   * @param target Vector to store the result
   * @return the target vector, now set to vector * n
   */
  static public Vector multiply(Vector vector, float n, Vector target) {
    if (target == null) {
      target = new Vector(vector._vector[0] * n, vector._vector[1] * n, vector._vector[2] * n);
    } else {
      target.set(vector._vector[0] * n, vector._vector[1] * n, vector._vector[2] * n);
    }
    return target;
  }

  /**
   * Divide this vector by a scalar.
   *
   * @param n the value to divide by
   */
  public void divide(float n) {
    this._vector[0] /= n;
    this._vector[1] /= n;
    this._vector[2] /= n;
  }

  /**
   * Divide a vector by a scalar and return the result in a new vector.
   *
   * @param vector a vector
   * @param n      scalar
   * @return a new vector that is vector / n
   */
  static public Vector divide(Vector vector, float n) {
    return divide(vector, n, null);
  }

  /**
   * Divide a vector by a scalar and store the result in another vector.
   *
   * @param target Vector in which to store the result
   */
  static public Vector divide(Vector vector, float n, Vector target) {
    if (target == null) {
      target = new Vector(vector._vector[0] / n, vector._vector[1] / n, vector._vector[2] / n);
    } else {
      target.set(vector._vector[0] / n, vector._vector[1] / n, vector._vector[2] / n);
    }
    return target;
  }

  /**
   * Calculate the Euclidean distance between two points (considering a point as a vector
   * object).
   *
   * @param vector another vector
   * @return the Euclidean distance between
   */
  public float distance(Vector vector) {
    float dx = this._vector[0] - vector._vector[0];
    float dy = this._vector[1] - vector._vector[1];
    float dz = this._vector[2] - vector._vector[2];
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Calculate the Euclidean distance between two points (considering a point as a vector
   * object).
   *
   * @param vector1 a vector
   * @param vector2 another vector
   * @return the Euclidean distance between vector1 and vector2
   */
  static public float distance(Vector vector1, Vector vector2) {
    float dx = vector1._vector[0] - vector2._vector[0];
    float dy = vector1._vector[1] - vector2._vector[1];
    float dz = vector1._vector[2] - vector2._vector[2];
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /**
   * Calculate the dot product with another vector.
   *
   * @return the dot product
   */
  public float dot(Vector vector) {
    return this._vector[0] * vector._vector[0] + this._vector[1] * vector._vector[1] + this._vector[2] * vector._vector[2];
  }

  /**
   * @param x x component of the vector
   * @param y y component of the vector
   * @param z z component of the vector
   */
  public float dot(float x, float y, float z) {
    return this._vector[0] * x + this._vector[1] * y + this._vector[2] * z;
  }

  /**
   * @param vector1 any variable of type Vector
   * @param vector2 any variable of type Vector
   */
  static public float dot(Vector vector1, Vector vector2) {
    return vector1._vector[0] * vector2._vector[0] + vector1._vector[1] * vector2._vector[1] + vector1._vector[2] * vector2._vector[2];
  }

  /**
   * Return a vector composed of the cross product between this and another.
   */
  public Vector cross(Vector v) {
    return cross(v, null);
  }

  /**
   * Perform cross product between this and another vector, and store the result in
   * 'target'. If target is null, a new vector is created.
   */
  public Vector cross(Vector vector, Vector target) {
    float crossX = this._vector[1] * vector._vector[2] - vector._vector[1] * this._vector[2];
    float crossY = this._vector[2] * vector._vector[0] - vector._vector[2] * this._vector[0];
    float crossZ = this._vector[0] * vector._vector[1] - vector._vector[0] * this._vector[1];

    if (target == null) {
      target = new Vector(crossX, crossY, crossZ);
    } else {
      target.set(crossX, crossY, crossZ);
    }
    return target;
  }

  /**
   * Cross product: target = vector1 * vector2.
   *
   * @param vector1 any variable of type Vector
   * @param vector2 any variable of type Vector
   * @param target  Vector to store the result
   */
  static public Vector cross(Vector vector1, Vector vector2, Vector target) {
    float crossX = vector1._vector[1] * vector2._vector[2] - vector2._vector[1] * vector1._vector[2];
    float crossY = vector1._vector[2] * vector2._vector[0] - vector2._vector[2] * vector1._vector[0];
    float crossZ = vector1._vector[0] * vector2._vector[1] - vector2._vector[0] * vector1._vector[1];

    if (target == null) {
      target = new Vector(crossX, crossY, crossZ);
    } else {
      target.set(crossX, crossY, crossZ);
    }
    return target;
  }

  /**
   * Normalize the vector to length 1 (make it a unit vector).
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
  public Vector normalize(Vector target) {
    if (target == null) {
      target = new Vector();
    }
    float m = magnitude();
    if (m > 0) {
      target.set(_vector[0] / m, _vector[1] / m, _vector[2] / m);
    } else {
      target.set(_vector[0], _vector[1], _vector[2]);
    }
    return target;
  }

  /**
   * Limit the magnitude of this vector.
   *
   * @param maximum the maximum length to limit this vector
   */
  public void limit(float maximum) {
    if (magnitude() > maximum) {
      normalize();
      multiply(maximum);
    }
  }

  /**
   * Sets the magnitude of the vector to an arbitrary amount.
   *
   * @param magnitude the new length for this vector
   */
  public void setMagnitude(float magnitude) {
    normalize();
    multiply(magnitude);
  }

  /**
   * Sets the magnitude of this vector, storing the result in another vector.
   *
   * @param target    Set to null to create a new vector
   * @param magnitude the new length for the new vector
   * @return a new vector (if target was null), or target
   */
  public Vector setMagnitude(Vector target, float magnitude) {
    target = normalize(target);
    target.multiply(magnitude);
    return target;
  }

  /**
   * Calculate the angle of rotation for this vector (only 2D vectors).
   *
   * @return the angle of rotation
   */
  public float heading() {
    float angle = (float) Math.atan2(-this._vector[1], this._vector[0]);
    return -1 * angle;
  }

  /**
   * Rotate the vector by an angle (only 2D vectors), magnitude remains the same.
   *
   * @param theta the angle of rotation
   */
  public void rotate(float theta) {
    float xTemp = this._vector[0];
    // Might need to check for rounding errors like with angleBetween function?
    this._vector[0] = this._vector[0] * (float) Math.cos(theta) - this._vector[1] * (float) Math.sin(theta);
    this._vector[1] = xTemp * (float) Math.sin(theta) + this._vector[1] * (float) Math.cos(theta);
  }

  /**
   * Calculates a number between two numbers at a specific increment. The {@code amount}
   * parameter is the amount to interpolate between the two values where 0.0 equal to the
   * first point, 0.1 is very near the first point, 0.5 is half-way in between, etc.
   */
  public static float lerp(float start, float stop, float amount) {
    return start + (stop - start) * amount;
  }

  /**
   * Linear interpolate the vector to another vector.
   *
   * @param vector the vector to lerp to
   * @param amount The amt parameter is the amount to interpolate between the two vectors where
   *               1.0 equal to the new vector 0.1 is very near the new vector, 0.5 is half-way
   *               in between.
   */
  public void lerp(Vector vector, float amount) {
    this._vector[0] = Vector.lerp(this._vector[0], vector._vector[0], amount);
    this._vector[1] = Vector.lerp(this._vector[1], vector._vector[1], amount);
    this._vector[2] = Vector.lerp(this._vector[2], vector._vector[2], amount);
  }

  /**
   * Linear interpolate between two vectors (returns a new Vector object).
   *
   * @param vector1 the vector to start from
   * @param vector2 the vector to lerp to
   */
  public static Vector lerp(Vector vector1, Vector vector2, float amount) {
    Vector v = vector1.get();
    v.lerp(vector2, amount);
    return v;
  }

  /**
   * Linear interpolate the vector to x,y,z values.
   *
   * @param x the x component to lerp to
   * @param y the y component to lerp to
   * @param z the z component to lerp to
   */
  public void lerp(float x, float y, float z, float amount) {
    this._vector[0] = Vector.lerp(this.x(), x, amount);
    this._vector[1] = Vector.lerp(this.y(), y, amount);
    this._vector[2] = Vector.lerp(this.z(), z, amount);
  }

  /**
   * Calculate the angle between two vectors, using the dot product.
   *
   * @param vector1 a vector
   * @param vector2 another vector
   * @return the angle between the vectors
   */
  static public float angleBetween(Vector vector1, Vector vector2) {
    // We get NaN if we pass in a zero vector which can cause problems
    // Zero seems like a reasonable angle between a 0 length vector and
    // something else
    if (vector1.magnitude() == 0)
      return 0.0f;
    if (vector2.magnitude() == 0)
      return 0.0f;

    // as in P5:
    // double dot = v1.x() * v2.x() + v1.y() * v2._y() + v1.z() * v2.z();
    // double v1mag = Math.sqrt(v1.x() * v1.x() + v1.y() * v1._y() + v1.z() *
    // v1.z());
    // double v2mag = Math.sqrt(v2.x() * v2.x() + v2.y() * v2._y() + v2.z() *
    // v2.z());
    // double amt = dot / (v1mag * v2mag);
    // if (amt <= -1) return (float) Math.PI; else if (amt >= 1) return 0;
    // return (float) Math.acos(amt);

    // as here:
    // http://stackoverflow.com/questions/10133957/signed-angle-between-two-vectors-without-a-reference-plane
    float s = cross(vector1, vector2, null).magnitude();
    float c = dot(vector1, vector2);
    return (float) Math.atan2(s, c);
  }

  /**
   * Make a new 2D unit vector from an angle.
   *
   * @param angle the angle
   * @return the new unit PVec
   */
  static public Vector fromAngle(float angle) {
    return fromAngle(angle, null);
  }

  /**
   * Make a new 2D unit vector from an angle.
   *
   * @param angle  the angle
   * @param target the target vector (if null, a new vector will be created)
   * @return the Vector
   */
  static public Vector fromAngle(float angle, Vector target) {
    if (target == null) {
      target = new Vector((float) Math.cos(angle), (float) Math.sin(angle), 0);
    } else {
      target.set((float) Math.cos(angle), (float) Math.sin(angle), 0);
    }
    return target;
  }

  public void print() {
    System.out.println(x() + " " + y() + " " + z() + "\n");
  }

  public String toString() {
    return "[ " + this._vector[0] + ", " + this._vector[1] + ", " + this._vector[2] + " ]";
  }
}
