package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Flock extends PApplet {
  Scene scene;
  //flock bounding box
  int flockWidth = 1280;
  int flockHeight = 720;
  int flockDepth = 600;
  boolean avoidWalls = true;

  int initBoidNum = 900; // amount of boids to start the program with
  ArrayList<Boid> flock;
  Node avatar;

  public void settings() {
    size(1000, 700, P3D);
  }

  public void setup() {
    size(1000, 700, P3D);
    scene = new Scene(this, new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2), 800);
    scene.fit();
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid(new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
  }

  public void draw() {
    scene.openContext();
    background(10, 50, 25);
    ambientLight(128, 128, 128);
    directionalLight(255, 255, 255, 0, 1, -100);
    walls();
    scene.closeContext();
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
          boid.task.toggle();
        break;
      case '+':
        for (Boid boid : flock)
          boid.task.increasePeriod(-2);
        break;
      case '-':
        for (Boid boid : flock)
          boid.task.increasePeriod(2);
        break;
      case 'e':
        for (Boid boid : flock)
          boid.task.enableConcurrence(true);
        break;
      case 'd':
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

  class Boid {
    // Node
    Node node;
    // timing task
    TimingTask task;
    // fields
    Vector position, velocity, acceleration, alignment, cohesion, separation; // position, velocity, and acceleration in
    // a vector datatype
    float neighborhoodRadius; // radius in which it looks for fellow boids
    float maxSpeed = 4; // maximum magnitude for the velocity vector
    float maxSteerForce = 0.1f; // maximum magnitude of the steering vector
    float sc = 3; // scale factor for the render of the boid
    float flap = 0;
    float t = 0;

    Boid(Vector inPos) {
      node = new Node(this::display);
      position = new Vector();
      position.set(inPos);
      node.setPosition(new Vector(position.x(), position.y(), position.z()));
      velocity = Vector.random();
      acceleration = new Vector();
      neighborhoodRadius = 100;
      task = new TimingTask(this::behavior);
      task.run();
    }

    public void display(PGraphics pg) {
      pg.pushStyle();
      // uncomment to draw boid axes
      //Scene.drawAxes(pg, 10);
      pg.strokeWeight(2);
      pg.stroke(color(40, 255, 40));
      pg.fill(color(0, 255, 0, 125));
      // highlight boids under the mouse
      if (scene.node("mouseMoved") == node) {
        pg.stroke(color(0, 0, 255));
        pg.fill(color(0, 0, 255));
      }
      // highlight avatar
      if (node ==  avatar) {
        pg.stroke(color(255, 0, 0));
        pg.fill(color(255, 0, 0));
      }
      //draw boid
      pg.beginShape(TRIANGLES);
      pg.vertex(3 * sc, 0, 0);
      pg.vertex(-3 * sc, 2 * sc, 0);
      pg.vertex(-3 * sc, -2 * sc, 0);
      pg.vertex(3 * sc, 0, 0);
      pg.vertex(-3 * sc, 2 * sc, 0);
      pg.vertex(-3 * sc, 0, 2 * sc);
      pg.vertex(3 * sc, 0, 0);
      pg.vertex(-3 * sc, 0, 2 * sc);
      pg.vertex(-3 * sc, -2 * sc, 0);
      pg.vertex(-3 * sc, 0, 2 * sc);
      pg.vertex(-3 * sc, 2 * sc, 0);
      pg.vertex(-3 * sc, -2 * sc, 0);
      pg.endShape();
      pg.popStyle();
    }

    Vector avoid(Vector target) {
      Vector steer = new Vector(); // creates vector for steering
      steer.set(Vector.subtract(position, target)); // steering vector points away from
      steer.multiply(1 / sq(Vector.distance(position, target)));
      return steer;
    }

    //-----------behaviors---------------

    void behavior() {
      t += 0.1;
      flap = 10 * sin(t);
      // acceleration.add(steer(new Vector(mouseX,mouseY,300),true));
      // acceleration.add(new Vector(0,.05,0));
      if (avoidWalls) {
        acceleration.add(Vector.multiply(avoid(new Vector(position.x(), flockHeight, position.z())), 5));
        acceleration.add(Vector.multiply(avoid(new Vector(position.x(), 0, position.z())), 5));
        acceleration.add(Vector.multiply(avoid(new Vector(flockWidth, position.y(), position.z())), 5));
        acceleration.add(Vector.multiply(avoid(new Vector(0, position.y(), position.z())), 5));
        acceleration.add(Vector.multiply(avoid(new Vector(position.x(), position.y(), 0)), 5));
        acceleration.add(Vector.multiply(avoid(new Vector(position.x(), position.y(), flockDepth)), 5));
      }
      //alignment
      alignment = new Vector();
      int alignmentCount = 0;
      //cohesion
      Vector posSum = new Vector();
      int cohesionCount = 0;
      //separation
      separation = new Vector();
      Vector repulse;
      for (int i = 0; i < flock.size(); i++) {
        Boid boid = flock.get(i);
        //alignment
        float distance = Vector.distance(position, boid.position);
        if (distance > 0 && distance <= neighborhoodRadius) {
          alignment.add(boid.velocity);
          alignmentCount++;
        }
        //cohesion
        float dist = dist(position.x(), position.y(), boid.position.x(), boid.position.y());
        if (dist > 0 && dist <= neighborhoodRadius) {
          posSum.add(boid.position);
          cohesionCount++;
        }
        //separation
        if (distance > 0 && distance <= neighborhoodRadius) {
          repulse = Vector.subtract(position, boid.position);
          repulse.normalize();
          repulse.divide(distance);
          separation.add(repulse);
        }
      }
      //alignment
      if (alignmentCount > 0) {
        alignment.divide((float) alignmentCount);
        alignment.limit(maxSteerForce);
      }
      //cohesion
      if (cohesionCount > 0)
        posSum.divide((float) cohesionCount);
      cohesion = Vector.subtract(posSum, position);
      cohesion.limit(maxSteerForce);
      acceleration.add(Vector.multiply(alignment, 1));
      acceleration.add(Vector.multiply(cohesion, 3));
      acceleration.add(Vector.multiply(separation, 1));
      move();
      checkBounds();
    }

    void move() {
      velocity.add(acceleration); // add acceleration to velocity
      velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
      // exceed maxSpeed
      position.add(velocity); // add velocity to position
      node.setPosition(position);
      node.setRotation(Quaternion.multiply(Quaternion.from(Vector.plusJ, atan2(-velocity.z(), velocity.x())),
              Quaternion.from(Vector.plusK, asin(velocity.y() / velocity.magnitude()))));
      acceleration.multiply(0); // reset acceleration
    }

    void checkBounds() {
      if (position.x() > flockWidth)
        position.setX(0);
      if (position.x() < 0)
        position.setX(flockWidth);
      if (position.y() > flockHeight)
        position.setY(0);
      if (position.y() < 0)
        position.setY(flockHeight);
      if (position.z() > flockDepth)
        position.setZ(0);
      if (position.z() < 0)
        position.setZ(flockDepth);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.Flock"});
  }
}
