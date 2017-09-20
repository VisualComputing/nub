/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.constraint;

import remixlab.dandelion.geom.Rotation;
import remixlab.dandelion.geom.Vec;
import remixlab.util.Util;

/**
 * An abstract class for Frame constraints defined by an axis or a plane.
 * <p>
 * AxisPlaneConstraint is an interface for (translation and/or rotation) Constraint that
 * are defined by a direction. {@link #translationConstraintType()} and
 * {@link #rotationConstraintType()} define how this direction should be interpreted: as
 * an axis or as a plane normal.
 * <p>
 * The three implementations of this class: LocalConstraint, WorldConstraint and
 * EyeConstraint differ by the coordinate system in which this direction is expressed.
 */
public abstract class AxisPlaneConstraint extends Constraint {
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

  ;

  private Type transConstraintType;
  private Type rotConstraintType;
  private Vec transConstraintDir;
  private Vec rotConstraintDir;

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
    transConstraintDir = new Vec(0.0f, 0.0f, 0.0f);
    rotConstraintDir = new Vec(0.0f, 0.0f, 0.0f);
  }

  /**
   * Returns the translation constraint Type.
   * <p>
   * Depending on this value, the Frame will freely translate ({@link Type#FREE} ), will
   * only be able to translate along an axis direction ( {@link Type#AXIS}), will be
   * forced to stay into a plane ({@link Type#PLANE} ) or will not able to translate at
   * all ({@link Type#FORBIDDEN}).
   * <p>
   * Use {@link remixlab.dandelion.geom.Frame#setPosition(Vec)} to define the position of
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
   * coordinate system (eye for EyeConstraint, local for LocalConstraint, and world for
   * WorldConstraint). This value can be modified with
   * {@link #setRotationConstraintDirection(Vec)}.
   */
  public Vec translationConstraintDirection() {
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
   * coordinate system (eye for EyeConstraint, local for LocalConstraint, and world for
   * WorldConstraint). This value can be modified with
   * {@link #setRotationConstraintDirection(Vec)}.
   */
  public Vec rotationConstraintDirection() {
    return rotConstraintDir;
  }

  /**
   * Simply calls {@link #setTranslationConstraintType(Type)} and
   * {@link #setTranslationConstraintDirection(Vec)}.
   */
  public void setTranslationConstraint(Type type, Vec direction) {
    setTranslationConstraintType(type);
    setTranslationConstraintDirection(direction);
  }

  /**
   * Defines the {@link #translationConstraintDirection()}. The coordinate system where
   * {@code direction} is expressed depends on your class implementation.
   */
  public void setTranslationConstraintDirection(Vec direction) {
    if ((translationConstraintType() != AxisPlaneConstraint.Type.FREE) && (translationConstraintType()
        != AxisPlaneConstraint.Type.FORBIDDEN)) {
      float norm = direction.magnitude();
      if (Util.zero(norm)) {
        System.out
            .println("Warning: AxisPlaneConstraint.setTranslationConstraintDir: null vector for translation constraint");
        transConstraintType = AxisPlaneConstraint.Type.FREE;
      } else
        transConstraintDir = Vec.multiply(direction, (1.0f / norm));
    }
  }

  /**
   * Simply calls {@link #setRotationConstraintType(Type)} and
   * {@link #setRotationConstraintDirection(Vec)}.
   */
  public void setRotationConstraint(Type type, Vec direction) {
    setRotationConstraintType(type);
    setRotationConstraintDirection(direction);
  }

  /**
   * Defines the {@link #rotationConstraintDirection()}. The coordinate system where
   * {@code direction} is expressed depends on your class implementation.
   */
  public void setRotationConstraintDirection(Vec direction) {
    if ((rotationConstraintType() != AxisPlaneConstraint.Type.FREE) && (rotationConstraintType()
        != AxisPlaneConstraint.Type.FORBIDDEN)) {
      float norm = direction.magnitude();
      if (Util.zero(norm)) {
        System.out.println("Warning: AxisPlaneConstraint.setRotationConstraintDir: null vector for rotation constraint");
        rotConstraintType = AxisPlaneConstraint.Type.FREE;
      } else
        rotConstraintDir = Vec.multiply(direction, (1.0f / norm));
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
   * Use {@link remixlab.dandelion.geom.Frame#setOrientation(Rotation)} to define the
   * orientation of the constrained Frame before it gets constrained.
   * <p>
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
