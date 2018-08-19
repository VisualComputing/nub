package intellij;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ApplicationControl extends PApplet {
  Scene scene;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(240, 840, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);

    scene.fitBallInterpolation();

    Shape shape1 = new Shape(scene) {
      @Override
      public void setGraphics(PGraphics pGraphics) {
        Scene.drawTorusSolenoid(pGraphics);
      }
    };
    shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175);
  }

  public void draw() {
    background(0);
    fill(0, 255, 255);
    scene.drawAxes();
    scene.traverse();
  }

  void control() {
    control(scene.defaultFrame(null));
  }

  void control(String hid) {
    control(scene.defaultFrame(hid));
  }

  void control(Frame frame) {
    if (frame == null)
      println("null");
    else
      println("ctrl");
  }

  public void keyPressed() {

  }

  public void mouseMoved() {
    scene.cast("mouseMoved", scene.mouse());
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin("mouseMoved", scene.pmouse(), scene.mouse());
    else if (mouseButton == RIGHT)
      scene.translate("mouseMoved", scene.mouseDX(), scene.mouseDY());
    else
      control("mouseMoved");
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom("mouseMoved", event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 1)
      scene.cast("mouseClicked", scene.mouse());
    else if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus("mouseMoved");
      else
        scene.align("mouseMoved");
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ApplicationControl"});
  }

}
