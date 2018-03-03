/****************************************************************************************
 * framesjs
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights refserved. Library that eases the creation of interactive
 * handling to a raster or ray-tracing renderer. Released under the terms of the GNU
 * Public License v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.primitives;

import frames.primitives.constraint.Constraint;
import frames.timing.TimingHandler;

/**
 * A frame is a 2D or 3D coordinate system, represented by a {@link #position()}, an
 * {@link #orientation()} and {@link #magnitude()}. The order of these transformations is
 * important: the frame is first translated, then rotated around the new translated origin
 * and then scaled. This class API aims to conform that of the great
 * <a href="http://libqglviewer.com/refManual/classqglviewer_1_1Frame.html">libQGLViewer
 * Frame</a>, but it adds {@link #magnitude()} to it.
 * <h2>Geometry transformations</h2>
 * A frame is useful to define the position, orientation and magnitude of an object, using
 * its {@link #matrix()} method, as shown below:
 * <p>
 * {@code // Builds a frame at position (0.5,0,0) and oriented such that its Y axis is
 * along the (1,1,1)} direction<br>
 * {@code Frame frame = new Frame(new Vector(0.5,0,0), new Quaternion(new Vector(0,1,0),
 * new Vector(1,1,1)));} <br>
 * {@code graph.pushModelView();} <br>
 * {@code graph.applyModelView(frame.matrix());} <br>
 * {@code // Draw your object here, in the local frame coordinate system.} <br>
 * {@code graph.popModelView();} <br>
 * <p>
 * Many functions are provided to transform a point from one frame to another, see
 * {@link #coordinatesOf(Vector)}, {@link #inverseCoordinatesOf(Vector)},
 * {@link #coordinatesOfIn(Vector, Frame)}, {@link #coordinatesOfFrom(Vector, Frame)}...
 * <p>
 * You may also want to transform a vector (such as a normal), which corresponds to
 * applying only the rotational part of the frame transformation: see
 * {@link #transformOf(Vector)} and {@link #inverseTransformOf(Vector)}.
 * <p>
 * The {@link #translation()}, {@link #rotation()} and uniform positive {@link #scaling()}
 * that are encapsulated in a frame can also be used to represent an angle preserving
 * transformation of space. Such a transformation can also be interpreted as a change of
 * coordinate system, and the coordinate system conversion functions actually allow you to
 * use a frame as an angle preserving transformation. Use
 * {@link #inverseCoordinatesOf(Vector)} (resp. {@link #coordinatesOf(Vector)}) to apply
 * the transformation (resp. its inverse). Note the inversion.
 * <h2>Hierarchy of frames</h2>
 * The frame position, orientation and magnitude are actually defined with respect to
 * a {@link #reference()} frame. The default {@link #reference()} is the world
 * coordinate system (represented by a {@code null} {@link #reference()}). If you
 * {@link #setReference(Frame)} to a different frame, you must then differentiate:
 * <p>
 * <ul>
 * <li>The <b>local</b> {@link #translation()}, {@link #rotation()} and {@link #scaling()},
 * defined with respect to the {@link #reference()}.</li>
 * <li>the <b>global</b> {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}, always defined with respect to the world coordinate system.</li>
 * </ul>
 * <p>
 * A frame is actually defined by its {@link #translation()} with respect to its
 * {@link #reference()}, then by {@link #rotation()} of the coordinate system around
 * the new translated origin and then by a uniform positive {@link #scaling()} along its
 * rotated axes.
 * <p>
 * This terminology for <b>local</b> ({@link #translation()}, {@link #rotation()} and
 * {@link #scaling()}) and <b>global</b> ({@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}) definitions is used in all the methods' names and should be
 * enough to prevent ambiguities. These notions are obviously identical when the
 * {@link #reference()} is {@code null}, i.e., when the frame is defined in the world
 * coordinate system (the one you are left with after calling a graph preDraw() method).
 * <p>
 * Frames can hence easily be organized in a tree hierarchy, which root is the world
 * coordinate system. A loop in the hierarchy would result in an inconsistent (multiple)
 * frame definition. Therefore {@link #settingAsReferenceWillCreateALoop(Frame)}
 * checks this and prevents {@link #reference()} from creating such a loop.
 * <p>
 * This frame hierarchy is used in methods like {@link #coordinatesOfIn(Vector, Frame)},
 * {@link #coordinatesOfFrom(Vector, Frame)} ... which allow coordinates (or vector)
 * conversions from a frame to any other one (including the world coordinate system).
 * <h2>Constraints</h2>
 * One interesting feature of a frame is that its displacements can be constrained. When a
 * {@link frames.primitives.constraint.Constraint} is attached to a frame, it filters
 * the input of {@link #translate(Vector)} and {@link #rotate(Quaternion)}, and only the
 * resulting filtered motion is applied to the frame. The default {@link #constraint()}
 * is {@code null} resulting in no filtering. Use {@link #setConstraint(Constraint)} to
 * attach a constraint to a frame.
 * <p>
 * Classical constraints are provided for convenience (see
 * {@link frames.primitives.constraint.LocalConstraint},
 * {@link frames.primitives.constraint.WorldConstraint} and
 * {@link frames.primitives.constraint.EyeConstraint}) and new constraints can very
 * easily be implemented.
 */
public class Frame {
  /**
   * Returns whether or not this frame matches other taking into account the {@link #translation()},
   * {@link #rotation()} and {@link #scaling()} frame parameters, but not its {@link #reference()}.
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
   * Same as {@code this(null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame() {
    this(null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(null, new Vector(), rotation, scaling)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Quaternion rotation, float scaling) {
    this(null, new Vector(), rotation, scaling);
  }

  /**
   * Same as {@code this(translation, rotation, 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Vector translation, Quaternion rotation) {
    this(translation, rotation, 1);
  }

  /**
   * Same as {@code this(null, translation, rotation, scaling)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Vector translation, Quaternion rotation, float scaling) {
    this(null, translation, rotation, scaling);
  }

  /**
   * Same as {@code this(reference, translation, rotation, 1)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Vector translation, Quaternion rotation) {
    this(reference, translation, rotation, 1);
  }

  /**
   * Same as {@code this(reference, new Vector(), rotation, 1)}.
   *
   * @see #Frame(Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Quaternion rotation, float scaling) {
    this(reference, new Vector(), rotation, 1);
  }

  /**
   * Creates a frame with {@code reference} as {@link #reference()}, and {@code translation},
   * {@code rotation} and {@code scaling} as the frame {@link #translation()},
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

  /**
   * Randomized this frame. The frame is randomly re-positioned inside the ball
   * defined by {@code center} and {@code radius} (see {@link Vector#random()}). The
   * {@link #orientation()} is randomized by {@link Quaternion#randomize()}. The new
   * magnitude is a random in oldMagnitude * [0,5...2].
   *
   * @see #random(Vector, float)
   */
  public void randomize(Vector center, float radius) {
    Vector displacement = Vector.random();
    displacement.setMagnitude(radius);
    setPosition(Vector.add(center, displacement));
    setOrientation(Quaternion.random());
    float lower = 0.5f;
    float upper = 2;
    setMagnitude(magnitude() * ((float) Math.random() * (upper - lower)) + lower);
  }

  /**
   * Returns a random frame. The frame is randomly positioned inside the ball defined
   * by {@code center} and {@code radius} (see {@link Vector#random()}). The
   * {@link #orientation()} is set by {@link Quaternion#random()}. The magnitude
   * is a random in [0,5...2].
   *
   * @see #randomize(Vector, float)
   */
  public static Frame random(Vector center, float radius) {
    Frame frame = new Frame();
    Vector displacement = Vector.random();
    displacement.setMagnitude(radius);
    frame.setPosition(Vector.add(center, displacement));
    frame.setOrientation(Quaternion.random());
    float lower = 0.5f;
    float upper = 2;
    frame.setMagnitude(((float) Math.random() * (upper - lower)) + lower);
    return frame;
  }

  /**
   * Returns a deep copy of this frame.
   */
  public Frame get() {
    return new Frame(this);
  }

  /**
   * Returns a new frame defined in the world coordinate system, with the same {@link #position()},
   * {@link #orientation()} and {@link #magnitude()} as this frame.
   *
   * @see #reference()
   */
  public Frame detach() {
    Frame frame = new Frame();
    frame.setWorldMatrix(this);
    return frame;
  }

  // MODIFIED

  /**
   * Internal use. Automatically call by all methods which change the frame state.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
  }

  /**
   * @return the last frame the this obect was updated.
   */
  public long lastUpdate() {
    return _lastUpdate;
  }

  // REFERENCE_FRAME

  /**
   * Returns the reference frame, in which this frame is defined.
   * <p>
   * The frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are
   * defined with respect to the {@link #reference()} coordinate system. A
   * {@code null} reference frame (default value) means that the frame is defined in the
   * world coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to
   * recursively convert values along the reference frame chain and to get values
   * expressed in the world coordinate system. The values match when the reference frame
   * is {@code null}.
   * <p>
   * Use {@link #setReference(Frame)} to set this value and create a frame hierarchy.
   * Convenient functions allow you to convert coordinates from one frame to another: see
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
   * Sets the {@link #reference()} of the frame.
   * <p>
   * The frame {@link #translation()}, {@link #rotation()} and {@link #scaling()} are then
   * defined in the {@link #reference()} coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to express
   * the frame global transformation in the world coordinate system.
   * <p>
   * Using this method, you can create a hierarchy of frames. This hierarchy needs to be a
   * tree, which root is the world coordinate system (i.e., {@code null}
   * {@link #reference()}). No action is performed if setting {@code reference} as the
   * {@link #reference()} would create a loop in the hierarchy.
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
   * Returns {@code true} if setting {@code frame} as the frame's
   * {@link #reference()} would create a loop in the frame hierarchy.
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
   * Returns the current {@link frames.primitives.constraint.Constraint} applied to the
   * frame.
   * <p>
   * A {@code null} value (default) means that no constraint is used to filter the frame
   * translation and rotation.
   * <p>
   * See the Constraint class documentation for details.
   */
  public Constraint constraint() {
    return _constraint;
  }

  /**
   * Sets the {@link #constraint()} attached to the frame.
   * <p>
   * A {@code null} value means set no constraint (also reset it if there was one).
   */
  public void setConstraint(Constraint constraint) {
    _constraint = constraint;
  }

  // TRANSLATION

  /**
   * Returns the frame translation, defined with respect to the {@link #reference()}.
   * <p>
   * Use {@link #position()} to get the result in world coordinates. These two values are
   * identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setTranslation(Vector)
   */
  public Vector translation() {
    return _translation;
  }

  /**
   * Sets the {@link #translation()} of the frame, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   * <p>
   * Use {@link #setPosition(Vector)} to define the world coordinates {@link #position()}.
   *
   * @see #setConstraint(Constraint)
   */
  public void setTranslation(Vector translation) {
    if (constraint() == null)
      _translation = translation;
    else
      translation().add(constraint().constrainTranslation(Vector.subtract(translation, this.translation()), this));
    _modified();
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
   * Translates the frame according to {@code vector}, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * If there's a {@link #constraint()} it is satisfied. Hence the translation actually
   * applied to the frame may differ from {@code vector} (since it can be filtered by the
   * {@link #constraint()}).
   *
   * @see #rotate(Quaternion)
   * @see #scale(float)
   */
  public void translate(Vector vector) {
    translation().add(constraint() != null ? constraint().constrainTranslation(vector, this) : vector);
    _modified();
  }

  // POSITION

  /**
   * Returns the frame position defined in the world coordinate system.
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
   * Sets the frame {@link #position()}, defined in the world coordinate system.
   * <p>
   * Use {@link #setTranslation(Vector)} to define the local frame translation (with respect
   * to the {@link #reference()}).
   * <p>
   * Note that the potential {@link #constraint()} of the frame is taken into account, i.e.,
   * to bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   */
  public void setPosition(Vector position) {
    setTranslation(reference() != null ? reference().coordinatesOf(position) : position);
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

  // ROTATION

  /**
   * Returns the frame rotation, defined with respect to the {@link #reference()}
   * (i.e, the current Quaternion orientation).
   * <p>
   * Use {@link #orientation()} to get the result in world coordinates. These two values
   * are identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setRotation(Quaternion)
   */
  public Quaternion rotation() {
    return _rotation;
  }

  /**
   * Same as {@link #setRotation(Quaternion)} but with {@code float} Quaternion parameters.
   */
  public void setRotation(float x, float y, float z, float w) {
    setRotation(new Quaternion(x, y, z, w));
  }

  /**
   * Set the current rotation. See the different {@link Quaternion} constructors.
   * <p>
   * Sets the frame {@link #rotation()}, locally defined with respect to the
   * {@link #reference()}. Use {@link #setOrientation(Quaternion)} to define the
   * world coordinates {@link #orientation()}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   * @see #rotation()
   * @see #setTranslation(Vector)
   */
  public void setRotation(Quaternion rotation) {
    if (constraint() == null)
      _rotation = rotation;
    else {
      rotation().compose(constraint().constrainRotation(Quaternion.compose(rotation().inverse(), rotation), this));
      rotation().normalize(); // Prevents numerical drift
    }
    _modified();
  }

  /**
   * Rotates the frame by {@code quaternion} (defined in the frame coordinate system):
   * {@code rotation().compose(quaternion)}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   * @see #translate(Vector)
   */
  public void rotate(Quaternion quaternion) {
    rotation().compose(constraint() != null ? constraint().constrainRotation(quaternion, this) : quaternion);
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
   * Makes the frame {@link #rotate(Quaternion)} by {@code rotation} around {@code point}.
   * The {@code point} is defined in the world coordinate system while the {@code rotation}
   * axis is defined in the frame coordinate system.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
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

  /**
   * Applies a {@code rotation} (to this frame) around the {@code frame} param.
   */
  public void rotateAroundFrame(Quaternion rotation, Frame frame) {
    Vector euler = rotation.eulerAngles();
    rotateAroundFrame(euler.x(), euler.y(), euler.z(), frame);
  }

  /**
   * Applies the rotation (to this frame) defined by the Euler angles
   * around the {@code frame} param.
   */
  public void rotateAroundFrame(float roll, float pitch, float yaw, Frame frame) {
    if (frame != null) {
      Frame axis = frame.detach();
      Frame copy = detach();
      copy.setReference(axis);
      axis.rotate(new Quaternion(roll, pitch, yaw));
      setWorldMatrix(copy);
    }
  }

  // ORIENTATION

  /**
   * Returns the orientation of the frame, defined in the world coordinate system.
   *
   * @see #position()
   * @see #magnitude()
   * @see #setOrientation(Quaternion)
   * @see #rotation()
   */
  public Quaternion orientation() {
    Quaternion quaternion = rotation().get();
    Frame reference = reference();
    while (reference != null) {
      quaternion = Quaternion.compose(reference.rotation(), quaternion);
      reference = reference.reference();
    }
    return quaternion;
  }

  /**
   * Sets the {@link #orientation()} of the frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setRotation(Quaternion)} to define the local frame rotation (with respect
   * to the {@link #reference()}).
   * <p>
   * Note that the potential {@link #constraint()} of the frame is taken into account, i.e.,
   * to bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
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

  // SCALING

  /**
   * Returns the frame scaling, defined with respect to the {@link #reference()}.
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
   * Use {@link #setMagnitude(float)} to define the world coordinates {@link #magnitude()}.
   */
  public void setScaling(float scaling) {
    if (scaling > 0) {
      _scaling = scaling;
      _modified();
    } else
      System.out.println("Warning. Scaling should be positive. Nothing done");
  }

  /**
   * Scales the frame according to {@code scaling}, locally defined with respect to the
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
   * Returns the magnitude of the frame, defined in the world coordinate system.
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
   * Sets the {@link #magnitude()} of the frame, defined in the world coordinate system.
   * <p>
   * Use {@link #setScaling(float)} to define the local frame scaling (with respect to the
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
   * Aligns the frame with {@code frame}, so that two of their axis are parallel.
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
   * Translates the frame so that its {@link #position()} lies on the line defined by
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
   * Returns the local transformation matrix represented by the frame.
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
   * This matrix only represents the local frame transformation (i.e., with respect to the
   * {@link #reference()}). Use {@link #worldMatrix()} to get the full Frame
   * transformation matrix (i.e., from the world to the Frame coordinate system). These
   * two match when the {@link #reference()} is {@code null}.
   * <p>
   * <b>Attention:</b> In Processing this technique is inefficient because
   * {@code papplet.applyMatrix} will try to calculate the inverse of the transform.
   * Use {@link frames.core.Graph#applyTransformation(Frame)} instead.
   *
   * @see #setMatrix(Frame)
   * @see #worldMatrix()
   * @see #view()
   */
  public Matrix matrix() {
    Matrix matrix = rotation().matrix();

    matrix._matrix[12] = translation()._vector[0];
    matrix._matrix[13] = translation()._vector[1];
    matrix._matrix[14] = translation()._vector[2];

    if (scaling() != 1) {
      matrix.setM00(matrix.m00() * scaling());
      matrix.setM10(matrix.m10() * scaling());
      matrix.setM20(matrix.m20() * scaling());

      matrix.setM01(matrix.m01() * scaling());
      matrix.setM11(matrix.m11() * scaling());
      matrix.setM21(matrix.m21() * scaling());

      matrix.setM02(matrix.m02() * scaling());
      matrix.setM12(matrix.m12() * scaling());
      matrix.setM22(matrix.m22() * scaling());
    }

    return matrix;
  }

  /**
   * Returns the global transformation matrix represented by the frame.
   * <p>
   * This method should be used in conjunction with {@code applyMatrix()} to modify a
   * graph modelView() matrix from a frame:
   * <p>
   * {@code // Here the modelview matrix corresponds to the world coordinate system.} <br>
   * {@code Frame frame = new Frame(translation, new Rotation(from, to));} <br>
   * {@code graph.pushModelView();} <br>
   * {@code graph.applyModelView(frame.worldMatrix());} <br>
   * {@code // draw object in the frame coordinate system.} <br>
   * {@code graph.popModelView();} <br>
   * <p>
   * This matrix represents the global frame transformation: the entire
   * {@link #reference()} hierarchy is taken into account to define the frame
   * transformation from the world coordinate system. Use {@link #matrix()} to get the
   * local frame transformation matrix (i.e. defined with respect to the
   * {@link #reference()}). These two match when the {@link #reference()} is
   * {@code null}.
   *
   * @see #setWorldMatrix(Frame)
   * @see #matrix()
   * @see #view()
   */
  public Matrix worldMatrix() {
    if (reference() != null)
      return new Frame(position(), orientation(), magnitude()).matrix();
    else
      return matrix();
  }

  /**
   * Same as {@link #worldMatrix()}, but the view matrix is computing with the frame magnitude
   * set to 1, i.e., returns the matrix associated with the frame position and orientation.
   * To be used when the frame represents an eye.
   * <p>
   * The view matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using a projection matrix.
   *
   * @see #matrix()
   * @see #worldMatrix()
   * @see #setMatrix(Frame)
   * @see #setWorldMatrix(Frame)
   */
  public Matrix view() {
    Matrix view = new Matrix();

    Quaternion q = orientation();

    float q00 = 2.0f * q._quaternion[0] * q._quaternion[0];
    float q11 = 2.0f * q._quaternion[1] * q._quaternion[1];
    float q22 = 2.0f * q._quaternion[2] * q._quaternion[2];

    float q01 = 2.0f * q._quaternion[0] * q._quaternion[1];
    float q02 = 2.0f * q._quaternion[0] * q._quaternion[2];
    float q03 = 2.0f * q._quaternion[0] * q._quaternion[3];

    float q12 = 2.0f * q._quaternion[1] * q._quaternion[2];
    float q13 = 2.0f * q._quaternion[1] * q._quaternion[3];
    float q23 = 2.0f * q._quaternion[2] * q._quaternion[3];

    view._matrix[0] = 1.0f - q11 - q22;
    view._matrix[1] = q01 - q23;
    view._matrix[2] = q02 + q13;
    view._matrix[3] = 0.0f;

    view._matrix[4] = q01 + q23;
    view._matrix[5] = 1.0f - q22 - q00;
    view._matrix[6] = q12 - q03;
    view._matrix[7] = 0.0f;

    view._matrix[8] = q02 - q13;
    view._matrix[9] = q12 + q03;
    view._matrix[10] = 1.0f - q11 - q00;
    view._matrix[11] = 0.0f;

    Vector t = q.inverseRotate(position());

    view._matrix[12] = -t._vector[0];
    view._matrix[13] = -t._vector[1];
    view._matrix[14] = -t._vector[2];
    view._matrix[15] = 1.0f;

    return view;
  }

  /**
   * Convenience function that simply calls {@code fromMatrix(matrix, 1))}.
   *
   * @see #fromMatrix(Matrix, float)
   */
  public void fromMatrix(Matrix matrix) {
    fromMatrix(matrix, 1);
  }

  /**
   * Sets the frame from a Matrix representation: rotation in the upper left 3x3 matrix and
   * translation on the last column. Scaling is defined separately in {@code scaling}.
   * <p>
   * Hence, if your openGL code fragment looks like:
   * <p>
   * {@code float [] m = new float [16]; m[0]=...;} <br>
   * {@code gl.glMultMatrixf(m);} <br>
   * <p>
   * It is equivalent to write:
   * <p>
   * {@code Frame frame = new Frame();} <br>
   * {@code frame.fromMatrix(m);} <br>
   * {@code graph.applyModelView(frame.matrix());} <br>
   * <p>
   * Using this conversion, you can benefit from the powerful frame transformation methods
   * to translate points and vectors to and from the frame coordinate system to any other
   * frame coordinate system (including the world coordinate system). See
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
   * those of the {@code other} frame.
   * <p>
   * After calling {@code setWorldMatrix(other)} a call to {@code this.matches(other)} should
   * return {@code true}.
   *
   * @see #worldMatrix()
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
   * those of the {@code other} frame.
   *
   * @see #matrix()
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
   * Returns a frame representing the inverse of this frame space transformation.
   * <p>
   * The the new frame {@link #rotation()} is the
   * {@link Quaternion#inverse()} of the original rotation. Its
   * {@link #translation()} is the negated inverse rotated image of the original
   * translation. Its {@link #scaling()} is 1 / original scaling.
   * <p>
   * If a frame is considered as a space rigid transformation, i.e., translation and
   * rotation, but no scaling (scaling=1), the inverse() frame performs the inverse
   * transformation.
   * <p>
   * Only the local frame transformation (i.e., defined with respect to the
   * {@link #reference()}) is inverted. Use {@link #worldInverse()} for a global
   * inverse.
   * <p>
   * The resulting frame has the same {@link #reference()} as the this frame and a
   * {@code null} {@link #constraint()}.
   *
   * @see #worldInverse()
   */
  public Frame inverse() {
    Frame frame = new Frame(Vector.multiply(rotation().inverseRotate(translation()), -1), rotation().inverse(), 1 / scaling());
    frame.setReference(reference());
    return frame;
  }

  /**
   * Returns the {@link #inverse()} of the frame world transformation.
   * <p>
   * The {@link #orientation()} of the new frame is the
   * {@link Quaternion#inverse()} of the original orientation. Its
   * {@link #position()} is the negated and inverse rotated image of the original
   * position. The {@link #magnitude()} is the the original magnitude multiplicative
   * inverse.
   * <p>
   * The result frame has a {@code null} {@link #reference()} and a {@code null}
   * {@link #constraint()}.
   * <p>
   * Use {@link #inverse()} for a local (i.e., with respect to {@link #reference()})
   * transformation inverse.
   *
   * @see #inverse()
   */
  public Frame worldInverse() {
    return (new Frame(Vector.multiply(orientation().inverseRotate(position()), -1), orientation().inverse(),
        1 / magnitude()));
  }

  // POINT CONVERSION

  /**
   * Returns the frame coordinates of the point whose position in the {@code from}
   * coordinate system is {@code src} (converts from {@code from} to this frame).
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
   * Returns the {@code in} coordinates of the point whose position in the frame
   * coordinate system is {@code src} (converts from this frame to {@code in}).
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
   * Returns the frame coordinates of a point {@code src} defined in the
   * {@link #reference()} coordinate system (converts from {@link #reference()}
   * to this frame).
   * <p>
   * {@link #localInverseCoordinatesOf(Vector)} performs the inverse conversion.
   *
   * @see #localTransformOf(Vector)
   */
  public Vector localCoordinatesOf(Vector vector) {
    return Vector.divide(rotation().inverseRotate(Vector.subtract(vector, translation())), scaling());
  }

  /**
   * Returns the frame coordinates of a point {@code src} defined in the world coordinate
   * system (converts from world to this frame).
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
   * Returns the frame transform of the vector whose coordinates in the {@code from}
   * coordinate system is {@code src} (converts vectors from {@code from} to this frame).
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
   * Returns the {@code in} transform of the vector whose coordinates in the frame
   * coordinate system is {@code src} (converts vectors from this frame to {@code in}).
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
   * the frame coordinate system (converts from this frame to {@link #reference()}).
   * <p>
   * {@link #localCoordinatesOf(Vector)} performs the inverse conversion.
   *
   * @see #localInverseTransformOf(Vector)
   */
  public Vector localInverseCoordinatesOf(Vector vector) {
    return Vector.add(rotation().rotate(Vector.multiply(vector, scaling())), translation());
  }

  /**
   * Returns the world coordinates of the point whose position in the frame coordinate
   * system is {@code src} (converts from this frame to world).
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
   * Returns the frame transform of a vector {@code src} defined in the world coordinate
   * system (converts vectors from world to this frame).
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
   * Returns the world transform of the vector whose coordinates in the fFrame coordinate
   * system is {@code src} (converts vectors from this frame to world).
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
   * Returns the frame transform of a vector {@code src} defined in the
   * {@link #reference()} coordinate system (converts vectors from
   * {@link #reference()} to this frame).
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
   * the Frame coordinate system (converts vectors from this frame to {@link #reference()}).
   * <p>
   * {@link #localTransformOf(Vector)} performs the inverse transformation.
   *
   * @see #localInverseCoordinatesOf(Vector)
   */
  public Vector localInverseTransformOf(Vector vector) {
    return rotation().rotate(Vector.multiply(vector, scaling()));
  }
}
