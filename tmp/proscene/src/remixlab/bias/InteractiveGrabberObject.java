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
 * A {@link remixlab.bias.GrabberObject} with a {@link Profile} instance which allows
 * {@link Shortcut} to {@link java.lang.reflect.Method} bindings high-level
 * customization (see all the <b>*Binding*()</b> methods). Refer to
 * {@link Profile#setBinding(Shortcut, String)} and
 * {@link Profile#setBinding(Object, Shortcut, String)} for the type of
 * actions and method signatures that may be bound.
 *
 * @see Profile
 */
public class InteractiveGrabberObject extends GrabberObject {
  protected Profile profile;

  /**
   * Empty constructor.
   */
  public InteractiveGrabberObject() {
    profile = new Profile(this);
  }

  /**
   * Constructs and adds this interactive-grabber object to the agent pool.
   *
   * @see Agent#grabbers()
   */
  public InteractiveGrabberObject(Agent agent) {
    super(agent);
    profile = new Profile(this);
  }

  /**
   * Constructs and adds this interactive-grabber object to all agents belonging to the input handler.
   *
   * @see InputHandler#agents()
   */
  public InteractiveGrabberObject(InputHandler inputHandler) {
    super(inputHandler);
    profile = new Profile(this);
  }

  /**
   * Same as {@code profile.handle(event)}.
   *
   * @see Profile#handle(BogusEvent)
   */
  @Override
  public void performInteraction(BogusEvent event) {
    profile.handle(event);
  }

  /**
   * Same as {@code profile.setBinding(shortcut, action)}.
   * <p>
   * Low-level profile handling routine. Call this method to set a binding for a custom bogus event, like this:
   * {@code grabber.setBinding(new CustomShortcut(mask, CustomAgent.CUSTOM_ID), "customBehavior")}.
   *
   * @see Profile#setBinding(Shortcut, String)
   * @see BogusEvent
   * @see Shortcut
   */
  public void setBinding(Shortcut shortcut, String action) {
    profile.setBinding(shortcut, action);
  }

  /**
   * Same as {@code profile.setBinding(object, shortcut, action)}.
   * <p>
   * Low-level profile handling routine. Call this method to set a binding for a custom bogus event, like this:
   * {@code grabber.setBinding(object, new CustomShortcut(mask, CustomAgent.CUSTOM_ID), "customBehavior")}.
   *
   * @see Profile#setBinding(Object, Shortcut, String)
   * @see BogusEvent
   * @see Shortcut
   */
  public void setBinding(Object object, Shortcut shortcut, String action) {
    profile.setBinding(object, shortcut, action);
  }

  /**
   * Same as {@code profile.set(otherGrabber.profile)}.
   *
   * @see Profile#set(Profile)
   */
  public void setBindings(InteractiveGrabberObject otherGrabber) {
    profile.set(otherGrabber.profile);
  }

  /**
   * Same as {@code return profile.hasBinding(shortcut)}.
   * <p>
   * <p>
   * Low-level profile handling routine. Call this method to query for a binding from a custom bogus event, like this:
   * {@code grabber.hasBinding(object, new CustomShortcut(mask, CustomAgent.CUSTOM_ID)}.
   *
   * @see Profile#hasBinding(Shortcut)
   * @see BogusEvent
   * @see Shortcut
   */
  public boolean hasBinding(Shortcut shortcut) {
    return profile.hasBinding(shortcut);
  }

  /**
   * Same as {@code profile.removeBinding(shortcut)}.
   * <p>
   * Low-level profile handling routine. Call this method to remove a binding for a custom bogus event, like this:
   * {@code grabber.removeBinding(new CustomShortcut(mask, CustomAgent.CUSTOM_ID)}.
   *
   * @see Profile#removeBinding(Shortcut)
   * @see BogusEvent
   * @see Shortcut
   */
  public void removeBinding(Shortcut shortcut) {
    profile.removeBinding(shortcut);
  }

  /**
   * Same as {@code profile.removeBindings()}.
   *
   * @see Profile#removeBindings()
   */
  public void removeBindings() {
    profile.removeBindings();
  }

  /**
   * Same as {@code profile.removeBindings(cls)}.
   *
   * @see Profile#removeBindings(Class)
   */
  public void removeBindings(Class<? extends Shortcut> cls) {
    profile.removeBindings(cls);
  }

  /**
   * Same as {@code profile.info(cls)}.
   *
   * @see Profile#info(Class)
   */
  public String info(Class<? extends Shortcut> cls) {
    return profile.info(cls);
  }

  /**
   * Returns a description of all the bindings this grabber holds.
   */
  public String info() {
    return profile.info();
  }

  /**
   * Same as {@code return profile.action(key)}.
   *
   * @see Profile#action(Shortcut)
   */
  public String action(Shortcut shortcut) {
    return profile.action(shortcut);
  }

  /**
   * Same as {@code return profile.isActionBound(action)}.
   *
   * @see Profile#isActionBound(String)
   */
  public boolean isActionBound(String action) {
    return profile.isActionBound(action);
  }
}