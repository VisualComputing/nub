package basics;

import frames.core.Graph;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewFrustumCulling extends PApplet {
  OctreeNode root;
  Scene scene1, scene2, focus;
  PGraphics canvas1, canvas2;

  //Choose one of P3D for a 3D scene1, or P2D or JAVA2D for a 2D scene1
  String renderer = P3D;
  int w = 1110;
  int h = 1110;

  public void settings() {
    size(w, h, renderer);
  }

  @Override
  public void setup() {
    // declare and build the octree hierarchy
    Vector p = new Vector(100, 70, 130);
    root = new OctreeNode(p, Vector.multiply(p, -1.0f));
    root.buildBoxHierarchy(4);

    canvas1 = createGraphics(w, h / 2, P3D);
    scene1 = new Scene(this, canvas1);
    scene1.setType(Graph.Type.ORTHOGRAPHIC);
    scene1.enableBoundaryEquations();
    scene1.setRadius(100);
    scene1.setFieldOfView(PI / 3);
    scene1.fitBallInterpolation();

    canvas2 = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, canvas2, 0, h / 2);
    scene2.setType(Graph.Type.ORTHOGRAPHIC);
    scene2.setRadius(200);
    scene1.setFieldOfView(PI / 3);
    scene2.fitBall();
  }

  @Override
  public void draw() {
    handleMouse();
    background(0);
    scene1.beginDraw();
    canvas1.background(0);
    root.drawIfAllChildrenAreVisible(scene1.frontBuffer(), scene1);
    scene1.endDraw();
    scene1.display();

    scene2.beginDraw();
    canvas2.background(0);
    root.drawIfAllChildrenAreVisible(scene2.frontBuffer(), scene1);
    scene2.frontBuffer().pushStyle();
    scene2.frontBuffer().stroke(255, 255, 0);
    scene2.frontBuffer().fill(255, 255, 0, 160);
    scene2.drawEye(scene1);
    scene2.frontBuffer().popStyle();
    scene2.endDraw();
    scene2.display();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.translate();
    else
      focus.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    focus.scale(event.getCount() * 20);
    //focus.zoom(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        focus.focus();
      else
        focus.align();
  }

  public void keyPressed() {
    if (key == ' ')
      if (focus.type() == Graph.Type.PERSPECTIVE)
        focus.setType(Graph.Type.ORTHOGRAPHIC);
      else
        focus.setType(Graph.Type.PERSPECTIVE);
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.ViewFrustumCulling"});
  }
}
