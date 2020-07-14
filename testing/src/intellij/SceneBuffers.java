package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class SceneBuffers extends PApplet {
  Scene scene;
  Node[] shapes;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 700;
  int h = 700;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(createGraphics(w, h / 2, renderer));
    //scene = new Scene(this);
    scene.enableHint(Scene.BACKGROUND, color(125));
    scene.setRadius(max(w, h));

    shapes = new Node[100];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(caja());
      scene.randomize(shapes[i]);
      //shapes[i].enableHint(Node.CAMERA);
      shapes[i].enableHint(Node.AXES);
    }
    scene.fit(1);
    scene.enableHint(Scene.AXES);
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    scene.display();
    // 2. Display back buffer
    scene.displayBackBuffer(0, h / 2);
  }

  public void mouseMoved() {
    scene.mouseTag();
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

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.SceneBuffers"});
  }
}
