/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core;

import frames.core.constraint.Constraint;
import frames.primitives.Matrix;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.timing.TimingHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A frame is a 2D or 3D coordinate system, represented by a {@link #position()}, an
 * {@link #orientation()} and {@link #magnitude()}. The order of these transformations is
 * important: the frame is first translated, then rotated around the new translated origin
 * and then scaled. This class API partially conforms that of the great
 * <a href="http://libqglviewer.com/refManual/classqglviewer_1_1Frame.html">libQGLViewer
 * Frame</a>.
 * <h2>Hierarchy of frames</h2>
 * The frame position, orientation and magnitude are actually defined with respect to
 * a {@link #reference()} frame. The default {@link #reference()} is the world
 * coordinate system (represented by a {@code null} {@link #reference()}). If you
 * {@link #setReference(Frame)} to a different frame, you must then differentiate:
 * <ul>
 * <li>The <b>local</b> {@link #translation()}, {@link #rotation()} and {@link #scaling()},
 * defined with respect to the {@link #reference()} which represents an angle preserving
 * transformation of space.</li>
 * <li>The <b>global</b> {@link #position()}, {@link #orientation()} and
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
 * <h2>Geometry transformations</h2>
 * A frame is useful to define the position, orientation and magnitude of an arbitrary object
 * which may represent a point-of-view.
 * <p>
 * Use {@link #matrix()} to access the frame coordinate system, as when drawing an object
 * locally:
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
 * Use {@link #view()} when rendering the scene from the frame point-of-view. Note this
 * this method is used by the graph when a frame is set as its eye.
 * <p>
 * To transform a point from one frame to another use {@link #location(Vector, Frame)} and
 * {@link #worldLocation(Vector)}. To instead transform a vector (such as a normal) use
 * {@link #displacement(Vector, Frame)} and {@link #worldDisplacement(Vector)}.
 * <h2>Hierarchical traversals</h2>
 * Hierarchical traversals of the frame hierarchy which automatically apply the local
 * frame transformations described above may be achieved with {@link Graph#traverse()}.
 * Automatic traversals require overriding {@link #visit()} and to instantiate a frame
 * attached to a graph which is referred to as attached frame (see {@link #isAttached(Graph)}
 * and {@link #isDetached()}).
 * <p>
 * To instantiate an attached frame use the frame constructors that take a {@code graph}
 * parameter or a (reference) frame which in turn is attached to a graph. Once instantiated,
 * a frame cannot be attached nor detached, but a copy of it can (see {@link #attach(Graph)}
 * and {@link #detach()}).
 * <h2>Constraints</h2>
 * One interesting feature of a frame is that its displacements can be constrained. When a
 * {@link frames.core.constraint.Constraint} is attached to a frame, it filters
 * the input of {@link #translate(Vector)} and {@link #rotate(Quaternion)}, and only the
 * resulting filtered motion is applied to the frame. The default {@link #constraint()}
 * is {@code null} resulting in no filtering. Use {@link #setConstraint(Constraint)} to
 * attach a constraint to a frame.
 * <p>
 * Classical constraints are provided for convenience (see
 * {@link frames.core.constraint.LocalConstraint},
 * {@link frames.core.constraint.WorldConstraint} and
 * {@link frames.core.constraint.EyeConstraint}) and new constraints can very
 * easily be implemented.
 * <h2>Syncing</h2>
 * Two frames can be synced together ({@link #sync(Frame, Frame)}), meaning that they will
 * share their global parameters (position, orientation and magnitude) taken the one
 * that has been most recently updated. Syncing can be useful to share frames
 * among different off-screen canvases.
 * <h2>Picking</h2>
 * Picking a frame is done accordingly to a {@link #precision()}. Refer to
 * {@link #setPrecision(Precision)} for details.
 * <h2>Application Control</h2>
 * Implementing an application control for the frame is a two step process:
 * <ul>
 * <li>Parse user gesture data by overriding {@link #interact(Object...)}.</li>
 * <li>Send gesture data to the frame by calling {@link Graph#defaultHIDControl(Object...)},
 * {@link Graph#control(String, Object...)} or {@link Graph#control(Frame, Object...)}.</li>
 * </ul>
 */
public class Frame {
  /**
   * Returns whether or not this frame matches other taking into account the {@link #translation()},
   * {@link #rotation()} and {@link #scaling()} frame parameters, but not its {@link #reference()}.
   *
   * @param frame frame
   */
  public boolean matches(Frame frame) {
    if (frame == null)
      frame = new Frame();
    return translation().matches(frame.translation()) && rotation().matches(frame.rotation()) && scaling() == frame.scaling();
  }

  protected Vector _translation;
  protected float _scaling;
  protected Quaternion _rotation;
  protected Frame _reference;
  protected Constraint _constraint;
  protected long _lastUpdate;

  // Tracking & Precision
  protected float _threshold;

  public enum Precision {
    FIXED, ADAPTIVE, EXACT
  }

  protected Precision _precision;

  // ID
  protected static int _counter;
  protected int _id;

  // Attached frames

  protected Graph _graph;
  protected List<Frame> _children;
  protected boolean _culled;
  protected boolean _tracking;

  /**
   * Same as {@code this(null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame() {
    this(null, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(translation, new Quaternion(), 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Vector translation) {
    this(translation, new Quaternion(), 1);
  }

  /**
   * Same as {@code this(new Vector(), rotation, 1)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(Quaternion rotation) {
    this(new Vector(), rotation, 1);
  }

  /**
   * Same as {@code this(new Vector(), new Quaternion(), scaling)}.
   *
   * @see #Frame(Vector, Quaternion, float)
   */
  public Frame(float scaling) {
    this(new Vector(), new Quaternion(), scaling);
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
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Vector translation, Quaternion rotation, float scaling) {
    this(null, null, translation, rotation, scaling);
  }

  /**
   * Same as {@code this(reference, translation, new Quaternion(), 1)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Vector translation) {
    this(null, reference, translation, new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, new Vector(), rotation, 1)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Quaternion rotation) {
    this(null, reference, new Vector(), rotation, 1);
  }

  /**
   * Same as {@code this(reference, new Vector(), new Quaternion(), scaling)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, float scaling) {
    this(null, reference, new Vector(), new Quaternion(), scaling);
  }

  /**
   * Same as {@code this(reference, translation, rotation, 1)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference, Vector translation, Quaternion rotation) {
    this(null, reference, translation, rotation, 1);
  }

  /**
   * Same as {@code this(graph, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Graph graph) {
    this(graph, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference.graph(), reference, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Frame(Graph, Frame, Vector, Quaternion, float)
   */
  public Frame(Frame reference) {
    this(reference.graph(), reference, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a frame with {@code reference} as {@link #reference()}, and {@code translation},
   * {@code rotation} and {@code scaling} as the frame {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   * <p>
   * Sets the {@link #precision()} to {@link Precision#FIXED}.
   */
  protected Frame(Graph graph, Frame reference, Vector translation, Quaternion rotation, float scaling) {
    _graph = graph;
    setReference(reference);
    setTranslation(translation);
    setRotation(rotation);
    setScaling(scaling);
    _id = ++_counter;
    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum frame instances reached. Exiting now!");
    _lastUpdate = 0;
    _precision = Precision.FIXED;
    setPrecisionThreshold(20);
    _tracking = true;

    if (graph() == null)
      return;

    // attached frames:
    _children = new ArrayList<Frame>();
    _culled = false;
  }

  /**
   * Copy constructor.
   */
  protected Frame(Graph graph, Frame frame) {
    this._graph = graph;
    this.setPosition(frame.position());
    this.setOrientation(frame.orientation());
    this.setMagnitude(frame.magnitude());
    this.setConstraint(frame.constraint());

    if ((this.isDetached() && frame.isDetached()) || !(this.isDetached() && !frame.isDetached()))
      setReference(frame.reference());

    this._id = ++_counter;
    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum frame instances reached. Exiting now!");
    _lastUpdate = frame.lastUpdate();
    this._precision = frame._precision;
    this._threshold = frame._threshold;
    this._tracking = frame._tracking;

    if (graph() == null)
      return;

    // attached frames:
    this._children = new ArrayList<Frame>();
    this._culled = frame._culled;
  }

  /**
   * Performs a deep copy of this frame into {@code graph}.
   * <p>
   * Same as {@code return new Frame(graph, this)}.
   *
   * @see #Frame(Graph, Frame)
   */
  public Frame attach(Graph graph) {
    return new Frame(graph, this);
  }

  /**
   * Performs a deep copy of this frame.
   * <p>
   * Same as {@code return attach(graph())}.
   *
   * @see #attach(Graph)
   */
  public Frame get() {
    return attach(graph());
  }

  /**
   * Returns a detached deep copy of this frame.
   * <p>
   * Same as {@code return attach(null)}.
   *
   * @see #attach(Graph)
   */
  public Frame detach() {
    return attach(null);
  }

  /**
   * Tells whether or not this frame belongs to the {@graph} hierarchy (see {@link Graph#traverse()}).
   * To test if the frame is detach from any graph hierarchy call {@code isAttached(null)}.
   * <p>
   * Note that a call to {@link #children()} never returns {@code null} if the frame is attached to
   * a graph, i.e., that graph will visit the frame during traversal.
   *
   * @see #isDetached()
   * @see Graph#traverse()
   */
  public boolean isAttached(Graph graph) {
    return _graph == graph;
  }

  /**
   * Same as {@code return isAttached(null)}.
   * <p>
   * Note that a call to {@link #children()} always returns {@code null} if the frame is detached,
   * i.e., the frame is not available for graph traversal (see {@link Graph#traverse()}).
   *
   * @see #isAttached(Graph)
   * @see Graph#traverse()
   */
  public boolean isDetached() {
    return isAttached(null);
  }

  /**
   * Sets {@link #position()}, {@link #orientation()} and {@link #magnitude()} values from
   * those of the {@code frame}. The frame {@link #graph()}, {@link #reference()} and
   * {@link #constraint()} are not affected by this call.
   * <p>
   * After calling {@code set(frame)} a call to {@code this.matches(frame)} should
   * return {@code true}.
   *
   * @see #reset()
   * @see #worldMatrix()
   */
  public void set(Frame frame) {
    if (frame == null)
      frame = new Frame();
    setPosition(frame.position());
    setOrientation(frame.orientation());
    setMagnitude(frame.magnitude());
  }

  /**
   * Sets an identity frame by resetting its {@link #translation()}, {@link #rotation()}
   * and {@link #scaling()}. The frame {@link #graph()}, {@link #reference()} and
   * {@link #constraint()} are not affected by this call. Call {@code set(null)} if you
   * want to reset the global {@link #position()}, {@link #orientation()} and
   * {@link #magnitude()} frame parameters instead.
   *
   * @see #set(Frame)
   */
  public void reset() {
    setTranslation(new Vector());
    setRotation(new Quaternion());
    setScaling(1);
  }

  // id

  /**
   * Uniquely identifies the frame. Also the color to be used for picking with a color buffer.
   * See: http://stackoverflow.com/questions/2262100/rgb-int-to-rgb-python
   */
  public int id() {
    return (255 << 24) | ((_id & 255) << 16) | (((_id >> 8) & 255) << 8) | (_id >> 16) & 255;
  }

  // MODIFIED

  /**
   * @return the last frame the this object was updated.
   */
  public long lastUpdate() {
    return _lastUpdate;
  }

  // SYNC

  /**
   * Same as {@code sync(this, other)}.
   *
   * @see #sync(Frame, Frame)
   */
  public void sync(Frame frame) {
    sync(this, frame);
  }

  /**
   * If {@code frame1} has been more recently updated than {@code frame2}, calls
   * {@code frame2.set(frame1)}, otherwise calls {@code frame1.set(frame2)}.
   * Does nothing if both objects were updated at the same time.
   * <p>
   * This method syncs only the global geometry attributes ({@link #position()},
   * {@link #orientation()} and {@link #magnitude()}) among the two frames. The
   * {@link #reference()} and {@link #constraint()} (if any) of each frame are kept
   * separately.
   *
   * @see #set(Frame)
   */
  public static void sync(Frame frame1, Frame frame2) {
    if (frame1 == null || frame2 == null)
      return;
    if (frame1.lastUpdate() == frame2.lastUpdate())
      return;
    Frame source = (frame1.lastUpdate() > frame2.lastUpdate()) ? frame1 : frame2;
    Frame target = (frame1.lastUpdate() > frame2.lastUpdate()) ? frame2 : frame1;
    target.set(source);
  }

  /**
   * Internal use. Automatically call by all methods which change the Frame state.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
    if (_children != null)
      for (Frame child : _children)
        child._modified();
  }

  // REFERENCE_FRAME

  /**
   * Returns {@code true} if {@code frame} is {@link #reference()} {@code this} frame.
   */
  public boolean isReference(Frame frame) {
    return reference() == frame;
  }

  /**
   * Returns {@code true} if {@code frame} is ancestor of {@code this} frame.
   */
  public boolean isAncestor(Frame frame) {
    if (frame == null)
      return true;
    return frame._isSuccessor(this);
  }

  /**
   * Returns {@code true} if {@code frame} is successor of {@code this} frame.
   */
  protected boolean _isSuccessor(Frame frame) {
    if (frame == this || frame == null)
      return false;
    Frame ancestor = frame.reference();
    while (ancestor != null) {
      if (ancestor == this)
        return true;
      ancestor = ancestor.reference();
    }
    return false;
  }

  /**
   * Same as {@code return successor.isAncestor(ancestor)}.
   *
   * @see #isAncestor(Frame)
   * @see #path(Frame, Frame)
   */
  public static boolean isAncestor(Frame successor, Frame ancestor) {
    return successor.isAncestor(ancestor);
  }

  /**
   * Returns an array containing a straight path of frames from {@code tail} to {@code tip}.
   * Returns an empty list if {@code tail} is not ancestor of {@code tip}.
   *
   * @see #isAncestor(Frame, Frame)
   */
  public static List<Frame> path(Frame tail, Frame tip) {
    ArrayList<Frame> list = new ArrayList<Frame>();
    if (tip.isAncestor(tail)) {
      Frame _tip = tip;
      while (_tip != tail) {
        list.add(0, _tip);
        _tip = _tip.reference();
      }
      if (tail != null)
        list.add(0, tail);
    }
    return list;
  }

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
   * Convenient functions allow you to convert coordinates and vectors from one frame to
   * another: see {@link #location(Vector, Frame)} and {@link #displacement(Vector, Frame)},
   * respectively.
   */
  public Frame reference() {
    return _reference;
  }

  /**
   * Same as {@code setReference(null)}.
   *
   * @see #setReference(Frame)
   */
  public void resetReference() {
    setReference(null);
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
  public void setReference(Frame frame) {
    if (frame == this) {
      System.out.println("A Frame cannot be a reference of itself.");
      return;
    }
    if (_isSuccessor(frame)) {
      System.out.println("A Frame descendant cannot be set as its reference.");
      return;
    }
    if (frame != null)
      if ((isDetached() && !frame.isDetached()) || !frame.isAttached(graph())) {
        System.out.println("Both frame and its reference should be detached, or attached to the same graph.");
        return;
      }
    if (isDetached()) {
      if (reference() == frame)
        return;
      _reference = frame;
    } else {
      // 1. no need to re-parent, just check this needs to be added as leadingFrame
      if (reference() == frame) {
        _restorePath(reference(), this);
        return;
      }
      // 2. else re-parenting
      // 2a. before assigning new reference frame
      if (reference() != null) // old
        reference()._removeChild(this);
      else if (graph() != null)
        graph()._removeLeadingFrame(this);
      // finally assign the reference frame
      _reference = frame;// reference() returns now the new value
      // 2b. after assigning new reference frame
      _restorePath(reference(), this);
    }
    _modified();
  }

  /**
   * Used by {@link #setReference(Frame)}.
   */
  protected void _restorePath(Frame parent, Frame child) {
    if (parent == null) {
      if (graph() != null)
        graph()._addLeadingFrame(child);
    } else {
      if (!parent._hasChild(child)) {
        parent._addChild(child);
        _restorePath(parent.reference(), parent);
      }
    }
  }

  /**
   * Used by {@link #_restorePath(Frame, Frame)}.
   */
  protected boolean _addChild(Frame frame) {
    if (frame == null)
      return false;
    if (_hasChild(frame))
      return false;
    return _children.add(frame);
  }

  /**
   * Removes the leading Frame if present. Typically used when re-parenting the Frame.
   */
  protected boolean _removeChild(Frame frame) {
    boolean result = false;
    Iterator<Frame> it = _children.iterator();
    while (it.hasNext()) {
      if (it.next() == frame) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  protected boolean _hasChild(Frame frame) {
    for (Frame child : _children)
      if (child == frame)
        return true;
    return false;
  }

  /**
   * Returns the list a child frames of this frame. Only meaningful if this frame {@link #isAttached(Graph)}
   * to a graph. Returns {@code null} if this frame {@link #isDetached()}.
   */
  public List<Frame> children() {
    return _children;
  }

  // Random

  /**
   * Macro that returns a number number between {@code lower} and {@code upper}.
   */
  protected static float _random(float lower, float upper) {
    return ((float) Math.random() * (upper - lower)) + lower;
  }

  /**
   * Same as {@code randomize(graph().center(), graph().radius(), graph().is3D())}.
   * <p>
   * Does nothing if the frame {@link #isDetached()}.
   *
   * @see #randomize(Vector, float, boolean)
   * @see Vector#randomize()
   * @see Quaternion#randomize()
   * @see #random(Graph)
   * @see #random(Vector, float, boolean)
   */
  public void randomize() {
    if (graph() != null)
      randomize(graph().center(), graph().radius(), graph().is3D());
    else
      System.out.println("randomize() is only available for attached frames, nothing done! Use randomize(center, radius, is3D) instead");
  }

  /**
   * Randomized this frame. The frame is randomly re-positioned inside the ball
   * defined by {@code center} and {@code radius}, which in 2D is a
   * circumference parallel to the x-y plane. The {@link #orientation()} is
   * randomized by {@link Quaternion#randomize()}. The new magnitude is a random
   * in old-magnitude * [0,5...2].
   *
   * @see #randomize()
   * @see Vector#randomize()
   * @see Quaternion#randomize()
   * @see #random(Graph)
   * @see #random(Vector, float, boolean)
   */
  public void randomize(Vector center, float radius, boolean is3D) {
    Vector displacement;
    Quaternion quaternion;
    if (is3D) {
      displacement = Vector.random();
      quaternion = Quaternion.random();
    } else {
      displacement = new Vector(_random(-1, 1), _random(-1, 1));
      displacement.normalize();
      quaternion = new Quaternion(new Vector(0, 0, 1), _random(0, 2 * (float) Math.PI));
    }
    displacement.setMagnitude(_random(radius * 0.1f, radius * 0.9f));
    setPosition(Vector.add(center, displacement));
    setOrientation(quaternion);
    setMagnitude(magnitude() * _random(0.5f, 2));
  }

  /**
   * Returns a random frame attached to {@code graph}. The frame is randomly positioned inside
   * the {@code graph} viewing volume which is defined by {@link Graph#center()} and {@link Graph#radius()}
   * (see {@link Vector#random()}). The {@link #orientation()} is set by {@link Quaternion#random()}. The
   * magnitude is a random in [0,5...2].
   *
   * @see #random(Vector, float, boolean)
   * @see Vector#random()
   * @see Quaternion#random()
   * @see #randomize()
   * @see #randomize(Vector, float, boolean)
   */
  public static Frame random(Graph graph) {
    Frame frame = new Frame(graph);
    frame.randomize(graph.center(), graph.radius(), graph.is3D());
    return frame;
  }

  /**
   * Returns a random detached frame. The frame is randomly positioned inside the ball defined
   * by {@code center} and {@code radius} (see {@link Vector#random()}), which in 2D is a
   * circumference parallel to the x-y plane. The {@link #orientation()} is set by
   * {@link Quaternion#random()}. The magnitude is a random in [0,5...2].
   *
   * @see #random(Graph)
   * @see Vector#random()
   * @see Quaternion#random()
   * @see #randomize()
   * @see #randomize(Vector, float, boolean)
   */
  public static Frame random(Vector center, float radius, boolean is3D) {
    Frame frame = new Frame();
    frame.randomize(center, radius, is3D);
    return frame;
  }

  // PRECISION

  /**
   * Returns the frame picking precision. See {@link #setPrecision(Precision)} for details.
   *
   * @see #setPrecision(Precision)
   * @see #setPrecisionThreshold(float)
   * @see #precisionThreshold()
   */
  public Precision precision() {
    return _precision;
  }

  /**
   * Sets the frame picking precision.
   * <p>
   * When {@link #precision()} is {@link Precision#FIXED} or {@link Precision#ADAPTIVE}
   * Picking is done by checking if the pointer lies within a squared area around the frame
   * {@link #position()} screen projection which size is defined by
   * {@link #setPrecisionThreshold(float)}.
   * <p>
   * When {@link #precision()} is {@link Precision#EXACT}, picking is done
   * in a precise manner according to the projected pixels of the visual representation
   * related to the frame. It is meant to be implemented by derived classes (providing the
   * means attach a visual representation to the frame) and requires the graph to implement
   * a back buffer.
   * <p>
   * Default implementation of this policy will behave like {@link Precision#FIXED}.
   *
   * @see #precision()
   * @see #setPrecisionThreshold(float)
   * @see #precisionThreshold()
   */
  public void setPrecision(Precision precision) {
    if (precision == Precision.EXACT)
      System.out.println("Warning: EXACT picking precision will behave like FIXED. EXACT precision is meant to be implemented for derived frames and scenes that support a backBuffer.");
    _precision = precision;
  }

  /**
   * Sets the length of the squared area around the frame {@link #position()} screen
   * projection that defined the frame picking condition.
   * <p>
   * If {@link #precision()} is {@link Precision#FIXED}, the {@code threshold} is expressed
   * in pixels and directly defines the fixed length of a 'shooter target', centered
   * at the projection of the frame origin onto the screen.
   * <p>
   * If {@link #precision()} is {@link Precision#ADAPTIVE}, the {@code threshold} is expressed
   * in object space (world units) and defines the edge length of a squared bounding box that
   * leads to an adaptive length of a 'shooter target', centered at the projection of the frame
   * origin onto the screen. Use this version only if you have a good idea of the bounding box
   * size of the object you are attaching to the frame shape.
   * <p>
   * The value is meaningless when the {@link #precision()} is* {@link Precision#EXACT}. See
   * {@link #setPrecision(Precision)} for details.
   * <p>
   * Default behavior is to set the PRECISIONTHRESHOLD (in a non-adaptive
   * manner) to 20.
   * <p>
   * Negative {@code threshold} values are silently ignored.
   *
   * @see #precision()
   * @see #setPrecision(Precision)
   * @see #precisionThreshold()
   */
  public void setPrecisionThreshold(float threshold) {
    if (threshold >= 0)
      _threshold = threshold;
  }

  /**
   * Returns the picking precision threshold in pixels used by {@link Graph#tracks(float, float, Frame)}.
   *
   * @see #setPrecisionThreshold(float)
   * @see #precision()
   * @see #setPrecision(Precision)
   */
  public float precisionThreshold() {
    return _threshold;
  }

  // CONSTRAINT

  /**
   * Returns the current {@link frames.core.constraint.Constraint} applied to the
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
    return worldLocation(new Vector(0, 0, 0));
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
    setTranslation(reference() != null ? reference().location(position) : position);
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
   * Same as {@code rotate(new Quaternion(axis, angle))}.
   *
   * @see #rotate(Quaternion)
   */
  public void rotate(Vector axis, float angle) {
    rotate(new Quaternion(axis, angle));
  }

  /**
   * Same as {@code rotate(new Vector(x,y,z), angle)}.
   *
   * @see #rotate(Vector, float)
   */
  public void rotate(float x, float y, float z, float angle) {
    rotate(new Vector(x, y, z), angle);
  }

  /**
   * Rotates the frame by the {@code quaternion} whose axis (see {@link Quaternion#axis()})
   * passes through {@code point}. The {@code quaternion} {@link Quaternion#axis()} is
   * defined in the frame coordinate system, while {@code point} is defined in the world
   * coordinate system).
   * <p>
   * Note: if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   */
  protected void _orbit(Quaternion quaternion, Vector center) {
    if (constraint() != null)
      quaternion = constraint().constrainRotation(quaternion, this);
    this.rotation().compose(quaternion);
    this.rotation().normalize(); // Prevents numerical drift

    // Original in frames-0.1.x and proscene:
    //Vector vector = Vector.add(center, (new Quaternion(orientation().rotate(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), center)));
    // TODO test frame hierarchy, we are using worldDisplacement instead of orientation().rotate
    Vector vector = Vector.add(center, (new Quaternion(worldDisplacement(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), center)));
    vector.subtract(translation());
    translate(vector);

    // Previous three lines are equivalent to:
    /*
    Quaternion worldQuaternion = new Quaternion(worldDisplacement(quaternion.axis()), quaternion.angle());
    Vector center2Position = Vector.subtract(position(), center);
    Vector center2PositionRotated = worldQuaternion.rotate(center2Position);
    Vector vector = Vector.add(center, center2PositionRotated);
    vector.subtract(translation());
    translate(vector);
    */
  }

  /**
   * Same as { orbit(new Quaternion(axis, angle))}.
   *
   * @see #orbit(Quaternion)
   */
  public void orbit(Vector axis, float angle) {
    orbit(new Quaternion(axis, angle));
  }

  /**
   * Same as {@code orbit(new Quaternion(axis, angle), frame)}.
   *
   * @see #orbit(Quaternion, Frame)
   */
  public void orbit(Vector axis, float angle, Frame frame) {
    orbit(new Quaternion(axis, angle), frame);
  }

  /**
   * Same as {@code orbit(quaternion, null)}.
   *
   * @see #orbit(Quaternion, Frame)
   */
  public void orbit(Quaternion quaternion) {
    orbit(quaternion, null);
  }

  /**
   * Rotates this frame around {@code frame} (which may be null for the world coordinate system)
   * according to {@code quaternion}.
   * <p>
   * The {@code quaternion} axes (see {@link Quaternion#axis()}) is defined in the {@code frame}
   * coordinate system.
   * <p>
   * Note: if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   */
  public void orbit(Quaternion quaternion, Frame frame) {
    Quaternion localQuaternion = new Quaternion(displacement(quaternion.axis(), frame), quaternion.angle());
    _orbit(localQuaternion, frame == null ? new Vector() : frame.position());

    // Note that the 'easy way' to do it (not relying on the _orbit() method)
    // by-passes the frame constraint (kept for the curious):
    /*
    Frame reference = frame == null ? new Frame() : frame.detach();
    Frame copy = new Frame(reference);
    copy.set(this);
    reference.rotate(quaternion);
    set(copy);
    // */
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
    Frame reference = reference();
    if (reference != null)
      setScaling(magnitude / reference.magnitude());
    else
      setScaling(magnitude);
  }

  // ALIGNMENT

  /**
   * Same as {@code align(null)}.
   *
   * @see #align(Frame)
   */
  public void align() {
    align(null);
  }

  /**
   * Convenience function that simply calls {@code align(false, 0.85f, frame)}
   *
   * @see #align(boolean, float, Frame)
   */
  public void align(Frame frame) {
    align(false, 0.85f, frame);
  }

  /**
   * Same as {@code align(move, null)}.
   *
   * @see #align(boolean, Frame)
   */
  public void align(boolean move) {
    align(move, null);
  }

  /**
   * Convenience function that simply calls {@code align(move, 0.85f, frame)}.
   *
   * @see #align(boolean, float, Frame)
   */
  public void align(boolean move, Frame frame) {
    align(move, 0.85f, frame);
  }

  /**
   * Same as {@code align(threshold, null)}.
   *
   * @see #align(boolean, Frame)
   */
  public void align(float threshold) {
    align(threshold, null);
  }

  /**
   * Convenience function that simply calls {@code align(false, threshold, frame)}.
   *
   * @see #align(boolean, float, Frame)
   */
  public void align(float threshold, Frame frame) {
    align(false, threshold, frame);
  }

  /**
   * Same as {@code align(move, threshold, null)}.
   *
   * @see #align(boolean, float, Frame)
   */
  public void align(boolean move, float threshold) {
    align(move, threshold, null);
  }

  /**
   * Aligns the frame with {@code frame}, so that two of their axis are parallel.
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
   * {@code frame} frame position (computed with {@link #location(Vector)}, in the Frame
   * coordinates system) does not change.
   * <p>
   * {@code frame} may be {@code null} and then represents the world coordinate system
   * (same convention than for the {@link #reference()}).
   */
  public void align(boolean move, float threshold, Frame frame) {
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
    //TODO needs testing
    Frame old = detach(); // correct line
    // VFrame old = this.get();// this call the get overloaded method and
    // hence add the frame to the mouse _grabber

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

      vector = Vector.subtract(center, worldDisplacement(old.location(center)));
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
   *
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #xAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setXAxis(Vector axis) {
    rotate(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), displacement(axis)));
  }

  /**
   * Rotates the frame so that its {@link #yAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   *
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #yAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setYAxis(Vector axis) {
    rotate(new Quaternion(new Vector(0.0f, 1.0f, 0.0f), displacement(axis)));
  }

  /**
   * Rotates the frame so that its {@link #zAxis()} becomes {@code axis} defined in the
   * world coordinate system.
   *
   * <b>Attention:</b> this rotation is not uniquely defined. See
   * {@link Quaternion#fromTo(Vector, Vector)}.
   *
   * @see #zAxis()
   * @see #setYAxis(Vector)
   * @see #setZAxis(Vector)
   */
  public void setZAxis(Vector axis) {
    rotate(new Quaternion(new Vector(0.0f, 0.0f, 1.0f), displacement(axis)));
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
    Vector axis = worldDisplacement(new Vector(positive ? 1.0f : -1.0f, 0.0f, 0.0f));
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
    Vector axis = worldDisplacement(new Vector(0.0f, positive ? 1.0f : -1.0f, 0.0f));
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
    Vector axis = worldDisplacement(new Vector(0.0f, 0.0f, positive ? 1.0f : -1.0f));
    if (magnitude() != 1)
      axis.normalize();
    return axis;
  }

  // CONVERSION

  /**
   * Returns the local transformation matrix represented by the frame.
   * <p>
   * This method could be used in conjunction with {@code pushMatrix()}, {@code popMatrix()}
   * and {@code applyMatrix()} to modify a graph modelView() matrix from a frame hierarchy.
   * For example, with this frame hierarchy:
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
   *
   * <b>Attention:</b> In Processing this technique is inefficient because
   * {@code papplet.applyMatrix} will try to calculate the inverse of the transform.
   * Use {@link frames.core.Graph#applyTransformation(Frame)} instead.
   *
   * @see #set(Frame)
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
   * Returns the global transformation matrix represented by the frame which grants
   * direct access to it, without the need to traverse its ancestor hierarchy first
   * (as it is the case with {@link #matrix()}).
   * <p>
   * This method should be used in conjunction with {@code applyMatrix()} to modify a
   * graph modelView() matrix from a frame:
   * <p>
   * {@code // Here the modelview matrix corresponds to the world coordinate system.} <br>
   * {@code Frame frame = new Frame(translation, new Rotation(from, to));} <br>
   * {@code graph.applyModelView(frame.worldMatrix());} <br>
   * {@code // draw object in the frame coordinate system.} <br>
   * <p>
   * This matrix represents the global frame transformation: the entire
   * {@link #reference()} hierarchy is taken into account to define the frame
   * transformation from the world coordinate system. Use {@link #matrix()} to get the
   * local frame transformation matrix (i.e. defined with respect to the
   * {@link #reference()}). These two match when the {@link #reference()} is
   * {@code null}.
   *
   * @see #set(Frame)
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
   * Returns the inverse of the matrix associated with the frame position and orientation that
   * is to be used when the frame represents an eye. This matrix matches the inverted of the
   * {@link #worldMatrix()} when {@link #scaling()} is {@code 1}.
   * <p>
   * The view matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using a projection matrix.
   *
   * @see Matrix#view(Vector, Quaternion)
   * @see #matrix()
   * @see #worldMatrix()
   * @see #set(Frame)
   * @see #set(Frame)
   */
  public Matrix view() {
    return Matrix.view(position(), orientation());
  }

  /**
   * Sets the frame from a {@link #matrix()} representation: rotation and scaling in the upper
   * left 3x3 matrix and translation on the last column.
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
   * {@code graph.pushModelView();} <br>
   * {@code graph.applyModelView(frame.matrix());} <br>
   * {@code // You are in the local frame coordinate system.} <br>
   * {@code graph.popModelView();} <br>
   * <p>
   * Which allows to apply local transformations to the {@code frame} while using geometry
   * data from other frame instances when necessary (see {@link #location(Vector, Frame)} and
   * {@link #displacement(Vector, Frame)}).
   *
   * @see #fromWorldMatrix(Matrix)
   * @see #matrix()
   */
  public void fromMatrix(Matrix matrix) {
    if (matrix._matrix[15] == 0) {
      System.out.println("Doing nothing: pM.mat[15] should be non-zero!");
      return;
    }

    setTranslation(
        matrix._matrix[12] / matrix._matrix[15],
        matrix._matrix[13] / matrix._matrix[15],
        matrix._matrix[14] / matrix._matrix[15]
    );

    float r00 = matrix._matrix[0] / matrix._matrix[15];
    float r01 = matrix._matrix[4] / matrix._matrix[15];
    float r02 = matrix._matrix[8] / matrix._matrix[15];
    setScaling(new Vector(r00, r01, r02).magnitude());// calls _modified() :P

    float[][] r = new float[3][3];
    r[0][0] = r00 / scaling();
    r[0][1] = r01 / scaling();
    r[0][2] = r02 / scaling();
    r[1][0] = (matrix._matrix[1] / matrix._matrix[15]) / scaling();
    r[1][1] = (matrix._matrix[5] / matrix._matrix[15]) / scaling();
    r[1][2] = (matrix._matrix[9] / matrix._matrix[15]) / scaling();
    r[2][0] = (matrix._matrix[2] / matrix._matrix[15]) / scaling();
    r[2][1] = (matrix._matrix[6] / matrix._matrix[15]) / scaling();
    r[2][2] = (matrix._matrix[10] / matrix._matrix[15]) / scaling();
    setRotation(new Quaternion(
        new Vector(r[0][0], r[1][0], r[2][0]),
        new Vector(r[0][1], r[1][1], r[2][1]),
        new Vector(r[0][2], r[1][2], r[2][2]))
    );
  }

  /**
   * Sets the frame from {@link #worldMatrix()} representation: orientation and magnitude
   * in the upper left 3x3 matrix and position on the last column.
   * <p>
   * Hence, if your openGL code fragment looks like:
   * <p>
   * {@code float [] m = new float [16]; m[0]=...;} <br>
   * {@code gl.glMultMatrixf(m);} <br>
   * <p>
   * It is equivalent to write:
   * <p>
   * {@code Frame frame = new Frame();} <br>
   * {@code frame.fromWorldMatrix(m);} <br>
   * {@code graph.applyModelView(frame.matrix());} <br>
   * {@code // You are in the local frame coordinate system.} <br>
   * <p>
   * Which allows to apply local transformations to the {@code frame} while using geometry
   * data from other frame instances when necessary (see {@link #location(Vector, Frame)} and
   * {@link #displacement(Vector, Frame)}).
   *
   * @see #fromMatrix(Matrix)
   * @see #worldMatrix()
   */
  public void fromWorldMatrix(Matrix matrix) {
    if (matrix._matrix[15] == 0) {
      System.out.println("Doing nothing: matrix.mat[15] should be non-zero!");
      return;
    }

    setPosition(
        matrix._matrix[12] / matrix._matrix[15],
        matrix._matrix[13] / matrix._matrix[15],
        matrix._matrix[14] / matrix._matrix[15]
    );

    float r00 = matrix._matrix[0] / matrix._matrix[15];
    float r01 = matrix._matrix[4] / matrix._matrix[15];
    float r02 = matrix._matrix[8] / matrix._matrix[15];
    setMagnitude(new Vector(r00, r01, r02).magnitude());// calls _modified() :P

    float[][] r = new float[3][3];
    r[0][0] = r00 / scaling();
    r[0][1] = r01 / scaling();
    r[0][2] = r02 / scaling();
    r[1][0] = (matrix._matrix[1] / matrix._matrix[15]) / scaling();
    r[1][1] = (matrix._matrix[5] / matrix._matrix[15]) / scaling();
    r[1][2] = (matrix._matrix[9] / matrix._matrix[15]) / scaling();
    r[2][0] = (matrix._matrix[2] / matrix._matrix[15]) / scaling();
    r[2][1] = (matrix._matrix[6] / matrix._matrix[15]) / scaling();
    r[2][2] = (matrix._matrix[10] / matrix._matrix[15]) / scaling();
    setOrientation(new Quaternion(
        new Vector(r[0][0], r[1][0], r[2][0]),
        new Vector(r[0][1], r[1][1], r[2][1]),
        new Vector(r[0][2], r[1][2], r[2][2]))
    );
  }

  /**
   * Returns a frame representing the inverse of this frame space transformation.
   * <p>
   * The new frame {@link #rotation()} is the
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
   * position. The {@link #magnitude()} is the original magnitude multiplicative
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

  // VECTOR CONVERSION

  /**
   * Converts {@code vector} displacement from world to this frame.
   * Same as {@code return displacement(vector, null)}.
   * {@link #location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Vector, Frame)
   * @see #location(Vector)
   */
  public Vector displacement(Vector vector) {
    return displacement(vector, null);
  }

  /**
   * Converts {@code vector} displacement from {@code frame} to this frame.
   * Use {@code frame.displacement(vector, this)} to perform the inverse transformation.
   * {@link #location(Vector, Frame)} converts locations instead of displacements.
   *
   * @see #displacement(Vector)
   * @see #worldDisplacement(Vector)
   */
  public Vector displacement(Vector vector, Frame frame) {
    return this == frame ? vector : _displacement(reference() != null ? reference().displacement(vector, frame) : frame == null ? vector : frame.worldDisplacement(vector));
  }

  /**
   * Converts {@code vector} displacement from this frame to world.
   * {@link #displacement(Vector)} performs the inverse transformation.
   * {@link #worldLocation(Vector)} converts locations instead of displacements.
   *
   * @see #location(Vector)
   * @see #displacement(Vector, Frame)
   */
  public Vector worldDisplacement(Vector vector) {
    Frame frame = this;
    Vector result = vector;
    while (frame != null) {
      result = frame._referenceDisplacement(result);
      frame = frame.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} displacement from {@link #reference()} to this frame.
   * <p>
   * {@link #_referenceDisplacement(Vector)} performs the inverse transformation.
   * {@link #_location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Vector)
   */
  protected Vector _displacement(Vector vector) {
    return Vector.divide(rotation().inverseRotate(vector), scaling());
  }

  /**
   * Converts {@code vector} displacement from this frame to {@link #reference()}.
   * <p>
   * {@link #_displacement(Vector)} performs the inverse transformation.
   * {@link #_referenceLocation(Vector)} converts locations instead of displacements.
   *
   * @see #worldDisplacement(Vector)
   */
  protected Vector _referenceDisplacement(Vector vector) {
    return rotation().rotate(Vector.multiply(vector, scaling()));
  }

  // POINT CONVERSION

  /**
   * Converts {@code vector} location from world to this frame.
   * Same as {@code return location(vector, null)}.
   * {@link #displacement(Vector)} converts displacements instead of locations.
   *
   * @see #location(Vector, Frame)
   * @see #displacement(Vector)
   */
  public Vector location(Vector vector) {
    return location(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code frame} to this frame.
   * Use {@code frame.location(vector, this)} to perform the inverse transformation.
   * {@link #displacement(Vector, Frame)} converts displacements instead of locations.
   *
   * @see #location(Vector)
   * @see #worldLocation(Vector)
   */
  public Vector location(Vector vector, Frame frame) {
    return this == frame ? vector : _location(reference() != null ? reference().location(vector, frame) : frame == null ? vector : frame.worldLocation(vector));
  }

  /**
   * Converts {@code vector} location from this frame to world.
   * {@link #location(Vector)} performs the inverse transformation.
   * {@link #worldDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #displacement(Vector)
   * @see #location(Vector, Frame)
   */
  public Vector worldLocation(Vector vector) {
    Frame frame = this;
    Vector result = vector;
    while (frame != null) {
      result = frame._referenceLocation(result);
      frame = frame.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} location from {@link #reference()} to this frame.
   * <p>
   * {@link #_referenceLocation(Vector)} performs the inverse transformation.
   * {@link #_displacement(Vector)} converts displacements instead of locations.
   *
   * @see #location(Vector)
   */
  protected Vector _location(Vector vector) {
    return Vector.divide(rotation().inverseRotate(Vector.subtract(vector, translation())), scaling());
  }

  /**
   * Converts {@code vector} location from this frame to {@link #reference()}.
   * <p>
   * {@link #_location(Vector)} performs the inverse transformation.
   * {@link #_referenceDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #worldLocation(Vector)
   */
  protected Vector _referenceLocation(Vector vector) {
    return Vector.add(rotation().rotate(Vector.multiply(vector, scaling())), translation());
  }

  // Attached frames

  /**
   * Returns the {@code graph} this frame is attached to. Always returns {@code false} if
   * the frame {@link #isDetached()}.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Returns {@code true} if tracking is enabled.
   *
   * @see #enableTracking(boolean)
   */
  public boolean isTrackingEnabled() {
    return _tracking;
  }

  /**
   * Enables frame tracking according to {@code flag}. When tracking is disabled {@link Graph#tracks(Point, Frame)}
   * returns {@code false}, {@link Graph#setTrackedFrame(String, Frame)} does nothing while
   * {@link Graph#track(String, Point)} and {@link Graph#cast(String, Point)} would bypass the frame.
   *
   * @see #isTrackingEnabled()
   */
  public void enableTracking(boolean flag) {
    _tracking = flag;
  }

  /**
   * Same as {@code return isDetached() ? false : isTracked(graph())}. Use it if the frame is
   * attached to a {@link #graph()}. Use {@link #isTracked(Graph)} if the frame {@link #isDetached()}.
   *
   * @see #isDetached()
   * @see #isTracked(Graph)
   */
  public boolean isTracked() {
    return isDetached() ? false : isTracked(graph());
  }

  /**
   * Returns {@code true} if the {@code frame} is being tracked by at least one {@code graph}
   * {@code hid} and {@code false} otherwise.
   *
   * @see Graph#isTrackedFrame(String, Frame)
   * @see Graph#isTrackedFrame(Frame)
   */
  public boolean isTracked(Graph graph) {
    return graph._agents.containsValue(this);
  }

  /**
   * Parse {@code gesture} params. Useful to implement the frame as an for application control.
   * Default implementation is empty. , i.e., it is meant to be implemented by derived classes.
   */
  public void interact(Object... gesture) {
  }

  /**
   * Procedure called on the frame by the graph traversal algorithm. Default implementation is
   * empty, i.e., it is meant to be implemented by derived classes. Only meaningful if the frame
   * is attached to a {@code graph}.
   * <p>
   * Hierarchical culling, i.e., culling of the frame and its children, should be decided here.
   * Set the culling flag with {@link #cull(boolean)} according to your culling condition:
   *
   * <pre>
   * {@code
   * frame = new Frame(graph) {
   *   public void visit() {
   *     // Hierarchical culling is optional and disabled by default. When the cullingCondition
   *     // (which should be implemented by you) is true, scene.traverse() will prune the branch
   *     // at the frame
   *     cull(cullingCondition);
   *     if(!isCulled())
   *       // Draw your object here, in the local coordinate system.
   *   }
   * }
   * }
   * </pre>
   *
   * @see Graph#traverse()
   * @see #cull(boolean)
   * @see #isCulled()
   */
  public void visit() {
  }

  /**
   * Same as {@code cull(true)}. Only meaningful if the frame is attached to
   * a {@code graph}.
   *
   * @see #cull(boolean)
   * @see #isCulled()
   */

  public void cull() {
    cull(true);
  }

  /**
   * Enables or disables {@link #visit()} of this frame and its children during
   * {@link Graph#traverse()}. Culling should be decided within {@link #visit()}.
   * Only meaningful if the frame is attached to a {@code graph}.
   *
   * @see #isCulled()
   */
  public void cull(boolean cull) {
    if (isDetached())
      System.out.println("Warning: culling a detached frame does nothing");
    _culled = cull;
  }

  /**
   * Returns whether or not the frame culled or not. Culled frames (and their children)
   * will not be visited by the {@link Graph#traverse()} algorithm. Always returns
   * {@code false} if the frame {@link #isDetached()}.
   *
   * @see #cull(boolean)
   */
  public boolean isCulled() {
    return isDetached() ? false : _culled;
  }
}
