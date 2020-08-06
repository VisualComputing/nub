/**
 * Scene Buffers.
 * by Jean Pierre Charalambos.
 *
 * This example displays the scene front and back buffers.
 *
 * The front buffer is filled with some scene objects.
 * The back buffer is used to pick them.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node[] shapes;

//Choose P2D or P3D
String renderer = P3D;
int w = 700;
int h = 700;

void settings() {
  size(w, h, renderer);
}

void setup() {
  rectMode(CENTER);
  scene = new Scene(createGraphics(w, h /2, renderer), max(w, h));
  scene.enableHint(Scene.BACKGROUND, color(10, 50, 25));

  shapes = new Node[100];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node(caja());
    scene.randomize(shapes[i]);
  }
  scene.fit(1);
}

void draw() {
  // 1. Fill in and display front-buffer
  scene.display();
  // 2. Display back buffer
  scene.displayBackBuffer(0, h / 2);
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 30);
  else
    scene.scaleEye(event.getCount() * 20);
}

PShape caja() {
  PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}
