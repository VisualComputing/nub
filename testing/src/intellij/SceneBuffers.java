package intellij;

import nub.core.Node;
import nub.processing.Scene;
import nub.timing.TimingHandler;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class SceneBuffers extends PApplet {
  Scene scene;
  Node root, root1, root2;
  Node[] shapes;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(createGraphics(w, h / 2, renderer), max(w, h));
    //scene = new Scene(this);
    root1 = new Node();
    root2 = new Node();
    shapes = new Node[10];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(i%2==0?root1:root2, caja());
      scene.randomize(shapes[i]);
      //shapes[i].enableHint(Node.CAMERA);
      shapes[i].enableHint(Node.AXES);
    }
    scene.fit(1);
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    // /*
    scene.openContext();
    scene.context().background(125);
    scene.drawAxes();
    scene.render(root1);
    scene.render(root2);
    scene.closeContext();
    scene.image();
    // */
    //scene.display(125, root);
    // 2. Display back buffer
    scene.displayBackBuffer(0, h / 2);
  }

  public void mouseMoved() {
    scene.tag();
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
    if (key == '0')
      root = null;
    if (key == '1')
      root = root1;
    if (key == '2')
      root = root2;
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
