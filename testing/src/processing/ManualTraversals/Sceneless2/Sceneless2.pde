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
Graph.Type type = Graph.Type.PERSPECTIVE;
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
    for (int i = 0; i < nodes.length; i++) {
      if ((i & 1) == 0) {
        nodes[i] = new Node() {
          @Override
          public void graphics(PGraphics pg) {
            pg.pushStyle();
            pg.strokeWeight(3);
            pg.stroke(255, 255, 0);
            pg.fill(0, 255, 0);
            if(pg.is3D())
              pg.box(20);
            else
              pg.square(0, 0, 20);
            pg.popStyle();
          }
        };
      } else {
        nodes[i] = new Node() {
          @Override
          public void graphics(PGraphics pg) {
            pg.pushStyle();
            pg.noStroke();
            pg.fill(255, 0, 255);
            if(pg.is3D())
              pg.sphere(20);
            else
              pg.circle(0, 0, 20);
            pg.popStyle();
          }
        };
      }
      nodes[i].randomize(new Vector(), 400, g.is3D());
    }
    rectMode(CENTER);
}

void draw() {
  background(0);
  eye.orbit(new Vector(0, g.is3D() ? 1 : 0, g.is3D() ? 0 : 1), 0.01);
  Graph.render(g, type, eye, g.width, g.height, zNear, zFar, leftHanded);
}