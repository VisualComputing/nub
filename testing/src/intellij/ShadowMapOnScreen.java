package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class ShadowMapOnScreen extends PApplet {
  Graph.Type shadowMapType = Graph.Type.ORTHOGRAPHIC;
  Scene scene;
  Frame[] shapes;
  Frame light;
  boolean show = true;
  PGraphics shadowMap;
  float zNear = 50;
  float zFar = 1000;
  int w = 1000;
  int h = 1000;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    shapes = new Frame[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Frame(scene);
      shapes[i].shape(caja());
      shapes[i].randomize();
    }
    light = new Frame(scene) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        Scene.drawAxes(pg, 150);
        pg.fill(isTracked() ? 255 : 25, isTracked() ? 0 : 255, 255);
        Scene.drawFrustum(pg, shadowMap, shadowMapType, this, zNear, zFar);
        pg.popStyle();
      }
    };
    scene.fit(1);
    shadowMap = createGraphics(w / 2, h / 2, P3D);
  }

  public void draw() {
    background(90, 80, 125);
    // 1. Fill in and display front-buffer
    scene.render();
    // 2. Fill in shadow map using the light point of view
    shadowMap.beginDraw();
    shadowMap.background(120);
    scene.render(shadowMap, shadowMapType, light, zNear, zFar);
    shadowMap.endDraw();
    // 3. Display shadow map
    if (show) {
      scene.beginHUD();
      image(shadowMap, w / 2, h / 2);
      scene.endHUD();
    }
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
      scene.moveForward(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ')
      show = !show;
    if (key == 't')
      shadowMapType = shadowMapType == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC;
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }


  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ShadowMapOnScreen"});
  }
}
