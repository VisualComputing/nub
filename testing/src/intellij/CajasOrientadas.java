package intellij;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class CajasOrientadas extends PApplet {
  Scene scene;
  Box[] cajas;
  boolean drawAxes = true, drawShooterTarget = true, adaptive = true;
  Sphere esfera;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(200);
    scene.togglePerspective();
    scene.fit();
    esfera = new Sphere();
    esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
    esfera.setColor(color(0, 0, 255));

    cajas = new Box[30];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box();

    scene.fit(1);
    scene.setTrackedFrame("keyboard", esfera.iFrame);
  }

  public void draw() {
    background(0);
    // calls visit() on all scene attached frames
    // automatically applying all the frame transformations
    //scene.traverse();
    scene.render();
  }

  public void mouseMoved() {
    scene.cast();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ') {
      adaptive = !adaptive;
      for (Box caja : cajas)
        if (adaptive)
          caja.iFrame.setPickingThreshold(0.25f);
        else
          caja.iFrame.setPickingThreshold(25);
    }
    if (key == 'a')
      drawAxes = !drawAxes;
    if (key == 'p')
      drawShooterTarget = !drawShooterTarget;
    if (key == 'e')
      scene.togglePerspective();
    if (key == 's')
      scene.fit(1);
    if (key == 'S')
      scene.fit();
    if (key == 'u')
      if (scene.trackedFrame("keyboard") == null)
        scene.setTrackedFrame("keyboard", esfera.iFrame);
      else
        scene.resetTrackedFrame("keyboard");
    if (key == CODED)
      if (keyCode == UP)
        scene.translate("keyboard", 0, -10);
      else if (keyCode == DOWN)
        scene.translate("keyboard", 0, 10);
      else if (keyCode == LEFT)
        scene.translate("keyboard", -10, 0);
      else if (keyCode == RIGHT)
        scene.translate("keyboard", 10, 0);
  }

  public class Box {
    public Frame iFrame;
    float w, h, d;
    int c;

    public Box() {
      iFrame = new Frame(scene) {
        // note that within visit() geometry is defined
        // at the frame local coordinate system
        @Override
        public boolean graphics(PGraphics pg) {
          pg.pushStyle();
          setOrientatio(CajasOrientadas.this.esfera.getPosition());
          if (drawAxes)
            scene.drawAxes(pg, PApplet.max(w, h, d) * 1.3f);
          pg.noStroke();
          if (isTracked())
            pg.fill(255, 0, 0);
          else
            pg.fill(getColor());
          pg.box(w, h, d);
          pg.stroke(255);
          if (drawShooterTarget)
            scene.drawShooterTarget(iFrame);
          pg.popStyle();
          return true;
        }
      };
      iFrame.setPickingThreshold(0);
      setSize();
      setColor();
      iFrame.randomize();
    }

    public void setSize() {
      w = random(10, 40);
      h = random(10, 40);
      d = random(10, 40);
      //iFrame.setPickingThreshold(PApplet.max(w, h, d));
    }

    public void setSize(float myW, float myH, float myD) {
      w = myW;
      h = myH;
      d = myD;
    }

    public int getColor() {
      return c;
    }

    public void setColor() {
      c = color(random(0, 255), random(0, 255), random(0, 255));
    }

    public void setColor(int myC) {
      c = myC;
    }

    public Vector getPosition() {
      return iFrame.position();
    }

    public void setPosition(Vector pos) {
      iFrame.setPosition(pos);
    }

    public Quaternion getOrientation() {
      return iFrame.orientation();
    }

    public void setOrientatio(Vector v) {
      Vector to = Vector.subtract(v, iFrame.position());
      iFrame.setOrientation(new Quaternion(new Vector(0, 1, 0), to));
    }
  }

  class Sphere {
    Frame iFrame;
    float r;
    int c;

    public Sphere() {
      iFrame = new Frame(scene) {
        // note that within visit() geometry is defined
        // at the frame local coordinate system
        @Override
        public boolean graphics(PGraphics pg) {
          pg.pushStyle();
          if (drawAxes)
            //DrawingUtils.drawAxes(parent, radius()*1.3f);
            Scene.drawAxes(pg, radius() * 1.3f);
          pg.noStroke();
          if (iFrame.isTracked()) {
            pg.fill(255, 0, 0);
            pg.sphere(radius() * 1.2f);
          } else {
            pg.fill(getColor());
            pg.sphere(radius());
          }
          pg.stroke(255);
          if (drawShooterTarget)
            scene.drawShooterTarget(iFrame);
          pg.popStyle();
          return true;
        }
      };
      iFrame.setPickingThreshold(0.15f);
      setRadius(10);
    }

    public float radius() {
      return r;
    }

    public void setRadius(float myR) {
      r = myR;
      iFrame.setPickingThreshold(2 * r);
    }

    public int getColor() {
      return c;
    }

    public void setColor() {
      c = color(random(0, 255), random(0, 255), random(0, 255));
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

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.CajasOrientadas"});
  }
}
