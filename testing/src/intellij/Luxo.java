package intellij;

import nub.core.Node;
import nub.core.constraint.AxisPlaneConstraint;
import nub.core.constraint.LocalConstraint;
import nub.core.constraint.WorldConstraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class Luxo extends PApplet {
  int w = 800;
  int h = 600;

  public void settings() {
    size(w, h, P3D);
  }

  Scene scene;
  Lamp lamp;

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(100);
    scene.fit(1);
    lamp = new Lamp(scene);
  }

  public void draw() {
    background(255);
    lights();

    scene.drawAxes();
    //draw the lamp
    scene.render();

    //draw the ground
    noStroke();
    fill(125);
    float nbPatches = 100;
    normal(0.0f, 0.0f, 1.0f);
    for (int j = 0; j < nbPatches; ++j) {
      beginShape(QUAD_STRIP);
      for (int i = 0; i <= nbPatches; ++i) {
        vertex((200 * (float) i / nbPatches - 100), (200 * j / nbPatches - 100));
        vertex((200 * (float) i / nbPatches - 100), (200 * (float) (j + 1) / nbPatches - 100));
      }
      endShape();
    }
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  class Lamp implements PConstants {
    Scene scene;
    Piece[] pieces;

    Lamp(Scene s) {
      scene = s;
      pieces = new Piece[4];

      for (int i = 0; i < 4; ++i) {
        pieces[i] = new Piece();
        node(i).setReference(i > 0 ? pieces[i - 1] : null);
      }

      // Initialize nodes
      node(1).setTranslation(0f, 0f, 8f); // Base height
      node(2).setTranslation(0, 0, 50);  // Arm length
      node(3).setTranslation(0, 0, 50);  // Arm length

      node(1).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), 0.6f));
      node(2).setRotation(new Quaternion(new Vector(1.0f, 0.0f, 0.0f), -2.0f));
      node(3).setRotation(new Quaternion(new Vector(1.0f, -0.3f, 0.0f), -1.7f));

      // Set node graphics modes
      node(0).mode = 1;
      node(1).mode = 2;
      node(2).mode = 2;
      node(3).mode = 3;

      // Set node constraints
      WorldConstraint baseConstraint = new WorldConstraint();
      baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vector(0.0f, 0.0f, 1.0f));
      baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(0.0f, 0.0f, 1.0f));
      node(0).setConstraint(baseConstraint);

      LocalConstraint XAxis = new LocalConstraint();
      XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
      XAxis.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vector(1.0f, 0.0f, 0.0f));
      node(1).setConstraint(XAxis);
      node(2).setConstraint(XAxis);

      LocalConstraint headConstraint = new LocalConstraint();
      headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vector(0.0f, 0.0f, 0.0f));
      node(3).setConstraint(headConstraint);
    }

    Piece node(int i) {
      return pieces[i];
    }
  }

  class Piece extends Node {
    int mode;

    void drawCone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
      pg.translate(0.0f, 0.0f, zMin);
      Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax - zMin);
      pg.translate(0.0f, 0.0f, -zMin);
    }

    @Override
    public void graphics(PGraphics pGraphics) {
      switch (mode) {
        case 1:
          pGraphics.fill(isTagged(scene) ? 255 : 0, 0, 255);
          drawCone(pGraphics, 0, 3, 15, 15, 30);
          drawCone(pGraphics, 3, 5, 15, 13, 30);
          drawCone(pGraphics, 5, 7, 13, 1, 30);
          drawCone(pGraphics, 7, 9, 1, 1, 10);
          break;
        case 2:
          pGraphics.pushMatrix();
          pGraphics.rotate(HALF_PI, 0, 1, 0);
          drawCone(pGraphics, -5, 5, 2, 2, 20);
          pGraphics.popMatrix();

          pGraphics.translate(2, 0, 0);
          drawCone(pGraphics, 0, 50, 1, 1, 10);
          pGraphics.translate(-4, 0, 0);
          drawCone(pGraphics, 0, 50, 1, 1, 10);
          pGraphics.translate(2, 0, 0);
          break;
        case 3:
          pGraphics.fill(0, 255, isTagged(scene) ? 0 : 255);
          drawCone(pGraphics, -2, 6, 4, 4, 30);
          drawCone(pGraphics, 6, 15, 4, 17, 30);
          drawCone(pGraphics, 15, 17, 17, 17, 30);
          pGraphics.spotLight(155, 255, 255, 0, 0, 0, 0, 0, 1, THIRD_PI, 1);
          break;
      }
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Luxo"});
  }
}
