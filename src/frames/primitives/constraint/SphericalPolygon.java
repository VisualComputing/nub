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

  /**
   * reference is a Quaternion that will be aligned to point to the given Basis Vectors
   * result will be stored on restRotation.
   * twist and up axis are defined locally on reference rotation
   * */
  public void setRestRotation(Quaternion reference, Vector up, Vector twist){
    _restRotation = reference.get();
    Vector Z = _restRotation.inverse().rotate(twist);
    //Align Y-Axis with Up Axis
    _restRotation.compose(new Quaternion(new Vector(0,1,0), up));
    //Align y-Axis with twist vector
    _restRotation.compose(new Quaternion(new Vector(0,0,1), twist));
  }

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
    this._vertices = _projectOnUnitSphere(vertices);
    this._visiblePoint = _setVisiblePoint();
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon() {
    _vertices = new ArrayList<Vector>();
    _restRotation = new Quaternion();
    _visiblePoint = new Vector(0, 0, 1);
  }

  public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation, Vector visiblePoint) {
    this._vertices = _projectOnUnitSphere(vertices);
    this._restRotation = restRotation.get();
    this._visiblePoint = visiblePoint;
    visiblePoint.normalize();
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon(ArrayList<Vector> vertices, Quaternion restRotation) {
    this._vertices = _projectOnUnitSphere(vertices);
    this._restRotation = restRotation.get();
    this._visiblePoint = _setVisiblePoint();
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon(ArrayList<Vector> vertices) {
    this._vertices = _projectOnUnitSphere(vertices);
    this._visiblePoint = _setVisiblePoint();
    _setBoundingBox();
    _init();
  }

  @Override
  public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        /*
        if(frame.is2D())
            throw new RuntimeException("This constrained not supports 2D Frames");
        */
    Quaternion desired = Quaternion.compose(frame.rotation(), rotation);
    //twist to frame
    Vector twist = _restRotation.rotate(new Vector(0,0,1));
    Vector new_pos = Quaternion.multiply(desired, twist);
    Vector constrained = apply(new_pos, _restRotation);
    //Get Quaternion
    return new Quaternion(twist, Quaternion.multiply(frame.rotation().inverse(), constrained));
  }


  @Override
  public Vector constrainTranslation(Vector translation, Frame frame) {
    return new Vector(0, 0, 0);
  }

  public Vector apply(Vector target) {
    return apply(target, _restRotation);
  }

  public Vector apply(Vector target, Quaternion restRotation) {
    Vector point = restRotation.inverse().multiply(target);
    if (!_isInside(point)) {
      Vector constrained = _closestPoint(point);
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
      if (v.z() < _min.z()) _min.setZ(v.z());
      if (v.x() > _max.x()) _max.setX(v.x());
      if (v.y() > _max.y()) _max.setY(v.y());
      if (v.z() > _max.z()) _max.setZ(v.z());
    }
  }

  //Compute centroid
  //TODO: Choose a Visible point which works well for non convex Polygons
  protected Vector _setVisiblePoint() {
    if (_vertices.isEmpty()) return null;
    Vector centroid = new Vector();
    //Assume that every vertex lie in the sphere boundary
    for (Vector vertex : _vertices) {
      centroid.add(vertex);
    }
    centroid.normalize();
    return centroid;
  }

  protected ArrayList<Vector> _projectOnUnitSphere(ArrayList<Vector> vertices) {
    ArrayList<Vector> newVertices = new ArrayList<Vector>();
    for (Vector vertex : vertices) {
      newVertices.add(vertex.normalize(new Vector()));
    }
    return newVertices;
  }

  //TODO: seems this one should be protected
  protected void _init() {
    _b = new ArrayList<Vector>();
    _s = new ArrayList<Vector>();
    for (int i = 0; i < _vertices.size(); i++) {
      Vector p_i = _vertices.get(i);
      Vector p_j = i + 1 == _vertices.size() ? _vertices.get(0) : _vertices.get(i + 1);
      _s.add(Vector.cross(_visiblePoint, p_i, null));
      _b.add(Vector.cross(p_i, p_j, null));
    }
  }

  protected boolean _isInside(Vector L) {
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