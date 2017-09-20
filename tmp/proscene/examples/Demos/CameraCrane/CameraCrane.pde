/**
 * CameraCrane.
 * by Jean Pierre Charalambos, Ivan Dario Chinome, and David Montanez.
 * 
 * This example illustrates "frame syncing" by implementing two camera 
 * cranes which defines two auxiliary point of views of the same scene.
 * 
 * When syncing two frames they will share their state (postion, orientation
 * and scaling) taken the one that has been most recently updated. Syncing
 * should always been called within draw().
 *
 * Press 'f' to display frame selection hints.
 * Press 'l' to enable lighting.
 * Press 'x' to draw the camera frustum volumes.
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.*;

boolean enabledLights = true;
boolean drawRobotCamFrustum = false;
ArmCam armCam;
HeliCam heliCam;
PGraphics canvas, armCanvas, heliCanvas;
Scene mainScene, armScene, heliScene;
int mainWinHeight = 400; // should be less than the PApplet height
PShape teapot;

public void setup() {
  size(1024, 720, P3D);
  teapot = loadShape("teapot.obj");

  canvas = createGraphics(width, mainWinHeight, P3D);
  mainScene = new Scene(this, (PGraphics3D) canvas);
  mainScene.setGridVisualHint(false);
  mainScene.setAxesVisualHint(false);
  mainScene.setRadius(110);
  mainScene.showAll();
  // press 'f' to display frame selection hints

  armCanvas = createGraphics(width / 2, (height - canvas.height), P3D);
  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  armScene = new Scene(this, (PGraphics3D) armCanvas, 0, canvas.height);
  armScene.setRadius(50);
  armScene.setGridVisualHint(false);
  armScene.setAxesVisualHint(false);
  heliCanvas = createGraphics(width / 2, (height - canvas.height), P3D);
  // Note that we pass the upper left corner coordinates where the scene
  // is to be drawn (see drawing code below) to its constructor.
  heliScene = new Scene(this, (PGraphics3D) heliCanvas, canvas.width / 2, 
  canvas.height);
  heliScene.setRadius(50);
  heliScene.setGridVisualHint(false);
  heliScene.setAxesVisualHint(false);

  // Eyes initial setup
  armCam = new ArmCam(60, -60, 2);
  heliCam = new HeliCam();

  heliScene.camera().frame().setWorldMatrix(heliCam.frame(3));
  armScene.camera().frame().setWorldMatrix(armCam.frame(5));  

  armScene.eyeFrame().setMotionBinding(LEFT, "lookAround");
  armScene.eyeFrame().setMotionBinding(CENTER, null);
  armScene.eyeFrame().removeMotionBinding(CENTER);
  armScene.eyeFrame().setMotionBinding(RIGHT, null);
  armScene.eyeFrame().removeMotionBinding(RIGHT);
  heliScene.eyeFrame().setMotionBinding(LEFT, "lookAround");
  heliScene.eyeFrame().setMotionBinding(CENTER, null);//same as removeMotionBinding
  heliScene.eyeFrame().removeMotionBinding(CENTER);
  heliScene.eyeFrame().setMotionBinding(RIGHT, null);//same as removeMotionBinding
  heliScene.eyeFrame().removeMotionBinding(RIGHT);
}

// off-screen rendering
public void draw() {
  InteractiveFrame.sync(armScene.camera().frame(), armCam.frame(5));
  InteractiveFrame.sync(heliScene.camera().frame(), heliCam.frame(3));
  mainScene.beginDraw();
  drawing(mainScene);
  mainScene.endDraw();
  mainScene.display();

  armScene.beginDraw();
  drawing(armScene);
  armScene.endDraw();
  armScene.display();

  heliScene.beginDraw();
  drawing(heliScene);
  heliScene.endDraw();
  heliScene.display();
}

// the actual drawing function, shared by the two scenes
public void drawing(Scene scn) {
  PGraphics pg3d = scn.pg();
  pg3d.background(0);
  if (enabledLights) {
    pg3d.lights();
  }
  // 1. draw the robot cams

  if (scn!=armScene) armCam.draw(scn);
  if (scn!=heliScene) heliCam.draw(scn);

  // 2. draw the scene

  // Rendering of the OBJ model
  pg3d.noStroke();
  pg3d.fill(24, 184, 199);
  pg3d.pushMatrix();
  pg3d.translate(0, 0, 20);
  pg3d.scale(2.5f);
  pg3d.rotateX(HALF_PI);

  pg3d.shape(teapot);
  pg3d.popMatrix();

  // 2a. draw a ground
  pg3d.noStroke();
  pg3d.fill(120, 120, 120);
  float nbPatches = 100;
  pg3d.normal(0.0f, 0.0f, 1.0f);
  for (int j = 0; j < nbPatches; ++j) {
    pg3d.beginShape(QUAD_STRIP);
    for (int i = 0; i <= nbPatches; ++i) {
      pg3d.vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
      pg3d.vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
    }
    pg3d.endShape();
  }
}

public void keyPressed() {
  if (key == 'l') {
    enabledLights = !enabledLights;
    if (enabledLights) {
      println("camera spot lights enabled");
    } else {
      println("camera spot lights disabled");
    }
  }
  if (key == 'x') {
    drawRobotCamFrustum = !drawRobotCamFrustum;
    if (drawRobotCamFrustum) {
      println("draw robot camera frustums");
    } else {
      println("don't draw robot camera frustums");
    }
  }
}