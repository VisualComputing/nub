/**
 * Box. 
 * by Jean Pierre Charalambos.
 * 
 * This class is part of the Mouse Grabber example.
 *
 * Any object that needs to be "pickable" (such as the Box), should be
 * attached to its own InteractiveFrame. That's all there is to it.
 *
 * The built-in picking proscene mechanism actually works as follows. At
 * instantiation time all InteractiveFrame objects are added to a mouse
 * grabber pool. Scene parses this pool every frame to check if the mouse
 * grabs a InteractiveFrame by projecting its origin onto the screen.
 * If the mouse position is close enough to that projection (default
 * implementation defines a 10x10 pixel square centered at it), the object
 * will be picked. 
 *
 * Override InteractiveFrame.checkIfGrabsInput if you need a more
 * sophisticated picking mechanism.
 *
 * Observe that this class is used among many examples, such as MouseGrabber
 * CajasOrientadas, PointUnderPixel and ScreenDrawing. Hence, it's quite
 * complete, but its functionality is not totally exploited by this example.
 */

public class Box {
  Scene scene;
  public InteractiveFrame iFrame;
  float w, h, d;
  int c;

  public Box(Scene scn) {
    scene = scn;
    iFrame = new InteractiveFrame(scn);
    iFrame.setPickingPrecision(InteractiveFrame.PickingPrecision.ADAPTIVE);
    if(customBindings)
      setCustomBindings(iFrame);
    setSize();
    setColor();    
    setPosition();
  }

  public void draw() {
    draw(false);
  }

  public void draw(boolean drawAxes) {
    pushMatrix();
    iFrame.applyWorldTransformation();
    if (drawAxes)
      scene.drawAxes(max(w, h, d)*1.3f);
    noStroke();
    if (scene.motionAgent().isInputGrabber(iFrame))
      fill(255, 0, 0);
    else
      fill(getColor());
    box(w, h, d);
    popMatrix();
  }

  public void setSize() {
    w = random(10, 40);
    h = random(10, 40);
    d = random(10, 40);
    iFrame.setGrabsInputThreshold(max(w,h,d));
  }

  public void setSize(float myW, float myH, float myD) {
    w=myW; 
    h=myH; 
    d=myD;
    iFrame.setGrabsInputThreshold(max(w,h,d));
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

  public Vec getPosition() {
    return iFrame.position();
  }  

  public void setPosition() {
    float low = -100;
    float high = 100;
    iFrame.setPosition(new Vec(random(low, high), random(low, high), random(low, high)));
  }

  public void setPosition(Vec pos) {
    iFrame.setPosition(pos);
  }

  public Rotation getOrientation() {
    return iFrame.orientation();
  }

  public void setOrientation(Vec v) {
    Vec to = Vec.subtract(v, iFrame.position()); 
    iFrame.setOrientation(new Quat(new Vec(0, 1, 0), to));
  }
}
