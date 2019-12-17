package intellij;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

public class OctreeNode extends Node {
  Vector p1, p2;
  int level = 4;

  OctreeNode(Scene scene, Vector P1, Vector P2) {
    super(scene);
    p1 = P1;
    p2 = P2;
  }

  OctreeNode(OctreeNode node, Vector P1, Vector P2) {
    super(node);
    level = node.level - 1;
    // setScaling(node.scaling() / 2);
    p1 = P1;
    p2 = P2;
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

  // customize traversal
  @Override
  public void visit() {
    switch (graph().boxVisibility(p1, p2)) {
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
