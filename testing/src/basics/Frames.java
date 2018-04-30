package basics;

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

public class Frames extends PApplet {
  Frame frame1, frame2, frame3, frame4, frame5, frame6;
  boolean world;
  int translation, rotation;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(800, 800, renderer);
  }

  public void setup() {
    frame1 = new Frame();
    frame1.translate(0, height / 2);
    frame2 = new Frame();
    frame2.translate(50, 0);
    frame2.setReference(frame1);
    Quaternion q = Quaternion.random();
    frame3 = new Frame(frame2, new Vector(200, -150), q);
    frame4 = new Frame(frame2, new Vector(200, -150), q, 3.2f);
    frame4.worldMatrix().print();

    //frame5 = new Frame();
    frame6 = new Frame();
    frame6.setReference(frame3);

    //frame5._fromMatrix(frame4.worldMatrix(), 3.2f);
    //frame5.worldMatrix().print();

    //frame6.fromMatrix(frame4.matrix());
    frame6.fromWorldMatrix(frame4.worldMatrix());
    //frame6.matrix().print();
    //frame6.worldMatrix().print();
  }

  // Scene.applyTransformation does the same as apply(PMatrix), but:
  // 1. It also works in 2D.
  // 2. It's far more efficient (apply(PMatrix) computes the inverse).
  public void draw() {
    background(0);
    updateFrames();
    push();
    Scene.applyTransformation(this.g, frame1);
    stroke(0, 255, 0);
    fill(255, 0, 255, 125);
    bola(100);
    push();
    Scene.applyTransformation(this.g, frame2);
    push();
    Scene.applyTransformation(this.g, frame3);
    stroke(255, 0, 0);
    fill(0, 255, 255);
    caja(100);
    pop();
    pop();
    pop();
  }

  void bola(float radius) {
    if (g.is3D())
      sphere(radius);
    else
      ellipse(0, 0, radius, radius);
  }

  void caja(float length) {
    if (g.is3D())
      box(length);
    else
      rect(0, 0, length, length);
  }

  void push() {
    pushStyle();
    pushMatrix();
  }

  void pop() {
    popStyle();
    popMatrix();
  }

  void updateFrames() {
    if (world)
      translation++;
    else
      --rotation;
    frame1.setTranslation(translation % width, height / 2);
    frame2.setRotation(new Quaternion(radians(rotation)));
  }

  public void keyPressed() {
    world = !world;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Frames"});
  }
}
