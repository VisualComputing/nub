package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by pierre on 11/15/16.
 */
public class FitNode extends PApplet {
  Scene scene;
  Node cajas, bolas;
  Node[] shapes;

  //Choose one of P3D for a 3D scene or P2D for a 2D one.
  String renderer = P3D;
  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    //scene = new Scene(createGraphics(w, h / 2, renderer), max(w, h));
    //scene = new Scene(createGraphics(w, h, renderer), max(w, h));
    scene = new Scene(this, 1200);
    cajas = new Node();
    cajas.enableHint(Node.BULLSEYE, color(0, 255, 0));
    bolas = new Node();
    bolas.enableHint(Node.BULLSEYE, color(255, 255, 0));
    scene.randomize(cajas);
    scene.randomize(bolas);
    shapes = new Node[10];
    for (int i = 0; i < shapes.length; i++) {
      if (i%2==0) {
        shapes[i] = new Node(cajas, caja());
      }
      else {
        shapes[i] = new Node(bolas, bola());
      }
      scene.randomize(shapes[i]);
      //shapes[i].enableHint(Node.CAMERA);
      shapes[i].enableHint(Node.AXES);
      shapes[i].setHUD(this::hud);
    }
    scene.fit(1000);
  }

  public void hud(PGraphics pg) {
    pg.pushStyle();
    pg.rectMode(CENTER);
    pg.fill(255, 0, 255, 125);
    pg.stroke(0,0,255);
    pg.strokeWeight(3);
    pg.rect(0, 0, 80, 50);
    pg.popStyle();
  }

  public void draw() {
    // 1. Fill in and display front-buffer
    /*
    background(0);
    scene.render(bolas);
    scene.render(cajas);
    // */
    scene.display(color(125));
    // /*
    //scene.display(color(125), bolas);
    //scene.display(cajas);
    // */
    /*
    scene.openContext();
    scene.context().background(125);
    scene.drawAxes();
    scene.render(cajas);
    //scene.render(bolas);
    scene.closeContext();
    //scene.image();
    // */

    /*
    scene.openContext();
    //scene.context().background(125);
    //scene.drawAxes();
    //scene.render(cajas);
    scene.render(bolas);
    scene.closeContext();
    scene.image();
    // */
  }

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
    if (key == 's') {
      scene.fit();
    }
    if (key == 'f') {
      scene.fit(shapes[3], 1000);
    }
    if (key == 't') {
      if (scene.eye().matches(shapes[3])) {
        println("win match 3");
      }
      else {
        println(scene.eye().toString());
        println(shapes[3].toString());
      }
    }
    if (key == 'F') {
      scene.fit(shapes[8], 1000);
    }
    if (key == 'T') {
      if (scene.eye().matches(shapes[8])) {
        println("win match 8");
      }
      else {
        println(scene.eye().toString());
        println(shapes[8].toString());
      }
    }
    //
    if (key == 'p') {
      if (scene.eye().position().matches(shapes[3].position())) {
        println("win position match 3");
      }
    }
    if (key == 'o') {
      if (scene.eye().orientation().matches(shapes[3].orientation())) {
        println("win orientation match 3");
      }
    }
    if (key == 'm') {
      if (scene.eye().magnitude() == shapes[3].magnitude()) {
        println("win magnitude match 3");
      }
      else {
        println(scene.eye().magnitude() - shapes[3].magnitude());
      }
    }
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  PShape bola() {
    PShape bola = scene.is3D() ? createShape(SPHERE, random(60, 100)) : createShape(ELLIPSE, 0, 0, random(60, 100), random(60, 100));
    //bola.noStroke();
    bola.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    bola.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return bola;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.FitNode"});
  }
}
