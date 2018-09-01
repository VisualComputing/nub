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

import java.util.ArrayList;

/**
* A Frame is constrained to disable translation and
* allow 2-DOF rotation limiting Z-Axis Rotation on a Cone which base is a Polygon.
* If no restRotation is set Quat() is assumed as restRotation
*/

public class PlanarPolygon extends ConeConstraint {
  //TODO: Find a Ball and Socket constraint that is enclosed by this one

  protected ArrayList<Vector> _vertices = new ArrayList<Vector>();
  protected float _height = 1.f;
  protected Vector _min, _max;

  public ArrayList<Vector> vertices() {
    return _vertices;
  }

  public void setVertices(ArrayList<Vector> vertices) {
    this._vertices = vertices;
  }

  public float height() {
    return _height;
  }

  public void setHeight(float height) {
    this._height = height;
  }

  public void setAngle(float angle){
    if(vertices().isEmpty()) return;
    //get the point who is farthest from the origin
    Vector max = vertices().get(0);
    for(Vector v : vertices()){
      if(v.magnitude() > max.magnitude()){
        max = v;
      }
    }
    float new_max = (float)(_height*Math.tan(angle));
    float alpha = new_max / max.magnitude();
    System.out.println("alp " + alpha);
    System.out.println("new_max " + new_max);

    for(Vector v : vertices()) {
      System.out.println("v " + v);
      v.multiply(alpha);
      System.out.println("v " + v);
    }
  }

  public PlanarPolygon() {
    _vertices = new ArrayList<Vector>();
    _restRotation = new Quaternion();
    _height = 5.f;
  }

  public PlanarPolygon(ArrayList<Vector> vertices, Quaternion restRotation, float height) {
    this._vertices = vertices;
    this._restRotation = restRotation.get();
    this._height = height;
    for (Vector v : _vertices)
      //Just not consider Z
      v.setZ(0);
    _setBoundingBox();
  }

  public PlanarPolygon(ArrayList<Vector> vertices, Quaternion restRotation) {
    this._vertices = vertices;
    this._restRotation = restRotation.get();
    for (Vector v : _vertices)
      //Just not consider Z
      v.setZ(0);
    _setBoundingBox();
  }

  public PlanarPolygon(ArrayList<Vector> vertices) {
    this._vertices = vertices;
    for (Vector v : _vertices)
      //Just not consider Z
      v.setZ(0);
    _setBoundingBox();
  }

  public Vector apply(Vector target) {
    return apply(target, _restRotation);
  }

  public Vector apply(Vector target, Quaternion restRotation) {
    Vector point = restRotation.inverse().multiply(target);
    if(point.z() == 0) point.setZ(0.5f);
    float alpha = Math.abs(_height/point.z());
    Vector proj = new Vector(alpha * point.x(), alpha * point.y());
    boolean inverse = point.z() * _height < 0;
    if (inverse || !_isInside(proj)) {
      //proj.multiply(inverse);
      Vector constrained = _closestPoint(proj);
      constrained.multiply(1.f/alpha);
      float z = inverse ? -point.z() : point.z();
      constrained.setZ(z);
      return restRotation.rotate(constrained);
    }
    return target;
  }

  protected void _setBoundingBox() {
    _min = new Vector();
    _max = new Vector();
    for (Vector v : _vertices) {
      if (v.x() < _min.x()) _min.setX(v.x());
      if (v.y() < _min.y()) _min.setY(v.y());
      if (v.x() > _max.x()) _max.setX(v.x());
      if (v.y() > _max.y()) _max.setY(v.y());
    }
  }

  /*Code was transcript from https://wrf.ecse.rpi.edu//Research/Short_Notes/pnpoly.html*/
  protected boolean _isInside(Vector point) {
    if (point.x() < _min.x() || point.x() > _max.x() ||
        point.y() < _min.y() || point.y() > _max.y()) return false;
    //Ray-casting algorithm
    boolean c = false;
    for (int i = 0, j = _vertices.size() - 1; i < _vertices.size(); j = i++) {
      Vector v_i = _vertices.get(i);
      Vector v_j = _vertices.get(j);
      if (((v_i.y() > point.y()) != (v_j.y() > point.y())) &&
          (point.x() < (v_j.x() - v_i.x()) * (point.y() - v_i.y()) / (v_j.y() - v_i.y()) + v_i.x()))
        c = !c;
    }
    return c;
  }

  protected Vector _closestPoint(Vector point) {
    float minDist = 999999;
    Vector target = new Vector();
    for (int i = 0, j = _vertices.size() - 1; i < _vertices.size(); j = i++) {
      Vector projection;
      float dist;
      Vector v_i = _vertices.get(i);
      Vector v_j = _vertices.get(j);
      Vector edge = Vector.subtract(v_i, v_j);
      //Get distance to line
      float t = Vector.dot(edge, Vector.subtract(point, v_j));
      t /= edge.magnitude() * edge.magnitude();

      if (t < 0) {
        dist = Vector.distance(v_j, point);
        projection = v_j.get();
      } else if (t > 1) {
        dist = Vector.distance(v_i, point);
        projection = v_i.get();
      } else {
        projection = Vector.add(v_j, Vector.multiply(edge, t));
        dist = Vector.subtract(point, projection).magnitude();
      }
      if (dist < minDist) {
        minDist = dist;
        target = projection;
      }
    }
    return target;
  }
}