/**
 * Ammonite
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
InteractiveFrame[] reperes;
Quat qua, qcam, qc;
float theta;
int nb;

void setup() {
  size(640, 640, P3D);
  scene=new Scene(this); 
  scene.setRadius(800);
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);
  nb=220;
  reperes=new InteractiveFrame[nb];
  reperes[0]=new InteractiveFrame(scene);
  for (int i=1;i<nb;i++)
    reperes[i]=new InteractiveFrame(scene, reperes[i-1]);
  qua=new Quat(new Vec(1, 0, 0), 0.90);
  qcam=Quat.multiply(new Quat(new Vec(0, 0, 1), PI/2), new Quat(new Vec(1, 0, 0), PI/2));
  qcam.normalize();
  scene.camera().setPosition(new Vec(0, 0, 1000));
}

void draw() {
  reperes[0].setOrientation(qcam); 
  theta=millis()*0.0005;
  qc=Quat.multiply(new Quat(new Vec(-0.2, 10, 0), theta), qcam);
  qua=new Quat(new Vec(2*sin(theta), 3*cos(-0.04*theta), 9*sin(0.25*theta)), 0.10+0.02*cos(-0.6*theta));
  reperes[0].setOrientation(qc);
  background(160+30*cos(theta), 160+30*cos(theta), 100+30*cos(3*theta));
 // directionalLight(100,50, 130, 0, 0, -1);
 // directionalLight(100, 60,120, -1, 1, -1);
  directionalLight(180, 100, 145, -1, -1, 0);
  directionalLight( 100, 100,0, 0, -1, -1);
 // lights();
  pushMatrix();
  reperes[0].applyTransformation();
  dessine(1);
  popMatrix();
}

void dessine(int n) {
  float p=12*pow(1.025, n);
  reperes[n].setTranslation(qua.rotate(new Vec(0, 0, 1.5*cos(-0.6*theta))));
  reperes[n].setRotation(qua);
  PVector v1, v0;
  noStroke();
  if (n%2==0) {
    fill(255);
    beginShape(TRIANGLE_STRIP);
    for (int i=0;i<=24;i++) {
      v1= Scene.toPVector(reperes[n].inverseCoordinatesOf(new Vec(p*cos(i*TWO_PI/24), p*sin(i*TWO_PI/24)-1.54*p, 0)));
      v0= Scene.toPVector(reperes[n-1].inverseCoordinatesOf(new Vec(p/1.025*cos(i*TWO_PI/24), p/1.025*sin(i*TWO_PI/24)-1.54*p/1.025, 0)));
      vertex(v0.x, v0.y, v0.z);
      vertex(v1.x, v1.y, v1.z);
    }
    endShape();
  } 
  if (n<nb-1)dessine(n+1);
}
