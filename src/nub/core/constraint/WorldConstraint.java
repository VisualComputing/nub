/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core.constraint;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

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
  public Vector constrainTranslation(Vector translation, Node node) {
    Vector res = new Vector(translation._vector[0], translation._vector[1], translation._vector[2]);
    Vector proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        if (node.reference() != null) {
          proj = node.reference().displacement(translationConstraintDirection());
          res = Vector.projectVectorOnPlane(translation, proj);
        } else
          res = Vector.projectVectorOnPlane(translation, translationConstraintDirection());
        break;
      case AXIS:
        if (node.reference() != null) {
          proj = node.reference().displacement(translationConstraintDirection());
          res = Vector.projectVectorOnAxis(translation, proj);
        } else
          res = Vector.projectVectorOnAxis(translation, translationConstraintDirection());
        break;
      case FORBIDDEN:
        res = new Vector(0.0f, 0.0f, 0.0f);
        break;
    }
    return res;
  }

  /**
   * When {@link #rotationConstraintType()} is of type AXIS, constrain {@code rotation} to
   * be a rotation around an axis whose direction is defined in the Node world coordinate
   * system by {@link #rotationConstraintDirection()}.
   */
  @Override
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    Quaternion res = rotation.get();
    switch (rotationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        break;
      case AXIS:
        Vector quat = new Vector(rotation._quaternion[0], rotation._quaternion[1], rotation._quaternion[2]);
        Vector axis = node.displacement(rotationConstraintDirection());
        quat = Vector.projectVectorOnAxis(quat, axis);
        res = new Quaternion(quat, 2.0f * (float) Math.acos(rotation._quaternion[3]));
        break;
      case FORBIDDEN:
        res = new Quaternion(); // identity
        break;
    }
    return res;
  }
}
