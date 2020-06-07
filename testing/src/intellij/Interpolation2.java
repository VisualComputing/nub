package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class Interpolation2 extends PApplet {
  Scene scene;
  Interpolator interpolator;
  boolean displayInterpolator;
  Node shape;
  float speed = 1;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(150);
    scene.fit(1);
    shape = new Node((pg) -> {
      pg.pushStyle();
      //pg.fill(0, 255, 255, 125);
      pg.fill(0, 255, 255 /*, 125*/);
      pg.stroke(0, 0, 255);
      pg.strokeWeight(2);
      if (pg.is2D())
        pg.rect(0, 0, 100, 100);
      else
        pg.box(30);
      pg.popStyle();
    });
    /*
    shape = new Node();
    shape.setIMRShape((pg) -> {
      pg.pushStyle();
      pg.fill(0, 255, 255, 125);
      pg.stroke(0, 0, 255);
      pg.strokeWeight(2);
      if (pg.is2D())
        pg.rect(0, 0, 100, 100);
      else
        pg.box(30);
      pg.popStyle();
    });
    // */
    interpolator = new Interpolator(shape);
    interpolator.enableRecurrence();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++) {
      Node node = scene.randomNode();
      node.disableTagging();
      node.setPickingPolicy(Node.PickingPolicy.BULLS_EYE);
      interpolator.addKeyFrame(node, i % 2 == 1 ? 1 : 4);
    }
    interpolator.run();


    //shape.disableHint(Node.IMR);
    shape.enableHint(Node.AXES);
    shape.enableHint(Node.BULLS_EYE);
    shape.configHint(Node.BULLS_EYE, color(255, 0, 0));
    shape.enableHint(Node.CAMERA);
    shape.configHint(Node.CAMERA, color(255, 0, 255), scene.radius() * 2);
    shape.setBullsEyeSize(50);

    scene.enableHint(Scene.AXES | Scene.GRID);
    scene.configHint(Scene.GRID, color(0, 255, 0));
    scene.enableHint(Scene.BACKGROUND, color(125));

    interpolator.configHint(Interpolator.SPLINE, color(255));
    //interpolator.configHint(Interpolator.CAMERA, color(0, 255, 0));
  }

  public void draw() {
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
    if (key == ' ') {
      displayInterpolator = !displayInterpolator;
      if (displayInterpolator) {
        interpolator.enableHint(Interpolator.SPLINE | Interpolator.AXES);
        for (Node node : interpolator.keyFrames().values()) {
          node.enableTagging();
          node.enableHint(Node.BULLS_EYE);
        }
      } else {
        interpolator.disableHint(Interpolator.SPLINE | Interpolator.AXES);
        for (Node node : interpolator.keyFrames().values()) {
          node.disableTagging();
          node.disableHint(Node.BULLS_EYE);
        }
      }
    }

    if (key == '-' || key == '+') {
      if (key == '-')
        speed -= 0.25f;
      else
        speed += 0.25f;
      interpolator.run(speed);
    }

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
    PApplet.main(new String[]{"intellij.Interpolation2"});
  }
}
