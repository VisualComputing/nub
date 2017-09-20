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
 * A click event encapsulates a {@link remixlab.bias.event.ClickShortcut} and it's defined
 * by the number of clicks. A click event holds the position where the event occurred (
 * {@link #x()} and {@link #y()}).
 */
public class ClickEvent extends BogusEvent {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(x).append(y).append(numberOfClicks)
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    ClickEvent other = (ClickEvent) obj;
    return new EqualsBuilder().appendSuper(super.equals(obj)).append(numberOfClicks, other.numberOfClicks)
        .append(x, other.x).append(y, other.y).isEquals();
  }

  protected float x, y;
  protected final int numberOfClicks;

  /**
   * Constructs a single click ClickEvent at the given position and from the given
   * gesture-id defining the events {@link #shortcut()}
   *
   * @param x
   * @param y
   * @param b
   */
  public ClickEvent(float x, float y, int b) {
    super(NO_MODIFIER_MASK, b);
    this.x = x;
    this.y = y;
    this.numberOfClicks = 1;
  }

  /**
   * Constructs a ClickEvent at the given position, from the given gesture-id defining the
   * events {@link #shortcut()}, and with the given number of clicks.
   *
   * @param x
   * @param y
   * @param b
   * @param clicks
   */
  public ClickEvent(float x, float y, int b, int clicks) {
    super(NO_MODIFIER_MASK, b);
    this.x = x;
    this.y = y;
    this.numberOfClicks = clicks;
  }

  /**
   * Constructs a ClickEvent at the given position, from the given gesture-id and
   * modifiers which defines the events {@link #shortcut()}, and with the given number of
   * clicks.
   *
   * @param x
   * @param y
   * @param modifiers
   * @param b
   * @param clicks
   */
  public ClickEvent(float x, float y, int modifiers, int b, int clicks) {
    super(modifiers, b);
    this.x = x;
    this.y = y;
    this.numberOfClicks = clicks;
  }

  protected ClickEvent(ClickEvent other) {
    super(other);
    this.x = other.x;
    this.y = other.y;
    this.numberOfClicks = other.numberOfClicks;
  }

  @Override
  public ClickEvent get() {
    return new ClickEvent(this);
  }

  @Override
  public ClickEvent flush() {
    return (ClickEvent) super.flush();
  }

  @Override
  public ClickEvent fire() {
    return (ClickEvent) super.fire();
  }

  @Override
  public ClickShortcut shortcut() {
    return new ClickShortcut(modifiers(), id(), clickCount());
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
   * @return event number of clicks
   */
  public int clickCount() {
    return numberOfClicks;
  }
}
