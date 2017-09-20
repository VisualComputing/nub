package frame;

import processing.core.PApplet;
import remixlab.dandelion.geom.Vec;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

/**
 * Created by pierre on 11/15/16.
 */
public class Sphere {
  Scene scene;
  PApplet parent;
  InteractiveFrame iFrame;
  float r;
  int c;

  public Sphere(Scene scn, InteractiveFrame iF) {
    scene = scn;
    parent = scn.pApplet();
    iFrame = iF;
    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    setRadius(10);
  }

  public Sphere(Scene scn) {
    scene = scn;
    parent = scn.pApplet();
    iFrame = new InteractiveFrame(scn);
    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    setRadius(10);
  }

  public void draw() {
    draw(true);
  }

  public void draw(boolean drawAxes) {
    parent.pushMatrix();
    //iFrame.applyTransformation(parent);
    iFrame.applyTransformation(scene);

    if (drawAxes)
      //DrawingUtils.drawAxes(parent, radius()*1.3f);
      scene.drawAxes(radius() * 1.3f);
    if (iFrame.grabsInput(scene.motionAgent())) {
      parent.fill(255, 0, 0);
      parent.sphere(radius() * 1.2f);
    } else {
      parent.fill(getColor());
      parent.sphere(radius());
    }
    parent.popMatrix();
  }

  public float radius() {
    return r;
  }

  public void setRadius(float myR) {
    r = myR;
    iFrame.setGrabsInputThreshold(2 * r);
  }

  public int getColor() {
    return c;
  }

  public void setColor() {
    c = parent.color(parent.random(0, 255), parent.random(0, 255), parent.random(0, 255));
  }

  public void setColor(int myC) {
    c = myC;
  }

  public void setPosition(Vec pos) {
    iFrame.setPosition(pos);
  }

  public Vec getPosition() {
    return iFrame.position();
  }
}
