package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

public class PointUnderPixel extends PApplet {
  Scene scene;
  Node[] models;

  Vector orig = new Vector();
  Vector dir = new Vector();
  Vector end = new Vector();
  Vector pup;

  // offScreen breaks reading depths in Processing
  // try offScreen = false to see how it should work
  boolean offScreen = false;

  @Override
  public void settings() {
    size(800, 800, P3D);
    //noSmooth();
  }

  public void setup() {
    scene = offScreen ? new Scene(createGraphics(width, height, P3D)) : new Scene(this);
    scene.setRadius(1000);
    scene.enableHint(Scene.BACKGROUND, color(0));
    scene.enableHint(Scene.SHAPE);
    scene.setShape(this::drawRay);
    scene.fit(1);
    models = new Node[100];
    for (int i = 0; i < models.length; i++) {
      models[i] = new Node(boxShape());
      scene.randomize(models[i]);
    }
    if (offScreen) {
      scene.context().hint(PConstants.ENABLE_BUFFER_READING);
    }
  }

  public void draw() {
    scene.display();
  }

  void drawRay(PGraphics pg) {
    if (pup != null) {
      pg.pushStyle();
      pg.strokeWeight(20);
      pg.stroke(255, 255, 0);
      pg.point(pup.x(), pup.y(), pup.z());
      pg.strokeWeight(8);
      pg.stroke(0, 0, 255);
      pg.line(orig.x(), orig.y(), orig.z(), end.x(), end.y(), end.z());
      pg.popStyle();
    }
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getButton() == RIGHT) {
      pup = scene.mouseLocation();
      if (pup != null) {
        scene.mouseToLine(orig, dir);
        end = Vector.add(orig, Vector.multiply(dir, 4000.0f));
      }
    } else {
      if (event.getCount() == 1)
        scene.focus();
      else
        scene.align();
    }
  }

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

  @Override
  public void keyPressed() {
    if (key == ' ')
      scene.togglePerspective();
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PointUnderPixel"});
  }
}
