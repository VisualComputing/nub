package ik.basic;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Vector;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.Hinge;
import frames.primitives.constraint.PlanarPolygon;
import frames.primitives.constraint.SphericalPolygon;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import processing.core.PApplet;

import java.util.ArrayList;
/*
 * Tree different chains (With different kind of constraints) are pursuing the same Target
 * */

public class BasicIK extends PApplet {
  Scene scene;
  Node eye;
  Target target;

  int num_joints = 8;

  //Ball and Socket
  public static float constraint_factor_x = 50;
  public static float constraint_factor_y = 50;

  public static float boneLength = 15;

  int TimesPerFrame = 1;


  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    eye = new InteractiveNode(scene);

    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultNode(eye);
    scene.fitBallInterpolation();

    target = new Target(scene);
    target.translate(0, 0, 0);

    //Four identical chains that will have different constraints
    ArrayList<Node> branchHingeConstraint = generateChain(num_joints, boneLength, new Vector(-scene.radius(), -scene.radius(), 0));
    ArrayList<Node> branchEllipseConstraint = generateChain(num_joints, boneLength, new Vector(-scene.radius(), scene.radius(), 0));
    ArrayList<Node> branchPlanarConstraints = generateChain(num_joints, boneLength, new Vector(scene.radius(), -scene.radius(), 0));
    ArrayList<Node> branchSphericalConstraints = generateChain(num_joints, boneLength, new Vector(scene.radius(), scene.radius(), 0));

    //Apply Constraints
    //Hinge constraints
    for (int i = 1; i < branchHingeConstraint.size() - 1; i++) {
      Hinge constraint = new Hinge(radians(constraint_factor_x), radians(constraint_factor_x));
      constraint.setRestRotation(branchHingeConstraint.get(i).rotation().get());
      constraint.setAxis(Vector.projectVectorOnPlane(new Vector(0, 1, 0), branchHingeConstraint.get(i + 1).translation()));
      branchHingeConstraint.get(i).setConstraint(constraint);
    }

    //Ellipse Constraints
    for (int i = 1; i < branchEllipseConstraint.size() - 1; i++) {
      BallAndSocket constraint = new BallAndSocket(radians(constraint_factor_y), radians(constraint_factor_y), radians(constraint_factor_x), radians(constraint_factor_x));
      Vector twist = branchEllipseConstraint.get(i + 1).translation().get();
      constraint.setRestRotation(branchEllipseConstraint.get(i).rotation().get(), new Vector(0, 1, 0), twist);
      branchEllipseConstraint.get(i).setConstraint(constraint);
    }

    //Sinus cone planar Polygon constraints

    //Define the Base (Any Polygon in clockwise or Counterclockwise order)
    ArrayList<Vector> vertices = new ArrayList<Vector>();
    vertices.add(new Vector(-10, -10));
    vertices.add(new Vector(10, -10));
    vertices.add(new Vector(10, 10));
    vertices.add(new Vector(-10, 10));

    for (int i = 1; i < branchPlanarConstraints.size() - 1; i++) {
      PlanarPolygon constraint = new PlanarPolygon(vertices);
      constraint.setHeight(boneLength / 2.f);
      Vector twist = branchPlanarConstraints.get(i + 1).translation().get();
      constraint.setRestRotation(branchPlanarConstraints.get(i).rotation().get(), new Vector(0, 1, 0), twist);
      branchPlanarConstraints.get(i).setConstraint(constraint);
    }

    //Define the Base (Any Polygon in Counterclockwise order)
    ArrayList<Vector> verticesSpherical = new ArrayList<Vector>();
    int numVertices = 12;
    for (int i = 0; i < numVertices; i++) {
      float step = i * (2 * PI / (float) numVertices);
      verticesSpherical.add(new Vector(cos(step), sin(step), random(0, 1)));
    }

    for (int i = 1; i < branchSphericalConstraints.size() - 1; i++) {
      SphericalPolygon constraint = new SphericalPolygon(verticesSpherical);
      Vector twist = branchSphericalConstraints.get(i + 1).translation().get();
      constraint.setRestRotation(branchSphericalConstraints.get(i).rotation().get(), new Vector(0, 1, 0), twist);
      branchSphericalConstraints.get(i).setConstraint(constraint);
    }

    Solver solverUnconstrained = scene.registerTreeSolver(branchHingeConstraint.get(0));
    scene.addIKTarget(branchHingeConstraint.get(branchHingeConstraint.size() - 1), target);

    Solver solverEllipseConstraint = scene.registerTreeSolver(branchEllipseConstraint.get(0));
    scene.addIKTarget(branchEllipseConstraint.get(branchEllipseConstraint.size() - 1), target);

    Solver solverPlanarConstraints = scene.registerTreeSolver(branchPlanarConstraints.get(0));
    scene.addIKTarget(branchPlanarConstraints.get(branchPlanarConstraints.size() - 1), target);

    Solver solverSphericalConstraints = scene.registerTreeSolver(branchSphericalConstraints.get(0));
    scene.addIKTarget(branchSphericalConstraints.get(branchSphericalConstraints.size() - 1), target);
  }

  public ArrayList<Node> generateChain(int num_joints, float boneLength, Vector translation) {
    Joint prevFrame = null;
    Joint chainRoot = null;
    int color = color(random(0, 255), random(0, 255), random(0, 255), 100);
    for (int i = 0; i < num_joints; i++) {
      Joint iFrame;
      iFrame = new Joint(scene, color);
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
    //Consider Standard Form: Parent Z Axis is Pointing at its Child
    chainRoot.setTranslation(translation);
    //chainRoot.setupHierarchy();
    chainRoot.setRoot(true);
    return scene.branch(chainRoot);
  }

  public void draw() {
    background(0);
    lights();
    //Draw Constraints
    scene.drawAxes();

    for (Node frame : scene.nodes()) {
      if (frame instanceof Shape) ((Shape) frame).draw();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.basic.BasicIK"});
  }

}