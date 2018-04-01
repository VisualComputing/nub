public class Eye extends Node {
  public Eye(Graph graph) {
    super(graph);
  }

  protected Eye(Graph otherGraph, Eye otherNode) {
    super(otherGraph, otherNode);
  }

  @Override
  public Eye get() {
    return new Eye(graph(), this);
  }

  @Override
  public void interact(frames.input.Event event) {
    if (graph().eye().reference() == null) {
      if (event.shortcut().matches(new Shortcut(RIGHT)))
        translate(event);
      else if (event.shortcut().matches(new Shortcut(LEFT)))
        rotate(event);
      if (event.shortcut().matches(new Shortcut(frames.input.Event.SHIFT, CENTER)))
        rotate(event);
      else if (event.shortcut().matches(new TapShortcut(CENTER, 2)))
        center();
      else if (event.shortcut().matches(new TapShortcut(RIGHT)))
        align();
      else if (event.shortcut().matches(new Shortcut(MouseEvent.WHEEL)))
        translateZ(event);
    } else {
      if (event.shortcut().matches(new Shortcut(RIGHT)))
        moveBackward(event);
      else if (event.shortcut().matches(new Shortcut(LEFT)))
        moveForward(event);
      else if (event.shortcut().matches(new Shortcut(frames.input.Event.NO_ID)))
        lookAround(event);
    }
  }
}
