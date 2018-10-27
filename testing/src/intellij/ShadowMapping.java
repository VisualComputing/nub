package intellij;

import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class ShadowMapping extends PApplet {
  Scene scene;
  Shape[] shapes;
  Shape eye;
  PGraphics shadowMap;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this, createGraphics(w, h / 2, renderer));
    scene.setRadius(max(w, h));

    shapes = new Shape[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Shape(scene);
      shapes[i].setGraphics(caja());
      shapes[i].randomize();
    }
    eye = new Shape(scene) {
      // Note that within visit() geometry is defined at the
      // frame local coordinate system.
      @Override
      public void setGraphics(PGraphics pg) {
        pg.pushStyle();
        scene.drawAxes();
        pg.fill(0, isTracked() ? 255 : 0, 255, 125);
        pg.stroke(0, 0, 255);
        pg.strokeWeight(2);
        pg.sphere(30);
        pg.popStyle();
      }
    };
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    shadowMap = createGraphics(w, h / 2, renderer);
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    scene.beginDraw();
    scene.frontBuffer().background(10, 50, 25);
    scene.traverse();
    scene.endDraw();
    scene.display();
    // 2. Display shadow map
    shadowMap.beginDraw();
    shadowMap.background(0);
    scene.traverse(shadowMap, eye);
    shadowMap.endDraw();
    image(shadowMap, 0, h / 2);
  }

  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 20);
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMapping"});
  }
}
