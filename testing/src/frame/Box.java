package frame;

import common.InteractiveNode;
import frames.core.Node;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

/**
 * Created by pierre on 11/15/16.
 */
public class Box {
  Scene scene;
  public Node iFrame;
  float w, h, d;
  int c;

  public Box(Scene scn) {
    scene = scn;
    iFrame = new InteractiveNode(scene);
    iFrame.setPrecision(Node.Precision.ADAPTIVE);
    iFrame.setPrecisionThreshold(25);
    setSize();
    setColor();
    iFrame.randomize();
  }

  public void draw() {
    draw(false);
  }

  public void draw(boolean drawAxes) {
    scene.frontBuffer().pushMatrix();

    /**
     PMatrix3D pM3d =  new PMatrix3D();
     float [] m = new float [16];
     Matrix m3d = iFrame.matrix();
     m = m3d.getTransposed(m);
     pM3d.set(m);
     graph.frontBuffer().applyMatrix(pM3d);
     // */
    //Same as the previous commented lines, but a lot more efficient:
    iFrame.applyWorldTransformation();

    if (drawAxes)
      //DrawingUtils.drawAxes(parent, PApplet.max(w,h,d)*1.3f);
      scene.drawAxes(PApplet.max(w, h, d) * 1.3f);
    scene.frontBuffer().noStroke();
    if (iFrame.grabsInput(scene.mouse()))
      scene.frontBuffer().fill(255, 0, 0);
    else
      scene.frontBuffer().fill(getColor());
    //Draw a box
    scene.frontBuffer().box(w, h, d);

    scene.frontBuffer().popMatrix();
  }

  public void setSize() {
    w = scene.pApplet().random(10, 40);
    h = scene.pApplet().random(10, 40);
    d = scene.pApplet().random(10, 40);
    iFrame.setPrecisionThreshold(PApplet.max(w, h, d));

  }

  public void setSize(float myW, float myH, float myD) {
    w = myW;
    h = myH;
    d = myD;
  }

  public int getColor() {
    return c;
  }

  public void setColor() {
    c = scene.pApplet().color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255));
  }

  public void setColor(int myC) {
    c = myC;
  }

  public Vector getPosition() {
    return iFrame.position();
  }

  public void setPosition(Vector pos) {
    iFrame.setPosition(pos);
  }

  public Quaternion getOrientation() {
    return (Quaternion) iFrame.orientation();
  }

  public void setOrientation(Vector v) {
    Vector to = Vector.subtract(v, iFrame.position());
    iFrame.setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}