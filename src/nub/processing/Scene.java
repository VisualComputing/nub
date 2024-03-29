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

// Thanks goes to Andres Colubri, https://www.sabetilab.org/andres-colubri/
// for implementing the first off-screen scene working example

package nub.processing;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Matrix;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.opengl.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

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
 * static ones), such as {@link #beginHUD()},
 * {@link #endHUD()}, {@link #drawAxes(PGraphics, float)},
 * {@link #drawCross(float, float, float)} and {@link #drawGrid(PGraphics)}
 * among others, can be used to set a {@link Node#setShape(PShape)} (see
 * also {@link Node#setShape(Consumer)}).
 * <p>
 * Another scene's eye (different than the graph {@link Graph#eye()}) can be drawn with
 * {@link #drawFrustum(Graph)}. Typical usage include interactive minimaps and
 * visibility culling visualization and debugging.
 * <h1>Picking and interaction</h1>
 * Refer to the {@link Graph} documentation for details about how picking and interaction works
 * in nub.
 * <p>
 * The scene just provides additional methods to pick and interact with the mouse that wrap the
 * {@link PApplet#pmouseX} and {@link PApplet#pmouseY}, {@link #mouseDX()} , {@link #mouseDY()},
 * {@link #mouseRADX()} and {@link #mouseRADY()} variables and thus simplify
 * the method signatures provide by the {@link Graph} counterparts. See: {@link #tag(String)}
 * and {@link #tag()} for node tagging; {@link #shift(Node, float)} and {@link #shift(String, float)}
 * for translation; {@link #spin(Node, float)} and {@link #spin(String, float)} for
 * spinning; {@link #lookAround(float)} and {@link #cad(float)} for eye look-around
 * and rotate-cad, to name a few.
 *
 * @see Graph
 * @see Node
 */
public class Scene extends Graph {
  @FunctionalInterface
  public interface Callback {
    void execute();
  }

  public static String prettyVersion = "1.0.0";
  public static String version = "12";

  // P R O C E S S I N G A P P L E T A N D O B J E C T S
  public static PApplet pApplet;

  // _bb : picking buffer
  protected PShader _triangleShader, _lineShader, _pointShader;

  // mouse speed
  long _timestamp;

  // Prettify bb display if required
  protected HashMap<Integer, Integer> _idToColor = new HashMap<>();

  // CONSTRUCTORS

  /**
   * Same as {@code this(pApplet.g, eye)}.
   *
   * @see #Scene(PGraphics, Node)
   */
  public Scene(PApplet pApplet, Node eye) {
    this(pApplet.g, eye);
  }

  /**
   * Same as {@code super(pGraphics, pGraphics.width, pGraphics.height, eye)}.
   *
   * @see Graph#Graph(Object, int, int, Node)
   * @see #Scene(PApplet, Node)
   */
  public Scene(PGraphics pGraphics, Node eye) {
    super(pGraphics, pGraphics.width, pGraphics.height, eye);
    _init(pGraphics);
  }

  /**
   * Same as {@code this(pApplet.g)}.
   *
   * @see #Scene(PGraphics)
   */
  public Scene(PApplet pApplet) {
    this(pApplet.g);
  }

  /**
   * Same as {@code this(pGraphics, new Vector(), 100)}.
   *
   * @see #Scene(PApplet, Vector, float)
   */
  public Scene(PGraphics pGraphics) {
    //super(pGraphics, pGraphics.width, pGraphics.height, pGraphics instanceof PGraphics2D ? Type.TWO_D : Type.PERSPECTIVE);
    this(pGraphics, new Vector(), 100);
  }

  /**
   * Same as {this(pApplet.g, new Vector(), radius)}.
   *
   * @see Scene#Scene(PGraphics, Vector, float)
   */
  public Scene(PApplet pApplet, float radius) {
    this(pApplet.g, new Vector(), radius);
  }

  /**
   * Same as {@code this(pApplet.g, center, radius)}.
   *
   * @see #Scene(PGraphics, Vector, float)
   */
  public Scene(PApplet pApplet, Vector center, float radius) {
    this(pApplet.g, center, radius);
  }

  /**
   * Same as {@code this(pGraphics, new Vector(), radius)}.
   *
   * @see #Scene(PGraphics, Vector, float)
   */
  public Scene(PGraphics pGraphics, float radius) {
    this(pGraphics, new Vector(), radius);
  }

  /**
   * Same as {@code super(pGraphics, pGraphics.width, pGraphics.height, pGraphics instanceof PGraphics2D ? Type.TWO_D : Type.PERSPECTIVE, center, radius)},
   * and then sets {@link #leftHanded} to {@code true}.
   *
   * @see Graph#Graph(Object, int, int, Type, Vector, float)
   */
  public Scene(PGraphics pGraphics, Vector center, float radius) {
    super(pGraphics, pGraphics.width, pGraphics.height, pGraphics instanceof PGraphics2D ? Type.TWO_D : Type.PERSPECTIVE, center, radius);
    _init(pGraphics);
  }

  /**
   * Same as {@code this(pApplet, eye, new Vector(), radius)}.
   *
   * @see Scene#Scene(PApplet, Node, Vector, float)
   */
  public Scene(PApplet pApplet, Node eye, float radius) {
    this(pApplet, eye, new Vector(), radius);
  }

  /**
   * Same as {@code this(pApplet.g, eye, center, rdius)}.
   *
   * @see #Scene(PGraphics, Node, Vector, float)
   */
  public Scene(PApplet pApplet, Node eye, Vector center, float radius) {
    this(pApplet.g, eye, center, radius);
  }

  /**
   * Same as {@code this(pGraphics, eye, new Vector(), radius)}.
   *
   * @see #Scene(PGraphics, Node, Vector, float)
   */
  public Scene(PGraphics pGraphics, Node eye, float radius) {
    this(pGraphics, eye, new Vector(), radius);
  }

  /**
   * Same as {@code super(pGraphics, pGraphics.width, pGraphics.height, eye, type, center, radius)},
   * and then sets {@link #leftHanded} to {@code true}.
   *
   * @see Graph#Graph(Object, int, int, Node, Type, Vector, float)
   */
  public Scene(PGraphics pGraphics, Node eye, Vector center, float radius) {
    super(pGraphics, pGraphics.width, pGraphics.height, eye, pGraphics instanceof PGraphics2D ? Type.TWO_D : Type.PERSPECTIVE, center, radius);
    _init(pGraphics);
  }

  protected void _init(PGraphics pGraphics) {
    // 1. P5 objects
    if (pApplet == null) pApplet = pGraphics.parent;
    _offscreen = pGraphics != pApplet.g;
    if (pGraphics instanceof PGraphicsOpenGL)
      _matrixHandler = new GLMatrixHandler((PGraphicsOpenGL) pGraphics);
    else
      throw new RuntimeException("context() is not instance of PGraphicsOpenGL");
    _bb = pApplet.createGraphics(pGraphics.width, pGraphics.height, pGraphics instanceof PGraphics3D ? PApplet.P3D : PApplet.P2D);
    _bbMatrixHandler = new GLMatrixHandler((PGraphicsOpenGL) _bb);
    if (!_offscreen && _onscreenGraph == null)
      _onscreenGraph = this;
    // 2. Back buffer
    // is noSmooth absorbed by the line shader
    // looks safer to call it though
    _backBuffer().noSmooth();
    _triangleShader = pApplet.loadShader("Picking.frag");
    _lineShader = pApplet.loadShader("Picking.frag", "LinePicking.vert");
    _pointShader = pApplet.loadShader("Picking.frag", "PointPicking.vert");
    // 3. Register P5 methods
    pApplet.registerMethod("pre", this);
    pApplet.registerMethod("draw", this);
    pApplet.registerMethod("dispose", this);
    // 4. Handed
    leftHanded = true;
  }

  // P5 STUFF

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

  // OPENGL

  /**
   * Same as {@code pixelToLine(mouseX(), mouseY(), origin, direction)}.
   *
   * @see #mouseX()
   * @see #mouseY()
   * @see #pixelToLine(int, int, Vector, Vector)
   */
  public void pixelToLine(Vector origin, Vector direction) {
    pixelToLine(mouseX(), mouseY(), origin, direction);
  }

  /**
   * Same as {@code return setCenter(mouseX(), mouseY())}.
   *
   * @see #mouseX()
   * @see #mouseY()
   * @see #setCenter(int, int)
   */
  public boolean setCenter() {
    return setCenter(mouseX(), mouseY());
  }

  /**
   * The {@link #center()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #center()} is set).
   */
  public boolean setCenter(int pixelX, int pixelY) {
    Vector pup = location(pixelX, pixelY);
    if (pup != null) {
      _center = pup;
      return true;
    }
    return false;
  }

  /**
   * Same as {@code return pixelDepth(mouseX(), mouseY())}.
   *
   * @see #mouseX()
   * @see #mouseY()
   * @see #pixelDepth(int, int)
   */
  public float pixelDepth() {
    return pixelDepth(mouseX(), mouseY());
  }

  /**
   * Returns the depth (z-value) of the object under the {@code pixel}. Used by
   * {@link #location(int, int)}.
   * <p>
   * The z-value ranges in [0..1] (near and far plane respectively). In 3D note that this
   * value is not a linear interpolation between {@link #zNear()} and {@link #zFar()}:
   * {@code z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z')} where {@code z'} is
   * the distance from the point you project to the eye, along the {@link #viewDirection()}.
   *
   * @see #location(int, int)
   */
  public float pixelDepth(int pixelX, int pixelY) {
    PGraphicsOpenGL pggl;
    if (context() instanceof PGraphicsOpenGL)
      pggl = (PGraphicsOpenGL) context();
    else
      throw new RuntimeException("context() is not instance of PGraphicsOpenGL");
    float[] depth = new float[1];
    PGL pgl = pggl.beginPGL();
    pgl.readPixels(pixelX, (height() - pixelY), 1, 1, PGL.DEPTH_COMPONENT, PGL.FLOAT, FloatBuffer.wrap(depth));
    pggl.endPGL();
    return depth[0];
  }

  /**
   * Same as {@code return location(mouseX(), mouseY())}.
   *
   * @see #mouseX()
   * @see #mouseY()
   * @see #location(int, int)
   */
  public Vector location() {
    return location(mouseX(), mouseY());
  }

  /**
   * Returns the world coordinates of the 3D point located at {@code (pixelX, pixelY)} on
   * screen. May be null if no object is under pixel.
   */
  public Vector location(int pixelX, int pixelY) {
    float depth = pixelDepth(pixelX, pixelY);
    Vector point = location(new Vector(pixelX, pixelY, depth));
    return (depth < 1.0f) ? point : null;
  }

  /**
   * Disables z-buffer on {@link #context()}.
   */
  public void disableDepthTest() {
    context().hint(PApplet.DISABLE_DEPTH_TEST);
  }

  /**
   * Enables z-buffer on {@link #context()}.
   */
  public void enableDepthTest() {
    context().hint(PApplet.ENABLE_DEPTH_TEST);
  }

  /**
   * Disables z-buffer on {@code pGraphics}.
   */
  public static void disableDepthTest(PGraphics pGraphics) {
    pGraphics.hint(PApplet.DISABLE_DEPTH_TEST);
  }

  /**
   * Enables z-buffer on {@code pGraphics}.
   */
  public static void enableDepthTest(PGraphics pGraphics) {
    pGraphics.hint(PApplet.ENABLE_DEPTH_TEST);
  }

  // 3. Drawing methods

  /**
   * The {@link #center()} is set to the point located under {@code pixel} on screen.
   * <p>
   * Returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #center()} is set).
   */
  public boolean setCenterFromPixel(int pixelX, int pixelY) {
    Vector pup = location(pixelX, pixelY);
    if (pup != null) {
      _center = pup;
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
   * Handles resizes events to update the scene {@link #width()} and {@link #height()}.
   */
  @Override
  protected void _resize() {
    if (isOffscreen())
      return;
    if ((width() != context().width))
      setWidth(context().width);
    if ((height() != context().height))
      setHeight(context().height);
  }

  /**
   * Same as {@code display(null, false, false, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display() {
    display(null, false, null, null, null, 0, 0);
  }

  /**
   * Same as {@code display(background, false, false, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background) {
    display(background, false, null, null, null, 0, 0);
  }

  /**
   * Same as {@code display(null, false, false, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(int x, int y) {
    display(null, false, null, null, null, x, y);
  }

  /**
   * Same as {@code display(background, false, false, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, int x, int y) {
    display(background, false, null, null, null, x, y);
  }

  /**
   * Same as {@code display(null, false, false, null, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Callback worldCallback) {
    display(null, false, null, null, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, false, false, null, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Callback worldCallback) {
    display(background, false, null, null, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(null, false, false, null, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Callback worldCallback, int x, int y) {
    display(null, false, null, null, worldCallback, x, y);
  }

  /**
   * Same as {@code display(background, false, false, null, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Callback worldCallback, int x, int y) {
    display(background, false, null, null, worldCallback, x, y);
  }

  /**
   * Same as {@code display(null, axes, grid, null, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, Callback worldCallback) {
    display(null, axes, grid, null, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, grid, null, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, Callback worldCallback) {
    display(background, axes, grid, null, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, grid, null, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, Callback worldCallback, int x, int y) {
    display(null, axes, grid, null, worldCallback, x, y);
  }

  /**
   * Same as {@code display(background, axes, grid, null, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, Callback worldCallback, int x, int y) {
    display(background, axes, grid, null, worldCallback, x, y);
  }

  /**
   * Same as {@code display(null, false, null, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Node subtree) {
    display(null, false, null, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(background, false, null, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Node subtree) {
    display(background, false, null, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(null, false, null, null, subtree, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Node subtree, int x, int y) {
    display(null, false, null, subtree, null, x, y);
  }

  /**
   * Same as {@code display(background, false, null, null, subtree, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Node subtree, int x, int y) {
    display(background, false, null, subtree, null, x, y);
  }

  /**
   * Same as {@code display(null, axes, null, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Node subtree) {
    display(null, axes, null, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, null, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Node subtree) {
    display(background, axes, null, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(axes, grid, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, Node subtree) {
    display(null, axes, grid, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, grid, subtree, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, Node subtree) {
    display(background, axes, grid, subtree, null, 0, 0);
  }

  /**
   * Same as {@code display(null, axes, null, subtree, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Node subtree, int x, int y) {
    display(null, axes, null, subtree, null, x, y);
  }

  /**
   * Same as {@code display(background, axes, null, subtree, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Node subtree, int x, int y) {
    display(background, axes, null, subtree, null, x, y);
  }

  /**
   * Same as {@code display(null, axes, grid, subtree, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, Node subtree, int x, int y) {
    display(null, axes, grid, subtree, null, x, y);
  }

  /**
   * Same as {@code display(background, axes, grid, subtree, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, Node subtree, int x, int y) {
    display(background, axes, grid, subtree, null, x, y);
  }

  /**
   * Same as {@code display(null, false, null, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Node subtree, Callback worldCallback) {
    display(null, false, null, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, false, null, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Node subtree, Callback worldCallback) {
    display(background, false, null, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(null, false, null, subtree, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Node subtree, Callback worldCallback, int x, int y) {
    display(null, false, null, subtree, worldCallback, x, y);
  }

  /**
   * Same as {@code display(background, false, null, subtree, worldCallback, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, Node subtree, Callback worldCallback, int x, int y) {
    display(background,false,null, subtree, worldCallback, x, y);
  }

  /**
   * Same as {@code display(null, axes, null, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes) {
    display(null, axes, null, null, null, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, null, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes) {
    display(background, axes, null, null, null, 0, 0);
  }

  /**
   * Same as {@code display(null, axes, grid, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid) {
    display(null, axes, grid, null, null, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, grid, null, null, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid) {
    display(background, axes, grid, null, null, 0, 0);
  }

  /**
   * Same as {@code display(null, axes, null, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, int x, int y) {
    display(null, axes, null, null, null, x, y);
  }

  /**
   * Same as {@code display(background, axes, null, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, int x, int y) {
    display(background, axes, null, null, null, x, y);
  }

  /**
   * Same as {@code display(null, axes, grid, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, int x, int y) {
    display(null, axes, grid, null, null, x, y);
  }

  /**
   * Same as {@code display(background, axes, grid, null, null, x, y)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, int x, int y) {
    display(background, axes, grid, null, null, x, y);
  }

  /**
   * Same as {@code display(null, axes, null, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Node subtree, Callback worldCallback) {
    display(null, axes, null, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, null, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Node subtree, Callback worldCallback) {
    display(background, axes, null, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(null, axes, grid, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Integer grid, Node subtree, Callback worldCallback) {
    display(null, axes, grid, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(background, axes, grid, subtree, worldCallback, 0, 0)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Integer grid, Node subtree, Callback worldCallback) {
    display(background, axes, grid, subtree, worldCallback, 0, 0);
  }

  /**
   * Same as {@code display(null, axes, subtree, worldCallback, cornerX, cornerY)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(boolean axes, Node subtree, Callback worldCallback, int cornerX, int cornerY) {
    display(null, axes, subtree, worldCallback, cornerX, cornerY);
  }

  /**
   * Same as {@code display(background, axes, null, subtree, worldCallback, cornerX, cornerY)}.
   *
   * @see #display(Object, boolean, Integer, Node, Callback, int, int)
   */
  public void display(Object background, boolean axes, Node subtree, Callback worldCallback, int cornerX, int cornerY) {
    display(background, axes, null, subtree, worldCallback, cornerX, cornerY);
  }

  /**
   * Display the scene tree.
   *
   * @param background color or image; may be null (don't refresh)
   * @param axes
   * @param grid color; may be null (no grid)
   * @param subtree
   * @param worldCallback
   * @param cornerX
   * @param cornerY
   */
  public void display(Object background, boolean axes, Integer grid, Node subtree, Callback worldCallback, int cornerX, int cornerY) {
    if (isOffscreen()) {
      openContext();
    }
    if (background instanceof PImage) {
      context().background((PImage)background);
    }
    else if (isNumInstance(background)) {
      context().background(castToInt(background));
    }
    if (axes) {
      drawAxes();
    }
    if (grid != null) {
      context().pushStyle();
      context().stroke(grid);
      drawGrid();
      context().popStyle();
    }
    render(subtree);
    if (worldCallback != null) {
      worldCallback.execute();
    }
    if (isOffscreen()) {
      closeContext();
      image(cornerX, cornerY);
    }
  }

  /**
   * Same as {@code image(0, 0)}.
   *
   * @see #image(int, int)
   */
  public void image() {
    image(0, 0);
  }

  /**
   * Similar to {@link #pApplet} {@code image()}. Used to display the offscreen scene {@link #context()}.
   * Does nothing if the scene is on-screen. Throws an error if there's an onscreen scene and this method
   * is called within {@link #openContext()} and {@link #closeContext()}.
   * <p>
   * Call this method, instead of {@link #pApplet} {@code image()}, to make {@link #hasFocus()}
   * work always properly.
   */
  public void image(int pixelX, int pixelY) {
    if (isOffscreen()) {
      if (_lastRendered != Graph._frameCount) {
        System.out.println("Warning: image() not updated since render wasn't called @" + Graph._frameCount);
      }
      if (_renderCount != 0) {
        throw new RuntimeException("Error: offscreen scenes should call image() after openContext() / closeContext()");
      }
      else {
        if (_onscreenGraph != null) {
          _onscreenGraph.beginHUD();
        }
        pApplet.pushStyle();
        _setUpperLeftCorner(pixelX, pixelY);
        _lastDisplayed = Graph._frameCount;
        pApplet.imageMode(PApplet.CORNER);
        pApplet.image(context(), pixelX, pixelY);
        pApplet.popStyle();
        if (_onscreenGraph != null) {
          _onscreenGraph.endHUD();
        }
      }
    }
    /*
    // TODO debug
    else {
      System.out.println("Warning: image() is only available for offscreen scenes. Nothing done!");
    }
    // */
  }

  /**
   * Same as {@code displayBackBuffer(0, 0)}.
   *
   * @see #displayBackBuffer(int, int, int)
   */
  public void displayBackBuffer() {
    displayBackBuffer(/* 0xFFFFFFFF */-16777216);
  }

  /**
   * Same as {@code displayBackBuffer(background, 0, 0)}.
   *
   * @see #displayBackBuffer(int, int, int)
   */
  public void displayBackBuffer(int background) {
    displayBackBuffer(background, 0, 0);
  }

  /**
   * Displays the buffer nub use for picking at the given pixel coordinates.
   * Used for debugging.
   *
   * @see #displayBackBuffer(int, int, int)
   */
  public void displayBackBuffer(int pixelX, int pixelY) {
    displayBackBuffer(/* 0xFFFFFFFF */-16777216, pixelX, pixelY);
  }

  /**
   * Displays the buffer nub use for picking at the given pixel coordinates,
   * with {@code background} color. Used for debugging.
   */
  public void displayBackBuffer(int background, int pixelX, int pixelY) {
    if (_onscreenGraph != null) {
      _onscreenGraph.beginHUD();
      _imageBackBuffer(background, pixelX, pixelY);
      _onscreenGraph.endHUD();
    }
    else {
      _imageBackBuffer(background, pixelX, pixelY);
    }
  }

  /**
   * Display the {@link #_backBuffer()} used for picking at screen coordinates
   * on top of the main sketch canvas at the upper left corner:
   * {@code (pixelX, pixelY)}. Mainly for debugging.
   */
  protected int _idToColor(int id) {
    if(_idToColor.containsKey(id)) return _idToColor.get(id);
    boolean isDifferent = false;
    int color = 0;
    while(!isDifferent){
      color = pApplet.color(pApplet.random(255),pApplet.random(255), pApplet.random(255));
      isDifferent = true;
      for(int c : _idToColor.values()) isDifferent &= c != color;
    }
    _idToColor.put(id, color);
    return color;
  }

  /**
   * Display the {@link #_backBuffer()} used for picking at screen coordinates
   * on top of the main sketch canvas at the upper left corner:
   * {@code (pixelX, pixelY)}. Mainly for debugging.
   */
  protected void _imageBackBuffer(int background, int pixelX, int pixelY) {
    if (_backBuffer() != null) {
      pApplet.pushStyle();
      pApplet.imageMode(PApplet.CORNER);
      PImage img = _backBuffer().get();
      img.loadPixels();
      // simple lookup table to discriminate bb shapes
      for (int i = 0; i < img.pixels.length; i++) {
        if (img.pixels[i] == -16777216) {
          img.pixels[i] = background;
          continue;
        }
        img.pixels[i] = _idToColor(img.pixels[i]);
      }
      img.updatePixels();
      pApplet.image(img, pixelX, pixelY);
      pApplet.popStyle();
    }
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
   * Saves the {@link #eye()}, the {@link #radius()} and the {@link #_type} into {@code fileName}.
   *
   * @see #saveConfig()
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig(String fileName) {
    JSONObject json = new JSONObject();
    json.setFloat("radius", _radius);
    json.setString("type", _type.name());
    json.setJSONObject("eye", _toJSONObject(eye()));
    // TODO: handle nodes (hint, ... restore keyframes)
    pApplet.saveJSONObject(json, fileName);
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
   * Loads the {@link #eye()}, the {@link #radius()} and the {@link #_type} from {@code fileName}.
   *
   * @see #saveConfig()
   * @see #saveConfig(String)
   * @see #loadConfig()
   */
  public void loadConfig(String fileName) {
    JSONObject json = null;
    try {
      json = pApplet.loadJSONObject(fileName);
    } catch (Exception e) {
      System.out.println("No such " + fileName + " found!");
    }
    if (json != null) {
      _radius = json.getFloat("radius");
      String type = json.getString("type");
      setType(type.equals("PERSPECTIVE") ? Type.PERSPECTIVE : type.equals("ORTHOGRAPHIC") ? Type.ORTHOGRAPHIC : type.equals("TWO_D") ? Type.TWO_D : Type.CUSTOM);
      eye().set(_toNode(json.getJSONObject("eye")));
      // TODO: handle nodes (hint, ... restore keyframes)
    }
  }

  /**
   * Used internally by {@link #loadConfig(String)}. Converts the P5 JSONObject into a node.
   */
  protected Node _toNode(JSONObject jsonNode) {
    float x, y, z;
    x = jsonNode.getJSONArray("position").getFloat(0);
    y = jsonNode.getJSONArray("position").getFloat(1);
    z = jsonNode.getJSONArray("position").getFloat(2);
    float qx, qy, qz, qw;
    qx = jsonNode.getJSONArray("orientation").getFloat(0);
    qy = jsonNode.getJSONArray("orientation").getFloat(1);
    qz = jsonNode.getJSONArray("orientation").getFloat(2);
    qw = jsonNode.getJSONArray("orientation").getFloat(3);
    Node node = new Node(new Vector(x, y, z), new Quaternion(qx, qy, qz, qw), jsonNode.getFloat("magnitude"), false);
    return node;
  }

  /**
   * Used internally by {@link #saveConfig(String)}. Converts {@code node} into a P5
   * JSONObject.
   */
  protected JSONObject _toJSONObject(Node node) {
    JSONObject jsonNode = new JSONObject();
    jsonNode.setFloat("magnitude", node.worldMagnitude());
    jsonNode.setJSONArray("position", _toJSONArray(node.worldPosition()));
    jsonNode.setJSONArray("orientation", _toJSONArray(node.worldOrientation()));
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

  @Override
  protected boolean _tracks(Node node, int pixelX, int pixelY) {
    if (node == null || isEye(node))
      return false;
    if (!node.tagging)
      return false;
    if(!(0 <= pixelX && pixelX < width() && 0 <= pixelY && pixelY < height())) //Check if pixel is out of bounds
      return false;
    int index = pixelY * width() + pixelX;
    if (_backBuffer().pixels != null)
        return _backBuffer().pixels[index] == node.colorID();
    return false;
  }

  @Override
  protected PGraphics _backBuffer() {
    return (PGraphics) _bb;
  }

  @Override
  protected void _initBackBuffer() {
    _backBuffer().beginDraw();
    // TODO seems style is not require since it should be absorbed by the shader
    //_backBuffer().pushStyle();
    _backBuffer().background(0);
  }

  @Override
  protected void _endBackBuffer() {
    if (!_huds.isEmpty()) {
      _bbMatrixHandler.beginHUD(width(), height());
      for (Node node : _huds) {
        if (_rendered(node)) {
          if (node.isPickingEnabled(Node.HUD)) {
            _emitBackBufferUniforms(node);
            _backBuffer().pushMatrix();
            Vector location = screenLocation(node);
            if (location != null) {
              _backBuffer().translate(location.x(), location.y());
              if (_imrHUD(node) != null) {
                _imrHUD(node).accept(_backBuffer());
              }
              if (_rmrHUD(node) != null) {
                _backBuffer().shape(_rmrHUD(node));
              }
            }
            _backBuffer().popMatrix();
          }
        }
      }
      _bbMatrixHandler.endHUD();
    }
    // TODO seems style is not require since it should be absorbed by the shader
    //_backBuffer().popStyle();
    _backBuffer().loadPixels();
    _backBuffer().endDraw();
  }

  @Override
  protected void _initFrontBuffer() {
    if (isOffscreen()) {
      context().beginDraw();
    }
  }

  @Override
  protected void _endFrontBuffer() {
    if (isOffscreen()) {
      context().endDraw();
    }
  }

  @Override
  protected void _emitBackBufferUniforms(Node node) {
    // TODO How to deal with these commands: breaks picking in Luxo when they're moved to the constructor
    // Seems related to: PassiveTransformations
    // funny, only safe way. Otherwise break things horribly when setting node shapes
    // and there are more than one node holding a shape
    int id = node.id();
    float r = Node.redID(id);
    float g = Node.greenID(id);
    float b = Node.blueID(id);
    _backBuffer().shader(_triangleShader);
    _backBuffer().shader(_lineShader, PApplet.LINES);
    _backBuffer().shader(_pointShader, PApplet.POINTS);
    _triangleShader.set("id", new PVector(r, g, b));
    _lineShader.set("id", new PVector(r, g, b));
    _pointShader.set("id", new PVector(r, g, b));
  }

  @Override
  protected void _displayHUD() {
    if (!_cacheHUDs.isEmpty()) {
      context().pushStyle();
      beginHUD();
      Iterator<Node> iterator = _cacheHUDs.iterator();
      while(iterator.hasNext()) {
        Node node = iterator.next();
        if (_rendered(node) && node.isHintEnabled(Node.HUD)) {
          context().pushMatrix();
          Vector location = screenLocation(node);
          if (location != null) {
            context().translate(location.x(), location.y());
            if (_imrHUD(node) != null) {
              _imrHUD(node).accept(context());
            }
            if (_rmrHUD(node) != null) {
              context().shape(_rmrHUD(node));
            }
          }
          context().popMatrix();
          iterator.remove();
        }
      }
      endHUD();
      context().popStyle();
    }
  }

  @Override
  protected void _displayPaths() {
    context().pushStyle();
    for (Node interpolator : _interpolators) {
      if (_rendered(interpolator) && interpolator.isHintEnabled(Node.KEYFRAMES)) {
        context().pushStyle();
        _drawSpline(interpolator);
        context().popStyle();
      }
    }
    context().popStyle();
  }

  @Override
  protected void _displayFrontHint(Node node) {
    PGraphics pg = context();
    if (node.isHintEnabled(Node.SHAPE)) {
      pg.pushStyle();
      if (_rmrShape(node) != null) {
        pg.shapeMode(pg.shapeMode);
        pg.shape(_rmrShape(node));
      }
      if (_imrShape(node) != null) {
        _imrShape(node).accept(pg);
      }
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.TORUS)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.fill(_torusColor(node));
      drawTorusSolenoid(pg, _torusFaces(node), 5);
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.BOUNDS)) {
      for (Graph graph : _frustumGraphs(node)) {
        if (graph != this) {
          pg.pushStyle();
          pg.colorMode(PApplet.RGB, 255);
          pg.strokeWeight(_boundsWeight(node));
          pg.stroke(((PGraphics)graph.context()).backgroundColor);
          pg.fill(((PGraphics)graph.context()).backgroundColor);
          drawFrustum(pg, graph);
          pg.popStyle();
        }
      }
    }
    if (node.isHintEnabled(Node.AXES)) {
      pg.pushStyle();
      drawAxes(pg, _axesLength(node) == 0 ? _radius / 5 : _axesLength(node));
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.CAMERA)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(_cameraStroke(node));
      pg.fill(_cameraStroke(node));
      _drawEye(pg, _cameraLength(node) == 0 ? _radius : _cameraLength(node));
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.BULLSEYE)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(_bullsEyeStroke(node));
      _drawBullsEye(node);
      pg.popStyle();
    }
  }

  protected void _displayAnimationHint(Node node) {
    PGraphics pg = context();
    if (Graph._isHintEnabled(_keyframesMask(node), Node.SHAPE)) {
      pg.pushStyle();
      if (_rmrShape(node) != null) {
        pg.shapeMode(pg.shapeMode);
        pg.shape(_rmrShape(node));
      }
      if (_imrShape(node) != null) {
        _imrShape(node).accept(pg);
      }
      pg.popStyle();
    }
    if (Graph._isHintEnabled(_keyframesMask(node), Node.TORUS)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.fill(_torusColor(node));
      drawTorusSolenoid(pg, _torusFaces(node), 5);
      pg.popStyle();
    }
    if (Graph._isHintEnabled(_keyframesMask(node), Node.AXES)) {
      pg.pushStyle();
      drawAxes(pg, _axesLength(node) == 0 ? _radius / 5 : _axesLength(node));
      pg.popStyle();
    }
    if (Graph._isHintEnabled(_keyframesMask(node), Node.CAMERA)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(_cameraStroke(node));
      pg.fill(_cameraStroke(node));
      _drawEye(pg, _cameraLength(node) == 0 ? _radius : _cameraLength(node));
      pg.popStyle();
    }
    if (Graph._isHintEnabled(_keyframesMask(node), Node.BULLSEYE)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(_bullsEyeStroke(node));
      _drawBullsEye(node);
      pg.popStyle();
    }
  }

  @Override
  protected void _displayBackHint(Node node) {
    _emitBackBufferUniforms(node);
    PGraphics pg = _backBuffer();
    if (node.isHintEnabled(Node.SHAPE) && node.isPickingEnabled(Node.SHAPE)) {
      if (_rmrShape(node) != null) {
        pg.shapeMode(pg.shapeMode);
        pg.shape(_rmrShape(node));
      }
      if (_imrShape(node) != null) {
        _imrShape(node).accept(pg);
      }
    }
    if (node.isHintEnabled(Node.TORUS) && node.isPickingEnabled(Node.TORUS)) {
      drawTorusSolenoid(pg, _torusFaces(node), 5);
    }
    if (node.isHintEnabled(Node.BOUNDS) && node.isPickingEnabled(Node.BOUNDS)) {
      for (Graph graph : _frustumGraphs(node)) {
        if (graph != this) {
          drawFrustum(pg, graph);
        }
      }
    }
    if (node.isHintEnabled(Node.AXES) && node.isPickingEnabled(Node.AXES)) {
      pg.pushStyle();
      pg.strokeWeight(6);
      drawAxes(pg, _axesLength(node) == 0 ? _radius / 5 : _axesLength(node));
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.CAMERA) && node.isPickingEnabled(Node.CAMERA)) {
      _drawEye(pg, _cameraLength(node) == 0 ? _radius : _cameraLength(node));
    }
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

  protected void _drawSpline(Node interpolator) {
    if (interpolator.hint() != 0) {
      List<Node> path = _path(interpolator);
      if (_splineWeight(interpolator) > 0 && path.size() > 1) {
        context().pushStyle();
        context().noFill();
        context().colorMode(PApplet.RGB, 255);
        context().strokeWeight(_splineWeight(interpolator));
        context().stroke(_splineStroke(interpolator));
        context().beginShape();
        for (Node node : path) {
          Vector position = node.worldPosition();
          vertex(position.x(), position.y(), position.z());
        }
        context().endShape();
        context().popStyle();
      }
      if (_steps(interpolator) > 0) {
        context().pushStyle();
        int count = 0;
        float goal = 0.0f;
        for (Node node : path) {
          if (count >= goal) {
            goal += Node.maxSteps / ((float) _steps(interpolator) + 1);
            if (count % Node.maxSteps != 0) {
              _matrixHandler.pushMatrix();
              _matrixHandler.applyTransformation(node);
              _displayAnimationHint(node);
              _matrixHandler.popMatrix();
            }
          }
          count++;
        }
        context().popStyle();
      }
    }
  }

  /**
   * Internal use.
   */
  protected void _drawEye(PGraphics pg, float scale) {
    pg.pushStyle();
    float halfHeight = scale * (is2D() ? 1.2f : 0.07f);
    float halfWidth = halfHeight * 1.3f;
    float dist = halfHeight / (float) Math.tan(PApplet.PI / 8.0f);

    float arrowHeight = 1.5f * halfHeight;
    float baseHeight = 1.2f * halfHeight;
    float arrowHalfWidth = 0.5f * halfWidth;
    float baseHalfWidth = 0.3f * halfWidth;

    // Frustum outline
    /*
    if (pg == context()) {
      pg.pushStyle();
      pg.noFill();
    }
    */
    pg.pushStyle();
    pg.noFill();

    pg.beginShape(PApplet.TRIANGLE_FAN);
    vertex(pg, 0.0f, 0.0f, 0.0f);
    vertex(pg, -halfWidth, -halfHeight, -dist);
    vertex(pg, halfWidth, -halfHeight, -dist);
    vertex(pg, halfWidth, halfHeight, -dist);
    vertex(pg, -halfWidth, halfHeight, -dist);
    vertex(pg, -halfWidth, -halfHeight, -dist);
    pg.endShape(PApplet.CLOSE);
    /*
    if (pg == context()) {
      pg.popStyle();
    }
    */
    pg.popStyle();

    // Up arrow
    pg.noStroke();
    // Base
    pg.beginShape(PApplet.QUADS);

    if (leftHanded) {
      vertex(pg, baseHalfWidth, -halfHeight, -dist);
      vertex(pg, -baseHalfWidth, -halfHeight, -dist);
      vertex(pg, -baseHalfWidth, -baseHeight, -dist);
      vertex(pg, baseHalfWidth, -baseHeight, -dist);
    } else {
      vertex(pg, -baseHalfWidth, halfHeight, -dist);
      vertex(pg, baseHalfWidth, halfHeight, -dist);
      vertex(pg, baseHalfWidth, baseHeight, -dist);
      vertex(pg, -baseHalfWidth, baseHeight, -dist);
    }

    pg.endShape();
    // Arrow
    pg.beginShape(PApplet.TRIANGLES);
    if (leftHanded) {
      vertex(pg, 0.0f, -arrowHeight, -dist);
      vertex(pg, arrowHalfWidth, -baseHeight, -dist);
      vertex(pg, -arrowHalfWidth, -baseHeight, -dist);
    } else {
      vertex(pg, 0.0f, arrowHeight, -dist);
      vertex(pg, -arrowHalfWidth, baseHeight, -dist);
      vertex(pg, arrowHalfWidth, baseHeight, -dist);
    }
    pg.endShape();
    pg.popStyle();
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
    drawCylinder(pGraphics, _radius / 6, _radius / 3);
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
    float radius = _radius / 4;
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
    drawAxes(_radius);
  }

  /**
   * Same as {@code drawAxes(context(), length, isLeftHanded())}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public void drawAxes(float length) {
    drawAxes(context(), length);
  }

  /**
   * Same as {@code drawAxes(pGraphics, radius(), isLeftHanded())}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public void drawAxes(PGraphics pGraphics) {
    drawAxes(pGraphics, _radius);
  }

  /**
   * Draws axes of {@code length} onto {@code pGraphics}.
   */
  public static void drawAxes(PGraphics pGraphics, float length) {
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
    drawGrid(_radius, 10);
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
    drawGrid(_radius, subdivisions);
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
    drawGrid(pGraphics, _radius, 10);
  }

  /**
   * Draws a grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public static void drawGrid(PGraphics pGraphics, float size, int subdivisions) {
    pGraphics.pushStyle();
    pGraphics.beginShape(PApplet.LINES);
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
    drawDottedGrid(_radius, 10);
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
    drawDottedGrid(_radius, subdivisions);
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
    drawDottedGrid(pGraphics, _radius, 10);
  }

  /**
   * Draws a dotted-grid of {@code size} onto {@code pGraphics} in the XY plane, centered on (0,0,0),
   * having {@code subdivisions}.
   */
  public static void drawDottedGrid(PGraphics pGraphics, float size, int subdivisions) {
    pGraphics.pushStyle();
    float posi, posj;
    pGraphics.beginShape(PApplet.POINTS);
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
    pGraphics.colorMode(PApplet.HSB, 255);
    float hue = pGraphics.hue(pGraphics.strokeColor);
    float saturation = pGraphics.saturation(pGraphics.strokeColor);
    float brightness = pGraphics.brightness(pGraphics.strokeColor);
    pGraphics.stroke(hue, saturation, brightness * 10f / 17f);
    pGraphics.strokeWeight(currentWeight / 2);
    pGraphics.beginShape(PApplet.POINTS);
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
   * Applies the {@code graph.eye()} transformation and then calls
   * {@link #drawFrustum(PGraphics, Graph)} on the scene {@link #context()}.
   *
   * @see #drawFrustum(PGraphics, Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Node, Type, float, float)
   */
  public void drawFrustum(Graph graph) {
    _matrixHandler.pushMatrix();
    _matrixHandler.applyTransformation(graph.eye());
    drawFrustum(context(), graph);
    _matrixHandler.popMatrix();
  }

  /**
   * Draws a representation of the viewing frustum onto {@code pGraphics} according to
   * {@code graph.eye()} and {@code graph._type}.
   * <p>
   * Note that if {@code pGraphics == graph.context()} this method has not effect at all.
   *
   * @see #drawFrustum(Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Node, Type, float, float)
   */
  public static void drawFrustum(PGraphics pGraphics, Graph graph) {
    if (pGraphics == graph.context())
      return;
    // texturing requires graph.isOffscreen() (third condition) otherwise got
    // "The pixels array is null" message and the frustum near plane texture and contour are missed
    boolean texture = pGraphics instanceof PGraphicsOpenGL && graph instanceof Scene && graph.isOffscreen();
    switch (graph.type()) {
      case TWO_D:
      case ORTHOGRAPHIC:
        _drawOrthographicFrustum(pGraphics, texture ? ((Scene) graph).context() : null, graph.eye().worldMagnitude(), graph.width(), leftHanded ? -graph.height() : graph.height(), graph.zNear(), graph.zFar());
        break;
      case PERSPECTIVE:
        _drawPerspectiveFrustum(pGraphics, texture ? ((Scene) graph).context() : null, graph.eye().worldMagnitude(), leftHanded ? -graph.aspectRatio() : graph.aspectRatio(), graph.zNear(), graph.zFar());
        break;
    }
  }

  /**
   * Draws a representation of the {@code eyeBuffer} frustum onto {@code pGraphics} according to frustum parameters:
   * {@code type}, eye {@link Node#worldMagnitude()}, {@code zNear} and {@code zFar}, while taking into account
   * whether or not the scene is {@code leftHanded}.
   *
   * @see #drawFrustum(Graph)
   * @see #drawFrustum(PGraphics, Graph)
   * @see #drawFrustum(PGraphics, PGraphics, Node, Type, float, float)
   */
  public static void drawFrustum(PGraphics pGraphics, PGraphics eyeBuffer, Node eye, Type type, float zNear, float zFar) {
    switch (type) {
      case TWO_D:
      case ORTHOGRAPHIC:
        _drawOrthographicFrustum(pGraphics, eyeBuffer, eye.worldMagnitude(), eyeBuffer.width, leftHanded ? -eyeBuffer.height : eyeBuffer.height, zNear, zFar);
        break;
      case PERSPECTIVE:
        _drawPerspectiveFrustum(pGraphics, eyeBuffer, eye.worldMagnitude(), leftHanded ? -eyeBuffer.width / eyeBuffer.height : eyeBuffer.width / eyeBuffer.height, zNear, zFar);
        break;
    }
  }

  protected static void _drawOrthographicFrustum(PGraphics pGraphics, PGraphics eyeBuffer, float magnitude, float width, float height, float zNear, float zFar) {
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

  protected static void _drawPerspectiveFrustum(PGraphics pGraphics, PGraphics eyeBuffer, float magnitude, float aspectRatio, float zNear, float zFar) {
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
      pGraphics.textureMode(PApplet.NORMAL);
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
   * Same as {@code drawProjectors(eye, Arrays.asList(point))}.
   *
   * @see #drawProjectors(Graph, List)
   */
  public void drawProjector(Graph eye, Vector point) {
    drawProjectors(eye, Arrays.asList(point));
  }

  /**
   * Draws the projection of each point in {@code points} in the near plane onto {@code pGraphics}.
   * <p>
   * This method should be used in conjunction with {@link #drawFrustum(PGraphics, Graph)}.
   * <p>
   * Note that if {@code graph == this} this method has not effect at all.
   *
   * @see #drawProjector(Graph, Vector)
   */
  // TODO needs testing
  public void drawProjectors(Graph graph, List<Vector> points) {
    if (graph == this) {
      System.out.println("Warning: No drawProjectors done!");
      return;
    }
    context().pushStyle();
    if (is2D()) {
      context().beginShape(PApplet.POINTS);
      for (Vector s : points)
        Scene.vertex(context(), s.x(), s.y());
      context().endShape();
    } else {
      // if ORTHOGRAPHIC: do it in the eye coordinate system
      // if PERSPECTIVE: do it in the world coordinate system
      Vector o = new Vector();
      if (graph.type() == Graph.Type.ORTHOGRAPHIC) {
        context().pushMatrix();
        _matrixHandler.applyTransformation(graph.eye());
      }
      // in PERSPECTIVE cache the transformed origin
      else
        o = graph.eye().worldLocation(new Vector());
      context().beginShape(PApplet.LINES);
      for (Vector s : points) {
        if (graph.type() == Graph.Type.ORTHOGRAPHIC) {
          Vector v = graph.eye().location(s);
          Scene.vertex(context(), v.x(), v.y(), v.z());
          // Key here is to represent the eye zNear param (which is given in world units)
          // in eye units.
          // Hence it should be multiplied by: 1 / eye.eye().magnitude()
          // The neg sign is because the zNear is positive but the eye view direction is
          // the negative Z-axis
          Scene.vertex(context(), v.x(), v.y(), -(graph.zNear() * 1 / graph.eye().worldMagnitude()));
        } else {
          Scene.vertex(context(), s.x(), s.y(), s.z());
          Scene.vertex(context(), o.x(), o.y(), o.z());
        }
      }
      context().endShape();
      if (graph.type() == Graph.Type.ORTHOGRAPHIC)
        context().popMatrix();
    }
    context().popStyle();
  }

  /**
   * {@link #drawCross(float, float, float)} centered at the projected node origin.
   * If node is a Node instance the length of the cross is the node
   * {@link Node#bullsEyeSize()}, otherwise it's {@link #radius()} / 5.
   * If node a Node instance and it is {@link #hasTag(Node)} it also applies
   * a stroke highlight.
   *
   * @see #drawCircledBullsEye(float, float, float)
   * @see #drawSquaredBullsEye(float, float, float)
   */
  public void drawCross(Node node) {
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    drawCross(node, node.bullsEyeSize() < 1 ? 200 * node.bullsEyeSize() * node.magnitude() * pixelToSceneRatio(node.worldPosition()) : node.bullsEyeSize());
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
      return;
    }
    Vector center = screenLocation(node);
    if (center != null)
      drawCross(center.x(), center.y(), length);
  }

  /**
   * Convenience function that simply calls {@code drawCross(x, y, radius()/5)}.
   *
   * @see #drawCross(float, float, float)
   */
  public void drawCross(float x, float y) {
    drawCross(x, y, _radius / 5);
  }

  /**
   * Draws a cross on the screen centered under pixel {@code (x, y)}, and edge of size
   * {@code length} onto {@link #context()}.
   */
  public void drawCross(float x, float y, float length) {
    float half_size = length / 2f;
    context().pushStyle();
    beginHUD();
    context().noFill();
    context().beginShape(PApplet.LINES);
    vertex(context(), x - half_size, y);
    vertex(context(), x + half_size, y);
    vertex(context(), x, y - half_size);
    vertex(context(), x, y + half_size);
    context().endShape();
    endHUD();
    context().popStyle();
  }

  /**
   * Draws a bullseye around the node {@link Node#worldPosition()} projection.
   * <p>
   * The shape of the bullseye may be squared or circled depending on the node
   * {@link Node#bullsEyeSize()} sign.
   *
   * @see Node#bullsEyeSize()
   * @see Node#worldPosition()
   * @see #_drawSquaredBullsEye(Node)
   * @see #_drawCircledBullsEye(Node)
   */
  protected void _drawBullsEye(Node node) {
    if (_bullsEyeShape(node) == Node.BullsEyeShape.SQUARE)
      _drawSquaredBullsEye(node);
    else
      _drawCircledBullsEye(node);
  }

  /**
   * Calls {@link #drawSquaredBullsEye(float, float, float)} centered
   * at the projected node origin. The length of the target is the node
   * {@link Node#bullsEyeSize()}. If node {@link #hasTag(Node)} it also
   * applies a stroke highlight.
   *
   * @see #_drawCircledBullsEye(Node)
   * @see #_drawBullsEye(Node)
   */
  protected void _drawSquaredBullsEye(Node node) {
    if (eye() == node) {
      return;
    }
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    Vector center = screenLocation(node);
    if (center != null)
      drawSquaredBullsEye(center.x(), center.y(), node.bullsEyeSize() < 1 ? 200 * node.bullsEyeSize() * node.magnitude() * pixelToSceneRatio(node.worldPosition()) : node.bullsEyeSize());
    context().popStyle();
  }

  /**
   * Same as {@code drawSquaredBullsEye(x, y, radius() / 5)}.
   *
   * @see #drawSquaredBullsEye(float, float, float)
   * @see #drawCircledBullsEye(float, float, float)
   */
  public void drawSquaredBullsEye(float x, float y) {
    drawSquaredBullsEye(x, y, _radius / 5);
  }

  /**
   * Draws a squared bullseye onto {@link #context()}, centered at {@code (x, y)},
   * having {@code length} pixels.
   *
   * @see #drawSquaredBullsEye(float, float, float)
   * @see #drawCircledBullsEye(float, float, float)
   */
  public void drawSquaredBullsEye(float x, float y, float length) {
    float half_length = length / 2f;
    context().pushStyle();
    beginHUD();
    context().noFill();

    context().beginShape();
    vertex(context(), (x - half_length), (y - half_length) + (0.6f * half_length));
    vertex(context(), (x - half_length), (y - half_length));
    vertex(context(), (x - half_length) + (0.6f * half_length), (y - half_length));
    context().endShape();

    context().beginShape();
    vertex(context(), (x + half_length) - (0.6f * half_length), (y - half_length));
    vertex(context(), (x + half_length), (y - half_length));
    vertex(context(), (x + half_length), ((y - half_length) + (0.6f * half_length)));
    context().endShape();

    context().beginShape();
    vertex(context(), (x + half_length), ((y + half_length) - (0.6f * half_length)));
    vertex(context(), (x + half_length), (y + half_length));
    vertex(context(), ((x + half_length) - (0.6f * half_length)), (y + half_length));
    context().endShape();

    context().beginShape();
    vertex(context(), (x - half_length) + (0.6f * half_length), (y + half_length));
    vertex(context(), (x - half_length), (y + half_length));
    vertex(context(), (x - half_length), ((y + half_length) - (0.6f * half_length)));
    context().endShape();
    endHUD();
    drawCross(x, y, 0.6f * length);
    context().popStyle();
  }

  /**
   * Calls {@link #drawCircledBullsEye(float, float, float)} centered
   * at the projected node origin. The length of the target is the node
   * {@link Node#bullsEyeSize()}. If node {@link #hasTag(Node)} it
   * also applies a stroke highlight.
   *
   * @see #_drawSquaredBullsEye(Node)
   * @see #_drawCircledBullsEye(Node)
   * @see #_drawBullsEye(Node)
   */
  protected void _drawCircledBullsEye(Node node) {
    if (eye() == node) {
      return;
    }
    context().pushStyle();
    if (isTagged(node))
      context().strokeWeight(2 + context().strokeWeight);
    Vector center = screenLocation(node);
    if (center != null)
      drawCircledBullsEye(center.x(), center.y(), node.bullsEyeSize() < 1 ? 200 * node.bullsEyeSize() * node.magnitude() * pixelToSceneRatio(node.worldPosition()) : node.bullsEyeSize());
    context().popStyle();
  }

  /**
   * Same as {@code drawCircledBullsEye(x, y, radius() / 5)}.
   *
   * @see #drawCircledBullsEye(float, float, float)
   * @see #drawSquaredBullsEye(float, float, float)
   */
  public void drawCircledBullsEye(float x, float y) {
    drawCircledBullsEye(x, y, _radius / 5);
  }

  /**
   * Draws a circled bullseye onto {@link #context()}, centered at {@code (x, y)},
   * having {@code length} pixels.
   *
   * @see #drawSquaredBullsEye(float, float, float)
   */
  public void drawCircledBullsEye(float x, float y, float diameter) {
    context().pushStyle();
    beginHUD();
    context().noFill();
    context().ellipseMode(PApplet.CENTER);
    context().circle(x, y, diameter);
    endHUD();
    drawCross(x, y, 0.6f * diameter);
    context().popStyle();
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
    drawTorusSolenoid(faces, 0.07f * _radius);
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
   * Same as {@code return hasFocus(pApplet.mouseX, pApplet.mouseY)}.
   *
   * @see #hasFocus(int, int)
   */
  public boolean hasFocus() {
    return hasFocus(pApplet.mouseX, pApplet.mouseY);
  }

  /**
   * Returns the last horizontal mouse displacement.
   */
  public float mouseDX() {
    return pApplet.mouseX - pApplet.pmouseX;
  }

  /**
   * Same as {@code return mouseRADX(PI / width())}.
   *
   * @see #mouseRADX(float)
   */
  public float mouseRADX() {
    return mouseRADX(PApplet.HALF_PI / (float) width());
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
    return pApplet.mouseY - pApplet.pmouseY;
  }

  /**
   * Same as {@code return mouseRADY(PI / height())}.
   *
   * @see #mouseRADY(float)
   */
  public float mouseRADY() {
    return mouseRADY(PApplet.HALF_PI / (float) height());
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
    return pApplet.mouseX - _upperLeftCornerX;
  }

  /**
   * Returns the current mouse y coordinate.
   */
  public int mouseY() {
    return pApplet.mouseY - _upperLeftCornerY;
  }

  /**
   * Returns the previous mouse x coordinate.
   */
  public int pmouseX() {
    return pApplet.pmouseX - _upperLeftCornerX;
  }

  /**
   * Returns the previous mouse y coordinate.
   */
  public int pmouseY() {
    return pApplet.pmouseY - _upperLeftCornerY;
  }

  /**
   * Returns the mouse speed expressed in pixels per milliseconds.
   */
  public float mouseSpeed() {
    float distance = Vector.distance(new Vector(pmouseX(), pmouseY()), new Vector(mouseX(), mouseY()));
    long now = System.nanoTime();
    //long now = System.currentTimeMillis();
    long delay = now - _timestamp;
    float speed = delay == 0 ? distance : distance / (float) delay;
    speed *= 1e6; // only if nanos are used
    //System.out.println(speed);
    _timestamp = now;
    return speed;
  }

  /**
   * Same as {@code return super.tracks(node, mouseX(), mouseY())}.
   *
   * @see #tracks(Node, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public boolean tracks(Node node) {
    return super.tracks(node, mouseX(), mouseY());
  }

  /**
   * Same as {@code return super.updateTag(mouseX(), mouseY(), nodeList)}.
   *
   * @see Graph#updateTag(int, int, List< Node >)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(List<Node> nodeList) {
    return super.updateTag(mouseX(), mouseY(), nodeList);
  }

  /**
   * Same as {@code return super.track(mouse(), nodeList)}.
   *
   * @see Graph#updateTag(String, int, int, List< Node >)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(String tag, List<Node> nodeList) {
    return super.updateTag(tag, mouseX(), mouseY(), nodeList);
  }

  /**
   * Same as {@code return super.track(mouse(), nodeArray)}.
   *
   * @see Graph#updateTag(int, int, Node[])
   */
  public Node updateTag(Node[] nodeArray) {
    return super.updateTag(mouseX(), mouseY(), nodeArray);
  }

  /**
   * Same as {@code return super.updateTag(tag, mouseX(), mouseY(), nodes)}.
   *
   * @see #updateTag(int, int, Node[])
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(String tag, Node[] nodes) {
    return super.updateTag(tag, mouseX(), mouseY(), nodes);
  }

  /**
   * Same as {@code return updateTag(null, tag)}.
   *
   * @see #updateTag(Node, String)
   */
  public Node updateTag(String tag) {
    return updateTag(null, tag);
  }

  /**
   * Same as {@code return super.updateTag(mouseX(), mouseY())}.
   *
   * @see #updateTag(Node, String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag() {
    return super.updateTag(null, null, mouseX(), mouseY());
  }

  /**
   * Same as {@code return updateTag(subtree, null)}.
   *
   * @see #updateTag(Node, String)
   */
  public Node updateTag(Node subtree) {
    return updateTag(subtree, null);
  }

  /**
   * Same as {@code return super.updateTag(subtree, tag, mouseX(), mouseY())}.
   *
   * @see #updateTag(Node, String, int, int)
   */
  public Node updateTag(Node subtree, String tag) {
    return super.updateTag(subtree, tag, mouseX(), mouseY());
  }

  /**
   * Same as {@code super.tag(tag, mouseX(), mouseY())}.
   *
   * @see Graph#tag(String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void tag(String tag) {
    super.tag(tag, mouseX(), mouseY());
  }

  /**
   * Same as {@code super.tag(mouseX(), mouseY())}.
   *
   * @see #tag(int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void tag() {
    super.tag(mouseX(), mouseY());
  }

  /**
   * Same as {@code shift(Graph.inertia)}.
   *
   * @see #shift(float)
   */
  public void shift() {
    shift(Graph.inertia);
  }

  /**
   * Same as {@code shift((String)null, inertia)}.
   *
   * @see #shift(String, float)
   */
  public void shift(float inertia) {
    shift((String)null, inertia);
  }

  /**
   * Same as {@code shift(tag, Graph.inertia)}.
   *
   * @see #shift(String, float)
   */
  public void shift(String tag) {
    shift(tag, Graph.inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) shift(node(tag), inertia)}.
   *
   * @see #shift(Node, float)
   */
  public void shift(String tag, float inertia) {
    if (tag == null || node(tag) != null)
      shift(node(tag), inertia);
  }

  /**
   * Same as {@code }.
   *
   * @see #shift(Node, float)
   */
  public void shift(Node node) {
    shift(node, Graph.inertia);
  }

  /**
   * Same as {@code super.shift(node, mouseDX() * (1 - lag), mouseDY() * (1 - lag), 0, lag)}.
   * It tries to keep the node under the mouse cursor independently of {@code lag} which should
   * be in [0..1], 0 responds immediately and 1 no response at all.
   *
   * @see #mouseDX()
   * @see #mouseDY()
   * @see #shift(Node, float, float, float, float)
   */
  public void shift(Node node, float lag) {
    float l = Math.abs(lag);
    while (l > 1)
      l /= 10;
    if (l != lag)
      System.out.println("Warning: lag should be in [0..1]. Setting it as " + l);
    // hack: idea is to have node always under the cursor
    super.shift(node, mouseDX() * (1 - l), mouseDY() * (1 - l), 0, l);
  }

  /**
   * Same as {@code spin((String)null)}.
   *
   * @see #spin(String)
   */
  public void spin() {
    spin((String)null);
  }

  /**
   * Same as {@code spin(tag, Graph.inertia)}.
   *
   * @see #spin(String, float)
   */
  public void spin(String tag) {
    spin(tag, Graph.inertia);
  }

  /**
   * Same as {@code spin((String)null, inertia)}.
   *
   * @see #spin(String, float)
   */
  public void spin(float inertia) {
    spin((String)null, inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) spin(node(tag), inertia)}.
   *
   * @see #spin(Node, float)
   */
  public void spin(String tag, float inertia) {
    if (tag == null || node(tag) != null)
      spin(node(tag), inertia);
  }

  /**
   * Same as {@code spin(node, Graph.inertia)}.
   *
   * @see #spin(Node, float)
   */
  public void spin(Node node) {
    spin(node, Graph.inertia);
  }

  /**
   * Same as {@code super.spinNode(node, pmouseX(), pmouseY(), mouseX(), mouseY(), inertia)}.
   *
   * @see #spin(Node, int, int, int, int, float)
   */
  public void spin(Node node, float inertia) {
    if (inertia == 1) {
      // Sensitivity is expressed in pixels per milliseconds. Default value is 30 (300 pixels per second).
      float sensitivity = 30;
      super.spin(node, pmouseX(), pmouseY(), mouseX(), mouseY(), mouseSpeed() > sensitivity ? 1 : 0.9f);
    } else
      super.spin(node, pmouseX(), pmouseY(), mouseX(), mouseY(), inertia);
  }

  // only eye

  /**
   * Same as {@code super.lookAround(mouseRADX(), mouseRADY(), inertia)}.
   *
   * @see #lookAround(float, float, float)
   */
  public void lookAround(float inertia) {
    super.lookAround(mouseRADX(), mouseRADY(), inertia);
  }

  /**
   * Same as {@code super.lookAround(mouseRADX(), mouseRADY())}.
   *
   * @see #lookAround(float, float)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void lookAround() {
    super.lookAround(mouseRADX(), mouseRADY());
  }

  /**
   * Same as {@code super.rotateCAD(mouseRADX(), mouseRADY(), new Vector(0, 1, 0), inertia)}.
   *
   * @see #cad(float, float, Vector, float)
   */
  public void cad(float inertia) {
    super.cad(mouseRADX(), mouseRADY(), new Vector(0, 1, 0), inertia);
  }

  /**
   * Same as {@code super.rotateCAD(mouseRADX(), mouseRADY())}.
   *
   * @see #cad(float, float)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void cad() {
    super.cad(mouseRADX(), mouseRADY());
  }

  /**
   * Same as {@code super.rotateCAD(mouseRADX(), mouseRADY(), up, inertia)}.
   *
   * @see #cad(float, float, Vector, float)
   */
  public void cad(Vector up, float inertia) {
    super.cad(mouseRADX(), mouseRADY(), up, inertia);
  }

  /**
   * Same as {@code super.rotateCAD(mouseRADX(), mouseRADY(), up)}.
   *
   * @see #cad(float, float, Vector)
   * @see #mouseRADX()
   * @see #mouseRADY()
   */
  public void cad(Vector up) {
    super.cad(mouseRADX(), mouseRADY(), up);
  }
}
