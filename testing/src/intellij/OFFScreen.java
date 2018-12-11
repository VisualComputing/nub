package intellij;

import frames.primitives.Quaternion;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class OFFScreen extends PApplet {
  Scene scene;
  Shape shape1, shape2;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(1800, 1200, renderer);
  }

  public void setup() {
    scene = new Scene(this, createGraphics(1300, 900, renderer), 400, 200);
    scene.setFOV(PI / 3);
    scene.setRadius(300);
    scene.fit(1);

    shape1 = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pGraphics) {
        scene.drawAxes(pGraphics, scene.radius() / 3);
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCone(pGraphics, 30, 90);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-55, -55, -55);

    shape2 = new Shape(shape1);
    shape2.setGraphics(shape());
    shape2.translate(-55, -85, 135);
  }

  public void draw() {
    background(255);
    scene.beginDraw();
    scene.frontBuffer().background(0);
    scene.drawAxes();
    scene.traverse();
    scene.endDraw();
    scene.display();
  }

  @Override
  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 35) : createShape(RECT, 0, 0, 40, 40);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.OFFScreen"});
  }
}
