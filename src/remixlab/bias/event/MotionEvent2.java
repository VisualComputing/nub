/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.bias.event;

/**
 * A {@link remixlab.bias.event.MotionEvent} with two degrees-of-freedom ({@link #x()}
 * and {@link #y()}).
 */
public class MotionEvent2 extends MotionEvent {
  protected float _x, _dx;
  protected float _y, _dy;

  /**
   * Construct an absolute event from the given dof's and modifiers.
   *
   * @param dx
   * @param dy
   * @param modifiers
   * @param id
   */
  public MotionEvent2(float dx, float dy, int modifiers, int id) {
    super(modifiers, id);
    this._dx = dx;
    this._dy = dy;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent2 ? (MotionEvent2) prevEvent : null, x, y, modifiers, id)}.
   *
   * @see #MotionEvent2(MotionEvent2, float, float, int, int)
   */
  public MotionEvent2(MotionEvent previous, float x, float y, int modifiers, int id) {
    this(previous instanceof MotionEvent2 ? (MotionEvent2) previous : null, x, y, modifiers, id);
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
   * @param modifiers
   * @param id
   */
  public MotionEvent2(MotionEvent2 previous, float x, float y, int modifiers, int id) {
    super(modifiers, id);
    this._x = x;
    this._y = y;
    _setPrevious(previous);
  }

  /**
   * Construct an absolute event from the given dof's.
   *
   * @param dx
   * @param dy
   */
  public MotionEvent2(float dx, float dy) {
    super();
    this._dx = dx;
    this._dy = dy;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent2 ? (MotionEvent2) prevEvent : null, x, y)}.
   *
   * @see #MotionEvent2(MotionEvent2, float, float)
   */
  public MotionEvent2(MotionEvent previous, float x, float y) {
    this(previous instanceof MotionEvent2 ? (MotionEvent2) previous : null, x, y);
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
   */
  public MotionEvent2(MotionEvent2 previous, float x, float y) {
    super();
    this._x = x;
    this._y = y;
    _setPrevious(previous);
  }

  protected MotionEvent2(MotionEvent2 other) {
    super(other);
    this._x = other._x;
    this._dx = other._dx;
    this._y = other._y;
    this._dy = other._dy;
  }

  @Override
  public MotionEvent2 get() {
    return new MotionEvent2(this);
  }

  @Override
  public MotionEvent2 flush() {
    return (MotionEvent2) super.flush();
  }

  @Override
  public MotionEvent2 fire() {
    return (MotionEvent2) super.fire();
  }

  @Override
  protected void _setPrevious(MotionEvent previous) {
    _relative = true;
    if (previous != null)
      if (previous instanceof MotionEvent2 && previous.id() == this.id()) {
        this._dx = this.x() - ((MotionEvent2) previous).x();
        this._dy = this.y() - ((MotionEvent2) previous).y();
        _distance = MotionEvent.distance(_x, _y, ((MotionEvent2) previous).x(), ((MotionEvent2) previous).y());
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

  @Override
  public boolean isNull() {
    if (dx()==0 && dy()==0 && !fired() && !flushed())
      return true;
    return false;
  }

  /**
   * Convenience function that simply returns {@code return event1(true)}
   *
   * @see #event1(boolean)
   */
  public MotionEvent1 event1() {
    return event1(true);
  }

  /**
   * Reduces the event to a {@link MotionEvent1} (lossy reduction).
   *
   * @param fromX if true keeps dof-1, else keeps dof-2
   */
  public MotionEvent1 event1(boolean fromX) {
    MotionEvent1 pe1;
    MotionEvent1 e1;
    if (fromX) {
      if (isRelative()) {
        pe1 = new MotionEvent1(null, previousX(), modifiers(), id());
        e1 = new MotionEvent1(pe1, x(), modifiers(), id());
      } else {
        e1 = new MotionEvent1(dx(), modifiers(), id());
      }
    } else {
      if (isRelative()) {
        pe1 = new MotionEvent1(null, previousY(), modifiers(), id());
        e1 = new MotionEvent1(pe1, y(), modifiers(), id());
      } else {
        e1 = new MotionEvent1(dy(), modifiers(), id());
      }
    }
    e1._delay = this.delay();
    e1._speed = this.speed();
    e1._distance = this.distance();
    if (fired())
      return e1.fire();
    else if (flushed())
      return e1.flush();
    return e1;
  }
}
