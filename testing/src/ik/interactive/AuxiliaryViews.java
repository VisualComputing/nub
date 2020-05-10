package ik.interactive;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class AuxiliaryViews extends PApplet {

  Scene scene;
  Node[] shapes;
  Node light;
  int w = 1000;
  int h = 1000;
  List<AuxiliaryView> views;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(max(w, h));
    shapes = new Node[20];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Node(caja());
      shapes[i].setPickingThreshold(0);
      shapes[i].randomize(new Vector(), scene.radius(), scene.is3D());
    }
    light = new Node() {
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        Scene.drawAxes(pg, 150);
        pg.fill(isTagged(scene) ? 255 : 25, isTagged(scene) ? 0 : 255, 255);
        //Scene.drawEye(pg, views.get(0)._pGraphics, views.get(0)._type, this, views.get(0)._zNear, views.get(0)._zFar);
        pg.popStyle();
      }
    };
    scene.fit(1);
    //create an auxiliary view per Orthogonal Plane
    views = new ArrayList<AuxiliaryView>();
    //create an auxiliary view to look at the XY Plane
    Node eyeXY = new Node();
    eyeXY.setPosition(0, 0, scene.radius());
    views.add(new AuxiliaryView(scene, eyeXY, 0, 2 * h / 3, w / 3, h / 3));
    //create an auxiliary view to look at the XY Plane
    Node eyeXZ = new Node();
    eyeXZ.setPosition(0, scene.radius(), 0);
    eyeXZ.setOrientation(new Quaternion(new Vector(1, 0, 0), -HALF_PI));
    views.add(new AuxiliaryView(scene, eyeXZ, w / 3, 2 * h / 3, w / 3, h / 3));
    //create an auxiliary view to look at the XY Plane
    Node eyeYZ = new Node();
    eyeYZ.setPosition(scene.radius(), 0, 0);
    eyeYZ.setOrientation(new Quaternion(new Vector(0, 1, 0), HALF_PI));
    views.add(new AuxiliaryView(scene, eyeYZ, 2 * w / 3, 2 * h / 3, w / 3, h / 3));

  }

  public void draw() {
    background(90, 80, 125);
    scene.render();
    //Drawing back buffer
        /*
        scene.beginHUD();
        image(scene.backBuffer(), 0, 0);
        scene.endHUD();
        */

    for (AuxiliaryView view : views) {
      view.draw();
      view.display();
    }
  }

  public AuxiliaryView currentView(float x, float y) {
    for (AuxiliaryView view : views) {
      if (view.focus(x, y)) return view;
    }
    return null;
  }


  public Vector cursorLocation(float x, float y) {
    for (AuxiliaryView view : views) {
      //check bounds
      if (view.focus(x, y)) return view.cursorLocation(x, y);
    }
    return new Vector(x, y);
  }


  public void mouseMoved() {
    Vector point = cursorLocation(mouseX, mouseY);
    scene.tag((int) point.x(), (int) point.y());
  }

  public void mouseDragged() {
    AuxiliaryView current = currentView(mouseX, mouseY);
    Vector previous = current == null ? new Vector(pmouseX, pmouseY) : current.cursorLocation(pmouseX, pmouseY);
    Vector point = current == null ? new Vector(mouseX, mouseY) : current.cursorLocation(mouseX, mouseY);
    Node eye = scene.eye();
    Graph.Type type = scene.type();
    if (current != null) {
      scene.setEye(current.eye());
      scene.setType(current.type());
    }
    if (mouseButton == LEFT)
      scene.spin((int) previous.x(), (int) previous.y(), (int) point.x(), (int) point.y());
    else if (mouseButton == RIGHT)
      scene.translate(point.x() - previous.x(), point.y() - previous.y(), 0);
    else
      scene.moveForward(mouseX - pmouseX);
    if (current != null) {
      scene.setEye(eye);
      scene.setType(type);
    }
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == '3')
      scene.setFOV(PI / 3);
    if (key == '4')
      scene.setFOV(PI / 4);
    if (key == ' ')
      for (AuxiliaryView view : views)
        view.setEnabled(!view.enabled());
    if (key == 'o')
      for (AuxiliaryView view : views)
        view.setType(view.type() == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC);
    if (key == 't') {
      scene.setType(scene.type() == Graph.Type.ORTHOGRAPHIC ? Graph.Type.PERSPECTIVE : Graph.Type.ORTHOGRAPHIC);
    }
    if (key == 'p') {
      //scene.eye().position().print();
      //scene.eye().orientation().print();
    }
  }

  PShape caja() {
    PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
    caja.setStrokeWeight(3);
    caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
    caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
    return caja;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"ik.interactive.AuxiliaryViews"});
  }
}
