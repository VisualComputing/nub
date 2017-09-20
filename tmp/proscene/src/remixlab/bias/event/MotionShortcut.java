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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents {@link remixlab.bias.event.MotionEvent} shortcuts.
 * <p>
 * Motion shortcuts can be of one of two forms: 1. A gesture-id (e.g., 'LEFT_ID' , or even
 * 'NO_ID') or, ; 2. A gesture-id + modifier key combinations (e.g., 'RIGHT_ID' + 'CTRL').
 * <p>
 * Note that the shortcut may be empty: the no-id (NO_ID) and no-modifier-mask
 * (NO_MODIFIER_MASK) combo may also defined a shortcut. Empty shortcuts may bind
 * gesture-less motion interactions (e.g., mouse move without any button pressed).
 * <p>
 * <b>Note</b> that the motion-event {@link #id()} DOFs should be registered first (see
 * {@link #registerID(int, String)}) before using the shortcut.
 */
public final class MotionShortcut extends Shortcut {
  protected static HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

  /**
   * Returns the {@link #id()} DOF's.
   * <p>
   * Returns {@code null} if nthe id is not registered.
   *
   * @see #registerID(int, String)
   * @see #registerID(int, int, String)
   * @see #hasID(int)
   */
  public static int dofs(int id) {
    if (!map.containsKey(id))
      System.out.println("MotionEvent id: " + id + " not registered. Call MotionShortcut.registerID(id) first");
    return map.get(id);
  }

  /**
   * Registers a MotionEvent {@link #id()} with the given {@code dof}s and
   * {@code description}.
   *
   * @param id  the intended {@link #id()} to be registered.
   * @param dof Motion id degrees-of-freedom. Either 1,2,3, or 6.
   * @return the id or an exception if the id exists.
   * @see #registerID(int, String)
   * @see #dofs(int)
   * @see #hasID(int)
   */
  public static int registerID(int id, int dof, String description) {
    if (map.containsKey(id)) {
      System.out.println(
          "Nothing done! id already present in MotionShortcut. Use an id different than: " + (new ArrayList<Integer>(
              map.keySet())).toString());
    } else if (dof == 1 || dof == 2 || dof == 3 || dof == 6) {
      Shortcut.registerID(MotionShortcut.class, id, description);
      map.put(id, dof);
    } else
      System.out.println("Nothing done! dofs in MotionShortcut.registerMotionID should be either 1, 2, 3 or 6.");
    return id;
  }

  /**
   * Registers a MotionEvent {@link #id()} with the given {@code dof}s and
   * {@code description}.
   *
   * @param dof Motion id degrees-of-freedom. Either 1,2,3, or 6.
   * @return the id.
   * @see #registerID(int, int, String)
   * @see #dofs(int)
   * @see #hasID(int)
   */
  public static int registerID(int dof, String description) {
    int key = 0;
    if (dof != 1 && dof != 2 && dof != 3 && dof != 6)
      System.out.println("Warning: Nothing done! dofs in Profile.registerMotionID should be either 1, 2, 3 or 6.");
    else {
      key = Shortcut.registerID(MotionShortcut.class, description);
      map.put(key, dof);
    }
    return key;
  }

  /**
   * Same as {@code return Shortcut.hasID(MotionShortcut.class, id)}.
   *
   * @see Shortcut#hasID(Class, int)
   * @see #registerID(int, String)
   * @see #registerID(int, int, String)
   */
  public static boolean hasID(int id) {
    return Shortcut.hasID(MotionShortcut.class, id);
  }

  /**
   * Constructs an "empty" shortcut by conveniently calling
   * {@code this(NO_MODIFIER_MASK, NO_ID);}
   */
  public MotionShortcut() {
    super();
  }

  /**
   * Defines a shortcut from the given gesture-id.
   *
   * @param id gesture-id
   */
  public MotionShortcut(int id) {
    super(id);
  }

  /**
   * Defines a shortcut from the given modifier mask and gesture-id combination.
   *
   * @param m  the mask
   * @param id gesture-id
   */
  public MotionShortcut(int m, int id) {
    super(m, id);
  }

  /**
   * Returns {@code DOFnEvent.class} where {@code n} is {@link #dofs(int)}.
   * <p>
   * Returns {@code MotionEvent.class} if the shortcut {@link #id()} isn't registered.
   *
   * @see #dofs(int)
   * @see MotionShortcut#registerID(int, int, String)
   */
  @Override
  public Class<? extends MotionEvent> eventClass() {
    Class<? extends MotionEvent> clazz = MotionEvent.class;
    if ((Integer) dofs(id()) != null)
      switch (dofs(id())) {
        case 1:
          clazz = DOF1Event.class;
          break;
        case 2:
          clazz = DOF2Event.class;
          break;
        case 3:
          clazz = DOF3Event.class;
          break;
        case 6:
          clazz = DOF6Event.class;
          break;
      }
    return clazz;
  }

  @Override
  public Class<? extends MotionEvent> defaultEventClass() {
    return MotionEvent.class;
  }
}
