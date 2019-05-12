class Sphere extends Node {
  float _radius;
  int _color;

  public Sphere(Scene scene, int tint, float radius) {
    super(scene);
    _color = tint;
    _radius = radius;
  }

  @Override
  public void graphics(PGraphics pg) {
    pg.pushStyle();
    if (drawAxes)
      Scene.drawAxes(pg, _radius * 1.3);
    pg.noStroke();
    pg.fill(isTracked() ? color(255, 0, 0) : _color);
    pg.sphere(isTracked() ? _radius * 1.2 : _radius);
    pg.stroke(255);
    if (bullseye)
      scene.drawBullsEye(this);
    pg.popStyle();
  }
}
