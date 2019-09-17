package intellij;

import nub.processing.Scene;
import nub.timing.AnimatorObject;
import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;

public class ParticleSystem2 extends PApplet {
  Scene scene;
  int nbPart;
  Particle[] particle;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    scene = new Scene(this);
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

  public void mouseMoved() {
    scene.track();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.scale(event.getCount() * 20, scene.eye());
  }

  public void keyPressed() {
    if (key == '+')
      for (int i = 0; i < particle.length; i++)
        particle[i].setPeriod(particle[i].period() - 2);
    if (key == '-')
      for (int i = 0; i < particle.length; i++)
        particle[i].setPeriod(particle[i].period() + 2);
    if (key == ' ')
      for (int i = 0; i < particle.length; i++)
        particle[i].toggle();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ParticleSystem2"});
  }

  public class Particle extends AnimatorObject {
    public PVector speed;
    public PVector pos;
    public int age;
    public int ageMax;

    public Particle(Scene scene) {
      super(scene.timingHandler());
      speed = new PVector();
      pos = new PVector();
      init();
      start();
    }

    @Override
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

    public void init() {
      pos = new PVector(0.0f, 0.0f, 0.0f);
      float angle = 2.0f * PI * random(1);
      float norm = 0.04f * random(1);
      speed = new PVector(norm * cos(angle), norm * sin(angle), random(1));
      age = 0;
      ageMax = 50 + (int) random(100);
    }

    public void draw() {
      stroke(255 * ((float) age / (float) ageMax), 255 * ((float) age / (float) ageMax), 255);
      vertex(pos.x, pos.y, pos.z);
    }
  }
}