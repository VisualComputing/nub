/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates some basic Frame properties, particularly how to orient them.
 * Select and move the sphere (holding the right mouse button pressed) to see how the
 * boxes will immediately be oriented towards it. You can also pick and move the boxes
 * and still they will be oriented towards the sphere. //<>//
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;

Scene scene;
Box [] cajas;
Sphere esfera;

public void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);  
  scene.setAxesVisualHint(false);
  scene.setDottedGrid(false);
  scene.setCameraType(Camera.Type.ORTHOGRAPHIC);
  scene.setRadius(160);
  scene.showAll();

  esfera = new Sphere(scene);
  esfera.setColor(color(0, 0, 255));

  cajas = new Box[30];
  for (int i = 0; i < cajas.length; i++)
    cajas[i] = new Box(scene);
}

public void draw() {
  background(0);
  esfera.draw();
  for (int i = 0; i < cajas.length; i++) {
    cajas[i].setOrientation(esfera.getPosition());
    cajas[i].draw(true);
  }
}