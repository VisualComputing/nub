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
Vector orig = new Vector();
Vector dir = new Vector();
Vector end = new Vector();
Vector pup;

void setup() {
  size(800, 800, P3D);
  // Set the inertia for all interactivity methods to 0.85. Default is 0.8.
  scene = new Scene(this);
  scene.setRadius(200);
  scene.togglePerspective();
  scene.fit();
  esfera = new Sphere(color(random(0, 255), random(0, 255), random(0, 255)), 10);
  esfera.setPosition(new Vector(0, 1.4, 0));
  cajas = new Box[15];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box(color(random(0, 255), random(0, 255), random(0, 255)),
      random(10, 40), random(10, 40), random(10, 40));
  scene.fit();
  scene.tag("keyboard", esfera);
}

void draw() {
  background(0);
  // calls render() on all scene nodes applying all their transformations
  scene.render();
  drawRay();
}

void drawRay() {
  if (pup != null) {
    pushStyle();
    strokeWeight(20);
    stroke(255, 255, 0);
    point(pup.x(), pup.y(), pup.z());
    strokeWeight(8);
    stroke(0, 0, 255);
    line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
    popStyle();
  }
}

void mouseClicked(MouseEvent event) {
  if (event.getButton() == RIGHT) {
    pup = scene.mouseLocation();
    if (pup != null) {
      scene.mouseToLine(orig, dir);
      end = Vector.add(orig, Vector.multiply(dir, 4000));
    }
  } else {
    scene.focusEye();
  }
}

void mouseMoved() {
  scene.mouseTag();
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
  if (key == 'c')
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
    if (scene.isTagValid("keyboard"))
      scene.removeTag("keyboard");
    else
      scene.tag("keyboard", esfera);
  if (key == CODED)
    if (keyCode == UP)
      scene.translate("keyboard", 0, -10, 0);
    else if (keyCode == DOWN)
      scene.translate("keyboard", 0, 10, 0);
    else if (keyCode == LEFT)
      scene.translate("keyboard", -10, 0, 0);
    else if (keyCode == RIGHT)
      scene.translate("keyboard", 10, 0, 0);
}
