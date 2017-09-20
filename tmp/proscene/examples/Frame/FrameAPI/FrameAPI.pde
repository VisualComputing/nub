/**
 * Frame API.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates the powerful Frame API used to convert points and
 * vectors along a frame hierarchy. The following frame hierarchy is implemented:
 *
 *  world
 *  ^
 *  |\
 *  | \  
 *  f1 eye
 *  ^   ^
 *  |\   \
 *  | \   \
 *  f2 f3  f5
 *  ^
 *  |
 *  |
 *  f4
 *
 * Press the space bar to browse the different conversion methods shown here.
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;

Scene scene;
InteractiveFrame f1, f2, f3, f4, f5;
Vec pnt = new Vec(40, 30, 20);
Vec vec	= new Vec(50, 50, 50);
PFont font16, font13;
Mode mode;
color wColor = color(255, 255, 255);
color f1Color = color(255, 0, 0);
color f2Color = color(0, 255, 0);
color f3Color = color(0, 0, 255);
color f4Color = color(255, 0, 255);
color f5Color = color(255, 255, 0);

void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);
  scene.setVisualHints(Scene.AXES | Scene.PICKING);
  mode = Mode.m1;

  f1 = new InteractiveFrame(scene);
  f1.translate(-50, -20, 30);
  f1.scale(1.3);

  f2 = new InteractiveFrame(scene, f1);
  f2.translate(60, -40, -30);
  f2.scale(1.2);

  f3 = new InteractiveFrame(scene, f1);
  f3.translate(60, 55, -30);
  f3.rotate(new Quat(new Vec(0, 1, 0), -HALF_PI));
  f3.scale(1.1);

  f4 = new InteractiveFrame(scene, f2);
  f4.translate(60, -55, 30);
  f4.rotate(new Quat(new Vec(0, 1, 0), QUARTER_PI));
  f4.scale(0.9);

  f5 = new InteractiveFrame(scene, scene.eyeFrame());
  f5.translate(-100, 0, -250);

  scene.setRadius(200);
  scene.showAll();
  scene.eyeFrame().setRotationSensitivity(1.3f);

  font16 = loadFont("FreeSans-16.vlw");
  font13 = loadFont("FreeSans-13.vlw");
}

void draw() {
  background(0);

  //world:
  drawPoint(wColor);

  pushMatrix();
  scene.applyTransformation(f1);
  scene.drawAxes(40);
  drawPoint(f1Color);
  pushMatrix();
  scene.applyTransformation(f3);
  scene.drawAxes(40);
  drawPoint(f3Color);
  popMatrix();
  pushMatrix();
  scene.applyTransformation(f2);
  scene.drawAxes(40);
  drawPoint(f2Color);
  pushMatrix();
  scene.applyTransformation(f4);
  scene.drawAxes(40);
  drawPoint(f4Color);
  popMatrix();
  popMatrix();
  popMatrix();

  //eye
  pushMatrix();
  scene.applyTransformation(scene.eyeFrame());
  pushMatrix();
  scene.applyTransformation(f5);
  scene.drawAxes(40);
  drawPoint(f5Color);
  popMatrix();
  popMatrix();

  drawMode();
  displayText();
}

void drawMode() {
  // points
  pushStyle();
  noStroke();
  fill(0, 255, 255);
  switch (mode) {
  case m1: // f2 -> world
    drawArrowConnectingPoints(f2.inverseCoordinatesOf(pnt));
    break;
  case m2: // f2 -> f1
    drawArrowConnectingPoints(f1, f2.coordinatesOfIn(pnt, f1));
    break;
  case m3: // f1 -> f2
    drawArrowConnectingPoints(f2, f2.localCoordinatesOf(pnt));
    break;
  case m4: // f3 -> f4
    drawArrowConnectingPoints(f4, f4.coordinatesOfFrom(pnt, f3));
    break;
  case m5: // f4 -> f3
    drawArrowConnectingPoints(f3, f4.coordinatesOfIn(pnt, f3));
    break;
  case m6: // f5 -> f4
    drawArrowConnectingPoints(f4, f5.coordinatesOfIn(pnt, f4));
    break;
  }
  popStyle();

  // vectors
  pushStyle();
  noStroke();
  fill(125);
  switch (mode) {
  case m1: // f2 -> world
    drawVector(f2, vec);
    drawVector(f2.inverseTransformOf(vec));
    break;
  case m2: // f2 -> f1
    drawVector(f2, vec);
    drawVector(f1, f2.transformOfIn(vec, f1));
    break;
  case m3: // f1 -> f2
    drawVector(f1, vec);
    drawVector(f2, f2.localTransformOf(vec));
    break;
  case m4: // f3 -> f4
    drawVector(f3, vec);
    drawVector(f4, f4.transformOfFrom(vec, f3));
    break;
  case m5: // f4 -> f3
    drawVector(f4, vec);
    drawVector(f3, f4.transformOfIn(vec, f3));
    break;
  case m6: // f5 -> f4
    drawVector(f5, vec);
    drawVector(f4, f5.transformOfIn(vec, f4));
    break;
  }
  popStyle();
}

void displayText() {
  pushStyle();
  Vec pos;
  scene.beginScreenDrawing();
  textFont(font13);
  fill(f1Color);
  pos = scene.eye().projectedCoordinatesOf(f1.position());
  text("Frame 1", pos.x(), pos.y());
  fill(f2Color);
  pos = scene.eye().projectedCoordinatesOf(f2.position());
  text("Frame 2", pos.x(), pos.y());
  fill(f3Color);
  pos = scene.eye().projectedCoordinatesOf(f3.position());
  text("Frame 3", pos.x(), pos.y());
  fill(f4Color);
  pos = scene.eye().projectedCoordinatesOf(f4.position());
  text("Frame 4", pos.x(), pos.y());
  fill(f5Color);
  pos = scene.eye().projectedCoordinatesOf(f5.position());
  text("Frame 5", pos.x(), pos.y());
  fill(wColor);
  textFont(font16);
  text("Press the space bar to change mode", 5, 15);
  switch (mode) {
  case m1: // f2 -> world
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 2 to world", 5, 35);
    break;
  case m2: // f2 -> f1
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 2 to frame 1", 5, 35);
    break;
  case m3: // f1 -> f2
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 1 to frame 2", 5, 35);
    break;
  case m4: // f3 -> f4
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 3 to frame 4", 5, 35);
    break;
  case m5: // f4 -> f3
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 4 to frame 3", 5, 35);
    break;
  case m6: // f5 -> f4
    text("Converts vectors (grey arrows) and points (see the cyan arrow) from frame 5 to frame 4", 5, 35);
    break;
  }
  scene.endScreenDrawing();
  popStyle();
}

void drawPoint(int c) {
  pushStyle();
  stroke(c);
  strokeWeight(10);
  point(pnt.x(), pnt.y(), pnt.z());
  popStyle();
}

void drawArrowConnectingPoints(Vec to) {
  drawArrow(null, pnt, to);
}

void drawArrowConnectingPoints(Frame frame, Vec to) {
  drawArrow(frame, pnt, to);
}

void drawVector(Vec to) {
  drawArrow(null, new Vec(), to);
}

void drawVector(Frame frame, Vec to) {
  drawArrow(frame, new Vec(), to);
}

void drawArrow(Frame frame, Vec from, Vec to) {
  if (frame != null) {
    pushMatrix();
    //scene.applyModelView(frame.worldMatrix());// world, is handy but inefficient
    scene.applyWorldTransformation(frame);
    scene.drawArrow(from, to, 1);
    popMatrix();
  } else
    scene.drawArrow(from, to, 1);
}

void keyPressed() {
  if (key == ' ') //<>//
    switch (mode) {
    case m1:
      mode = Mode.m2;
      break;
    case m2:
      mode = Mode.m3;
      break;
    case m3:
      mode = Mode.m4;
      break;
    case m4:
      mode = Mode.m5;
      break;
    case m5:
      mode = Mode.m6;
      break;
    case m6:
      mode = Mode.m1;
      break;
    }
  if (key == 'v' || key == 'V')
    scene.flip();
  if (key == '+')
    scene.eyeFrame().setScaling(scene.eyeFrame().scaling() * 1.1f);
  if (key == '-')
    scene.eyeFrame().setScaling(scene.eyeFrame().scaling() / 1.1f);
}