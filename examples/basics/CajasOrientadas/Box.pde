public class Box extends Node {
  float _w, _h, _d;
  int _color;

  public Box(int tint, float w, float h, float d) {
    _color = tint;
    _w = w;
    _h = h;
    _d = d;
    setBullsEyeSize(max(_w, _h, _d) / scene.radius());
    scene.randomize(this);
    enableHint(Node.AXES | Node.BULLSEYE);
    setVisit(scene);
  }

  // geometry is defined at the node local coordinate system
  @Override
  public void graphics(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(_color);
    pg.box(_w, _h, _d);
    pg.popStyle();
  }

  @Override
  public void visit(Graph grahp, Node node) {
    Vector to = Vector.subtract(esfera.position(), node.position());
    node.setOrientation(Quaternion.from(Vector.plusJ, to));
  }
}
