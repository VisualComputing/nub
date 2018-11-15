package intellij;

import frames.core.Graph;
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
  Vector randomVector;
  boolean cad, lookAround;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    //scene.setAperture(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(1000);
    scene.fitBallInterpolation();

    Shape shape1 = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pGraphics) {
        Scene.drawAxes(pGraphics, scene.radius() / 3);
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

    Shape shape2 = new Shape(shape1);
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
    // visit scene frames (shapes simply get drawn)
    scene.traverse();
  }

  public void keyPressed() {
    if (key == 'e')
      scene.enableBackBuffer();
    if (key == 'd')
      scene.disableBackBuffer();
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
        scene.fitBall();
      }
    }
    if (key == 'a')
      lookAround = !lookAround;
    if (key == 'r')
      scene.setRightHanded();
    if (key == 'l')
      scene.setLeftHanded();
    if (key == 'p')
      if (scene.type() == Graph.Type.PERSPECTIVE)
        scene.setType(Graph.Type.ORTHOGRAPHIC);
      else
        scene.setType(Graph.Type.PERSPECTIVE);
  }

  @Override
  public void mouseMoved() {
    scene.cast();
    //scene.track();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      if (cad) {
        //scene.mouseCAD(randomVector);
        scene.rotateCAD(randomVector);
      } else if (lookAround) {
        //scene.lookAround();
        scene.lookAround();
      } else {
        //scene.spin();
        scene.spin();
      }
    else if (mouseButton == RIGHT) {
      //scene.translate();
      //scene.translate(scene.mouseDX(), scene.mouseDY(), scene.defaultFrame());
      scene.translate();
    } else {
      //scene.mouseZoom(mouseX - pmouseX);
      //scene.zoom(scene.mouseDX(), scene.defaultFrame());
      scene.scale(scene.mouseDX());
      //scene.scale(mouseX - pmouseX);
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
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
    PApplet.main(new String[]{"intellij.MouseDragInteraction"});
  }
}
