/**
 * Window Culling.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates culling against the view-window boundary.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;

Scene scene, auxScene;
PGraphics canvas, auxCanvas;
float circleRadius = 150;

int w = 1110;
int h = 1110;

void settings() {
  size(w, h, P2D);
}

void setup() {
  canvas = createGraphics(w, h/2, P2D);
  scene = new Scene(this, canvas);

  scene.setRadius(200);
  scene.showAll();

  // enable computation of the boundary hyper-planes equations (disabled by default)
  scene.enableBoundaryEquations();
  scene.setGridVisualHint(false);

  auxCanvas = createGraphics(w, h/2, P2D);
  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  auxScene = new Scene(this, auxCanvas, 0, h/2);
  auxScene.setAxesVisualHint(false);
  auxScene.setGridVisualHint(false);
  auxScene.setRadius(400);
  auxScene.showAll();
}

void mainDrawing(Scene s) {
  PGraphics p = s.pg();
  p.background(0);
  p.noStroke();
  p.ellipseMode(RADIUS);
  // the main viewer camera is used to cull the sphere object against its frustum
  switch (scene.ballVisibility(new Vec(0, 0), circleRadius)) {
  case VISIBLE:
    p.fill(0, 255, 0);
    p.ellipse(0, 0, circleRadius, circleRadius);
    break;
  case SEMIVISIBLE:
    p.fill(255, 0, 0);
    p.ellipse(0, 0, circleRadius, circleRadius);
    break;
  case INVISIBLE:
    break;
  }
}

void auxiliarDrawing(Scene s) {
  mainDrawing(s);    
  s.pg().pushStyle();
  s.pg().stroke(255, 255, 0);
  s.pg().fill(255, 255, 0, 160);
  s.drawEye(scene.eye());
  s.pg().popStyle();
}

void draw() {
  scene.beginDraw();
  mainDrawing(scene);
  scene.endDraw();
  scene.display();

  auxScene.beginDraw();
  auxiliarDrawing(auxScene);
  auxScene.endDraw();
  auxScene.display();
}
