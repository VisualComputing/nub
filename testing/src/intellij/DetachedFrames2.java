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
      applyTransformation(nodes[i]);
      Scene.drawTorusSolenoid(g);
      popMatrix();
    }
  }

  public void applyTransformation(Node node) {
    if (g.is3D()) {
      translate(node.translation()._vector[0], node.translation()._vector[1], node.translation()._vector[2]);
      rotate(node.rotation().angle(), (node.rotation()).axis()._vector[0], (node.rotation()).axis()._vector[1], (node.rotation()).axis()._vector[2]);
      scale(node.scaling(), node.scaling(), node.scaling());
    } else {
      translate(node.translation().x(), node.translation().y());
      rotate(node.rotation().angle2D());
      scale(node.scaling(), node.scaling());
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.DetachedFrames2"});
  }
}
