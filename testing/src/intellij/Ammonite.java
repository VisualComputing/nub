package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class Ammonite extends PApplet {
  Scene scene;
  Node[] reperes;
  Quaternion qua, qcam, qc;
  float theta;
  int nb;

  public void settings() {
    size(640, 640, P3D);
  }

  public void setup() {
    scene=new Scene(this);
    scene.setRadius(800);
    //scene.setGridVisualHint(false);
    //scene.setAxesVisualHint(false);
    nb=220;
    reperes=new Node[nb];
    reperes[0]=new Node();
    for (int i=1;i<nb;i++)
      reperes[i]=new Node(reperes[i-1]);
    qua=new Quaternion(new Vector(1, 0, 0), 0.90f);
    qcam=Quaternion.multiply(new Quaternion(new Vector(0, 0, 1), PI/2), new Quaternion(new Vector(1, 0, 0), PI/2));
    qcam.normalize();
    scene.eye().setPosition(new Vector(0, 0, 1000));
  }

  public void draw() {
    reperes[0].setOrientation(qcam);
    theta=millis()*0.0005f;
    qc=Quaternion.multiply(new Quaternion(new Vector(-0.2f, 10, 0), theta), qcam);
    qua=new Quaternion(new Vector(2*sin(theta), 3*cos(-0.04f*theta), 9*sin(0.25f*theta)), 0.10f+0.02f*cos(-0.6f*theta));
    reperes[0].setOrientation(qc);
    background(160+30*cos(theta), 160+30*cos(theta), 100+30*cos(3*theta));
    // directionalLight(100,50, 130, 0, 0, -1);
    // directionalLight(100, 60,120, -1, 1, -1);
    directionalLight(180, 100, 145, -1, -1, 0);
    directionalLight( 100, 100,0, 0, -1, -1);
    // lights();

    scene.render();

    /*
    pushMatrix();
    reperes[0].applyTransformation();
    dessine(1);
    popMatrix();
    // */
  }

  void dessine(int n) {
    float p=12*pow(1.025f, n);
    reperes[n].setPosition(qua.rotate(new Vector(0, 0, 1.5f*cos(-0.6f*theta))));
    reperes[n].setOrientation(qua);
    PVector v1, v0;
    noStroke();
    if (n%2==0) {
      fill(255);
      beginShape(TRIANGLE_STRIP);
      for (int i=0;i<=24;i++) {
        v1= Scene.toPVector(reperes[n].worldLocation(new Vector(p*cos(i*TWO_PI/24), p*sin(i*TWO_PI/24)-1.54f*p, 0)));
        v0= Scene.toPVector(reperes[n-1].worldLocation(new Vector(p/1.025f*cos(i*TWO_PI/24), p/1.025f*sin(i*TWO_PI/24)-1.54f*p/1.025f, 0)));
        vertex(v0.x, v0.y, v0.z);
        vertex(v1.x, v1.y, v1.z);
      }
      endShape();
    }
    if (n<nb-1)dessine(n+1);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Ammonite"});
  }
}
