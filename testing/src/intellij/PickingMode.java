package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class PickingMode extends PApplet {
  Scene scene;
  Node[] shapes;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 1600;
  int h = 1600;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(createGraphics(w, h / 2, renderer), max(w, h));
    //scene = new Scene(this);

    shapes = new Node[100];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node();
      //shapes[i] = new Node(this::point);
      //shapes[i] = new Node(caja());
      scene.randomize(shapes[i]);
      shapes[i].enableHint(Node.AXES);
    }
    //scene.fit(1);
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    background(125);
    scene.openContext();
    scene.context().background(0);
    scene.render();
    scene.drawAxes();
    scene.closeContext();
    scene.image();
    // 2. Display back buffer
    scene.displayBackBuffer(0, h / 2);
  }

  public void keyPressed() {
    scene.togglePerspective();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseShift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  void point(PGraphics pg) {
    pg.pushStyle();
    pg.stroke(0, 255, 0);
    pg.strokeWeight(10);
    pg.point(0, 0, 0);
    pg.popStyle();
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.PickingMode"});
  }
}
