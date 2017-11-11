/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.primitives;

/**
 * A 3D Quaternion is a 4 element unit quaternion
 * represented by single precision floating point x,y,z,w coordinates. This class API aims
 * to conform that of the great <a href=
 * "http://libqglviewer.com/refManual/classqglviewer_1_1Quaternion.html">libQGLViewer
 * Quaternion</a>.
 */
public class Quaternion {
  /**
   * Returns whether or not this Rot matches other.
   *
   * @param other quat
   */
  public boolean matches(Quaternion other) {
    return quat[0] == ((Quaternion)other).quat[0]
            && quat[1] == ((Quaternion)other).quat[1]
            && quat[2] == ((Quaternion)other).quat[2]
            && quat[3] == ((Quaternion)other).quat[3];
  }

  /**
   * The x, y, z, and w coordinates of the Quaternion represented as a public array.
   */
  public float quat[] = new float[4];

  /**
   * Constructs and initializes a Quaternion to (0.0,0.0,0.0,1.0), i.e., an identity rotation.
   */
  public Quaternion() {
    reset();
  }

  /**
   * Default constructor for Quaternion(float x, float y, float z, float w, boolean normalize),
   * with {@code normalize=true}.
   */
  public Quaternion(float x, float y, float z, float w) {
    this(x, y, z, w, true);
  }

  /**
   * Constructs and initializes a Quaternion from the specified xyzw coordinates.
   *
   * @param x         the x coordinate
   * @param y         the y coordinate
   * @param z         the z coordinate
   * @param w         the w scalar component
   * @param normalize tells whether or not the constructed Quaternion should be normalized.
   */
  public Quaternion(float x, float y, float z, float w, boolean normalize) {
    if (normalize) {
      float mag = (float) Math.sqrt(x * x + y * y + z * z + w * w);
      if (mag > 0.0f) {
        this.quat[0] = x / mag;
        this.quat[1] = y / mag;
        this.quat[2] = z / mag;
        this.quat[3] = w / mag;
      } else {
        this.quat[0] = 0;
        this.quat[1] = 0;
        this.quat[2] = 0;
        this.quat[3] = 1;
      }
    } else {
      this.quat[0] = x;
      this.quat[1] = y;
      this.quat[2] = z;
      this.quat[3] = w;
    }
  }

  /**
   * Convenience constructor that simply calls {@code this(q, true)}
   */
  public Quaternion(float[] q) {
    this(q, true);
  }

  /**
   * Constructs and initializes a Quaternion from the array of length 4.
   *
   * @param q the array of length 4 containing xyzw in order
   */
  public Quaternion(float[] q, boolean normalize) {
    if (normalize) {
      float mag = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
      if (mag > 0.0f) {
        this.quat[0] = q[0] / mag;
        this.quat[1] = q[1] / mag;
        this.quat[2] = q[2] / mag;
        this.quat[3] = q[3] / mag;
      } else {
        this.quat[0] = 0;
        this.quat[1] = 0;
        this.quat[2] = 0;
        this.quat[3] = 1;
      }
    } else {
      this.quat[0] = q[0];
      this.quat[1] = q[1];
      this.quat[2] = q[2];
      this.quat[3] = q[3];
    }
  }

  /**
   * Copy constructor. If {@code normalize} is {@code true} this Quaternion is
   * {@link #normalize()}.
   *
   * @param q1 the Quaternion containing the initialization x y z w data
   */
  public Quaternion(Quaternion q1, boolean normalize) {
    set(q1, normalize);
  }

  /**
   * Constructs and initializes a Quaternion from the specified rotation {@link #axis() axis}
   * (non null) and {@link #angle() angle} (in radians).
   *
   * @param axis  the Vector representing the axis
   * @param angle the angle in radians
   * @see #fromAxisAngle(Vector, float)
   */
  public Quaternion(Vector axis, float angle) {
    fromAxisAngle(axis, angle);
  }

  /**
   * Constructs a Quaternion that will rotate from the {@code from} direction to the {@code to}
   * direction.
   *
   * @param from the first Vector
   * @param to   the second Vector
   * @see #fromTo(Vector, Vector)
   */
  public Quaternion(Vector from, Vector to) {
    fromTo(from, to);
  }

  /**
   * Constructs a Quaternion from the given Euler angles.
   *
   * @param roll  Rotation angle in radians around the x-Axis
   * @param pitch Rotation angle in radians around the y-Axis
   * @param yaw   Rotation angle in radians around the z-Axis
   * @see #fromEulerAngles(float, float, float)
   */
  public Quaternion(float roll, float pitch, float yaw) {
    fromEulerAngles(roll, pitch, yaw);
  }

  /**
   * Constructs a Quaternion from a (supposedly correct) 3x3 rotation matrix given in the upper
   * left 3x3 sub-matrix of the Matrix.
   *
   * @param glMatrix
   * @see #fromMatrix(Matrix)
   */
  public Quaternion(Matrix glMatrix) {
    fromMatrix(glMatrix);
  }

  /**
   * Constructs a Quaternion from the three rotated vectors of an orthogonal basis.
   *
   * @param X 1st Orthogonal Vector
   * @param Y 2nd Orthogonal Vector
   * @param Z 3rd Orthogonal Vector
   * @see #fromRotatedBasis(Vector, Vector, Vector)
   */
  public Quaternion(Vector X, Vector Y, Vector Z) {
    fromRotatedBasis(X, Y, Z);
  }

  protected Quaternion(Quaternion q1) {
    set(q1);
  }

  public Quaternion get() {
    return new Quaternion(this);
  }

  public void reset() {
    this.quat[0] = 0;
    this.quat[1] = 0;
    this.quat[2] = 0;
    this.quat[3] = 1;
  }

  public void link(float[] src) {
    quat = src;
  }

  public void unLink() {
    float[] data = new float[4];
    get(data);
    set(data);
  }

  public float[] get(float[] target) {
    if ((target == null) || (target.length != 4)) {
      target = new float[4];
    }
    target[0] = quat[0];
    target[1] = quat[1];
    target[2] = quat[2];
    target[3] = quat[3];

    return target;
  }

  public void set(float[] source) {
    if (source.length == 4) {
      quat[0] = source[0];
      quat[1] = source[1];
      quat[2] = source[2];
      quat[3] = source[3];
    }
  }

  /**
   * @return Quaternion x component
   */
  public float x() {
    return this.quat[0];
  }

  /**
   * @return Quaternion y component
   */
  public float y() {
    return this.quat[1];
  }

  /**
   * @return Quaternion z component
   */
  public float z() {
    return this.quat[2];
  }

  /**
   * @return Quaternion w component
   */
  public float w() {
    return this.quat[3];
  }

  /**
   * Sets the Quaternion x component
   */
  public void setX(float x) {
    this.quat[0] = x;
  }

  /**
   * Sets the Quaternion y component
   */
  public void setY(float y) {
    this.quat[1] = y;
  }

  /**
   * Sets the Quaternion z component
   */
  public void setZ(float z) {
    this.quat[2] = z;
  }

  /**
   * Sets the Quaternion w component
   */
  public void setW(float w) {
    this.quat[3] = w;
  }

  /**
   * Convenience function that simply calls {@code set(q1, true);}
   *
   * @see #set(Quaternion, boolean)
   */
  public void set(Quaternion q1) {
    set(q1, true);
  }

  /**
   * Set this Quaternion from quaternion {@code q1}. If {@code normalize} is {@code true} this
   * Quaternion is {@link #normalize()}.
   */
  public void set(Quaternion q1, boolean normalize) {
    this.quat[0] = q1.quat[0];
    this.quat[1] = q1.quat[1];
    this.quat[2] = q1.quat[2];
    this.quat[3] = q1.quat[3];
    if (normalize)
      this.normalize();
  }

  /**
   * Sets the value of this Quaternion to the conjugate of itself.
   */
  public void conjugate() {
    this.quat[0] = -this.quat[0];
    this.quat[1] = -this.quat[1];
    this.quat[2] = -this.quat[2];
  }

  /**
   * Sets the value of this Quaternion to the conjugate of Quaternion q1.
   *
   * @param q1 the source vector
   */
  public void conjugate(Quaternion q1) {
    this.quat[0] = -q1.quat[0];
    this.quat[1] = -q1.quat[1];
    this.quat[2] = -q1.quat[2];
    this.quat[3] = q1.quat[3];
  }

  /**
   * Negates all the coefficients of the Quaternion.
   */
  public void negate() {
    this.quat[0] = -this.quat[0];
    this.quat[1] = -this.quat[1];
    this.quat[2] = -this.quat[2];
    this.quat[3] = -this.quat[3];
  }

  /**
   * Returns the "dot" product of this Quaternion and {@code b}:
   * <p>
   * {@code this.quat[0] * b.x + this.quat[1] * b.y + this.quat[2] * b.z + this.quat[3] * b.quat[3]}
   *
   * @param b the Quaternion
   */
  public float dotProduct(Quaternion b) {
    return this.quat[0] * b.quat[0] + this.quat[1] * b.quat[1] + this.quat[2] * b.quat[2] + this.quat[3] * b.quat[3];
  }

  /**
   * Returns the "dot" product of {@code a} and {@code b}:
   * <p>
   * {@code a.x * b.x + a.y * b.y + a.z * b.z + a.quat[3] * b.quat[3]}
   *
   * @param a the first Quaternion
   * @param b the second Quaternion
   */
  public static float dot(Quaternion a, Quaternion b) {
    return a.quat[0] * b.quat[0] + a.quat[1] * b.quat[1] + a.quat[2] * b.quat[2] + a.quat[3] * b.quat[3];
  }

  public void compose(Quaternion q) {
    multiply(q);
  }

  /**
   * Sets the value of this Quaternion to the Quaternion product of itself and {@code q1}, (i.e.,
   * {@code this = this * q1}).
   *
   * @param q1 the other Quaternion
   */
  public void multiply(Quaternion q1) {
    float x, y, w;

    w = this.quat[3] * q1.quat[3] - this.quat[0] * q1.quat[0] - this.quat[1] * q1.quat[1] - this.quat[2] * q1.quat[2];
    x = this.quat[3] * q1.quat[0] + q1.quat[3] * this.quat[0] + this.quat[1] * q1.quat[2] - this.quat[2] * q1.quat[1];
    y = this.quat[3] * q1.quat[1] + q1.quat[3] * this.quat[1] - this.quat[0] * q1.quat[2] + this.quat[2] * q1.quat[0];
    this.quat[2] =
        this.quat[3] * q1.quat[2] + q1.quat[3] * this.quat[2] + this.quat[0] * q1.quat[1] - this.quat[1] * q1.quat[0];
    this.quat[3] = w;
    this.quat[0] = x;
    this.quat[1] = y;
  }

  public static Quaternion compose(Quaternion q1, Quaternion q2) {
    return multiply(q1, q2);
  }

  /**
   * Returns the Quaternion which is product of quaternions {@code q1} and {@code q2}.
   *
   * @param q1 the first Quaternion
   * @param q2 the second Quaternion
   */
  public static Quaternion multiply(Quaternion q1, Quaternion q2) {
    float x, y, z, w;
    w = q1.quat[3] * q2.quat[3] - q1.quat[0] * q2.quat[0] - q1.quat[1] * q2.quat[1] - q1.quat[2] * q2.quat[2];
    x = q1.quat[3] * q2.quat[0] + q2.quat[3] * q1.quat[0] + q1.quat[1] * q2.quat[2] - q1.quat[2] * q2.quat[1];
    y = q1.quat[3] * q2.quat[1] + q2.quat[3] * q1.quat[1] - q1.quat[0] * q2.quat[2] + q1.quat[2] * q2.quat[0];
    z = q1.quat[3] * q2.quat[2] + q2.quat[3] * q1.quat[2] + q1.quat[0] * q2.quat[1] - q1.quat[1] * q2.quat[0];
    return new Quaternion(x, y, z, w);
  }

  /**
   * Returns the image of {@code v} by the rotation of this vector. Same as
   * {@code this.rotate(v).}
   *
   * @param v the Vector
   * @see #rotate(Vector)
   * @see #inverseRotate(Vector)
   */
  public Vector multiply(Vector v) {
    return this.rotate(v);
  }

  /**
   * Returns the image of {@code v} by the rotation {@code q1}. Same as
   * {@code q1.rotate(v).}
   *
   * @param q1 the Quaternion
   * @param v  the Vector
   * @see #rotate(Vector)
   * @see #inverseRotate(Vector)
   */
  public static Vector multiply(Quaternion q1, Vector v) {
    return q1.rotate(v);
  }

  /**
   * Multiplies this Quaternion by the inverse of Quaternion {@code q1} and places the value into this
   * Quaternion (i.e., {@code this = this * q^-1}). The value of the argument Quaternion is preserved.
   *
   * @param q1 the other Quaternion
   */
  public void multiplyInverse(Quaternion q1) {
    Quaternion tempQuaternion = new Quaternion(q1);
    tempQuaternion.invert();
    this.multiply(tempQuaternion);
  }

  /**
   * Returns the product of Quaternion {@code q1} by the inverse of Quaternion {@code q2} (i.e.,
   * {@code q1 * q2^-1}). The value of both argument quaternions is preserved.
   *
   * @param q1 the first Quaternion
   * @param q2 the second Quaternion
   */
  public static Quaternion multiplyInverse(Quaternion q1, Quaternion q2) {
    Quaternion tempQuaternion = new Quaternion(q2);
    tempQuaternion.invert();
    return Quaternion.multiply(q1, tempQuaternion);
  }

  /**
   * Returns the inverse Quaternion (inverse rotation).
   * <p>
   * The result hasGrabber a negated {@link #axis()} direction and the same {@link #angle()}.
   * <p>
   * A composition of a Quaternion and its {@link #inverse()} results in an identity function.
   * Use {@link #invert()} to actually modify the Quaternion.
   *
   * @see #invert()
   */
  public Quaternion inverse() {
    Quaternion tempQuaternion = new Quaternion(this);
    tempQuaternion.invert();
    return tempQuaternion;
  }

  /**
   * Sets the value of this Quaternion to the inverse of itself.
   *
   * @see #inverse()
   */
  public void invert() {
    float sqNorm = squaredNorm(this);
    this.quat[3] /= sqNorm;
    this.quat[0] /= -sqNorm;
    this.quat[1] /= -sqNorm;
    this.quat[2] /= -sqNorm;
  }

  /**
   * Sets the value of this Quaternion to the Quaternion inverse of {@code q1}.
   *
   * @param q1 the Quaternion to be inverted
   */
  public void invert(Quaternion q1) {
    float sqNorm = squaredNorm(q1);
    this.quat[3] = q1.quat[3] / sqNorm;
    this.quat[0] = -q1.quat[0] / sqNorm;
    this.quat[1] = -q1.quat[1] / sqNorm;
    this.quat[2] = -q1.quat[2] / sqNorm;
  }

  /**
   * Normalizes the value of this Quaternion in place and return its {@code norm}.
   */
  public float normalize() {
    float norm = (float) Math.sqrt(this.quat[0] * this.quat[0] + this.quat[1] * this.quat[1] + this.quat[2] * this.quat[2]
        + this.quat[3] * this.quat[3]);
    if (norm > 0.0f) {
      this.quat[0] /= norm;
      this.quat[1] /= norm;
      this.quat[2] /= norm;
      this.quat[3] /= norm;
    } else {
      this.quat[0] = (float) 0.0;
      this.quat[1] = (float) 0.0;
      this.quat[2] = (float) 0.0;
      this.quat[3] = (float) 1.0;
    }
    return norm;
  }

  /**
   * Returns the image of {@code v} by the Quaternion rotation.
   *
   * @param v the Vector
   */
  public Vector rotate(Vector v) {
    float q00 = 2.0f * this.quat[0] * this.quat[0];
    float q11 = 2.0f * this.quat[1] * this.quat[1];
    float q22 = 2.0f * this.quat[2] * this.quat[2];

    float q01 = 2.0f * this.quat[0] * this.quat[1];
    float q02 = 2.0f * this.quat[0] * this.quat[2];
    float q03 = 2.0f * this.quat[0] * this.quat[3];

    float q12 = 2.0f * this.quat[1] * this.quat[2];
    float q13 = 2.0f * this.quat[1] * this.quat[3];

    float q23 = 2.0f * this.quat[2] * this.quat[3];

    return new Vector((1.0f - q11 - q22) * v.vec[0] + (q01 - q23) * v.vec[1] + (q02 + q13) * v.vec[2],
        (q01 + q23) * v.vec[0] + (1.0f - q22 - q00) * v.vec[1] + (q12 - q03) * v.vec[2],
        (q02 - q13) * v.vec[0] + (q12 + q03) * v.vec[1] + (1.0f - q11 - q00) * v.vec[2]);
  }

  /**
   * Returns the image of {@code v} by the Quaternion {@link #inverse()} rotation.
   * <p>
   * {@link #rotate(Vector)} performs an inverse transformation.
   *
   * @param v the Vector
   */
  public Vector inverseRotate(Vector v) {
    Quaternion tempQuaternion = new Quaternion(this.quat[0], this.quat[1], this.quat[2], this.quat[3]);
    tempQuaternion.invert();
    return tempQuaternion.rotate(v);
  }

  /**
   * Sets the Quaternion as a rotation of {@link #axis() axis} and {@link #angle() angle} (in
   * radians).
   * <p>
   * The {@code axis} does not need to be normalized. A null {@code axis} will result in
   * an identity Quaternion.
   *
   * @param axis  the Vector representing the axis
   * @param angle the angle in radians
   */
  public void fromAxisAngle(Vector axis, float angle) {
    float norm = axis.magnitude();
    if (norm == 0) {
      // Null rotation
      this.quat[0] = 0.0f;
      this.quat[1] = 0.0f;
      this.quat[2] = 0.0f;
      this.quat[3] = 1.0f;
    } else {
      float sin_half_angle = (float) Math.sin(angle / 2.0f);
      this.quat[0] = sin_half_angle * axis.vec[0] / norm;
      this.quat[1] = sin_half_angle * axis.vec[1] / norm;
      this.quat[2] = sin_half_angle * axis.vec[2] / norm;
      this.quat[3] = (float) Math.cos(angle / 2.0f);
    }
  }

  /**
   * Same as {@code fromAxisAngle(new Vector(x,y,z), angle)}.
   *
   * @see #fromAxisAngle(Vector, float)
   */
  public void fromAxisAngle(float x, float y, float z, float angle) {
    fromAxisAngle(new Vector(x, y, z), angle);
  }

  /**
   * Same as {@link #fromEulerAngles(Vector)}.
   */
  public void fromTaitBryan(Vector angles) {
    fromEulerAngles(angles);
  }

  /**
   * Same as {@link #fromEulerAngles(float, float, float)}.
   */
  public void fromTaitBryan(float roll, float pitch, float yaw) {
    fromEulerAngles(roll, pitch, yaw);
  }

  /**
   * Convenience function that simply calls
   * {@code fromEulerAngles(angles.vec[0], angles.vec[1], angles.vec[2])}.
   *
   * @see #fromEulerAngles(float, float, float)
   * @see #eulerAngles()
   */
  public void fromEulerAngles(Vector angles) {
    fromEulerAngles(angles.vec[0], angles.vec[1], angles.vec[2]);
  }

  /**
   * Converts Euler rotation angles {@code roll}, {@code pitch} and {@code yaw},
   * respectively defined to the x, y and z axes, to this Quaternion. In the convention used
   * here these angles represent a composition of extrinsic rotations (rotations about the
   * reference frame axes), which is also known as {@link #taitBryanAngles()} (See
   * http://en.wikipedia.org/wiki/Euler_angles and
   * http://en.wikipedia.org/wiki/Tait-Bryan_angles). {@link #eulerAngles()} performs the
   * inverse operation.
   * <p>
   * Each rotation angle is converted to an axis-angle pair, with the axis corresponding
   * to one of the Euclidean axes. The axis-angle pairs are converted to quaternions and
   * multiplied together. The order of the rotations is: y,z,x which follows the
   * convention found here:
   * http://www.euclideanspace.com/maths/geometry/rotations/euler/index.htm.
   *
   * @see #eulerAngles()
   */
  public void fromEulerAngles(float roll, float pitch, float yaw) {
    Quaternion qx = new Quaternion(new Vector(1, 0, 0), roll);
    Quaternion qy = new Quaternion(new Vector(0, 1, 0), pitch);
    Quaternion qz = new Quaternion(new Vector(0, 0, 1), yaw);
    set(qy);
    multiply(qz);
    multiply(qx);
  }

  /**
   * Same as {@link #eulerAngles()}.
   */
  public Vector taitBryanAngles() {
    return eulerAngles();
  }

  /**
   * Converts this Quaternion to Euler rotation angles {@code roll}, {@code pitch} and
   * {@code yaw} in radians. {@link #fromEulerAngles(float, float, float)} performs the
   * inverse operation. The code was adapted from:
   * http://www.euclideanspace.com/maths/geometry/rotations/conversions/
   * quaternionToEuler/index.htm.
   * <p>
   * <b>Attention:</b> This method assumes that this Quaternion is normalized.
   *
   * @return the Vector holding the roll (x coordinate of the vector), pitch (y coordinate of
   * the vector) and yaw angles (z coordinate of the vector). <b>Note:</b> The
   * order of the rotations that would produce this Quaternion (i.e., as with
   * {@code fromEulerAngles(roll, pitch, yaw)}) is: y,z,x.
   * @see #fromEulerAngles(float, float, float)
   */
  public Vector eulerAngles() {
    float roll, pitch, yaw;
    float test = this.quat[0] * this.quat[1] + this.quat[2] * this.quat[3];
    if (test > 0.499) { // singularity at north pole
      pitch = 2 * (float) Math.atan2(this.quat[0], this.quat[3]);
      yaw = (float) Math.PI / 2;
      roll = 0;
      return new Vector(roll, pitch, yaw);
    }
    if (test < -0.499) { // singularity at south pole
      pitch = -2 * (float) Math.atan2(this.quat[0], this.quat[3]);
      yaw = -(float) Math.PI / 2;
      roll = 0;
      return new Vector(roll, pitch, yaw);
    }
    float sqx = this.quat[0] * this.quat[0];
    float sqy = this.quat[1] * this.quat[1];
    float sqz = this.quat[2] * this.quat[2];
    pitch = (float) Math.atan2(2 * this.quat[1] * this.quat[3] - 2 * this.quat[0] * this.quat[2], 1 - 2 * sqy - 2 * sqz);
    yaw = (float) Math.asin(2 * test);
    roll = (float) Math.atan2(2 * this.quat[0] * this.quat[3] - 2 * this.quat[1] * this.quat[2], 1 - 2 * sqx - 2 * sqz);
    return new Vector(roll, pitch, yaw);
  }

  /**
   * Sets the Quaternion as a rotation from the {@code from} direction to the {@code to}
   * direction.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. The selected axis is usually
   * orthogonal to {@code from} and {@code to}, minimizing the rotation angle. This method
   * is robust and can handle small or almost identical vectors.
   *
   * @see #fromAxisAngle(Vector, float)
   */
  public void fromTo(Vector from, Vector to) {
    float fromSqNorm = from.squaredNorm();
    float toSqNorm = to.squaredNorm();
    // Identity Quaternion when one vector is null
    if (fromSqNorm == 0 || toSqNorm == 0) {
      this.quat[0] = this.quat[1] = this.quat[2] = 0.0f;
      this.quat[3] = 1.0f;
    } else {

      Vector axis = from.cross(to);

      float axisSqNorm = axis.squaredNorm();

      // Aligned vectors, pick any axis, not aligned with from or to
      if (axisSqNorm == 0)
        axis = from.orthogonalVector();

      float angle = (float) Math.asin((float) Math.sqrt(axisSqNorm / (fromSqNorm * toSqNorm)));

      if (from.dot(to) < 0.0)
        angle = (float) Math.PI - angle;

      fromAxisAngle(axis, angle);
    }
  }

  /**
   * Same as {@code fromTo(new Vector(x1,y1,z1), new Vector(x2,y2,z2))}.
   *
   * @see #fromTo(Vector, Vector)
   */
  public void fromTo(float x1, float y1, float z1, float x2, float y2, float z2) {
    fromTo(new Vector(x1, y1, z1), new Vector(x2, y2, z2));
  }

  /**
   * Set the Quaternion from a (supposedly correct) 3x3 rotation matrix given in the upper left
   * 3x3 sub-matrix of the Matrix.
   *
   * @see #fromRotatedBasis(Vector, Vector, Vector)
   */
  public void fromMatrix(Matrix glMatrix) {
    Vector x = new Vector(glMatrix.mat[0], glMatrix.mat[4], glMatrix.mat[8]);
    Vector y = new Vector(glMatrix.mat[1], glMatrix.mat[5], glMatrix.mat[9]);
    Vector z = new Vector(glMatrix.mat[2], glMatrix.mat[6], glMatrix.mat[10]);
    fromRotatedBasis(x, y, z);
  }

  /**
   * Sets the Quaternion from the three rotated vectors of an orthogonal basis.
   * <p>
   * The three vectors do not have to be normalized but must be orthogonal and direct
   * (i,e., {@code X^Y=k*Z, with k>0}).
   *
   * @param X the first Vector
   * @param Y the second Vector
   * @param Z the third Vector
   * @see #fromRotatedBasis(Vector, Vector, Vector)
   * @see #Quaternion(Vector, Vector)
   */
  public void fromRotatedBasis(Vector X, Vector Y, Vector Z) {
    float threeXthree[][] = new float[3][3];
    float normX = X.magnitude();
    float normY = Y.magnitude();
    float normZ = Z.magnitude();

    for (int i = 0; i < 3; ++i) {
      threeXthree[i][0] = X.vec[i] / normX;
      threeXthree[i][1] = Y.vec[i] / normY;
      threeXthree[i][2] = Z.vec[i] / normZ;
    }

    // Here it comes: fromRotationMatrix(threeXthree):
    // Compute one plus the trace of the matrix
    float onePlusTrace = 1.0f + threeXthree[0][0] + threeXthree[1][1] + threeXthree[2][2];

    if (onePlusTrace > 0) {
      // Direct computation
      float s = (float) Math.sqrt(onePlusTrace) * 2.0f;
      this.quat[0] = (threeXthree[2][1] - threeXthree[1][2]) / s;
      this.quat[1] = (threeXthree[0][2] - threeXthree[2][0]) / s;
      this.quat[2] = (threeXthree[1][0] - threeXthree[0][1]) / s;
      this.quat[3] = 0.25f * s;
    } else {
      // Computation depends on major diagonal term
      if ((threeXthree[0][0] > threeXthree[1][1]) & (threeXthree[0][0] > threeXthree[2][2])) {
        float s = (float) Math.sqrt(1.0f + threeXthree[0][0] - threeXthree[1][1] - threeXthree[2][2]) * 2.0f;
        this.quat[0] = 0.25f * s;
        this.quat[1] = (threeXthree[0][1] + threeXthree[1][0]) / s;
        this.quat[2] = (threeXthree[0][2] + threeXthree[2][0]) / s;
        this.quat[3] = (threeXthree[1][2] - threeXthree[2][1]) / s;
      } else if (threeXthree[1][1] > threeXthree[2][2]) {
        float s = (float) Math.sqrt(1.0f + threeXthree[1][1] - threeXthree[0][0] - threeXthree[2][2]) * 2.0f;
        this.quat[0] = (threeXthree[0][1] + threeXthree[1][0]) / s;
        this.quat[1] = 0.25f * s;
        this.quat[2] = (threeXthree[1][2] + threeXthree[2][1]) / s;
        this.quat[3] = (threeXthree[0][2] - threeXthree[2][0]) / s;
      } else {
        float s = (float) Math.sqrt(1.0f + threeXthree[2][2] - threeXthree[0][0] - threeXthree[1][1]) * 2.0f;
        this.quat[0] = (threeXthree[0][2] + threeXthree[2][0]) / s;
        this.quat[1] = (threeXthree[1][2] + threeXthree[2][1]) / s;
        this.quat[2] = 0.25f * s;
        this.quat[3] = (threeXthree[0][1] - threeXthree[1][0]) / s;
      }
    }
    normalize();
  }

  /**
   * Returns the normalized axis direction of the rotation represented by the Quaternion.
   * <p>
   * The result is null for an identity Quaternion.
   *
   * @see #angle()
   */
  public Vector axis() {
    Vector res = new Vector(x(), y(), z());
    float sinus = res.magnitude();
    if (sinus != 0)
      res.divide(sinus);
    return res;
  }

  /**
   * Returns the {@code angle} (in radians) of the rotation represented by the Quaternion.
   * <p>
   * This value is always in the range {@code [0-pi]}. Larger rotational angles are
   * obtained by inverting the {@link #axis()} direction.
   *
   * @see #axis()
   */
  public float angle() {
    return 2.0f * (float) Math.acos(w());
  }

  public float angle2D() {
    return axis().vec[2] > 0 ? angle() : -angle();
  }

  /**
   * Returns the Matrix which represents the rotation matrix associated with the Quaternion.
   */
  public Matrix matrix() {
    float q00 = 2.0f * this.quat[0] * this.quat[0];
    float q11 = 2.0f * this.quat[1] * this.quat[1];
    float q22 = 2.0f * this.quat[2] * this.quat[2];

    float q01 = 2.0f * this.quat[0] * this.quat[1];
    float q02 = 2.0f * this.quat[0] * this.quat[2];
    float q03 = 2.0f * this.quat[0] * this.quat[3];

    float q12 = 2.0f * this.quat[1] * this.quat[2];
    float q13 = 2.0f * this.quat[1] * this.quat[3];
    float q23 = 2.0f * this.quat[2] * this.quat[3];

    float m00 = 1.0f - q11 - q22;
    float m10 = q01 - q23;
    float m20 = q02 + q13;

    float m01 = q01 + q23;
    float m11 = 1.0f - q22 - q00;
    float m21 = q12 - q03;

    float m02 = q02 - q13;
    float m12 = q12 + q03;
    float m22 = 1.0f - q11 - q00;

    float m03 = 0.0f;
    float m13 = 0.0f;
    float m23 = 0.0f;

    float m30 = 0.0f;
    float m31 = 0.0f;
    float m32 = 0.0f;
    float m33 = 1.0f;

    return new Matrix(m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33);
  }

  /**
   * Returns the associated inverse rotation Matrix. This is simply {@link #matrix()} of the
   * {@link #inverse()}.
   * <p>
   * <b>Attention:</b> The result is only valid until the next call to
   * {@link #inverseMatrix()}. Use it immediately (as in
   * {@code applyMatrix(q.inverseMatrix())}).
   */
  public Matrix inverseMatrix() {
    Quaternion tempQuaternion = new Quaternion(this.quat[0], this.quat[1], this.quat[2], this.quat[3]);
    tempQuaternion.invert();
    return tempQuaternion.matrix();
  }

  /**
   * Returns the logarithm of the Quaternion.
   *
   * @see #exp()
   */
  public Quaternion log() {
    // Warning: this method should not normalize the Quaternion
    float len = (float) Math
        .sqrt(this.quat[0] * this.quat[0] + this.quat[1] * this.quat[1] + this.quat[2] * this.quat[2]);

    if (len == 0)
      return new Quaternion(this.quat[0], this.quat[1], this.quat[2], 0.0f, false);
    else {
      float coef = (float) Math.acos(this.quat[3]) / len;
      return new Quaternion(this.quat[0] * coef, this.quat[1] * coef, this.quat[2] * coef, 0.0f, false);
    }
  }

  /**
   * Returns the exponential of the Quaternion.
   *
   * @see #log()
   */
  public Quaternion exp() {
    float theta = (float) Math
        .sqrt(this.quat[0] * this.quat[0] + this.quat[1] * this.quat[1] + this.quat[2] * this.quat[2]);

    if (theta == 0)
      return new Quaternion(this.quat[0], this.quat[1], this.quat[2], (float) Math.cos(theta));
    else {
      float coef = (float) Math.sin(theta) / theta;
      return new Quaternion(this.quat[0] * coef, this.quat[1] * coef, this.quat[2] * coef, (float) Math.cos(theta));
    }
  }

  /**
   * Returns a random unit Quaternion.
   * <p>
   * You can create a randomly directed unit vector using:
   * <p>
   * {@code Vector randomDir = new Vector(1.0f, 0.0f, 0.0f);} <br>
   * {@code randomDir = Quaternion.multiply(Quaternion.randomQuat(),
   * randomDir);}
   */
  public static Quaternion randomQuat() {
    float seed = (float) Math.random();
    float r1 = (float) Math.sqrt(1.0f - seed);
    float r2 = (float) Math.sqrt(seed);
    float t1 = 2.0f * (float) Math.PI * (float) Math.random();
    float t2 = 2.0f * (float) Math.PI * (float) Math.random();

    return new Quaternion((float) Math.sin(t1) * r1, (float) Math.cos(t1) * r1, (float) Math.sin(t2) * r2,
        (float) Math.cos(t2) * r2);
  }

  /**
   * Wrapper function that simply calls {@code slerp(a, b, t, true)}.
   * <p>
   * See {@link #slerp(Quaternion, Quaternion, float, boolean)} for details.
   */
  public static Quaternion slerp(Quaternion a, Quaternion b, float t) {
    return Quaternion.slerp(a, b, t, true);
  }

  /**
   * Returns the slerp interpolation of quaternions {@code a} and {@code b}, at time
   * {@code t}.
   * <p>
   * {@code t} should range in {@code [0,1]}. Result is {@code a} when {@code t=0} and
   * {@code b} when {@code t=1}.
   * <p>
   * When {@code allowFlip} is true (default) the slerp interpolation will always use the
   * "shortest path" between the quaternions' orientations, by "flipping" the source Quaternion
   * if needed (see {@link #negate()}).
   *
   * @param a         the first Quaternion
   * @param b         the second Quaternion
   * @param t         the t interpolation parameter
   * @param allowFlip tells whether or not the interpolation allows axis flip
   */
  public static Quaternion slerp(Quaternion a, Quaternion b, float t, boolean allowFlip) {
    // Warning: this method should not normalize the Quaternion
    float cosAngle = Quaternion.dot(a, b);

    float c1, c2;
    // Linear interpolation for close orientations
    if ((1.0 - Math.abs(cosAngle)) < 0.01) {
      c1 = 1.0f - t;
      c2 = t;
    } else {
      // Spherical interpolation
      float angle = (float) Math.acos(Math.abs(cosAngle));
      float sinAngle = (float) Math.sin(angle);
      c1 = (float) Math.sin(angle * (1.0f - t)) / sinAngle;
      c2 = (float) Math.sin(angle * t) / sinAngle;
    }

    // Use the shortest path
    if (allowFlip && (cosAngle < 0.0))
      c1 = -c1;

    return new Quaternion(c1 * a.quat[0] + c2 * b.quat[0], c1 * a.quat[1] + c2 * b.quat[1], c1 * a.quat[2] + c2 * b.quat[2],
        c1 * a.quat[3] + c2 * b.quat[3], false);
  }

  /**
   * Returns the slerp interpolation of the two quaternions {@code a} and {@code b}, at
   * time {@code t}, using tangents {@code tgA} and {@code tgB}.
   * <p>
   * The resulting Quaternion is "between" {@code a} and {@code b} (result is {@code a} when
   * {@code t=0} and {@code b} for {@code t=1}).
   * <p>
   * Use {@link #squadTangent(Quaternion, Quaternion, Quaternion)} to define the Quaternion tangents {@code tgA}
   * and {@code tgB}.
   *
   * @param a   the first Quaternion
   * @param tgA the first tangent Quaternion
   * @param tgB the second tangent Quaternion
   * @param b   the second Quaternion
   * @param t   the t interpolation parameter
   */
  public static Quaternion squad(Quaternion a, Quaternion tgA, Quaternion tgB, Quaternion b, float t) {
    Quaternion ab = Quaternion.slerp(a, b, t);
    Quaternion tg = Quaternion.slerp(tgA, tgB, t, false);
    return Quaternion.slerp(ab, tg, 2.0f * t * (1.0f - t), false);
  }

  /**
   * Simply returns {@code log(a. inverse() * b)}.
   * <p>
   * Useful for {@link #squadTangent(Quaternion, Quaternion, Quaternion)}.
   *
   * @param a the first Quaternion
   * @param b the second Quaternion
   */
  public static Quaternion lnDif(Quaternion a, Quaternion b) {
    Quaternion dif = a.inverse();
    dif.multiply(b);

    dif.normalize();
    return dif.log();
  }

  /**
   * Returns a tangent Quaternion for {@code center}, defined by {@code before} and
   * {@code after} quaternions.
   *
   * @param before the first Quaternion
   * @param center the second Quaternion
   * @param after  the third Quaternion
   */
  public static Quaternion squadTangent(Quaternion before, Quaternion center, Quaternion after) {
    Quaternion l1 = Quaternion.lnDif(center, before);
    Quaternion l2 = Quaternion.lnDif(center, after);
    Quaternion e = new Quaternion();

    e.quat[0] = -0.25f * (l1.quat[0] + l2.quat[0]);
    e.quat[1] = -0.25f * (l1.quat[1] + l2.quat[1]);
    e.quat[2] = -0.25f * (l1.quat[2] + l2.quat[2]);
    e.quat[3] = -0.25f * (l1.quat[3] + l2.quat[3]);

    return Quaternion.multiply(center, e.exp());
  }

  /**
   * Utility function that returns the squared norm of the Quaternion.
   */
  public static float squaredNorm(Quaternion q) {
    return (q.quat[0] * q.quat[0]) + (q.quat[1] * q.quat[1]) + (q.quat[2] * q.quat[2]) + (q.quat[3] * q.quat[3]);
  }

  public void print() {
    axis().print();
    System.out.println(angle());
  }
}
