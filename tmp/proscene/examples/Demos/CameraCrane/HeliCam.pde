public class HeliCam {
  InteractiveFrame[] frameArray;
  Quat rotation = new Quat(new Vec(0.0f, 0.0f, 1.0f), 0.3f);

  HeliCam() {
    frameArray = new InteractiveFrame[5];
    for (int i = 0; i < 4; ++i)
      frameArray[i] = new InteractiveFrame(mainScene, i>0 ? frameArray[i-1] : null);

    frameArray[4] = new InteractiveFrame(mainScene);
    // set the propeller's reference frame as the body of the heli
    frame(4).setReferenceFrame(frame(0));

    // Initialize frames
    frame(0).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), HALF_PI));
    frame(0).setTranslation(-25, 56, 80);
    frame(1).setTranslation(0, 0, 6);
    frame(2).setTranslation(0, 0, 15);
    frame(3).setTranslation(0, 0, 15);
    frame(4).setTranslation(0, 6, 0);
    frame(1).setRotation( new Quat(new Vec(1.0f, 0.0f, 0.0f), 0.6f));
    frame(2).setRotation(new Quat(new Vec(1.0f, 0.0f, 0.0f), HALF_PI));
    frame(3).setRotation(new Quat(new Vec(1.0f, -0.3f, 0.0f), 1.7f));
    frame(4).setRotation(new Quat(new Vec(1.0f, -0.0f, 0.0f), -HALF_PI));

    // Set frame constraints
    WorldConstraint baseConstraint = new WorldConstraint();
    baseConstraint.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, 
    new Vec(0.0f, 0.0f, 1.0f));
    frame(0).setConstraint(baseConstraint);

    LocalConstraint XAxis = new LocalConstraint();
    XAxis.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    XAxis.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(1.0f, 0.0f, 0.0f));
    frame(1).setConstraint(XAxis);
    frame(2).setConstraint(XAxis);

    LocalConstraint headConstraint = new LocalConstraint();
    headConstraint.setTranslationConstraint(
    AxisPlaneConstraint.Type.FORBIDDEN, new Vec(0.0f, 0.0f, 0.0f));
    frame(3).setConstraint(headConstraint);

    LocalConstraint rotor = new LocalConstraint();
    rotor.setTranslationConstraint(AxisPlaneConstraint.Type.FORBIDDEN, 
    new Vec(0.0f, 0.0f, 0.0f));
    rotor.setRotationConstraint(AxisPlaneConstraint.Type.AXIS, new Vec(0.0f, 0.0f, 1.0f));
    frame(4).setConstraint(rotor);
    frame(4).setSpinningRotation(rotation);
    mainScene.motionAgent().removeGrabber(frame(4));
  }

  public void draw(Scene scn) {
    // Robot arm's local frame
    PGraphics pg3d = scn.pg();

    pg3d.pushMatrix();
    scn.applyTransformation(frame(0));
    setColor(scn, frame(0).grabsInput(scn.motionAgent()));
    drawBody(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(1));
    setColor(scn, frame(1).grabsInput(scn.motionAgent()));
    drawSmallCylinder(scn);
    drawOneArm(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(2));
    setColor(scn, frame(2).grabsInput(scn.motionAgent()));
    drawSmallCylinder(scn);
    drawOneArm(scn);

    pg3d.pushMatrix();
    scn.applyTransformation(frame(3));
    setColor(scn, frame(3).grabsInput(scn.motionAgent()));
    drawHead(scn);

    // Add light if the flag enables it
    if (enabledLights)
      pg3d.spotLight(155, 255, 255, 0, 0, 0, 0, 0, -1, THIRD_PI, 1);

    pg3d.popMatrix();// frame(3)
    pg3d.popMatrix();// frame(2)
    pg3d.popMatrix();// frame(1)

    // now it is at frame(0), so we draw the propeller

    pg3d.pushMatrix();
    scn.applyTransformation(frame(4));
    setColor(scn, frame(4).grabsInput(scn.motionAgent()));
    drawPropeller(scn);

    pg3d.popMatrix();// frame(4)

    // totally necessary
    pg3d.popMatrix();// frame(0)

    // Scene.drawCamera takes into account the whole scene hierarchy above
    // the camera iFrame. Thus, we call it after restoring the gl state.
    // Calling it before the first push matrix above, should do it too.
    if ( drawRobotCamFrustum && scn.equals(mainScene) )
      scn.drawEye( heliScene.camera() );
  }

  public void drawBody(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.sphere(7);
  }

  public void drawPropeller(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.sphere(2);
    drawCone(scn, 0, 5, 1, 1, 10);
    drawBlade(scn);
    pg3d.rotateZ(HALF_PI);
    drawBlade(scn);
    pg3d.rotateZ(HALF_PI);
    drawBlade(scn);
    pg3d.rotateZ(HALF_PI);
    drawBlade(scn);
    pg3d.translate(0, 0, 5);
    pg3d.sphere(2);
    pg3d.popMatrix();
  }

  public void drawBlade(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.translate(0, 0, 5);
    pg3d.rotateX(HALF_PI);
    drawCone(scn, 0, 12, 1, 2, 2);
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

  public void drawOneArm(Scene scn) {
    drawCone(scn, 0, 15, 1, 1, 10);
  }

  public void drawHead(Scene scn) {
    if (drawRobotCamFrustum && scn.equals(mainScene))
      scn.drawAxes( heliScene.camera().sceneRadius() * 1.2f );
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

  public void drawSmallCylinder(Scene scn) {
    PGraphics pg3d = scn.pg();
    pg3d.pushMatrix();
    pg3d.rotate(HALF_PI, 0, 1, 0);
    drawCone(scn, -2, 2, 2, 2, 20);
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
    if (selected)
      pg3d.fill(200, 200, 0);
    else
      pg3d.fill(200, 200, 200);
  }

  public InteractiveFrame frame(int i) {
    return frameArray[i];
  }
}