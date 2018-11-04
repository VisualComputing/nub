package intellij;

import frames.core.Frame;
import frames.core.Graph;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

public class TwoScenes extends PApplet {
  Scene scene1, scene2, focus;
  PGraphics canvas1, canvas2;
  Frame frame;
  Vector vector;

  int w = 1200;
  int h = 1200;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    canvas1 = createGraphics(w, h / 2, P3D);
    scene1 = new Scene(this, canvas1);
    //scene1.setZClippingCoefficient(1);
    scene1.setRadius(200);
    //scene1.setType(Graph.Type.ORTHOGRAPHIC);
    scene1.setFieldOfView(PI / 3);
    //scene1.fitBallInterpolation();
    scene1.fitBall();

    // enable computation of the frustum planes equations (disabled by default)
    scene1.enableBoundaryEquations();

    canvas2 = createGraphics(w, h / 2, P3D);
    // Note that we pass the upper left corner coordinates where the scene1
    // is to be drawn (see drawing code below) to its constructor.
    scene2 = new Scene(this, canvas2, 0, h / 2);
    //scene2.setType(Graph.Type.ORTHOGRAPHIC);
    scene2.setRadius(400);
    //scene2.fitBallInterpolation();
    scene2.fitBall();
    frame = new Frame();
  }

  public void keyPressed() {
    if (key == 'f')
      scene1.fitBall();
    if (key == 'a') {
      println(scene1.zNear());
      vector = new Vector(0, 0, -scene1.zNear() / scene1.eye().magnitude());
      vector = scene1.eye().worldLocation(vector);
      frame.setPosition(vector);
    }
    if (key == 'b') {
      Vector zNear = new Vector(0, 0, scene1.zNear());
      Vector zFar = new Vector(0, 0, scene1.zFar());
      Vector zNear2ZFar = Vector.subtract(zFar, zNear);
      scene1.translate(0, 0, zNear2ZFar.magnitude(), frame);
    }
    if (key == 'n')
      scene1.eye().setMagnitude(1);
    if (key == 'm')
      scene1.setFieldOfView(PI / 3);
    if (key == 't') {
      if (scene1.type() == Graph.Type.PERSPECTIVE) {
        scene1.setType(Graph.Type.ORTHOGRAPHIC);
      } else {
        scene1.setType(Graph.Type.PERSPECTIVE);
      }
    }
    if (key == '+')
      scene1.eye().rotate(0, 1, 0, QUARTER_PI / 2);
    if (key == '-')
      scene1.eye().rotate(0, 1, 0, -QUARTER_PI / 2);
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      focus.spin();
    else if (mouseButton == RIGHT)
      focus.translate();
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
        focus.align();
  }

  void draw(PGraphics graphics) {
    graphics.background(0);
    graphics.noStroke();
    graphics.fill(0, 255, 0);
    graphics.pushMatrix();
    Scene.applyTransformation(graphics, frame);
    //scene1.applyTransformation(frame);
    graphics.sphere(50);
    graphics.popMatrix();
  }

  void handleMouse() {
    focus = mouseY < h / 2 ? scene1 : scene2;
  }

  public void draw() {
    handleMouse();
    scene1.beginDraw();
    canvas1.background(0);
    draw(canvas1);
    scene1.drawAxes();
    scene1.endDraw();
    scene1.display();

    scene2.beginDraw();
    canvas2.background(0);
    draw(canvas2);
    scene2.drawAxes();

    // draw with axes
    //eye
    canvas2.pushStyle();
    canvas2.stroke(255, 255, 0);
    canvas2.fill(255, 255, 0, 160);
    scene2.drawEye(scene1);
    canvas2.popStyle();
    //axes
    canvas2.pushMatrix();
    scene2.applyTransformation(scene1.eye());
    scene2.drawAxes(60);
    canvas2.popMatrix();

    scene2.endDraw();
    scene2.display();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"intellij.TwoScenes"});
  }
}
