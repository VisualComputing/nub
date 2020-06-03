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
    scene = new Scene(this);
    scene.setRadius(150);
    scene.fit(1);
    node = new Node();
    node.translate(50, 50, 50);
    node.enableHint(Node.AXES);
    node.enableHint(Node.BULLS_EYE);
    node.setPickingPolicy(Node.PickingPolicy.BULLS_EYE);
    node.setBullsEyeShape(Node.BullsEyeShape.CIRCLE);
    node.configHint(Node.BULLS_EYE, color(0, 255, 0));
    node.enableHint(Node.CAMERA, color(255, 255, 0), scene.radius() * 2);
    //node.setBullsEyeSize(50);
    scene.setHint(Scene.AXES | Scene.GRID);
    scene.configHint(Scene.GRID, color(255, 0, 0));
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
    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();

    if (key == 'x')
      for (Task task : Scene.timingHandler().tasks())
        task.enableConcurrence();
    if (key == 'y')
      for (Task task : Scene.timingHandler().tasks())
        task.disableConcurrence();
    if (key == 'p')
      println(Scene.nodes().size());
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.BasicUse"});
  }
}
