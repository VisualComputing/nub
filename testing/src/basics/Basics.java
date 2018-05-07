package basics;

import frames.core.Frame;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;

/**
 * Created by pierre on 11/15/16.
 */
public class Basics extends PApplet {
  Scene scene;
  Frame eye;
  Frame node, frame;
  Frame trackedFrame;
  Frame defaultNode;
  Vector upVector;
  boolean mouseDragged;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    eye = new Frame(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    node = new Frame(scene) {
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
        popStyle();
        scene.drawShooterTarget(this);
      }
    };
    node.setRotation(Quaternion.random());

    //scene.removeNodes();
    node.translate(75, 75, 75);

    frame = new Frame(node) {
      @Override
      public void visit() {
        scene.drawAxes(scene.radius() / 3);
        pushStyle();
        rectMode(CENTER);
        fill(255, 255, 0);
        if (scene.is3D())
          scene.drawCylinder(30, scene.radius() / 4, 200);
        else
          rect(10, 10, 200, 200);
        popStyle();
        scene.drawShooterTarget(this);
      }
    };
    frame.translate(275, 275, 275);

    //defaultNode = eye;
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    //scene.cast();
    if(mouseDragged)
      scene.cast();
    else
      trackedFrame = scene.cast(mouseX, mouseY);

    //if (mousePressed && (mouseButton == LEFT))
    //eye.spin(eye.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY)));
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
    if (key == '1')
      defaultNode = eye;
    if (key == '2')
      defaultNode = node;
    if (key == '3')
      defaultNode = frame;
    if (key == ' ') {
      if (eye.isAttached())
        println("win1!");
      Frame f1 = eye.detach();
      if (f1.isDetached())
        println("win2!");
    }
  }

  public void mousePressed() {
    upVector = eye.yAxis();
  }

  public void mouseDragged() {
    mouseDragged = true;
    //if(mouseY-pmouseY > 0)
    //println("deltaY positive");
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(mouseX-pmouseX, mouseY-pmouseY)));
    //defaultNode.spin(defaultNode.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY)));
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(mouseX-pmouseX, 0)));
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(0, mouseY-pmouseY)));
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(0, 0, mouseX-pmouseX)));
    //defaultNode.rotate(defaultNode.gestureRotate(0,(mouseY-pmouseY),0, PI / width));
    //defaultNode.rotate(defaultNode.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    //defaultNode.rotate(defaultNode.gestureRotate(0,0,(mouseX-pmouseX), PI / height));
    //defaultNode.spin(defaultNode.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY), 5));
    //eye.rotate(eye.gestureLookAround(mouseX-pmouseX, mouseY-pmouseY, upVector, PI/(4*width)));
    //eye.spin(eye.gestureRotateCAD(mouseX - pmouseX, mouseY - pmouseY, 2.0f / height));
    /*
    if (defaultNode == eye)
      scene.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else
      scene.rotate((mouseY - pmouseY), 0, 0, PI / width, defaultNode);
      */
    //defaultNode.rotate(defaultNode.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    //defaultNode.spin(defaultNode.gestureRotate((mouseY-pmouseY),0, 0, PI / width));
    if (trackedFrame == null)
      scene.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else
      scene.rotate((mouseY - pmouseY), 0, 0, PI / width, trackedFrame);
  }

  public void mouseReleased() {
    mouseDragged = false;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Basics"});
  }
}
