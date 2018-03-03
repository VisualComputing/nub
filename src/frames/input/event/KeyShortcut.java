/****************************************************************************************
 * framesjs
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a raster or ray-tracing renderer. Released under the terms of the GNU
 * Public License v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.input.event;

import frames.input.Shortcut;

/**
 * This class represents {@link KeyEvent} shortcuts.
 * <p>
 * Key shortcuts can be of one out of two forms: 1. Characters (e.g., 'a'); 2.
 * Virtual keys (e.g., right arrow key); or, 2. Key combinations (e.g., CTRL key + virtual
 * key representing 'a').
 */
public class KeyShortcut extends Shortcut {
  protected char _key;

  /**
   * Defines a key shortcut from the given character.
   *
   * @param key the character that defines the key shortcut.
   */
  public KeyShortcut(char key) {
    super();
    _key = key;
  }

  /**
   * Defines a key shortcut from the given modifier mask and virtual key combination.
   *
   * @param modifiers  the mask
   * @param virtualKey the virtual key that defines the key shortcut.
   */
  public KeyShortcut(int modifiers, int virtualKey) {
    super(modifiers, virtualKey);
    _key = '\0';
  }

  /**
   * Defines a key shortcut from the given virtual key.
   *
   * @param virtualKey the virtual key that defines the key shortcut.
   */
  public KeyShortcut(int virtualKey) {
    super(virtualKey);
    _key = '\0';
  }

  /**
   * Returns the key-shortcut key.
   */
  public char getKey() {
    return _key;
  }

  @Override
  public boolean matches(Shortcut other) {
    if (super.matches(other))
      return getKey() == ((KeyShortcut) other).getKey();
    return false;
  }
}
