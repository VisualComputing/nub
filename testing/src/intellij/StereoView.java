package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class StereoView extends PApplet {
  Scene leftEye, rightEye, focus;
  Node head;
  Node[] models;

  int w = 1600;
  int h = 800;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    head = new Node();
    leftEye = new Scene(this, P3D, w / 2, h);
    // eye only should belong only to the scene
    // so set a detached 'node' instance as the eye
    leftEye.setEye(new Node(head));
    leftEye.setRadius(1000);
    rectMode(CENTER);
    leftEye.fit();
    models = new Node[30];
    for (int i = 0; i < models.length; i++) {
      if ((i & 1) == 0) {
        models[i] = new Node(leftEye, shape());
      } else {
        models[i] = new Node(leftEye) {
          int _faces = (int) StereoView.this.random(3, 15);
          // We need to call the PApplet random function instead of the node random version
          int _color = color(StereoView.this.random(255), StereoView.this.random(255), StereoView.this.random(255));
          @Override
          public void graphics(PGraphics pg) {
            pg.pushStyle();
            pg.fill(_color);
            Scene.drawTorusSolenoid(pg, _faces, leftEye.radius() / 30);
            pg.popStyle();
          }
        };
      }
      // set picking precision to the pixels of the node projection
      models[i].setPickingThreshold(0);
      leftEye.randomize(models[i]);
    }

    rightEye = new Scene(this, P3D, w / 2, h, w / 2, 0);
    // eye only should belong only to the minimap
    // so set a detached 'node' instance as the eye
    rightEye.setEye(new Node(head));
    rightEye.setRadius(1000);
    rightEye.fit();
  }

  PShape shape() {
    PShape shape = P3D == P3D ? createShape(BOX, 60) : createShape(RECT, 0, 0, 80, 100);
    shape.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return shape;
  }

  public void mouseMoved() {
    leftEye.mouseLookAroundNode(head);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.scale(focus.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    if (P3D == P3D)
      focus.moveForward(event.getCount() * 10);
    else
      focus.scale(event.getCount() * 40);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = mouseX > w / 2 && mouseY > h / 2 ? rightEye : leftEye;
    background(0, 0, 0);
    leftEye.beginDraw();
    leftEye.context().background(75, 25, 15);
    leftEye.drawAxes();
    leftEye.render();
    leftEye.endDraw();
    leftEye.display();
    // shift scene attached nodes to minimap
    leftEye.shift(rightEye);
    rightEye.beginDraw();
    rightEye.context().background(125, 80, 90);
    rightEye.drawAxes();
    rightEye.render();
    rightEye.endDraw();
    rightEye.display();
    // shift back minimap attached nodes to the scene
    rightEye.shift(leftEye);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.StereoView"});
  }
}
