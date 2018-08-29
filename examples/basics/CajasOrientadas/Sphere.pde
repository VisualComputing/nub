class Sphere {
  Frame iFrame;
  float r;
  int c;

  public Sphere() {
    iFrame = new Frame(scene) {
      // note that within visit() geometry is defined
      // at the frame local coordinate system
      @Override
      public void visit() {
        draw();
      }
    };
    iFrame.setPrecision(Frame.Precision.ADAPTIVE);
    setRadius(10);
  }

  public void draw() {
    pushStyle();
    if (drawAxes)
      //DrawingUtils.drawAxes(parent, radius()*1.3f);
      scene.drawAxes(radius() * 1.3f);
    noStroke();
    if (iFrame.isTracked()) {
      fill(255, 0, 0);
      sphere(radius() * 1.2f);
    } else {
      fill(getColor());
      sphere(radius());
    }
    stroke(255);
    if (drawShooterTarget)
      scene.drawShooterTarget(iFrame);
    popStyle();
  }

  public float radius() {
    return r;
  }

  public void setRadius(float myR) {
    r = myR;
    iFrame.setPrecisionThreshold(2 * r);
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

  public void setPosition(Vector pos) {
    iFrame.setPosition(pos);
  }

  public Vector getPosition() {
    return iFrame.position();
  }
}
