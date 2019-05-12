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

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

/**
 * An interface class for Node constraints. This interface API aims to conform that of the
 * great <a href=
 * "http://libqglviewer.com/refManual/classqglviewer_1_1Constraint.html">libQGLViewer
 * Constraint</a>.
 * <p>
 * This class defines the interface for the constraint that can be applied to a Node to
 * limit its motion. Use {@link Node#setConstraint(Constraint)}
 * to associate a Constraint to a Node (default is a {@code null}
 * {@link Node#constraint()}.
 */
public abstract class Constraint {
  /**
   * Filters the translation applied to the Node. This default implementation is empty
   * (no filtering).
   * <p>
   * Overload this method in your own Constraint class to define a new translation
   * constraint. {@code node} is the Node to which is applied the translation. You
   * should refrain from directly changing its value in the constraint. Use its
   * {@link Node#position()} and update the translation
   * accordingly instead.
   * <p>
   * {@code translation} is expressed in the local Node coordinate system. Use
   * {@link Node#worldDisplacement(Vector)} to express it in the
   * world coordinate system if needed.
   */
  public Vector constrainTranslation(Vector translation, Node node) {
    return translation.get();
  }

  /**
   * Filters the rotation applied to the {@code node}. This default implementation is
   * empty (no filtering).
   * <p>
   * Overload this method in your own Constraint class to define a new rotation
   * constraint. See {@link #constrainTranslation(Vector, Node)} for details.
   * <p>
   * Use {@link Node#worldDisplacement(Vector)} on the
   * {@code rotation} {@link Quaternion#axis()} to express
   * {@code rotation} in the world coordinate system if needed.
   */
  public Quaternion constrainRotation(Quaternion rotation, Node node) {
    return rotation.get();
  }
}
