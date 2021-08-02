package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class DebugEye extends PApplet {
  Scene scene;
  Vector axis;
  boolean cad, peasy, orbit;
  float inertia = 0.8f;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this, 50);
    scene.enableHint(Scene.BACKGROUND | Scene.AXES | Scene.GRID);
    //scene.togglePerspective();
    Node box1 = new Node();
    box1.setShape((PGraphics pg) -> {
      pg.pushStyle();
      pg.strokeWeight(1 / 10f);
      pg.fill(255, 0, 0);
      pg.box(30);
      pg.popStyle();
    });
    box1.setInteraction((Object... gesture) -> {
      if (gesture.length == 1) {
        if (gesture[0] instanceof Float) {
          if (orbit) {
            // op1
            //Quaternion q = new Quaternion(box1.displacement(axis), (float) gesture[0]);
            //box1.orbit(q, scene.center(), inertia);
            // op2
            box1.orbit(axis, (float) gesture[0], scene.center(), inertia);
            // op3
            //box1.orbit(axis, (float) gesture[0], inertia);
          } else {
            Quaternion q = new Quaternion((float) gesture[0], 0, 0);
            box1.rotate(q, inertia);
          }
        }
      }
    });
    Node box2 = new Node(box1);
    box2.setShape((PGraphics pg) -> {
      pg.pushStyle();
      pg.strokeWeight(1 / 10f);
      pg.fill(0, 0, 255);
      pg.box(5);
      pg.popStyle();
    });
    box2.translate(0, 0, 20);
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void draw() {
    lights();
    scene.render();
    noStroke();
    fill(255, 255, 0);
    scene.drawArrow(axis);
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged(MouseEvent event) {
    switch (mouseButton) {
      case LEFT:
        scene.mouseSpin();
        break;
      case RIGHT:
        scene.mouseShift();
        break;
      case CENTER:
        if (event.isShiftDown()) {
          scene.zoom((float) mouseX - pmouseX);
        } else {
          if (scene.node() != null) {
            scene.interact(scene.mouseRADX());
          }
          else {
            if (cad)
              scene.mouseCAD();
            else
              scene.mouseLookAround();
          }
        }
        break;
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.node() != null) {
      scene.interact((float) event.getCount() * 10.f * PI / (float) width);
    }
    else {
      scene.moveForward(event.getCount() * 10);
    }
  }

  public void keyPressed() {
    if (key == 'r') {
      axis = Vector.random();
      axis.multiply(scene.radius() / 3);
    }
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
    if (key == 'p')
      peasy = !peasy;
    if (key == 'c')
      cad = !cad;
    if (key == 'o')
      orbit = !orbit;
    if (key == 's')
      scene.fit(1);
    if (key == 'S')
      scene.fit();
    if (key == '0')
      inertia = 0.f;
    if (key == '1')
      inertia = 0.1f;
    if (key == '2')
      inertia = 0.2f;
    if (key == '3')
      inertia = 0.3f;
    if (key == '4')
      inertia = 0.4f;
    if (key == '5')
      inertia = 0.5f;
    if (key == '6')
      inertia = 0.6f;
    if (key == '7')
      inertia = 0.7f;
    if (key == '8')
      inertia = 0.8f;
    if (key == '9')
      inertia = 0.9f;
    if (key == 'd')
      inertia = 1.f;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.DebugEye"});
  }
}
