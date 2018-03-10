/**
 * OrbitNodes.
 * by Jean Pierre Charalambos.
 * 
 * This class implements a node behavior which requires
 * overriding the interact(Event) method.
 *
 * Feel free to copy paste it.
 */

public class OrbitNode extends Node {
  public OrbitNode(Graph graph) {
    super(graph);
  }
  
  public OrbitNode(Node node) {
    super(node);
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

  // behavior is here :P
  @Override
  public void interact(frames.input.Event event) {
    if (event.shortcut().matches(new Shortcut(RIGHT)))
      translate(event);
    if (event.shortcut().matches(new Shortcut(LEFT)))
      rotate(event);
    if (event.shortcut().matches(new Shortcut(CENTER)))
      rotate(event);
    if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }
}