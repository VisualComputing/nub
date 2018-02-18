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

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.Frame;
import java.util.ArrayList;

public class SphericalPolygon extends Constraint{
    /*
    TODO: Enable Setting different Axis Direction
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on a Cone which base is a Spherical Polygon. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */

    private ArrayList<Vector> vertices = new ArrayList<Vector>();
    private Vector visiblePoint = new Vector();
    private Quaternion restRotation = new Quaternion();
    private Vector min, max;

    //Some pre-computations
    private ArrayList<Vector> B= new ArrayList<Vector>();
    private ArrayList<Vector> S = new ArrayList<Vector>();



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
        this.vertices = projectToUnitSphere(vertices);
        this.visiblePoint = computeVisiblePoint();
        computeBoundingBox();
        doPrecomputations();
    }


    public SphericalPolygon(){
        vertices = new ArrayList<Vector>();
        restRotation = new Quaternion();
        visiblePoint = new Vector(0,0,1);
    }

    public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation, Vector visiblePoint) {
        this.vertices = projectToUnitSphere(vertices);
        this.restRotation = restRotation.get();
        this.visiblePoint = visiblePoint;
        visiblePoint.normalize();
        computeBoundingBox();
        doPrecomputations();
    }

    public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation) {
        this.vertices = projectToUnitSphere(vertices);
        this.restRotation = restRotation.get();
        this.visiblePoint = computeVisiblePoint();
        computeBoundingBox();
        doPrecomputations();
    }

    public SphericalPolygon(ArrayList<Vector> vertices) {
        this.vertices = projectToUnitSphere(vertices);
        this.visiblePoint = computeVisiblePoint();
        computeBoundingBox();
        doPrecomputations();
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
        if(!isInside(point)){
            Vector constrained = closestPoint(point);
            return restRotation.rotate(constrained);
        }
        return target;
    }

    public void computeBoundingBox(){
        min = new Vector(); max = new Vector();
        for(Vector v : vertices){
            if(v.x() < min.x()) min.setX(v.x());
            if(v.y() < min.y()) min.setY(v.y());
            if(v.z() < min.z()) min.setZ(v.z());
            if(v.x() > max.x()) max.setX(v.x());
            if(v.y() > max.y()) max.setY(v.y());
            if(v.z() > max.z()) max.setZ(v.z());
        }
    }


    //Compute centroid
    //TO DO: Choose a Visible point which works well for non convex Polygons
    public Vector computeVisiblePoint(){
        if(vertices.isEmpty()) return null;
        Vector centroid = new Vector();
        //Assume that every vertex lie in the sphere boundary
        for(Vector vertex : vertices){
            centroid.add(vertex);
        }
        centroid.normalize();
        return centroid;
    }

    public ArrayList<Vector> projectToUnitSphere(ArrayList<Vector> vertices){
        ArrayList<Vector> newVertices = new ArrayList<Vector>();
        for(Vector vertex : vertices){
            newVertices.add(vertex.normalize(new Vector()));
        }
        return newVertices;
    }

    public void doPrecomputations(){
        B = new ArrayList<Vector>();
        S = new ArrayList<Vector>();
        for(int i = 0; i < vertices.size(); i++){
            Vector p_i =  vertices.get(i);
            Vector p_j =  i + 1 == vertices.size() ? vertices.get(0) : vertices.get(i + 1);
            S.add(Vector.cross(visiblePoint, p_i, null));
            B.add(Vector.cross(p_i, p_j, null));
        }
    }

    public boolean isInside(Vector L){
        //1. Find i s.t p_i = S_i . L >= 0 and p_j = S_j . L < 0 with j = i + 1
        int index = 0;
        for(int i = 0; i < vertices.size(); i++){
            if(Vector.dot(S.get(i), L) >= 0 && Vector.dot(S.get((i + 1) % vertices.size()), L) < 0){
                index = i;
                break;
            }
        }
        return Vector.dot(B.get(index),L) >= 0;
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