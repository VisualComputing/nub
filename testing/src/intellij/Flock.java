package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Flock extends PApplet {
  Scene scene;
  //flock bounding box
  static int flockWidth = 1280;
  static int flockHeight = 720;
  static int flockDepth = 600;
  static boolean avoidWalls = true;

  int initBoidNum = 900; // amount of boids to start the program with
  ArrayList<Boid> flock;
  static Node avatar;

  public void settings() {
    size(1000, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND, color(10, 50, 25));
    scene.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    scene.fit();
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid(scene, flock, new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
  }

  public void draw() {
    ambientLight(128, 128, 128);
    directionalLight(255, 255, 255, 0, 1, -100);
    walls();
    scene.render();
    // uncomment to asynchronously update boid avatar. See mouseClicked()
    // updateAvatar(scene.node("mouseClicked"));
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
  public void mouseClicked() {
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
  public void mouseDragged() {
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
  public void mouseMoved(MouseEvent event) {
    // 1. highlighting
    scene.mouseTag("mouseMoved");
    // 2. third-person interaction
    if (scene.eye().reference() != null)
      // press shift to move the mouse without looking around
      if (!event.isShiftDown())
        scene.mouseLookAround();
  }

  public void mouseWheel(MouseEvent event) {
    // same as: scene.scale(event.getCount() * 20, scene.eye());
    scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    switch (key) {
      case 'a':
        for (Boid boid : flock)
          boid.toggle();
        break;
      case '+':
        for (Boid boid : flock)
          boid.increasePeriod(-2);
        break;
      case '-':
        for (Boid boid : flock)
          boid.increasePeriod(2);
        break;
      case 'e':
        for (Boid boid : flock)
          boid.enableConcurrence(true);
        break;
      case 'd':
        for (Boid boid : flock)
          boid.enableConcurrence(false);
        break;
      case 's':
        if (scene.eye().reference() == null)
          scene.fit(1);
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

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Flock"});
  }
}
