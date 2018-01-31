package eye;

import common.InteractiveShape;
import processing.core.PApplet;
import processing.core.PGraphics;
import remixlab.primitives.Vector;
import remixlab.processing.Scene;

public class WindowCulling extends PApplet {
  Scene scene, auxScene;
  PGraphics canvas, auxCanvas;
  float circleRadius = 150;

  int w = 1110;
  int h = 1110;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P2D;

  public void settings() {
    size(w, h, renderer);
  }

  public void info() {
    println(scene.radius());
    scene.center().print();
    scene.eye().position().print();
    println(scene.zNear());
    println(scene.zFar());
    scene.matrixHandler().projection().print();
    scene.matrixHandler().view().print();
    scene.matrixHandler().modelView().print();
  }

  public void setup() {
    canvas = createGraphics(w, h / 2, renderer);
    scene = new Scene(this, canvas);

    InteractiveShape eye = new InteractiveShape(scene);
    scene.setEye(eye);
    scene.setDefaultNode(eye);
    scene.setRadius(200);
    scene.fitBall();

    // enable computation of the boundary hyper-planes equations (disabled by default)
    scene.enableBoundaryEquations();

    auxCanvas = createGraphics(w, h / 2, renderer);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas, 0, h / 2);

    InteractiveShape auxEye = new InteractiveShape(auxScene);
    auxScene.setEye(auxEye);
    auxScene.setDefaultNode(auxEye);
    auxScene.setRadius(400);
    auxScene.fitBall();
  }

  void mainDrawing(Scene s) {
    PGraphics p = s.frontBuffer();
    p.background(0);
    p.noStroke();
    p.ellipseMode(RADIUS);
    // the main viewer camera is used to cull the sphere object against its frustum
    switch (scene.ballVisibility(new Vector(0, 0), circleRadius)) {
      case VISIBLE:
        p.fill(0, 255, 0);
        p.ellipse(0, 0, circleRadius, circleRadius);
        break;
      case SEMIVISIBLE:
        p.fill(255, 0, 0);
        p.ellipse(0, 0, circleRadius, circleRadius);
        break;
      case INVISIBLE:
        break;
    }
  }

  void auxiliarDrawing(Scene s) {
    mainDrawing(s);
    s.frontBuffer().pushStyle();
    s.frontBuffer().stroke(255, 255, 0);
    s.frontBuffer().fill(255, 255, 0, 160);
    s.drawEye(scene);
    s.frontBuffer().popStyle();
  }

  public void draw() {
    scene.beginDraw();
    mainDrawing(scene);
    scene.endDraw();
    scene.display();

    auxScene.beginDraw();
    auxiliarDrawing(auxScene);
    auxScene.endDraw();
    auxScene.display();
  }

  public void keyPressed() {
    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'f')
      scene.fitBall();
    if (key == 'i')
      info();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"eye.WindowCulling"});
  }
}
