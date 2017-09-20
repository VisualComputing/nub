/**
 * Frame Interaction.
 * by Jean Pierre Charalambos.
 * 
 * Android version of the Frame.FrameInteraction example.
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
import android.view.MotionEvent;

Scene scene;
InteractiveFrame frame1, frame2, frame3, frame4;

String renderer = P3D;

// set this flag to true to set the eye custom bindings defined by setCustomBindings
// note that you can use this function with any other frame too
boolean customBindings = false;

void setup() {
  fullScreen(P3D, 1);
  scene = new Scene(this);
  scene.eyeFrame().setDamping(0);
  scene.setPickingVisualHint(true);

  //frame 1
  frame1 = new InteractiveFrame(scene, "drawTorusSolenoid");
  frame1.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame1.setGrabsInputThreshold(scene.radius()/4);
  frame1.translate(50, 50);

  // frame 2
  // Thanks to the Processing Foundation for providing the rocket shape
  frame2 = new InteractiveFrame(scene, loadShape("rocket.obj"));
  frame2.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame2.setGrabsInputThreshold(scene.radius()*4);
  frame2.scale(0.2);
  // comment the previous 4 lines and do it with a cylinder:
  //frame2 = new InteractiveFrame(scene, "cylinder");

  //frame 3
  frame3 = new InteractiveFrame(scene);
  frame3.setFrontShape("drawAxes");
  frame3.setPickingShape(this, "sphere");
  frame3.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame3.setHighlightingMode(InteractiveFrame.HighlightingMode.FRONT_PICKING_SHAPES);
  frame3.translate(-100, -50);
  
  //frame 4
  //frame4 will behave as frame3 since the latter is passed as its
  //referenceFrame() in its constructor 
  frame4 = new InteractiveFrame(scene, frame3);
  frame4.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame4.setShape(this, "box");
  frame4.translate(0, 100);
  
  // If picking is not too slow in your Android, comment to enable it
  scene.disablePickingBuffer();
  
  if(customBindings)
    setCustomBindings(scene.eyeFrame());
  else
    println(scene.eyeFrame().info());
}

void cylinder(PGraphics pg) {
  pg.fill(0,0,255,125);
  scene.drawCylinder(pg);
}

void box(PGraphics pg) {
  pg.fill(0,255,0,125);
  pg.strokeWeight(3);
  pg.box(30);
}

void sphere(PGraphics pg) {
  pg.noStroke();
  pg.fill(0,255,255,125);
  pg.strokeWeight(3);
  pg.sphere(20);
}

void draw() {
  background(0);
  // Set the torus fill color
  fill(255,255,0,125);
  scene.drawFrames();
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