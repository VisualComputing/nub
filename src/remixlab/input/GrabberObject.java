/**************************************************************************************
 * bias_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.input;

import remixlab.input.event.*;

/**
 * {@link Grabber} object which eases third-party implementation of the
 * {@link Grabber} interface.
 * <p>
 * Based on the concrete event type, this model object splits the
 * {@link #track(Event)} and the {@link #interact(Event)}
 * methods into more specific versions of them, e.g.,
 * {@link #track(TapEvent)}, {@link #track(MotionEvent3)},
 * {@link #interact(MotionEvent6)} , {@link #interact(KeyEvent)} and
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
    if (event instanceof TapEvent)
      interact((TapEvent) event);
    if (event instanceof MotionEvent)
      interact((MotionEvent) event);
  }

  /**
   * Calls interact() on the proper motion event:
   * {@link MotionEvent1}, {@link MotionEvent2},
   * {@link MotionEvent3} or {@link MotionEvent6}.
   * <p>
   * Override this method when you want the object to interact an interaction from a
   * {@link remixlab.input.event.MotionEvent}.
   */
  protected void interact(MotionEvent event) {
    if (event instanceof MotionEvent1)
      interact((MotionEvent1) event);
    if (event instanceof MotionEvent2)
      interact((MotionEvent2) event);
    if (event instanceof MotionEvent3)
      interact((MotionEvent3) event);
    if (event instanceof MotionEvent6)
      interact((MotionEvent6) event);
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link KeyEvent}.
   */
  protected void interact(KeyEvent event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link TapEvent}.
   */
  protected void interact(TapEvent event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent1}.
   */
  protected void interact(MotionEvent1 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent2}.
   */
  protected void interact(MotionEvent2 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent3}.
   */
  protected void interact(MotionEvent3 event) {
  }

  /**
   * Override this method when you want the object to interact an interaction from a
   * {@link MotionEvent6}.
   */
  protected void interact(MotionEvent6 event) {
  }

  @Override
  public boolean track(Event event) {
    if (event instanceof KeyEvent)
      return track((KeyEvent) event);
    if (event instanceof TapEvent)
      return track((TapEvent) event);
    if (event instanceof MotionEvent)
      return track((MotionEvent) event);
    return false;
  }

  /**
   * Calls track() on the proper motion event:
   * {@link MotionEvent1}, {@link MotionEvent2},
   * {@link MotionEvent3} or {@link MotionEvent6}.
   * <p>
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  public boolean track(MotionEvent event) {
    if (event instanceof MotionEvent1)
      return track((MotionEvent1) event);
    if (event instanceof MotionEvent2)
      return track((MotionEvent2) event);
    if (event instanceof MotionEvent3)
      return track((MotionEvent3) event);
    if (event instanceof MotionEvent6)
      return track((MotionEvent6) event);
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
   * {@link TapEvent}.
   */
  protected boolean track(TapEvent event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent1}.
   */
  protected boolean track(MotionEvent1 event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent2}.
   */
  protected boolean track(MotionEvent2 event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent3}.
   */
  protected boolean track(MotionEvent3 event) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent6}.
   */
  protected boolean track(MotionEvent6 event) {
    return false;
  }
}
