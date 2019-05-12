/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates some basic Node properties, particularly how to
 * orient them.
 *
 * The sphere and the boxes are interactive. Pick and drag them with the
 * right mouse button. Use also the arrow keys to select and move the sphere.
 * See how the boxes will always remain oriented towards the sphere.
 * 
 * Press ' ' the change the picking policy adaptive/fixed.
 * Press 'c' to change the bullseye shape.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Box[] cajas;
boolean drawAxes = true, bullseye = true;
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
  scene.setTrackedNode("keyboard", esfera);
}

void draw() {
  background(0);
  // calls render() on all scene attached nodes
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
  if (key == ' ')
    for (Box caja : cajas)
      if (caja.pickingThreshold() != 0)
        if (abs(caja.pickingThreshold()) < 1)
          caja.setPickingThreshold(100 * caja.pickingThreshold());
        else
          caja.setPickingThreshold(caja.pickingThreshold() / 100);
  if(key == 'c')
    for (Box caja : cajas)
      caja.setPickingThreshold(-1 * caja.pickingThreshold());
  if (key == 'a')
    drawAxes = !drawAxes;
  if (key == 'p')
    bullseye = !bullseye;
  if (key == 'e')
    scene.togglePerspective();
  if (key == 's')
    scene.fit(1);
  if (key == 'S')
    scene.fit();
  if (key == 'u')
    if (scene.trackedNode("keyboard") == null)
      scene.setTrackedNode("keyboard", esfera);
    else
      scene.resetTrackedNode("keyboard");
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
