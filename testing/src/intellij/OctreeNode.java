package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

public class OctreeNode extends Node {
  int level = 4;

  OctreeNode(Scene scene) {
    super(scene);
  }

  OctreeNode(OctreeNode node, Vector vector) {
    super(node);
    scale(0.5f);
    translate(Vector.multiply(vector, scaling() / 2));
    level = node.level - 1;
    // setScaling(node.scaling() / 2);
  }

  // Calculates the base-10 logarithm of a number
  float log2(float x) {
    return (PApplet.log(x) / PApplet.log(2));
  }

  //public void draw(PGraphics pg) {
  @Override
  public void graphics(PGraphics pg) {
    //float level = 4 - log2(1/magnitude());
    pg.stroke(pg.color(0.3f * level * 255, 0.2f * 255, (1.0f - 0.3f * level) * 255));
    pg.strokeWeight(8);
    pg.noFill();
    pg.box(ViewFrustumCulling.a, ViewFrustumCulling.b, ViewFrustumCulling.c);
  }

  // customize traversal
  @Override
  public void visit() {
    Vector corner1 = worldLocation(new Vector(-ViewFrustumCulling.a / 2f, -ViewFrustumCulling.b / 2f, -ViewFrustumCulling.c / 2f));
    Vector corner2 = worldLocation(new Vector(ViewFrustumCulling.a / 2f, ViewFrustumCulling.b / 2f, ViewFrustumCulling.c / 2f));
    switch (graph().boxVisibility(corner1, corner2)) {
      case VISIBLE:
        for (Node node : children())
          node.cull();
        break;
      case SEMIVISIBLE:
        for (Node node : children())
          node.cull(false);
        break;
      case INVISIBLE:
        cull();
        break;
    }
  }
}
