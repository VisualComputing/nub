package basics;

import common.InteractiveNode;
import frames.core.Node;
import frames.input.Event;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
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

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(400);

    frame = new Frame();
    eye = new InteractiveNode(scene);
    //eye.setDamping(0f);
    //eye.setRotationSensitivity(0.1f);
    //eye.setSpinningSensitivity(0);

    node = new Shape(scene) {
      @Override
      public void set(PGraphics pGraphics) {
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if(scene.is3D())
          Scene.drawCylinder(pGraphics, 30, radius, 200);
        else
          pGraphics.rect(10,10,200,200);
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
    node.setDamping(0.2f);
    node.setSpinningSensitivity(0);
    //node.startSpinning(Quaternion.random(), 10,1);
    if(node.isSpinning())
      println("node is spinning");
    else
      println("node is NOT spinning");

    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultGrabber(eye);
    scene.fitBallInterpolation();
  }

  public void draw() {
    background(0);
    scene.drawAxes();

    scene.traverse();

    pushStyle();
    scene.pushModelView();
    scene.applyTransformation(frame);
    fill(255, 0, 0, 100);
    if(scene.is3D())
      sphere(radius);
    else
      ellipse(0,0,radius,radius);
    scene.popModelView();
    popStyle();
  }

  public void keyPressed() {
    if(key == ' ')
      if(eye.isFlying())
        println("IS flying");
    else
        println("is NOT flying");
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
