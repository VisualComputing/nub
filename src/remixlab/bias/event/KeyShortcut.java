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

/**
 * This class represents {@link KeyEvent} shortcuts.
 * <p>
 * Keyboard shortcuts can be of one out of two forms: 1. Characters (e.g., 'a'); 2.
 * Virtual keys (e.g., right arrow key); or, 2. Key combinations (e.g., CTRL key + virtual
 * key representing 'a').
 */
public final class KeyShortcut extends Shortcut {
  protected final char key;

  /**
   * Defines a keyboard shortcut from the given character.
   *
   * @param k the character that defines the keyboard shortcut.
   */
  public KeyShortcut(char k) {
    super();
    key = k;
  }

  /**
   * Defines a keyboard shortcut from the given modifier mask and virtual key combination.
   *
   * @param m  the mask
   * @param vk the virtual key that defines the keyboard shortcut.
   */
  public KeyShortcut(int m, int vk) {
    super(m, vk);
    key = '\0';
  }

  /**
   * Defines a keyboard shortcut from the given virtual key.
   *
   * @param vk the virtual key that defines the keyboard shortcut.
   */
  public KeyShortcut(int vk) {
    super(vk);
    key = '\0';
  }

  @Override
  public Class<? extends KeyEvent> eventClass() {
    return KeyEvent.class;
  }

  /**
   * Same as {@code return Shortcut.registerID(KeyShortcut.class, id, description)}.
   *
   * @see Shortcut#registerID(Class, int, String)
   * @see #hasID(int)
   */
  public static int registerID(int id, String description) {
    return Shortcut.registerID(KeyShortcut.class, id, description);
  }

  /**
   * Same as {@code return Shortcut.hasID(KeyShortcut.class, id)}.
   *
   * @see Shortcut#hasID(Class, int)
   * @see #registerID(int, String)
   */
  public static boolean hasID(int id) {
    return Shortcut.hasID(KeyShortcut.class, id);
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

  @Override
  public boolean matches(Shortcut other) {
    if(other instanceof KeyShortcut)
      return super.matches(other) && getKey() == ((KeyShortcut) other).getKey();
    return false;
  }
}
