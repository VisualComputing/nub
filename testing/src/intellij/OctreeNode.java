package intellij;

import nub.core.Graph;
import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

public class OctreeNode extends Node {
  Graph.Visibility visibility;
  Scene scn;

  OctreeNode(Scene s) {
    super();
    scn = s;
    disableTagging();
  }

  OctreeNode(Scene s, OctreeNode node, Vector vector) {
    super(node);
    scn = s;
    scale(0.5f);
    translate(Vector.multiply(vector, scaling() / 2));
    disableTagging();
  }

  float level() {
    return 1 - PApplet.log(magnitude()) / PApplet.log(2);
  }

  @Override
  public void graphics(PGraphics pg) {
    float level = level();
    pg.stroke(pg.color(0.3f * level * 255, 0.2f * 255, (1.0f - 0.3f * level) * 255));
    pg.strokeWeight(PApplet.pow(2, ViewFrustumCulling.levels - 1));
    pg.noFill();
    pg.box(ViewFrustumCulling.a, ViewFrustumCulling.b, ViewFrustumCulling.c);
  }

  // customize traversal
  @Override
  public void visit() {
    if (scn != ViewFrustumCulling.scene1)
      return;
    visibility = scn.boxVisibility(worldLocation(new Vector(-ViewFrustumCulling.a / 2f, -ViewFrustumCulling.b / 2f, -ViewFrustumCulling.c / 2f)),
        worldLocation(new Vector(ViewFrustumCulling.a / 2f, ViewFrustumCulling.b / 2f, ViewFrustumCulling.c / 2f)));
    switch (visibility) {
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
