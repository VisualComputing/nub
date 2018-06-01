package basics;

import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class StandardCamera extends PApplet {
  StdCamera scene;
  Scene auxScene;
  PGraphics canvas, auxCanvas;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    canvas = createGraphics(w, h / 2, P3D);
    scene = new StdCamera(this, canvas);
    scene.setFieldOfView(PI / 3);
    scene.setRadius(200);
    scene.fitBallInterpolation();

    // enable computation of the frustum planes equations (disabled by default)
    scene.enableBoundaryEquations();

    auxCanvas = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene
    // is to be drawn (see drawing code below) to its constructor.
    auxScene = new Scene(this, auxCanvas, 0, h / 2);
    auxScene.setRadius(400);
    auxScene.fitBallInterpolation();
  }

  public void keyPressed() {
    if (key == ' ')
      scene.toggleMode();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.zoom(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
    //scene.zoom(event.getCount() * 50);
  }

  public void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
      if (event.getButton() == LEFT)
        scene.focus();
      else
        scene.align();
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

  public void draw() {
    scene.beginDraw();
    draw(canvas);
    scene.endDraw();
    scene.display();

    auxScene.beginDraw();
    draw(auxCanvas);
    auxCanvas.pushStyle();
    auxCanvas.stroke(255, 255, 0);
    auxCanvas.fill(255, 255, 0, 160);
    auxScene.drawEye(scene);
    auxCanvas.popStyle();
    auxScene.endDraw();
    auxScene.display();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.StandardCamera"});
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