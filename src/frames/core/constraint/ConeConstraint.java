/****************************************************************************************
 * frames
 * Copyright (c) 2018 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package frames.core.constraint;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import static java.lang.Math.PI;

/**
 * A Frame is constrained to disable translation and
 * allow 2-DOF rotation limiting Rotation in a Sphere to
 * laid inside an Ellipse.
 */

//TODO : Add Twist limit & check for unnecessary transformations
public abstract class ConeConstraint extends Constraint{
    protected Quaternion _restRotation = new Quaternion();
    protected Quaternion _idleRotation = new Quaternion();
    protected Quaternion _orientation = new Quaternion();

    public Quaternion restRotation() {
        return _restRotation;
    }
    public Quaternion idleRotation() {
        return _idleRotation;
    }

    public Quaternion orientation() {
        return _orientation;
    }

    public void setRestRotation(Quaternion restRotation) {
        this._restRotation = restRotation.get();
    }

    /**
     * reference is a Quaternion that will be aligned to point to the given Basis Vectors
     * result will be stored on restRotation.
     * twist and up axis are defined locally on reference rotation
     */
    Quaternion off = new Quaternion();
    public void setRestRotation(Quaternion reference, Vector up, Vector twist, Vector rest) {
        _orientation = reference.get();
        _idleRotation = reference.get();
        Quaternion delta = new Quaternion(new Vector(0, 1, 0), up);
        Vector tw = delta.inverseRotate(twist);
        delta.compose(new Quaternion(new Vector(0, 0, 1), tw));
        _orientation.compose(delta);
        Vector rs = delta.inverseRotate(rest);
        off = new Quaternion(twist, rest);
        //_idleRotation = Quaternion.compose(_idleRotation, off.inverse());
        //delta.compose(new Quaternion(new Vector(0, 0, 1), rs));//Quaternion.compose(_idleRotation.inverse(), q);
        _restRotation = delta;
        //_orientation.compose(delta);
        //Align y-Axis with twist vector
        //_orientation.compose(new Quaternion(new Vector(0, 0, 1), tw));
        //Vector rs = new Quaternion(new Vector(0, 1, 0), up).inverseRotate(rest);
        //Quaternion q = Quaternion.compose(_orientation,new Quaternion(rs, new Vector(0, 0, 1)));
        //_restRotation = Quaternion.compose(_idleRotation.inverse(), q);
    }


    public void setRestRotation(Quaternion reference, Vector up, Vector twist) {
        setRestRotation(reference, up, twist, twist);
    }

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        //Identify rotation that must be applied to move twist and up to its  new values
        Quaternion curr = _idleRotation; //w.r.t ref
        Quaternion next = Quaternion.compose(frame.rotation(), rotation); // w.r.t ref
        Quaternion change = Quaternion.compose(curr.inverse(), next); // w.r.t idle
        //Decompose change in terms of twist and swing
        Vector tw = _restRotation.rotate(new Vector(0, 0, 1)); // w.r.t idle
        Vector rotationAxis = new Vector(change._quaternion[0], change._quaternion[1], change._quaternion[2]);
        rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t idle
        //Get rotation component on Axis direction
        Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), change.w()); //w.r.t idle
        Quaternion rotationSwing = Quaternion.compose(change, rotationTwist.inverse()); //w.r.t idle
        //Constraint swing
        Vector w = rotationSwing.rotate(_restRotation.rotate(new Vector(0, 0, 1)));
        w = off.rotate(w);
        Vector constrained = apply(w, _restRotation);
        constrained = off.inverseRotate(constrained);
        rotationSwing = new Quaternion(tw, constrained);
        //constrained change
        Quaternion constrained_change = Quaternion.compose(rotationSwing, rotationTwist);
        //find change in terms of frame rot
        //_idle * constrained_change = frame * rot
        Quaternion rot = Quaternion.compose(frame.rotation().inverse(), Quaternion.compose(_idleRotation, constrained_change));

        return rot;
    }

    @Override
    public Vector constrainTranslation(Vector translation, Frame frame) {
        return new Vector(0, 0, 0);
    }

    public abstract Vector apply(Vector target, Quaternion restRotation);

}

