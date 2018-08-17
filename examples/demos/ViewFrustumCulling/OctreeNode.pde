class OctreeNode {
  Vector p1, p2;
  OctreeNode child[];
  int level;

  OctreeNode(Vector P1, Vector P2) {
    p1 = P1;
    p2 = P2;
    child = new OctreeNode[8];
  }

  void draw(PGraphics pg) {
    pg.stroke(color(0.3f * level * 255, 0.2f * 255, (1.0f - 0.3f * level) * 255));
    pg.strokeWeight(level + 1);

    pg.beginShape();
    pg.vertex(p1.x(), p1.y(), p1.z());
    pg.vertex(p1.x(), p2.y(), p1.z());
    pg.vertex(p2.x(), p2.y(), p1.z());
    pg.vertex(p2.x(), p1.y(), p1.z());
    pg.vertex(p1.x(), p1.y(), p1.z());
    pg.vertex(p1.x(), p1.y(), p2.z());
    pg.vertex(p1.x(), p2.y(), p2.z());
    pg.vertex(p2.x(), p2.y(), p2.z());
    pg.vertex(p2.x(), p1.y(), p2.z());
    pg.vertex(p1.x(), p1.y(), p2.z());
    pg.endShape();

    pg.beginShape(PApplet.LINES);
    pg.vertex(p1.x(), p2.y(), p1.z());
    pg.vertex(p1.x(), p2.y(), p2.z());
    pg.vertex(p2.x(), p2.y(), p1.z());
    pg.vertex(p2.x(), p2.y(), p2.z());
    pg.vertex(p2.x(), p1.y(), p1.z());
    pg.vertex(p2.x(), p1.y(), p2.z());
    pg.endShape();
  }

  void drawIfAllChildrenAreVisible(PGraphics pg, Graph camera) {
    Graph.Visibility vis = camera.boxVisibility(p1, p2);
    if (vis == Graph.Visibility.VISIBLE)
      draw(pg);
    else if (vis == Graph.Visibility.SEMIVISIBLE)
      if (child[0] != null)
        for (int i = 0; i < 8; ++i)
          child[i].drawIfAllChildrenAreVisible(pg, camera);
      else
        draw(pg);
  }

  void buildBoxHierarchy(int l) {
    level = l;
    Vector middle = Vector.multiply(Vector.add(p1, p2), 1 / 2.0f);
    for (int i = 0; i < 8; ++i) {
      // point in one of the 8 box corners
      Vector point = new Vector(((i & 4) != 0) ? p1.x() : p2.x(), ((i & 2) != 0) ? p1.y() : p2.y(), ((i & 1) != 0) ? p1.z() : p2.z());
      if (level > 0) {
        child[i] = new OctreeNode(point, middle);
        child[i].buildBoxHierarchy(level - 1);
      } else
        child[i] = null;
    }
  }
}
