/**
 * Joint.
 * by Sebastian Chaparro.
 *
 * This class implements a shape behavior which requires
 * overriding the interact(Event) method.
 * Furthermore, it overrides  set(PGraphics) method to
 * draw a skeleton.
 *
 * Feel free to copy paste it.
 */

public class Joint extends OrbitShape {
  private int colour;
  private boolean root = false;

  public Joint(Scene scene) {
    this(scene, color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255)));
  }

  public Joint(Scene scene, int colour) {
    super(scene);
    this.colour = colour;
  }

  /* Whenever the joint is the root use this method to specify it
   otherwise it will be drawn an additional bone */
  public void setRoot(boolean root) {
    this.root = root;
  }

  /* Draws the bone that connects the previous Joint with this Joint
   and the kind of constraint that is used by this Joint. */
  public void set(PGraphics pg) {
    pg.pushStyle();
    pg.fill(colour);
    pg.noStroke();
    if (pg.is2D()) pg.ellipse(0, 0, 3, 3);
    else pg.sphere(3);
    if (!root) {
      pg.strokeWeight(5);
      pg.stroke(colour);
      Vector v = localCoordinatesOf(new Vector());
      if (pg.is2D()) {
        pg.line(0, 0, v.x(), v.y());
      } else {
        pg.line(0, 0, 0, v.x(), v.y(), v.z());
      }
      pg.popStyle();
    }

    if (constraint() != null) {
      graph().drawConstraint(this);
    }
  }
}
