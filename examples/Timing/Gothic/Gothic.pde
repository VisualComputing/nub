/**
 * Gothic
 * by Jacques Maire (http://www.alcys.com/)
 * Coded on Monday 18/06/12. Modified the next day.
 * 
 * Part of proscene classroom: http://www.openprocessing.org/classroom/1158
 * Check also the collection: http://www.openprocessing.org/collection/1438
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;

Scene scene;
Planche planch;
ArrayList planches;
float d, r, h, angl, angl0;

void setup() {
  size(640, 640, P3D);
  scene=new Scene(this); 
  d=900;
  r=1200;
  angl=0.13;
  angl0=0.10;
  h=150;
  planches= new ArrayList();
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  scene.setRadius(1800);
  scene.showAll();
  scene.camera().setPosition(new Vec(0, 4500, 4000));
  scene.camera().lookAt(new Vec(0, 0, 1000));
  scene.camera().setOrientation(  new Quat(  new Vec(1, 0, 0), -0.85  ) ); 
  assemblage();
}

void assemblage() {
  PVector pos;
  Quat qor;
  float dp=d+100;
  float dp2=dp-50;
  for (int i=0;i<4;i++) {
    pos=new PVector(dp2*cos(HALF_PI*i), dp2*sin(HALF_PI*i), -1550);
    qor=new Quat(new Vec(0, 0, 1), 0);
    planches.add(new Planche( pos, qor, 150, 200, 0, 1));

    pos=new PVector(dp2*cos(HALF_PI*i), dp2*sin(HALF_PI*i), -1350);
    planches.add(new Planche( pos, qor, 100, 1200, 0, 1));
    pos=new PVector(dp2*cos(HALF_PI*i), dp2*sin(HALF_PI*i), -100);
    qor=new Quat(new Vec(0, 0, 1), QUARTER_PI);
    planches.add(new Planche( pos, qor, 200, 200, 100, 0));
  }

  for (int i=0;i<11;i++) {
    pos=new PVector(d-r+r*cos(angl*i), 0, r*sin(angl*i));
    qor=new Quat(new Vec(0, 1, 0), -angl*i);
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//suivant ox
    pos=new PVector(r-d+r*cos(PI-angl*i), 0, r*sin(PI-angl*i));
    qor=new Quat(new Vec(0, 1, 0), angl*i);
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//suivant ox
    pos=new PVector(0, r-d+r*cos(PI-angl*i), r*sin(PI-angl*i));
    qor=new Quat(new Vec(1, 0, 0), -angl*i);
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//suivant oy
    pos=new PVector(0, d-r+r*cos(angl*i), r*sin(angl*i));
    qor=new Quat(new Vec(1, 0, 0), angl*i);
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//suivant oy

    pos=new PVector(dp*cos(angl0*i), dp-dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), QUARTER_PI), new Quat(new Vec(1, 0, 0), -angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//y=-x+d
    pos=new PVector(dp-dp*cos(angl0*i), dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), QUARTER_PI), new Quat(new Vec(1, 0, 0), angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//diag y=-x+d

    pos=new PVector(dp*cos(angl0*i), -dp+dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), -QUARTER_PI), new Quat(new Vec(1, 0, 0), angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//diag y=x-d 

    pos=new PVector(dp-dp*cos(angl0*i), -dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), -QUARTER_PI), new Quat(new Vec(1, 0, 0), -angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//diag y=x-d  

    pos=new PVector(-dp+dp*cos(angl0*i), dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), -QUARTER_PI), new Quat(new Vec(1, 0, 0), angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//diag  

    pos=new PVector(-dp*cos(angl0*i), dp-dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), -QUARTER_PI), new Quat(new Vec(1, 0, 0), -angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//diag  

    pos=new PVector(-dp*cos(angl0*i), -dp+dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), QUARTER_PI), new Quat(new Vec(1, 0, 0), angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//y=-x-d

    pos=new PVector(-dp+dp*cos(angl0*i), -dp*cos(angl0*i), dp*sqrt(2)*sin(angl0*i));
    qor=Quat.multiply(new Quat(new Vec(0, 0, 1), QUARTER_PI), new Quat(new Vec(1, 0, 0), -angl0*i));
    planches.add(new Planche( pos, qor, 60, 80, h, 0));//y=-x-d
  }
}

void draw() {
  background(0);
  directionalLight(105, 105, 100, 0, -1, -1);
  directionalLight(0, 30, 155, -1, 0, -1);
  directionalLight(0, 0, 155, -1, 0, 1);
  directionalLight(155, 155, 30, 1, 1, -1);
  // fill(0, 125, 0);
  //rect(-2000, -2000, 4000, 4000);
  float temps=min(planches.size(), millis()/1000.0);
  for (int i=0;i<temps;i++) {

    Planche planch=(Planche)planches.get(i);
    planch.actualiser();
  }
}
