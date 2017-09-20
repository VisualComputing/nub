/**
 * Animation Handler.
 * by Jean Pierre Charalambos.
 * 
 * The addAnimationHandler() function illustrated by a water particle simulation.
 *
 * When an animation is activated (scene.startAnimation()), the animation
 * function registered at the scene using addAnimationHandler() (in this named
 * animateScene()) is called in an infinite loop which is synced with the drawing
 * loop by proscene according to scene.animationPeriod().
 * 
 * You can tune the frequency of your animation (default is 60Hz) using
 * setAnimationPeriod(). The frame rate will then be fixed, provided that
 * your animation loop function is fast enough.
 *
 * Press 'm' to toggle (start/stop) animation.
 * Press 'x' to decrease the animation period (animation speeds up).
 * Press 'y' to increase the animation period (animation speeds down).
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

int nbPart;
Particle[] particle;
Scene scene;

void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);
  scene.addAnimationHandler(this, "animateScene");
  scene.setAxesVisualHint(false);
  scene.setAnimationPeriod(40); // 25Hz
  scene.startAnimation();

  nbPart = 2000;
  particle = new Particle[nbPart];
  for (int i = 0; i < particle.length; i++)
    particle[i] = new Particle();
}

void draw() {
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

void keyPressed() {
  if ((key == 'x') || (key == 'X'))
    scene.setAnimationPeriod(scene.animationPeriod()-2);
  if ((key == 'y') || (key == 'Y'))
    scene.setAnimationPeriod(scene.animationPeriod()+2);
}

void animateScene(Scene s) {
  for (int i = 0; i < nbPart; i++)
    if (particle[i] != null)
      particle[i].animate();
}