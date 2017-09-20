/**
 * Flock
 * by Matt Wetmore. Adapted to proscene by Jean Pierre Charalambos. 
 * 
 * A more complex example which interactively enables the selection of a frame
 * "avatar" for the camera to follow.
 * 
 * This example displays the famous artificial life program "Boids", developed by
 * Craig Reynolds in 1986.
 *
 * When an animation is activated (scene.startAnimation()), the
 * scene.animatedFrameWasTriggered boolean variable is updated each frame of your
 * drawing loop by proscene according to scene.animationPeriod().
 * 
 * You can tune the frequency of your animation (default is 60Hz) using
 * setAnimationPeriod(). The frame rate will then be fixed, provided that
 * your animation loop function is fast enough.
 * 
 * Boids under the mouse will be colored blue. If you click on a boid it will be
 * selected as the avatar, useful for the THIRD_PERSON proscene camera mode.
 * Click the space bar to switch between the different camera modes.
 *
 * Press 'm' to toggle (start/stop) animation.
 * Press '+' to decrease the animation period (animation speeds up).
 * Press '-' to increase the animation period (animation speeds down).
 * Press 'v' to toggle boids' wall skipping.
 * Press 'f' to toggle the drawing of the frame selection hits.
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;

Scene scene;
Trackable lastAvatar;
//flock bounding box
int flockWidth = 1280;
int flockHeight = 720;
int flockDepth = 600;
int initBoidNum = 300; // amount of boids to start the program with
ArrayList<Boid> flock;
boolean avoidWalls = true;
float hue = 255;
boolean triggered;
boolean inThirdPerson;
boolean changedMode;

void setup() {
  size(640, 360, P3D);
  scene = new Scene(this);
  scene.mouseAgent().setPickingMode(MouseAgent.PickingMode.CLICK);
  scene.setAxesVisualHint(false);
  scene.setGridVisualHint(false);
  scene.setBoundingBox(new Vec(0, 0, 0), new Vec(flockWidth, flockHeight, flockDepth));
  scene.showAll();
  // create and fill the list of boids
  flock = new ArrayList();
  for (int i = 0; i < initBoidNum; i++)
    flock.add(new Boid(scene, new PVector(flockWidth/2, flockHeight/2, flockDepth/2 )));
  scene.startAnimation();
}

void draw() {
  background(0);
  if (inThirdPerson && scene.avatar()==null) {
    inThirdPerson = false;
    adjustFrameRate();
  } else if (!inThirdPerson && scene.avatar()!=null) {
    inThirdPerson = true;
    adjustFrameRate();
  }
  ambientLight(128, 128, 128);
  directionalLight(255, 255, 255, 0, 1, -100);
  noFill();
  stroke(255);

  line(0, 0, 0, 0, flockHeight, 0);
  line(0, 0, flockDepth, 0, flockHeight, flockDepth);
  line(0, 0, 0, flockWidth, 0, 0);
  line(0, 0, flockDepth, flockWidth, 0, flockDepth);

  line(flockWidth, 0, 0, flockWidth, flockHeight, 0);
  line(flockWidth, 0, flockDepth, flockWidth, flockHeight, flockDepth);
  line(0, flockHeight, 0, flockWidth, flockHeight, 0);
  line(0, flockHeight, flockDepth, flockWidth, flockHeight, flockDepth);

  line(0, 0, 0, 0, 0, flockDepth);
  line(0, flockHeight, 0, 0, flockHeight, flockDepth);
  line(flockWidth, 0, 0, flockWidth, 0, flockDepth);
  line(flockWidth, flockHeight, 0, flockWidth, flockHeight, flockDepth);

  triggered = scene.timer().trigggered();
  for (Boid boid : flock) {
    if (triggered)
      boid.run(flock);
    boid.render();
  }
}

void adjustFrameRate() {
  if (scene.avatar() != null)
    frameRate(1000/scene.animationPeriod());
  else
    frameRate(60);
  if (scene.animationStarted())
    scene.restartAnimation();
}

void keyPressed() {
  switch (key) {
  case 't':
    scene.shiftTimers();
  case 'p':
    println("Frame rate: " + frameRate);
    break;
  case 'v':
    avoidWalls = !avoidWalls;
    break;
  case '+':
    scene.setAnimationPeriod(scene.animationPeriod()-2, false);
    adjustFrameRate();
    break;
  case '-':
    scene.setAnimationPeriod(scene.animationPeriod()+2, false);
    adjustFrameRate();
    break;
  case ' ':
    if ( scene.avatar() == null && lastAvatar != null)
      scene.setAvatar(lastAvatar);
    else
      lastAvatar = scene.resetAvatar();
    break;
  }
}
