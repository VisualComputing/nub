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

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.timing.TimingHandler;
import frames.timing.TimingTask;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A key frame Catmull-Rom interpolator.
 * <p>
 * An interpolator holds key frames (that define a path) and, optionally, a
 * reference to a frame of your application (which will be interpolated). In this case,
 * when the user {@link #start()}, the interpolator regularly updates
 * the {@link #frame()} position, orientation and magnitude along the path.
 * <p>
 * Here is a typical usage example:
 * <pre>
 * {@code
 * void init() {
 *   Graph graph = new Graph(1200, 800);
 *   Interpolator interpolator = new Interpolator(graph);
 *   for (int i = 0; i < 10; i++)
 *     interpolator.addKeyFrame(Frame.random(graph));
 *   interpolator.start();
 * }
 * }
 * </pre>
 * which will create a random (see {@link Frame#random(Graph)}) interpolator path
 * containing 10 key-frames (see {@link #addKeyFrame(Frame)}). The interpolation is
 * also started (see {@link #start()}).
 * <p>
 * The graph main drawing loop should look like:
 * <pre>
 * {@code
 * void mainLoop() {
 *   graph.pushModelView();
 *   graph.applyTransformation(interpolator.frame());
 *   // draw your object here. Its position, orientation and magnitude are interpolated.
 *   graph.popModelView();
 * }
 * }
 * </pre>
 * The key frames are defined by a frame and a time, expressed in seconds. The time has to
 * be monotonously increasing over key frames. When {@link #speed()} equals
 * 1 (default value), these times correspond to actual user's seconds during
 * interpolation (provided that your main loop is fast enough). The interpolation is then
 * real-time: the key frames will be reached at their {@link #time(int)}.
 *
 * <h3>Interpolation details</h3>
 * <p>
 * When {@link #start()} is called, a timer is started which will update the
 * {@link #frame()}'s position, orientation and magnitude every {@link #period()} milliseconds.
 * This update increases the {@link #time()} by {@link #period()} * {@link #speed()}
 * milliseconds.
 * <p>
 * Note that this mechanism ensures that the number of interpolation steps is constant and
 * equal to the total path {@link #duration()} divided by the
 * {@link #period()} * {@link #speed()}. This is especially
 * useful for benchmarking or movie creation (constant number of snapshots).
 * <p>
 * The interpolation is stopped when {@link #time()} is greater than the
 * {@link #lastTime()} (unless loop() is {@code true}).
 *
 * <b>Attention:</b> If a {@link frames.core.constraint.Constraint} is attached to
 * the {@link #frame()} (see {@link Frame#constraint()}), it should be reset before
 * {@link #start()} is called, otherwise the interpolated motion (computed as if
 * there was no constraint) will probably be erroneous.
 */
public class Interpolator {
  /**
   * Returns whether or not this interpolator matches other.
   *
   * @param interpolator other interpolator
   */
  public boolean matches(Interpolator interpolator) {
    boolean result = true;
    for (int i = 0; i < _list.size(); i++) {
      if (!_list.get(i).matches(interpolator._list.get(i)))
        result = false;
      break;
    }
    return result;
  }

  /**
   * Internal protected class representing 2d and 3d key frames.
   */
  protected class KeyFrame {
    /**
     * Returns whether or not this key frame matches the {@code other}.
     *
     * @param keyFrame other keyFrame
     */
    public boolean matches(KeyFrame keyFrame) {
      return frame().matches(keyFrame.frame()) && time() == keyFrame.time();
    }

    protected Quaternion _tangentQuaternion;
    protected Vector _tangentVector;
    protected float _time;
    protected Frame _frame;

    KeyFrame(Frame frame, float time) {
      _time = time;
      _frame = frame;
    }

    protected KeyFrame(KeyFrame other) {
      this._time = other._time;
      this._frame = other._frame.get();
    }

    public KeyFrame get() {
      return new KeyFrame(this);
    }

    Frame frame() {
      return _frame;
    }

    Quaternion tangentQuaternion() {
      return _tangentQuaternion;
    }

    Vector tangentVector() {
      return _tangentVector;
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
      return _time;
    }

    void computeTangent(KeyFrame prev, KeyFrame next) {
      _tangentVector = Vector.multiply(Vector.subtract(next.position(), prev.position()), 0.5f);
      _tangentQuaternion = Quaternion.squadTangent(prev.orientation(), orientation(), next.orientation());
    }
  }

  protected long _lastUpdate;
  protected List<KeyFrame> _list;
  protected ListIterator<KeyFrame> _current0;
  protected ListIterator<KeyFrame> _current1;
  protected ListIterator<KeyFrame> _current2;
  protected ListIterator<KeyFrame> _current3;
  protected List<Frame> _path;

  // Main frame
  protected Frame _frame;

  // Beat
  protected TimingTask _task;
  protected int _period;
  protected float _time;
  protected float _speed;
  protected boolean _started;

  // Misc
  protected boolean _loop;

  // Cached values and flags
  protected boolean _pathIsValid;
  protected boolean _valuesAreValid;
  protected boolean _currentFrameValid;
  protected boolean _splineCacheIsValid;
  protected Vector _vector1, _vector2;

  // Graph
  protected Graph _graph;

  /**
   * Convenience constructor that simply calls {@code this(graph, new Frame())}.
   * <p>
   * Creates an anonymous {@link #frame()} to be interpolated by this
   * interpolator.
   *
   * @see #Interpolator(Frame)
   * @see #Interpolator(Graph, Frame)
   */
  public Interpolator(Graph graph) {
    this(graph, new Frame());
  }

  /**
   * Same as {@code this(frame.graph(), frame)}. Note that {@code frame} should be attached to
   * a {@link Graph}.
   *
   * @see #Interpolator(Graph)
   * @see #Interpolator(Graph, Frame)
   */
  public Interpolator(Frame frame) {
    this(frame.graph(), frame);
  }

  /**
   * Creates an interpolator, with {@code frame} as associated {@link #frame()}.
   * <p>
   * The {@link #frame()} can be set or changed using {@link #setFrame(Frame)}.
   * <p>
   * {@link #time()}, {@link #speed()} and {@link #period()} are set to their default values.
   */
  public Interpolator(Graph graph, Frame frame) {
    if (graph == null)
      throw new RuntimeException("Warning: no interpolator instantiated");
    _graph = graph;
    _list = new ArrayList<KeyFrame>();
    _path = new ArrayList<Frame>();
    setFrame(frame);
    _period = 40;
    _time = 0.0f;
    _speed = 1.0f;
    _started = false;
    _loop = false;
    _pathIsValid = false;
    _valuesAreValid = true;
    _currentFrameValid = false;

    _current0 = _list.listIterator();
    _current1 = _list.listIterator();
    _current2 = _list.listIterator();
    _current3 = _list.listIterator();

    _task = new TimingTask() {
      public void execute() {
        _update();
      }
    };
    _graph.registerTask(_task);
  }

  protected Interpolator(Interpolator other) {
    this._graph = other._graph;
    this._path = new ArrayList<Frame>();
    ListIterator<Frame> frameIt = other._path.listIterator();
    while (frameIt.hasNext()) {
      this._path.add(frameIt.next().get());
    }

    this.setFrame(other.frame());

    this._period = other._period;
    this._time = other._time;
    this._speed = other._speed;
    this._started = other._started;
    this._loop = other._loop;
    this._pathIsValid = other._pathIsValid;
    this._valuesAreValid = other._valuesAreValid;
    this._currentFrameValid = other._currentFrameValid;

    this._list = new ArrayList<KeyFrame>();

    for (KeyFrame element : other._list) {
      KeyFrame kf = (KeyFrame) element.get();
      this._list.add(kf);
    }

    this._current0 = _list.listIterator(other._current0.nextIndex());
    this._current1 = _list.listIterator(other._current1.nextIndex());
    this._current2 = _list.listIterator(other._current2.nextIndex());
    this._current3 = _list.listIterator(other._current3.nextIndex());

    this._task = new TimingTask() {
      public void execute() {
        _update();
      }
    };
    _graph.registerTask(_task);

    this._invalidateValues();
  }

  /**
   * Returns a deep copy of this interpolator.
   */
  public Interpolator get() {
    return new Interpolator(this);
  }

  /**
   * Returns the graph this interpolator belongs to.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Internal use. Updates the last frame path was updated. Called by
   * {@link #_checkValidity()}.
   */
  protected void _checked() {
    _lastUpdate = TimingHandler.frameCount;
  }

  /**
   * Internal use. Called by {@link #_checkValidity()}.
   */
  protected long _lastUpdate() {
    return _lastUpdate;
  }

  /**
   * Sets the interpolator {@link #frame()}. If frame is instance of {@link frames.core.Frame},
   * the frame graph ({@link Frame#graph()}) and {@link #graph()} should match.
   */
  public void setFrame(Frame frame) {
    if (frame == _frame)
      return;
    if (frame.graph() != null)
      if (graph() != frame.graph())
        throw new RuntimeException("Frame and Interpolator graphs should match");
    _frame = frame;
  }

  /**
   * Returns the frame that is to be interpolated by the interpolator.
   * <p>
   * When {@link #started()}, this frame's position, orientation and
   * magnitude will regularly be updated by a timer, so that they follow the
   * interpolator path.
   * <p>
   * Set using {@link #setFrame(Frame)} or with the interpolator constructor.
   */
  public Frame frame() {
    return _frame;
  }

  /**
   * Returns the number of key frames used by the interpolation. Use
   * {@link #addKeyFrame(Frame)} to add new key frames.
   */
  public int size() {
    return _list.size();
  }

  /**
   * Returns the current interpolation time (in seconds) along the interpolator
   * path.
   * <p>
   * This time is regularly updated when {@link #started()}. Can be set
   * directly with {@link #setTime(float)} or {@link #interpolate(float)}.
   */
  public float time() {
    return _time;
  }

  /**
   * Returns the current interpolation speed.
   * <p>
   * Default value is 1, which means {@link #time(int)} will be matched during
   * the interpolation (provided that your main loop is fast enough).
   * <p>
   * A negative value will result in a reverse interpolation of the key frames.
   *
   * @see #period()
   */
  public float speed() {
    return _speed;
  }

  /**
   * Returns the current interpolation period, expressed in milliseconds. The update of
   * the {@link #frame()} state will be done by a timer at this period when
   * {@link #started()}.
   * <p>
   * This period (multiplied by {@link #speed()}) is added to the
   * {@link #time()} at each update, and the {@link #frame()} state is
   * modified accordingly (see {@link #interpolate(float)}). Default value is 40
   * milliseconds.
   *
   * @see #setPeriod(int)
   */
  public int period() {
    return _period;
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
    return _loop;
  }

  /**
   * Sets the {@link #time()}.
   *
   * <b>Attention:</b> The {@link #frame()} state is not affected by this method. Use this
   * function to define the starting time of a future interpolation (see
   * {@link #start()}). Use {@link #interpolate(float)} to actually
   * interpolate at a given time.
   */
  public void setTime(float time) {
    _time = time;
  }

  /**
   * Sets the {@link #speed()}. Negative or null values are allowed.
   */
  public void setSpeed(float speed) {
    _speed = speed;
  }

  /**
   * Sets the {@link #period()}. Should positive.
   */
  public void setPeriod(int period) {
    if (period > 0)
      _period = period;
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
    _loop = loop;
  }

  /**
   * Returns {@code true} when the interpolation is being performed. Use
   * {@link #start()} or {@link #stop()} to modify this state.
   */
  public boolean started() {
    return _started;
  }

  /**
   * Updates {@link #frame()} state according to current {@link #time()}.
   * Then adds {@link #period()}* {@link #speed()} to {@link #time()}.
   * <p>
   * This internal method is called by a timer when {@link #started()}. It
   * can be used for debugging purpose. {@link #stop()} is called when
   * {@link #time()} reaches {@link #firstTime()} or {@link #lastTime()},
   * unless {@link #loop()} is {@code true}.
   */
  protected void _update() {
    interpolate(time());

    _time += speed() * period() / 1000.0f;

    if (time() > _list.get(_list.size() - 1).time()) {
      if (loop())
        setTime(
            _list.get(0).time() + _time - _list.get(_list.size() - 1).time());
      else {
        // Make sure last KeyFrame is reached and displayed
        interpolate(_list.get(_list.size() - 1).time());
        stop();
      }
    } else if (time() < _list.get(0).time()) {
      if (loop())
        setTime(
            _list.get(_list.size() - 1).time() - _list.get(0).time() + _time);
      else {
        // Make sure first KeyFrame is reached and displayed
        interpolate(_list.get(0).time());
        stop();
      }
    }
  }

  /**
   * Internal use. Called by {@link #_checkValidity()}.
   */
  protected void _invalidateValues() {
    _valuesAreValid = false;
    _pathIsValid = false;
    _splineCacheIsValid = false;
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
   * A timer is started with an {@link #period()} that updates the
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
   *
   * <b>Attention:</b> The key frames must be defined (see
   * {@link #addKeyFrame(Frame, float)}) before you start(), or else
   * the interpolation will naturally immediately stop.
   */
  public void start(int period) {
    if (started())
      stop();
    if (period >= 0)
      setPeriod(period);

    if (!_list.isEmpty()) {
      if ((speed() > 0.0) && (time() >= _list.get(_list.size() - 1).time()))
        setTime(_list.get(0).time());
      if ((speed() < 0.0) && (time() <= _list.get(0).time()))
        setTime(_list.get(_list.size() - 1).time());
      if (_list.size() > 1)
        _task.run(period());
      _started = true;
      _update();
    }
  }

  /**
   * Stops an interpolation started with {@link #start()}. See {@link #started()}.
   */
  public void stop() {
    _task.stop();
    _started = false;
  }

  public void toggle() {
    if (started())
      stop();
    else
      start();
  }

  /**
   * Stops the interpolation and resets {@link #time()} to the {@link #firstTime()}.
   * <p>
   * If desired, call {@link #interpolate(float)} after this method to actually move
   * the {@link #frame()} to {@link #firstTime()}.
   */
  public void reset() {
    stop();
    setTime(firstTime());
  }

  /**
   * Same as {@code addKeyFrame(graph().eye().get())}.
   *
   * @see #addKeyFrame(Frame)
   * @see #graph()
   * @see Frame#get()
   * @see Graph#eye()
   */
  public void addKeyFrame() {
    addKeyFrame(graph().eye().get());
  }

  /**
   * Appends a new key frame to the path.
   * <p>
   * Same as {@link #addKeyFrame(Frame, float)}, except that the
   * {@link #time(int)} is set to the previous {@link #time(int)} plus one
   * second (or 0 if there is no previous key frame).
   */
  public void addKeyFrame(Frame frame) {
    float time;

    if (_list.isEmpty())
      time = 0.0f;
    else
      time = _list.get(_list.size() - 1).time() + 1.0f;

    addKeyFrame(frame, time);
  }

  /**
   * Same as {@code addKeyFrame(graph().eye().get(), time)}.
   *
   * @see #addKeyFrame(Frame, float)
   * @see #graph()
   * @see Frame#get()
   * @see Graph#eye()
   */
  public void addKeyFrame(float time) {
    addKeyFrame(graph().eye().get(), time);
  }

  /**
   * Appends a new key frame to the path, with its associated {@code time} (in seconds).
   * <p>
   * Note that when {@code frame} is modified, the interpolator path is updated accordingly.
   * This allows for dynamic paths, where key frames can be edited, even during the
   * interpolation.
   * <p>
   * {@code null} frame references are silently ignored. The {@link #time(int)} has to be
   * monotonously increasing over key frames.
   */
  public void addKeyFrame(Frame frame, float time) {
    if (frame == null)
      return;

    if (frame.graph() != null)
      if (graph() != frame.graph())
        throw new RuntimeException("Frame and Interpolator graphs should match");

    if (_list.isEmpty())
      _time = time;

    if ((!_list.isEmpty()) && (_list.get(_list.size() - 1).time() > time))
      System.out.println("Error in Interpolator.addKeyFrame: time is not monotone");
    else
      _list.add(new KeyFrame(frame, time));

    _valuesAreValid = false;
    _pathIsValid = false;
    _currentFrameValid = false;
    reset();
  }

  /**
   * Remove key frame according to {@code index} in the list and
   * {@link #stop()} if {@link #started()}. If
   * {@code index < 0 || index >= keyFr.size()} the call is silently ignored.
   */
  public Frame removeKeyFrame(int index) {
    if (index < 0 || index >= _list.size())
      return null;
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentFrameValid = false;
    if (started())
      stop();
    KeyFrame keyFrame = _list.remove(index);
    setTime(firstTime());
    return keyFrame.frame();
  }

  /**
   * Same as {@link #removeKeyFrame(int)}, but also removes the key frame from the scene if it is a node instance.
   */
  public void purgeKeyFrame(int index) {
    Frame frame = removeKeyFrame(index);
    _graph.pruneBranch(frame);
  }

  /**
   * Removes all key frames from the path. The {@link #size()} is set to 0.
   *
   * @see #purge()
   */
  public void clear() {
    stop();
    _list.clear();
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentFrameValid = false;
  }

  /**
   * Same as {@link #clear()}, but also removes the key frames node instances from the scene.
   *
   * @see #clear()
   * @see frames.core.Graph#pruneBranch(Frame)
   */
  public void purge() {
    stop();
    ListIterator<KeyFrame> it = _list.listIterator();
    while (it.hasNext()) {
      KeyFrame keyFrame = it.next();
      _graph.pruneBranch(keyFrame._frame);
    }
    _list.clear();
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentFrameValid = false;
  }

  protected void _updateModifiedFrames() {
    KeyFrame kf;
    KeyFrame prev = _list.get(0);
    kf = _list.get(0);

    int index = 1;
    while (kf != null) {
      KeyFrame next = (index < _list.size()) ? _list.get(index) : null;
      index++;
      if (next != null)
        kf.computeTangent(prev, next);
      else
        kf.computeTangent(prev, kf);
      prev = kf;
      kf = next;
    }
    _valuesAreValid = true;
  }

  /**
   * Returns the list of key frames which defines this interpolator.
   */
  public List<Frame> keyFrames() {
    List<Frame> list = new ArrayList<Frame>();
    for (KeyFrame keyFrame : _list)
      list.add(keyFrame.frame());
    return list;
  }

  /**
   * Computes a path from {@link #keyFrames()} for the interpolator to be drawn.
   * <p>
   * Calls {@link #_updatePath()} and then returns a list of frames defining the
   * interpolator path (which is different than that of {@link #keyFrames()}).
   * <p>
   * Use it in your interpolator path drawing routine.
   */
  public List<Frame> path() {
    _updatePath();
    return _path;
  }

  /**
   * Internal use. Call {@link #_checkValidity()} and if path is not valid recomputes it.
   */
  protected void _updatePath() {
    _checkValidity();
    if (!_pathIsValid) {
      _path.clear();
      int nbSteps = 30;

      if (_list.isEmpty())
        return;

      if (!_valuesAreValid)
        _updateModifiedFrames();

      if (_list.get(0) == _list.get(_list.size() - 1))
        _path.add(
            new Frame(_list.get(0).position(), _list.get(0).orientation(), _list.get(0).magnitude()));
      else {
        KeyFrame[] kf = new KeyFrame[4];
        kf[0] = _list.get(0);
        kf[1] = kf[0];

        int index = 1;
        kf[2] = (index < _list.size()) ? _list.get(index) : null;
        index++;
        kf[3] = (index < _list.size()) ? _list.get(index) : null;

        while (kf[2] != null) {
          Vector pdiff = Vector.subtract(kf[2].position(), kf[1].position());
          Vector pvec1 = Vector.add(Vector.multiply(pdiff, 3.0f), Vector.multiply(kf[1].tangentVector(), (-2.0f)));
          pvec1 = Vector.subtract(pvec1, kf[2].tangentVector());
          Vector pvec2 = Vector.add(Vector.multiply(pdiff, (-2.0f)), kf[1].tangentVector());
          pvec2 = Vector.add(pvec2, kf[2].tangentVector());

          for (int step = 0; step < nbSteps; ++step) {
            float alpha = step / (float) nbSteps;
            _path.add(new Frame(
                Vector.add(kf[1].position(), Vector.multiply(Vector.add(kf[1].tangentVector(), Vector.multiply(Vector.add(pvec1, Vector.multiply(pvec2, alpha)), alpha)), alpha)),
                Quaternion.squad(kf[1].orientation(), kf[1].tangentQuaternion(), kf[2].tangentQuaternion(), kf[2].orientation(), alpha),
                Vector.lerp(kf[1].magnitude(), kf[2].magnitude(), alpha))
            );
          }

          // Shift
          kf[0] = kf[1];
          kf[1] = kf[2];
          kf[2] = kf[3];

          index++;
          kf[3] = (index < _list.size()) ? _list.get(index) : null;
        }
        // Add last KeyFrame
        _path.add(new Frame(kf[1].position(), kf[1].orientation(), kf[1].magnitude()));
      }
      _pathIsValid = true;
    }
  }

  /**
   * Internal use. Calls {@link #_invalidateValues()} if a key frame (frame) defining the
   * path was recently modified.
   */
  protected void _checkValidity() {
    boolean flag = false;
    for (KeyFrame element : _list) {
      if (element.frame().lastUpdate() > _lastUpdate()) {
        flag = true;
        break;
      }
    }
    if (flag) {
      this._invalidateValues();
      this._checked();
    }
  }

  /**
   * Returns the frame associated with the key frame at index {@code index}.
   * <p>
   * See also {@link #time(int)}. {@code index} has to be in the range 0..
   * {@link #size()}-1.
   */
  public Frame keyFrame(int index) {
    return _list.get(index).frame();
  }

  /**
   * Returns the time corresponding to the {@code index} key frame. Not that index has
   * to be in the range 0.. {@link #size()}-1.
   *
   * @see #keyFrame(int)
   */
  public float time(int index) {
    return _list.get(index).time();
  }

  /**
   * Returns the duration of the interpolator path, expressed in seconds.
   * <p>
   * Simply corresponds to {@link #lastTime()} - {@link #firstTime()}. Returns 0 if the
   * path has less than 2 key frames.
   *
   * @see #time(int)
   */
  public float duration() {
    return lastTime() - firstTime();
  }

  /**
   * Returns the time corresponding to the first key frame, expressed in seconds.
   * <p>
   * Returns 0 if the path is empty.
   *
   * @see #lastTime()
   * @see #duration()
   * @see #time(int)
   */
  public float firstTime() {
    if (_list.isEmpty())
      return 0.0f;
    else
      return _list.get(0).time();
  }

  /**
   * Returns the time corresponding to the last key frame, expressed in seconds.
   *
   * @see #firstTime()
   * @see #duration()
   * @see #time(int)
   */
  public float lastTime() {
    if (_list.isEmpty())
      return 0.0f;
    else
      return _list.get(_list.size() - 1).time();
  }

  protected void _updateCurrentKeyFrameForTime(float time) {
    // Assertion: times are sorted in monotone order.
    // Assertion: keyFrame_ is not empty

    // TODO: Special case for loops when closed path is implemented !!
    if (!_currentFrameValid)
      // Recompute everything from scratch
      _current1 = _list.listIterator();

    // currentFrame_[1]->peekNext() <---> keyFr.get(_current1.nextIndex());
    while (_list.get(_current1.nextIndex()).time() > time) {
      _currentFrameValid = false;
      if (!_current1.hasPrevious())
        break;
      _current1.previous();
    }

    if (!_currentFrameValid)
      _current2 = _list.listIterator(_current1.nextIndex());

    while (_list.get(_current2.nextIndex()).time() < time) {
      _currentFrameValid = false;

      if (!_current2.hasNext())
        break;

      _current2.next();
    }

    if (!_currentFrameValid) {
      _current1 = _list.listIterator(_current2.nextIndex());

      if ((_current1.hasPrevious()) && (time < _list.get(_current2.nextIndex()).time()))
        _current1.previous();

      _current0 = _list.listIterator(_current1.nextIndex());

      if (_current0.hasPrevious())
        _current0.previous();

      _current3 = _list.listIterator(_current2.nextIndex());

      if (_current3.hasNext())
        _current3.next();

      _currentFrameValid = true;
      _splineCacheIsValid = false;
    }
  }

  /**
   * Internal use. Used by {@link #interpolate(float)}.
   */
  protected void _updateSplineCache() {
    Vector deltaP = Vector.subtract(_list.get(_current2.nextIndex()).position(),
        _list.get(_current1.nextIndex()).position());
    _vector1 = Vector.add(Vector.multiply(deltaP, 3.0f), Vector.multiply(_list.get(_current1.nextIndex()).tangentVector(), (-2.0f)));
    _vector1 = Vector.subtract(_vector1, _list.get(_current2.nextIndex()).tangentVector());
    _vector2 = Vector.add(Vector.multiply(deltaP, (-2.0f)), _list.get(_current1.nextIndex()).tangentVector());
    _vector2 = Vector.add(_vector2, _list.get(_current2.nextIndex()).tangentVector());
    _splineCacheIsValid = true;
  }

  /**
   * Interpolate {@link #frame()} at time {@code time} (expressed in seconds).
   * {@link #time()} is set to {@code time} and {@link #frame()} is set accordingly.
   * <p>
   * If you simply want to change {@link #time()} but not the
   * {@link #frame()} state, use {@link #setTime(float)} instead.
   */
  public void interpolate(float time) {
    this._checkValidity();
    setTime(time);

    if ((_list.isEmpty()) || (frame() == null))
      return;

    if (!_valuesAreValid)
      _updateModifiedFrames();

    _updateCurrentKeyFrameForTime(time);

    if (!_splineCacheIsValid)
      _updateSplineCache();

    float alpha;
    float dt = _list.get(_current2.nextIndex()).time() - _list.get(_current1.nextIndex()).time();
    if (dt == 0)
      alpha = 0.0f;
    else
      alpha = (time - _list.get(_current1.nextIndex()).time()) / dt;

    Vector pos = Vector.add(_list.get(_current1.nextIndex()).position(), Vector.multiply(
        Vector.add(_list.get(_current1.nextIndex()).tangentVector(),
            Vector.multiply(Vector.add(_vector1, Vector.multiply(_vector2, alpha)), alpha)), alpha));

    float mag = Vector.lerp(_list.get(_current1.nextIndex()).magnitude(),
        _list.get(_current2.nextIndex()).magnitude(), alpha);

    Quaternion q = Quaternion.squad(_list.get(_current1.nextIndex()).orientation(),
        _list.get(_current1.nextIndex()).tangentQuaternion(),
        _list.get(_current2.nextIndex()).tangentQuaternion(),
        _list.get(_current2.nextIndex()).orientation(), alpha);

    frame().setPosition(pos);
    frame().setRotation(q);
    frame().setMagnitude(mag);
  }
}
