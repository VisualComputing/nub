/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.input;

import frames.input.event.TapEvent;

/**
 * The root of all events that are to be handled by an {@link Agent}.
 * Every Event encapsulates a {@link Shortcut}. Gesture initialization and
 * termination, which may be of the interest of {@link Grabber}
 * objects, are reported by {@link #fired()} and {@link #flushed()}, respectively.
 * <p>
 * The following are the main class specializations:
 * {@link frames.input.event.MotionEvent}, {@link TapEvent}, and
 * {@link frames.input.event.KeyEvent}. Please refer to their documentation for
 * details.
 * <p>
 * If you ever need to define you're own event type, derive from this class; and, optional,
 * declare a shortcut type for your event (overriding the {@link #shortcut()). For details
 * refer to the {@link Shortcut}. If your custom event class defines it's own attributes, its
 * {@link #get()} method should be overridden.
 *
 * <b>Note</b> Event detection/reduction could happened in several different ways.
 * For instance, in the context of Java-based application, it typically takes place when
 * implementing a mouse listener interface. In Processing, it does it when registering at
 * the PApplet the so called mouseEvent and KeyEvent methods. Moreover, the
 * {@link Agent#handleFeed()} provides a callback alternative when none
 * of these mechanisms are available (as it often happens when dealing with specialized,
 * non-default inputGrabber hardware).
 */
public class Event {
  // modifier keys
  public static final int NO_MODIFIER_MASK = 0;
  public static final int NO_ID = 0;
  public static final int SHIFT = 1 << 0;
  public static final int CTRL = 1 << 1;
  public static final int META = 1 << 2;
  public static final int ALT = 1 << 3;
  public static final int ALT_GRAPH = 1 << 4;

  protected boolean _fire, _flush;
  protected int _modifiers;
  protected long _timestamp;
  protected int _id;

  /**
   * Constructs an event with an "empty" {@link Shortcut}.
   */
  public Event() {
    this._modifiers = NO_MODIFIER_MASK;
    this._id = NO_ID;
    _timestamp = System.currentTimeMillis();
  }

  /**
   * Constructs an event taking the given {@code modifiers} as a
   * {@link Shortcut}.
   */
  public Event(int modifiers, int id) {
    this._modifiers = modifiers;
    this._id = id;
    _timestamp = System.currentTimeMillis();
  }

  protected Event(Event event) {
    this._modifiers = event._modifiers;
    this._id = event._id;
    this._timestamp = System.currentTimeMillis();
    this._fire = event._fire;
    this._flush = event._flush;
  }

  public Event get() {
    return new Event(this);
  }

  /**
   * Same as {@code this.get()} but sets the {@link #flushed()} flag to true. Only agents
   * may call this.
   *
   * @see #flushed()
   */
  public Event flush() {
    if (fired() || flushed()) {
      System.out.println("Warning: event already " + (fired() ? "fired" : "flushed"));
      return this;
    }
    Event event = this.get();
    event._flush = true;
    return event;
  }

  /**
   * Same as {@code this.get()} but sets the {@link #fired()} flag to true. Only agents
   * may call this.
   *
   * @see #flushed()
   */
  public Event fire() {
    if (fired() || flushed()) {
      System.out.println("Warning: event already " + (fired() ? "fired" : "flushed"));
      return this;
    }
    Event event = this.get();
    event._fire = true;
    return event;
  }

  /**
   * Returns true if this is a 'flushed' event. Flushed events indicate gesture
   * termination, such as a mouse-release.
   *
   * @see #fired()
   */
  public boolean flushed() {
    return _flush;
  }

  /**
   * Returns true if this is a 'fired' event. Fired events indicate gesture activation,
   * such as a mouse-press.
   *
   * @see #flushed()
   */
  public boolean fired() {
    return _fire;
  }

  /**
   * @return the shortcut encapsulated by this event.
   * @see Shortcut
   */
  public Shortcut shortcut() {
    return new Shortcut(modifiers(), id());
  }

  /**
   * @return the modifiers defining the event {@link Shortcut}.
   */
  public int modifiers() {
    return _modifiers;
  }

  /**
   * Returns the id defining the event's {@link Shortcut}.
   */
  public int id() {
    return _id;
  }

  /**
   * @return the time at which the event occurs
   */
  public long timestamp() {
    return _timestamp;
  }

  /**
   * Only {@link frames.input.event.MotionEvent}s may be null.
   */
  public boolean isNull() {
    return false;
  }

  /**
   * @return true if Shift was down when the event occurs
   */
  public boolean isShiftDown() {
    return (_modifiers & SHIFT) != 0;
  }

  /**
   * @return true if Ctrl was down when the event occurs
   */
  public boolean isControlDown() {
    return (_modifiers & CTRL) != 0;
  }

  /**
   * @return true if Meta was down when the event occurs
   */
  public boolean isMetaDown() {
    return (_modifiers & META) != 0;
  }

  /**
   * @return true if Alt was down when the event occurs
   */
  public boolean isAltDown() {
    return (_modifiers & ALT) != 0;
  }

  /**
   * @return true if AltGraph was down when the event occurs
   */
  public boolean isAltGraph() {
    return (_modifiers & ALT_GRAPH) != 0;
  }

  /**
   * @param mask of modifiers
   * @return a String listing the event modifiers
   */
  public static String modifiersText(int mask) {
    String r = new String();
    if ((ALT & mask) == ALT)
      r += "ALT";
    if ((SHIFT & mask) == SHIFT)
      r += (r.length() > 0) ? "+SHIFT" : "SHIFT";
    if ((CTRL & mask) == CTRL)
      r += (r.length() > 0) ? "+CTRL" : "CTRL";
    if ((META & mask) == META)
      r += (r.length() > 0) ? "+META" : "META";
    if ((ALT_GRAPH & mask) == ALT_GRAPH)
      r += (r.length() > 0) ? "+ALT_GRAPH" : "ALT_GRAPH";
    return r;
  }
}
