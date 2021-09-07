package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class AuxViewers extends PApplet {
  Scene scene1, scene2, scene3, focus;
  Node[] shapes;
  Node[] toruses;
  boolean displayAuxiliarViewers = true;
  // whilst scene1 is either on-screen or not; scene2 and scene3 are off-screen
  // test both cases here
  boolean onScreen = false;

  int w = 1920;
  int h = 1080;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene1 = onScreen ? new Scene(g) : new Scene(createGraphics(w, h, P3D));
    scene1.eye().tagging = false;
    scene1.setBounds(1000);
    scene1.fit(1);
    shapes = new Node[15];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(boxShape());
      shapes[i].setHUD(this::hud);
      shapes[i].disablePicking(Node.SHAPE);
      //shapes[i].enableHint(Node.BULLSEYE);
      //shapes[i].disablePickingMode(Node.SHAPE);
      scene1.randomize(shapes[i]);
    }
    toruses = new Node[15];
    for (int i = 0; i < toruses.length; i++) {
      toruses[i] = new Node();
      toruses[i].scale(8);
      toruses[i].enableHint(Node.TORUS);
      //toruses[i].enableHint(Node.BULLSEYE);
      //toruses[i].disablePickingMode(Node.SHAPE);
      //toruses[i].setPickingPolicy(Node.SHAPE);
      scene1.randomize(toruses[i]);
    }

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(createGraphics(w / 2, h / 2, P3D));
    scene2.eye().tagging = false;
    scene2.setBounds(1000);
    scene2.fit(1);

    // idem here
    scene3 = new Scene(createGraphics(w / 2, h / 2, P3D));
    scene3.eye().tagging = false;
    scene3.setBounds(1000);
    scene3.fit(1);
  }

  public void hud(PGraphics pg) {
    pg.pushStyle();
    pg.rectMode(CENTER);
    pg.fill(255, 0, 255, 125);
    pg.rect(0,0, 20, 20);
    pg.popStyle();
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  public void keyPressed() {
    if (key == ' ')
      displayAuxiliarViewers = !displayAuxiliarViewers;
    if (key == 'f')
      focus.fit(1);
    if (key == 't') {
      if (focus == null)
        return;
      focus.togglePerspective();
    }
  }

  @Override
  public void mouseMoved() {
    if (focus == null)
      return;
    focus.mouseTag();
  }

  @Override
  public void mouseDragged() {
    if (focus == null)
      return;
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseShift();
    else
      focus.moveForward(focus.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    if (focus == null)
      return;
    focus.zoom(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (focus == null)
      return;
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = scene2.hasMouseFocus() ? scene2 : scene3.hasMouseFocus() ? scene3 : scene1.hasMouseFocus() ? scene1 : null;
    // /*
    scene1.openContext();
    scene1.context().background(75, 25, 15);
    scene1.context().stroke(0, 225, 15);
    scene1.drawGrid();
    scene1.render();
    scene1.closeContext();
    //scene1.render();// if offscreen, render should be forbidden after close context!
    scene1.image();
    // */

    /*
    // works
    scene1.openContext();
    scene1.context().background(75, 25, 15);
    scene1.context().stroke(0, 225, 15);
    scene1.drawGrid();
    scene1.closeContext();
    scene1.openContext();
    scene1.render();
    scene1.closeContext();
    scene1.image();
    // */
    if (displayAuxiliarViewers) {
      scene2.openContext();
      scene2.context().background(75, 25, 175);
      scene2.drawAxes();
      scene2.render();
      //scene2.image(w / 2, 0);// if there's a onscreen scene, image should be forbidden before close context!
      scene2.closeContext();
      //scene2.render();
      scene2.image(w / 2, 0);
      scene3.openContext();
      scene3.context().background(175, 200, 20);
      scene3.drawAxes();
      scene3.render();
      //scene3.image(w / 2, h / 2);// if there's a onscreen scene, image should be forbidden before close context!
      scene3.closeContext();
      //scene3.render();
      scene3.image(w / 2, h / 2);
    }
    //println(frameRate);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.AuxViewers"});
  }
}