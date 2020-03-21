package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Created by pierre on 11/15/16.
 */
public class Basics extends PApplet {
  Scene scene;
  Node node;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fit(1);

    node = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        Scene.drawAxes(pg, scene.radius() / 3);
        pg.pushStyle();
        pg.rectMode(CENTER);
        pg.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCylinder(pg, 30, scene.radius() / 4, 200);
        else
          pg.rect(10, 10, 200, 200);
        pg.stroke(255, 255, 0);
        scene.drawSquaredBullsEye(this);
        pg.popStyle();
      }
    };
    node.setRotation(Quaternion.random());
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseSpin();
  }

  public void keyPressed() {
    if (key == ' ')
      scene.updateMouseTag();
    if (key == 'f')
      scene.flip();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Basics"});
  }
}
