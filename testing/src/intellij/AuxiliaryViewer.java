package intellij;

import frames.core.Graph;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class AuxiliaryViewer extends PApplet {
  Scene scene1, scene2, focus;
  Shape[] models;
  Shape[] shapes;
  boolean displayAuxiliarViewer = true;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene1 = new Scene(this, P3D, w, h);
    //scene1 = new Scene(this);
    scene1.setRadius(1000);
    //scene1.setAperture(Graph.Type.ORTHOGRAPHIC);
    scene1.fitBallInterpolation();
    models = new Shape[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Shape(scene1, boxShape());
      scene1.randomize(models[i]);
    }

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, P3D, w / 2, h / 2, w / 2, h / 2);
    scene2.setRadius(500);
    scene2.fitBallInterpolation();
    shapes = new Shape[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Shape(scene2, boxShape());
      scene1.randomize(models[i]);
    }
    if (scene1.isOffscreen())
      println("offScreen!");
    else
      println("onScreen!");
  }

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  public void keyPressed() {
    if (key == ' ')
      displayAuxiliarViewer = !displayAuxiliarViewer;
    if (key == 'f')
      focus.fitBallInterpolation();
    if (key == 't') {
      if (focus.type() == Graph.Type.PERSPECTIVE) {
        focus.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        focus.setType(Graph.Type.PERSPECTIVE);
      }
    }
  }

  @Override
  public void mouseMoved() {
    focus.cast();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.translate();
    else
      focus.moveForward(focus.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void draw() {
    focus = displayAuxiliarViewer ? (mouseX > w / 2 && mouseY > h / 2) ? scene2 : scene1 : scene1;
    background(0);
    if (scene1.isOffscreen()) {
      scene1.beginDraw();
      scene1.frontBuffer().background(0);
      scene1.traverse();
      scene1.endDraw();
      scene1.display();
    } else
      scene1.traverse();

    if (displayAuxiliarViewer) {
      if (!scene1.isOffscreen())
        scene1.beginHUD();
      scene2.beginDraw();
      scene2.frontBuffer().background(125);
      scene2.drawAxes();
      scene2.traverse();
      scene2.endDraw();
      scene2.display();
      if (!scene1.isOffscreen())
        scene1.endHUD();
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.AuxiliaryViewer"});
  }
}
