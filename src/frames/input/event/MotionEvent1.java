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
 * A {@link frames.input.event.MotionEvent} with one degree of freedom ({@link #x()}).
 */
public class MotionEvent1 extends MotionEvent {
  protected float _x, _dx;

  /**
   * Construct an absolute DOF1 motion event.
   *
   * @param dx        1-dof
   * @param modifiers MotionShortcut modifiers
   * @param id        MotionShortcut gesture-id
   */
  public MotionEvent1(float dx, int modifiers, int id) {
    super(modifiers, id);
    this._dx = dx;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent1 ? (MotionEvent1) prevEvent : null, x, modifiers, id)}.
   *
   * @see #MotionEvent1(MotionEvent1, float, int, int)
   */
  public MotionEvent1(MotionEvent previous, float x, int modifiers, int id) {
    this(previous instanceof MotionEvent1 ? (MotionEvent1) previous : null, x, modifiers, id);
  }

  /**
   * Construct a relative DOF1 motion event.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param previous
   * @param x         1-dof
   * @param modifiers MotionShortcut modifiers
   * @param id        MotionShortcut gesture-id
   */
  public MotionEvent1(MotionEvent1 previous, float x, int modifiers, int id) {
    super(modifiers, id);
    this._x = x;
    _setPrevious(previous);
  }

  /**
   * Construct an absolute DOF1 motion event.
   *
   * @param dx 1-dof
   */
  public MotionEvent1(float dx) {
    super();
    this._dx = dx;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof MotionEvent1 ? (MotionEvent1) prevEvent : null, x)}.
   *
   * @see #MotionEvent1(MotionEvent1, float)
   */
  public MotionEvent1(MotionEvent previous, float x) {
    this(previous instanceof MotionEvent1 ? (MotionEvent1) previous : null, x);
  }

  /**
   * Construct a relative DOF1 motion event.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param previous
   * @param x        1-dof
   */
  public MotionEvent1(MotionEvent1 previous, float x) {
    super();
    this._x = x;
    _setPrevious(previous);
  }

  protected MotionEvent1(MotionEvent1 other) {
    super(other);
    this._x = other._x;
    this._dx = other._dx;
  }

  @Override
  public MotionEvent1 get() {
    return new MotionEvent1(this);
  }

  @Override
  public MotionEvent1 flush() {
    return (MotionEvent1) super.flush();
  }

  @Override
  public MotionEvent1 fire() {
    return (MotionEvent1) super.fire();
  }

  @Override
  protected void _setPrevious(MotionEvent previous) {
    _relative = true;
    if (previous != null)
      if (previous instanceof MotionEvent1 && previous.id() == this.id()) {
        this._dx = this.x() - ((MotionEvent1) previous).x();
        _distance = this.x() - ((MotionEvent1) previous).x();
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

  @Override
  public boolean isNull() {
    if (dx() == 0 && !fired() && !flushed())
      return true;
    return false;
  }
}
