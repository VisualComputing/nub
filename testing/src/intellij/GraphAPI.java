package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/*
World
  ^
  |\
  1 eye
  ^
  |\
  2 3
  |
  4
  |
  5
 */

public class GraphAPI extends PApplet {
  Scene scene;
  PShape p1, p2, p3, p4, p5;
  Node n1, n2, n3, n4, n5;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(900, 900, renderer);
  }

  public void setup() {
    scene = new Scene(this);

    // red
    p1 = createShape(BOX, 30);
    p1.setFill(color(255, 0, 0));
    n1 = new Node(p1);
    //n1.randomize();
    n1.setPickingThreshold(0);

    // green
    p2 = createShape(BOX, 30);
    p2.setFill(color(0, 255, 0));
    n2 = new Node(n1, p2);
    scene.randomize(n2);
    n2.scale(0.5f);
    n2.setPickingThreshold(0);

    //blue
    p3 = createShape(BOX, 30);
    p3.setFill(color(0, 0, 255));
    n3 = new Node(n1, p3);
    scene.randomize(n3);
    n3.setPickingThreshold(0);

    //yellow
    p4 = createShape(BOX, 30);
    p4.setFill(color(255, 255, 0));
    n4 = new Node(n2, p4);
    scene.randomize(n4);
    n4.setPickingThreshold(0);

    // magenta
    p5 = createShape(BOX, 30);
    p5.setFill(color(255, 0, 255));
    n5 = new Node(n4, p5);
    scene.randomize(n5);
    n5.setPickingThreshold(0);
  }

  public void draw() {
    background(125);
    scene.drawAxes();
    scene.render();
  }

  public void keyPressed() {
    if (key == 'p')
      scene.prune(n4);
    if (key == 'a')
      n4.setReference(n2);
    if (key == 'r')
      n4.resetReference();
    if (key == 's')
      n5.setReference(n2);
    if (key == 't')
      n4.setReference(n3);
    if (key == 'u')
      if (scene.isReachable(n4))
        println("yes");
      else
        println("no");
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.alignTag();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.GraphAPI"});
  }
}
