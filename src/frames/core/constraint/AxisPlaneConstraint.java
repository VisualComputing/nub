/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core.constraint;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

/**
 * Base class for Frame constraints defined by an axis or a plane.
 * <p>
 * AxisPlaneConstraint is an interface for (translation and/or rotation) Constraint that
 * are defined by a direction. {@link #translationConstraintType()} and
 * {@link #rotationConstraintType()} define how this direction should be interpreted: as
 * an axis or as a plane normal.
 * <p>
 * The three implementations of this class: LocalConstraint, WorldConstraint and
 * EyeConstraint differ by the coordinate system in which this direction is expressed.
 */
public class AxisPlaneConstraint extends Constraint {
  /**
   * Type lists the different types of translation and rotation constraints that are
   * available.
   * <p>
   * It specifies the meaning of the constraint direction (see
   * {@link #translationConstraintDirection()} and {@link #rotationConstraintDirection()}
   * ): as an axis direction or a plane normal. {@link Type#FREE} means no constraint
   * while {@link Type#FORBIDDEN} completely forbids the translation and/or the rotation.
   * <b>Attention: </b> The {@link Type#PLANE} Type is not valid for rotational
   * constraint.
   * <p>
   * New derived classes can use their own extended {@code enum} for specific constraints.
   */
  public enum Type {
    FREE, AXIS, PLANE, FORBIDDEN
  }

  private Type transConstraintType;
  private Type rotConstraintType;
  private Vector transConstraintDir;
  private Vector rotConstraintDir;

  /**
   * Default constructor.
   * <p>
   * {@link #translationConstraintType()} and {@link #rotationConstraintType()} are set to
   * {@link Type#FREE}. {@link #translationConstraintDirection()} and
   * {@link #rotationConstraintDirection()} are set to (0,0,0).
   */
  public AxisPlaneConstraint() {
    // Do not use set since setRotationConstraintType needs a read.
    this.transConstraintType = AxisPlaneConstraint.Type.FREE;
    this.rotConstraintType = AxisPlaneConstraint.Type.FREE;
    transConstraintDir = new Vector(0.0f, 0.0f, 0.0f);
    rotConstraintDir = new Vector(0.0f, 0.0f, 0.0f);
  }

  /**
   * Returns the translation constraint Type.
   * <p>
   * Depending on this value, the Frame will freely translate ({@link Type#FREE} ), will
   * only be able to translate along an axis direction ( {@link Type#AXIS}), will be
   * forced to stay into a plane ({@link Type#PLANE} ) or will not able to translate at
   * all ({@link Type#FORBIDDEN}).
   * <p>
   * Use {@link Frame#setPosition(Vector)} to define the position of
   * the constrained Frame before it gets constrained.
   */
  public Type translationConstraintType() {
    return transConstraintType;
  }

  /**
   * Returns the direction used by the translation constraint.
   * <p>
   * It represents the axis direction ({@link Type#AXIS}) or the plane normal (
   * {@link Type#PLANE}) depending on the {@link #translationConstraintType()}. It is
   * undefined for ({@link Type#FREE}) or ({@link Type#FORBIDDEN}).
   * <p>
   * The AxisPlaneConstraint derived classes express this direction in different
   * coordinate system (frame for EyeConstraint, local for LocalConstraint, and world for
   * WorldConstraint). This value can be modified with
   * {@link #setRotationConstraintDirection(Vector)}.
   */
  public Vector translationConstraintDirection() {
    return transConstraintDir;
  }

  /**
   * Returns the rotation constraint Type.
   */
  public Type rotationConstraintType() {
    return rotConstraintType;
  }

  /**
   * Returns the axis direction used by the rotation constraint.
   * <p>
   * This direction is defined only when {@link #rotationConstraintType()} is
   * {@link Type#AXIS}.
   * <p>
   * The AxisPlaneConstraint derived classes express this direction in different
   * coordinate system (frame for EyeConstraint, local for LocalConstraint, and world for
   * WorldConstraint). This value can be modified with
   * {@link #setRotationConstraintDirection(Vector)}.
   */
  public Vector rotationConstraintDirection() {
    return rotConstraintDir;
  }

  /**
   * Simply calls {@link #setTranslationConstraintType(Type)} and
   * {@link #setTranslationConstraintDirection(Vector)}.
   */
  public void setTranslationConstraint(Type type, Vector direction) {
    setTranslationConstraintType(type);
    setTranslationConstraintDirection(direction);
  }

  /**
   * Defines the {@link #translationConstraintDirection()}. The coordinate system where
   * {@code direction} is expressed depends on your class implementation.
   */
  public void setTranslationConstraintDirection(Vector direction) {
    if ((translationConstraintType() != AxisPlaneConstraint.Type.FREE) && (translationConstraintType()
        != AxisPlaneConstraint.Type.FORBIDDEN)) {
      float norm = direction.magnitude();
      if (norm == 0) {
        System.out
            .println("Warning: AxisPlaneConstraint.setTranslationConstraintDir: null vector for translation constraint");
        transConstraintType = AxisPlaneConstraint.Type.FREE;
      } else
        transConstraintDir = Vector.multiply(direction, (1.0f / norm));
    }
  }

  /**
   * Simply calls {@link #setRotationConstraintType(Type)} and
   * {@link #setRotationConstraintDirection(Vector)}.
   */
  public void setRotationConstraint(Type type, Vector direction) {
    setRotationConstraintType(type);
    setRotationConstraintDirection(direction);
  }

  /**
   * Defines the {@link #rotationConstraintDirection()}. The coordinate system where
   * {@code direction} is expressed depends on your class implementation.
   */
  public void setRotationConstraintDirection(Vector direction) {
    if ((rotationConstraintType() != AxisPlaneConstraint.Type.FREE) && (rotationConstraintType()
        != AxisPlaneConstraint.Type.FORBIDDEN)) {
      float norm = direction.magnitude();
      if (norm == 0) {
        System.out.println("Warning: AxisPlaneConstraint.setRotationConstraintDir: null vector for rotation constraint");
        rotConstraintType = AxisPlaneConstraint.Type.FREE;
      } else
        rotConstraintDir = Vector.multiply(direction, (1.0f / norm));
    }
  }

  /**
   * Sets the Type() of the {@link #translationConstraintType()}. Default is
   * {@link Type#FREE}
   */
  public void setTranslationConstraintType(Type type) {
    transConstraintType = type;
  }

  /**
   * Set the Type of the {@link #rotationConstraintType()}. Default is {@link Type#FREE}.
   * <p>
   * Depending on this value, the Frame will freely rotate ({@link Type#FREE}), will only
   * be able to rotate around an axis ({@link Type#AXIS}), or will not able to rotate at
   * all {@link Type#FORBIDDEN}.
   * <p>
   * Use {@link Frame#setOrientation(Quaternion)} to define the
   * orientation of the constrained Frame before it gets constrained.
   *
   * <b>Attention:</b> An {@link Type#PLANE} Type is not meaningful for rotational
   * constraints and will be ignored.
   */
  public void setRotationConstraintType(Type type) {
    if (rotationConstraintType() == AxisPlaneConstraint.Type.PLANE) {
      System.out.println(
          "Warning: AxisPlaneConstraint.setRotationConstraintType: the PLANE type cannot be used for a rotation constraints");
      return;
    }
    rotConstraintType = type;
  }
}
