package basics;

import common.InteractiveNode;
import frames.core.Graph;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class Raster extends PApplet {
  Scene scene;
  int n = 4;
  boolean triangleHint = true;
  boolean gridHint = true;
  boolean debug = true;
  Vector v1, v2, v3;
  boolean yDirection;
  Frame frame;
  protected TimingTask spinningTask;

  public void settings() {
    size(1024, 1024, P3D);
  }

  public void spin() {
    scene.eye().rotate(new Quaternion(yDirection ? new Vector(0, 1, 0) : new Vector(1, 0, 0), PI / 100), scene.anchor());
  }

  public void setup() {
    scene = new Scene(this);
    if (scene.is3D())
      scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(width / 2);
    scene.fitBallInterpolation();

    spinningTask = new TimingTask() {
      public void execute() {
        spin();
      }
    };
    scene.registerTask(spinningTask);

    frame = new Frame();
    frame.setScaling(width / pow(2, n));

    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultGrabber(eye);
    randomizeTriangle();
  }

  public void draw() {
    background(0);
    stroke(0, 255, 0);
    if (gridHint)
      scene.drawGrid(scene.radius(), (int) pow(2, n));
    if (triangleHint)
      drawTriangleHint();
    pushMatrix();
    pushStyle();
    scene.applyTransformation(frame);
    triangleRaster();
    popStyle();
    popMatrix();
  }

  // coordinates are given in the frame coordinate
  // system which has a dimension of 2^n dimension
  public void triangleRaster() {
    // frame.coordinatesOf converts from world to frame
    // here we convert v1 to illustrate the idea
    if (debug) {
      pushStyle();
      stroke(255, 255, 0, 125);
      point(round(frame.coordinatesOf(v1).x()), round(frame.coordinatesOf(v1).y()));
      popStyle();
    }
  }

  void randomizeTriangle() {
    int low = -width / 2;
    int high = width / 2;
    v1 = new Vector(random(low, high), random(low, high));
    v2 = new Vector(random(low, high), random(low, high));
    v3 = new Vector(random(low, high), random(low, high));
  }

  void drawTriangleHint() {
    pushStyle();
    noFill();
    strokeWeight(2);
    stroke(255, 0, 0);
    triangle(v1.x(), v1.y(), v2.x(), v2.y(), v3.x(), v3.y());
    strokeWeight(5);
    stroke(0, 255, 255);
    point(v1.x(), v1.y());
    point(v2.x(), v2.y());
    point(v3.x(), v3.y());
    popStyle();
  }

  public void keyPressed() {
    if (key == 'g')
      gridHint = !gridHint;
    if (key == 't')
      triangleHint = !triangleHint;
    if (key == 'd')
      debug = !debug;
    if (key == '+') {
      n = n < 7 ? n + 1 : 2;
      frame.setScaling(1024 / pow(2, n));
    }
    if (key == '-') {
      n = n > 2 ? n - 1 : 7;
      frame.setScaling(1024 / pow(2, n));
    }
    if (key == 'r')
      randomizeTriangle();
    if (key == ' ')
      if (spinningTask.isActive())
        spinningTask.stop();
      else
        spinningTask.run(20);
    if (key == 'x')
      yDirection = false;
    if (key == 'y')
      yDirection = true;
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.Raster"});
  }
}
