/**
 * ShadowMapping.
 * by Jean Pierre Charalambos.
 *
 * This example shows how to generate and display a shadow map volume,
 * from a light point-of-view.
 *
 * Press ' ' to toggle the shadow map display.
 * Press ' ' to change the shadow map shape (ORTHOGRAPHIC / PERSPECTIVE).
 */

import frames.core.*;
import frames.processing.*;

Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
Scene scene;
Shape[] shapes;
Shape light;
boolean show = true;
PGraphics shadowMap;
float zNear = 50;
float zFar = 1000;
int w = 1000;
int h = 1000;

void settings() {
  size(w, h, P3D);
}

void setup() {
  scene = new Scene(this);
  scene.setRadius(max(w, h));
  shapes = new Shape[20];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Shape(scene);
    shapes[i].setGraphics(caja());
    shapes[i].randomize();
  }
  light = new Shape(scene) {
    @Override
    public void setGraphics(PGraphics pg) {
      pg.pushStyle();
      Scene.drawAxes(pg, 150);
      pg.fill(isTracked() ? 255 : 25, isTracked() ? 0 : 255, 255);
      Scene.drawEye(pg, shadowMap, shadowMapType, this, zNear, zFar);
      pg.popStyle();
    }
  };
  scene.fitBallInterpolation();
  shadowMap = createGraphics(w / 2, h / 2, P3D);
}

void draw() {
  background(90, 80, 125);
  // 1. Fill in and display front-buffer
  scene.traverse();
  // 2. Fill in shadow map using the light point of view
  shadowMap.beginDraw();
  shadowMap.background(120);
  scene.traverse(shadowMap, shadowMapType, light, zNear, zFar);
  shadowMap.endDraw();
  // 3. Display shadow map
  if (show) {
    scene.beginHUD();
    image(shadowMap, w / 2, h / 2);
    scene.endHUD();
  }
}

void mouseMoved() {
  scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.moveForward(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.scale(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ')
    show = !show;
  if (key == 't')
    shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
}

PShape caja() {
  PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}