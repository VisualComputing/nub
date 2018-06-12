package basics;

import frames.processing.Scene;
import frames.processing.Shape;
import org.gamecontrolplus.ControlButton;
import org.gamecontrolplus.ControlDevice;
import org.gamecontrolplus.ControlIO;
import org.gamecontrolplus.ControlSlider;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

// The space navigator uses a right-handed coordinate system
public class SpaceNavigator extends PApplet {
  ControlIO control;
  ControlDevice device; // my SpaceNavigator
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
  boolean snPicking;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    openSpaceNavigator();
    scene = new Scene(this);
    scene.setFieldOfView(PI / 3);
    //scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(1500);
    scene.fitBallInterpolation();
    Shape[] shapes = new Shape[50];
    for (int i = 0; i < shapes.length; i++) {
      shapes[i] = new Shape(scene, shape());
      scene.randomize(shapes[i]);
    }
    smooth();
  }

  PShape shape() {
    PShape fig = createShape(BOX, 150);
    fig.setStroke(color(0, 255, 0));
    fig.setStrokeWeight(3);
    fig.setFill(color(random(0, 255), random(0, 255), random(0, 255)));
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

  void spaceNavigatorPicking() {
    float x = map(snXPos.getValue(), -.8f, .8f, 0, width);
    float y = map(snYPos.getValue(), -.8f, .8f, 0, height);
    // update the space navigator tracked frame:
    scene.cast("SPCNAV", x, y);
    // draw picking visual hint
    pushStyle();
    strokeWeight(3);
    stroke(0, 255, 0);
    scene.drawCross(x, y, 30);
    popStyle();
  }

  void spaceNavigatorInteraction() {
    scene.translate("SPCNAV", 10 * snXPos.getValue(), 10 * snYPos.getValue(), -10 * snZPos.getValue());
    scene.rotate("SPCNAV", snXRot.getValue() * 10 * PI / width, snYRot.getValue() * 10 * PI / width, snZRot.getValue() * 10 * PI / width);
  }

  public void mouseMoved() {
    scene.cast("mouse");
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.spin("mouse");
    else if (mouseButton == RIGHT)
      scene.translate("mouse");
    else
      scene.scale("mouse", scene.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom("mouse", event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'r')
      scene.setRightHanded();
    if (key == 'l')
      scene.setLeftHanded();
    // enables/disables picking with the space navigator
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
    PApplet.main(new String[]{"basics.SpaceNavigator"});
  }
}
