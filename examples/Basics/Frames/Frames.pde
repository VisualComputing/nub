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
  frame1.translate(new Vector(250, 250));
  frame2 = new Frame(frame1, new Vector(200, 200), new Quaternion());
}

void draw() {
  background(0);
  push();
  translate(float(translation % width) - 250, 0);
  if (world)
    translation++;
  apply(frame1);
  stroke(0, 255, 0);
  fill(255, 0, 255, 125);
  bola(100);
  push();
  rotate(radians(rotation));
  if (!world)
    --rotation;
  apply(frame2);
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

void apply(Frame frame) {
  if (g.is3D())
    applyMatrix(Scene.toPMatrix(frame.matrix()));
  // applyMatrix is not available in 2D!
  else {
    translate(frame.translation().x(), frame.translation().y());
    rotate(frame.rotation().angle2D());
    scale(frame.scaling(), frame.scaling());
  }
}

void keyPressed() {
  world = !world;
}