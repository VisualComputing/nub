/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.core.constraint;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

/**
 * An AxisPlaneConstraint defined in the Eye coordinate system.
 * <p>
 * The {@link #translationConstraintDirection()} and
 * {@link #rotationConstraintDirection()} are expressed in the associated {@link #eye()}
 * coordinate system.
 */
public class EyeConstraint extends AxisPlaneConstraint {
  private Graph scene;

  /**
   * Creates an EyeConstraint, whose constrained directions are defined in the
   * {@link #eye()} coordinate system.
   */
  public EyeConstraint(Graph scn) {
    super();
    scene = scn;
  }

  /**
   * Returns the associated Eye. Set using the EyeConstraint constructor.
   */
  public Node eye() {
    return scene.eye();
  }

  /**
   * Depending on {@link #translationConstraintType()}, {@code constrain} translation to
   * be along an axis or limited to a plane defined in the {@link #eye()} coordinate
   * system by {@link #translationConstraintDirection()}.
   */
  @Override
  public Vector constrainTranslation(Vector translation, Node node) {
    Vector res = translation.get();
    Vector proj;
    switch (translationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        proj = eye().worldDisplacement(translationConstraintDirection());
        if (node.reference() != null)
          proj = node.reference().displacement(proj);
        res = Vector.projectVectorOnPlane(translation, proj);
        break;
      case AXIS:
        proj = eye().worldDisplacement(translationConstraintDirection());
        if (node.reference() != null)
          proj = node.reference().displacement(proj);
        res = Vector.projectVectorOnAxis(translation, proj);
        break;
      case FORBIDDEN:
        res = new Vector(0.0f, 0.0f, 0.0f);
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
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    Quaternion res = rotation.get();
    switch (rotationConstraintType()) {
      case FREE:
        break;
      case PLANE:
        break;
      case AXIS:
        Vector axis = node.displacement(eye().worldDisplacement(rotationConstraintDirection()));
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
