package ik.flock;

import frames.core.Interpolator;
import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.AnimatorObject;
import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import ik.common.LinearBlendSkinningGPU;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */
class Boid extends AnimatorObject {
  Scene scene;
  PApplet p;
  Quaternion q;
  int grabsMouseColor;//color
  int avatarColor;
  boolean cpu = false;
  //SHAPE AND IK BEHAVIOR
  Node node;
  LinearBlendSkinning skinningCPU;
  LinearBlendSkinningGPU skinningGPU;
  PShape shape;
  int color;

  Target target;
  Interpolator targetInterpolator;
  //--------------------

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
  Boid(Scene scn, PVector inPos, String shapePath, String texturePath) {
    super(scn.timingHandler());
    scene = scn;
    p = scene.pApplet();
    grabsMouseColor = p.color(0, 0, 255);
    avatarColor = p.color(255, 0, 0);
    pos = new PVector();
    pos.set(inPos);
    setupShape(shapePath, texturePath);
    vel = new PVector(p.random(-1, 1), p.random(-1, 1), p.random(1, -1));
    acc = new PVector(0, 0, 0);
    neighborhoodRadius = 100;
    setPeriod(1000 / Flock.FPS);
    start();
  }

  boolean norun = false;

  @Override
  public void animate() {
    if (cpu) skinningCPU.applyTransformations();
    else skinningGPU.updateParams();

    if (scene.mouse().inputGrabber() == node && scene.eye().reference() != node) {
      Flock.thirdPerson = node;
      ((Node) scene.eye()).setReference(node);
      scene.interpolateTo(node);
      //scene.resetMouseAgentInputNode();
    } else {

      if (!norun) run(Flock.flock);
    }

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

  public boolean isAvatar() {
    return scene.mouse().inputGrabber() == node && scene.eye() != node;
  }

    /*
    boolean isAvatar() {
        return scene.avatar() == null ? false : scene.avatar().equals(node) ? true : false;
    }
    */

  void render() {
    Vector direction = node.transformOf(new Vector(vel.x, vel.y, vel.z));
    //q = Quaternion.multiply(new Quaternion(new Vector(0, 1, 0), PApplet.atan2(-vel.z, vel.x)),
    //        new Quaternion(new Vector(0, 0, 1), PApplet.asin(vel.y / vel.mag())));
    q = new Quaternion(new Vector(0, 0, 1), direction);
    node.rotate(q);
    if (!cpu) p.shader(skinningGPU.shader);
    p.pushMatrix();
    // Multiply matrix to get in the node coordinate system.
    node.applyTransformation();
    // highlight boids under the mouse
    if (node.track(p.mouseX, p.mouseY))
      shape.setFill(grabsMouseColor);
    else
      shape.setFill(color);
    p.shape(shape);
    p.popMatrix();

    if (!cpu) p.resetShader();

    //Uncomment to see how IK is working
        /*
        target.draw();
        for(Frame f : targetInterpolator.keyFrames()){
            p.pushMatrix();
            p.noStroke();
            scene.applyWorldTransformation(f);
            p.sphere(10);
            p.popMatrix();
        }
        */
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

  //SHAPE AND IK BEHAVIOR
  public void setupShape(String shapePath, String texturePath) {
    shape = scene.pApplet().loadShape(scene.pApplet().sketchPath() + shapePath);
    //TODO : TEXTURE MODE IS DISABLED DUE TO PERFORMANCE
    //shape.setTexture(scene.pApplet().loadImage( scene.pApplet().sketchPath() + texturePath));
    color = scene.pApplet().color(scene.pApplet().random(0, 255),
        scene.pApplet().random(0, 255), scene.pApplet().random(0, 255));
    shape.setFill(color);
    target = new Target(scene);

    Vector[] box = getBoundingBox(shape);
    //Scale model
    float max = PApplet.max(PApplet.abs(box[0].x() - box[1].x()),
        PApplet.abs(box[0].y() - box[1].y()),
        PApplet.abs(box[0].z() - box[1].z()));

    shape.scale(200.f * 1.f / max);
    //Invert Y Axis and set Fill
    node = new Node(scene);
    //shape.setPrecision(Node.Precision.FIXED);
    node.rotate(new Quaternion(new Vector(0, 0, 1), PApplet.PI));
    Joint root = fishSkeleton(node);
    node.setPosition(new Vector(pos.x, pos.y, pos.z));

    ArrayList<Node> skeleton = scene.branch(root);

    //Uncomment to use Linear Blending Skinning with CPU
    if (cpu) {
      skinningCPU = new LinearBlendSkinning(node, shape);
      skinningCPU.setup(skeleton);
    } else {
      skinningGPU = new LinearBlendSkinningGPU(node, skeleton);
      skinningGPU.setSkinning(scene.pApplet(), scene);
    }
    //Adding IK behavior
    target.setReference(node);
    target.setPosition(skeleton.get(skeleton.size() - 1).position());
    //Making a default Path that target must follow
    targetInterpolator = setupTargetInterpolator(target);
    scene.registerTreeSolver(root);
    scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);

  }

  public static Vector[] getBoundingBox(PShape shape) {
    Vector v[] = new Vector[2];
    float minx = 999;
    float miny = 999;
    float maxx = -999;
    float maxy = -999;
    float minz = 999;
    float maxz = -999;
    for (int j = 0; j < shape.getChildCount(); j++) {
      PShape aux = shape.getChild(j);
      for (int i = 0; i < aux.getVertexCount(); i++) {
        float x = aux.getVertex(i).x;
        float y = aux.getVertex(i).y;
        float z = aux.getVertex(i).z;
        minx = minx > x ? x : minx;
        miny = miny > y ? y : miny;
        minz = minz > z ? z : minz;
        maxx = maxx < x ? x : maxx;
        maxy = maxy < y ? y : maxy;
        maxz = maxz < z ? z : maxz;
      }
    }

    v[0] = new Vector(minx, miny, minz);
    v[1] = new Vector(maxx, maxy, maxz);
    return v;
  }

  public Joint fishSkeleton(Node reference) {
    Joint j1 = new Joint(scene);
    j1.setReference(reference);
    j1.setScaling(1.f / reference.scaling());
    j1.setPosition(0, 10.8f, 93);
    Joint j2 = new Joint(scene);
    j2.setReference(j1);
    j2.setPosition(0, 2.3f, 54.7f);
    Joint j3 = new Joint(scene);
    j3.setReference(j2);
    j3.setPosition(0, 0.4f, 22);
    Joint j4 = new Joint(scene);
    j4.setReference(j3);
    j4.setPosition(0, 0, -18);
    Joint j5 = new Joint(scene);
    j5.setReference(j4);
    j5.setPosition(0, 1.8f, -54);
    Joint j6 = new Joint(scene);
    j6.setReference(j5);
    j6.setPosition(0, -1.1f, -95);
    return j1;
  }

  public Interpolator setupTargetInterpolator(Node target) {
    Interpolator targetInterpolator = new Interpolator(target);
    targetInterpolator.setLoop();
    targetInterpolator.setSpeed(3.2f);
    // Create an initial path
    int nbKeyFrames = 4;
    float step = 2.0f * PApplet.PI / (nbKeyFrames - 1);
    for (int i = 0; i < nbKeyFrames; i++) {
      Node iFrame = new Node(scene);
      iFrame.setReference(node);
      iFrame.setPosition(new Vector(50 * PApplet.sin(step * i) + target.position().x(), target.position().y(), target.position().z()));
      targetInterpolator.addKeyFrame(iFrame);
    }
    targetInterpolator.start();
    return targetInterpolator;
  }

}