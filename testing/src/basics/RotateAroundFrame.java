package basics;

import frames.core.Node;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class RotateAroundFrame extends PApplet {
  Scene scene;
  Frame eye;
  Shape shape1, shape2;
  Vector upVector;

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

    shape1 = new Shape(scene) {
      @Override
      public void setShape(PGraphics pGraphics) {
        scene.drawAxes(pGraphics, scene.radius() / 3);
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCylinder(pGraphics, 30, scene.radius() / 4, 200);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175, -275);

    //shape2 = new Shape(shape1);
    shape2 = new Shape(scene);
    shape2.setShape(shape());
    shape2.translate(275, 275, 275);

    scene.setTrackedFrame(shape2);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.traverse();
  }

  public void keyPressed() {
    if (key == 'i')
      scene.setTrackedFrame(scene.isTrackedFrame(shape1) ? shape2 : shape1);
    if (key == 'f')
      scene.flip();
  }

  public void mousePressed() {
    upVector = eye.yAxis();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      if (scene.isTrackedFrame(shape2))
        //shape2.rotate((mouseX-pmouseX)* PI / width, 0, 0, shape1);
        shape2.rotate(new Quaternion(new Vector(0, 1, 0), (mouseX - pmouseX) * PI / width), shape1);
      else
        scene.mouseSpin(shape1);
    } else if (mouseButton == RIGHT)
      //scene.mouseTranslate();
      scene.mousePan();
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.RotateAroundFrame"});
  }
}
