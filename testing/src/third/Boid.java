package third;

import frames.core.Node;
import frames.input.event.TapEvent;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

import java.util.ArrayList;

class Boid {
  Scene scene;
  PApplet pApplet;
  Node node;
  int grabsMouseColor;//color
  int avatarColor;
  // fields
  Vector position, velocity, acceleration, alignment, cohesion, separation; // position, velocity, and acceleration in
  // a vector datatype
  float neighborhoodRadius; // radius in which it looks for fellow boids
  float maxSpeed = 4; // maximum magnitude for the velocity vector
  float maxSteerForce = .1f; // maximum magnitude of the steering vector
  float sc = 3; // scale factor for the render of the boid
  float flap = 0;
  float t = 0;

  Boid(Scene scn, Vector inPos) {
    scene = scn;
    pApplet = scene.pApplet();
    grabsMouseColor = pApplet.color(0, 0, 255);
    avatarColor = pApplet.color(255, 0, 0);
    position = new Vector();
    position.set(inPos);
    node = new Node(scene) {
      // Note that within visit() geometry is defined at the
      // node local coordinate system.
      @Override
      public void visit() {
        if (Flock.animate)
          run(Flock.flock);
        render();
      }

      // Behaviour: tapping over a boid will select the node as
      // the eye reference and perform an eye interpolation to it.
      @Override
      public void interact(TapEvent event) {
        if (Flock.avatar != this && scene.eye().reference() != this) {
          Flock.avatar = this;
          scene.eye().setReference(this);
          scene.interpolateTo(this);
        }
      }
    };
    node.setPosition(new Vector(position.x(), position.y(), position.z()));
    velocity = new Vector(pApplet.random(-1, 1), pApplet.random(-1, 1), pApplet.random(1, -1));
    acceleration = new Vector(0, 0, 0);
    neighborhoodRadius = 100;
  }

  public void run(ArrayList<Boid> bl) {
    t += .1;
    flap = 10 * PApplet.sin(t);
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
    flock(bl);
    move();
    checkBounds();
  }

  Vector avoid(Vector target) {
    Vector steer = new Vector(); // creates vector for steering
    steer.set(Vector.subtract(position, target)); // steering vector points away from
    steer.multiply(1 / PApplet.sq(Vector.distance(position, target)));
    return steer;
  }

  //-----------behaviors---------------

  void flock(ArrayList<Boid> boids) {
    //alignment
    alignment = new Vector(0, 0, 0);
    int alignmentCount = 0;
    //cohesion
    Vector posSum = new Vector();
    int cohesionCount = 0;
    //separation
    separation = new Vector(0, 0, 0);
    Vector repulse;
    for (int i = 0; i < boids.size(); i++) {
      Boid boid = boids.get(i);
      //alignment
      float distance = Vector.distance(position, boid.position);
      if (distance > 0 && distance <= neighborhoodRadius) {
        alignment.add(boid.velocity);
        alignmentCount++;
      }
      //cohesion
      float dist = PApplet.dist(position.x(), position.y(), boid.position.x(), boid.position.y());
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
  }

  void move() {
    velocity.add(acceleration); // add acceleration to velocity
    velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    position.add(velocity); // add velocity to position
    node.setPosition(position);
    node.setRotation(Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), PApplet.atan2(-velocity.z(), velocity.x())),
        new Quaternion(new Vector(0, 0, 1), PApplet.asin(velocity.y() / velocity.magnitude()))));
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

  void render() {
    pApplet.pushStyle();

    // uncomment to draw boid axes
    //scene.drawAxes(10);

    pApplet.strokeWeight(2);
    pApplet.stroke(pApplet.color(0, 255, 0));
    pApplet.fill(pApplet.color(255, 0, 0));

    // highlight boids under the mouse
    if (node.track(pApplet.mouseX, pApplet.mouseY))
      pApplet.fill(grabsMouseColor);

    // highlight avatar
    if (node == Flock.avatar)
      pApplet.fill(avatarColor);

    //draw boid
    pApplet.beginShape(PApplet.TRIANGLES);
    pApplet.vertex(3 * sc, 0, 0);
    pApplet.vertex(-3 * sc, 2 * sc, 0);
    pApplet.vertex(-3 * sc, -2 * sc, 0);

    pApplet.vertex(3 * sc, 0, 0);
    pApplet.vertex(-3 * sc, 2 * sc, 0);
    pApplet.vertex(-3 * sc, 0, 2 * sc);

    pApplet.vertex(3 * sc, 0, 0);
    pApplet.vertex(-3 * sc, 0, 2 * sc);
    pApplet.vertex(-3 * sc, -2 * sc, 0);

    pApplet.vertex(-3 * sc, 0, 2 * sc);
    pApplet.vertex(-3 * sc, 2 * sc, 0);
    pApplet.vertex(-3 * sc, -2 * sc, 0);
    pApplet.endShape();

    pApplet.popStyle();
  }
}