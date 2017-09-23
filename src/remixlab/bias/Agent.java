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

import java.util.ArrayList;
import java.util.List;

/**
 * Agents gather data from different sources --mostly from input devices such touch
 * surfaces or simple mice-- and reduce them into a rather simple but quite 'useful' set
 * of interface events ({@link Event} ) for third party objects (
 * {@link Grabber} objects) to consume them (
 * {@link #handle(Event)}). Agents thus effectively open up a channel between all
 * kinds of input data sources and user-space objects. To add/removeGrabbers a grabber to/from the
 * {@link #grabbers()} collection issue {@link #addGrabber(Grabber)} /
 * {@link #removeGrabber(Grabber)} calls. Derive from this agent and either call
 * {@link #handle(Event)} or override {@link #handleFeed()} .
 * <p>
 * The agent may send events to its {@link #inputGrabber()} which may be regarded as
 * the agent's grabber target. The {@link #inputGrabber()} may be set by querying each
 * grabber object in {@link #grabbers()} to check if its
 * {@link Grabber#track(Event)}) condition is met (see
 * {@link #poll(Event)}, {@link #pollFeed()}). The
 * first grabber meeting the condition, namely the {@link #trackedGrabber()}), will then
 * be set as the {@link #inputGrabber()}. When no grabber meets the condition, the
 * {@link #trackedGrabber()} is then set to null. In this case, a non-null
 * {@link #inputGrabber()} may still be set with {@link #setDefaultGrabber(Grabber)} (see
 * also {@link #defaultGrabber()}).
 */
public abstract class Agent {
  protected List<Grabber> grabberList;
  protected Grabber trackedGrabber, defaultGrabber;
  protected boolean agentTrckn;
  protected InputHandler handler;

  /**
   * Constructs an Agent and registers is at the given inputHandler.
   */
  public Agent(InputHandler inputHandler) {
    grabberList = new ArrayList<Grabber>();
    setTracking(true);
    handler = inputHandler;
    handler.registerAgent(this);
  }

  // 1. Grabbers

  /**
   * Removes the grabber from the {@link #grabbers()} list.
   *
   * @see #removeGrabbers()
   * @see #addGrabber(Grabber)
   * @see #hasGrabber(Grabber)
   * @see #grabbers()
   */
  public boolean removeGrabber(Grabber grabber) {
    if (defaultGrabber() == grabber)
      setDefaultGrabber(null);
    if (trackedGrabber() == grabber)
      resetTrackedGrabber();
    return grabberList.remove(grabber);
  }

  /**
   * Clears the {@link #grabbers()} list.
   *
   * @see #removeGrabber(Grabber)
   * @see #addGrabber(Grabber)
   * @see #hasGrabber(Grabber)
   * @see #grabbers()
   */
  public void removeGrabbers() {
    setDefaultGrabber(null);
    trackedGrabber = null;
    grabberList.clear();
  }

  /**
   * Returns the list of grabber (and interactive-grabber) objects handled by this agent.
   *
   * @see #removeGrabber(Grabber)
   * @see #addGrabber(Grabber)
   * @see #hasGrabber(Grabber)
   * @see #removeGrabbers()
   */
  public List<Grabber> grabbers() {
    return grabberList;
  }

  /**
   * Returns true if the grabber is currently in the agents {@link #grabbers()} list.
   *
   * @see #removeGrabber(Grabber)
   * @see #addGrabber(Grabber)
   * @see #grabbers()
   * @see #removeGrabbers()
   */
  public boolean hasGrabber(Grabber grabber) {
    for (Grabber g : grabbers())
      if (g == grabber)
        return true;
    return false;
  }

  /**
   * Adds the grabber in {@link #grabbers()}.
   *
   * @see #removeGrabber(Grabber)
   * @see #hasGrabber(Grabber)
   * @see #grabbers()
   * @see #removeGrabbers()
   */
  public boolean addGrabber(Grabber grabber) {
    if (grabber == null)
      return false;
    if (hasGrabber(grabber))
      return false;
    return grabberList.add(grabber);
  }

  /**
   * Feeds {@link #poll(Event)} and {@link #handle(Event)} with
   * the returned event. Returns null by default. Use it in place of
   * {@link #pollFeed()} and/or {@link #handleFeed()} which take
   * higher-precedence.
   * <p>
   * Automatically call by the main event loop (
   * {@link InputHandler#handle()}). See ProScene's Space-Navigator
   * example.
   *
   * @see InputHandler#handle()
   * @see #handleFeed()
   * @see #pollFeed()
   * @see #handle(Event)
   * @see #poll(Event)
   */
  protected Event feed() {
    return null;
  }

  /**
   * Feeds {@link #handle(Event)} with the returned event. Returns null by default.
   * Use it in place of {@link #feed()} which takes lower-precedence.
   * <p>
   * Automatically call by the main event loop (
   * {@link InputHandler#handle()}). See ProScene's Space-Navigator
   * example.
   *
   * @see InputHandler#handle()
   * @see #feed()
   * @see #pollFeed()
   * @see #handle(Event)
   * @see #poll(Event)
   */
  protected Event handleFeed() {
    return null;
  }

  /**
   * Feeds {@link #poll(Event)} with the returned event. Returns null
   * by default. Use it in place of {@link #feed()} which takes lower-precedence.
   * <p>
   * Automatically call by the main event loop (
   * {@link InputHandler#handle()}).
   *
   * @see InputHandler#handle()
   * @see #feed()
   * @see #handleFeed()
   * @see #handle(Event)
   * @see #poll(Event)
   */
  protected Event pollFeed() {
    return null;
  }

  /**
   * Returns the {@link InputHandler} this agent is registered to.
   */
  public InputHandler inputHandler() {
    return handler;
  }

  /**
   * If {@link #isTracking()} and the agent is registered at the {@link #inputHandler()}
   * then queries each object in the {@link #grabbers()} to check if the
   * {@link Grabber#track(Event)}) condition is met.
   * The first object meeting the condition will be set as the {@link #inputGrabber()} and
   * returned. Note that a null grabber means that no object in the {@link #grabbers()}
   * met the condition. A {@link #inputGrabber()} may also be enforced simply with
   * {@link #setDefaultGrabber(Grabber)}.
   *
   * @param event to query the {@link #grabbers()}
   * @return the new grabber which may be null.
   * @see #setDefaultGrabber(Grabber)
   * @see #isTracking()
   * @see #handle(Event)
   * @see #trackedGrabber()
   * @see #defaultGrabber()
   * @see #inputGrabber()
   */
  protected Grabber poll(Event event) {
    if (event == null || !inputHandler().isAgentRegistered(this) || !isTracking())
      return trackedGrabber();
    // We first check if default grabber is trackedGrabber,
    // i.e., default grabber has the highest priority (which is good for
    // keyboards and doesn't hurt motion grabbers:
    Grabber dG = defaultGrabber();
    if (dG != null)
      if (dG.track(event)) {
        trackedGrabber = dG;
        return trackedGrabber();
      }
    // then if trackedGrabber grabber remains the matches:
    Grabber tG = trackedGrabber();
    if (tG != null)
      if (tG.track(event))
        return trackedGrabber();
    // pick the first otherwise
    trackedGrabber = null;
    for (Grabber grabber : grabberList)
      if (grabber != dG && grabber != tG)
        if (grabber.track(event)) {
          trackedGrabber = grabber;
          return trackedGrabber();
        }
    return trackedGrabber();
  }

  /**
   * Enqueues an Tuple(event, input()) on the
   * {@link InputHandler#tupleQueue()}, thus enabling a call on
   * the {@link #inputGrabber()}
   * {@link Grabber#interact(Event)} method (which is
   * scheduled for execution till the end of this main event loop iteration, see
   * {@link InputHandler#enqueueTuple(Tuple)} for
   * details).
   *
   * @see #inputGrabber()
   * @see #poll(Event)
   */
  protected boolean handle(Event event) {
    if (event == null || !handler.isAgentRegistered(this) || inputHandler() == null)
      return false;
    //Just trying to get rid of the nasty MotionEvent dependency
    //TODO: really needs testing everywhere, -jp
    //if (event instanceof MotionEvent)
      //if (((MotionEvent) event).isAbsolute())
        if (event.isNull() && !event.flushed())
          return false;
    Grabber inputGrabber = inputGrabber();
    if (inputGrabber != null)
      return inputHandler().enqueueTuple(new Tuple(event, inputGrabber));
    return false;
  }

  /**
   * If {@link #trackedGrabber()} is non null, returns it. Otherwise returns the
   * {@link #defaultGrabber()}.
   *
   * @see #trackedGrabber()
   */
  public Grabber inputGrabber() {
    return trackedGrabber() != null ? trackedGrabber() : defaultGrabber();
  }

  /**
   * Returns true if {@code g} is the agent's {@link #inputGrabber()} and false otherwise.
   */
  public boolean isInputGrabber(Grabber g) {
    return inputGrabber() == g;
  }

  /**
   * Returns {@code true} if this agent is tracking its grabbers.
   * <p>
   * You may need to {@link #enableTracking()} first.
   */
  public boolean isTracking() {
    return agentTrckn;
  }

  /**
   * Enables tracking so that the {@link #inputGrabber()} may be updated when calling
   * {@link #poll(Event)}.
   *
   * @see #disableTracking()
   */
  public void enableTracking() {
    setTracking(true);
  }

  /**
   * Disables tracking.
   *
   * @see #enableTracking()
   */
  public void disableTracking() {
    setTracking(false);
  }

  /**
   * Sets the {@link #isTracking()} value.
   */
  public void setTracking(boolean enable) {
    agentTrckn = enable;
    if (!isTracking())
      trackedGrabber = null;
  }

  /**
   * Calls {@link #setTracking(boolean)} to toggle the {@link #isTracking()} value.
   */
  public void toggleTracking() {
    setTracking(!isTracking());
  }

  /**
   * Returns the grabber set after {@link #poll(Event)} is called. It
   * may be null.
   */
  public Grabber trackedGrabber() {
    return trackedGrabber;
  }

  /**
   * Default {@link #inputGrabber()} returned when {@link #trackedGrabber()} is null and
   * set with {@link #setDefaultGrabber(Grabber)}.
   *
   * @see #inputGrabber()
   * @see #trackedGrabber()
   */
  public Grabber defaultGrabber() {
    return defaultGrabber;
  }

  /**
   * Same as
   * {@code defaultGrabber() != g1 ? setDefaultGrabber(g1) ? true : setDefaultGrabber(g2) : setDefaultGrabber(g2)}
   * which is ubiquitous among the examples.
   */
  public boolean shiftDefaultGrabber(Grabber g1, Grabber g2) {
    return defaultGrabber() != g1 ? setDefaultGrabber(g1) ? true : setDefaultGrabber(g2) : setDefaultGrabber(g2);
    // return defaultGrabber() == g1 ? setDefaultGrabber(g2) ? true : false :
    // defaultGrabber() == g2 ? setDefaultGrabber(g1) : false;
  }

  /**
   * Sets the {@link #defaultGrabber()}
   * <p>
   * {@link #inputGrabber()}
   */
  public boolean setDefaultGrabber(Grabber grabber) {
    if (grabber == null) {
      this.defaultGrabber = null;
      return true;
    }
    if (!hasGrabber(grabber)) {
      System.out.println(
          "To set a " + getClass().getSimpleName() + " default grabber the " + grabber.getClass().getSimpleName()
              + " should be added into agent first. Use one of the agent addGrabber() methods");
      return false;
    }
    defaultGrabber = grabber;
    return true;
  }

  /**
   * Sets the {@link #trackedGrabber()} to {@code null}.
   */
  public void resetTrackedGrabber() {
    trackedGrabber = null;
  }
}
