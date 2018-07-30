package basics;

import frames.core.Graph;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class ViewFrustumCulling extends PApplet {
  OctreeNode root;
  Scene scene, auxScene, focus;
  PGraphics canvas, auxCanvas;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
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

    canvas = createGraphics(w, h / 2, P3D);
    scene = new Scene(this, canvas);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.enableBoundaryEquations();
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    auxCanvas = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas, 0, h / 2);
    auxScene.setType(Graph.Type.ORTHOGRAPHIC);
    auxScene.setRadius(200);
    scene.setFieldOfView(PI / 3);
    auxScene.fitBall();
  }

  @Override
  public void draw() {
    handleMouse();
    background(0);
    scene.beginDraw();
    canvas.background(0);
    root.drawIfAllChildrenAreVisible(scene.frontBuffer(), scene);
    scene.endDraw();
    scene.display();

    auxScene.beginDraw();
    auxCanvas.background(0);
    root.drawIfAllChildrenAreVisible(auxScene.frontBuffer(), scene);
    auxScene.frontBuffer().pushStyle();
    auxScene.frontBuffer().stroke(255, 255, 0);
    auxScene.frontBuffer().fill(255, 255, 0, 160);
    auxScene.drawEye(scene);
    auxScene.frontBuffer().popStyle();
    auxScene.endDraw();
    auxScene.display();
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
    focus = mouseY < h / 2 ? scene : auxScene;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.ViewFrustumCulling"});
  }
}
