package intellij;

import nub.core.Graph;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewingVolume extends PApplet {
  Scene scene1, scene2, focus;
  Vector point;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;
  int w = 1200;
  int h = 1600;

  public void settings() {
    size(w, h, renderer);
  }

  @Override
  public void setup() {
    scene1 = new Scene(this, P3D, w, h / 2);
    scene1.enableHint(Scene.BACKGROUND, color(125));
    scene1.enableHint(Scene.AXES | Scene.SHAPE);
    scene1.setShape(this::draw1);
    //scene1.togglePerspective();
    scene1.setRadius(150);
    scene1.fit();
    //scene1.eye().setMagnitude(1);

    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, P3D, w, h / 2);
    scene2.enableHint(Scene.FRUSTUM, scene1);
    scene2.enableHint(Scene.SHAPE);
    scene2.setShape(this::draw2);
    scene2.setType(Graph.Type.ORTHOGRAPHIC);
    scene2.setRadius(600);
    scene2.fit();

    point = new Vector(50,50,50);
  }

  @Override
  public void draw() {
    focus = scene1.hasMouseFocus() ? scene1 : scene2;
    scene1.display();
    scene2.display(0, h / 2);
  }

  public void draw1(PGraphics pg) {
    pg.noStroke();
    pg.fill(255, 255, 0, 125);
    pg.sphere(scene1.radius());
  }

  public void draw2(PGraphics canvas2) {
    canvas2.noStroke();
    canvas2.fill(255, 255, 0, 125);
    canvas2.sphere(scene1.radius());

    float zNear = scene1.zNear() / scene1.eye().magnitude();
    float zFar = scene1.zFar() / scene1.eye().magnitude();
    Vector v1 = scene1.eye().worldLocation(new Vector(0, 0, -zNear));
    Vector v2 = scene1.eye().worldLocation(new Vector(0, 0, -zFar));
    canvas2.pushStyle();
    canvas2.stroke(0, 255, 0);
    canvas2.strokeWeight(5);
    //pg.sphere(50);
    canvas2.line(v1.x(), v1.y(), v1.z(), v2.x(), v2.y(), v2.z());
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      //focus.zoom(mouseX - pmouseX);
      focus.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    //focus.scale(event.getCount() * 20);
    //println(event.getCount());
    //focus.moveForward(((float)event.getCount()) / 0.1f);
    focus.moveForward(event.getCount() / 50);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.alignTag();
  }

  public void keyPressed() {
    if (key == ' ')
      if (focus.type() == Graph.Type.PERSPECTIVE)
        focus.setType(Graph.Type.ORTHOGRAPHIC);
      else
        focus.setType(Graph.Type.PERSPECTIVE);
    if (key == 'f') {
      Scene.leftHanded = !Scene.leftHanded;
    }
    if (key == 's')
      scene1.fit(1);
    if (key == '1')
      scene1.fitFOV();
    if (key == '2')
      scene2.fitFOV();
    if (key == 'p') {
      Vector r = new Vector(random(0, width), random(0, height), random(0, 1));
      Vector d = scene1.displacement(r);
      Vector s = scene1.screenDisplacement(d);
      println("random vector : " + r.toString() + " displacement(r): " + d + " screenDisplacement(d): " + s);
      Vector n = new Vector(0, 0, scene1.zFar() - scene1.zNear());
      Vector o = scene1.screenDisplacement(n);
      println("vector n : " + n.toString() + " screenDisplacement(n): " + o);

      Vector t = new Vector(0, 0, -600);
      Vector u = scene1.screenDisplacement(t, scene1.eye());
      println("vector t : " + t.toString() + " screenDisplacement(t): " + u);
    }
    if(key == 'n') {
      Vector r = Vector.multiply(Vector.random(), scene1.radius());
      //Vector r = point;
      Vector s = scene1.screenLocation(r);
      Vector t = scene1.screenToNDCLocation(s);
      Vector u = scene1.ndcToScreenLocation(t);
      println("screenLocation: " + s.toString());
      println("screenToNDCLocation: " + t.toString());
      println("screenLocation: " + u.toString());

      Vector a = scene1.screenDisplacement(r);
      Vector b = scene1.screenToNDCDisplacement(a);
      Vector c = scene1.ndcToScreenDisplacement(b);
      println("screenDisplacement: " + a.toString());
      println("screenToNDCDisplacement: " + b.toString());
      println("screenDisplacement: " + c.toString());
    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ViewingVolume"});
  }
}
