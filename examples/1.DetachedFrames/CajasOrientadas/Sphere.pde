public class Sphere extends Frame {
  float r;
  int c;

  public Sphere() {
    setPrecision(Frame.Precision.ADAPTIVE);
    setRadius(10);
  }

  public void draw() {
    draw(true);
  }

  public void draw(boolean drawAxes) {
    pushMatrix();
    scene.applyTransformation(this);

    if (drawAxes)
      //DrawingUtils.drawAxes(parent, radius()*1.3f);
      scene.drawAxes(radius() * 1.3f);
    
    fill(255, isTracked(scene) ? 0 : 255, 0);
    sphere(radius() * 1.2f);
 
    popMatrix();
  }

  public float radius() {
    return r;
  }

  public void setRadius(float myR) {
    r = myR;
    setPrecisionThreshold(2 * r);
  }
}
