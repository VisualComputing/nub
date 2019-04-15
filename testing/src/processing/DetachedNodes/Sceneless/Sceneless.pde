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
import nub.processing.*;

Node eye;
Node[] nodes;
boolean leftHanded = false;
float zNear = 80;
float zFar = 800;

//Choose FX2D, JAVA2D, P2D or P3D
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
}

void draw() {
  background(0);
  if (g.is3D()) {
    // 1. Define a projection
    ((PGraphicsOpenGL)g).setProjection(Scene.toPMatrix(eye.projection(Graph.Type.PERSPECTIVE, width, height, zNear, zFar, leftHanded)));
    // 2. Render from the eye poin-of-view
    eye.orbit(new Vector(0, 1, 0), 0.01);
    // apply the 3D eye transformation
    setMatrix(Scene.toPMatrix(eye.view()));
  } else {
    // 1. Render from the eye poin-of-view
    eye.orbit(new Vector(0, 0, 1), 0.01);
    // apply the 2D eye transformation
    bind2D();
  }
  Scene.drawAxes(g, 100);
  for (int i = 0; i < nodes.length; i++) {
    pushMatrix();
    Scene.applyTransformation(g, nodes[i]);
    Scene.drawTorusSolenoid(g);
    popMatrix();
  }
}

void bind2D() {
  Vector pos = eye.position();
  Quaternion o = eye.orientation();
  translate(width / 2, height / 2);
  scale(1 / eye.magnitude(), 1 / eye.magnitude());
  rotate(-o.angle2D());
  translate(-pos.x(), -pos.y());
}
