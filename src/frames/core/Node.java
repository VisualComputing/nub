/****************************************************************************************
 * framesjs
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core;

import frames.input.Agent;
import frames.input.Event;
import frames.input.Grabber;
import frames.input.InputHandler;
import frames.input.event.*;
import frames.primitives.*;
import frames.primitives.constraint.WorldConstraint;
import frames.timing.TimingHandler;
import frames.timing.TimingTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Node is a {@link Frame} element on a {@link Graph} hierarchy, which converts user gestures
 * into translation, rotation and scaling updates (see {@link #translationSensitivity()},
 * {@link #rotationSensitivity()} and {@link #scalingSensitivity()}). A node may be attached
 * to some of your visual objects to control their behavior using an {@link Agent}.
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
 * traversal algorithm (see {@link frames.core.Graph#traverse()}), provided
 * that the node {@link #visit()} method is overridden, as shown below:
 * <p>
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
 * A node may also be defined as the {@link Graph#eye()} (see {@link #isEye()}
 * and {@link Graph#setEye(Frame)}). Some user gestures are then interpreted in a negated way,
 * respect to non-eye nodes. For instance, with a move-to-the-right user gesture the
 * {@link Graph#eye()} has to go to the <i>left</i>, so that the scene seems to move
 * to the right.
 * <h2>Behaviors</h2>
 * To implement a node behavior derive from this class and override {@code interact()}.
 * For example, with the following code:
 * <p>
 * <pre>
 * {@code
 * Shortcut left = new Shortcut(PApplet.LEFT);
 * Shortcut right = new Shortcut(PApplet.RIGHT);
 * node = new Node(graph) {
 *   public void interact(Event event) {
 *     if(left.matches(event.shortcut()))
 *       rotate(event);
 *     if(right.matches(event.shortcut()))
 *       translate(event);
 *   }
 * }
 * }
 * </pre>
 * <p>
 * your custom node will then accordingly react to the LEFT and RIGHT mouse buttons,
 * provided it's added to the mouse-agent first (see {@link Agent#addGrabber(Grabber)}.
 * <p>
 * Note that a node implements by default several gesture-to-motion converting methods,
 * such as: {@link #rotate(Event)}, {@link #moveForward(Event)},
 * {@link #translateXPos()}, etc.
 * <h2>Picking</h2>
 * Picking a node is done accordingly to a {@link #precision()}. Refer to
 * {@link #setPrecision(Precision)} for details.
 * <h2>Syncing</h2>
 * Two nodes can be synced together ({@link #sync(Node, Node)}), meaning that they will
 * share their global parameters (position, orientation and magnitude) taken the one
 * that hasGrabber been most recently updated. Syncing can be useful to share nodes
 * among different off-screen graphs.
 */
public class Node extends Frame implements Grabber {
  // according to space-nav fine tuning it turned out that the space-nav is
  // right handed
  // we thus define our gesture physical space as right-handed as follows:
  // hid.sens should be non-negative for the space-nav to behave as expected
  // from the physical interface
  // TODO: really need to check the second part above. For a fact it's known
  // 1. from the space-bav pov LH vs RH works the same way
  // 2. all space-nav sens are positive
  // Sens
  protected float _rotationSensitivity;
  protected float _translationSensitivity;
  protected float _scalingSensitivity;
  protected float _wheelSensitivity;
  protected float _keySensitivity;

  // spinning stuff:
  protected float _spinningSensitivity;
  protected TimingTask _spinningTask;
  protected Quaternion _spinningQuaternion;
  protected float _damping; // new

  // Whether the SCREEN_TRANS direction (horizontal or vertical) is fixed or not
  public boolean _directionIsFixed;
  protected boolean _horizontal = true; // Two simultaneous nodes require two mice!

  protected float _eventSpeed; // spinning and flying
  protected long _eventDelay;

  // _fly
  protected Vector _flyDirection;
  protected float _flySpeed;
  protected TimingTask _flyTask;
  protected Vector _fly;
  protected long _flyUpdatePeriod = 20;
  //TODO move to Frame? see Graph.setUpVector
  protected Vector _upVector;
  protected Graph _graph;

  protected float _threshold;

  protected boolean _culled;

  // id
  protected int _id;

  /**
   * Enumerates the Picking precision modes.
   */
  public enum Precision {
    FIXED, ADAPTIVE, EXACT
  }

  protected Precision _Precision;

  protected MotionEvent2 _initEvent;
  protected float _flySpeedCache;

  protected List<Node> _children;

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
   * The {@link Graph#inputHandler()} will attempt to addGrabber the node to all its
   * {@link InputHandler#agents()}.
   * <p>
   * The node sensitivities are set to their default values, see
   * {@link #spinningSensitivity()}, {@link #wheelSensitivity()},
   * {@link #keySensitivity()}, {@link #rotationSensitivity()},
   * {@link #translationSensitivity()} and {@link #scalingSensitivity()}.
   * <p>
   * Sets the {@link #precision()} to {@link Precision#FIXED}.
   * <p>
   * After object creation a call to {@link #isEye()} will return {@code false}.
   */
  protected Node(Graph graph, Node reference, Vector translation, Quaternion rotation, float scaling) {
    super(reference, translation, rotation, scaling);
    _graph = graph;
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

    setFlySpeed(0.01f * graph().radius());
    _upVector = new Vector(0.0f, 1.0f, 0.0f);
    _culled = false;
    _children = new ArrayList<Node>();
    // graph()._addLeadingNode(this);
    setReference(reference());// _restorePath seems more robust
    setRotationSensitivity(1.0f);
    setScalingSensitivity(1.0f);
    setTranslationSensitivity(1.0f);
    setWheelSensitivity(15f);
    setKeySensitivity(10f);
    setSpinningSensitivity(0.3f);
    setDamping(0.5f);

    _spinningTask = new TimingTask() {
      public void execute() {
        _spin();
      }
    };
    graph().registerTask(_spinningTask);

    _fly = new Vector(0.0f, 0.0f, 0.0f);
    _flyTask = new TimingTask() {
      public void execute() {
        _fly();
      }
    };
    graph().registerTask(_flyTask);
    // end

    // pkgnPrecision = Precision.ADAPTIVE;
    // setPrecisionThreshold(Math.round(scn.radius()/4));
    graph().inputHandler().addGrabber(this);
    _Precision = Precision.FIXED;
    setPrecisionThreshold(20);
    setFlySpeed(0.01f * graph().radius());
  }

  protected Node(Graph graph, Node other) {
    super(other);
    this._graph = graph;
    if (this.graph() == other.graph()) {
      this._id = ++graph()._nodeCount;
      // unlikely but theoretically possible
      if (this._id == 16777216)
        throw new RuntimeException("Maximum iFrame instances reached. Exiting now!");
    } else {
      this._id = other._id();
      this.setWorldMatrix(other);
    }

    this._upVector = other._upVector.get();
    this._culled = other._culled;

    this._children = new ArrayList<Node>();
    if (this.graph() == other.graph()) {
      this.setReference(reference());// _restorePath
    }

    this._spinningTask = new TimingTask() {
      public void execute() {
        _spin();
      }
    };

    this._graph.registerTask(_spinningTask);

    this._fly = new Vector();
    this._fly.set(other._fly.get());
    this._flyTask = new TimingTask() {
      public void execute() {
        _fly();
      }
    };
    this._graph.registerTask(_flyTask);
    _lastUpdate = other.lastUpdate();
    // end
    // this.isInCamPath = otherFrame.isInCamPath;
    //
    // this.setPrecisionThreshold(otherFrame.precisionThreshold(),
    // otherFrame.adaptiveGrabsInputThreshold());
    this._Precision = other._Precision;
    this._threshold = other._threshold;

    this.setRotationSensitivity(other.rotationSensitivity());
    this.setScalingSensitivity(other.scalingSensitivity());
    this.setTranslationSensitivity(other.translationSensitivity());
    this.setWheelSensitivity(other.wheelSensitivity());
    this.setKeySensitivity(other.keySensitivity());
    //
    this.setSpinningSensitivity(other.spinningSensitivity());
    this.setDamping(other.damping());
    //
    this.setFlySpeed(other.flySpeed());

    if (this.graph() == other.graph()) {
      for (Agent agent : this._graph.inputHandler().agents())
        if (agent.hasGrabber(other))
          agent.addGrabber(this);
    } else {
      this.graph().inputHandler().addGrabber(this);
    }
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

  //_id

  /**
   * Internal use. Frame graphics color to be used for picking with a color buffer.
   */
  protected int _id() {
    // see here:
    // http://stackoverflow.com/questions/2262100/rgb-int-to-rgb-python
    return (255 << 24) | ((_id & 255) << 16) | (((_id >> 8) & 255) << 8) | (_id >> 16) & 255;
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

  // GRAPH

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
    if (settingAsReferenceWillCreateALoop(node)) {
      System.out.println("Frame.setReference would create a loop in Frame hierarchy. Nothing done.");
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
   * Returns a list of the node children, i.e., nodes which {@link #reference()} is this.
   */
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
    for (Node frame : children())
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
   * <p>
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
   * Returns the graph this node belongs to.
   *
   * @see Graph#eye()
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Returns true if this node is the {@link Graph#eye()}, and false otherwise.
   *
   * @see Graph#setEye(Frame)
   * @see Graph#eye()
   */
  public boolean isEye() {
    return graph().eye() == this;
  }

  @Override
  public boolean track(Event event) {
    if (event instanceof KeyEvent)
      return track((KeyEvent) event);
    if (event instanceof TapEvent)
      return track((TapEvent) event);
    if (event instanceof MotionEvent)
      return track((MotionEvent) event);
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   */
  public boolean track(MotionEvent motionEvent) {
    if (isEye())
      return false;
    if (motionEvent instanceof MotionEvent1)
      return track((MotionEvent1) motionEvent);
    if (motionEvent instanceof MotionEvent2)
      return track((MotionEvent2) motionEvent);
    if (motionEvent instanceof MotionEvent3)
      return track((MotionEvent3) motionEvent);
    if (motionEvent instanceof MotionEvent6)
      return track((MotionEvent6) motionEvent);
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   */
  public boolean track(TapEvent tapEvent) {
    if (isEye())
      return false;
    return track(tapEvent.x(), tapEvent.y());
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   * <p>
   * Override this method when you want the object to be picked from a {@link KeyEvent}.
   */
  public boolean track(KeyEvent keyEvent) {
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   * <p>
   * Override this method when you want the object to be picked from a {@link MotionEvent1}.
   */
  public boolean track(MotionEvent1 motionEvent1) {
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   */
  public boolean track(MotionEvent2 motionEvent2) {
    if (isEye())
      return false;
    if (motionEvent2.isAbsolute()) {
      System.out.println("track(Event) requires a relative motion-event");
      return false;
    }
    return track(motionEvent2.x(), motionEvent2.y());
  }

  /**
   * Picks the node according to the {@link #precision()}.
   *
   * @see #precision()
   * @see #setPrecision(Precision)
   */
  public boolean track(float x, float y) {
    Vector proj = _graph.projectedCoordinatesOf(position());
    float halfThreshold = precisionThreshold() / 2;
    return ((Math.abs(x - proj._vector[0]) < halfThreshold) && (Math.abs(y - proj._vector[1]) < halfThreshold));
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   */
  public boolean track(MotionEvent3 motionEvent3) {
    return track(motionEvent3.event2());
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this node.
   */
  public boolean track(MotionEvent6 motionEvent6) {
    return track(motionEvent6.event3().event2());
  }

  @Override
  public void interact(Event event) {
    if (event instanceof TapEvent)
      interact((TapEvent) event);
    if (event instanceof MotionEvent)
      interact((MotionEvent) event);
    if (event instanceof KeyEvent)
      interact((KeyEvent) event);
  }

  /**
   * Calls interact() on the proper motion event:
   * {@link MotionEvent1}, {@link MotionEvent2},
   * {@link MotionEvent3} or {@link MotionEvent6}.
   * <p>
   * Override this method when you want the object to perform an interaction from a
   * {@link frames.input.event.MotionEvent}.
   */
  protected void interact(MotionEvent motionEvent) {
    if (motionEvent instanceof MotionEvent1)
      interact((MotionEvent1) motionEvent);
    if (motionEvent instanceof MotionEvent2)
      interact((MotionEvent2) motionEvent);
    if (motionEvent instanceof MotionEvent3)
      interact((MotionEvent3) motionEvent);
    if (motionEvent instanceof MotionEvent6)
      interact((MotionEvent6) motionEvent);
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link MotionEvent1}.
   */
  protected void interact(MotionEvent1 motionEvent1) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link MotionEvent2}.
   */
  protected void interact(MotionEvent2 motionEvent2) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link MotionEvent3}.
   */
  protected void interact(MotionEvent3 motionEvent3) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link MotionEvent6}.
   */
  protected void interact(MotionEvent6 motionEvent6) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link TapEvent}.
   */
  protected void interact(TapEvent tapEvent) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link KeyEvent}.
   */
  protected void interact(KeyEvent keyEvent) {
  }

  // APPLY TRANSFORMATION

  /**
   * Convenience function that simply calls {@code applyTransformation(graph())}. It applies
   * the transformation defined by this node to {@link #graph()}.
   *
   * @see #applyTransformation(Graph)
   * @see #matrix()
   * @see #graph()
   */
  public void applyTransformation() {
    applyTransformation(graph());
  }

  /**
   * Convenience function that simply calls {@code applyWorldTransformation(graph())}. It
   * applies the world transformation defined by this node to {@link #graph()}.
   *
   * @see #applyWorldTransformation(Graph)
   * @see #worldMatrix()
   * @see #graph()
   */
  public void applyWorldTransformation() {
    applyWorldTransformation(graph());
  }

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
  public void applyTransformation(Graph graph) {
    graph.applyTransformation(this);
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
  public void applyWorldTransformation(Graph graph) {
    graph.applyWorldTransformation(this);
  }

  // MODIFIED

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

  // SYNC

  /**
   * Same as {@code sync(this, other)}.
   *
   * @see #sync(Node, Node)
   */
  public void sync(Node other) {
    sync(this, other);
  }

  /**
   * If {@code node1} has been more recently updated than {@code node2}, calls
   * {@code node2.setWorldMatrix(node1)}, otherwise calls {@code node1.setWorldMatrix(node2)}.
   * Does nothing if both objects were updated at the same frame.
   * <p>
   * This method syncs only the global geometry attributes ({@link #position()},
   * {@link #orientation()} and {@link #magnitude()}) among the two nodes. The
   * {@link #reference()} and {@link #constraint()} (if any) of each node are kept
   * separately.
   *
   * @see #setWorldMatrix(Frame)
   */
  public static void sync(Node node1, Node node2) {
    if (node1 == null || node2 == null)
      return;
    if (node1.lastUpdate() == node2.lastUpdate())
      return;
    Node source = (node1.lastUpdate() > node2.lastUpdate()) ? node1 : node2;
    Node target = (node1.lastUpdate() > node2.lastUpdate()) ? node2 : node1;
    target.setWorldMatrix(source);
  }

  /**
   * Defines the spinning deceleration.
   * <p>
   * Default value is 0.5. Use {@link #setDamping(float)} to tune this value. A higher
   * value will make damping more difficult (a value of 1 forbids damping).
   */
  public float damping() {
    return _damping;
  }

  /**
   * Defines the {@link #damping()}. Values must be in the range [0..1].
   */
  public void setDamping(float damping) {
    if (damping < 0 || damping > 1)
      return;
    _damping = damping;
  }

  /**
   * Defines the {@link #rotationSensitivity()}.
   */
  public void setRotationSensitivity(float sensitivity) {
    _rotationSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #scalingSensitivity()}.
   */
  public void setScalingSensitivity(float sensitivity) {
    _scalingSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #translationSensitivity()}.
   */
  public void setTranslationSensitivity(float sensitivity) {
    _translationSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #spinningSensitivity()}.
   */
  public void setSpinningSensitivity(float sensitivity) {
    _spinningSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #wheelSensitivity()}.
   */
  public void setWheelSensitivity(float sensitivity) {
    _wheelSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #keySensitivity()}.
   */
  public void setKeySensitivity(float sensitivity) {
    _keySensitivity = sensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node rotation.
   * <p>
   * Default value is 1 (for instance matching an identical mouse displacement), a higher
   * value will generate a larger rotation (and inversely for lower values). A 0 value will
   * forbid rotation (see also {@link #constraint()}).
   *
   * @see #setRotationSensitivity(float)
   * @see #translationSensitivity()
   * @see #scalingSensitivity()
   * @see #keySensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public float rotationSensitivity() {
    return _rotationSensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node scaling.
   * <p>
   * Default value is 1, a higher value will generate a larger scaling (and inversely
   * for lower values). A 0 value will forbid scaling (see also {@link #constraint()}).
   *
   * @see #setScalingSensitivity(float)
   * @see #setRotationSensitivity(float)
   * @see #translationSensitivity()
   * @see #keySensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public float scalingSensitivity() {
    return _scalingSensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node translation.
   * <p>
   * Default value is 1 which in the case of a mouse interaction makes the node
   * precisely stays under the mouse cursor.
   * <p>
   * With an identical gesture displacement, a higher value will generate a larger
   * translation (and inversely for lower values). A 0 value will forbid translation
   * (see also {@link #constraint()}).
   *
   * @see #setTranslationSensitivity(float)
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keySensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public float translationSensitivity() {
    return _translationSensitivity;
  }

  /**
   * Returns the minimum gesture speed required to make the node spin.
   * Spinning requires to set {@link #damping()} to 0.
   * <p>
   * See {@link #_spin()}, {@link #spinningQuaternion()} and
   * {@link #startSpinning(Quaternion, float, long)} for details.
   * <p>
   * Gesture speed is expressed in pixels per milliseconds. Default value is 0.3 (300
   * pixels per second). Use {@link #setSpinningSensitivity(float)} to tune this value. A
   * higher value will make spinning more difficult (a value of 100 forbids spinning in
   * practice).
   *
   * @see #setSpinningSensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keySensitivity()
   * @see #wheelSensitivity()
   * @see #setDamping(float)
   */
  public float spinningSensitivity() {
    return _spinningSensitivity;
  }

  /**
   * Returns the wheel sensitivity.
   * <p>
   * Default value is 15. A higher value will make the wheel action more efficient
   * (usually meaning faster motion). Use a negative value to invert the operation
   * direction.
   *
   * @see #setWheelSensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keySensitivity()
   * @see #spinningSensitivity()
   */
  public float wheelSensitivity() {
    return _wheelSensitivity;
  }

  /**
   * Returns the keyboard sensitivity.
   * <p>
   * Default value is 10. A higher value will make the keyboard more efficient (usually
   * meaning faster motion).
   *
   * @see #setKeySensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #wheelSensitivity()
   * @see #setDamping(float)
   */
  public float keySensitivity() {
    return _keySensitivity;
  }

  /**
   * Returns {@code true} when the node is spinning.
   * <p>
   * During spinning, {@link #_spin()} rotates the node by its
   * {@link #spinningQuaternion()} at a frequency defined when the node
   * {@link #startSpinning(Quaternion, float, long)}.
   * <p>
   * Use {@link #startSpinning(Quaternion, float, long)} and {@link #stopSpinning()} to
   * change this state. Default value is {@code false}.
   *
   * @see #isFlying()
   */
  public boolean isSpinning() {
    return _spinningTask.isActive();
  }

  /**
   * Stops the spinning motion _started using {@link #startSpinning(Quaternion, float, long)}.
   * Note that {@link #isSpinning()} will return {@code false} after this call.
   * <p>
   * <b>Attention: </b>This method may be called by {@link #_spin()}, since spinning may be
   * decelerated according to {@link #damping()} till it stops completely.
   *
   * @see #damping()
   */
  public void stopSpinning() {
    _spinningTask.stop();
  }

  /**
   * Starts the spinning of the node.
   * <p>
   * This method starts a timer that will call {@link #_spin()} every
   * {@code updateInterval} milliseconds. The node {@link #isSpinning()} until
   * you call {@link #stopSpinning()}.
   * <p>
   * <b>Attention: </b>Spinning may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   * @see #startFlying(Vector, float)
   */
  public void startSpinning(Quaternion quaternion, float speed, long delay) {
    _spinningQuaternion = quaternion;
    _eventSpeed = speed;
    _eventDelay = delay;
    if (damping() == 0 && _eventSpeed < spinningSensitivity())
      return;
    if (delay > 0)
      _spinningTask.run(delay);
  }

  /**
   * Cache version. Used by rotate methods when damping is 0.
   */
  protected void _startSpinning() {
    startSpinning(_spinningQuaternion, _eventSpeed, _eventDelay);
  }

  protected void _spin() {
    if (damping() == 0)
      spin(_spinningQuaternion);
    else {
      if (_eventSpeed == 0) {
        stopSpinning();
        return;
      }
      spin(_spinningQuaternion);
      _recomputeSpinningQuaternion();
    }
  }

  protected void _spin(Quaternion quaternion, float speed, long delay) {
    if (damping() == 0) {
      _spinningQuaternion = quaternion;
      _eventSpeed = speed;
      _eventDelay = delay;
      spin(_spinningQuaternion);
    } else
      startSpinning(quaternion, speed, delay);
  }

  /*
  protected void _spin(Quaternion quaternion) {
    setSpinningQuaternion(quaternion);
    _spin();
  }
  */

  public void spin(Quaternion quaternion) {
    if (isEye())
      rotateAroundPoint(quaternion, graph().anchor());
    else
      rotate(quaternion);
  }

  /**
   * Rotates the node by its {@link #spinningQuaternion()} or around the {@link Graph#anchor()}
   * when this node is the {@link Graph#eye()}. Called by a timer when the node {@link #isSpinning()}.
   * <p>
   * <b>Attention: </b>Spinning may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   */
  /*
  protected void _spin() {
    if (isEye())
      rotateAroundPoint(_spinningQuaternion, graph().anchor());
    else
      rotate(_spinningQuaternion);
  }
  */

  /**
   * Internal method. Recomputes the {@link #spinningQuaternion()} according to {@link #damping()}.
   */
  protected void _recomputeSpinningQuaternion() {
    float prevSpeed = _eventSpeed;
    float damping = 1.0f - (float) Math.pow(damping(), 3);
    _eventSpeed *= damping;
    if (Math.abs(_eventSpeed) < .001f)
      _eventSpeed = 0;
    // float currSpeed = eventSpeed;
    _spinningQuaternion.fromAxisAngle((_spinningQuaternion).axis(), _spinningQuaternion.angle() * (_eventSpeed / prevSpeed));
  }

  protected int _originalDirection(MotionEvent event) {
    return _originalDirection(event, true);
  }

  protected int _originalDirection(MotionEvent event, boolean fromX) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event, fromX);
    if (motionEvent2 != null)
      return _originalDirection(motionEvent2);
    else
      return 0;
  }

  /**
   * Return 1 if mouse motion was started horizontally and -1 if it was more vertical.
   * Returns 0 if this could not be determined yet (perfect diagonal motion, rare).
   */
  protected int _originalDirection(MotionEvent2 event) {
    if (!_directionIsFixed) {
      Point delta = new Point(event.dx(), event.dy());
      _directionIsFixed = Math.abs(delta.x()) != Math.abs(delta.y());
      _horizontal = Math.abs(delta.x()) > Math.abs(delta.y());
    }

    if (_directionIsFixed)
      if (_horizontal)
        return 1;
      else
        return -1;
    else
      return 0;
  }

  /**
   * Returns a quaternion computed according to the 2-DOF gesture motion, such as those gathered
   * from mice (mouse positions are projected on a deformed ball, centered on ({@code center.x()},
   * {@code center.y()})).
   */
  protected Quaternion _deformedBallQuaternion(MotionEvent2 event, Vector center) {
    if (event.isAbsolute()) {
      System.out.println("deformedBallQuaternion(Event) requires a relative motion-event");
      return null;
    }
    float cx = center.x();
    float cy = center.y();
    float x = event.x();
    float y = event.y();
    float prevX = event.previousX();
    float prevY = event.previousY();
    // Points on the deformed ball
    float px = rotationSensitivity() * ((int) prevX - cx) / _graph.width();
    float py =
        rotationSensitivity() * (_graph.isLeftHanded() ? ((int) prevY - cy) : (cy - (int) prevY)) / _graph.height();
    float dx = rotationSensitivity() * (x - cx) / _graph.width();
    float dy = rotationSensitivity() * (_graph.isLeftHanded() ? (y - cy) : (cy - y)) / _graph.height();

    Vector p1 = new Vector(px, py, _projectOnBall(px, py));
    Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
    // Approximation of rotation angle Should be divided by the _projectOnBall
    // size, but it is 1.0
    Vector axis = p2.cross(p1);
    float angle = 2.0f * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / p1.squaredNorm() / p2.squaredNorm()));
    return new Quaternion(axis, angle);
  }

  /**
   * Returns "pseudo-_distance" from (x,y) to ball of radius size. For a point inside the
   * ball, it is proportional to the euclidean distance to the ball. For a point outside
   * the ball, it is proportional to the inverse of this distance (tends to zero) on the
   * ball, the function is continuous.
   */
  protected float _projectOnBall(float x, float y) {
    // If you change the size value, change angle computation in
    // deformedBallQuaternion().
    float size = 1.0f;
    float size2 = size * size;
    float size_limit = size2 * 0.5f;

    float d = x * x + y * y;
    return d < size_limit ? (float) Math.sqrt(size2 - d) : size_limit / (float) Math.sqrt(d);
  }

  // macro's

  protected float _computeAngle(MotionEvent1 event) {
    return _computeAngle(event.dx());
  }

  protected float _computeAngle() {
    return _computeAngle(1);
  }

  protected float _computeAngle(float dx) {
    return dx * (float) Math.PI / _graph.width();
  }

  protected boolean _wheel(MotionEvent event) {
    return event instanceof MotionEvent1;
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

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translateX(Event event) {
    if (event instanceof MotionEvent)
      translateX((MotionEvent) event);
    else
      System.out.println("translateX(Event) requires a motion event");
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateX(MotionEvent event) {
    translateX(event, true);
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateX(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _translateX(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateX(MotionEvent1 event) {
    _translateX(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  protected void _translateX(MotionEvent1 event, float sensitivity) {
    translate(screenToVector(Vector.multiply(new Vector(isEye() ? -event.dx() : event.dx(), 0, 0), sensitivity)));
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateXPos() {
    _translateX(true);
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateXNeg() {
    _translateX(false);
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  protected void _translateX(boolean right) {
    translate(screenToVector(
        Vector.multiply(new Vector(1, 0), (right ^ this.isEye()) ? keySensitivity() : -keySensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translateY(Event event) {
    if (event instanceof MotionEvent)
      translateY((MotionEvent) event);
    else
      System.out.println("translateY(Event) requires a motion event");
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateY(MotionEvent event) {
    translateY(event, false);
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateY(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _translateY(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateY(MotionEvent1 event) {
    _translateY(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  protected void _translateY(MotionEvent1 event, float sensitivity) {
    translate(
        screenToVector(Vector.multiply(new Vector(0, isEye() ^ _graph.isRightHanded() ? -event.dx() : event.dx()), sensitivity)));
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateYPos() {
    _translateY(true);
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateYNeg() {
    _translateY(false);
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  protected void _translateY(boolean up) {
    translate(screenToVector(
        Vector.multiply(new Vector(0, (up ^ this.isEye() ^ _graph.isLeftHanded()) ? 1 : -1), this.keySensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translateZ(Event event) {
    if (event instanceof MotionEvent)
      translateZ((MotionEvent) event);
    else
      System.out.println("translateZ(Event) requires a motion event");
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZ(MotionEvent event) {
    translateZ(event, true);
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZ(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _translateZ(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZ(MotionEvent1 event) {
    _translateZ(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  protected void _translateZ(MotionEvent1 event, float sensitivity) {
    translate(screenToVector(Vector.multiply(new Vector(0.0f, 0.0f, isEye() ? -event.dx() : event.dx()), sensitivity)));
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZPos() {
    _translateZ(true);
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZNeg() {
    _translateZ(false);
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  protected void _translateZ(boolean up) {
    translate(screenToVector(
        Vector.multiply(new Vector(0.0f, 0.0f, 1), (up ^ this.isEye()) ? -keySensitivity() : keySensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translate(Event event) {
    if (event instanceof MotionEvent)
      translate((MotionEvent) event, true);
    else
      System.out.println("translate(Event) requires a motion event");
  }

  /**
   * User gesture into xy-translation conversion routine.
   */
  public void translate(MotionEvent event) {
    translate(event, true);
  }

  /**
   * User gesture into xy-translation conversion routine.
   */
  public void translate(MotionEvent event, boolean fromX) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event, fromX);
    if (motionEvent2 != null)
      translate(motionEvent2);
    else
      System.out.println("translate(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture into xy-translation conversion routine.
   */
  public void translate(MotionEvent2 event) {
    translate(screenToVector(Vector.multiply(new Vector(isEye() ? -event.dx() : event.dx(),
        (_graph.isRightHanded() ^ isEye()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translateXYZ(Event event) {
    if (event instanceof MotionEvent)
      translateXYZ((MotionEvent) event);
    else
      System.out.println("translateXYZ(Event) requires a motion event");
  }

  /**
   * User gesture into xyz-translation conversion routine.
   */
  public void translateXYZ(MotionEvent event) {
    MotionEvent3 motionEvent3 = MotionEvent.event3(event, true);
    if (motionEvent3 != null)
      translateXYZ(motionEvent3);
    else
      System.out.println("translateXYZ(Event) requires a motion event of at least 3 DOFs");
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void translateRotateXYZ(Event event) {
    if (event instanceof MotionEvent)
      translateRotateXYZ((MotionEvent) event);
    else
      System.out.println("translateY(Event) requires a motion event");
  }

  /**
   * User gesture into xyz-translation and rotation conversion routine.
   */
  public void translateRotateXYZ(MotionEvent event) {
    translateXYZ(event);
    // B. Rotate the iFrame
    rotateXYZ(event);
  }

  /**
   * User gesture into xyz-translation conversion routine.
   */
  public void translateXYZ(MotionEvent3 event) {
    translate(screenToVector(
        Vector.multiply(new Vector(event.dx(), _graph.isRightHanded() ? -event.dy() : event.dy(), -event.dz()),
            this.translationSensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void zoomOnAnchor(Event event) {
    if (event instanceof MotionEvent)
      zoomOnAnchor((MotionEvent) event);
    else
      System.out.println("zoomOnAnchor(Event) requires a motion event");
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchor(MotionEvent event) {
    zoomOnAnchor(event, true);
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchor(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _zoomOnAnchor(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  protected void _zoomOnAnchor(MotionEvent1 event, float sensitivity) {
    Vector direction = Vector.subtract(_graph.anchor(), position());
    if (reference() != null)
      direction = reference().transformOf(direction);
    float delta = event.dx() * sensitivity / _graph.height();
    if (direction.magnitude() > 0.02f * _graph.radius() || delta > 0.0f)
      translate(Vector.multiply(direction, delta));
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchorPos() {
    _zoomOnAnchor(true);
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchorNeg() {
    _zoomOnAnchor(false);
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  protected void _zoomOnAnchor(boolean in) {
    Vector direction = Vector.subtract(_graph.anchor(), position());
    if (reference() != null)
      direction = reference().transformOf(direction);
    float delta = (in ? keySensitivity() : -keySensitivity()) / _graph.height();
    if (direction.magnitude() > 0.02f * _graph.radius() || delta > 0.0f)
      translate(Vector.multiply(direction, delta));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void zoomOnRegion(Event event) {
    if (event instanceof MotionEvent)
      zoomOnRegion((MotionEvent) event);
    else
      System.out.println("zoomOnRegion(Event) requires a motion event");
  }

  /**
   * User gesture into zoom-on-region conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void zoomOnRegion(MotionEvent event) {
    MotionEvent2 dof2 = MotionEvent.event2(event);
    if (dof2 == null) {
      System.out.println("zoomOnRegion(Event) requires a motion event of at least 2 DOFs");
      return;
    }
    zoomOnRegion(dof2);
  }

  /**
   * User gesture into zoom-on-region conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void zoomOnRegion(MotionEvent2 event) {
    if (!isEye()) {
      System.out.println("zoomOnRegion(Event) only makes sense for the eye");
      return;
    }
    if (event.isAbsolute()) {
      System.out.println("zoomOnRegion(Event) requires a relative motion-event");
      return;
    }
    if (event.fired()) {
      _initEvent = event.get();
      //TODO handle me
      //graph.setZoomVisualHint(true);
    } else if (event.flushed()) {
      MotionEvent2 e = new MotionEvent2(_initEvent.get(), event.x(), event.y(), event.modifiers(), event.id());
      //TODO handle me
      //graph.setZoomVisualHint(false);
      int w = (int) Math.abs(e.dx());
      int tlX = (int) e.previousX() < (int) e.x() ? (int) e.previousX() : (int) e.x();
      int h = (int) Math.abs(e.dy());
      int tlY = (int) e.previousY() < (int) e.y() ? (int) e.previousY() : (int) e.y();
      graph().fitScreenRegionInterpolation(new Rectangle(tlX, tlY, w, h));
      //graph().fitScreenRegion(new Rectangle(tlX, tlY, w, h));
    }
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotateX(Event event) {
    if (event instanceof MotionEvent)
      rotateX((MotionEvent) event);
    else
      System.out.println("rotateX(Event) requires a motion event");
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateX(MotionEvent event) {
    rotateX(event, false);
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateX(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _rotateX(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateX(MotionEvent1 event) {
    _rotateX(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  protected void _rotateX(MotionEvent1 event, float sensitivity) {
    _spin(screenToQuaternion(_computeAngle(event) * (isEye() ? -sensitivity : sensitivity), 0, 0), event.speed(), event.delay());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateXPos() {
    _rotateX(true);
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateXNeg() {
    _rotateX(false);
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  protected void _rotateX(boolean up) {
    rotate(screenToQuaternion(_computeAngle() * (up ? keySensitivity() : -keySensitivity()), 0, 0));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotateY(Event event) {
    if (event instanceof MotionEvent)
      rotateY((MotionEvent) event);
    else
      System.out.println("rotateY(Event) requires a motion event");
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateY(MotionEvent event) {
    rotateY(event, true);
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateY(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      _rotateY(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateY(MotionEvent1 event) {
    _rotateY(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  protected void _rotateY(MotionEvent1 event, float sensitivity) {
    _spin(screenToQuaternion(0, _computeAngle(event) * (isEye() ? -sensitivity : sensitivity), 0), event.speed(), event.delay());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateYPos() {
    _rotateY(true);
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateYNeg() {
    _rotateY(false);
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  protected void _rotateY(boolean up) {
    Quaternion rt = screenToQuaternion(0, _computeAngle() * (up ? keySensitivity() : -keySensitivity()), 0);
    rotate(rt);
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotateZ(Event event) {
    if (event instanceof MotionEvent)
      rotateZ((MotionEvent) event);
    else
      System.out.println("rotateZ(Event) requires a motion event");
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZ(MotionEvent event) {
    rotateZ(event, false);
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZ(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event);
    if (motionEvent1 != null)
      _rotateZ(motionEvent1, _wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZ(MotionEvent1 event) {
    _rotateZ(event, _wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  protected void _rotateZ(MotionEvent1 event, float sensitivity) {
    _spin(screenToQuaternion(0, 0, sensitivity * (isEye() ? -_computeAngle(event) : _computeAngle(event))), event.speed(), event.delay());
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZPos() {
    _rotateZ(true);
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZNeg() {
    _rotateZ(false);
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  protected void _rotateZ(boolean up) {
    rotate(screenToQuaternion(0, 0, _computeAngle() * (up ? keySensitivity() : -keySensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotateXYZ(Event event) {
    if (event instanceof MotionEvent)
      rotateXYZ((MotionEvent) event);
    else
      System.out.println("rotateY(Event) requires a motion event");
  }

  /**
   * User gesture into xyz-rotation conversion routine.
   */
  public void rotateXYZ(MotionEvent event) {
    MotionEvent3 motionEvent3 = MotionEvent.event3(event, false);
    if (motionEvent3 != null)
      rotateXYZ(motionEvent3);
    else
      System.out.println("rotateXYZ(Event) requires a motion event of at least 3 DOFs");
  }

  /**
   * User gesture into xyz-rotation conversion routine.
   */
  public void rotateXYZ(MotionEvent3 event) {
    if (event.fired())
      _graph._cadRotationIsReversed = _graph.eye().transformOf(_upVector).y() < 0.0f;
    rotate(screenToQuaternion(
        Vector.multiply(new Vector(_computeAngle(event.dx()), _computeAngle(-event.dy()), _computeAngle(-event.dz())),
            rotationSensitivity())));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotate(Event event) {
    if (event instanceof MotionEvent)
      rotate((MotionEvent) event);
    else
      System.out.println("rotate(Event) requires a motion event");
  }

  /**
   * User gesture into arcball-rotation conversion routine.
   */
  public void rotate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      rotate(motionEvent2);
    else
      System.out.println("rotate(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture into arcball-rotation conversion routine.
   */
  public void rotate(MotionEvent2 event) {
    if (event.isAbsolute()) {
      System.out.println("rotate(Event) requires a relative motion-event");
      return;
    }
    if (event.fired()) {
      stopSpinning();
      _graph._cadRotationIsReversed = _graph.eye().transformOf(_upVector).y() < 0.0f;
    }
    if (event.flushed() && damping() == 0) {
      _startSpinning();
      return;
    }
    if (!event.flushed()) {
      Quaternion rt;
      Vector trns;
      if (isEye())
        rt = _deformedBallQuaternion(event, graph().projectedCoordinatesOf(graph().anchor()));
      else {
        trns = _graph.projectedCoordinatesOf(position());
        rt = _deformedBallQuaternion(event, trns);
        trns = rt.axis();
        trns = _graph.eye().orientation().rotate(trns);
        trns = transformOf(trns);
        rt = new Quaternion(trns, -rt.angle());
      }
      _spin(rt, event.speed(), event.delay());
    }
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void scale(Event event) {
    if (event instanceof MotionEvent)
      scale((MotionEvent) event);
    else
      System.out.println("scale(Event) requires a motion event");
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scale(MotionEvent event) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event);
    if (motionEvent1 != null)
      _scale(motionEvent1, _wheel(event) ? wheelSensitivity() : scalingSensitivity());
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scale(MotionEvent1 event) {
    _scale(event, _wheel(event) ? wheelSensitivity() : scalingSensitivity());
  }

  /**
   * User gesture into scaling conversion routine.
   */
  protected void _scale(MotionEvent1 event, float sensitivity) {
    if (isEye()) {
      float delta = event.dx() * sensitivity;
      float s = 1 + Math.abs(delta) / (float) -_graph.height();
      scale(delta >= 0 ? s : 1 / s);
    } else {
      float delta = event.dx() * sensitivity;
      float s = 1 + Math.abs(delta) / (float) _graph.height();
      scale(delta >= 0 ? s : 1 / s);
    }
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scalePos() {
    _scale(true);
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scaleNeg() {
    _scale(false);
  }

  /**
   * User gesture into scaling conversion routine.
   */
  protected void _scale(boolean up) {
    float s = 1 + Math.abs(keySensitivity()) / (isEye() ? (float) -_graph.height() : (float) _graph.height());
    scale(up ? s : 1 / s);
  }

  /**
   * Use for first person (move forward/backward, lookAround) and cad motion actions.
   */
  protected void _updateUpVector() {
    _upVector = orientation().rotate(new Vector(0.0f, 1.0f, 0.0f));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void lookAround(Event event) {
    if (event instanceof MotionEvent)
      lookAround((MotionEvent) event);
    else
      System.out.println("lookAround(Event) requires a motion event");
  }

  /**
   * User gesture into lookAround conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void lookAround(MotionEvent event) {
    if (!isEye()) {
      System.out.println("lookAround(Event) only makes sense for the eye");
      return;
    }
    rotate(_rollPitchQuaternion(event));
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void moveBackward(Event event) {
    if (event instanceof MotionEvent)
      moveBackward((MotionEvent) event);
    else
      System.out.println("moveBackward(Event) requires a motion event");
  }

  /**
   * User gesture into move-backward conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void moveBackward(MotionEvent event) {
    _moveForward(event, false);
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void moveForward(Event event) {
    if (event instanceof MotionEvent)
      moveForward((MotionEvent) event);
    else
      System.out.println("moveForward(Event) requires a motion event");
  }

  /**
   * User gesture into move-forward conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void moveForward(MotionEvent event) {
    _moveForward(event, true);
  }

  /**
   * User gesture into move-forward conversion routine.
   */
  protected void _moveForward(MotionEvent event, boolean forward) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      _moveForward(motionEvent2, forward);
    else
      System.out.println("moveForward(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture into move-forward conversion routine.
   */
  protected void _moveForward(MotionEvent2 event, boolean forward) {
    if (!isEye()) {
      System.out.println("moveForward(Event) only makes sense for the eye");
      return;
    }
    if (event.fired())
      _updateUpVector();
    else if (event.flushed()) {
      stopFlying();
      return;
    }
    float fSpeed = forward ? -flySpeed() : flySpeed();
    rotate(_rollPitchQuaternion(event));
    _fly.set(0.0f, 0.0f, fSpeed);
    startFlying(rotation().rotate(_fly), event.speed());
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void drive(Event event) {
    if (event instanceof MotionEvent)
      drive((MotionEvent) event);
    else
      System.out.println("drive(Event) requires a motion event");
  }

  /**
   * User gesture into drive conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void drive(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      drive(motionEvent2);
    else
      System.out.println("drive(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture into drive conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void drive(MotionEvent2 event) {
    if (!isEye()) {
      System.out.println("drive(Event) only makes sense for the eye");
      return;
    }
    if (event.fired()) {
      _initEvent = event.get();
      _updateUpVector();
      _flySpeedCache = flySpeed();
    } else if (event.flushed()) {
      setFlySpeed(_flySpeedCache);
      stopFlying();
      return;
    }
    setFlySpeed(0.01f * _graph.radius() * 0.01f * (event.y() - _initEvent.y()));
    rotate(_turnQuaternion(event.event1()));
    _fly.set(0.0f, 0.0f, flySpeed());
    startFlying(rotation().rotate(_fly), event.speed());
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void rotateCAD(Event event) {
    if (event instanceof MotionEvent)
      rotateCAD((MotionEvent) event);
    else
      System.out.println("rotateCAD(Event) requires a motion event");
  }

  /**
   * User gesture into CAD-rotation conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void rotateCAD(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      rotateCAD(motionEvent2);
    else
      System.out.println("rotateCAD(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture into CAD-rotation conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void rotateCAD(MotionEvent2 event) {
    if (!isEye()) {
      System.out.println("rotateCAD(Event) only makes sense for the eye");
      return;
    }
    if (event.isAbsolute()) {
      System.out.println("rotateCAD(Event) requires a relative motion-event");
      return;
    }
    if (event.fired())
      stopSpinning();
    if (event.fired())
      _graph._cadRotationIsReversed = _graph.eye().transformOf(_upVector).y() < 0.0f;
    if (event.flushed() && damping() == 0) {
      _startSpinning();
      return;
    } else {
      // Multiply by 2.0 to get on average about the same _speed as with the
      // deformed ball
      float dx = -2.0f * rotationSensitivity() * event.dx() / _graph.width();
      float dy = 2.0f * rotationSensitivity() * event.dy() / _graph.height();
      if (_graph._cadRotationIsReversed)
        dx = -dx;
      if (_graph.isRightHanded())
        dy = -dy;
      Vector verticalAxis = transformOf(_upVector);
      _spin(Quaternion.multiply(new Quaternion(verticalAxis, dx), new Quaternion(new Vector(1.0f, 0.0f, 0.0f), dy)), event.speed(),
          event.delay());
    }
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void hinge(Event event) {
    if (event instanceof MotionEvent)
      hinge((MotionEvent) event);
    else
      System.out.println("hinge(Event) requires a motion event");
  }

  /**
   * User gesture into hinge conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void hinge(MotionEvent event) {
    MotionEvent6 motionEvent6 = MotionEvent.event6(event);
    if (motionEvent6 != null)
      hinge(motionEvent6);
    else
      System.out.println("hinge(Event) requires a motion event of at least 6 DOFs");
  }

  /**
   * User gesture into hinge conversion routine. Only meaningful if {@link #isEye()}.
   */
  public void hinge(MotionEvent6 event) {
    if (!isEye()) {
      System.out.println("hinge(Event) only makes sense for the eye");
      return;
    }
    // aka google earth navigation
    // 1. Relate the eye reference frame:
    Vector trns = new Vector();
    Vector pos = position();
    Quaternion o = orientation();
    Frame oldRef = reference();
    Node rFrame = new Node(_graph);
    rFrame.setPosition(graph().anchor());
    rFrame.setZAxis(Vector.subtract(pos, graph().anchor()));
    rFrame.setXAxis(xAxis());
    setReference(rFrame);
    setPosition(pos);
    setOrientation(o);
    // 2. Translate the refFrame along its Z-axis:
    float deltaZ = event.dz();
    trns = new Vector(0, deltaZ, 0);
    screenToEye(trns);
    float pmag = trns.magnitude();
    translate(0, 0, (deltaZ > 0) ? -pmag : pmag);
    // 3. Rotate the refFrame around its X-axis -> translate forward-backward
    // the frame on the sphere surface
    float deltaY = _computeAngle(event.dy());
    rFrame.rotate(new Quaternion(new Vector(1, 0, 0), _graph.isRightHanded() ? deltaY : -deltaY));
    // 4. Rotate the refFrame around its Y-axis -> translate left-right the
    // frame on the sphere surface
    float deltaX = _computeAngle(event.dx());
    rFrame.rotate(new Quaternion(new Vector(0, 1, 0), deltaX));
    // 5. Rotate the refFrame around its Z-axis -> look around
    float rZ = _computeAngle(event.drz());
    rFrame.rotate(new Quaternion(new Vector(0, 0, 1), _graph.isRightHanded() ? -rZ : rZ));
    // 6. Rotate the frame around x-axis -> move head up and down :P
    float rX = _computeAngle(event.drx());
    Quaternion q = new Quaternion(new Vector(1, 0, 0), _graph.isRightHanded() ? rX : -rX);
    rotate(q);
    // 7. Unrelate the frame and restore state:
    pos = position();
    o = orientation();
    setReference(oldRef);
    graph().pruneBranch(rFrame);
    setPosition(pos);
    setOrientation(o);
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void screenTranslate(Event event) {
    if (event instanceof MotionEvent)
      screenTranslate((MotionEvent) event);
    else
      System.out.println("screenTranslate(Event) requires a motion event");
  }

  /**
   * User gesture screen-translate conversion routine.
   */
  public void screenTranslate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      screenTranslate(motionEvent2);
    else
      System.out.println("screenTranslate(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture screen-translate conversion routine.
   */
  public void screenTranslate(MotionEvent2 event) {
    if (event.fired())
      _directionIsFixed = false;
    int dir = _originalDirection(event);
    if (dir == 1)
      translateX(event, true);
    else if (dir == -1)
      translateY(event, false);
  }

  /**
   * Java ugliness and madness requires this one. Should NOT be implemented in JS (due to its dynamism).
   */
  public void screenRotate(Event event) {
    if (event instanceof MotionEvent)
      screenRotate((MotionEvent) event);
    else
      System.out.println("screenRotate(Event) requires a motion event");
  }

  /**
   * User gesture screen-rotation conversion routine.
   */
  public void screenRotate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      screenRotate(motionEvent2);
    else
      System.out.println("screenRotate(Event) requires a motion event of at least 2 DOFs");
  }

  /**
   * User gesture screen-rotation conversion routine.
   */
  public void screenRotate(MotionEvent2 event) {
    if (event.isAbsolute()) {
      System.out.println("screenRotate(Event) requires a relative motion-event");
      return;
    }
    if (event.fired()) {
      stopSpinning();
      //TODO handle me
      //graph.setRotateVisualHint(true); // display visual hint
      _graph._cadRotationIsReversed = _graph.eye().transformOf(_upVector).y() < 0.0f;
    }
    if (event.flushed()) {
      //TODO handle me
      //graph.setRotateVisualHint(false);
      if (damping() == 0) {
        _startSpinning();
        return;
      }
    }
    if (!event.flushed()) {
      Quaternion rt;
      Vector trns;
      float angle;
      if (isEye()) {
        trns = graph().projectedCoordinatesOf(graph().anchor());
        angle = (float) Math.atan2(event.y() - trns._vector[1], event.x() - trns._vector[0]) - (float) Math
            .atan2(event.previousY() - trns._vector[1], event.previousX() - trns._vector[0]);
        if (_graph.isLeftHanded())
          angle = -angle;
        rt = new Quaternion(new Vector(0.0f, 0.0f, 1.0f), angle);
      } else {
        trns = _graph.projectedCoordinatesOf(position());
        float prev_angle = (float) Math.atan2(event.previousY() - trns._vector[1], event.previousX() - trns._vector[0]);
        angle = (float) Math.atan2(event.y() - trns._vector[1], event.x() - trns._vector[0]);
        Vector axis = transformOf(_graph.eye().orientation().rotate(new Vector(0.0f, 0.0f, -1.0f)));
        if (_graph.isRightHanded())
          rt = new Quaternion(axis, angle - prev_angle);
        else
          rt = new Quaternion(axis, prev_angle - angle);
      }
      _spin(rt, event.speed(), event.delay());
    }
  }

  /**
   * User gesture into anchor from pixel conversion routine.
   */
  //TODO missed
  /*
  public void anchorFromPixel(TapEvent _event) {
    if (isEye())
      graph().setAnchorFromPixel(new Point(_event.x(), _event.y()));
    else
      Graph.showOnlyEyeWarning("anchorFromPixel");
  }
  */

  // Quite nice

  /**
   * Same as {@code return screenToVector(new Vector(x, y, z))}.
   *
   * @see #screenToVector(Vector)
   */
  public Vector screenToVector(float x, float y, float z) {
    return screenToVector(new Vector(x, y, z));
  }

  /**
   * Same as {@code return eyeToReferenceFrame(screenToEye(vector))}. Transforms the vector
   * from screen (device) coordinates to {@link #reference()} coordinates.
   *
   * @see #screenToEye(Vector)
   * @see #eyeToReferenceFrame(Vector)
   */
  public Vector screenToVector(Vector vector) {
    return eyeToReferenceFrame(screenToEye(vector));
  }

  /**
   * Same as {@code return eyeToReferenceFrame(new Vector(x, y, z))}.
   *
   * @see #eyeToReferenceFrame(Vector)
   */
  public Vector eyeToReferenceFrame(float x, float y, float z) {
    return eyeToReferenceFrame(new Vector(x, y, z));
  }

  /**
   * Converts the vector from eye coordinates to {@link #reference()} coordinates.
   * <p>
   * It's worth noting that all gesture to node motion converting methods, are
   * implemented from just {@link #screenToEye(Vector)}, {@link #eyeToReferenceFrame(Vector)}
   * and {@link #screenToQuaternion(float, float, float)}.
   *
   * @see #screenToEye(Vector)
   * @see #screenToQuaternion(float, float, float)
   */
  public Vector eyeToReferenceFrame(Vector vector) {
    Frame gFrame = isEye() ? this : /* respectToEye() ? */_graph.eye() /* : this */;
    Vector t = gFrame.inverseTransformOf(vector);
    if (reference() != null)
      t = reference().transformOf(t);
    return t;
  }

  /**
   * Same as {@code return screenToEye(new Vector(x, y, z))}.
   *
   * @see #screenToEye(Vector)
   */
  public Vector screenToEye(float x, float y, float z) {
    return screenToEye(new Vector(x, y, z));
  }

  /**
   * Converts the vector from screen (device) coordinates into eye coordinates.
   * <p>
   * It's worth noting that all gesture to node motion converting methods, are
   * implemented from just {@link #screenToEye(Vector)}, {@link #eyeToReferenceFrame(Vector)}
   * and {@link #screenToQuaternion(float, float, float)}.
   *
   * @see #eyeToReferenceFrame(Vector)
   * @see #screenToQuaternion(float, float, float)
   */
  public Vector screenToEye(Vector vector) {
    Vector eyeVector = vector.get();
    // Scale to fit the screen relative _event displacement
    // Quite excited to see how simple it's in 2d:
    //if (_graph.is2D())
    //return eyeVector;
    // ... and amazed as to how dirty it's in 3d:
    switch (_graph.type()) {
      case PERSPECTIVE:
        float k = (float) Math.tan(_graph.fieldOfView() / 2.0f) * Math.abs(
            _graph.eye().coordinatesOf(isEye() ? graph().anchor() : position())._vector[2] * _graph.eye().magnitude());
        // * Math.abs(graph.eye().frame().coordinatesOf(isEye() ?
        // graph.eye().anchor() : position()).vec[2]);
        //TODO check me weird to find height instead of width working (may it has to do with fov?)
        eyeVector._vector[0] *= 2.0 * k / _graph.height();
        eyeVector._vector[1] *= 2.0 * k / _graph.height();
        break;
      case TWO_D:
      case ORTHOGRAPHIC:
        float[] wh = _graph.boundaryWidthHeight();
        // float[] wh = graph.eye().getOrthoWidthHeight();
        eyeVector._vector[0] *= 2.0 * wh[0] / _graph.width();
        eyeVector._vector[1] *= 2.0 * wh[1] / _graph.height();
        break;
    }
    float coef;
    if (isEye()) {
      // float coef = 8E-4f;
      coef = Math.max(Math.abs((coordinatesOf(graph().anchor()))._vector[2] * magnitude()), 0.2f * graph().radius());
      eyeVector._vector[2] *= coef / graph().height();
      // eye _wheel seems different
      // trns.vec[2] *= coef * 8E-4f;
      eyeVector.divide(graph().eye().magnitude());
    } else {
      coef = Vector.subtract(_graph.eye().position(), position()).magnitude();
      eyeVector._vector[2] *= coef / _graph.height();
      eyeVector.divide(_graph.eye().magnitude());
    }
    // if( isEye() )
    return eyeVector;
  }

  /**
   * Same as {@code return screenToQuaternion(angles.vec[0], angles.vec[1], angles.vec[2])}.
   *
   * @see #screenToQuaternion(float, float, float)
   */
  public Quaternion screenToQuaternion(Vector angles) {
    return screenToQuaternion(angles._vector[0], angles._vector[1], angles._vector[2]);
  }

  /**
   * Reduces the screen (device)
   * <a href="http://en.wikipedia.org/wiki/Euler_angles#Extrinsic_rotations"> Extrinsic
   * rotation</a> into a {@link Quaternion}.
   * <p>
   * It's worth noting that all gesture to node motion converting methods, are
   * implemented from just {@link #screenToEye(Vector)}, {@link #eyeToReferenceFrame(Vector)}
   * and {@link #screenToQuaternion(float, float, float)}.
   *
   * @param roll  Rotation angle in radians around the screen x-Axis
   * @param pitch Rotation angle in radians around the screen y-Axis
   * @param yaw   Rotation angle in radians around the screen z-Axis
   * @see Quaternion#fromEulerAngles(float, float, float)
   */
  public Quaternion screenToQuaternion(float roll, float pitch, float yaw) {
    // don't really need to differentiate among the two cases, but eyeFrame can
    // be speeded up
    if (isEye() /* || (!isEye() && !this.respectToEye()) */) {
      return new Quaternion(_graph.isLeftHanded() ? -roll : roll, pitch, _graph.isLeftHanded() ? -yaw : yaw);
    } else {
      Vector trns = new Vector();
      Quaternion q = new Quaternion(_graph.isLeftHanded() ? roll : -roll, -pitch, _graph.isLeftHanded() ? yaw : -yaw);
      trns.set(-q.x(), -q.y(), -q.z());
      trns = _graph.eye().orientation().rotate(trns);
      trns = transformOf(trns);
      q.setX(trns.x());
      q.setY(trns.y());
      q.setZ(trns.z());
      return q;
    }
  }

  //TODO needs testing
  @Override
  public void rotateAroundFrame(float roll, float pitch, float yaw, Frame frame) {
    if (frame != null) {
      Frame axis = frame.detach();
      Frame copy = detach();
      copy.setReference(axis);
      axis.rotate(new Quaternion(_graph.isLeftHanded() ? -roll : roll, pitch, _graph.isLeftHanded() ? -yaw : yaw));
      setWorldMatrix(copy);
    }
  }

  /**
   * Returns {@code true} when the node is flying.
   * <p>
   * When flying, {@link #damping()} translates the node by its {@link #flyDirection()}
   * at a frequency defined when the node {@link #startFlying(Vector, float)}.
   * <p>
   * Use {@link #startFlying(Vector, float)} and {@link #stopFlying()} to change this
   * state. Default value is {@code false}.
   * <p>
   * {@link #isSpinning()}
   */
  public boolean isFlying() {
    return _flyTask.isActive();
  }

  /**
   * Stops the flying motion started using {@link #startFlying(Vector, float)}.
   * {@link #isFlying()} will return {@code false} after this call.
   * <p>
   * <b>Attention: </b>This method may be called by {@link #damping()}, since flying may
   * be decelerated according to {@link #damping()} till it stops completely.
   *
   * @see #damping()
   * @see #stopSpinning()
   */
  public void stopFlying() {
    _flyTask.stop();
  }

  /**
   * Returns the incremental translation that is applied by {@link #damping()} to the
   * node position when it {@link #isFlying()}.
   * <p>
   * Default value is no translation. Use {@link #setFlyDirection(Vector)} to change this
   * value.
   * <p>
   * <b>Attention: </b>Flying may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #spinningQuaternion()
   */
  public Vector flyDirection() {
    return _flyDirection;
  }

  /**
   * Defines the {@link #flyDirection()} in the reference frame coordinate system.
   *
   * @see #setSpinningQuaternion(Quaternion)
   */
  public void setFlyDirection(Vector dir) {
    _flyDirection = dir;
  }

  /**
   * Starts the flying of the node.
   * <p>
   * This method starts a timer that will call {@link #damping()} every 20
   * milliseconds. The node {@link #isFlying()} until you call
   * {@link #stopFlying()}.
   * <p>
   * <b>Attention: </b>Flying may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   * @see #_spin()
   * @see #startSpinning(Quaternion, float, long)
   */
  public void startFlying(Vector direction, float speed) {
    _eventSpeed = speed;
    setFlyDirection(direction);
    _flyTask.run(_flyUpdatePeriod);
  }

  /**
   * Translates the node by its {@link #flyDirection()}. Invoked by
   * {@link #_moveForward(MotionEvent, boolean)} and {@link #drive(MotionEvent)}.
   * <p>
   * <b>Attention: </b>Flying may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #_spin()
   */
  protected void _fly() {
    translate(flyDirection());
  }

  /**
   * Returns the fly speed, expressed in graph units.
   * <p>
   * It corresponds to the incremental displacement that is periodically applied to the
   * node by {@link #_moveForward(MotionEvent, boolean)}.
   * <p>
   * <b>Attention:</b> When the node is set as the {@link Graph#eye()}, this value is set according
   * to the {@link Graph#radius()} by {@link Graph#setRadius(float)}.
   */
  public float flySpeed() {
    return _flySpeed;
  }

  /**
   * Sets the {@link #flySpeed()}, defined in  graph units.
   * <p>
   * Default value is 0, but it is modified according to the {@link Graph#radius()} when the node
   * is set as the {@link Graph#eye()}.
   */
  public void setFlySpeed(float speed) {
    _flySpeed = speed;
  }

  protected Quaternion _rollPitchQuaternion(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      return _rollPitchQuaternion(motionEvent2);
    else {
      System.out.println("rollPitchQuaternion(Event) requires a motion event of at least 2 DOFs");
      return null;
    }
  }

  /**
   * Returns a Quaternion that is the composition of two rotations, inferred from the
   * 2-DOF gesture (e.g., mouse) roll (X axis) and pitch.
   */
  protected Quaternion _rollPitchQuaternion(MotionEvent2 event) {
    float deltaX = event.dx();
    float deltaY = event.dy();

    if (_graph.isRightHanded())
      deltaY = -deltaY;

    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), rotationSensitivity() * deltaY / graph().height());
    Quaternion rotY = new Quaternion(transformOf(_upVector), rotationSensitivity() * (-deltaX) / graph().width());
    return Quaternion.multiply(rotY, rotX);
  }

  // drive:

  /**
   * Returns a Quaternion that is a rotation around Y-axis, proportional to the horizontal
   * event X-displacement.
   */
  protected Quaternion _turnQuaternion(MotionEvent1 event) {
    float deltaX = event.dx();
    return new Quaternion(new Vector(0.0f, 1.0f, 0.0f), rotationSensitivity() * (-deltaX) / graph().width());
  }

  // end decide

  /**
   * Returns the picking precision threshold in pixels used by the node to {@link #track(Event)}.
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
    return _Precision;
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
    _Precision = precision;
  }

  /**
   * Sets the length of the squared area around the node {@link #center()} screen
   * projection that defined the {@link #track(Event)} condition used for
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
   * @see #track(Event)
   */
  public void setPrecisionThreshold(float threshold) {
    if (threshold >= 0)
      _threshold = threshold;
  }

  /**
   * Check if this node is the {@link Agent#inputGrabber()}. Returns
   * {@code true} if this object grabs the agent and {@code false} otherwise.
   */
  public boolean grabsInput(Agent agent) {
    return agent.inputGrabber() == this;
  }

  /**
   * Checks if the node grabs input from any agent registered at the graph input-handler.
   */
  public boolean grabsInput() {
    for (Agent agent : _graph.inputHandler().agents()) {
      if (agent.inputGrabber() == this)
        return true;
    }
    return false;
  }


  //SOME USEFUL METHODS FOR IK

  /**
   * Set orientation of a Node in the following way:
   * When Graph is 3D:
   * 1. Z-Axis points to direction vector
   * 2. Y-Axis will be orthogonal to Z-Axis Parent Node and Z-Axis.
   * 3. Y-Axis Parent Node and Y-Axis must have same Sign
   * When Graph is 2D:
   * 1. X-Axis points to direction vector
   * Returns the Quaternion that allows the described Transformation
   **/
  //TODO discard me
  protected Quaternion _fixRotation(Node child) {
    Vector direction = child.translation();
    Vector x = new Vector(1, 0, 0);
    Vector y = new Vector(0, 1, 0);
    Vector z = new Vector(0, 0, 1);
    Quaternion rotation = rotation().inverse();
    Quaternion inverse_rotation;
    rotate(rotation);
    if (this.graph().is3D()) {
      /*First we get the rotation required to set Z-Axis in the direction of the child new position*/
      Quaternion z_rotation = new Quaternion(z, rotation.inverse().rotate(direction));
      rotate(z_rotation);
      /*Then we let Y-Axis to be orthogonal to Z_parent and Z*/
      Vector z_parent = localTransformOf(z);
      Vector y_direction = new Vector();
      Vector.cross(z_parent, z, y_direction);
      //Check dot product
      if (z_parent.dot(z) < 0) {
        y_direction.multiply(-1);
      }
      /*Get how much Y-Axis must rotate*/
      if (y_direction.magnitude() > 1e-6f) {
        Quaternion y_rotation = new Quaternion(y, y_direction);
        rotate(y_rotation);
        rotation.compose(Quaternion.compose(z_rotation, y_rotation));
        inverse_rotation = rotation.inverse();
      } else {
        rotation.compose(z_rotation);
        inverse_rotation = rotation.inverse();
      }
    } else {
      /*First we get the rotation required to set X-Axis in the direction of the child new position*/
      rotation.compose(new Quaternion(x, direction));
      inverse_rotation = rotation.inverse();
      rotate(rotation);
    }
    //Apply inverse rotation to each children
    for (int i = 0; i < children().size(); i++) {
      children().get(i).setRotation(Quaternion.compose(inverse_rotation, children().get(i).rotation()));
      children().get(i).setTranslation(new Vector(0, 0, children().get(i).translation().magnitude()));
    }
    return rotation;
  }

  /**
   * From the branch specified by this as root, change Nodes rotation to follow  Standard Notation
   */
  public void setupHierarchy() {
    if (children().isEmpty()) setRotation(new Quaternion());
    if (children().size() == 1) _fixRotation(children().get(0));
    for (Node node : children()) {
      node.setupHierarchy();
    }
  }
}
