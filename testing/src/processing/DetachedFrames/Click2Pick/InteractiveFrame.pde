class InteractiveFrame extends Frame {
  int _c;
  Vector pnt;

  InteractiveFrame(int colour) {
    super();
    _c = colour;
    pnt = new Vector(40, 30, 20);
  }

  InteractiveFrame(Frame node, int colour) {
    super(node);
    _c = colour;
    pnt = new Vector(40, 30, 20);
  }

  void draw(Scene scn) {
    pushStyle();
    scn.drawAxes(40);
    stroke(_c);
    scn.drawShooterTarget(this);
    strokeWeight(10);
    point(pnt.x(), pnt.y(), pnt.z());
    popStyle();
  }
}
