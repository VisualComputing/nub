/***************************************************************************************
 * nub
 * Copyright (c) 2019-2021 Universidad Nacional de Colombia
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * An internal keyframe Catmull-Rom interpolator class having the
 * keyframes to be interpolated by a given node. See to
 * {@link Node#animate()}, {@link Node#addKeyFrame()},
 * {@link Node#setAnimationTime(float)} and {@link Node#interpolate(float)},
 * among others.
 */
class Interpolator {
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
    protected boolean _handled;
    protected Node _node;

    KeyFrame(Node node, float time, boolean handled) {
      _node = node;
      _time = time;
      _handled = handled;
    }

    protected KeyFrame(KeyFrame other) {
      if (other._handled) {
        this._node = other._node.copy(false);
      }
      else {
        this._node = other._node;
      }
      this._time = other._time;
      this._handled = other._handled;
    }

    public KeyFrame copy() {
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
      return node().reference() == _node.reference() ? _node.position() :
          node().reference() == null ? _node.worldPosition() : node().reference().location(_node.worldPosition());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.position() : node().reference().location(_node.position());
    }

    /**
     * Returns the key-frame rotation respect to the {@link #node()} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #node()} {@link Node#reference()}.
     */
    protected Quaternion _rotation() {
      return node().reference() == _node.reference() ? _node.orientation() :
          node().reference() == null ? _node.worldOrientation() : node().reference().displacement(_node.worldOrientation());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.orientation() : node().reference().displacement(_node.orientation());
    }

    /**
     * Returns the key-frame scaling respect to the {@link #node()} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #node()} {@link Node#reference()}.
     */
    protected float _scaling() {
      return node().reference() == _node.reference() ? _node.magnitude() :
          node().reference() == null ? _node.worldMagnitude() : node().reference().displacement(_node.worldMagnitude());
      // perhaps less efficient but simpler equivalent form:
      // return node().reference() == null ? _node.magnitude() : node().reference().displacement(_node.magnitude());
    }
  }

  protected long _lastUpdate;
  // Attention: We should go like this: protected Map<Float, Node> _list;
  // but Java doesn't allow to iterate backwards a map
  protected List<KeyFrame> _list;
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

  /**
   * Creates an interpolator for the given {@code node}. Note that
   * {@link #time()}, {@link #speed()} are set to their default values.
   * <p>
   * Note that the interpolator {@link Task} is decoupled from this class
   * and thus should be handled i.e., (un)register, outside this class.
   */
  public Interpolator(Node node) {
    _list = new ArrayList<KeyFrame>();
    _path = new ArrayList<Node>();
    setNode(node);
    _t = 0.0f;
    _speed = 1.0f;
    _recurrent = false;
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentKeyFrameValid = false;
    _splineCacheIsValid = false;
    _backwards = _list.listIterator();
    _forwards = _list.listIterator();
  }

  protected Interpolator(Interpolator other) {
    this._list = new ArrayList<KeyFrame>();
    for (KeyFrame element : other._list) {
      KeyFrame keyFrame = element.copy();
      this._list.add(keyFrame);
    }
    this._path = new ArrayList<Node>();
    this.setNode(other.node());
    this._t = other._t;
    this._speed = other._speed;
    if (Graph.TimingHandler.isTaskRegistered(other._task)) {
      this._task = new Task(() -> Interpolator.this._execute());
      this._task.setPeriod(other._task.period());
    }
    this._recurrent = other._recurrent;
    this._pathIsValid = false;
    this._valuesAreValid = false;
    this._currentKeyFrameValid = false;
    this._splineCacheIsValid = false;
    this._backwards = _list.listIterator();
    this._forwards = _list.listIterator();
  }

  // TODO copy is experimental

  /**
   * Returns a deep copy of this interpolator.
   */
  public Interpolator copy() {
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
   * @see #run(float, long)
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
    _t += _speed * _task.period();
    if (time() >= _list.get(_list.size() - 1)._time) {
      if (isRecurrent())
        setTime(_list.get(0)._time + _t - _list.get(_list.size() - 1)._time);
      else {
        // Make sure last KeyFrame is reached and displayed
        interpolate(_list.get(_list.size() - 1)._time);
        _task.stop();
      }
    } else if (time() <= _list.get(0)._time) {
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
    if (Graph.TimingHandler.isTaskRegistered(_task))
      _task.toggle();
  }

  /**
   * Same as {@code task().run()}.
   *
   * @see Task#run()
   * @see #run(float, long)
   * @see #run(float)
   */
  public void run() {
    if (Graph.TimingHandler.isTaskRegistered(_task))
      _task.run();
  }

  /**
   * Sets the time (see {@link #setTime(float)}) and then call {@code task().run()}.
   *
   * @see #run()
   * @see #run(float, long)
   */
  public void run(float time) {
    setTime(time);
    run();
  }

  /**
   * Sets the speed (see {@link #setSpeed(float)}) and then call {@code task().run(period)}.
   *
   * @see #run()
   * @see #run(float, long)
   */
  public void run(float speed, long period) {
    setSpeed(speed);
    if (Graph.TimingHandler.isTaskRegistered(_task))
      _task.run(period);
  }

  /**
   * Starts the interpolation process.
   * <p>
   * A task is started at {@code time } (see {@link #setTime(float)}) which
   * will update the {@link #node()}'s position, orientation and magnitude
   * at the current {@link #time()}.
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
  public void run(float time, float speed, int period) {
    setTime(time);
    setSpeed(speed);
    if (Graph.TimingHandler.isTaskRegistered(_task))
      _task.run(period);
  }

  /**
   * Stops the interpolation and resets {@link #time()} to the {@link #firstTime()}.
   * <p>
   * If desired, call {@link #interpolate(float)} after this method to actually move
   * the {@link #node()} to {@link #firstTime()}.
   */
  public void reset() {
    if (Graph.TimingHandler.isTaskRegistered(_task))
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
   * Returns the current interpolation time (in milliseconds) along the interpolator
   * path.
   * <p>
   * This time is regularly updated when {@link Task#isActive()}. Can be set
   * directly with {@link #setTime(float)} or {@link #interpolate(float)}.
   */
  public float time() {
    return _t;
  }

  /**
   * Returns the duration of the interpolator path, expressed in milliseconds.
   * <p>
   * Simply corresponds to {@link #lastTime()} - {@link #firstTime()}. Returns 0 if the
   * path has less than 2 keyframes.
   */
  public float duration() {
    return lastTime() - firstTime();
  }

  /**
   * Returns the time corresponding to the first keyframe, expressed in milliseconds.
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
   * Returns the time corresponding to the last keyframe, expressed in milliseconds.
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
   * {@code speed} which is set with {@link #run(float, long)}) or
   * {@link #lastTime()}.
   * <p>
   * {@link #time()} is otherwise reset to {@link #firstTime()} (+
   * {@link #time()} - {@link #lastTime()}) (and inversely for negative
   * {@code speed} which is set with {@link #run(float, long)}) and
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
   * Same as {@code addKeyFrame(1000)}.
   *
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node)
   */
  public void addKeyFrame() {
    addKeyFrame(1000);
  }

  /**
   * Same as {@code addKeyFrame(node().hint(), time)}.
   *
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(Node)
   */
  public void addKeyFrame(float time) {
    addKeyFrame(node().hint(), time);
  }

  /**
   * Adds a {@link #node()} copy as a keyframe at {@code time} and a mask {@code hint}.
   */
  public void addKeyFrame(int hint, float time) {
    _addKeyFrame(node()._copy(hint, true), time, true);
  }

  /**
   * Same as {@code addKeyFrame(node, _list.isEmpty() ? 0.0f : 1000.0f)}.
   *
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(Node node) {
    addKeyFrame(node, _list.isEmpty() ? 0.0f : 1000.0f);
  }

  /**
   * Appends a new keyframe {@code time} milliseconds after the previously added one.
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
  public void addKeyFrame(Node node, float time) {
    _addKeyFrame(node, time, false);
  }

  protected void _addKeyFrame(Node node, float time, boolean handled) {
    if (_list.size() == 0) {
      if (time < 0)
        return;
    } else if (time <= 0)
      return;
    if (node == null)
      return;
    _list.add(new KeyFrame(node, _list.isEmpty() ? time : _list.get(_list.size() - 1)._time + time, handled));
    if (handled) {
      node.tagging = node().isHintEnabled(Node.KEYFRAMES);
      node.cull = !node.tagging;
    }
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentKeyFrameValid = false;
    reset();
  }

  /**
   * Remove the closest keyframe to {@code time} and returns it.
   * May return {@code null} if the interpolator is empty.
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
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentKeyFrameValid = false;
    boolean rerun = false;
    if (Graph.TimingHandler.isTaskRegistered(_task))
      rerun = _task.isActive();
    if (rerun) {
      _task.stop();
    }
    _list.remove(index);
    setTime(firstTime());
    if (rerun) {
      if (_list.size() > 1)
        _task.run();
      else
        interpolate(0);
    }
    if (keyFrame._handled) {
      keyFrame._node.detach();
    }
    return keyFrame._node;
  }

  /**
   * Removes all keyframes from the path.
   *
   * @see Node#detach()
   */
  public void clear() {
    if (Graph.TimingHandler.isTaskRegistered(_task))
      _task.stop();
    ListIterator<KeyFrame> it = _list.listIterator();
    while (it.hasNext()) {
      KeyFrame keyFrame = it.next();
      if (keyFrame._handled) {
        keyFrame._node.detach();
      }
    }
    _list.clear();
    _path.clear();
    _pathIsValid = false;
    _valuesAreValid = false;
    _currentKeyFrameValid = false;
  }

  /**
   * Interpolate {@link #node()} at time {@code time} (expressed in milliseconds).
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
    node().setPosition(pos);
    node().setOrientation(q);
    node().setMagnitude(mag);
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
        _path.add(new Node(_list.get(0)._node.worldPosition(), _list.get(0)._node.worldOrientation(), _list.get(0)._node.worldMagnitude(), false));
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
          Vector pdiff = Vector.subtract(keyFrames[2]._node.worldPosition(), keyFrames[1]._node.worldPosition());
          Vector pvec1 = Vector.add(Vector.multiply(pdiff, 3.0f), Vector.multiply(keyFrames[1]._tangentVector(), (-2.0f)));
          pvec1 = Vector.subtract(pvec1, keyFrames[2]._tangentVector());
          Vector pvec2 = Vector.add(Vector.multiply(pdiff, (-2.0f)), keyFrames[1]._tangentVector());
          pvec2 = Vector.add(pvec2, keyFrames[2]._tangentVector());
          for (int step = 0; step < Node.maxSteps; ++step) {
            float alpha = step / (float) Node.maxSteps;
            Node node = new Node(
                    Vector.add(keyFrames[1]._node.worldPosition(), Vector.multiply(Vector.add(keyFrames[1]._tangentVector(), Vector.multiply(Vector.add(pvec1, Vector.multiply(pvec2, alpha)), alpha)), alpha)),
                    Quaternion.squad(keyFrames[1]._node.worldOrientation(), keyFrames[1]._tangentQuaternion(), keyFrames[2]._tangentQuaternion(), keyFrames[2]._node.worldOrientation(), alpha),
                    Vector.lerp(keyFrames[1]._node.worldMagnitude(), keyFrames[2]._node.worldMagnitude(), alpha), false);
            node._setHint(node(), node()._keyframesMask);
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
        _path.add(new Node(keyFrames[1]._node.worldPosition(), keyFrames[1]._node.worldOrientation(), keyFrames[1]._node.worldMagnitude(),false));
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
        if (Quaternion.dot(next._node.orientation(), keyFrame._node.orientation()) < 0) {
          // change sign
          next._node.orientation().negate();
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
      _lastUpdate = Graph.frameCount;
      _valuesAreValid = false;
      _pathIsValid = false;
      _splineCacheIsValid = false;
    }
  }
}
