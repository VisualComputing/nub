package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
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
  boolean recurrent;

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
    shape.setMinMaxScalingFilter(0.8f, 1.2f);
    shape.setHUD(this::hud);
    shape.enableHint(Node.AXES);
    shape.enableHint(Node.BULLSEYE);
    shape.enableHint(Node.KEYFRAMES, Node.AXES | Node.BULLSEYE, 2, color(0, 255, 0), 10);
    shape.configHint(Node.BULLSEYE, color(255, 0, 0));
    shape.setBullsEyeSize(50);
    int count = (int) random(4, 10);
    count = 5;
    for (int i = 0; i < count; i++) {
      //shape.addKeyFrame(scene.randomNode(), i % 2 == 1 ? 1000 : 4000);
      /*
      Node node = scene.randomNode();
      node.enableHint(Node.BULLSEYE);
      shape.addKeyFrame(node, i % 2 == 1 ? 1000 : 4000);
      // */
      // /*
      scene.randomize(shape);
      shape.addKeyFrame(Node.AXES | Node.SHAPE | Node.HUD, i % 2 == 1 ? 1000 : 4000);
      // */
    }
    shape.resetScalingFilter();
    //shape.animate();
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
    scene.display(color(125), false, color(0, 255, 0)/*, this::sceneHUD*/);
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
    scene.updateTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.moveForward((mouseX - pmouseX)/5);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  float speed = 1;

  public void keyPressed() {
    if (key == 'm') {
      shape.animate(-1);
    }
    if (key == 'n') {
      recurrent = !recurrent;
      shape.setAnimationRecurrence(recurrent);
    }
    if (key == 'q') {
      shape.resetAnimation();
    }
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
      shape.animate(speed += key == '+' ? 0.25f : -0.25f);
    }

    if (key == '1') {
      scene.eye().addKeyFrame(Node.CAMERA | Node.BULLSEYE, 1000);
    }
    if (key == 'a')
      scene.eye().toggleAnimation();
    if (key == 'b')
      scene.eye().removeKeyFrames();

    if (key == 's')
      scene.fit(1000);
    if (key == 'f')
      scene.fit();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Interpolators"});
  }
}
