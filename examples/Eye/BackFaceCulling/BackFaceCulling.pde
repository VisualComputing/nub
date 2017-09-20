/**
 * Back-Face Culling.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates various back face camera culling routines to early
 * discard primitive processing.
 * 
 * Press 'c' to switch between different back-face culling conditions
 * (cone or on a face by face basis).
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;

float            size        = 50;
Scene            scene, auxScene;
PGraphics        canvas, auxCanvas;

Vec              normalXPos  = new Vec(1, 0, 0);
Vec              normalYPos  = new Vec(0, 1, 0);
Vec              normalZPos  = new Vec(0, 0, 1);
Vec              normalXNeg  = new Vec(-1, 0, 0);
Vec              normalYNeg  = new Vec(0, -1, 0);
Vec              normalZNeg  = new Vec(0, 0, -1);
ArrayList<Vec>   normals;

boolean facebyface = true;

int w = 1110;
int h = 1110;

void settings() {
  size(w, h, P3D);
}

void setup() {
  normals = new ArrayList<Vec>();
  normals.add(normalZPos);
  normals.add(normalXPos);
  normals.add(normalYPos);

  canvas = createGraphics(w, h/2, P3D);
  scene = new Scene(this, canvas);
  scene.setGridVisualHint(false);

  auxCanvas = createGraphics(w, h/2, P3D);
  // Note that we pass the upper left corner coordinates where the
  // scene is to be drawn (see drawing code below) to its constructor.
  auxScene = new Scene(this, auxCanvas, 0, h/2);
  auxScene.camera().setType(Camera.Type.ORTHOGRAPHIC);
  auxScene.setAxesVisualHint(false);
  auxScene.setGridVisualHint(false);
  auxScene.setRadius(350);
  auxScene.camera().setPosition(new Vec(125, 125, 125));
  auxScene.camera().lookAt(auxScene.center());
  colorMode(RGB, 1);
}

void draw() {
  scene.beginDraw();
  mainDrawing(scene.pg());
  scene.endDraw();
  scene.display();

  auxScene.beginDraw();
  auxiliarDrawing(auxScene.pg());
  auxScene.endDraw();
  auxScene.display();
}

void mainDrawing(PGraphics pg) {
  pg.background(0);
  drawScene(pg);
}

void auxiliarDrawing(PGraphics pg) {
  mainDrawing(pg);
  pg.pushStyle();
  pg.fill(0, 255, 255, 120);
  pg.stroke(0, 255, 255);
  auxScene.drawEye(scene.eye());
  pg.popStyle();
}

void drawScene(PGraphics pg) {
  pg.noStroke();
  pg.beginShape(QUADS);

  //1. Attempt to discard three faces at once using a cone of normals.
  if (facebyface || coneCondition()) {
    // z-axis
    if (zCondition() || !facebyface) {
      pg.fill(0, size, size);
      pg.vertex(-size, size, size);
      pg.fill(size, size, size);
      pg.vertex(size, size, size);
      pg.fill(size, 0, size);
      pg.vertex(size, -size, size);
      pg.fill(0, 0, size);
      pg.vertex(-size, -size, size);
    }

    // x-axis
    if (xCondition() || !facebyface) {
      pg.fill(size, size, size);
      pg.vertex(size, size, size);
      pg.fill(size, size, 0);
      pg.vertex(size, size, -size);
      pg.fill(size, 0, 0);
      pg.vertex(size, -size, -size);
      pg.fill(size, 0, size);
      pg.vertex(size, -size, size);
    }

    // y-axis
    if (yCondition() || !facebyface) {
      pg.fill(0, size, 0);
      pg.vertex(-size, size, -size);
      pg.fill(size, size, 0);
      pg.vertex(size, size, -size);
      pg.fill(size, size, size);
      pg.vertex(size, size, size);
      pg.fill(0, size, size);
      pg.vertex(-size, size, size);
    }
  } // cone condition

  //2. Attempt to discard a single face using one of its vertices and its normal.
  // -z-axis
  if (!scene.camera().isFaceBackFacing(new Vec(size, size, -size), normalZNeg)) {
    pg.fill(size, size, 0);
    pg.vertex(size, size, -size);
    pg.fill(0, size, 0);
    pg.vertex(-size, size, -size);
    pg.fill(0, 0, 0);
    pg.vertex(-size, -size, -size);
    pg.fill(size, 0, 0);
    pg.vertex(size, -size, -size);
  }

  //3. Attempt to discard a single face using three of its vertices. 
  // -x-axis
  if (scene.camera().isFaceFrontFacing(new Vec(-size, size, -size), new Vec(-size, -size, -size), 
    new Vec(-size, -size, size))) {
    pg.fill(0, size, 0);
    pg.vertex(-size, size, -size);
    pg.fill(0, size, size);
    pg.vertex(-size, -size, -size);
    pg.fill(0, 0, size);
    pg.vertex(-size, -size, size);
    pg.fill(0, 0, 0);
    pg.vertex(-size, size, size);
  }

  //4. Attempt to discard a single face using one of its vertices and its normal.
  // -y-axis
  if (scene.camera().isFaceFrontFacing(new Vec(-size, -size, -size), normalYNeg)) {
    pg.fill(0, 0, 0);
    pg.vertex(-size, -size, -size);
    pg.fill(size, 0, 0);
    pg.vertex(size, -size, -size);
    pg.fill(size, 0, size);
    pg.vertex(size, -size, size);
    pg.fill(0, 0, size);
    pg.vertex(-size, -size, size);
  }

  pg.endShape();
}

boolean xCondition() {
  return scene.camera().isFaceFrontFacing(new Vec(size, size, size), normalXPos);
}

boolean yCondition() {
  return scene.camera().isFaceFrontFacing(new Vec(-size, size, -size), normalYPos);
}

boolean zCondition() {
  return scene.camera().isFaceFrontFacing(new Vec(-size, size, size), normalZPos);
}

boolean coneCondition() {
  return scene.camera().isConeFrontFacing(new Vec(size, size, size), normals);
}

void keyPressed() {
  if (key == 'u')
    scene.flip();
  if (key == 'c')
    facebyface = !facebyface;
}
