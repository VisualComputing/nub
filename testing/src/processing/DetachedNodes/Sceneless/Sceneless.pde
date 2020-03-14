/**
 * Sceneless.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates the use of nub without even instantiating a
 * Scene object.
 *
 * Just define an eye-node and some nodes with torus shapes.
 */

import nub.primitives.*;
import nub.core.*;

Node eye;
Node[] nodes;
boolean leftHanded = false;
float zNear = 80;
float zFar = 800;

//Choose P2D or P3D
String renderer = P3D;

void setup() {
  size(800, 800, renderer);
  eye = new Node();
  float fov = PI / 3;
  eye.setMagnitude(tan(fov/2));
  eye.setPosition(0, 0, 400);
  nodes = new Node[50];
  for (int i = 0; i < nodes.length; i++)
    nodes[i] = Node.random(new Vector(), 400, g.is3D());
  g.rectMode(CENTER);
}

void draw() {
  background(0);
  // 1. Define a projection
  // horrible cast. Yep that's Java :p
  ((PGraphicsOpenGL)g).setProjection(toPMatrix(Graph.projection(eye, Graph.Type.PERSPECTIVE, width, height, zNear, zFar, leftHanded)));
  // 2. Render from the eye point-of-view
  eye.orbit(new Vector(0, g.is3D() ? 1 : 0, g.is3D() ? 0 : 1), 0.01f);
  // 3. Apply the eye transformation
  ((PGraphicsOpenGL)g).modelview.set(toPMatrix(eye.view()));
  for (int i = 0; i < nodes.length; i++) {
    g.push();
    Graph.applyTransformation(g, nodes[i]);
    if (i%2 == 1) {
      g.strokeWeight(3);
      g.stroke(255, 255, 0);
      g.fill(0, 255, 0);
      if (g.is3D())
        g.box(20);
      else
        g.square(0, 0, 20);
    } else {
      g.noStroke();
      g.fill(255, 0, 255);
      if (g.is3D())
        g.sphere(20);
      else
        g.circle(0, 0, 20);
    }
    g.pop();
  }
}

// IMPORTANT:
// Processing desktop uses row-major order matrix presetation
// whereas nub uses column-major order as with openGL
// See: https://en.wikipedia.org/wiki/Row-_and_column-major_order
// Check how it's done in p5.js
public static PMatrix3D toPMatrix(Matrix matrix) {
  float[] a = matrix.get(new float[16], false);
  return new PMatrix3D(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10], a[11], a[12], a[13], a[14], a[15]);
}
