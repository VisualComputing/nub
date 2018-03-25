package third;

import common.InteractiveNode;
import frames.core.Node;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Flock is a work in progress still
 */
public class Flock extends PApplet {
  Scene scene;
  //flock bounding box
  static int flockWidth = 1280;
  static int flockHeight = 720;
  static int flockDepth = 600;
  static boolean avoidWalls = true;
  static float hue = 255;

  int initBoidNum = 900; // amount of boids to start the program with
  static ArrayList<Boid> flock;
  static Node avatar;
  static boolean animate = true;

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setBoundingBox(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    scene.setAnchor(scene.center());
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    //interactivity defaults to the eye
    scene.setDefaultGrabber(eye);
    scene.fitBall();
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid(scene, new PVector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
  }

  public void draw() {
    background(0);
    ambientLight(128, 128, 128);
    directionalLight(255, 255, 255, 0, 1, -100);
    walls();
    //calls visit() on all nodes
    scene.traverse();
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

  public void keyPressed() {
    switch (key) {
      case 'a':
        animate = !animate;
        break;
      case 's':
        if (scene.eye().reference() == null)
          scene.fitBallInterpolation();
        break;
      case 't':
        scene.shiftTimers();
        break;
      case 'p':
        println("Frame rate: " + frameRate);
        break;
      case 'v':
        avoidWalls = !avoidWalls;
        break;
      case ' ':
        if (scene.eye().reference() != null) {
          scene.lookAt(scene.center());
          scene.fitBallInterpolation();
          scene.eye().setReference(null);
        } else if (avatar != null) {
          scene.eye().setReference(avatar);
          scene.interpolateTo(avatar);
        }
        break;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"third.Flock"});
  }
}
