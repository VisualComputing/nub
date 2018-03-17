/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.input;

/**
 * A [{@link Event},{@link Grabber}] tuple. An
 * enqueued tuple fires {@link Grabber#interact(Event)}
 * call from the event in the tuple.
 * <p>
 * Tuples are typically enqueued by an agent (see
 * {@link Agent#handle(Event)}), but may be enqueued manually, see
 * {@link InputHandler#enqueueTuple(Tuple)}.
 */
public class Tuple {
  protected Event _event;
  protected Grabber _grabber;

  /**
   * Constructs a {@link Event},
   * {@link Grabber} tuple.
   *
   * @param event   {@link Event}
   * @param grabber {@link Grabber}
   */
  public Tuple(Event event, Grabber grabber) {
    _event = event;
    _grabber = grabber;
  }

  /**
   * Calls {@link Grabber#interact(Event)}.
   *
   * @return true if succeeded and false otherwise.
   */
  public boolean interact() {
    if (_grabber == null || _event == null)
      return false;
    _grabber.interact(_event);
    return true;
  }

  /**
   * Returns the event from the tuple.
   */
  public Event event() {
    return _event;
  }

  /**
   * Returns the object Grabber in the tuple.
   */
  public Grabber grabber() {
    return _grabber;
  }
}
