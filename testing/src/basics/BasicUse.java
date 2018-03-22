package basics;

import common.InteractiveNode;
import common.OrbitNode;
import frames.core.Node;
import frames.input.Event;
import frames.input.Shortcut;
import frames.input.event.TapShortcut;
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
public class BasicUse extends PApplet {
  Scene scene;
  boolean yDirection;
  Frame frame;
  Node eye, node;
  float radius = 100;
  protected TimingTask spinningTask;

  public void settings() {
    size(800, 800, P3D);
  }

  public void spin() {
    scene.eye().rotate(new Quaternion(yDirection ? new Vector(0, 1, 0) : new Vector(1, 0, 0), PI / 100), scene.anchor());
    //scene.eye().rotateAround(new Quaternion(yDirection ? new Vector(0, 1, 0) : new Vector(1, 0, 0), PI / 100), scene.anchor());
    //scene.eye()._rotate(new Quaternion(yDirection ? scene.eye().yAxis() : scene.eye().xAxis(), PI / 100), scene.anchor());
  }

  public void setup() {
    rectMode(CENTER);
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

    ///*
    //eye = new InteractiveNode(scene);
    eye = new OrbitNode(scene);
    //eye.setDamping(0f);
    //eye.setDamping(1);
    //eye.setRotationSensitivity(0.1f);
    //eye.setSpinningSensitivity(1);
    scene.setEye(eye);
    scene.setFieldOfView(PI / 3);
    scene.setDefaultGrabber(eye);
    scene.fitBallInterpolation();
    //*/

    node = new Shape(scene) {
      @Override
      public void set(PGraphics pGraphics) {
        pGraphics.pushStyle();
        pGraphics.rectMode(CENTER);
        pGraphics.fill(255, 0, 255);
        if (scene.is3D())
          Scene.drawCylinder(pGraphics, 30, radius, 200);
        else
          pGraphics.rect(10, 10, 200, 200);
        pGraphics.popStyle();
      }

      @Override
      public void interact(Event event) {
        if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
          translate(event);
        else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
          rotate(event);
        else if (event.shortcut().matches(new TapShortcut(PApplet.RIGHT)))
          align();
        else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
          if (isEye() && graph().is3D())
            translateZ(event);
          else
            scale(event);
      }
    };

    node.setDamping(0.2f);
    node.setSpinningSensitivity(0);
    //node.startSpinning(Quaternion.random(), 10,1);
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.traverse();
    pushStyle();
    scene.pushModelView();
    scene.applyTransformation(frame);
    fill(255, 0, 0, 100);
    if (scene.is3D())
      sphere(radius);
    else
      ellipse(0, 0, radius, radius);
    scene.popModelView();
    popStyle();
  }

  public void keyPressed() {
    if (key == 's')
      scene.fitBall();
    if (key == ' ')
      if (scene.mouse().mode() == Mouse.Mode.CLICK)
        scene.mouse().setMode(Mouse.Mode.MOVE);
      else
        scene.mouse().setMode(Mouse.Mode.CLICK);
    if (key == 't')
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
    PApplet.main(new String[]{"basics.BasicUse"});
  }
}
