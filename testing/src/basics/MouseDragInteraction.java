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
public class MouseDragInteraction extends PApplet {
  Scene scene;
  Shape shape1, shape2;
  Vector randomVector;
  boolean cad, lookAround;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);
    //scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    shape1 = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pGraphics) {
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
    shape1.translate(-375, 175);

    shape2 = new Shape(shape1);
    shape2.setGraphics(shape());
    shape2.translate(275, 275);

    randomVector = Vector.random();
    randomVector.setMagnitude(scene.radius() * 0.5f);
  }

  public void draw() {
    background(0);
    fill(0, 255, 255);
    scene.drawArrow(randomVector);
    scene.drawAxes();
    // draw + mouse move picking
    scene.castOnMouseMove();
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'f')
      scene.fitBall();
    if (key == 'c') {
      cad = !cad;
      if (cad) {
        scene.eye().setYAxis(randomVector);
        //scene.focus(scene.eye());
        scene.fitBall();
      }
    }
    if (key == 'l')
      lookAround = !lookAround;
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      if (cad)
        scene.mouseCAD(randomVector);
      else if (lookAround)
        scene.mouseLookAround();
      else
        scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.zoom(mouseX - pmouseX);
      //scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
    //scene.zoom(event.getCount() * 50);
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
    PApplet.main(new String[]{"basics.MouseDragInteraction"});
  }
}
