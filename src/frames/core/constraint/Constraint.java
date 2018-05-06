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
 * An interface class for Frame constraints. This interface API aims to conform that of the
 * great <a href=
 * "http://libqglviewer.com/refManual/classqglviewer_1_1Constraint.html">libQGLViewer
 * Constraint</a>.
 * <p>
 * This class defines the interface for the constraint that can be applied to a Frame to
 * limit its motion. Use {@link Frame#setConstraint(Constraint)}
 * to associate a Constraint to a Frame (default is a {@code null}
 * {@link Frame#constraint()}.
 */
public abstract class Constraint {
  /**
   * Filters the translation applied to the Frame. This default implementation is empty
   * (no filtering).
   * <p>
   * Overload this method in your own Constraint class to define a new translation
   * constraint. {@code frame} is the Frame to which is applied the translation. You
   * should refrain from directly changing its value in the constraint. Use its
   * {@link Frame#position()} and update the translation
   * accordingly instead.
   * <p>
   * {@code translation} is expressed in the local Frame coordinate system. Use
   * {@link Frame#_referenceDisplacement(Vector)} to express it in the
   * world coordinate system if needed.
   */
  public Vector constrainTranslation(Vector translation, Frame frame) {
    return translation.get();
  }

  /**
   * Filters the rotation applied to the {@code frame}. This default implementation is
   * empty (no filtering).
   * <p>
   * Overload this method in your own Constraint class to define a new rotation
   * constraint. See {@link #constrainTranslation(Vector, Frame)} for details.
   * <p>
   * Use {@link Frame#_referenceDisplacement(Vector)} on the
   * {@code rotation} {@link Quaternion#axis()} to express
   * {@code rotation} in the world coordinate system if needed.
   */
  public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
    return rotation.get();
  }
}
