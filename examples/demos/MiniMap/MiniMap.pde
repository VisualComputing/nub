/**
 * Mini Map
 * by Jean Pierre Charalambos.
 *
 * This example illustrates how to use off-screen rendering to build
 * a mini-map of the main Scene where all objects are interactive.
 * Note that the minimap displays the projection of the scene onto
 * the near plane in 3D.
 *
 * Press ' ' to toggle the minimap display.
 * Press 'i' to toggle the interactivity of the minimap scene eye.
 * Press 'f' to show the entire scene or minimap.
 * Press 't' to toggle the scene camera type (only in 3D).
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene, minimap, focus;
Node[] models;
boolean displayMinimap = true;
// whilst scene is either on-screen or not, the minimap is always off-screen
// test both cases here:
boolean onScreen = false;
boolean interactiveEye;

//Choose P2D or P3D
String renderer = P2D;

void setup() {
  rectMode(CENTER);
  size(1920, 1080, renderer);
  scene = onScreen ? new Scene(this, 1000) : new Scene(createGraphics(width, height, renderer), 1000);
  scene.eye().enableHint(Node.BOUNDS | Node.BULLSEYE);
  scene.eye().disablePicking(Node.BULLSEYE);
  scene.eye().setBullsEyeSize(50);
  scene.eye().setHighlight(0);
  scene.enableHint(Scene.BACKGROUND, color(75, 25, 15));
  models = new Node[30];
  for (int i = 0; i < models.length; i++) {
    if ((i & 1) == 0) {
      models[i] = new Node(shape());
    } else {
      models[i] = new Node();
      models[i].enableHint(Node.TORUS);
      models[i].scale(3);
    }
    scene.randomize(models[i]);
  }
  // Note that we pass the upper left corner coordinates where the minimap
  // is to be drawn (see drawing code below) to its constructor.
  minimap = new Scene(createGraphics(width / 2, height / 2, renderer), 2000);
  if (renderer == P3D)
    minimap.togglePerspective();
  minimap.enableHint(Scene.BACKGROUND, color(125, 80, 90));
}

PShape shape() {
  PShape shape = renderer == P3D ? createShape(BOX, 60) : createShape(RECT, 0, 0, 80, 100);
  shape.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return shape;
}

void keyPressed() {
  if (key == ' ')
    displayMinimap = !displayMinimap;
  if (key == 'i') {
    interactiveEye = !interactiveEye;
    if (interactiveEye)
      minimap.tag(scene.eye());
    else
      minimap.untag(scene.eye());
  }
  if (key == 'f')
    focus.fit(1);
  if (key == 't')
    focus.togglePerspective();
  if (key == 'p')
    scene.eye().togglePicking(Node.BULLSEYE | Node.BOUNDS);
}

void mouseMoved() {
  if (!interactiveEye || focus == scene)
    focus.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.mouseSpin();
  else if (mouseButton == RIGHT)
    focus.mouseTranslate();
  else
    focus.scale(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  if (renderer == P3D)
    focus.moveForward(event.getCount() * 40);
  else
    focus.scale(event.getCount() * 40);
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focus();
    else
      focus.align();
}

void draw() {
  focus = minimap.hasMouseFocus() ? minimap : scene;
  scene.display();
  if (displayMinimap) {
    minimap.display(width / 2, height / 2);
  }
}
