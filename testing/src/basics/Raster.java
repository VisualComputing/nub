package basics;

import common.InteractiveNode;
import frames.core.Node;
import frames.input.Event;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Mouse;
import frames.processing.Scene;
import frames.processing.Shape;
import frames.timing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Created by pierre on 11/15/16.
 */
public class Raster extends PApplet {
  Scene scene;
  boolean yDirection;
  Frame frame;
  //Node eye, node;
  float radius = 100;
  protected TimingTask spinningTask;

  public void settings() {
    size(800, 800, P3D);
  }

  public void spin() {
    //scene.eye().rotateAroundPoint(new Quaternion(yDirection ? new Vector(0, 1, 0) : new Vector(1, 0, 0), PI / 100), scene.anchor());
  }

  public void setup() {
    scene = new Scene(this);
    scene.setRadius(400);
    scene.fitBallInterpolation();

    spinningTask = new TimingTask() {
      public void execute() {
        spin();
      }
    };
    scene.registerTask(spinningTask);

    frame = new Frame();
  }

  public void draw() {
    background(0);
    scene.drawAxes();

    pushStyle();
    scene.pushModelView();
    scene.applyTransformation(frame);
    //fill(255, 0, 0, 100);
    scene.drawGrid();

    scene.popModelView();
    popStyle();
  }

  public void keyPressed() {
    if (key == 's')
      scene.fitBall();
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
