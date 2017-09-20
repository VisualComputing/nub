public class ArmCam {
  InteractiveFrame[] frameArray;

  ArmCam(int x, int y, float ang) {
    frameArray = new InteractiveFrame[6];
    for (int i = 0; i < 6; ++i)
      frameArray[i] = new InteractiveFrame(mainScene, i>0 ? frameArray[i-1] : null);

    // Initialize frames
    frame(0).setTranslation(x, y, 0);
    frame(1).setTranslation(0, 0, 21);
    frame(1).setRotation(new Quat(new Vec(0.0f, 0.0f, 1.0f), ang));
    frame(2).setTranslation(0, 0, 35);
    frame(3).setTranslation(0, 0, -55);
    frame(4).setTranslation(0, -8, 0);    
    frame(5).setTranslation(0, -3, 12);
    frame(2).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), 1.8f));
    frame(3).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), 0.0f));
    frame(4).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), HALF_PI));
    frame(5).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), HALF_PI));

    // Set frame constraints
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setTranslationConstraint(AxisPlaneConstraint.Type.PLANE, 
    new Vec(0.0f, 0.0f, 1.0f));
    baseConstraint.setRotationConstraint(
    AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 1.0f));
    frame(0).setConstraint(baseConstraint);

    LocalConstraint rotor = new LocalConstraint();
    rotor.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 1.0f));
    rotor.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(0.0f, 0.0f, 1.0f));
    frame(1).setConstraint(rotor);

    LocalConstraint XAxis = new LocalConstraint();
    XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    XAxis.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(1.0f, 0.0f, 0.0f));
    frame(2).setConstraint(XAxis);

    LocalConstraint freeBarMove = new LocalConstraint();
    freeBarMove.setRotationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, 
    new Vec(0.0f, 0.0f, 0.0f));
    freeBarMove.setTranslationConstraint(AxisPlaneConstraint.Type.AXIS, 
    new Vec(0.0f, 0.0f, 1.0f));
    frame(3).setConstraint(freeBarMove);

    LocalConstraint camBase = new LocalConstraint();
    camBase.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, 
    new Vec(0.0f, 0.0f, 1.0f));
    camBase.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, 
    new Vec(0.0f, 0.0f, 1.0f));
    frame(4).setConstraint(camBase);
    frame(4).rotate(new Quat(new Vec(0, 0, 1), -HALF_PI));

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(
    AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    headConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, 
    new Vec(1.0f, 0.0f, 0.0f));
    frame(5).setConstraint(headConstraint);
  }

  public void draw(Scene scn) {
    // Robot arm's local frame
    PGraphics pg3d = scn.pg();

    pg3d.pushMatrix();
    scn.applyTransformation(frame(0));    
    setColor(scn, frame(0).grabsInput(scn.motionAgent()));
    drawTripod(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(1));
    setColor(scn, frame(1).grabsInput(scn.motionAgent()));
    drawBase(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(2));
    setColor(scn, frame(2).grabsInput(scn.motionAgent()));
    drawLongArm(scn);

    pg3d.pushMatrix();

    setColor(scn, frame(3).grabsInput(scn.motionAgent()));

    // here goes the movement constraint that keeps the camera holder
    // attached to the rails
    if (frame(3).localInverseCoordinatesOf(frame(3).coordinatesOf(frame(3).position())).z() < -55)
      frame(3).setTranslation(0, 0, -55);

    if (frame(3).localInverseCoordinatesOf(frame(3).coordinatesOf(frame(3).position())).z() > -4)
      frame(3).setTranslation(0, 0, -4);
    scn.applyTransformation(frame(3));
    drawHolder(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(4));
    setColor(scn, frame(4).grabsInput(scn.motionAgent()));
    drawInvertedBase(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(5));
    setColor(scn, frame(5).grabsInput(scn.motionAgent()));
    drawHead(scn);

    // Add light if the flag enables it
    if (enabledLights) {
      pg3d.spotLight(155, 255, 255, 0, 0, 0, 0, 0, -1, THIRD_PI, 1);
    }

    pg3d.popMatrix();// frame(5)
    pg3d.popMatrix();// frame(4)
    pg3d.popMatrix();// frame(3)
    pg3d.popMatrix();// frame(2)
    pg3d.popMatrix();// frame(1)

    // totally necessary
    pg3d.popMatrix();// frame(0)

    // Scene.drawEye takes into account the whole scene hierarchy above
    // the camera iFrame. Thus, we call it after restoring the gl state.
    // Calling it before the first push matrix above, should do it too.
    if ( drawRobotCamFrustum && scn.equals(mainScene) )
      scn.drawEye( armScene.camera() );
  }

  public void drawBase(Scene scn) {
    PGraphics pg3d = scn.pg();
    drawCone(scn, 0, 3, 7, 7, 4);
    drawCone(scn, 3, 5, 7, 5, 4);
    drawCone(scn, 5, 6, 5, 1, 4);
    pg3d.translate(2, 0, 0);
    drawCone(scn, 0, 36, 1, 1, 10);
    pg3d.translate(-4, 0, 0);
    drawCone(scn, 0, 36, 1, 1, 10);
    pg3d.translate(2, 0, 0);
    pg3d.pushMatrix();
    pg3d.rotate(HALF_PI, 0, 1, 0);
    pg3d.translate(-35, 0, 0);
    drawCone(scn, -3, 3, 2, 2, 20);
    pg3d.popMatrix();
  }

  public void drawArm(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.translate(2, 0, 0);
    drawCone(scn, 0, 50, 1, 1, 10);
    pg3d.translate(-4, 0, 0);
    drawCone(scn, 0, 50, 1, 1, 10);
    pg3d.translate(2, 0, 0);
  }

  public void drawHead(Scene scn) {
    if ( drawRobotCamFrustum && scn.equals( mainScene) )
      scn.drawAxes( armScene.camera().sceneRadius() * 1.2f );
    drawCone(scn, 9, 12, 7, 0, 6);
    drawCone(scn, 8, 9, 6, 7, 6);
    drawCone(scn, 5, 8, 8, 6, 30);
    drawCone(scn, -5, 5, 8, 8, 30);
    drawCone(scn, -8, -5, 6, 8, 30);
    drawCone(scn, -12, -8, 7, 6, 30);
  }

  public void drawCylinder(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.rotate(HALF_PI, 0, 1, 0);
    drawCone(scn, -5, 5, 2, 2, 20);
    pg3d.popMatrix();
  }

  public void drawCone(Scene scn, float zMin, float zMax, float r1, float r2, int nbSub) {
    PGraphics pg3d = scn.pg();
    pg3d.translate(0.0f, 0.0f, zMin);
    scn.drawCone(nbSub, 0, 0, r1, r2, zMax - zMin);
    pg3d.translate(0.0f, 0.0f, -zMin);
  }

  public void setColor(Scene scn, boolean selected) {
    PGraphics pg3d = scn.pg();
    if (selected) {
      pg3d.fill(200, 200, 0);
    } 
    else {
      pg3d.fill(200, 200, 200);
    }
  }

  public void drawInvertedBase(Scene scn) {
    drawCone(scn, -1, 0, 0, 5, 4);
    drawCone(scn, 0, 3, 5, 5, 4);
    drawCone(scn, 3, 5, 5, 3, 4);
    drawCone(scn, 5, 6, 3, 1, 4);
  }

  public void drawLongArm(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.translate(2, 0, -57);
    drawCone(scn, 0, 70, 1, 1, 10);
    pg3d.translate(-4, 0, 0);
    drawCone(scn, 0, 70, 1, 1, 10);
    pg3d.translate(2, 0, 70);
    drawCone(scn, 0, 5, 4, 4, 5);
    drawCone(scn, -1, 0, 0, 4, 5);
    drawCone(scn, 5, 6, 4, 0, 5);
  }

  public void drawHolder(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.translate(0, 0, -13);
    drawCylinder(scn);
    pg3d.pushMatrix();
    pg3d.rotateX(HALF_PI);
    pg3d.translate(2, 0, 0);
    drawCone(scn, 0, 10, 1, 1, 10);
    pg3d.translate(-4, 0, 0);
    drawCone(scn, 0, 10, 1, 1, 10);
    pg3d.translate(2, 0, 0);
    pg3d.popMatrix();
  }

  public void drawTripod(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.translate(0, 0, 21);
    pg3d.rotateX(PI);
    drawStick(scn);
    pg3d.rotateZ((2 * PI) / 3);
    drawStick(scn);
    pg3d.rotateZ(90);
    drawStick(scn);
    pg3d.popMatrix();
  }

  public void drawStick(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.rotateX((float) (PI / 5.5));
    drawCone(scn, 0, 25, 1, 1, 10);
    pg3d.popMatrix();
  }

  public InteractiveFrame frame(int i) {
    return frameArray[i];
  }
}