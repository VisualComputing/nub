class Sphere {
  Frame iFrame;
  float r;
  int c;

  public Sphere() {
    iFrame = new Frame(scene) {
      // note that within visit() geometry is defined
      // at the frame local coordinate system
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        if (drawAxes)
          //DrawingUtils.drawAxes(parent, radius()*1.3f);
          Scene.drawAxes(pg, radius() * 1.3f);
        pg.noStroke();
        if (iFrame.isTracked()) {
          pg.fill(255, 0, 0);
          pg.sphere(radius() * 1.2f);
        } else {
          pg.fill(getColor());
          pg.sphere(radius());
        }
        pg.stroke(255);
        if (drawShooterTarget)
          scene.drawShooterTarget(iFrame);
        pg.popStyle();
        return true;
      }
    };
    setRadius(10);
  }

  public float radius() {
    return r;
  }

  public void setRadius(float myR) {
    r = myR;
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
