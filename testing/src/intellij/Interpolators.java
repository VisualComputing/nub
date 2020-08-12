package intellij;

import nub.core.Interpolator;
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
  Interpolator shapeInterpolator, eyeInterpolator;
  Node shape;

  //Choose P3D or P2D
  String renderer = P3D;

  public void settings() {
    size(1920, 1080, renderer);
  }

  public void setup() {
    scene = new Scene(this, 150);
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
    shape.configHint(Node.BULLSEYE, color(255, 0, 0));
    shape.setBullsEyeSize(50);

    shapeInterpolator = new Interpolator(shape);
    shapeInterpolator.configHint(Interpolator.SPLINE, color(255));
    shapeInterpolator.enableRecurrence();
    // Create an initial shape interpolator path
    int count = (int) random(4, 10);
    count = 5;
    for (int i = 0; i < count; i++) {
      //shapeInterpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
      shapeInterpolator.addKeyFrame(scene.randomNode());
    }
    // decide what to reproduce along path
    //shapeInterpolator.configHint(Interpolator.STEPS, Node.AXES);
    shapeInterpolator.setSteps(1);
    shapeInterpolator.run();

    eyeInterpolator = new Interpolator(scene.eye());
    eyeInterpolator.configHint(Interpolator.SPLINE, color(255, 255, 0));

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
      shapeInterpolator.toggleHint(Interpolator.SPLINE /* | Interpolator.STEPS */);
      eyeInterpolator.toggleHint(Interpolator.SPLINE | Interpolator.STEPS);
    }
    if (key == 'r') {
      shapeInterpolator.removeKeyFrame(3);
    }
    if (key == 'p') {
      shapeInterpolator.toggle();
    }
    if (key == 'i') {
      println(shapeInterpolator.keyFrames().size());
    }
    if (key == '-' || key == '+') {
      shapeInterpolator.increaseSpeed(key == '+' ? 0.25f : -0.25f);
    }
    if (key == '1') {
      eyeInterpolator.addKeyFrame();
    }
    if (key == 'a')
      eyeInterpolator.toggle();
    if (key == 'b')
      eyeInterpolator.clear();
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
