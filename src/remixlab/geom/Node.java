/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.geom;

import remixlab.bias.Agent;
import remixlab.bias.Event;
import remixlab.bias.Grabber;
import remixlab.bias.InputHandler;
import remixlab.bias.event.*;
import remixlab.fpstiming.TimingHandler;
import remixlab.fpstiming.TimingTask;
import remixlab.primitives.*;
import remixlab.primitives.constraint.AxisPlaneConstraint;
import remixlab.primitives.constraint.LocalConstraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link Frame} implementing the
 * {@link Grabber} interface, which converts user gestures into
 * translation, rotation and scaling {@link Frame} updates (see
 * {@link #translationSensitivity()}, {@link #rotationSensitivity()} and
 * {@link #scalingSensitivity()}). A node may thus be attached to some of your
 * graph objects to control their motion using an {@link Agent}, such
 * as the {@link Graph#motionAgent()} and the
 * {@link Graph#keyAgent()} (see
 * {@link #Node(Graph)} and all the constructors that take an graph
 * parameter). To attach a node to {@code MyObject} use code like this:
 * <p>
 * <pre>
 * {@code
 * public class MyObject {
 *   public Node gFrame;
 *
 *   public void draw() {
 *     gFrame.graph().pushModelView();
 *     gFrame.applyWorldTransformation();
 *     drawMyObject();
 *     gFrame.graph().popModelView();
 *   }
 * }
 * }
 * </pre>
 * <p>
 * See {@link #applyTransformation()}, {@link #applyWorldTransformation()},
 * {@link #graph()}, {@link Graph#pushModelView()} and
 * {@link Graph#popModelView()}
 * <p>
 * A frame may also be defined as the {@link Graph#eye()} (see {@link #isEye()}
 * and {@link Graph#setEye(Frame)}).
 * Some user gestures are then interpreted in a negated way, respect to non-eye nodes.
 * For instance, with a move-to-the-right user gesture the
 * {@link Graph#eye()} hasGrabber to go to the <i>left</i>,
 * so that the <i>graph</i> seems to move to the right. A interactive-frame can be set
 * as the {@link Graph#eye()}, see {@link Graph#setEye(Frame)}.
 * <p>
 * This class provides several gesture-to-motion converting methods, such as:
 * {@link #rotate(MotionEvent)}, {@link #moveForward(MotionEvent2, boolean)},
 * {@link #translateX(boolean)}, etc. To use them, derive from this class and override the
 * version of {@code interact} with the (bogus-event) parameter type you want to
 * customize (see {@link #interact(MotionEvent)},
 * {@link #interact(KeyEvent)}, etc.). For example, with the following
 * code:
 * <p>
 * <pre>
 * {@code
 * protected void interact(MotionEvent2 event) {
 *   if(event.id() == LEFT)
 *     gestureArcball(event);
 *   if(event.id() == RIGHT)
 *     gestureTranslateXY(event);
 * }
 * }
 * </pre>
 * <p>
 * your custom node will then accordingly react to the LEFT and RIGHT mouse
 * buttons, provided it's added to the mouse-agent first (see
 * {@link Agent#addGrabber(Grabber)}.
 * <p>
 * Picking a node is done accordingly to a {@link #pickingPrecision()}. Refer to
 * {@link #setPickingPrecision(PickingPrecision)} for details.
 * <p>
 * A node is loosely-coupled with the graph object used to instantiate it, i.e.,
 * the transformation it represents may be applied to a different graph. See
 * {@link #applyTransformation()} and {@link #applyTransformation(Graph)}.
 * <p>
 * Two generic-nodes can be synced together ({@link #sync(Node, Node)}),
 * meaning that they will share their global parameters (position, orientation and
 * magnitude) taken the one that hasGrabber been most recently updated. Syncing can be useful to
 * share nodes among different off-screen scenes (see ProScene's EyeCrane and the
 * AuxiliarViewer examples).
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
  private float rotSensitivity;
  private float transSensitivity;
  private float sclSensitivity;
  private float wheelSensitivity;
  private float keySensitivity;

  // spinning stuff:
  private float spngSensitivity;
  private TimingTask spinningTimerTask;
  private Quaternion spngRotation;
  protected float dampFriction; // new
  // toss and spin share the damp var:
  private float sFriction; // new

  // Whether the SCREEN_TRANS direction (horizontal or vertical) is fixed or not
  public boolean dirIsFixed;
  private boolean horiz = true; // Two simultaneous nodes require two mice!

  protected float eventSpeed; // spnning and tossing
  protected long eventDelay;

  // fly
  protected Vector fDir;
  protected float flySpd;
  protected TimingTask flyTimerTask;
  protected Vector flyDisp;
  protected static final long FLY_UPDATE_PERDIOD = 20;
  //TODO move to Frame? see Graph.setUpVector
  protected Vector upVector;
  protected Graph graph;

  private float grabsInputThreshold;

  private boolean visit;

  // id
  protected int id;

  /**
   * Enumerates the Picking precision modes.
   */
  public enum PickingPrecision {
    FIXED, ADAPTIVE, EXACT
  }

  protected PickingPrecision pkgnPrecision;

  public MotionEvent2 initEvent;
  private float flySpeedCache;

  protected List<Node> childrenList;

  /**
   * Same as {@code this(graph, null, new Vector(), new Quaternion(), 1)}.
   *
   * @see #Node(Graph, Node, Vector, Quaternion, float)
   */
  public Node(Graph graph) {
    this(graph, null, new Vector(), new Quaternion(), 1);
  }

  /**
   * Same as
   * {@code this(reference.graph(), reference, new Vector(), scn.is3D() ? new Quaternion() : new Rot(), 1)}
   * .
   *
   * @see #Node(Graph, Node, Vector, Quaternion, float)
   */
  public Node(Node referenceFrame) {
    this(referenceFrame.graph(), referenceFrame, new Vector(), new Quaternion(), 1);
  }

  /**
   * Creates a graph node with {@code reference} as
   * {@link #reference()}, and {@code p}, {@code r} and {@code s} as the frame
   * {@link #translation()}, {@link #rotation()} and {@link #scaling()}, respectively.
   * <p>
   * The {@link Graph#inputHandler()} will attempt to addGrabber
   * the node to all its {@link InputHandler#agents()}, such
   * as the {@link Graph#motionAgent()} and the
   * {@link Graph#keyAgent()}.
   * <p>
   * The node sensitivities are set to their default values, see
   * {@link #spinningSensitivity()}, {@link #wheelSensitivity()},
   * {@link #keyboardSensitivity()}, {@link #rotationSensitivity()},
   * {@link #translationSensitivity()} and {@link #scalingSensitivity()}.
   * <p>
   * Sets the {@link #pickingPrecision()} to {@link PickingPrecision#FIXED}.
   * <p>
   * After object creation a call to {@link #isEye()} will return {@code false}.
   */
  protected Node(Graph scn, Node referenceFrame, Vector p, Quaternion r, float s) {
    super(referenceFrame, p, r, s);
    graph = scn;
    id = ++graph().nodeCount;
    // unlikely but theoretically possible
    if (id == 16777216)
      throw new RuntimeException("Maximum iFrame instances reached. Exiting now!");

    if(graph().is2D()) {
      LocalConstraint constraint2D = new LocalConstraint();
      //constraint2D.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vector(0, 0, 1));
      constraint2D.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(0, 0, 1));
      setConstraint(constraint2D);
    }

    setFlySpeed(0.01f * graph().radius());
    upVector = new Vector(0.0f, 1.0f, 0.0f);
    visit = true;
    childrenList = new ArrayList<Node>();
    // graph().addLeadingNode(this);
    setReference(reference());// restorePath seems more robust
    setRotationSensitivity(1.0f);
    setScalingSensitivity(1.0f);
    setTranslationSensitivity(1.0f);
    setWheelSensitivity(15f);
    setKeyboardSensitivity(10f);
    setSpinningSensitivity(0.3f);
    setDamping(0.5f);

    spinningTimerTask = new TimingTask() {
      public void execute() {
        spinExecution();
      }
    };
    graph().registerTimingTask(spinningTimerTask);

    flyDisp = new Vector(0.0f, 0.0f, 0.0f);
    flyTimerTask = new TimingTask() {
      public void execute() {
        fly();
      }
    };
    graph().registerTimingTask(flyTimerTask);
    // end

    // pkgnPrecision = PickingPrecision.ADAPTIVE;
    // setGrabsInputThreshold(Math.round(scn.radius()/4));
    graph().inputHandler().addGrabber(this);
    pkgnPrecision = PickingPrecision.FIXED;
    setGrabsInputThreshold(20);
    setFlySpeed(0.01f * graph().radius());
  }

  protected Node(Graph grp, Node otherNode) {
    super(otherNode);
    this.graph = grp;
    if(this.graph() == otherNode.graph()) {
      this.id = ++graph().nodeCount;
      // unlikely but theoretically possible
      if (this.id == 16777216)
        throw new RuntimeException("Maximum iFrame instances reached. Exiting now!");
    }
    else {
      this.id = otherNode.id();
      this.setWorldMatrix(otherNode);
    }

    this.upVector = otherNode.upVector.get();
    this.visit = otherNode.visit;

    this.childrenList = new ArrayList<Node>();
    if(this.graph() == otherNode.graph()) {
      this.setReference(reference());// restorePath
    }

    this.spinningTimerTask = new TimingTask() {
      public void execute() {
        spinExecution();
      }
    };

    this.graph.registerTimingTask(spinningTimerTask);

    this.flyDisp = new Vector();
    this.flyDisp.set(otherNode.flyDisp.get());
    this.flyTimerTask = new TimingTask() {
      public void execute() {
        fly();
      }
    };
    this.graph.registerTimingTask(flyTimerTask);
    lastUpdate = otherNode.lastUpdate();
    // end
    // this.isInCamPath = otherFrame.isInCamPath;
    //
    // this.setGrabsInputThreshold(otherFrame.grabsInputThreshold(),
    // otherFrame.adaptiveGrabsInputThreshold());
    this.pkgnPrecision = otherNode.pkgnPrecision;
    this.grabsInputThreshold = otherNode.grabsInputThreshold;

    this.setRotationSensitivity(otherNode.rotationSensitivity());
    this.setScalingSensitivity(otherNode.scalingSensitivity());
    this.setTranslationSensitivity(otherNode.translationSensitivity());
    this.setWheelSensitivity(otherNode.wheelSensitivity());
    this.setKeyboardSensitivity(otherNode.keyboardSensitivity());
    //
    this.setSpinningSensitivity(otherNode.spinningSensitivity());
    this.setDamping(otherNode.damping());
    //
    this.setFlySpeed(otherNode.flySpeed());

    if(this.graph() == otherNode.graph()) {
      for (Agent agent : this.graph.inputHandler().agents())
        if (agent.hasGrabber(otherNode))
          agent.addGrabber(this);
    }
    else {
      this.graph().inputHandler().addGrabber(this);
    }
  }

  /**
   * Perform a deep, non-recursive copy of this node.
   * <p>
   * The copied frame will keep this frame {@link #reference()}, but its children
   * aren't copied.
   *
   * @return node copy
   */
  @Override
  public Node get() {
    return new Node(this.graph(), this);
  }

  //id

  /**
   * Internal use. Frame graphics color to be used for picking with a color buffer.
   */
  public int id() {
    // see here:
    // http://stackoverflow.com/questions/2262100/rgb-int-to-rgb-python
    return (255 << 24) | ((id & 255) << 16) | (((id >> 8) & 255) << 8) | (id >> 16) & 255;
  }

  // GRAPH

  @Override
  public Node reference() {
    return (Node) this.refFrame;
  }

  @Override
  public void setReference(Frame frame) {
    if (frame instanceof Node || frame == null)
      setReference((Node) frame);
    else
      System.out.println("Warning: nothing done: Generic.reference() should be instanceof Node");
  }

  public void setReference(Node node) {
    if (settingAsReferenceWillCreateALoop(node)) {
      System.out.println("Frame.setReference would create a loop in Frame hierarchy. Nothing done.");
      return;
    }
    // 1. no need to re-parent, just check this needs to be added as leadingFrame
    if (reference() == node) {
      restorePath(reference(), this);
      return;
    }
    // 2. else re-parenting
    // 2a. before assigning new reference frame
    if (reference() != null) // old
      reference().removeChild(this);
    else if (graph() != null)
      graph().removeLeadingNode(this);
    // finally assign the reference frame
    refFrame = node;// reference() returns now the new value
    // 2b. after assigning new reference frame
    restorePath(reference(), this);
    modified();
  }

  protected void restorePath(Node parent, Node child) {
    if (parent == null) {
      if (graph() != null)
        graph().addLeadingNode(child);
    } else {
      if (!parent.hasChild(child)) {
        parent.addChild(child);
        restorePath(parent.reference(), parent);
      }
    }
  }

  /**
   * Returns a list of the frame children, i.e., frame which {@link #reference()} is
   * this.
   */
  public final List<Node> children() {
    return childrenList;
  }

  protected boolean addChild(Node frame) {
    if (frame == null)
      return false;
    if (hasChild(frame))
      return false;
    return children().add(frame);
  }

  /**
   * Removes the leading frame if present. Typically used when re-parenting the frame.
   */
  protected boolean removeChild(Node frame) {
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

  protected boolean hasChild(Node gFrame) {
    for (Node frame : children())
      if (frame == gFrame)
        return true;
    return false;
  }

  /**
   * Procedure called by the graph frame traversal algorithm. Default implementation is
   * empty, i.e., it is meant to be implemented by derived classes.
   *
   * @see Graph#traverse()
   */
  protected void visit() {
  }

  public void visitCallback() {
    if (isVisitEnabled())
      visit();
  }

  /**
   * Enables {@link #visit()} of this frame when performing the
   * {@link Graph#traverse()}.
   *
   * @see #disableVisit()
   * @see #toggleVisit()
   * @see #isVisitEnabled()
   */
  public void enableVisit() {
    visit = true;
  }

  /**
   * Disables {@link #visit()} of this frame when performing the
   * {@link Graph#traverse()}.
   *
   * @see #enableVisit()
   * @see #toggleVisit()
   * @see #isVisitEnabled()
   */
  public void disableVisit() {
    visit = false;
  }

  /**
   * Toggles {@link #visit()} of this frame when performing the
   * {@link Graph#traverse()}.
   *
   * @see #enableVisit()
   * @see #disableVisit()
   * @see #isVisitEnabled()
   */
  public void toggleVisit() {
    visit = !visit;
  }

  /**
   * Returns true if {@link #visit()} of this frame when performing the
   * {@link Graph#traverse() is enabled}.
   *
   * @see #enableVisit()
   * @see #disableVisit()
   * @see #toggleVisit()
   */
  public boolean isVisitEnabled() {
    return visit;
  }

  /**
   * Returns the graph this object belongs to.
   * <p>
   * Note that if this {@link #isEye()} then returns {@code eye().graph()}.
   *
   * @see Graph#eye()
   */
  public Graph graph() {
    return graph;
  }

  /**
   * Returns true if the node is attached to an eye, and false otherwise.
   * generic-nodes can only be attached to an eye at construction times. Refer to the
   * node constructors that take an eye parameter.
   *
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
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean track(MotionEvent event) {
    if (isEye())
      return false;
    if (event instanceof MotionEvent1)
      return track((MotionEvent1) event);
    if (event instanceof MotionEvent2)
      return track((MotionEvent2) event);
    if (event instanceof MotionEvent3)
      return track((MotionEvent3) event);
    if (event instanceof MotionEvent6)
      return track((MotionEvent6) event);
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean track(TapEvent event) {
    if (isEye())
      return false;
    return track(event.x(), event.y());
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   * <p>
   * Override this method when you want the object to be picked from a {@link KeyEvent}.
   */
  public boolean track(KeyEvent event) {
    Graph.showMissingImplementationWarning("track(KeyEvent event)", this.getClass().getName());
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   * <p>
   * Override this method when you want the object to be picked from a {@link MotionEvent1}.
   */
  public boolean track(MotionEvent1 event) {
    if (isEye())
      return false;
    Graph.showMissingImplementationWarning("track(MotionEvent1 event)", this.getClass().getName());
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean track(MotionEvent2 event) {
    if (isEye())
      return false;
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("track");
      return false;
    }
    return track(event.x(), event.y());
  }

  /**
   * Picks the node according to the {@link #pickingPrecision()}.
   *
   * @see #pickingPrecision()
   * @see #setPickingPrecision(PickingPrecision)
   */
  public boolean track(float x, float y) {
    Vector proj = graph.projectedCoordinatesOf(position());
    float halfThreshold = grabsInputThreshold() / 2;
    return ((Math.abs(x - proj.vec[0]) < halfThreshold) && (Math.abs(y - proj.vec[1]) < halfThreshold));
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean track(MotionEvent3 event) {
    return track(event.event2());
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean track(MotionEvent6 event) {
    return track(event.event3().event2());
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
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.MotionEvent}.
   */
  protected void interact(MotionEvent event) {
    if (event instanceof MotionEvent1)
      interact((MotionEvent1) event);
    if (event instanceof MotionEvent2)
      interact((MotionEvent2) event);
    if (event instanceof MotionEvent3)
      interact((MotionEvent3) event);
    if (event instanceof MotionEvent6)
      interact((MotionEvent6) event);
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent1}.
   */
  protected void interact(MotionEvent1 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent2}.
   */
  protected void interact(MotionEvent2 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent3}.
   */
  protected void interact(MotionEvent3 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent6}.
   */
  protected void interact(MotionEvent6 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link TapEvent}.
   */
  protected void interact(TapEvent event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link KeyEvent}.
   */
  protected void interact(KeyEvent event) {
  }

  // APPLY TRANSFORMATION

  /**
   * Convenience function that simply calls {@code applyTransformation(graph)}. It applies
   * the transformation defined by the frame to the graph used to instantiated.
   *
   * @see #applyTransformation(Graph)
   * @see #matrix()
   */
  public void applyTransformation() {
    applyTransformation(graph);
  }

  /**
   * Convenience function that simply calls {@code applyWorldTransformation(graph)}. It
   * applies the world transformation defined by the frame to the graph used to
   * instantiated.
   *
   * @see #applyWorldTransformation(Graph)
   * @see #worldMatrix()
   */
  public void applyWorldTransformation() {
    applyWorldTransformation(graph);
  }

  /**
   * Convenience function that simply calls {@code scn.applyTransformation(this)}. You may
   * apply the transformation represented by this frame to any graph you want using this
   * method.
   * <p>
   * Very efficient prefer always this than
   *
   * @see #applyTransformation()
   * @see #matrix()
   * @see Graph#applyTransformation(Frame)
   */
  public void applyTransformation(Graph scn) {
    scn.applyTransformation(this);
  }

  /**
   * Convenience function that simply calls {@code scn.applyWorldTransformation(this)}.
   * You may apply the world transformation represented by this frame to any graph you
   * want using this method.
   *
   * @see #applyWorldTransformation()
   * @see #worldMatrix()
   * @see Graph#applyWorldTransformation(Frame)
   */
  public void applyWorldTransformation(Graph scn) {
    scn.applyWorldTransformation(this);
  }

  // MODIFIED

  /**
   * Internal use. Automatically call by all methods which change the Frame state.
   */
  @Override
  protected void modified() {
    lastUpdate = TimingHandler.frameCount;
    if (children() != null)
      for (Node child : children())
        child.modified();
  }

  // SYNC

  /**
   * Same as {@code sync(this, otherFrame)}.
   *
   * @see #sync(Node, Node)
   */
  public void sync(Node otherFrame) {
    sync(this, otherFrame);
  }

  /**
   * If {@code f1} hasGrabber been more recently updated than {@code f2}, calls
   * {@code f2.setWorldMatrix(f1)}, otherwise calls {@code f1.setWorldMatrix(f2)}. Does
   * nothing if both objects were updated at the same frame.
   * <p>
   * This method syncs only the global geometry attributes ({@link #position()},
   * {@link #orientation()} and {@link #magnitude()}) among the two nodes. The
   * {@link #reference()} and {@link #constraint()} (if any) of each frame are kept
   * separately.
   *
   * @see #set(Frame)
   */
  public static void sync(Node f1, Node f2) {
    if (f1 == null || f2 == null)
      return;
    if (f1.lastUpdate() == f2.lastUpdate())
      return;
    Node source = (f1.lastUpdate() > f2.lastUpdate()) ? f1 : f2;
    Node target = (f1.lastUpdate() > f2.lastUpdate()) ? f2 : f1;
    target.setWorldMatrix(source);
  }

  // Fx

  /**
   * Internal use.
   * <p>
   * Returns the cached value of the spinning friction used in
   * {@link #recomputeSpinningRotation()}.
   */
  protected float dampingFx() {
    return sFriction;
  }

  /**
   * Defines the spinning deceleration.
   * <p>
   * Default value is 0.5. Use {@link #setDamping(float)} to tune this value. A higher
   * value will make damping more difficult (a value of 1.0 forbids damping).
   */
  public float damping() {
    return dampFriction;
  }

  /**
   * Defines the {@link #damping()}. Values must be in the range [0..1].
   */
  public void setDamping(float f) {
    if (f < 0 || f > 1)
      return;
    dampFriction = f;
    setDampingFx(dampFriction);
  }

  /**
   * Internal use.
   * <p>
   * Computes and caches the value of the spinning friction used in
   * {@link #recomputeSpinningRotation()}.
   */
  protected void setDampingFx(float spinningFriction) {
    sFriction = spinningFriction * spinningFriction * spinningFriction;
  }

  /**
   * Defines the {@link #rotationSensitivity()}.
   */
  public final void setRotationSensitivity(float sensitivity) {
    rotSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #scalingSensitivity()}.
   */
  public final void setScalingSensitivity(float sensitivity) {
    sclSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #translationSensitivity()}.
   */
  public final void setTranslationSensitivity(float sensitivity) {
    transSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #spinningSensitivity()}.
   */
  public final void setSpinningSensitivity(float sensitivity) {
    spngSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #wheelSensitivity()}.
   */
  public final void setWheelSensitivity(float sensitivity) {
    wheelSensitivity = sensitivity;
  }

  /**
   * Defines the {@link #keyboardSensitivity()}.
   */
  public final void setKeyboardSensitivity(float sensitivity) {
    keySensitivity = sensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node rotation.
   * <p>
   * Default value is 1.0 (which matches an identical mouse displacement), a higher value
   * will generate a larger rotation (and inversely for lower values). A 0.0 value will
   * forbid rotation (see also {@link #constraint()}).
   *
   * @see #setRotationSensitivity(float)
   * @see #translationSensitivity()
   * @see #scalingSensitivity()
   * @see #keyboardSensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public final float rotationSensitivity() {
    return rotSensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node scaling.
   * <p>
   * Default value is 1.0, a higher value will generate a larger scaling (and inversely
   * for lower values). A 0.0 value will forbid scaling (see also {@link #constraint()}).
   *
   * @see #setScalingSensitivity(float)
   * @see #setRotationSensitivity(float)
   * @see #translationSensitivity()
   * @see #keyboardSensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public final float scalingSensitivity() {
    return sclSensitivity;
  }

  /**
   * Returns the influence of a gesture displacement on the node translation.
   * <p>
   * Default value is 1.0 which in the case of a mouse interaction makes the node
   * precisely stays under the mouse cursor.
   * <p>
   * With an identical gesture displacement, a higher value will generate a larger
   * translation (and inversely for lower values). A 0.0 value will forbid translation
   * (see also {@link #constraint()}).
   *
   * @see #setTranslationSensitivity(float)
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keyboardSensitivity()
   * @see #spinningSensitivity()
   * @see #wheelSensitivity()
   */
  public final float translationSensitivity() {
    return transSensitivity;
  }

  /**
   * Returns the minimum gesture speed required to make the node {@link #spin()}.
   * Spinning requires to set to {@link #damping()} to 0.
   * <p>
   * See {@link #spin()}, {@link #spinningRotation()} and
   * {@link #startSpinning(MotionEvent, Quaternion)} for details.
   * <p>
   * Gesture speed is expressed in pixels per milliseconds. Default value is 0.3 (300
   * pixels per second). Use {@link #setSpinningSensitivity(float)} to tune this value. A
   * higher value will make spinning more difficult (a value of 100.0 forbids spinning in
   * practice).
   *
   * @see #setSpinningSensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keyboardSensitivity()
   * @see #wheelSensitivity()
   * @see #setDamping(float)
   */
  public final float spinningSensitivity() {
    return spngSensitivity;
  }

  /**
   * Returns the wheel sensitivity.
   * <p>
   * Default value is 15.0. A higher value will make the wheel action more efficient
   * (usually meaning faster motion). Use a negative value to invert the operation
   * direction.
   *
   * @see #setWheelSensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #keyboardSensitivity()
   * @see #spinningSensitivity()
   */
  public float wheelSensitivity() {
    return wheelSensitivity;
  }

  /**
   * Returns the keyboard sensitivity.
   * <p>
   * Default value is 10. A higher value will make the keyboard more efficient (usually
   * meaning faster motion).
   *
   * @see #setKeyboardSensitivity(float)
   * @see #translationSensitivity()
   * @see #rotationSensitivity()
   * @see #scalingSensitivity()
   * @see #wheelSensitivity()
   * @see #setDamping(float)
   */
  public float keyboardSensitivity() {
    return keySensitivity;
  }

  /**
   * Returns {@code true} when the node is spinning.
   * <p>
   * During spinning, {@link #spin()} rotates the node by its
   * {@link #spinningRotation()} at a frequency defined when the node
   * {@link #startSpinning(MotionEvent, Quaternion)}.
   * <p>
   * Use {@link #startSpinning(MotionEvent, Quaternion)} and {@link #stopSpinning()} to
   * change this state. Default value is {@code false}.
   *
   * @see #isFlying()
   */
  public final boolean isSpinning() {
    return spinningTimerTask.isActive();
  }

  /**
   * Returns the incremental rotation that is applied by {@link #spin()} to the
   * node orientation when it {@link #isSpinning()}.
   * <p>
   * Default value is a {@code null} rotation. Use {@link #setSpinningRotation(Quaternion)}
   * to change this value.
   * <p>
   * The {@link #spinningRotation()} axis is defined in the node coordinate
   * system. You can use {@link Frame#transformOfFrom(Vector, Frame)}
   * to convert this axis from another Frame coordinate system.
   * <p>
   * <b>Attention: </b>Spinning may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #flyDirection()
   */
  public final Quaternion spinningRotation() {
    return spngRotation;
  }

  /**
   * Defines the {@link #spinningRotation()}. Its axis is defined in the node
   * coordinate system.
   *
   * @see #setFlyDirection(Vector)
   */
  public final void setSpinningRotation(Quaternion spinningRotation) {
    spngRotation = spinningRotation;
  }

  /**
   * Stops the spinning motion started using {@link #startSpinning(MotionEvent, Quaternion)}
   * . {@link #isSpinning()} will return {@code false} after this call.
   * <p>
   * <b>Attention: </b>This method may be called by {@link #spin()}, since spinning may be
   * decelerated according to {@link #damping()} till it stops completely.
   *
   * @see #damping()
   */
  public final void stopSpinning() {
    spinningTimerTask.stop();
  }

  /**
   * Internal use. Same as {@code startSpinning(rt, event.speed(), event.delay())}.
   *
   * @see #startFlying(MotionEvent, Vector)
   * @see #startSpinning(Quaternion, float, long)
   */
  protected void startSpinning(MotionEvent event, Quaternion rt) {
    startSpinning(rt, event.speed(), event.delay());
  }

  /**
   * Starts the spinning of the node.
   * <p>
   * This method starts a timer that will call {@link #spin()} every
   * {@code updateInterval} milliseconds. The node {@link #isSpinning()} until
   * you call {@link #stopSpinning()}.
   * <p>
   * <b>Attention: </b>Spinning may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   * @see #startFlying(Vector, float)
   */
  public void startSpinning(Quaternion rt, float speed, long delay) {
    setSpinningRotation(rt);
    eventSpeed = speed;
    eventDelay = delay;
    if (damping() == 0 && eventSpeed < spinningSensitivity())
      return;
    int updateInterval = (int) delay;
    if (updateInterval > 0)
      spinningTimerTask.run(updateInterval);
  }

  /**
   * Cache version. Used by rotate methods when damping = 0.
   */
  protected void startSpinning() {
    startSpinning(spinningRotation(), eventSpeed, eventDelay);
  }

  protected void spinExecution() {
    if (damping() == 0)
      spin();
    else {
      if (eventSpeed == 0) {
        stopSpinning();
        return;
      }
      spin();
      recomputeSpinningRotation();
    }
  }

  protected void spin(Quaternion rt, float speed, long delay) {
    if (damping() == 0) {
      spin(rt);
      eventSpeed = speed;
      eventDelay = delay;
    } else
      startSpinning(rt, speed, delay);
  }

  protected void spin(Quaternion rt) {
    setSpinningRotation(rt);
    spin();
  }

  /**
   * Rotates the graph-frame by its {@link #spinningRotation()} or around the
   * {@link Graph#anchor()} when this graph-frame is the
   * {@link Graph#eye()}. Called by a timer when the
   * node {@link #isSpinning()}.
   * <p>
   * <b>Attention: </b>Spinning may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   */
  protected void spin() {
    if (isEye())
      rotateAroundPoint(spinningRotation(), graph().anchor());
    else
      rotate(spinningRotation());
  }

  /**
   * Internal method. Recomputes the {@link #spinningRotation()} according to
   * {@link #damping()}.
   */
  protected void recomputeSpinningRotation() {
    float prevSpeed = eventSpeed;
    float damping = 1.0f - dampingFx();
    eventSpeed *= damping;
    if (Math.abs(eventSpeed) < .001f)
      eventSpeed = 0;
    // float currSpeed = eventSpeed;
    spinningRotation().fromAxisAngle(((Quaternion) spinningRotation()).axis(), spinningRotation().angle() * (eventSpeed / prevSpeed));
    //TODO Restore 2D
    /*
    if (graph.is3D())
      ((Quaternion) spinningRotation())
          .fromAxisAngle(((Quaternion) spinningRotation()).axis(), spinningRotation().angle() * (eventSpeed / prevSpeed));
    else
      this.setSpinningRotation(new Rot(spinningRotation().angle() * (eventSpeed / prevSpeed)));
    */
  }

  protected int originalDirection(MotionEvent event) {
    return originalDirection(event, true);
  }

  protected int originalDirection(MotionEvent event, boolean fromX) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event, fromX);
    if (motionEvent2 != null)
      return originalDirection(motionEvent2);
    else {
      Graph.showMinDOFsWarning("originalDirection", 2);
      return 0;
    }
  }

  /**
   * Return 1 if mouse motion was started horizontally and -1 if it was more vertical.
   * Returns 0 if this could not be determined yet (perfect diagonal motion, rare).
   */
  protected int originalDirection(MotionEvent2 event) {
    if (!dirIsFixed) {
      Point delta = new Point(event.dx(), event.dy());
      dirIsFixed = Math.abs(delta.x()) != Math.abs(delta.y());
      horiz = Math.abs(delta.x()) > Math.abs(delta.y());
    }

    if (dirIsFixed)
      if (horiz)
        return 1;
      else
        return -1;
    else
      return 0;
  }

  /**
   * Returns a Rotation computed according to the mouse motion. Mouse positions are
   * projected on a deformed ball, centered on ({@code center.x()}, {@code center.y()}).
   */
  public Quaternion deformedBallRotation(MotionEvent2 event, Vector center) {
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("deformedBallRotation");
      return null;
    }
    float cx = center.x();
    float cy = center.y();
    float x = event.x();
    float y = event.y();
    float prevX = event.prevX();
    float prevY = event.prevY();
    // Points on the deformed ball
    float px = rotationSensitivity() * ((int) prevX - cx) / graph.width();
    float py =
            rotationSensitivity() * (graph.isLeftHanded() ? ((int) prevY - cy) : (cy - (int) prevY)) / graph.height();
    float dx = rotationSensitivity() * (x - cx) / graph.width();
    float dy = rotationSensitivity() * (graph.isLeftHanded() ? (y - cy) : (cy - y)) / graph.height();

    Vector p1 = new Vector(px, py, projectOnBall(px, py));
    Vector p2 = new Vector(dx, dy, projectOnBall(dx, dy));
    // Approximation of rotation angle Should be divided by the projectOnBall
    // size, but it is 1.0
    Vector axis = p2.cross(p1);
    float angle = 2.0f * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / p1.squaredNorm() / p2.squaredNorm()));
    return new Quaternion(axis, angle);
  }
  //TODO Restore 2D
  /*
  public Rotation deformedBallRotation(MotionEvent2 event, Vector center) {
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("deformedBallRotation");
      return null;
    }
    if (graph.is2D()) {
      Rot rt;
      Point prevPos = new Point(event.prevX(), event.prevY());
      Point curPos = new Point(event.x(), event.y());
      rt = new Rot(new Point(center.x(), center.y()), prevPos, curPos);
      rt = new Rot(rt.angle() * rotationSensitivity());
      if ((graph.isRightHanded() && !isEye()) || (graph.isLeftHanded() && isEye()))
        rt.negate();
      return rt;
    } else {
      float cx = center.x();
      float cy = center.y();
      float x = event.x();
      float y = event.y();
      float prevX = event.prevX();
      float prevY = event.prevY();
      // Points on the deformed ball
      float px = rotationSensitivity() * ((int) prevX - cx) / graph.eye().screenWidth();
      float py =
          rotationSensitivity() * (graph.isLeftHanded() ? ((int) prevY - cy) : (cy - (int) prevY)) / graph.eye()
              .screenHeight();
      float dx = rotationSensitivity() * (x - cx) / graph.eye().screenWidth();
      float dy = rotationSensitivity() * (graph.isLeftHanded() ? (y - cy) : (cy - y)) / graph.eye().screenHeight();

      Vector p1 = new Vector(px, py, projectOnBall(px, py));
      Vector p2 = new Vector(dx, dy, projectOnBall(dx, dy));
      // Approximation of rotation angle Should be divided by the projectOnBall
      // size, but it is 1.0
      Vector axis = p2.cross(p1);
      float angle = 2.0f * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / p1.squaredNorm() / p2.squaredNorm()));
      return new Quaternion(axis, angle);
    }
  }
  */

  /**
   * Returns "pseudo-distance" from (x,y) to ball of radius size. For a point inside the
   * ball, it is proportional to the euclidean distance to the ball. For a point outside
   * the ball, it is proportional to the inverse of this distance (tends to zero) on the
   * ball, the function is continuous.
   */
  protected float projectOnBall(float x, float y) {
    // If you change the size value, change angle computation in
    // deformedBallQuaternion().
    float size = 1.0f;
    float size2 = size * size;
    float size_limit = size2 * 0.5f;

    float d = x * x + y * y;
    return d < size_limit ? (float) Math.sqrt(size2 - d) : size_limit / (float) Math.sqrt(d);
  }

  // macro's

  protected float computeAngle(MotionEvent1 e1) {
    return computeAngle(e1.dx());
  }

  protected float computeAngle() {
    return computeAngle(1);
  }

  protected float computeAngle(float dx) {
    return dx * (float) Math.PI / graph.width();
  }

  protected boolean wheel(MotionEvent event) {
    return event instanceof MotionEvent1;
  }

  /**
   * Wrapper method for {@link #alignWithFrame(Frame, boolean, float)} that discriminates
   * between eye and non-eye nodes.
   */
  public void align() {
    if (isEye())
      alignWithFrame(null, true);
    else
      alignWithFrame(graph.eye());
  }

  /**
   * Centers the node into the graph.
   */
  public void center() {
    if (isEye())
      graph().center();
    else
      projectOnLine(graph.eye().position(), graph.eye().zAxis(false));
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
  protected void translateX(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      translateX(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateX(MotionEvent1 event) {
    translateX(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  protected void translateX(MotionEvent1 event, float sens) {
    translate(screenToVec(Vector.multiply(new Vector(isEye() ? -event.dx() : event.dx(), 0, 0), sens)));
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateXPos() {
    translateX(true);
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  public void translateXNeg() {
    translateX(false);
  }

  /**
   * User gesture into x-translation conversion routine.
   */
  protected void translateX(boolean right) {
    translate(screenToVec(
        Vector.multiply(new Vector(1, 0), (right ^ this.isEye()) ? keyboardSensitivity() : -keyboardSensitivity())));
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
  protected void translateY(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      translateY(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateY(MotionEvent1 event) {
    translateY(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  protected void translateY(MotionEvent1 event, float sens) {
    translate(
        screenToVec(Vector.multiply(new Vector(0, isEye() ^ graph.isRightHanded() ? -event.dx() : event.dx()), sens)));
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateYPos() {
    translateY(true);
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  public void translateYNeg() {
    translateY(false);
  }

  /**
   * User gesture into y-translation conversion routine.
   */
  protected void translateY(boolean up) {
    translate(screenToVec(
        Vector.multiply(new Vector(0, (up ^ this.isEye() ^ graph.isLeftHanded()) ? 1 : -1), this.keyboardSensitivity())));
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
  protected void translateZ(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      translateZ(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZ(MotionEvent1 event) {
    translateZ(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  protected void translateZ(MotionEvent1 event, float sens) {
    if (graph.is2D()) {
      Graph.showDepthWarning("translateZ");
      return;
    }
    translate(screenToVec(Vector.multiply(new Vector(0.0f, 0.0f, isEye() ? -event.dx() : event.dx()), sens)));
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZPos() {
    translateZ(true);
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  public void translateZNeg() {
    translateZ(false);
  }

  /**
   * User gesture into z-translation conversion routine.
   */
  protected void translateZ(boolean up) {
    if (graph.is2D()) {
      Graph.showDepthWarning("translateZ");
      return;
    }
    translate(screenToVec(
        Vector.multiply(new Vector(0.0f, 0.0f, 1), (up ^ this.isEye()) ? -keyboardSensitivity() : keyboardSensitivity())));
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
  protected void translate(MotionEvent event, boolean fromX) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event, fromX);
    if (motionEvent2 != null)
      translate(motionEvent2);
    else
      Graph.showMinDOFsWarning("translate", 2);
  }

  /**
   * User gesture into xy-translation conversion routine.
   */
  public void translate(MotionEvent2 event) {
    translate(screenToVec(Vector.multiply(new Vector(isEye() ? -event.dx() : event.dx(),
        (graph.isRightHanded() ^ isEye()) ? -event.dy() : event.dy(), 0.0f), this.translationSensitivity())));
  }

  /**
   * User gesture into xyz-translation conversion routine.
   */
  public void translateXYZ(MotionEvent event) {
    MotionEvent3 motionEvent3 = MotionEvent.event3(event, true);
    if (motionEvent3 != null)
      translateXYZ(motionEvent3);
    else
      Graph.showMinDOFsWarning("translateXYZ", 3);
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
    if (graph.is2D()) {
      Graph.showDepthWarning("translateXYZ");
      return;
    }
    translate(screenToVec(
        Vector.multiply(new Vector(event.dx(), graph.isRightHanded() ? -event.dy() : event.dy(), -event.dz()),
            this.translationSensitivity())));
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
  protected void zoomOnAnchor(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      zoomOnAnchor(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  protected void zoomOnAnchor(MotionEvent1 event, float sens) {
    Vector direction = Vector.subtract(graph.anchor(), position());
    if (reference() != null)
      direction = reference().transformOf(direction);
    float delta = event.dx() * sens / graph.height();
    if (direction.magnitude() > 0.02f * graph.radius() || delta > 0.0f)
      translate(Vector.multiply(direction, delta));
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchorPos() {
    zoomOnAnchor(true);
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  public void zoomOnAnchorNeg() {
    zoomOnAnchor(false);
  }

  /**
   * User gesture into zoom-on-anchor conversion routine.
   *
   * @see Graph#anchor()
   */
  protected void zoomOnAnchor(boolean in) {
    Vector direction = Vector.subtract(graph.anchor(), position());
    if (reference() != null)
      direction = reference().transformOf(direction);
    float delta = (in ? keyboardSensitivity() : -keyboardSensitivity()) / graph.height();
    if (direction.magnitude() > 0.02f * graph.radius() || delta > 0.0f)
      translate(Vector.multiply(direction, delta));
  }

  /**
   * User gesture into zoom-on-region conversion routine.
   */
  public void zoomOnRegion(MotionEvent event) {
    MotionEvent2 dof2 = MotionEvent.event2(event);
    if (dof2 == null) {
      Graph.showMinDOFsWarning("zoomOnRegion", 2);
      return;
    }
    zoomOnRegion(dof2);
  }

  /**
   * User gesture into zoom-on-region conversion routine.
   */
  public void zoomOnRegion(MotionEvent2 event) {
    if (!isEye()) {
      Graph.showOnlyEyeWarning("zoomOnRegion");
      return;
    }
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("zoomOnRegion");
      return;
    }
    if (event.fired()) {
      initEvent = event.get();
      //TODO handle me
      //graph.setZoomVisualHint(true);
    } else if (event.flushed()) {
      MotionEvent2 e = new MotionEvent2(initEvent.get(), event.x(), event.y(), event.modifiers(), event.id());
      //TODO handle me
      //graph.setZoomVisualHint(false);
      int w = (int) Math.abs(e.dx());
      int tlX = (int) e.prevX() < (int) e.x() ? (int) e.prevX() : (int) e.x();
      int h = (int) Math.abs(e.dy());
      int tlY = (int) e.prevY() < (int) e.y() ? (int) e.prevY() : (int) e.y();
      graph().fitScreenRegionInterpolation(new Rectangle(tlX, tlY, w, h));
      //graph().fitScreenRegion(new Rectangle(tlX, tlY, w, h));
    }
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
  protected void rotateX(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      rotateX(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateX(MotionEvent1 event) {
    rotateX(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  protected void rotateX(MotionEvent1 event, float sens) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateX");
      return;
    }
    spin(screenToQuat(computeAngle(event) * (isEye() ? -sens : sens), 0, 0), event.speed(), event.delay());
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateXPos() {
    rotateX(true);
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  public void rotateXNeg() {
    rotateX(false);
  }

  /**
   * User gesture into x-rotation conversion routine.
   */
  protected void rotateX(boolean up) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateX");
      return;
    }
    rotate(screenToQuat(computeAngle() * (up ? keyboardSensitivity() : -keyboardSensitivity()), 0, 0));
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
  protected void rotateY(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event, fromX);
    if (motionEvent1 != null)
      rotateY(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateY(MotionEvent1 event) {
    rotateY(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  protected void rotateY(MotionEvent1 event, float sens) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateY");
      return;
    }
    spin(screenToQuat(0, computeAngle(event) * (isEye() ? -sens : sens), 0), event.speed(), event.delay());
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateYPos() {
    rotateY(true);
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  public void rotateYNeg() {
    rotateY(false);
  }

  /**
   * User gesture into y-rotation conversion routine.
   */
  protected void rotateY(boolean up) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateY");
      return;
    }
    Quaternion rt = screenToQuat(0, computeAngle() * (up ? keyboardSensitivity() : -keyboardSensitivity()), 0);
    rotate(rt);
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
  protected void rotateZ(MotionEvent event, boolean fromX) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event);
    if (motionEvent1 != null)
      rotateZ(motionEvent1, wheel(event) ? this.wheelSensitivity() : this.rotationSensitivity());
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZ(MotionEvent1 event) {
    rotateZ(event, wheel(event) ? this.wheelSensitivity() : this.translationSensitivity());
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  protected void rotateZ(MotionEvent1 event, float sens) {
    spin(screenToQuat(0, 0, sens * (isEye() ? -computeAngle(event) : computeAngle(event))), event.speed(), event.delay());
  }
  //TODO Restore 2D
  /*
  protected void rotateZ(MotionEvent1 event, float sens) {
    Rotation rt;
    if (isEye())
      if (is2D())
        rt = new Rot(sens * (graph.isRightHanded() ? computeAngle(event) : -computeAngle(event)));
      else
        rt = screenToQuat(0, 0, sens * -computeAngle(event));
    else if (is2D())
      rt = new Rot(sens * (graph.isRightHanded() ? -computeAngle(event) : computeAngle(event)));
    else
      rt = screenToQuat(0, 0, sens * computeAngle(event));
    spin(rt, event.speed(), event.delay());
  }
  */

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZPos() {
    rotateZ(true);
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  public void rotateZNeg() {
    rotateZ(false);
  }

  /**
   * User gesture into z-rotation conversion routine.
   */
  protected void rotateZ(boolean up) {
    //TODO Restore 2D
    /*
    Rotation rt;
    if (is2D())
      rt = new Rot(computeAngle() * (up ? keyboardSensitivity() : -keyboardSensitivity()));
    else
      rt = screenToQuat(0, 0, computeAngle() * (up ? keyboardSensitivity() : -keyboardSensitivity()));
    */
    rotate(screenToQuat(0, 0, computeAngle() * (up ? keyboardSensitivity() : -keyboardSensitivity())));
  }

  /**
   * User gesture into xyz-rotation conversion routine.
   */
  protected void rotateXYZ(MotionEvent event) {
    MotionEvent3 motionEvent3 = MotionEvent.event3(event, false);
    if (motionEvent3 != null)
      rotateXYZ(motionEvent3);
    else
      Graph.showMinDOFsWarning("rotateXYZ", 3);
  }

  /**
   * User gesture into xyz-rotation conversion routine.
   */
  public void rotateXYZ(MotionEvent3 event) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateXYZ");
      return;
    }
    if (event.fired() && graph.is3D())
      graph.cadRotationIsReversed = graph.eye().transformOf(upVector).y() < 0.0f;
    rotate(screenToQuat(
        Vector.multiply(new Vector(computeAngle(event.dx()), computeAngle(-event.dy()), computeAngle(-event.dz())),
            rotationSensitivity())));
  }

  /**
   * User gesture into arcball-rotation conversion routine.
   */
  public void rotate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      rotate(motionEvent2);
    else
      Graph.showMinDOFsWarning("rotate", 2);
  }

  /**
   * User gesture into arcball-rotation conversion routine.
   */
  public void rotate(MotionEvent2 event) {
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("rotate");
      return;
    }
    if (event.fired())
      stopSpinning();
    if (event.fired() && graph.is3D())
      graph.cadRotationIsReversed = graph.eye().transformOf(upVector).y() < 0.0f;
    if (event.flushed() && damping() == 0) {
      startSpinning();
      return;
    }
    if (!event.flushed()) {
      Quaternion rt;
      Vector trns;
      if (isEye())
        rt = deformedBallRotation(event, graph().projectedCoordinatesOf(graph().anchor()));
      else {
        trns = graph.projectedCoordinatesOf(position());
        rt = deformedBallRotation(event, trns);
        trns = ((Quaternion) rt).axis();
        trns = graph.eye().orientation().rotate(trns);
        trns = transformOf(trns);
        rt = new Quaternion(trns, -rt.angle());
      }
      spin(rt, event.speed(), event.delay());
    }
  }
  //TODO Restore 2D
  /*
  public void rotate(MotionEvent2 event) {
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("rotate");
      return;
    }
    if (event.fired())
      stopSpinning();
    if (event.fired() && graph.is3D())
      graph.eye().cadRotationIsReversed =
          graph.eye().frame().transformOf(graph.eye().frame().upVector()).y() < 0.0f;
    if (event.flushed() && damping() == 0) {
      startSpinning();
      return;
    }
    if (!event.flushed()) {
      Rotation rt;
      Vector trns;
      if (isEye())
        rt = deformedBallRotation(event, eye().projectedCoordinatesOf(eye().anchor()));
      else {
        if (is2D())
          rt = deformedBallRotation(event, graph.eye().projectedCoordinatesOf(position()));
        else {
          trns = graph.eye().projectedCoordinatesOf(position());
          rt = deformedBallRotation(event, trns);
          trns = ((Quaternion) rt).axis();
          trns = graph.eye().frame().orientation().rotate(trns);
          trns = transformOf(trns);
          rt = new Quaternion(trns, -rt.angle());
        }
      }
      spin(rt, event.speed(), event.delay());
    }
  }
  */

  /**
   * User gesture into scaling conversion routine.
   */
  public void scale(MotionEvent event) {
    MotionEvent1 motionEvent1 = MotionEvent.event1(event);
    if (motionEvent1 != null)
      scale(motionEvent1, wheel(event) ? wheelSensitivity() : scalingSensitivity());
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scale(MotionEvent1 event) {
    scale(event, wheel(event) ? wheelSensitivity() : scalingSensitivity());
  }

  /**
   * User gesture into scaling conversion routine.
   */
  protected void scale(MotionEvent1 event, float sens) {
    if (isEye()) {
      float delta = event.dx() * sens;
      float s = 1 + Math.abs(delta) / (float) -graph.height();
      scale(delta >= 0 ? s : 1 / s);
    } else {
      float delta = event.dx() * sens;
      float s = 1 + Math.abs(delta) / (float) graph.height();
      scale(delta >= 0 ? s : 1 / s);
    }
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scalePos() {
    scale(true);
  }

  /**
   * User gesture into scaling conversion routine.
   */
  public void scaleNeg() {
    scale(false);
  }

  /**
   * User gesture into scaling conversion routine.
   */
  protected void scale(boolean up) {
    float s = 1 + Math.abs(keyboardSensitivity()) / (isEye() ? (float) -graph.height() : (float) graph.height());
    scale(up ? s : 1 / s);
  }

  /**
   * Use for first person (move forward/backward, lookAround) and cad motion actions.
   */
  protected final void updateUpVector() {
    upVector = orientation().rotate(new Vector(0.0f, 1.0f, 0.0f));
  }

  public void lookAround(MotionEvent event) {
    rotate(rollPitchQuaternion(event));
  }

  /**
   * User gesture into move-backward conversion routine.
   */
  public void moveBackward(MotionEvent event) {
    moveForward(event, false);
  }

  /**
   * User gesture into move-forward conversion routine.
   */
  public void moveForward(MotionEvent event) {
    moveForward(event, true);
  }

  /**
   * User gesture into move-forward conversion routine.
   */
  protected void moveForward(MotionEvent event, boolean forward) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      moveForward(motionEvent2, forward);
    else
      Graph.showMinDOFsWarning("moveForward", 2);
  }

  /**
   * User gesture into move-forward conversion routine.
   */
  protected void moveForward(MotionEvent2 event, boolean forward) {
    if (event.fired())
      updateUpVector();
    else if (event.flushed()) {
      stopFlying();
      return;
    }
    Vector trns;
    float fSpeed = forward ? -flySpeed() : flySpeed();
    if (is2D()) {
      rotate(deformedBallRotation(event, graph.projectedCoordinatesOf(position())));
      flyDisp.set(-fSpeed, 0.0f, 0.0f);
      trns = localInverseTransformOf(flyDisp);
      startFlying(event, trns);
    } else {
      rotate(rollPitchQuaternion(event));
      flyDisp.set(0.0f, 0.0f, fSpeed);
      trns = rotation().rotate(flyDisp);
      startFlying(event, trns);
    }
  }

  /**
   * User gesture into drive conversion routine.
   */
  public void drive(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      drive(motionEvent2);
    else
      Graph.showMinDOFsWarning("drive", 2);
  }

  /**
   * User gesture into drive conversion routine.
   */
  public void drive(MotionEvent2 event) {
    if (graph.is2D()) {
      Graph.showDepthWarning("drive");
      return;
    }
    if (event.fired()) {
      initEvent = event.get();
      updateUpVector();
      flySpeedCache = flySpeed();
    } else if (event.flushed()) {
      setFlySpeed(flySpeedCache);
      stopFlying();
      return;
    }
    setFlySpeed(0.01f * graph.radius() * 0.01f * (event.y() - initEvent.y()));
    Vector trns;
    rotate(turnQuaternion(event.event1()));
    flyDisp.set(0.0f, 0.0f, flySpeed());
    trns = rotation().rotate(flyDisp);
    startFlying(event, trns);
  }

  /**
   * User gesture into CAD-rotation conversion routine.
   */
  public void rotateCAD(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      rotateCAD(motionEvent2);
    else
      Graph.showMinDOFsWarning("rotateCAD", 2);
  }

  /**
   * User gesture into CAD-rotation conversion routine.
   */
  public void rotateCAD(MotionEvent2 event) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rotateCAD");
      return;
    }
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("rotateCAD");
      return;
    }
    if (event.fired())
      stopSpinning();
    if (event.fired() && graph.is3D())
      graph.cadRotationIsReversed = graph.eye().transformOf(upVector).y() < 0.0f;
    if (event.flushed() && damping() == 0) {
      startSpinning();
      return;
    } else {
      // Multiply by 2.0 to get on average about the same speed as with the
      // deformed ball
      float dx = -2.0f * rotationSensitivity() * event.dx() / graph.width();
      float dy = 2.0f * rotationSensitivity() * event.dy() / graph.height();
      if (graph.cadRotationIsReversed)
        dx = -dx;
      if (graph.isRightHanded())
        dy = -dy;
      Vector verticalAxis = transformOf(upVector);
      spin(Quaternion.multiply(new Quaternion(verticalAxis, dx), new Quaternion(new Vector(1.0f, 0.0f, 0.0f), dy)), event.speed(),
          event.delay());
    }
  }

  /**
   * User gesture into hinge conversion routine.
   */
  public void hinge(MotionEvent event) {
    MotionEvent6 motionEvent6 = MotionEvent.event6(event);
    if (motionEvent6 != null)
      hinge(motionEvent6);
    else
      Graph.showMinDOFsWarning("hinge", 6);
  }

  /**
   * User gesture into hinge conversion routine.
   */
  public void hinge(MotionEvent6 event) {
    if (graph.is2D()) {
      Graph.showDepthWarning("hinge");
      return;
    }
    if (!isEye()) {
      Graph.showOnlyEyeWarning("hinge");
      return;
    }
    // aka google earth navigation
    // 1. Relate the eye reference frame:
    Vector trns = new Vector();
    Vector pos = position();
    Quaternion o = (Quaternion) orientation();
    Frame oldRef = reference();
    Node rFrame = new Node(graph);
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
    float deltaY = computeAngle(event.dy());
    rFrame.rotate(new Quaternion(new Vector(1, 0, 0), graph.isRightHanded() ? deltaY : -deltaY));
    // 4. Rotate the refFrame around its Y-axis -> translate left-right the
    // frame on the sphere surface
    float deltaX = computeAngle(event.dx());
    rFrame.rotate(new Quaternion(new Vector(0, 1, 0), deltaX));
    // 5. Rotate the refFrame around its Z-axis -> look around
    float rZ = computeAngle(event.drz());
    rFrame.rotate(new Quaternion(new Vector(0, 0, 1), graph.isRightHanded() ? -rZ : rZ));
    // 6. Rotate the frame around x-axis -> move head up and down :P
    float rX = computeAngle(event.drx());
    Quaternion q = new Quaternion(new Vector(1, 0, 0), graph.isRightHanded() ? rX : -rX);
    rotate(q);
    // 7. Unrelate the frame and restore state:
    pos = position();
    o = (Quaternion) orientation();
    setReference(oldRef);
    graph().pruneBranch(rFrame);
    setPosition(pos);
    setOrientation(o);
  }

  /**
   * User gesture screen-translate conversion routine.
   */
  public void screenTranslate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      screenTranslate(motionEvent2);
    else
      Graph.showMinDOFsWarning("screenTranslate", 2);
  }

  /**
   * User gesture screen-translate conversion routine.
   */
  public void screenTranslate(MotionEvent2 event) {
    if (event.fired())
      dirIsFixed = false;
    int dir = originalDirection(event);
    if (dir == 1)
      translateX(event, true);
    else if (dir == -1)
      translateY(event, false);
  }

  /**
   * User gesture screen-rotation conversion routine.
   */
  public void screenRotate(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      screenRotate(motionEvent2);
    else
      Graph.showMinDOFsWarning("screenRotate", 2);
  }

  /**
   * User gesture screen-rotation conversion routine.
   */
  public void screenRotate(MotionEvent2 event) {
    if (event.isAbsolute()) {
      Graph.showEventVariationWarning("screenRotate");
      return;
    }
    if (event.fired()) {
      stopSpinning();
      //TODO handle me
      //graph.setRotateVisualHint(true); // display visual hint
      if (graph.is3D())
        graph.cadRotationIsReversed = graph.eye().transformOf(upVector).y() < 0.0f;
    }
    if (event.flushed()) {
      //TODO handle me
      //graph.setRotateVisualHint(false);
      if (damping() == 0) {
        startSpinning();
        return;
      }
    }
    if (!event.flushed()) {
      if (this.is2D()) {
        rotate(event);
        return;
      }
      Quaternion rt;
      Vector trns;
      float angle;
      if (isEye()) {
        trns = graph().projectedCoordinatesOf(graph().anchor());
        angle = (float) Math.atan2(event.y() - trns.vec[1], event.x() - trns.vec[0]) - (float) Math
            .atan2(event.prevY() - trns.vec[1], event.prevX() - trns.vec[0]);
        if (graph.isLeftHanded())
          angle = -angle;
        rt = new Quaternion(new Vector(0.0f, 0.0f, 1.0f), angle);
      } else {
        trns = graph.projectedCoordinatesOf(position());
        float prev_angle = (float) Math.atan2(event.prevY() - trns.vec[1], event.prevX() - trns.vec[0]);
        angle = (float) Math.atan2(event.y() - trns.vec[1], event.x() - trns.vec[0]);
        Vector axis = transformOf(graph.eye().orientation().rotate(new Vector(0.0f, 0.0f, -1.0f)));
        if (graph.isRightHanded())
          rt = new Quaternion(axis, angle - prev_angle);
        else
          rt = new Quaternion(axis, prev_angle - angle);
      }
      spin(rt, event.speed(), event.delay());
    }
  }

  /**
   * User gesture into anchor from pixel conversion routine.
   */
  //TODO missed
  /*
  public void anchorFromPixel(TapEvent event) {
    if (isEye())
      graph().setAnchorFromPixel(new Point(event.x(), event.y()));
    else
      Graph.showOnlyEyeWarning("anchorFromPixel");
  }
  */

  // Quite nice

  /**
   * Same as {@code return screenToVec(new Vector(x, y, z))}.
   *
   * @see #screenToVec(Vector)
   */
  public Vector screenToVec(float x, float y, float z) {
    return screenToVec(new Vector(x, y, z));
  }

  /**
   * Same as {@code return eyeToReferenceFrame(screenToEye(trns))}. Transforms the vector
   * from screen (device) coordinates to {@link #reference()} coordinates.
   *
   * @see #screenToEye(Vector)
   * @see #eyeToReferenceFrame(Vector)
   */
  public Vector screenToVec(Vector trns) {
    return eyeToReferenceFrame(screenToEye(trns));
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
   * and {@link #screenToQuat(float, float, float)}.
   *
   * @see #screenToEye(Vector)
   * @see #screenToQuat(float, float, float)
   */
  public Vector eyeToReferenceFrame(Vector trns) {
    Frame gFrame = isEye() ? this : /* respectToEye() ? */graph.eye() /* : this */;
    Vector t = gFrame.inverseTransformOf(trns);
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
   * and {@link #screenToQuat(float, float, float)}.
   *
   * @see #eyeToReferenceFrame(Vector)
   * @see #screenToQuat(float, float, float)
   */
  public Vector screenToEye(Vector trns) {
    Vector eyeVector = trns.get();
    // Scale to fit the screen relative event displacement
    if (graph.is2D())
      // Quite excited to see how simple it's in 2d:
      return eyeVector;
    // ... and amazed as to how dirty it's in 3d:
    switch (graph.type()) {
      case PERSPECTIVE:
        float k = (float) Math.tan(graph.fieldOfView() / 2.0f) * Math.abs(
            graph.eye().coordinatesOf(isEye() ? graph().anchor() : position()).vec[2] * graph.eye().magnitude());
        // * Math.abs(graph.eye().frame().coordinatesOf(isEye() ?
        // graph.eye().anchor() : position()).vec[2]);
        //TODO check me wierd to find height instead of width working (may it has to do with fov?)
        eyeVector.vec[0] *= 2.0 * k / graph.height();
        eyeVector.vec[1] *= 2.0 * k / graph.height();
        break;
      case ORTHOGRAPHIC:
        float[] wh = graph.getBoundaryWidthHeight();
        // float[] wh = graph.eye().getOrthoWidthHeight();
        eyeVector.vec[0] *= 2.0 * wh[0] / graph.width();
        eyeVector.vec[1] *= 2.0 * wh[1] / graph.height();
        break;
    }
    float coef;
    if (isEye()) {
      // float coef = 8E-4f;
      coef = Math.max(Math.abs((coordinatesOf(graph().anchor())).vec[2] * magnitude()), 0.2f * graph().radius());
      eyeVector.vec[2] *= coef / graph().height();
      // eye wheel seems different
      // trns.vec[2] *= coef * 8E-4f;
      eyeVector.divide(graph().eye().magnitude());
    } else {
      coef = Vector.subtract(graph.eye().position(), position()).magnitude();
      eyeVector.vec[2] *= coef / graph.height();
      eyeVector.divide(graph.eye().magnitude());
    }
    // if( isEye() )
    return eyeVector;
  }

  /**
   * Same as {@code return screenToQuat(angles.vec[0], angles.vec[1], angles.vec[2])}.
   *
   * @see #screenToQuat(float, float, float)
   */
  public Quaternion screenToQuat(Vector angles) {
    return screenToQuat(angles.vec[0], angles.vec[1], angles.vec[2]);
  }

  /**
   * Reduces the screen (device)
   * <a href="http://en.wikipedia.org/wiki/Euler_angles#Extrinsic_rotations"> Extrinsic
   * rotation</a> into a {@link Quaternion}.
   * <p>
   * It's worth noting that all gesture to node motion converting methods, are
   * implemented from just {@link #screenToEye(Vector)}, {@link #eyeToReferenceFrame(Vector)}
   * and {@link #screenToQuat(float, float, float)}.
   *
   * @param roll  Rotation angle in radians around the screen x-Axis
   * @param pitch Rotation angle in radians around the screen y-Axis
   * @param yaw   Rotation angle in radians around the screen z-Axis
   * @see Quaternion#fromEulerAngles(float, float, float)
   */
  public Quaternion screenToQuat(float roll, float pitch, float yaw) {
    if (graph.is2D()) {
      Graph.showDepthWarning("screenToQuat");
      return null;
    }

    // don't really need to differentiate among the two cases, but eyeFrame can
    // be speeded up
    if (isEye() /* || (!isEye() && !this.respectToEye()) */) {
      return new Quaternion(graph.isLeftHanded() ? -roll : roll, pitch, graph.isLeftHanded() ? -yaw : yaw);
    } else {
      Vector trns = new Vector();
      Quaternion q = new Quaternion(graph.isLeftHanded() ? roll : -roll, -pitch, graph.isLeftHanded() ? yaw : -yaw);
      trns.set(-q.x(), -q.y(), -q.z());
      trns = graph.eye().orientation().rotate(trns);
      trns = transformOf(trns);
      q.setX(trns.x());
      q.setY(trns.y());
      q.setZ(trns.z());
      return q;
    }
  }

  @Override
  public void rotateAroundFrame(float roll, float pitch, float yaw, Frame frame) {
    if (frame != null) {
      Frame ref = frame.get();
      if (ref instanceof Node)
        graph.pruneBranch((Node) ref);
      else if (ref instanceof Grabber) {
        graph.inputHandler().removeGrabber((Grabber) ref);
      }
      Node copy = get();
      graph.pruneBranch(copy);
      copy.setReference(ref);
      copy.setWorldMatrix(this);
      ref.rotate(new Quaternion(graph.isLeftHanded() ? -roll : roll, pitch, graph.isLeftHanded() ? -yaw : yaw));
      setWorldMatrix(copy);
      return;
    }
  }

  /**
   * Returns {@code true} when the node is tossing.
   * <p>
   * During tossing, {@link #damping()} translates the node by its
   * {@link #flyDirection()} at a frequency defined when the node
   * {@link #startFlying(MotionEvent, Vector)}.
   * <p>
   * Use {@link #startFlying(MotionEvent, Vector)} and {@link #stopFlying()} to change this
   * state. Default value is {@code false}.
   * <p>
   * {@link #isSpinning()}
   */
  public final boolean isFlying() {
    return flyTimerTask.isActive();
  }

  /**
   * Stops the tossing motion started using {@link #startFlying(MotionEvent, Vector)}.
   * {@link #isFlying()} will return {@code false} after this call.
   * <p>
   * <b>Attention: </b>This method may be called by {@link #damping()}, since tossing may
   * be decelerated according to {@link #damping()} till it stops completely.
   *
   * @see #damping()
   * @see #spin()
   */
  public final void stopFlying() {
    flyTimerTask.stop();
  }

  /**
   * Returns the incremental translation that is applied by {@link #damping()} to the
   * node position when it {@link #isFlying()}.
   * <p>
   * Default value is no translation. Use {@link #setFlyDirection(Vector)} to change this
   * value.
   * <p>
   * <b>Attention: </b>Tossing may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #spinningRotation()
   */
  public final Vector flyDirection() {
    return fDir;
  }

  /**
   * Defines the {@link #flyDirection()} in the reference frame coordinate system.
   *
   * @see #setSpinningRotation(Quaternion)
   */
  public final void setFlyDirection(Vector dir) {
    fDir = dir;
  }

  /**
   * Internal use. Same as {@code startFlying(direction, event.speed())}.
   *
   * @see #startFlying(Vector, float)
   * @see #startSpinning(MotionEvent, Quaternion)
   */
  protected void startFlying(MotionEvent event, Vector direction) {
    startFlying(direction, event.speed());
  }

  /**
   * Starts the tossing of the node.
   * <p>
   * This method starts a timer that will call {@link #damping()} every FLY_UPDATE_PERDIOD
   * milliseconds. The node {@link #isFlying()} until you call
   * {@link #stopFlying()}.
   * <p>
   * <b>Attention: </b>Tossing may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #damping()
   * @see #spin()
   * @see #startFlying(MotionEvent, Vector)
   * @see #startSpinning(Quaternion, float, long)
   */
  public void startFlying(Vector direction, float speed) {
    eventSpeed = speed;
    setFlyDirection(direction);
    flyTimerTask.run(FLY_UPDATE_PERDIOD);
  }

  /**
   * Translates the node by its {@link #flyDirection()}. Invoked by
   * {@link #moveForward(MotionEvent, boolean)} and {@link #drive(MotionEvent)}.
   * <p>
   * <b>Attention: </b>Tossing may be decelerated according to {@link #damping()} till it
   * stops completely.
   *
   * @see #spin()
   */
  protected void fly() {
    translate(flyDirection());
  }

  /**
   * Returns the fly speed, expressed in virtual graph units.
   * <p>
   * It corresponds to the incremental displacement that is periodically applied to the
   * node by {@link #moveForward(MotionEvent, boolean)}.
   * <p>
   * <b>Attention:</b> When frame is set as the
   * {@link Graph#eye()}, this value is set according
   * to the {@link Graph#radius()} by
   * {@link Graph#setRadius(float)}.
   */
  public float flySpeed() {
    return flySpd;
  }

  /**
   * Sets the {@link #flySpeed()}, defined in virtual graph units.
   * <p>
   * Default value is 0.0, but it is modified according to the
   * {@link Graph#radius()} when the node is set
   * as the {@link Graph#eye()}.
   */
  public void setFlySpeed(float speed) {
    flySpd = speed;
  }

  protected Quaternion rollPitchQuaternion(MotionEvent event) {
    MotionEvent2 motionEvent2 = MotionEvent.event2(event);
    if (motionEvent2 != null)
      return rollPitchQuaternion(motionEvent2);
    else {
      Graph.showMinDOFsWarning("rollPitchQuaternion", 2);
      return null;
    }
  }

  /**
   * Returns a Quaternion that is the composition of two rotations, inferred from the
   * mouse roll (X axis) and pitch.
   */
  protected Quaternion rollPitchQuaternion(MotionEvent2 event) {
    if (graph.is2D()) {
      Graph.showDepthWarning("rollPitchQuaternion");
      return null;
    }
    float deltaX = event.dx();
    float deltaY = event.dy();

    if (graph.isRightHanded())
      deltaY = -deltaY;

    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), rotationSensitivity() * deltaY / graph().height());
    Quaternion rotY = new Quaternion(transformOf(upVector), rotationSensitivity() * (-deltaX) / graph().width());
    return Quaternion.multiply(rotY, rotX);
  }

  // drive:

  /**
   * Returns a Quaternion that is a rotation around Y-axis, proportional to the horizontal
   * event X-displacement.
   */
  protected Quaternion turnQuaternion(MotionEvent1 event) {
    float deltaX = event.dx();
    return new Quaternion(new Vector(0.0f, 1.0f, 0.0f), rotationSensitivity() * (-deltaX) / graph().width());
  }

  // end decide

  /**
   * Returns the grabs inputGrabber threshold which is used by the interactive frame to
   * {@link #track(Event)}.
   *
   * @see #setGrabsInputThreshold(float)
   */
  public float grabsInputThreshold() {
    if (isEye()) {
      Graph.showOnlyEyeWarning("grabsInputThreshold", false);
      return 0;
    }
    if (pickingPrecision() == PickingPrecision.ADAPTIVE)
      return grabsInputThreshold * scaling() * graph.pixelToSceneRatio(position());
    return grabsInputThreshold;
  }

  /**
   * Returns the frame picking precision. See
   * {@link #setPickingPrecision(PickingPrecision)} for details.
   *
   * @see #setPickingPrecision(PickingPrecision)
   * @see #setGrabsInputThreshold(float)
   */
  public PickingPrecision pickingPrecision() {
    if (isEye())
      Graph.showOnlyEyeWarning("pickingPrecision", false);
    return pkgnPrecision;
  }

  /**
   * Sets the picking precision of the frame.
   * <p>
   * When {@link #pickingPrecision()} is {@link PickingPrecision#FIXED} or
   * {@link PickingPrecision#ADAPTIVE} Picking is done by checking if the pointer lies
   * within a squared area around the frame {@link #center()} screen projection which size
   * is defined by {@link #setGrabsInputThreshold(float)}.
   * <p>
   * When {@link #pickingPrecision()} is {@link PickingPrecision#EXACT}, picking is done
   * in a precise manner according to the projected pixels of the graphics related to the
   * frame. It is meant to be implemented by generic frame derived classes and requires
   * the graph to implement a so called picking buffer (see the proscene
   * <a href="http://remixlab.github.io/proscene-javadocs/remixlab/proscene/Scene.html">
   * Scene</a> class for a possible implementation) and the frame to implement means to
   * attach graphics to it (see the proscene <a href=
   * "http://remixlab.github.io/proscene-javadocs/remixlab/proscene/InteractiveFrame.html">
   * Node</a> class for a possible implementation). Default implementation of
   * this policy will behave like {@link PickingPrecision#FIXED}.
   *
   * @see #pickingPrecision()
   * @see #setGrabsInputThreshold(float)
   */
  public void setPickingPrecision(PickingPrecision precision) {
    if (precision == PickingPrecision.EXACT)
      System.out.println(
          "Warning: EXACT picking precision will behave like FIXED. EXACT precision is meant to be implemented for derived feneric nodes and scenes that support a pickingBuffer.");
    pkgnPrecision = precision;
    if (isEye()) {
      Graph.showOnlyEyeWarning("setPickingPrecision", false);
      return;
    }
  }

  /**
   * Sets the length of the squared area around the frame {@link #center()} screen
   * projection that defined the {@link #track(Event)} condition used for
   * frame picking.
   * <p>
   * If {@link #pickingPrecision()} is {@link PickingPrecision#FIXED}, the
   * {@code threshold} is expressed in pixels and directly defines the fixed length of a
   * 'shooter target', centered
   * at the projection of the frame origin onto the screen.
   * <p>
   * If {@link #pickingPrecision()} is {@link PickingPrecision#ADAPTIVE}, the
   * {@code threshold} is expressed in object space (world units) and defines the edge
   * length of a squared bounding box that leads to an adaptive length of a
   * 'shooter target',
   * centered at the projection of the frame origin onto the screen. Use this version only
   * if you have a good idea of the bounding box size of the object you are attaching to
   * the frame.
   * <p>
   * The value is meaningless when the {@link #pickingPrecision()} is
   * {@link PickingPrecision#EXACT}. See {@link #setPickingPrecision(PickingPrecision)}
   * for details.
   * <p>
   * Default behavior is to set the {@link #grabsInputThreshold()} (in a non-adaptive
   * manner) to 20.
   * <p>
   * Negative {@code threshold} values are silently ignored.
   *
   * @see #pickingPrecision()
   * @see #grabsInputThreshold()
   * @see #track(Event)
   */
  public void setGrabsInputThreshold(float threshold) {
    if (isEye()) {
      Graph.showOnlyEyeWarning("setGrabsInputThreshold", false);
      return;
    }
    if (threshold >= 0)
      grabsInputThreshold = threshold;
  }

  /**
   * Check if this object is the {@link Agent#inputGrabber()} . Returns
   * {@code true} if this object grabs the agent and {@code false} otherwise.
   */
  public boolean grabsInput(Agent agent) {
    return agent.inputGrabber() == this;
  }

  /**
   * Checks if the frame grabs inputGrabber from any agent registered at the graph inputGrabber handler.
   */
  public boolean grabsInput() {
    for (Agent agent : graph.inputHandler().agents()) {
      if (agent.inputGrabber() == this)
        return true;
    }
    return false;
  }
}
