package basics;

import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class DettachedShapes extends PApplet {
  Scene scene;
  Shape[] shapes;
  Shape eye, trackedShape;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    eye = new Shape();
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();
    shapes = new Shape[25];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Shape(shape());
      scene.randomize(shapes[i]);
    }
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    for (int i = 0; i < shapes.length; i++) {
      shapes[i].draw(this);
      scene.drawShooterTarget(shapes[i]);
    }
  }

  public void keyPressed() {
    if (key == 's')
      scene.fitBallInterpolation();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin(defaultShape());
    else if (mouseButton == RIGHT)
      scene.mouseTranslate(defaultShape());
    else
      scene.scale(mouseX - pmouseX, defaultShape());
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 50, defaultShape());
  }

  public void mouseClicked(MouseEvent event) {
    for (int i = 0; i < shapes.length; i++) {
      trackedShape = null;
      if (scene.track(mouseX, mouseY, shapes[i])) {
        trackedShape = shapes[i];
        break;
      }
    }
  }

  Shape defaultShape() {
    return trackedShape == null ? eye : trackedShape;
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 15) : createShape(RECT, 0, 0, 15, 15);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.DettachedShapes"});
  }
}