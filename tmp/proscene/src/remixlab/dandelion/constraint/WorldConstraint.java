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

import remixlab.dandelion.geom.*;
import remixlab.util.Util;

/**
 * An AxisPlaneConstraint defined in the world coordinate system.
 * <p>
 * The {@link #translationConstraintDirection()} and
 * {@link #rotationConstraintDirection()} are expressed in the world coordinate system.
 */
public class WorldConstraint extends AxisPlaneConstraint {
  /**
   * Depending on {@link #translationConstraintType()}, {@code constrain} translation to
   * be along an axis or limited to a plane defined in the world coordinate system by
   * {@link #translationConstraintDirection()}.
   */
  @Override
  public Vec constrainTranslation(Vec translation, Frame frame) {
    Vec res = new Vec(translation.vec[0], translation.vec[1], translation.vec[2]);
    Vec proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        if (frame.is2D() && Util.nonZero(translationConstraintDirection().z()))
          break;
        if (frame.referenceFrame() != null) {
          proj = frame.referenceFrame().transformOf(translationConstraintDirection());
          res = Vec.projectVectorOnPlane(translation, proj);
        } else
          res = Vec.projectVectorOnPlane(translation, translationConstraintDirection());
        break;
      case AXIS:
        if (frame.is2D() && Util.nonZero(translationConstraintDirection().z()))
          break;
        if (frame.referenceFrame() != null) {
          proj = frame.referenceFrame().transformOf(translationConstraintDirection());
          res = Vec.projectVectorOnAxis(translation, proj);
        } else
          res = Vec.projectVectorOnAxis(translation, translationConstraintDirection());
        break;
      case FORBIDDEN:
        res = new Vec(0.0f, 0.0f, 0.0f);
        break;
    }
    return res;
  }

  /**
   * When {@link #rotationConstraintType()} is of type AXIS, constrain {@code rotation} to
   * be a rotation around an axis whose direction is defined in the Frame world coordinate
   * system by {@link #rotationConstraintDirection()}.
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
          Vec quat = new Vec(((Quat) rotation).quat[0], ((Quat) rotation).quat[1], ((Quat) rotation).quat[2]);
          Vec axis = frame.transformOf(rotationConstraintDirection());
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
