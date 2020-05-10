package ik.obj;

import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.skinning.GPULinearBlendSkinning;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 10/05/19.
 */
public class Eagle extends PApplet {
  Scene scene;
  GPULinearBlendSkinning skinning;

  String shapePath = "/testing/data/objs/EAGLE_2.OBJ";
  String texturePath = "/testing/data/objs/EAGLE2.jpg";
  Node reference;


  int activeJ = 0;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    //Joint.markers = true;
    Joint.depth = false;
    //1. Create and set the scene
    scene = new Scene(this);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRightHanded();
    scene.fit(1);
    //2. Define the Skeleton
    //2.1 Define a reference node to the skeleton and the mesh
    reference = new Node();
    reference.enableTagging(false); //disable interaction
    //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
    //2.3 Create the Joints based on 2.2.
    List<Node> skeleton = buildSkeleton(reference);

    for (int i = 0; i < skeleton.size(); i++) {
      if (skeleton.get(i) instanceof Joint) {
        int c = Color.HSBtoRGB((i + 1.0f) / skeleton.size(), 1f, 1f);
        ((Joint) skeleton.get(i)).setColor((int) red(c), (int) green(c), (int) blue(c));
      }
    }

    //3. Relate the shape with a skinning method (CPU or GPU)
    skinning = new GPULinearBlendSkinning(skeleton, this.g, sketchPath() + shapePath, sketchPath() + texturePath, scene.radius());

    //4. Adding IK behavior
    //4.1 Identify root and end effector(s)
    Node root = skeleton.get(0); //root is the fist joint of the structure
    List<Node> endEffectors = new ArrayList<>(); //Ende Effectors are leaf nodes (with no children)
    for (Node node : skeleton) {
      if (node.children().size() == 0) {
        endEffectors.add(node);
      }
    }

    //4.2 relate a skeleton with an IK Solver
    scene.enableTRIK(true);
    Solver solver = scene.registerTreeSolver(root);
    //Update params
    solver.setMaxError(1f);

    for (Node endEffector : endEffectors) {
      //4.3 Create target(s) to relate with End Effector(s)
      Target target = new Target(scene, scene.radius() * 0.01f);
      target.setReference(reference); //Target also depends on reference
      target.setPosition(endEffector.position().get());
      //4.4 Relate target(s) with end effector(s)
      scene.addIKTarget(endEffector, target);
    }

    //use this method to visualize which node influences the most on a region of the mesh.
    skinning.paintAllJoints();
  }

  public void draw() {
    background(0);
    lights();
    scene.drawAxes();
    //Render mesh with respect to the node
    skinning.render(scene, reference);
    scene.render();

    //Optionally print some info:
    scene.beginHUD();
    for (int i = 0; i < skinning.skeleton().size(); i++) {
      if (skinning.skeleton().get(i).translation().magnitude() == 0) {
        continue;
      }
      fill(255);
      Vector p = scene.screenLocation(skinning.skeleton().get(i).position());
      text(skinning.ids().get(skinning.skeleton().get(i)).toString(), p.x(), p.y());

      Vector pos = skinning.skeleton().get(i).position();
      String spos = "" + Math.round(pos.x()) + ", " + Math.round(pos.y()) + ", " + Math.round(pos.z());

      //text(spos, p.x(), p.y() + 10);

    }
    String msg = activeJ == 0 ? "Painting all Joints" : activeJ == -1 ? "" : "Painting Joint : " + activeJ;
    text(msg, width / 2, height - 50);
    scene.endHUD();
    //skinning.applyTransformations();
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
      scene.scale(mouseX - pmouseX);
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

  public void keyPressed() {
    if (key == 'A' || key == 'a') {
      activeJ = (activeJ + 1) % skinning.skeleton().size();
      skinning.paintJoint(activeJ);
    }
    if (key == 's' || key == 'S') {
      activeJ = activeJ > 0 ? (activeJ - 1) : skinning.skeleton().size() - 1;
      skinning.paintJoint(activeJ);
    }
    if (key == 'd' || key == 'D') {
      skinning.disablePaintMode();
    }
  }

  //Skeleton is founded by interacting with SimpleBuilder
    /* No Local coordinate has rotation (all are aligned with respect to reference system coordinates)
        J1 |-> Node translation: [ -1.7894811E-7, -1.2377515, -1.5709928 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J2 |-> Node translation: [ 6.425498E-7, 1.2980552, 5.463369 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J3 |-> Node translation: [ 6.5103023E-7, 0.23802762, 5.4746757 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J4 |-> Node translation: [ -4.70038E-7, -2.0343544, -4.0577974 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J5 |-> Node translation: [ -4.5386977E-7, -4.236917, -4.046496 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J6 |-> Node translation: [ -6.223473E-7, 1.202842, -5.1527314 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J7 |-> Node translation: [ -7.298398E-7, -0.33323926, -6.1411514 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J8 |-> Node translation: [ -6.5542355E-7, -0.4284538, -5.5222764 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J9 |-> Node translation: [ -5.269003, 3.248286, -1.3883741 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J10 |-> Node translation: [ -12.133301, 5.3501167, 0.6687609 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J11 |-> Node translation: [ -19.107552, 5.445654, 2.483986 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
            J12 |-> Node translation: [ 8.201833, 3.9170508, -1.8660631 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                J13 |-> Node translation: [ 11.942226, 5.541193, 1.8152181 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
                    J14 |-> Node translation: [ 13.184211, 3.8215134, 2.3884451 ]rotation axis: [ 0.0, 0.0, 0.0 ]rotation angle : 0.0
    */

  public List<Node> buildSkeleton(Node reference) {
    Joint j1 = new Joint(scene.radius() * 0.01f);
    j1.setPickingThreshold(-0.01f);
    j1.setReference(reference);
    j1.setTranslation(-1.7894811E-7f, -1.2377515f, -1.5709928f);

    Joint dummy;
    dummy = new Joint(scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTagging(false);

    Joint j2 = new Joint(scene.radius() * 0.01f);
    j2.setPickingThreshold(-0.01f);
    j2.setReference(dummy);
    j2.setTranslation(6.425498E-7f, 1.2980552f, 5.463369f);
    Joint j3 = new Joint(scene.radius() * 0.01f);
    j3.setPickingThreshold(-0.01f);
    j3.setReference(j2);
    j3.setTranslation(6.5103023E-7f, 0.23802762f, 5.4746757f);


    dummy = new Joint(scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTagging(false);
    Joint j4 = new Joint(scene.radius() * 0.01f);
    j4.setPickingThreshold(-0.01f);
    j4.setReference(dummy);
    j4.setTranslation(-4.70038E-7f, -2.0343544f, -4.0577974f);
    Joint j5 = new Joint(scene.radius() * 0.01f);
    j5.setPickingThreshold(-0.01f);
    j5.setReference(j4);
    j5.setTranslation(-4.5386977E-7f, -4.236917f, -4.046496f);

    dummy = new Joint(scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTagging(false);
    Joint j6 = new Joint(scene.radius() * 0.01f);
    j6.setPickingThreshold(-0.01f);
    j6.setReference(dummy);
    j6.setTranslation(-6.223473E-7f, 1.202842f, -5.1527314f);
    Joint j7 = new Joint(scene.radius() * 0.01f);
    j7.setPickingThreshold(-0.01f);
    j7.setReference(j6);
    j7.setTranslation(-7.298398E-7f, -0.33323926f, -6.1411514f);
    Joint j8 = new Joint(scene.radius() * 0.01f);
    j8.setPickingThreshold(-0.01f);
    j8.setReference(j7);
    j8.setTranslation(-6.5542355E-7f, -0.4284538f, -5.5222764f);

    dummy = new Joint(scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTagging(false);
    Joint j9 = new Joint(scene.radius() * 0.01f);
    j9.setPickingThreshold(-0.01f);
    j9.setReference(dummy);
    j9.setTranslation(-5.269003f, 3.248286f, -1.3883741f);
    Joint j10 = new Joint(scene.radius() * 0.01f);
    j10.setPickingThreshold(-0.01f);
    j10.setReference(j9);
    j10.setTranslation(-12.133301f, 5.3501167f, 0.6687609f);
    Joint j11 = new Joint(scene.radius() * 0.01f);
    j11.setPickingThreshold(-0.01f);
    j11.setReference(j10);
    j11.setTranslation(-19.107552f, 5.445654f, 2.483986f);

    dummy = new Joint(scene.radius() * 0.01f);
    dummy.setReference(j1);
    dummy.enableTagging(false);
    Joint j12 = new Joint(scene.radius() * 0.01f);
    j12.setPickingThreshold(-0.01f);
    j12.setReference(dummy);
    j12.setTranslation(8.201833f, 3.9170508f, -1.8660631f);
    Joint j13 = new Joint(scene.radius() * 0.01f);
    j13.setPickingThreshold(-0.01f);
    j13.setReference(j12);
    j13.setTranslation(11.942226f, 5.541193f, 1.8152181f);
    Joint j14 = new Joint(scene.radius() * 0.01f);
    j14.setPickingThreshold(-0.01f);
    j14.setReference(j13);
    j14.setTranslation(13.184211f, 3.8215134f, 2.3884451f);

    j1.setRoot(true);
    return Scene.branch(j1);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.obj.Eagle"});
  }

}
