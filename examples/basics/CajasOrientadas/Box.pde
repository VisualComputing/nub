public class Box extends Node {
  float _w, _h, _d;
  int _color;

  public Box(Scene scene, int tint, float w, float h, float d) {
    super(scene);
    _color = tint;
    _w = w;
    _h = h;
    _d = d;
    setPickingThreshold(PApplet.max(_w, _h, _d)/scene.radius());
    randomize();
  }

  // note that within render() geometry is defined
  // at the node local coordinate system
  @Override
  public boolean graphics(PGraphics pg) {
    pg.pushStyle();
    updateOrientation(esfera.position());
    if (drawAxes)
      Scene.drawAxes(pg, PApplet.max(_w, _h, _d) * 1.3);
    pg.noStroke();
    pg.fill(isTracked() ? color(255, 0, 0) : _color);
    pg.box(_w, _h, _d);
    pg.stroke(255);
    if (drawShooterTarget)
      scene.drawShooterTarget(this);
    pg.popStyle();
    return true;
  }

  public void updateOrientation(Vector v) {
    Vector to = Vector.subtract(v, position());
    setOrientation(new Quaternion(new Vector(0, 1, 0), to));
  }
}
