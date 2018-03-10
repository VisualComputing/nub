/**
 * OrbitShape.
 * by Jean Pierre Charalambos.
 * 
 * This class implements a shape behavior which requires
 * overriding the interact(Event) method.
 *
 * Feel free to copy paste it.
 */

public class OrbitShape extends Shape {
  public OrbitShape(Scene scene) {
    super(scene);
  }
  
  public OrbitShape(Node node) {
    super(node);
  }

  // this one gotta be overridden because we want a copied node
  // to have the same behavior as its original.
  protected OrbitShape(Scene otherScene, OrbitShape otherShape) {
    super(otherScene, otherShape);
  }

  @Override
  public OrbitShape get() {
    return new OrbitShape(this.graph(), this);
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