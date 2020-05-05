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

int initBoidNum = 80 ,numFlocks = 20; // amount of boids to start the program with
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
          flock.add(new Boid(objShape, skinning.get(k), new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2), flock));
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
  updateAvatar(scene.updateMouseTag("mouseClicked"));
  // which is the same as these two lines:
  // scene.updateMouseTag("mouseClicked");
  // updateAvatar(scene.node("mouseClicked"));
  // 2. Asynchronously
  // which requires updateAvatar(scene.node("mouseClicked")) to be called within draw()
  // scene.mouseTag("mouseClicked");
}

// 'first-person' interaction
void mouseDragged() {
  if (scene.eye().reference() == null)
    if (mouseButton == LEFT)
      // same as: scene.spin(scene.eye());
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      // same as: scene.translate(scene.eye());
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
}

// highlighting and 'third-person' interaction
void mouseMoved(MouseEvent event) {
  // 1. highlighting
  scene.mouseTag("mouseMoved");
  // 2. third-person interaction
  if (scene.eye().reference() != null)
    // press shift to move the mouse without looking around
    if (!event.isShiftDown())
      scene.mouseLookAround();
}

void mouseWheel(MouseEvent event) {
  // same as: scene.scale(event.getCount() * 20, scene.eye());
  scene.scale(event.getCount() * 20);
}

void keyPressed() {
  switch (key) {
  case 'a':
    for(ArrayList<Boid> flock : flocks)
      for (Boid boid : flock)
        boid.task.toggle();
    break;
  case '+':
    for(ArrayList<Boid> flock : flocks)
      for (Boid boid : flock)
        boid.task.increasePeriod(-2);
    break;
  case '-':
    for(ArrayList<Boid> flock : flocks)
      for (Boid boid : flock)
        boid.task.increasePeriod(2);
    break;
  case 'e':
    for(ArrayList<Boid> flock : flocks)
      for (Boid boid : flock)
        boid.task.enableConcurrence(true);
    break;
  case 'd':
    for(ArrayList<Boid> flock : flocks)
      for (Boid boid : flock)
        boid.task.enableConcurrence(false);
    break;
  case 's':
    if (scene.eye().reference() == null)
      scene.fit(1);
    break;
  case 'p':
    println("Frame rate: " + frameRate);
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
  Node objShape = new Node();
  objShape.rotate(new Quaternion(new Vector(0, 0, 1), PI));

  List<Node> skeleton = loadSkeleton(null);
  skeleton.get(0).cull();
  
  objShape.scale(0.1f);
  objShape.rotate(new Quaternion(new Vector(0, 1, 0), -PI/2.f));

  //Uncomment to use Linear Blending Skinning with CPU
  skinning.add(new GPULinearBlendSkinning(skeleton, this.g, shapeFile, textureFile, 200, true));
  //Adding IK behavior
  Node target = new Node();
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

List<Node> loadSkeleton(Node reference){
  JSONArray skeleton_data = loadJSONArray("skeleton.json");
  HashMap<String, Joint> dict = new HashMap<String, Joint>();
  List<Node> skeleton = new ArrayList<Node>();
  for(int i = 0; i < skeleton_data.size(); i++){
    JSONObject joint_data = skeleton_data.getJSONObject(i);
    Joint joint = new Joint(joint_data.getFloat("radius"));
    joint.setPickingThreshold(joint_data.getFloat("picking"));
    if(i == 0){
      joint.setRoot(true);
      joint.setReference(reference);
    }else{
      joint.setReference(dict.get(joint_data.getString("reference")));
    }
    joint.setTranslation(joint_data.getFloat("x"), joint_data.getFloat("y"), joint_data.getFloat("z"));
    joint.setRotation(joint_data.getFloat("q_x"), joint_data.getFloat("q_y"), joint_data.getFloat("q_z"), joint_data.getFloat("q_w"));
    skeleton.add(joint);
    dict.put(joint_data.getString("name"), joint);
  }  
  return skeleton;
}

Interpolator setupTargetInterpolator(Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.enableRecurrence();
    targetInterpolator.setSpeed(5f);
    // Create an initial path
    int nbKeyFrames = 7;
    float step = 2.0f * PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
        Node iFrame = new Node();
        iFrame.setReference(target.reference());
        iFrame.setTranslation(new Vector(50 * sin(step * i), target.translation().y(), target.translation().z() - 25 + 25 * abs(sin(step * i)) ));
        targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.run();
    return targetInterpolator;
}
