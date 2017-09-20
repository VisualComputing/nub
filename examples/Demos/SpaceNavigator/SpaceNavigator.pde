/**
 * Space Navigator
 * by Jean Pierre Charalambos.
 *
 * This demo shows how to control your scene Eye and iFrames using a Space Navigator
 * (3D mouse), with 6 degrees-of-freedom (DOFs). It requires the GameControlPlus
 * library and a Space Navigator and it has been tested only under Linux.
 *
 * We implement the (non-conventional) user interaction mechanism as an HIDAgent
 * which provides up to 6 DOFs. The Agent gathers Space Navigator input data and reduces
 * it as "bogus" DOF6 event from which some proscene actions may be bound.
 * 
 * Press 'h' to display the key shortcuts, mouse and SpaceNavigator bindings in the console.
 */

import remixlab.proscene.*;
import remixlab.bias.*;
import remixlab.bias.event.*;
import remixlab.dandelion.geom.*;
import remixlab.dandelion.core.*;

import org.gamecontrolplus.*;
import net.java.games.input.*;

PImage bg;
PImage texmap;

int sDetail = 35;  // Sphere detail setting
float rotationX = 0;
float rotationY = 0;
float velocityX = 0;
float velocityY = 0;
float globeRadius = 400;
float pushBack = 0;

float[] cx, cz, sphereX, sphereY, sphereZ;
float sinLUT[];
float cosLUT[];
float SINCOS_PRECISION = 0.5;
int SINCOS_LENGTH = int(360.0 / SINCOS_PRECISION);

Scene scene;
static int SN_ID;
InteractiveFrame iFrame;
HIDAgent hidAgent;

ControlIO control;
ControlDevice device; // my SpaceNavigator
ControlSlider sliderXpos; // Positions
ControlSlider sliderYpos;
ControlSlider sliderZpos;
ControlSlider sliderXrot; // Rotations
ControlSlider sliderYrot;
ControlSlider sliderZrot;
ControlButton button1; // Buttons
ControlButton button2;

public class HIDAgent extends Agent {
  // array of sensitivities that will multiply the sliders input
  // found pretty much as trial an error
  float [] sens = {10, 10, 10, 10, 10, 10};
  
  public HIDAgent(Scene scn) {
    super(scn.inputHandler());
    // SN_ID will be assigned an unique id with 6 DOF's. The id may be
    // used to bind (frame) actions to the gesture, pretty much in
    // the same way as it's done with the LEFT and RIGHT mouse gestures.
    SN_ID = MotionShortcut.registerID(6, "SN_SENSOR");
    addGrabber(scene.eyeFrame());
    setDefaultGrabber(scene.eyeFrame());
  }
  
  // we need to override the agent sensitivities method for the agent
  // to apply them to the input data gathered from the sliders
  @Override
  public float[] sensitivities(MotionEvent event) {
    if (event instanceof DOF6Event)
      return sens;
    else
      return super.sensitivities(event);
  }
  
  // polling is done by overriding the feed agent method
  // note that we pass the id of the gesture
  @Override
  public DOF6Event feed() {
    return new DOF6Event(sliderXpos.getValue(), sliderYpos.getValue(), sliderZpos.getValue(), sliderXrot.getValue(), sliderYrot.getValue(), sliderZrot.getValue(), BogusEvent.NO_MODIFIER_MASK, SN_ID);
  }
}

void setup() {
  size(800, 600, P3D);
  openSpaceNavigator();
  texmap = loadImage("world32k.jpg");    
  initializeSphere(sDetail);
  scene = new Scene(this);
  
  scene.setGridVisualHint(false);
  scene.setAxesVisualHint(false);  
  scene.setRadius(260);
  scene.showAll();

  hidAgent = new HIDAgent(scene);
  
  // the iFrame is added to all scene agents (that's why we previously instantiated the hidAgent)
  // Thanks to the Processing Foundation for providing the rocket shape
  iFrame = new InteractiveFrame(scene, loadShape("rocket.obj"));
  iFrame.translate(new Vec(275, 180, 0));
  iFrame.scale(0.3);
  
  // we bound some frame DOF6 actions to the gesture on both frames
  scene.eyeFrame().setMotionBinding(SN_ID, "translateRotateXYZ");
  iFrame.setMotionBinding(SN_ID, "translateRotateXYZ");
  // and the custom behavior to the right mouse button
  iFrame.setMotionBinding(RIGHT, "customBehavior");

  smooth();
}

void customBehavior(InteractiveFrame frame, MotionEvent event) {
  frame.screenRotate(event);
}

void draw() {    
  background(0);    
  renderGlobe();
  scene.drawFrames();
}

void keyPressed() {
  if (key == 'y')
      scene.flip();
  //Shift the default grabber for all agents: mouseAgent, keyboardAgent and the hidAgent
  if ( key == 'i')
    scene.inputHandler().shiftDefaultGrabber(scene.eyeFrame(), iFrame);
  if(key == ' ')
    if( scene.eyeFrame().isActionBound("hinge") ) {
      scene.eyeFrame().setMotionBinding(SN_ID, "translateRotateXYZ");
      scene.eye().lookAt(scene.center());
      scene.showAll();
    }
    else {
      scene.eyeFrame().setMotionBinding(SN_ID, "hinge");
      Vec t = new Vec(0,0,0.7*globeRadius);
      float a = TWO_PI - 2; 
      scene.camera().setPosition(t);
      //For HINGE to work flawlessly we need to line up the eye up vector along the anchor and
      //the camera position:
      scene.camera().setUpVector(Vec.subtract(scene.camera().position(), scene.anchor()));
      //The rest is just to make the scene appear in front of us. We could have just used
      //the space navigator itself to make that happen too.
      scene.camera().frame().rotate(new Quat(a, 0, 0));
    }
}

void renderGlobe() {  
  //lights();
  fill(200);
  noStroke();
  textureMode(IMAGE);  
  texturedSphere(globeRadius, texmap);
}

void initializeSphere(int res) {
  sinLUT = new float[SINCOS_LENGTH];
  cosLUT = new float[SINCOS_LENGTH];

  for (int i = 0; i < SINCOS_LENGTH; i++) {
    sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
    cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
  }

  float delta = (float)SINCOS_LENGTH/res;
  float[] cx = new float[res];
  float[] cz = new float[res];

  // Calc unit circle in XZ plane
  for (int i = 0; i < res; i++) {
    cx[i] = -cosLUT[(int) (i*delta) % SINCOS_LENGTH];
    cz[i] = sinLUT[(int) (i*delta) % SINCOS_LENGTH];
  }

  // Computing vertexlist vertexlist starts at south pole
  int vertCount = res * (res-1) + 2;
  int currVert = 0;

  // Re-init arrays to store vertices
  sphereX = new float[vertCount];
  sphereY = new float[vertCount];
  sphereZ = new float[vertCount];
  float angle_step = (SINCOS_LENGTH*0.5f)/res;
  float angle = angle_step;

  // Step along Y axis
  for (int i = 1; i < res; i++) {
    float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
    float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
    for (int j = 0; j < res; j++) {
      sphereX[currVert] = cx[j] * curradius;
      sphereY[currVert] = currY;
      sphereZ[currVert++] = cz[j] * curradius;
    }
    angle += angle_step;
  }
  sDetail = res;
}

// Generic routine to draw textured sphere
void texturedSphere(float r, PImage t) {
  int v1, v11, v2;
  r = (r + 240 ) * 0.33;
  beginShape(TRIANGLE_STRIP);
  texture(t);
  float iu=(float)(t.width-1)/(sDetail);
  float iv=(float)(t.height-1)/(sDetail);
  float u=0, v=iv;
  for (int i = 0; i < sDetail; i++) {
    vertex(0, -r, 0, u, 0);
    vertex(sphereX[i]*r, sphereY[i]*r, sphereZ[i]*r, u, v);
    u+=iu;
  }
  vertex(0, -r, 0, u, 0);
  vertex(sphereX[0]*r, sphereY[0]*r, sphereZ[0]*r, u, v);
  endShape();   

  // Middle rings
  int voff = 0;
  for (int i = 2; i < sDetail; i++) {
    v1=v11=voff;
    voff += sDetail;
    v2=voff;
    u=0;
    beginShape(TRIANGLE_STRIP);
    texture(t);
    for (int j = 0; j < sDetail; j++) {
      vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1++]*r, u, v);
      vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2++]*r, u, v+iv);
      u+=iu;
    }

    // Close each ring
    v1=v11;
    v2=voff;
    vertex(sphereX[v1]*r, sphereY[v1]*r, sphereZ[v1]*r, u, v);
    vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v+iv);
    endShape();
    v+=iv;
  }
  u=0;

  // Add the northern cap
  beginShape(TRIANGLE_STRIP);
  texture(t);
  for (int i = 0; i < sDetail; i++) {
    v2 = voff + i;
    vertex(sphereX[v2]*r, sphereY[v2]*r, sphereZ[v2]*r, u, v);
    vertex(0, r, 0, u, v+iv);    
    u+=iu;
  }
  vertex(sphereX[voff]*r, sphereY[voff]*r, sphereZ[voff]*r, u, v);
  endShape();
}

void openSpaceNavigator() {
  println(System.getProperty("os.name"));
  control = ControlIO.getInstance(this);  
  String os = System.getProperty("os.name").toLowerCase();  
  if (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0)
    device = control.getDevice("3Dconnexion SpaceNavigator");// magic name for linux    
  else
    device = control.getDevice("SpaceNavigator");//magic name, for windows
  if (device == null) {
    println("No suitable device configured");
    System.exit(-1); // End the program NOW!
  }
  //device.setTolerance(5.00f);
  sliderXpos = device.getSlider(0);
  sliderYpos = device.getSlider(1);
  sliderZpos = device.getSlider(2);
  sliderXrot = device.getSlider(3);
  sliderYrot = device.getSlider(4);
  sliderZrot = device.getSlider(5);
  //button1 = device.getButton(0);
  //button2 = device.getButton(1);
}
