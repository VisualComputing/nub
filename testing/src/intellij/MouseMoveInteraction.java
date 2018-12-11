package intellij;

import frames.primitives.Quaternion;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class MouseMoveInteraction extends PApplet {
  Scene scene;
  boolean lookAround = true;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    scene.fit(1);

    Shape shape1 = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pGraphics) {
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawTorusSolenoid(pGraphics, 80);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175, -275);

    Shape shape2 = new Shape(shape1);
    shape2.setGraphics(shape());
    shape2.translate(275, 275, 275);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    // visit scene frames (shapes simply get drawn)
    scene.traverse();
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
    if (key == 'l')
      lookAround = !lookAround;
  }

  public void mouseMoved(MouseEvent event) {
    if (event.isShiftDown())
      scene.translate();
    else if (lookAround && scene.trackedFrame() == null)
      scene.lookAround();
    else
      scene.spin();
  }

  public void mouseWheel(MouseEvent event) {
    //scene.zoom(event.getCount() * 20);
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      //scene.track();
      scene.cast();
    else if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.MouseMoveInteraction"});
  }
}
