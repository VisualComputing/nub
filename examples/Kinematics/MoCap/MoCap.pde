/**
 * InteractiveFish.
 * by Sebastian Chaparro.
 *
 * This example shows how to read and display a bvh file.
 * A copy of the read Skeleton is created to use Inverse Kinematics methods.
 *
 * As the copy does not include constraints the obtained output configuration
 * is not quite good.
 *
 * Each important step is enumerated
 */

import frames.core.*;
import frames.ik.*;
import frames.primitives.*;
import frames.processing.*;
import frames.primitives.constraint.*;
import frames.input.*;
import java.util.HashMap;
import java.util.Map;

boolean read = false;
Scene scene;
Node eye;
String path = "walk-03-sneak-yokoyama.bvh";
BVHParser parser;
HashMap<String, Node> originalLimbs = new HashMap<String, Node>();
HashMap<String, Joint> limbs = new HashMap<String, Joint>();
HashMap<String, Target> targets = new HashMap<String, Target>();
Node root, rootIK;

void setup() {
  size(700, 700, P3D);
  //1. Set a scene and an Eye
  scene = new Scene(this);
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  eye = new OrbitShape(scene);
  eye.rotate(new Quaternion(new Vector(1, 0, 0), PI));
  scene.setEye(eye);
  scene.setRadius(200);
  scene.setDefaultGrabber(eye);
  scene.fitBallInterpolation();

  //2. Create the Targets to each limb
  targets.put("LEFTHAND", new Target(scene));
  targets.put("RIGHTHAND", new Target(scene));
  targets.put("LEFTFOOT", new Target(scene));
  targets.put("RIGHTFOOT", new Target(scene));
  targets.put("HEAD", new Target(scene));

  //3. Create a Parser, this class is used to read the bvh file
  parser = new BVHParser();
  //4. Built the skeleton according to bvh file Header
  root = parser.readHeader(dataPath(path), scene, null);
  //this is used to properly draw the skeleton
  ((Joint) root).setRoot(true);

  //5. make a copy of the skeleton and set Target positions to be at same position as End Effectors
  //When the program start there are 2 skeletons (one overlapping the other)
  rootIK = (Joint) copy(scene.branch(root));

  //Adding IK behavior
  //6. Tell the scene that a Solver must be register passing as parameter the root of the Skeleton
  /*
    The method registerTreeSolver returns a Solver class that could be used to modify some of its Parameters
   Here we are setting how many times the solver will be executed per Frame and which is the allowed error
   between the Target and the End Effector to consider that the solver find a solution
   */
  Solver solver = scene.registerTreeSolver(rootIK);
  solver.timesPerFrame = 100;
  solver.error = 0.05f;
  //8. Associate the Targets with the End Effectors of the Skeleton (leaf Nodes)
  scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
  scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
  scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
  scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
  scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));
}

void draw() {
  background(0);
  lights();
  //Draw Constraints
  scene.drawAxes();
  scene.traverse();
  if (read) {
    parser.readNextFrame();
    updateTargets();
  }
}

Node copy(ArrayList<Node> branch) {
  ArrayList<Node> copy = new ArrayList<Node>();
  Node reference = branch.get(0).reference();
  HashMap<Node, Node> map = new HashMap<Node, Node>();
  map.put(branch.get(0), reference);
  for (Node joint : branch) {
    Joint newJoint = new Joint(scene);
    newJoint.setReference(map.get(joint));
    newJoint.setPosition(joint.position().get());
    newJoint.setOrientation(joint.orientation().get());
    newJoint.setConstraint(joint.constraint());
    copy.add(newJoint);
    //it's no too efficient but it is just executed once
    for (Node child : joint.children()) {
      if (joint.children().size() > 1) {
        //add a new joint per child
        Node dummy = new Node(scene);
        dummy.setReference(newJoint);
        dummy.setPosition(newJoint.position());
        scene.inputHandler().removeGrabber(dummy);
        copy.add(dummy);
        map.put(child, dummy);
      } else {
        map.put(child, newJoint);
      }
    }
    if (parser._joint.get(joint)._name.equals("LEFTHAND")) {
      originalLimbs.put("LEFTHAND", joint);
      limbs.put("LEFTHAND", newJoint);
      targets.get("LEFTHAND").setPosition(newJoint.position());
    } else if (parser._joint.get(joint)._name.equals("RIGHTHAND")) {
      originalLimbs.put("RIGHTHAND", joint);
      limbs.put("RIGHTHAND", newJoint);
      targets.get("RIGHTHAND").setPosition(newJoint.position());
    } else if (parser._joint.get(joint)._name.equals("LEFTFOOT")) {
      originalLimbs.put("LEFTFOOT", joint);
      limbs.put("LEFTFOOT", newJoint);
      targets.get("LEFTFOOT").setPosition(newJoint.position());
    } else if (parser._joint.get(joint)._name.equals("RIGHTFOOT")) {
      originalLimbs.put("RIGHTFOOT", joint);
      limbs.put("RIGHTFOOT", newJoint);
      targets.get("RIGHTFOOT").setPosition(newJoint.position());
    } else if (parser._joint.get(joint)._name.equals("HEAD")) {
      originalLimbs.put("HEAD", joint);
      limbs.put("HEAD", newJoint);
      targets.get("HEAD").setPosition(newJoint.position());
    }
  }
  ((Joint) copy.get(0)).setRoot(true);
  return copy.get(0);
}

void updateTargets() {
  rootIK.setPosition(root.position());
  for (Map.Entry<String, Node> entry : originalLimbs.entrySet()) {
    targets.get(entry.getKey()).setPosition(entry.getValue().position());
  }
}

void keyPressed() {
  read = !read;
}
