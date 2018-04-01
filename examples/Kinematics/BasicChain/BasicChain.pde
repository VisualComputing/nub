/**
 * BasicChain.
 * by Sebastian Chaparro.
 *
 * This example introduces the basic usage
 * of Inverse Kinematics methods when a
 * given Hierarchy is defined.
 * Each important step is enumerated
 */

import frames.core.*;
import frames.kinematics.*;
import frames.primitives.*;
import frames.processing.*;
import frames.primitives.constraint.*;
import frames.input.*;

Scene scene;
Node eye;
Target target;
int numJoints = 8;
float boneLength = 15;

public void setup() {
  size(700, 700, P3D);
  //1. Set a scene and an Eye
  scene = new Scene(this);
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  eye = new OrbitShape(scene);
  scene.setEye(eye);
  scene.setFieldOfView(PI / 3);
  scene.setDefaultGrabber(eye);
  scene.fitBallInterpolation();

  //2. Create the Targets
  /*
  create the Target that the End Effector must Follow.
   It is important to first create the Targets and then
   the Skeleton (Hierarchy of Joints or Nodes), otherwise
   the interaction will not be handled properly.
   */
  target = new Target(scene);
  target.translate(0, 0, 0);

  //3. Create the Skeleton (List or Hierarchy of Joints).
  ArrayList<Node> chain = generateChain(numJoints, boneLength, new Vector());
  /*Due to the way the Skeleton was built, its root will  be the first element in the list.
   Its endEffector (a leaf) will be the las element on the list
   */
  Node root = chain.get(0);
  Node endEffector = chain.get(chain.size()-1);

  //4. Tell the scene that a Solver must be register passing as parameter the root of the Skeleton in 3.
  scene.registerTreeSolver(root);

  //Optional. Set Target initial position to be the same as endEffector position
  target.setPosition(endEffector.position());

  //5. Associate the Target in 2. with the End Effector in 3.
  scene.addIKTarget(endEffector, target);

  //That's it! Enjoy using your own Hierarchies...
}

public void draw() {
  background(0);
  lights();
  scene.drawAxes();
  scene.traverse();
}

/*
given a number of Joints, a bone Length and an initial translation of
the root this method will generate a Chain of Joints
*/
ArrayList<Node> generateChain(int numJoints, float boneLength, Vector translation) {
  Joint prevFrame = null;
  Joint chainRoot = null;
  for (int i = 0; i < numJoints; i++) {
    int colour = color(random(0, 255), random(0, 255), random(0, 255), 100);
    Joint iFrame;
    iFrame = new Joint(scene, colour);
    if (i == 0)
      chainRoot = iFrame;
    if (prevFrame != null) iFrame.setReference(prevFrame);
    Vector translate = new Vector(1, 1, 1);
    translate.normalize();
    translate.multiply(boneLength);
    iFrame.setTranslation(translate);
    iFrame.setPrecision(Node.Precision.FIXED);
    prevFrame = iFrame;
  }
  chainRoot.setTranslation(translation);
  //this is required to draw the structure properly
  chainRoot.setRoot(true);
  //returns the created list
  return scene.branch(chainRoot);
}
