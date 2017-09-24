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

import remixlab.bias.Event;

/**
 * Base class of all DOF_n_Events: {@link Event}s defined from
 * DOFs (degrees-of-freedom).
 * <p>
 * MotionEvents may be relative or absolute (see {@link #isRelative()}, {@link #isAbsolute()})
 * depending whether or not they're constructed from a previous MotionEvent. While
 * relative motion events have {@link #distance()}, {@link #speed()}, and
 * {@link #delay()}, absolute motion events don't.
 */
public class MotionEvent extends Event {
  // defaulting to zero:
  // http://stackoverflow.com/questions/3426843/what-is-the-default-initialization-of-an-array-in-java
  protected long delay;
  protected float distance, speed;
  protected boolean rel;

  /**
   * Constructs an absolute MotionEvent with an "empty"
   * {@link remixlab.bias.Shortcut}.
   */
  public MotionEvent() {
    super();
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} as a
   * {@link remixlab.bias.Shortcut}.
   */
  public MotionEvent(int modifiers) {
    super(modifiers, NO_ID);
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} and
   * {@code modifiers} as a {@link remixlab.bias.Shortcut}.
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
  protected void setPrevious(MotionEvent prevEvent) {
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
   * Same as {@code return event1(event, true)}.
   *
   * @see #event1(MotionEvent, boolean)
   */
  public static Event1 event1(MotionEvent event) {
    return event1(event, true);
  }

  /**
   * Returns a {@link Event1} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static Event1 event1(MotionEvent event, boolean fromX) {
    if (event instanceof Event1)
      return (Event1) event;
    if (event instanceof Event2)
      return ((Event2) event).event1(fromX);
    if (event instanceof Event3)
      return ((Event3) event).event2().event1(fromX);
    if (event instanceof Event6)
      return ((Event6) event).event3(fromX).event2().event1(fromX);
    return null;
  }

  /**
   * Same as {@code return event2(event, true)}.
   *
   * @see #event2(MotionEvent, boolean)
   */
  public static Event2 event2(MotionEvent event) {
    return event2(event, true);
  }

  /**
   * Returns a {@link Event2} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static Event2 event2(MotionEvent event, boolean fromX) {
    if (event instanceof Event1)
      return null;
    if (event instanceof Event2)
      // return ((Event2) event).get();//TODO better?
      return (Event2) event;
    if (event instanceof Event3)
      return ((Event3) event).event2();
    if (event instanceof Event6)
      return ((Event6) event).event3(fromX).event2();
    return null;
  }

  /**
   * Same as {@code return event3(event, true)}.
   *
   * @see #event3(MotionEvent, boolean)
   */
  public static Event3 event3(MotionEvent event) {
    return event3(event, true);
  }

  /**
   * Returns a {@link Event3} from the MotionEvent
   * translation-coordinates if {@code fromTranslation} is {@code true} and from the
   * rotation-coordinate otherwise.
   */
  public static Event3 event3(MotionEvent event, boolean fromTranslation) {
    if (event instanceof Event1)
      return null;
    if (event instanceof Event2)
      return null;
    if (event instanceof Event3)
      return (Event3) event;
    if (event instanceof Event6)
      return ((Event6) event).event3(fromTranslation);
    return null;
  }

  /**
   * Returns a {@link Event6} if the MotionEvent {@code instanceof}
   * {@link Event6} and null otherwise..
   */
  public static Event6 event6(MotionEvent event) {
    if (event instanceof Event6)
      return (Event6) event;
    return null;
  }

  /**
   * @return Euclidean distance between points (x1,y1) and (x2,y2).
   */
  public static float distance(float x1, float y1, float x2, float y2) {
    return (float) Math.sqrt((float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0));
  }

  /**
   * @return Euclidean distance between points (x1,y1,z1) and (x2,y2,z2).
   */
  public static float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
    return (float) Math
        .sqrt((float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0) + (float) Math.pow((z2 - z1), 2.0));
  }

  /**
   * @return Euclidean distance between points (x1,y1,z1,rx1,y1,rz1) and
   * (x2,y2,z2,rx2,y2,rz2).
   */
  public static float distance(float x1, float y1, float z1, float rx1, float ry1, float rz1, float x2, float y2,
                               float z2, float rx2, float ry2, float rz2) {
    return (float) Math.sqrt(
        (float) Math.pow((x2 - x1), 2.0) + (float) Math.pow((y2 - y1), 2.0) + (float) Math.pow((z2 - z1), 2.0)
            + (float) Math.pow((rx2 - rx1), 2.0) + (float) Math.pow((ry2 - ry1), 2.0) + (float) Math
            .pow((rz2 - rz1), 2.0));
  }
}
