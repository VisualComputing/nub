/**
 * Shape6.
 * by Jean Pierre Charalambos.
 * 
 * This class implements a shape behavior which requires
 * overriding the interact(Event) method.
 *
 * Feel free to copy paste it.
 */

public class Shape6 extends Shape {  
  public Shape6(Scene scene) {
    super(scene);
  }
  
  public Shape6(Node node) {
    super(node);
  }
  
  public Shape6(Scene scene, PShape pShape) {
    super(scene, pShape);
  }

  // this one gotta be overridden because we want a copied node
  // to have the same behavior as its original.
  protected Shape6(Scene otherScene, Shape6 otherShape) {
    super(otherScene, otherShape);
  }

  @Override
  public Shape6 get() {
    return new Shape6(this.graph(), this);
  }

  // behavior is here
  @Override
  public void interact(frames.input.Event event) {
    // The SN_ID shorcut is fired by agent6 (the one handling the space-navigator)
    if (event.shortcut().matches(new Shortcut(SN_ID)))
      // its safe to call node methods having 6-DOFs or less.
      translateRotateXYZ(event);
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
