/**************************************************************************************
 * ProScene (version 3.0.0)
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Victor Manuel Forero, Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive scenes
 * in Processing, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

//TouchProcessor and helper classes were adapted from Android Multi-Touch event demo by David Bouchard,
//http://www.deadpixel.ca
//Event classes
///////////////////////////////////////////////////////////////////////////////////

package remixlab.proscene;

import remixlab.dandelion.geom.Vec;
import remixlab.util.Util;

import java.util.ArrayList;
import java.util.Iterator;

class TouchProcessor {
  // heuristic constants
  enum Gestures {
    TAP_ID("Briefly touch surface with fingertip", DroidTouchAgent.TAP_ID), DRAG_ONE_ID(
        "Move fingertip over surface without losing contact", DroidTouchAgent.DRAG_ONE_ID), DRAG_TWO_ID(
        "Move two fingertips over surface without losing contact", DroidTouchAgent.DRAG_TWO_ID), DRAG_THREE_ID(
        "Move three fingertips over surface without losing contact", DroidTouchAgent.DRAG_THREE_ID), TURN_TWO_ID(
        "Touch surface with two fingers and move them in a clockwise or counterclockwise direction",
        DroidTouchAgent.TURN_TWO_ID), TURN_THREE_ID(
        "Touch surface with three fingers and move them in a clockwise or counterclockwise direction",
        DroidTouchAgent.TURN_THREE_ID), PINCH_TWO_ID(
        "Touch surface with two fingers and bring them together or move them appart",
        DroidTouchAgent.PINCH_TWO_ID), PINCH_THREE_ID(
        "Touch surface with three fingers and bring them together or move them appart",
        DroidTouchAgent.PINCH_THREE_ID), OPPOSABLE_THREE_ID(
        "Press surface with two fingers and move third finger over surface without losing contact",
        DroidTouchAgent.OPPOSABLE_THREE_ID);

    String description;
    int id;

    Gestures(String description, int id) {
      this.description = description;
      this.id = id;
    }

    /**
     * Returns a description of the gesture.
     */
    String description() {
      return description;
    }

    /**
     * Returns gesture id.
     */
    int id() {
      return id;
    }
  }

  final long TAP_INTERVAL = 200;
  final long TAP_TIMEOUT = 0;
  final int DOUBLE_TAP_DIST_THRESHOLD = 30;
  final int FLICK_VELOCITY_THRESHOLD = 20;
  final float MAX_MULTI_DRAG_DISTANCE = 400; // from the centroid

  final float TURN_THRESHOLD = 0.01f;
  final float PINCH_THRESHOLD = 1.2f;

  // A list of currently active touch points
  ArrayList<TouchPoint> touchPoints;

  // Used for tap/doubletaps
  TouchPoint firstTap;
  TouchPoint secondTap;
  long tap;
  int tapCount = 0;

  // Events to be broadcast to the sketch
  ArrayList<TouchEvent> events;

  // centroid information
  float cx;
  float cy;
  float old_cx, old_cy;
  float r;
  float z = 1;

  // tap info
  int tapType;

  boolean pointsChanged = false;

  // -------------------------------------------------------------------------------------
  TouchProcessor() {
    touchPoints = new ArrayList<TouchPoint>();
    events = new ArrayList<TouchEvent>();
  }

  // -------------------------------------------------------------------------------------
  // Point Update functions
  synchronized void pointDown(float x, float y, int id) {
    TouchPoint p = new TouchPoint(x, y, id);
    touchPoints.add(p);
    setZ(1);
    setR(0);
    updateCentroid();
    if (touchPoints.size() >= 2) {
      p.initGestureData(getCx(), getCy());
      if (touchPoints.size() == 2) {
        // if this is the second point, we now have a valid centroid to update the first
        // point
        TouchPoint frst = touchPoints.get(0);
        frst.initGestureData(getCx(), getCy());
      }
    }

    // tap detection
    if (tapCount == 0) {
      firstTap = p;
    }
    if (tapCount == 1) {
      secondTap = p;
    }
    tap = System.currentTimeMillis();
    pointsChanged = true;
  }

  // -------------------------------------------------------------------------------------
  synchronized void pointUp(int id) {
    TouchPoint p = getPoint(id);
    touchPoints.remove(p);

    // tap detection
    // TODO: handle a long press event here?
    if (p == firstTap || p == secondTap) {
      // this could be either a Tap or a Flick gesture, based on movement
      float d = Util.distance(p.x, p.y, p.px, p.py);
      if (d > FLICK_VELOCITY_THRESHOLD) {
        FlickEvent event = new FlickEvent(p.px, p.py, new Vec(p.x - p.px, p.y - p.py));
        events.add(event);
      } else {
        long interval = System.currentTimeMillis() - tap;

        if (interval < TAP_INTERVAL) {
          tapCount++;
        }
      }
    }
    pointsChanged = true;
  }

  // -------------------------------------------------------------------------------------
  synchronized void pointMoved(float x, float y, int id) {
    TouchPoint p = getPoint(id);
    p.update(x, y);
    // since the events will be in sync with draw(), we just wait until analyse() to
    // look for gestures
    pointsChanged = true;
  }

  // -------------------------------------------------------------------------------------
  // Calculate the centroid of all active points
  void updateCentroid() {
    old_cx = getCx();
    old_cy = getCy();
    setCx(0);
    setCy(0);
    for (int i = 0; i < touchPoints.size(); i++) {
      TouchPoint p = touchPoints.get(i);
      setCx(getCx() + p.x);
      setCy(getCy() + p.y);
    }
    setCx(getCx() / touchPoints.size());
    setCy(getCy() / touchPoints.size());
  }

  // -------------------------------------------------------------------------------------

  synchronized void parse() {
    // simple event priority rule: do not try to rotate or pinch while dragging
    // this gets rid of a lot of jittery events
    if (pointsChanged) {
      updateCentroid();
      pointsChanged = false;
    }
  }

  // TODO testing: original was this method, without the tapType property
  // synchronized Gestures parseTap() {
  // if (handleTaps() == null)
  // return null;
  // else
  // return Gestures.TAP_ID;
  // }

  synchronized Gestures parseTap() {
    TapEvent event = handleTaps();
    if (event == null)
      return null;
    this.setTapType(event.type + 1);
    return Gestures.TAP_ID;
  }

  synchronized Gestures parseGesture() {
    Gestures gesture = null;
    if (pointsChanged) {
      updateCentroid();
      DragEvent dragEvent = handleDrag();
      PinchEvent pinchEvent = handlePinch();
      TurnEvent TurnEvent = handleTurn();
      if (TurnEvent != null)
        // TODO experimental, previous value was TurnEvent.angle * -500
        // but it ended up being too sensible for my moto g4 touch screen
        setR(getR() + (TurnEvent.angle * -100));
      if (pinchEvent != null)
        setZ(getZ() + pinchEvent.amount);
      if (dragEvent != null) {
        if (dragEvent.numberOfPoints == 1)
          gesture = Gestures.DRAG_ONE_ID;
        else if (dragEvent.numberOfPoints == 2)
          gesture = Gestures.DRAG_TWO_ID;
        else if (dragEvent.numberOfPoints == 3)
          gesture = Gestures.DRAG_THREE_ID;
      } else if (pinchEvent != null) {
        if (pinchEvent.numberOfPoints == 2)
          gesture = Gestures.PINCH_TWO_ID;
        else if (pinchEvent.numberOfPoints == 3)
          gesture = Gestures.PINCH_THREE_ID;
      } else if (TurnEvent != null) {
        if (TurnEvent.numberOfPoints == 2)
          gesture = Gestures.TURN_TWO_ID;
        else if (TurnEvent.numberOfPoints == 3)
          gesture = Gestures.TURN_THREE_ID;
      } else {
        if (touchPoints.size() == 3)
          gesture = Gestures.OPPOSABLE_THREE_ID;
      }
      pointsChanged = false;
    }
    return gesture;
  }

  // -------------------------------------------------------------------------------------
  TapEvent handleTaps() {
    TapEvent tapEvent = null;
    if (tapCount == 2) {
      // check if the tap point has moved
      float d = Util.distance(firstTap.x, firstTap.y, secondTap.x, secondTap.y);
      if (d > DOUBLE_TAP_DIST_THRESHOLD) {
        // if the two taps are apart, count them as two single taps
        // TapEvent event1 = new TapEvent(firstTap.x, firstTap.y, TapEvent.SINGLE);
        // onTap(event1);
        // TapEvent event2 = new TapEvent(secondTap.x, secondTap.y, TapEvent.SINGLE);
        // onTap(event2);
        tapEvent = new TapEvent(firstTap.x, firstTap.y, TapEvent.SINGLE);
        events.add(tapEvent);
      } else {
        tapEvent = new TapEvent(firstTap.x, firstTap.y, TapEvent.DOUBLE);
        events.add(tapEvent);
      }
      tapCount = 0;
    } else if (tapCount == 1) {
      long interval = System.currentTimeMillis() - tap;
      if (interval > TAP_TIMEOUT) {
        tapEvent = new TapEvent(firstTap.x, firstTap.y, TapEvent.SINGLE);
        events.add(tapEvent);
        tapCount = 0;
      }
    }
    return tapEvent;
  }

  // -------------------------------------------------------------------------------------
  // turn is the average angle change between each point and the centroid
  TurnEvent handleTurn() {
    TurnEvent TurnEvent = null;
    if (touchPoints.size() >= 2) {
      // look for turn events
      float turn = 0;
      for (int i = 0; i < touchPoints.size(); i++) {
        TouchPoint p = touchPoints.get(i);
        float angle = (float) Math.atan2(p.y - getCy(), p.x - getCx());
        p.setAngle(angle);
        float delta = p.angle - p.oldAngle;
        if (delta > Math.PI)
          delta -= 2 * Math.PI;
        if (delta < -Math.PI)
          delta += 2 * Math.PI;
        turn += delta;
      }
      turn /= touchPoints.size();
      if (Math.abs(turn) > TURN_THRESHOLD) {
        TurnEvent = new TurnEvent(getCx(), getCy(), turn, touchPoints.size());
        events.add(TurnEvent);
      }
    }
    return TurnEvent;
  }

  // -------------------------------------------------------------------------------------
  // pinch is simply the average distance change from each points to the centroid
  PinchEvent handlePinch() {
    PinchEvent pinchEvent = null;
    if (touchPoints.size() >= 2) {
      // look for pinch events
      float pinch = 0;
      for (int i = 0; i < touchPoints.size(); i++) {
        TouchPoint p = touchPoints.get(i);
        float distance = Util.distance(p.x, p.y, getCx(), getCy());
        p.setPinch(distance);
        float delta = p.pinch - p.oldPinch;
        pinch += delta;
      }
      pinch /= touchPoints.size();
      if (Math.abs(pinch) > PINCH_THRESHOLD) {
        pinchEvent = new PinchEvent(getCx(), getCy(), pinch, touchPoints.size());
        events.add(pinchEvent);
      }
    }
    return pinchEvent;
  }

  // -------------------------------------------------------------------------------------
  DragEvent handleDrag() {
    // look for multi-finger drag events
    // multi-drag is defined as all the fingers moving close-ish together in the same
    // direction
    boolean x_drag = true;
    boolean y_drag = true;
    boolean clustered = false;
    int first_x_dir = 0;
    int first_y_dir = 0;
    DragEvent dragEvent = null;

    for (int i = 0; i < touchPoints.size(); i++) {
      TouchPoint p = touchPoints.get(i);
      int x_dir = 0;
      int y_dir = 0;
      if (p.dx() > 0)
        x_dir = 1;
      if (p.dx() < 0)
        x_dir = -1;
      if (p.dy() > 0)
        y_dir = 1;
      if (p.dy() < 0)
        y_dir = -1;

      if (i == 0) {
        first_x_dir = x_dir;
        first_y_dir = y_dir;
      } else {
        if (first_x_dir != x_dir)
          x_drag = false;
        if (first_y_dir != y_dir)
          y_drag = false;
      }

      // if the point is stationary
      if (x_dir == 0)
        x_drag = false;
      if (y_dir == 0)
        y_drag = false;

      if (touchPoints.size() == 1)
        clustered = true;
      else {
        float distance = Util.distance(p.x, p.y, getCx(), getCy());
        if (distance < MAX_MULTI_DRAG_DISTANCE) {
          clustered = true;
        }
      }
    }

    if ((x_drag || y_drag) && clustered) {
      if (touchPoints.size() == 1) {
        TouchPoint p = touchPoints.get(0);
        // use the centroid to calculate the position and delta of this drag event
        dragEvent = new DragEvent(p.x, p.y, p.dx(), p.dy(), 1);
        events.add(dragEvent);
      } else {
        // use the centroid to calculate the position and delta of this drag event
        dragEvent = new DragEvent(getCx(), getCy(), getCx() - old_cx, getCy() - old_cy, touchPoints.size());
        events.add(dragEvent);
      }
    }
    return dragEvent;
  }

  // -------------------------------------------------------------------------------------
  // TODO implement me in a cleaner way
  @SuppressWarnings("unchecked")
  synchronized ArrayList<TouchPoint> getPoints() {
    return (ArrayList<TouchPoint>) touchPoints.clone();
  }

  // -------------------------------------------------------------------------------------
  synchronized TouchPoint getPoint(int pid) {
    Iterator<TouchPoint> i = touchPoints.iterator();
    while (i.hasNext()) {
      TouchPoint tp = i.next();
      if (tp.id == pid)
        return tp;
    }
    return null;
  }

  float getCx() {
    return cx;
  }

  void setCx(float cx) {
    this.cx = cx;
  }

  float getCy() {
    return cy;
  }

  void setCy(float cy) {
    this.cy = cy;
  }

  float getZ() {
    return z;
  }

  void setZ(float z) {
    this.z = z;
  }

  float getR() {
    return r;
  }

  void setR(float r) {
    this.r = r;
  }

  void setTapType(int t) {
    this.tapType = t;
  }

  int getTapType() {
    return tapType;
  }
}

class TouchEvent {
  // empty base class to make event handling easier
}

// /////////////////////////////////////////////////////////////////////////////////
class DragEvent extends TouchEvent {
  float x; // position
  float y;
  float dx; // movement
  float dy;
  int numberOfPoints;

  DragEvent(float x, float y, float dx, float dy, int n) {
    this.x = x;
    this.y = y;
    this.dx = dx;
    this.dy = dy;
    numberOfPoints = n;
  }
}

// /////////////////////////////////////////////////////////////////////////////////
class PinchEvent extends TouchEvent {
  float centerX;
  float centerY;
  float amount; // in pixels
  int numberOfPoints;

  PinchEvent(float centerX, float centerY, float amount, int n) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.amount = amount;
    this.numberOfPoints = n;
  }
}

// /////////////////////////////////////////////////////////////////////////////////
class TurnEvent extends TouchEvent {
  float centerX;
  float centerY;
  float angle; // delta, in radians
  int numberOfPoints;

  TurnEvent(float centerX, float centerY, float angle, int n) {
    this.centerX = centerX;
    this.centerY = centerY;
    this.angle = angle;
    this.numberOfPoints = n;
  }
}

// /////////////////////////////////////////////////////////////////////////////////
class TapEvent extends TouchEvent {
  static final int SINGLE = 0;
  static final int DOUBLE = 1;

  float x;
  float y;
  int type;

  TapEvent(float x, float y, int type) {
    this.x = x;
    this.y = y;
    this.type = type;
  }

  boolean isSingleTap() {
    return (type == SINGLE) ? true : false;
  }

  boolean isDoubleTap() {
    return (type == DOUBLE) ? true : false;
  }
}

// /////////////////////////////////////////////////////////////////////////////////
class FlickEvent extends TouchEvent {
  float x;
  float y;
  Vec velocity;

  FlickEvent(float x, float y, Vec velocity) {
    this.x = x;
    this.y = y;
    this.velocity = velocity;
  }
}

class TouchPoint {
  float x;
  float y;
  float px;
  float py;
  int id;

  // used for gesture detection
  float angle;
  float oldAngle;
  float pinch;
  float oldPinch;

  // -------------------------------------------------------------------------------------
  TouchPoint(float x, float y, int id) {
    this.x = x;
    this.y = y;
    this.px = x;
    this.py = y;
    this.id = id;
  }

  // -------------------------------------------------------------------------------------
  void update(float x, float y) {
    px = this.x;
    py = this.y;
    this.x = x;
    this.y = y;
  }

  // -------------------------------------------------------------------------------------
  void initGestureData(float cx, float cy) {
    pinch = oldPinch = Vec.distance(new Vec(x, y), new Vec(cx, cy));
    angle = oldAngle = (float) Math.atan2((y - cy), (x - cx));
  }

  // -------------------------------------------------------------------------------------
  // delta x -- int to get rid of some noise
  int dx() {
    return (int) (x - px);
  }

  // -------------------------------------------------------------------------------------
  // delta y -- int to get rid of some noise
  int dy() {
    return (int) (y - py);
  }

  // -------------------------------------------------------------------------------------
  void setAngle(float angle) {
    oldAngle = this.angle;
    this.angle = angle;
  }

  // -------------------------------------------------------------------------------------
  void setPinch(float pinch) {
    oldPinch = this.pinch;
    this.pinch = pinch;
  }
}
