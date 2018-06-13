package basics;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class GraphTypes extends PApplet {
  StdCamera scene;
  Scene auxScene, focus;
  Frame boxFrame;
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
    //scene.setZClippingCoefficient(1);
    scene.setRadius(200);
    //scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setFieldOfView(PI / 3);
    scene.fitBallInterpolation();

    // enable computation of the frustum planes equations (disabled by default)
    scene.enableBoundaryEquations();

    auxCanvas = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas, 0, h / 2);
    auxScene.setType(Graph.Type.ORTHOGRAPHIC);
    auxScene.setRadius(400);
    auxScene.fitBallInterpolation();
    boxFrame = new Frame(auxScene);
    boxFrame.rotate(new Quaternion(new Vector(0, 1, 0), QUARTER_PI));
  }

  public void keyPressed() {
    if (key == ' ')
      scene.toggleMode();
    if (key == 'f')
      scene.fitFieldOfView();
    if (key == 'b')
      box = !box;
    if (key == 's')
      sphere = !sphere;
    if (key == 'a') {
      Vector from = new Vector(0, 0, scene.zNear());
      Vector to = new Vector(0, 0, scene.zFar());
      Vector fromto = Vector.subtract(to, from);
      Vector fromtoeye = scene.eye().displacement(fromto);
      println("fromto: " + fromto.magnitude());
      println("fromtoeye: " + fromtoeye.magnitude());
      println("2*radius*sqrt(3): " + 2 * scene.radius() * sqrt(3));
    }
    if (key == 't') {
      if (scene.type() == Graph.Type.PERSPECTIVE) {
        scene.eye().setMagnitude(1);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        //scene.setFieldOfView(PI / 3);
        scene.setType(Graph.Type.PERSPECTIVE);
      }
      scene.fitBallInterpolation();
    }
    if (key == 'e')
      if (auxScene.trackedFrame() == boxFrame)
        auxScene.resetTrackedFrame();
      else
        auxScene.setTrackedFrame(boxFrame);
    if (key == '+')
      scene.eye().rotate(0, 1, 0, QUARTER_PI / 2);
    if (key == '-')
      scene.eye().rotate(0, 1, 0, -QUARTER_PI / 2);
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
    //draw(canvas);
    scene.drawAxes();

    scene.endDraw();
    scene.display();

    auxScene.beginDraw();
    //draw(auxCanvas);
    auxCanvas.background(0);
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
      auxScene.applyTransformation(boxFrame);
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
    auxScene.drawEye(scene);
    auxCanvas.popStyle();
    //axes
    auxCanvas.pushMatrix();
    auxScene.applyTransformation(scene.eye());
    auxScene.drawAxes(60);
    auxCanvas.popMatrix();

    auxScene.endDraw();
    auxScene.display();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.GraphTypes"});
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

    @Override
    protected float _rescalingFactor() {
      if (isStandard())
        return 1.0f;
      return super._rescalingFactor();
    }
  }
}
