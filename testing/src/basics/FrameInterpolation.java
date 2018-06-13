package basics;

import frames.core.Frame;
import frames.core.Interpolator;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.event.MouseEvent;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class FrameInterpolation extends PApplet {
  Scene scene;
  Interpolator interpolator, eyeInterpolator1, eyeInterpolator2;
  Shape shape;
  boolean showEyePath = true;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    //scene.setFieldOfView(PI / 3);
    scene.setRadius(150);

    // interpolation 1. Default eye interpolations
    scene.fitBallInterpolation();

    // interpolation 2. Custom eye interpolations
    eyeInterpolator1 = new Interpolator(scene.eye());
    eyeInterpolator2 = new Interpolator(scene.eye());

    // interpolation 3. Custom (arbitrary)frame interpolations

    shape = new Shape(scene) {
      // Note that within visit() geometry is defined at the
      // frame local coordinate system.
      @Override
      public void setGraphics(PGraphics pg) {
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
    interpolator.setLoop();
    // Create an initial path
    int nbKeyFrames = 4;
    for (int i = 0; i < nbKeyFrames; i++) {
      Frame iFrame = new Frame(scene);
      iFrame.setPosition(-100 + 200 * i / (nbKeyFrames - 1), 0, 0);
      iFrame.setScaling(random(0.25f, 4.0f));
      interpolator.addKeyFrame(iFrame);
    }
    interpolator.start();
  }

  public void draw() {
    background(0);
    scene.traverse();

    pushStyle();
    stroke(255);
    scene.drawPath(interpolator, 5);
    popStyle();

    for (Frame frame : interpolator.keyFrames()) {
      pushMatrix();
      scene.applyTransformation(frame);
      if (scene.track(scene.mouse(), frame))
        scene.drawAxes(40);
      else
        scene.drawAxes(20);
      popMatrix();
    }
    if (showEyePath) {
      pushStyle();
      fill(255, 0, 0);
      stroke(0, 255, 0);
      scene.drawPath(eyeInterpolator1, 3);
      scene.drawPath(eyeInterpolator2, 3);
      popStyle();
    }
  }

  @Override
  public void mouseMoved() {
    scene.track();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin();
      //scene.lookAround(upVector);
      //scene.mouseCAD();
    else if (mouseButton == RIGHT)
      scene.translate();
      //scene.mousePan();
    else
      //scene.zoom(mouseX - pmouseX);
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    //scene.zoom(event.getCount() * 20);
    scene.scale(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'i') {
      //println(((Node)scene.eye()).interpolators().size());
      println("path 1: " + eyeInterpolator1.size());
      println("path 2: " + eyeInterpolator2.size());
    }

    if (key == ' ')
      showEyePath = !showEyePath;

    if (key == '1')
      eyeInterpolator1.addKeyFrame(scene.eye().get());
    if (key == 'a')
      eyeInterpolator1.toggle();
    if (key == 'b')
      eyeInterpolator1.purge();

    if (key == '2')
      eyeInterpolator2.addKeyFrame(scene.eye().get());
    if (key == 'c')
      eyeInterpolator2.toggle();
    if (key == 'd')
      eyeInterpolator2.purge();

    if (key == '-')
      interpolator.setSpeed(interpolator.speed() - 0.25f);
    if (key == '+')
      interpolator.setSpeed(interpolator.speed() + 0.25f);

    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'f')
      scene.fitBall();

    if (key == 'u') scene.saveConfig("/home/pierre/config.json");
    if (key == 'v') scene.loadConfig("/home/pierre/config.json");
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.FrameInterpolation"});
  }
}
