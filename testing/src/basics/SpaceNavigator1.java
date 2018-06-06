package basics;

import frames.core.Frame;
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
public class SpaceNavigator1 extends PApplet {
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
  Shape[] shapes;
  Frame snTrackedFrame;
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
    // set the eye as the space navigator default frame
    snTrackedFrame = scene.eye();
    // mouseShapes
    shapes = new Shape[50];
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
    if (snPicking)
      spaceNavigatorPicking();
    else {
      scene.castOnMouseMove();
      spaceNavigatorInteraction();
    }
  }

  void spaceNavigatorInteraction() {
    //scene.translate(10 * snXPos.getValue(), 10 * snYPos.getValue(), -10 * snZPos.getValue(), snTrackedFrame);
    //scene.rotate(snXRot.getValue() * 10 * PI / width, snYRot.getValue() * 10 * PI / width, snZRot.getValue() * 10 * PI / width, snTrackedFrame);
    println(snXRot.getValue());
    // eye in '1st person'
    //scene.translate(10*snXPos.getValue(),0,0,snTrackedFrame);//eye inv
    //scene.translate(0,10*snYPos.getValue(),0,snTrackedFrame);//eye inv
    //scene.translate(0,0,10*snZPos.getValue(),snTrackedFrame);//eye inv
    scene.rotate(snXRot.getValue() * 10 * PI / width, 0, 0, snTrackedFrame);
    //scene.rotate(0,snYRot.getValue()*10*PI/width,0,snTrackedFrame);
    //scene.rotate(0,0,snZRot.getValue()*10*PI/width,snTrackedFrame);
  }

  // TODO: key! Try to do this in terms of scene.mouseCast() instead!
  void spaceNavigatorPicking() {
    float x = map(snXPos.getValue(), -.8f, .8f, 0, width);
    float y = map(snYPos.getValue(), -.8f, .8f, 0, height);
    // frames' drawing + space navigator picking
    Frame frame = scene.cast(x, y);
    snTrackedFrame = frame == null ? scene.eye() : frame;
    // draw picking visual hint
    pushStyle();
    strokeWeight(3);
    stroke(0, 255, 0);
    scene.drawCross(x, y, 30);
    popStyle();
  }

  public void mouseDragged() {
    if (mouseButton == LEFT)
      scene.mouseSpin();
    else if (mouseButton == RIGHT)
      scene.mouseTranslate();
    else
      scene.scale(mouseX - pmouseX);
  }

  public void mouseWheel(MouseEvent event) {
    scene.zoom(event.getCount() * 20);
  }

  public void keyPressed() {
    if (key == 'r')
      scene.setRightHanded();
    if (key == 'l')
      scene.setLeftHanded();
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
    PApplet.main(new String[]{"basics.SpaceNavigator1"});
  }
}
