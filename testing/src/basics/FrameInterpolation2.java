package basics;

import common.InteractiveNode;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import proscene.core.Interpolator;
import proscene.input.Event;
import proscene.input.Shortcut;
import proscene.input.event.MotionEvent1;
import proscene.input.event.MotionEvent2;
import proscene.input.event.TapEvent;
import proscene.input.event.TapShortcut;
import proscene.primitives.Frame;
import proscene.processing.Scene;
import proscene.processing.Shape;

/**
 * This example introduces the three different interpolations offered
 * by the Graph.
 */
public class FrameInterpolation2 extends PApplet {
  Scene scene;
  Interpolator nodeInterpolator, eyeInterpolator;
  boolean showEyePath;

  //controls
  Scene auxScene;
  Button button;
  PGraphics auxCanvas;
  int w = 1200;
  int h = 900;
  int oW = 2 * w / 3;
  int oH = h / 3;
  int oX = w - oW;
  int oY = h - oH;
  boolean showControls = true;

  public void settings() {
    size(w, h, P3D);
  }

  public void setup() {
    scene = new Scene(this);
    InteractiveNode eye = new InteractiveNode(scene);
    scene.setEye(eye);
    //interactivity defaults to the eye
    scene.setDefaultNode(eye);
    scene.setRadius(150);

    // interpolation 1. Default eye interpolations
    scene.fitBallInterpolation();

    // interpolation 2. Custom eye interpolations
    eyeInterpolator = new Interpolator(eye);

    // interpolation 3. Custom (arbitrary)frame interpolations, like the one
    // you guys David & Juan are currently exploring to deform a shape
    nodeInterpolator = new Interpolator(scene);
    nodeInterpolator.setLoop();
    // Create an initial path
    int nbKeyFrames = 4;
    for (int i = 0; i < nbKeyFrames; i++) {
      InteractiveNode iNode = new InteractiveNode(scene);
      iNode.setPosition(-100 + 200 * i / (nbKeyFrames - 1), 0, 0);
      iNode.setScaling(random(0.25f, 4.0f));
      nodeInterpolator.addKeyFrame(iNode);
    }
    nodeInterpolator.start();

    // application control
    auxCanvas = createGraphics(oW, oH, P2D);
    auxCanvas.rectMode(CENTER);
    auxScene = new Scene(this, auxCanvas, oX, oY);
    //auxScene.disablePickingBuffer();
    InteractiveNode eye1 = new InteractiveNode(auxScene);
    auxScene.setEye(eye1);
    //interactivity defaults to the eye
    auxScene.setDefaultNode(eye1);
    button = new Button(100, 60);
    //button.setPosition(200,50);
    auxScene.setRadius(200);
    auxScene.fitBall();
  }

  public void draw() {
    background(0);
    pushMatrix();
    scene.applyTransformation(nodeInterpolator.frame());
    scene.drawAxes(30);
    pushStyle();
    fill(0, 255, 255, 125);
    stroke(0, 0, 255);
    strokeWeight(2);
    if (scene.is2D())
      rect(0, 0, 100, 100);
    else
      box(30);
    popStyle();
    popMatrix();

    pushStyle();
    stroke(255);
    scene.drawPath(nodeInterpolator, 5);
    popStyle();

    for (Frame frame : nodeInterpolator.keyFrames()) {
      pushMatrix();
      scene.applyTransformation(frame);
      // Horrible cast, but Java is just horrible
      if (((InteractiveNode) frame).grabsInput())
        scene.drawAxes(35);
      else
        scene.drawAxes(20);
      popMatrix();
    }
    if (showEyePath) {
      pushStyle();
      fill(255, 0, 0);
      stroke(0, 255, 255);
      scene.drawPath(eyeInterpolator, 3);
      popStyle();
    }

    if (showControls) {
      scene.beginScreenCoordinates();
      auxScene.beginDraw();
      auxCanvas.background(29, 153, 243);
      auxScene.drawAxes();
      // calls visit() for each node in the graph
      //auxScene.traverse();
      button.draw();
      auxScene.endDraw();
      auxScene.display();
      scene.endScreenCoordinates();
    }
  }

  public void keyPressed() {
    if (key == 'c')
      showControls = !showControls;
    if (key == ' ')
      showEyePath = !showEyePath;
    if (key == 'l')
      eyeInterpolator.addKeyFrame(scene.eye().get());
    if (key == 'm')
      eyeInterpolator.toggle();
    if (key == 'n')
      eyeInterpolator.clear();
    if (key == 'u')
      nodeInterpolator.setSpeed(nodeInterpolator.speed() - 0.25f);
    if (key == 'v')
      nodeInterpolator.setSpeed(nodeInterpolator.speed() + 0.25f);
    if (key == 's')
      scene.fitBallInterpolation();
    if (key == 'f')
      scene.fitBall();
    if (key == 'i') {
      //println(scene._lastDisplay + " and " + auxScene._lastDisplay);
    }
  }

  // Controls are implemented from using (the new) Shape class which uses
  // the picking buffer (exact picking according to the picking shape)
  public class Button extends Shape {
    //button dimensions
    int _w, _h;

    public Button(int w, int h) {
      super(auxScene);
      _w = w;
      _h = h;
      ///*
      PShape rectangle = createShape(RECT, 0, 0, _w, _h);
      rectangle.setStroke(color(255));
      rectangle.setStrokeWeight(4);
      rectangle.setFill(color(127));
      //set(rectangle);
      //*/
    }

    @Override
    public void interact(MotionEvent2 event) {
      if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        nodeInterpolator.setSpeed(nodeInterpolator.speed() + event.dx() / 10);
      else if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
        rotate(event);
      else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
        if (isEye() && graph().is3D())
          translateZ(event);
        else
          scale(event);
    }

    @Override
    public void interact(MotionEvent1 event) {
      nodeInterpolator.setSpeed(nodeInterpolator.speed() + event.dx() / 10);
    }

    @Override
    public void interact(TapEvent event) {
      if (event.shortcut().matches(new TapShortcut(Event.NO_MODIFIER_MASK, LEFT, 2)))
        scene.fitBallInterpolation();
      if (event.shortcut().matches(new TapShortcut(Event.SHIFT, RIGHT, 1)))
        println("got me!");
    }

    ///*
    @Override
    protected void set(PGraphics pg) {
      pg.rectMode(CENTER);
      pg.fill(255, 0, 0);
      pg.rect(0, 0, _w, _h);
    }
    //*/
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.FrameInterpolation2"});
  }
}
