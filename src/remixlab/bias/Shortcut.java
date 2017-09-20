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

import remixlab.util.EqualsBuilder;
import remixlab.util.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Shortcuts are {@link BogusEvent} means to bind user-defined actions
 * from a {@link BogusEvent}.
 * <p>
 * Every {@link BogusEvent} instance has a shortcut which represents a
 * gesture-{@link #id()}, for instance, the button being dragged and the modifier key
 * pressed (see {@link #modifiers()}) at the very moment an user interaction takes place,
 * such as when she drags a giving mouse button while pressing the 'CTRL' modifier key.
 * See {@link BogusEvent#shortcut()}. Note that for the shortcut
 * {@link #description()} to work properly, gesture-{@link #id()}s should be registered at
 * the shortcut class first (see {@link #registerID(String)}).
 * <p>
 * Different bogus event types should be related to different shortcuts. The current
 * implementation supports the following event/shortcut types:
 * <ol>
 * <li>{@link remixlab.bias.event.MotionEvent} /
 * {@link remixlab.bias.event.MotionShortcut}. Note that motion-event derived classes:
 * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
 * {@link remixlab.bias.event.DOF3Event}, {@link remixlab.bias.event.DOF6Event}, are also
 * related to motion shortcuts.</li>
 * <li>{@link remixlab.bias.event.ClickEvent} / {@link remixlab.bias.event.ClickShortcut}
 * </li>
 * <li>{@link remixlab.bias.event.KeyboardEvent} /
 * {@link remixlab.bias.event.KeyboardShortcut}</li>
 * </ol>
 * If you ever need to define your own shortcut type (such as when declaring a custom
 * bogus-event type) derive from this class and override {@link #eventClass()}.e.g.,
 * <p>
 * <pre>
 * {@code
 * protected Class<? extends CustomEvent> eventClass() {
 *   return CustomEvent.class;
 * }
 * }
 * </pre>
 * <p>
 * and implement a register id routine either from {@link #registerID(Class, String)} or
 * {@link #registerID(Class, int, String)}, e.g.,
 * <p>
 * <pre>
 * {@code
 * public static int registerID(int id, String description) {
 *   return Shortcut.registerID(CustomShortcut.class, id, description);
 * }
 * }
 * </pre>
 * <p>
 * Note that if your <b>CustomShortcut</b> class defines it's own attributes, its
 * {@link #hashCode()} and {@link #equals(Object)} methods should be overridden as well.
 */
public class Shortcut {
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(mask).append(id).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (obj == this)
      return true;
    if (obj.getClass() != getClass())
      return false;

    Shortcut other = (Shortcut) obj;
    return new EqualsBuilder().append(mask, other.mask).append(id, other.id).isEquals();
  }

  protected final int mask;
  protected final int id;
  protected static HashMap<String, String> ids = new HashMap<String, String>();

  /**
   * Constructs an "empty" shortcut. Same as: {@link #Shortcut(int)} with the integer
   * parameter being NO_NOMODIFIER_MASK.
   */
  public Shortcut() {
    mask = BogusEvent.NO_MODIFIER_MASK;
    id = BogusEvent.NO_ID;
  }

  /**
   * Defines a shortcut from the given id.
   *
   * @param _id gesture-id
   */
  public Shortcut(int _id) {
    mask = BogusEvent.NO_MODIFIER_MASK;
    id = _id;
  }

  /**
   * @param m modifier mask defining the shortcut
   */
  public Shortcut(int m, int i) {
    mask = m;
    id = i;
  }

  /**
   * Registers (and returns) the first available {@code id} for the shortcut {@code clazz}
   * with the given {@code description}.
   *
   * @see #registerID(Class, int, String)
   * @see #hasID(Class, int)
   */
  public static int registerID(Class<? extends Shortcut> clazz, String description) {
    int key = 0;
    ArrayList<String> stringIDS = new ArrayList<String>(ids.keySet());
    ArrayList<Integer> intIDS = new ArrayList<Integer>();
    int l = clazz.getSimpleName().length();
    for (String str : stringIDS)
      if (str.substring(0, l).equals(clazz.getSimpleName()))
        intIDS.add(Integer.parseInt(str.substring(l)));
    if (intIDS.size() > 0)
      key = Collections.max(intIDS) + 1;
    return registerID(clazz, key, description);
  }

  /**
   * Returns {@code true} if the given Shortcut {@code clazz} {@code id} is registered and
   * {@code false} otherwise.
   *
   * @see #registerID(Class, String)
   * @see #registerID(Class, int, String)
   */
  public static boolean hasID(Class<? extends Shortcut> clazz, int id) {
    return ids.containsKey((clazz.getSimpleName() + String.valueOf(id)));
  }

  /**
   * Registers (and returns) the given {@code id} for the shortcut {@code clazz} with the
   * given {@code description}.
   *
   * @see #registerID(Class, String)
   * @see #hasID(Class, int)
   */
  public static int registerID(Class<? extends Shortcut> clazz, int id, String description) {
    if (ids.put(clazz.getSimpleName() + String.valueOf(id), description) != null)
      System.out.println("Warning: overwriting id: " + id + " description");
    return id;
  }

  /**
   * Same as {@code return registerID(Shortcut.class, id, description)}.
   *
   * @see #registerID(Class, int, String)
   * @see #hasID(Class, int)
   */
  public static int registerID(int id, String description) {
    return registerID(Shortcut.class, id, description);
  }

  /**
   * Same as {@code return registerID(Shortcut.class, description)}.
   *
   * @see Shortcut#registerID(Class, String)
   * @see #hasID(Class, int)
   */
  public static int registerID(String description) {
    return registerID(Shortcut.class, description);
  }

  /**
   * Same as {@code return Shortcut.hasID(Shortcut.class, id)}.
   *
   * @see #registerID(int, String)
   * @see #hasID(Class, int)
   */
  public static boolean hasID(int id) {
    return Shortcut.hasID(Shortcut.class, id);
  }

  /**
   * Shortcut description.
   *
   * @return description as a String
   */
  public String description() {
    String m = BogusEvent.modifiersText(mask);
    String i = ids.get(getClass().getSimpleName() + String.valueOf(id));
    return ((m.length() > 0) ? m + "+" + i : i);
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
   * Returns the event class this shortcut is to be attached to. Should be non-null.
   *
   * @see #defaultEventClass()
   */
  protected Class<? extends BogusEvent> eventClass() {
    return BogusEvent.class;
  }

  /**
   * Returns the default-event class (alternative to {@link #eventClass()}) this shortcut is
   * to be attached to. Default value is {@code null}.
   * <p>
   * Override it when an optional {@link #eventClass()} is needed (rarely). For an example
   * refer to the {@code MotionEvent} implementation.
   *
   * @see #eventClass()
   */
  protected Class<? extends BogusEvent> defaultEventClass() {
    return null;
  }
}
