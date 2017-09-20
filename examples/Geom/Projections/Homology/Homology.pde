/**
 * Homology
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
WorldConstraint contrainteX, libreT, libreTR;
Plan plan1, plan2, planProjection;
Faisceau tetraedre;

void setup() {
  size(640, 640, P3D);
  scene=new Scene(this);
  scene.setVisualHints(Scene.AXES);
  scene.setRadius(380);

  contrainteX=new WorldConstraint();
  contrainteX.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
  contrainteX.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(0.1f, 0.0f, 0.0f));
  libreT=new WorldConstraint();
  libreT.setTranslationConstraint(AxisPlaneConstraint.Type.FREE, new Vec(0.0f, 0.0f, 0.0f));
  libreT.setRotationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
  libreTR=new WorldConstraint();
  libreTR.setTranslationConstraint(AxisPlaneConstraint.Type.FREE, new Vec(0.0f, 0.0f, 0.0f));
  libreTR.setRotationConstraint(AxisPlaneConstraint.Type.FREE, new Vec(0.0f, 0.0f, 0.0f));

  plan1=new Plan(-200.0, -2.16);
  plan2=new Plan(200.0, -0.9);
  planProjection=new Plan(-400, 0.0);
  planProjection.repere.setPosition(-300, -300, -300);
  planProjection.setCouleur(color(130, 100, 120, 254));
  planProjection.repere.setConstraint(libreTR);
  planProjection.setLargeur(800);
  tetraedre=new Faisceau();

  scene.camera().setPosition(new Vec(0, 0, 800));
  scene.camera().setOrientation(new Quat(0, 0, sin(PI/4), cos(PI/4)));
}

void draw() {
  background( 170, 170, 250);
  //  directionalLight(55, 55, 255, -0.2, 1, 1);
  planProjection.draw();
  plan1.draw();
  plan2.draw();
  tetraedre.draw();
}

PVector projection(PVector WP) {
  PVector PO=comb(-1, WP, 1, Scene.toPVector(planProjection.repere.position()));
  PVector rep= comb(1, WP, planProjection.normale.dot(PO), planProjection.normale);
  ligne(WP, rep);
  return rep;
}

void rectangle(color c, float dx, float dy, float ax, float ay) {
  stroke(150);
  fill(c);
  beginShape();
  vertex(dx, dy, 0);
  vertex(ax, dy, 0);
  fill(color(red(c)*2, green(c)*2, blue(c)*2));
  vertex(ax, ay, 0);
  vertex(dx, ay, 0);
  endShape(CLOSE);
}

void triangle3d(PVector a, PVector b, PVector c) {
  beginShape();
  fill(255, 200, 0, 200);
  vertex( a.x, a.y, a.z);
  fill(255, 255, 0, 200);        
  vertex( b.x, b.y, b.z);
  fill(155, 50, 250, 200);
  vertex( c.x, c.y, c.z);
  endShape();
}

void triangle3d(PVector a, PVector b, PVector c, color couleur) {
  stroke(0, 100, 255);
  beginShape();
  fill(couleur);
  vertex( a.x, a.y, a.z);
  vertex( b.x, b.y, b.z);
  vertex( c.x, c.y, c.z);
  endShape();
}     

PVector comb(float t1, PVector v1, float t2, PVector v2) {
  PVector res=PVector.add(PVector.mult(v1, t1), PVector.mult(v2, t2));
  return res;
}

float angleEntre(PVector u, PVector v) {
  u.normalize();
  v.normalize();
  float sinus=u.x*v.y-u.y*v.x;
  return asin(sinus);
}

void balle(int i) {
  pushStyle();  
  pushMatrix();
  fill(0, 0, 255);
  noStroke();
  sphere(6);
  popMatrix();  
  popStyle();
}

void ligne(PVector a, PVector b) {
  line(a.x, a.y, a.z, b.x, b.y, b.z);
}
