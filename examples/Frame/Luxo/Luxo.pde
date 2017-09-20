/**
 * Luxo.
 * by Jean Pierre Charalambos.
 * 
 * A more complex example that combines InteractiveFrames, selection and constraints.
 * 
 * This example displays a famous luxo lamp (Pixar) that can be interactively
 * manipulated with the mouse. It illustrates the use of several InteractiveFrames
 * in the same scene.
 * 
 * Hover over lamp elements to select them, and then drag them with the mouse.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.*;

Scene scene;
Lamp lamp;

public void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);  
  scene.setRadius(100);
  scene.showAll();
  scene.setGridVisualHint(false);
  lamp = new Lamp(scene);
}

public void draw() {
  background(0);
  lights();
  
  //draw the lamp
  scene.drawFrames();

  //draw the ground
  noStroke();
  fill(120, 120, 120);
  float nbPatches = 100;
  normal(0.0f, 0.0f, 1.0f);
  for (int j=0; j<nbPatches; ++j) {
    beginShape(QUAD_STRIP);
    for (int i=0; i<=nbPatches; ++i) {
      vertex((200*(float)i/nbPatches-100), (200*j/nbPatches-100));
      vertex((200*(float)i/nbPatches-100), (200*(float)(j+1)/nbPatches-100));
    }
    endShape();
  }
}