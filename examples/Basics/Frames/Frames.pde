/**
 * Frames.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates the following graph-less (i.e.,
 * no Graph needed to be instantiated) frame hierarchy:
 *
 * World
 *   ^
 *   |
 * frame1
 *   |
 * frame2
 *
 * Press any key to change the animation.
 */

import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Frame frame1, frame2;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;
boolean world;
int translation, rotation;

void setup() {
  size(700, 700, renderer);
  frame1 = new Frame();
  frame2 = new Frame(frame1, new Vector(200, 200), new Quaternion());
}

// Scene.applyTransformation does the same as apply(PMatrix), but:
// 1. It also works in 2D.
// 2. It's far more efficient (apply(PMatrix) computes the inverse).
void draw() {
  background(0);
  push();
  translate(float(translation % width), height/2);
  if (world)
    translation++;
  Scene.applyTransformation(this.g, frame1);
  stroke(0, 255, 0);
  fill(255, 0, 255, 125);
  bola(100);
  push();
  rotate(radians(rotation));
  if (!world)
    --rotation;
  Scene.applyTransformation(this.g, frame2);
  stroke(255, 0, 0);
  fill(0, 255, 255);
  caja(100);
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

void keyPressed() {
  world = !world;
}