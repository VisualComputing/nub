package ik.basic;

import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.animation.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class Multiple_Test extends PApplet {
  Scene scene;
  int branches = 3, num = 8, depth = 3;
  List<Target> targets = new ArrayList<>();

  public void settings() {
    size(500, 500, P3D);
  }

  public void setup() {
    Joint.axes = true;
    //Setting the scene
    scene = new Scene(this);
    if (scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(2000);
    scene.fit(1);

    Joint root = new Joint();
    root.setRoot(true);
    generateTree(scene, root, branches, num, depth, 0, 0.5f * scene.radius() / ((num + 1) * depth));

    //Add IK
    scene.registerTreeSolver(root);
    //Add end effectors
    generateEFF(scene, root);


  }

  public void draw() {
    background(0);
    scene.render();
  }

  public void generateEFF(Scene scene, Node root) {
    if (root == null) ;
    if (root.children() == null || root.children().isEmpty()) {
      root.enableTagging(false);
      Target target = new Target(scene, 6);
      target.setPosition(root.position().get());
      scene.addIKTarget(root, target);
      targets.add(target);
    }

    for (Node child : root.children()) {
      generateEFF(scene, child);
    }
  }


  public Node createTarget(Scene scene, float radius) {
    /*
     * A target is a Node, we represent a Target as a
     * Red ball.
     * */
    PShape redBall;
    if (scene.is2D()) redBall = createShape(ELLIPSE, 0, 0, radius * 2, radius * 2);
    else redBall = createShape(SPHERE, radius);
    redBall.setStroke(false);
    redBall.setFill(color(255, 0, 0));

    Node target = new Node(redBall);
    //Exact picking precision
    target.setPickingThreshold(0);
    return target;
  }


  public Node generateTree(Scene scene, Node root, int branches, int num, int depth, int cur, float length) {
    if (cur - depth == 0) return root;
    float step = radians(70) / (branches / 2);
    float angle = 0;
    float l = (1f - (cur + 1f) / (depth + 1f)) * length;

    for (int i = 0; i < branches; i++) {
      //Add joint
      Node child = new Joint();
      child.setReference(root);
      child.rotate(new Vector(0, 0, 1), angle);
      child.translate(root.displacement(new Vector(0, l), child));
      for (int j = 0; j < num; j++) {
        Node next = new Joint();
        next.setReference(child);
        next.translate(new Vector(0, l));
        child = next;
      }
      angle = -angle;
      if (i % 2 == 0) {
        angle -= step;
      }

      //recursive step:
      generateTree(scene, child, branches, num, depth, cur + 1, length);
    }
    return root;
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged(MouseEvent event) {
    if (mouseButton == RIGHT && event.isControlDown()) {
      Vector vector = new Vector(scene.mouseX(), scene.mouseY());
      if (scene.node() != null)
        scene.node().interact("OnAdding", vector);
    } else if (mouseButton == LEFT) {
      scene.spin(scene.pmouseX(), scene.pmouseY(), scene.mouseX(), scene.mouseY());
    } else if (mouseButton == RIGHT) {
      scene.translate(scene.mouseX() - scene.pmouseX(), scene.mouseY() - scene.pmouseY(), 0);
      Target.multipleTranslate();
    } else if (mouseButton == CENTER) {
      scene.scale(scene.mouseDX());
    } else if (scene.node() != null)
      scene.node().interact("Reset");
    if (!Target.selectedTargets().contains(scene.node())) {
      Target.clearSelectedTargets();
    }

  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1) {
      if (event.getButton() == LEFT) {
        if (event.isControlDown()) {
          if (scene.node() != null)
            scene.node().interact("KeepSelected");
        }
      }
    } else if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.basic.Multiple_Test"});
  }

}
