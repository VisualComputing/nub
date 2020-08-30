/***************************************************************************************
 * nub
 * Copyright (c) 2019-2020 Universidad Nacional de Colombia
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A simple, expressive, language-agnostic, and extensible visual
 * computing library, featuring interaction, visualization and animation frameworks and
 * supporting advanced (onscreen/offscreen) (real/non-real time) rendering techniques.
 * Released under the terms of the GPLv3, refer to: http://www.gnu.org/licenses/gpl.html
 ***************************************************************************************/

package nub.core;

import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.timing.Task;
import nub.timing.TimingHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * A keyframe Catmull-Rom interpolator.
 * <p>
 * An interpolator holds keyframes (that define a path) and, optionally, a
 * reference to a node of your application (which will be interpolated).
 * In this case, when the user call {@link #run()}, an interpolator
 * {@link #task()} will regularly updates the {@link #node()} position,
 * orientation and magnitude along the path.
 * <p>
 * Here is a typical usage example:
 * <pre>
 * {@code
 * void setup() {
 *   Graph graph = new Graph(1200, 800);
 *   Interpolator interpolator = new Interpolator();
 *   for (int i = 0; i < 10; i++)
 *     interpolator.addKeyFrame(scene.randomNode());
 *   interpolator.run();
 * }
 * }
 * </pre>
 * which will create a random (see {@link Node#random(Graph)}) interpolator path
 * containing 10 keyframes (see {@link #addKeyFrame(Node)}). The interpolation is
 * also run (see {@link #run()}). The interpolation is stopped when {@link #time()} is
 * greater than the {@link #lastTime()} (unless {@link #enableRecurrence()} is called).
 * <p>
 * The interpolator visual representation may be configured using the following hints:
 * {@link #SPLINE} and {@link #STEPS}. See {@link #hint()},
 * {@link #configHint(int, Object...)} {@link #enableHint(int)},
 * {@link #enableHint(int, Object...)}, {@link #disableHint(int)}, {@link #toggleHint(int)}
 * and {@link #resetHint()}.
 * <b>Attention:</b> If a {@link nub.core.constraint.Constraint} is attached to
 * the {@link #node()} (see {@link Node#constraint()}), it should be reset before
 * {@link #run()} is called, otherwise the interpolated motion (computed as if
 * there was no constraint) will probably be erroneous.
 */
public class Interpolator {
  /**
   * Returns whether or not this interpolator matches other.
   *
   * @param interpolator other interpolator
   */
  public boolean matches(Interpolator interpolator) {
    for (int i = 0; i < _list.size(); i++)
      if (!_list.get(i).matches(interpolator._list.get(i)))
        return false;
    return true;
  }

  /**
   * Internal protected class representing 2d and 3d key-frames. It's just
   * a time-node pairing with vector and quaternion tangent caches.
   */
  protected class KeyFrame {
    /**
     * Returns whether or not this keyframe matches the {@code other}.
     *
     * @param keyFrame other keyFrame
     */
    public boolean matches(KeyFrame keyFrame) {
      return node().matches(keyFrame._node) && _time == keyFrame._time;
    }

    protected Quaternion _tangentQuaternion;
    protected Vector _tangentVector;
    protected float _time;
    protected Node _node;
    protected int _hint;
    protected int _cacheHint;

    KeyFrame(Node node, float time) {
      this(node, node.hint(), time);
    }

    KeyFrame(Node node, int hint, float time) {
      _node = node;
      _cacheHint = node.hint();
      _hint = hint;
      _time = time;
      if (isHintEnabled(SPLINE) && _hint != _cacheHint)
        _node._mask = _hint;
    }

    protected KeyFrame(KeyFrame other) {
      this._time = other._time;
      this._node = other._node.get();
      this._hint = other._hint;
      this._cacheHint = other._cacheHint;
    }

    public KeyFrame get() {
      return new KeyFrame(this);
    }

    /**
     * Returns the cache {@code _tangentVector} world-view. Good for drawing. See {@link #_updatePath()}.
     */
    protected Vector _tangentVector() {
      return node().reference() == null ? _tangentVector : node().reference().worldDisplacement(_tangentVector);
    }

    /**
     * Returns the cache {@code _tangentQuaternion} world-view. Good for drawing. See {@link #_updatePath()}.
     */
    protected Quaternion _tangentQuaternion() {
      return node().reference() == null ? _tangentQuaternion : node().reference().worldDisplacement(_tangentQuaternion);
    }

    /**
     * Returns the key-frame translation respect to the {@link #node()} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #node()} {@link Node#reference()}.
     */
    protected Vector _translation() {
      return node().reference() == _node.reference() ? _node.translation() :
          node().reference() == null ? _node.position() : node().reference().location(_node.position());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.position() : node().reference().location(_node.position());
    }

    /**
     * Returns the key-frame rotation respect to the {@link #node()} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #node()} {@link Node#reference()}.
     */
    protected Quaternion _rotation() {
      return node().reference() == _node.reference() ? _node.rotation() :
          node().reference() == null ? _node.orientation() : node().reference().displacement(_node.orientation());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.orientation() : node().reference().displacement(_node.orientation());
    }

    /**
     * Returns the key-frame scaling respect to the {@link #node()} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #node()} {@link Node#reference()}.
     */
    protected float _scaling() {
      return node().reference() == _node.reference() ? _node.scaling() :
          node().reference() == null ? _node.magnitude() : node().reference().displacement(_node.magnitude());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.magnitude() : node().reference().displacement(_node.magnitude());
    }
  }

  protected long _lastUpdate;
  // Attention: We should go like this: protected Map<Float, Node> _list;
  // but Java doesn't allow to iterate backwards a map
  protected List<KeyFrame> _list;
  public static int maxSteps = 30;
  protected ListIterator<KeyFrame> _backwards;
  protected ListIterator<KeyFrame> _forwards;
  protected List<Node> _path;

  // Main node
  protected Node _node;

  // Beat
  protected Task _task;
  protected float _t;
  protected float _speed;

  // Misc
  protected boolean _recurrent;

  // Cached values and flags
  protected boolean _pathIsValid;
  protected boolean _valuesAreValid;
  protected boolean _currentKeyFrameValid;
  protected boolean _splineCacheIsValid;
  protected Vector _vector1, _vector2;

  // Visual hint
  protected int _mask;
  public final static int SPLINE = 1 << 0;
  public final static int STEPS = 1 << 1;
  protected int _splineStroke;
  protected int _splineWeight;
  protected int _steps;
  protected int _stepsHint;

  /**
   * Creates an interpolator, with {@code node} as associated {@link #node()}.
   * <p>
   * The {@link #node()} can be set or changed using {@link #setNode(Node)}.
   * <p>
   * {@link #time()}, {@link #speed()} are set to their default values.
   */
  public Interpolator(Node node) {
    _list = new ArrayList<KeyFrame>();
    _path = new ArrayList<Node>();
    setNode(node);
    _t = 0.0f;
    _speed = 1.0f;
    _task = new nub.processing.TimingTask(() -> Interpolator.this._execute());
    _recurrent = false;
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentKeyFrameValid = false;
    _splineCacheIsValid = false;
    _backwards = _list.listIterator();
    _forwards = _list.listIterator();
    // hints
    // magenta (color(255, 0, 255)) encoded as a processing int rgb color
    _splineStroke = -65281;
    _splineWeight = 3;
    _steps = 3;

    // TODO deprecated
    // hack (refer to Node.get())
    if (node().isHintEnabled(Node.SHAPE) && node()._imrShape != null || node()._rmrShape != null) {
      //if (node()._imrShape != null || node()._rmrShape != null) {
      _stepsHint = Node.SHAPE;
    } else {
      if (node().isEye()) {
        _stepsHint = Node.CAMERA;
      } else {
        _stepsHint = Node.AXES;
      }
    }
  }

  protected Interpolator(Interpolator other) {
    this._list = new ArrayList<KeyFrame>();
    for (KeyFrame element : other._list) {
      KeyFrame keyFrame = element.get();
      this._list.add(keyFrame);
    }
    this._path = new ArrayList<Node>();
    this.setNode(other.node());
    this._t = other._t;
    this._speed = other._speed;
    this._task = new nub.processing.TimingTask(() -> Interpolator.this._execute());
    this._task.setPeriod(other.task().period());
    this._task.enableConcurrence(other._task.isConcurrent());
    this._recurrent = other._recurrent;
    this._pathIsValid = false;
    this._valuesAreValid = false;
    this._currentKeyFrameValid = false;
    this._splineCacheIsValid = false;
    this._backwards = _list.listIterator();
    this._forwards = _list.listIterator();
    // hints
    this._splineStroke = other._splineStroke;
    this._splineWeight = other._splineWeight;
    this._steps = other._steps;
    this._stepsHint = other._stepsHint;
  }

  /**
   * Returns a deep copy of this interpolator.
   */
  public Interpolator get() {
    return new Interpolator(this);
  }

  /**
   * Internal use. Called by {@link #_checkValidity()}.
   */
  protected long _lastUpdate() {
    return _lastUpdate;
  }

  /**
   * Sets the interpolator {@link #node()}.
   */
  public void setNode(Node node) {
    if (node == null) {
      throw new RuntimeException("Interpolator node should be non-null!");
    }
    if (_node != node) {
      _node = node;
      _pathIsValid = false;
    }
  }

  /**
   * Returns the node that is to be interpolated by the interpolator.
   * <p>
   * When {@link Task#isActive()}, this node's position, orientation and
   * magnitude will regularly be updated by a task, so that they follow the
   * interpolator path.
   * <p>
   * Set using {@link #setNode(Node)} or with the interpolator constructor.
   */
  public Node node() {
    return _node;
  }

  /**
   * Returns the number of keyframes used by the interpolation. Use
   * {@link #addKeyFrame(Node)} to add new keyframes.
   */
  public int size() {
    return _list.size();
  }

  /**
   * Returns the low-level timing task. Prefer the high-level-api instead:
   * {@link #run()}, {@link #reset()}, {@link #time()} ({@link #setTime(float)})
   * and {@link #toggle()}. Useful if you need to customize the low-level
   * timing-task, e.g., to enable concurrency on it.
   */
  public Task task() {
    return _task;
  }

  /**
   * Updates the {@link #node()} state at the current {@link #time()} and
   * then increments it by {@link Task#period()} * {@link #speed()} ms.
   * This method is called by an interpolator task (see {@link Task}) when
   * the interpolation is running.
   * <p>
   * The above mechanism ensures that the number of interpolation steps is
   * constant and equal to the total path {@link #duration()} divided by the
   * {@link Task#period()} * {@link #speed()} which is is especially useful
   * for benchmarking or movie creation (constant number of snapshots). Note
   * that if {@code speed = 1} then {@link #time()} will be matched
   * during the interpolation (provided that your main loop is fast enough).
   * <p>
   * Note that {@link Task#stop()} is called when {@link #time()} reaches
   * {@link #firstTime()} or {@link #lastTime()}, unless {@link #isRecurrent()}
   * is {@code true}.
   *
   * @see #run(int, float)
   * @see #time()
   */
  protected void _execute() {
    if ((_list.isEmpty()) || (node() == null))
      return;
    if ((_speed > 0.0) && (time() >= _list.get(_list.size() - 1)._time))
      setTime(_list.get(0)._time);
    if ((_speed < 0.0) && (time() <= _list.get(0)._time))
      setTime(_list.get(_list.size() - 1)._time);
    interpolate(time());
    _t += _speed * _task.period() / 1000.0f;
    if (time() > _list.get(_list.size() - 1)._time) {
      if (isRecurrent())
        setTime(_list.get(0)._time + _t - _list.get(_list.size() - 1)._time);
      else {
        // Make sure last KeyFrame is reached and displayed
        interpolate(_list.get(_list.size() - 1)._time);
        _task.stop();
      }
    } else if (time() < _list.get(0)._time) {
      if (isRecurrent())
        setTime(_list.get(_list.size() - 1)._time - _list.get(0)._time + _t);
      else {
        // Make sure first KeyFrame is reached and displayed
        interpolate(_list.get(0)._time);
        _task.stop();
      }
    }
  }

  /**
   * Same as {@code task().toggle()}.
   *
   * @see Task#toggle()
   */
  public void toggle() {
    _task.toggle();
  }

  /**
   * Same as {@code task().run()}.
   *
   * @see Task#run()
   * @see #run(int, float)
   * @see #run(float)
   */
  public void run() {
    _task.run();
  }

  /**
   * Sets the speed ({@link #setSpeed(float)}) and then call {@code task().run()}.
   *
   * @see #run()
   * @see #run(int, float)
   */
  public void run(float speed) {
    setSpeed(speed);
    _task.run();
  }

  /**
   * Starts the interpolation process.
   * <p>
   * A task is started which will update the {@link #node()}'s position,
   * orientation and magnitude at the current {@link #time()}.
   * <p>
   * If {@link #time()} is larger than {@link #lastTime()},
   * {@link #time()} is reset to {@link #firstTime()} before interpolation
   * starts (and conversely for negative {@code speed}.
   * <p>
   * Use {@link #setTime(float)} before calling this method to change the
   * starting {@link #time()}.
   * <p>
   * Note that {@link Task#isActive()} will return {@code true} until
   * {@link Task#stop()} is called.
   * <p>
   * <b>Attention:</b> The keyframes must be defined (see
   * {@link #addKeyFrame(Node, float)}) before you start(), or else
   * the interpolation will naturally immediately stop.
   *
   * @see #run()
   * @see #run(float)
   */
  public void run(int period, float speed) {
    setSpeed(speed);
    _task.run(period);
  }

  /**
   * Stops the interpolation and resets {@link #time()} to the {@link #firstTime()}.
   * <p>
   * If desired, call {@link #interpolate(float)} after this method to actually move
   * the {@link #node()} to {@link #firstTime()}.
   */
  public void reset() {
    _task.stop();
    setTime(firstTime());
  }

  /**
   * Sets the {@link #time()}.
   *
   * <b>Attention:</b> The {@link #node()} state is not affected by this method. Use this
   * function to define the starting time of a future interpolation (see
   * {@link #run()}). Use {@link #interpolate(float)} to actually
   * interpolate at a given time.
   */
  public void setTime(float time) {
    _t = time;
  }

  /**
   * Returns the current interpolation time (in seconds) along the interpolator
   * path.
   * <p>
   * This time is regularly updated when {@link Task#isActive()}. Can be set
   * directly with {@link #setTime(float)} or {@link #interpolate(float)}.
   */
  public float time() {
    return _t;
  }

  /**
   * Returns the duration of the interpolator path, expressed in seconds.
   * <p>
   * Simply corresponds to {@link #lastTime()} - {@link #firstTime()}. Returns 0 if the
   * path has less than 2 keyframes.
   */
  public float duration() {
    return lastTime() - firstTime();
  }

  /**
   * Returns the time corresponding to the first keyframe, expressed in seconds.
   * <p>
   * Returns 0 if the path is empty.
   *
   * @see #lastTime()
   * @see #duration()
   * @see #time()
   */
  public float firstTime() {
    return _list.isEmpty() ? 0.0f : _list.get(0)._time;
  }

  /**
   * Returns the time corresponding to the last keyframe, expressed in seconds.
   *
   * @see #firstTime()
   * @see #duration()
   * @see #time()
   */
  public float lastTime() {
    return _list.isEmpty() ? 0.0f : _list.get(_list.size() - 1)._time;
  }

  /**
   * Convenience function that simply calls {@code enableRecurrence(false)}.
   */
  public void disableRecurrence() {
    enableRecurrence(false);
  }

  /**
   * Convenience function that simply calls {@code enableRecurrence(true)}.
   */
  public void enableRecurrence() {
    enableRecurrence(true);
  }

  /**
   * Sets the {@link #isRecurrent()} value.
   */
  public void enableRecurrence(boolean enable) {
    _recurrent = enable;
  }

  /**
   * Returns {@code true} when the interpolation is played in an infinite loop.
   * <p>
   * When {@code false} (default), the interpolation stops when
   * {@link #time()} reaches {@link #firstTime()} (with negative
   * {@code speed} which is set with {@link #run(int, float)}) or
   * {@link #lastTime()}.
   * <p>
   * {@link #time()} is otherwise reset to {@link #firstTime()} (+
   * {@link #time()} - {@link #lastTime()}) (and inversely for negative
   * {@code speed} which is set with {@link #run(int, float)}) and
   * interpolation continues.
   */
  public boolean isRecurrent() {
    return _recurrent;
  }

  /**
   * Returns the current interpolation speed.
   * <p>
   * Default value is 1, which means {@link #time()} will be matched during
   * the interpolation (provided that your main loop is fast enough).
   * <p>
   * A negative value will result in a reverse interpolation of the keyframes.
   *
   * @see #setSpeed(float)
   * @see Task#period()
   */
  public float speed() {
    return _speed;
  }

  /**
   * Same as {@code setSpeed(speed() + delta)}.
   *
   * @see #speed()
   * @see #setSpeed(float)
   * @see Task#increasePeriod(long)
   */
  public void increaseSpeed(float delta) {
    setSpeed(speed() + delta);
  }

  /**
   * Sets the {@link #speed()}. Negative values are allowed.
   *
   * @see #speed()
   * @see #increaseSpeed(float)
   * @see Task#period()
   */
  public void setSpeed(float speed) {
    _speed = speed;
  }

  /**
   * Returns the collection of keyframes represented as a map of
   * time to node pairings.
   */
  public HashMap<Float, Node> keyFrames() {
    HashMap map = new HashMap<Float, Node>();
    for (KeyFrame keyFrame : _list)
      map.put(keyFrame._time, keyFrame._node);
    return map;
  }

  /**
   * Same as {@code addKeyFrame(1)}.
   *
   * @see #addKeyFrame(Node, int, float)
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node)
   */
  public void addKeyFrame() {
    addKeyFrame(1);
  }

  /**
   * Same as
   * {@code addKeyFrame(node().isEye() ? (Node.CAMERA | Node.BULLSEYE) : node().hint() == 0 ? (Node.AXES | Node.BULLSEYE) : node().hint(), time)}.
   *
   * @see #addKeyFrame(Node, int, float)
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node)
   */
  public void addKeyFrame(float time) {
    addKeyFrame(node().isEye() ? (Node.CAMERA | Node.BULLSEYE) : node().hint() == 0 ? (Node.AXES | Node.BULLSEYE) : node().hint(), time);
  }

  /**
   * Same as {@code addKeyFrame(node().get(), hint, time)}.
   *
   * @see #addKeyFrame(Node, int, float)
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(int hint, float time) {
    addKeyFrame(node().get(), hint, time);
  }

  /**
   * Same as {@code addKeyFrame(node, _list.isEmpty() ? 0.0f : 1.0f)}.
   *
   * @see #addKeyFrame(Node, int, float)
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(Node node) {
    addKeyFrame(node, _list.isEmpty() ? 0.0f : 1.0f);
  }

  /**
   * Same as
   * {@code addKeyFrame(node, node.isEye() ? (Node.CAMERA | Node.BULLSEYE) : node.hint() == 0 ? (Node.AXES | Node.BULLSEYE) : node().hint(), time)}.
   *
   * @see #addKeyFrame(Node, int, float)
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(Node node, float time) {
    addKeyFrame(node, node.isEye() ? (Node.CAMERA | Node.BULLSEYE) : node.hint() == 0 ? (Node.AXES | Node.BULLSEYE) : node().hint(), time);
  }

  /**
   * Appends a new keyframe which will display the node {@code hint} when the interpolator
   * {@link #SPLINE} hint is enabled, {@code time} seconds after the previously added one.
   * <p>
   * Note that when {@code node} is modified, the interpolator path is updated accordingly.
   * This allows for dynamic paths, where keyframes can be edited, even during the
   * interpolation.
   * <p>
   * {@code null} node references are silently ignored.
   *
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(Node node, int hint, float time) {
    if (_list.size() == 0) {
      if (time < 0)
        return;
    } else if (time <= 0)
      return;
    if (node == null)
      return;
    _list.add(new KeyFrame(node, hint, _list.isEmpty() ? time : _list.get(_list.size() - 1)._time + time));
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentKeyFrameValid = false;
    reset();
  }

  /**
   * Remove the closest keyframe to {@code time} and returns it.
   * May return {@code null} is the interpolator is empty.
   */
  public Node removeKeyFrame(float time) {
    if (_list.isEmpty())
      return null;
    int index = 0;
    if (_list.size() > 1 && time >= 0) {
      float previousTime = firstTime();
      float currentTime;
      while (index < _list.size()) {
        currentTime = _list.get(index)._time;
        if (currentTime < time) {
          previousTime = currentTime;
        } else {
          if (time - previousTime < currentTime - time & index > 0)
            index--;
          break;
        }
        index++;
      }
    }
    if (index == _list.size())
      index--;
    KeyFrame keyFrame = _list.get(index);
    keyFrame._node._mask = keyFrame._cacheHint;
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentKeyFrameValid = false;
    boolean rerun = _task.isActive();
    if (rerun) {
      _task.stop();
    }
    _list.remove(index);
    setTime(firstTime());
    if (rerun /* && _list.size() > 1 */)
      _task.run();
    return keyFrame._node;
  }

  /**
   * Removes all keyframes from the path.
   *
   * @see Graph#prune(Node)
   */
  public void clear() {
    _task.stop();
    ListIterator<KeyFrame> it = _list.listIterator();
    while (it.hasNext()) {
      KeyFrame keyFrame = it.next();
      Graph.prune(keyFrame._node);
    }
    _list.clear();
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentKeyFrameValid = false;
  }

  /**
   * Interpolate {@link #node()} at time {@code time} (expressed in seconds).
   * {@link #time()} is set to {@code time} and {@link #node()} is set accordingly.
   * <p>
   * If you simply want to change {@link #time()} but not the
   * {@link #node()} state, use {@link #setTime(float)} instead.
   */
  public void interpolate(float time) {
    this._checkValidity();
    setTime(time);
    if ((_list.isEmpty()) || (node() == null))
      return;
    if (!_valuesAreValid)
      _updateModifiedKeyFrames();
    _updateCurrentKeyFrameForTime(time);
    if (!_splineCacheIsValid)
      _updateSplineCache();
    float alpha;
    float dt = _list.get(_forwards.nextIndex())._time - _list.get(_backwards.nextIndex())._time;
    if (dt == 0)
      alpha = 0.0f;
    else
      alpha = (time - _list.get(_backwards.nextIndex())._time) / dt;
    Vector pos = Vector.add(_list.get(_backwards.nextIndex())._translation(), Vector.multiply(
        Vector.add(_list.get(_backwards.nextIndex())._tangentVector,
            Vector.multiply(Vector.add(_vector1, Vector.multiply(_vector2, alpha)), alpha)), alpha));
    float mag = Vector.lerp(_list.get(_backwards.nextIndex())._scaling(),
        _list.get(_forwards.nextIndex())._scaling(), alpha);
    Quaternion q = Quaternion.squad(_list.get(_backwards.nextIndex())._rotation(),
        _list.get(_backwards.nextIndex())._tangentQuaternion,
        _list.get(_forwards.nextIndex())._tangentQuaternion,
        _list.get(_forwards.nextIndex())._rotation(), alpha);
    node().setTranslation(pos);
    node().setRotation(q);
    node().setScaling(mag);
  }

  /**
   * Internal use.
   */
  protected void _updateCurrentKeyFrameForTime(float time) {
    // TODO: Special case for loops when closed path is implemented !!
    if (!_currentKeyFrameValid)
      _backwards = _list.listIterator();
    while (_list.get(_backwards.nextIndex())._time > time) {
      _currentKeyFrameValid = false;
      if (!_backwards.hasPrevious())
        break;
      _backwards.previous();
    }
    if (!_currentKeyFrameValid)
      _forwards = _list.listIterator(_backwards.nextIndex());
    while (_list.get(_forwards.nextIndex())._time < time) {
      _currentKeyFrameValid = false;
      if (!_forwards.hasNext())
        break;
      _forwards.next();
    }
    if (!_currentKeyFrameValid) {
      _backwards = _list.listIterator(_forwards.nextIndex());
      if ((_backwards.hasPrevious()) && (time < _list.get(_forwards.nextIndex())._time))
        _backwards.previous();
      _currentKeyFrameValid = true;
      _splineCacheIsValid = false;
    }
  }

  /**
   * Internal use. Used by {@link #interpolate(float)}.
   */
  protected void _updateSplineCache() {
    Vector deltaP = Vector.subtract(_list.get(_forwards.nextIndex())._translation(),
        _list.get(_backwards.nextIndex())._translation());
    _vector1 = Vector.add(Vector.multiply(deltaP, 3.0f), Vector.multiply(_list.get(_backwards.nextIndex())._tangentVector, (-2.0f)));
    _vector1 = Vector.subtract(_vector1, _list.get(_forwards.nextIndex())._tangentVector);
    _vector2 = Vector.add(Vector.multiply(deltaP, (-2.0f)), _list.get(_backwards.nextIndex())._tangentVector);
    _vector2 = Vector.add(_vector2, _list.get(_forwards.nextIndex())._tangentVector);
    _splineCacheIsValid = true;
  }

  /**
   * Computes a path from {@link #keyFrames()} for the interpolator to be drawn.
   * <p>
   * Calls {@link #_updatePath()} and then returns a list of nodes defining the
   * interpolator path (which is different than that of {@link #keyFrames()}).
   * <p>
   * Use it in your interpolator path drawing routine.
   */
  protected List<Node> _path() {
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
      if (_list.isEmpty())
        return;
      if (!_valuesAreValid)
        _updateModifiedKeyFrames();
      if (_list.get(0) == _list.get(_list.size() - 1)) {
        _path.add(Node.detach(_list.get(0)._node.position(), _list.get(0)._node.orientation(), _list.get(0)._node.magnitude()));
      }
      else {
        KeyFrame[] keyFrames = new KeyFrame[4];
        keyFrames[0] = _list.get(0);
        keyFrames[1] = keyFrames[0];
        int index = 1;
        keyFrames[2] = (index < _list.size()) ? _list.get(index) : null;
        index++;
        keyFrames[3] = (index < _list.size()) ? _list.get(index) : null;
        while (keyFrames[2] != null) {
          Vector pdiff = Vector.subtract(keyFrames[2]._node.position(), keyFrames[1]._node.position());
          Vector pvec1 = Vector.add(Vector.multiply(pdiff, 3.0f), Vector.multiply(keyFrames[1]._tangentVector(), (-2.0f)));
          pvec1 = Vector.subtract(pvec1, keyFrames[2]._tangentVector());
          Vector pvec2 = Vector.add(Vector.multiply(pdiff, (-2.0f)), keyFrames[1]._tangentVector());
          pvec2 = Vector.add(pvec2, keyFrames[2]._tangentVector());
          for (int step = 0; step < maxSteps; ++step) {
            float alpha = step / (float) maxSteps;
            Node node = Node.detach(
                Vector.add(keyFrames[1]._node.position(), Vector.multiply(Vector.add(keyFrames[1]._tangentVector(), Vector.multiply(Vector.add(pvec1, Vector.multiply(pvec2, alpha)), alpha)), alpha)),
                Quaternion.squad(keyFrames[1]._node.orientation(), keyFrames[1]._tangentQuaternion(), keyFrames[2]._tangentQuaternion(), keyFrames[2]._node.orientation(), alpha),
                Vector.lerp(keyFrames[1]._node.magnitude(), keyFrames[2]._node.magnitude(), alpha));
            if (step % Interpolator.maxSteps != 0) {
              node._mask = _stepsHint;
              if (node.isHintEnabled(Node.SHAPE) && (node()._imrShape != null || node()._rmrShape != null)) {
                node.setShape(node());
              }
            }
            node._torusFaces = node()._torusFaces;
            node._torusColor = node()._torusColor;
            node._bullsEyeStroke = node()._bullsEyeStroke;
            node._cameraStroke = node()._cameraStroke;
            node._axesLength = node()._axesLength;
            _path.add(node);
          }
          // Shift
          keyFrames[0] = keyFrames[1];
          keyFrames[1] = keyFrames[2];
          keyFrames[2] = keyFrames[3];
          index++;
          keyFrames[3] = (index < _list.size()) ? _list.get(index) : null;
        }
        // Add last KeyFrame
        _path.add(Node.detach(keyFrames[1]._node.position(), keyFrames[1]._node.orientation(), keyFrames[1]._node.magnitude()));
      }
      _pathIsValid = true;
    }
  }

  /**
   * Internal use.
   */
  protected void _updateModifiedKeyFrames() {
    KeyFrame keyFrame;
    KeyFrame prev = _list.get(0);
    keyFrame = _list.get(0);
    int index = 1;
    while (keyFrame != null) {
      KeyFrame next = (index < _list.size()) ? _list.get(index) : null;
      index++;
      if (next != null) {
        // Interpolate using the shortest path between two quaternions
        // See: https://stackoverflow.com/questions/2886606/flipping-issue-when-interpolating-rotations-using-quaternions
        if (Quaternion.dot(next._node.rotation(), keyFrame._node.rotation()) < 0) {
          // change sign
          next._node.rotation().negate();
        }
        keyFrame._tangentVector = Vector.multiply(Vector.subtract(next._translation(), prev._translation()), 0.5f);
        keyFrame._tangentQuaternion = Quaternion.squadTangent(prev._rotation(), keyFrame._rotation(), next._rotation());
      } else {
        keyFrame._tangentVector = Vector.multiply(Vector.subtract(keyFrame._translation(), prev._translation()), 0.5f);
        keyFrame._tangentQuaternion = Quaternion.squadTangent(prev._rotation(), keyFrame._rotation(), keyFrame._rotation());
      }
      prev = keyFrame;
      keyFrame = next;
    }
    _valuesAreValid = true;
  }

  /**
   * Internal use. Checks if any of the keyframes defining the path was recently modified.
   */
  protected void _checkValidity() {
    boolean modified = false;
    for (KeyFrame keyFrame : _list) {
      if (keyFrame._node.lastUpdate() > _lastUpdate()) {
        modified = true;
        break;
      }
    }
    if (modified) {
      _lastUpdate = TimingHandler.frameCount;
      _valuesAreValid = false;
      _pathIsValid = false;
      _splineCacheIsValid = false;
    }
  }

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
   * single visual hints available the interpolator:
   * <p>
   * <ol>
   * <li>{@link #SPLINE} which displays a Catmull-Rom spline having the key-frames
   * as its control points.</li>
   * <li>{@link #STEPS} which defines what to draw between consecutive key-frames.</li>
   * </ol>
   * Displaying the hint requires first to enabling it (see {@link #enableHint(int)}) and then
   * calling either {@link Graph#render(Node)} or {@link Graph#render()}.
   * Use {@link #configHint(int, Object...)} to configure the hint different visual aspects.
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
    Graph._interpolators.remove(this);
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
    if (_mask == 0) _disabled();
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
    if (_mask != 0) _enabled();
  }

  protected void _enabled() {
    for (KeyFrame keyFrame : _list) {
      if (keyFrame._hint != keyFrame._cacheHint)
        keyFrame._node._mask = keyFrame._hint;
    }
    Graph._interpolators.add(this);
  }

  protected void _disabled() {
    for (KeyFrame keyFrame : _list) {
      if (keyFrame._hint != keyFrame._cacheHint)
        keyFrame._node._mask = keyFrame._cacheHint;
    }
    Graph._interpolators.remove(this);
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
    if (_mask != 0)
      _enabled();
    else
      _disabled();
  }

  /**
   * Configures the hint using varargs as follows:
   * <p>
   * <ol>
   * <li>{@link #SPLINE} hint: {@code configHint(Interpolator.SPLINE, splineStroke)}.</li>
   * <li>{@link #SPLINE} hint: {@code configHint(Interpolator.SPLINE, splineStroke, splineWeight)}.</li>
   * <li>{@link #STEPS} hint: {@code configHint(Interpolator.STEPS, hint)}.</li>
   * </ol>
   * Note that the {@code splineStroke} is a color {@code int} var;{@code splineWeight}
   * is a stroke weight int var; and, {@code hint} is a node {@link Node#hint()} defining
   * what to draw between two consecutives key-frames. See also {@link #setSteps(int)}.
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
        if (Graph.isNumInstance(params[0])) {
          if (hint == SPLINE) {
            _splineStroke = Graph.castToInt(params[0]);
            return;
          }
          if (hint == STEPS) {
             _stepsHint = Graph.castToInt(params[0]);
            return;
          }
        }
        break;
      case 2:
        if (Graph.isNumInstance(params[0]) && Graph.isNumInstance(params[1])) {
          if (hint == SPLINE) {
            _splineStroke = Graph.castToInt(params[0]);
            _splineWeight = Graph.castToInt(params[1]);
            return;
          }
        }
        break;
    }
    System.out.println("Warning: some params in Interpolator.configHint(hint, params) couldn't be parsed!");
  }

  /**
   * Returns the number of steps between two consecutive key-frames
   * to be drawn by the interpolator hint. Sets this value with {@link #setSteps(int)}.
   *
   * @see #hint()
   */
  public int steps() {
    return _steps;
  }

  /**
   * Sets the number of steps between two consecutive key-frames to be drawn
   * by the interpolator hint.
   *
   * @see #steps()
   * @see #hint()
   */
  public void setSteps(int steps) {
    if (0 <= steps && steps < maxSteps)
      _steps = steps;
    else
      System.out.println("Warning: spline steps should be in [0..maxSteps-1]. Nothing done!");
  }

  /**
   * Clear every key-frame cache hint.
   */
  public void clearKeyFrameCacheHint() {
    for (KeyFrame keyFrame : _list) {
      keyFrame._hint = keyFrame._cacheHint;
    }
  }
}
