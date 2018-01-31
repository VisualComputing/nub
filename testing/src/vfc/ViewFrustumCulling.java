package vfc;

import common.InteractiveShape;
import processing.core.PApplet;
import processing.core.PGraphics;
import proscene.core.Graph;
import proscene.primitives.Vector;
import proscene.processing.Scene;

public class ViewFrustumCulling extends PApplet {
  OctreeNode root;
  Scene scene, auxScene;
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
    InteractiveShape eye = new InteractiveShape(scene);
    scene.setEye(eye);
    scene.setDefaultNode(eye);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    auxCanvas = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas, 0, h / 2);
    auxScene.setType(Graph.Type.ORTHOGRAPHIC);
    InteractiveShape auxEye = new InteractiveShape(auxScene);
    auxScene.setEye(auxEye);
    auxScene.setDefaultNode(auxEye);
    auxScene.setRadius(200);
    scene.setFieldOfView(PI / 3);
    auxScene.fitBall();
  }

  @Override
  public void draw() {
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

  public void keyPressed() {
    if (key == 'i') {
      println(scene._lastDisplay + " and " + auxScene._lastDisplay);
    }
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"vfc.ViewFrustumCulling"});
  }
}
