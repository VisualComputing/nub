/**
 * Kinematics.
 * by Juan Pablo Bonilla and Juli??n Dur??n. 
 * 
 * A example about kinematics for SCARA ROBOT
 * References: https://sites.google.com/site/proyectosroboticos/cinematica-inversa-i
 * http://en.wikipedia.org/wiki/SCARA
 *
 * Drag the iFrame to control the robot arm.
 *
 * Press 'h' to display the key shortcuts and mouse bindings in the console.
 */
 
import remixlab.proscene.*;

PShape base_robot; 
PShape link_one;
PShape link_two;
PShape link_three;
PShape table;
//Declare the 3D Scene
Scene scene;
//Inverse Kinematics SCARA ROBOT variables
PVector posicion;//Position Vector
float posicion_x;//Axis (X) Position Variable
float posicion_y;//Axis (Y) Position Variable
float posicion_z;//Axis (Z) Position Variable
//Inverse Kinematics cariables
float hipotenusa;
float alfa;
float beta;
float gamma;
float AngBrazo;
float AngAntBr;
//iFrame defining the position Vector
InteractiveFrame punto;

void setup() {
  size(640, 500, P3D);
  //Load OBJ models
  base_robot = loadShape("base_robot.obj");
  link_one = loadShape("link_one.obj");
  link_two = loadShape("link_two.obj");
  link_three = loadShape("link_three.obj");
  table = loadShape("table.obj");
  scene = new Scene(this);
  scene.setRadius(200);
  scene.setVisualHints( Scene.PICKING );
  punto = new InteractiveFrame( scene );
  punto.setPosition(0, -10, 0);
}

void draw() {
  //Compute angles for revolute joints and traslation for the prismatic joint
  posicion = new PVector(punto.position().x(), punto.position().y(), punto.position().z());
  posicion_x = posicion.x-32;
  posicion_y = posicion.y;
  posicion_z = posicion.z;
  //Solution for Inverse Kinematics on SCARA ROBOT
  hipotenusa = sqrt ((posicion_x*posicion_x)+(posicion_z*posicion_z));
  alfa = atan2(-posicion_z, -posicion_x);
  //Constraint of work space in SCARA TOROBOT
  if (hipotenusa<=64) {
    beta = acos((hipotenusa*hipotenusa)/(2*32*hipotenusa));
    gamma = acos((32*32 + 32*32 - hipotenusa*hipotenusa)/(2*32*32));
  }
  AngBrazo=alfa+beta;
  AngAntBr=gamma-PI;
  //Constraint of movement in prismatic joint           
  if (posicion_y<=-48) {
    posicion_y=-48;
  }
  if (posicion_y>=3) {
    posicion_y=3;
  }

  background(70);

  drawBaseRobot();
  drawLinkOne();
  drawLinkTwo();
  drawLinkThree();
}    

//BASE_ROBOT Rendering 
public void drawBaseRobot() {
  translate(0, 50, 0);
  scale(1);
  rotateX(PI);
  fill(50, 10, 100);
  box(400, 5, 200);
  shape(base_robot);
  pushMatrix();
  translate(-50, 30, 0);
  scale(0.5);
  shape(table);
  popMatrix();
}

//LINK_ONE Rendering
public void drawLinkOne() {
  translate(32, 103, 0);
  scale(1);
  rotateY(AngBrazo);
  shape(link_one);
}

//LINK_TWO Rendering
public void drawLinkTwo() {
  translate(-32, 15, 0);
  scale(1);
  rotateY(AngAntBr); 
  shape(link_two);
}

//LINK_THREE Rendering
public void drawLinkThree() {
  translate(-32, -posicion_y-70, 0);
  noStroke();
  fill(24, 184, 19);
  scale(1);
  shape(link_three);
}

//Sets color according to selection
public void setColor(boolean selected) {
  if (selected)
    fill(200, 200, 0);
  else
    fill(200, 200, 200);
}