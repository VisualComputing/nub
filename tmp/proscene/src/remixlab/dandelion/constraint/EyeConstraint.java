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

import remixlab.dandelion.core.Eye;
import remixlab.dandelion.geom.*;
import remixlab.util.Util;

/**
 * An AxisPlaneConstraint defined in the Eye coordinate system.
 * <p>
 * The {@link #translationConstraintDirection()} and
 * {@link #rotationConstraintDirection()} are expressed in the associated {@link #eye()}
 * coordinate system.
 */
public class EyeConstraint extends AxisPlaneConstraint {

  private Eye eye;

  /**
   * Creates an EyeConstraint, whose constrained directions are defined in the
   * {@link #eye()} coordinate system.
   */
  public EyeConstraint(Eye theEye) {
    super();
    eye = theEye;
  }

  /**
   * Returns the associated Eye. Set using the EyeConstraint constructor.
   */
  public Eye eye() {
    return eye;
  }

  /**
   * Depending on {@link #translationConstraintType()}, {@code constrain} translation to
   * be along an axis or limited to a plane defined in the {@link #eye()} coordinate
   * system by {@link #translationConstraintDirection()}.
   */
  @Override
  public Vec constrainTranslation(Vec translation, Frame frame) {
    Vec res = translation.get();
    Vec proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        if (frame.is2D() && Util.nonZero(translationConstraintDirection().z()))
          break;
        proj = eye().frame().inverseTransformOf(translationConstraintDirection());
        if (frame.referenceFrame() != null)
          proj = frame.referenceFrame().transformOf(proj);
        res = Vec.projectVectorOnPlane(translation, proj);
        break;
      case AXIS:
        if (frame.is2D() && Util.nonZero(translationConstraintDirection().z()))
          break;
        proj = eye().frame().inverseTransformOf(translationConstraintDirection());
        if (frame.referenceFrame() != null)
          proj = frame.referenceFrame().transformOf(proj);
        res = Vec.projectVectorOnAxis(translation, proj);
        break;
      case FORBIDDEN:
        res = new Vec(0.0f, 0.0f, 0.0f);
        break;
    }
    return res;
  }

  /**
   * When {@link #rotationConstraintType()} is of type AXIS, constrain {@code rotation} to
   * be a rotation around an axis whose direction is defined in the {@link #eye()}
   * coordinate system by {@link #rotationConstraintDirection()}.
   */
  @Override
  public Rotation constrainRotation(Rotation rotation, Frame frame) {
    Rotation res = rotation.get();
    switch (rotationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        break;
      case AXIS:
        if (frame.is2D())
          break;
        if (rotation instanceof Quat) {
          Vec axis = frame.transformOf(eye().frame().inverseTransformOf(rotationConstraintDirection()));
          Vec quat = new Vec(((Quat) rotation).quat[0], ((Quat) rotation).quat[1], ((Quat) rotation).quat[2]);
          quat = Vec.projectVectorOnAxis(quat, axis);
          res = new Quat(quat, 2.0f * (float) Math.acos(((Quat) rotation).quat[3]));
        }
        break;
      case FORBIDDEN:
        if (rotation instanceof Quat)
          res = new Quat(); // identity
        else
          res = new Rot(); // identity
        break;
    }
    return res;
  }
}
