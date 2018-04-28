package ik.basic;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.Solver;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import processing.core.PApplet;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 29/03/18.
 */
public class MultipleEndEffectors extends PApplet {
  Scene scene;
  Node eye;
  Target leftTarget, rightTarget;
  ArrayList<Node> leftLeg;
  ArrayList<Node> rightLeg;

  public static float boneLength = 15;

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

    leftTarget = new Target(scene);
    rightTarget = new Target(scene);

    Joint root = new Joint(scene);
    root.setRoot(true);
    Node root1 = new Node(scene);
    root1.setReference(root);
    scene.inputHandler().removeGrabber(root1);
    Node root2 = new Node(scene);
    root2.setReference(root);
    scene.inputHandler().removeGrabber(root2);

    Joint left1 = new Joint(scene);
    left1.setReference(root1);
    left1.setPosition(-20, -20, 0);

    Joint right1 = new Joint(scene);
    right1.setReference(root2);
    right1.setPosition(20, 20, 0);

    leftTarget.setPosition(left1.position());
    rightTarget.setPosition(right1.position());

    Solver solver = scene.registerTreeSolver(root);
    scene.addIKTarget(left1, leftTarget);
    scene.addIKTarget(right1, rightTarget);
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
    PApplet.main(new String[]{"ik.basic.MultipleEndEffectors"});
  }

}