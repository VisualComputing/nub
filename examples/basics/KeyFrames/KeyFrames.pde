/**
 * KeyFrames.
 * by Jean Pierre Charalambos.
 *
 * This example introduces node keyframes.
 *
 * Press ' ' to toggle the shape end eye keyframes hint.
 * Press 's' to toggle the shape animation.
 * Press '+' to speed up the shape animation.
 * Press '-' to speed down the shape animation.
 * Press 'f' to interpolate the eye to fit the scene bounding volume.
 * Press '1' to add key-frame to the eye path.
 * Press 'a' to play the eye path.
 * Press 'b' to remove the eye path.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node shape;

// Choose P3D or P2D
String renderer = P3D;
float speed = 1;

void setup() {
  size(1000, 700, renderer);
  scene = new Scene(this, 150);
  PShape pshape;
  if (scene.is2D()) {
    rectMode(CENTER);
    pshape = createShape(RECT, 0, 0, 100, 100);
  } else {
    pshape = createShape(BOX, 30);
  }
  pshape.setFill(color(0, 255, 255, 125));
  shape = new Node(pshape);
  shape.setMinMaxScalingFilter(0.8, 1.2);
  shape.setHUD(this::hud);
  shape.enableHint(Node.AXES);
  shape.enableHint(Node.BULLSEYE);
  shape.configHint(Node.BULLSEYE, color(255, 0, 0));
  shape.setBullsEyeSize(50);
  shape.enableHint(Node.KEYFRAMES, Node.AXES, 2, color(0, 255, 0), 6);
  shape.setAnimationRecurrence(true);
  // Create an initial shape interpolator path
  for (int i = 0; i < random(4, 10); i++) {
    scene.randomize(shape);
    shape.addKeyFrame(Node.AXES | Node.SHAPE | Node.HUD, i % 2 == 1 ? 1 : 4);
  }
  shape.animate();
  scene.eye().enableHint(Node.KEYFRAMES);
}

void hud(PGraphics pg) {
  pg.pushStyle();
  pg.rectMode(CENTER);
  pg.fill(255, 0, 255, 125);
  pg.stroke(0,0,255);
  pg.strokeWeight(3);
  pg.rect(0, 0, 80, 50);
  pg.popStyle();
}

void draw() {
  background(125);
  scene.render();
  stroke(255, 255, 0);
  scene.drawGrid();
}

void mouseMoved() {
  scene.updateTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.zoom(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 20);
  else
    scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ') {
    shape.toggleHint(Node.KEYFRAMES);
    scene.eye().toggleHint(Node.KEYFRAMES);
  }
  if (key == 's')
    shape.toggleAnimation();
  if (key == '-' || key == '+') {
    speed += key == '+' ? 0.25f : -0.25f;
    shape.animate(speed);
  }
  if (key == '1')
    scene.eye().addKeyFrame(Node.CAMERA | Node.BULLSEYE, 1);
  if (key == 'a')
    scene.eye().toggleAnimation();
  if (key == 'b')
    scene.eye().removeKeyFrames();
  if (key == 'f')
    scene.fit(2);
}