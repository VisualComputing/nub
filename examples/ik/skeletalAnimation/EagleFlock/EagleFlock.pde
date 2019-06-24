/**
 * Flock of birds 
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
import nub.core.constraint.*;
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

int initBoidNum = 30 ,numFlocks = 2; // amount of boids to start the program with
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
      Node objShape = generateEagle();
      for (int i = 0; i < initBoidNum; i++)
          flock.add(new Boid(scene, objShape, skinning.get(k), new Vector(flockWidth / 2 + random(-flockWidth / 4, flockWidth / 4  ),
                                                                          flockHeight / 2 + random(-flockHeight / 4, flockHeight / 4  ),
                                                                          flockDepth / 2  + random(-flockDepth / 4, flockDepth / 4  )), flock));
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
Node generateEagle(){
  String shapeFile = "EAGLE_2.OBJ";
  String textureFile = "EAGLE2.jpg";
  //Invert Y Axis and set Fill
  Node objShape = new Node(scene);
  objShape.rotate(new Quaternion(new Vector(0, 0, 1), PI));

  List<Node> skeleton = buildSkeleton(null);
  skeleton.get(0).cull();
  setConstraints(skeleton);
  objShape.rotate(new Quaternion(new Vector(0, 1, 0), -PI/2.f));
  objShape.scale(1);

  skinning.add(new LinearBlendSkinningGPU(skeleton, this.g, shapeFile, textureFile, 100, false));
  
  //Adding IK behavior
  //Identify root and end effector(s)
  Node root = skeleton.get(0); //root is the fist joint of the structure
  List<Node> endEffectors = new ArrayList<Node>(); //End Effectors are leaf nodes (with no children)
  for(Node node : skeleton) {
      if (node.children().size() == 0) {
          endEffectors.add(node);
      }
  }
  
  Solver solver = scene.registerTreeSolver(skeleton.get(0));
  //Update params
  solver.setMaxError(1f);
  solver.setMaxIterations(5);
  for(Node endEffector : endEffectors){
      //4.3 Create target(s) to relate with End Effector(s)
      Node target = new Node(scene);
      target.setPickingThreshold(0);
      target.setPosition(endEffector.position().get());
      //4.4 Relate target(s) with end effector(s)
      scene.addIKTarget(endEffector, target);
      //disable enf effector tracking
      endEffector.enableTracking(false);

      //If desired generates a default Path that target must follow
      if(endEffector == skeleton.get(14)){
        setupTargetInterpolator(target, new Vector[]{new Vector(-48,0,0), new Vector(-40,-13,0), new Vector(-32,0,0) , new Vector(-40,20,0), new Vector(-48,0,0)});
      }

      if(endEffector == skeleton.get(18)){
        setupTargetInterpolator(target, new Vector[]{new Vector(44,0,0), new Vector(38,-16,0), new Vector(28.5,0,0) , new Vector(38,19,0), new Vector(44,0,0)});
      } 
  }
  return objShape;
}

List<Node> buildSkeleton(Node reference){
    Joint j1 = new Joint(scene, scene.radius() * 0.01f);
    j1.setPickingThreshold(-0.01f);
    j1.setReference(reference);
    j1.setTranslation(-1.7894811E-7f, -1.2377515f, -1.5709928f);

    Joint dummy;
    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);

    Joint j2 = new Joint(scene, scene.radius() * 0.01f);
    j2.setPickingThreshold(-0.01f);
    j2.setReference(dummy);
    j2.setTranslation(6.425498E-7f, 1.2980552f, 5.463369f);
    Joint j3 = new Joint(scene, scene.radius() * 0.01f);
    j3.setPickingThreshold(-0.01f);
    j3.setReference(j2);
    j3.setTranslation(6.5103023E-7f, 0.23802762f, 5.4746757f);


    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j4 = new Joint(scene, scene.radius() * 0.01f);
    j4.setPickingThreshold(-0.01f);
    j4.setReference(dummy);
    j4.setTranslation(-4.70038E-7f, -2.0343544f, -4.0577974f);
    Joint j5 = new Joint(scene, scene.radius() * 0.01f);
    j5.setPickingThreshold(-0.01f);
    j5.setReference(j4);
    j5.setTranslation( -4.5386977E-7f, -4.236917f, -4.046496f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j6 = new Joint(scene, scene.radius() * 0.01f);
    j6.setPickingThreshold(-0.01f);
    j6.setReference(dummy);
    j6.setTranslation( -6.223473E-7f, 1.202842f, -5.1527314f);
    Joint j7 = new Joint(scene, scene.radius() * 0.01f);
    j7.setPickingThreshold(-0.01f);
    j7.setReference(j6);
    j7.setTranslation( -7.298398E-7f, -0.33323926f, -6.1411514f);
    Joint j8 = new Joint(scene, scene.radius() * 0.01f);
    j8.setPickingThreshold(-0.01f);
    j8.setReference(j7);
    j8.setTranslation( -6.5542355E-7f, -0.4284538f, -5.5222764f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j9 = new Joint(scene, scene.radius() * 0.01f);
    j9.setPickingThreshold(-0.01f);
    j9.setReference(dummy);
    j9.setTranslation( -5.269003f, 3.248286f, -1.3883741f);
    Joint j10 = new Joint(scene, scene.radius() * 0.01f);
    j10.setPickingThreshold(-0.01f);
    j10.setReference(j9);
    j10.setTranslation( -12.133301f, 5.3501167f, 0.6687609f);
    Joint j11 = new Joint(scene, scene.radius() * 0.01f);
    j11.setPickingThreshold(-0.01f);
    j11.setReference(j10);
    j11.setTranslation( -19.107552f, 5.445654f, 2.483986f);

    dummy = new Joint(scene, scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTracking(false);
    Joint j12 = new Joint(scene, scene.radius() * 0.01f);
    j12.setPickingThreshold(-0.01f);
    j12.setReference(dummy);
    j12.setTranslation( 8.201833f, 3.9170508f, -1.8660631f);
    Joint j13 = new Joint(scene, scene.radius() * 0.01f);
    j13.setPickingThreshold(-0.01f);
    j13.setReference(j12);
    j13.setTranslation( 11.942226f, 5.541193f, 1.8152181f);
    Joint j14 = new Joint(scene, scene.radius() * 0.01f);
    j14.setPickingThreshold(-0.01f);
    j14.setReference(j13);
    j14.setTranslation( 13.184211f, 3.8215134f, 2.3884451f);
    j1.setRoot(true);
    return scene.branch(j1);
}

void setConstraints(List<Node> skeleton){
    Node j11 = skeleton.get(11);
    Vector up11 = j11.children().get(0).translation();//Same as child translation 
    Vector twist11 = Vector.cross(up11, new Vector(0,1,0), null);//Same as child translation 
    Hinge h11 = new Hinge(radians(40), radians(40), j11.rotation(), up11, twist11);
    j11.setConstraint(h11);
    
    
    Node j12 = skeleton.get(12);
    Vector up12 = j12.children().get(0).translation();//Same as child translation 
    Vector twist12 = Vector.cross(up12, new Vector(0,1,0), null);//Same as child translation 
    Hinge h12 = new Hinge(radians(40), radians(40), j12.rotation(), up12, twist12);
    j12.setConstraint(h12);
    
    Node j13 = skeleton.get(13);
    Vector up13 = j13.children().get(0).translation();//Same as child translation 
    Vector twist13 = Vector.cross(up13, new Vector(0,1,0), null);//Same as child translation 
    Hinge h13 = new Hinge(radians(45), radians(5), skeleton.get(13).rotation(), up13, twist13);
    j13.setConstraint(h13);

    
    Node j15 = skeleton.get(15);
    Vector up15 = j15.children().get(0).translation();//Same as child translation 
    Vector twist15 = Vector.cross(up15, new Vector(0,1,0), null);//Same as child translation 
    Hinge h15 = new Hinge(radians(40), radians(40), j15.rotation(), up15, twist15);
    j15.setConstraint(h15);
    
    
    Node j16 = skeleton.get(16);
    Vector up16 = j16.children().get(0).translation();//Same as child translation 
    Vector twist16 = Vector.cross(up16, new Vector(0,1,0), null);//Same as child translation 
    Hinge h16 = new Hinge(radians(40), radians(40), j16.rotation(), up16, twist16);
    j16.setConstraint(h16);
    
    Node j17 = skeleton.get(17);
    Vector up17 = j17.children().get(0).translation();//Same as child translation 
    Vector twist17 = Vector.cross(up17, new Vector(0,1,0), null);//Same as child translation 
    Hinge h17 = new Hinge(radians(45), radians(5), skeleton.get(17).rotation(), up17, twist17);
    j17.setConstraint(h17);
}


Interpolator setupTargetInterpolator(Node target, Vector[] positions) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.setLoop();
    targetInterpolator.setSpeed(3f);
    // Create a path
    for(int i = 0; i < positions.length; i++){
        Node iFrame = new Node(scene);
        iFrame.setPickingThreshold(5);
        iFrame.setReference(target.reference());
        iFrame.setTranslation(positions[i]);
        targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.start();
    return targetInterpolator;
}
