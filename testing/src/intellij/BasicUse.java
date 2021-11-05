package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class BasicUse extends PApplet {
  Scene scene;
  Node node, child, sibling;
  Vector axis;

  //Choose P3D for a 3D scene, or P2D for a 2D one
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    scene = new Scene(this, 150);
    scene.fit(1000);
    //node = new Node();
    node = new Node((pg) -> {
      pg.pushStyle();
      //pg.fill(0, 255, 255, 125);
      pg.fill(0, 255, 255 /*, 125*/);
      pg.stroke(0, 0, 255);
      pg.strokeWeight(2);
      pg.popStyle();
    });
    node.translate(50, 50, 50);
    // node.disableHint(Node.IMR);
    node.enableHint(Node.AXES);
    node.enableHint(Node.BULLSEYE);
    child = new Node(node);
    child.enableHint(Node.AXES | Node.TORUS);
    child.translate(-25, -75, 50);
    child.rotate(Quaternion.random());
    //sibling = child.get();
    //sibling.setReference(node);
    sibling = new Node();
    sibling.setReference(node);
    sibling.set(child);
    //child
    //node.setPickingPolicy(Node.PickingPolicy.BULLS_EYE);
    //node.setBullsEyeSize(50);
    node.configHint(Node.BULLSEYE, Node.BullsEyeShape.CIRCLE);
    node.configHint(Node.BULLSEYE, color(0, 255, 0));
    node.enableHint(Node.CAMERA, color(255, 255, 0), scene.radius() * 2);
    //scene.enableHint(Graph.BACKGROUND, color(100, 155, 255));
    randomize();
  }

  public void draw() {
    background(125);
    scene.render();
    noStroke();
    fill(0, 255, 255);
    scene.drawArrow(axis);
  }

  public void mouseMoved() {
    scene.updateTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT) {
      //Vector v = new Vector(scene.mouseDX(), 0, 0);
      //child.translate(v);

      //child.rotate(Quaternion.from(child.displacement(axis), scene.mouseRADX()));
      // works
      //sibling.rotate(Quaternion.from(sibling.displacement(axis), scene.mouseRADX()));
      // fails
      //sibling.rotate(sibling.displacement(Quaternion.from(axis, scene.mouseRADX())));

      scene.turn(0.0f, scene.mouseRADX(), 0.0f);
      /*
      //scene.mouseTranslate();
      //scene.rotate(0, scene.mouseRADX(), 0);
      Node node = scene.node();
      if (node == child || node == sibling) {
        //Vector v = node.reference().displacement(axis);
        //node.rotate(Quaternion.from(v, scene.mouseRADX()));
        Quaternion q = Quaternion.from(axis, 0.5f * scene.mouseRADX());//node.reference().displacement(axis);
        node.rotate(node.reference().displacement(q));
      }
      // */
    }
    else
      scene.shift();
      //scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  public void randomize() {
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void keyPressed() {
    if (key == 'a')
      randomize();
    if (key == ' ')
      node.resetHint();
    if (key == '1')
      node.toggleHint(Node.AXES);
    if (key == '2')
      node.toggleHint(Node.CAMERA);
    if (key == '3')
      node.toggleHint(Node.BULLSEYE);
    if (key == '4')
      node.toggleHint(Node.SHAPE);
    //if (key == '5')
      //node.toggleHint(Node.FRUSTUM);
    if (key == '6')
      node.toggleHint(Node.TORUS);
    if (key == '7')
      node.toggleHint(Node.FILTER);
    if (key == '8')
      node.toggleHint(Node.BONE);

    if (key == 's')
      scene.fit(1000);
    if (key == 'f')
      scene.fit();

    if (key == 'p') {
      println(Scene.nodes().size());
      println("node hint: " + node.hint());
      if (node.isHintEnabled(Node.AXES))
        println("Node.AXES");
      if (node.isHintEnabled(Node.CAMERA))
        println("Node.CAMERA");
      if (node.isHintEnabled(Node.BULLSEYE))
        println("Node.BULLS_EYE");
      if (node.isHintEnabled(Node.SHAPE))
        println("Node.SHAPE");
      //if (node.isHintEnable(Node.FRUSTUM))
        //println("Node.FRUSTUM");
      if (node.isHintEnabled(Node.TORUS))
        println("Node.TORUS");
      if (node.isHintEnabled(Node.FILTER))
        println("Node.CONSTRAINT");
      if (node.isHintEnabled(Node.BONE))
        println("Node.BONE");
    }
    if (key == 'r') {
      Quaternion q = Quaternion.random();
      child.setWorldOrientation(q);
      /*
      sibling.resetReference();
      sibling.setRotation(q);
      sibling.setReference(node);
      // */
      sibling.setOrientation(node.displacement(q));
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.BasicUse"});
  }
}
