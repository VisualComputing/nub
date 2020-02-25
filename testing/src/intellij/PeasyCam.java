package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class PeasyCam extends PApplet {
  Scene scene;
  Vector axis;
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
      @Override
      public void interact(Object... gesture) {
        if (gesture.length == 1) {
          if (gesture[0] instanceof Float) {
            /*
            // Orbit
            //orbit(axis, (float)gesture[0], new Vector(0,0,0));
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            Quaternion q = new Quaternion(displacement(axis), (float) gesture[0]);
            orbit(q, scene.center(), 0.2f);
            Vector e =  q.eulerAngles();
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            _orbitTask._x += e.x();
            _orbitTask._y += e.y();
            _orbitTask._z += e.z();
            if (!_orbitTask.isActive())
              _orbitTask.run();
            // */
            // /*
            // Scaling
            float factor = 1 + (Math.abs((float) gesture[0]) / graph().height());
            //scale(delta >= 0 ? factor : 1 / factor);
            scale((float) gesture[0] >= 0 ? factor : 1 / factor, 0.2f);

            //float dx = Math.abs((float)gesture[0]);
            //float factor = 1 + dx / graph().height();
            //scale(dx >= 0 ? factor : 1 / factor);
            //scale(dx >= 0 ? factor : 1 / factor, 0.2f);
            //_scalingTask._x += (float)gesture[0];
            //if (!_scalingTask.isActive())
            //  _scalingTask.run();
            // */
            /*
            // Rotation
            Quaternion q = new Quaternion(displacement(axis), (float) gesture[0]);
            Vector e = q.eulerAngles();
            //orbit(new Quaternion(displacement(axis), (float)gesture[0]), new Vector(0,0,0));
            _rotationTask._x += (float) gesture[0];
            //_rotationTask._y += e.y();
            //_rotationTask._z += e.z();
            if (!_rotationTask.isActive())
              _rotationTask.run();
            // */
            /*
            Quaternion q = new Quaternion((float) gesture[0], 0, 0);
            rotate(q, 0f);
            // */
          }
        }
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
    axis = Vector.random();
    axis.multiply(scene.radius() / 3);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.drawArrow(axis);
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
          scene.mouseDampedSpinEye2();
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
        // Scaling
        //println(mouseX - pmouseX);
        scene.interactTag((float) mouseX - pmouseX);
        /*
        if (cad)
          scene.mouseDampedRotateCAD();
        else
          scene.mouseDampedLookAround();
        // */
        break;
    }
  }

  public void mouseWheel(MouseEvent event) {
    if (!scene.interactTag((float)event.getCount() * 10.f * PI / (float)width))
      scene.dampedMoveForward(event.getCount() * 20);
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
