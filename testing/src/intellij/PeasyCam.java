package intellij;

import nub.core.Node;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class PeasyCam extends PApplet {
  //Node box1, box2;
  Scene scene;
  boolean cad;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    //scene.togglePerspective();
    Node box1 = new Node(scene) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(255, 0, 0);
        pg.box(30);
        pg.popStyle();
      }
    };
    box1.setPickingThreshold(0);
    Node box2 = new Node(box1) {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.strokeWeight(1 / 10f);
        pg.fill(0, 0, 255);
        pg.box(5);
        pg.popStyle();
      }
    };
    box2.setPickingThreshold(0);
    box2.translate(0, 0, 20);
    scene.setRadius(50);
    scene.fit(1);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    stroke(125);
    scene.drawGrid();
    lights();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    switch (mouseButton) {
      case LEFT:
        if (!scene.mouseSpinTag())
          scene.mouseDampedSpinEye();
        //scene.mouseSpinEye();
        break;
      case RIGHT:
        if (!scene.mouseTranslateTag())
          scene.mouseDampedTranslateEye();
        //scene.rotateEye(scene.mouseRADY(), 0, 0);
        //scene.rotateEye(0, scene.mouseRADX(), 0);
        //scene.rotateEye(0, 0, scene.mouseRADX());
        break;
      case CENTER:
        if (cad)
          scene.mouseDampedRotateCAD();
        else
          scene.mouseDampedLookAround();
        break;
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.dampedMoveForward(event.getCount() * 40);
  }

  public void keyPressed() {
    if (key == 'f')
      scene.flip();
    if (key == 'c')
      cad = !cad;
    if (key == 's')
      scene.fit(1);
    if (key == 'S')
      scene.fit();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.PeasyCam"});
  }
}
