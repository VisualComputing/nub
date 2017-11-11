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

import remixlab.bias.Agent;
import remixlab.bias.Event;
import remixlab.bias.event.*;

/**
 * Proscene mouse-agent. A Processing fully fledged mouse
 * {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.KeyAgent
 */
public class MouseAgent extends Agent {
  public static int LEFT_ID = 37, CENTER_ID = 3, RIGHT_ID = 39, WHEEL_ID = 8, NO_BUTTON = Event.NO_ID,
          LEFT_CLICK_ID = LEFT_ID, RIGHT_CLICK_ID = RIGHT_ID, CENTER_CLICK_ID = CENTER_ID;
  protected float xSens = 1f;
  protected float ySens = 1f;
  protected Scene scene;
  protected MotionEvent2 currentEvent, prevEvent;
  protected boolean move, press, drag, release;
  protected PickingMode pMode;

  public enum PickingMode {
    MOVE, CLICK
  }

  ;

  /**
   * Calls super on (scn,n) and sets {@link #pickingMode()} to {@link PickingMode#MOVE}.
   *
   * @see #setPickingMode(PickingMode)
   */
  public MouseAgent(Scene scn) {
    super(scn.inputHandler());
    scene = scn;
    setPickingMode(PickingMode.MOVE);
  }

  /**
   * Returns the graph this object belongs to.
   */
  public Scene scene() {
    return scene;
  }

  /**
   * Sets the agent {@link #pickingMode()}. Either {@link PickingMode#MOVE} or
   * {@link PickingMode#CLICK}.
   *
   * @see #pickingMode()
   */
  public void setPickingMode(PickingMode mode) {
    pMode = mode;
  }

  /**
   * Returns the agent {@link #pickingMode()}. Either {@link PickingMode#MOVE} or
   * {@link PickingMode#CLICK}.
   *
   * @see #setPickingMode(PickingMode)
   */
  public PickingMode pickingMode() {
    return pMode;
  }

  /**
   * Processing mouseEvent method to be registered at the PApplet's instance.
   */
  public void mouseEvent(processing.event.MouseEvent e) {
    move = e.getAction() == processing.event.MouseEvent.MOVE;
    press = e.getAction() == processing.event.MouseEvent.PRESS;
    drag = e.getAction() == processing.event.MouseEvent.DRAG;
    release = e.getAction() == processing.event.MouseEvent.RELEASE;
    if (move || press || drag || release) {
      currentEvent = new MotionEvent2(prevEvent, e.getX() - scene.originCorner().x(), e.getY() - scene.originCorner().y(),
          e.getModifiers(), move ? Event.NO_ID : e.getButton());
      if (move && (pickingMode() == PickingMode.MOVE))
        poll(currentEvent);
      handle(press ? currentEvent.fire() : release ? currentEvent.flush() : currentEvent);
      prevEvent = currentEvent.get();
      return;
    }
    if (e.getAction() == processing.event.MouseEvent.WHEEL) {
      handle(new MotionEvent1(e.getCount(), e.getModifiers(), WHEEL_ID));
      return;
    }
    if (e.getAction() == processing.event.MouseEvent.CLICK) {
      TapEvent bogusTapEvent = new TapEvent(e.getX() - scene.originCorner().x(), e.getY() - scene.originCorner().y(),
          e.getModifiers(), e.getButton(), e.getCount());
      if (pickingMode() == PickingMode.CLICK)
        poll(bogusTapEvent);
      handle(bogusTapEvent);
      return;
    }
  }

  /**
   * Defines the {@link #xSensitivity()}.
   */
  public void setXSensitivity(float sensitivity) {
    xSens = sensitivity;
  }

  /**
   * Returns the x sensitivity.
   * <p>
   * Default value is 1. A higher value will make the _event more efficient (usually
   * meaning a faster motion). Use a negative value to invert the along x-Axis motion
   * direction.
   */
  public float xSensitivity() {
    return xSens;
  }

  /**
   * Defines the {@link #ySensitivity()}.
   */
  public void setYSensitivity(float sensitivity) {
    ySens = sensitivity;
  }

  /**
   * Returns the y sensitivity.
   * <p>
   * Default value is 1. A higher value will make the _event more efficient (usually
   * meaning a faster motion). Use a negative value to invert the along y-Axis motion
   * direction.
   */
  public float ySensitivity() {
    return ySens;
  }
}
