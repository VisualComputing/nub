package basics;

import processing.core.*;
import processing.event.Event;
import remixlab.dandelion.core.GenericFrame;
import remixlab.proscene.*;

public class Demo extends PApplet {
  Scene scene;

  InteractiveFrame frame, frame2;
  PShape shape;

  String renderer = P3D;
  public void settings() {
    size(800, 800, renderer);
  }

  @Override
  public void setup() {
    scene = new Scene(this);
    frame = new InteractiveFrame(scene);
    // shapes

    //frame.setShape("drawCylinder");
    frame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    frame.setShape("cyl");

    frame2 = new InteractiveFrame(scene, frame);
    shape = createShape(RECT, 0, 0, 80, 80);
    frame2.setShape(shape);

    // behaviors
    frame.setMotionBinding(Event.CTRL | Event.SHIFT, LEFT, "translate");
    frame.setClickBinding(RIGHT, 1, "action");

    scene.loadConfig();
  }

  @Override
  public void draw() {
    background(124);
    //scene.drawFrames();

    frame.draw();
    frame2.draw();
    /*
    frame.applyTransformation();
    cyl(scene.pg());
    frame2.applyTransformation();
    shape(shape);
    //*/
  }

  public void cyl(PGraphics pg) {
    pg.fill(255,0,0);
    scene.drawCylinder(pg);
  }

  public void action(InteractiveFrame f) {
    println("click");
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Demo"});
  }
}
