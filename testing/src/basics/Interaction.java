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
public class Interaction extends PApplet {
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

    shape2 = new Shape(shape1);
    shape2.setShape(shape());
    shape2.translate(275, 275, 275);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    if (mousePressed)
      scene.traverse();
    else
      scene.cast();
  }

  public void keyPressed() {
    if (key == 'f')
      if (scene.isLeftHanded()) {
        scene.setRightHanded();
        println("right");
      } else {
        scene.setLeftHanded();
        println("left");
      }
  }

  public void mousePressed() {
    upVector = eye.yAxis();
  }

  public void mouseDragged() {
    //defaultFrame.translate(defaultFrame.gestureTranslate(new Vector(mouseX-pmouseX, mouseY-pmouseY)));
    //defaultFrame.spin(defaultFrame.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY)));
    //defaultFrame.translate(defaultFrame.gestureTranslate(new Vector(mouseX-pmouseX, 0)));
    //defaultFrame.translate(defaultFrame.gestureTranslate(new Vector(0, mouseY-pmouseY)));
    //defaultFrame.translate(defaultFrame.gestureTranslate(new Vector(0, 0, mouseX-pmouseX)));
    //defaultFrame.rotate(defaultFrame.gestureRotate(0,(mouseY-pmouseY),0, PI / width));
    //defaultFrame.rotate(defaultFrame.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    //defaultFrame.rotate(defaultFrame.gestureRotate(0,0,(mouseX-pmouseX), PI / height));
    //defaultFrame.spin(defaultFrame.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY), 5));
    //eye.rotate(eye.gestureLookAround(mouseX-pmouseX, mouseY-pmouseY, upVector, PI/(4*width)));
    //eye.spin(eye.gestureRotateCAD(mouseX - pmouseX, mouseY - pmouseY, 2.0f / height));
    /*
    if (defaultFrame == eye)
      scene.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else
      scene.rotate((mouseY - pmouseY), 0, 0, PI / width, defaultFrame);
      */
    //defaultFrame.rotate(defaultFrame.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    //defaultFrame.spin(defaultFrame.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    /*
    if (trackedFrame == null)
      scene.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else
      scene.rotate(0, (mouseX - pmouseX), 0, PI / width, trackedFrame);
    // */
    /*
    if (trackedFrame == null)
      scene.rotate(0, (mouseX - pmouseX), 0, PI / height);
    else
      scene.rotate(0, (mouseX - pmouseX), 0,PI / height, trackedFrame);
      //*/
    ///*
    if (mouseButton == LEFT)
      scene.mouseSpin();
      //scene.mouseCAD(PI / height);
    else
      if(mouseButton == RIGHT)
      //scene.mouseTranslate();
      scene.mousePan();
    else
      //scene.zoom(mouseX - pmouseX);
        scene.scale(mouseX - pmouseX);
    //*/
    /*
    if (mouseButton == LEFT)
      scene.spinY((pmouseX - mouseX), PI / width);
    else
      //scene.spin(0, (pmouseX - mouseX), 0, PI / width);
      scene.spinX((pmouseY - mouseY), PI / width);
      //scene.spin((pmouseY - mouseY), (pmouseX - mouseX), 0, PI / width);
      */
    //scene.rotateCAD(mouseX - pmouseX, mouseY - pmouseY, upVector, 2.0f / height);
    //scene.rotateCAD(mouseX - pmouseX, mouseY - pmouseY, 2.0f / height);
  }

  public void mouseWheel(MouseEvent event) {
    //scene.zoom(event.getCount() * 20);
    scene.scale(event.getCount() * 20);
    //scene.spinX(event.getCount(), 20*PI / width);
    //scene.eye().rotate(new Quaternion(event.getCount() * 20*PI / width,0,0), scene.anchor());
    //scene.eye().rotate(new Quaternion(new Vector(1, 0, 0), event.getCount() * 20 * PI / width), scene.anchor());
    //scene.exp(0, event.getCount(), 0, 20*PI / width, trackedFrame);
    //equivalent to:
    //scene.rotate(0, (mouseX - pmouseX), 0, PI / width, trackedFrame);
    //scene.rotate(0, event.getCount(), 0, 20 * PI / width, trackedFrame);
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Interaction"});
  }
}
