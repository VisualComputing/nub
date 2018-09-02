/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core;

import frames.primitives.*;
import frames.timing.Animator;
import frames.timing.TimingHandler;
import frames.timing.TimingTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A 2D or 3D scene graph providing eye, input and timing handling to a raster or ray-tracing
 * renderer.
 * <h1>1. Types and dimensions</h1>
 * A graph uses a ball to set the 2d or 3d viewing space (instead of the more traditional
 * methods to set a 3d viewing frustum). See {@link #setCenter(Vector)} and
 * {@link #setRadius(float)}, and also {@link #setZClippingCoefficient(float)} and
 * {@link #setZNearCoefficient(float)} for a 3d graph. See also
 * {@link #setBoundingBox(Vector, Vector)}.
 * <p>
 * The way the {@link #projection()} matrix is computed (see {@link #computeProjection()}),
 * defines the type of the graph as: {@link Type#PERSPECTIVE}, {@link Type#ORTHOGRAPHIC}
 * for 3d graphs and {@link Type#TWO_D} for a 2d graph. To set a {@link Type#CUSTOM}
 * override {@link #computeCustomProjection()}.
 * <h1>2. Scene graph handling</h1>
 * A graph forms a tree of (attached) {@link Frame}s which may be {@link #traverse()},
 * calling {@link Frame#visit()} on each visited frame (refer to the {@link Frame}
 * documentation). Note that {@link #traverse()} should be called within your main-event loop.
 * <p>
 * The frame collection belonging to the graph may be retrieved with {@link #frames()}.
 * The graph provides other useful routines to handle the hierarchy, such as
 * {@link #pruneBranch(Frame)}, {@link #appendBranch(List)}, {@link #isReachable(Frame)},
 * {@link #branch(Frame)}, and {@link #clear()}.
 * <h2>2.1. Eye handling</h2>
 * Any {@link Frame} (belonging or not to the graph hierarchy) may be set as the {@link #eye()}
 * (see {@link #setEye(Frame)}). Several frame wrapper functions to handle the eye, such as
 * {@link #lookAt(Vector)}, {@link #at()}, {@link #setViewDirection(Vector)},
 * {@link #setUpVector(Vector)}, {@link #upVector()}, {@link #fitFieldOfView()},
 * {@link #fieldOfView()}, {@link #setHorizontalFieldOfView(float)}, {@link #fitBall()}
 * {@link #screenLocation(Vector, Frame)} and
 * {@link #location(Vector, Frame)}, are provided for convenience.
 * <h1>3. Interactivity</h1>
 * Several methods taking a {@link Frame} parameter provide interactivity to frames, such as
 * {@link #translate(float, float, float, Frame)},
 * {@link #rotate(float, float, float, Frame)} and {@link #scale(float, Frame)}.
 * <p>
 * Some interactivity methods are only available for the {@link #eye()} and hence they don't
 * take a frame parameter, such as {@link #lookAround(float, float)} or {@link #rotateCAD(float, float)}.
 * <p>
 * Call {@link #control(Frame, Object...)} to send arbitrary gesture data to the frame. Note that
 * {@link Frame#interact(Object...)} should be overridden to implement the frame custom behavior.
 * <p>
 * To check if a given frame would be picked with a ray casted at a given screen position
 * use {@link #tracks(float, float, Frame)}. Refer to {@link Frame#precision()} (and
 * {@link Frame#setPrecision(Frame.Precision)}) for the different frame picking policies.
 * <h1>4. Human Interface Devices</h1>
 * Setting up a <a href="https://en.wikipedia.org/wiki/Human_interface_device">Human Interface Device (hid)</a>
 * is a two step process: 1. Define an {@code hid} tracked-frame instance, using an arbitrary name for it
 * (see {@link #setTrackedFrame(String, Frame)}); and, 2. Call any interactivity method that take an {@code hid}
 * param (such as {@link #translate(String, float, float, float)}, {@link #rotate(String, float, float, float)}
 * or {@link #scale(String, float)}) following the name convention you defined in 1. Observations:
 * <ol>
 * <li>An {@code hid} tracked-frame (see {@link #trackedFrame(String)}) defines in turn an {@code hid} default-frame
 * (see {@link #defaultFrame(String)}) which simply returns the tracked-frame or the {@link #eye()} when the
 * {@code hid} tracked-frame is {@code null}.</li>
 * <li>The {@code hid} interactivity methods are implemented in terms of the ones defined previously
 * by simply passing the {@code hid} {@link #defaultFrame(String)} to them (e.g.,
 * {@link #scale(String, float)} calls {@link #scale(float, Frame)} passing the {@code hid} default-frame).</li>
 * <li>The default {@code hid} is defined with a {@code null} String parameter (e.g.,
 * {@link #scale(float delta)} simply calls {@code scale(null, delta)}).</li>
 * <li>To update an {@code hid} tracked-frame using ray-casting call {@link #track(String, Point, Frame[])}
 * (detached or attached frames), {@link #track(String, Point)} (only attached frames) or
 * {@link #cast(String, Point)} (only for attached frames too). While {@link #track(String, Point, Frame[])} and
 * {@link #track(String, Point)} update the {@code hid} tracked-frame synchronously (i.e., they return the
 * {@code hid} tracked-frame immediately), {@link #cast(String, Point)} updates it asynchronously (i.e., it
 * optimally updates the {@code hid} tracked-frame during the next call to the {@link #traverse()} algorithm).</li>
 * </ol>
 * <h1>5. Timing handling</h1>
 * The graph performs timing handling through a {@link #timingHandler()}. Several
 * {@link TimingHandler} wrapper functions, such as {@link #registerTask(TimingTask)}
 * and {@link #registerAnimator(Animator)}, are provided for convenience.
 * <p>
 * A default {@link #interpolator()} may perform several {@link #eye()} interpolations
 * such as {@link #fitBallInterpolation()}, {@link #zoomOnRegionInterpolation(Rectangle)},
 * {@link #interpolateTo(Frame)} and {@link #interpolateTo(Frame, float)}. Refer to the
 * {@link Interpolator} documentation for details.
 * <h1>6. Visibility and culling techniques</h1>
 * Geometry may be culled against the viewing volume by calling {@link #isPointVisible(Vector)},
 * {@link #ballVisibility(Vector, float)} or {@link #boxVisibility(Vector, Vector)}. Make sure
 * to call {@link #enableBoundaryEquations()} first, since update of the viewing volume
 * boundary equations are disabled by default (see {@link #enableBoundaryEquations()} and
 * {@link #areBoundaryEquationsEnabled()}).
 * <h1>7. Matrix handling</h1>
 * The graph performs matrix handling through a {@link #matrixHandler()}.
 * To set shader matrices use {@link #projection()}, {@link #modelView()}
 * (which wrap {@link MatrixHandler} functions with the same signatures) and
 * (possibly) {@code Matrix.multiply(projection(), modelView())}.
 * <p>
 * To apply the transformation defined by a frame call {@link #applyTransformation(Frame)}
 * (see also {@link #applyWorldTransformation(Frame)}) between {@link #pushModelView()} and
 * {@link #popModelView()} (which wrap {@link MatrixHandler} functions with the same signatures).
 * Note that the frame transformations are applied automatically by the {@link #traverse()}
 * algorithm (in this case you don't need to call them).
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
 * @see #applyTransformation(Frame)
 * @see MatrixHandler
 */
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
  // handed and HUD
  protected boolean _rightHanded;
  protected int _hudCalls;
  // size and dim
  protected int _width, _height;

  // 2. Matrix helper
  protected MatrixHandler _matrixHandler;

  // 3. Handlers
  protected class Tuple {
    public String _hid;
    public Point _pixel;

    Tuple(String hid, Point pixel) {
      _hid = hid;
      _pixel = pixel;
    }
  }

  protected TimingHandler _timingHandler;
  protected HashMap<String, Frame> _agents;
  protected ArrayList<Tuple> _tuples;

  // 4. Graph
  protected List<Frame> _seeds;
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
   * The constructor sets a {@link Frame} instance as the graph {@link #eye()} and then
   * calls {@link #fitBall()}, so that the entire scene fits the screen dimensions.
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
   * @see #setEye(Frame)
   */
  public Graph(Type type, int width, int height) {
    setType(type);
    setWidth(width);
    setHeight(height);

    _seeds = new ArrayList<Frame>();
    _timingHandler = new TimingHandler();

    setRadius(100);
    setCenter(new Vector());
    _anchor = center().get();
    setEye(new Frame(this));
    fitBall();

    setMatrixHandler(new MatrixHandler(this));
    _agents = new HashMap<String, Frame>();
    _tuples = new ArrayList<Tuple>();
    setRightHanded();

    enableBoundaryEquations(false);

    setZNearCoefficient(0.005f);
    setZClippingCoefficient((float) Math.sqrt(3.0f));
  }

  /**
   * Same as {@code return Frame.random(this)}. Creates a random frame attached to this graph.
   *
   * @see Frame#random(Graph)
   * @see #randomize(Frame)
   */
  public Frame randomFrame() {
    return Frame.random(this);
  }

  /**
   * Same as {@code frame.randomize(center(), radius(), is3D())}.
   *
   * @see Frame#randomize(Vector, float, boolean)
   */
  public void randomize(Frame frame) {
    frame.randomize(center(), radius(), is3D());
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
   * Returns the graph type. Set by {@link #setType(Type)}.
   *
   * @see #setType(Type)
   */
  public Type type() {
    return _type;
  }

  /**
   * Defines the graph {@link #type()}.
   * <p>
   * A {@link Type#PERSPECTIVE} uses a classical projection mainly defined by its
   * {@link #fieldOfView()}. With a {@link Type#ORTHOGRAPHIC}, the {@link #fieldOfView()}
   * is meaningless and the width and height of the graph frustum are inferred from the
   * distance to the {@link #anchor()} using {@link #boundaryWidthHeight()}. Both types
   * use {@link #zNear()} and {@link #zFar()} (to define their clipping planes) and
   * {@link #aspectRatio()} (for frustum shape).
   * <p>
   * A {@link Type#TWO_D} behaves like {@link Type#ORTHOGRAPHIC}, but instantiated graph
   * frames will be constrained so that they will remain at the x-y plane. See
   * {@link frames.core.constraint.Constraint}.
   * <p>
   * To set a {@link Type#CUSTOM} override {@link #computeCustomProjection()}.
   */
  public void setType(Type type) {
    if (type != type()) {
      _modified();
      this._type = type;
    }
  }

  /**
   * Returns the vertical field of view of the {@link #eye()} (in radians) computed as
   * {@code 2 * (float) Math.atan(eye().magnitude())}.
   * <p>
   * Value is set using {@link #setFieldOfView(float)}. Default value is pi/3 radians.
   * This value is meaningless if the graph {@link #type()} is {@link Type#ORTHOGRAPHIC}.
   * <p>
   * The field of view corresponds the one used in {@code gluPerspective}. It sets the Y
   * (vertical) aperture of the eye. The X (horizontal) angle is inferred from the
   * window aspect ratio (see {@link #aspectRatio()} and {@link #horizontalFieldOfView()}).
   *
   * @see #setFieldOfView(float)
   * @see #eye()
   */
  public float fieldOfView() {
    return 2 * (float) Math.atan(eye().magnitude());
  }

  /**
   * Same as {@code eye().setMagnitude((float) Math.tan(fov / 2))}.
   * <p>
   * Sets the field-of-view of the current {@link #eye()}.
   *
   * @see Frame#setMagnitude(float)
   */
  public void setFieldOfView(float fov) {
    eye().setMagnitude((float) Math.tan(fov / 2));
  }

  /**
   * Returns the horizontal field of view of the {@link #eye()} (in radians).
   * <p>
   * Value is set using {@link #setHorizontalFieldOfView(float)} or
   * {@link #setFieldOfView(float)}. These values are always linked by:
   * {@code horizontalFieldOfView() = 2 * atan ( tan(fieldOfView()/2) * aspectRatio() )}.
   */
  public float horizontalFieldOfView() {
    return 2 * (float) Math.atan((eye() == null ? 1 : eye().magnitude()) * aspectRatio());
  }

  /**
   * Changes the {@link #eye()} {@link Graph#fieldOfView()} so that the entire scene
   * (defined by {@link #center()} and {@link Graph#radius()}) is visible.
   * <p>
   * The eye position and orientation are not modified and you first have to orientate
   * the eye in order to actually see the scene (see {@link Graph#lookAt(Vector)},
   * {@link Graph#fitBall()} or {@link Graph#fitBall(Vector, float)}).
   *
   * <b>Attention:</b> The {@link Graph#fieldOfView()} is clamped to PI/2. This happens
   * when the eye is at a distance lower than sqrt(2) * radius() from the center().
   *
   * @see #setFieldOfView(float)
   */
  // TODO shadow maps computation docs are missing
  public void fitFieldOfView() {
    if (Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis()) > (float) Math.sqrt(2) * radius())
      setFieldOfView(2 * (float) Math.asin(radius() / Vector.scalarProjection(Vector.subtract(eye().position(), center()), eye().zAxis())));
    else
      setFieldOfView((float) Math.PI / 2);
  }

  /**
   * Sets the {@link Graph#horizontalFieldOfView()} of the {@link #eye()} (in radians).
   * <p>
   * {@link Graph#horizontalFieldOfView()} and {@link Graph#fieldOfView()} are linked by the
   * {@link Graph#aspectRatio()}. This method actually calls:
   * {@code setFieldOfView(( 2 * atan (tan(hfov / 2) / aspectRatio()) ))} so that a
   * call to {@link Graph#horizontalFieldOfView()} returns the expected value.
   */
  public void setHorizontalFieldOfView(float hfov) {
    setFieldOfView(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()));
  }

  /**
   * Returns the near clipping plane distance used by {@link #computeProjection()} matrix in
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
   * inside the {@link #radius()} sphere:
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
   * Returns the far clipping plane distance used by the {@link #computeProjection()} matrix in world units.
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
   * inside the sphere defined by {@link #center()} and {@link #zClippingCoefficient()} * {@link #radius()}.
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

  /**
   * Returns the {@code halfWidth} and {@code halfHeight} of the eye boundary.
   * While first element holds {@code halfWidth}, the second one
   * holds {@code halfHeight}. Values are computed as:
   * {@code _rescalingFactor() * (eye().magnitude() * width() / 2)}
   * and {@code _rescalingFactor() * (eye().magnitude() * height() / 2)},
   * respectively.
   * <p>
   * These values are valid for 2d and orthographic graphs (but not perspective) and they are
   * expressed in virtual world units.
   * <p>
   * In the case of orthographs graphs these values are proportional to the eye (z
   * projected) distance to the {@link #anchor()}. When zooming on the object, the eye
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
   * In 2D returns {@code 1}.
   * <p>
   * In 3D returns a value proportional to the eye (z projected) distance to the
   * {@link #anchor()} so that when zooming on the object, the ortho eye is translated
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

  // Graph and frames stuff

  /**
   * Returns the top-level frames (those which reference is null).
   * <p>
   * All leading frames are also reachable by the {@link #traverse()} algorithm for which they are the seeds.
   *
   * @see #frames()
   * @see #isReachable(Frame)
   * @see #pruneBranch(Frame)
   */
  protected List<Frame> _leadingFrames() {
    return _seeds;
  }

  /**
   * Returns {@code true} if the frame is top-level.
   */
  protected boolean _isLeadingFrame(Frame frame) {
    for (Frame leadingFrame : _leadingFrames())
      if (leadingFrame == frame)
        return true;
    return false;
  }

  /**
   * Add the frame as top-level if its reference frame is null and it isn't already added.
   */
  protected boolean _addLeadingFrame(Frame frame) {
    if (frame == null || frame.reference() != null)
      return false;
    if (_isLeadingFrame(frame))
      return false;
    return _leadingFrames().add(frame);
  }

  /**
   * Removes the leading frame if present. Typically used when re-parenting the frame.
   */
  protected boolean _removeLeadingFrame(Frame frame) {
    boolean result = false;
    Iterator<Frame> it = _leadingFrames().iterator();
    while (it.hasNext()) {
      if (it.next() == frame) {
        it.remove();
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Same as {@code for(Frame frame : _leadingFrames()) pruneBranch(frame)}.
   *
   * @see #pruneBranch(Frame)
   */
  public void clear() {
    for (Frame frame : _leadingFrames())
      pruneBranch(frame);
  }

  /**
   * Make all the frames in the {@code frame} branch eligible for garbage collection.
   * <p>
   * A call to {@link #isReachable(Frame)} on all {@code frame} descendants
   * (including {@code frame}) will return false, after issuing this method. It also means
   * that all frames in the {@code frame} branch will become unreachable by the
   * {@link #traverse()} algorithm.
   * <p>
   * To make all the frames in the branch reachable again, first cache the frames
   * belonging to the branch (i.e., {@code branch=pruneBranch(frame)}) and then call
   * {@link #appendBranch(List)} on the cached branch. Note that calling
   * {@link Frame#setReference(Frame)} on a frame belonging to the pruned branch will become
   * reachable again by the traversal algorithm.
   * <p>
   * When collected, pruned frames behave like {@link Frame}, otherwise they are eligible for
   * garbage collection.
   *
   * @see #clear()
   * @see #appendBranch(List)
   * @see #isReachable(Frame)
   */
  public List<Frame> pruneBranch(Frame frame) {
    if (!isReachable(frame))
      return new ArrayList<Frame>();
    ArrayList<Frame> list = new ArrayList<Frame>();
    _collect(list, frame);
    for (Frame collectedFrame : list)
      if (collectedFrame.reference() != null)
        collectedFrame.reference()._removeChild(collectedFrame);
      else
        _removeLeadingFrame(collectedFrame);
    return list;
  }

  /**
   * Appends the branch which typically should come from the one pruned (and cached) with
   * {@link #pruneBranch(Frame)}.
   * <p>
   * {@link #pruneBranch(Frame)}
   */
  public void appendBranch(List<Frame> branch) {
    if (branch == null)
      return;
    for (Frame frame : branch)
      if (frame.reference() != null)
        frame.reference()._addChild(frame);
      else
        _addLeadingFrame(frame);
  }

  /**
   * Returns {@code true} if the frame is reachable by the {@link #traverse()}
   * algorithm and {@code false} otherwise.
   * <p>
   * Frames are made unreachable with {@link #pruneBranch(Frame)} and reachable
   * again with {@link Frame#setReference(Frame)}.
   *
   * @see #traverse()
   * @see #frames()
   */
  public boolean isReachable(Frame frame) {
    if (frame == null)
      return false;
    return frame.isAttached(this);
  }

  /**
   * Returns a list of all the frames that are reachable by the {@link #traverse()}
   * algorithm.
   * <p>
   * The method traverse the hierarchy to collect. Frame collections should thus be kept at user space
   * for efficiency.
   *
   * @see #isReachable(Frame)
   * @see #isEye(Frame)
   */
  public List<Frame> frames() {
    ArrayList<Frame> list = new ArrayList<Frame>();
    for (Frame frame : _leadingFrames())
      _collect(list, frame);
    return list;
  }

  /**
   * Collects {@code frame} and all its descendant frames. Note that for a frame to be collected
   * it must be reachable.
   *
   * @see #isReachable(Frame)
   */
  public List<Frame> branch(Frame frame) {
    ArrayList<Frame> list = new ArrayList<Frame>();
    _collect(list, frame);
    return list;
  }

  /**
   * Returns a straight path of frames between {@code tail} and {@code tip}. Returns an empty list
   * if either {@code tail} or {@code tip} aren't reachable. Use {@link Frame#path(Frame, Frame)}
   * to include all frames even if they aren't reachable.
   * <p>
   * If {@code tail} is ancestor of {@code tip} the returned list will include both of them.
   * Otherwise it will be empty.
   *
   * @see #isReachable(Frame)
   * @see Frame#path(Frame, Frame)
   */
  public List<Frame> path(Frame tail, Frame tip) {
    return (isReachable(tail) && isReachable(tip)) ? Frame.path(tail, tip) : new ArrayList<Frame>();
  }

  /**
   * Collects {@code frame} and all its descendant frames. Note that for a frame to be collected
   * it must be reachable.
   *
   * @see #isReachable(Frame)
   */
  protected void _collect(List<Frame> list, Frame frame) {
    if (frame == null)
      return;
    list.add(frame);
    for (Frame child : frame.children())
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
   * Returns the number of frames displayed since the graph was instantiated.
   * <p>
   * Use {@code TimingHandler.frameCount} to retrieve the number of frames displayed since
   * the first graph was instantiated.
   */
  //TODO check name
  public long frameCount() {
    return timingHandler().frameCount();
  }

  /**
   * Convenience wrapper function that simply calls {@code timingHandler().registerTask(task)}.
   *
   * @see frames.timing.TimingHandler#registerTask(TimingTask)
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
   * @see frames.timing.TimingHandler#unregisterAnimator(Animator)
   */
  public void unregisterAnimator(Animator animator) {
    timingHandler().unregisterAnimator(animator);
  }

  /**
   * Convenience wrapper function that simply returns {@code timingHandler().isAnimatorRegistered(animator)}.
   *
   * @see frames.timing.TimingHandler#isAnimatorRegistered(Animator)
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
   * Computes and returns the projection matrix associated with the graph.
   * <p>
   * If the graph type is PERSPECTIVE, defines a projection matrix using the
   * {@link #fieldOfView()}, {@link #aspectRatio()}, {@link #zNear()} and {@link #zFar()}
   * parameters.
   * <p>
   * If the graph type is ORTHOGRAPHIC or TWO_D, the frustum's width and height are set using
   * {@link #boundaryWidthHeight()}.
   * <p>
   * Both PERSPECTIVE and ORTHOGRAPHIC types use {@link #zNear()} and {@link #zFar()}
   * to place the clipping planes. These values are determined from radius() and center() so that
   * they best fit the graph size.
   * <p>
   * Override {@link #computeCustomProjection()} to define a CUSTOM projection.
   *
   * <b>Note 1:</b> This method is called by {@link #preDraw()}.
   *
   * <b>Note 2:</b> Note that the computation of both, the PERSPECTIVE and ORTHOGRAPHIC frustum
   * shapes depend on the eye magnitude, see {@link #fieldOfView()} and {@link #boundaryWidthHeight()}.
   */
  public Matrix computeProjection() {
    Matrix projection = null;
    switch (type()) {
      case PERSPECTIVE:
        projection = Matrix.perspective(zNear(), zFar(), aspectRatio(), isLeftHanded() ? -eye().magnitude() : eye().magnitude());
        break;
      case TWO_D:
      case ORTHOGRAPHIC:
        float[] wh = boundaryWidthHeight();
        projection = Matrix.orthographic(zNear(), zFar(), wh[0], isLeftHanded() ? -wh[1] : wh[1]);
        break;
      case CUSTOM:
        projection = computeCustomProjection();
    }
    return projection;
  }

  /**
   * Override this method to define a graph CUSTOM projection matrix.
   */
  protected Matrix computeCustomProjection() {
    return new Matrix();
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
   * Wrapper for {@link MatrixHandler#modelView()}
   */
  public Matrix modelView() {
    return _matrixHandler.modelView();
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
   * Wrapper for {@link MatrixHandler#applyModelView(Matrix)}
   */
  public void applyModelView(Matrix source) {
    _matrixHandler.applyModelView(source);
  }

  /**
   * Wrapper for {@link MatrixHandler#projection()}
   */
  public Matrix projection() {
    return _matrixHandler.projection();
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
   * Wrapper for {@link MatrixHandler#applyProjection(Matrix)}
   */
  public void applyProjection(Matrix source) {
    _matrixHandler.applyProjection(source);
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

  // Drawing stuff

  /**
   * Called before your main drawing and performs the following:
   * <ol>
   * <li>Calls {@link MatrixHandler#_bind()}</li>
   * <li>Calls {@link #updateBoundaryEquations()} if {@link #areBoundaryEquationsEnabled()}</li>
   * </ol>
   */
  public void preDraw() {
    //TODO test syncing, since timingHandler().handle() was called in postDraw()
    timingHandler().handle();
    matrixHandler()._bind();
    if (areBoundaryEquationsEnabled() && (eye().lastUpdate() > _lastEqUpdate || _lastEqUpdate == 0)) {
      updateBoundaryEquations();
      _lastEqUpdate = TimingHandler.frameCount;
    }
  }

  // Eye stuff

  /**
   * Checks wheter or not the given frame is the {@link #eye()}.
   */
  public boolean isEye(Frame frame) {
    return _eye == frame;
  }

  /**
   * Returns the associated eye. Never null.
   *
   * @see #setEye(Frame)
   */
  public Frame eye() {
    return _eye;
  }

  /**
   * Replaces the current {@link #eye()} with {@code eye}. If {@code eye} is instance of
   * {@link Frame} it should belong to this graph object.
   *
   * @see #eye()
   */
  public void setEye(Frame eye) {
    if (eye == null || _eye == eye)
      return;
    _eye = eye;
    if (_interpolator == null)
      _interpolator = new Interpolator(this, _eye);
    else
      _interpolator.setFrame(_eye);
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
   * {@link Visibility#SEMIVISIBLE}, depending whether the sphere (of radius {@code radius}
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
   * every frame instead.
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

  protected float[][] _updateBoundaryEquations2() {
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
   * Disables automatic update of the frustum plane equations every frame.
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
   * Enables automatic update of the frustum plane equations every frame.
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
   * @see #setCenter(Vector)
   * @see #setRadius(float)
   * @see #setBoundingBox(Vector, Vector)
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
   * Sets the {@link #radius()} value in world units.
   *
   * @see #setCenter(Vector)
   */
  public void setRadius(float radius) {
    if (radius <= 0.0f) {
      System.out.println("Warning: Scene radius must be positive - Ignoring value");
      return;
    }
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
   * Similar to {@link #setRadius(float)} and {@link #setCenter(Vector)}, but the
   * graph limits are defined by a world axis aligned bounding box.
   */
  public void setBoundingBox(Vector corner1, Vector corner2) {
    setCenter(Vector.multiply(Vector.add(corner1, corner2), 1 / 2.0f));
    setRadius(0.5f * (Vector.subtract(corner2, corner1)).magnitude());
  }

  /**
   * Returns the normalized view direction of the eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #eye()}
   * ({@code frame().worldDisplacement(new Vector(0.0f, 0.0f, -1.0f))}). In 2D
   * it always is (0,0,-1).
   * <p>
   * Change this value using {@link #setViewDirection(Vector)}, {@link #lookAt(Vector)} or
   * {@link Frame#setOrientation(Quaternion)}. It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vector viewDirection() {
    return eye().zAxis(false);
  }

  /**
   * Rotates the eye so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The eye {@link Frame#position()} is not modified. The eye is rotated so that the
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
   * When {@code noMove} is true, the Eye {@link Frame#position()} is left unchanged, which is
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
   * Set using {@link #setUpVector(Vector)} or {@link Frame#setOrientation(Quaternion)}. It is
   * orthogonal to {@link #viewDirection()} and to {@link #rightVector()}.
   * <p>
   * It corresponds to the Y axis of the associated {@link #eye()} (actually returns
   * {@code frame().yAxis()}
   */
  public Vector upVector() {
    return eye().yAxis();
  }

  /**
   * 2D eyes simply call {@code frame().setPosition(target.x(), target.y())}. 3D
   * eyes set {@link Frame#orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The eye {@link Frame#position()} is not modified.
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
   * {@link #setUpVector(Vector)}, {@link #lookAt(Vector)} or {@link Frame#setOrientation(Quaternion)}.
   * <p>
   * Simply returns {@code frame().xAxis()}.
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
   * Returns the {@link #eye()} {@link Interpolator} used by {@link #fitBallInterpolation()},
   * {@link #zoomOnRegionInterpolation(Rectangle)}, {@link #interpolateTo(Frame)}, etc.
   */
  public Interpolator interpolator() {
    return _interpolator;
  }

  /**
   * Convenience function that simply calls {@code interpolateTo(fr, 1)}.
   *
   * @see #interpolateTo(Frame, float)
   */
  public void interpolateTo(Frame frame) {
    interpolateTo(frame, 1);
  }

  /**
   * Smoothly interpolates the eye on a interpolator path so that it goes to
   * {@code frame}.
   * <p>
   * {@code frame} is expressed in world coordinates. {@code duration} tunes the
   * interpolation speed.
   *
   * @see #interpolateTo(Frame)
   * @see #fitBallInterpolation()
   */
  public void interpolateTo(Frame frame, float duration) {
    _interpolator.stop();
    _interpolator.purge();
    _interpolator.addKeyFrame(eye().detach());
    _interpolator.addKeyFrame(frame.detach(), duration);
    _interpolator.start();
  }

  /**
   * Smoothly moves the eye so that the rectangular screen region defined by
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
  public void zoomOnRegionInterpolation(Rectangle rectangle) {
    _interpolator.stop();
    _interpolator.purge();
    Frame eye = eye();
    setEye(eye().detach());
    _interpolator.addKeyFrame(eye().detach());
    zoomOnRegion(rectangle);
    _interpolator.addKeyFrame(eye().detach());
    setEye(eye);
    _interpolator.start();
  }

  /**
   * Interpolates the eye so that the entire graph fits the screen.
   * <p>
   * The graph is defined by its {@link #center()} and its {@link #radius()}.
   * See {@link #fitBall()}.
   * <p>
   * The {@link Frame#orientation()} of the {@link #eye()} is not modified.
   */
  public void fitBallInterpolation() {
    _interpolator.stop();
    _interpolator.purge();
    Frame eye = eye();
    setEye(eye().detach());
    _interpolator.addKeyFrame(eye().detach());
    fitBall();
    _interpolator.addKeyFrame(eye().detach());
    setEye(eye);
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
   * In 3D the eye is simply translated along its {@link #viewDirection()} so that the
   * sphere fits the screen. Its {@link Frame#orientation()} and its
   * {@link #fieldOfView()} are unchanged. You should therefore orientate the eye
   * before you call this method.
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
   * Moves the eye so that the world axis aligned bounding box ({@code corner1} and
   * {@code corner2}) is entirely visible, using {@link #fitBall(Vector, float)}.
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
   * in 3D the eye is translated (its {@link Frame#orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #center()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   */
  public void zoomOnRegion(Rectangle rectangle) {
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
        direction.set(new Vector(((2.0f * pixelCopy.x() / width()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f) * aspectRatio(),
            ((2.0f * (height() - pixelCopy.y()) / height()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f),
            -1.0f));
        direction.set(Vector.subtract(eye().worldLocation(direction), origin));
        direction.normalize();
        break;

      case TWO_D:
      case ORTHOGRAPHIC: {
        float[] wh = boundaryWidthHeight();
        origin.set(
            new Vector((2.0f * pixelCopy.x() / width() - 1.0f) * wh[0], -(2.0f * pixelCopy.y() / height() - 1.0f) * wh[1],
                0.0f));
        origin.set(eye().worldLocation(origin));
        direction.set(viewDirection());
        break;
      }
    }
  }

  // Nice stuff :P

  /**
   * Apply the local transformation defined by {@code frame}, i.e., respect to its
   * {@link Frame#reference()}. The Frame is first translated, then rotated around
   * the new translated origin and then scaled.
   * <p>
   * This method may be used to modify the modelview matrix from a frame hierarchy. For
   * example, with this frame hierarchy:
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
   *
   * @see #applyWorldTransformation(Frame)
   */
  public void applyTransformation(Frame frame) {
    if (is2D()) {
      matrixHandler().translate(frame.translation().x(), frame.translation().y());
      matrixHandler().rotate(frame.rotation().angle2D());
      matrixHandler().scale(frame.scaling(), frame.scaling());
    } else {
      matrixHandler().translate(frame.translation()._vector[0], frame.translation()._vector[1], frame.translation()._vector[2]);
      matrixHandler().rotate(frame.rotation().angle(), (frame.rotation()).axis()._vector[0], (frame.rotation()).axis()._vector[1], (frame.rotation()).axis()._vector[2]);
      matrixHandler().scale(frame.scaling(), frame.scaling(), frame.scaling());
    }
  }

  /**
   * Same as {@link #applyTransformation(Frame)}, but applies the global transformation
   * defined by the frame.
   */
  public void applyWorldTransformation(Frame frame) {
    Frame reference = frame.reference();
    if (reference != null) {
      applyWorldTransformation(reference);
      applyTransformation(frame);
    } else
      applyTransformation(frame);
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
   * Max between {@link Frame#lastUpdate()} and {@link #_lastNonEyeUpdate()}.
   *
   * @return last frame the eye was updated
   * @see #_lastNonEyeUpdate()
   */
  public long lastUpdate() {
    return Math.max(eye().lastUpdate(), _lastNonEyeUpdate());
  }

  /**
   * @return last frame a local eye parameter (different than the Frame) was updated.
   * @see #lastUpdate()
   */
  protected long _lastNonEyeUpdate() {
    return _lastNonEyeUpdate;
  }

  // traversal

  // detached frames

  /**
   * Same as {@code return track(null, pixel, frameArray)}.
   *
   * @see #track(String, float, float, Frame[])
   */
  public Frame track(Point pixel, Frame[] frameArray) {
    return track(null, pixel, frameArray);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y(), frameArray)}.
   *
   * @see #track(String, float, float, Frame[])
   */
  public Frame track(String hid, Point pixel, Frame[] frameArray) {
    return track(hid, pixel.x(), pixel.y(), frameArray);
  }

  /**
   * Same as {@code return track(null, x, y, frameArray)}.
   *
   * @see #track(String, float, float, Frame[])
   */
  public Frame track(float x, float y, Frame[] frameArray) {
    return track(null, x, y, frameArray);
  }

  /**
   * Updates the {@code hid} device tracked-frame from the {@code frameArray} and returns it.
   * <p>
   * To set the {@link #trackedFrame(String)} the algorithm casts a ray at pixel position {@code (x, y)}
   * (see {@link #tracks(float, float, Frame)}). If no frame is found under the pixel, it returns {@code null}.
   * <p>
   * Use this version of the method instead of {@link #track(String, float, float)} when dealing with
   * detached frames.
   *
   * @see #track(String, float, float)
   * @see #track(String, float, float, List)
   * @see #traverse()
   * @see #trackedFrame(String)
   * @see #resetTrackedFrame(String)
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#enableTracking(boolean)
   * @see Frame#precision()
   * @see Frame#setPrecision(Frame.Precision)
   * @see #cast(String, Point)
   * @see #cast(String, float, float)
   */
  public Frame track(String hid, float x, float y, Frame[] frameArray) {
    resetTrackedFrame(hid);
    for (Frame frame : frameArray)
      if (tracks(x, y, frame)) {
        setTrackedFrame(hid, frame);
        break;
      }
    return trackedFrame(hid);
  }

  /**
   * Same as {@code return track(null, pixel, frameList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Frame track(Point pixel, List<Frame> frameList) {
    return track(null, pixel, frameList);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y(), frameList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Frame track(String hid, Point pixel, List<Frame> frameList) {
    return track(hid, pixel.x(), pixel.y(), frameList);
  }

  /**
   * Same as {@code return track(null, x, y, frameList)}.
   *
   * @see #track(String, float, float, List)
   */
  public Frame track(float x, float y, List<Frame> frameList) {
    return track(null, x, y, frameList);
  }

  /**
   * Same as {@link #track(String, float, float, Frame[])} but using a frame list instead of an array.
   *
   * @see #track(String, float, float, Frame[])
   */
  public Frame track(String hid, float x, float y, List<Frame> frameList) {
    resetTrackedFrame(hid);
    for (Frame frame : frameList)
      if (tracks(x, y, frame)) {
        setTrackedFrame(hid, frame);
        break;
      }
    return trackedFrame(hid);
  }

  // attached frames

  /**
   * Same as {@code return track(null, pixel.x(), pixel.y())}.
   *
   * @see #track(String, Point)
   */
  public Frame track(Point pixel) {
    return track(null, pixel.x(), pixel.y());
  }

  /**
   * Same as {@code return track(null, x, y)}.
   *
   * @see #track(String, float, float)
   */
  public Frame track(float x, float y) {
    return track(null, x, y);
  }

  /**
   * Same as {@code return track(hid, pixel.x(), pixel.y())}.
   *
   * @see #track(String, float, float)
   */
  public Frame track(String hid, Point pixel) {
    return track(hid, pixel.x(), pixel.y());
  }

  /**
   * Updates the {@code hid} device tracked-frame and returns it.
   * <p>
   * To set the {@link #trackedFrame(String)} the algorithm casts a ray at pixel position {@code (x, y)}
   * (see {@link #tracks(float, float, Frame)}). If no frame is found under the pixel, it returns {@code null}.
   * <p>
   * Use this version of the method instead of {@link #track(String, float, float, Frame[])} when dealing with
   * attached frames to the graph.
   *
   * @see #track(String, float, float, Frame[])
   * @see #traverse()
   * @see #trackedFrame(String)
   * @see #resetTrackedFrame(String)
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#enableTracking(boolean)
   * @see Frame#precision()
   * @see Frame#setPrecision(Frame.Precision)
   * @see #cast(String, Point)
   * @see #cast(String, float, float)
   */
  public Frame track(String hid, float x, float y) {
    resetTrackedFrame(hid);
    for (Frame frame : _leadingFrames())
      _track(hid, frame, x, y);
    return trackedFrame(hid);
  }

  /**
   * Use internally by {@link #track(String, float, float)}.
   */
  protected void _track(String hid, Frame frame, float x, float y) {
    if (trackedFrame(hid) == null && frame.isTrackingEnabled())
      if (tracks(x, y, frame)) {
        setTrackedFrame(hid, frame);
        return;
      }
    if (!frame.isCulled() && trackedFrame(hid) == null)
      for (Frame child : frame.children())
        _track(hid, child, x, y);
  }

  /**
   * Same as {tracks(pixel.x(), pixel.y(), frame)}.
   *
   * @see #tracks(float, float, Frame)
   */
  public boolean tracks(Point pixel, Frame frame) {
    return tracks(pixel.x(), pixel.y(), frame);
  }

  /**
   * Casts a ray at pixel position {@code (x, y)} and returns {@code true} if the ray picks the {@code frame} and
   * {@code false} otherwise. The frame is picked according to the {@link Frame#precision()}.
   *
   * @see #trackedFrame(String)
   * @see #resetTrackedFrame(String)
   * @see #defaultFrame(String)
   * @see #track(String, float, float)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#enableTracking(boolean)
   * @see Frame#precision()
   * @see Frame#setPrecision(Frame.Precision)
   */
  public boolean tracks(float x, float y, Frame frame) {
    return _tracks(x, y, screenLocation(frame.position()), frame);
  }

  /**
   * Cached version of {@link #tracks(float, float, Frame)}.
   */
  protected boolean _tracks(float x, float y, Vector projection, Frame frame) {
    if (frame == null || isEye(frame))
      return false;
    if (!frame.isTrackingEnabled())
      return false;
    float threshold = frame.precision() == Frame.Precision.ADAPTIVE ? frame.precisionThreshold() * frame.scaling() * pixelToGraphRatio(frame.position()) / 2
        : frame.precisionThreshold() / 2;
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
   * Same as {@link #track(String, Point)} but doesn't return immediately the {@code hid} device tracked-frame.
   * The algorithm schedules an updated of the {@code hid} tracked-frame for the next traversal and hence should be
   * always be used in conjunction with {@link #traverse()}.
   * <p>
   * This method is optimal since it updated the {@code hid} tracked-frame at traversal time. Prefer this method over
   * {@link #track(String, Point)} when dealing with several {@code hids}.
   *
   * @see #traverse()
   * @see #trackedFrame(String)
   * @see #resetTrackedFrame(String)
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#enableTracking(boolean)
   * @see Frame#precision()
   * @see Frame#setPrecision(Frame.Precision)
   * @see #cast(String, float, float)
   */
  public void cast(String hid, Point pixel) {
    _tuples.add(new Tuple(hid, pixel));
  }

  /**
   * Traverse the frame hierarchy, successively applying the local transformation defined
   * by each traversed frame, and calling {@link Frame#visit()} on it.
   * <p>
   * Note that only reachable frames are visited by this algorithm.
   *
   * <b>Attention:</b> this method should be called within the main event loop, just after
   * {@link #preDraw()} (i.e., eye update) and before any other transformation of the
   * modelview matrix takes place.
   *
   * @see #track(String, float, float)
   * @see #isReachable(Frame)
   * @see #pruneBranch(Frame)
   */
  public void traverse() {
    for (Frame frame : _leadingFrames())
      _visit(frame);
    _tuples.clear();
  }

  /**
   * Used by the traversal algorithm.
   */
  protected void _visit(Frame frame) {
    pushModelView();
    applyTransformation(frame);
    _track(frame);
    frame.visit();
    if (!frame.isCulled())
      for (Frame child : frame.children())
        _visit(child);
    popModelView();
  }

  /**
   * Internally used by {@link #_visit(Frame)}.
   */
  protected void _track(Frame frame) {
    if (!_tuples.isEmpty()) {
      Vector projection = screenLocation(frame.position());
      Iterator<Tuple> it = _tuples.iterator();
      while (it.hasNext()) {
        Tuple tuple = it.next();
        resetTrackedFrame(tuple._hid);
        // Condition is overkill. Use it only in place of resetTrackedFrame
        //if (!isTracking(tuple._hid))
        if (_tracks(tuple._pixel.x(), tuple._pixel.y(), projection, frame)) {
          setTrackedFrame(tuple._hid, frame);
          it.remove();
        }
      }
    }
  }

  /**
   * Same as {@code setTrackedFrame(null, frame)}.
   *
   * @see #setTrackedFrame(String, Frame)
   */
  public void setTrackedFrame(Frame frame) {
    setTrackedFrame(null, frame);
  }

  /**
   * Sets the {@code hid} tracked-frame (see {@link #trackedFrame(String)}). Call this function if you want to set the
   * tracked frame manually and {@link #track(String, Point)} or {@link #cast(String, Point)} to set it automatically
   * using ray casting.
   *
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #resetTrackedFrame(String)
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#enableTracking(boolean)
   */
  public void setTrackedFrame(String hid, Frame frame) {
    if (frame == null) {
      System.out.println("Warning. Cannot track a null frame!");
      return;
    }
    if (!frame.isTrackingEnabled()) {
      System.out.println("Warning. Frame cannot be tracked! Enable tracking on the frame first by call frame.enableTracking(true)");
      return;
    }
    _agents.put(hid, frame);
  }

  /**
   * Same as {@code return trackedFrame(null)}.
   *
   * @see #trackedFrame(String)
   */
  public Frame trackedFrame() {
    return trackedFrame(null);
  }

  /**
   * Returns the current {@code hid} tracked frame which is usually set by ray casting (see
   * {@link #track(String, float, float)}). May return {@code null}. Reset it with {@link #resetTrackedFrame(String)}.
   *
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #resetTrackedFrame(String)
   * @see #isTrackedFrame(String, Frame)
   * @see #setTrackedFrame(String, Frame)
   */
  public Frame trackedFrame(String hid) {
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
   * Returns {@code true} if the {@code hid} has a non-null tracked frame and {@code false} otherwise.
   */
  public boolean isTracking(String hid) {
    return _agents.containsKey(hid);
  }

  /**
   * Same as {@code return isTrackedFrame(null, frame)}.
   *
   * @see #isTrackedFrame(String, Frame)
   * @see Frame#isTracked()
   */
  public boolean isTrackedFrame(Frame frame) {
    return isTrackedFrame(null, frame);
  }

  /**
   * Returns {@code true} if {@code frame} is the current {@code hid} {@link #trackedFrame(String)} and {@code false} otherwise.
   *
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #resetTrackedFrame(String)
   * @see #setTrackedFrame(String, Frame)
   * @see Frame#isTracked()
   */
  public boolean isTrackedFrame(String hid, Frame frame) {
    return trackedFrame(hid) == frame;
  }

  /**
   * Resets all HID's {@link #trackedFrame(String)}.
   *
   * @see #trackedFrame(String)
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   */
  public void resetTracking() {
    _agents.clear();
  }

  /**
   * Same as {@code resetTrackedFrame(null)}.
   *
   * @see #resetTrackedFrame(String)
   */
  public void resetTrackedFrame() {
    resetTrackedFrame(null);
  }

  /**
   * Resets the current {@code hid} {@link #trackedFrame(String)} so that a call to {@link #isTracking(String)}
   * will return {@code false}. Note that {@link #track(String, float, float)} will reset the tracked frame automatically.
   *
   * @see #trackedFrame(String)
   * @see #defaultFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   */
  public void resetTrackedFrame(String hid) {
    _agents.remove(hid);
  }

  /**
   * Same as {@code return defaultFrame(null)}.
   *
   * @see #defaultFrame(String)
   */
  public Frame defaultFrame() {
    return defaultFrame(null);
  }

  /**
   * Returns the {@code hid} default-frame which is used by methods dealing interactivity that don take a frame
   * param. Same as {@code return trackedFrame(hid) == null ? eye() : trackedFrame(hid)}. Never returns {@code null}.
   *
   * @see #trackedFrame(String)
   * @see #resetTrackedFrame(String)
   * @see #tracks(float, float, Frame)
   * @see #track(String, float, float)
   * @see #setTrackedFrame(String, Frame)
   * @see #isTrackedFrame(String, Frame)
   */
  public Frame defaultFrame(String hid) {
    return trackedFrame(hid) == null ? eye() : trackedFrame(hid);
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
   * Same as {@code align(defaultFrame(hid))}.
   *
   * @see #alignWith(Frame)
   * @see #defaultFrame(String)
   */
  public void align(String hid) {
    alignWith(defaultFrame(hid));
  }

  /**
   * If {@code frame} is {@link #isEye(Frame)} aligns the {@link #eye()} with the world.
   * Otherwise aligns the {@code frame} with the {@link #eye()}. {@code frame} should be
   * non-null.
   * <p>
   * Wrapper method for {@link Frame#align(boolean, float, Frame)}.
   *
   * @see #isEye(Frame)
   * @see #defaultFrame(String)
   */
  public void alignWith(Frame frame) {
    if (frame == null)
      throw new RuntimeException("align(frame) requires a non-null frame param");
    if (isEye(frame))
      frame.align(true);
    else
      frame.align(eye());
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
   * Same as {@code focus(defaultFrame())}.
   *
   * @see #focusWith(Frame)
   * @see #defaultFrame(String)
   */
  public void focus(String hid) {
    focusWith(defaultFrame(hid));
  }

  /**
   * Centers the frame into the graph.
   */
  public void focusWith(Frame frame) {
    if (frame == null)
      throw new RuntimeException("focus(frame) requires a non-null frame param");
    if (isEye(frame))
      frame.projectOnLine(center(), viewDirection());
    else
      frame.projectOnLine(eye().position(), eye().zAxis(false));
  }

  // Screen to frame conversion

  /**
   * Convenience function that simply returns {@code screenLocation(src, null)}.
   *
   * @see #screenLocation(Vector, Frame)
   */
  public Vector screenLocation(Vector vector) {
    return screenLocation(vector, null);
  }

  /**
   * Converts {@code vector} location from {@code frame} to screen.
   * Use {@code location(vector, frame)} to perform the inverse transformation.
   * <p>
   * The x and y coordinates of the returned vector are expressed in screen coordinates,
   * (0,0) being the upper left corner of the window. The z coordinate ranges between 0
   * (near plane) and 1 (excluded, far plane).
   *
   * @see #screenLocation(Vector)
   * @see #location(Vector, Frame)
   * @see #location(Vector)
   */
  public Vector screenLocation(Vector vector, Frame frame) {
    float xyz[] = new float[3];

    if (frame != null) {
      Vector tmp = frame.worldLocation(vector);
      _screenLocation(tmp._vector[0], tmp._vector[1], tmp._vector[2], xyz);
    } else
      _screenLocation(vector._vector[0], vector._vector[1], vector._vector[2], xyz);

    return new Vector(xyz[0], xyz[1], xyz[2]);
  }

  // cached version
  protected boolean _screenLocation(float objx, float objy, float objz, float[] windowCoordinate) {
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
   * Convenience function that simply returns {@code location(pixel, null)}.
   * <p>
   * #see {@link #location(Vector, Frame)}
   */
  public Vector location(Vector pixel) {
    return this.location(pixel, null);
  }

  /**
   * Returns the {@code frame} coordinates of {@code pixel}.
   * <p>
   * The pixel (0,0) corresponds to the upper left corner of the window. The
   * {@code pixel.z()} is a depth value ranging in [0..1] (near and far plane respectively).
   * In 3D note that {@code pixel.z} is not a linear interpolation between {@link #zNear()} and
   * {@link #zFar()};
   * {@code pixel.z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z);} where {@code z}
   * is the distance from the point you project to the camera, along the {@link #viewDirection()}.
   * <p>
   * The result is expressed in the {@code frame} coordinate system. When {@code frame} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code frame} hierarchy (i.e., when {@link Frame#reference()} is non-null) is taken into
   * account.
   * <p>
   * {@link #screenLocation(Vector, Frame)} performs the inverse transformation.
   * <p>
   * This method only uses the intrinsic eye parameters (view and projection matrices),
   * {@link #width()} and {@link #height()}). You can hence define a virtual eye and use
   * this method to compute un-projections out of a classical rendering context.
   * <p>
   * This method is not computationally optimized by default. If you call it several times with no
   * change in the matrices, you should buffer the inverse of the projection times view matrix
   * to speed-up the queries. See {@link #cacheProjectionViewInverse(boolean)}.
   *
   * @see #screenLocation(Vector, Frame)
   * @see #setWidth(int)
   * @see #setHeight(int)
   */
  public Vector location(Vector pixel, Frame frame) {
    float xyz[] = new float[3];
    _location(pixel._vector[0], pixel._vector[1], pixel._vector[2], xyz);
    if (frame != null)
      return frame.location(new Vector(xyz[0], xyz[1], xyz[2]));
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

  // Gesture physical interface is quite nice!
  // It always maps physical (screen) space geom data respect to the eye

  /**
   * Same as {@code zoom(null, delta)}.
   *
   * @see #zoom(String, float)
   */
  public void zoom(float delta) {
    zoom(null, delta);
  }

  /**
   * Same as {@code zoom(delta, defaultFrame(hid))}.
   *
   * @see #translate(float, float, Frame)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Frame)
   * @see #translate(String, float, float, float)
   * @see #zoom(float, Frame)
   * @see #defaultFrame(String)
   */
  public void zoom(String hid, float delta) {
    zoom(delta, defaultFrame(hid));
  }

  /**
   * Same as {@code translate(0, 0, delta, frame)}.
   *
   * @see #translate(float, float, Frame)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Frame)
   * @see #translate(String, float, float, float)
   * @see #zoom(String, float)
   * @see #defaultFrame(String)
   */
  public void zoom(float delta, Frame frame) {
    translate(0, 0, delta, frame);
  }

  /**
   * Same as {@code translate(null, dx, dy)}.
   *
   * @see #translate(String, float, float)
   */
  public void translate(float dx, float dy) {
    translate(null, dx, dy);
  }

  /**
   * Same as {@code translate(dx, dy, 0, defaultFrame(hid))}.
   *
   * @see #translate(float, float, Frame)
   * @see #translate(float, float, float, Frame)
   * @see #translate(String, float, float, float)
   * @see #zoom(String, float)
   * @see #zoom(float, Frame)
   * @see #defaultFrame(String)
   */
  public void translate(String hid, float dx, float dy) {
    translate(dx, dy, 0, defaultFrame(hid));
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
   * Same as {@code translate(dx, dy, dz, defaultFrame(hid))}.
   *
   * @see #translate(float, float, Frame)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Frame)
   * @see #zoom(float, Frame)
   * @see #zoom(String, float)
   * @see #defaultFrame(String)
   */
  public void translate(String hid, float dx, float dy, float dz) {
    translate(dx, dy, dz, defaultFrame(hid));
  }

  /**
   * Same as {@code translate(dx, dy, 0, frame)}.
   *
   * @see #translate(String, float, float, float)
   * @see #translate(String, float, float)
   * @see #translate(float, float, float, Frame)
   * @see #zoom(float, Frame)
   * @see #zoom(String, float)
   * @see #defaultFrame(String)
   */
  public void translate(float dx, float dy, Frame frame) {
    translate(dx, dy, 0, frame);
  }

  /**
   * Translates the {@code frame} according to {@code dx}, {@code dy} and {@code dz}. The {@code dx} and {@code dy}
   * coordinates are expressed in screen space, and the {@code dz} coordinate is given in world units.
   * The translated frame would be kept exactly under a pointer if such a device were used to translate it.
   *
   * @see #translate(float, float, Frame)
   * @see #translate(String, float, float)
   * @see #translate(String, float, float, float)
   * @see #zoom(float, Frame)
   * @see #zoom(String, float)
   * @see #defaultFrame(String)
   */
  public void translate(float dx, float dy, float dz, Frame frame) {
    if (frame == null)
      throw new RuntimeException("translate(vector, frame) requires a non-null frame param");
    frame.translate(_translate(dx, dy, dz, frame));
  }

  /**
   * Same as {@code return _translate(dx, dy, dz, Math.min(width(), height()), frame)}.
   *
   * @see #_translate(float, float, float, int, Frame)
   */
  protected Vector _translate(float dx, float dy, float dz, Frame frame) {
    return _translate(dx, dy, dz, Math.min(width(), height()), frame);
  }

  /**
   * Interactive translation low-level implementation. Converts {@code dx} and {@code dy} defined in screen space to
   * {@link Frame#reference()} (or world coordinates if the frame reference is null).
   * <p>
   * The projection onto the screen of the returned vector exactly match the screen {@code (dx, dy)} vector displacement
   * (e.g., the translated frame would be kept exactly under a pointer if such a device were used to translate it).
   * The z-coordinate is mapped from [0..{@code zMax}] to the [0..2*{@link #radius()}}] range.
   */
  protected Vector _translate(float dx, float dy, float dz, int zMax, Frame frame) {
    if (is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    }
    dx = isEye(frame) ? -dx : dx;
    dy = isRightHanded() ^ isEye(frame) ? -dy : dy;
    dz = isEye(frame) ? dz : -dz;
    // Scale to fit the screen relative vector displacement
    switch (type()) {
      case PERSPECTIVE:
        float k = (float) Math.tan(fieldOfView() / 2.0f) * Math.abs(
            eye().location(isEye(frame) ? anchor() : frame.position())._vector[2] * eye().magnitude());
        //TODO check me weird to find height instead of width working (may it has to do with fov?)
        dx *= 2.0 * k / height();
        dy *= 2.0 * k / height();
        break;
      case TWO_D:
      case ORTHOGRAPHIC:
        float[] wh = boundaryWidthHeight();
        dx *= 2.0 * wh[0] / width();
        dy *= 2.0 * wh[1] / height();
        break;
    }
    // this expresses the dz coordinate in world units:
    //Vector eyeVector = new Vector(dx / eye().magnitude(), dy / eye().magnitude(), dz / eye().magnitude());
    Vector eyeVector = new Vector(dx / eye().magnitude(), dy / eye().magnitude(), dz * 2 * radius() / zMax);
    return frame.reference() == null ? eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, eye());
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
   * Same as {@code scale(delta, defaultFrame(hid))}.
   *
   * @see #scale(float, Frame)
   * @see #defaultFrame(String)
   */
  public void scale(String hid, float delta) {
    scale(delta, defaultFrame(hid));
  }

  /**
   * Scales the {@code frame} according to {@code delta}. Note that if {@code frame} is the {@link #eye()}
   * this call simply changes the {@link #fieldOfView()}.
   *
   * @see #scale(String, float)
   */
  public void scale(float delta, Frame frame) {
    float factor = 1 + Math.abs(delta) / (float) (isEye(frame) ? -height() : height());
    frame.scale(delta >= 0 ? factor : 1 / factor);
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
   * Rotates the {@code hid} default-frame (see {@link #defaultFrame(String)}) roll, pitch and yaw radians around screen
   * space x, y and z axes, respectively.
   *
   * @see #rotate(float, float, float, Frame)
   */
  public void rotate(String hid, float roll, float pitch, float yaw) {
    rotate(roll, pitch, yaw, defaultFrame(hid));
  }

  /**
   * Rotates the {@code frame} {@code roll}, {@code pitch} and {@code yaw} radians relative to the screen space
   * x, y and z axes, respectively. The center of the rotation is the graph {@link #anchor()} if the frame is the
   * {@link #eye()}, or the frame origin (see {@link Frame#position()}) otherwise.
   * <p>
   * To rotate an eye frame around its origin and local axes simply call:
   * {@code eye().rotate(new Quaternion(roll, pitch, yaw))}.
   *
   * @see #rotate(String, float, float, float)
   * @see #spin(Point, Point, float, Frame)
   */
  public void rotate(float roll, float pitch, float yaw, Frame frame) {
    if (frame == null)
      throw new RuntimeException("rotate(roll, pitch, yaw, frame) requires a non-null frame param");
    spin(_rotate(roll, pitch, yaw, frame), frame);
  }

  /**
   * Low-level roll-pitch and yaw rotation. Axes are physical, i.e., screen space.
   */
  protected Quaternion _rotate(float roll, float pitch, float yaw, Frame frame) {
    if (is2D() && (roll != 0 || pitch != 0)) {
      roll = 0;
      pitch = 0;
      System.out.println("Warning: graph is 2D. Roll and/or pitch reset");
    }
    // don't really need to differentiate among the two cases, but eyeFrame can be speeded up
    Quaternion quaternion = new Quaternion(isLeftHanded() ? -roll : roll, pitch, isLeftHanded() ? -yaw : yaw);
    if (isEye(frame))
      return quaternion;
    else {
      Vector vector = new Vector(-quaternion.x(), -quaternion.y(), -quaternion.z());
      vector = eye().orientation().rotate(vector);
      vector = frame.displacement(vector);
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
   * Same as {@code spin(tail, head, defaultFrame(hid))}.
   *
   * @see #spin(Point, Point, float, Frame)
   * @see #spin(Point, Point, Frame)
   * @see #spin(String, Point, Point, float)
   */
  public void spin(String hid, Point tail, Point head) {
    spin(tail, head, defaultFrame(hid));
  }

  /**
   * Same as {@code spin(tail, head, 1, frame)}.
   *
   * @see #spin(Point, Point, float, Frame)
   * @see #spin(String, Point, Point)
   * @see #spin(String, Point, Point, float)
   */
  public void spin(Point tail, Point head, Frame frame) {
    spin(tail, head, 1, frame);
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
   * Same as {@code spin(tail, head, sensitivity, defaultFrame(hid))}.
   *
   * @see #spin(Point, Point, float, Frame)
   * @see #spin(String, Point, Point)
   * @see #spin(Point, Point, Frame)
   * @see #defaultFrame(String)
   */
  public void spin(String hid, Point tail, Point head, float sensitivity) {
    spin(tail, head, sensitivity, defaultFrame(hid));
  }

  /**
   * Rotates the {@code frame} using an arcball interface, from {@code tail} to {@code head} pixel positions. The
   * {@code sensitivity} controls the gesture strength. The center of the rotation is the graph {@link #anchor()}
   * if the frame is the {@link #eye()}, or the frame origin (see {@link Frame#position()}) otherwise.
   * <p>
   * For implementation details refer to Shoemake 92 paper: Arcball: a user interface for specifying three-dimensional
   * orientation using a mouse.
   * <p>
   * Override this class an call {@link #_spin(Point, Point, Point, float, Frame)} if you want to define a different
   * rotation center (rare).
   *
   * @see #spin(String, Point, Point)
   * @see #spin(Point, Point, Frame)
   * @see #spin(String, Point, Point, float)
   * @see #rotate(float, float, float, Frame)
   */
  public void spin(Point tail, Point head, float sensitivity, Frame frame) {
    if (frame == null)
      throw new RuntimeException("spin(point1, point2, sensitivity, frame) requires a non-null frame param");
    spin(_spin(tail, head, sensitivity, frame), frame);
  }

  /**
   * Same as {@code return _spin(point1, point2, center, sensitivity, frame)} where {@code center} is {@link #anchor()}
   * if the frame is the {@link #eye()} or {@link Frame#position()} otherwise.
   */
  protected Quaternion _spin(Point point1, Point point2, float sensitivity, Frame frame) {
    Vector vector = screenLocation(isEye(frame) ? anchor() : frame.position());
    Point center = new Point(vector.x(), vector.y());
    return _spin(point1, point2, center, sensitivity, frame);
  }

  /**
   * Computes the classical arcball quaternion. Refer to Shoemake 92 paper: Arcball: a user interface for specifying
   * three-dimensional orientation using a mouse.
   */
  protected Quaternion _spin(Point point1, Point point2, Point center, float sensitivity, Frame frame) {
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
    if (!isEye(frame)) {
      Vector vector = quaternion.axis();
      vector = eye().orientation().rotate(vector);
      vector = frame.displacement(vector);
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
   * Rotates the frame using {@code quaternion} around its {@link Frame#position()} (non-eye frames)
   * or around the {@link Graph#anchor()} when the {@code frame} is the {@link Graph#eye()}.
   */
  public void spin(Quaternion quaternion, Frame frame) {
    if (isEye(frame))
      //same as:
      //frame.orbit(new Quaternion(frame.worldDisplacement(quaternion.axis()), quaternion.angle()));
      frame._orbit(quaternion, anchor());
    else
      frame.rotate(quaternion);
  }

  // only 3d eye

  /**
   * Look around (without translating the eye) according to angular displacements {@code deltaX} and {@code deltaY}
   * expressed in radians.
   */
  public void lookAround(float deltaX, float deltaY) {
    eye().rotate(_lookAround(deltaX, deltaY));
  }

  /**
   * Look around without moving the eye while preserving its {@link Frame#yAxis()} when the action began.
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
   * {@link Frame#setYAxis(Vector)}) and {@link #fitBall()} first.
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
   * Same as {@code control(defaultFrame(), gesture)}. This method implements application control with
   * the {@link #defaultFrame()} which requires overriding {@link Frame#interact(Object...)}.
   *
   * @see #defaultFrame()
   * @see #control(Frame, Object...)
   */
  public void defaultHIDControl(Object... gesture) {
    control(defaultFrame(), gesture);
  }

  /**
   * Same as {@code control(defaultFrame(hid), gesture)}. This method implements application control with
   * the {@link #defaultFrame(String)} which requires overriding {@link Frame#interact(Object...)}.
   *
   * @see #defaultFrame(String)
   * @see #control(Frame, Object...)
   */
  public void control(String hid, Object... gesture) {
    control(defaultFrame(hid), gesture);
  }

  /**
   * Same as {@code frame.interact(gesture)}.
   *
   * @see #defaultHIDControl(Object...)
   * @see #control(String, Object...)
   * @see Frame#interact(Object...)
   */
  public void control(Frame frame, Object... gesture) {
    frame.interact(gesture);
  }

  /*
  // nice interactivity examples: spinning (spin + timer), moveForward, moveBackward, spinX/Y/Z
  // screenRotate/Translate.

  public void spin(Point point1, Point point2, Point center) {
    spin(point1, point2, center, defaultFrame());
  }

  public void spin(Point point1, Point point2, Point center, Frame frame) {
    if (frame == null)
      throw new RuntimeException("spin(point1, point2, center, frame) requires a non-null frame param");
    spin(_spin(point1, point2, center, 1, frame), frame);
  }

  public void spin(Point point1, Point point2, Point center, float sensitivity) {
    spin(point1, point2, center, sensitivity, defaultFrame());
  }

  public void spin(Point point1, Point point2, Point center, float sensitivity, Frame frame) {
    if (frame == null)
      throw new RuntimeException("spin(point1, point2, center, sensitivity, frame) requires a non-null frame param");
    spin(_spin(point1, point2, center, sensitivity, frame), frame);
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

  public void screenRotate(Point point1, Point point2, Frame frame) {
    screenRotate(point1, point2, 1, frame);
  }

  public void screenRotate(Point point1, Point point2, float sensitivity, Frame frame) {
    spin(_spin2(point1, point2, sensitivity, frame), frame);
  }

  protected Quaternion _spin2(Point point1, Point point2, float sensitivity, Frame frame) {
    Quaternion quaternion;
    Vector vector;
    float x = point2.x();
    float y = point2.y();
    float prevX = point1.x();
    float prevY = point1.y();
    float angle;
    if (isEye(frame)) {
      vector = screenLocation(anchor());
      angle = (float) Math.atan2(y - vector._vector[1], x - vector._vector[0]) - (float) Math
          .atan2(prevY - vector._vector[1], prevX - vector._vector[0]);
      if (isLeftHanded())
        angle = -angle;
      quaternion = new Quaternion(new Vector(0.0f, 0.0f, 1.0f), angle);
    } else {
      vector = screenLocation(frame.position());
      float prev_angle = (float) Math.atan2(prevY - vector._vector[1], prevX - vector._vector[0]);
      angle = (float) Math.atan2(y - vector._vector[1], x - vector._vector[0]);
      Vector axis = frame.displacement(eye().orientation().rotate(new Vector(0.0f, 0.0f, -1.0f)));
      if (isRightHanded())
        quaternion = new Quaternion(axis, angle - prev_angle);
      else
        quaternion = new Quaternion(axis, prev_angle - angle);
    }
    return quaternion;
  }
  */
}