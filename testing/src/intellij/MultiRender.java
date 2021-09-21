package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.event.MouseEvent;
import processing.opengl.PGraphics2D;
import processing.opengl.PShader;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class MultiRender extends PApplet {
  Scene scene;
  Node node, child, sibling;
  PShader tShader, lShader, pShader;
  PGraphics fb, bb;
  int fbID, bbID;
  boolean io;

  boolean onscreen = false;
  //Choose P3D for a 3D scene, or P2D for a 2D one
  String renderer = P2D;

  public void settings() {
    size(1200, 1200, renderer);
  }

  public void setup() {
    //scene = new Scene(this, 150);
    fb = createGraphics(300, 300, P2D);
    fbID = (int) random(0, 16777216);
    bb = createGraphics(300, 300, P2D);
    bb.noSmooth();
    bbID = (int) random(0, 16777216);
    // stroke params should go in draw in all cases
    tShader = loadShader("Picking.frag");
    lShader = loadShader("Picking.frag", "LinePicking.vert");
    pShader = loadShader("Picking.frag", "PointPicking.vert");
    bb.shader(tShader);
    bb.shader(lShader, PApplet.LINES);
    bb.shader(pShader, PApplet.POINTS);
  }

  public void emit(int id) {
    float r = Node.redID(id);
    float g = Node.greenID(id);
    float b = Node.blueID(id);
    tShader.set("id", new PVector(r, g, b));
    lShader.set("id", new PVector(r, g, b));
    pShader.set("id", new PVector(r, g, b));
  }

  public void draw() {
    background(125);
    if (onscreen) {
      fill(Node.colorID(fbID));
      strokeWeight(6);
      stroke(255);
      rect(20, 20, 100, 100);
    }
    else {
      //scene.render();
      fb.beginDraw();
      fb.background(255, 0, 255, 125);
      fb.fill(Node.colorID(fbID));
      fb.strokeWeight(6);
      fb.stroke(255);
      fb.rect(20, 20, 100, 100);
      fb.endDraw();
      //image(fb, 50, 50);
    }

    bb.beginDraw();
    bb.background(0, 255, 255, 125);
    //bb.fill(bbColor);
    //bb.strokeWeight(6);
    //bb.stroke(255);
    emit(fbID);
    bb.rect(20, 20, 100, 100);
    bb.endDraw();
    //image(bb, 370,370);

    if (onscreen) {
      strokeWeight(6);
      fill(Node.colorID(bbID));
      strokeWeight(6);
      stroke(0, 255, 0);
      rect(140, 140, 100, 100);
    }
    else {
      fb.beginDraw();
      //fb.background(255, 0, 255, 125);
      fb.fill(Node.colorID(bbID));
      fb.strokeWeight(6);
      fb.stroke(0,255, 0);
      fb.rect(140, 140, 100, 100);
      fb.endDraw();
      image(fb, 50, 50);
    }

    bb.beginDraw();
    //bb.background(0, 255, 255, 125);
    //bb.fill(bbColor);
    //bb.strokeWeight(6);
    //bb.stroke(0,255, 0);
    emit(bbID);
    bb.rect(140, 140, 100, 100);
    /*
    if (io) {
      bbIO();
      image(bb, 700,700);
    }
    // */
    bb.endDraw();
    image(bb, 370,370);
    // /*
    if (io) {
      bbIO();
      image(bb, 700,700);
    }
    // */
  }

  public void bbIO() {
    int halfImage = bb.width*bb.height/2;
    bb.loadPixels();
    for (int i = 0; i < halfImage; i++) {
      bb.pixels[i+halfImage] = bb.pixels[i];
    }
    bb.updatePixels();
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
  // */

  public void keyPressed() {
    if (key == '1') {
      fbID = (int) random(0, 16777216);
      emit(fbID);
    }
    if (key == '2') {
      bbID = (int) random(0, 16777216);
      emit(bbID);
    }
    if (key == '3') {
      io = !io;
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.MultiRender"});
  }
}
