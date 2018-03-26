package common;

import frames.core.Graph;
import frames.core.Node;
import frames.input.Event;
import frames.input.Shortcut;
import frames.input.event.TapShortcut;
import processing.core.PApplet;

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
    profile3(event);
  }

  public void profile1(Event event) {
    if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
      translate(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
      rotate(event);
    if (event.shortcut().matches(new Shortcut(PApplet.CENTER)))
      rotate(event);
    else if (event.shortcut().matches(new TapShortcut(PApplet.CENTER, 2)))
      center();
    else if (event.shortcut().matches(new TapShortcut(PApplet.RIGHT)))
      align();
    else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }

  public void profile2(Event event) {
    if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
      moveForward(event);
    else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
      moveBackward(event);
    if (event.shortcut().matches(new Shortcut(PApplet.CENTER)))
      lookAround(event);
    else if (event.shortcut().matches(new TapShortcut(PApplet.CENTER, 2)))
      center();
    else if (event.shortcut().matches(new TapShortcut(PApplet.RIGHT)))
      align();
    else if (event.shortcut().matches(new Shortcut(processing.event.MouseEvent.WHEEL)))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
  }

  public void profile3(Event event) {
    if (graph().eye().reference() == null) {
      if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
        translate(event);
      else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        rotate(event);
      if (event.shortcut().matches(new Shortcut(Event.SHIFT, PApplet.CENTER)))
        rotate(event);
      else if (event.shortcut().matches(new TapShortcut(PApplet.CENTER, 2)))
        center();
      else if (event.shortcut().matches(new TapShortcut(PApplet.RIGHT)))
        align();
      else if (event.shortcut().matches(new Shortcut(Event.CTRL, processing.event.MouseEvent.WHEEL)))
        if (isEye() && graph().is3D())
          translateZ(event);
        else
          scale(event);
    } else {
      if (event.shortcut().matches(new Shortcut(PApplet.RIGHT)))
        moveBackward(event);
      else if (event.shortcut().matches(new Shortcut(PApplet.LEFT)))
        moveForward(event);
      else if (event.shortcut().matches(new Shortcut(frames.input.Event.NO_ID)))
        lookAround(event);
    }
  }
}
