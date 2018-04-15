package ik.common;

import common.InteractiveShape;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PGraphics;

/**
 * Created by sebchaparr on 28/01/18.
 */
public class Joint extends InteractiveShape {
  private int color;
  private boolean root = false;

  public Joint(Scene scene) {
    this(scene, scene.pApplet().color(scene.pApplet().random(0, 255), scene.pApplet().random(0, 255), scene.pApplet().random(0, 255)));
  }

  public Joint(Scene scene, int color) {
    super(scene);
    this.color = color;
  }

  public void setRoot(boolean root) {
    this.root = root;
  }

  public void set(PGraphics pg) {
    pg.pushStyle();
    pg.fill(color);
    pg.noStroke();
    if (pg.is2D()) pg.ellipse(0, 0, 3, 3);
    else pg.sphere(3);

    if (!root) {
      pg.strokeWeight(5);
      pg.stroke(color);
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