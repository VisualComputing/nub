package intellij;

import nub.core.Graph;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewingVolume extends PApplet {
  Scene scene1, scene2, focus;
  PGraphics canvas1, canvas2;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;
  int w = 1200;
  int h = 1600;

  public void settings() {
    size(w, h, renderer);
  }

  @Override
  public void setup() {
    canvas1 = createGraphics(w, h / 2, P3D);
    scene1 = new Scene(this, canvas1);
    //scene1.togglePerspective();
    scene1.setRadius(150);
    scene1.fit();
    //scene1.eye().setMagnitude(1);

    canvas2 = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, canvas2, 0, h / 2);
    scene2.setType(Graph.Type.ORTHOGRAPHIC);
    scene2.setRadius(600);
    scene2.fit();
  }

  @Override
  public void draw() {
    handleMouse();
    background(255);
    scene1.beginDraw();
    canvas1.background(125);
    //scene1.drawAxes(canvas1);
    canvas1.noStroke();
    canvas1.fill(255, 255, 0, 125);
    canvas1.sphere(scene1.radius());
    scene1.endDraw();
    scene1.display();

    scene2.beginDraw();
    canvas2.background(255);
    scene2.context().pushStyle();
    scene2.context().strokeWeight(2);
    scene2.context().stroke(255, 0, 255);
    scene2.context().fill(255, 0, 255, 160);
    scene2.drawFrustum(scene1);
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

    scene2.context().popStyle();
    scene2.endDraw();
    scene2.display();
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
    focus.moveForward(event.getCount() * 20);
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
      scene1.flip();
      scene2.flip();
    }
    if (key == 's')
      scene1.fit(1);
    if (key == '1')
      scene1.fitFOV();
    if (key == '2')
      scene2.fitFOV();
    if (key == 'p') {
      Vector r = new Vector(random(-50, 50), random(-50, 50), random(-50, 50));
      Vector d = scene1.displacement(r);
      Vector s = scene1.screenDisplacement(d);
      println("random vector : " + r.toString() + " displacement(r): " + d + " screenDisplacement(d): " + s);
    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.ViewingVolume"});
  }
}
