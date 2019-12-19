class OctreeNode extends Node {
  OctreeNode(Scene scene) {
    super(scene);
    disableTagging();
  }

  OctreeNode(OctreeNode node, Vector vector) {
    super(node);
    scale(0.5);
    translate(Vector.multiply(vector, scaling() / 2));
    disableTagging();
  }

  float level() {
    return (log(1 / magnitude()) / log(2)) - 1;
  }

  @Override
  public void graphics(PGraphics pg) {
    float level = level();
    pg.stroke(color(0.3 * level * 255, 0.2 * 255, (1 - 0.3 * level) * 255));
    pg.strokeWeight(pow(2, levels - 1));
    pg.noFill();
    pg.box(a, b, c);
  }

  // customize traversal
  @Override
  public void visit() {
    // cull only against main scene
    if (graph() != mainScene)
      return;
    switch (graph().boxVisibility(worldLocation(new Vector(-a / 2, -b / 2, -c / 2)),
                                  worldLocation(new Vector(a / 2, b / 2, c / 2)))) {
    case VISIBLE:
      for (Node node : children())
        node.cull();
      break;
    case SEMIVISIBLE:
      if (!children().isEmpty()) {
        // don't render the node...
        bypass();
        // ... but don't cull its children either
        for (Node node : children())
          node.cull(false);
      }
      break;
    case INVISIBLE:
      cull();
      break;
    }
  }
}
