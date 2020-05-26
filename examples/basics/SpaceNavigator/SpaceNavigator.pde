/**
 * Space Navigator
 * by Jean Pierre Charalambos.
 *
 * This demo shows how to control your scene shapes using a Space Navigator
 * (3D mouse), with 6 degrees-of-freedom (DOFs). It requires the GameControlPlus
 * library and a Space Navigator and it has been tested only under Linux.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;

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

// nub stuff:
Scene scene;
boolean snPicking;

void setup() {
  size(1600, 800, P3D);
  openSpaceNavigator();
  scene = new Scene(this);
  scene.setRadius(1500);
  scene.fit(1);
  Node[] shapes = new Node[50];
  for (int i = 0; i < shapes.length; i++) {
    tint(random(0,255), random(0,255), random(0,255), random(150,255));
    shapes[i] = new Node(loadShape("rocket.obj"));
    scene.randomize(shapes[i]);
  }
  smooth();
}

void draw() {
  background(0);
  scene.drawAxes();
  scene.render();
  if (snPicking)
    spaceNavigatorPicking();
  else
    spaceNavigatorInteraction();
}

void spaceNavigatorPicking() {
  float x = map(snXPos.getValue(), -0.8, 0.8, 0, width);
  float y = map(snYPos.getValue(), -0.8, 0.8, 0, height);
  // update the space navigator tagged node:
  scene.tag("SPCNAV", int(x), int(y));
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
  scene.rotate("SPCNAV", -snXRot.getValue() * 20 * PI / width, snYRot.getValue() * 20 * PI / width, snZRot.getValue() * 20 * PI / width);
}

void mouseMoved() {
  scene.mouseTag();
}

void mouseDragged() {
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.scale(scene.mouseDX());
}

void mouseWheel(MouseEvent event) {
  scene.moveForward(event.getCount() * 20);
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
  //device.setTolerance(5);
  snXPos = device.getSlider(0);
  snYPos = device.getSlider(1);
  snZPos = device.getSlider(2);
  snXRot = device.getSlider(3);
  snYRot = device.getSlider(4);
  snZRot = device.getSlider(5);
  //button1 = device.getButton(0);
  //button2 = device.getButton(1);
}
