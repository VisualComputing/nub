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
 * {@link #checkIfGrabsInput(BogusEvent)} and the {@link #performInteraction(BogusEvent)}
 * methods into more specific versions of them, e.g.,
 * {@link #checkIfGrabsInput(ClickEvent)}, {@link #checkIfGrabsInput(DOF3Event)},
 * {@link #performInteraction(DOF6Event)} , {@link #performInteraction(KeyboardEvent)} and
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
   * Constructs and adds this grabber to all agents belonging to the input handler.
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
   * Checks if the frame grabs input from any agent registered at the given input handler.
   */
  public boolean grabsInput(InputHandler inputHandler) {
    for (Agent agent : inputHandler.agents()) {
      if (agent.inputGrabber() == this)
        return true;
    }
    return false;
  }

  @Override
  public void performInteraction(BogusEvent event) {
    if (event instanceof KeyboardEvent)
      performInteraction((KeyboardEvent) event);
    if (event instanceof ClickEvent)
      performInteraction((ClickEvent) event);
    if (event instanceof MotionEvent)
      performInteraction((MotionEvent) event);
  }

  /**
   * Calls performInteraction() on the proper motion event:
   * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
   * {@link remixlab.bias.event.DOF3Event} or {@link remixlab.bias.event.DOF6Event}.
   * <p>
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.MotionEvent}.
   */
  protected void performInteraction(MotionEvent event) {
    if (event instanceof DOF1Event)
      performInteraction((DOF1Event) event);
    if (event instanceof DOF2Event)
      performInteraction((DOF2Event) event);
    if (event instanceof DOF3Event)
      performInteraction((DOF3Event) event);
    if (event instanceof DOF6Event)
      performInteraction((DOF6Event) event);
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.KeyboardEvent}.
   */
  protected void performInteraction(KeyboardEvent event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.ClickEvent}.
   */
  protected void performInteraction(ClickEvent event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF1Event}.
   */
  protected void performInteraction(DOF1Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF2Event}.
   */
  protected void performInteraction(DOF2Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF3Event}.
   */
  protected void performInteraction(DOF3Event event) {
  }

  /**
   * Override this method when you want the object to perform an interaction from a
   * {@link remixlab.bias.event.DOF6Event}.
   */
  protected void performInteraction(DOF6Event event) {
  }

  @Override
  public boolean checkIfGrabsInput(BogusEvent event) {
    if (event instanceof KeyboardEvent)
      return checkIfGrabsInput((KeyboardEvent) event);
    if (event instanceof ClickEvent)
      return checkIfGrabsInput((ClickEvent) event);
    if (event instanceof MotionEvent)
      return checkIfGrabsInput((MotionEvent) event);
    return false;
  }

  /**
   * Calls checkIfGrabsInput() on the proper motion event:
   * {@link remixlab.bias.event.DOF1Event}, {@link remixlab.bias.event.DOF2Event},
   * {@link remixlab.bias.event.DOF3Event} or {@link remixlab.bias.event.DOF6Event}.
   * <p>
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.KeyboardEvent}.
   */
  public boolean checkIfGrabsInput(MotionEvent event) {
    if (event instanceof DOF1Event)
      return checkIfGrabsInput((DOF1Event) event);
    if (event instanceof DOF2Event)
      return checkIfGrabsInput((DOF2Event) event);
    if (event instanceof DOF3Event)
      return checkIfGrabsInput((DOF3Event) event);
    if (event instanceof DOF6Event)
      return checkIfGrabsInput((DOF6Event) event);
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.KeyboardEvent}.
   */
  protected boolean checkIfGrabsInput(KeyboardEvent event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.ClickEvent}.
   */
  protected boolean checkIfGrabsInput(ClickEvent event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF1Event}.
   */
  protected boolean checkIfGrabsInput(DOF1Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF2Event}.
   */
  protected boolean checkIfGrabsInput(DOF2Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF3Event}.
   */
  protected boolean checkIfGrabsInput(DOF3Event event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link remixlab.bias.event.DOF6Event}.
   */
  protected boolean checkIfGrabsInput(DOF6Event event) {
    return false;
  }
}
