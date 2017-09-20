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

import remixlab.bias.Agent;
import remixlab.bias.BogusEvent;
import remixlab.bias.Grabber;
import remixlab.bias.event.ClickEvent;
import remixlab.bias.event.KeyboardEvent;
import remixlab.bias.event.MotionEvent;
import remixlab.dandelion.core.AbstractScene.Platform;
import remixlab.dandelion.geom.*;
import remixlab.fpstiming.TimingTask;
import remixlab.util.Copyable;
import remixlab.util.Util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for 3D {@link remixlab.dandelion.core.Camera}s and 2D
 * {@link remixlab.dandelion.core.Window}s.
 * <p>
 * An Eye defines some intrinsic parameters ({@link #position()}, {@link #viewDirection()}
 * , {@link #upVector()}...) and useful positioning tools that ease its placement (
 * {@link #showEntireScene()}, {@link #fitBall(Vec, float)}, {@link #lookAt(Vec)}...). It
 * exports its associated projection and view matrices and it can interactively be
 * modified using any interaction mechanism you can think of.
 * <p>
 * An Eye holds a collection of paths ({@link #keyFrameInterpolator(int key)}) each of
 * which can be interpolated ( {@link #playPath}). It also provides visibility routines (
 * {@link #isPointVisible(Vec)}, {@link #ballVisibility(Vec, float)},
 * {@link #boxVisibility(Vec, Vec)}), from which advanced geometry culling techniques can
 * be implemented.
 * <p>
 * The {@link #position()} and {@link #orientation()} of the Eye are defined by an
 * {@link remixlab.dandelion.core.GenericFrame} (retrieved using {@link #frame()}). These
 * methods are just convenient wrappers to the equivalent Frame methods. This also means
 * that the Eye {@link #frame()} can be attached to a
 * {@link remixlab.dandelion.geom.Frame#referenceFrame()} which enables complex Eye
 * setups. An Eye has its own magnitude, different from that of the scene (i.e.,
 * {@link remixlab.dandelion.geom.Frame#magnitude()} doesn't necessarily equals {@code 1}
 * ), which allows to scale the view. Use {@link #eyeCoordinatesOf(Vec)} and
 * {@link #worldCoordinatesOf(Vec)} (or any of the powerful Frame transformations, such as
 * {@link remixlab.dandelion.geom.Frame#coordinatesOf(Vec)},
 * {@link remixlab.dandelion.geom.Frame#transformOf(Vec)}, ...) to convert to and from the
 * Eye {@link #frame()} coordinate system. {@link #projectedCoordinatesOf(Vec)} and
 * {@link #unprojectedCoordinatesOf(Vec)} will convert from screen to 3D coordinates.
 * <p>
 * An Eye can also be used outside of an Scene for its coordinate system conversion
 * capabilities.
 */
public abstract class Eye implements Copyable {
  class GrabberEyeFrame extends GenericFrame {
    public final int LEFT_ID = 37, CENTER_ID = 3, RIGHT_ID = 39, WHEEL_ID = 8, NO_BUTTON = BogusEvent.NO_ID, // 1.b. Keys
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
    public void performInteraction(MotionEvent event) {
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
    public void performInteraction(ClickEvent event) {
      if (event.clickCount() == 2) {
        if (event.id() == LEFT_ID)
          center();
        if (event.id() == RIGHT_ID)
          align();
      }
    }

    @Override
    public void performInteraction(KeyboardEvent event) {
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
  protected GenericFrame gFrame;

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

  // P o i n t s o f V i e w s a n d K e y F r a m e s
  protected HashMap<Integer, KeyFrameInterpolator> kfi;
  // protected Iterator<Integer> itrtr;
  protected KeyFrameInterpolator interpolationKfi;
  // protected GrabberFrame tempFrame;

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
  public boolean anchorFlag;
  public boolean pupFlag;
  public Vec pupVec;
  protected TimingTask timerFx;

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
    interpolationKfi = new KeyFrameInterpolator(gScene, frame());
    kfi = new HashMap<Integer, KeyFrameInterpolator>();
    anchorPnt = new Vec(0.0f, 0.0f, 0.0f);

    this.timerFx = new TimingTask() {
      public void execute() {
        unSetTimerFlag();
      }
    };
    this.gScene.registerTimingTask(timerFx);

    setFrame(new GrabberEyeFrame(this));
    setSceneRadius(100);
    setSceneCenter(new Vec(0.0f, 0.0f, 0.0f));
    setScreenWidthAndHeight(gScene.width(), gScene.height());

    viewMat = new Mat();
    projectionMat = new Mat();
    projectionMat.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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

    this.timerFx = new TimingTask() {
      public void execute() {
        unSetTimerFlag();
      }
    };
    this.gScene.registerTimingTask(timerFx);

    this.gFrame = oVP.frame().get();
    this.interpolationKfi = oVP.interpolationKfi.get();
    this.kfi = new HashMap<Integer, KeyFrameInterpolator>();

    Iterator<Integer> itrtr = oVP.kfi.keySet().iterator();
    while (itrtr.hasNext()) {
      Integer key = itrtr.next();
      this.kfi.put(new Integer(key.intValue()), oVP.kfi.get(key).get());
    }

    this.setSceneRadius(oVP.sceneRadius());
    this.setSceneCenter(oVP.sceneCenter());
    this.setScreenWidthAndHeight(oVP.screenWidth(), oVP.screenHeight());
    this.viewMat = oVP.viewMat.get();
    this.projectionMat = oVP.projectionMat.get();
  }

  @Override
  public abstract Eye get();

  /**
   * Returns the GrabberFrame attached to the Eye.
   * <p>
   * This GrabberFrame defines its {@link #position()}, {@link #orientation()} and can
   * translate bogus events into Eye displacement. Set using
   * {@link #setFrame(GenericFrame)}.
   */
  public GenericFrame frame() {
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
   * Internal use. Called from the timer to stop displaying the point under pixel and
   * anchor visual hints.
   */
  protected void unSetTimerFlag() {
    anchorFlag = false;
    pupFlag = false;
  }

  /**
   * Internal use. Run the reset anchor hint timer according to {@code period}.
   */
  protected void runResetAnchorHintTimer(long period) {
    timerFx.runOnce(period);
  }

  /**
   * Returns the Eye {@link #position()} to {@link #sceneCenter()} distance in Scene
   * units.
   * <p>
   * 3D Cameras return the projected Eye {@link #position()} to {@link #sceneCenter()}
   * distance along the Camera Z axis and use it in
   * {@link remixlab.dandelion.core.Camera#zNear()} and
   * {@link remixlab.dandelion.core.Camera#zFar()} to optimize the Z range.
   */
  public abstract float distanceToSceneCenter();

  /**
   * Returns the Eye {@link #position()} to {@link #anchor()} distance in Scene units.
   * <p>
   * 3D Cameras return the projected Eye {@link #position()} to {@link #anchor()} distance
   * along the Camera Z axis and use it in {@link #getBoundaryWidthHeight(float[])} so
   * that when the Camera is translated forward then its frustum is narrowed, making the
   * object appear bigger on screen, as intuitively expected.
   */
  public abstract float distanceToAnchor();

  /**
   * Returns the Eye orientation, defined in the world coordinate system.
   * <p>
   * Actually returns {@code frame().orientation()}. Use {@link #setOrientation(Rotation)}
   * , {@link #setUpVector(Vec)} or {@link #lookAt(Vec)} to set the Eye orientation.
   */
  public Rotation orientation() {
    return frame().orientation();
  }

  protected void modified() {
    lastNonFrameUpdate = AbstractScene.frameCount;
  }

  /**
   * Max between {@link remixlab.dandelion.core.GenericFrame#lastUpdate()} and
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
   * @see remixlab.dandelion.core.AbstractScene#flip()
   */
  public void flip() {
    gScene.flip();
  }

  /**
   * Internal use. Temporarily attach a frame to the Eye which is useful to some
   * interpolation methods such as {@link #interpolateToFitScene()}.
   */
  protected final void replaceFrame(GenericFrame g) {
    if (g == null || g == frame())
      return;
    if (g.theeye == null) {// only detached frames which call pruneBranch on g
      gFrame = g;
      frame().theeye = this;
      interpolationKfi.setFrame(frame());
      Iterator<KeyFrameInterpolator> itr = kfi.values().iterator();
      while (itr.hasNext())
        itr.next().setFrame(frame());
    }
  }

  /**
   * Sets the Eye {@link #frame()}.
   * <p>
   * If you want to move the Eye, use {@link #setPosition(Vec)} and
   * {@link #setOrientation(Rotation)} or one of the Eye positioning methods (
   * {@link #lookAt(Vec)}, {@link #fitBall(Vec, float)}, {@link #showEntireScene()}...)
   * instead.
   * <p>
   * This method is actually mainly useful if you derive the GrabberFrame class and want
   * to use an instance of your new class to move the Eye.
   * <p>
   * A {@code null} {@code icf} reference will silently be ignored.
   */
  public final void setFrame(GenericFrame g) {
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
      scene().pruneBranch(frame());// better than remove grabber
      // */
      // option 2
      // scene().inputHandler().shiftDefaultGrabber(g, frame());
      // //scene().inputHandler().removeGrabber(frame());
      // scene().pruneBranch(frame());// better than remove grabber
      gFrame = g;// frame() is new
      if (gScene.is3D())
        ((Camera) this).setFocusDistance(sceneRadius() / (float) Math.tan(((Camera) this).fieldOfView() / 2.0f));
      interpolationKfi.setFrame(frame());
      Iterator<KeyFrameInterpolator> itr = kfi.values().iterator();
      while (itr.hasNext())
        itr.next().setFrame(frame());
    } else {
      System.out.println(
          "Warning no eye frame set as the eye class (" + this.getClass().getSimpleName() + ") and the frame eye class ("
              + g.eye().getClass().getSimpleName() + ") are different");
    }
  }

  /**
   * 2D Windows simply call {@code frame().setPosition(target.x(), target.y())}. 3D
   * Cameras set {@link #orientation()}, so that it looks at point {@code target} defined
   * in the world coordinate system (The Camera {@link #position()} is not modified.
   * Simply {@link remixlab.dandelion.core.Camera#setViewDirection(Vec)}).
   *
   * @see #at()
   * @see #setUpVector(Vec)
   * @see #setOrientation(Rotation)
   * @see #showEntireScene()
   * @see #fitBall(Vec, float)
   * @see #fitBoundingBox(Vec, Vec)
   */
  public abstract void lookAt(Vec target);

  // 6. ASSOCIATED FRAME AND FRAME WRAPPER FUNCTIONS

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().spinningSensitivity()}
   *
   * @see remixlab.dandelion.core.GenericFrame#spinningSensitivity()
   */
  public final float spinningSensitivity() {
    return frame().spinningSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setSpinningSensitivity(sensitivity)}
   *
   * @see remixlab.dandelion.core.GenericFrame#setSpinningSensitivity(float)
   */
  public final void setSpinningSensitivity(float sensitivity) {
    frame().setSpinningSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().rotationSensitivity()}
   *
   * @see remixlab.dandelion.core.GenericFrame#rotationSensitivity()
   */
  public final float rotationSensitivity() {
    return frame().rotationSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setRotationSensitivity(sensitivity)}
   *
   * @see remixlab.dandelion.core.GenericFrame#setRotationSensitivity(float)
   */
  public final void setRotationSensitivity(float sensitivity) {
    frame().setRotationSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns
   * {@code frame().translationSensitivity()}
   *
   * @see remixlab.dandelion.core.GenericFrame#translationSensitivity()
   */
  public final float translationSensitivity() {
    return frame().translationSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setTranslationSensitivity(sensitivity)}
   *
   * @see remixlab.dandelion.core.GenericFrame#setTranslationSensitivity(float)
   */
  public final void setTranslationSensitivity(float sensitivity) {
    frame().setTranslationSensitivity(sensitivity);
  }

  /**
   * Convenience wrapper function that simply returns {@code frame().scalingSensitivity()}
   *
   * @see remixlab.dandelion.core.GenericFrame#scalingSensitivity()
   */
  public final float scalingSensitivity() {
    return frame().scalingSensitivity();
  }

  /**
   * Convenience wrapper function that simply calls
   * {@code frame().setScalingSensitivity(sensitivity)}
   *
   * @see remixlab.dandelion.core.GenericFrame#setScalingSensitivity(float)
   */
  public final void setScalingSensitivity(float sensitivity) {
    frame().setScalingSensitivity(sensitivity);
  }

  /**
   * Returns the Eye position, defined in the world coordinate system.
   * <p>
   * Use {@link #setPosition(Vec)} to set the Eye position. Other convenient methods are
   * showEntireScene() or fitSphere(). Actually returns
   * {@link remixlab.dandelion.geom.Frame#position()}.
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
   * Set using {@link #setUpVector(Vec)} or {@link #setOrientation(Rotation)}. It is
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
   * @see #setOrientation(Rotation)
   */
  public abstract void setUpVector(Vec up, boolean noMove);

  /**
   * Returns the normalized right vector of the Eye, defined in the world coordinate
   * system.
   * <p>
   * This vector lies in the Eye horizontal plane, directed along the X axis (orthogonal
   * to {@link #upVector()} and to {@link #viewDirection()}. Set using
   * {@link #setUpVector(Vec)}, {@link #lookAt(Vec)} or {@link #setOrientation(Rotation)}.
   * <p>
   * Simply returns {@code frame().xAxis()}.
   */
  public Vec rightVector() {
    return frame().xAxis();
  }

  /**
   * Sets the Eye {@link #orientation()}, defined in the world coordinate system.
   */
  public abstract void setOrientation(Rotation q);

  /**
   * Returns the radius of the scene observed by the Eye in scene (world) units.
   * <p>
   * In the case of a 3D Eye (a Camera) you need to provide such an approximation of the
   * scene dimensions so that the it can adapt its
   * {@link remixlab.dandelion.core.Camera#zNear()} and
   * {@link remixlab.dandelion.core.Camera#zFar()} values. See the {@link #sceneCenter()}
   * documentation.
   * <p>
   * Note that {@link remixlab.dandelion.core.AbstractScene#radius()} (resp.
   * {@link remixlab.dandelion.core.AbstractScene#setRadius(float)} simply call this
   * method on its associated Eye.
   *
   * @see #setSceneBoundingBox(Vec, Vec)
   */
  public float sceneRadius() {
    return scnRadius;

  }

  /**
   * Sets the {@link #sceneRadius()} value in scene (world) units. Negative values are
   * ignored. It also sets {@link #flySpeed()} to 1% of {@link #sceneRadius()}
   * <p>
   * <b>Attention:</b> 3d Camera also sets
   * {@link remixlab.dandelion.core.Camera#focusDistance()} to
   * {@code sceneRadius() / tan(fieldOfView()/2)}.
   */
  public void setSceneRadius(float radius) {
    if (radius <= 0.0f) {
      System.out.println("Warning: Scene radius must be positive - Ignoring value");
      return;
    }
    scnRadius = radius;
    setFlySpeed(0.01f * sceneRadius());
    for (Grabber mg : gScene.motionAgent().grabbers()) {
      if (mg instanceof GenericFrame)
        ((GenericFrame) mg).setFlySpeed(0.01f * sceneRadius());
    }
  }

  /**
   * Similar to {@link #setSceneRadius(float)} and {@link #setSceneCenter(Vec)}, but the
   * scene limits are defined by a (world axis aligned) bounding box.
   */
  public abstract void setSceneBoundingBox(Vec min, Vec max);

  // 11. FLYSPEED

  /**
   * Returns the fly speed of the Eye.
   * <p>
   * Simply returns {@code frame().flySpeed()}. See the
   * {@link remixlab.dandelion.core.GenericFrame#flySpeed()} documentation. This value is
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
   * The {@link #sceneCenter()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #sceneCenter()} is set).
   */
  public abstract boolean setSceneCenterFromPixel(Point pixel);

  public boolean setSceneCenterFromPixel(float x, float y) {
    return setSceneCenterFromPixel(new Point(x, y));
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
   * @see remixlab.dandelion.core.Camera#zNear()
   * @see remixlab.dandelion.core.Camera#zFar()
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
   * Sets the {@link #anchor()}, defined in the world coordinate system.
   */
  public void setAnchor(Vec refP) {
    anchorPnt = refP;
    if (gScene.is2D())
      anchorPnt.setZ(0);
  }

  /**
   * The {@link #anchor()} is set to the point located under {@code pixel} on screen.
   * <p>
   * 2D windows always returns true.
   * <p>
   * 3D Cameras returns {@code true} if a point was found under {@code pixel} and
   * {@code false} if none was found (in this case no {@link #anchor()} is set).
   */
  public abstract boolean setAnchorFromPixel(Point pixel);

  public boolean setAnchorFromPixel(float x, float y) {
    return setAnchorFromPixel(new Point(x, y));
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
   * @see remixlab.dandelion.core.Camera#setFOVToFitScene()
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
   * {@link remixlab.dandelion.core.MatrixHelper#loadProjection()}.
   *
   * @see #getView(Mat, boolean)
   * @see remixlab.dandelion.core.MatrixHelper#loadProjection()
   * @see remixlab.dandelion.core.MatrixHelper#loadModelView()
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
   * Computes the projection matrix associated with the Eye.
   * <p>
   * If Eye is a 3D PERSPECTIVE Camera, defines a projection matrix using the
   * {@link remixlab.dandelion.core.Camera#fieldOfView()}, {@link #aspectRatio()},
   * {@link remixlab.dandelion.core.Camera#zNear()} and
   * {@link remixlab.dandelion.core.Camera#zFar()} parameters. If Eye is a 3D ORTHOGRAPHIC
   * Camera, the frustum's width and height are set using
   * {@link #getBoundaryWidthHeight()}. Both types use
   * {@link remixlab.dandelion.core.Camera#zNear()} and
   * {@link remixlab.dandelion.core.Camera#zFar()} to place clipping planes. These values
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
  public abstract void computeProjection();

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
   * Simply returns {@code 1} which is valid for 2d Windows. Value is different for ortho
   * Cameras and thus the method is overridden by the camera class.
   *
   * @see #getBoundaryWidthHeight(float[])
   */
  public float rescalingOrthoFactor() {
    return 1.0f;
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
  public abstract void computeView();

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
   * with {@link remixlab.dandelion.core.MatrixHelper#loadModelView()}.
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
   * Sets the Eye {@link #position()} and {@link #orientation()} from an OpenGL-like View
   * matrix.
   * <p>
   * After this method has been called, {@link #getView()} returns a matrix equivalent to
   * {@code mv}. Only the {@link #position()} and {@link #orientation()} of the Eye are
   * modified.
   */
  public abstract void fromView(Mat mv, boolean recompute);

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
   * Use {@link remixlab.dandelion.core.AbstractScene#projectedCoordinatesOf(Vec)} which
   * is simpler and has been optimized by caching the Projection x View matrix.
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
   * The {@code src.x} and {@code src.y} input values are expressed in pixels, (0,0) being
   * the upper left corner of the window. The {@code src.z} is a depth value ranging in
   * [0..1] (near and far plane respectively). In 3D Note that {@code src.z} is not a
   * linear interpolation between {@link remixlab.dandelion.core.Camera#zNear()} and
   * {@link remixlab.dandelion.core.Camera#zFar()};
   * {@code src.z = zFar() / (zFar() - zNear()) * (1.0f - zNear() / z);} where {@code z}
   * is the distance from the point you project to the camera, along the
   * {@link #viewDirection()} . See the {@code gluUnProject} man page for details.
   * <p>
   * The result is expressed in the {@code frame} coordinate system. When {@code frame} is
   * {@code null}, the result is expressed in the world coordinates system. The possible
   * {@code frame} hierarchy (i.e., when
   * {@link remixlab.dandelion.geom.Frame#referenceFrame()} is non-null) is taken into
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
    if (Util.zero(out[3]))
      return false;

    out[0] /= out[3];
    out[1] /= out[3];
    out[2] /= out[3];

    objCoordinate[0] = out[0];
    objCoordinate[1] = out[1];
    objCoordinate[2] = out[2];

    return true;
  }

  // 7. KEYFRAMED PATHS

  /**
   * Returns the eye {@code <id, KeyFrameInterpolator>} map.
   *
   * @see #keyFrameInterpolatorArray()
   * @see #keyFrameInterpolatorList()
   */
  public HashMap<Integer, KeyFrameInterpolator> keyFrameInterpolatorMap() {
    return kfi;
  }

  /**
   * Returns the eye {@code paths} as an array.
   *
   * @see #keyFrameInterpolatorMap()
   * @see #keyFrameInterpolatorList()
   */
  public KeyFrameInterpolator[] keyFrameInterpolatorArray() {
    return kfi.values().toArray(new KeyFrameInterpolator[0]);
  }

  /**
   * Returns the eye {@code paths} as a list.
   *
   * @see #keyFrameInterpolatorArray()
   * @see #keyFrameInterpolatorMap()
   */
  public List<KeyFrameInterpolator> keyFrameInterpolatorList() {
    return Arrays.asList(keyFrameInterpolatorArray());
  }

  /**
   * Returns the KeyFrameInterpolator that defines the Eye path number {@code key}.
   * <p>
   * The returned KeyFrameInterpolator may be null (if no path is defined for key
   * {@code key}).
   */
  public KeyFrameInterpolator keyFrameInterpolator(int key) {
    return kfi.get(key);
  }

  /**
   * Sets the KeyFrameInterpolator that defines the Eye path of index {@code key}.
   */
  public void setKeyFrameInterpolator(int key, KeyFrameInterpolator keyFInterpolator) {
    if (kfi.get(key) != null) {
      deletePath(key);
    }
    if (keyFInterpolator != null) {
      if (frame() != keyFInterpolator.frame())
        keyFInterpolator.setFrame(frame());
      if (keyFInterpolator.gScene != gScene) {
        keyFInterpolator.gScene = gScene;
        for (int i = 0; i < keyFInterpolator.numberOfKeyFrames(); ++i)
          keyFInterpolator.keyFrame(i).gScene = gScene;
      }
      kfi.put(key, keyFInterpolator);
      System.out.println("Path " + key + " set");
    } else
      deletePath(key);
  }

  /**
   * Adds the current Eye {@link #position()} and {@link #orientation()} as a keyFrame to
   * path {@code key}.
   * <p>
   * This method can also be used if you simply want to save an Eye point of view (a path
   * made of a single keyFrame). Use {@link #playPath(int)} to make the Eye play the
   * keyFrame path (resp. restore the point of view). Use {@link #deletePath(int)} to
   * clear the path.
   * <p>
   * The default keyboard shortcuts for this method are keys [1-5].
   * <p>
   * If you use directly this method and the {@link #keyFrameInterpolator(int)} does not
   * exist, a new one is created.
   */
  public void addKeyFrameToPath(int key) {
    boolean info = true;
    if (!kfi.containsKey(key)) {
      setKeyFrameInterpolator(key, new KeyFrameInterpolator(gScene, frame()));
      System.out.println("Position " + key + " saved");
      info = false;
    }

    GenericFrame keyFrame = frame().detach();
    keyFrame.setPickingPrecision(GenericFrame.PickingPrecision.FIXED);
    keyFrame.setGrabsInputThreshold(AbstractScene.platform() == Platform.PROCESSING_ANDROID ? 50 : 20);
    if (gScene.pathsVisualHint())
      gScene.inputHandler().addGrabber(keyFrame);
    kfi.get(key).addKeyFrame(keyFrame);

    if (info)
      System.out.println("Path " + key + ", position " + kfi.get(key).numberOfKeyFrames() + " added");
  }

  protected void detachPaths() {
    for (int key : keyFrameInterpolatorMap().keySet())
      detachPath(key);
  }

  /**
   * Removes all the Frames from all the pools of the agents registered at the
   * {@link remixlab.dandelion.core.AbstractScene#inputHandler()}.
   *
   * @see #attachPath(int)
   */
  protected void detachPath(int key) {
    if (kfi.containsKey(key)) {
      KeyFrameInterpolator k = kfi.get(key);
      for (int i = 0; i < k.keyFrames().size(); ++i)
        gScene.inputHandler().removeGrabber(k.keyFrames().get(i).frame());
      // Doesn't work since branch is already detached, i.e., frame is not reachable
      // gScene.pruneBranch(k.keyFrames().get(i).frame());
    }
  }

  protected void attachPaths() {
    for (int key : keyFrameInterpolatorMap().keySet())
      attachPath(key);
  }

  /**
   * Re-adds all the Frames to all the pools of the agents registered at the
   * {@link remixlab.dandelion.core.AbstractScene#inputHandler()}.
   *
   * @see #detachPath(int)
   */
  protected void attachPath(int key) {
    if (kfi.containsKey(key)) {
      KeyFrameInterpolator k = kfi.get(key);
      for (int i = 0; i < k.keyFrames().size(); ++i)
        gScene.inputHandler().addGrabber(k.keyFrames().get(i).frame());
    }
  }

  /**
   * Makes the Eye follow the path of keyFrameInterpolator() number {@code key}.
   * <p>
   * If the interpolation is started, it stops it instead.
   * <p>
   * This method silently ignores undefined (empty) paths (see keyFrameInterpolator()).
   * <p>
   * The default keyboard shortcuts for this method are keys [1-5].
   */
  public void playPath(int key) {
    if (kfi.containsKey(key)) {
      if (kfi.get(key).interpolationStarted()) {
        kfi.get(key).stopInterpolation();
        System.out.println("Path " + key + " stopped");
      } else {
        if (anyInterpolationStarted())
          stopInterpolations();
        kfi.get(key).startInterpolation();
        if (kfi.get(key).numberOfKeyFrames() > 1)
          System.out.println("Path " + key + " started");
        else
          System.out.println("Position " + key + " restored");
      }
    }
  }

  /**
   * Deletes the {@link #keyFrameInterpolator(int)} of index {@code key}.
   */
  public void deletePath(int key) {
    if (kfi.containsKey(key)) {
      kfi.get(key).stopInterpolation();
      detachPath(key);
      kfi.get(key).deletePath();
      kfi.remove(key);
      System.out.println("Path " + key + " deleted");
    }
  }

  /**
   * Resets the path of the {@link #keyFrameInterpolator(int)} number {@code key}.
   * <p>
   * If this path is not being played (see {@link #playPath(int)} and
   * {@link remixlab.dandelion.core.KeyFrameInterpolator#interpolationStarted()} ), resets
   * it to its starting position (see
   * {@link remixlab.dandelion.core.KeyFrameInterpolator#resetInterpolation()}). If the
   * path is played, simply stops interpolation.
   */
  public void resetPath(int key) {
    if (kfi.containsKey(key)) {
      if ((kfi.get(key).interpolationStarted()))
        kfi.get(key).stopInterpolation();
      else {
        kfi.get(key).resetInterpolation();
        kfi.get(key).interpolateAtTime(kfi.get(key).interpolationTime());
      }
    }
  }

  /**
   * Returns {@code true} if any interpolation associated with this Eye is currently being
   * performed (and {@code false} otherwise).
   */
  public boolean anyInterpolationStarted() {
    Iterator<Integer> itrtr = kfi.keySet().iterator();
    while (itrtr.hasNext()) {
      Integer key = itrtr.next();
      if (kfi.get(key).interpolationStarted())
        return true;
    }
    return interpolationKfi.interpolationStarted();
  }

  /**
   * Stops all interpolations currently being performed associated with this Eye.
   */
  public void stopInterpolations() {
    Iterator<Integer> itrtr = kfi.keySet().iterator();
    while (itrtr.hasNext()) {
      Integer key = itrtr.next();
      if (kfi.get(key).interpolationStarted())
        kfi.get(key).stopInterpolation();
    }
    if (interpolationKfi.interpolationStarted())
      interpolationKfi.stopInterpolation();
  }

  /**
   * Convenience function that in 2D simply returns
   * {@code computeFrustumPlanesCoefficients(new float [4][3])} and in 3D
   * {@code computeFrustumPlanesCoefficients(new float [6][4])}.
   * <p>
   * <b>Attention:</b> You should not call this method explicitly, unless you need the
   * frustum equations to be updated only occasionally (rare). Use
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations(float[][])
   */
  public abstract float[][] computeBoundaryEquations();

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
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()} which
   * automatically update the frustum equations every frame instead.
   *
   * @see #computeBoundaryEquations()
   */
  public abstract float[][] computeBoundaryEquations(float[][] coef);

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
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()} which
   * automatically update the boundary equations every frame instead.
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
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
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
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
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()}).
   *
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
   */
  public float distanceToBoundary(int index, Vec pos) {
    if (!gScene.areBoundaryEquationsEnabled())
      System.out.println("The viewpoint boundary equations (needed by distanceToBoundary) may be outdated. Please "
          + "enable automatic updates of the equations in your PApplet.setup " + "with Scene.enableBoundaryEquations()");
    Vec myVec = new Vec(fpCoefficients[index][0], fpCoefficients[index][1], fpCoefficients[index][2]);
    return Vec.dot(pos, myVec) - fpCoefficients[index][3];
  }

  /**
   * Returns {@code true} if {@code point} is visible (i.e, lies within the Eye boundary)
   * and {@code false} otherwise.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #ballVisibility(Vec, float)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
   */
  public abstract boolean isPointVisible(Vec point);

  /**
   * Returns {@link remixlab.dandelion.core.Eye.Visibility#VISIBLE},
   * {@link remixlab.dandelion.core.Eye.Visibility#INVISIBLE}, or
   * {@link remixlab.dandelion.core.Eye.Visibility#SEMIVISIBLE}, depending whether the
   * sphere (of radius {@code radius} and center {@code center}) is visible, invisible, or
   * semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #boxVisibility(Vec, Vec)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
   */
  public abstract Visibility ballVisibility(Vec center, float radius);

  /**
   * Returns {@link remixlab.dandelion.core.Eye.Visibility#VISIBLE},
   * {@link remixlab.dandelion.core.Eye.Visibility#INVISIBLE}, or
   * {@link remixlab.dandelion.core.Eye.Visibility#SEMIVISIBLE}, depending whether the
   * axis aligned box (defined by corners {@code p1} and {@code p2}) is visible,
   * invisible, or semi-visible, respectively.
   * <p>
   * <b>Attention:</b> The Eye boundary plane equations should be updated before calling
   * this method. You may compute them explicitly (by calling
   * {@link #computeBoundaryEquations()} ) or enable them to be automatic updated in your
   * Scene setup (with
   * {@link remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()}).
   *
   * @see #distanceToBoundary(int, Vec)
   * @see #isPointVisible(Vec)
   * @see #ballVisibility(Vec, float)
   * @see #computeBoundaryEquations()
   * @see #updateBoundaryEquations()
   * @see #getBoundaryEquations()
   * @see remixlab.dandelion.core.AbstractScene#enableBoundaryEquations()
   */
  public abstract Visibility boxVisibility(Vec p1, Vec p2);

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
   * {@code Vec v = Vec.add(sceneCenter(), Vec.mult(upVector(), 20 * sceneToPixelRatio(sceneCenter())));}
   * <br>
   * {@code vertex(v.x, v.y, v.z);}<br>
   * {@code endShape();}<br>
   */
  public abstract float sceneToPixelRatio(Vec position);

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
   * Smoothly moves the Eye so that the rectangular screen region defined by
   * {@code rectangle} (pixel units, with origin in the upper left corner) fits the
   * screen.
   * <p>
   * The Eye is translated (its {@link #orientation()} is unchanged) so that
   * {@code rectangle} is entirely visible. Since the pixel coordinates only define a
   * <i>boundary</i> in 3D, it's the intersection of this boundary with a plane
   * (orthogonal to the {@link #viewDirection()} and passing through the
   * {@link #sceneCenter()}) that is used to define the 3D rectangle that is eventually
   * fitted.
   *
   * @see #fitScreenRegion(Rect)
   */
  public void interpolateToZoomOnRegion(Rect rectangle) {
    if (anyInterpolationStarted())
      stopInterpolations();

    interpolationKfi.deletePath();
    interpolationKfi.addKeyFrame(frame().detach());
    GenericFrame originalFrame = frame();
    GenericFrame tempFrame = frame().detach();
    replaceFrame(tempFrame);
    fitScreenRegion(rectangle);
    setFrame(originalFrame);
    interpolationKfi.addKeyFrame(tempFrame);
    interpolationKfi.startInterpolation();
  }

  /**
   * Interpolates the Eye on a one second KeyFrameInterpolator path so that the entire
   * scene fits the screen at the end.
   * <p>
   * The scene is defined by its {@link #sceneCenter()} and its {@link #sceneRadius()}.
   * See {@link #showEntireScene()}.
   * <p>
   * The {@link #orientation()} of the Eye is not modified.
   *
   * @see #interpolateToZoomOnPixel(Point)
   */
  public void interpolateToFitScene() {
    if (anyInterpolationStarted())
      stopInterpolations();

    interpolationKfi.deletePath();
    interpolationKfi.addKeyFrame(frame().detach());
    GenericFrame originalFrame = frame();
    GenericFrame tempFrame = frame().detach();
    replaceFrame(tempFrame);
    showEntireScene();
    setFrame(originalFrame);
    interpolationKfi.addKeyFrame(tempFrame);
    interpolationKfi.startInterpolation();
  }

  /**
   * Convenience function that simply calls {@code interpolateTo(fr, 1)}.
   *
   * @see #interpolateTo(GenericFrame, float)
   */
  public void interpolateTo(GenericFrame fr) {
    interpolateTo(fr, 1);
  }

  /**
   * Smoothly interpolates the Eye on a KeyFrameInterpolator path so that it goes to
   * {@code fr}.
   * <p>
   * {@code fr} is expressed in world coordinates. {@code duration} tunes the
   * interpolation speed.
   *
   * @see #interpolateTo(GenericFrame)
   * @see #interpolateToFitScene()
   * @see #interpolateToZoomOnPixel(Point)
   */
  public void interpolateTo(GenericFrame fr, float duration) {
    if (anyInterpolationStarted())
      stopInterpolations();

    interpolationKfi.deletePath();
    interpolationKfi.addKeyFrame(frame().detach());
    interpolationKfi.addKeyFrame(fr, duration);
    interpolationKfi.startInterpolation();
  }

  /**
   * Moves the Eye so that the ball defined by {@code center} and {@code radius} is
   * visible and fits the window.
   * <p>
   * In 3D the Camera is simply translated along its {@link #viewDirection()} so that the
   * sphere fits the screen. Its {@link #orientation()} and its
   * {@link remixlab.dandelion.core.Camera#fieldOfView()} are unchanged. You should
   * therefore orientate the Camera before you call this method.
   *
   * @see #lookAt(Vec)
   * @see #setOrientation(Rotation)
   * @see #setUpVector(Vec, boolean)
   */
  public abstract void fitBall(Vec center, float radius);

  /**
   * Moves the Eye so that the (world axis aligned) bounding box ({@code min} ,
   * {@code max}) is entirely visible, using {@link #fitBall(Vec, float)}.
   */
  public abstract void fitBoundingBox(Vec min, Vec max);

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
  public abstract void fitScreenRegion(Rect rectangle);

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
   * {@link remixlab.dandelion.core.Camera#fieldOfView()}) is (are) unchanged.
   * <p>
   * Simply projects the current position on a line passing through {@link #sceneCenter()}
   * .
   *
   * @see #showEntireScene()
   */
  public void centerScene() {
    frame().projectOnLine(sceneCenter(), viewDirection());
  }

  /**
   * Returns the normalized view direction of the Eye, defined in the world coordinate
   * system. This corresponds to the negative Z axis of the {@link #frame()} (
   * {@code frame().inverseTransformOf(new Vec(0.0f, 0.0f, -1.0f))} ) whih in 2D always is
   * (0,0,-1)
   * <p>
   * In 3D change this value using
   * {@link remixlab.dandelion.core.Camera#setViewDirection(Vec)}, {@link #lookAt(Vec)} or
   * {@link #setOrientation(Rotation)} . It is orthogonal to {@link #upVector()} and to
   * {@link #rightVector()}.
   */
  public Vec viewDirection() {
    return new Vec(0, 0, (frame().zAxis().z() > 0) ? -1 : 1);
  }

  /**
   * 2D Windows return the postion. 3D Cameras return a point defined in the world
   * coordinate system where the camera is pointing at (just in front of
   * {@link #viewDirection()}). Useful for setting the Processing camera() which uses a
   * similar approach of that found in gluLookAt.
   *
   * @see #lookAt(Vec)
   */
  public abstract Vec at();

  /**
   * Makes the Eye smoothly zoom on the
   * {@link remixlab.dandelion.core.Camera#pointUnderPixel(Point)} {@code pixel} and
   * returns the world coordinates of the
   * {@link remixlab.dandelion.core.Camera#pointUnderPixel(Point)}.
   * <p>
   * In 3D nothing happens if no
   * {@link remixlab.dandelion.core.Camera#pointUnderPixel(Point)} is found. Otherwise a
   * KeyFrameInterpolator is created that animates the Camera on a one second path that
   * brings the Camera closer to the point under {@code pixel}.
   *
   * @see #interpolateToFitScene()
   */
  public abstract void interpolateToZoomOnPixel(Point pixel);

  public void interpolateToZoomOnPixel(float x, float y) {
    interpolateToZoomOnPixel(new Point(x, y));
  }
}
