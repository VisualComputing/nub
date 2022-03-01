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

import processing.core.*;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.opengl.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.nio.FloatBuffer;

/**
 * A 2D or 3D scene.
 * <h1>1. Scene handling</h1>
 * A scene forms a tree of {@link Node}s whose visual representations may be
 * {@link #render()}. To render a subtree call {@link #render(Node)}.
 * Note that rendering routines should be called within your main-event loop.
 * <p>
 * The node collection belonging to the scene may be retrieved with {@link #nodes()}.
 * Nub provides other useful routines to handle the hierarchy, such as {@link Node#setReference(Node)},
 * {@link Node#detach()}, {@link Node#attach()}, {@link #branch(Node)}, and {@link #clearTree()}.
 * <h2>Transformations</h2>
 * The scene acts as interface between screen space (a box of {@link #width()} * {@link #height()} * 1
 * dimensions), from where user gesture data is gathered, and the {@code nodes}. To transform points
 * from/to screen space to/from node space use {@link #location(Vector, Node)} and
 * {@link #screenLocation(Vector, Node)}. To transform vectors from/to screen space to/from node space
 * use {@link #displacement(Vector, Node)} and {@link #screenDisplacement(Vector, Node)}.
 * <h1>2. Picking and interaction</h1>
 * Picking a node to interact with it is a two-step process:
 * <ol>
 * <li>Tag the node using an arbitrary name (which may be {@code null}) either with
 * {@link #tag(String, Node)}) or ray-casting: {@link #updateTag(String, int, int, Node[])},
 * {@link #updateTag(String, int, int)} or {@link #tag(String, int, int)}. While
 * {@link #updateTag(String, int, int, Node[])} and {@link #updateTag(String, int, int)} update the
 * tagged node synchronously (i.e., they return the tagged node immediately),
 * {@link #tag(String, int, int)} updates it asynchronously (i.e., it optimally updates the tagged
 * node during the next call to the {@link #render()} or {@link #render(Node)} algorithms); and, </li>
 * <li>Interact with your tagged nodes by calling any of the following methods: {@link #align(String)},
 * {@link #focus(String)}, {@link #shift(String, float, float, float, float)},
 * {@link #turn(String, float, float, float, float)}, {@link #zoom(String, float, float)},
 * or {@link #spin(String, int, int, int, int, float)}).
 * </li>
 * </ol>
 * Observations:
 * <ol>
 * <li>Refer to {@link Node#bullsEyeSize()} (and {@link Node#setBullsEyeSize(float)}) for the different
 * ray-casting node picking policies.</li>
 * <li>To check if a given node would be picked with a ray casted at a given screen position,
 * call {@link #tracks(Node, int, int)}.</li>
 * <li>To interact with the node that is referred with the {@code null} tag, call any of the following methods:
 * {@link #align()}, {@link #focus()}, {@link #shift(float, float, float, float)},
 * {@link #turn(float, float, float, float)}, {@link #zoom(float, float)} and
 * {@link #spin(int, int, int, int, float)}).</li>
 * <li>To directly interact with a given node, call any of the following methods: {@link #align(Node)},
 * {@link #focus(Node)}, {@link #shift(Node, float, float, float, float)},
 * {@link #turn(Node, float, float, float, float)},
 * {@link #zoom(Node, float, float)} and {@link #spin(Node, int, int, int, int, float)}).</li>
 * <li>To either interact with the node referred with a given tag or the eye, when that tag is not in use,
 * call any of the following methods: {@link #align(Node)}, {@link #focus(String)},
 * {@link #shift(String, float, float, float, float)}, {@link #turn(String, float, float, float, float)},
 * {@link #zoom(String, float, float)} and {@link #spin(String, int, int, int, int, float)}.</li>
 * <li>Set {@code Scene.inertia} in  [0..1] (0 no inertia & 1 no friction) to change the default inertia
 * value globally, instead of setting it on a per method call basis. Note that it is initially set to 0.8.</li>
 * <li>Customize node behaviors by overridden {@link Node#interact(Object...)}
 * and then invoke them by either calling: {@link #interact(Object...)},
 * {@link #interact(String, Object...)} or {@link #interact(Node, Object...)}.
 * </li>
 * </ol>
 * <h1>3. Visibility and culling techniques</h1>
 * Geometry may be culled against the viewing volume by calling {@link #isPointVisible(Vector)},
 * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}.
 * <h1>4. Drawing functionality</h1>
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
 * To define your geometry on the screen coordinate system (such as when drawing 2d controls
 * on top of a 3d scene) issue your drawing code between {@link #beginHUD()} and
 * {@link #endHUD()}.
 */
public class Scene {
  // number of frames displayed since this timing handler was instantiated.
  static protected long _frameCount;

  protected static Scene _onscreenScene;
  public static Random random = new Random();
  protected static int _hudCalls;
  protected static HashSet<Node> _huds = new HashSet<Node>();
  protected HashSet<Node> _cacheHUDs;
  protected static HashSet<Node> _interpolators = new HashSet<Node>();

  // Custom render
  protected HashMap<Integer, BiConsumer<Scene, Node>> _behaviors;

  // offscreen
  protected int _upperLeftCornerX, _upperLeftCornerY;
  protected long _lastDisplayed, _lastRendered;
  protected boolean _offscreen;

  // 0. Contexts
  protected PGraphics _fb;
  // 1. Eye
  protected Node _eye;
  protected boolean _male;
  protected long _lastEqUpdate;
  public Vector center = new Vector();
  public float radius = 100;
  public static float inertia = 0.85f;
  // TODO restore cad and other only eye methods
  //protected Vector _eyeUp;
  //bounds eqns
  protected float[][] _coefficients;
  protected Vector[] _normal;
  protected float[] _distance;
  // handed
  protected boolean _leftHanded;

  // 2. Matrix handler
  protected int _renderCount;
  protected int _width, _height;
  // _bb : picking buffer
  public boolean picking;
  protected Matrix _projection, _view, _projectionView, _projectionViewInverse;
  protected long _cacheProjectionViewInverse;

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
   * Returns {@code true} if {@code o} is instance of {@link Integer}, {@link Float} or {@link Double},
   * and {@code false} otherwise.
   */
  public static boolean isNumInstance(Object o) {
    return o instanceof Float || o instanceof Integer || o instanceof Double;
  }

  /**
   * Cast {@code o} to a {@link Float}. Returns {@code null} if {@code o} is not a num instance
   * (See {@link #isNumInstance(Object)}).
   */
  public static Float castToFloat(Object o) {
    return isNumInstance(o) ? o instanceof Integer ? ((Integer) o).floatValue() :
        o instanceof Double ? ((Double) o).floatValue() : o instanceof Float ? (Float) o : null : null;
  }

  /**
   * Cast {@code o} to an {@link Integer}. Returns {@code null} if {@code o} is not a num instance
   * (See {@link #isNumInstance(Object)}).
   */
  public static Integer castToInt(Object o) {
    return isNumInstance(o) ? o instanceof Float ? ((Float) o).intValue() :
        o instanceof Double ? ((Double) o).intValue() : o instanceof Integer ? (Integer) o : null : null;
  }

  static final int _color(float v1, float v2, float v3) {
    if (v1 > 255.0f) {
      v1 = 255.0f;
    } else if (v1 < 0.0f) {
      v1 = 0.0f;
    }
    if (v2 > 255.0f) {
      v2 = 255.0f;
    } else if (v2 < 0.0f) {
      v2 = 0.0f;
    }
    if (v3 > 255.0f) {
      v3 = 255.0f;
    } else if (v3 < 0.0f) {
      v3 = 0.0f;
    }
    return -16777216 | (int) v1 << 16 | (int) v2 << 8 | (int) v3;
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

  // 3. Handlers
  protected class Ray {
    public String _tag;
    public int _pixelX, _pixelY;

    Ray(String tag, int pixelX, int pixelY) {
      _tag = tag;
      _pixelX = pixelX;
      _pixelY = pixelY;
    }
  }

  public static boolean _seeded;
  protected boolean _seededGraph;
  protected HashMap<String, Node> _tags;
  protected ArrayList<Ray> _i1rays, _i2rays, _irays, _orays;

  // 4. Scene
  protected static List<Node> _seeds = new ArrayList<Node>();
  protected long _lastNonEyeUpdate = 0;

  // 5. Interaction methods
  Vector _upVector;
  protected long _lookAroundCount;

  // 6. Visibility

  /**
   * Enumerates the different visibility states an object may have respect to the bounding volume.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  // 7. Projection stuff

  protected Type _type;

  /**
   * Enumerates the scene types.
   * <p>
   * The type mainly defines the way the projection matrix is computed.
   */
  public enum Type {
    PERSPECTIVE, ORTHOGRAPHIC
  }

  // 8. Scene stuff
  @FunctionalInterface
  public interface Callback {
    void execute();
  }

  public static String prettyVersion = "0.9.97";
  public static String version = "11";

  // P R O C E S S I N G A P P L E T A N D O B J E C T S
  public static PApplet pApplet;

  // mouse speed
  long _timestamp;

  // Prettify bb display if required
  protected HashMap<Integer, Integer> _idToColor = new HashMap<>();

  // high-level constructors

  /**
   * Same as {@code this(pApplet.g)}.
   *
   * @see #Scene(PGraphics)
   */
  public Scene(PApplet pApplet) {
    this(pApplet.g);
  }

  /**
   * Same as {@code this(pGraphics, pGraphics.width, pGraphics.height, eye)}.
   *
   * @see #Scene(PApplet)
   */
  public Scene(PGraphics pGraphics) {
    if (!_seeded) {
      _seededGraph = true;
      _seeded = true;
    }
    _fb = pGraphics;
    setWidth(pGraphics.width);
    setHeight(pGraphics.height);
    _tags = new HashMap<String, Node>();
    _i1rays = new ArrayList<Ray>();
    _i2rays = new ArrayList<Ray>();
    _irays = _i1rays;
    // dummy
    _orays = _i2rays;
    _behaviors = new HashMap<Integer, BiConsumer<Scene, Node>>();
    picking = true;
    // 1. P5 objects
    if (pApplet == null) pApplet = pGraphics.parent;
    _offscreen = pGraphics != pApplet.g;
    if (!(pGraphics instanceof PGraphicsOpenGL))
      throw new RuntimeException("context() is not instance of PGraphicsOpenGL");
    if (!_offscreen && _onscreenScene == null)
      _onscreenScene = this;
    // 2. Register P5 methods
    pApplet.registerMethod("pre", this);
    pApplet.registerMethod("draw", this);
    pApplet.registerMethod("dispose", this);
  }

  // JSON

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
      System.out.println("Warning: no config saved! Off-screen scene config requires saveConfig(String fileName) to be called");
    else
      saveConfig("data/config.json");
  }

  /**
   * Saves the eye.
   *
   * @see #saveConfig()
   * @see #loadConfig()
   * @see #loadConfig(String)
   */
  public void saveConfig(String fileName) {
    JSONObject json = new JSONObject();
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
      System.out.println("Warning: no config loaded! Off-screen scene config requires loadConfig(String fileName) to be called");
    else
      loadConfig("config.json");
  }

  /**
   * Loads the eye from {@code fileName}.
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

  // Mouse

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
   * Same as {@code return Node.random(this)}. Creates a random node.
   *
   * @see Node#random(Scene)
   * @see #randomize(Node)
   */
  public Node randomNode() {
    return Node.random(this);
  }

  /**
   * Same as {@code node.randomize(center(), radius())}.
   *
   * @see Node#randomize(Vector, float)
   */
  public void randomize(Node node) {
    node.randomize(center, radius);
  }

  // Dimensions stuff

  /**
   * Returns the {@link #width()} to {@link #height()} aspect ratio of the display window.
   */
  public float aspectRatio() {
    return (float) width() / (float) height();
  }

  /**
   * Returns width of the screen window.
   */
  public int width() {
    return _width;
  }

  /**
   * Returns height of the screen window.
   */
  public int height() {
    return _height;
  }

  /**
   * Sets the scene {@link #width()} in pixels.
   */
  public void setWidth(int width) {
    if (width != width() && width > 0) {
      _width = width;
      _modified();
    }
  }

  /**
   * Sets the scene {@link #height()} in pixels.
   */
  public void setHeight(int height) {
    if (height != height() && height > 0) {
      _height = height;
      _modified();
    }
  }

  // Scene and nodes stuff

  /**
   * Returns the top-level nodes (those which reference is null).
   * <p>
   * All leading nodes are also reachable by the {@link #render()} algorithm for which they are the seeds.
   *
   * @see #nodes()
   * @see Node#detach()
   */
  protected static List<Node> _leadingNodes() {
    return _seeds;
  }

  /**
   * Returns {@code true} if the node is top-level.
   */
  protected static boolean _isLeadingNode(Node node) {
    for (Node leadingNode : _leadingNodes())
      if (leadingNode == node)
        return true;
    return false;
  }

  /**
   * Add the node as top-level if its reference node is null and it isn't already added.
   */
  protected static boolean _addLeadingNode(Node node) {
    if (node == null || node.reference() != null)
      return false;
    if (_isLeadingNode(node))
      return false;
    return _leadingNodes().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected static boolean _removeLeadingNode(Node node) {
    boolean result = false;
    Iterator<Node> it = _leadingNodes().iterator();
    while (it.hasNext()) {
      if (it.next() == node) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Same as {@code for(Node node : _leadingNodes()) detach(node)}.
   *
   * @see Node#detach()
   */
  public static void clearTree() {
    for (Node node : _leadingNodes()) {
      node.detach();
    }
  }

  /**
   * Returns a list of all the nodes that are reachable by the {@link #render()}
   * algorithm.
   * <p>
   * Note that node collections should be kept at user space for efficiency.
   */
  public static List<Node> nodes() {
    ArrayList<Node> list = new ArrayList<Node>();
    for (Node node : _leadingNodes())
      _collect(list, node);
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes.
   */
  public static List<Node> branch(Node node) {
    ArrayList<Node> list = new ArrayList<Node>();
    _collect(list, node);
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. Note that for a node to be collected
   * it must be reachable.
   */
  protected static void _collect(List<Node> list, Node node) {
    if (node == null)
      return;
    list.add(node);
    for (Node child : node.children())
      _collect(list, child);
  }

  // Eye stuff

  /**
   * Same as {@code return isPointVisible(new Vector(x, y, z))}.
   *
   * @see #isPointVisible(Vector)
   */
  public boolean isPointVisible(float x, float y, float z) {
    return isPointVisible(new Vector(x, y, z));
  }

  /**
   * Returns {@code true} if {@code point} is visible (i.e, lies within the eye bounds)
   * and {@code false} otherwise.
   *
   * @see #distanceToBound(int, Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #bounds()
   */
  public boolean isPointVisible(Vector point) {
    if (_eye.lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = _frameCount;
    }
    for (int i = 0; i < 6; ++i)
      if (distanceToBound(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Returns {@link Visibility#VISIBLE}, {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the ball (of radius {@code radius}
   * and center {@link #center}) is visible, invisible, or semi-visible, respectively.
   *
   * @see #distanceToBound(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #boxVisibility(Vector, Vector)
   * @see #bounds()
   * @see #_updateBounds()
   */
  public Visibility ballVisibility(Vector center, float radius) {
    if (_eye.lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = _frameCount;
    }
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      float d = distanceToBound(i, center);
      if (d > radius)
        return Visibility.INVISIBLE;
      if ((d > 0) || (-d < radius))
        allInForAllPlanes = false;
    }
    if (allInForAllPlanes)
      return Visibility.VISIBLE;
    return Visibility.SEMIVISIBLE;
  }

  /**
   * Returns {@link Visibility#VISIBLE}, {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the axis aligned box
   * (defined by corners {@code p1} and {@code p2}) is visible, invisible,
   * or semi-visible, respectively.
   *
   * @see #distanceToBound(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #bounds()
   * @see #_updateBounds()
   */
  public Visibility boxVisibility(Vector corner1, Vector corner2) {
    if (_eye.lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = _frameCount;
    }
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      boolean allOut = true;
      for (int c = 0; c < 8; ++c) {
        Vector pos = new Vector(((c & 4) != 0) ? corner1._vector[0] : corner2._vector[0], ((c & 2) != 0) ? corner1._vector[1] : corner2._vector[1],
            ((c & 1) != 0) ? corner1._vector[2] : corner2._vector[2]);
        if (distanceToBound(i, pos) > 0.0)
          allInForAllPlanes = false;
        else
          allOut = false;
      }
      // The eight points are on the outside side of this plane
      if (allOut)
        return Visibility.INVISIBLE;
    }
    if (allInForAllPlanes)
      return Visibility.VISIBLE;
    // Too conservative, but tangent cases are too expensive to detect
    return Visibility.SEMIVISIBLE;
  }

  /**
   * Updates the 4 or 6 plane equations of the eye bounds.
   * <p>
   * In 2D the four 4-component vectors, respectively correspond to the
   * left, right, top and bottom eye bounds lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * In 3D the six 4-component vectors, respectively correspond to the
   * left, right, near, far, top and bottom frustum planes. Each vector holds a
   * plane equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   */
  protected void _updateBounds() {
    _initCoefficients();
    _updateBoundaryEquations3();
  }

  protected void _initCoefficients() {
    int rows = 6, cols = 4;
    if (_coefficients == null)
      _coefficients = new float[rows][cols];
    else if (_coefficients.length != rows)
      _coefficients = new float[rows][cols];
    if (_normal == null) {
      _normal = new Vector[rows];
      for (int i = 0; i < _normal.length; i++)
        _normal[i] = new Vector();
    } else if (_normal.length != rows) {
      _normal = new Vector[rows];
      for (int i = 0; i < _normal.length; i++)
        _normal[i] = new Vector();
    }
    if (_distance == null)
      _distance = new float[rows];
    else if (_distance.length != rows)
      _distance = new float[rows];
  }

  protected void _updateBoundaryEquations3() {
    // Computed once and for all
    Vector pos = _eye.worldPosition();
    Vector viewDir = viewDirection();
    Vector up = upVector();
    Vector right = rightVector();
    float posViewDir = Vector.dot(pos, viewDir);
    switch (_type) {
      case PERSPECTIVE: {
        float hhfov = hfov() / 2.0f;
        float chhfov = (float) Math.cos(hhfov);
        float shhfov = (float) Math.sin(hhfov);
        _normal[0] = Vector.multiply(viewDir, -shhfov);
        _normal[1] = Vector.add(_normal[0], Vector.multiply(right, chhfov));
        _normal[0] = Vector.add(_normal[0], Vector.multiply(right, -chhfov));
        _normal[2] = Vector.multiply(viewDir, -1);
        _normal[3] = viewDir;
        float hfov = fov() / 2.0f;
        float chfov = (float) Math.cos(hfov);
        float shfov = (float) Math.sin(hfov);
        _normal[4] = Vector.multiply(viewDir, -shfov);
        _normal[5] = Vector.add(_normal[4], Vector.multiply(up, -chfov));
        _normal[4] = Vector.add(_normal[4], Vector.multiply(up, chfov));
        for (int i = 0; i < 2; ++i)
          _distance[i] = Vector.dot(pos, _normal[i]);
        for (int j = 4; j < 6; ++j)
          _distance[j] = Vector.dot(pos, _normal[j]);
        // Natural equations are:
        // dist[0,1,4,5] = pos * normal[0,1,4,5];
        // dist[2] = (pos + zNear() * viewDir) * normal[2];
        // dist[3] = (pos + zFar() * viewDir) * normal[3];
        // 2 times less computations using expanded/merged equations. Dir vectors
        // are normalized.
        float posRightCosHH = chhfov * Vector.dot(pos, right);
        _distance[0] = -shhfov * posViewDir;
        _distance[1] = _distance[0] + posRightCosHH;
        _distance[0] = _distance[0] - posRightCosHH;
        float posUpCosH = chfov * Vector.dot(pos, up);
        _distance[4] = -shfov * posViewDir;
        _distance[5] = _distance[4] - posUpCosH;
        _distance[4] = _distance[4] + posUpCosH;
        break;
      }
      case ORTHOGRAPHIC:
        _normal[0] = Vector.multiply(right, -1);
        _normal[1] = right;
        _normal[4] = up;
        _normal[5] = Vector.multiply(up, -1);
        float wh0 = Math.abs(right() - left()) / 2;
        float wh1 = Math.abs(top() - bottom()) / 2;
        _distance[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh0)), _normal[0]);
        _distance[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh0)), _normal[1]);
        _distance[4] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh1)), _normal[4]);
        _distance[5] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh1)), _normal[5]);
        break;
    }
    // Front and far planes are identical for both camera types.
    _normal[2] = Vector.multiply(viewDir, -1);
    _normal[3] = viewDir;
    _distance[2] = -posViewDir - near();
    _distance[3] = posViewDir + far();
    for (int i = 0; i < 6; ++i) {
      _coefficients[i][0] = _normal[i]._vector[0];
      _coefficients[i][1] = _normal[i]._vector[1];
      _coefficients[i][2] = _normal[i]._vector[2];
      _coefficients[i][3] = _distance[i];
    }
  }

  /**
   * Returns the bounds plane equations.
   * <p>
   * In 2D the four 4-component vectors, respectively correspond to the
   * left, right, top and bottom eye bounds lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * In 3D the six 4-component vectors returned by this method, respectively correspond to the
   * left, right, near, far, top and bottom eye bounding planes. Each vector holds a plane
   * equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   *
   * @see #distanceToBound(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #_updateBounds()
   */
  public float[][] bounds() {
    if (_eye.lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = _frameCount;
    }
    return _coefficients;
  }

  /**
   * Returns the signed distance between point {@code position} and plane {@code index}
   * in world units. The distance is negative if the point lies in the planes's bounding
   * halfspace, and positive otherwise.
   * <p>
   * In 2D {@code index} is a value between {@code 0} and {@code 3} which respectively
   * correspond to the left, right, top and bottom eye bounding planes.
   * <p>
   * In 3D {@code index} is a value between {@code 0} and {@code 5} which respectively
   * correspond to the left, right, near, far, top and bottom eye bounding planes.
   *
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #bounds()
   */
  public float distanceToBound(int index, Vector position) {
    if (_eye.lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = _frameCount;
    }
    Vector myVector = new Vector(_coefficients[index][0], _coefficients[index][1], _coefficients[index][2]);
    return Vector.dot(position, myVector) - _coefficients[index][3];
  }

  /**
   * Returns the pixel to scene (units) ratio at {@code position}.
   * <p>
   * Convenience function that simply returns {@code 1 / sceneToPixelRatio(position)}.
   *
   * @see #sceneToPixelRatio(Vector)
   */
  public float pixelToSceneRatio(Vector position) {
    return 1 / sceneToPixelRatio(position);
  }

  /**
   * Returns the ratio of scene (units) to pixel at {@code position}.
   * <p>
   * A line of {@code n * sceneToPixelRatio()} scene units, located at {@code position} in
   * the world coordinate system, will be projected with a length of {@code n} pixels on
   * screen.
   * <p>
   * Use this method to scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #center} and
   * always directed along the screen vertical direction ({@link #upVector()}):
   * <p>
   * {@code beginShape(LINES);}<br>
   * {@code vertex(scene.center().x(), scene.center().y(), scene.center().z());}<br>
   * {@code Vector v = Vector.add(scene.center(), Vector.multiply(scene.upVector(), 20 * scene.sceneToPixelRatio(scene.center())));}
   * <br>
   * {@code vertex(v.x(), v.y(), v.z());}<br>
   * {@code endShape();}<br>
   */
  public float sceneToPixelRatio(Vector position) {
    switch (_type) {
      case PERSPECTIVE:
        // TODO: QGLViewer
        //return 2.0 * fabs((frame()->coordinatesOf(position)).z) * tan(fieldOfView() / 2.0) / screenHeight();
        return 2.0f * Math.abs((_eye.location(position))._vector[2]) * (float) Math.tan(fov() / 2) / (float) height();
      case ORTHOGRAPHIC:
        return Math.abs(top() - bottom()) / (float) height();
    }
    return 1.0f;
  }

  /**
   * Same as {@code return !isFaceBackFacing(a, b, c)}.
   *
   * @see #isFaceBackFacing(Vector, Vector, Vector)
   */
  public boolean isFaceFrontFacing(Vector a, Vector b, Vector c) {
    return !isFaceBackFacing(a, b, c);
  }

  /**
   * Returns {@code true} if the given face is back-facing the eye. Otherwise returns
   * {@code false}.
   * <p>
   * Vertices must given in clockwise order if scene is right-handed or in counter-clockwise otherwise.
   *
   * @param a first face vertex
   * @param b second face vertex
   * @param c third face vertex
   * @see #isFaceBackFacing(Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector a, Vector b, Vector c) {
    return isFaceBackFacing(a, _leftHanded ?
        Vector.subtract(b, a).cross(Vector.subtract(c, a)) :
        Vector.subtract(c, a).cross(Vector.subtract(b, a)));
  }

  /**
   * Same as {@code return !isFaceBackFacing(vertex, normal)}.
   *
   * @see #isFaceBackFacing(Vector, Vector)
   */
  public boolean isFaceFrontFacing(Vector vertex, Vector normal) {
    return !isFaceBackFacing(vertex, normal);
  }

  /**
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   *
   * @param vertex belonging to the face
   * @param normal face normal
   * @see #isFaceBackFacing(Vector, Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector vertex, Vector normal) {
    return isConeBackFacing(vertex, normal, 0);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vector, ArrayList)
   */
  public boolean isConeFrontFacing(Vector vertex, ArrayList<Vector> normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the eye and {@code false} otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals ArrayList of normals defining the cone.
   * @see #isConeBackFacing(Vector, Vector[])
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeBackFacing(Vector vertex, ArrayList<Vector> normals) {
    return isConeBackFacing(vertex, normals.toArray(new Vector[normals.size()]));
  }

  /**
   * Same as {@code !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vector, Vector[])
   */
  public boolean isConeFrontFacing(Vector vertex, Vector[] normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the eye and {@code false} otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals Array of normals defining the cone.
   * @see #isConeBackFacing(Vector, ArrayList)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeBackFacing(Vector vertex, Vector[] normals) {
    float angle;
    Vector axis = new Vector(0, 0, 0);
    if (normals.length == 0)
      throw new RuntimeException("Normal array provided is empty");
    Vector[] n = new Vector[normals.length];
    for (int i = 0; i < normals.length; i++) {
      n[i] = new Vector();
      n[i].set(normals[i]);
      n[i].normalize();
      axis = Vector.add(axis, n[i]);
    }
    if (axis.magnitude() != 0)
      axis.normalize();
    else
      axis.set(0, 0, 1);
    angle = 0;
    for (int i = 0; i < normals.length; i++)
      angle = Math.max(angle, (float) Math.acos(Vector.dot(n[i], axis)));
    return isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, axis, angle)}.
   *
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isConeFrontFacing(Vector vertex, Vector axis, float angle) {
    return !isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the eye and {@code false} otherwise.
   *
   * @param vertex Cone vertex
   * @param axis   Cone axis
   * @param angle  Cone angle
   */
  public boolean isConeBackFacing(Vector vertex, Vector axis, float angle) {
    // more or less inspired by this:
    // http://en.wikipedia.org/wiki/Back-face_culling (perspective case :P)
    Vector camAxis;
    if (_type == Type.ORTHOGRAPHIC)
      camAxis = viewDirection();
    else {
      camAxis = Vector.subtract(vertex, _eye.worldPosition());
      if (angle != 0)
        camAxis.normalize();
    }
    if (angle == 0)
      return Vector.dot(camAxis, axis) >= 0;
    float absAngle = Math.abs(angle);
    if (absAngle >= Math.PI / 2)
      return true;
    Vector faceNormal = axis.copy();
    faceNormal.normalize();
    return Math.acos(Vector.dot(camAxis, faceNormal)) + absAngle < Math.PI / 2;
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the eye
   * ({@code node().worldDisplacement(new Vector(0.0f, 0.0f, -1.0f))}). In 2D
   * it always is (0,0,-1). It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    return _eye.zAxis(false);
  }

  /**
   * Returns the normalized up vector of the eye, defined in the world coordinate system.
   * <p>
   * It corresponds to the Y axis of the associated eye (actually returns
   * {@code node().yAxis()}
   */
  public Vector upVector() {
    return _eye.yAxis();
  }

  /**
   * Returns the normalized right vector of the eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}.
   * <p>
   * Simply returns {@code node().xAxis()}.
   */
  public Vector rightVector() {
    return _eye.xAxis();
  }

  /**
   * 2D eyes return the position. 3D eyes return a point defined in the world
   * coordinate system where the eyes is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   */
  public Vector at() {
    return Vector.add(_eye.worldPosition(), viewDirection());
  }

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
   * Gives the coefficients of a 3D half-line passing through the eye position and pixel
   * (pixelX,pixelY). Origin in the upper left corner. Use {@link #height()} - pixelY to locate the
   * origin at the lower left corner.
   * <p>
   * The origin of the half line (eye position) is stored in {@code origin}, while
   * {@code direction} contains the properly oriented and normalized direction of the half line.
   * <p>
   * This method is useful for analytical intersection in a selection method.
   */
  public void pixelToLine(int pixelX, int pixelY, Vector origin, Vector direction) {
    switch (_type) {
      case PERSPECTIVE:
        // left-handed coordinate system correction
        if (_leftHanded) {
          pixelY = height() - pixelY;
        }
        origin.set(_eye.worldPosition());
        /*
        // TODO: QGLViewer
        dir = Vec(((2.0 * pixel.x() / screenWidth()) - 1.0) * tan(fieldOfView() / 2.0) * aspectRatio(),
                  ((2.0 * (screenHeight() - pixel.y()) / screenHeight()) - 1.0) * tan(fieldOfView() / 2.0),
                   -1.0);
        */
        direction.set(new Vector(((2.0f * pixelX / (float) width()) - 1.0f) * (float) Math.tan(fov() / 2.0f) * aspectRatio(),
                                 ((2.0f * (height() - pixelY) / (float) height()) - 1.0f) * (float) Math.tan(fov() / 2.0f),
                                 -1.0f));
        direction.set(Vector.subtract(_eye.worldLocation(direction), origin));
        direction.normalize();
        break;
      case ORTHOGRAPHIC: {
        origin.set(location(new Vector(pixelX, pixelY, 0)));
        direction.set(viewDirection());
        break;
      }
    }
  }

  protected void _modified() {
    _lastNonEyeUpdate = _frameCount;
  }

  /**
   * Max between {@link Node#lastUpdate()} and {@link #_lastNonEyeUpdate()}.
   *
   * @return last frame the eye was updated
   * @see #_lastNonEyeUpdate()
   */
  public long lastUpdate() {
    return Math.max(_eye.lastUpdate(), _lastNonEyeUpdate());
  }

  /**
   * @return last frame when a local eye parameter (different than the eye) was updated.
   * @see #lastUpdate()
   */
  protected long _lastNonEyeUpdate() {
    return _lastNonEyeUpdate;
  }

  // traversal

  /**
   * Same as {@code return this.track(mouse(), nodeArray)}.
   *
   * @see Scene#updateTag(int, int, Node[])
   */
  public Node updateTag(Node[] nodeArray) {
    return this.updateTag(mouseX(), mouseY(), nodeArray);
  }

  /**
   * Same as {@code return track(null, pixelX, pixelY, nodeArray)}.
   *
   * @see #updateTag(String, int, int, Node[])
   */
  public Node updateTag(int pixelX, int pixelY, Node[] nodeArray) {
    return updateTag(null, pixelX, pixelY, nodeArray);
  }

  /**
   * Same as {@code return this.updateTag(tag, mouseX(), mouseY(), nodes)}.
   *
   * @see #updateTag(int, int, Node[])
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(String tag, Node[] nodes) {
    return this.updateTag(tag, mouseX(), mouseY(), nodes);
  }

  /**
   * Tags (with {@code tag} which may be {@code null}) the node in {@code nodeArray} picked with ray-casting
   * at pixel {@code pixelX, pixelY} and returns it (see {@link #node(String)}).
   *
   * @see #updateTag(Node, String, int, int)
   * @see #updateTag(String, int, int)
   * @see #updateTag(String, int, int, List)
   * @see #render()
   * @see #node(String)
   * @see #removeTag(String)
   * @see #tracks(Node, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   * @see Node#bullsEyeSize()
   * @see Node#setBullsEyeSize(float)
   * @see #tag(String, int, int)
   * @see #tag(String, int, int)
   */
  public Node updateTag(String tag, int pixelX, int pixelY, Node[] nodeArray) {
    removeTag(tag);
    for (Node node : nodeArray)
      if (tracks(node, pixelX, pixelY)) {
        tag(tag, node);
        break;
      }
    return node(tag);
  }

  /**
   * Same as {@code return this.updateTag(mouseX(), mouseY(), nodeList)}.
   *
   * @see Scene#updateTag(int, int, List< Node >)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(List<Node> nodeList) {
    return this.updateTag(mouseX(), mouseY(), nodeList);
  }

  /**
   * Same as {@code return track(null, pixelX, pixelY, nodeList)}.
   *
   * @see #updateTag(String, int, int, List)
   */
  public Node updateTag(int pixelX, int pixelY, List<Node> nodeList) {
    return updateTag(null, pixelX, pixelY, nodeList);
  }

  /**
   * Same as {@code return this.track(mouse(), nodeList)}.
   *
   * @see #updateTag(String, int, int, List< Node >)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag(String tag, List<Node> nodeList) {
    return this.updateTag(tag, mouseX(), mouseY(), nodeList);
  }

  /**
   * Same as {@link #updateTag(String, int, int, Node[])} but using a node list instead of an array.
   *
   * @see #updateTag(String, int, int, Node[])
   */
  public Node updateTag(String tag, int pixelX, int pixelY, List<Node> nodeList) {
    removeTag(tag);
    for (Node node : nodeList)
      if (tracks(node, pixelX, pixelY)) {
        tag(tag, node);
        break;
      }
    return node(tag);
  }

  /**
   * Same as {@code return updateTag(null, null, pixelX, pixelY)}.
   *
   * @see #updateTag(Node, String, int, int)
   */
  public Node updateTag(int pixelX, int pixelY) {
    return updateTag(null, null, pixelX, pixelY);
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
   * Same as {@code return updateTag(subtree, null)}.
   *
   * @see #updateTag(Node, String)
   */
  public Node updateTag(Node subtree) {
    return updateTag(subtree, null);
  }

  /**
   * Same as {@code return return updateTag(subtree, null, pixelX, pixelY)}.
   *
   * @see #updateTag(Node, String, int, int)
   */
  public Node updateTag(Node subtree, int pixelX, int pixelY) {
    return updateTag(subtree, null, pixelX, pixelY);
  }

  /**
   * Tags (with {@code tag} which may be {@code null}) the node in {@link #nodes()} picked with ray-casting at pixel
   * {@code pixelX, pixelY} and returns it (see {@link #node(String)}). May return {@code null} if no node is intersected by
   * the ray. Not that the eye is never tagged. Same as {@code return updateTag(null, tag, pixelX, pixelY)}.
   *
   * @see #updateTag(Node, int, int)
   * @see #updateTag(String, int, int, Node[])
   * @see #render()
   * @see #node(String)
   * @see #removeTag(String)
   * @see #tracks(Node, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   * @see Node#bullsEyeSize()
   * @see Node#setBullsEyeSize(float)
   * @see #tag(String, int, int)
   */
  public Node updateTag(String tag, int pixelX, int pixelY) {
    return updateTag(null, tag, pixelX, pixelY);
  }

  /**
   * Same as {@code return this.updateTag(mouseX(), mouseY())}.
   *
   * @see #updateTag(Node, String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public Node updateTag() {
    return this.updateTag(null, null, mouseX(), mouseY());
  }

  /**
   * Same as {@code return this.updateTag(subtree, tag, mouseX(), mouseY())}.
   *
   * @see #updateTag(Node, String, int, int)
   */
  public Node updateTag(Node subtree, String tag) {
    return this.updateTag(subtree, tag, mouseX(), mouseY());
  }

  /**
   * Tags (with {@code tag} which may be {@code null}) the node in the {@code subtree} (or the whole tree when
   * {@code subtree} is {@code null}) picked with ray-casting at pixel {@code pixelX, pixelY} and returns it
   * (see {@link #node(String)}). May return {@code null} if no node is intersected by the ray.
   * Not that the eye is never tagged.
   *
   * @see #updateTag(String, int, int)
   * @see #updateTag(String, int, int, Node[])
   * @see #render()
   * @see #node(String)
   * @see #removeTag(String)
   * @see #tracks(Node, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   * @see Node#bullsEyeSize()
   * @see Node#setBullsEyeSize(float)
   * @see #tag(String, int, int)
   */
  public Node updateTag(Node subtree, String tag, int pixelX, int pixelY) {
    removeTag(tag);
    if (subtree == null) {
      for (Node node : _leadingNodes())
        _track(tag, node, pixelX, pixelY);
    } else {
      _track(tag, subtree, pixelX, pixelY);
    }
    return node(tag);
  }

  /**
   * Use internally by {@link #updateTag(String, int, int)}.
   */
  protected void _track(String tag, Node node, int pixelX, int pixelY) {
    if (node(tag) == null && node.tagging == true && (node._bypass != _frameCount))
      if (tracks(node, pixelX, pixelY)) {
        tag(tag, node);
        return;
      }
    if (!node.cull && node(tag) == null)
      for (Node child : node.children())
        _track(tag, child, pixelX, pixelY);
  }

  /**
   * Condition for the node front picking.
   */
  protected boolean _frontPicking(Node node) {
    return picking && node.tagging == true && !_isEye(node) && node.isHintEnabled(Node.BULLSEYE);
  }

  /**
   * Same as {@code return this.tracks(node, mouseX(), mouseY())}.
   *
   * @see #tracks(Node, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public boolean tracks(Node node) {
    return this.tracks(node, mouseX(), mouseY());
  }

  /**
   * Casts a ray at pixel position {@code (pixelX, pixelY)} and returns {@code true} if the ray picks the {@code node} and
   * {@code false} otherwise. The node is picked according to the {@link Node#bullsEyeSize()}.
   *
   * @see #node(String)
   * @see #removeTag(String)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   * @see Node#bullsEyeSize()
   * @see Node#setBullsEyeSize(float)
   */
  public boolean tracks(Node node, int pixelX, int pixelY) {
    if(_frontPicking(node)) {
      return _tracks(node, pixelX, pixelY, screenLocation(node.worldPosition()));
    }
    return false;
  }

  /**
   * Cached version of {@link #tracks(Node, int, int)}.
   */
  protected boolean _tracks(Node node, int pixelX, int pixelY, Vector projection) {
    if (node == null || _isEye(node) || projection == null)
      return false;
    if (!node.tagging)
      return false;
    float threshold = node.bullsEyeSize() < 1 ?
        100 * node.bullsEyeSize() * node.magnitude() * pixelToSceneRatio(node.worldPosition()) :
        node.bullsEyeSize() / 2;
    return node._bullsEyeShape == Node.BullsEyeShape.SQUARE ?
        ((Math.abs(pixelX - projection._vector[0]) < threshold) && (Math.abs(pixelY - projection._vector[1]) < threshold)) :
        (float) Math.sqrt((float) Math.pow((projection._vector[0] - pixelX), 2.0) + (float) Math.pow((projection._vector[1] - pixelY), 2.0)) < threshold;
  }

  /**
   * Used by tracking to never track an eye node.
   */
  protected boolean _isEye(Node eye) {
    return eye ==  null ? false : this._eye == eye;
  }

  /**
   * Same as {@code this.tag(mouseX(), mouseY())}.
   *
   * @see #tag(int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void tag() {
    this.tag(mouseX(), mouseY());
  }

  /**
   * Same as {@code tag(null, pixelX, pixelY)}.
   *
   * @see #tag(String, int, int)
   */
  public void tag(int pixelX, int pixelY) {
    tag(null, pixelX, pixelY);
  }

  /**
   * Same as {@code this.tag(tag, mouseX(), mouseY())}.
   *
   * @see #tag(String, int, int)
   * @see #mouseX()
   * @see #mouseY()
   */
  public void tag(String tag) {
    this.tag(tag, mouseX(), mouseY());
  }

  /**
   * Same as {@link #updateTag(String, int, int)} but doesn't return immediately the tagged node.
   * The algorithm schedules an updated of the node to be tagged for the next traversal and hence
   * should be always be used in conjunction with {@link #render()}.
   * <p>
   * The tagged node (see {@link #node(String)}) would be available after the next call to
   * {@link #render()}. It may be {@code null} if no node is intersected by the ray. Not that
   * the eye is never tagged.
   * <p>
   * This method is optimal since it tags the nodes at traversal time. Prefer this method over
   * {@link #updateTag(String, int, int)} when dealing with several tags.
   *
   * @see #render()
   * @see #node(String)
   * @see #removeTag(String)
   * @see #tracks(Node, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   * @see Node#bullsEyeSize()
   * @see Node#setBullsEyeSize(float)
   * @see #tag(int, int)
   */
  public void tag(String tag, int pixelX, int pixelY) {
    _irays.add(new Ray(tag, pixelX, pixelY));
  }

  // Off-screen

  /**
   * Returns whether or not the scene has focus or not under the given pixel.
   */
  public boolean hasFocus(int pixelX, int pixelY) {
    return _lastDisplayed + 1 >= _frameCount
        && _upperLeftCornerX <= pixelX && pixelX < _upperLeftCornerX + width()
        && _upperLeftCornerY <= pixelY && pixelY < _upperLeftCornerY + height();
  }

  /**
   * Internal use by {@link #hasFocus(int, int)} and the display of offscreen scenes.
   */
  protected void _setUpperLeftCorner(int x, int y) {
    _upperLeftCornerX = x;
    _upperLeftCornerY = y;
  }

  /**
   * Returns {@code true} if this scene is off-screen and {@code false} otherwise.
   */
  public boolean isOffscreen() {
    return _offscreen;
  }

  /**
   * Returns the main renderer context.
   */
  public PGraphicsOpenGL context() {
    return (PGraphicsOpenGL) _fb;
  }

  /**
   * Paint method which is called just before your main event loop starts.
   * Handles timing tasks, resize events, prepares caches, and opens the
   * context if the scene is onscreen.
   * <p>
   * This method should be registered at the PApplet (which requires it to be public and named
   * as pre) and hence you don't need to call it.
   *
   * @see #draw()
   * @see #render()
   * @see #isOffscreen()
   */
  public void pre() {
    if (_seededGraph) {
      _frameCount++;
    }
    if (!isOffscreen()) {
      if ((width() != context().width))
        setWidth(context().width);
      if ((height() != context().height))
        setHeight(context().height);
    }
    _cacheHUDs = new HashSet<Node>(_huds);
    if (!isOffscreen()) {
      openContext();
    }
  }

  /**
   * Paint method which is called just after your main event loop. Closes the context if the scene
   * is onscreen and renders when needed the back buffer (useful for picking).
   * <p>
   * This method should be registered at the PApplet (which requires it to be pubic and be named
   * as draw) and hence you don't need to call it.
   *
   * @see #pre()
   * @see #render()
   * @see #isOffscreen()
   */
  public void draw() {
    if (!isOffscreen()) {
      closeContext();
    }
    // swap rays
    _orays.clear();
    _orays = _irays;
    _irays = _irays == _i1rays ? _i2rays : _i1rays;
  }

  // caches

  /**
   * Returns the cached projection matrix computed at {@link #openContext()}.
   */
  public Matrix projection() {
    return _projection;
  }

  /**
   * Returns the cached view matrix computed at {@link #_bind()}.
   */
  public Matrix view() {
    return _view;
  }

  /**
   * Returns the projection times view cached matrix computed at {@link #openContext()}}.
   */
  public Matrix projectionView() {
    return _projectionView;
  }

  /**
   * Returns the projection times view inverse matrix.
   */
  public Matrix projectionViewInverse() {
    if (_cacheProjectionViewInverse < _frameCount) {
      _projectionViewInverse = Matrix.inverse(_projectionView);
      _cacheProjectionViewInverse = _frameCount;
    }
    return _projectionViewInverse;
  }

  /**
   * Resets the eye. Same as {@code setEye(null)}.
   */
  public void resetEye() {
    setEye(null);
  }

  /**
   * Sets the eye. {@link #untag(Node)} is called iff {@link #hasTag(Node)}.
   */
  public void setEye(Node eye) {
    _male = eye != null;
    if (_male) {
      if (isTagged(eye)) {
        untag(eye);
        System.out.println("Warning: node was untagged since it was set as the eye");
      }
      if (_eye != null) {
        _eye._frustumScenes.remove(this);
        _eye._keyframesMask = Node.AXES;
      }
      _eye = eye;
      _eye._frustumScenes.add(this);
      _eye._keyframesMask = Node.CAMERA;
    }
    _modified();
  }

  // see: https://stackoverflow.com/questions/10830293/decompose-projection-matrix44-to-left-right-bottom-top-near-and-far-boundary/12926655

  /**
   * Retrieves the left clipping plane.
   */
  public float left() {
    return context().projection.m33 == 1 ? -(1 + context().projection.m03 ) / context().projection.m00 :
            near() * (context().projection.m02 - 1) / context().projection.m00;
  }

  /**
   * Retrieves the right clipping plane.
   */
  public float right() {
    return context().projection.m33 == 1 ? (1 - context().projection.m03) / context().projection.m00 :
            near() * (context().projection.m02 + 1) / context().projection.m00;
  }

  /**
   * Retrieves the top clipping plane.
   */
  public float top() {
    return context().projection.m33 == 1 ? (context().projection.m13 - 1) / context().projection.m11 :
            near() * (context().projection.m12 - 1) / context().projection.m11;
  }

  /**
   * Retrieves the bottom clipping plane.
   */
  public float bottom() {
    return context().projection.m33 == 1 ? (1 + context().projection.m13) / context().projection.m11 :
            near() * (1 - context().projection.m12) / context().projection.m11;
  }

  /**
   * Retrieves the near clipping plane.
   */
  public float near() {
    return context().projection.m33 == 0 ? context().projection.m23 / (context().projection.m22 - 1) :
            (1 + context().projection.m23) / context().projection.m22;
  }

  /**
   * Retrieves the far clipping plane.
   */
  public float far() {
    return context().projection.m33 == 0 ? context().projection.m23 / (context().projection.m22 + 1) :
            - (1 - context().projection.m23) / context().projection.m22;
  }

  /**
   * Retrieves the scene field-of-view in radians. Only meaningful for perspective projections.
   *
   * @see #hfov()
   */
  public float fov() {
    if (context().projection.m33 != 0) {
      throw new RuntimeException("Error: fov only works for a perspective projection");
    }
    return Math.abs(2 * (float) Math.atan(1 / context().projection.m11));
  }

  /**
   * Returns the eye horizontal field-of-view in radians. Only meaningful for perspective projections.
   *
   * @see #fov()
   */
  public float hfov() {
    if (context().projection.m33 != 0) {
      throw new RuntimeException("Error: hfov only works for a perspective projection");
    }
    return Math.abs(2 * (float) Math.atan(1 / context().projection.m00));
  }

  /**
   * Same as {@code beginHUD(context())}.
   *
   * @see #beginHUD(PGraphicsOpenGL)
   * @see #context()
   */
  public void beginHUD() {
    beginHUD(context());
  }

  /**
   * Same as {@code endHUD(context())}.
   *
   * @see #endHUD(PGraphicsOpenGL)
   * @see #context()
   */
  public void endHUD() {
    endHUD(context());
  }

  /**
   * Begin Heads Up Display (HUD)  on {@code pg} so that drawing can be done using 2D
   * screen coordinates.
   * <p>
   * All screen drawing should be enclosed between {@link #beginHUD()} and
   * {@link #endHUD()}. Then you can just begin drawing your screen shapes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d scene
   * then draw first all your 3d before doing any call to a {@link #beginHUD()}
   * and {@link #endHUD()} pair.
   * <p>
   * <b>Warning:</b> Offscreen scenes should call {@link #beginHUD()} and {@link #endHUD()}
   * before closing the context ({@link #closeContext()}).
   *
   * @see #endHUD()
   */
  public void beginHUD(PGraphicsOpenGL pg) {
    pg.hint(pg.DISABLE_OPTIMIZED_STROKE);
    disableDepthTest(pg);
    if (_hudCalls < 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
              + "endHUD() and they cannot be nested. Check your implementation!");
    _hudCalls++;
    pg.pushProjection();
    pg.setProjection(Scene.toPMatrix(Matrix.hudProjection(pg.width, pg.height)));
    pg.pushMatrix();
    pg.setMatrix(Scene.toPMatrix(Matrix.hudView(pg.width, pg.height)));
  }

  /**
   * Ends Heads Up Display (HUD) on {@code pg}. Throws an exception if
   * {@link #beginHUD()} wasn't properly called before.
   *
   * @see #beginHUD()
   */
  public void endHUD(PGraphicsOpenGL pg) {
    _hudCalls--;
    if (_hudCalls < 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
              + "endHUD() and they cannot be nested. Check your implementation!");
    pg.popProjection();
    pg.popMatrix();
    enableDepthTest(pg);
    pg.hint(pg.ENABLE_OPTIMIZED_STROKE);
  }

  /**
   * Similar to {@link #_applyTransformation(PGraphicsOpenGL, Node)}, but applies the global
   * transformation defined by the {@code node}.
   */
  protected void _applyWorldTransformation(PGraphicsOpenGL pg, Node node) {
    Node reference = node.reference();
    if (reference != null) {
      _applyWorldTransformation(pg, reference);
      _applyTransformation(pg, node);
    } else {
      _applyTransformation(pg, node);
    }
  }

  /**
   * Apply the local transformation defined by {@code node}, i.e., respect to its
   * {@link Node#reference()}. The {@code node} is first translated, then rotated around
   * the new translated origin and then scaled.
   */
  protected void _applyTransformation(PGraphicsOpenGL pg, Node node) {
    pg.translate(node.position()._vector[0], node.position()._vector[1], node.position()._vector[2]);
    pg.rotate(node.orientation().angle(), (node.orientation()).axis()._vector[0], (node.orientation()).axis()._vector[1], (node.orientation()).axis()._vector[2]);
    pg.scale(node.magnitude(), node.magnitude(), node.magnitude());
  }

  /**
   * Cache matrices and binds processing and nub matrices together.
   */
  protected void _bind() {
    _projection = Scene.toMatrix(context().projection);
    _leftHanded = _projection.m11() < 0;
    if (_male) {
      _eye._execute();
    }
    else {
      _eye = new Node(false);
      _eye.fromWorldMatrix(Matrix.inverse(Scene.toMatrix(context().modelview)));
    }
    _view = _eye.view();
    _projectionView = Matrix.multiply(_projection, _view);
    _type = _projection.m33() == 0 ? Type.PERSPECTIVE : Type.ORTHOGRAPHIC;
    // debug
    if (_male) {
      context().setMatrix(Scene.toPMatrix(_view));
    }
  }

  /**
   * Begins the rendering process (see {@link #render(Node)}). Use it always before
   * {@link #closeContext()}. Binds the matrices to the renderer.
   * <p>
   * This method is automatically called by {@link #render(Node)}. Call it explicitly when
   * you need to customize that which is to be rendered, as follows:
   * <pre>
   * {@code
   * scene.openContext();
   * worldCustomRender()
   * scene.closeContext();
   * }
   * </pre>
   * You may call {@link #render(Node)} within {@code openContext} and {@code closeContext},
   * before {@code openContext} or after {@code closeContext}, or not even call it.
   *
   * @see #render(Node)
   * @see #closeContext()
   * @see #isOffscreen()
   * @see #context()
   */
  public void openContext() {
    _renderCount++;
    if (_renderCount != 1) {
      throw new RuntimeException(isOffscreen() ? "Error: render() should be nested within a single openContext() / closeContext() call!" : "openContext() / closeContext is only available for off-screen scenes");
    }
    else {
      if (isOffscreen()) {
        context().beginDraw();
      }
      _bind();
      context().pushMatrix();
    }
  }

  /**
   * Ends the rendering process (see {@link #render(Node)}). Use it always after
   * {@link #openContext()}. Clears the picking cache. Displays the scene HUD.
   *
   * @see #render(Node)
   * @see #openContext()
   * @see #isOffscreen()
   * @see #context()
   */
  public void closeContext() {
    _renderCount--;
    if (_renderCount != 0) {
      throw new RuntimeException("Error: render() should be nested within a single openContext() / closeContext() call!");
    }
    else {
      if (_lastRendered == _frameCount) {
        _displayPaths();
        _displayHUD();
      }
      context().popMatrix();
      if (isOffscreen()) {
        context().endDraw();
      }
      else {
        _lastDisplayed = _frameCount;
      }
    }
  }

  /**
   * Renders the node tree onto the {@link #context()} from the eye viewpoint.
   * Calls {@link #addBehavior(Node, BiConsumer)} on each visited node (refer to the {@link Node} documentation).
   * Same as {@code render(null)}.
   *
   * @see #render(Node)
   * @see #addBehavior(Node, BiConsumer)
   * @see Node#cull
   * @see Node#bypass()
   * @see Node#setShape(Consumer)
   * @see Node#setShape(processing.core.PShape)
   */
  public void render() {
    render(null);
  }

  /**
   * Calls {@link #openContext()}, then renders the node {@code subtree} (or the whole tree
   * when {@code subtree} is {@code null}) onto the {@link #context()} from the eye
   * viewpoint, and calls {@link #closeContext()}. All rendered nodes are marked as such
   * ({@link Node#rendered(Scene)}). After issuing a render command you'll be left out
   * at the world coordinate system. If the scene is offscreen this method should be called
   * within {@link #openContext()} and {@link #closeContext()}.
   *
   * <p>
   * Note that the rendering algorithm executes the custom behavior (set with
   * {@link #addBehavior(Node, BiConsumer)}) on each rendered node.
   * (refer to the {@link Node} documentation).
   *
   * @see #addBehavior(Node, BiConsumer)
   * @see Node#cull
   * @see Node#bypass()
   * @see Node#setShape(Consumer)
   * @see Node#setShape(processing.core.PShape)
   */
  public void render(Node subtree) {
    if (_renderCount != 1) {
      throw new RuntimeException("Error: context should be open before render offscreen scenes!");
    }
    _lastRendered = _frameCount;
    if (subtree == null) {
      for (Node node : _leadingNodes()) {
        _render(node);
      }
    } else if (subtree.isAttached()) {
      if (subtree.reference() != null) {
        context().pushMatrix();
        _applyWorldTransformation(context(), subtree.reference());
      }
      _render(subtree);
      if (subtree.reference() != null) {
        context().popMatrix();
      }
    }
  }

  /**
   * Adds a custom node behavior to be executed for this scene
   * {@link #render()} algorithm.
   * <p>
   * Bypassing the node rendering and/or performing hierarchical culling, i.e.,
   * culling of the node and its children, should be done here.
   *
   * <pre>
   * {@code
   * Scene scene = new Scene(context, width, height);
   * Node space = new Node();
   * public void behavior(Scene scene, Node node) {
   *   if (scene.cullingCondition) {
   *     node.cull = true;
   *   }
   *   else if (bypassCondition) {
   *     node.bypass();
   *   }
   * }
   * scene.addBehavior(space, behavior);
   * }
   * </pre>
   * Note that the scene culling condition may be set from
   * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}.
   *
   * @see #addBehavior(Node, Consumer)
   * @see #resetBehavior(Node)
   * @see #render(Node)
   * @see Node#setBehavior(Scene, BiConsumer)
   * @see Node#setBehavior(Scene, Consumer)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void addBehavior(Node node, BiConsumer<Scene, Node> behavior) {
    _behaviors.put(node.id(), behavior);
  }

  /**
   * Same as {@code setBehavior(node, (g, n) -> behavior.accept(n))}.
   *
   * @see #addBehavior(Node, BiConsumer)
   * @see #resetBehavior(Node)
   * @see #render(Node)
   * @see Node#setBehavior(Scene, BiConsumer)
   * @see Node#setBehavior(Scene, Consumer)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void addBehavior(Node node, Consumer<Node> behavior) {
    addBehavior(node, (g, n) -> behavior.accept(n));
  }

  /**
   * Resets the node custom behavior which is set with {@link #addBehavior(Node, BiConsumer)}.
   *
   * @see #addBehavior(Node, BiConsumer)
   * @see #render(Node)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void resetBehavior(Node node) {
    _behaviors.remove(node.id());
  }

  /**
   * Used by the {@link #render(Node)} algorithm.
   */
  protected void _render(Node node) {
    context().pushMatrix();
    node._execute();
    _applyTransformation(context(), node);
    // TODO ordering of operations is a bit experimental.
    // For instance should the visits go before pushMatrix?
    // I believe it belongs here, i.e., current node culling
    // condition may require local geometry operations.
    // On the oder hand, timing stuff (node_execute) may only
    // be executed once it's known for sure the node is not
    // culled :-/
    BiConsumer<Scene, Node> behavior = _behaviors.get(node.id());
    if (behavior != null) {
      behavior.accept(this, node);
    }
    if (!node.cull) {
      if (node._bypass != _frameCount) {
        node._update(this);
        _trackFrontBuffer(node);
        if (isTagged(node) && node._highlight > 0 && node._highlight <= 1) {
          context().pushMatrix();
          float scl = 1 + node._highlight;
          context().scale(scl, scl, scl);
          _displayFrontHint(node);
          context().popMatrix();
        } else {
          _displayFrontHint(node);
        }
      }
      for (Node child : node.children()) {
        _render(child);
      }
    }
    context().popMatrix();
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
      if (_lastRendered != Scene._frameCount) {
        System.out.println("Warning: image() not updated since render wasn't called @" + Scene._frameCount);
      }
      if (_renderCount != 0) {
        throw new RuntimeException("Error: offscreen scenes should call image() after openContext() / closeContext()");
      }
      else {
        if (_onscreenScene != null) {
          _onscreenScene.beginHUD();
        }
        pApplet.pushStyle();
        _setUpperLeftCorner(pixelX, pixelY);
        _lastDisplayed = Scene._frameCount;
        pApplet.imageMode(PApplet.CORNER);
        pApplet.image(context(), pixelX, pixelY);
        pApplet.popStyle();
        if (_onscreenScene != null) {
          _onscreenScene.endHUD();
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
   * Displays the scene and nodes hud hint.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayHUD() {
    if (!_cacheHUDs.isEmpty()) {
      context().pushStyle();
      beginHUD();
      Iterator<Node> iterator = _cacheHUDs.iterator();
      while(iterator.hasNext()) {
        Node node = iterator.next();
        if (node.rendered(this) && node.isHintEnabled(Node.HUD)) {
          context().pushMatrix();
          Vector location = screenLocation(node);
          if (location != null) {
            context().translate(location.x(), location.y());
            if (node._imrHUD != null) {
              node._imrHUD.accept(context());
            }
            if (node._rmrHUD != null) {
              context().shape(node._rmrHUD);
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

  /**
   * Displays all node keyframes' spline hints. Default implementation is empty, i.e., it is
   * meant to be implemented by derived classes.
   */
  protected void _displayPaths() {
    context().pushStyle();
    for (Node interpolator : _interpolators) {
      if (interpolator.rendered(this) && interpolator.isHintEnabled(Node.KEYFRAMES)) {
        context().pushStyle();
        _drawSpline(interpolator);
        context().popStyle();
      }
    }
    context().popStyle();
  }

  /**
   * Draws the node {@link Node#hint()} onto the {@link #context()}.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayFrontHint(Node node) {
    PGraphics pg = context();
    if (node.isHintEnabled(Node.SHAPE)) {
      pg.pushStyle();
      if (node._rmrShape != null) {
        pg.shapeMode(pg.shapeMode);
        pg.shape(node._rmrShape);
      }
      if (node._imrShape != null) {
        node._imrShape.accept(pg);
      }
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.TORUS)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.fill(node._torusColor);
      drawTorusSolenoid(pg, node._torusFaces, 5);
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.BOUNDS)) {
      for (Scene scene : node._frustumScenes) {
        if (scene != this) {
          pg.pushStyle();
          pg.colorMode(PApplet.RGB, 255);
          pg.strokeWeight(node._boundsWeight);
          pg.stroke(((PGraphics) scene.context()).backgroundColor);
          pg.fill(((PGraphics) scene.context()).backgroundColor);
          drawFrustum(pg, scene);
          pg.popStyle();
        }
      }
    }
    if (node.isHintEnabled(Node.AXES)) {
      pg.pushStyle();
      drawAxes(pg, node._axesLength == 0 ? radius / 5 : node._axesLength);
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.CAMERA)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(node._cameraStroke);
      pg.fill(node._cameraStroke);
      _drawEye(pg, node._cameraLength == 0 ? radius : node._cameraLength);
      pg.popStyle();
    }
    if (node.isHintEnabled(Node.BULLSEYE)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(node._bullsEyeStroke);
      _drawBullsEye(node);
      pg.popStyle();
    }
  }

  /**
   * Internally used by {@link #_render(Node)}.
   */
  protected void _trackFrontBuffer(Node node) {
    if (_frontPicking(node) && _orays != null) {
      if (!_orays.isEmpty()) {
        Vector projection = screenLocation(node.worldPosition());
        Iterator<Ray> it = _orays.iterator();
        while (it.hasNext()) {
          Ray ray = it.next();
          removeTag(ray._tag);
          if (_tracks(node, ray._pixelX, ray._pixelY, projection)) {
            tag(ray._tag, node);
            it.remove();
          }
        }
      }
    }
  }

  /**
   * Same as {@code tag(null, node)}.
   *
   * @see #tag(String, Node)
   */
  public void tag(Node node) {
    tag(null, node);
  }

  /**
   * Tags the {@code node} (with {@code tag} which may be {@code null})
   * (see {@link #node(String)}). Tagging the eye is not allowed.
   * Call {@link #updateTag(String, int, int)} or
   * {@link #tag(String, int, int)} to tag the node with ray casting.
   *
   * @see #tracks(Node, int, int)
   * @see #updateTag(String, int, int)
   * @see #removeTag(String)
   * @see #hasTag(String, Node)
   * @see Node#tagging
   */
  public void tag(String tag, Node node) {
    if (node == null) {
      System.out.println("Warning. Cannot tag a null node!");
      return;
    }
    if (node == _eye) {
      System.out.println("Warning. Cannot tag the eye!");
      return;
    }
    if (!node.tagging) {
      System.out.println("Warning. Node cannot be tagged! Enable tagging on the node first by call node.enableTagging(true)");
      return;
    }
    _tags.put(tag, node);
  }

  /**
   * Same as {@code return node(null)}.
   *
   * @see #node(String)
   */
  public Node node() {
    return node(null);
  }

  /**
   * Returns the node tagged with {@code tag} (which may be {@code null}) which is usually set by
   * ray casting (see {@link #updateTag(String, int, int)}). May return {@code null}. Reset it with
   * {@link #removeTag(String)}.
   *
   * @see #tracks(Node, int, int)
   * @see #updateTag(String, int, int)
   * @see #removeTag(String)
   * @see #hasTag(String, Node)
   * @see #tag(String, Node)
   */
  public Node node(String tag) {
    return _tags.get(tag);
  }

  /**
   * Same as {@code isTagValid(null)}.
   *
   * @see #isTagValid(String)
   */
  public boolean isTagValid() {
    return isTagValid(null);
  }

  /**
   * Returns {@code true} if some node is tagged with {@code tag} (which may be {code null})
   * and {@code false} otherwise.
   */
  public boolean isTagValid(String tag) {
    return _tags.containsKey(tag);
  }

  /**
   * Returns {@code true} if the node is currently being tagged and {@code false} otherwise.
   */
  public boolean isTagged(Node node) {
    return _tags.containsValue(node);
  }

  /**
   * Same as {@code return hasTag(null, node)}.
   *
   * @see #hasTag(String, Node)
   */
  public boolean hasTag(Node node) {
    return hasTag(null, node);
  }

  /**
   * Returns {@code true} if {@code node(tag)} (see {@link #node(String)})
   * returns {@code node} and {@code false} otherwise.
   *
   * @see #tracks(Node, int, int)
   * @see #updateTag(String, int, int)
   * @see #removeTag(String)
   * @see #tag(String, Node)
   * @see Node#isTagged(Scene)
   */
  public boolean hasTag(String tag, Node node) {
    return node(tag) == node;
  }

  /**
   * Removes all tags so that {@link #node(String)} returns {@code null}.
   *
   * @see #node(String)
   * @see #tracks(Node, int, int)
   * @see #updateTag(String, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   */
  public void clearTags() {
    _tags.clear();
  }

  /**
   * Disables tagging the node. Calls {@code unTag(node)} and then {@code node.disableTagging()}.
   *
   * @see #untag(Node)
   * @see Node#tagging
   */
  public void disableTagging(Node node) {
    untag(node);
    node.tagging = false;
  }

  /**
   * Removes all tags pointing to the {@code node}.
   */
  public void untag(Node node) {
    _tags.entrySet().removeIf(entry -> (node == entry.getValue()));
  }

  /**
   * Same as {@code removeTag(null)}.
   *
   * @see #removeTag(String)
   */
  public void removeTag() {
    removeTag(null);
  }

  /**
   * Removes the {@code tag} so that a call to {@link #isTagValid(String)}
   * will return {@code false}.
   *
   * @see #node(String)
   * @see #tracks(Node, int, int)
   * @see #updateTag(String, int, int)
   * @see #tag(String, Node)
   * @see #hasTag(String, Node)
   */
  public void removeTag(String tag) {
    _tags.remove(tag);
  }

  // Screen to node conversion

  /**
   * Remap of {@code value} between two ranges. Used to convert locations between Screen and NDC.
   */
  protected static float _map(float value, float start1, float stop1, float start2, float stop2) {
    return start2 + (value - start1) * (stop2 - start2) / (stop1 - start1);
  }

  /**
   * Converts {@code vector} location from normalized device coordinates (NDC) to screen space.
   * {@link #screenToNDCLocation(Vector)} performs the inverse transformation.
   * {@link #ndcToScreenDisplacement(Vector)} transforms vector displacements instead of locations.
   *
   * @see #screenToNDCLocation(Vector)
   * @see #ndcToScreenDisplacement(Vector)
   */
  public Vector ndcToScreenLocation(Vector vector) {
    return new Vector(_map(vector.x(), -1, 1, 0, width()),
        _map(vector.y(), -1, 1, 0, height()),
        _map(vector.z(), -1, 1, 0, 1));
  }

  /**
   * Converts {@code vector} location from screen space to normalized device coordinates (NDC).
   * {@link #ndcToScreenLocation(Vector)} performs the inverse transformation.
   * {@link #screenToNDCDisplacement(Vector)} transforms vector displacements instead of locations.
   *
   * @see #ndcToScreenLocation(Vector)
   * @see #screenToNDCDisplacement(Vector)
   */
  public Vector screenToNDCLocation(Vector vector) {
    return new Vector(_map(vector.x(), 0, width(), -1, 1),
        _map(vector.y(), 0, height(), -1, 1),
        _map(vector.z(), 0, 1, -1, 1));
  }

  /**
   * Converts the {@code node} origin location to screen space.
   * Same as {@code return screenLocation(new Vector(), node)}.
   *
   * @see #screenLocation(Vector)
   * @see #screenLocation(Vector, Node)
   */
  public Vector screenLocation(Node node) {
    return screenLocation(new Vector(), node);
  }

  /**
   * Converts {@code vector} location from world to screen space.
   * Same as {@code return screenLocation(src, null)}.
   *
   * @see #screenLocation(Node)
   * @see #screenLocation(Vector, Node)
   */
  public Vector screenLocation(Vector vector) {
    return screenLocation(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code node} to screen.
   * Use {@link #location(Vector, Node)} to perform the inverse transformation.
   * <p>
   * The x and y coordinates of the returned vector are expressed in screen coordinates,
   * (0,0) being the upper left corner of the window. The z coordinate ranges between 0
   * (near plane) and 1 (excluded, far plane).
   *
   * @see #screenLocation(Node)
   * @see #screenLocation(Vector)
   * @see #location(Vector, Node)
   * @see #location(Vector)
   */
  public Vector screenLocation(Vector vector, Node node) {
    return _screenLocation(vector, node, projectionView(), width(), height());
  }

  /**
   * Static cached version of {@link #screenLocation(Vector, Node)}. Requires the programmer
   * to supply the cached {@code projectionView} matrix.
   */
  protected static Vector _screenLocation(Vector vector, Node node, Matrix projectionView, int width, int height) {
    return _screenLocation(node != null ? node.worldLocation(vector) : vector, projectionView, width, height);
  }

  protected static Vector _screenLocation(Vector obj, Matrix projectionViewMatrix, int width, int height) {
    float[] in = new float[4];
    float[] out = new float[4];
    in[0] = obj.x();
    in[1] = obj.y();
    in[2] = obj.z();
    in[3] = 1.0f;
    out[0] = projectionViewMatrix._matrix[0] * in[0] + projectionViewMatrix._matrix[4] * in[1] + projectionViewMatrix._matrix[8] * in[2]
            + projectionViewMatrix._matrix[12] * in[3];
    out[1] = projectionViewMatrix._matrix[1] * in[0] + projectionViewMatrix._matrix[5] * in[1] + projectionViewMatrix._matrix[9] * in[2]
            + projectionViewMatrix._matrix[13] * in[3];
    out[2] = projectionViewMatrix._matrix[2] * in[0] + projectionViewMatrix._matrix[6] * in[1] + projectionViewMatrix._matrix[10] * in[2]
            + projectionViewMatrix._matrix[14] * in[3];
    out[3] = projectionViewMatrix._matrix[3] * in[0] + projectionViewMatrix._matrix[7] * in[1] + projectionViewMatrix._matrix[11] * in[2]
            + projectionViewMatrix._matrix[15] * in[3];
    if (out[3] == 0.0)
      return null;
    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height;
    viewport[2] = width;
    viewport[3] = -height;
    // ndc, but y is inverted
    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];
    // Map x, y and z to range 0-1
    out[0] = out[0] * 0.5f + 0.5f;
    out[1] = out[1] * 0.5f + 0.5f;
    out[2] = out[2] * 0.5f + 0.5f;
    // Map x,y to viewport
    out[0] = out[0] * viewport[2] + viewport[0];
    out[1] = out[1] * viewport[3] + viewport[1];
    return new Vector(out[0], out[1], out[2]);
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
   * value is not a linear interpolation between {@link #near()} and {@link #far()}:
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
   * Convenience function that simply returns {@code location(pixel, null)}.
   * <p>
   * @see #location(Vector, Node)
   */
  public Vector location(Vector pixel) {
    return location(pixel, null);
  }

  /**
   * Returns the {@code node} coordinates of {@code pixel}.
   * <p>
   * The pixel (0,0) corresponds to the upper left corner of the window. The
   * {@code pixel.z()} is a depth value ranging in [0..1] (near and far plane respectively).
   * In 3D note that {@code pixel.z} is not a linear interpolation between {@link #near()} and
   * {@link #far()};
   * {@code pixel.z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z);} where {@code z}
   * is the distance from the point you project to the camera, along the {@link #viewDirection()}.
   * <p>
   * The result is expressed in the {@code node} coordinate system. When {@code node} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code node} hierarchy (i.e., when {@link Node#reference()} is non-null) is taken into
   * account.
   * <p>
   * {@link #screenLocation(Vector, Node)} performs the inverse transformation.
   * <p>
   * {@link #screenDisplacement(Vector, Node)} converts vector displacements instead of locations.
   * <p>
   * This method only uses the intrinsic eye parameters (view and projection matrices),
   * {@link #width()} and {@link #height()}). You can hence define a virtual eye and use
   * this method to compute un-projections out of a classical rendering context.
   *
   * @see #screenLocation(Vector, Node)
   * @see #screenDisplacement(Vector, Node)
   * @see #setWidth(int)
   * @see #setHeight(int)
   */
  public Vector location(Vector pixel, Node node) {
    return _location(pixel, node, projectionViewInverse(), width(), height());
  }

  /**
   * Static cached version of {@link #_location(Vector, Matrix, int, int)}. Requires the programmer
   * to suply the cached {@code projectionViewInverseMatrix} matrix.
   */
  protected static Vector _location(Vector pixel, Node node, Matrix projectionViewInverseMatrix, int width, int height) {
    Vector worldLocation = _location(pixel, projectionViewInverseMatrix, width, height);
    return node != null ? node.location(worldLocation) : worldLocation;
  }

  /**
   * Similar to {@code gluUnProject}: map window coordinates to object coordinates.
   *
   * @param win          Specify the window x-y-z coordinates.
   */
  protected static Vector _location(Vector win, Matrix projectionViewInverseMatrix, int width, int height) {
    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height;
    viewport[2] = width;
    viewport[3] = -height;
    float[] in = new float[4];
    float[] out = new float[4];
    in[0] = win.x();
    in[1] = win.y();
    in[2] = win.z();
    in[3] = 1.0f;
    /* Map x and y from window coordinates */
    in[0] = (in[0] - viewport[0]) / viewport[2];
    in[1] = (in[1] - viewport[1]) / viewport[3];
    /* Map to range -1 to 1 */
    in[0] = in[0] * 2 - 1;
    in[1] = in[1] * 2 - 1;
    in[2] = in[2] * 2 - 1;
    projectionViewInverseMatrix.multiply(in, out);
    if (out[3] == 0)
      return null;
    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];
    return new Vector(out[0], out[1], out[2]);
  }

  /**
   * Converts {@code vector} displacement from normalized device coordinates (NDC) to screen space.
   * {@link #screenToNDCDisplacement(Vector)} performs the inverse transformation.
   * {@link #ndcToScreenLocation(Vector)} transforms locations instead of vector displacements.
   *
   * @see #screenToNDCDisplacement(Vector)
   * @see #ndcToScreenLocation(Vector)
   */
  public Vector ndcToScreenDisplacement(Vector vector) {
    return new Vector(width() * vector.x() / 2, height() * vector.y() / 2, vector.z() / 2);
  }

  /**
   * Converts {@code vector} displacement from screen space to normalized device coordinates (NDC).
   * {@link #ndcToScreenDisplacement(Vector)} performs the inverse transformation.
   * {@link #screenToNDCLocation(Vector)} transforms locations instead of vector displacements.
   *
   * @see #ndcToScreenDisplacement(Vector)
   * @see #screenToNDCLocation(Vector)
   */
  public Vector screenToNDCDisplacement(Vector vector) {
    return new Vector(2 * vector.x() / (float) width(), 2 * vector.y() / (float) height(), 2 * vector.z());
  }

  /**
   * Same as {@code return displacement(vector, null)}.
   *
   * @see #displacement(Vector, Node)
   * @see #location(Vector, Node)
   */
  public Vector displacement(Vector vector) {
    return this.displacement(vector, null);
  }

  /**
   * Converts {@code vector} displacement given in screen space to the {@code node} coordinate system.
   * The screen space coordinate system is centered at the bounding box of {@link #width()} *
   * {@link #height()} * 1} dimensions. The screen space defines the place where
   * user gestures takes place, e.g., {@link #shift(Node, float, float, float)}.
   * <p>
   * {@link #screenDisplacement(Vector, Node)} performs the inverse transformation.
   * {@link #screenLocation(Vector, Node)} converts pixel locations instead.
   *
   * @see #displacement(Vector, Node)
   * @see #screenLocation(Vector, Node)
   * @see #shift(Node, float, float, float)
   * @see #shift(float, float, float)
   */
  // TODO needs testing
  public Vector displacement(Vector vector, Node node) {
    float dx = vector.x();
    float dy = _leftHanded ? vector.y() : -vector.y();
    // Scale to fit the screen relative vector displacement
    if (_type == Type.PERSPECTIVE) {
      Vector position = node == null ? new Vector() : node.worldPosition();
      float k = Math.abs(_eye.location(position)._vector[2] * (float) Math.tan(fov() / 2.0f));
      dx *= 2.0 * k / ((float) height());
      dy *= 2.0 * k / ((float) height());
    }
    float dz = vector.z();
    dz *= (near() - far()) / (_type == Type.PERSPECTIVE ? (float) Math.tan(fov() / 2.0f) : Math.abs(right() - left()) / (float) width());
    Vector eyeVector = new Vector(dx, dy, dz);
    return node == null ? _eye.worldDisplacement(eyeVector) : node.displacement(eyeVector, _eye);
  }

  /**
   * Same as {@code return screenDisplacement(vector, null)}.
   *
   * @see #screenDisplacement(Vector, Node)
   * @see #screenLocation(Node)
   */
  public Vector screenDisplacement(Vector vector) {
    return screenDisplacement(vector, null);
  }

  /**
   * Converts the {@code node} {@code vector} displacement to screen space.
   * {@link #displacement(Vector, Node)} performs the inverse transformation.
   * {@link #screenLocation(Vector, Node)} converts pixel locations instead.
   *
   * @see #displacement(Vector, Node)
   * @see #screenLocation(Vector, Node)
   */
  // TODO needs testing
  public Vector screenDisplacement(Vector vector, Node node) {
    Vector eyeVector = _eye.displacement(vector, node);
    float dx = eyeVector.x();
    float dy = _leftHanded ? eyeVector.y() : -eyeVector.y();
    if (_type == Type.PERSPECTIVE) {
      Vector position = node == null ? new Vector() : node.worldPosition();
      float k = Math.abs(_eye.location(position)._vector[2] * (float) Math.tan(fov() / 2.0f));
      dx /= 2.0 * k / ((float) height() * _eye.worldMagnitude());
      dy /= 2.0 * k / ((float) height() * _eye.worldMagnitude());
    }
    float dz = eyeVector.z();
    // sign is inverted
    dz /= (near() - far()) / (_type == Type.PERSPECTIVE ? (float) Math.tan(fov() / 2.0f) : Math.abs(right() - left()) / (float) width());
    return new Vector(dx, dy, dz);
  }

  // Gesture screen space interface is quite nice!
  // It always maps screen space geom data respect to the eye

  // 0. Patterns

  /**
   * Same as {@code interact((String)null, gesture)}.
   *
   * @see #interact(String, Object...)
   */
  public void interact(Object... gesture) {
    interact((String)null, gesture);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) interact(node(tag), gesture)}.
   *
   * @see #interact(Node, Object...)
   */
  public void interact(String tag, Object... gesture) {
    if (tag == null || node(tag) != null)
      interact(node(tag), gesture);
  }

  /**
   * Call the {@code node} (or the eye if {@code node} is null) interact
   * gesture parser function set either with {@link Node#setInteraction(Consumer)} or
   * {@link Node#setInteraction(BiConsumer)}.
   *
   * @see Node#setInteraction(BiConsumer)
   * @see Node#setInteraction(Consumer)
   */
  public void interact(Node node, Object... gesture) {
    if (node == null) {
      node = _eye;
    }
    if (node._interact != null) {
      node._interact.accept(node, gesture);
    }
  }

  // 1. Align

  /**
   * Same as {@code align((String)null)}.
   *
   * @see #align(String)
   * @see #align(Node)
   */
  public void align() {
    align((String)null);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) align(node(tag))}.
   *
   * @see #align()
   * @see #align(Node)
   * @see #node(String)
   */
  public void align(String tag) {
    if (tag == null || node(tag) != null)
      align(node(tag));
  }

  /**
   * Aligns the node (use null for the world) with the eye.
   *
   * @see #align()
   * @see #align(String tag)
   */
  public void align(Node node) {
    if (node == null || node == _eye) {
      _eye.align(true);
    }
    else {
      node.align(_eye);
    }
  }

  // 2. Focus

  /**
   * Same as {@code focus((String)null)}.
   *
   * @see #focus(String)
   * @see #focus(Node)
   */
  public void focus() {
    focus((String)null);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) focus(node(tag))}.
   *
   * @see #focus()
   * @see #focus(Node)
   * @see #node(String)
   */
  public void focus(String tag) {
    if (tag == null || node(tag) != null)
      focus(node(tag));
  }

  /**
   * Focuses the node (use null for the world) with the eye.
   * <p>
   * Note that the ball {@link #center} is used as reference point when focusing the eye.
   *
   * @see #focus()
   * @see #focus(String tag)
   */
  public void focus(Node node) {
    if (node == null || node == _eye) {
      _eye.projectOnLine(center, viewDirection());
    }
    else {
      node.projectOnLine(_eye.worldPosition(), _eye.zAxis(false));
    }
  }

  // 3. Scale

  /**
   * Same as {@code zoom(delta, Scene.inertia)}.
   *
   * @see #zoom(float, float)
   */
  public void zoom(float delta) {
    zoom(delta, Scene.inertia);
  }

  /**
   * Same as {@code zoom((String)null, delta, inertia)}.
   *
   * @see #zoom(String, float, float)
   */
  public void zoom(float delta, float inertia) {
    zoom((String)null, delta, inertia);
  }

  /**
   * Same as {@code zoom(tag, delta, Scene.inertia)}.
   *
   * @see #zoom(String, float, float)
   */
  public void zoom(String tag, float delta) {
    zoom(tag, delta, Scene.inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) zoom(node(tag), delta, inertia)}.
   *
   * @see #zoom(Node, float, float)
   */
  public void zoom(String tag, float delta, float inertia) {
    if (tag == null || node(tag) != null)
      zoom(node(tag), delta, inertia);
  }

  /**
   * Same as {@code zoom(node, delta, Scene.inertia)}.
   *
   * @see #zoom(Node, float, float)
   */
  public void zoom(Node node, float delta) {
    zoom(node, delta, Scene.inertia);
  }

  /**
   * Scales the {@code node} (use null for the world) according to {@code delta} and
   * {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   */
  public void zoom(Node node, float delta, float inertia) {
    if (node == null || node == _eye) {
      // we negate z which targets the Processing mouse wheel
      shift(_eye, 0, 0, delta / (near() - far()), inertia);
    }
    else {
      float factor = 1 + Math.abs(delta) / (float) height();
      node.scale(delta >= 0 ? factor : 1 / factor, inertia);
    }
  }

  // 4. Translate

  /**
   * Same as {@code shift(Scene.inertia)}.
   *
   * @see #shift(float)
   */
  public void shift() {
    shift(Scene.inertia);
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
   * Same as {@code shift(tag, Scene.inertia)}.
   *
   * @see #shift(String, float)
   */
  public void shift(String tag) {
    shift(tag, Scene.inertia);
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
    shift(node, Scene.inertia);
  }

  /**
   * Same as {@code this.shift(node, mouseDX() * (1 - lag), mouseDY() * (1 - lag), 0, lag)}.
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
    this.shift(node, mouseDX() * (1 - l), mouseDY() * (1 - l), 0, l);
  }

  /**
   * Same as {@code shift(dx, dy, dz, Scene.inertia)}.
   *
   * @see #shift(float, float, float, float)
   */
  public void shift(float dx, float dy, float dz) {
    shift(dx, dy, dz, Scene.inertia);
  }

  /**
   * Same as {@code shift((String)null, dx, dy, dz, inertia)}.
   *
   * @see #shift(String, float, float, float, float)
   */
  public void shift(float dx, float dy, float dz, float inertia) {
    shift((String)null, dx, dy, dz, inertia);
  }

  /**
   * Same as {@code shift(tag, dx, dy, dz, Scene.inertia)}.
   *
   * @see #shift(String, float, float, float, float)
   */
  public void shift(String tag, float dx, float dy, float dz) {
    shift(tag, dx, dy, dz, Scene.inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) shift(node(tag), dx, dy, dz, inertia)}.
   *
   * @see #shift(Node, float, float, float, float)
   */
  public void shift(String tag, float dx, float dy, float dz, float inertia) {
    if (tag == null || node(tag) != null)
      shift(node(tag), dx, dy, dz, inertia);
  }

  /**
   * Same as {@code shift(node, dx, dy, dz, Scene.inertia)}.
   *
   * @see #shift(Node, float, float, float, float)
   */
  public void shift(Node node, float dx, float dy, float dz) {
    shift(node, dx, dy, dz, Scene.inertia);
  }

  /**
   * Translates the {@code node} (use null for the world) according to {@code (dx, dy, dz)}
   * defined in screen-space ((a box of {@link #width()} * {@link #height()} * 1 dimensions),
   * and {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   * <p>
   * Note that the ball {@link #center} is used as reference point when shifting the eye.
   */
  public void shift(Node node, float dx, float dy, float dz, float inertia) {
    if (node == null || node == _eye) {
      node = new Node(null, _eye.worldPosition(), _eye.worldOrientation(), _eye.worldMagnitude(), false);
      node.setWorldPosition(center);
      Vector vector = displacement(new Vector(dx, dy, dz), node);
      vector.multiply(-1);
      Vector translation = _eye.referenceDisplacement(vector);
      _eye.translate(translation.x(), translation.y(), translation.z(), inertia);
    }
    else {
      node.translate(node.referenceDisplacement(displacement(new Vector(dx, dy, dz), node)), inertia);
    }
  }

  // 5. Rotate

  /**
   * Same as {@code turn(roll, pitch, yaw, Scene.inertia)}.
   *
   * @see #turn(float, float, float, float)
   */
  public void turn(float roll, float pitch, float yaw) {
    turn(roll, pitch, yaw, Scene.inertia);
  }

  /**
   * Same as {@code turn((String)null, roll, pitch, yaw, inertia)}.
   *
   * @see #turn(String, float, float, float, float)
   */
  public void turn(float roll, float pitch, float yaw, float inertia) {
    turn((String)null, roll, pitch, yaw, inertia);
  }

  /**
   * Same as {@code turn(tag, roll, pitch, yaw, Scene.inertia)}.
   *
   * @see #turn(String, float, float, float, float)
   */
  public void turn(String tag, float roll, float pitch, float yaw) {
    turn(tag, roll, pitch, yaw, Scene.inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) turn(node(tag), roll, pitch, yaw, inertia)}.
   *
   * @see #turn(Node, float, float, float, float)
   */
  public void turn(String tag, float roll, float pitch, float yaw, float inertia) {
    if (tag == null || node(tag) != null)
      turn(node(tag), roll, pitch, yaw, inertia);
  }

  /**
   * Same as {@code turn(node, roll, pitch, yaw, Scene.inertia)}.
   *
   * @see #turn(Node, float, float, float, float)
   */
  public void turn(Node node, float roll, float pitch, float yaw) {
    turn(node, roll, pitch, yaw, Scene.inertia);
  }

  /**
   * Rotates the {@code node} (use null for the world) around the x-y-z screen axes according to
   * {@code roll}, {@code pitch} and {@code yaw} radians, resp., and according to {@code inertia}
   * which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   * <p>
   * Note that the ball {@link #center} is used as reference point when turning the eye.
   */
  public void turn(Node node, float roll, float pitch, float yaw, float inertia) {
    if (node == null || node == _eye) {
      _eye._orbit(new Quaternion(_leftHanded ? -roll : roll, pitch, _leftHanded ? -yaw : yaw), center, inertia);
      // same as:
      //Quaternion q = new Quaternion(leftHanded ? -roll : roll, pitch, leftHanded ? -yaw : yaw);
      //_eye.orbit(_eye.worldDisplacement(q.axis()), q.angle(), _center, inertia);
      // whereas the following doesn't work
      /*
      Quaternion q = new Quaternion(leftHanded ? -roll : roll, pitch, leftHanded ? -yaw : yaw);
      q = _eye.worldDisplacement(q);
      _eye.orbit(q.axis(), q.angle(), _center, inertia);
      // */
    }
    else {
      Quaternion quaternion = new Quaternion(_leftHanded ? roll : -roll, -pitch, _leftHanded ? yaw : -yaw);
      // TODO test node quaternion displacement here
      node.rotate(new Quaternion(node.displacement(quaternion.axis(), _eye), quaternion.angle()), inertia);
    }
  }

  // 6. Spin

  /**
   * Same as {@code spin((String)null)}.
   *
   * @see #spin(String)
   */
  public void spin() {
    spin((String)null);
  }

  /**
   * Same as {@code spin(tag, Scene.inertia)}.
   *
   * @see #spin(String, float)
   */
  public void spin(String tag) {
    spin(tag, Scene.inertia);
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
   * Same as {@code spin(node, Scene.inertia)}.
   *
   * @see #spin(Node, float)
   */
  public void spin(Node node) {
    spin(node, Scene.inertia);
  }

  /**
   * Same as {@code this.spinNode(node, pmouseX(), pmouseY(), mouseX(), mouseY(), inertia)}.
   *
   * @see #spin(Node, int, int, int, int, float)
   */
  public void spin(Node node, float inertia) {
    if (inertia == 1) {
      // Sensitivity is expressed in pixels per milliseconds. Default value is 30 (300 pixels per second).
      float sensitivity = 30;
      this.spin(node, pmouseX(), pmouseY(), mouseX(), mouseY(), mouseSpeed() > sensitivity ? 1 : 0.9f);
    } else
      this.spin(node, pmouseX(), pmouseY(), mouseX(), mouseY(), inertia);
  }

  /**
   * Same as {@code spin(pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia)}.
   *
   * @see #spin(int, int, int, int, float)
   */
  public void spin(int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia);
  }

  /**
   * Same as {@code spin((String)null, pixel1X, pixel1Y, pixel2X, pixel2Y, inertia)}.
   *
   * @see #spin(String, int, int, int, int, float)
   */
  public void spin(int pixel1X, int pixel1Y, int pixel2X, int pixel2Y, float inertia) {
    spin((String)null, pixel1X, pixel1Y, pixel2X, pixel2Y, inertia);
  }

  /**
   * Same as {@code spin(tag, pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia)}.
   *
   * @see #spin(String, int, int, int, int, float)
   */
  public void spin(String tag, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(tag, pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia);
  }

  /**
   * Same as {@code if (tag == null || node(tag) != null) spin(node(tag), pixel1X, pixel1Y, pixel2X, pixel2Y, inertia)}.
   *
   * @see #spin(Node, int, int, int, int, float)
   */
  public void spin(String tag, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y, float inertia) {
    if (tag == null || node(tag) != null)
      spin(node(tag), pixel1X, pixel1Y, pixel2X, pixel2Y, inertia);
  }

  /**
   * Same as {@code spin(node, pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia)}.
   *
   * @see #spin(Node, int, int, int, int, float)
   */
  public void spin(Node node, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(node, pixel1X, pixel1Y, pixel2X, pixel2Y, Scene.inertia);
  }

  /**
   * Rotates the {@code node} (use null for the world) using an arcball interface, from points
   * {@code (pixel1X, pixel1Y)} to {@code (pixel2X, pixel2Y)} pixel positions. The {@code inertia}
   * controls the gesture strength and it should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   * The center of the rotation is the screen projected node origin (see {@link Node#worldPosition()}).
   * <p>
   * For implementation details refer to Shoemake 92 paper: Arcball: a user interface for specifying
   * three-dimensional orientation using a mouse.
   * <p>
   * Note that the ball {@link #center} is used as pivot when spinning the eye.
   */
  public void spin(Node node, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y, float inertia) {
    if (node == null || node == _eye) {
      float sensitivity = 1;
      Vector center = screenLocation(this.center);
      if (center == null)
        return;
      int centerX = (int) center.x();
      int centerY = (int) center.y();
      float px = sensitivity * (pixel1X - centerX) / (float) width();
      float py = sensitivity * (_leftHanded ? (pixel1Y - centerY) : (centerY - pixel1Y)) / (float) height();
      float dx = sensitivity * (pixel2X - centerX) / (float) width();
      float dy = sensitivity * (_leftHanded ? (pixel2Y - centerY) : (centerY - pixel2Y)) / (float) height();
      Vector p1 = new Vector(px, py, _projectOnBall(px, py));
      Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
      // Approximation of rotation angle should be divided by the projectOnBall size, but it is 1.0
      Vector axis = p2.cross(p1);
      // 2D is an ad-hoc
      float angle = 2.0f * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / (p1.squaredNorm() * p2.squaredNorm())));
      _eye._orbit(new Quaternion(axis, angle), this.center, inertia);
      // same as:
      //_eye.orbit(_eye.worldDisplacement(axis), angle, _center, inertia);
    }
    else {
      float sensitivity = 1;
      Vector center = screenLocation(node.worldPosition());
      if (center == null)
        return;
      int centerX = (int) center.x();
      int centerY = (int) center.y();
      float px = sensitivity * (pixel1X - centerX) / (float) width();
      float py = sensitivity * (_leftHanded ? (pixel1Y - centerY) : (centerY - pixel1Y)) / (float) height();
      float dx = sensitivity * (pixel2X - centerX) / (float) width();
      float dy = sensitivity * (_leftHanded ? (pixel2Y - centerY) : (centerY - pixel2Y)) / (float) height();
      Vector p1 = new Vector(px, py, _projectOnBall(px, py));
      Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
      // Approximation of rotation angle should be divided by the projectOnBall size, but it is 1.0
      Vector axis = p2.cross(p1);
      // 2D is an ad-hoc
      float angle = sensitivity * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / (p1.squaredNorm() * p2.squaredNorm())));
      Quaternion quaternion = new Quaternion(axis, -angle);
      node.rotate(new Quaternion(node.displacement(quaternion.axis(), _eye), quaternion.angle()), inertia);
    }
  }

  /**
   * Returns "pseudo-_distance" from (x,y) to ball of radius size. For a point inside the
   * ball, it is proportional to the euclidean distance to the ball. For a point outside
   * the ball, it is proportional to the inverse of this distance (tends to zero) on the
   * ball, the function is continuous.
   */
  protected float _projectOnBall(float x, float y) {
    // If you change the size value, change angle computation in deformedBallQuaternion().
    float size = 1.0f;
    float size2 = size * size;
    float size_limit = size2 * 0.5f;
    float d = x * x + y * y;
    return d < size_limit ? (float) Math.sqrt(size2 - d) : size_limit / (float) Math.sqrt(d);
  }

  // DRAWING

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
   * Same as {@code vertex(context(), x, y, z)}.
   *
   * @see #vertex(PGraphics, float, float, float)
   */
  public void vertex(float x, float y, float z) {
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
   * Same as {@code vertex(context(), x, y, z, u, v)}.
   *
   * @see #vertex(PGraphics, float, float, float, float)
   * @see #vertex(PGraphics, float, float, float, float, float)
   */
  public void vertex(float x, float y, float z, float u, float v) {
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
   * Same as {@code line(context(), x1, y1, z1, x2, y2, z2)}.
   *
   * @see #line(PGraphics, float, float, float, float, float, float)
   */
  public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
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

  protected void _drawSpline(Node interpolator) {
    if (interpolator.hint() != 0) {
      List<Node> path = interpolator._interpolator._path();
      if (interpolator._splineWeight > 0 && path.size() > 1) {
        context().pushStyle();
        context().noFill();
        context().colorMode(PApplet.RGB, 255);
        context().strokeWeight(interpolator._splineWeight);
        context().stroke(interpolator._splineStroke);
        context().beginShape();
        for (Node node : path) {
          Vector position = node.worldPosition();
          vertex(position.x(), position.y(), position.z());
        }
        context().endShape();
        context().popStyle();
      }
      if (interpolator._steps > 0) {
        context().pushStyle();
        int count = 0;
        float goal = 0.0f;
        for (Node node : path) {
          if (count >= goal) {
            goal += Node.maxSteps / ((float) interpolator._steps + 1);
            if (count % Node.maxSteps != 0) {
              context().pushMatrix();
              _applyTransformation(context(), node);
              _displayAnimationHint(node);
              context().popMatrix();
            }
          }
          count++;
        }
        context().popStyle();
      }
    }
  }

  protected static boolean _isHintEnabled(int mask, int hint) {
    return ~(mask | ~hint) == 0;
  }

  protected void _displayAnimationHint(Node node) {
    PGraphics pg = context();
    if (Scene._isHintEnabled(node._keyframesMask, Node.SHAPE)) {
      pg.pushStyle();
      if (node._rmrShape != null) {
        pg.shapeMode(pg.shapeMode);
        pg.shape(node._rmrShape);
      }
      if (node._imrShape != null) {
        node._imrShape.accept(pg);
      }
      pg.popStyle();
    }
    if (Scene._isHintEnabled(node._keyframesMask, Node.TORUS)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.fill(node._torusColor);
      drawTorusSolenoid(pg, node._torusFaces, 5);
      pg.popStyle();
    }
    if (Scene._isHintEnabled(node._keyframesMask, Node.AXES)) {
      pg.pushStyle();
      drawAxes(pg, node._axesLength == 0 ? radius / 5 : node._axesLength);
      pg.popStyle();
    }
    if (Scene._isHintEnabled(node._keyframesMask, Node.CAMERA)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(node._cameraStroke);
      pg.fill(node._cameraStroke);
      _drawEye(pg, node._cameraLength == 0 ? radius : node._cameraLength);
      pg.popStyle();
    }
    if (Scene._isHintEnabled(node._keyframesMask, Node.BULLSEYE)) {
      pg.pushStyle();
      pg.colorMode(PApplet.RGB, 255);
      pg.stroke(node._bullsEyeStroke);
      _drawBullsEye(node);
      pg.popStyle();
    }
  }

  /**
   * Internal use.
   */
  protected void _drawEye(PGraphics pg, float scale) {
    pg.pushStyle();
    float halfHeight = scale * 0.07f;
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

    if (_leftHanded) {
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
    if (_leftHanded) {
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
   * Draws an arrow of {@link #radius} and {@code length} along the positive Z axis.
   * <p>
   * Use {@link #drawArrow(Vector, Vector, float)} to place the arrow in 3D.
   */
  public void drawArrow(float length, float radius) {
    float head = 2.5f * (radius / length) + 0.1f;
    float coneRadiusCoef = 4.0f - 5.0f * head;

    drawCylinder(radius, length * (1.0f - head / coneRadiusCoef));

    context().translate(0.0f, 0.0f, length * (1.0f - head));
    drawCone(coneRadiusCoef * radius, head * length);
    context().translate(0.0f, 0.0f, -length * (1.0f - head));
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
   * Draws an arrow of {@link #radius} between {@code from} and the 3D point {@code to}.
   *
   * @see #drawArrow(float, float)
   */
  public void drawArrow(Vector from, Vector to, float radius) {
    context().pushMatrix();
    context().translate(from.x(), from.y(), from.z());
    context().applyMatrix(Scene.toPMatrix(new Quaternion(new Vector(0, 0, 1), Vector.subtract(to, from)).matrix()));
    drawArrow(Vector.subtract(to, from).magnitude(), radius);
    context().popMatrix();
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
    drawCylinder(pGraphics, radius / 6, radius / 3);
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
   * Draws a cylinder of {@link #radius} and {@code height} onto {@code pGraphics}.
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
    float radius = this.radius / 4;
    drawCone(pGraphics, 12, 0, 0, radius, (float) Math.sqrt((float) 3) * radius);
  }

  /**
   * Draws a cone onto {@code pGraphics} centered at {@code (x,y)} having
   * {@link #radius} and {@code height} dimensions.
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
    drawAxes(radius);
  }

  /**
   * Same as {@code drawAxes(context(), length)}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public void drawAxes(float length) {
    drawAxes(context(), length);
  }

  /**
   * Same as {@code drawAxes(pGraphics, radius)}.
   *
   * @see #drawAxes(PGraphics, float)
   */
  public void drawAxes(PGraphics pGraphics) {
    drawAxes(pGraphics, radius);
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
    // The X
    pGraphics.stroke(200, 0, 0);
    pGraphics.vertex(charShift, charWidth, -charHeight);
    pGraphics.vertex(charShift, -charWidth, charHeight);
    pGraphics.vertex(charShift, -charWidth, -charHeight);
    pGraphics.vertex(charShift, charWidth, charHeight);
    // The Y
    pGraphics.stroke(0, 200, 0);
    pGraphics.vertex(charWidth, charShift, (_leftHanded ? charHeight : -charHeight));
    pGraphics.vertex(0.0f, charShift, 0.0f);
    pGraphics.vertex(-charWidth, charShift, (_leftHanded ? charHeight : -charHeight));
    pGraphics.vertex(0.0f, charShift, 0.0f);
    pGraphics.vertex(0.0f, charShift, 0.0f);
    pGraphics.vertex(0.0f, charShift, -(_leftHanded ? charHeight : -charHeight));
    // The Z
    pGraphics.stroke(0, 100, 200);
    pGraphics.vertex(-charWidth, !_leftHanded ? charHeight : -charHeight, charShift);
    pGraphics.vertex(charWidth, !_leftHanded ? charHeight : -charHeight, charShift);
    pGraphics.vertex(charWidth, !_leftHanded ? charHeight : -charHeight, charShift);
    pGraphics.vertex(-charWidth, !_leftHanded ? -charHeight : charHeight, charShift);
    pGraphics.vertex(-charWidth, !_leftHanded ? -charHeight : charHeight, charShift);
    pGraphics.vertex(charWidth, !_leftHanded ? -charHeight : charHeight, charShift);
    pGraphics.endShape();
    pGraphics.popStyle();
    // X Axis
    pGraphics.stroke(200, 0, 0);
    pGraphics.line(0, 0, 0, length, 0, 0);
    // Y Axis
    pGraphics.stroke(0, 200, 0);
    pGraphics.line(0, 0, 0, 0, length, 0);
    // Z Axis
    pGraphics.stroke(0, 100, 200);
    pGraphics.line(0, 0, 0, 0, 0, length);
    pGraphics.popStyle();
  }

  /**
   * Convenience function that simply calls {@code drawGrid(radius(), 10)}
   *
   * @see #drawGrid(float, int)
   */
  public void drawGrid() {
    drawGrid(radius, 10);
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
    drawGrid(radius, subdivisions);
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
    drawGrid(pGraphics, radius, 10);
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
    drawDottedGrid(radius, 10);
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
    drawDottedGrid(radius, subdivisions);
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
    drawDottedGrid(pGraphics, radius, 10);
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
   * Applies the {@code scene.eye()} transformation and then calls
   * {@link #drawFrustum(PGraphics, Scene)} on the scene {@link #context()}.
   *
   * @see #drawFrustum(PGraphics, Scene)
   */
  public void drawFrustum(Scene scene) {
    context().pushMatrix();
    _applyTransformation(context(), scene._eye);
    drawFrustum(context(), scene);
    context().popMatrix();
  }

  /**
   * Draws a representation of the viewing frustum onto {@code pGraphics} according to
   * the eye and type.
   * <p>
   * Note that if {@code pGraphics == scene.context()} this method has not effect at all.
   *
   * @see #drawFrustum(Scene)
   */
  public static void drawFrustum(PGraphics pGraphics, Scene scene) {
    if (pGraphics == scene.context())
      return;
    scene._type = scene.context().projection.m33 == 0 ? Type.PERSPECTIVE : Type.ORTHOGRAPHIC;
    // texturing requires scene.isOffscreen() (third condition) otherwise got
    // "The pixels array is null" message and the frustum near plane texture and contour are missed
    boolean texture = pGraphics instanceof PGraphicsOpenGL && scene.isOffscreen();
    switch (scene._type) {
      case ORTHOGRAPHIC:
        _drawOrthographicFrustum(pGraphics, texture ? scene.context() : null, scene.left(), scene.right(), scene.bottom(), scene.top(), scene.near(), scene.far());
        break;
      case PERSPECTIVE:
        _drawPerspectiveFrustum(pGraphics, texture ? scene.context() : null, (float) Math.tan(scene.fov() / 2.0f), scene._leftHanded ? -scene.aspectRatio() : scene.aspectRatio(), scene.near(), scene.far());
        break;
    }
  }

  // TODO make it work for non-symmetrical ortho volumes.
  protected static void _drawOrthographicFrustum(PGraphics pGraphics, PGraphics eyeBuffer, float left, float right, float bottom, float top, float zNear, float zFar) {
    if (pGraphics == eyeBuffer)
      return;
    boolean leftHanded = bottom < top;
    pGraphics.pushStyle();
    // 0 is the upper left coordinates of the near corner, 1 for the far one
    // Frustum lines
    pGraphics.beginShape(PApplet.LINES);
    Scene.vertex(pGraphics, right, top, -zNear);
    Scene.vertex(pGraphics, right, top, -zFar);
    Scene.vertex(pGraphics, left, top, -zNear);
    Scene.vertex(pGraphics, left, top, -zFar);
    Scene.vertex(pGraphics, left, bottom, -zNear);
    Scene.vertex(pGraphics, left, bottom, -zFar);
    Scene.vertex(pGraphics, right, bottom, -zNear);
    Scene.vertex(pGraphics, right, bottom, -zFar);
    pGraphics.endShape();
    // Up arrow
    float arrowHeight = 1.5f * Math.abs(top - bottom) / 2;
    float baseHeight = 1.2f * Math.abs(top - bottom) / 2;
    float arrowHalfWidth = 0.5f * Math.abs(right - left) / 2;
    float baseHalfWidth = 0.3f * Math.abs(right - left) / 2;
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
    Scene.vertex(pGraphics, -baseHalfWidth, -top, -zNear);
    Scene.vertex(pGraphics, baseHalfWidth, -top, -zNear);
    Scene.vertex(pGraphics, baseHalfWidth, -baseHeight, -zNear);
    Scene.vertex(pGraphics, -baseHalfWidth, -baseHeight, -zNear);
    pGraphics.endShape();
    // Arrow
    pGraphics.beginShape(PApplet.TRIANGLES);
    if (leftHanded) {
      Scene.vertex(pGraphics, 0.0f, -arrowHeight, -zNear);
      Scene.vertex(pGraphics, -arrowHalfWidth, -baseHeight, -zNear);
      Scene.vertex(pGraphics, arrowHalfWidth, -baseHeight, -zNear);
    } else {
      Scene.vertex(pGraphics, 0.0f, arrowHeight, -zNear);
      Scene.vertex(pGraphics, -arrowHalfWidth, baseHeight, -zNear);
      Scene.vertex(pGraphics, arrowHalfWidth, baseHeight, -zNear);
    }
    if (eyeBuffer != null)
      pGraphics.popStyle();// begin at arrow base
    pGraphics.endShape();
    // Planes
    if (leftHanded) {
      // far
      _drawPlane(pGraphics, null, new Vector(right, top, zFar), new Vector(0, 0, -1), true);
      // near
      _drawPlane(pGraphics, eyeBuffer, new Vector(right, top, zNear), new Vector(0, 0, 1), true);
    }
    else {
      // near
      _drawPlane(pGraphics, eyeBuffer, new Vector(right, -top, zFar), new Vector(0, 0, -1), false);
      // far
      _drawPlane(pGraphics, null, new Vector(right, -top, zNear), new Vector(0, 0, 1), false);
    }
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

    points[0].setZ(zNear);
    points[1].setZ(zFar);
    //(2 * (float) Math.atan(_eye.magnitude()))
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
   * @see #drawProjectors(Scene, List)
   */
  public void drawProjector(Scene eye, Vector point) {
    drawProjectors(eye, Arrays.asList(point));
  }

  /**
   * Draws the projection of each point in {@code points} in the near plane onto {@code pGraphics}.
   * <p>
   * This method should be used in conjunction with {@link #drawFrustum(PGraphics, Scene)}.
   * <p>
   * Note that if {@code scene == this} this method has not effect at all.
   *
   * @see #drawProjector(Scene, Vector)
   */
  // TODO needs testing
  public void drawProjectors(Scene scene, List<Vector> points) {
    if (scene == this) {
      System.out.println("Warning: No drawProjectors done!");
      return;
    }
    context().pushStyle();
    // if ORTHOGRAPHIC: do it in the eye coordinate system
    // if PERSPECTIVE: do it in the world coordinate system
    Vector o = new Vector();
    if (_type == Scene.Type.ORTHOGRAPHIC) {
      context().pushMatrix();
      _applyTransformation(context(), scene._eye);
    }
    // in PERSPECTIVE cache the transformed origin
    else
      o = scene._eye.worldLocation(new Vector());
    context().beginShape(PApplet.LINES);
    for (Vector s : points) {
      if (_type == Scene.Type.ORTHOGRAPHIC) {
        Vector v = scene._eye.location(s);
        Scene.vertex(context(), v.x(), v.y(), v.z());
        // Key here is to represent the eye zNear param (which is given in world units)
        // in eye units.
        // Hence it should be multiplied by: 1 / eye.magnitude()
        // The neg sign is because the zNear is positive but the eye view direction is
        // the negative Z-axis
        Scene.vertex(context(), v.x(), v.y(), -(scene.near() * 1 / Math.abs(scene.right() - scene.left()) / (float) scene.width()));
      } else {
        Scene.vertex(context(), s.x(), s.y(), s.z());
        Scene.vertex(context(), o.x(), o.y(), o.z());
      }
    }
    context().endShape();
    if (_type == Scene.Type.ORTHOGRAPHIC) {
      context().popMatrix();
    }
    context().popStyle();
  }

  /**
   * {@link #drawCross(float, float, float)} centered at the projected node origin.
   * If node is a Node instance the length of the cross is the node
   * {@link Node#bullsEyeSize()}, otherwise it's {@link #radius} / 5.
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
    if (_eye == node) {
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
    drawCross(x, y, radius / 5);
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
    if (node._bullsEyeShape == Node.BullsEyeShape.SQUARE)
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
    if (_eye == node) {
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
    drawSquaredBullsEye(x, y, radius / 5);
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
    if (_eye == node) {
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
    drawCircledBullsEye(x, y, radius / 5);
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
    drawTorusSolenoid(faces, 0.07f * radius);
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
}
