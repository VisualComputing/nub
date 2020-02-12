package intellij;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class CajasOrientadas extends PApplet {
  Scene scene;
  Box[] cajas;
  boolean drawAxes = true, bullsEye = true;
  Sphere esfera;

  public void settings() {
    size(800, 800, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(200);
    //scene.togglePerspective();
    scene.fit();
    esfera = new Sphere();
    esfera.setPosition(new Vector(0.0f, 1.4f, 0.0f));
    esfera.setColor(color(0, 0, 255));

    cajas = new Box[30];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box();

    scene.fit(1);
    scene.tag("keyboard", esfera.iNode);
  }

  public void draw() {
    background(0);
    // calls visit() on all scene attached nodes
    // automatically applying all the node transformations
    //scene.traverse();
    scene.render();
  }

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT) {
      if (!scene.mouseSpinTag())
        scene.mouseDampedSpinEye();
    } else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ')
      for (Box caja : cajas)
        if (caja.iNode.pickingThreshold() != 0)
          if (abs(caja.iNode.pickingThreshold()) < 1)
            caja.iNode.setPickingThreshold(100 * caja.iNode.pickingThreshold());
          else
            caja.iNode.setPickingThreshold(caja.iNode.pickingThreshold() / 100);
    if (key == 'a')
      drawAxes = !drawAxes;
    if (key == 'p')
      bullsEye = !bullsEye;
    if (key == 'e')
      scene.togglePerspective();
    if (key == 's')
      scene.fit(1);
    if (key == 'S')
      scene.fit();
    if (key == 'u')
      if (scene.node("keyboard") == null)
        scene.tag("keyboard", esfera.iNode);
      else
        scene.removeTag("keyboard");
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
    public Node iNode;
    float w, h, d;
    int c;

    public Box() {
      iNode = new Node(scene) {
        // note that within visit() geometry is defined
        // at the node local coordinate system
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          Box.this.setOrientation(CajasOrientadas.this.esfera.getPosition());
          if (drawAxes)
            Scene.drawAxes(pg, PApplet.max(w, h, d) * 1.3f);
          pg.noStroke();
          if (isTagged(scene))
            pg.fill(255, 0, 0);
          else
            pg.fill(getColor());
          pg.box(w, h, d);
          pg.stroke(0, 225, 0);
          if (bullsEye)
            scene.drawBullsEye(iNode);
          pg.popStyle();
        }
      };
      iNode.setPickingThreshold(-25);
      setSize();
      setColor();
      iNode.randomize();
    }

    public void setSize() {
      w = random(10, 40);
      h = random(10, 40);
      d = random(10, 40);
      //iNode.setPickingThreshold(PApplet.max(w, h, d));
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
      return iNode.position();
    }

    public void setPosition(Vector pos) {
      iNode.setPosition(pos);
    }

    public Quaternion getOrientation() {
      return iNode.orientation();
    }

    public void setOrientation(Vector v) {
      Vector to = Vector.subtract(v, iNode.position());
      iNode.setOrientation(new Quaternion(new Vector(0, 1, 0), to));
    }
  }

  class Sphere {
    Node iNode;
    float r;
    int c;

    public Sphere() {
      iNode = new Node(scene) {
        // note that within visit() geometry is defined
        // at the node local coordinate system
        @Override
        public void graphics(PGraphics pg) {
          pg.pushStyle();
          if (drawAxes)
            //DrawingUtils.drawAxes(parent, radius()*1.3f);
            Scene.drawAxes(pg, radius() * 1.3f);
          pg.noStroke();
          if (iNode.isTagged(scene)) {
            pg.fill(255, 0, 0);
            pg.sphere(radius() * 1.2f);
          } else {
            pg.fill(getColor());
            pg.sphere(radius());
          }
          pg.stroke(255);
          if (bullsEye)
            scene.drawSquaredBullsEye(iNode);
          pg.popStyle();
        }
      };
      //iNode.setPickingThreshold(0.15f);
      iNode.setPickingThreshold(0);
      setRadius(10);
    }

    public float radius() {
      return r;
    }

    public void setRadius(float myR) {
      r = myR;
      iNode.setPickingThreshold(2 * r);
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
      iNode.setPosition(pos);
    }

    public Vector getPosition() {
      return iNode.position();
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.CajasOrientadas"});
  }
}
