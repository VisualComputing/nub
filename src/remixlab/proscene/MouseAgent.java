/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import remixlab.core.Graph;
import remixlab.input.Agent;
import remixlab.input.Event;
import remixlab.input.event.*;
import remixlab.primitives.Point;

/**
 * Proscene mouse-agent. A Processing fully fledged mouse
 * {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.KeyAgent
 */
public class MouseAgent extends Agent {
  protected Point _upperLeftCorner;
  public static int LEFT_ID = 37, CENTER_ID = 3, RIGHT_ID = 39, WHEEL_ID = 8, NO_BUTTON = Event.NO_ID,
          LEFT_CLICK_ID = LEFT_ID, RIGHT_CLICK_ID = RIGHT_ID, CENTER_CLICK_ID = CENTER_ID;
  protected Graph _graph;
  protected MotionEvent2 _currentEvent, _previousEvent;
  protected boolean _move, _press, _drag, _release;
  protected PickingMode _pickingMode;

  public enum PickingMode {
    MOVE, CLICK
  }

  /**
   * Same as {@code this(graph, new Point())}.
   *
   * @see #MouseAgent(Graph, Point)
   */
  public MouseAgent(Graph graph) {
    this(graph, new Point());
  }

  /**
   * Calls super on (graph.inputHandler()) and sets {@link #pickingMode()} to {@link PickingMode#MOVE}.
   *
   * @see #setPickingMode(PickingMode)
   */
  public MouseAgent(Graph graph, Point upperLeftCorner) {
    super(graph.inputHandler());
    _graph = graph;
    _upperLeftCorner = upperLeftCorner;
    setPickingMode(PickingMode.MOVE);
  }

  /**
   * Returns the graph this object belongs to.
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Sets the agent {@link #pickingMode()}. Either {@link PickingMode#MOVE} or
   * {@link PickingMode#CLICK}.
   *
   * @see #pickingMode()
   */
  public void setPickingMode(PickingMode mode) {
    _pickingMode = mode;
  }

  /**
   * Returns the agent {@link #pickingMode()}. Either {@link PickingMode#MOVE} or
   * {@link PickingMode#CLICK}.
   *
   * @see #setPickingMode(PickingMode)
   */
  public PickingMode pickingMode() {
    return _pickingMode;
  }

  /**
   * Processing mouseEvent method to be registered at the PApplet's instance.
   */
  public void mouseEvent(processing.event.MouseEvent mouseEvent) {
    _move = mouseEvent.getAction() == processing.event.MouseEvent.MOVE;
    _press = mouseEvent.getAction() == processing.event.MouseEvent.PRESS;
    _drag = mouseEvent.getAction() == processing.event.MouseEvent.DRAG;
    _release = mouseEvent.getAction() == processing.event.MouseEvent.RELEASE;
    if (_move || _press || _drag || _release) {
      _currentEvent = new MotionEvent2(_previousEvent, mouseEvent.getX() - _upperLeftCorner.x(), mouseEvent.getY() - _upperLeftCorner.y(),
          mouseEvent.getModifiers(), _move ? Event.NO_ID : mouseEvent.getButton());
      if (_move && (pickingMode() == PickingMode.MOVE))
        poll(_currentEvent);
      handle(_press ? _currentEvent.fire() : _release ? _currentEvent.flush() : _currentEvent);
      _previousEvent = _currentEvent.get();
      return;
    }
    if (mouseEvent.getAction() == processing.event.MouseEvent.WHEEL) {
      handle(new MotionEvent1(mouseEvent.getCount(), mouseEvent.getModifiers(), WHEEL_ID));
      return;
    }
    if (mouseEvent.getAction() == processing.event.MouseEvent.CLICK) {
      TapEvent tapEvent = new TapEvent(mouseEvent.getX() - _upperLeftCorner.x(), mouseEvent.getY() - _upperLeftCorner.y(),
          mouseEvent.getModifiers(), mouseEvent.getButton(), mouseEvent.getCount());
      if (pickingMode() == PickingMode.CLICK)
        poll(tapEvent);
      handle(tapEvent);
      return;
    }
  }
}
