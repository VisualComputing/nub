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
      return _node.matches(keyFrame._node) && _time == keyFrame._time;
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
      return _node.reference() == null ? _tangentVector : _node.reference().worldDisplacement(_tangentVector);
    }

    /**
     * Returns the cache {@code _tangentQuaternion} world-view. Good for drawing. See {@link #_updatePath()}.
     */
    protected Quaternion _tangentQuaternion() {
      return _node.reference() == null ? _tangentQuaternion : _node.reference().worldDisplacement(_tangentQuaternion);
    }

    /**
     * Returns the key-frame translation respect to the {@code node} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@code node} {@link Node#reference()}.
     */
    protected Vector _translation() {
      return _node.reference() == _node.reference() ? _node.position() :
          _node.reference() == null ? _node.worldPosition() : _node.reference().location(_node.worldPosition());
      // perhaps less efficient but simpler equivalent form:
      // return _node.reference() == null ? _node.position() : _node.reference().location(_node.position());
    }

    /**
     * Returns the key-frame rotation respect to the {@code node} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@code node} {@link Node#reference()}.
     */
    protected Quaternion _rotation() {
      return _node.reference() == _node.reference() ? _node.orientation() :
          _node.reference() == null ? _node.worldOrientation() : _node.reference().displacement(_node.worldOrientation());
      // perhaps less efficient but simpler equivalent form:
      // return _node.reference() == null ? _node.orientation() : _node.reference().displacement(_node.orientation());
    }

    /**
     * Returns the key-frame scaling respect to the {@link #_node} {@link Node#reference()} space.
     * Optimally computed when the node's key-frame reference is the same as the {@link #_node} {@link Node#reference()}.
     */
    protected float _scaling() {
      return _node.reference() == _node.reference() ? _node.magnitude() :
          _node.reference() == null ? _node.worldMagnitude() : _node.reference().displacement(_node.worldMagnitude());
      // perhaps less efficient but simpler equivalent form:
      // return _node.reference() == null ? _node.magnitude() : _node.reference().displacement(_node.magnitude());
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
  Node _node;

  // Beat
  boolean _active;
  protected float _t;
  protected float _speed;

  // Misc
  protected boolean _recurrent;
  protected long _timestamp;

  // Cached values and flags
  protected boolean _pathIsValid;
  protected boolean _valuesAreValid;
  protected boolean _currentKeyFrameValid;
  protected boolean _splineCacheIsValid;
  protected Vector _vector1, _vector2;

  /**
   * Creates an interpolator for the given {@code node}. Note that
   * {@code t}, {@code speed} are set to their default values.
   */
  public Interpolator(Node node) {
    _list = new ArrayList<KeyFrame>();
    _path = new ArrayList<Node>();
    _node = node;
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
    this._node = other._node;
    this._t = other._t;
    this._speed = other._speed;
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
   * Returns the number of keyframes used by the interpolation. Use
   * {@link #addKeyFrame(Node)} to add new keyframes.
   */
  public int size() {
    return _list.size();
  }

  /**
   * Updates the {@code node} state at the current {@code t} and
   * then increments it by {@code delay} * {@code speed} ms.
   * This method is called by the node when it's rendered.
   * <p>
   * Note that interpolations stops when {@code t} reaches
   * {@link #firstTime()} or {@link #lastTime()}, unless
   * it is recurrent.
   */
  void _execute() {
    if (_active && !_list.isEmpty()) {
      // update _t according to current framerate
      long now = System.currentTimeMillis();
      if (_timestamp != 0) {
        long delay = now - _timestamp;
        _t += _speed * delay;
      }
      _timestamp = now;
      // interpolate
      if ((_speed > 0.0) && (_t >= _list.get(_list.size() - 1)._time)) {
        if (_recurrent) {
          _t = _list.get(0)._time;
        }
        else {
          _t = _list.get(_list.size() - 1)._time;
          _active = false;
        }
      }
      if ((_speed < 0.0) && (_t <= _list.get(0)._time)) {
        if (_recurrent) {
          _t = _list.get(_list.size() - 1)._time;
        }
        else {
          _t = _list.get(0)._time;
          _active = false;
        }
      }
      interpolate(_t);
    }
  }

  /**
   * resets timestamp and actives the animation.
   */
  public void animate() {
    _timestamp = 0;
    _active = true;
    if(!_recurrent) {
      if ((_speed > 0.0) && (_t >= _list.get(_list.size() - 1)._time)) {
        _t = _list.get(0)._time;
      }
      if ((_speed < 0.0) && (_t <= _list.get(0)._time)) {
        _t = _list.get(_list.size() - 1)._time;
      }
    }
  }

  /**
   * (de)activates the animation and resets timestamp if animation results inactive.
   */
  public void toggle() {
    if (!_active) {
      animate();
    }
    else {
      _active = false;
    }
  }

  /**
   * Stops the interpolation and resets timestamp and {@code t} (i.e., it is set
   * to the {@link #firstTime()}).
   * <p>
   * If desired, call {@link #interpolate(float)} after this method to actually move
   * the {@code node} to {@link #firstTime()}.
   */
  public void reset() {
    _timestamp = 0;
    _active = false;
    _t = firstTime();
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
   */
  public float firstTime() {
    return _list.isEmpty() ? 0.0f : _list.get(0)._time;
  }

  /**
   * Returns the time corresponding to the last keyframe, expressed in milliseconds.
   *
   * @see #firstTime()
   * @see #duration()
   */
  public float lastTime() {
    return _list.isEmpty() ? 0.0f : _list.get(_list.size() - 1)._time;
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
   * Same as {@code addKeyFrame(_node.hint(), time)}.
   *
   * @see #addKeyFrame(int, float)
   * @see #addKeyFrame(float)
   * @see #addKeyFrame(Node, float)
   * @see #addKeyFrame(Node)
   */
  public void addKeyFrame(float time) {
    addKeyFrame(_node.hint(), time);
  }

  /**
   * Adds a {@code node} copy as a keyframe at {@code time} and a mask {@code hint}.
   */
  public void addKeyFrame(int hint, float time) {
    _addKeyFrame(_node._copy(hint, true), time, true);
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
      node.tagging = _node.isHintEnabled(Node.KEYFRAMES);
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
    boolean rerun = _active;
    if (_active) {
      _active = false;
    }
    _list.remove(index);
    _t = firstTime();
    if (rerun) {
      if (_list.size() > 1)
        animate();
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
    _active = false;
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
   * Interpolate {@code node} at time {@code time} (expressed in milliseconds).
   * {@code t} is set to {@code time} and {@code node} is set accordingly.
   */
  public void interpolate(float time) {
    this._checkValidity();
    _t = time;
    if ((_list.isEmpty()) || (_node == null))
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
    _node.setPosition(pos);
    _node.setOrientation(q);
    _node.setMagnitude(mag);
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
            node._setHint(_node, _node._keyframesMask);
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
      _lastUpdate = Graph._frameCount;
      _valuesAreValid = false;
      _pathIsValid = false;
      _splineCacheIsValid = false;
    }
  }
}
