package nub.ik.visual;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PGraphics;

public class Arrow extends Node {
  int _color;

  public Arrow(Node reference, Vector translation, int color) {
    super();
    setReference(reference);
    translate(translation);
    _color = color;
    this.setConstraint(new Constraint() {
      @Override
      public Vector constrainTranslation(Vector translation, Node frame) {
        return new Vector();
      }

      @Override
      public Quaternion constrainRotation(Quaternion rotation, Node frame) {
        return new Quaternion();
      }
    });
    setHighlighting(0);
  }

  @Override
  public void graphics(PGraphics pg) {
    pg.pushStyle();
    pg.noStroke();
    pg.fill(_color);
    Vector a = Vector.multiply(translation(), -1);
    Vector b = new Vector(0, 0, 0);
    pg.line(a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
    //((Scene) graph()).drawArrow(a, b, 1);
    pg.popStyle();
  }

  public void applyReferenceRotation(Scene scene, Vector mouse) {
    Vector delta = translateDesired(scene, mouse);
    Vector normal = Vector.multiply(scene.viewDirection(), -1);
    normal = displacement(normal);
    Vector p = Vector.projectVectorOnPlane(translation(), normal);
    Vector q = Vector.projectVectorOnPlane(Vector.add(translation(), delta), normal);

    //Find amount of rotation
    Quaternion rotation = new Quaternion(translation(), Vector.add(translation(), delta));
    reference().rotate(rotation);
  }

  //------------------------------------
  //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
  protected Vector _translateDesired(Scene scene, float dx, float dy, float dz, int zMax, Node frame) {
    if (scene.is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    }
    dx = scene.isEye(frame) ? -dx : dx;
    dy = scene.isRightHanded() ^ scene.isEye(frame) ? -dy : dy;
    dz = scene.isEye(frame) ? dz : -dz;
    // Scale to fit the screen relative vector displacement
    if (scene.type() == Graph.Type.PERSPECTIVE) {
      float k = (float) Math.tan(scene.fov() / 2.0f) * Math.abs(
              scene.eye().location(scene.isEye(frame) ? scene.anchor() : frame.position())._vector[2] * scene.eye().magnitude());
      //TODO check me weird to find height instead of width working (may it has to do with fov?)
      dx *= 2.0 * k / (scene.height() * scene.eye().magnitude());
      dy *= 2.0 * k / (scene.height() * scene.eye().magnitude());
    }
    // this expresses the dz coordinate in world units:
    //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
    Vector eyeVector = new Vector(dx, dy, dz * 2 * scene.radius() / zMax);
    return frame.reference() == null ? scene.eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, scene.eye());
  }

  public Vector translateDesired(Scene scene, Vector point) {
    Vector delta = Vector.subtract(point, scene.screenLocation(position()));
    return _translateDesired(scene, delta.x(), delta.y(), 0, Math.min(scene.width(), scene.height()), this);
  }
}

