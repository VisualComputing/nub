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
  size(800, 600, renderer);
  scene = onScreen ? new Scene(this) : new Scene(this, renderer);
  scene.setRadius(1000);
  rectMode(CENTER);
  scene.fit(1);
  scene.eye().setBullsEyeSize(50);
  scene.eye().disableHint(Node.HIGHLIGHT);
  scene.eye().enableHint(Node.BULLSEYE);
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
    // set picking precision to the pixels of the node projection
    models[i].setPickingPolicy(Node.SHAPE);
    scene.randomize(models[i]);
  }
  // Note that we pass the upper left corner coordinates where the minimap
  // is to be drawn (see drawing code below) to its constructor.
  minimap = new Scene(this, renderer, width / 2, height / 2);
  minimap.setRadius(2000);
  if (renderer == P3D)
    minimap.togglePerspective();
  minimap.fit(1);
  minimap.enableHint(Scene.BACKGROUND, color(125, 80, 90));
  minimap.enableHint(Scene.FRUSTUM, scene, color(255, 0, 0, 125));
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
  if (scene.isOffscreen()) {
    scene.display();
  } else {
    scene.render();
  }
  if (displayMinimap) {
    minimap.display(width / 2, height / 2);
  }
}
