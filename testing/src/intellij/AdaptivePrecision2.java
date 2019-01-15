package intellij;

import frames.core.Frame;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class AdaptivePrecision2 extends PApplet {
  Scene scene;
  Frame[] shapes;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.fit(1);
    shapes = new Frame[25];
    for (int i = 0; i < shapes.length; i++) {
      //shapes[i] = new Frame(scene, shape());
      shapes[i] = new Frame(scene);
      shapes[i].shape(shape());
      scene.randomize(shapes[i]);
      shapes[i].setPrecisionThreshold(0.25f);
    }
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    for (int i = 0; i < shapes.length; i++) {
      //scene.draw(shapes[i]);
      pushMatrix();
      scene.applyTransformation(shapes[i]);
      scene.draw(shapes[i]);
      popMatrix();
      pushStyle();
      stroke(255);
      scene.drawShooterTarget(shapes[i]);
      popStyle();
    }
  }

  public void keyPressed() {
    if (key == 's')
      scene.fit(1);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    scene.resetTrackedFrame();
    for (int i = 0; i < shapes.length; i++)
      if (scene.tracks(shapes[i])) {
        scene.setTrackedFrame(shapes[i]);
        break;
      }
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 15) : createShape(RECT, 0, 0, 15, 15);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.AdaptivePrecision2"});
  }
}
