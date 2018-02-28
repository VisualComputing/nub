/****************************************************************************************
 * framesjs
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights refserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.processing;

import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.MatrixHandler;
import frames.core.Node;
import frames.input.Agent;
import frames.input.Event;
import frames.input.Grabber;
import frames.primitives.*;
import frames.timing.SequentialTimer;
import frames.timing.TimingHandler;
import frames.timing.TimingTask;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * A 2D or 3D interactive, on-screen or off-screen, Processing {@link Graph}.
 * <p>
 * <h2>Usage</h2>
 * Typical usage comprises three steps: scene instantiation, setting an eye
 * and setting some shapes.
 * <h3>Scene instantiation</h3>
 * Instantiate your on-screen scene at the {@code PApplet.setup()}:
 * <pre>
 * {@code
 * Scene scene;
 * void setup() {
 *   scene = new Scene(this);
 * }
 * }
 * </pre>
 * The scene {@link #frontBuffer()} corresponds to the {@code PApplet} main canvas.
 * <p>
 * Off-screen scenes should be instantiated upon a {@code PGraphics} object:
 * <pre>
 * {@code
 * Scene scene;
 * PGraphics canvas;
 * void setup() {
 *   canvas = createGraphics(500, 500, P3D);
 *   scene = new Scene(this, canvas);
 * }
 * }
 * </pre>
 * In this case, the scene {@link #frontBuffer()} corresponds to the {@code canvas}.
 * <h3>The eye</h3>
 * The scene eye can be an instance of {@link Frame} or a {@link Node}. To set the
 * eye from a frame instance use code such as the following:
 * <pre>
 * {@code
 * ...
 * Frame eye;
 * void setup() {
 *   ...
 *   eye = new Frame();
 *   scene.setEye(eye);
 * }
 * }
 * </pre>
 * The eye can be controlled programmatically using the powerful {@link Frame} API.
 * <p>
 * To set the eye from a node instance use code such as the following:
 * <pre>
 * {@code
 * ...
 * Node eye;
 * void setup() {
 *   ...
 *   eye = new Node(scene) {
 *     public void interact(Event event) {
 *       if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
 *         translate(event);
 *     }
 *   };
 *   scene.setDefaultGrabber(eye);
 * }
 * }
 * </pre>
 * The eye can be controlled both programmatically (since a {@link Node} is a
 * {@link Frame} specialization) and interactively (using the mouse, see
 * {@link #mouse()} and {@link Mouse}). Note the use of the anonymous
 * inner {@link Node} class used to define how the node will behave, refer to the
 * {@link Node} API for details. Note also the {@link #setDefaultGrabber(Grabber)}
 * call which will direct input to the eye when no other node is being picked.
 * <h3>Shapes</h3>
 * A {@link Shape} is a {@link Node} specialization that can be set from a
 * retained-mode rendering Processing {@code PShape} or from an immediate-mode
 * rendering Processing procedure. Shapes can be picked precisely using their projection
 * onto the screen, see {@link Shape#setPrecision(Node.Precision)}. Use
 * {@link #traverse()} to render all scene-graph shapes or {@link Shape#draw()} to
 * render a specific one instead.
 * <h3>Retained-mode shapes</h3>
 * To set a retained-mode shape use {@code Shape shape = new Shape(Scene scene,
 * PShape shape)} or {@code Shape shape = new Shape(Scene scene)} and then call
 * {@link Shape#set(PGraphics)}.
 * <h3>Immediate-mode shapes</h3>
 * To set an immediate-mode shape use code such as the following:
 * <pre>
 * {@code
 * ...
 * Shape shape;
 * void setup() {
 *   ...
 *   shape = new Shape(scene) {
 *     public void set(PGraphics canvas) {
 *       //immediate-mode rendering procedure
 *     }
 *   };
 * }
 * }
 * </pre>
 * <p>
 * Note tha shapes like nodes can be control interactively. Override
 * {@link Node#interact(Event)}, like it has been done above.
 * <h2>Key-frame interpolators</h2>
 * A frame (and hence a node or a shape) can be animated through a key-frame
 * Catmull-Rom interpolator path. Use code such as the following:
 * <pre>
 * {@code
 * Scene scene;
 * PShape pshape;
 * Shape shape;
 * Interpolator interpolator;
 * void setup() {
 *   ...
 *   shape = new Shape(scene, pshape);
 *   interpolator = new Interpolator(shape);
 *   for (int i = 0; i < random(4, 10); i++)
 *     interpolator.addKeyFrame(Node.random(scene));
 *   interpolator.start();
 * }
 * }
 * </pre>
 * which will create a random (see {@link Node#random(Graph)}) interpolator path
 * containing [4..10] key-frames (see {@link Interpolator#addKeyFrame(Frame)}).
 * The interpolation is also started (see {@link Interpolator#start()}). The
 * interpolator path may be drawn with code like this:
 * <pre>
 * {@code
 * ...
 * void draw() {
 *   scene.traverse();
 *   scene.drawPath(interpolator, 5);
 * }
 * }
 * </pre>
 * while {@link #traverse()} will draw the animated shape(s),
 * {@link #drawPath(Interpolator, int)} will draw the interpolated path too.
 * <h2>Non-standard interactivity</h2>
 * To control your scene nodes by means different than the {@link #mouse()} (see
 * {@link Mouse}), implement an {@link Agent} and call {@link #registerAgent(Agent)}.
 * <h2>Drawing functionality</h2>
 * There are several static drawing functions that complements those already provided
 * by Processing, such as: {@link #drawCylinder(PGraphics, int, float, float)},
 * {@link #drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)},
 * {@link #drawCone(PGraphics, int, float, float, float, float)},
 * {@link #drawCone(PGraphics, int, float, float, float, float, float)} and
 * {@link #drawTorusSolenoid(PGraphics, int, int, float, float)}.
 * <p>
 * Drawing functions that take a {@code PGraphics} parameter (including the above
 * static ones), such as {@link #beginScreenCoordinates(PGraphics)},
 * {@link #endScreenCoordinates(PGraphics)}, {@link #drawAxes(PGraphics, float)},
 * {@link #drawCross(PGraphics, float, float, float)} and {@link #drawGrid(PGraphics)}
 * among others, can be used to set a {@link Shape} (see {@link Shape#set(PGraphics)}).
 * <p>
 * Another scene's eye (different than this one) can be drawn with
 * {@link #drawEye(Graph)}. Typical usage include interactive minimaps and
 * visibility culling visualization and debugging.
 */
public class Scene extends Graph implements PConstants {
  // Timing
  protected boolean _javaTiming;
  public static String prettyVersion = "0.1.0";
  public static String version = "1";

  // P R O C E S S I N G A P P L E T A N D O B J E C T S
  protected PApplet _parent;
  protected PGraphics _fg;

  // E X C E P T I O N H A N D L I N G
  protected int _beginOffScreenDrawingCalls;

  // off-screen scenes:
  //TODO make protected
  public static Scene _lastScene;
  public long _lastDisplay;
  protected boolean _autofocus;
  protected static int _offScreenScenes;

  // offscreen
  protected Point _upperLeftCorner;
  protected boolean _offscreen;

  // 4. Agents
  protected Mouse _mouse;

  // _bb : picking buffer
  protected PGraphics _targetPGraphics;
  protected PGraphics _bb;
  protected boolean _bbEnabled;
  protected PShader _triangleShader, _lineShader, _pointShader;

  // CONSTRUCTORS

  /**
   * Constructor that defines an on-screen Processing scene. Same as
   * {@code this(pApplet, pApplet.g)}.
   *
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, PGraphics, int, int)
   */
  public Scene(PApplet pApplet) {
    this(pApplet, pApplet.g);
  }

  /**
   * Same as {@code this(pApplet, pGraphics, 0, 0)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics, int, int)
   */
  public Scene(PApplet pApplet, PGraphics pGraphics) {
    this(pApplet, pGraphics, 0, 0);
  }

  /**
   * Main constructor defining a left-handed Processing compatible scene. Calls
   * {@link #setMatrixHandler(MatrixHandler)} using a customized
   * {@link MatrixHandler} depending on the {@link #frontBuffer()} type (see
   * {@link Java2DMatrixHandler} and {@link GLMatrixHandler}). The constructor
   * instantiates also the {@link #mouse()}.
   * <p>
   * An off-screen Processing scene is defined if {@code pGraphics != pApplet.g}. In this
   * case the {@code x} and {@code y} parameters define the position of the upper-left corner
   * where the off-screen scene is expected to be displayed, see {@link #display()}. If
   * {@code pGraphics == pApplet.g}) (which defines an on-screen scene, see also
   * {@link #isOffscreen()}), the values of x and y are meaningless (both are set to 0 to be
   * taken as dummy values). Render into an off-screen graph requires the drawing code to be
   * enclose by {@link #beginDraw()} and {@link #endDraw()}. To display an off-screen scene
   * call {@link #display()}.
   *
   * @see Graph#Graph(int, int)
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics)
   */
  public Scene(PApplet pApplet, PGraphics pGraphics, int x, int y) {
    super(pGraphics instanceof PGraphics3D ? Type.PERSPECTIVE : Type.TWO_D, pGraphics.width, pGraphics.height);
    // 1. P5 objects
    _parent = pApplet;
    _fg = pGraphics;
    _offscreen = pGraphics != pApplet.g;
    _upperLeftCorner = _offscreen ? new Point(x, y) : new Point(0, 0);

    // 2. Matrix helper
    setMatrixHandler(matrixHandler(pGraphics));

    // 3. Frames & picking buffer
    _bb = (frontBuffer() instanceof processing.opengl.PGraphicsOpenGL) ?
        pApplet().createGraphics(frontBuffer().width, frontBuffer().height, frontBuffer() instanceof PGraphics3D ? P3D : P2D) :
        null;
    if (_bb != null) {
      _triangleShader = pApplet().loadShader("PickingBuffer.frag");
      _lineShader = pApplet().loadShader("PickingBuffer.frag");
      _pointShader = pApplet().loadShader("PickingBuffer.frag");
    }

    // 4. Create _agents and register P5 methods
    _mouse = new Mouse(this, originCorner());
    _parent.registerMethod("mouseEvent", mouse());

    // this.setDefaultKeyBindings();
    if (!isOffscreen()) {
      pApplet().registerMethod("pre", this);
      pApplet().registerMethod("draw", this);
    } else
      _offScreenScenes++;
    enableAutoFocus();
    // TODO buggy
    pApplet().registerMethod("dispose", this);

    // 5. Handed
    setLeftHanded();
  }

  //TODO experimental rename and add api docs.
  // decide later
  /*
  public Node orbitNode() {
    class OrbitNode extends Node {
      Shortcut left = new Shortcut(PApplet.LEFT);
      Shortcut right = new Shortcut(PApplet.RIGHT);
      Shortcut wheel = new Shortcut(processing.event.MouseEvent.WHEEL);
      KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
      KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
      KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
      KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);

      public OrbitNode(Graph graph) {
        super(graph);
      }

      // this one gotta be overridden because we want a copied frame (e.g., line 100 above, i.e.,
      // scene.eye().get()) to have the same behavior as its original.
      protected OrbitNode(Graph otherGraph, OrbitNode otherNode) {
        super(otherGraph, otherNode);
      }

      @Override
      public OrbitNode get() {
        return new OrbitNode(this.graph(), this);
      }

      @Override
      public void interact(MotionEvent2 event) {
        if (left.matches(event.shortcut()))
          rotate(event);
        if (right.matches(event.shortcut()))
          translate(event);
      }

      @Override
      public void interact(MotionEvent1 event) {
        if (event.shortcut().matches(wheel))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.shortcut().matches(upArrow))
          translateYPos();
        else if (event.shortcut().matches(downArrow))
          translateYNeg();
        else if (event.shortcut().matches(leftArrow))
          translateXNeg();
        else if (event.shortcut().matches(rightArrow))
          translateXPos();
      }
    }
    return new OrbitNode(this);
  }
  //*/

  /*
  public Shape orbitShape() {
    class OrbitShape extends Shape {
      Shortcut left = new Shortcut(PApplet.LEFT);
      Shortcut right = new Shortcut(PApplet.RIGHT);
      Shortcut wheel = new Shortcut(processing.event.MouseEvent.WHEEL);
      KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
      KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
      KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
      KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);

      public OrbitShape(Scene scene) {
        super(scene);
      }

      // this one gotta be overridden because we want a copied frame (e.g., line 141 above, i.e.,
      // scene.eye().get()) to have the same behavior as its original.
      protected OrbitShape(Scene otherScene, OrbitShape otherShape) {
        super(otherScene, otherShape);
      }

      @Override
      public OrbitShape get() {
        return new OrbitShape(this.scene(), this);
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.shortcut().matches(upArrow))
          translateYPos();
        else if (event.shortcut().matches(downArrow))
          translateYNeg();
        else if (event.shortcut().matches(leftArrow))
          translateXNeg();
        else if (event.shortcut().matches(rightArrow))
          translateXPos();
      }

      @Override
      public void interact(MotionEvent2 event) {
        if (left.matches(event.shortcut()))
          rotate(event);
        if (right.matches(event.shortcut()))
          translate(event);
      }

      @Override
      public void interact(MotionEvent1 event) {
        if (event.shortcut().matches(wheel))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }
    }
    return new OrbitShape(this);
  }
  //*/

  /**
   * Returns the upper left corner of the scene window. It's always (0,0) for on-screen
   * scenes, but off-screen scenes may define it elsewhere on a canvas.
   */
  public Point originCorner() {
    return _upperLeftCorner;
  }

  // P5 STUFF

  /**
   * Returns the PApplet instance this scene is related to.
   */
  public PApplet pApplet() {
    return _parent;
  }

  /**
   * Returns the PGraphics instance this scene is related to. It may be the PApplet's,
   * if the scene is on-screen or an user-defined one if the scene {@link #isOffscreen()}.
   */
  public PGraphics frontBuffer() {
    return _fg;
  }

  // PICKING BUFFER

  /**
   * Returns the back buffer, used for
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a>.
   */
  public PGraphics backBuffer() {
    return _bb;
  }

  /**
   * Internal use. Traverse the scene {@link #nodes()}) into the
   * {@link #backBuffer()} to perform picking on the scene {@link #nodes()}.
   * <p>
   * Called by {@link #draw()} (on-screen scenes) and {@link #endDraw()} (off-screen
   * scenes).
   */
  protected void _renderBackBuffer() {
    if (!_bbEnabled)
      return;
    backBuffer().beginDraw();
    backBuffer().pushStyle();
    backBuffer().background(0);
    traverse(backBuffer());
    backBuffer().popStyle();
    backBuffer().endDraw();
    // if (frames().size() > 0)
    backBuffer().loadPixels();
  }

  // Mouse agent

  /**
   * Returns the default mouse.
   *
   * @see #enableMouse()
   * @see #isMouseEnabled()
   * @see #disableMouse()
   */
  public Mouse mouse() {
    return _mouse;
  }

  /**
   * Enables the {@link #mouse()}.
   *
   * @see #mouse()
   * @see #isMouseEnabled()
   * @see #disableMouse()
   */
  public void enableMouse() {
    if (!isMouseEnabled()) {
      registerAgent(mouse());
      _parent.registerMethod("mouseEvent", mouse());
    }
  }

  /**
   * Disables the {@link #mouse()}.
   *
   * @see #mouse()
   * @see #isMouseEnabled()
   * @see #enableMouse()
   */
  public boolean disableMouse() {
    if (isMouseEnabled()) {
      _parent.unregisterMethod("mouseEvent", mouse());
      return unregisterAgent(mouse());
    }
    return false;
  }

  /**
   * Returns {@code true} if the {@link #mouse()} is enabled and {@code false}
   * otherwise.
   *
   * @see #mouse()
   * @see #enableMouse()
   * @see #disableMouse()
   */
  public boolean isMouseEnabled() {
    return isAgentRegistered(mouse());
  }

  // OPENGL

  /**
   * Same as {@code return setCenterFromPixel(new Point(x, y))}.
   *
   * @see #setCenterFromPixel(Point)
   */
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

  /**
   * Returns the depth (z-value) of the object under the {@code pixel}. Used by
   * {@link #pointUnderPixel(Point)}.
   * <p>
   * The z-value ranges in [0..1] (near and far plane respectively). In 3D note that this
   * value is not a linear interpolation between {@link #zNear()} and {@link #zFar()}:
   * {@code z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z')} where {@code z'} is
   * the distance from the point you project to the eye, along the {@link #viewDirection()}.
   *
   * @see #pointUnderPixel(Point)
   */
  public float pixelDepth(Point pixel) {
    PGraphicsOpenGL pggl;
    if (frontBuffer() instanceof PGraphicsOpenGL)
      pggl = (PGraphicsOpenGL) frontBuffer();
    else
      throw new RuntimeException("frontBuffer() is not instance of PGraphicsOpenGL");
    float[] depth = new float[1];
    PGL pgl = pggl.beginPGL();
    pgl.readPixels(pixel.x(), (height() - pixel.y()), 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT,
        FloatBuffer.wrap(depth));
    pggl.endPGL();
    return depth[0];
  }

  /**
   * Returns the world coordinates of the 3D point located at {@code pixel} (x,y) on
   * screen. May be null if no object is under pixel.
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
   * Same as {@code return pixelDepth(new Point(x, y))}.
   *
   * @see #pixelDepth(Point)
   */
  public float pixelDepth(float x, float y) {
    return pixelDepth(new Point(x, y));
  }

  /**
   * Disables z-buffer.
   */
  public void disableDepthTest() {
    disableDepthTest(frontBuffer());
  }

  /**
   * Disables depth test on the PGraphics instance.
   *
   * @see #enableDepthTest(PGraphics)
   */
  public void disableDepthTest(PGraphics pGraphics) {
    pGraphics.hint(PApplet.DISABLE_DEPTH_TEST);
  }

  /**
   * Enables z-buffer.
   */
  public void enableDepthTest() {
    enableDepthTest(frontBuffer());
  }

  /**
   * Enables depth test on the PGraphics instance.
   *
   * @see #disableDepthTest(PGraphics)
   */
  public void enableDepthTest(PGraphics pGraphics) {
    pGraphics.hint(PApplet.ENABLE_DEPTH_TEST);
  }

  // TIMING

  @Override
  public void registerTask(TimingTask task) {
    if (areTimersSequential())
      timingHandler().registerTask(task);
    else
      timingHandler().registerTask(task, new ParallelTimer(task));
  }

  /**
   * Sets all {@link #timingHandler()} timers as (single-threaded) {@link SequentialTimer}(s).
   *
   * @see #setParallelTimers()
   * @see #shiftTimers()
   * @see #areTimersSequential()
   */
  public void setSequentialTimers() {
    if (areTimersSequential())
      return;

    _javaTiming = false;
    timingHandler().restoreTimers();
  }

  /**
   * Sets all {@link #timingHandler()} timers as (multi-threaded) java.util.Timer(s).
   *
   * @see #setSequentialTimers()
   * @see #shiftTimers()
   * @see #areTimersSequential()
   */
  public void setParallelTimers() {
    if (!areTimersSequential())
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
      task.setTimer(new ParallelTimer(task));
      if (isActive) {
        if (rOnce)
          task.runOnce(period);
        else
          task.run(period);
      }
    }

    _javaTiming = true;
    PApplet.println("java util timers set");
  }

  /**
   * Returns true, if timing is handling sequentially (i.e., all {@link #timingHandler()}
   * timers are (single-threaded) {@link SequentialTimer}(s)).
   *
   * @see #setSequentialTimers()
   * @see #setParallelTimers()
   * @see #shiftTimers()
   */
  public boolean areTimersSequential() {
    return !_javaTiming;
  }

  /**
   * If {@link #areTimersSequential()} calls {@link #setParallelTimers()}, otherwise call
   * {@link #setSequentialTimers()}.
   */
  public void shiftTimers() {
    if (areTimersSequential())
      setParallelTimers();
    else
      setSequentialTimers();
  }

  // 3. Drawing methods

  /**
   * Convenience function that simply calls:
   * {@code return setAnchorFromPixel(new Point(x, y))}.
   *
   * @see #setAnchorFromPixel(Point)
   */
  public boolean setAnchorFromPixel(float x, float y) {
    return setAnchorFromPixel(new Point(x, y));
  }

  /**
   * The {@link #anchor()} is set to the point located under {@code pixel} on screen.
   * <p>
   * Returns {@code true} if a point was found under {@code pixel} and
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
   * Paint method which is called just before your {@code PApplet.draw()} method. Simply
   * calls {@link #preDraw()}. This method is registered at the PApplet and hence you
   * don't need to call it. Only meaningful if the graph is on-screen (it the graph
   * {@link #isOffscreen()} it even doesn't get registered at the PApplet.
   * <p>
   * If {@link #frontBuffer()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls {@link #setWidth(int)} and {@link #setHeight(int)}.
   *
   * @see #draw()
   * @see #preDraw()
   * @see #postDraw()
   * @see #beginDraw()
   * @see #endDraw()
   * @see #isOffscreen()
   */
  public void pre() {
    if ((width() != frontBuffer().width) || (height() != frontBuffer().height)) {
      setWidth(frontBuffer().width);
      setHeight(frontBuffer().height);
    }
    preDraw();
    pushModelView();
  }

  /**
   * Paint method which is called just after your {@code PApplet.draw()} method. Simply
   * render the back buffer (useful for picking) and call {@link #postDraw()}. This method
   * is registered at the PApplet and hence you don't need to call it. Only meaningful if
   * the graph is on-screen (it the graph {@link #isOffscreen()} it even doesn't get
   * registered at the PApplet.
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
    popModelView();
    _renderBackBuffer();
    postDraw();
    if (hasAutoFocus())
      _handleFocus();
  }

  // Off-screen

  /**
   * Returns {@code true} if this scene is off-screen and {@code false} otherwise.
   */
  public boolean isOffscreen() {
    return _offscreen;
  }

  /**
   * Only if the scene {@link #isOffscreen()}. Calls {@code frontBuffer().beginDraw()}
   * (hence there's no need to explicitly call it) and then {@link #preDraw()} .
   * <p>
   * If {@link #frontBuffer()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls {@link #setWidth(int)} and {@link #setHeight(int)}.
   *
   * @see #draw()
   * @see #preDraw()
   * @see #postDraw()
   * @see #pre()
   * @see #endDraw()
   * @see #isOffscreen()
   * @see #frontBuffer()
   */
  public void beginDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "begin(/end)Draw() should be used only within offscreen scenes. Check your implementation!");
    if (_beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    _beginOffScreenDrawingCalls++;
    if ((_width != frontBuffer().width) || (_height != frontBuffer().height)) {
      _width = frontBuffer().width;
      _height = frontBuffer().height;
      setWidth(_width);
      setHeight(_height);
    }
    // open off-screen pgraphics for drawing:
    frontBuffer().beginDraw();
    preDraw();
    pushModelView();
  }

  /**
   * Only if the dcene {@link #isOffscreen()}. Calls:
   * <p>
   * <ol>
   * <li>{@code frontBuffer().endDraw()} and hence there's no need to explicitly call it</li>
   * <li>{@code _renderBackBuffer()}: Render the back buffer (useful for picking)</li>
   * <li>{@link #postDraw()}</li>
   * <li>{@link #_handleFocus()} if {@link #hasAutoFocus()} is {@code true}</li>
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
   * @see #frontBuffer()
   */
  public void endDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "(begin/)endDraw() should be used only within offscreen scenes. Check your implementation!");
    _beginOffScreenDrawingCalls--;
    if (_beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    popModelView();
    frontBuffer().endDraw();
    _renderBackBuffer();
    postDraw();
    _lastDisplay = TimingHandler.frameCount;
    if (hasAutoFocus())
      _handleFocus();
  }

  /**
   * Same as {@code display(frontBuffer())}. Only meaningful if the graph {@link #isOffscreen()}.
   *
   * @see #display(PGraphics)
   * @see #frontBuffer()
   */
  public void display() {
    display(frontBuffer());
  }

  /**
   * Same as {@code pApplet().image(pgraphics, originCorner().x(), originCorner().y())}.
   * Only meaningful if the graph {@link #isOffscreen()}.
   */
  public void display(PGraphics pgraphics) {
    if (isOffscreen())
      pApplet().image(pgraphics, originCorner().x(), originCorner().y());
  }

  /**
   * Implementation of the "Focus follows mouse" policy. Used by {@link #_hasFocus()}.
   */
  protected boolean _hasMouseFocus() {
    return originCorner().x() < pApplet().mouseX && pApplet().mouseX < originCorner().x() + this.width()
        && originCorner().y() < pApplet().mouseY && pApplet().mouseY < originCorner().y() + this.height();
  }

  /**
   * Main condition evaluated by the {@link #_handleFocus()} algorithm, which defaults to
   * {@link #_hasMouseFocus()}.
   * <p>
   * Override this method to define a focus policy different than "focus follows mouse".
   */
  protected boolean _hasFocus() {
    return _hasMouseFocus();
  }

  /**
   * Called by {@link #endDraw()} if {@link #hasAutoFocus()} is {@code true}.
   */
  protected void _handleFocus() {
    if (_offScreenScenes == 0)
      return;
    if (isOffscreen()) {
      // Handling focus of non-overlapping scenes is trivial.
      // Suppose scn1 and scn2 overlap and also that scn2 is displayed on top of scn1, i.e.,
      // scn2.display() is to be called after scn1.display() (which is the _key observation).
      // Then, for a given frame either only scn1 _hasFocus() (which is handled trivially);
      // or, both, scn1 and scn2 _hasFocus(), which means only scn2 should retain focus
      // (while scn1 lose it).
      boolean available = true;
      if (_lastScene != null)
        if (_lastScene != this)
          // Note that _lastScene._lastDisplay == TimingHandler.frameCount - 1 returns true only
          // if the lastScene was assigned in the previous frame and false otherwise
          // (particularly, if it was assigned in the current frame) which means both: 1. If scn1
          // gained focus on the current frame it will lose it when the routine is run on scn2 in
          // the current frame; and, 2. If scn2 has gained focus in the previous frame, it will
          // prevent scn1 from having it back in the current frame.
          if (_lastScene._hasFocus() && _lastScene._lastDisplay == TimingHandler.frameCount - 1)
            available = false;
      if (_hasFocus() && _lastDisplay == TimingHandler.frameCount && available) {
        enableMouse();
        _lastScene = this;
      } else
        disableMouse();
    } else {
      if (_lastScene != null) {
        boolean available = true;
        if (_lastScene.isOffscreen() && (_lastScene._lastDisplay == TimingHandler.frameCount - 1
            || _lastScene._lastDisplay == TimingHandler.frameCount) && _lastScene._hasFocus()) {
          disableMouse();
          available = false;
        }
        if (_hasFocus() && available) {
          enableMouse();
          _lastScene = this;
        }
      }
    }
  }

  /**
   * When having multiple off-screen scenes displayed at once, it should be decided which
   * one will grab input from the {@link #mouse()}, so that code like this:
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
   * and it implements a "focus follows mouse" policy, so that the scene under the cursor
   * will grab input. If multiple scenes overlaps the scene on top will grab the input as
   * expected.
   * <p>
   * To implement a different policy either:
   * <p>
   * <ol>
   * <li>Override the {@link #_hasFocus()} scene method; or,</li>
   * <li>Call {@link #disableAutoFocus()} and implement your own focus policy at the
   * sketch space.</li>
   * </ol>
   * <p>
   * <b>Note</b> that for this policy to work, you should call {@link #display()} instead
   * of the papplet image() function on the {@link #frontBuffer()}.
   *
   * @see #beginDraw()
   * @see #endDraw()
   * @see #display()
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public boolean hasAutoFocus() {
    return _autofocus;
  }

  /**
   * Toggles the scene auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   */
  public void toggleAutoFocus() {
    if (hasAutoFocus())
      disableAutoFocus();
    else
      enableAutoFocus();
  }

  /**
   * Disables the scene auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #enableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void disableAutoFocus() {
    enableAutoFocus(false);
  }

  /**
   * Enables the scene auto-focus property.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus(boolean)
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void enableAutoFocus() {
    enableAutoFocus(true);
  }

  /**
   * Turns on or off the scene auto-focus property according to {@code flag}.
   * <p>
   * The {@link #hasAutoFocus()} property is {@code true} by default.
   *
   * @see #hasAutoFocus()
   * @see #enableAutoFocus()
   * @see #disableAutoFocus()
   * @see #toggleAutoFocus()
   */
  public void enableAutoFocus(boolean autoFocus) {
    _autofocus = autoFocus;
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
      System.out.println("Warning: no config saved! Off-screen graph config requires saveConfig(String fileName) to be called");
    else
      saveConfig("data/config.json");
  }

  /**
   * Saves the {@link #eye()}, the {@link #radius()} and the {@link #type()} into {@code fileName}.
   *
   * @see #saveConfig()
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig(String fileName) {
    JSONObject json = new JSONObject();
    json.setFloat("radius", radius());
    json.setString("type", type().name());
    json.setJSONObject("eye", _toJSONObject(eye()));

    //TODO restore
    /*
    // keyFrames
    JSONArray jsonPaths = new JSONArray();
    int i = 0;
    for (int _id : eye().keyFrameInterpolatorMap().keySet()) {
      JSONObject jsonPath = new JSONObject();
      jsonPath.setInt("_key", _id);
      jsonPath.setJSONArray("keyFrames", _toJSONArray(_id));
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
      System.out.println("Warning: no config loaded! Off-screen graph config requires loadConfig(String fileName) to be called");
    else
      loadConfig("config.json");
  }

  /**
   * Loads the {@link #eye()}, the {@link #radius()} and the {@link #type()} from {@code fileName}.
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
      String type = json.getString("type");
      setType(type.equals("PERSPECTIVE") ? Type.PERSPECTIVE :
          type.equals("ORTHOGRAPHIC") ? Type.ORTHOGRAPHIC : type.equals("TWO_D") ? Type.TWO_D : Type.CUSTOM);
      eye().setWorldMatrix(_toFrame(json.getJSONObject("eye")));

      /*
      JSONObject jsonEye = json.getJSONObject("eye");
      eye().setWorldMatrix(_toFrame(jsonEye));
      JSONArray paths = jsonEye.getJSONArray("paths");
      for (int i = 0; i < paths.size(); i++) {
        JSONObject path = paths.getJSONObject(i);
        int _id = path.getInt("_key");
        eye().clear(_id);
        JSONArray keyFrames = path.getJSONArray("keyFrames");
        _toInterpolator(keyFrames);
      }
      //*/
    }
  }

  /**
   * Used internally by {@link #loadConfig(String)}. Converts the P5 JSONArray into an interpolator.
   */
  protected Interpolator _toInterpolator(JSONArray jsonInterpolator) {
    Interpolator interpolator = new Interpolator(this);
    for (int j = 0; j < jsonInterpolator.size(); j++) {
      Node keyFrame = new Node(this);
      pruneBranch(keyFrame);
      keyFrame.setWorldMatrix(_toFrame(jsonInterpolator.getJSONObject(j)));
      keyFrame.setPrecision(Node.Precision.FIXED);
      keyFrame.setPrecisionThreshold(20);
      interpolator.addKeyFrame(keyFrame, jsonInterpolator.getJSONObject(j).getFloat("time"));
      /*
      if (pathsVisualHint())
        inputHandler().addGrabber(keyFrame);
      if (!eye().keyFrameInterpolatorMap().containsKey(_id))
        eye().setKeyFrameInterpolator(_id, new Interpolator(this, eyeFrame()));
      eye().keyFrameInterpolator(_id).addKeyFrame(keyFrame, keyFrames.getJSONObject(j).getFloat("time"));
      //*/
    }
    return interpolator;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts the {@code interpolator}
   * to a P5 JSONArray.
   */
  protected JSONArray _toJSONArray(Interpolator interpolator) {
    JSONArray jsonKeyFrames = new JSONArray();
    for (int i = 0; i < interpolator.size(); i++) {
      JSONObject jsonKeyFrame = _toJSONObject(interpolator.keyFrame(i));
      jsonKeyFrame.setFloat("time", interpolator.time(i));
      jsonKeyFrames.setJSONObject(i, jsonKeyFrame);
    }
    return jsonKeyFrames;
  }

  /**
   * Used internally by {@link #loadConfig(String)}. Converts the P5 JSONObject into a frame.
   */
  protected Frame _toFrame(JSONObject jsonFrame) {
    Frame frame = new Frame();
    float x, y, z, w;
    x = jsonFrame.getJSONArray("position").getFloat(0);
    y = jsonFrame.getJSONArray("position").getFloat(1);
    z = jsonFrame.getJSONArray("position").getFloat(2);
    frame.setPosition(new Vector(x, y, z));
    x = jsonFrame.getJSONArray("orientation").getFloat(0);
    y = jsonFrame.getJSONArray("orientation").getFloat(1);
    z = jsonFrame.getJSONArray("orientation").getFloat(2);
    w = jsonFrame.getJSONArray("orientation").getFloat(3);
    frame.setOrientation(new Quaternion(x, y, z, w));
    frame.setMagnitude(jsonFrame.getFloat("magnitude"));
    return frame;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code frame} into a P5
   * JSONObject.
   */
  protected JSONObject _toJSONObject(Frame frame) {
    JSONObject jsonFrame = new JSONObject();
    jsonFrame.setFloat("magnitude", frame.magnitude());
    jsonFrame.setJSONArray("position", _toJSONArray(frame.position()));
    jsonFrame.setJSONArray("orientation", _toJSONArray(frame.orientation()));
    return jsonFrame;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code vector} into a P5
   * JSONArray.
   */
  protected JSONArray _toJSONArray(Vector vector) {
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
  protected JSONArray _toJSONArray(Quaternion quaternion) {
    JSONArray jsonRot = new JSONArray();
    jsonRot.setFloat(0, quaternion.x());
    jsonRot.setFloat(1, quaternion.y());
    jsonRot.setFloat(2, quaternion.z());
    jsonRot.setFloat(3, quaternion.w());
    return jsonRot;
  }

  /**
   * Same as {@code traverse(frontBuffer())}.
   *
   * @see #frontBuffer()
   * @see #traverse(PGraphics)
   */
  @Override
  public void traverse() {
    traverse(frontBuffer());
  }

  /**
   * Draw all {@link #nodes()} into the given pGraphics. No
   * {@code pGraphics.beginDraw()/endDraw()} calls take place. This method allows shader
   * chaining.
   * <p>
   * Note that {@code drawNodes(backBuffer())} (which enables 'picking' of the nodes
   * using a <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a>
   * technique is called by {@link #postDraw()}.
   * <p>
   * <b>Attention:</b> this method should be called after {@link #_bind(PGraphics)}
   * (i.e., manual eye update) and before any other transformation of the modelview takes
   * place.
   *
   * @param pGraphics
   * @see #nodes()
   * @see #traverse()
   */
  public void traverse(PGraphics pGraphics) {
    _targetPGraphics = pGraphics;
    if (pGraphics != frontBuffer())
      _bind(pGraphics);
    super.traverse();
  }

  @Override
  protected void _visit(Node node) {
    _targetPGraphics.pushMatrix();
    applyTransformation(_targetPGraphics, node);
    node.visit();
    if (!node.isCulled())
      for (Node child : node.children())
        _visit(child);
    _targetPGraphics.popMatrix();
  }

  /**
   * Returns a new matrix helper for the given {@code pGraphics}. Rarely needed.
   * <p>
   * Note that the current graph matrix helper may be retrieved by {@link #matrixHandler()}.
   *
   * @see #matrixHandler()
   * @see #setMatrixHandler(MatrixHandler)
   * @see #applyWorldTransformation(PGraphics, Frame)
   */
  public MatrixHandler matrixHandler(PGraphics pGraphics) {
    return (pGraphics instanceof processing.opengl.PGraphicsOpenGL) ?
        new GLMatrixHandler(this, (PGraphicsOpenGL) pGraphics) :
        new Java2DMatrixHandler(this, pGraphics);
  }

  /**
   * Sets the {@code pGraphics} matrices by calling
   * {@link MatrixHandler#bindProjection(Matrix)} and
   * {@link MatrixHandler#bindModelView(Matrix)} (only makes sense
   * when {@link #frontBuffer()} is different than {@code pGraphics}).
   * <p>
   * This method doesn't perform any computation, but simple retrieve the current matrices
   * whose actual computation has been updated in {@link #preDraw()}.
   */
  protected void _bind(PGraphics pGraphics) {
    if (this.frontBuffer() == pGraphics)
      return;
    MatrixHandler matrixHandler = matrixHandler(pGraphics);
    matrixHandler.bindProjection(projection());
    //matrixHandler.bindView(matrixHandler().cacheView());
    matrixHandler.bindModelView(matrixHandler().cacheView());
  }

  /**
   * Apply the local transformation defined by the given {@code frame} on the given
   * {@code pGraphics}. This method doesn't call {@link #_bind(PGraphics)} which
   * should be called manually (only makes sense when {@link #frontBuffer()} is different than
   * {@code pGraphics}). Needed by {@link #applyWorldTransformation(PGraphics, Frame)}.
   *
   * @see #applyWorldTransformation(PGraphics, Frame)
   * @see #_bind(PGraphics)
   */
  public static void applyTransformation(PGraphics pGraphics, Frame frame) {
    if (pGraphics instanceof PGraphics3D) {
      pGraphics.translate(frame.translation()._vector[0], frame.translation()._vector[1], frame.translation()._vector[2]);
      pGraphics.rotate(frame.rotation().angle(), frame.rotation().axis()._vector[0],
          frame.rotation().axis()._vector[1], frame.rotation().axis()._vector[2]);
      pGraphics.scale(frame.scaling(), frame.scaling(), frame.scaling());
    } else {
      pGraphics.translate(frame.translation().x(), frame.translation().y());
      pGraphics.rotate(frame.rotation().angle2D());
      pGraphics.scale(frame.scaling(), frame.scaling());
    }
  }

  /**
   * Apply the global transformation defined by the given {@code frame} on the given
   * {@code pGraphics}. This method doesn't call {@link #_bind(PGraphics)} which
   * should be called manually (only makes sense when {@link #frontBuffer()} is different than
   * {@code pGraphics}).
   *
   * @see #applyTransformation(PGraphics, Frame)
   * @see #_bind(PGraphics)
   */
  public static void applyWorldTransformation(PGraphics pGraphics, Frame frame) {
    Frame reference = frame.reference();
    if (reference != null) {
      applyWorldTransformation(pGraphics, reference);
      applyTransformation(pGraphics, frame);
    } else {
      applyTransformation(pGraphics, frame);
    }
  }

  // SCREENDRAWING

  /**
   * Need to override it because of this issue: https://github.com/remixlab/proscene/issues/1
   */
  @Override
  public void beginScreenCoordinates() {
    beginScreenCoordinates(frontBuffer());
  }

  /**
   * Begins screen drawing on pGraphics using the {@link #eye()} parameters. Don't forget
   * to call {@link #endScreenCoordinates(PGraphics)} after screen drawing ends.
   *
   * @see #endScreenCoordinates(PGraphics)
   * @see #beginScreenCoordinates()
   */
  public void beginScreenCoordinates(PGraphics pGraphics) {
    if (_startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenCoordinates() call followed by a "
          + "endScreenCoordinates() and they cannot be nested. Check your implementation!");
    _startCoordCalls++;
    pGraphics.hint(PApplet.DISABLE_OPTIMIZED_STROKE);// -> new line not present in Graph.bS
    disableDepthTest(pGraphics);
    // if-else same as:
    // matrixHandler(p).beginScreenCoordinates();
    // but perhaps a bit more efficient
    if (pGraphics == frontBuffer())
      matrixHandler().beginScreenCoordinates();
    else
      matrixHandler(pGraphics).beginScreenCoordinates();
  }

  /**
   * Need to override it because of this issue: https://github.com/remixlab/proscene/issues/1
   */
  @Override
  public void endScreenCoordinates() {
    endScreenCoordinates(frontBuffer());
  }

  /**
   * Ends screen drawing on the pGraphics instance using {@link #eye()}
   * parameters. The screen drawing should happen between
   * {@link #beginScreenCoordinates(PGraphics)} and this method.
   *
   * @see #beginScreenCoordinates(PGraphics)
   * @see #endScreenCoordinates()
   */
  public void endScreenCoordinates(PGraphics pGraphics) {
    _startCoordCalls--;
    if (_startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenCoordinates() call followed by a "
          + "endScreenCoordinates() and they cannot be nested. Check your implementation!");
    // if-else same as:
    // matrixHandler(p).endScreenCoordinates();
    // but perhaps a bit more efficient
    if (pGraphics == frontBuffer())
      matrixHandler().endScreenCoordinates();
    else
      matrixHandler(pGraphics).endScreenCoordinates();
    enableDepthTest(pGraphics);
    pGraphics.hint(PApplet.ENABLE_OPTIMIZED_STROKE);// -> new line not present in Graph.eS
  }

  // drawing

  /**
   * Same as {@code vertex(frontBuffer(), v)}.
   *
   * @see #vertex(PGraphics, float[])
   */
  public void vertex(float[] v) {
    vertex(frontBuffer(), v);
  }

  /**
   * Wrapper for PGraphics.vertex(v)
   */
  public static void vertex(PGraphics pGraphics, float[] v) {
    pGraphics.vertex(v);
  }

  /**
   * Same as {@code if (this.is2D()) vertex(frontBuffer(), x, y); else vertex(frontBuffer(), x, y, z)}.
   *
   * @see #vertex(PGraphics, float, float, float)
   */
  public void vertex(float x, float y, float z) {
    if (is2D())
      vertex(frontBuffer(), x, y);
    else
      vertex(frontBuffer(), x, y, z);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,z)
   */
  public static void vertex(PGraphics pGraphics, float x, float y, float z) {
    if (pGraphics instanceof PGraphics3D)
      pGraphics.vertex(x, y, z);
    else
      pGraphics.vertex(x, y);
  }

  /**
   * Same as {@code if (this.is2D()) vertex(frontBuffer(), x, y, u, v); else
   * vertex(frontBuffer(), x, y, z, u, v);}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   * @see #vertex(PGraphics, float, float, float, float, float)
   */
  public void vertex(float x, float y, float z, float u, float v) {
    if (is2D())
      vertex(frontBuffer(), x, y, u, v);
    else
      vertex(frontBuffer(), x, y, z, u, v);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,z,u,v)
   */
  public static void vertex(PGraphics pGraphics, float x, float y, float z, float u, float v) {
    if (pGraphics instanceof PGraphics3D)
      pGraphics.vertex(x, y, z, u, v);
    else
      pGraphics.vertex(x, y, u, v);
  }

  /**
   * Same as {@code vertex(frontBuffer(), x, y)}.
   *
   * @see #vertex(PGraphics, float, float)
   */
  public void vertex(float x, float y) {
    vertex(frontBuffer(), x, y);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y)
   */
  public static void vertex(PGraphics pGraphics, float x, float y) {
    pGraphics.vertex(x, y);
  }

  /**
   * Same as {@code vertex(frontBuffer(), x, y, u, v)}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   */
  public void vertex(float x, float y, float u, float v) {
    vertex(frontBuffer(), x, y, u, v);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,u,v)
   */
  public static void vertex(PGraphics pGraphics, float x, float y, float u, float v) {
    pGraphics.vertex(x, y, u, v);
  }

  /**
   * Same as {@code if (this.is2D()) line(frontBuffer(), x1, y1, x2, y2);
   * else line(frontBuffer(), x1, y1, z1, x2, y2, z2);}.
   *
   * @see #line(PGraphics, float, float, float, float, float, float)
   */
  public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
    if (is2D())
      line(frontBuffer(), x1, y1, x2, y2);
    else
      line(frontBuffer(), x1, y1, z1, x2, y2, z2);
  }

  /**
   * Wrapper for PGraphics.line(x1, y1, z1, x2, y2, z2).
   */
  public static void line(PGraphics pGraphics, float x1, float y1, float z1, float x2, float y2, float z2) {
    if (pGraphics instanceof PGraphics3D)
      pGraphics.line(x1, y1, z1, x2, y2, z2);
    else
      pGraphics.line(x1, y1, x2, y2);
  }

  /**
   * Same as {@code frontBuffer().line(x1, y1, x2, y2)}.
   *
   * @see #line(PGraphics, float, float, float, float)
   */
  public void line(float x1, float y1, float x2, float y2) {
    line(frontBuffer(), x1, y1, x2, y2);
  }

  /**
   * Wrapper for PGraphics.line(x1, y1, x2, y2).
   */
  public static void line(PGraphics pGraphics, float x1, float y1, float x2, float y2) {
    pGraphics.line(x1, y1, x2, y2);
  }

  /**
   * Converts a {@link Vector} to a PVector.
   */
  public static PVector toPVector(Vector vector) {
    return new PVector(vector.x(), vector.y(), vector.z());
  }

  /**
   * Converts a PVector to a {@link Vector}.
   */
  public static Vector toVec(PVector pVector) {
    return new Vector(pVector.x, pVector.y, pVector.z);
  }

  /**
   * Converts a {@link Matrix} to a PMatrix3D.
   */
  public static PMatrix3D toPMatrix(Matrix matrix) {
    float[] a = matrix.getTransposed(new float[16]);
    return new PMatrix3D(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14],
        a[15]);
  }

  /**
   * Converts a PMatrix3D to a {@link Matrix}.
   */
  public static Matrix toMat(PMatrix3D pMatrix3D) {
    return new Matrix(pMatrix3D.get(new float[16]), true);
  }

  /**
   * Converts a PMatrix2D to a {@link Matrix}.
   */
  public static Matrix toMat(PMatrix2D pMatrix2D) {
    return toMat(new PMatrix3D(pMatrix2D));
  }

  /**
   * Converts a {@link Matrix} to a PMatrix2D.
   */
  public static PMatrix2D toPMatrix2D(Matrix matrix) {
    float[] a = matrix.getTransposed(new float[16]);
    return new PMatrix2D(a[0], a[1], a[3], a[4], a[5], a[7]);
  }

  // DRAWING

  /**
   * Convenience function that simply calls {@code drawPath(kfi, 1, 6, 100)}.
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator) {
    drawPath(interpolator, 1, 6, radius());
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, mask, 6, radius())}
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator, int mask) {
    drawPath(interpolator, mask, 6, radius());
  }

  /**
   * Convenience function that simply calls {@code drawPath(kfi, mask, nbFrames, * 100)}
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator, int mask, int frameCount) {
    drawPath(interpolator, mask, frameCount, radius());
  }

  /**
   * Draws the path used to interpolate the {@link Interpolator#frame()}
   * <p>
   * {@code mask} controls what is drawn: If ( (mask &amp; 1) != 0 ), the position path is
   * drawn. If ( (mask &amp; 2) != 0 ), an eye representation is regularly drawn and if
   * ( (mask &amp; 4) != 0 ), oriented axes are regularly drawn. Examples:
   * <p>
   * {@code drawPath(1); // Simply draws the interpolation path} <br>
   * {@code drawPath(3); // Draws path and eyes} <br>
   * {@code drawPath(5); // Draws path and axes} <br>
   * <p>
   * In the case where the eye or axes are drawn, {@code frameCount} controls the number of
   * objects (axes or eyes) drawn between two successive key-frames. When
   * {@code frameCount = 1}, only the key-frames are drawn, when {@code frameCount = 2} it
   * also draws the intermediate orientation, etc. The maximum value is 30, so that an object
   * is drawn for each key-frame. Default value is 6.
   * <p>
   * {@code scale} controls the scaling of the eye and axes drawing. A value of
   * {@link #radius()} should give good results.
   */
  public void drawPath(Interpolator interpolator, int mask, int frameCount, float scale) {
    frontBuffer().pushStyle();
    if (mask != 0) {
      int nbSteps = 30;
      frontBuffer().strokeWeight(2 * frontBuffer().strokeWeight);
      frontBuffer().noFill();
      List<Frame> path = interpolator.path();
      if (((mask & 1) != 0) && path.size() > 1) {
        frontBuffer().beginShape();
        for (Frame myFr : path)
          vertex(myFr.position().x(), myFr.position().y(), myFr.position().z());
        frontBuffer().endShape();
      }
      if ((mask & 6) != 0) {
        int count = 0;
        if (frameCount > nbSteps)
          frameCount = nbSteps;
        float goal = 0.0f;

        for (Frame myFr : path)
          if ((count++) >= goal) {
            goal += nbSteps / (float) frameCount;
            pushModelView();

            applyTransformation(myFr);

            if ((mask & 2) != 0)
              _drawEye(scale);
            if ((mask & 4) != 0)
              drawAxes(scale / 10.0f);

            popModelView();
          }
      }
      frontBuffer().strokeWeight(frontBuffer().strokeWeight / 2f);
    }
    // draw the picking targets:
    for (Frame frame : interpolator.keyFrames())
      if (frame instanceof Node)
        drawPickingTarget((Node) frame);
    frontBuffer().popStyle();
  }

  /**
   * Internal use.
   */
  protected void _drawEye(float scale) {
    frontBuffer().pushStyle();
    float halfHeight = scale * (is2D() ? 1.2f : 0.07f);
    float halfWidth = halfHeight * 1.3f;
    float dist = halfHeight / (float) Math.tan(PApplet.PI / 8.0f);

    float arrowHeight = 1.5f * halfHeight;
    float baseHeight = 1.2f * halfHeight;
    float arrowHalfWidth = 0.5f * halfWidth;
    float baseHalfWidth = 0.3f * halfWidth;

    // Frustum outline
    frontBuffer().noFill();
    frontBuffer().beginShape();
    vertex(-halfWidth, halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(halfWidth, -halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    frontBuffer().endShape();
    frontBuffer().noFill();
    frontBuffer().beginShape();
    vertex(halfWidth, -halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(-halfWidth, halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    frontBuffer().endShape();

    // Up arrow
    frontBuffer().noStroke();
    frontBuffer().fill(frontBuffer().strokeColor);
    // Base
    frontBuffer().beginShape(PApplet.QUADS);

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

    frontBuffer().endShape();
    // Arrow
    frontBuffer().beginShape(PApplet.TRIANGLES);

    if (isLeftHanded()) {
      vertex(0.0f, -arrowHeight, -dist);
      vertex(arrowHalfWidth, -baseHeight, -dist);
      vertex(-arrowHalfWidth, -baseHeight, -dist);
    } else {
      vertex(0.0f, arrowHeight, -dist);
      vertex(-arrowHalfWidth, baseHeight, -dist);
      vertex(arrowHalfWidth, baseHeight, -dist);
    }
    frontBuffer().endShape();
    frontBuffer().popStyle();
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
   * Draws an arrow of {@code radius} and {@code length} along the positive Z axis.
   * <p>
   * Use {@link #drawArrow(Vector, Vector, float)} to place the arrow in 3D.
   */
  public void drawArrow(float length, float radius) {
    float head = 2.5f * (radius / length) + 0.1f;
    float coneRadiusCoef = 4.0f - 5.0f * head;

    drawCylinder(radius, length * (1.0f - head / coneRadiusCoef));
    matrixHandler().translate(0.0f, 0.0f, length * (1.0f - head));
    drawCone(coneRadiusCoef * radius, head * length);
    matrixHandler().translate(0.0f, 0.0f, -length * (1.0f - head));
  }

  /**
   * Draws an arrow of {@code radius} between {@code from} and the 3D point {@code to}.
   *
   * @see #drawArrow(float, float)
   */
  public void drawArrow(Vector from, Vector to, float radius) {
    pushModelView();
    matrixHandler().translate(from.x(), from.y(), from.z());
    applyModelView(new Quaternion(new Vector(0, 0, 1), Vector.subtract(to, from)).matrix());
    drawArrow(Vector.subtract(to, from).magnitude(), radius);
    popModelView();
  }

  /**
   * Same as {@code drawCylinder(20, radius, height)}.
   *
   * @see #drawCylinder(int, float, float)
   */
  public void drawCylinder(float radius, float height) {
    drawCylinder(20, radius, height);
  }

  /**
   * Same as {@code drawCylinder(frontBuffer(), detail, radius, height)}.
   *
   * @see #drawCylinder(PGraphics, int, float, float)
   */
  public void drawCylinder(int detail, float radius, float height) {
    drawCylinder(frontBuffer(), detail, radius, height);
  }

  /**
   * Same as {@code drawCylinder(frontBuffer, radius()/6, radius()/3)}.
   *
   * @see #drawCylinder(PGraphics, float, float)
   */
  public void drawCylinder(PGraphics pGraphics) {
    drawCylinder(pGraphics, radius() / 6, radius() / 3);
  }

  /**
   * Same as {@code drawCylinder(pGraphics, 20, radius, height)}.
   *
   * @see #drawCylinder(PGraphics, int, float, float)
   */
  public static void drawCylinder(PGraphics pGraphics, float radius, float height) {
    drawCylinder(pGraphics, 20, radius, height);
  }

  /**
   * Draws a cylinder of {@code radius} and {@code height} onto {@code pGraphics}.
   */
  public static void drawCylinder(PGraphics pGraphics, int detail, float radius, float height) {
    if (!(pGraphics instanceof PGraphics3D))
      return;
    pGraphics.pushStyle();
    float px, py;

    float degrees = 360 / detail;

    pGraphics.beginShape(PApplet.QUAD_STRIP);
    for (float i = 0; i < detail + 1; i++) {
      px = (float) Math.cos(PApplet.radians(i * degrees)) * radius;
      py = (float) Math.sin(PApplet.radians(i * degrees)) * radius;
      vertex(pGraphics, px, py, 0);
      vertex(pGraphics, px, py, height);
    }
    pGraphics.endShape();

    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pGraphics, 0, 0, 0);
    for (float i = detail; i > -1; i--) {
      px = (float) Math.cos(PApplet.radians(i * degrees)) * radius;
      py = (float) Math.sin(PApplet.radians(i * degrees)) * radius;
      vertex(pGraphics, px, py, 0);
    }
    pGraphics.endShape();

    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pGraphics, 0, 0, height);
    for (float i = 0; i < detail + 1; i++) {
      px = (float) Math.cos(PApplet.radians(i * degrees)) * radius;
      py = (float) Math.sin(PApplet.radians(i * degrees)) * radius;
      vertex(pGraphics, px, py, height);
    }
    pGraphics.endShape();
    pGraphics.popStyle();
  }

  /**
   * Same as {@code drawHollowCylinder(frontBuffer(), radius, height, normal1, normal2)}.
   *
   * @see #drawHollowCylinder(PGraphics, float, float, Vector, Vector)
   */
  public void drawHollowCylinder(float radius, float height, Vector normal1, Vector normal2) {
    drawHollowCylinder(frontBuffer(), radius, height, normal1, normal2);
  }

  /**
   * Same as {@code drawHollowCylinder(frontBuffer(), detail, radius, height, normal1, normal2)}.
   *
   * @see #drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)
   * @see #drawCylinder(float, float)
   */
  public void drawHollowCylinder(int detail, float radius, float height, Vector normal1, Vector normal2) {
    drawHollowCylinder(frontBuffer(), detail, radius, height, normal1, normal2);
  }

  /**
   * Same as {@code drawHollowCylinder(pGraphics, 30, radius, height, normal1, normal2)}.
   *
   * @see #drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)
   */
  public static void drawHollowCylinder(PGraphics pGraphics, float radius, float height, Vector normal1, Vector normal2) {
    drawHollowCylinder(pGraphics, 30, radius, height, normal1, normal2);
  }

  /**
   * Draws a hollow cylinder onto {@code pGraphics} whose bases are formed by two cutting
   * planes ({@code normal1} and {@code normal2}), along the positive {@code z} axis.
   *
   * @param detail
   * @param radius  radius of the hollow cylinder
   * @param height  height of the hollow cylinder
   * @param normal1 normal of the plane that intersects the cylinder at z=0
   * @param normal2 normal of the plane that intersects the cylinder at z=height
   * @see #drawCylinder(float, float)
   */
  public static void drawHollowCylinder(PGraphics pGraphics, int detail, float radius, float height, Vector normal1, Vector normal2) {
    if (!(pGraphics instanceof PGraphics3D))
      return;
    pGraphics.pushStyle();
    // eqs taken from: http://en.wikipedia.org/wiki/Line-plane_intersection
    Vector pm0 = new Vector(0, 0, 0);
    Vector pn0 = new Vector(0, 0, height);
    Vector l0 = new Vector();
    Vector l = new Vector(0, 0, 1);
    Vector p = new Vector();
    float x, y, d;

    pGraphics.noStroke();
    pGraphics.beginShape(PApplet.QUAD_STRIP);

    for (float t = 0; t <= detail; t++) {
      x = radius * PApplet.cos(t * PApplet.TWO_PI / detail);
      y = radius * PApplet.sin(t * PApplet.TWO_PI / detail);
      l0.set(x, y, 0);

      d = (normal1.dot(Vector.subtract(pm0, l0))) / (l.dot(normal1));
      p = Vector.add(Vector.multiply(l, d), l0);
      vertex(pGraphics, p.x(), p.y(), p.z());

      l0.setZ(height);
      d = (normal2.dot(Vector.subtract(pn0, l0))) / (l.dot(normal2));
      p = Vector.add(Vector.multiply(l, d), l0);
      vertex(pGraphics, p.x(), p.y(), p.z());
    }
    pGraphics.endShape();
    pGraphics.popStyle();
  }

  // Cone v1

  /**
   * Same as {@code drawCone(detail, 0, 0, radius, height)}
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(int detail, float radius, float height) {
    drawCone(detail, 0, 0, radius, height);
  }

  /**
   * Same as {@code drawCone(12, 0, 0, radius, height)}
   *
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(float radius, float height) {
    drawCone(12, 0, 0, radius, height);
  }


  /**
   * Same as {@code drawCone(frontBuffer(), detail, x, y, radius, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float radius, float height) {
    drawCone(frontBuffer(), detail, x, y, radius, height);
  }

  /**
   * Same as {@code drawCone(pGraphics, detail, 0, 0, radius, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public static void drawCone(PGraphics pGraphics, int detail, float radius, float height) {
    drawCone(pGraphics, detail, 0, 0, radius, height);
  }

  /**
   * Same as {@code drawCone(pGraphics, 12, 0, 0, radius, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public static void drawCone(PGraphics pGraphics, float radius, float height) {
    drawCone(pGraphics, 12, 0, 0, radius, height);
  }

  /**
   * Same as {@code drawCone(pGraphics, 12, 0, 0, radius()/4, sqrt(3) * radius()/4)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public void drawCone(PGraphics pGraphics) {
    float radius = radius() / 4;
    drawCone(pGraphics, 12, 0, 0, radius, (float) Math.sqrt((float) 3) * radius);
  }

  /**
   * Draws a cone onto {@code pGraphics} centered at {@code (x,y)} having
   * {@code radius} and {@code height} dimensions.
   */
  public static void drawCone(PGraphics pGraphics, int detail, float x, float y, float radius, float height) {
    if (!(pGraphics instanceof PGraphics3D))
      return;
    pGraphics.pushStyle();
    float unitConeX[] = new float[detail + 1];
    float unitConeY[] = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float a1 = PApplet.TWO_PI * i / detail;
      unitConeX[i] = radius * (float) Math.cos(a1);
      unitConeY[i] = radius * (float) Math.sin(a1);
    }

    pGraphics.pushMatrix();
    pGraphics.translate(x, y);
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pGraphics, 0, 0, height);
    for (int i = 0; i <= detail; i++) {
      vertex(pGraphics, unitConeX[i], unitConeY[i], 0.0f);
    }
    pGraphics.endShape();
    pGraphics.popMatrix();
    pGraphics.popStyle();
  }

  // Cone v2

  /**
   * Same as {@code drawCone(detail, 0, 0, radius1, radius2, height)}.
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public void drawCone(int detail, float radius1, float radius2, float height) {
    drawCone(detail, 0, 0, radius1, radius2, height);
  }

  /**
   * Same as {@code drawCone(18, 0, 0, radius1, radius2, height)}.
   *
   * @see #drawCone(int, float, float, float, float, float)
   */
  public void drawCone(float radius1, float radius2, float height) {
    drawCone(18, 0, 0, radius1, radius2, height);
  }

  /**
   * Same as {@code drawCone(pGraphics, detail, 0, 0, radius1, radius2, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   */
  public static void drawCone(PGraphics pGraphics, int detail, float radius1, float radius2, float height) {
    drawCone(pGraphics, detail, 0, 0, radius1, radius2, height);
  }

  /**
   * Same as {@code drawCone(pGraphics, 18, 0, 0, radius1, radius2, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   */
  public static void drawCone(PGraphics pGraphics, float radius1, float radius2, float height) {
    drawCone(pGraphics, 18, 0, 0, radius1, radius2, height);
  }

  /**
   * Same as {@code drawCone(frontBuffer(), detail, x, y, radius1, radius2, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float radius1, float radius2, float height) {
    drawCone(frontBuffer(), detail, x, y, radius1, radius2, height);
  }

  /**
   * Draws a truncated cone onto {@code pGraphics} along the positive {@code z} axis,
   * with its base centered at {@code (x,y)}, {@code height}, and radii {@code radius1}
   * and {@code radius2} (basis and height respectively).
   */
  public static void drawCone(PGraphics pGraphics, int detail, float x, float y, float radius1, float radius2, float height) {
    if (!(pGraphics instanceof PGraphics3D))
      return;
    pGraphics.pushStyle();
    float firstCircleX[] = new float[detail + 1];
    float firstCircleY[] = new float[detail + 1];
    float secondCircleX[] = new float[detail + 1];
    float secondCircleY[] = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float a1 = PApplet.TWO_PI * i / detail;
      firstCircleX[i] = radius1 * (float) Math.cos(a1);
      firstCircleY[i] = radius1 * (float) Math.sin(a1);
      secondCircleX[i] = radius2 * (float) Math.cos(a1);
      secondCircleY[i] = radius2 * (float) Math.sin(a1);
    }

    pGraphics.pushMatrix();
    pGraphics.translate(x, y);
    pGraphics.beginShape(PApplet.QUAD_STRIP);
    for (int i = 0; i <= detail; i++) {
      vertex(pGraphics, firstCircleX[i], firstCircleY[i], 0);
      vertex(pGraphics, secondCircleX[i], secondCircleY[i], height);
    }
    pGraphics.endShape();
    pGraphics.popMatrix();
    pGraphics.popStyle();
  }

  /**
   * Convenience function that simply calls {@code drawAxes(100)}.
   *
   * @see #drawAxes(float)
   */
  public void drawAxes() {
    drawAxes(100);
  }

  /**
   * Same as {@code drawAxes(frontBuffer(), length)}.
   *
   * @see #drawAxes(PGraphics, float)
   * @see #drawGrid(float, int)
   */
  public void drawAxes(float length) {
    drawAxes(frontBuffer(), length);
  }

  /**
   * Same as {@code drawAxes(pGraphics, radius() / 5)}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public void drawAxes(PGraphics pGraphics) {
    drawAxes(pGraphics, radius() / 5);
  }

  /**
   * Draws axes of {@code length} onto {@code pGraphics}.
   */
  public void drawAxes(PGraphics pGraphics, float length) {
    pGraphics.pushStyle();
    pGraphics.colorMode(PApplet.RGB, 255);
    float charWidth = length / 40.0f;
    float charHeight = length / 30.0f;
    float charShift = 1.04f * length;

    pGraphics.pushStyle();
    pGraphics.beginShape(PApplet.LINES);
    pGraphics.strokeWeight(2);
    if (is2D()) {
      // The X
      pGraphics.stroke(200, 0, 0);
      pGraphics.vertex(charShift + charWidth, -charHeight);
      pGraphics.vertex(charShift - charWidth, charHeight);
      pGraphics.vertex(charShift - charWidth, -charHeight);
      pGraphics.vertex(charShift + charWidth, charHeight);

      // The Y
      charShift *= 1.02;
      pGraphics.stroke(0, 200, 0);
      pGraphics.vertex(charWidth, charShift + (isRightHanded() ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(-charWidth, charShift + (isRightHanded() ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(0.0f, charShift + -(isRightHanded() ? charHeight : -charHeight));
    } else {
      // The X
      pGraphics.stroke(200, 0, 0);
      pGraphics.vertex(charShift, charWidth, -charHeight);
      pGraphics.vertex(charShift, -charWidth, charHeight);
      pGraphics.vertex(charShift, -charWidth, -charHeight);
      pGraphics.vertex(charShift, charWidth, charHeight);
      // The Y
      pGraphics.stroke(0, 200, 0);
      pGraphics.vertex(charWidth, charShift, (isLeftHanded() ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(-charWidth, charShift, (isLeftHanded() ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(0.0f, charShift, -(isLeftHanded() ? charHeight : -charHeight));
      // The Z
      pGraphics.stroke(0, 100, 200);
      pGraphics.vertex(-charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pGraphics.vertex(charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pGraphics.vertex(charWidth, isRightHanded() ? charHeight : -charHeight, charShift);
      pGraphics.vertex(-charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
      pGraphics.vertex(-charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
      pGraphics.vertex(charWidth, isRightHanded() ? -charHeight : charHeight, charShift);
    }
    pGraphics.endShape();
    pGraphics.popStyle();

    // X Axis
    pGraphics.stroke(200, 0, 0);
    if (is2D())
      pGraphics.line(0, 0, length, 0);
    else
      pGraphics.line(0, 0, 0, length, 0, 0);
    // Y Axis
    pGraphics.stroke(0, 200, 0);
    if (is2D())
      pGraphics.line(0, 0, 0, length);
    else
      pGraphics.line(0, 0, 0, 0, length, 0);

    // Z Axis
    if (is3D()) {
      pGraphics.stroke(0, 100, 200);
      pGraphics.line(0, 0, 0, 0, 0, length);
    }
    pGraphics.popStyle();
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
   * Convenience function that simply calls {@code drawGrid(size, 10)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid(float size) {
    drawGrid(size, 10);
  }

  /**
   * Convenience function that simply calls {@code drawGrid(100, subdivisions)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid(int subdivisions) {
    drawGrid(100, subdivisions);
  }

  /**
   * Same as {@code drawGrid(frontBuffer(), size, subdivisions)}.
   *
   * @see #drawGrid(PGraphics, float, int)
   * @see #drawAxes(float)
   */
  public void drawGrid(float size, int subdivisions) {
    drawGrid(frontBuffer(), size, subdivisions);
  }

  /**
   * Same as {@code drawGrid(frontBuffer, radius()/4, 10)}.
   */
  public void drawGrid(PGraphics pGraphics) {
    drawGrid(pGraphics, radius() / 4, 10);
  }

  /**
   * Draws a grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public void drawGrid(PGraphics pGraphics, float size, int subdivisions) {
    pGraphics.pushStyle();
    pGraphics.beginShape(LINES);
    for (int i = 0; i <= subdivisions; ++i) {
      float pos = size * (2.0f * i / subdivisions - 1.0f);
      vertex(pGraphics, pos, -size);
      vertex(pGraphics, pos, +size);
      vertex(pGraphics, -size, pos);
      vertex(pGraphics, size, pos);
    }
    pGraphics.endShape();
    pGraphics.popStyle();
  }

  /**
   * Convenience function that simply calls {@code drawDottedGrid(100, 10)}.
   *
   * @see #drawDottedGrid(float, int)
   */
  public void drawDottedGrid() {
    drawDottedGrid(100, 10);
  }

  /**
   * Convenience function that simply calls {@code drawDottedGrid(size, 10)}.
   *
   * @see #drawDottedGrid(float, int)
   */
  public void drawDottedGrid(float size) {
    drawDottedGrid(size, 10);
  }

  /**
   * Convenience function that simplt calls {@code drawDottedGrid(100, subdivisions)}.
   *
   * @see #drawDottedGrid(float, int)
   */
  public void drawDottedGrid(int subdivisions) {
    drawDottedGrid(100, subdivisions);
  }

  /**
   * Same as {@code drawDottedGrid(frontBuffer(), size, subdivisions)}.
   *
   * @see #drawDottedGrid(PGraphics, float, int)
   */
  public void drawDottedGrid(float size, int subdivisions) {
    drawDottedGrid(frontBuffer(), size, subdivisions);
  }

  /**
   * Same as {@code drawDottedGrid(pGraphics, radius() / 4, 10)}.
   *
   * @see #drawDottedGrid(PGraphics, float, int)
   */
  public void drawDottedGrid(PGraphics pGraphics) {
    drawDottedGrid(pGraphics, radius() / 4, 10);
  }

  /**
   * Draws a dotted-grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public void drawDottedGrid(PGraphics pGraphics, float size, int subdivisions) {
    pGraphics.pushStyle();
    float posi, posj;
    pGraphics.beginShape(POINTS);
    for (int i = 0; i <= subdivisions; ++i) {
      posi = size * (2.0f * i / subdivisions - 1.0f);
      for (int j = 0; j <= subdivisions; ++j) {
        posj = size * (2.0f * j / subdivisions - 1.0f);
        vertex(pGraphics, posi, posj);
      }
    }
    pGraphics.endShape();
    int internalSub = 5;
    int subSubdivisions = subdivisions * internalSub;
    float currentWeight = pGraphics.strokeWeight;
    pGraphics.colorMode(HSB, 255);
    float hue = pGraphics.hue(pGraphics.strokeColor);
    float saturation = pGraphics.saturation(pGraphics.strokeColor);
    float brightness = pGraphics.brightness(pGraphics.strokeColor);
    pGraphics.stroke(hue, saturation, brightness * 10f / 17f);
    pGraphics.strokeWeight(currentWeight / 2);
    pGraphics.beginShape(POINTS);
    for (int i = 0; i <= subSubdivisions; ++i) {
      posi = size * (2.0f * i / subSubdivisions - 1.0f);
      for (int j = 0; j <= subSubdivisions; ++j) {
        posj = size * (2.0f * j / subSubdivisions - 1.0f);
        if (((i % internalSub) != 0) || ((j % internalSub) != 0))
          vertex(pGraphics, posi, posj);
      }
    }
    pGraphics.endShape();
    pGraphics.popStyle();
  }

  public void drawEye(Graph graph) {
    drawEye(graph, false);
  }

  /**
   * Applies the {@code graph.eye()} transformation and then calls
   * {@link #drawEye(PGraphics, Graph, boolean)} on the scene {@link #frontBuffer()}. If
   * {@code texture} draws the projected scene on the near plane.
   *
   * @see #applyTransformation(Frame)
   * @see #drawEye(PGraphics, Graph, boolean)
   */
  public void drawEye(Graph graph, boolean texture) {
    frontBuffer().pushMatrix();
    applyTransformation(graph.eye());
    drawEye(frontBuffer(), graph, texture);
    frontBuffer().popMatrix();
  }

  /**
   * Same as {@code drawEye(pGraphics, graph, false)}.
   *
   * @see #drawEye(PGraphics, Graph, boolean)
   */
  public void drawEye(PGraphics pGraphics, Graph graph) {
    drawEye(pGraphics, graph, false);
  }

  /**
   * Draws a representations of the {@code graph.eye()} onto {@code pGraphics}.
   * If {@code texture} draws the projected scene on the near plane.
   * <p>
   * Warning: texture only works with opengl renderers.
   * <p>
   * Note that if {@code graph == this} this method has not effect at all.
   */
  public void drawEye(PGraphics pGraphics, Graph graph, boolean texture) {
    // Key here is to represent the eye boundaryWidthHeight, zNear and zFar params
    // (which are given in world units) in eye units.
    // Hence they should be multiplied by: 1 / eye.eye().magnitude()
    if (graph == this) {
      System.out.println("Warning: No drawEye done!");
      return;
    }
    pGraphics.pushStyle();

    // boolean drawFarPlane = true;
    // int farIndex = drawFarPlane ? 1 : 0;
    int farIndex = is3D() ? 1 : 0;
    boolean ortho = false;
    if (is3D())
      if (graph.type() == Graph.Type.ORTHOGRAPHIC)
        ortho = true;

    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector[] points = new Vector[2];
    points[0] = new Vector();
    points[1] = new Vector();

    if (is2D() || ortho) {
      float[] wh = graph.boundaryWidthHeight();
      points[0].setX(wh[0] * 1 / graph.eye().magnitude());
      points[1].setX(wh[0] * 1 / graph.eye().magnitude());
      points[0].setY(wh[1] * 1 / graph.eye().magnitude());
      points[1].setY(wh[1] * 1 / graph.eye().magnitude());
    }

    if (is3D()) {
      points[0].setZ(graph.zNear() * 1 / graph.eye().magnitude());
      points[1].setZ(graph.zFar() * 1 / graph.eye().magnitude());
      if (graph.type() == Graph.Type.PERSPECTIVE) {
        points[0].setY(points[0].z() * PApplet.tan((graph.fieldOfView() / 2.0f)));
        points[0].setX(points[0].y() * graph.aspectRatio());
        float ratio = points[1].z() / points[0].z();
        points[1].setY(ratio * points[0].y());
        points[1].setX(ratio * points[0].x());
      }

      // Frustum lines
      switch (graph.type()) {
        case PERSPECTIVE: {
          pGraphics.beginShape(PApplet.LINES);
          Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pGraphics, points[farIndex].x(), points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pGraphics, -points[farIndex].x(), points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pGraphics, -points[farIndex].x(), -points[farIndex].y(), -points[farIndex].z());
          Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
          Scene.vertex(pGraphics, points[farIndex].x(), -points[farIndex].y(), -points[farIndex].z());
          pGraphics.endShape();
          break;
        }
        case ORTHOGRAPHIC: {
          // if (drawFarPlane) {
          pGraphics.beginShape(PApplet.LINES);
          Scene.vertex(pGraphics, points[0].x(), points[0].y(), -points[0].z());
          Scene.vertex(pGraphics, points[1].x(), points[1].y(), -points[1].z());
          Scene.vertex(pGraphics, -points[0].x(), points[0].y(), -points[0].z());
          Scene.vertex(pGraphics, -points[1].x(), points[1].y(), -points[1].z());
          Scene.vertex(pGraphics, -points[0].x(), -points[0].y(), -points[0].z());
          Scene.vertex(pGraphics, -points[1].x(), -points[1].y(), -points[1].z());
          Scene.vertex(pGraphics, points[0].x(), -points[0].y(), -points[0].z());
          Scene.vertex(pGraphics, points[1].x(), -points[1].y(), -points[1].z());
          pGraphics.endShape();
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

    pGraphics.noStroke();
    // Arrow base
    if (texture) {
      pGraphics.pushStyle();// end at arrow
      pGraphics.colorMode(PApplet.RGB, 255);
      float r = pGraphics.red(pGraphics.fillColor);
      float g = pGraphics.green(pGraphics.fillColor);
      float b = pGraphics.blue(pGraphics.fillColor);
      pGraphics.fill(r, g, b, 126);// same transparency as near plane texture
    }
    pGraphics.beginShape(PApplet.QUADS);
    if (isLeftHanded()) {
      Scene.vertex(pGraphics, -baseHalfWidth, -points[0].y(), -points[0].z());
      Scene.vertex(pGraphics, baseHalfWidth, -points[0].y(), -points[0].z());
      Scene.vertex(pGraphics, baseHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pGraphics, -baseHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pGraphics, -baseHalfWidth, points[0].y(), -points[0].z());
      Scene.vertex(pGraphics, baseHalfWidth, points[0].y(), -points[0].z());
      Scene.vertex(pGraphics, baseHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pGraphics, -baseHalfWidth, baseHeight, -points[0].z());
    }
    pGraphics.endShape();

    // Arrow
    pGraphics.beginShape(PApplet.TRIANGLES);
    if (isLeftHanded()) {
      Scene.vertex(pGraphics, 0.0f, -arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pGraphics, 0.0f, arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, baseHeight, -points[0].z());
    }
    if (texture)
      pGraphics.popStyle();// begin at arrow base
    pGraphics.endShape();

    // Planes
    // far plane
    _drawPlane(pGraphics, graph, points[1], new Vector(0, 0, -1), false);
    // near plane
    _drawPlane(pGraphics, graph, points[0], new Vector(0, 0, 1), texture);

    pGraphics.popStyle();
  }

  public void drawEyeNearPlane(Graph graph) {
    drawEyeNearPlane(graph, false);
  }

  /**
   * Applies the {@code graph.eye()} transformation and then calls
   * {@link #drawEyeNearPlane(PGraphics, Graph, boolean)} on the scene {@link #frontBuffer()}.
   * If {@code texture} draws the projected scene on the near plane.
   *
   * @see #drawEyeNearPlane(PGraphics, Graph, boolean)
   * @see #applyTransformation(Frame)
   * @see #drawEye(PGraphics, Graph, boolean)
   */
  public void drawEyeNearPlane(Graph graph, boolean texture) {
    frontBuffer().pushMatrix();
    applyTransformation(graph.eye());
    drawEyeNearPlane(frontBuffer(), graph, texture);
    frontBuffer().popMatrix();
  }

  /**
   * Same as {@code drawEyeNearPlane(pGraphics, graph, false)}.
   *
   * @see #drawEyeNearPlane(PGraphics, Graph, boolean)
   */
  public void drawEyeNearPlane(PGraphics pGraphics, Graph graph) {
    drawEyeNearPlane(pGraphics, graph, false);
  }

  /**
   * Draws the eye near plane onto {@code pGraphics}. If {@code texture} draws the projected
   * scene on the plane.
   * <p>
   * Warning: texture only works with opengl renderers.
   * <p>
   * Note that if {@code graph == this} this method has not effect at all.
   */
  public void drawEyeNearPlane(PGraphics pGraphics, Graph graph, boolean texture) {
    // Key here is to represent the eye boundaryWidthHeight and zNear params
    // (which are is given in world units) in eye units.
    // Hence they should be multiplied by: 1 / eye.eye().magnitude()
    if (graph == this) {
      System.out.println("Warning: No drawEyeNearPlane done!");
      return;
    }
    pGraphics.pushStyle();
    boolean ortho = false;
    if (is3D())
      if (graph.type() == Graph.Type.ORTHOGRAPHIC)
        ortho = true;
    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector corner = new Vector();
    if (is2D() || ortho) {
      float[] wh = graph.boundaryWidthHeight();
      corner.setX(wh[0] * 1 / graph.eye().magnitude());
      corner.setY(wh[1] * 1 / graph.eye().magnitude());
    }
    if (is3D()) {
      corner.setZ(graph.zNear() * 1 / graph.eye().magnitude());
      if (graph.type() == Graph.Type.PERSPECTIVE) {
        corner.setY(corner.z() * PApplet.tan((graph.fieldOfView() / 2.0f)));
        corner.setX(corner.y() * graph.aspectRatio());
      }
    }
    _drawPlane(pGraphics, graph, corner, new Vector(0, 0, 1), texture);
  }

  protected void _drawPlane(PGraphics pGraphics, Graph graph, Vector corner, Vector normal, boolean texture) {
    pGraphics.pushStyle();
    // near plane
    pGraphics.beginShape(PApplet.QUAD);
    pGraphics.normal(normal.x(), normal.y(), normal.z());
    if (pGraphics instanceof PGraphicsOpenGL && texture && graph instanceof Scene) {
      pGraphics.textureMode(NORMAL);
      pGraphics.tint(255, 126); // Apply transparency without changing color
      pGraphics.texture(((Scene) graph).frontBuffer());
      Scene.vertex(pGraphics, corner.x(), corner.y(), -corner.z(), 1, 1);
      Scene.vertex(pGraphics, -corner.x(), corner.y(), -corner.z(), 0, 1);
      Scene.vertex(pGraphics, -corner.x(), -corner.y(), -corner.z(), 0, 0);
      Scene.vertex(pGraphics, corner.x(), -corner.y(), -corner.z(), 1, 0);
    } else {
      Scene.vertex(pGraphics, corner.x(), corner.y(), -corner.z());
      Scene.vertex(pGraphics, -corner.x(), corner.y(), -corner.z());
      Scene.vertex(pGraphics, -corner.x(), -corner.y(), -corner.z());
      Scene.vertex(pGraphics, corner.x(), -corner.y(), -corner.z());
    }
    pGraphics.endShape();
    pGraphics.popStyle();
  }

  /**
   * Same as {@code drawProjector(frontBuffer(), graph, vector)}.
   *
   * @see #drawProjector(PGraphics, Graph, Vector)
   * @see #drawProjectors(Graph, List)
   */
  public void drawProjector(Graph graph, Vector vector) {
    drawProjector(frontBuffer(), graph, vector);
  }

  /**
   * Same as {@code drawProjectors(pGraphics, eye, Arrays.asList(point))}.
   *
   * @see #drawProjector(PGraphics, Graph, Vector)
   * @see #drawProjectors(PGraphics, Graph, List)
   */
  public void drawProjector(PGraphics pGraphics, Graph eye, Vector point) {
    drawProjectors(pGraphics, eye, Arrays.asList(point));
  }

  /**
   * Same as {@code drawProjectors(frontBuffer(), graph, points)}.
   *
   * @see #drawProjectors(PGraphics, Graph, List)
   * @see #drawProjector(Graph, Vector)
   */
  public void drawProjectors(Graph graph, List<Vector> points) {
    drawProjectors(frontBuffer(), graph, points);
  }

  /**
   * Draws the projection of each point in {@code points} in the near plane onto {@code pGraphics}.
   * <p>
   * This method should be used in conjunction with {@link #drawEye(PGraphics, Graph, boolean)}.
   * <p>
   * Note that if {@code graph == this} this method has not effect at all.
   *
   * @see #drawProjector(PGraphics, Graph, Vector)
   */
  public void drawProjectors(PGraphics pGraphics, Graph graph, List<Vector> points) {
    if (graph == this) {
      System.out.println("Warning: No drawProjectors done!");
      return;
    }
    pGraphics.pushStyle();
    if (is2D()) {
      pGraphics.beginShape(PApplet.POINTS);
      for (Vector s : points)
        Scene.vertex(pGraphics, s.x(), s.y());
      pGraphics.endShape();
    } else {
      // if ORTHOGRAPHIC: do it in the eye coordinate system
      // if PERSPECTIVE: do it in the world coordinate system
      Vector o = new Vector();
      if (graph.type() == Graph.Type.ORTHOGRAPHIC) {
        pGraphics.pushMatrix();
        applyTransformation(graph.eye());
      }
      // in PERSPECTIVE cache the transformed origin
      else
        o = graph.eye().inverseCoordinatesOf(new Vector());
      pGraphics.beginShape(PApplet.LINES);
      for (Vector s : points) {
        if (graph.type() == Graph.Type.ORTHOGRAPHIC) {
          Vector v = graph.eye().coordinatesOf(s);
          Scene.vertex(pGraphics, v.x(), v.y(), v.z());
          // Key here is to represent the eye zNear param (which is given in world units)
          // in eye units.
          // Hence it should be multiplied by: 1 / eye.eye().magnitude()
          // The neg sign is because the zNear is positive but the eye view direction is
          // the negative Z-axis
          Scene.vertex(pGraphics, v.x(), v.y(), -(graph.zNear() * 1 / graph.eye().magnitude()));
        } else {
          Scene.vertex(pGraphics, s.x(), s.y(), s.z());
          Scene.vertex(pGraphics, o.x(), o.y(), o.z());
        }
      }
      pGraphics.endShape();
      if (graph.type() == Graph.Type.ORTHOGRAPHIC)
        pGraphics.popMatrix();
    }
    pGraphics.popStyle();
  }

  /**
   * Convenience function that simply calls {@code drawCross(px, py, 30)}.
   *
   * @see #drawCross(float, float, float)
   */
  public void drawCross(float px, float py) {
    drawCross(px, py, 30);
  }

  /**
   * Same as {@code drawCross(frontBuffer(), px, py, size)}.
   *
   * @see #drawCross(PGraphics, float, float, float)
   */
  public void drawCross(float px, float py, float size) {
    drawCross(frontBuffer(), px, py, size);
  }

  /**
   * Draws a cross on the screen centered under pixel {@code (px, py)}, and edge of size
   * {@code size} onto {@code pGraphics}.
   */
  public void drawCross(PGraphics pGraphics, float px, float py, float size) {
    float half_size = size / 2f;
    pGraphics.pushStyle();
    beginScreenCoordinates(pGraphics);
    pGraphics.noFill();
    pGraphics.beginShape(LINES);
    vertex(pGraphics, px - half_size, py);
    vertex(pGraphics, px + half_size, py);
    vertex(pGraphics, px, py - half_size);
    vertex(pGraphics, px, py + half_size);
    pGraphics.endShape();
    endScreenCoordinates(pGraphics);
    pGraphics.popStyle();
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
   * Same as {@code drawFilledCircle(frontBuffer(), subdivisions, center, radius)}.
   *
   * @see #drawFilledCircle(PGraphics, int, Vector, float)
   */
  public void drawFilledCircle(int subdivisions, Vector center, float radius) {
    drawFilledCircle(frontBuffer(), subdivisions, center, radius);
  }

  /**
   * Draws a filled circle onto {@code pGraphics} using screen coordinates.
   *
   * @param subdivisions Number of triangles approximating the circle.
   * @param center       Circle screen center.
   * @param radius       Circle screen radius.
   */
  public void drawFilledCircle(PGraphics pGraphics, int subdivisions, Vector center, float radius) {
    pGraphics.pushStyle();
    float precision = PApplet.TWO_PI / subdivisions;
    float x = center.x();
    float y = center.y();
    float angle, x2, y2;
    beginScreenCoordinates(pGraphics);
    pGraphics.noStroke();
    pGraphics.beginShape(TRIANGLE_FAN);
    vertex(pGraphics, x, y);
    for (angle = 0.0f; angle <= PApplet.TWO_PI + 1.1 * precision; angle += precision) {
      x2 = x + PApplet.sin(angle) * radius;
      y2 = y + PApplet.cos(angle) * radius;
      vertex(pGraphics, x2, y2);
    }
    pGraphics.endShape();
    endScreenCoordinates(pGraphics);
    pGraphics.popStyle();
  }

  /**
   * Same as {@code drawFilledSquare(frontBuffer(), center, edge)}.
   *
   * @see #drawFilledSquare(PGraphics, Vector, float)
   */
  public void drawFilledSquare(Vector center, float edge) {
    drawFilledSquare(frontBuffer(), center, edge);
  }

  /**
   * Draws a filled square onto {@code pGraphics} using screen coordinates.
   *
   * @param center Square screen center.
   * @param edge   Square edge length.
   */
  public void drawFilledSquare(PGraphics pGraphics, Vector center, float edge) {
    float half_edge = edge / 2f;
    pGraphics.pushStyle();
    float x = center.x();
    float y = center.y();
    beginScreenCoordinates(pGraphics);
    pGraphics.noStroke();
    pGraphics.beginShape(QUADS);
    vertex(pGraphics, x - half_edge, y + half_edge);
    vertex(pGraphics, x + half_edge, y + half_edge);
    vertex(pGraphics, x + half_edge, y - half_edge);
    vertex(pGraphics, x - half_edge, y - half_edge);
    pGraphics.endShape();
    endScreenCoordinates(pGraphics);
    pGraphics.popStyle();
  }

  /**
   * Same as {@code drawShooterTarget(frontBuffer(), center, length)}.
   *
   * @see #drawShooterTarget(PGraphics, Vector, float)
   */
  public void drawShooterTarget(Vector center, float length) {
    drawShooterTarget(frontBuffer(), center, length);
  }

  /**
   * Draws the classical shooter target onto {@code pGraphics}.
   *
   * @param center Center of the target on the screen
   * @param length Length of the target in pixels
   */
  public void drawShooterTarget(PGraphics pGraphics, Vector center, float length) {
    float half_length = length / 2f;
    pGraphics.pushStyle();
    float x = center.x();
    float y = center.y();
    beginScreenCoordinates(pGraphics);
    pGraphics.noFill();

    pGraphics.beginShape();
    vertex(pGraphics, (x - half_length), (y - half_length) + (0.6f * half_length));
    vertex(pGraphics, (x - half_length), (y - half_length));
    vertex(pGraphics, (x - half_length) + (0.6f * half_length), (y - half_length));
    pGraphics.endShape();

    pGraphics.beginShape();
    vertex(pGraphics, (x + half_length) - (0.6f * half_length), (y - half_length));
    vertex(pGraphics, (x + half_length), (y - half_length));
    vertex(pGraphics, (x + half_length), ((y - half_length) + (0.6f * half_length)));
    pGraphics.endShape();

    pGraphics.beginShape();
    vertex(pGraphics, (x + half_length), ((y + half_length) - (0.6f * half_length)));
    vertex(pGraphics, (x + half_length), (y + half_length));
    vertex(pGraphics, ((x + half_length) - (0.6f * half_length)), (y + half_length));
    pGraphics.endShape();

    pGraphics.beginShape();
    vertex(pGraphics, (x - half_length) + (0.6f * half_length), (y + half_length));
    vertex(pGraphics, (x - half_length), (y + half_length));
    vertex(pGraphics, (x - half_length), ((y + half_length) - (0.6f * half_length)));
    pGraphics.endShape();
    endScreenCoordinates(pGraphics);
    drawCross(center.x(), center.y(), 0.6f * length);
    pGraphics.popStyle();
  }

  /**
   * Draws the node picking target: a shooter target visual hint of
   * {@link Node#precisionThreshold()} pixels size.
   */
  //TODO better to implement it from Frame param, but I'm just too lazy to do that ;)
  public void drawPickingTarget(Node node) {
    if (node.isEye()) {
      System.err.println("eye nodes don't have a picking target");
      return;
    }
    // if (!inputHandler().hasGrabber(iFrame)) {
    // System.err.println("addGrabber iFrame to motionAgent before drawing picking target");
    // return;
    // }
    Vector center = projectedCoordinatesOf(node.position());
    if (inputHandler().isInputGrabber(node)) {
      frontBuffer().pushStyle();
      frontBuffer().strokeWeight(2 * frontBuffer().strokeWeight);
      frontBuffer().colorMode(HSB, 255);
      float hue = frontBuffer().hue(frontBuffer().strokeColor);
      float saturation = frontBuffer().saturation(frontBuffer().strokeColor);
      float brightness = frontBuffer().brightness(frontBuffer().strokeColor);
      frontBuffer().stroke(hue, saturation * 1.4f, brightness * 1.4f);
      drawShooterTarget(center, (node.precisionThreshold() + 1));
      frontBuffer().popStyle();
    } else {
      frontBuffer().pushStyle();
      frontBuffer().colorMode(HSB, 255);
      float hue = frontBuffer().hue(frontBuffer().strokeColor);
      float saturation = frontBuffer().saturation(frontBuffer().strokeColor);
      float brightness = frontBuffer().brightness(frontBuffer().strokeColor);
      frontBuffer().stroke(hue, saturation * 1.4f, brightness);
      drawShooterTarget(center, node.precisionThreshold());
      frontBuffer().popStyle();
    }
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(6)}.
   *
   * @see #drawTorusSolenoid(int)
   */
  public void drawTorusSolenoid() {
    drawTorusSolenoid(6);
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(faces, 0.07f * radius())}.
   *
   * @see #drawTorusSolenoid(int, float)
   */
  public void drawTorusSolenoid(int faces) {
    drawTorusSolenoid(faces, 0.07f * radius());
  }

  /**
   * Convenience function that simply calls {@code drawTorusSolenoid(6, insideRadius)}.
   *
   * @see #drawTorusSolenoid(int, float)
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
   * Same as {@code drawTorusSolenoid(frontBuffer(), faces, detail, insideRadius, outsideRadius)}.
   *
   * @see #drawTorusSolenoid(PGraphics, int, int, float, float)
   */
  public void drawTorusSolenoid(int faces, int detail, float insideRadius, float outsideRadius) {
    drawTorusSolenoid(frontBuffer(), faces, detail, insideRadius, outsideRadius);
  }

  /**
   * Same as {@code drawTorusSolenoid(pGraphics, 6)}.
   *
   * @see #drawTorusSolenoid(PGraphics, float)
   */
  public static void drawTorusSolenoid(PGraphics pGraphics) {
    drawTorusSolenoid(pGraphics, 6);
  }

  /**
   * Same as {@code drawTorusSolenoid(pGraphics, 6, insideRadius)}.
   *
   * @see #drawTorusSolenoid(PGraphics, float)
   */
  public static void drawTorusSolenoid(PGraphics pGraphics, float insideRadius) {
    drawTorusSolenoid(pGraphics, 6, insideRadius);
  }

  /**
   * Same as {@code drawTorusSolenoid(pGraphics, faces, 100, insideRadius, insideRadius * 1.3f)}.
   *
   * @see #drawTorusSolenoid(PGraphics, int, int, float, float)
   */
  public static void drawTorusSolenoid(PGraphics pGraphics, int faces, float insideRadius) {
    drawTorusSolenoid(pGraphics, faces, 100, insideRadius, insideRadius * 1.3f);
  }

  /**
   * Draws a torus solenoid onto {@code pGraphics}.
   * <p>
   * Code contributed by Jacques Maire (http://www.alcys.com/) See also:
   * http://www.mathcurve.com/courbes3d/solenoidtoric/solenoidtoric.shtml
   * http://crazybiocomputing.blogspot.fr/2011/12/3d-curves-toric-solenoids.html
   *
   * @param faces
   * @param detail
   * @param insideRadius
   * @param outsideRadius
   */
  public static void drawTorusSolenoid(PGraphics pGraphics, int faces, int detail, float insideRadius, float outsideRadius) {
    pGraphics.pushStyle();
    pGraphics.noStroke();
    Vector v1, v2;
    int b, ii, jj, a;
    float eps = PApplet.TWO_PI / detail;
    for (a = 0; a < faces; a += 2) {
      pGraphics.beginShape(PApplet.TRIANGLE_STRIP);
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
        vertex(pGraphics, v1.x(), v1.y(), v1.z());
        vertex(pGraphics, v2.x(), v2.y(), v2.z());
      }
      pGraphics.endShape();
    }
    pGraphics.popStyle();
  }
}
