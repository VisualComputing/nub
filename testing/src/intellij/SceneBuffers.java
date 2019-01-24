package intellij;

import frames.core.Frame;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class SceneBuffers extends PApplet {
  Scene scene;
  Frame[] shapes;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this, createGraphics(w, h / 2, renderer));
    scene.setRadius(max(w, h));

    shapes = new Frame[100];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Frame(scene, caja());
      shapes[i].randomize();
      shapes[i].setPickingThreshold(0);
    }
    scene.fit(1);
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    scene.beginDraw();
    scene.frontBuffer().background(10,50,25);
    scene.render();
    scene.endDraw();
    scene.display();
    // 2. Display back buffer
    image(scene.backBuffer(), 0, h / 2);
  }

  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.scale(event.getCount() * 20, scene.eye());
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.SceneBuffers"});
  }
}
