package intellij;

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
    scene = new Scene(this, 150);

    // interpolation 1. Default eye interpolations
    scene.fit(1);

    // interpolation 3. Custom (arbitrary) node interpolations

    shape = new Node((PGraphics pg) -> {
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
    shape.setAnimationRecurrence(true);
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      shape.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1 : 4);
    shape.animate();

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
    scene.updateTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ')
      showEyePath = !showEyePath;

    if (key == '-' || key == '+') {
      if (key == '-')
        speed -= 0.25f;
      else
        speed += 0.25f;
      shape.animate(speed);
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
