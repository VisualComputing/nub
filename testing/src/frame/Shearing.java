package frame;

import processing.core.*;
import processing.event.Event;
import remixlab.bias.BogusEvent;
import remixlab.bias.event.MotionEvent;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.MouseAgent;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class Shearing extends PApplet {
  Scene scene;
  InteractiveFrame frame1;
  PMatrix3D matrix;

  //Choose one of P3D for a 3D scene, or P2D or JAVA2D for a 2D scene
  String renderer = P3D;

  public void settings() {
    size(700, 700, renderer);
  }

  public void setup() {
    scene = new Scene(this);
    scene.eyeFrame().setDamping(0);
    scene.setPickingVisualHint(true);

    //frame 1
    frame1 = new InteractiveFrame(scene);
    frame1.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    frame1.setGrabsInputThreshold(scene.radius() / 4);
    //frame1.translate(50, 50);

    float a = 0.7f;
    float b = 0.4f;

    matrix = new PMatrix3D(
        1, 0, a, 0,
        0, 1, b, 0,
        0, 0, 1, 0,
        0, 0, 0, 1
    );
  }

  public void draw() {
    background(34,34,34);
    strokeWeight(2);
    stroke(0,200,200);
    fill(0,255,255);
    box(50);
    frame1.applyTransformation();//very efficient
    // Draw the axes
    scene.drawAxes(80);
    applyMatrix(matrix);
    stroke(0,0,255);
    fill(0,255,255,125);
    box(50);
  }

  public static void main(String args[]) {
    PApplet.main(new String[]{"frame.Shearing"});
  }
}