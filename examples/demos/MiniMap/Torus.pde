class Torus extends Shape {
  color _color;
  public Torus(Scene scene, color rgb) {
    super(scene);
    _color = rgb;
  }
  
  public Torus(Frame frame, color rgb) {
    super(frame);
    _color = rgb;
  }

  @Override
  void setGraphics(PGraphics pGraphics) {
    pGraphics.fill(_color);
    Scene.drawTorusSolenoid(pGraphics, 6, 8);
  }
}
