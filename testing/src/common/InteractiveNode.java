package common;

import proscene.core.Graph;
import proscene.core.Node;
import proscene.input.Event;
import proscene.processing.Mouse;

public class InteractiveNode extends Node {
  public InteractiveNode(Graph graph) {
    super(graph);
  }

  // this one gotta be overridden because we want a copied frame (e.g., line 100 above, i.e.,
  // scene.eye().get()) to have the same behavior as its original.
  protected InteractiveNode(Graph otherGraph, InteractiveNode otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public InteractiveNode get() {
    return new InteractiveNode(this.graph(), this);
  }

  // behavior is here :P
  @Override
  public void interact(Event event) {
    if (event.shortcut().matches(Mouse.RIGHT))
      translate(event);
    else if (event.shortcut().matches(Mouse.LEFT))
      rotate(event);
    else if (event.shortcut().matches(Mouse.CENTER_TAP2))
      center();
    else if (event.shortcut().matches(Mouse.RIGHT_TAP))
      align();
    else if (event.shortcut().matches(Mouse.WHEEL))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }
}
