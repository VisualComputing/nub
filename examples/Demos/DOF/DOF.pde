/**
 * DOF.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to attach a PShape to an interactive frame.
 * PShapes attached to interactive frames can then be automatically picked
 * and easily drawn.
 */


import frames.core.*;
import frames.processing.*;

PShader depthShader, dofShader;
PGraphics srcPGraphics, depthPGraphics, dofPGraphics;
Scene scene;
OrbitShape[] models;
int mode = 2;

void setup() {
  size(1000, 800, P3D);
  colorMode(HSB, 255);
  srcPGraphics = createGraphics(width, height, P3D);
  scene = new Scene(this, srcPGraphics);
  OrbitShape eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setFieldOfView(PI / 3);
  //interactivity defaults to the eye
  scene.setDefaultGrabber(eye);
  scene.setRadius(1000);
  scene.fitBallInterpolation();

  models = new OrbitShape[100];

  for (int i = 0; i < models.length; i++) {
    models[i] = new OrbitShape(scene);
    models[i].set(boxShape());
    models[i].randomize();
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
}