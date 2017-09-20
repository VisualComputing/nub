/**************************************************************************************
 * util_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.util;

/**
 * Interface for returning a deep copy on the object.
 * <p>
 * The pattern to perform a deep copy on an object using this Interface comprises a
 * two-step process:
 * <ol>
 * <li>Declare/implement a protected copy constructor
 * {@code protected Object(Object otherObject)}.</li>
 * <li>Override {@link #get()} by invoking the protected constructor
 * {@code return new Object(this)}.</li>
 * </ol>
 */
public interface Copyable {
  /**
   * Returns a deep copy of the object.
   * <p>
   * Typical implementation should simple look like: {@code return new Object(this)}.
   */
  public Object get();
}
