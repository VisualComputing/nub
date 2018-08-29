public class Box {
  public Frame iFrame;
  float w, h, d;
  int c;

  public Box() {
    iFrame = new Frame(scene) {
      // note that within visit() geometry is defined
      // at the frame local coordinate system
      @Override
      public void visit() {
        draw();
      }
    };
    iFrame.setPrecision(Frame.Precision.ADAPTIVE);
    iFrame.setPrecisionThreshold(25);
    setSize();
    setColor();
    iFrame.randomize();
  }

  public void draw() {
    pushStyle();
    setOrientation(esfera.getPosition());
    if (drawAxes)
      scene.drawAxes(PApplet.max(w, h, d) * 1.3f);
    noStroke();
    if (iFrame.isTracked())
      fill(255, 0, 0);
    else
      fill(getColor());
    box(w, h, d);
    stroke(255);
    if (drawShooterTarget)
      scene.drawShooterTarget(iFrame);
    popStyle();
  }

  public void setSize() {
    w = random(10, 40);
    h = random(10, 40);
    d = random(10, 40);
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
    c = color(random(0, 255), random(0, 255), random(0, 255));
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
    return iFrame.orientation();
  }

  public void setOrientation(Vector v) {
    Vector to = Vector.subtract(v, iFrame.position());
    iFrame.setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}
