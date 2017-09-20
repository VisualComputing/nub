/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.dandelion.core;

import remixlab.dandelion.geom.*;
import remixlab.util.Copyable;
import remixlab.util.Util;

import java.util.ArrayList;

/**
 * 3D implementation of the {@link remixlab.dandelion.core.Eye} abstract class. This class
 * API aims to conform that of the great
 * <a href="http://libqglviewer.com/refManual/classqglviewer_1_1Camera.html">libQGLViewer
 * Camera</a>.
 * <p>
 * The Camera {@link #type()} can be {@code ORTHOGRAPHIC} or {@code PERSPECTIVE} (
 * {@link #fieldOfView()} is meaningless in the latter case).
 * <p>
 * The near and far planes of the Camera are fitted to the scene and determined from the
 * {@link remixlab.dandelion.core.AbstractScene#radius()},
 * {@link remixlab.dandelion.core.AbstractScene#center()} and
 * {@link #zClippingCoefficient()} by the {@link #zNear()} and {@link #zFar()}. Reasonable
 * values on the scene extends thus have to be provided to the Scene in order for the
 * Camera to correctly display the scene. High level positioning methods also use this
 * information ({@link #showEntireScene()}, {@link #centerScene()}, ...).
 * <p>
 * Stereo display is possible on devices with quad buffer capabilities (with
 * {@code PERSPECTIVE} {@link #type()} only).
 * <p>
 * <b>Attention: </b> the {@link #frame()}
 * {@link remixlab.dandelion.geom.Frame#magnitude()} is used to set the
 * {@link #fieldOfView()} or compute {@link #getBoundaryWidthHeight()} if the camera
 * {@link #type()} is {@code PERSPECTIVE} or {@code ORTHOGRAPHIC}, respectively. The
 * Camera magnitude is thus generally different from that of the scene. Use
 * {@link #eyeCoordinatesOf(Vec)} and {@link #worldCoordinatesOf(Vec)} (or any of the
 * powerful Frame transformations (
 * {@link remixlab.dandelion.geom.Frame#coordinatesOf(Vec)},
 * {@link remixlab.dandelion.geom.Frame#transformOf(Vec)}, ...)) to convert to and from
 * the Eye {@link #frame()} coordinate system.
 */
public class Camera extends Eye implements Copyable {
  /**
   * Enumerates the two possible types of Camera.
   * <p>
   * This type mainly defines different camera projection matrix. Many other methods take
   * this Type into account.
   */
  public enum Type {
    PERSPECTIVE, ORTHOGRAPHIC
  }

  ;

  // C a m e r a p a r a m e t e r s
  private float zNearCoef;
  private float zClippingCoef;
  private Type tp; // PERSPECTIVE or ORTHOGRAPHIC

  // S t e r e o p a r a m e t e r s
  private float IODist; // inter-ocular distance, in meters
  private float focusDist; // in scene units
  private float physicalDist2Scrn; // in meters
  private float physicalScrnWidth; // in meters

  // rescale ortho when anchor changes
  private float rapK = 1;

  // Inverse the direction of an horizontal mouse motion. Depends on the
  // projected
  // screen orientation of the vertical axis when the mouse button is pressed.
  public boolean cadRotationIsReversed;

  /**
   * Main constructor.
   * <p>
   * {@link #sceneCenter()} is set to (0,0,0) and {@link #sceneRadius()} is set to 100.
   * {@link #type()} Camera.PERSPECTIVE, with a {@code PI/3} {@link #fieldOfView()} (same
   * value used in P5 by default).
   * <p>
   * Camera matrices (projection and view) are created and computed according to remaining
   * default Camera parameters.
   * <p>
   * See {@link #IODistance()}, {@link #physicalDistanceToScreen()},
   * {@link #physicalScreenWidth()} and {@link #focusDistance()} documentations for
   * default stereo parameter values.
   */
  public Camera(AbstractScene scn) {
    super(scn);

    if (gScene.is2D())
      throw new RuntimeException("Use Camera only for a 3D Scene");

    // dist = new float[6];
    // normal = new Vec[6];
    // for (int i = 0; i < normal.length; i++) normal[i] = new Vec();

    // fldOfView = (float) Math.PI / 3.0f; //in Proscene 1.x it was Pi/4
    // setFieldOfView((float) Math.PI / 2.0f);//fov yMagnitude -> 1
    // setFieldOfView((float) Math.PI / 3.0f);
    // Initial value (only scaled after this)
    // orthoCoef = (float)Math.tan(fieldOfView() / 2.0f);

    // fpCoefficients = new float[6][4];

    // Initial value (only scaled after this)
    // orthoCoef = (float) Math.tan(fieldOfView() / 2.0f);

    // Requires fieldOfView() when called with ORTHOGRAPHIC. Attention to
    // projectionMat below.
    setType(Camera.Type.PERSPECTIVE);
    setZNearCoefficient(0.005f);
    setZClippingCoefficient((float) Math.sqrt(3.0f));

    // Stereo parameters
    setIODistance(0.062f);
    setPhysicalDistanceToScreen(0.5f);
    setPhysicalScreenWidth(0.4f);
    // focusDistance is set from setFieldOfView()

    computeProjection();
  }

  protected Camera(Camera oCam) {
    super(oCam);
    this.setType(oCam.type());
    this.setZNearCoefficient(oCam.zNearCoefficient());
    this.setZClippingCoefficient(oCam.zClippingCoefficient());
    this.setIODistance(oCam.IODistance());
    this.setPhysicalDistanceToScreen(oCam.physicalDistanceToScreen());
    this.setPhysicalScreenWidth(oCam.physicalScreenWidth());
    this.rapK = oCam.rapK;
  }

  @Override
  public Camera get() {
    return new Camera(this);
  }

  // 2. POSITION AND ORIENTATION

  @Override
  public void setUpVector(Vec up, boolean noMove) {
    Quat q = new Quat(new Vec(0.0f, 1.0f, 0.0f), frame().transformOf(up));

    if (!noMove && gScene.is3D())
      frame().setPosition(Vec.subtract(anchor(),
          (Quat.multiply((Quat) frame().orientation(), q)).rotate(frame().coordinatesOf(anchor()))));

    frame().rotate(q);

    // Useful in fly mode to keep the horizontal direction.
    frame().updateSceneUpVector();
  }

  /**
   * Same as {@code setUpVector(new Vec(x,y,z))}.
   *
   * @see #setUpVector(Vec)
   */
  public void setUpVector(float x, float y, float z) {
    setUpVector(new Vec(x, y, z));
  }

  /**
   * Same as {@code setUpVector(new Vec(x,y,z), boolean noMove)}.
   *
   * @see #setUpVector(Vec, boolean)
   */
  public void setUpVector(float x, float y, float z, boolean noMove) {
    setUpVector(new Vec(x, y, z), noMove);
  }

  /**
   * Same as {@code setPosition(new Vec(x,y,z))}.
   *
   * @see #setPosition(Vec)
   */
  public void setPosition(float x, float y, float z) {
    setPosition(new Vec(x, y, z));
  }

  @Override
  public Vec viewDirection() {
    return frame().zAxis(false);
  }

  /**
   * Rotates the Camera so that its {@link #viewDirection()} is {@code direction} (defined
   * in the world coordinate system).
   * <p>
   * The Camera {@link #position()} is not modified. The Camera is rotated so that the
   * horizon (defined by its {@link #upVector()}) is preserved.
   *
   * @see #lookAt(Vec)
   * @see #setUpVector(Vec)
   */
  public void setViewDirection(Vec direction) {
    if (Util.zero(direction.squaredNorm()))
      return;

    Vec xAxis = direction.cross(upVector());
    if (Util.zero(xAxis.squaredNorm())) {
      // target is aligned with upVector, this means a rotation around X axis
      // X axis is then unchanged, let's keep it !
      xAxis = frame().xAxis();
    }

    Quat q = new Quat();
    q.fromRotatedBasis(xAxis, xAxis.cross(direction), Vec.multiply(direction, -1));
    frame().setOrientationWithConstraint(q);
  }

  /**
   * Same as {@code setViewDirection(new Vec(x, y, z))}.
   *
   * @see #setViewDirection(Vec)
   */
  public void setViewDirection(float x, float y, float z) {
    setViewDirection(new Vec(x, y, z));
  }

  /**
   * Sets the {@link #orientation()} of the Camera using polar coordinates.
   * <p>
   * {@code theta} rotates the Camera around its Y axis, and then {@code phi} rotates it
   * around its X axis.
   * <p>
   * The polar coordinates are defined in the world coordinates system:
   * {@code theta = phi = 0} means that the Camera is directed towards the world Z axis.
   * Both angles are expressed in radians.
   * <p>
   * The {@link #position()} of the Camera is unchanged, you may want to call
   * {@link #showEntireScene()} after this method to move the Camera.
   *
   * @see #setUpVector(Vec)
   */
  public void setOrientation(float theta, float phi) {
    // TODO: need check.
    Vec axis = new Vec(0.0f, 1.0f, 0.0f);
    Quat rot1 = new Quat(axis, theta);
    axis.set(-(float) Math.cos(theta), 0.0f, (float) Math.sin(theta));
    Quat rot2 = new Quat(axis, phi);
    setOrientation(Quat.multiply(rot1, rot2));
  }

  @Override
  public void setOrientation(Rotation q) {
    frame().setOrientation(q);
    frame().updateSceneUpVector();
  }

  // 3. FRUSTUM

  /**
   * Returns the Camera.Type.
   * <p>
   * Set by {@link #setType(Type)}.
   * <p>
   * A {@link remixlab.dandelion.core.Camera.Type#PERSPECTIVE} Camera uses a classical
   * projection mainly defined by its {@link #fieldOfView()}.
   * <p>
   * With a {@link remixlab.dandelion.core.Camera.Type#ORTHOGRAPHIC} {@link #type()}, the
   * {@link #fieldOfView()} is meaningless and the width and height of the Camera frustum
   * are inferred from the distance to the {@link #anchor()} using
   * {@link #getBoundaryWidthHeight()}.
   * <p>
   * Both types use {@link #zNear()} and {@link #zFar()} (to define their clipping planes)
   * and {@link #aspectRatio()} (for frustum shape).
   */
  public final Type type() {
    return tp;
  }

  /**
   * Defines the Camera {@link #type()}.
   * <p>
   * Changing the Camera Type alters the viewport and the objects' size can be changed.
   * This method guarantees that the two frustum match in a plane normal to
   * {@link #viewDirection()}, passing through the arcball reference point.
   */
  public final void setType(Type type) {
    if (type != type()) {
      modified();
      this.tp = type;
    }
  }

  /**
   * Returns the vertical field of view of the Camera (in radians) computed as
   * {@code 2.0f * (float) Math.atan(frame().magnitude())}.
   * <p>
   * Value is set using {@link #setFieldOfView(float)}. Default value is pi/3 radians.
   * This value is meaningless if the Camera {@link #type()} is
   * {@link remixlab.dandelion.core.Camera.Type#ORTHOGRAPHIC}.
   * <p>
   * The field of view corresponds the one used in {@code gluPerspective} (see manual). It
   * sets the Y (vertical) aperture of the Camera. The X (horizontal) angle is inferred
   * from the window aspect ratio (see {@link #aspectRatio()} and
   * {@link #horizontalFieldOfView()}).
   * <p>
   * Use {@link #setFOVToFitScene()} to adapt the {@link #fieldOfView()} to a given scene.
   *
   * @see #setFieldOfView(float)
   */
  public float fieldOfView() {
    return 2.0f * (float) Math.atan(frame().magnitude());
  }

  /**
   * Sets the vertical {@link #fieldOfView()} of the Camera (in radians). The
   * {@link #fieldOfView()} is encapsulated as the camera
   * {@link remixlab.dandelion.geom.Frame#magnitude()} using the following expression:
   * {@code frame().setMagnitude((float) Math.tan(fov / 2.0f))}.
   * <p>
   * Note that {@link #focusDistance()} is set to {@link #sceneRadius()} / tan(
   * {@link #fieldOfView()}/2) by this method.
   *
   * @see #fieldOfView()
   */
  public void setFieldOfView(float fov) {
    // fldOfView = fov;
    frame().setMagnitude((float) Math.tan(fov / 2.0f));
    setFocusDistance(sceneRadius() / frame().magnitude());
  }

  /**
   * Changes the Camera {@link #fieldOfView()} so that the entire scene (defined by
   * {@link remixlab.dandelion.core.AbstractScene#center()} and
   * {@link remixlab.dandelion.core.AbstractScene#radius()} is visible from the Camera
   * {@link #position()}.
   * <p>
   * The {@link #position()} and {@link #orientation()} of the Camera are not modified and
   * you first have to orientate the Camera in order to actually see the scene (see
   * {@link #lookAt(Vec)}, {@link #showEntireScene()} or {@link #fitBall(Vec, float)}).
   * <p>
   * This method is especially useful for <i>shadow maps</i> computation. Use the Camera
   * positioning tools ( {@link #setPosition(Vec)}, {@link #lookAt(Vec)}) to position a
   * Camera at the light position. Then use this method to define the
   * {@link #fieldOfView()} so that the shadow map resolution is optimally used:
   * <p>
   * {@code // The light camera needs size hints in order to optimize its
   * fieldOfView} <br>
   * {@code lightCamera.setSceneRadius(sceneRadius());} <br>
   * {@code lightCamera.setSceneCenter(sceneCenter());} <br>
   * {@code // Place the light camera} <br>
   * {@code lightCamera.setPosition(lightFrame.position());} <br>
   * {@code lightCamera.lookAt(sceneCenter());} <br>
   * {@code lightCamera.setFOVToFitScene();} <br>
   * <p>
   * <b>Attention:</b> The {@link #fieldOfView()} is clamped to M_PI/2.0. This happens
   * when the Camera is at a distance lower than sqrt(2.0) * sceneRadius() from the
   * sceneCenter(). It optimizes the shadow map resolution, although it may miss some
   * parts of the scene.
   */
  public void setFOVToFitScene() {
    if (distanceToSceneCenter() > (float) Math.sqrt(2.0f) * sceneRadius())
      setFieldOfView(2.0f * (float) Math.asin(sceneRadius() / distanceToSceneCenter()));
    else
      setFieldOfView((float) Math.PI / 2.0f);
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
    // return 2.0f * (float) Math.atan((float) Math.tan(fieldOfView() / 2.0f) *
    // aspectRatio());
    return 2.0f * (float) Math.atan(frame().magnitude() * aspectRatio());
  }

  /**
   * Sets the {@link #horizontalFieldOfView()} of the Camera (in radians).
   * <p>
   * {@link #horizontalFieldOfView()} and {@link #fieldOfView()} are linked by the
   * {@link #aspectRatio()}. This method actually calls
   * {@code setFieldOfView(( 2.0 * atan (tan(hfov / 2.0) / aspectRatio()) ))} so that a
   * call to {@link #horizontalFieldOfView()} returns the expected value.
   */
  public void setHorizontalFieldOfView(float hfov) {
    setFieldOfView(2.0f * (float) Math.atan((float) Math.tan(hfov / 2.0f) / aspectRatio()));
  }

  /**
   * Returns the near clipping plane distance used by the Camera projection matrix in
   * scene (world) units.
   * <p>
   * The clipping planes' positions depend on the {@link #sceneRadius()} and
   * {@link #sceneCenter()} rather than being fixed small-enough and large-enough values.
   * A good scene dimension approximation will hence result in an optimal precision of the
   * z-buffer.
   * <p>
   * The near clipping plane is positioned at a distance equal to
   * {@link #zClippingCoefficient()} * {@link #sceneRadius()} in front of the
   * {@link #sceneCenter()}: {@code distanceToSceneCenter() -
   * zClippingCoefficient() * sceneRadius()}
   * <p>
   * In order to prevent negative or too small {@link #zNear()} values (which would
   * degrade the z precision), {@link #zNearCoefficient()} is used when the Camera is
   * inside the {@link #sceneRadius()} sphere:
   * <p>
   * {@code zMin = zNearCoefficient() * zClippingCoefficient() * sceneRadius();} <br>
   * {@code zNear = zMin;}<br>
   * {@code // With an ORTHOGRAPHIC type, the value is simply clamped to 0.0} <br>
   * <p>
   * See also the {@link #zFar()}, {@link #zClippingCoefficient()} and
   * {@link #zNearCoefficient()} documentations.
   * <p>
   * If you need a completely different zNear computation, overload the {@link #zNear()}
   * and {@link #zFar()} methods in a new class that publicly inherits from Camera and use
   * {@link remixlab.dandelion.core.AbstractScene#setEye(Eye)}.
   * <p>
   * <b>Attention:</b> The value is always positive although the clipping plane is
   * positioned at a negative z value in the Camera coordinate system. This follows the
   * {@code gluPerspective} standard.
   *
   * @see #zFar()
   */
  public float zNear() {
    float z = distanceToSceneCenter() - zClippingCoefficient() * sceneRadius();

    // Prevents negative or null zNear values.
    final float zMin = zNearCoefficient() * zClippingCoefficient() * sceneRadius();
    if (z < zMin)
      switch (type()) {
        case PERSPECTIVE:
          z = zMin;
          break;
        case ORTHOGRAPHIC:
          z = 0.0f;
          break;
      }
    return z;
  }

  /**
   * Returns the far clipping plane distance used by the Camera projection matrix in scene
   * (world) units.
   * <p>
   * The far clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * sceneRadius()} behind the {@link #sceneCenter()}:
   * <p>
   * {@code zFar = distanceToSceneCenter() + zClippingCoefficient()*sceneRadius()}
   *
   * @see #zNear()
   */
  public float zFar() {
    return distanceToSceneCenter() + zClippingCoefficient() * sceneRadius();
  }

  /**
   * Returns the coefficient which is used to set {@link #zNear()} when the Camera is
   * inside the sphere defined by {@link #sceneCenter()} and
   * {@link #zClippingCoefficient()} * {@link #sceneRadius()}.
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
    return zNearCoef;
  }

  /**
   * Sets the {@link #zNearCoefficient()} value.
   */
  public void setZNearCoefficient(float coef) {
    if (coef != zNearCoef)
      modified();
    zNearCoef = coef;
  }

  /**
   * Returns the coefficient used to position the near and far clipping planes.
   * <p>
   * The near (resp. far) clipping plane is positioned at a distance equal to
   * {@code zClippingCoefficient() * sceneRadius()} in front of (resp. behind) the
   * {@link #sceneCenter()}. This guarantees an optimal use of the z-buffer range and
   * minimizes aliasing. See the {@link #zNear()} and {@link #zFar()} documentations.
   * <p>
   * Default value is square root of 3.0 (so that a cube of size 2*{@link #sceneRadius()}
   * is not clipped).
   * <p>
   * However, since the {@link #sceneRadius()} is used for other purposes (see
   * showEntireScene(), flySpeed(), ...) and you may want to change this value to define
   * more precisely the location of the clipping planes. See also
   * {@link #zNearCoefficient()}.
   */
  public float zClippingCoefficient() {
    return zClippingCoef;
  }

  /**
   * Sets the {@link #zClippingCoefficient()} value.
   */
  public void setZClippingCoefficient(float coef) {
    if (coef != zClippingCoef)
      modified();
    zClippingCoef = coef;
  }

  @Override
  public float sceneToPixelRatio(Vec position) {
    switch (type()) {
      case PERSPECTIVE:
        return 2.0f * Math.abs((frame().coordinatesOf(position)).vec[2] * frame().magnitude()) * (float) Math
            .tan(fieldOfView() / 2.0f) / screenHeight();
      case ORTHOGRAPHIC:
        float[] wh = getBoundaryWidthHeight();
        return 2.0f * wh[1] / screenHeight();
    }
    return 1.0f;
  }

  @Override
  public boolean isPointVisible(Vec point) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by pointIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    for (int i = 0; i < 6; ++i)
      if (distanceToBoundary(i, point) > 0)
        return false;
    return true;
  }

  /**
   * Same as {@code return isPointVisible(new Vec(x, y, z))}.
   *
   * @see #isPointVisible(Vec)
   */
  public boolean isPointVisible(float x, float y, float z) {
    return isPointVisible(new Vec(x, y, z));
  }

  @Override
  public Visibility ballVisibility(Vec center, float radius) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by sphereIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      float d = distanceToBoundary(i, center);
      if (d > radius)
        return Camera.Visibility.INVISIBLE;
      if ((d > 0) || (-d < radius))
        allInForAllPlanes = false;
    }
    if (allInForAllPlanes)
      return Camera.Visibility.VISIBLE;
    return Camera.Visibility.SEMIVISIBLE;
  }

  @Override
  public Visibility boxVisibility(Vec p1, Vec p2) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by aaBoxIsVisible) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
      boolean allOut = true;
      for (int c = 0; c < 8; ++c) {
        Vec pos = new Vec(((c & 4) != 0) ? p1.vec[0] : p2.vec[0], ((c & 2) != 0) ? p1.vec[1] : p2.vec[1],
            ((c & 1) != 0) ? p1.vec[2] : p2.vec[2]);
        if (distanceToBoundary(i, pos) > 0.0)
          allInForAllPlanes = false;
        else
          allOut = false;
      }
      // The eight points are on the outside side of this plane
      if (allOut)
        return Camera.Visibility.INVISIBLE;
    }

    if (allInForAllPlanes)
      return Camera.Visibility.VISIBLE;

    // Too conservative, but tangent cases are too expensive to detect
    return Camera.Visibility.SEMIVISIBLE;
  }

  @Override
  public float[][] computeBoundaryEquations() {
    return computeBoundaryEquations(new float[6][4]);
  }

  @Override
  public float[][] computeBoundaryEquations(float[][] coef) {
    // soft check:
    if (coef == null || (coef.length == 0))
      coef = new float[6][4];
    else if ((coef.length != 6) || (coef[0].length != 4))
      coef = new float[6][4];

    // Computed once and for all
    Vec pos = position();
    Vec viewDir = viewDirection();
    Vec up = upVector();
    Vec right = rightVector();

    float posViewDir = Vec.dot(pos, viewDir);

    switch (type()) {
      case PERSPECTIVE: {
        float hhfov = horizontalFieldOfView() / 2.0f;
        float chhfov = (float) Math.cos(hhfov);
        float shhfov = (float) Math.sin(hhfov);
        normal[0] = Vec.multiply(viewDir, -shhfov);
        normal[1] = Vec.add(normal[0], Vec.multiply(right, chhfov));
        normal[0] = Vec.add(normal[0], Vec.multiply(right, -chhfov));
        normal[2] = Vec.multiply(viewDir, -1);
        normal[3] = viewDir;

        float hfov = fieldOfView() / 2.0f;
        float chfov = (float) Math.cos(hfov);
        float shfov = (float) Math.sin(hfov);
        normal[4] = Vec.multiply(viewDir, -shfov);
        normal[5] = Vec.add(normal[4], Vec.multiply(up, -chfov));
        normal[4] = Vec.add(normal[4], Vec.multiply(up, chfov));

        for (int i = 0; i < 2; ++i)
          dist[i] = Vec.dot(pos, normal[i]);
        for (int j = 4; j < 6; ++j)
          dist[j] = Vec.dot(pos, normal[j]);

        // Natural equations are:
        // dist[0,1,4,5] = pos * normal[0,1,4,5];
        // dist[2] = (pos + zNear() * viewDir) * normal[2];
        // dist[3] = (pos + zFar() * viewDir) * normal[3];

        // 2 times less computations using expanded/merged equations. Dir vectors
        // are normalized.
        float posRightCosHH = chhfov * Vec.dot(pos, right);
        dist[0] = -shhfov * posViewDir;
        dist[1] = dist[0] + posRightCosHH;
        dist[0] = dist[0] - posRightCosHH;
        float posUpCosH = chfov * Vec.dot(pos, up);
        dist[4] = -shfov * posViewDir;
        dist[5] = dist[4] - posUpCosH;
        dist[4] = dist[4] + posUpCosH;
        break;
      }
      case ORTHOGRAPHIC:
        normal[0] = Vec.multiply(right, -1);
        normal[1] = right;
        normal[4] = up;
        normal[5] = Vec.multiply(up, -1);

        float[] wh = getBoundaryWidthHeight();
        dist[0] = Vec.dot(Vec.subtract(pos, Vec.multiply(right, wh[0])), normal[0]);
        dist[1] = Vec.dot(Vec.add(pos, Vec.multiply(right, wh[0])), normal[1]);
        dist[4] = Vec.dot(Vec.add(pos, Vec.multiply(up, wh[1])), normal[4]);
        dist[5] = Vec.dot(Vec.subtract(pos, Vec.multiply(up, wh[1])), normal[5]);
        break;
    }

    // Front and far planes are identical for both camera types.
    normal[2] = Vec.multiply(viewDir, -1);
    normal[3] = viewDir;
    dist[2] = -posViewDir - zNear();
    dist[3] = posViewDir + zFar();

    for (int i = 0; i < 6; ++i) {
      coef[i][0] = normal[i].vec[0];
      coef[i][1] = normal[i].vec[1];
      coef[i][2] = normal[i].vec[2];
      coef[i][3] = dist[i];
    }

    return coef;
  }

  /**
   * Same as {@code return !isFaceBackFacing(a, b, c)}.
   *
   * @see #isFaceBackFacing(Vec, Vec, Vec)
   */
  public boolean isFaceFrontFacing(Vec a, Vec b, Vec c) {
    return !isFaceBackFacing(a, b, c);
  }

  /**
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   * <p>
   * Vertices must given in clockwise order if
   * {@link remixlab.dandelion.core.AbstractScene#isLeftHanded()} or in counter-clockwise
   * order if {@link remixlab.dandelion.core.AbstractScene#isRightHanded()}.
   *
   * @param a first face vertex
   * @param b second face vertex
   * @param c third face vertex
   * @see #isFaceBackFacing(Vec, Vec)
   * @see #isConeBackFacing(Vec, Vec, float)
   */
  public boolean isFaceBackFacing(Vec a, Vec b, Vec c) {
    return isFaceBackFacing(a, gScene.isLeftHanded() ?
        Vec.subtract(b, a).cross(Vec.subtract(c, a)) :
        Vec.subtract(c, a).cross(Vec.subtract(b, a)));
  }

  /**
   * Same as {@code return !isFaceBackFacing(vertex, normal)}.
   *
   * @see #isFaceBackFacing(Vec, Vec)
   */
  public boolean isFaceFrontFacing(Vec vertex, Vec normal) {
    return !isFaceBackFacing(vertex, normal);
  }

  /**
   * Returns {@code true} if the given face is back-facing the camera. Otherwise returns
   * {@code false}.
   *
   * @param vertex belonging to the face
   * @param normal face normal
   * @see #isFaceBackFacing(Vec, Vec, Vec)
   * @see #isConeBackFacing(Vec, Vec, float)
   */
  public boolean isFaceBackFacing(Vec vertex, Vec normal) {
    return isConeBackFacing(vertex, normal, 0);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vec, ArrayList)
   */
  public boolean isConeFrontFacing(Vec vertex, ArrayList<Vec> normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals ArrayList of normals defining the cone.
   * @see #isConeBackFacing(Vec, Vec[])
   * @see #isConeBackFacing(Vec, Vec, float)
   */
  public boolean isConeBackFacing(Vec vertex, ArrayList<Vec> normals) {
    return isConeBackFacing(vertex, normals.toArray(new Vec[normals.size()]));
  }

  /**
   * Same as {@code !isConeBackFacing(vertex, normals)}.
   *
   * @see #isConeBackFacing(Vec, Vec[])
   */
  public boolean isConeFrontFacing(Vec vertex, Vec[] normals) {
    return !isConeBackFacing(vertex, normals);
  }

  /**
   * Returns {@code true} if the given cone is back-facing the camera and {@code false}
   * otherwise.
   *
   * @param vertex  Cone vertex
   * @param normals Array of normals defining the cone.
   * @see #isConeBackFacing(Vec, ArrayList)
   * @see #isConeBackFacing(Vec, Vec, float)
   */
  public boolean isConeBackFacing(Vec vertex, Vec[] normals) {
    float angle;
    Vec axis = new Vec(0, 0, 0);

    if (normals.length == 0)
      throw new RuntimeException("Normal array provided is empty");

    Vec[] n = new Vec[normals.length];
    for (int i = 0; i < normals.length; i++) {
      n[i] = new Vec();
      n[i].set(normals[i]);
      n[i].normalize();
      axis = Vec.add(axis, n[i]);
    }

    if (Util.nonZero(axis.magnitude()))
      axis.normalize();
    else
      axis.set(0, 0, 1);

    angle = 0;
    for (int i = 0; i < normals.length; i++)
      angle = Math.max(angle, (float) Math.acos(Vec.dot(n[i], axis)));

    return isConeBackFacing(vertex, axis, angle);
  }

  /**
   * Same as {@code return !isConeBackFacing(vertex, axis, angle)}.
   *
   * @see #isConeBackFacing(Vec, Vec, float)
   */
  public boolean isConeFrontFacing(Vec vertex, Vec axis, float angle) {
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
  public boolean isConeBackFacing(Vec vertex, Vec axis, float angle) {
    // more or less inspired by this:
    // http://en.wikipedia.org/wiki/Back-face_culling (perspective case :P)
    Vec camAxis;
    if (type() == Type.ORTHOGRAPHIC)
      camAxis = viewDirection();
    else {
      camAxis = Vec.subtract(vertex, position());
      if (angle != 0)
        camAxis.normalize();
    }
    if (angle == 0)
      return Vec.dot(camAxis, axis) >= 0;
    float absAngle = Math.abs(angle);
    if (absAngle >= Math.PI / 2)
      return true;
    Vec faceNormal = axis.get();
    faceNormal.normalize();
    return Math.acos(Vec.dot(camAxis, faceNormal)) + absAngle < Math.PI / 2;
  }

  // 4. SCENE RADIUS AND CENTER

  @Override
  public void setSceneRadius(float radius) {
    super.setSceneRadius(radius);
    setFocusDistance(sceneRadius() / (float) Math.tan(fieldOfView() / 2.0f));
  }

  @Override
  public float distanceToSceneCenter() {
    Vec zCam = frame().zAxis();
    Vec cam2SceneCenter = Vec.subtract(position(), sceneCenter());
    return Math.abs(Vec.dot(cam2SceneCenter, zCam));
  }

  @Override
  public float distanceToAnchor() {
    Vec zCam = frame().zAxis();
    Vec cam2anchor = Vec.subtract(position(), anchor());
    return Math.abs(Vec.dot(cam2anchor, zCam));
  }

  @Override
  public void setSceneBoundingBox(Vec min, Vec max) {
    setSceneCenter(Vec.multiply(Vec.add(min, max), 1 / 2.0f));
    setSceneRadius(0.5f * (Vec.subtract(max, min)).magnitude());
  }

  // 5. ANCHOR REFERENCE POINT

  @Override
  public boolean setAnchorFromPixel(Point pixel) {
    Vec pup = pointUnderPixel(pixel);
    if (pup != null) {
      setAnchor(pup);
      // new animation
      anchorFlag = true;
      timerFx.runOnce(1000);
      return true;
    }
    return false;
  }

  @Override
  public boolean setSceneCenterFromPixel(Point pixel) {
    Vec pup = pointUnderPixel(pixel);
    if (pup != null) {
      setSceneCenter(pup);
      return true;
    }
    return false;
  }

  /**
   * Returns the coordinates of the 3D point located at {@code pixel} (x,y) on screen. May
   * be null if no point is found under pixel.
   * <p>
   * Override this method in your jogl-based camera class.
   * <p>
   * Current implementation always returns {@code WorlPoint.found = false} (dummy value),
   * meaning that no point was found under pixel.
   */
  public Vec pointUnderPixel(Point pixel) {
    return gScene.pointUnderPixel(pixel);
  }

  // 8. MATRICES

  @Override
  public void computeView() {
    Quat q = (Quat) frame().orientation();

    float q00 = 2.0f * q.quat[0] * q.quat[0];
    float q11 = 2.0f * q.quat[1] * q.quat[1];
    float q22 = 2.0f * q.quat[2] * q.quat[2];

    float q01 = 2.0f * q.quat[0] * q.quat[1];
    float q02 = 2.0f * q.quat[0] * q.quat[2];
    float q03 = 2.0f * q.quat[0] * q.quat[3];

    float q12 = 2.0f * q.quat[1] * q.quat[2];
    float q13 = 2.0f * q.quat[1] * q.quat[3];
    float q23 = 2.0f * q.quat[2] * q.quat[3];

    viewMat.mat[0] = 1.0f - q11 - q22;
    viewMat.mat[1] = q01 - q23;
    viewMat.mat[2] = q02 + q13;
    viewMat.mat[3] = 0.0f;

    viewMat.mat[4] = q01 + q23;
    viewMat.mat[5] = 1.0f - q22 - q00;
    viewMat.mat[6] = q12 - q03;
    viewMat.mat[7] = 0.0f;

    viewMat.mat[8] = q02 - q13;
    viewMat.mat[9] = q12 + q03;
    viewMat.mat[10] = 1.0f - q11 - q00;
    viewMat.mat[11] = 0.0f;

    Vec t = q.inverseRotate(frame().position());

    viewMat.mat[12] = -t.vec[0];
    viewMat.mat[13] = -t.vec[1];
    viewMat.mat[14] = -t.vec[2];
    viewMat.mat[15] = 1.0f;
  }

  /**
   * Returns a value proportional to the Camera (z projected) distance to the
   * {@link #anchor()} so that when zooming on the object, the ortho Camera is translated
   * forward and its boundary is narrowed, making the object appear bigger on screen, as
   * intuitively expected.
   * <p>
   * Value is computed as: {@code 2 * distanceToAnchor() / screenHeight()}.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  @Override
  public float rescalingOrthoFactor() {
    float toAnchor = this.distanceToAnchor();
    return (2 * (Util.zero(toAnchor) ? Util.FLOAT_EPS : toAnchor) * rapK / screenHeight());
  }

  @Override
  public void setAnchor(Vec rap) {
    float prevDist = distanceToAnchor();
    this.anchorPnt = rap;
    float newDist = distanceToAnchor();
    if ((Util.nonZero(prevDist)) && (Util.nonZero(newDist)))
      rapK *= prevDist / newDist;
  }

  /**
   * Same as {@code setAnchor(new Vec(x,y,z))}.
   *
   * @see AbstractScene#setAnchor(Vec)
   */
  public void setAnchor(float x, float y, float z) {
    setAnchor(new Vec(x, y, z));
  }

  @Override
  public void computeProjection() {
    float ZNear = zNear();
    float ZFar = zFar();

    switch (type()) {
      case PERSPECTIVE:
        // #CONNECTION# all non null coefficients were set to 0.0 in constructor.
        projectionMat.mat[0] = 1 / (frame().magnitude() * this.aspectRatio());
        projectionMat.mat[5] = 1 / (gScene.isLeftHanded() ? -frame().magnitude() : frame().magnitude());
        projectionMat.mat[10] = (ZNear + ZFar) / (ZNear - ZFar);
        projectionMat.mat[11] = -1.0f;
        projectionMat.mat[14] = 2.0f * ZNear * ZFar / (ZNear - ZFar);
        projectionMat.mat[15] = 0.0f;
        // same as gluPerspective( 180.0*fieldOfView()/M_PI, aspectRatio(),
        // zNear(), zFar() );
        break;
      case ORTHOGRAPHIC:
        float[] wh = getBoundaryWidthHeight();
        projectionMat.mat[0] = 1.0f / wh[0];
        projectionMat.mat[5] = (gScene.isLeftHanded() ? -1.0f : 1.0f) / wh[1];
        projectionMat.mat[10] = -2.0f / (ZFar - ZNear);
        projectionMat.mat[11] = 0.0f;
        projectionMat.mat[14] = -(ZFar + ZNear) / (ZFar - ZNear);
        projectionMat.mat[15] = 1.0f;
        // same as glOrtho( -w, w, -h, h, zNear(), zFar() );
        break;
    }
  }

  @Override
  public void fromView(Mat mv, boolean recompute) {
    Quat q = new Quat();
    q.fromMatrix(mv);
    setOrientation(q);
    setPosition(Vec.multiply(q.rotate(new Vec(mv.mat[12], mv.mat[13], mv.mat[14])), -1));
    if (recompute)
      this.computeView();
  }

  // 9. WORLD -> CAMERA

  // 10. 2D -> 3D

  /**
   * Gives the coefficients of a 3D half-line passing through the Camera eye and pixel
   * (x,y). Origin in the upper left corner. Use {@link #screenHeight()} - y to locate the
   * origin at the lower left corner.
   * <p>
   * The origin of the half line (eye position) is stored in {@code orig}, while
   * {@code dir} contains the properly oriented and normalized direction of the half line.
   * <p>
   * This method is useful for analytical intersection in a selection method.
   */
  public void convertClickToLine(final Point pixelInput, Vec orig, Vec dir) {
    Point pixel = new Point(pixelInput.x(), pixelInput.y());

    // lef-handed coordinate system correction
    if (gScene.isLeftHanded())
      pixel.setY(screenHeight() - pixelInput.y());

    switch (type()) {
      case PERSPECTIVE:
        orig.set(position());
        dir.set(new Vec(((2.0f * pixel.x() / screenWidth()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f) * aspectRatio(),
            ((2.0f * (screenHeight() - pixel.y()) / screenHeight()) - 1.0f) * (float) Math.tan(fieldOfView() / 2.0f),
            -1.0f));
        dir.set(Vec.subtract(frame().inverseCoordinatesOf(dir), orig));
        dir.normalize();
        break;

      case ORTHOGRAPHIC: {
        float[] wh = getBoundaryWidthHeight();
        orig.set(
            new Vec((2.0f * pixel.x() / screenWidth() - 1.0f) * wh[0], -(2.0f * pixel.y() / screenHeight() - 1.0f) * wh[1],
                0.0f));
        orig.set(frame().inverseCoordinatesOf(orig));
        dir.set(viewDirection());
        break;
      }
    }
  }

  // 12. POSITION TOOLS

  @Override
  public void lookAt(Vec target) {
    setViewDirection(Vec.subtract(target, position()));
  }

  /**
   * Same as {@code lookAt(new Vec(x,y,z))}.
   *
   * @see #lookAt(Vec)
   */
  public void lookAt(float x, float y, float z) {
    lookAt(new Vec(x, y, z));
  }

  @Override
  public Vec at() {
    return Vec.add(position(), viewDirection());
  }

  @Override
  public void fitBall(Vec center, float radius) {
    float distance = 0.0f;
    switch (type()) {
      case PERSPECTIVE: {
        float yview = radius / (float) Math.sin(fieldOfView() / 2.0f);
        float xview = radius / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distance = Math.max(xview, yview);
        break;
      }
      case ORTHOGRAPHIC: {
        distance = Vec.dot(Vec.subtract(center, anchor()), viewDirection()) + (radius / frame().magnitude());
        break;
      }
    }

    Vec newPos = Vec.subtract(center, Vec.multiply(viewDirection(), distance));
    frame().setPositionWithConstraint(newPos);
  }

  @Override
  public void fitBoundingBox(Vec min, Vec max) {
    float diameter = Math.max(Math.abs(max.vec[1] - min.vec[1]), Math.abs(max.vec[0] - min.vec[0]));
    diameter = Math.max(Math.abs(max.vec[2] - min.vec[2]), diameter);
    fitBall(Vec.multiply(Vec.add(min, max), 0.5f), 0.5f * diameter);
  }

  @Override
  public void fitScreenRegion(Rect rectangle) {
    Vec vd = viewDirection();
    float distToPlane = distanceToSceneCenter();

    Point center = new Point((int) rectangle.centerX(), (int) rectangle.centerY());

    Vec orig = new Vec();
    Vec dir = new Vec();
    convertClickToLine(center, orig, dir);
    Vec newCenter = Vec.add(orig, Vec.multiply(dir, (distToPlane / Vec.dot(dir, vd))));

    convertClickToLine(new Point(rectangle.x(), center.y()), orig, dir);
    final Vec pointX = Vec.add(orig, Vec.multiply(dir, (distToPlane / Vec.dot(dir, vd))));

    convertClickToLine(new Point(center.x(), rectangle.y()), orig, dir);
    final Vec pointY = Vec.add(orig, Vec.multiply(dir, (distToPlane / Vec.dot(dir, vd))));

    float distance = 0.0f;
    float distX, distY;
    switch (type()) {
      case PERSPECTIVE:
        distX = Vec.distance(pointX, newCenter) / (float) Math.sin(horizontalFieldOfView() / 2.0f);
        distY = Vec.distance(pointY, newCenter) / (float) Math.sin(fieldOfView() / 2.0f);
        distance = Math.max(distX, distY);
        break;
      case ORTHOGRAPHIC:
        float dist = Vec.dot(Vec.subtract(newCenter, anchor()), vd);
        distX = Vec.distance(pointX, newCenter) / frame().magnitude() / aspectRatio();
        distY = Vec.distance(pointY, newCenter) / frame().magnitude() / 1.0f;
        distance = dist + Math.max(distX, distY);
        break;
    }

    frame().setPositionWithConstraint(Vec.subtract(newCenter, Vec.multiply(vd, distance)));
  }

  @Override
  public void interpolateToZoomOnPixel(Point pixel) {
    Vec target = pointUnderPixel(pixel);

    if (target == null) {
      System.out.println("No object under pixel was found");
      // return target;
      return;
    }

    interpolateToZoomOnTarget(target);

    // draw hint
    pupVec = target;
    pupFlag = true;
    timerFx.runOnce(1000);
  }

  protected void interpolateToZoomOnTarget(Vec target) {
    if (target == null)
      return;

    float coef = 0.1f;

    if (anyInterpolationStarted())
      stopInterpolations();

    interpolationKfi.deletePath();
    interpolationKfi.addKeyFrame(frame().detach());

    GenericFrame frame = new GenericFrame(gScene,
        Vec.add(Vec.multiply(frame().position(), 0.3f), Vec.multiply(target, 0.7f)), frame().orientation(),
        frame().magnitude());
    scene().pruneBranch(frame);
    interpolationKfi.addKeyFrame(frame, 0.4f);
    // interpolationKfi.addKeyFrame(new InteractiveFrame(gScene,
    // Vec.add(Vec.multiply(frame().position(), 0.3f), Vec.multiply(target,
    // 0.7f)), frame().orientation(), frame().magnitude()).detach(), 0.4f);

    GenericFrame originalFrame = frame();
    GenericFrame tempFrame = frame().detach();
    tempFrame.setPosition(Vec.add(Vec.multiply(frame().position(), coef), Vec.multiply(target, (1.0f - coef))));
    replaceFrame(tempFrame);
    lookAt(target);
    setFrame(originalFrame);

    interpolationKfi.addKeyFrame(tempFrame, 1.0f);
    interpolationKfi.startInterpolation();
  }

  // 13. STEREO PARAMETERS

  /**
   * Returns the user's inter-ocular distance (in meters). Default value is 0.062m, which
   * fits most people.
   *
   * @see #setIODistance(float)
   */
  public float IODistance() {
    return IODist;
  }

  /**
   * Sets the {@link #IODistance()}.
   */
  public void setIODistance(float distance) {
    IODist = distance;
  }

  /**
   * Returns the physical distance between the user's eyes and the screen (in meters).
   * <p>
   * Default value is 0.5m.
   * <p>
   * Value is set using {@link #setPhysicalDistanceToScreen(float)}.
   * <p>
   * physicalDistanceToScreen() and {@link #focusDistance()} represent the same distance.
   * The first one is expressed in physical real world units, while the latter is
   * expressed in virtual world units. Use their ratio to convert distances between these
   * worlds.
   */
  public float physicalDistanceToScreen() {
    return physicalDist2Scrn;
  }

  /**
   * Sets the {@link #physicalDistanceToScreen()}.
   */
  public void setPhysicalDistanceToScreen(float distance) {
    physicalDist2Scrn = distance;
  }

  /**
   * Returns the physical screen width, in meters. Default value is 0.4m (average
   * monitor).
   * <p>
   * Used for stereo display only. Set using {@link #setPhysicalScreenWidth(float)}.
   * <p>
   * See {@link #physicalDistanceToScreen()} for reality center automatic configuration.
   */
  public float physicalScreenWidth() {
    return physicalScrnWidth;
  }

  /**
   * Sets the physical screen (monitor or projected wall) width (in meters).
   */
  public void setPhysicalScreenWidth(float width) {
    physicalScrnWidth = width;
  }

  /**
   * Returns the focus distance used by stereo display, expressed in virtual world units.
   * <p>
   * This is the distance in the virtual world between the Camera and the plane where the
   * horizontal stereo parallax is null (the stereo left and right images are
   * superimposed).
   * <p>
   * This distance is the virtual world equivalent of the real-world
   * {@link #physicalDistanceToScreen()}.
   * <p>
   * <b>attention:</b> This value is modified by Scene.setSceneRadius(), setSceneRadius()
   * and {@link #setFieldOfView(float)}. When one of these values is modified,
   * {@link #focusDistance()} is set to {@link #sceneRadius()} / tan(
   * {@link #fieldOfView()}/2), which provides good results.
   */
  public float focusDistance() {
    return focusDist;
  }

  /**
   * Sets the focusDistance(), in virtual scene units.
   */
  public void setFocusDistance(float distance) {
    if (distance != focusDist)
      modified();
    focusDist = distance;
  }
}
