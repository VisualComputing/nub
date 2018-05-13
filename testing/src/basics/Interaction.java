package basics;

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class Interaction extends PApplet {
  Scene scene;
  Shape shape1, shape2;
  Vector upVector;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    shape1 = new Shape(scene) {
      @Override
      public void setShape(PGraphics pGraphics) {
        scene.drawAxes(pGraphics, scene.radius() / 3);
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

    shape2 = new Shape(shape1);
    shape2.setShape(shape());
    shape2.translate(275, 275, 275);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    if (mousePressed)
      scene.traverse();
    else
      // same as traverse, but updates the scene tracked frame
      // according to the current mouse position
      scene.cast();
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
  }

  public void mousePressed() {
    upVector = scene.eye().yAxis();
  }

  public void mouseDragged() {
    // Note 1.
    // When mouse methods are invoked without the frame parameter
    // the scene.defaultFrame is used.
    // Note 2.
    // Mouse methods that don't take a frame parameter (such as mouseCAD)
    // are only available to the scene.eye().
    if (mouseButton == LEFT)
      scene.mouseSpin();
      //scene.mouseLookAround(upVector);
      //scene.mouseCAD();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
      //scene.mousePan();
    else
      //scene.zoom(mouseX - pmouseX);
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    //scene.zoom(event.getCount() * 20);
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
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
    PApplet.main(new String[]{"basics.Interaction"});
  }
}
