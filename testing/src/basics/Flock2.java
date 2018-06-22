package basics;

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Flock2 extends PApplet {
  Scene scene;
  //flock bounding box
  static int flockWidth = 1280;
  static int flockHeight = 720;
  static int flockDepth = 600;
  static boolean avoidWalls = true;

  int initBoidNum = 900; // amount of boids to start the program with
  static ArrayList<Boid2> flock;
  static Frame avatar;
  static boolean animate = true;

  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setBoundingBox(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    scene.setAnchor(scene.center());
    scene.setFieldOfView(PI / 3);
    scene.fitBall();
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid2(scene, new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
  }

  public void draw() {
    background(0);
    ambientLight(128, 128, 128);
    directionalLight(255, 255, 255, 0, 1, -100);
    walls();
    scene.traverse();
  }

  // interaction in 'first-person'
  public void mouseDragged() {
    if (scene.eye().reference() == null)
      if (mouseButton == LEFT)
        scene.spin(scene.eye());
      else if (mouseButton == RIGHT)
        scene.translate();
      else
        scene.zoom(mouseX - pmouseX, scene.eye());
    //scene.scale(mouseX - pmouseX);
  }

  // interaction in 'third-person'
  public void mouseMoved(MouseEvent event) {
    if (scene.eye().reference() != null)
      // press shift to move the mouse without looking around
      if (!event.isShiftDown())
        scene.lookAround();
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
    //scene.zoom(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    // picks up a boid avatar, may be null
    avatar = scene.track();
    if (avatar != null)
      thirdPerson();
    else if (scene.eye().reference() != null)
      resetEye();
  }

  // Sets current avatar as the eye reference and interpolate the eye to it
  public void thirdPerson() {
    scene.eye().setReference(avatar);
    scene.interpolateTo(avatar);
  }

  // Resets the eye
  public void resetEye() {
    scene.eye().setReference(null);
    scene.lookAt(scene.center());
    scene.fitBallInterpolation();
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
        if (scene.eye().reference() != null)
          resetEye();
        else if (avatar != null)
          thirdPerson();
        break;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Flock2"});
  }
}
