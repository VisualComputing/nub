package basics;

import frames.core.Frame;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import org.gamecontrolplus.ControlButton;
import org.gamecontrolplus.ControlDevice;
import org.gamecontrolplus.ControlIO;
import org.gamecontrolplus.ControlSlider;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

public class SpaceNavigator2 extends PApplet {
  ControlIO control;
  ControlDevice device; // my SpaceNavigator1
  ControlSlider snXPos; // Positions
  ControlSlider snYPos;
  ControlSlider snZPos;
  ControlSlider snXRot; // Rotations
  ControlSlider snYRot;
  ControlSlider snZRot;
  ControlButton button1; // Buttons
  ControlButton button2;

  // frames stuff:
  Scene scene;
  Shape[] mouseShapes;
  Frame mouseTrackedFrame;
  Shape[] snShapes;
  Frame snTrackedFrame;
  boolean snPicking;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    openSpaceNavigator();
    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);
    scene.setRadius(1500);
    scene.fitBallInterpolation();
    // set the eye as the space navigator default frame
    snTrackedFrame = scene.eye();
    // mouseShapes
    mouseShapes = new Shape[25];
    for (int i = 0; i < mouseShapes.length; i++) {
      mouseShapes[i] = new Shape(scene, mouseShape());
      scene.randomize(mouseShapes[i]);
    }
    snShapes = new Shape[25];
    for (int i = 0; i < snShapes.length; i++) {
      snShapes[i] = new Shape(scene, snShape());
      scene.randomize(snShapes[i]);
    }
    smooth();
  }

  PShape mouseShape() {
    PShape fig = createShape(BOX, 150);
    fig.setStroke(color(0, 255, 255));
    fig.setStrokeWeight(3);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255), 125));
    return fig;
  }

  PShape snShape() {
    PShape fig = createShape(SPHERE, 150);
    fig.setStroke(color(255, 255, 0));
    fig.setStrokeWeight(3);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255), 125));
    return fig;
  }

  public void draw() {
    background(0);
    scene.drawAxes();
    scene.traverse();
    if (snPicking)
      spaceNavigatorPicking();
    else
      spaceNavigatorInteraction();
  }

  void spaceNavigatorInteraction() {
    if (snTrackedFrame == null)
      return;
    scene.translate(new Vector(10 * snXPos.getValue(), 10 * snYPos.getValue(), -10 * snZPos.getValue()), snTrackedFrame);
    scene.rotate(snXRot.getValue(), snYRot.getValue(), -snZRot.getValue(), 10 * PI / width, snTrackedFrame);
  }

  void spaceNavigatorPicking() {
    float x = map(snXPos.getValue(), -.8f, .8f, 0, width);
    float y = map(snYPos.getValue(), -.8f, .8f, 0, height);
    snTrackedFrame = null;
    for (int i = 0; i < snShapes.length; i++)
      if (scene.track(x, y, snShapes[i])) {
        snTrackedFrame = snShapes[i];
        break;
      }
    // draw picking visual hint
    pushStyle();
    strokeWeight(3);
    stroke(0, 255, 0);
    scene.drawCross(x, y, 30);
    popStyle();
  }

  // same as 'mousePicking'
  public void mouseMoved() {
    mouseTrackedFrame = null;
    for (int i = 0; i < mouseShapes.length; i++)
      if (scene.track(mouseX, mouseY, mouseShapes[i])) {
        mouseTrackedFrame = mouseShapes[i];
        break;
      }
  }

  // same as 'mouseInteraction'
  public void mouseDragged() {
    Frame frame = mouseTrackedFrame == null ? scene.eye() : mouseTrackedFrame;
    if (mouseButton == LEFT)
      scene.mouseSpin(frame);
    else if (mouseButton == RIGHT)
      scene.mouseTranslate(frame);
    else
      scene.scale(mouseX - pmouseX, frame);
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'y')
      scene.flip();
    // define the tracking device
    if (key == 'i')
      snPicking = !snPicking;
  }

  void openSpaceNavigator() {
    println(System.getProperty("os.name"));
    control = ControlIO.getInstance(this);
    String os = System.getProperty("os.name").toLowerCase();
    if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0)
      device = control.getDevice("3Dconnexion SpaceNavigator");// magic name for linux
    else
      device = control.getDevice("SpaceNavigator");//magic name, for windows
    if (device == null) {
      println("No suitable device configured");
      System.exit(-1); // End the program NOW!
    }
    //device.setTolerance(5.00f);
    snXPos = device.getSlider(0);
    snYPos = device.getSlider(1);
    snZPos = device.getSlider(2);
    snXRot = device.getSlider(3);
    snYRot = device.getSlider(4);
    snZRot = device.getSlider(5);
    //button1 = device.getButton(0);
    //button2 = device.getButton(1);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"basics.SpaceNavigator2"});
  }
}
