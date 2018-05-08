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

import frames.primitives.*;
import frames.primitives.constraint.WorldConstraint;
import frames.timing.TimingHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Node is a {@link Frame} element on a {@link Graph} hierarchy.
 * <h2>Geometry transformations</h2>
 * <p>
 * To define the position, orientation and magnitude of a visual object, use {@link #matrix()}
 * (see the {@link frames.primitives.Frame} class documentation for details) or
 * {@link #applyTransformation()} (or {@link #applyWorldTransformation()}), as shown below:
 * <p>
 * {@code // Builds a node located at (0,0,0) with an identity orientation (node and
 * world axes match)} <br>
 * {@code Node node = new Node(graph);} <br>
 * {@code graph.pushModelView();} <br>
 * {@code node.applyWorldTransformation(); //same as graph.applyModelView(node.matrix());} <br>
 * {@code // Draw your object here, in the local coordinate system.} <br>
 * {@code graph.popModelView();} <br>
 * <p>
 * Alternatively, the node geometry transformation may be automatically handled by the graph
 * traversal algorithm (see {@link frames.core.Graph#cast()}), provided
 * that the node {@link #visit()} method is overridden, as shown below:
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
 * <p>
 * Implement a {@code cullingCondition} to perform hierarchical culling on the node
 * (culling of the node and its descendants by the {@link frames.core.Graph#cast()}
 * algorithm). The {@link #isCulled()} flag is {@code false} by default, see
 * {@link #cull(boolean)}.
 * <p>
 * A node may also be defined as the {@link Graph#eye()} (see {@link #isEye()}
 * and {@link Graph#setEye(Frame)}). Some user gestures are then interpreted in a negated way,
 * respect to non-eye nodes. For instance, with a move-to-the-right user gesture the
 * {@link Graph#eye()} has to go to the <i>left</i>, so that the scene seems to move
 * to the right.
 * <h2>Picking</h2>
 * Picking a node is done accordingly to a {@link #precision()}. Refer to
 * {@link #setPrecision(Precision)} for details.
 */
public class Node extends Frame {
  protected Graph _graph;
  protected List<Node> _children;
  protected float _threshold;
  protected boolean _culled;
  protected boolean _tracking;
  public enum Precision {
    FIXED, ADAPTIVE, EXACT
  }
  protected Precision _precision;

  /**
   * Same as {@code this(graph, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Graph, Node, Vector, Quaternion, float)
   */
  public Node(Graph graph) {
    this(graph, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference.graph(), reference, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Graph, Node, Vector, Quaternion, float)
   */
  public Node(Node reference) {
    this(reference.graph(), reference, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a graph node with {@code reference} as {@link #reference()}, and
   * {@code translation}, {@code rotation} and {@code scaling} as the frame
   * {@link #translation()}, {@link #rotation()} and {@link #scaling()}, respectively.
   * <p>
   * Sets the {@link #precision()} to {@link Precision#FIXED}.
   * <p>
   * After object creation a call to {@link #isEye()} will return {@code false}.
   */
  protected Node(Graph graph, Node reference, Vector translation, Quaternion rotation, float scaling) {
    super(reference, translation, rotation, scaling);
    _graph = graph;
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
    _children = new ArrayList<Node>();
    //setReference(reference());// _restorePath seems more robust
    _precision = Precision.FIXED;
    setPrecisionThreshold(20);
    enableTracking(true);
  }

  protected Node(Graph graph, Node node) {
    super(node);
    _graph = graph;
    this._culled = node._culled;
    this._children = new ArrayList<Node>();
    /*
    if (this.graph() == frame.graph()) {
      this.setReference(reference());// _restorePath
    }
    */
    this._precision = node._precision;
    this._threshold = node._threshold;
    this._tracking = node._tracking;
  }

  /**
   * Perform a deep, non-recursive copy of this node.
   * <p>
   * The copied node will keep this node {@link #reference()}, but its children aren't copied.
   *
   * @return node copy
   */
  @Override
  public Node get() {
    return new Node(this.graph(), this);
  }

  /**
   * Internal use. Automatically call by all methods which change the node state.
   */
  @Override
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
    if (children() != null)
      for (Node child : children())
        child._modified();
  }

  @Override
  public Node reference() {
    return (Node) this._reference;
  }

  @Override
  public void setReference(Frame frame) {
    if (frame instanceof Node || frame == null)
      setReference((Node) frame);
    else
      System.out.println("Warning: nothing done: Node.reference() should be instanceof Node");
  }

  /**
   * Same as {@link #setReference(Frame)} but using a Node parameter.
   */
  public void setReference(Node node) {
    if (node == this) {
      System.out.println("A node cannot be a reference of itself.");
      return;
    }
    if (isAncestor(node)) {
      System.out.println("A node descendant cannot be set as its reference.");
      return;
    }
    // 1. no need to re-parent, just check this needs to be added as leadingFrame
    if (reference() == node) {
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
    _reference = node;// reference() returns now the new value
    // 2b. after assigning new reference frame
    _restorePath(reference(), this);
    _modified();
  }

  public boolean isEye() {
    return graph().eye() == this;
  }

  public Graph graph() {
    return _graph;
  }

  protected void _restorePath(Node parent, Node child) {
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

  public List<Node> children() {
    return _children;
  }

  protected boolean _addChild(Node frame) {
    if (frame == null)
      return false;
    if (_hasChild(frame))
      return false;
    return children().add(frame);
  }

  /**
   * Removes the leading frame if present. Typically used when re-parenting the frame.
   */
  protected boolean _removeChild(Frame frame) {
    boolean result = false;
    Iterator<Node> it = children().iterator();
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
    for (Frame child : children())
      if (child == frame)
        return true;
    return false;
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
   * Returns a random graph node. The node is randomly positioned inside the ball defined
   * by {@code center} and {@code radius} (see {@link Vector#random()}). The
   * {@link #orientation()} is set by {@link Quaternion#random()}. The magnitude
   * is a random in [0,5...2].
   *
   * @see #randomize()
   */
  public static Node random(Graph graph) {
    Node node = new Node(graph);
    Vector displacement = Vector.random();
    displacement.setMagnitude(graph.radius());
    node.setPosition(Vector.add(graph.center(), displacement));
    node.setOrientation(Quaternion.random());
    float lower = 0.5f;
    float upper = 2;
    node.setMagnitude(((float) Math.random() * (upper - lower)) + lower);
    return node;
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
   * Enables frame tracking according to {@code flag}.
   *
   * @see #isTrackingEnabled()
   */
  public void enableTracking(boolean flag) {
    _tracking = flag;
  }

  //TODO docs
  protected void _visit(float x, float y) {
    if (graph().trackedFrame() == null && isTrackingEnabled())
      if (track(x, y))
        graph().setTrackedFrame(this);
    visit();
  }

  /**
   * Procedure called on the frame by the graph traversal algorithm. Default implementation is
   * empty, i.e., it is meant to be implemented by derived classes.
   * <p>
   * Hierarchical culling, i.e., culling of the frame and its children, should be decided here.
   * Set the culling flag with {@link #cull(boolean)} according to your culling condition:
   *
   * <pre>
   * {@code
   * frame = new Frame(graph) {
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
   * @see Graph#cast()
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
   * Enables or disables {@link #visit()} of this frame and its children during
   * {@link Graph#cast()}. Culling should be decided within {@link #visit()}.
   *
   * @see #isCulled()
   */
  public void cull(boolean cull) {
    _culled = cull;
  }

  /**
   * Returns whether or not the frame culled or not. Culled frames (and their children)
   * will not be visited by the {@link Graph#cast()} algoruthm.
   *
   * @see #cull(boolean)
   */
  public boolean isCulled() {
    return _culled;
  }

  /**
   * Convenience function that simply calls {@code graph.applyTransformation(this)}. You may
   * apply the transformation represented by this frame to any graph you want using this
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
   * You may apply the world transformation represented by this frame to any graph you
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
   * Rotates the frame using {@code quaternion} around its {@link #position()} (non-eye frames)
   * or around the {@link Graph#anchor()} when this frame is the {@link Graph#eye()}.
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
   * between eye and non-eye frames.
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
   * Centers the frame into the graph.
   */
  public void center() {
    if (isEye())
      projectOnLine(graph().center(), graph().viewDirection());
    else
      projectOnLine(_graph.eye().position(), _graph.eye().zAxis(false));
  }

  // PRECISION

  /**
   * Returns the picking precision threshold in pixels used by the frame to {@link #track(float, float)}.
   *
   * @see #setPrecisionThreshold(float)
   */
  public float precisionThreshold() {
    if (precision() == Precision.ADAPTIVE)
      return _threshold * scaling() * _graph.pixelToGraphRatio(position());
    return _threshold;
  }

  /**
   * Returns the frame picking precision. See {@link #setPrecision(Precision)} for details.
   *
   * @see #setPrecision(Precision)
   * @see #setPrecisionThreshold(float)
   */
  public Precision precision() {
    return _precision;
  }

  /**
   * Sets the frame picking precision.
   * <p>
   * When {@link #precision()} is {@link Precision#FIXED} or
   * {@link Precision#ADAPTIVE} Picking is done by checking if the pointer lies
   * within a squared area around the frame {@link #center()} screen projection which size
   * is defined by {@link #setPrecisionThreshold(float)}.
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
   */
  public void setPrecision(Precision precision) {
    if (precision == Precision.EXACT)
      System.out.println("Warning: EXACT picking precision will behave like FIXED. EXACT precision is meant to be implemented for derived frames and scenes that support a backBuffer.");
    _precision = precision;
  }

  /**
   * Sets the length of the squared area around the frame {@link #center()} screen
   * projection that defined the {@link #track(float, float)} condition used for
   * frame picking.
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
   * Picks the frame according to the {@link #precision()}.
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
}