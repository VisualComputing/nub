package intellij;

import nub.core.Graph;
import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class BasicInterpolation extends PApplet {
  Scene scene;
  Interpolator interpolator;
  float speed = 1;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(150);
    scene.setType(Graph.Type.ORTHOGRAPHIC);

    // interpolation 1. Default eye interpolations
    scene.fit(1);

    interpolator = new Interpolator();
    interpolator.enableRecurrence();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      interpolator.addKeyFrame(Node.random(scene));
    interpolator.run();
  }

  public void draw() {
    background(0);

    pushStyle();
    stroke(255);
    scene.drawCatmullRom(interpolator, 5);
    popStyle();

    pushStyle();
    fill(255, 0, 0, 125);
    scene.matrixHandler().pushMatrix();
    scene.applyTransformation(interpolator.node());
    box(50);
    scene.matrixHandler().popMatrix();
    popStyle();
  }

  @Override
  public void mouseMoved() {
    scene.updateMouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
      //scene.lookAround(upVector);
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

  public void keyPressed() {
    if (key == '-' || key == '+') {
      if (key == '-')
        speed -= 0.25f;
      else
        speed += 0.25f;
      interpolator.run(speed);
    }

    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.BasicInterpolation"});
  }
}
