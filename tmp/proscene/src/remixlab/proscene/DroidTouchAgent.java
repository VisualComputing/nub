/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Victor Manuel Forero, Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.proscene;

import processing.core.PApplet;
import remixlab.bias.Agent;
import remixlab.bias.event.*;
import remixlab.proscene.TouchProcessor.Gestures;

/**
 * Proscene Android touch-agent. A Processing fully fledged touch
 * {@link Agent}.
 *
 * @see Agent
 * @see remixlab.proscene.KeyAgent
 * @see remixlab.proscene.DroidKeyAgent
 * @see remixlab.proscene.MouseAgent
 */
public class DroidTouchAgent extends Agent {
  Scene scene;
  protected MotionEvent newevent, oldevent;
  protected TouchProcessor touchProcessor;
  public static final int TAP_ID = ClickShortcut.registerID("TAP"), DRAG_ONE_ID = MotionShortcut
      .registerID(2, "DRAG_ONE"), DRAG_TWO_ID = MotionShortcut.registerID(2, "DRAG_TWO"), DRAG_THREE_ID = MotionShortcut
      .registerID(2, "DRAG_THREE"), OPPOSABLE_THREE_ID = MotionShortcut
      .registerID(2, "OPPOSABLE_THREE"), TURN_TWO_ID = MotionShortcut
      .registerID(1, "TURN_TWO"), TURN_THREE_ID = MotionShortcut
      .registerID(1, "TURN_THREE"), PINCH_TWO_ID = MotionShortcut
      .registerID(1, "PINCH_TWO"), PINCH_THREE_ID = MotionShortcut.registerID(1, "PINCH_THREE");
  // TODO: debug
  private boolean debug;

  public DroidTouchAgent(Scene scn) {
    super(scn.inputHandler());
    scene = scn;
    touchProcessor = new TouchProcessor();
  }

  protected void setDefaultBindings(InteractiveFrame frame) {
    frame.removeMotionBindings();
    frame.removeClickBindings();

    frame.setMotionBinding(DRAG_ONE_ID, "rotate");
    // TODO TURN_TWO_ID currently implemented as it were a DOF1Event
    // frame.setMotionBinding(TURN_TWO_ID, frame.isEyeFrame() ? "zoomOnRegion" :
    // "screenRotate");
    frame.setMotionBinding(DRAG_TWO_ID, "translate");
    frame.setMotionBinding(PINCH_TWO_ID, scene().is3D() ? frame.isEyeFrame() ? "translateZ" : "scale" : "scale");
    // TODO touch processor double tap seems to be broken as it never gets identified.
    frame.setClickBinding(TAP_ID, 1, "center");
  }

  /**
   * Returns the scene this object belongs to.
   */
  public Scene scene() {
    return scene;
  }

  public void touchEvent(android.view.MotionEvent e) {
    // TODO debug
    if (e == null) {
      System.out.println("Warning: android MotionEvent is null");
      return;
    }
    int action = e.getAction();
    int code = action & android.view.MotionEvent.ACTION_MASK;
    int index = action >> android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT;
    float x = e.getX(index);
    float y = e.getY(index);
    int id = e.getPointerId(index);
    Gestures gesture;
    if (debug) {
      PApplet.println("touch");
      PApplet.print(x + " " + y + " " + id);
    }
    // pass the events to the TouchProcessor
    if (code == android.view.MotionEvent.ACTION_DOWN || code == android.view.MotionEvent.ACTION_POINTER_DOWN) {
      if (debug)
        PApplet.print("down");
      touchProcessor.pointDown(x, y, id);
      touchProcessor.parse();
      newevent = new DOF2Event(oldevent, touchProcessor.getCx(), touchProcessor.getCy(), MotionEvent.NO_MODIFIER_MASK,
          MotionEvent.NO_ID);
      if (e.getPointerCount() == 1)
        updateTrackedGrabber(newevent);
      oldevent = newevent.get();
    } else if (code == android.view.MotionEvent.ACTION_UP || code == android.view.MotionEvent.ACTION_POINTER_UP) {
      if (debug)
        PApplet.print("up");
      touchProcessor.pointUp(id);
      if (e.getPointerCount() == 1) {
        gesture = touchProcessor.parseTap();
        if (gesture == Gestures.TAP_ID) {
          if (debug)
            PApplet.print("tap " + touchProcessor.getTapType());
          handle(new ClickEvent(e.getX() - scene.originCorner().x(), e.getY() - scene.originCorner().y(), gesture.id(),
              touchProcessor.getTapType()));
        }
        resetTrackedGrabber();
      }
    } else if (code == android.view.MotionEvent.ACTION_MOVE) {
      if (debug)
        PApplet.print("move");
      int numPointers = e.getPointerCount();
      for (int i = 0; i < numPointers; i++) {
        id = e.getPointerId(i);
        x = e.getX(i);
        y = e.getY(i);
        touchProcessor.pointMoved(x, y, id);
      }
      gesture = touchProcessor.parseGesture();
      if (gesture != null) {
        if (debug)
          PApplet.print("Gesture " + gesture + ", id: " + gesture.id());
        switch (gesture) {
          case DRAG_ONE_ID:
          case DRAG_TWO_ID:
          case DRAG_THREE_ID:// Drag
            newevent = new DOF2Event(oldevent, touchProcessor.getCx() - scene.originCorner().x(),
                touchProcessor.getCy() - scene.originCorner().y(), MotionEvent.NO_MODIFIER_MASK, gesture.id());
            break;
          case OPPOSABLE_THREE_ID:
            newevent = new DOF2Event(oldevent, x - scene.originCorner().x(), y - scene.originCorner().y(),
                MotionEvent.NO_MODIFIER_MASK, gesture.id());
            break;
          case PINCH_TWO_ID:
            // TODO
            // translateZ on the eyeFrame works only the first time the gesture is performed
            // after the second time it behaves weirdly.
            // Maybe it has to do with the handling of null events?
          case PINCH_THREE_ID: // Pinch
            newevent = new DOF1Event(oldevent, touchProcessor.getZ(), MotionEvent.NO_MODIFIER_MASK, gesture.id());
            break;
          case TURN_TWO_ID:
          case TURN_THREE_ID:
            // int turnOrientation = 1;
            // TODO enumerate which actions need turn, as it should be handled at the
            // GenericFrame and not here
            // if (inputGrabber() instanceof InteractiveFrame)
            // turnOrientation = ((InteractiveFrame) inputGrabber()).isEyeFrame() ? -1 : 1;
            newevent = new DOF1Event(oldevent, touchProcessor.getR(), MotionEvent.NO_MODIFIER_MASK, gesture.id());
            break;
          default:
            break;
        }
        handle(newevent);
        oldevent = newevent.get();
      }
    }
  }
}
