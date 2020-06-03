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
  Node n1, n2, n3, n4, n5, detached;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(900, 900, renderer);
  }

  public void setup() {
    scene = new Scene(this);

    // red
    n1 = new Node(shape(color(255, 0, 0)));

    // green
    n2 = new Node(n1, shape(color(0, 255, 0)));
    scene.randomize(n2);
    n2.scale(0.5f);

    // blue
    n3 = new Node(n1, shape(color(0, 0, 255)));
    scene.randomize(n3);

    // yellow
    n4 = new Node(n2, shape(color(255, 255, 0)));
    scene.randomize(n4);

    // magenta
    n5 = new Node(n4, shape(color(255, 0, 255)));
    scene.randomize(n5);

    // cyan
    detached = Node.detach(n1);
    detached.setShape(shape(color(0, 255, 255)));
    scene.randomize(detached);
  }

  PShape shape(int c) {
    PShape pShape = createShape(BOX, 30);
    pShape.setFill(c);
    return pShape;
  }

  public void draw() {
    background(125);
    scene.drawAxes();
    scene.render();
  }

  public void keyPressed() {
    if (key == 'x')
      detached.resetReference();
    if (key == 'y')
      n4.setReference(detached);
    if (key == 'z')
      detached.setReference(n1);
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
