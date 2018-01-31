package basics;

import common.InteractiveNode;
import processing.core.PApplet;
import remixlab.processing.Scene;

public class Animation extends PApplet {
  int nbPart;
  Particle[] particle;
  Scene scene;

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    //scene.shiftTimers();
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    //interactivity defaults to the eye
    scene.setDefaultNode(eye);
    scene.setRadius(150);
    scene.fitBall();

    nbPart = 2000;
    particle = new Particle[nbPart];
    for (int i = 0; i < particle.length; i++)
      particle[i] = new Particle(scene);
  }

  public void draw() {
    background(0);
    pushStyle();
    strokeWeight(3); // Default
    beginShape(POINTS);
    for (int i = 0; i < nbPart; i++) {
      particle[i].draw();
    }
    endShape();
    popStyle();
  }

  public void keyPressed() {
    if (key == '+') {
      for (Particle p : particle)
        p.setPeriod(p.period() - 2);

    }
    if (key == '-') {
      for (Particle p : particle)
        p.setPeriod(p.period() + 2);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Animation"});
  }
}
