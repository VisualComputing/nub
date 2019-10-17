package intellij;

import nub.core.Node;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.MouseEvent;

public class ParticleSystem extends PApplet {
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
    scene.render();
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
        particle[i].task.setPeriod(particle[i].task.period() - 2);
    if (key == '-')
      for (int i = 0; i < particle.length; i++)
        particle[i].task.setPeriod(particle[i].task.period() + 2);
    //particle[i].toggle();
    if (key == ' ')
      for (int i = 0; i < particle.length; i++)
        if (particle[i].task.isActive())
          particle[i].task.stop();
        else
          particle[i].task.run(60);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.ParticleSystem"});
  }

  class Particle extends Node {
    //Task task;
    TimingTask task;
    PVector speed;
    PVector pos;
    int age;
    int ageMax;

    public Particle(Scene scene) {
      super(scene);
      speed = new PVector();
      pos = new PVector();
      init();
      //task = new Task() {
      task = new TimingTask(scene) {
        @Override
        public void execute() {
          speed.z -= 0.05f;
          pos = PVector.add(pos, PVector.mult(speed, 10f));
          if (pos.z < 0.0) {
            speed.z = -0.8f * speed.z;
            pos.z = 0.0f;
          }
          if (++age == ageMax)
            init();
        }
      };
      task.enableConcurrence();
      task.run(100);
    }

    @Override
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
  }
}