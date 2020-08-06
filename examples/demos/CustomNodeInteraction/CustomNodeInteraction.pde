/**
 * Custom Node Interaction.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to customize shape behaviors by
 * overriding the node interact(Object... gesture) method and
 * sending gesture data to the node with the scene
 * interactTag(Object... gesture) method.
 *
 * The toruses color and number of faces are controled with the
 * keys and the mouse. To pick a torus press the [0..9] keys
 * or double-click on it; press other key or double-click on the
 * the background to reset your picking selection. Once a torus
 * is picked try the key arrows, the mouse wheel or a single
 * mouse click, to control its color and number of faces.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node[] shapes;
PFont font36;
int totalShapes;

//Choose P2D or P3D
String renderer = P3D;

void settings() {
  size(1200, 700, renderer);
}

void setup() {
  scene = new Scene(this);
  scene.enableHint(Scene.AXES | Scene.BACKGROUND);
  scene.configHint(Scene.BACKGROUND, color(0));
  scene.fit(1);
  shapes = new Node[10];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node() {
      int _id = totalShapes++, _faces = randomFaces(), _color = randomColor();

      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(_color);
        Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 20);
        scene.beginHUD();
        Vector position = scene.screenLocation(position());
        pg.fill(isTagged(scene) ? 0 : 255, isTagged(scene) ? 255 : 0, isTagged(scene) ? 0 : 255);
        pg.textFont(font36);
        pg.text(_id, position.x(), position.y());
        scene.endHUD();
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
            if (_faces +  delta > 1)
              _faces = _faces + delta;
          }
      }
    };
    scene.randomize(shapes[i]);
  }
  font36 = loadFont("FreeSans-36.vlw");
}

int randomColor() {
  return color(random(255), random(255), random(255), random(125, 255));
}

int randomFaces() {
  return (int) random(3, 15);
}

void draw() {
  scene.render();
}

void keyPressed() {
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

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin("key");
  else if (mouseButton == CENTER)
    scene.scale("key", scene.mouseDX());
  else
    scene.mouseTranslate("key");
}

void mouseWheel(MouseEvent event) {
  scene.interactTag("key", event.getCount());
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.interactTag("key");
  if (event.getCount() == 2)
    scene.mouseTag("key");
}
