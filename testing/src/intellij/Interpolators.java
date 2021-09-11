package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
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
    //scene = new Scene(this, 150);
    scene = new Scene(createGraphics(1920, 1080, P3D), 150);
    scene.eye().enableHint(Node.KEYFRAMES);
    PShape pshape;
    if (scene.is2D()) {
      rectMode(CENTER);
      pshape = createShape(RECT, 0, 0, 100, 100);
    }
    else {
      pshape = createShape(BOX, 30);
    }
    pshape.setFill(color(0, 255, 255/*, 125*/));
    shape = new Node(pshape);
    shape.setHUD(this::hud);
    shape.setAnimationRecurrence(true);
    shape.enableHint(Node.AXES);
    shape.enableHint(Node.BULLSEYE);
    shape.enableHint(Node.KEYFRAMES, Node.AXES | Node.BULLSEYE, 2, color(0, 255, 0), 10);
    shape.configHint(Node.BULLSEYE, color(255, 0, 0));
    shape.setBullsEyeSize(50);
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
      shape.addKeyFrame(Node.AXES | Node.SHAPE | Node.HUD, i % 2 == 1 ? 1 : 4);
      // */
    }
    shape.animate();
    frameRate(1000);
  }

  public void draw() {
    /*
    // WARNING: works only for onscreen scenes
    background(125);
    scene.render();
    scene.drawAxes();
    stroke(0,255,0);
    scene.drawGrid();
    scene.beginHUD();
    hud(scene.context());
    scene.endHUD();
    // */
    // /*
    // Works for both onscreen and offscreen scenes!!!
    scene.display(color(125), false, color(0, 255, 0), this::sceneHUD);
    //scene.display(color(125), true, true, this::customBox);
    // */
  }

  public void customBox() {
    PGraphics pg = scene.context();
    pg.pushStyle();
    // /*
    pg.fill(255, 0, 0, 125);
    pg.box(30);
    // */
    // pg.shape(box);
    pg.popStyle();
  }

  public void sceneHUD() {
    scene.beginHUD();
    hud(scene.context());
    scene.endHUD();
  }

  public void hud(PGraphics pg) {
    pg.pushStyle();
    pg.rectMode(CENTER);
    pg.fill(255, 0, 255, 125);
    pg.stroke(0,0,255);
    pg.strokeWeight(3);
    pg.rect(0, 0, 80, 50);
    pg.popStyle();
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
    if (key == 'y') {
      if (shape.isAttached())
        shape.detach();
      else
        shape.attach();
    }
    if (key == 'x')
      shape.removeKeyFrames();
    if (key == 'c') {
      println(Graph.nodes().size());
      shape.removeKeyFrame(1);
      println(Graph.nodes().size());
    }
    if (key == 'z') {
      println(scene.fov());
      println(scene.eye().worldMagnitude());
    }
    if (key == ' ') {
      shape.toggleHint(Node.KEYFRAMES);
      scene.eye().toggleHint(Node.KEYFRAMES);
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

    if (key == '1') {
      scene.eye().addKeyFrame(Node.CAMERA | Node.BULLSEYE, 1);
    }
    if (key == 'a')
      scene.eye().toggleAnimation();
    if (key == 'b')
      scene.eye().removeKeyFrames();

    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
    if (key == 'e')
      for (Task task : Scene.TimingHandler.tasks())
        task.enableConcurrence();
    if (key == 'f')
      for (Task task : Scene.TimingHandler.tasks())
        task.disableConcurrence();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Interpolators"});
  }
}
