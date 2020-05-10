class Sphere extends Node {
  float r;
  int c;

  // default detached node constructor
  Sphere() {
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
    fill(255, isTagged(scene) ? 0 : 255, 0);
    sphere(radius() * 1.2f);

    popMatrix();
    popStyle();
  }

  float radius() {
    return r;
  }

  void setRadius(float myR) {
    r = myR;
  }
}
