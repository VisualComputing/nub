package frame;

import common.InteractiveNode;
import processing.core.PApplet;
import proscene.core.Node;
import proscene.primitives.Quaternion;
import proscene.primitives.Vector;
import proscene.processing.Scene;

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
    setPosition();
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
    if (iFrame.grabsInput(scene.mouseAgent()))
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

  public void setPosition() {
    float low = -100;
    float high = 100;
    iFrame.setPosition(new Vector(scene.pApplet().random(low, high), scene.pApplet().random(low, high), scene.pApplet().random(low, high)));
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