package basics;

import common.InteractiveNode;
import frames.core.Node;
import frames.input.Event;
import frames.primitives.Frame;
import frames.processing.Mouse;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;

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
    eye.setDamping(0f);

    node = new Shape(scene) {
      @Override
      public void set(PGraphics pGraphics) {
        pGraphics.pushStyle();
        pGraphics.fill(255, 0, 255);
        Scene.drawCylinder(pGraphics, 30, radius, 200);
        pGraphics.popStyle();
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
    node.setDamping(0);

    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultGrabber(eye);
    scene.fitBallInterpolation();
  }

  public void graphics(PGraphics pg) {

    pg.rect(0, 0, radius, radius);
  }

  public void draw() {
    background(0);
    scene.drawAxes();

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
