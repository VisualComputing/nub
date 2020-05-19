/**
 * Simple Joint Editor
 * by Sebastian Chaparro Cuevas.
 *
 * In this example a Skeleton is loaded from an .json file (specified by skeletonPath) and the idea is to
 * set the properties of the Skeleton's joints, specially their associated names and their constraints.
 * 
 * Double Click on a joint to display a panel on the left with its information and modify it.
 * Double Click on the scene to hide the panel.
 * Press 'P' when you want to save the skeleton.
 *
 * This example requires the g4p library: http://www.lagers.org.uk/g4p/
 * 
 * Please refer to https://github.com/sechaparroc/nub/wiki/6.-Rigging-skeletons-with-constraints for a detailed explanation.
 */

import g4p_controls.*;
import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.core.constraint.*;
import nub.ik.animation.*;
import nub.ik.solver.*;


//Edit easily the Skeleton's joints
Scene scene;
Skeleton skeleton;

//This is the path in which the skeleton will be loaded and saved if specified
String skeletonInputPath = "data/rex.json"; //Where to read the skeleton information 
String skeletonOutputPath = "data/rex_constrained.json"; //where to save the skeleton information

//Choose P2D or P3D
String renderer = P3D;
int w = 1000;
int h = 1000;

JointPanel panel;

void settings() {
  size(w, h, renderer);
}

void setup() {
  Graph.inertia = 0;
  Joint.axes = true;
  // Create a scene
  scene = new Scene(this, renderer, w, h);
  scene.setType(Graph.Type.ORTHOGRAPHIC);
  scene.setRightHanded();
  //Create the Skeleton and add an Interactive Joint at the center of the scene
  skeleton = new Skeleton(scene, skeletonInputPath);
  //Set the radius of the scene
  setSceneRadius(skeleton);
  scene.fit();
  //Create the Joint editor panel
  float offset = w * 0.7f;
  panel = new JointPanel(this, offset, 0, width - offset, height);
}

void setSceneRadius(Skeleton skeleton){
  //Get max coordinate
  float max_dist = 0;  
  for(Node n : skeleton.joints().values()){
    float d = n.position().magnitude();
    max_dist = d > max_dist ? d : max_dist;
  } 
  skeleton.scene().setRadius(max_dist * 1.5f);
}

void draw() {
  background(0);
  // 1. Fill in and display front-buffer
  scene.beginDraw();
  scene.context().background(0);
  scene.drawAxes();
  scene.render();
  //Draw panel information
  panel.drawInformation();
  scene.endDraw();
  scene.display();
  
}

void mouseClicked(MouseEvent event){
  if(panel.isOver(mouseX, mouseY)) return;
  if(event.getClickCount() == 2){
    if (event.getButton() == RIGHT) {
      scene.align();
    } else{
      panel.setJointName(skeleton);
      if(scene.node() == null){ 
        panel.setEnabled(false);
      }
      else{
        panel.getInformationFromJoint(skeleton, scene.node());
      }
    }
  }
}

void mouseMoved() {
  if(panel.isOver(mouseX, mouseY)) return; 
  scene.mouseTag();
}

void mouseDragged() {
  if(panel.isOver(mouseX, mouseY)) return; 
  if (mouseButton == LEFT)
    scene.mouseSpin();
  else if (mouseButton == RIGHT)
    scene.mouseTranslate();
  else
    scene.scale(mouseX - pmouseX);
}

void mouseWheel(MouseEvent event) {
  if(panel.isOver(mouseX, mouseY)) return; 
  scene.scale(event.getCount() * 20);
}

void keyPressed(){
  if(key == 'P' || key == 'p'){
    if(!panel._nameField.hasFocus()){
      panel.setJointName(skeleton);
      println("Skeleton information saved on : " + skeletonOutputPath);
      skeleton.save(skeletonOutputPath);
    }
  }
}

PShape caja() {
  PShape caja = scene.is3D() ? createShape(BOX, random(60, 100)) : createShape(RECT, 0, 0, random(60, 100), random(60, 100));
  caja.setStrokeWeight(3);
  caja.setStroke(color(random(0, 255), random(0, 255), random(0, 255)));
  caja.setFill(color(random(0, 255), random(0, 255), random(0, 255), random(0, 255)));
  return caja;
}
