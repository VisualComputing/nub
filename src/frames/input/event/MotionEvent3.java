/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.input.event;

/**
 * A {@link frames.input.event.MotionEvent} with three degrees-of-freedom ( {@link #x()},
 * {@link #y()} and {@link #z()} ).
 */
public class MotionEvent3 extends MotionEvent {
  protected float _x, _dx;
  protected float _y, _dy;
  protected float _z, _dz;

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param modifiers
   * @param id
   */
  public MotionEvent3(float dx, float dy, float dz, int modifiers, int id) {
    super(modifiers, id);
    this._dx = dx;
    this._dy = dy;
    this._dz = dz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z, modifiers, id)}.
   *
   * @see #MotionEvent3(MotionEvent3, float, float, float, int, int)
   */
  public MotionEvent3(MotionEvent previous, float x, float y, float z, int modifiers, int id) {
    this(previous instanceof MotionEvent3 ? (MotionEvent3) previous : null, x, y, z, modifiers, id);
  }

  /**
   * Construct a relative event from the given previous event, dof's and modifiers.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param previous
   * @param x
   * @param y
   * @param z
   * @param modifiers
   * @param id
   */
  public MotionEvent3(MotionEvent3 previous, float x, float y, float z, int modifiers, int id) {
    super(modifiers, id);
    this._x = x;
    this._y = y;
    this._z = z;
    _setPrevious(previous);
  }

  /**
   * Construct an absolute event from the given dof's.
   *
   * @param dx
   * @param dy
   * @param dz
   */
  public MotionEvent3(float dx, float dy, float dz) {
    super();
    this._dx = dx;
    this._dy = dy;
    this._dz = dz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent3 ? (MotionEvent3) prevEvent : null, x, y, z)}.
   *
   * @see #MotionEvent3(MotionEvent3, float, float, float)
   */
  public MotionEvent3(MotionEvent previous, float x, float y, float z) {
    this(previous instanceof MotionEvent3 ? (MotionEvent3) previous : null, x, y, z);
  }

  /**
   * Construct a relative event from the given previous event, dof's and modifiers.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param previous
   * @param x
   * @param y
   * @param z
   */
  public MotionEvent3(MotionEvent3 previous, float x, float y, float z) {
    super();
    this._x = x;
    this._y = y;
    this._z = z;
    _setPrevious(previous);
  }

  protected MotionEvent3(MotionEvent3 other) {
    super(other);
    this._x = other._x;
    this._dx = other._dx;
    this._y = other._y;
    this._dy = other._dy;
    this._z = other._z;
    this._dz = other._z;
  }

  @Override
  public MotionEvent3 get() {
    return new MotionEvent3(this);
  }

  @Override
  public MotionEvent3 flush() {
    return (MotionEvent3) super.flush();
  }

  @Override
  public MotionEvent3 fire() {
    return (MotionEvent3) super.fire();
  }

  @Override
  protected void _setPrevious(MotionEvent previous) {
    _relative = true;
    if (previous != null)
      if (previous instanceof MotionEvent3 && previous.id() == this.id()) {
        this._dx = this.x() - ((MotionEvent3) previous).x();
        this._dy = this.y() - ((MotionEvent3) previous).y();
        this._dz = this.z() - ((MotionEvent3) previous).z();
        _distance = MotionEvent
            .distance(_x, _y, _z, ((MotionEvent3) previous).x(), ((MotionEvent3) previous).y(), ((MotionEvent3) previous).z());
        _delay = this.timestamp() - previous.timestamp();
        if (_delay == 0)
          _speed = _distance;
        else
          _speed = _distance / (float) _delay;
      }
  }

  /**
   * @return dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float x() {
    return _x;
  }

  /**
   * @return dof-1 delta
   */
  public float dx() {
    return _dx;
  }

  /**
   * @return previous dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float previousX() {
    return x() - dx();
  }

  /**
   * @return dof-2, only meaningful if the event {@link #isRelative()}
   */
  public float y() {
    return _y;
  }

  /**
   * @return dof-2 delta
   */
  public float dy() {
    return _dy;
  }

  /**
   * @return previous dof-2, only meaningful if the event {@link #isRelative()}
   */
  public float previousY() {
    return y() - dy();
  }

  /**
   * @return dof-3, only meaningful if the event {@link #isRelative()}
   */
  public float z() {
    return _z;
  }

  /**
   * @return dof-3 delta
   */
  public float dz() {
    return _dz;
  }

  /**
   * @return previous dof-3, only meaningful if the event {@link #isRelative()}
   */
  public float previousZ() {
    return z() - dz();
  }

  @Override
  public boolean isNull() {
    if (dx() == 0 && dy() == 0 && dz() == 0 && !fired() && !flushed())
      return true;
    return false;
  }

  /**
   * Reduces the event to a {@link MotionEvent2} (lossy reduction). Keeps
   * dof-1 and dof-2 and discards dof-3.
   */
  public MotionEvent2 event2() {
    MotionEvent2 pe2;
    MotionEvent2 e2;
    if (isRelative()) {
      pe2 = new MotionEvent2(null, previousX(), previousY(), modifiers(), id());
      e2 = new MotionEvent2(pe2, x(), y(), modifiers(), id());
    } else {
      e2 = new MotionEvent2(dx(), dy(), modifiers(), id());
    }
    e2._delay = this.delay();
    e2._speed = this.speed();
    e2._distance = this.distance();
    if (fired())
      return e2.fire();
    else if (flushed())
      return e2.flush();
    return e2;
  }
}
