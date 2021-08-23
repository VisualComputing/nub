package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class Interpolators extends PApplet {
  Scene scene;
  Node shape;

  //Choose P3D or P2D
  String renderer = P3D;

  public void settings() {
    size(1920, 1080, renderer);
  }

  public void setup() {
    scene = new Scene(this, 150);
    println(scene.fov());
    println(scene.eye().worldMagnitude());
    PShape pshape;
    if (scene.is2D()) {
      rectMode(CENTER);
      pshape = createShape(RECT, 0, 0, 100, 100);
    }
    else {
      pshape = createShape(BOX, 30);
    }
    pshape.setFill(color(0, 255, 255, 125));
    shape = new Node(pshape);
    shape.enableHint(Node.AXES);
    shape.enableHint(Node.BULLSEYE);
    shape.enableHint(Node.ANIMATION);
    shape.configHint(Node.BULLSEYE, color(255, 0, 0));
    shape.setBullsEyeSize(50);

    //shapeInterpolator.configHint(Interpolator.SPLINE, color(255));
    // Create an initial shape interpolator path
    int count = (int) random(4, 10);
    count = 5;
    for (int i = 0; i < count; i++) {
      //shape.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
      /*
      Node node = scene.randomNode();
      node.enableHint(Node.BULLSEYE);
      shape.addKeyFrame(node, i % 2 == 1 ? 1 : 4);
      // */
      // /*
      scene.randomize(shape);
      shape.addKeyFrame(Node.AXES | Node.SHAPE, i % 2 == 1 ? 1 : 4);
      // */
    }

    shape.setAnimationRecurrence(true);
    shape.resetAnimationHint();
    shape.enableAnimationHint(Node.AXES);
    // decide what to reproduce along path
    //shapeInterpolator.configHint(Interpolator.STEPS, Node.AXES);
    //shapeInterpolator.setSteps(1);
    shape.animate();

    //eyeInterpolator.configHint(Interpolator.SPLINE, color(255, 255, 0));

    scene.enableHint(Scene.AXES | Scene.GRID);
    scene.configHint(Scene.GRID, color(0, 255, 0));
    scene.enableHint(Scene.BACKGROUND, color(125));

    scene.eye().configHint(Node.CAMERA, color(0, 255, 0));

    frameRate(1000);
  }

  public void draw() {
    scene.render();
    // println("-> frameRate: " + Scene.TimingHandler.frameRate + " (nub) " + frameRate + " (p5)");
  }

  public void mouseMoved() {
    scene.updateMouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseShift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  float speed = 1;

  public void keyPressed() {
    if (key == 'c') {
      println(Graph.nodes().size());
      shape.removeKeyFrame(1);
      println(Graph.nodes().size());
    }
    if (key == 't') {
      shape.resetAnimationHint();
      shape.enableAnimationHint(Node.CAMERA);
    }
    if (key == 'z') {
      println(scene.fov());
      println(scene.eye().worldMagnitude());
    }
    if (key == ' ') {
      shape.toggleHint(Node.ANIMATION);
      //shapeInterpolator.toggleHint(Interpolator.SPLINE /* | Interpolator.STEPS */);
      //eyeInterpolator.toggleHint(Interpolator.SPLINE | Interpolator.STEPS);
    }
    if (key == 'r') {
      shape.removeKeyFrame(3);
    }
    if (key == 'p') {
      shape.toggleAnimation();
    }
    if (key == '-' || key == '+') {
      speed += key == '+' ? 0.25f : -0.25f;
      shape.animate(speed);
      //shapeInterpolator.increaseSpeed(key == '+' ? 0.25f : -0.25f);
    }
    /*
    if (key == '1') {
      eyeInterpolator.addKeyFrame();
    }
    if (key == 'a')
      eyeInterpolator.toggle();
    if (key == 'b')
      eyeInterpolator.clear();

     */
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
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Interpolators"});
  }
}
