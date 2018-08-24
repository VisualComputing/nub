class Eye extends Shape {
  public Eye(Scene scene) {
    super(scene);
  }

  @Override
  void setGraphics(PGraphics pGraphics) {
    pGraphics.fill(0, 255, 0);
    pGraphics.stroke(0, 0, 255);
    pGraphics.strokeWeight(2);
    minimap.drawEye(pGraphics, scene, true);
  }
}
