/**
 * Auxiliary Views.
 * by Jean Pierre Charalambos.
 *
 * This example shows how to setup different views of the same scene.
 * It comprises three scenes: the main scene and two auxiliary ones.
 *
 * Both, the scene objects and their eyes are interactive and controlled
 * with the mouse in either scene. Hover over the scene objects to pick
 * them. Mouse dragging and clicking defines the interaction of a picked
 * object or the eye when no object is currently picked.
 *
 * Press ' ' to toggle the auxiliary viewer display.
 * Press 'f' to fit scene.
 * Press 'p' to toggle the focused scene perspective projection.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

// main and auxiliary scenes
Scene mainScene, sideScene, frontScene;
// focus is either scene and is defined according to the current mouse
// position
Scene focus;
Node[] shapes;
Node[] toruses;
boolean displayAuxiliaryViews = true;
// whilst the main scene is either on-screen or not; the side and the
// front scenes are off-screen. Test both cases here
boolean onScreen = true;

int w = 1920;
int h = 1080;

void settings() {
  size(w, h, P3D);
}

void setup() {
  mainScene = onScreen ? new Scene(g) : new Scene(createGraphics(w, h, P3D));
  mainScene.setUpVector(Vector.minusK);
  mainScene.eye().setPosition(new Vector(150, 150, 50));
  mainScene.lookAt(Vector.zero);
  mainScene.eye().tagging = false;
  shapes = new Node[15];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node(boxShape());
    mainScene.randomize(shapes[i]);
  }
  toruses = new Node[15];
  for (int i = 0; i < toruses.length; i++) {
    toruses[i] = new Node();
    toruses[i].enableHint(Node.TORUS);
    mainScene.randomize(toruses[i]);
  }
  // Note that we pass the upper left corner coordinates where the
  // scene is to be drawn (see drawing code below) to its constructor.
  sideScene = new Scene(createGraphics(w / 2, h / 2, P3D));
  sideScene.setUpVector(Vector.minusJ);
  sideScene.eye().setPosition(new Vector(200, 0, 0));
  sideScene.lookAt(Vector.zero);
  sideScene.eye().tagging = false;
  // idem here
  frontScene = new Scene(createGraphics(w / 2, h / 2, P3D));
  frontScene.eye().setPosition(new Vector(0, 200, 0));
  frontScene.lookAt(Vector.zero);
  frontScene.eye().tagging = false;
}

PShape boxShape() {
  PShape box = createShape(BOX, 6);
  box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return box;
}

void keyPressed() {
  if (key == ' ')
    displayAuxiliaryViews = !displayAuxiliaryViews;
  if (key == 'f')
    focus.fit(1000);
  if (key == 'p') {
    if (focus == null) return;
    focus.togglePerspective();
  }
}

void mouseMoved() {
  if (focus == null) return;
  focus.tag();
}

void mouseDragged() {
  if (focus == null) return;
  if (mouseButton == LEFT)
    focus.spin();
  else if (mouseButton == RIGHT)
    focus.shift();
  else
    focus.moveForward(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  if (focus == null) return;
  focus.zoom(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
  if (focus == null) return;
  if (event.getCount() == 2)
    if (event.getButton() == LEFT)
      focus.focus();
    else
      focus.align();
}

void draw() {
  focus = sideScene.hasFocus() ? sideScene : frontScene.hasFocus() ? frontScene : mainScene.hasFocus() ? mainScene : null;
  mainScene.display(color(75, 25, 15), true, color(0, 225, 15));
  if (displayAuxiliaryViews) {
    sideScene.openContext();
    sideScene.context().background(75, 25, 175, 175);
    sideScene.drawAxes();
    sideScene.render();
    sideScene.closeContext();
    sideScene.image(w / 2, 0);
    frontScene.openContext();
    frontScene.context().background(175, 200, 20, 175);
    frontScene.render();
    frontScene.drawAxes();
    frontScene.closeContext();
    frontScene.image(w / 2, h / 2);
  }
}