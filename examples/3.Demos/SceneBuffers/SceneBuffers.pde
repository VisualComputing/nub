/**
 * SceneBuffers.
 * by Jean Pierre Charalambos.
 * 
 * This example displays the scene front and back buffers.
 * 
 * The front buffer is filled with some scene objects.
 * The back buffer is used to pick them.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Shape[] shapes;
PGraphics canvas;

//Choose one of P3D for a 3D scene or P2D for a 2D one.
String renderer = P3D;
int w = 1000;
int h = 1000;

void settings() {
  size(w, h, renderer);
}

void setup() {
  rectMode(CENTER);
  scene = new Scene(this, createGraphics(w, h / 2, renderer));
  scene.setRadius(max(w, h));

  shapes = new Shape[100];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Shape(scene);
    shapes[i].setGraphics(caja());
    shapes[i].randomize();
  }
  scene.setFieldOfView(PI / 3);
  scene.fitBallInterpolation();
}

void draw() {
  // 1. Fill in and display front-buffer
  scene.beginDraw();
  scene.frontBuffer().background(10,50,25);
  scene.traverse();
  scene.endDraw();
  scene.display();
  // 2. Display back buffer
  image(scene.backBuffer(), 0, h / 2);
}

void mouseMoved() {
  scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.zoom(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}

PShape caja() {
  PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}
