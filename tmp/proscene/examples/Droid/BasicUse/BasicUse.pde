/**
 * Basic Use Android.
 * by Victor Forero and Jean Pierre Charalambos.
 * 
 * This example sets up a basic Android scene with some box shape iFrames.
 * 
 * This example requires the Processing Android Mode and an Android device.
 *
 * Currently the following gestures are supported:
 * 
 * 1. TAP_ID: tap (currently only one single tap is supported)
 * 2. DRAG_ONE_ID: drag with one finger
 * 3. DRAG_TWO_ID: drag with two fingers
 * 4. DRAG_THREE_ID: drag with three fingers
 * 5. TURN_TWO_ID: turn with two fingers
 * 6. TURN_THREE_ID: turn with three fingers
 * 7. PINCH_TWO_ID: zoom with two fingers
 * 8. PINCH_THREE_ID: zoom with three fingers
 * 
 * Better than describe them is to try them. Please refer to the setCustomBindings()
 * method defined below to learn how to customize them.
 *
 * To print frame bindings call frame.info()
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;

import android.view.MotionEvent;

Scene scene;
float x,y,z;
Box [] boxes;

// set this flag to true to set the custom bindings defined by setCustomBindings
boolean customBindings = false;

void setup() {
  //size(displayWidth, displayHeight, P3D);
  fullScreen(P3D, 1);
  boxes = new Box[10];
  scene = new Scene(this);
  for (int i = 0; i < boxes.length; i++)
    boxes[i] = new Box(scene);
  frameRate(100);
  if(customBindings)
    setCustomBindings(scene.eyeFrame());
  else
    println(scene.eyeFrame().info());
}

void draw() {
  background(0);
  lights();
  println(Scene.platform());
  scene.beginScreenDrawing();  
  text(frameRate, 5, 17);
  scene.endScreenDrawing();
  for (int i = 0; i < boxes.length; i++)      
    boxes[i].draw(); 
}

void setCustomBindings(InteractiveFrame frame) {
  frame.removeBindings();
  frame.setMotionBinding(DroidTouchAgent.PINCH_THREE_ID, scene.is3D() ? frame.isEyeFrame() ? "translateZ" : "scale" : "scale");
  frame.setMotionBinding(DroidTouchAgent.DRAG_THREE_ID, "translate");
  frame.setMotionBinding(DroidTouchAgent.TURN_THREE_ID, "rotateX");
  frame.setMotionBinding(DroidTouchAgent.DRAG_TWO_ID, "rotateY");
  frame.setMotionBinding(DroidTouchAgent.TURN_TWO_ID, "rotateZ");
  frame.setClickBinding(DroidTouchAgent.TAP_ID, 1, "align");
}

// Processing currently doesn't support registering Android MotionEvent. 
// This method thus needs to be declared.
public boolean surfaceTouchEvent(MotionEvent event) {
  scene.droidTouchAgent().touchEvent(event);
  return true;
}