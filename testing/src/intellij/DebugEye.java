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
    scene = new Scene(this);
    //scene.togglePerspective();
    Node box1 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(255, 0, 0);
        pg.box(30);
        pg.popStyle();
      }

      @Override
      public void interact(Object... gesture) {
        if (gesture.length == 1) {
          if (gesture[0] instanceof Float) {
            if (orbit) {
              Quaternion q = new Quaternion(displacement(axis), (float) gesture[0]);
              orbit(q, scene.center(), inertia);
            } else {
              Quaternion q = new Quaternion((float) gesture[0], 0, 0);
              rotate(q, inertia);
            }
          }
        }
      }
    };
    Node box2 = new Node(box1) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(0, 0, 255);
        pg.box(5);
        pg.popStyle();
      }
    };
    box2.translate(0, 0, 20);
    scene.setRadius(50);
    scene.fit(1);
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.drawArrow(axis);
    stroke(125);
    scene.drawGrid();
    lights();
    scene.render();
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
        scene.mouseTranslate();
        break;
      case CENTER:
        if (event.isShiftDown()) {
          scene.scale((float) mouseX - pmouseX);
        } else {
          if (!scene.interactTag(scene.mouseRADX()))
            if (cad)
              scene.mouseRotateCAD();
            else
              scene.mouseLookAround();
        }
        break;
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (!scene.interactTag((float) event.getCount() * 10.f * PI / (float) width))
      scene.moveForward(event.getCount() * 10);
  }

  public void keyPressed() {
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
