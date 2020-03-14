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
  if (g.is3D()) {
    // 1. Define a projection
    // horrible cast. Yep that's Java :p
    ((PGraphicsOpenGL)g).setProjection(toPMatrix(Graph.projection(eye, Graph.Type.PERSPECTIVE, width, height, zNear, zFar, leftHanded)));
    // 2. Render from the eye point-of-view
    eye.orbit(new Vector(0, 1, 0), 0.01);
    // 3. Apply the 3D eye transformation
    setMatrix(toPMatrix(eye.view()));
  } else {
    // IMPORTANT: Need **only** to discriminate 
    // 2D from 3D when there's no projection,
    // as it happens to be with Processing desktop.
    // Check how it's done in P5.js (!)
    // 1. Render from the eye point-of-view
    eye.orbit(new Vector(0, 0, 1), 0.01);
    // 2. Apply the 2D eye transformation
    bind2D();
  }
  for (int i = 0; i < nodes.length; i++) {
    g.push();
    applyTransformation(g, nodes[i]);
    if (i%2 == 1) {
      g.strokeWeight(3);
      g.stroke(255, 255, 0);
      g.fill(0, 255, 0);
      if(g.is3D())
        g.box(20);
      else
        g.square(0, 0, 20);
    }
    else {
      g.noStroke();
      g.fill(255, 0, 255);
      if(g.is3D())
        g.sphere(20);
      else
        g.circle(0, 0, 20);
    }
    g.pop();
  }
}

public static void applyTransformation(PGraphics pGraphics, Node node) {
  if (pGraphics.is3D()) {
    pGraphics.translate(node.translation().x(), node.translation().y(), node.translation().z());
    pGraphics.rotate(node.rotation().angle(), (node.rotation()).axis().x(), (node.rotation()).axis().y(), (node.rotation()).axis().z());
    pGraphics.scale(node.scaling(), node.scaling(), node.scaling());
  } else {
    pGraphics.translate(node.translation().x(), node.translation().y());
    pGraphics.rotate(node.rotation().angle2D());
    pGraphics.scale(node.scaling(), node.scaling());
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

void bind2D() {
  Vector pos = eye.position();
  Quaternion o = eye.orientation();
  translate(width / 2, height / 2);
  scale(1 / eye.magnitude(), 1 / eye.magnitude());
  rotate(-o.angle2D());
  translate(-pos.x(), -pos.y());
}
