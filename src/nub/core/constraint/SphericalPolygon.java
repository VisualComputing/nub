/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.core.constraint;

import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;


/**
 * A Spherical polygon constraint is a {@link ConeConstraint} that defines the boundaries of the cone constraint
 * with a list of vertices of a polygon lying in the surface of a sphere.
 * This code is based on Efficient Spherical Joint Limits with Reach Cones
 * @see{https://users.soe.ucsc.edu/~avg/Papers/jtl-tr.pdf}
*/

public class SphericalPolygon extends ConeConstraint {
  protected ArrayList<Vector> _vertices = new ArrayList<Vector>();
  protected Vector _visiblePoint = new Vector(0,0,1);
  protected Vector _min, _max;

  //Some pre-computations
  protected ArrayList<Vector> _b = new ArrayList<Vector>();
  protected ArrayList<Vector> _s = new ArrayList<Vector>();

  public ArrayList<Vector> vertices() {
    return _vertices;
  }

  public void setVertices(List<Vector> vertices) {
    this._vertices = _projectOnUnitSphere(vertices);
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
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon(ArrayList<Vector> vertices) {
    this._vertices = _projectOnUnitSphere(vertices);
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon(float down, float up, float left, float right) {
    _setVertices(down, up, left, right, 24);
    _setBoundingBox();
    _init();
  }

  public SphericalPolygon(float vertical, float horizontal) {
    this(vertical, vertical, horizontal, horizontal);
  }

  public SphericalPolygon(float angle, float horizontal, Quaternion restRotation) {
    this(angle, angle, angle, angle);
  }

  public Vector apply(Vector target) {
    Vector point = target;
    if (!_isInside(point)) {
      Vector constrained = _closestPoint(point);
      return constrained;
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

  protected ArrayList<Vector> _projectOnUnitSphere(List<Vector> vertices) {
    ArrayList<Vector> newVertices = new ArrayList<Vector>();
    for (Vector vertex : vertices) {
      newVertices.add(vertex.normalize(new Vector()));
    }
    return newVertices;
  }

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
    float minDist = Float.MAX_VALUE;
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

  protected void _setVertices(float down, float up, float left, float right, int detail){
    int detailPerSector = Math.max(detail / 4, 2);
    _vertices = new ArrayList<Vector>();
    float cd = (float) Math.cos(up);
    float sd = (float) Math.sin(up);
    float cu = (float) Math.cos(down);
    float su = (float) Math.sin(down);
    float cl = (float) Math.cos(left);
    float sl = (float) Math.sin(left);
    float cr = (float) Math.cos(right);
    float sr = (float) Math.sin(right);
    Vector xl = new Vector(-sl, 0, cl);
    Vector xr = new Vector(sr, 0, cr);
    Vector yu = new Vector(0, su, cu);
    Vector yd = new Vector(0, -sd, cd);
    _constructEllipseQuadrant(xr, yu, detailPerSector, 0);
    _constructEllipseQuadrant(xl, yu, detailPerSector, (float) Math.toRadians(90));
    _constructEllipseQuadrant(xl, yd, detailPerSector, (float) Math.toRadians(180));
    _constructEllipseQuadrant(xr, yd, detailPerSector, (float) Math.toRadians(270));
  }

  protected void _constructEllipseQuadrant(Vector a, Vector b, int detail, float initial_theta){
    //find the projection of each semi axis in the plane
    Vector ap = _stereographicProjection(a);
    Vector bp = _stereographicProjection(b);
    float ap_mag = ap.magnitude();
    float bp_mag = bp.magnitude();
    //work in the plane
    float step = (float) Math.PI / (2 * detail);
    for(int i = 0; i < detail; i++){
      float theta = initial_theta + step * i;
      float bp_cos2 = (float) (bp_mag * Math.cos(theta));
      bp_cos2 *= bp_cos2;
      float ap_sin2 = (float) (ap_mag * Math.sin(theta));
      ap_sin2 *= ap_sin2;
      float r = (ap_mag * bp_mag) / (float) (Math.sqrt(bp_cos2 + ap_sin2));
      Vector v = new Vector((float) (r * Math.cos(theta)), (float) (r * Math.sin(theta)), 1);
      _vertices.add(_inverseStereographicProjection(v).normalize(null));
    }
  }
}