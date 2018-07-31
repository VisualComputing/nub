/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.primitives;

/**
 * 4x4 matrix affine matrix implementation. Matrix is represented in column major order: |
 * m0 m4 m8 m12 | | m1 m5 m9 m13 | | m2 m6 m10 m14 | | m3 m7 m11 m15 |
 */
public class Matrix {
  /**
   * Returns whether or not this matrix matches other.
   *
   * @param matrix other matrix
   */
  public boolean matches(Matrix matrix) {
    boolean result = true;
    for (int i = 0; i < _matrix.length; i++) {
      if (_matrix[i] != matrix._matrix[i])
        result = false;
      break;
    }
    return result;
  }

  public float _matrix[] = new float[16];

  /**
   * Constructor for an identity matrix.
   */
  public Matrix() {
    reset();
  }

  /**
   * Same as {@code this(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, true)}.
   *
   * @see #Matrix(float, float, float, float, float, float, float, float, float, float, float, float, float, float, float, float, boolean)
   */
  public Matrix(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                float m9, float m10, float m11, float m12, float m13, float m14, float m15) {
    this(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, true);
  }

  /**
   * Same as {@code set(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, columnMajorOrder)}.
   *
   * @see #set(float, float, float, float, float, float, float, float, float, float, float, float, float, float, float, float, boolean)
   */
  public Matrix(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                float m9, float m10, float m11, float m12, float m13, float m14, float m15, boolean columnMajorOrder) {
    set(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, columnMajorOrder);
  }

  protected Matrix(Matrix matrix) {
    set(matrix);
  }

  /**
   * Same as {@code this(data, true)}.
   *
   * @see #Matrix(float[], boolean)
   */
  public Matrix(float[] source) {
    this(source, true);
  }

  /**
   * Sets the matrix contents from the 16 entry {@code source} float array given in
   * {@code columnMajorOrder} or row major order (if {@code columnMajorOrder} is {@code false}).
   */
  public Matrix(float[] source, boolean columnMajorOrder) {
    set(source, columnMajorOrder);
  }

  /**
   * Sets the 1st-row, 1st-column matrix entry.
   */
  public void setM00(float value) {
    _matrix[0] = value;
  }

  /**
   * Sets the 1st-row, 2nd-column matrix entry.
   */
  public void setM01(float value) {
    _matrix[4] = value;
  }

  /**
   * Sets the 1st-row, 3rd-column matrix entry.
   */
  public void setM02(float value) {
    _matrix[8] = value;
  }

  /**
   * Sets the 1st-row, 4th-column matrix entry.
   */
  public void setM03(float value) {
    _matrix[12] = value;
  }

  /**
   * Sets the 2nd-row, 1st-column matrix entry.
   */
  public void setM10(float value) {
    _matrix[1] = value;
  }

  /**
   * Sets the 2nd-row, 2nd-column matrix entry.
   */
  public void setM11(float value) {
    _matrix[5] = value;
  }

  /**
   * Sets the 2nd-row, 3rd-column matrix entry.
   */
  public void setM12(float value) {
    _matrix[9] = value;
  }

  /**
   * Sets the 2nd-row, 4th-column matrix entry.
   */
  public void setM13(float value) {
    _matrix[13] = value;
  }

  /**
   * Sets the 3rd-row, 1st-column matrix entry.
   */
  public void setM20(float value) {
    _matrix[2] = value;
  }

  /**
   * Sets the 3rd-row, 2nd-column matrix entry.
   */
  public void setM21(float value) {
    _matrix[6] = value;
  }

  /**
   * Sets the 3rd-row, 3rd-column matrix entry.
   */
  public void setM22(float value) {
    _matrix[10] = value;
  }

  /**
   * Sets the 3rd-row, 4th-column matrix entry.
   */
  public void setM23(float value) {
    _matrix[14] = value;
  }

  /**
   * Sets the 4th-row, 1st-column matrix entry.
   */
  public void setM30(float value) {
    _matrix[3] = value;
  }

  /**
   * Sets the 4th-row, 2nd-column matrix entry.
   */
  public void setM31(float value) {
    _matrix[7] = value;
  }

  /**
   * Sets the 4th-row, 3rd-column matrix entry.
   */
  public void setM32(float value) {
    _matrix[11] = value;
  }

  /**
   * Sets the 4th-row, 4th-column matrix entry.
   */
  public void setM33(float value) {
    _matrix[15] = value;
  }

  /**
   * Sets the 1st-row, 1st-column matrix entry.
   */
  public float m00() {
    return _matrix[0];
  }

  /**
   * Returns the 1st-row, 2nd-column matrix entry.
   */
  public float m01() {
    return _matrix[4];
  }

  /**
   * Returns the 1st-row, 3rd-column matrix entry.
   */
  public float m02() {
    return _matrix[8];
  }

  /**
   * Returns the 1st-row, 4th-column matrix entry.
   */
  public float m03() {
    return _matrix[12];
  }

  /**
   * Returns the 2nd-row, 1st-column matrix entry.
   */
  public float m10() {
    return _matrix[1];
  }

  /**
   * Returns the 2nd-row, 2nd-column matrix entry.
   */
  public float m11() {
    return _matrix[5];
  }

  /**
   * Returns the 2nd-row, 3rd-column matrix entry.
   */
  public float m12() {
    return _matrix[9];
  }

  /**
   * Returns the 2nd-row, 4th-column matrix entry.
   */
  public float m13() {
    return _matrix[13];
  }

  /**
   * Returns the 3rd-row, 1st-column matrix entry.
   */
  public float m20() {
    return _matrix[2];
  }

  /**
   * Returns the 3rd-row, 2nd-column matrix entry.
   */
  public float m21() {
    return _matrix[6];
  }

  /**
   * Returns the 3rd-row, 3rd-column matrix entry.
   */
  public float m22() {
    return _matrix[10];
  }

  /**
   * Returns the 3rd-row, 4th-column matrix entry.
   */
  public float m23() {
    return _matrix[14];
  }

  /**
   * Returns the 4th-row, 1st-column matrix entry.
   */
  public float m30() {
    return _matrix[3];
  }

  /**
   * Returns the 4th-row, 2nd-column matrix entry.
   */
  public float m31() {
    return _matrix[7];
  }

  /**
   * Returns the 4th-row, 3rd-column matrix entry.
   */
  public float m32() {
    return _matrix[11];
  }

  /**
   * Returns the 4th-row, 4th-column matrix entry.
   */
  public float m33() {
    return _matrix[15];
  }

  /**
   * Sets the identity matrix.
   */
  public void reset() {
    set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  /**
   * Returns a copy of this matrix.
   */
  public Matrix get() {
    return new Matrix(this);
  }

  /**
   * Internally used by both {@link #set(float[], boolean)} and {@link #get(float[], boolean)}.
   * <p>
   * Sets {@code target} array from {@code source} array according to {@code columnMajorOrder}.
   */
  protected void _set(float[] target, float[] source, boolean columnMajorOrder) {
    if (columnMajorOrder) {
      target[0] = source[0];
      target[1] = source[1];
      target[2] = source[2];
      target[3] = source[3];

      target[4] = source[4];
      target[5] = source[5];
      target[6] = source[6];
      target[7] = source[7];

      target[8] = source[8];
      target[9] = source[9];
      target[10] = source[10];
      target[11] = source[11];

      target[12] = source[12];
      target[13] = source[13];
      target[14] = source[14];
      target[15] = source[15];
    } else {
      target[0] = source[0];
      target[1] = source[4];
      target[2] = source[8];
      target[3] = source[12];

      target[4] = source[1];
      target[5] = source[5];
      target[6] = source[9];
      target[7] = source[13];

      target[8] = source[2];
      target[9] = source[6];
      target[10] = source[10];
      target[11] = source[14];

      target[12] = source[3];
      target[13] = source[7];
      target[14] = source[11];
      target[15] = source[15];
    }
  }

  /**
   * Same as {@code return get(target, true)}.
   *
   * @see #get(float[], boolean)
   */
  public float[] get(float[] target) {
    return get(target, true);
  }

  /**
   * Copies the matrix contents into a 16 entry {@code target} float array. If target is null
   * (or not the correct size), a new array will be created. Column or row major order is defined
   * by the {@code columnMajorOrder} boolean parameter.
   */
  public float[] get(float[] target, boolean columnMajorOrder) {
    if ((target == null) || (target.length != 16)) {
      target = new float[16];
    }
    _set(target, _matrix, columnMajorOrder);
    return target;
  }

  /**
   * Sets the matrix contents from the {@code matrix} contents.
   */
  public void set(Matrix matrix) {
    set(matrix._matrix[0], matrix._matrix[1], matrix._matrix[2], matrix._matrix[3], matrix._matrix[4], matrix._matrix[5], matrix._matrix[6], matrix._matrix[7], matrix._matrix[8],
        matrix._matrix[9], matrix._matrix[10], matrix._matrix[11], matrix._matrix[12], matrix._matrix[13], matrix._matrix[14], matrix._matrix[15]);
  }

  /**
   * Same as {@code set(source, true)}.
   *
   * @see #set(float[], boolean)
   */
  public void set(float[] source) {
    set(source, true);
  }

  /**
   * Sets the matrix contents from the 16 entry float {@code source} array. Column or row major
   * order is defined by the {@code columnMajorOrder} boolean parameter.
   */
  public void set(float[] source, boolean columnMajorOrder) {
    if (source.length == 16)
      _set(_matrix, source, columnMajorOrder);
  }

  public void set(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                  float m9, float m10, float m11, float m12, float m13, float m14, float m15) {
    set(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, true);
  }

  /**
   * Sets the column-major matrix contents from the given 16 consecutive values.
   */
  public void set(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                  float m9, float m10, float m11, float m12, float m13, float m14, float m15, boolean columnMajorOrder) {
    set(new float[]{m0, m1, m2, m3,
        m4, m5, m6, m7,
        m8, m9, m10, m11,
        m12, m13, m14, m15}, columnMajorOrder);
  }

  /**
   * Same as {@code translate(tx, ty, 0}.
   */
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  /**
   * Multiply this matrix by the translation matrix defined from {@code tx}, {@code ty}
   * and {@code tz}.
   */
  public void translate(float tx, float ty, float tz) {
    _matrix[12] += tx * _matrix[0] + ty * _matrix[4] + tz * _matrix[8];
    _matrix[13] += tx * _matrix[1] + ty * _matrix[5] + tz * _matrix[9];
    _matrix[14] += tx * _matrix[2] + ty * _matrix[6] + tz * _matrix[10];
    _matrix[15] += tx * _matrix[3] + ty * _matrix[7] + tz * _matrix[11];
  }

  /**
   * Same as {@code _rotateZ(angle)}.
   *
   * @see #rotateZ(float)
   */
  public void rotate(float angle) {
    rotateZ(angle);
  }

  /**
   * Multiply this matrix by the rotation around x-axis matrix defined from {@code angle}.
   */
  public void rotateX(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    apply(new Matrix(1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by the rotation around y-axis matrix defined from {@code angle}.
   */
  public void rotateY(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    apply(new Matrix(c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by the rotation around z-axis matrix defined from {@code angle}.
   */
  public void rotateZ(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    apply(new Matrix(c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by the rotation matrix defined from the {@code (v0,v1,v2)} and
   * {@code angle}.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
    float epsilon = 0.0001f;
    if (norm2 < epsilon) {
      // The vector is zero, cannot apply rotation.
      return;
    }

    if (Math.abs(norm2 - 1) > epsilon) {
      // The rotation vector is not normalized.
      float norm = (float) Math.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }

    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    float t = 1.0f - c;

    apply(new Matrix((t * v0 * v0) + c, (t * v0 * v1) + (s * v2), (t * v0 * v2) - (s * v1), 0, (t * v0 * v1) - (s * v2),
        (t * v1 * v1) + c, (t * v1 * v2) + (s * v0), 0, (t * v0 * v2) + (s * v1), (t * v1 * v2) - (s * v0),
        (t * v2 * v2) + c, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code s}.
   */
  public void scale(float s) {
    scale(s, s, s);
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code sx} and {@code sy}.
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code x}, {@code y} and
   * {@code z}.
   */
  public void scale(float x, float y, float z) {
    _matrix[0] *= x;
    _matrix[4] *= y;
    _matrix[8] *= z;
    _matrix[1] *= x;
    _matrix[5] *= y;
    _matrix[9] *= z;
    _matrix[2] *= x;
    _matrix[6] *= y;
    _matrix[10] *= z;
    _matrix[3] *= x;
    _matrix[7] *= y;
    _matrix[11] *= z;
  }

  /**
   * Multiply this matrix by the x-shearing matrix defined from {@code angle}.
   */
  public void shearX(float angle) {
    float t = (float) Math.tan(angle);
    apply(new Matrix(1, 0, 0, 0, t, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by the y-shearing matrix defined from {@code angle}.
   */
  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    apply(new Matrix(1, t, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));
  }

  /**
   * Multiply this matrix by {@code matrix}.
   */
  public void apply(Matrix matrix) {
    float r00 = _matrix[0] * matrix._matrix[0] + _matrix[4] * matrix._matrix[1] + _matrix[8] * matrix._matrix[2] + _matrix[12] * matrix._matrix[3];
    float r01 = _matrix[0] * matrix._matrix[4] + _matrix[4] * matrix._matrix[5] + _matrix[8] * matrix._matrix[6] + _matrix[12] * matrix._matrix[7];
    float r02 = _matrix[0] * matrix._matrix[8] + _matrix[4] * matrix._matrix[9] + _matrix[8] * matrix._matrix[10] + _matrix[12] * matrix._matrix[11];
    float r03 = _matrix[0] * matrix._matrix[12] + _matrix[4] * matrix._matrix[13] + _matrix[8] * matrix._matrix[14] + _matrix[12] * matrix._matrix[15];

    float r10 = _matrix[1] * matrix._matrix[0] + _matrix[5] * matrix._matrix[1] + _matrix[9] * matrix._matrix[2] + _matrix[13] * matrix._matrix[3];
    float r11 = _matrix[1] * matrix._matrix[4] + _matrix[5] * matrix._matrix[5] + _matrix[9] * matrix._matrix[6] + _matrix[13] * matrix._matrix[7];
    float r12 = _matrix[1] * matrix._matrix[8] + _matrix[5] * matrix._matrix[9] + _matrix[9] * matrix._matrix[10] + _matrix[13] * matrix._matrix[11];
    float r13 = _matrix[1] * matrix._matrix[12] + _matrix[5] * matrix._matrix[13] + _matrix[9] * matrix._matrix[14] + _matrix[13] * matrix._matrix[15];

    float r20 = _matrix[2] * matrix._matrix[0] + _matrix[6] * matrix._matrix[1] + _matrix[10] * matrix._matrix[2] + _matrix[14] * matrix._matrix[3];
    float r21 = _matrix[2] * matrix._matrix[4] + _matrix[6] * matrix._matrix[5] + _matrix[10] * matrix._matrix[6] + _matrix[14] * matrix._matrix[7];
    float r22 = _matrix[2] * matrix._matrix[8] + _matrix[6] * matrix._matrix[9] + _matrix[10] * matrix._matrix[10] + _matrix[14] * matrix._matrix[11];
    float r23 = _matrix[2] * matrix._matrix[12] + _matrix[6] * matrix._matrix[13] + _matrix[10] * matrix._matrix[14] + _matrix[14] * matrix._matrix[15];

    float r30 = _matrix[3] * matrix._matrix[0] + _matrix[7] * matrix._matrix[1] + _matrix[11] * matrix._matrix[2] + _matrix[15] * matrix._matrix[3];
    float r31 = _matrix[3] * matrix._matrix[4] + _matrix[7] * matrix._matrix[5] + _matrix[11] * matrix._matrix[6] + _matrix[15] * matrix._matrix[7];
    float r32 = _matrix[3] * matrix._matrix[8] + _matrix[7] * matrix._matrix[9] + _matrix[11] * matrix._matrix[10] + _matrix[15] * matrix._matrix[11];
    float r33 = _matrix[3] * matrix._matrix[12] + _matrix[7] * matrix._matrix[13] + _matrix[11] * matrix._matrix[14] + _matrix[15] * matrix._matrix[15];

    _matrix[0] = r00;
    _matrix[4] = r01;
    _matrix[8] = r02;
    _matrix[12] = r03;
    _matrix[1] = r10;
    _matrix[5] = r11;
    _matrix[9] = r12;
    _matrix[13] = r13;
    _matrix[2] = r20;
    _matrix[6] = r21;
    _matrix[10] = r22;
    _matrix[14] = r23;
    _matrix[3] = r30;
    _matrix[7] = r31;
    _matrix[11] = r32;
    _matrix[15] = r33;
  }

  /**
   * Returns {@code a x b}.
   */
  public static Matrix multiply(Matrix a, Matrix b) {
    Matrix c = new Matrix();
    multiply(a, b, c);
    return c;
  }

  /**
   * Define {@code c} as {@code a x b}.
   */
  public static void multiply(Matrix a, Matrix b, Matrix c) {
    c._matrix[0] = a._matrix[0] * b._matrix[0] + a._matrix[4] * b._matrix[1] + a._matrix[8] * b._matrix[2] + a._matrix[12] * b._matrix[3];
    c._matrix[4] = a._matrix[0] * b._matrix[4] + a._matrix[4] * b._matrix[5] + a._matrix[8] * b._matrix[6] + a._matrix[12] * b._matrix[7];
    c._matrix[8] = a._matrix[0] * b._matrix[8] + a._matrix[4] * b._matrix[9] + a._matrix[8] * b._matrix[10] + a._matrix[12] * b._matrix[11];
    c._matrix[12] = a._matrix[0] * b._matrix[12] + a._matrix[4] * b._matrix[13] + a._matrix[8] * b._matrix[14] + a._matrix[12] * b._matrix[15];

    c._matrix[1] = a._matrix[1] * b._matrix[0] + a._matrix[5] * b._matrix[1] + a._matrix[9] * b._matrix[2] + a._matrix[13] * b._matrix[3];
    c._matrix[5] = a._matrix[1] * b._matrix[4] + a._matrix[5] * b._matrix[5] + a._matrix[9] * b._matrix[6] + a._matrix[13] * b._matrix[7];
    c._matrix[9] = a._matrix[1] * b._matrix[8] + a._matrix[5] * b._matrix[9] + a._matrix[9] * b._matrix[10] + a._matrix[13] * b._matrix[11];
    c._matrix[13] = a._matrix[1] * b._matrix[12] + a._matrix[5] * b._matrix[13] + a._matrix[9] * b._matrix[14] + a._matrix[13] * b._matrix[15];

    c._matrix[2] = a._matrix[2] * b._matrix[0] + a._matrix[6] * b._matrix[1] + a._matrix[10] * b._matrix[2] + a._matrix[14] * b._matrix[3];
    c._matrix[6] = a._matrix[2] * b._matrix[4] + a._matrix[6] * b._matrix[5] + a._matrix[10] * b._matrix[6] + a._matrix[14] * b._matrix[7];
    c._matrix[10] = a._matrix[2] * b._matrix[8] + a._matrix[6] * b._matrix[9] + a._matrix[10] * b._matrix[10] + a._matrix[14] * b._matrix[11];
    c._matrix[14] = a._matrix[2] * b._matrix[12] + a._matrix[6] * b._matrix[13] + a._matrix[10] * b._matrix[14] + a._matrix[14] * b._matrix[15];

    c._matrix[3] = a._matrix[3] * b._matrix[0] + a._matrix[7] * b._matrix[1] + a._matrix[11] * b._matrix[2] + a._matrix[15] * b._matrix[3];
    c._matrix[7] = a._matrix[3] * b._matrix[4] + a._matrix[7] * b._matrix[5] + a._matrix[11] * b._matrix[6] + a._matrix[15] * b._matrix[7];
    c._matrix[11] = a._matrix[3] * b._matrix[8] + a._matrix[7] * b._matrix[9] + a._matrix[11] * b._matrix[10] + a._matrix[15] * b._matrix[11];
    c._matrix[15] = a._matrix[3] * b._matrix[12] + a._matrix[7] * b._matrix[13] + a._matrix[11] * b._matrix[14] + a._matrix[15] * b._matrix[15];
  }

  /**
   * Same as {@code return multiply(source, null)}.
   *
   * @see #multiply(Vector, Vector)
   */
  public Vector multiply(Vector source) {
    return multiply(source, null);
  }

  /**
   * Multiply this matrix by the {@code source} vector and stores the result in the
   * {@code target} vector which is then returned.
   */
  public Vector multiply(Vector source, Vector target) {
    if (target == null) {
      target = new Vector();
    }
    target.set(_matrix[0] * source.x() + _matrix[4] * source.y() + _matrix[8] * source.z() + _matrix[12],
        _matrix[1] * source.x() + _matrix[5] * source.y() + _matrix[9] * source.z() + _matrix[13],
        _matrix[2] * source.x() + _matrix[6] * source.y() + _matrix[10] * source.z() + _matrix[14]);
    return target;
  }

  /**
   * Multiply a three or four element vector {@code source} against this matrix and store the result
   * in {@code target}. If {@code target} is null or not length 3 or 4, a new float array (length 3)
   * will be returned.
   */
  public float[] multiply(float[] source, float[] target) {
    if (target == null || target.length < 3) {
      target = new float[3];
    }
    if (source == target) {
      throw new RuntimeException("The source and target vectors used in Matrix multiply() cannot be the same.");
    }
    if (target.length == 3) {
      target[0] = _matrix[0] * source[0] + _matrix[4] * source[1] + _matrix[8] * source[2] + _matrix[12];
      target[1] = _matrix[1] * source[0] + _matrix[5] * source[1] + _matrix[9] * source[2] + _matrix[13];
      target[2] = _matrix[2] * source[0] + _matrix[6] * source[1] + _matrix[10] * source[2] + _matrix[14];
    } else {
      target[0] = _matrix[0] * source[0] + _matrix[4] * source[1] + _matrix[8] * source[2] + _matrix[12] * source[3];
      target[1] = _matrix[1] * source[0] + _matrix[5] * source[1] + _matrix[9] * source[2] + _matrix[13] * source[3];
      target[2] = _matrix[2] * source[0] + _matrix[6] * source[1] + _matrix[10] * source[2] + _matrix[14] * source[3];
      target[3] = _matrix[3] * source[0] + _matrix[7] * source[1] + _matrix[11] * source[2] + _matrix[15] * source[3];
    }
    return target;
  }

  /**
   * Transpose this matrix.
   */
  public void transpose() {
    float temp;
    temp = _matrix[4];
    _matrix[4] = _matrix[1];
    _matrix[1] = temp;
    temp = _matrix[8];
    _matrix[8] = _matrix[2];
    _matrix[2] = temp;
    temp = _matrix[12];
    _matrix[12] = _matrix[3];
    _matrix[3] = temp;
    temp = _matrix[9];
    _matrix[9] = _matrix[6];
    _matrix[6] = temp;
    temp = _matrix[13];
    _matrix[13] = _matrix[7];
    _matrix[7] = temp;
    temp = _matrix[14];
    _matrix[14] = _matrix[11];
    _matrix[11] = temp;
  }

  /**
   * Invert this matrix into {@code matrix}, i.e., doesn't modify this matrix.
   * <p>
   * {@code matrix} should be non-null.
   */
  public boolean invert(Matrix matrix) {
    float determinant = determinant();
    if (determinant == 0) {
      return false;
    }

    // first row
    float t00 = _determinant3x3(_matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t01 = -_determinant3x3(_matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t02 = _determinant3x3(_matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t03 = -_determinant3x3(_matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // second row
    float t10 = -_determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t11 = _determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t12 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t13 = _determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // third row
    float t20 = _determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[7], _matrix[11], _matrix[15]);
    float t21 = -_determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[3], _matrix[11], _matrix[15]);
    float t22 = _determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[3], _matrix[7], _matrix[15]);
    float t23 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[3], _matrix[7], _matrix[11]);

    // fourth row
    float t30 = -_determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14]);
    float t31 = _determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14]);
    float t32 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14]);
    float t33 = _determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10]);

    // transpose and divide by the determinant
    matrix._matrix[0] = t00 / determinant;
    matrix._matrix[4] = t10 / determinant;
    matrix._matrix[8] = t20 / determinant;
    matrix._matrix[12] = t30 / determinant;

    matrix._matrix[1] = t01 / determinant;
    matrix._matrix[5] = t11 / determinant;
    matrix._matrix[9] = t21 / determinant;
    matrix._matrix[13] = t31 / determinant;

    matrix._matrix[2] = t02 / determinant;
    matrix._matrix[6] = t12 / determinant;
    matrix._matrix[10] = t22 / determinant;
    matrix._matrix[14] = t32 / determinant;

    matrix._matrix[3] = t03 / determinant;
    matrix._matrix[7] = t13 / determinant;
    matrix._matrix[11] = t23 / determinant;
    matrix._matrix[15] = t33 / determinant;

    return true;
  }

  /**
   * Invert this matrix.
   *
   * @return true if successful
   */
  public boolean invert() {
    float determinant = determinant();
    if (determinant == 0) {
      return false;
    }

    // first row
    float t00 = _determinant3x3(_matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t01 = -_determinant3x3(_matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t02 = _determinant3x3(_matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t03 = -_determinant3x3(_matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // second row
    float t10 = -_determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t11 = _determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t12 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t13 = _determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // third row
    float t20 = _determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[7], _matrix[11], _matrix[15]);
    float t21 = -_determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[3], _matrix[11], _matrix[15]);
    float t22 = _determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[3], _matrix[7], _matrix[15]);
    float t23 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[3], _matrix[7], _matrix[11]);

    // fourth row
    float t30 = -_determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14]);
    float t31 = _determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14]);
    float t32 = -_determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14]);
    float t33 = _determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10]);

    // transpose and divide by the determinant
    _matrix[0] = t00 / determinant;
    _matrix[4] = t10 / determinant;
    _matrix[8] = t20 / determinant;
    _matrix[12] = t30 / determinant;

    _matrix[1] = t01 / determinant;
    _matrix[5] = t11 / determinant;
    _matrix[9] = t21 / determinant;
    _matrix[13] = t31 / determinant;

    _matrix[2] = t02 / determinant;
    _matrix[6] = t12 / determinant;
    _matrix[10] = t22 / determinant;
    _matrix[14] = t32 / determinant;

    _matrix[3] = t03 / determinant;
    _matrix[7] = t13 / determinant;
    _matrix[11] = t23 / determinant;
    _matrix[15] = t33 / determinant;

    return true;
  }

  /**
   * Calculate the determinant of a 3x3 matrix.
   *
   * @return result
   */
  protected float _determinant3x3(float t00, float t01, float t02, float t10, float t11, float t12, float t20, float t21,
                                  float t22) {
    return (t00 * (t11 * t22 - t12 * t21) + t01 * (t12 * t20 - t10 * t22) + t02 * (t10 * t21 - t11 * t20));
  }

  /**
   * @return the determinant of the matrix
   */
  public float determinant() {
    float f = _matrix[0] * ((_matrix[5] * _matrix[10] * _matrix[15] + _matrix[9] * _matrix[14] * _matrix[7] + _matrix[13] * _matrix[6] * _matrix[11])
        - _matrix[13] * _matrix[10] * _matrix[7] - _matrix[5] * _matrix[14] * _matrix[11] - _matrix[9] * _matrix[6] * _matrix[15]);
    f -= _matrix[4] * ((_matrix[1] * _matrix[10] * _matrix[15] + _matrix[9] * _matrix[14] * _matrix[3] + _matrix[13] * _matrix[2] * _matrix[11])
        - _matrix[13] * _matrix[10] * _matrix[3] - _matrix[1] * _matrix[14] * _matrix[11] - _matrix[9] * _matrix[2] * _matrix[15]);
    f += _matrix[8] * ((_matrix[1] * _matrix[6] * _matrix[15] + _matrix[5] * _matrix[14] * _matrix[3] + _matrix[13] * _matrix[2] * _matrix[7])
        - _matrix[13] * _matrix[6] * _matrix[3] - _matrix[1] * _matrix[14] * _matrix[7] - _matrix[5] * _matrix[2] * _matrix[15]);
    f -= _matrix[12] * ((_matrix[1] * _matrix[6] * _matrix[11] + _matrix[5] * _matrix[10] * _matrix[3] + _matrix[9] * _matrix[2] * _matrix[7])
        - _matrix[9] * _matrix[6] * _matrix[3] - _matrix[1] * _matrix[10] * _matrix[7] - _matrix[5] * _matrix[2] * _matrix[11]);
    return f;
  }

  /**
   * Print this matrix contents onto the console.
   */
  public void print() {
    System.out.println(
        _matrix[0] + " " + _matrix[4] + " " + _matrix[8] + " " + _matrix[12] + "\n" + _matrix[1] + " " + _matrix[5] + " " + _matrix[9] + " " + _matrix[13]
            + "\n" + _matrix[2] + " " + _matrix[6] + " " + _matrix[10] + " " + _matrix[14] + "\n" + _matrix[3] + " " + _matrix[7] + " " + _matrix[11]
            + " " + _matrix[15] + "\n");
  }

  /**
   * Returns a perspective projection matrix from the given parameters.
   * <p>
   * Compute the {@code magnitude} as {@code tan(field-of-view / 2)} if you want to set the matrix from the field-of-view.
   * <p>
   * All parameter values should be positive, but {@code magnitude} which may be negative in case you want to invert
   * the projected image across the eye y-axis.
   *
   * @see #orthographic(float, float, float, float)
   * @see #view(Vector, Quaternion)
   */
  public static Matrix perspective(float zNear, float zFar, float aspectRatio, float magnitude) {
    // same as gluPerspective( 180*fieldOfView()/PI, aspectRatio(), zNear(), zFar() );
    Matrix projection = new Matrix();
    // all non null coefficients were set to 0 in constructor
    projection._matrix[0] = 1 / (Math.abs(magnitude) * aspectRatio);
    projection._matrix[5] = 1 / magnitude;
    projection._matrix[10] = (zNear + zFar) / (zNear - zFar);
    projection._matrix[11] = -1;
    projection._matrix[14] = 2 * zNear * zFar / (zNear - zFar);
    projection._matrix[15] = 0;
    return projection;
  }

  /**
   * Returns an orthographic projection matrix from the given parameters.
   * <p>
   * All parameter values should be positive, but {@code halfHeight} which may be negative in case you want to invert
   * the projected image across the eye y-axis.
   *
   * @see #perspective(float, float, float, float)
   * @see #view(Vector, Quaternion)
   */
  public static Matrix orthographic(float zNear, float zFar, float halfWidth, float halfHeight) {
    // same as glOrtho( -w, w, -h, h, zNear(), zFar() );
    Matrix projection = new Matrix();
    projection._matrix[0] = 1 / halfWidth;
    projection._matrix[5] = 1 / halfHeight;
    projection._matrix[10] = -2 / (zFar - zNear);
    projection._matrix[11] = 0;
    projection._matrix[14] = -(zFar + zNear) / (zFar - zNear);
    projection._matrix[15] = 1;
    return projection;
  }

  /**
   * Returns the inverse of the matrix associated with the eye {@code position} and
   * {@code orientation}.
   * <p>
   * The view matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using a projection matrix.
   *
   * @see #perspective(float, float, float, float)
   * @see #orthographic(float, float, float, float)
   */
  public static Matrix view(Vector position, Quaternion orientation) {
    Matrix view = new Matrix();

    float q00 = 2.0f * orientation._quaternion[0] * orientation._quaternion[0];
    float q11 = 2.0f * orientation._quaternion[1] * orientation._quaternion[1];
    float q22 = 2.0f * orientation._quaternion[2] * orientation._quaternion[2];

    float q01 = 2.0f * orientation._quaternion[0] * orientation._quaternion[1];
    float q02 = 2.0f * orientation._quaternion[0] * orientation._quaternion[2];
    float q03 = 2.0f * orientation._quaternion[0] * orientation._quaternion[3];

    float q12 = 2.0f * orientation._quaternion[1] * orientation._quaternion[2];
    float q13 = 2.0f * orientation._quaternion[1] * orientation._quaternion[3];
    float q23 = 2.0f * orientation._quaternion[2] * orientation._quaternion[3];

    view._matrix[0] = 1.0f - q11 - q22;
    view._matrix[1] = q01 - q23;
    view._matrix[2] = q02 + q13;
    view._matrix[3] = 0.0f;

    view._matrix[4] = q01 + q23;
    view._matrix[5] = 1.0f - q22 - q00;
    view._matrix[6] = q12 - q03;
    view._matrix[7] = 0.0f;

    view._matrix[8] = q02 - q13;
    view._matrix[9] = q12 + q03;
    view._matrix[10] = 1.0f - q11 - q00;
    view._matrix[11] = 0.0f;

    Vector t = orientation.inverseRotate(position);

    view._matrix[12] = -t._vector[0];
    view._matrix[13] = -t._vector[1];
    view._matrix[14] = -t._vector[2];
    view._matrix[15] = 1.0f;

    return view;
  }
}