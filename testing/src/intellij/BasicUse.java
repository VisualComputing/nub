package intellij;

import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class BasicUse extends PApplet {
  Scene scene;
  Node node;

  //Choose P3D for a 3D scene, or P2D for a 2D one
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    scene = new Scene(this, 150);
    scene.fit(1);
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
    //node.setPickingPolicy(Node.PickingPolicy.BULLS_EYE);
    //node.setBullsEyeSize(50);
    node.configHint(Node.BULLSEYE, Node.BullsEyeShape.CIRCLE);
    node.configHint(Node.BULLSEYE, color(0, 255, 0));
    node.enableHint(Node.CAMERA, color(255, 255, 0), scene.radius() * 2);
    scene.enableHint(Scene.AXES | Scene.GRID);
    scene.configHint(Scene.GRID, color(255, 0, 0));
    //scene.enableHint(Graph.BACKGROUND, color(100, 155, 255));
  }

  public void draw() {
    background(125);
    scene.render();
  }

  public void mouseMoved() {
    scene.updateMouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.scaleEye(event.getCount() * 20);
  }

  public void keyPressed() {
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
      node.toggleHint(Node.CONSTRAINT);
    if (key == '8')
      node.toggleHint(Node.BONE);

    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();

    if (key == 'x')
      for (Task task : Scene.TimingHandler.tasks())
        task.enableConcurrence();
    if (key == 'y')
      for (Task task : Scene.TimingHandler.tasks())
        task.disableConcurrence();
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
      if (node.isHintEnabled(Node.CONSTRAINT))
        println("Node.CONSTRAINT");
      if (node.isHintEnabled(Node.BONE))
        println("Node.BONE");
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.BasicUse"});
  }
}
