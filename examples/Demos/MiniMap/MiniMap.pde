/**
 * Mini Map
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to use proscene off-screen rendering to build
 * a mini-map of the main Scene where all objetcs are interactive. It also
 * shows Frame syncing among views. 
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 * Press 'x' and 'y' to change the mini-map eye representation.
 */

import remixlab.proscene.*;

Scene scene, auxScene;
PGraphics canvas, auxCanvas;  
InteractiveFrame frame1, auxFrame1, frame2, auxFrame2, frame3, auxFrame3;
InteractiveFrame iFrame;

int w = 1110;
int h = 510;
int oW = w/3;
int oH = h/3;
int oX = w - oW;
int oY = h - oH;
boolean showMiniMap  = true;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P2D;

void settings() {
  size(w, h, renderer);
}

void setup() {
  canvas = createGraphics(w, h, renderer);
  scene = new Scene(this, canvas);
  frame1 = new InteractiveFrame(scene, "frameDrawing");
  frame1.translate(30, 30);
  frame2 = new InteractiveFrame(scene, frame1, "frameDrawing");
  frame2.translate(40, 0);
  frame3 = new InteractiveFrame(scene, frame2, "frameDrawing");
  frame3.translate(40, 0);

  auxCanvas = createGraphics(oW, oH, renderer);
  auxScene = new Scene(this, auxCanvas, oX, oY);
  auxScene.setVisualHints(0);
  auxScene.setRadius(200);
  auxScene.showAll();

  auxFrame1 = new InteractiveFrame(auxScene);
  auxFrame1.set(frame1);
  auxFrame2 = new InteractiveFrame(auxScene, auxFrame1);
  auxFrame2.set(frame2);
  auxFrame3 = new InteractiveFrame(auxScene, auxFrame2);
  auxFrame3.set(frame3);

  iFrame = new InteractiveFrame(auxScene);
  //to not scale the iFrame on mouse hover uncomment:
  //iFrame.setHighlightingMode(InteractiveFrame.HighlightingMode.NONE);
  iFrame.setWorldMatrix(scene.eyeFrame());
  iFrame.setShape(scene.eyeFrame());
}

void draw() {
  InteractiveFrame.sync(scene.eyeFrame(), iFrame);
  InteractiveFrame.sync(frame1, auxFrame1);
  InteractiveFrame.sync(frame2, auxFrame2);
  InteractiveFrame.sync(frame3, auxFrame3);
  scene.beginDraw();
  canvas.background(0);
  scene.drawFrames();
  scene.endDraw();
  scene.display();
  if (showMiniMap) {
    auxScene.beginDraw();
    auxCanvas.background(29, 153, 243);
    auxScene.pg().fill(255, 0, 255, 125);
    auxScene.drawFrames();
    auxScene.endDraw();
    auxScene.display();
  }
}

void keyPressed() {
  if (key == ' ')
    showMiniMap = !showMiniMap;
  if (key == 'x')
    iFrame.setShape("eyeDrawing");
  if (key == 'y')
    iFrame.setShape(scene.eyeFrame());
}

void frameDrawing(PGraphics pg) {
  pg.fill(random(0, 255), random(0, 255), random(0, 255));
  if (scene.is3D())
    pg.box(40, 10, 5);
  else
    pg.rect(0, 0, 40, 10, 5);
}

void eyeDrawing(PGraphics pg) {
  if (auxScene.is3D())
    pg.box(200);
  else {
    pg.pushStyle();
    pg.rectMode(CENTER);
    pg.rect(0, 0, 200, 200);
    pg.popStyle();
  }
}