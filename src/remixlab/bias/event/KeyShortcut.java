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
 * Key shortcuts can be of one out of two forms: 1. Characters (e.g., 'a'); 2.
 * Virtual keys (e.g., right arrow key); or, 2. Key combinations (e.g., CTRL key + virtual
 * key representing 'a').
 */
public final class KeyShortcut extends Shortcut {
  protected final char key;

  /**
   * Defines a key shortcut from the given character.
   *
   * @param k the character that defines the key shortcut.
   */
  public KeyShortcut(char k) {
    super();
    key = k;
  }

  /**
   * Defines a key shortcut from the given modifier mask and virtual key combination.
   *
   * @param m  the mask
   * @param vk the virtual key that defines the key shortcut.
   */
  public KeyShortcut(int m, int vk) {
    super(m, vk);
    key = '\0';
  }

  /**
   * Defines a key shortcut from the given virtual key.
   *
   * @param vk the virtual key that defines the key shortcut.
   */
  public KeyShortcut(int vk) {
    super(vk);
    key = '\0';
  }

  /**
   * Returns the key-shortcut key.
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
