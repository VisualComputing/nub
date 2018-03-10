/**
 * Eye2.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates the following frame hierarchy:
 *
 *   World
 *     ^
 *     |\
 *     | \
 * shape1 eye
 *     |
 * shape2
 */

import frames.core.*;
import frames.primitives.*;
import frames.processing.*;
import frames.input.*;

Shape shape1, shape2, eye;
Scene scene;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(700, 700, renderer);
  scene = new Scene(this);
  // node instantation requires a graph or a (reference) node.
  // Inner classes are used to define what is going to happen during traversal
  shape1 = new OrbitShape(scene);
  shape1.set(caja(100));
  shape2 = new OrbitShape(shape1);
  shape2.set(bola(100));
  shape2.translate(new Vector(250, 250));
  eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setFieldOfView(PI / 3);
  // user gesture input data is directed towards
  // the eye when no other node is being picked
  scene.setDefaultGrabber(eye);
  scene.setRadius(300);
  scene.fitBallInterpolation();
}

void draw() {
  background(0);
  // traversal algorithm visit all nodes belonging to the graph
  scene.traverse();
}

PShape bola(float radius) {
  PShape bola = scene.is3D() ? createShape(SPHERE, radius) : createShape(ELLIPSE, 0, 0, radius, radius);
  bola.setStrokeWeight(3);
  bola.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  bola.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return bola;
}

PShape caja(float length) {
  PShape caja = scene.is3D() ? createShape(BOX, length) : createShape(RECT, 0, 0, length, length);
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}