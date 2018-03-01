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

public class SphericalPolygon extends Constraint {
    /*
    TODO: Enable Setting different Axis Direction
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on a Cone which base is a Spherical Polygon. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */

  protected ArrayList<Vector> _vertices = new ArrayList<Vector>();
  protected Vector _visiblePoint = new Vector();
  protected Quaternion _restRotation = new Quaternion();
  protected Vector _min, _max;

  //Some pre-computations
  protected ArrayList<Vector> _b = new ArrayList<Vector>();
  protected ArrayList<Vector> _s = new ArrayList<Vector>();


  public Quaternion restRotation() {
    return _restRotation;
  }

  public void setRestRotation(Quaternion restRotation) {
    this._restRotation = restRotation.get();
  }

  public ArrayList<Vector> vertices() {
    return _vertices;
  }

  public void setVertices(ArrayList<Vector> vertices) {
    this._vertices = projectOnUnitSphere(vertices);
    this._visiblePoint = computeVisiblePoint();
    computeBoundingBox();
    doPrecomputations();
  }


  public SphericalPolygon() {
    _vertices = new ArrayList<Vector>();
    _restRotation = new Quaternion();
    _visiblePoint = new Vector(0, 0, 1);
  }

  public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation, Vector visiblePoint) {
    this._vertices = projectOnUnitSphere(vertices);
    this._restRotation = restRotation.get();
    this._visiblePoint = visiblePoint;
    visiblePoint.normalize();
    computeBoundingBox();
    doPrecomputations();
  }

  public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation) {
    this._vertices = projectOnUnitSphere(vertices);
    this._restRotation = restRotation.get();
    this._visiblePoint = computeVisiblePoint();
    computeBoundingBox();
    doPrecomputations();
  }

  public SphericalPolygon(ArrayList<Vector> vertices) {
    this._vertices = projectOnUnitSphere(vertices);
    this._visiblePoint = computeVisiblePoint();
    computeBoundingBox();
    doPrecomputations();
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        /*
        if(frame.is2D())
            throw new RuntimeException("This constrained not supports 2D Frames");
        */
    Quaternion desired = Quaternion.compose(frame.rotation(), rotation);
    Vector new_pos = Quaternion.multiply(desired, new Vector(0, 0, 1));
    Vector constrained = constraint(new_pos, _restRotation);
    //Get Quaternion
    return new Quaternion(new Vector(0, 0, 1), Quaternion.multiply(frame.rotation().inverse(), constrained));
  }


  @Override
  public Vector constrainTranslation(Vector translation, Frame frame) {
    return new Vector(0, 0, 0);
  }

  public Vector constraint(Vector target) {
    return constraint(target, _restRotation);
  }

  public Vector constraint(Vector target, Quaternion restRotation) {
    Vector point = restRotation.inverse().multiply(target);
    if (!isInside(point)) {
      Vector constrained = closestPoint(point);
      return restRotation.rotate(constrained);
    }
    return target;
  }

  public void computeBoundingBox() {
    _min = new Vector();
    _max = new Vector();
    for (Vector v : _vertices) {
      if (v.x() < _min.x()) _min.setX(v.x());
      if (v.y() < _min.y()) _min.setY(v.y());
      if (v.z() < _min.z()) _min.setZ(v.z());
      if (v.x() > _max.x()) _max.setX(v.x());
      if (v.y() > _max.y()) _max.setY(v.y());
      if (v.z() > _max.z()) _max.setZ(v.z());
    }
  }

  //Compute centroid
  //TO DO: Choose a Visible point which works well for non convex Polygons
  public Vector computeVisiblePoint() {
    if (_vertices.isEmpty()) return null;
    Vector centroid = new Vector();
    //Assume that every vertex lie in the sphere boundary
    for (Vector vertex : _vertices) {
      centroid.add(vertex);
    }
    centroid.normalize();
    return centroid;
  }

  public ArrayList<Vector> projectOnUnitSphere(ArrayList<Vector> vertices) {
    ArrayList<Vector> newVertices = new ArrayList<Vector>();
    for (Vector vertex : vertices) {
      newVertices.add(vertex.normalize(new Vector()));
    }
    return newVertices;
  }

  //TODO: seems this one should be protected
  public void doPrecomputations() {
    _b = new ArrayList<Vector>();
    _s = new ArrayList<Vector>();
    for (int i = 0; i < _vertices.size(); i++) {
      Vector p_i = _vertices.get(i);
      Vector p_j = i + 1 == _vertices.size() ? _vertices.get(0) : _vertices.get(i + 1);
      _s.add(Vector.cross(_visiblePoint, p_i, null));
      _b.add(Vector.cross(p_i, p_j, null));
    }
  }

  public boolean isInside(Vector L) {
    //1. Find i s.t p_i = S_i . L >= 0 and p_j = S_j . L < 0 with j = i + 1
    int index = 0;
    for (int i = 0; i < _vertices.size(); i++) {
      if (Vector.dot(_s.get(i), L) >= 0 && Vector.dot(_s.get((i + 1) % _vertices.size()), L) < 0) {
        index = i;
        break;
      }
    }
    return Vector.dot(_b.get(index), L) >= 0;
  }

  public Vector closestPoint(Vector point) {
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