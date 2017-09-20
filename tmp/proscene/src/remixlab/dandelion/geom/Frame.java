/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights refserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.geom;

import remixlab.dandelion.constraint.Constraint;
import remixlab.util.Copyable;
import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;
import remixlab.util.Util;

/**
 * A Frame is a 2D or 3D coordinate system, represented by a {@link #position()} , an
 * {@link #orientation()} and {@link #magnitude()}. The order of these transformations is
 * important: the Frame is first translated, then rotated around the new translated origin
 * and then scaled. This class API aims to conform that of the great
 * <a href="http://libqglviewer.com/refManual/classqglviewer_1_1Frame.html">libQGLViewer
 * Frame</a>, but it adds {@link #magnitude()} to it.
 * <p>
 * A Frame is useful to define the position, orientation and magnitude of an object, using
 * its {@link #matrix()} method, as shown below:
 * <p>
 * {@code // Builds a Frame at position (0.5,0,0) and oriented such that its Y axis is along the (1,1,1) }
 * <br>
 * {@code // direction. One could also have used setPosition() and setOrientation().} <br>
 * {@code Frame fr(new Vec(0.5,0,0), new Quat(new Vec(0,1,0), new Vec(1,1,1)));} <br>
 * {@code scene.pushModelView();} <br>
 * {@code scene.applyModelView(fr.matrix());} <br>
 * {@code // Draw your object here, in the local fr coordinate system.} <br>
 * {@code scene.popModelView();} <br>
 * <p>
 * Many functions are provided to transform a point from one coordinate system (Frame) to
 * an other: see {@link #coordinatesOf(Vec)}, {@link #inverseCoordinatesOf(Vec)},
 * {@link #coordinatesOfIn(Vec, Frame)}, {@link #coordinatesOfFrom(Vec, Frame)}...
 * <p>
 * You may also want to transform a vector (such as a normal), which corresponds to
 * applying only the rotational part of the frame transformation: see
 * {@link #transformOf(Vec)} and {@link #inverseTransformOf(Vec)}.
 * <p>
 * The {@link #translation()}, {@link #rotation()} and uniform positive {@link #scaling()}
 * that are encapsulated in a Frame can also be used to represent an angle preserving
 * transformation of space. Such a transformation can also be interpreted as a change of
 * coordinate system, and the coordinate system conversion functions actually allow you to
 * use a Frame as an angle preserving transformation. Use
 * {@link #inverseCoordinatesOf(Vec)} (resp. {@link #coordinatesOf(Vec)}) to apply the
 * transformation (resp. its inverse). Note the inversion.
 * <p>
 * <p>
 * <h3>Hierarchy of Frames</h3>
 * <p>
 * The position, orientation and magnitude of a Frame are actually defined with respect to
 * a {@link #referenceFrame()}. The default {@link #referenceFrame()} is the world
 * coordinate system (represented by a {@code null} {@link #referenceFrame()}). If you
 * {@link #setReferenceFrame(Frame)} to a different Frame, you must then differentiate:
 * <p>
 * <ul>
 * <li>The <b>local</b> {@link #translation()}, {@link #rotation()} and {@link #scaling()}
 * , defined with respect to the {@link #referenceFrame()}.</li>
 * <li>the <b>global</b> {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}, always defined with respect to the world coordinate system.</li>
 * </ul>
 * <p>
 * A Frame is actually defined by its {@link #translation()} with respect to its
 * {@link #referenceFrame()}, then by {@link #rotation()} of the coordinate system around
 * the new translated origin and then by a uniform positive {@link #scaling()} along its
 * rotated axes.
 * <p>
 * This terminology for <b>local</b> ({@link #translation()}, {@link #rotation()} and
 * {@link #scaling()}) and <b>global</b> ( {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}) definitions is used in all the methods' names and should be
 * sufficient to prevent ambiguities. These notions are obviously identical when the
 * {@link #referenceFrame()} is {@code null}, i.e., when the Frame is defined in the world
 * coordinate system (the one you are left with after calling
 * {@link remixlab.dandelion.core.AbstractScene#preDraw()}).
 * <p>
 * Frames can hence easily be organized in a tree hierarchy, which root is the world
 * coordinate system. A loop in the hierarchy would result in an inconsistent (multiple)
 * Frame definition. Therefore {@link #settingAsReferenceFrameWillCreateALoop(Frame)}
 * checks this and prevents {@link #referenceFrame()} from creating such a loop.
 * <p>
 * This frame hierarchy is used in methods like {@link #coordinatesOfIn(Vec, Frame)},
 * {@link #coordinatesOfFrom(Vec, Frame)} ... which allow coordinates (or vector)
 * conversions from a Frame to any other one (including the world coordinate system).
 * <p>
 * <h3>Constraints</h3>
 * <p>
 * An interesting feature of Frames is that their displacements can be constrained. When a
 * {@link remixlab.dandelion.constraint.Constraint} is attached to a Frame, it filters the
 * input of {@link #translate(Vec)} and {@link #rotate(Rotation)}, and only the resulting
 * filtered motion is applied to the Frame. The default {@link #constraint()} {@code null}
 * resulting in no filtering. Use {@link #setConstraint(Constraint)} to attach a
 * Constraint to a frame.
 * <p>
 * Classical constraints are provided for convenience (see
 * {@link remixlab.dandelion.constraint.LocalConstraint},
 * {@link remixlab.dandelion.constraint.WorldConstraint} and
 * {@link remixlab.dandelion.constraint.EyeConstraint}) and new constraints can very
 * easily be implemented.
 * <p>
 * <h3>Derived classes</h3>
 * <p>
 * The {@link remixlab.dandelion.core.GenericFrame} class inherits Frame and implements
 * all sorts of motion actions, so that a Frame (and hence an object) can be manipulated
 * in the scene by whatever user interaction means you can imagine.
 */
public class Frame implements Copyable {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(translation()).append(rotation()).append(scaling()).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    Frame other = (Frame) obj;
    return new EqualsBuilder().append(translation(), other.translation()).append(rotation(), other.rotation())
        .append(scaling(), other.scaling()).isEquals();
  }

  protected Vec trans;
  protected float scl;
  protected Rotation rot;
  protected Frame refFrame;
  protected Constraint cnstrnt;

  public Frame() {
    this(true);
  }

  /**
   * Same as {@code this(null, new Vec(), three_d ? new Quat() : new Rot(), 1)}.
   *
   * @see #Frame(Vec, Rotation, float)
   */
  public Frame(boolean three_d) {
    this(null, new Vec(), three_d ? new Quat() : new Rot(), 1);
  }

  /**
   * Same as {@code this(null, new Vec(), r, s)}.
   *
   * @see #Frame(Vec, Rotation, float)
   */
  public Frame(Rotation r, float s) {
    this(null, new Vec(), r, s);
  }

  /**
   * Same as {@code this(p, r, 1)}.
   *
   * @see #Frame(Vec, Rotation, float)
   */
  public Frame(Vec p, Rotation r) {
    this(p, r, 1);
  }

  /**
   * Same as {@code this(null, p, r, s)}.
   *
   * @see #Frame(Frame, Vec, Rotation, float)
   */
  public Frame(Vec p, Rotation r, float s) {
    this(null, p, r, s);
  }

  /**
   * Same as {@code this(referenceFrame, p, r, 1)}.
   *
   * @see #Frame(Frame, Vec, Rotation, float)
   */
  public Frame(Frame referenceFrame, Vec p, Rotation r) {
    this(referenceFrame, p, r, 1);
  }

  /**
   * Same as {@code this(referenceFrame, new Vec(), r, 1)}.
   *
   * @see #Frame(Frame, Vec, Rotation, float)
   */
  public Frame(Frame referenceFrame, Rotation r, float s) {
    this(referenceFrame, new Vec(), r, 1);
  }

  /**
   * Creates a Frame with {@code referenceFrame} as {@link #referenceFrame()}, and
   * {@code p}, {@code r} and {@code s} as the frame {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   */
  public Frame(Frame referenceFrame, Vec p, Rotation r, float s) {
    setTranslation(p);
    setRotation(r);
    setScaling(s);
    setReferenceFrame(referenceFrame);
  }

  protected Frame(Frame other) {
    trans = other.translation().get();
    rot = other.rotation().get();
    scl = other.scaling();
    refFrame = other.referenceFrame();
    cnstrnt = other.constraint();
  }

  @Override
  public Frame get() {
    return new Frame(this);
  }

  // MODIFIED

  /**
   * Internal use. Automatically call by all methods which change the Frame state.
   */
  protected void modified() {
  }

  // DIM

  /**
   * @return true if frame is 2D.
   */
  public boolean is2D() {
    return !is3D();
  }

  /**
   * @return true if frame is 3D.
   */
  public boolean is3D() {
    return rotation() instanceof Quat;
  }

  // REFERENCE_FRAME

  /**
   * Returns the reference Frame, in which coordinates system the Frame is defined.
   * <p>
   * The Frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are
   * defined with respect to the {@link #referenceFrame()} coordinate system. A
   * {@code null} reference Frame (default value) means that the Frame is defined in the
   * world coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to
   * recursively convert values along the reference Frame chain and to get values
   * expressed in the world coordinate system. The values match when the reference Frame
   * is {@code null}.
   * <p>
   * Use {@link #setReferenceFrame(Frame)} to set this value and create a Frame hierarchy.
   * Convenient functions allow you to convert coordinates from one Frame to another: see
   * {@link #coordinatesOf(Vec)}, {@link #localCoordinatesOf(Vec)} ,
   * {@link #coordinatesOfIn(Vec, Frame)} and their inverse functions.
   * <p>
   * Vectors can also be converted using {@link #transformOf(Vec)},
   * {@link #transformOfIn(Vec, Frame)}, {@link #localTransformOf(Vec)} and their inverse
   * functions.
   */
  public Frame referenceFrame() {
    return refFrame;
  }

  /**
   * Sets the {@link #referenceFrame()} of the Frame.
   * <p>
   * The Frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are then
   * defined in the {@link #referenceFrame()} coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to express
   * these in the world coordinate system.
   * <p>
   * Using this method, you can create a hierarchy of Frames. This hierarchy needs to be a
   * tree, which root is the world coordinate system (i.e., {@code null}
   * {@link #referenceFrame()}). No action is performed if setting {@code refFrame} as the
   * {@link #referenceFrame()} would create a loop in the Frame hierarchy.
   */
  public void setReferenceFrame(Frame rFrame) {
    if (settingAsReferenceFrameWillCreateALoop(rFrame)) {
      System.out.println("Frame.setReferenceFrame would create a loop in Frame hierarchy. Nothing done.");
      return;
    }
    if (referenceFrame() == rFrame)
      return;
    refFrame = rFrame;
    modified();
  }

  /**
   * Returns {@code true} if setting {@code frame} as the Frame's
   * {@link #referenceFrame()} would create a loop in the Frame hierarchy.
   */
  public final boolean settingAsReferenceFrameWillCreateALoop(Frame frame) {
    Frame f = frame;
    while (f != null) {
      if (f == Frame.this)
        return true;
      f = f.referenceFrame();
    }
    return false;
  }

  // CONSTRAINT

  /**
   * Returns the current {@link remixlab.dandelion.constraint.Constraint} applied to the
   * Frame.
   * <p>
   * A {@code null} value (default) means that no Constraint is used to filter the Frame
   * translation and rotation.
   * <p>
   * See the Constraint class documentation for details.
   */
  public Constraint constraint() {
    return cnstrnt;
  }

  /**
   * Sets the {@link #constraint()} attached to the Frame.
   * <p>
   * A {@code null} value means no constraint.
   */
  public void setConstraint(Constraint c) {
    cnstrnt = c;
  }

  // TRANSLATION

  /**
   * Returns the Frame translation, defined with respect to the {@link #referenceFrame()}.
   * <p>
   * Use {@link #position()} to get the result in world coordinates. These two values are
   * identical when the {@link #referenceFrame()} is {@code null} (default).
   *
   * @see #setTranslation(Vec)
   * @see #setTranslationWithConstraint(Vec)
   */
  public final Vec translation() {
    return trans;
  }

  /**
   * Sets the {@link #translation()} of the frame, locally defined with respect to the
   * {@link #referenceFrame()}.
   * <p>
   * Use {@link #setPosition(Vec)} to define the world coordinates {@link #position()}.
   * Use {@link #setTranslationWithConstraint(Vec)} to take into account the potential
   * {@link #constraint()} of the Frame.
   */
  public final void setTranslation(Vec t) {
    trans = t;
    modified();
  }

  /**
   * Same as {@link #setTranslation(Vec)}, but if there's a {@link #constraint()} it is
   * satisfied.
   *
   * @see #setRotationWithConstraint(Rotation)
   * @see #setPositionWithConstraint(Vec)
   * @see #setScaling(float)
   */
  public final void setTranslationWithConstraint(Vec translation) {
    Vec deltaT = Vec.subtract(translation, this.translation());
    if (constraint() != null)
      deltaT = constraint().constrainTranslation(deltaT, this);

    translate(deltaT);
  }

  /**
   * Same as {@link #setTranslation(Vec)}, but with {@code float} parameters.
   */
  public final void setTranslation(float x, float y) {
    setTranslation(new Vec(x, y));
  }

  /**
   * Same as {@link #setTranslation(Vec)}, but with {@code float} parameters.
   */
  public final void setTranslation(float x, float y, float z) {
    setTranslation(new Vec(x, y, z));
  }

  /**
   * Same as {@link #translate(Vec)} but with {@code float} parameters.
   */
  public final void translate(float x, float y, float z) {
    translate(new Vec(x, y, z));
  }

  /**
   * Same as {@link #translate(Vec)} but with {@code float} parameters.
   */
  public final void translate(float x, float y) {
    translate(new Vec(x, y));
  }

  /**
   * Translates the Frame according to {@code t}, locally defined with respect to the
   * {@link #referenceFrame()}.
   * <p>
   * If there's a {@link #constraint()} it is satisfied. Hence the translation actually
   * applied to the Frame may differ from {@code t} (since it can be filtered by the
   * {@link #constraint()}). Use {@link #setTranslation(Vec)} to directly translate the
   * Frame without taking the {@link #constraint()} into account.
   *
   * @see #rotate(Rotation)
   * @see #scale(float)
   */
  public void translate(Vec t) {
    if (constraint() != null)
      translation().add(constraint().constrainTranslation(t, this));
    else
      translation().add(t);
    modified();
  }

  // POSITION

  /**
   * Returns the position of the Frame, defined in the world coordinate system.
   *
   * @see #orientation()
   * @see #magnitude()
   * @see #setPosition(Vec)
   * @see #translation()
   */
  public final Vec position() {
    return inverseCoordinatesOf(new Vec(0, 0, 0));
  }

  /**
   * Sets the {@link #position()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setTranslation(Vec)} to define the local Frame translation (with respect
   * to the {@link #referenceFrame()}). The potential {@link #constraint()} of the Frame
   * is not taken into account, use {@link #setPositionWithConstraint(Vec)} instead.
   */
  public final void setPosition(Vec p) {
    if (referenceFrame() != null)
      setTranslation(referenceFrame().coordinatesOf(p));
    else
      setTranslation(p);
  }

  /**
   * Same as {@link #setPosition(Vec)}, but with {@code float} parameters.
   */
  public final void setPosition(float x, float y) {
    setPosition(new Vec(x, y));
  }

  /**
   * Same as {@link #setPosition(Vec)}, but with {@code float} parameters.
   */
  public final void setPosition(float x, float y, float z) {
    setPosition(new Vec(x, y, z));
  }

  /**
   * Same as {@link #setPosition(Vec)}, but if there's a {@link #constraint()} it is
   * satisfied (without modifying {@code position}).
   *
   * @see #setOrientationWithConstraint(Rotation)
   * @see #setTranslationWithConstraint(Vec)
   */
  public final void setPositionWithConstraint(Vec position) {
    if (referenceFrame() != null)
      position = referenceFrame().coordinatesOf(position);

    setTranslationWithConstraint(position);
  }

  // ROTATION

  /**
   * Returns the Frame rotation, defined with respect to the {@link #referenceFrame()}
   * (i.e, the current Rotation orientation).
   * <p>
   * Use {@link #orientation()} to get the result in world coordinates. These two values
   * are identical when the {@link #referenceFrame()} is {@code null} (default).
   *
   * @see #setRotation(Rotation)
   * @see #setRotationWithConstraint(Rotation)
   */
  public final Rotation rotation() {
    return rot;
  }

  /**
   * Set the current rotation. See the different {@link remixlab.dandelion.geom.Rotation}
   * constructors.
   * <p>
   * Sets the {@link #rotation()} of the Frame, locally defined with respect to the
   * {@link #referenceFrame()}.
   * <p>
   * Use {@link #setOrientation(Rotation)} to define the world coordinates
   * {@link #orientation()}. The potential {@link #constraint()} of the Frame is not taken
   * into account, use {@link #setRotationWithConstraint(Rotation)} instead.
   *
   * @see #setRotationWithConstraint(Rotation)
   * @see #rotation()
   * @see #setTranslation(Vec)
   */
  public final void setRotation(Rotation r) {
    rot = r;
    modified();
  }

  /**
   * Same as {@link #setRotation(Rotation)} but with {@code float} Rotation parameters.
   */
  public final void setRotation(float x, float y, float z, float w) {
    if (is2D()) {
      System.err.println("setRotation(float x, float y, float z, float w) is not available in 2D");
      return;
    }
    setRotation(new Quat(x, y, z, w));
  }

  /**
   * Defines a 2D {@link remixlab.dandelion.geom.Rotation}.
   *
   * @param a angle
   */
  public final void setRotation(float a) {
    if (is3D()) {
      System.err.println("setRotation(float a) is not available in 3D");
      return;
    }
    setRotation(new Rot(a));
  }

  /**
   * Same as {@link #setRotation(Rotation)}, but if there's a {@link #constraint()} it's
   * satisfied.
   *
   * @see #setTranslationWithConstraint(Vec)
   * @see #setOrientationWithConstraint(Rotation)
   * @see #setScaling(float)
   */
  public final void setRotationWithConstraint(Rotation rotation) {
    Rotation deltaQ;

    if (is3D())
      deltaQ = Quat.compose(rotation().inverse(), rotation);
    else
      deltaQ = Rot.compose(rotation().inverse(), rotation);

    if (constraint() != null)
      deltaQ = constraint().constrainRotation(deltaQ, this);

    deltaQ.normalize(); // Prevent numerical drift

    rotate(deltaQ);
  }

  /**
   * Rotates the Frame by {@code r} (defined in the Frame coordinate system):
   * {@code rotation().compose(r)}.
   * <p>
   * If there's a {@link #constraint()} it is satisfied. Hence the rotation actually
   * applied to the Frame may differ from {@code q} (since it can be filtered by the
   * {@link #constraint()}). Use {@link #setRotation(Rotation)} to directly rotate the
   * Frame without taking the {@link #constraint()} into account.
   *
   * @see #translate(Vec)
   */
  public final void rotate(Rotation r) {
    if (constraint() != null)
      rotation().compose(constraint().constrainRotation(r, this));
    else
      rotation().compose(r);
    if (is3D())
      ((Quat) rotation()).normalize(); // Prevents numerical drift
    modified();
  }

  /**
   * Same as {@link #rotate(Rotation)} but with {@code float} rotation parameters.
   */
  public final void rotate(float x, float y, float z, float w) {
    if (is2D()) {
      System.err.println("rotate(float x, float y, float z, float w) is not available in 2D");
      return;
    }
    rotate(new Quat(x, y, z, w));
  }

  /**
   * Makes the Frame {@link #rotate(Rotation)} by {@code rotation} around {@code point}.
   * <p>
   * {@code point} is defined in the world coordinate system, while the {@code rotation}
   * axis is defined in the Frame coordinate system.
   * <p>
   * If the Frame has a {@link #constraint()}, {@code rotation} is first constrained using
   * {@link remixlab.dandelion.constraint.Constraint#constrainRotation(Rotation, Frame)} .
   * Hence the rotation actually applied to the Frame may differ from {@code rotation}
   * (since it can be filtered by the {@link #constraint()}).
   * <p>
   * The translation which results from the filtered rotation around {@code point} is then
   * computed and filtered using
   * {@link remixlab.dandelion.constraint.Constraint#constrainTranslation(Vec, Frame)} .
   */
  public void rotateAroundPoint(Rotation rotation, Vec point) {
    if (constraint() != null)
      rotation = constraint().constrainRotation(rotation, this);

    this.rotation().compose(rotation);
    if (is3D())
      this.rotation().normalize(); // Prevents numerical drift

    Rotation q;
    if (is3D())
      q = new Quat(orientation().rotate(((Quat) rotation).axis()), rotation.angle());
    else
      q = new Rot(rotation.angle());
    Vec t = Vec.add(point, q.rotate(Vec.subtract(position(), point)));
    t.subtract(translation());
    if (constraint() != null)
      translate(constraint().constrainTranslation(t, this));
    else
      translate(t);
  }

  // TODO this one needs testing, specially 2d case
  public void rotateAroundFrame(Rotation rotation, Frame frame) {
    if (is3D()) {
      Vec euler = ((Quat) rotation).eulerAngles();
      rotateAroundFrame(euler.x(), euler.y(), euler.z(), frame);
    } else
      rotateAroundFrame(0, 0, rotation.angle(), frame);
  }

  public void rotateAroundFrame(float roll, float pitch, float yaw, Frame frame) {
    if (frame != null) {
      Frame ref = frame.get();
      Frame copy = get();
      copy.setReferenceFrame(ref);
      copy.setWorldMatrix(this);
      ref.rotate(new Quat(roll, pitch, yaw));
      setWorldMatrix(copy);
      return;
    }
  }

  // ORIENTATION

  /**
   * Returns the orientation of the Frame, defined in the world coordinate system.
   *
   * @see #position()
   * @see #magnitude()
   * @see #setOrientation(Rotation)
   * @see #rotation()
   */
  public final Rotation orientation() {
    Rotation res = rotation().get();
    Frame fr = referenceFrame();
    while (fr != null) {
      if (is3D())
        res = Quat.compose(fr.rotation(), res);
      else
        res = Rot.compose(fr.rotation(), res);
      fr = fr.referenceFrame();
    }
    return res;
  }

  /**
   * Sets the {@link #orientation()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setRotation(Rotation)} to define the local frame rotation (with respect
   * to the {@link #referenceFrame()}). The potential {@link #constraint()} of the Frame
   * is not taken into account, use {@link #setOrientationWithConstraint(Rotation)}
   * instead.
   */
  public final void setOrientation(Rotation q) {
    if (referenceFrame() != null) {
      if (is3D())
        setRotation(Quat.compose(referenceFrame().orientation().inverse(), q));
      else
        setRotation(Rot.compose(referenceFrame().orientation().inverse(), q));
    } else
      setRotation(q);
  }

  /**
   * Same as {@link #setOrientation(Rotation)}, but with {@code float} parameters.
   */
  public final void setOrientation(float x, float y, float z, float w) {
    setOrientation(new Quat(x, y, z, w));
  }

  /**
   * Same as {@link #setOrientation(Rotation)}, but if there's a {@link #constraint()} it
   * is satisfied (without modifying {@code orientation}).
   *
   * @see #setPositionWithConstraint(Vec)
   * @see #setRotationWithConstraint(Rotation)
   */
  public final void setOrientationWithConstraint(Rotation orientation) {
    if (referenceFrame() != null) {
      if (is3D())
        orientation = Quat.compose(referenceFrame().orientation().inverse(), orientation);
      else
        orientation = Rot.compose(referenceFrame().orientation().inverse(), orientation);
    }

    setRotationWithConstraint(orientation);
  }

  // SCALING

  /**
   * Returns the Frame scaling, defined with respect to the {@link #referenceFrame()}.
   * <p>
   * Use {@link #magnitude()} to get the result in world coordinates. These two values are
   * identical when the {@link #referenceFrame()} is {@code null} (default).
   *
   * @see #setScaling(float)
   */
  public final float scaling() {
    return scl;
  }

  /**
   * Sets the {@link #scaling()} of the frame, locally defined with respect to the
   * {@link #referenceFrame()}.
   * <p>
   * Use {@link #setMagnitude(float)} to define the world coordinates {@link #magnitude()}
   * .
   */
  public final void setScaling(float s) {
    if (Util.positive(s)) {
      scl = s;
      modified();
    } else
      System.out.println("Warning. Scaling should be positive. Nothing done");
  }

  /**
   * Scales the Frame according to {@code s}, locally defined with respect to the
   * {@link #referenceFrame()}.
   *
   * @see #rotate(Rotation)
   * @see #translate(Vec)
   */
  public void scale(float s) {
    setScaling(scaling() * s);
  }

  // MAGNITUDE

  /**
   * Returns the magnitude of the Frame, defined in the world coordinate system.
   *
   * @see #orientation()
   * @see #position()
   * @see #setPosition(Vec)
   * @see #translation()
   */
  public float magnitude() {
    if (referenceFrame() != null)
      return referenceFrame().magnitude() * scaling();
    else
      return scaling();
  }

  /**
   * Sets the {@link #magnitude()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setScaling(float)} to define the local Frame scaling (with respect to the
   * {@link #referenceFrame()}).
   */
  public final void setMagnitude(float m) {
    Frame refFrame = referenceFrame();
    if (refFrame != null)
      setScaling(m / refFrame.magnitude());
    else
      setScaling(m);
  }

  // ALIGNMENT

  /**
   * Convenience function that simply calls {@code alignWithFrame(frame, false, 0.85f)}
   */
  public final void alignWithFrame(Frame frame) {
    alignWithFrame(frame, false, 0.85f);
  }

  /**
   * Convenience function that simply calls {@code alignWithFrame(frame, move, 0.85f)}
   */
  public final void alignWithFrame(Frame frame, boolean move) {
    alignWithFrame(frame, move, 0.85f);
  }

  /**
   * Convenience function that simply calls
   * {@code alignWithFrame(frame, false, threshold)}
   */
  public final void alignWithFrame(Frame frame, float threshold) {
    alignWithFrame(frame, false, threshold);
  }

  /**
   * Aligns the Frame with {@code frame}, so that two of their axis are parallel.
   * <p>
   * If one of the X, Y and Z axis of the Frame is almost parallel to any of the X, Y, or
   * Z axis of {@code frame}, the Frame is rotated so that these two axis actually become
   * parallel.
   * <p>
   * If, after this first rotation, two other axis are also almost parallel, a second
   * alignment is performed. The two frames then have identical orientations, up to 90
   * degrees rotations.
   * <p>
   * {@code threshold} measures how close two axis must be to be considered parallel. It
   * is compared with the absolute values of the dot product of the normalized axis.
   * <p>
   * When {@code move} is set to {@code true}, the Frame {@link #position()} is also
   * affected by the alignment. The new Frame {@link #position()} is such that the
   * {@code frame} frame position (computed with {@link #coordinatesOf(Vec)}, in the Frame
   * coordinates system) does not change.
   * <p>
   * {@code frame} may be {@code null} and then represents the world coordinate system
   * (same convention than for the {@link #referenceFrame()}).
   */
  public final void alignWithFrame(Frame frame, boolean move, float threshold) {
    if (is3D()) {
      Vec[][] directions = new Vec[2][3];

      for (int d = 0; d < 3; ++d) {
        Vec dir = new Vec((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
        if (frame != null)
          directions[0][d] = frame.orientation().rotate(dir);
        else
          directions[0][d] = dir;
        directions[1][d] = orientation().rotate(dir);
      }

      float maxProj = 0.0f;
      float proj;
      short[] index = new short[2];
      index[0] = index[1] = 0;

      Vec vec = new Vec(0.0f, 0.0f, 0.0f);
      for (int i = 0; i < 3; ++i) {
        for (int j = 0; j < 3; ++j) {
          vec.set(directions[0][i]);
          proj = Math.abs(vec.dot(directions[1][j]));
          if ((proj) >= maxProj) {
            index[0] = (short) i;
            index[1] = (short) j;
            maxProj = proj;
          }
        }
      }
      Frame old = new Frame(this); // correct line
      // VFrame old = this.get();// this call the get overloaded method and
      // hence add the frame to the mouse grabber

      vec.set(directions[0][index[0]]);
      float coef = vec.dot(directions[1][index[1]]);

      if (Math.abs(coef) >= threshold) {
        vec.set(directions[0][index[0]]);
        Vec axis = vec.cross(directions[1][index[1]]);
        float angle = (float) Math.asin(axis.magnitude());
        if (coef >= 0.0)
          angle = -angle;
        // setOrientation(Quaternion(axis, angle) * orientation());
        Quat q = new Quat(axis, angle);
        q = Quat.multiply(((Quat) rotation()).inverse(), q);
        q = Quat.multiply(q, (Quat) orientation());
        rotate(q);

        // Try to align an other axis direction
        short d = (short) ((index[1] + 1) % 3);
        Vec dir = new Vec((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
        dir = orientation().rotate(dir);

        float max = 0.0f;
        for (int i = 0; i < 3; ++i) {
          vec.set(directions[0][i]);
          proj = Math.abs(vec.dot(dir));
          if (proj > max) {
            index[0] = (short) i;
            max = proj;
          }
        }

        if (max >= threshold) {
          vec.set(directions[0][index[0]]);
          axis = vec.cross(dir);
          angle = (float) Math.asin(axis.magnitude());
          vec.set(directions[0][index[0]]);
          if (vec.dot(dir) >= 0.0)
            angle = -angle;
          // setOrientation(Quaternion(axis, angle) * orientation());
          q.fromAxisAngle(axis, angle);
          q = Quat.multiply(((Quat) rotation()).inverse(), q);
          q = Quat.multiply(q, (Quat) orientation());
          rotate(q);
        }
      }
      if (move) {
        Vec center = new Vec(0.0f, 0.0f, 0.0f);
        if (frame != null)
          center = frame.position();

        vec = Vec.subtract(center, inverseTransformOf(old.coordinatesOf(center)));
        vec.subtract(translation());
        translate(vec);
      }
    } else {
      Rot o;
      if (frame != null)
        o = (Rot) frame.orientation();
      else
        o = new Rot();
      o.normalize(true);
      ((Rot) orientation()).normalize(true);

      float angle = 0; // if( (-QUARTER_PI <= delta) && (delta < QUARTER_PI) )
      float delta = Math.abs(o.angle() - orientation().angle());

      if (((float) Math.PI / 4 <= delta) && (delta < ((float) Math.PI * 3 / 4)))
        angle = (float) Math.PI / 2;
      else if ((((float) Math.PI * 3 / 4) <= delta) && (delta < ((float) Math.PI * 5 / 4)))
        angle = (float) Math.PI;
      else if ((((float) Math.PI * 5 / 4) <= delta) && (delta < ((float) Math.PI * 7 / 4)))
        angle = (float) Math.PI * 3 / 2;

      angle += o.angle();
      Rot other = new Rot(angle);
      other.normalize();
      setOrientation(other);
    }
  }

  /**
   * Translates the Frame so that its {@link #position()} lies on the line defined by
   * {@code origin} and {@code direction} (defined in the world coordinate system).
   * <p>
   * Simply uses an orthogonal projection. {@code direction} does not need to be
   * normalized.
   */
  public final void projectOnLine(Vec origin, Vec direction) {
    Vec position = position();
    Vec shift = Vec.subtract(origin, position);
    Vec proj = shift;
    proj = Vec.projectVectorOnAxis(proj, direction);
    setPosition(Vec.add(position, Vec.subtract(shift, proj)));
  }

  /**
   * Rotates the frame so that its {@link #xAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link remixlab.dandelion.geom.Quat#fromTo(Vec, Vec)}.
   *
   * @see #xAxis()
   * @see #setYAxis(Vec)
   * @see #setZAxis(Vec)
   */
  public void setXAxis(Vec axis) {
    if (is3D())
      rotate(new Quat(new Vec(1.0f, 0.0f, 0.0f), transformOf(axis)));
    else
      rotate(new Rot(new Vec(1.0f, 0.0f, 0.0f), transformOf(axis)));
  }

  /**
   * Rotates the frame so that its {@link #yAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link remixlab.dandelion.geom.Quat#fromTo(Vec, Vec)}.
   *
   * @see #yAxis()
   * @see #setYAxis(Vec)
   * @see #setZAxis(Vec)
   */
  public void setYAxis(Vec axis) {
    if (is3D())
      rotate(new Quat(new Vec(0.0f, 1.0f, 0.0f), transformOf(axis)));
    else
      rotate(new Rot(new Vec(0.0f, 1.0f, 0.0f), transformOf(axis)));
  }

  /**
   * Rotates the frame so that its {@link #zAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link remixlab.dandelion.geom.Quat#fromTo(Vec, Vec)}.
   *
   * @see #zAxis()
   * @see #setYAxis(Vec)
   * @see #setZAxis(Vec)
   */
  public void setZAxis(Vec axis) {
    if (is3D())
      rotate(new Quat(new Vec(0.0f, 0.0f, 1.0f), transformOf(axis)));
    else
      System.out.println("There's no point in setting the Z axis in 2D");
  }

  /**
   * Same as {@code return xAxis(true)}
   *
   * @see #xAxis(boolean)
   */
  public Vec xAxis() {
    return xAxis(true);
  }

  /**
   * Returns the x-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setXAxis(Vec)
   * @see #yAxis()
   * @see #zAxis()
   */
  public Vec xAxis(boolean positive) {
    Vec res;
    if (is3D()) {
      res = inverseTransformOf(new Vec(positive ? 1.0f : -1.0f, 0.0f, 0.0f));
      if (Util.diff(magnitude(), 1))
        res.normalize();
    } else {
      res = inverseTransformOf(new Vec(positive ? 1.0f : -1.0f, 0.0f));
      if (Util.diff(magnitude(), 1))
        res.normalize();
    }
    return res;
  }

  /**
   * Same as {@code return yAxis(true)}
   *
   * @see #yAxis(boolean)
   */
  public Vec yAxis() {
    return yAxis(true);
  }

  /**
   * Returns the y-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setYAxis(Vec)
   * @see #xAxis()
   * @see #zAxis()
   */
  public Vec yAxis(boolean positive) {
    Vec res;
    if (is3D()) {
      res = inverseTransformOf(new Vec(0.0f, positive ? 1.0f : -1.0f, 0.0f));
      if (Util.diff(magnitude(), 1))
        res.normalize();
    } else {
      res = inverseTransformOf(new Vec(0.0f, positive ? 1.0f : -1.0f));
      if (Util.diff(magnitude(), 1))
        res.normalize();
    }
    return res;
  }

  /**
   * Same as {@code return zAxis(true)}
   *
   * @see #zAxis(boolean)
   */
  public Vec zAxis() {
    return zAxis(true);
  }

  /**
   * Returns the z-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setZAxis(Vec)
   * @see #xAxis()
   * @see #yAxis()
   */
  public Vec zAxis(boolean positive) {
    Vec res = new Vec();
    if (is3D()) {
      res = inverseTransformOf(new Vec(0.0f, 0.0f, positive ? 1.0f : -1.0f));
      if (Util.diff(magnitude(), 1))
        res.normalize();
    }
    return res;
  }

  // CONVERSION

  /**
   * Returns the local transformation matrix represented by the Frame.
   * <p>
   * This method could be used in conjunction with {@code applyMatrix()} to modify the
   * {@link remixlab.dandelion.core.AbstractScene#modelView()} matrix from a Frame
   * hierarchy. For example, with this Frame hierarchy:
   * <p>
   * {@code Frame body = new Frame();} <br>
   * {@code Frame leftArm = new Frame();} <br>
   * {@code Frame rightArm = new Frame();} <br>
   * {@code leftArm.setReferenceFrame(body);} <br>
   * {@code rightArm.setReferenceFrame(body);} <br>
   * <p>
   * The associated drawing code should look like:
   * <p>
   * {@code scene.pushModelView();}<br>
   * {@code scene.applyMatrix(body.matrix());} <br>
   * {@code drawBody();} <br>
   * {@code scene.pushModelView();} <br>
   * {@code scene.applyMatrix(leftArm.matrix());} <br>
   * {@code drawArm();} <br>
   * {@code scene.popModelView();} <br>
   * {@code scene.pushModelView();} <br>
   * {@code scene.applyMatrix(rightArm.matrix());} <br>
   * {@code drawArm();} <br>
   * {@code scene.popModelView();} <br>
   * {@code scene.popModelView();} <br>
   * <p>
   * Note the use of nested {@code pushModelView()} and {@code popModelView()} blocks to
   * represent the frame hierarchy: {@code leftArm} and {@code rightArm} are both
   * correctly drawn with respect to the {@code body} coordinate system.
   * <p>
   * <b>Attention:</b> In Processing this technique is inefficient because
   * {@code papplet.applyMatrix} will try to calculate the inverse of the transform.
   * <p>
   * This matrix only represents the local Frame transformation (i.e., with respect to the
   * {@link #referenceFrame()}). Use {@link #worldMatrix()} to get the full Frame
   * transformation matrix (i.e., from the world to the Frame coordinate system). These
   * two match when the {@link #referenceFrame()} is {@code null}.
   * <p>
   * The result is only valid until the next call to {@code matrix()} or
   * {@link #worldMatrix()}. Use it immediately (as above).
   */
  public final Mat matrix() {
    Mat pM = new Mat();

    pM = rotation().matrix();

    pM.mat[12] = translation().vec[0];
    pM.mat[13] = translation().vec[1];
    pM.mat[14] = translation().vec[2];

    if (scaling() != 1) {
      pM.setM00(pM.m00() * scaling());
      pM.setM10(pM.m10() * scaling());
      pM.setM20(pM.m20() * scaling());

      pM.setM01(pM.m01() * scaling());
      pM.setM11(pM.m11() * scaling());
      pM.setM21(pM.m21() * scaling());

      pM.setM02(pM.m02() * scaling());
      pM.setM12(pM.m12() * scaling());
      pM.setM22(pM.m22() * scaling());
    }

    return pM;
  }

  /**
   * Returns the global transformation matrix represented by the Frame.
   * <p>
   * This method should be used in conjunction with {@code applyMatrix()} to modify the
   * {@link remixlab.dandelion.core.AbstractScene#modelView()} matrix from a Frame:
   * <p>
   * {@code // Here the modelview matrix corresponds to the world coordinate system.} <br>
   * {@code Frame fr = new Frame(pos, Rotation(from, to));} <br>
   * {@code scene.pushModelView();} <br>
   * {@code scene.applyMatrix(worldMatrix());} <br>
   * {@code // draw object in the fr coordinate system.} <br>
   * {@code scene.popModelView();} <br>
   * <p>
   * This matrix represents the global Frame transformation: the entire
   * {@link #referenceFrame()} hierarchy is taken into account to define the Frame
   * transformation from the world coordinate system. Use {@link #matrix()} to get the
   * local Frame transformation matrix (i.e. defined with respect to the
   * {@link #referenceFrame()}). These two match when the {@link #referenceFrame()} is
   * {@code null}.
   * <p>
   * <b>Attention:</b> The result is only valid until the next call to {@link #matrix()}
   * or {@code worldMatrix()}. Use it immediately (as above).
   */
  public final Mat worldMatrix() {
    if (referenceFrame() != null)
      return new Frame(position(), orientation(), magnitude()).matrix();
    else
      return matrix();
  }

  /**
   * Convenience function that simply calls {@code fromMatrix(pM, 1))}.
   *
   * @see #fromMatrix(Mat, float)
   */
  public final void fromMatrix(Mat pM) {
    fromMatrix(pM, 1);
  }

  /**
   * Sets the Frame from a Mat representation: rotation in the upper left 3x3 matrix and
   * translation on the last column. Scaling is defined separately in {@code scl}.
   * <p>
   * Hence, if a code fragment looks like:
   * <p>
   * {@code float [] m = new float [16]; m[0]=...;} <br>
   * {@code gl.glMultMatrixf(m);} <br>
   * <p>
   * It is equivalent to write:
   * <p>
   * {@code Frame fr = new Frame();} <br>
   * {@code fr.fromMatrix(m);} <br>
   * {@code applyMatrix(fr.matrix());} <br>
   * <p>
   * Using this conversion, you can benefit from the powerful Frame transformation methods
   * to translate points and vectors to and from the Frame coordinate system to any other
   * Frame coordinate system (including the world coordinate system). See
   * {@link #coordinatesOf(Vec)} and {@link #transformOf(Vec)}.
   */
  public final void fromMatrix(Mat pM, float scl) {
    if (Util.zero(pM.mat[15])) {
      System.out.println("Doing nothing: pM.mat[15] should be non-zero!");
      return;
    }

    translation().vec[0] = pM.mat[12] / pM.mat[15];
    translation().vec[1] = pM.mat[13] / pM.mat[15];
    translation().vec[2] = pM.mat[14] / pM.mat[15];

    float[][] r = new float[3][3];

    r[0][0] = pM.mat[0] / pM.mat[15];
    r[0][1] = pM.mat[4] / pM.mat[15];
    r[0][2] = pM.mat[8] / pM.mat[15];
    r[1][0] = pM.mat[1] / pM.mat[15];
    r[1][1] = pM.mat[5] / pM.mat[15];
    r[1][2] = pM.mat[9] / pM.mat[15];
    r[2][0] = pM.mat[2] / pM.mat[15];
    r[2][1] = pM.mat[6] / pM.mat[15];
    r[2][2] = pM.mat[10] / pM.mat[15];

    setScaling(scl);// calls modified() :P

    if (scaling() != 1) {
      r[0][0] = r[0][0] / scaling();
      r[1][0] = r[1][0] / scaling();
      r[2][0] = r[2][0] / scaling();

      r[0][1] = r[0][1] / scaling();
      r[1][1] = r[1][1] / scaling();
      r[2][1] = r[2][1] / scaling();

      if (this.is3D()) {
        r[0][2] = r[0][2] / scaling();
        r[1][2] = r[1][2] / scaling();
        r[2][2] = r[2][2] / scaling();
      }
    }

    Vec x = new Vec(r[0][0], r[1][0], r[2][0]);
    Vec y = new Vec(r[0][1], r[1][1], r[2][1]);
    Vec z = new Vec(r[0][2], r[1][2], r[2][2]);

    rotation().fromRotatedBasis(x, y, z);
  }

  /**
   * Same as {@code #setWorldMatrix(Frame)}.
   *
   * @see #setWorldMatrix(Frame)
   */
  public void set(Frame otherFrame) {
    setWorldMatrix(otherFrame);
  }

  /**
   * Sets {@link #position()}, {@link #orientation()} and {@link #magnitude()} values from
   * those of {@code otherFrame}.
   * <p>
   * After calling {@code set} a call to {@code this.equals(otherFrame)} should return
   * {@code true}.
   *
   * @see #setMatrix(Frame)
   */
  public void setWorldMatrix(Frame otherFrame) {
    if (otherFrame == null)
      return;
    setPosition(otherFrame.position());
    setOrientation(otherFrame.orientation());
    setMagnitude(otherFrame.magnitude());
  }

  /**
   * Sets {@link #translation()}, {@link #rotation()} and {@link #scaling()} values from
   * those of {@code otherFrame}.
   *
   * @see #setWorldMatrix(Frame)
   */
  public void setMatrix(Frame otherFrame) {
    if (otherFrame == null)
      return;
    setTranslation(otherFrame.translation());
    setRotation(otherFrame.rotation());
    setScaling(otherFrame.scaling());
  }

  /**
   * Returns a Frame representing the inverse of the Frame space transformation.
   * <p>
   * The the new Frame {@link #rotation()} is the
   * {@link remixlab.dandelion.geom.Rotation#inverse()} of the original rotation. Its
   * {@link #translation()} is the negated inverse rotated image of the original
   * translation. Its {@link #scaling()} is 1 / original scaling.
   * <p>
   * If a Frame is considered as a space rigid transformation, i.e., translation and
   * rotation, but no scaling (scaling=1), the inverse() Frame performs the inverse
   * transformation.
   * <p>
   * Only the local Frame transformation (i.e., defined with respect to the
   * {@link #referenceFrame()}) is inverted. Use {@link #worldInverse()} for a global
   * inverse.
   * <p>
   * The resulting Frame has the same {@link #referenceFrame()} as the Frame and a
   * {@code null} {@link #constraint()}.
   */
  public final Frame inverse() {
    Frame fr = new Frame(Vec.multiply(rotation().inverseRotate(translation()), -1), rotation().inverse(), 1 / scaling());
    fr.setReferenceFrame(referenceFrame());
    return fr;
  }

  /**
   * Returns the {@link #inverse()} of the Frame world transformation.
   * <p>
   * The {@link #orientation()} of the new Frame is the
   * {@link remixlab.dandelion.geom.Quat#inverse()} of the original orientation. Its
   * {@link #position()} is the negated and inverse rotated image of the original
   * position. The {@link #magnitude()} is the the original magnitude multiplicative
   * inverse.
   * <p>
   * The result Frame has a {@code null} {@link #referenceFrame()} and a {@code null}
   * {@link #constraint()}.
   * <p>
   * Use {@link #inverse()} for a local (i.e., with respect to {@link #referenceFrame()})
   * transformation inverse.
   */
  public final Frame worldInverse() {
    return (new Frame(Vec.multiply(orientation().inverseRotate(position()), -1), orientation().inverse(),
        1 / magnitude()));
  }

  // POINT CONVERSION

  /**
   * Returns the Frame coordinates of the point whose position in the {@code from}
   * coordinate system is {@code src} (converts from {@code from} to Frame).
   * <p>
   * {@link #coordinatesOfIn(Vec, Frame)} performs the inverse transformation.
   */
  public final Vec coordinatesOfFrom(Vec src, Frame from) {
    if (this == from)
      return src;
    else if (referenceFrame() != null)
      return localCoordinatesOf(referenceFrame().coordinatesOfFrom(src, from));
    else
      return localCoordinatesOf(from.inverseCoordinatesOf(src));
  }

  /**
   * Returns the {@code in} coordinates of the point whose position in the Frame
   * coordinate system is {@code src} (converts from Frame to {@code in}).
   * <p>
   * {@link #coordinatesOfFrom(Vec, Frame)} performs the inverse transformation.
   */
  public final Vec coordinatesOfIn(Vec src, Frame in) {
    Frame fr = this;
    Vec res = src;
    while ((fr != null) && (fr != in)) {
      res = fr.localInverseCoordinatesOf(res);
      fr = fr.referenceFrame();
    }

    if (fr != in)
      // in was not found in the branch of this, res is now expressed in the
      // world
      // coordinate system. Simply convert to in coordinate system.
      res = in.coordinatesOf(res);

    return res;
  }

  /**
   * Returns the Frame coordinates of a point {@code src} defined in the
   * {@link #referenceFrame()} coordinate system (converts from {@link #referenceFrame()}
   * to Frame).
   * <p>
   * {@link #localInverseCoordinatesOf(Vec)} performs the inverse conversion.
   *
   * @see #localTransformOf(Vec)
   */
  public final Vec localCoordinatesOf(Vec src) {
    return Vec.divide(rotation().inverseRotate(Vec.subtract(src, translation())), scaling());
  }

  /**
   * Returns the Frame coordinates of a point {@code src} defined in the world coordinate
   * system (converts from world to Frame).
   * <p>
   * {@link #inverseCoordinatesOf(Vec)} performs the inverse conversion.
   * {@link #transformOf(Vec)} converts vectors instead of coordinates.
   */
  public final Vec coordinatesOf(Vec src) {
    if (referenceFrame() != null)
      return localCoordinatesOf(referenceFrame().coordinatesOf(src));
    else
      return localCoordinatesOf(src);
  }

  // VECTOR CONVERSION

  /**
   * Returns the Frame transform of the vector whose coordinates in the {@code from}
   * coordinate system is {@code src} (converts vectors from {@code from} to Frame).
   * <p>
   * {@link #transformOfIn(Vec, Frame)} performs the inverse transformation.
   */
  public final Vec transformOfFrom(Vec src, Frame from) {
    if (this == from)
      return src;
    else if (referenceFrame() != null)
      return localTransformOf(referenceFrame().transformOfFrom(src, from));
    else
      return localTransformOf(from.inverseTransformOf(src));
  }

  /**
   * Returns the {@code in} transform of the vector whose coordinates in the Frame
   * coordinate system is {@code src} (converts vectors from Frame to {@code in}).
   * <p>
   * {@link #transformOfFrom(Vec, Frame)} performs the inverse transformation.
   */
  public final Vec transformOfIn(Vec src, Frame in) {
    Frame fr = this;
    Vec res = src;
    while ((fr != null) && (fr != in)) {
      res = fr.localInverseTransformOf(res);
      fr = fr.referenceFrame();
    }

    if (fr != in)
      // in was not found in the branch of this, res is now expressed in
      // the world coordinate system. Simply convert to in coordinate system.
      res = in.transformOf(res);

    return res;
  }

  /**
   * Returns the {@link #referenceFrame()} coordinates of a point {@code src} defined in
   * the Frame coordinate system (converts from Frame to {@link #referenceFrame()}).
   * <p>
   * {@link #localCoordinatesOf(Vec)} performs the inverse conversion.
   *
   * @see #localInverseTransformOf(Vec)
   */
  public final Vec localInverseCoordinatesOf(Vec src) {
    return Vec.add(rotation().rotate(Vec.multiply(src, scaling())), translation());
  }

  /**
   * Returns the world coordinates of the point whose position in the Frame coordinate
   * system is {@code src} (converts from Frame to world).
   * <p>
   * {@link #coordinatesOf(Vec)} performs the inverse conversion. Use
   * {@link #inverseTransformOf(Vec)} to transform vectors instead of coordinates.
   */
  public final Vec inverseCoordinatesOf(Vec src) {
    Frame fr = this;
    Vec res = src;
    while (fr != null) {
      res = fr.localInverseCoordinatesOf(res);
      fr = fr.referenceFrame();
    }
    return res;
  }

  /**
   * Returns the Frame transform of a vector {@code src} defined in the world coordinate
   * system (converts vectors from world to Frame).
   * <p>
   * {@link #inverseTransformOf(Vec)} performs the inverse transformation.
   * {@link #coordinatesOf(Vec)} converts coordinates instead of vectors (here only the
   * rotational part of the transformation is taken into account).
   */
  public final Vec transformOf(Vec src) {
    if (referenceFrame() != null)
      return localTransformOf(referenceFrame().transformOf(src));
    else
      return localTransformOf(src);
  }

  /**
   * Returns the world transform of the vector whose coordinates in the Frame coordinate
   * system is {@code src} (converts vectors from Frame to world).
   * <p>
   * {@link #transformOf(Vec)} performs the inverse transformation. Use
   * {@link #inverseCoordinatesOf(Vec)} to transform coordinates instead of vectors.
   */
  public final Vec inverseTransformOf(Vec src) {
    Frame fr = this;
    Vec res = src;
    while (fr != null) {
      res = fr.localInverseTransformOf(res);
      fr = fr.referenceFrame();
    }
    return res;
  }

  /**
   * Returns the Frame transform of a vector {@code src} defined in the
   * {@link #referenceFrame()} coordinate system (converts vectors from
   * {@link #referenceFrame()} to Frame).
   * <p>
   * {@link #localInverseTransformOf(Vec)} performs the inverse transformation.
   *
   * @see #localCoordinatesOf(Vec)
   */
  public final Vec localTransformOf(Vec src) {
    return Vec.divide(rotation().inverseRotate(src), scaling());
  }

  /**
   * Returns the {@link #referenceFrame()} transform of a vector {@code src} defined in
   * the Frame coordinate system (converts vectors from Frame to {@link #referenceFrame()}
   * ).
   * <p>
   * {@link #localTransformOf(Vec)} performs the inverse transformation.
   *
   * @see #localInverseCoordinatesOf(Vec)
   */
  public final Vec localInverseTransformOf(Vec src) {
    return rotation().rotate(Vec.multiply(src, scaling()));
  }
}
