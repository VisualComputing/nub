/**
 * Interpolation1.
 * by Jean Pierre Charalambos.
 *
 * This example introduces the three different interpolations offered
 * by the Graph.
 *
 * Press ' ' to toggle the path display.
 * Press 's' to fit ball interpolation.
 * Press 'f' to fit ball.
 * Press 't' to shift timers.
 * Press the arrow keys to move the camera.
 */

import frames.input.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
PShape pbox, psphere;
Shape box, sphere;
Interpolator interpolator;
OrbitNode eye;
boolean showPath = true;

//Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
String renderer = P2D;

void setup() {
  size(1000, 800, renderer);
  rectMode(CENTER);
  scene = new Scene(this);
  eye = new OrbitNode(scene);
  eye.setDamping(0);
  scene.setEye(eye);
  //interactivity defaults to the eye
  scene.setDefaultNode(eye);
  scene.setRadius(150);

  // interpolation 1. Default eye interpolations
  scene.fitBallInterpolation();

  //PShape pshape = scene.is3D() ? createShape(SPHERE, 10) : createShape(RECT, 0,0,50,50);
  pbox = scene.is3D() ? createShape(BOX, 10) : createShape(RECT, 0, 0, 50, 50);
  pbox.setFill(color(50, 250, 80));
  box = new Shape(scene, pbox);

  psphere = scene.is3D() ? createShape(SPHERE, 10) : createShape(ELLIPSE, 0, 0, 50, 50);
  psphere.setFill(color(250, 50, 80));
  sphere = new Shape(box, psphere);
  sphere.translate(15, 15, 15);

  // interpolation 2. Custom interpolations
  interpolator = new Interpolator(box);
  interpolator.setLoop();
  // Create an initial path

  // Several options:
  // 1. Using frames:
  //for (int i = 0; i < random(4, 10); i++)
  //  interpolator.addKeyFrame(Frame.random(scene.center(), scene.radius()));


  // 2. Using nodes:
  //for (int i = 0; i < random(4, 10); i++)
  // interpolator.addKeyFrame(Node.random(scene));

  // 3. Using OrbitNodes, which is the same as 2., but makes path editable
  for (int i = 0; i < random(4, 10); i++) {
    Node node = new OrbitNode(scene);
    node.randomize();
    interpolator.addKeyFrame(node);
  }
  interpolator.start();
}

void draw() {
  background(0);
  scene.traverse();
  if (showPath) {
    pushStyle();
    fill(255, 0, 0);
    stroke(0, 255, 0);
    scene.drawPath(interpolator, 5);
    popStyle();
  }
}

void keyPressed() {
  if (key == ' ')
    showPath = !showPath;
  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'f')
    scene.fitBall();
  // use java parallel timers instead of those provided
  // by frames.timing which are sequential instead
  if (key == 't')
    scene.shiftTimers();
  if (key == CODED)
    if (keyCode == UP)
      eye.translateYPos();
    else if (keyCode == DOWN)
      eye.translateYNeg();
    else if (keyCode == RIGHT)
      eye.translateXPos();
    else if (keyCode == LEFT)
      eye.translateXNeg();
}
