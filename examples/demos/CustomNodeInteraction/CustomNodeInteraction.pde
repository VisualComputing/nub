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
Torus[] shapes;
PFont font36;
int totalShapes;

//Choose P2D or P3D
String renderer = P3D;

void settings() {
  size(1200, 700, renderer);
}

void setup() {
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

void draw() {
  scene.render();
}

void keyPressed() {
  int value = Character.getNumericValue(key);
  if (value >= 0 && value < 10)
    scene.tag("key", shapes[value].node);
  if (key == ' ')
    scene.removeTag("key");
  if (key == CODED)
    if (keyCode == UP)
      scene.shift("key", 0, -10, 0);
    else if (keyCode == DOWN)
      scene.shift("key", 0, 10, 0);
    else if (keyCode == LEFT)
      scene.interact("key", "menos");
    else if (keyCode == RIGHT)
      scene.interact("key", "mas");
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin("key");
  else if (mouseButton == CENTER)
    scene.zoom("key", scene.mouseDX());
  else
    scene.mouseShift("key");
}

void mouseWheel(MouseEvent event) {
  scene.interact("key", event.getCount());
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.interact("key");
  if (event.getCount() == 2)
    scene.mouseTag("key");
}
