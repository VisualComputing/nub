import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene, visualHint, focus;
int w = 500, h = 500, atX, atY;
Node[] models;
Vector pup;

void setup() {
  size(1800, 1400, P3D);
  scene = new Scene(this, 1000);
  scene.fit(1000);
  models = new Node[100];
  for (int i = 0; i < models.length; i++) {
    models[i] = new Node(boxShape());
    scene.randomize(models[i]);
  }
  visualHint = new Scene(createGraphics(w, h, P3D), 300);
}

void draw() {
  focus = pup != null ? visualHint.hasFocus() ? visualHint : scene : scene;
  background(0);
  scene.drawAxes();
  scene.render();
  if (pup != null) {
    visualHint.openContext();
    visualHint.context().background(125);
    visualHint.render();
    visualHint.closeContext();
    visualHint.image(atX, atY);
  }
}

void mouseClicked(MouseEvent event) {
  if (event.getCount() == 1)
    scene.focus();
  else
    scene.align();
}

PShape boxShape() {
  PShape box = createShape(BOX, 60);
  box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return box;
}

void mouseMoved(MouseEvent event) {
  if (event.isControlDown()) {
    pup = scene.location();
    if (pup != null) {
      visualHint.setCenter(pup);
      visualHint.setRadius(300);
      visualHint.eye().setWorldPosition(pup);
      visualHint.setViewDirection(scene.displacement(new Vector(0, 1, 0)));
      visualHint.setUpVector(scene.displacement(new Vector(0, 0, -1)));
      visualHint.fit();
      atX = mouseX - w / 2;
      atY = mouseY - h;
    }
  } else {
    pup = null;
    focus.tag();
  }
}

void mouseDragged() {
  if (mouseButton == LEFT)
    focus.spin();
  else if (mouseButton == RIGHT)
    focus.shift();
  else
    focus.moveForward(focus.mouseDX());
}

void mouseWheel(MouseEvent event) {
  focus.zoom(event.getCount() * 20);
}

void keyPressed() {
  scene.togglePerspective();
}
