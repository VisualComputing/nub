package ik.common;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.Constraint;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PGraphics;

public class Arrow extends Frame {
    int _color;

    public Arrow(Scene scene, Frame reference, Vector translation, int color){
        super(scene);
        setReference(reference);
        translate(translation);
        _color = color;
        this.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Frame frame) {
                return new Vector();
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
                return new Quaternion();
            }
        });
        setHighlighting(Highlighting.NONE);
    }

    @Override
    public boolean graphics(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(_color);
        ((Scene) graph()).drawArrow(Vector.multiply(translation(), -1) , new Vector(0,0,0), 1);
        pg.popStyle();
        return true;
    }

    public void applyReferenceRotation(Vector mouse){
        Vector delta = translateDesired(mouse);
        Vector normal = Vector.multiply(graph().viewDirection(),-1);
        normal = displacement(normal);
        Vector p = Vector.projectVectorOnPlane(translation(), normal);
        Vector q = Vector.projectVectorOnPlane(Vector.add(translation(), delta), normal);

        //Find amount of rotation
        Quaternion rotation  = new Quaternion(translation(), Vector.add(translation(), delta));
        reference().rotate(rotation);
    }

    //------------------------------------
    //Interactive actions - same method found in Graph Class (duplicated cause of visibility)
    protected Vector _translateDesired(float dx, float dy, float dz, int zMax, Frame frame) {
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
            dy *= 2.0 * k / (graph().height() *graph(). eye().magnitude());
        }
        // this expresses the dz coordinate in world units:
        //Vector eyeVector = new Vector(dx, dy, dz / eye().magnitude());
        Vector eyeVector = new Vector(dx, dy, dz * 2 * graph().radius() / zMax);
        return frame.reference() == null ? graph().eye().worldDisplacement(eyeVector) : frame.reference().displacement(eyeVector, graph().eye());
    }

    public Vector translateDesired(Vector point){
        Vector delta = Vector.subtract(point, graph().screenLocation(position()));
        return _translateDesired(delta.x(), delta.y(), 0, Math.min(graph().width(), graph().height()), this);
    }
}

