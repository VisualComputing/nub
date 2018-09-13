/**
 * Interpolators.
 * by Jean Pierre Charalambos.
 *
 * This example introduces the three different frame interpolations.
 *
 * Press ' ' to toggle the eye path display.
 * Press 's' to fit ball interpolation.
 * Press 'f' to fit ball.
 * Press the arrow keys to move the camera.
 * Press '1' and '2' to add eye key-frames to the eye paths.
 * Press 'a' and 'c' to play the eye paths.
 * Press 'b' and 'd' to remove the eye paths.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Interpolator interpolator, eyeInterpolator1, eyeInterpolator2;
Shape shape;
boolean showEyePath = true;

//Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
String renderer = P3D;

void setup() {
  size(1000, 800, renderer);
  rectMode(CENTER);
  scene = new Scene(this);
  //scene.setFieldOfView(PI / 3);
  scene.setRadius(150);

  // interpolation 1. Default eye interpolations
  scene.fitBallInterpolation();

  // interpolation 2. Custom eye interpolations
  eyeInterpolator1 = new Interpolator(scene.eye());
  eyeInterpolator2 = new Interpolator(scene.eye());

  // interpolation 3. Custom (arbitrary) frame interpolations

  shape = new Shape(scene) {
    // Note that within visit() geometry is defined at the
    // frame local coordinate system.
    @Override
    public void setGraphics(PGraphics pg) {
      pg.pushStyle();
      pg.fill(0, 255, 255, 125);
      pg.stroke(0, 0, 255);
      pg.strokeWeight(2);
      if (pg.is2D())
        pg.rect(0, 0, 100, 100);
      else
        pg.box(30);
      pg.popStyle();
    }
  };
  interpolator = new Interpolator(shape);
  interpolator.setLoop();
  // Create an initial path
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(scene.randomFrame());
  interpolator.start();
}

void draw() {
  background(0);
  scene.traverse();

  pushStyle();
  stroke(255);
  scene.drawPath(interpolator, 5);
  popStyle();

  for (Frame frame : interpolator.keyFrames()) {
    pushMatrix();
    scene.applyTransformation(frame);
    if (scene.tracks(frame))
      scene.drawAxes(40);
    else
      scene.drawAxes(20);
    popMatrix();
  }
  if (showEyePath) {
    pushStyle();
    fill(255, 0, 0);
    stroke(0, 255, 0);
    scene.drawPath(eyeInterpolator1, 3);
    scene.drawPath(eyeInterpolator2, 3);
    popStyle();
  }
}

void mouseMoved() {
  scene.track();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ')
    showEyePath = !showEyePath;

  if (key == '1')
    // same as: eyeInterpolator1.addKeyFrame(scene.eye().get())
    eyeInterpolator1.addKeyFrame();
  if (key == 'a')
    eyeInterpolator1.toggle();
  if (key == 'b')
    eyeInterpolator1.purge();

  if (key == '2')
    // same as: eyeInterpolator2.addKeyFrame(scene.eye().get());
    eyeInterpolator2.addKeyFrame();
  if (key == 'c')
    eyeInterpolator2.toggle();
  if (key == 'd')
    eyeInterpolator2.purge();

  if (key == '-')
    interpolator.setSpeed(interpolator.speed() - 0.25f);
  if (key == '+')
    interpolator.setSpeed(interpolator.speed() + 0.25f);

  if (key == 's')
    scene.fitBallInterpolation();
  if (key == 'f')
    scene.fitBall();
}