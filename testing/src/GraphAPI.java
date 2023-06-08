import nub.core.Node;
import nub.primitives.Matrix;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Thorough graph api test based upon the following graph:
 * 1: red; 2: green; 3: blue; 4: yellow; 5: magenta; detached1: cyan; detached2:grey
 * World
 *   ^
 *   |\
 *   1 eye
 *   ^
 *   |\
 *   2 3
 *   |
 *   4
 *   |
 *   5
 */
public class GraphAPI extends PApplet {
  Scene scene;
  Node n1, n2, n3, n4, n5, detached1, detached2, clone;

  //Choose FX2D, JAVA2D, P2D or P3D
  String renderer = P3D;

  public void settings() {
    size(900, 900, renderer);
  }

  public void setup() {
    scene = new Scene(this);

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
    else
      scene.moveForward(mouseX - pmouseX);
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
    PApplet.main(new String[]{"GraphAPI"});
  }
}
