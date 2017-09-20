/**
 * Ceva Sonia
 * by Jacques Maire (http://www.alcys.com/)
 * 
 * Part of proscene classroom: http://www.openprocessing.org/classroom/1158
 * Check also the collection: http://www.openprocessing.org/collection/1438
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.*;

Scene scene;
Ceva ceva;
PImage abeille;

void setup() {
  size(640, 640, P3D);
  scene =new Scene(this);
  scene.setRadius(500);
  scene.camera().setPosition(new Vec(0, 0, 800));
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  ceva=new Ceva();
  abeille = loadImage("sonia.gif");
}

void draw() {
  background(255, 200, 0);
  ceva.cevaDraw();
}
