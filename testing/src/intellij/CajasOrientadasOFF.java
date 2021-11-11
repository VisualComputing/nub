package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class CajasOrientadasOFF extends PApplet {
  Scene scene1, scene2, focus;
  Box[] cajas;
  Sphere esfera;
  boolean circle;

  public void settings() {
    size(700, 1400, P3D);
  }

  public void setup() {
    // Set the inertia for all interactivity methods to 0.85. Default is 0.8.
    scene1 = new Scene(createGraphics(width, height/2, P3D), 200);
    scene1.togglePerspective();
    scene1.fit();
    scene1.eye().enableHint(Node.BOUNDS);
    esfera = new Sphere(color(random(0, 255), random(0, 255), random(0, 255)), 10);
    esfera.setWorldPosition(new Vector(0, 1.4f, 0));
    cajas = new Box[15];
    for (int i = 0; i < cajas.length; i++)
      cajas[i] = new Box(color(random(0, 255), random(0, 255), random(0, 255)),
          random(10, 40), random(10, 40), random(10, 40));
    scene1.tag("keyboard", esfera);
    scene2 = new Scene(createGraphics(width, height/2, P3D), 200);
  }

  public void draw() {
    focus = scene1.hasFocus() ? scene1 : scene2;
    scene1.display(color(0));
    scene2.display(color(125), 0, height/2);
  }

  public void mouseClicked() {
    focus.focus();
  }

  public void mouseMoved() {
    focus.tag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.shift();
    else
      focus.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    focus.moveForward(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'z') {
      scene1.eye().setForbidScalingFilter();
    }
    if (key == ' ') {
      for (Box caja : cajas) {
        caja.toggleHint(Node.BULLSEYE);
        caja.togglePicking(Node.SHAPE);
      }
    }
    if (key == 'f') {
      scene1.fitFOV(1000);
    }
    if (key == 'c') {
      for (Box caja : cajas)
        if (caja.bullsEyeSize() < 1)
          caja.setBullsEyeSize(caja.bullsEyeSize() * 200);
        else
          caja.setBullsEyeSize(caja.bullsEyeSize() / 200);
    }
    if (key == 'd') {
      circle = !circle;
      for (Box caja : cajas)
        caja.configHint(Node.BULLSEYE, circle ?  Node.BullsEyeShape.CIRCLE : Node.BullsEyeShape.SQUARE);
    }
    if (key == 'a')
      for (Box caja : cajas)
        caja.toggleHint(Node.AXES);
    if (key == 'p')
      for (Box caja : cajas)
        caja.toggleHint(Node.BULLSEYE);
    if (key == 'e')
      scene1.togglePerspective();
    if (key == 's')
      scene1.fit(1000);
    if (key == 'S')
      scene1.fit();
    if (key == 'u')
      if (scene1.isTagValid("keyboard"))
        scene1.removeTag("keyboard");
      else
        scene1.tag("keyboard", esfera);
    if (key == CODED)
      if (keyCode == UP)
        scene1.shift("keyboard", 0, -10, 0);
      else if (keyCode == DOWN)
        scene1.shift("keyboard", 0, 10, 0);
      else if (keyCode == LEFT)
        scene1.shift("keyboard", -10, 0, 0);
      else if (keyCode == RIGHT)
        scene1.shift("keyboard", 10, 0, 0);
  }

  public class Box extends Node {
    float _w, _h, _d;
    int _color;

    public Box(int tint, float w, float h, float d) {
      setShape(this::caja);
      setBehavior(scene1, this::behavior);
      _color = tint;
      _w = w;
      _h = h;
      _d = d;
      setBullsEyeSize(max(_w, _h, _d) / scene1.radius());
      setForbidScalingFilter();
      scene1.randomize(this);
      enableHint(Node.AXES);
    }

    // geometry is defined at the node local coordinate system
    public void caja(PGraphics pg) {
      pg.pushStyle();
      pg.noStroke();
      pg.fill(_color);
      pg.box(_w, _h, _d);
      pg.popStyle();
    }

    public void behavior(Graph graph) {
      Vector to = Vector.subtract(esfera.worldPosition(), worldPosition());
      setWorldOrientation(Quaternion.from(Vector.plusJ, to));
    }
  }

  class Sphere extends Node {
    float _radius;
    int _color;

    public Sphere(int tint, float radius) {
      _color = tint;
      _radius = radius;
      setShape((PGraphics pg) -> {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(_color);
        pg.sphere(_radius);
        pg.popStyle();
      });
    }
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.CajasOrientadasOFF"});
  }
}
