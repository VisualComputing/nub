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

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
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
 * {@link Graph#applyTransformation(Frame)} (or {@link Graph#applyWorldTransformation(Frame)}), as shown below:
 * <p>
 * {@code // Builds a node located at (0,0,0) with an identity orientation (node and
 * world axes match)} <br>
 * {@code Node node = new Node(graph);} <br>
 * {@code graph.pushModelView();} <br>
 * {@code graph.applyWorldTransformation(node); //same as graph.applyModelView(node.matrix());} <br>
 * {@code // Draw your object here, in the local coordinate system.} <br>
 * {@code graph.popModelView();} <br>
 * <p>
 * Alternatively, the node geometry transformation may be automatically handled by the graph
 * traversal algorithm (see {@link frames.core.Graph#traverse()}), provided
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
 * (culling of the node and its descendants by the {@link frames.core.Graph#traverse()}
 * algorithm). The {@link #isCulled()} flag is {@code false} by default, see
 * {@link #cull(boolean)}.
 * <p>
 * A node may also be defined as the {@link Graph#eye()} (see {@link Graph#setEye(Frame)}).
 * Some user gestures are then interpreted in a negated way,
 * respect to non-eye nodes. For instance, with a move-to-the-right user gesture the
 * {@link Graph#eye()} has to go to the <i>left</i>, so that the scene seems to move
 * to the right.
 */
public class Node extends Frame {
  protected Graph _graph;
  protected List<Node> _children;
  protected boolean _culled;
  protected boolean _tracking;

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
    setReference(reference());// _restorePath is completely needed here
    enableTracking(true);
  }

  protected Node(Graph graph, Node node) {
    super(node);
    _graph = graph;
    this._culled = node._culled;
    this._children = new ArrayList<Node>();
    if (this.graph() == node.graph()) {
      this.setReference(reference());// _restorePath
    }
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

  protected boolean _addChild(Node node) {
    if (node == null)
      return false;
    if (_hasChild(node))
      return false;
    return children().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected boolean _removeChild(Node node) {
    boolean result = false;
    Iterator<Node> it = children().iterator();
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
    for (Node child : children())
      if (child == node)
        return true;
    return false;
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
   * Enables or disables {@link #visit()} of this frame and its children during
   * {@link Graph#traverse()}. Culling should be decided within {@link #visit()}.
   *
   * @see #isCulled()
   */
  public void cull(boolean cull) {
    _culled = cull;
  }

  /**
   * Returns whether or not the frame culled or not. Culled frames (and their children)
   * will not be visited by the {@link Graph#traverse()} algoruthm.
   *
   * @see #cull(boolean)
   */
  public boolean isCulled() {
    return _culled;
  }
}