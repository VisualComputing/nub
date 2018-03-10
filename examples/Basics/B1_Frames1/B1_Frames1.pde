/**
 * Frames.
 * by Jean Pierre Charalambos.
 *
 * This example implements the following 'graph-less' frame hierarchy:
 *
 * World
 *   ^
 *   |
 * frame1
 *   |
 * frame2
 *   |
 * frame3
 *
 * To enter a frame coordinate system use the following pattern:
 *
 * push();
 * Scene.applyTransformation(this.g, frame);
 * // coordinates here are given in the frame system
 * pop();
 *
 * Press any key to change the animation.
 */

import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Frame frame1, frame2, frame3;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P2D;
boolean world;
int translation, rotation;

void setup() {
  size(700, 700, renderer);
  frame1 = new Frame();
  frame1.translate(0, height/2);
  frame2 = new Frame();
  frame2.setReference(frame1);
  frame3 = new Frame(frame2, new Vector(200, 200), new Quaternion());
}

// Scene.applyTransformation does the same as apply(PMatrix), but:
// 1. It also works in 2D.
// 2. It's far more efficient (apply(PMatrix) computes the inverse).
void draw() {
  background(0);
  updateFrames();
  push();
  Scene.applyTransformation(this.g, frame1);
  stroke(0, 255, 0);
  fill(255, 0, 255, 125);
  bola(100);
  push();
  Scene.applyTransformation(this.g, frame2);
  push();
  Scene.applyTransformation(this.g, frame3);
  stroke(255, 0, 0);
  fill(0, 255, 255);
  caja(100);
  pop();
  pop();
  pop();
}

void bola(float radius) {
  if (g.is3D())
    sphere(radius);
  else
    ellipse(0, 0, radius, radius);
}

void caja(float length) {
  if (g.is3D())
    box(length);
  else
    rect(0, 0, length, length);
}

void push() {
  pushStyle();
  pushMatrix();
}

void pop() {
  popStyle();
  popMatrix();
}

void updateFrames() {
  if (world)
    translation++;
  else
    --rotation;
  frame1.setTranslation(float(translation % width), height/2);
  frame2.setRotation(new Quaternion(radians(rotation)));
}

void keyPressed() {
  world = !world;
}