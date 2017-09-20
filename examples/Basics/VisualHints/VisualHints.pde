/**
 * Visual Hints.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates how to customize proscene visual hints look and feel.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.geom.*;

Scene scene;
InteractiveFrame iFrame;
boolean displayPaths = true;
Point fCorner = new Point();

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

public void setup() {
  size(640, 360, renderer);
  scene = new CustomizedScene(this);
  //do not overwrite the config file on dispose
  unregisterMethod("dispose", scene);
  iFrame = new InteractiveFrame(scene);
  iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
  iFrame.setGrabsInputThreshold(scene.radius()/4);
  iFrame.translate(30, -30, 0);
  scene.removeKeyBinding('r');
  scene.setKeyBinding('u', "togglePathsVisualHint");
  scene.setNonSeqTimers();
  scene.setVisualHints(Scene.AXES | Scene.GRID | Scene.PICKING | Scene.PATHS);
  scene.loadConfig();
}

public void draw() {
  background(40);
  fill(204, 102, 0, 150);
  scene.drawTorusSolenoid(2);

  // Save the current model view matrix
  pushMatrix();
  // Multiply matrix to get in the frame coordinate system.
  // applyMatrix(Scene.toPMatrix(iFrame.matrix())); //is possible but inefficient
  iFrame.applyTransformation();//very efficient
  // Draw an axis using the Scene static function
  scene.drawAxes(20);

  // Draw a second box
  if (scene.motionAgent().defaultGrabber() == iFrame) {
    fill(0, 255, 255);
    scene.drawTorusSolenoid(6, 10);
  }
  else if (iFrame.grabsInput()) {
    fill(255, 0, 0);
    scene.drawTorusSolenoid(8, 10);
  }
  else {
    fill(0, 0, 255, 150);
    scene.drawTorusSolenoid(6, 10);
  }
  popMatrix();
}

public void keyPressed() {
  if ( key == 'i')
    scene.inputHandler().shiftDefaultGrabber(scene.eyeFrame(), iFrame);
}

void mousePressed() {
  fCorner.set(mouseX, mouseY);
}

public class CustomizedScene extends Scene {
  // We need to call super(p) to instantiate the base class
  public CustomizedScene(PApplet p) {
    super(p);
  }

  @Override
  protected void drawPickingHint() {
    pg().pushStyle();
    pg().colorMode(RGB, 255);
    pg().strokeWeight(1);
    pg().stroke(100,220,100);
    drawPickingTargets();
    pg().popStyle();
  }
  
  @Override
  protected void drawZoomWindowHint() {
    pg().pushStyle();
    float p1x = fCorner.x();
    float p1y = fCorner.y();
    float p2x = mouseX;
    float p2y = mouseY;
    beginScreenDrawing();
    pg().stroke(0, 255, 255);
    pg().strokeWeight(2);
    pg().noFill();
    pg().beginShape();
    vertex(p1x, p1y);
    vertex(p2x, p1y);
    vertex(p2x, p2y);
    vertex(p1x, p2y);
    pg().endShape(CLOSE);
    endScreenDrawing();
    pg().popStyle();
  }
  
  @Override
  protected void drawScreenRotateHint() {
    pg().pushStyle();
    float p1x = mouseX;
    float p1y = mouseY;
    Vec p2 = eye().projectedCoordinatesOf(anchor());
    beginScreenDrawing();
    pg().stroke(255, 255, 0);
    pg().strokeWeight(2);
    pg().noFill();
    line(p2.x(), p2.y(), p1x, p1y);
    endScreenDrawing();
    pg().popStyle();
  }
  
  @Override
  protected void drawPathsHint() {
    pg().pushStyle();
    pg().colorMode(RGB, 255);
    pg().strokeWeight(1);
    pg().stroke(220,0,220);
    drawPaths();
    pg().popStyle();
  }
}