/****************************************************************************************
 * framesjs
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a raster or ray-tracing renderer. Released under the terms of the GNU
 * Public License v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.input;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The InputHandler object is the high level package handler which holds a collection of
 * {@link #agents()}, and an event dispatcher queue of
 * {@link Tuple}s ({@link #tupleQueue()}). Such tuple
 * represents a message passing to application objects, allowing an object to be
 * instructed to interact a particular user-defined action from a given
 * {@link Event}. For an introduction to BIAS please refer to
 * <a href="http://nakednous.github.io/projects/bias">this</a>.
 * <p>
 * At runtime, the inputGrabber handler should continuously run the two loops defined in
 * {@link #handle()}. Therefore, simply attach a call to {@link #handle()} at the end of
 * your main event (drawing) loop for that to take effect.
 */
public class InputHandler {
  // D E V I C E S & E V E N T S
  protected List<Agent> _agents;
  protected LinkedList<Tuple> _tupleQueue;

  public InputHandler() {
    // agents
    _agents = new ArrayList<Agent>();
    // events
    _tupleQueue = new LinkedList<Tuple>();
  }

  /**
   * Main handler method. Call it at the end of your main event (drawing) loop.
   * <p>
   * The handle comprises the following two loops:
   * <p>
   * 1. {@link Tuple} producer loop which for each
   * registered agent calls: a.
   * {@link Agent#poll(Event)}; and, b.
   * {@link Agent#handle(Event)}. Note that the event are
   * obtained from the agents callback
   * {@link Agent#pollFeed()} and
   * {@link Agent#handleFeed()} methods, respectively. The event
   * may also be obtained from {@link Agent#handleFeed()} which may
   * replace both of the previous feeds when they are null.<br>
   * 2. User-defined action consumer loop: which for each
   * {@link Tuple} calls
   * {@link Tuple#interact()}.<br>
   *
   * @see Agent#feed()
   * @see Agent#pollFeed()
   * @see Agent#handleFeed()
   */
  public void handle() {
    // 1. Agents
    for (Agent agent : agents()) {
      agent.poll(agent.pollFeed() != null ? agent.pollFeed() : agent.feed());
      agent.handle(agent.handleFeed() != null ? agent.handleFeed() : agent.feed());
    }
    // 2. Low level events
    while (!_tupleQueue.isEmpty())
      _tupleQueue.remove().interact();
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
  public void shiftDefaultGrabber(Grabber grabber1, Grabber grabber2) {
    for (Agent agent : agents())
      agent.shiftDefaultGrabber(grabber1, grabber2);
  }

  /**
   * Returns {@code true} if {@link Agent#isInputGrabber(Grabber)} is
   * {@code true} for at least one agent in {@link #agents()}.
   */
  public boolean isInputGrabber(Grabber grabber) {
    for (Agent agent : agents())
      if (agent.isInputGrabber(grabber))
        return true;
    return false;
  }

  /**
   * Returns {@code true} if {@link Agent#hasGrabber(Grabber)} is
   * {@code true} for at least one agent in {@link #agents()}.
   */
  public boolean hasGrabber(Grabber grabber) {
    for (Agent agent : agents())
      if (agent.hasGrabber(grabber))
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
    return _agents;
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
    _agents.clear();
  }

  /**
   * Returns the event tuple queue. Rarely needed.
   */
  public LinkedList<Tuple> tupleQueue() {
    return _tupleQueue;
  }

  /**
   * Enqueues the eventTuple for later execution which happens at the end of
   * {@link #handle()}. Returns {@code true} if succeeded and {@code false} otherwise.
   *
   * @see #handle()
   */
  public boolean enqueueTuple(Tuple tuple) {
    if (!_tupleQueue.contains(tuple))
      return _tupleQueue.add(tuple);
    return false;
  }

  /**
   * Removes the given event from the event queue. No action is executed.
   *
   * @param event to be removed.
   */
  public void removeTuple(Event event) {
    _tupleQueue.remove(event);
  }

  /**
   * Clears the event queue. Nothing is executed.
   */
  public void removeTuples() {
    _tupleQueue.clear();
  }
}
