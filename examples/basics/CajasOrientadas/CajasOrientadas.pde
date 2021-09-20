/**
 * Cajas Orientadas.
 * by Jean Pierre Charalambos.
 *
 * This example illustrates some basic node properties, particularly how to
 * orient them.
 *
 * The sphere and the boxes are interactive. Pick and drag them with the
 * right mouse button. Use also the arrow keys to select and move the sphere.
 * See how the boxes will always remain oriented towards the sphere.
 *
 * Press ' ' the change the picking policy.
 * Press 'c' to change the bullseye space.
 * Press 'd' to change the bullseye shape.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene;
Node[] cajas;
Node esfera;
boolean circle;

void setup() {
  size(800, 600, P3D);
  // Set the inertia for all interactivity methods to 0.85. Default is 0.8.
  Scene.inertia = 0.85;
  scene = new Scene(this, 200);
  scene.togglePerspective();
  esfera = new Node(esfera(10));
  esfera.setPosition(new Vector(0, 1.4, 0));
  cajas = new Node[15];
  for (int i = 0; i < cajas.length; i++) {
    cajas[i] = new Node(caja(random(10, 40), random(10, 40), random(10, 40)));
    cajas[i].togglePicking(Node.SHAPE);
    cajas[i].enableHint(Node.AXES | Node.BULLSEYE);
    scene.setVisit(cajas[i], (Node node) -> {
      Vector to = Vector.subtract(esfera.position(), node.position());
      node.setOrientation(Quaternion.from(Vector.plusJ, to));
    });
    scene.randomize(cajas[i]);
  }
  scene.tag("keyboard", esfera);
}

PShape caja(float w, float h, float d) {
  PShape caja = createShape(BOX, w, h, d);
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setStroke(false);
  return caja;
}

PShape esfera(float r) {
  PShape esfera = createShape(SPHERE, r);
  esfera.setStroke(false);
  esfera.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return esfera;
}

void draw() {
  background(0);
  scene.render();
}

void mouseClicked() {
  scene.focus();
}

void mouseMoved() {
  scene.tag();
}

void mouseDragged() {
  if (scene.node("keyboard") != null) {
    scene.shift("keyboard");
  }
  else {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.shift();
  else
    scene.zoom(mouseX - pmouseX);
  }
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ') {
    for (Node caja : cajas)
      caja.togglePicking(Node.BULLSEYE | Node.SHAPE);
  }
  if (key == 'c') {
    for (Node caja : cajas)
      if (caja.bullsEyeSize() < 1)
        caja.setBullsEyeSize(caja.bullsEyeSize() * 200);
      else
        caja.setBullsEyeSize(caja.bullsEyeSize() / 200);
  }
  if (key == 'd') {
    circle = !circle;
    for (Node caja : cajas)
      caja.configHint(Node.BULLSEYE, circle ?
        Node.BullsEyeShape.CIRCLE :
        Node.BullsEyeShape.SQUARE);
  }
  if (key == 'a') {
    for (Node caja : cajas)
      caja.toggleHint(Node.AXES);
  }
  if (key == 'p') {
    for (Node caja : cajas)
      caja.toggleHint(Node.BULLSEYE);
  }
  if (key == 'e') {
    scene.togglePerspective();
  }
  if (key == 's') {
    scene.fit(2000);
  }
  if (key == 'S') {
    scene.fit();
  }
  if (key == 'u') {
    if (scene.isTagValid("keyboard"))
      scene.removeTag("keyboard");
    else
      scene.tag("keyboard", esfera);
  }
  if (key == CODED) {
    if (keyCode == UP)
      scene.shift("keyboard", 0, -10, 0);
    else if (keyCode == DOWN)
      scene.shift("keyboard", 0, 10, 0);
    else if (keyCode == LEFT)
      scene.shift("keyboard", -10, 0, 0);
    else if (keyCode == RIGHT)
      scene.shift("keyboard", 10, 0, 0);
  }
}
