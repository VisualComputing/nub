class Box extends Frame {
  float w, h, d;
  int c;

  Box(color c) {
    setPrecision(Frame.Precision.FIXED);
    setPrecisionThreshold(25);
    setSize();
    setColor(c);
    randomize(new Vector(), 200, g.is3D());
  }

  // detached-nub drawing require to
  // manually apply the node transformation
  void draw() {
    pushStyle();
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
    stroke(255);
    if (drawShooterTarget)
      scene.drawShooterTarget(this);
    popStyle();
  }

  void setSize() {
    w = CajasOrientadas.this.random(10, 40);
    h = CajasOrientadas.this.random(10, 40);
    d = CajasOrientadas.this.random(10, 40);
    setPrecisionThreshold(max(w, h, d));
  }

  void setSize(float myW, float myH, float myD) {
    w = myW;
    h = myH;
    d = myD;
  }

  void setColor(int myC) {
    c = myC;
  }

  void setOrientation(Vector v) {
    Vector to = Vector.subtract(v, position());
    setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}
