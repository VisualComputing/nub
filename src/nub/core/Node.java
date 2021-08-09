/***************************************************************************************
 * nub
 * Copyright (c) 2019-2021 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core;

import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.timing.Task;
import nub.timing.TimingHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A node encapsulates a 2D or 3D coordinate system, represented by a {@link #position()}, an
 * {@link #orientation()} and {@link #magnitude()} defined respect to a {@link #reference()} node.
 * The order of these transformations is important: the node is first translated, then rotated
 * around the new translated origin and then scaled.
 * <h2>Hierarchy of nodes</h2>
 * The default {@link #reference()} node is the world coordinate system
 * (represented by a {@code null}). If you {@link #setReference(Node)} to a different node,
 * you must then differentiate:
 * <ul>
 * <li>The <b>local</b> {@link #position()}, {@link #orientation()} and {@link #magnitude()},
 * defined with respect to the {@link #reference()} which represents an angle preserving
 * transformation of space.</li>
 * <li>The <b>global</b> {@link #worldPosition()}, {@link #worldOrientation()} and
 * {@link #worldMagnitude()}, always defined with respect to the world coordinate system.</li>
 * </ul>
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
 * {@code applyMatrix(node.matrix());} <br>
 * {@code // Draw your object here, in the local node coordinate system.} <br>
 * {@code popMatrix();} <br>
 * <p>
 * Use {@link #view()} when rendering the scene from the node point-of-view. Note that
 * this method is automatically called by the graph, see {@link Graph#render(Node)}.
 * <p>
 * To transform a point from one node to another use {@link #location(Vector, Node)} and
 * {@link #worldLocation(Vector)}. To transform a vector (such as a normal) use
 * {@link #displacement(Vector, Node)} and {@link #worldDisplacement(Vector)}.
 * To transform a quaternion use {@link #displacement(Quaternion, Node)} and
 * {@link #worldDisplacement(Quaternion)}. To transform a scalar use
 * {@link #displacement(float, Node)} and {@link #worldDisplacement(float)}.
 * <p>
 * The methods {@link #translate(Vector, float)}, {@link #rotate(Quaternion, float)},
 * {@link #orbit(Vector, float, Vector, float)} and {@link #scale(float, float)}, locally apply
 * differential geometry transformations damped with a given {@code inertia}. Note that
 * when the inertial parameter is omitted its value is defaulted to 0.
 * <h2>Hierarchical traversals</h2>
 * Hierarchical traversals of the node hierarchy which automatically apply the local
 * node transformations described above may be achieved with {@link Graph#render()} and
 * {@link Graph#render(Node)}. Customize the rendering traversal with
 * {@link Graph#setVisit(Node, BiConsumer)} (see also {@link #cull} and {@link #bypass()}).
 * <h2>Visual hints</h2>
 * The node space visual representation may be configured using the following hints:
 * {@link #CAMERA}, {@link #AXES}, {@link #HUD}, {@link #BOUNDS},, {@link #SHAPE},
 * {@link #BULLSEYE}, and {@link #TORUS}.
 * <p>
 * See {@link #hint()}, {@link #configHint(int, Object...)} {@link #enableHint(int)},
 * {@link #enableHint(int, Object...)}, {@link #disableHint(int)}, {@link #toggleHint(int)}
 * and {@link #resetHint()}.
 * <h2>Ray casting</h2>
 * Set the node picking ray-casting mode with {@link #enablePicking(int)} (see also
 * {@link #picking()}).
 * <h2>Custom behavior</h2>
 * Implementing a custom behavior for node is a two step process:
 * <ul>
 * <li>Register a user gesture data parser, see {@link #setInteraction(BiConsumer)}
 * and {@link #setInteraction(Consumer)}.</li>
 * <li>Send gesture data to the node by calling {@link Graph#interact(Node, Object...)},
 * {@link Graph#interact(String, Object...)} or {@link Graph#interact(Object...)}.</li>
 * </ul>
 */
public class Node {
  /**
   * Returns whether or not this node matches other taking into account the {@link #position()},
   * {@link #orientation()} and {@link #magnitude()} node parameters, but not its {@link #reference()}.
   *
   * @param node node
   */
  public boolean matches(Node node) {
    if (node == null)
      node = Node.detach(new Vector(), new Quaternion(), 1);
    return position().matches(node.position()) && orientation().matches(node.orientation()) && magnitude() == node.magnitude();
  }

  protected Vector _position;
  protected float _magnitude;
  protected Quaternion _orientation;
  protected Node _reference;
  protected long _lastUpdate;

  // Tagging & Precision
  protected float _bullsEyeSize;

  public enum BullsEyeShape {
    SQUARE, CIRCLE
  }
  protected BullsEyeShape _bullsEyeShape;

  // ID
  protected static int _counter;
  protected int _id;

  // tree
  protected List<Node> _children;
  public boolean cull;
  public boolean tagging;

  // filter caches
  public Vector cacheTargetPosition, cacheTargetTranslation;
  public Quaternion cacheTargetOrientation, cacheTargetRotation;
  public float cacheTargetMagnitude, cacheTargetScaling;

  // Visual hints
  protected int _picking;
  protected int _mask;
  public final static int CAMERA = 1 << 0;
  public final static int AXES = Graph.AXES;
  public final static int HUD = Graph.HUD;
  public final static int SHAPE = Graph.SHAPE;
  public final static int BOUNDS = 1 << 4;
  public final static int BULLSEYE = 1 << 5;
  public final static int TORUS = 1 << 6;
  public final static int FILTER = 1 << 7;
  public final static int BONE = 1 << 8;
  protected float _highlight;
  // Bounds
  protected HashSet<Graph> _frustumGraphs;
  // torus
  protected int _torusColor;
  protected int _torusFaces;
  protected int _bullsEyeStroke;
  protected float _axesLength;
  protected int _cameraStroke;
  protected float _cameraLength;
  protected Consumer<processing.core.PGraphics> _imrHUD;
  protected processing.core.PShape _rmrHUD;
  // Rendering
  // Immediate mode rendering
  protected Consumer<processing.core.PGraphics> _imrShape;
  // Retained mode rendering
  // PShape is only available in Java
  protected processing.core.PShape _rmrShape;
  protected long _bypass = -1;

  //Object... gesture
  protected BiConsumer<Node, Object[]> _interact;

  // Tasks
  protected InertialTask _translationTask, _rotationTask, _orbitTask, _scalingTask;
  protected final float _scalingFactor = 800;

  /**
   * Same as {@code this(null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node() {
    this(null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(null, position, new Quaternion(), 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node(Vector position) {
    this(null, position, new Quaternion(), 1);
  }

  /**
   * Same as {@code this(null, position, orientation, 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node(Vector position, Quaternion orientation) {
    this(null, position, orientation, 1);
  }

  /**
   * Same as {@code this(reference, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node(Node reference) {
    this(reference, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, position, new Quaternion(), 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node(Node reference, Vector position) {
    this(reference, position, new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, position, orientation, 1)}.
   *
   * @see #Node(Node, Vector, Quaternion, float)
   */
  public Node(Node reference, Vector position, Quaternion orientation) {
    this(reference, position, orientation, 1);
  }

  /**
   * Creates a node with {@code reference} as {@link #reference()}, having {@code position},
   * {@code orientation} and {@code magnitude} as the {@link #position()}, {@link #orientation()}
   * and {@link #magnitude()}, respectively. The {@link #bullsEyeSize()} is set to {@code 0.2}
   * and the {@link #highlight()} hint magnitude to {@code 0.15}.
   */
  public Node(Node reference, Vector position, Quaternion orientation, float magnitude) {
    this(position, orientation, magnitude);
    _reference = reference;
    _restorePath(reference(), this);
    if (reference == null || reference.isReachable())
      _registerTasks();
    // hack
    Method method = null;
    try {
      method = this.getClass().getMethod("graphics", processing.core.PGraphics.class);
    } catch(NoSuchMethodException e) {
      System.out.println(e.toString()); //print exception object
    }
    if (!method.getDeclaringClass().equals(Node.class)) {
      setShape(this::graphics);
    }
  }

  /**
   * Internally used by both {@link #Node(Node, Vector, Quaternion, float)}
   * (attached nodes) and {@link #detach(Vector, Quaternion, float)} (detached nodes).
   */
  protected Node(Vector position, Quaternion orientation, float magnitude) {
    setPosition(position);
    setOrientation(orientation);
    setMagnitude(magnitude);
    enablePicking(CAMERA | AXES | HUD | SHAPE | BOUNDS | BULLSEYE | TORUS | FILTER | BONE);
    _id = ++_counter;
    // unlikely but theoretically possible
    if (_id == 16777216)
      throw new RuntimeException("Maximum node instances reached. Exiting now!");
    _bullsEyeSize = 30;
    _bullsEyeShape = BullsEyeShape.SQUARE;
    tagging = true;
    // hints
    _highlight = 0.15f;
    int min = 2, max = 20;
    _torusFaces = Graph.random.nextInt(max - min + 1) + min;
    min = 0;
    max = 255;
    float r = (float) Graph.random.nextInt(max - min + 1) + min;
    float g = (float) Graph.random.nextInt(max - min + 1) + min;
    float b = (float) Graph.random.nextInt(max - min + 1) + min;
    _torusColor = Graph._color(r, g, b);
    // cyan (color(0, 255, 255)) encoded as a processing int rgb color
    _bullsEyeStroke = -16711681;
    // magenta (color(255, 0, 255)) encoded as a processing int rgb color
    _cameraStroke = -65281;
    _children = new ArrayList<Node>();
    _frustumGraphs = new HashSet<Graph>();
    setInteraction(this::interact);
  }

  // From here only Java constructors

  /**
   * Same as {@code this(null, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, Consumer, Vector, Quaternion, float)
   */
  public Node(Consumer<processing.core.PGraphics> shape) {
    this(null, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(null, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, processing.core.PShape, Vector, Quaternion, float)
   */
  public Node(processing.core.PShape shape) {
    this(null, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, Consumer, Vector, Quaternion, float)
   */
  public Node(Node reference, Consumer<processing.core.PGraphics> shape) {
    this(reference, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as {@code this(reference, shape, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Node, processing.core.PShape, Vector, Quaternion, float)
   */
  public Node(Node reference, processing.core.PShape shape) {
    this(reference, shape, new Vector(), new Quaternion(), 1);
  }

  /**
   * Calls {@link #Node(Node, Vector, Quaternion, float)} and then {@link #setShape(Consumer)}.
   */
  public Node(Node reference, Consumer<processing.core.PGraphics> shape, Vector position, Quaternion orientation, float magnitude) {
    this(reference, position, orientation, magnitude);
    setShape(shape);
  }

  /**
   * Creates a node with {@code reference} as {@link #reference()} and {@code shape}, having {@code position},
   * {@code orientation} and {@code magnitude} as the {@link #position()}, {@link #orientation()}
   * and {@link #magnitude()}, respectively. The {@link #bullsEyeSize()} is set to
   * {@code 0} and the {@link #highlight()} hint to {@code 0.15}.
   */
  public Node(Node reference, processing.core.PShape shape, Vector position, Quaternion orientation, float magnitude) {
    this(reference, position, orientation, magnitude);
    setShape(shape);
  }

  /**
   * Same as {@code return detach(this)}.
   *
   * @see #detach(Node)
   * @see #copy()
   */
  public Node detach() {
    Node node = detach(this);
    node._torusFaces = this._torusFaces;
    node._torusColor = this._torusColor;
    node._bullsEyeStroke = this._bullsEyeStroke;
    node._cameraStroke = this._cameraStroke;
    node._axesLength = this._axesLength;
    return node;
  }

  /**
   * Same as {@code return Node.detach(node.position(), node.orientation(), node.magnitude())}.
   *
   * @see #detach(Vector, Quaternion, float)
   */
  public static Node detach(Node node) {
    return Node.detach(node.worldPosition(), node.worldOrientation(), node.worldMagnitude());
  }

  /**
   * Returns a detached node (i.e., a pruned non-reachable node from the graph, see {@link Graph#prune(Node)})
   * whose {@link #reference()} is {@code null} for the given params. Mostly used internally.
   */
  public static Node detach(Vector position, Quaternion orientation, float magnitude) {
    return new Node(position, orientation, magnitude);
  }

  /**
   * Returns the translation inertial task.
   * Useful if you need to customize the timing-task, e.g., to enable concurrency on it.
   */
  public Task translationInertialTask() {
    return _translationTask;
  }

  /**
   * Returns the rotation inertial task.
   * Useful if you need to customize the timing-task, e.g., to enable concurrency on it.
   */
  public Task rotationInertialTask() {
    return _rotationTask;
  }

  /**
   * Returns the orbit inertial task.
   * Useful if you need to customize the timing-task, e.g., to enable concurrency on it.
   */
  public Task orbitInertialTask() {
    return _orbitTask;
  }

  /**
   * Returns the scaling inertial task.
   * Useful if you need to customize the timing-task, e.g., to enable concurrency on it.
   */
  public Task scalingInertialTask() {
    return _scalingTask;
  }

  /**
   * Return this node components as a String.
   */
  @Override
  public String toString() {
    return "Position: " + worldPosition().toString() + " Orientation: " + worldOrientation().toString() + " Magnitude: " + Float.toString(worldMagnitude());
  }

  /**
   * Performs a deep copy of this node and returns it. Only the {@link #worldPosition()},
   * {@link #worldOrientation()} and {@link #worldMagnitude()} of the node are copied.
   *
   * @see #detach()
   */
  public Node copy() {
    Node node = new Node();
    node.set(this);
    if (!isHintEnabled(Node.SHAPE))
      node.disableHint(SHAPE);
    return node;
  }

  /**
   * Sets {@link #worldPosition()}, {@link #worldOrientation()}, {@link #worldMagnitude()},
   * {@link #hint()} and {@link #picking()} values from those of the {@code node}.
   * The node {@link #reference()} is not affected by this call.
   * <p>
   * After calling {@code set(node)} a call to {@code this.matches(node)} should
   * return {@code true}.
   *
   * @see #reset()
   * @see #worldMatrix()
   * @see #setWorldPosition(Node)
   * @see #setWorldOrientation(Node)
   * @see #setWorldMagnitude(Node)
   */
  public void set(Node node) {
    setWorldPosition(node);
    setWorldOrientation(node);
    setWorldMagnitude(node);
    _mask = node.hint();
    _picking = node.picking();
    setShape(node);
    setHUD(node);
    _bullsEyeSize = node._bullsEyeSize;
    _bullsEyeShape = node._bullsEyeShape;
    tagging = node.tagging;
    _highlight = node._highlight;
    _torusFaces = node._torusFaces;
    _torusColor = node._torusColor;
    _bullsEyeStroke = node._bullsEyeStroke;
    _cameraStroke = node._cameraStroke;
  }

  /**
   * Sets an identity node by resetting its {@link #position()}, {@link #orientation()}
   * and {@link #magnitude()}. The node {@link #reference()} is not affected by this call.
   * Call {@code set(null)} if you want to reset the global {@link #worldPosition()},
   * {@link #worldOrientation()} and {@link #worldMagnitude()} node parameters instead.
   *
   * @see #set(Node)
   */
  public void reset() {
    setPosition(new Vector());
    setOrientation(new Quaternion());
    setMagnitude(1);
  }

  // colorID

  /**
   * Returns the unique sequential node id assigned at instantiation time.
   * Used by {@link #colorID()} and {@link Graph#_displayBackHint(Node)}.
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
   * @return the last frame this node affine transformation ({@link #worldPosition()},
   * {@link #worldOrientation()} or {@link #worldMagnitude()}) or {@link #reference()} was updated.
   */
  public long lastUpdate() {
    return _lastUpdate;
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

  // reference

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
   * The node {@link #position()}, {@link #orientation()} and {@link #magnitude()} are
   * defined with respect to the {@link #reference()} coordinate system. A
   * {@code null} reference node (default value) means that the node is defined in the
   * world coordinate system.
   * <p>
   * Use {@link #worldPosition()}, {@link #worldOrientation()} and {@link #worldMagnitude()} to
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
   */
  public void resetReference() {
    setReference(null);
  }

  /**
   * Sets the {@link #reference()} of the node.
   * <p>
   * The node {@link #position()}, {@link #orientation()} and {@link #magnitude()} are then
   * defined in the {@link #reference()} coordinate system.
   * <p>
   * Use {@link #worldPosition()}, {@link #worldOrientation()} and {@link #worldMagnitude()} to express
   * the node global transformation in the world coordinate system.
   * <p>
   * Using this method, you can create a hierarchy of nodes. This hierarchy needs to be a
   * tree, which root is the world coordinate system (i.e., {@code null}
   * {@link #reference()}). No action is performed if setting {@code reference} as the
   * {@link #reference()} would create a loop in the hierarchy.
   * <p>
   * To make all the nodes in the {@code node} branch eligible for garbage collection
   * call {@link Graph#prune(Node)}.
   *
   * @see Graph#prune(Node)
   */
  public void setReference(Node node) {
    // 0. filter
    if (node != null && node == reference()) {
      System.out.println("Warning: Node reference already set. Nothing done!");
      return;
    }
    if (node == this) {
      System.out.println("Warning: A node cannot be a reference of itself. Nothing done!");
      return;
    }
    if (_isSuccessor(node)) {
      System.out.println("Warning: A node descendant cannot be set as its reference. Nothing done!");
      return;
    }
    // 1. Determine prev and target states
    boolean prevReachable = isReachable();
    boolean nowReachable = node == null;
    if (!nowReachable) {
      nowReachable = node.isReachable();
    }
    // 2. cache prev state
    Vector position = position = this.worldPosition().copy();
    Quaternion orientation = this.worldOrientation().copy();
    float magnitude = this.worldMagnitude();
    // 3. temporarily remove node from the graph while setting new reference
    if (reference() != null) {
      reference()._removeChild(this);
    }
    else {
      Graph._removeLeadingNode(this);
    }
    _reference = node;
    // 4. re-add node to graph
    _restorePath(reference(), this);
    // 5. Propagate state
    // 5.1. prevReachable -> !nowReachable
    if (prevReachable && !nowReachable) {
      List<Node> branch = Graph.branch(this);
      for (Node descendant : branch) {
        descendant._unregisterTasks();
      }
    }
    // 5.2. !prevReachable -> nowReachable
    if (!prevReachable && nowReachable) {
      List<Node> branch = Graph.branch(this);
      for (Node descendant : branch) {
        descendant._registerTasks();
      }
    }
    // 6. restore cache prev state (step 2. above)
    this.setWorldPosition(position);
    this.setWorldOrientation(orientation);
    this.setWorldMagnitude(magnitude);
    _modified();
  }

  /**
   * Returns whether or not the node is reachable by the rendering algorithm.
   * 
   * @see #setReference(Node) 
   * @see Graph#prune(Node)
   */
  public boolean isReachable() {
    return Graph.TimingHandler.isTaskRegistered(_translationTask);
  }
  
  protected void _registerTasks() {
    if (!Graph.TimingHandler.isTaskRegistered(_translationTask)) {
      _translationTask = new InertialTask() {
        @Override
        public void action() {
          translate(_x, _y, _z);
        }
      };
    }
    if (!Graph.TimingHandler.isTaskRegistered(_rotationTask)) {
      _rotationTask = new InertialTask() {
        @Override
        public void action() {
          rotate(new Quaternion(_x, _y, _z));
        }
      };
    }
    if (!Graph.TimingHandler.isTaskRegistered(_orbitTask)) {
      _orbitTask = new InertialTask() {
        @Override
        public void action() {
          _orbit(new Quaternion(_x, _y, _z), _center);
        }
      };
    }
    if (!Graph.TimingHandler.isTaskRegistered(_scalingTask)) {
      _scalingTask = new InertialTask() {
        @Override
        public void action() {
          float factor = 1 + Math.abs(_x) / _scalingFactor;
          scale(_x >= 0 ? factor : 1 / factor);
        }
      };
    }
  }

  protected void _unregisterTasks() {
    TimingHandler.unregisterTask(_translationTask);
    TimingHandler.unregisterTask(_rotationTask);
    TimingHandler.unregisterTask(_orbitTask);
    TimingHandler.unregisterTask(_scalingTask);
  }

  /**
   * Used by {@link #setReference(Node)}.
   */
  protected void _restorePath(Node parent, Node child) {
    if (parent == null) {
      Graph._addLeadingNode(child);
    } else {
      if (!parent._hasChild(child)) {
        parent._addChild(child);
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
   * Returns the list a child nodes of this node.
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
   * Randomized this node. The node is randomly re-positioned inside the ball
   * defined by {@code center} and {@code radius}, which in 2D is a
   * circumference parallel to the x-y plane. The {@link #worldOrientation()} is
   * randomized by {@link Quaternion#randomize()}. The new magnitude is a random
   * in old-magnitude * [0,5...2].
   *
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
    setWorldPosition(Vector.add(center, displacement));
    setWorldOrientation(quaternion);
    setWorldMagnitude(worldMagnitude() * _random(0.5f, 2));
  }

  /**
   * Returns a random node attached to {@code graph}. The node is randomly positioned inside
   * the {@code graph} viewing volume which is defined by {@link Graph#center()} and {@link Graph#radius()}
   * (see {@link Vector#random()}). The {@link #worldOrientation()} is set by {@link Quaternion#random()}. The
   * {@link #worldMagnitude()} is a random in [0,5...2].
   *
   * @see #random(Vector, float, boolean)
   * @see Vector#random()
   * @see Quaternion#random()
   * @see #randomize(Vector, float, boolean)
   */
  public static Node random(Graph graph) {
    Node node = new Node();
    node.randomize(graph.center(), graph.radius(), graph.is3D());
    return node;
  }

  /**
   * Returns a random node. The node is randomly positioned inside the ball defined
   * by {@code center} and {@code radius} (see {@link Vector#random()}), which in 2D is a
   * circumference parallel to the x-y plane. The {@link #worldOrientation()} is set by
   * {@link Quaternion#random()}. The {@link #worldMagnitude()} is a random in [0,5...2].
   *
   * @see #random(Graph)
   * @see Vector#random()
   * @see Quaternion#random()
   * @see #randomize(Vector, float, boolean)
   */
  public static Node random(Vector center, float radius, boolean is3D) {
    Node node = new Node();
    node.randomize(center, radius, is3D);
    return node;
  }

  // PRECISION

  /**
   * Sets the {@link #bullsEyeSize()} of the {@link #BULLSEYE} {@link #hint()}.
   * <p>
   * Picking a node is done with ray casting against a screen-space bullseye shape
   * (see {@link #configHint(int, Object...)}) whose length is defined as follows:
   * <ul>
   * <li>A percentage of the graph diameter (see {@link Graph#radius()}). Set it
   * with {@code size in [0..1]}.</li>
   * <li>A fixed numbers of pixels. Set it with {@code size > 1}.</li>
   * </ul>
   *
   * @see #bullsEyeSize()
   */
  public void setBullsEyeSize(float size) {
    if (size <= 0)
      System.out.println("Warning: bulls eye size should be positive. Nothing done");
    _bullsEyeSize = size;
  }

  /**
   * Sets the node {@link #highlight()} which should be a value in  {@code [0..1]}.
   * Default value is {@code 0.15}.
   *
   * @see #highlight()
   */
  public void setHighlight(float highlight) {
    float val = Math.abs(highlight);
    while (val > 1)
      val /= 10;
    if (val != highlight)
      System.out.println("Warning: highlight should be in [0..1]. Setting it as " + val);
    _highlight = val;
  }

  /**
   * Returns the highlighting magnitude use to scale the node when it's tagged.
   *
   * @see #setHighlight(float)
   */
  public float highlight() {
    return _highlight;
  }

  /**
   * Returns the node {@link #BULLSEYE} {@link #hint()} size. Set it with
   * {@link #setBullsEyeSize(float)}.
   *
   * @see #setBullsEyeSize(float)
   */
  public float bullsEyeSize() {
    return _bullsEyeSize;
  }

  // TRANSLATION

  /**
   * Returns the node position, defined with respect to the {@link #reference()}.
   * <p>
   * Use {@link #worldPosition()} to get the result in world coordinates. These two values are
   * identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setPosition(Vector)
   */
  public Vector position() {
    return _position;
  }

  /**
   * Sets the {@link #position()} of the node, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Use {@link #setWorldPosition(Vector)} to define the world coordinates {@link #worldPosition()}.
   */
  public void setPosition(Vector position) {
    _position = position;
    _modified();
  }

  /**
   * Same as {@code setPosition(vector, filter, this, params)}.
   *
   * @see #setPosition(Vector, BiFunction, Node, Object[])
   */
  public void setPosition(Vector vector, BiFunction<Node, Object[], Vector> filter, Object [] params) {
    setPosition(vector, filter, this, params);
  }

  /**
   * Same as {@code node.translate(node.cacheTargetTranslation, filter, params)}. Note that the filter may access the
   * {@code vector} as {@code cacheTargetPosition} and the target translation as {@code cacheTargetTranslation}
   * which is computed as {@code Vector.subtract(vector, node.position())}.
   *
   * @see #translate(Vector)
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public static void setPosition(Vector vector, BiFunction<Node, Object[], Vector> filter, Node node, Object[] params) {
    node.cacheTargetPosition = vector;
    node.cacheTargetTranslation = Vector.subtract(vector, node.position());
    node.translate(node.cacheTargetTranslation, filter, params);
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
   * Same as {@code translate(new Vector(x, y, z), inertia)}.
   *
   * @see #translate(Vector, float)
   */
  public void translate(float x, float y, float z, float inertia) {
    translate(new Vector(x, y, z), inertia);
  }

  /**
   * Same as {@link #translate(Vector)} but with {@code float} parameters.
   */
  public void translate(float x, float y, float z) {
    translate(new Vector(x, y, z));
  }

  /**
   * Translates the node according to {@code vector}, locally defined with respect to the
   * {@link #reference()} and with an impulse defined with {@code inertia} which should
   * be in {@code [0..1]}, 0 no inertia & 1 no friction.
   *
   * @see #rotate(Quaternion, float)
   * @see #scale(float, float)
   * @see #orbit(Vector, float, Vector, float)
   */
  public void translate(Vector vector, float inertia) {
    translate(vector);
    if (!Graph.TimingHandler.isTaskRegistered(_translationTask)) {
      System.out.println("Warning: inertia is disabled. Perhaps your node is detached. Use translate(vector) instead");
      return;
    }
    _translationTask.setInertia(inertia);
    _translationTask._x += vector.x();
    _translationTask._y += vector.y();
    _translationTask._z += vector.z();
    if (!_translationTask.isActive()) {
      _translationTask.run();
    }
  }

  /**
   * Same as {@code translate(vector, 0)}.
   *
   * @see #translate(Vector, float)
   * @see #translate(Vector, BiFunction, Object[], float)
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public void translate(Vector vector) {
    position().add(vector);
    _modified();
  }

  /**
   * Filters {@code vector} so that the node is translated along {@code axis}
   * (defined in this node coordinate system). Call it as:
   * <p>
   * {@code translate(vector, vectorAxisFilter, this, new Object[] { axis }))} or
   * {@code translate(vector, vectorAxisFilter, this, new Object[] { axis }, inertia)}.
   *
   * @see #translate(Vector, BiFunction, Node, Object[])
   * @see #translate(Vector, BiFunction, Node, Object[], float)
   * @see #vectorPlaneFilter
   */
  public static BiFunction<Node, Object[], Vector> vectorAxisFilter = (node, params)-> {
    return Vector.projectVectorOnAxis(node.cacheTargetTranslation, node.referenceDisplacement((Vector) params[0]));
  };

  /**
   * Filters {@code vector} so that the node is translated along the plane defined by {@code normal}
   * (defined in this node coordinate system). Call it as:
   * <p>
   * {@code translate(vector, vectorPlaneFilter, this, new Object[] { normal }))} or
   * {@code translate(vector, vectorPlaneFilter, this, new Object[] { normal }, inertia)}.
   *
   * @see #translate(Vector, BiFunction, Node, Object[])
   * @see #translate(Vector, BiFunction, Node, Object[], float)
   * @see #vectorAxisFilter
   */
  public static BiFunction<Node, Object[], Vector> vectorPlaneFilter = (node, params)-> {
    return Vector.projectVectorOnPlane(node.cacheTargetTranslation, node.referenceDisplacement((Vector) params[0]));
  };

  /**
   * Same as {@code translate(vector, filter, this, params, inertia)}.
   *
   * @see #translate(Vector, BiFunction, Node, Object[], float)
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public void translate(Vector vector, BiFunction<Node, Object[], Vector> filter, Object [] params, float inertia) {
    translate(vector, filter, this, params, inertia);
  }

  /**
   * Same as {@code node.translate(filter.apply(node, params), inertia)}. Note that the filter may access the
   * {@code vector} as {@code cacheTargetTranslation} and the target position as {@code cacheTargetPosition}.
   * .
   * @see #translate(Vector, float)
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public static void translate(Vector vector, BiFunction<Node, Object[], Vector> filter, Node node, Object [] params, float inertia) {
    node.cacheTargetTranslation = vector;
    node.cacheTargetPosition = Vector.add(node.position(), vector);
    node.translate(filter.apply(node, params), inertia);
  }

  /**
   * Same as {@code translate(vector, filter, this, params)}.
   *
   * @see #translate(Vector, BiFunction, Node, Object[])
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public void translate(Vector vector, BiFunction<Node, Object[], Vector> filter, Object [] params) {
    translate(vector, filter, this, params);
  }

  /**
   * Same as {@code node.translate(filter.apply(node, params))}. Note that the filter may access the
   * {@code vector} as {@code cacheTargetTranslation} and the target position as {@code cacheTargetPosition}.
   *
   * @see #translate(Vector)
   * @see #vectorAxisFilter
   * @see #vectorPlaneFilter
   */
  public static void translate(Vector vector, BiFunction<Node, Object[], Vector> filter, Node node, Object[] params) {
    node.cacheTargetTranslation = vector;
    node.cacheTargetPosition = Vector.add(node.position(), vector);
    node.translate(filter.apply(node, params));
  }

  // POSITION

  /**
   * Returns the node position defined in the world coordinate system.
   *
   * @see #worldOrientation()
   * @see #worldMagnitude()
   * @see #setWorldPosition(Vector)
   * @see #position()
   */
  public Vector worldPosition() {
    return worldLocation(new Vector());
  }

  /**
   * Sets the {@link #worldPosition()} to that of {@code node}.
   *
   * @see #setWorldPosition(Vector)
   * @see #set(Node)
   */
  public void setWorldPosition(Node node) {
    setWorldPosition(node == null ? new Vector() : node.worldPosition());
  }

  /**
   * Sets the node {@link #worldPosition()}, defined in the world coordinate system.
   * <p>
   * Use {@link #setPosition(Vector)} to define the local node position (with respect
   * to the {@link #reference()}).
   */
  public void setWorldPosition(Vector position) {
    setPosition(reference() != null ? reference().location(position) : position);
  }

  /**
   * Same as {@code setWorldPosition(vector, filter, this, params)}.
   *
   * @see #setWorldPosition(Vector, BiFunction, Node, Object[])
   */
  public void setWorldPosition(Vector vector, BiFunction<Node, Object[], Vector> filter, Object [] params) {
    setWorldPosition(vector, filter, this, params);
  }

  /**
   * Same as {@code Node.setPosition(node.reference() != null ? node.reference().location(vector) : vector, filter, node, params)}.
   *
   * @see #setPosition(Vector, BiFunction, Node, Object[])
   */
  public static void setWorldPosition(Vector vector, BiFunction<Node, Object[], Vector> filter, Node node, Object[] params) {
    Node.setPosition(node.reference() != null ? node.reference().location(vector) : vector, filter, node, params);
  }

  /**
   * Same as {@link #setWorldPosition(Vector)}, but with {@code float} parameters.
   */
  public void setWorldPosition(float x, float y) {
    setWorldPosition(new Vector(x, y));
  }

  /**
   * Same as {@link #setWorldPosition(Vector)}, but with {@code float} parameters.
   */
  public void setWorldPosition(float x, float y, float z) {
    setWorldPosition(new Vector(x, y, z));
  }

  // ROTATION

  /**
   * Returns the node orientation, defined with respect to the {@link #reference()}
   * (i.e, the current Quaternion orientation).
   * <p>
   * Use {@link #worldOrientation()} to get the result in world coordinates. These two values
   * are identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setOrientation(Quaternion)
   */
  public Quaternion orientation() {
    return _orientation;
  }

  /**
   * Same as {@link #setOrientation(Quaternion)} but with {@code float} Quaternion parameters.
   */
  public void setOrientation(float x, float y, float z, float w) {
    setOrientation(new Quaternion(x, y, z, w));
  }

  /**
   * Sets the node {@link #orientation()}, locally defined with respect to the
   * {@link #reference()}. Use {@link #setWorldOrientation(Quaternion)} to define the
   * world coordinates {@link #worldOrientation()}.
   *
   * @see #orientation()
   * @see #setPosition(Vector)
   */
  public void setOrientation(Quaternion orientation) {
    _orientation = orientation;
    _modified();
  }

  /**
   * Same as {@code setOrientation(quaternion, filter, this, params)}.
   *
   * @see #setOrientation(Quaternion, BiFunction, Node, Object[])
   */
  public void setOrientation(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Object [] params) {
    setOrientation(quaternion, filter, this, params);
  }

  /**
   * Same as {@code node.rotate(node.cacheTargetRotation, filter, params)}. Note that the filter may
   * access the {@code quaternion} as {@code cacheTargetOrientation} and the target orientation as
   * {@code cacheTargetRotation} which is computed as
   * {@code Quaternion.compose(node.orientation().inverse(), quaternion)}.
   *
   * @see #rotate(Quaternion, BiFunction, Object[])
   */
  public static void setOrientation(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Node node, Object[] params) {
    node.cacheTargetOrientation = quaternion;
    node.cacheTargetRotation = Quaternion.compose(node.orientation().inverse(), quaternion);
    node.rotate(node.cacheTargetRotation, filter, params);
  }

  /**
   * Same as {@code rotate(new Quaternion(axis, angle), inertia)}.
   *
   * @see #rotate(Quaternion, float)
   */
  public void rotate(Vector axis, float angle, float inertia) {
    rotate(new Quaternion(axis, angle), inertia);
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
   * Same as {@code rotate(new Vector(x, y, z), angle, inertia)}.
   *
   * @see #rotate(Vector, float, float)
   */
  public void rotate(float x, float y, float z, float angle, float inertia) {
    rotate(new Vector(x, y, z), angle, inertia);
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
   * Rotates the node by {@code quaternion} (defined in the node coordinate system):
   * {@code orientation().compose(quaternion)} and with an impulse defined with
   * {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   *
   * @see #translate(Vector, float)
   * @see #orbit(Vector, float, Vector, float)
   * @see #scale(float, float)
   */
  public void rotate(Quaternion quaternion, float inertia) {
    rotate(quaternion);
    if (!Graph.TimingHandler.isTaskRegistered(_rotationTask)) {
      System.out.println("Warning: inertia is disabled. Perhaps your node is detached. Use rotate(quaternion) instead");
      return;
    }
    _rotationTask.setInertia(inertia);
    Vector e = quaternion.eulerAngles();
    _rotationTask._x += e.x();
    _rotationTask._y += e.y();
    _rotationTask._z += e.z();
    if (!_rotationTask.isActive())
      _rotationTask.run();
  }

  /**
   * Same as {@code rotate(quaternion, 0)}.
   *
   * @see #rotate(Quaternion, float)
   * @see #rotate(Quaternion, BiFunction, Node, Object[])
   * @see #rotate(Quaternion, BiFunction, Node, Object[], float)
   * @see #quaternionAxisFilter
   */
  public void rotate(Quaternion quaternion) {
    orientation().compose(quaternion);
    orientation().normalize(); // Prevents numerical drift
    _modified();
  }

  /**
   * Filters {@code quaternion} so that its {@link Quaternion#axis()} become {@code axis}
   * (defined in this node coordinate system). Call it as:
   * <p>
   * {@code rotate(quaternion, quaternionAxisFilter, this, new Object[] { axis })} or
   * {@code rotate(quaternion, quaternionAxisFilter, this, new Object[] { axis }, inertia)}.
   *
   * @see #rotate(Quaternion, BiFunction, Node, Object[])
   * @see #rotate(Quaternion, BiFunction, Node, Object[], float)
   */
  public static BiFunction<Node, Object[], Quaternion> quaternionAxisFilter = (node, params)-> {
    return new Quaternion(Vector.projectVectorOnAxis(node.cacheTargetRotation.axis(), (Vector)params[0]), node.cacheTargetRotation.angle());
  };

  /**
   * Same as {@code rotate(quaternion, filter, this, params, inertia)}.
   *
   * @see #rotate(Quaternion, BiFunction, Node, Object[], float)
   * @see #quaternionAxisFilter
   */
  public void rotate(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Object [] params, float inertia) {
    rotate(quaternion, filter, this, params, inertia);
  }

  /**
   * Same as {@code node.rotate(filter.apply(node, params), inertia)}. Note that filter may access the
   * {@code quaternion} as {@code cacheTargetRotation} and the target orientation as {@code cacheTargetOrientation}.
   *
   * @see #rotate(Quaternion)
   * @see #quaternionAxisFilter
   */
  public static void rotate(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Node node, Object[] params, float inertia) {
    node.cacheTargetRotation = quaternion;
    node.cacheTargetOrientation = Quaternion.compose(node.orientation(), quaternion);
    node.rotate(filter.apply(node, params));
  }

  /**
   * Same as {@code rotate(quaternion, filter, this, params)}.
   *
   * @see #rotate(Quaternion, BiFunction, Node, Object[])
   * @see #quaternionAxisFilter
   */
  public void rotate(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Object [] params) {
    rotate(quaternion, filter, this, params);
  }

  /**
   * Same as {@code node.rotate(filter.apply(node, params))}. Note that filter may access the
   * {@code quaternion} as {@code cacheTargetRotation} and the target orientation as {@code cacheTargetOrientation}.
   *
   * @see #rotate(Quaternion)
   * @see #quaternionAxisFilter
   */
  public static void rotate(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Node node, Object[] params) {
    node.cacheTargetRotation = quaternion;
    node.cacheTargetOrientation = Quaternion.compose(node.orientation(), quaternion);
    node.rotate(filter.apply(node, params));
  }

  /**
   * Rotates the node by the {@code quaternion} (defined in the node coordinate system)
   * around {@code center} defined in the world coordinate system, and with an impulse
   * defined with {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   *
   * @see #translate(Vector, float)
   * @see #rotate(Quaternion, float)
   * @see #scale(float, float)
   */
  protected void _orbit(Quaternion quaternion, Vector center, float inertia) {
    _orbit(quaternion, center);
    if (!Graph.TimingHandler.isTaskRegistered(_orbitTask)) {
      System.out.println("Warning: inertia is disabled. Perhaps your node is detached. Use orbit(quaternion, center) instead");
      return;
    }
    _orbitTask.setInertia(inertia);
    _orbitTask._center = center;
    Vector e = quaternion.eulerAngles();
    _orbitTask._x += e.x();
    _orbitTask._y += e.y();
    _orbitTask._z += e.z();
    if (!_orbitTask.isActive())
      _orbitTask.run();
  }

  /**
   * Same as {@code orbit(quaternion, center, 0)}.
   *
   * @see #_orbit(Quaternion, Vector, float)
   */
  // TODO should not be constrained!
  protected void _orbit(Quaternion quaternion, Vector center) {
    orientation().compose(quaternion);
    orientation().normalize(); // Prevents numerical drift

    // Original in nodes-0.1.x and proscene:
    //Vector vector = Vector.add(center, (new Quaternion(orientation().rotate(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(position(), center)));
    // TODO test node hierarchy, we are using worldDisplacement instead of orientation().rotate
    Vector vector = Vector.add(center, (new Quaternion(worldDisplacement(quaternion.axis()), quaternion.angle())).rotate(Vector.subtract(worldPosition(), center)));
    vector.subtract(position());
    translate(vector);

    // Previous three lines are equivalent to:
    /*
    Quaternion worldQuaternion = new Quaternion(worldDisplacement(quaternion.axis()), quaternion.angle());
    Vector center2Position = Vector.subtract(position(), center);
    Vector center2PositionRotated = worldQuaternion.rotate(center2Position);
    Vector vector = Vector.add(center, center2PositionRotated);
    vector.subtract(position());
    translate(vector);
    */
  }

  /**
   * Same as {@code orbit(axis, angle, new Vector(), inertia)}.
   * @see #orbit(Vector, float)
   * @see #orbit(Vector, float, Vector, float)
   */
  public void orbit(Vector axis, float angle, float inertia) {
    orbit(axis, angle, new Vector(), inertia);
  }

  /**
   * Rotates the node around {@code axis} passing through the origin, both defined in the world
   * coordinate system. Same as {@code orbit(axis, angle, new Vector())}.
   *
   * @see #orbit(Vector, float, Vector)
   * @see #orbit(Vector, float, Vector, float)
   */
  public void orbit(Vector axis, float angle) {
    orbit(axis, angle, new Vector());
  }

  /**
   * Same as {@code orbit(new Quaternion(displacement(axis), angle), center, inertia)}.
   *
   * @see #orbit(Vector, float)
   * @see #orbit(Vector, float, float)
   */
  public void orbit(Vector axis, float angle, Vector center, float inertia) {
    _orbit(new Quaternion(displacement(axis), angle), center, inertia);
  }

  /**
   * Rotates the node around {@code axis} passing through {@code center}, both defined in the world
   * coordinate system. Same as {@code orbit(new Quaternion(displacement(axis), angle), center)}.
   *
   * @see #_orbit(Quaternion, Vector)
   * @see #orbit(Vector, float)
   */
  public void orbit(Vector axis, float angle, Vector center) {
    _orbit(new Quaternion(displacement(axis), angle), center);
  }

  // ORIENTATION

  /**
   * Returns the orientation of the node, defined in the world coordinate system.
   *
   * @see #worldPosition()
   * @see #worldMagnitude()
   * @see #setWorldOrientation(Quaternion)
   * @see #orientation()
   */
  public Quaternion worldOrientation() {
    return worldDisplacement(new Quaternion());
  }

  /**
   * Sets the {@link #worldOrientation()} to that of {@code node}.
   *
   * @see #setWorldOrientation(Quaternion)
   * @see #set(Node)
   */
  public void setWorldOrientation(Node node) {
    setWorldOrientation(node == null ? new Quaternion() : node.worldOrientation());
  }

  /**
   * Sets the {@link #worldOrientation()} of the node, defined in the world coordinate system.
   * <p>
   * Use {@link #setOrientation(Quaternion)} to define the local node orientation (with respect
   * to the {@link #reference()}).
   */
  public void setWorldOrientation(Quaternion quaternion) {
    setOrientation(reference() != null ? reference().displacement(quaternion) : quaternion);
  }

  /**
   * Same as {@code setWorldOrientation(quaternion, filter, this, params)}.
   *
   * @see #setWorldOrientation(Quaternion, BiFunction, Node, Object[])
   */
  public void setWorldOrientation(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Object [] params) {
    setWorldOrientation(quaternion, filter, this, params);
  }

  /**
   * Same as {@code Node.setOrientation(node.reference() != null ? node.reference().displacement(quaternion) : quaternion, filter, node, params)}.
   *
   * @see #setOrientation(Quaternion, BiFunction, Node, Object[])
   */
  public static void setWorldOrientation(Quaternion quaternion, BiFunction<Node, Object[], Quaternion> filter, Node node, Object[] params) {
    Node.setOrientation(node.reference() != null ? node.reference().displacement(quaternion) : quaternion, filter, node, params);
  }

  /**
   * Same as {@link #setWorldOrientation(Quaternion)}, but with {@code float} parameters.
   */
  public void setWorldOrientation(float x, float y, float z, float w) {
    setWorldOrientation(new Quaternion(x, y, z, w));
  }

  // SCALING

  /**
   * Returns the node magnitude, defined with respect to the {@link #reference()}.
   * <p>
   * Use {@link #worldMagnitude()} to get the result in world coordinates. These two values are
   * identical when the {@link #reference()} is {@code null} (default).
   *
   * @see #setMagnitude(float)
   */
  public float magnitude() {
    return _magnitude;
  }

  /**
   * Sets the {@link #magnitude()} of the node, locally defined with respect to the
   * {@link #reference()}.
   * <p>
   * Use {@link #setWorldMagnitude(float)} to define the world coordinates {@link #worldMagnitude()}.
   */
  public void setMagnitude(float magnitude) {
    if (magnitude > 0) {
      _magnitude = magnitude;
      _modified();
    } else
      System.out.println("Warning. Scaling should be positive. Nothing done");
  }

  /**
   * Same as {@code setMagnitude(magnitude, filter, this, params)}.
   *
   * @see #setMagnitude(float, BiFunction, Node, Object[])
   */
  public void setMagnitude(float magnitude, BiFunction<Node, Object[], Float> filter, Object [] params) {
    setMagnitude(magnitude, filter, this, params);
  }

  /**
   * Same as {@code node.scale(node.cacheTargetScaling, filter, params)}. Note that the filter may
   * access the {@code magnitude} as {@code cacheTargetMagnitude} and the target scaling as
   * {@code cacheTargetScaling} which is computed as {@code magnitude / node.magnitude()}.
   *
   * @see #scale(float, BiFunction, Object[])
   */
  public static void setMagnitude(float magnitude, BiFunction<Node, Object[], Float> filter, Node node, Object[] params) {
    node.cacheTargetMagnitude = magnitude;
    node.cacheTargetScaling = magnitude / node.magnitude();
    node.scale(node.cacheTargetScaling, filter, params);
  }

  /**
   * Scales the node according to {@code scaling}, locally defined with respect to the
   * {@link #reference()}  and with an impulse defined with {@code inertia} which should
   * be in {@code [0..1]}, 0 no inertia & 1 no friction.
   *
   * @see #translate(Vector, float)
   * @see #rotate(Quaternion, float)
   * @see #_orbit(Quaternion, Vector, float)
   */
  public void scale(float scaling, float inertia) {
    scale(scaling);
    if (!Graph.TimingHandler.isTaskRegistered(_scalingTask)) {
      System.out.println("Warning: inertia is disabled. Perhaps your node is detached. Use scale(scaling) instead");
      return;
    }
    _scalingTask._inertia = inertia;
    _scalingTask._x += scaling > 1 ? _scalingFactor * (scaling - 1) : _scalingFactor * (scaling - 1) / scaling;
    if (!_scalingTask.isActive()) {
      _scalingTask.run();
    }
  }

  /**
   * Same as {@code setMagnitude(magnitude() * scaling)}.
   *
   * @see #scale(float, float)
   */
  public void scale(float scaling) {
    setMagnitude(magnitude() * scaling);
  }

  /**
   * Same as {@code scale(scaling, filter, this, params, inertia)}.
   *
   * @see #scale(float, BiFunction, Node, Object[], float)
   */
  public void scale(float scaling, BiFunction<Node, Object[], Float> filter, Object [] params, float inertia) {
    scale(scaling, filter, this, params, inertia);
  }

  /**
   * Same as {@code node.scale(filter.apply(node, params))}. Note that the filter may access the
   * {@code scaling} as {@code cacheTargetScaling}.
   *
   * @see #scale(float)
   */
  public static void scale(float scaling, BiFunction<Node, Object[], Float> filter, Node node, Object[] params, float inertia) {
    node.cacheTargetScaling = scaling;
    node.cacheTargetMagnitude = node.magnitude() * scaling;
    node.scale(filter.apply(node, params));
  }

  /**
   * Same as {@code scale(scaling, filter, this, params)}.
   *
   * @see #scale(float, BiFunction, Node, Object[])
   */
  public void scale(float scaling, BiFunction<Node, Object[], Float> filter, Object [] params) {
    scale(scaling, filter, this, params);
  }

  /**
   * Same as {@code node.scale(filter.apply(node, params))}. Note that the filter may access the
   * {@code scaling} as {@code cacheTargetScaling} and the resulting target magnitude as
   * {@code cacheTargetMagnitude}.
   *
   * @see #scale(float)
   */
  public static void scale(float scaling, BiFunction<Node, Object[], Float> filter, Node node, Object[] params) {
    node.cacheTargetScaling = scaling;
    node.cacheTargetMagnitude = node.magnitude() * scaling;
    node.scale(filter.apply(node, params));
  }

  // MAGNITUDE

  /**
   * Returns the magnitude of the node, defined in the world coordinate system.
   * <p>
   * Note that the magnitude is used to compute the node {@link Graph#projection()}
   * matrix to render a scene from the node point-of-view.
   *
   * @see #worldOrientation()
   * @see #worldPosition()
   * @see #setWorldPosition(Vector)
   * @see #position()
   * @see Graph#projection()
   */
  public float worldMagnitude() {
    return reference() != null ? reference().worldMagnitude() * magnitude() : magnitude();
  }

  /**
   * Sets the {@link #worldMagnitude()} to that of {@code node}.
   *
   * @see #setWorldMagnitude(float)
   * @see #set(Node)
   */
  public void setWorldMagnitude(Node node) {
    setWorldMagnitude(node == null ? 1 : node.worldMagnitude());
  }

  /**
   * Sets the {@link #worldMagnitude()} of the node, defined in the world coordinate system.
   * <p>
   * Use {@link #setMagnitude(float)} to define the local node magnitude (with respect to the
   * {@link #reference()}).
   */
  public void setWorldMagnitude(float magnitude) {
    setMagnitude(reference() != null ? magnitude / reference().worldMagnitude() : magnitude);
    // option 2 mimics setOrientation (should produce same results)
    //setScaling(reference() != null ? reference().displacement(magnitude) : magnitude);
  }

  /**
   * Same as {@code setWorldMagnitude(magnitude, filter, this, params)}.
   *
   * @see #setWorldMagnitude(float, BiFunction, Node, Object[])
   */
  public void setWorldMagnitude(float magnitude, BiFunction<Node, Object[], Float> filter, Object [] params) {
    setWorldMagnitude(magnitude, filter, this, params);
  }

  /**
   * Same as {@code Node.setMagnitude(node.reference() != null ? magnitude / node.reference().worldMagnitude() : magnitude, filter, node, params)}.
   *
   * @see #setMagnitude(float, BiFunction, Node, Object[])
   */
  public static void setWorldMagnitude(float magnitude, BiFunction<Node, Object[], Float> filter, Node node, Object[] params) {
    Node.setMagnitude(node.reference() != null ? magnitude / node.reference().worldMagnitude() : magnitude, filter, node, params);
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
   * When {@code move} is set to {@code true}, the Node {@link #worldPosition()} is also
   * affected by the alignment. The new Node {@link #worldPosition()} is such that the
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
        directions[0][d] = node.worldOrientation().rotate(dir);
      else
        directions[0][d] = dir;
      directions[1][d] = worldOrientation().rotate(dir);
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
    Node old = detach();
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
      q = Quaternion.multiply(orientation().inverse(), q);
      q = Quaternion.multiply(q, worldOrientation());
      rotate(q);
      // Try to align an other axis direction
      short d = (short) ((index[1] + 1) % 3);
      Vector dir = new Vector((d == 0) ? 1.0f : 0.0f, (d == 1) ? 1.0f : 0.0f, (d == 2) ? 1.0f : 0.0f);
      dir = worldOrientation().rotate(dir);
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
        q = Quaternion.multiply(orientation().inverse(), q);
        q = Quaternion.multiply(q, worldOrientation());
        rotate(q);
      }
    }
    if (move) {
      Vector center = new Vector(0.0f, 0.0f, 0.0f);
      if (node != null)
        center = node.worldPosition();
      vector = Vector.subtract(center, worldDisplacement(old.location(center)));
      vector.subtract(position());
      translate(vector);
    }
  }


  /**
   * Translates the node so that its {@link #worldPosition()} lies on the line defined by
   * {@code origin} and {@code direction} (defined in the world coordinate system).
   * <p>
   * Simply uses an orthogonal projection. {@code direction} does not need to be
   * normalized.
   */
  public void projectOnLine(Vector origin, Vector direction) {
    Vector position = worldPosition();
    Vector shift = Vector.subtract(origin, position);
    Vector proj = shift;
    proj = Vector.projectVectorOnAxis(proj, direction);
    setWorldPosition(Vector.add(position, Vector.subtract(shift, proj)));
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
    if (worldMagnitude() != 1)
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
    if (worldMagnitude() != 1)
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
    if (worldMagnitude() != 1)
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
   * @see #set(Node)
   * @see #worldMatrix()
   * @see #view()
   * @see #viewInverse()
   */
  public Matrix matrix() {
    Matrix matrix = orientation().matrix();

    matrix._matrix[12] = position()._vector[0];
    matrix._matrix[13] = position()._vector[1];
    matrix._matrix[14] = position()._vector[2];

    if (magnitude() != 1) {
      matrix.setM00(matrix.m00() * magnitude());
      matrix.setM10(matrix.m10() * magnitude());
      matrix.setM20(matrix.m20() * magnitude());

      matrix.setM01(matrix.m01() * magnitude());
      matrix.setM11(matrix.m11() * magnitude());
      matrix.setM21(matrix.m21() * magnitude());

      matrix.setM02(matrix.m02() * magnitude());
      matrix.setM12(matrix.m12() * magnitude());
      matrix.setM22(matrix.m22() * magnitude());
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
   * @see #set(Node)
   * @see #matrix()
   * @see #view()
   * @see #viewInverse()
   */
  public Matrix worldMatrix() {
    if (reference() != null)
      return detach().matrix();
    else
      return matrix();
  }

  /**
   * Returns the inverse of the matrix associated with the node position and orientation that
   * is to be used when the node represents an eye. This matrix matches the inverted of the
   * {@link #worldMatrix()} when {@link #magnitude()} is {@code 1}.
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
    return Matrix.view(worldPosition(), worldOrientation());
  }

  /**
   * Returns the inverse of the {@link #view()} matrix. This matrix matches the
   * {@link #worldMatrix()} when {@link #worldMagnitude()} is {@code 1}.
   * <p>
   * This matrix converts from the eye to world space.
   *
   * @see #view()
   * @see #matrix()
   * @see #worldMatrix()
   * @see #set(Node)
   */
  public Matrix viewInverse() {
    return Node.detach(worldPosition(), worldOrientation(), 1).matrix();
  }

  /**
   * Sets the node from a {@link #matrix()} representation: orientation and magnitude in the upper
   * left 3x3 matrix and position on the last column.
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
    r[0][0] = r00 / magnitude();
    r[0][1] = r01 / magnitude();
    r[0][2] = r02 / magnitude();
    r[1][0] = (matrix._matrix[1] / matrix._matrix[15]) / magnitude();
    r[1][1] = (matrix._matrix[5] / matrix._matrix[15]) / magnitude();
    r[1][2] = (matrix._matrix[9] / matrix._matrix[15]) / magnitude();
    r[2][0] = (matrix._matrix[2] / matrix._matrix[15]) / magnitude();
    r[2][1] = (matrix._matrix[6] / matrix._matrix[15]) / magnitude();
    r[2][2] = (matrix._matrix[10] / matrix._matrix[15]) / magnitude();
    setOrientation(new Quaternion(
        new Vector(r[0][0], r[1][0], r[2][0]),
        new Vector(r[0][1], r[1][1], r[2][1]),
        new Vector(r[0][2], r[1][2], r[2][2]))
    );
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

    setWorldPosition(
        matrix._matrix[12] / matrix._matrix[15],
        matrix._matrix[13] / matrix._matrix[15],
        matrix._matrix[14] / matrix._matrix[15]
    );

    float r00 = matrix._matrix[0] / matrix._matrix[15];
    float r01 = matrix._matrix[4] / matrix._matrix[15];
    float r02 = matrix._matrix[8] / matrix._matrix[15];
    setWorldMagnitude(new Vector(r00, r01, r02).magnitude());// calls _modified() :P

    float[][] r = new float[3][3];
    r[0][0] = r00 / magnitude();
    r[0][1] = r01 / magnitude();
    r[0][2] = r02 / magnitude();
    r[1][0] = (matrix._matrix[1] / matrix._matrix[15]) / magnitude();
    r[1][1] = (matrix._matrix[5] / matrix._matrix[15]) / magnitude();
    r[1][2] = (matrix._matrix[9] / matrix._matrix[15]) / magnitude();
    r[2][0] = (matrix._matrix[2] / matrix._matrix[15]) / magnitude();
    r[2][1] = (matrix._matrix[6] / matrix._matrix[15]) / magnitude();
    r[2][2] = (matrix._matrix[10] / matrix._matrix[15]) / magnitude();
    setWorldOrientation(new Quaternion(
        new Vector(r[0][0], r[1][0], r[2][0]),
        new Vector(r[0][1], r[1][1], r[2][1]),
        new Vector(r[0][2], r[1][2], r[2][2]))
    );
  }

  /**
   * Returns a node representing the inverse of this node space transformation.
   * <p>
   * The new node {@link #orientation()} is the
   * {@link Quaternion#inverse()} of the original orientation. Its
   * {@link #position()} is the negated inverse rotated image of the original
   * position. Its {@link #magnitude()} is 1 / original magnitude.
   * <p>
   * If a node is considered as a space rigid transformation, i.e., position and
   * orientation, but no magnitude (magnitude=1), the inverse() node performs the inverse
   * transformation.
   * <p>
   * Only the local node transformation (i.e., defined with respect to the
   * {@link #reference()}) is inverted. Use {@link #worldInverse()} for a global
   * inverse.
   * <p>
   * The resulting node has the same {@link #reference()} as the this node.
   *
   * @see #worldInverse()
   */
  public Node inverse() {
    Node node = new Node(Vector.multiply(orientation().inverseRotate(position()), -1), orientation().inverse(), 1 / magnitude());
    node.setReference(reference());
    return node;
  }

  /**
   * Returns the {@link #inverse()} of the node world transformation.
   * <p>
   * The {@link #worldOrientation()} of the new node is the
   * {@link Quaternion#inverse()} of the original orientation. Its
   * {@link #worldPosition()} is the negated and inverse rotated image of the original
   * position. The {@link #worldMagnitude()} is the original magnitude multiplicative
   * inverse.
   * <p>
   * Use {@link #inverse()} for a local (i.e., with respect to {@link #reference()})
   * transformation inverse.
   *
   * @see #inverse()
   */
  public Node worldInverse() {
    return new Node(Vector.multiply(worldOrientation().inverseRotate(worldPosition()), -1), worldOrientation().inverse(), 1 / worldMagnitude());
  }

  // SCALAR CONVERSION

  /**
   * Converts {@code scalar} displacement from world to this node.
   * Same as {@code return displacement(scalar, null)}.
   * {@link #displacement(Vector)} converts vector displacements instead of scalar displacements.
   * {@link #displacement(Quaternion)} converts quaternion displacements instead of scalar displacements.
   * {@link #location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(float, Node)
   * @see #displacement(Vector)
   * @see #displacement(Quaternion)
   * @see #location(Vector)
   */
  public float displacement(float scalar) {
    return scalar / worldMagnitude();
  }

  /**
   * Converts {@code scalar} displacement from {@code node} to this node.
   * Use {@code node.displacement(scalar, this)} to perform the inverse transformation.
   * {@link #displacement(Vector, Node)} converts vector displacements instead of scalar displacements.
   * {@link #displacement(Quaternion, Node)} converts quaternion displacements instead of scalar displacements.
   * {@link #location(Vector, Node)} converts locations instead of displacements.
   *
   * @see #displacement(float)
   * @see #displacement(Quaternion)
   * @see #displacement(Vector)
   * @see #worldDisplacement(Vector)
   */
  public float displacement(float scalar, Node node) {
    return scalar * (node == null ? 1 : node.worldMagnitude()) / worldMagnitude();
  }

  /**
   * Converts {@code scalar} displacement from this node to world.
   * {@link #displacement(float)} performs the inverse transformation.
   * {@link #worldDisplacement(Vector)} converts vector displacements instead of scalar displacements.
   * {@link #worldDisplacement(Quaternion)} converts quaternion displacements instead of scalar displacements.
   * {@link #worldLocation(Vector)} converts locations instead of displacements.
   *
   * @see #location(Vector)
   * @see #worldDisplacement(Vector)
   * @see #worldDisplacement(Quaternion)
   * @see #displacement(float, Node)
   */
  public float worldDisplacement(float scalar) {
    return scalar * worldMagnitude();
  }

  /**
   * Converts {@code scalar} displacement from {@link #reference()} to this node.
   * <p>
   * {@link #referenceDisplacement(float)} performs the inverse transformation.
   *
   * @see #displacement(Quaternion)
   * @see #displacement(Vector)
   */
  public float localDisplacement(float scalar) {
    return scalar / magnitude();
  }

  /**
   * Converts {@code scalar} displacement from this node to {@link #reference()}.
   * <p>
   * {@link #localDisplacement(float)} performs the inverse transformation.
   *
   * @see #worldDisplacement(Quaternion)
   * @see #worldDisplacement(Vector)
   */
  public float referenceDisplacement(float scalar) {
    return scalar * magnitude();
  }

  // QUATERNION CONVERSION

  /**
   * Converts {@code quaternion} displacement from world to this node.
   * Same as {@code return displacement(quaternion, null)}.
   * {@link #displacement(Vector)} converts vector displacements instead of quaternion displacements.
   * {@link #displacement(float)} converts scalar displacements instead of quaternion displacements.
   * {@link #location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Quaternion, Node)
   * @see #displacement(Vector)
   * @see #displacement(float)
   * @see #location(Vector)
   */
  public Quaternion displacement(Quaternion quaternion) {
    return displacement(quaternion, null);
  }

  /**
   * Converts {@code quaternion} displacement from {@code node} to this node.
   * Use {@code node.displacement(quaternion, this)} to perform the inverse transformation.
   * {@link #displacement(Vector, Node)} converts vector displacements instead of quaternion displacements.
   * {@link #displacement(float, Node)} converts scalar displacements instead of quaternion displacements.
   * {@link #location(Vector, Node)} converts locations instead of displacements.
   *
   * @see #displacement(Quaternion)
   * @see #displacement(Vector)
   * @see #displacement(float)
   * @see #worldDisplacement(Vector)
   */
  public Quaternion displacement(Quaternion quaternion, Node node) {
    return this == node ? quaternion : localDisplacement(reference() != null ? reference().displacement(quaternion, node) : node == null ? quaternion : node.worldDisplacement(quaternion));
  }

  /**
   * Converts {@code quaternion} displacement from this node to world.
   * {@link #displacement(Vector)} performs the inverse transformation.
   * {@link #worldDisplacement(Vector)} converts vector displacements instead of quaternion displacements.
   * {@link #worldDisplacement(float)} converts scalar displacements instead of quaternion displacements.
   * {@link #worldLocation(Vector)} converts locations instead of displacements.
   *
   * @see #location(Vector)
   * @see #worldDisplacement(Vector)
   * @see #worldDisplacement(float)
   * @see #displacement(Quaternion, Node)
   */
  public Quaternion worldDisplacement(Quaternion quaternion) {
    Node node = this;
    Quaternion result = quaternion;
    while (node != null) {
      result = node.referenceDisplacement(result);
      node = node.reference();
    }
    return result;
  }

  /**
   * Converts {@code quaternion} displacement from {@link #reference()} to this node.
   * <p>
   * {@link #referenceDisplacement(Quaternion)} performs the inverse transformation.
   * {@link #localLocation(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Quaternion)
   * @see #displacement(Vector)
   */
  public Quaternion localDisplacement(Quaternion quaternion) {
    return Quaternion.compose(orientation().inverse(), quaternion);
  }

  /**
   * Converts {@code quaternion} displacement from this node to {@link #reference()}.
   * <p>
   * {@link #localDisplacement(Quaternion)} performs the inverse transformation.
   * {@link #referenceLocation(Vector)} converts locations instead of displacements.
   *
   * @see #worldDisplacement(Quaternion)
   * @see #worldDisplacement(Vector)
   */
  public Quaternion referenceDisplacement(Quaternion quaternion) {
    return Quaternion.compose(orientation(), quaternion);
  }

  // VECTOR CONVERSION

  /**
   * Converts {@code vector} displacement from world to this node.
   * Same as {@code return displacement(vector, null)}.
   * {@link #displacement(Quaternion)} converts quaternion displacements instead of vector displacements.
   * {@link #location(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Vector, Node)
   * @see #displacement(Quaternion)
   * @see #location(Vector)
   */
  public Vector displacement(Vector vector) {
    return displacement(vector, null);
  }

  /**
   * Converts {@code vector} displacement from {@code node} to this node.
   * Use {@code node.displacement(vector, this)} to perform the inverse transformation.
   * {@link #displacement(Quaternion, Node)} converts quaternion displacements instead of vector displacements.
   * {@link #location(Vector, Node)} converts locations instead of displacements.
   *
   * @see #displacement(Vector)
   * @see #displacement(Quaternion)
   * @see #worldDisplacement(Vector)
   */
  public Vector displacement(Vector vector, Node node) {
    return this == node ? vector : localDisplacement(reference() != null ? reference().displacement(vector, node) : node == null ? vector : node.worldDisplacement(vector));
  }

  /**
   * Converts {@code vector} displacement from this node to world.
   * {@link #displacement(Vector)} performs the inverse transformation.
   * {@link #worldDisplacement(Quaternion)} converts quaternion displacements instead of vector displacements.
   * {@link #worldLocation(Vector)} converts locations instead of displacements.
   *
   * @see #location(Vector)
   * @see #worldDisplacement(Quaternion)
   * @see #displacement(Vector, Node)
   */
  public Vector worldDisplacement(Vector vector) {
    Node node = this;
    Vector result = vector;
    while (node != null) {
      result = node.referenceDisplacement(result);
      node = node.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} displacement from {@link #reference()} to this node.
   * <p>
   * {@link #referenceDisplacement(Vector)} performs the inverse transformation.
   * {@link #localLocation(Vector)} converts locations instead of displacements.
   *
   * @see #displacement(Vector)
   */
  public Vector localDisplacement(Vector vector) {
    return Vector.divide(orientation().inverseRotate(vector), magnitude());
  }

  /**
   * Converts {@code vector} displacement from this node to {@link #reference()}.
   * <p>
   * {@link #localDisplacement(Vector)} performs the inverse transformation.
   * {@link #referenceLocation(Vector)} converts locations instead of displacements.
   *
   * @see #worldDisplacement(Vector)
   */
  public Vector referenceDisplacement(Vector vector) {
    return orientation().rotate(Vector.multiply(vector, magnitude()));
  }

  // POINT CONVERSION

  /**
   * Converts the {@code node} origin location to this node.
   * Same as {@code return location(new Vector(), node)}.
   * {@link #displacement(Vector, Node)} converts vector displacements instead of locations.
   * {@link #displacement(Quaternion, Node)} converts quaternion displacements instead of locations.
   *
   * @see #location(Vector)
   * @see #location(Vector, Node)
   * @see #displacement(Vector, Node)
   * @see #displacement(Quaternion, Node)
   */
  public Vector location(Node node) {
    return location(new Vector(), node);
  }

  /**
   * Converts {@code vector} location from world to this node.
   * Same as {@code return location(vector, null)}.
   * {@link #displacement(Vector)} converts vector displacements instead of locations.
   * {@link #displacement(Quaternion)} converts quaternion displacements instead of locations.
   *
   * @see #location(Node)
   * @see #location(Vector, Node)
   * @see #displacement(Vector)
   * @see #displacement(Quaternion)
   */
  public Vector location(Vector vector) {
    return location(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code node} to this node.
   * Use {@code node.location(vector, this)} to perform the inverse transformation.
   * {@link #displacement(Vector, Node)} converts vector displacements instead of locations.
   * {@link #displacement(Quaternion, Node)} converts quaternion displacements instead of locations.
   *
   * @see #location(Node)
   * @see #location(Vector)
   * @see #worldLocation(Vector)
   */
  public Vector location(Vector vector, Node node) {
    return this == node ? vector : localLocation(reference() != null ? reference().location(vector, node) : node == null ? vector : node.worldLocation(vector));
  }

  /**
   * Converts {@code vector} location from this node to world.
   * {@link #location(Vector)} performs the inverse transformation.
   * {@link #worldDisplacement(Vector)} converts vector displacements instead of locations.
   * {@link #worldDisplacement(Quaternion)} converts quaternion displacements instead of locations.
   *
   * @see #displacement(Vector)
   * @see #displacement(Quaternion)
   * @see #location(Vector, Node)
   */
  public Vector worldLocation(Vector vector) {
    Node node = this;
    Vector result = vector;
    while (node != null) {
      result = node.referenceLocation(result);
      node = node.reference();
    }
    return result;
  }

  /**
   * Converts {@code vector} location from {@link #reference()} to this node.
   * <p>
   * {@link #referenceLocation(Vector)} performs the inverse transformation.
   * {@link #localDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #location(Vector)
   */
  public Vector localLocation(Vector vector) {
    return Vector.divide(orientation().inverseRotate(Vector.subtract(vector, position())), magnitude());
  }

  /**
   * Converts {@code vector} location from this node to {@link #reference()}.
   * <p>
   * {@link #localLocation(Vector)} performs the inverse transformation.
   * {@link #referenceDisplacement(Vector)} converts displacements instead of locations.
   *
   * @see #worldLocation(Vector)
   */
  public Vector referenceLocation(Vector vector) {
    return Vector.add(orientation().rotate(Vector.multiply(vector, magnitude())), position());
  }

  // Attached nodes

  /**
   * Returns {@code true} if the {@code node} has been tagged by the {@code graph} at least once
   * and {@code false} otherwise.
   *
   * @see Graph#hasTag(String, Node)
   * @see Graph#hasTag(Node)
   */
  public boolean isTagged(Graph graph) {
    return graph.isTagged(this);
  }

  /**
   * Same as {@code graph.setVisit(this, functor)}.
   *
   * @see #setVisit(Graph, Consumer)
   * @see Graph#setVisit(Node, BiConsumer)
   * @see Graph#setVisit(Node, Consumer)
   */
  public void setVisit(Graph graph, BiConsumer<Graph, Node> functor) {
    graph.setVisit(this, functor);
  }

  /**
   * Same as {@code graph.setVisit(this, (g, n) -> functor.accept(g))}.
   *
   * @see #setVisit(Graph, BiConsumer)
   * @see Graph#setVisit(Node, BiConsumer)
   * @see Graph#setVisit(Node, Consumer)
   */
  public void setVisit(Graph graph, Consumer<Graph> functor) {
    graph.setVisit(this, (g, n) -> functor.accept(g));
  }

  /**
   * This together with {@link #visit(Graph)} is a workaround for
   * {@link #setVisit(Graph, BiConsumer)} which is not available in Processing 3.x.
   */
  public void setVisit(Graph graph) {
    setVisit(graph, this::visit);
  }

  /**
   * This together with {@link #setVisit(Graph)} is a workaround for
   * {@link #setVisit(Graph, BiConsumer)} which is not available in Processing 3.x.
   */
  public void visit(Graph graph) {}

  /**
   * Bypass rendering the node for the current frame. Set it before calling {@link Graph#render()}
   * or any rendering algorithm. Note that the node nor its children get culled.
   */
  public void bypass() {
    _bypass = TimingHandler.frameCount;
  }

  // js go:
  // public void graphics(Object context) {}

  /**
   * Calls {@link #resetIMRShape()} and {@link #resetRMRShape()}.
   *
   * @see #setShape(processing.core.PShape)
   * @see #setShape(Consumer)
   */
  public void resetShape() {
    _rmrShape = null;
    _imrShape = null;
    disableHint(SHAPE);
  }

  /**
   * Resets the retained-mode rendering shape.
   *
   * @see #setShape(Consumer)
   */
  public void resetRMRShape() {
    _rmrShape = null;
    if (_imrShape == null)
      disableHint(SHAPE);
  }

  /**
   * Resets the immediate-mode rendering shape.
   *
   * @see #setShape(processing.core.PShape)
   */
  public void resetIMRShape() {
    _imrShape = null;
    if (_rmrShape == null)
      disableHint(SHAPE);
  }

  /**
   * Sets this shape from the {@code node} shape.
   */
  public void setShape(Node node) {
    setShape(node._rmrShape);
    setShape(node._imrShape);
  }

  /**
   * Sets the node retained mode rendering (rmr) {@link #SHAPE} hint
   * (see {@link #hint()}). Use {@code enableHint(Node.SHAPE)},
   * {@code disableHint(Node.SHAPE)} and {@code toggleHint(Node.SHAPE)}
   * to (dis)enable the hint.
   *
   * @see #setShape(Consumer)
   * @see #resetShape()
   * @see #resetIMRShape()
   * @see #resetRMRShape()
   */
  public void setShape(processing.core.PShape shape) {
    if (shape == null) {
      resetRMRShape();
    }
    else {
      _rmrShape = shape;
      enableHint(SHAPE);
    }
  }

  /**
   * Sets the node immediate mode rendering (imr) {@link #SHAPE} procedure
   * hint (see {@link #hint()}). Use {@code enableHint(Node.SHAPE)},
   * {@code disableHint(Node.SHAPE)} and {@code toggleHint(Node.SHAPE)}
   * to (dis)enable the hint.
   *
   * @see #setShape(processing.core.PShape)
   * @see #resetShape()
   * @see #resetIMRShape()
   * @see #resetRMRShape()
   */
  public void setShape(Consumer<processing.core.PGraphics> callback) {
    if (callback == null) {
      resetIMRShape();
    } else {
      _imrShape = callback;
      enableHint(SHAPE);
    }
  }

  /**
   * Sets this shape from the {@code graph} shape.
   */
  public void setShape(Graph graph) {
    setShape(graph._rmrShape);
    setShape(graph._imrShape);
  }

  /**
   * Sets the node interaction procedure {@code callback} which is a function
   * implemented by a {@link Node} derived class and which takes
   * no params, or a gesture encoded as an array of on Object params.
   * <p>
   * The interaction is performed either after calling the
   * {@link Graph#interact(String, Object...)}, {@link Graph#interact(Object...)}
   * or {@link Graph#interact(Node, Object...)} scene procedures.
   * <p>
   * Same as {@code setInteraction((n, o) -> callback.accept(o))}.
   *
   * @see #setInteraction(BiConsumer)
   */
  public void setInteraction(Consumer<Object[]> callback) {
    setInteraction((n, o) -> callback.accept(o));
  }

  /**
   * Sets the node interaction procedure {@code callback} which is a function that takes
   * a node param (holding this node instance) and, optionally, a gesture encoded as
   * an array of on Object params.
   * <p>
   * The interaction is performed either after calling the
   * {@link Graph#interact(String, Object...)}, {@link Graph#interact(Object...)}
   * or {@link Graph#interact(Node, Object...)} scene procedures.
   *
   * @see #setInteraction(Consumer)
   */
  public void setInteraction(BiConsumer<Node, Object[]> callback) {
    _interact = callback;
  }

  /**
   * Parse {@code gesture} params. Useful to customize the node behavior.
   * Default implementation is empty. , i.e., it is meant to be implemented by derived classes.
   *
   * @see #setInteraction(BiConsumer)
   * @see #setInteraction(Consumer)
   */
  public void interact(Object[] gesture) {
    System.out.println("Warning: Node.interact() missed implementation");
  }

  /**
   * Override this method to set an immediate mode graphics procedure on the Processing
   * {@code PGraphics} or use {@link #setShape(Consumer)} instead.
   *
   * @see #setShape(Consumer)
   */
  public void graphics(processing.core.PGraphics pGraphics) {
  }

  protected void _updateHUD() {
    if ((_rmrHUD == null && _imrHUD == null) || !isHintEnabled(HUD)) {
      Graph._hudSet.remove(this);
    } else {
      Graph._hudSet.add(this);
    }
  }

  /**
   * Same as calling {@link #resetRMRHUD()} and {@link #resetIMRHUD()}.
   */
  public void resetHUD() {
    _rmrHUD = null;
    _imrHUD = null;
    disableHint(HUD);
    _updateHUD();
  }

  /**
   * Same as {@code setRMRShape(null)}.
   *
   * @see #setShape(processing.core.PShape)
   */
  public void resetRMRHUD() {
    _rmrHUD = null;
    if (_imrHUD == null)
      disableHint(HUD);
    _updateHUD();
  }

  /**
   * Same as {@code setIMRShape(null)}.
   *
   * @see #setShape(Consumer)
   */
  public void resetIMRHUD() {
    _imrHUD = null;
    if (_rmrHUD == null)
      disableHint(HUD);
    _updateHUD();
  }

  /**
   * Sets this (H)eads (U)p (D)isplay from the {@code node} hud.
   */
  public void setHUD(Node node) {
    setHUD(node._rmrHUD);
    setHUD(node._imrHUD);
  }

  /**
   * Sets the node retained mode rendering (rmr) shape {@link #HUD} hint
   * (see {@link #hint()}). The 2D {@code shape} screen coordinates are
   * interpreted relative to the node {@link #worldPosition()} screen projection
   * when the hint is rendered ({@link Graph#render(Node)}). Use
   * {@code enableHint(Node.HUD)}, {@code disableHint(Node.HUD)} and
   * {@code toggleHint(Node.HUD)} to (dis)enable the hint.
   *
   * @see #setHUD(Consumer)
   * @see #resetHUD()
   * @see #resetIMRHUD()
   * @see #resetRMRHUD()
   */
  public void setHUD(processing.core.PShape shape) {
    if (shape == null) {
      resetRMRHUD();
    }
    else {
      _rmrHUD = shape;
      enableHint(HUD);
      _updateHUD();
    }
  }

  /**
   * Sets the node immediate mode rendering (imr) drawing procedure
   * {@link #HUD} hint (see {@link #hint()}). The 2D {@code shape} screen
   * coordinates are interpreted relative to the node {@link #worldPosition()}
   * screen projection when the hint is rendered ({@link Graph#render(Node)}).
   * Use {@code enableHint(Node.HUD)}, {@code disableHint(Node.HUD)} and
   * {@code toggleHint(Node.HUD)} to (dis)enable the hint.
   *
   * @see #setShape(processing.core.PShape)
   * @see #resetHUD()
   * @see #resetIMRHUD()
   * @see #resetRMRHUD()
   */
  public void setHUD(Consumer<processing.core.PGraphics> callback) {
    if (callback == null) {
      resetIMRHUD();
    }
    else {
      _imrHUD = callback;
      enableHint(HUD);
      _updateHUD();
    }
  }

  /**
   * Returns whether or not all hints encoded in the bitwise-or
   * {@code hint} mask are enable for node picking with ray casting.
   *
   * @see #picking()
   * @see #resetPicking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   */
  public boolean isPickingEnabled(int hint) {
    return ~(_picking | ~hint) == 0;
  }

  /**
   * Returns the current visual picking hint mask encoding the
   * visual aspects that are to be taken into account for picking
   * the node with ray casting. Refer to {@link #hint()} to learn
   * how to configure the mask.
   * <p>
   * Note that picking a node from one of its visual aspects requires both
   * the {@link #hint()} and this mask to have enabled the same aspects.
   *
   * @see #picking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #disableHint(int)
   * @see #enableHint(int)
   * @see #toggleHint(int)
   */
  public int picking() {
    return this._picking;
  }

  /**
   * Resets {@link #picking()}, i.e., disables all single
   * visual hints available for node picking with ray casting.
   *
   * @see #picking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #disableHint(int)
   * @see #enableHint(int)
   * @see #toggleHint(int)
   */
  public void resetPicking() {
    _picking = 0;
  }

  /**
   * Disables all the single visual hints encoded in the bitwise-or {@code hint} mask.
   *
   * @see #picking()
   * @see #resetPicking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #disableHint(int)
   * @see #enableHint(int)
   * @see #toggleHint(int)
   */
  public void disablePicking(int hint) {
    _picking &= ~hint;
  }

  /**
   * Enables all single visual hints encoded in the bitwise-or {@code pickingMode} mask.
   *
   * @see #picking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #disableHint(int)
   * @see #enableHint(int)
   * @see #toggleHint(int)
   */
  public void enablePicking(int picking) {
    _picking |= picking;
  }

  /**
   * Toggles all single visual hints encoded in the bitwise-or {@code hint} mask.
   *
   * @see #resetPicking()
   * @see #resetPicking()
   * @see #disablePicking(int)
   * @see #enablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #hint()
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #disableHint(int)
   * @see #enableHint(int)
   * @see #toggleHint(int)
   */
  public void togglePicking(int hint) {
    _picking ^= hint;
  }

  /**
   * Returns whether or not all single visual hints encoded in the bitwise-or
   * {@code hint} mask are enable or not.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #resetHint()
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public boolean isHintEnabled(int hint) {
    return ~(_mask | ~hint) == 0;
  }

  /**
   * Returns the current visual hint mask. The mask is a bitwise-or of the following
   * single visual hints available for the node:
   * <p>
   * <ol>
   * <li>{@link #CAMERA} which displays a camera hint centered at the node screen
   * projection.</li>
   * <li>{@link #AXES} which displays an axes hint centered at the node
   * {@link #worldPosition()} an oriented according to the node {@link #worldOrientation()}.</li>
   * <li>{@link #HUD} which displays the node Heads-Up-Display set with
   * {@link #setHUD(processing.core.PShape)} or {@link #setHUD(Consumer)}.</li>
   * <li>{@link #BOUNDS} which displays the bounding volume of each graph for which
   * this node is the eye. Only meaningful if there's a second scene perspective
   * to look at this eye node from.</li>
   * <li>{@link #SHAPE} which displays the node shape set with
   * {@link #setShape(processing.core.PShape)} or {@link #setShape(Consumer)}.</li>
   * <li>{@link #BULLSEYE} which displays a bullseye centered at the node
   * {@link #worldPosition()} screen projection. Call {@link #setBullsEyeSize(float)}
   * to set the size of the hint</li>
   * <li>{@link #TORUS} which displays a torus solenoid.</li>
   * </ol>
   * Displaying the hint requires first to enabling it (see {@link #enableHint(int)}) and then
   * calling a {@link Graph} rendering algorithm.
   *
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public int hint() {
    return this._mask;
  }

  /**
   * Resets the current {@link #hint()}, i.e., disables all single
   * visual hints available for the node.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void resetHint() {
    _mask = 0;
    _updateHUD();
  }

  /**
   * Disables all the single visual hints encoded in the bitwise-or {@code hint} mask.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #resetHint()
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void disableHint(int hint) {
    _mask &= ~hint;
    _updateHUD();
  }

  /**
   * Calls {@link #enableHint(int)} followed by {@link #configHint(int, Object...)}.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #disableHint(int)
   * @see #resetHint()
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void enableHint(int hint, Object... params) {
    enableHint(hint);
    configHint(hint, params);
  }

  /**
   * Enables all single visual hints encoded in the bitwise-or {@code hint} mask.
   *
   * @see #hint()
   * @see #disableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #resetHint()
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void enableHint(int hint) {
    _mask |= hint;
    _updateHUD();
  }

  /**
   * Toggles all single visual hints encoded in the bitwise-or {@code hint} mask.
   *
   * @see #hint()
   * @see #disableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #resetHint()
   * @see #enableHint(int)
   * @see #isHintEnabled(int)
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void toggleHint(int hint) {
    _mask ^= hint;
    _updateHUD();
  }

  /**
   * Configures the hint using varargs as follows:
   * <p>
   * <ol>
   * <li>{@link #CAMERA} hint: {@code configHint(Node.CAMERA, cameraStroke)} or
   * {@code configHint(Node.CAMERA, cameraStroke, cameraLength)}.</li>
   * <li>{@link #AXES} hint: {@code configHint(Node.AXES, axesLength)}.</li>
   * <li>{@link #BULLSEYE} hint: {@code configHint(Node.BULLSEYE, bullseyeStroke)},
   * {@code configHint(Node.BULLSEYE, bullseyeShape)}, or
   * {@code configHint(Node.BULLSEYE, bullseyeStroke, bullseyeShape)}.</li>
   * <li>{@link #TORUS} hint: configHint(Node.TORUS, torusStroke)}, or
   * configHint(Node.TORUS, torusStroke, torusFaces)}.</li>
   * </ol>
   * Note that the {@code cameraStroke}, {@code splineStroke}, {@code bullseyeStroke}
   * and {@code torusStroke} are color {@code int} vars; {@code cameraLength} and
   * {@code exesLength} are world magnitude numerical values; {@code highlight} is a
   * numerical value in {@code [0..1]} which represents the scale factor to be
   * applied to the node when it gets tagged (see {@link Graph#tag(String, Node)});
   * {@code bullseyeShape} is either of type {@link BullsEyeShape#SQUARE} or
   * {@link BullsEyeShape#CIRCLE}; {@code graph} is of type {@link Graph}; and,
   * {@code graph} may be of type {@link processing.core.PGraphics}.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #resetHint()
   * @see #picking()
   * @see #enablePicking(int)
   * @see #disablePicking(int)
   * @see #togglePicking(int)
   * @see #isPickingEnabled(int)
   * @see #resetPicking()
   */
  public void configHint(int hint, Object... params) {
    switch (params.length) {
      case 1:
        if (hint == BULLSEYE && params[0] instanceof BullsEyeShape) {
          _bullsEyeShape = (BullsEyeShape) params[0];
          return;
        }
        if (hint == BULLSEYE && Graph.isNumInstance(params[0])) {
          _bullsEyeStroke = Graph.castToInt(params[0]);
          return;
        }
        if (hint == AXES && Graph.isNumInstance(params[0])) {
          _axesLength = Graph.castToFloat(params[0]);
          return;
        }
        if (hint == CAMERA && Graph.isNumInstance(params[0])) {
          _cameraStroke = Graph.castToInt(params[0]);
          return;
        }
        if (hint == TORUS && Graph.isNumInstance(params[0])) {
          _torusColor = Graph.castToInt(params[0]);
          return;
        }
        break;
      case 2:
        if (hint == BULLSEYE && params[0] instanceof BullsEyeShape && Graph.isNumInstance(params[1])) {
          _bullsEyeShape = (BullsEyeShape) params[0];
          _bullsEyeStroke = Graph.castToInt(params[1]);
          return;
        }
        if (hint == BULLSEYE && Graph.isNumInstance(params[0]) && params[1] instanceof BullsEyeShape) {
          _bullsEyeStroke = Graph.castToInt(params[0]);
          _bullsEyeShape = (BullsEyeShape) params[1];
          return;
        }
        if (hint == CAMERA) {
          if (Graph.isNumInstance(params[0]) && Graph.isNumInstance(params[1])) {
            _cameraStroke = Graph.castToInt(params[0]);
            _cameraLength = Graph.castToFloat(params[1]);
            return;
          }
        }
        if (hint == TORUS) {
          if (Graph.isNumInstance(params[0]) && Graph.isNumInstance(params[1])) {
            _torusColor = Graph.castToInt(params[0]);
            _torusFaces = Graph.castToInt(params[1]);
            return;
          }
        }
        break;
    }
    System.out.println("Warning: some params in Node.configHint(hint, params) couldn't be parsed!");
  }

  /**
   * Returns whether or not this node is some graph {@link Graph#eye()}.
   */
  public boolean isEye() {
    return !_frustumGraphs.isEmpty();
  }

  /**
   * Returns whether or not this node is the given {@code graph} {@link Graph#eye()}.
   */
  public boolean isEye(Graph graph) {
    return _frustumGraphs.contains(graph);
  }
}
