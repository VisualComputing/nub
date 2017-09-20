/**
 * Associativite
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

float raySphere, zplan;
LocalConstraint planaire;
WorldConstraint drag;
boolean montretout;
Arcad arc1, arc2, arc3;
Arcal ar12, ar23, ar12_3, ar1_23;
PVector or, nord;
PFont font;

void setup() {
  size(640, 640, P3D);
  font = loadFont("FreeSans-24.vlw");
  textFont(font);
  raySphere=400;
  zplan=-500;
  nord=new PVector(0, 0, raySphere);
  or=new PVector(0, 0, 0);
  scene=new Scene(this);
  scene.setRadius(1000);
  scene.setVisualHints(0);

  planaire=new LocalConstraint();
  planaire.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vec(0, 0, 1));
  planaire.setRotationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0, 0, 0));
  drag=new WorldConstraint();
  drag.setTranslationConstraint(AxisPlaneConstraint.Type.FREE, new Vec(0, 0, 0));
  drag.setRotationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0, 0, 0));

  arc1=new Arcad(new PVector(0, 150, 150), new PVector(raySphere*cos(PI/3.0)*cos(PI/8.0), raySphere*sin(PI/3.0)*cos(PI/8.0), -raySphere*sin(PI/8.0)), 2.8, "arc1");
  arc2=new Arcad(new PVector(-50, 300, 10), new PVector(raySphere*cos(PI/4.0)*cos(PI/10.0), raySphere*sin(PI/4.0)*cos(PI/10.0), raySphere*sin(PI/10.0)), 1.2, "arc2");
  arc3=new Arcad(new PVector(300, -100, 50), new PVector(raySphere*cos(-PI*1.2)*cos(PI*1.2), raySphere*sin(-PI*1.2)*cos(PI*1.2), raySphere*sin(PI*1.2)), 2.7, "arc3");
  ar12=new Arcal(Quat.multiply(arc2.quat, arc1.quat), arc1.depart, arc2.arrivee, "arc2 x arc1", 0);
  ar23=new Arcal(Quat.multiply(arc3.quat, arc2.quat), arc2.depart, arc3.arrivee, "arc3 x arc2", 0);
  ar1_23=new Arcal(Quat.multiply(ar23.quat, arc1.quat), arc1.depart, ar23.arrivee, "( arc3 x arc2 ) x arc1", 0);
  ar12_3=new Arcal(Quat.multiply(arc3.quat, ar12.quat), ar12.depart, arc3.arrivee, " arc3 x (arc2 x arc1)", 80);
  scene.camera().setPosition(new Vec(0, 0, 1800));
  montretout=false;
}

void draw() {
  background(0, 0, 105);
  directionalLight(255, 255, 255, -1, 0, 0);
  directionalLight(255, 255, 255, 1, 0, 0);

  arc1.draw();
  arc2.depart=arc1.arrivee;
  arc2.draw();
  arc3.depart=arc2.arrivee;
  arc3.draw();
  ar12.init(Quat.multiply(arc2.quat, arc1.quat), arc1.depart, arc2.arrivee);
  ar12.draw();
  ar23.init(Quat.multiply(arc3.quat, arc2.quat), arc2.depart, arc3.arrivee);
  ar23.draw();
  ar1_23.init(Quat.multiply(ar23.quat, arc1.quat), arc1.depart, ar23.arrivee);
  ar1_23.draw();
  ar12_3.init(Quat.multiply(arc3.quat, ar12.quat), ar12.depart, arc3.arrivee);
  ar12_3.draw();
}

void keyPressed() {
  montretout=!montretout;
}
