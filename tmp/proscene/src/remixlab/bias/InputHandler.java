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
import java.util.LinkedList;
import java.util.List;

/**
 * The InputHandler object is the high level package handler which holds a collection of
 * {@link #agents()}, and an event dispatcher queue of
 * {@link EventGrabberTuple}s ({@link #eventTupleQueue()}). Such tuple
 * represents a message passing to application objects, allowing an object to be
 * instructed to perform a particular user-defined action from a given
 * {@link BogusEvent}. For an introduction to BIAS please refer to
 * <a href="http://nakednous.github.io/projects/bias">this</a>.
 * <p>
 * At runtime, the input handler should continuously run the two loops defined in
 * {@link #handle()}. Therefore, simply attach a call to {@link #handle()} at the end of
 * your main event (drawing) loop for that to take effect (like it's done in
 * <b>dandelion</b> by the <b>AbstractScene.postDraw()</b> method).
 */
public class InputHandler {
  // D E V I C E S & E V E N T S
  protected List<Agent> agents;
  protected LinkedList<EventGrabberTuple> eventTupleQueue;

  public InputHandler() {
    // agents
    agents = new ArrayList<Agent>();
    // events
    eventTupleQueue = new LinkedList<EventGrabberTuple>();
  }

  /**
   * Main handler method. Call it at the end of your main event (drawing) loop (like it's
   * done in <b>dandelion</b> by the <b>AbstractScene.postDraw()</b> method)
   * <p>
   * The handle comprises the following two loops:
   * <p>
   * 1. {@link EventGrabberTuple} producer loop which for each
   * registered agent calls: a.
   * {@link Agent#updateTrackedGrabber(BogusEvent)}; and, b.
   * {@link Agent#handle(BogusEvent)}. Note that the bogus event are
   * obtained from the agents callback
   * {@link Agent#updateTrackedGrabberFeed()} and
   * {@link Agent#handleFeed()} methods, respectively. The bogus event
   * may also be obtained from {@link Agent#handleFeed()} which may
   * replace both of the previous feeds when they are null.<br>
   * 2. User-defined action consumer loop: which for each
   * {@link EventGrabberTuple} calls
   * {@link EventGrabberTuple#perform()}.<br>
   *
   * @see Agent#feed()
   * @see Agent#updateTrackedGrabberFeed()
   * @see Agent#handleFeed()
   */
  public void handle() {
    // 1. Agents
    for (Agent agent : agents()) {
      agent.updateTrackedGrabber(
          agent.updateTrackedGrabberFeed() != null ? agent.updateTrackedGrabberFeed() : agent.feed());
      agent.handle(agent.handleFeed() != null ? agent.handleFeed() : agent.feed());
    }
    // 2. Low level events
    while (!eventTupleQueue.isEmpty())
      eventTupleQueue.remove().perform();
  }

  /**
   * Calls {@link Agent#addGrabber(Grabber)} on registered
   * {@link #agents()}.
   */
  public void addGrabber(Grabber grabber) {
    for (Agent agent : agents())
      agent.addGrabber(grabber);
  }

  /**
   * Calls {@link Agent#removeGrabber(Grabber)} on registered
   * {@link #agents()}.
   */
  public void removeGrabber(Grabber grabber) {
    for (Agent agent : agents())
      agent.removeGrabber(grabber);
  }

  /**
   * Calls {@link Agent#removeGrabbers()} on registered
   * {@link #agents()}.
   */
  public void removeGrabbers() {
    for (Agent agent : agents())
      agent.removeGrabbers();
  }

  /**
   * Calls {@link Agent#setDefaultGrabber(Grabber)} on registered
   * {@link #agents()}.
   */
  public void setDefaultGrabber(Grabber grabber) {
    for (Agent agent : agents())
      agent.setDefaultGrabber(grabber);
  }

  /**
   * Calls {@link Agent#shiftDefaultGrabber(Grabber, Grabber)} on
   * registered {@link #agents()}.
   */
  public void shiftDefaultGrabber(Grabber g1, Grabber g2) {
    for (Agent agent : agents())
      agent.shiftDefaultGrabber(g1, g2);
  }

  /**
   * Returns {@code true} if {@link Agent#isInputGrabber(Grabber)} is
   * {@code true} for at least one agent in {@link #agents()}.
   */
  public boolean isInputGrabber(Grabber g) {
    for (Agent agent : agents())
      if (agent.isInputGrabber(g))
        return true;
    return false;
  }

  /**
   * Returns {@code true} if {@link Agent#hasGrabber(Grabber)} is
   * {@code true} for at least one agent in {@link #agents()}.
   */
  public boolean hasGrabber(Grabber g) {
    for (Agent agent : agents())
      if (agent.hasGrabber(g))
        return true;
    return false;
  }

  /**
   * Calls {@link Agent#resetTrackedGrabber()} on registered
   * {@link #agents()}.
   */
  public void resetTrackedGrabber() {
    for (Agent agent : agents())
      agent.resetTrackedGrabber();
  }

  /**
   * Returns a list of the registered agents.
   */
  public List<Agent> agents() {
    return agents;
  }

  /**
   * Registers the given agent.
   */
  public boolean registerAgent(Agent agent) {
    if (agents().contains(agent))
      return false;
    else
      return agents().add(agent);
  }

  /**
   * Returns true if the given agent is registered.
   */
  public boolean isAgentRegistered(Agent agent) {
    return agents().contains(agent);
  }

  /**
   * Unregisters the given agent.
   */
  public boolean unregisterAgent(Agent agent) {
    return agents().remove(agent);
  }

  /**
   * Unregisters all agents from the handler.
   */
  public void unregisterAgents() {
    agents.clear();
  }

  /**
   * Returns the event tuple queue. Rarely needed.
   */
  public LinkedList<EventGrabberTuple> eventTupleQueue() {
    return eventTupleQueue;
  }

  /**
   * Enqueues the eventTuple for later execution which happens at the end of
   * {@link #handle()}. Returns {@code true} if succeeded and {@code false} otherwise.
   *
   * @see #handle()
   */
  public boolean enqueueEventTuple(EventGrabberTuple eventTuple) {
    if (!eventTupleQueue.contains(eventTuple))
      return eventTupleQueue.add(eventTuple);
    return false;
  }

  /**
   * Removes the given event from the event queue. No action is executed.
   *
   * @param event to be removed.
   */
  public void removeEventTuple(BogusEvent event) {
    eventTupleQueue.remove(event);
  }

  /**
   * Clears the event queue. Nothing is executed.
   */
  public void removeEventTuples() {
    eventTupleQueue.clear();
  }
}
