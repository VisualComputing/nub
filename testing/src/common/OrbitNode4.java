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

public class OrbitNode4 extends Node {
  int _c;
  Vector pnt;
  Scene scene;
  PApplet parent;

  public OrbitNode4(Graph graph) {
    super(graph);
  }

  public OrbitNode4(Graph graph, int color) {
    super(graph);
    if (graph() instanceof Scene) {
      scene = (Scene) graph();
      parent = scene.pApplet();
    }
    _c = color;
    pnt = new Vector(40, 30, 20);
  }

  public OrbitNode4(Node node, int color) {
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
  protected OrbitNode4(Graph otherGraph, OrbitNode4 otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public OrbitNode4 get() {
    return new OrbitNode4(this.graph(), this);
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
      drive(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
      rotateCAD(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.CENTER)))
      rotate(event);
    else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }
}
