package intellij;

import frames.core.Node;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class DetachedFrames1 extends PApplet {
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] = new Node();
      nodes[i].randomize(new Vector(400, 400, 0), 400, g.is3D());
    }
  }

  public void draw() {
    background(0);
    for (int i = 0; i < nodes.length; i++) {
      pushMatrix();
      Scene.applyTransformation(g, nodes[i]);
      Scene.drawTorusSolenoid(g);
      popMatrix();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.DetachedFrames1"});
  }
}
