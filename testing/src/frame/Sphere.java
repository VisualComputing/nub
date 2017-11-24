package frame;

import processing.core.PApplet;
import remixlab.input.event.KeyEvent;
import remixlab.input.event.MotionEvent;
import remixlab.core.Node;
import remixlab.primitives.Vector;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class Sphere {
  Scene scene;
  PApplet parent;
  Node iFrame;
  float r;
  int c;

  public Sphere(Scene scn) {
    scene = scn;
    parent = scn.pApplet();
    iFrame = new Node(scene) {
      @Override
      public void interact(MotionEvent event) {
        switch (event.shortcut().id()) {
          case PApplet.LEFT:
            rotate(event);
            break;
          case PApplet.RIGHT:
            translate(event);
            break;
          case processing.event.MouseEvent.WHEEL:
            scale(event);
            break;
        }
      }

      @Override
      public void interact(KeyEvent event) {
        if (event.id() == PApplet.UP)
          _translateY(true);
        if (event.id() == PApplet.DOWN)
          _translateY(false);
        if (event.id() == PApplet.LEFT)
          _translateX(false);
        if (event.id() == PApplet.RIGHT)
          _translateX(true);
      }
    };
    iFrame.setPrecision(Node.Precision.ADAPTIVE);
    setRadius(10);
  }

  public void draw() {
    draw(true);
  }

  public void draw(boolean drawAxes) {
    parent.pushMatrix();
    //iFrame.applyTransformation(parent);
    iFrame.applyTransformation(scene);

    if (drawAxes)
      //DrawingUtils.drawAxes(parent, radius()*1.3f);
      scene.drawAxes(radius() * 1.3f);
    if (iFrame.grabsInput(scene.mouseAgent())) {
      parent.fill(255, 0, 0);
      parent.sphere(radius() * 1.2f);
    } else {
      parent.fill(getColor());
      parent.sphere(radius());
    }
    parent.popMatrix();
  }

  public float radius() {
    return r;
  }

  public void setRadius(float myR) {
    r = myR;
    iFrame.setPrecisionThreshold(2 * r);
  }

  public int getColor() {
    return c;
  }

  public void setColor() {
    c = parent.color(parent.random(0, 255), parent.random(0, 255), parent.random(0, 255));
  }

  public void setColor(int myC) {
    c = myC;
  }

  public void setPosition(Vector pos) {
    iFrame.setPosition(pos);
  }

  public Vector getPosition() {
    return iFrame.position();
  }
}
