package common;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Event;
import frames.processing.Mouse;

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
    /*
    if (event.shortcut().matches(Mouse.RIGHT))
      moveForward(event);
    if (event.shortcut().matches(Mouse.LEFT))
      moveBackward(event);
    if (event.shortcut().matches(Mouse.CENTER))
      lookAround(event);
    //*/
    // /*
    if (event.shortcut().matches(Mouse.RIGHT))
      drive(event);
      //translate(event);
      //rotateX(event);
      //screenRotate(event);
      //moveForward(event);
    else if (event.shortcut().matches(Mouse.LEFT))
      translate(event);
      //rotateCAD(event);
      //moveBackward(event);
    if (event.shortcut().matches(Mouse.CENTER))
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
     // */
  }
}
