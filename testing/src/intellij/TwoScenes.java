package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class TwoScenes extends PApplet {
  Scene scene1, scene2, focus;
  Node node;
  Vector vector;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene1 = new Scene(this, P3D, w, h / 2);
    //scene1.setZClippingCoefficient(1);
    scene1.setRadius(200);
    //scene1.setType(Graph.Type.ORTHOGRAPHIC);
    //scene1.fit(1);
    scene1.fit();

    // enable computation of the frustum planes equations (disabled by default)
    scene1.enableBoundaryEquations();

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, P3D, w, h / 2);
    //scene2.setType(Graph.Type.ORTHOGRAPHIC);
    scene2.setRadius(400);
    //scene2.fit(1);
    scene2.fit();
    node = new Node();
    node.translate(50, 50, 30);
    node.setPickingThreshold(10);
  }

  public void keyPressed() {
    if (key == 'f')
      scene1.fit();
    if (key == 'a') {
      println(scene1.zNear());
      vector = new Vector(0, 0, -scene1.zNear() / scene1.eye().magnitude());
      vector = scene1.eye().worldLocation(vector);
      node.setPosition(vector);
    }
    if (key == 'b') {
      Vector zNear = new Vector(0, 0, scene1.zNear());
      Vector zFar = new Vector(0, 0, scene1.zFar());
      Vector zNear2ZFar = Vector.subtract(zFar, zNear);
      scene1.translateNode(node, 0, 0, zNear2ZFar.magnitude());
    }
    if (key == 'n')
      scene1.eye().setMagnitude(1);
    if (key == 'm')
      scene1.setFOV(PI / 3);
    if (key == 't') {
      if (scene1.type() == Graph.Type.PERSPECTIVE) {
        scene1.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        scene1.setType(Graph.Type.PERSPECTIVE);
      }
    }
    if (key == '+')
      scene1.eye().rotate(0, 1, 0, QUARTER_PI / 2);
    if (key == '-')
      scene1.eye().rotate(0, 1, 0, -QUARTER_PI / 2);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
    //focus.zoom(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.alignTag();
  }

  void draw(Scene scn) {
    PGraphics graphics = scn.context();
    graphics.background(0);
    graphics.noStroke();
    graphics.fill(0, 255, 0);
    graphics.pushMatrix();
    applyTransformation(graphics, node);
    graphics.sphere(50);
    graphics.popMatrix();
  }

  public void applyTransformation(PGraphics pg, Node node) {
    if (pg.is3D()) {
      pg.translate(node.translation()._vector[0], node.translation()._vector[1], node.translation()._vector[2]);
      pg.rotate(node.rotation().angle(), (node.rotation()).axis()._vector[0], (node.rotation()).axis()._vector[1], (node.rotation()).axis()._vector[2]);
      pg.scale(node.scaling(), node.scaling(), node.scaling());
    } else {
      pg.translate(node.translation().x(), node.translation().y());
      pg.rotate(node.rotation().angle2D());
      pg.scale(node.scaling(), node.scaling());
    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public void draw() {
    handleMouse();
    scene1.beginDraw();
    scene1.context().background(0);
    draw(scene1);
    scene1.drawAxes();
    scene1.endDraw();
    scene1.display();

    scene2.beginDraw();
    scene2.context().background(0);
    draw(scene2);
    scene2.drawAxes();

    // draw with axes
    //eye
    scene2.context().pushStyle();
    scene2.context().stroke(255, 255, 0);
    scene2.context().fill(255, 255, 0, 160);
    scene2.drawFrustum(scene1);
    scene2.context().popStyle();
    //axes
    scene2.context().pushMatrix();
    scene2.applyTransformation(scene1.eye());
    scene2.drawAxes(60);
    scene2.context().popMatrix();

    scene2.endDraw();
    scene2.display(0, h / 2);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.TwoScenes"});
  }
}
