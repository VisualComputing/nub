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

import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import java.util.ArrayList;

public class PlanarPolygon extends Constraint{
    /*
    TODO: Enable Setting different Axis Direction
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on a Cone which base is a Polygon. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */

    private ArrayList<Vector> vertices = new ArrayList<Vector>();
    private float height = 1.f;
    private Quaternion restRotation = new Quaternion();
    private Vector min, max;

    public Quaternion getRestRotation() {
        return restRotation;
    }
    public void setRestRotation(Quaternion restRotation) {
        this.restRotation = restRotation.get();
    }

    public ArrayList<Vector> getVertices() {
        return vertices;
    }

    public void setVertices(ArrayList<Vector> vertices) {
        this.vertices = vertices;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public PlanarPolygon(){
        vertices = new ArrayList<Vector>();
        restRotation = new Quaternion();
        height = 5.f;
    }

    public PlanarPolygon(ArrayList<Vector> vertices, Quaternion restRotation, float height) {
        this.vertices = vertices;
        this.restRotation = restRotation.get();
        this.height = height;
        projectToPlane();
        computeBoundingBox();
    }

    public PlanarPolygon(ArrayList<Vector> vertices, Quaternion restRotation) {
        this.vertices = vertices;
        this.restRotation = restRotation.get();
        projectToPlane();
        computeBoundingBox();
    }

    public PlanarPolygon(ArrayList<Vector> vertices) {
        this.vertices = vertices;
        projectToPlane();
        computeBoundingBox();
    }

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        /*
            if(frame.is2D())
            throw new RuntimeException("This constrained not supports 2D Frames");
        */
        Quaternion desired = Quaternion.compose(frame.rotation(),rotation);
        Vector new_pos = Quaternion.multiply(desired, new Vector(0,0,1));
        Vector constrained = getConstraint(new_pos, restRotation);
        //Get Quaternion
        return new Quaternion(new Vector(0,0,1), Quaternion.multiply(frame.rotation().inverse(),constrained));
    }


    @Override
    public Vector constrainTranslation(Vector translation, Frame frame) {
        return new Vector(0,0,0);
    }


    public Vector getConstraint(Vector target){
        return getConstraint(target, restRotation);
    }

    public Vector getConstraint(Vector target, Quaternion restRotation){
        Vector point   = restRotation.inverse().multiply(target);
        Vector proj    = new Vector(height*point.x()/point.z(),height*point.y()/point.z());
        float inverse = (height < 0) == (point.z() < 0) ? 1 : -1;
        if(!isInside(proj)){
            proj.multiply(inverse);
            Vector constrained = closestPoint(proj);
            constrained.setZ(height);
            constrained.multiply(inverse*point.z()/height);
            return restRotation.rotate(constrained);
        }
        return inverse == -1 ? new Vector(target.x(),target.y(), -target.z()) : target;
    }

    public void computeBoundingBox(){
        min = new Vector(); max = new Vector();
        for(Vector v : vertices){
            if(v.x() < min.x()) min.setX(v.x());
            if(v.y() < min.y()) min.setY(v.y());
            if(v.x() > max.x()) max.setX(v.x());
            if(v.y() > max.y()) max.setY(v.y());
        }
    }

    public void projectToPlane(){
        for(Vector v : vertices){
            //Just not consider Z
            v.setZ(0);
        }
    }

    /*Code was transcript from https://wrf.ecse.rpi.edu//Research/Short_Notes/pnpoly.html*/
    public boolean isInside(Vector point){
        if(point.x() < min.x() || point.x() > max.x() ||
                point.y() < min.y() || point.y() > max.y()) return false;
        //Ray-casting algorithm
        boolean c = false;
        for(int i = 0, j = vertices.size()-1; i < vertices.size(); j = i++){
            Vector v_i = vertices.get(i);
            Vector v_j = vertices.get(j);
            if ( ((v_i.y()>point.y()) != (v_j.y()>point.y())) &&
                    (point.x() < (v_j.x()-v_i.x()) * (point.y()-v_i.y()) / (v_j.y()-v_i.y()) + v_i.x()))
                c = !c;
        }
        return c;
    }

    public Vector closestPoint(Vector point){
        float minDist = 999999;
        Vector target = new Vector();
        for(int i = 0, j = vertices.size()-1; i < vertices.size(); j = i++){
            Vector projection;
            float dist;
            Vector v_i = vertices.get(i);
            Vector v_j = vertices.get(j);
            Vector edge = Vector.subtract(v_i, v_j);
            //Get distance to line
            float t = Vector.dot(edge, Vector.subtract(point, v_j));
            t /= edge.magnitude()*edge.magnitude();

            if(t < 0){
                dist = Vector.distance(v_j,point);
                projection = v_j.get();
            }else if(t > 1){
                dist = Vector.distance(v_i,point);
                projection = v_i.get();
            }else{
                projection = Vector.add(v_j, Vector.multiply(edge,t));
                dist = Vector.subtract(point, projection).magnitude();
            }
            if(dist < minDist){
                minDist = dist;
                target = projection;
            }
        }
        return target;
    }
}