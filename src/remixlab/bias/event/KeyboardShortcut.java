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

import remixlab.bias.Shortcut;
import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;

/**
 * This class represents {@link remixlab.bias.event.KeyboardEvent} shortcuts.
 * <p>
 * Keyboard shortcuts can be of one out of two forms: 1. Characters (e.g., 'a'); 2.
 * Virtual keys (e.g., right arrow key); or, 2. Key combinations (e.g., CTRL key + virtual
 * key representing 'a').
 */
public final class KeyboardShortcut extends Shortcut {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(key).toHashCode();
  }

  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    KeyboardShortcut rhs = (KeyboardShortcut) obj;
    return new EqualsBuilder().appendSuper(super.equals(obj)).append(key, rhs.key).isEquals();
  }

  protected final char key;

  /**
   * Defines a keyboard shortcut from the given character.
   *
   * @param k the character that defines the keyboard shortcut.
   */
  public KeyboardShortcut(char k) {
    super();
    key = k;
  }

  /**
   * Defines a keyboard shortcut from the given modifier mask and virtual key combination.
   *
   * @param m  the mask
   * @param vk the virtual key that defines the keyboard shortcut.
   */
  public KeyboardShortcut(int m, int vk) {
    super(m, vk);
    key = '\0';
  }

  /**
   * Defines a keyboard shortcut from the given virtual key.
   *
   * @param vk the virtual key that defines the keyboard shortcut.
   */
  public KeyboardShortcut(int vk) {
    super(vk);
    key = '\0';
  }

  @Override
  public Class<? extends KeyboardEvent> eventClass() {
    return KeyboardEvent.class;
  }

  /**
   * Same as {@code return Shortcut.registerID(KeyboardShortcut.class, id, description)}.
   *
   * @see Shortcut#registerID(Class, int, String)
   * @see #hasID(int)
   */
  public static int registerID(int id, String description) {
    return Shortcut.registerID(KeyboardShortcut.class, id, description);
  }

  /**
   * Same as {@code return Shortcut.hasID(KeyboardShortcut.class, id)}.
   *
   * @see Shortcut#hasID(Class, int)
   * @see #registerID(int, String)
   */
  public static boolean hasID(int id) {
    return Shortcut.hasID(KeyboardShortcut.class, id);
  }

  @Override
  public String description() {
    if (key != '\0')
      return String.valueOf(key);
    return super.description();
  }

  /**
   * Returns the keyboard-shortcut key.
   */
  public char getKey() {
    return key;
  }
}
