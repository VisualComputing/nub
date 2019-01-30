/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates some basic Frame properties, particularly how to
 * orient them.
 *
 * The sphere and the boxes are interactive. Pick and drag them with the
 * right mouse button. Use also the arrow keys to select and move the sphere.
 * See how the boxes will always remain oriented towards the sphere.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

Scene scene;
Box[] cajas;
boolean drawAxes = true, drawShooterTarget = true, adaptive = true;
Sphere esfera;

void setup() {
  size(800, 800, P3D);
  scene = new Scene(this);
  scene.setRadius(200);
  scene.togglePerspective();
  scene.fit();
  esfera = new Sphere(scene, color(random(0, 255), random(0, 255), random(0, 255)), 10);
  esfera.setPosition(new Vector(0, 1.4, 0));
  cajas = new Box[30];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box(scene, color(random(0, 255), random(0, 255), random(0, 255)),
                       random(10, 40), random(10, 40), random(10, 40));
  scene.fit(1);
  scene.setTrackedFrame("keyboard", esfera);
}

void draw() {
  background(0);
  // calls render() on all scene attached frames
  // automatically applying all the node transformations
  scene.render();
}

void mouseMoved() {
  scene.cast();
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
  scene.moveForward(event.getCount() * 20);
}

int randomColor() {
  return color(random(0, 255), random(0, 255), random(0, 255));
}

int randomLength(int min, int max) {
  return int(random(min, max));
}

void keyPressed() {
  if (key == 'a')
    drawAxes = !drawAxes;
  if (key == 'p')
    drawShooterTarget = !drawShooterTarget;
  if (key == 'e')
    scene.togglePerspective();
  if (key == 's')
    scene.fit(1);
  if (key == 'S')
    scene.fit();
  if (key == 'u')
    if (scene.trackedFrame("keyboard") == null)
      scene.setTrackedFrame("keyboard", esfera);
    else
      scene.resetTrackedFrame("keyboard");
  if (key == CODED)
    if (keyCode == UP)
      scene.translate("keyboard", 0, -10);
    else if (keyCode == DOWN)
      scene.translate("keyboard", 0, 10);
    else if (keyCode == LEFT)
      scene.translate("keyboard", -10, 0);
    else if (keyCode == RIGHT)
      scene.translate("keyboard", 10, 0);
}
