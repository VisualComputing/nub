/**
 * Luxo.
 * by Jean Pierre Charalambos.
 * 
 * Android version of the Frame.Luxo example.
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
import remixlab.dandelion.constraint.*;

import android.view.MotionEvent;

Scene scene;
Lamp lamp;

// set this flag to true to set the custom bindings defined by setCustomBindings
boolean customBindings = false;

public void setup() {
  fullScreen(P3D, 1);
  scene = new Scene(this);  
  scene.setRadius(100);
  scene.showAll();
  scene.setGridVisualHint(false);
  lamp = new Lamp(scene);
  scene.setPickingVisualHint(true);
  // If picking is not too slow in your Android, comment to enable it
  scene.disablePickingBuffer();
  
  if(customBindings)
    setCustomBindings(scene.eyeFrame());
  else
    println(scene.eyeFrame().info());
}

public void draw() {
  background(0);
  lights();
  
  //draw the lamp
  scene.drawFrames();

  //draw the ground
  noStroke();
  fill(120, 120, 120);
  float nbPatches = 100;
  normal(0.0f, 0.0f, 1.0f);
  for (int j=0; j<nbPatches; ++j) {
    beginShape(QUAD_STRIP);
    for (int i=0; i<=nbPatches; ++i) {
      vertex((200*(float)i/nbPatches-100), (200*j/nbPatches-100));
      vertex((200*(float)i/nbPatches-100), (200*(float)(j+1)/nbPatches-100));
    }
    endShape();
  }
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