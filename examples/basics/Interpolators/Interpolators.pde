/**
 * Interpolators.
 * by Jean Pierre Charalambos.
 *
 * This example introduces the three different node interpolations.
 *
 * Press ' ' to toggle the eye path display.
 * Press 's' to interpolate to fit the scene bounding volume.
 * Press 'f' to fit the scene bounding volume.
 * Press the arrow keys to move the camera.
 * Press '1' and '2' to add eye key-frame to the eye paths.
 * Press 'a' and 'c' to play the eye paths.
 * Press 'b' and 'd' to remove the eye paths.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Interpolator shapeInterpolator, eyeInterpolator;
boolean edit;
Node shape;

// Choose P3D or P2D
String renderer = P3D;

void setup() {
  size(1000, 700, renderer);
  scene = new Scene(this, 150);
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

  shapeInterpolator = new Interpolator(shape);
  shapeInterpolator.configHint(Interpolator.SPLINE, color(255));
  shapeInterpolator.enableRecurrence();
  // Create an initial shape interpolator path
  for (int i = 0; i < random(4, 10); i++) {
    shapeInterpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
  }
  shapeInterpolator.setSteps(1);
  shapeInterpolator.run();

  eyeInterpolator = new Interpolator(scene.eye());
  eyeInterpolator.configHint(Interpolator.SPLINE, color(255, 255, 0));

  scene.enableHint(Scene.AXES | Scene.GRID);
  scene.configHint(Scene.GRID, color(0, 255, 0));
  scene.enableHint(Scene.BACKGROUND, color(125));
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
    scene.mouseTranslate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if (scene.is3D())
    scene.moveForward(event.getCount() * 20);
  else
    scene.scaleEye(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ') {
    shapeInterpolator.toggleHint(Interpolator.SPLINE | Interpolator.STEPS);
    eyeInterpolator.toggleHint(Interpolator.SPLINE | Interpolator.STEPS);
  }
  if (key == '-' || key == '+') {
    shapeInterpolator.increaseSpeed(key == '+' ? 0.25 : -0.25);
  }

  if (key == '1') {
    eyeInterpolator.addKeyFrame();
  }
  if (key == 'a')
    eyeInterpolator.toggle();
  if (key == 'b')
    eyeInterpolator.clear();

  if (key == 's')
    scene.fit(1);
  if (key == 'f')
    scene.fit();
}
