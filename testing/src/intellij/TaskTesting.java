package intellij;

import nub.core.Interpolator;
import nub.core.Node;
import nub.processing.Scene;
import nub.processing.TimingTask;
import nub.timing.Task;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class TaskTesting extends PApplet {
  Scene scene;
  Task task, task2, task3;
  float fps = 60;
  long lapse, totalLapse, targetLapse;
  long period = 100;
  Interpolator interpolator, eyeInterpolator1, eyeInterpolator2;
  Node shape;
  boolean showEyePath = true;
  float speed = 1;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    scene.setRadius(150);

    // interpolation 1. Default eye interpolations
    scene.fit(1);

    // interpolation 2. Custom eye interpolations
    eyeInterpolator1 = new Interpolator(scene.eye());
    eyeInterpolator2 = new Interpolator(scene.eye());

    // interpolation 3. Custom (arbitrary) node interpolations

    shape = new Node(scene) {
      // Note that within render() geometry is defined at the
      // node local coordinate system.
      @Override
      public void graphics(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 255, 255, 125);
        pg.stroke(0, 0, 255);
        pg.strokeWeight(2);
        if (pg.is2D())
          pg.rect(0, 0, 100, 100);
        else
          pg.box(30);
        pg.popStyle();
      }
    };
    interpolator = new Interpolator(shape);
    //interpolator.setLoop();
    // Create an initial path
    for (int i = 0; i < random(4, 10); i++)
      interpolator.addKeyFrame(scene.randomNode());
    interpolator.run();

    //frameRate(100);
    task = new TimingTask(scene) {
      @Override
      public void execute() {
        long current = System.currentTimeMillis();
        if (lapse == 0)
          lapse = period;
        else
          lapse = current - lapse;
        totalLapse += lapse;
        targetLapse += period;
        println(scene.frameCount() + " target fps: " + fps + " nub fps: " + scene.frameRate() + " period: " + period + " lapse: " + lapse + " targetLapse: " + targetLapse + " totalLapse: " + totalLapse);
        lapse = current;
      }
    };
    //task.run(60);

    ///*
    task2 = new TimingTask(scene) {
      @Override
      public void execute() {
        println("one timer seq");
      }
    };
    task2.enableRecurrence();
    //task2.run(3000);

    task3 = new TimingTask(scene) {
      @Override
      public void execute() {
        println("recurrent timer parallel");
      }
    };
    task3.enableRecurrence(false);
    task3.enableConcurrence();
    task3.run(2000);

    //println("Total steps (according to formula): " + (interpolator.duration() * 1000) / (interpolator.period() * interpolator.speed()));
    //*/
  }

  public void draw() {
    background(0);
    drawScene();
    //scene.render();
    //println("count: p5 " + frameCount + " nub " + scene.frameCount() + " fps: p5 " + frameRate + " nub " + scene.frameRate());
  }

  void drawScene() {
    scene.render();

    pushStyle();
    stroke(255);
    // same as:scene.drawPath(interpolator, 5);
    scene.drawPath(interpolator);
    popStyle();

    for (Node node : interpolator.keyFrames()) {
      pushMatrix();
      scene.applyTransformation(node);
      scene.drawAxes(scene.tracks(node) ? 40 : 20);
      popMatrix();
    }
    if (showEyePath) {
      pushStyle();
      fill(255, 0, 0);
      stroke(0, 255, 0);
      // same as:
      // scene.drawPath(eyeInterpolator1, 3);
      // scene.drawPath(eyeInterpolator2, 3);
      scene.drawPath(eyeInterpolator1);
      scene.drawPath(eyeInterpolator2);
      popStyle();
    }
  }

  public void mouseMoved() {
    scene.track();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
    else if (mouseButton == RIGHT)
      scene.translate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    if (scene.is3D())
      scene.moveForward(event.getCount() * 20);
    else
      scene.scale(scene.eye(), event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == ' ')
      showEyePath = !showEyePath;

    if (key == 't')
      task3.toggle();

    if (key == 'q')
      if (task3.isActive())
        println("task3 is active");
      else
        println("task3 is NOT active");

    if (key == '1')
      eyeInterpolator1.addKeyFrame();
    if (key == 'a')
      eyeInterpolator1.toggle();
    if (key == 'b')
      eyeInterpolator1.purge();

    if (key == '2')
      eyeInterpolator2.addKeyFrame();
    if (key == 'c')
      eyeInterpolator2.toggle();
    if (key == 'd')
      eyeInterpolator2.purge();

    if (key == '-' || key == '+') {
      if (key == '-')
        speed -= 0.25f;
      else
        speed += 0.25f;
      interpolator.run(speed);
    }

    if (key == 'n')
      //if(((TimingTask)task3)._timer == null)
      println("null");

    if (key == 's')
      scene.fit(1);
    if (key == 'f')
      scene.fit();

    if (key == 'g')
      interpolator.toggle();

    if (key == 'j') {
      task2.enableRecurrence();
      task3.enableRecurrence();
    }

    if (key == 'f' || key == 'F') {
      if (key == 'F')
        fps += 5;
      else
        fps -= 5;
      frameRate(fps);
      println("New target fps set to: " + fps);
    }

    if (key == 'p') {
      println("count: p5 " + frameCount + " nub " + scene.frameCount() + " fps: p5 " + frameRate + " nub " + scene.frameRate());
    }

    if (key == 'h' || key == 'H')
      if (key == 'H')
        period += 5;
      else
        period -= 5;

    if (key == 'r' || key == 'R')
      if (task.isActive())
        task.stop();
      else
        task.run(period);
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.TaskTesting"});
  }
}
