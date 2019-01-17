public class Box {
  public Frame iFrame;
  float w, h, d;
  int c;

  public Box() {
    iFrame = new Frame(scene) {
      // note that within render() geometry is defined
      // at the frame local coordinate system
      @Override
      public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        updateOrientation(esfera.getPosition());
        if (drawAxes)
          Scene.drawAxes(pg, PApplet.max(w, h, d) * 1.3f);
        pg.noStroke();
        if (iFrame.isTracked())
          pg.fill(255, 0, 0);
        else
          pg.fill(getColor());
        pg.box(w, h, d);
        pg.stroke(255);
        if (drawShooterTarget)
          scene.drawShooterTarget(iFrame);
        pg.popStyle();
        return true;
      }
    };
    setSize();
    setColor();
    iFrame.randomize();
  }

  public void setSize() {
    w = random(10, 40);
    h = random(10, 40);
    d = random(10, 40);
    iFrame.setPickingThreshold(PApplet.max(w, h, d)/scene.radius());
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

  public void updateOrientation(Vector v) {
    Vector to = Vector.subtract(v, iFrame.position());
    iFrame.setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}
