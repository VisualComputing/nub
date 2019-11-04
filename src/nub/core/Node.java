/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

package nub.core;

import nub.core.constraint.Constraint;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.timing.TimingHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A node encapsulates a 2D or 3D coordinate system, represented by a {@link #position()}, an
 * {@link #orientation()} and {@link #magnitude()}. The order of these transformations is
 * important: the node is first translated, then rotated around the new translated origin
 * and then scaled. This class API partially conforms that of the great
 * <a href="http://libqglviewer.com/refManual/classqglviewer_1_1Frame.html">libQGLViewer</a>.
 * <h2>Hierarchy of nodes</h2>
 * The node position, orientation and magnitude are actually defined with respect to
 * a {@link #reference()} node. The default {@link #reference()} is the world
 * coordinate system (represented by a {@code null} {@link #reference()}). If you
 * {@link #setReference(Node)} to a different node, you must then differentiate:
 * <ul>
 * <li>The <b>local</b> {@link #translation()}, {@link #rotation()} and {@link #scaling()},
 * defined with respect to the {@link #reference()} which represents an angle preserving
 * transformation of space.</li>
 * <li>The <b>global</b> {@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}, always defined with respect to the world coordinate system.</li>
 * </ul>
 * <p>
 * A node is actually defined by its {@link #translation()} with respect to its
 * {@link #reference()}, then by {@link #rotation()} of the coordinate system around
 * the new translated origin and then by a uniform positive {@link #scaling()} along its
 * rotated axes.
 * <p>
 * This terminology for <b>local</b> ({@link #translation()}, {@link #rotation()} and
 * {@link #scaling()}) and <b>global</b> ({@link #position()}, {@link #orientation()} and
 * {@link #magnitude()}) definitions is used in all the methods' names and should be
 * enough to prevent ambiguities. These notions are obviously identical when the
 * {@link #reference()} is {@code null}, i.e., when the node is defined in the world
 * coordinate system (the one you are left with after calling a graph preDraw() method).
 * <h2>Geometry transformations</h2>
 * A node is useful to define the position, orientation and magnitude of an arbitrary object
 * which may represent a point-of-view.
 * <p>
 * Use {@link #matrix()} to access the node coordinate system, as when drawing an object
 * locally:
 * <p>
 * {@code // Builds a node at position (0.5,0,0) and oriented such that its Y axis is
 * along the (1,1,1)} direction<br>
 * {@code Node node = new Node(new Vector(0.5,0,0), new Quaternion(new Vector(0,1,0),
 * new Vector(1,1,1)));} <br>
 * {@code pushMatrix();} <br>
 * {@code graph.applyTransformation(node);} <br>
 * {@code // Draw your object here, in the local node coordinate system.} <br>
 * {@code popMatrix();} <br>
 * <p>
 * Use {@link #view()} and {@link #projection(Graph.Type, float, float, float, float, boolean)}
 * when rendering the scene from the node point-of-view. Note these methods are used by the
 * graph when a node is set as its eye, see {@link Graph#preDraw()}.
 * <p>
 * To transform a point from one node to another use {@link #location(Vector, Node)} and
 * {@link #worldLocation(Vector)}. To instead transform a vector (such as a normal) use
 * {@link #displacement(Vector, Node)} and {@link #worldDisplacement(Vector)}.
 * <h2>Hierarchical traversals</h2>
 * Hierarchical traversals of the node hierarchy which automatically apply the local
 * node transformations described above may be achieved with {@link Graph#render()} or
 * {@link Graph#render()}.
 * Automatic traversals require overriding {@link #visit()} or {@link Graph#draw(Object, Node)}
 * and to instantiate a node attached to a graph which is referred to as attached node (see
 * {@link #isAttached(Graph)} and {@link #isDetached()}).
 * <p>
 * To instantiate an attached node use the node constructors that take a {@code graph}
 * parameter or a (reference) node which in turn is attached to a graph. Once instantiated,
 * a node cannot be attached nor detached, but a copy of it can (see {@link #attach(Graph)}
 * and {@link #detach()}).
 * <h2>Constraints</h2>
 * One interesting feature of a node is that its displacements can be constrained. When a
 * {@link Constraint} is attached to a node, it filters the input of {@link #translate(Vector)}
 * and {@link #rotate(Quaternion)}, and only the resulting filtered motion is applied to the
 * node. The default {@link #constraint()} is {@code null} resulting in no filtering.
 * Use {@link #setConstraint(Constraint)} to attach a constraint to a node.
 * <p>
 * Classical constraints are provided for convenience (see
 * {@link nub.core.constraint.LocalConstraint},
 * {@link nub.core.constraint.WorldConstraint} and
 * {@link nub.core.constraint.EyeConstraint}) and new constraints can very
 * easily be implemented.
 * <h2>Shapes</h2>
 * A node shape can be set from a retained-mode rendering object, see {@link #setShape(Object)};
 * or from an immediate-mode rendering procedure, see {@link #graphics(Object)}.
 * Picking a node is done according to a {@link #pickingThreshold()}. When a node is tracked
 * it will be highlighted (scaled) according to a {@link #highlighting()} magnitude.
 * See also {@link #enableTracking(boolean)}.
 * <h2>Application Control</h2>
 * Implementing an application control for the node is a two step process:
 * <ul>
 * <li>Parse user gesture data by overriding {@link #interact(Object...)}.</li>
 * <li>Send gesture data to the node by calling {@link Graph#interact(String, Object...)} or
 * {@link Graph#interact(Node, Object...)}.</li>
 * </ul>
 * <h2>Syncing</h2>
 * Two nodes can be synced together ({@link #sync(Node, Node)}), meaning that they will
 * share their global parameters (position, orientation and magnitude) taken the one
 * that has been most recently updated. Syncing can be useful to share nodes
 * among different off-screen canvases.
 */
public class Node {
  /**
   * Returns whether or not this node matches other taking into account the {@link #translation()},
   * {@link #rotation()} and {@link #scaling()} node parameters, but not its {@link #reference()}.
   *
   * @param node node
   */
  public boolean matches(Node node) {
    if (node == null)
      node = new Node();
    return translation().matches(node.translation()) && rotation().matches(node.rotation()) && scaling() == node.scaling();
  }

  protected Vector _translation;
  protected float _scaling;
  protected Quaternion _rotation;
  protected Node _reference;
  protected Constraint _constraint;
  protected long _lastUpdate;

  // Tracking & Precision
  protected float _threshold;

  // ID
  protected static int _counter;
  protected int _id;

  // Attached nodes
  protected Graph _graph;
  protected List<Node> _children;
  protected boolean _culled;
  protected boolean _tracking;

  // Rendering
  protected Object _shape;
  protected float _highlight;

  /**
   * Creates a detached node.
   * Same as {@code this(null, null, null, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Object)
   * @see #Node(Graph)
   * @see #Node(Node)
   * @see #Node(Constraint)
   */
  public Node() {
    this(null, null, null, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a node attached to {@code graph}.
   * Same as {@code this(graph, null, null, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node()
   * @see #Node(Object)
   * @see #Node(Node)
   * @see #Node(Constraint)
   */
  public Node(Graph graph) {
    this(graph, null, null, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a child node with {@code reference} as {@link #reference()}.
   * Same as {@code this(reference.graph(), reference, null, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node()
   * @see #Node(Graph)
   * @see #Node(Object)
   * @see #Node(Constraint)
   */
  public Node(Node reference) {
    this(reference.graph(), reference, null, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a detached node with {@code constraint} as {@link #constraint()}.
   * Same as {@code this(null, null, constraint, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Graph)
   * @see #Node(Object)
   * @see #Node()
   * @see #Node(Node)
   */
  public Node(Constraint constraint) {
    this(null, null, constraint, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a detached node with {@code shape}.
   * Same as {@code this(null, null, null, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node()
   * @see #Node(Graph)
   * @see #Node(Node)
   * @see #Node(Constraint)
   */
  public Node(Object shape) {
    this(null, null, null, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Constructs a shapeless node, attached to {@code graph}, having {@code translation},
   * {@code rotation} and {@code scaling} as the node {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   * The {@link #pickingThreshold()} is set to {@code 0.2}.
   * Same as {@code this(graph, null, null, null, translation, rotation, scaling)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   * @see #Node(Vector, Quaternion, float)
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Graph graph, Vector translation, Quaternion rotation, float scaling) {
    this(graph, null, null, null, translation, rotation, scaling);
  }

  /**
   * Constructs a shapeless node, having {@code reference}, {@code translation},
   * {@code rotation} and {@code scaling} as the node {@link #reference()},
   * {@link #translation()}, {@link #rotation()} and {@link #scaling()}, respectively.
   * The {@link #pickingThreshold()} is set to {@code 0.2}.
   * Same as {@code this(reference.graph(), reference, null, null, translation, rotation, scaling)}.
   *
   * @see #Node(Graph, Vector, Quaternion, float)
   * @see #Node(Vector, Quaternion, float)
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Node reference, Vector translation, Quaternion rotation, float scaling) {
    this(reference.graph(), reference, null, null, translation, rotation, scaling);
  }

  /**
   * Constructs a shapeless detached node, having {@code translation},
   * {@code rotation} and {@code scaling} as the node {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   * The {@link #pickingThreshold()} is set to {@code 0.2}.
   * Same as {@code this(null, null, null, null, translation, rotation, scaling)}.
   *
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Vector translation, Quaternion rotation, float scaling) {
    this(null, null, null, null, translation, rotation, scaling);
  }

  /**
   * Same as {@code this(graph, null, shape)}.
   *
   * @see #Node(Graph, Constraint, Object)
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Graph graph, Object shape) {
    this(graph, null, shape);
  }

  /**
   * Same as {@code this(graph, constraint, null)}.
   *
   * @see #Node(Graph, Constraint, Object)
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Graph graph, Constraint constraint) {
    this(graph, constraint, null);
  }

  /**
   * Same as {@code this(graph, constraint, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Graph, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Graph graph, Constraint constraint, Object shape) {
    this(graph, constraint, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, null, shape)}.
   *
   * @see #Node(Node, Constraint, Object)
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Node reference, Object shape) {
    this(reference, null, shape);
  }

  /**
   * Same as {@code this(reference, constraint, null)}.
   *
   * @see #Node(Node, Constraint, Object)
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Node reference, Constraint constraint) {
    this(reference, constraint, null);
  }

  /**
   * Same as {@code this(reference, constraint, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Node reference, Constraint constraint, Object shape) {
    this(reference, constraint, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a node attached to {@code graph} with {@code constraint} as {@link #constraint()},
   * having {@code translation}, {@code rotation} and {@code scaling} as the node {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively. The {@link #pickingThreshold()} is set to {@code 0.2}.
   * Same as {@code this(graph, null, constraint, shape, translation, rotation, scaling)}.
   *
   * @see #Node(Graph, Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Graph graph, Constraint constraint, Object shape, Vector translation, Quaternion rotation, float scaling) {
    this(graph, null, constraint, shape, translation, rotation, scaling);
  }

  /**
   * Creates a child node of {@code reference} with {@code constraint} as {@link #constraint()},
   * having {@code translation}, {@code rotation} and {@code scaling} as the node {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively. The {@link #pickingThreshold()} is set to {@code 0.2}.
   * Same as {@code this(reference.graph(), reference, constraint, shape, translation, rotation, scaling)}.
   *
   * @see #Node(Graph, Node, Constraint, Object, Vector, Quaternion, float)
   */
  public Node(Node reference, Constraint constraint, Object shape, Vector translation, Quaternion rotation, float scaling) {
    this(reference.graph(), reference, constraint, shape, translation, rotation, scaling);
  }

  /**
   * Creates a node attached to {@code graph} with {@code constraint} as {@link #constraint()},
   * having {@code reference} as {@link #reference()}, {@code translation},
   * {@code rotation} and {@code scaling} as the node {@link #translation()},
   * {@link #rotation()} and {@link #scaling()}, respectively.
   * The {@link #pickingThreshold()} is set to {@code 0.2} and the {@link #highlighting()}
   * magnitude to {@code 0.15}.
   */
  protected Node(Graph graph, Node reference, Constraint constraint, Object shape, Vector translation, Quaternion rotation, float scaling) {
    _graph = graph;
    setReference(reference);
    setConstraint(constraint);
    setShape(shape);
    setTranslation(translation);
    setRotation(rotation);
    setScaling(scaling);
    _id = ++_counter;
    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum node instances reached. Exiting now!");
    _lastUpdate = 0;
    _threshold = .2f;
    _tracking = true;
    _highlight = 0.15f;
    if (graph() == null)
      return;
    // attached nodes:
    _children = new ArrayList<Node>();
    _culled = false;
  }

  /**
   * Copy constructor.
   */
  protected Node(Graph graph, Node node) {
    this._graph = graph;
    this.setPosition(node.position());
    this.setOrientation(node.orientation());
    this.setMagnitude(node.magnitude());
    this.setConstraint(node.constraint());

    if ((this.isDetached() && node.isDetached()) || !(this.isDetached() && !node.isDetached()))
      setReference(node.reference());

    this._id = ++_counter;
    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum node instances reached. Exiting now!");
    _lastUpdate = node.lastUpdate();
    this._threshold = node._threshold;
    this._tracking = node._tracking;

    if (graph() == null)
      return;

    // attached nodes:
    this._children = new ArrayList<Node>();
    this._culled = node._culled;

    this._shape = node._shape;
    this._highlight = node._highlight;
  }

  /**
   * Performs a deep copy of this node into {@code graph}.
   * <p>
   * Same as {@code return new Node(graph, this)}.
   *
   * @see #Node(Graph, Node)
   */
  public Node attach(Graph graph) {
    return new Node(graph, this);
  }

  /**
   * Performs a deep copy of this node.
   * <p>
   * Same as {@code return attach(graph())}.
   *
   * @see #attach(Graph)
   */
  public Node get() {
    return attach(graph());
  }

  /**
   * Returns a detached deep copy of this node.
   * <p>
   * Same as {@code return attach(null)}.
   *
   * @see #attach(Graph)
   */
  public Node detach() {
    return attach(null);
  }

  /**
   * Tells whether or not this node belongs to the {@graph} hierarchy (see {@link Graph#render()}).
   * To test if the node is detach from any graph hierarchy call {@code isAttached(null)}.
   * <p>
   * Note that a call to {@link #children()} never returns {@code null} if the node is attached to
   * a graph, i.e., that graph will visit the node during traversal.
   *
   * @see #isDetached()
   * @see Graph#render()
   */
  public boolean isAttached(Graph graph) {
    return _graph == graph;
  }

  /**
   * Same as {@code return isAttached(null)}.
   * <p>
   * Note that a call to {@link #children()} always returns {@code null} if the node is detached,
   * i.e., the node is not available for graph traversal (see {@link Graph#render()}).
   *
   * @see #isAttached(Graph)
   * @see Graph#render()
   */
  public boolean isDetached() {
    return isAttached(null);
  }

  /**
   * Sets {@link #position()}, {@link #orientation()} and {@link #magnitude()} values from
   * those of the {@code node}. The node {@link #graph()}, {@link #reference()} and
   * {@link #constraint()} are not affected by this call.
   * <p>
   * After calling {@code set(node)} a call to {@code this.matches(node)} should
   * return {@code true}.
   *
   * @see #reset()
   * @see #worldMatrix()
   * @see #setPosition(Node)
   * @see #setOrientation(Node)
   * @see #setMagnitude(Node)
   */
  public void set(Node node) {
    if (node == null)
      node = new Node();
    setPosition(node.position());
    setOrientation(node.orientation());
    setMagnitude(node.magnitude());
  }

  /**
   * Sets an identity node by resetting its {@link #translation()}, {@link #rotation()}
   * and {@link #scaling()}. The node {@link #graph()}, {@link #reference()} and
   * {@link #constraint()} are not affected by this call. Call {@code set(null)} if you
   * want to reset the global {@link #position()}, {@link #orientation()} and
   * {@link #magnitude()} node parameters instead.
   *
   * @see #set(Node)
   */
  public void reset() {
    setTranslation(new Vector());
    setRotation(new Quaternion());
    setScaling(1);
  }

  // colorID

  /**
   * Returns the unique sequential node id assigned at instantiation time.
   * Used by {@link #colorID()} and {@link Graph#_drawBackBuffer(Node)}.
   */
  public int id() {
    return _id;
  }

  /**
   * Uniquely identifies the node. Also the color to be used for picking with a color buffer.
   * See: http://stackoverflow.com/questions/2262100/rgb-int-to-rgb-python
   */
  public int colorID() {
    return (255 << 24) | ((_id & 255) << 16) | (((_id >> 8) & 255) << 8) | (_id >> 16) & 255;
  }

  // MODIFIED

  /**
   * @return the last frame the this node was updated.
   */
  public long lastUpdate() {
    return _lastUpdate;
  }

  // SYNC

  /**
   * Same as {@code sync(this, other)}.
   *
   * @see #sync(Node, Node)
   */
  public void sync(Node node) {
    sync(this, node);
  }

  /**
   * If {@code node1} has been more recently updated than {@code node2}, calls
   * {@code node2.set(node1)}, otherwise calls {@code node1.set(node2)}.
   * Does nothing if both objects were updated at the same time.
   * <p>
   * This method syncs only the global geometry attributes ({@link #position()},
   * {@link #orientation()} and {@link #magnitude()}) among the two nodes. The
   * {@link #reference()} and {@link #constraint()} (if any) of each node are kept
   * separately.
   *
   * @see #set(Node)
   */
  public static void sync(Node node1, Node node2) {
    if (node1 == null || node2 == null)
      return;
    if (node1.lastUpdate() == node2.lastUpdate())
      return;
    Node source = (node1.lastUpdate() > node2.lastUpdate()) ? node1 : node2;
    Node target = (node1.lastUpdate() > node2.lastUpdate()) ? node2 : node1;
    target.set(source);
  }

  /**
   * Internal use. Automatically call by all methods which change the node state.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
    if (_children != null)
      for (Node child : _children)
        child._modified();
  }

  // REFERENCE_node

  /**
   * Returns {@code true} if {@code node} is {@link #reference()} {@code this} node.
   */
  public boolean isReference(Node node) {
    return reference() == node;
  }

  /**
   * Returns {@code true} if {@code node} is ancestor of {@code this} node.
   */
  public boolean isAncestor(Node node) {
    if (node == null)
      return true;
    return node._isSuccessor(this);
  }

  /**
   * Returns {@code true} if {@code node} is successor of {@code this} node.
   */
  protected boolean _isSuccessor(Node node) {
    if (node == this || node == null)
      return false;
    Node ancestor = node.reference();
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
   * @see #isAncestor(Node)
   * @see #path(Node, Node)
   */
  public static boolean isAncestor(Node successor, Node ancestor) {
    return successor.isAncestor(ancestor);
  }

  /**
   * Returns an array containing a straight path of nodes from {@code tail} to {@code tip}.
   * Returns an empty list if {@code tail} is not ancestor of {@code tip}.
   *
   * @see #isAncestor(Node, Node)
   */
  public static List<Node> path(Node tail, Node tip) {
    ArrayList<Node> list = new ArrayList<Node>();
    if (tip.isAncestor(tail)) {
      Node _tip = tip;
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
   * Returns the reference node, in which this node is defined.
   * <p>
   * The node {@link #translation()}, {@link #rotation()} and {@link #scaling()} are
   * defined with respect to the {@link #reference()} coordinate system. A
   * {@code null} reference node (default value) means that the node is defined in the
   * world coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to
   * recursively convert values along the reference node chain and to get values
   * expressed in the world coordinate system. The values match when the reference node
   * is {@code null}.
   * <p>
   * Use {@link #setReference(Node)} to set this value and create a node hierarchy.
   * Convenient functions allow you to convert coordinates and vectors from one node to
   * another: see {@link #location(Vector, Node)} and {@link #displacement(Vector, Node)},
   * respectively.
   */
  public Node reference() {
    return _reference;
  }

  /**
   * Same as {@code setReference(null)}.
   *
   * @see #setReference(Node)
   * @see #resetConstraint()
   */
  public void resetReference() {
    setReference(null);
  }

  /**
   * Sets the {@link #reference()} of the node.
   * <p>
   * The node {@link #translation()}, {@link #rotation()} and {@link #scaling()} are then
   * defined in the {@link #reference()} coordinate system.
   * <p>
   * Use {@link #position()}, {@link #orientation()} and {@link #magnitude()} to express
   * the node global transformation in the world coordinate system.
   * <p>
   * Using this method, you can create a hierarchy of nodes. This hierarchy needs to be a
   * tree, which root is the world coordinate system (i.e., {@code null}
   * {@link #reference()}). No action is performed if setting {@code reference} as the
   * {@link #reference()} would create a loop in the hierarchy.
   */
  public void setReference(Node node) {
    if (node == this) {
      System.out.println("A Node cannot be a reference of itself.");
      return;
    }
    if (_isSuccessor(node)) {
      System.out.println("A Node descendant cannot be set as its reference.");
      return;
    }
    if (node != null)
      if ((isDetached() && !node.isDetached()) || !node.isAttached(graph())) {
        System.out.println("Both node and its reference should be detached, or attached to the same graph.");
        return;
      }
    if (isDetached()) {
      if (reference() == node)
        return;
      _reference = node;
    } else {
      // 1. no need to re-parent, just check this needs to be added as leadingnode
      if (reference() == node) {
        _restorePath(reference(), this);
        return;
      }
      // 2. else re-parenting
      // 2a. before assigning new reference node
      if (reference() != null) // old
        reference()._removeChild(this);
      else if (graph() != null)
        graph()._removeLeadingNode(this);
      // finally assign the reference node
      _reference = node;// reference() returns now the new value
      // 2b. after assigning new reference node
      _restorePath(reference(), this);
    }
    _modified();
  }

  /**
   * Used by {@link #setReference(Node)}.
   */
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

  /**
   * Used by {@link #_restorePath(Node, Node)}.
   */
  protected boolean _addChild(Node node) {
    if (node == null)
      return false;
    if (_hasChild(node))
      return false;
    return _children.add(node);
  }

  /**
   * Removes the leading Node if present. Typically used when re-parenting the Node.
   */
  protected boolean _removeChild(Node node) {
    boolean result = false;
    Iterator<Node> it = _children.iterator();
    while (it.hasNext()) {
      if (it.next() == node) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  protected boolean _hasChild(Node node) {
    for (Node child : _children)
      if (child == node)
        return true;
    return false;
  }

  /**
   * Returns the list a child nodes of this node. Only meaningful if this node {@link #isAttached(Graph)}
   * to a graph. Returns {@code null} if this node {@link #isDetached()}.
   */
  public List<Node> children() {
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
   * Does nothing if the node {@link #isDetached()}.
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
      System.out.println("randomize() is only available for attached nodes, nothing done! Use randomize(center, radius, is3D) instead");
  }

  /**
   * Randomized this node. The node is randomly re-positioned inside the ball
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
   * Returns a random node attached to {@code graph}. The node is randomly positioned inside
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
  public static Node random(Graph graph) {
    Node node = new Node(graph);
    node.randomize(graph.center(), graph.radius(), graph.is3D());
    return node;
  }

  /**
   * Returns a random detached node. The node is randomly positioned inside the ball defined
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
  public static Node random(Vector center, float radius, boolean is3D) {
    Node node = new Node();
    node.randomize(center, radius, is3D);
    return node;
  }

  // PRECISION

  /**
   * Sets the {@link #pickingThreshold()}.
   *
   * @see #pickingThreshold()
   * @see #setHighlighting(float)
   */
  public void setPickingThreshold(float threshold) {
    _threshold = threshold;
  }

  /**
   * Returns the node picking threshold. Set it with {@link #setPickingThreshold(float)}.
   * <p>
   * Picking a node is done with ray casting against a screen-space shape defined according
   * to a {@link #pickingThreshold()} as follows:
   * <ul>
   * <li>The projected pixels of the node visual representation (see {@link #graphics(Object)}
   * and {@link #setShape(Object)}). Set it with {@code threshold = 0}.</li>
   * <li>A node bounding box whose length is defined as percentage of the graph diameter
   * (see {@link Graph#radius()}). Set it with {@code threshold in [0..1]}.</li>
   * <li>A squared 'bullseye' of a fixed pixels length. Set it with {@code threshold > 1}.</li>
   * <li>A node bounding sphere whose length is defined as percentage of the graph diameter
   * (see {@link Graph#radius()}). Set it with {@code threshold in [-1..0]}.</li>
   * <li>A circled 'bullseye' of a fixed pixels length. Set it with {@code threshold < -1}.</li>
   * </ul>
   * Default picking precision is defined with {@code threshold = 0.2}.
   *
   * @see #setPickingThreshold(float)
   * @see #highlighting()
   */
  public float pickingThreshold() {
    return _threshold;
  }

  // CONSTRAINT

  /**
   * Returns the current {@link Constraint} applied to the node.
   * <p>
   * A {@code null} value (default) means that no constraint is used to filter the node
   * translation and rotation.
   * <p>
   * See the Constraint class documentation for details.
   */
  public Constraint constraint() {
    return _constraint;
  }

  /**
   * Sets the {@link #constraint()} attached to the node.
   * <p>
   * A {@code null} value means set no constraint (also reset it if there was one).
   */
  public void setConstraint(Constraint constraint) {
    _constraint = constraint;
  }

  /**
   * Same as {@code setConstraint(null)}.
   *
   * @see #setConstraint(Constraint)
   * @see #resetReference()
   */
  public void resetConstraint() {
    setConstraint(null);
  }

  // TRANSLATION

  /**
   * Returns the node translation, defined with respect to the {@link #reference()}.
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
   * Sets the {@link #translation()} of the node, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
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
   * Translates the node according to {@code vector}, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * If there's a {@link #constraint()} it is satisfied. Hence the translation actually
   * applied to the node may differ from {@code vector} (since it can be filtered by the
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
   * Returns the node position defined in the world coordinate system.
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
   * Sets the {@link #position()} to that of {@code node}.
   *
   * @see #setPosition(Vector)
   * @see #set(Node)
   */
  public void setPosition(Node node) {
    if (node == null)
      node = new Node();
    setPosition(node.position());
  }

  /**
   * Sets the node {@link #position()}, defined in the world coordinate system.
   * <p>
   * Use {@link #setTranslation(Vector)} to define the local node translation (with respect
   * to the {@link #reference()}).
   * <p>
   * Note that the potential {@link #constraint()} of the node is taken into account, i.e.,
   * to bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
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
   * Returns the node rotation, defined with respect to the {@link #reference()}
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
   * Sets the node {@link #rotation()}, locally defined with respect to the
   * {@link #reference()}. Use {@link #setOrientation(Quaternion)} to define the
   * world coordinates {@link #orientation()}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
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
   * Rotates the node by {@code quaternion} (defined in the node coordinate system):
   * {@code rotation().compose(quaternion)}.
   * <p>
   * Note that if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
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
   * Rotates the node by the {@code quaternion} whose axis (see {@link Quaternion#axis()})
   * passes through {@code point}. The {@code quaternion} {@link Quaternion#axis()} is
   * defined in the node coordinate system, while {@code point} is defined in the world
   * coordinate system).
   * <p>
   * Note: if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
   *
   * @see #setConstraint(Constraint)
   */
  protected void _orbit(Quaternion quaternion, Vector center) {
    if (constraint() != null)
      quaternion = constraint().constrainRotation(quaternion, this);
    this.rotation().compose(quaternion);
    this.rotation().normalize(); // Prevents numerical drift

    // Original in nodes-0.1.x and proscene:
    //Vector vector = Vector.add(center, (new Quaternion(orientation().rotate(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), center)));
    // TODO test node hierarchy, we are using worldDisplacement instead of orientation().rotate
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
   * Same as {@code orbit(new Quaternion(axis, angle), node)}.
   *
   * @see #orbit(Quaternion, Node)
   */
  public void orbit(Vector axis, float angle, Node node) {
    orbit(new Quaternion(axis, angle), node);
  }

  /**
   * Same as {@code orbit(quaternion, null)}.
   *
   * @see #orbit(Quaternion, Node)
   */
  public void orbit(Quaternion quaternion) {
    orbit(quaternion, null);
  }

  /**
   * Rotates this node around {@code node} (which may be null for the world coordinate system)
   * according to {@code quaternion}.
   * <p>
   * The {@code quaternion} axes (see {@link Quaternion#axis()}) is defined in the {@code node}
   * coordinate system.
   * <p>
   * Note: if there's a {@link #constraint()} it is satisfied, i.e., to
   * bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
   */
  public void orbit(Quaternion quaternion, Node node) {
    Quaternion localQuaternion = new Quaternion(displacement(quaternion.axis(), node), quaternion.angle());
    _orbit(localQuaternion, node == null ? new Vector() : node.position());

    // Note that the 'easy way' to do it (not relying on the _orbit() method)
    // by-passes the node constraint (kept for the curious):
    /*
    Node reference = node == null ? new Node() : node.detach();
    Node copy = new Node(reference);
    copy.set(this);
    reference.rotate(quaternion);
    set(copy);
    // */
  }

  // ORIENTATION

  /**
   * Returns the orientation of the node, defined in the world coordinate system.
   *
   * @see #position()
   * @see #magnitude()
   * @see #setOrientation(Quaternion)
   * @see #rotation()
   */
  public Quaternion orientation() {
    Quaternion quaternion = rotation().get();
    Node reference = reference();
    while (reference != null) {
      quaternion = Quaternion.compose(reference.rotation(), quaternion);
      reference = reference.reference();
    }
    return quaternion;
  }

  /**
   * Sets the {@link #orientation()} to that of {@code node}.
   *
   * @see #setOrientation(Quaternion)
   * @see #set(Node)
   */
  public void setOrientation(Node node) {
    if (node == null)
      node = new Node();
    setOrientation(node.orientation());
  }

  /**
   * Sets the {@link #orientation()} of the node, defined in the world coordinate system.
   * <p>
   * Use {@link #setRotation(Quaternion)} to define the local node rotation (with respect
   * to the {@link #reference()}).
   * <p>
   * Note that the potential {@link #constraint()} of the node is taken into account, i.e.,
   * to bypass a node constraint simply reset it (see {@link #setConstraint(Constraint)}).
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
   * Returns the node scaling, defined with respect to the {@link #reference()}.
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
   * Sets the {@link #scaling()} of the node, locally defined with respect to the
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
   * Scales the node according to {@code scaling}, locally defined with respect to the
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
   * Returns the magnitude of the node, defined in the world coordinate system.
   * <p>
   * Note that the magnitude is used to compute the node
   * {@link #projection(Graph.Type, float, float, float, float, boolean)} which is useful to render a
   * scene from the node point-of-view.
   *
   * @see #orientation()
   * @see #position()
   * @see #setPosition(Vector)
   * @see #translation()
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   */
  public float magnitude() {
    if (reference() != null)
      return reference().magnitude() * scaling();
    else
      return scaling();
  }

  /**
   * Sets the {@link #magnitude()} to that of {@code node}.
   *
   * @see #setMagnitude(float)
   * @see #set(Node)
   */
  public void setMagnitude(Node node) {
    if (node == null)
      node = new Node();
    setMagnitude(node.magnitude());
  }

  /**
   * Sets the {@link #magnitude()} of the node, defined in the world coordinate system.
   * <p>
   * Use {@link #setScaling(float)} to define the local node scaling (with respect to the
   * {@link #reference()}).
   */
  public void setMagnitude(float magnitude) {
    Node reference = reference();
    if (reference != null)
      setScaling(magnitude / reference.magnitude());
    else
      setScaling(magnitude);
  }

  // ALIGNMENT

  /**
   * Same as {@code align(null)}.
   *
   * @see #align(Node)
   */
  public void align() {
    align(null);
  }

  /**
   * Convenience function that simply calls {@code align(false, 0.85f, node)}
   *
   * @see #align(boolean, float, Node)
   */
  public void align(Node node) {
    align(false, 0.85f, node);
  }

  /**
   * Same as {@code align(move, null)}.
   *
   * @see #align(boolean, Node)
   */
  public void align(boolean move) {
    align(move, null);
  }

  /**
   * Convenience function that simply calls {@code align(move, 0.85f, node)}.
   *
   * @see #align(boolean, float, Node)
   */
  public void align(boolean move, Node node) {
    align(move, 0.85f, node);
  }

  /**
   * Same as {@code align(threshold, null)}.
   *
   * @see #align(boolean, Node)
   */
  public void align(float threshold) {
    align(threshold, null);
  }

  /**
   * Convenience function that simply calls {@code align(false, threshold, node)}.
   *
   * @see #align(boolean, float, Node)
   */
  public void align(float threshold, Node node) {
    align(false, threshold, node);
  }

  /**
   * Same as {@code align(move, threshold, null)}.
   *
   * @see #align(boolean, float, Node)
   */
  public void align(boolean move, float threshold) {
    align(move, threshold, null);
  }

  /**
   * Aligns the node with {@code node}, so that two of their axis are parallel.
   * <p>
   * If one of the X, Y and Z axis of the Node is almost parallel to any of the X, Y, or
   * Z axis of {@code node}, the Node is rotated so that these two axis actually become
   * parallel.
   * <p>
   * If, after this first rotation, two other axis are also almost parallel, a second
   * alignment is performed. The two nodes then have identical orientations, up to 90
   * degrees rotations.
   * <p>
   * {@code threshold} measures how close two axis must be to be considered parallel. It
   * is compared with the absolute values of the dot product of the normalized axis.
   * <p>
   * When {@code move} is set to {@code true}, the Node {@link #position()} is also
   * affected by the alignment. The new Node {@link #position()} is such that the
   * {@code node} node position (computed with {@link #location(Vector)}, in the Node
   * coordinates system) does not change.
   * <p>
   * {@code node} may be {@code null} and then represents the world coordinate system
   * (same convention than for the {@link #reference()}).
   */
  public void align(boolean move, float threshold, Node node) {
    Vector[][] directions = new Vector[2][3];

    for (int d = 0; d < 3; ++d) {
      Vector dir = new Vector((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
      if (node != null)
        directions[0][d] = node.orientation().rotate(dir);
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
    Node old = detach(); // correct line
    // Vnode old = this.get();// this call the get overloaded method and
    // hence add the node to the mouse _grabber

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
      if (node != null)
        center = node.position();

      vector = Vector.subtract(center, worldDisplacement(old.location(center)));
      vector.subtract(translation());
      translate(vector);
    }
  }


  /**
   * Translates the node so that its {@link #position()} lies on the line defined by
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
   * Rotates the node so that its {@link #xAxis()} becomes {@code axis} defined in the
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
   * Rotates the node so that its {@link #yAxis()} becomes {@code axis} defined in the
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
   * Rotates the node so that its {@link #zAxis()} becomes {@code axis} defined in the
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
   * Returns the x-axis of the node, represented as a normalized vector defined in the
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
   * Returns the y-axis of the node, represented as a normalized vector defined in the
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
   * Returns the z-axis of the node, represented as a normalized vector defined in the
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
   * Returns the local transformation matrix represented by the node.
   * <p>
   * This matrix only represents the local node transformation (i.e., with respect to the
   * {@link #reference()}). Use {@link #worldMatrix()} to get the full Node
   * transformation matrix (i.e., from the world to the Node coordinate system). These
   * two match when the {@link #reference()} is {@code null}.
   *
   * @see Graph#applyTransformation(Node)
   * @see #set(Node)
   * @see #worldMatrix()
   * @see #view()
   * @see #viewInverse()
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
   * Returns the global transformation matrix represented by the node which grants
   * direct access to it, without the need to traverse its ancestor hierarchy first
   * (as it is the case with {@link #matrix()}).
   * <p>
   * This matrix represents the global node transformation: the entire
   * {@link #reference()} hierarchy is taken into account to define the node
   * transformation from the world coordinate system. Use {@link #matrix()} to get the
   * local node transformation matrix (i.e. defined with respect to the
   * {@link #reference()}). These two match when the {@link #reference()} is
   * {@code null}.
   *
   * @see Graph#applyWorldTransformation(Node)
   * @see #set(Node)
   * @see #matrix()
   * @see #view()
   * @see #viewInverse()
   */
  public Matrix worldMatrix() {
    if (reference() != null)
      return new Node(position(), orientation(), magnitude()).matrix();
    else
      return matrix();
  }

  /**
   * Returns the inverse of the matrix associated with the node position and orientation that
   * is to be used when the node represents an eye. This matrix matches the inverted of the
   * {@link #worldMatrix()} when {@link #scaling()} is {@code 1}.
   * <p>
   * The view matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using a projection matrix.
   *
   * @see Matrix#view(Vector, Quaternion)
   * @see #viewInverse()
   * @see #matrix()
   * @see #worldMatrix()
   * @see #set(Node)
   * @see #set(Node)
   */
  public Matrix view() {
    return Matrix.view(position(), orientation());
  }

  /**
   * Returns the inverse of the {@link #view()} matrix. This matrix matches the
   * {@link #worldMatrix()} when {@link #magnitude()} is {@code 1}.
   * <p>
   * This matrix converts from the eye to world space.
   *
   * @see #view()
   * @see #matrix()
   * @see #worldMatrix()
   * @see #set(Node)
   * @see #set(Node)
   */
  public Matrix viewInverse() {
    return new Node(position(), orientation(), 1).matrix();
  }

  /**
   * Sets the node from a {@link #matrix()} representation: rotation and scaling in the upper
   * left 3x3 matrix and translation on the last column.
   * <p>
   * Hence, if your openGL code fragment looks like:
   * <p>
   * {@code float [] m = new float [16]; m[0]=...;} <br>
   * {@code gl.glMultMatrixf(m);} <br>
   * <p>
   * It is equivalent to write:
   * <p>
   * {@code Node node = new Node();} <br>
   * {@code node.fromMatrix(m);} <br>
   * {@code pushMatrix();} <br>
   * {@code graph.applyTransformation(node);} <br>
   * {@code // You are in the local node coordinate system.} <br>
   * {@code popMatrix();} <br>
   * <p>
   * Which allows to apply local transformations to the {@code node} while using geometry
   * data from other node instances when necessary (see {@link #location(Vector, Node)} and
   * {@link #displacement(Vector, Node)}).
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
   * Same as {@code return projection(type, width, height, zNear, zFar, graph().isLeftHanded())}. To use it, node must me
   * attached to a graph.
   *
   * @see #isDetached()
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   */
  public Matrix projection(Graph.Type type, float width, float height, float zNear, float zFar) {
    if (!isDetached())
      return projection(type, width, height, zNear, zFar, graph().isLeftHanded());
    else
      throw new RuntimeException("To call node.projection(type, width, height, zNear, zFar) the node should be attached to a graph." +
          "Use node.projection(type, width, height, zNear, zFar, leftHanded), instead");
  }

  /**
   * Returns either {@code perspective(width / height, zNear, zFar, lefTHanded)} if
   * the {@link Graph.Type} is {@link Graph.Type#PERSPECTIVE} or
   * {@code orthographic(width, height, zNear, zFar, lefTHanded)}, if the
   * the {@link Graph.Type} is {@link Graph.Type#ORTHOGRAPHIC} or {@link Graph.Type#TWO_D}.
   * In both cases it uses the node {@link #magnitude()}.
   * <p>
   * Override this method to set a {@link Graph.Type#CUSTOM} projection.
   *
   * @see #perspective(float, float, float, boolean)
   * @see #orthographic(float, float, float, float, boolean)
   * @see #magnitude()
   */
  public Matrix projection(Graph.Type type, float width, float height, float zNear, float zFar, boolean leftHanded) {
    if (type == Graph.Type.PERSPECTIVE)
      return perspective(width / height, zNear, zFar, leftHanded);
    else
      return orthographic(width, height, zNear, zFar, leftHanded);
  }

  /**
   * Same as {@code return orthographic(width, height, zNear, zFar, graph().isLeftHanded())}. To use it, node must me
   * attached to a graph.
   *
   * @see #isDetached()
   * @see #orthographic(float, float, float, float, boolean)
   */
  public Matrix orthographic(float width, float height, float zNear, float zFar) {
    if (!isDetached())
      return orthographic(width, height, zNear, zFar, graph().isLeftHanded());
    else
      throw new RuntimeException("To call node.orthographic(width, height, zNear, zFar) the node should be attached to a graph." +
          "Use node.orthographic(float width, float height, float zNear, float zFar, boolean leftHanded), instead");
  }

  /**
   * Same as {@code return Matrix.orthographic(width * magnitude(), (leftHanded ? -height : height) * magnitude(), zNear, zFar}.
   * <p>
   * Note that to compute the orthographic matrix the node {@link #magnitude()} scales the viewing volume width and height.
   *
   * @see Matrix#orthographic(float, float, float, float)
   * @see #perspective(float, float, float, boolean)
   * @see #magnitude()
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   */
  public Matrix orthographic(float width, float height, float zNear, float zFar, boolean leftHanded) {
    return Matrix.orthographic(width * magnitude(), (leftHanded ? -height : height) * magnitude(), zNear, zFar);
  }

  /**
   * Same as {@code return perspective(aspectRatio, zNear, zFar, graph().isLeftHanded())}. To use it, node must me
   * attached to a graph.
   *
   * @see #isDetached()
   * @see #perspective(float, float, float, boolean)
   */
  public Matrix perspective(float aspectRatio, float zNear, float zFar) {
    if (!isDetached())
      return perspective(aspectRatio, zNear, zFar, graph().isLeftHanded());
    else
      throw new RuntimeException("To call node.perspective(float aspectRatio, float zNear, float zFar) the node should be attached to a graph." +
          "Use node.perspective(aspectRatio, zNear, zFar, leftHanded), instead");
  }

  /**
   * Same as {@code return Matrix.perspective(leftHanded ? -magnitude() : magnitude(), aspectRatio, zNear, zFar)}.
   * <p>
   * Note that to compute the perspective matrix the node {@link #magnitude()} is taken as: {@code tan(fov / 2)},
   * where {@code fov} stands for the frustum field-of-view.
   *
   * @see Matrix#perspective(float, float, float, float)
   * @see #orthographic(float, float, float, float, boolean)
   * @see #magnitude()
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   */
  public Matrix perspective(float aspectRatio, float zNear, float zFar, boolean leftHanded) {
    return Matrix.perspective(leftHanded ? -magnitude() : magnitude(), aspectRatio, zNear, zFar);
  }

  /**
   * Same as {@code return Matrix.multiply(projection(type, width, height, zNear, zFar), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float)
   * @see #view()
   */
  public Matrix projectionView(Graph.Type type, float width, float height, float zNear, float zFar) {
    return Matrix.multiply(projection(type, width, height, zNear, zFar), view());
  }

  /**
   * Same as {@code return Matrix.multiply(projection(type, width, height, zNear, zFar, leftHanded), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   * @see #view()
   */
  public Matrix projectionView(Graph.Type type, float width, float height, float zNear, float zFar, boolean leftHanded) {
    return Matrix.multiply(projection(type, width, height, zNear, zFar, leftHanded), view());
  }

  /**
   * Same as {@code return Matrix.multiply(projection(Graph.Type.ORTHOGRAPHIC, width, height, zNear, zFar), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float)
   * @see #view()
   */
  public Matrix orthographicView(float width, float height, float zNear, float zFar) {
    return Matrix.multiply(projection(Graph.Type.ORTHOGRAPHIC, width, height, zNear, zFar), view());
  }

  /**
   * Same as {code return Matrix.multiply(projection(Graph.Type.ORTHOGRAPHIC, width, height, zNear, zFar, leftHanded), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   * @see #view()
   */
  public Matrix orthographicView(float width, float height, float zNear, float zFar, boolean leftHanded) {
    return Matrix.multiply(projection(Graph.Type.ORTHOGRAPHIC, width, height, zNear, zFar, leftHanded), view());
  }

  /**
   * Same as {@code return Matrix.multiply(projection(Graph.Type.PERSPECTIVE, width, height, zNear, zFar), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float)
   * @see #view()
   */
  public Matrix perspectiveView(float width, float height, float zNear, float zFar) {
    return Matrix.multiply(projection(Graph.Type.PERSPECTIVE, width, height, zNear, zFar), view());
  }

  /**
   * Same as {@code return Matrix.multiply(projection(Graph.Type.PERSPECTIVE, width, height, zNear, zFar, leftHanded), view())}.
   *
   * @see #projection(Graph.Type, float, float, float, float, boolean)
   * @see #view()
   */
  public Matrix perspectiveView(float width, float height, float zNear, float zFar, boolean leftHanded) {
    return Matrix.multiply(projection(Graph.Type.PERSPECTIVE, width, height, zNear, zFar, leftHanded), view());
  }

  /**
   * Sets the node from {@link #worldMatrix()} representation: orientation and magnitude
   * in the upper left 3x3 matrix and position on the last column.
   * <p>
   * Hence, if your openGL code fragment looks like:
   * <p>
   * {@code float [] m = new float [16]; m[0]=...;} <br>
   * {@code gl.glMultMatrixf(m);} <br>
   * <p>
   * It is equivalent to write:
   * <p>
   * {@code Node node = new Node();} <br>
   * {@code node.fromWorldMatrix(m);} <br>
   * {@code graph.applyWorldTransformation(node);} <br>
   * {@code // You are in the local node coordinate system.} <br>
   * <p>
   * Which allows to apply local transformations to the {@code node} while using geometry
   * data from other node instances when necessary (see {@link #location(Vector, Node)} and
   * {@link #displacement(Vector, Node)}).
   *
   * @see #fromMatrix(Matrix)
   * @see #worldMatrix()
   */
  public void fromWorldMatrix(Matrix matrix) {
    if (matrix._matrix[15] == 0) {
      System.out.println("Doing nothing: model.mat[15] should be non-zero!");
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
   * Returns a node representing the inverse of this node space transformation.
   * <p>
   * The new node {@link #rotation()} is the
   * {@link Quaternion#inverse()} of the original rotation. Its
   * {@link #translation()} is the negated inverse rotated image of the original
   * translation. Its {@link #scaling()} is 1 / original scaling.
   * <p>
   * If a node is considered as a space rigid transformation, i.e., translation and
   * rotation, but no scaling (scaling=1), the inverse() node performs the inverse
   * transformation.
   * <p>
   * Only the local node transformation (i.e., defined with respect to the
   * {@link #reference()}) is inverted. Use {@link #worldInverse()} for a global
   * inverse.
   * <p>
   * The resulting node has the same {@link #reference()} as the this node and a
   * {@code null} {@link #constraint()}.
   *
   * @see #worldInverse()
   */
  public Node inverse() {
    Node node = new Node(Vector.multiply(rotation().inverseRotate(translation()), -1), rotation().inverse(), 1 / scaling());
    node.setReference(reference());
    return node;
  }

  /**
   * Returns the {@link #inverse()} of the node world transformation.
   * <p>
   * The {@link #orientation()} of the new node is the
   * {@link Quaternion#inverse()} of the original orientation. Its
   * {@link #position()} is the negated and inverse rotated image of the original
   * position. The {@link #magnitude()} is the original magnitude multiplicative
   * inverse.
   * <p>
   * The result node has a {@code null} {@link #reference()} and a {@code null}
   * {@link #constraint()}.
   * <p>
   * Use {@link #inverse()} for a local (i.e., with respect to {@link #reference()})
   * transformation inverse.
   *
   * @see #inverse()
   */
  public Node worldInverse() {
    return new Node(Vector.multiply(orientation().inverseRotate(position()), -1), orientation().inverse(), 1 / magnitude());
  }

  // VECTOR CONVERSION

  /**
   * Converts {@code vector} displacement from world to this node.
   * Same as {@code return displacement(vector, null)}.
   * {@link #location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Vector, Node)
   * @see #location(Vector)
   */
  public Vector displacement(Vector vector) {
    return displacement(vector, null);
  }

  /**
   * Converts {@code vector} displacement from {@code node} to this node.
   * Use {@code node.displacement(vector, this)} to perform the inverse transformation.
   * {@link #location(Vector, Node)} converts locations instead of displacements.
   *
   * @see #displacement(Vector)
   * @see #worldDisplacement(Vector)
   */
  public Vector displacement(Vector vector, Node node) {
    return this == node ? vector : _displacement(reference() != null ? reference().displacement(vector, node) : node == null ? vector : node.worldDisplacement(vector));
  }

  /**
   * Converts {@code vector} displacement from this node to world.
   * {@link #displacement(Vector)} performs the inverse transformation.
   * {@link #worldLocation(Vector)} converts locations instead of displacements.
   *
   * @see #location(Vector)
   * @see #displacement(Vector, Node)
   */
  public Vector worldDisplacement(Vector vector) {
    Node node = this;
    Vector result = vector;
    while (node != null) {
      result = node._referenceDisplacement(result);
      node = node.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} displacement from {@link #reference()} to this node.
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
   * Converts {@code vector} displacement from this node to {@link #reference()}.
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
   * Converts the {@code node} origin location to this node.
   * Same as {@code return location(new Vector(), node)}.
   * {@link #displacement(Vector, Node)} converts displacements instead of locations.
   *
   * @see #location(Vector)
   * @see #location(Vector, Node)
   * @see #displacement(Vector, Node)
   */
  public Vector location(Node node) {
    return location(new Vector(), node);
  }

  /**
   * Converts {@code vector} location from world to this node.
   * Same as {@code return location(vector, null)}.
   * {@link #displacement(Vector)} converts displacements instead of locations.
   *
   * @see #location(Node)
   * @see #location(Vector, Node)
   * @see #displacement(Vector)
   */
  public Vector location(Vector vector) {
    return location(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code node} to this node.
   * Use {@code node.location(vector, this)} to perform the inverse transformation.
   * {@link #displacement(Vector, Node)} converts displacements instead of locations.
   *
   * @see #location(Node)
   * @see #location(Vector)
   * @see #worldLocation(Vector)
   */
  public Vector location(Vector vector, Node node) {
    return this == node ? vector : _location(reference() != null ? reference().location(vector, node) : node == null ? vector : node.worldLocation(vector));
  }

  /**
   * Converts {@code vector} location from this node to world.
   * {@link #location(Vector)} performs the inverse transformation.
   * {@link #worldDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #displacement(Vector)
   * @see #location(Vector, Node)
   */
  public Vector worldLocation(Vector vector) {
    Node node = this;
    Vector result = vector;
    while (node != null) {
      result = node._referenceLocation(result);
      node = node.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} location from {@link #reference()} to this node.
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
   * Converts {@code vector} location from this node to {@link #reference()}.
   * <p>
   * {@link #_location(Vector)} performs the inverse transformation.
   * {@link #_referenceDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #worldLocation(Vector)
   */
  protected Vector _referenceLocation(Vector vector) {
    return Vector.add(rotation().rotate(Vector.multiply(vector, scaling())), translation());
  }

  // Attached nodes

  /**
   * Returns the {@code graph} this node is attached to. Always returns {@code false} if
   * the node {@link #isDetached()}.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Returns {@code true} if tracking is enabled.
   *
   * @see #enableTracking(boolean)
   * @see #enableTracking()
   * @see #disableTracking()
   */
  public boolean isTrackingEnabled() {
    return _tracking;
  }

  /**
   * Same as {@code enableTracking(false)}.
   *
   * @see #isTrackingEnabled()
   * @see #enableTracking()
   * @see #enableTracking(boolean)
   */
  public void disableTracking() {
    enableTracking(false);
  }

  /**
   * Same as {@code enableTracking(true)}.
   *
   * @see #isTrackingEnabled()
   * @see #enableTracking(boolean)
   * @see #disableTracking()
   */
  public void enableTracking() {
    enableTracking(true);
  }

  /**
   * Enables node tracking according to {@code flag}. When tracking is disabled {@link Graph#tracks(Node, int, int)}
   * returns {@code false}, {@link Graph#tag(String, Node)} does nothing while
   * {@link Graph#track(String, int, int)} and {@link Graph#cast(int, int)} would bypass the node.
   *
   * @see #isTrackingEnabled()
   * @see #enableTracking()
   * @see #disableTracking()
   */
  public void enableTracking(boolean flag) {
    _tracking = flag;
  }

  /**
   * Same as {@code return isDetached() ? false : isTracked(graph())}. Use it if the node is
   * attached to a {@link #graph()}. Use {@link #isTracked(Graph)} if the node {@link #isDetached()}.
   *
   * @see #isDetached()
   * @see #isTracked(Graph)
   */
  public boolean isTracked() {
    return !isDetached() && isTracked(graph());
  }

  /**
   * Returns {@code true} if the {@code node} is being tracked by the {@code graph} at least once
   * and {@code false} otherwise.
   *
   * @see Graph#isTagged(String, Node)
   * @see Graph#isTagged(Node)
   */
  public boolean isTracked(Graph graph) {
    return graph._hids.containsValue(this);
  }

  /**
   * Parse {@code gesture} params. Useful to implement the node as an for application control.
   * Default implementation is empty. , i.e., it is meant to be implemented by derived classes.
   */
  public void interact(Object... gesture) {
  }

  /**
   * This method is called on each node of the graph hierarchy by the {@link Graph#render()}
   * algorithm to visit it. Default implementation is empty, i.e., it is meant to be implemented
   * by derived classes.
   * <p>
   * Hierarchical culling, i.e., culling of the node and its children, should be decided here.
   * Set the culling flag with {@link #cull(boolean)} according to your culling condition:
   *
   * <pre>
   * {@code
   * node = new Node(graph) {
   *   @Override
   *   public void visit() {
   *     // Hierarchical culling is optional and disabled by default. When the cullingCondition
   *     // (which should be implemented by you) is true, scene.traverse() will prune the branch
   *     // at the node
   *     cull(cullingCondition);
   *   }
   * }
   * }
   * </pre>
   *
   * @see Graph#render()
   * @see Graph#render()
   * @see #cull(boolean)
   * @see #isCulled()
   * @see Graph#draw(Object, Node)
   */
  public void visit() {
  }

  /**
   * Same as {@code cull(true)}. Only meaningful if the node is attached to
   * a {@code graph}.
   *
   * @see #cull(boolean)
   * @see #isCulled()
   */
  public void cull() {
    cull(true);
  }

  /**
   * Enables or disables {@link #visit()} of this node and its children during
   * {@link Graph#render()}. Culling should be decided within {@link #visit()}.
   * Only meaningful if the node is attached to a {@code graph}.
   *
   * @see #isCulled()
   */
  public void cull(boolean cull) {
    if (isDetached())
      System.out.println("Warning: culling a detached node does nothing");
    _culled = cull;
  }

  /**
   * Returns whether or not the node culled or not. Culled nodes (and their children)
   * will not be visited by the {@link Graph#render()} algorithm. Always returns
   * {@code false} if the node {@link #isDetached()}.
   *
   * @see #cull(boolean)
   */
  public boolean isCulled() {
    return !isDetached() && _culled;
  }

  /**
   * Sets the node {@link #highlighting()} which should be a value in  {@code [0..1]}.
   * Default value is {@code 0.15}.
   *
   * @see #highlighting()
   * @see #setPickingThreshold(float)
   */
  public void setHighlighting(float highlighting) {
    _highlight = highlighting;
  }

  /**
   * Returns the highlighting magnitude use to scale the node when it's tracked.
   *
   * @see #setHighlighting(float)
   * @see #pickingThreshold()
   */
  public float highlighting() {
    return _highlight;
  }

  // JS version of the rendering methods

  /**
   * Override this method to set an immediate mode graphics procedure on {@code context}.
   *
   * @see #setShape(Object)
   */
  public void graphics(Object context) {
  }

  /**
   * Same as {@code setShape(null)}.
   *
   * @see #setShape(Object)
   */
  public void resetShape() {
    setShape(null);
  }

  /**
   * Sets the node retained mode shape.
   *
   * @see #graphics(Object)
   * @see #resetShape()
   */
  public void setShape(Object shape) {
    _shape = shape;
  }

  /**
   * Returns the node retained mode shape. Maybe null.
   *
   * @see #resetShape()
   * @see #shape()
   * @see #graphics(Object)
   */
  public Object shape() {
    return _shape;
  }


  // Java version of the immediate mode rendering methods

  /**
   * Override this method to set an immediate mode graphics procedure on the Processing
   * {@code PGraphics}. Return {@code true} if succeeded and {@code false} otherwise.
   */
  public void graphics(processing.core.PGraphics pGraphics) {
  }
}
