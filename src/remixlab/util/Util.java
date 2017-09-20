/**************************************************************************************
 * util_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.util;

/**
 * Various utility functions use throughout the framework.
 */
public class Util {
  public static float FLOAT_EPS = Float.MIN_VALUE;

  // Calculation of the Machine Epsilon for float precision. From:
  // http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
  static {
    float eps = 1.0f;

    do {
      eps /= 2.0f;
    } while ((float) (1.0 + (eps / 2.0)) != 1.0);

    FLOAT_EPS = eps;
  }

  /**
   * @return true if the two values are equal.
   */
  public static boolean same(float a, float b) {
    return Math.abs(a - b) < FLOAT_EPS;
  }

  /**
   * @return true if the two values are different.
   */
  public static boolean diff(float a, float b) {
    return FLOAT_EPS <= Math.abs(a - b);
  }

  /**
   * @return true if the value is zero.
   */
  public static boolean zero(float a) {
    return Math.abs(a) < FLOAT_EPS;
  }

  /**
   * @return true if the value is non-zero.
   */
  public static boolean nonZero(float a) {
    return FLOAT_EPS <= Math.abs(a);
  }

  /**
   * @return true if the value is positive.
   */
  public static boolean positive(float a) {
    return FLOAT_EPS <= a;
  }

  /**
   * @return true if the value is negative.
   */
  public static boolean negative(float a) {
    return a <= -FLOAT_EPS;
  }

  /**
   * @return Euclidean distance between points (x1,y1) and (x2,y2).
   */
  public static float distance(float x1, float y1, float x2, float y2) {
    return (float) Math.sqrt((float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0));
  }

  /**
   * @return Euclidean distance between points (x1,y1,z1) and (x2,y2,z2).
   */
  public static float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
    return (float) Math
        .sqrt((float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0) + (float) Math.pow((z2 - z1), 2.0));
  }

  /**
   * @return Euclidean distance between points (x1,y1,z1,rx1,y1,rz1) and
   * (x2,y2,z2,rx2,y2,rz2).
   */
  public static float distance(float x1, float y1, float z1, float rx1, float ry1, float rz1, float x2, float y2,
                               float z2, float rx2, float ry2, float rz2) {
    return (float) Math.sqrt(
        (float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0) + (float) Math.pow((z2 - z1), 2.0)
            + (float) Math.pow((rx2 - rx1), 2.0) + (float) Math.pow((ry2 - ry1), 2.0) + (float) Math
            .pow((rz2 - rz1), 2.0));
  }

  /**
   * Calculates a number between two numbers at a specific increment. The {@code amt}
   * parameter is the amount to interpolate between the two values where 0.0 equal to the
   * first point, 0.1 is very near the first point, 0.5 is half-way in between, etc.
   */
  public static final float lerp(float start, float stop, float amt) {
    return start + (stop - start) * amt;
  }
}
