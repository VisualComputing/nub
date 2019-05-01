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
Node sceneEye;
boolean displayMinimap = true;
// whilst scene is either on-screen or not, the minimap is always off-screen
// test both cases here:
boolean onScreen = true;
boolean interactiveEye;

int w = 1200;
int h = 1200;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P2D;

void settings() {
  size(w, h, renderer);
}

void setup() {
  scene = onScreen ? new Scene(this) : new Scene(this, renderer);
  // eye only should belong only to the scene
  // so set a detached 'node' instance as the eye
  scene.setEye(new Node());
  scene.setRadius(1000);
  rectMode(CENTER);
  scene.fit(1);
  models = new Node[30];
  for (int i = 0; i < models.length; i++) {
    if ((i & 1) == 0) {
      models[i] = new Node(scene, shape());
    } else {
      models[i] = new Node(scene) {
        int _faces = (int) MiniMap.this.random(3, 15);
        // We need to call the PApplet random function instead of the node random version
        int _color = color(MiniMap.this.random(255), MiniMap.this.random(255), MiniMap.this.random(255));
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          pg.fill(_color);
          Scene.drawTorusSolenoid(pg, _faces, scene.radius() / 30);
          pg.popStyle();
        }
      };
    }
    // set picking precision to the pixels of the node projection
    models[i].setPickingThreshold(0);
    scene.randomize(models[i]);
  }

  // Note that we pass the upper left corner coordinates where the minimap
  // is to be drawn (see drawing code below) to its constructor.
  minimap = new Scene(this, renderer, w / 2, h / 2, w / 2, h / 2);
  // eye only should belong only to the minimap
  // so set a detached 'node' instance as the eye
  minimap.setEye(new Node());
  minimap.setRadius(2000);
  if (renderer == P3D)
    minimap.togglePerspective();
  minimap.fit(1);
  // detached node
  sceneEye = new Node();
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
      minimap.setTrackedNode(sceneEye);
    else
      minimap.resetTrackedNode();
  }
  if (key == 'f')
    focus.fit(1);
  if (key == 't')
    focus.togglePerspective();
}

void mouseMoved() {
  if (!interactiveEye || focus == scene)
    focus.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.spin();
  else if (mouseButton == RIGHT)
    focus.translate();
  else
    focus.scale(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  if (renderer == P3D)
    focus.moveForward(event.getCount() * 10);
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
  focus = displayMinimap ? (mouseX > w / 2 && mouseY > h / 2) ? minimap : scene : scene;
  if (interactiveEye)
    Node.sync(scene.eye(), sceneEye);
  background(75, 25, 15);
  if (scene.isOffscreen()) {
    scene.beginDraw();
    scene.context().background(75, 25, 15);
    scene.drawAxes();
    scene.render();
    scene.endDraw();
    scene.display();
  } else {
    scene.drawAxes();
    scene.render();
  }
  if (displayMinimap) {
    // shift scene attached nodes to minimap
    scene.shift(minimap);
    if (!scene.isOffscreen())
      scene.beginHUD();
    minimap.beginDraw();
    minimap.context().background(125, 80, 90);
    minimap.drawAxes();
    minimap.render();
    // draw scene eye
    minimap.context().fill(sceneEye.isTracked(minimap) ? 255 : 25, sceneEye.isTracked(minimap) ? 0 : 255, 255);
    minimap.context().stroke(0, 0, 255);
    minimap.context().strokeWeight(2);
    minimap.drawFrustum(scene);
    minimap.endDraw();
    minimap.display();
    if (!scene.isOffscreen())
      scene.endHUD();
    // shift back minimap attached nodes to the scene
    minimap.shift(scene);
  }
}
