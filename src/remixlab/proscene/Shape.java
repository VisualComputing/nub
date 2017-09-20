/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import remixlab.dandelion.core.AbstractScene;
import remixlab.dandelion.geom.Vec;
import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;

import java.lang.reflect.Method;

/**
 * An interactive-frame shape may wrap either a PShape (Processing retained mode) or a
 * graphics procedure (Processing immediate mode), but not both.
 * <p>
 * This class allows to easily set an interactive-frame shape (see all the set() methods)
 * and is provided to ease the {@link remixlab.proscene.InteractiveFrame} class
 * implementation itself.
 */
class Shape {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(shp).append(obj).append(mth).append(shift).toHashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (object == null)
      return false;
    if (object == this)
      return true;
    if (object.getClass() != getClass())
      return false;
    Shape other = (Shape) object;
    return new EqualsBuilder().append(shp, other.shp).append(obj, other.obj).append(mth, other.mth)
        .append(shift, other.shift).isEquals();
  }

  InteractiveFrame iFrame;
  PShape shp;
  Object obj;
  Method mth;
  Vec shift;

  Shape(InteractiveFrame frame) {
    iFrame = frame;
  }

  /**
   * Defines the shape shift, i.e., the translation respect to the frame origin used to
   * draw the shape.
   */
  void shift(Vec s) {
    if (iFrame.isEyeFrame())
      AbstractScene.showOnlyEyeWarning("shift", true);
    shift = s;
  }

  /**
   * Draw the shape into an arbitrary PGraphics.
   */
  void draw(PGraphics pg) {
    if (iFrame.isEyeFrame())
      return;
    if (shift != null)
      if (pg.is3D())
        pg.translate(shift.x(), shift.y(), shift.z());
      else
        pg.translate(shift.x(), shift.y());
    // The shape part took verbatim from Processing, see:
    // https://github.com/processing/processing/blob/master/core/src/processing/core/PGraphics.java
    if (shp != null) {
      // don't do expensive matrix ops if invisible
      if (shp.isVisible() && !iFrame.isEyeFrame()) {
        pg.flush();
        if (pg.shapeMode == PApplet.CENTER) {
          pg.pushMatrix();
          pg.translate(-shp.getWidth() / 2, -shp.getHeight() / 2);
        }
        shp.draw(pg); // needs to handle recorder too
        if (pg.shapeMode == PApplet.CENTER) {
          pg.popMatrix();
        }
      }
    } else if (mth != null && obj != null) {
      try {
        mth.invoke(obj, new Object[]{pg});
      } catch (Exception e1) {
        try {
          mth.invoke(obj, new Object[]{iFrame, pg});
        } catch (Exception e2) {
          PApplet.println("Something went wrong when invoking your " + mth.getName() + " method");
        }
      }
    }
  }

  /**
   * Retained mode.
   */
  void set(PShape ps) {
    if (!isSetable(ps))
      return;
    shp = ps;
    mth = null;
    obj = null;
  }

  /**
   * Sets shape from other.
   * <p>
   * Note that all fields are copied by reference.
   */
  void set(Shape other) {
    if (equals(other))
      return;
    if (!isReset())
      System.out.println("Overwriting shape with other frame shape");
    shp = other.shp;
    obj = other.obj;
    mth = other.mth;
    shift = other.shift;
  }

  /**
   * Immediate mode.
   * <p>
   * Low-level routine. Looks for a {@code void methodName(PGraphics)} function prototype
   * in {@code object}.
   */
  void singleParam(Object object, String methodName) throws NoSuchMethodException, SecurityException {
    mth = object.getClass().getMethod(methodName, new Class<?>[]{PGraphics.class});
    obj = object;
  }

  /**
   * Immediate mode.
   * <p>
   * Low-level routine. Looks for a {@code void methodName(InteractiveFrame, PGraphics)}
   * function prototype in {@code object}.
   */
  void doubleParam(Object object, String methodName) throws NoSuchMethodException, SecurityException {
    mth = object.getClass().getMethod(methodName, new Class<?>[]{InteractiveFrame.class, PGraphics.class});
    obj = object;
  }

  /**
   * Immediate mode.
   * <p>
   * High-level routine where the {@code object} declaring the graphics procedure is
   * explicitly given.
   * <p>
   * Looks for a {@link #singleParam(Object, String)} function prototype first. If nothing
   * is hit, then looks for a {@link #doubleParam(Object, String)} function prototype,
   * only if the {@link remixlab.proscene.InteractiveFrame} instance this shape is
   * attached to is not a {@link remixlab.proscene.InteractiveFrame#isEyeFrame()}.
   */
  boolean set(Object object, String methodName) {
    if (!isSetable(object, methodName))
      return false;
    boolean success = false;
    try {
      singleParam(object, methodName);
      success = true;
    } catch (Exception e1) {
      try {
        if (iFrame.isEyeFrame()) {
          System.out.println(
              "Warning: not shape set! Check the existence of one of the following method prototypes: " + prototypes(object,
                  methodName));
          return false;
        }
        doubleParam(object, methodName);
        success = true;
      } catch (Exception e2) {
        System.out.println(
            "Warning: not shape set! Check the existence of one of the following method prototypes: " + prototypes(object,
                methodName));
      }
    }
    if (success)
      shp = null;
    return success;
  }

  /**
   * Immediate mode.
   * <p>
   * High-level routine where the object declaring the graphics procedure is not given and
   * hence need to be inferred. It could be either:
   * <ol>
   * <li>The {@link remixlab.proscene.Scene#pApplet()};</li>
   * <li>The {@link remixlab.proscene.InteractiveFrame} instance this shape is attached
   * to, or;</li>
   * <li>The {@link remixlab.proscene.InteractiveFrame#scene()} handling that frame
   * instance.
   * </ol>
   * The algorithm looks for a {@link #singleParam(Object, String)} function prototype
   * first. If nothing is hit, then looks for a {@link #doubleParam(Object, String)}
   * function prototype, within the objects in the above order.
   */
  boolean set(String methodName) {
    boolean success = false;
    if (!isSetable(iFrame.scene().pApplet(), methodName))
      return false;
    try {
      singleParam(iFrame.scene().pApplet(), methodName);
      success = true;
    } catch (Exception e1) {
      try {
        doubleParam(iFrame.scene().pApplet(), methodName);
        success = true;
      } catch (Exception e2) {
        if (!isSetable(iFrame, methodName))
          return false;
        try {
          singleParam(iFrame, methodName);
          success = true;
        } catch (Exception e4) {
          if (!isSetable(iFrame.scene(), methodName))
            return false;
          try {
            singleParam(iFrame.scene(), methodName);
            success = true;
          } catch (Exception e3) {
            try {
              doubleParam(iFrame.scene(), methodName);
              success = true;
            } catch (Exception e5) {
              System.out.println(
                  "Warning: not shape set! Check the existence of one of the following method prototypes: " + prototypes(
                      iFrame.scene().pApplet(), methodName) + ", " + prototypes(iFrame, methodName) + ", " + prototypes(
                      iFrame.scene(), methodName)
                      + ". Or, if your shape lies within a different object, use setShape(Object object, String methodName) instead.");
            }
          }
        }
      }
    }
    if (success)
      shp = null;
    return success;
  }

  /**
   * Internal use.
   *
   * @see #set(String)
   * @see #set(Object, String)
   */
  String prototypes(Object object, String action) {
    String sgn1 =
        "public void " + object.getClass().getSimpleName() + "." + action + "(" + PGraphics.class.getSimpleName() + ")";
    if (!(object instanceof InteractiveFrame) && !iFrame.isEyeFrame()) {
      String sgn2 =
          "public void " + object.getClass().getSimpleName() + "." + action + "(" + InteractiveFrame.class.getSimpleName()
              + ", " + PGraphics.class.getSimpleName() + ")";
      return sgn1 + ", " + sgn2;
    }
    return sgn1;
  }

  /**
   * Sets all internal shape references to null.
   */
  void reset() {
    if (isReset())
      return;
    shp = null;
    mth = null;
    obj = null;
    shift = null;
  }

  boolean isSetable(Object object, String methodName) {
    if (object == null || methodName == null)
      return false;
    if (isImmediate())
      if (obj == object && mth.getName().equals(methodName))
        return false;
    if (!isReset())
      System.out.println("Warning: overwriting shape in immediate mode");
    return true;
  }

  boolean isSetable(PShape shape) {
    if (shape == null || shape == shp)
      return false;
    if (!isReset())
      System.out.println("Warning: overwriting shape in retained mode");
    return true;
  }

  /**
   * Checks if internal references are null.
   */
  boolean isReset() {
    return shp == null && mth == null && shp == null && shift == null;
  }

  /**
   * Does the shape wraps a PShape Processing object?
   */
  boolean isRetained() {
    return shp != null;
  }

  /**
   * Does the shape wraps a graphics procedure Processing object?
   */
  boolean isImmediate() {
    return obj != null && mth != null;
  }
}
