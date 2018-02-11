package basics;

import common.InteractiveNode;
import processing.core.PApplet;
import proscene.core.Interpolator;
import proscene.core.Node;
import proscene.primitives.Frame;
import proscene.processing.Scene;
import proscene.processing.Shape;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class ShapeInterpolation extends PApplet {
  Scene scene;
  Shape shape;
  Interpolator interpolator;
  boolean showEyePath = true;

  //Choose P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(1000, 800, renderer);
  }

  public void setup() {
    rectMode(CENTER);
    scene = new Scene(this);
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    //interactivity defaults to the eye
    scene.setDefaultNode(eye);
    scene.setRadius(150);

    // interpolation 1. Default eye interpolations
    scene.fitBallInterpolation();

    if(scene.is3D())
      shape = new Shape(scene, createShape(SPHERE, 10));
    else
      shape = new Shape(scene, createShape(RECT, 0,0,50,50));
    // interpolation 2. Custom eye interpolations
    interpolator = new Interpolator(shape);
    interpolator.setLoop();
    // Create an initial path
    int nbKeyFrames = 4;
    //for (int i = 0; i < nbKeyFrames; i++)
      //interpolator.addKeyFrame(Frame.random(scene.center(), scene.radius()));
    /*
    for (int i = 0; i < nbKeyFrames; i++) {
      Node node = new Node(scene);
      node.randomize();
      interpolator.addKeyFrame(node);
    }
    */
    for (int i = 0; i < nbKeyFrames; i++)
      interpolator.addKeyFrame(Node.random(scene));
    interpolator.start();
  }

  public void draw() {
    background(0);
    scene.traverse();
    if (showEyePath) {
      pushStyle();
      fill(255, 0, 0);
      stroke(0, 255, 0);
      scene.drawPath(interpolator, 5);
      popStyle();
    }
  }

  public void keyPressed() {
    if (key == ' ')
      showEyePath = !showEyePath;
    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'f')
      scene.fitBall();
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.ShapeInterpolation"});
  }
}
