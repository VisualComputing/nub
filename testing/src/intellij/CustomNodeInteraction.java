package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class CustomNodeInteraction extends PApplet {
  Scene scene;
  Node[] shapes;
  PFont font36;
  int totalShapes;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(1240, 840, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    scene.fit(1);
    shapes = new Node[10];
    for (int i = 0; i < shapes.length; i++) {
      // shapes[i] = new CustomNode();
      // /*
      shapes[i] = new Node() {
        int _id = totalShapes++, _faces = randomFaces(), _color = randomColor();

        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          pg.fill(_color);
          Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 20);
          pg.popStyle();
        }

        @Override
        public void interact(Object... gesture) {
          if (gesture.length == 0)
            _color = randomColor();
          if (gesture.length == 1)
            if (gesture[0] instanceof String) {
              if (((String) gesture[0]).matches("mas"))
                _faces++;
              else if (((String) gesture[0]).matches("menos"))
                if (_faces > 2)
                  _faces--;
            } else if (gesture[0] instanceof Integer) {
              int delta = (Integer) gesture[0];
              if (_faces + delta > 1)
                _faces = _faces + delta;
            }
        }
      };
      // */
      //shapes[i].setInteraction(this::customInteraction2);
      scene.randomize(shapes[i]);
    }
    font36 = loadFont("FreeSans-36.vlw");
  }

  public void hud(PGraphics pg) {
    pg.fill(2555, 0, 255);
    pg.textFont(font36);
    // TODO pending!
    //pg.text(_id);
  }

  int randomColor() {
    return color(random(255), random(255), random(255), random(125, 255));
  }

  int randomFaces() {
    return (int) random(3, 15);
  }

  public void customInteraction2(Node node, Object[] gesture) {
    CustomNode customNode = (CustomNode) node;
    if (gesture.length == 0)
      customNode._color = randomColor();
    if (gesture.length == 1)
      if (gesture[0] instanceof String) {
        if (((String) gesture[0]).matches("mas"))
          customNode._faces++;
        else if (((String) gesture[0]).matches("menos"))
          if (customNode._faces > 2)
            customNode._faces--;
      } else if (gesture[0] instanceof Integer) {
        int delta = (Integer) gesture[0];
        if (customNode._faces + delta > 1)
          customNode._faces = customNode._faces + delta;
      }
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.render();
  }

  public void keyPressed() {
    int value = Character.getNumericValue(key);
    if (value >= 0 && value < 10)
      scene.tag("key", shapes[value]);
    if (key == ' ')
      scene.removeTag("key");
    if (key == CODED)
      if (keyCode == UP)
        scene.translate("key", 0, -10, 0);
      else if (keyCode == DOWN)
        scene.translate("key", 0, 10, 0);
      else if (keyCode == LEFT)
        scene.interactTag("key", "menos");
      else if (keyCode == RIGHT)
        scene.interactTag("key", "mas");
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin("key");
    else if (mouseButton == CENTER)
      scene.scale("key", scene.mouseDX());
    else
      scene.mouseTranslate("key");
  }

  public void mouseWheel(MouseEvent event) {
    scene.interactTag("key", event.getCount());
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      scene.interactTag("key");
    if (event.getCount() == 2)
      scene.mouseTag("key");
  }

  public class CustomNode extends Node {
    int _id = totalShapes++, _faces = randomFaces(), _color = randomColor();

    public CustomNode() {
      //setInteraction(this::customInteraction);
    }

    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.fill(_color);
      Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 20);
      pg.popStyle();
    }

    public void customInteraction(Object[] gesture) {
      if (gesture.length == 0)
        _color = randomColor();
      if (gesture.length == 1)
        if (gesture[0] instanceof String) {
          if (((String) gesture[0]).matches("mas"))
            _faces++;
          else if (((String) gesture[0]).matches("menos"))
            if (_faces > 2)
              _faces--;
        } else if (gesture[0] instanceof Integer) {
          int delta = (Integer) gesture[0];
          if (_faces + delta > 1)
            _faces = _faces + delta;
        }
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.CustomNodeInteraction"});
  }
}
