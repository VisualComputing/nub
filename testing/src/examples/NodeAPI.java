package examples;

/*
1: red; 2: green; 3: blue; 4: yellow; 5: magenta; detached1: cyan; detached2:grey
World
  ^
  |\
  1 eye
  ^
  |\
  2 3
  |
  4
  |
  5
 */

import nub.core.Node;
import nub.core.Scene;
import nub.primitives.Matrix;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class NodeAPI extends PApplet {
  Scene scene;
  Node n1, n2, n3, n4, n5, detached1, detached2, clone;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(900, 900, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    Node eye = new Node();
    eye.setWorldPosition(0,0,300);
    scene.setEye(eye);

    // red
    n1 = new Node(shape(color(255, 0, 0, 125)));

    // green
    n2 = new Node(n1, shape(color(0, 255, 0, 125)));
    scene.randomize(n2);
    n2.scale(0.5f);

    // blue
    n3 = new Node(n1, shape(color(0, 0, 255, 125)));
    scene.randomize(n3);

    // yellow
    n4 = new Node(n2, shape(color(255, 255, 0, 125)));
    scene.randomize(n4);

    // magenta
    n5 = new Node(n4, shape(color(255, 0, 255, 125)));
    scene.randomize(n5);

    // cyan
    //detached1 = new Node();
    //Graph.detach(detached1);
    // same as two prev lines
    detached1 = new Node(false);
    detached1.set(n3);
    detached1.setShape(shape(color(0, 255, 255, 125)));

    // grey
    detached2 = new Node(false);
    detached2.setShape(shape(color(125, 125)));

    /*
    float dx = -10;
    float dy = 15;
    float dz = 5;
    Matrix t = new Matrix(1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            dx, dy, dz, 0);
    float b = QUARTER_PI;
    Matrix r = new Matrix(cos(b), sin(b), 0, 0,
            -sin(b), cos(b), 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1);
    println(t.toString());
    println(r.toString());
    println(Matrix.multiply(t, r).toString());
    t.rotateZ(b);
    println(t.toString());
    // */
    n3.setWorldMagnitude(1);
    n4.setWorldMagnitude(1);
    n3.fromWorldMatrix(new Matrix(0.756289f, -0.5062419f, -0.41442266f, 0.0f,
            -0.054126263f, 0.58285666f, -0.81077033f, 0.0f,
            0.65199494f, 0.6356078f, 0.4134071f, 0.0f,
            5.372316f, -7.620868f, -6.542904f, 1.0f));
    n4.fromWorldMatrix(new Matrix(0.94123274f, 0.18576221f, -0.28208724f, 0.0f,
            0.019796228f, 0.80339795f, 0.59511316f, 0.0f,
            0.33717784f, -0.56572425f, 0.75250715f, 0.0f,
            14.867519f, -4.308632f, -11.14892f, 1.0f));
    Matrix m3 = n3.worldMatrix();
    Matrix m4 = n4.worldMatrix();
    Matrix tm3 = m3.copy();
    Matrix tm4 = m4.copy();
    tm3.transpose();
    tm4.transpose();
    println(tm3.toString());
    //println(m4.toString());
    println(tm4.toString());
    Vector vec = new Vector(-5, 10, 15);
    // n4 -> n3
    println(vec.toString() + " n4 -> n3 loc: " + n3.location(vec, n4).toString());
    println(vec.toString() + " n4 -> n3 dis: " + n3.displacement(vec, n4).toString());
  }

  PShape shape(int c) {
    PShape pShape = createShape(BOX, 30);
    pShape.setFill(c);
    return pShape;
  }

  public void draw() {
    background(125);
    scene.render();
    scene.drawAxes();
  }

  public void keyPressed() {
    if (key == 'c') {
      clone = n5.copy();
      clone.resetHint();
      clone.enableHint(Node.TORUS);
      n5.resetHint();
      n5.enableHint(Node.AXES);
    }
    if (key == '1') {
      n4.setReference(n3);
    }
    if (key == '2') {
      detached1.setReference(n2);
    }
    if (key == '3') {
      n2.setReference(detached1);
    }
    if (key == '4') {
      detached2.setReference(detached1);
    }
    if (key == '5') {
      if (detached1 != null)
        detached1.attach();
    }
    if (key == '6') {
      if (detached2 != null)
        detached2.attach();
    }
    if (key == '7') {
      n2.copy();
    }
    if (key == '8') {
      n2.detach();
    }
    if (key == '9') {
      detached1.detach();
    }
  }

  @Override
  public void mouseMoved() {
    scene.tag();
  }

  @Override
  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.shift();
  }

  @Override
  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 20);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"examples.NodeAPI"});
  }
}
