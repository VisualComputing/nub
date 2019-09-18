/**
 * Particle System.
 * by Jean Pierre Charalambos.
 *
 * Each particle in the system is implemented as an AnimatorObject
 * by overriding its animate() function.
 *
 * You can tune the particles animation frequency (default is 60Hz)
 * by calling the setPeriod(). The frame rate will then be fixed,
 * provided that your animation loop function is fast enough.
 *
 * Press '+' to speed up the particles animation.
 * Press '-' to speed down the particles animation.
 * Press ' ' (the space bar) to toggle the particles animation.
 */

import nub.processing.*;
import nub.timing.*;

Scene scene;
int nbPart;
Particle[] particle;

void setup() {
  size(1000, 800, P3D);
  scene = new Scene(this);
  nbPart = 2000;
  particle = new Particle[nbPart];
  for (int i = 0; i < particle.length; i++)
    particle[i] = new Particle(scene);
}

void draw() {
  background(0);
  pushStyle();
  strokeWeight(3); // Default
  beginShape(POINTS);
  for (int i = 0; i < nbPart; i++)
    particle[i].draw();
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
  if (key == '+')
    for (int i = 0; i < particle.length; i++)
      particle[i].setPeriod(particle[i].period()-2);
  if (key == '-')
    for (int i = 0; i < particle.length; i++)
      particle[i].setPeriod(particle[i].period()+2);
  if (key == ' ')
    for (int i = 0; i < particle.length; i++)
      particle[i].toggle();
}