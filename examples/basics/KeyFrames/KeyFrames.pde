/**
 * KeyFrames.
 * by Jean Pierre Charalambos.
 *
 * This example introduces keyframes.
 *
 * Press ' ' to toggle the eye path display.
 * Press 's' to interpolate to fit the scene bounding volume.
 * Press 'f' to fit the scene bounding volume.
 * Press '1' and '2' to add eye key-frame to the eye paths.
 * Press 'a' and 'c' to play the eye paths.
 * Press 'b' and 'd' to remove the eye paths.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
boolean edit;
Node shape;

// Choose P3D or P2D
String renderer = P3D;
float speed = 1;

void setup() {
  size(1000, 700, renderer);
  scene = new Scene(this, 150);
  scene.enableHint(Scene.AXES | Scene.GRID);
  scene.configHint(Scene.GRID, color(0, 255, 0));
  scene.enableHint(Scene.BACKGROUND, color(125));
  scene.fit(1);
  PShape pshape;
  if (scene.is2D()) {
    rectMode(CENTER);
    pshape = createShape(RECT, 0, 0, 100, 100);
  } else {
    pshape = createShape(BOX, 30);
  }
  pshape.setFill(color(0, 255, 255, 125));
  shape = new Node(pshape);
  shape.enableHint(Node.AXES);
  shape.enableHint(Node.BULLSEYE);
  shape.configHint(Node.BULLSEYE, color(255, 0, 0));
  shape.setBullsEyeSize(50);
  shape.enableHint(Node.KEYFRAMES, Node.AXES, 2, color(0, 255, 0), 6);
  shape.setAnimationRecurrence(true);
  // Create an initial shape interpolator path
  for (int i = 0; i < random(4, 10); i++) {
    scene.randomize(shape);
    shape.addKeyFrame(Node.AXES | Node.SHAPE, i % 2 == 1 ? 1 : 4);
  }
  shape.animate();
  scene.eye().enableHint(Node.KEYFRAMES);
}

void draw() {
  scene.render();
}

void mouseMoved() {
  scene.updateMouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseShift();
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
  if (key == 's')
    shape.toggleAnimation();
  if (key == 'f')
    scene.fit(1);
}