/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.core;

import remixlab.input.Agent;
import remixlab.input.InputHandler;
import remixlab.primitives.*;
import remixlab.timing.Animator;
import remixlab.timing.TimingHandler;
import remixlab.timing.TimingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A 2D or 3D scene graph.
 * <p>
 * Instantiated graph {@link Node}s form a graph-tree of
 * transformations which may be traverse with {@link #traverse()}. The node
 * collection belonging to the graph may be retrieved with {@link #nodes()}. The
 * graph provides other useful routines to handle the hierarchy, such as
 * {@link #pruneBranch(Node)}, {@link #appendBranch(List)},
 * {@link #isNodeReachable(Node)}, {@link #branch(Node)}, and
 * {@link #clear()}.
 * <p>
 * Each graph provides the following main object instances:
 * <ol>
 * <li>An {@link #eye()} which represents the 2D/3D controlling object.</li>
 * <li>A {@link #timingHandler()} which control (single-threaded) timing operations. For
 * details please refer to the {@link remixlab.timing.TimingHandler} class.</li>
 * <li>An {@link #inputHandler()} which handles all user input through
 * {@link Agent}s (for details please refer to the
 * {@link InputHandler} class). The {@link #inputHandler()} holds agents which should
 * be instantiated by derived classes at construction time.</li>
 * <li>A {@link #matrixHandler()} which handles matrix operations either through the
 * {@link MatrixHandler} or through a third party matrix stack
 * (like it's done with Processing). For details please refer to the
 * {@link MatrixHandler} interface.</li>
 * </ol>
 */
//TODO
// (decide) Remove printing stuff (vector, map, ...) and warnings (hashmap).
public class Graph {
  // 1. Eye
  protected Frame _eye;
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
  // rescale ortho when anchor changes
  protected float _rapK = 1;
  // Inverse the direction of an horizontal mouse motion. Depends on the
  // projected
  // screen orientation of the vertical axis when the mouse button is pressed.
  public boolean _cadRotationIsReversed;
  // handed and screen drawing
  protected boolean _rightHanded;
  protected int _startCoordCalls;
  // size and dim
  protected int _width, _height;

  // 2. Matrix helper
  protected MatrixHandler _matrixHandler;

  // 3. Handlers
  protected TimingHandler _timingHandler;
  protected InputHandler _inputHandler;

  // 4. Graph
  protected List<Node> _seeds;
  protected int _nodeCount;
  protected long _lastNonEyeUpdate = 0;

  /**
   * Enumerates the different visibility states an object may have respect to the eye
   * boundary.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  Type _type;

  /**
   * Enumerates the two possible types of Camera.
   * <p>
   * This type mainly defines different camera projection matrix. Many other methods take
   * this Type into account.
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
   * Default constructor which defines a right-handed OpenGL compatible Scene with its own
   * {@link MatrixHandler}. The constructor also instantiates
   * the {@link #inputHandler()} and the {@link #timingHandler()}, and sets the AXES and
   * GRID visual hint flags.
   * <p>
   * Third party (concrete) Scenes should additionally:
   * <ol>
   * <li>(Optionally) Define a custom {@link #matrixHandler()}. Only if the target platform
   * (such as Processing) provides its own matrix handling.</li>
   * <li>Call {@link #setEye(Frame)} to set the {@link #eye()}, once it's known if the Scene
   * {@link #is2D()} or {@link #is3D()}.</li>
   * <li>Instantiate some agents and enable them (register them at the
   * {@link #inputHandler()}).</li>
   * </ol>
   *
   * @see #timingHandler()
   * @see #inputHandler()
   * @see #setMatrixHandler(MatrixHandler)
   * @see #setRightHanded()
   * @see #setEye(Frame)
   */
  //TODO graph.get()
  public Graph(Type type, int width, int height) {
    setType(type);
    setWidth(width);
    setHeight(height);

    _seeds = new ArrayList<Node>();
    _timingHandler = new TimingHandler();
    _inputHandler = new InputHandler();

    setRadius(100);
    setCenter(new Vector());
    _anchor = center().get();
    _interpolator = new Interpolator(this);
    setEye(new Frame());
    fitBall();

    setMatrixHandler(new MatrixHandler(this));
    setRightHanded();

    enableBoundaryEquations(false);

    setZNearCoefficient(0.005f);
    setZClippingCoefficient((float) Math.sqrt(3.0f));
  }

  // Dimensions stuff

  /**
   * Returns the {@link #width()} to {@link #height()} aspect ratio of the display window.
   */
  public float aspectRatio() {
    return (float) width() / (float) height();
  }

  /**
   * @return width of the screen window.
   */
  public int width() {
    return _width;
  }

  /**
   * @return height of the screen window.
   */
  public int height() {
    return _height;
  }

  /**
   * Sets eye {@link #width()} and {@link #height()} (expressed in pixels).
   * <p>
   * Non-positive dimension are silently replaced by a 1 pixel value to ensure boundary
   * coherence.
   */
  public void setWidth(int width) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    if ((width != width()))
      _modified();
    _width = width > 0 ? width : 1;
  }

  public void setHeight(int height) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    if (height != height())
      _modified();
    _height = height > 0 ? height : 1;
  }

  // Type handling stuff

  /**
   * Returns the Camera.Type.
   * <p>
   * Set by {@link #setType(Type)}.
   * <p>
   * A {@link Type#PERSPECTIVE} Camera uses a classical
   * projection mainly defined by its {@link #fieldOfView()}.
   * <p>
   * With a {@link Type#ORTHOGRAPHIC} {@link #type()}, the
   * {@link #fieldOfView()} is meaningless and the width and height of the Camera frustum
   * are inferred from the distance to the {@link #anchor()} using
   * {@link #boundaryWidthHeight()}.
   * <p>
   * Both types use {@link #zNear()} and {@link #zFar()} (to define their clipping planes)
   * and {@link #aspectRatio()} (for frustum shape).
   */
  public Type type() {
    return _type;
  }

  /**
   * Defines the Camera {@link #type()}.
   * <p>
   * Changing the Camera Type alters the viewport and the objects' size can be changed.
   * This method guarantees that the two frustum match in a plane normal to
   * {@link #viewDirection()}, passing through the arcball reference point.
   */
  public void setType(Type type) {
    if (type != type()) {
      _modified();
      this._type = type;
    }
  }

  /**
   * Returns the vertical field of view of the Camera (in radians) computed as
   * {@code 2.0f * (float) Math.atan(frame().magnitude())}.
   * <p>
   * Value is set using {@link #setFieldOfView(float)}. Default value is pi/3 radians.
   * This value is meaningless if the Camera {@link #type()} is
   * {@link Type#ORTHOGRAPHIC}.
   * <p>
   * The field of view corresponds the one used in {@code gluPerspective} (see manual). It
   * sets the Y (vertical) aperture of the Camera. The X (horizontal) angle is inferred
   * from the window aspect ratio (see {@link #aspectRatio()} and
   * {@link #horizontalFieldOfView()}).
   * <p>
   * Use {@link #setFieldOfView} to adapt the {@link #fieldOfView()} to a given graph.
   *
   * @see #setFieldOfView(float)
   */
  public float fieldOfView() {
    return 2.0f * (float) Math.atan(eye().magnitude());
  }

  /**
   * Same as {@code eye().setMagnitude((float) Math.tan(fov / 2.0f))}.
   * <p>
   * Sets the so called vertical field-of-view of an eye provided this frame
   * is set as a Graph eye.
   *
   * @see Frame#setMagnitude(float)
   */
  public void setFieldOfView(float fov) {
    eye().setMagnitude((float) Math.tan(fov / 2.0f));
  }

  /**
   * Returns the horizontal field of view of the Camera (in radians).
   * <p>
   * Value is set using {@link #setHorizontalFieldOfView(float)} or
   * {@link #setFieldOfView(float)}. These values are always linked by:
   * {@code horizontalFieldOfView() = 2.0 * atan ( tan(fieldOfView()/2.0) * aspectRatio() )}
   * .
   */
  public float horizontalFieldOfView() {
    return 2.0f * (float) Math.atan((eye() == null ? 1 : eye().magnitude()) * aspectRatio());
  }

  /**
   * Changes the Camera {@link Graph#fieldOfView()} so that the entire graph (defined by
   * {@link #center()} and
   * {@link Graph#radius()} is visible from the Camera
   * {@link Node#position()}.
   * <p>
   * The eye position and orientation of the Camera are not _modified and
   * you first have to orientate the Camera in order to actually see the graph (see
   * {@link Graph#lookAt(Vector)}, {@link Graph#fitBall()} or {@link Graph#fitBall(Vector, float)}).
   * <p>
   * This method is especially useful for <i>shadow maps</i> computation. Use the Camera
   * positioning tools ({@link Graph#lookAt(Vector)}) to position a
   * Camera at the light position. Then use this method to define the
   * {@link Graph#fieldOfView()} so that the shadow map resolution is optimally used:
   * <p>
   * {@code // The light camera needs size hints in order to optimize its
   * fieldOfView} <br>
   * {@code lightCamera.setSceneRadius(sceneRadius());} <br>
   * {@code lightCamera.setSceneCenter(sceneCenter());} <br>
   * {@code // Place the light camera} <br>
   * {@code lightCamera.setPosition(lightFrame.position());} <br>
   * {@code lightCamera.lookAt(sceneCenter());} <br>
   * {@code lightCamera.setFieldOfView();} <br>
   * <p>
   * <b>Attention:</b> The {@link Graph#fieldOfView()} is clamped to M_PI/2.0. This happens
   * when the Camera is at a distance lower than sqrt(2.0) * sceneRadius() from the
   * sceneCenter(). It optimizes the shadow map resolution, although it may miss some
   * parts of the graph.
   */
  public void setFieldOfView() {
    if (Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) > (float) Math.sqrt(2.0f) * radius())
      setFieldOfView(2.0f * (float) Math.asin(radius() / Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())));
    else
      setFieldOfView((float) Math.PI / 2.0f);
  }

  /**
   * Sets the {@link Graph#horizontalFieldOfView()} of the Camera (in radians).
   * <p>
   * {@link Graph#horizontalFieldOfView()} and {@link Graph#fieldOfView()} are linked by the
   * {@link Graph#aspectRatio()}. This method actually calls
   * {@code setFieldOfView(( 2.0 * atan (tan(hfov / 2.0) / aspectRatio()) ))} so that a
   * call to {@link Graph#horizontalFieldOfView()} returns the expected value.
   */
  public void setHorizontalFieldOfView(float hfov) {
    setFieldOfView(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()));
  }

  /**
   * Returns the near clipping plane distance used by the Camera projection matrix in
   * graph (world) units.
   * <p>
   * The clipping planes' positions depend on the {@link #radius()} and
   * {@link #center()} rather than being fixed small-enough and large-enough values.
   * A good graph dimension approximation will hence result in an optimal precision of the
   * z-buffer.
   * <p>
   * The near clipping plane is positioned at a distance equal to
   * {@link #zClippingCoefficient()} * {@link #radius()} in front of the
   * {@link #center()}: {@code Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) -
   * zClippingCoefficient() * sceneRadius()}
   * <p>
   * In order to prevent negative or too small {@link #zNear()} values (which would
   * degrade the z precision), {@link #zNearCoefficient()} is used when the eye is
   * inside the {@link #radius()} sphere:
   * <p>
   * {@code zMin = zNearCoefficient() * zClippingCoefficient() * sceneRadius();} <br>
   * {@code zNear = zMin;}<br>
   * {@code With an ORTHOGRAPHIC type, the value is simply clamped to 0.0} <br>
   * <p>
   * See also the {@link #zFar()}, {@link #zClippingCoefficient()} and
   * {@link #zNearCoefficient()} documentations.
   * <p>
   * If you need a completely different zNear computation, overload the {@link #zNear()}
   * and {@link #zFar()} methods.
   * <p>
   * <b>Attention:</b> The value is always positive although the clipping plane is
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
   * Returns the far clipping plane distance used by the Camera projection matrix in graph
   * (world) units.
   * <p>
   * The far clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * sceneRadius()} behind the {@link #center()}:
   * <p>
   * {@code zFar = Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) + zClippingCoefficient()*sceneRadius()}
   *
   * @see #zNear()
   */
  public float zFar() {
    return Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) + zClippingCoefficient() * radius();
  }

  /**
   * Returns the coefficient which is used to set {@link #zNear()} when the Camera is
   * inside the sphere defined by {@link #center()} and
   * {@link #zClippingCoefficient()} * {@link #radius()}.
   * <p>
   * In that case, the {@link #zNear()} value is set to
   * {@code zNearCoefficient() * zClippingCoefficient() * sceneRadius()}. See the
   * {@code zNear()} documentation for details.
   * <p>
   * Default value is 0.005, which is appropriate for most applications. In case you need
   * a high dynamic ZBuffer precision, you can increase this value (~0.1). A lower value
   * will prevent clipping of very close objects at the expense of a worst Z precision.
   * <p>
   * Only meaningful when Camera type is PERSPECTIVE.
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
   * {@code zClippingCoefficient() * sceneRadius()} in front of (resp. behind) the
   * {@link #center()}. This guarantees an optimal use of the z-buffer range and
   * minimizes aliasing. See the {@link #zNear()} and {@link #zFar()} documentations.
   * <p>
   * Default value is square root of 3.0 (so that a cube of size 2*{@link #radius()}
   * is not clipped).
   * <p>
   * However, since the {@link #radius()} is used for other purposes (see
   * showEntireScene(), flySpeed(), ...) and you may want to change this value to define
   * more precisely the location of the clipping planes. See also
   * {@link #zNearCoefficient()}.
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

  /**
   * Returns the {@code halfWidth} and {@code halfHeight} of the eye boundary.
   * While first element holds {@code halfWidth}, the second one
   * holds {@code halfHeight}. Values are computed as:
   * {@code _rescalingFactor() * (eye().magnitude() * width() / 2)}
   * and {@code _rescalingFactor() * (eye().magnitude() * height() / 2)}
   * respectively.
   * <p>
   * These values are valid for 2d Windows and ortho Cameras (but not persp) and they are
   * expressed in virtual graph units.
   * <p>
   * In the case of ortho Cameras these values are proportional to the Camera (z
   * projected) distance to the {@link #anchor()}. When zooming on the object, the Camera
   * is translated forward and its boundary is narrowed, making the object appear bigger
   * on screen, as intuitively expected.
   * <p>
   * Overload this method to change this behavior if desired.
   *
   * @see #_rescalingFactor()
   */
  public float[] boundaryWidthHeight() {
    float[] target = new float[2];
    if ((target == null) || (target.length != 2)) {
      target = new float[2];
    }

    float orthoCoef = _rescalingFactor();

    target[0] = orthoCoef * (eye().magnitude() * width() / 2);
    target[1] = orthoCoef * (eye().magnitude() * height() / 2);

    return target;
  }

  /**
   * Simply returns {@code 1} which is valid for 2d Windows.
   * <p>
   * In 3D returns a value proportional to the Camera (z projected) distance to the
   * {@link #anchor()} so that when zooming on the object, the ortho Camera is translated
   * forward and its boundary is narrowed, making the object appear bigger on screen, as
   * intuitively expected.
   * <p>
   * Value is computed as: {@code 2 * Vector.scalarProjection(Vector.subtract(eye().position(), anchor()), eye().zAxis()) / screenHeight()}.
   *
   * @see #boundaryWidthHeight()
   */
  protected float _rescalingFactor() {
    if (is2D())
      return 1.0f;
    float toAnchor = Vector.scalarProjection(Vector.subtract(eye().position(), anchor()), eye().zAxis());
    float epsilon = 0.0001f;
    return (2 * (toAnchor == 0 ? epsilon : toAnchor) * _rapK / height());
  }

  // Graph and nodes stuff

  /**
   * Returns the top-level nodes (those which reference is null).
   * <p>
   * All leading nodes are also reachable by the {@link #traverse()} algorithm for
   * which they are the seeds.
   *
   * @see #nodes()
   * @see #isNodeReachable(Node)
   * @see #pruneBranch(Node)
   */
  public List<Node> leadingNodes() {
    return _seeds;
  }

  /**
   * Returns {@code true} if the node is top-level.
   */
  protected boolean _isLeadingNode(Node node) {
    for (Node _node : leadingNodes())
      if (_node == node)
        return true;
    return false;
  }

  /**
   * Add the node as top-level if its reference node is null and it isn't already added.
   */
  protected boolean _addLeadingNode(Node node) {
    if (node == null || node.reference() != null)
      return false;
    if (_isLeadingNode(node))
      return false;
    return leadingNodes().add(node);
  }

  /**
   * Removes the leading node if present. Typically used when re-parenting the node.
   */
  protected boolean _removeLeadingNode(Node node) {
    boolean result = false;
    Iterator<Node> it = leadingNodes().iterator();
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
   * Traverse the node hierarchy, successively applying the local transformation defined
   * by each traversed node, and calling {@link Node#visit()} on it.
   * <p>
   * Note that only reachable nodes are visited by this algorithm.
   * <p>
   * <b>Attention:</b> this method should be called after {@link MatrixHandler#bind()} (i.e.,
   * eye update) and before any other transformation of the modelview takes place.
   *
   * @see #isNodeReachable(Node)
   * @see #pruneBranch(Node)
   */
  public void traverse() {
    for (Node node : leadingNodes())
      _visitNode(node);
  }

  /**
   * Used by the traverse node tree algorithm.
   */
  protected void _visitNode(Node node) {
    pushModelView();
    applyTransformation(node);
    node.visitCallback();
    for (Node child : node.children())
      _visitNode(child);
    popModelView();
  }

  /**
   * Same as {@code for(Node node : leadingNodes()) pruneBranch(node)}.
   *
   * @see #pruneBranch(Node)
   */
  public void clear() {
    for (Node node : leadingNodes())
      pruneBranch(node);
  }

  /**
   * Make all the nodes in the {@code node} branch eligible for garbage collection.
   * <p>
   * A call to {@link #isNodeReachable(Node)} on all {@code node} descendants
   * (including {@code node}) will return false, after issuing this method. It also means
   * that all nodes in the {@code node} branch will become unreachable by the
   * {@link #traverse()} algorithm.
   * <p>
   * nodes in the {@code node} branch will also be removed from all the agents currently
   * registered in the {@link #inputHandler()}.
   * <p>
   * To make all the nodes in the branch reachable again, first cache the nodes
   * belonging to the branch (i.e., {@code branch=pruneBranch(node)}) and then call
   * {@link #appendBranch(List)} on the cached branch. Note that calling
   * {@link Node#setReference(Node)} on a
   * node belonging to the pruned branch will become reachable again by the traversal
   * algorithm. In this case, the node should be manually added to some agents to
   * interactively handle it.
   * <p>
   * Note that if node is not reachable ({@link #isNodeReachable(Node)}) this
   * method returns {@code null}.
   * <p>
   * When collected, pruned nodes behave like {@link Node},
   * otherwise they are eligible for garbage collection.
   *
   * @see #clear()
   * @see #appendBranch(List)
   * @see #isNodeReachable(Node)
   */
  public ArrayList<Node> pruneBranch(Node node) {
    if (!isNodeReachable(node))
      return null;
    ArrayList<Node> list = new ArrayList<Node>();
    _collectNodes(list, node);
    for (Node _node : list) {
      inputHandler().removeGrabber(_node);
      if (_node.reference() != null)
        _node.reference()._removeChild(_node);
      else
        _removeLeadingNode(_node);
    }
    return list;
  }

  /**
   * Appends the branch which typically should come from the one pruned (and cached) with
   * {@link #pruneBranch(Node)}.
   * <p>
   * All nodes belonging to the branch are automatically added to all graph agents.
   * <p>
   * {@link #pruneBranch(Node)}
   */
  public void appendBranch(List<Node> branch) {
    if (branch == null)
      return;
    for (Node node : branch) {
      inputHandler().addGrabber(node);
      if (node.reference() != null)
        node.reference()._addChild(node);
      else
        _addLeadingNode(node);
    }
  }

  /**
   * Returns {@code true} if the node is reachable by the {@link #traverse()}
   * algorithm and {@code false} otherwise.
   * <p>
   * Nodes are make unreachable with {@link #pruneBranch(Node)} and reachable
   * again with
   * {@link Node#setReference(Node)}.
   *
   * @see #traverse()
   * @see #nodes()
   */
  public boolean isNodeReachable(Node node) {
    if (node == null)
      return false;
    return node.reference() == null ? _isLeadingNode(node) : node.reference()._hasChild(node);
  }

  /**
   * Returns a list of all the nodes that are reachable by the {@link #traverse()}
   * algorithm.
   *
   * @see #isNodeReachable(Node)
   * @see Node#isEye()
   */
  public ArrayList<Node> nodes() {
    ArrayList<Node> list = new ArrayList<Node>();
    for (Node node : leadingNodes())
      _collectNodes(list, node);
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. When {@code eyeframes} is
   * {@code true} eye-nodes will also be collected. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isNodeReachable(Node)
   */
  public ArrayList<Node> branch(Node node) {
    ArrayList<Node> list = new ArrayList<Node>();
    _collectNodes(list, node);
    return list;
  }

  /**
   * Returns a straight path of nodes between {@code tail} and {@code tip}. When {@code eyeframes} is
   * {@code true} eye-nodes will also be included.
   * <p>
   * If {@code tip} is descendant of {@code tail} the returned list will include both of them. Otherwise it will be empty.
   */
  public ArrayList<Node> branch(Node tail, Node tip) {
    ArrayList<Node> list = new ArrayList<Node>();
    //1. Check if tip is a tail descendant
    boolean desc = false;
    ArrayList<Node> descList = branch(tail);
    for (Node node : descList)
      if (node == tip) {
        desc = true;
        break;
      }
    //2. If so, return the path between the two
    if (desc) {
      Node _tip = tip;
      while (_tip != tail) {
        list.add(0, _tip);
        _tip = _tip.reference();
      }
      list.add(0, tail);
    }
    return list;
  }

  /**
   * Collects {@code node} and all its descendant nodes. When {@code eyeframes} is
   * {@code true} eye-nodes will also be collected. Note that for a node to be collected
   * it must be reachable.
   *
   * @see #isNodeReachable(Node)
   */
  protected void _collectNodes(List<Node> list, Node node) {
    if (node == null)
      return;
    list.add(node);
    for (Node child : node.children())
      _collectNodes(list, child);
  }

  // Input stuff

  /**
   * Returns the graph {@link InputHandler}.
   */
  public InputHandler inputHandler() {
    return _inputHandler;
  }

  // TODO docs missing
  // param instead of Node. It allows to manipulate the eye() instance more easily sometimes
  public void setDefaultNode(Node node) {
    inputHandler().setDefaultGrabber(node);
  }

  public void resetInputNode() {
    inputHandler().resetTrackedGrabber();
  }

  public void shiftDefaultNode(Node node1, Node node2) {
    inputHandler().shiftDefaultGrabber(node1, node2);
  }

  public boolean isInputNode(Node node) {
    return inputHandler().isInputGrabber(node);
  }

  public boolean registerAgent(Agent agent) {
    return inputHandler().registerAgent(agent);
  }

  public boolean isAgentRegistered(Agent agent) {
    return inputHandler().isAgentRegistered(agent);
  }

  public boolean unregisterAgent(Agent agent) {
    return inputHandler().unregisterAgent(agent);
  }

  public void unregisterAgents() {
    inputHandler().unregisterAgents();
  }

  // Timing stuff

  /**
   * Returns the graph {@link TimingHandler}.
   */
  public TimingHandler timingHandler() {
    return _timingHandler;
  }

  /**
   * Returns the number of frames displayed since the graph was instantiated.
   * <p>
   * Use {@code TimingHandler.frameCount} to retrieve the number of nodes displayed since
   * the first graph was instantiated.
   */
  public long frameCount() {
    return timingHandler().frameCount();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerTask(_task)}.
   *
   * @see remixlab.timing.TimingHandler#registerTask(TimingTask)
   */
  public void registerTask(TimingTask task) {
    timingHandler().registerTask(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterTask(_task)}.
   */
  public void unregisterTask(TimingTask task) {
    timingHandler().unregisterTask(task);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isTaskRegistered(_task)}.
   */
  public boolean isTaskRegistered(TimingTask task) {
    return timingHandler().isTaskRegistered(task);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().registerAnimator(object)}.
   */
  public void registerAnimator(Animator animator) {
    timingHandler().registerAnimator(animator);
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code timingHandler().unregisterAnimator(object)}.
   *
   * @see remixlab.timing.TimingHandler#unregisterAnimator(Animator)
   */
  public void unregisterAnimator(Animator animator) {
    timingHandler().unregisterAnimator(animator);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code timingHandler().isAnimatorRegistered(object)}.
   *
   * @see remixlab.timing.TimingHandler#isAnimatorRegistered(Animator)
   */
  public boolean isAnimatorRegistered(Animator animator) {
    return timingHandler().isAnimatorRegistered(animator);
  }

  // Matrix and transformations stuff

  /**
   * Wrapper for {@link MatrixHandler#beginScreenCoordinates()}. Adds
   * exception when no properly closing the screen drawing with a call to
   * {@link #endScreenCoordinates()}.
   *
   * @see MatrixHandler#beginScreenCoordinates()
   */
  public void beginScreenCoordinates() {
    if (_startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenCoordinates() call followed by a "
          + "endScreenCoordinates() and they cannot be nested. Check your implementation!");

    _startCoordCalls++;
    _matrixHandler.beginScreenCoordinates();
  }

  /**
   * Wrapper for {@link MatrixHandler#endScreenCoordinates()} . Adds
   * exception if {@link #beginScreenCoordinates()} wasn't properly called before
   *
   * @see MatrixHandler#endScreenCoordinates()
   */
  public void endScreenCoordinates() {
    _startCoordCalls--;
    if (_startCoordCalls != 0)
      throw new RuntimeException("There should be exactly one beginScreenCoordinates() call followed by a "
          + "endScreenCoordinates() and they cannot be nested. Check your implementation!");

    _matrixHandler.endScreenCoordinates();
  }

  /**
   * Computes the projection matrix associated with the eye.
   * <p>
   * If eye is a 3D PERSPECTIVE Camera, defines a projection matrix using the
   * {@link #fieldOfView()}, {@link #aspectRatio()},
   * {@link #zNear()} and
   * {@link #zFar()} parameters. If eye is a 3D ORTHOGRAPHIC
   * Camera, the frustum's width and height are set using
   * {@link #boundaryWidthHeight()}. Both types use
   * {@link #zNear()} and
   * {@link #zFar()} to place clipping planes. These values
   * are determined from sceneRadius() and sceneCenter() so that they best fit the graph
   * size.
   * <p>
   * <b>Note:</b> You must call this method if your eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   */
  //TODO pass a Matrix param!
  public Matrix computeProjection() {
    Matrix m = new Matrix();

    float ZNear = zNear();
    float ZFar = zFar();

    switch (type()) {
      case PERSPECTIVE:
        // #CONNECTION# all non null coefficients were set to 0.0 in constructor.
        m._matrix[0] = 1 / (eye().magnitude() * this.aspectRatio());
        m._matrix[5] = 1 / (isLeftHanded() ? -eye().magnitude() : eye().magnitude());
        m._matrix[10] = (ZNear + ZFar) / (ZNear - ZFar);
        m._matrix[11] = -1.0f;
        m._matrix[14] = 2.0f * ZNear * ZFar / (ZNear - ZFar);
        m._matrix[15] = 0.0f;
        // same as gluPerspective( 180.0*fieldOfView()/M_PI, aspectRatio(), zNear(), zFar() );
        break;
      case TWO_D:
      case ORTHOGRAPHIC:
        float[] wh = boundaryWidthHeight();
        m._matrix[0] = 1.0f / wh[0];
        m._matrix[5] = (isLeftHanded() ? -1.0f : 1.0f) / wh[1];
        m._matrix[10] = -2.0f / (ZFar - ZNear);
        m._matrix[11] = 0.0f;
        m._matrix[14] = -(ZFar + ZNear) / (ZFar - ZNear);
        m._matrix[15] = 1.0f;
        // same as glOrtho( -w, w, -h, h, zNear(), zFar() );
        break;
      case CUSTOM:
        return computeCustomProjection();
    }
    return m;
  }

  protected Matrix computeCustomProjection() {
    return new Matrix();
  }

  /**
   * Computes the View matrix associated with the {@link #eye()}) position and orientation.
   * <p>
   * This matrix converts from the world coordinates system to the eye coordinates system,
   * so that coordinates can then be projected on screen using the projection matrix (see
   * {@link #computeProjection()}).
   * <p>
   * <b>Note:</b> You must call this method if your eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   */
  //TODO pass a Matrix param!
  public Matrix computeView() {
    Matrix m = new Matrix();

    Quaternion q = eye().orientation();

    float q00 = 2.0f * q._quaternion[0] * q._quaternion[0];
    float q11 = 2.0f * q._quaternion[1] * q._quaternion[1];
    float q22 = 2.0f * q._quaternion[2] * q._quaternion[2];

    float q01 = 2.0f * q._quaternion[0] * q._quaternion[1];
    float q02 = 2.0f * q._quaternion[0] * q._quaternion[2];
    float q03 = 2.0f * q._quaternion[0] * q._quaternion[3];

    float q12 = 2.0f * q._quaternion[1] * q._quaternion[2];
    float q13 = 2.0f * q._quaternion[1] * q._quaternion[3];
    float q23 = 2.0f * q._quaternion[2] * q._quaternion[3];

    m._matrix[0] = 1.0f - q11 - q22;
    m._matrix[1] = q01 - q23;
    m._matrix[2] = q02 + q13;
    m._matrix[3] = 0.0f;

    m._matrix[4] = q01 + q23;
    m._matrix[5] = 1.0f - q22 - q00;
    m._matrix[6] = q12 - q03;
    m._matrix[7] = 0.0f;

    m._matrix[8] = q02 - q13;
    m._matrix[9] = q12 + q03;
    m._matrix[10] = 1.0f - q11 - q00;
    m._matrix[11] = 0.0f;

    Vector t = q.inverseRotate(eye().position());

    m._matrix[12] = -t._vector[0];
    m._matrix[13] = -t._vector[1];
    m._matrix[14] = -t._vector[2];
    m._matrix[15] = 1.0f;

    return m;
  }

  /**
   * Sets the {@link MatrixHandler} defining how dandelion matrices
   * are to be handled.
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
   * Wrapper for {@link MatrixHandler#pushModelView()}
   */
  public void pushModelView() {
    _matrixHandler.pushModelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#popModelView()}
   */
  public void popModelView() {
    _matrixHandler.popModelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#pushProjection()}
   */
  public void pushProjection() {
    _matrixHandler.pushProjection();
  }

  /**
   * Wrapper for {@link MatrixHandler#popProjection()}
   */
  public void popProjection() {
    _matrixHandler.popProjection();
  }

  /**
   * Wrapper for {@link MatrixHandler#translate(float, float)}
   */
  public void translate(float tx, float ty) {
    _matrixHandler.translate(tx, ty);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#translate(float, float, float)}
   */
  public void translate(float tx, float ty, float tz) {
    _matrixHandler.translate(tx, ty, tz);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotate(float)}
   */
  public void rotate(float angle) {
    _matrixHandler.rotate(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateX(float)}
   */
  public void rotateX(float angle) {
    _matrixHandler.rotateX(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateY(float)}
   */
  public void rotateY(float angle) {
    _matrixHandler.rotateY(angle);
  }

  /**
   * Wrapper for {@link MatrixHandler#rotateZ(float)}
   */
  public void rotateZ(float angle) {
    _matrixHandler.rotateZ(angle);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#rotate(float, float, float, float)}
   */
  public void rotate(float angle, float vx, float vy, float vz) {
    _matrixHandler.rotate(angle, vx, vy, vz);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float)}
   */
  public void scale(float s) {
    _matrixHandler.scale(s);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float, float)}
   */
  public void scale(float sx, float sy) {
    _matrixHandler.scale(sx, sy);
  }

  /**
   * Wrapper for {@link MatrixHandler#scale(float, float, float)}
   */
  public void scale(float x, float y, float z) {
    _matrixHandler.scale(x, y, z);
  }

  /**
   * Wrapper for {@link MatrixHandler#modelView()}
   */
  public Matrix modelView() {
    return _matrixHandler.modelView();
  }

  /**
   * Wrapper for {@link MatrixHandler#projection()}
   */
  public Matrix projection() {
    return _matrixHandler.projection();
  }

  /**
   * Wrapper for {@link MatrixHandler#projection()}
   */
  public Matrix view() {
    return _matrixHandler.view();
  }

  /**
   * Wrapper for {@link MatrixHandler#bindModelView(Matrix)}
   */
  public void setModelView(Matrix source) {
    _matrixHandler.bindModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#bindProjection(Matrix)}
   */
  public void setProjection(Matrix source) {
    _matrixHandler.bindProjection(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#applyModelView(Matrix)}
   */
  public void applyModelView(Matrix source) {
    _matrixHandler.applyModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#applyProjection(Matrix)}
   */
  public void applyProjection(Matrix source) {
    _matrixHandler.applyProjection(source);
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#isProjectionViewInverseCached()} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vector)}.
   *
   * @see #cacheProjectionViewInverse(boolean)
   * @see #unprojectedCoordinatesOf(Vector)
   */
  public boolean isProjectionViewInverseCached() {
    return _matrixHandler.isProjectionViewInverseCached();
  }

  /**
   * Wrapper for
   * {@link MatrixHandler#cacheProjectionViewInverse(boolean)} .
   * <p>
   * Use it only when continuously calling {@link #unprojectedCoordinatesOf(Vector)}.
   *
   * @see #isProjectionViewInverseCached()
   * @see #unprojectedCoordinatesOf(Vector)
   */
  public void cacheProjectionViewInverse(boolean optimise) {
    _matrixHandler.cacheProjectionViewInverse(optimise);
  }

  // Drawing stuff

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link MatrixHandler#bind()}</li>
   * <li>Calls {@link #updateBoundaryEquations()} if
   * {@link #areBoundaryEquationsEnabled()}</li>
   * </ol>
   *
   * @see #postDraw()
   */
  public void preDraw() {
    // 1. Eye, raster graph
    matrixHandler().bind();
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0)) {
      updateBoundaryEquations();
      _lastEqUpdate = TimingHandler.frameCount;
    }
  }

  /**
   * Called after your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link remixlab.timing.TimingHandler#handle()} and increments the the
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
    // 2. Agents
    inputHandler().handle();
  }

  // Eye stuff

  /**
   * Returns the associated eye. Never null.
   *
   * @see #setEye(Frame)
   */
  public Frame eye() {
    return _eye;
  }

  /**
   * Replaces the current {@link #eye()} with {@code e}.
   *
   * @see #eye()
   */
  public void setEye(Frame eye) {
    if (eye == null || _eye == eye)
      return;
    _eye = eye;
    _interpolator.setFrame(eye);
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
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public boolean isPointVisible(Vector point) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by pointIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    for (int i = 0; i < (is3D() ? 6 : 4); ++i)
      if (distanceToBoundary(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Returns {@link Visibility#VISIBLE},
   * {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the
   * sphere (of radius {@code radius} and center {@code center}) is visible, invisible, or
   * semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility ballVisibility(Vector center, float radius) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by sphereIsVisible) may be outdated. Please "
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
   * Returns {@link Visibility#VISIBLE},
   * {@link Visibility#INVISIBLE}, or
   * {@link Visibility#SEMIVISIBLE}, depending whether the
   * axis aligned box (defined by corners {@code p1} and {@code p2}) is visible,
   * invisible, or semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link Graph#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see Graph#enableBoundaryEquations()
   */
  public Visibility boxVisibility(Vector corner1, Vector corner2) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by aaBoxIsVisible) may be outdated. Please "
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
   * Returns the 6 plane equations of the eye boundary.
   * <p>
   * In 2D the four 4-component vectors of respectively correspond to the
   * left, right, top and bottom Window boundary lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * <p>
   * In 3D the six 4-component vectors of respectively correspond to the
   * left, right, near, far, top and bottom Camera frustum planes. Each vector holds a
   * plane equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   * <p>
   * This format is compatible with the {@code gl.glClipPlane()} function. One camera
   * frustum plane can hence be applied in an other viewer to visualize the culling
   * results:
   * <p>
   * {@code Retrieve place equations}<br>
   * {@code float [][] coef =
   * mainViewer.camera().getFrustumPlanesCoefficients();}<br>
   * {@code These two additional clipping planes (which must have been enabled)} <br>
   * {@code will reproduce the mainViewer's near and far clipping.}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE0, coef[2]);}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE1, coef[3]);}<br>
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link Graph#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations()
   */
  public float[][] computeBoundaryEquations() {
    _initCoefficients();
    return is3D() ? _computeBoundaryEquations3() : _computeBoundaryEquations2();
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

  protected float[][] _computeBoundaryEquations3() {
    // Computed once and for all
    Vector pos = eye().position();
    Vector viewDir = viewDirection();
    Vector up = upVector();
    Vector right = rightVector();

    float posViewDir = Vector.dot(pos, viewDir);

    switch (type()) {
      case PERSPECTIVE: {
        float hhfov = horizontalFieldOfView() / 2.0f;
        float chhfov = (float) Math.cos(hhfov);
        float shhfov = (float) Math.sin(hhfov);
        _normal[0] = Vector.multiply(viewDir, -shhfov);
        _normal[1] = Vector.add(_normal[0], Vector.multiply(right, chhfov));
        _normal[0] = Vector.add(_normal[0], Vector.multiply(right, -chhfov));
        _normal[2] = Vector.multiply(viewDir, -1);
        _normal[3] = viewDir;

        float hfov = fieldOfView() / 2.0f;
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

        float[] wh = boundaryWidthHeight();
        _distance[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh[0])), _normal[0]);
        _distance[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh[0])), _normal[1]);
        _distance[4] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh[1])), _normal[4]);
        _distance[5] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh[1])), _normal[5]);
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

  protected float[][] _computeBoundaryEquations2() {
    // Computed once and for all
    Vector pos = eye().position();
    Vector up = upVector();
    Vector right = rightVector();

    _normal[0] = Vector.multiply(right, -1);
    _normal[1] = right;
    _normal[2] = up;
    _normal[3] = Vector.multiply(up, -1);

    float[] wh = boundaryWidthHeight();

    _distance[0] = Vector.dot(Vector.subtract(pos, Vector.multiply(right, wh[0])), _normal[0]);
    _distance[1] = Vector.dot(Vector.add(pos, Vector.multiply(right, wh[0])), _normal[1]);
    _distance[2] = Vector.dot(Vector.add(pos, Vector.multiply(up, wh[1])), _normal[2]);
    _distance[3] = Vector.dot(Vector.subtract(pos, Vector.multiply(up, wh[1])), _normal[3]);

    for (int i = 0; i < 4; ++i) {
      _coefficients[i][0] = _normal[i]._vector[0];
      _coefficients[i][1] = _normal[i]._vector[1];
      // Change respect to Camera occurs here:
      _coefficients[i][2] = -_distance[i];
    }

    return _coefficients;
  }

  /**
   * Disables automatic update of the camera frustum plane equations every frame.
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
   * Enables automatic update of the camera frustum plane equations every frame.
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
   * Enables or disables automatic update of the eye boundary plane equations every frame
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
   * Updates the boundary plane equations according to the current eye setup, by simply
   * calling {@link #computeBoundaryEquations()}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * boundary equations to be updated only occasionally (rare). Use
   * {@link #enableBoundaryEquations()} which
   * automatically update the boundary equations every frame instead.
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #boundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public void updateBoundaryEquations() {
    computeBoundaryEquations();
  }

  /**
   * Returns the boundary plane equations.
   * <p>
   * The six 4-component vectors returned by this method, respectively correspond to the
   * left, right, near, far, top and bottom eye boundary planes. Each vector holds a plane
   * equation of the form:
   * <p>
   * {@code a*x + b*y + c*z + d = 0}
   * <p>
   * where {@code a}, {@code b}, {@code c} and {@code d} are the 4 components of each
   * vector, in that order.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with {@link #enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vector)
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float[][] boundaryEquations() {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    return _coefficients;
  }

  /**
   * Returns the signed distance between point {@code pos} and plane {@code index} in
   * Scene units. The distance is negative if the point lies in the planes's boundary
   * halfspace, and positive otherwise.
   * <p>
   * {@code index} is a value between {@code 0} and {@code 5} which respectively
   * correspond to the left, right, near, far, top and bottom eye boundary planes.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link #enableBoundaryEquations()}).
   *
   * @see #isPointVisible(Vector)
   * @see #ballVisibility(Vector, float)
   * @see #boxVisibility(Vector, Vector)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #boundaryEquations()
   * @see #enableBoundaryEquations()
   */
  public float distanceToBoundary(int index, Vector position) {
    if (!areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations (needed by distanceToBoundary) may be outdated. Please "
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
   * Use this method to _scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #center()} and
   * always directed along the screen vertical direction:
   * <p>
   * {@code beginShape(LINES);}<br>
   * {@code vertex(sceneCenter().x, sceneCenter().y, sceneCenter().z);}<br>
   * {@code Vector v = Vector.addGrabber(sceneCenter(), Vector.mult(upVector(), 20 * graphToPixelRatio(sceneCenter())));}
   * <br>
   * {@code vertex(v.x, v.y, v.z);}<br>
   * {@code endShape();}<br>
   */
  public float graphToPixelRatio(Vector position) {
    switch (type()) {
      case PERSPECTIVE:
        return 2.0f * Math.abs((eye().coordinatesOf(position))._vector[2] * eye().magnitude()) * (float) Math
            .tan(fieldOfView() / 2.0f) / height();
      case TWO_D:
      case ORTHOGRAPHIC:
        float[] wh = boundaryWidthHeight();
        return 2.0f * wh[1] / height();
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
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   * <p>
   * Vertices must given in clockwise order if
   * {@link Graph#isLeftHanded()} or in counter-clockwise
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
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
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
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
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
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
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
   * Convenience function that simply returns {@code projectedCoordinatesOf(src, null)}.
   *
   * @see #projectedCoordinatesOf(Vector, Frame)
   */
  public Vector projectedCoordinatesOf(Vector vector) {
    return projectedCoordinatesOf(vector, null);
  }

  /**
   * Returns the screen projected coordinates of a point {@code src} defined in the
   * {@code frame} coordinate system.
   * <p>
   * When {@code frame} is {@code null}, {@code src} is expressed in the world coordinate
   * system. See {@link #projectedCoordinatesOf(Vector)}.
   * <p>
   * The x and y coordinates of the returned Vector are expressed in pixel, (0,0) being the
   * upper left corner of the window. The z coordinate ranges between 0.0 (near plane) and
   * 1.0 (excluded, far plane). See the {@code gluProject} man page for details.
   *
   * @see #unprojectedCoordinatesOf(Vector, Frame)
   */
  public Vector projectedCoordinatesOf(Vector vector, Frame frame) {
    float xyz[] = new float[3];

    if (frame != null) {
      Vector tmp = frame.inverseCoordinatesOf(vector);
      _project(tmp._vector[0], tmp._vector[1], tmp._vector[2], xyz);
    } else
      _project(vector._vector[0], vector._vector[1], vector._vector[2], xyz);

    return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  // cached version
  protected boolean _project(float objx, float objy, float objz, float[] windowCoordinate) {
    Matrix projectionViewMatrix = matrixHandler().cacheProjectionView();

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
   * Convenience function that simply returns {@code unprojectedCoordinatesOf(src, null)}.
   * <p>
   * #see {@link #unprojectedCoordinatesOf(Vector, Frame)}
   */
  public Vector unprojectedCoordinatesOf(Vector vector) {
    return this.unprojectedCoordinatesOf(vector, null);
  }

  /**
   * Returns the world unprojected coordinates of a point {@code src} defined in the
   * screen coordinate system.
   * <p>
   * The {@code src.x} and {@code src.y} inputGrabber values are expressed in pixels, (0,0) being
   * the upper left corner of the window. The {@code src.z} is a depth value ranging in
   * [0..1] (near and far plane respectively). In 3D Note that {@code src.z} is not a
   * linear interpolation between {@link #zNear()} and
   * {@link #zFar()};
   * {@code src.z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z);} where {@code z}
   * is the distance from the point you _project to the camera, along the
   * {@link #viewDirection()} . See the {@code gluUnProject} man page for details.
   * <p>
   * The result is expressed in the {@code frame} coordinate system. When {@code frame} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code frame} hierarchy (i.e., when
   * {@link Frame#reference()} is non-null) is taken into
   * account.
   * <p>
   * {@link #projectedCoordinatesOf(Vector, Frame)} performs the inverse transformation.
   * <p>
   * This method only uses the intrinsic eye parameters (see {@link #view()},
   * {@link #projection()}, {@link #width()} and {@link #height()}) and is completely independent of
   * the Processing matrices. You can hence define a virtual eye and use this method to
   * compute un-projections out of a classical rendering context.
   * <p>
   * This method is not computationally optimized by default. If you call it several times with no
   * change in the matrices, you should buffer the entire inverse projection matrix (view,
   * projection) to _speed-up the queries. See {@link #cacheProjectionViewInverse(boolean)}.
   *
   * @see #projectedCoordinatesOf(Vector, Frame)
   * @see #setWidth(int)
   * @see #setHeight(int)
   */
  public Vector unprojectedCoordinatesOf(Vector vector, Frame frame) {
    float xyz[] = new float[3];
    // _unproject(src.vec[0], src.vec[1], src.vec[2], this.getViewMatrix(true),
    // this.getProjectionMatrix(true),
    // getViewport(), xyz);
    _unproject(vector._vector[0], vector._vector[1], vector._vector[2], xyz);
    if (frame != null)
      return frame.coordinatesOf(new Vector(xyz[0], xyz[1], xyz[2]));
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
  protected boolean _unproject(float winx, float winy, float winz, float[] objCoordinate) {
    Matrix projectionViewInverseMatrix;
    if (matrixHandler().isProjectionViewInverseCached())
      projectionViewInverseMatrix = matrixHandler().cacheProjectionViewInverse();
    else {
      projectionViewInverseMatrix = Matrix.multiply(matrixHandler().cacheProjection(), matrixHandler().cacheView());
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

  /**
   * Returns the radius of the graph observed by the eye in graph (world) units.
   * <p>
   * In the case of a 3D eye you need to provide such an approximation of the
   * graph dimensions so that the it can adapt its
   * {@link #zNear()} and
   * {@link #zFar()} values. See the {@link #center()}
   * documentation.
   * <p>
   * Note that {@link Graph#radius()} (resp.
   * {@link Graph#setRadius(float)} simply call this
   * method on its associated eye.
   *
   * @see #setBoundingBox(Vector, Vector)
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
   * @see #setBoundingBox(Vector, Vector)
   * @see #zNear()
   * @see #zFar()
   */
  public Vector center() {
    return _center;
  }

  /**
   * The point the eye revolves around with the ROTATE action binding. Defined in world
   * coordinate system.
   * <p>
   * Default value is the {@link #center()}.
   * <p>
   * <b>Attention:</b> {@link #setCenter(Vector)} changes this value.
   */
  public Vector anchor() {
    return _anchor;
  }

  /**
   * Sets the {@link #anchor()}, defined in the world coordinate system.
   */
  public void setAnchor(Vector anchor) {
    if (is2D()) {
      _anchor = anchor;
      _anchor.setZ(0);
    } else {
      float prevDist = Vector.scalarProjection(Vector.subtract(eye().position(), anchor()), eye().zAxis());
      this._anchor = anchor;
      float newDist = Vector.scalarProjection(Vector.subtract(eye().position(), anchor()), eye().zAxis());
      if (prevDist != 0 && newDist != 0)
        _rapK *= prevDist / newDist;
    }
  }

  /**
   * Sets the {@link #radius()} value in graph (world) units. Negative values are
   * ignored. It also sets {@link Node#flySpeed()} to 1% of {@link #radius()}.
   */
  public void setRadius(float radius) {
    if (radius <= 0.0f) {
      System.out.println("Warning: Scene radius must be positive - Ignoring value");
      return;
    }
    _radius = radius;
  }

  /**
   * Sets the {@link #center()} of the Scene.
   *
   * @see #setRadius(float)
   */
  public void setCenter(Vector center) {
    _center = center;
  }

  /**
   * Similar to {@link #setRadius(float)} and {@link #setCenter(Vector)}, but the
   * graph limits are defined by a (world axis aligned) bounding box.
   */
  public void setBoundingBox(Vector corner1, Vector corner2) {
    setCenter(Vector.multiply(Vector.add(corner1, corner2), 1 / 2.0f));
    setRadius(0.5f * (Vector.subtract(corner2, corner1)).magnitude());
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #eye()} (
   * {@code frame().inverseTransformOf(new Vector(0.0f, 0.0f, -1.0f))} ) whih in 2D always is
   * (0,0,-1)
   * <p>
   * In 3D change this value using
   * {@link #setViewDirection(Vector)}, {@link #lookAt(Vector)} or
   * {@link Node#setOrientation(Quaternion)} . It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    return eye().zAxis(false);
  }

  /**
   * Rotates the Camera so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The Camera {@link Node#position()} is not _modified. The Camera is rotated so that the
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
   * Rotates the Eye so that its {@link #upVector()} becomes {@code up} (defined in the
   * world coordinate system).
   * <p>
   * The Eye is rotated around an axis orthogonal to {@code up} and to the current
   * {@link #upVector()} direction.
   * <p>
   * Use this method in order to define the Eye horizontal plane.
   * <p>
   * When {@code noMove} is set to {@code false}, the orientation modification is
   * compensated by a translation, so that the {@link #anchor()} stays projected at the
   * same position on screen. This is especially useful when the Eye is an observer of the
   * graph (default action binding).
   * <p>
   * When {@code noMove} is true, the Eye {@link Node#position()} is left unchanged, which is
   * an intuitive behavior when the Eye is in first person mode.
   *
   * @see #lookAt(Vector)
   */
  public void setUpVector(Vector up, boolean noMove) {
    Quaternion q = new Quaternion(new Vector(0.0f, 1.0f, 0.0f), eye().transformOf(up));

    if (!noMove)
      eye().setPosition(Vector.subtract(anchor(), (Quaternion.multiply(eye().orientation(), q)).rotate(eye().coordinatesOf(anchor()))));

    eye().rotate(q);

    // Useful in fly mode to keep the horizontal direction.
    if (eye() instanceof Node)
      ((Node) eye())._updateUpVector();
  }

  /**
   * Returns the normalized up vector of the eye, defined in the world coordinate system.
   * <p>
   * Set using {@link #setUpVector(Vector)} or {@link Node#setOrientation(Quaternion)}. It is
   * orthogonal to {@link #viewDirection()} and to {@link #rightVector()}.
   * <p>
   * It corresponds to the Y axis of the associated {@link #eye()} (actually returns
   * {@code frame().yAxis()}
   */
  public Vector upVector() {
    return eye().yAxis();
  }

  /**
   * 2D Windows simply call {@code frame().setPosition(target.x(), target.y())}. 3D
   * Cameras set {@link Node#orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The Camera {@link Node#position()} is not _modified.
   * Simply {@link #setViewDirection(Vector)}).
   *
   * @see #at()
   * @see #setUpVector(Vector)
   * @see #fitBall()
   * @see #fitBall(Vector, float)
   * @see #fitBoundingBox(Vector, Vector)
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
   * Simply returns {@code frame().xAxis()}.
   */
  public Vector rightVector() {
    return eye().xAxis();
  }

  /**
   * 2D Windows return the postion. 3D Cameras return a point defined in the world
   * coordinate system where the camera is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   *
   * @see #lookAt(Vector)
   */
  public Vector at() {
    return Vector.add(eye().position(), viewDirection());
  }

  /**
   * Returns the {@link #eye()} {@link Interpolator} used by {@link #fitBallInterpolation()},
   * {@link #fitScreenRegionInterpolation(Rectangle)}, {@link #interpolateTo(Frame)}, etc.
   */
  public Interpolator interpolator() {
    return _interpolator;
  }

  /**
   * Convenience function that simply calls {@code interpolateTo(fr, 1)}.
   *
   * @see #interpolateTo(Frame, float)
   */
  //TODO needs testing, e.g., setAvatar
  public void interpolateTo(Frame frame) {
    interpolateTo(frame, 1);
  }

  /**
   * Smoothly interpolates the Eye on a KeyFrameInterpolator path so that it goes to
   * {@code fr}.
   * <p>
   * {@code fr} is expressed in world coordinates. {@code duration} tunes the
   * interpolation _speed.
   *
   * @see #interpolateTo(Frame)
   * @see #fitBallInterpolation()
   */
  public void interpolateTo(Frame frame, float duration) {
    _interpolator.stop();
    _interpolator.clear();
    _interpolator.addKeyFrame(eye().detach());
    _interpolator.addKeyFrame(frame, duration);
    _interpolator.start();
  }

  /**
   * Smoothly moves the Eye so that the rectangular screen region defined by
   * {@code rectangle} (pixel units, with origin in the upper left corner) fits the
   * screen.
   * <p>
   * The eye is translated (its {@link Frame#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>boundary</i> in 3D, it's the intersection of this boundary with a plane
   * (orthogonal to the {@link #viewDirection()} and passing through the
   * {@link #center()}) that is used to define the 3D rectangle that is eventually
   * fitted.
   */
  public void fitScreenRegionInterpolation(Rectangle rectangle) {
    _interpolator.stop();
    _interpolator.clear();
    _interpolator.addKeyFrame(eye().detach());
    Frame originalFrame = eye();
    Frame tempFrame = eye().detach();
    setEye(tempFrame);
    fitScreenRegion(rectangle);
    setEye(originalFrame);
    _interpolator.addKeyFrame(tempFrame);
    _interpolator.start();
  }

  /**
   * Interpolates the eye so that the entire graph fits the screen at the end.
   * <p>
   * The graph is defined by its {@link #center()} and its {@link #radius()}.
   * See {@link #fitBall()}.
   * <p>
   * The {@link Frame#orientation()} of the {@link #eye()} is not _modified.
   */
  public void fitBallInterpolation() {
    _interpolator.stop();
    _interpolator.clear();
    _interpolator.addKeyFrame(eye().detach());
    Frame originalFrame = eye();
    Frame tempFrame = eye().detach();
    setEye(tempFrame);
    fitBall();
    setEye(originalFrame);
    _interpolator.addKeyFrame(tempFrame);
    _interpolator.start();
  }

  /**
   * Same as {@code fitBall(center(), radius())}.
   *
   * @see #fitBall(Vector, float)
   * @see #center()
   * @see #radius()
   */
  public void fitBall() {
    fitBall(center(), radius());
  }

  /**
   * Moves the eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   * <p>
   * In 3D the Camera is simply translated along its {@link #viewDirection()} so that the
   * sphere fits the screen. Its {@link Node#orientation()} and its
   * {@link #fieldOfView()} are unchanged. You should
   * therefore orientate the Camera before you call this method.
   *
   * @see #lookAt(Vector)
   * @see #setUpVector(Vector, boolean)
   */
  public void fitBall(Vector center, float radius) {
    if (is2D()) {
      float size = Math.min(width(), height());
      eye().setMagnitude(2 * radius / size);
      lookAt(center);
      return;
    }
    float distance = 0.0f;
    switch (type()) {
      case PERSPECTIVE: {
        float yview = radius / (float) Math.sin(fieldOfView() / 2.0f);
        float xview = radius / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distance = Math.max(xview, yview);
        break;
      }
      case ORTHOGRAPHIC: {
        distance = Vector.dot(Vector.subtract(center, anchor()), viewDirection()) + (radius / eye().magnitude());
        break;
      }
    }
    Vector newPos = Vector.subtract(center, Vector.multiply(viewDirection(), distance));
    eye().setPosition(newPos);
  }

  /**
   * Moves the eye so that the (world axis aligned) bounding box ({@code min} ,
   * {@code max}) is entirely visible, using {@link #fitBall(Vector, float)}.
   */
  public void fitBoundingBox(Vector corner1, Vector corner2) {
    float diameter = Math.max(Math.abs(corner2._vector[1] - corner1._vector[1]), Math.abs(corner2._vector[0] - corner1._vector[0]));
    diameter = Math.max(Math.abs(corner2._vector[2] - corner1._vector[2]), diameter);
    fitBall(Vector.multiply(Vector.add(corner1, corner2), 0.5f), 0.5f * diameter);
  }

  /**
   * Moves the eye so that the rectangular screen region defined by {@code rectangle}
   * (pixel units, with origin in the upper left corner) fits the screen.
   * <p>
   * in 3D the Camera is translated (its {@link Node#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #center()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   */
  public void fitScreenRegion(Rectangle rectangle) {
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
      lookAt(unprojectedCoordinatesOf(new Vector(rectangle.centerX(), rectangle.centerY(), 0)));
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
        distX = Vector.distance(pointX, newCenter) / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distY = Vector.distance(pointY, newCenter) / (float) Math.sin(fieldOfView() / 2.0f);
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
   * Gives the coefficients of a 3D half-line passing through the Camera eye and pixel
   * (x,y). Origin in the upper left corner. Use {@link #height()} - _y to locate the
   * origin at the lower left corner.
   * <p>
   * The origin of the half line (eye position) is stored in {@code orig}, while
   * {@code dir} contains the properly oriented and normalized direction of the half line.
   * <p>
   * This method is useful for analytical intersection in a selection method.
   */
  public void convertClickToLine(Point pixelInput, Vector origin, Vector direction) {
    Point pixel = new Point(pixelInput.x(), pixelInput.y());

    // lef-handed coordinate system correction
    if (isLeftHanded())
      pixel.setY(height() - pixelInput.y());

    switch (type()) {
      case PERSPECTIVE:
        origin.set(eye().position());
        direction.set(new Vector(((2.0f * pixel.x() / width()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f) * aspectRatio(),
            ((2.0f * (height() - pixel.y()) / height()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f),
            -1.0f));
        direction.set(Vector.subtract(eye().inverseCoordinatesOf(direction), origin));
        direction.normalize();
        break;

      case TWO_D:
      case ORTHOGRAPHIC: {
        float[] wh = boundaryWidthHeight();
        origin.set(
            new Vector((2.0f * pixel.x() / width() - 1.0f) * wh[0], -(2.0f * pixel.y() / height() - 1.0f) * wh[1],
                0.0f));
        origin.set(eye().inverseCoordinatesOf(origin));
        direction.set(viewDirection());
        break;
      }
    }
  }

  // Warning and exception handling stuff

  static protected HashMap<String, Object> _warnings;

  /**
   * Show warning, and keep track of it so that it's only shown once.
   *
   * @param message the error message (which will be stored for later comparison)
   */
  static public void showWarning(String message) { // ignore
    if (_warnings == null) {
      _warnings = new HashMap<String, Object>();
    }
    if (!_warnings.containsKey(message)) {
      System.err.println(message);
      _warnings.put(message, new Object());
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
   * bogus _event.
   */
  static public void showEventVariationWarning(String method) {
    showWarning(method + " can only be performed using a relative _event.");
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

  static public void showMinDOFsWarning(String themethod, int dofs) {
    showWarning(themethod + "() requires at least a " + dofs + " dofs.");
  }

  // Nice stuff :P

  /**
   * Apply the local transformation defined by {@code frame}, i.e., respect to the frame
   * {@link Frame#reference()}. The Frame is first translated
   * and then rotated around the new translated origin.
   * <p>
   * This method may be used to modify the modelview matrix from a Frame hierarchy. For
   * example, with this Frame hierarchy:
   * <p>
   * {@code Frame body = new Frame();} <br>
   * {@code Frame leftArm = new Frame();} <br>
   * {@code Frame rightArm = new Frame();} <br>
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
      rotate(frame.rotation().angle2D());
      scale(frame.scaling(), frame.scaling());
    } else {
      translate(frame.translation()._vector[0], frame.translation()._vector[1], frame.translation()._vector[2]);
      rotate(frame.rotation().angle(), (frame.rotation()).axis()._vector[0], (frame.rotation()).axis()._vector[1], (frame.rotation()).axis()._vector[2]);
      scale(frame.scaling(), frame.scaling(), frame.scaling());
    }
  }

  /**
   * Same as {@link #applyTransformation(Frame)} but applies the global transformation
   * defined by the frame.
   */
  public void applyWorldTransformation(Frame frame) {
    Frame refFrame = frame.reference();
    if (refFrame != null) {
      applyWorldTransformation(refFrame);
      applyTransformation(frame);
    } else
      applyTransformation(frame);
  }

  // Other stuff

  /**
   * Returns true if graph is left handed. Note that the graph is right handed by default.
   * However in proscene we set it as right handed (same as with P5).
   *
   * @see #setLeftHanded()
   */
  public boolean isLeftHanded() {
    return !_rightHanded;
  }

  /**
   * Returns true if graph is right handed. Note that the graph is right handed by
   * default. However in proscene we set it as right handed (same as with P5).
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
   * Max between {@link Node#lastUpdate()} and
   * {@link #_lastNonEyeUpdate()}.
   *
   * @return last frame the Eye was updated
   * @see #_lastNonEyeUpdate()
   */
  public long lastUpdate() {
    return Math.max(eye().lastUpdate(), _lastNonEyeUpdate());
  }

  /**
   * @return last frame a local Eye parameter (different than the Frame) was updated.
   * @see #lastUpdate()
   */
  protected long _lastNonEyeUpdate() {
    return _lastNonEyeUpdate;
  }
}
