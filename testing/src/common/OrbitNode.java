package common;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Shortcut;
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

public class OrbitNode extends Node {
  Scene scene;
  PApplet parent;

  public OrbitNode(Graph graph) {
    super(graph);
    if (graph() instanceof Scene) {
      scene = (Scene) graph();
      parent = scene.pApplet();
    }
  }

  public OrbitNode(Node node) {
    super(node);
    if (graph() instanceof Scene) {
      scene = (Scene) graph();
      parent = scene.pApplet();
    }
  }

  // this one gotta be overridden because we want a copied node
  // to have the same behavior as its original.
  protected OrbitNode(Graph otherGraph, OrbitNode otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public OrbitNode get() {
    return new OrbitNode(this.graph(), this);
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
