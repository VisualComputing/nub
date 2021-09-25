/**
 * Eye.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to setup and use an scene eye.
 *
 * Press '1' to add keyframes to the eye.
 * Press '2' to animate the eye along the keyframes.
 * Press '3' to remove all keyframes.
 */

import nub.processing.*;

Scene scene;

void setup() {
  size(800, 600, P3D);
  scene = new Scene(this, 400);
}

void draw() {
  background(0);
  stroke(0, 255, 0);
  strokeWeight(1);
  scene.drawGrid();
  strokeWeight(8);
  scene.drawAxes();
  lights();
  fill(255, 0, 0);
  rotateX(QUARTER_PI);
  stroke(255, 255, 0);
  strokeWeight(3);
  box(200);
  push();
  translate(0, 0, 180);
  rotateY(QUARTER_PI);
  fill(0, 0, 255);
  scene.drawTorusSolenoid();
  pop();
}

void mouseDragged() {
  if (mouseButton == LEFT) {
    scene.spin();
  }
  else if (mouseButton == RIGHT) {
    scene.shift();
  }
  else {
    scene.moveForward(scene.mouseDX());
  }
}

void keyPressed() {
  if (key == '1') {
    scene.eye().addKeyFrame();
  }
  if (key == '2') {
    scene.eye().animate();
  }
  if (key == '3') {
    scene.eye().removeKeyFrames();
  }
}