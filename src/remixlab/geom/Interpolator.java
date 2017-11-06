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

import remixlab.fpstiming.TimingHandler;
import remixlab.fpstiming.TimingTask;
import remixlab.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A keyFrame Catmull-Rom Frame interpolator.
 * <p>
 * A Interpolator holds keyFrames (that define a path) and, optionally, a
 * reference to a Frame of your application (which will be interpolated). In this case,
 * when the user {@link #start()}, the Interpolator regularly updates
 * the {@link #frame()} position, orientation and magnitude along the path.
 * <p>
 * Here is a typical utilization example (see also ProScene's FrameInterpolation and
 * CameraInterpolation examples):
 * <p>
 * {@code //init() should look like:}<br>
 * {@code // The Interpolator kfi is given the Frame that it will drive over time.}
 * <br>
 * {@code myFrame = new Frame());}<br>
 * {@code kfi = new Interpolator( myScene, myFrame );}<br>
 * {@code // With an anonymous frame would look like this: kfi = new Interpolator( myScene );}
 * <br>
 * {@code kfi.addKeyFrame( new Frame( new Vector(1,0,0), new Quaternion() ) );}<br>
 * {@code kfi.addKeyFrame( new Frame( new Vector(2,1,0), new Quaternion() ) );}<br>
 * {@code // ...and so on for all the keyFrames.}<br>
 * {@code kfi.start();}<br>
 * <p>
 * {@code //mainDrawingLoop() should look like:}<br>
 * {@code graph.pushModelView();}<br>
 * {@code kfi.frame().applyTransformation(this);}<br>
 * {@code // Draw your object here. Its position, orientation and magnitude are interpolated.}
 * <br>
 * {@code graph.popModelView();}<br>
 * <p>
 * The keyFrames are defined by a Frame and a time, expressed in seconds. The time has to
 * be monotonously increasing over keyFrames. When {@link #speed()} equals
 * 1.0 (default value), these times correspond to actual user's seconds during
 * interpolation (provided that your main loop is fast enough). The interpolation is then
 * real-time: the keyFrames will be reached at their {@link #keyFrameTime(int)}.
 * <p>
 * <h3>Interpolation details</h3>
 * <p>
 * When the user {@link #start()}, a timer is started which will update the
 * {@link #frame()}'s position, orientation and magnitude every
 * {@link #period()} milliseconds. This update increases the
 * {@link #time()} by {@link #period()} *
 * {@link #speed()} milliseconds.
 * <p>
 * Note that this mechanism ensures that the number of interpolation steps is constant and
 * equal to the total path {@link #duration()} divided by the
 * {@link #period()} * {@link #speed()}. This is especially
 * useful for benchmarking or movie creation (constant number of snapshots).
 * <p>
 * The interpolation is stopped when {@link #time()} is greater than the
 * {@link #lastTime()} (unless loop() is {@code true}).
 * <p>
 * <b>Attention:</b> If a Constraint is attached to the {@link #frame()} (see
 * {@link Frame#constraint()}), it should be deactivated before
 * {@link #started()}, otherwise the interpolated motion (computed as if
 * there was no constraint) will probably be erroneous.
 */
public class Interpolator {
  /**
   * Returns whether or not this Interpolator matches other.
   *
   * @param other keyFrameInterpolator
   */
  public boolean matches(Interpolator other) {
    boolean result = true;
    for(int i = 0; i < keyFrameList.size(); i++) {
      if(!keyFrameList.get(i).matches(other.keyFrameList.get(i)))
        result = false;
      break;
    }
    return result;
  }

  /**
   * Internal protected abstract base class for 2d and 3d KeyFrames
   */
  protected class KeyFrame {
    /**
     * Returns whether or not this KeyFrame matches other.
     *
     * @param other KeyFrame
     */
    public boolean matches(KeyFrame other) {
      return frame().matches(other.frame()) && time() == other.time();
    }

    protected Quaternion tgQuaternion;
    protected Vector tgPVector;
    protected float tm;
    protected Frame frm;

    KeyFrame(Frame fr, float t) {
      tm = t;
      frm = fr;
    }

    protected KeyFrame(KeyFrame otherKF) {
      this.tm = otherKF.tm;
      this.frm = otherKF.frm.get();
    }

    public KeyFrame get() {
      return new KeyFrame(this);
    }

    Frame frame() {
      return frm;
    }

    Quaternion tgQ() {
      return tgQuaternion;
    }

    Vector tgP() {
      return tgPVector;
    }

    Vector position() {
      return frame().position();
    }

    Quaternion orientation() {
      return frame().orientation();
    }

    float magnitude() {
      return frame().magnitude();
    }

    float time() {
      return tm;
    }

    void computeTangent(KeyFrame prev, KeyFrame next) {
      tgPVector = Vector.multiply(Vector.subtract(next.position(), prev.position()), 0.5f);
      tgQuaternion = Quaternion.squadTangent(prev.orientation(), orientation(), next.orientation());
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
  private Vector pv1, pv2;
  // Option 2 (interpolate magnitude using a spline)
  // private Vector sv1, sv2;

  // S C E N E
  protected Graph graph;

  /**
   * Convenience constructor that simply calls {@code this(scn, new Frame())}.
   * <p>
   * Creates an anonymous {@link #frame()} to be interpolated by this
   * Interpolator.
   *
   * @see #Interpolator(Graph, Frame)
   */
  public Interpolator(Graph graph) {
    this(graph, new Frame());
  }

  public Interpolator(Node node) {
    this(node.graph(), node);
  }

  /**
   * Creates a Interpolator, with {@code frame} as associated {@link #frame()}.
   * <p>
   * The {@link #frame()} can be set or changed using {@link #setFrame(Frame)}.
   * <p>
   * {@link #time()}, {@link #speed()} and
   * {@link #period()} are set to their default values.
   */
  public Interpolator(Graph g, Frame frame) {
    graph = g;
    keyFrameList = new ArrayList<KeyFrame>();
    path = new ArrayList<Frame>();
    setFrame(frame);
    period = 40;
    interpolationTm = 0.0f;
    interpolationSpd = 1.0f;
    interpolationStrt = false;
    lpInterpolation = false;
    pathIsValid = false;
    valuesAreValid = true;
    currentFrmValid = false;

    currentFrame0 = keyFrameList.listIterator();
    currentFrame1 = keyFrameList.listIterator();
    currentFrame2 = keyFrameList.listIterator();
    currentFrame3 = keyFrameList.listIterator();

    interpolationTimerTask = new TimingTask() {
      public void execute() {
        update();
      }
    };
    graph.registerTimingTask(interpolationTimerTask);
  }

  protected Interpolator(Interpolator otherKFI) {
    this.graph = otherKFI.graph;
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
    graph.registerTimingTask(interpolationTimerTask);

    this.invalidateValues();
  }

  public Interpolator get() {
    return new Interpolator(this);
  }

  /**
   * Returns the graph this object belongs to
   */
  public Graph graph() {
    return graph;
  }

  /**
   * Internal use. Updates the last frame path was updated. Called by
   * {@link #checkValidity()}.
   */
  protected void checked() {
    lUpdate = TimingHandler.frameCount;
  }

  /**
   * Internal use. Called by {@link #checkValidity()}.
   */
  protected long lastUpdate() {
    return lUpdate;
  }

  /**
   * Sets the {@link #frame()} associated to the Interpolator.
   */
  public void setFrame(Frame f) {
    mainFrame = f;
  }

  /**
   * Returns the associated Frame that is interpolated by the Interpolator.
   * <p>
   * When {@link #started()}, this Frame's position, orientation and
   * magnitude will regularly be updated by a timer, so that they follow the
   * Interpolator path.
   * <p>
   * Set using {@link #setFrame(Frame)} or with the Interpolator constructor.
   */
  public Frame frame() {
    return mainFrame;
  }

  /**
   * Returns the number of keyFrames used by the interpolation. Use
   * {@link #addKeyFrame(Frame)} to addGrabber new keyFrames.
   */
  public int numberOfKeyFrames() {
    return keyFrameList.size();
  }

  /**
   * Returns the current interpolation time (in seconds) along the Interpolator
   * path.
   * <p>
   * This time is regularly updated when {@link #started()}. Can be set
   * directly with {@link #setTime(float)} or
   * {@link #interpolateAtTime(float)}.
   */
  public float time() {
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
   * @see #period()
   */
  public float speed() {
    return interpolationSpd;
  }

  /**
   * Returns the current interpolation period, expressed in milliseconds. The update of
   * the {@link #frame()} state will be done by a timer at this period when
   * {@link #started()}.
   * <p>
   * This period (multiplied by {@link #speed()}) is added to the
   * {@link #time()} at each update, and the {@link #frame()} state is
   * modified accordingly (see {@link #interpolateAtTime(float)}). Default value is 40
   * milliseconds.
   *
   * @see #setPeriod(int)
   */
  public int period() {
    return period;
  }

  /**
   * Returns {@code true} when the interpolation is played in an infinite loop.
   * <p>
   * When {@code false} (default), the interpolation stops when
   * {@link #time()} reaches {@link #firstTime()} (with negative
   * {@link #speed()}) or {@link #lastTime()}.
   * <p>
   * {@link #time()} is otherwise reset to {@link #firstTime()} (+
   * {@link #time()} - {@link #lastTime()}) (and inversely for negative
   * {@link #speed()}) and interpolation continues.
   */
  public boolean loop() {
    return lpInterpolation;
  }

  /**
   * Sets the {@link #time()}.
   * <p>
   * <b>Attention:</b> The {@link #frame()} state is not affected by this method. Use this
   * function to define the starting time of a future interpolation (see
   * {@link #start()}). Use {@link #interpolateAtTime(float)} to actually
   * interpolate at a given time.
   */
  public void setTime(float time) {
    interpolationTm = time;
  }

  /**
   * Sets the {@link #speed()}. Negative or null values are allowed.
   */
  public void setSpeed(float speed) {
    interpolationSpd = speed;
  }

  /**
   * Sets the {@link #period()}. Should positive.
   */
  public void setPeriod(int myPeriod) {
    if (myPeriod > 0)
      period = myPeriod;
  }

  /**
   * Convenience function that simply calls {@code setLoop(true)}.
   */
  public void setLoop() {
    setLoop(true);
  }

  /**
   * Sets the {@link #loop()} value.
   */
  public void setLoop(boolean loop) {
    lpInterpolation = loop;
  }

  /**
   * Returns {@code true} when the interpolation is being performed. Use
   * {@link #start()} or {@link #stop()} to modify this state.
   */
  public boolean started() {
    return interpolationStrt;
  }

  /**
   * Same as {@code if(started()) stop(); else start();}
   *
   * @see #start()
   * @see #stop()
   */
  public void toggle() {
    if(started())
      stop();
    else
      start();
  }

  /**
   * Updates {@link #frame()} state according to current {@link #time()}.
   * Then adds {@link #period()}* {@link #speed()} to
   * {@link #time()}.
   * <p>
   * This internal method is called by a timer when {@link #started()}. It
   * can be used for debugging purpose. {@link #stop()} is called when
   * {@link #time()} reaches {@link #firstTime()} or {@link #lastTime()},
   * unless {@link #loop()} is {@code true}.
   */
  protected void update() {
    interpolateAtTime(time());

    interpolationTm += speed() * period() / 1000.0f;

    if (time() > keyFrameList.get(keyFrameList.size() - 1).time()) {
      if (loop())
        setTime(
            keyFrameList.get(0).time() + interpolationTm - keyFrameList.get(keyFrameList.size() - 1).time());
      else {
        // Make sure last KeyFrame is reached and displayed
        interpolateAtTime(keyFrameList.get(keyFrameList.size() - 1).time());
        stop();
      }
    } else if (time() < keyFrameList.get(0).time()) {
      if (loop())
        setTime(
            keyFrameList.get(keyFrameList.size() - 1).time() - keyFrameList.get(0).time() + interpolationTm);
      else {
        // Make sure first KeyFrame is reached and displayed
        interpolateAtTime(keyFrameList.get(0).time());
        stop();
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
   * Convenience function that simply calls {@code start(-1)}.
   *
   * @see #start(int)
   */
  public void start() {
    start(-1);
  }

  /**
   * Starts the interpolation process.
   * <p>
   * A timer is started with an {@link #period()} period that updates the
   * {@link #frame()}'s position, orientation and magnitude.
   * {@link #started()} will return {@code true} until
   * {@link #stop()} is called.
   * <p>
   * If {@code period} is positive, it is set as the new {@link #period()}.
   * The previous {@link #period()} is used otherwise (default).
   * <p>
   * If {@link #time()} is larger than {@link #lastTime()},
   * {@link #time()} is reset to {@link #firstTime()} before interpolation
   * starts (and inversely for negative {@link #speed()}.
   * <p>
   * Use {@link #setTime(float)} before calling this method to change the
   * starting {@link #time()}.
   * <p>
   * <b>Attention:</b> The keyFrames must be defined (see
   * {@link #addKeyFrame(Frame, float)}) before you start(), or else
   * the interpolation will naturally immediately stop.
   */
  public void start(int myPeriod) {
    if(started())
      stop();
    if (myPeriod >= 0)
      setPeriod(myPeriod);

    if (!keyFrameList.isEmpty()) {
      if ((speed() > 0.0) && (time() >= keyFrameList.get(keyFrameList.size() - 1).time()))
        setTime(keyFrameList.get(0).time());
      if ((speed() < 0.0) && (time() <= keyFrameList.get(0).time()))
        setTime(keyFrameList.get(keyFrameList.size() - 1).time());
      if (keyFrameList.size() > 1)
        interpolationTimerTask.run(period());
      interpolationStrt = true;
      update();
    }
  }

  /**
   * Stops an interpolation started with {@link #start()}. See
   * {@link #started()}.
   */
  public void stop() {
    interpolationTimerTask.stop();
    interpolationStrt = false;
  }

  /**
   * Stops the interpolation and resets {@link #time()} to the
   * {@link #firstTime()}.
   * <p>
   * If desired, call {@link #interpolateAtTime(float)} after this method to actually move
   * the {@link #frame()} to {@link #firstTime()}.
   */
  public void reset() {
    stop();
    setTime(firstTime());
  }

  /**
   * Appends a new keyFrame to the path.
   * <p>
   * Same as {@link #addKeyFrame(Frame, float)}, except that the
   * {@link #keyFrameTime(int)} is set to the previous {@link #keyFrameTime(int)} plus one
   * second (or 0.0 if there is no previous keyFrame).
   */
  public void addKeyFrame(Frame node) {
    float time;

    if (keyFrameList.isEmpty())
      time = 0.0f;
    else
      time = keyFrameList.get(keyFrameList.size() - 1).time() + 1.0f;

    addKeyFrame(node, time);
  }

  /**
   * Appends a new keyFrame to the path, with its associated {@code time} (in seconds).
   * <p>
   * When {@code setRef} is {@code false} the keyFrame is added by value, meaning that the
   * path will use the current {@code frame} state.
   * <p>
   * When {@code setRef} is {@code true} the keyFrame is given as a reference to a Frame,
   * which will be connected to the Interpolator: when {@code frame} is modified,
   * the Interpolator path is updated accordingly. This allows for dynamic paths,
   * where keyFrame can be edited, even during the interpolation. {@code null} frame
   * references are silently ignored. The {@link #keyFrameTime(int)} has to be
   * monotonously increasing over keyFrames.
   */
  public void addKeyFrame(Frame node, float time) {
    if (node == null)
      return;

    if (keyFrameList.isEmpty())
      interpolationTm = time;

    if ((!keyFrameList.isEmpty()) && (keyFrameList.get(keyFrameList.size() - 1).time() > time))
      System.out.println("Error in Interpolator.addKeyFrame: time is not monotone");
    else
      keyFrameList.add(new KeyFrame(node, time));

    valuesAreValid = false;
    pathIsValid = false;
    currentFrmValid = false;
    reset();
  }

  /**
   * Remove KeyFrame according to {@code index} in the list and
   * {@link #stop()} if {@link #started()}. If
   * {@code index < 0 || index >= keyFr.size()} the call is silently ignored.
   */
  public void removeKeyFrame(int index) {
    if (index < 0 || index >= keyFrameList.size())
      return;
    valuesAreValid = false;
    pathIsValid = false;
    currentFrmValid = false;
    if (started())
      stop();
    KeyFrame kf = keyFrameList.remove(index);
    if(kf.frame() instanceof Node)
      graph.pruneBranch((Node)kf.frm);
    setTime(firstTime());
  }

  /**
   * Removes all keyFrames from the path. The {@link #numberOfKeyFrames()} is set to 0.
   */
  public void clear() {
    stop();
    ListIterator<KeyFrame> it = keyFrameList.listIterator();
    while (it.hasNext()) {
      KeyFrame keyFrame = it.next();
      if(keyFrame.frame() instanceof Node)
        graph.pruneBranch((Node)keyFrame.frm);
    }
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
   * Interpolator path.
   * <p>
   * Use it in your Interpolator path drawing routine.
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
          Vector pdiff = Vector.subtract(kf[2].position(), kf[1].position());
          Vector pvec1 = Vector.add(Vector.multiply(pdiff, 3.0f), Vector.multiply(kf[1].tgP(), (-2.0f)));
          pvec1 = Vector.subtract(pvec1, kf[2].tgP());
          Vector pvec2 = Vector.add(Vector.multiply(pdiff, (-2.0f)), kf[1].tgP());
          pvec2 = Vector.add(pvec2, kf[2].tgP());

          for (int step = 0; step < nbSteps; ++step) {
            Frame frame = new Frame();
            float alpha = step / (float) nbSteps;
            frame.setPosition(Vector.add(kf[1].position(),
                Vector.multiply(Vector.add(kf[1].tgP(), Vector.multiply(Vector.add(pvec1, Vector.multiply(pvec2, alpha)), alpha)), alpha)));
            if (graph.is3D()) {
              frame.setOrientation(
                  Quaternion.squad(kf[1].orientation(), kf[1].tgQ(), kf[2].tgQ(), kf[2].orientation(), alpha));
            } else {
              // linear interpolation
              float start = kf[1].orientation().angle();
              float stop = kf[2].orientation().angle();
              frame.setOrientation(new Quaternion(new Vector(0,0,1), start + (stop - start) * alpha));
            }
            frame.setMagnitude(Vector.lerp(kf[1].magnitude(), kf[2].magnitude(), alpha));
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
   * {@link #addKeyFrame(Frame, float)} the current referenced Frame state is
   * returned.
   */
  public Frame keyFrame(int index) {
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
   * Returns the duration of the Interpolator path, expressed in seconds.
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
    Vector deltaP = Vector.subtract(keyFrameList.get(currentFrame2.nextIndex()).position(),
        keyFrameList.get(currentFrame1.nextIndex()).position());
    pv1 = Vector.add(Vector.multiply(deltaP, 3.0f), Vector.multiply(keyFrameList.get(currentFrame1.nextIndex()).tgP(), (-2.0f)));
    pv1 = Vector.subtract(pv1, keyFrameList.get(currentFrame2.nextIndex()).tgP());
    pv2 = Vector.add(Vector.multiply(deltaP, (-2.0f)), keyFrameList.get(currentFrame1.nextIndex()).tgP());
    pv2 = Vector.add(pv2, keyFrameList.get(currentFrame2.nextIndex()).tgP());
    splineCacheIsValid = true;
  }

  /**
   * Interpolate {@link #frame()} at time {@code time} (expressed in seconds).
   * {@link #time()} is set to {@code time} and {@link #frame()} is set
   * accordingly.
   * <p>
   * If you simply want to change {@link #time()} but not the
   * {@link #frame()} state, use {@link #setTime(float)} instead.
   */
  //TODO rename me as atTime() ??
  public void interpolateAtTime(float time) {
    this.checkValidity();
    setTime(time);

    if ((keyFrameList.isEmpty()) || (frame() == null))
      return;

    if (!valuesAreValid)
      updateModifiedFrameValues();

    updateCurrentKeyFrameForTime(time);

    if (!splineCacheIsValid)
      updateSplineCache();

    float alpha;
    float dt = keyFrameList.get(currentFrame2.nextIndex()).time() - keyFrameList.get(currentFrame1.nextIndex()).time();
    if (dt == 0)
      alpha = 0.0f;
    else
      alpha = (time - keyFrameList.get(currentFrame1.nextIndex()).time()) / dt;

    Vector pos = Vector.add(keyFrameList.get(currentFrame1.nextIndex()).position(), Vector.multiply(
        Vector.add(keyFrameList.get(currentFrame1.nextIndex()).tgP(),
            Vector.multiply(Vector.add(pv1, Vector.multiply(pv2, alpha)), alpha)), alpha));

    float mag = Vector.lerp(keyFrameList.get(currentFrame1.nextIndex()).magnitude(),
        keyFrameList.get(currentFrame2.nextIndex()).magnitude(), alpha);

    Quaternion q;
    if (graph.is3D()) {
      q = Quaternion.squad((Quaternion) keyFrameList.get(currentFrame1.nextIndex()).orientation(),
          keyFrameList.get(currentFrame1.nextIndex()).tgQ(),
          keyFrameList.get(currentFrame2.nextIndex()).tgQ(),
          keyFrameList.get(currentFrame2.nextIndex()).orientation(), alpha);
    } else {
      q = new Quaternion(new Vector(0,0,1), Vector.lerp(keyFrameList.get(currentFrame1.nextIndex()).orientation().angle(),
          keyFrameList.get(currentFrame2.nextIndex()).orientation().angle(), (alpha)));
    }

    frame().setPositionWithConstraint(pos);
    frame().setRotationWithConstraint(q);
    frame().setMagnitude(mag);
  }
}
