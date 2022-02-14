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
 * Another scene's eye (different than the graph eye) can be drawn with
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
 * spinning, to name a few.
 *
 * @see Graph
 * @see Node
 */
public class Scene extends Graph {
  // CONSTRUCTORS

  /**
   * Same as {@code this(pApplet.g)}.
   *
   * @see #Scene(PGraphics)
   */
  public Scene(PApplet pApplet) {
    this(pApplet.g);
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
   * Same as {@code super(pGraphics, pGraphics.width, pGraphics.height, eye)}.
   *
   * @see Graph#Graph(PGraphics, int, int)
   * @see #Scene(PApplet)
   */
  public Scene(PGraphics pGraphics) {
    super(pGraphics, pGraphics.width, pGraphics.height);
    _init(pGraphics);
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
   * @see Graph#Graph(PGraphics, int, int, Vector, float)
   */
  public Scene(PGraphics pGraphics, Vector center, float radius) {
    super(pGraphics, pGraphics.width, pGraphics.height, center, radius);
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

  // OPENGL

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
   * Saves the eye, the {@link #radius()} and the {@link #_type} into {@code fileName}.
   *
   * @see #saveConfig()
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig(String fileName) {
    JSONObject json = new JSONObject();
    json.setFloat("radius", _radius);
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
   * Loads the eye, the {@link #radius()} and the {@link #_type} from {@code fileName}.
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

  // drawing

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
}
