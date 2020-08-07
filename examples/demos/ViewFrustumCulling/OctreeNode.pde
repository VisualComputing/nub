class OctreeNode extends Node {
  OctreeNode() {
    tagging = false;
    // register the visit method only at the main scene
    setVisit(mainScene);
  }

  OctreeNode(OctreeNode node, Vector vector) {
    super(node);
    scale(0.5);
    translate(Vector.multiply(vector, scaling() / 2));
    tagging = false;
    // register the visit method only at the main scene
    setVisit(mainScene);
  }

  float level() {
    return 1 - log(magnitude()) / log(2);
  }

  @Override
  public void graphics(PGraphics pg) {
    float level = level();
    pg.stroke(color(0.3 * level * 255, 0.2 * 255, (1 - 0.3 * level) * 255));
    pg.strokeWeight(pow(2, levels - 1));
    pg.noFill();
    pg.box(a, b, c);
  }

  // The culling method is called just before the graphics(PGraphics) method
  @Override
  public void visit(Graph graph) {
    switch (graph.boxVisibility(worldLocation(new Vector(-a / 2, -b / 2, -c / 2)),
      worldLocation(new Vector(a / 2, b / 2, c / 2)))) {
    case VISIBLE:
      for (Node child : children())
        child.cull = true;
      break;
    case SEMIVISIBLE:
      if (!children().isEmpty()) {
        // don't render the node...
        bypass();
        // ... but don't cull its children either
        for (Node child : children())
          child.cull = false;
      }
      break;
    case INVISIBLE:
      cull = true;
      break;
    }
  }
}
