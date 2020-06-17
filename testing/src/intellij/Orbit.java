package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class Orbit extends PApplet {
  Scene scene;
  Node shape1, shape2;
  Vector axis;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fit(1);

    shape1 = new Node() {
      @Override
      public void graphics(PGraphics pGraphics) {
        Scene.drawAxes(pGraphics, scene.radius() / 3);
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCylinder(pGraphics, 30, scene.radius() / 4, 200);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175, -275);

    //shape2 = new Node(shape1);
    shape2 = new Node();
    shape2.setShape(shape());
    shape2.translate(275, 275, 275);

    scene.tag(shape2);
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void draw() {
    background(0);
    stroke(0, 255, 0, 125);
    scene.drawArrow(axis);
    scene.drawAxes();
    scene.render();
  }

  public void keyPressed() {
    if (key == 'i')
      scene.tag(scene.hasTag(shape1) ? shape2 : shape1);
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      if (shape2.isTagged(scene))
        //shape2.rotate((mouseX-pmouseX)* PI / width, 0, 0, shape1);
        //shape2.rotateAround(new Quaternion(new Vector(0, 1, 0), (mouseX - pmouseX) * PI / width), shape1);
        //shape2.rotateAround(new Quaternion(new Vector(0, 1, 0), (mouseX - pmouseX) * PI / width), shape1);
        //shape2.orbit(new Quaternion(new Vector(0, 1, 0), (mouseX - pmouseX) * PI / width), shape1);
        shape2.orbit(axis, (mouseX - pmouseX) * PI / width);
      else
        scene.mouseSpin();
    } else if (mouseButton == RIGHT)
      scene.mouseSpinEye();
    //scene.translate();
    //scene.mousePan();
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      scene.focus();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Orbit"});
  }
}
