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
import remixlab.fpstiming.TimingTask;
import remixlab.util.Copyable;
import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;
import remixlab.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A keyFrame Catmull-Rom Frame interpolator.
 * <p>
 * A KeyFrameInterpolator holds keyFrames (that define a path) and, optionally, a
 * reference to a Frame of your application (which will be interpolated). In this case,
 * when the user {@link #startInterpolation()}, the KeyFrameInterpolator regularly updates
 * the {@link #frame()} position, orientation and magnitude along the path.
 * <p>
 * Here is a typical utilization example (see also ProScene's FrameInterpolation and
 * CameraInterpolation examples):
 * <p>
 * {@code //init() should look like:}<br>
 * {@code // The KeyFrameInterpolator kfi is given the Frame that it will drive over time.}
 * <br>
 * {@code myFrame = new Frame());}<br>
 * {@code kfi = new KeyFrameInterpolator( myScene, myFrame );}<br>
 * {@code // With an anonymous frame would look like this: kfi = new KeyFrameInterpolator( myScene );}
 * <br>
 * {@code kfi.addKeyFrame( new Frame( new Vec(1,0,0), new Quat() ) );}<br>
 * {@code kfi.addKeyFrame( new Frame( new Vec(2,1,0), new Quat() ) );}<br>
 * {@code // ...and so on for all the keyFrames.}<br>
 * {@code kfi.startInterpolation();}<br>
 * <p>
 * {@code //mainDrawingLoop() should look like:}<br>
 * {@code scene.pushModelView();}<br>
 * {@code kfi.frame().applyTransformation(this);}<br>
 * {@code // Draw your object here. Its position, orientation and magnitude are interpolated.}
 * <br>
 * {@code scene.popModelView();}<br>
 * <p>
 * The keyFrames are defined by a Frame and a time, expressed in seconds. The time has to
 * be monotonously increasing over keyFrames. When {@link #interpolationSpeed()} equals
 * 1.0 (default value), these times correspond to actual user's seconds during
 * interpolation (provided that your main loop is fast enough). The interpolation is then
 * real-time: the keyFrames will be reached at their {@link #keyFrameTime(int)}.
 * <p>
 * <h3>Interpolation details</h3>
 * <p>
 * When the user {@link #startInterpolation()}, a timer is started which will update the
 * {@link #frame()}'s position, orientation and magnitude every
 * {@link #interpolationPeriod()} milliseconds. This update increases the
 * {@link #interpolationTime()} by {@link #interpolationPeriod()} *
 * {@link #interpolationSpeed()} milliseconds.
 * <p>
 * Note that this mechanism ensures that the number of interpolation steps is constant and
 * equal to the total path {@link #duration()} divided by the
 * {@link #interpolationPeriod()} * {@link #interpolationSpeed()}. This is especially
 * useful for benchmarking or movie creation (constant number of snapshots).
 * <p>
 * The interpolation is stopped when {@link #interpolationTime()} is greater than the
 * {@link #lastTime()} (unless loopInterpolation() is {@code true}).
 * <p>
 * Note that an Eye has {@link remixlab.dandelion.core.Eye#keyFrameInterpolator(int)},
 * that can be used to drive the Eye along a path.
 * <p>
 * <b>Attention:</b> If a Constraint is attached to the {@link #frame()} (see
 * {@link remixlab.dandelion.geom.Frame#constraint()}), it should be deactivated before
 * {@link #interpolationStarted()}, otherwise the interpolated motion (computed as if
 * there was no constraint) will probably be erroneous.
 */
public class KeyFrameInterpolator implements Copyable {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(keyFrameList).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    KeyFrameInterpolator other = (KeyFrameInterpolator) obj;
    return new EqualsBuilder().append(keyFrameList, other.keyFrameList).isEquals();
  }

  /**
   * Internal protected abstract base class for 2d and 3d KeyFrames
   */
  protected abstract class KeyFrame implements Copyable {
    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37).append(frame()).append(time()).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null)
        return false;
      if (obj == this)
        return true;
      if (obj.getClass() != getClass())
        return false;

      KeyFrame other = (KeyFrame) obj;
      return new EqualsBuilder().append(frame(), other.frame()).append(time(), other.time()).isEquals();
    }

    protected Vec tgPVec;
    protected float tm;
    protected GenericFrame frm;

    KeyFrame(GenericFrame fr, float t) {
      tm = t;
      frm = fr;
    }

    protected KeyFrame(KeyFrame otherKF) {
      this.tm = otherKF.tm;
      this.frm = otherKF.frm.get();
    }

    Vec position() {
      return frame().position();
    }

    Rotation orientation() {
      return frame().orientation();
    }

    float magnitude() {
      return frame().magnitude();
    }

    float time() {
      return tm;
    }

    GenericFrame frame() {
      return frm;
    }

    Vec tgP() {
      return tgPVec;
    }

    abstract void computeTangent(KeyFrame prev, KeyFrame next);
  }

  /**
   * 3D KeyFrame internal class.
   */
  protected class KeyFrame3D extends KeyFrame {
    protected Quat tgQuat;

    KeyFrame3D(GenericFrame fr, float t) {
      super(fr, t);
    }

    protected KeyFrame3D(KeyFrame3D other) {
      super(other);
    }

    @Override
    public KeyFrame3D get() {
      return new KeyFrame3D(this);
    }

    Quat tgQ() {
      return tgQuat;
    }

    @Override
    void computeTangent(KeyFrame prev, KeyFrame next) {
      tgPVec = Vec.multiply(Vec.subtract(next.position(), prev.position()), 0.5f);
      tgQuat = Quat.squadTangent((Quat) prev.orientation(), (Quat) orientation(), (Quat) next.orientation());
    }
  }

  /**
   * 2D KeyFrame internal class.
   */
  protected class KeyFrame2D extends KeyFrame {
    KeyFrame2D(GenericFrame fr, float t) {
      super(fr, t);
    }

    protected KeyFrame2D(KeyFrame2D other) {
      super(other);
    }

    @Override
    public KeyFrame2D get() {
      return new KeyFrame2D(this);
    }

    @Override
    void computeTangent(KeyFrame prev, KeyFrame next) {
      tgPVec = Vec.multiply(Vec.subtract(next.position(), prev.position()), 0.5f);
    }
  }

  private long lUpdate;
  protected List<KeyFrame> keyFrameList;
  private ListIterator<KeyFrame> currentFrame0;
  private ListIterator<KeyFrame> currentFrame1;
  private ListIterator<KeyFrame> currentFrame2;
  private ListIterator<KeyFrame> currentFrame3;
  protected List<Frame> path;
  // A s s o c i a t e d f r a m e
  private Frame mainFrame;

  // R h y t h m
  private TimingTask interpolationTimerTask;
  private int period;
  private float interpolationTm;
  private float interpolationSpd;
  private boolean interpolationStrt;

  // M i s c
  private boolean lpInterpolation;

  // C a c h e d v a l u e s a n d f l a g s
  private boolean pathIsValid;
  private boolean valuesAreValid;
  private boolean currentFrmValid;
  private boolean splineCacheIsValid;
  private Vec pv1, pv2;
  // Option 2 (interpolate magnitude using a spline)
  // private Vec sv1, sv2;

  // S C E N E
  protected AbstractScene gScene;

  /**
   * Convenience constructor that simply calls {@code this(scn, new Frame())}.
   * <p>
   * Creates an anonymous {@link #frame()} to be interpolated by this
   * KeyFrameInterpolator.
   *
   * @see #KeyFrameInterpolator(AbstractScene, Frame)
   */
  public KeyFrameInterpolator(AbstractScene scn) {
    this(scn, new Frame());
  }

  /**
   * Creates a KeyFrameInterpolator, with {@code frame} as associated {@link #frame()}.
   * <p>
   * The {@link #frame()} can be set or changed using {@link #setFrame(Frame)}.
   * <p>
   * {@link #interpolationTime()}, {@link #interpolationSpeed()} and
   * {@link #interpolationPeriod()} are set to their default values.
   */
  public KeyFrameInterpolator(AbstractScene scn, Frame frame) {
    gScene = scn;
    keyFrameList = new ArrayList<KeyFrame>();
    path = new ArrayList<Frame>();
    mainFrame = null;
    period = 40;
    interpolationTm = 0.0f;
    interpolationSpd = 1.0f;
    interpolationStrt = false;
    lpInterpolation = false;
    pathIsValid = false;
    valuesAreValid = true;
    currentFrmValid = false;
    setFrame(frame);

    currentFrame0 = keyFrameList.listIterator();
    currentFrame1 = keyFrameList.listIterator();
    currentFrame2 = keyFrameList.listIterator();
    currentFrame3 = keyFrameList.listIterator();

    interpolationTimerTask = new TimingTask() {
      public void execute() {
        update();
      }
    };
    gScene.registerTimingTask(interpolationTimerTask);
  }

  protected KeyFrameInterpolator(KeyFrameInterpolator otherKFI) {
    this.gScene = otherKFI.gScene;
    this.path = new ArrayList<Frame>();
    ListIterator<Frame> frameIt = otherKFI.path.listIterator();
    while (frameIt.hasNext()) {
      this.path.add(frameIt.next().get());
    }

    this.setFrame(otherKFI.frame());

    this.period = otherKFI.period;
    this.interpolationTm = otherKFI.interpolationTm;
    this.interpolationSpd = otherKFI.interpolationSpd;
    this.interpolationStrt = otherKFI.interpolationStrt;
    this.lpInterpolation = otherKFI.lpInterpolation;
    this.pathIsValid = otherKFI.pathIsValid;
    this.valuesAreValid = otherKFI.valuesAreValid;
    this.currentFrmValid = otherKFI.currentFrmValid;

    this.keyFrameList = new ArrayList<KeyFrame>();

    for (KeyFrame element : otherKFI.keyFrameList) {
      KeyFrame kf = (KeyFrame) element.get();
      this.keyFrameList.add(kf);
    }

    this.currentFrame0 = keyFrameList.listIterator(otherKFI.currentFrame0.nextIndex());
    this.currentFrame1 = keyFrameList.listIterator(otherKFI.currentFrame1.nextIndex());
    this.currentFrame2 = keyFrameList.listIterator(otherKFI.currentFrame2.nextIndex());
    this.currentFrame3 = keyFrameList.listIterator(otherKFI.currentFrame3.nextIndex());

    this.interpolationTimerTask = new TimingTask() {
      public void execute() {
        update();
      }
    };
    gScene.registerTimingTask(interpolationTimerTask);

    this.invalidateValues();
  }

  @Override
  public KeyFrameInterpolator get() {
    return new KeyFrameInterpolator(this);
  }

  /**
   * Returns the scene this object belongs to
   */
  public AbstractScene scene() {
    return gScene;
  }

  /**
   * Internal use. Updates the last frame path was updated. Called by
   * {@link #checkValidity()}.
   */
  protected void checked() {
    lUpdate = AbstractScene.frameCount;
  }

  /**
   * Internal use. Called by {@link #checkValidity()}.
   */
  protected long lastUpdate() {
    return lUpdate;
  }

  /**
   * Sets the {@link #frame()} associated to the KeyFrameInterpolator.
   */
  public void setFrame(Frame f) {
    mainFrame = f;
  }

  /**
   * Returns the associated Frame that is interpolated by the KeyFrameInterpolator.
   * <p>
   * When {@link #interpolationStarted()}, this Frame's position, orientation and
   * magnitude will regularly be updated by a timer, so that they follow the
   * KeyFrameInterpolator path.
   * <p>
   * Set using {@link #setFrame(Frame)} or with the KeyFrameInterpolator constructor.
   */
  public Frame frame() {
    return mainFrame;
  }

  /**
   * Returns the number of keyFrames used by the interpolation. Use
   * {@link #addKeyFrame(GenericFrame)} to add new keyFrames.
   */
  public int numberOfKeyFrames() {
    return keyFrameList.size();
  }

  /**
   * Returns the current interpolation time (in seconds) along the KeyFrameInterpolator
   * path.
   * <p>
   * This time is regularly updated when {@link #interpolationStarted()}. Can be set
   * directly with {@link #setInterpolationTime(float)} or
   * {@link #interpolateAtTime(float)}.
   */
  public float interpolationTime() {
    return interpolationTm;
  }

  /**
   * Returns the current interpolation speed.
   * <p>
   * Default value is 1.0f, which means {@link #keyFrameTime(int)} will be matched during
   * the interpolation (provided that your main loop is fast enough).
   * <p>
   * A negative value will result in a reverse interpolation of the keyFrames.
   *
   * @see #interpolationPeriod()
   */
  public float interpolationSpeed() {
    return interpolationSpd;
  }

  /**
   * Returns the current interpolation period, expressed in milliseconds. The update of
   * the {@link #frame()} state will be done by a timer at this period when
   * {@link #interpolationStarted()}.
   * <p>
   * This period (multiplied by {@link #interpolationSpeed()}) is added to the
   * {@link #interpolationTime()} at each update, and the {@link #frame()} state is
   * modified accordingly (see {@link #interpolateAtTime(float)}). Default value is 40
   * milliseconds.
   *
   * @see #setInterpolationPeriod(int)
   */
  public int interpolationPeriod() {
    return period;
  }

  /**
   * Returns {@code true} when the interpolation is played in an infinite loop.
   * <p>
   * When {@code false} (default), the interpolation stops when
   * {@link #interpolationTime()} reaches {@link #firstTime()} (with negative
   * {@link #interpolationSpeed()}) or {@link #lastTime()}.
   * <p>
   * {@link #interpolationTime()} is otherwise reset to {@link #firstTime()} (+
   * {@link #interpolationTime()} - {@link #lastTime()}) (and inversely for negative
   * {@link #interpolationSpeed()}) and interpolation continues.
   */
  public boolean loopInterpolation() {
    return lpInterpolation;
  }

  /**
   * Sets the {@link #interpolationTime()}.
   * <p>
   * <b>Attention:</b> The {@link #frame()} state is not affected by this method. Use this
   * function to define the starting time of a future interpolation (see
   * {@link #startInterpolation()}). Use {@link #interpolateAtTime(float)} to actually
   * interpolate at a given time.
   */
  public void setInterpolationTime(float time) {
    interpolationTm = time;
  }

  ;

  /**
   * Sets the {@link #interpolationSpeed()}. Negative or null values are allowed.
   */
  public void setInterpolationSpeed(float speed) {
    interpolationSpd = speed;
  }

  /**
   * Sets the {@link #interpolationPeriod()}. Should positive.
   */
  public void setInterpolationPeriod(int myPeriod) {
    if (myPeriod > 0)
      period = myPeriod;
  }

  /**
   * Convenience function that simply calls {@code setLoopInterpolation(true)}.
   */
  public void setLoopInterpolation() {
    setLoopInterpolation(true);
  }

  /**
   * Sets the {@link #loopInterpolation()} value.
   */
  public void setLoopInterpolation(boolean loop) {
    lpInterpolation = loop;
  }

  /**
   * Returns {@code true} when the interpolation is being performed. Use
   * {@link #startInterpolation()}, {@link #stopInterpolation()} or
   * {@link #toggleInterpolation()} to modify this state.
   */
  public boolean interpolationStarted() {
    return interpolationStrt;
  }

  /**
   * Calls {@link #startInterpolation()} or {@link #stopInterpolation()}, depending on
   * {@link #interpolationStarted()} .
   */
  public void toggleInterpolation() {
    if (interpolationStarted())
      stopInterpolation();
    else
      startInterpolation();
  }

  /**
   * Updates {@link #frame()} state according to current {@link #interpolationTime()}.
   * Then adds {@link #interpolationPeriod()}* {@link #interpolationSpeed()} to
   * {@link #interpolationTime()}.
   * <p>
   * This internal method is called by a timer when {@link #interpolationStarted()}. It
   * can be used for debugging purpose. {@link #stopInterpolation()} is called when
   * {@link #interpolationTime()} reaches {@link #firstTime()} or {@link #lastTime()},
   * unless {@link #loopInterpolation()} is {@code true}.
   */
  protected void update() {
    interpolateAtTime(interpolationTime());

    interpolationTm += interpolationSpeed() * interpolationPeriod() / 1000.0f;

    if (interpolationTime() > keyFrameList.get(keyFrameList.size() - 1).time()) {
      if (loopInterpolation())
        setInterpolationTime(
            keyFrameList.get(0).time() + interpolationTm - keyFrameList.get(keyFrameList.size() - 1).time());
      else {
        // Make sure last KeyFrame is reached and displayed
        interpolateAtTime(keyFrameList.get(keyFrameList.size() - 1).time());
        stopInterpolation();
      }
    } else if (interpolationTime() < keyFrameList.get(0).time()) {
      if (loopInterpolation())
        setInterpolationTime(
            keyFrameList.get(keyFrameList.size() - 1).time() - keyFrameList.get(0).time() + interpolationTm);
      else {
        // Make sure first KeyFrame is reached and displayed
        interpolateAtTime(keyFrameList.get(0).time());
        stopInterpolation();
      }
    }
  }

  /**
   * Internal use. Called by {@link #checkValidity()}.
   */
  protected void invalidateValues() {
    valuesAreValid = false;
    pathIsValid = false;
    splineCacheIsValid = false;
  }

  /**
   * Convenience function that simply calls {@code startInterpolation(-1)}.
   *
   * @see #startInterpolation(int)
   */
  public void startInterpolation() {
    startInterpolation(-1);
  }

  /**
   * Starts the interpolation process.
   * <p>
   * A timer is started with an {@link #interpolationPeriod()} period that updates the
   * {@link #frame()}'s position, orientation and magnitude.
   * {@link #interpolationStarted()} will return {@code true} until
   * {@link #stopInterpolation()} or {@link #toggleInterpolation()} is called.
   * <p>
   * If {@code period} is positive, it is set as the new {@link #interpolationPeriod()}.
   * The previous {@link #interpolationPeriod()} is used otherwise (default).
   * <p>
   * If {@link #interpolationTime()} is larger than {@link #lastTime()},
   * {@link #interpolationTime()} is reset to {@link #firstTime()} before interpolation
   * starts (and inversely for negative {@link #interpolationSpeed()}.
   * <p>
   * Use {@link #setInterpolationTime(float)} before calling this method to change the
   * starting {@link #interpolationTime()}.
   * <p>
   * <b>Attention:</b> The keyFrames must be defined (see
   * {@link #addKeyFrame(GenericFrame, float)}) before you startInterpolation(), or else
   * the interpolation will naturally immediately stop.
   */
  public void startInterpolation(int myPeriod) {
    if (myPeriod >= 0)
      setInterpolationPeriod(myPeriod);

    if (!keyFrameList.isEmpty()) {
      if ((interpolationSpeed() > 0.0) && (interpolationTime() >= keyFrameList.get(keyFrameList.size() - 1).time()))
        setInterpolationTime(keyFrameList.get(0).time());
      if ((interpolationSpeed() < 0.0) && (interpolationTime() <= keyFrameList.get(0).time()))
        setInterpolationTime(keyFrameList.get(keyFrameList.size() - 1).time());
      if (keyFrameList.size() > 1)
        interpolationTimerTask.run(interpolationPeriod());
      interpolationStrt = true;
      update();
    }
  }

  /**
   * Stops an interpolation started with {@link #startInterpolation()}. See
   * {@link #interpolationStarted()} and {@link #toggleInterpolation()}.
   */
  public void stopInterpolation() {
    interpolationTimerTask.stop();
    interpolationStrt = false;
  }

  /**
   * Stops the interpolation and resets {@link #interpolationTime()} to the
   * {@link #firstTime()}.
   * <p>
   * If desired, call {@link #interpolateAtTime(float)} after this method to actually move
   * the {@link #frame()} to {@link #firstTime()}.
   */
  public void resetInterpolation() {
    stopInterpolation();
    setInterpolationTime(firstTime());
  }

  /**
   * Appends a new keyFrame to the path.
   * <p>
   * Same as {@link #addKeyFrame(GenericFrame, float)}, except that the
   * {@link #keyFrameTime(int)} is set to the previous {@link #keyFrameTime(int)} plus one
   * second (or 0.0 if there is no previous keyFrame).
   */
  public void addKeyFrame(GenericFrame frame) {
    float time;

    if (keyFrameList.isEmpty())
      time = 0.0f;
    else
      time = keyFrameList.get(keyFrameList.size() - 1).time() + 1.0f;

    addKeyFrame(frame, time);
  }

  /**
   * Appends a new keyFrame to the path, with its associated {@code time} (in seconds).
   * <p>
   * When {@code setRef} is {@code false} the keyFrame is added by value, meaning that the
   * path will use the current {@code frame} state.
   * <p>
   * When {@code setRef} is {@code true} the keyFrame is given as a reference to a Frame,
   * which will be connected to the KeyFrameInterpolator: when {@code frame} is modified,
   * the KeyFrameInterpolator path is updated accordingly. This allows for dynamic paths,
   * where keyFrame can be edited, even during the interpolation. {@code null} frame
   * references are silently ignored. The {@link #keyFrameTime(int)} has to be
   * monotonously increasing over keyFrames.
   */
  public void addKeyFrame(GenericFrame frame, float time) {
    if (frame == null)
      return;

    if (keyFrameList.isEmpty())
      interpolationTm = time;

    if ((!keyFrameList.isEmpty()) && (keyFrameList.get(keyFrameList.size() - 1).time() > time))
      System.out.println("Error in KeyFrameInterpolator.addKeyFrame: time is not monotone");
    else {
      if (gScene.is3D())
        keyFrameList.add(new KeyFrame3D(frame, time));
      else
        keyFrameList.add(new KeyFrame2D(frame, time));
    }

    valuesAreValid = false;
    pathIsValid = false;
    currentFrmValid = false;
    resetInterpolation();
  }

  /**
   * Remove KeyFrame according to {@code index} in the list and
   * {@link #stopInterpolation()} if {@link #interpolationStarted()}. If
   * {@code index < 0 || index >= keyFr.size()} the call is silently ignored.
   */
  public void removeKeyFrame(int index) {
    if (index < 0 || index >= keyFrameList.size())
      return;
    valuesAreValid = false;
    pathIsValid = false;
    currentFrmValid = false;
    if (interpolationStarted())
      stopInterpolation();
    KeyFrame kf = keyFrameList.remove(index);
    gScene.pruneBranch(kf.frm);
    setInterpolationTime(firstTime());
  }

  /**
   * Removes all keyFrames from the path. The {@link #numberOfKeyFrames()} is set to 0.
   */
  public void deletePath() {
    stopInterpolation();
    keyFrameList.clear();
    pathIsValid = false;
    valuesAreValid = false;
    currentFrmValid = false;
  }

  protected void updateModifiedFrameValues() {
    KeyFrame kf;
    KeyFrame prev = keyFrameList.get(0);
    kf = keyFrameList.get(0);

    int index = 1;
    while (kf != null) {
      KeyFrame next = (index < keyFrameList.size()) ? keyFrameList.get(index) : null;
      index++;
      if (next != null)
        kf.computeTangent(prev, next);
      else
        kf.computeTangent(prev, kf);
      prev = kf;
      kf = next;
    }
    valuesAreValid = true;
  }

  protected List<KeyFrame> keyFrames() {
    return keyFrameList;
  }

  /**
   * Calls {@link #updatePath()} and then returns a list of Frames defining the
   * KeyFrameInterpolator path.
   * <p>
   * Use it in your KeyFrameInterpolator path drawing routine.
   *
   * @see remixlab.dandelion.core.AbstractScene#drawPath(KeyFrameInterpolator, int, int, float)
   */
  public List<Frame> path() {
    updatePath();
    return path;
  }

  /**
   * Intenal use. Call {@link #checkValidity()} and if path is not valid recomputes it.
   */
  protected void updatePath() {
    checkValidity();
    if (!pathIsValid) {
      path.clear();
      int nbSteps = 30;

      if (keyFrameList.isEmpty())
        return;

      if (!valuesAreValid)
        updateModifiedFrameValues();

      if (keyFrameList.get(0) == keyFrameList.get(keyFrameList.size() - 1))
        path.add(
            new Frame(keyFrameList.get(0).position(), keyFrameList.get(0).orientation(), keyFrameList.get(0).magnitude()));
      else {
        KeyFrame[] kf = new KeyFrame[4];
        kf[0] = keyFrameList.get(0);
        kf[1] = kf[0];

        int index = 1;
        kf[2] = (index < keyFrameList.size()) ? keyFrameList.get(index) : null;
        index++;
        kf[3] = (index < keyFrameList.size()) ? keyFrameList.get(index) : null;

        while (kf[2] != null) {
          Vec pdiff = Vec.subtract(kf[2].position(), kf[1].position());
          Vec pvec1 = Vec.add(Vec.multiply(pdiff, 3.0f), Vec.multiply(kf[1].tgP(), (-2.0f)));
          pvec1 = Vec.subtract(pvec1, kf[2].tgP());
          Vec pvec2 = Vec.add(Vec.multiply(pdiff, (-2.0f)), kf[1].tgP());
          pvec2 = Vec.add(pvec2, kf[2].tgP());

          for (int step = 0; step < nbSteps; ++step) {
            Frame frame = new Frame();
            float alpha = step / (float) nbSteps;
            frame.setPosition(Vec.add(kf[1].position(),
                Vec.multiply(Vec.add(kf[1].tgP(), Vec.multiply(Vec.add(pvec1, Vec.multiply(pvec2, alpha)), alpha)), alpha)));
            if (gScene.is3D()) {
              frame.setOrientation(
                  Quat.squad((Quat) kf[1].orientation(), ((KeyFrame3D) kf[1]).tgQ(), ((KeyFrame3D) kf[2]).tgQ(),
                      (Quat) kf[2].orientation(), alpha));
            } else {
              // linear interpolation
              float start = kf[1].orientation().angle();
              float stop = kf[2].orientation().angle();
              frame.setOrientation(new Rot(start + (stop - start) * alpha));
            }
            frame.setMagnitude(Util.lerp(kf[1].magnitude(), kf[2].magnitude(), alpha));
            path.add(frame.get());
          }

          // Shift
          kf[0] = kf[1];
          kf[1] = kf[2];
          kf[2] = kf[3];

          index++;
          kf[3] = (index < keyFrameList.size()) ? keyFrameList.get(index) : null;
        }
        // Add last KeyFrame
        path.add(new Frame(kf[1].position(), kf[1].orientation(), kf[1].magnitude()));
      }
      pathIsValid = true;
    }
  }

  /**
   * Internal use. Calls {@link #invalidateValues()} if a keyFrame (frame) defining the
   * path was recently modified.
   */
  protected void checkValidity() {
    boolean flag = false;
    for (KeyFrame element : keyFrameList) {
      if (element.frame().lastUpdate() > lastUpdate()) {
        flag = true;
        break;
      }
    }
    if (flag) {
      this.invalidateValues();
      this.checked();
    }
  }

  /**
   * Returns the Frame associated with the keyFrame at index {@code index}.
   * <p>
   * See also {@link #keyFrameTime(int)}. {@code index} has to be in the range 0..
   * {@link #numberOfKeyFrames()}-1.
   * <p>
   * <b>Note:</b> If this keyFrame was defined using a reference to a Frame (see
   * {@link #addKeyFrame(GenericFrame, float)} the current referenced Frame state is
   * returned.
   */
  public GenericFrame keyFrame(int index) {
    return keyFrameList.get(index).frame();
  }

  /**
   * Returns the time corresponding to the {@code index} keyFrame. index has to be in the
   * range 0.. {@link #numberOfKeyFrames()}-1.
   *
   * @see #keyFrame(int)
   */
  public float keyFrameTime(int index) {
    return keyFrameList.get(index).time();
  }

  /**
   * Returns the duration of the KeyFrameInterpolator path, expressed in seconds.
   * <p>
   * Simply corresponds to {@link #lastTime()} - {@link #firstTime()}. Returns 0.0 if the
   * path has less than 2 keyFrames.
   *
   * @see #keyFrameTime(int)
   */
  public float duration() {
    return lastTime() - firstTime();
  }

  /**
   * Returns the time corresponding to the first keyFrame, expressed in seconds.
   * <p>
   * Returns 0.0 if the path is empty.
   *
   * @see #lastTime()
   * @see #duration()
   * @see #keyFrameTime(int)
   */
  public float firstTime() {
    if (keyFrameList.isEmpty())
      return 0.0f;
    else
      return keyFrameList.get(0).time();
  }

  /**
   * Returns the time corresponding to the last keyFrame, expressed in seconds.
   * <p>
   *
   * @see #firstTime()
   * @see #duration()
   * @see #keyFrameTime(int)
   */
  public float lastTime() {
    if (keyFrameList.isEmpty())
      return 0.0f;
    else
      return keyFrameList.get(keyFrameList.size() - 1).time();
  }

  protected void updateCurrentKeyFrameForTime(float time) {
    // Assertion: times are sorted in monotone order.
    // Assertion: keyFrame_ is not empty

    // TODO: Special case for loops when closed path is implemented !!
    if (!currentFrmValid)
      // Recompute everything from scratch
      currentFrame1 = keyFrameList.listIterator();

    // currentFrame_[1]->peekNext() <---> keyFr.get(currentFrame1.nextIndex());
    while (keyFrameList.get(currentFrame1.nextIndex()).time() > time) {
      currentFrmValid = false;
      if (!currentFrame1.hasPrevious())
        break;
      currentFrame1.previous();
    }

    if (!currentFrmValid)
      currentFrame2 = keyFrameList.listIterator(currentFrame1.nextIndex());

    while (keyFrameList.get(currentFrame2.nextIndex()).time() < time) {
      currentFrmValid = false;

      if (!currentFrame2.hasNext())
        break;

      currentFrame2.next();
    }

    if (!currentFrmValid) {
      currentFrame1 = keyFrameList.listIterator(currentFrame2.nextIndex());

      if ((currentFrame1.hasPrevious()) && (time < keyFrameList.get(currentFrame2.nextIndex()).time()))
        currentFrame1.previous();

      currentFrame0 = keyFrameList.listIterator(currentFrame1.nextIndex());

      if (currentFrame0.hasPrevious())
        currentFrame0.previous();

      currentFrame3 = keyFrameList.listIterator(currentFrame2.nextIndex());

      if (currentFrame3.hasNext())
        currentFrame3.next();

      currentFrmValid = true;
      splineCacheIsValid = false;
    }
  }

  protected void updateSplineCache() {
    Vec deltaP = Vec.subtract(keyFrameList.get(currentFrame2.nextIndex()).position(),
        keyFrameList.get(currentFrame1.nextIndex()).position());
    pv1 = Vec.add(Vec.multiply(deltaP, 3.0f), Vec.multiply(keyFrameList.get(currentFrame1.nextIndex()).tgP(), (-2.0f)));
    pv1 = Vec.subtract(pv1, keyFrameList.get(currentFrame2.nextIndex()).tgP());
    pv2 = Vec.add(Vec.multiply(deltaP, (-2.0f)), keyFrameList.get(currentFrame1.nextIndex()).tgP());
    pv2 = Vec.add(pv2, keyFrameList.get(currentFrame2.nextIndex()).tgP());
    splineCacheIsValid = true;
  }

  /**
   * Interpolate {@link #frame()} at time {@code time} (expressed in seconds).
   * {@link #interpolationTime()} is set to {@code time} and {@link #frame()} is set
   * accordingly.
   * <p>
   * If you simply want to change {@link #interpolationTime()} but not the
   * {@link #frame()} state, use {@link #setInterpolationTime(float)} instead.
   */
  public void interpolateAtTime(float time) {
    this.checkValidity();
    setInterpolationTime(time);

    if ((keyFrameList.isEmpty()) || (frame() == null))
      return;

    if (!valuesAreValid)
      updateModifiedFrameValues();

    updateCurrentKeyFrameForTime(time);

    if (!splineCacheIsValid)
      updateSplineCache();

    float alpha;
    float dt = keyFrameList.get(currentFrame2.nextIndex()).time() - keyFrameList.get(currentFrame1.nextIndex()).time();
    if (Util.zero(dt))
      alpha = 0.0f;
    else
      alpha = (time - keyFrameList.get(currentFrame1.nextIndex()).time()) / dt;

    Vec pos = Vec.add(keyFrameList.get(currentFrame1.nextIndex()).position(), Vec.multiply(
        Vec.add(keyFrameList.get(currentFrame1.nextIndex()).tgP(),
            Vec.multiply(Vec.add(pv1, Vec.multiply(pv2, alpha)), alpha)), alpha));

    float mag = Util.lerp(keyFrameList.get(currentFrame1.nextIndex()).magnitude(),
        keyFrameList.get(currentFrame2.nextIndex()).magnitude(), alpha);

    Rotation q;
    if (gScene.is3D()) {
      q = Quat.squad((Quat) keyFrameList.get(currentFrame1.nextIndex()).orientation(),
          ((KeyFrame3D) keyFrameList.get(currentFrame1.nextIndex())).tgQ(),
          ((KeyFrame3D) keyFrameList.get(currentFrame2.nextIndex())).tgQ(),
          (Quat) keyFrameList.get(currentFrame2.nextIndex()).orientation(), alpha);
    } else {
      q = new Rot(Util.lerp(keyFrameList.get(currentFrame1.nextIndex()).orientation().angle(),
          keyFrameList.get(currentFrame2.nextIndex()).orientation().angle(), (alpha)));
    }

    frame().setPositionWithConstraint(pos);
    frame().setRotationWithConstraint(q);
    frame().setMagnitude(mag);
  }
}
