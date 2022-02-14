class Boid {
  // Node
  Node node;
  // fields
  Vector velocity, acceleration, alignment, cohesion, separation; // position, velocity, and acceleration in
  // a vector datatype
  final float neighborhoodRadius = 100; // radius in which it looks for fellow boids
  final float maxSpeed = 4; // maximum magnitude for the velocity vector
  final float maxSteerForce = 0.1f; // maximum magnitude of the steering vector
  final float sc = 3; // scale factor for the render of the boid

  Boid(Vector inPos) {
    node = new Node(inPos);
    node.setShape(this::display);
    startAnimation();
    velocity = Vector.random();
    acceleration = new Vector();
  }

  void startAnimation() {
    node.setBehavior(scene, this::behavior);
  }

  void stopAnimation() {
    scene.resetBehavior(node);
  }

  void display(PGraphics pg) {
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

  //-----------behaviors---------------

  void behavior(Graph scene) {
    // acceleration.add(steer(new Vector(mouseX,mouseY,300),true));
    // acceleration.add(new Vector(0,.05,0));
    if (avoidWalls) {
      acceleration.add(Vector.multiply(avoid(new Vector(node.position().x(), flockHeight, node.position().z())), 5));
      acceleration.add(Vector.multiply(avoid(new Vector(node.position().x(), 0, node.position().z())), 5));
      acceleration.add(Vector.multiply(avoid(new Vector(flockWidth, node.position().y(), node.position().z())), 5));
      acceleration.add(Vector.multiply(avoid(new Vector(0, node.position().y(), node.position().z())), 5));
      acceleration.add(Vector.multiply(avoid(new Vector(node.position().x(), node.position().y(), 0)), 5));
      acceleration.add(Vector.multiply(avoid(new Vector(node.position().x(), node.position().y(), flockDepth)), 5));
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
      float distance = Vector.distance(node.position(), boid.node.position());
      if (distance > 0 && distance <= neighborhoodRadius) {
        alignment.add(boid.velocity);
        alignmentCount++;
      }
      //cohesion
      float dist = dist(node.position().x(), node.position().y(), boid.node.position().x(), boid.node.position().y());
      if (dist > 0 && dist <= neighborhoodRadius) {
        posSum.add(boid.node.position());
        cohesionCount++;
      }
      //separation
      if (distance > 0 && distance <= neighborhoodRadius) {
        repulse = Vector.subtract(node.position(), boid.node.position());
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
    cohesion = Vector.subtract(posSum, node.position());
    cohesion.limit(maxSteerForce);
    acceleration.add(Vector.multiply(alignment, 1));
    acceleration.add(Vector.multiply(cohesion, 3));
    acceleration.add(Vector.multiply(separation, 1));
    move();
    checkBounds();
  }

  Vector avoid(Vector target) {
    Vector steer = new Vector(); // creates vector for steering
    steer.set(Vector.subtract(node.position(), target)); // steering vector points away from
    steer.multiply(1 / sq(Vector.distance(node.position(), target)));
    return steer;
  }

  void move() {
    velocity.add(acceleration); // add acceleration to velocity
    velocity.limit(maxSpeed); // make sure the velocity vector magnitude does not
    // exceed maxSpeed
    node.translate(velocity);
    node.setOrientation(Quaternion.multiply(Quaternion.from(Vector.plusJ, atan2(-velocity.z(), velocity.x())),
      Quaternion.from(Vector.plusK, asin(velocity.y() / velocity.magnitude()))));
    acceleration.multiply(0); // reset acceleration
  }

  void checkBounds() {
    if (node.position().x() > flockWidth)
      node.position().setX(0);
    if (node.position().x() < 0)
      node.position().setX(flockWidth);
    if (node.position().y() > flockHeight)
      node.position().setY(0);
    if (node.position().y() < 0)
      node.position().setY(flockHeight);
    if (node.position().z() > flockDepth)
      node.position().setZ(0);
    if (node.position().z() < 0)
      node.position().setZ(flockDepth);
  }
}