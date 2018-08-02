class InteractiveFrame extends Frame {
  int _c;
  Vector pnt;
  Scene scene;

  InteractiveFrame(Scene graph, int colour) {
    super(graph);
    scene = (Scene) graph();
    _c = colour;
    pnt = new Vector(40, 30, 20);
  }

  InteractiveFrame(Frame frame, int colour) {
    super(frame);
    scene = (Scene) graph();
    _c = colour;
    pnt = new Vector(40, 30, 20);
  }

  @Override
  public void visit() {
    pushStyle();
    scene.drawAxes(40);
    stroke(_c);
    scene.drawShooterTarget(this);
    strokeWeight(10);
    point(pnt.x(), pnt.y(), pnt.z());
    popStyle();
  }
}
