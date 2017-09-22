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

import remixlab.primitives.Frame;
import remixlab.primitives.Quat;
import remixlab.primitives.Rotation;
import remixlab.primitives.Vec;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

/**
 * A Frame is constrained to disable translation and
 * allow 2-DOF rotation limiting Rotation in a Sphere to
 * laid inside an Ellipse.
 */
public class BallAndSocket extends Constraint{
    /*
    TODO: Enable Setting different Axis Direction
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on 4 angles. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */
    private float down = (float)(PI/2.f);
    private float up = (float)(PI/2.f);
    private float left = (float)(PI/2.f);
    private float right = (float)(PI/2.f);
    private Quat restRotation = new Quat();

    public float getDown() {
        return down;
    }

    public void setDown(float down) {
        this.down = down;
    }

    public float getUp() {
        return up;
    }

    public void setUp(float up) {
        this.up = up;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public Quat getRestRotation() {
        return restRotation;
    }

    public void setRestRotation(Quat restRotation) {
        this.restRotation = restRotation.get();
    }

    public BallAndSocket(){
        down = (float)(PI/2.f);
        left = (float)(PI/2.f);
        up = (float)(PI/2.f);
        right = (float)(PI/2.f);
        restRotation = new Quat();
    }

    public BallAndSocket(float down, float up, float left, float right, Quat restRotation) {
        this.down = down;
        this.up = up;
        this.left = left;
        this.right = right;
        this.restRotation = restRotation.get();
    }

    public BallAndSocket(float down, float up, float left, float right) {
        this.down = down;
        this.up = up;
        this.left = left;
        this.right = right;
    }

    @Override
    public Rotation constrainRotation(Rotation rotation, Frame frame) {
        if(frame.is2D())
            throw new RuntimeException("This constrained not supports 2D Frames");
        Quat desired = (Quat) Quat.compose(frame.rotation(),rotation);
        Vec new_pos = Quat.multiply(desired, new Vec(0,0,1));
        Vec constrained = getConstraint(new_pos, restRotation);
        //Get Quaternion
        return new Quat(new Vec(0,0,1), Quat.multiply((Quat)frame.rotation().inverse(),constrained));
    }


    @Override
    public Vec constrainTranslation(Vec translation, Frame frame) {
        return new Vec(0,0,0);
    }


    /*
      * Adapted from http://wiki.roblox.com/index.php?title=Inverse_kinematics
      * new_pos: new position defined in terms of local coordinates
    */

    public Vec getConstraint(Vec target){
        return getConstraint(target, restRotation);
    }

    public Vec getConstraint(Vec target, Quat restRotation){
        Vec uvec    = Quat.multiply(restRotation,new Vec(0,1,0));
        Vec rvec = Quat.multiply(restRotation,new Vec(1,0,0));
        Vec line = Quat.multiply(restRotation, new Vec(0,0,1));

        float PI = (float) Math.PI;
        Vec f = target.get();
        float scalar = Vec.dot(target, line)/line.magnitude();
        Vec proj = Vec.multiply(line, scalar);
        Vec adjust = Vec.subtract(target, proj);
        float xaspect = Vec.dot(adjust, rvec);
        float yaspect = Vec.dot(adjust, uvec);
        float clampDown     = this.down;
        float clampUp       = this.up;
        float clampLeft     = this.left;
        float clampRight    = this.right;
        boolean inv = false;
        float xbound = xaspect >= 0 ? clampRight : clampLeft;
        float ybound = yaspect >= 0 ? clampUp : clampDown;
        boolean inbounds = true;
        if(scalar < 0){
            if(xbound > PI/2. && ybound > PI/2.){
                xbound = proj.magnitude()* (float)(Math.tan(PI - xbound));
                ybound = proj.magnitude()* (float)(Math.tan(PI - ybound));
                inv = true;
            }else{
                xbound = proj.magnitude()* (float)(Math.tan(xbound));
                ybound = proj.magnitude()* (float)(Math.tan(ybound));
                proj.multiply(-1.f);
                inbounds = false;
            }
        }else{
            xbound = xbound > PI/2. ? proj.magnitude()* (float)(Math.tan(PI/2.f))
                    : proj.magnitude()* (float)(Math.tan(xbound));
            ybound = ybound > PI/2. ? proj.magnitude()* (float)(Math.tan(PI/2.f))
                    : proj.magnitude()* (float)(Math.tan(ybound));
        }

        xbound = xbound > Math.pow(10,2) ? (float)Math.pow(10,2) : xbound;
        ybound = ybound > Math.pow(10,2) ? (float)Math.pow(10,2) : ybound;
        xbound = xbound < -Math.pow(10,2) ? (float)-Math.pow(10,2) : xbound;
        ybound = ybound < -Math.pow(10,2) ? (float)-Math.pow(10,2) : ybound;

        float ellipse = ((xaspect*xaspect)/(xbound*xbound)) + ((yaspect*yaspect)/(ybound*ybound));
        inbounds = inbounds && ellipse <=1;
        if((!inbounds && !inv && proj.magnitude() > Float.MIN_VALUE) || (inbounds && inv && proj.magnitude() > Float.MIN_VALUE)){
            float a = (float)(Math.atan2(yaspect, xaspect));
            float cos = (float)(Math.cos(a));
            if(cos < 0) cos = -cos < Math.pow(10,-4) ? -Float.MIN_VALUE : cos;
            else cos = cos < Math.pow(10,-4) ? Float.MIN_VALUE : cos;
            float sin = (float)(Math.sin(a));
            if(sin < 0)sin = -sin < Math.pow(10,-4) ? -Float.MIN_VALUE : sin;
            else sin = sin < Math.pow(10,-4) ? Float.MIN_VALUE : sin;
            float rad = 1.f/(float)Math.sqrt(((cos*cos)/(xbound*xbound)) + ((sin*sin)/(ybound*ybound)));
            if(Math.abs(cos) <= Float.MIN_VALUE ){
                rad = (float) Math.sqrt((ybound*ybound)/(sin*sin));
            }
            if(Math.abs(sin) <= Float.MIN_VALUE ){
                rad = (float) Math.sqrt((xbound*xbound)/(cos*cos));
            }
            float x = rad * cos;
            float y = rad * sin;

            f = Vec.add(proj, Vec.multiply(rvec, x));
            f = Vec.add(f, Vec.multiply(uvec, y));

            if(Math.abs(f.x()) < Math.pow(10,-4))f.setX(0);
            if(Math.abs(f.y()) < Math.pow(10,-4))f.setY(0);
            if(Math.abs(f.z()) < Math.pow(10,-4))f.setZ(0);
            f.normalize();
            f.multiply(target.magnitude());
        }
        return f;
    }
}