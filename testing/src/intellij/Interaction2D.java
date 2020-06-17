package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class Interaction2D extends PApplet {
  Scene scene;
  Node shape1, shape2, shape3;
  Vector upVector;
  PFont font36;

  public void settings() {
    size(2600, 1400, P2D);
  }

  public void setup() {
    font36 = loadFont("FreeSans-36.vlw");
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(1000);
    //scene.eye().setScaling(1);
    scene.fit(1);

    shape1 = new Node() {
      @Override
      public void graphics(PGraphics pGraphics) {
        Scene.drawAxes(pGraphics, scene.radius() / 3);
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(0, 255, 255);
        pGraphics.rect(10, 10, 200, 200);
        pGraphics.stroke(255, 0, 0);
        /*
        scene._drawSquaredBullsEye(this);
        scene.beginHUD(pGraphics);
        Vector position = scene.screenLocation(position());
        pGraphics.fill(isTracked() ? 0 : 255, isTracked() ? 255 : 0, isTracked() ? 0 : 255);
        pGraphics.textFont(font36);
        pGraphics.text("center", position.x(), position.y());
        scene.endHUD(pGraphics);
        */
        pGraphics.popStyle();
      }
    };
    shape1.enableHint(Node.BULLSEYE);
    //shape1.setRotation(Quaternion.random());
    shape1.translate(-375, 175, 0);

    shape2 = new Node(shape1);
    shape2.setShape(shape());
    shape2.translate(75, 475, 0);

    shape3 = new Node(shape2);
    shape3.setShape(createShape(RECT, 0, 0, 150, 150));
    shape3.translate(-775, -575, 0);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.render();

    scene.beginHUD();
    Vector position = scene.screenLocation(shape1.position());
    fill(shape1.isTagged(scene) ? 0 : 255, shape1.isTagged(scene) ? 255 : 0, shape1.isTagged(scene) ? 0 : 255);
    textFont(font36);
    text("center", position.x(), position.y());
    scene.endHUD();

    scene.beginHUD();
    textFont(font36);
    text("Hello World", mouseX, mouseY);
    scene.endHUD();
  }

  public void keyPressed() {
    if (key == 'f')
      Scene.leftHanded = !Scene.leftHanded;
    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();
    if (key == 'r')
      if (shape3.reference() == shape2) {
        shape3.setReference(shape1);
      } else {
        shape3.setReference(shape2);
      }
    if (key == 'w') {
      shape3.setReference(null);
    }
    if (key == 'p') {
      print(scene.screenLocation(shape1.position()).toString());
      println(mouseX + " " + mouseY);
    }
  }

  @Override
  public void mouseMoved() {
    scene.updateMouseTag();
  }

  public void mousePressed() {
    upVector = scene.eye().yAxis();
  }

  public void mouseDragged() {
    // Note 1.
    // When mouse methods are invoked without the node parameter
    // the scene.defaultNode is used.
    // Note 2.
    // Mouse methods that don't take a node parameter (such as mouseCAD)
    // are only available to the scene.eye().
    if (mouseButton == LEFT)
      scene.mouseSpin();
      //scene.mouseCAD();
      //scene.lookAround(upVector);
      //scene.mouseCAD();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
      //scene.mousePan();
    else
      //scene.zoom(mouseX - pmouseX);
      scene.scale(mouseX - pmouseX);
    //scene.rotate(0, 0, mouseX - pmouseX, PI/width);
  }

  public void mouseWheel(MouseEvent event) {
    //scene.zoom(event.getCount() * 20);
    scene.scale(event.getCount() * 20);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.alignTag();
  }

  PShape shape() {
    PShape fig = scene.is3D() ? createShape(BOX, 150) : createShape(RECT, 0, 0, 150, 150);
    fig.setStroke(255);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
    return fig;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.Interaction2D"});
  }
}
