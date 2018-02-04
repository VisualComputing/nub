package common;

import processing.core.PApplet;
import proscene.core.Graph;
import proscene.core.Node;
import proscene.input.event.KeyEvent;
import proscene.input.event.KeyShortcut;
import proscene.input.event.MotionEvent;
import proscene.input.event.TapEvent;
import proscene.processing.MouseAgent;

public class InteractiveNode extends Node {
  KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
  KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
  KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
  KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);

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
  public void interact(MotionEvent event) {
    if (event.shortcut().matches(MouseAgent.RIGHT))
      translate(event);
    else if (event.shortcut().matches(MouseAgent.LEFT))
      rotate(event);
    else if (event.shortcut().matches(MouseAgent.WHEEL))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }

  @Override
  public void interact(TapEvent event) {
    if (event.shortcut().matches(MouseAgent.CENTER_TAP2))
      center();
    else if (event.shortcut().matches(MouseAgent.RIGHT_TAP))
      align();
  }

  @Override
  public void interact(KeyEvent event) {
    if (event.shortcut().matches(upArrow))
      translateYPos();
    else if (event.shortcut().matches(downArrow))
      translateYNeg();
    else if (event.shortcut().matches(leftArrow))
      translateXNeg();
    else if (event.shortcut().matches(rightArrow))
      translateXPos();
  }
}
