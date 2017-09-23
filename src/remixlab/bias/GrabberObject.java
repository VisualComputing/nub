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

import remixlab.bias.event.*;

/**
 * {@link Grabber} object which eases third-party implementation of the
 * {@link Grabber} interface.
 * <p>
 * Based on the concrete event type, this model object splits the
 * {@link #track(Event)} and the {@link #interact(Event)}
 * methods into more specific versions of them, e.g.,
 * {@link #track(ClickEvent)}, {@link #track(DOF3Event)},
 * {@link #interact(DOF6Event)} , {@link #interact(KeyEvent)} and
 * so on. Thus allowing implementations of this abstract GrabberObject to override only
 * those method signatures that might be of their interest.
 */
public abstract class GrabberObject implements Grabber {
  /**
   * Empty constructor.
   */
  public GrabberObject() {
  }

  /**
   * Constructs and adds this grabber to the agent pool.
   *
   * @see Agent#grabbers()
   */
  public GrabberObject(Agent agent) {
    agent.addGrabber(this);
  }

  /**
   * Constructs and adds this grabber to all agents belonging to the inputGrabber handler.
   *
   * @see InputHandler#agents()
   */
  public GrabberObject(InputHandler inputHandler) {
    inputHandler.addGrabber(this);
  }

  /**
   * Check if this object is the {@link Agent#inputGrabber()} . Returns
   * {@code true} if this object grabs the agent and {@code false} otherwise.
   */
  public boolean grabsInput(Agent agent) {
    return agent.inputGrabber() == this;
  }

  /**
   * Checks if the frame grabs inputGrabber from any agent registered at the given inputGrabber handler.
   */
  public boolean grabsInput(InputHandler inputHandler) {
    for (Agent agent : inputHandler.agents()) {
      if (agent.inputGrabber() == this)
        return true;
    }
    return false;
  }

  @Override
  public void interact(Event event) {
    if (event instanceof KeyEvent)
      interact((KeyEvent) event);
    if (event instanceof ClickEvent)
      interact((ClickEvent) event);
    if (event instanceof MotionEvent)
      interact((MotionEvent) event);
  }

  /**
   * Calls interact() on the proper motion event:
   * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
   * {@link remixlab.bias.event.DOF3Event} or {@link remixlab.bias.event.DOF6Event}.
   * <p>
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.MotionEvent}.
   */
  protected void interact(MotionEvent event) {
    if (event instanceof DOF1Event)
      interact((DOF1Event) event);
    if (event instanceof DOF2Event)
      interact((DOF2Event) event);
    if (event instanceof DOF3Event)
      interact((DOF3Event) event);
    if (event instanceof DOF6Event)
      interact((DOF6Event) event);
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link KeyEvent}.
   */
  protected void interact(KeyEvent event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.ClickEvent}.
   */
  protected void interact(ClickEvent event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.DOF1Event}.
   */
  protected void interact(DOF1Event event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.DOF2Event}.
   */
  protected void interact(DOF2Event event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.DOF3Event}.
   */
  protected void interact(DOF3Event event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.bias.event.DOF6Event}.
   */
  protected void interact(DOF6Event event) {
  }

  @Override
  public boolean track(Event event) {
    if (event instanceof KeyEvent)
      return track((KeyEvent) event);
    if (event instanceof ClickEvent)
      return track((ClickEvent) event);
    if (event instanceof MotionEvent)
      return track((MotionEvent) event);
    return false;
  }

  /**
   * Calls track() on the proper motion event:
   * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
   * {@link remixlab.bias.event.DOF3Event} or {@link remixlab.bias.event.DOF6Event}.
   * <p>
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  public boolean track(MotionEvent event) {
    if (event instanceof DOF1Event)
      return track((DOF1Event) event);
    if (event instanceof DOF2Event)
      return track((DOF2Event) event);
    if (event instanceof DOF3Event)
      return track((DOF3Event) event);
    if (event instanceof DOF6Event)
      return track((DOF6Event) event);
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  protected boolean track(KeyEvent event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.ClickEvent}.
   */
  protected boolean track(ClickEvent event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF1Event}.
   */
  protected boolean track(DOF1Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF2Event}.
   */
  protected boolean track(DOF2Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF3Event}.
   */
  protected boolean track(DOF3Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF6Event}.
   */
  protected boolean track(DOF6Event event) {
    return false;
  }
}
