/**
 * Mouse Modifiers
 * by Jean Pierre Charalambos.
 *
 * Temporal example (will not be part of the final release).
 *
 * Idea is to test the modifiers key issue found in the JAVA2D renderer
 * (https://github.com/processing/processing/issues/3828). I found the use
 * of modifier keys + the mouse pretty universal within desktop VR apps.
 * I think they thus must be supported in Proscene.
 *
 * Please test if the mouse bindings work in the same way for all renderers
 * (specially JAVA2D which has an "issue":
 * https://github.com/processing/processing/issues/3828) under MacOS and report
 * back at the forum or PM me.
 */

import remixlab.proscene.*;

Scene scene;
InteractiveFrame frame;

//Choose JAVA2D or P2D for a 2D scene, or P3D for a 3D one
String renderer = JAVA2D;

void setup() {
  size(700, 700, renderer);
  scene = new Scene(this);
  scene.setPickingVisualHint(true);
  scene.setGridVisualHint(false);

  //frame
  frame = new InteractiveFrame(scene, "drawTorusSolenoid");
  frame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame.setGrabsInputThreshold(scene.radius() / 4);
  frame.translate(50, 50);

  customBindings1();
}

void customBindings1() {
  scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.MOVE);
  // eyeFrame() and frame
  for (InteractiveFrame iFrame : scene.frames()) {
    iFrame.removeBindings();
    iFrame.setMotionBinding(LEFT, "rotate");
    iFrame.setMotionBinding(RIGHT, "translate");
    iFrame.setMotionBinding(CENTER, "scale");
    iFrame.setMotionBinding((Event.SHIFT | Event.CTRL), RIGHT, "screenTranslate");
    iFrame.setMotionBinding(MouseAgent.WHEEL_ID, "scale");
    iFrame.setClickBinding(LEFT, 1, "center");
    iFrame.setClickBinding(RIGHT, 1, "align");
    println(iFrame == scene.eyeFrame() ? "eyeFrame BINDINGS" : "frame BINDINGS");
    println(iFrame.info());
  }
}

void customBindings2() {
  scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.CLICK);
  // eyeFrame() and frame
  for (InteractiveFrame iFrame : scene.frames()) {
    iFrame.removeBindings();
    iFrame.setMotionBinding(MouseAgent.NO_BUTTON, "rotate");
    iFrame.setMotionBinding(Event.SHIFT, MouseAgent.NO_BUTTON, "translate");
    iFrame.setMotionBinding(Event.CTRL, MouseAgent.NO_BUTTON, "scale");
    iFrame.setMotionBinding((Event.SHIFT | Event.CTRL), MouseAgent.NO_BUTTON, "screenTranslate");
    iFrame.setMotionBinding(Event.ALT, MouseAgent.WHEEL_ID, "scale");
    iFrame.setClickBinding(Event.SHIFT, LEFT, 1, "center");
    iFrame.setClickBinding(Event.CTRL, RIGHT, 1, "align");
    println(iFrame == scene.eyeFrame() ? "eyeFrame BINDINGS" : "frame BINDINGS");
    println(iFrame.info());
  }
}

void draw() {
  background(0);
  scene.drawFrames();
}

void keyPressed() {
  if (key == ' ')
    if (scene.mouseAgent().pickingMode() == MouseAgent.PickingMode.CLICK)
      customBindings1();
    else
      customBindings2();
  // set the default grabber at both the scene.motionAgent() and the scene.keyAgent()
  if (key == 'u')
    scene.inputHandler().setDefaultGrabber(frame);
  if (key == 'v')
    scene.inputHandler().setDefaultGrabber(scene.eyeFrame());
}