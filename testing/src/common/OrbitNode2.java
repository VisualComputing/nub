package common;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Shortcut;
import frames.processing.Mouse;
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

public class OrbitNode2 extends Node {
  public OrbitNode2(Graph graph) {
    super(graph);
  }

  public OrbitNode2(Node node) {
    super(node);
  }

  // this one gotta be overridden because we want a copied node
  // to have the same behavior as its original.
  protected OrbitNode2(Graph otherGraph, OrbitNode2 otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public OrbitNode2 get() {
    return new OrbitNode2(this.graph(), this);
  }

  // behavior is here
  @Override
  public void interact(frames.input.Event event) {
    if (((Scene) graph()).mouse().mode() == Mouse.Mode.MOVE) {
      if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
        moveBackward(event);
      else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        moveForward(event);
      else if (event.shortcut().matches(new Shortcut(frames.input.Event.NO_ID)))
        lookAround(event);
      else
        stopFlying();
    } else {
      if (event.shortcut().matches(new Shortcut(frames.input.Event.SHIFT, frames.input.Event.NO_ID)))
        moveBackward(event);
      else if (event.shortcut().matches(new Shortcut(frames.input.Event.CTRL, frames.input.Event.NO_ID)))
        moveForward(event);
      else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)) || event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
        lookAround(event);
      else
        stopFlying();
    }
    if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      translateZ(event);
  }
}
