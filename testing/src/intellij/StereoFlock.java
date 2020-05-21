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

public class StereoFlock extends PApplet {
  Scene leftEye, rightEye, focus;
  Node head;
  //flock bounding box
  int flockWidth = 1280;
  int flockHeight = 720;
  int flockDepth = 600;
  boolean avoidWalls = true;

  Vector upVector;
  protected long lrCount;

  int initBoidNum = 300; // amount of boids to start the program with
  ArrayList<Boid> flock;
  static Node avatar;
  Node nodes[];

  int w = 2000;
  int h = 800;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    head = new Node();
    head.setPosition(640, 360, 1500);
    leftEye = new Scene(this, P3D, w / 2, h);
    // set a detached 'node' instance as the eye
    leftEye.setEye(new Node(head));
    leftEye.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    leftEye.eye().setMagnitude(tan((PI / 3) / 2));
    // create and fill the list of boids
    flock = new ArrayList();
    for (int i = 0; i < initBoidNum; i++)
      flock.add(new Boid(new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2)));
    nodes = new Node[flock.size()];
    nodes = flock.toArray(nodes);
    rightEye = new Scene(this, P3D, w / 2, h);
    // set a detached 'node' instance as the eye
    rightEye.setEye(new Node(head));
    rightEye.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
    rightEye.eye().setMagnitude(tan((PI / 3) / 2));
  }

  public void draw() {
    focus = mouseX < w / 2 ? leftEye : rightEye;
    draw(leftEye, 0, 0);
    draw(rightEye, w / 2, 0);
  }

  void draw(Scene scene, int pixelX, int pixelY) {
    scene.beginDraw();
    scene.context().background(75, 25, 15);
    walls(scene.context());
    for (Node node : nodes) {
      scene.context().pushMatrix();
      scene.applyTransformation(node);
      scene.draw(node);
      scene.context().popMatrix();
    }
    scene.endDraw();
    scene.display(pixelX, pixelY);
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

  void lookAround(float deltaX, float deltaY) {
    Quaternion quaternion;
    if (leftEye.frameCount() > lrCount) {
      upVector = head.yAxis();
      lrCount = leftEye.frameCount();
    }
    lrCount++;
    Quaternion rotX = new Quaternion(new Vector(1.0f, 0.0f, 0.0f), leftEye.isRightHanded() ? -deltaY : deltaY);
    Quaternion rotY = new Quaternion(head.displacement(upVector), -deltaX);
    quaternion = Quaternion.multiply(rotY, rotX);
    head.rotate(quaternion);
  }

  public void mousePressed() {
    updateAvatar(focus.updateMouseTag("mousePressed", nodes));
  }

  public void mouseMoved(MouseEvent event) {
    // 1. highlighting
    focus.updateMouseTag("mouseMoved", nodes);
    // 2. third-person interaction
    if (head.reference() != null)
      // press shift to move the mouse without looking around
      if (!event.isShiftDown())
        this.lookAround(focus.mouseRADX(), focus.mouseRADY());
  }

  public void keyPressed() {
    switch (key) {
      case 'a':
        for (Boid boid : flock)
          boid.animation.toggle();
        break;
      case '+':
        for (Boid boid : flock)
          boid.animation.increasePeriod(-2);
        break;
      case '-':
        for (Boid boid : flock)
          boid.animation.increasePeriod(2);
        break;
      case 'e':
        for (Boid boid : flock)
          boid.animation.enableConcurrence(true);
        break;
      case 'd':
        for (Boid boid : flock)
          boid.animation.enableConcurrence(false);
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
        if (head.reference() != null)
          resetEye();
        else if (avatar != null)
          thirdPerson();
        break;
    }
  }

  class Boid extends Node {
    TimingTask animation;
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
      position = new Vector();
      position.set(inPos);
      setPosition(new Vector(position.x(), position.y(), position.z()));
      velocity = new Vector(StereoFlock.this.random(-1, 1), StereoFlock.this.random(-1, 1), StereoFlock.this.random(1, -1));
      acceleration = new Vector(0, 0, 0);
      neighborhoodRadius = 100;
      animation = new TimingTask() {
        @Override
        public void execute() {
          t += 0.1;
          flap = 10 * sin(t);
          // acceleration.add(steer(new Vector(mouseX,mouseY,300),true));
          // acceleration.add(new Vector(0,.05,0));
          if (Flock.avoidWalls) {
            acceleration.add(Vector.multiply(avoid(new Vector(position.x(), Flock.flockHeight, position.z())), 5));
            acceleration.add(Vector.multiply(avoid(new Vector(position.x(), 0, position.z())), 5));
            acceleration.add(Vector.multiply(avoid(new Vector(Flock.flockWidth, position.y(), position.z())), 5));
            acceleration.add(Vector.multiply(avoid(new Vector(0, position.y(), position.z())), 5));
            acceleration.add(Vector.multiply(avoid(new Vector(position.x(), position.y(), 0)), 5));
            acceleration.add(Vector.multiply(avoid(new Vector(position.x(), position.y(), Flock.flockDepth)), 5));
          }
          //alignment
          alignment = new Vector(0, 0, 0);
          int alignmentCount = 0;
          //cohesion
          Vector posSum = new Vector();
          int cohesionCount = 0;
          //separation
          separation = new Vector(0, 0, 0);
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
      };
      animation.run(60);
    }

    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();

      // uncomment to draw boid axes
      //Scene.drawAxes(pg, 10);

      pg.strokeWeight(2);
      pg.stroke(color(40, 255, 40));
      pg.fill(color(0, 255, 0, 125));

      // highlight boids under the mouse
      if (focus.node("mouseMoved") == this) {
        pg.stroke(color(0, 0, 255));
        pg.fill(color(0, 0, 255));
      }

      // highlight avatar
      if (this == avatar) {
        pg.stroke(color(255, 0, 0));
        pg.fill(color(255, 0, 0));
      }

      //draw boid
      pg.beginShape(PApplet.TRIANGLES);
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

    void move() {
      velocity.add(acceleration); // add acceleration to velocity
      velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
      // exceed maxSpeed
      position.add(velocity); // add velocity to position
      setPosition(position);
      setRotation(Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), atan2(-velocity.z(), velocity.x())),
          new Quaternion(new Vector(0, 0, 1), asin(velocity.y() / velocity.magnitude()))));
      acceleration.multiply(0); // reset acceleration
    }

    void checkBounds() {
      if (position.x() > Flock.flockWidth)
        position.setX(0);
      if (position.x() < 0)
        position.setX(Flock.flockWidth);
      if (position.y() > Flock.flockHeight)
        position.setY(0);
      if (position.y() < 0)
        position.setY(Flock.flockHeight);
      if (position.z() > Flock.flockDepth)
        position.setZ(0);
      if (position.z() < 0)
        position.setZ(Flock.flockDepth);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.StereoFlock"});
  }
}
