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

import remixlab.util.Copyable;

/**
 * Interface for 2D {@link remixlab.dandelion.geom.Rot} and 3D
 * {@link remixlab.dandelion.geom.Quat} rotations.
 */
public interface Rotation extends Copyable {
  @Override
  public Rotation get();

  /**
   * @return rotation angle
   */
  public float angle();

  /**
   * Negates all object components
   */
  public void negate();

  /**
   * Compose the rotation
   *
   * @param o rotation to compose with
   */
  public void compose(Rotation o);

  /**
   * Returns the inverse rotation
   */
  public Rotation inverse();

  /**
   * Returns the image of {@code v} by the rotation.
   */
  public Vec rotate(Vec v);

  /**
   * Returns the image of {@code v} by the {@link #inverse()} rotation.
   */
  public Vec inverseRotate(Vec v);

  /**
   * Returns the Mat which represents the rotation matrix associated with the Quat.
   */
  public Mat matrix();

  /**
   * Returns the associated inverse rotation Mat. This is simply {@link #matrix()} of the
   * {@link #inverse()}.
   */
  public Mat inverseMatrix();

  /**
   * Sets the rotation from the given Mat representation.
   */
  public void fromMatrix(Mat glMatrix);

  /**
   * Sets the rotation from the three rotated vectors of an orthogonal basis.
   * <p>
   * The three vectors do not have to be normalized but must be orthogonal and direct
   * (i,e., {@code X^Y=k*Z, with k>0}).
   */
  public void fromRotatedBasis(Vec X, Vec Y, Vec Z);

  /**
   * Normalizes the rotation
   */
  public float normalize();

  /**
   * Define orientation from the two vectors.
   */
  public void fromTo(Vec from, Vec to);

  /**
   * Prints orientation data.
   */
  public void print();
}
