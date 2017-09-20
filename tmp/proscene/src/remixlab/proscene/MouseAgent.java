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
import remixlab.bias.BogusEvent;
import remixlab.bias.event.*;

/**
 * Proscene mouse-agent. A Processing fully fledged mouse
 * {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.KeyAgent
 * @see remixlab.proscene.DroidKeyAgent
 * @see remixlab.proscene.DroidTouchAgent
 */
public class MouseAgent extends Agent {
  public static final int LEFT_ID = MotionShortcut.registerID(37, 2, "LEFT"), CENTER_ID = MotionShortcut
      .registerID(3, 2, "CENTER"), RIGHT_ID = MotionShortcut.registerID(39, 2, "RIGHT"), WHEEL_ID = MotionShortcut
      .registerID(8, 1, "WHEEL"), NO_BUTTON = MotionShortcut
      .registerID(BogusEvent.NO_ID, 2, "NO_BUTTON"), LEFT_CLICK_ID = ClickShortcut
      .registerID(LEFT_ID, "LEFT"), RIGHT_CLICK_ID = ClickShortcut
      .registerID(RIGHT_ID, "RIGHT"), CENTER_CLICK_ID = ClickShortcut.registerID(CENTER_ID, "CENTER");
  protected float xSens = 1f;
  protected float ySens = 1f;
  protected Scene scene;
  protected DOF2Event currentEvent, prevEvent;
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
   * Returns the scene this object belongs to.
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
      currentEvent = new DOF2Event(prevEvent, e.getX() - scene.originCorner().x(), e.getY() - scene.originCorner().y(),
          e.getModifiers(), move ? BogusEvent.NO_ID : e.getButton());
      if (move && (pickingMode() == PickingMode.MOVE))
        updateTrackedGrabber(currentEvent);
      handle(press ? currentEvent.fire() : release ? currentEvent.flush() : currentEvent);
      prevEvent = currentEvent.get();
      return;
    }
    if (e.getAction() == processing.event.MouseEvent.WHEEL) {
      handle(new DOF1Event(e.getCount(), e.getModifiers(), WHEEL_ID));
      return;
    }
    if (e.getAction() == processing.event.MouseEvent.CLICK) {
      ClickEvent bogusClickEvent = new ClickEvent(e.getX() - scene.originCorner().x(), e.getY() - scene.originCorner().y(),
          e.getModifiers(), e.getButton(), e.getCount());
      if (pickingMode() == PickingMode.CLICK)
        updateTrackedGrabber(bogusClickEvent);
      handle(bogusClickEvent);
      return;
    }
  }

  @Override
  public float[] sensitivities(MotionEvent event) {
    if (event instanceof DOF2Event)
      return new float[]{xSens, ySens, 1f, 1f, 1f, 1f};
    else
      return super.sensitivities(event);
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
   * Default value is 1. A higher value will make the event more efficient (usually
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
   * Default value is 1. A higher value will make the event more efficient (usually
   * meaning a faster motion). Use a negative value to invert the along y-Axis motion
   * direction.
   */
  public float ySensitivity() {
    return ySens;
  }

  /**
   * Internal use. Other Agents should follow a similar pattern: 1. Register some motion
   * ids within the MotionShortcut; and, 2. Define the default bindings on the frame parameter.
   *
   * @see remixlab.bias.event.MotionShortcut#registerID(int, String)
   * @see remixlab.bias.event.MotionShortcut#registerID(int, int, String)
   */
  protected void setDefaultBindings(InteractiveFrame frame) {
    frame.removeMotionBindings();
    frame.removeClickBindings();

    frame.setMotionBinding(LEFT_ID, "rotate");
    frame.setMotionBinding(CENTER_ID, frame.isEyeFrame() ? "zoomOnRegion" : "screenRotate");
    frame.setMotionBinding(RIGHT_ID, "translate");
    frame.setMotionBinding(WHEEL_ID, scene().is3D() ? frame.isEyeFrame() ? "translateZ" : "scale" : "scale");

    frame.setClickBinding(LEFT_CLICK_ID, 2, "align");
    frame.setClickBinding(RIGHT_CLICK_ID, 2, "center");
  }
}
