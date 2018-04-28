/**
 * InteractiveNodes.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates the following frame hierarchy:
 *
 * World
 *   ^
 *   |
 * node1
 *   |
 * node2
 */

import frames.core.*;
import frames.primitives.*;
import frames.processing.*;
import frames.input.*;

Node node1, node2;
Graph graph;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;
float length = 100;

void setup() {
  size(700, 700, renderer);
  graph = new Scene(this);
  // node instantation requires a graph or a (reference) node.
  // Inner classes are used to define what is going to happen during traversal
  node1 = new OrbitNode(graph) {
    @Override
    public void visit() {
      pushStyle();
      stroke(0, 255, 0);
      fill(255, 0, 255, 125);
      float dim = graph.isInputNode(this) ? length*1.1 : length;
      if (graph().is3D())
        sphere(dim);
      else
        ellipse(0, 0, dim, dim);
      popStyle();
    }
  };
  node2 = new OrbitNode(node1) {
    @Override
    public void visit() {
      pushStyle();
      stroke(255, 0, 0);
      fill(0, 255, 255);
      float dim = graph.isInputNode(this) ? length*1.1 : length;
      if (graph().is3D())
        box(dim);
      else
        rect(0, 0, dim, dim);
      popStyle();
    }
  };
  node2.translate(new Vector(250, 250));
  graph.setRadius(300);
  graph.fitBallInterpolation();
}

void draw() {
  background(0);
  // traversal algorithm visit all nodes belonging to the graph
  graph.traverse();
}
