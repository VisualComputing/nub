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
import frames.core.constraint.WorldConstraint;
import frames.primitives.Matrix;
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
 * <h2>Geometry transformations</h2>
 * A frame is useful to define the position, orientation and magnitude of an arbitrary object
 * which may represent a scene point-of-view.
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
 * <p>
 * The {@link #translation()}, {@link #rotation()} and uniform positive {@link #scaling()}
 * that are encapsulated in a frame can also be used to represent an angle preserving
 * transformation of space.
 * <h2>Hierarchy of frames</h2>
 * The frame position, orientation and magnitude are actually defined with respect to
 * a {@link #reference()} frame. The default {@link #reference()} is the world
 * coordinate system (represented by a {@code null} {@link #reference()}). If you
 * {@link #setReference(Frame)} to a different frame, you must then differentiate:
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
      frame = new Frame(_graph);
    return translation().matches(frame.translation()) && rotation().matches(frame.rotation()) && scaling() == frame.scaling();
  }

  protected Vector _translation;
  protected float _scaling;
  protected Quaternion _rotation;
  protected Frame _reference;
  protected Constraint _constraint;
  protected long _lastUpdate;

  //TODO new attributes

  /**
   * Enumerates the Picking precision modes.
   */
  public enum Precision {
    FIXED, ADAPTIVE, EXACT
  }

  protected Precision _precision;
  protected int _id;
  protected Graph _graph;
  protected List<Frame> _children;
  protected float _threshold;
  protected boolean _culled;

  //TODO check new Frame() vs detach() al over the place

  public Frame() {
    this(null, null, new Vector(), new Quaternion(), 1);
  }

  public Frame(Vector translation, Quaternion rotation, float scaling) {
    this(null, null, translation, rotation, scaling);
  }

  public Frame(Graph graph) {
    this(graph, null, new Vector(), new Quaternion(), 1);
  }

  public Frame(Frame reference) {
    this(reference.graph(), reference, new Vector(), new Quaternion(), 1);
  }

  public Frame(Graph graph, Vector translation, Quaternion rotation, float scaling) {
    this(graph, null, translation, rotation, scaling);
  }

  public Frame(Frame reference, Vector translation, Quaternion rotation, float scaling) {
    this(reference.graph(), reference, translation, rotation, scaling);
  }

  /**
   * Creates a frame with {@code reference} as {@link #reference()}, and {@code translation},
   * {@code rotation} and {@code scaling} as the frame {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   */
  protected Frame(Graph graph, Frame reference, Vector translation, Quaternion rotation, float scaling) {
    _graph = graph;
    setTranslation(translation);
    setRotation(rotation);
    setScaling(scaling);
    setReference(reference);

    if (graph == null)
      return;

    _id = ++graph()._nodeCount;

    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum node instances reached. Exiting now!");

    if (graph().is2D()) {
      if (position().z() != 0)
        throw new RuntimeException("2D frame z-position should be 0. Set it as: setPosition(x, y)");
      if (orientation().axis().x() != 0 || orientation().axis().y() != 0)
        throw new RuntimeException("2D frame rotation axis should (0,0,1). Set it as: setOrientation(new Quaternion(orientation().angle()))");
      WorldConstraint constraint2D = new WorldConstraint();
      constraint2D.setTranslationConstraint(WorldConstraint.Type.PLANE, new Vector(0, 0, 1));
      constraint2D.setRotationConstraint(WorldConstraint.Type.AXIS, new Vector(0, 0, 1));
      setConstraint(constraint2D);
    }

    _culled = false;
    _children = new ArrayList<Frame>();
    //setReference(reference());// _restorePath seems more robust
    _precision = Precision.FIXED;
    setPrecisionThreshold(20);
  }

  protected Frame(Frame frame, Graph graph) {
    _graph = graph;
    this.setTranslation(frame.translation().get());
    this.setRotation(frame.rotation().get());
    this.setScaling(frame.scaling());
    this.setReference(frame.reference());
    this.setConstraint(frame.constraint());

    if (graph == null)
      return;

    if (this.graph() == frame.graph()) {
      this._id = ++graph()._nodeCount;
      // unlikely but theoretically possible
      if (this._id == 16777216)
        throw new RuntimeException("Maximum iFrame instances reached. Exiting now!");
    } else {
      this._id = frame._id();
      this.set(frame);
    }

    this._culled = frame._culled;

    this._children = new ArrayList<Frame>();
    /*
    if (this.graph() == frame.graph()) {
      this.setReference(reference());// _restorePath
    }
    */

    _lastUpdate = frame.lastUpdate();
    this._precision = frame._precision;
    this._threshold = frame._threshold;
  }

  public Frame get() {
    return get(_graph);
  }

  /**
   * Returns a deep copy of this frame.
   */
  public Frame get(Graph graph) {
    if (graph == null)
      throw new RuntimeException("Frame graph should be non-null!");
    return new Frame(this, graph);
  }

  /**
   * Returns a new frame defined in the world coordinate system, with the same {@link #position()},
   * {@link #orientation()} and {@link #magnitude()} as this frame.
   *
   * @see #reference()
   */
  public Frame detach() {
    Frame frame = new Frame();
    frame.set(this);
    return frame;
  }

  public boolean isDetached() {
    return graph() == null;
  }

  public boolean isAttached() {
    return graph() != null;
  }

  /**
   * Sets {@link #position()}, {@link #orientation()} and {@link #magnitude()} values from
   * those of the {@code frame}. This frame {@link #constraint()} and {@link #reference()}
   * are not affected by this call.
   * <p>
   * After calling {@code set(frame)} a call to {@code this.matches(other)} should
   * return {@code true}.
   *
   * @see #worldMatrix()
   */
  public void set(Frame frame) {
    if (frame != null) {
      setPosition(frame.position());
      setOrientation(frame.orientation());
      setMagnitude(frame.magnitude());
    } else
      throw new RuntimeException("Frame should be non-null!");
  }

  public void reset() {
    setPosition(new Vector());
    setOrientation(new Quaternion());
    setMagnitude(1);
  }

  /**
   * Internal use. Frame graphics color to be used for picking with a color buffer.
   */
  protected int _id() {
    // see here:
    // http://stackoverflow.com/questions/2262100/rgb-int-to-rgb-python
    return (255 << 24) | ((_id & 255) << 16) | (((_id >> 8) & 255) << 8) | (_id >> 16) & 255;
  }

  public boolean isEye() {
    return graph().eye() == this;
  }

  public Graph graph() {
    return _graph;
  }

  // MODIFIED

  /**
   * @return the last frame the this obect was updated.
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
   * Internal use. Automatically call by all methods which change the frame state.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
    if (children() != null)
      for (Frame child : children())
        child._modified();
  }

  // REFERENCE_FRAME

  /**
   * Returns {@code true} if this frame is ancestor of {@code frame}.
   */
  public boolean isAncestor(Frame frame) {
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
   * Same as {@code return ancestor.isAncestor(frame)}.
   *
   * @see #isAncestor(Frame)
   * @see #path(Frame, Frame)
   */
  public static boolean isAncestor(Frame ancestor, Frame frame) {
    return ancestor.isAncestor(frame);
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
      System.out.println("A frame cannot be a reference of itself.");
      return;
    }
    if (isAncestor(frame)) {
      System.out.println("A frame descendant cannot be set as its reference.");
      return;
    }
    if (frame != null) {
      if (isDetached() && !frame.isDetached()) {
        System.out.println("An attached frame cannot be set as reference of a detached one.");
        return;
      }
      if (!isDetached() && frame.isDetached()) {
        System.out.println("An detached frame cannot be set as reference of an attached one.");
        return;
      }
    }
    if (isDetached()) {
      if (reference() == frame)
        return;
      _reference = frame;
      _modified();
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
        graph()._removeLeadingNode(this);
      // finally assign the reference frame
      _reference = frame;// reference() returns now the new value
      // 2b. after assigning new reference frame
      _restorePath(reference(), this);
      _modified();
    }
  }

  protected void _restorePath(Frame parent, Frame child) {
    if (parent == null) {
      if (graph() != null)
        graph()._addLeadingNode(child);
    } else {
      if (!parent._hasChild(child)) {
        parent._addChild(child);
        _restorePath(parent.reference(), parent);
      }
    }
  }

  public List<Frame> children() {
    return _children;
  }

  protected boolean _addChild(Frame node) {
    if (node == null)
      return false;
    if (_hasChild(node))
      return false;
    return children().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected boolean _removeChild(Frame node) {
    boolean result = false;
    Iterator<Frame> it = children().iterator();
    while (it.hasNext()) {
      if (it.next() == node) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  protected boolean _hasChild(Frame node) {
    for (Frame frame : children())
      if (frame == node)
        return true;
    return false;
  }

  /**
   * Procedure called on the node by the graph traversal algorithm. Default implementation is
   * empty, i.e., it is meant to be implemented by derived classes.
   * <p>
   * Hierarchical culling, i.e., culling of the node and its children, should be decided here.
   * Set the culling flag with {@link #cull(boolean)} according to your culling condition:
   *
   * <pre>
   * {@code
   * node = new Node(graph) {
   *   public void visit() {
   *     //hierarchical culling is optional and disabled by default
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
   * Same as {@code cull(true)}.
   *
   * @see #cull(boolean)
   * @see #isCulled()
   */

  public void cull() {
    cull(true);
  }

  /**
   * Enables or disables {@link #visit()} of this node and its children during
   * {@link Graph#traverse()}. Culling should be decided within {@link #visit()}.
   *
   * @see #isCulled()
   */
  public void cull(boolean cull) {
    _culled = cull;
  }

  /**
   * Returns whether or not the node culled or not. Culled nodes (and their children)
   * will not be visited by the {@link Graph#traverse()} algoruthm.
   *
   * @see #cull(boolean)
   */
  public boolean isCulled() {
    return _culled;
  }

  /**
   * Same as {@code randomize(graph().center(), graph().radius())}.
   *
   * @see #random(Graph)
   */
  public void randomize() {
    randomize(graph().center(), graph().radius());
  }

  /**
   * Randomized this frame. The frame is randomly re-positioned inside the ball
   * defined by {@code center} and {@code radius} (see {@link Vector#random()}). The
   * {@link #orientation()} is randomized by {@link Quaternion#randomize()}. The new
   * magnitude is a random in oldMagnitude * [0,5...2].
   *
   * @see #random(Graph)
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
   * Returns a random graph node. The node is randomly positioned inside the ball defined
   * by {@code center} and {@code radius} (see {@link Vector#random()}). The
   * {@link #orientation()} is set by {@link Quaternion#random()}. The magnitude
   * is a random in [0,5...2].
   *
   * @see #randomize()
   */
  public static Frame random(Graph graph) {
    Frame frame = new Frame(graph);
    Vector displacement = Vector.random();
    displacement.setMagnitude(graph.radius());
    frame.setPosition(Vector.add(graph.center(), displacement));
    frame.setOrientation(Quaternion.random());
    float lower = 0.5f;
    float upper = 2;
    frame.setMagnitude(((float) Math.random() * (upper - lower)) + lower);
    return frame;
  }

  //detached version
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

  // PRECISION

  /**
   * Returns the picking precision threshold in pixels used by the node to {@link #track(float, float)}.
   *
   * @see #setPrecisionThreshold(float)
   */
  public float precisionThreshold() {
    if (precision() == Precision.ADAPTIVE)
      return _threshold * scaling() * _graph.pixelToGraphRatio(position());
    return _threshold;
  }

  /**
   * Returns the node picking precision. See {@link #setPrecision(Precision)} for details.
   *
   * @see #setPrecision(Precision)
   * @see #setPrecisionThreshold(float)
   */
  public Precision precision() {
    return _precision;
  }

  /**
   * Sets the node picking precision.
   * <p>
   * When {@link #precision()} is {@link Precision#FIXED} or
   * {@link Precision#ADAPTIVE} Picking is done by checking if the pointer lies
   * within a squared area around the node {@link #center()} screen projection which size
   * is defined by {@link #setPrecisionThreshold(float)}.
   * <p>
   * When {@link #precision()} is {@link Precision#EXACT}, picking is done
   * in a precise manner according to the projected pixels of the visual representation
   * related to the node. It is meant to be implemented by derived classes (providing the
   * means attach a visual representation to the node) and requires the graph to implement
   * a back buffer.
   * <p>
   * Default implementation of this policy will behave like {@link Precision#FIXED}.
   *
   * @see #precision()
   * @see #setPrecisionThreshold(float)
   */
  public void setPrecision(Precision precision) {
    if (precision == Precision.EXACT)
      System.out.println("Warning: EXACT picking precision will behave like FIXED. EXACT precision is meant to be implemented for derived nodes and scenes that support a backBuffer.");
    _precision = precision;
  }

  /**
   * Sets the length of the squared area around the node {@link #center()} screen
   * projection that defined the {@link #track(float, float)} condition used for
   * node picking.
   * <p>
   * If {@link #precision()} is {@link Precision#FIXED}, the {@code threshold} is expressed
   * in pixels and directly defines the fixed length of a 'shooter target', centered
   * at the projection of the node origin onto the screen.
   * <p>
   * If {@link #precision()} is {@link Precision#ADAPTIVE}, the {@code threshold} is expressed
   * in object space (world units) and defines the edge length of a squared bounding box that
   * leads to an adaptive length of a 'shooter target', centered at the projection of the node
   * origin onto the screen. Use this version only if you have a good idea of the bounding box
   * size of the object you are attaching to the node shape.
   * <p>
   * The value is meaningless when the {@link #precision()} is* {@link Precision#EXACT}. See
   * {@link #setPrecision(Precision)} for details.
   * <p>
   * Default behavior is to set the {@link #precisionThreshold()} (in a non-adaptive
   * manner) to 20.
   * <p>
   * Negative {@code threshold} values are silently ignored.
   *
   * @see #precision()
   * @see #precisionThreshold()
   */
  public void setPrecisionThreshold(float threshold) {
    if (threshold >= 0)
      _threshold = threshold;
  }

  /**
   * Picks the node according to the {@link #precision()}.
   *
   * @see #precision()
   * @see #setPrecision(Precision)
   */
  public boolean track(float x, float y) {
    if (isEye())
      return false;
    Vector proj = _graph.screenLocation(position());
    float halfThreshold = precisionThreshold() / 2;
    return ((Math.abs(x - proj._vector[0]) < halfThreshold) && (Math.abs(y - proj._vector[1]) < halfThreshold));
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
   * Same as {@link #rotate(Quaternion)} but with {@code float} rotation parameters.
   */
  public void rotate(float x, float y, float z, float w) {
    rotate(new Quaternion(x, y, z, w));
  }

  /**
   * Rotates the frame by the {@code quaternion} whose axis (see {@link Quaternion#axis()})
   * passes through {@code point}. The {@code quaternion} {@link Quaternion#axis()} is
   * defined in the frame coordinate system, while {@code point} is defined in the world
   * coordinate system).
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a frame constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   */
  public void rotate(Quaternion quaternion, Vector point) {
    if (constraint() != null)
      quaternion = constraint().constrainRotation(quaternion, this);
    this.rotation().compose(quaternion);
    this.rotation().normalize(); // Prevents numerical drift

    Vector vector = Vector.add(point, (new Quaternion(orientation().rotate(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), point)));
    //Vector vector = Vector.add(point, (new Quaternion(worldDisplacement(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), point)));
    vector.subtract(translation());
    if (constraint() != null)
      translate(constraint().constrainTranslation(vector, this));
    else
      translate(vector);
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
    //TODO test
    Frame old = detach(); // correct line
    //Frame old = new Frame(this, graph()); // correct line
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

      //TODO really needs checking!
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
   * Convenience function that simply calls {@code graph.applyTransformation(this)}. You may
   * apply the transformation represented by this node to any graph you want using this
   * method.
   * <p>
   * Very efficient prefer always this than
   *
   * @see #applyTransformation()
   * @see #matrix()
   * @see Graph#applyTransformation(Frame)
   */
  public void applyTransformation() {
    graph().applyTransformation(this);
  }

  /**
   * Convenience function that simply calls {@code graph.applyWorldTransformation(this)}.
   * You may apply the world transformation represented by this node to any graph you
   * want using this method.
   *
   * @see #applyWorldTransformation()
   * @see #worldMatrix()
   * @see Graph#applyWorldTransformation(Frame)
   */
  public void applyWorldTransformation() {
    graph().applyWorldTransformation(this);
  }

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
   * @see #matrix()
   * @see #worldMatrix()
   * @see #set(Frame)
   * @see #set(Frame)
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

  /**
   * Rotates the node using {@code quaternion} around its {@link #position()} (non-eye nodes)
   * or around the {@link Graph#anchor()} when this node is the {@link Graph#eye()}.
   */
  //TODO discard in favor of graph spin!
  public void spin(Quaternion quaternion) {
    if (isEye())
      rotate(quaternion, graph().anchor());
    else
      rotate(quaternion);
  }

  /**
   * Wrapper method for {@link #alignWithFrame(Frame, boolean, float)} that discriminates
   * between eye and non-eye nodes.
   *
   * @see #isEye()
   */
  public void align() {
    if (isEye())
      alignWithFrame(null, true);
    else
      alignWithFrame(_graph.eye());
  }

  /**
   * Centers the node into the graph.
   */
  public void center() {
    if (isEye())
      projectOnLine(graph().center(), graph().viewDirection());
    else
      projectOnLine(_graph.eye().position(), _graph.eye().zAxis(false));
  }

  //TODO pending, see graph.path

  /**
   * Returns an array containg a straight path of frames from {@code tail} to {@code tip}.
   * Returns {@code null} if {@code tail} is not ancestor of {@code tip}.
   *
   * @see #isAncestor(Frame, Frame)
   */
  public static Frame[] path(Frame tail, Frame tip) {
    Frame[] path = null;
    if (tail.isAncestor(tip)) {
      int steps = 0;
      Frame _tip = tip;
      while (_tip != tail) {
        steps++;
        _tip = _tip.reference();
      }
      path = new Frame[steps + 1];
      _tip = tip;
      for (int i = steps; i >= 0; i--) {
        path[i] = _tip;
        _tip = _tip.reference();
      }
    }
    return path;
  }
}