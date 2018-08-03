class Torus extends Shape {
  public Torus(Scene scene) {
    super(scene);
  }
  
  public Torus(Frame frame) {
    super(frame);
  }

  @Override
  protected void setGraphics(PGraphics pGraphics) {
    pGraphics.fill(graph().pApplet().random(255), graph().pApplet().random(255), graph().pApplet().random(255), graph().pApplet().random(255));
    Scene.drawTorusSolenoid(pGraphics, 6, 8);
  }
}
