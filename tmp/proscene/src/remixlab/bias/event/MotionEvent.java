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

import remixlab.bias.BogusEvent;
import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;

/**
 * Base class of all DOF_n_Events: {@link BogusEvent}s defined from
 * DOFs (degrees-of-freedom).
 * <p>
 * A MotionEvent encapsulates a {@link remixlab.bias.event.MotionShortcut}. MotionEvents
 * may be relative or absolute (see {@link #isRelative()}, {@link #isAbsolute()})
 * depending whether or not they're constructed from a previous MotionEvent. While
 * relative motion events have {@link #distance()}, {@link #speed()}, and
 * {@link #delay()}, absolute motion events don't.
 */
public class MotionEvent extends BogusEvent {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(id).append(delay).append(distance)
        .append(speed).append(rel).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    MotionEvent other = (MotionEvent) obj;
    return new EqualsBuilder().appendSuper(super.equals(obj)).append(id, other.id).append(delay, other.delay)
        .append(distance, other.distance).append(speed, other.speed).append(rel, other.rel).isEquals();
  }

  // defaulting to zero:
  // http://stackoverflow.com/questions/3426843/what-is-the-default-initialization-of-an-array-in-java
  protected long delay;
  protected float distance, speed;
  protected boolean rel;

  /**
   * Constructs an absolute MotionEvent with an "empty"
   * {@link remixlab.bias.event.MotionShortcut}.
   */
  public MotionEvent() {
    super();
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} as a
   * {@link remixlab.bias.event.MotionShortcut}.
   */
  public MotionEvent(int modifiers) {
    super(modifiers, NO_ID);
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} and
   * {@code modifiers} as a {@link remixlab.bias.event.MotionShortcut}.
   */
  public MotionEvent(int modifiers, int id) {
    super(modifiers, id);
  }

  protected MotionEvent(MotionEvent other) {
    super(other);
    this.delay = other.delay;
    this.distance = other.distance;
    this.speed = other.speed;
    this.rel = other.rel;
  }

  @Override
  public MotionEvent get() {
    return new MotionEvent(this);
  }

  @Override
  public MotionEvent flush() {
    return (MotionEvent) super.flush();
  }

  @Override
  public MotionEvent fire() {
    return (MotionEvent) super.fire();
  }

  /**
   * Modulate the event dofs according to {@code sens}. Only meaningful if the event
   * {@link #isAbsolute()}.
   */
  public void modulate(float[] sens) {
  }

  @Override
  public MotionShortcut shortcut() {
    return new MotionShortcut(modifiers(), id());
  }

  /**
   * Returns the delay between two consecutive motion events. Meaningful only if the event
   * {@link #isRelative()}.
   */
  public long delay() {
    return delay;
  }

  /**
   * Returns the distance between two consecutive motion events. Meaningful only if the
   * event {@link #isRelative()}.
   */
  public float distance() {
    return distance;
  }

  /**
   * Returns the speed between two consecutive motion events. Meaningful only if the event
   * {@link #isRelative()}.
   */
  public float speed() {
    return speed;
  }

  /**
   * Returns true if the motion event is relative, i.e., it has been built from a previous
   * motion event.
   */
  public boolean isRelative() {
    // return distance() != 0;
    return rel;
  }

  /**
   * Returns true if the motion event is absolute, i.e., it hasn't been built from a
   * previous motion event.
   */
  public boolean isAbsolute() {
    return !isRelative();
  }

  /**
   * Sets the event's previous event to build a relative event.
   */
  protected void setPreviousEvent(MotionEvent prevEvent) {
    rel = true;
    // makes sense only if derived classes call it
    if (prevEvent != null)
      if (prevEvent.id() == this.id()) {
        delay = this.timestamp() - prevEvent.timestamp();
        if (delay == 0)
          speed = distance;
        else
          speed = distance / (float) delay;
      }
  }

  /**
   * Same as {@code return dof1Event(event, true)}.
   *
   * @see #dof1Event(MotionEvent, boolean)
   */
  public static DOF1Event dof1Event(MotionEvent event) {
    return dof1Event(event, true);
  }

  /**
   * Returns a {@link remixlab.bias.event.DOF1Event} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static DOF1Event dof1Event(MotionEvent event, boolean fromX) {
    if (event instanceof DOF1Event)
      return (DOF1Event) event;
    if (event instanceof DOF2Event)
      return ((DOF2Event) event).dof1Event(fromX);
    if (event instanceof DOF3Event)
      return ((DOF3Event) event).dof2Event().dof1Event(fromX);
    if (event instanceof DOF6Event)
      return ((DOF6Event) event).dof3Event(fromX).dof2Event().dof1Event(fromX);
    return null;
  }

  /**
   * Same as {@code return dof2Event(event, true)}.
   *
   * @see #dof2Event(MotionEvent, boolean)
   */
  public static DOF2Event dof2Event(MotionEvent event) {
    return dof2Event(event, true);
  }

  /**
   * Returns a {@link remixlab.bias.event.DOF2Event} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static DOF2Event dof2Event(MotionEvent event, boolean fromX) {
    if (event instanceof DOF1Event)
      return null;
    if (event instanceof DOF2Event)
      // return ((DOF2Event) event).get();//TODO better?
      return (DOF2Event) event;
    if (event instanceof DOF3Event)
      return ((DOF3Event) event).dof2Event();
    if (event instanceof DOF6Event)
      return ((DOF6Event) event).dof3Event(fromX).dof2Event();
    return null;
  }

  /**
   * Same as {@code return dof3Event(event, true)}.
   *
   * @see #dof3Event(MotionEvent, boolean)
   */
  public static DOF3Event dof3Event(MotionEvent event) {
    return dof3Event(event, true);
  }

  /**
   * Returns a {@link remixlab.bias.event.DOF3Event} from the MotionEvent
   * translation-coordinates if {@code fromTranslation} is {@code true} and from the
   * rotation-coordinate otherwise.
   */
  public static DOF3Event dof3Event(MotionEvent event, boolean fromTranslation) {
    if (event instanceof DOF1Event)
      return null;
    if (event instanceof DOF2Event)
      return null;
    if (event instanceof DOF3Event)
      return (DOF3Event) event;
    if (event instanceof DOF6Event)
      return ((DOF6Event) event).dof3Event(fromTranslation);
    return null;
  }

  /**
   * Returns a {@link remixlab.bias.event.DOF6Event} if the MotionEvent {@code instanceof}
   * {@link remixlab.bias.event.DOF6Event} and null otherwise..
   */
  public static DOF6Event dof6Event(MotionEvent event) {
    if (event instanceof DOF6Event)
      return (DOF6Event) event;
    return null;
  }
}
