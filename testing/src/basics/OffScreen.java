package basics;

import processing.core.*;
import processing.event.Event;
import remixlab.dandelion.core.GenericFrame;
import remixlab.proscene.*;

public class OffScreen extends PApplet {
  Scene scene1, scene2;
  PGraphics pg1, pg2;
  boolean showMiniMap;
  PShape shape;
  InteractiveFrame iFrame;

  String renderer = P3D;
  public void settings() {
    size(800, 800, renderer);
  }

  @Override
  public void setup() {
    pg1 = createGraphics(800,800, renderer);
    scene1 = new Scene(this, pg1);
    pg2 = createGraphics(400,400, renderer);
    scene2 = new Scene(this, pg2, 400, 400);
    scene2.setRadius(200);
    iFrame = new InteractiveFrame(scene2);
    iFrame.setWorldMatrix(scene1.eyeFrame());
    iFrame.setShape(scene1.eyeFrame());
  }

  @Override
  public void draw() {
    InteractiveFrame.sync(scene1.eyeFrame(), iFrame);
    background(124);
    scene1.beginDraw();
    pg1.background(0);
    scene1.drawFrames();
    scene1.endDraw();
    scene1.display();
    if (showMiniMap) {
      scene2.beginDraw();
      pg2.background(29, 153, 243);
      scene2.pg().fill(255, 0, 255, 125);
      scene2.drawFrames();
      scene2.endDraw();
      scene2.display();
    }
  }

  public void keyPressed() {
    if (key == ' ')
      showMiniMap = !showMiniMap;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.OffScreen"});
  }
}
