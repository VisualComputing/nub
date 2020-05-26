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
boolean onScreen = true;
boolean interactiveEye;

int w = 1200;
int h = 1200;

//Choose P2D or P3D
String renderer = P2D;

void settings() {
  size(w, h, renderer);
}

void setup() {
  Node eye = new Node() {
    @Override
    public void graphics(PGraphics pg) {
      pg.pushStyle();
      pg.fill(isTagged(minimap) ? 255 : 25, isTagged(minimap) ? 0 : 255, 255, 125);
      pg.strokeWeight(2);
      pg.stroke(0, 0, 255);
      Scene.drawFrustum(pg, scene);
      pg.popStyle();
    }
  };
  eye.setPickingThreshold(50);
  eye.setHighlighting(0);
  scene = onScreen ? new Scene(this, eye) : new Scene(this, renderer, eye);
  scene.setRadius(1000);
  rectMode(CENTER);
  scene.fit(1);
  models = new Node[30];
  for (int i = 0; i < models.length; i++) {
    if ((i & 1) == 0) {
      models[i] = new Node(shape());
    } else {
      models[i] = new Node() {
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
    scene.randomize(models[i]);
  }
  // Note that we pass the upper left corner coordinates where the minimap
  // is to be drawn (see drawing code below) to its constructor.
  minimap = new Scene(this, renderer, w / 2, h / 2);
  minimap.setRadius(2000);
  if (renderer == P3D)
    minimap.togglePerspective();
  minimap.fit(1);
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
    if (!scene.isOffscreen())
      scene.beginHUD();
    minimap.beginDraw();
    minimap.context().background(125, 80, 90);
    minimap.drawAxes();
    minimap.render();
    minimap.context().stroke(255);
    minimap.drawBullsEye(scene.eye());
    minimap.endDraw();
    minimap.display(w / 2, h / 2);
    if (!scene.isOffscreen())
      scene.endHUD();
  }
}
