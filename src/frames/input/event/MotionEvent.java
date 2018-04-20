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

import frames.input.Event;

/**
 * Base class of all motion events defined from DOFs (degrees-of-freedom).
 * <p>
 * MotionEvents may be relative or absolute (see {@link #isRelative()}, {@link #isAbsolute()})
 * depending whether or not they're constructed from a previous MotionEvent. While
 * relative motion events have {@link #distance()}, {@link #speed()}, and
 * {@link #delay()}, absolute motion events don't.
 */
public class MotionEvent extends Event {
  // defaulting to zero:
  // http://stackoverflow.com/questions/3426843/what-is-the-default-initialization-of-an-array-in-java
  protected long _delay;
  protected float _distance, _speed;
  protected boolean _relative;

  /**
   * Constructs an absolute MotionEvent with an "empty"
   * {@link frames.input.Shortcut}.
   */
  public MotionEvent() {
    super();
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} as a
   * {@link frames.input.Shortcut}.
   */
  public MotionEvent(int modifiers) {
    super(modifiers, NO_ID);
  }

  /**
   * Constructs an absolute MotionEvent taking the given {@code modifiers} and
   * {@code modifiers} as a {@link frames.input.Shortcut}.
   */
  public MotionEvent(int modifiers, int id) {
    super(modifiers, id);
  }

  protected MotionEvent(MotionEvent motionEvent) {
    super(motionEvent);
    this._delay = motionEvent._delay;
    this._distance = motionEvent._distance;
    this._speed = motionEvent._speed;
    this._relative = motionEvent._relative;
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
    return _delay;
  }

  /**
   * Returns the distance between two consecutive motion events. Meaningful only if the
   * event {@link #isRelative()}.
   */
  public float distance() {
    return _distance;
  }

  /**
   * Returns the speed between two consecutive motion events. Meaningful only if the event
   * {@link #isRelative()}.
   */
  public float speed() {
    return _speed;
  }

  /**
   * Returns true if the motion event is relative, i.e., it has been built from a previous
   * motion event.
   */
  public boolean isRelative() {
    // return _distance() != 0;
    return _relative;
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
  protected void _setPrevious(MotionEvent previous) {
    _relative = true;
    // makes sense only if derived classes call it
    if (previous != null)
      if (previous.id() == this.id()) {
        _delay = this.timestamp() - previous.timestamp();
        if (_delay == 0)
          _speed = _distance;
        else
          _speed = _distance / (float) _delay;
      }
  }

  /**
   * Same as {@code return event1(event, true)}.
   *
   * @see #event1(MotionEvent, boolean)
   */
  public static MotionEvent1 event1(MotionEvent event) {
    return event1(event, true);
  }

  /**
   * Returns a {@link MotionEvent1} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static MotionEvent1 event1(MotionEvent event, boolean fromX) {
    if (event instanceof MotionEvent1)
      return (MotionEvent1) event;
    if (event instanceof MotionEvent2)
      return ((MotionEvent2) event).event1(fromX);
    if (event instanceof MotionEvent3)
      return ((MotionEvent3) event).event2().event1(fromX);
    if (event instanceof MotionEvent6)
      return ((MotionEvent6) event).event3(fromX).event2().event1(fromX);
    return null;
  }

  /**
   * Same as {@code return event2(event, true)}.
   *
   * @see #event2(MotionEvent, boolean)
   */
  public static MotionEvent2 event2(MotionEvent event) {
    return event2(event, true);
  }

  /**
   * Returns a {@link MotionEvent2} from the MotionEvent x-coordinate if
   * {@code fromX} is {@code true} and from the y-coordinate otherwise.
   */
  public static MotionEvent2 event2(MotionEvent event, boolean fromX) {
    if (event instanceof MotionEvent1)
      return null;
    if (event instanceof MotionEvent2)
      // return ((MotionEvent2) _event).get();//TODO better?
      return (MotionEvent2) event;
    if (event instanceof MotionEvent3)
      return ((MotionEvent3) event).event2();
    if (event instanceof MotionEvent6)
      return ((MotionEvent6) event).event3(fromX).event2();
    return null;
  }

  /**
   * Same as {@code return event3(event, true)}.
   *
   * @see #event3(MotionEvent, boolean)
   */
  public static MotionEvent3 event3(MotionEvent event) {
    return event3(event, true);
  }

  /**
   * Returns a {@link MotionEvent3} from the MotionEvent
   * translation-coordinates if {@code fromTranslation} is {@code true} and from the
   * rotation-coordinate otherwise.
   */
  public static MotionEvent3 event3(MotionEvent event, boolean fromTranslation) {
    if (event instanceof MotionEvent1)
      return null;
    if (event instanceof MotionEvent2)
      return null;
    if (event instanceof MotionEvent3)
      return (MotionEvent3) event;
    if (event instanceof MotionEvent6)
      return ((MotionEvent6) event).event3(fromTranslation);
    return null;
  }

  /**
   * Returns a {@link MotionEvent6} if the MotionEvent {@code instanceof}
   * {@link MotionEvent6} and null otherwise..
   */
  public static MotionEvent6 event6(MotionEvent event) {
    if (event instanceof MotionEvent6)
      return (MotionEvent6) event;
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
