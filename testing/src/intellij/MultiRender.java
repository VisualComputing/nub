package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;
import processing.opengl.PGraphics2D;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class MultiRender extends PApplet {
  Scene scene;
  Node node, child, sibling;
  PGraphics fb, bb;
  int fbColor, bbColor;

  boolean onscreen = true;
  //Choose P3D for a 3D scene, or P2D for a 2D one
  String renderer = P2D;

  public void settings() {
    size(1200, 1200, renderer);
  }

  public void setup() {
    //scene = new Scene(this, 150);
    fb = createGraphics(400, 400, P2D);
    bb = createGraphics(400, 400, P2D);
    fbColor = Node.colorID((int) random(0, 16777216));
    bbColor = Node.colorID((int) random(0, 16777216));
  }

  public void draw() {
    background(125);

    if (onscreen) {
      fill(fbColor);
      rect(20, 20, 100, 100);
    }
    else {
      //scene.render();
      fb.beginDraw();
      fb.background(255, 0, 255);
      fb.fill(fbColor);
      fb.rect(20, 20, 100, 100);
      fb.endDraw();
      image(fb, 50, 50);
    }

    bb.beginDraw();
    bb.background(0, 255, 255);
    bb.fill(bbColor);
    bb.rect(20, 20, 100, 100);
    bb.endDraw();
    image(bb, 550,550);

    if (onscreen) {
      fill(fbColor);
      rect(120, 120, 100, 100);
    }
    else {
      fb.beginDraw();
      //fb.background(255, 0, 255);
      fb.fill(fbColor);
      fb.rect(120, 120, 100, 100);
      fb.endDraw();
      image(fb, 50, 50);
    }

    bb.beginDraw();
    //bb.background(0, 255, 255);
    bb.fill(bbColor);
    bb.rect(120, 120, 100, 100);
    bb.endDraw();
    image(bb, 550,550);
  }

  /*
  public void mouseMoved() {
    scene.tag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {

  }
   */

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.MultiRender"});
  }
}
