package third;

import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.AnimatorObject;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

// this is so obviously an animator object
class Boid extends AnimatorObject {
  Scene scene;
  PApplet p;
  Node node;
  Quaternion q;
  int grabsMouseColor;//color
  int avatarColor;

  // fields
  PVector pos, vel, acc, ali, coh, sep; // pos, velocity, and acceleration in
  // a vector datatype
  float neighborhoodRadius; // radius in which it looks for fellow boids
  float maxSpeed = 4; // maximum magnitude for the velocity vector
  float maxSteerForce = .1f; // maximum magnitude of the steering vector
  float sc = 3; // scale factor for the render of the boid
  float flap = 0;
  float t = 0;

  // constructors
  Boid(Scene scn, PVector inPos) {
    super(scn.timingHandler());
    scene = scn;
    p = scene.pApplet();
    grabsMouseColor = p.color(0, 0, 255);
    avatarColor = p.color(255, 0, 0);
    pos = new PVector();
    pos.set(inPos);
    node = new Node(scene);
    node.setPosition(new Vector(pos.x, pos.y, pos.z));
    vel = new PVector(p.random(-1, 1), p.random(-1, 1), p.random(1, -1));
    acc = new PVector(0, 0, 0);
    neighborhoodRadius = 100;
    setPeriod(1000 / Flock.FPS);
    start();
  }

  @Override
  public void animate() {
    if (scene.mouse().inputGrabber() == node && scene.eye().reference() != node) {
      Flock.thirdPerson = node;
      scene.eye().setReference(node);
      scene.interpolateTo(node);
    } else
      run(Flock.flock);
    render();
  }

  public void run(ArrayList bl) {
    t += .1;
    flap = 10 * PApplet.sin(t);
    // acc.add(steer(new PVector(mouseX,mouseY,300),true));
    // acc.add(new PVector(0,.05,0));
    if (Flock.avoidWalls) {
      acc.add(PVector.mult(avoid(new PVector(pos.x, Flock.flockHeight, pos.z), true), 5));
      acc.add(PVector.mult(avoid(new PVector(pos.x, 0, pos.z), true), 5));
      acc.add(PVector.mult(avoid(new PVector(Flock.flockWidth, pos.y, pos.z), true), 5));
      acc.add(PVector.mult(avoid(new PVector(0, pos.y, pos.z), true), 5));
      acc.add(PVector.mult(avoid(new PVector(pos.x, pos.y, 0), true), 5));
      acc.add(PVector.mult(avoid(new PVector(pos.x, pos.y, Flock.flockDepth), true), 5));
    }
    flock(bl);
    move();
    checkBounds();
    //render();
  }

  // ///-----------behaviors---------------
  void flock(ArrayList bl) {
    ali = alignment(bl);
    coh = cohesion(bl);
    sep = seperation(bl);
    acc.add(PVector.mult(ali, 1));
    acc.add(PVector.mult(coh, 3));
    acc.add(PVector.mult(sep, 1));
  }

  void scatter() {
  }

  // //------------------------------------

  void move() {
    vel.add(acc); // add acceleration to velocity
    vel.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    pos.add(vel); // add velocity to position
    node.setPosition(new Vector(pos.x, pos.y, pos.z));
    acc.mult(0); // reset acceleration
  }

  void checkBounds() {
    if (pos.x > Flock.flockWidth)
      pos.x = 0;
    if (pos.x < 0)
      pos.x = Flock.flockWidth;
    if (pos.y > Flock.flockHeight)
      pos.y = 0;
    if (pos.y < 0)
      pos.y = Flock.flockHeight;
    if (pos.z > Flock.flockDepth)
      pos.z = 0;
    if (pos.z < 0)
      pos.z = Flock.flockDepth;
  }

  /*
  public boolean isAvatar() {
    return scene.mouse().inputGrabber() == node && scene.eye() != node;
  }
  */

  void render() {
    p.pushStyle();
    p.stroke(Flock.hue);
    p.noFill();
    p.noStroke();
    p.fill(Flock.hue);

    q = Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), PApplet.atan2(-vel.z, vel.x)),
        new Quaternion(new Vector(0, 0, 1), PApplet.asin(vel.y / vel.mag())));
    node.setRotation(q);

    p.pushMatrix();
    // Multiply matrix to get in the node coordinate system.
    node.applyTransformation();
    scene.drawAxes(10);

    // highlight boids under the mouse
    if (node.track(p.mouseX, p.mouseY))
      p.fill(grabsMouseColor);

        /*
        // setAvatar according to scene.motionAgent().inputGrabber()
        // check if this boid's node is the avatar
        if (node.grabsInput())
            if (!isAvatar())
                scene.setAvatar(node);

        // highlight the boid if its node is the avatar
        if ( isAvatar() )
            p.fill( avatarColor );
        */

    //draw boid
    p.beginShape(PApplet.TRIANGLES);
    p.vertex(3 * sc, 0, 0);
    p.vertex(-3 * sc, 2 * sc, 0);
    p.vertex(-3 * sc, -2 * sc, 0);

    p.vertex(3 * sc, 0, 0);
    p.vertex(-3 * sc, 2 * sc, 0);
    p.vertex(-3 * sc, 0, 2 * sc);

    p.vertex(3 * sc, 0, 0);
    p.vertex(-3 * sc, 0, 2 * sc);
    p.vertex(-3 * sc, -2 * sc, 0);

    p.vertex(-3 * sc, 0, 2 * sc);
    p.vertex(-3 * sc, 2 * sc, 0);
    p.vertex(-3 * sc, -2 * sc, 0);
    p.endShape();

    p.popMatrix();
    p.popStyle();
  }

  // steering. If arrival==true, the boid slows to meet the target. Credit to
  // Craig Reynolds
  PVector steer(PVector target, boolean arrival) {
    PVector steer = new PVector(); // creates vector for steering
    if (!arrival) {
      steer.set(PVector.sub(target, pos)); // steering vector points
      // towards target (switch
      // target and pos for
      // avoiding)
      steer.limit(maxSteerForce); // limits the steering force to
      // maxSteerForce
    } else {
      PVector targetOffset = PVector.sub(target, pos);
      float distance = targetOffset.mag();
      float rampedSpeed = maxSpeed * (distance / 100);
      float clippedSpeed = PApplet.min(rampedSpeed, maxSpeed);
      PVector desiredVelocity = PVector.mult(targetOffset,
          (clippedSpeed / distance));
      steer.set(PVector.sub(desiredVelocity, vel));
    }
    return steer;
  }

  // avoid. If weight == true avoidance vector is larger the closer the boid
  // is to the target
  PVector avoid(PVector target, boolean weight) {
    PVector steer = new PVector(); // creates vector for steering
    steer.set(PVector.sub(pos, target)); // steering vector points away from
    // target
    if (weight)
      steer.mult(1 / PApplet.sq(PVector.dist(pos, target)));
    // steer.limit(maxSteerForce); //limits the steering force to
    // maxSteerForce
    return steer;
  }

  PVector seperation(ArrayList boids) {
    PVector posSum = new PVector(0, 0, 0);
    PVector repulse;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      float d = PVector.dist(pos, b.pos);
      if (d > 0 && d <= neighborhoodRadius) {
        repulse = PVector.sub(pos, b.pos);
        repulse.normalize();
        repulse.div(d);
        posSum.add(repulse);
      }
    }
    return posSum;
  }

  PVector alignment(ArrayList boids) {
    PVector velSum = new PVector(0, 0, 0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      float d = PVector.dist(pos, b.pos);
      if (d > 0 && d <= neighborhoodRadius) {
        velSum.add(b.vel);
        count++;
      }
    }
    if (count > 0) {
      velSum.div((float) count);
      velSum.limit(maxSteerForce);
    }
    return velSum;
  }

  PVector cohesion(ArrayList boids) {
    PVector posSum = new PVector(0, 0, 0);
    PVector steer = new PVector(0, 0, 0);
    int count = 0;
    for (int i = 0; i < boids.size(); i++) {
      Boid b = (Boid) boids.get(i);
      float d = PApplet.dist(pos.x, pos.y, b.pos.x, b.pos.y);
      if (d > 0 && d <= neighborhoodRadius) {
        posSum.add(b.pos);
        count++;
      }
    }
    if (count > 0) {
      posSum.div((float) count);
    }
    steer = PVector.sub(posSum, pos);
    steer.limit(maxSteerForce);
    return steer;
  }
}