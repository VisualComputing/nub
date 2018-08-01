package basics;

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class DetachedFrames1 extends PApplet {
  Frame[] frames;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    frames = new Frame[50];
    for (int i = 0; i < frames.length; i++) {
      frames[i] = new Frame();
      frames[i].randomize(new Vector(400, 400, 0), 400, g.is3D());
    }
  }

  public void draw() {
    background(0);
    for (int i = 0; i < frames.length; i++) {
      pushMatrix();
      Scene.applyTransformation(g, frames[i]);
      Scene.drawTorusSolenoid(g);
      popMatrix();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.DetachedFrames1"});
  }
}
