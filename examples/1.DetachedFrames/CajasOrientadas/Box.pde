public class Box extends Frame {
  float w, h, d;
  int c;

  public Box(color c) {
    setPrecision(Frame.Precision.ADAPTIVE);
    setPrecisionThreshold(25);
    setSize();
    setColor(c);
    randomize(new Vector(), 200, g.is3D());
  }

  public void draw() {
    draw(false);
  }

  public void draw(boolean drawAxes) {
    pushMatrix();
    scene.applyTransformation(this);
    if (drawAxes)
      scene.drawAxes(max(w, h, d) * 1.3f);
    noStroke();
    if (isTracked(scene))
      fill(255, 0, 0);
    else
      fill(c);
    //Draw a box
    box(w, h, d);
    popMatrix();
  }

  public void setSize() {
    w = CajasOrientadas.this.random(10, 40);
    h = CajasOrientadas.this.random(10, 40);
    d = CajasOrientadas.this.random(10, 40);
    setPrecisionThreshold(max(w, h, d));
  }

  public void setSize(float myW, float myH, float myD) {
    w = myW;
    h = myH;
    d = myD;
  }

  public void setColor(int myC) {
    c = myC;
  }

  public void setOrientation(Vector v) {
    Vector to = Vector.subtract(v, position());
    setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}
