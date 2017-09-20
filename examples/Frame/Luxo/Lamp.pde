/**
 * Lamp by Jean Pierre Charalambos.
 * 
 * This class is part of the Luxo example.
 *
 * Add a graphics handler to an InteractiveFrame to automatically pick
 * an object. The object is described in the graphics handler procedure.
 *
 * Press 'h' to toggle the mouse and keyboard navigation help.
 */

public class Lamp {
  Scene scene;
  InteractiveFrame [] frameArray;

  Camera cam;

  Lamp(Scene s) {
    scene =  s;
    frameArray = new InteractiveFrame[4];
    
    for (int i = 0; i < 4; ++i) {
      frameArray[i] = new InteractiveFrame(scene, i>0 ? frameArray[i-1] : null);
      frame(i).setHighlightingMode(InteractiveFrame.HighlightingMode.FRONT_SHAPE);
    }

    // Initialize frames
    frame(1).setTranslation(0, 0, 8); // Base height
    frame(2).setTranslation(0, 0, 50);  // Arm length
    frame(3).setTranslation(0, 0, 50);  // Arm length

    frame(1).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), 0.6f));
    frame(2).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), -2.0f));
    frame(3).setRotation(new Quat(new Vec(1.0f, -0.3f, 0.0f), -1.7f));
    
    //graphics handers
    frame(0).setShape(this, "drawBase");
    frame(1).setShape(this, "drawPivotArm");
    frame(2).setShape(this, "drawPivotArm");
    frame(3).setShape(this, "drawHead");

    // Set frame constraints
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, new Vec(0.0f, 0.0f, 1.0f));
    baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(0.0f, 0.0f, 1.0f));
    frame(0).setConstraint(baseConstraint);

    LocalConstraint XAxis = new LocalConstraint();
    XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    XAxis.setRotationConstraint   (AxisPlaneConstraint.Type.AXIS, new Vec(1.0f, 0.0f, 0.0f));
    frame(1).setConstraint(XAxis);
    frame(2).setConstraint(XAxis);

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    frame(3).setConstraint(headConstraint);
  }
  
  public void drawBase(InteractiveFrame iFrame, PGraphics pg) {
  	pg.fill(iFrame.grabsInput() ? 255 : 0, 0, 255);
    drawCone(pg, 0, 3, 15, 15, 30);
    drawCone(pg, 3, 5, 15, 13, 30);
    drawCone(pg, 5, 7, 13, 1, 30);
    drawCone(pg, 7, 9, 1, 1, 10);
  }
  
  public void drawPivotArm(PGraphics pg) {
    drawCylinder(pg);
    drawArm(pg);
  }
  
  public void drawHead(InteractiveFrame iFrame, PGraphics pg) {
  	pg.fill(0, 255, iFrame.grabsInput() ? 0 : 255);
    drawCone(pg, -2, 6, 4, 4, 30);
    drawCone(pg, 6, 15, 4, 17, 30);
    drawCone(pg, 15, 17, 17, 17, 30);
    pg.spotLight(155, 255, 255, 0, 0, 0, 0, 0, 1, THIRD_PI, 1);
  }

  public void drawArm(PGraphics pg) {
    pg.translate(2, 0, 0);
    drawCone(pg, 0, 50, 1, 1, 10);
    pg.translate(-4, 0, 0);  
    drawCone(pg, 0, 50, 1, 1, 10);    
    pg.translate(2, 0, 0);
  }

  public void drawCylinder(PGraphics pg) {
    pg.pushMatrix();
    pg.rotate(HALF_PI, 0, 1, 0);
    drawCone(pg, -5, 5, 2, 2, 20);
    pg.popMatrix();
  }

  public void drawCone(PGraphics pg, float zMin, float zMax, float r1, float r2, int nbSub) {
    pg.translate(0.0f, 0.0f, zMin);
    Scene.drawCone(pg, nbSub, 0, 0, r1, r2, zMax-zMin);
    pg.translate(0.0f, 0.0f, -zMin);
  }

  public InteractiveFrame frame(int i) {
    return frameArray[i];
  }
}