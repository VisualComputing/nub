/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.geom;

import remixlab.bias.Agent;
import remixlab.bias.Event;
import remixlab.bias.Grabber;
import remixlab.bias.event.TapEvent;
import remixlab.bias.event.KeyEvent;
import remixlab.bias.event.MotionEvent;
import remixlab.fpstiming.TimingTask;
import remixlab.primitives.*;

import java.util.*;

/**
 * A 2D /3D eye.
 * <p>
 * An Eye defines some intrinsic parameters ({@link #position()}, {@link #viewDirection()}
 * , {@link #upVector()}...) and useful positioning tools that ease its placement (
 * {@link #showEntireScene()}, {@link #fitBall(Vec, float)}, {@link #lookAt(Vec)}...). It
 * exports its associated projection and view matrices and it can interactively be
 * modified using any interaction mechanism you can think of.
 * <p>
 * An Eye provides visibility routines (
 * {@link #isPointVisible(Vec)}, {@link #ballVisibility(Vec, float)},
 * {@link #boxVisibility(Vec, Vec)}), from which advanced geometry culling techniques can
 * be implemented.
 * <p>
 * The {@link #position()} and {@link #orientation()} of the Eye are defined by an
 * {@link InteractiveFrame} (retrieved using {@link #frame()}). These
 * methods are just convenient wrappers to the equivalent Frame methods. This also means
 * that the Eye {@link #frame()} can be attached to a
 * {@link Frame#referenceFrame()} which enables complex Eye
 * setups. An Eye hasGrabber its own magnitude, different from that of the scene (i.e.,
 * {@link Frame#magnitude()} doesn't necessarily equals {@code 1}
 * ), which allows to scale the view. Use {@link #eyeCoordinatesOf(Vec)} and
 * {@link #worldCoordinatesOf(Vec)} (or any of the powerful Frame transformations, such as
 * {@link Frame#coordinatesOf(Vec)},
 * {@link Frame#transformOf(Vec)}, ...) to convert to and from the
 * Eye {@link #frame()} coordinate system. {@link #projectedCoordinatesOf(Vec)} and
 * {@link #unprojectedCoordinatesOf(Vec)} will convert from screen to 3D coordinates.
 * <p>
 * An Eye can also be used outside of an Scene for its coordinate system conversion
 * capabilities.
 * <p>
 * {@link #sceneCenter()} is set to (0,0,0) and {@link #sceneRadius()} is set to 100.
 * {@link #type()} Camera.PERSPECTIVE, with a {@code PI/3} {@link #fieldOfView()} (same
 * value used in P5 by default).
 * <p>
 * Camera matrices (projection and view) are created and computed according to remaining
 * default Camera parameters.
 */
public class Eye {
  class GrabberEyeFrame extends InteractiveFrame {
    public final int LEFT_ID = 37, CENTER_ID = 3, RIGHT_ID = 39, WHEEL_ID = 8, NO_BUTTON = Event.NO_ID, // 1.b. Keys
        LEFT_KEY = 37, RIGHT_KEY = 39, UP_KEY = 38, DOWN_KEY = 40;

    public GrabberEyeFrame(AbstractScene _scene) {
      super(_scene);
    }

    public GrabberEyeFrame(Eye _eye) {
      super(_eye);
    }

    protected GrabberEyeFrame(GrabberEyeFrame otherFrame) {
      super(otherFrame);
    }

    @Override
    public GrabberEyeFrame get() {
      return new GrabberEyeFrame(this);
    }

    @Override
    protected GrabberEyeFrame detach() {
      GrabberEyeFrame frame = new GrabberEyeFrame(scene());
      scene().pruneBranch(frame);
      frame.setWorldMatrix(this);
      return frame;
    }

    @Override
    public void interact(MotionEvent event) {
      switch (event.shortcut().id()) {
        case LEFT_ID:
          rotate(event);
          break;
        case CENTER_ID:
          screenRotate(event);
          break;
        case RIGHT_ID:
          translate(event);
          break;
        case WHEEL_ID:
          if (scene().is3D() && isEyeFrame())
            translateZ(event);
          else
            scale(event);
          break;
      }
    }

    @Override
    public void interact(TapEvent event) {
      if (event.count() == 2) {
        if (event.id() == LEFT_ID)
          center();
        if (event.id() == RIGHT_ID)
          align();
      }
    }

    @Override
    public void interact(KeyEvent event) {
      if (event.isShiftDown()) {
        if (event.id() == UP_KEY)
          translateY(true);
        if (event.id() == DOWN_KEY)
          translateY(false);
        if (event.id() == LEFT_KEY)
          translateX(false);
        if (event.id() == RIGHT_KEY)
          translateX(true);
      } else {
        if (event.id() == UP_KEY)
          if (gScene.is3D())
            rotateX(true);
        if (event.id() == DOWN_KEY)
          if (gScene.is3D())
            rotateY(false);
        if (event.id() == LEFT_KEY)
          rotateZ(false);
        if (event.id() == RIGHT_KEY)
          rotateZ(true);
      }
    }
  }

  /**
   * Enumerates the different visibility states an object may have respect to the Eye
   * boundary.
   */
  public enum Visibility {
    VISIBLE, SEMIVISIBLE, INVISIBLE
  }

  ;

  // F r a m e
  protected InteractiveFrame gFrame;

  // S C E N E O B J E C T
  protected AbstractScene gScene;

  // C a m e r a p a r a m e t e r s
  protected int scrnWidth, scrnHeight; // size of the window,
  // in pixels
  // protected float orthoSize;
  protected Vec scnCenter;
  protected float scnRadius; // processing scene
  // units
  protected int viewport[] = new int[4];

  protected Mat viewMat;
  protected Mat projectionMat;

  // F r u s t u m p l a n e c o e f f i c i e n t s
  protected float fpCoefficients[][];
  protected boolean fpCoefficientsUpdate;

  protected Vec normal[];
  protected float dist[];

  /**
   * Which was the last frame the Eye changes.
   */
  public long lastNonFrameUpdate = 0;
  protected long lastFPCoeficientsUpdateIssued = -1;

  protected Vec anchorPnt;

  // L O C A L T I M E R
  //TODO restore
  /*
  public boolean anchorFlag;
  public boolean pupFlag;
  public Vec pupVec;
  protected TimingTask timerFx;
  */

  public Eye(AbstractScene scn) {
    gScene = scn;

    if (gScene.is2D()) {
      fpCoefficients = new float[4][3];
      normal = new Vec[4];
      for (int i = 0; i < normal.length; i++)
        normal[i] = new Vec();
      dist = new float[4];
    } else {
      fpCoefficients = new float[6][4];
      normal = new Vec[6];
      for (int i = 0; i < normal.length; i++)
        normal[i] = new Vec();
      dist = new float[6];
    }

    enableBoundaryEquations(false);
    anchorPnt = new Vec(0.0f, 0.0f, 0.0f);

    //TODO restore
    /*
    this.timerFx = new TimingTask() {
      public void execute() {
        unSetTimerFlag();
      }
    };
    this.gScene.registerTimingTask(timerFx);
    */

    setFrame(new GrabberEyeFrame(this));
    setSceneRadius(100);
    setSceneCenter(new Vec(0.0f, 0.0f, 0.0f));
    setScreenWidthAndHeight(gScene.width(), gScene.height());

    viewMat = new Mat();
    projectionMat = new Mat();
    projectionMat.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    if(gScene.is3D()) {
      //TODO only 3D
      setType(Type.PERSPECTIVE);
      setZNearCoefficient(0.005f);
      setZClippingCoefficient((float) Math.sqrt(3.0f));

      // TODO Stereo parameters
      //setIODistance(0.062f);
      //setPhysicalDistanceToScreen(0.5f);
      //setPhysicalScreenWidth(0.4f);
      // focusDistance is set from setFieldOfView()

      computeProjection();
    }
  }

  protected Eye(Eye oVP) {
    this.gScene = oVP.gScene;
    this.fpCoefficientsUpdate = oVP.fpCoefficientsUpdate;

    this.anchorPnt = new Vec();
    this.anchorPnt.set(oVP.anchorPnt);

    if (gScene.is2D()) {
      this.fpCoefficients = new float[4][3];
      for (int i = 0; i < 4; i++)
        for (int j = 0; j < 3; j++)
          this.fpCoefficients[i][j] = oVP.fpCoefficients[i][j];
      this.normal = new Vec[4];
      for (int i = 0; i < normal.length; i++)
        this.normal[i] = new Vec(oVP.normal[i].vec[0], oVP.normal[i].vec[1], oVP.normal[i].vec[2]);
      this.dist = new float[4];
      for (int i = 0; i < dist.length; i++)
        this.dist[i] = oVP.dist[i];
    } else {
      this.fpCoefficients = new float[6][4];
      for (int i = 0; i < 6; i++)
        for (int j = 0; j < 4; j++)
          this.fpCoefficients[i][j] = oVP.fpCoefficients[i][j];
      this.normal = new Vec[6];
      for (int i = 0; i < normal.length; i++)
        this.normal[i] = new Vec(oVP.normal[i].vec[0], oVP.normal[i].vec[1], oVP.normal[i].vec[2]);
      this.dist = new float[6];
      for (int i = 0; i < dist.length; i++)
        this.dist[i] = oVP.dist[i];
    }

    //TODO restore
    /*
    this.timerFx = new TimingTask() {
      public void execute() {
        unSetTimerFlag();
      }
    };
    this.gScene.registerTimingTask(timerFx);
    */
    this.gFrame = oVP.frame().get();
    this.setSceneRadius(oVP.sceneRadius());
    this.setSceneCenter(oVP.sceneCenter());
    this.setScreenWidthAndHeight(oVP.screenWidth(), oVP.screenHeight());
    this.viewMat = oVP.viewMat.get();
    this.projectionMat = oVP.projectionMat.get();

    //TODO only 3D
    this.setType(oVP.type());
    this.setZNearCoefficient(oVP.zNearCoefficient());
    this.setZClippingCoefficient(oVP.zClippingCoefficient());
    //this.setIODistance(oVP.IODistance());
    //this.setPhysicalDistanceToScreen(oVP.physicalDistanceToScreen());
    //this.setPhysicalScreenWidth(oVP.physicalScreenWidth());
    this.rapK = oVP.rapK;
  }

  public Eye get() {
    return new Eye(this);
  }

  /**
   * Returns the GrabberFrame attached to the Eye.
   * <p>
   * This GrabberFrame defines its {@link #position()}, {@link #orientation()} and can
   * translate bogus events into Eye displacement. Set using
   * {@link #setFrame(InteractiveFrame)}.
   */
  public InteractiveFrame frame() {
    return gFrame;
  }

  /**
   * Returns the scene this object belongs to
   */
  public AbstractScene scene() {
    return gScene;
  }

  // 2. Local timer

  /**
   * Returns the Eye orientation, defined in the world coordinate system.
   * <p>
   * Actually returns {@code frame().orientation()}. Use {@link #setOrientation(Quat)}
   * , {@link #setUpVector(Vec)} or {@link #lookAt(Vec)} to set the Eye orientation.
   */
  public Quat orientation() {
    return (Quat)frame().orientation();
  }

  protected void modified() {
    lastNonFrameUpdate = AbstractScene.frameCount;
  }

  /**
   * Max between {@link InteractiveFrame#lastUpdate()} and
   * {@link #lastNonFrameUpdate()}.
   *
   * @return last frame the Eye was updated
   * @see #lastNonFrameUpdate()
   */
  public long lastUpdate() {
    return Math.max(frame().lastUpdate(), lastNonFrameUpdate());
  }

  /**
   * @return last frame a local Eye parameter (different than the Frame) was updated.
   * @see #lastUpdate()
   */
  public long lastNonFrameUpdate() {
    return lastNonFrameUpdate;
  }

  // 2. POSITION AND ORIENTATION

  /**
   * Same as {@code scene.flip()}.
   *
   * @see AbstractScene#flip()
   */
  public void flip() {
    gScene.flip();
  }

  /**
   * Internal use. Temporarily attach a frame to the Eye which is useful to some
   * interpolation methods.
   */
  protected final void replaceFrame(InteractiveFrame g) {
    if (g == null || g == frame())
      return;
    if (g.theeye == null) {// only detached frames which call pruneBranch on g
      gFrame = g;
      frame().theeye = this;
    }
  }

  /**
   * Sets the Eye {@link #frame()}.
   * <p>
   * If you want to move the Eye, use {@link #setPosition(Vec)} and
   * {@link #setOrientation(Quat)} or one of the Eye positioning methods (
   * {@link #lookAt(Vec)}, {@link #fitBall(Vec, float)}, {@link #showEntireScene()}...)
   * instead.
   * <p>
   * This method is actually mainly useful if you derive the GrabberFrame class and want
   * to use an instance of your new class to move the Eye.
   * <p>
   * A {@code null} {@code icf} reference will silently be ignored.
   */
  public final void setFrame(InteractiveFrame g) {
    if (g == null || g == frame())
      return;
    if (g.theeye == this) {
      // /* option 1
      for (Agent agent : scene().inputHandler().agents())
        if (agent.defaultGrabber() != null)
          if (agent.defaultGrabber() == frame()) {
            agent.addGrabber(g);
            agent.setDefaultGrabber(g);
          }
      // scene().inputHandler().removeGrabber(frame());
      scene().pruneBranch(frame());// better than removeGrabber grabber
      // */
      // option 2
      // scene().inputHandler().shiftDefaultGrabber(g, frame());
      // //scene().inputHandler().removeGrabber(frame());
      // scene().pruneBranch(frame());// better than removeGrabber grabber
      gFrame = g;// frame() is new
    } else {
      System.out.println(
          "Warning no eye frame set as the eye class (" + this.getClass().getSimpleName() + ") and the frame eye class ("
              + g.eye().getClass().getSimpleName() + ") are different");
    }
  }

  // 6. ASSOCIATED FRAME AND FRAME WRAPPER FUNCTIONS

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().spinningSensitivity()}
   *
   * @see InteractiveFrame#spinningSensitivity()
   */
  public final float spinningSensitivity() {
    return frame().spinningSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setSpinningSensitivity(sensitivity)}
   *
   * @see InteractiveFrame#setSpinningSensitivity(float)
   */
  public final void setSpinningSensitivity(float sensitivity) {
    frame().setSpinningSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().rotationSensitivity()}
   *
   * @see InteractiveFrame#rotationSensitivity()
   */
  public final float rotationSensitivity() {
    return frame().rotationSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setRotationSensitivity(sensitivity)}
   *
   * @see InteractiveFrame#setRotationSensitivity(float)
   */
  public final void setRotationSensitivity(float sensitivity) {
    frame().setRotationSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().translationSensitivity()}
   *
   * @see InteractiveFrame#translationSensitivity()
   */
  public final float translationSensitivity() {
    return frame().translationSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setTranslationSensitivity(sensitivity)}
   *
   * @see InteractiveFrame#setTranslationSensitivity(float)
   */
  public final void setTranslationSensitivity(float sensitivity) {
    frame().setTranslationSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns {@code frame().scalingSensitivity()}
   *
   * @see InteractiveFrame#scalingSensitivity()
   */
  public final float scalingSensitivity() {
    return frame().scalingSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setScalingSensitivity(sensitivity)}
   *
   * @see InteractiveFrame#setScalingSensitivity(float)
   */
  public final void setScalingSensitivity(float sensitivity) {
    frame().setScalingSensitivity(sensitivity);
  }

  /**
   * Returns the Eye position, defined in the world coordinate system.
   * <p>
   * Use {@link #setPosition(Vec)} to set the Eye position. Other convenient methods are
   * showEntireScene() or fitSphere(). Actually returns
   * {@link Frame#position()}.
   */
  public final Vec position() {
    return frame().position();
  }

  /**
   * Sets the Eye {@link #position()} (the eye), defined in the world coordinate system.
   */
  public void setPosition(Vec pos) {
    frame().setPosition(pos);
  }

  /**
   * Returns the normalized up vector of the Eye, defined in the world coordinate system.
   * <p>
   * Set using {@link #setUpVector(Vec)} or {@link #setOrientation(Quat)}. It is
   * orthogonal to {@link #viewDirection()} and to {@link #rightVector()}.
   * <p>
   * It corresponds to the Y axis of the associated {@link #frame()} (actually returns
   * {@code frame().yAxis()}
   */
  public Vec upVector() {
    return frame().yAxis();
  }

  /**
   * Convenience function that simply calls {@code setUpVector(up, true)}.
   *
   * @see #setUpVector(Vec, boolean)
   */
  public void setUpVector(Vec up) {
    setUpVector(up, true);
  }

  /**
   * Returns the normalized right vector of the Eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the Eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}. Set using
   * {@link #setUpVector(Vec)}, {@link #lookAt(Vec)} or {@link #setOrientation(Quat)}.
   * <p>
   * Simply returns {@code frame().xAxis()}.
   */
  public Vec rightVector() {
    return frame().xAxis();
  }

  /**
   * Returns the radius of the scene observed by the Eye in scene (world) units.
   * <p>
   * In the case of a 3D Eye you need to provide such an approximation of the
   * scene dimensions so that the it can adapt its
   * {@link #zNear()} and
   * {@link #zFar()} values. See the {@link #sceneCenter()}
   * documentation.
   * <p>
   * Note that {@link AbstractScene#radius()} (resp.
   * {@link AbstractScene#setRadius(float)} simply call this
   * method on its associated Eye.
   *
   * @see #setSceneBoundingBox(Vec, Vec)
   */
  public float sceneRadius() {
    return scnRadius;

  }

  // 11. FLYSPEED

  /**
   * Returns the fly speed of the Eye.
   * <p>
   * Simply returns {@code frame().flySpeed()}. See the
   * {@link InteractiveFrame#flySpeed()} documentation. This value is
   * only meaningful when the action binding is MOVE_FORWARD or is MOVE_BACKWARD.
   * <p>
   * Set to 0.5% of the {@link #sceneRadius()} by {@link #setSceneRadius(float)} .
   *
   * @see #setFlySpeed(float)
   */
  public float flySpeed() {
    return frame().flySpeed();
  }

  /**
   * Sets the Eye {@link #flySpeed()}.
   * <p>
   * <b>Attention:</b> This value is modified by {@link #setSceneRadius(float)}.
   */
  public void setFlySpeed(float speed) {
    frame().setFlySpeed(speed);
  }

  /**
   * Returns the position of the scene center, defined in the world coordinate system.
   * <p>
   * The scene observed by the Eye should be roughly centered on this position, and
   * included in a {@link #sceneRadius()} ball.
   * <p>
   * Default value is the world origin. Use {@link #setSceneCenter(Vec)} to change it.
   *
   * @see #setSceneBoundingBox(Vec, Vec)
   * @see #zNear()
   * @see #zFar()
   */
  public Vec sceneCenter() {
    return scnCenter;
  }

  /**
   * Sets the {@link #sceneCenter()}.
   * <p>
   * <b>Attention:</b> This method also sets the {@link #anchor()} to
   * {@link #sceneCenter()}.
   */
  public void setSceneCenter(Vec center) {
    scnCenter = center;
    setAnchor(sceneCenter());
  }

  /**
   * The point the Eye revolves around with the ROTATE action binding. Defined in world
   * coordinate system.
   * <p>
   * Default value is the {@link #sceneCenter()}.
   * <p>
   * <b>Attention:</b> {@link #setSceneCenter(Vec)} changes this value.
   */
  public Vec anchor() {
    return anchorPnt;
  }

  /**
   * Returns the Eye aspect ratio defined by {@link #screenWidth()} /
   * {@link #screenHeight()}.
   * <p>
   * When the Eye is attached to a Scene, these values and hence the aspectRatio() are
   * automatically fitted to the viewer's window aspect ratio using
   * setScreenWidthAndHeight().
   */
  public float aspectRatio() {
    return (float) scrnWidth / (float) scrnHeight;
  }

  /**
   * Defines the Eye {@link #aspectRatio()}.
   * <p>
   * This value is actually inferred from the {@link #screenWidth()} /
   * {@link #screenHeight()} ratio. You should use
   * {@link #setScreenWidthAndHeight(int, int)} instead.
   * <p>
   * This method might however be convenient when the Eye is not associated with a Scene.
   * It actually sets the {@link #screenHeight()} to 100 and the {@link #screenWidth()}
   * accordingly.
   *
   * @see #setFOVToFitScene()
   */
  public void setAspectRatio(float aspect) {
    setScreenWidthAndHeight((int) (100.0 * aspect), 100);
  }

  /**
   * Sets Eye {@link #screenWidth()} and {@link #screenHeight()} (expressed in pixels).
   * <p>
   * You should not call this method when the Eye is associated with a Scene, since the
   * latter automatically updates these values when it is resized (hence overwriting your
   * values).
   * <p>
   * Non-positive dimension are silently replaced by a 1 pixel value to ensure boundary
   * coherence.
   * <p>
   * If your Eye is used without a Scene (offscreen rendering, shadow maps), use
   * {@link #setAspectRatio(float)} instead to define the projection matrix.
   */
  public void setScreenWidthAndHeight(int width, int height) {
    // Prevent negative and zero dimensions that would cause divisions by zero.
    if ((width != scrnWidth) || (height != scrnHeight))
      modified();
    scrnWidth = width > 0 ? width : 1;
    scrnHeight = height > 0 ? height : 1;
  }

  /**
   * Returns the width (in pixels) of the Eye screen.
   * <p>
   * Set using {@link #setScreenWidthAndHeight(int, int)}. This value is automatically
   * fitted to the Scene's window dimensions when the Eye is attached to a Scene.
   */
  public final int screenWidth() {
    return scrnWidth;
  }

  /**
   * Returns the height (in pixels) of the Eye screen.
   * <p>
   * Set using {@link #setScreenWidthAndHeight(int, int)}. This value is automatically
   * fitted to the Scene's window dimensions when the Eye is attached to a Scene.
   */
  public final int screenHeight() {
    return scrnHeight;
  }

  /**
   * Convenience function that simply returns {@code getProjectionMatrix(false)}
   *
   * @see #getProjection(Mat, boolean)
   */
  public Mat getProjection() {
    return getProjection(false);
  }

  /**
   * Convenience function that simply returns {@code getProjection(new Mat(), recompute)}
   *
   * @see #getProjection(Mat, boolean)
   */
  public Mat getProjection(boolean recompute) {
    return getProjection(new Mat(), recompute);
  }

  /**
   * Convenience function that simply returns {@code getProjection(m, false)}
   *
   * @see #getProjection(Mat, boolean)
   */
  public Mat getProjection(Mat m) {
    return getProjection(m, false);
  }

  /**
   * Fills {@code m} with the Eye projection matrix values and returns it. If {@code m} is
   * {@code null} a new Mat will be created.
   * <p>
   * If {@code recompute} is {@code true} first calls {@link #computeProjection()} to
   * define the Eye projection matrix. Otherwise it returns the projection matrix
   * previously computed, e.g., as with
   * {@link MatrixHelper#loadProjection()}.
   *
   * @see #getView(Mat, boolean)
   * @see MatrixHelper#loadProjection()
   * @see MatrixHelper#loadModelView()
   */
  public Mat getProjection(Mat m, boolean recompute) {
    if (m == null)
      m = new Mat();

    if (recompute)
      // May not be needed, but easier and more robust like this.
      computeProjection();
    m.set(projectionMat);

    return m;
  }

  /**
   * Fills the projection matrix with the {@code proj} matrix values.
   *
   * @see #setProjection(float[])
   * @see #setProjection(float[], boolean)
   */
  public void setProjection(Mat proj) {
    projectionMat.set(proj);
  }

  /**
   * Convenience function that simply calls {@code setProjectionMatrix(source, false)}.
   *
   * @see #setProjection(Mat)
   * @see #setProjection(float[], boolean)
   */
  public void setProjection(float[] source) {
    setProjection(source, false);
  }

  /**
   * Fills the projection matrix with the {@code source} matrix values (defined in
   * row-major order).
   *
   * @see #setProjection(Mat)
   * @see #setProjection(float[])
   */
  public void setProjection(float[] source, boolean transpose) {
    if (transpose)
      projectionMat.setTransposed(source);
    else
      projectionMat.set(source);
  }

  /**
   * Convenience function that simply returns {@code getOrthoWidthHeight(new
   * float[2])}.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  public float[] getBoundaryWidthHeight() {
    return getBoundaryWidthHeight(new float[2]);
  }

  /**
   * Fills in {@code target} with the {@code halfWidth} and {@code halfHeight} of the Eye
   * boundary and returns it. While {@code target[0]} holds {@code halfWidth},
   * {@code target[1]} holds {@code halfHeight}. Values are computed as:
   * {@code target[0] = rescalingOrthoFactor() * (frame().magnitude() * this.screenWidth() / 2)}
   * and {@code rescalingOrthoFactor() * (frame().magnitude() * this.screenHeight() / 2)}
   * .
   * <p>
   * These values are valid for 2d Windows and ortho Cameras (but not persp) and they are
   * expressed in virtual scene units.
   * <p>
   * In the case of ortho Cameras these values are proportional to the Camera (z
   * projected) distance to the {@link #anchor()}. When zooming on the object, the Camera
   * is translated forward and its boundary is narrowed, making the object appear bigger
   * on screen, as intuitively expected.
   * <p>
   * Overload this method to change this behavior if desired.
   *
   * @see #rescalingOrthoFactor()
   */
  public float[] getBoundaryWidthHeight(float[] target) {
    if ((target == null) || (target.length != 2)) {
      target = new float[2];
    }

    float orthoCoef = this.rescalingOrthoFactor();

    target[0] = (orthoCoef) * (frame().magnitude() * this.screenWidth() / 2);
    target[1] = (orthoCoef) * (frame().magnitude() * this.screenHeight() / 2);

    return target;
  }

  public float[] getOrthoWidthHeight() {
    return getBoundaryWidthHeight(new float[2]);
  }

  public float[] getOrthoWidthHeight(float[] target) {
    if ((target == null) || (target.length != 2)) {
      target = new float[2];
    }

    target[0] = (frame().magnitude() * this.screenWidth() / 2);
    target[1] = (frame().magnitude() * this.screenHeight() / 2);

    return target;
  }

  /**
   * Returns the Eye frame coordinates of a point {@code src} defined in world
   * coordinates.
   * <p>
   * {@link #worldCoordinatesOf(Vec)} performs the inverse transformation.
   * <p>
   * Note that the point coordinates are simply converted in a different coordinate
   * system. They are not projected on screen. Use
   * {@link #projectedCoordinatesOf(Vec, Frame)} for that.
   */
  public Vec eyeCoordinatesOf(Vec src) {
    return frame().coordinatesOf(src);
  }

  /**
   * Returns the world coordinates of the point whose position {@code src} is defined in
   * the Eye coordinate system.
   * <p>
   * {@link #eyeCoordinatesOf(Vec)} performs the inverse transformation.
   */
  public Vec worldCoordinatesOf(final Vec src) {
    return frame().inverseCoordinatesOf(src);
  }

  /**
   * Convenience function that simply returns {@code getViewMatrix(false)}
   *
   * @see #getView(boolean)
   * @see #getView(Mat)
   * @see #getView(Mat, boolean)
   * @see #getProjection()
   * @see #getProjection(boolean)
   * @see #getProjection(Mat)
   * @see #getProjection(Mat, boolean)
   */
  public Mat getView() {
    return getView(false);
  }

  /**
   * Convenience function that simply returns {@code getViewMatrix(new Mat(), recompute)}
   *
   * @see #getView()
   * @see #getView(Mat)
   * @see #getView(Mat, boolean)
   * @see #getProjection()
   * @see #getProjection(boolean)
   * @see #getProjection(Mat)
   * @see #getProjection(Mat, boolean)
   */
  public Mat getView(boolean recompute) {
    return getView(new Mat(), recompute);
  }

  /**
   * Convenience function that simply returns {@code getViewMatrix(m, false)}
   *
   * @see #getView()
   * @see #getView(boolean)
   * @see #getView(Mat, boolean)
   * @see #getProjection()
   * @see #getProjection(boolean)
   * @see #getProjection(Mat)
   * @see #getProjection(Mat, boolean)
   */
  public Mat getView(Mat m) {
    return getView(m, false);
  }

  /**
   * Fills {@code m} with the Eye View matrix values and returns it. If {@code m} is
   * {@code null} a new Mat will be created.
   * <p>
   * If {@code recompute} is {@code true} first calls {@link #computeView()} to define the
   * Eye view matrix. Otherwise it returns the view matrix previously computed, e.g., as
   * with {@link MatrixHelper#loadModelView()}.
   *
   * @see #getView()
   * @see #getView(boolean)
   * @see #getView(Mat)
   * @see #getProjection()
   * @see #getProjection(boolean)
   * @see #getProjection(Mat, boolean)
   */
  public Mat getView(Mat m, boolean recompute) {
    if (m == null)
      m = new Mat();
    if (recompute)
      // May not be needed, but easier like this.
      // Prevents from retrieving matrix in stereo mode -> overwrites shifted
      // value.
      computeView();
    m.set(viewMat);
    return m;
  }

  /**
   * Convenience function that simply calls {@code fromView(mv, true)}.
   *
   * @see #fromView(Mat, boolean)
   */
  public void fromView(Mat mv) {
    fromView(mv, true);
  }

  /**
   * Convenience function that simply returns {@code projectedCoordinatesOf(src, null)}.
   *
   * @see #projectedCoordinatesOf(Vec, Frame)
   */
  public final Vec projectedCoordinatesOf(Vec src) {
    return projectedCoordinatesOf(null, src, null);
  }

  /**
   * Convenience function that simply returns
   * {@code projectedCoordinatesOf(projview, src, null)}.
   *
   * @see #projectedCoordinatesOf(Vec, Frame)
   */
  public final Vec projectedCoordinatesOf(Mat projview, Vec src) {
    return projectedCoordinatesOf(projview, src, null);
  }

  /**
   * Convenience function that simply returns
   * {@code projectedCoordinatesOf(null, src, frame)}.
   *
   * @see #projectedCoordinatesOf(Vec, Frame)
   */
  public final Vec projectedCoordinatesOf(Vec src, Frame frame) {
    return projectedCoordinatesOf(null, src, frame);
  }

  /**
   * Returns the screen projected coordinates of a point {@code src} defined in the
   * {@code frame} coordinate system.
   * <p>
   * When {@code frame} is {@code null}, {@code src} is expressed in the world coordinate
   * system. See {@link #projectedCoordinatesOf(Vec)}.
   * <p>
   * The x and y coordinates of the returned Vec are expressed in pixel, (0,0) being the
   * upper left corner of the window. The z coordinate ranges between 0.0 (near plane) and
   * 1.0 (excluded, far plane). See the {@code gluProject} man page for details.
   * <p>
   * Use {@link AbstractScene#projectedCoordinatesOf(Vec)} which
   * is simpler and hasGrabber been optimized by caching the Projection x View matrix.
   * <p>
   * <b>Attention:</b> This method only uses the intrinsic Eye parameters (see
   * {@link #getView()}, {@link #getProjection()} and {@link #getViewport()}) and is
   * completely independent of the processing matrices. You can hence define a virtual Eye
   * and use this method to compute projections out of a classical rendering context.
   *
   * @see #unprojectedCoordinatesOf(Vec, Frame)
   */
  public final Vec projectedCoordinatesOf(Mat projview, Vec src, Frame frame) {
    float xyz[] = new float[3];

    if (frame != null) {
      Vec tmp = frame.inverseCoordinatesOf(src);
      project(projview, tmp.vec[0], tmp.vec[1], tmp.vec[2], xyz);
    } else
      project(projview, src.vec[0], src.vec[1], src.vec[2], xyz);

    return new Vec(xyz[0], xyz[1], xyz[2]);
  }

  /**
   * Convenience function that simply returns {@code unprojectedCoordinatesOf(src, null)}.
   * <p>
   * #see {@link #unprojectedCoordinatesOf(Vec, Frame)}
   */
  public final Vec unprojectedCoordinatesOf(Vec src) {
    return this.unprojectedCoordinatesOf(null, src, null);
  }

  /**
   * Convenience function that simply returns
   * {@code unprojectedCoordinatesOf(projviewInv, src, null)}.
   * <p>
   * #see {@link #unprojectedCoordinatesOf(Vec, Frame)}
   */
  public final Vec unprojectedCoordinatesOf(Mat projviewInv, Vec src) {
    return this.unprojectedCoordinatesOf(projviewInv, src, null);
  }

  /**
   * Convenience function that simply returns
   * {@code unprojectedCoordinatesOf(null, src, frame)}.
   * <p>
   * #see {@link #unprojectedCoordinatesOf(Vec, Frame)}
   */
  public final Vec unprojectedCoordinatesOf(Vec src, Frame frame) {
    return unprojectedCoordinatesOf(null, src, frame);
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
   * is the distance from the point you project to the camera, along the
   * {@link #viewDirection()} . See the {@code gluUnProject} man page for details.
   * <p>
   * The result is expressed in the {@code frame} coordinate system. When {@code frame} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code frame} hierarchy (i.e., when
   * {@link Frame#referenceFrame()} is non-null) is taken into
   * account.
   * <p>
   * {@link #projectedCoordinatesOf(Vec, Frame)} performs the inverse transformation.
   * <p>
   * This method only uses the intrinsic Eye parameters (see {@link #getView()},
   * {@link #getProjection()} and {@link #getViewport()}) and is completely independent of
   * the Processing matrices. You can hence define a virtual Eye and use this method to
   * compute un-projections out of a classical rendering context.
   * <p>
   * <b>Attention:</b> However, if your Eye is not attached to a Scene (used for offscreen
   * computations for instance), make sure the Eye matrices are updated before calling
   * this method (use {@link #computeView()}, {@link #computeProjection()}).
   * <p>
   * This method is not computationally optimized. If you call it several times with no
   * change in the matrices, you should buffer the entire inverse projection matrix (view,
   * projection and then viewport) to speed-up the queries. See the gluUnProject man page
   * for details.
   *
   * @see #projectedCoordinatesOf(Vec, Frame)
   * @see #setScreenWidthAndHeight(int, int)
   */
  public final Vec unprojectedCoordinatesOf(Mat projviewInv, Vec src, Frame frame) {
    float xyz[] = new float[3];
    // unproject(src.vec[0], src.vec[1], src.vec[2], this.getViewMatrix(true),
    // this.getProjectionMatrix(true),
    // getViewport(), xyz);
    unproject(projviewInv, src.vec[0], src.vec[1], src.vec[2], xyz);
    if (frame != null)
      return frame.coordinatesOf(new Vec(xyz[0], xyz[1], xyz[2]));
    else
      return new Vec(xyz[0], xyz[1], xyz[2]);
  }

  protected void updateViewPort() {
    viewport = getViewport();
  }

  /**
   * Convenience function that simply calls {@code return} {@link #getViewport(int[])}.
   */
  public int[] getViewport() {
    return getViewport(new int[4]);
  }

  /**
   * Fills {@code viewport} with the Eye viewport and returns it. If viewport is null (or
   * not the correct size), a new array will be created.
   * <p>
   * This method is mainly used in conjunction with
   * {@code project(float, float, float, Mat, Mat, int[], float[])} , which requires such
   * a viewport. Returned values are (0, {@link #screenHeight()}, {@link #screenWidth()},
   * - {@link #screenHeight()}), so that the origin is located in the upper left corner of
   * the window.
   */
  public int[] getViewport(int[] vp) {
    if ((vp == null) || (vp.length != 4)) {
      vp = new int[4];
    }
    vp[0] = 0;
    vp[1] = screenHeight();
    vp[2] = screenWidth();
    vp[3] = -screenHeight();
    return vp;
  }

  /**
   * Similar to {@code gluProject}: map object coordinates to window coordinates.
   *
   * @param objx             Specify the object x coordinate.
   * @param objy             Specify the object y coordinate.
   * @param objz             Specify the object z coordinate.
   * @param windowCoordinate Return the computed window coordinates.
   */
  public boolean project(float objx, float objy, float objz, float[] windowCoordinate) {
    return project(null, objx, objy, objz, windowCoordinate);
  }

  // cached version
  public boolean project(Mat projectionViewMat, float objx, float objy, float objz, float[] windowCoordinate) {
    if (projectionViewMat == null)
      projectionViewMat = Mat.multiply(projectionMat, viewMat);

    float in[] = new float[4];
    float out[] = new float[4];

    in[0] = objx;
    in[1] = objy;
    in[2] = objz;
    in[3] = 1.0f;

    out[0] = projectionViewMat.mat[0] * in[0] + projectionViewMat.mat[4] * in[1] + projectionViewMat.mat[8] * in[2]
        + projectionViewMat.mat[12] * in[3];
    out[1] = projectionViewMat.mat[1] * in[0] + projectionViewMat.mat[5] * in[1] + projectionViewMat.mat[9] * in[2]
        + projectionViewMat.mat[13] * in[3];
    out[2] = projectionViewMat.mat[2] * in[0] + projectionViewMat.mat[6] * in[1] + projectionViewMat.mat[10] * in[2]
        + projectionViewMat.mat[14] * in[3];
    out[3] = projectionViewMat.mat[3] * in[0] + projectionViewMat.mat[7] * in[1] + projectionViewMat.mat[11] * in[2]
        + projectionViewMat.mat[15] * in[3];

    if (out[3] == 0.0)
      return false;

    updateViewPort();

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

  public boolean unproject(float winx, float winy, float winz, float[] objCoordinate) {
    return unproject(null, winx, winy, winz, objCoordinate);
  }

  /**
   * Similar to {@code gluUnProject}: map window coordinates to object coordinates.
   *
   * @param projectionViewInverseMat Specify the cached (projection * modelvview)^1 matrix.
   * @param winx                     Specify the window x coordinate.
   * @param winy                     Specify the window y coordinate.
   * @param winz                     Specify the window z coordinate.
   * @param objCoordinate            Return the computed object coordinates.
   */
  // Warning projectionViewInverseMat should be invertible (not checked here)
  // cached version
  public boolean unproject(Mat projectionViewInverseMat, float winx, float winy, float winz, float[] objCoordinate) {
    if (projectionViewInverseMat == null) {
      projectionViewInverseMat = new Mat();
      boolean projectionViewMatHasInverse = Mat.multiply(projectionMat, viewMat).invert(projectionViewInverseMat);
      if (projectionViewMatHasInverse)
        return unproject(projectionViewInverseMat, winx, winy, winz, objCoordinate);
      else
        return false;
    }

    updateViewPort();

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

    projectionViewInverseMat.multiply(in, out);
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
   * Enables or disables automatic update of the eye boundary plane equations every frame
   * according to {@code flag}. Computation of the equations is expensive and hence is
   * disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public void enableBoundaryEquations(boolean flag) {
    fpCoefficientsUpdate = flag;
  }

  /**
   * Returns {@code true} if automatic update of the eye boundary plane equations is
   * enabled and {@code false} otherwise. Computation of the equations is expensive and
   * hence is disabled by default.
   *
   * @see #updateBoundaryEquations()
   */
  public boolean areBoundaryEquationsEnabled() {
    return fpCoefficientsUpdate;
  }

  /**
   * Updates the boundary plane equations according to the current eye setup, by simply
   * calling {@link #computeBoundaryEquations()}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * boundary equations to be updated only occasionally (rare). Use
   * {@link AbstractScene#enableBoundaryEquations()} which
   * automatically update the boundary equations every frame instead.
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
  public void updateBoundaryEquations() {
    if (lastUpdate() != lastFPCoeficientsUpdateIssued) {
      computeBoundaryEquations(fpCoefficients);
      lastFPCoeficientsUpdateIssued = lastUpdate();
    }
  }

  /**
   * Returns the boundary plane equations.
   * <p>
   * The six 4-component vectors returned by this method, respectively correspond to the
   * left, right, near, far, top and bottom Eye boundary planes. Each vector holds a plane
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
   * Scene setup (with
   * {@link AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
  public float[][] getBoundaryEquations() {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    return fpCoefficients;
  }

  /**
   * Returns the signed distance between point {@code pos} and plane {@code index} in
   * Scene units. The distance is negative if the point lies in the planes's boundary
   * halfspace, and positive otherwise.
   * <p>
   * {@code index} is a value between {@code 0} and {@code 5} which respectively
   * correspond to the left, right, near, far, top and bottom Eye boundary planes.
   * <p>
   * <b>Attention:</b> The eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link AbstractScene#enableBoundaryEquations()}).
   *
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
  public float distanceToBoundary(int index, Vec pos) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations (needed by distanceToBoundary) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    Vec myVec = new Vec(fpCoefficients[index][0], fpCoefficients[index][1], fpCoefficients[index][2]);
    return Vec.dot(pos, myVec) - fpCoefficients[index][3];
  }

  /**
   * Returns the pixel to scene (units) ratio at {@code position}.
   * <p>
   * Convenience function that simply returns {@code 1 / sceneToPixelRatio(position)}.
   *
   * @see #sceneToPixelRatio(Vec)
   */
  public float pixelToSceneRatio(Vec position) {
    return 1 / sceneToPixelRatio(position);
  }

  /**
   * Moves the Eye so that the entire scene is visible.
   * <p>
   * Simply calls {@link #fitBall(Vec, float)} on a sphere defined by
   * {@link #sceneCenter()} and {@link #sceneRadius()}.
   * <p>
   * You will typically use this method at init time after you defined a new
   * {@link #sceneRadius()}.
   */
  public void showEntireScene() {
    fitBall(sceneCenter(), sceneRadius());
  }

  /**
   * Moves the Eye so that its {@link #sceneCenter()} is projected on the center of the
   * window. The {@link #orientation()} (and in the case of perps 3d
   * {@link #fieldOfView()}) is (are) unchanged.
   * <p>
   * Simply projects the current position on a line passing through {@link #sceneCenter()}
   * .
   *
   * @see #showEntireScene()
   */
  public void centerScene() {
    frame().projectOnLine(sceneCenter(), viewDirection());
  }

  //TODO work in progress 2D and 3D

  /**
   * Returns the ratio of scene (units) to pixel at {@code position}.
   * <p>
   * A line of {@code n * sceneToPixelRatio()} scene units, located at {@code position} in
   * the world coordinates system, will be projected with a length of {@code n} pixels on
   * screen.
   * <p>
   * Use this method to scale objects so that they have a constant pixel size on screen.
   * The following code will draw a 20 pixel line, starting at {@link #sceneCenter()} and
   * always directed along the screen vertical direction:
   * <p>
   * {@code beginShape(LINES);}<br>
   * {@code vertex(sceneCenter().x, sceneCenter().y, sceneCenter().z);}<br>
   * {@code Vec v = Vec.addGrabber(sceneCenter(), Vec.mult(upVector(), 20 * sceneToPixelRatio(sceneCenter())));}
   * <br>
   * {@code vertex(v.x, v.y, v.z);}<br>
   * {@code endShape();}<br>
   */
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

  /**
   * Returns {@code true} if {@code point} is visible (i.e, lies within the Eye boundary)
   * and {@code false} otherwise.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
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

  /**
   * Returns {@link Eye.Visibility#VISIBLE},
   * {@link Eye.Visibility#INVISIBLE}, or
   * {@link Eye.Visibility#SEMIVISIBLE}, depending whether the
   * sphere (of radius {@code radius} and center {@code center}) is visible, invisible, or
   * semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
  public Visibility ballVisibility(Vec center, float radius) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The camera frustum plane equations (needed by sphereIsVisible) may be outdated. Please "
              + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    boolean allInForAllPlanes = true;
    for (int i = 0; i < 6; ++i) {
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
   * Returns {@link Eye.Visibility#VISIBLE},
   * {@link Eye.Visibility#INVISIBLE}, or
   * {@link Eye.Visibility#SEMIVISIBLE}, depending whether the
   * axis aligned box (defined by corners {@code p1} and {@code p2}) is visible,
   * invisible, or semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see AbstractScene#enableBoundaryEquations()
   */
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
        return Visibility.INVISIBLE;
    }

    if (allInForAllPlanes)
      return Visibility.VISIBLE;

    // Too conservative, but tangent cases are too expensive to detect
    return Visibility.SEMIVISIBLE;
  }

  /**
   * Convenience function that in 2D simply returns
   * {@code computeFrustumPlanesCoefficients(new float [4][3])} and in 3D
   * {@code computeFrustumPlanesCoefficients(new float [6][4])}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link AbstractScene#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations(float[][])
   */
  public float[][] computeBoundaryEquations() {
    return computeBoundaryEquations(new float[6][4]);
  }

  /**
   * Fills {@code coef} with the 6 plane equations of the camera frustum and returns it.
   * <p>
   * In 2D the four 4-component vectors of {@code coef} respectively correspond to the
   * left, right, top and bottom Window boundary lines. Each vector holds a plane equation
   * of the form:
   * <p>
   * {@code a*x + b*y + c = 0} where {@code a}, {@code b} and {@code c} are the 3
   * components of each vector, in that order.
   * <p>
   * <p>
   * In 3D the six 4-component vectors of {@code coef} respectively correspond to the
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
   * {@code // Retrieve place equations}<br>
   * {@code float [][] coef =
   * mainViewer.camera().getFrustumPlanesCoefficients();}<br>
   * {@code // These two additional clipping planes (which must have been enabled)} <br>
   * {@code // will reproduce the mainViewer's near and far clipping.}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE0, coef[2]);}<br>
   * {@code gl.glClipPlane(GL.GL_CLIP_PLANE1, coef[3]);}<br>
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link AbstractScene#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations()
   */
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

  // 4. SCENE RADIUS AND CENTER

  /**
   * Sets the {@link #sceneRadius()} value in scene (world) units. Negative values are
   * ignored. It also sets {@link #flySpeed()} to 1% of {@link #sceneRadius()}.
   */
  public void setSceneRadius(float radius) {
    if (radius <= 0.0f) {
      System.out.println("Warning: Scene radius must be positive - Ignoring value");
      return;
    }
    scnRadius = radius;
    setFlySpeed(0.01f * sceneRadius());
    for (Grabber mg : gScene.motionAgent().grabbers()) {
      if (mg instanceof InteractiveFrame)
        ((InteractiveFrame) mg).setFlySpeed(0.01f * sceneRadius());
    }
    // TODO previous was:
    //if(gScene.is3D())
      //setFocusDistance(sceneRadius() / (float) Math.tan(fieldOfView() / 2.0f));
  }

  /**
   * Returns the Eye {@link #position()} to {@link #sceneCenter()} distance in Scene
   * units.
   * <p>
   * 3D Cameras return the projected Eye {@link #position()} to {@link #sceneCenter()}
   * distance along the Camera Z axis and use it in
   * {@link #zNear()} and
   * {@link #zFar()} to optimize the Z range.
   */
  public float distanceToSceneCenter() {
    Vec zCam = frame().zAxis();
    Vec cam2SceneCenter = Vec.subtract(position(), sceneCenter());
    return Math.abs(Vec.dot(cam2SceneCenter, zCam));
  }

  /**
   * Returns the Eye {@link #position()} to {@link #anchor()} distance in Scene units.
   * <p>
   * 3D Cameras return the projected Eye {@link #position()} to {@link #anchor()} distance
   * along the Camera Z axis and use it in {@link #getBoundaryWidthHeight(float[])} so
   * that when the Camera is translated forward then its frustum is narrowed, making the
   * object appear bigger on screen, as intuitively expected.
   */
  public float distanceToAnchor() {
    Vec zCam = frame().zAxis();
    Vec cam2anchor = Vec.subtract(position(), anchor());
    return Math.abs(Vec.dot(cam2anchor, zCam));
  }

  /**
   * Similar to {@link #setSceneRadius(float)} and {@link #setSceneCenter(Vec)}, but the
   * scene limits are defined by a (world axis aligned) bounding box.
   */
  public void setSceneBoundingBox(Vec min, Vec max) {
    setSceneCenter(Vec.multiply(Vec.add(min, max), 1 / 2.0f));
    setSceneRadius(0.5f * (Vec.subtract(max, min)).magnitude());
  }

  // 5. ANCHOR REFERENCE POINT

  // 8. MATRICES

  /**
   * Computes the View matrix associated with the Eye's {@link #position()} and
   * {@link #orientation()}.
   * <p>
   * This matrix converts from the world coordinates system to the Eye coordinates system,
   * so that coordinates can then be projected on screen using the projection matrix (see
   * {@link #computeProjection()}).
   * <p>
   * Use {@link #getView()} to retrieve this matrix.
   * <p>
   * <b>Note:</b> You must call this method if your Eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   */
  public void computeView() {
    //TODO remove cast
    Quat q = (Quat)frame().orientation();

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
   * Simply returns {@code 1} which is valid for 2d Windows.
   * <p>
   * In 3D returns a value proportional to the Camera (z projected) distance to the
   * {@link #anchor()} so that when zooming on the object, the ortho Camera is translated
   * forward and its boundary is narrowed, making the object appear bigger on screen, as
   * intuitively expected.
   * <p>
   * Value is computed as: {@code 2 * distanceToAnchor() / screenHeight()}.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  public float rescalingOrthoFactor() {
    if(gScene.is2D())
      return 1.0f;
    float toAnchor = this.distanceToAnchor();
    float epsilon = 0.0001f;
    return (2 * (toAnchor == 0 ? epsilon : toAnchor) * rapK / screenHeight());
  }

  /**
   * Sets the {@link #anchor()}, defined in the world coordinate system.
   */
  public void setAnchor(Vec rap) {
    if(gScene.is2D()) {
      anchorPnt = rap;
      anchorPnt.setZ(0);
    }
    else {
      float prevDist = distanceToAnchor();
      this.anchorPnt = rap;
      float newDist = distanceToAnchor();
      if (prevDist != 0 && newDist != 0)
        rapK *= prevDist / newDist;
    }
  }

  /**
   * Same as {@code setAnchor(new Vec(x,y,z))}.
   *
   * @see AbstractScene#setAnchor(Vec)
   */
  public void setAnchor(float x, float y, float z) {
    setAnchor(new Vec(x, y, z));
  }

  /**
   * Computes the projection matrix associated with the Eye.
   * <p>
   * If Eye is a 3D PERSPECTIVE Camera, defines a projection matrix using the
   * {@link #fieldOfView()}, {@link #aspectRatio()},
   * {@link #zNear()} and
   * {@link #zFar()} parameters. If Eye is a 3D ORTHOGRAPHIC
   * Camera, the frustum's width and height are set using
   * {@link #getBoundaryWidthHeight()}. Both types use
   * {@link #zNear()} and
   * {@link #zFar()} to place clipping planes. These values
   * are determined from sceneRadius() and sceneCenter() so that they best fit the scene
   * size.
   * <p>
   * Use {@link #getProjection()} to retrieve this matrix.
   * <p>
   * <b>Note:</b> You must call this method if your Eye is not associated with a Scene and
   * is used for offscreen computations (using {@code projectedCoordinatesOf()} for
   * instance).
   *
   * @see #setProjection(Mat)
   */
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

  /**
   * Sets the Eye {@link #position()} and {@link #orientation()} from an OpenGL-like View
   * matrix.
   * <p>
   * After this method hasGrabber been called, {@link #getView()} returns a matrix equivalent to
   * {@code mv}. Only the {@link #position()} and {@link #orientation()} of the Eye are
   * modified.
   */
  public void fromView(Mat mv, boolean recompute) {
    Quat q = new Quat();
    q.fromMatrix(mv);
    setOrientation(q);
    setPosition(Vec.multiply(q.rotate(new Vec(mv.mat[12], mv.mat[13], mv.mat[14])), -1));
    if (recompute)
      this.computeView();
  }

  /**
   * 2D Windows simply call {@code frame().setPosition(target.x(), target.y())}. 3D
   * Cameras set {@link #orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The Camera {@link #position()} is not modified.
   * Simply {@link #setViewDirection(Vec)}).
   *
   * @see #at()
   * @see #setUpVector(Vec)
   * @see #setOrientation(Quat)
   * @see #showEntireScene()
   * @see #fitBall(Vec, float)
   * @see #fitBoundingBox(Vec, Vec)
   */
  public void lookAt(Vec target) {
    setViewDirection(Vec.subtract(target, position()));
  }

  /**
   * 2D Windows return the postion. 3D Cameras return a point defined in the world
   * coordinate system where the camera is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   *
   * @see #lookAt(Vec)
   */
  public Vec at() {
    return Vec.add(position(), viewDirection());
  }

  /**
   * Moves the Eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   * <p>
   * In 3D the Camera is simply translated along its {@link #viewDirection()} so that the
   * sphere fits the screen. Its {@link #orientation()} and its
   * {@link #fieldOfView()} are unchanged. You should
   * therefore orientate the Camera before you call this method.
   *
   * @see #lookAt(Vec)
   * @see #setOrientation(Quat)
   * @see #setUpVector(Vec, boolean)
   */
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

  /**
   * Moves the Eye so that the (world axis aligned) bounding box ({@code min} ,
   * {@code max}) is entirely visible, using {@link #fitBall(Vec, float)}.
   */
  public void fitBoundingBox(Vec min, Vec max) {
    float diameter = Math.max(Math.abs(max.vec[1] - min.vec[1]), Math.abs(max.vec[0] - min.vec[0]));
    diameter = Math.max(Math.abs(max.vec[2] - min.vec[2]), diameter);
    fitBall(Vec.multiply(Vec.add(min, max), 0.5f), 0.5f * diameter);
  }

  /**
   * Moves the Eye so that the rectangular screen region defined by {@code rectangle}
   * (pixel units, with origin in the upper left corner) fits the screen.
   * <p>
   * in 3D the Camera is translated (its {@link #orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>frustum</i> in 3D, it's the intersection of this frustum with a plane (orthogonal
   * to the {@link #viewDirection()} and passing through the {@link #sceneCenter()}) that
   * is used to define the 3D rectangle that is eventually fitted.
   */
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
   * scene (default action binding).
   * <p>
   * When {@code noMove} is true, the Eye {@link #position()} is left unchanged, which is
   * an intuitive behavior when the Eye is in first person mode.
   *
   * @see #lookAt(Vec)
   * @see #setOrientation(Quat)
   */
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
   * Returns the normalized view direction of the Eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #frame()} (
   * {@code frame().inverseTransformOf(new Vec(0.0f, 0.0f, -1.0f))} ) whih in 2D always is
   * (0,0,-1)
   * <p>
   * In 3D change this value using
   * {@link #setViewDirection(Vec)}, {@link #lookAt(Vec)} or
   * {@link #setOrientation(Quat)} . It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vec viewDirection() {
    //TODO test me
    //before it was:
    //if(gScene.is2D())
      //return new Vec(0, 0, (frame().zAxis().z() > 0) ? -1 : 1);
    //bu now I think we should simply go something like this:
    return frame().zAxis(false);
  }

  /**
   * Sets the Eye {@link #orientation()}, defined in the world coordinate system.
   */
  public void setOrientation(Quat q) {
    frame().setOrientation(q);
    frame().updateSceneUpVector();
  }

  //TODO only 3D!

  /**
   * Enumerates the two possible types of Camera.
   * <p>
   * This type mainly defines different camera projection matrix. Many other methods take
   * this Type into account.
   */
  public enum Type {
    PERSPECTIVE, ORTHOGRAPHIC
  }

  // C a m e r a p a r a m e t e r s
  private float zNearCoef;
  private float zClippingCoef;
  private Type tp; // PERSPECTIVE or ORTHOGRAPHIC

  // rescale ortho when anchor changes
  private float rapK = 1;

  // Inverse the direction of an horizontal mouse motion. Depends on the
  // projected
  // screen orientation of the vertical axis when the mouse button is pressed.
  public boolean cadRotationIsReversed;

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
    if (direction.squaredNorm() == 0)
      return;

    Vec xAxis = direction.cross(upVector());
    if (xAxis.squaredNorm() == 0) {
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

  // 3. FRUSTUM

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
   * {@link Type#ORTHOGRAPHIC}.
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
   * {@link Frame#magnitude()} using the following expression:
   * {@code frame().setMagnitude((float) Math.tan(fov / 2.0f))}.
   *
   * @see #fieldOfView()
   */
  public void setFieldOfView(float fov) {
    // fldOfView = fov;
    frame().setMagnitude((float) Math.tan(fov / 2.0f));
    //TODO decide after stereo params
    //setFocusDistance(sceneRadius() / frame().magnitude());
  }

  /**
   * Changes the Camera {@link #fieldOfView()} so that the entire scene (defined by
   * {@link AbstractScene#center()} and
   * {@link AbstractScene#radius()} is visible from the Camera
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
   * {@link AbstractScene#setEye(Eye)}.
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
   * {@link AbstractScene#isLeftHanded()} or in counter-clockwise
   * order if {@link AbstractScene#isRightHanded()}.
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

    if (axis.magnitude() != 0)
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

  /**
   * Same as {@code lookAt(new Vec(x,y,z))}.
   *
   * @see #lookAt(Vec)
   */
  public void lookAt(float x, float y, float z) {
    lookAt(new Vec(x, y, z));
  }
}
