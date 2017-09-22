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

package remixlab.primitives.constraint;

import remixlab.primitives.*;

import static java.lang.Math.PI;

//TODO: CHECK FORWARD STEP WITH HINGE 3D

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
    private float max = (float)(PI);
    private float min = (float)(PI);
    private Rotation restRotation;
    private Vec axis = new Vec(0,1,0);

    public Vec getAxis() {
        return axis;
    }

    public void setAxis(Vec axis) {
        this.axis = axis;
    }

    public Rotation getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(Rotation restRotation) {
        this.restRotation = restRotation.get();
    }

    public Hinge(){
        max = (float)(PI);
        min = (float)(PI);
    }

    public Hinge(float min, float max, Rotation rotation) {
        this.max = max;
        this.min = min;
        this.restRotation = rotation;
    }

    public Hinge(float min, float max) {
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
    public Rotation constrainRotation(Rotation rotation, Frame frame) {
        if(frame.is2D() && rotation instanceof Quat)
            throw new RuntimeException("Rotation must be an instance of Rot");
        else if(!frame.is2D() && rotation instanceof Rot)
            throw new RuntimeException("Rotation must be an instance of Quat");
        float deltaAngle = rotation.angle();
        float frameAngle = frame.rotation().angle();

        Rot rest;

        if(frame.is3D()){
            /*First constraint rotation to be defined with respect to Axis*/
            Vec deltaAxis = new Vec(((Quat) rotation).quat[0], ((Quat) rotation).quat[1], ((Quat) rotation).quat[2]);
            deltaAxis = Vec.projectVectorOnAxis(deltaAxis, axis);
            deltaAngle = 2.0f * (float) Math.acos(((Quat) rotation).quat[3]);
            if(deltaAxis.dot(axis) < 0) deltaAngle *=-1;
            /*First rotation of Frame with respect to Axis*/
            Quat diff = (Quat) Quat.compose(frame.rotation(),restRotation.inverse());
            frameAngle = diff.angle();
            if(diff.axis().dot(axis) < 0) frameAngle *=-1;
            frameAngle = new Rot(frameAngle).normalize(true);
            rest = new Rot();
        }else{
            rest = (Rot)restRotation;
        }
        Rot current = ((Rot)Rot.compose(rest.inverse(), new Rot(frameAngle)));
        current.normalize(false);
        Rot next = ((Rot)Rot.compose(current, new Rot(deltaAngle)));
        next.normalize(false);

        if(next.angle() > max){
            Rot r = new Rot(max - current.angle());
            r.normalize(false);
            return frame.is3D() ? new Quat(axis, r.angle()) : r;
        }else if (next.angle() < -min){
            Rot r = new Rot(-min - current.angle());
            r.normalize(false);
            return frame.is3D() ? new Quat(axis, r.angle()) : r;
        }else{
            return frame.is3D() ? new Quat(axis,deltaAngle) : rotation;
        }
    }

    @Override
    public Vec constrainTranslation(Vec translation, Frame frame) {
        return new Vec();
    }
}
