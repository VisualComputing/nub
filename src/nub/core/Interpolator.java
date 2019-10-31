/******************************************************************************************
 * nub
 * Copyright (c) 2019 Universidad Nacional de Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ******************************************************************************************/

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
 * void init() {
 *   Graph graph = new Graph(1200, 800);
 *   Interpolator interpolator = new Interpolator(graph);
 *   for (int i = 0; i < 10; i++)
 *     interpolator.addKeyFrame(Node.random(graph));
 *   interpolator.start();
 * }
 * }
 * </pre>
 * which will create a random (see {@link Node#random(Graph)}) interpolator path
 * containing 10 keyframes (see {@link #addKeyFrame(Node)}). The interpolation is
 * also started (see {@link #run()}).
 * <p>
 * The graph main drawing loop should look like:
 * <pre>
 * {@code
 * void mainLoop() {
 *   pushMatrix();
 *   graph.applyTransformation(interpolator.node());
 *   // draw your object here. Its position, orientation and magnitude are interpolated.
 *   popMatrix();
 * }
 * }
 * </pre>
 * The interpolation is stopped when {@link #time()} is greater than the
 * {@link #lastTime()} (unless loop() is {@code true}).
 * <p>
 * The
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
   * Internal protected class representing 2d and 3d key-frames.
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

    KeyFrame(Node node, float time) {
      _time = time;
      _node = node;
    }

    protected KeyFrame(KeyFrame other) {
      this._time = other._time;
      this._node = other._node.get();
    }

    public KeyFrame get() {
      return new KeyFrame(this);
    }
  }

  protected long _lastUpdate;
  protected List<KeyFrame> _list;
  protected ListIterator<KeyFrame> _current1;
  protected ListIterator<KeyFrame> _current2;
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

  // Graph
  protected Graph _graph;

  /**
   * Convenience constructor that simply calls {@code this(graph, new Node())}.
   * <p>
   * Creates an anonymous {@link #node()} to be interpolated by this
   * interpolator.
   *
   * @see #Interpolator(Node)
   * @see #Interpolator(Graph, Node)
   */
  public Interpolator(Graph graph) {
    this(graph, new Node());
  }

  /**
   * Same as {@code this(node.graph(), node)}. Note that {@code node} should be attached to
   * a {@link Graph}.
   *
   * @see #Interpolator(Graph)
   * @see #Interpolator(Graph, Node)
   */
  public Interpolator(Node node) {
    this(node.graph(), node);
  }

  /**
   * Creates an interpolator, with {@code node} as associated {@link #node()}.
   * <p>
   * The {@link #node()} can be set or changed using {@link #setNode(Node)}.
   * <p>
   * {@link #time()}, {@link #speed()} are set to their default values.
   */
  public Interpolator(Graph graph, Node node) {
    if (graph == null)
      throw new RuntimeException("Warning: no interpolator instantiated");
    _graph = graph;
    _list = new ArrayList<KeyFrame>();
    _path = new ArrayList<Node>();
    setNode(node);
    _t = 0.0f;
    _speed = 1.0f;
    _task = _graph._initTask(this);
    _task.setPeriod(40);
    _recurrent = false;
    _pathIsValid = false;
    _valuesAreValid = true;
    _currentKeyFrameValid = false;
    _current1 = _list.listIterator();
    _current2 = _list.listIterator();
  }

  protected Interpolator(Interpolator other) {
    this._graph = other._graph;
    this._path = new ArrayList<Node>();
    ListIterator<Node> nodeIt = other._path.listIterator();
    while (nodeIt.hasNext()) {
      this._path.add(nodeIt.next().get());
    }
    this.setNode(other.node());
    this._t = other._t;
    this._speed = other._speed;
    this._task = _graph._initTask(this);
    this._task.setPeriod(other.task().period());
    this._task.enableConcurrence(other._task.isConcurrent());
    this._recurrent = other._recurrent;
    this._pathIsValid = other._pathIsValid;
    this._valuesAreValid = other._valuesAreValid;
    this._currentKeyFrameValid = other._currentKeyFrameValid;
    this._list = new ArrayList<KeyFrame>();
    for (KeyFrame element : other._list) {
      KeyFrame keyFrame = element.get();
      this._list.add(keyFrame);
    }
    this._current1 = _list.listIterator(other._current1.nextIndex());
    this._current2 = _list.listIterator(other._current2.nextIndex());
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
   * Internal use. Updates the last node path was updated. Called by
   * {@link #_checkValidity()}.
   */
  protected void _modified() {
    _lastUpdate = TimingHandler.frameCount;
  }

  /**
   * Internal use. Called by {@link #_checkValidity()}.
   */
  protected long _lastUpdate() {
    return _lastUpdate;
  }

  /**
   * Sets the interpolator {@link #node()}. If node {@link Node#isDetached()},
   * the node graph ({@link Node#graph()}) and {@link #graph()} should match.
   */
  public void setNode(Node node) {
    if (node == _node)
      return;
    if (node.graph() != null)
      if (graph() != node.graph())
        throw new RuntimeException("Node and Interpolator graphs should match");
    _node = node;
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
   * Sets the interpolator {@link #task()}. Useful if for example you need to
   * customize the timing task to enable concurrency on it.
   */
  public void setTask(Task task) {
    _task = task;
  }

  /**
   * Returns the low-level timing task. Prefer the high-level-api instead:
   * {@link #run()}, {@link #reset()}, {@link #time()} ({@link #setTime(float)})
   * and {@link #toggle()}.
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
  public void execute() {
    if ((_list.isEmpty()) || (node() == null))
      return;
    // TODO experimental
    if ((_speed > 0.0) && (time() >= _list.get(_list.size() - 1)._time))
      setTime(_list.get(0)._time);
    if ((_speed < 0.0) && (time() <= _list.get(0)._time))
      setTime(_list.get(_list.size() - 1)._time);
    // */
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
   * Appends the current {@link #graph()} {@link Graph#eye()}
   * one second after the previously added keyframe.
   *
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame(Node, float)
   * @see #graph()
   * @see Node#get()
   * @see Graph#eye()
   */
  public void addKeyFrame() {
    Node node = graph().eye().get();
    node.setPickingThreshold(20);
    addKeyFrame(node);
  }

  /**
   * Appends the current {@link #graph()} {@link Graph#eye()}
   * {@code time} seconds after the previously added keyframe.
   *
   * @see #addKeyFrame()
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame(Node, float)
   * @see #graph()
   * @see Node#get()
   * @see Graph#eye()
   */
  public void addKeyFrame(float time) {
    Node node = graph().eye().get();
    node.setPickingThreshold(20);
    addKeyFrame(node, time);
  }

  /**
   * Appends a new keyframe one second after the previously added one.
   *
   * @see #addKeyFrame(float)
   * @see #addKeyFrame()
   * @see #addKeyFrame(Node, float)
   */
  public void addKeyFrame(Node node) {
    addKeyFrame(node, _list.isEmpty() ? 0.0f : 1.0f);
  }

  /**
   * Appends a new keyframe {@code time} seconds after the previously added one.
   * <p>
   * Note that when {@code node} is modified, the interpolator path is updated accordingly.
   * This allows for dynamic paths, where keyframes can be edited, even during the
   * interpolation.
   * <p>
   * {@code null} node references are silently ignored.
   *
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node)
   * @see #addKeyFrame()
   */
  public void addKeyFrame(Node node, float time) {
    if (_list.size() == 0) {
      if (time < 0)
        return;
    } else if (time <= 0)
      return;
    if (node == null)
      return;
    if (node.graph() != null)
      if (graph() != node.graph()) {
        throw new RuntimeException("Node and Interpolator graphs should match");
      }
    _list.add(new KeyFrame(node, _list.isEmpty() ? time : _list.get(_list.size() - 1)._time + time));
    _valuesAreValid = false;
    _pathIsValid = false;
    _currentKeyFrameValid = false;
    reset();
  }

  /**
   * Remove keyframe according to {@code time}. Calls {@link Task#stop()}
   * and returns {@code true} if the deletion was successful and returns
   * {@code false} otherwise.
   */
  // TODO needs testing
  public boolean removeKeyFrame(float time) {
    HashMap<Float, Node> map = keyFrames();
    Node node = map.get(time);
    if (node != null) {
      _valuesAreValid = false;
      _pathIsValid = false;
      _currentKeyFrameValid = false;
      if (_task.isActive())
        _task.stop();
      _list.remove(node);
      setTime(firstTime());
      _graph.prune(node);
      return true;
    }
    return false;
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
      _graph.prune(keyFrame._node);
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
    float dt = _list.get(_current2.nextIndex())._time - _list.get(_current1.nextIndex())._time;
    if (dt == 0)
      alpha = 0.0f;
    else
      alpha = (time - _list.get(_current1.nextIndex())._time) / dt;
    Vector pos = Vector.add(_list.get(_current1.nextIndex())._node.position(), Vector.multiply(
        Vector.add(_list.get(_current1.nextIndex())._tangentVector,
            Vector.multiply(Vector.add(_vector1, Vector.multiply(_vector2, alpha)), alpha)), alpha));
    float mag = Vector.lerp(_list.get(_current1.nextIndex())._node.magnitude(),
        _list.get(_current2.nextIndex())._node.magnitude(), alpha);
    Quaternion q = Quaternion.squad(_list.get(_current1.nextIndex())._node.orientation(),
        _list.get(_current1.nextIndex())._tangentQuaternion,
        _list.get(_current2.nextIndex())._tangentQuaternion,
        _list.get(_current2.nextIndex())._node.orientation(), alpha);
    node().setPosition(pos);
    node().setRotation(q);
    node().setMagnitude(mag);
  }

  /**
   * Internal use.
   */
  protected void _updateCurrentKeyFrameForTime(float time) {
    // TODO: Special case for loops when closed path is implemented !!
    if (!_currentKeyFrameValid)
      // Recompute everything from scratch
      _current1 = _list.listIterator();
    // currentFrame_[1]->peekNext() <---> keyFr.get(_current1.nextIndex());
    while (_list.get(_current1.nextIndex())._time > time) {
      _currentKeyFrameValid = false;
      if (!_current1.hasPrevious())
        break;
      _current1.previous();
    }
    if (!_currentKeyFrameValid)
      _current2 = _list.listIterator(_current1.nextIndex());
    while (_list.get(_current2.nextIndex())._time < time) {
      _currentKeyFrameValid = false;
      if (!_current2.hasNext())
        break;
      _current2.next();
    }
    if (!_currentKeyFrameValid) {
      _current1 = _list.listIterator(_current2.nextIndex());
      if ((_current1.hasPrevious()) && (time < _list.get(_current2.nextIndex())._time))
        _current1.previous();
      _currentKeyFrameValid = true;
      _splineCacheIsValid = false;
    }
  }

  /**
   * Internal use. Used by {@link #interpolate(float)}.
   */
  protected void _updateSplineCache() {
    Vector deltaP = Vector.subtract(_list.get(_current2.nextIndex())._node.position(),
        _list.get(_current1.nextIndex())._node.position());
    _vector1 = Vector.add(Vector.multiply(deltaP, 3.0f), Vector.multiply(_list.get(_current1.nextIndex())._tangentVector, (-2.0f)));
    _vector1 = Vector.subtract(_vector1, _list.get(_current2.nextIndex())._tangentVector);
    _vector2 = Vector.add(Vector.multiply(deltaP, (-2.0f)), _list.get(_current1.nextIndex())._tangentVector);
    _vector2 = Vector.add(_vector2, _list.get(_current2.nextIndex())._tangentVector);
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
  public List<Node> path() {
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
        _updateModifiedKeyFrames();
      if (_list.get(0) == _list.get(_list.size() - 1))
        _path.add(
            new Node(_list.get(0)._node.position(), _list.get(0)._node.orientation(), _list.get(0)._node.magnitude()));
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
          Vector pvec1 = Vector.add(Vector.multiply(pdiff, 3.0f), Vector.multiply(keyFrames[1]._tangentVector, (-2.0f)));
          pvec1 = Vector.subtract(pvec1, keyFrames[2]._tangentVector);
          Vector pvec2 = Vector.add(Vector.multiply(pdiff, (-2.0f)), keyFrames[1]._tangentVector);
          pvec2 = Vector.add(pvec2, keyFrames[2]._tangentVector);
          for (int step = 0; step < nbSteps; ++step) {
            float alpha = step / (float) nbSteps;
            _path.add(new Node(
                Vector.add(keyFrames[1]._node.position(), Vector.multiply(Vector.add(keyFrames[1]._tangentVector, Vector.multiply(Vector.add(pvec1, Vector.multiply(pvec2, alpha)), alpha)), alpha)),
                Quaternion.squad(keyFrames[1]._node.orientation(), keyFrames[1]._tangentQuaternion, keyFrames[2]._tangentQuaternion, keyFrames[2]._node.orientation(), alpha),
                Vector.lerp(keyFrames[1]._node.magnitude(), keyFrames[2]._node.magnitude(), alpha))
            );
          }
          // Shift
          keyFrames[0] = keyFrames[1];
          keyFrames[1] = keyFrames[2];
          keyFrames[2] = keyFrames[3];
          index++;
          keyFrames[3] = (index < _list.size()) ? _list.get(index) : null;
        }
        // Add last KeyFrame
        _path.add(new Node(keyFrames[1]._node.position(), keyFrames[1]._node.orientation(), keyFrames[1]._node.magnitude()));
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
        keyFrame._tangentVector = Vector.multiply(Vector.subtract(next._node.position(), prev._node.position()), 0.5f);
        keyFrame._tangentQuaternion = Quaternion.squadTangent(prev._node.orientation(), keyFrame._node.orientation(), next._node.orientation());
      } else {
        keyFrame._tangentVector = Vector.multiply(Vector.subtract(keyFrame._node.position(), prev._node.position()), 0.5f);
        keyFrame._tangentQuaternion = Quaternion.squadTangent(prev._node.orientation(), keyFrame._node.orientation(), keyFrame._node.orientation());
      }
      prev = keyFrame;
      keyFrame = next;
    }
    _valuesAreValid = true;
  }

  /**
   * Internal use. Calls {@link #_invalidateValues()} if a keyframe defining the
   * path was recently modified.
   */
  protected void _checkValidity() {
    boolean flag = false;
    for (KeyFrame keyFrame : _list) {
      if (keyFrame._node.lastUpdate() > _lastUpdate()) {
        flag = true;
        break;
      }
    }
    if (flag) {
      this._invalidateValues();
      this._modified();
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
}
