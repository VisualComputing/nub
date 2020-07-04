package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

import java.util.function.Consumer;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class Lambda extends PApplet {
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
    interpolator = new Interpolator(shape);
    interpolator.enableRecurrence();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      interpolator.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
    interpolator.run();

    // key lines:
    callback = (pg) -> {
      pg.fill(0, 55, 0);
      pg.rect(0, 0, 300, 300);
    };
  }

  Consumer<PGraphics> callback;

  public void hint(PGraphics pGraphics) {
    graphics(callback, pGraphics);
  }

  public void graphics(Consumer<PGraphics> callback, PGraphics pGraphics) {
    //println("Exec graphics...");
    if (callback != null)
      callback.accept(pGraphics);
  }

  public void draw() {
    background(125);
    scene.render();
    //graphics(callback, g);
    hint(g);
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
      eyeInterpolator1.addKeyFrame(scene.eye().get());
    if (key == 'a')
      eyeInterpolator1.toggle();
    if (key == 'b')
      eyeInterpolator1.clear();

    if (key == '2')
      eyeInterpolator2.addKeyFrame(scene.eye().get());
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
      for (Task task : Scene.TimingHandler.tasks())
        task.enableConcurrence();
    if (key == 'y')
      for (Task task : Scene.TimingHandler.tasks())
        task.disableConcurrence();
    if (key == 'p')
      println(Scene.nodes().size());
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Lambda"});
  }
}
