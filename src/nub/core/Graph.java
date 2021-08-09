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
import processing.core.PShape;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A 2D or 3D scene-graph providing eye, input and timing handling to a raster or ray-tracing
 * renderer.
 * <h1>1. Types and dimensions</h1>
 * To set the viewing volume use {@link #setBounds(Vector, float)} or {@link #setBounds(float, float)}.
 * <p>
 * The way the projection matrix is computed (see
 * {@link #projection()}), defines the type of the
 * graph as: {@link Type#PERSPECTIVE}, {@link Type#ORTHOGRAPHIC} for 3d graphs and {@link Type#TWO_D}
 * for a 2d graph.
 * <h1>2. Scene-graph handling</h1>
 * A graph forms a tree of {@link Node}s whose visual representations may be
 * {@link #render()}. To render a subtree call {@link #render(Node)}.
 * Note that rendering routines should be called within your main-event loop.
 * <p>
 * The node collection belonging to the graph may be retrieved with {@link #nodes()}.
 * The graph provides other useful routines to handle the hierarchy, such as
 * {@link #prune(Node)}, {@link Node#isReachable()}, {@link #branch(Node)}, and {@link #clear()}.
 * <h2>The eye</h2>
 * Any {@link Node} (belonging or not to the graph hierarchy) may be set as the {@link #eye()}
 * (see {@link #setEye(Node)}). Several functions handle the eye, such as
 * {@link #lookAt(Vector)}, {@link #at()}, {@link #setViewDirection(Vector)},
 * {@link #setUpVector(Vector)}, {@link #upVector()}, {@link #fitFOV()},
 * {@link #fov()}, {@link #fit()}, {@link #lookAround(float, float)},
 * {@link #cad(float, float)}, and {@link #moveForward(float)}.
 * <h2>2.1. Transformations</h2>
 * The graph acts as interface between screen space (a box of {@link #width()} * {@link #height()} * 1
 * dimensions), from where user gesture data is gathered, and the {@code nodes}. To transform points
 * from/to screen space to/from node space use {@link #location(Vector, Node)} and
 * {@link #screenLocation(Vector, Node)}. To transform vectors from/to screen space to/from node space
 * use {@link #displacement(Vector, Node)} and {@link #screenDisplacement(Vector, Node)}.
 * <h1>3. Picking and interaction</h1>
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
 * <li>Set {@code Graph.inertia} in  [0..1] (0 no inertia & 1 no friction) to change the default inertia
 * value globally, instead of setting it on a per method call basis. Note that it is initially set to 0.8.</li>
 * <li>Customize node behaviors by overridden {@link Node#interact(Object...)}
 * and then invoke them by either calling: {@link #interact(Object...)},
 * {@link #interact(String, Object...)} or {@link #interact(Node, Object...)}.
 * </li>
 * </ol>
 * <h1>4. Timing handling</h1>
 * The graph performs timing handling through a {@link #TimingHandler}. Several
 * {@link TimingHandler} wrapper functions, such as {@link TimingHandler#registerTask(Task)}
 * are provided for convenience.
 * <p>
 * A default {@link #interpolator()} may perform several {@link #eye()} interpolations
 * such as {@link #fit(float)}, {@link #fit(int, int, int, int)}, {@link #fit(Node)} and {@link #fit(Node, float)}.
 * Refer to the {@link Interpolator} documentation for details.
 * <h1>5. Visual hints</h2>
 * The world space visual representation may be configured using the following hints:
 * {@link #AXES}, {@link #HUD}, {@link #GRID}, {@link #BACKGROUND} and {@link #SHAPE}.
 * <p>
 * See {@link #hint()}, {@link #configHint(int, Object...)} {@link #enableHint(int)},
 * {@link #enableHint(int, Object...)}, {@link #disableHint(int)}, {@link #toggleHint(int)}
 * and {@link #resetHint()}.
 * <h1>6. Visibility and culling techniques</h1>
 * Geometry may be culled against the viewing volume by calling {@link #isPointVisible(Vector)},
 * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}.
 * <h1>7. Matrix handling</h1>
 * The graph performs matrix handling through a matrix-handler. Refer to the {@link MatrixHandler}
 * documentation for details.
 * <p>
 * To define your geometry on the screen coordinate system (such as when drawing 2d controls
 * on top of a 3d graph) issue your drawing code between {@link #beginHUD()} and
 * {@link #endHUD()}. These methods are {@link MatrixHandler} wrapper functions
 * with the same signatures provided for convenience.
 *
 * @see TimingHandler
 * @see MatrixHandler
 */
public class Graph {
  protected static Graph _onscreenGraph;
  public static Random random = new Random();
  // Visual hints
  protected int _mask;
  public final static int GRID = 1 << 0;
  public final static int AXES = 1 << 1;
  public final static int HUD = 1 << 2;
  public final static int SHAPE = 1 << 3;
  public final static int BACKGROUND = 1 << 4;
  protected Consumer<processing.core.PGraphics> _imrHUD;
  protected processing.core.PShape _rmrHUD;
  protected Consumer<processing.core.PGraphics> _imrShape;
  protected processing.core.PShape _rmrShape;
  public enum GridType {
    LINES, DOTS
  }
  protected GridType _gridType;
  protected int _gridStroke;
  protected int _centerStroke;
  protected int _gridSubDiv;
  protected Object _background;
  protected static HashSet<Interpolator> _interpolators = new HashSet<Interpolator>();
  protected static HashSet<Node> _hudSet = new HashSet<Node>();

  // Custom render
  protected HashMap<Integer, BiConsumer<Graph, Node>> _functors;

  // offscreen
  protected int _upperLeftCornerX, _upperLeftCornerY;
  protected long _lastDisplayed;
  protected boolean _offscreen;

  // 0. Contexts
  protected Object _bb, _fb;
  // 1. Eye
  protected Node _eye;
  protected long _lastEqUpdate;
  protected Vector _center;
  protected float _radius;
  protected boolean _fixed;
  protected float _zNear, _zFar;
  // Inertial stuff
  public static float inertia = 0.8f;
  protected InertialTask _translationTask;
  protected InertialTask _lookAroundTask;
  protected InertialTask _cadRotateTask;
  protected Vector _eyeUp;

  //Interpolator
  protected Interpolator _interpolator;
  //bounds eqns
  protected float[][] _coefficients;
  protected Vector[] _normal;
  protected float[] _distance;
  // handed
  public static boolean leftHanded;

  // 2. Matrix handler
  protected int _renderCount;
  protected int _width, _height;
  protected MatrixHandler _matrixHandler, _bbMatrixHandler;
  // _bb : picking buffer
  public boolean picking;
  protected long _bbNeed, _bbCount;
  protected Matrix _projection, _view, _projectionView, _projectionViewInverse;
  protected long _cacheProjectionViewInverse;

  // TODO these three are not only related to Quaternion.from but mainly to hint stuff

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

  public static TimingHandler TimingHandler;
  public static boolean _seeded;
  protected boolean _seededGraph;
  protected HashMap<String, Node> _tags;
  protected ArrayList<Ray> _rays;

  // 4. Graph
  protected static List<Node> _seeds = new ArrayList<Node>();
  protected long _lastNonEyeUpdate = 0;

  // 5. Interaction methods
  Vector _upVector;
  protected long _lookAroundCount;

  // 6. subtree rendering
  // this variable is only needed to keep track of the subtree
  // that's to be rendered in the back buffer
  protected Node _subtree;

  // 7. Visibility

  /**
   * Enumerates the different visibility states an object may have respect to the bounding volume.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  // 8. Projection stuff

  protected Type _type;

  /**
   * Enumerates the graph types.
   * <p>
   * The type mainly defines the way the projection matrix is computed.
   */
  public enum Type {
    PERSPECTIVE, ORTHOGRAPHIC, TWO_D, CUSTOM
  }

  private float _zNearCoefficient;
  private float _zClippingCoefficient;

  /**
   * Same as {@code this(context, width, height, eye, Type.PERSPECTIVE)}.
   *
   * @see #Graph(Object, int, int, Node, Type)
   */
  public Graph(Object context, int width, int height, Node eye) {
    this(context, width, height, eye, Type.PERSPECTIVE);
  }

  /**
   * Same as {@code this(context, width, height, eye, type, new Vector(), 100)}.
   *
   * @see #Graph(Object, int, int, Node, Type, Vector, float)
   */
  protected Graph(Object context, int width, int height, Node eye, Type type) {
    this(context, width, height, eye, type, new Vector(), 100);
  }

  /**
   * Same as {@code this(context, width, height, Type.PERSPECTIVE)}.
   *
   * @see #Graph(Object, int, int, Type)
   */
  public Graph(Object context, int width, int height) {
    this(context, width, height, Type.PERSPECTIVE);
  }

  /**
   * Same as {@code this(context, width, height, type, new Vector(), 100)}.
   *
   * @see #Graph(Object, int, int, Type, Vector, float)
   */
  protected Graph(Object context, int width, int height, Type type) {
    this(context, width, height, type, new Vector(), 100);
  }

  /**
   * Defines a right-handed graph with the specified {@code width} and {@code height}
   * screen window dimensions. Creates and {@link #eye()} node, sets its {@link #fov()} to
   * {@code PI/3}. Calls {@link #setBounds(float, float)} on {@code zNear} and
   * {@code zFar} to set up the scene frustum.
   * <p>
   * The constructor also instantiates the graph main {@link #context()} and
   * {@code back-buffer} matrix-handlers (see {@link MatrixHandler}) and
   * {@link #TimingHandler}.
   *
   * @see #setBounds(float, float)
   * @see #Graph(Object, int, int, Type, Vector, float)
   * @see #TimingHandler
   * @see MatrixHandler
   */
  protected Graph(Object context, int width, int height, Type type, float zNear, float zFar) {
    _init(context, width, height, new Node(), type);
    if (is3D())
      setFOV((float) Math.PI / 3);
    setBounds(zNear, zFar);
  }

  /**
   * Same as {@code this(context, width, height, type, new Vector(), radius)}.
   *
   * @see #Graph(Object, int, int, Type, Vector, float)
   */
  protected Graph(Object context, int width, int height, Type type, float radius) {
    this(context, width, height, type, new Vector(), radius);
  }

  /**
   * Defines a right-handed graph with the specified {@code width} and {@code height}
   * screen window dimensions. Creates and {@link #eye()} node, sets its {@link #fov()} to
   * {@code PI/3}. Calls {@link #setBounds(Vector, float)} on {@code center} and
   * {@code radius} to set up the scene frustum and {@link #fit()} to display the
   * whole scene.
   * <p>
   * The constructor also instantiates the graph main {@link #context()} and
   * {@code back-buffer} matrix-handlers (see {@link MatrixHandler}) and
   * {@link #TimingHandler}.
   *
   * @see #setBounds(float, float)
   * @see #Graph(Object, int, int, Type, float, float)
   * @see #TimingHandler
   * @see MatrixHandler
   */
  protected Graph(Object context, int width, int height, Type type, Vector center, float radius) {
    _init(context, width, height, new Node(), type);
    if (is3D())
      setFOV((float) Math.PI / 3);
    setBounds(center, radius);
    fit();
  }

  /**
   * Same as {@code this(context, width, height, eye, type, new Vector(), radius)}.
   *
   * @see #Graph(Object, int, int, Node, Type, Vector, float)
   */
  protected Graph(Object context, int width, int height, Node eye, Type type, float radius) {
    this(context, width, height, eye, type, new Vector(), radius);
  }

  /**
   * Defines a right-handed graph with the specified {@code width} and {@code height}
   * screen window dimensions. Calls {@link #setBounds(Vector, float)}
   * on {@code center} and {@code radius} to set up the scene frustum.
   * <p>
   * The constructor also instantiates the graph main {@link #context()} and
   * {@code back-buffer} matrix-handlers (see {@link MatrixHandler}) and
   * {@link #TimingHandler}.
   *
   * @see #setBounds(Vector, float)
   * @see #Graph(Object, int, int, Node, Type, float, float)
   * @see #TimingHandler
   * @see MatrixHandler
   */
  protected Graph(Object context, int width, int height, Node eye, Type type, Vector center, float radius) {
    _init(context, width, height, eye, type);
    setBounds(center, radius);
  }

  /**
   * Same as {@code this(context, width, height, eye, Type.PERSPECTIVE, zNear, zFar)}.
   *
   * @see #Graph(Object, int, int, Node, Type, float, float)
   */
  public Graph(Object context, int width, int height, Node eye, float zNear, float zFar) {
    this(context, width, height, eye, Type.PERSPECTIVE, zNear, zFar);
  }

  /**
   * Defines a right-handed graph with the specified {@code width} and {@code height}
   * screen window dimensions. Calls {@link #setBounds(float, float)}
   * on {@code zNear} and {@code zFar} to set up the scene frustum.
   * <p>
   * The constructor also instantiates the graph main {@link #context()} and
   * {@code back-buffer} matrix-handlers (see {@link MatrixHandler}) and
   * {@link #TimingHandler}.
   *
   * @see #setBounds(Vector, float)
   * @see #Graph(Object, int, int, Node, Type, Vector, float)
   * @see #TimingHandler
   * @see MatrixHandler
   */
  protected Graph(Object context, int width, int height, Node eye, Type type, float zNear, float zFar) {
    _init(context, width, height, eye, type);
    setBounds(zNear, zFar);
  }

  /**
   * Used internally by several constructors.
   */
  protected void _init(Object context, int width, int height, Node eye, Type type) {
    if (!_seeded) {
      _seededGraph = true;
      _seeded = true;
      // only Java disable concurrence
      boolean message = false;
      for (Task task : TimingHandler.tasks()) {
        if (task.isConcurrent())
          message = true;
        task.disableConcurrence();
      }
      if (message)
        System.out.println("Warning: all timing-tasks made non-concurrent");
    }
    _fb = context;
    _matrixHandler = new MatrixHandler();
    _bbMatrixHandler = new MatrixHandler();
    setWidth(width);
    setHeight(height);
    _tags = new HashMap<String, Node>();
    _rays = new ArrayList<Ray>();
    _functors = new HashMap<Integer, BiConsumer<Graph, Node>>();
    if (eye == null)
      throw new RuntimeException("Error eye shouldn't be null");
    setEye(eye);
    _translationTask = new InertialTask() {
      @Override
      public void action() {
        _shift(_x, _y, _z);
      }
    };
    _lookAroundTask = new InertialTask() {
      @Override
      public void action() {
        _lookAround();
      }
    };
    _cadRotateTask = new InertialTask() {
      @Override
      public void action() {
        _cad();
      }
    };
    setType(type);
    enableHint(HUD | SHAPE);
    picking = true;
    // middle grey encoded as a processing int rgb color
    _gridStroke = -8553091;
    // green encoded as a processing int rgb color
    _centerStroke = -16711936;
    _gridType = GridType.DOTS;
    _gridSubDiv = 10;
    _background = -16777216;
  }

  /**
   * Same as {@code return Node.random(this)}. Creates a random node.
   *
   * @see Node#random(Graph)
   * @see #randomize(Node)
   */
  public Node randomNode() {
    return Node.random(this);
  }

  /**
   * Same as {@code node.randomize(center(), radius(), is3D())}.
   *
   * @see Node#randomize(Vector, float, boolean)
   */
  public void randomize(Node node) {
    node.randomize(center(), _radius, is3D());
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
   * Sets the graph {@link #width()} in pixels.
   */
  public void setWidth(int width) {
    if (width != width() && width > 0) {
      _width = width;
      _modified();
    }
  }

  /**
   * Sets the graph {@link #height()} in pixels.
   */
  public void setHeight(int height) {
    if (height != height() && height > 0) {
      _height = height;
      _modified();
    }
  }

  // Type handling stuff

  /**
   * Returns the graph type. Set by {@link #setType(Type)}.
   *
   * @see #setType(Type)
   */
  public Type type() {
    return _type;
  }

  /**
   * Defines the graph {@link #_type} according to the projection of the scene.
   * Either {@link Type#PERSPECTIVE}, {@link Type#ORTHOGRAPHIC}, {@link Type#TWO_D}
   * or {@link Type#CUSTOM}.
   * <p>
   * {@link Type#PERSPECTIVE} and {@link Type#ORTHOGRAPHIC} use the classical projection
   * matrices and the node {@link Node#worldMagnitude()}. Both use {@link #zNear()} and
   * {@link #zFar()} (to define their clipping planes) and {@link #width()} and {@link #height()}
   * for frustum shape.
   * <p>
   * A {@link Type#TWO_D} behaves like {@link Type#ORTHOGRAPHIC}, but instantiated graph
   * nodes will be constrained so that they will remain at the x-y plane.
   *
   * @see Node#worldMagnitude()
   */
  public void setType(Type type) {
    if (_type == Type.TWO_D) {
      return;
    }
    if (type != _type && type != null) {
      _modified();
      this._type = type;
    }
  }

  /**
   * Shifts the graph {@link #_type} between {@link Type#PERSPECTIVE} and {@link Type#ORTHOGRAPHIC} while trying
   * to keep the {@link #fov()}. Only meaningful if graph {@link #is3D()}.
   *
   * @see #setType(Type)
   * @see #setFOV(float)
   * @see #fov()
   * @see #hfov()
   * @see #setHFOV(float)
   */
  public void togglePerspective() {
    if (is3D()) {
      float fov = fov();
      setType(_type == Type.PERSPECTIVE ? Type.ORTHOGRAPHIC : Type.PERSPECTIVE);
      setFOV(fov);
    }
  }

  /**
   * Sets the {@link #eye()} {@link Node#worldMagnitude()}, according to {@code fov} (field-of-view)
   * which is expressed in radians. Meaningless if the graph {@link #is2D()}.
   * If the graph {@link #_type} is {@link Type#ORTHOGRAPHIC} it will match the perspective
   * projection obtained using {@code fov} of an image centered at the world XY plane from
   * the eye current position.
   * <p>
   * Computed as as {@code Math.tan(fov/2)} if the graph type is {@link Type#PERSPECTIVE} and as
   * {@code Math.tan(fov / 2) * 2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())) / width()}
   * if the graph {@link #_type} is {@link Type#ORTHOGRAPHIC}.
   *
   * @see #fov()
   * @see #hfov()
   * @see #setHFOV(float)
   * @see #setType(Type)
   */
  public void setFOV(float fov) {
    if (is2D()) {
      System.out.println("Warning: setFOV() is meaningless in 2D. Use eye().setMagnitude() instead");
      return;
    }
    float magnitude = _type == Type.PERSPECTIVE ?
        (float) Math.tan(fov / 2) :
        (float) Math.tan(fov / 2) * 2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis())) / (float) width();
    if (magnitude > 0)
      eye().setWorldMagnitude(magnitude);
  }

  /**
   * Retrieves the scene field-of-view in radians. Meaningless if the scene {@link #is2D()}.
   * See {@link #setFOV(float)} for details. The value is related to the {@link #eye()}
   * {@link Node#worldMagnitude()} as follows:
   * <p>
   * <ol>
   * <li>It returns {@code 2 * Math.atan(eye().magnitude())}, when the
   * graph {@link #type()} is {@link Type#PERSPECTIVE}.</li>
   * <li>It returns {@code 2 * Math.atan(eye().magnitude() * width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()))))},
   * if the graph {@link #_type} is {@link Type#ORTHOGRAPHIC}.</li>
   * </ol>
   * Set this value with {@link #setFOV(float)} or {@link #setHFOV(float)}.
   *
   * @see Node#worldMagnitude()
   * @see #setType(Type)
   * @see #setHFOV(float)
   * @see #hfov()
   * @see #setFOV(float)
   */
  public float fov() {
    if (is2D()) {
      System.out.println("Warning: fov() is meaningless in 2D. Use eye().magnitude() instead");
      return 1;
    }
    return _type == Type.PERSPECTIVE ?
        2 * (float) Math.atan(eye().worldMagnitude()) :
        2 * (float) Math.atan(eye().worldMagnitude() * (float) width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis()))));
  }

  /**
   * Sets the {@link #hfov()} of the {@link #eye()} (in radians).
   * <p>
   * {@link #hfov()} and {@link #fov()} are linked by the {@link Graph#aspectRatio()}. This method actually
   * calls: {@code setFOV(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()))} so that a
   * call to {@link #hfov()} returns the expected value.
   *
   * @see #setFOV(float)
   * @see #fov()
   * @see #hfov()
   * @see #setFOV(float)
   */
  public void setHFOV(float hfov) {
    setFOV(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()));
  }

  /**
   * Same as {@code return type() == Type.PERSPECTIVE ? radians(eye().magnitude() * aspectRatio()) : eye().magnitude()}.
   * <p>
   * Returns the {@link #eye()} horizontal field-of-view in radians.
   *
   * @see #fov()
   * @see #setHFOV(float)
   * @see #setFOV(float)
   */
  public float hfov() {
    if (is2D()) {
      System.out.println("Warning: hfov() is meaningless in 2D. Use eye().magnitude() instead");
      return 1;
    }
    return _type == Type.PERSPECTIVE ?
        2 * (float) Math.atan(eye().worldMagnitude() * aspectRatio()) :
        2 * (float) Math.atan(eye().worldMagnitude() * aspectRatio() * (float) width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis()))));
  }

  /**
   * Returns the near clipping plane distance used to compute the {@link #projection()} matrix.
   * <p>
   * The clipping planes' positions depend on the {@link #radius()} and {@link #center()}
   * rather than being fixed small-enough and large-enough values. A good approximation will
   * hence result in an optimal precision of the z-buffer.
   * <p>
   * The near clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient} * {@link #radius()} in front of the
   * {@link #center()}: {@code Vector.scalarProjection(
   * Vector.subtract(eye().position(), center()), eye().zAxis()) - zClippingCoefficient() * radius()}
   * <p>
   * In order to prevent negative or too small {@link #zNear()} values (which would
   * degrade the z precision), {@code zNearCoefficient} is used when the eye is
   * inside the {@link #radius()} ball:
   * <p>
   * {@code zMin = zNearCoefficient() * zClippingCoefficient() * radius();} <br>
   * {@code zNear = zMin;}<br>
   * {@code With an ORTHOGRAPHIC and TWO_D types, the value is simply clamped to 0}<br>
   * <p>
   * See also the {@link #zFar()} documentation.
   *
   * <b>Attention:</b> The value is always positive, although the clipping plane is
   * positioned at a negative z value in the eye coordinate system.
   *
   * @see #zFar()
   */
  public float zNear() {
    if (_fixed)
      return _zNear;
    float z = Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis()) - _zClippingCoefficient * _radius;
    // Prevents negative or null zNear values.
    float zMin = _zNearCoefficient * _zClippingCoefficient * _radius;
    if (z < zMin)
      switch (_type) {
        case PERSPECTIVE:
          z = zMin;
          break;
        case TWO_D:
        case ORTHOGRAPHIC:
          z = 0.0f;
          break;
      }
    return z;
  }

  /**
   * Returns the far clipping plane distance used to compute the
   * {@link #projection()} matrix in world units.
   * <p>
   * The far clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * radius()} behind the {@link #center()}:
   * <p>
   * {@code zFar = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())
   * + zClippingCoefficient() * radius()}
   *
   * @see #zNear()
   */
  public float zFar() {
    return _fixed ? _zFar: Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis()) + _zClippingCoefficient * _radius;
  }

  // Graph and nodes stuff

  /**
   * Returns the top-level nodes (those which reference is null).
   * <p>
   * All leading nodes are also reachable by the {@link #render()} algorithm for which they are the seeds.
   *
   * @see #nodes()
   * @see #prune(Node)
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
   * Same as {@code for(Node node : _leadingNodes()) pruneBranch(node)}.
   *
   * @see #prune(Node)
   */
  public static void clear() {
    for (Node node : _leadingNodes())
      prune(node);
  }

  /**
   * Make all the nodes in the {@code node} branch eligible for garbage collection.
   * <p>
   * A call to {@link Node#isReachable()} on all {@code node} descendants
   * (including {@code node}) will return false, after issuing this method. It also means
   * that all nodes in the {@code node} branch will become unreachable by the
   * {@link #render()} algorithm.
   * <p>
   * Note that all the node inertial tasks are unregistered from the {@link #TimingHandler}.
   * <p>
   * To make all the nodes in the branch reachable again, call {@link Node#setReference(Node)}
   * on the pruned node.
   *
   * @see #clear()
   * @see Node#setReference(Node)
   * @see Node#isReachable()
   */
  public static void prune(Node node) {
    if (node.isReachable()) {
      List<Node> branch = branch(node);
      for (Node descendant : branch) {
        descendant._unregisterTasks();
      }
    }
    if (node.reference() != null) {
      node.reference()._removeChild(node);
    } else
      _removeLeadingNode(node);
  }

  /**
   * Returns a list of all the nodes that are reachable by the {@link #render()}
   * algorithm.
   * <p>
   * The method render the hierarchy to collect. Node collections should thus be kept at user space
   * for efficiency.
   *
   * @see #isEye(Node)
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

  // Timing stuff

  /**
   * Returns the translation inertial task.
   * Useful if you need to customize the timing task, e.g., to enable concurrency on it.
   */
  public Task translationInertialTask() {
    return _translationTask;
  }

  /**
   * Returns the look-around inertial task.
   * Useful if you need to customize the timing task, e.g., to enable concurrency on it.
   */
  public Task lookAroundInertialTask() {
    return _lookAroundTask;
  }

  /**
   * Returns the cad-rotate inertial task.
   * Useful if you need to customize the timing task, e.g., to enable concurrency on it.
   */
  public Task cadRotateInertialTask() {
    return _cadRotateTask;
  }

  // Matrix and transformations stuff

  /**
   * Begin Heads Up Display (HUD) so that drawing can be done using 2D screen coordinates.
   * <p>
   * All screen drawing should be enclosed between {@link #beginHUD()} and
   * {@link #endHUD()}. Then you can just begin drawing your screen shapes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d graph
   * then draw first all your 3d before doing any call to a {@link #beginHUD()}
   * and {@link #endHUD()} pair.
   * <p>
   * Wrapper for {@link MatrixHandler#beginHUD(int, int)}.
   *
   * @see #endHUD()
   * @see MatrixHandler#beginHUD(int, int)
   */
  public void beginHUD() {
    _matrixHandler.beginHUD(width(), height());
  }

  /**
   * Ends Heads Up Display (HUD). Throws an exception if
   * {@link #beginHUD()} wasn't properly called before.
   * <p>
   * Wrapper for {@link MatrixHandler#endHUD()}.
   *
   * @see #beginHUD()
   * @see MatrixHandler#endHUD()
   */
  public void endHUD() {
    _matrixHandler.endHUD();
  }

  // Eye stuff

  /**
   * Checks whether or not the given node is the {@link #eye()}.
   */
  public boolean isEye(Node node) {
    return _eye == node;
  }

  /**
   * Returns the associated eye. Never null.
   *
   * @see #setEye(Node)
   */
  public Node eye() {
    return _eye;
  }

  /**
   * Replaces the current {@link #eye()} with {@code eye}. If {@code eye} is instance of
   * {@link Node} it should belong to this graph object.
   *
   * @see #eye()
   */
  public void setEye(Node eye) {
    if (eye == null || _eye == eye)
      return;
    if (isTagged(eye)) {
      untag(eye);
      System.out.println("Warning: node was untagged since it was set as the eye");
    }
    if (_eye != null) {
      _eye._frustumGraphs.remove(this);
    }
    _eye = eye;
    _eye._frustumGraphs.add(this);
    if (_interpolator == null)
      _interpolator = new Interpolator(_eye);
    else
      _interpolator.setNode(_eye);
    _modified();
  }

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
    if (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = TimingHandler.frameCount;
    }
    for (int i = 0; i < (is3D() ? 6 : 4); ++i)
      if (distanceToBound(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Returns {@link Visibility#VISIBLE}, {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the ball (of radius {@code radius}
   * and center {@code center}) is visible, invisible, or semi-visible, respectively.
   *
   * @see #distanceToBound(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #boxVisibility(Vector, Vector)
   * @see #bounds()
   * @see #_updateBounds()
   */
  public Visibility ballVisibility(Vector center, float radius) {
    if (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = TimingHandler.frameCount;
    }
    boolean allInForAllPlanes = true;
    for (int i = 0; i < (is3D() ? 6 : 4); ++i) {
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
    if (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = TimingHandler.frameCount;
    }
    boolean allInForAllPlanes = true;
    for (int i = 0; i < (is3D() ? 6 : 4); ++i) {
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
    if (is3D())
      _updateBoundaryEquations3();
    else
      _updateBoundaryEquations2();
  }

  protected void _initCoefficients() {
    int rows = is3D() ? 6 : 4, cols = is3D() ? 4 : 3;
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
    Vector pos = eye().worldPosition();
    Vector viewDir = viewDirection();
    Vector up = upVector();
    Vector right = rightVector();
    float posViewDir = Vector.dot(pos, viewDir);
    switch (_type) {
      case PERSPECTIVE: {
        // horizontal fov: radians(eye().magnitude() * aspectRatio())
        float hhfov = 2 * (float) Math.atan(eye().worldMagnitude() * aspectRatio()) / 2.0f;
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
        float wh0 = eye().worldMagnitude() * (float) width() / 2;
        float wh1 = eye().worldMagnitude() * (float) height() / 2;
        _distance[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh0)), _normal[0]);
        _distance[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh0)), _normal[1]);
        _distance[4] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh1)), _normal[4]);
        _distance[5] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh1)), _normal[5]);
        break;
    }
    // Front and far planes are identical for both camera types.
    _normal[2] = Vector.multiply(viewDir, -1);
    _normal[3] = viewDir;
    _distance[2] = -posViewDir - zNear();
    _distance[3] = posViewDir + zFar();
    for (int i = 0; i < 6; ++i) {
      _coefficients[i][0] = _normal[i]._vector[0];
      _coefficients[i][1] = _normal[i]._vector[1];
      _coefficients[i][2] = _normal[i]._vector[2];
      _coefficients[i][3] = _distance[i];
    }
  }

  protected void _updateBoundaryEquations2() {
    // Computed once and for all
    Vector pos = eye().worldPosition();
    Vector up = upVector();
    Vector right = rightVector();
    _normal[0] = Vector.multiply(right, -1);
    _normal[1] = right;
    _normal[2] = up;
    _normal[3] = Vector.multiply(up, -1);
    float wh0 = eye().worldMagnitude() * (float) width() / 2;
    float wh1 = eye().worldMagnitude() * (float) height() / 2;
    _distance[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh0)), _normal[0]);
    _distance[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh0)), _normal[1]);
    _distance[2] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh1)), _normal[2]);
    _distance[3] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh1)), _normal[3]);
    for (int i = 0; i < 4; ++i) {
      _coefficients[i][0] = _normal[i]._vector[0];
      _coefficients[i][1] = _normal[i]._vector[1];
      // Change respect to Camera occurs here:
      _coefficients[i][2] = -_distance[i];
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
    if (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = TimingHandler.frameCount;
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
    if (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0) {
      _updateBounds();
      _lastEqUpdate = TimingHandler.frameCount;
    }
    Vector myVector = new Vector(_coefficients[index][0], _coefficients[index][1], _coefficients[index][2]);
    if (is3D())
      return Vector.dot(position, myVector) - _coefficients[index][3];
    else
      return (_coefficients[index][0] * position.x() + _coefficients[index][1] * position.y() + _coefficients[index][2])
          / (float) Math
          .sqrt(_coefficients[index][0] * _coefficients[index][0] + _coefficients[index][1] * _coefficients[index][1]);
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
   * A line of {@code n * sceneToPixelRatio()} graph units, located at {@code position} in
   * the world coordinate system, will be projected with a length of {@code n} pixels on
   * screen.
   * <p>
   * Use this method to scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #center()} and
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
        return 2.0f * Math.abs((eye().location(position))._vector[2] * eye().worldMagnitude()) * (float) Math.tan(fov() / 2.0f) / (float) height();
      case TWO_D:
      case ORTHOGRAPHIC:
        return eye().worldMagnitude();
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
   * Vertices must given in clockwise order if graph is not {@link Graph#leftHanded}
   * or in counter-clockwise order if {@link Graph#leftHanded}.
   *
   * @param a first face vertex
   * @param b second face vertex
   * @param c third face vertex
   * @see #isFaceBackFacing(Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector a, Vector b, Vector c) {
    return isFaceBackFacing(a, leftHanded ?
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
      camAxis = Vector.subtract(vertex, eye().worldPosition());
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
   * Returns the radius of the graph observed by the eye in world units. Set it with
   * {@link #setBounds(Vector, float)} or {@link #setBounds(float, float)}.
   *
   * @see #setBounds(Vector, float)
   * @see #setBounds(float, float)
   * @see #center()
   */
  public float radius() {
    return _radius;
  }

  /**
   * Returns the position of the graph center, defined in the world coordinate system.
   * Set it with {@link #setBounds(Vector, float)} or {@link #setBounds(float, float)}.
   *
   * @see #setBounds(Vector, float)
   * @see #setBounds(float, float)
   * @see #zNear()
   * @see #zFar()
   */
  public Vector center() {
    return _fixed ? eye().worldLocation(new Vector(0, 0, -(zNear() + zFar()) / (eye().worldMagnitude() * 2) )) : _center;
  }

  /**
   * Same as {@code setBounds(new Vector(), radius)}.
   *
   * @see #setBounds(float, float)
   * @see #setBounds(Vector, float, float, float)
   * @see #setBounds(float, float)
   */
  public void setBounds(float radius) {
    setBounds(new Vector(), radius);
  }

  /**
   * Same as {@code setBounds(center, radius, 0.005f, (float) Math.sqrt(3.0f))}.
   *
   * @see #setBounds(float, float)
   * @see #setBounds(Vector, float, float, float)
   * @see #setBounds(float)
   */
  public void setBounds(Vector center, float radius) {
    setBounds(center, radius, 0.005f, (float) Math.sqrt(3.0f));
  }

  /**
   * Sets the scene bounding sphere, defined by {@code center} and {@code radius}) in the
   * world coordinate system. The {@link #zNear()} and {@link #zFar()} computation is performed
   * so that it adapts to best fit this bounding sphere. To set fixed {@link #zNear()} and
   * {@link #zFar()} values use {@link #setBounds(float, float)} instead.
   * <p>
   * The {@code zNearCoefficient} (only meaningful for perspective projections) is used to set
   * the {@link #zNear()} when the {@link #eye()} is
   * inside the ball defined by {@code #center} and zClippingCoefficient * {@code #radius}.
   * In that case, the {@link #zNear()} value is set to
   * {@code zNearCoefficient * zClippingCoefficient * radius}. Default value is 0.005, which
   * is appropriate for most applications. In case you need a high dynamic ZBuffer precision,
   * you can increase this value (~0.1). A lower value will prevent clipping of very close
   * objects at the expense of a worst Z precision.
   * <p>
   * The {@code zClippingCoefficient} is used to position the near and far clipping planes.
   * The near (resp. far) clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient * radius()} in front of (resp. behind) the {@code center}.
   * This guarantees an optimal use of the z-buffer range and minimizes aliasing. Default
   * value is square root of 3 (so that a cube of edge size 2*{@link #radius()} is not clipped).
   * <p>
   * See the {@link #zNear()} and {@link #zFar()} documentations.
   *
   * @see #setBounds(float)
   * @see #setBounds(Vector, float)
   * @see #setBounds(float, float)
   * @see #zNear()
   * @see #zFar()
   */
  public void setBounds(Vector center, float radius, float zNearCoefficient, float zClippingCoefficient) {
    _fixed = false;
    _center = center;
    _radius = Math.abs(radius);
    _zNearCoefficient = zNearCoefficient;
    _zClippingCoefficient = zClippingCoefficient;
    _modified();
  }

  /**
   * Sets fixed {@link #zNear()} and {@link #zFar()} values. Use
   * {@link #setBounds(Vector, float, float, float)} to make them fit a
   * bounding sphere defined in the world coordinate system instead.
   * <p>
   * Note that the {@link #center()} is computed as
   * {@code eye().worldLocation(new Vector(0, 0, -(zNear() + zFar()) / (eye().magnitude() * 2) ))}.
   *
   * @see #setBounds(Vector, float, float, float)
   * @see #setBounds(Vector, float)
   * @see #zNear()
   * @see #zFar()
   */
  public void setBounds(float zNear, float zFar) {
    float near = Math.abs(zNear);
    float far = Math.abs(zFar);
    if (far <= near || near == 0)
      return;
    if (is2D()) {
      System.out.println("Warning: setBounds(zNear, zFar) only available in 3D. Calling setBounds((zFar - zNear) / 2) instead!");
      setBounds((far - near) / 2);
      return;
    }
    _fixed = true;
    _zNear = near;
    _zFar = far;
    _radius = (far - near) / 2;
    _modified();
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #eye()}
   * ({@code node().worldDisplacement(new Vector(0.0f, 0.0f, -1.0f))}). In 2D
   * it always is (0,0,-1).
   * <p>
   * Change this value using {@link #setViewDirection(Vector)}, {@link #lookAt(Vector)} or
   * {@link Node#setWorldOrientation(Quaternion)}. It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    return eye().zAxis(false);
  }

  /**
   * Rotates the eye so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The eye {@link Node#worldPosition()} is not modified. The eye is rotated so that the
   * horizon (defined by its {@link #upVector()}) is preserved.
   *
   * @see #lookAt(Vector)
   * @see #setUpVector(Vector)
   */
  public void setViewDirection(Vector direction) {
    if (direction.squaredNorm() == 0)
      return;

    Vector xAxis = direction.cross(upVector());
    if (xAxis.squaredNorm() == 0) {
      // target is aligned with upVector, this means a rotation around X axis
      // X axis is then unchanged, let's keep it !
      xAxis = eye().xAxis();
    }

    Quaternion q = new Quaternion();
    q.fromRotatedBasis(xAxis, xAxis.cross(direction), Vector.multiply(direction, -1));
    eye().setWorldOrientation(q);
  }

  /**
   * Convenience function that simply calls {@code setUpVector(up, true)}.
   *
   * @see #setUpVector(Vector, boolean)
   */
  public void setUpVector(Vector up) {
    setUpVector(up, true);
  }

  /**
   * Rotates the eye so that its {@link #upVector()} becomes {@code up} (defined in the
   * world coordinate system).
   * <p>
   * The eye is rotated around an axis orthogonal to {@code up} and to the current
   * {@link #upVector()} direction.
   * <p>
   * Use this method in order to define the eye horizontal plane.
   * <p>
   * When {@code noMove} is set to {@code false}, the orientation modification is
   * compensated by a translation, so that the {@link #center()} stays projected at the
   * same position on screen. This is especially useful when the eye is an observer of the
   * graph.
   * <p>
   * When {@code noMove} is true, the Eye {@link Node#worldPosition()} is left unchanged, which is
   * an intuitive behavior when the Eye is in first person mode.
   *
   * @see #lookAt(Vector)
   */
  public void setUpVector(Vector up, boolean noMove) {
    Quaternion q = new Quaternion(new Vector(0.0f, 1.0f, 0.0f), eye().displacement(up));
    if (!noMove)
      eye().setWorldPosition(Vector.subtract(center(), (Quaternion.multiply(eye().worldOrientation(), q)).rotate(eye().location(center()))));
    eye().rotate(q);
  }

  /**
   * Returns the normalized up vector of the eye, defined in the world coordinate system.
   * <p>
   * Set using {@link #setUpVector(Vector)} or {@link Node#setWorldOrientation(Quaternion)}. It is
   * orthogonal to {@link #viewDirection()} and to {@link #rightVector()}.
   * <p>
   * It corresponds to the Y axis of the associated {@link #eye()} (actually returns
   * {@code node().yAxis()}
   */
  public Vector upVector() {
    return eye().yAxis();
  }

  /**
   * 2D eyes simply call {@code node().setPosition(target.x(), target.y())}. 3D
   * eyes set {@link Node#worldOrientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The eye {@link Node#worldPosition()} is not modified.
   * Simply {@link #setViewDirection(Vector)}).
   *
   * @see #at()
   * @see #setUpVector(Vector)
   * @see #fit()
   * @see #fit(Vector, float)
   * @see #fit(Vector, Vector)
   */
  public void lookAt(Vector target) {
    if (is2D())
      eye().setWorldPosition(target.x(), target.y());
    else
      setViewDirection(Vector.subtract(target, eye().worldPosition()));
  }

  /**
   * Returns the normalized right vector of the eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}. Set using
   * {@link #setUpVector(Vector)}, {@link #lookAt(Vector)} or {@link Node#setWorldOrientation(Quaternion)}.
   * <p>
   * Simply returns {@code node().xAxis()}.
   */
  public Vector rightVector() {
    return eye().xAxis();
  }

  /**
   * 2D eyes return the position. 3D eyes return a point defined in the world
   * coordinate system where the eyes is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   *
   * @see #lookAt(Vector)
   */
  public Vector at() {
    return Vector.add(eye().worldPosition(), viewDirection());
  }

  /**
   * Returns the {@link #eye()} {@link Interpolator} used by {@link #fit(float)},
   * {@link #fit(int, int, int, int)}, {@link #fit(Node)}, etc.
   */
  public Interpolator interpolator() {
    return _interpolator;
  }

  /**
   * Convenience function that simply calls {@code fit(node, 0)}.
   *
   * @see #fit(Node, float)
   * @see #fit(Vector, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit(float)
   * @see #fit(int, int, int, int)
   * @see #fit()
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Node node) {
    fit(node, 0);
  }

  /**
   * Smoothly interpolates the eye on a interpolator path so that it goes to {@code node}.
   * The {@code duration} defines the interpolation speed.
   *
   * @see #fit(Node)
   * @see #fit(Vector, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit(float)
   * @see #fit(int, int, int, int)
   * @see #fit()
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Node node, float duration) {
    if (duration <= 0) {
      eye().setWorldPosition(node);
      eye().setWorldOrientation(node);
      eye().setWorldMagnitude(node);
    } else {
      _interpolator.reset();
      _interpolator.clear();
      _interpolator.addKeyFrame(eye().detach());
      _interpolator.addKeyFrame(node, duration);
      _interpolator.run();
    }
  }

  /**
   * Same as {@code fitBall(center(), radius())}.
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #center()
   * @see #radius()
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit(float)
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit() {
    if (_fixed) {
      System.out.println("Warning: fit() is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    fit(center(), _radius);
  }

  /**
   * Same as {@code fitBall(center(), radius(), duration)}.
   * <p>
   * Note that this method is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #center()
   * @see #radius()
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(float duration) {
    if (_fixed) {
      System.out.println("Warning: fit(duration) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    fit(center(), _radius, duration);
  }

  /**
   * Moves the eye during {@code duration} seconds so that the ball defined by {@code center}
   * and {@code radius} is visible and fits the window.
   * <p>
   * In 3D the eye is simply translated along its {@link #viewDirection()} so that the
   * ball fits the screen. Its {@link Node#worldOrientation()} and its
   * {@link #fov()} are unchanged. You should therefore orientate the eye
   * before you call this method.
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #fit(float)
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector center, float radius, float duration) {
    if (_fixed) {
      System.out.println("Warning: fit(center, radius, duration) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    if (duration <= 0)
      fit(center, radius);
    else {
      _interpolator.reset();
      _interpolator.clear();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(center, radius);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.run();
    }
  }

  /**
   * Moves the eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector center, float radius) {
    if (_fixed) {
      System.out.println("Warning: fit(center, radius) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    switch (_type) {
      case TWO_D:
        lookAt(center);
        fitFOV();
        break;
      case ORTHOGRAPHIC:
        float distance = Vector.dot(Vector.subtract(center, center()), viewDirection()) + (radius / eye().worldMagnitude());
        eye().setWorldPosition(Vector.subtract(center, Vector.multiply(viewDirection(), distance)));
        fitFOV();
        break;
      case PERSPECTIVE:
        float yview = radius / (float) Math.sin(fov() / 2.0f);
        // horizontal fov: radians(eye().magnitude() * aspectRatio())
        float xview = radius / (float) Math.sin(2 * (float) Math.atan(eye().worldMagnitude() * aspectRatio()) / 2.0f);
        eye().setWorldPosition(Vector.subtract(center, Vector.multiply(viewDirection(), Math.max(xview, yview))));
        break;
    }
  }

  /**
   * Rescales the {@link #fov()} {@code duration} seconds so that the ball defined by {@code center}
   * and {@code radius} is visible and fits the window.
   * <p>
   * The eye position and orientation are not modified and you first have to orientate
   * the eye in order to actually see the scene (see {@link #lookAt(Vector)},
   * {@link #fit()} or {@link #fit(Vector, float)}).
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   * <p>
   * <b>Attention:</b> The {@link #fov()} is clamped to PI/2. This happens
   * when the eye is at a distance lower than sqrt(2) * radius() from the center().
   *
   * @see #fitFOV()
   * @see #fit(Vector, float)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   */
  public void fitFOV(float duration) {
    if (_fixed) {
      System.out.println("Warning: fitFOV(duration) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    if (duration <= 0)
      fitFOV();
    else {
      _interpolator.reset();
      _interpolator.clear();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fitFOV();
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.run();
    }
  }

  /**
   * Changes the {@link #eye()} {@link #fov()} so that the entire scene
   * (defined by {@link #center()} and {@link #radius()}) is visible.
   * <p>
   * The eye position and orientation are not modified and you first have to orientate
   * the eye in order to actually see the scene (see {@link #lookAt(Vector)},
   * {@link #fit()} or {@link #fit(Vector, float)}).
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   * <p>
   * <b>Attention:</b> The {@link #fov()} is clamped to PI/2. This happens
   * when the eye is at a distance lower than sqrt(2) * radius() from the center().
   *
   * @see #fitFOV(float)
   * @see #fit(Vector, float)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   */
  public void fitFOV() {
    if (_fixed) {
      System.out.println("Warning: fitFOV() is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    float distance = Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis());
    float magnitude = distance < (float) Math.sqrt(2) * _radius ? ((float) Math.PI / 2) : 2 * (float) Math.asin(_radius / distance);
    switch (_type) {
      case PERSPECTIVE:
        setFOV(magnitude);
        break;
      case ORTHOGRAPHIC:
        eye().setWorldMagnitude(distance < (float) Math.sqrt(2) * _radius ? 2 * _radius / (float) Math.min(width(), height()) : 2 * (float) Math.sin(magnitude) * distance / width());
      case TWO_D:
        eye().setWorldMagnitude(2 * _radius / (float) Math.min(width(), height()));
        break;
    }
  }

  /**
   * Smoothly moves the eye during {@code duration} seconds so that the world axis aligned
   * box defined by {@code corner1} and {@code corner2} is entirely visible.
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #fit(Vector, Vector)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector corner1, Vector corner2, float duration) {
    if (_fixed) {
      System.out.println("Warning: fit(corner1, corner2, duration) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    if (duration <= 0)
      fit(corner1, corner2);
    else {
      _interpolator.reset();
      _interpolator.clear();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(corner1, corner2);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.run();
    }
  }

  /**
   * Moves the eye so that the world axis aligned box defined by {@code corner1}
   * and {@code corner2} is entirely visible.
   * <p>
   * Note that this is not available when bounds are fixed. Use {@link #setBounds(Vector, float)} instead.
   *
   * @see #fit(Vector, Vector, float)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(int, int, int, int, float)
   * @see #fit()
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector corner1, Vector corner2) {
    if (_fixed) {
      System.out.println("Warning: fit(corner1, corner2) is not available when bounds are fixed. Call setBounds(center, radius) first.");
      return;
    }
    float diameter = Math.max(Math.abs(corner2._vector[1] - corner1._vector[1]), Math.abs(corner2._vector[0] - corner1._vector[0]));
    diameter = Math.max(Math.abs(corner2._vector[2] - corner1._vector[2]), diameter);
    fit(Vector.multiply(Vector.add(corner1, corner2), 0.5f), 0.5f * diameter);
  }

  /**
   * Smoothly moves the eye during {@code duration} seconds so that the rectangular
   * screen region defined by {@code rectangle} (pixel units, with origin in the
   * upper left corner) fits the screen.
   * <p>
   * The eye is translated (its {@link Node#worldOrientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define
   * <i>bounds</i> in 3D, it's the intersection of this bounds with a plane
   * (orthogonal to the {@link #viewDirection()} and passing through the
   * {@link #center()}) that is used to define the 3D rectangle that is eventually
   * fitted.
   *
   * @see #fit(int, int, int, int)
   * @see #fit(Vector, Vector, float)
   * @see #fit(Vector, Vector)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit()
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(int x, int y, int width, int height, float duration) {
    if (duration <= 0)
      fit(x, y, width, height);
    else {
      _interpolator.reset();
      _interpolator.clear();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(x, y, width, height);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.run();
    }
  }

  /**
   * Moves the eye so that the rectangular screen region defined by
   * {@code x, y, width, height} (pixel units, with origin in the upper left corner)
   * fits the screen.
   * <p>
   * In 3D the eye is translated (its {@link Node#worldOrientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #center()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   *
   * @see #fit(int, int, int, int, float)
   * @see #fit(Vector, Vector, float)
   * @see #fit(Vector, Vector)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit()
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   * @param x coordinate of the rectangle
   * @param y coordinate of the rectangle
   * @param width width of the rectangle
   * @param height height of the rectangle
   */
  public void fit(int x, int y, int width, int height) {
    int centerX = (int) ((float) x + (float) width / 2);
    int centerY = (int) ((float) y + (float) height / 2);
    //ad-hoc
    if (is2D()) {
      float rectRatio = (float) width / (float) height;
      if (aspectRatio() < 1.0f) {
        if (aspectRatio() < rectRatio)
          eye().setWorldMagnitude(eye().worldMagnitude() * (float) width / (float) width());
        else
          eye().setWorldMagnitude(eye().worldMagnitude() * (float) height / (float) height());
      } else {
        if (aspectRatio() < rectRatio)
          eye().setWorldMagnitude(eye().worldMagnitude() * (float) width / (float) width());
        else
          eye().setWorldMagnitude(eye().worldMagnitude() * (float) height / (float) height());
      }
      lookAt(location(new Vector(centerX, centerY, 0)));
      return;
    }
    Vector vd = viewDirection();
    float distToPlane = Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis());
    Vector orig = new Vector();
    Vector dir = new Vector();
    pixelToLine(centerX, centerY, orig, dir);
    Vector newCenter = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    pixelToLine(x, centerY, orig, dir);
    Vector pointX = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    pixelToLine(centerX, y, orig, dir);
    Vector pointY = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    float distance = 0.0f;
    float distX, distY;
    switch (_type) {
      case PERSPECTIVE:
        //horizontal fov: radians(eye().magnitude() * aspectRatio())
        distX = Vector.distance(pointX, newCenter) / (float) Math.sin(2 * (float) Math.atan(eye().worldMagnitude() * aspectRatio()) / 2.0f);
        distY = Vector.distance(pointY, newCenter) / (float) Math.sin(fov() / 2.0f);
        distance = Math.max(distX, distY);
        break;
      case ORTHOGRAPHIC:
        if (!_fixed) {
          float dist = Vector.dot(Vector.subtract(newCenter, center()), vd);
          distX = Vector.distance(pointX, newCenter) / eye().worldMagnitude() / aspectRatio();
          distY = Vector.distance(pointY, newCenter) / eye().worldMagnitude() / 1.0f;
          distance = dist + Math.max(distX, distY);
        } else {
          System.out.println("Warning: fit(x, y, width, height) is not available when bounds are fixed. Call setBounds(center, radius) first.");
        }
        break;
    }
    eye().setWorldPosition(Vector.subtract(newCenter, Vector.multiply(vd, distance)));
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
        if (leftHanded)
          pixelY = height() - pixelY;
        origin.set(eye().worldPosition());
        direction.set(new Vector(((2.0f * pixelX / (float) width()) - 1.0f) * eye().worldMagnitude() * aspectRatio(),
            ((2.0f * (height() - pixelY) / (float) height()) - 1.0f) * eye().worldMagnitude(),
            -1.0f));
        direction.set(Vector.subtract(eye().worldLocation(direction), origin));
        direction.normalize();
        break;
      case TWO_D:
      case ORTHOGRAPHIC: {
        origin.set(location(new Vector(pixelX, pixelY, 0)));
        direction.set(viewDirection());
        break;
      }
    }
  }

  // Other stuff

  /**
   * @return true if the graph is 2D.
   */
  public boolean is2D() {
    return _type == Type.TWO_D;
  }

  /**
   * @return true if the graph is 3D.
   */
  public boolean is3D() {
    return !is2D();
  }

  protected void _modified() {
    _lastNonEyeUpdate = TimingHandler.frameCount;
  }

  /**
   * Max between {@link Node#lastUpdate()} and {@link #_lastNonEyeUpdate()}.
   *
   * @return last frame the eye was updated
   * @see #_lastNonEyeUpdate()
   */
  public long lastUpdate() {
    return Math.max(eye().lastUpdate(), _lastNonEyeUpdate());
  }

  /**
   * @return last frame when a local eye parameter (different than the {@link #eye()}) was updated.
   * @see #lastUpdate()
   */
  protected long _lastNonEyeUpdate() {
    return _lastNonEyeUpdate;
  }

  // traversal

  /**
   * Same as {@code return track(null, pixelX, pixelY, nodeArray)}.
   *
   * @see #updateTag(String, int, int, Node[])
   */
  public Node updateTag(int pixelX, int pixelY, Node[] nodeArray) {
    return updateTag(null, pixelX, pixelY, nodeArray);
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
   * Same as {@code return track(null, pixelX, pixelY, nodeList)}.
   *
   * @see #updateTag(String, int, int, List)
   */
  public Node updateTag(int pixelX, int pixelY, List<Node> nodeList) {
    return updateTag(null, pixelX, pixelY, nodeList);
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
   * the ray. Not that the {@link #eye()} is never tagged. Same as {@code return updateTag(null, tag, pixelX, pixelY)}.
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
   * Tags (with {@code tag} which may be {@code null}) the node in the {@code subtree} (or the whole tree when
   * {@code subtree} is {@code null}) picked with ray-casting at pixel {@code pixelX, pixelY} and returns it
   * (see {@link #node(String)}). May return {@code null} if no node is intersected by the ray.
   * Not that the {@link #eye()} is never tagged.
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
    if (node(tag) == null && node.tagging == true && (node._bypass != TimingHandler.frameCount))
      if (tracks(node, pixelX, pixelY)) {
        tag(tag, node);
        return;
      }
    if (!node.cull && node(tag) == null)
      for (Node child : node.children())
        _track(tag, child, pixelX, pixelY);
  }

  /**
   * Condition for the node back picking.
   */
  protected boolean _backPicking(Node node) {
    return picking && node.tagging == true && !isEye(node) && _bb != null && (
        (node.isPickingEnabled(Node.CAMERA) && node.isHintEnabled(Node.CAMERA)) ||
            (node.isPickingEnabled(Node.AXES) && node.isHintEnabled(Node.AXES)) ||
            (node.isPickingEnabled(Node.HUD) && node.isHintEnabled(Node.HUD) && (node._imrHUD != null || node._rmrHUD != null)) ||
            (node._frustumGraphs != null && node.isPickingEnabled(Node.BOUNDS) && node.isHintEnabled(Node.BOUNDS)) ||
            (node.isPickingEnabled(Node.SHAPE) && node.isHintEnabled(Node.SHAPE) && (node._imrShape != null || node._rmrShape != null)) ||
            (node.isPickingEnabled(Node.TORUS) && node.isHintEnabled(Node.TORUS)) ||
            (node.isPickingEnabled(Node.FILTER) && node.isHintEnabled(Node.FILTER)) ||
            (node.isPickingEnabled(Node.BONE) && node.isHintEnabled(Node.BONE))
    );
  }

  /**
   * Condition for the node front picking.
   */
  protected boolean _frontPicking(Node node) {
    return picking && node.tagging == true && !isEye(node) && node.isPickingEnabled(Node.BULLSEYE) && node.isHintEnabled(Node.BULLSEYE);
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
    boolean result = false;
    if (_backPicking(node)) {
      result = _tracks(node, pixelX, pixelY);
    }
    if (!result) {
      if(_frontPicking(node)) {
        result = _tracks(node, pixelX, pixelY, screenLocation(node.worldPosition()));
      }
    }
    return result;
  }

  /**
   * A shape may be picked using
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a> with a
   * color buffer. This method
   * compares the color of a back-buffer at {@code (pixelX,pixelY)} against the {@link Node#id()}.
   * Returns true if both colors are the same, and false otherwise.
   * <p>
   * This method should be overridden. Default implementation simply return {@code false}.
   *
   * @see Node#setBullsEyeSize(float)
   */
  protected boolean _tracks(Node node, int pixelX, int pixelY) {
    return false;
  }

  /**
   * Cached version of {@link #tracks(Node, int, int)}.
   */
  protected boolean _tracks(Node node, int pixelX, int pixelY, Vector projection) {
    if (node == null || isEye(node) || projection == null)
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
   * Same as {@code tag(null, pixelX, pixelY)}.
   *
   * @see #tag(String, int, int)
   */
  public void tag(int pixelX, int pixelY) {
    tag(null, pixelX, pixelY);
  }

  /**
   * Same as {@link #updateTag(String, int, int)} but doesn't return immediately the tagged node.
   * The algorithm schedules an updated of the node to be tagged for the next traversal and hence
   * should be always be used in conjunction with {@link #render()}.
   * <p>
   * The tagged node (see {@link #node(String)}) would be available after the next call to
   * {@link #render()}. It may be {@code null} if no node is intersected by the ray. Not that
   * the {@link #eye()} is never tagged.
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
    _rays.add(new Ray(tag, pixelX, pixelY));
  }

  // Off-screen

  /**
   * Returns whether or not the scene has focus or not under the given pixel.
   */
  public boolean hasFocus(int pixelX, int pixelY) {
    return _lastDisplayed + 1 >= TimingHandler.frameCount
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
  public Object context() {
    return _fb;
  }

  /**
   * Returns the back buffer, used for
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a>. Maybe {@code null}
   */
  protected Object _backBuffer() {
    return _bb;
  }

  protected void _initBackBuffer() {}

  protected void _endBackBuffer() {}

  protected void _initFrontBuffer() {}

  protected void _endFrontBuffer() {}

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
    if (_cacheProjectionViewInverse < TimingHandler.frameCount) {
      _projectionViewInverse = Matrix.inverse(_projectionView);
      _cacheProjectionViewInverse = TimingHandler.frameCount;
    }
    return _projectionViewInverse;
  }

  // cache setters for projection times view and its inverse

  /**
   * Called by {@link #render(Node)} and performs the following:
   * <ol>
   * <li>Updates the projection matrix using
   * {@link Matrix#perspective(float, float, float, float)} or
   * {@link Matrix#orthographic(float, float, float, float)}.</li>
   * <li>Updates the view matrix by calling {@code eye().view()}.</li>
   * <li>Updates the {@link #projectionView()} matrix.</li>
   * </ol>
   *
   * @see #fov()
   * @see Node#view()
   */
  protected void _bind() {
    _projection = _type == Graph.Type.PERSPECTIVE ? Matrix.perspective(leftHanded ? -eye().worldMagnitude() : eye().worldMagnitude(), aspectRatio(), zNear(), zFar())
            : Matrix.orthographic(width() * eye().worldMagnitude(), (leftHanded ? -height() : height()) * eye().worldMagnitude(), zNear(), zFar());
    _view = eye().view();
    _projectionView = Matrix.multiply(_projection, _view);
    _matrixHandler.bind(_projection, _view);
  }

  /**
   * Begins the rendering process (see {@link #render(Node)}). Use it always before
   * {@link #closeContext()}. Binds the matrices to the renderer and displays the scene
   * {@link #hint()}.
   * <p>
   * This method is automatically called by {@link #render(Node)}. Call it explicitly only
   * when the scene {@link #hint()} is reset (see also {@link #resetHint()}) and you need
   * to customize that which is to be rendered, as follows:
   * <pre>
   * {@code
   * scene.beginRender();
   * scene.render(subtree);
   * // render your world located stuff here
   * // enable also the hud here if you want too:
   * // scene.beginHUD();
   * // draw your screen located stuff here
   * // scene.endHUD();
   * scene.endRender();
   * // call scene.image(pixelX, pixelY) to display the customized rendered scene:
   * scene.image();
   * }
   * </pre>
   *
   * @see #render(Node)
   * @see #closeContext()
   * @see #isOffscreen()
   * @see #context()
   */
  public void openContext() {
    _renderCount++;
    if (_renderCount < 1 || _renderCount > 2) {
      throw new RuntimeException("Error: render() should be nested within a single openContext() / closeContext() call!");
    }
    if (_renderCount == 1) {
      if (isOffscreen())
        _initFrontBuffer();
      _bind();
      _matrixHandler.pushMatrix();
      _displayHint();
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
    if (_renderCount < 0 || _renderCount > 1) {
      throw new RuntimeException("Error: render() should be nested within a single openContext() / closeContext() call!");
    }
    if (_renderCount == 0) {
      _rays.clear();
      _displayHUD();
      _matrixHandler.popMatrix();
      if (isOffscreen())
        _endFrontBuffer();
      else
        _lastDisplayed = TimingHandler.frameCount;
    }
  }

  /**
   * Renders the node tree onto the {@link #context()} from the {@link #eye()} viewpoint.
   * Calls {@link #setVisit(Node, BiConsumer)} on each visited node (refer to the {@link Node} documentation).
   * Same as {@code render(null)}.
   *
   * @see #render(Node)
   * @see #setVisit(Node, BiConsumer)
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
   * when {@code subtree} is {@code null}) onto the {@link #context()} from the {@link #eye()}
   * viewpoint, and calls {@link #closeContext()}.
   * <p>
   * Note that the rendering algorithm calls {@link #setVisit(Node, BiConsumer)} on each visited node
   * (refer to the {@link Node} documentation).
   *
   * @see #setVisit(Node, BiConsumer)
   * @see Node#cull
   * @see Node#bypass()
   * @see Node#setShape(Consumer)
   * @see Node#setShape(processing.core.PShape)
   */
  public void render(Node subtree) {
    openContext();
    _subtree = subtree;
    if (subtree == null) {
      for (Node node : _leadingNodes())
        _render(node);
    } else if (subtree.isReachable()) {
      if (subtree.reference() != null) {
        _matrixHandler.pushMatrix();
        _matrixHandler.applyWorldTransformation(subtree.reference());
      }
      _render(subtree);
      if (subtree.reference() != null) {
        _matrixHandler.popMatrix();
      }
    }
    closeContext();
  }

  /**
   * Sets a custom node visit for the {@link #render()} algorithm.
   * <p>
   * Bypassing the node rendering and/or performing hierarchical culling, i.e.,
   * culling of the node and its children, should be done here.
   *
   * <pre>
   * {@code
   * Graph scene = new Graph(context, width, height);
   * Node space = new Node();
   * public void visit(Graph graph, Node node) {
   *   if (graph.cullingCondition) {
   *     node.cull = true;
   *   }
   *   else if (bypassCondition) {
   *     node.bypass();
   *   }
   * }
   * scene.setVisit(space, visit);
   * }
   * </pre>
   * Note that the graph culling condition may be set from
   * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}.
   *
   * @see #setVisit(Node, Consumer)
   * @see #resetVisit(Node)
   * @see #render(Node)
   * @see Node#setVisit(Graph, BiConsumer)
   * @see Node#setVisit(Graph, Consumer)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void setVisit(Node node, BiConsumer<Graph, Node> functor) {
    _functors.put(node.id(), functor);
  }

  /**
   * Same as {@code setVisit(node, (g, n) -> functor.accept(n))}.
   *
   * @see #setVisit(Node, BiConsumer)
   * @see #resetVisit(Node)
   * @see #render(Node)
   * @see Node#setVisit(Graph, BiConsumer)
   * @see Node#setVisit(Graph, Consumer)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void setVisit(Node node, Consumer<Node> functor) {
    setVisit(node, (g, n) -> functor.accept(n));
  }

  /**
   * Resets the node custom visit set with {@link #setVisit(Node, BiConsumer)}.
   *
   * @see #setVisit(Node, BiConsumer)
   * @see #render(Node)
   * @see Node#bypass()
   * @see Node#cull
   */
  public void resetVisit(Node node) {
    _functors.remove(node.id());
  }

  /**
   * Used by the {@link #render(Node)} algorithm.
   */
  protected void _render(Node node) {
    _matrixHandler.pushMatrix();
    _matrixHandler.applyTransformation(node);
    BiConsumer<Graph, Node> functor = _functors.get(node.id());
    if (functor != null)
      functor.accept(this, node);
    if (!node.cull) {
      if (node._bypass != TimingHandler.frameCount) {
        _trackFrontBuffer(node);
        if (isOffscreen())
          _trackBackBuffer(node);
        if (_backPicking(node))
          _bbNeed = TimingHandler.frameCount;
        if (isTagged(node) && node._highlight > 0 && node._highlight <= 1) {
          _matrixHandler.pushMatrix();
          float scl = 1 + node._highlight;
          if (is2D())
            _matrixHandler.scale(scl, scl);
          else
            _matrixHandler.scale(scl, scl, scl);
          _displayFrontHint(node);
          _matrixHandler.popMatrix();
        } else {
          _displayFrontHint(node);
        }
      }
      for (Node child : node.children())
        _render(child);
    }
    _matrixHandler.popMatrix();
  }

  /**
   * Internal use. Traverse the scene {@link #nodes()}) into the
   * {@link #_backBuffer()} to perform picking on the scene {@link #nodes()}.
   * Use it as a {@code _postDraw()}.
   */
  protected void _renderBackBuffer() {
    if (picking && _bb != null && _bbCount < _bbNeed) {
      _initBackBuffer();
      _bbMatrixHandler.bind(projection(), view());
      if (_subtree == null) {
        for (Node node : _leadingNodes())
          _renderBackBuffer(node);
      } else {
        if (_subtree.reference() != null) {
          _bbMatrixHandler.pushMatrix();
          _bbMatrixHandler.applyWorldTransformation(_subtree.reference());
        }
        _renderBackBuffer(_subtree);
        if (_subtree.reference() != null) {
          _bbMatrixHandler.popMatrix();
        }
      }
      if (isOffscreen())
        _rays.clear();
      _endBackBuffer();
      _bbCount = _bbNeed;
    }
  }

  /**
   * Used by the {@link #_renderBackBuffer()} algorithm.
   */
  protected void _renderBackBuffer(Node node) {
    _bbMatrixHandler.pushMatrix();
    _bbMatrixHandler.applyTransformation(node);
    if (!node.cull) {
      if (node._bypass != TimingHandler.frameCount) {
        if (_backPicking(node)) {
          _displayBackHint(node);
        }
        if (!isOffscreen())
          _trackBackBuffer(node);
      }
      for (Node child : node.children())
        _renderBackBuffer(child);
    }
    _bbMatrixHandler.popMatrix();
  }

  protected void _emitBackBufferUniforms(Node node) {}

  /**
   * Displays the graph and nodes hud hint.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayHUD() {
  }

  /**
   * Draws the graph {@link #hint()}.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayHint() {
  }

  /**
   * Draws the node {@link Node#hint()} onto the {@link #context()}.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayFrontHint(Node node) {
  }

  /**
   * Draws the node {@link Node#hint()} into the picking buffer.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   */
  protected void _displayBackHint(Node node) {
  }

  /**
   * Internally used by {@link #_render(Node)}.
   */
  protected void _trackFrontBuffer(Node node) {
    if (_frontPicking(node) && !_rays.isEmpty()) {
      Vector projection = screenLocation(node.worldPosition());
      Iterator<Ray> it = _rays.iterator();
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

  /**
   * Internally used by {@link #_render(Node)} and {@link #_renderBackBuffer(Node)}.
   */
  protected void _trackBackBuffer(Node node) {
    if (_backPicking(node) && !_rays.isEmpty()) {
      Iterator<Ray> it = _rays.iterator();
      while (it.hasNext()) {
        Ray ray = it.next();
        removeTag(ray._tag);
        if (_tracks(node, ray._pixelX, ray._pixelY)) {
          tag(ray._tag, node);
          it.remove();
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
   * (see {@link #node(String)}). Tagging the {@link #eye()} is not allowed.
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
    if (node == eye()) {
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
   * @see Node#isTagged(Graph)
   */
  public boolean hasTag(String tag, Node node) {
    return node(tag) == node;
  }

  /**
   * Removes all tags {@link #node(String)}.
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
   * In 3D note that {@code pixel.z} is not a linear interpolation between {@link #zNear()} and
   * {@link #zFar()};
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
  public Vector displacement(Vector vector, Node node) {
    float dx = vector.x();
    float dy = leftHanded ? vector.y() : -vector.y();
    // Scale to fit the screen relative vector displacement
    if (_type == Type.PERSPECTIVE) {
      Vector position = node == null ? new Vector() : node.worldPosition();
      float k = (float) Math.tan(fov() / 2.0f) * Math.abs(eye().location(position)._vector[2] * eye().worldMagnitude());
      dx *= 2.0 * k / ((float) height() * eye().worldMagnitude());
      dy *= 2.0 * k / ((float) height() * eye().worldMagnitude());
    }
    float dz = vector.z();
    if (is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    } else {
      dz *= (zNear() - zFar()) / eye().worldMagnitude();
    }
    Vector eyeVector = new Vector(dx, dy, dz);
    return node == null ? eye().worldDisplacement(eyeVector) : node.displacement(eyeVector, eye());
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
  public Vector screenDisplacement(Vector vector, Node node) {
    Vector eyeVector = eye().displacement(vector, node);
    float dx = eyeVector.x();
    float dy = leftHanded ? eyeVector.y() : -eyeVector.y();
    if (_type == Type.PERSPECTIVE) {
      Vector position = node == null ? new Vector() : node.worldPosition();
      float k = (float) Math.tan(fov() / 2.0f) * Math.abs(eye().location(position)._vector[2] * eye().worldMagnitude());
      dx /= 2.0 * k / ((float) height() * eye().worldMagnitude());
      dy /= 2.0 * k / ((float) height() * eye().worldMagnitude());
    }
    float dz = eyeVector.z();
    if (is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    } else {
      // sign is inverted
      dz /= (zNear() - zFar()) / eye().worldMagnitude();
    }
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
   * Call the {@code node} (or the {@link #eye()} if {@code node} is null) interact
   * gesture parser function set either with {@link Node#setInteraction(Consumer)} or
   * {@link Node#setInteraction(BiConsumer)}.
   *
   * @see Node#setInteraction(BiConsumer)
   * @see Node#setInteraction(Consumer)
   */
  public void interact(Node node, Object... gesture) {
    if (node == null) {
      node = eye();
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
   * Aligns the node (use null for the world) with the {@link #eye()}.
   *
   * @see #align()
   * @see #align(String tag)
   */
  public void align(Node node) {
    if (node == null || node == eye()) {
      eye().align(true);
    }
    else {
      node.align(eye());
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
   * Focuses the node (use null for the world) with the {@link #eye()}.
   *
   * @see #focus()
   * @see #focus(String tag)
   */
  public void focus(Node node) {
    if (node == null || node == eye()) {
      eye().projectOnLine(center(), viewDirection());
    }
    else {
      node.projectOnLine(eye().worldPosition(), eye().zAxis(false));
    }
  }

  // 3. Scale

  /**
   * Same as {@code zoom(delta, Graph.inertia)}.
   *
   * @see #zoom(float, float)
   */
  public void zoom(float delta) {
    zoom(delta, Graph.inertia);
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
   * Same as {@code zoom(tag, delta, Graph.inertia)}.
   *
   * @see #zoom(String, float, float)
   */
  public void zoom(String tag, float delta) {
    zoom(tag, delta, Graph.inertia);
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
   * Same as {@code zoom(node, delta, Graph.inertia)}.
   *
   * @see #zoom(Node, float, float)
   */
  public void zoom(Node node, float delta) {
    zoom(node, delta, Graph.inertia);
  }

  /**
   * Scales the {@code node} (use null for the world) according to {@code delta} and
   * {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   */
  public void zoom(Node node, float delta, float inertia) {
    if (node == null || node == eye()) {
      float factor = 1 + Math.abs(delta) / (float) -height();
      eye().scale(delta >= 0 ? factor : 1 / factor, inertia);
    }
    else {
      float factor = 1 + Math.abs(delta) / (float) height();
      node.scale(delta >= 0 ? factor : 1 / factor, inertia);
    }
  }

  // 4. Translate

  /**
   * Same as {@code shift(dx, dy, dz, Graph.inertia)}.
   *
   * @see #shift(float, float, float, float)
   */
  public void shift(float dx, float dy, float dz) {
    shift(dx, dy, dz, Graph.inertia);
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
   * Same as {@code shift(tag, dx, dy, dz, Graph.inertia)}.
   *
   * @see #shift(String, float, float, float, float)
   */
  public void shift(String tag, float dx, float dy, float dz) {
    shift(tag, dx, dy, dz, Graph.inertia);
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
   * Same as {@code shift(node, dx, dy, dz, Graph.inertia)}.
   *
   * @see #shift(Node, float, float, float, float)
   */
  public void shift(Node node, float dx, float dy, float dz) {
    shift(node, dx, dy, dz, Graph.inertia);
  }

  /**
   * Translates the {@code node} (use null for the world) according to {@code (dx, dy, dz)}
   * defined in screen-space ((a box of {@link #width()} * {@link #height()} * 1 dimensions),
   * and {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   */
  public void shift(Node node, float dx, float dy, float dz, float inertia) {
    if (node == null || node == eye()) {
      node = eye().detach();
      node.setWorldPosition(center());
      Vector vector = displacement(new Vector(dx, dy, dz), node);
      vector.multiply(-1);
      Vector translation = eye().referenceDisplacement(vector);
      // Option 1: don't compensate orthographic, i.e., use Node.translate(vector, inertia)
      //eye().translate(translation);
      // Option 2: compensate orthographic, i.e., use Graph inertial translation task
      _shift(translation.x(), translation.y(), translation.z());
      _translationTask.setInertia(inertia);
      _translationTask._x += translation.x();
      _translationTask._y += translation.y();
      _translationTask._z += translation.z();
      if (!_translationTask.isActive()) {
        _translationTask.run();
      }
    }
    else {
      node.translate(node.referenceDisplacement(displacement(new Vector(dx, dy, dz), node)), inertia);
    }
  }

  /**
   * Internally by the _translationTask to compensate orthographic projection eye translation.
   */
  protected void _shift(float x, float y, float z) {
    float d1 = 1, d2;
    if (_type == Type.ORTHOGRAPHIC)
      d1 = Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis());
    eye().translate(x, y, z);
    if (_type == Type.ORTHOGRAPHIC) {
      d2 = Vector.scalarProjection(Vector.subtract(eye().worldPosition(), center()), eye().zAxis());
      if (d1 != 0)
        if (d2 / d1 > 0)
          eye().scale(d2 / d1);
    }
  }

  // 5. Rotate

  /**
   * Same as {@code turn(roll, pitch, yaw, Graph.inertia)}.
   *
   * @see #turn(float, float, float, float)
   */
  public void turn(float roll, float pitch, float yaw) {
    turn(roll, pitch, yaw, Graph.inertia);
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
   * Same as {@code turn(tag, roll, pitch, yaw, Graph.inertia)}.
   *
   * @see #turn(String, float, float, float, float)
   */
  public void turn(String tag, float roll, float pitch, float yaw) {
    turn(tag, roll, pitch, yaw, Graph.inertia);
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
   * Same as {@code turn(node, roll, pitch, yaw, Graph.inertia)}.
   *
   * @see #turn(Node, float, float, float, float)
   */
  public void turn(Node node, float roll, float pitch, float yaw) {
    turn(node, roll, pitch, yaw, Graph.inertia);
  }

  /**
   * Rotates the {@code node} (use null for the world) around the x-y-z screen axes according to
   * {@code roll}, {@code pitch} and {@code yaw} radians, resp., and according to {@code inertia}
   * which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   */
  public void turn(Node node, float roll, float pitch, float yaw, float inertia) {
    if (node == null || node == eye()) {
      if (is2D() && (roll != 0 || pitch != 0)) {
        roll = 0;
        pitch = 0;
        System.out.println("Warning: graph is 2D. Roll and/or pitch reset");
      }
      eye()._orbit(new Quaternion(leftHanded ? -roll : roll, pitch, leftHanded ? -yaw : yaw), center(), inertia);
      // same as:
      //Quaternion q = new Quaternion(leftHanded ? -roll : roll, pitch, leftHanded ? -yaw : yaw);
      //eye().orbit(eye().worldDisplacement(q.axis()), q.angle(), center(), inertia);
      // whereas the following doesn't work
      /*
      Quaternion q = new Quaternion(leftHanded ? -roll : roll, pitch, leftHanded ? -yaw : yaw);
      q = eye().worldDisplacement(q);
      eye().orbit(q.axis(), q.angle(), center(), inertia);
      // */
    }
    else {
      if (is2D() && (roll != 0 || pitch != 0)) {
        roll = 0;
        pitch = 0;
        System.out.println("Warning: graph is 2D. Roll and/or pitch reset");
      }
      Quaternion quaternion = new Quaternion(leftHanded ? roll : -roll, -pitch, leftHanded ? yaw : -yaw);
      node.rotate(new Quaternion(node.displacement(quaternion.axis(), eye()), quaternion.angle()), inertia);
    }
  }

  // 6. Spin

  /**
   * Same as {@code spin(pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia)}.
   *
   * @see #spin(int, int, int, int, float)
   */
  public void spin(int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia);
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
   * Same as {@code spin(tag, pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia)}.
   *
   * @see #spin(String, int, int, int, int, float)
   */
  public void spin(String tag, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(tag, pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia);
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
   * Same as {@code spin(node, pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia)}.
   *
   * @see #spin(Node, int, int, int, int, float)
   */
  public void spin(Node node, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y) {
    spin(node, pixel1X, pixel1Y, pixel2X, pixel2Y, Graph.inertia);
  }

  /**
   * Rotates the {@code node} (use null for the world) using an arcball interface, from points
   * {@code (pixel1X, pixel1Y)} to {@code (pixel2X, pixel2Y)} pixel positions. The {@code inertia}
   * controls the gesture strength and it should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   * The center of the rotation is the screen projected node origin (see {@link Node#worldPosition()}).
   * <p>
   * For implementation details refer to Shoemake 92 paper: Arcball: a user interface for specifying
   * three-dimensional orientation using a mouse.
   */
  public void spin(Node node, int pixel1X, int pixel1Y, int pixel2X, int pixel2Y, float inertia) {
    if (node == null || node == eye()) {
      float sensitivity = 1;
      Vector center = screenLocation(center());
      if (center == null)
        return;
      int centerX = (int) center.x();
      int centerY = (int) center.y();
      float px = sensitivity * (pixel1X - centerX) / (float) width();
      float py = sensitivity * (leftHanded ? (pixel1Y - centerY) : (centerY - pixel1Y)) / (float) height();
      float dx = sensitivity * (pixel2X - centerX) / (float) width();
      float dy = sensitivity * (leftHanded ? (pixel2Y - centerY) : (centerY - pixel2Y)) / (float) height();
      Vector p1 = new Vector(px, py, _projectOnBall(px, py));
      Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
      // Approximation of rotation angle should be divided by the projectOnBall size, but it is 1.0
      Vector axis = p2.cross(p1);
      // 2D is an ad-hoc
      float angle = (is2D() ? sensitivity : 2.0f) * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / (p1.squaredNorm() * p2.squaredNorm())));
      eye()._orbit(new Quaternion(axis, angle), center(), inertia);
      // same as:
      //eye().orbit(eye().worldDisplacement(axis), angle, center(), inertia);
    }
    else {
      float sensitivity = 1;
      Vector center = screenLocation(node.worldPosition());
      if (center == null)
        return;
      int centerX = (int) center.x();
      int centerY = (int) center.y();
      float px = sensitivity * (pixel1X - centerX) / (float) width();
      float py = sensitivity * (leftHanded ? (pixel1Y - centerY) : (centerY - pixel1Y)) / (float) height();
      float dx = sensitivity * (pixel2X - centerX) / (float) width();
      float dy = sensitivity * (leftHanded ? (pixel2Y - centerY) : (centerY - pixel2Y)) / (float) height();
      Vector p1 = new Vector(px, py, _projectOnBall(px, py));
      Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
      // Approximation of rotation angle should be divided by the projectOnBall size, but it is 1.0
      Vector axis = p2.cross(p1);
      // 2D is an ad-hoc
      float angle = (is2D() ? sensitivity : 2.0f) * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / (p1.squaredNorm() * p2.squaredNorm())));
      Quaternion quaternion = new Quaternion(axis, -angle);
      node.rotate(new Quaternion(node.displacement(quaternion.axis(), eye()), quaternion.angle()), inertia);
    }
  }

  /**
   * Returns "pseudo-_distance" from (x,y) to ball of radius size. For a point inside the
   * ball, it is proportional to the euclidean distance to the ball. For a point outside
   * the ball, it is proportional to the inverse of this distance (tends to zero) on the
   * ball, the function is continuous.
   */
  protected float _projectOnBall(float x, float y) {
    if (is2D())
      return 0;
    // If you change the size value, change angle computation in deformedBallQuaternion().
    float size = 1.0f;
    float size2 = size * size;
    float size_limit = size2 * 0.5f;
    float d = x * x + y * y;
    return d < size_limit ? (float) Math.sqrt(size2 - d) : size_limit / (float) Math.sqrt(d);
  }

  // only 3d eye

  // 7. move forward

  /**
   * Same as {@code moveForward(delta, Graph.inertia)}.
   *
   * @see #moveForward(float, float)
   */
  public void moveForward(float delta) {
    moveForward(delta, Graph.inertia);
  }

  /**
   * Same as {@code translateEye(0, 0, delta / (zNear() - zFar()))}. The gesture strength is controlled
   * with {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction. Also rescales
   * the {@link #eye()} if the graph type is {@link Type#ORTHOGRAPHIC} so that nearby objects
   * appear bigger when moving towards them.
   *
   * @see #shift(float, float, float)
   */
  public void moveForward(float delta, float inertia) {
    // we negate z which targets the Processing mouse wheel
    shift(eye(),0, 0, delta / (zNear() - zFar()), inertia);
  }

  // 8. lookAround

  /**
   * Same as {@code lookAround(deltaX, deltaY, Graph.inertia)}.
   */
  public void lookAround(float deltaX, float deltaY) {
    lookAround(deltaX, deltaY, Graph.inertia);
  }

  /**
   * Look around (without translating the eye) according to angular displacements {@code deltaX} and {@code deltaY}
   * expressed in radians. The gesture strength is controlled with {@code inertia} which should be in {@code [0..1]},
   * 0 no inertia & 1 no friction.
   */
  public void lookAround(float deltaX, float deltaY, float inertia) {
    if (is2D()) {
      System.out.println("Warning: lookAround is only available in 3D");
    } else {
      _lookAroundTask.setInertia(inertia);
      _lookAroundTask._x += deltaX / 5;
      _lookAroundTask._y += deltaY / 5;
      _lookAround();
      if (!_lookAroundTask.isActive())
        _lookAroundTask.run();
    }
  }

  /**
   * {@link #lookAround(float, float)}  procedure}.
   */
  protected void _lookAround() {
    if (TimingHandler.frameCount > _lookAroundCount) {
      _upVector = eye().yAxis();
      _lookAroundCount = TimingHandler.frameCount;
    }
    _lookAroundCount++;
    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), leftHanded ? _lookAroundTask._y : -_lookAroundTask._y);
    Quaternion rotY = new Quaternion(eye().displacement(_upVector), -_lookAroundTask._x);
    Quaternion quaternion = Quaternion.multiply(rotY, rotX);
    eye().rotate(quaternion);
  }

  // 9. rotate CAD

  /**
   * Same as {@code rotateCAD(roll, pitch, new Vector(0, 1, 0), Graph.inertia)}.
   *
   * @see #cad(float, float, Vector, float)
   */
  public void cad(float roll, float pitch) {
    cad(roll, pitch, new Vector(0, 1, 0), Graph.inertia);
  }

  /**
   * Same as {@code rotateCAD(roll, pitch, upVector, Graph.inertia)}.
   *
   * @see #cad(float, float, Vector, float)
   */
  public void cad(float roll, float pitch, Vector upVector) {
    cad(roll, pitch, upVector, Graph.inertia);
  }

  /**
   * Defines an axis which the eye rotates around. The eye can rotate left or right around
   * this axis. It can also be moved up or down to show the 'top' and 'bottom' views of the scene.
   * As a result, the {@code upVector} will always appear vertical in the scene, and the horizon
   * is preserved and stays projected along the eye's horizontal axis. The gesture strength is
   * controlled with {@code inertia} which should be in {@code [0..1]}, 0 no inertia & 1 no friction.
   * <p>
   * This method requires calling {@code scene.eye().setYAxis(upVector)} (see
   * {@link Node#setYAxis(Vector)}) and {@link #fit()} first.
   *
   * @see #cad(float, float)
   */
  public void cad(float roll, float pitch, Vector upVector, float inertia) {
    if (is2D()) {
      System.out.println("Warning: rotateCAD is only available in 3D");
    } else {
      _eyeUp = upVector;
      _cad();
      _cadRotateTask.setInertia(inertia);
      _cadRotateTask._x += roll;
      _cadRotateTask._y += pitch;
      if (!_cadRotateTask.isActive())
        _cadRotateTask.run();
    }
  }

  /**
   * {@link #cad(float, float, Vector, float) procedure}.
   */
  protected void _cad() {
    Vector _up = eye().displacement(_eyeUp);
    eye()._orbit(Quaternion.multiply(new Quaternion(_up, _up.y() < 0.0f ? _cadRotateTask._x : -_cadRotateTask._x), new Quaternion(new Vector(1.0f, 0.0f, 0.0f), leftHanded ? _cadRotateTask._y : -_cadRotateTask._y)), center());
  }

  // visual hints

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
   */
  public boolean isHintEnabled(int hint) {
    return ~(_mask | ~hint) == 0;
  }

  /**
   * Returns the current visual hint mask. The mask is a bitwise-or of the following
   * single visual hints available for the graph:
   * <p>
   * <ol>
   * <li>{@link #GRID} which displays a grid hint centered at the world origin.</li>
   * <li>{@link #AXES} which displays an axes hint centered at the world origin.</li>
   * <li>{@link #HUD} which displays the graph Heads-Up-Display set with
   * {@link #setHUD(PShape)} or {@link #setHUD(Consumer)}.</li>
   * <li>{@link #SHAPE} which displays the node shape set with
   * {@link #setShape(PShape)} or {@link #setShape(Consumer)}.</li>
   * <li>{@link #BACKGROUND} which sets up the graph background to be displayed.</li>
   * </ol>
   * Displaying the hint requires first to enabling it (see {@link #enableHint(int)}) and then
   * calling either {@link #render(Node)} or {@link #render()}.
   *
   * @see #enableHint(int)
   * @see #configHint(int, Object...)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #resetHint()
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
   */
  public void resetHint() {
    _mask = 0;
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
   */
  public void disableHint(int hint) {
    _mask &= ~hint;
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
   */
  public void enableHint(int hint) {
    _mask |= hint;
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
   */
  public void toggleHint(int hint) {
    _mask ^= hint;
  }

  /**
   * Configures the hint using varargs as follows:
   * <p>
   * <ol>
   * <li>{@link #GRID} hint: {@code configHint(Graph.GRID, gridStroke)},
   * {@code configHint(Graph.GRID, gridType)},
   * {@code configHint(Graph.GRID, gridStroke, gridType)},
   * {@code configHint(Graph.GRID, gridStroke, gridSubdivs)}
   * {@code configHint(Graph.GRID, gridStroke, gridSubdivs, gridType)}.</li>
   * <li>{@link #BACKGROUND} hint: {@code configHint(Graph.BACKGROUND, background)}.</li>
   * </ol>
   * Note that the {@code gridStroke}, {@code cameraStroke} and {@code frustumColor}
   * are color {@code int} vars; {@code otherGraph} is of type {@link Graph};
   * {@code gridType} is either {@link GridType#DOTS} or {@link GridType#LINES};
   * {@code gridSubdivs} is the number of grid subdivisions to be displayed; and,
   * {@code background} is either a color {@code int} var, or a
   * {@code processing.core.PImage} type.
   *
   * @see #hint()
   * @see #enableHint(int)
   * @see #enableHint(int, Object...)
   * @see #disableHint(int)
   * @see #toggleHint(int)
   * @see #isHintEnabled(int)
   * @see #resetHint()
   */
  public void configHint(int hint, Object... params) {
    switch (params.length) {
      case 1:
        if (hint == GRID) {
          if (isNumInstance(params[0])) {
            _gridStroke = castToInt(params[0]);
            return;
          }
          if (params[0] instanceof GridType) {
            _gridType = (GridType) params[0];
            return;
          }
        }
        if (hint == BACKGROUND) {
          if (isNumInstance(params[0])) {
            _background = castToInt(params[0]);
            return;
          }
          if (params[0] instanceof processing.core.PImage) {
            _background = params[0];
            return;
          }
        }
        break;
      case 2:
        if (hint == GRID) {
          if (isNumInstance(params[0]) && isNumInstance(params[1])) {
            _gridStroke = castToInt(params[0]);
            _gridSubDiv = castToInt(params[1]);
            return;
          }
          if (isNumInstance(params[0]) && params[1] instanceof GridType) {
            _gridStroke = castToInt(params[0]);
            _gridType = (GridType) params[1];
            return;
          }
          if (params[0] instanceof GridType && isNumInstance(params[1])) {
            _gridType = (GridType) params[0];
            _gridStroke = castToInt(params[1]);
            return;
          }
        }
        break;
      case 3:
        if (hint == GRID) {
          if (isNumInstance(params[0]) && isNumInstance(params[1]) && params[2] instanceof GridType) {
            _gridStroke = castToInt(params[0]);
            _gridSubDiv = castToInt(params[1]);
            _gridType = (GridType) params[2];
            return;
          }
          if (params[0] instanceof GridType && isNumInstance(params[1]) && isNumInstance(params[2])) {
            _gridType = (GridType) params[0];
            _gridStroke = castToInt(params[1]);
            _gridSubDiv = castToInt(params[2]);
            return;
          }
        }
        break;
    }
    System.out.println("Warning: some params in Scene.configHint(hint, params) couldn't be parsed!");
  }

  /**
   * Sets this (H)eads (U)p (D)isplay from the {@code graph} hud.
   */
  public void setHUD(Graph graph) {
    setHUD(graph._rmrHUD);
    setHUD(graph._imrHUD);
  }

  /**
   * Sets the graph retained mode rendering (rmr) shape {@link #HUD} hint
   * (see {@link #hint()}). Use {@code enableHint(Node.HUD)},
   * {@code disableHint(Node.HUD)} and {@code toggleHint(Node.HUD)} to (dis)enable the hint.
   *
   * @see #setHUD(Consumer)
   * @see #resetHUD()
   * @see #resetIMRHUD()
   * @see #resetRMRHUD()
   */
  public void setHUD(processing.core.PShape hud) {
    _rmrHUD = hud;
    enableHint(HUD);
  }

  /**
   * Sets the node immediate mode rendering (imr) drawing procedure
   * {@link #HUD} hint (see {@link #hint()}). Use {@code enableHint(Node.HUD)},
   * {@code disableHint(Node.HUD)} and {@code toggleHint(Node.HUD)} to (dis)enable the hint.
   *
   * @see #setShape(processing.core.PShape)
   * @see #resetHUD()
   * @see #resetIMRHUD()
   * @see #resetRMRHUD()
   */
  public void setHUD(Consumer<processing.core.PGraphics> hud) {
    _imrHUD = hud;
    enableHint(HUD);
  }

  /**
   * Same as calling {@link #resetRMRHUD()} and {@link #resetIMRHUD()}.
   */
  public void resetHUD() {
    _imrHUD = null;
    _rmrHUD = null;
    disableHint(HUD);
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
  }

  /**
   * Calls {@link #resetIMRShape()} and {@link #resetRMRShape()} .
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
    _rmrShape = shape;
    enableHint(SHAPE);
  }

  /**
   * Sets the node immediate mode rendering (imr) {@link #SHAPE} procedure
   * hint (see {@link #hint()}). Use {@code enableHint(Node.SHAPE)},
   * {@code disableHint(Node.SHAPE)} and {@code toggleHint(Node.SHAPE)}
   * to (dis)enable the hint.
   *
   * @see #setShape(PShape)
   * @see #resetShape()
   * @see #resetIMRShape()
   * @see #resetRMRShape()
   */
  public void setShape(Consumer<processing.core.PGraphics> callback) {
    _imrShape = callback;
    enableHint(SHAPE);
  }

  // Hack to hide node & interpolator hint properties

  // Graph

  protected Object _background(Graph graph) {
    return graph._background;
  }

  // Node

  protected Consumer<processing.core.PGraphics> _imrShape(Node node) {
    return node._imrShape;
  }

  protected processing.core.PShape _rmrShape(Node node) {
    return node._rmrShape;
  }

  protected int _torusColor(Node node) {
    return node._torusColor;
  }

  protected int _torusFaces(Node node) {
    return node._torusFaces;
  }

  protected HashSet<Graph> _frustumGraphs(Node node) {
    return node._frustumGraphs;
  }

  protected Node.BullsEyeShape _bullsEyeShape(Node node) {
    return node._bullsEyeShape;
  }

  protected int _bullsEyeStroke(Node node) {
    return node._bullsEyeStroke;
  }

  protected float _axesLength(Node node) {
    return node._axesLength;
  }

  protected int _cameraStroke(Node node) {
    return node._cameraStroke;
  }

  protected float _cameraLength(Node node) {
    return node._cameraLength;
  }

  protected Consumer<processing.core.PGraphics> _imrHUD(Node node) {
    return node._imrHUD;
  }

  protected processing.core.PShape _rmrHUD(Node node) {
    return node._rmrHUD;
  }

  // Interpolator

  /**
   * Used to display the interpolator in {@link #_displayHint()}.
   */
  protected int _splineStroke(Interpolator interpolator) {
    return interpolator._splineStroke;
  }

  /**
   * Used to display the interpolator in {@link #_displayHint()}.
   */
  protected int _splineWeight(Interpolator interpolator) {
    return interpolator._splineWeight;
  }

  protected List<Node> _path(Interpolator interpolator) {
    return interpolator._path();
  }
}
