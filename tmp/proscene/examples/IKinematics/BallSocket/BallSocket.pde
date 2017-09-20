import remixlab.bias.*;
import remixlab.bias.event.*;
import remixlab.dandelion.constraint.*;
import remixlab.dandelion.core.*;
import remixlab.dandelion.geom.*;
import remixlab.fpstiming.*;
import remixlab.proscene.*;
import remixlab.util.*;

/*
 * Testing Ball And Socket Constraint and Hinge Constraint
 * Author : Sebastian Chaparro
 * Press 'x' to rotate the Frames in the scene around X Axis  
 * Press 'y' to rotate the Frames in the scene around Y Axis
 * Press 'z' to rotate the Frames in the scene around Z Axis
 * Press 'g' to choose between global or local Axis 
 * Press 'c' to invert rotation angle
 * There are two Interactive Frames in the scene, the first one has
 * a Ball and Socket constraint attached and a Thorus is drawn in the center
 * of the Local coordinates System, the other one has a Hinge constraint 
 * attached with respect to Y Axis and a Sphere is drawn in the center
 * of the Local coordinates System.
 */

Scene scene;
PFont myFont;
InteractiveFrame frame, f2, hf;
BallAndSocket ball = new BallAndSocket();
BallAndSocket b2 = new BallAndSocket();
Hinge h = new Hinge(); 

int activeConstraint;
boolean wC = true;

String renderer = P3D;

public void setup() {
  size(640, 360, renderer);
  scene = new Scene(this);
  scene.setCameraType(Camera.Type.ORTHOGRAPHIC);
  scene.setAxesVisualHint(true);
  
  ball.setUp(radians(80));
  ball.setDown(radians(80));
  ball.setLeft(radians(80));
  ball.setRight(radians(80));
  ball.setRestRotation(new Quat());

  InteractiveFrame dummy = new InteractiveFrame(scene);
  dummy.translate(new Vec(0,0,0));
  Vec v = new Vec(10, 10, 10);
  frame = new InteractiveFrame(scene);
  frame.setReferenceFrame(dummy);
  frame.translate(v);
  frame.setConstraint(ball);

  h.setAxis(new Vec(0,1,0));
  h.setMax(radians(30));
  h.setMin(radians(30));

  Vec v2 = new Vec(30,30,30);
  f2 = new InteractiveFrame(scene);
  f2.translate(v2);
  f2.rotate(new Quat(new Vec(1,0,0),radians(45)));
  h.setRestRotation((Quat)f2.rotation());
  f2.setConstraint(h);  
}

public void draw() {
  background(0);
  pushMatrix();
  frame.applyWorldTransformation();
  scene.drawAxes(40);  
  if (scene.motionAgent().defaultGrabber() == frame) {
    fill(0, 255, 255);
    scene.drawTorusSolenoid();
  }
  else if (frame.grabsInput()) {
    fill(255, 0, 0);
    scene.drawTorusSolenoid();
  }
  else {
    fill(0, 0, 255, 150);
    scene.drawTorusSolenoid();
  }
  popMatrix();
  
  pushMatrix();
  pushStyle();
  f2.applyTransformation();
  scene.drawAxes(60);  
  noStroke();
  if (scene.motionAgent().defaultGrabber() == f2) {
    fill(0, 255, 255);
    sphere(20);
  }
  else if (f2.grabsInput()) {
    fill(255, 0, 0);
    sphere(5);
  }
  else {
    fill(0, 0, 255, 150);
    sphere(5);
  }
  popStyle();
  popMatrix();

  fill(0, 0, 255);
}

float q = 5;
boolean global = true;

public void keyPressed(){
  if(key == 'c'){
    q = -1*q;
  }
  if(key == 'g'){
    global = !global;
  }
  
  if(key == 'z'){
    if(global){
      frame.rotate(new Quat(frame.transformOf(new Vec(0,0,1)), radians(q)));
      f2.rotate(new Quat(f2.transformOf(new Vec(0,0,1)), radians(q)));
    }
    else{
      frame.rotate(new Quat((new Vec(0,0,1)), radians(q)));
      f2.rotate(new Quat((new Vec(0,0,1)), radians(q)));
    }
      
  }
  if(key == 'x'){
    if(global){
      frame.rotate(new Quat(frame.transformOf(new Vec(1,0,0)), radians(q)));
      f2.rotate(new Quat(f2.transformOf(new Vec(1,0,0)), radians(q)));
    }
    else{ 
      frame.rotate(new Quat((new Vec(1,0,0)), radians(q)));
      f2.rotate(new Quat((new Vec(1,0,0)), radians(q)));
    }
  }
  if(key == 'y'){
    if(global){
      frame.rotate(new Quat(frame.transformOf(new Vec(0,1,0)), radians(q)));
      f2.rotate(new Quat(f2.transformOf(new Vec(0,1,0)), radians(q)));
    }
    else{
      frame.rotate(new Quat((new Vec(0,1,0)), radians(q)));
      f2.rotate(new Quat((new Vec(0,1,0)), radians(q)));
    }
  }  
}