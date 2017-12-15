/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights refserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.primitives;

import remixlab.timing.TimingHandler;
import remixlab.primitives.constraint.Constraint;

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
 * {@code Frame fr(new Vector(0.5,0,0), new Quaternion(new Vector(0,1,0), new Vector(1,1,1)));} <br>
 * {@code graph.pushModelView();} <br>
 * {@code graph.applyModelView(fr.matrix());} <br>
 * {@code // Draw your object here, in the local fr coordinate system.} <br>
 * {@code graph.popModelView();} <br>
 * <p>
 * Many functions are provided to transform a point from one coordinate system (Frame) to
 * an other: see {@link #coordinatesOf(Vector)}, {@link #inverseCoordinatesOf(Vector)},
 * {@link #coordinatesOfIn(Vector, Frame)}, {@link #coordinatesOfFrom(Vector, Frame)}...
 * <p>
 * You may also want to transform a vector (such as a normal), which corresponds to
 * applying only the rotational part of the frame transformation: see
 * {@link #transformOf(Vector)} and {@link #inverseTransformOf(Vector)}.
 * <p>
 * The {@link #translation()}, {@link #rotation()} and uniform positive {@link #scaling()}
 * that are encapsulated in a Frame can also be used to represent an angle preserving
 * transformation of space. Such a transformation can also be interpreted as a change of
 * coordinate system, and the coordinate system conversion functions actually allow you to
 * use a Frame as an angle preserving transformation. Use
 * {@link #inverseCoordinatesOf(Vector)} (resp. {@link #coordinatesOf(Vector)}) to apply the
 * transformation (resp. its inverse). Note the inversion.
 * <p>
 * <p>
 * <h3>Hierarchy of Frames</h3>
 * <p>
 * The position, orientation and magnitude of a Frame are actually defined with respect to
 * a {@link #reference()}. The default {@link #reference()} is the world
 * coordinate system (represented by a {@code null} {@link #reference()}). If you
 * {@link #setReference(Frame)} to a different Frame, you must then differentiate:
 * <p>
 * <ul>
 * <li>The <b>local</b> {@link #translation()}, {@link #rotation()} and {@link #scaling()}
 * , defined with respect to the {@link #reference()}.</li>
 * <li>the <b>global</b> {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}, always defined with respect to the world coordinate system.</li>
 * </ul>
 * <p>
 * A Frame is actually defined by its {@link #translation()} with respect to its
 * {@link #reference()}, then by {@link #rotation()} of the coordinate system around
 * the new translated origin and then by a uniform positive {@link #scaling()} along its
 * rotated axes.
 * <p>
 * This terminology for <b>local</b> ({@link #translation()}, {@link #rotation()} and
 * {@link #scaling()}) and <b>global</b> ( {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}) definitions is used in all the methods' names and should be
 * sufficient to prevent ambiguities. These notions are obviously identical when the
 * {@link #reference()} is {@code null}, i.e., when the frame is defined in the world
 * coordinate system (the one you are left with after calling a graph preDraw() method).
 * <p>
 * Frames can hence easily be organized in a tree hierarchy, which root is the world
 * coordinate system. A loop in the hierarchy would result in an inconsistent (multiple)
 * Frame definition. Therefore {@link #settingAsReferenceWillCreateALoop(Frame)}
 * checks this and prevents {@link #reference()} from creating such a loop.
 * <p>
 * This frame hierarchy is used in methods like {@link #coordinatesOfIn(Vector, Frame)},
 * {@link #coordinatesOfFrom(Vector, Frame)} ... which allow coordinates (or vector)
 * conversions from a Frame to any other one (including the world coordinate system).
 * <p>
 * <h3>Constraints</h3>
 * <p>
 * An interesting feature of Frames is that their displacements can be constrained. When a
 * {@link remixlab.primitives.constraint.Constraint} is attached to a Frame, it filters the
 * inputGrabber of {@link #translate(Vector)} and {@link #rotate(Quaternion)}, and only the resulting
 * filtered motion is applied to the Frame. The default {@link #constraint()} {@code null}
 * resulting in no filtering. Use {@link #setConstraint(Constraint)} to attach a
 * Constraint to a frame.
 * <p>
 * Classical constraints are provided for convenience (see
 * {@link remixlab.primitives.constraint.LocalConstraint},
 * {@link remixlab.primitives.constraint.WorldConstraint} and
 * {@link remixlab.primitives.constraint.EyeConstraint}) and new constraints can very
 * easily be implemented.
 * <p>
 * <h3>Derived classes</h3>
 * <p>
 * The Node class inherits Frame and implements
 * all sorts of motion actions, so that a Frame (and hence an object) can be manipulated
 * in the graph by whatever user interaction means you can imagine.
 */
public class Frame {
  /**
   * Returns whether or not this Frame matches other.
   *
   * @param other frame
   */
  public boolean matches(Frame other) {
    return translation().matches(other.translation()) && rotation().matches(other.rotation()) && scaling() == other.scaling();
  }

  protected Vector _translation;
  protected float _scaling;
  protected Quaternion _rotation;
  protected Frame _reference;
  protected Constraint _constraint;
  protected long _lastUpdate;

  /**
   * Same as {@code this(null, new Vector(), three_d ? new Quaternion() : new Rot(), 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame() {
    this(null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(null, new Vector(), r, s)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Quaternion rotation, float scaling) {
    this(null, new Vector(), rotation, scaling);
  }

  /**
   * Same as {@code this(p, r, 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Vector translation, Quaternion rotation) {
    this(translation, rotation, 1);
  }

  /**
   * Same as {@code this(null, p, r, s)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Vector translation, Quaternion rotation, float scaling) {
    this(null, translation, rotation, scaling);
  }

  /**
   * Same as {@code this(reference, p, r, 1)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Vector translation, Quaternion rotation) {
    this(reference, translation, rotation, 1);
  }

  /**
   * Same as {@code this(reference, new Vector(), r, 1)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Quaternion rotation, float scaling) {
    this(reference, new Vector(), rotation, 1);
  }

  /**
   * Creates a Frame with {@code reference} as {@link #reference()}, and
   * {@code p}, {@code r} and {@code s} as the frame {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   */
  public Frame(Frame reference, Vector translation, Quaternion rotation, float scaling) {
    setTranslation(translation);
    setRotation(rotation);
    setScaling(scaling);
    setReference(reference);
  }

  protected Frame(Frame other) {
    _translation = other.translation().get();
    _rotation = other.rotation().get();
    _scaling = other.scaling();
    _reference = other.reference();
    _constraint = other.constraint();
  }

  public Frame detach() {
    Frame frame = new Frame();
    frame.setWorldMatrix(this);
    return frame;
  }

  public Frame get() {
    return new Frame(this);
  }

  // MODIFIED

  /**
   * Internal use. Automatically call by all methods which change the Frame state.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
  }

  /**
   * @return the last frame the Frame was updated.
   */
  public long lastUpdate() {
    return _lastUpdate;
  }

  // REFERENCE_FRAME

  /**
   * Returns the reference Frame, in which coordinates system the Frame is defined.
   * <p>
   * The Frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are
   * defined with respect to the {@link #reference()} coordinate system. A
   * {@code null} reference Frame (default value) means that the Frame is defined in the
   * world coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to
   * recursively convert values along the reference Frame chain and to get values
   * expressed in the world coordinate system. The values match when the reference Frame
   * is {@code null}.
   * <p>
   * Use {@link #setReference(Frame)} to set this value and create a Frame hierarchy.
   * Convenient functions allow you to convert coordinates from one Frame to another: see
   * {@link #coordinatesOf(Vector)}, {@link #localCoordinatesOf(Vector)} ,
   * {@link #coordinatesOfIn(Vector, Frame)} and their inverse functions.
   * <p>
   * Vectors can also be converted using {@link #transformOf(Vector)},
   * {@link #transformOfIn(Vector, Frame)}, {@link #localTransformOf(Vector)} and their inverse
   * functions.
   */
  public Frame reference() {
    return _reference;
  }

  /**
   * Sets the {@link #reference()} of the Frame.
   * <p>
   * The Frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are then
   * defined in the {@link #reference()} coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to express
   * these in the world coordinate system.
   * <p>
   * Using this method, you can create a hierarchy of Frames. This hierarchy needs to be a
   * tree, which root is the world coordinate system (i.e., {@code null}
   * {@link #reference()}). No action is performed if setting {@code refFrame} as the
   * {@link #reference()} would create a loop in the Frame hierarchy.
   */
  public void setReference(Frame reference) {
    if (settingAsReferenceWillCreateALoop(reference)) {
      System.out.println("Frame.setReference would create a loop in Frame hierarchy. Nothing done.");
      return;
    }
    if (reference() == reference)
      return;
    _reference = reference;
    _modified();
  }

  /**
   * Returns {@code true} if setting {@code frame} as the Frame's
   * {@link #reference()} would create a loop in the Frame hierarchy.
   */
  public boolean settingAsReferenceWillCreateALoop(Frame frame) {
    Frame f = frame;
    while (f != null) {
      if (f == Frame.this)
        return true;
      f = f.reference();
    }
    return false;
  }

  // CONSTRAINT

  /**
   * Returns the current {@link remixlab.primitives.constraint.Constraint} applied to the
   * Frame.
   * <p>
   * A {@code null} value (default) means that no Constraint is used to filter the Frame
   * translation and rotation.
   * <p>
   * See the Constraint class documentation for details.
   */
  public Constraint constraint() {
    return _constraint;
  }

  /**
   * Sets the {@link #constraint()} attached to the Frame.
   * <p>
   * A {@code null} value means no constraint.
   */
  public void setConstraint(Constraint constraint) {
    _constraint = constraint;
  }

  // TRANSLATION

  /**
   * Returns the Frame translation, defined with respect to the {@link #reference()}.
   * <p>
   * Use {@link #position()} to get the result in world coordinates. These two values are
   * identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setTranslation(Vector)
   * @see #setTranslationWithConstraint(Vector)
   */
  public Vector translation() {
    return _translation;
  }

  /**
   * Sets the {@link #translation()} of the frame, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Use {@link #setPosition(Vector)} to define the world coordinates {@link #position()}.
   * Use {@link #setTranslationWithConstraint(Vector)} to take into account the potential
   * {@link #constraint()} of the Frame.
   */
  public void setTranslation(Vector translation) {
    _translation = translation;
    _modified();
  }

  /**
   * Same as {@link #setTranslation(Vector)}, but if there's a {@link #constraint()} it is
   * satisfied.
   *
   * @see #setRotationWithConstraint(Quaternion)
   * @see #setPositionWithConstraint(Vector)
   * @see #setScaling(float)
   */
  public void setTranslationWithConstraint(Vector translation) {
    Vector deltaT = Vector.subtract(translation, this.translation());
    if (constraint() != null)
      deltaT = constraint().constrainTranslation(deltaT, this);

    translate(deltaT);
  }

  /**
   * Same as {@link #setTranslation(Vector)}, but with {@code float} parameters.
   */
  public void setTranslation(float x, float y) {
    setTranslation(new Vector(x, y));
  }

  /**
   * Same as {@link #setTranslation(Vector)}, but with {@code float} parameters.
   */
  public void setTranslation(float x, float y, float z) {
    setTranslation(new Vector(x, y, z));
  }

  /**
   * Same as {@link #translate(Vector)} but with {@code float} parameters.
   */
  public void translate(float x, float y, float z) {
    translate(new Vector(x, y, z));
  }

  /**
   * Same as {@link #translate(Vector)} but with {@code float} parameters.
   */
  public void translate(float x, float y) {
    translate(new Vector(x, y));
  }

  /**
   * Translates the Frame according to {@code t}, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * If there's a {@link #constraint()} it is satisfied. Hence the translation actually
   * applied to the Frame may differ from {@code t} (since it can be filtered by the
   * {@link #constraint()}). Use {@link #setTranslation(Vector)} to directly translate the
   * Frame without taking the {@link #constraint()} into account.
   *
   * @see #rotate(Quaternion)
   * @see #scale(float)
   */
  public void translate(Vector vector) {
    if (constraint() != null)
      translation().add(constraint().constrainTranslation(vector, this));
    else
      translation().add(vector);
    _modified();
  }

  // POSITION

  /**
   * Returns the position of the Frame, defined in the world coordinate system.
   *
   * @see #orientation()
   * @see #magnitude()
   * @see #setPosition(Vector)
   * @see #translation()
   */
  public Vector position() {
    return inverseCoordinatesOf(new Vector(0, 0, 0));
  }

  /**
   * Sets the {@link #position()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setTranslation(Vector)} to define the local Frame translation (with respect
   * to the {@link #reference()}). The potential {@link #constraint()} of the Frame
   * is not taken into account, use {@link #setPositionWithConstraint(Vector)} instead.
   */
  public void setPosition(Vector position) {
    if (reference() != null)
      setTranslation(reference().coordinatesOf(position));
    else
      setTranslation(position);
  }

  /**
   * Same as {@link #setPosition(Vector)}, but with {@code float} parameters.
   */
  public void setPosition(float x, float y) {
    setPosition(new Vector(x, y));
  }

  /**
   * Same as {@link #setPosition(Vector)}, but with {@code float} parameters.
   */
  public void setPosition(float x, float y, float z) {
    setPosition(new Vector(x, y, z));
  }

  /**
   * Same as {@link #setPosition(Vector)}, but if there's a {@link #constraint()} it is
   * satisfied (without modifying {@code position}).
   *
   * @see #setOrientationWithConstraint(Quaternion)
   * @see #setTranslationWithConstraint(Vector)
   */
  public void setPositionWithConstraint(Vector position) {
    if (reference() != null)
      position = reference().coordinatesOf(position);

    setTranslationWithConstraint(position);
  }

  // ROTATION

  /**
   * Returns the Frame rotation, defined with respect to the {@link #reference()}
   * (i.e, the current Quaternion orientation).
   * <p>
   * Use {@link #orientation()} to get the result in world coordinates. These two values
   * are identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setRotation(Quaternion)
   * @see #setRotationWithConstraint(Quaternion)
   */
  public Quaternion rotation() {
    return _rotation;
  }

  /**
   * Set the current rotation. See the different {@link Quaternion}
   * constructors.
   * <p>
   * Sets the {@link #rotation()} of the Frame, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Use {@link #setOrientation(Quaternion)} to define the world coordinates
   * {@link #orientation()}. The potential {@link #constraint()} of the Frame is not taken
   * into account, use {@link #setRotationWithConstraint(Quaternion)} instead.
   *
   * @see #setRotationWithConstraint(Quaternion)
   * @see #rotation()
   * @see #setTranslation(Vector)
   */
  public void setRotation(Quaternion rotation) {
    _rotation = rotation;
    _modified();
  }

  /**
   * Same as {@link #setRotation(Quaternion)} but with {@code float} Quaternion parameters.
   */
  public void setRotation(float x, float y, float z, float w) {
    setRotation(new Quaternion(x, y, z, w));
  }

  /**
   * Same as {@link #setRotation(Quaternion)}, but if there's a {@link #constraint()} it's
   * satisfied.
   *
   * @see #setTranslationWithConstraint(Vector)
   * @see #setOrientationWithConstraint(Quaternion)
   * @see #setScaling(float)
   */
  public void setRotationWithConstraint(Quaternion rotation) {
    Quaternion deltaQ;
    deltaQ = Quaternion.compose(rotation().inverse(), rotation);
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
   * {@link #constraint()}). Use {@link #setRotation(Quaternion)} to directly rotate the
   * Frame without taking the {@link #constraint()} into account.
   *
   * @see #translate(Vector)
   */
  public void rotate(Quaternion quaternion) {
    if (constraint() != null)
      rotation().compose(constraint().constrainRotation(quaternion, this));
    else
      rotation().compose(quaternion);
    rotation().normalize(); // Prevents numerical drift
    _modified();
  }

  /**
   * Same as {@link #rotate(Quaternion)} but with {@code float} rotation parameters.
   */
  public void rotate(float x, float y, float z, float w) {
    rotate(new Quaternion(x, y, z, w));
  }

  /**
   * Makes the Frame {@link #rotate(Quaternion)} by {@code rotation} around {@code point}.
   * <p>
   * {@code point} is defined in the world coordinate system, while the {@code rotation}
   * axis is defined in the Frame coordinate system.
   * <p>
   * If the Frame has a {@link #constraint()}, {@code rotation} is first constrained using
   * {@link remixlab.primitives.constraint.Constraint#constrainRotation(Quaternion, Frame)} .
   * Hence the rotation actually applied to the Frame may differ from {@code rotation}
   * (since it can be filtered by the {@link #constraint()}).
   * <p>
   * The translation which results from the filtered rotation around {@code point} is then
   * computed and filtered using
   * {@link remixlab.primitives.constraint.Constraint#constrainTranslation(Vector, Frame)} .
   */
  public void rotateAroundPoint(Quaternion rotation, Vector point) {
    if (constraint() != null)
      rotation = constraint().constrainRotation(rotation, this);

    this.rotation().compose(rotation);
    this.rotation().normalize(); // Prevents numerical drift

    Quaternion q = new Quaternion(orientation().rotate(rotation.axis()), rotation.angle());

    Vector t = Vector.add(point, q.rotate(Vector.subtract(position(), point)));
    t.subtract(translation());
    if (constraint() != null)
      translate(constraint().constrainTranslation(t, this));
    else
      translate(t);
  }

  public void rotateAroundFrame(Quaternion rotation, Frame frame) {
    Vector euler = rotation.eulerAngles();
    rotateAroundFrame(euler.x(), euler.y(), euler.z(), frame);
  }

  public void rotateAroundFrame(float roll, float pitch, float yaw, Frame frame) {
    if (frame != null) {
      Frame ref = frame.get();
      Frame copy = get();
      copy.setReference(ref);
      copy.setWorldMatrix(this);
      ref.rotate(new Quaternion(roll, pitch, yaw));
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
   * @see #setOrientation(Quaternion)
   * @see #rotation()
   */
  public Quaternion orientation() {
    Quaternion quaternion = rotation().get();
    Frame fr = reference();
    while (fr != null) {
        quaternion = Quaternion.compose(fr.rotation(), quaternion);
      fr = fr.reference();
    }
    return quaternion;
  }

  /**
   * Sets the {@link #orientation()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setRotation(Quaternion)} to define the local frame rotation (with respect
   * to the {@link #reference()}). The potential {@link #constraint()} of the Frame
   * is not taken into account, use {@link #setOrientationWithConstraint(Quaternion)}
   * instead.
   */
  public void setOrientation(Quaternion quaternion) {
    setRotation(reference() != null ? Quaternion.compose(reference().orientation().inverse(), quaternion) : quaternion);
  }

  /**
   * Same as {@link #setOrientation(Quaternion)}, but with {@code float} parameters.
   */
  public void setOrientation(float x, float y, float z, float w) {
    setOrientation(new Quaternion(x, y, z, w));
  }

  /**
   * Same as {@link #setOrientation(Quaternion)}, but if there's a {@link #constraint()} it
   * is satisfied (without modifying {@code orientation}).
   *
   * @see #setPositionWithConstraint(Vector)
   * @see #setRotationWithConstraint(Quaternion)
   */
  public void setOrientationWithConstraint(Quaternion orientation) {
    if (reference() != null)
      orientation = Quaternion.compose(reference().orientation().inverse(), orientation);
    setRotationWithConstraint(orientation);
  }

  // SCALING

  /**
   * Returns the Frame scaling, defined with respect to the {@link #reference()}.
   * <p>
   * Use {@link #magnitude()} to get the result in world coordinates. These two values are
   * identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setScaling(float)
   */
  public float scaling() {
    return _scaling;
  }

  /**
   * Sets the {@link #scaling()} of the frame, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Use {@link #setMagnitude(float)} to define the world coordinates {@link #magnitude()}
   * .
   */
  public void setScaling(float scaling) {
    if (scaling > 0) {
      _scaling = scaling;
      _modified();
    } else
      System.out.println("Warning. Scaling should be positive. Nothing done");
  }

  /**
   * Scales the Frame according to {@code s}, locally defined with respect to the
   * {@link #reference()}.
   *
   * @see #rotate(Quaternion)
   * @see #translate(Vector)
   */
  public void scale(float scaling) {
    setScaling(scaling() * scaling);
  }

  // MAGNITUDE

  /**
   * Returns the magnitude of the Frame, defined in the world coordinate system.
   *
   * @see #orientation()
   * @see #position()
   * @see #setPosition(Vector)
   * @see #translation()
   */
  public float magnitude() {
    if (reference() != null)
      return reference().magnitude() * scaling();
    else
      return scaling();
  }

  /**
   * Sets the {@link #magnitude()} of the Frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setScaling(float)} to define the local Frame scaling (with respect to the
   * {@link #reference()}).
   */
  public void setMagnitude(float magnitude) {
    Frame refFrame = reference();
    if (refFrame != null)
      setScaling(magnitude / refFrame.magnitude());
    else
      setScaling(magnitude);
  }

  // ALIGNMENT

  /**
   * Convenience function that simply calls {@code alignWithFrame(frame, false, 0.85f)}
   */
  public void alignWithFrame(Frame frame) {
    alignWithFrame(frame, false, 0.85f);
  }

  /**
   * Convenience function that simply calls {@code alignWithFrame(frame, move, 0.85f)}
   */
  public void alignWithFrame(Frame frame, boolean move) {
    alignWithFrame(frame, move, 0.85f);
  }

  /**
   * Convenience function that simply calls
   * {@code alignWithFrame(frame, false, threshold)}
   */
  public void alignWithFrame(Frame frame, float threshold) {
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
   * alignment is performed. The two nodes then have identical orientations, up to 90
   * degrees rotations.
   * <p>
   * {@code threshold} measures how close two axis must be to be considered parallel. It
   * is compared with the absolute values of the dot product of the normalized axis.
   * <p>
   * When {@code move} is set to {@code true}, the Frame {@link #position()} is also
   * affected by the alignment. The new Frame {@link #position()} is such that the
   * {@code frame} frame position (computed with {@link #coordinatesOf(Vector)}, in the Frame
   * coordinates system) does not change.
   * <p>
   * {@code frame} may be {@code null} and then represents the world coordinate system
   * (same convention than for the {@link #reference()}).
   */
  public void alignWithFrame(Frame frame, boolean move, float threshold) {
    Vector[][] directions = new Vector[2][3];

    for (int d = 0; d < 3; ++d) {
      Vector dir = new Vector((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
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

    Vector vector = new Vector(0.0f, 0.0f, 0.0f);
    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 3; ++j) {
        vector.set(directions[0][i]);
        proj = Math.abs(vector.dot(directions[1][j]));
        if ((proj) >= maxProj) {
          index[0] = (short) i;
          index[1] = (short) j;
          maxProj = proj;
        }
      }
    }
    Frame old = new Frame(this); // correct line
    // VFrame old = this.get();// this call the get overloaded method and
    // hence addGrabber the frame to the mouse _grabber

    vector.set(directions[0][index[0]]);
    float coef = vector.dot(directions[1][index[1]]);

    if (Math.abs(coef) >= threshold) {
      vector.set(directions[0][index[0]]);
      Vector axis = vector.cross(directions[1][index[1]]);
      float angle = (float) Math.asin(axis.magnitude());
      if (coef >= 0.0)
        angle = -angle;
      // setOrientation(Quaternion(axis, angle) * orientation());
      Quaternion q = new Quaternion(axis, angle);
      q = Quaternion.multiply(rotation().inverse(), q);
      q = Quaternion.multiply(q, orientation());
      rotate(q);

      // Try to align an other axis direction
      short d = (short) ((index[1] + 1) % 3);
      Vector dir = new Vector((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
      dir = orientation().rotate(dir);

      float max = 0.0f;
      for (int i = 0; i < 3; ++i) {
        vector.set(directions[0][i]);
        proj = Math.abs(vector.dot(dir));
        if (proj > max) {
          index[0] = (short) i;
          max = proj;
        }
      }

      if (max >= threshold) {
        vector.set(directions[0][index[0]]);
        axis = vector.cross(dir);
        angle = (float) Math.asin(axis.magnitude());
        vector.set(directions[0][index[0]]);
        if (vector.dot(dir) >= 0.0)
          angle = -angle;
        // setOrientation(Quaternion(axis, angle) * orientation());
        q.fromAxisAngle(axis, angle);
        q = Quaternion.multiply(rotation().inverse(), q);
        q = Quaternion.multiply(q, orientation());
        rotate(q);
      }
    }
    if (move) {
      Vector center = new Vector(0.0f, 0.0f, 0.0f);
      if (frame != null)
        center = frame.position();

      vector = Vector.subtract(center, inverseTransformOf(old.coordinatesOf(center)));
      vector.subtract(translation());
      translate(vector);
    }
  }

  /**
   * Translates the Frame so that its {@link #position()} lies on the line defined by
   * {@code origin} and {@code direction} (defined in the world coordinate system).
   * <p>
   * Simply uses an orthogonal projection. {@code direction} does not need to be
   * normalized.
   */
  public void projectOnLine(Vector origin, Vector direction) {
    Vector position = position();
    Vector shift = Vector.subtract(origin, position);
    Vector proj = shift;
    proj = Vector.projectVectorOnAxis(proj, direction);
    setPosition(Vector.add(position, Vector.subtract(shift, proj)));
  }

  /**
   * Rotates the frame so that its {@link #xAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #xAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setXAxis(Vector axis) {
    rotate(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), transformOf(axis)));
  }

  /**
   * Rotates the frame so that its {@link #yAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #yAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setYAxis(Vector axis) {
    rotate(new Quaternion(new Vector(0.0f, 1.0f, 0.0f), transformOf(axis)));
  }

  /**
   * Rotates the frame so that its {@link #zAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   * <p>
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #zAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setZAxis(Vector axis) {
    rotate(new Quaternion(new Vector(0.0f, 0.0f, 1.0f), transformOf(axis)));
  }

  /**
   * Same as {@code return xAxis(true)}
   *
   * @see #xAxis(boolean)
   */
  public Vector xAxis() {
    return xAxis(true);
  }

  /**
   * Returns the x-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setXAxis(Vector)
   * @see #yAxis()
   * @see #zAxis()
   */
  public Vector xAxis(boolean positive) {
    Vector axis = inverseTransformOf(new Vector(positive ? 1.0f : -1.0f, 0.0f, 0.0f));
    if (magnitude() != 1)
      axis.normalize();
    return axis;
  }

  /**
   * Same as {@code return yAxis(true)}
   *
   * @see #yAxis(boolean)
   */
  public Vector yAxis() {
    return yAxis(true);
  }

  /**
   * Returns the y-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setYAxis(Vector)
   * @see #xAxis()
   * @see #zAxis()
   */
  public Vector yAxis(boolean positive) {
    Vector axis = inverseTransformOf(new Vector(0.0f, positive ? 1.0f : -1.0f, 0.0f));
    if (magnitude() != 1)
      axis.normalize();
    return axis;
  }

  /**
   * Same as {@code return zAxis(true)}
   *
   * @see #zAxis(boolean)
   */
  public Vector zAxis() {
    return zAxis(true);
  }

  /**
   * Returns the z-axis of the frame, represented as a normalized vector defined in the
   * world coordinate system.
   *
   * @see #setZAxis(Vector)
   * @see #xAxis()
   * @see #yAxis()
   */
  public Vector zAxis(boolean positive) {
    Vector axis = inverseTransformOf(new Vector(0.0f, 0.0f, positive ? 1.0f : -1.0f));
    if (magnitude() != 1)
      axis.normalize();
    return axis;
  }

  // CONVERSION

  /**
   * Returns the local transformation matrix represented by the Frame.
   * <p>
   * This method could be used in conjunction with {@code applyMatrix()} to modify a graph
   * modelView() matrix from a frame hierarchy. For example, with this frame hierarchy:
   * <p>
   * {@code Frame body = new Frame();} <br>
   * {@code Frame leftArm = new Frame();} <br>
   * {@code Frame rightArm = new Frame();} <br>
   * {@code leftArm.setReference(body);} <br>
   * {@code rightArm.setReference(body);} <br>
   * <p>
   * The associated drawing code should look like:
   * <p>
   * {@code graph.pushModelView();}<br>
   * {@code graph.applyMatrix(body.matrix());} <br>
   * {@code drawBody();} <br>
   * {@code graph.pushModelView();} <br>
   * {@code graph.applyMatrix(leftArm.matrix());} <br>
   * {@code drawArm();} <br>
   * {@code graph.popModelView();} <br>
   * {@code graph.pushModelView();} <br>
   * {@code graph.applyMatrix(rightArm.matrix());} <br>
   * {@code drawArm();} <br>
   * {@code graph.popModelView();} <br>
   * {@code graph.popModelView();} <br>
   * <p>
   * Note the use of nested {@code pushModelView()} and {@code popModelView()} blocks to
   * represent the frame hierarchy: {@code leftArm} and {@code rightArm} are both
   * correctly drawn with respect to the {@code body} coordinate system.
   * <p>
   * <b>Attention:</b> In Processing this technique is inefficient because
   * {@code papplet.applyMatrix} will try to calculate the inverse of the transform.
   * <p>
   * This matrix only represents the local Frame transformation (i.e., with respect to the
   * {@link #reference()}). Use {@link #worldMatrix()} to get the full Frame
   * transformation matrix (i.e., from the world to the Frame coordinate system). These
   * two match when the {@link #reference()} is {@code null}.
   * <p>
   * The result is only valid until the next call to {@code matrix()} or
   * {@link #worldMatrix()}. Use it immediately (as above).
   */
  public Matrix matrix() {
    Matrix pM = new Matrix();

    pM = rotation().matrix();

    pM._matrix[12] = translation()._vector[0];
    pM._matrix[13] = translation()._vector[1];
    pM._matrix[14] = translation()._vector[2];

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
   * This method should be used in conjunction with {@code applyMatrix()} to modify a
   * graph modelView() matrix from a Frame:
   * <p>
   * {@code // Here the modelview matrix corresponds to the world coordinate system.} <br>
   * {@code Frame fr = new Frame(pos, Rotation(from, to));} <br>
   * {@code graph.pushModelView();} <br>
   * {@code graph.applyMatrix(worldMatrix());} <br>
   * {@code // draw object in the fr coordinate system.} <br>
   * {@code graph.popModelView();} <br>
   * <p>
   * This matrix represents the global Frame transformation: the entire
   * {@link #reference()} hierarchy is taken into account to define the Frame
   * transformation from the world coordinate system. Use {@link #matrix()} to get the
   * local Frame transformation matrix (i.e. defined with respect to the
   * {@link #reference()}). These two match when the {@link #reference()} is
   * {@code null}.
   * <p>
   * <b>Attention:</b> The result is only valid until the next call to {@link #matrix()}
   * or {@code worldMatrix()}. Use it immediately (as above).
   */
  public Matrix worldMatrix() {
    if (reference() != null)
      return new Frame(position(), orientation(), magnitude()).matrix();
    else
      return matrix();
  }

  /**
   * Convenience function that simply calls {@code fromMatrix(pM, 1))}.
   *
   * @see #fromMatrix(Matrix, float)
   */
  public void fromMatrix(Matrix pM) {
    fromMatrix(pM, 1);
  }

  /**
   * Sets the Frame from a Matrix representation: rotation in the upper left 3x3 matrix and
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
   * {@link #coordinatesOf(Vector)} and {@link #transformOf(Vector)}.
   */
  public void fromMatrix(Matrix matrix, float scaling) {
    if (matrix._matrix[15] == 0) {
      System.out.println("Doing nothing: pM.mat[15] should be non-zero!");
      return;
    }

    translation()._vector[0] = matrix._matrix[12] / matrix._matrix[15];
    translation()._vector[1] = matrix._matrix[13] / matrix._matrix[15];
    translation()._vector[2] = matrix._matrix[14] / matrix._matrix[15];

    float[][] r = new float[3][3];

    r[0][0] = matrix._matrix[0] / matrix._matrix[15];
    r[0][1] = matrix._matrix[4] / matrix._matrix[15];
    r[0][2] = matrix._matrix[8] / matrix._matrix[15];
    r[1][0] = matrix._matrix[1] / matrix._matrix[15];
    r[1][1] = matrix._matrix[5] / matrix._matrix[15];
    r[1][2] = matrix._matrix[9] / matrix._matrix[15];
    r[2][0] = matrix._matrix[2] / matrix._matrix[15];
    r[2][1] = matrix._matrix[6] / matrix._matrix[15];
    r[2][2] = matrix._matrix[10] / matrix._matrix[15];

    setScaling(scaling);// calls _modified() :P

    if (scaling() != 1) {
      r[0][0] = r[0][0] / scaling();
      r[1][0] = r[1][0] / scaling();
      r[2][0] = r[2][0] / scaling();

      r[0][1] = r[0][1] / scaling();
      r[1][1] = r[1][1] / scaling();
      r[2][1] = r[2][1] / scaling();

      r[0][2] = r[0][2] / scaling();
      r[1][2] = r[1][2] / scaling();
      r[2][2] = r[2][2] / scaling();
    }

    Vector x = new Vector(r[0][0], r[1][0], r[2][0]);
    Vector y = new Vector(r[0][1], r[1][1], r[2][1]);
    Vector z = new Vector(r[0][2], r[1][2], r[2][2]);

    rotation().fromRotatedBasis(x, y, z);
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
  public void setWorldMatrix(Frame other) {
    if (other == null)
      return;
    setPosition(other.position());
    setOrientation(other.orientation());
    setMagnitude(other.magnitude());
  }

  /**
   * Sets {@link #translation()}, {@link #rotation()} and {@link #scaling()} values from
   * those of {@code otherFrame}.
   *
   * @see #setWorldMatrix(Frame)
   */
  public void setMatrix(Frame other) {
    if (other == null)
      return;
    setTranslation(other.translation());
    setRotation(other.rotation());
    setScaling(other.scaling());
  }

  /**
   * Returns a Frame representing the inverse of the Frame space transformation.
   * <p>
   * The the new Frame {@link #rotation()} is the
   * {@link Quaternion#inverse()} of the original rotation. Its
   * {@link #translation()} is the negated inverse rotated image of the original
   * translation. Its {@link #scaling()} is 1 / original scaling.
   * <p>
   * If a Frame is considered as a space rigid transformation, i.e., translation and
   * rotation, but no scaling (scaling=1), the inverse() Frame performs the inverse
   * transformation.
   * <p>
   * Only the local Frame transformation (i.e., defined with respect to the
   * {@link #reference()}) is inverted. Use {@link #worldInverse()} for a global
   * inverse.
   * <p>
   * The resulting Frame has the same {@link #reference()} as the Frame and a
   * {@code null} {@link #constraint()}.
   */
  public Frame inverse() {
    Frame fr = new Frame(Vector.multiply(rotation().inverseRotate(translation()), -1), rotation().inverse(), 1 / scaling());
    fr.setReference(reference());
    return fr;
  }

  /**
   * Returns the {@link #inverse()} of the Frame world transformation.
   * <p>
   * The {@link #orientation()} of the new Frame is the
   * {@link Quaternion#inverse()} of the original orientation. Its
   * {@link #position()} is the negated and inverse rotated image of the original
   * position. The {@link #magnitude()} is the the original magnitude multiplicative
   * inverse.
   * <p>
   * The result Frame has a {@code null} {@link #reference()} and a {@code null}
   * {@link #constraint()}.
   * <p>
   * Use {@link #inverse()} for a local (i.e., with respect to {@link #reference()})
   * transformation inverse.
   */
  public Frame worldInverse() {
    return (new Frame(Vector.multiply(orientation().inverseRotate(position()), -1), orientation().inverse(),
        1 / magnitude()));
  }

  // POINT CONVERSION

  /**
   * Returns the Frame coordinates of the point whose position in the {@code from}
   * coordinate system is {@code src} (converts from {@code from} to Frame).
   * <p>
   * {@link #coordinatesOfIn(Vector, Frame)} performs the inverse transformation.
   */
  public Vector coordinatesOfFrom(Vector src, Frame from) {
    if (this == from)
      return src;
    else if (reference() != null)
      return localCoordinatesOf(reference().coordinatesOfFrom(src, from));
    else
      return localCoordinatesOf(from.inverseCoordinatesOf(src));
  }

  /**
   * Returns the {@code in} coordinates of the point whose position in the Frame
   * coordinate system is {@code src} (converts from Frame to {@code in}).
   * <p>
   * {@link #coordinatesOfFrom(Vector, Frame)} performs the inverse transformation.
   */
  public Vector coordinatesOfIn(Vector vector, Frame in) {
    Frame fr = this;
    Vector res = vector;
    while ((fr != null) && (fr != in)) {
      res = fr.localInverseCoordinatesOf(res);
      fr = fr.reference();
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
   * {@link #reference()} coordinate system (converts from {@link #reference()}
   * to Frame).
   * <p>
   * {@link #localInverseCoordinatesOf(Vector)} performs the inverse conversion.
   *
   * @see #localTransformOf(Vector)
   */
  public Vector localCoordinatesOf(Vector vector) {
    return Vector.divide(rotation().inverseRotate(Vector.subtract(vector, translation())), scaling());
  }

  /**
   * Returns the Frame coordinates of a point {@code src} defined in the world coordinate
   * system (converts from world to Frame).
   * <p>
   * {@link #inverseCoordinatesOf(Vector)} performs the inverse conversion.
   * {@link #transformOf(Vector)} converts vectors instead of coordinates.
   */
  public Vector coordinatesOf(Vector vector) {
    if (reference() != null)
      return localCoordinatesOf(reference().coordinatesOf(vector));
    else
      return localCoordinatesOf(vector);
  }

  // VECTOR CONVERSION

  /**
   * Returns the Frame transform of the vector whose coordinates in the {@code from}
   * coordinate system is {@code src} (converts vectors from {@code from} to Frame).
   * <p>
   * {@link #transformOfIn(Vector, Frame)} performs the inverse transformation.
   */
  public Vector transformOfFrom(Vector vector, Frame from) {
    if (this == from)
      return vector;
    else if (reference() != null)
      return localTransformOf(reference().transformOfFrom(vector, from));
    else
      return localTransformOf(from.inverseTransformOf(vector));
  }

  /**
   * Returns the {@code in} transform of the vector whose coordinates in the Frame
   * coordinate system is {@code src} (converts vectors from Frame to {@code in}).
   * <p>
   * {@link #transformOfFrom(Vector, Frame)} performs the inverse transformation.
   */
  public Vector transformOfIn(Vector vector, Frame in) {
    Frame fr = this;
    Vector res = vector;
    while ((fr != null) && (fr != in)) {
      res = fr.localInverseTransformOf(res);
      fr = fr.reference();
    }

    if (fr != in)
      // in was not found in the branch of this, res is now expressed in
      // the world coordinate system. Simply convert to in coordinate system.
      res = in.transformOf(res);

    return res;
  }

  /**
   * Returns the {@link #reference()} coordinates of a point {@code src} defined in
   * the Frame coordinate system (converts from Frame to {@link #reference()}).
   * <p>
   * {@link #localCoordinatesOf(Vector)} performs the inverse conversion.
   *
   * @see #localInverseTransformOf(Vector)
   */
  public Vector localInverseCoordinatesOf(Vector vector) {
    return Vector.add(rotation().rotate(Vector.multiply(vector, scaling())), translation());
  }

  /**
   * Returns the world coordinates of the point whose position in the Frame coordinate
   * system is {@code src} (converts from Frame to world).
   * <p>
   * {@link #coordinatesOf(Vector)} performs the inverse conversion. Use
   * {@link #inverseTransformOf(Vector)} to transform vectors instead of coordinates.
   */
  public Vector inverseCoordinatesOf(Vector vector) {
    Frame fr = this;
    Vector res = vector;
    while (fr != null) {
      res = fr.localInverseCoordinatesOf(res);
      fr = fr.reference();
    }
    return res;
  }

  /**
   * Returns the Frame transform of a vector {@code src} defined in the world coordinate
   * system (converts vectors from world to Frame).
   * <p>
   * {@link #inverseTransformOf(Vector)} performs the inverse transformation.
   * {@link #coordinatesOf(Vector)} converts coordinates instead of vectors (here only the
   * rotational part of the transformation is taken into account).
   */
  public Vector transformOf(Vector vector) {
    if (reference() != null)
      return localTransformOf(reference().transformOf(vector));
    else
      return localTransformOf(vector);
  }

  /**
   * Returns the world transform of the vector whose coordinates in the Frame coordinate
   * system is {@code src} (converts vectors from Frame to world).
   * <p>
   * {@link #transformOf(Vector)} performs the inverse transformation. Use
   * {@link #inverseCoordinatesOf(Vector)} to transform coordinates instead of vectors.
   */
  public Vector inverseTransformOf(Vector vector) {
    Frame fr = this;
    Vector res = vector;
    while (fr != null) {
      res = fr.localInverseTransformOf(res);
      fr = fr.reference();
    }
    return res;
  }

  /**
   * Returns the Frame transform of a vector {@code src} defined in the
   * {@link #reference()} coordinate system (converts vectors from
   * {@link #reference()} to Frame).
   * <p>
   * {@link #localInverseTransformOf(Vector)} performs the inverse transformation.
   *
   * @see #localCoordinatesOf(Vector)
   */
  public Vector localTransformOf(Vector vector) {
    return Vector.divide(rotation().inverseRotate(vector), scaling());
  }

  /**
   * Returns the {@link #reference()} transform of a vector {@code src} defined in
   * the Frame coordinate system (converts vectors from Frame to {@link #reference()}
   * ).
   * <p>
   * {@link #localTransformOf(Vector)} performs the inverse transformation.
   *
   * @see #localInverseCoordinatesOf(Vector)
   */
  public Vector localInverseTransformOf(Vector vector) {
    return rotation().rotate(Vector.multiply(vector, scaling()));
  }
}
