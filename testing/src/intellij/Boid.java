package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;

class Boid extends TimingTask {
  Scene scene;
  ArrayList<Boid> flock;
  // render
  Node node;
  // fields
  Vector position, velocity, acceleration, alignment, cohesion, separation; // position, velocity, and acceleration in
  // a vector datatype
  float neighborhoodRadius; // radius in which it looks for fellow boids
  float maxSpeed = 4; // maximum magnitude for the velocity vector
  float maxSteerForce = 0.1f; // maximum magnitude of the steering vector
  float sc = 3; // scale factor for the render of the boid
  float flap = 0;
  float t = 0;

  Boid(Scene scn, ArrayList<Boid> f, Vector inPos) {
    super();
    scene = scn;
    flock = f;
    // the boid node just holds the boid appearance for rendering
    node = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();

        // uncomment to draw boid axes
        //Scene.drawAxes(pg, 10);

        pg.strokeWeight(2);
        pg.stroke(scene.pApplet.color(40, 255, 40));
        pg.fill(scene.pApplet.color(0, 255, 0, 125));

        // highlight boids under the mouse
        if (scene.node("mouseMoved") == this) {
          pg.stroke(scene.pApplet.color(0, 0, 255));
          pg.fill(scene.pApplet.color(0, 0, 255));
        }

        // highlight avatar
        if (this == Flock.avatar) {
          pg.stroke(scene.pApplet.color(255, 0, 0));
          pg.fill(scene.pApplet.color(255, 0, 0));
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
    };
    position = new Vector();
    position.set(inPos);
    node.setPosition(new Vector(position.x(), position.y(), position.z()));
    velocity = new Vector(scene.pApplet.random(-1, 1), scene.pApplet.random(-1, 1), scene.pApplet.random(1, -1));
    acceleration = new Vector(0, 0, 0);
    neighborhoodRadius = 100;
    run();
  }

  @Override
  public void execute() {
    t += 0.1;
    flap = 10 * scene.pApplet.sin(t);
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
      float dist = scene.pApplet.dist(position.x(), position.y(), boid.position.x(), boid.position.y());
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

  Vector avoid(Vector target) {
    Vector steer = new Vector(); // creates vector for steering
    steer.set(Vector.subtract(position, target)); // steering vector points away from
    steer.multiply(1 / scene.pApplet.sq(Vector.distance(position, target)));
    return steer;
  }

  //-----------behaviors---------------

  void move() {
    velocity.add(acceleration); // add acceleration to velocity
    velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    position.add(velocity); // add velocity to position
    node.setPosition(position);
    node.setRotation(Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), scene.pApplet.atan2(-velocity.z(), velocity.x())),
        new Quaternion(new Vector(0, 0, 1), scene.pApplet.asin(velocity.y() / velocity.magnitude()))));
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
