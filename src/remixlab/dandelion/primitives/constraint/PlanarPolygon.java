package remixlab.dandelion.primitives.constraint;

import remixlab.dandelion.primitives.Frame;
import remixlab.dandelion.primitives.Quat;
import remixlab.dandelion.primitives.Rotation;
import remixlab.dandelion.primitives.Vec;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 19/08/17.
 */
public class PlanarPolygon extends Constraint{
    /*
    TODO: Enable Setting different Axis Direction
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on a Cone which base is a Polygon. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */

    private ArrayList<Vec> vertices = new ArrayList<Vec>();
    private float height = 1.f;
    private Quat restRotation = new Quat();
    private Vec min, max;

    public Quat getRestRotation() {
        return restRotation;
    }
    public void setRestRotation(Quat restRotation) {
        this.restRotation = restRotation.get();
    }

    public ArrayList<Vec> getVertices() {
        return vertices;
    }

    public void setVertices(ArrayList<Vec> vertices) {
        this.vertices = vertices;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public PlanarPolygon(){
        vertices = new ArrayList<Vec>();
        restRotation = new Quat();
        height = 5.f;
    }

    public PlanarPolygon(ArrayList<Vec> vertices, Quat restRotation, float height) {
        this.vertices = vertices;
        this.restRotation = restRotation.get();
        this.height = height;
        projectToPlane();
        computeBoundingBox();
    }

    public PlanarPolygon(ArrayList<Vec> vertices, Quat restRotation) {
        this.vertices = vertices;
        this.restRotation = restRotation.get();
        projectToPlane();
        computeBoundingBox();
    }

    public PlanarPolygon(ArrayList<Vec> vertices) {
        this.vertices = vertices;
        projectToPlane();
        computeBoundingBox();
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


    public Vec getConstraint(Vec target){
        return getConstraint(target, restRotation);
    }

    public Vec getConstraint(Vec target, Quat restRotation){
        Vec uvec    = new Vec(0,1,0);
        Vec rvec    = new Vec(1,0,0);
        Vec line    = new Vec(0,0,1);
        Vec point   = restRotation.inverse().multiply(target);
        Vec proj    = new Vec(height*point.x()/point.z(),height*point.y()/point.z());
        float inverse = (height < 0) == (point.z() < 0) ? 1 : -1;
        if(!isInside(proj)){
            proj.multiply(inverse);
            Vec constrained = closestPoint(proj);
            constrained.setZ(height);
            constrained.multiply(inverse*point.z()/height);
            return restRotation.rotate(constrained);
        }
        return inverse == -1 ? new Vec(target.x(),target.y(), -target.z()) : target;
    }

    public void computeBoundingBox(){
        min = new Vec(); max = new Vec();
        for(Vec v : vertices){
            if(v.x() < min.x()) min.setX(v.x());
            if(v.y() < min.y()) min.setY(v.y());
            if(v.x() > max.x()) max.setX(v.x());
            if(v.y() > max.y()) max.setY(v.y());
        }
    }

    public void projectToPlane(){
        for(Vec v : vertices){
            //Just not consider Z
            v.setZ(0);
        }
    }

    /*Code was transcript from https://wrf.ecse.rpi.edu//Research/Short_Notes/pnpoly.html*/
    public boolean isInside(Vec point){
        if(point.x() < min.x() || point.x() > max.x() ||
                point.y() < min.y() || point.y() > max.y()) return false;
        //Ray-casting algorithm
        boolean c = false;
        for(int i = 0, j = vertices.size()-1; i < vertices.size(); j = i++){
            Vec v_i = vertices.get(i);
            Vec v_j = vertices.get(j);
            if ( ((v_i.y()>point.y()) != (v_j.y()>point.y())) &&
                    (point.x() < (v_j.x()-v_i.x()) * (point.y()-v_i.y()) / (v_j.y()-v_i.y()) + v_i.x()))
                        c = !c;
        }
        return c;
    }

    public Vec closestPoint(Vec point){
        float minDist = 999999;
        Vec target = new Vec();
        for(int i = 0, j = vertices.size()-1; i < vertices.size(); j = i++){
            Vec projection;
            float dist;
            Vec v_i = vertices.get(i);
            Vec v_j = vertices.get(j);
            Vec edge = Vec.subtract(v_i, v_j);
            //Get distance to line
            float t = Vec.dot(edge, Vec.subtract(point, v_j));
            t /= edge.magnitude()*edge.magnitude();

            if(t < 0){
                dist = Vec.distance(v_j,point);
                projection = v_j.get();
            }else if(t > 1){
                dist = Vec.distance(v_i,point);
                projection = v_i.get();
            }else{
                projection = Vec.add(v_j, Vec.multiply(edge,t));
                dist = Vec.subtract(point, projection).magnitude();
            }
            if(dist < minDist){
                minDist = dist;
                target = projection;
            }
        }
        return target;
    }
}
