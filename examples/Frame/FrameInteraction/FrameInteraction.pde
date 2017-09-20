/**
 * Frame Interaction.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to deal with interactive frames: how to pick &
 * manipulate them and how to visually represent them.
 * 
 * Interactivity may be fine-tuned either from an InteractiveFrame instance (frame2) or
 * from some code within the sketch (frame3 and frame4). Note that frame1 has default
 * mouse and keyboard interactivity. Also note that the scene eye has a frame instance
 * (scene.eyeFrame()) which may be controlled in the same way.
 * 
 * Visual representations may be related to a frame in two different ways: 1. Applying
 * the frame transformation just before the graphics code happens in draw() (frame1);
 * or, 2. Setting a visual representation to the frame, either by calling
 * frame.setShape(myPShape) or frame.setShape(myProcedure) in setup() (frame2 and frame3,
 * resp.), and then calling scene.drawFrames() in draw() (frame2, frame3 and frame4).
 * Note that in frame4 different visual representations for the front and picking shapes
 * are set with setFrontShape() and setPickingShape() resp. Also note that setShape() is
 * just a wrapper method that call both functions on the same shape parameter.
 * 
 * Frame picking is achieved by tracking the pointer and checking whether or not it
 * lies within the frame 'selection area': a square around the frame's projected origin
 * (frame 1 and frame3) or the projected frame visual representation (frame2 and frame4)
 * which requires drawing the frame picking-shape into an scene.pickingBuffer().
 * 
 * Press 'f' to display the interactive frame picking hint.
 * Press 'h' to display the global shortcuts in the console.
 * Press 'H' to display the current camera profile keyboard shortcuts
 * and mouse bindings in the console.
 */

import remixlab.bias.event.*;
import remixlab.proscene.*;

Scene scene;
InteractiveFrame frame1, frame2, frame3, frame4;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(640, 360, renderer);    
  scene = new Scene(this);
  scene.eyeFrame().setDamping(0);
  scene.setPickingVisualHint(true);

  //frame 1
  frame1 = new InteractiveFrame(scene);
  frame1.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  frame1.setGrabsInputThreshold(scene.radius()/4);
  frame1.translate(50, 50);

  // frame 2
  // Thanks to the Processing Foundation for providing the rocket shape
  frame2 = new InteractiveFrame(scene, loadShape("rocket.obj"));
  frame2.scale(0.2);
  frame2.setMotionBinding(LEFT, "translate");
  frame2.setMotionBinding(RIGHT, "scale");
  frame2.setKeyBinding('u', "translateXPos");
  frame2.setKeyBinding(UP, "translateZPos");
  frame2.setKeyBinding(Event.CTRL, UP, "translateZNeg");

  //frame 3
  frame3 = new InteractiveFrame(scene, "drawAxes");
  //same as:
  //frame3 = new InteractiveFrame(scene);
  //frame3.setShape("drawAxes");
  //frame3.setPickingPrecision(InteractiveFrame.PickingPrecision.FIXED);
  frame3.translate(-100, -50);
  frame3.setMotionBinding(LEFT, "boxCustomMotion");
  frame3.setClickBinding(LEFT, 1, "boxCustomClick");
  // note that the following: 
  //frame3.setMotionBinding(this, LEFT, "boxCustomMotion");
  //frame3.setClickBinding(this, LEFT, 1, "boxCustomClick");
  // also works. The first parameter points to the class where your code is implemented.
  // You will always need it when your code is declared within a class different than the PApplet.
  frame3.setKeyBinding(LEFT, "rotateYPos");
  frame3.setKeyBinding((Event.SHIFT | Event.CTRL), LEFT, "rotateXNeg");
  
  //frame 4
  //frame4 will behave as frame3 since the latter is passed as its
  //referenceFrame() in its constructor 
  frame4 = new InteractiveFrame(scene, frame3);
  frame4.setFrontShape("boxDrawing");
  frame4.setPickingShape("boxPicking");
  // note that the following:
  //frame4.setFrontShape(this, "boxDrawing");
  //frame4.setPickingShape(this, "boxPicking");
  // also works. The first parameter points to the class where your code is implemented.
  // You will always need it when your code is declared within a class different than the PApplet.
  frame4.setHighlightingMode(InteractiveFrame.HighlightingMode.FRONT_PICKING_SHAPES);
  frame4.translate(0, 100);
  // if two frames have the same key binding (frame2 has also the 'u' binding)
  // the one that is the default grabber takes higher precedence
  frame4.setKeyBinding('u', "translateXPos");
  frame4.setKeyBinding(Event.SHIFT, 'u', "translateXNeg");
}

void boxDrawing(PGraphics pg) {
  pg.fill(0,255,0);
  pg.strokeWeight(3);
  pg.box(30);
}

void boxPicking(PGraphics pg) {
  pg.noStroke();
  pg.fill(255,0,0,126);
  pg.sphere(30);
}

void boxCustomMotion(InteractiveFrame frame, MotionEvent event) {
  frame.screenRotate(event);
}

void boxCustomClick(InteractiveFrame frame) {
  if(frame.scene().mouseAgent().pickingMode() == MouseAgent.PickingMode.MOVE)
    frame.center();
}

void draw() {
  background(0);
  
  // 1. Draw frames for which visual representations have been set
  // this methods returns at the world coordinate system and hence
  // there's no need to push/pop the modelview matrix to render the render frame1
  //pushMatrix();
  scene.drawFrames();
  //popMatrix();
  
  // 2. Draw the remaining frame
  // Multiply matrix to get in the frame coordinate system.
  //applyMatrix(Scene.toPMatrix(frame1.matrix())); //is possible but inefficient
  frame1.applyTransformation();//very efficient
  // Draw the axes
  scene.drawAxes(20);
  if (frame1.grabsInput())
    fill(255, 0, 0);
  else 
    fill(0, 255, 255);
  scene.drawTorusSolenoid();
}

void keyPressed() {
  if(key == ' ')
    if( scene.mouseAgent().pickingMode() == MouseAgent.PickingMode.CLICK ) {
      scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.MOVE);
      scene.eyeFrame().setMotionBinding(LEFT, "rotate");
      scene.eyeFrame().removeMotionBinding(MouseAgent.NO_BUTTON);
    }
    else {
      scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.CLICK);
      scene.eyeFrame().setMotionBinding(MouseAgent.NO_BUTTON, "rotate");
      scene.eyeFrame().removeMotionBinding(LEFT);
    }
  // set the default grabber at both the scene.motionAgent() and the scene.keyAgent()
  if(key == 'v') {
    scene.inputHandler().setDefaultGrabber(frame1);
    println(frame1.info());
  }
  if(key == 'x') {
    scene.inputHandler().setDefaultGrabber(frame2);
    println(frame2.info());
  }
  if(key == 'y') {
    scene.inputHandler().setDefaultGrabber(frame3);
    println(frame3.info());
  }
  if(key == 'z') {
    scene.inputHandler().setDefaultGrabber(frame4);
    println(frame4.info());
  }
  if(key == 'w') {
    scene.inputHandler().setDefaultGrabber(scene.eyeFrame());
    println(scene.eyeFrame().info());
  }
}
