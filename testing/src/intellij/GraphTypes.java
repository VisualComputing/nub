package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class GraphTypes extends PApplet {
  StdCamera scene;
  Scene auxScene, focus;
  Node boxNode;
  PGraphics canvas, auxCanvas;
  boolean box, sphere;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    canvas = createGraphics(w, h / 2, P3D);
    scene = new StdCamera(this, canvas);
    //scene1.setZClippingCoefficient(1);
    scene.setRadius(200);
    scene.setType(Graph.Type.ORTHOGRAPHIC);
    //scene1.fit(1);
    scene.fit();

    // enable computation of the frustum planes equations (disabled by default)
    scene.enableBoundaryEquations();

    auxCanvas = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas);
    //scene2.setType(Graph.Type.ORTHOGRAPHIC);
    auxScene.setRadius(400);
    //scene2.fit(1);
    auxScene.fit();
    boxNode = new Node();
    boxNode.rotate(new Quaternion(new Vector(0, 1, 0), QUARTER_PI));
  }

  public void keyPressed() {
    if (key == ' ')
      scene.toggleMode();
    if (key == 'f')
      scene.fit();
    if (key == 'g')
      scene.fitFOV();
    if (key == 'b')
      box = !box;
    if (key == 's')
      sphere = !sphere;
    if (key == 'a') {
      Vector zNear = new Vector(0, 0, scene.zNear());
      Vector zFar = new Vector(0, 0, scene.zFar());
      Vector zNear2ZFar = Vector.subtract(zFar, zNear);
      Vector zNear2ZFarEye = scene.eye().displacement(zNear2ZFar);
      println("zNear2ZFar: " + zNear2ZFar.magnitude());
      println("zNear2ZFarEye: " + zNear2ZFarEye.magnitude());
      println("2*radius*sqrt(3): " + 2 * scene.radius() * sqrt(3));
      println(version1() + " " + version2() + " eye magnitude: " + scene.eye().magnitude());
      println((scene.type() == Graph.Type.ORTHOGRAPHIC ? "ORTHO" : "PERSP") + " zNear: " + scene.zNear() + " zFar: " + scene.zFar());
      print(scene.eye().position().toString());
    }
    if (key == 'n')
      scene.setFOV(1);
    if (key == 'm')
      scene.setFOV(PI / 3);
    if (key == 't') {
      if (scene.type() == Graph.Type.PERSPECTIVE) {
        scene.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        scene.setType(Graph.Type.PERSPECTIVE);
      }
      //scene1.fit(1);
      //scene1.fit();
    }
    if (key == 'e')
      if (auxScene.node() == boxNode)
        auxScene.removeTag();
      else
        auxScene.tag(boxNode);
    if (key == '+')
      scene.eye().rotate(0, 1, 0, QUARTER_PI / 2);
    if (key == '-')
      scene.eye().rotate(0, 1, 0, -QUARTER_PI / 2);
  }

  public String version1() {
    float z = Vector.scalarProjection(Vector.subtract(scene.eye().position(), scene.center()), scene.eye().zAxis()) - scene.zClippingCoefficient() * scene.radius();
    // Prevents negative or null zNear values.
    float zMin = scene.zNearCoefficient() * scene.zClippingCoefficient() * scene.radius();
    return ("nodes z: " + z + " nodes zMin: " + zMin);
  }

  public String version2() {
    /*
    float zNearScene = zClippingCoefficient() * sceneRadius();
    float z = distanceToSceneCenter() - zNearScene;
    // Prevents negative or null zNear values.
    float zMin = zNearCoefficient() * zNearScene;
    */

    float zNearScene = scene.zClippingCoefficient() * scene.radius();
    float z = distanceToSceneCenter() - zNearScene;
    float zMin = scene.zNearCoefficient() * zNearScene;
    return ("Viewer z: " + z + " Viewer zMin: " + zMin);
  }

  float distanceToSceneCenter() {
    return Math.abs((scene.eye().location(scene.center())).z());
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.mouseSpin();
    else if (mouseButton == RIGHT)
      focus.mouseTranslate();
    else
      focus.moveForward(mouseX - pmouseX);
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
        focus.alignTag();
  }

  void draw(PGraphics graphics) {
    graphics.background(0);
    graphics.noStroke();
    // the main viewer camera is used to cull the sphere object against its frustum
    switch (scene.ballVisibility(new Vector(0, 0, 0), scene.radius() * 0.6f)) {
      case VISIBLE:
        graphics.fill(0, 255, 0);
        graphics.sphere(scene.radius() * 0.6f);
        break;
      case SEMIVISIBLE:
        graphics.fill(255, 0, 0);
        graphics.sphere(scene.radius() * 0.6f);
        break;
      case INVISIBLE:
        break;
    }
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene : auxScene;
  }

  public void draw() {
    handleMouse();
    scene.beginDraw();
    canvas.background(0);
    //draw(canvas1);
    scene.drawAxes();

    scene.endDraw();
    scene.display();

    auxScene.beginDraw();
    auxCanvas.background(0);
    //draw(canvas2);
    auxScene.drawAxes();

    if (sphere) {
      auxCanvas.pushStyle();
      auxCanvas.fill(255, 0, 255, 160);
      auxCanvas.sphere(scene.radius());
      auxCanvas.popStyle();
    }

    if (box) {
      auxCanvas.pushStyle();
      auxCanvas.pushMatrix();
      auxScene.applyTransformation(boxNode);
      auxCanvas.fill(0, 255, 0, 160);
      auxCanvas.box(2 * scene.radius());
      auxCanvas.popMatrix();
      auxCanvas.popStyle();
    }

    // draw with axes
    //eye
    auxCanvas.pushStyle();
    auxCanvas.stroke(255, 255, 0);
    auxCanvas.fill(255, 255, 0, 160);
    auxScene.drawFrustum(scene);
    auxCanvas.popStyle();
    //axes
    auxCanvas.pushMatrix();
    auxScene.applyTransformation(scene.eye());
    auxScene.drawAxes(60);
    auxCanvas.popMatrix();

    auxScene.endDraw();
    auxScene.display(0, h / 2);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.GraphTypes"});
  }

  public class StdCamera extends Scene {
    boolean standard;

    public StdCamera(PApplet applet) {
      super(applet);
      standard = false;
    }

    public StdCamera(PApplet applet, PGraphics pg) {
      super(applet, pg);
      standard = false;
    }

    public void toggleMode() {
      standard = !standard;
    }

    public boolean isStandard() {
      return standard;
    }

    @Override
    public float zNear() {
      if (standard)
        return 0.001f;
      else
        return super.zNear();
    }

    @Override
    public float zFar() {
      if (standard)
        return 1000.0f;
      else
        return super.zFar();
    }
  }
}
