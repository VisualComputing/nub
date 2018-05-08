package basics;

import frames.core.Node;
import frames.processing.Scene;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class Basics extends PApplet {
  Scene scene;
  Node eye, node;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    eye = new Node(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    node = new Node(scene) {
      @Override
      public void visit() {
        println("called!");
        scene.drawAxes(scene.radius() / 3);
        pushStyle();
        rectMode(CENTER);
        fill(255, 0, 255);
        if (scene.is3D())
          scene.drawCylinder(30, scene.radius() / 4, 200);
        else
          rect(10, 10, 200, 200);
        popStyle();
        scene.drawShooterTarget(this);
      }
    };
    //node.setRotation(Quaternion.random());
    //node.translate(75, 75, 75);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.cast();
    /*
    if (mouseDragged)
      scene.cast();
    else
      trackedFrame = scene.cast(mouseX, mouseY);
      */
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
  }

  public void mouseDragged() {

  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Basics"});
  }
}
