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
 * Press 'e' to enable concurrence of the system animation.
 * Press 'd' to disable concurrence of the system animation.
 */

import nub.processing.*;

Scene scene;
int nbPart;
Particle[] particles;

void setup() {
  size(1000, 700, P3D);
  scene = new Scene(this);
  nbPart = 2000;
  particles = new Particle[nbPart];
  for (int i = 0; i < particles.length; i++)
    particles[i] = new Particle();
  scene.enableHint(Scene.BACKGROUND | Scene.GRID);
}

void draw() {
  scene.render();
  pushStyle();
  stroke(255, 0, 0);
  strokeWeight(3);
  beginShape(POINTS);
  for (Particle particle : particles)
    particle.draw();
  endShape();
  popStyle();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpinEye();
  else if (mouseButton == RIGHT)
    scene.mouseTranslateEye();
  else
    scene.scaleEye(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
}

void keyPressed() {
  for (Particle particle : particles) {
    if (key == '+')
      // less period is faster
      particle.increasePeriod(-2);
    if (key == '-')
      particle.increasePeriod(2);
    if (key == ' ')
      particle.toggle();
    if (key == 'e')
      particle.enableConcurrence(true);
    if (key == 'd')
      particle.enableConcurrence(false);
  }
  println((!particles[0].isConcurrent() ? "Non-concurrent " : "Concurrent ") + "system. Particle period: " + particles[0].period());
}
