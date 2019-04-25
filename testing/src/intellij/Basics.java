package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;

/**
 * Created by pierre on 11/15/16.
 */
public class Basics extends PApplet {
  Scene scene;
  Node node, shape;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fit(1);

    node = new Node(scene) {
      @Override
      public void visit() {
        scene.drawAxes(scene.radius() / 3);
        pushStyle();
        rectMode(CENTER);
        fill(255, 0, 255);
        if (scene.is3D())
          scene.drawCylinder(30, scene.radius() / 4, 200);
        else
          rect(10, 10, 200, 200);
        stroke(255, 255, 0);
        scene.drawShooterTarget(this);
        popStyle();
      }
    };
    node.setRotation(Quaternion.random());
    //shape = new Node(scene, shape());
    shape = new Node(scene);
    shape.setShape(shape());
    shape.setRotation(Quaternion.random());
    shape.translate(275, 275, 275);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.render();
  }

  public void mouseMoved() {
    scene.spin();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public void keyPressed() {
    if (key == ' ')
      scene.track();
    if (key == 'f')
      scene.flip();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Basics"});
  }
}
