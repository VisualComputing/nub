/**
 * DOF.
 * by Jean Pierre Charalambos.
 *
 * This example implements a Depth-Of-Field (DOF) shader effect
 * using the traverse(), traverse(PGraphics), display() and
 * display(PGraphics) Scene methods.
 *
 * Press 0 to display the original scene.
 * Press 1 to display a depth shader (which is used by DOF).
 * Press 2 to display the DOF effect.
 * Press 's' to save the eye.
 * Press 'l' to load the eye previously saved.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

PShader depthShader, dofShader;
PGraphics srcPGraphics, depthPGraphics, dofPGraphics;
Scene scene;
Shape[] models;
int mode = 2;

void setup() {
  size(1000, 800, P3D);
  colorMode(HSB, 255);
  srcPGraphics = createGraphics(width, height, P3D);
  scene = new Scene(this, srcPGraphics);
  scene.setFieldOfView(PI / 3);
  scene.setRadius(1000);
  scene.fitBallInterpolation();

  models = new Shape[100];

  for (int i = 0; i < models.length; i++) {
    models[i] = new Shape(scene, boxShape());
    scene.randomize(models[i]);
  }

  depthShader = loadShader("depth.glsl");
  depthShader.set("maxDepth", scene.radius() * 2);
  depthPGraphics = createGraphics(width, height, P3D);
  depthPGraphics.shader(depthShader);

  dofShader = loadShader("dof.glsl");
  dofShader.set("aspect", width / (float) height);
  dofShader.set("maxBlur", (float) 0.015);
  dofShader.set("aperture", (float) 0.02);
  dofPGraphics = createGraphics(width, height, P3D);
  dofPGraphics.shader(dofShader);

  frameRate(1000);
}

void draw() {
  // 1. Draw into main buffer
  scene.beginDraw();
  scene.frontBuffer().background(0);
  scene.traverse();
  scene.endDraw();

  // 2. Draw into depth buffer
  depthPGraphics.beginDraw();
  depthPGraphics.background(0);
  scene.traverse(depthPGraphics);
  depthPGraphics.endDraw();

  // 3. Draw destination buffer
  dofPGraphics.beginDraw();
  dofShader.set("focus", map(mouseX, 0, width, -0.5f, 1.5f));
  dofShader.set("tDepth", depthPGraphics);
  dofPGraphics.image(scene.frontBuffer(), 0, 0);
  dofPGraphics.endDraw();

  // display one of the 3 buffers
  if (mode == 0)
    scene.display();
  else if (mode == 1)
    scene.display(depthPGraphics);
  else
    scene.display(dofPGraphics);
}

PShape boxShape() {
  PShape box = createShape(BOX, 60);
  box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return box;
}

void keyPressed() {
  if (key == '0') mode = 0;
  if (key == '1') mode = 1;
  if (key == '2') mode = 2;
  if (key == 's') scene.saveConfig();
  if (key == 'l') scene.loadConfig();
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
    scene.zoom(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      scene.focus();
    else
      scene.align();
}
