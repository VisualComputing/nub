/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.bias;

/**
 * Every {@link Event} instance has a shortcut which represents a
 * gesture-{@link #id()}. For instance, the button being dragged and the modifier key
 * pressed (see {@link #modifiers()}) at the very moment an user interaction takes place,
 * such as when she drags a giving mouse button while pressing the 'CTRL' modifier key.
 * See {@link Event#shortcut()}.
 * <p>
 * The current implementation supports the following event/shortcut types:
 * <ol>
 * <li>{@link remixlab.bias.event.MotionEvent} /
 * {@link remixlab.bias.Shortcut}. Note that motion-event derived classes:
 * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
 * {@link remixlab.bias.event.DOF3Event}, {@link remixlab.bias.event.DOF6Event}, are also
 * related to shortcuts.</li>
 * <li>{@link remixlab.bias.event.ClickEvent} / {@link remixlab.bias.event.ClickShortcut}
 * </li>
 * </ol>
 */
public class Shortcut {
  protected final int mask;
  protected final int id;

  /**
   * Constructs an "empty" shortcut. Same as: {@link #Shortcut(int)} with the integer
   * parameter being NO_NOMODIFIER_MASK.
   */
  public Shortcut() {
    mask = Event.NO_MODIFIER_MASK;
    id = Event.NO_ID;
  }

  /**
   * Defines a shortcut from the given id.
   *
   * @param _id gesture-id
   */
  public Shortcut(int _id) {
    mask = Event.NO_MODIFIER_MASK;
    id = _id;
  }

  /**
   * Defines a shortcut from the given modifier mask and id
   *
   * @param m modifier mask defining the shortcut
   */
  public Shortcut(int m, int i) {
    mask = m;
    id = i;
  }

  /**
   * Returns the shortcut's modifiers mask.
   */
  public int modifiers() {
    return mask;
  }

  /**
   * Returns the shortcut's id.
   */
  public int id() {
    return id;
  }

  /**
   * Returns whether or not this shortcut matches the other.
   *
   * @param other shortcut
   */
  public boolean matches(Shortcut other) {
    return id() == other.id() && modifiers() == other.modifiers();
  }
}
