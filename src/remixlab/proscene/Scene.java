/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import remixlab.fpstiming.TimingTask;
import remixlab.geom.Graph;
import remixlab.geom.Node;
import remixlab.geom.Interpolator;
import remixlab.geom.MatrixHandler;
import remixlab.primitives.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * A 2D or 3D interactive, on-screen or off-screen, Processing Scene. The Scene is a specialization of the
 * {@link Graph}, providing an interface between Dandelion
 * and Processing.
 * <p>
 * <h3>Usage</h3> To use a Scene you have two main choices:
 * <ol>
 * <li><b>Direct instantiation</b>. In this case you should instantiate your own Scene
 * object at the {@code PApplet.setup()} function. See the example <i>BasicUse</i>.
 * <li><b>Inheritance</b>. In this case, once you declare a Scene derived class, you
 * should implement {@link #proscenium()} which defines the objects in your graph. Just
 * make sure to define the {@code PApplet.draw()} method, even if it's empty. See the
 * example <i>AlternativeUse</i>.
 * </ol>
 * <h3>Interactivity mechanisms</h3> ProScene provides powerful interactivity mechanisms
 * allowing a wide range of graph setups ranging from very simple to complex ones. For
 * convenience, two interaction mechanisms are provided by default:
 * {@link #keyAgent()}, and {@link #motionAgent()} (which in the desktop version of
 * proscene defaults to a {@link #mouseAgent()}):
 * <ol>
 * <li><b>The default key agent</b> provides shortcuts to
 * node and graph key actions (such as
 * {@link #drawGrid()} or {@link #drawAxes()}). See {@link #keyAgent()}.
 * <li><b>The default mouse agent</b> provides high-level methods to manage the
 * eye and node
 * motion actions. Please refer to the {@link remixlab.proscene.MouseAgent} and
 * {@link remixlab.proscene.KeyAgent} API's.
 * </ol>
 */
public class Scene extends Graph implements PConstants {
  // Timing
  protected boolean javaTiming;
  public static final String prettyVersion = "3.0.0";
  public static final String version = "32";

  // P R O C E S S I N G A P P L E T A N D O B J E C T S
  protected PApplet parent;
  protected PGraphics mainPGraphics;

  // E X C E P T I O N H A N D L I N G
  protected int beginOffScreenDrawingCalls;

  // off-screen scenes:
  protected static Scene lastScene;
  protected long lastDisplay;
  protected boolean autofocus;
  // just to make it compatible with previous versions of proscene
  protected static int offScreenScenes;

  // offscreen
  protected Point upperLeftCorner;
  protected boolean offscreen;

  protected boolean dottedGrid;

  // CONSTRUCTORS

  /**
   * Constructor that defines an on-screen Processing Scene. Same as {@code this(p, p.g}.
   *
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, PGraphics, int, int)
   */
  public Scene(PApplet p) {
    this(p, p.g);
  }

  /**
   * Same as {@code this(p, renderer, 0, 0)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics, int, int)
   */
  public Scene(PApplet p, PGraphics renderer) {
    this(p, renderer, 0, 0);
  }

  /**
   * Main constructor defining a left-handed Processing compatible Scene. Calls
   * {@link #setMatrixHandler(MatrixHandler)} using a customized
   * {@link MatrixHandler} depending on the {@code pg} type (see
   * {@link Java2DMatrixHandler} and
   * {@link GLMatrixHandler}). The constructor instantiates the
   * {@link #inputHandler()} and the {@link #timingHandler()}, sets the AXIS and GRID
   * visual hint flags, instantiates the {@link #eye()}. It also
   * instantiates the {@link #keyAgent()} and the {@link #mouseAgent()}, and finally
   * calls {@link #init()}.
   * <p>
   * An off-screen Processing Scene is defined if {@code pg != p.g}. In this case the
   * {@code x} and {@code y} parameters define the position of the upper-left corner where
   * the off-screen Scene is expected to be displayed, e.g., for instance with a call to
   * Processing the {@code image(img, x, y)} function. If {@code pg == p.g}) (which
   * defines an on-screen Scene, see also {@link #isOffscreen()}), the values of x and y
   * are meaningless (both are set to 0 to be taken as dummy values). Render into an
   * off-screen graph requires the drawing code to be enclose by {@link #beginDraw()} and
   * {@link #endDraw()}. To display an off-screen graph call {@link #display()}.
   *
   * @see Graph#Graph(int, int)
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics)
   */
  public Scene(PApplet p, PGraphics pg, int x, int y) {
    super(pg.width, pg.height);
    // 1. P5 objects
    parent = p;
    mainPGraphics = pg;
    offscreen = pg != p.g;
    upperLeftCorner = offscreen ? new Point(x, y) : new Point(0, 0);

    // 2. Matrix helper
    setMatrixHandler(matrixHelper(pg));

    // 3. Create agents and register P5 methods
    defMotionAgent = new MouseAgent(this);
    defKeyboardAgent = new KeyAgent(this);
    parent.registerMethod("mouseEvent", motionAgent());
    parent.registerMethod("keyEvent", keyAgent());

    // this.setDefaultKeyBindings();
    if (!isOffscreen()) {
      pApplet().registerMethod("pre", this);
      pApplet().registerMethod("draw", this);
    } else {
      offScreenScenes++;
      enableAutoFocus();
    }
    // TODO buggy
    pApplet().registerMethod("dispose", this);

    // 5. Handed
    setLeftHanded();

    // 6. Misc stuff:
    setDottedGrid(is2D());
    //this.setNonSeqTimers();
    // pApplet().frameRate(100);

    // 7. Init should be called only once
    init();
  }

  @Override
  public boolean is3D() {
    return (mainPGraphics instanceof PGraphics3D);
  }

  /**
   * Returns the upper left corner of the Scene window. It's always (0,0) for on-screen
   * scenes, but off-screen scenes may be defined elsewhere on a canvas.
   */
  public Point originCorner() {
    return upperLeftCorner;
  }

  // P5 STUFF

  /**
   * Returns the PApplet instance this Scene is related to.
   */
  public PApplet pApplet() {
    return parent;
  }

  /**
   * Returns the PGraphics instance this Scene is related to. It may be the PApplets one,
   * if the Scene is on-screen or an user-defined if the Scene {@link #isOffscreen()}.
   */
  public PGraphics pg() {
    return mainPGraphics;
  }

  // DEFAULT MOTION-AGENT

  /**
   * Enables Proscene mouse handling through the {@link #mouseAgent()}.
   *
   * @see #isMotionAgentEnabled()
   * @see #disableMotionAgent()
   * @see #enableKeyAgent()
   */
  @Override
  public void enableMotionAgent() {
    enableMouseAgent();
  }

  /**
   * Disables the default mouse agent and returns it.
   *
   * @see #isMotionAgentEnabled()
   * @see #enableMotionAgent()
   * @see #enableKeyAgent()
   * @see #disableKeyAgent()
   */
  @Override
  public boolean disableMotionAgent() {
    return disableMouseAgent();
  }

  // KEYBOARD

  // Mouse

  /**
   * Returns the default mouse agent handling Processing mouse events. If you plan to
   * customize your mouse use this method.
   *
   * @see #enableMouseAgent()
   * @see #isMouseAgentEnabled()
   * @see #disableMouseAgent()
   * @see #keyAgent()
   */
  public MouseAgent mouseAgent() {
    return (MouseAgent) motionAgent();
  }

  /**
   * Enables motion handling through the {@link #mouseAgent()}.
   *
   * @see #mouseAgent()
   * @see #isMouseAgentEnabled()
   * @see #disableMouseAgent()
   * @see #enableKeyAgent()
   */
  public void enableMouseAgent() {
    if (!isMotionAgentEnabled()) {
      inputHandler().registerAgent(motionAgent());
      parent.registerMethod("mouseEvent", motionAgent());
    }
  }

  /**
   * Disables the default mouse agent and returns it.
   *
   * @see #mouseAgent()
   * @see #isMouseAgentEnabled()
   * @see #enableMouseAgent()
   * @see #disableKeyAgent()
   */
  public boolean disableMouseAgent() {
    if (isMotionAgentEnabled()) {
      parent.unregisterMethod("mouseEvent", motionAgent());
      return inputHandler().unregisterAgent(motionAgent());
    }
    return false;
  }

  /**
   * Returns {@code true} if the {@link #mouseAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #mouseAgent()
   * @see #enableMouseAgent()
   * @see #disableMouseAgent()
   * @see #enableKeyAgent()
   */
  public boolean isMouseAgentEnabled() {
    return isMotionAgentEnabled();
  }

  // keyAgent

  /**
   * Enables key handling through the {@link #keyAgent()}.
   *
   * @see #keyAgent()
   * @see #isKeyAgentEnabled()
   * @see #disableKeyAgent()
   * @see #enableMouseAgent()
   */
  @Override
  public void enableKeyAgent() {
    if (!isKeyAgentEnabled()) {
      inputHandler().registerAgent(keyAgent());
      parent.registerMethod("keyEvent", keyAgent());
    }
  }

  /**
   * Disables the key agent and returns it.
   *
   * @see #keyAgent()
   * @see #isKeyAgentEnabled()
   * @see #enableKeyAgent()
   * @see #disableMouseAgent()
   */
  @Override
  public boolean disableKeyAgent() {
    if (inputHandler().isAgentRegistered(keyAgent())) {
      parent.unregisterMethod("keyEvent", keyAgent());
      return inputHandler().unregisterAgent(keyAgent());
    }
    return false;
  }

  /**
   * Returns {@code true} if the {@link #keyAgent()} is enabled and {@code false}
   * otherwise.
   *
   * @see #keyAgent()
   * @see #enableKeyAgent()
   * @see #disableKeyAgent()
   * @see #enableKeyAgent()
   */
  @Override
  public boolean isKeyAgentEnabled() {
    return isKeyAgentEnabled();
  }

  // OPENGL

  public boolean setCenterFromPixel(float x, float y) {
    return setCenterFromPixel(new Point(x, y));
  }

  /**
   * The {@link #center()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #center()} is set).
   */
  public boolean setCenterFromPixel(Point pixel) {
    Vector pup = pointUnderPixel(pixel);
    if (pup != null) {
      setCenter(pup);
      return true;
    }
    return false;
  }

  public float pixelDepth(Point pixel) {
    PGraphicsOpenGL pggl;
    if (pg() instanceof PGraphicsOpenGL)
      pggl = (PGraphicsOpenGL) pg();
    else
      throw new RuntimeException("pg() is not instance of PGraphicsOpenGL");
    float[] depth = new float[1];
    PGL pgl = pggl.beginPGL();
    pgl.readPixels(pixel.x(), (height() - pixel.y()), 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT,
        FloatBuffer.wrap(depth));
    pggl.endPGL();
    return depth[0];
  }

  /**
   * Returns the world coordinates of the 3D point located at {@code pixel} (x,y) on
   * screen. May be null if no pixel is under pixel.
   */
  public Vector pointUnderPixel(Point pixel) {
    float depth = pixelDepth(pixel);
    Vector point = unprojectedCoordinatesOf(new Vector(pixel.x(), pixel.y(), depth));
    return (depth < 1.0f) ? point : null;
  }

  /**
   * Same as {@code return pointUnderPixel(new Point(x, y))}.
   *
   * @see #pointUnderPixel(Point)
   */
  public Vector pointUnderPixel(float x, float y) {
    return pointUnderPixel(new Point(x, y));
  }

  /**
   * Returns the depth (z-value) of the object under the {@code pixel}.
   * <p>
   * The z-value ranges in [0..1] (near and far plane respectively). In 3D Note that this
   * value is not a linear interpolation between
   * {@link #zNear()} and
   * {@link #zFar()};
   * {@code z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z');} where {@code z'} is
   * the distance from the point you project to the camera, along the
   * {@link #viewDirection()}. See the {@code gluUnProject}
   * man page for details.
   */
  public float pixelDepth(float x, float y) {
    return pixelDepth(new Point(x, y));
  }

  /**
   * Disables z-buffer.
   */
  public void disableDepthTest() {
    disableDepthTest(pg());
  }

  /**
   * Disables depth test on the PGraphics instance.
   *
   * @see #enableDepthTest(PGraphics)
   */
  public void disableDepthTest(PGraphics p) {
    p.hint(PApplet.DISABLE_DEPTH_TEST);
  }

  /**
   * Enables z-buffer.
   */
  public void enableDepthTest() {
    enableDepthTest(pg());
  }

  /**
   * Enables depth test on the PGraphics instance.
   *
   * @see #disableDepthTest(PGraphics)
   */
  public void enableDepthTest(PGraphics p) {
    p.hint(PApplet.ENABLE_DEPTH_TEST);
  }

  // TIMING

  @Override
  public void registerTimingTask(TimingTask task) {
    if (areTimersSeq())
      timingHandler().registerTask(task);
    else
      timingHandler().registerTask(task, new NonSeqTimer(this, task));
  }

  /**
   * Sets all {@link #timingHandler()} timers as (single-threaded)
   * {@link remixlab.fpstiming.SeqTimer}(s).
   *
   * @see #setNonSeqTimers()
   * @see #shiftTimers()
   * @see #areTimersSeq()
   */
  public void setSeqTimers() {
    if (areTimersSeq())
      return;

    javaTiming = false;
    timingHandler().restoreTimers();
  }

  /**
   * Sets all {@link #timingHandler()} timers as (multi-threaded) java.util.Timer(s).
   *
   * @see #setSeqTimers()
   * @see #shiftTimers()
   * @see #areTimersSeq()
   */
  public void setNonSeqTimers() {
    if (!areTimersSeq())
      return;

    boolean isActive;

    for (TimingTask task : timingHandler().timerPool()) {
      long period = 0;
      boolean rOnce = false;
      isActive = task.isActive();
      if (isActive) {
        period = task.period();
        rOnce = task.timer().isSingleShot();
      }
      task.stop();
      task.setTimer(new NonSeqTimer(this, task));
      if (isActive) {
        if (rOnce)
          task.runOnce(period);
        else
          task.run(period);
      }
    }

    javaTiming = true;
    PApplet.println("java util timers set");
  }

  /**
   * @return true, if timing is handling sequentially (i.e., all {@link #timingHandler()}
   * timers are (single-threaded) {@link remixlab.fpstiming.SeqTimer}(s)).
   * @see #setSeqTimers()
   * @see #setNonSeqTimers()
   * @see #shiftTimers()
   */
  public boolean areTimersSeq() {
    return !javaTiming;
  }

  /**
   * If {@link #areTimersSeq()} calls {@link #setNonSeqTimers()}, otherwise call
   * {@link #setSeqTimers()}.
   */
  public void shiftTimers() {
    if (areTimersSeq())
      setNonSeqTimers();
    else
      setSeqTimers();
  }

  // 3. Drawing methods

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

  @Override
  protected void drawGridHint() {
    pg().pushStyle();
    pg().stroke(170);
    if (gridIsDotted()) {
      pg().strokeWeight(2);
      drawDottedGrid(radius());
    } else {
      pg().strokeWeight(1);
      drawGrid(radius());
    }
    pg().popStyle();
  }

  /*
   * Copy paste from Graph but we addGrabber the style (color, stroke, etc) here.
   */
  @Override
  protected void drawAxesHint() {
    pg().pushStyle();
    pg().strokeWeight(2);
    drawAxes(radius());
    pg().popStyle();
  }

  /*
   * Copy paste from Graph but we addGrabber the style (color, stroke, etc) here.
   */
  @Override
  protected void drawPickingHint() {
    pg().pushStyle();
    pg().colorMode(PApplet.RGB, 255);
    pg().strokeWeight(1);
    pg().stroke(220, 220, 220);
    drawPickingTargets();
    pg().popStyle();
  }

  protected void drawPickingTargets() {
    for (Node node : nodes(false))
      // if(inputHandler().hasGrabber(frame))
      if (node.isVisualHintEnabled())
        drawPickingTarget(node);
  }

  /*
  @Override
  protected void drawAnchorHint() {
    pg().pushStyle();
    Vector p = eye().projectedCoordinatesOf(anchor());
    pg().stroke(255);
    pg().strokeWeight(3);
    drawCross(p.vec[0], p.vec[1]);
    pg().popStyle();
  }
  */

  /**
   * Convenience function that simply calls:
   * {@code return setAnchorFromPixel(new Point(x, y))}
   *
   * @see #setAnchorFromPixel(Point)
   */
  public boolean setAnchorFromPixel(float x, float y) {
    return setAnchorFromPixel(new Point(x, y));
  }

  /**
   * The {@link #anchor()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #anchor()} is set).
   */
  public boolean setAnchorFromPixel(Point pixel) {
    Vector pup = pointUnderPixel(pixel);
    if (pup != null) {
      setAnchor(pup);
      // new animation
      //TODO restore
      //anchorFlag = true;
      //timerFx.runOnce(1000);
      return true;
    }
    return false;
  }

  /**
   * Internal use.
   */
  //TODO restore
  //protected abstract void drawPointUnderPixelHint();

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link MatrixHandler#bind()}</li>
   * <li>Calls {@link #updateBoundaryEquations()} if
   * {@link #areBoundaryEquationsEnabled()}</li>
   * <li>Calls {@link #proscenium()}</li>
   * </ol>
   * <p>
   * <b>Note</b> that this method overloads
   * {@link Graph#preDraw()} where a call to
   * {@link #displayVisualHints()} is done. Here, however, it needs to be bypassed for the
   * PApplet.background() method not to hide the display of the {@link #visualHints()}.
   * The {@link #displayVisualHints()} mostly happens then at the {@link #draw()} method,
   * if the graph is on-screen, or at the {@link #endDraw()} if it is off-screen.
   *
   * @see #postDraw()
   */
  @Override
  public void preDraw() {
    // 1. Eye, raster graph
    matrixHandler().bind();
    if (areBoundaryEquationsEnabled()) {
      if(eye() instanceof Node) {
        if(( ((Node)eye()).lastUpdate() > lastEqUpdate || lastEqUpdate == 0)) {
          updateBoundaryEquations();
          lastEqUpdate = frameCount;
        }
      }
      else {
        updateBoundaryEquations();
        lastEqUpdate = frameCount;
      }
    }
    //TODO really needs checking. Previously we went like this:
    /*
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > lastEqUpdate || lastEqUpdate == 0)) {
      updateBoundaryEquations();
      lastEqUpdate = frameCount;
    }
    */
  }

  /**
   * Paint method which is called just before your {@code PApplet.draw()} method. Simply
   * calls {@link #preDraw()}. This method is registered at the PApplet and hence you
   * don't need to call it. Only meaningful if the graph is on-screen (it the graph
   * {@link #isOffscreen()} it even doesn't get registered at the PApplet.
   * <p>
   * If {@link #pg()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls
   * {@link #setWidth(int)} and {@link #setHeight(int)}.
   * <p>
   *
   * @see #draw()
   * @see #preDraw()
   * @see #postDraw()
   * @see #beginDraw()
   * @see #endDraw()
   * @see #isOffscreen()
   */
  public void pre() {
    if ((width() != pg().width) || (height() != pg().height)) {
      setWidth(pg().width);
      setHeight(pg().height);
    }
    preDraw();
    pushModelView();
  }

  /**
   * Paint method which is called just after your {@code PApplet.draw()} method. Calls
   * {@link #proscenium()}, {@link #displayVisualHints()} and {@link #postDraw()}. This method is
   * registered at the PApplet and hence you don't need to call it. Only meaningful if the graph is
   * on-screen (it the graph {@link #isOffscreen()} it even doesn't get registered at the
   * PApplet.
   * <p>
   * If {@link #isOffscreen()} does nothing.
   *
   * @see #pre()
   * @see #preDraw()
   * @see #postDraw()
   * @see #beginDraw()
   * @see #endDraw()
   * @see #isOffscreen()
   */
  public void draw() {
    proscenium();
    popModelView();
    displayVisualHints();
    postDraw();
  }

  // Off-screen

  /**
   * Returns {@code true} if this Scene is associated to an off-screen renderer and
   * {@code false} otherwise.
   */
  public boolean isOffscreen() {
    return offscreen;
  }

  /**
   * Same as {@code showOnlyOffScreenWarning(method, true)}.
   *
   * @see #showOnlyOffScreenWarning(String, boolean)
   */
  static public void showOnlyOffScreenWarning(String method) {
    showOnlyOffScreenWarning(method, true);
  }

  /**
   * Display a warning that the specified method is only available for off-screen scenes
   * if {@code offscreen} is {@code true}, or (on-screen scenes if if {@code offscreen} is
   * {@code false}).
   */
  static public void showOnlyOffScreenWarning(String method, boolean offscreen) {
    if (offscreen)
      showWarning(method + "() is only meaningful for offscreen scenes.");
    else
      showWarning(method + "() is only meaningful for onscreen scenes.");
  }

  /**
   * Only if the Scene {@link #isOffscreen()}. Calls {@code pg().beginDraw()} (hence
   * there's no need to explicitly call it) and then {@link #preDraw()} .
   * <p>
   * If {@link #pg()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls
   * {@link #setWidth(int)} and {@link #setHeight(int)}.
   *
   * @see #draw()
   * @see #preDraw()
   * @see #postDraw()
   * @see #pre()
   * @see #endDraw()
   * @see #isOffscreen()
   * @see #pg()
   */
  public void beginDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "begin(/end)Draw() should be used only within offscreen scenes. Check your implementation!");
    if (beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    beginOffScreenDrawingCalls++;
    if ((width != pg().width) || (height != pg().height)) {
      width = pg().width;
      height = pg().height;
      setWidth(width);
      setHeight(height);
    }
    // open off-screen pgraphics for drawing:
    pg().beginDraw();
    preDraw();
    pushModelView();
  }

  /**
   * Only if the Scene {@link #isOffscreen()}. Calls
   * <p>
   * <ol>
   * <li>{@link #proscenium()}</li>
   * <li>{@link #displayVisualHints()}</li>
   * <li>{@code pg().endDraw()} and hence there's no need to explicitly call it</li>
   * <li>{@link #handleFocus()} if {@link #hasAutoFocus()} is {@code true}</li>
   * <li>{@link #postDraw()}</li>
   * </ol>
   * <p>
   * {@link #postDraw()}.
   *
   * @see #draw()
   * @see #preDraw()
   * @see #postDraw()
   * @see #beginDraw()
   * @see #pre()
   * @see #isOffscreen()
   * @see #pg()
   */
  public void endDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "(begin/)endDraw() should be used only within offscreen scenes. Check your implementation!");
    beginOffScreenDrawingCalls--;
    if (beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    proscenium();
    popModelView();
    displayVisualHints();
    pg().endDraw();
    if (hasAutoFocus())
      handleFocus();
    postDraw();
  }

  /**
   * Same as {@code display(pg())}. Only meaningful if the graph {@link #isOffscreen()}.
   *
   * @see #display(PGraphics)
   * @see #pg()
   */
  public void display() {
    display(pg());
  }

  /**
   * Same as {@code pApplet().image(pgraphics, originCorner().x(), originCorner().y())}.
   * Only meaningful if the graph {@link #isOffscreen()}.
   */
  public void display(PGraphics pgraphics) {
    if (!isOffscreen())
      showOnlyOffScreenWarning("display");
    pApplet().image(pgraphics, originCorner().x(), originCorner().y());
    lastDisplay = pApplet().frameCount;
  }

  /**
   * Implementation of the "Focus follows mouse" policy. Used by {@link #hasFocus()}.
   */
  protected boolean hasMouseFocus() {
    return originCorner().x() < pApplet().mouseX && pApplet().mouseX < originCorner().x() + this.width()
        && originCorner().y() < pApplet().mouseY && pApplet().mouseY < originCorner().y() + this.height();
  }

  /**
   * Main condition evaluated by the {@link #handleFocus()} algorithm, which defaults to
   * {@link #hasMouseFocus()}.
   * <p>
   * Override this method to define a focus policy different than "focus follows mouse".
   */
  protected boolean hasFocus() {
    return hasMouseFocus();
  }

  /**
   * Macro used by {@link #handleFocus()}.
   */
  protected boolean displayed() {
    return lastDisplay == pApplet().frameCount - 1;
  }

  /**
   * Called by {@link #endDraw()} if {@link #hasAutoFocus()} is {@code true}.
   */
  protected void handleFocus() {
    if (offScreenScenes < 2)
      return;
    // Handling focus of non-overlapping scenes is trivial.
    // Suppose scn1 and scn2 overlap and also that scn2 is displayed on top of scn1, i.e.,
    // scn2.display() is to be called after scn1.display() (which is the key observation).
    // Then, for a given frame either only scn1 hasFocus() (which is handled trivially);
    // or, both, scn1 and scn2 hasFocus(), which means only scn2 should retain focus
    // (while scn1 lose it).
    boolean available = true;
    if (lastScene != null)
      if (lastScene != this)
        // Note that lastScene.displayed() returns true only if the lastScene was assigned
        // in the previous frame and false otherwise (particularly, if it was assigned in
        // the current frame) which means both: 1. If scn1 gained focus on the current
        // frame it will lose it when the routine is run on scn2 in the current frame;
        // and, 2. If scn2 hasGrabber gained focus in the previous frame, it will prevent scn1
        // from having it back in the current frame.
        if (lastScene.hasFocus() && lastScene.displayed())
          available = false;
    if (hasFocus() && displayed() && available) {
      enableMotionAgent();
      enableKeyAgent();
      lastScene = this;
    } else {
      disableMotionAgent();
      disableKeyAgent();
    }
  }

  /**
   * When having multiple off-screen scenes displayed at once, one should decide which
   * graph will grab inputGrabber from both, the {@link #motionAgent()} and the
   * {@link #keyAgent()}, so that code like this:
   * <p>
   * <pre>
   * {@code
   * scene1.beginDraw();
   * drawScene1();
   * graph.endDraw();
   * graph.display();
   * scene2.beginDraw();
   * drawScene2();
   * scene2.endDraw();
   * scene2.display();
   * }
   * </pre>
   * <p>
   * will behave according to a given focus policy. This property is enabled by default
   * and it implements a "focus follows mouse" policy, so that the graph under the cursor
   * will grab inputGrabber. If multiple scenes overlaps the graph on top will grab the inputGrabber as
   * expected.
   * <p>
   * To implement a different policy either:
   * <p>
   * <ol>
   * <li>Override the {@link #hasFocus()} Scene object; or,</li>
   * <li>Call {@link #disableAutoFocus()} and implement your own focus policy at the
   * sketch space.</li>
   * </ol>
   * <p>
   * <b>Note</b> that for this policy to work you should call {@link #display()} instead
   * of the papplet image() function on the {@link #pg()}.
   *
   * @see #beginDraw()
   * @see #endDraw()
   * @see #display()
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public boolean hasAutoFocus() {
    if (!isOffscreen())
      showOnlyOffScreenWarning("hasAutoFocus");
    return autofocus;
  }

  /**
   * Toggles the off-screen graph auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   */
  public void toggleAutoFocus() {
    if (!isOffscreen()) {
      showOnlyOffScreenWarning("toggleAutoFocus");
      return;
    }
    if (hasAutoFocus())
      disableAutoFocus();
    else
      enableAutoFocus();
  }

  /**
   * Disables the off-screen graph auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #enableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void disableAutoFocus() {
    if (!isOffscreen()) {
      showOnlyOffScreenWarning("disableAutoFocus");
      return;
    }
    enableAutoFocus(false);
  }

  /**
   * Enables the off-screen graph auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void enableAutoFocus() {
    if (!isOffscreen()) {
      showOnlyOffScreenWarning("enableAutoFocus");
      return;
    }
    enableAutoFocus(true);
  }

  /**
   * Turns on or off the off-screen graph auto-focus property according to {@code flag}.
   * <p>
   * The {@link #hasAutoFocus()} property for off-screen scenes is {@code true} by
   * default.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void enableAutoFocus(boolean flag) {
    if (!isOffscreen()) {
      showOnlyOffScreenWarning("enableAutoFocus");
      return;
    }
    autofocus = flag;
  }

  // TODO: Future work should include the eye and graph profiles.
  // Probably related with iFrame.fromFrame

  /**
   * Same as {@link #saveConfig()}.
   * <p>
   * Should be called automatically by P5, but it is currently broken. See:
   * https://github.com/processing/processing/issues/4445
   *
   * @see #saveConfig()
   * @see #saveConfig(String)
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void dispose() {
    System.out.println("Debug: saveConfig() (i.e., dispose()) called!");
    if (!this.isOffscreen())
      saveConfig();
  }

  /**
   * Same as {@code saveConfig("data/config.json")}.
   * <p>
   * Note that off-screen scenes require {@link #saveConfig(String)} instead.
   *
   * @see #saveConfig(String)
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig() {
    if (this.isOffscreen())
      System.out
          .println("Warning: no config saved! Off-screen graph config requires saveConfig(String fileName) to be called");
    else
      saveConfig("data/config.json");
  }

  /**
   * Saves the {@link #eye()}, the {@link #radius()}, the {@link #visualHints()}, the
   * {@link #type()} and the
   * keyFrameInterpolators into
   * {@code fileName}.
   *
   * @see #saveConfig()
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig(String fileName) {
    JSONObject json = new JSONObject();
    json.setFloat("radius", radius());
    json.setInt("visualHints", visualHints());
    json.setBoolean("ortho", is2D() ? true : type() == Type.ORTHOGRAPHIC ? true : false);
    json.setJSONObject("eye", toJSONObject(eye()));
    JSONArray jsonPaths = new JSONArray();
    //TODO restore
    // keyFrames
    /*
    int i = 0;
    for (int id : eye().keyFrameInterpolatorMap().keySet()) {
      JSONObject jsonPath = new JSONObject();
      jsonPath.setInt("key", id);
      jsonPath.setJSONArray("keyFrames", toJSONArray(id));
      jsonPaths.setJSONObject(i++, jsonPath);
    }
    json.setJSONArray("paths", jsonPaths);
    //*/
    pApplet().saveJSONObject(json, fileName);
  }

  /**
   * Same as {@code loadConfig("data/config.json")}.
   * <p>
   * Note that off-screen scenes require {@link #loadConfig(String)} instead.
   *
   * @see #loadConfig(String)
   * @see #saveConfig()
   * @see #saveConfig(String)
   */
  public void loadConfig() {
    if (this.isOffscreen())
      System.out
          .println("Warning: no config loaded! Off-screen graph config requires loadConfig(String fileName) to be called");
    else
      loadConfig("config.json");
  }

  /**
   * Loads the {@link #eye()}, the {@link #radius()}, the {@link #visualHints()}, the
   * {@link #type()} and the
   * keyFrameInterpolators from
   * {@code fileName}.
   *
   * @see #saveConfig()
   * @see #saveConfig(String)
   * @see #loadConfig()
   */
  public void loadConfig(String fileName) {
    JSONObject json = null;
    try {
      json = pApplet().loadJSONObject(fileName);
    } catch (Exception e) {
      System.out.println("No such " + fileName + " found!");
    }
    if (json != null) {
      setRadius(json.getFloat("radius"));
      setVisualHints(json.getInt("visualHints"));
      if (is3D())
        setType(json.getBoolean("ortho") ? Type.ORTHOGRAPHIC : Type.PERSPECTIVE);
      eye().setWorldMatrix(toFrame(json.getJSONObject("eye")));
      // keyFrames
      //TODO restore
      /*
      JSONArray paths = json.getJSONArray("paths");
      for (int i = 0; i < paths.size(); i++) {
        JSONObject path = paths.getJSONObject(i);
        int id = path.getInt("key");
        eye().deletePath(id);
        JSONArray keyFrames = path.getJSONArray("keyFrames");
        for (int j = 0; j < keyFrames.size(); j++) {
          Node keyFrame = new Node(this);
          pruneBranch(keyFrame);
          keyFrame.setWorldMatrix(toFrame(keyFrames.getJSONObject(j)));
          keyFrame.setPickingPrecision(Node.PickingPrecision.FIXED);
          keyFrame.setGrabsInputThreshold(Graph.platform() == Platform.PROCESSING_ANDROID ? 50 : 20);
          if (pathsVisualHint())
            inputHandler().addGrabber(keyFrame);
          if (!eye().keyFrameInterpolatorMap().containsKey(id))
            eye().setKeyFrameInterpolator(id, new Interpolator(this, eyeFrame()));
          eye().keyFrameInterpolator(id).addKeyFrame(keyFrame, keyFrames.getJSONObject(j).getFloat("time"));
        }
      }
      //*/
    }
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts the {@code id} eye path into
   * a P5 JSONArray.
   */
  /*
  protected JSONArray toJSONArray(int id) {
    JSONArray jsonKeyFrames = new JSONArray();
    for (int i = 0; i < eye().keyFrameInterpolator(id).numberOfKeyFrames(); i++) {
      JSONObject jsonKeyFrame = toJSONObject(eye().keyFrameInterpolator(id).keyFrame(i));
      jsonKeyFrame.setFloat("time", eye().keyFrameInterpolator(id).keyFrameTime(i));
      jsonKeyFrames.setJSONObject(i, jsonKeyFrame);
    }
    return jsonKeyFrames;
  }
  */

  /**
   * Used internally by {@link #loadConfig(String)}. Converts the P5 JSONObject into a
   * {@code frame}.
   */
  protected Frame toFrame(JSONObject jsonFrame) {
    Frame frame = new Frame();
    float x, y, z;
    x = jsonFrame.getJSONArray("position").getFloat(0);
    y = jsonFrame.getJSONArray("position").getFloat(1);
    z = jsonFrame.getJSONArray("position").getFloat(2);
    Vector pos = new Vector(x, y, z);
    frame.setPosition(pos);
    x = jsonFrame.getJSONArray("orientation").getFloat(0);
    y = jsonFrame.getJSONArray("orientation").getFloat(1);
    z = jsonFrame.getJSONArray("orientation").getFloat(2);
    float w = jsonFrame.getJSONArray("orientation").getFloat(3);
    frame.setOrientation(new Quaternion(x, y, z, w));
    frame.setMagnitude(jsonFrame.getFloat("magnitude"));
    return frame;
  }
  //TODO Restore 2D
  /*
  protected Frame toFrame(JSONObject jsonFrame) {
    Frame frame = new Frame(is3D());
    float x, y, z;
    x = jsonFrame.getJSONArray("position").getFloat(0);
    y = jsonFrame.getJSONArray("position").getFloat(1);
    z = jsonFrame.getJSONArray("position").getFloat(2);
    Vector pos = new Vector(x, y, z);
    frame.setPosition(pos);
    if (is2D())
      frame.setOrientation(new Rot(jsonFrame.getJSONArray("orientation").getFloat(0)));
    else {
      x = jsonFrame.getJSONArray("orientation").getFloat(0);
      y = jsonFrame.getJSONArray("orientation").getFloat(1);
      z = jsonFrame.getJSONArray("orientation").getFloat(2);
      float w = jsonFrame.getJSONArray("orientation").getFloat(3);
      frame.setOrientation(new Quaternion(x, y, z, w));
    }
    frame.setMagnitude(jsonFrame.getFloat("magnitude"));
    return frame;
  }
  */

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code frame} into a P5
   * JSONObject.
   */
  protected JSONObject toJSONObject(Frame frame) {
    JSONObject jsonFrame = new JSONObject();
    jsonFrame.setFloat("magnitude", frame.magnitude());
    jsonFrame.setJSONArray("position", toJSONArray(frame.position()));
    jsonFrame.setJSONArray("orientation", toJSONArray(frame.orientation()));
    return jsonFrame;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code vector} into a P5
   * JSONArray.
   */
  protected JSONArray toJSONArray(Vector vector) {
    JSONArray jsonVec = new JSONArray();
    jsonVec.setFloat(0, vector.x());
    jsonVec.setFloat(1, vector.y());
    jsonVec.setFloat(2, vector.z());
    return jsonVec;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code rot} into a P5
   * JSONArray.
   */
  protected JSONArray toJSONArray(Quaternion rot) {
    JSONArray jsonRot = new JSONArray();
    Quaternion quaternion = (Quaternion) rot;
    jsonRot.setFloat(0, quaternion.x());
    jsonRot.setFloat(1, quaternion.y());
    jsonRot.setFloat(2, quaternion.z());
    jsonRot.setFloat(3, quaternion.w());
    return jsonRot;
  }
  //TODO Restore 2D
  /*
  protected JSONArray toJSONArray(Rotation rot) {
    JSONArray jsonRot = new JSONArray();
    if (is3D()) {
      Quaternion quat = (Quaternion) rot;
      jsonRot.setFloat(0, quat.x());
      jsonRot.setFloat(1, quat.y());
      jsonRot.setFloat(2, quat.z());
      jsonRot.setFloat(3, quat.w());
    } else
      jsonRot.setFloat(0, rot.angle());
    return jsonRot;
  }
  */

  protected boolean unchachedBuffer;
  public PGraphics targetPGraphics;

  /**
   * Draw all graph {@link #nodes(boolean)} into the {@link #pg()} buffer. A similar (but
   * slightly less efficient) effect may be achieved with
   * {@code for (Node frame : nodes()) frame.draw(pg());}.
   * <p>
   * Note that {@code drawNodes()} is typically called from within your sketch
   * {@link #pApplet()} draw() loop.
   * <p>
   * This method is implementing by simply calling
   * {@link Graph#traverse()}.
   * <p>
   * <b>Attention:</b> this method should be called after {@link MatrixHandler#bind()} (i.e.,
   * eye update which happens at {@link #preDraw()}) and before any other transformation
   * of the modelview takes place.
   *
   * @see #nodes(boolean)
   * @see #pg()
   * @see #drawNodes(PGraphics)
   */
  public void drawNodes() {
    targetPGraphics = pg();
    traverse();
  }

  /**
   * Draw all {@link #nodes(boolean)} into the given pgraphics. No
   * {@code pgraphics.beginDraw()/endDraw()} calls take place. This method allows shader
   * chaining.
   * <p>
   * Note that {@code drawNodes(pickingBuffer())} (which enables 'picking' of the nodes
   * using a <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a>
   * technique is called by {@link #postDraw()}.
   * <p>
   * <b>Attention:</b> this method should be called after {@link #bindMatrices(PGraphics)}
   * (i.e., manual eye update) and before any other transformation of the modelview takes
   * place.
   *
   * @param pgraphics
   * @see #nodes(boolean)
   * @see #drawNodes()
   */
  public void drawNodes(PGraphics pgraphics) {
    // 1. Set pgraphics matrices using a custom MatrixHandler
    bindMatrices(pgraphics);
    // 2. Draw all nodes into pgraphics
    targetPGraphics = pgraphics;
    traverse();
  }

  /**
   * Returns a new matrix helper for the given {@code pgraphics}. Rarely needed.
   * <p>
   * Note that the current graph matrix helper may be retrieved by {@link #matrixHandler()}
   * .
   *
   * @see #matrixHandler()
   * @see #setMatrixHandler(MatrixHandler)
   * @see #drawNodes()
   * @see #drawNodes(PGraphics)
   * @see #applyWorldTransformation(PGraphics, Frame)
   */
  public MatrixHandler matrixHelper(PGraphics pgraphics) {
    return (pgraphics instanceof processing.opengl.PGraphicsOpenGL) ?
        new GLMatrixHandler(this, (PGraphicsOpenGL) pgraphics) :
        new Java2DMatrixHandler(this, pgraphics);
  }

  /**
   * Same as {@code matrixHandler(pgraphics).bind(false)}. Set the {@code pgraphics}
   * matrices by calling
   * {@link MatrixHandler#bindProjection(Matrix)} and
   * {@link MatrixHandler#bindModelView(Matrix)} (only makes sense
   * when {@link #pg()} is different than {@code pgraphics}).
   * <p>
   * This method doesn't perform any computation, but simple retrieve the current matrices
   * whose actual computation has been updated in {@link #preDraw()}.
   */
  public void bindMatrices(PGraphics pgraphics) {
    if (this.pg() == pgraphics)
      return;
    MatrixHandler mh = matrixHelper(pgraphics);
    mh.bindProjection(projection());
    mh.bindView(view());
    mh.cacheProjectionView();
    mh.bindModelView(view());
  }

  @Override
  protected void visitNode(Node node) {
    targetPGraphics.pushMatrix();
    applyTransformation(targetPGraphics, node);
    if (node instanceof Node)
      node.visitCallback();
    for (Node child : node.children())
      visitNode(child);
    targetPGraphics.popMatrix();
  }

  /**
   * Apply the local transformation defined by the given {@code frame} on the given
   * {@code pgraphics}. This method doesn't call {@link #bindMatrices(PGraphics)} which
   * should be called manually (only makes sense when {@link #pg()} is different than
   * {@code pgraphics}). Needed by {@link #applyWorldTransformation(PGraphics, Frame)}.
   *
   * @see #applyWorldTransformation(PGraphics, Frame)
   * @see #bindMatrices(PGraphics)
   */
  public static void applyTransformation(PGraphics pgraphics, Frame frame) {
    if (pgraphics instanceof PGraphics3D) {
      pgraphics.translate(frame.translation().vec[0], frame.translation().vec[1], frame.translation().vec[2]);
      pgraphics.rotate(frame.rotation().angle(), ((Quaternion) frame.rotation()).axis().vec[0],
          ((Quaternion) frame.rotation()).axis().vec[1], ((Quaternion) frame.rotation()).axis().vec[2]);
      pgraphics.scale(frame.scaling(), frame.scaling(), frame.scaling());
    } else {
      pgraphics.translate(frame.translation().x(), frame.translation().y());
      pgraphics.rotate(frame.rotation().angle());
      pgraphics.scale(frame.scaling(), frame.scaling());
    }
  }

  /**
   * Apply the global transformation defined by the given {@code frame} on the given
   * {@code pgraphics}. This method doesn't call {@link #bindMatrices(PGraphics)} which
   * should be called manually (only makes sense when {@link #pg()} is different than
   * {@code pgraphics}).
   *
   * @see #applyTransformation(PGraphics, Frame)
   * @see #bindMatrices(PGraphics)
   */
  public static void applyWorldTransformation(PGraphics pgraphics, Frame frame) {
    Frame refFrame = frame.reference();
    if (refFrame != null) {
      applyWorldTransformation(pgraphics, refFrame);
      applyTransformation(pgraphics, frame);
    } else {
      applyTransformation(pgraphics, frame);
    }
  }

  // SCREENDRAWING

  /**
   * Need to override it because of this issue:
   * https://github.com/remixlab/proscene/issues/1
   */
  @Override
  public void beginScreenDrawing() {
    beginScreenDrawing(pg());
  }

  /**
   * Begins screen drawing on an arbitrary PGraphics instance using {@link #eye()}
   * parameters. Don't forget to call {@link #endScreenDrawing(PGraphics)} after screen
   * drawing ends.
   *
   * @see #endScreenDrawing(PGraphics)
   * @see #beginScreenDrawing()
   */
  public void beginScreenDrawing(PGraphics p) {
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
          + "endScreenDrawing() and they cannot be nested. Check your implementation!");
    startCoordCalls++;
    p.hint(PApplet.DISABLE_OPTIMIZED_STROKE);// -> new line not present in Graph.bS
    disableDepthTest(p);
    // if-else same as:
    // matrixHandler(p).beginScreenDrawing();
    // but perhaps a bit more efficient
    if (p == pg())
      matrixHandler().beginScreenDrawing();
    else
      matrixHelper(p).beginScreenDrawing();
  }

  /**
   * Need to override it because of this issue:
   * https://github.com/remixlab/proscene/issues/1
   */
  @Override
  public void endScreenDrawing() {
    endScreenDrawing(pg());
  }

  /**
   * Ends screen drawing on the arbitrary PGraphics instance using {@link #eye()}
   * parameters. The screen drawing should happen between
   * {@link #beginScreenDrawing(PGraphics)} and this method.
   *
   * @see #beginScreenDrawing(PGraphics)
   * @see #endScreenDrawing()
   */
  public void endScreenDrawing(PGraphics p) {
    startCoordCalls--;
    if (startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenDrawing() call followed by a "
          + "endScreenDrawing() and they cannot be nested. Check your implementation!");
    // if-else same as:
    // matrixHandler(p).endScreenDrawing();
    // but perhaps a bit more efficient
    if (p == pg())
      matrixHandler().endScreenDrawing();
    else
      matrixHelper(p).endScreenDrawing();
    enableDepthTest(p);
    p.hint(PApplet.ENABLE_OPTIMIZED_STROKE);// -> new line not present in Graph.eS
  }

  // TODO took from Abstract



  // drawing

  /**
   * Same as {@code vertex(pg(), v)}.
   *
   * @see #vertex(PGraphics, float[])
   */
  public void vertex(float[] v) {
    vertex(pg(), v);
  }

  /**
   * Wrapper for PGraphics.vertex(v)
   */
  public static void vertex(PGraphics pg, float[] v) {
    pg.vertex(v);
  }

  /**
   * Same as {@code if (this.is2D()) vertex(pg(), x, y); elsevertex(pg(), x, y, z)}.
   *
   * @see #vertex(PGraphics, float, float, float)
   */
  public void vertex(float x, float y, float z) {
    if (is2D())
      vertex(pg(), x, y);
    else
      vertex(pg(), x, y, z);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,z)
   */
  public static void vertex(PGraphics pg, float x, float y, float z) {
    if (pg instanceof PGraphics3D)
      pg.vertex(x, y, z);
    else
      pg.vertex(x, y);
  }

  /**
   * Same as
   * {@code if (this.is2D()) vertex(pg(), x, y, u, v); else vertex(pg(), x, y, z, u, v);}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   * @see #vertex(PGraphics, float, float, float, float, float)
   */
  public void vertex(float x, float y, float z, float u, float v) {
    if (is2D())
      vertex(pg(), x, y, u, v);
    else
      vertex(pg(), x, y, z, u, v);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,z,u,v)
   */
  public static void vertex(PGraphics pg, float x, float y, float z, float u, float v) {
    if (pg instanceof PGraphics3D)
      pg.vertex(x, y, z, u, v);
    else
      pg.vertex(x, y, u, v);
  }

  /**
   * Same as {@code vertex(pg(), x, y)}.
   *
   * @see #vertex(PGraphics, float, float)
   */
  public void vertex(float x, float y) {
    vertex(pg(), x, y);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y)
   */
  public static void vertex(PGraphics pg, float x, float y) {
    pg.vertex(x, y);
  }

  /**
   * Same as {@code vertex(pg(), x, y, u, v)}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   */
  public void vertex(float x, float y, float u, float v) {
    vertex(pg(), x, y, u, v);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,u,v)
   */
  public static void vertex(PGraphics pg, float x, float y, float u, float v) {
    pg.vertex(x, y, u, v);
  }

  /**
   * Same as
   * {@code if (this.is2D()) line(pg(), x1, y1, x2, y2); else line(pg(), x1, y1, z1, x2, y2, z2);}
   * .
   *
   * @see #line(PGraphics, float, float, float, float, float, float)
   */
  public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
    if (is2D())
      line(pg(), x1, y1, x2, y2);
    else
      line(pg(), x1, y1, z1, x2, y2, z2);
  }

  /**
   * Wrapper for PGraphics.line(x1, y1, z1, x2, y2, z2)
   */
  public static void line(PGraphics pg, float x1, float y1, float z1, float x2, float y2, float z2) {
    if (pg instanceof PGraphics3D)
      pg.line(x1, y1, z1, x2, y2, z2);
    else
      pg.line(x1, y1, x2, y2);
  }

  /**
   * Same as {@code pg().line(x1, y1, x2, y2)}.
   *
   * @see #line(PGraphics, float, float, float, float)
   */
  public void line(float x1, float y1, float x2, float y2) {
    line(pg(), x1, y1, x2, y2);
  }

  /**
   * Wrapper for PGraphics.line(x1, y1, x2, y2)
   */
  public static void line(PGraphics pg, float x1, float y1, float x2, float y2) {
    pg.line(x1, y1, x2, y2);
  }

  /**
   * Converts a {@link Vector} to a PVec.
   */
  public static PVector toPVector(Vector v) {
    return new PVector(v.x(), v.y(), v.z());
  }

  /**
   * Converts a PVec to a {@link Vector}.
   */
  public static Vector toVec(PVector v) {
    return new Vector(v.x, v.y, v.z);
  }

  /**
   * Converts a {@link Matrix} to a PMatrix3D.
   */
  public static PMatrix3D toPMatrix(Matrix m) {
    float[] a = m.getTransposed(new float[16]);
    return new PMatrix3D(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14],
            a[15]);
  }

  /**
   * Converts a PMatrix3D to a {@link Matrix}.
   */
  public static Matrix toMat(PMatrix3D m) {
    return new Matrix(m.get(new float[16]), true);
  }

  /**
   * Converts a PMatrix2D to a {@link Matrix}.
   */
  public static Matrix toMat(PMatrix2D m) {
    return toMat(new PMatrix3D(m));
  }

  /**
   * Converts a {@link Matrix} to a PMatrix2D.
   */
  public static PMatrix2D toPMatrix2D(Matrix m) {
    float[] a = m.getTransposed(new float[16]);
    return new PMatrix2D(a[0], a[1], a[3], a[4], a[5], a[7]);
  }

  // DRAWING

  /**
   * Convenience function that simply calls {@code drawPath(kfi, 1, 6, 100)}.
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator kfi) {
    drawPath(kfi, 1, 6, 100);
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, 1, 6, scale)}
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator kfi, float scale) {
    drawPath(kfi, 1, 6, scale);
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, mask, nbFrames, * 100)}
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator kfi, int mask, int nbFrames) {
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
   * Use {@link #drawArrow(Vector, Vector, float)} to place the arrow in 3D.
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
  public void drawArrow(Vector from, Vector to, float radius) {
    pushModelView();
    translate(from.x(), from.y(), from.z());
    applyModelView(new Quaternion(new Vector(0, 0, 1), Vector.subtract(to, from)).matrix());
    drawArrow(Vector.subtract(to, from).magnitude(), radius);
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
   * @see #drawFilledCircle(int, Vector, float)
   */
  public void drawFilledCircle(Vector center, float radius) {
    drawFilledCircle(40, center, radius);
  }

  /**
   * Draws a cylinder of width {@code w} and height {@code h}, along the positive
   * {@code z} axis.
   */
  public void drawCylinder(float w, float h) {
    drawCylinder(pg(), w, h);
  }

  /**
   * Same as {@code drawCylinder(pg, radius()/6, radius()/3)}.
   */
  public void drawCylinder(PGraphics pg) {
    drawCylinder(pg, radius() / 6, radius() / 3);
  }

  /**
   * Low-level version of {@link #drawCylinder(float, float)}.
   * <p>
   * Calls {@link #drawCylinder(float, float)} on {@code pg}.
   */
  public static void drawCylinder(PGraphics pg, float w, float h) {
    if (!(pg instanceof PGraphics3D)) {
      Graph.showDepthWarning("drawCylinder");
      return;
    }
    pg.pushStyle();
    float px, py;

    pg.beginShape(PApplet.QUAD_STRIP);
    for (float i = 0; i < 13; i++) {
      px = (float) Math.cos(PApplet.radians(i * 30)) * w;
      py = (float) Math.sin(PApplet.radians(i * 30)) * w;
      vertex(pg, px, py, 0);
      vertex(pg, px, py, h);
    }
    pg.endShape();

    pg.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pg, 0, 0, 0);
    for (float i = 12; i > -1; i--) {
      px = (float) Math.cos(PApplet.radians(i * 30)) * w;
      py = (float) Math.sin(PApplet.radians(i * 30)) * w;
      vertex(pg, px, py, 0);
    }
    pg.endShape();

    pg.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pg, 0, 0, h);
    for (float i = 0; i < 13; i++) {
      px = (float) Math.cos(PApplet.radians(i * 30)) * w;
      py = (float) Math.sin(PApplet.radians(i * 30)) * w;
      vertex(pg, px, py, h);
    }
    pg.endShape();
    pg.popStyle();
  }

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
  public void drawHollowCylinder(int detail, float w, float h, Vector m, Vector n) {
    drawHollowCylinder(pg(), detail, w, h, m, n);
  }

  /**
   * Low-level version of {@link #drawHollowCylinder(int, float, float, Vector, Vector)}.
   * <p>
   * Calls {@link #drawHollowCylinder(int, float, float, Vector, Vector)} on {@code pg}.
   */
  public static void drawHollowCylinder(PGraphics pg, int detail, float w, float h, Vector m, Vector n) {
    if (!(pg instanceof PGraphics3D)) {
      Graph.showDepthWarning("drawHollowCylinder");
      return;
    }
    pg.pushStyle();
    // eqs taken from: http://en.wikipedia.org/wiki/Line-plane_intersection
    Vector pm0 = new Vector(0, 0, 0);
    Vector pn0 = new Vector(0, 0, h);
    Vector l0 = new Vector();
    Vector l = new Vector(0, 0, 1);
    Vector p = new Vector();
    float x, y, d;

    pg.noStroke();
    pg.beginShape(PApplet.QUAD_STRIP);

    for (float t = 0; t <= detail; t++) {
      x = w * PApplet.cos(t * PApplet.TWO_PI / detail);
      y = w * PApplet.sin(t * PApplet.TWO_PI / detail);
      l0.set(x, y, 0);

      d = (m.dot(Vector.subtract(pm0, l0))) / (l.dot(m));
      p = Vector.add(Vector.multiply(l, d), l0);
      vertex(pg, p.x(), p.y(), p.z());

      l0.setZ(h);
      d = (n.dot(Vector.subtract(pn0, l0))) / (l.dot(n));
      p = Vector.add(Vector.multiply(l, d), l0);
      vertex(pg, p.x(), p.y(), p.z());
    }
    pg.endShape();
    pg.popStyle();
  }

  // Cone v1

  /**
   * Draws a cone along the positive {@code z} axis, with its base centered at
   * {@code (x,y)}, height {@code h}, and radius {@code r}.
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float r, float h) {
    drawCone(pg(), detail, x, y, r, h);
  }

  /**
   * Same as {@code cone(pg, det, 0, 0, r, h);}
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public static void drawCone(PGraphics pg, int det, float r, float h) {
    drawCone(pg, det, 0, 0, r, h);
  }

  /**
   * Same as {@code cone(pg, 12, 0, 0, r, h);}
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public static void drawCone(PGraphics pg, float r, float h) {
    drawCone(pg, 12, 0, 0, r, h);
  }

  /**
   * Same as {@code drawCone(pg, 12, 0, 0, radius()/4, sqrt(3) * radius()/4)}.
   */
  public void drawCone(PGraphics pg) {
    float r = radius() / 4;
    drawCone(pg, 12, 0, 0, r, (float) Math.sqrt((float) 3) * r);
  }

  /**
   * Low-level version of {@link #drawCone(int, float, float, float, float)}.
   * <p>
   * Calls {@link #drawCone(int, float, float, float, float)} on {@code pg}.
   */
  public static void drawCone(PGraphics pg, int detail, float x, float y, float r, float h) {
    if (!(pg instanceof PGraphics3D)) {
      Graph.showDepthWarning("drawCone");
      return;
    }
    pg.pushStyle();
    float unitConeX[] = new float[detail + 1];
    float unitConeY[] = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float a1 = PApplet.TWO_PI * i / detail;
      unitConeX[i] = r * (float) Math.cos(a1);
      unitConeY[i] = r * (float) Math.sin(a1);
    }

    pg.pushMatrix();
    pg.translate(x, y);
    pg.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pg, 0, 0, h);
    for (int i = 0; i <= detail; i++) {
      vertex(pg, unitConeX[i], unitConeY[i], 0.0f);
    }
    pg.endShape();
    pg.popMatrix();
    pg.popStyle();
  }

  // Cone v2

  /**
   * Same as {@code cone(pg, det, 0, 0, r1, r2, h)}
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   */
  public static void drawCone(PGraphics pg, int det, float r1, float r2, float h) {
    drawCone(pg, det, 0, 0, r1, r2, h);
  }

  /**
   * Same as {@code cone(pg, 18, 0, 0, r1, r2, h);}
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   */
  public static void drawCone(PGraphics pg, float r1, float r2, float h) {
    drawCone(pg, 18, 0, 0, r1, r2, h);
  }

  /**
   * Draws a truncated cone along the positive {@code z} axis, with its base centered at
   * {@code (x,y)}, height {@code h} , and radii {@code r1} and {@code r2} (basis and
   * height respectively).
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float r1, float r2, float h) {
    drawCone(pg(), detail, x, y, r1, r2, h);
  }

  /**
   * Low-level version of {@link #drawCone(int, float, float, float, float, float)}.
   * <p>
   * Calls {@link #drawCone(int, float, float, float, float, float)} on {@code pg}.
   */
  public static void drawCone(PGraphics pg, int detail, float x, float y, float r1, float r2, float h) {
    if (!(pg instanceof PGraphics3D)) {
      Graph.showDepthWarning("drawCone");
      return;
    }
    pg.pushStyle();
    float firstCircleX[] = new float[detail + 1];
    float firstCircleY[] = new float[detail + 1];
    float secondCircleX[] = new float[detail + 1];
    float secondCircleY[] = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float a1 = PApplet.TWO_PI * i / detail;
      firstCircleX[i] = r1 * (float) Math.cos(a1);
      firstCircleY[i] = r1 * (float) Math.sin(a1);
      secondCircleX[i] = r2 * (float) Math.cos(a1);
      secondCircleY[i] = r2 * (float) Math.sin(a1);
    }

    pg.pushMatrix();
    pg.translate(x, y);
    pg.beginShape(PApplet.QUAD_STRIP);
    for (int i = 0; i <= detail; i++) {
      vertex(pg, firstCircleX[i], firstCircleY[i], 0);
      vertex(pg, secondCircleX[i], secondCircleY[i], h);
    }
    pg.endShape();
    pg.popMatrix();
    pg.popStyle();
  }

  /**
   * Draws axes of length {@code length} which origin correspond to the world coordinate
   * system origin.
   *
   * @see #drawGrid(float, int)
   */
  public void drawAxes(float length) {
    drawAxes(pg(), length);
  }

  /**
   * Same as {@code drawAxes(pg, radius()/5)}.
   */
  public void drawAxes(PGraphics pg) {
    drawAxes(pg, radius() / 5);
  }

  /**
   * Low-level version of {@link #drawAxes(float)}.
   * <p>
   * Calls {@link #drawAxes(float)} on {@code pg}.
   */
  public void drawAxes(PGraphics pg, float length) {
    pg.pushStyle();
    pg.colorMode(PApplet.RGB, 255);
    float charWidth = length / 40.0f;
    float charHeight = length / 30.0f;
    float charShift = 1.04f * length;

    pg.pushStyle();
    pg.beginShape(PApplet.LINES);
    pg.strokeWeight(2);
    if (is2D()) {
      // The X
      pg.stroke(200, 0, 0);
      pg.vertex(charShift + charWidth, -charHeight);
      pg.vertex(charShift - charWidth, charHeight);
      pg.vertex(charShift - charWidth, -charHeight);
      pg.vertex(charShift + charWidth, charHeight);

      // The Y
      charShift *= 1.02;
      pg.stroke(0, 200, 0);
      pg.vertex(charWidth, charShift + (isRightHanded() ? charHeight : -charHeight));
      pg.vertex(0.0f, charShift + 0.0f);
      pg.vertex(-charWidth, charShift + (isRightHanded() ? charHeight : -charHeight));
      pg.vertex(0.0f, charShift + 0.0f);
      pg.vertex(0.0f, charShift + 0.0f);
      pg.vertex(0.0f, charShift + -(isRightHanded() ? charHeight : -charHeight));
    } else {
      // The X
      pg.stroke(200, 0, 0);
      pg.vertex(charShift, charWidth, -charHeight);
      pg.vertex(charShift, -charWidth, charHeight);
      pg.vertex(charShift, -charWidth, -charHeight);
      pg.vertex(charShift, charWidth, charHeight);
      // The Y
      pg.stroke(0, 200, 0);
      pg.vertex(charWidth, charShift, (isLeftHanded() ? charHeight : -charHeight));
      pg.vertex(0.0f, charShift, 0.0f);
      pg.vertex(-charWidth, charShift, (isLeftHanded() ? charHeight : -charHeight));
      pg.vertex(0.0f, charShift, 0.0f);
      pg.vertex(0.0f, charShift, 0.0f);
      pg.vertex(0.0f, charShift, -(isLeftHanded() ? charHeight : -charHeight));
      // The Z
      pg.stroke(0, 100, 200);
      pg.vertex(-charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pg.vertex(charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pg.vertex(charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pg.vertex(-charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
      pg.vertex(-charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
      pg.vertex(charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
    }
    pg.endShape();
    pg.popStyle();

    // X Axis
    pg.stroke(200, 0, 0);
    if (is2D())
      pg.line(0, 0, length, 0);
    else
      pg.line(0, 0, 0, length, 0, 0);
    // Y Axis
    pg.stroke(0, 200, 0);
    if (is2D())
      pg.line(0, 0, 0, length);
    else
      pg.line(0, 0, 0, 0, length, 0);

    // Z Axis
    if (is3D()) {
      pg.stroke(0, 100, 200);
      pg.line(0, 0, 0, 0, 0, length);
    }
    pg.popStyle();
  }

  /**
   * Draws a grid in the XY plane, centered on (0,0,0) (defined in the current coordinate
   * system).
   * <p>
   * {@code size} and {@code nbSubdivisions} define its geometry.
   *
   * @see #drawAxes(float)
   */
  public void drawGrid(float size, int nbSubdivisions) {
    drawGrid(pg(), size, nbSubdivisions);
  }

  /**
   * Same as {@code drawGrid(pg, radius()/4, 10)}.
   */
  public void drawGrid(PGraphics pg) {
    drawGrid(pg, radius() / 4, 10);
  }

  /**
   * Low-level version of {@link #drawGrid(float)}.
   * <p>
   * Calls {@link #drawGrid(float)} on {@code pg}.
   */
  public void drawGrid(PGraphics pg, float size, int nbSubdivisions) {
    pg.pushStyle();
    pg.beginShape(LINES);
    for (int i = 0; i <= nbSubdivisions; ++i) {
      final float pos = size * (2.0f * i / nbSubdivisions - 1.0f);
      vertex(pg, pos, -size);
      vertex(pg, pos, +size);
      vertex(pg, -size, pos);
      vertex(pg, size, pos);
    }
    pg.endShape();
    pg.popStyle();
  }

  /**
   * Draws a dotted-grid in the XY plane, centered on (0,0,0) (defined in the current
   * coordinate system).
   * <p>
   * {@code size} and {@code nbSubdivisions} define its geometry.
   *
   * @see #drawAxes(float)
   */
  public void drawDottedGrid(float size, int nbSubdivisions) {
    drawDottedGrid(pg(), size, nbSubdivisions);
  }

  /**
   * Same as {@code drawDottedGrid(pg, radius()/4, 10)}.
   */
  public void drawDottedGrid(PGraphics pg) {
    drawDottedGrid(pg, radius() / 4, 10);
  }

  /**
   * Low-level version of {@link #drawDottedGrid(float, int)}.
   * <p>
   * Calls {@link #drawDottedGrid(float, int)} on {@code pg}.
   */
  public void drawDottedGrid(PGraphics pg, float size, int nbSubdivisions) {
    pg.pushStyle();
    float posi, posj;
    pg.beginShape(POINTS);
    for (int i = 0; i <= nbSubdivisions; ++i) {
      posi = size * (2.0f * i / nbSubdivisions - 1.0f);
      for (int j = 0; j <= nbSubdivisions; ++j) {
        posj = size * (2.0f * j / nbSubdivisions - 1.0f);
        vertex(pg, posi, posj);
      }
    }
    pg.endShape();
    int internalSub = 5;
    int subSubdivisions = nbSubdivisions * internalSub;
    float currentWeight = pg.strokeWeight;
    pg.colorMode(HSB, 255);
    float hue = pg.hue(pg.strokeColor);
    float saturation = pg.saturation(pg.strokeColor);
    float brightness = pg.brightness(pg.strokeColor);
    pg.stroke(hue, saturation, brightness * 10f / 17f);
    pg.strokeWeight(currentWeight / 2);
    pg.beginShape(POINTS);
    for (int i = 0; i <= subSubdivisions; ++i) {
      posi = size * (2.0f * i / subSubdivisions - 1.0f);
      for (int j = 0; j <= subSubdivisions; ++j) {
        posj = size * (2.0f * j / subSubdivisions - 1.0f);
        if (((i % internalSub) != 0) || ((j % internalSub) != 0))
          vertex(pg, posi, posj);
      }
    }
    pg.endShape();
    pg.popStyle();
  }

  /**
   * Draws a representation of the {@code eye} in the graph.
   * <p>
   * The near and far planes are drawn as quads, the frustum is drawn using lines and the
   * camera up vector is represented by an arrow to disambiguate the drawing.
   * <p>
   * <b>Note:</b> The drawing of a Scene's own Scene.eye() should not be visible, but
   * may create artifacts due to numerical imprecisions.
   */
  public void drawEye() {
    drawEye(false);
  }

  /**
   * Applies the {@code eye.frame()} transformation and then calls
   * {@link #drawEye(PGraphics, boolean)} on the graph {@link #pg()}. If
   * {@code texture} draws the projected graph on the near plane.
   *
   * @see #applyTransformation(Frame)
   * @see #drawEye(PGraphics, boolean)
   */
  public void drawEye(boolean texture) {
    pg().pushMatrix();
    applyTransformation(eye());
    drawEye(pg(), texture);
    pg().popMatrix();
  }

  /**
   * Same as {@code drawEye(pg, eye, false)}.
   *
   * @see #drawEye(PGraphics, boolean)
   */
  public void drawEye(PGraphics pg) {
    drawEye(pg, false);
  }

  /**
   * Implementation of {@link #drawEye()}. If {@code texture} draws the projected graph
   * on the near plane.
   * <p>
   * Warning: texture only works with opengl renderers.
   * <p>
   * Note that if {@code eye.graph()).pg() == pg} this method hasGrabber not effect at all.
   */
  public void drawEye(PGraphics pg, boolean texture) {
    // Key here is to represent the eye getBoundaryWidthHeight, zNear and zFar params
    // (which are is given in world units) in eye units.
    // Hence they should be multiplied by: 1 / eye.frame().magnitude()
    if (pg() == pg) {
      System.out.println("Warning: No drawEye done, eye.graph()).pg() and pg are the same!");
      return;
    }
    pg.pushStyle();

    // boolean drawFarPlane = true;
    // int farIndex = drawFarPlane ? 1 : 0;
    int farIndex = is3D() ? 1 : 0;
    boolean ortho = false;
    if (is3D())
      if (type() == Type.ORTHOGRAPHIC)
        ortho = true;

    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector[] points = new Vector[2];
    points[0] = new Vector();
    points[1] = new Vector();

    if (is2D() || ortho) {
      float[] wh = getBoundaryWidthHeight();
      points[0].setX(wh[0] * 1 / eye().magnitude());
      points[1].setX(wh[0] * 1 / eye().magnitude());
      points[0].setY(wh[1] * 1 / eye().magnitude());
      points[1].setY(wh[1] * 1 / eye().magnitude());
    }

    if (is3D()) {
      points[0].setZ(zNear() * 1 / eye().magnitude());
      points[1].setZ(zFar() * 1 / eye().magnitude());
      if (type() == Type.PERSPECTIVE) {
        points[0].setY(points[0].z() * PApplet.tan(fieldOfView() / 2.0f));
        points[0].setX(points[0].y() * aspectRatio());
        float ratio = points[1].z() / points[0].z();
        points[1].setY(ratio * points[0].y());
        points[1].setX(ratio * points[0].x());
      }

      // Frustum lines
      switch (type()) {
        case PERSPECTIVE: {
          pg.beginShape(PApplet.LINES);
          Scene.vertex(pg, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pg, points[farIndex].x(), points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pg, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pg, -points[farIndex].x(), points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pg, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pg, -points[farIndex].x(), -points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pg, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pg, points[farIndex].x(), -points[farIndex].y(), -points[farIndex].z());
          pg.endShape();
          break;
        }
        case ORTHOGRAPHIC: {
          // if (drawFarPlane) {
          pg.beginShape(PApplet.LINES);
          Scene.vertex(pg, points[0].x(), points[0].y(), -points[0].z());
          Scene.vertex(pg, points[1].x(), points[1].y(), -points[1].z());
          Scene.vertex(pg, -points[0].x(), points[0].y(), -points[0].z());
          Scene.vertex(pg, -points[1].x(), points[1].y(), -points[1].z());
          Scene.vertex(pg, -points[0].x(), -points[0].y(), -points[0].z());
          Scene.vertex(pg, -points[1].x(), -points[1].y(), -points[1].z());
          Scene.vertex(pg, points[0].x(), -points[0].y(), -points[0].z());
          Scene.vertex(pg, points[1].x(), -points[1].y(), -points[1].z());
          pg.endShape();
          // }
          break;
        }
      }
    }

    // Up arrow
    float arrowHeight = 1.5f * points[0].y();
    float baseHeight = 1.2f * points[0].y();
    float arrowHalfWidth = 0.5f * points[0].x();
    float baseHalfWidth = 0.3f * points[0].x();

    pg.noStroke();
    // Arrow base
    if (texture) {
      pg.pushStyle();// end at arrow
      pg.colorMode(PApplet.RGB, 255);
      float r = pg.red(pg.fillColor);
      float g = pg.green(pg.fillColor);
      float b = pg.blue(pg.fillColor);
      pg.fill(r, g, b, 126);// same transparency as near plane texture
    }
    pg.beginShape(PApplet.QUADS);
    if (isLeftHanded()) {
      Scene.vertex(pg, -baseHalfWidth, -points[0].y(), -points[0].z());
      Scene.vertex(pg, baseHalfWidth, -points[0].y(), -points[0].z());
      Scene.vertex(pg, baseHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pg, -baseHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pg, -baseHalfWidth, points[0].y(), -points[0].z());
      Scene.vertex(pg, baseHalfWidth, points[0].y(), -points[0].z());
      Scene.vertex(pg, baseHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pg, -baseHalfWidth, baseHeight, -points[0].z());
    }
    pg.endShape();

    // Arrow
    pg.beginShape(PApplet.TRIANGLES);
    if (isLeftHanded()) {
      Scene.vertex(pg, 0.0f, -arrowHeight, -points[0].z());
      Scene.vertex(pg, -arrowHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pg, arrowHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pg, 0.0f, arrowHeight, -points[0].z());
      Scene.vertex(pg, -arrowHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pg, arrowHalfWidth, baseHeight, -points[0].z());
    }
    if (texture)
      pg.popStyle();// begin at arrow base
    pg.endShape();

    // Planes
    // far plane
    drawPlane(pg, points[1], new Vector(0, 0, -1), false);
    // near plane
    drawPlane(pg, points[0], new Vector(0, 0, 1), texture);

    pg.popStyle();
  }

  public void drawEyeNearPlane() {
    drawEyeNearPlane(false);
  }

  /**
   * Applies the {@code eye.frame()} transformation and then calls
   * {@link #drawEye(PGraphics, boolean)} on the graph {@link #pg()}. If
   * {@code texture} draws the projected graph on the near plane.
   *
   * @see #applyTransformation(Frame)
   * @see #drawEye(PGraphics, boolean)
   */
  public void drawEyeNearPlane(boolean texture) {
    pg().pushMatrix();
    applyTransformation(eye());
    drawEyeNearPlane(pg(), texture);
    pg().popMatrix();
  }

  /**
   * Same as {@code drawEyeNearPlane(pg, eye, false)}.
   *
   * @see #drawEyeNearPlane(PGraphics, boolean)
   */
  public void drawEyeNearPlane(PGraphics pg) {
    drawEyeNearPlane(pg, false);
  }

  /**
   * Draws the eye near plane. If {@code texture} draws the projected graph on the plane.
   * <p>
   * Warning: texture only works with opengl renderers.
   * <p>
   * Note that if {@code eye.graph()).pg() == pg} this method hasGrabber not effect at all.
   */
  public void drawEyeNearPlane(PGraphics pg, boolean texture) {
    // Key here is to represent the eye getBoundaryWidthHeight and zNear params
    // (which are is given in world units) in eye units.
    // Hence they should be multiplied by: 1 / eye.frame().magnitude()
    if (pg() == pg) {
      System.out.println("Warning: No drawEyeNearPlane done, eye.graph()).pg() and pg are the same!");
      return;
    }
    pg.pushStyle();
    boolean ortho = false;
    if (is3D())
      if (type() == Type.ORTHOGRAPHIC)
        ortho = true;
    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector corner = new Vector();
    if (is2D() || ortho) {
      float[] wh = getBoundaryWidthHeight();
      corner.setX(wh[0] * 1 / eye().magnitude());
      corner.setY(wh[1] * 1 / eye().magnitude());
    }
    if (is3D()) {
      corner.setZ(zNear() * 1 / eye().magnitude());
      if (type() == Type.PERSPECTIVE) {
        corner.setY(corner.z() * PApplet.tan(fieldOfView() / 2.0f));
        corner.setX(corner.y() * aspectRatio());
      }
    }
    drawPlane(pg, corner, new Vector(0, 0, 1), texture);
  }

  protected void drawPlane(PGraphics pg, Vector corner, Vector normal, boolean texture) {
    pg.pushStyle();
    // near plane
    pg.beginShape(PApplet.QUAD);
    pg.normal(normal.x(), normal.y(), normal.z());
    if (pg instanceof PGraphicsOpenGL && texture) {
      pg.textureMode(NORMAL);
      pg.tint(255, 126); // Apply transparency without changing color
      pg.texture(pg());
      Scene.vertex(pg, corner.x(), corner.y(), -corner.z(), 1, 1);
      Scene.vertex(pg, -corner.x(), corner.y(), -corner.z(), 0, 1);
      Scene.vertex(pg, -corner.x(), -corner.y(), -corner.z(), 0, 0);
      Scene.vertex(pg, corner.x(), -corner.y(), -corner.z(), 1, 0);
    } else {
      Scene.vertex(pg, corner.x(), corner.y(), -corner.z());
      Scene.vertex(pg, -corner.x(), corner.y(), -corner.z());
      Scene.vertex(pg, -corner.x(), -corner.y(), -corner.z());
      Scene.vertex(pg, corner.x(), -corner.y(), -corner.z());
    }
    pg.endShape();
    pg.popStyle();
  }

  /**
   * Calls {@link #drawProjector(PGraphics, Vector)} on the graph {@link #pg()}.
   * <p>
   * Since this method uses the eye origin and zNear plane to draw the other end of the
   * projector it should be used in conjunction with {@link #drawEye(PGraphics)}.
   *
   * @see #drawProjector(PGraphics, Vector)
   * @see #drawProjectors(List)
   */
  public void drawProjector(Vector src) {
    drawProjector(pg(), src);
  }

  /**
   * Draws as a line (or point in 2D) the projection of {@code src} (given in the world
   * coordinate system) onto the near plane.
   * <p>
   * Since this method uses the eye origin and zNear plane to draw the other end of the
   * projector it should be used in conjunction with
   * {@link #drawEye(PGraphics, boolean)}.
   * <p>
   * Note that if {@code eye.graph()).pg() == pg} this method hasGrabber not effect at all.
   *
   * @see #drawProjector(PGraphics, Vector)
   * @see #drawProjectors(PGraphics, List)
   */
  public void drawProjector(PGraphics pg, Vector src) {
    drawProjectors(pg, Arrays.asList(src));
  }

  /**
   * Calls {@link #drawProjectors(PGraphics, List)} on the graph {@link #pg()}.
   * <p>
   * Since this method uses the eye origin and zNear plane to draw the other end of the
   * projector it should be used in conjunction with {@link #drawEye(PGraphics)}.
   *
   * @see #drawProjectors(PGraphics, List)
   * @see #drawProjector(Vector)
   */
  public void drawProjectors(List<Vector> src) {
    drawProjectors(pg(), src);
  }

  /**
   * Draws as lines (or points in 2D) the projection of each vector in {@code src} (all of
   * which should be given in the world coordinate system) onto the near plane.
   * <p>
   * Since this method uses the eye origin and zNear plane to draw the other end of the
   * projector it should be used in conjunction with
   * {@link #drawEye(PGraphics, boolean)}.
   * <p>
   * Note that if {@code pg() == pg} this method has not effect at all.
   *
   * @see #drawProjectors(PGraphics, List)
   * @see #drawProjector(PGraphics, Vector)
   */
  public void drawProjectors(PGraphics pg, List<Vector> src) {
    if (pg() == pg) {
      System.out.println("Warning: No drawProjectors done, eye.graph()).pg() and pg are the same!");
      return;
    }
    pg.pushStyle();
    if (is2D()) {
      pg.beginShape(PApplet.POINTS);
      for (Vector s : src)
        Scene.vertex(pg, s.x(), s.y());
      pg.endShape();
    } else {
      // if ORTHOGRAPHIC: do it in the eye coordinate system
      // if PERSPECTIVE: do it in the world coordinate system
      Vector o = new Vector();
      if (type() == Type.ORTHOGRAPHIC) {
        pg.pushMatrix();
        applyTransformation(eye());
      }
      // in PERSPECTIVE cache the transformed origin
      else
        o = eye().inverseCoordinatesOf(new Vector());
      pg.beginShape(PApplet.LINES);
      for (Vector s : src) {
        if (type() == Type.ORTHOGRAPHIC) {
          Vector v = eye().coordinatesOf(s);
          Scene.vertex(pg, v.x(), v.y(), v.z());
          // Key here is to represent the eye zNear param (which is given in world units)
          // in eye units.
          // Hence it should be multiplied by: 1 / eye.frame().magnitude()
          // The neg sign is because the zNear is positive but the eye view direction is
          // the negative Z-axis
          Scene.vertex(pg, v.x(), v.y(), -zNear() * 1 / eye().magnitude());
        } else {
          Scene.vertex(pg, s.x(), s.y(), s.z());
          Scene.vertex(pg, o.x(), o.y(), o.z());
        }
      }
      pg.endShape();
      if (type() == Type.ORTHOGRAPHIC)
        pg.popMatrix();
    }
    pg.popStyle();
  }

  /**
   * Draws the path used to interpolate the
   * {@link Interpolator#frame()}
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
  public void drawPath(Interpolator kfi, int mask, int nbFrames, float scale) {
    pg().pushStyle();
    if (mask != 0) {
      int nbSteps = 30;
      pg().strokeWeight(2 * pg().strokeWeight);
      pg().noFill();
      List<Frame> path = kfi.path();
      if (((mask & 1) != 0) && path.size() > 1) {
        pg().beginShape();
        for (Frame myFr : path)
          vertex(myFr.position().x(), myFr.position().y(), myFr.position().z());
        pg().endShape();
      }
      if ((mask & 6) != 0) {
        int count = 0;
        if (nbFrames > nbSteps)
          nbFrames = nbSteps;
        float goal = 0.0f;

        for (Frame myFr : path)
          if ((count++) >= goal) {
            goal += nbSteps / (float) nbFrames;
            pushModelView();

            applyTransformation(myFr);

            if ((mask & 2) != 0)
              drawKFIEye(scale);
            if ((mask & 4) != 0)
              drawAxes(scale / 10.0f);

            popModelView();
          }
      }
      pg().strokeWeight(pg().strokeWeight / 2f);
    }
    // draw the picking targets:
    for (int index = 0; index < kfi.numberOfKeyFrames(); index++)
      drawPickingTarget(kfi.keyFrame(index));
    pg().popStyle();
  }

  /**
   * Internal use.
   */
  protected void drawKFIEye(float scale) {
    pg().pushStyle();
    float halfHeight = scale * (is2D() ? 1.2f : 0.07f);
    float halfWidth = halfHeight * 1.3f;
    float dist = halfHeight / (float) Math.tan(PApplet.PI / 8.0f);

    float arrowHeight = 1.5f * halfHeight;
    float baseHeight = 1.2f * halfHeight;
    float arrowHalfWidth = 0.5f * halfWidth;
    float baseHalfWidth = 0.3f * halfWidth;

    // Frustum outline
    pg().noFill();
    pg().beginShape();
    vertex(-halfWidth, halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(halfWidth, -halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    pg().endShape();
    pg().noFill();
    pg().beginShape();
    vertex(halfWidth, -halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(-halfWidth, halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    pg().endShape();

    // Up arrow
    pg().noStroke();
    pg().fill(pg().strokeColor);
    // Base
    pg().beginShape(PApplet.QUADS);

    if (isLeftHanded()) {
      vertex(baseHalfWidth, -halfHeight, -dist);
      vertex(-baseHalfWidth, -halfHeight, -dist);
      vertex(-baseHalfWidth, -baseHeight, -dist);
      vertex(baseHalfWidth, -baseHeight, -dist);
    } else {
      vertex(-baseHalfWidth, halfHeight, -dist);
      vertex(baseHalfWidth, halfHeight, -dist);
      vertex(baseHalfWidth, baseHeight, -dist);
      vertex(-baseHalfWidth, baseHeight, -dist);
    }

    pg().endShape();
    // Arrow
    pg().beginShape(PApplet.TRIANGLES);

    if (isLeftHanded()) {
      vertex(0.0f, -arrowHeight, -dist);
      vertex(arrowHalfWidth, -baseHeight, -dist);
      vertex(-arrowHalfWidth, -baseHeight, -dist);
    } else {
      vertex(0.0f, arrowHeight, -dist);
      vertex(-arrowHalfWidth, baseHeight, -dist);
      vertex(arrowHalfWidth, baseHeight, -dist);
    }
    pg().endShape();
    pg().popStyle();
  }

  /**
   * Draws a cross on the screen centered under pixel {@code (px, py)}, and edge of size
   * {@code size}.
   */
  public void drawCross(float px, float py, float size) {
    drawCross(pg(), px, py, size);
  }

  public void drawCross(PGraphics pg, float px, float py, float size) {
    float half_size = size / 2f;
    pg.pushStyle();
    beginScreenDrawing(pg);
    pg.noFill();
    pg.beginShape(LINES);
    vertex(pg, px - half_size, py);
    vertex(pg, px + half_size, py);
    vertex(pg, px, py - half_size);
    vertex(pg, px, py + half_size);
    pg.endShape();
    endScreenDrawing(pg);
    pg.popStyle();
  }

  /**
   * Draws a filled circle using screen coordinates.
   *
   * @param subdivisions Number of triangles approximating the circle.
   * @param center       Circle screen center.
   * @param radius       Circle screen radius.
   */
  public void drawFilledCircle(int subdivisions, Vector center, float radius) {
    drawFilledCircle(pg(), subdivisions, center, radius);
  }

  public void drawFilledCircle(PGraphics pg, int subdivisions, Vector center, float radius) {
    pg.pushStyle();
    float precision = PApplet.TWO_PI / subdivisions;
    float x = center.x();
    float y = center.y();
    float angle, x2, y2;
    beginScreenDrawing(pg);
    pg.noStroke();
    pg.beginShape(TRIANGLE_FAN);
    vertex(pg, x, y);
    for (angle = 0.0f; angle <= PApplet.TWO_PI + 1.1 * precision; angle += precision) {
      x2 = x + PApplet.sin(angle) * radius;
      y2 = y + PApplet.cos(angle) * radius;
      vertex(pg, x2, y2);
    }
    pg.endShape();
    endScreenDrawing(pg);
    pg.popStyle();
  }

  /**
   * Draws a filled square using screen coordinates.
   *
   * @param center Square screen center.
   * @param edge   Square edge length.
   */
  public void drawFilledSquare(Vector center, float edge) {
    drawFilledSquare(pg(), center, edge);
  }

  public void drawFilledSquare(PGraphics pg, Vector center, float edge) {
    float half_edge = edge / 2f;
    pg.pushStyle();
    float x = center.x();
    float y = center.y();
    beginScreenDrawing(pg);
    pg.noStroke();
    pg.beginShape(QUADS);
    vertex(pg, x - half_edge, y + half_edge);
    vertex(pg, x + half_edge, y + half_edge);
    vertex(pg, x + half_edge, y - half_edge);
    vertex(pg, x - half_edge, y - half_edge);
    pg.endShape();
    endScreenDrawing(pg);
    pg.popStyle();
  }

  /**
   * Draws the classical shooter target on the screen.
   *
   * @param center Center of the target on the screen
   * @param length Length of the target in pixels
   */
  public void drawShooterTarget(Vector center, float length) {
    drawShooterTarget(pg(), center, length);
  }

  public void drawShooterTarget(PGraphics pg, Vector center, float length) {
    float half_length = length / 2f;
    pg.pushStyle();
    float x = center.x();
    float y = center.y();
    beginScreenDrawing(pg);
    pg.noFill();

    pg.beginShape();
    vertex(pg, (x - half_length), (y - half_length) + (0.6f * half_length));
    vertex(pg, (x - half_length), (y - half_length));
    vertex(pg, (x - half_length) + (0.6f * half_length), (y - half_length));
    pg.endShape();

    pg.beginShape();
    vertex(pg, (x + half_length) - (0.6f * half_length), (y - half_length));
    vertex(pg, (x + half_length), (y - half_length));
    vertex(pg, (x + half_length), ((y - half_length) + (0.6f * half_length)));
    pg.endShape();

    pg.beginShape();
    vertex(pg, (x + half_length), ((y + half_length) - (0.6f * half_length)));
    vertex(pg, (x + half_length), (y + half_length));
    vertex(pg, ((x + half_length) - (0.6f * half_length)), (y + half_length));
    pg.endShape();

    pg.beginShape();
    vertex(pg, (x - half_length) + (0.6f * half_length), (y + half_length));
    vertex(pg, (x - half_length), (y + half_length));
    vertex(pg, (x - half_length), ((y + half_length) - (0.6f * half_length)));
    pg.endShape();
    endScreenDrawing(pg);
    drawCross(center.x(), center.y(), 0.6f * length);
    pg.popStyle();
  }

  /**
   * Draws all GrabberFrames' picking targets: a shooter target visual hint of
   * {@link Node#grabsInputThreshold()} pixels size.
   * <p>
   * <b>Attention:</b> the target is drawn either if the iFrame is part of camera path and
   * keyFrame is {@code true}, or if the iFrame is not part of camera path and keyFrame is
   * {@code false}.
   */
  public void drawPickingTarget(Node iFrame) {
    if (iFrame.isEye()) {
      System.err.println("eye nodes don't have a picking target");
      return;
    }
    if (!iFrame.isVisualHintEnabled())
      return;
    // if (!inputHandler().hasGrabber(iFrame)) {
    // System.err.println("addGrabber iFrame to motionAgent before drawing picking target");
    // return;
    // }
    Vector center = projectedCoordinatesOf(iFrame.position());
    if (inputHandler().isInputGrabber(iFrame)) {
      pg().pushStyle();
      pg().strokeWeight(2 * pg().strokeWeight);
      pg().colorMode(HSB, 255);
      float hue = pg().hue(pg().strokeColor);
      float saturation = pg().saturation(pg().strokeColor);
      float brightness = pg().brightness(pg().strokeColor);
      pg().stroke(hue, saturation * 1.4f, brightness * 1.4f);
      drawShooterTarget(center, (iFrame.grabsInputThreshold() + 1));
      pg().popStyle();
    } else {
      pg().pushStyle();
      pg().colorMode(HSB, 255);
      float hue = pg().hue(pg().strokeColor);
      float saturation = pg().saturation(pg().strokeColor);
      float brightness = pg().brightness(pg().strokeColor);
      pg().stroke(hue, saturation * 1.4f, brightness);
      drawShooterTarget(center, iFrame.grabsInputThreshold());
      pg().popStyle();
    }
  }

  /**
   * Draws a torus solenoid. Dandelion logo.
   *
   * Code contributed by Jacques Maire (http://www.alcys.com/) See also:
   * http://www.mathcurve.com/courbes3d/solenoidtoric/solenoidtoric.shtml
   * http://crazybiocomputing.blogspot.fr/2011/12/3d-curves-toric-solenoids.html
   *
   * @param faces
   * @param detail
   * @param insideRadius
   * @param outsideRadius
   */
  public void drawTorusSolenoid(int faces, int detail, float insideRadius, float outsideRadius) {
    drawTorusSolenoid(pg(), faces, detail, insideRadius, outsideRadius);
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(pg, 6)}.
   *
   * @see #drawTorusSolenoid(PGraphics, int, int, float, float)
   */
  public static void drawTorusSolenoid(PGraphics pg) {
    drawTorusSolenoid(pg, 6);
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(pg, 6, insideRadius)}
   * .
   *
   * @see #drawTorusSolenoid(PGraphics, int, int, float, float)
   */
  public static void drawTorusSolenoid(PGraphics pg, float insideRadius) {
    drawTorusSolenoid(pg, 6, insideRadius);
  }

  /**
   * Convenience function that simply calls
   * {@code drawTorusSolenoid(pg, faces, 100, insideRadius, insideRadius * 1.3f)} .
   *
   * @see #drawTorusSolenoid(int, int, float, float)
   */
  public static void drawTorusSolenoid(PGraphics pg, int faces, float insideRadius) {
    drawTorusSolenoid(pg, faces, 100, insideRadius, insideRadius * 1.3f);
  }

  /**
   * {@link #drawTorusSolenoid(PGraphics, int, int, float, float)} pn {@code pg} .
   */
  public static void drawTorusSolenoid(PGraphics pg, int faces, int detail, float insideRadius, float outsideRadius) {
    pg.pushStyle();
    pg.noStroke();
    Vector v1, v2;
    int b, ii, jj, a;
    float eps = PApplet.TWO_PI / detail;
    for (a = 0; a < faces; a += 2) {
      pg.beginShape(PApplet.TRIANGLE_STRIP);
      b = (a <= (faces - 1)) ? a + 1 : 0;
      for (int i = 0; i < (detail + 1); i++) {
        ii = (i < detail) ? i : 0;
        jj = ii + 1;
        float ai = eps * jj;
        float alpha = a * PApplet.TWO_PI / faces + ai;
        v1 = new Vector((outsideRadius + insideRadius * PApplet.cos(alpha)) * PApplet.cos(ai),
                (outsideRadius + insideRadius * PApplet.cos(alpha)) * PApplet.sin(ai), insideRadius * PApplet.sin(alpha));
        alpha = b * PApplet.TWO_PI / faces + ai;
        v2 = new Vector((outsideRadius + insideRadius * PApplet.cos(alpha)) * PApplet.cos(ai),
                (outsideRadius + insideRadius * PApplet.cos(alpha)) * PApplet.sin(ai), insideRadius * PApplet.sin(alpha));
        vertex(pg, v1.x(), v1.y(), v1.z());
        vertex(pg, v2.x(), v2.y(), v2.z());
      }
      pg.endShape();
    }
    pg.popStyle();
  }
}