public class InteractiveTorus {
  InteractiveFrame iFrame;
  int c;
  Scene scene;

  InteractiveTorus(Scene scn) {
    scene = scn;    
    iFrame = new InteractiveFrame(scene);
    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    iFrame.setGrabsInputThreshold(scene.radius()/4);
    setColor();
    setPosition();
  }

  // don't draw local axis
  public void draw() {
    draw(false);
  }

  public void draw(boolean drawAxes) {
    pushMatrix();
    pushStyle();
    // Multiply matrix to get in the frame coordinate system.
    //applyMatrix(Scene.toPMatrix(iFrame.matrix())); // is handy but inefficient
    iFrame.applyTransformation(); // optimum
    if (drawAxes)
      scene.drawAxes(20 * 1.3f);
    noStroke();

    fill(255, 0, 0);

    if (iFrame.grabsInput())
      fill(255, 0, 0);
    else
      fill(getColor());
    
    scene.drawTorusSolenoid();
    popStyle();
    popMatrix();
  }

  public int getColor() {
    return c;
  }

  // sets color randomly
  public void setColor() {
    c = color(random(0, 255), random(0, 255), random(0, 255), random(100, 200));
  }

  public void setColor(int myC) {
    c = myC;
  }

  public Vec getPosition() {
    return iFrame.position();
  }

  // sets position randomly
  public void setPosition() {
    float low = -100;
    float high = 100;
    Vec pos = scene.is3D() ? new Vec(random(low, high), random(low, high), random(low, high)) 
                           : new Vec(random(low, high), random(low, high));
    iFrame.setPosition(pos);
  }

  public void setPosition(Vec pos) {
    iFrame.setPosition(pos);
  }

  public Rotation getOrientation() {
    return iFrame.orientation();
  }
}
