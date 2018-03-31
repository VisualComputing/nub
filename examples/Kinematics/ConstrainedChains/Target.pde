/**
 * Joint.
 * by Sebastian Chaparro.
 *
 * This class implements a shape behavior which requires
 * overriding the interact(Event) method (extends OrbitShape).
 * Furthermore, it overrides  set(PGraphics) method to
 * draw a Target (Basically an Sphere).
 *
 * Feel free to copy paste it.
 */

public class Target extends OrbitShape  {
  public Target(Scene scene) {
    super(scene);
  }

  /* Draws an Sphere that represents the desired End Effector position. */
  public void set(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(255, 0, 0, 200);
    if (pg.is2D())
      pg.ellipse(0, 0, 5, 5);
    else
      pg.sphere(5);
    pg.popStyle();
    graph().drawAxes(5);

  }
}