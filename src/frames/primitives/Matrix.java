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

  public Matrix(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                float m9, float m10, float m11, float m12, float m13, float m14, float m15) {
    set(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15, true);
  }

  /**
   * 16 consecutive values that are used as the elements of a 4 x 4 column-major matrix.
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
   * Sets the matrix contents from the 16 entry float array. If {@code transpose} is false
   * the matrix contents are set in column major order, otherwise they're set in row-major
   * order.
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
   * Link {@code source} array to this matrix.
   *
   * @see #unLink()
   */
  public void link(float[] source) {
    _matrix = source;
  }

  /**
   * Unlinks this matrix if it was previously {@link #link(float[])}.
   */
  public void unLink() {
    float[] data = new float[16];
    get(data);
    set(data);
  }

  /**
   * Returns a copy of this matrix.
   */
  public Matrix get() {
    return new Matrix(this);
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
   * Copies the matrix contents into a 16 entry float array. If target is null (or not the
   * correct size), a new array will be created. Column or row major order is defined by
   * the {@code columnMajorOrder} boolean parameter.
   */
  public float[] get(float[] target, boolean columnMajorOrder) {
    if ((target == null) || (target.length != 16)) {
      target = new float[16];
    }
    if (columnMajorOrder) {
      target[0] = _matrix[0];
      target[1] = _matrix[1];
      target[2] = _matrix[2];
      target[3] = _matrix[3];

      target[4] = _matrix[4];
      target[5] = _matrix[5];
      target[6] = _matrix[6];
      target[7] = _matrix[7];

      target[8] = _matrix[8];
      target[9] = _matrix[9];
      target[10] = _matrix[10];
      target[11] = _matrix[11];

      target[12] = _matrix[12];
      target[13] = _matrix[13];
      target[14] = _matrix[14];
      target[15] = _matrix[15];
    } else {
      target[0] = _matrix[0];
      target[1] = _matrix[4];
      target[2] = _matrix[8];
      target[3] = _matrix[12];

      target[4] = _matrix[1];
      target[5] = _matrix[5];
      target[6] = _matrix[9];
      target[7] = _matrix[13];

      target[8] = _matrix[2];
      target[9] = _matrix[6];
      target[10] = _matrix[10];
      target[11] = _matrix[14];

      target[12] = _matrix[3];
      target[13] = _matrix[7];
      target[14] = _matrix[11];
      target[15] = _matrix[15];
    }
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
   * Sets the matrix contents from the 16 entry float array. Column or row major order
   * is defined by the {@code columnMajorOrder} boolean parameter.
   */
  public void set(float[] source, boolean columnMajorOrder) {
    if (source.length == 16) {
      if (columnMajorOrder) {
        _matrix[0] = source[0];
        _matrix[1] = source[1];
        _matrix[2] = source[2];
        _matrix[3] = source[3];

        _matrix[4] = source[4];
        _matrix[5] = source[5];
        _matrix[6] = source[6];
        _matrix[7] = source[7];

        _matrix[8] = source[8];
        _matrix[9] = source[9];
        _matrix[10] = source[10];
        _matrix[11] = source[11];

        _matrix[12] = source[12];
        _matrix[13] = source[13];
        _matrix[14] = source[14];
        _matrix[15] = source[15];
      } else {
        _matrix[0] = source[0];
        _matrix[1] = source[4];
        _matrix[2] = source[8];
        _matrix[3] = source[12];

        _matrix[4] = source[1];
        _matrix[5] = source[5];
        _matrix[6] = source[9];
        _matrix[7] = source[13];

        _matrix[8] = source[2];
        _matrix[9] = source[6];
        _matrix[10] = source[10];
        _matrix[11] = source[14];

        _matrix[12] = source[3];
        _matrix[13] = source[7];
        _matrix[14] = source[11];
        _matrix[15] = source[15];
      }
    }
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
    // applyTranspose(1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1);
    apply(1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1);
  }

  /**
   * Multiply this matrix by the rotation around y-axis matrix defined from {@code angle}.
   */
  public void rotateY(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    // applyTranspose(c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1);
    apply(c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1);
  }

  /**
   * Multiply this matrix by the rotation around z-axis matrix defined from {@code angle}.
   */
  public void rotateZ(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    // applyTranspose(c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    apply(c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
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

    apply((t * v0 * v0) + c, (t * v0 * v1) + (s * v2), (t * v0 * v2) - (s * v1), 0, (t * v0 * v1) - (s * v2),
        (t * v1 * v1) + c, (t * v1 * v2) + (s * v0), 0, (t * v0 * v2) + (s * v1), (t * v1 * v2) - (s * v0),
        (t * v2 * v2) + c, 0, 0, 0, 0, 1);
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code s}.
   */
  public void scale(float s) {
    // apply(s, 0, 0, 0, 0, s, 0, 0, 0, 0, s, 0, 0, 0, 0, 1);
    scale(s, s, s);
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code sx} and {@code sy}.
   */
  public void scale(float sx, float sy) {
    // apply(sx, 0, 0, 0, 0, sy, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    scale(sx, sy, 1);
  }

  /**
   * Multiply this matrix by the scaling matrix defined from {@code x}, {@code y} and
   * {@code z}.
   */
  public void scale(float x, float y, float z) {
    // apply(x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1);
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
    apply(1, 0, 0, 0, t, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  /**
   * Multiply this matrix by the y-shearing matrix defined from {@code angle}.
   */
  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    apply(1, t, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  /**
   * Multiply this matrix by the 16 consecutive float array defined by {@code source}.
   */
  public void apply(float[] source) {
    if (source != null) {
      if (source.length == 16) {
        apply(source[0], source[1], source[2], source[3], source[4], source[5], source[6], source[7], source[8], source[9],
            source[10], source[11], source[12], source[13], source[14], source[15]);
      }
    }
  }

  /**
   * Multiply this matrix by the one defined from {@code source}.
   */
  public void apply(Matrix matrix) {
    // applyTranspose(source.mat[0], source.mat[4], source.mat[8],
    // source.mat[12], source.mat[1], source.mat[5],
    // source.mat[9], source.mat[13], source.mat[2], source.mat[6],
    // source.mat[10], source.mat[14], source.mat[3],
    // source.mat[7], source.mat[11], source.mat[15]);
    // Same as the previous line:
    apply(matrix._matrix[0], matrix._matrix[1], matrix._matrix[2], matrix._matrix[3], matrix._matrix[4], matrix._matrix[5], matrix._matrix[6],
        matrix._matrix[7], matrix._matrix[8], matrix._matrix[9], matrix._matrix[10], matrix._matrix[11], matrix._matrix[12], matrix._matrix[13],
        matrix._matrix[14], matrix._matrix[15]);
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
   * Multiply this matrix by the 16 consecutive values that are used as the elements of a
   * 4 x 4 column-major matrix.
   */
  public void apply(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8, float m9,
                    float m10, float m11, float m12, float m13, float m14, float m15) {

    float r00 = _matrix[0] * m0 + _matrix[4] * m1 + _matrix[8] * m2 + _matrix[12] * m3;
    float r01 = _matrix[0] * m4 + _matrix[4] * m5 + _matrix[8] * m6 + _matrix[12] * m7;
    float r02 = _matrix[0] * m8 + _matrix[4] * m9 + _matrix[8] * m10 + _matrix[12] * m11;
    float r03 = _matrix[0] * m12 + _matrix[4] * m13 + _matrix[8] * m14 + _matrix[12] * m15;

    float r10 = _matrix[1] * m0 + _matrix[5] * m1 + _matrix[9] * m2 + _matrix[13] * m3;
    float r11 = _matrix[1] * m4 + _matrix[5] * m5 + _matrix[9] * m6 + _matrix[13] * m7;
    float r12 = _matrix[1] * m8 + _matrix[5] * m9 + _matrix[9] * m10 + _matrix[13] * m11;
    float r13 = _matrix[1] * m12 + _matrix[5] * m13 + _matrix[9] * m14 + _matrix[13] * m15;

    float r20 = _matrix[2] * m0 + _matrix[6] * m1 + _matrix[10] * m2 + _matrix[14] * m3;
    float r21 = _matrix[2] * m4 + _matrix[6] * m5 + _matrix[10] * m6 + _matrix[14] * m7;
    float r22 = _matrix[2] * m8 + _matrix[6] * m9 + _matrix[10] * m10 + _matrix[14] * m11;
    float r23 = _matrix[2] * m12 + _matrix[6] * m13 + _matrix[10] * m14 + _matrix[14] * m15;

    float r30 = _matrix[3] * m0 + _matrix[7] * m1 + _matrix[11] * m2 + _matrix[15] * m3;
    float r31 = _matrix[3] * m4 + _matrix[7] * m5 + _matrix[11] * m6 + _matrix[15] * m7;
    float r32 = _matrix[3] * m8 + _matrix[7] * m9 + _matrix[11] * m10 + _matrix[15] * m11;
    float r33 = _matrix[3] * m12 + _matrix[7] * m13 + _matrix[11] * m14 + _matrix[15] * m15;

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
   * Pre-multiply this matrix by the 16 consecutive float array defined by {@code source}.
   */
  public void preApply(float[] source) {
    if (source != null) {
      if (source.length == 16) {
        preApply(source[0], source[1], source[2], source[3], source[4], source[5], source[6], source[7], source[8],
            source[9], source[10], source[11], source[12], source[13], source[14], source[15]);
      }
    }
  }

  /**
   * Pre-multiply this matrix by the one defined from {@code left}.
   */
  public void preApply(Matrix left) {
    preApply(left._matrix[0], left._matrix[1], left._matrix[2], left._matrix[3], left._matrix[4], left._matrix[5], left._matrix[6], left._matrix[7],
        left._matrix[8], left._matrix[9], left._matrix[10], left._matrix[11], left._matrix[12], left._matrix[13], left._matrix[14], left._matrix[15]);
  }

  /**
   * Pre-multiply this matrix by the 16 consecutive values that are used as the elements
   * of a 4 x 4 column-major matrix.
   */
  public void preApply(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                       float m9, float m10, float m11, float m12, float m13, float m14, float m15) {
    float r00 = m0 * _matrix[0] + m4 * _matrix[1] + m8 * _matrix[2] + m12 * _matrix[3];
    float r01 = m0 * _matrix[4] + m4 * _matrix[5] + m8 * _matrix[6] + m12 * _matrix[7];
    float r02 = m0 * _matrix[8] + m4 * _matrix[9] + m8 * _matrix[10] + m12 * _matrix[11];
    float r03 = m0 * _matrix[12] + m4 * _matrix[13] + m8 * _matrix[14] + m12 * _matrix[15];

    float r10 = m1 * _matrix[0] + m5 * _matrix[1] + m9 * _matrix[2] + m13 * _matrix[3];
    float r11 = m1 * _matrix[4] + m5 * _matrix[5] + m9 * _matrix[6] + m13 * _matrix[7];
    float r12 = m1 * _matrix[8] + m5 * _matrix[9] + m9 * _matrix[10] + m13 * _matrix[11];
    float r13 = m1 * _matrix[12] + m5 * _matrix[13] + m9 * _matrix[14] + m13 * _matrix[15];

    float r20 = m2 * _matrix[0] + m6 * _matrix[1] + m10 * _matrix[2] + m14 * _matrix[3];
    float r21 = m2 * _matrix[4] + m6 * _matrix[5] + m10 * _matrix[6] + m14 * _matrix[7];
    float r22 = m2 * _matrix[8] + m6 * _matrix[9] + m10 * _matrix[10] + m14 * _matrix[11];
    float r23 = m2 * _matrix[12] + m6 * _matrix[13] + m10 * _matrix[14] + m14 * _matrix[15];

    float r30 = m3 * _matrix[0] + m7 * _matrix[1] + m11 * _matrix[2] + m15 * _matrix[3];
    float r31 = m3 * _matrix[4] + m7 * _matrix[5] + m11 * _matrix[6] + m15 * _matrix[7];
    float r32 = m3 * _matrix[8] + m7 * _matrix[9] + m11 * _matrix[10] + m15 * _matrix[11];
    float r33 = m3 * _matrix[12] + m7 * _matrix[13] + m11 * _matrix[14] + m15 * _matrix[15];

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
   * Same as {@code return multiply(source, null)}.
   *
   * @see #multiply(Vector, Vector)
   */
  public Vector multiply(Vector source) {
    return multiply(source, null);
  }

  /**
   * Multiply this matrix by the {@code source} Vector and stores the result in the
   * {@code target} Vector which is then returned.
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
   * Multiply a three or four element vector against this matrix. If out is null or not
   * length 3 or 4, a new float array (length 3) will be returned.
   */
  public float[] multiply(float[] source, float[] target) {
    if (target == null || target.length < 3) {
      target = new float[3];
    }
    if (source == target) {
      throw new RuntimeException("The source and target vectors used in " + "Matrix.mult() cannot be identical.");
    }
    if (target.length == 3) {
      target[0] = _matrix[0] * source[0] + _matrix[4] * source[1] + _matrix[8] * source[2] + _matrix[12];
      target[1] = _matrix[1] * source[0] + _matrix[5] * source[1] + _matrix[9] * source[2] + _matrix[13];
      target[2] = _matrix[2] * source[0] + _matrix[6] * source[1] + _matrix[10] * source[2] + _matrix[14];
    } else if (target.length > 3) {
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
   * Invert this matrix into {@code m}, i.e., doesn't modify this matrix.
   * <p>
   * {@code m} should be non-null.
   */
  public boolean invert(Matrix matrix) {
    float determinant = determinant();
    if (determinant == 0) {
      return false;
    }

    // first row
    float t00 = determinant3x3(_matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t01 = -determinant3x3(_matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t02 = determinant3x3(_matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t03 = -determinant3x3(_matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // second row
    float t10 = -determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t11 = determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t12 = -determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t13 = determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // third row
    float t20 = determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[7], _matrix[11], _matrix[15]);
    float t21 = -determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[3], _matrix[11], _matrix[15]);
    float t22 = determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[3], _matrix[7], _matrix[15]);
    float t23 = -determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[3], _matrix[7], _matrix[11]);

    // fourth row
    float t30 = -determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14]);
    float t31 = determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14]);
    float t32 = -determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14]);
    float t33 = determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10]);

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
    float t00 = determinant3x3(_matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t01 = -determinant3x3(_matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t02 = determinant3x3(_matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t03 = -determinant3x3(_matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // second row
    float t10 = -determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[6], _matrix[10], _matrix[14], _matrix[7], _matrix[11], _matrix[15]);
    float t11 = determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[2], _matrix[10], _matrix[14], _matrix[3], _matrix[11], _matrix[15]);
    float t12 = -determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[2], _matrix[6], _matrix[14], _matrix[3], _matrix[7], _matrix[15]);
    float t13 = determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[2], _matrix[6], _matrix[10], _matrix[3], _matrix[7], _matrix[11]);

    // third row
    float t20 = determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[7], _matrix[11], _matrix[15]);
    float t21 = -determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[3], _matrix[11], _matrix[15]);
    float t22 = determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[3], _matrix[7], _matrix[15]);
    float t23 = -determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[3], _matrix[7], _matrix[11]);

    // fourth row
    float t30 = -determinant3x3(_matrix[4], _matrix[8], _matrix[12], _matrix[5], _matrix[9], _matrix[13], _matrix[6], _matrix[10], _matrix[14]);
    float t31 = determinant3x3(_matrix[0], _matrix[8], _matrix[12], _matrix[1], _matrix[9], _matrix[13], _matrix[2], _matrix[10], _matrix[14]);
    float t32 = -determinant3x3(_matrix[0], _matrix[4], _matrix[12], _matrix[1], _matrix[5], _matrix[13], _matrix[2], _matrix[6], _matrix[14]);
    float t33 = determinant3x3(_matrix[0], _matrix[4], _matrix[8], _matrix[1], _matrix[5], _matrix[9], _matrix[2], _matrix[6], _matrix[10]);

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
  private float determinant3x3(float t00, float t01, float t02, float t10, float t11, float t12, float t20, float t21,
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
}
