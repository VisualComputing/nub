package common;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Shortcut;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;

/**
 * OrbitNodes.
 * by Jean Pierre Charalambos.
 * <p>
 * This class implements a node behavior which requires
 * overriding the interact(Event) method.
 * <p>
 * Feel free to copy paste it.
 */

public class OrbitNode3 extends Node {
  int _c;
  Vector pnt;
  Scene scene;
  PApplet parent;

  public OrbitNode3(Graph graph, int color) {
    super(graph);
    if (graph() instanceof Scene) {
      scene = (Scene) graph();
      parent = scene.pApplet();
    }
    _c = color;
    pnt = new Vector(40, 30, 20);
  }

  public OrbitNode3(Node node, int color) {
    super(node);
    if (graph() instanceof Scene) {
      scene = (Scene) graph();
      parent = scene.pApplet();
    }
    _c = color;
    pnt = new Vector(40, 30, 20);
  }

  // this one gotta be overridden because we want a copied node
  // to have the same behavior as its original.
  protected OrbitNode3(Graph otherGraph, OrbitNode3 otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public OrbitNode3 get() {
    return new OrbitNode3(this.graph(), this);
  }

  @Override
  public void visit() {
    if (isEye())
      return;
    parent.pushStyle();
    scene.drawAxes(40);
    parent.stroke(_c);
    scene.drawShooterTarget(this);
    parent.strokeWeight(10);
    parent.point(pnt.x(), pnt.y(), pnt.z());
    parent.popStyle();
  }

  // behavior is here
  @Override
  public void interact(frames.input.Event event) {
    if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
      translate(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
      rotate(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.CENTER)))
      rotate(event);
    else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }
}
