/**
 * Particle System.
 * by Jean Pierre Charalambos.
 *
 * Each particle animation is control by a timing task.
 *
 * You can tune the particles animation frequency (default is 25Hz)
 * by calling the setPeriod(). The frame rate will then be fixed,
 * provided that your animation loop function is fast enough.
 *
 * Press '+' to speed up the particles animation.
 * Press '-' to speed down the particles animation.
 * Press ' ' (the space bar) to toggle the particles animation.
 * Press 'e' to enable animation system concurrence.
 * Press 'd' to disable animation system concurrence.
 */

import nub.processing.*;

Scene scene;
int nbPart;
Particle[] particles;

void setup() {
  size(1000, 800, P3D);
  scene = new Scene(this);
  nbPart = 2000;
  particles = new Particle[nbPart];
  for (int i = 0; i < particles.length; i++)
    particles[i] = new Particle();
}

void draw() {
  background(0);
  pushStyle();
  strokeWeight(3); // Default
  beginShape(POINTS);
  for (Particle particle : particles)
    particle.draw();
  endShape();
  popStyle();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
}

void keyPressed() {
  for (Particle particle : particles) {
    if (key == '+')
      particle.decrementPeriod();
    if (key == '-')
      particle.incrementPeriod();
    if (key == ' ')
      particle.toggle();
    if (key == 'e')
      particle.enableConcurrence(true);
    if (key == 'd')
      particle.enableConcurrence(false);
  }
  println((!particles[0].task.isConcurrent() ? "Non-concurrent " : "Concurrent ") + "system. Particle period: " + particles[0].period());
}