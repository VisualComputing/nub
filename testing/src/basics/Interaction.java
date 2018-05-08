package basics;

import frames.core.Node;
import frames.primitives.Frame;
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
public class Interaction extends PApplet {
  Scene scene;
  Node eye;
  Shape node, frame;
  Frame trackedFrame;
  Frame defaultFrame;
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

    eye = new Node(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    node = new Shape(scene) {
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
    node.setRotation(Quaternion.random());

    //scene.removeNodes();
    node.translate(75, 75, 75);

    frame = new Shape(node);
    frame.setShape(shape());
    frame.translate(275, 275, 275);

    defaultFrame = eye;
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    if (mouseDragged)
      scene.cast();
    else
      trackedFrame = scene.cast(mouseX, mouseY);
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
      defaultFrame = eye;
    if (key == '2')
      defaultFrame = node;
    if (key == '3')
      defaultFrame = frame;
  }

  public void mousePressed() {
    upVector = eye.yAxis();
  }

  public void mouseDragged() {
    mouseDragged = true;
    //if(mouseY-pmouseY > 0)
    //println("deltaY positive");
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
    if (trackedFrame == null)
      scene.spin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY));
    else
      scene.rotate((mouseY - pmouseY), 0, 0, PI / width, trackedFrame);
  }

  public void mouseReleased() {
    mouseDragged = false;
  }

  PShape shape() {
    PShape fig = createShape(BOX, 160);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Interaction"});
  }
}
