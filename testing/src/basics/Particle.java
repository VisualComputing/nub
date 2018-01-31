package basics;

import processing.core.PApplet;
import processing.core.PVector;
import remixlab.processing.Scene;
import remixlab.timing.AnimatorObject;

public class Particle extends AnimatorObject {
  Scene scene;
  PVector speed;
  PVector pos;
  int age;
  int ageMax;

  public Particle(Scene scn) {
    super(scn.timingHandler());
    scene = scn;
    speed = new PVector();
    pos = new PVector();
    init();
    start();
  }

  public void animate() {
    speed.z -= 0.05f;
    pos = PVector.add(pos, PVector.mult(speed, 10f));

    if (pos.z < 0.0) {
      speed.z = -0.8f * speed.z;
      pos.z = 0.0f;
    }

    if (++age == ageMax)
      init();
  }

  public void draw() {
    scene.pApplet().stroke(255 * ((float) age / (float) ageMax), 255 * ((float) age / (float) ageMax), 255);
    scene.pApplet().vertex(pos.x, pos.y, pos.z);
  }

  public void init() {
    pos = new PVector(0.0f, 0.0f, 0.0f);
    float angle = 2.0f * PApplet.PI * scene.pApplet().random(1);
    float norm = 0.04f * scene.pApplet().random(1);
    speed = new PVector(norm * scene.pApplet().cos(angle), norm * scene.pApplet().sin(angle), scene.pApplet().random(1));
    age = 0;
    ageMax = 50 + (int) scene.pApplet().random(100);
  }
}
