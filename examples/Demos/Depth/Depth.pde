/**
 * Depth.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to attach a PShape to an interactive frame.
 * PShapes attached to interactive frames can then be automatically picked
 * and easily drawn.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

PShader shader;
Scene scene;
boolean original = true;
float posns[];
InteractiveFrame[] models;

void setup() {
  size(900, 900, P3D);
  colorMode(HSB, 255);
  posns = new float[300];
  for (int i = 0; i<100; i++) {
    posns[3*i]=random(-1000, 1000);
    posns[3*i+1]=random(-1000, 1000);
    posns[3*i+2]=random(-1000, 1000);
  }

  scene = new Scene(this);
  models = new InteractiveFrame[100];

  for (int i = 0; i < models.length; i++) {
    models[i] = new InteractiveFrame(scene, drawBox());
    models[i].translate(posns[3*i], posns[3*i+1], posns[3*i+2]);
  }

  scene.setRadius(1000);
  scene.showAll();

  shader = loadShader("depth.glsl");
  shader.set("maxDepth", scene.radius()*2);

  frameRate(1000);
}

void draw() {
  background(0);
  if (original)
    scene.drawFrames();
  else {
    scene.pg().shader(shader);
    scene.drawFrames();
    scene.pg().resetShader(); 
  }
}

PShape drawBox() {
  PShape box = createShape(BOX, 60);
  box.setFill(color(random(0,255), random(0,255), random(0,255)));
  return box;
}

void keyPressed() {
  original = !original;
}
