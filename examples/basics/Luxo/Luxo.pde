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

import frames.primitives.*;
import frames.core.*;
import frames.core.constraint.*;
import frames.processing.*;

Scene scene;
Lamp lamp;

void setup() {
  size(1000, 800, P3D);
  scene = new Scene(this);
  scene.setFieldOfView(PI / 3);
  scene.setRadius(100);
  scene.fitBallInterpolation();
  lamp = new Lamp(scene);
}

void draw() {
  background(0);
  lights();

  //draw the lamp
  scene.traverse();

  //draw the ground
  noStroke();
  fill(120, 120, 120);
  float nbPatches = 100;
  normal(0.0f, 0.0f, 1.0f);
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
  scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.zoom(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}
