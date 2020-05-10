package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.List;

public class SimpleSkeleton extends PApplet {
  Scene scene;
  //Set the scene as P3D or P2D
  //Skeleton structure defined above
  List<Node> skeleton;


  String shapePath = "/testing/data/objs/cup.obj";
  Node target;

  public void settings() {
    size(700, 700, P3D);
  }

  public void setup() {
    //Setting the scene
    scene = new Scene(this);
    if (scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(100);
    scene.fit(1);
    //1. Create the Skeleton
    Node root = createSkeleton(null);
    skeleton = scene.branch(root);
    root.rotate(new Quaternion(new Vector(1, 0, 0), PI));
    PShape cup = loadShape(sketchPath() + shapePath);
    cup.setFill(color(218, 165, 32));
    cup.rotateX(PI / 2);
    cup.scale(0.25f);
    target = new Node(cup);
    target.setPickingThreshold(0);
    target.setPosition(9.889991f, -38.6116f);

    //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
    skeleton.get(6).enableTagging(false);

    //Locate the Targets on same spot of the end effectors
    //rightTarget.setPosition(skeleton[6].position());

    //3. Relate the structure with a Solver. In this example we register a solver in the graph scene
    Solver solver = scene.registerTreeSolver(skeleton.get(4));
    //Optionally you could modify the following parameters of the Solver:
    //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
    solver.setMaxError(1);
    //Number of iterations to perform in order to reach the target (Default value is 50)
    solver.setMaxIterations(15);
    //Times a solver will iterate on a single Frame (Default value is 5)
    solver.setTimesPerFrame(5);
    //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
    solver.setMinDistance(0.5f);

    //4. relate targets with end effectors


    //Define Text Properties
    textAlign(CENTER);
    textSize(14);
  }

  public void draw() {
    background(0);
    if (scene.is3D()) lights();
    //scene.drawAxes();
    scene.render();
    scene.beginHUD();
    for (int i = 0; i < skeleton.size(); i++) {
      //Print Node names
      Vector screenLocation = scene.screenLocation(skeleton.get(i).position());
      text(i, screenLocation.x() - 14, screenLocation.y() - 20);
    }

    Vector screenLocation = scene.screenLocation(target.position());
    text("Target", screenLocation.x(), screenLocation.y() + 20);


    scene.endHUD();

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
    if (key == '1') {
      Quaternion q = new Quaternion(new Vector(0, 0, 1), radians(5));
      q = Quaternion.compose(skeleton.get(4).orientation(), q);
      skeleton.get(4).setOrientation(q);
    }
    if (key == '2') {
      Quaternion q = new Quaternion(new Vector(0, 0, 1), radians(5));
      q = Quaternion.compose(skeleton.get(5).orientation(), q);
      skeleton.get(5).setOrientation(q);
    }
    if (key == '3') {
      Quaternion q = new Quaternion(new Vector(0, 0, 1), radians(-5));
      q = Quaternion.compose(skeleton.get(4).orientation(), q);
      skeleton.get(4).setOrientation(q);
    }
    if (key == '4') {
      Quaternion q = new Quaternion(new Vector(0, 0, 1), radians(-5));
      q = Quaternion.compose(skeleton.get(5).orientation(), q);
      skeleton.get(5).setOrientation(q);
    }
    //scene.addIKTarget(skeleton.get(6), target);
    System.out.println("Target pos : " + skeleton.get(6).position());
  }

  public Node createSkeleton(Node reference) {
    Joint j1 = new Joint(scene.radius() * 0.01f);
    j1.setPickingThreshold(-0.01f);
    j1.setReference(reference);
    j1.setTranslation(0.0f, 0.0f, 0.0f);
    j1.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j2 = new Joint(scene.radius() * 0.01f);
    j2.setPickingThreshold(-0.01f);
    j2.setReference(j1);
    j2.setTranslation(0.0f, 10.796892f, 0.0f);
    j2.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j4 = new Joint(scene.radius() * 0.01f);
    j4.setPickingThreshold(-0.01f);
    j4.setReference(j1);
    j4.setTranslation(-0.1384217f, 21.593784f, 0.0f);
    j4.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j5 = new Joint(scene.radius() * 0.01f);
    j5.setPickingThreshold(-0.01f);
    j5.setReference(j4);
    j5.setTranslation(-0.075141065f, 11.19239f, 0.0f);
    j5.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j10 = new Joint(scene.radius() * 0.01f);
    j10.setPickingThreshold(-0.01f);
    j10.setReference(j4);
    j10.setTranslation(7.905712f, -0.17451948f, 0.0f);
    j10.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j11 = new Joint(scene.radius() * 0.01f);
    j11.setPickingThreshold(-0.01f);
    j11.setReference(j10);
    j11.setTranslation(8.117025f, -7.732076f, 0.0f);
    j11.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j12 = new Joint(scene.radius() * 0.01f);
    j12.setPickingThreshold(-0.01f);
    j12.setReference(j11);
    j12.setTranslation(3.5418904f, -11.074137f, 0.0f);
    j12.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j43 = new Joint(scene.radius() * 0.01f);
    j43.setPickingThreshold(-0.01f);
    j43.setReference(j4);
    j43.setTranslation(-8.640845f, -0.07596502f, 0.0f);
    j43.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j44 = new Joint(scene.radius() * 0.01f);
    j44.setPickingThreshold(-0.01f);
    j44.setReference(j43);
    j44.setTranslation(-9.010527f, -7.1288486f, 0.0f);
    j44.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j45 = new Joint(scene.radius() * 0.01f);
    j45.setPickingThreshold(-0.01f);
    j45.setReference(j44);
    j45.setTranslation(-4.7031236f, -10.25346f, 0.0f);
    j45.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j178 = new Joint(scene.radius() * 0.01f);
    j178.setPickingThreshold(-0.01f);
    j178.setReference(j1);
    j178.setTranslation(-7.33635f, -7.059507f, -0.0f);
    j178.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j179 = new Joint(scene.radius() * 0.01f);
    j179.setPickingThreshold(-0.01f);
    j179.setReference(j178);
    j179.setTranslation(-0.93631315f, -12.508251f, 0.0f);
    j179.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j180 = new Joint(scene.radius() * 0.01f);
    j180.setPickingThreshold(-0.01f);
    j180.setReference(j179);
    j180.setTranslation(-0.26124993f, -9.927497f, -0.0f);
    j180.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j715 = new Joint(scene.radius() * 0.01f);
    j715.setPickingThreshold(-0.01f);
    j715.setReference(j1);
    j715.setTranslation(8.997411f, -7.1979284f, 0.0f);
    j715.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j716 = new Joint(scene.radius() * 0.01f);
    j716.setPickingThreshold(-0.01f);
    j716.setReference(j715);
    j716.setTranslation(-0.29267085f, -12.762048f, 0.0f);
    j716.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    Joint j717 = new Joint(scene.radius() * 0.01f);
    j717.setPickingThreshold(-0.01f);
    j717.setReference(j716);
    j717.setTranslation(0.37650722f, -9.79303f, 0.0f);
    j717.setRotation(new Quaternion(new Vector(0.0f, 0.0f, 0.0f), 0.0f));
    return j1;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.basic.SimpleSkeleton"});
  }
}
