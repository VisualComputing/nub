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

  public Arrow(Scene scene, Node reference, Vector translation, int color) {
    super(scene);
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
    ((Scene) graph()).drawArrow(Vector.multiply(translation(), -1), new Vector(0, 0, 0), 1);
    pg.popStyle();
  }

  public void applyReferenceRotation(Vector mouse) {
    Vector delta = translateDesired(mouse);
    Vector normal = Vector.multiply(graph().viewDirection(), -1);
    normal = displacement(normal);
    Vector p = Vector.projectVectorOnPlane(translation(), normal);
    Vector q = Vector.projectVectorOnPlane(Vector.add(translation(), delta), normal);

    //Find amount of rotation
    Quaternion rotation = new Quaternion(translation(), Vector.add(translation(), delta));
    reference().rotate(rotation);
  }

  //------------------------------------
  //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
  protected Vector _translateDesired(float dx, float dy, float dz, int zMax, Node frame) {
    if (graph().is2D() && dz != 0) {
      System.out.println("Warning: graph is 2D. Z-translation reset");
      dz = 0;
    }
    dx = graph().isEye(frame) ? -dx : dx;
    dy = graph().isRightHanded() ^ graph().isEye(frame) ? -dy : dy;
    dz = graph().isEye(frame) ? dz : -dz;
    // Scale to fit the screen relative vector displacement
    if (graph().type() == Graph.Type.PERSPECTIVE) {
      float k = (float) Math.tan(graph().fov() / 2.0f) * Math.abs(
          graph().eye().location(graph().isEye(frame) ? graph().anchor() : frame.position())._vector[2] * graph().eye().magnitude());
      //TODO check me weird to find height instead of width working (may it has to do with fov?)
      dx *= 2.0 * k / (graph().height() * graph().eye().magnitude());
      dy *= 2.0 * k / (graph().height() * graph().eye().magnitude());
    }
    // this expresses the dz coordinate in world units:
    //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
    Vector eyeVector = new Vector(dx, dy, dz * 2 * graph().radius() / zMax);
    return frame.reference() == null ? graph().eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, graph().eye());
  }

  public Vector translateDesired(Vector point) {
    Vector delta = Vector.subtract(point, graph().screenLocation(position()));
    return _translateDesired(delta.x(), delta.y(), 0, Math.min(graph().width(), graph().height()), this);
  }
}

