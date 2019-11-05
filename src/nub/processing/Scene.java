/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

// Thanks goes to Andres Colubri, https://www.sabetilab.org/andres-colubri/
// for implementing the first off-screen scene working example

package nub.processing;

import nub.core.Graph;
import nub.core.Interpolator;
import nub.core.MatrixHandler;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.timing.Task;
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
import java.util.Map;

/**
 * A 2D or 3D interactive, on-screen or off-screen, Processing mouse-driven {@link Graph}.
 * <h1>Usage</h1>
 * Typical usage comprises two steps: scene instantiation and setting some nodes.
 * <h2>Scene instantiation</h2>
 * Instantiate your on-screen scene at the {@code PApplet.setup()}:
 * <pre>
 * {@code
 * Scene scene;
 * void setup() {
 *   scene = new Scene(this);
 * }
 * }
 * </pre>
 * The scene {@link #context()} corresponds to the {@code PApplet} main canvas.
 * <p>
 * Off-screen scenes should be instantiated upon a {@code PGraphics} object:
 * <pre>
 * {@code
 * Scene scene;
 * PGraphics canvas;
 * void setup() {
 *   canvas = createGraphics(500, 500, P3D);
 *   scene = new Scene(this, canvas);
 *   // or use the equivalent (to the previous two lines) but simpler version:
 *   // scene = new Scene(this, P3D, 500, 500);
 * }
 * }
 * </pre>
 * In this case, the scene {@link #context()} corresponds to the {@code canvas}.
 * <h1>Drawing functionality</h1>
 * There are several static drawing functions that complements those already provided
 * by Processing, such as: {@link #drawCylinder(PGraphics, int, float, float)},
 * {@link #drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)},
 * {@link #drawCone(PGraphics, int, float, float, float, float)},
 * {@link #drawCone(PGraphics, int, float, float, float, float, float)} and
 * {@link #drawTorusSolenoid(PGraphics, int, int, float, float)}.
 * <p>
 * Drawing functions that take a {@code PGraphics} parameter (including the above
 * static ones), such as {@link #beginHUD(PGraphics)},
 * {@link #endHUD(PGraphics)}, {@link #drawAxes(PGraphics, float)},
 * {@link #drawCross(PGraphics, float, float, float)} and {@link #drawGrid(PGraphics)}
 * among others, can be used to set a {@link Node#setShape(Object)} (see
 * also {@link Node#graphics(PGraphics)}).
 * <p>
 * Another scene's eye (different than the graph {@link Graph#eye()}) can be drawn with
 * {@link #drawFrustum(Graph)}. Typical usage include interactive minimaps and
 * visibility culling visualization and debugging.
 * <p>
 * An {@link Interpolator} path may be drawn with code like this:
 * <pre>
 * {@code
 * void draw() {
 *   scene.render();
 *   scene.drawPath(interpolator, 5);
 * }
 * }
 * </pre>
 * while {@link #render()} will draw the animated node(s), {@link #drawPath(Interpolator, int)}
 * will draw the interpolated path too.
 * <h1>Human Interface Devices</h1>
 * The default <a href="https://en.wikipedia.org/wiki/Human_interface_device">Human Interface Device</a>
 * is the Processing mouse, see {@link #track()}, {@link #cast()}, {@link #spin()}, {@link #translate()}
 * {@link #scale(float)}, etc. To set up another device refer to the {@link Graph} documentation.
 *
 * @see Graph
 * @see Node
 * @see Interpolator
 */
public class Scene extends Graph implements PConstants {
  public static String prettyVersion = "0.3.0";
  public static String version = "3";

  // P R O C E S S I N G A P P L E T A N D O B J E C T S
  protected PApplet _parent;

  // E X C E P T I O N H A N D L I N G
  protected int _beginOffScreenDrawingCalls;

  // _bb : picking buffer
  protected long _bbNeed, _bbCount;
  protected PShader _triangleShader, _lineShader, _pointShader;

  // CONSTRUCTORS

  /**
   * Constructor that defines an on-screen Processing scene. Same as
   * {@code this(pApplet, pApplet.g)}.
   *
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, PGraphics, int, int)
   * @see #Scene(PApplet, String, int, int)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, String)
   */
  public Scene(PApplet pApplet) {
    this(pApplet, pApplet.g);
  }

  /**
   * Same as {@code this(pApplet, pGraphics, 0, 0)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics, int, int)
   * @see #Scene(PApplet, String, int, int)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, String)
   */
  public Scene(PApplet pApplet, PGraphics pGraphics) {
    this(pApplet, pGraphics, 0, 0);
  }

  /**
   * Same as {@code this(pApplet, renderer, pApplet.width, pApplet.height)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics, int, int)
   * @see #Scene(PApplet, String, int, int)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, PGraphics)
   */
  public Scene(PApplet pApplet, String renderer) {
    this(pApplet, renderer, pApplet.width, pApplet.height);
  }

  /**
   * Same as {@code this(pApplet, renderer, width, height, 0, 0)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics, int, int)
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, String)
   */
  public Scene(PApplet pApplet, String renderer, int width, int height) {
    this(pApplet, renderer, width, height, 0, 0);
  }

  /**
   * Same as {@code this(pApplet, pApplet.createGraphics(width, height, renderer), x, y)}.
   *
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, String, int, int)
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, String)
   */
  public Scene(PApplet pApplet, String renderer, int width, int height, int x, int y) {
    this(pApplet, pApplet.createGraphics(width, height, renderer), x, y);
  }

  /**
   * Main constructor defining a left-handed Processing compatible scene. Calls
   * {@link #setMatrixHandler(MatrixHandler)} using a customized
   * {@link MatrixHandler} depending on the {@link #context()} type (see
   * {@link GLMatrixHandler}).
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
   * @see Graph#Graph(Object, nub.core.Graph.Type, int, int)
   * @see #Scene(PApplet)
   * @see #Scene(PApplet, PGraphics)
   * @see #Scene(PApplet, String, int, int)
   * @see #Scene(PApplet, String, int, int, int, int)
   * @see #Scene(PApplet, String)
   */
  public Scene(PApplet pApplet, PGraphics pGraphics, int x, int y) {
    super(pGraphics, pGraphics instanceof PGraphics3D ? Type.PERSPECTIVE : Type.TWO_D, pGraphics.width, pGraphics.height);
    // 1. P5 objects
    _parent = pApplet;
    _offscreen = pGraphics != pApplet.g;
    _upperLeftCornerX = _offscreen ? x : 0;
    _upperLeftCornerY = _offscreen ? y : 0;
    // 2. Back buffer
    _bb = (context() instanceof processing.opengl.PGraphicsOpenGL) ?
        pApplet().createGraphics(context().width, context().height, context() instanceof PGraphics3D ? P3D : P2D) :
        null;
    if (_bb != null) {
      _bbMatrixHandler = new GLMatrixHandler((PGraphicsOpenGL) _bb);
      _triangleShader = pApplet().loadShader("PickingBuffer.frag");
      _lineShader = pApplet().loadShader("PickingBuffer.frag");
      _pointShader = pApplet().loadShader("PickingBuffer.frag");
    }
    // 3. Register P5 methods
    if (!isOffscreen()) {
      pApplet().registerMethod("pre", this);
      pApplet().registerMethod("draw", this);
    }
    // TODO buggy
    pApplet().registerMethod("dispose", this);
    // 4. Handed
    setLeftHanded();
  }

  // Tasks

  @Override
  protected Task _initTask(Interpolator interpolator) {
    return new TimingTask(this) {
      @Override
      public void execute() {
        interpolator.execute();
      }
    };
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
  @Override
  public PGraphics context() {
    return (PGraphics) _fb;
  }

  /**
   * Sets the {@code name} {@code shader} uniform from {@code matrix}.
   */
  public static void setUniform(PShader shader, String name, Matrix matrix) {
    PMatrix3D pmatrix = new PMatrix3D();
    //pmatrix.set(Scene.toPMatrix(matrix));
    //pmatrix.transpose();
    // same as:
    pmatrix.set(matrix.get(new float[16]));
    shader.set(name, pmatrix);
  }

  /**
   * Sets the {@code name} {@code shader} uniform from {@code vector}.
   */
  public static void setUniform(PShader shader, String name, Vector vector) {
    PVector pvector = new PVector(vector.x(), vector.y(), vector.z());
    shader.set(name, pvector);
  }

  /**
   * Applies the {@code node} transformation on {@code pGraphics}.
   *
   * @see #applyWorldTransformation(PGraphics, Node)
   */
  public static void applyTransformation(PGraphics pGraphics, Node node) {
    if (pGraphics.is3D()) {
      pGraphics.translate(node.translation()._vector[0], node.translation()._vector[1], node.translation()._vector[2]);
      pGraphics.rotate(node.rotation().angle(), (node.rotation()).axis()._vector[0], (node.rotation()).axis()._vector[1], (node.rotation()).axis()._vector[2]);
      pGraphics.scale(node.scaling(), node.scaling(), node.scaling());
    } else {
      pGraphics.translate(node.translation().x(), node.translation().y());
      pGraphics.rotate(node.rotation().angle2D());
      pGraphics.scale(node.scaling(), node.scaling());
    }
  }

  /**
   * Applies the {@code node} world transformation on {@code pGraphics}.
   *
   * @see #applyTransformation(PGraphics, Node)
   */
  public static void applyWorldTransformation(PGraphics pGraphics, Node node) {
    Node reference = node.reference();
    if (reference != null) {
      applyWorldTransformation(pGraphics, reference);
      applyTransformation(pGraphics, node);
    } else {
      applyTransformation(pGraphics, node);
    }
  }

  // PICKING BUFFER

  @Override
  protected PGraphics _backBuffer() {
    return (PGraphics) _bb;
  }

  /**
   * Internal use. Traverse the scene {@link #nodes()}) into the
   * {@link #_backBuffer()} to perform picking on the scene {@link #nodes()}.
   * <p>
   * Called by {@link #draw()} (on-screen scenes) and {@link #endDraw()} (off-screen
   * scenes).
   */
  @Override
  protected void _renderBackBuffer() {
    if (_bb != null && _bbCount < _bbNeed) {
      _backBuffer().beginDraw();
      _backBuffer().pushStyle();
      _backBuffer().background(0);
      super._renderBackBuffer();
      _backBuffer().popStyle();
      _backBuffer().endDraw();
      _backBuffer().loadPixels();
      _bbCount = _bbNeed;
    }
  }

  // OPENGL

  /**
   * The {@link #center()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #center()} is set).
   */
  public boolean setCenterFromPixel(int pixelX, int pixelY) {
    Vector pup = pointUnderPixel(pixelX, pixelY);
    if (pup != null) {
      setCenter(pup);
      return true;
    }
    return false;
  }

  /**
   * Returns the depth (z-value) of the object under the {@code pixel}. Used by
   * {@link #pointUnderPixel(int, int)}.
   * <p>
   * The z-value ranges in [0..1] (near and far plane respectively). In 3D note that this
   * value is not a linear interpolation between {@link #zNear()} and {@link #zFar()}:
   * {@code z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z')} where {@code z'} is
   * the distance from the point you project to the eye, along the {@link #viewDirection()}.
   *
   * @see #pointUnderPixel(int, int)
   */
  public float pixelDepth(int pixelX, int pixelY) {
    PGraphicsOpenGL pggl;
    if (context() instanceof PGraphicsOpenGL)
      pggl = (PGraphicsOpenGL) context();
    else
      throw new RuntimeException("context() is not instance of PGraphicsOpenGL");
    float[] depth = new float[1];
    PGL pgl = pggl.beginPGL();
    pgl.readPixels(pixelX, (height() - pixelY), 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT,
        FloatBuffer.wrap(depth));
    pggl.endPGL();
    return depth[0];
  }

  /**
   * Returns the world coordinates of the 3D point located at {@code pixel} (x,y) on
   * screen. May be null if no object is under pixel.
   */
  public Vector pointUnderPixel(int pixelX, int pixelY) {
    float depth = pixelDepth(pixelX, pixelY);
    Vector point = location(new Vector(pixelX, pixelY, depth));
    return (depth < 1.0f) ? point : null;
  }

  /**
   * Disables z-buffer.
   */
  public void disableDepthTest() {
    disableDepthTest(context());
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
    enableDepthTest(context());
  }

  /**
   * Enables depth test on the PGraphics instance.
   *
   * @see #disableDepthTest(PGraphics)
   */
  public void enableDepthTest(PGraphics pGraphics) {
    pGraphics.hint(PApplet.ENABLE_DEPTH_TEST);
  }

  // 3. Drawing methods

  /**
   * The {@link #anchor()} is set to the point located under {@code pixel} on screen.
   * <p>
   * Returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #anchor()} is set).
   */
  public boolean setAnchorFromPixel(int pixelX, int pixelY) {
    Vector pup = pointUnderPixel(pixelX, pixelY);
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
   * handles resize events. This method is registered at the PApplet and hence you
   * don't need to call it. Only meaningful if the graph is on-screen (it the graph
   * {@link #isOffscreen()} it even doesn't get registered at the PApplet.
   * <p>
   * If {@link #context()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls {@link #setWidth(int)} and {@link #setHeight(int)}.
   *
   * @see #draw()
   * @see #render()
   * @see #beginDraw()
   * @see #endDraw()
   * @see #isOffscreen()
   */
  public void pre() {
    if ((width() != context().width) || (height() != context().height)) {
      setWidth(context().width);
      setHeight(context().height);
    }
    preDraw();
    _matrixHandler.pushMatrix();
  }

  /**
   * Paint method which is called just after your {@code PApplet.draw()} method. Simply
   * render the back buffer (useful for picking). This method is registered at the PApplet
   * and hence you don't need to call it. Only meaningful if the graph is on-screen (it
   * the graph {@link #isOffscreen()} it even doesn't get registered at the PApplet.
   * <p>
   * If {@link #isOffscreen()} does nothing.
   *
   * @see #pre()
   * @see #render()
   * @see #beginDraw()
   * @see #endDraw()
   * @see #isOffscreen()
   */
  public void draw() {
    _matrixHandler.popMatrix();
    _renderBackBuffer();
  }

  /**
   * Only if the scene {@link #isOffscreen()}. Calls {@code context().beginDraw()}
   * (hence there's no need to explicitly call it).
   * <p>
   * If {@link #context()} is resized then (re)sets the graph {@link #width()} and
   * {@link #height()}, and calls {@link #setWidth(int)} and {@link #setHeight(int)}.
   *
   * @see #draw()
   * @see #render()
   * @see #pre()
   * @see #endDraw()
   * @see #isOffscreen()
   * @see #context()
   */
  public void beginDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "begin(/end)Draw() should be used only within offscreen scenes. Check your implementation!");
    if (_beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    _beginOffScreenDrawingCalls++;
    if ((width() != context().width))
      setWidth(context().width);
    if ((height() != context().height))
      setHeight(context().height);
    // open off-screen pgraphics for drawing:
    context().beginDraw();
    preDraw();
    _matrixHandler.pushMatrix();
  }

  /**
   * Only if the scene {@link #isOffscreen()}. Calls:
   *
   * <ol>
   * <li>{@code context().endDraw()} and hence there's no need to explicitly call it</li>
   * <li>{@code _updateBackBuffer()}: Render the back buffer (useful for picking)</li>
   * </ol>
   *
   * @see #draw()
   * @see #render()
   * @see #beginDraw()
   * @see #pre()
   * @see #isOffscreen()
   * @see #context()
   */
  public void endDraw() {
    if (!isOffscreen())
      throw new RuntimeException(
          "(begin/)endDraw() should be used only within offscreen scenes. Check your implementation!");
    _beginOffScreenDrawingCalls--;
    if (_beginOffScreenDrawingCalls != 0)
      throw new RuntimeException("There should be exactly one beginDraw() call followed by a "
          + "endDraw() and they cannot be nested. Check your implementation!");
    _matrixHandler.popMatrix();
    context().endDraw();
    _renderBackBuffer();
  }

  /**
   * Same as {@code display(context())}. Only meaningful if the graph {@link #isOffscreen()}.
   *
   * @see #display(PGraphics)
   * @see #context()
   */
  public void display() {
    display(context());
  }

  /**
   * Same as {@code pApplet().image(pgraphics, originCorner().x(), originCorner().y())}.
   * Only meaningful if the graph {@link #isOffscreen()}.
   */
  public void display(PGraphics pgraphics) {
    if (isOffscreen())
      pApplet().image(pgraphics, _upperLeftCornerX, _upperLeftCornerY);
  }

  /**
   * Display the {@link #_backBuffer()} used for picking at screen coordinates
   * {@code (x, y)}. Mainly for debugging.
   */
  public void displayBackBuffer(int x, int y) {
    if (_backBuffer() != null)
      pApplet().image(_backBuffer(), x, y);
  }

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
      eye().set(_toNode(json.getJSONObject("eye")));

      /*
      JSONObject jsonEye = json.getJSONObject("eye");
      eye().set(_toNode(jsonEye));
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
      Node node = new Node(this);
      prune(node);
      node.set(_toNode(jsonInterpolator.getJSONObject(j)));
      node.setPickingThreshold(20);
      interpolator.addKeyFrame(node, jsonInterpolator.getJSONObject(j).getFloat("time"));
      /*
      if (pathsVisualHint())
        inputHandler().addNode(keyNode);
      if (!eye().keyFrameInterpolatorMap().containsKey(_id))
        eye().setKeyFrameInterpolator(_id, new Interpolator(this, eyeFrame()));
      eye().keyFrameInterpolator(_id).addKeyFrame(keyNode, keyFrames.getJSONObject(j).getFloat("time"));
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
    // TODO needs testing
    int i = 0;
    for (Map.Entry<Float, Node> entry : interpolator.keyFrames().entrySet()) {
      JSONObject jsonKeyFrame = _toJSONObject(entry.getValue());
      jsonKeyFrame.setFloat("time", entry.getKey());
      jsonKeyFrames.setJSONObject(i, jsonKeyFrame);
      i++;
    }
    /*
    // it previously was
    for (int i = 0; i < interpolator.size(); i++) {
      JSONObject jsonKeyFrame = _toJSONObject(interpolator.keyFrame(i));
      jsonKeyFrame.setFloat("time", interpolator.timeBluf(i));
      jsonKeyFrames.setJSONObject(i, jsonKeyFrame);
    }
    */
    return jsonKeyFrames;
  }

  /**
   * Used internally by {@link #loadConfig(String)}. Converts the P5 JSONObject into a node.
   */
  protected Node _toNode(JSONObject jsonNode) {
    Node node = new Node();
    float x, y, z, w;
    x = jsonNode.getJSONArray("position").getFloat(0);
    y = jsonNode.getJSONArray("position").getFloat(1);
    z = jsonNode.getJSONArray("position").getFloat(2);
    node.setPosition(new Vector(x, y, z));
    x = jsonNode.getJSONArray("orientation").getFloat(0);
    y = jsonNode.getJSONArray("orientation").getFloat(1);
    z = jsonNode.getJSONArray("orientation").getFloat(2);
    w = jsonNode.getJSONArray("orientation").getFloat(3);
    node.setOrientation(new Quaternion(x, y, z, w));
    node.setMagnitude(jsonNode.getFloat("magnitude"));
    return node;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code node} into a P5
   * JSONObject.
   */
  protected JSONObject _toJSONObject(Node node) {
    JSONObject jsonNode = new JSONObject();
    jsonNode.setFloat("magnitude", node.magnitude());
    jsonNode.setJSONArray("position", _toJSONArray(node.position()));
    jsonNode.setJSONArray("orientation", _toJSONArray(node.orientation()));
    return jsonNode;
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
   * Same as {@code return super.track(mouse(), nodeArray)}.
   *
   * @see Graph#track(int, int, Node[])
   */
  public Node track(Node[] nodeArray) {
    return track(mouseX(), mouseY(), nodeArray);
  }

  /**
   * Same as {@code return super.track(mouse(), nodeList)}.
   *
   * @see Graph#track(int, int, List< Node >)
   */
  public Node track(List<Node> nodeList) {
    return track(mouseX(), mouseY(), nodeList);
  }

  /**
   * Same as {@code return track(mouse(), node)}.
   *
   * @see #mouseX()
   * @see #mouseY()
   * @see Graph#tracks(Node, int, int)
   */
  public boolean tracks(Node node) {
    return tracks(node, mouseX(), mouseY());
  }

  @Override
  protected boolean _tracks(Node node, int x, int y) {
    if (node == null || isEye(node))
      return false;
    if (!node.isTaggingEnabled())
      return false;
    int index = y * width() + x;
    if (_backBuffer().pixels != null)
      if ((0 <= index) && (index < _backBuffer().pixels.length))
        return _backBuffer().pixels[index] == node.colorID();
    return false;
  }

  @Override
  protected void _drawFrontBuffer(Node node) {
    PGraphics pGraphics = context();
    pGraphics.pushStyle();
    pGraphics.pushMatrix();

    if (isTagged(node))
      pGraphics.scale(1 + node.highlighting());

    if (node.shape() != null)
      pGraphics.shapeMode(context().shapeMode);
    if (node.shape() != null)
      pGraphics.shape((PShape) node.shape());
    else
      node.graphics(pGraphics);

    if (node.pickingThreshold() == 0 && node.isTaggingEnabled())
      _bbNeed = frameCount();

    pGraphics.popStyle();
    pGraphics.popMatrix();
  }

  @Override
  protected void _drawOntoBuffer(Object context, Node node) {
    PGraphics pGraphics = (PGraphics) context;
    pGraphics.pushStyle();
    pGraphics.pushMatrix();

    if (node.shape() != null)
      pGraphics.shapeMode(context().shapeMode);
    if (node.shape() != null)
      pGraphics.shape((PShape) node.shape());
    else
      node.graphics(pGraphics);

    pGraphics.popStyle();
    pGraphics.popMatrix();
  }

  @Override
  protected void _drawBackBuffer(Node node) {
    PGraphics pGraphics = _backBuffer();
    if (node.pickingThreshold() == 0) {
      pGraphics.pushStyle();
      pGraphics.pushMatrix();

      float r = (float) (node.id() & 255) / 255.f;
      float g = (float) ((node.id() >> 8) & 255) / 255.f;
      float b = (float) ((node.id() >> 16) & 255) / 255.f;

      // TODO How to deal with these commands: breaks picking in Luxo when they're moved to the constructor
      // Seems related to: PassiveTransformations
      // funny, only safe way. Otherwise break things horribly when setting node shapes
      // and there are more than one node holding a shape
      pGraphics.shader(_triangleShader);
      pGraphics.shader(_lineShader, PApplet.LINES);
      pGraphics.shader(_pointShader, PApplet.POINTS);

      _triangleShader.set("id", new PVector(r, g, b));
      _lineShader.set("id", new PVector(r, g, b));
      _pointShader.set("id", new PVector(r, g, b));

      if (node.shape() != null)
        pGraphics.shapeMode(context().shapeMode);
      if (node.shape() != null)
        pGraphics.shape((PShape) node.shape());
      else
        node.graphics(pGraphics);

      pGraphics.popStyle();
      pGraphics.popMatrix();
    }
  }

  /**
   * Renders the scene onto {@code pGraphics} using the {@code eye} node point of view and
   * remaining frustum parameters. Useful to compute a shadow map taking the {@code eye} as
   * the light point-of-view. Same as {@code render(pGraphics, type, eye, zNear, zFar, true)}.
   *
   * @see #render(Object)
   * @see #render(PGraphics, Type, Node, float, float, boolean)
   * @see #render()
   */
  public void render(PGraphics pGraphics, Type type, Node eye, float zNear, float zFar) {
    render(pGraphics, type, eye, zNear, zFar, true);
  }

  /**
   * Renders the scene onto {@code pGraphics} using the {@code eye} node point of view and
   * remaining frustum parameters. Useful to compute a shadow map taking the {@code eye}
   * as the light point-of-view. Same as
   * {@code render(pGraphics, eye.view(), eye.projection(type, pGraphics.width, pGraphics.height, zNear, zFar, leftHanded))}.
   *
   * @see #render(MatrixHandler, Object, Matrix, Matrix)
   * @see #render(Object)
   * @see #render(PGraphics, Type, Node, float, float)
   * @see #render()
   */
  public void render(PGraphics pGraphics, Type type, Node eye, float zNear, float zFar, boolean leftHanded) {
    if (pGraphics instanceof PGraphicsOpenGL)
      render(pGraphics, type, eye, pGraphics.width, pGraphics.height, zNear, zFar, leftHanded);
    else
      System.out.println("Nothing done: pg should be instance of PGraphicsOpenGL in render()");
  }

  @Override
  public MatrixHandler matrixHandler(Object context) {
    if (!(context instanceof PGraphicsOpenGL))
      return new Java2DMatrixHandler(this);
    return new GLMatrixHandler((PGraphicsOpenGL) context);
  }

  // HUD

  /**
   * Same as {@code beginHUD(context())}.
   *
   * @see #context()
   * @see #beginHUD(PGraphics)
   */
  @Override
  public void beginHUD() {
    beginHUD(context());
  }

  /**
   * Begins Heads Up Display (HUD) on the {@code pGraphics} so that drawing can be done
   * using 2D screen coordinates. Don't forget to call {@link #endHUD(PGraphics)} after screen
   * drawing ends.
   * <p>
   * All screen drawing should be enclosed between {@link #beginHUD(PGraphics)} and
   * {@link #endHUD(PGraphics)}. Then you can just begin drawing your screen nodes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d graph
   * then draw first all your 3d before doing any call to a {@link #beginHUD(PGraphics)}
   * and {@link #endHUD(PGraphics)} pair.
   *
   * @see #endHUD(PGraphics)
   * @see #beginHUD()
   */
  public void beginHUD(PGraphics pGraphics) {
    if (_hudCalls != 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    _hudCalls++;
    // Otherwise Processing says: "Optimized strokes can only be disabled in 3D"
    if (is3D())
      pGraphics.hint(PApplet.DISABLE_OPTIMIZED_STROKE);
    disableDepthTest(pGraphics);
    // if-else same as:
    // matrixHandler(p).beginHUD();
    // but perhaps a bit more efficient
    if (pGraphics == context())
      _matrixHandler.beginHUD(width(), height());
    else
      matrixHandler(pGraphics).beginHUD(pGraphics.width, pGraphics.height);
  }

  /**
   * Same as {@code endHUD(context())}.
   *
   * @see #context()
   * @see #endHUD(PGraphics)
   */
  @Override
  public void endHUD() {
    endHUD(context());
  }

  /**
   * Ends Heads Up Display (HUD) on the {@code pGraphics}. See {@link #beginHUD(PGraphics)} for details.
   *
   * @see #beginHUD(PGraphics)
   * @see #endHUD()
   */
  public void endHUD(PGraphics pGraphics) {
    _hudCalls--;
    if (_hudCalls != 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    // if-else same as:
    // matrixHandler(p).endHUD();
    // but perhaps a bit more efficient
    if (pGraphics == context())
      _matrixHandler.endHUD();
    else
      matrixHandler(pGraphics).endHUD();
    enableDepthTest(pGraphics);
    // Otherwise Processing says: "Optimized strokes can only be disabled in 3D"
    if (is3D())
      pGraphics.hint(PApplet.ENABLE_OPTIMIZED_STROKE);
  }

  // drawing

  /**
   * Same as {@code vertex(context(), v)}.
   *
   * @see #vertex(PGraphics, float[])
   */
  public void vertex(float[] v) {
    vertex(context(), v);
  }

  /**
   * Wrapper for PGraphics.vertex(v)
   */
  public static void vertex(PGraphics pGraphics, float[] v) {
    pGraphics.vertex(v);
  }

  /**
   * Same as {@code if (this.is2D()) vertex(context(), x, y); else vertex(context(), x, y, z)}.
   *
   * @see #vertex(PGraphics, float, float, float)
   */
  public void vertex(float x, float y, float z) {
    if (is2D())
      vertex(context(), x, y);
    else
      vertex(context(), x, y, z);
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
   * Same as {@code if (this.is2D()) vertex(context(), x, y, u, v); else
   * vertex(context(), x, y, z, u, v);}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   * @see #vertex(PGraphics, float, float, float, float, float)
   */
  public void vertex(float x, float y, float z, float u, float v) {
    if (is2D())
      vertex(context(), x, y, u, v);
    else
      vertex(context(), x, y, z, u, v);
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
   * Same as {@code vertex(context(), x, y)}.
   *
   * @see #vertex(PGraphics, float, float)
   */
  public void vertex(float x, float y) {
    vertex(context(), x, y);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y)
   */
  public static void vertex(PGraphics pGraphics, float x, float y) {
    pGraphics.vertex(x, y);
  }

  /**
   * Same as {@code vertex(context(), x, y, u, v)}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   */
  public void vertex(float x, float y, float u, float v) {
    vertex(context(), x, y, u, v);
  }

  /**
   * Wrapper for PGraphics.vertex(x,y,u,v)
   */
  public static void vertex(PGraphics pGraphics, float x, float y, float u, float v) {
    pGraphics.vertex(x, y, u, v);
  }

  /**
   * Same as {@code if (this.is2D()) line(context(), x1, y1, x2, y2);
   * else line(context(), x1, y1, z1, x2, y2, z2);}.
   *
   * @see #line(PGraphics, float, float, float, float, float, float)
   */
  public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
    if (is2D())
      line(context(), x1, y1, x2, y2);
    else
      line(context(), x1, y1, z1, x2, y2, z2);
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
   * Same as {@code context().line(x1, y1, x2, y2)}.
   *
   * @see #line(PGraphics, float, float, float, float)
   */
  public void line(float x1, float y1, float x2, float y2) {
    line(context(), x1, y1, x2, y2);
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
  public static Vector toVector(PVector pVector) {
    return new Vector(pVector.x, pVector.y, pVector.z);
  }

  /**
   * Converts a {@link Matrix} to a PMatrix3D.
   */
  public static PMatrix3D toPMatrix(Matrix matrix) {
    float[] a = matrix.get(new float[16], false);
    return new PMatrix3D(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14],
        a[15]);
  }

  /**
   * Converts a PMatrix3D to a {@link Matrix}.
   */
  public static Matrix toMatrix(PMatrix3D pMatrix3D) {
    return new Matrix(pMatrix3D.get(new float[16]), false);
  }

  /**
   * Converts a PMatrix2D to a {@link Matrix}.
   */
  public static Matrix toMatrix(PMatrix2D pMatrix2D) {
    return toMatrix(new PMatrix3D(pMatrix2D));
  }

  /**
   * Converts a {@link Matrix} to a PMatrix2D.
   */
  public static PMatrix2D toPMatrix2D(Matrix matrix) {
    float[] a = matrix.get(new float[16], false);
    return new PMatrix2D(a[0], a[1], a[3], a[4], a[5], a[7]);
  }

  // DRAWING

  /**
   * Convenience function that simply calls {@code drawPath(interpolator, isEye(interpolator.node()) ? 3 : 5, 6, radius())}.
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator) {
    drawPath(interpolator, isEye(interpolator.node()) ? 3 : 5, 6, radius());
  }

  /**
   * Convenience function that simply calls {@code drawPath(interpolator, mask, 6, radius())}
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator, int mask) {
    drawPath(interpolator, mask, 6, radius());
  }

  /**
   * Convenience function that simply calls {@code drawPath(interpolator, mask, count, radius())}.
   *
   * @see #drawPath(Interpolator, int, int, float)
   */
  public void drawPath(Interpolator interpolator, int mask, int steps) {
    drawPath(interpolator, mask, steps, radius());
  }

  /**
   * Draws the {@link Interpolator} path.
   * <p>
   * {@code mask} controls what is drawn: If ( (mask &amp; 1) != 0 ), the position path is
   * drawn. If ( (mask &amp; 2) != 0 ), an eye representation is regularly drawn and if
   * ( (mask &amp; 4) != 0 ), oriented axes are regularly drawn. Examples:
   * <p>
   * {@code drawPath(1); // Simply draws the interpolation path} <br>
   * {@code drawPath(3); // Draws path and eyes} <br>
   * {@code drawPath(5); // Draws path and axes} <br>
   * <p>
   * In the case where the eye or axes are drawn, {@code steps} controls the number of
   * objects (axes or eyes) drawn between two successive keyframes. When
   * {@code steps = 1}, only the keyframes are drawn, when {@code steps = 2} it
   * also draws the intermediate orientation, etc. The maximum value is 30, so that an object
   * is drawn for each keyframe. Default value is 6.
   * <p>
   * {@code scale} controls the scaling of the eye and axes drawing. A value of
   * {@link #radius()} should give good results.
   */
  public void drawPath(Interpolator interpolator, int mask, int steps, float scale) {
    context().pushStyle();
    if (mask != 0) {
      int nbSteps = 30;
      context().strokeWeight(2 * context().strokeWeight);
      context().noFill();
      List<Node> path = interpolator.path();
      if (((mask & 1) != 0) && path.size() > 1) {
        context().beginShape();
        for (Node node : path)
          vertex(node.position().x(), node.position().y(), node.position().z());
        context().endShape();
      }
      if ((mask & 6) != 0) {
        int count = 0;
        if (steps > nbSteps)
          steps = nbSteps;
        float goal = 0.0f;
        for (Node node : path)
          if ((count++) >= goal) {
            goal += nbSteps / (float) steps;
            _matrixHandler.pushMatrix();
            _matrixHandler.applyTransformation(node);
            if ((mask & 2) != 0)
              _drawEye(scale);
            if ((mask & 4) != 0)
              drawAxes(scale / 10.0f);
            _matrixHandler.popMatrix();
          }
      }
      context().strokeWeight(context().strokeWeight / 2f); // draw the picking targets:
      /*
      // TODO picking targets currently broken, requires attach nodes in interpolator._list to interpolator._path
      for (Node node : interpolator.path())
        if(node.isAttached(this))
          drawBullsEye(node);
       */
    }
    // draw the picking targets:
    for (Node node : interpolator.keyFrames().values())
      drawBullsEye(node);
    context().popStyle();
  }

  /**
   * Internal use.
   */
  protected void _drawEye(float scale) {
    context().pushStyle();
    float halfHeight = scale * (is2D() ? 1.2f : 0.07f);
    float halfWidth = halfHeight * 1.3f;
    float dist = halfHeight / (float) Math.tan(PApplet.PI / 8.0f);

    float arrowHeight = 1.5f * halfHeight;
    float baseHeight = 1.2f * halfHeight;
    float arrowHalfWidth = 0.5f * halfWidth;
    float baseHalfWidth = 0.3f * halfWidth;

    // Frustum outline
    context().noFill();
    context().beginShape();
    vertex(-halfWidth, halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(halfWidth, -halfHeight, -dist);
    vertex(-halfWidth, -halfHeight, -dist);
    context().endShape();
    context().noFill();
    context().beginShape();
    vertex(halfWidth, -halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    vertex(0.0f, 0.0f, 0.0f);
    vertex(-halfWidth, halfHeight, -dist);
    vertex(halfWidth, halfHeight, -dist);
    context().endShape();

    // Up arrow
    context().noStroke();
    context().fill(context().strokeColor);
    // Base
    context().beginShape(PApplet.QUADS);

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

    context().endShape();
    // Arrow
    context().beginShape(PApplet.TRIANGLES);

    if (isLeftHanded()) {
      vertex(0.0f, -arrowHeight, -dist);
      vertex(arrowHalfWidth, -baseHeight, -dist);
      vertex(-arrowHalfWidth, -baseHeight, -dist);
    } else {
      vertex(0.0f, arrowHeight, -dist);
      vertex(-arrowHalfWidth, baseHeight, -dist);
      vertex(arrowHalfWidth, baseHeight, -dist);
    }
    context().endShape();
    context().popStyle();
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
    _matrixHandler.translate(0.0f, 0.0f, length * (1.0f - head));
    drawCone(coneRadiusCoef * radius, head * length);
    _matrixHandler.translate(0.0f, 0.0f, -length * (1.0f - head));
  }

  /**
   * Same as {@code drawArrow(vector, 0.05f * vector.magnitude())}.
   *
   * @see #drawArrow(Vector, float)
   */
  public void drawArrow(Vector vector) {
    drawArrow(vector, 0.05f * vector.magnitude());
  }

  /**
   * Same as {@code drawArrow(new Vector(), vector, radius)}.
   *
   * @see #drawArrow(Vector, Vector, float)
   */
  public void drawArrow(Vector vector, float radius) {
    drawArrow(new Vector(), vector, radius);
  }

  /**
   * Draws an arrow of {@code radius} between {@code from} and the 3D point {@code to}.
   *
   * @see #drawArrow(float, float)
   */
  public void drawArrow(Vector from, Vector to, float radius) {
    _matrixHandler.pushMatrix();
    _matrixHandler.translate(from.x(), from.y(), from.z());
    _matrixHandler.applyMatrix(new Quaternion(new Vector(0, 0, 1), Vector.subtract(to, from)).matrix());
    drawArrow(Vector.subtract(to, from).magnitude(), radius);
    _matrixHandler.popMatrix();
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
   * Same as {@code drawCylinder(context(), detail, radius, height)}.
   *
   * @see #drawCylinder(PGraphics, int, float, float)
   */
  public void drawCylinder(int detail, float radius, float height) {
    drawCylinder(context(), detail, radius, height);
  }

  /**
   * Same as {@code drawCylinder(context, radius()/6, radius()/3)}.
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
   * Same as {@code drawHollowCylinder(context(), radius, height, normal1, normal2)}.
   *
   * @see #drawHollowCylinder(PGraphics, float, float, Vector, Vector)
   */
  public void drawHollowCylinder(float radius, float height, Vector normal1, Vector normal2) {
    drawHollowCylinder(context(), radius, height, normal1, normal2);
  }

  /**
   * Same as {@code drawHollowCylinder(context(), detail, radius, height, normal1, normal2)}.
   *
   * @see #drawHollowCylinder(PGraphics, int, float, float, Vector, Vector)
   * @see #drawCylinder(float, float)
   */
  public void drawHollowCylinder(int detail, float radius, float height, Vector normal1, Vector normal2) {
    drawHollowCylinder(context(), detail, radius, height, normal1, normal2);
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
   * Same as {@code drawCone(context(), detail, x, y, radius, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float radius, float height) {
    drawCone(context(), detail, x, y, radius, height);
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
    float[] unitConeX = new float[detail + 1];
    float[] unitConeY = new float[detail + 1];

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
   * Same as {@code drawCone(context(), detail, x, y, radius1, radius2, height)}.
   *
   * @see #drawCone(PGraphics, int, float, float, float, float, float)
   * @see #drawCone(int, float, float, float, float)
   */
  public void drawCone(int detail, float x, float y, float radius1, float radius2, float height) {
    drawCone(context(), detail, x, y, radius1, radius2, height);
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
    float[] firstCircleX = new float[detail + 1];
    float[] firstCircleY = new float[detail + 1];
    float[] secondCircleX = new float[detail + 1];
    float[] secondCircleY = new float[detail + 1];

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
   * Convenience function that simply calls {@code drawAxes(radius())}.
   *
   * @see #drawAxes(float)
   */
  public void drawAxes() {
    drawAxes(radius());
  }

  /**
   * Same as {@code drawAxes(context(), length, isLeftHanded())}.
   *
   * @see #drawAxes(PGraphics, float, boolean)
   */
  public void drawAxes(float length) {
    drawAxes(context(), length, isLeftHanded());
  }

  /**
   * Same as {@code drawAxes(pGraphics, radius(), isLeftHanded())}.
   *
   * @see #drawAxes(PGraphics, float, boolean)
   */
  public void drawAxes(PGraphics pGraphics) {
    drawAxes(pGraphics, radius(), isLeftHanded());
  }

  /**
   * Same as {@code drawAxes(pGraphics, length, true)}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public static void drawAxes(PGraphics pGraphics, float length) {
    drawAxes(pGraphics, length, true);
  }

  /**
   * Draws axes of {@code length} onto {@code pGraphics} taking into account
   * {@code leftHanded}.
   */
  public static void drawAxes(PGraphics pGraphics, float length, boolean leftHanded) {
    pGraphics.pushStyle();
    pGraphics.colorMode(PApplet.RGB, 255);
    float charWidth = length / 40.0f;
    float charHeight = length / 30.0f;
    float charShift = 1.04f * length;

    pGraphics.pushStyle();
    pGraphics.beginShape(PApplet.LINES);
    pGraphics.strokeWeight(2);
    if (pGraphics.is2D()) {
      // The X
      pGraphics.stroke(200, 0, 0);
      pGraphics.vertex(charShift + charWidth, -charHeight);
      pGraphics.vertex(charShift - charWidth, charHeight);
      pGraphics.vertex(charShift - charWidth, -charHeight);
      pGraphics.vertex(charShift + charWidth, charHeight);

      // The Y
      charShift *= 1.02;
      pGraphics.stroke(0, 200, 0);
      pGraphics.vertex(charWidth, charShift + (!leftHanded ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(-charWidth, charShift + (!leftHanded ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(0.0f, charShift + 0.0f);
      pGraphics.vertex(0.0f, charShift + -(!leftHanded ? charHeight : -charHeight));
    } else {
      // The X
      pGraphics.stroke(200, 0, 0);
      pGraphics.vertex(charShift, charWidth, -charHeight);
      pGraphics.vertex(charShift, -charWidth, charHeight);
      pGraphics.vertex(charShift, -charWidth, -charHeight);
      pGraphics.vertex(charShift, charWidth, charHeight);
      // The Y
      pGraphics.stroke(0, 200, 0);
      pGraphics.vertex(charWidth, charShift, (leftHanded ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(-charWidth, charShift, (leftHanded ? charHeight : -charHeight));
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(0.0f, charShift, 0.0f);
      pGraphics.vertex(0.0f, charShift, -(leftHanded ? charHeight : -charHeight));
      // The Z
      pGraphics.stroke(0, 100, 200);
      pGraphics.vertex(-charWidth, !leftHanded ? charHeight : -charHeight, charShift);
      pGraphics.vertex(charWidth, !leftHanded ? charHeight : -charHeight, charShift);
      pGraphics.vertex(charWidth, !leftHanded ? charHeight : -charHeight, charShift);
      pGraphics.vertex(-charWidth, !leftHanded ? -charHeight : charHeight, charShift);
      pGraphics.vertex(-charWidth, !leftHanded ? -charHeight : charHeight, charShift);
      pGraphics.vertex(charWidth, !leftHanded ? -charHeight : charHeight, charShift);
    }
    pGraphics.endShape();
    pGraphics.popStyle();

    // X Axis
    pGraphics.stroke(200, 0, 0);
    if (pGraphics.is2D())
      pGraphics.line(0, 0, length, 0);
    else
      pGraphics.line(0, 0, 0, length, 0, 0);
    // Y Axis
    pGraphics.stroke(0, 200, 0);
    if (pGraphics.is2D())
      pGraphics.line(0, 0, 0, length);
    else
      pGraphics.line(0, 0, 0, 0, length, 0);

    // Z Axis
    if (pGraphics.is3D()) {
      pGraphics.stroke(0, 100, 200);
      pGraphics.line(0, 0, 0, 0, 0, length);
    }
    pGraphics.popStyle();
  }

  /**
   * Convenience function that simply calls {@code drawGrid(radius(), 10)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid() {
    drawGrid(radius(), 10);
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
   * Convenience function that simply calls {@code drawGrid(radius(), subdivisions)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid(int subdivisions) {
    drawGrid(radius(), subdivisions);
  }

  /**
   * Same as {@code drawGrid(context(), size, subdivisions)}.
   *
   * @see #drawGrid(PGraphics, float, int)
   * @see #drawAxes(float)
   */
  public void drawGrid(float size, int subdivisions) {
    drawGrid(context(), size, subdivisions);
  }

  /**
   * Same as {@code drawGrid(pGraphics, radius(), 10)}.
   *
   * @see #drawGrid(PGraphics, float, int)
   * @see #drawAxes(float)
   */
  public void drawGrid(PGraphics pGraphics) {
    drawGrid(pGraphics, radius(), 10);
  }

  /**
   * Draws a grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public static void drawGrid(PGraphics pGraphics, float size, int subdivisions) {
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
   * Convenience function that simply calls {@code drawDottedGrid(radius(), 10)}.
   *
   * @see #drawDottedGrid(float, int)
   */
  public void drawDottedGrid() {
    drawDottedGrid(radius(), 10);
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
   * Convenience function that simplt calls {@code drawDottedGrid(radius(), subdivisions)}.
   *
   * @see #drawDottedGrid(float, int)
   */
  public void drawDottedGrid(int subdivisions) {
    drawDottedGrid(radius(), subdivisions);
  }

  /**
   * Same as {@code drawDottedGrid(context(), size, subdivisions)}.
   *
   * @see #drawDottedGrid(PGraphics, float, int)
   */
  public void drawDottedGrid(float size, int subdivisions) {
    drawDottedGrid(context(), size, subdivisions);
  }

  /**
   * Same as {@code drawDottedGrid(pGraphics, radius(), 10)}.
   *
   * @see #drawDottedGrid(PGraphics, float, int)
   */
  public void drawDottedGrid(PGraphics pGraphics) {
    drawDottedGrid(pGraphics, radius(), 10);
  }

  /**
   * Draws a dotted-grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public static void drawDottedGrid(PGraphics pGraphics, float size, int subdivisions) {
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

  /**
   * Applies the {@code graph.eye()} transformation (see {@link #applyTransformation(Node)})
   * and then calls {@link #drawFrustum(PGraphics, Graph)} on the scene {@link #context()}.
   *
   * @see #drawFrustum(PGraphics, Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float, boolean)
   */
  public void drawFrustum(Graph graph) {
    _matrixHandler.pushMatrix();
    _matrixHandler.applyTransformation(graph.eye());
    drawFrustum(context(), graph);
    _matrixHandler.popMatrix();
  }

  /**
   * Draws a representation of the viewing frustum onto {@code pGraphics} according to
   * {@code graph.eye()} and {@code graph.type()}.
   * <p>
   * Note that if {@code pGraphics == graph.context()} this method has not effect at all.
   *
   * @see #drawFrustum(Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float, boolean)
   */
  public void drawFrustum(PGraphics pGraphics, Graph graph) {
    if (pGraphics == graph.context())
      return;
    // texturing requires graph.isOffscreen() (third condition) otherwise got
    // "The pixels array is null" message and the frustum near plane texture and contour are missed
    boolean texture = pGraphics instanceof PGraphicsOpenGL && graph instanceof Scene && graph.isOffscreen();
    switch (graph.type()) {
      case TWO_D:
      case ORTHOGRAPHIC:
        _drawOrthographicFrustum(pGraphics, graph.eye().magnitude(), graph.width(), graph.isLeftHanded() ? -graph.height() : graph.height(), graph.zNear(), graph.zFar(), texture ? ((Scene) graph).context() : null);
        break;
      case PERSPECTIVE:
        _drawPerspectiveFrustum(pGraphics, graph.eye().magnitude(), graph.isLeftHanded() ? -graph.aspectRatio() : graph.aspectRatio(), graph.zNear(), graph.zFar(), texture ? ((Scene) graph).context() : null);
        break;
    }
  }

  /**
   * Same as {@code drawFrustum(pGraphics, eyeBuffer, type, eye, zNear, zFar, true)}.
   *
   * @see #drawFrustum(Graph)
   * @see #drawFrustum(PGraphics, Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float, boolean)
   */
  public static void drawFrustum(PGraphics pGraphics, PGraphics eyeBuffer, Type type, Node eye, float zNear, float zFar) {
    drawFrustum(pGraphics, eyeBuffer, type, eye, zNear, zFar, true);
  }

  /**
   * Draws a representation of the {@code eyeBuffer} frustum onto {@code pGraphics} according to frustum parameters:
   * {@code type}, eye {@link Node#magnitude()}, {@code zNear} and {@code zFar}, while taking into account
   * whether or not the scene is {@code leftHanded}.
   * <p>
   * Use it in conjunction with {@link #render(PGraphics, Type, Node, float, float, boolean)} as when rendering
   * a shadow map.
   *
   * @see #drawFrustum(Graph)
   * @see #drawFrustum(PGraphics, Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Type, Node, float, float)
   */
  public static void drawFrustum(PGraphics pGraphics, PGraphics eyeBuffer, Type type, Node eye, float zNear, float zFar, boolean leftHanded) {
    switch (type) {
      case TWO_D:
      case ORTHOGRAPHIC:
        _drawOrthographicFrustum(pGraphics, eye.magnitude(), eyeBuffer.width, leftHanded ? -eyeBuffer.height : eyeBuffer.height, zNear, zFar, eyeBuffer);
        break;
      case PERSPECTIVE:
        _drawPerspectiveFrustum(pGraphics, eye.magnitude(), leftHanded ? -eyeBuffer.width / eyeBuffer.height : eyeBuffer.width / eyeBuffer.height, zNear, zFar, eyeBuffer);
        break;
    }
  }

  protected static void _drawOrthographicFrustum(PGraphics pGraphics, float magnitude, float width, float height, float zNear, float zFar, PGraphics eyeBuffer) {
    if (pGraphics == eyeBuffer)
      return;
    boolean threeD = pGraphics.is3D();
    boolean lh = height < 0;
    height = Math.abs(height);

    pGraphics.pushStyle();

    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector[] points = new Vector[2];
    points[0] = new Vector();
    points[1] = new Vector();

    points[0].setX(width / 2);
    points[1].setX(width / 2);
    points[0].setY(height / 2);
    points[1].setY(height / 2);
    if (threeD) {
      points[0].setZ(zNear / magnitude);
      points[1].setZ(zFar / magnitude);
      // Frustum lines
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
    }

    // Up arrow
    float arrowHeight = 1.5f * points[0].y();
    float baseHeight = 1.2f * points[0].y();
    float arrowHalfWidth = 0.5f * points[0].x();
    float baseHalfWidth = 0.3f * points[0].x();

    pGraphics.noStroke();
    // Arrow base
    if (eyeBuffer != null) {
      pGraphics.pushStyle();// end at arrow
      pGraphics.colorMode(PApplet.RGB, 255);
      float r = pGraphics.red(pGraphics.fillColor);
      float g = pGraphics.green(pGraphics.fillColor);
      float b = pGraphics.blue(pGraphics.fillColor);
      pGraphics.fill(r, g, b, 126);// same transparency as near plane texture
    }
    pGraphics.beginShape(PApplet.QUADS);
    if (lh) {
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
    if (lh) {
      Scene.vertex(pGraphics, 0.0f, -arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pGraphics, 0.0f, arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, baseHeight, -points[0].z());
    }
    if (eyeBuffer != null)
      pGraphics.popStyle();// begin at arrow base
    pGraphics.endShape();

    // Planes
    // far plane
    if (threeD)
      _drawPlane(pGraphics, null, points[1], new Vector(0, 0, -1), lh);
    // near plane
    _drawPlane(pGraphics, eyeBuffer, points[0], new Vector(0, 0, 1), lh);

    pGraphics.popStyle();
  }

  protected static void _drawPerspectiveFrustum(PGraphics pGraphics, float magnitude, float aspectRatio, float zNear, float zFar, PGraphics eyeBuffer) {
    if (pGraphics == eyeBuffer)
      return;
    boolean lh = aspectRatio < 0;
    aspectRatio = Math.abs(aspectRatio);

    pGraphics.pushStyle();

    // 0 is the upper left coordinates of the near corner, 1 for the far one
    Vector[] points = new Vector[2];
    points[0] = new Vector();
    points[1] = new Vector();

    points[0].setZ(zNear / magnitude);
    points[1].setZ(zFar / magnitude);
    //(2 * (float) Math.atan(eye().magnitude()))
    //points[0].setY(points[0].z() * PApplet.tan(((2 * (float) Math.atan(eye().magnitude())) / 2.0f)));
    points[0].setY(points[0].z() * magnitude);
    points[0].setX(points[0].y() * aspectRatio);
    float ratio = points[1].z() / points[0].z();
    points[1].setY(ratio * points[0].y());
    points[1].setX(ratio * points[0].x());

    // Frustum lines
    pGraphics.beginShape(PApplet.LINES);
    Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
    Scene.vertex(pGraphics, points[1].x(), points[1].y(), -points[1].z());
    Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
    Scene.vertex(pGraphics, -points[1].x(), points[1].y(), -points[1].z());
    Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
    Scene.vertex(pGraphics, -points[1].x(), -points[1].y(), -points[1].z());
    Scene.vertex(pGraphics, 0.0f, 0.0f, 0.0f);
    Scene.vertex(pGraphics, points[1].x(), -points[1].y(), -points[1].z());
    pGraphics.endShape();

    // Up arrow
    float arrowHeight = 1.5f * points[0].y();
    float baseHeight = 1.2f * points[0].y();
    float arrowHalfWidth = 0.5f * points[0].x();
    float baseHalfWidth = 0.3f * points[0].x();

    pGraphics.noStroke();
    // Arrow base
    if (eyeBuffer != null) {
      pGraphics.pushStyle();// end at arrow
      pGraphics.colorMode(PApplet.RGB, 255);
      float r = pGraphics.red(pGraphics.fillColor);
      float g = pGraphics.green(pGraphics.fillColor);
      float b = pGraphics.blue(pGraphics.fillColor);
      pGraphics.fill(r, g, b, 126);// same transparency as near plane texture
    }
    pGraphics.beginShape(PApplet.QUADS);
    if (lh) {
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
    if (lh) {
      Scene.vertex(pGraphics, 0.0f, -arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, -baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, -baseHeight, -points[0].z());
    } else {
      Scene.vertex(pGraphics, 0.0f, arrowHeight, -points[0].z());
      Scene.vertex(pGraphics, -arrowHalfWidth, baseHeight, -points[0].z());
      Scene.vertex(pGraphics, arrowHalfWidth, baseHeight, -points[0].z());
    }
    if (eyeBuffer != null)
      pGraphics.popStyle();// begin at arrow base
    pGraphics.endShape();

    // Planes
    // far plane
    _drawPlane(pGraphics, null, points[1], new Vector(0, 0, -1), lh);
    // near plane
    _drawPlane(pGraphics, eyeBuffer, points[0], new Vector(0, 0, 1), lh);

    pGraphics.popStyle();
  }

  protected static void _drawPlane(PGraphics pGraphics, PGraphics eyeBuffer, Vector corner, Vector normal, boolean lh) {
    if (pGraphics == eyeBuffer)
      return;
    pGraphics.pushStyle();
    // near plane
    pGraphics.beginShape(PApplet.QUAD);
    pGraphics.normal(normal.x(), normal.y(), normal.z());
    if (eyeBuffer != null) {
      pGraphics.textureMode(NORMAL);
      pGraphics.tint(255, 126); // Apply transparency without changing color
      pGraphics.texture(eyeBuffer);
      Scene.vertex(pGraphics, corner.x(), corner.y(), -corner.z(), 1, lh ? 1 : 0);
      Scene.vertex(pGraphics, -corner.x(), corner.y(), -corner.z(), 0, lh ? 1 : 0);
      Scene.vertex(pGraphics, -corner.x(), -corner.y(), -corner.z(), 0, lh ? 0 : 1);
      Scene.vertex(pGraphics, corner.x(), -corner.y(), -corner.z(), 1, lh ? 0 : 1);
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
   * Same as {@code drawProjector(context(), graph, vector)}.
   *
   * @see #drawProjector(PGraphics, Graph, Vector)
   * @see #drawProjectors(Graph, List)
   */
  public void drawProjector(Graph graph, Vector vector) {
    drawProjector(context(), graph, vector);
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
   * Same as {@code drawProjectors(context(), graph, points)}.
   *
   * @see #drawProjectors(PGraphics, Graph, List)
   * @see #drawProjector(Graph, Vector)
   */
  public void drawProjectors(Graph graph, List<Vector> points) {
    drawProjectors(context(), graph, points);
  }

  /**
   * Draws the projection of each point in {@code points} in the near plane onto {@code pGraphics}.
   * <p>
   * This method should be used in conjunction with {@link #drawFrustum(PGraphics, Graph)}.
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
        matrixHandler(pGraphics).applyTransformation(graph.eye());
      }
      // in PERSPECTIVE cache the transformed origin
      else
        o = graph.eye().worldLocation(new Vector());
      pGraphics.beginShape(PApplet.LINES);
      for (Vector s : points) {
        if (graph.type() == Graph.Type.ORTHOGRAPHIC) {
          Vector v = graph.eye().location(s);
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
   * {@link #drawCross(float, float, float)} centered at the projected node origin.
   * If node is a Node instance the length of the cross is the node
   * {@link Node#pickingThreshold()}, otherwise it's {@link #radius()} / 5.
   * If node a Node instance and it is {@link #hasNullTag(Node)} it also applies
   * a stroke highlight.
   *
   * @see #drawSquaredBullsEye(Node, float)
   */
  public void drawCross(Node node) {
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    drawCross(node, node.pickingThreshold() < 1 ? 200 * node.pickingThreshold() * node.scaling() * pixelToGraphRatio(node.position()) : node.pickingThreshold());
    context().popStyle();
  }

  /**
   * {@link #drawCross(float, float, float)} centered at the projected node origin, having
   * {@code length} pixels.
   *
   * @see #drawCross(float, float, float)
   */
  public void drawCross(Node node, float length) {
    if (eye() == node) {
      System.err.println("eye nodes don't have an screen target");
      return;
    }
    Vector center = screenLocation(node.position());
    drawCross(center.x(), center.y(), length);
  }

  /**
   * Convenience function that simply calls {@code drawCross(x, y, radius()/5)}.
   *
   * @see #drawCross(float, float, float)
   */
  public void drawCross(float x, float y) {
    drawCross(x, y, radius() / 5);
  }

  /**
   * Same as {@code drawCross(context(), x, y, length)}.
   *
   * @see #drawCross(PGraphics, float, float, float)
   */
  public void drawCross(float x, float y, float length) {
    drawCross(context(), x, y, length);
  }

  /**
   * Draws a cross on the screen centered under pixel {@code (x, y)}, and edge of size
   * {@code length} onto {@code pGraphics}.
   */
  public void drawCross(PGraphics pGraphics, float x, float y, float length) {
    float half_size = length / 2f;
    pGraphics.pushStyle();
    beginHUD(pGraphics);
    pGraphics.noFill();
    pGraphics.beginShape(LINES);
    vertex(pGraphics, x - half_size, y);
    vertex(pGraphics, x + half_size, y);
    vertex(pGraphics, x, y - half_size);
    vertex(pGraphics, x, y + half_size);
    pGraphics.endShape();
    endHUD(pGraphics);
    pGraphics.popStyle();
  }

  /**
   * Draws a bullseye around the node {@link Node#position()} projection.
   * <p>
   * The shape of the bullseye may be squared or circled depending on the node
   * {@link Node#pickingThreshold()} sign.
   *
   * @see Node#pickingThreshold()
   * @see Node#position()
   * @see #drawSquaredBullsEye(Node)
   * @see #drawCircledBullsEye(Node)
   */
  public void drawBullsEye(Node node) {
    if (node.pickingThreshold() > 0)
      this.drawSquaredBullsEye(node);
    else if (node.pickingThreshold() < 0)
      this.drawCircledBullsEye(node);
  }

  /**
   * {@link #drawSquaredBullsEye(float, float, float)} centered at the projected node origin.
   * The length of the target is the node {@link Node#pickingThreshold()}.
   * If node {@link #hasNullTag(Node)} it also applies a stroke highlight.
   *
   * @see #drawSquaredBullsEye(Node, float)
   * @see #drawCircledBullsEye(Node)
   * @see #drawBullsEye(Node)
   */
  public void drawSquaredBullsEye(Node node) {
    if (node.pickingThreshold() == 0)
      return;
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    drawSquaredBullsEye(node, Math.abs(node.pickingThreshold()) < 1 ? 200 * Math.abs(node.pickingThreshold()) * node.scaling() * pixelToGraphRatio(node.position())
        : Math.abs(node.pickingThreshold()));
    context().popStyle();
  }

  /**
   * {@link #drawSquaredBullsEye(float, float, float)} centered at the projected node origin, having
   * {@code length} pixels.
   *
   * @see #drawSquaredBullsEye(float, float, float)
   * @see #drawCircledBullsEye(float, float, float)
   */
  public void drawSquaredBullsEye(Node node, float length) {
    if (eye() == node) {
      System.err.println("eye nodes don't have an screen target");
      return;
    }
    Vector center = screenLocation(node.position());
    drawSquaredBullsEye(center.x(), center.y(), length);
  }

  /**
   * Same as {@code drawSquaredBullsEye(context(), x, y, radius() / 5)}.
   *
   * @see #drawSquaredBullsEye(float, float, float)
   * @see #drawCircledBullsEye(float, float, float)
   */
  public void drawSquaredBullsEye(float x, float y) {
    drawSquaredBullsEye(context(), x, y, radius() / 5);
  }

  /**
   * Same as {@code drawSquaredBullsEye(context(), center, length)}.
   *
   * @see #drawSquaredBullsEye(PGraphics, float, float, float)
   * @see #drawCircledBullsEye(PGraphics, float, float, float)
   */
  public void drawSquaredBullsEye(float x, float y, float length) {
    drawSquaredBullsEye(context(), x, y, length);
  }

  /**
   * Draws a squared bullseye onto {@code pGraphics}, centered at {@code (x, y)},
   * having {@code length} pixels.
   *
   * @see #drawCircledBullsEye(PGraphics, float, float, float)
   */
  public void drawSquaredBullsEye(PGraphics pGraphics, float x, float y, float length) {
    float half_length = length / 2f;
    pGraphics.pushStyle();
    beginHUD(pGraphics);
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
    endHUD(pGraphics);
    drawCross(x, y, 0.6f * length);
    pGraphics.popStyle();
  }

  /**
   * {@link #drawCircledBullsEye(float, float, float)} centered at the projected node origin.
   * The length of the target is the node {@link Node#pickingThreshold()}.
   * If node {@link #hasNullTag(Node)} it also applies a stroke highlight.
   *
   * @see #drawSquaredBullsEye(Node, float)
   * @see #drawCircledBullsEye(Node)
   * @see #drawBullsEye(Node)
   */
  public void drawCircledBullsEye(Node node) {
    if (node.pickingThreshold() == 0)
      return;
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    drawCircledBullsEye(node, Math.abs(node.pickingThreshold()) < 1 ? 200 * Math.abs(node.pickingThreshold()) * node.scaling() * pixelToGraphRatio(node.position())
        : Math.abs(node.pickingThreshold()));
    context().popStyle();
  }

  /**
   * {@link #drawCircledBullsEye(float, float, float)} centered at the projected node origin, having
   * {@code length} pixels.
   *
   * @see #drawCircledBullsEye(float, float, float)
   * @see #drawSquaredBullsEye(float, float, float)
   */
  public void drawCircledBullsEye(Node node, float length) {
    if (eye() == node) {
      System.err.println("eye nodes don't have an screen target");
      return;
    }
    Vector center = screenLocation(node.position());
    drawCircledBullsEye(center.x(), center.y(), length);
  }

  /**
   * Same as {@code drawCircledBullsEye(context(), x, y, radius() / 5)}.
   *
   * @see #drawCircledBullsEye(float, float, float)
   * @see #drawSquaredBullsEye(float, float, float)
   */
  public void drawCircledBullsEye(float x, float y) {
    drawCircledBullsEye(context(), x, y, radius() / 5);
  }

  /**
   * Same as {@code drawCircledBullsEye(context(), x, y, diameter)}.
   *
   * @see #drawCircledBullsEye(PGraphics, float, float, float)
   * @see #drawSquaredBullsEye(PGraphics, float, float, float)
   */
  public void drawCircledBullsEye(float x, float y, float diameter) {
    drawCircledBullsEye(context(), x, y, diameter);
  }

  /**
   * Draws a circled bullseye onto {@code pGraphics}, centered at {@code (x, y)},
   * having {@code length} pixels.
   *
   * @see #drawSquaredBullsEye(PGraphics, float, float, float)
   */
  public void drawCircledBullsEye(PGraphics pGraphics, float x, float y, float diameter) {
    pGraphics.pushStyle();
    beginHUD(pGraphics);
    pGraphics.noFill();
    pGraphics.ellipseMode(CENTER);
    pGraphics.circle(x, y, diameter);
    endHUD(pGraphics);
    drawCross(x, y, 0.6f * diameter);
    pGraphics.popStyle();
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
   * Same as {@code drawTorusSolenoid(context(), faces, detail, insideRadius, outsideRadius)}.
   *
   * @see #drawTorusSolenoid(PGraphics, int, int, float, float)
   */
  public void drawTorusSolenoid(int faces, int detail, float insideRadius, float outsideRadius) {
    drawTorusSolenoid(context(), faces, detail, insideRadius, outsideRadius);
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

  /**
   * Draws a cone onto {@code pGraphics} centered at {@code (0,0)} having
   * Semi-axis {@code a} and {@code b} and  {@code height} dimensions.
   * <p>
   * {@code a} represents the horizontal semi-axis
   * {@code b} represents the horizontal semi-axis
   *
   * @param pGraphics
   * @param height
   * @param a
   * @param b
   * @param detail
   */
  public void drawCone(PGraphics pGraphics, float height, float a, float b, int detail) {
    float[] x = new float[detail + 1];
    float[] y = new float[detail + 1];

    for (int i = 0; i <= detail; i++) {
      float theta = PApplet.TWO_PI * i / detail;
      float r = a * b / (float) (Math.sqrt(b * b * Math.cos(theta) * Math.cos(theta) + a * a * Math.sin(theta) * Math.sin(theta)));
      x[i] = r * (float) Math.cos(theta);
      y[i] = r * (float) Math.sin(theta);
    }
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    pGraphics.vertex(0, 0, 0);
    for (int i = 0; i <= detail; i++) {
      pGraphics.vertex(x[i], y[i], height);
    }
    pGraphics.endShape(PApplet.CLOSE);
  }

  /**
   * Draws a cone onto {@code pGraphics} centered at {@code (0,0)} where
   * {@code vertices} represents a polygon on XY Plane and with {@code height} as height.
   *
   * @param pGraphics
   * @param height
   * @param vertices
   */
  public void drawCone(PGraphics pGraphics, float height, List<Vector> vertices) {
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    pGraphics.vertex(0, 0, 0);
    for (Vector v : vertices) {
      pGraphics.vertex(v.x(), v.y(), height);
    }
    if (!vertices.isEmpty()) pGraphics.vertex(vertices.get(0).x(), vertices.get(0).y(), height);
    pGraphics.endShape();
  }

  /**
   * Draws a cone onto {@code pGraphics} centered at {@code (0,0,0)} where
   * {@code vertices} represents the base of the Polygon and with {@code scale} as maximum height.
   * <p>
   * It is desirable that each point in {@code vertices} lie inside the unit Sphere
   *
   * @param pGraphics
   * @param vertices
   * @param scale
   */
  public void drawCone(PGraphics pGraphics, List<Vector> vertices, float scale) {
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    pGraphics.vertex(0, 0, 0);
    for (Vector v : vertices)
      pGraphics.vertex(scale * v.x(), scale * v.y(), scale * v.z());
    if (!vertices.isEmpty())
      pGraphics.vertex(scale * vertices.get(0).x(), scale * vertices.get(0).y(), scale * vertices.get(0).z());
    pGraphics.endShape();
  }

  /**
   * Draws an Arc onto {@code pGraphics} centered at {@code (0,0)} on the XY Plane
   * {@code minAngle} and {@code maxAngle} represents the Arc's width.
   *
   * @param pGraphics
   * @param radius
   * @param minAngle
   * @param maxAngle
   * @param detail
   */
  public void drawArc(PGraphics pGraphics, float radius, float minAngle, float maxAngle, int detail) {
    pGraphics.beginShape(PApplet.TRIANGLE_FAN);
    pGraphics.vertex(0, 0, 0);
    float step = (maxAngle - minAngle) / detail;
    for (float theta = minAngle; theta <= maxAngle; theta += step)
      pGraphics.vertex(radius * (float) Math.cos(theta), radius * (float) Math.sin(theta));
    pGraphics.endShape(PApplet.CLOSE);
  }

  /**
   * Returns the last horizontal mouse displacement.
   */
  public float mouseDX() {
    return pApplet().mouseX - pApplet().pmouseX;
  }

  /**
   * Same as {@code return mouseRADX(PI / width())}.
   *
   * @see #mouseRADX(float)
   */
  public float mouseRADX() {
    return mouseRADX(PI / width());
  }

  /**
   * Converts {@link #mouseDX()} into angular displacement (in radians) according to {@code sensitivity}
   * and returns it.
   */
  public float mouseRADX(float sensitivity) {
    return mouseDX() * sensitivity;
  }

  /**
   * Returns the last vertical mouse displacement.
   */
  public float mouseDY() {
    return pApplet().mouseY - pApplet().pmouseY;
  }

  /**
   * Same as {@code return mouseRADY(PI / height())}.
   *
   * @see #mouseRADY(float)
   */
  public float mouseRADY() {
    return mouseRADY(PI / height());
  }

  /**
   * Converts {@link #mouseDY()} into angular displacement (in radians) according to {@code sensitivity}
   * and returns it.
   */
  public float mouseRADY(float sensitivity) {
    return mouseDY() * sensitivity;
  }

  /**
   * Returns the current mouse x coordinate.
   */
  public int mouseX() {
    return pApplet().mouseX - _upperLeftCornerX;
  }

  /**
   * Returns the current mouse y coordinate.
   */
  public int mouseY() {
    return pApplet().mouseY - _upperLeftCornerY;
  }

  /**
   * Returns the previous mouse x coordinate.
   */
  public int pmouseX() {
    return pApplet().pmouseX - _upperLeftCornerX;
  }

  /**
   * Returns the previous mouse y coordinate.
   */
  public int pmouseY() {
    return pApplet().pmouseY - _upperLeftCornerY;
  }

  /**
   * Same as {@code return track(tag, mouse())}.
   *
   * @see #track(String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node track(String tag) {
    return track(tag, mouseX(), mouseY());
  }

  /**
   * Same as {@code return track(mouse())}.
   *
   * @see #track(int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node track() {
    return super.track(mouseX(), mouseY());
  }

  /**
   * Same as {@code cast(tag, mouse())}.
   *
   * @see Graph#cast(String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void cast(String tag) {
    cast(tag, mouseX(), mouseY());
  }

  /**
   * Same as {@code cast(mouse())}.
   *
   * @see #cast(int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void cast() {
    cast(mouseX(), mouseY());
  }

  /**
   * Same as {@code translate(mouseDX(), mouseDY())}.
   *
   * @see #translate(float, float)
   * @see #mouseDX()
   * @see #mouseDY()
   */
  public void translate() {
    translate(mouseDX(), mouseDY());
  }

  /**
   * Same as {@code translate(trackedNode(tag))}.
   *
   * @see #translate(Node)
   */
  public void translate(String tag) {
    translate(node(tag));
  }

  /**
   * Same as {@code translate(mouseDX(), mouseDY(), node)}.
   *
   * @see Graph#translate(Node, float, float)
   * @see #mouseDX()
   * @see #mouseDY()
   */
  public void translate(Node node) {
    translate(node, mouseDX(), mouseDY());
  }

  /**
   * Same as {@code spin(pmouse(), mouse())}.
   *
   * @see #spin(int, int, int, int)
   * @see #pmouseX()
   * @see #pmouseY()
   * @see #mouseX()
   * @see #mouseY()
   */
  public void spin() {
    spin(pmouseX(), pmouseY(), mouseX(), mouseY());
  }

  /**
   * Same as {@code spin(trackedNode(tag)}.
   *
   * @see #spin(Node)
   */
  public void spin(String tag) {
    spin(node(tag));
  }

  /**
   * Same as {@code spin(pmouse(), mouse(), node)}.
   *
   * @see Graph#spin(Node, int, int, int, int)
   * @see #pmouseX()
   * @see #pmouseY()
   * @see #mouseX()
   * @see #mouseY()
   */
  public void spin(Node node) {
    spin(node, pmouseX(), pmouseY(), mouseX(), mouseY());
  }

  // only eye

  /**
   * Same as {@code lookAround(mouseRADX(), mouseRADY())}.
   *
   * @see #lookAround(float, float)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void lookAround() {
    lookAround(mouseRADX(), mouseRADY());
  }

  /**
   * Same as {@code rotateCAD(mouseRADX(), mouseRADY())}.
   *
   * @see #rotateCAD(float, float)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void rotateCAD() {
    rotateCAD(mouseRADX(), mouseRADY());
  }

  /**
   * Same as {@code rotateCAD(mouseRADX(), mouseRADY(), up)}.
   *
   * @see #rotateCAD(float, float, Vector)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void rotateCAD(Vector up) {
    rotateCAD(mouseRADX(), mouseRADY(), up);
  }

  /*
  public void screenRotate() {
    super.screenRotate(new Point(pApplet().pmouseX, pApplet().pmouseY), new Point(pApplet().mouseX, pApplet().mouseY));
  }

  public void screenRotate(float sensitivity) {
    super.screenRotate(new Point(pApplet().pmouseX, pApplet().pmouseY), new Point(pApplet().mouseX, pApplet().mouseY), sensitivity);
  }

  public void screenRotate(Node node) {
    super.screenRotate(new Point(pApplet().pmouseX, pApplet().pmouseY), new Point(pApplet().mouseX, pApplet().mouseY), node);
  }

  public void screenRotate(float sensitivity, Node node) {
    super.screenRotate(new Point(pApplet().pmouseX, pApplet().pmouseY), new Point(pApplet().mouseX, pApplet().mouseY), sensitivity, node);
  }
  */
}
