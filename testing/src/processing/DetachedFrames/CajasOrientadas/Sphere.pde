class Sphere extends Frame {
  float r;
  int c;

  // default detached node constructor
  Sphere() {
    //setPrecision(Frame.Precision.ADAPTIVE);
    setRadius(10);
  }

  // detached-nub drawing require to
  // manually apply the node transformation
  void draw() {
    pushStyle();
    pushMatrix();
    scene.applyTransformation(this);

    if (drawAxes)
      //DrawingUtils.drawAxes(parent, radius()*1.3f);
      scene.drawAxes(radius() * 1.3f);

    noStroke();
    fill(255, isTracked(scene) ? 0 : 255, 0);
    sphere(radius() * 1.2f);

    popMatrix();
    popStyle();
  }

  float radius() {
    return r;
  }

  void setRadius(float myR) {
    r = myR;
    setPrecisionThreshold(2 * r);
  }
}
