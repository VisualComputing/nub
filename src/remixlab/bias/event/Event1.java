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
 * A {@link remixlab.bias.event.MotionEvent} with one degree of freedom ( {@link #x()}).
 */
public class Event1 extends MotionEvent {
  protected float x, dx;

  /**
   * Construct an absolute DOF1 event.
   *
   * @param dx        1-dof
   * @param modifiers MotionShortcut modifiers
   * @param id        MotionShortcut gesture-id
   */
  public Event1(float dx, int modifiers, int id) {
    super(modifiers, id);
    this.dx = dx;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof Event1 ? (Event1) prevEvent : null, x, modifiers, id)}.
   *
   * @see #Event1(Event1, float, int, int)
   */
  public Event1(MotionEvent prevEvent, float x, int modifiers, int id) {
    this(prevEvent instanceof Event1 ? (Event1) prevEvent : null, x, modifiers, id);
  }

  /**
   * Construct a relative DOF1 event.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param prevEvent
   * @param x         1-dof
   * @param modifiers MotionShortcut modifiers
   * @param id        MotionShortcut gesture-id
   */
  public Event1(Event1 prevEvent, float x, int modifiers, int id) {
    super(modifiers, id);
    this.x = x;
    setPrevious(prevEvent);
  }

  /**
   * Construct an absolute DOF1 event.
   *
   * @param dx 1-dof
   */
  public Event1(float dx) {
    super();
    this.dx = dx;
  }

  /**
   * Same as
   * {@code this(prevEvent instanceof Event1 ? (Event1) prevEvent : null, x)}.
   *
   * @see #Event1(Event1, float)
   */
  public Event1(MotionEvent prevEvent, float x) {
    this(prevEvent instanceof Event1 ? (Event1) prevEvent : null, x);
  }

  /**
   * Construct a relative DOF1 event.
   * <p>
   * If the {@link #id()} of the {@code prevEvent} is different then {@link #id()}, sets
   * the {@link #distance()}, {@link #delay()} and {@link #speed()} all to {@code zero}.
   *
   * @param prevEvent
   * @param x         1-dof
   */
  public Event1(Event1 prevEvent, float x) {
    super();
    this.x = x;
    setPrevious(prevEvent);
  }

  protected Event1(Event1 other) {
    super(other);
    this.x = other.x;
    this.dx = other.dx;
  }

  @Override
  public Event1 get() {
    return new Event1(this);
  }

  @Override
  public Event1 flush() {
    return (Event1) super.flush();
  }

  @Override
  public Event1 fire() {
    return (Event1) super.fire();
  }

  @Override
  protected void setPrevious(MotionEvent prevEvent) {
    rel = true;
    if (prevEvent != null)
      if (prevEvent instanceof Event1 && prevEvent.id() == this.id()) {
        this.dx = this.x() - ((Event1) prevEvent).x();
        distance = this.x() - ((Event1) prevEvent).x();
        delay = this.timestamp() - prevEvent.timestamp();
        if (delay == 0)
          speed = distance;
        else
          speed = distance / (float) delay;
      }
  }

  /**
   * @return dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float x() {
    return x;
  }

  /**
   * @return dof-1 delta
   */
  public float dx() {
    return dx;
  }

  /**
   * @return previous dof-1, only meaningful if the event {@link #isRelative()}
   */
  public float prevX() {
    return x() - dx();
  }

  @Override
  public boolean isNull() {
    if (dx()==0)
      return true;
    return false;
  }
}
