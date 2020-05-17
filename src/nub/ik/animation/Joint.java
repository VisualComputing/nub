package nub.ik.animation;

/**
 * Created by sebchaparr on 21/07/18.
 */

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.List;

public class Joint extends Node {
  public static boolean depth = false;
  protected String _name;
  protected int _r, _g, _b, _alpha = 255;
  protected float _radius;
  protected List<PShape> _mesh;
  public static boolean axes = false;
  public static float constraintFactor = 0.5f;
  //set to true only when the joint is the root (for rendering purposes)
  protected boolean _isRoot = false, _drawConstraint = true;

  public void addMesh(PShape mesh) {
    if (_mesh == null) _mesh = new ArrayList<>();
    _mesh.add(mesh);
  }

  public Joint(int red, int green, int blue, float radius) {
    super();
    _r = red;
    _g = green;
    _b = blue;
    _radius = radius;
    setPickingThreshold(0);
  }

  public Joint(int red, int green, int blue) {
    this(red, green, blue, 5f);
  }

  public Joint() {
    this((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
  }

  public Joint(float radius) {
    this((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255), radius);
  }

  public void setDrawConstraint(boolean drawConstraint) {
    _drawConstraint = drawConstraint;
  }

  @Override
  public void graphics(PGraphics pg) {
    if (_mesh != null) {
      for (PShape shape : _mesh) pg.shape(shape);
    }

    if (!depth) pg.hint(PConstants.DISABLE_DEPTH_TEST);
    pg.pushStyle();
    if (!_isRoot) {
      pg.strokeWeight(Math.max(_radius / 4f, 2));
      if (reference() instanceof Joint) {
        Joint ref = ((Joint) reference());
        pg.stroke(ref._r, ref._g, ref._b, _alpha);
      } else {
        pg.stroke(_r, _g, _b, _alpha);
      }

      Vector v = location(new Vector(), reference());
      float m = v.magnitude();
      if (pg.is2D()) {
        pg.line(_radius * v.x() / m, _radius * v.y() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m);
      } else {
        pg.line(_radius * v.x() / m, _radius * v.y() / m, _radius * v.z() / m, (m - _radius) * v.x() / m, (m - _radius) * v.y() / m, (m - _radius) * v.z() / m);
      }
    }
    pg.fill(_r, _g, _b, _alpha);
    pg.noStroke();
    if (pg.is2D()) pg.ellipse(0, 0, _radius * 2, _radius * 2);
    else pg.sphere(_radius);
    pg.strokeWeight(_radius / 4f);
    if (constraint() != null && _drawConstraint) {
      Scene.drawConstraint(pg, this, constraintFactor);
    }
    if (axes) Scene.drawAxes(pg, _radius * 2);
    if (!depth) pg.hint(PConstants.ENABLE_DEPTH_TEST);

    pg.stroke(255);
    //pg.strokeWeight(2);
    pg.popStyle();
  }

  public void setColor(int red, int green, int blue) {
    _r = red;
    _g = green;
    _b = blue;
  }

  public void setAlpha(int alpha) {
    _alpha = alpha;
  }

  public void setRadius(float radius) {
    _radius = radius;
    setPickingThreshold(-_radius * 2);
  }

  public void setName(String name) {
    _name = name;
  }

  public void setRoot(boolean isRoot) {
    _isRoot = isRoot;
  }

  public float radius() {
    return _radius;
  }

  public int alpha() {
    return _alpha;
  }

  public String name() {
    return _name;
  }

  public int red() {
    return _r;
  }

  public int green() {
    return _g;
  }

  public int blue() {
    return _b;
  }
}
