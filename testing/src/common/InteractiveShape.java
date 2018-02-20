package common;


import frames.input.Shortcut;
import frames.input.event.KeyEvent;
import frames.input.event.KeyShortcut;
import frames.input.event.MotionEvent;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;
import processing.core.PShape;

public class InteractiveShape extends Shape {
  Shortcut left = new Shortcut(PApplet.LEFT);
  Shortcut right = new Shortcut(PApplet.RIGHT);
  Shortcut wheel = new Shortcut(processing.event.MouseEvent.WHEEL);
  KeyShortcut upArrow = new KeyShortcut(PApplet.UP);
  KeyShortcut downArrow = new KeyShortcut(PApplet.DOWN);
  KeyShortcut leftArrow = new KeyShortcut(PApplet.LEFT);
  KeyShortcut rightArrow = new KeyShortcut(PApplet.RIGHT);

  public InteractiveShape(Scene scene) {
    super(scene);
  }

  public InteractiveShape(InteractiveShape interactiveShape) {
    super(interactiveShape);
  }

  public InteractiveShape(Scene scene, PShape shape) {
    super(scene, shape);
  }

  public InteractiveShape(InteractiveShape interactiveShape, PShape shape) {
    super(interactiveShape, shape);
  }

  // this one gotta be overridden because we want a copied frame (e.g., line 141 above, i.e.,
  // scene.eye().get()) to have the same behavior as its original.
  protected InteractiveShape(Scene otherScene, InteractiveShape otherShape) {
    super(otherScene, otherShape);
  }

  @Override
  public InteractiveShape get() {
    return new InteractiveShape(this.graph(), this);
  }

  // behavior is here :P
  @Override
  public void interact(MotionEvent event) {
    if (event.shortcut().matches(left))
      rotate(event);
    else if (event.shortcut().matches(right))
      translate(event);
    else if (event.shortcut().matches(wheel))
      if (isEye() && graph().is3D())
        translateZ(event);
      else
        scale(event);
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
