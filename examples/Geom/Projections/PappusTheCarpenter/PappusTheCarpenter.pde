/** 
 * Pappus The Carpenter
 * by Jacques Maire (http://www.xelyx.fr and http:www.alcys.com)
 *
 * Illustrates and explains Pappus theorem in 3D.
 * 
 * The two consecutive perspectives of the the Pappus figure (in the purple plane)
 * are obtained as cylindrical projections of the two consecutive perspectives
 * located at the Pappus roof sides.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.constraint.*;

Scene scene;
Figure figure;
float temps;

void setup() {
  size(600, 600, P3D); 
  scene=new Scene(this);
  scene.setRadius(620);
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  scene.camera().setPosition(new Vec(0, 0, 800 ));
  scene.showAll();
  figure=new Figure();
}

void draw() { 
  temps=0.0005*millis();
  background(250, 100, 170, 70);
  pushStyle();
  strokeWeight(4);
  scene.drawAxes();
  popStyle();
  figure.draw();
}

Vec comb(float t1, Vec v1, float t2, Vec v2) {
  Vec res=Vec.add(Vec.multiply(v1, t1), Vec.multiply(v2, t2));
  return res;
}
Vec comb(float t1, Vec v1, float t2, Vec v2, float t3, Vec v3) {
  Vec res=Vec.add(Vec.add(Vec.multiply(v1, t1), Vec.multiply(v2, t2)), Vec.multiply(v3, t3));
  return res;
}

Vec barycentre(float lamb, Vec u, Vec v) {
  return comb(1-lamb, u, lamb, v);
}

float determinant(Vec u, Vec v) {
  return u.x()*v.y()-u.y()*v.x();
}