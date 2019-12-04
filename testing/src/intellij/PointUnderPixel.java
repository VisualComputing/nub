package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class PointUnderPixel extends PApplet {
  Scene scene;
  Node[] models;

  //Point screenPoint = new Point();
  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();

  Vector pup;

  @Override
  public void settings() {
    size(1000, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    //scene = new Scene(this, P3D, width, height);
    scene.setRadius(1000);
    scene.fit(1);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(scene, boxShape());
      models[i].setPickingThreshold(0);
      scene.randomize(models[i]);
    }
  }

  public void draw() {
    background(0);
    drawRay();
    scene.render();
    /*
    scene.beginDraw();
    scene.context().background(0);
    scene.render();
    scene.endDraw();

     */
  }

  // /*
  void drawRay() {
    if (pup != null) {
      pushStyle();
      strokeWeight(10);
      stroke(255, 255, 0);
      point(pup.x(), pup.y(), pup.z());
      stroke(0, 0, 255);
      line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
      popStyle();
    }
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getButton() == RIGHT) {
      pup = scene.pointUnderPixel(mouseX, mouseY);
      if (pup != null) {
        scene.convertPixelToLine(mouseX, mouseY, orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      if (event.getCount() == 1)
        scene.focus();
      else
        scene.align();
    }
  }
  // */

  PShape boxShape() {
    PShape box = createShape(BOX, 60);
    box.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return box;
  }

  @Override
  public void mouseMoved() {
    scene.mouseTag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.moveForward(scene.mouseDX());
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PointUnderPixel"});
  }
}
