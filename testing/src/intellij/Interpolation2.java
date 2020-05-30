package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.function.Function;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class Interpolation2 extends PApplet {
  Scene scene;
  Interpolator interpolator, eyeInterpolator1, eyeInterpolator2;
  Node shape;
  boolean showEyePath = true;
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

    // interpolation 1. Default eye interpolations
    scene.fit(1);

    // interpolation 2. Custom eye interpolations
    eyeInterpolator1 = new Interpolator(scene.eye());
    eyeInterpolator2 = new Interpolator(scene.eye());

    // interpolation 3. Custom (arbitrary) node interpolations

    /*
    shape = new Node() {
      // Note that within render() geometry is defined at the
      // node local coordinate system.
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 255, 255, 125);
        pg.stroke(0, 0, 255);
        pg.strokeWeight(2);
        if (pg.is2D())
          pg.rect(0, 0, 100, 100);
        else
          pg.box(30);
        pg.popStyle();
      }
    };
    // */
    shape = new Node((pg) -> {
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
    /*
    shape = new Node();
    shape.hint((pg) -> {
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
    */
    interpolator = new Interpolator(shape);
    interpolator.enableRecurrence();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      interpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
    interpolator.run();

    /*
    println(shape.isHintEnable(Node.AXES) ? "yep axes" : "nope exes");
    println(shape.isHintEnable(Node.BULLS_EYE) ? "yep bulls" : "nope bulls");
    shape.setVisualHint(Node.AXES | Node.BULLS_EYE);
    println(shape.isHintEnable(Node.AXES) ? "yep axes" : "nope exes");
    println(shape.isHintEnable(Node.BULLS_EYE) ? "yep bulls" : "nope bulls");
    shape.toggleHint(Node.AXES);
    println(shape.isHintEnable(Node.AXES) ? "yep axes" : "nope exes");
    println(shape.isHintEnable(Node.BULLS_EYE) ? "yep bulls" : "nope bulls");
     */
    shape.disableHint(Node.IMR);
    //shape.configHint(Node.AXES, 500);
    shape.enableHint(Node.AXES);
  }

  public void draw() {
    background(125);
    scene.render();

    pushStyle();
    stroke(255);
    // same as:scene.drawCatmullRom(interpolator, 5);
    scene.drawCatmullRom(interpolator);
    popStyle();
    for (Node node : interpolator.keyFrames().values()) {
      pushMatrix();
      scene.applyTransformation(node);
      scene.drawAxes(scene.mouseTracks(node) ? 40 : 20);
      popMatrix();
    }
    if (showEyePath) {
      pushStyle();
      fill(255, 0, 0);
      stroke(0, 255, 0);
      // same as:
      // scene.drawCatmullRom(eyeInterpolator1, 3);
      // scene.drawCatmullRom(eyeInterpolator2, 3);
      scene.drawCatmullRom(eyeInterpolator1);
      scene.drawCatmullRom(eyeInterpolator2);
      popStyle();
    }
    //println(frameRate);
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
      showEyePath = !showEyePath;

    if (key == '1')
      eyeInterpolator1.addKeyFrame();
    if (key == 'a')
      eyeInterpolator1.toggle();
    if (key == 'b')
      eyeInterpolator1.clear();

    if (key == '2')
      eyeInterpolator2.addKeyFrame();
    if (key == 'c')
      eyeInterpolator2.toggle();
    if (key == 'd')
      eyeInterpolator2.clear();

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
