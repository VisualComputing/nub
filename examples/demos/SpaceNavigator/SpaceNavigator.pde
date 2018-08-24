/**
 * Space Navigator
 * by Jean Pierre Charalambos.
 *
 * This demo shows how to control your scene shapes using a Space Navigator
 * (3D mouse), with 6 degrees-of-freedom (DOFs). It requires the GameControlPlus
 * library and a Space Navigator and it has been tested only under Linux.
 */

import frames.primitives.*;
import frames.core.*;
import frames.processing.*;

import org.gamecontrolplus.*;
import net.java.games.input.*;

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

void setup() {
  size(1600, 800, P3D);
  openSpaceNavigator();
  scene = new Scene(this);
  scene.setFieldOfView(PI / 3);
  //scene.setType(Graph.Type.ORTHOGRAPHIC);
  scene.setRadius(1500);
  scene.fitBallInterpolation();
  Shape[] shapes = new Shape[50];
  for (int i = 0; i < shapes.length; i++) {
    tint(random(0,255), random(0,255), random(0,255), random(150,255));
    shapes[i] = new Shape(scene, loadShape("rocket.obj"));
    scene.randomize(shapes[i]);
  }
  smooth();
}

void draw() {
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
  scene.translate("SPCNAV", 10 * snXPos.getValue(), 10 * snYPos.getValue(), 10 * snZPos.getValue());
  scene.rotate("SPCNAV", -snXRot.getValue() * 20 * PI / width, snYRot.getValue() * 20 * PI / width, snZRot.getValue() * 20 * PI / width);
}

void mouseMoved() {
  scene.cast();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.spin();
  else if (mouseButton == RIGHT)
    scene.translate();
  else
    scene.scale(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  scene.zoom(event.getCount() * 20);
}

void keyPressed() {
  if (key == ' ')
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