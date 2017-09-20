/**
 * Spirals
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

Scene scene;
InteractiveFrame repere;
float k=1;
float a, r, angle, rapport;
PVector oz=new PVector(0, 0, 1);
int nb=10;

void setup () {
  size( 640, 640, P3D ); 
  scene=new Scene(this); 
  scene.setRadius(800);
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  scene.camera().setPosition(new Vec(0, 0, 8000));
  repere=new InteractiveFrame(scene);
  frameRate(40);
}

void draw () { 
  angle=0.0003*millis();
  rapport=0.1+0.5*sq(cos(angle));
  background(255, 0, 0 );
  directionalLight(255, 0, 0, -0.1, -0.1, -1);
  directionalLight(0, 255, 0, 0.2, 0.5, -0.6);
  directionalLight(255, 0, 255, -0.2, -0.5, -0.3);
  for (int u=0;u<nb;u++) { 
    for (int v=0;v<44;v++) {
      PVector v0=vecteur(u, PI/nb*v);
      PVector v1=vecteur(u, PI/nb*(v+1));
      PVector dif=PVector.sub(v1, v0);
      float lon=dif.mag();
      fill(255);
      noStroke();
      if (v0.mag()>10) {
        repere.setPosition(Scene.toVec(v0));
        repere.setOrientation(new Quat(Scene.toVec(oz), Scene.toVec(dif)));
        pushMatrix();
        repere.applyTransformation();
        rotate(angle*12);
        scene.drawCone(4, lon/3.0, lon/2.05, lon);
        fill(255, 200, 0);
        rotate(-angle*24); 
        box(lon*0.8, lon*0.8, lon*rapport);
        popMatrix();
      }
    }
  }
}

PVector vecteur(int u, float t) {
  float  aa=0.01*exp(k*2*PI*u/nb);
  float rayon=aa*exp(k*t);
  return new PVector(rayon*cos(t), rayon*sin(t), 0.05*sq(u));
}
