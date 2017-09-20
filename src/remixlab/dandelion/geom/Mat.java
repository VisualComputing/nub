/*******************************************************************************
 * dandelion_tree (version 1.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/

package remixlab.dandelion.geom;

import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;
import remixlab.util.Util;

/**
 * 4x4 matrix affine matrix implementation. Matrix is represented in column major order: |
 * m0 m4 m8 m12 | | m1 m5 m9 m13 | | m2 m6 m10 m14 | | m3 m7 m11 m15 |
 */
public class Mat implements Linkable {
  /**
   * Array col major representation
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(this.mat[0]).append(this.mat[1]).append(this.mat[2]).append(this.mat[3])
        .append(this.mat[4]).append(this.mat[5]).append(this.mat[6]).append(this.mat[7]).append(this.mat[8])
        .append(this.mat[9]).append(this.mat[10]).append(this.mat[11]).append(this.mat[12]).append(this.mat[13])
        .append(this.mat[14]).append(this.mat[15]).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    Mat other = (Mat) obj;
    return new EqualsBuilder().append(this.mat[0], other.mat[0]).append(this.mat[1], other.mat[1])
        .append(this.mat[2], other.mat[2]).append(this.mat[3], other.mat[3]).append(this.mat[4], other.mat[4])
        .append(this.mat[5], other.mat[5]).append(this.mat[6], other.mat[6]).append(this.mat[7], other.mat[7])
        .append(this.mat[8], other.mat[8]).append(this.mat[9], other.mat[9]).append(this.mat[10], other.mat[10])
        .append(this.mat[11], other.mat[11]).append(this.mat[12], other.mat[12]).append(this.mat[13], other.mat[13])
        .append(this.mat[14], other.mat[14]).append(this.mat[15], other.mat[15]).isEquals();
  }

  public float mat[] = new float[16];

  /**
   * Constructor for an identity matrix.
   */
  public Mat() {
    reset();
  }

  /**
   * 16 consecutive values that are used as the elements of a 4 x 4 column-major matrix.
   */
  public Mat(float _m0, float _m1, float _m2, float _m3, float _m4, float _m5, float _m6, float _m7, float _m8,
             float _m9, float _m10, float _m11, float _m12, float _m13, float _m14, float _m15) {
    set(_m0, _m1, _m2, _m3, _m4, _m5, _m6, _m7, _m8, _m9, _m10, _m11, _m12, _m13, _m14, _m15);
  }

  protected Mat(Mat matrix) {
    set(matrix);
  }

  /**
   * Same as {@code this(data, false)}.
   *
   * @see #Mat(float[], boolean)
   */
  public Mat(float[] data) {
    this(data, false);
  }

  /**
   * Sets the matrix contents from the 16 entry float array. If {@code transpose} is false
   * the matrix contents are set in column major order, otherwise they're set in row-major
   * order.
   */
  public Mat(float[] data, boolean transposed) {
    if (transposed)
      setTransposed(data);
    else
      set(data);
  }

  /**
   * Sets the 1st-row, 1st-column matrix entry.
   */
  public void setM00(float v) {
    mat[0] = v;
  }

  /**
   * Sets the 1st-row, 2nd-column matrix entry.
   */
  public void setM01(float v) {
    mat[4] = v;
  }

  /**
   * Sets the 1st-row, 3rd-column matrix entry.
   */
  public void setM02(float v) {
    mat[8] = v;
  }

  /**
   * Sets the 1st-row, 4th-column matrix entry.
   */
  public void setM03(float v) {
    mat[12] = v;
  }

  /**
   * Sets the 2nd-row, 1st-column matrix entry.
   */
  public void setM10(float v) {
    mat[1] = v;
  }

  /**
   * Sets the 2nd-row, 2nd-column matrix entry.
   */
  public void setM11(float v) {
    mat[5] = v;
  }

  /**
   * Sets the 2nd-row, 3rd-column matrix entry.
   */
  public void setM12(float v) {
    mat[9] = v;
  }

  /**
   * Sets the 2nd-row, 4th-column matrix entry.
   */
  public void setM13(float v) {
    mat[13] = v;
  }

  /**
   * Sets the 3rd-row, 1st-column matrix entry.
   */
  public void setM20(float v) {
    mat[2] = v;
  }

  /**
   * Sets the 3rd-row, 2nd-column matrix entry.
   */
  public void setM21(float v) {
    mat[6] = v;
  }

  /**
   * Sets the 3rd-row, 3rd-column matrix entry.
   */
  public void setM22(float v) {
    mat[10] = v;
  }

  /**
   * Sets the 3rd-row, 4th-column matrix entry.
   */
  public void setM23(float v) {
    mat[14] = v;
  }

  /**
   * Sets the 4th-row, 1st-column matrix entry.
   */
  public void setM30(float v) {
    mat[3] = v;
  }

  /**
   * Sets the 4th-row, 2nd-column matrix entry.
   */
  public void setM31(float v) {
    mat[7] = v;
  }

  /**
   * Sets the 4th-row, 3rd-column matrix entry.
   */
  public void setM32(float v) {
    mat[11] = v;
  }

  /**
   * Sets the 4th-row, 4th-column matrix entry.
   */
  public void setM33(float v) {
    mat[15] = v;
  }

  /**
   * Sets the 1st-row, 1st-column matrix entry.
   */
  public float m00() {
    return mat[0];
  }

  /**
   * Returns the 1st-row, 2nd-column matrix entry.
   */
  public float m01() {
    return mat[4];
  }

  /**
   * Returns the 1st-row, 3rd-column matrix entry.
   */
  public float m02() {
    return mat[8];
  }

  /**
   * Returns the 1st-row, 4th-column matrix entry.
   */
  public float m03() {
    return mat[12];
  }

  /**
   * Returns the 2nd-row, 1st-column matrix entry.
   */
  public float m10() {
    return mat[1];
  }

  /**
   * Returns the 2nd-row, 2nd-column matrix entry.
   */
  public float m11() {
    return mat[5];
  }

  /**
   * Returns the 2nd-row, 3rd-column matrix entry.
   */
  public float m12() {
    return mat[9];
  }

  /**
   * Returns the 2nd-row, 4th-column matrix entry.
   */
  public float m13() {
    return mat[13];
  }

  /**
   * Returns the 3rd-row, 1st-column matrix entry.
   */
  public float m20() {
    return mat[2];
  }

  /**
   * Returns the 3rd-row, 2nd-column matrix entry.
   */
  public float m21() {
    return mat[6];
  }

  /**
   * Returns the 3rd-row, 3rd-column matrix entry.
   */
  public float m22() {
    return mat[10];
  }

  /**
   * Returns the 3rd-row, 4th-column matrix entry.
   */
  public float m23() {
    return mat[14];
  }

  /**
   * Returns the 4th-row, 1st-column matrix entry.
   */
  public float m30() {
    return mat[3];
  }

  /**
   * Returns the 4th-row, 2nd-column matrix entry.
   */
  public float m31() {
    return mat[7];
  }

  /**
   * Returns the 4th-row, 3rd-column matrix entry.
   */
  public float m32() {
    return mat[11];
  }

  /**
   * Returns the 4th-row, 4th-column matrix entry.
   */
  public float m33() {
    return mat[15];
  }

  /**
   * Sets the identity matrix.
   */
  @Override
  public void reset() {
    set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
  }

  /**
   * Retubs the matrix contents as a 16 entry float array.
   */
  public float[] getData() {
    return mat;
  }

  @Override
  public void link(float[] source) {
    mat = source;
  }

  @Override
  public void unLink() {
    float[] data = new float[16];
    get(data);
    set(data);
  }

  /**
   * Returns a copy of this Matrix.
   */
  @Override
  public Mat get() {
    return new Mat(this);
  }

  /**
   * Copies the matrix contents into a 16 entry float array. If target is null (or not the
   * correct size), a new array will be created.
   */
  @Override
  public float[] get(float[] target) {
    if ((target == null) || (target.length != 16)) {
      target = new float[16];
    }
    target[0] = mat[0];
    target[1] = mat[1];
    target[2] = mat[2];
    target[3] = mat[3];

    target[4] = mat[4];
    target[5] = mat[5];
    target[6] = mat[6];
    target[7] = mat[7];

    target[8] = mat[8];
    target[9] = mat[9];
    target[10] = mat[10];
    target[11] = mat[11];

    target[12] = mat[12];
    target[13] = mat[13];
    target[14] = mat[14];
    target[15] = mat[15];

    return target;
  }

  /**
   * Copies the transposed matrix contents into a 16 entry float array. If target is null
   * (or not the correct size), a new array will be created.
   */
  public float[] getTransposed(float[] rowMajor) {
    if ((rowMajor == null) || (rowMajor.length != 16)) {
      rowMajor = new float[16];
    }
    rowMajor[0] = mat[0];
    rowMajor[1] = mat[4];
    rowMajor[2] = mat[8];
    rowMajor[3] = mat[12];

    rowMajor[4] = mat[1];
    rowMajor[5] = mat[5];
    rowMajor[6] = mat[9];
    rowMajor[7] = mat[13];

    rowMajor[8] = mat[2];
    rowMajor[9] = mat[6];
    rowMajor[10] = mat[10];
    rowMajor[11] = mat[14];

    rowMajor[12] = mat[3];
    rowMajor[13] = mat[7];
    rowMajor[14] = mat[11];
    rowMajor[15] = mat[15];

    return rowMajor;
  }

  @Override
  public void set(Linkable src) {
    if (!(src instanceof Mat))
      throw new RuntimeException("src should be an instance of Mat");
    set((Mat) src);
  }

  /**
   * Sets the matrix contents from the {@code src} matrix contents.
   */
  public void set(Mat src) {
    set(src.mat[0], src.mat[1], src.mat[2], src.mat[3], src.mat[4], src.mat[5], src.mat[6], src.mat[7], src.mat[8],
        src.mat[9], src.mat[10], src.mat[11], src.mat[12], src.mat[13], src.mat[14], src.mat[15]);
  }

  @Override
  public void set(float[] source) {
    if (source.length == 16) {
      mat[0] = source[0];
      mat[1] = source[1];
      mat[2] = source[2];
      mat[3] = source[3];

      mat[4] = source[4];
      mat[5] = source[5];
      mat[6] = source[6];
      mat[7] = source[7];

      mat[8] = source[8];
      mat[9] = source[9];
      mat[10] = source[10];
      mat[11] = source[11];

      mat[12] = source[12];
      mat[13] = source[13];
      mat[14] = source[14];
      mat[15] = source[15];
    }
  }

  /**
   * Sets the row-major matrix contents from the given 16 consecutive values.
   *
   * @see #set(float[])
   */
  public void setTransposed(float[] rowMajor) {
    if (rowMajor.length == 16) {
      mat[0] = rowMajor[0];
      mat[1] = rowMajor[4];
      mat[2] = rowMajor[8];
      mat[3] = rowMajor[12];

      mat[4] = rowMajor[1];
      mat[5] = rowMajor[5];
      mat[6] = rowMajor[9];
      mat[7] = rowMajor[13];

      mat[8] = rowMajor[2];
      mat[9] = rowMajor[6];
      mat[10] = rowMajor[10];
      mat[11] = rowMajor[14];

      mat[12] = rowMajor[3];
      mat[13] = rowMajor[7];
      mat[14] = rowMajor[11];
      mat[15] = rowMajor[15];
    }
  }

  /**
   * Sets the column-major matrix contents from the given 16 consecutive values.
   */
  public void set(float _m0, float _m1, float _m2, float _m3, float _m4, float _m5, float _m6, float _m7, float _m8,
                  float _m9, float _m10, float _m11, float _m12, float _m13, float _m14, float _m15) {
    this.mat[0] = _m0;
    this.mat[1] = _m1;
    this.mat[2] = _m2;
    this.mat[3] = _m3;
    this.mat[4] = _m4;
    this.mat[5] = _m5;
    this.mat[6] = _m6;
    this.mat[7] = _m7;
    this.mat[8] = _m8;
    this.mat[9] = _m9;
    this.mat[10] = _m10;
    this.mat[11] = _m11;
    this.mat[12] = _m12;
    this.mat[13] = _m13;
    this.mat[14] = _m14;
    this.mat[15] = _m15;
  }

  /**
   * Sets the matrix contents from the given elements where first "index" is row, second
   * is column, e.g., _m20 corresponds to the element located at the third row and first
   * column of the matrix.
   */
  public void setTransposed(float _m00, float _m01, float _m02, float _m03, float _m10, float _m11, float _m12,
                            float _m13, float _m20, float _m21, float _m22, float _m23, float _m30, float _m31, float _m32, float _m33) {
    this.mat[0] = _m00;
    this.mat[4] = _m01;
    this.mat[8] = _m02;
    this.mat[12] = _m03;
    this.mat[1] = _m10;
    this.mat[5] = _m11;
    this.mat[9] = _m12;
    this.mat[13] = _m13;
    this.mat[2] = _m20;
    this.mat[6] = _m21;
    this.mat[10] = _m22;
    this.mat[14] = _m23;
    this.mat[3] = _m30;
    this.mat[7] = _m31;
    this.mat[11] = _m32;
    this.mat[15] = _m33;
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
    mat[12] += tx * mat[0] + ty * mat[4] + tz * mat[8];
    mat[13] += tx * mat[1] + ty * mat[5] + tz * mat[9];
    mat[14] += tx * mat[2] + ty * mat[6] + tz * mat[10];
    mat[15] += tx * mat[3] + ty * mat[7] + tz * mat[11];
  }

  /**
   * Same as {@code rotateZ(angle)}.
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
    if (norm2 < Util.FLOAT_EPS) {
      // The vector is zero, cannot apply rotation.
      return;
    }

    if (Math.abs(norm2 - 1) > Util.FLOAT_EPS) {
      // The rotation vector is not normalized.
      float norm = (float) Math.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }

    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);
    float t = 1.0f - c;

    // applyTranspose((t*v0*v0) + c, (t*v0*v1) - (s*v2), (t*v0*v2) + (s*v1), 0,
    // (t*v0*v1) + (s*v2), (t*v1*v1) + c, (t*v1*v2) - (s*v0), 0,
    // (t*v0*v2) - (s*v1), (t*v1*v2) + (s*v0), (t*v2*v2) + c, 0,
    // 0, 0, 0, 1);

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
    mat[0] *= x;
    mat[4] *= y;
    mat[8] *= z;
    mat[1] *= x;
    mat[5] *= y;
    mat[9] *= z;
    mat[2] *= x;
    mat[6] *= y;
    mat[10] *= z;
    mat[3] *= x;
    mat[7] *= y;
    mat[11] *= z;
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
   * Multiply this matrix by the transposed matrix defined from the 16 consecutive float
   * {@code source} array.
   */
  public void applyTransposed(float[] rowMajor) {
    if (rowMajor != null) {
      if (rowMajor.length == 16) {
        applyTransposed(rowMajor[0], rowMajor[1], rowMajor[2], rowMajor[3], rowMajor[4], rowMajor[5], rowMajor[6],
            rowMajor[7], rowMajor[8], rowMajor[9], rowMajor[10], rowMajor[11], rowMajor[12], rowMajor[13], rowMajor[14],
            rowMajor[15]);
      }
    }
  }

  /**
   * Multiply this matrix by the one defined from {@code source}.
   */
  public void apply(Mat source) {
    // applyTranspose(source.mat[0], source.mat[4], source.mat[8],
    // source.mat[12], source.mat[1], source.mat[5],
    // source.mat[9], source.mat[13], source.mat[2], source.mat[6],
    // source.mat[10], source.mat[14], source.mat[3],
    // source.mat[7], source.mat[11], source.mat[15]);
    // Same as the previous line:
    apply(source.mat[0], source.mat[1], source.mat[2], source.mat[3], source.mat[4], source.mat[5], source.mat[6],
        source.mat[7], source.mat[8], source.mat[9], source.mat[10], source.mat[11], source.mat[12], source.mat[13],
        source.mat[14], source.mat[15]);
  }

  /**
   * Returns {@code a x b}.
   */
  public static Mat multiply(Mat a, Mat b) {
    Mat c = new Mat();
    multiply(a, b, c);
    return c;
  }

  /**
   * Define {@code c} as {@code a x b}.
   */
  public static void multiply(Mat a, Mat b, Mat c) {
    c.mat[0] = a.mat[0] * b.mat[0] + a.mat[4] * b.mat[1] + a.mat[8] * b.mat[2] + a.mat[12] * b.mat[3];
    c.mat[4] = a.mat[0] * b.mat[4] + a.mat[4] * b.mat[5] + a.mat[8] * b.mat[6] + a.mat[12] * b.mat[7];
    c.mat[8] = a.mat[0] * b.mat[8] + a.mat[4] * b.mat[9] + a.mat[8] * b.mat[10] + a.mat[12] * b.mat[11];
    c.mat[12] = a.mat[0] * b.mat[12] + a.mat[4] * b.mat[13] + a.mat[8] * b.mat[14] + a.mat[12] * b.mat[15];

    c.mat[1] = a.mat[1] * b.mat[0] + a.mat[5] * b.mat[1] + a.mat[9] * b.mat[2] + a.mat[13] * b.mat[3];
    c.mat[5] = a.mat[1] * b.mat[4] + a.mat[5] * b.mat[5] + a.mat[9] * b.mat[6] + a.mat[13] * b.mat[7];
    c.mat[9] = a.mat[1] * b.mat[8] + a.mat[5] * b.mat[9] + a.mat[9] * b.mat[10] + a.mat[13] * b.mat[11];
    c.mat[13] = a.mat[1] * b.mat[12] + a.mat[5] * b.mat[13] + a.mat[9] * b.mat[14] + a.mat[13] * b.mat[15];

    c.mat[2] = a.mat[2] * b.mat[0] + a.mat[6] * b.mat[1] + a.mat[10] * b.mat[2] + a.mat[14] * b.mat[3];
    c.mat[6] = a.mat[2] * b.mat[4] + a.mat[6] * b.mat[5] + a.mat[10] * b.mat[6] + a.mat[14] * b.mat[7];
    c.mat[10] = a.mat[2] * b.mat[8] + a.mat[6] * b.mat[9] + a.mat[10] * b.mat[10] + a.mat[14] * b.mat[11];
    c.mat[14] = a.mat[2] * b.mat[12] + a.mat[6] * b.mat[13] + a.mat[10] * b.mat[14] + a.mat[14] * b.mat[15];

    c.mat[3] = a.mat[3] * b.mat[0] + a.mat[7] * b.mat[1] + a.mat[11] * b.mat[2] + a.mat[15] * b.mat[3];
    c.mat[7] = a.mat[3] * b.mat[4] + a.mat[7] * b.mat[5] + a.mat[11] * b.mat[6] + a.mat[15] * b.mat[7];
    c.mat[11] = a.mat[3] * b.mat[8] + a.mat[7] * b.mat[9] + a.mat[11] * b.mat[10] + a.mat[15] * b.mat[11];
    c.mat[15] = a.mat[3] * b.mat[12] + a.mat[7] * b.mat[13] + a.mat[11] * b.mat[14] + a.mat[15] * b.mat[15];
  }

  /**
   * Multiply this matrix by the 16 consecutive values that are used as the elements of a
   * 4 x 4 column-major matrix.
   */
  public void apply(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8, float m9,
                    float m10, float m11, float m12, float m13, float m14, float m15) {

    float r00 = mat[0] * m0 + mat[4] * m1 + mat[8] * m2 + mat[12] * m3;
    float r01 = mat[0] * m4 + mat[4] * m5 + mat[8] * m6 + mat[12] * m7;
    float r02 = mat[0] * m8 + mat[4] * m9 + mat[8] * m10 + mat[12] * m11;
    float r03 = mat[0] * m12 + mat[4] * m13 + mat[8] * m14 + mat[12] * m15;

    float r10 = mat[1] * m0 + mat[5] * m1 + mat[9] * m2 + mat[13] * m3;
    float r11 = mat[1] * m4 + mat[5] * m5 + mat[9] * m6 + mat[13] * m7;
    float r12 = mat[1] * m8 + mat[5] * m9 + mat[9] * m10 + mat[13] * m11;
    float r13 = mat[1] * m12 + mat[5] * m13 + mat[9] * m14 + mat[13] * m15;

    float r20 = mat[2] * m0 + mat[6] * m1 + mat[10] * m2 + mat[14] * m3;
    float r21 = mat[2] * m4 + mat[6] * m5 + mat[10] * m6 + mat[14] * m7;
    float r22 = mat[2] * m8 + mat[6] * m9 + mat[10] * m10 + mat[14] * m11;
    float r23 = mat[2] * m12 + mat[6] * m13 + mat[10] * m14 + mat[14] * m15;

    float r30 = mat[3] * m0 + mat[7] * m1 + mat[11] * m2 + mat[15] * m3;
    float r31 = mat[3] * m4 + mat[7] * m5 + mat[11] * m6 + mat[15] * m7;
    float r32 = mat[3] * m8 + mat[7] * m9 + mat[11] * m10 + mat[15] * m11;
    float r33 = mat[3] * m12 + mat[7] * m13 + mat[11] * m14 + mat[15] * m15;

    mat[0] = r00;
    mat[4] = r01;
    mat[8] = r02;
    mat[12] = r03;
    mat[1] = r10;
    mat[5] = r11;
    mat[9] = r12;
    mat[13] = r13;
    mat[2] = r20;
    mat[6] = r21;
    mat[10] = r22;
    mat[14] = r23;
    mat[3] = r30;
    mat[7] = r31;
    mat[11] = r32;
    mat[15] = r33;
  }

  /**
   * Multiply this matrix by the 16 consecutive values that are used as the elements of a
   * 4 x 4 row-major matrix. First "index" is row, second is column, e.g., n20 corresponds
   * to the element located at the third row and first column of the matrix.
   */
  public void applyTransposed(float n00, float n01, float n02, float n03, float n10, float n11, float n12, float n13,
                              float n20, float n21, float n22, float n23, float n30, float n31, float n32, float n33) {

    float r00 = mat[0] * n00 + mat[4] * n10 + mat[8] * n20 + mat[12] * n30;
    float r01 = mat[0] * n01 + mat[4] * n11 + mat[8] * n21 + mat[12] * n31;
    float r02 = mat[0] * n02 + mat[4] * n12 + mat[8] * n22 + mat[12] * n32;
    float r03 = mat[0] * n03 + mat[4] * n13 + mat[8] * n23 + mat[12] * n33;

    float r10 = mat[1] * n00 + mat[5] * n10 + mat[9] * n20 + mat[13] * n30;
    float r11 = mat[1] * n01 + mat[5] * n11 + mat[9] * n21 + mat[13] * n31;
    float r12 = mat[1] * n02 + mat[5] * n12 + mat[9] * n22 + mat[13] * n32;
    float r13 = mat[1] * n03 + mat[5] * n13 + mat[9] * n23 + mat[13] * n33;

    float r20 = mat[2] * n00 + mat[6] * n10 + mat[10] * n20 + mat[14] * n30;
    float r21 = mat[2] * n01 + mat[6] * n11 + mat[10] * n21 + mat[14] * n31;
    float r22 = mat[2] * n02 + mat[6] * n12 + mat[10] * n22 + mat[14] * n32;
    float r23 = mat[2] * n03 + mat[6] * n13 + mat[10] * n23 + mat[14] * n33;

    float r30 = mat[3] * n00 + mat[7] * n10 + mat[11] * n20 + mat[15] * n30;
    float r31 = mat[3] * n01 + mat[7] * n11 + mat[11] * n21 + mat[15] * n31;
    float r32 = mat[3] * n02 + mat[7] * n12 + mat[11] * n22 + mat[15] * n32;
    float r33 = mat[3] * n03 + mat[7] * n13 + mat[11] * n23 + mat[15] * n33;

    mat[0] = r00;
    mat[4] = r01;
    mat[8] = r02;
    mat[12] = r03;
    mat[1] = r10;
    mat[5] = r11;
    mat[9] = r12;
    mat[13] = r13;
    mat[2] = r20;
    mat[6] = r21;
    mat[10] = r22;
    mat[14] = r23;
    mat[3] = r30;
    mat[7] = r31;
    mat[11] = r32;
    mat[15] = r33;
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
   * Pre-multiply this matrix by the transposed matrix defined from the 16 consecutive
   * float {@code rowMajor} array.
   */
  public void preApplyTransposed(float[] rowMajor) {
    if (rowMajor != null) {
      if (rowMajor.length == 16) {
        preApplyTransposed(rowMajor[0], rowMajor[1], rowMajor[2], rowMajor[3], rowMajor[4], rowMajor[5], rowMajor[6],
            rowMajor[7], rowMajor[8], rowMajor[9], rowMajor[10], rowMajor[11], rowMajor[12], rowMajor[13], rowMajor[14],
            rowMajor[15]);
      }
    }
  }

  /**
   * Pre-multiply this matrix by the one defined from {@code source}.
   */
  public void preApply(Mat left) {
    preApply(left.mat[0], left.mat[1], left.mat[2], left.mat[3], left.mat[4], left.mat[5], left.mat[6], left.mat[7],
        left.mat[8], left.mat[9], left.mat[10], left.mat[11], left.mat[12], left.mat[13], left.mat[14], left.mat[15]);
  }

  /**
   * Pre-multiply this matrix by the 16 consecutive values that are used as the elements
   * of a 4 x 4 column-major matrix.
   */
  public void preApply(float m0, float m1, float m2, float m3, float m4, float m5, float m6, float m7, float m8,
                       float m9, float m10, float m11, float m12, float m13, float m14, float m15) {
    float r00 = m0 * mat[0] + m4 * mat[1] + m8 * mat[2] + m12 * mat[3];
    float r01 = m0 * mat[4] + m4 * mat[5] + m8 * mat[6] + m12 * mat[7];
    float r02 = m0 * mat[8] + m4 * mat[9] + m8 * mat[10] + m12 * mat[11];
    float r03 = m0 * mat[12] + m4 * mat[13] + m8 * mat[14] + m12 * mat[15];

    float r10 = m1 * mat[0] + m5 * mat[1] + m9 * mat[2] + m13 * mat[3];
    float r11 = m1 * mat[4] + m5 * mat[5] + m9 * mat[6] + m13 * mat[7];
    float r12 = m1 * mat[8] + m5 * mat[9] + m9 * mat[10] + m13 * mat[11];
    float r13 = m1 * mat[12] + m5 * mat[13] + m9 * mat[14] + m13 * mat[15];

    float r20 = m2 * mat[0] + m6 * mat[1] + m10 * mat[2] + m14 * mat[3];
    float r21 = m2 * mat[4] + m6 * mat[5] + m10 * mat[6] + m14 * mat[7];
    float r22 = m2 * mat[8] + m6 * mat[9] + m10 * mat[10] + m14 * mat[11];
    float r23 = m2 * mat[12] + m6 * mat[13] + m10 * mat[14] + m14 * mat[15];

    float r30 = m3 * mat[0] + m7 * mat[1] + m11 * mat[2] + m15 * mat[3];
    float r31 = m3 * mat[4] + m7 * mat[5] + m11 * mat[6] + m15 * mat[7];
    float r32 = m3 * mat[8] + m7 * mat[9] + m11 * mat[10] + m15 * mat[11];
    float r33 = m3 * mat[12] + m7 * mat[13] + m11 * mat[14] + m15 * mat[15];

    mat[0] = r00;
    mat[4] = r01;
    mat[8] = r02;
    mat[12] = r03;
    mat[1] = r10;
    mat[5] = r11;
    mat[9] = r12;
    mat[13] = r13;
    mat[2] = r20;
    mat[6] = r21;
    mat[10] = r22;
    mat[14] = r23;
    mat[3] = r30;
    mat[7] = r31;
    mat[11] = r32;
    mat[15] = r33;
  }

  /**
   * Pre-multiply this matrix by the 16 consecutive values that are used as the elements
   * of a 4 x 4 row-major matrix. First "index" is row, second is column, e.g., n20
   * corresponds to the element located at the third row and first column of the matrix.
   */
  public void preApplyTransposed(float n00, float n01, float n02, float n03, float n10, float n11, float n12, float n13,
                                 float n20, float n21, float n22, float n23, float n30, float n31, float n32, float n33) {

    float r00 = n00 * mat[0] + n01 * mat[1] + n02 * mat[2] + n03 * mat[3];
    float r01 = n00 * mat[4] + n01 * mat[5] + n02 * mat[6] + n03 * mat[7];
    float r02 = n00 * mat[8] + n01 * mat[9] + n02 * mat[10] + n03 * mat[11];
    float r03 = n00 * mat[12] + n01 * mat[13] + n02 * mat[14] + n03 * mat[15];

    float r10 = n10 * mat[0] + n11 * mat[1] + n12 * mat[2] + n13 * mat[3];
    float r11 = n10 * mat[4] + n11 * mat[5] + n12 * mat[6] + n13 * mat[7];
    float r12 = n10 * mat[8] + n11 * mat[9] + n12 * mat[10] + n13 * mat[11];
    float r13 = n10 * mat[12] + n11 * mat[13] + n12 * mat[14] + n13 * mat[15];

    float r20 = n20 * mat[0] + n21 * mat[1] + n22 * mat[2] + n23 * mat[3];
    float r21 = n20 * mat[4] + n21 * mat[5] + n22 * mat[6] + n23 * mat[7];
    float r22 = n20 * mat[8] + n21 * mat[9] + n22 * mat[10] + n23 * mat[11];
    float r23 = n20 * mat[12] + n21 * mat[13] + n22 * mat[14] + n23 * mat[15];

    float r30 = n30 * mat[0] + n31 * mat[1] + n32 * mat[2] + n33 * mat[3];
    float r31 = n30 * mat[4] + n31 * mat[5] + n32 * mat[6] + n33 * mat[7];
    float r32 = n30 * mat[8] + n31 * mat[9] + n32 * mat[10] + n33 * mat[11];
    float r33 = n30 * mat[12] + n31 * mat[13] + n32 * mat[14] + n33 * mat[15];

    mat[0] = r00;
    mat[4] = r01;
    mat[8] = r02;
    mat[12] = r03;
    mat[1] = r10;
    mat[5] = r11;
    mat[9] = r12;
    mat[13] = r13;
    mat[2] = r20;
    mat[6] = r21;
    mat[10] = r22;
    mat[14] = r23;
    mat[3] = r30;
    mat[7] = r31;
    mat[11] = r32;
    mat[15] = r33;
  }

  /**
   * Same as {@code return multiply(source, null)}.
   *
   * @see #multiply(Vec, Vec)
   */
  public Vec multiply(Vec source) {
    return multiply(source, null);
  }

  /**
   * Multiply this matrix by the {@code source} Vec and stores the result in the
   * {@code target} Vec which is then returned.
   */
  public Vec multiply(Vec source, Vec target) {
    if (target == null) {
      target = new Vec();
    }
    target.set(mat[0] * source.x() + mat[4] * source.y() + mat[8] * source.z() + mat[12],
        mat[1] * source.x() + mat[5] * source.y() + mat[9] * source.z() + mat[13],
        mat[2] * source.x() + mat[6] * source.y() + mat[10] * source.z() + mat[14]);
    // float tw = m30*source.x + m31*source.y + m32*source.z + m33;
    // if (tw != 0 && tw != 1) {
    // target.div(tw);
    // }
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
      throw new RuntimeException("The source and target vectors used in " + "Mat.mult() cannot be identical.");
    }
    if (target.length == 3) {
      target[0] = mat[0] * source[0] + mat[4] * source[1] + mat[8] * source[2] + mat[12];
      target[1] = mat[1] * source[0] + mat[5] * source[1] + mat[9] * source[2] + mat[13];
      target[2] = mat[2] * source[0] + mat[6] * source[1] + mat[10] * source[2] + mat[14];
      // float w = mat[12]*source[0] + mat[13]*source[1] + mat[14]*source[2] +
      // mat[15];
      // if (w != 0 && w != 1) {
      // target[0] /= w; target[1] /= w; target[2] /= w;
      // }
    } else if (target.length > 3) {
      target[0] = mat[0] * source[0] + mat[4] * source[1] + mat[8] * source[2] + mat[12] * source[3];
      target[1] = mat[1] * source[0] + mat[5] * source[1] + mat[9] * source[2] + mat[13] * source[3];
      target[2] = mat[2] * source[0] + mat[6] * source[1] + mat[10] * source[2] + mat[14] * source[3];
      target[3] = mat[3] * source[0] + mat[7] * source[1] + mat[11] * source[2] + mat[15] * source[3];
    }
    return target;
  }

  /**
   * Transpose this matrix.
   */
  public void transpose() {
    float temp;
    temp = mat[4];
    mat[4] = mat[1];
    mat[1] = temp;
    temp = mat[8];
    mat[8] = mat[2];
    mat[2] = temp;
    temp = mat[12];
    mat[12] = mat[3];
    mat[3] = temp;
    temp = mat[9];
    mat[9] = mat[6];
    mat[6] = temp;
    temp = mat[13];
    mat[13] = mat[7];
    mat[7] = temp;
    temp = mat[14];
    mat[14] = mat[11];
    mat[11] = temp;
  }

  /**
   * Invert this matrix into {@code m}, i.e., doesn't modify this matrix.
   * <p>
   * {@code m} should be non-null.
   */
  public boolean invert(Mat m) {
    float determinant = determinant();
    if (determinant == 0) {
      return false;
    }

    // first row
    float t00 = determinant3x3(mat[5], mat[9], mat[13], mat[6], mat[10], mat[14], mat[7], mat[11], mat[15]);
    float t01 = -determinant3x3(mat[1], mat[9], mat[13], mat[2], mat[10], mat[14], mat[3], mat[11], mat[15]);
    float t02 = determinant3x3(mat[1], mat[5], mat[13], mat[2], mat[6], mat[14], mat[3], mat[7], mat[15]);
    float t03 = -determinant3x3(mat[1], mat[5], mat[9], mat[2], mat[6], mat[10], mat[3], mat[7], mat[11]);

    // second row
    float t10 = -determinant3x3(mat[4], mat[8], mat[12], mat[6], mat[10], mat[14], mat[7], mat[11], mat[15]);
    float t11 = determinant3x3(mat[0], mat[8], mat[12], mat[2], mat[10], mat[14], mat[3], mat[11], mat[15]);
    float t12 = -determinant3x3(mat[0], mat[4], mat[12], mat[2], mat[6], mat[14], mat[3], mat[7], mat[15]);
    float t13 = determinant3x3(mat[0], mat[4], mat[8], mat[2], mat[6], mat[10], mat[3], mat[7], mat[11]);

    // third row
    float t20 = determinant3x3(mat[4], mat[8], mat[12], mat[5], mat[9], mat[13], mat[7], mat[11], mat[15]);
    float t21 = -determinant3x3(mat[0], mat[8], mat[12], mat[1], mat[9], mat[13], mat[3], mat[11], mat[15]);
    float t22 = determinant3x3(mat[0], mat[4], mat[12], mat[1], mat[5], mat[13], mat[3], mat[7], mat[15]);
    float t23 = -determinant3x3(mat[0], mat[4], mat[8], mat[1], mat[5], mat[9], mat[3], mat[7], mat[11]);

    // fourth row
    float t30 = -determinant3x3(mat[4], mat[8], mat[12], mat[5], mat[9], mat[13], mat[6], mat[10], mat[14]);
    float t31 = determinant3x3(mat[0], mat[8], mat[12], mat[1], mat[9], mat[13], mat[2], mat[10], mat[14]);
    float t32 = -determinant3x3(mat[0], mat[4], mat[12], mat[1], mat[5], mat[13], mat[2], mat[6], mat[14]);
    float t33 = determinant3x3(mat[0], mat[4], mat[8], mat[1], mat[5], mat[9], mat[2], mat[6], mat[10]);

    // transpose and divide by the determinant
    m.mat[0] = t00 / determinant;
    m.mat[4] = t10 / determinant;
    m.mat[8] = t20 / determinant;
    m.mat[12] = t30 / determinant;

    m.mat[1] = t01 / determinant;
    m.mat[5] = t11 / determinant;
    m.mat[9] = t21 / determinant;
    m.mat[13] = t31 / determinant;

    m.mat[2] = t02 / determinant;
    m.mat[6] = t12 / determinant;
    m.mat[10] = t22 / determinant;
    m.mat[14] = t32 / determinant;

    m.mat[3] = t03 / determinant;
    m.mat[7] = t13 / determinant;
    m.mat[11] = t23 / determinant;
    m.mat[15] = t33 / determinant;

    return true;
  }

  /**
   * Invert this matrix.
   *
   * @return true if successful
   */
  public boolean invert() {
    float determinant = determinant();
    if (Util.zero(determinant)) {
      return false;
    }

    // first row
    float t00 = determinant3x3(mat[5], mat[9], mat[13], mat[6], mat[10], mat[14], mat[7], mat[11], mat[15]);
    float t01 = -determinant3x3(mat[1], mat[9], mat[13], mat[2], mat[10], mat[14], mat[3], mat[11], mat[15]);
    float t02 = determinant3x3(mat[1], mat[5], mat[13], mat[2], mat[6], mat[14], mat[3], mat[7], mat[15]);
    float t03 = -determinant3x3(mat[1], mat[5], mat[9], mat[2], mat[6], mat[10], mat[3], mat[7], mat[11]);

    // second row
    float t10 = -determinant3x3(mat[4], mat[8], mat[12], mat[6], mat[10], mat[14], mat[7], mat[11], mat[15]);
    float t11 = determinant3x3(mat[0], mat[8], mat[12], mat[2], mat[10], mat[14], mat[3], mat[11], mat[15]);
    float t12 = -determinant3x3(mat[0], mat[4], mat[12], mat[2], mat[6], mat[14], mat[3], mat[7], mat[15]);
    float t13 = determinant3x3(mat[0], mat[4], mat[8], mat[2], mat[6], mat[10], mat[3], mat[7], mat[11]);

    // third row
    float t20 = determinant3x3(mat[4], mat[8], mat[12], mat[5], mat[9], mat[13], mat[7], mat[11], mat[15]);
    float t21 = -determinant3x3(mat[0], mat[8], mat[12], mat[1], mat[9], mat[13], mat[3], mat[11], mat[15]);
    float t22 = determinant3x3(mat[0], mat[4], mat[12], mat[1], mat[5], mat[13], mat[3], mat[7], mat[15]);
    float t23 = -determinant3x3(mat[0], mat[4], mat[8], mat[1], mat[5], mat[9], mat[3], mat[7], mat[11]);

    // fourth row
    float t30 = -determinant3x3(mat[4], mat[8], mat[12], mat[5], mat[9], mat[13], mat[6], mat[10], mat[14]);
    float t31 = determinant3x3(mat[0], mat[8], mat[12], mat[1], mat[9], mat[13], mat[2], mat[10], mat[14]);
    float t32 = -determinant3x3(mat[0], mat[4], mat[12], mat[1], mat[5], mat[13], mat[2], mat[6], mat[14]);
    float t33 = determinant3x3(mat[0], mat[4], mat[8], mat[1], mat[5], mat[9], mat[2], mat[6], mat[10]);

    // transpose and divide by the determinant
    mat[0] = t00 / determinant;
    mat[4] = t10 / determinant;
    mat[8] = t20 / determinant;
    mat[12] = t30 / determinant;

    mat[1] = t01 / determinant;
    mat[5] = t11 / determinant;
    mat[9] = t21 / determinant;
    mat[13] = t31 / determinant;

    mat[2] = t02 / determinant;
    mat[6] = t12 / determinant;
    mat[10] = t22 / determinant;
    mat[14] = t32 / determinant;

    mat[3] = t03 / determinant;
    mat[7] = t13 / determinant;
    mat[11] = t23 / determinant;
    mat[15] = t33 / determinant;

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
    float f = mat[0] * ((mat[5] * mat[10] * mat[15] + mat[9] * mat[14] * mat[7] + mat[13] * mat[6] * mat[11])
        - mat[13] * mat[10] * mat[7] - mat[5] * mat[14] * mat[11] - mat[9] * mat[6] * mat[15]);
    f -= mat[4] * ((mat[1] * mat[10] * mat[15] + mat[9] * mat[14] * mat[3] + mat[13] * mat[2] * mat[11])
        - mat[13] * mat[10] * mat[3] - mat[1] * mat[14] * mat[11] - mat[9] * mat[2] * mat[15]);
    f += mat[8] * ((mat[1] * mat[6] * mat[15] + mat[5] * mat[14] * mat[3] + mat[13] * mat[2] * mat[7])
        - mat[13] * mat[6] * mat[3] - mat[1] * mat[14] * mat[7] - mat[5] * mat[2] * mat[15]);
    f -= mat[12] * ((mat[1] * mat[6] * mat[11] + mat[5] * mat[10] * mat[3] + mat[9] * mat[2] * mat[7])
        - mat[9] * mat[6] * mat[3] - mat[1] * mat[10] * mat[7] - mat[5] * mat[2] * mat[11]);
    return f;
  }

  /**
   * Print this matrix contents onto the console.
   */
  public void print() {
    System.out.println(
        mat[0] + " " + mat[4] + " " + mat[8] + " " + mat[12] + "\n" + mat[1] + " " + mat[5] + " " + mat[9] + " " + mat[13]
            + "\n" + mat[2] + " " + mat[6] + " " + mat[10] + " " + mat[14] + "\n" + mat[3] + " " + mat[7] + " " + mat[11]
            + " " + mat[15] + "\n");
  }
}
