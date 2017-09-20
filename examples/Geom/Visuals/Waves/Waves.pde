/**
 * Waves
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
InteractiveFrame souris;
AxisPlaneConstraint planaire;
float compteur;
float nscal=2.2;
float decalage = PI; 
float pente=100;
int dheight, dwidth;

void setup() {
  size(640, 640, P3D);
  dheight=512;
  dwidth=512;
  scene=new Scene(this);
  scene.setRadius(600);
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  souris=new InteractiveFrame(scene);
  planaire =  new WorldConstraint();
  planaire.setRotationConstraintType(AxisPlaneConstraint.Type.FORBIDDEN);
  planaire.setTranslationConstraintType(AxisPlaneConstraint.Type.PLANE);
  planaire.setTranslationConstraintDirection(new Vec(0, 0, 1));   
  souris.setConstraint(planaire);
  souris.setPosition(180, 180, 500);
  scene.camera().setPosition(new Vec(0, 700, 1100));
  scene.camera().setOrientation(new Quat(new Vec(1, 0, 0), -0.8));
  noStroke();
  frameRate(30);
}

void draw() {
  if (!(souris.grabsInput(scene.motionAgent()) && mousePressed))compteur+=0.01;
  background(0);
  noiseDetail(4);
  for (int y = -dheight; y < dheight; y+=5) {
    for (int x =-dwidth; x <dwidth; x+=5) {
      float npx= map(souris.position().x(), 0, width, 0, 1);
      float nx= map(x, -dwidth, dwidth, 0, 1);
      float npy= map(souris.position().y(), 0, height, 0, 1);
      float ny= map(y, -dheight, dheight, 0, 1);
      float xfil = noise(compteur+(npx+nx) * nscal+ decalage, compteur*3+(npy+ny) * nscal+ decalage);
      float yfil=noise(compteur+(npx+nx) * nscal- decalage, compteur*0.5+(npy+ny) * nscal- decalage);

      Quat q=new Quat(new Vec(xfil, -yfil, 0), lerp(-3, 3, (yfil*xfil)*1.8));
      PVector vq=Scene.toPVector(q.rotate(new Vec(lerp(-pente, pente, xfil), lerp(-pente, pente, yfil), lerp(-200, 200, (yfil*xfil)*5))));
      pushMatrix();
      translate(x+vq.x, y+ vq.y, vq.z);
      fill(lerp(0, 255, vq.z/300.0));
      rect(0, 0, 4, 4);
      popMatrix();
    }
  }
  pushMatrix();
  souris.applyTransformation();
  noStroke();
  fill(255, 250, 0);
  sphere(20);
  popMatrix();
}
