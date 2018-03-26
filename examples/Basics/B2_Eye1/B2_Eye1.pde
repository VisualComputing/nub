/**
 * Eye1.
 * by Jean Pierre Charalambos.
 *
 * This example implements the following graph frame hierarchy:
 *
 *    World
 *      ^
 *      |\
 * frame1 eye
 *      |
 * frame2
 *      |
 * frame3
 *
 * Instantiate a graph object to use an eye frame.
 *
 * Press ' ' to toggle spinning.
 * Press 's' to fit scene smoothly.
 * press 'y' to change the eye spinning axis.
 */

import frames.timing.*;
import frames.core.*;
import frames.primitives.*;
import frames.processing.*;

Graph graph;
TimingTask spinningTask;
Frame frame1, frame2, frame3;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;
boolean yDirection;
int rotation;

void setup() {
  size(700, 700, renderer);
  graph = new Scene(this);
  spinningTask = new TimingTask() {
    public void execute() {
      spin();
    }
  };
  graph.registerTask(spinningTask);
  spinningTask.run(20);
  graph.setRadius(250);
  graph.fitBallInterpolation();
  frame1 = new Frame();
  frame2 = new Frame();
  frame2.setReference(frame1);
  frame3 = new Frame(frame2, new Vector(200, 200), new Quaternion());
}

// scene.applyTransformation does the same as apply(PMatrix), but:
// 1. It also works in 2D.
// 2. It's far more efficient (apply(PMatrix) computes the inverse).
void draw() {
  background(0);
  push();
  graph.applyTransformation(frame1);
  stroke(0, 255, 0);
  fill(255, 0, 255, 125);
  bola(100);
  push();
  graph.applyTransformation(frame2);
  push();
  graph.applyTransformation(frame3);
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

public void spin() {
  graph.eye().rotate(new Quaternion(yDirection ? new Vector(0, 1, 0) : new Vector(1, 0, 0), PI / 100), graph.anchor());
}

void keyPressed() {
  if (key == 's')
    graph.fitBallInterpolation();
  if (key == ' ')
    if (spinningTask.isActive())
      spinningTask.stop();
    else
      spinningTask.run(20);
  if (key == 'y')
    yDirection = !yDirection;
}