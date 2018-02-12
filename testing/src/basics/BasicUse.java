package basics;

import common.InteractiveNode;
import processing.core.PApplet;
import processing.core.PGraphics;
import proscene.core.Node;
import proscene.input.Event;
import proscene.primitives.Frame;
import proscene.processing.Mouse;
import proscene.processing.Scene;
import proscene.processing.Shape;

/**
 * Created by pierre on 11/15/16.
 */
public class BasicUse extends PApplet {
  Scene scene;
  Frame frame;
  Node eye, node;
  float radius = 100;
  PGraphics pg;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    pg = this.g;
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(400);

    frame = new Frame();
    eye = new InteractiveNode(scene);

    node = new Shape(scene) {
      @Override
      public void set(PGraphics pg) {
        pg.pushStyle();
        pg.fill(255, 0, 255);
        Scene.drawCylinder(pg, 30, radius, 200);
        pg.popStyle();
      }

      @Override
      public void interact(Event event) {
        if (event.shortcut().matches(Mouse.RIGHT))
          translate(event);
        else if (event.shortcut().matches(Mouse.LEFT))
          rotate(event);
        else if (event.shortcut().matches(Mouse.RIGHT_TAP))
          align();
        else if (event.shortcut().matches(Mouse.WHEEL))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }
    };

    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultNode(eye);
    scene.fitBallInterpolation();
  }

  public void graphics(PGraphics pg) {

    pg.rect(0, 0, radius, radius);
  }

  public void draw() {
    background(0);
    scene.traverse();

    pushStyle();
    scene.pushModelView();
    scene.applyTransformation(frame);
    fill(255, 0, 0, 100);
    sphere(radius);
    scene.popModelView();
    popStyle();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
