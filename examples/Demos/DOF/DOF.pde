/**
 * DOF.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to attach a PShape to an interactive frame.
 * PShapes attached to interactive frames can then be automatically picked
 * and easily drawn.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

PShader depthShader, dofShader;
PGraphics srcPGraphics, depthPGraphics, dofPGraphics;
Scene scene;
float posns[];
InteractiveFrame[] models;
int mode = 2;

void setup() {
  size(900, 900, P3D);
  colorMode(HSB, 255);
  posns = new float[300];
  for (int i = 0; i<100; i++) {
    posns[3*i]=random(-1000, 1000);
    posns[3*i+1]=random(-1000, 1000);
    posns[3*i+2]=random(-1000, 1000);
  }

  srcPGraphics = createGraphics(width, height, P3D);
  scene = new Scene(this, srcPGraphics);
  models = new InteractiveFrame[100];

  for (int i = 0; i < models.length; i++) {
    models[i] = new InteractiveFrame(scene, boxShape());
    models[i].translate(posns[3*i], posns[3*i+1], posns[3*i+2]);
  }

  scene.setRadius(1000);
  scene.showAll();

  depthShader = loadShader("depth.glsl");
  depthShader.set("maxDepth", scene.radius()*2);
  depthPGraphics = createGraphics(width, height, P3D);
  depthPGraphics.shader(depthShader);

  dofShader = loadShader("dof.glsl");
  dofShader.set("aspect", width / (float) height);
  dofShader.set("maxBlur", 0.015);  
  dofShader.set("aperture", 0.02);
  dofPGraphics = createGraphics(width, height, P3D);
  dofPGraphics.shader(dofShader);

  frameRate(1000);
}

void draw() {
  // 1. Draw into main buffer
  scene.beginDraw();
  scene.pg().background(0);
  scene.drawFrames();
  scene.endDraw();

  // 2. Draw into depth buffer
  depthPGraphics.beginDraw();
  depthPGraphics.background(0);
  scene.drawFrames(depthPGraphics);
  depthPGraphics.endDraw();

  // 3. Draw destination buffer
  dofPGraphics.beginDraw();
  dofShader.set("focus", map(mouseX, 0, width, -0.5f, 1.5f));
  dofShader.set("tDepth", depthPGraphics);    
  dofPGraphics.image(scene.pg(), 0, 0);
  dofPGraphics.endDraw();

  // display one of the 3 buffers
  if (mode==0)
    scene.display();
  else if (mode==1)
    scene.display(depthPGraphics);
  else
    scene.display(dofPGraphics);
}

PShape boxShape() {
  PShape box = createShape(BOX, 60);
  box.setFill(color(random(0,255), random(0,255), random(0,255)));
  return box;
}

void keyPressed() {
  if ( key=='0') mode = 0;
  if ( key=='1') mode = 1;
  if ( key=='2') mode = 2;
}
