class Boid extends Node {
  // timing task
  TimingTask task;
  // fields
  Vector position, velocity, acceleration, alignment, cohesion, separation; // position, velocity, and acceleration in
  // a vector datatype
  float neighborhoodRadius; // radius in which it looks for fellow boids
  float maxSpeed = 4; // maximum magnitude for the velocity vector
  float maxSteerForce = 0.1; // maximum magnitude of the steering vector
  float sc = 3; // scale factor for the render of the boid
  float flap = 0;
  float t = 0;

  Boid(Vector inPos) {
    position = new Vector();
    position.set(inPos);
    setPosition(new Vector(position.x(), position.y(), position.z()));
    velocity = Vector.random();
    acceleration = new Vector();
    neighborhoodRadius = 100;
    task = new TimingTask() {
      @Override
      public void execute() {
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
    };
    task.run();
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
    if (scene.node("mouseMoved") == this) {
      pg.stroke(color(0, 0, 255));
      pg.fill(color(0, 0, 255));
    }
    // highlight avatar
    if (this ==  avatar) {
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

  void move() {
    velocity.add(acceleration); // add acceleration to velocity
    velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    position.add(velocity); // add velocity to position
    setPosition(position);
    setRotation(Quaternion.multiply(Quaternion.from(Vector.plusJ, atan2(-velocity.z(), velocity.x())),
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
