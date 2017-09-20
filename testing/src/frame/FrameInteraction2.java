package frame;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.Event;
import remixlab.bias.event.MotionShortcut;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class FrameInteraction2 extends PApplet {
  Scene scene;
  InteractiveFrame frame1, frame2;
  boolean corner = true;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P2D;

  public void settings() {
    size(600, 600, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    scene.eyeFrame().setDamping(0);
    scene.setPickingVisualHint(true);

    //frame 1
    frame1 = new InteractiveFrame(scene, "drawAxes");
    frame1.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    frame1.setGrabsInputThreshold(scene.radius() / 4);
    frame1.translate(50, 50);

    //frame 3
    frame2 = new InteractiveFrame(scene, frame1, "drawRect");
    frame2.translate(-100, -50);
    frame2.setKeyBinding(LEFT, "rotateYPos");
    frame2.setMotionBinding((Event.SHIFT | Event.CTRL), LEFT, "translate");
    frame2.setMotionBinding((Event.SHIFT | Event.CTRL), RIGHT, "rotate");
    frame2.setKeyBinding((Event.SHIFT | Event.CTRL), LEFT, "rotateZNeg");

    println(frame2.action(new MotionShortcut((Event.SHIFT | Event.CTRL), LEFT)));
    println(frame2.action(new MotionShortcut((Event.SHIFT | Event.CTRL), RIGHT)));
  }

  public void drawRect(PGraphics pg) {
    pg.color(0, 255, 255);
    ///*
    if (corner)
      pg.rectMode(CENTER);
    else
      pg.shapeMode(CORNER);
    //*/
    pg.rect(20, 20, 40, 40);
  }

  public void draw() {
    background(0);
    scene.drawFrames();
  }

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
      if (corner)
        corner = false;
      else
        corner = true;
    }
    if (key == 'v') {
      if (g.rectMode == CORNER)
        rectMode(CENTER);
      else
        rectMode(CORNER);
    }
    // set the default grabber at both the scene.motionAgent() and the scene.keyAgent()
    if (key == 'x') {
      scene.inputHandler().setDefaultGrabber(frame1);
      println(frame1.info());
    }
    if (key == 'y') {
      scene.inputHandler().setDefaultGrabber(frame2);
      println(frame2.info());
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.FrameInteraction2"});
  }
}