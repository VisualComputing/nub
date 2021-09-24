/**
 * Scene Buffers.
 * by Jean Pierre Charalambos.
 *
 * This example displays the scene front and back buffers.
 *
 * The front buffer is filled with some scene objects.
 * The back buffer is used to pick them.
 *
 * Press 'a' to toggle the node axes hint.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node cajas, bolas;
Node[] shapes;

//Choose P2D or P3D
String renderer = P3D;
int w = 1200;
int h = 1200;

void settings() {
  size(w, h, renderer);
}

void setup() {
  rectMode(CENTER);
  scene = new Scene(createGraphics(w, h /2, renderer), max(w, h));
  cajas = new Node();
  bolas = new Node();
  shapes = new Node[100];
  for (int i = 0; i < shapes.length; i++) {
    if (i%2==0) {
      shapes[i] = new Node(cajas, caja());
    }
    else {
      shapes[i] = new Node(bolas, bola());
    }
    scene.randomize(shapes[i]);
    shapes[i].enableHint(Node.AXES);
  }
}

void draw() {
  // (un)comment as you wish
  // 1. Fill in and display front-buffer
  // 1a. Display both cajas and bolas
  //scene.display(color(10, 50, 25));
  // 1b. Display either cajas or bolas, or both
  scene.openContext();
  scene.context().background(color(10, 50, 25));
  scene.drawAxes();
  scene.render(cajas);
  scene.render(bolas);
  scene.closeContext();
  scene.image();
  // 2. Display back buffer
  scene.displayBackBuffer(0, h / 2);
}

void mouseMoved() {
  scene.tag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.zoom(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 30);
  else
    scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == 'a') {
    for (int i = 0; i < shapes.length; i++)
      shapes[i].toggleHint(Node.AXES);
  }
}

PShape caja() {
  PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}

PShape bola() {
  PShape bola = scene.is3D() ? createShape(SPHERE, random(60, 100)) : createShape(ELLIPSE, 0, 0, random(60, 100), random(60, 100));
  bola.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  bola.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return bola;
}