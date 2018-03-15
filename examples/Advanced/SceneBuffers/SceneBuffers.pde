/**
 * SceneBuffers.
 * by Jean Pierre Charalambos.
 * 
 * This example displays the scene front and back buffers.
 * 
 * The front buffer is filled with some scene objects.
 * The back buffer is used to pick them.
 */

import frames.input.*;
import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Scene scene;
OrbitShape[] models;
PGraphics canvas;

//Choose one of P3D for a 3D scene or P2D.
String renderer = P3D;
int w = 1110;
int h = 1110;

void setup() {
  size(1000, 1000, renderer);
  scene = new Scene(this, createGraphics(w, h / 2, renderer));
  scene.setRadius(1000);

  models = new OrbitShape[100];
  for (int i = 0; i < models.length; i++) {
    models[i] = new OrbitShape(scene);
    models[i].set(boxShape());
    models[i].randomize();
  }
  OrbitShape eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setDefaultGrabber(eye);
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

PShape boxShape() {
  PShape box = createShape(BOX, 60);
  box.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return box;
}