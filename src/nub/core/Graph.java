/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.core;

import nub.primitives.*;
import nub.timing.Animator;
import nub.timing.TimingHandler;
import nub.timing.TimingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A 2D or 3D scene graph providing eye, input and timing handling to a raster or ray-tracing
 * renderer.
 * <h1>1. Types and dimensions</h1>
 * To set the viewing volume use {@link #setFrustum(Vector, float)} or {@link #setFrustum(Vector, Vector)}.
 * Both call {@link #setCenter(Vector)} and {@link #setRadius(float)} which defined a viewing ball
 * with {@link #center()} and {@link #radius()} parameters. See also {@link #setZClippingCoefficient(float)} and
 * {@link #setZNearCoefficient(float)} for a 3d graph.
 * <p>
 * The way the projection matrix is computed (see
 * {@link Node#projection(Type, float, float, float, float, boolean)}),
 * defines the type of the graph as: {@link Type#PERSPECTIVE}, {@link Type#ORTHOGRAPHIC}
 * for 3d graphs and {@link Type#TWO_D} for a 2d graph.
 * <h1>2. Scene graph handling</h1>
 * A graph forms a tree of (attached) {@link Node}s whose visual representations may be
 * {@link #render()}. Note that {@link #render()} should be called within your main-event loop.
 * <p>
 * The node collection belonging to the graph may be retrieved with {@link #nodes()}.
 * The graph provides other useful routines to handle the hierarchy, such as
 * {@link #pruneBranch(Node)}, {@link #appendBranch(List)}, {@link #isReachable(Node)},
 * {@link #branch(Node)}, and {@link #clear()}.
 * <h2>2.1. Eye handling</h2>
 * Any {@link Node} (belonging or not to the graph hierarchy) may be set as the {@link #eye()}
 * (see {@link #setEye(Node)}). Several node wrapper functions to handle the eye, such as
 * {@link #lookAt(Vector)}, {@link #at()}, {@link #setViewDirection(Vector)},
 * {@link #setUpVector(Vector)}, {@link #upVector()}, {@link #fitFOV()},
 * {@link #fov()}, {@link #fit()}, {@link #screenLocation(Vector, Node)} and
 * {@link #location(Vector, Node)}, are provided for convenience.
 * <h1>3. Interactivity</h1>
 * Several methods taking a {@link Node} parameter provide interactivity to nodes, such as
 * {@link #translate(float, float, float, Node)},
 * {@link #rotate(float, float, float, Node)} and {@link #scale(float, Node)}.
 * <p>
 * Some interactivity methods are only available for the {@link #eye()} and hence they don't
 * take a node parameter, such as {@link #lookAround(float, float)} or {@link #rotateCAD(float, float)}.
 * <p>
 * Call {@link #control(Node, Object...)} to send arbitrary gesture data to the node. Note that
 * {@link Node#interact(Object...)} should be overridden to implement the node custom behavior.
 * <p>
 * To check if a given node would be picked with a ray casted at a given screen position
 * use {@link #tracks(float, float, Node)}. Refer to {@link Node#pickingThreshold()} (and
 * {@link Node#setPickingThreshold(float)}) for the different node picking policies.
 * <h1>4. Human Interface Devices</h1>
 * Setting up a <a href="https://en.wikipedia.org/wiki/Human_interface_device">Human Interface Device (hid)</a>
 * is a two step process: 1. Define an {@code hid} tracked-node instance, using an arbitrary name for it
 * (see {@link #setTrackedNode(String, Node)}); and, 2. Call any interactivity method that take an {@code hid}
 * param (such as {@link #translate(String, float, float, float)}, {@link #rotate(String, float, float, float)}
 * or {@link #scale(String, float)}) following the name convention you defined in 1. Observations:
 * <ol>
 * <li>An {@code hid} tracked-node (see {@link #trackedNode(String)}) defines in turn an {@code hid} default-node
 * (see {@link #defaultNode(String)}) which simply returns the tracked-node or the {@link #eye()} when the
 * {@code hid} tracked-node is {@code null}.</li>
 * <li>The {@code hid} interactivity methods are implemented in terms of the ones defined previously
 * by simply passing the {@code hid} {@link #defaultNode(String)} to them (e.g.,
 * {@link #scale(String, float)} calls {@link #scale(float, Node)} passing the {@code hid} default-node).</li>
 * <li>The default {@code hid} is defined with a {@code null} String parameter (e.g.,
 * {@link #scale(float delta)} simply calls {@code scale(null, delta)}).</li>
 * <li>To update an {@code hid} tracked-node using ray-casting call {@link #track(String, Point, Node[])}
 * (detached or attached nodes), {@link #track(String, Point)} (only attached nodes) or
 * {@link #cast(String, Point)} (only for attached nodes too). While {@link #track(String, Point, Node[])} and
 * {@link #track(String, Point)} update the {@code hid} tracked-node synchronously (i.e., they return the
 * {@code hid} tracked-node immediately), {@link #cast(String, Point)} updates it asynchronously (i.e., it
 * optimally updates the {@code hid} tracked-node during the next call to the {@link #render()} algorithm).</li>
 * </ol>
 * <h1>5. Timing handling</h1>
 * The graph performs timing handling through a {@link #timingHandler()}. Several
 * {@link TimingHandler} wrapper functions, such as {@link #registerTask(TimingTask)}
 * and {@link #registerAnimator(Animator)}, are provided for convenience.
 * <p>
 * A default {@link #interpolator()} may perform several {@link #eye()} interpolations
 * such as {@link #fit(float)}, {@link #fit(Rectangle)},
 * {@link #fit(Node)} and {@link #fit(Node, float)}. Refer to the
 * {@link Interpolator} documentation for details.
 * <h1>6. Visibility and culling techniques</h1>
 * Geometry may be culled against the viewing volume by calling {@link #isPointVisible(Vector)},
 * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}. Make sure
 * to call {@link #enableBoundaryEquations()} first, since update of the viewing volume
 * boundary equations are disabled by default (see {@link #enableBoundaryEquations()} and
 * {@link #areBoundaryEquationsEnabled()}).
 * <h1>7. Matrix handling</h1>
 * The graph performs matrix handling through a {@link #matrixHandler()} (see also
 * {@link #setMatrixHandler(MatrixHandler)}) which should be overridden according to how your
 * renderer handles the matrix shader uniform variables. Refer to the {@link MatrixHandler}
 * documentation for details.
 * <p>
 * To apply the transformation defined by a node call {@link #applyTransformation(Node)}
 * (see also {@link #applyWorldTransformation(Node)}) between {@code pushModelView()} and
 * {@code popModelView()}. Note that the node transformations are applied automatically by
 * the {@link #render(MatrixHandler, Object)} algorithm (in this case you don't need to call them).
 * <p>
 * To define your geometry on the screen coordinate system (such as when drawing 2d controls
 * on top of a 3d graph) issue your drawing code between {@link #beginHUD()} and
 * {@link #endHUD()}. These methods are {@link MatrixHandler} wrapper functions
 * with the same signatures provided for convenience.
 * <p>
 * To bind a graph to a third party renderer override {@link MatrixHandler} and set it
 * with {@link #setMatrixHandler(MatrixHandler)} (refer to the {@link MatrixHandler}
 * documentation for details).
 *
 * @see TimingHandler
 * @see #applyTransformation(Node)
 * @see MatrixHandler
 */
public class Graph {
  // offscreen
  protected Point _upperLeftCorner;
  protected boolean _offscreen;

  // 0. Contexts
  protected Object _bb, _fb;
  // 1. Eye
  protected Node _eye;
  protected long _lastEqUpdate;
  protected Vector _center;
  protected float _radius;
  protected Vector _anchor;
  //Interpolator
  protected Interpolator _interpolator;
  //boundary eqns
  protected float _coefficients[][];
  protected boolean _coefficientsUpdate;
  protected Vector _normal[];
  protected float _distance[];
  // handed and HUD
  protected boolean _rightHanded;
  protected int _hudCalls;

  // 2. Matrix handler
  protected MatrixHandler _matrixHandler, _bbMatrixHandler;

  // 3. Handlers
  protected class Ray {
    public String _hid;
    public Point _pixel;

    Ray(String hid, Point pixel) {
      _hid = hid;
      _pixel = pixel;
    }
  }

  protected TimingHandler _timingHandler;
  protected HashMap<String, Node> _agents;
  protected ArrayList<Ray> _rays;

  // 4. Graph
  protected List<Node> _seeds;
  protected long _lastNonEyeUpdate = 0;

  // 5. Interaction methods
  Vector _upVector;
  protected long _lookAroundCount;

  /**
   * Enumerates the different visibility states an object may have respect to the eye
   * boundary.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  Type _type;

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
   * Same as {@code this(Type.PERSPECTIVE, w, h)}
   *
   * @see #Graph(Type, int, int)
   */
  public Graph(int width, int height) {
    this(Type.PERSPECTIVE, width, height);
  }

  /**
   * Default constructor defines a right-handed graph with the specified {@code width} and
   * {@code height} screen window dimensions. The graph {@link #center()} and
   * {@link #anchor()} are set to {@code (0,0,0)} and its {@link #radius()} to {@code 100}.
   * <p>
   * The constructor sets a {@link Node} instance as the graph {@link #eye()} and then
   * calls {@link #fit()}, so that the entire scene fits the screen dimensions.
   * <p>
   * The constructor also instantiates the graph {@link #matrixHandler()} and
   * {@link #timingHandler()}.
   * <p>
   * Third party graphs should additionally:
   * <ol>
   * <li>(Optionally) Define a custom {@link #matrixHandler()}. Only if the target platform
   * (such as Processing) provides its own matrix handling.</li>
   * </ol>
   *
   * @see #timingHandler()
   * @see #setMatrixHandler(MatrixHandler)
   * @see #setRightHanded()
   * @see #setEye(Node)
   */
  public Graph(Type type, int width, int height) {
    setMatrixHandler(new MatrixHandler(type != Type.TWO_D, width, height));

    _seeds = new ArrayList<Node>();
    _timingHandler = new TimingHandler();

    setFrustum(new Vector(), 100);
    setEye(new Node(this));
    setType(type);
    if (is3D())
      setFOV((float) Math.PI / 3);
    fit();

    _agents = new HashMap<String, Node>();
    _rays = new ArrayList<Ray>();
    setRightHanded();

    enableBoundaryEquations(false);

    setZNearCoefficient(0.005f);
    setZClippingCoefficient((float) Math.sqrt(3.0f));
  }

  /**
   * Same as {@code return Node.random(this)}. Creates a random node attached to this graph.
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
    node.randomize(center(), radius(), is3D());
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
    return _matrixHandler.width();
  }

  /**
   * Returns height of the screen window.
   */
  public int height() {
    return _matrixHandler.height();
  }

  /**
   * Returns graph projection matrix.
   */
  public Matrix projection() {
    return _matrixHandler.projection();
  }

  /**
   * Returns graph cached view matrix.
   */
  public Matrix view() {
    return _matrixHandler.cacheView();
  }

  /**
   * Returns the projection times view cached matrix.
   */
  public Matrix projectionView() {
    return _matrixHandler.cacheProjectionView();
  }

  /**
   * Returns the projection times modelview matrix.
   */
  public Matrix projectionModelView() {
    return _matrixHandler.projectionModelView();
  }

  /**
   * Returns the modelview matrix.
   */
  public Matrix modelView() {
    return _matrixHandler.modelView();
  }

  /**
   * Sets the graph {@link #width()} in pixels.
   */
  public void setWidth(int width) {
    if ((width != width())) {
      _matrixHandler.setWidth(width);
      _modified();
    }
  }

  /**
   * Sets the graph {@link #height()} in pixels.
   */
  public void setHeight(int height) {
    if ((height != height())) {
      _matrixHandler.setWidth(height);
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
   * Defines the graph {@link #type()} according to the projection of the scene.
   * Either {@link Type#PERSPECTIVE}, {@link Type#ORTHOGRAPHIC}, {@link Type#TWO_D}
   * or {@link Type#CUSTOM}.
   * <p>
   * {@link Type#PERSPECTIVE} and {@link Type#ORTHOGRAPHIC} use the classical projection
   * matrices and the node {@link Node#magnitude()}. Both use {@link #zNear()} and
   * {@link #zFar()} (to define their clipping planes) and {@link #width()} and {@link #height()}
   * for frustum shape.
   * <p>
   * A {@link Type#TWO_D} behaves like {@link Type#ORTHOGRAPHIC}, but instantiated graph
   * nodes will be constrained so that they will remain at the x-y plane. See
   * {@link nub.core.constraint.Constraint}.
   *
   * @see Node#projection(Type, float, float, float, float, boolean)
   * @see Node#magnitude()
   */
  public void setType(Type type) {
    if (type != type()) {
      _modified();
      this._type = type;
    }
  }

  /**
   * Shifts the graph {@link #type()} between {@link Type#PERSPECTIVE} and {@link Type#ORTHOGRAPHIC} while trying
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
      setType(type() == Type.PERSPECTIVE ? Type.ORTHOGRAPHIC : Type.PERSPECTIVE);
      setFOV(fov);
    }
  }

  /**
   * Sets the {@link #eye()} {@link Node#magnitude()} (which is used to compute the
   * {@link Node#projection(Type, float, float, float, float, boolean)} matrix),
   * according to {@code fov} (field-of-view) which is expressed in radians. Meaningless
   * if the graph {@link #is2D()}. If the graph {@link #type()} is {@link Type#ORTHOGRAPHIC}
   * it will match the perspective projection obtained using {@code fov} of an image
   * centered at the world XY plane from the eye current position.
   * <p>
   * Computed as as {@code Math.tan(fov/2)} if the graph type is {@link Type#PERSPECTIVE} and as
   * {@code Math.tan(fov / 2) * 2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())) / width()}
   * if the graph {@link #type()} is {@link Type#ORTHOGRAPHIC}.
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
    eye().setMagnitude(type() == Type.PERSPECTIVE ?
        (float) Math.tan(fov / 2) :
        (float) Math.tan(fov / 2) * 2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())) / width());
  }

  /**
   * Retrieves the graph field-of-view in radians. Meaningless if the graph {@link #is2D()}.
   * See {@link #setFOV(float)} for details. The value is related to the {@link #eye()}
   * {@link Node#magnitude()} (which in turn is used to compute the
   * {@link Node#projection(Type, float, float, float, float, boolean)} matrix) as follows:
   * <p>
   * <ol>
   * <li>It returns {@code 2 * Math.atan(eye().magnitude())}, when the
   * graph {@link #type()} is {@link Type#PERSPECTIVE}.</li>
   * <li>It returns {@code 2 * Math.atan(eye().magnitude() * width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()))))},
   * if the graph {@link #type()} is {@link Type#ORTHOGRAPHIC}.</li>
   * </ol>
   * Set this value with {@link #setFOV(float)} or {@link #setHFOV(float)}.
   *
   * @see Node#magnitude()
   * @see Node#perspective(float, float, float, boolean)
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
    return type() == Type.PERSPECTIVE ?
        2 * (float) Math.atan(eye().magnitude()) :
        2 * (float) Math.atan(eye().magnitude() * width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()))));
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
   * Returns the {@link #eye()} horizontal field-of-view in radians if the graph {@link #type()} is
   * {@link Type#PERSPECTIVE}, or the {@link #eye()} {@link Node#magnitude()} otherwise.
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
    return type() == Type.PERSPECTIVE ?
        2 * (float) Math.atan(eye().magnitude() * aspectRatio()) :
        2 * (float) Math.atan(eye().magnitude() * aspectRatio() * width() / (2 * Math.abs(Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()))));
  }

  /**
   * Returns the near clipping plane distance used by the eye node
   * {@link Node#projection(Type, float, float, float, float, boolean)} matrix in
   * world units.
   * <p>
   * The clipping planes' positions depend on the {@link #radius()} and {@link #center()}
   * rather than being fixed small-enough and large-enough values. A good approximation will
   * hence result in an optimal precision of the z-buffer.
   * <p>
   * The near clipping plane is positioned at a distance equal to
   * {@link #zClippingCoefficient()} * {@link #radius()} in front of the
   * {@link #center()}: {@code Vector.scalarProjection(
   * Vector.subtract(eye().position(), center()), eye().zAxis()) - zClippingCoefficient() * radius()}
   * <p>
   * In order to prevent negative or too small {@link #zNear()} values (which would
   * degrade the z precision), {@link #zNearCoefficient()} is used when the eye is
   * inside the {@link #radius()} ball:
   * <p>
   * {@code zMin = zNearCoefficient() * zClippingCoefficient() * radius();} <br>
   * {@code zNear = zMin;}<br>
   * {@code With an ORTHOGRAPHIC and TWO_D types, the value is simply clamped to 0}<br>
   * <p>
   * See also the {@link #zFar()}, {@link #zClippingCoefficient()} and
   * {@link #zNearCoefficient()} documentations.
   * <p>
   * If you need a completely different zNear computation, overload the {@link #zNear()}
   * and {@link #zFar()} methods.
   *
   * <b>Attention:</b> The value is always positive, although the clipping plane is
   * positioned at a negative z value in the eye coordinate system.
   *
   * @see #zFar()
   */
  public float zNear() {
    float z = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) - zClippingCoefficient() * radius();

    // Prevents negative or null zNear values.
    float zMin = zNearCoefficient() * zClippingCoefficient() * radius();
    if (z < zMin)
      switch (type()) {
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
   * Returns the far clipping plane distance used by the eye node
   * {@link Node#projection(Type, float, float, float, float, boolean)} matrix in world units.
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
    return Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) + zClippingCoefficient() * radius();
  }

  /**
   * Returns the coefficient used to set {@link #zNear()} when the {@link #eye()} is
   * inside the ball defined by {@link #center()} and {@link #zClippingCoefficient()} * {@link #radius()}.
   * <p>
   * In that case, the {@link #zNear()} value is set to
   * {@code zNearCoefficient() * zClippingCoefficient() * radius()}. See the
   * {@code zNear()} documentation for details.
   * <p>
   * Default value is 0.005, which is appropriate for most applications. In case you need
   * a high dynamic ZBuffer precision, you can increase this value (~0.1). A lower value
   * will prevent clipping of very close objects at the expense of a worst Z precision.
   * <p>
   * Only meaningful when the graph type is PERSPECTIVE.
   */
  public float zNearCoefficient() {
    return _zNearCoefficient;
  }

  /**
   * Sets the {@link #zNearCoefficient()} value.
   */
  public void setZNearCoefficient(float coef) {
    if (coef != _zNearCoefficient)
      _modified();
    _zNearCoefficient = coef;
  }

  /**
   * Returns the coefficient used to position the near and far clipping planes.
   * <p>
   * The near (resp. far) clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * radius()} in front of (resp. behind) the
   * {@link #center()}. This guarantees an optimal use of the z-buffer range and
   * minimizes aliasing. See the {@link #zNear()} and {@link #zFar()} documentations.
   * <p>
   * Default value is square root of 3 (so that a cube of edge size 2*{@link #radius()}
   * is not clipped).
   *
   * @see #zNearCoefficient()
   */
  public float zClippingCoefficient() {
    return _zClippingCoefficient;
  }

  /**
   * Sets the {@link #zClippingCoefficient()} value.
   */
  public void setZClippingCoefficient(float coef) {
    if (coef != _zClippingCoefficient)
      _modified();
    _zClippingCoefficient = coef;
  }

  // Graph and nodes stuff

  /**
   * Returns the top-level nodes (those which reference is null).
   * <p>
   * All leading nodes are also reachable by the {@link #render()} algorithm for which they are the seeds.
   *
   * @see #nodes()
   * @see #isReachable(Node)
   * @see #pruneBranch(Node)
   */
  protected List<Node> _leadingNodes() {
    return _seeds;
  }

  /**
   * Returns {@code true} if the node is top-level.
   */
  protected boolean _isLeadingNode(Node node) {
    for (Node leadingNode : _leadingNodes())
      if (leadingNode == node)
        return true;
    return false;
  }

  /**
   * Transfers the graph nodes to the {@code target} graph. Useful to display auxiliary
   * viewers of the main graph. Use it in your drawing code such as:
   * <p>
   * <pre>
   * {@code
   * Graph graph graph, auxiliaryGraph;
   * void draw() {
   *   graph.render();
   *   // shift nodes to the auxiliaryGraph
   *   scene.shift(auxiliaryGraph);
   *   auxiliaryGraph.render();
   *   // shift nodes back to the main graph
   *   auxiliaryGraph.shift(graph);
   * }
   * }
   * </pre>
   */
  public void shift(Graph target) {
    for (Node leadingNode : _leadingNodes()) {
      leadingNode._graph = target;
      target._addLeadingNode(leadingNode);
    }
  }

  /**
   * Add the node as top-level if its reference node is null and it isn't already added.
   */
  protected boolean _addLeadingNode(Node node) {
    if (node == null || node.reference() != null)
      return false;
    if (_isLeadingNode(node))
      return false;
    return _leadingNodes().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected boolean _removeLeadingNode(Node node) {
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
   * @see #pruneBranch(Node)
   */
  public void clear() {
    for (Node node : _leadingNodes())
      pruneBranch(node);
  }

  /**
   * Make all the nodes in the {@code node} branch eligible for garbage collection.
   * <p>
   * A call to {@link #isReachable(Node)} on all {@code node} descendants
   * (including {@code node}) will return false, after issuing this method. It also means
   * that all nodes in the {@code node} branch will become unreachable by the
   * {@link #render()} algorithm.
   * <p>
   * To make all the nodes in the branch reachable again, first cache the nodes
   * belonging to the branch (i.e., {@code branch=pruneBranch(node)}) and then call
   * {@link #appendBranch(List)} on the cached branch. Note that calling
   * {@link Node#setReference(Node)} on a node belonging to the pruned branch will become
   * reachable again by the traversal algorithm.
   * <p>
   * When collected, pruned nodes behave like {@link Node}, otherwise they are eligible for
   * garbage collection.
   *
   * @see #clear()
   * @see #appendBranch(List)
   * @see #isReachable(Node)
   */
  public List<Node> pruneBranch(Node node) {
    if (!isReachable(node))
      return new ArrayList<Node>();
    ArrayList<Node> list = new ArrayList<Node>();
    _collect(list, node);
    for (Node collectedNode : list)
      if (collectedNode.reference() != null)
        collectedNode.reference()._removeChild(collectedNode);
      else
        _removeLeadingNode(collectedNode);
    return list;
  }

  /**
   * Appends the branch which typically should come from the one pruned (and cached) with
   * {@link #pruneBranch(Node)}.
   * <p>
   * {@link #pruneBranch(Node)}
   */
  public void appendBranch(List<Node> branch) {
    if (branch == null)
      return;
    for (Node node : branch)
      if (node.reference() != null)
        node.reference()._addChild(node);
      else
        _addLeadingNode(node);
  }

  /**
   * Returns {@code true} if the node is reachable by the {@link #render()}
   * algorithm and {@code false} otherwise.
   * <p>
   * Nodes are made unreachable with {@link #pruneBranch(Node)} and reachable
   * again with {@link Node#setReference(Node)}.
   *
   * @see #render()
   * @see #nodes()
   */
  public boolean isReachable(Node node) {
    if (node == null)
      return false;
    return node.isAttached(this);
  }

  /**
   * Returns a list of all the nodes that are reachable by the {@link #render()}
   * algorithm.
   * <p>
   * The method render the hierarchy to collect. Node collections should thus be kept at user space
   * for efficiency.
   *
   * @see #isReachable(Node)
   * @see #isEye(Node)
   */
  public List<Node> nodes() {
    ArrayList<Node> list = new ArrayList<Node>();
    for (Node node : _leadingNodes())
      _collect(list, node);
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isReachable(Node)
   */
  public List<Node> branch(Node node) {
    ArrayList<Node> list = new ArrayList<Node>();
    _collect(list, node);
    return list;
  }

  /**
   * Returns a straight path of nodes between {@code tail} and {@code tip}. Returns an empty list
   * if either {@code tail} or {@code tip} aren't reachable. Use {@link Node#path(Node, Node)}
   * to include all nodes even if they aren't reachable.
   * <p>
   * If {@code tail} is ancestor of {@code tip} the returned list will include both of them.
   * Otherwise it will be empty.
   *
   * @see #isReachable(Node)
   * @see Node#path(Node, Node)
   */
  public List<Node> path(Node tail, Node tip) {
    return (isReachable(tail) && isReachable(tip)) ? Node.path(tail, tip) : new ArrayList<Node>();
  }

  /**
   * Collects {@code node} and all its descendant nodes. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isReachable(Node)
   */
  protected void _collect(List<Node> list, Node node) {
    if (node == null)
      return;
    list.add(node);
    for (Node child : node.children())
      _collect(list, child);
  }

  // Timing stuff

  /**
   * Returns the graph {@link TimingHandler}.
   */
  public TimingHandler timingHandler() {
    return _timingHandler;
  }

  /**
   * Returns the number of nodes displayed since the graph was instantiated.
   * <p>
   * Use {@code TimingHandler.frameCount} to retrieve the number of nodes displayed since
   * the first graph was instantiated.
   */
  public long frameCount() {
    return timingHandler().frameCount();
  }

  /**
   * Convenience wrapper function that simply calls {@code timingHandler().registerTask(task)}.
   *
   * @see TimingHandler#registerTask(TimingTask)
   */
  public void registerTask(TimingTask task) {
    timingHandler().registerTask(task);
  }

  /**
   * Convenience wrapper function that simply calls {@code timingHandler().unregisterTask(task)}.
   */
  public void unregisterTask(TimingTask task) {
    timingHandler().unregisterTask(task);
  }

  /**
   * Convenience wrapper function that simply returns {@code timingHandler().isTaskRegistered(task)}.
   */
  public boolean isTaskRegistered(TimingTask task) {
    return timingHandler().isTaskRegistered(task);
  }

  /**
   * Convenience wrapper function that simply calls {@code timingHandler().registerAnimator(animator)}.
   */
  public void registerAnimator(Animator animator) {
    timingHandler().registerAnimator(animator);
  }

  /**
   * Convenience wrapper function that simply calls {@code timingHandler().unregisterAnimator(animator)}.
   *
   * @see TimingHandler#unregisterAnimator(Animator)
   */
  public void unregisterAnimator(Animator animator) {
    timingHandler().unregisterAnimator(animator);
  }

  /**
   * Convenience wrapper function that simply returns {@code timingHandler().isAnimatorRegistered(animator)}.
   *
   * @see TimingHandler#isAnimatorRegistered(Animator)
   */
  public boolean isAnimatorRegistered(Animator animator) {
    return timingHandler().isAnimatorRegistered(animator);
  }

  // Matrix and transformations stuff

  /**
   * Wrapper for {@link MatrixHandler#beginHUD()}. Adds exception when no properly
   * closing the screen drawing with a call to {@link #endHUD()}.
   *
   * @see MatrixHandler#beginHUD()
   */
  public void beginHUD() {
    if (_hudCalls != 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    _hudCalls++;
    _matrixHandler.beginHUD();
  }

  /**
   * Wrapper for {@link MatrixHandler#endHUD()} . Adds exception
   * if {@link #beginHUD()} wasn't properly called before
   *
   * @see MatrixHandler#endHUD()
   */
  public void endHUD() {
    _hudCalls--;
    if (_hudCalls != 0)
      throw new RuntimeException("There should be exactly one beginHUD() call followed by a "
          + "endHUD() and they cannot be nested. Check your implementation!");
    _matrixHandler.endHUD();
  }

  /**
   * Sets the {@link MatrixHandler} defining how matrices are to be handled.
   *
   * @see #matrixHandler()
   */
  public void setMatrixHandler(MatrixHandler matrixHandler) {
    _matrixHandler = matrixHandler;
  }

  /**
   * Returns the {@link MatrixHandler}.
   *
   * @see #setMatrixHandler(MatrixHandler)
   */
  public MatrixHandler matrixHandler() {
    return _matrixHandler;
  }

  /**
   * Wrapper for {@link MatrixHandler#isProjectionViewInverseCached()}.
   * <p>
   * Use it only when continuously calling {@link #location(Vector)}.
   *
   * @see #cacheProjectionViewInverse(boolean)
   * @see #location(Vector)
   */
  public boolean isProjectionViewInverseCached() {
    return _matrixHandler.isProjectionViewInverseCached();
  }

  /**
   * Wrapper for {@link MatrixHandler#cacheProjectionViewInverse(boolean)}.
   * <p>
   * Use it only when continuously calling {@link #location(Vector)}.
   *
   * @see #isProjectionViewInverseCached()
   * @see #location(Vector)
   */
  public void cacheProjectionViewInverse(boolean optimise) {
    _matrixHandler.cacheProjectionViewInverse(optimise);
  }

  // Eye stuff

  /**
   * Checks wheter or not the given node is the {@link #eye()}.
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
    //TODO experimental
    pruneBranch(_eye);
    _eye = eye;
    if (_interpolator == null)
      _interpolator = new Interpolator(this, _eye);
    else
      _interpolator.setNode(_eye);
    _modified();
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
   * Same as {@code return isPointVisible(new Vector(x, y, z))}.
   *
   * @see #isPointVisible(Vector)
   */
  public boolean isPointVisible(float x, float y, float z) {
    return isPointVisible(new Vector(x, y, z));
  }

  /**
   * Returns {@code true} if {@code point} is visible (i.e, lies within the eye boundary)
   * and {@code false} otherwise.
   *
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling {@link #updateBoundaryEquations()})
   * or enable them to be automatic updated in your graph setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public boolean isPointVisible(Vector point) {
    if (!areBoundaryEquationsEnabled())
      throw new RuntimeException("The frustum plane equations (needed by isPointVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    for (int i = 0; i < (is3D() ? 6 : 4); ++i)
      if (distanceToBoundary(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Returns {@link Visibility#VISIBLE}, {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the ball (of radius {@code radius}
   * and center {@code center}) is visible, invisible, or semi-visible, respectively.
   *
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #updateBoundaryEquations()} ) or enable them to be automatic updated in your
   * graph setup (with {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #boxVisibility(Vector, Vector)
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility ballVisibility(Vector center, float radius) {
    if (!areBoundaryEquationsEnabled())
      throw new RuntimeException("The frustum plane equations (needed by ballVisibility) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < (is3D() ? 6 : 4); ++i) {
      float d = distanceToBoundary(i, center);
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
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #updateBoundaryEquations()} ) or enable them to be automatic updated in your
   * graph setup (with {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility boxVisibility(Vector corner1, Vector corner2) {
    if (!areBoundaryEquationsEnabled())
      throw new RuntimeException("The frustum plane equations (needed by boxVisibility) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < (is3D() ? 6 : 4); ++i) {
      boolean allOut = true;
      for (int c = 0; c < 8; ++c) {
        Vector pos = new Vector(((c & 4) != 0) ? corner1._vector[0] : corner2._vector[0], ((c & 2) != 0) ? corner1._vector[1] : corner2._vector[1],
            ((c & 1) != 0) ? corner1._vector[2] : corner2._vector[2]);
        if (distanceToBoundary(i, pos) > 0.0)
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
   * Returns the 4 or 6 plane equations of the eye boundary.
   * <p>
   * In 2D the four 4-component vectors, respectively correspond to the
   * left, right, top and bottom eye boundary lines. Each vector holds a plane equation
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
   *
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link Graph#enableBoundaryEquations()} which automatically update the frustum equations
   * every node instead.
   */
  public float[][] updateBoundaryEquations() {
    _initCoefficients();
    return is3D() ? _updateBoundaryEquations3() : _updateBoundaryEquations2();
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

  protected float[][] _updateBoundaryEquations3() {
    // Computed once and for all
    Vector pos = eye().position();
    Vector viewDir = viewDirection();
    Vector up = upVector();
    Vector right = rightVector();

    float posViewDir = Vector.dot(pos, viewDir);

    switch (type()) {
      case PERSPECTIVE: {
        // horizontal fov: radians(eye().magnitude() * aspectRatio())
        float hhfov = 2 * (float) Math.atan(eye().magnitude() * aspectRatio()) / 2.0f;
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

        float wh0 = eye().magnitude() * width() / 2;
        float wh1 = eye().magnitude() * height() / 2;
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

    return _coefficients;
  }

  protected float[][] _updateBoundaryEquations2() {
    // Computed once and for all
    Vector pos = eye().position();
    Vector up = upVector();
    Vector right = rightVector();

    _normal[0] = Vector.multiply(right, -1);
    _normal[1] = right;
    _normal[2] = up;
    _normal[3] = Vector.multiply(up, -1);

    float wh0 = eye().magnitude() * width() / 2;
    float wh1 = eye().magnitude() * height() / 2;
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

    return _coefficients;
  }

  /**
   * Disables automatic update of the frustum plane equations every node.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #enableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see #updateBoundaryEquations()
   */
  public void disableBoundaryEquations() {
    enableBoundaryEquations(false);
  }

  /**
   * Enables automatic update of the frustum plane equations every node.
   * Computation of the equations is expensive and hence is disabled by default.
   *
   * @see #areBoundaryEquationsEnabled()
   * @see #disableBoundaryEquations()
   * @see #enableBoundaryEquations(boolean)
   * @see #updateBoundaryEquations()
   */
  public void enableBoundaryEquations() {
    enableBoundaryEquations(true);
  }

  /**
   * Enables or disables automatic update of the eye boundary plane equations every node
   * according to {@code flag}. Computation of the equations is expensive and hence is
   * disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public void enableBoundaryEquations(boolean flag) {
    _coefficientsUpdate = flag;
  }

  /**
   * Returns {@code true} if automatic update of the eye boundary plane equations is
   * enabled and {@code false} otherwise. Computation of the equations is expensive and
   * hence is disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public boolean areBoundaryEquationsEnabled() {
    return _coefficientsUpdate;
  }

  /**
   * Returns the boundary plane equations.
   * <p>
   * In 2D the four 4-component vectors, respectively correspond to the
   * left, right, top and bottom eye boundary lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * In 3D the six 4-component vectors returned by this method, respectively correspond to the
   * left, right, near, far, top and bottom eye boundary planes. Each vector holds a plane
   * equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   *
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #updateBoundaryEquations()}) or enable them to be automatic updated in your
   * graph setup (with {@link #enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #updateBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float[][] boundaryEquations() {
    if (!areBoundaryEquationsEnabled())
      throw new RuntimeException("The graph boundary equations may be outdated. Please "
          + "enable automatic updates of the equations in your setup with enableBoundaryEquations()");
    return _coefficients;
  }

  /**
   * Returns the signed distance between point {@code position} and plane {@code index}
   * in world units. The distance is negative if the point lies in the planes's boundary
   * halfspace, and positive otherwise.
   * <p>
   * In 2D {@code index} is a value between {@code 0} and {@code 3} which respectively
   * correspond to the left, right, top and bottom eye boundary planes.
   * <p>
   * In 3D {@code index} is a value between {@code 0} and {@code 5} which respectively
   * correspond to the left, right, near, far, top and bottom eye boundary planes.
   *
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #updateBoundaryEquations()}) or enable them to be automatic updated in your
   * graph setup (with {@link #enableBoundaryEquations()}).
   *
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float distanceToBoundary(int index, Vector position) {
    if (!areBoundaryEquationsEnabled())
      throw new RuntimeException("The viewpoint boundary equations (needed by distanceToBoundary) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    Vector myVector = new Vector(_coefficients[index][0], _coefficients[index][1], _coefficients[index][2]);
    if (is3D())
      return Vector.dot(position, myVector) - _coefficients[index][3];
    else
      return (_coefficients[index][0] * position.x() + _coefficients[index][1] * position.y() + _coefficients[index][2])
          / (float) Math
          .sqrt(_coefficients[index][0] * _coefficients[index][0] + _coefficients[index][1] * _coefficients[index][1]);
  }

  /**
   * Returns the pixel to graph (units) ratio at {@code position}.
   * <p>
   * Convenience function that simply returns {@code 1 / graphToPixelRatio(position)}.
   *
   * @see #graphToPixelRatio(Vector)
   */
  public float pixelToGraphRatio(Vector position) {
    return 1 / graphToPixelRatio(position);
  }

  /**
   * Returns the ratio of graph (units) to pixel at {@code position}.
   * <p>
   * A line of {@code n * graphToPixelRatio()} graph units, located at {@code position} in
   * the world coordinates system, will be projected with a length of {@code n} pixels on
   * screen.
   * <p>
   * Use this method to scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #center()} and
   * always directed along the screen vertical direction:
   * <p>
   * {@code beginShape(LINES);}<br>
   * {@code vertex(sceneCenter().x, sceneCenter().y, sceneCenter().z);}<br>
   * {@code Vector v = Vector.add(sceneCenter(), Vector.mult(upVector(), 20 * graphToPixelRatio(sceneCenter())));}
   * <br>
   * {@code vertex(v.x, v.y, v.z);}<br>
   * {@code endShape();}<br>
   */
  public float graphToPixelRatio(Vector position) {
    switch (type()) {
      case PERSPECTIVE:
        return 2.0f * Math.abs((eye().location(position))._vector[2] * eye().magnitude()) * (float) Math
            .tan(fov() / 2.0f) / height();
      case TWO_D:
      case ORTHOGRAPHIC:
        return eye().magnitude();
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
   * Vertices must given in clockwise order if {@link Graph#isLeftHanded()} or in counter-clockwise
   * order if {@link Graph#isRightHanded()}.
   *
   * @param a first face vertex
   * @param b second face vertex
   * @param c third face vertex
   * @see #isFaceBackFacing(Vector, Vector)
   * @see #isConeBackFacing(Vector, Vector, float)
   */
  public boolean isFaceBackFacing(Vector a, Vector b, Vector c) {
    return isFaceBackFacing(a, isLeftHanded() ?
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
    if (type() == Type.ORTHOGRAPHIC)
      camAxis = viewDirection();
    else {
      camAxis = Vector.subtract(vertex, eye().position());
      if (angle != 0)
        camAxis.normalize();
    }
    if (angle == 0)
      return Vector.dot(camAxis, axis) >= 0;
    float absAngle = Math.abs(angle);
    if (absAngle >= Math.PI / 2)
      return true;
    Vector faceNormal = axis.get();
    faceNormal.normalize();
    return Math.acos(Vector.dot(camAxis, faceNormal)) + absAngle < Math.PI / 2;
  }

  /**
   * Returns the radius of the graph observed by the eye in world units.
   * <p>
   * In 3D you need to provide such an approximation of the graph dimensions so
   * that the it can adapt its {@link #zNear()} and {@link #zFar()} values. See the {@link #center()}
   * documentation.
   * <p>
   * Note that {@link Graph#radius()} (resp. {@link Graph#setRadius(float)} simply call this
   * method on its associated eye.
   *
   * @see #setFrustum(Vector, Vector)
   */
  public float radius() {
    return _radius;
  }

  /**
   * Returns the position of the graph center, defined in the world coordinate system.
   * <p>
   * The graph observed by the eye should be roughly centered on this position, and
   * included in a {@link #radius()} ball.
   * <p>
   * Default value is the world origin. Use {@link #setCenter(Vector)} to change it.
   *
   * @see #setCenter(Vector)
   * @see #setRadius(float)
   * @see #setFrustum(Vector, Vector)
   * @see #zNear()
   * @see #zFar()
   */
  public Vector center() {
    return _center;
  }

  /**
   * The point the eye revolves around with a ROTATE gesture. Defined in world
   * coordinate system.
   * <p>
   * Default value is the {@link #center()}. Use {@link #setAnchor(Vector)} to change it.
   * <p>
   *
   * @see #setAnchor(Vector)
   */
  public Vector anchor() {
    return _anchor;
  }

  /**
   * Sets the {@link #anchor()}, defined in the world coordinate system.
   */
  public void setAnchor(Vector anchor) {
    _anchor = anchor;
  }

  /**
   * Sets the {@link #radius()} value in world units.
   *
   * @see #setCenter(Vector)
   */
  public void setRadius(float radius) {
    _radius = radius;
  }

  /**
   * Sets the {@link #center()} of the graph.
   *
   * @see #setRadius(float)
   */
  public void setCenter(Vector center) {
    _center = center;
  }

  /**
   * Same as {@code setCenter(center); setRadius(radius)}.
   *
   * @see #setCenter(Vector)
   * @see #setRadius(float)
   * @see #setFrustum(Vector, Vector)
   */
  public void setFrustum(Vector center, float radius) {
    setCenter(center);
    setAnchor(center);
    setRadius(radius);
  }

  /**
   * Similar to {@link #setRadius(float)} and {@link #setCenter(Vector)}, but the
   * graph limits are defined by a world axis aligned bounding box.
   *
   * @see #setFrustum(Vector, float)
   */
  public void setFrustum(Vector corner1, Vector corner2) {
    setFrustum(Vector.multiply(Vector.add(corner1, corner2), 1 / 2.0f), 0.5f * (Vector.subtract(corner2, corner1)).magnitude());
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #eye()}
   * ({@code node().worldDisplacement(new Vector(0.0f, 0.0f, -1.0f))}). In 2D
   * it always is (0,0,-1).
   * <p>
   * Change this value using {@link #setViewDirection(Vector)}, {@link #lookAt(Vector)} or
   * {@link Node#setOrientation(Quaternion)}. It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    return eye().zAxis(false);
  }

  /**
   * Rotates the eye so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The eye {@link Node#position()} is not modified. The eye is rotated so that the
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
    eye().setOrientation(q);
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
   * compensated by a translation, so that the {@link #anchor()} stays projected at the
   * same position on screen. This is especially useful when the eye is an observer of the
   * graph (default action binding).
   * <p>
   * When {@code noMove} is true, the Eye {@link Node#position()} is left unchanged, which is
   * an intuitive behavior when the Eye is in first person mode.
   *
   * @see #lookAt(Vector)
   */
  public void setUpVector(Vector up, boolean noMove) {
    Quaternion q = new Quaternion(new Vector(0.0f, 1.0f, 0.0f), eye().displacement(up));
    if (!noMove)
      eye().setPosition(Vector.subtract(anchor(), (Quaternion.multiply(eye().orientation(), q)).rotate(eye().location(anchor()))));
    eye().rotate(q);
  }

  /**
   * Returns the normalized up vector of the eye, defined in the world coordinate system.
   * <p>
   * Set using {@link #setUpVector(Vector)} or {@link Node#setOrientation(Quaternion)}. It is
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
   * eyes set {@link Node#orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The eye {@link Node#position()} is not modified.
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
      eye().setPosition(target.x(), target.y());
    else
      setViewDirection(Vector.subtract(target, eye().position()));
  }

  /**
   * Returns the normalized right vector of the eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}. Set using
   * {@link #setUpVector(Vector)}, {@link #lookAt(Vector)} or {@link Node#setOrientation(Quaternion)}.
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
    return Vector.add(eye().position(), viewDirection());
  }

  /**
   * Returns the {@link #eye()} {@link Interpolator} used by {@link #fit(float)},
   * {@link #fit(Rectangle)}, {@link #fit(Node)}, etc.
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
   * @see #fit(Rectangle, float)
   * @see #fit(float)
   * @see #fit(Rectangle)
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
   * Smoothly interpolates the eye on a interpolator path so that it goes to {@code node}
   * which is defined in world coordinates. The {@code duration} defines the interpolation speed.
   *
   * @see #fit(Node)
   * @see #fit(Vector, float)
   * @see #fit(Vector, Vector)
   * @see #fit(Rectangle, float)
   * @see #fit(float)
   * @see #fit(Rectangle)
   * @see #fit()
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Node node, float duration) {
    if (duration <= 0)
      eye().set(node);
    else {
      _interpolator.stop();
      _interpolator.purge();
      _interpolator.addKeyFrame(eye().detach());
      _interpolator.addKeyFrame(node.detach(), duration);
      _interpolator.start();
    }
  }

  /**
   * Same as {@code fitBall(center(), radius())}.
   *
   * @see #center()
   * @see #radius()
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(Rectangle, float)
   * @see #fit(float)
   * @see #fit(Rectangle)
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit() {
    fit(center(), radius());
  }

  /**
   * Same as {@code fitBall(center(), radius(), duration)}.
   *
   * @see #center()
   * @see #radius()
   * @see #fit(Vector, float, float)
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(float duration) {
    fit(center(), radius(), duration);
  }

  /**
   * Moves the eye during {@code duration} seconds so that the ball defined by {@code center}
   * and {@code radius} is visible and fits the window.
   * <p>
   * In 3D the eye is simply translated along its {@link #viewDirection()} so that the
   * ball fits the screen. Its {@link Node#orientation()} and its
   * {@link #fov()} are unchanged. You should therefore orientate the eye
   * before you call this method.
   *
   * @see #fit(float)
   * @see #fit(Vector, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector center, float radius, float duration) {
    if (duration <= 0)
      fit(center, radius);
    else {
      _interpolator.stop();
      _interpolator.purge();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(center, radius);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.start();
    }
  }

  /**
   * Moves the eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   *
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Vector, Vector)
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, Vector, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector center, float radius) {
    switch (type()) {
      case TWO_D:
        //TODO test 2d case since I swapped the calling order with the above lookAt
        lookAt(center);
        fitFOV();
        break;
      case ORTHOGRAPHIC:
        float distance = Vector.dot(Vector.subtract(center, anchor()), viewDirection()) + (radius / eye().magnitude());
        eye().setPosition(Vector.subtract(center, Vector.multiply(viewDirection(), distance)));
        fitFOV();
        break;
      case PERSPECTIVE:
        float yview = radius / (float) Math.sin(fov() / 2.0f);
        // horizontal fov: radians(eye().magnitude() * aspectRatio())
        float xview = radius / (float) Math.sin(2 * (float) Math.atan(eye().magnitude() * aspectRatio()) / 2.0f);
        eye().setPosition(Vector.subtract(center, Vector.multiply(viewDirection(), Math.max(xview, yview))));
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
   *
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
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, Vector, float)
   */
  public void fitFOV(float duration) {
    if (duration <= 0)
      fitFOV();
    else {
      _interpolator.stop();
      _interpolator.purge();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fitFOV();
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.start();
    }
  }

  /**
   * Changes the {@link #eye()} {@link #fov()} so that the entire scene
   * (defined by {@link #center()} and {@link #radius()}) is visible.
   * <p>
   * The eye position and orientation are not modified and you first have to orientate
   * the eye in order to actually see the scene (see {@link #lookAt(Vector)},
   * {@link #fit()} or {@link #fit(Vector, float)}).
   *
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
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, Vector, float)
   */
  public void fitFOV() {
    float distance = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis());
    float magnitude = distance < (float) Math.sqrt(2) * radius() ? ((float) Math.PI / 2) : 2 * (float) Math.asin(radius() / distance);
    switch (type()) {
      case PERSPECTIVE:
        setFOV(magnitude);
        break;
      case ORTHOGRAPHIC:
        eye().setMagnitude(distance < (float) Math.sqrt(2) * radius() ? 2 * radius() / Math.min(width(), height()) : 2 * (float) Math.sin(magnitude) * distance / width());
      case TWO_D:
        eye().setMagnitude(2 * radius() / Math.min(width(), height()));
        break;
    }
  }

  /**
   * Smoothly moves the eye during {@code duration} seconds so that the world axis aligned
   * box defined by {@code corner1} and {@code corner2} is entirely visible.
   *
   * @see #fit(Vector, Vector)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector corner1, Vector corner2, float duration) {
    if (duration <= 0)
      fit(corner1, corner2);
    else {
      _interpolator.stop();
      _interpolator.purge();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(corner1, corner2);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.start();
    }
  }

  /**
   * Moves the eye so that the world axis aligned box defined by {@code corner1}
   * and {@code corner2} is entirely visible.
   *
   * @see #fit(Vector, Vector, float)
   * @see #fit(float)
   * @see #fit(Vector, float, float)
   * @see #fit(Node)
   * @see #fit(Node, float)
   * @see #fit(Rectangle, float)
   * @see #fit()
   * @see #fit(Rectangle)
   * @see #fit(Vector, float, float)
   * @see #fitFOV()
   * @see #fitFOV(float)
   */
  public void fit(Vector corner1, Vector corner2) {
    float diameter = Math.max(Math.abs(corner2._vector[1] - corner1._vector[1]), Math.abs(corner2._vector[0] - corner1._vector[0]));
    diameter = Math.max(Math.abs(corner2._vector[2] - corner1._vector[2]), diameter);
    fit(Vector.multiply(Vector.add(corner1, corner2), 0.5f), 0.5f * diameter);
  }

  /**
   * Smoothly moves the eye during {@code duration} seconds so that the rectangular
   * screen region defined by {@code rectangle} (pixel units, with origin in the
   * upper left corner) fits the screen.
   * <p>
   * The eye is translated (its {@link Node#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>boundary</i> in 3D, it's the intersection of this boundary with a plane
   * (orthogonal to the {@link #viewDirection()} and passing through the
   * {@link #center()}) that is used to define the 3D rectangle that is eventually
   * fitted.
   *
   * @see #fit(Rectangle)
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
  public void fit(Rectangle rectangle, float duration) {
    if (duration <= 0)
      fit(rectangle);
    else {
      _interpolator.stop();
      _interpolator.purge();
      Node eye = eye();
      setEye(eye().detach());
      _interpolator.addKeyFrame(eye().detach());
      fit(rectangle);
      _interpolator.addKeyFrame(eye().detach(), duration);
      setEye(eye);
      _interpolator.start();
    }
  }

  /**
   * Moves the eye so that the rectangular screen region defined by {@code rectangle}
   * (pixel units, with origin in the upper left corner) fits the screen.
   * <p>
   * In 3D the eye is translated (its {@link Node#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #center()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   *
   * @see #fit(Rectangle, float)
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
  public void fit(Rectangle rectangle) {
    //ad-hoc
    if (is2D()) {
      float rectRatio = (float) rectangle.width() / (float) rectangle.height();
      if (aspectRatio() < 1.0f) {
        if (aspectRatio() < rectRatio)
          eye().setMagnitude(eye().magnitude() * (float) rectangle.width() / width());
        else
          eye().setMagnitude(eye().magnitude() * (float) rectangle.height() / height());
      } else {
        if (aspectRatio() < rectRatio)
          eye().setMagnitude(eye().magnitude() * (float) rectangle.width() / width());
        else
          eye().setMagnitude(eye().magnitude() * (float) rectangle.height() / height());
      }
      lookAt(location(new Vector(rectangle.centerX(), rectangle.centerY(), 0)));
      return;
    }

    Vector vd = viewDirection();
    float distToPlane = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis());
    Point center = new Point((int) rectangle.centerX(), (int) rectangle.centerY());
    Vector orig = new Vector();
    Vector dir = new Vector();
    convertClickToLine(center, orig, dir);
    Vector newCenter = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    convertClickToLine(new Point(rectangle.x(), center.y()), orig, dir);
    Vector pointX = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    convertClickToLine(new Point(center.x(), rectangle.y()), orig, dir);
    Vector pointY = Vector.add(orig, Vector.multiply(dir, (distToPlane / Vector.dot(dir, vd))));
    float distance = 0.0f;
    float distX, distY;
    switch (type()) {
      case PERSPECTIVE:
        //horizontal fov: radians(eye().magnitude() * aspectRatio())
        distX = Vector.distance(pointX, newCenter) / (float) Math.sin(2 * (float) Math.atan(eye().magnitude() * aspectRatio()) / 2.0f);
        distY = Vector.distance(pointY, newCenter) / (float) Math.sin(fov() / 2.0f);
        distance = Math.max(distX, distY);
        break;
      case ORTHOGRAPHIC:
        float dist = Vector.dot(Vector.subtract(newCenter, anchor()), vd);
        distX = Vector.distance(pointX, newCenter) / eye().magnitude() / aspectRatio();
        distY = Vector.distance(pointY, newCenter) / eye().magnitude() / 1.0f;
        distance = dist + Math.max(distX, distY);
        break;
    }
    eye().setPosition(Vector.subtract(newCenter, Vector.multiply(vd, distance)));
  }

  /**
   * Gives the coefficients of a 3D half-line passing through the eye and pixel
   * (x,y). Origin in the upper left corner. Use {@link #height()} - y to locate the
   * origin at the lower left corner.
   * <p>
   * The origin of the half line (eye position) is stored in {@code orig}, while
   * {@code dir} contains the properly oriented and normalized direction of the half line.
   * <p>
   * This method is useful for analytical intersection in a selection method.
   */
  public void convertClickToLine(Point pixel, Vector origin, Vector direction) {
    Point pixelCopy = new Point(pixel.x(), pixel.y());

    // left-handed coordinate system correction
    if (isLeftHanded())
      pixelCopy.setY(height() - pixel.y());

    switch (type()) {
      case PERSPECTIVE:
        origin.set(eye().position());
        direction.set(new Vector(((2.0f * pixelCopy.x() / width()) - 1.0f) * eye().magnitude() * aspectRatio(),
            ((2.0f * (height() - pixelCopy.y()) / height()) - 1.0f) * eye().magnitude(),
            -1.0f));
        direction.set(Vector.subtract(eye().worldLocation(direction), origin));
        direction.normalize();
        break;

      case TWO_D:
      case ORTHOGRAPHIC: {
        float wh0 = eye().magnitude() * width() / 2;
        float wh1 = eye().magnitude() * height() / 2;
        origin.set(new Vector((2.0f * pixelCopy.x() / width() - 1.0f) * wh0,
            -(2.0f * pixelCopy.y() / height() - 1.0f) * wh1,
            0.0f));
        origin.set(eye().worldLocation(origin));
        direction.set(viewDirection());
        break;
      }
    }
  }

  // Nice stuff :P

  /**
   * Apply the local transformation defined by {@code node}, i.e., respect to its
   * {@link Node#reference()}. The Node is first translated, then rotated around
   * the new translated origin and then scaled.
   * <p>
   * This method may be used to modify the modelview matrix from a node hierarchy. For
   * example, with this node hierarchy:
   * <p>
   * {@code Node body = new Node();} <br>
   * {@code Node leftArm = new Node();} <br>
   * {@code Node rightArm = new Node();} <br>
   * {@code leftArm.setReference(body);} <br>
   * {@code rightArm.setReference(body);} <br>
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
   * Note the use of nested {@code pushModelView()} and {@code popModelView()} blocks to
   * represent the node hierarchy: {@code leftArm} and {@code rightArm} are both
   * correctly drawn with respect to the {@code body} coordinate system.
   *
   * @see #applyWorldTransformation(Node)
   */
  public void applyTransformation(Node node) {
    _matrixHandler._applyTransformation(node);
  }

  /**
   * Same as {@link #applyTransformation(Node)}, but applies the global transformation
   * defined by the node.
   */
  public void applyWorldTransformation(Node node) {
    _matrixHandler._applyWorldTransformation(node);
  }

  // Other stuff

  /**
   * Returns true if graph is left handed. Note that the graph is right handed by default.
   *
   * @see #setLeftHanded()
   */
  public boolean isLeftHanded() {
    return !_rightHanded;
  }

  /**
   * Returns true if graph is right handed. Note that the graph is right handed by default.
   *
   * @see #setRightHanded()
   */
  public boolean isRightHanded() {
    return _rightHanded;
  }

  /**
   * Set the graph as right handed.
   *
   * @see #isRightHanded()
   */
  public void setRightHanded() {
    _rightHanded = true;
  }

  /**
   * Set the graph as left handed.
   *
   * @see #isLeftHanded()
   */
  public void setLeftHanded() {
    _rightHanded = false;
  }

  /**
   * @return true if the graph is 2D.
   */
  public boolean is2D() {
    return type() == Type.TWO_D;
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
   * @return last node the eye was updated
   * @see #_lastNonEyeUpdate()
   */
  public long lastUpdate() {
    return Math.max(eye().lastUpdate(), _lastNonEyeUpdate());
  }

  /**
   * @return last node a local eye parameter (different than the Node) was updated.
   * @see #lastUpdate()
   */
  protected long _lastNonEyeUpdate() {
    return _lastNonEyeUpdate;
  }

  // traversal

  // detached nodes

  /**
   * Same as {@code return track(null, pixel, nodeArray)}.
   *
   * @see #track(String, float, float, Node[])
   */
  public Node track(Point pixel, Node[] nodeArray) {
    return track(null, pixel, nodeArray);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y(), nodeArray)}.
   *
   * @see #track(String, float, float, Node[])
   */
  public Node track(String hid, Point pixel, Node[] nodeArray) {
    return track(hid, pixel.x(), pixel.y(), nodeArray);
  }

  /**
   * Same as {@code return track(null, x, y, nodeArray)}.
   *
   * @see #track(String, float, float, Node[])
   */
  public Node track(float x, float y, Node[] nodeArray) {
    return track(null, x, y, nodeArray);
  }

  /**
   * Updates the {@code hid} device tracked-node from the {@code nodeArray} and returns it.
   * <p>
   * To set the {@link #trackedNode(String)} the algorithm casts a ray at pixel position {@code (x, y)}
   * (see {@link #tracks(float, float, Node)}). If no node is found under the pixel, it returns {@code null}.
   * <p>
   * Use this version of the method instead of {@link #track(String, float, float)} when dealing with
   * detached nodes.
   *
   * @see #track(String, float, float)
   * @see #track(String, float, float, List)
   * @see #render()
   * @see #trackedNode(String)
   * @see #resetTrackedNode(String)
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   * @see Node#enableTracking(boolean)
   * @see Node#pickingThreshold()
   * @see Node#setPickingThreshold(float)
   * @see #cast(String, Point)
   * @see #cast(String, float, float)
   */
  public Node track(String hid, float x, float y, Node[] nodeArray) {
    resetTrackedNode(hid);
    for (Node node : nodeArray)
      if (tracks(x, y, node)) {
        setTrackedNode(hid, node);
        break;
      }
    return trackedNode(hid);
  }

  /**
   * Same as {@code return track(null, pixel, nodeList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Node track(Point pixel, List<Node> nodeList) {
    return track(null, pixel, nodeList);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y(), nodeList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Node track(String hid, Point pixel, List<Node> nodeList) {
    return track(hid, pixel.x(), pixel.y(), nodeList);
  }

  /**
   * Same as {@code return track(null, x, y, nodeList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Node track(float x, float y, List<Node> nodeList) {
    return track(null, x, y, nodeList);
  }

  /**
   * Same as {@link #track(String, float, float, Node[])} but using a node list instead of an array.
   *
   * @see #track(String, float, float, Node[])
   */
  public Node track(String hid, float x, float y, List<Node> nodeList) {
    resetTrackedNode(hid);
    for (Node node : nodeList)
      if (tracks(x, y, node)) {
        setTrackedNode(hid, node);
        break;
      }
    return trackedNode(hid);
  }

  // attached nodes

  /**
   * Same as {@code return track(null, pixel.x(), pixel.y())}.
   *
   * @see #track(String, Point)
   */
  public Node track(Point pixel) {
    return track(null, pixel.x(), pixel.y());
  }

  /**
   * Same as {@code return track(null, x, y)}.
   *
   * @see #track(String, float, float)
   */
  public Node track(float x, float y) {
    return track(null, x, y);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y())}.
   *
   * @see #track(String, float, float)
   */
  public Node track(String hid, Point pixel) {
    return track(hid, pixel.x(), pixel.y());
  }

  /**
   * Updates the {@code hid} device tracked-node and returns it.
   * <p>
   * To set the {@link #trackedNode(String)} the algorithm casts a ray at pixel position {@code (x, y)}
   * (see {@link #tracks(float, float, Node)}). If no node is found under the pixel, it returns {@code null}.
   * <p>
   * Use this version of the method instead of {@link #track(String, float, float, Node[])} when dealing with
   * attached nodes to the graph.
   *
   * @see #track(String, float, float, Node[])
   * @see #render()
   * @see #trackedNode(String)
   * @see #resetTrackedNode(String)
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   * @see Node#enableTracking(boolean)
   * @see Node#pickingThreshold()
   * @see Node#setPickingThreshold(float)
   * @see #cast(String, Point)
   * @see #cast(String, float, float)
   */
  public Node track(String hid, float x, float y) {
    resetTrackedNode(hid);
    for (Node node : _leadingNodes())
      _track(hid, node, x, y);
    return trackedNode(hid);
  }

  /**
   * Use internally by {@link #track(String, float, float)}.
   */
  protected void _track(String hid, Node node, float x, float y) {
    if (trackedNode(hid) == null && node.isTrackingEnabled())
      if (tracks(x, y, node)) {
        setTrackedNode(hid, node);
        return;
      }
    if (!node.isCulled() && trackedNode(hid) == null)
      for (Node child : node.children())
        _track(hid, child, x, y);
  }

  /**
   * Same as {tracks(pixel.x(), pixel.y(), node)}.
   *
   * @see #tracks(float, float, Node)
   */
  public boolean tracks(Point pixel, Node node) {
    return tracks(pixel.x(), pixel.y(), node);
  }

  /**
   * Casts a ray at pixel position {@code (x, y)} and returns {@code true} if the ray picks the {@code node} and
   * {@code false} otherwise. The node is picked according to the {@link Node#pickingThreshold()}.
   *
   * @see #trackedNode(String)
   * @see #resetTrackedNode(String)
   * @see #defaultNode(String)
   * @see #track(String, float, float)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   * @see Node#enableTracking(boolean)
   * @see Node#pickingThreshold()
   * @see Node#setPickingThreshold(float)
   */
  public boolean tracks(float x, float y, Node node) {
    if (node.pickingThreshold() == 0 && _bb != null)
      return _tracks(x, y, node);
    else
      return _tracks(x, y, screenLocation(node.position()), node);
  }

  /**
   * A shape may be picked using
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a> with a
   * color buffer (see {@link #_backBuffer()}). This method
   * compares the color of the {@link #_backBuffer()} at
   * {@code (x,y)} with the shape id. Returns true if both colors are the same, and false
   * otherwise.
   * <p>
   * This method should be overridden. Default implementation symply return {@code false}.
   *
   * @see Node#setPickingThreshold(float)
   */
  protected boolean _tracks(float x, float y, Node node) {
    return false;
  }

  /**
   * Cached version of {@link #tracks(float, float, Node)}.
   */
  protected boolean _tracks(float x, float y, Vector projection, Node node) {
    if (node == null || isEye(node))
      return false;
    if (!node.isTrackingEnabled())
      return false;
    float threshold = node.pickingThreshold() < 1 ? 100 * node.pickingThreshold() * node.scaling() * pixelToGraphRatio(node.position())
        : node.pickingThreshold() / 2;
    return ((Math.abs(x - projection._vector[0]) < threshold) && (Math.abs(y - projection._vector[1]) < threshold));
  }

  /**
   * Same as {@code cast(null, new Point(x, y))}.
   *
   * @see #cast(String, Point)
   */
  public void cast(float x, float y) {
    cast(null, new Point(x, y));
  }

  /**
   * Same as {@code cast(hid, new Point(x, y))}.
   *
   * @see #cast(String, Point)
   */
  public void cast(String hid, float x, float y) {
    cast(hid, new Point(x, y));
  }

  /**
   * Same as {@code cast(null, pixel)}.
   *
   * @see #cast(String, float, float)
   */
  public void cast(Point pixel) {
    cast(null, pixel);
  }

  /**
   * Same as {@link #track(String, Point)} but doesn't return immediately the {@code hid} device tracked-node.
   * The algorithm schedules an updated of the {@code hid} tracked-node for the next traversal and hence should be
   * always be used in conjunction with {@link #render()}.
   * <p>
   * This method is optimal since it updates the {@code hid} tracked-node at traversal time. Prefer this method over
   * {@link #track(String, Point)} when dealing with several {@code hids}.
   *
   * @see #render()
   * @see #trackedNode(String)
   * @see #resetTrackedNode(String)
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   * @see Node#enableTracking(boolean)
   * @see Node#pickingThreshold()
   * @see Node#setPickingThreshold(float)
   * @see #cast(String, float, float)
   */
  public void cast(String hid, Point pixel) {
    _rays.add(new Ray(hid, pixel));
  }

  // Off-screen

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
   * <a href="http://schabby.de/picking-opengl-ray-tracing/">'ray-picking'</a>.
   */
  protected Object _backBuffer() {
    return _bb;
  }

  /**
   * Render the scene into the back-buffer used for picking.
   */
  protected void _renderBackBuffer() {
    _bbMatrixHandler._bindProjection(_matrixHandler.projection());
    _bbMatrixHandler._bindModelView(_matrixHandler.cacheView());
    for (Node node : _leadingNodes())
      _renderBackBuffer(node);
    if(isOffscreen())
      _rays.clear();
  }

  /**
   * Used by the {@link #_renderBackBuffer()} algorithm.
   */
  protected void _renderBackBuffer(Node node) {
    _bbMatrixHandler.pushModelView();
    _bbMatrixHandler._applyTransformation(node);
    if (!node.isCulled()) {
      _drawBackBuffer(node);
      if (!isOffscreen())
        _trackBackBuffer(node);
      for (Node child : node.children())
        _renderBackBuffer(child);
    }
    _bbMatrixHandler.popModelView();
  }

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link TimingHandler#handle()}.</li>
   * <li>Updates the projection matrix by calling
   * {@code eye().projection(type(), width(), height(), zNear(), zFar(), isLeftHanded())}.</li>
   * <li>Updates the view matrix by calling {@code eye().view()}.</li>
   * <li>Calls {@link #updateBoundaryEquations()} if {@link #areBoundaryEquationsEnabled()}</li>
   * </ol>
   *
   * @see #fov()
   * @see TimingHandler#handle()
   * @see Node#projection(Type, float, float, float, float, boolean)
   * @see Node#view()
   */
  public void preDraw() {
    timingHandler().handle();
    _matrixHandler._bind(eye().projection(type(), width(), height(), zNear(), zFar(), isLeftHanded()), eye().view());
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0)) {
      updateBoundaryEquations();
      _lastEqUpdate = TimingHandler.frameCount;
    }
  }

  /**
   * Renders the scene onto the {@link #context()}. Calls {@link Node#visit()} on each visited node
   * (refer to the {@link Node} documentation).
   * <p>
   * Note that only reachable nodes (nodes attached to this graph, see
   * {@link Node#isAttached(Graph)}) are rendered by this algorithm.
   *
   * @see #render(MatrixHandler, Object)
   * @see Node#visit()
   * @see Node#cull(boolean)
   * @see Node#isCulled()
   * @see Node#graphics(Object)
   * @see Node#shape(Object)
   */
  public void render() {
    for (Node node : _leadingNodes())
      _render(node);
    _rays.clear();
  }

  /**
   * Used by the {@link #render()} algorithm.
   */
  protected void _render(Node node) {
    _matrixHandler.pushModelView();
    applyTransformation(node);
    node.visit();
    if (!node.isCulled()) {
      _trackFrontBuffer(node);
      if (isOffscreen())
        _trackBackBuffer(node);
      _drawFrontBuffer(node);
      for (Node child : node.children())
        _render(child);
    }
    _matrixHandler.popModelView();
  }

  /**
   * Renders the scene onto {@code context} using {@code matrixHandler}.
   *
   * @see #render()
   */
  public void render(MatrixHandler matrixHandler, Object context) {
    // TODO adjust
    if (context == context())
      throw new RuntimeException("Cannot render into front-buffer, use render() instead of render(context, view, projection)");
      //if (context == context())
      //render();
    else {
      matrixHandler._bindProjection(_matrixHandler.projection());
      matrixHandler._bindModelView(_matrixHandler.cacheView());
      for (Node node : _leadingNodes())
        _render(matrixHandler, context, node);
    }
  }

  // TODO pending
  //public MatrixHandler matrixHandler(Object context, Type type, float zNear, float zFar, boolean leftHanded) {
  public MatrixHandler matrixHandler(Object context) {
    return null;
  }

  // TODO goal. Big Q: perhaps move type, zNear, zFar and lh into matrixHandler???
  // public void render(MatrixHandler matrixHandler, Node eye)

  public void render(MatrixHandler matrixHandler, Object context, Matrix projection, Matrix view) {
    // TODO adjust
    if (context == context())
      throw new RuntimeException("Cannot render into front-buffer, use render() instead of render(context, view, projection)");
      //if (context == context())
      //render();
    else {
      matrixHandler._bindProjection(projection);
      matrixHandler._bindModelView(view);
      for (Node node : _leadingNodes())
        _render(matrixHandler, context, node);
    }
  }

  /**
   * Used by the {@link #render(MatrixHandler, Object)} algorithm.
   */
  protected void _render(MatrixHandler matrixHandler, Object context, Node node) {
    matrixHandler.pushModelView();
    matrixHandler._applyTransformation(node);
    if (!node.isCulled()) {
      _drawOntoBuffer(context, node);
      for (Node child : node.children())
        _render(matrixHandler, context, child);
    }
    matrixHandler.popModelView();
  }

  /**
   * Renders the node onto {@link #context()}. Same as {@code draw(context(), node)}.
   *
   * @see #draw(Object, Node)
   */
  public void draw(Node node) {
    draw(context(), node);
  }

  /**
   * Renders the node onto {@code context}, provided that it holds a visual
   * representation (see {@link Node#graphics(Object)} and {@link Node#shape(Object)}).
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   *
   * @see #render()
   */
  public void draw(Object context, Node node) {
    if (context == _backBuffer())
      return;
    if(context == context())
      _drawFrontBuffer(node);
    else
      _drawOntoBuffer(context, node);
  }

  protected void _drawFrontBuffer(Node node) {
  }

  /**
   * Renders the node onto the {@link #_backBuffer()}.
   * <p>
   * Default implementation is empty, i.e., it is meant to be implemented by derived classes.
   *
   * @see #render()
   * @see Node#cull(boolean)
   * @see Node#isCulled()
   * @see Node#visit()
   * @see Node#graphics(Object)
   * @see Node#shape(Object)
   */
  protected void _drawBackBuffer(Node node) {
  }

  protected void _drawOntoBuffer(Object context, Node node) {
  }

  /**
   * Internally used by {@link #_render(Node)}.
   */
  protected void _trackFrontBuffer(Node node) {
    if (node.isTrackingEnabled() && !_rays.isEmpty() && node.pickingThreshold() > 0) {
      Vector projection = screenLocation(node.position());
      Iterator<Ray> it = _rays.iterator();
      while (it.hasNext()) {
        Ray ray = it.next();
        resetTrackedNode(ray._hid);
        // Condition is overkill. Use it only in place of resetTrackedNode
        //if (!isTracking(ray._hid))
        if (_tracks(ray._pixel.x(), ray._pixel.y(), projection, node)) {
          setTrackedNode(ray._hid, node);
          it.remove();
        }
      }
    }
  }

  /**
   * Internally used by {@link #_render(Node)} and {@link #_renderBackBuffer(Node)}.
   */
  protected void _trackBackBuffer(Node node) {
    if (node.isTrackingEnabled() && !_rays.isEmpty() && node.pickingThreshold() == 0 && _bb != null) {
      Iterator<Ray> it = _rays.iterator();
      while (it.hasNext()) {
        Ray ray = it.next();
        resetTrackedNode(ray._hid);
        // Condition is overkill. Use it only in place of resetTrackedNode
        //if (!isTracking(ray._hid))
        if (_tracks(ray._pixel.x(), ray._pixel.y(), node)) {
          setTrackedNode(ray._hid, node);
          it.remove();
        }
      }
    }
  }

  /**
   * Same as {@code setTrackedNode(null, node)}.
   *
   * @see #setTrackedNode(String, Node)
   */
  public void setTrackedNode(Node node) {
    setTrackedNode(null, node);
  }

  /**
   * Sets the {@code hid} tracked-node (see {@link #trackedNode(String)}). Call this function if you want to set the
   * tracked node manually and {@link #track(String, Point)} or {@link #cast(String, Point)} to set it automatically
   * using ray casting.
   *
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #resetTrackedNode(String)
   * @see #isTrackedNode(String, Node)
   * @see Node#enableTracking(boolean)
   */
  public void setTrackedNode(String hid, Node node) {
    if (node == null) {
      System.out.println("Warning. Cannot track a null node!");
      return;
    }
    if (!node.isTrackingEnabled()) {
      System.out.println("Warning. Node cannot be tracked! Enable tracking on the node first by call node.enableTracking(true)");
      return;
    }
    _agents.put(hid, node);
  }

  /**
   * Same as {@code return trackedNode(null)}.
   *
   * @see #trackedNode(String)
   */
  public Node trackedNode() {
    return trackedNode(null);
  }

  /**
   * Returns the current {@code hid} tracked node which is usually set by ray casting (see
   * {@link #track(String, float, float)}). May return {@code null}. Reset it with {@link #resetTrackedNode(String)}.
   *
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #resetTrackedNode(String)
   * @see #isTrackedNode(String, Node)
   * @see #setTrackedNode(String, Node)
   */
  public Node trackedNode(String hid) {
    return _agents.get(hid);
  }

  /**
   * Same as {@code isTracking(null)}.
   *
   * @see #isTracking(String)
   */
  public boolean isTracking() {
    return isTracking(null);
  }

  /**
   * Returns {@code true} if the {@code hid} has a non-null tracked node and {@code false} otherwise.
   */
  public boolean isTracking(String hid) {
    return _agents.containsKey(hid);
  }

  /**
   * Same as {@code return isTrackedNode(null, node)}.
   *
   * @see #isTrackedNode(String, Node)
   * @see Node#isTracked()
   */
  public boolean isTrackedNode(Node node) {
    return isTrackedNode(null, node);
  }

  /**
   * Returns {@code true} if {@code node} is the current {@code hid} {@link #trackedNode(String)} and {@code false} otherwise.
   *
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #resetTrackedNode(String)
   * @see #setTrackedNode(String, Node)
   * @see Node#isTracked()
   */
  public boolean isTrackedNode(String hid, Node node) {
    return trackedNode(hid) == node;
  }

  /**
   * Resets all HID's {@link #trackedNode(String)}.
   *
   * @see #trackedNode(String)
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   */
  public void resetTracking() {
    _agents.clear();
  }

  /**
   * Same as {@code resetTrackedNode(null)}.
   *
   * @see #resetTrackedNode(String)
   */
  public void resetTrackedNode() {
    resetTrackedNode(null);
  }

  /**
   * Resets the current {@code hid} {@link #trackedNode(String)} so that a call to {@link #isTracking(String)}
   * will return {@code false}. Note that {@link #track(String, float, float)} will reset the tracked node automatically.
   *
   * @see #trackedNode(String)
   * @see #defaultNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   */
  public void resetTrackedNode(String hid) {
    _agents.remove(hid);
  }

  /**
   * Same as {@code return defaultNode(null)}.
   *
   * @see #defaultNode(String)
   */
  public Node defaultNode() {
    return defaultNode(null);
  }

  /**
   * Returns the {@code hid} default-node. Used by methods dealing with interactivity that don't take a node
   * param. Same as {@code return trackedNode(hid) == null ? eye() : trackedNode(hid)}. Never returns {@code null}.
   *
   * @see #trackedNode(String)
   * @see #resetTrackedNode(String)
   * @see #tracks(float, float, Node)
   * @see #track(String, float, float)
   * @see #setTrackedNode(String, Node)
   * @see #isTrackedNode(String, Node)
   */
  public Node defaultNode(String hid) {
    return trackedNode(hid) == null ? eye() : trackedNode(hid);
  }

  /**
   * Same as {@code align(null)}.
   *
   * @see #align(String)
   */
  public void align() {
    align(null);
  }

  /**
   * Same as {@code align(defaultNode(hid))}.
   *
   * @see #alignWith(Node)
   * @see #defaultNode(String)
   */
  public void align(String hid) {
    alignWith(defaultNode(hid));
  }

  /**
   * If {@code node} is {@link #isEye(Node)} aligns the {@link #eye()} with the world.
   * Otherwise aligns the {@code node} with the {@link #eye()}. {@code node} should be
   * non-null.
   * <p>
   * Wrapper method for {@link Node#align(boolean, float, Node)}.
   *
   * @see #isEye(Node)
   * @see #defaultNode(String)
   */
  public void alignWith(Node node) {
    if (node == null)
      throw new RuntimeException("align(node) requires a non-null node param");
    if (isEye(node))
      node.align(true);
    else
      node.align(eye());
  }

  /**
   * Same as {@code focus(null)}.
   *
   * @see #focus(String)
   */
  public void focus() {
    focus(null);
  }

  /**
   * Same as {@code focus(defaultNode())}.
   *
   * @see #focusWith(Node)
   * @see #defaultNode(String)
   */
  public void focus(String hid) {
    focusWith(defaultNode(hid));
  }

  /**
   * Centers the node into the graph.
   */
  public void focusWith(Node node) {
    if (node == null)
      throw new RuntimeException("focus(node) requires a non-null node param");
    if (isEye(node))
      node.projectOnLine(center(), viewDirection());
    else
      node.projectOnLine(eye().position(), eye().zAxis(false));
  }

  // Screen to node conversion

  /**
   * Convenience function that simply returns {@code screenLocation(src, null)}.
   *
   * @see #screenLocation(Vector, Node)
   */
  public Vector screenLocation(Vector vector) {
    return screenLocation(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code node} to screen.
   * Use {@code location(vector, node)} to perform the inverse transformation.
   * <p>
   * The x and y coordinates of the returned vector are expressed in screen coordinates,
   * (0,0) being the upper left corner of the window. The z coordinate ranges between 0
   * (near plane) and 1 (excluded, far plane).
   *
   * @see #screenLocation(Vector)
   * @see #location(Vector, Node)
   * @see #location(Vector)
   */
  public Vector screenLocation(Vector vector, Node node) {
    float xyz[] = new float[3];

    if (node != null) {
      Vector tmp = node.worldLocation(vector);
      _screenLocation(tmp._vector[0], tmp._vector[1], tmp._vector[2], xyz);
    } else
      _screenLocation(vector._vector[0], vector._vector[1], vector._vector[2], xyz);

    return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  // cached version
  protected boolean _screenLocation(float objx, float objy, float objz, float[] windowCoordinate) {
    Matrix projectionViewMatrix = _matrixHandler.cacheProjectionView();

    float in[] = new float[4];
    float out[] = new float[4];

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
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
      return false;

    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height();
    viewport[2] = width();
    viewport[3] = -height();

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

    windowCoordinate[0] = out[0];
    windowCoordinate[1] = out[1];
    windowCoordinate[2] = out[2];

    return true;
  }

  /**
   * Convenience function that simply returns {@code location(pixel, null)}.
   * <p>
   * #see {@link #location(Vector, Node)}
   */
  public Vector location(Vector pixel) {
    return this.location(pixel, null);
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
   * This method only uses the intrinsic eye parameters (view and projection matrices),
   * {@link #width()} and {@link #height()}). You can hence define a virtual eye and use
   * this method to compute un-projections out of a classical rendering context.
   * <p>
   * This method is not computationally optimized by default. If you call it several times with no
   * change in the matrices, you should buffer the inverse of the projection times view matrix
   * to speed-up the queries. See {@link #cacheProjectionViewInverse(boolean)}.
   *
   * @see #screenLocation(Vector, Node)
   * @see #setWidth(int)
   * @see #setHeight(int)
   */
  public Vector location(Vector pixel, Node node) {
    float xyz[] = new float[3];
    _location(pixel._vector[0], pixel._vector[1], pixel._vector[2], xyz);
    if (node != null)
      return node.location(new Vector(xyz[0], xyz[1], xyz[2]));
    else
      return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  /**
   * Similar to {@code gluUnProject}: map window coordinates to object coordinates.
   *
   * @param winx          Specify the window x coordinate.
   * @param winy          Specify the window y coordinate.
   * @param winz          Specify the window z coordinate.
   * @param objCoordinate Return the computed object coordinates.
   */
  protected boolean _location(float winx, float winy, float winz, float[] objCoordinate) {
    Matrix projectionViewInverseMatrix;
    if (_matrixHandler.isProjectionViewInverseCached())
      projectionViewInverseMatrix = _matrixHandler.cacheProjectionViewInverse();
    else {
      projectionViewInverseMatrix = Matrix.multiply(_matrixHandler.cacheProjection(), _matrixHandler.cacheView());
      projectionViewInverseMatrix.invert();
    }

    int[] viewport = new int[4];
    viewport[0] = 0;
    viewport[1] = height();
    viewport[2] = width();
    viewport[3] = -height();

    float in[] = new float[4];
    float out[] = new float[4];

    in[0] = winx;
    in[1] = winy;
    in[2] = winz;
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
      return false;

    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];

    objCoordinate[0] = out[0];
    objCoordinate[1] = out[1];
    objCoordinate[2] = out[2];

    return true;
  }

  // Gesture physical interface is quite nice!
  // It always maps physical (screen) space geom data respect to the eye

  /**
   * Same as {@code translate(null, dx, dy)}.
   *
   * @see #translate(String, float, float)
   */
  public void translate(float dx, float dy) {
    translate(null, dx, dy);
  }

  /**
   * Same as {@code translate(dx, dy, 0, defaultNode(hid))}.
   *
   * @see #translate(float, float, Node)
   * @see #translate(float, float, float, Node)
   * @see #translate(String, float, float, float)
   * @see #defaultNode(String)
   */
  public void translate(String hid, float dx, float dy) {
    translate(dx, dy, 0, defaultNode(hid));
  }

  /**
   * Same as {@code translate(null, dx, dy, dz)}.
   *
   * @see #translate(String, float, float, float)
   */
  public void translate(float dx, float dy, float dz) {
    translate(null, dx, dy, dz);
  }

  /**
   * Same as {@code translate(dx, dy, dz, defaultNode(hid))}.
   *
   * @see #translate(float, float, Node)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Node)
   * @see #defaultNode(String)
   */
  public void translate(String hid, float dx, float dy, float dz) {
    translate(dx, dy, dz, defaultNode(hid));
  }

  /**
   * Same as {@code translate(dx, dy, 0, node)}.
   *
   * @see #translate(String, float, float, float)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Node)
   * @see #defaultNode(String)
   */
  public void translate(float dx, float dy, Node node) {
    translate(dx, dy, 0, node);
  }

  /**
   * Translates the {@code node} according to {@code dx}, {@code dy} and {@code dz}. The {@code dx} and {@code dy}
   * coordinates are expressed in screen space, and the {@code dz} coordinate is given in world units.
   * The translated node would be kept exactly under a pointer if such a device were used to translate it.
   *
   * @see #translate(float, float, Node)
   * @see #translate(String, float, float)
   * @see #translate(String, float, float, float)
   * @see #defaultNode(String)
   */
  public void translate(float dx, float dy, float dz, Node node) {
    if (node == null)
      throw new RuntimeException("translate(vector, node) requires a non-null node param");
    node.translate(_translate(dx, dy, dz, node));
  }

  /**
   * Same as {@code return _translate(dx, dy, dz, Math.min(width(), height()), node)}.
   *
   * @see #_translate(float, float, float, int, Node)
   */
  protected Vector _translate(float dx, float dy, float dz, Node node) {
    return _translate(dx, dy, dz, Math.min(width(), height()), node);
  }

  /**
   * Interactive translation low-level implementation. Converts {@code dx} and {@code dy} defined in screen space to
   * {@link Node#reference()} (or world coordinates if the node reference is null).
   * <p>
   * The projection onto the screen of the returned vector exactly match the screen {@code (dx, dy)} vector displacement
   * (e.g., the translated node would be kept exactly under a pointer if such a device were used to translate it).
   * The z-coordinate is mapped from [0..{@code zMax}] to the [0..2*{@link #radius()}}] range.
   */
  protected Vector _translate(float dx, float dy, float dz, int zMax, Node node) {
    if (is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    }
    dx = isEye(node) ? -dx : dx;
    dy = isRightHanded() ^ isEye(node) ? -dy : dy;
    dz = isEye(node) ? dz : -dz;
    // Scale to fit the screen relative vector displacement
    if (type() == Type.PERSPECTIVE) {
      float k = (float) Math.tan(fov() / 2.0f) * Math.abs(
          eye().location(isEye(node) ? anchor() : node.position())._vector[2] * eye().magnitude());
      //TODO check me weird to find height instead of width working (may it has to do with fov?)
      dx *= 2.0 * k / (height() * eye().magnitude());
      dy *= 2.0 * k / (height() * eye().magnitude());
    }
    // this expresses the dz coordinate in world units:
    //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
    Vector eyeVector = new Vector(dx, dy, dz * 2 * radius() / zMax);
    return node.reference() == null ? eye().worldDisplacement(eyeVector) : node.reference().displacement(eyeVector, eye());
  }

  /**
   * Same as {@code scale(null, delta)}.
   *
   * @see #scale(String, float)
   */
  public void scale(float delta) {
    scale(null, delta);
  }

  /**
   * Same as {@code scale(delta, defaultNode(hid))}.
   *
   * @see #scale(float, Node)
   * @see #defaultNode(String)
   */
  public void scale(String hid, float delta) {
    scale(delta, defaultNode(hid));
  }

  /**
   * Scales the {@code node} according to {@code delta}. Note that if {@code node} is the {@link #eye()}
   * this call simply changes the {@link #fov()}.
   *
   * @see #scale(String, float)
   */
  public void scale(float delta, Node node) {
    float factor = 1 + Math.abs(delta) / (float) (isEye(node) ? -height() : height());
    node.scale(delta >= 0 ? factor : 1 / factor);
  }

  /**
   * Same as {@code rotate(null, roll, pitch, yaw)}.
   *
   * @see #rotate(String, float, float, float)
   */
  public void rotate(float roll, float pitch, float yaw) {
    rotate(null, roll, pitch, yaw);
  }

  /**
   * Rotates the {@code hid} default-node (see {@link #defaultNode(String)}) roll, pitch and yaw radians around screen
   * space x, y and z axes, respectively.
   *
   * @see #rotate(float, float, float, Node)
   */
  public void rotate(String hid, float roll, float pitch, float yaw) {
    rotate(roll, pitch, yaw, defaultNode(hid));
  }

  /**
   * Rotates the {@code node} {@code roll}, {@code pitch} and {@code yaw} radians relative to the screen space
   * x, y and z axes, respectively. The center of the rotation is the graph {@link #anchor()} if the node is the
   * {@link #eye()}, or the node origin (see {@link Node#position()}) otherwise.
   * <p>
   * To rotate an eye node around its origin and local axes simply call:
   * {@code eye().rotate(new Quaternion(roll, pitch, yaw))}.
   *
   * @see #rotate(String, float, float, float)
   * @see #spin(Point, Point, float, Node)
   */
  public void rotate(float roll, float pitch, float yaw, Node node) {
    if (node == null)
      throw new RuntimeException("rotate(roll, pitch, yaw, node) requires a non-null node param");
    spin(_rotate(roll, pitch, yaw, node), node);
  }

  /**
   * Low-level roll-pitch and yaw rotation. Axes are physical, i.e., screen space.
   */
  protected Quaternion _rotate(float roll, float pitch, float yaw, Node node) {
    if (is2D() && (roll != 0 || pitch != 0)) {
      roll = 0;
      pitch = 0;
      System.out.println("Warning: graph is 2D. Roll and/or pitch reset");
    }
    // don't really need to differentiate among the two cases, but the eye can be speeded up
    Quaternion quaternion = new Quaternion(isLeftHanded() ? -roll : roll, pitch, isLeftHanded() ? -yaw : yaw);
    if (isEye(node))
      return quaternion;
    else {
      Vector vector = new Vector(-quaternion.x(), -quaternion.y(), -quaternion.z());
      vector = eye().orientation().rotate(vector);
      vector = node.displacement(vector);
      quaternion.setX(vector.x());
      quaternion.setY(vector.y());
      quaternion.setZ(vector.z());
      return quaternion;
    }
  }

  /**
   * Same as {@code spin(null, tail, head)}.
   *
   * @see #spin(String, Point, Point)
   */
  public void spin(Point tail, Point head) {
    spin(null, tail, head);
  }

  /**
   * Same as {@code spin(tail, head, defaultNode(hid))}.
   *
   * @see #spin(Point, Point, float, Node)
   * @see #spin(Point, Point, Node)
   * @see #spin(String, Point, Point, float)
   */
  public void spin(String hid, Point tail, Point head) {
    spin(tail, head, defaultNode(hid));
  }

  /**
   * Same as {@code spin(tail, head, 1, node)}.
   *
   * @see #spin(Point, Point, float, Node)
   * @see #spin(String, Point, Point)
   * @see #spin(String, Point, Point, float)
   */
  public void spin(Point tail, Point head, Node node) {
    spin(tail, head, 1, node);
  }

  /**
   * Same as {@code spin(null, tail, head, sensitivity)}.
   *
   * @see #spin(String, Point, Point, float)
   */
  public void spin(Point tail, Point head, float sensitivity) {
    spin(null, tail, head, sensitivity);
  }

  /**
   * Same as {@code spin(tail, head, sensitivity, defaultNode(hid))}.
   *
   * @see #spin(Point, Point, float, Node)
   * @see #spin(String, Point, Point)
   * @see #spin(Point, Point, Node)
   * @see #defaultNode(String)
   */
  public void spin(String hid, Point tail, Point head, float sensitivity) {
    spin(tail, head, sensitivity, defaultNode(hid));
  }

  /**
   * Rotates the {@code node} using an arcball interface, from {@code tail} to {@code head} pixel positions. The
   * {@code sensitivity} controls the gesture strength. The center of the rotation is the graph {@link #anchor()}
   * if the node is the {@link #eye()}, or the node origin (see {@link Node#position()}) otherwise.
   * <p>
   * For implementation details refer to Shoemake 92 paper: Arcball: a user interface for specifying three-dimensional
   * orientation using a mouse.
   * <p>
   * Override this class an call {@link #_spin(Point, Point, Point, float, Node)} if you want to define a different
   * rotation center (rare).
   *
   * @see #spin(String, Point, Point)
   * @see #spin(Point, Point, Node)
   * @see #spin(String, Point, Point, float)
   * @see #rotate(float, float, float, Node)
   */
  public void spin(Point tail, Point head, float sensitivity, Node node) {
    if (node == null)
      throw new RuntimeException("spin(point1, point2, sensitivity, node) requires a non-null node param");
    spin(_spin(tail, head, sensitivity, node), node);
  }

  /**
   * Same as {@code return _spin(point1, point2, center, sensitivity, node)} where {@code center} is {@link #anchor()}
   * if the node is the {@link #eye()} or {@link Node#position()} otherwise.
   */
  protected Quaternion _spin(Point point1, Point point2, float sensitivity, Node node) {
    Vector vector = screenLocation(isEye(node) ? anchor() : node.position());
    Point center = new Point(vector.x(), vector.y());
    return _spin(point1, point2, center, sensitivity, node);
  }

  /**
   * Computes the classical arcball quaternion. Refer to Shoemake 92 paper: Arcball: a user interface for specifying
   * three-dimensional orientation using a mouse.
   */
  protected Quaternion _spin(Point point1, Point point2, Point center, float sensitivity, Node node) {
    float cx = center.x();
    float cy = center.y();
    float x = point2.x();
    float y = point2.y();
    float prevX = point1.x();
    float prevY = point1.y();
    // Points on the deformed ball
    float px = sensitivity * ((int) prevX - cx) / width();
    float py = sensitivity * (isLeftHanded() ? ((int) prevY - cy) : (cy - (int) prevY)) / height();
    float dx = sensitivity * (x - cx) / width();
    float dy = sensitivity * (isLeftHanded() ? (y - cy) : (cy - y)) / height();
    Vector p1 = new Vector(px, py, _projectOnBall(px, py));
    Vector p2 = new Vector(dx, dy, _projectOnBall(dx, dy));
    // Approximation of rotation angle Should be divided by the projectOnBall size, but it is 1.0
    Vector axis = p2.cross(p1);
    // 2D is an ad-hoc
    float angle = (is2D() ? sensitivity : 2.0f) * (float) Math.asin((float) Math.sqrt(axis.squaredNorm() / p1.squaredNorm() / p2.squaredNorm()));
    Quaternion quaternion = new Quaternion(axis, angle);
    if (!isEye(node)) {
      Vector vector = quaternion.axis();
      vector = eye().orientation().rotate(vector);
      vector = node.displacement(vector);
      quaternion = new Quaternion(vector, -quaternion.angle());
    }
    return quaternion;
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

  /**
   * Rotates the node using {@code quaternion} around its {@link Node#position()} (non-eye nodes)
   * or around the {@link Graph#anchor()} when the {@code node} is the {@link Graph#eye()}.
   */
  public void spin(Quaternion quaternion, Node node) {
    if (isEye(node))
      //same as:
      //node.orbit(new Quaternion(node.worldDisplacement(quaternion.axis()), quaternion.angle()));
      node._orbit(quaternion, anchor());
    else
      node.rotate(quaternion);
  }

  // only 3d eye

  /**
   * Same as {@code translate(0, 0, delta, eye()); }.
   *
   * @see #translate(float, float, float, Node)
   */
  public void moveForward(float delta) {
    float d1 = type() == Type.ORTHOGRAPHIC ? Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) : 1;
    translate(0, 0, delta, eye());
    float d2 = type() == Type.ORTHOGRAPHIC ? Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) : 1;
    if (type() == Type.ORTHOGRAPHIC)
      if (d2 / d1 > 0 && d1 != 0)
        eye().scale(d2 / d1);
  }

  /**
   * Look around (without translating the eye) according to angular displacements {@code deltaX} and {@code deltaY}
   * expressed in radians.
   */
  public void lookAround(float deltaX, float deltaY) {
    eye().rotate(_lookAround(deltaX, deltaY));
  }

  /**
   * Look around without moving the eye while preserving its {@link Node#yAxis()} when the action began.
   */
  protected Quaternion _lookAround(float deltaX, float deltaY) {
    if (is2D()) {
      System.out.println("Warning: lookAround is only available in 3D");
      return new Quaternion();
    }
    if (frameCount() > _lookAroundCount) {
      _upVector = eye().yAxis();
      _lookAroundCount = this.frameCount();
    }
    _lookAroundCount++;
    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), isRightHanded() ? -deltaY : deltaY);
    Quaternion rotY = new Quaternion(eye().displacement(_upVector), -deltaX);
    return Quaternion.multiply(rotY, rotX);
  }

  //Replace previous call with the following two to preserve the upVector param.
  /*
  protected Quaternion _lookAround(float deltaX, float deltaY, float sensitivity) {
    if (is2D()) {
      System.out.println("Warning: lookAround is only available in 3D");
      return new Quaternion();
    }
    if(frameCount() > _lookAroundCount) {
      _upVector = eye().yAxis();
      _lookAroundCount = this.frameCount();
    }
    _lookAroundCount++;
    return _lookAround(deltaX, deltaY, _upVector, sensitivity);
  }

  protected Quaternion _lookAround(float deltaX, float deltaY, Vector upVector, float sensitivity) {
    if (is2D()) {
      System.out.println("Warning: lookAround is only available in 3D");
      return new Quaternion();
    }
    deltaX *= -sensitivity;
    deltaY *= sensitivity;
    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), isRightHanded() ? -deltaY : deltaY);
    Quaternion rotY = new Quaternion(eye().displacement(upVector), deltaX);
    return Quaternion.multiply(rotY, rotX);
  }
  // */

  /**
   * Same as {@code rotateCAD(roll, pitch, new Vector(0, 1, 0))}.
   *
   * @see #rotateCAD(float, float, Vector)
   */
  public void rotateCAD(float roll, float pitch) {
    rotateCAD(roll, pitch, new Vector(0, 1, 0));
  }

  /**
   * Defines an axis which the eye rotates around. The eye can rotate left or right around
   * this axis. It can also be moved up or down to show the 'top' and 'bottom' views of the scene.
   * As a result, the {@code upVector} will always appear vertical in the scene, and the horizon
   * is preserved and stays projected along the eye's horizontal axis.
   * <p>
   * This method requires calling {@code scene.eye().setYAxis(upVector)} (see
   * {@link Node#setYAxis(Vector)}) and {@link #fit()} first.
   *
   * @see #rotateCAD(float, float)
   */
  public void rotateCAD(float roll, float pitch, Vector upVector) {
    spin(_rotateCAD(roll, pitch, upVector), eye());
  }

  /**
   * Computes and returns the quaternion used by {@link #rotateCAD(float, float, Vector)}.
   */
  protected Quaternion _rotateCAD(float roll, float pitch, Vector upVector) {
    if (is2D()) {
      System.out.println("Warning: rotateCAD is only available in 3D");
      return new Quaternion();
    }
    Vector eyeUp = eye().displacement(upVector);
    return Quaternion.multiply(new Quaternion(eyeUp, eyeUp.y() < 0.0f ? roll : -roll), new Quaternion(new Vector(1.0f, 0.0f, 0.0f), isRightHanded() ? -pitch : pitch));
  }

  /**
   * Same as {@code control(defaultNode(), gesture)}. This method implements application control with
   * the {@link #defaultNode()} which requires overriding {@link Node#interact(Object...)}.
   *
   * @see #defaultNode()
   * @see #control(Node, Object...)
   */
  public void defaultHIDControl(Object... gesture) {
    control(defaultNode(), gesture);
  }

  /**
   * Same as {@code control(defaultNode(hid), gesture)}. This method implements application control with
   * the {@link #defaultNode(String)} which requires overriding {@link Node#interact(Object...)}.
   *
   * @see #defaultNode(String)
   * @see #control(Node, Object...)
   */
  public void control(String hid, Object... gesture) {
    control(defaultNode(hid), gesture);
  }

  /**
   * Same as {@code node.interact(gesture)}.
   *
   * @see #defaultHIDControl(Object...)
   * @see #control(String, Object...)
   * @see Node#interact(Object...)
   */
  public void control(Node node, Object... gesture) {
    node.interact(gesture);
  }

  /*
  // nice interactivity examples: spinning (spin + timer), moveForward, moveBackward, spinX/Y/Z
  // screenRotate/Translate.

  public void zoom(float delta) {
    zoom(null, delta);
  }

  public void zoom(String hid, float delta) {
    zoom(delta, defaultNode(hid));
  }

  public void zoom(float delta, Node node) {
    translate(0, 0, delta, node);
  }

  public void spin(Point point1, Point point2, Point center) {
    spin(point1, point2, center, defaultNode());
  }

  public void spin(Point point1, Point point2, Point center, Node node) {
    if (node == null)
      throw new RuntimeException("spin(point1, point2, center, node) requires a non-null node param");
    spin(_spin(point1, point2, center, 1, node), node);
  }

  public void spin(Point point1, Point point2, Point center, float sensitivity) {
    spin(point1, point2, center, sensitivity, defaultNode());
  }

  public void spin(Point point1, Point point2, Point center, float sensitivity, Node node) {
    if (node == null)
      throw new RuntimeException("spin(point1, point2, center, sensitivity, node) requires a non-null node param");
    spin(_spin(point1, point2, center, sensitivity, node), node);
  }

  public void spinX(float roll) {
    spinX(roll, 1);
  }

  public void spinX(float roll, float sensitivity) {
    spin(_rotate(roll, 0, 0, sensitivity, eye()), eye());
  }

  public void spinY(float pitch) {
    spinY(pitch, 1);
  }

  public void spinY(float pitch, float sensitivity) {
    spin(_rotate(0, pitch, 0, sensitivity, eye()), eye());
  }

  public void spinZ(float yaw) {
    spinZ(yaw, 1);
  }

  public void spinZ(float yaw, float sensitivity) {
    spin(_rotate(0, 0, yaw, sensitivity, eye()), eye());
  }

  scene.spinX(event.getCount(), 20*PI / width); can be emulated through either:
  1. scene.eye().rotate(new Quaternion(event.getCount() * 20*PI / width,0,0), scene.anchor()); or,
  2. scene.eye().rotate(new Quaternion(new Vector(1,0,0), event.getCount() * 20*PI / width), scene.anchor());

  // Overkill 2: simply accomplish these with constraints
  public void screenRotate(Point point1, Point point2) {
    screenRotate(point1, point2, 1);
  }

  public void screenRotate(Point point1, Point point2, float sensitivity) {
    screenRotate(point1, point2, sensitivity, eye());
  }

  public void screenRotate(Point point1, Point point2, Node node) {
    screenRotate(point1, point2, 1, node);
  }

  public void screenRotate(Point point1, Point point2, float sensitivity, Node node) {
    spin(_spin2(point1, point2, sensitivity, node), node);
  }

  protected Quaternion _spin2(Point point1, Point point2, float sensitivity, Node node) {
    Quaternion quaternion;
    Vector vector;
    float x = point2.x();
    float y = point2.y();
    float prevX = point1.x();
    float prevY = point1.y();
    float angle;
    if (isEye(node)) {
      vector = screenLocation(anchor());
      angle = (float) Math.atan2(y - vector._vector[1], x - vector._vector[0]) - (float) Math
          .atan2(prevY - vector._vector[1], prevX - vector._vector[0]);
      if (isLeftHanded())
        angle = -angle;
      quaternion = new Quaternion(new Vector(0.0f, 0.0f, 1.0f), angle);
    } else {
      vector = screenLocation(node.position());
      float prev_angle = (float) Math.atan2(prevY - vector._vector[1], prevX - vector._vector[0]);
      angle = (float) Math.atan2(y - vector._vector[1], x - vector._vector[0]);
      Vector axis = node.displacement(eye().orientation().rotate(new Vector(0.0f, 0.0f, -1.0f)));
      if (isRightHanded())
        quaternion = new Quaternion(axis, angle - prev_angle);
      else
        quaternion = new Quaternion(axis, prev_angle - angle);
    }
    return quaternion;
  }
  */
}
