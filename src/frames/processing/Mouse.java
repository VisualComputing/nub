/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights refserved. Library that eases the creation of interactive
 * handling to a raster or ray-tracing renderer. Released under the terms of the GNU
 * Public License v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.processing;

import frames.core.Graph;
import frames.input.Agent;
import frames.input.Event;
import frames.input.event.MotionEvent1;
import frames.input.event.MotionEvent2;
import frames.input.event.TapEvent;
import frames.primitives.Point;
import processing.awt.PGraphicsJava2D;
import processing.core.PApplet;

/**
 * Mouse agent. A Processing fully fledged mouse {@link Agent}.
 *
 * @see Agent
 */
public class Mouse extends Agent {
  protected Point _upperLeftCorner;
  protected Graph _graph;
  protected MotionEvent2 _currentEvent, _previousEvent;
  protected boolean _move, _press, _drag, _release;
  protected Mode _mode;

  public enum Mode {
    MOVE, CLICK
  }

  /**
   * Same as {@code this(graph, new Point())}.
   *
   * @see #Mouse(Graph, Point)
   */
  public Mouse(Graph graph) {
    this(graph, new Point());
  }

  /**
   * Calls super on (graph.inputHandler()) and sets {@link #mode()} to {@link Mode#MOVE}.
   *
   * @see #setMode(Mode)
   */
  public Mouse(Graph graph, Point upperLeftCorner) {
    super(graph.inputHandler());
    _graph = graph;
    _upperLeftCorner = upperLeftCorner;
    setMode(Mode.MOVE);
  }

  /**
   * Sets the mouse {@link #mode()}. Either {@link Mode#MOVE} or {@link Mode#CLICK}.
   *
   * @see #mode()
   */
  public void setMode(Mode mode) {
    _mode = mode;
  }

  /**
   * Returns the mouse {@link #mode()}. Either {@link Mode#MOVE} or {@link Mode#CLICK}.
   *
   * @see #setMode(Mode)
   */
  public Mode mode() {
    return _mode;
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
          _modifiers(mouseEvent), _move ? Event.NO_ID : mouseEvent.getButton());
      if (_move && (mode() == Mode.MOVE))
        poll(_currentEvent);
      handle(_press ? _currentEvent.fire() : _release ? _currentEvent.flush() : _currentEvent);
      _previousEvent = _currentEvent.get();
      return;
    }
    if (mouseEvent.getAction() == processing.event.MouseEvent.WHEEL) {
      handle(new MotionEvent1(mouseEvent.getCount(), mouseEvent.getModifiers(), processing.event.MouseEvent.WHEEL));
      return;
    }
    if (mouseEvent.getAction() == processing.event.MouseEvent.CLICK) {
      TapEvent tapEvent = new TapEvent(mouseEvent.getX() - _upperLeftCorner.x(), mouseEvent.getY() - _upperLeftCorner.y(),
          _modifiers(mouseEvent), mouseEvent.getButton(), mouseEvent.getCount());
      if (mode() == Mode.CLICK)
        poll(tapEvent);
      handle(tapEvent);
      return;
    }
  }

  /**
   * PGraphicsJava2D mouse event modifiers fix.
   * <p>
   * See: https://github.com/processing/processing/issues/1693
   */
  protected int _modifiers(processing.event.MouseEvent event) {
    int modifiers = event.getModifiers();
    if (_graph instanceof Scene)
      if (((Scene) _graph).frontBuffer() instanceof PGraphicsJava2D)
        if (event.getButton() == PApplet.CENTER)
          modifiers = Event.ALT ^ event.getModifiers();
        else if (event.getButton() == PApplet.RIGHT)
          modifiers = Event.META ^ event.getModifiers();
    return modifiers;
  }
}