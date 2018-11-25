package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class ShiftViewers extends PApplet {
  Scene scene1, scene2, scene3, focus;
  Shape[] models;
  boolean displayAuxiliarViewers = true;
  // whilst scene1 is either on-screen or not; scene2 and scene3 are off-screen
  // test both cases here
  boolean onScreen;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene1 = onScreen ? new Scene(this) : new Scene(this, P3D);
    scene1.setRadius(1000);
    // set a detached eye frame
    scene1.setEye(new Frame());
    scene1.setAperture(Graph.Type.PERSPECTIVE, THIRD_PI);
    scene1.fitBallInterpolation();
    models = new Shape[50];
    for (int i = 0; i < models.length; i++) {
      if ((i & 1) == 0) {
        models[i] = new Shape(scene1, boxShape());
      } else {
        models[i] = new Shape(scene1) {
          int _faces = (int) ShiftViewers.this.random(3, 15), _color = color(ShiftViewers.this.random(255), ShiftViewers.this.random(255), ShiftViewers.this.random(255));

          @Override
          public void setGraphics(PGraphics pg) {
            pg.pushStyle();
            pg.fill(_color);
            scene1.drawTorusSolenoid(pg, _faces, scene1.radius() / 30);
            pg.popStyle();
          }
        };
      }
      scene1.randomize(models[i]);
    }

    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, P3D, w / 2, h / 2, w / 2, 0);
    scene2.setRadius(1000);
    // set a detached eye frame
    scene2.setEye(new Frame());
    scene2.setAperture(Graph.Type.PERSPECTIVE, THIRD_PI);
    scene2.fitBallInterpolation();

    // idem here
    scene3 = new Scene(this, P3D, w / 2, h / 2, w / 2, h / 2);
    scene3.setRadius(1000);
    // set a detached eye frame
    scene3.setEye(new Frame());
    scene3.setAperture(Graph.Type.PERSPECTIVE, THIRD_PI);
    scene3.fitBallInterpolation();
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
      focus.fitBallInterpolation();
    if (key == 't') {
      if (focus.type() == Graph.Type.PERSPECTIVE) {
        focus.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        focus.setType(Graph.Type.PERSPECTIVE);
      }
    }
    if (key == 's')
      scene1.shift(scene3);
    if (key == 't')
      scene3.shift(scene1);
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
    focus = displayAuxiliarViewers ? (mouseX > w / 2 && mouseY < h / 2) ? scene2
        : (mouseX > w / 2 && mouseY > h / 2) ? scene3 : scene1 : scene1;

    background(75, 25, 15);

    if (scene1.isOffscreen()) {
      scene1.beginDraw();
      scene1.frontBuffer().background(75, 25, 15);
      scene1.drawAxes();
      scene1.traverse();
      scene1.endDraw();
      scene1.display();
    } else {
      scene1.drawAxes();
      scene1.traverse();
    }

    if (displayAuxiliarViewers) {
      scene1.shift(scene2);
      if (!scene1.isOffscreen())
        scene1.beginHUD();
      scene2.beginDraw();
      scene2.frontBuffer().background(175, 200, 20);
      scene2.drawAxes();
      scene2.traverse();
      scene2.endDraw();
      scene2.display();
      if (!scene1.isOffscreen())
        scene1.endHUD();

      scene2.shift(scene3);
      if (!scene1.isOffscreen())
        scene1.beginHUD();
      scene3.beginDraw();
      scene3.frontBuffer().background(125, 80, 90);
      scene3.drawAxes();
      scene3.traverse();
      scene3.endDraw();
      scene3.display();
      if (!scene1.isOffscreen())
        scene1.endHUD();

      scene3.shift(scene1);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShiftViewers"});
  }
}
