package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class CustomNodeInteraction extends PApplet {
  Scene scene;
  Torus[] shapes;
  PFont font36;
  int totalShapes;

  //Choose P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(1200, 700, renderer);
  }

  public void setup() {
    font36 = loadFont("FreeSans-36.vlw");
    scene = new Scene(this);
    scene.enableHint(Scene.AXES | Scene.BACKGROUND);
    scene.configHint(Scene.BACKGROUND, color(0));
    scene.fit(1);
    shapes = new Torus[10];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Torus();
    }
  }

  public void draw() {
    scene.render();
  }

  public void keyPressed() {
    int value = Character.getNumericValue(key);
    if (value >= 0 && value < 10)
      scene.tag(shapes[value].node);
    if (key == ' ')
      scene.removeTag();
    if (key == CODED)
      if (keyCode == UP)
        scene.translateNode(0, -10, 0);
      else if (keyCode == DOWN)
        scene.translateNode(0, 10, 0);
      else if (keyCode == LEFT)
        scene.interact( "menos");
      else if (keyCode == RIGHT)
        scene.interact("mas");
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpinNode();
    else if (mouseButton == CENTER)
      scene.scaleNode(scene.mouseDX());
    else
      scene.mouseTranslateNode();
  }

  public void mouseWheel(MouseEvent event) {
    scene.interact(event.getCount());
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      scene.interact();
    if (event.getCount() == 2)
      scene.mouseTag();
  }

  public class Torus {
    int id = totalShapes++, faces = randomFaces(), colour = randomColor();
    Node node;

    Torus() {
      node = new Node();
      node.enableHint(Node.TORUS, colour, faces);
      node.setInteraction(this::interact);
      node.setHUD(this::hud);
      scene.randomize(node);
    }

    void hud(PGraphics pg) {
      pg.fill(node.isTagged(scene) ? 0 : 255, node.isTagged(scene) ? 255 : 0, node.isTagged(scene) ? 0 : 255);
      pg.textFont(font36);
      pg.text(id, 0, 0);
    }

    void interact(Object[] gesture) {
      if (gesture.length == 0){
        colour = randomColor();
        node.configHint(Node.TORUS, colour, faces);
      }
      if (gesture.length == 1) {
        if (gesture[0] instanceof String) {
          if (((String) gesture[0]).matches("mas"))
            faces++;
          else if (((String) gesture[0]).matches("menos"))
            if (faces > 2)
              faces--;
        } else if (gesture[0] instanceof Integer) {
          int delta = (Integer) gesture[0];
          if (faces +  delta > 1)
            faces = faces + delta;
        }
        node.configHint(Node.TORUS, colour, faces);
      }
    }

    int randomColor() {
      return color(random(255), random(255), random(255), random(125, 255));
    }

    int randomFaces() {
      return (int) random(3, 15);
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.CustomNodeInteraction"});
  }
}
