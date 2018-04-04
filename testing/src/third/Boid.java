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
  Vector pos, vel, acc, ali, coh, sep; // pos, velocity, and acceleration in
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
    pos = new Vector();
    pos.set(inPos);
    node = new Node(scene) {
      @Override
      public void visit() {
        //We uncoupled render() from run(Flock) to be able to set different frequencies
        //for them, as we want to appreciate the visual results (when the freqs are different).
        //The best results I found is when they both are called together every frame:
        if (Flock.animate)
          run(Flock.flock);
        render();
      }

      @Override
      public void interact(TapEvent event) {
        if (Flock.avatar != this && scene.eye().reference() != this) {
          Flock.avatar = this;
          scene.eye().setReference(this);
          scene.interpolateTo(this);
        }
      }
    };
    node.setPosition(new Vector(pos.x(), pos.y(), pos.z()));
    vel = new Vector(pApplet.random(-1, 1), pApplet.random(-1, 1), pApplet.random(1, -1));
    acc = new Vector(0, 0, 0);
    neighborhoodRadius = 100;
  }

  public void run(ArrayList<Boid> bl) {
    t += .1;
    flap = 10 * PApplet.sin(t);
    // acc.add(steer(new Vector(mouseX,mouseY,300),true));
    // acc.add(new Vector(0,.05,0));
    if (Flock.avoidWalls) {
      acc.add(Vector.multiply(avoid(new Vector(pos.x(), Flock.flockHeight, pos.z()), true), 5));
      acc.add(Vector.multiply(avoid(new Vector(pos.x(), 0, pos.z()), true), 5));
      acc.add(Vector.multiply(avoid(new Vector(Flock.flockWidth, pos.y(), pos.z()), true), 5));
      acc.add(Vector.multiply(avoid(new Vector(0, pos.y(), pos.z()), true), 5));
      acc.add(Vector.multiply(avoid(new Vector(pos.x(), pos.y(), 0), true), 5));
      acc.add(Vector.multiply(avoid(new Vector(pos.x(), pos.y(), Flock.flockDepth), true), 5));
    }
    flock(bl);
    move();
    checkBounds();
  }

  // ///-----------behaviors---------------
  void flock(ArrayList<Boid> boids) {
    //ali = alignment(boids);
    //coh = cohesion(boids);
    //sep = seperation(boids);

    // alignment
    ali = new Vector(0, 0, 0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = boids.get(i);
      float d = Vector.distance(pos, b.pos);
      if (d > 0 && d <= neighborhoodRadius) {
        ali.add(b.vel);
        count++;
      }
    }
    if (count > 0) {
      ali.divide((float) count);
      ali.limit(maxSteerForce);
    }

    // cohesion
    Vector posSum = new Vector();
    count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = boids.get(i);
      float d = PApplet.dist(pos.x(), pos.y(), b.pos.x(), b.pos.y());
      if (d > 0 && d <= neighborhoodRadius) {
        posSum.add(b.pos);
        count++;
      }
    }
    if (count > 0)
      posSum.divide((float) count);
    coh = Vector.subtract(posSum, pos);
    coh.limit(maxSteerForce);

    //separation
    sep = new Vector(0, 0, 0);
    Vector repulse;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = boids.get(i);
      float d = Vector.distance(pos, b.pos);
      if (d > 0 && d <= neighborhoodRadius) {
        repulse = Vector.subtract(pos, b.pos);
        repulse.normalize();
        repulse.divide(d);
        sep.add(repulse);
      }
    }

    acc.add(Vector.multiply(ali, 1));
    acc.add(Vector.multiply(coh, 3));
    acc.add(Vector.multiply(sep, 1));
  }

  // //------------------------------------

  void move() {
    vel.add(acc); // add acceleration to velocity
    vel.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    pos.add(vel); // add velocity to position
    node.setPosition(pos);
    node.setRotation(Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), PApplet.atan2(-vel.z(), vel.x())),
        new Quaternion(new Vector(0, 0, 1), PApplet.asin(vel.y() / vel.magnitude()))));
    acc.multiply(0); // reset acceleration
  }

  void checkBounds() {
    if (pos.x()> Flock.flockWidth)
      pos.setX(0);
    if (pos.x()< 0)
      pos.setX(Flock.flockWidth);
    if (pos.y() > Flock.flockHeight)
      pos.setY(0);
    if (pos.y() < 0)
      pos.setY(Flock.flockHeight);
    if (pos.z() > Flock.flockDepth)
      pos.setZ(0);
    if (pos.z() < 0)
      pos.setZ(Flock.flockDepth);
  }

  void render() {
    pApplet.pushStyle();

    scene.drawAxes(10);

    pApplet.stroke(Flock.hue);
    pApplet.noFill();
    pApplet.noStroke();
    pApplet.fill(Flock.hue);

    // highlight boids under the mouse
    if (node.track(pApplet.mouseX, pApplet.mouseY))
      pApplet.fill(grabsMouseColor);

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

  // avoid. If weight == true avoidance vector is larger the closer the boid
  // is to the target
  Vector avoid(Vector target, boolean weight) {
    Vector steer = new Vector(); // creates vector for steering
    steer.set(Vector.subtract(pos, target)); // steering vector points away from
    // target
    if (weight)
      steer.multiply(1 / PApplet.sq(Vector.distance(pos, target)));
    // steer.limit(maxSteerForce); //limits the steering force to
    // maxSteerForce
    return steer;
  }
}