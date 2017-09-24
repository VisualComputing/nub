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
 * A tap event encapsulates a {@link TapShortcut} and it's defined
 * by the number of taps. A tap event holds the position where the event occurred (
 * {@link #x()} and {@link #y()}).
 */
public class TapEvent extends Event {
  protected float x, y;
  protected final int numberOfTaps;

  /**
   * Constructs a single TapEvent at the given position and from the given
   * gesture-id defining the events {@link #shortcut()}
   *
   * @param x
   * @param y
   * @param b
   */
  public TapEvent(float x, float y, int b) {
    super(NO_MODIFIER_MASK, b);
    this.x = x;
    this.y = y;
    this.numberOfTaps = 1;
  }

  /**
   * Constructs a TapEvent at the given position, from the given gesture-id defining the
   * events {@link #shortcut()}, and with the given number of taps.
   *
   * @param x
   * @param y
   * @param b
   * @param taps
   */
  public TapEvent(float x, float y, int b, int taps) {
    super(NO_MODIFIER_MASK, b);
    this.x = x;
    this.y = y;
    this.numberOfTaps = taps;
  }

  /**
   * Constructs a TapEvent at the given position, from the given gesture-id and
   * modifiers which defines the events {@link #shortcut()}, and with the given number of
   * taps.
   *
   * @param x
   * @param y
   * @param modifiers
   * @param b
   * @param taps
   */
  public TapEvent(float x, float y, int modifiers, int b, int taps) {
    super(modifiers, b);
    this.x = x;
    this.y = y;
    this.numberOfTaps = taps;
  }

  protected TapEvent(TapEvent other) {
    super(other);
    this.x = other.x;
    this.y = other.y;
    this.numberOfTaps = other.numberOfTaps;
  }

  @Override
  public TapEvent get() {
    return new TapEvent(this);
  }

  @Override
  public TapEvent flush() {
    return (TapEvent) super.flush();
  }

  @Override
  public TapEvent fire() {
    return (TapEvent) super.fire();
  }

  @Override
  public TapShortcut shortcut() {
    return new TapShortcut(modifiers(), id(), count());
  }

  /**
   * @return event x coordinate
   */
  public float x() {
    return x;
  }

  /**
   * @return event y coordinate
   */
  public float y() {
    return y;
  }

  /**
   * @return event number of taps
   */
  public int count() {
    return numberOfTaps;
  }
}
