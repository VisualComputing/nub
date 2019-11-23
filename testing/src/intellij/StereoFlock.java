package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;

public class StereoFlock extends PApplet {
  Scene leftEye, rightEye, focus;
  Node head;
  //flock bounding box
  static int flockWidth = 1280;
  static int flockHeight = 720;
  static int flockDepth = 600;
  static boolean avoidWalls = true;

  Vector upVector;
  protected long lrCount;

  int initBoidNum = 300; // amount of boids to start the program with
  ArrayList<Boid> flock;
  static Node avatar;

  int w = 2000;
  int h = 800;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    head = new Node();
    head.setPosition(640, 360, 1500);
    leftEye = new Scene(this, P3D, w / 2, h);
    // eye only should belong only to the scene
    // so set a detached 'node' instance as the eye
    leftEye.setEye(new Node(head));
    leftEye.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    leftEye.eye().setMagnitude(tan((PI / 3) / 2));
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid(leftEye, flock, new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
    rightEye = new Scene(this, P3D, w / 2, h, w / 2, 0);
    // eye only should belong only to the minimap
    // so set a detached 'node' instance as the eye
    rightEye.setEye(new Node(head));
    rightEye.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    rightEye.eye().setMagnitude(tan((PI / 3) / 2));
  }

  public void draw() {
    leftEye.beginDraw();
    leftEye.context().background(75, 25, 15);
    walls(leftEye.context());
    for(Boid boid : flock) {
      leftEye.context().pushMatrix();
      leftEye.applyTransformation(boid.node);
      leftEye.draw(boid.node);
      leftEye.context().popMatrix();
    }
    leftEye.endDraw();
    leftEye.display();
    rightEye.beginDraw();
    rightEye.context().background(125, 80, 90);
    walls(rightEye.context());
    for(Boid boid : flock) {
      rightEye.context().pushMatrix();
      rightEye.applyTransformation(boid.node);
      rightEye.draw(boid.node);
      rightEye.context().popMatrix();
    }
    rightEye.endDraw();
    rightEye.display();
    // uncomment to asynchronously update boid avatar. See mouseClicked()
    // updateAvatar(scene.node("mouseClicked"));
  }

  void walls(PGraphics pg) {
    pg.pushStyle();
    pg.noFill();
    pg.stroke(255, 255, 0);
    pg.line(0, 0, 0, 0, flockHeight, 0);
    pg.line(0, 0, flockDepth, 0, flockHeight, flockDepth);
    pg.line(0, 0, 0, flockWidth, 0, 0);
    pg.line(0, 0, flockDepth, flockWidth, 0, flockDepth);
    pg.line(flockWidth, 0, 0, flockWidth, flockHeight, 0);
    pg.line(flockWidth, 0, flockDepth, flockWidth, flockHeight, flockDepth);
    pg.line(0, flockHeight, 0, flockWidth, flockHeight, 0);
    pg.line(0, flockHeight, flockDepth, flockWidth, flockHeight, flockDepth);
    pg.line(0, 0, 0, 0, 0, flockDepth);
    pg.line(0, flockHeight, 0, 0, flockHeight, flockDepth);
    pg.line(flockWidth, 0, 0, flockWidth, 0, flockDepth);
    pg.line(flockWidth, flockHeight, 0, flockWidth, flockHeight, flockDepth);
    pg.popStyle();
  }

  void updateAvatar(Node node) {
    if (node != avatar) {
      avatar = node;
      if (avatar != null)
        thirdPerson();
      else if (head.reference() != null)
        resetEye();
    }
  }

  // Sets current avatar as the eye reference and interpolate the eye to it
  void thirdPerson() {
    head.setReference(avatar);
    head.set(avatar);
  }

  // Resets the eye
  void resetEye() {
    // same as: scene.eye().setReference(null);
    head.resetReference();
    head.setPosition(640, 360, 1500);
    head.setOrientation(new Quaternion());
  }

  void lookAround(Node node, float deltaX, float deltaY) {
    Quaternion quaternion;
    if (leftEye.is2D()) {
      System.out.println("Warning: lookAroundEye is only available in 3D");
      quaternion = new Quaternion();
    } else {
      if (leftEye.frameCount() > lrCount) {
        upVector = node.yAxis();
        lrCount = leftEye.frameCount();
      }
      lrCount++;
      Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), leftEye.isRightHanded() ? -deltaY : deltaY);
      Quaternion rotY = new Quaternion(node.displacement(upVector), -deltaX);
      quaternion = Quaternion.multiply(rotY, rotX);
    }
    node.rotate(quaternion);
  }

  public void mousePressed() {
    // two options to update the boid avatar:
    // 1. Synchronously
    updateAvatar(leftEye.updateMouseTag("mousePressed"));
    // which is the same as these two lines:
    // scene.updateMouseTag("mouseClicked");
    // updateAvatar(scene.node("mouseClicked"));
    // 2. Asynchronously
    // which requires updateAvatar(scene.node("mouseClicked")) to be called within draw()
    // scene.mouseTag("mouseClicked");
  }

  /*
  // picks up a boid avatar, may be null
  public void mouseClicked() {
    // two options to update the boid avatar:
    // 1. Synchronously
    updateAvatar(leftEye.updateMouseTag("mouseClicked"));
    // which is the same as these two lines:
    // scene.updateMouseTag("mouseClicked");
    // updateAvatar(scene.node("mouseClicked"));
    // 2. Asynchronously
    // which requires updateAvatar(scene.node("mouseClicked")) to be called within draw()
    // scene.mouseTag("mouseClicked");
  }

  // 'first-person' interaction
  public void mouseDragged() {
    if (leftEye.eye().reference() == null)
      if (mouseButton == LEFT)
        // same as: scene.spin(scene.eye());
        leftEye.mouseSpin();
      else if (mouseButton == RIGHT)
        // same as: scene.translate(scene.eye());
        leftEye.mouseTranslate();
      else
        leftEye.moveForward(mouseX - pmouseX);
  }

  // highlighting and 'third-person' interaction
  public void mouseMoved(MouseEvent event) {
    // 1. highlighting
    leftEye.mouseTag("mouseMoved");
    // 2. third-person interaction
    if (leftEye.eye().reference() != null)
      // press shift to move the mouse without looking around
      if (!event.isShiftDown())
        leftEye.mouseLookAround();
  }

  public void mouseWheel(MouseEvent event) {
    // same as: scene.scale(event.getCount() * 20, scene.eye());
    leftEye.scale(event.getCount() * 20);
  }
   */

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
        if (leftEye.eye().reference() == null)
          leftEye.fit(1);
        break;
      case 'p':
        println(leftEye.eye().magnitude());
        break;
      case 'v':
        avoidWalls = !avoidWalls;
        break;
      case ' ':
        if (leftEye.eye().reference() != null)
          resetEye();
        else if (avatar != null)
          thirdPerson();
        break;
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.StereoFlock"});
  }
}
