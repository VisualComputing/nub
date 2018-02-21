/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.primitives.constraint;

//TODO: CHECK FORWARD STEP WITH HINGE 3D

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.Frame;
import static java.lang.Math.PI;

/**
 * A Frame is constrained to disable translation and
 * allow 1-DOF rotation limiting Rotation by defining an
 * Axis (according to Local Frame Coordinates) a Rest Rotation
 * and upper and lower bounds with values between 0 and PI
 */
public class Hinge extends Constraint{
    /*
    With this Kind of Constraint no Translation is allowed
    * and the rotation depends on 2 angles this kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set Identity is assumed as rest position
    * */
    private float max;
    private float min;
    private Quaternion restRotation;
    private Quaternion restTwist;
    private Vector axis;

    public Vector getAxis() {
        return axis;
    }

    public void setAxis(Vector axis) {
        this.axis = axis;
    }

    public Quaternion getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(Quaternion restRotation) {
        this.restRotation = restRotation.get();
    }

    public Hinge(){
        max = (float)(PI);
        min = (float)(PI);
        axis = new Vector(0,1,0);
    }

    public Hinge(boolean is2D){
        this();
        if(is2D) axis = new Vector(0,0,1);
    }

    /*Create a Hinge constraint in a with rest rotation
    * Given by the axis (0,0,1) and angle restAngle*/
    public Hinge(float min, float max, float restAngle) {
        this(true);
        this.restRotation = new Quaternion(new Vector(0,0,1), restAngle);
    }

    public Hinge(float min, float max, Quaternion rotation) {
        this(min, max);
        this.restRotation = rotation;
    }

    public Hinge(boolean is2D, float min, float max) {
        this(is2D);
        this.max = max;
        this.min = min;
    }

    public Hinge(float min, float max) {
        this(false);
        this.max = max;
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        /*First constraint rotation to be defined with respect to Axis*/
        Vector rotationAxis = Vector.projectVectorOnAxis(rotation.axis(), this.axis);
        //Get rotation component on Axis direction
        Quaternion rotationTwist = new Quaternion(rotationAxis, rotation.angle());
        float deltaAngle = rotationTwist.angle();
        if(rotationAxis.dot(axis) < 0) deltaAngle *= -1;
        /*First rotation of Frame with respect to Axis*/
        Quaternion current = Quaternion.compose(frame.rotation(),restRotation.inverse());
        /*It is possible that the current rotation axis is not parallel to Axis*/
        Vector currentAxis = Vector.projectVectorOnAxis(current.axis(), this.axis);
        //Get rotation component on Axis direction
        Quaternion currentTwist = new Quaternion(currentAxis, current.angle());
        float frameAngle = currentTwist.angle();

        if(current.axis().dot(axis) < 0) frameAngle *= -1;
        if(frameAngle + deltaAngle > max){
            float r = max - frameAngle;
            return new Quaternion(axis, r);
        }else if (frameAngle + deltaAngle < -min){
            float r = -min - frameAngle;
            return new Quaternion(axis, r);
        }else{
            return new Quaternion(axis,deltaAngle);
        }
    }

    @Override
    public Vector constrainTranslation(Vector translation, Frame frame) {
        return new Vector();
    }
}
