import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

Scene scene1, scene2, scene3, focus;
Node[] shapes;
Node[] toruses;
boolean displayAuxiliarViewers = true;
// whilst scene1 is either on-screen or not; scene2 and scene3 are off-screen
// test both cases here
boolean onScreen = true;

int w = 1920;
int h = 1080;

void settings() {
  size(w, h, P3D);
}

void setup() {
  scene1 = onScreen ? new Scene(g) : new Scene(createGraphics(w, h, P3D));
  scene1.eye().tagging = false;
  shapes = new Node[15];
  for (int i = 0; i < shapes.length; i++) {
    shapes[i] = new Node(boxShape());
    shapes[i].disablePicking(Node.SHAPE);
    scene1.randomize(shapes[i]);
  }
  toruses = new Node[15];
  for (int i = 0; i < toruses.length; i++) {
    toruses[i] = new Node();
    toruses[i].enableHint(Node.TORUS);
    scene1.randomize(toruses[i]);
  }
  // Note that we pass the upper left corner coordinates where the scene1
  // is to be drawn (see drawing code below) to its constructor.
  scene2 = new Scene(createGraphics(w / 2, h / 2, P3D));
  scene2.eye().tagging = false;
  // idem here
  scene3 = new Scene(createGraphics(w / 2, h / 2, P3D));
  scene3.eye().tagging = false;
}

PShape boxShape() {
  PShape box = createShape(BOX, 6);
  box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
  return box;
}

void keyPressed() {
  if (key == ' ')
    displayAuxiliarViewers = !displayAuxiliarViewers;
  if (key == 'f')
    focus.fit(1000);
  if (key == 't') {
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
  focus = scene2.hasFocus() ? scene2 : scene3.hasFocus() ? scene3 : scene1.hasFocus() ? scene1 : null;
  scene1.display(color(75, 25, 15), false, color(0, 225, 15));
  if (displayAuxiliarViewers) {
    scene2.openContext();
    scene2.context().background(75, 25, 175);
    scene2.drawAxes();
    scene2.render();
    scene2.closeContext();
    scene2.image(w / 2, 0);
    scene3.openContext();
    scene3.context().background(175, 200, 20);
    scene3.render();
    scene3.drawAxes();
    scene3.closeContext();
    scene3.image(w / 2, h / 2);
  }
}
