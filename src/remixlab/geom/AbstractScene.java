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
import remixlab.primitives.*;
import remixlab.primitives.constraint.Constraint;
import remixlab.geom.ik.Solver;
import remixlab.fpstiming.Animator;
import remixlab.fpstiming.AnimatorObject;
import remixlab.fpstiming.TimingHandler;
import remixlab.fpstiming.TimingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A 2D or 3D {@link Grabber} scene.
 * <p>
 * Main package class representing an interface between Dandelion and the outside world.
 * For an introduction to DANDELION please refer to
 * <a href="http://nakednous.github.io/projects/dandelion">this</a>.
 * <p>
 * Instantiated scene {@link InteractiveFrame}s form a scene-tree of
 * transformations which may be traverse with {@link #traverseTree()}. The frame
 * collection belonging to the scene may be retrieved with {@link #frames(boolean)}. The
 * scene provides other useful routines to handle the hierarchy, such as
 * {@link #pruneBranch(InteractiveFrame)}, {@link #appendBranch(List)},
 * {@link #isFrameReachable(InteractiveFrame)}, {@link #branch(InteractiveFrame, boolean)}, and
 * {@link #clearTree()}.
 * <p>
 * Each AbstractScene provides the following main object instances:
 * <ol>
 * <li>An {@link #eye()} which represents the 2D ( {@link Window})
 * or 3D ( {@link Camera}) controlling object. For details please
 * refer to the {@link Eye} class.</li>
 * <li>A {@link #timingHandler()} which control (single-threaded) timing operations. For
 * details please refer to the {@link remixlab.fpstiming.TimingHandler} class.</li>
 * <li>An {@link #inputHandler()} which handles all user input through
 * {@link Agent}s (for details please refer to the
 * {@link InputHandler} class). The {@link #inputHandler()} holds a
 * (default) {@link #motionAgent()} and a (default) {@link #keyAgent()} which should
 * be instantiated by derived classes at construction time.</li>
 * <li>A {@link #matrixHelper()} which handles matrix operations either through the
 * {@link MatrixStackHelper} or through a third party matrix stack
 * (like it's done with Processing). For details please refer to the
 * {@link MatrixHelper} interface.</li>
 * </ol>
 * <h3>Animation mechanisms</h3> The AbstractScene provides two animation mechanisms to
 * define how your scene evolves over time:
 * <ol>
 * <li>Overriding the Dandelion {@link #animate()} method. In this case, once you declare
 * a Scene derived class, you should implement {@link #animate()} which defines how your
 * scene objects evolve over time.
 * <li>By checking if the scene's {@link #timer()} was triggered within the frame.
 * </ol>
 * <p>
 * A grabber scene implements the {@link Grabber} interface and thus
 * can react to user (key) gestures, (see {@link #performInteraction(KeyEvent)}
 * and {@link #checkIfGrabsInput(KeyEvent)}). For example, with the following code:
 * <p>
 * <pre>
 * {@code
 * protected void performInteraction(KeyEvent event) {
 *   if(event.key() == 'z')
 *     toggleCameraType();
 * }
 * }
 * </pre>
 * <p>
 * your custom scene will {@link #toggleCameraType()} when the key 'z' is pressed
 * (provided that scene is the {@link #keyAgent()}
 * {@link Agent#inputGrabber()}).
 */
public abstract class AbstractScene extends AnimatorObject implements Grabber {
  protected boolean dottedGrid;

  // O B J E C T S
  protected MatrixHelper matrixHelper;
  protected Eye eye;
  protected Trackable trck;

  // E X C E P T I O N H A N D L I N G
  protected int startCoordCalls;

  // NUMBER OF FRAMES SINCE THE FIRST SCENE WAS INSTANTIATED
  static public long frameCount;

  // InputHandler
  protected InputHandler iHandler;

  // D I S P L A Y F L A G S
  protected int visualHintMask;

  // LEFT vs RIGHT_HAND
  protected boolean rightHanded;

  // S I Z E
  protected int width, height;

  // offscreen
  protected Point upperLeftCorner;
  protected boolean offscreen;
  protected long lastEqUpdate;

  // FRAME SYNC requires this:
  protected final long deltaCount;

  protected Agent defMotionAgent, defKeyboardAgent;

  /**
   * Visual hints as "the last shall be first"
   */
  public final static int AXES = 1 << 0;
  public final static int GRID = 1 << 1;
  public final static int PICKING = 1 << 2;
  public final static int PATHS = 1 << 3;
  public final static int ZOOM = 1 << 4; // prosceneMouse.zoomOnRegion
  public final static int ROTATE = 1 << 5; // prosceneMouse.screenRotate

  protected static Platform platform;

  public enum Platform {
    PROCESSING_DESKTOP, PROCESSING_ANDROID, PROCESSING_JS
  }

  protected List<InteractiveFrame> seeds;

  // iFrames
  public int nodeCount;

  // public final static int PUP = 1 << 6;
  // public final static int ARP = 1 << 7;

  // IKinematics solvers
  protected List<Solver.TreeSolver> solvers;


  /**
   * Default constructor which defines a right-handed OpenGL compatible Scene with its own
   * {@link MatrixStackHelper}. The constructor also instantiates
   * the {@link #inputHandler()} and the {@link #timingHandler()}, and sets the AXES and
   * GRID visual hint flags.
   * <p>
   * Third party (concrete) Scenes should additionally:
   * <ol>
   * <li>(Optionally) Define a custom {@link #matrixHelper()}. Only if the target platform
   * (such as Processing) provides its own matrix handling.</li>
   * <li>Call {@link #setEye(Eye)} to set the {@link #eye()}, once it's known if the Scene
   * {@link #is2D()} or {@link #is3D()}.</li>
   * <li>Instantiate the {@link #motionAgent()} and the {@link #keyAgent()} and
   * enable them (register them at the {@link #inputHandler()}) and possibly some other
   * {@link Agent}s as well and .</li>
   * <li>Define whether or not the Scene {@link #isOffscreen()}.</li>
   * <li>Call {@link #init()} at the end of the constructor.</li>
   * </ol>
   *
   * @see #timingHandler()
   * @see #inputHandler()
   * @see #setMatrixHelper(MatrixHelper)
   * @see #setRightHanded()
   * @see #setVisualHints(int)
   * @see #setEye(Eye)
   */
  public AbstractScene() {
    seeds = new ArrayList<InteractiveFrame>();
    solvers = new ArrayList<Solver.TreeSolver>();
    setPlatform();
    setTimingHandler(new TimingHandler(this));
    deltaCount = frameCount;
    iHandler = new InputHandler();
    setMatrixHelper(new MatrixStackHelper(this));
    setRightHanded();
    setVisualHints(AXES | GRID);
    upperLeftCorner = new Point(0, 0);
  }

  /**
   * Returns the top-level frames (those which referenceFrame is null).
   * <p>
   * All leading frames are also reachable by the {@link #traverseTree()} algorithm for
   * which they are the seeds.
   *
   * @see #frames(boolean)
   * @see #isFrameReachable(InteractiveFrame)
   * @see #pruneBranch(InteractiveFrame)
   */
  public List<InteractiveFrame> leadingFrames() {
    return seeds;
  }

  /**
   * Returns {@code true} if the frame is top-level.
   */
  protected boolean isLeadingFrame(InteractiveFrame gFrame) {
    for (InteractiveFrame frame : leadingFrames())
      if (frame == gFrame)
        return true;
    return false;
  }

  /**
   * Add the frame as top-level if its reference frame is null and it isn't already added.
   */
  protected boolean addLeadingFrame(InteractiveFrame gFrame) {
    if (gFrame == null || gFrame.referenceFrame() != null)
      return false;
    if (isLeadingFrame(gFrame))
      return false;
    return leadingFrames().add(gFrame);
  }

  /**
   * Removes the leading frame if present. Typically used when re-parenting the frame.
   */
  protected boolean removeLeadingFrame(InteractiveFrame iFrame) {
    boolean result = false;
    Iterator<InteractiveFrame> it = leadingFrames().iterator();
    while (it.hasNext()) {
      if (it.next() == iFrame) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Traverse the frame hierarchy, successively applying the local transformation defined
   * by each traversed frame, and calling
   * {@link InteractiveFrame#visit()} on it.
   * <p>
   * Note that only reachable frames are visited by this algorithm.
   * <p>
   * <b>Attention:</b> this method should be called after {@link #bindMatrices()} (i.e.,
   * eye update) and before any other transformation of the modelview takes place.
   *
   * @see #isFrameReachable(InteractiveFrame)
   * @see #pruneBranch(InteractiveFrame)
   */
  public void traverseTree() {
    for (InteractiveFrame frame : leadingFrames())
      visitFrame(frame);
  }

  /**
   * Used by the traverse frame tree algorithm.
   */
  protected void visitFrame(InteractiveFrame frame) {
    pushModelView();
    applyTransformation(frame);
    frame.visitCallback();
    for (InteractiveFrame child : frame.children())
      visitFrame(child);
    popModelView();
  }

  /**
   * Same as {@code for(InteractiveFrame frame : leadingFrames()) pruneBranch(frame)}.
   *
   * @see #pruneBranch(InteractiveFrame)
   */
  public void clearTree() {
    for (InteractiveFrame frame : leadingFrames())
      pruneBranch(frame);
  }

  /**
   * Make all the frames in the {@code frame} branch eligible for garbage collection.
   * <p>
   * A call to {@link #isFrameReachable(InteractiveFrame)} on all {@code frame} descendants
   * (including {@code frame}) will return false, after issuing this method. It also means
   * that all frames in the {@code frame} branch will become unreachable by the
   * {@link #traverseTree()} algorithm.
   * <p>
   * Frames in the {@code frame} branch will also be removed from all the agents currently
   * registered in the {@link #inputHandler()}.
   * <p>
   * To make all the frames in the branch reachable again, first cache the frames
   * belonging to the branch (i.e., {@code branch=pruneBranch(frame)}) and then call
   * {@link #appendBranch(List)} on the cached branch. Note that calling
   * {@link InteractiveFrame#setReferenceFrame(InteractiveFrame)} on a
   * frame belonging to the pruned branch will become reachable again by the traversal
   * algorithm. In this case, the frame should be manually added to some agents to
   * interactively handle it.
   * <p>
   * Note that if frame is not reachable ({@link #isFrameReachable(InteractiveFrame)}) this
   * method returns {@code null}.
   * <p>
   * When collected, pruned frames behave like {@link Frame},
   * otherwise they are eligible for garbage collection.
   *
   * @see #clearTree()
   * @see #appendBranch(List)
   * @see #isFrameReachable(InteractiveFrame)
   */
  public ArrayList<InteractiveFrame> pruneBranch(InteractiveFrame frame) {
    if (!isFrameReachable(frame))
      return null;
    ArrayList<InteractiveFrame> list = new ArrayList<InteractiveFrame>();
    collectFrames(list, frame, true);
    for (InteractiveFrame gFrame : list) {
      inputHandler().removeGrabber(gFrame);
      if (gFrame.referenceFrame() != null)
        gFrame.referenceFrame().removeChild(gFrame);
      else
        removeLeadingFrame(gFrame);
    }
    return list;
  }

  /**
   * Appends the branch which typically should come from the one pruned (and cached) with
   * {@link #pruneBranch(InteractiveFrame)}.
   * <p>
   * All frames belonging to the branch are automatically added to all scene agents.
   * <p>
   * {@link #pruneBranch(InteractiveFrame)}
   */
  public void appendBranch(List<InteractiveFrame> branch) {
    if (branch == null)
      return;
    for (InteractiveFrame gFrame : branch) {
      inputHandler().addGrabber(gFrame);
      if (gFrame.referenceFrame() != null)
        gFrame.referenceFrame().addChild(gFrame);
      else
        addLeadingFrame(gFrame);
    }
  }

  /**
   * Returns {@code true} if the frame is reachable by the {@link #traverseTree()}
   * algorithm and {@code false} otherwise.
   * <p>
   * Frames are make unreachable with {@link #pruneBranch(InteractiveFrame)} and reachable
   * again with
   * {@link InteractiveFrame#setReferenceFrame(InteractiveFrame)}.
   *
   * @see #traverseTree()
   * @see #frames(boolean)
   */
  public boolean isFrameReachable(InteractiveFrame frame) {
    if (frame == null)
      return false;
    return frame.referenceFrame() == null ? isLeadingFrame(frame) : frame.referenceFrame().hasChild(frame);
  }

  /**
   * Returns a list of all the frames that are reachable by the {@link #traverseTree()}
   * algorithm, including the EyeFrames (when {@code eyeframes} is {@code true}).
   *
   * @see #isFrameReachable(InteractiveFrame)
   * @see InteractiveFrame#isEyeFrame()
   */
  public ArrayList<InteractiveFrame> frames(boolean eyeframes) {
    ArrayList<InteractiveFrame> list = new ArrayList<InteractiveFrame>();
    for (InteractiveFrame gFrame : leadingFrames())
      collectFrames(list, gFrame, eyeframes);
    return list;
  }

  /**
   * Collects {@code frame} and all its descendant frames. When {@code eyeframes} is
   * {@code true} eye-frames will also be collected. Note that for a frame to be collected
   * it must be reachable.
   *
   * @see #isFrameReachable(InteractiveFrame)
   */
  public ArrayList<InteractiveFrame> branch(InteractiveFrame frame, boolean eyeframes) {
    ArrayList<InteractiveFrame> list = new ArrayList<InteractiveFrame>();
    collectFrames(list, frame, eyeframes);
    return list;
  }

  /**
   * Returns a straight path of frames between {@code tail} and {@code tip}. When {@code eyeframes} is
   * {@code true} eye-frames will also be included.
   * <p>
   * If {@code tip} is descendant of {@code tail} the returned list will include both of them. Otherwise it will be empty.
   */
  //TODO decide me
  public ArrayList<InteractiveFrame> branch(InteractiveFrame tail, InteractiveFrame tip, boolean eyeframes) {
    ArrayList<InteractiveFrame> list = new ArrayList<InteractiveFrame>();
    //1. Check if tip is a tail descendant
    boolean desc = false;
    ArrayList<InteractiveFrame> descList = branch(tail, eyeframes);
    for(InteractiveFrame gFrame : descList)
      if(gFrame == tip) {
        desc = true;
        break;
      }
    //2. If so, return the path between the two
    if(desc) {
      InteractiveFrame _tip = tip;
      while(_tip != tail) {
        if (!_tip.isEyeFrame() || eyeframes)
          list.add(0, _tip);
          _tip = _tip.referenceFrame();
      }
      list.add(0, tail);
    }
    return list;
  }

  /**
   * Collects {@code frame} and all its descendant frames. When {@code eyeframes} is
   * {@code true} eye-frames will also be collected. Note that for a frame to be collected
   * it must be reachable.
   *
   * @see #isFrameReachable(InteractiveFrame)
   */
  protected void collectFrames(List<InteractiveFrame> list, InteractiveFrame frame, boolean eyeframes) {
    if (frame == null)
      return;
    if (!frame.isEyeFrame() || eyeframes)
      list.add(frame);
    for (InteractiveFrame child : frame.children())
      collectFrames(list, child, eyeframes);
  }

  // Actions

  /**
   * Same as {@code eye().addKeyFrameToPath(1)}.
   *
   * @see Eye#addKeyFrameToPath(int)
   */
  public void addKeyFrameToPath1() {
    eye().addKeyFrameToPath(1);
  }

  /**
   * Same as {@code eye().addKeyFrameToPath(2)}.
   *
   * @see Eye#addKeyFrameToPath(int)
   */
  public void addKeyFrameToPath2() {
    eye().addKeyFrameToPath(2);
  }

  /**
   * Same as {@code eye().addKeyFrameToPath(1)}.
   *
   * @see Eye#addKeyFrameToPath(int)
   */
  public void addKeyFrameToPath3() {
    eye().addKeyFrameToPath(3);
  }

  /**
   * Same as {@code eye().deletePath(1)}.
   *
   * @see Eye#deletePath(int)
   */
  public void deletePath1() {
    eye().deletePath(1);
  }

  /**
   * Same as {@code eye().deletePath(2)}.
   *
   * @see Eye#deletePath(int)
   */
  public void deletePath2() {
    eye().deletePath(2);
  }

  /**
   * Same as {@code eye().deletePath(3)}.
   *
   * @see Eye#deletePath(int)
   */
  public void deletePath3() {
    eye().deletePath(3);
  }

  /**
   * Same as {@code eye().playPath(1)}.
   *
   * @see Eye#playPath(int)
   */
  public void playPath1() {
    eye().playPath(1);
  }

  /**
   * Same as {@code eye().playPath(2)}.
   *
   * @see Eye#playPath(int)
   */
  public void playPath2() {
    eye().playPath(2);
  }

  /**
   * Same as {@code eye().playPath(3)}.
   *
   * @see Eye#playPath(int)
   */
  public void playPath3() {
    eye().playPath(3);
  }

  /**
   * Same as {@code eye().interpolateToFitScene()}.
   *
   * @see Eye#interpolateToFitScene()
   */
  public void interpolateToFitScene() {
    eye().interpolateToFitScene();
  }

  /**
   * Same as {@code eye().setAnchor(new Vec(0, 0, 0))}.
   *
   * @see Eye#setAnchor(Vec)
   */
  public void resetAnchor() {
    eye().setAnchor(new Vec(0, 0, 0));
    // looks horrible, but works ;)
    eye().anchorFlag = true;
    eye().runResetAnchorHintTimer(1000);
  }

  // Grabber Implementation

  @Override
  public boolean checkIfGrabsInput(Event event) {
    if (event instanceof KeyEvent)
      return checkIfGrabsInput((KeyEvent) event);
    if (event instanceof ClickEvent)
      return checkIfGrabsInput((ClickEvent) event);
    if (event instanceof MotionEvent)
      return checkIfGrabsInput((MotionEvent) event);
    return false;
  }

  /**
   * Internal use. You don't need to call this.
   * <p>
   * Automatically called by agents handling this frame.
   */
  public boolean checkIfGrabsInput(MotionEvent event) {
    if (event instanceof DOF1Event)
      return checkIfGrabsInput((DOF1Event) event);
    if (event instanceof DOF2Event)
      return checkIfGrabsInput((DOF2Event) event);
    if (event instanceof DOF3Event)
      return checkIfGrabsInput((DOF3Event) event);
    if (event instanceof DOF6Event)
      return checkIfGrabsInput((DOF6Event) event);
    return false;
  }

  /**
   * Internal use. You don't need to call this.
   * <p>
   * Automatically called by agents handling this frame.
   */
  public boolean checkIfGrabsInput(ClickEvent event) {
    AbstractScene.showMissingImplementationWarning("checkIfGrabsInput(clickEvent event)", this.getClass().getName());
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  public boolean checkIfGrabsInput(KeyEvent event) {
    AbstractScene.showMissingImplementationWarning("checkIfGrabsInput(KeyEvent event)", this.getClass().getName());
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   * <p>
   * Override this method when you want the object to be picked from a {@link remixlab.bias.event.DOF1Event}.
   */
  public boolean checkIfGrabsInput(DOF1Event event) {
    AbstractScene.showMissingImplementationWarning("checkIfGrabsInput(DOF1Event event)", this.getClass().getName());
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   * <p>
   * Override this method when you want the object to be picked from a {@link remixlab.bias.event.DOF1Event}.
   */
  public boolean checkIfGrabsInput(DOF2Event event) {
    AbstractScene.showMissingImplementationWarning("checkIfGrabsInput(DOF2Event event)", this.getClass().getName());
    return false;
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean checkIfGrabsInput(DOF3Event event) {
    return checkIfGrabsInput(event.dof2Event());
  }

  /**
   * Internal use. You don't need to call this. Automatically called by agents handling this frame.
   */
  public boolean checkIfGrabsInput(DOF6Event event) {
    return checkIfGrabsInput(event.dof3Event().dof2Event());
  }

  @Override
  public void performInteraction(Event event) {
    if (event instanceof ClickEvent)
      performInteraction((ClickEvent) event);
    if (event instanceof MotionEvent)
      performInteraction((MotionEvent) event);
    if (event instanceof KeyEvent)
      performInteraction((KeyEvent) event);
  }

  /**
   * Calls performInteraction() on the proper motion event:
   * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
   * {@link remixlab.bias.event.DOF3Event} or {@link remixlab.bias.event.DOF6Event}.
   * <p>
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.MotionEvent}.
   */
  protected void performInteraction(MotionEvent event) {
    if (event instanceof DOF1Event)
      performInteraction((DOF1Event) event);
    if (event instanceof DOF2Event)
      performInteraction((DOF2Event) event);
    if (event instanceof DOF3Event)
      performInteraction((DOF3Event) event);
    if (event instanceof DOF6Event)
      performInteraction((DOF6Event) event);
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF1Event}.
   */
  protected void performInteraction(DOF1Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF2Event}.
   */
  protected void performInteraction(DOF2Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF3Event}.
   */
  protected void performInteraction(DOF3Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF6Event}.
   */
  protected void performInteraction(DOF6Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.ClickEvent}.
   */
  protected void performInteraction(ClickEvent event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link KeyEvent}.
   */
  protected void performInteraction(KeyEvent event) {
    AbstractScene.showMissingImplementationWarning("performInteraction(KeyEvent event)", this.getClass().getName());
  }

  /**
   * Check if this object is the {@link Agent#inputGrabber()} . Returns
   * {@code true} if this object grabs the agent and {@code false} otherwise.
   */
  public boolean grabsInput(Agent agent) {
    return agent.inputGrabber() == this;
  }

  /**
   * Checks if the scene grabs input from any agent registered at the input handler.
   */
  public boolean grabsInput() {
    for (Agent agent : inputHandler().agents()) {
      if (agent.inputGrabber() == this)
        return true;
    }
    return false;
  }

  //

  /**
   * Returns the upper left corner of the Scene window. It's always (0,0) for on-screen
   * scenes, but off-screen scenes may be defined elsewhere on a canvas.
   */
  public Point originCorner() {
    return upperLeftCorner;
  }

  /**
   * Determines under which platform dandelion is running. Either DESKTOP, ANDROID or JS.
   */
  protected abstract void setPlatform();

  /**
   * Returns the platform where dandelion is running. Either DESKTOP, ANDROID or JS.
   */
  public static Platform platform() {
    return platform;
  }

  // AGENTs

  // Keys

  /**
   * Returns the default {@link Agent} key agent.
   *
   * @see #motionAgent()
   */
  public Agent keyAgent() {
    return defKeyboardAgent;
  }

  /**
   * Returns {@code true} if the {@link #keyAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #enableKeyAgent()
   * @see #disableKeyAgent()
   * @see #isMotionAgentEnabled()
   */
  public boolean isKeyAgentEnabled() {
    return inputHandler().isAgentRegistered(keyAgent());
  }

  /**
   * Enables key handling through the {@link #keyAgent()}.
   *
   * @see #isKeyAgentEnabled()
   * @see #disableKeyAgent()
   * @see #enableMotionAgent()
   */
  public void enableKeyAgent() {
    if (!inputHandler().isAgentRegistered(keyAgent())) {
      inputHandler().registerAgent(keyAgent());
    }
  }

  // Motion agent

  /**
   * Returns the default motion agent.
   *
   * @see #keyAgent()
   */
  public Agent motionAgent() {
    return defMotionAgent;
  }

  /**
   * Returns {@code true} if the {@link #motionAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #enableMotionAgent()
   * @see #disableMotionAgent()
   * @see #isKeyAgentEnabled()
   */
  public boolean isMotionAgentEnabled() {
    return inputHandler().isAgentRegistered(motionAgent());
  }

  /**
   * Enables motion handling through the {@link #motionAgent()}.
   *
   * @see #isMotionAgentEnabled()
   * @see #disableMotionAgent()
   * @see #enableKeyAgent()
   */
  public void enableMotionAgent() {
    if (!inputHandler().isAgentRegistered(motionAgent())) {
      inputHandler().registerAgent(motionAgent());
    }
  }

  /**
   * Disables the default {@link Agent} and returns it.
   *
   * @see #isKeyAgentEnabled()
   * @see #enableMotionAgent()
   * @see #disableMotionAgent()
   */
  public boolean disableKeyAgent() {
    return inputHandler().unregisterAgent(keyAgent());
  }

  /**
   * Disables the default motion agent and returns it.
   *
   * @see #isMotionAgentEnabled()
   * @see #enableMotionAgent()
   * @see #enableKeyAgent()
   */
  public boolean disableMotionAgent() {
    return inputHandler().unregisterAgent(motionAgent());
  }

  // FPSTiming STUFF

  /**
   * Returns the number of frames displayed since the scene was instantiated.
   * <p>
   * Use {@code AbstractScene.frameCount} to retrieve the number of frames displayed since
   * the first scene was instantiated.
   */
  public long frameCount() {
    return timingHandler().frameCount();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerTask(task)}.
   *
   * @see remixlab.fpstiming.TimingHandler#registerTask(TimingTask)
   */
  public void registerTimingTask(TimingTask task) {
    timingHandler().registerTask(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterTask(task)}.
   */
  public void unregisterTimingTask(TimingTask task) {
    timingHandler().unregisterTask(task);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isTaskRegistered(task)}.
   */
  public boolean isTimingTaskRegistered(TimingTask task) {
    return timingHandler().isTaskRegistered(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerAnimator(object)}.
   */
  public void registerAnimator(Animator object) {
    timingHandler().registerAnimator(object);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterAnimator(object)}.
   *
   * @see remixlab.fpstiming.TimingHandler#unregisterAnimator(Animator)
   */
  public void unregisterAnimator(Animator object) {
    timingHandler().unregisterAnimator(object);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isAnimatorRegistered(object)}.
   *
   * @see remixlab.fpstiming.TimingHandler#isAnimatorRegistered(Animator)
   */
  public boolean isAnimatorRegistered(Animator object) {
    return timingHandler().isAnimatorRegistered(object);
  }

  // E V E N T H A N D L I N G

  /**
   * Returns the scene {@link InputHandler}.
   */
  public InputHandler inputHandler() {
    return iHandler;
  }

  /**
   * Convenience function that simply returns {@code inputHandler().info()}.
   *
   * @see #displayInfo(boolean)
   */
  public abstract String info();

  /**
   * Convenience function that simply calls {@code displayInfo(true)}.
   */
  public void displayInfo() {
    displayInfo(true);
  }

  /**
   * Displays the {@link #info()} bindings.
   *
   * @param onConsole if this flag is true displays the help on console. Otherwise displays it on
   *                  the applet
   * @see #info()
   */
  public void displayInfo(boolean onConsole) {
    if (onConsole)
      System.out.println(info());
    else
      AbstractScene.showMissingImplementationWarning("displayInfo", getClass().getName());
  }

  // 1. Scene overloaded

  // MATRIX and TRANSFORMATION STUFF

  /**
   * Sets the {@link MatrixHelper} defining how dandelion matrices
   * are to be handled.
   *
   * @see #matrixHelper()
   */
  public void setMatrixHelper(MatrixHelper r) {
    matrixHelper = r;
  }

  /**
   * Returns the {@link MatrixHelper}.
   *
   * @see #setMatrixHelper(MatrixHelper)
   */
  public MatrixHelper matrixHelper() {
    return matrixHelper;
  }

  /**
   * Wrapper for {@link MatrixHelper#beginScreenDrawing()}. Adds
   * exception when no properly closing the screen drawing with a call to
   * {@link #endScreenDrawing()}.
   *
   * @see MatrixHelper#beginScreenDrawing()
   */
  public void beginScreenDrawing() {
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
          + "endScreenDrawing() and they cannot be nested. Check your implementation!");

    startCoordCalls++;

    disableDepthTest();
    matrixHelper.beginScreenDrawing();
  }

  /**
   * Wrapper for {@link MatrixHelper#endScreenDrawing()} . Adds
   * exception if {@link #beginScreenDrawing()} wasn't properly called before
   *
   * @see MatrixHelper#endScreenDrawing()
   */
  public void endScreenDrawing() {
    startCoordCalls--;
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
          + "endScreenDrawing() and they cannot be nested. Check your implementation!");

    matrixHelper.endScreenDrawing();
    enableDepthTest();
  }

  /**
   * Wrapper for {@link MatrixHelper#bind()}
   */
  protected void bindMatrices() {
    matrixHelper.bind();
  }

  /**
   * Wrapper for {@link MatrixHelper#pushModelView()}
   */
  public void pushModelView() {
    matrixHelper.pushModelView();
  }

  /**
   * Wrapper for {@link MatrixHelper#popModelView()}
   */
  public void popModelView() {
    matrixHelper.popModelView();
  }

  /**
   * Wrapper for {@link MatrixHelper#pushProjection()}
   */
  public void pushProjection() {
    matrixHelper.pushProjection();
  }

  /**
   * Wrapper for {@link MatrixHelper#popProjection()}
   */
  public void popProjection() {
    matrixHelper.popProjection();
  }

  /**
   * Wrapper for {@link MatrixHelper#translate(float, float)}
   */
  public void translate(float tx, float ty) {
    matrixHelper.translate(tx, ty);
  }

  /**
   * Wrapper for
   * {@link MatrixHelper#translate(float, float, float)}
   */
  public void translate(float tx, float ty, float tz) {
    matrixHelper.translate(tx, ty, tz);
  }

  /**
   * Wrapper for {@link MatrixHelper#rotate(float)}
   */
  public void rotate(float angle) {
    matrixHelper.rotate(angle);
  }

  /**
   * Wrapper for {@link MatrixHelper#rotateX(float)}
   */
  public void rotateX(float angle) {
    matrixHelper.rotateX(angle);
  }

  /**
   * Wrapper for {@link MatrixHelper#rotateY(float)}
   */
  public void rotateY(float angle) {
    matrixHelper.rotateY(angle);
  }

  /**
   * Wrapper for {@link MatrixHelper#rotateZ(float)}
   */
  public void rotateZ(float angle) {
    matrixHelper.rotateZ(angle);
  }

  /**
   * Wrapper for
   * {@link MatrixHelper#rotate(float, float, float, float)}
   */
  public void rotate(float angle, float vx, float vy, float vz) {
    matrixHelper.rotate(angle, vx, vy, vz);
  }

  /**
   * Wrapper for {@link MatrixHelper#scale(float)}
   */
  public void scale(float s) {
    matrixHelper.scale(s);
  }

  /**
   * Wrapper for {@link MatrixHelper#scale(float, float)}
   */
  public void scale(float sx, float sy) {
    matrixHelper.scale(sx, sy);
  }

  /**
   * Wrapper for {@link MatrixHelper#scale(float, float, float)}
   */
  public void scale(float x, float y, float z) {
    matrixHelper.scale(x, y, z);
  }

  /**
   * Wrapper for {@link MatrixHelper#resetModelView()}
   */
  public void resetModelView() {
    matrixHelper.resetModelView();
  }

  /**
   * Wrapper for {@link MatrixHelper#resetProjection()}
   */
  public void resetProjection() {
    matrixHelper.resetProjection();
  }

  /**
   * Wrapper for {@link MatrixHelper#applyModelView(Mat)}
   */
  public void applyModelView(Mat source) {
    matrixHelper.applyModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHelper#applyProjection(Mat)}
   */
  public void applyProjection(Mat source) {
    matrixHelper.applyProjection(source);
  }

  /**
   * Wrapper for {@link MatrixHelper#modelView()}
   */
  public Mat modelView() {
    return matrixHelper.modelView();
  }

  /**
   * Wrapper for {@link MatrixHelper#projection()}
   */
  public Mat projection() {
    return matrixHelper.projection();
  }

  /**
   * Wrapper for {@link MatrixHelper#getModelView(Mat)}
   */
  public Mat getModelView(Mat target) {
    return matrixHelper.getModelView(target);
  }

  /**
   * Wrapper for {@link MatrixHelper#getProjection(Mat)}
   */
  public Mat getProjection(Mat target) {
    return matrixHelper.getProjection(target);
  }

  /**
   * Wrapper for {@link MatrixHelper#setModelView(Mat)}
   */
  public void setModelView(Mat source) {
    matrixHelper.setModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHelper#setProjection(Mat)}
   */
  public void setProjection(Mat source) {
    matrixHelper.setProjection(source);
  }

  /**
   * Wrapper for {@link MatrixHelper#printModelView()}
   */
  public void printModelView() {
    matrixHelper.printModelView();
  }

  /**
   * Wrapper for {@link MatrixHelper#printProjection()}
   */
  public void printProjection() {
    matrixHelper.printProjection();
  }

  /**
   * Wrapper for
   * {@link MatrixHelper#isProjectionViewInverseCached()} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vec)}.
   *
   * @see #optimizeUnprojectedCoordinatesOf(boolean)
   * @see #unprojectedCoordinatesOf(Vec)
   */
  public boolean isUnprojectedCoordinatesOfOptimized() {
    return matrixHelper.isProjectionViewInverseCached();
  }

  /**
   * Wrapper for
   * {@link MatrixHelper#cacheProjectionViewInverse(boolean)} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vec)}.
   *
   * @see #isUnprojectedCoordinatesOfOptimized()
   * @see #unprojectedCoordinatesOf(Vec)
   */
  public void optimizeUnprojectedCoordinatesOf(boolean optimise) {
    matrixHelper.cacheProjectionViewInverse(optimise);
  }

  // DRAWING STUFF

  /**
   * Returns the visual hints flag.
   */
  public int visualHints() {
    return this.visualHintMask;
  }

  /**
   * Low level setting of visual flags. You'd prefer {@link #setAxesVisualHint(boolean)},
   * {@link #setGridVisualHint(boolean)}, {@link #setPathsVisualHint(boolean)} and
   * {@link #setPickingVisualHint(boolean)}, unless you want to set them all at once,
   * e.g., {@code setVisualHints(Scene.AXES | Scene.GRID | Scene.PATHS | Scene.PICKING)}.
   */
  public void setVisualHints(int flag) {
    visualHintMask = flag;
  }

  /**
   * Toggles the state of {@link #axesVisualHint()}.
   *
   * @see #axesVisualHint()
   * @see #setAxesVisualHint(boolean)
   */
  public void toggleAxesVisualHint() {
    setAxesVisualHint(!axesVisualHint());
  }

  /**
   * Toggles the state of {@link #gridVisualHint()}.
   *
   * @see #setGridVisualHint(boolean)
   */
  public void toggleGridVisualHint() {
    setGridVisualHint(!gridVisualHint());
  }

  /**
   * Toggles the state of {@link #pickingVisualHint()}.
   *
   * @see #setPickingVisualHint(boolean)
   */
  public void togglePickingVisualhint() {
    setPickingVisualHint(!pickingVisualHint());
  }

  /**
   * Toggles the state of {@link #pathsVisualHint()}.
   *
   * @see #setPathsVisualHint(boolean)
   */
  public void togglePathsVisualHint() {
    setPathsVisualHint(!pathsVisualHint());
  }

  /**
   * Internal :p
   */
  protected void toggleZoomVisualHint() {
    setZoomVisualHint(!zoomVisualHint());
  }

  /**
   * Internal :p
   */
  protected void toggleRotateVisualHint() {
    setRotateVisualHint(!rotateVisualHint());
  }

  /**
   * Returns {@code true} if axes are currently being drawn and {@code false} otherwise.
   */
  public boolean axesVisualHint() {
    return ((visualHintMask & AXES) != 0);
  }

  /**
   * Returns {@code true} if grid is currently being drawn and {@code false} otherwise.
   */
  public boolean gridVisualHint() {
    return ((visualHintMask & GRID) != 0);
  }

  /**
   * Returns {@code true} if the picking selection visual hint is currently being drawn
   * and {@code false} otherwise.
   */
  public boolean pickingVisualHint() {
    return ((visualHintMask & PICKING) != 0);
  }

  /**
   * Returns {@code true} if the eye paths visual hints are currently being drawn and
   * {@code false} otherwise.
   */
  public boolean pathsVisualHint() {
    return ((visualHintMask & PATHS) != 0);
  }

  /**
   * Internal. Third parties should not call this.
   */
  public boolean zoomVisualHint() {
    return ((visualHintMask & ZOOM) != 0);
  }

  /**
   * Internal. Third parties should not call this.
   */
  public boolean rotateVisualHint() {
    return ((visualHintMask & ROTATE) != 0);
  }

  /**
   * Sets the display of the axes according to {@code draw}
   */
  public void setAxesVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= AXES;
    else
      visualHintMask &= ~AXES;
  }

  /**
   * Sets the display of the grid according to {@code draw}
   */
  public void setGridVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= GRID;
    else
      visualHintMask &= ~GRID;
  }

  /**
   * Sets the display of the interactive frames' selection hints according to {@code draw}
   */
  public void setPickingVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= PICKING;
    else
      visualHintMask &= ~PICKING;
  }

  /**
   * Sets the display of the camera key frame paths according to {@code draw}
   */
  public void setPathsVisualHint(boolean draw) {
    if (draw) {
      if (eye() != null) {
        visualHintMask |= PATHS;
        eye().attachPaths();
      } else
        System.err.println("Warning: null eye, no path attached!");
    } else {
      if (eye() != null) {
        visualHintMask &= ~PATHS;
        eye().detachPaths();
      } else
        System.err.println("Warning: null eye, no path dettached!");
    }
  }

  /**
   * Internal. Third parties should not call this.
   */
  public void setZoomVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= ZOOM;
    else
      visualHintMask &= ~ZOOM;
  }

  /**
   * Internal. Third parties should not call this.
   */
  public void setRotateVisualHint(boolean draw) {
    if (draw)
      visualHintMask |= ROTATE;
    else
      visualHintMask &= ~ROTATE;
  }

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Handles the {@link #avatar()}</li>
   * <li>Calls {@link #bindMatrices()}</li>
   * <li>Calls {@link Eye#updateBoundaryEquations()} if
   * {@link #areBoundaryEquationsEnabled()}</li>
   * <li>Calls {@link #proscenium()}</li>
   * <li>Calls {@link #displayVisualHints()}.</li>
   * </ol>
   *
   * @see #postDraw()
   */
  public void preDraw() {
    // 1. Avatar
    if (avatar() != null && (!eye().anyInterpolationStarted()))
      eye().frame().setWorldMatrix(avatar().trackingEyeFrame());
    // 2. Eye
    bindMatrices();
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > lastEqUpdate || lastEqUpdate == 0)) {
      eye().updateBoundaryEquations();
      lastEqUpdate = frameCount;
    }
    // 3. Alternative use only
    proscenium();
    // 4. Display visual hints
    displayVisualHints(); // abstract
  }

  /**
   * Called after your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link remixlab.fpstiming.TimingHandler#handle()} and increments the the
   * {@link #frameCount()}</li>
   * <li>Increments the {@link #frameCount()}</li>
   * <li>Calls {@link InputHandler#handle()}</li>
   * </ol>
   *
   * @see #preDraw()
   */
  public void postDraw() {
    // 1. timers (include IK Solvers' execution in the order they were registered)
    timingHandler().handle();
    if (frameCount < timingHandler().frameCount())
      frameCount = timingHandler().frameCount();
    if (frameCount < timingHandler().frameCount() + deltaCount)
      frameCount = timingHandler().frameCount() + deltaCount;
    // 2. Agents
    inputHandler().handle();
  }

  /**
   * Internal use. Display various on-screen visual hints to be called from
   * {@link #postDraw()}.
   */
  protected void displayVisualHints() {
    if (gridVisualHint())
      drawGridHint();
    if (axesVisualHint())
      drawAxesHint();
    if (pickingVisualHint())
      drawPickingHint();
    if (pathsVisualHint())
      drawPathsHint();
    if (zoomVisualHint())
      drawZoomWindowHint();
    if (rotateVisualHint())
      drawScreenRotateHint();
    if (eye().anchorFlag)
      drawAnchorHint();
    if (eye().pupFlag)
      drawPointUnderPixelHint();
  }

  /**
   * Internal use.
   */
  protected void drawPickingHint() {
    drawPickingTargets();
  }

  protected void drawPickingTargets() {
    for (InteractiveFrame frame : frames(false))
      // if(inputHandler().hasGrabber(frame))
      if (frame.isVisualHintEnabled())
        drawPickingTarget(frame);
  }

  /**
   * Internal use.
   */
  protected void drawAxesHint() {
    drawAxes(eye().sceneRadius());
  }

  /**
   * Internal use.
   */
  protected void drawGridHint() {
    if (gridIsDotted())
      drawDottedGrid(eye().sceneRadius());
    else
      drawGrid(eye().sceneRadius());
  }

  /**
   * Internal use.
   */
  protected void drawPathsHint() {
    drawPaths();
  }

  protected void drawPaths() {
    // Iterator<Integer> itrtr = eye.kfi.keySet().iterator(); while (itrtr.hasNext()) {
    // Integer key = itrtr.next();
    // drawPath(eye.keyFrameInterpolatorMap().get(key), 3, is3D() ? 5 : 2, radius());
    // }

    // alternative:
    // /*
    KeyFrameInterpolator[] k = eye.keyFrameInterpolatorArray();
    for (int i = 0; i < k.length; i++)
      drawPath(k[i], 3, 5, radius());
    // */
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, 1, 6, 100)}.
   *
   * @see #drawPath(KeyFrameInterpolator, int, int, float)
   */
  public void drawPath(KeyFrameInterpolator kfi) {
    drawPath(kfi, 1, 6, 100);
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, 1, 6, scale)}
   *
   * @see #drawPath(KeyFrameInterpolator, int, int, float)
   */
  public void drawPath(KeyFrameInterpolator kfi, float scale) {
    drawPath(kfi, 1, 6, scale);
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, mask, nbFrames, * 100)}
   *
   * @see #drawPath(KeyFrameInterpolator, int, int, float)
   */
  public void drawPath(KeyFrameInterpolator kfi, int mask, int nbFrames) {
    drawPath(kfi, mask, nbFrames, 100);
  }

  /**
   * Convenience function that simply calls {@code drawAxis(100)}.
   */
  public void drawAxes() {
    drawAxes(100);
  }

  /**
   * Convenience function that simplt calls {@code drawDottedGrid(100, 10)}.
   */
  public void drawDottedGrid() {
    drawDottedGrid(100, 10);
  }

  /**
   * Convenience function that simply calls {@code drawGrid(100, 10)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid() {
    drawGrid(100, 10);
  }

  /**
   * Convenience function that simplt calls {@code drawDottedGrid(size, 10)}.
   */
  public void drawDottedGrid(float size) {
    drawDottedGrid(size, 10);
  }

  /**
   * Convenience function that simply calls {@code drawGrid(size, 10)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid(float size) {
    drawGrid(size, 10);
  }

  /**
   * Convenience function that simplt calls {@code drawDottedGrid(100, nbSubdivisions)}.
   */
  public void drawDottedGrid(int nbSubdivisions) {
    drawDottedGrid(100, nbSubdivisions);
  }

  /**
   * Convenience function that simply calls {@code drawGrid(100, nbSubdivisions)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid(int nbSubdivisions) {
    drawGrid(100, nbSubdivisions);
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(6)}.
   *
   * @see #drawTorusSolenoid(int, int, float, float)
   */
  public void drawTorusSolenoid() {
    drawTorusSolenoid(6);
  }

  /**
   * Convenience function that simply calls
   * {@code drawTorusSolenoid(faces, 0.07f * radius())}.
   *
   * @see #drawTorusSolenoid(int, int, float, float)
   */
  public void drawTorusSolenoid(int faces) {
    drawTorusSolenoid(faces, 0.07f * radius());
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(6, insideRadius)}.
   *
   * @see #drawTorusSolenoid(int, int, float, float)
   */
  public void drawTorusSolenoid(float insideRadius) {
    drawTorusSolenoid(6, insideRadius);
  }

  /**
   * Convenience function that simply calls
   * {@code drawTorusSolenoid(faces, 100, insideRadius, insideRadius * 1.3f)}.
   *
   * @see #drawTorusSolenoid(int, int, float, float)
   */
  public void drawTorusSolenoid(int faces, float insideRadius) {
    drawTorusSolenoid(faces, 100, insideRadius, insideRadius * 1.3f);
  }

  /**
   * Draws a torus solenoid. Dandelion logo.
   *
   * @param faces
   * @param detail
   * @param insideRadius
   * @param outsideRadius
   */
  public abstract void drawTorusSolenoid(int faces, int detail, float insideRadius, float outsideRadius);

  /**
   * Same as {@code cone(det, 0, 0, r, h);}
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(int det, float r, float h) {
    drawCone(det, 0, 0, r, h);
  }

  /**
   * Same as {@code cone(12, 0, 0, r, h);}
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(float r, float h) {
    drawCone(12, 0, 0, r, h);
  }

  /**
   * Same as {@code cone(det, 0, 0, r1, r2, h);}
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public void drawCone(int det, float r1, float r2, float h) {
    drawCone(det, 0, 0, r1, r2, h);
  }

  /**
   * Same as {@code cone(18, 0, 0, r1, r2, h);}
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public void drawCone(float r1, float r2, float h) {
    drawCone(18, 0, 0, r1, r2, h);
  }

  /**
   * Simply calls {@code drawArrow(length, 0.05f * length)}
   *
   * @see #drawArrow(float, float)
   */
  public void drawArrow(float length) {
    drawArrow(length, 0.05f * length);
  }

  /**
   * Draws a 3D arrow along the positive Z axis.
   * <p>
   * {@code length} and {@code radius} define its geometry.
   * <p>
   * Use {@link #drawArrow(Vec, Vec, float)} to place the arrow in 3D.
   */
  public void drawArrow(float length, float radius) {
    float head = 2.5f * (radius / length) + 0.1f;
    float coneRadiusCoef = 4.0f - 5.0f * head;

    drawCylinder(radius, length * (1.0f - head / coneRadiusCoef));
    translate(0.0f, 0.0f, length * (1.0f - head));
    drawCone(coneRadiusCoef * radius, head * length);
    translate(0.0f, 0.0f, -length * (1.0f - head));
  }

  /**
   * Draws a 3D arrow between the 3D point {@code from} and the 3D point {@code to}, both
   * defined in the current world coordinate system.
   *
   * @see #drawArrow(float, float)
   */
  public void drawArrow(Vec from, Vec to, float radius) {
    pushModelView();
    translate(from.x(), from.y(), from.z());
    applyModelView(new Quat(new Vec(0, 0, 1), Vec.subtract(to, from)).matrix());
    drawArrow(Vec.subtract(to, from).magnitude(), radius);
    popModelView();
  }

  /**
   * Convenience function that simply calls
   * {@code drawCross(pg3d.color(255, 255, 255), px, py, 15, 3)}.
   */
  public void drawCross(float px, float py) {
    drawCross(px, py, 30);
  }

  /**
   * Convenience function that simply calls {@code drawFilledCircle(40, center, radius)}.
   *
   * @see #drawFilledCircle(int, Vec, float)
   */
  public void drawFilledCircle(Vec center, float radius) {
    drawFilledCircle(40, center, radius);
  }

  // abstract drawing methods

  /**
   * Draws a cylinder of width {@code w} and height {@code h}, along the positive
   * {@code z} axis.
   */
  public abstract void drawCylinder(float w, float h);

  /**
   * Draws a cylinder whose bases are formed by two cutting planes ({@code m} and
   * {@code n}), along the Camera positive {@code z} axis.
   *
   * @param detail
   * @param w      radius of the cylinder and h is its height
   * @param h      height of the cylinder
   * @param m      normal of the plane that intersects the cylinder at z=0
   * @param n      normal of the plane that intersects the cylinder at z=h
   * @see #drawCylinder(float, float)
   */
  public abstract void drawHollowCylinder(int detail, float w, float h, Vec m, Vec n);

  /**
   * Draws a cone along the positive {@code z} axis, with its base centered at
   * {@code (x,y)}, height {@code h}, and radius {@code r}.
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public abstract void drawCone(int detail, float x, float y, float r, float h);

  /**
   * Draws a truncated cone along the positive {@code z} axis, with its base centered at
   * {@code (x,y)}, height {@code h} , and radii {@code r1} and {@code r2} (basis and
   * height respectively).
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public abstract void drawCone(int detail, float x, float y, float r1, float r2, float h);

  /**
   * Draws axes of length {@code length} which origin correspond to the world coordinate
   * system origin.
   *
   * @see #drawGrid(float, int)
   */
  public abstract void drawAxes(float length);

  /**
   * Draws a grid in the XY plane, centered on (0,0,0) (defined in the current coordinate
   * system).
   * <p>
   * {@code size} and {@code nbSubdivisions} define its geometry.
   *
   * @see #drawAxes(float)
   */
  public abstract void drawGrid(float size, int nbSubdivisions);

  /**
   * Draws a dotted-grid in the XY plane, centered on (0,0,0) (defined in the current
   * coordinate system).
   * <p>
   * {@code size} and {@code nbSubdivisions} define its geometry.
   *
   * @see #drawAxes(float)
   */
  public abstract void drawDottedGrid(float size, int nbSubdivisions);

  /**
   * Draws the path used to interpolate the
   * {@link KeyFrameInterpolator#frame()}
   * <p>
   * {@code mask} controls what is drawn: If ( (mask &amp; 1) != 0 ), the position path is
   * drawn. If ( (mask &amp; 2) != 0 ), a camera representation is regularly drawn and if
   * ( (mask &amp; 4) != 0 ), oriented axes are regularly drawn. Examples:
   * <p>
   * {@code drawPath(); // Simply draws the interpolation path} <br>
   * {@code drawPath(3); // Draws path and cameras} <br>
   * {@code drawPath(5); // Draws path and axes} <br>
   * <p>
   * In the case where camera or axes are drawn, {@code nbFrames} controls the number of
   * objects (axes or camera) drawn between two successive keyFrames. When
   * {@code nbFrames = 1}, only the path KeyFrames are drawn. {@code nbFrames = 2} also
   * draws the intermediate orientation, etc. The maximum value is 30. {@code nbFrames}
   * should divide 30 so that an object is drawn for each KeyFrame. Default value is 6.
   * <p>
   * {@code scale} controls the scaling of the camera and axes drawing. A value of
   * {@link #radius()} should give good results.
   */
  public abstract void drawPath(KeyFrameInterpolator kfi, int mask, int nbFrames, float scale);

  /**
   * Draws a representation of the {@code eye} in the scene.
   * <p>
   * The near and far planes are drawn as quads, the frustum is drawn using lines and the
   * camera up vector is represented by an arrow to disambiguate the drawing.
   * <p>
   * <b>Note:</b> The drawing of a Scene's own Scene.camera() should not be visible, but
   * may create artifacts due to numerical imprecisions.
   */
  public abstract void drawEye(Eye eye);

  /**
   * Internal use.
   */
  protected abstract void drawKFIEye(float scale);

  /**
   * Draws a rectangle on the screen showing the region where a zoom operation is taking
   * place.
   */
  protected abstract void drawZoomWindowHint();

  /**
   * Draws visual hint (a line on the screen) when a screen rotation is taking place.
   */
  protected abstract void drawScreenRotateHint();

  /**
   * Draws visual hint (a cross on the screen) when the
   * {@link Eye#anchor()} is being set.
   * <p>
   * Simply calls {@link #drawCross(float, float, float)} on
   * {@link Eye#projectedCoordinatesOf(Vec)}} from
   * {@link Eye#anchor()}.
   *
   * @see #drawCross(float, float, float)
   */
  protected abstract void drawAnchorHint();

  /**
   * Internal use.
   */
  protected abstract void drawPointUnderPixelHint();

  /**
   * Draws a cross on the screen centered under pixel {@code (px, py)}, and edge of size
   * {@code size}.
   *
   * @see #drawAnchorHint()
   */
  public abstract void drawCross(float px, float py, float size);

  /**
   * Draws a filled circle using screen coordinates.
   *
   * @param subdivisions Number of triangles approximating the circle.
   * @param center       Circle screen center.
   * @param radius       Circle screen radius.
   */
  public abstract void drawFilledCircle(int subdivisions, Vec center, float radius);

  /**
   * Draws a filled square using screen coordinates.
   *
   * @param center Square screen center.
   * @param edge   Square edge length.
   */
  public abstract void drawFilledSquare(Vec center, float edge);

  /**
   * Draws the classical shooter target on the screen.
   *
   * @param center Center of the target on the screen
   * @param length Length of the target in pixels
   */
  public abstract void drawShooterTarget(Vec center, float length);

  /**
   * Draws all GrabberFrames' picking targets: a shooter target visual hint of
   * {@link InteractiveFrame#grabsInputThreshold()} pixels size.
   * <p>
   * <b>Attention:</b> the target is drawn either if the iFrame is part of camera path and
   * keyFrame is {@code true}, or if the iFrame is not part of camera path and keyFrame is
   * {@code false}.
   */
  public abstract void drawPickingTarget(InteractiveFrame gFrame);

  // end wrapper

  // 0. Optimization stuff

  // public abstract long frameCount();

  // 1. Associated objects

  // AVATAR STUFF

  /**
   * Returns the avatar object to be tracked by the Camera when it is in Third Person
   * mode.
   * <p>
   * Simply returns {@code null} if no avatar has been set.
   */
  public Trackable avatar() {
    return trck;
  }

  /**
   * Sets the avatar object to be tracked by the Camera when it is in Third Person mode.
   *
   * @see #resetAvatar()
   */
  public void setAvatar(Trackable t) {
    trck = t;
    if (avatar() == null)
      return;
    eye().frame().stopSpinning();
    if (avatar() instanceof InteractiveFrame)
      ((InteractiveFrame) (avatar())).stopSpinning();

    // perform small animation ;)
    if (eye().anyInterpolationStarted())
      eye().stopInterpolations();
    // eye().interpolateTo(avatar().eyeFrame());//works only when eyeFrame
    // scaling = magnitude
    InteractiveFrame eyeFrameCopy = avatar().trackingEyeFrame().get();
    eyeFrameCopy.setMagnitude(avatar().trackingEyeFrame().scaling());
    eye().interpolateTo(eyeFrameCopy);
    pruneBranch(eyeFrameCopy);

    if (avatar() instanceof InteractiveFrame)
      inputHandler().setDefaultGrabber((InteractiveFrame) avatar());
  }

  /**
   * Returns the current avatar before resetting it (i.e., setting it to null).
   *
   * @see #setAvatar(Trackable)
   */
  public Trackable resetAvatar() {
    Trackable prev = trck;
    if (prev != null) {
      inputHandler().resetTrackedGrabber();
      inputHandler().setDefaultGrabber(eye().frame());
      eye().interpolateToFitScene();
    }
    trck = null;
    return prev;
  }

  // 3. EYE STUFF

  /**
   * Returns the associated Eye, never {@code null}. This is the high level version of
   * {@link #window()} and {@link #camera()} which holds that which is common of the two.
   * <p>
   * 2D applications should simply use {@link #window()} and 3D applications should simply
   * use {@link #camera()}. If you plan to implement two versions of the same application
   * one in 2D and the other in 3D, use this method.
   * <p>
   * <b>Note</b> that not all methods defined in the Camera class are available in the Eye
   * class and that all methods defined in the Window class are.
   */
  public Eye eye() {
    return eye;
  }

  public InteractiveFrame eyeFrame() {
    return eye.frame();
  }

  /**
   * Replaces the current {@link #eye()} with {@code vp}.
   * <p>
   * The {@link #inputHandler()} will attempt to add the {@link #eyeFrame()} to all its
   * {@link InputHandler#agents()}, such as the {@link #motionAgent()}
   * and {@link #keyAgent()}.
   */
  public void setEye(Eye vp) {
    if (vp == null)
      return;
    if (vp.scene() != this)
      return;
    if (!replaceEye(vp)) {
      eye = vp;
      inputHandler().addGrabber(eye.frame());
      inputHandler().setDefaultGrabber(eye.frame());
    }
    eye().setSceneRadius(radius());
    eye().setSceneCenter(center());
    eye().setScreenWidthAndHeight(width(), height());
    showAll();
  }

  protected boolean replaceEye(Eye vp) {
    if (vp == null || vp == eye())
      return false;
    if (eye() != null) {
      // /* option 1
      for (Agent agent : inputHandler().agents())
        if (agent.defaultGrabber() != null)
          if (agent.defaultGrabber() == eye.frame()) {
            agent.addGrabber(vp.frame());
            agent.setDefaultGrabber(vp.frame());
          }
      // inputHandler().removeGrabber(eye.frame());
      pruneBranch(eye.frame());// better than remove grabber
      // */
      // option 2
      // inputHandler().shiftDefaultGrabber(vp.frame(), eye.frame());
      // //inputHandler().removeGrabber(eye.frame());
      // pruneBranch(eye.frame());// better than remove grabber
      eye = vp;// eye() changed
      return true;
    }
    return false;
  }

  /**
   * If {@link #isLeftHanded()} calls {@link #setRightHanded()}, otherwise calls
   * {@link #setLeftHanded()}.
   */
  public void flip() {
    if (isLeftHanded())
      setRightHanded();
    else
      setLeftHanded();
  }

  /**
   * If {@link #is3D()} returns the associated Camera, never {@code null}. If
   * {@link #is2D()} throws an exception.
   *
   * @see #eye()
   */
  public Camera camera() {
    if (this.is3D())
      return (Camera) eye;
    else
      throw new RuntimeException("Camera type is only available in 3D");
  }

  /**
   * If {@link #is3D()} sets the Camera. If {@link #is2D()} throws an exception.
   *
   * @see #setEye(Eye)
   */
  public void setCamera(Camera cam) {
    if (this.is2D()) {
      System.out.println("Warning: Camera Type is only available in 3D");
    } else
      setEye(cam);
  }

  /**
   * If {@link #is2D()} returns the associated Window, never {@code null}. If
   * {@link #is3D()} throws an exception.
   *
   * @see #eye()
   */
  public Window window() {
    if (this.is2D())
      return (Window) eye;
    else
      throw new RuntimeException("Window type is only available in 2D");
  }

  /**
   * If {@link #is2D()} sets the Window. If {@link #is3D()} throws an exception.
   *
   * @see #setEye(Eye)
   */
  public void setWindow(Window win) {
    if (this.is3D()) {
      System.out.println("Warning: Window Type is only available in 2D");
    } else
      setEye(win);
  }

  /**
   * Same as {@code eye().frame().setConstraint(constraint)}.
   *
   * @see InteractiveFrame#setConstraint(Constraint)
   */
  public void setEyeConstraint(Constraint constraint) {
    eye().frame().setConstraint(constraint);
  }

  /**
   * Same as {@code return eye().pointIsVisible(point)}.
   *
   * @see Eye#isPointVisible(Vec)
   */
  public boolean isPointVisible(Vec point) {
    return eye().isPointVisible(point);
  }

  /**
   * Same as {@code return eye().ballIsVisible(center, radius)}.
   *
   * @see Eye#ballVisibility(Vec, float)
   */
  public Eye.Visibility ballVisibility(Vec center, float radius) {
    return eye().ballVisibility(center, radius);
  }

  /**
   * Same as {@code return eye().boxIsVisible(p1, p2)}.
   *
   * @see Eye#boxVisibility(Vec, Vec)
   */
  public Eye.Visibility boxVisibility(Vec p1, Vec p2) {
    return eye().boxVisibility(p1, p2);
  }

  /**
   * Returns {@code true} if automatic update of the camera frustum plane equations is
   * enabled and {@code false} otherwise. Computation of the equations is expensive and
   * hence is disabled by default.
   *
   * @see #toggleBoundaryEquations()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see Camera#updateBoundaryEquations()
   */
  public boolean areBoundaryEquationsEnabled() {
    return eye().areBoundaryEquationsEnabled();
  }

  /**
   * Toggles automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see Camera#updateBoundaryEquations()
   */
  public void toggleBoundaryEquations() {
    if (areBoundaryEquationsEnabled())
      disableBoundaryEquations();
    else
      enableBoundaryEquations();
  }

  /**
   * Disables automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #toggleBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see Camera#updateBoundaryEquations()
   */
  public void disableBoundaryEquations() {
    enableBoundaryEquations(false);
  }

  /**
   * Enables automatic update of the camera frustum plane equations every frame.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #toggleBoundaryEquations()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see Camera#updateBoundaryEquations()
   */
  public void enableBoundaryEquations() {
    enableBoundaryEquations(true);
  }

  /**
   * Enables or disables automatic update of the camera frustum plane equations every
   * frame according to {@code flag}. Computation of the equations is expensive and hence
   * is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #toggleBoundaryEquations()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations()
   * @see Camera#updateBoundaryEquations()
   */
  public void enableBoundaryEquations(boolean flag) {
    eye().enableBoundaryEquations(flag);
  }

  /**
   * Toggles the {@link #eye()} type between PERSPECTIVE and ORTHOGRAPHIC.
   */
  public void toggleCameraType() {
    if (this.is2D()) {
      AbstractScene.showDepthWarning("toggleCameraType");
      return;
    } else {
      if (((Camera) eye()).type() == Camera.Type.PERSPECTIVE)
        setCameraType(Camera.Type.ORTHOGRAPHIC);
      else
        setCameraType(Camera.Type.PERSPECTIVE);
    }
  }

  /**
   * Same as {@code return camera().isFaceBackFacing(a, b, c)}.
   * <p>
   * This method is only available in 3D.
   *
   * @see Camera#isFaceBackFacing(Vec, Vec, Vec)
   */
  public boolean isFaceBackFacing(Vec a, Vec b, Vec c) {
    if (this.is2D()) {
      AbstractScene.showDepthWarning("isFaceBackFacing");
      return false;
    }
    return camera().isFaceBackFacing(a, b, c);
  }

  /**
   * Same as {@code return camera().isConeBackFacing(vertex, normals)}.
   * <p>
   * This method is only available in 3D.
   *
   * @see Camera#isConeBackFacing(Vec, Vec[])
   */
  public boolean isConeBackFacing(Vec vertex, Vec[] normals) {
    if (this.is2D()) {
      AbstractScene.showDepthWarning("isConeBackFacing");
      return false;
    }
    return camera().isConeBackFacing(vertex, normals);
  }

  /**
   * Same as {@code return camera().isConeBackFacing(vertex, axis, angle)}.
   * <p>
   * This method is only available in 3D.
   *
   * @see Camera#isConeBackFacing(Vec, Vec, float)
   */
  public boolean isConeBackFacing(Vec vertex, Vec axis, float angle) {
    if (this.is2D()) {
      AbstractScene.showDepthWarning("isConeBackFacing");
      return false;
    }
    return camera().isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Returns the world coordinates of the 3D point located at {@code pixel} (x,y) on
   * screen. May be null if no pixel is under pixel.
   */
  public Vec pointUnderPixel(Point pixel) {
    float depth = pixelDepth(pixel);
    Vec point = unprojectedCoordinatesOf(new Vec(pixel.x(), pixel.y(), depth));
    return (depth < 1.0f) ? point : null;
  }

  /**
   * Same as {@code return pointUnderPixel(new Point(x, y))}.
   *
   * @see #pointUnderPixel(Point)
   */
  public Vec pointUnderPixel(float x, float y) {
    return pointUnderPixel(new Point(x, y));
  }

  /**
   * Returns the depth (z-value) of the object under the {@code pixel}.
   * <p>
   * The z-value ranges in [0..1] (near and far plane respectively). In 3D Note that this
   * value is not a linear interpolation between
   * {@link Camera#zNear()} and
   * {@link Camera#zFar()};
   * {@code z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z');} where {@code z'} is
   * the distance from the point you project to the camera, along the
   * {@link Camera#viewDirection()}. See the {@code gluUnProject}
   * man page for details.
   */
  public abstract float pixelDepth(Point pixel);

  public float pixelDepth(float x, float y) {
    return pixelDepth(new Point(x, y));
  }

  /**
   * Same as {@link Eye#projectedCoordinatesOf(Mat, Vec)}.
   */
  public Vec projectedCoordinatesOf(Vec src) {
    return eye().projectedCoordinatesOf(this.matrixHelper().projectionView(), src);
  }

  /**
   * If {@link MatrixHelper#isProjectionViewInverseCached()}
   * (cache version) returns
   * {@link Eye#unprojectedCoordinatesOf(Mat, Vec)} (Mat is
   * {@link MatrixHelper#projectionViewInverse()}). Otherwise
   * (non-cache version) returns
   * {@link Eye#unprojectedCoordinatesOf(Vec)}.
   */
  public Vec unprojectedCoordinatesOf(Vec src) {
    if (isUnprojectedCoordinatesOfOptimized())
      return eye().unprojectedCoordinatesOf(this.matrixHelper().projectionViewInverse(), src);
    else
      return eye().unprojectedCoordinatesOf(src);
  }

  /**
   * Returns the scene radius.
   * <p>
   * Convenience wrapper function that simply calls {@code camera().sceneRadius()}
   *
   * @see #setRadius(float)
   * @see #center()
   */
  public float radius() {
    return eye().sceneRadius();
  }

  /**
   * Returns the scene center.
   * <p>
   * Convenience wrapper function that simply returns {@code camera().sceneCenter()}
   *
   * @see #setCenter(Vec) {@link #radius()}
   */
  public Vec center() {
    return eye().sceneCenter();
  }

  /**
   * Returns the {@link Eye#anchor()}.
   * <p>
   * Convenience wrapper function that simply returns {@code eye().anchor()}
   *
   * @see #setCenter(Vec) {@link #radius()}
   */
  public Vec anchor() {
    return eye().anchor();
  }

  /**
   * Same as {@link Eye#setAnchor(Vec)}.
   */
  public void setAnchor(Vec anchor) {
    eye().setAnchor(anchor);
  }

  /**
   * Sets the {@link #radius()} of the Scene.
   * <p>
   * Convenience wrapper function that simply calls
   * {@code camera().setSceneRadius(radius)}.
   *
   * @see #setCenter(Vec)
   */
  public void setRadius(float radius) {
    eye().setSceneRadius(radius);
  }

  /**
   * Sets the {@link #center()} of the Scene.
   * <p>
   * Convenience wrapper function that simply calls {@code }
   *
   * @see #setRadius(float)
   */
  public void setCenter(Vec center) {
    eye().setSceneCenter(center);
  }

  /**
   * Sets the {@link #center()} and {@link #radius()} of the Scene from the {@code min}
   * and {@code max} vectors.
   * <p>
   * Convenience wrapper function that simply calls
   * {@code camera().setSceneBoundingBox(min,max)}
   *
   * @see #setRadius(float)
   * @see #setCenter(Vec)
   */
  public void setBoundingBox(Vec min, Vec max) {
    if (this.is2D())
      System.out.println("setBoundingBox is available only in 3D. Use setBoundingRect instead");
    else
      ((Camera) eye()).setSceneBoundingBox(min, max);
  }

  public void setBoundingRect(Vec min, Vec max) {
    if (this.is3D())
      System.out.println("setBoundingRect is available only in 2D. Use setBoundingBox instead");
    else
      ((Window) eye()).setSceneBoundingBox(min, max);
  }

  /**
   * Convenience wrapper function that simply calls {@code camera().showEntireScene()}
   *
   * @see Camera#showEntireScene()
   */
  public void showAll() {
    eye().showEntireScene();
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code eye().setAnchorFromPixel(pixel)}.
   * <p>
   * Current implementation set no {@link Eye#anchor()}. Override
   * {@link Camera#pointUnderPixel(Point)} in your openGL based
   * camera for this to work.
   *
   * @see Eye#setAnchorFromPixel(Point)
   * @see Camera#pointUnderPixel(Point)
   */
  public boolean setAnchorFromPixel(Point pixel) {
    return eye().setAnchorFromPixel(pixel);
  }

  public boolean setAnchorFromPixel(float x, float y) {
    return setAnchorFromPixel(new Point(x, y));
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code camera().setSceneCenterFromPixel(pixel)}
   * <p>
   * Current implementation set no {@link Camera#sceneCenter()}.
   * Override {@link Camera#pointUnderPixel(Point)} in your openGL
   * based camera for this to work.
   *
   * @see Camera#setSceneCenterFromPixel(Point)
   * @see Camera#pointUnderPixel(Point)
   */
  public boolean setCenterFromPixel(Point pixel) {
    return eye().setSceneCenterFromPixel(pixel);
  }

  public boolean setCenterFromPixel(float x, float y) {
    return setCenterFromPixel(new Point(x, y));
  }

  /**
   * Returns the current {@link #eye()} type.
   */
  public final Camera.Type cameraType() {
    if (this.is2D()) {
      System.out.println("Warning: Camera Type is only available in 3D");
      return null;
    } else
      return ((Camera) eye()).type();
  }

  /**
   * Sets the {@link #eye()} type.
   */
  public void setCameraType(Camera.Type type) {
    if (this.is2D()) {
      System.out.println("Warning: Camera Type is only available in 3D");
    } else if (type != ((Camera) eye()).type())
      ((Camera) eye()).setType(type);
  }

  // WARNINGS and EXCEPTIONS STUFF

  static protected HashMap<String, Object> warnings;

  /**
   * Show warning, and keep track of it so that it's only shown once.
   *
   * @param msg the error message (which will be stored for later comparison)
   */
  static public void showWarning(String msg) { // ignore
    if (warnings == null) {
      warnings = new HashMap<String, Object>();
    }
    if (!warnings.containsKey(msg)) {
      System.err.println(msg);
      warnings.put(msg, new Object());
    }
  }

  /**
   * Display a warning that the specified method is only available in 3D.
   *
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarning(String method) {
    showWarning(method + "() is not available in 2d");
  }

  /**
   * Display a warning that the specified method lacks implementation.
   */
  static public void showMissingImplementationWarning(String method, String theclass) {
    showWarning(method + "(), should be implemented by your " + theclass + " derived class.");
  }

  /**
   * Display a warning that the specified method can only be implemented from a relative
   * bogus event.
   */
  static public void showEventVariationWarning(String method) {
    showWarning(method + " can only be performed using a relative event.");
  }

  /**
   * Same as {@code showOnlyEyeWarning(method, true)}.
   *
   * @see #showOnlyEyeWarning(String, boolean)
   */
  static public void showOnlyEyeWarning(String method) {
    showOnlyEyeWarning(method, true);
  }

  /**
   * Display a warning that the specified method is only available for an eye-frame if
   * {@code eye} is {@code true} or a frame, different than an eye-frame, if {@code eye}
   * is {@code false}.
   */
  static public void showOnlyEyeWarning(String method, boolean eye) {
    if (eye)
      showWarning(method + "() is meaningful only when frame is attached to an eye.");
    else
      showWarning(method + "() is meaningful only when frame is detached from an eye.");
  }

  /**
   * Display a warning that the specified method is not available under the specified
   * platform.
   */
  static public void showPlatformVariationWarning(String themethod, Platform platform) {
    showWarning(themethod + " is not available under the " + platform + " platform.");
  }

  static public void showMinDOFsWarning(String themethod, int dofs) {
    showWarning(themethod + "() requires at least a " + dofs + " dofs.");
  }

  // NICE STUFF

  /**
   * Apply the local transformation defined by {@code frame}, i.e., respect to the frame
   * {@link Frame#referenceFrame()}. The Frame is first translated
   * and then rotated around the new translated origin.
   * <p>
   * This method may be used to modify the modelview matrix from a Frame hierarchy. For
   * example, with this Frame hierarchy:
   * <p>
   * {@code Frame body = new Frame();} <br>
   * {@code Frame leftArm = new Frame();} <br>
   * {@code Frame rightArm = new Frame();} <br>
   * {@code leftArm.setReferenceFrame(body);} <br>
   * {@code rightArm.setReferenceFrame(body);} <br>
   * <p>
   * The associated drawing code should look like:
   * <p>
   * {@code pushModelView();} <br>
   * {@code applyTransformation(body);} <br>
   * {@code drawBody();} <br>
   * {@code pushModelView();} <br>
   * {@code applyTransformation(leftArm);} <br>
   * {@code drawArm();} <br>
   * {@code popMatrix();} <br>
   * {@code pushMatrix();} <br>
   * {@code applyTransformation(rightArm);} <br>
   * {@code drawArm();} <br>
   * {@code popModelView();} <br>
   * {@code popModelView();} <br>
   * <p>
   * Note the use of nested {@link #pushModelView()} and {@link #popModelView()} blocks to
   * represent the frame hierarchy: {@code leftArm} and {@code rightArm} are both
   * correctly drawn with respect to the {@code body} coordinate system.
   * <p>
   * <b>Attention:</b> When drawing a frame hierarchy as above, this method should be used
   * whenever possible.
   *
   * @see #applyWorldTransformation(Frame)
   */
  public void applyTransformation(Frame frame) {
    if (is2D()) {
      translate(frame.translation().x(), frame.translation().y());
      rotate(frame.rotation().angle());
      scale(frame.scaling(), frame.scaling());
    } else {
      translate(frame.translation().vec[0], frame.translation().vec[1], frame.translation().vec[2]);
      rotate(frame.rotation().angle(), ((Quat) frame.rotation()).axis().vec[0], ((Quat) frame.rotation()).axis().vec[1],
          ((Quat) frame.rotation()).axis().vec[2]);
      scale(frame.scaling(), frame.scaling(), frame.scaling());
    }
  }

  /**
   * Same as {@link #applyTransformation(Frame)} but applies the global transformation
   * defined by the frame.
   */
  public void applyWorldTransformation(Frame frame) {
    Frame refFrame = frame.referenceFrame();
    if (refFrame != null) {
      applyWorldTransformation(refFrame);
      applyTransformation(frame);
    } else {
      applyTransformation(frame);
    }
  }

  /**
   * This method is called before the first drawing happen and should be overloaded to
   * initialize stuff. The default implementation is empty.
   */
  public void init() {
  }

  /**
   * The method that actually defines the scene.
   * <p>
   * If you build a class that inherits from Scene, this is the method you should
   * overload, but no if you instantiate your own Scene object (for instance, in
   * Processing you should just overload {@code PApplet.draw()} to define your scene).
   * <p>
   * The eye matrices set in {@link #bindMatrices()} converts from the world to the camera
   * coordinate systems. Thus vertices given here can then be considered as being given in
   * the world coordinate system. The eye is moved in this world using the mouse. This
   * representation is much more intuitive than a camera-centric system (which for
   * instance is the standard in OpenGL).
   */
  public void proscenium() {
  }

  // GENERAL STUFF

  /**
   * Returns true if scene is left handed. Note that the scene is right handed by default.
   * However in proscene we set it as right handed (same as with P5).
   *
   * @see #setLeftHanded()
   */
  public boolean isLeftHanded() {
    return !rightHanded;
  }

  /**
   * Returns true if scene is right handed. Note that the scene is right handed by
   * default. However in proscene we set it as right handed (same as with P5).
   *
   * @see #setRightHanded()
   */
  public boolean isRightHanded() {
    return rightHanded;
  }

  /**
   * Set the scene as right handed.
   *
   * @see #isRightHanded()
   */
  public void setRightHanded() {
    rightHanded = true;
  }

  /**
   * Set the scene as left handed.
   *
   * @see #isLeftHanded()
   */
  public void setLeftHanded() {
    rightHanded = false;
  }

  /**
   * Returns {@code true} if this Scene is associated to an off-screen renderer and
   * {@code false} otherwise.
   */
  public boolean isOffscreen() {
    return offscreen;
  }

  /**
   * @return true if the scene is 2D.
   */
  public boolean is2D() {
    return !is3D();
  }

  /**
   * @return true if the scene is 3D.
   */
  public abstract boolean is3D();

  // dimensions

  /**
   * Returns the {@link #width()} to {@link #height()} aspect ratio of the display window.
   */
  public float aspectRatio() {
    return (float) width() / (float) height();
  }

  /**
   * Returns true grid is dotted.
   */
  public boolean gridIsDotted() {
    return dottedGrid;
  }

  /**
   * Sets the drawing of the grid visual hint as dotted or not.
   */
  public void setDottedGrid(boolean dotted) {
    dottedGrid = dotted;
  }

  // ABSTRACT STUFF

  /**
   * @return width of the screen window.
   */
  public abstract int width();

  /**
   * @return height of the screen window.
   */
  public abstract int height();

  /**
   * Disables z-buffer.
   */
  public abstract void disableDepthTest();

  /**
   * Enables z-buffer.
   */
  public abstract void enableDepthTest();

  //TODO: high-level ik api handling

  /**
   * Return registered solvers
   * */
  public List<Solver.TreeSolver> solvers() {
    return solvers;
  }

  /**
   * Registers the given chain with the given name
   * to solve IK.
   */
  public Solver.TreeSolver setIKStructure(InteractiveFrame branchRoot) {
    for(Solver.TreeSolver solver : solvers) {
      //If Head is Contained in any structure do nothing
      if(!branch(solver.getHead(), branchRoot, true).isEmpty())
        return null;
    }
    Solver.TreeSolver solver = new Solver.TreeSolver(branchRoot);
    solvers.add(solver);
    //Add task
    registerTimingTask(solver.getExecutionTask());
    solver.getExecutionTask().run(1);
    return solver;
  }

  /**
   * Unregisters the IK Solver with the given Frame as branchRoot
   */
  public boolean resetIKStructure(InteractiveFrame branchRoot) {
    Solver.TreeSolver toRemove = null;
    for(Solver.TreeSolver solver: solvers) {
      if (solver.getHead().id() == branchRoot.id()) {
        toRemove = solver;
        break;
      }
    }
    //Remove task
    unregisterTimingTask(toRemove.getExecutionTask());
    return solvers.remove(toRemove);
  }

  /**
   * Gets the IK Solver with the given name
   */
  public Solver.TreeSolver getSolver(InteractiveFrame branchRoot){
    for(Solver.TreeSolver solver: solvers) {
      if (solver.getHead().id() == branchRoot.id()) {
        return solver;
      }
    }
    return null;
  }

  public boolean addIKTarget(InteractiveFrame endEffector, Frame target){
    for(Solver.TreeSolver solver: solvers) {
      if(solver.addTarget(endEffector, target)) return true;
    }
    return false;
  }

  /**
   * Execute IK Task for a IK Solver that is not registered
   */
  public void executeIKSolver(Solver solver){
    executeIKSolver(solver, 1);
  }

  public void executeIKSolver(Solver solver, long period){
    registerTimingTask(solver.getExecutionTask());
    solver.getExecutionTask().run(period);
  }

}
