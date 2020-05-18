package ik.constraintTest;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.animation.Joint;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;


public class SimpleHinge extends PApplet {
  Scene scene;
  Solver solver;
  Joint j1, j2;
  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(200);
    scene.fit(1);
    scene.setRightHanded();
    //Create a simple structure
    j1 = new Joint();
    j1.rotate(Quaternion.random());
    j2 = new Joint();
    j2.setReference(j1);
    j2.translate(100,0,0, 0);
    //Add a constraint
    Hinge h1 = new Hinge(radians(60), radians(60));
    h1.setRestRotation(j1.rotation(), new Vector(1,0,0), new Vector(0,0,1));
    j1.setConstraint(h1);
    List<Node> skeleton = new ArrayList<Node>();
    skeleton.add(j1);
    skeleton.add(j2);
    solver = new SimpleTRIK(skeleton, SimpleTRIK.HeuristicMode.FINAL);
    Node target = Util.createTarget(scene, scene.radius() * 0.07f);
    target.set(j2);
    solver.setTarget(j2, target);
  }

  public void draw() {
    background(0);
    lights();
    scene.drawAxes();
    scene.render();
    solver.solve();
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
      scene.mouseTranslate();
    } else {
      scene.scale(scene.mouseDX());
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }


  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.constraintTest.SimpleHinge"});
  }

}

