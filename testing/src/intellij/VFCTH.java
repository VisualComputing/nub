package intellij;

import nub.core.Graph;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class VFCTH extends PApplet {
  Scene scene1, scene2, focus;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;
  int w = 1200;
  int h = 800;
  static float a = 100;
  static float b = 70;
  static float c = 130;
  static final int levels = 4;

  public void settings() {
    size(w, h, renderer);
  }

  @Override
  public void setup() {
  }

  @Override
  public void draw() {
    handleMouse();
    background(255);
    if (scene1 != null) {
      scene1.beginDraw();
      scene1.context().background(255);
      scene1.drawAxes();
      scene1.render();
      scene1.endDraw();
      scene1.display();
    }

    if (scene2 != null) {
      //scene1.shift(scene2);
      scene2.beginDraw();
      scene2.context().background(125);
      scene2.render();
      scene2.context().pushStyle();
      scene2.context().strokeWeight(2);
      scene2.context().stroke(255, 0, 255);
      scene2.context().fill(255, 0, 255, 160);
      if (scene1 != null)
        scene2.drawFrustum(scene1);
      scene2.context().popStyle();
      scene2.endDraw();
      scene2.display();
      //scene2.shift(scene1);
    }
  }

  public void mouseDragged() {
    if (focus == null) return;
    if (mouseButton == LEFT)
      focus.mouseSpinEye();
    else if (mouseButton == RIGHT)
      focus.mouseTranslateEye();
    else
      focus.scaleEye(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (focus == null) return;
    focus.moveForward(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (focus == null) return;
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focusEye();
      else
        focus.alignEye();
  }

  public void keyPressed() {
    if (key == ' ')
      if (focus.type() == Graph.Type.PERSPECTIVE)
        focus.setType(Graph.Type.ORTHOGRAPHIC);
      else
        focus.setType(Graph.Type.PERSPECTIVE);
    if (key == 'f') {
      scene1.flip();
      scene2.flip();
    }
    if (key == '1') {
      scene1 = new Scene(this, P3D, w, h / 2);
      //scene1.setType(Graph.Type.ORTHOGRAPHIC);
      scene1.enableBoundaryEquations();
      //scene1.setRadius(150);
      scene1.fit(1);
    }
    if (key == '2') {
      scene2 = new Scene(this, P3D, w, h / 2, 0, h / 2);
      //scene2 = new Scene(this, createGraphics(w, h / 2, renderer));
      //scene2.setType(Graph.Type.ORTHOGRAPHIC);
      scene2.setRadius(200);
      scene2.fit();
    }
    if (key == 'p') {

    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.VFCTH"});
  }
}
