/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.input.event;

/**
 * A {@link frames.input.event.MotionEvent} with six degrees-of-freedom ( {@link #x()},
 * {@link #y()}, {@link #z()} , {@link #rx()}, {@link #ry()} and {@link #rz()}).
 */
public class MotionEvent6 extends MotionEvent {
  protected float _x, _dx;
  protected float _y, _dy;
  protected float _z, _dz;

  protected float _rx, _drx;
  protected float _ry, _dry;
  protected float _rz, _drz;

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param drx
   * @param dry
   * @param drz
   * @param modifiers
   * @param id
   */
  public MotionEvent6(float dx, float dy, float dz, float drx, float dry, float drz, int modifiers, int id) {
    super(modifiers, id);
    this._dx = dx;
    this._dy = dy;
    this._dz = dz;
    this._drx = drx;
    this._dry = dry;
    this._drz = drz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent6 ? (MotionEvent6) prevEvent : null, x, y, z, rx, ry, rz, modifiers, id)}.
   *
   * @see #MotionEvent6(MotionEvent6, float, float, float, float, float, float, int, int)
   */
  public MotionEvent6(MotionEvent previous, float x, float y, float z, float rx, float ry, float rz, int modifiers,
                      int id) {
    this(previous instanceof MotionEvent6 ? (MotionEvent6) previous : null, x, y, z, rx, ry, rz, modifiers, id);
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
   * @param rx
   * @param ry
   * @param rz
   * @param modifiers
   * @param id
   */
  public MotionEvent6(MotionEvent6 previous, float x, float y, float z, float rx, float ry, float rz, int modifiers, int id) {
    super(modifiers, id);
    this._x = x;
    this._y = y;
    this._z = z;
    this._rx = rx;
    this._ry = ry;
    this._rz = rz;
    _setPrevious(previous);
  }

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param dz
   * @param drx
   * @param dry
   * @param drz
   */
  public MotionEvent6(float dx, float dy, float dz, float drx, float dry, float drz) {
    super();
    this._dx = dx;
    this._dy = dy;
    this._dz = dz;
    this._drx = drx;
    this._dry = dry;
    this._drz = drz;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent6 ? (MotionEvent6) prevEvent : null, x, y, z, rx, ry, rz)}.
   *
   * @see #MotionEvent6(MotionEvent6, float, float, float, float, float, float)
   */
  public MotionEvent6(MotionEvent previous, float x, float y, float z, float rx, float ry, float rz) {
    this(previous instanceof MotionEvent6 ? (MotionEvent6) previous : null, x, y, z, rx, ry, rz);
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
   * @param rx
   * @param ry
   * @param rz
   */
  public MotionEvent6(MotionEvent6 previous, float x, float y, float z, float rx, float ry, float rz) {
    super();
    this._x = x;
    this._y = y;
    this._z = z;
    this._rx = rx;
    this._ry = ry;
    this._rz = rz;
    _setPrevious(previous);
  }

  protected MotionEvent6(MotionEvent6 motionEvent6) {
    super(motionEvent6);
    this._x = motionEvent6._x;
    this._dx = motionEvent6._dx;
    this._y = motionEvent6._y;
    this._dy = motionEvent6._dy;
    this._z = motionEvent6._z;
    this._dz = motionEvent6._z;
    this._rx = motionEvent6._rx;
    this._drx = motionEvent6._drx;
    this._ry = motionEvent6._ry;
    this._dry = motionEvent6._dry;
    this._rz = motionEvent6._rz;
    this._drz = motionEvent6._drz;
  }

  @Override
  public MotionEvent6 get() {
    return new MotionEvent6(this);
  }

  @Override
  public MotionEvent6 flush() {
    return (MotionEvent6) super.flush();
  }

  @Override
  public MotionEvent6 fire() {
    return (MotionEvent6) super.fire();
  }

  @Override
  protected void _setPrevious(MotionEvent previous) {
    _relative = true;
    if (previous != null)
      if (previous instanceof MotionEvent6 && previous.id() == this.id()) {
        this._dx = this.x() - ((MotionEvent6) previous).x();
        this._dy = this.y() - ((MotionEvent6) previous).y();
        this._dz = this.z() - ((MotionEvent6) previous).z();
        this._drx = this.rx() - ((MotionEvent6) previous).rx();
        this._dry = this.ry() - ((MotionEvent6) previous).ry();
        this._drz = this.rz() - ((MotionEvent6) previous).rz();
        _distance = MotionEvent.distance(_x, _y, _z, _rx, _ry, _rz, ((MotionEvent6) previous).x(), ((MotionEvent6) previous).y(),
            ((MotionEvent6) previous).z(), ((MotionEvent6) previous).rx(), ((MotionEvent6) previous).ry(),
            ((MotionEvent6) previous).rz());
        _delay = this.timestamp() - previous.timestamp();
        if (_delay == 0)
          _speed = _distance;
        else
          _speed = _distance / (float) _delay;
      }
  }

  /**
   * @return dof1, only meaningful if the event {@link #isRelative()}
   */
  public float x() {
    return _x;
  }

  /**
   * @return dof1 delta
   */
  public float dx() {
    return _dx;
  }

  /**
   * @return previous dof1, only meaningful if the event {@link #isRelative()}
   */
  public float previousX() {
    return x() - dx();
  }

  /**
   * @return dof2, only meaningful if the event {@link #isRelative()}
   */
  public float y() {
    return _y;
  }

  /**
   * @return dof2 delta
   */
  public float dy() {
    return _dy;
  }

  /**
   * @return previous dof2, only meaningful if the event {@link #isRelative()}
   */
  public float previousY() {
    return y() - dy();
  }

  /**
   * @return dof3, only meaningful if the event {@link #isRelative()}
   */
  public float z() {
    return _z;
  }

  /**
   * @return dof3 delta
   */
  public float dz() {
    return _dz;
  }

  /**
   * @return previous dof3, only meaningful if the event {@link #isRelative()}
   */
  public float previousZ() {
    return z() - dz();
  }

  /**
   * Alias for {@link #rx()}, only meaningful if the event {@link #isRelative()}
   */
  public float roll() {
    return rx();
  }

  /**
   * @return dof4, only meaningful if the event {@link #isRelative()}
   */
  public float rx() {
    return _rx;
  }

  /**
   * Alias for {@link #ry()}, only meaningful if the event {@link #isRelative()}
   */
  public float pitch() {
    return ry();
  }

  /**
   * @return dof5, only meaningful if the event {@link #isRelative()}
   */
  public float ry() {
    return _ry;
  }

  /**
   * alias for {@link #rz()}, only meaningful if the event {@link #isRelative()}
   */
  public float yaw() {
    return rz();
  }

  /**
   * @return dof6, only meaningful if the event {@link #isRelative()}
   */
  public float rz() {
    return _rz;
  }

  /**
   * @return dof4 delta
   */
  public float drx() {
    return _drx;
  }

  /**
   * @return dof5 delta
   */
  public float dry() {
    return _dry;
  }

  /**
   * @return dof6 delta
   */
  public float drz() {
    return _drz;
  }

  /**
   * @return previous dof4, only meaningful if the event {@link #isRelative()}
   */
  public float previousRX() {
    return rx() - drx();
  }

  /**
   * @return previous dof5, only meaningful if the event {@link #isRelative()}
   */
  public float previousRY() {
    return ry() - dry();
  }

  /**
   * @return previous dof6, only meaningful if the event {@link #isRelative()}
   */
  public float previousRZ() {
    return rz() - drz();
  }

  @Override
  public boolean isNull() {
    if (dx() == 0 && dy() == 0 && dz() == 0 && drx() == 0 && dry() == 0 && drz() == 0 && !fired() && !flushed())
      return true;
    return false;
  }

  /**
   * Convenience function that simply returns {@code return event3(true)}
   *
   * @see #event3(boolean)
   */
  public MotionEvent3 event3() {
    return event3(true);
  }

  /**
   * Reduces the event to a {@link MotionEvent3} (lossy reduction).
   *
   * @param fromTranslation if true keeps dof1, dof2 and dof3; otherwise keeps dof4, dof4 and dof6.
   */
  public MotionEvent3 event3(boolean fromTranslation) {
    MotionEvent3 pe3;
    MotionEvent3 e3;
    if (isRelative()) {
      if (fromTranslation) {
        pe3 = new MotionEvent3(null, previousX(), previousY(), previousZ(), modifiers(), id());
        e3 = new MotionEvent3(pe3, x(), y(), z(), modifiers(), id());
      } else {
        pe3 = new MotionEvent3(null, previousRX(), previousRY(), previousRZ(), modifiers(), id());
        e3 = new MotionEvent3(pe3, rx(), ry(), rz(), modifiers(), id());
      }
    } else {
      if (fromTranslation) {
        e3 = new MotionEvent3(dx(), dy(), dz(), modifiers(), id());
      } else {
        e3 = new MotionEvent3(drx(), dry(), drz(), modifiers(), id());
      }
    }
    e3._delay = this.delay();
    e3._speed = this.speed();
    e3._distance = this.distance();
    if (fired())
      return e3.fire();
    else if (flushed())
      return e3.flush();
    return e3;
  }
}
