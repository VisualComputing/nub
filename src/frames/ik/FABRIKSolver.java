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

package frames.ik;

import frames.core.Frame;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.DistanceFieldConstraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class FABRIKSolver extends Solver {
  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)

  public static class Properties{
    //Enable constraint or not
    protected boolean _useConstraint = true;
    protected boolean _enableFixWeight = true;
    //It is useful when the chain is highly constrained
    protected float _directionWeight = 0.3f;
    //When it is 1 the Joint will not move at all
    protected float _fixWeight = 0;


    public Properties(){
    }

    public Properties(boolean basic){
      if(basic){
        _useConstraint = false;
        _enableFixWeight = false;
      }
    }
  }

  protected HashMap<Frame, Properties> _properties = new HashMap<>();
  /*Store Joint's desired position*/
  protected ArrayList<Vector> _positions = new ArrayList<Vector>();
  protected ArrayList<Quaternion> _orientations = new ArrayList<Quaternion>();
  protected ArrayList<Float> _distances = new ArrayList<Float>();
  protected ArrayList<Vector> _positions() {
    return _positions;
  }
  protected int _head = 0;


  /*Move vector v to u while keeping certain distance*/
  public Vector _move(Vector u, Vector v, float distance, float fixWeight){
    float r = Vector.distance(u, v);
    float lambda_i = distance / r;
    lambda_i *= (1-fixWeight);
    Vector new_u = Vector.multiply(u, 1.f - lambda_i);
    new_u.add(Vector.multiply(v, lambda_i));
    return new_u;
  }

  public Vector _move(Vector u, Vector v, float distance) {
    return _move(u,v,distance, 0);
  }

  /*
   * Performs First Stage of FABRIK Algorithm, receives a chain of Frames, being the Frame at i
   * the reference frame of the Frame at i + 1
   * */
  public int opt = 0;

  protected float _forwardReaching(ArrayList<? extends Frame> chain) {
    float change = 0;
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = _positions.get(i);
      Vector pos_i1 = _positions.get(i + 1);
      float dist_i = _distances.get(i + 1);
      if (dist_i == 0) {
        _positions.set(i, pos_i1.get());
        continue;
      }
      Properties props_i = _properties.get(chain.get(i));
      Properties props_i1 = _properties.get(chain.get(i+1));
      if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
        Vector o_hat = chain.get(i + 1).position();
        Vector tr = Vector.subtract(pos_i, pos_i1);
        Vector n_tr = Vector.subtract(pos_i, o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), props_i._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        _positions.set(i, Vector.add(pos_i1, desired));
      }
      if(props_i1._useConstraint){
        pos_i = _constrainForwardReaching(chain, i);
      }
      if(props_i._enableFixWeight) {
        _positions.set(i, _move(pos_i1, pos_i, dist_i, props_i._fixWeight));
      } else{
        _positions.set(i, _move(pos_i1, pos_i, dist_i, 0));
      }
      change +=  Vector.distance(pos_i, _positions().get(i));
    }
    return change;
  }

  protected float _backwardReaching(ArrayList<? extends Frame> chain, Vector o) {
    float change = 0;
    Quaternion orientation;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    Vector o_hat = o;
    for (int i = 0; i < chain.size() - 1; i++) {
      if (_distances.get(i + 1) == 0) {
        _positions.set(i + 1, _positions.get(i));
        continue;
      }
      //Find delta rotation
      Properties props_i = _properties.get(chain.get(i));
      Properties props_i1 = _properties.get(chain.get(i+1));
      if  (chain.get(i+1).children().size() < 2 && chain.get(i+1).constraint() != null  && opt < 1){
        Vector tr = Vector.subtract(_positions.get(i + 1), chain.get(i).position());
        Vector n_tr = Vector.subtract(_positions.get(i + 1), o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(),props_i1._directionWeight* delta.angle());
        Vector desired = delta.rotate(tr);
        o_hat = _positions.get(i + 1);
        _positions.set(i + 1, Vector.add(chain.get(i).position(), desired));
      }
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.subtract(_positions.get(i + 1), _positions.get(i)));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      if(props_i._useConstraint)chain.get(i).rotate(deltaRotation);
      else{
        chain.get(i).setRotation(Quaternion.compose(chain.get(i).rotation(), deltaRotation));
      }
      orientation.compose(chain.get(i).rotation());
      _orientations.set(i, orientation.get());
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.add(_positions.get(i));
      change += Vector.distance(_positions.get(i + 1), constrained_pos);
      _positions.set(i + 1, constrained_pos);
    }
    return change;
  }

  /*
   * Check the type of the constraint related to the Frame Parent (at the i-th position),
   * Frame J is the frame used to verify if the orientation of Parent is appropriate,
   * Vector o is a Vector where Parent is located, whereas p is express the position of J
   * Vector q is the position of Child of J.
   * */

  public Vector _constrainForwardReaching(ArrayList<? extends Frame> chain, int i) {
    Frame j = chain.get(i + 1);
    Frame parent = chain.get(i + 1).reference();
    Vector o = _positions.get(i);
    Vector p = _positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : _positions.get(i + 2);

    if (parent.constraint() instanceof BallAndSocket) {
      if (q == null) return o.get();
      //Find the orientation of restRotation
      BallAndSocket constraint = (BallAndSocket) parent.constraint();
      Quaternion reference = Quaternion.compose(_orientations.get(i), parent.rotation().inverse());
      Quaternion restOrientation = Quaternion.compose(reference, constraint.restRotation());

      //Align axis
      Vector translation = _orientations.get(i).rotate(j.translation().get());
      Vector newTranslation = Vector.subtract(q, p);
      restOrientation = Quaternion.compose(new Quaternion(translation, newTranslation), restOrientation);

      //Vector pos_i1_constrained = _constrainForwardReaching(chain, i);
      //Vector diff = Vector.subtract(pos_i1, pos_i1_constrained);
      //pos_i.add(diff);

      //Find constraint
      Vector target = constraint.apply(Vector.subtract(p, o), restOrientation);
      target.add(o);
      Vector diff = Vector.subtract(p, target);
      return Vector.add(o, diff);
    } else if(j.constraint() instanceof PlanarPolygon){
      if(q == null) return o.get();
      Vector x = chain.get(i+1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i+1).position()));
      Vector y = chain.get(i+1).displacement(Vector.subtract(chain.get(i+2).position(), chain.get(i+1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i+1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i+1).displacement(w);
      Quaternion delta = new Quaternion(z,y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w,x);
      PlanarPolygon constraint = (PlanarPolygon) chain.get(i+1).constraint();
      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i+1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof Hinge) {
      if (q == null) return o.get();
      Vector x = chain.get(i+1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i+1).position()));
      Vector y = chain.get(i+1).displacement(Vector.subtract(chain.get(i+2).position(), chain.get(i+1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i+1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i+1).displacement(w);

      Quaternion delta = new Quaternion(z,y);

      w = delta.rotate(w);
      Hinge constraint = (Hinge) chain.get(i+1).constraint();

      //project w to plane
      w = Vector.projectVectorOnPlane(w, constraint.axis());

      Quaternion desired = new Quaternion(w,x);

      Quaternion constrained = constraint.constrainRotation(desired, chain.get(i+1));

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).worldDisplacement(target);
      target.add(p);
      return target;
    } else if (j.constraint() instanceof DistanceFieldConstraint) {
      if(q == null) return o.get();
      Vector x = chain.get(i+1).displacement(Vector.subtract(chain.get(i).position(), chain.get(i+1).position()));
      Vector y = chain.get(i+1).displacement(Vector.subtract(chain.get(i+2).position(), chain.get(i+1).position()));
      Vector z = Vector.subtract(q, p);
      z = chain.get(i+1).displacement(z);
      Vector w = Vector.subtract(o, p);
      w = chain.get(i+1).displacement(w);
      Quaternion delta = new Quaternion(z,y);

      w = delta.rotate(w);
      Quaternion desired = new Quaternion(w,x);
      DistanceFieldConstraint constraint = (DistanceFieldConstraint) chain.get(i+1).constraint();
      Quaternion constrained = Quaternion.compose(chain.get(i+1).rotation().get(),desired);
      constrained = constraint.apply(constrained);
      constrained = Quaternion.compose(chain.get(i+1).rotation().inverse(), constrained);

      Vector target = x.get();
      target.normalize();
      target.multiply(w.magnitude());
      target = constrained.inverseRotate(target);
      target = delta.inverseRotate(target);
      target = chain.get(i+1).worldDisplacement(target);
      target.add(p);

      return target;
    }
    return o.get();
  }

  protected float _distance(ArrayList<? extends Frame> chain) {
    float distance = 0.f;
    for (int i = 0; i < chain.size(); i++) {
      distance += Vector.distance(chain.get(i).position(), _positions.get(i));
    }
    return distance;
  }

  protected ArrayList<Frame> _copy(ArrayList<? extends Frame> chain) {
    ArrayList<Frame> copy = new ArrayList<Frame>();
    Frame reference = chain.get(0).reference();
    if (reference != null) {
      reference = new Frame(reference.position().get(), reference.orientation().get());
    }
    for (Frame joint : chain) {
      Frame newJoint = new Frame();
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }

  public ArrayList<Vector> positions(){
    return _positions;
  }

  /// TO DEBUG
  public ArrayList<Vector> copy_p(ArrayList<Vector> _positions){
    ArrayList<Vector> copy = new ArrayList<Vector>();
    for(Vector p : _positions){
      copy.add(p.get());
    }
    return copy;
  }

  public static PGraphics pg;
  public ArrayList<Vector> cop;

  public void draw_pos(){
    draw_pos(cop, pg.color(0,255,0), 3);
  }

  public void draw_pos(ArrayList<Vector> pos, int color, float str) {
    if(pos == null) return;
    Vector prev = null;
    for(Vector p : pos){
      pg.pushMatrix();
      pg.pushStyle();
      pg.stroke(color);
      pg.strokeWeight(str);
      if(prev != null) pg.line(prev.x(),prev.y(),prev.z(), p.x(),p.y(),p.z());
      pg.noStroke();
      pg.fill(color, 100);
      pg.translate(p.x(),p.y(),p.z());
      pg.sphere(3);
      pg.popStyle();
      pg.popMatrix();
      prev = p;
    }

  }

  public FABRIKSolver() {
    super();
  }
}