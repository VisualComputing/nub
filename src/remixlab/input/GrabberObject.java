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
 * {@link #tapTracking(TapEvent)}, {@link #motion3Tracking(MotionEvent3)},
 * {@link #motion6Interaction(MotionEvent6)} , {@link #keyInteraction(KeyEvent)} and
 * so on. Thus allowing implementations of this abstract GrabberObject to override only
 * those method signatures that might be of their interest.
 */
public abstract class GrabberObject implements Grabber {
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
      keyInteraction((KeyEvent) event);
    if (event instanceof TapEvent)
      tapInteraction((TapEvent) event);
    if (event instanceof MotionEvent)
      motionInteraction((MotionEvent) event);
  }

  /**
   * Calls keyInteraction() on the proper motion event:
   * {@link MotionEvent1}, {@link MotionEvent2},
   * {@link MotionEvent3} or {@link MotionEvent6}.
   * <p>
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link remixlab.input.event.MotionEvent}.
   */
  protected void motionInteraction(MotionEvent event) {
    if (event instanceof MotionEvent1)
      motion1Interaction((MotionEvent1) event);
    if (event instanceof MotionEvent2)
      motion2Interaction((MotionEvent2) event);
    if (event instanceof MotionEvent3)
      motion3Interaction((MotionEvent3) event);
    if (event instanceof MotionEvent6)
      motion6Interaction((MotionEvent6) event);
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link KeyEvent}.
   */
  protected void keyInteraction(KeyEvent keyEvent) {
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link TapEvent}.
   */
  protected void tapInteraction(TapEvent tapEvent) {
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link MotionEvent1}.
   */
  protected void motion1Interaction(MotionEvent1 motionEvent1) {
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link MotionEvent2}.
   */
  protected void motion2Interaction(MotionEvent2 motionEvent2) {
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link MotionEvent3}.
   */
  protected void motion3Interaction(MotionEvent3 motionEvent3) {
  }

  /**
   * Override this method when you want the object to keyInteraction an interaction from a
   * {@link MotionEvent6}.
   */
  protected void motion6Interaction(MotionEvent6 motionEvent6) {
  }

  @Override
  public boolean track(Event event) {
    if (event instanceof KeyEvent)
      return keyTracking((KeyEvent) event);
    if (event instanceof TapEvent)
      return tapTracking((TapEvent) event);
    if (event instanceof MotionEvent)
      return motionTracking((MotionEvent) event);
    return false;
  }

  /**
   * Calls keyTracking() on the proper motion event:
   * {@link MotionEvent1}, {@link MotionEvent2},
   * {@link MotionEvent3} or {@link MotionEvent6}.
   * <p>
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  public boolean motionTracking(MotionEvent motionEvent) {
    if (motionEvent instanceof MotionEvent1)
      return motion1Tracking((MotionEvent1) motionEvent);
    if (motionEvent instanceof MotionEvent2)
      return motion2Tracking((MotionEvent2) motionEvent);
    if (motionEvent instanceof MotionEvent3)
      return motion3Tracking((MotionEvent3) motionEvent);
    if (motionEvent instanceof MotionEvent6)
      return motion6Tracking((MotionEvent6) motionEvent);
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link KeyEvent}.
   */
  protected boolean keyTracking(KeyEvent keyEvent) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link TapEvent}.
   */
  protected boolean tapTracking(TapEvent tapEvent) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent1}.
   */
  protected boolean motion1Tracking(MotionEvent1 motionEvent1) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent2}.
   */
  protected boolean motion2Tracking(MotionEvent2 motionEvent2) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent3}.
   */
  protected boolean motion3Tracking(MotionEvent3 motionEvent3) {
    return false;
  }

  /**
   * Override this method when you want the object to be picked from a
   * {@link MotionEvent6}.
   */
  protected boolean motion6Tracking(MotionEvent6 motionEvent6) {
    return false;
  }
}
