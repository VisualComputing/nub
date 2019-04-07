package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class DetachedFrames2 extends PApplet {
  Node eye;
  Node[] nodes;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    eye = new Node();
    eye.setPosition(0, 0, 200);
    nodes = new Node[50];
    for (int i = 0; i < nodes.length; i++)
      nodes[i] = Node.random(new Vector(), 100, g.is3D());
  }

  public void draw() {
    background(0);
    float fov = PI / 3.0f;
    float cameraZ = (height / 2.0f) / tan(fov / 2.0f);
    perspective(fov, width / height, cameraZ / 10.0f, cameraZ * 10.0f);
    //((PGraphicsOpenGL)g).setProjection(Scene.toPMatrix(Matrix.perspective(cameraZ / 10.0f, cameraZ * 10.0f, width / height, -tan(fov / 2.0f))));
    //resetMatrix();
    //applyMatrix(Scene.toPMatrix(eye.view()));
    setMatrix(Scene.toPMatrix(eye.view()));
    Scene.drawAxes(g, 100);
    for (int i = 0; i < nodes.length; i++) {
      pushMatrix();
      // TODO fix me!
      //Scene.applyTransformation(g, nodes[i]);
      Scene.drawTorusSolenoid(g);
      popMatrix();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.DetachedFrames2"});
  }
}
