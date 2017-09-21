package frame;

import processing.core.*;
import remixlab.bias.Event;
import remixlab.bias.event.MotionEvent;
import remixlab.dandelion.core.GenericFrame;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;

import java.util.ArrayList;

/**
 * Created by pierre on 11/15/16.
 */
public class FrameInteraction extends PApplet {
  Scene scene;
  InteractiveFrame frame1, frame2, frame3, frame4;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(700, 700, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    scene.eyeFrame().setDamping(0);
    scene.setPickingVisualHint(true);

    Button2D button = new Button2D(scene, new PVector(100, 100), loadFont("FreeSans-16.vlw"), "hello world");
    button.setFont(loadFont("FreeSans-36.vlw"));
    button.setText("PiERRE");

    //frame 1
    frame1 = new InteractiveFrame(scene);
    frame1.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    frame1.setGrabsInputThreshold(scene.radius() / 4);
    frame1.translate(50, 50);

    // frame 2
    // Thanks to the Processing Foundation for providing the rocket shape
    frame2 = new InteractiveFrame(scene, frame1, loadShape("testing/data/frame_interaction/rocket.obj"));
    frame2.scale(0.2f);
    frame2.translate(-100, -100);
    frame2.setMotionBinding((processing.event.Event.SHIFT | processing.event.Event.CTRL), LEFT, "translate");
    frame2.setMotionBinding(RIGHT, "scale");
    frame2.setKeyBinding('u', "translateXPos");
    frame2.setKeyBinding(UP, "translateZPos");
    frame2.setKeyBinding(processing.event.Event.CTRL, UP, "translateZNeg");

    //frame 3
    frame3 = new InteractiveFrame(scene, "drawAxes");
    //same as:
    //frame3 = new InteractiveFrame(scene);
    //frame3.setShape("drawAxes");
    //frame3.setPickingPrecision(InteractiveFrame.PickingPrecision.FIXED);
    //frame3.disableVisualHint();
    frame3.translate(-100, -50);
    frame3.setMotionBinding(LEFT, "boxCustomMotion");
    frame3.setClickBinding(LEFT, 1, "boxCustomClick");
    // note that the following:
    //frame3.setMotionBinding(this, LEFT, "boxCustomMotion");
    //frame3.setClickBinding(this, LEFT, 1, "boxCustomClick");
    // also works. The first parameter points to the class where your code is implemented.
    // You will always need it when your code is declared within a class different than the PApplet.
    frame3.setKeyBinding(LEFT, "rotateYPos");
    frame3.setKeyBinding((processing.event.Event.SHIFT | processing.event.Event.CTRL), LEFT, "rotateXNeg");

    //frame 4
    //frame4 will behave as frame3 since the latter is passed as its
    //referenceFrame() in its constructor
    frame4 = new InteractiveFrame(scene, frame2);
    frame4.setFrontShape("boxDrawing");
    frame4.setPickingShape("boxPicking");
    // note that the following:
    //frame4.setFrontShape(this, "boxDrawing");
    //frame4.setPickingShape(this, "boxPicking");
    // also works. The first parameter points to the class where your code is implemented.
    // You will always need it when your code is declared within a class different than the PApplet.
    frame4.setHighlightingMode(InteractiveFrame.HighlightingMode.FRONT_PICKING_SHAPES);
    frame4.translate(200, 200);
    frame4.scale(3);
    // if two frames have the same key binding (frame2 has also the 'u' binding)
    // the one that is the default grabber takes higher precedence
    frame4.setKeyBinding('u', "translateXPos");
    frame4.setKeyBinding(processing.event.Event.SHIFT, 'u', "translateXNeg");

    frame4.setTrackingEyeDistance(1000);
    frame4.setTrackingEyeAzimuth(PI);
    frame4.setTrackingEyeInclination(PI);
    scene.setAvatar(frame4);
    //scene.eyeFrame().removeMotionBindings();

    ArrayList<GenericFrame> path = scene.branch(frame1, frame2, false);
    println(path.size());
  }

  public void boxDrawing(PGraphics pg) {
    //pg.fill(0, 255, 0);
    pg.noFill();
    scene.drawCone(pg);
    //pg.strokeWeight(3);
    //pg.box(30);
  }

  public void boxPicking(PGraphics pg) {
    pg.noStroke();
    pg.fill(255, 0, 0, 126);
    pg.sphere(30);
  }

  ///*
  public void boxCustomMotion(InteractiveFrame frame, MotionEvent event) {
    frame.screenRotate(event);
  }
  //*/

  /*
  public void boxCustomMotion(InteractiveFrame frame, DOF2Event event) {
    println("custom"); frame.screenRotate(event);
  }
  //*/

  public void boxCustomClick(InteractiveFrame frame) {
    if (frame.scene().mouseAgent().pickingMode() == MouseAgent.PickingMode.MOVE)
      frame.center();
  }

  public void draw() {
    background(255);

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

  /*
  public boolean checkIfGrabsInput(Scene frame, KeyEvent event) {
    println("scene picking condition called!");
    return scene.checkIfGrabsInput(event);
  }

  public boolean checkIfGrabsInput(InteractiveFrame frame, CustomEvent event) {
    //println("proof-of-concept picking condition called!");
    return false;
  }

  public boolean checkIfGrabsInput(InteractiveFrame frame, DOF2Event event) {
    //println("custom picking condition called!");
    return frame.checkIfGrabsInput(event);
  }
  */

  public void keyPressed() {
    if (key == ' ')
      if (scene.mouseAgent().pickingMode() == MouseAgent.PickingMode.CLICK) {
        scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.MOVE);
        scene.eyeFrame().setMotionBinding(LEFT, "rotate");
        scene.eyeFrame().removeMotionBinding(MouseAgent.NO_BUTTON);
      } else {
        scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.CLICK);
        scene.eyeFrame().setMotionBinding(MouseAgent.NO_BUTTON, "rotate");
        scene.eyeFrame().removeMotionBinding(LEFT);
      }
    if (key == 'u') {
      if (scene.pg().shapeMode == CORNER)
        scene.pg().shapeMode(CENTER);
      else
        scene.pg().shapeMode(CORNER);
    }
    // set the default grabber at both the scene.motionAgent() and the scene.keyAgent()
    if (key == 'v') {
      scene.inputHandler().setDefaultGrabber(frame1);
      println(frame1.info());
    }
    if (key == 'x') {
      scene.inputHandler().setDefaultGrabber(frame2);
      println(frame2.info());
    }
    if (key == 'y') {
      scene.inputHandler().setDefaultGrabber(frame3);
      println(frame3.info());
    }
    if (key == 'z') {
      scene.inputHandler().setDefaultGrabber(frame4);
      println(frame4.info());
    }
    if (key == 'w') {
      scene.inputHandler().setDefaultGrabber(scene.eyeFrame());
      println(scene.eyeFrame().info());
    }
    if(key == 'm')
      frame2.get();
    if(key == 'n')
      frame4.get();
  }

  public class CustomEvent extends Event {
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.FrameInteraction"});
  }
}