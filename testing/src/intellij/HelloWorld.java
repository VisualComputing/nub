package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class HelloWorld extends PApplet {
  Scene scene, auxScene;
  Vector v;
  Task task;

  @Override
  public void settings() {
    size(1200, 1000, P3D);
  }

  @Override
  public void setup() {
    scene = new Scene(this, 500);
    // pendiente
    scene.eye().enableHint(Node.BOUNDS);
    scene.enableHint(Scene.BACKGROUND | Scene.AXES | Scene.GRID);
    scene.configHint(Scene.BACKGROUND, color(125));
    scene.enableHint(Scene.HUD);
    scene.setHUD(this::rect);
    scene.setShape(createShape(SPHERE, 50));
    task = new Task(this::functor);
    task.run();
    task.setFrequency(1);
    auxScene = new Scene(createGraphics(500, 500, P3D), scene.radius() * 2);
    auxScene.enableHint(Scene.BACKGROUND | Scene.AXES | Scene.GRID);
    auxScene.enableHint(Scene.SHAPE);
    PShape sphere = createShape(SPHERE, scene.radius());
    sphere.fill(255, 255, 0, 125);
    auxScene.setShape(sphere);
    auxScene.togglePerspective();
  }

  @Override
  public void keyPressed() {
    if (key == 'f')
      scene.fit(1);
    if (key == 't') {
      scene.toggleHint(Scene.HUD);
    }
  }

  /**
   * mode inmediato
   */
  void rect(PGraphics pg) {
    pg.pushStyle();
    pg.fill(255, 255, 0, 125);
    rect(300, 200, 200, 300);
    pg.pushStyle();
  }

  void functor() {
    println(frameCount);
  }

  @Override
  public void draw() {
    scene.display();
    if (v != null) {
      pushStyle();
      stroke(255, 0, 0);
      strokeWeight(10);
      point(v.x(), v.y(), v.z());
      popStyle();
    }
    //auxScene.render();
    //auxScene.image(width/2, height/2);
    auxScene.display(width - auxScene.width(), height - auxScene.height());
  }

  @Override
  public void mouseMoved(MouseEvent event) {
    if (event.isControlDown()) {
      v = scene.mouseLocation();
    }
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.HelloWorld"});
  }
}
