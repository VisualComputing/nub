package intellij;

import nub.core.Node;
import nub.processing.Scene;
import nub.core.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.MouseEvent;

public class ParticleSystem extends PApplet {
  Scene scene;
  int nbPart;
  Particle[] particles;
  boolean concurrence;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    nbPart = 2000;
    particles = new Particle[nbPart];
    for (int i = 0; i < particles.length; i++)
      particles[i] = new Particle();
  }

  public void draw() {
    background(0);
    scene.render();
  }

  public void mouseMoved() {
    scene.updateTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    for (Particle particle : particles) {
      if (key == '+')
        particle.decrementPeriod();
      if (key == '-')
        particle.incrementPeriod();
      if (key == ' ')
        particle.toggle();
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ParticleSystem"});
  }

  class Particle extends Node {
    Task task;
    PVector speed;
    PVector pos;
    int age;
    int ageMax;

    public Particle() {
      speed = new PVector();
      pos = new PVector();
      init();
      task = new Task(this::run);
      task.run();
      setShape(this::graphics);
    }

    long period() {
      return task.period();
    }

    void incrementPeriod() {
      task.setPeriod(task.period() + 2);
    }

    void decrementPeriod() {
      task.setPeriod(task.period() - 2);
    }

    void toggle() {
      task.toggle();
    }

    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.strokeWeight(3); // Default
      pg.beginShape(POINTS);
      pg.stroke(255 * ((float) age / (float) ageMax), 255 * ((float) age / (float) ageMax), 255);
      pg.vertex(pos.x, pos.y, pos.z);
      pg.endShape();
      pg.popStyle();
    }

    public void init() {
      pos = new PVector(0.0f, 0.0f, 0.0f);
      float angle = 2.0f * PI * ParticleSystem.this.random(1);
      float norm = 0.04f * ParticleSystem.this.random(1);
      speed = new PVector(norm * cos(angle), norm * sin(angle), ParticleSystem.this.random(1));
      age = 0;
      ageMax = 50 + (int) ParticleSystem.this.random(100);
    }

    void run() {
      speed.z -= 0.05f;
      pos = PVector.add(pos, PVector.mult(speed, 10f));
      if (pos.z < 0.0) {
        speed.z = -0.8f * speed.z;
        pos.z = 0.0f;
      }
      if (++age == ageMax)
        init();
    }
  }
}
