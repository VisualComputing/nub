/**
 * Luxo.
 * by Jean Pierre Charalambos.
 *
 * A more complex example that combines Shapes, selection and constraints.
 *
 * This example displays a famous luxo lamp (Pixar) that can be interactively
 * manipulated with the mouse.
 *
 * Hover over lamp elements to select them, and then drag them with the mouse.
 */

import nub.primitives.*;
import nub.core.*;
import nub.core.constraint.*;
import nub.processing.*;

Scene scene;
Lamp lamp;

void setup() {
  size(1000, 700, P3D);
  scene = new Scene(this);
  scene.fit(1);
  lamp = new Lamp();
}

void draw() {
  background(0);
  lights();

  //draw the lamp
  scene.render();

  //draw the ground
  noStroke();
  fill(120, 120, 120);
  float nbPatches = 100;
  normal(0, 0, 1);
  for (int j = 0; j < nbPatches; ++j) {
    beginShape(QUAD_STRIP);
    for (int i = 0; i <= nbPatches; ++i) {
      vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
      vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
    }
    endShape();
  }
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  // no inertia for the nodes, but for the eye
  if (mouseButton == LEFT) {
    if (!scene.mouseSpinTag(0))
      scene.mouseSpinEye(0.85);
  } else if (mouseButton == RIGHT) {
    if (!scene.mouseTranslateTag(0))
      scene.mouseTranslateEye(0.85);
  } else {
    if (!scene.scaleTag(mouseX - pmouseX, 0))
      scene.scaleEye(mouseX - pmouseX, 0.85);
  }
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
}
