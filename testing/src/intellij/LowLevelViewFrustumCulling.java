package intellij;

import nub.core.Graph;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class LowLevelViewFrustumCulling extends PApplet {
  LowLevelOctreeNode root;
  Scene scene1, scene2, focus;
  PGraphics canvas1, canvas2;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;
  int w = 1200;
  int h = 800;

  public void settings() {
    size(w, h, renderer);
  }

  @Override
  public void setup() {
    // declare and build the octree hierarchy
    Vector p = new Vector(100, 70, 130);
    root = new LowLevelOctreeNode(p, Vector.multiply(p, -1.0f));
    root.buildBoxHierarchy(4);

    canvas1 = createGraphics(w, h / 2, P3D);
    scene1 = new Scene(this, canvas1);
    scene1.setType(Graph.Type.ORTHOGRAPHIC);
    scene1.enableBoundaryEquations();
    scene1.setRadius(150);
    scene1.fit(1);

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
    canvas1.background(255);
    root.drawIfAllChildrenAreVisible(scene1.context(), scene1);
    scene1.endDraw();
    scene1.display();

    scene2.beginDraw();
    canvas2.background(255);
    root.drawIfAllChildrenAreVisible(scene2.context(), scene1);
    scene2.context().pushStyle();
    scene2.context().strokeWeight(2);
    scene2.context().stroke(255, 0, 255);
    scene2.context().fill(255, 0, 255, 160);
    scene2.drawFrustum(scene1);
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
    if (key == '1')
      scene1.fitFOV();
    if (key == '2')
      scene2.fitFOV();
    if (key == 'p') {
      println(Vector.distance(scene1.eye().position(), scene1.anchor()));
    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.LowLevelViewFrustumCulling"});
  }
}
