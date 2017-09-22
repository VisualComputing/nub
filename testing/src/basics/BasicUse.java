package basics;

import processing.core.*;
import remixlab.proscene.*;
import remixlab.dandelion.primitives.*;
import remixlab.dandelion.geom.*;

/**
 * Created by pierre on 11/15/16.
 */
public class BasicUse extends PApplet {
  Scene scene;
  InteractiveFrame iFrame;
  float length = 100;

  public void settings() {
    size(800, 800);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setPickingVisualHint(true);
    scene.setRadius(200);

    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    iFrame.setGrabsInputThreshold(length);
    iFrame.translate(50,50);
    iFrame.rotate(new Rot(QUARTER_PI));
    scene.showAll();
  }

  public void graphics(PGraphics pg) {
    pg.fill(255,0,255);
    pg.rect(0,0,length,length);
  }

  public void draw() {
    background(0);
    scene.drawFrames();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
