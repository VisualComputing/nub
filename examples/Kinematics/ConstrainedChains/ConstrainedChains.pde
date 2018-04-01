/**
 * BasicChain.
 * by Sebastian Chaparro.
 *
 * Four different chains (With different kind of constraints) are pursuing the same Target.
 *
 * This example introduces the basic usage of constraints with Inverse Kinematics methods when
 * a given Hierarchy is defined and there are some Joints (or Nodes) that use the Following
 * rotation constraints:
 *    - Hinge: 1-DOF constraint. i.e the joint will rotate just in one direction
 *      defined by a given Axis (Called Twist Axis).
 *    - Ball And Socket: 3-DOF constraint. i.e the joint could rotate in any direction but
 *      the twist axis (defined by the user) must remain inside a cone with an elliptical base.
 *      user mut specify ellipse semi-axis to constraint the movement.
 *      (for further info look at http://wiki.roblox.com/index.php?title=Inverse_kinematics)
 *    - Planar Polygon: As Ball and Socket is a 3-DOF constraint. i.e the joint could rotate in
 *      any direction but the twist axis (defined by the user) must remain inside a cone with
 *      a polygonal base. A set of vertices on XY plane in Clockwise or Counter Clockwise order must be given to
 *      constraint the movement.
 *    - Spherical Polygon: As Ball and Socket is a 3-DOF constraint. i.e the joint could rotate in
 *      any direction but the twist axis (defined by the user) must remain inside a cone defined by
 *      an spherical polygon. A set of vertices that lies in a unit sphere must be given in Counter Clockwise
 *      order  to constraint the movement.
 *      (for further info look at https://pdfs.semanticscholar.org/d535/e562effd08694821ea6a8a5769fe10ffb5b6.pdf)
 *
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
  create the Targets that the End Effectors must Follow.
   It is important to first create the Targets and then
   the Skeleton (Hierarchy of Joints or Nodes), otherwise
   the interaction will not be handled properly.
   
   Here is just one Target followed by all the End Effectors
   */

  target = new Target(scene);
  target.translate(0, 0, 0);

  //3. Create the Skeleton (List or Hierarchy of Joints).
  /*
  Here four identical chains that will have different constraints
   generateChain method just create a chain of Joints with the specified number of joints, bonelength per Joint.
   The initial root position of the created chain is determined by a Vector.
   */
  ArrayList<Node> chainHinge = generateChain(numJoints, boneLength, new Vector(-scene.radius(), -scene.radius(), 0));
  ArrayList<Node> chainBallSocket = generateChain(numJoints, boneLength, new Vector(-scene.radius(), scene.radius(), 0));
  ArrayList<Node> chainPlanarPoly = generateChain(numJoints, boneLength, new Vector(scene.radius(), -scene.radius(), 0));
  ArrayList<Node> chainSphericalPoly = generateChain(numJoints, boneLength, new Vector(scene.radius(), scene.radius(), 0));

  //4. Apply Constraints
  //Hinge constraints
  for (int i = 1; i < chainHinge.size() - 1; i++) {
    //New Hinge constraint with minAngle = -50, maxAngle = 50
    Hinge constraint = new Hinge(radians(50), radians(50));
    //A rest rotation must be given always! it could be determined by the current Joint Rotation
    constraint.setRestRotation(chainHinge.get(i).rotation().get());
    //An axis pointing to Y Direction and Perpendicular to Child Translation
    //This Axis must be specified in terms of restRotation coordinates.
    constraint.setAxis(Vector.projectVectorOnPlane(new Vector(0, 1, 0), chainHinge.get(i+1).translation()));
    chainHinge.get(i).setConstraint(constraint);
  }

  //Ball and Socket Constraints
  for (int i = 1; i < chainBallSocket.size() - 1; i++) {
    //New Ball and Socket constraint (for further info look at http://wiki.roblox.com/index.php?title=Inverse_kinematics)
    BallAndSocket constraint = new BallAndSocket(radians(50), radians(50), radians(50), radians(50));
    Vector twist = chainBallSocket.get(i+1).translation().get();
    //A rest rotation must be given always! it determines where the Twist Axis will point to.
    //Here Twist Axis points to Child (it is the most common case)
    constraint.setRestRotation(chainBallSocket.get(i).rotation().get(), new Vector(0, 1, 0), twist);
    chainBallSocket.get(i).setConstraint(constraint);
  }

  //Planar Polygon constraints
  //Define the Base (Any Polygon in clockwise or Counterclockwise order) lying on XY Plane
  ArrayList<Vector> vertices = new ArrayList<Vector>();
  vertices.add(new Vector(-10, -10));
  vertices.add(new Vector(10, -10));
  vertices.add(new Vector(10, 10));
  vertices.add(new Vector(-10, 10));

  for (int i = 1; i < chainPlanarPoly.size() - 1; i++) {
    //New Planar Polygon constraint, it constrains the movement of Twist axis inside a Cone with a Polygonal Constraint
    PlanarPolygon constraint = new PlanarPolygon(vertices);
    constraint.setHeight(boneLength / 2.f);
    Vector twist = chainPlanarPoly.get(i+1).translation().get();
    //A rest rotation must be given always! it determines where the Twist Axis will point to.
    //Here Twist Axis points to Child (it is the most common case)
    constraint.setRestRotation(chainPlanarPoly.get(i).rotation().get(), new Vector(0, 1, 0), twist);
    chainPlanarPoly.get(i).setConstraint(constraint);
  }

  //Define the Base (Any Polygon in Counterclockwise order)
  //Define the vertices that define the cone
  ArrayList<Vector> verticesSpherical = new ArrayList<Vector>();
  int numVertices = 12;
  for (int i = 0; i < numVertices; i++) {
    float step = i * (2 * PI / (float) numVertices);
    //here the base of the cone is not planar, but it could be defined on the unit sphere
    verticesSpherical.add(new Vector(cos(step), sin(step), random(0, 1)));
  }

  for (int i = 1; i < chainSphericalPoly.size() - 1; i++) {
    /* New Spherical Polygon constraint.
     It constrains the movement of Twist axis inside a Cone with a base defined by the vertices at verticesSpherical
     */
    SphericalPolygon constraint = new SphericalPolygon(verticesSpherical);
    //A rest rotation must be given always! it determines where the Twist Axis will point to.
    //Here Twist Axis points to Child (it is the most common case)
    Vector twist = chainSphericalPoly.get(i+1).translation().get();
    constraint.setRestRotation(chainSphericalPoly.get(i).rotation().get(), new Vector(0, 1, 0), twist);
    chainSphericalPoly.get(i).setConstraint(constraint);
  }


  //5. Tell the scene that a Solver must be register passing as parameter the root of each Skeleton (chain) in 3.
  /*
    The method registerTreeSolver returns a Solver class that could be used to modify some of its Parameters
   Here we use the default Solver configuration
   */
  //6. After 4. is applied to each chain associate the Target in 2. with the End Effector of the chain (leaf Node)

  //Apply 5. and 6. to each chain
  scene.registerTreeSolver(chainHinge.get(0));
  scene.addIKTarget(chainHinge.get(chainHinge.size() - 1), target);

  Solver solverEllipseConstraint = scene.registerTreeSolver(chainBallSocket.get(0));
  scene.addIKTarget(chainBallSocket.get(chainBallSocket.size() - 1), target);

  Solver solverPlanarConstraints = scene.registerTreeSolver(chainPlanarPoly.get(0));
  scene.addIKTarget(chainPlanarPoly.get(chainPlanarPoly.size() - 1), target);

  Solver solverSphericalConstraints = scene.registerTreeSolver(chainSphericalPoly.get(0));
  scene.addIKTarget(chainSphericalPoly.get(chainSphericalPoly.size() - 1), target);
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
