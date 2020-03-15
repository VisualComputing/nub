package intellij;

import nub.core.Node;
import nub.processing.Scene;
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

  // nodes stuff:
  Scene scene;
  boolean rocket;
  boolean snPicking;
  boolean success;

  public void settings() {
    size(1600, 800, P3D);
  }

  public void setup() {
    success = openSpaceNavigator();
    scene = new Scene(this);
    scene.setRadius(1500);
    scene.fit(1);
    Node[] shapes = new Node[50];
    for (int i = 0; i < shapes.length; i++) {
      PShape pshape = rocket ? loadShape("/home/pierre/IdeaProjects/nub/testing/data/interaction/rocket.obj") : shape();
      shapes[i] = new Node(pshape);
      scene.randomize(shapes[i]);
      shapes[i].setPickingThreshold(0);
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
    scene.render();
    if (success)
      if (snPicking)
        spaceNavigatorPicking();
      else
        spaceNavigatorInteraction();
  }

  void spaceNavigatorPicking() {
    int x = (int) map(snXPos.getValue(), -.8f, .8f, 0, width);
    int y = (int) map(snYPos.getValue(), -.8f, .8f, 0, height);
    // update the space navigator tracked node:
    scene.tag("SPCNAV", x, y);
    // draw picking visual hint
    pushStyle();
    strokeWeight(3);
    stroke(0, 255, 0);
    scene.drawCross(x, y, 30);
    popStyle();
  }

  void spaceNavigatorInteraction() {
    // translate(x, y, z) expects params in screen-space
    // which has dimensions width * height * 1
    scene.translate("SPCNAV", 20 * snXPos.getValue(), 20 * snYPos.getValue(), snZPos.getValue() / 50);
    //scene.rotate("SPCNAV", -snXRot.getValue() * 20 * PI / width, snYRot.getValue() * 20 * PI / width, snZRot.getValue() * 20 * PI / width);
  }

  /*
  void spaceNavigatorInteraction() {
    //Vector eyeVector =  new Vector(-30 * snXPos.getValue(), -30 * snYPos.getValue(), 30 * snZPos.getValue());
    //Vector worldVector = scene.eye().worldDisplacement(eyeVector);
    //scene.eye().translate(worldVector);
    //eyeVector.setY(0);
    //eyeVector.setZ(0);
    //scene.eye().translate(eyeVector);
    float roll = -snXRot.getValue() * 20 * PI / width;
    float pitch = snYRot.getValue() * 20 * PI / width;
    float yaw = snZRot.getValue() * 20 * PI / width;
    //roll = 0;
    //pitch = 0;
    //yaw = 0;
    scene.eye().rotate(new Quaternion(roll, pitch, yaw));
    // scene.spin(new Quaternion(scene.isLeftHanded() ? roll : -roll, -pitch, scene.isLeftHanded() ? yaw : -yaw), scene.defaultNode("SPCNAV"));
  }
  */

  public void mouseMoved() {
    scene.mouseTag();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(scene.mouseDX());
  }

  public void mouseWheel(MouseEvent event) {
    scene.moveForward(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'r')
      scene.setRightHanded();
    if (key == 'l')
      scene.setLeftHanded();
    if (key == 'p')
      scene.togglePerspective();
    // enables/disables picking with the space navigator
    if (key == 'i')
      snPicking = !snPicking;
  }

  boolean openSpaceNavigator() {
    control = ControlIO.getInstance(this);
    String os = System.getProperty("os.name").toLowerCase();
    try {
      device = control.getDevice("3Dconnexion SpaceNavigator");// magic name for linux
    } catch (Exception exc) {
      println("No suitable device configured for Linux");
    }
    try {
      device = control.getDevice("SpaceNavigator");//magic name, for windows
    } catch (Exception exc) {
      println("No suitable device configured for Win");
    }
    if (device == null)
      return false;

    snXPos = device.getSlider(0);
    snYPos = device.getSlider(1);
    snZPos = device.getSlider(2);
    snXRot = device.getSlider(3);
    snYRot = device.getSlider(4);
    snZRot = device.getSlider(5);
    //button1 = device.getButton(0);
    //button2 = device.getButton(1);
    //device.setTolerance(5.00f);
    return true;
  }

  public static void main(String[] args) {
    PApplet.main(new String[]{"intellij.SpaceNavigator"});
  }
}
