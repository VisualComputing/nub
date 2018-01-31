/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package proscene.primitives.constraint;

import proscene.primitives.Frame;
import proscene.primitives.Quaternion;
import proscene.primitives.Vector;

/**
 * An AxisPlaneConstraint defined in the Frame local coordinate system.
 * <p>
 * The {@link #translationConstraintDirection()} and
 * {@link #rotationConstraintDirection()} are expressed in the Frame local coordinate
 * system (see {@link Frame#reference()} ).
 */
public class LocalConstraint extends AxisPlaneConstraint {
  /**
   * Depending on {@link #translationConstraintType()}, {@code constrain} translation to
   * be along an axis or limited to a plane defined in the local coordinate system by
   * {@link #translationConstraintDirection()}.
   */
  @Override
  public Vector constrainTranslation(Vector translation, Frame frame) {
    Vector res = new Vector(translation._vector[0], translation._vector[1], translation._vector[2]);
    Vector proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        proj = frame.rotation().rotate(translationConstraintDirection());
        // proj = frame.localInverseTransformOf(translationConstraintDirection());
        res = Vector.projectVectorOnPlane(translation, proj);
        break;
      case AXIS:
        proj = frame.rotation().rotate(translationConstraintDirection());
        // proj = frame.localInverseTransformOf(translationConstraintDirection());
        res = Vector.projectVectorOnAxis(translation, proj);
        break;
      case FORBIDDEN:
        res = new Vector(0.0f, 0.0f, 0.0f);
        break;
    }
    return res;
  }

  /**
   * When {@link #rotationConstraintType()} is of Type AXIS, constrain {@code rotation} to
   * be a rotation around an axis whose direction is defined in the Frame local coordinate
   * system by {@link #rotationConstraintDirection()}.
   */
  @Override
  public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
    Quaternion res = rotation.get();
    switch (rotationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        break;
      case AXIS:
        Vector axis = rotationConstraintDirection();
        Vector quat = new Vector(rotation._quaternion[0], rotation._quaternion[1], rotation._quaternion[2]);
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
