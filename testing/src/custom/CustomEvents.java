package custom;

import processing.core.PApplet;
import remixlab.bias.BogusEvent;
import remixlab.bias.Shortcut;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/24/16.
 */
public class CustomEvents extends PApplet {
  Scene scene;
  InteractiveFrame iFrame;
  //String renderer = JAVA2D;
  //String renderer = P2D;
  String renderer = P3D;
  WeirdAgent weirdAgent;
  SimpleAgent simpleAgent;

  public void settings() {
    size(600, 600, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    scene.disableMouseAgent();

    weirdAgent = new WeirdAgent(scene);
    //simpleAgent = new SimpleAgent(scene);

    iFrame = new InteractiveFrame(scene);
    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    iFrame.setGrabsInputThreshold(scene.radius() / 4);
    iFrame.translate(50, 50);

    scene.eyeFrame().setBinding(new WeirdShortcut(BogusEvent.NO_MODIFIER_MASK, WeirdAgent.WEIRD_ID), "weirdBehavior");
    //scene.eyeFrame().setBinding(new Shortcut(BogusEvent.NO_MODIFIER_MASK, SimpleAgent.SIMPLE_ID), "simpleBehavior");
  }

  void drawGeom() {
    if (scene.is2D())
      rect(0, 0, 30, 30, 7);
    else
      box(30);
  }

  public void weirdBehavior(InteractiveFrame frame, WeirdEvent event) {
    //println(event.x + " " + event.y);
    frame.translate(event.x/100, event.y/100);
  }

  public void simpleBehavior(InteractiveFrame frame, BogusEvent event) {
    println("Simple behavior Arrived!");
  }

  //checkIfGrabsInput example

  public boolean checkIfGrabsInput(InteractiveFrame frame, WeirdEvent event) {
    Vec proj = scene.eye().projectedCoordinatesOf(frame.position());
    float halfThreshold = 50;
    boolean result = ((Math.abs(event.x - proj.vec[0]) < halfThreshold) && (Math.abs(event.y - proj.vec[1]) < halfThreshold));
    if(result)
      println("frame selected");
    return result;
  }

  public void draw() {
    background(0);
    fill(204, 102, 0, 150);
    drawGeom();

    // Save the current model view matrix
    pushMatrix();
    // Multiply matrix to get in the frame coordinate system.
    // applyMatrix(Scene.toPMatrix(iFrame.matrix())); //is possible but inefficient
    iFrame.applyTransformation();//very efficient
    // Draw an axis using the Scene static function
    scene.drawAxes(20);

    // Draw a second torus
    if (scene.motionAgent().defaultGrabber() == iFrame) {
      fill(0, 255, 255);
      drawGeom();
    } else if (iFrame.grabsInput()) {
      fill(255, 0, 0);
      drawGeom();
    } else {
      fill(0, 0, 255, 150);
      drawGeom();
    }
    popMatrix();
  }

  public void keyPressed() {
    if (key == 'i')
      scene.inputHandler().shiftDefaultGrabber(scene.eyeFrame(), iFrame);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"custom.CustomEvents"});
  }
}