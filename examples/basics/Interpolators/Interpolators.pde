/**
 * Interpolators.
 * by Jean Pierre Charalambos.
 *
 * This example introduces the three different node interpolations.
 *
 * Press ' ' to toggle the eye path display.
 * Press 's' to fit ball interpolation.
 * Press 'f' to fit ball.
 * Press the arrow keys to move the camera.
 * Press '1' and '2' to add eye key-frame to the eye paths.
 * Press 'a' and 'c' to play the eye paths.
 * Press 'b' and 'd' to remove the eye paths.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Interpolator interpolator, eyeInterpolator1, eyeInterpolator2;
Node shape;
boolean showEyePath = true;

//Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
String renderer = P3D;

void setup() {
  size(1000, 800, renderer);
  rectMode(CENTER);
  scene = new Scene(this);
  scene.setRadius(150);

  // interpolation 1. Default eye interpolations
  scene.fit(1);

  // interpolation 2. Custom eye interpolations
  eyeInterpolator1 = new Interpolator(scene);
  eyeInterpolator2 = new Interpolator(scene);

  // interpolation 3. Custom (arbitrary) node interpolations

  shape = new Node() {
    // Note that within render() geometry is defined at the
    // node local coordinate system.
    @Override
    public void graphics(PGraphics pg) {
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
  interpolator.enableRecurrence();
  // Create an initial path
  for (int i = 0; i < random(4, 10); i++)
    interpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
  interpolator.run();
}

void draw() {
  background(0);
  scene.render();
  pushStyle();
  stroke(255);
  // same as:scene.drawCatmullRom(interpolator, 5);
  scene.drawCatmullRom(interpolator);
  popStyle();
  if (showEyePath) {
    pushStyle();
    fill(255, 0, 0);
    stroke(0, 255, 0);
    // same as:
    // scene.drawCatmullRom(eyeInterpolator1, 3);
    // scene.drawCatmullRom(eyeInterpolator2, 3);
    scene.drawCatmullRom(eyeInterpolator1);
    scene.drawCatmullRom(eyeInterpolator2);
    popStyle();
  }
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
  if (key == ' ')
    showEyePath = !showEyePath;

  if (key == '1')
    eyeInterpolator1.addKeyFrame(scene);
  if (key == 'a')
    eyeInterpolator1.toggle();
  if (key == 'b')
    eyeInterpolator1.clear();

  if (key == '2')
    eyeInterpolator2.addKeyFrame(scene);
  if (key == 'c')
    eyeInterpolator2.toggle();
  if (key == 'd')
    eyeInterpolator2.clear();

  if (key == '-')
    interpolator.increaseSpeed(-0.25);
  if (key == '+')
    interpolator.increaseSpeed(0.25);

  if (key == 's')
    scene.fit(1);
  if (key == 'f')
    scene.fit();
}
