package basics;

import frames.core.Node;
import frames.input.Event;
import frames.input.Shortcut;
import frames.input.event.TapShortcut;
import frames.primitives.Frame;
import frames.primitives.Point;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Mouse;
import frames.processing.Scene;
import frames.processing.Shape;
import frames.timing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Created by pierre on 11/15/16.
 */
public class Interaction extends PApplet {
  Scene scene;
  Node eye, node;
  Node defaultNode;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(400);
    scene.fitBallInterpolation();

    eye = new Node(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.removeNode(eye);
    scene.fitBallInterpolation();

    node = new Shape(scene) {
      @Override
      public void set(PGraphics pGraphics) {
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCylinder(pGraphics, 30, scene.radius()/4, 200);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };

    scene.removeNodes();
    defaultNode = eye;
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.traverse();

    //if (mousePressed && (mouseButton == LEFT))
      //eye.spin(eye.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY)));
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if(key == ' ')
      if(defaultNode == eye)
        defaultNode = node;
      else
        defaultNode = eye;
  }

  public void mouseDragged() {
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(mouseX-pmouseX, mouseY-pmouseY)));
    //defaultNode.spin(defaultNode.gestureSpin(new Point(pmouseX, pmouseY), new Point(mouseX, mouseY)));
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(mouseX-pmouseX, 0)));
    //defaultNode.translate(defaultNode.gestureTranslate(new Vector(0, mouseY-pmouseY)));
    defaultNode.translate(defaultNode.gestureTranslate(new Vector(0, 0, mouseX-pmouseX)));
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Interaction"});
  }
}
