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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

/**
 * A {@link Grabber} extension which allows to define
 * {@link Shortcut} to {@link java.lang.reflect.Method} bindings. See
 * {@link #setBinding(Shortcut, String)} and {@link #setBinding(Object, Shortcut, String)}
 * .
 * <p>
 * To attach a profile to a grabber first override your
 * {@link Grabber#performInteraction(BogusEvent)} method like this:
 * <p>
 * <pre>
 * {@code
 *   public void performInteraction(BogusEvent event) {
 *     profile.handle(event);
 *   }
 * }
 * </pre>
 * <p>
 * (see {@link #handle(BogusEvent)}) and then simply pass the grabber instance to the
 * {@link #Profile(Grabber)} constructor.
 */
public class Profile {
  class ObjectMethodTuple {
    Object object;
    Method method;

    ObjectMethodTuple(Object o, Method m) {
      object = o;
      method = m;
    }
  }

  protected HashMap<Shortcut, ObjectMethodTuple> map;
  protected Grabber grabber;

  // static stuff

  /**
   * Utility function to programmatically register virtual keys to a {@link remixlab.bias.Shortcut} class,
   * typically {@code KeyboardShortcuts}.
   */
  public static void registerVKeys(Class<? extends Shortcut> shortcutClass, Class<?> keyEventClass) {
    // TODO android needs testing
    // idea took from here:
    // http://stackoverflow.com/questions/15313469/java-keyboard-keycodes-list
    // and here:
    // http://www.java2s.com/Code/JavaAPI/java.lang.reflect/FieldgetIntObjectobj.htm
    String prefix = keyEventClass.getName().contains("android") ? "KEYCODE_" : "VK_";
    int l = prefix.length();
    Field[] fields = keyEventClass.getDeclaredFields();
    for (Field f : fields) {
      if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())) {
        Class<?> clazzType = f.getType();
        if (clazzType.toString().equals("int")) {
          int id = -1;
          try {
            id = f.getInt(keyEventClass);
            String name = f.getName();
            if (!Shortcut.hasID(shortcutClass, id) && name.substring(0, l).equals(prefix))
              Shortcut.registerID(shortcutClass, id, name);
          } catch (Exception e) {
            System.out.println("Warning: couldn't register key");
            e.printStackTrace();
          }
        }
      }
    }
  }

  public static Object context = null;

  /**
   * Attaches a profile to the given grabber.
   */
  public Profile(Grabber g) {
    map = new HashMap<Shortcut, ObjectMethodTuple>();
    grabber = g;
  }

  /**
   * Instantiates this profile from another profile. Both Profile {@link #grabber()}
   * should be of the same type.
   */
  public void set(Profile p) {
    if (grabber.getClass() != p.grabber.getClass()) {
      System.err.println("Profile grabbers should be of the same type");
      return;
    }
    map = new HashMap<Shortcut, ObjectMethodTuple>();
    for (Map.Entry<Shortcut, ObjectMethodTuple> entry : p.map().entrySet()) {
      if (entry.getValue().object == p.grabber)
        map.put(entry.getKey(), new ObjectMethodTuple(grabber, entry.getValue().method));
      else
        map.put(entry.getKey(), new ObjectMethodTuple(entry.getValue().object, entry.getValue().method));
    }
  }

  // public HashMap<Shortcut, Method>

  /**
   * Returns this profile set of shortcuts.
   */
  public Set<Shortcut> shortcuts() {
    return map.keySet();
  }

  /**
   * Returns the grabber to which this profile is attached.
   */
  public Grabber grabber() {
    return grabber;
  }

  /**
   * Internal use. Shortcut to object-method map.
   */
  protected HashMap<Shortcut, ObjectMethodTuple> map() {
    return map;
  }

  /**
   * Returns the {@link java.lang.reflect.Method} binding for the given
   * {@link Shortcut} key.
   *
   * @see #action(Shortcut)
   */
  public Method method(Shortcut shortcut) {
    return map.get(shortcut) == null ? null : map.get(shortcut).method;
  }

  /**
   * Returns the {@link java.lang.reflect.Method} binding for the given
   * {@link Shortcut} key.
   *
   * @see #method(Shortcut)
   */
  public String action(Shortcut shortcut) {
    Method m = method(shortcut);
    if (m == null)
      return null;
    return m.getName();
  }

  /**
   * Returns the action performing object. Either the {@link #grabber()} or an external
   * object.
   */
  public Object object(Shortcut shortcut) {
    return map.get(shortcut) == null ? null : map.get(shortcut).object;
  }

  /**
   * Main class method to be called from
   * {@link Grabber#performInteraction(BogusEvent)}. Calls an action
   * handler if the {@link BogusEvent#shortcut()} is bound.
   *
   * @see #setBinding(Shortcut, String)
   * @see #setBinding(Object, Shortcut, String)
   */
  public boolean handle(BogusEvent event) {
    Method iHandlerMethod = method(event.shortcut());
    if (iHandlerMethod != null) {
      try {
        if (object(event.shortcut()) == grabber)
          iHandlerMethod.invoke(object(event.shortcut()), new Object[]{event});
        else
          iHandlerMethod.invoke(object(event.shortcut()), new Object[]{grabber, event});
        return true;
      } catch (Exception e) {
        try {
          if (object(event.shortcut()) == grabber)
            iHandlerMethod.invoke(object(event.shortcut()), new Object[]{});
          else
            iHandlerMethod.invoke(object(event.shortcut()), new Object[]{grabber});
          return true;
        } catch (Exception empty) {
          System.out.println("Something went wrong when invoking your " + iHandlerMethod.getName() + " method");
          empty.printStackTrace();
        }
        System.out.println("Something went wrong when invoking your " + iHandlerMethod.getName() + " method");
        e.printStackTrace();
      }
    }
    return false;
  }

  /**
   * Internal macro.
   */
  protected boolean printWarning(Shortcut shortcut, String action) {
    if (action == null) {
      this.removeBinding(shortcut);
      System.out.println(shortcut.description() + " removed");
      return true;
    }
    if (hasBinding(shortcut)) {
      Method a = method(shortcut);
      if (a.getName().equals(action)) {
        System.out.println("Warning: shortcut " + shortcut.description() + " already bound to " + a.getName());
        return true;
      } else {
        System.out.println(
            "Warning: overwriting shortcut " + shortcut.description() + " which was previously bound to " + a.getName());
        return false;
      }
    }
    return false;
  }

  /**
   * Defines the shortcut that triggers the given action.
   * <p>
   * Attempt to set a shortcut for the {@code action} implemented by the {@link #context}
   * (e.g., the {@code PApplet} in the case of a Processing application) or the
   * {@link #grabber()} (e.g., the {@code InteractiveFrame} instance this profile is
   * attached to, in the case of a Processing application) when no prototype is found in
   * the {@link #context}.
   * <p>
   * The available action prototypes for the {@link #context} are:
   * <ol>
   * <li><b>public void action(grabber.getClass(), BogusEvent)</b></li>
   * <li><b>public void action(grabber.getClass())</b></li>
   * </ol>
   * <p>
   * The available action prototypes for the {@link #grabber()} are:
   * <ol>
   * <li><b>public void action(BogusEvent)</b></li>
   * <li><b>public void action()</b></li>
   * </ol>
   * <p>
   * The bogus-event type that may be passed to the above prototypes is the one specified
   * by the {@link Shortcut#eventClass()} method:
   * <ol>
   * <li>A {@link remixlab.bias.event.ClickEvent} for a
   * {@link remixlab.bias.event.ClickShortcut}</li>
   * <li>A {@link remixlab.bias.event.KeyboardEvent} for a
   * {@link remixlab.bias.event.KeyboardShortcut}</li>
   * <li>A {@code DOFnEvent} for a {@link remixlab.bias.event.MotionShortcut}, where
   * {@code n} is the {@link remixlab.bias.event.MotionShortcut#dofs(int)} of the
   * motion-shortcut {@link remixlab.bias.event.MotionShortcut#id()}.</li>
   * </ol>
   * <b>Note</b> that in the latter case a {@link remixlab.bias.event.MotionEvent} may be
   * passed too.
   *
   * @param shortcut {@link Shortcut}
   * @param action   {@link java.lang.String}
   * @see #setBinding(Object, Shortcut, String)
   * @see Shortcut#eventClass()
   * @see remixlab.bias.event.MotionShortcut#eventClass()
   * @see remixlab.bias.event.MotionShortcut#dofs(int)
   * @see remixlab.bias.event.MotionShortcut#registerID(int, int, String)
   */
  public boolean setBinding(Shortcut shortcut, String action) {
    if (printWarning(shortcut, action))
      return false;
    // 1. Search at context:
    String proto1 = null;
    Method method = null;
    if (context != null && context != grabber) {
      try {
        method = context.getClass().getMethod(action, new Class<?>[]{grabber.getClass(), shortcut.eventClass()});
      } catch (Exception clazz) {
        try {
          method = context.getClass().getMethod(action, new Class<?>[]{grabber.getClass()});
        } catch (Exception empty) {
          if (shortcut.defaultEventClass() != null)
            try {
              method = context.getClass().getMethod(action, new Class<?>[]{grabber.getClass(), shortcut.defaultEventClass()});
            } catch (Exception e) {
              proto1 = prototypes(context, shortcut, action);
            }
          else {
            proto1 = prototypes(context, shortcut, action);
          }
        }
      }
      if (method != null) {
        map.put(shortcut, new ObjectMethodTuple(context, method));
        return true;
      }
    }
    // 2. If not found, search at grabber:
    String proto2 = null;
    String other = ". Or, if your binding lies within other object, use setBinding(Object object, Shortcut key, String action) instead.";
    try {
      method = grabber.getClass().getMethod(action, new Class<?>[]{shortcut.eventClass()});
    } catch (Exception clazz) {
      try {
        method = grabber.getClass().getMethod(action, new Class<?>[]{});
      } catch (Exception empty) {
        if (shortcut.defaultEventClass() != null)
          try {
            method = grabber.getClass().getMethod(action, new Class<?>[]{shortcut.defaultEventClass()});
          } catch (Exception motion) {
            proto2 = prototypes(shortcut, action);
            System.out.println("Warning: not binding set! Check the existence of one of the following method prototypes: " + (
                proto1 != null ?
                    proto1 + ", " + proto2 :
                    proto2) + other);
          }
        else {
          proto2 = prototypes(shortcut, action);
          System.out.println("Warning: not binding set! Check the existence of one of the following method prototypes: " + (
              proto1 != null ?
                  proto1 + ", " + proto2 :
                  proto2) + other);
        }
      }
    }
    if (method != null) {
      map.put(shortcut, new ObjectMethodTuple(grabber, method));
      return true;
    }
    return false;
  }

  /**
   * Defines the shortcut that triggers the given action.
   * <p>
   * Attempt to set a shortcut for the {@code action} implemented by {@code object}. The
   * action procedure may have two different prototypes:
   * <ol>
   * <li><b>public void action(BogusEvent)</b></li>
   * <li><b>public void action()</b></li>
   * </ol>
   * The bogus-event type that may be passed to the first prototype is the one specified
   * by the {@link Shortcut#eventClass()} method:
   * <ol>
   * <li>A {@link remixlab.bias.event.ClickEvent} for a
   * {@link remixlab.bias.event.ClickShortcut}</li>
   * <li>A {@link remixlab.bias.event.KeyboardEvent} for a
   * {@link remixlab.bias.event.KeyboardShortcut}</li>
   * <li>A {@code DOFnEvent} for a {@link remixlab.bias.event.MotionShortcut}, where
   * {@code n} is the {@link remixlab.bias.event.MotionShortcut#dofs(int)} of the
   * motion-shortcut {@link remixlab.bias.event.MotionShortcut#id()}.</li>
   * </ol>
   * <b>Note</b> that in the latter case a {@link remixlab.bias.event.MotionEvent} may be
   * passed too.
   *
   * @param object   {@link java.lang.Object}
   * @param shortcut {@link Shortcut}
   * @param action   {@link java.lang.String}
   * @see #setBinding(Object, Shortcut, String)
   * @see Shortcut#eventClass()
   * @see remixlab.bias.event.MotionShortcut#eventClass()
   * @see remixlab.bias.event.MotionShortcut#dofs(int)
   * @see remixlab.bias.event.MotionShortcut#registerID(int, int, String)
   */
  public boolean setBinding(Object object, Shortcut shortcut, String action) {
    if (object == null) {
      System.out.println("Warning: no binding set. Object can't be null");
      return false;
    }
    if (object == grabber())
      return setBinding(shortcut, action);
    if (printWarning(shortcut, action))
      return false;
    Method method = null;
    try {
      method = object.getClass().getMethod(action, new Class<?>[]{grabber.getClass(), shortcut.eventClass()});
    } catch (Exception clazz) {
      try {
        method = object.getClass().getMethod(action, new Class<?>[]{grabber.getClass()});
      } catch (Exception empty) {
        if (shortcut.defaultEventClass() != null)
          try {
            method = object.getClass().getMethod(action, new Class<?>[]{grabber.getClass(), shortcut.defaultEventClass()});
          } catch (Exception e) {
            System.out.println(
                "Warning: not binding set! Check the existence of one of the following method prototypes: " + prototypes(
                    object, shortcut, action));
          }
        else {
          System.out.println(
              "Warning: not binding set! Check the existence of one of the following method prototypes:: " + prototypes(
                  object, shortcut, action));
        }
      }
    }
    if (method != null) {
      map.put(shortcut, new ObjectMethodTuple(object, method));
      return true;
    }
    return false;
  }

  /**
   * Internal use.
   *
   * @see #setBinding(Shortcut, String)
   * @see #setBinding(Object, Shortcut, String)
   */
  protected String prototypes(Object object, Shortcut shortcut, String action) {
    String sgn1 =
        "public void " + object.getClass().getSimpleName() + "." + action + "(" + grabber().getClass().getSimpleName()
            + ")";
    String sgn2 =
        "public void " + object.getClass().getSimpleName() + "." + action + "(" + grabber().getClass().getSimpleName()
            + ", " + shortcut.eventClass().getSimpleName() + ")";
    if (shortcut.defaultEventClass() != null) {
      String sgn3 =
          "public void " + object.getClass().getSimpleName() + "." + action + "(" + grabber().getClass().getSimpleName()
              + ", " + shortcut.defaultEventClass().getSimpleName() + ")";
      return sgn1 + ", " + sgn2 + ", " + sgn3;
    } else
      return sgn1 + ", " + sgn2;
  }

  /**
   * Internal use.
   *
   * @see #setBinding(Shortcut, String)
   */
  protected String prototypes(Shortcut shortcut, String action) {
    String sgn1 = "public void " + grabber.getClass().getSimpleName() + "." + action + "()";
    String sgn2 =
        "public void " + grabber.getClass().getSimpleName() + "." + action + "(" + shortcut.eventClass().getSimpleName() + ")";
    if (shortcut.defaultEventClass() != null) {
      String sgn3 =
          "public void " + grabber.getClass().getSimpleName() + "." + action + "(" + shortcut.defaultEventClass().getSimpleName()
              + ")";
      return sgn1 + ", " + sgn2 + ", " + sgn3;
    } else
      return sgn1 + ", " + sgn2;
  }

  /**
   * Removes the shortcut binding.
   *
   * @param shortcut {@link Shortcut}
   */
  public void removeBinding(Shortcut shortcut) {
    map.remove(shortcut);
  }

  /**
   * Removes all the shortcuts from this object.
   */
  public void removeBindings() {
    map.clear();
  }

  /**
   * Removes all the shortcuts from the given shortcut class.
   */
  public void removeBindings(Class<? extends Shortcut> cls) {
    Iterator<Entry<Shortcut, ObjectMethodTuple>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Shortcut, ObjectMethodTuple> pair = it.next();
      if (cls.equals(pair.getKey().getClass()))
        it.remove();
    }
  }

  /**
   * Returns a description of all the bindings this profile holds from the given shortcut
   * class.
   */
  public String info(Class<? extends Shortcut> cls) {
    String result = new String();
    HashMap<Shortcut, ObjectMethodTuple> clsMap = map(cls);
    String info = new String();
    for (Entry<Shortcut, ObjectMethodTuple> entry : clsMap.entrySet())
      info += entry.getKey().description() + " -> " + entry.getValue().method.getName() + "\n";
    if (!info.isEmpty()) {
      result += cls.getSimpleName() + " bindings:\n";
      result += info;
    }
    return result;
  }

  /**
   * (Internal) Used by {@link #info(Class)}.
   */
  protected HashMap<Shortcut, ObjectMethodTuple> map(Class<? extends Shortcut> cls) {
    HashMap<Shortcut, ObjectMethodTuple> result = new HashMap<Shortcut, ObjectMethodTuple>();
    for (Entry<Shortcut, ObjectMethodTuple> entry : map.entrySet())
      if (entry.getKey() != null && entry.getValue() != null)
        if (cls.equals(entry.getKey().getClass()))
          result.put(entry.getKey(), entry.getValue());
    return result;
  }

  /**
   * Returns a description of all the bindings this profile holds.
   */
  public String info() {
    // 1. Shortcut class list
    ArrayList<Class<? extends Shortcut>> list = new ArrayList<Class<? extends Shortcut>>();
    for (Shortcut s : map.keySet())
      if (!list.contains(s.getClass()))
        list.add(s.getClass());
    // 2. Print info per Shortcut class
    String result = new String();
    for (Class<? extends Shortcut> clazz : list) {
      String info = info(clazz);
      if (!info.isEmpty())
        result += info;
    }
    return result;
  }

  /**
   * Returns true if this object contains a binding for the specified shortcut.
   *
   * @param shortcut {@link Shortcut}
   * @return true if this object contains a binding for the specified shortcut.
   */
  public boolean hasBinding(Shortcut shortcut) {
    return map.containsKey(shortcut);
  }

  /**
   * Returns true if this object maps one or more shortcuts to the action specified by the
   * {@link #grabber()}.
   *
   * @param action {@link java.lang.String}
   * @return true if this object maps one or more shortcuts to the specified action.
   */
  public boolean isActionBound(String action) {
    for (ObjectMethodTuple tuple : map.values()) {
      if (grabber == tuple.object && tuple.method.getName().equals(action))
        return true;
    }
    return false;
  }

  /**
   * Returns true if this object maps one or more shortcuts to method specified by the
   * {@link #grabber()}.
   *
   * @param method {@link java.lang.reflect.Method}
   * @return true if this object maps one or more shortcuts to the specified action.
   */
  protected boolean isMethodBound(Method method) {
    return isMethodBound(grabber, method);
  }

  /**
   * Returns true if this object maps one or more shortcuts to the {@code method}
   * specified by the {@code object}.
   *
   * @param object {@link java.lang.Object}
   * @param method {@link java.lang.reflect.Method}
   * @return true if this object maps one or more shortcuts to the specified action.
   */
  protected boolean isMethodBound(Object object, Method method) {
    return map.containsValue(new ObjectMethodTuple(object, method));
  }
}