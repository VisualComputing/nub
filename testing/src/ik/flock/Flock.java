package ik.flock;


import nub.core.Interpolator;
import nub.core.Node;
import nub.ik.skinning.CPULinearBlendSkinning;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class Flock extends PApplet {
  Scene scene;
  //flock bounding box
  static int flockWidth = 1280;
  static int flockHeight = 720;
  static int flockDepth = 600;
  static boolean avoidWalls = true;

  String shapePath = "/testing/data/objs/";
  PShape pshape;
  Node objShape;

  int initBoidNum = 50, numFlocks = 10; // amount of boids to start the program with
  static ArrayList<ArrayList<Boid>> flocks = new ArrayList<>();
  static Node avatar;
  static boolean animate = true;
  ArrayList<CPULinearBlendSkinning> skinning = new ArrayList<>();

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    scene.setAnchor(scene.center());
    scene.setFOV(PI / 3);
    scene.fit();
    // create and fill the list of boids
    for (int k = 0; k < numFlocks; k++) {
      ArrayList<Boid> flock = new ArrayList();
      generateFish("fish" + k % 4);
      for (int i = 0; i < initBoidNum; i++)
        flock.add(new Boid(scene, objShape, skinning.get(k), new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2), flock));
      flocks.add(flock);
    }
    frameRate(30);
  }

  public void draw() {
    background(0);
    ambientLight(128, 128, 128);
    directionalLight(255, 255, 255, 0, 1, -100);
    walls();
    if (act) {
      scene.render();
      updateAvatar();
    }
  }

  public void updateAvatar() {
    // boid is the one picked with a 'mouseClicked'
    Node boid = scene.node("mouseClicked");
    if (boid != avatar) {
      avatar = boid;
      if (avatar != null)
        thirdPerson();
      else if (scene.eye().reference() != null)
        resetEye();
    }
  }

  // 'first-person' interaction
  public void mouseDragged() {
    if (scene.eye().reference() == null)
      if (mouseButton == LEFT)
        // same as: scene.spin(scene.eye());
        scene.mouseSpin();
      else if (mouseButton == RIGHT)
        // same as: scene.translate(scene.eye());
        scene.mouseTranslate();
      else
        // same as: scene.zoom(mouseX - pmouseX, scene.eye());
        scene.scale(mouseX - pmouseX);
  }

  // highlighting and 'third-person' interaction
  public void mouseMoved(MouseEvent event) {
    // 1. highlighting
    scene.tag("mouseMoved", mouseX, mouseY);
    // 2. 'third-person interaction
    if (scene.eye().reference() != null)
      // press shift to move the mouse without looking around
      if (!event.isShiftDown())
        scene.mouseLookAround();
  }

  public void mouseWheel(MouseEvent event) {
    // same as: scene.scale(event.getCount() * 20, scene.eye());
    scene.scale(event.getCount() * 20);
  }

  // picks up a boid avatar, may be null
  public void mouseClicked() {
    scene.tag("mouseClicked", mouseX, mouseY);
  }

  // Sets current avatar as the eye reference and interpolate the eye to it
  public void thirdPerson() {
    scene.eye().setReference(avatar);
    scene.fit(avatar, 0);
  }

  // Resets the eye
  public void resetEye() {
    // same as: scene.eye().setReference(null);
    scene.eye().resetReference();
    scene.lookAt(scene.center());
    scene.fit(1);
  }

  public void walls() {
    pushStyle();
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
    popStyle();
  }

  boolean act = false;

  public void keyPressed() {
    act = true;

    switch (key) {
      case 'a':
        animate = !animate;
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

  public void generateFish(String name) {
    String shapeFile = sketchPath() + shapePath + name + ".obj";
    String textureFile = sketchPath() + shapePath + name + ".jpg";
    //Invert Y Axis and set Fill
    objShape = new Node();
    objShape.rotate(new Quaternion(new Vector(0, 0, 1), PI));

    List<Node> skeleton = fishSkeleton(objShape);
    objShape.scale(0.2f);
    objShape.rotate(new Quaternion(new Vector(0, 1, 0), -PI / 2.f));

    //Uncomment to use Linear Blending Skinning with CPU
    skinning.add(new CPULinearBlendSkinning(skeleton, scene.context(), shapeFile, textureFile, 200, true));
    //Adding IK behavior
    Node target = new Node();
    target.setReference(objShape);
    target.setPosition(skeleton.get(skeleton.size() - 1).position());
    //Making a default Path that target must follow
    setupTargetInterpolator(objShape, target);
    Solver solver = scene.registerTreeSolver(skeleton.get(0));
    solver.setMaxError(0.1f);
    scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);
  }

  public List<Node> fishSkeleton(Node reference) {
    Node j1 = new Node();
    j1.setReference(reference);
    j1.setPosition(0, 10.8f, 93);
    Node j2 = new Node();
    j2.setReference(j1);
    j2.setPosition(0, 2.3f, 54.7f);
    Node j3 = new Node();
    j3.setReference(j2);
    j3.setPosition(0, 0.4f, 22);
    Node j4 = new Node();
    j4.setReference(j3);
    j4.setPosition(0, 0, -18);
    Node j5 = new Node();
    j5.setReference(j4);
    j5.setPosition(0, 1.8f, -54);
    Node j6 = new Node();
    j6.setReference(j5);
    j6.setPosition(0, -1.1f, -95);
    return scene.branch(j1);
  }

  public Interpolator setupTargetInterpolator(Node reference, Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.enableRecurrence();
    targetInterpolator.setSpeed(8.2f);
    // Create an initial path
    int nbKeyFrames = 10;
    float step = 2.0f * PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
      Node iFrame = new Node();
      iFrame.setReference(reference);
      iFrame.setTranslation(new Vector(140 * sin(step * i), target.translation().y(), target.translation().z()));
      targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.run();
    return targetInterpolator;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.flock.Flock"});
  }
}
