package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
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
    scene = new Scene(this);// red
    n1 = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(255, 0, 0);
        pg.box(30);
        pg.popStyle();
      }
    };
    // green
    n2 = new Node(n1) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 255, 0);
        pg.box(30);
        pg.popStyle();
      }
    };
    n2.scale(0.5f);
    scene.randomize(n2);
    //blue
    n3 = new Node(n1) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 0, 255);
        pg.box(30);
        pg.popStyle();
      }
    };
    scene.randomize(n3);
    //yellow
    n4 = new Node(n2) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(255, 255, 0);
        pg.box(30);
        pg.popStyle();
      }
    };
    scene.randomize(n4);
    // magenta
    n5 = new Node(n4) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(255, 0, 255);
        pg.box(30);
        pg.popStyle();
      }
    };
    scene.randomize(n5);

    // cyan
    detached = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 255, 255);
        pg.box(30);
        pg.popStyle();
      }
    };
    scene.randomize(detached);
    Graph.prune(detached);
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
