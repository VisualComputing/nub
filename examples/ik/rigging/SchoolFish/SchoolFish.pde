/**
 * Fish school 
 * by Sebastian Chaparro Cuevas.
 * 
 * This example is an Adaptation of Flock of Boids, replacing each Boid by a Fish
 * with a simple IK Structure (see Fish and Flock of Boids examples). 
 *
 * Press ' ' to switch between the different eye modes.
 * Press 'a' to toggle (start/stop) animation.
 * Press 'p' to print the current node rate.
 * Press 'm' to change the boid visual mode.
 * Press 'v' to toggle boids' wall skipping.
 * Press 's' to call scene.fit(1).
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.ik.solver.*;
import nub.ik.skinning.*;
import nub.ik.visual.Joint; //Joint provides default way to visualize the skeleton
import java.util.List;

Scene scene;
//flock bounding box
int flockWidth = 1280;
int flockHeight = 720;
int flockDepth = 600;
boolean avoidWalls = true;

int initBoidNum = 80 ,numFlocks = 10; // amount of boids to start the program with
ArrayList< ArrayList<Boid> > flocks = new ArrayList< ArrayList<Boid> >();
Node avatar;
boolean animate = true;

ArrayList<Skinning> skinning = new ArrayList<Skinning>();

void setup() {
  size(1000, 800, P3D);
  scene = new Scene(this);
  scene.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
  scene.fit();
  // create and fill the list of boids
  for(int k = 0; k < numFlocks; k++){
      ArrayList<Boid> flock = new ArrayList();
      Node objShape = generateFish("fish" + k % 4);
      for (int i = 0; i < initBoidNum; i++)
          flock.add(new Boid(scene, objShape, skinning.get(k), new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2), flock));
      flocks.add(flock);
  }
}

void draw() {
  background(0);
  ambientLight(128, 128, 128);
  directionalLight(255, 255, 255, 0, 1, -100);
  walls();
  scene.render();
  // uncomment to asynchronously update boid avatar. See mouseClicked()
  // updateAvatar(scene.trackedNode("mouseClicked"));
}

void walls() {
  pushStyle();
  noFill();
  stroke(255, 255, 0);

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
  popStyle();
}

void updateAvatar(Node node) {
  if (node != avatar) {
    avatar = node;
    if (avatar != null)
      thirdPerson();
    else if (scene.eye().reference() != null)
      resetEye();
  }
}

// Sets current avatar as the eye reference and interpolate the eye to it
void thirdPerson() {
  scene.eye().setReference(avatar);
  scene.fit(avatar, 1);
}

// Resets the eye
void resetEye() {
  // same as: scene.eye().setReference(null);
  scene.eye().resetReference();
  scene.lookAt(scene.center());
  scene.fit(1);
}

// picks up a boid avatar, may be null
void mouseClicked() {
  // two options to update the boid avatar:
  // 1. Synchronously
  updateAvatar(scene.track("mouseClicked", mouseX, mouseY));
  // which is the same as these two lines:
  // scene.track("mouseClicked", mouseX, mouseY);
  // updateAvatar(scene.trackedNode("mouseClicked"));
  // 2. Asynchronously
  // which requires updateAvatar(scene.trackedNode("mouseClicked")) to be called within draw()
  // scene.cast("mouseClicked", mouseX, mouseY);
}

// 'first-person' interaction
void mouseDragged() {
  if (scene.eye().reference() == null)
    if (mouseButton == LEFT)
      // same as: scene.spin(scene.eye());
      scene.spin();
    else if (mouseButton == RIGHT)
      // same as: scene.translate(scene.eye());
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
}

// highlighting and 'third-person' interaction
void mouseMoved(MouseEvent event) {
  // 1. highlighting
  scene.cast("mouseMoved", mouseX, mouseY);
  // 2. third-person interaction
  if (scene.eye().reference() != null)
    // press shift to move the mouse without looking around
    if (!event.isShiftDown())
      scene.lookAround();
}

void mouseWheel(MouseEvent event) {
  // same as: scene.scale(event.getCount() * 20, scene.eye());
  scene.scale(event.getCount() * 20);
}

void keyPressed() {
  switch (key) {
  case 'a':
    animate = !animate;
    break;
  case 's':
    if (scene.eye().reference() == null)
      scene.fit(1);
    break;
  case 't':
    scene.shiftTimers();
    break;
  case 'p':
    println("Node rate: " + frameRate);
    break;
  case 'v':
    avoidWalls = !avoidWalls;
    break;
  case ' ':
    if (scene.eye().reference() != null)
      resetEye();
    else if (avatar != null)
      thirdPerson();
    break;
  }
}
  
//Generate a Fish
Node generateFish(String name){
  String  shapeFile = name + ".obj";
  String  textureFile = name + ".jpg";
  //Invert Y Axis and set Fill
  Node objShape = new Node(scene);
  objShape.rotate(new Quaternion(new Vector(0, 0, 1), PI));

  List<Node> skeleton = fishSkeleton(null);
  skeleton.get(0).cull();
  
  objShape.scale(0.1f);
  objShape.rotate(new Quaternion(new Vector(0, 1, 0), -PI/2.f));

  //Uncomment to use Linear Blending Skinning with CPU
  skinning.add(new GPULinearBlendSkinning(skeleton, this.g, shapeFile, textureFile, 200, true));
  //Adding IK behavior
  Node target = new Node(scene);
  target.setReference(null);
  target.setPosition(skeleton.get(skeleton.size() - 1).position());
  //Making a default Path that target must follow
  setupTargetInterpolator(target);
  Solver solver = scene.registerTreeSolver(skeleton.get(0));
  solver.setMaxError(0.1f);
  solver.setMaxIterations(15);
  scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);
  return objShape;
}

List<Node> fishSkeleton(Node reference) {
    Joint j1 = new Joint(scene);
    j1.setReference(reference);
    j1.setPosition(0, 10.8f, 93);
    Joint j2 = new Joint(scene);
    j2.setReference(j1);
    j2.setPosition(0, 2.3f, 54.7f);
    Joint j3 = new Joint(scene);
    j3.setReference(j2);
    j3.setPosition(0, 0.4f, 22);
    Joint j4 = new Joint(scene);
    j4.setReference(j3);
    j4.setPosition(0, 0, -18);
    Joint j5 = new Joint(scene);
    j5.setReference(j4);
    j5.setPosition(0, 1.8f, -54);
    Joint j6 = new Joint(scene);
    j6.setReference(j5);
    j6.setPosition(0, -1.1f, -95);
    j1.setRoot(true);
    return scene.branch(j1);
}

Interpolator setupTargetInterpolator(Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.setLoop();
    targetInterpolator.setSpeed(3f);
    // Create an initial path
    int nbKeyFrames = 7;
    float step = 2.0f * PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
        Node iFrame = new Node(scene);
        iFrame.setReference(target.reference());
        iFrame.setTranslation(new Vector(50 * sin(step * i), target.translation().y(), target.translation().z() - 25 + 25 * abs(sin(step * i)) ));
        targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.start();
    return targetInterpolator;
}
