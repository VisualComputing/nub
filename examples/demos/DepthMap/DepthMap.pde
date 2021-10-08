/**
 * Depth Map.
 * by Jean Pierre Charalambos.
 *
 * This example shows how to generate and display a depth map from a light point-of-view.
 *
 * Refer also to the shadow mapping demo.
 *
 * Press shift and pick (mouse move) a box to define it as the light source.
 * Press shift and scroll (mouse wheel) the light source to change the shadow map zFar plane.
 * Press ' ' to change the shadow map volume (ORTHOGRAPHIC / PERSPECTIVE).
 * Press 'p' to toggle the scene perspective.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene.Type shadowMapType = Scene.Type.ORTHOGRAPHIC;
Scene scene, shadowMapScene;
Node[] shapes;
PGraphics shadowMap;
PShader depthShader;
float zNear = 50;
float zFar = 700;
int w = 1200;
int h = 1200;

void settings() {
  size(w, h, P3D);
}

void setup() {
  // main scene
  scene = new Scene(this, max(w, h));
  // shapes
  shapes = new Node[20];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node((PGraphics pg) -> {
      pg.pushStyle();
      if (pg == shadowMap)
      pg.noStroke();
      else {
        pg.strokeWeight(3);
        pg.stroke(0, 255, 255);
      }
      pg.fill(255, 0, 0);
      pg.box(80);
      pg.popStyle();
    });
    shapes[i].setHighlight(0);
    scene.randomize(shapes[i]);
  }
  // light
  scene.tag("light", shapes[(int) random(0, shapes.length - 1)]);
  scene.node("light").toggleHint(Node.SHAPE | Node.BOUNDS | Node.AXES);
  scene.node("light").setOrientation(Quaternion.from(Vector.plusK, scene.node("light").position()));
  // shadow map
  shadowMap = createGraphics(w / 2, h / 2, P3D);
  depthShader = loadShader("depth.glsl");
  depthShader.set("near", zNear);
  depthShader.set("far", zFar);
  shadowMap.shader(depthShader);
  // shadow map scene
  shadowMapScene = new Scene(shadowMap, scene.node("light"), zNear, zFar);
  shadowMapScene.togglePerspective();
  shadowMapScene.picking = false;
}

void draw() {
  // 1. Fill in and display front-buffer
  background(75, 25, 15);
  scene.render();
  // 2. Fill in shadow map using the light point of view
  if (scene.isTagValid("light")) {
    shadowMapScene.display(color(140, 160, 125), w / 2, h / 2);
  }
}

void mouseMoved(MouseEvent event) {
  if (event.isShiftDown()) {
    if (scene.isTagValid("light")) {
      scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
    }
    // no calling tag since we need to immediately update the tagged node
    scene.updateTag("light");
    if (scene.isTagValid("light")) {
      shadowMapScene.setEye(scene.node("light"));
      scene.node("light").toggleHint(Node.SHAPE | Node.AXES | Node.BOUNDS);
    }
  } else
    scene.tag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.moveForward(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (event.isShiftDown() && scene.isTagValid("light")) {
    depthShader.set("far", zFar += event.getCount() * 20);
    shadowMapScene.setZFar(() -> zFar);
  } else
    scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ' && scene.isTagValid("light")) {
    shadowMapScene.togglePerspective();
  }
  if (key == 'p')
    scene.togglePerspective();
}