/**
 * Basic Use.
 * by Jean Pierre Charalambos.
 * 
 * This example illustrates a direct approach to use proscene by Scene proper
 * instantiation.
 * 
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */

import remixlab.proscene.*;

Scene scene;
//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

void setup() {
  size(640, 360, renderer);
  //Scene instantiation
  scene = new Scene(this);
  // when damping friction = 0 -> spin
  scene.eyeFrame().setDamping(0);
  //println("spinning sens: " +  scene.eyeFrame().spinningSensitivity());
}

void draw() {
  background(0);
  fill(204, 102, 0, 150);
  scene.drawTorusSolenoid();
}

void keyPressed() {
  if(scene.eyeFrame().damping() == 0)
    scene.eyeFrame().setDamping(0.5);
  else
    scene.eyeFrame().setDamping(0);
  println("Camera damping friction now is " + scene.eyeFrame().damping());
}