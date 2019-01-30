package intellij;

import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.Node;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class BasicInterpolation extends PApplet {
  Scene scene;
  Interpolator interpolator;

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

    interpolator = new Interpolator(scene);
    interpolator.setLoop();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      interpolator.addKeyFrame(Node.random(scene));
    interpolator.start();
  }

  public void draw() {
    background(0);

    pushStyle();
    stroke(255);
    scene.drawPath(interpolator, 5);
    popStyle();

    pushStyle();
    fill(255, 0, 0, 125);
    scene.matrixHandler().pushModelView();
    scene.applyTransformation(interpolator.node());
    box(50);
    scene.matrixHandler().popModelView();
    popStyle();
  }

  @Override
  public void mouseMoved() {
    scene.track();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
      //scene.lookAround(upVector);
      //scene.mouseCAD();
    else if (mouseButton == RIGHT)
      scene.translate();
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
    if (key == '-')
      interpolator.setSpeed(interpolator.speed() - 0.25f);
    if (key == '+')
      interpolator.setSpeed(interpolator.speed() + 0.25f);

    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.BasicInterpolation"});
  }
}
