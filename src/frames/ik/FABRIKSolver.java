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
import frames.core.constraint.*;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public abstract class FABRIKSolver extends Solver {
  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)
  /*
  * Some ideas:
  * Increase of weight direction: Last segments must move a lot, but First ones must try
  * to keep it's direction, due last segments could be fixed by first ones ?
  * Weights depends on iterations: First iterations must be exploratory whereas last ones must
  * preserve directions ?
  *
  * FABRIK K-Steps
  * Direction D given by J_n and J_(n-1) is quite important:
  * Choose randomly k different directions:
  *     Axis : (J_n - J_(n-1)) cross (Target - J_(n-1))
  *     Angle : (J_n - J_(n-1)) dot (Target - J_(n-1))
  *     A_i = Angle + random(-1,1) * 2PI/k
  *     D_i = Rotate (J_n - J_(n-1)) by A_i angles w.r.t Axis
  *     J'_(n-1) = Target - D_i
  * Apply FABRIK k times in parallel with depth d
  * Dist^d_i  = dist(J'_(n-1-d), J_(n-1-d-1))
  * a. Discard all executions but i* s.t Dist_i* < Dist_i for i in (1,k)
  * Perform 1st phase of FABRIK. If dist(J'_0, J_0) is to Big discard (keep track of all Dist^d_i)
  * Execute repeat from a. and discard at depth d if Dist^d_i is worst than best execution so far.
  * Worst case O(k*FABRIK_Iteration_Complexity(n))
  *
  * FABRIK UP-DOWN (Further Thinking) - Basic idea : when movement is constrained, try to favor it by looking upward
  * Define a threshold Thr globally or per Joint.
  * Execute FABRIK as usual on fist Phase.
  * At time t:
  *   idx = n-1-t
  *   Possible conditions:
  *     1. dist(J'_(idx), J_(idx-1)) < Thr_(idx) and J_(idx) has constraint
  *     2. Movement to perform is far away due to constraint
  *   Get max movement with constaint
  *   Move J'_(idx)  according to obtained movement
  *   Execute Phase 2 and Get position of J_n
  *   ...
  *   Sub-problem Reach phi w.rt A axis while keeping J_n position
  *   Distribute phi in remaining chain J_i to J_n
  *   Calculate bounds phi_max_t, phi_min_t w.r.t A axis for each joint -> get phi_max_sum
  *   Define a way to propagate:
  *     phi_max_sum - phi_min_sum must always be > 0
  *
  *   There're some cases in which is desirable to Propagate all movement through Joints
  * */

  public static class Properties{
    //Enable constraint or not
    protected boolean _useConstraint = true;
    protected boolean _enableFixWeight = true;
    //It is useful when the chain is highly constrained
    protected float _directionWeight = 0.5f;
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

  protected HashMap<Integer, Properties> _properties = new HashMap<>();
  /*Store Joint's desired position*/
  protected ArrayList<Vector> _positions = new ArrayList<Vector>();
  protected ArrayList<Quaternion> _orientations = new ArrayList<Quaternion>();
  protected ArrayList<Float> _distances = new ArrayList<Float>();
  protected ArrayList<Float> _jointChange = new ArrayList<Float>();
  protected ArrayList<Vector> _positions() {
    return _positions;
  }
  protected int _head = 0;

  /*Heuristic Parameters*/
  protected boolean _fixTwisting = false;
  protected boolean _keepDirection = true;

  //TODO : Clean code
  static Random r = new Random();
  static boolean rand = false;

  /*
  * Move vector u to v while keeping certain distance.
  *
  * Find the point that lies on the segment v - u that is at the given distance to
  * Point u.
  * Fix weight is a parameter in range [0,1] that will scale the distance between V and
  * the found point. A bigger value of this parameter implies to perform a smaller movement.
  * */
  public boolean fixTwisting(){
    return  _fixTwisting;
  }

  public boolean keepDirection(){
    return _keepDirection;
  }

  public void setFixTwisting(boolean fixTwisting){
    _fixTwisting = fixTwisting;
  }

  public void setKeepDirection(boolean keepDirection){
    _keepDirection = keepDirection;
  }


  public static Vector _move(Vector u, Vector v, float distance, float fixWeight){
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
  protected float _forwardReaching(ArrayList<? extends Frame> chain) {
    if(_fixTwisting) _applyTwistRotation(chain, _positions.get(_positions.size() - 1));
    float change = 0;
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = _positions.get(i);
      Vector pos_i1 = _positions.get(i + 1);
      float dist_i = _distances.get(i + 1);
      if (dist_i == 0) {
        //As rigid segment between J_i and J_i1 lies on same position, J_i must match exactly J_i1 position
        _positions.set(i, pos_i1.get());
        continue;
      }

      Properties props_i = _properties.get(chain.get(i).id());
      Properties props_i1 = _properties.get(chain.get(i+1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if(chain.get(i).constraint() != null && _keepDirection){
        /*
         * H1 : If Joint has a constrat it is desirable that:
         * a) J_i reach its target position
         * b) J_i keeps its orientation
         * */
        Vector o_hat = chain.get(i + 1).position();
        Vector tr = Vector.subtract(pos_i, pos_i1);
        Vector n_tr = Vector.subtract(pos_i, o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float)(Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
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


  protected static float _forwardReaching(ArrayList<? extends Frame> chain, ArrayList<Vector> positions, ArrayList<Float> distances, HashMap<Integer, Properties> properties, boolean kd) {
    float change = 0;
    for (int i = chain.size() - 2; i >= 0; i--) {
      Vector pos_i = positions.get(i);
      Vector pos_i1 = positions.get(i + 1);
      float dist_i = distances.get(i + 1);
      if (dist_i == 0) {
        //As rigid segment between J_i and J_i1 lies on same position, J_i must match exactly J_i1 position
        positions.set(i, pos_i1.get());
        continue;
      }

      Properties props_i = properties.get(chain.get(i).id());
      Properties props_i1 = properties.get(chain.get(i + 1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if(chain.get(i).constraint() != null && kd){
        Vector o_hat = chain.get(i + 1).position();
        Vector tr = Vector.subtract(pos_i, pos_i1);
        Vector n_tr = Vector.subtract(pos_i, o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(), rand ? (float)(Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        positions.set(i, Vector.add(pos_i1, desired));
      }
      if(props_i1._useConstraint){
        pos_i = _constrainForwardReaching(chain, positions, i);
      }
      if(props_i._enableFixWeight) {
        positions.set(i, _move(pos_i1, pos_i, dist_i, props_i._fixWeight));
      } else{
        positions.set(i, _move(pos_i1, pos_i, dist_i, 0));
      }
      change +=  Vector.distance(pos_i, positions.get(i));
    }
    return change;
  }


  //TODO : Check for scaling when chain has constraints
  protected static float _backwardReaching(ArrayList<? extends Frame> chain, ArrayList<Vector> positions, ArrayList<Float> distances, HashMap<Integer, Properties> properties,  boolean kd, Vector o) {
    float change = 0;
    Quaternion orientation;
    float magnitude;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    magnitude = chain.get(0).reference() != null ? chain.get(0).reference().scaling() : 1;

    Vector o_hat = o;
    for (int i = 0; i < chain.size() - 1; i++) {
      if (distances.get(i + 1) == 0) {
        positions.set(i + 1, positions.get(i));
        continue;
      }
      magnitude *= chain.get(i).scaling();
      //Find delta rotation
      Properties props_i = properties.get(chain.get(i).id());
      Properties props_i1 = properties.get(chain.get(i+1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if(chain.get(i).constraint() != null && kd){
        Vector tr = Vector.subtract(positions.get(i + 1), chain.get(i).position());
        Vector n_tr = Vector.subtract(positions.get(i + 1), o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(),rand ? (float)(Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        o_hat = positions.get(i + 1);
        positions.set(i + 1, Vector.add(chain.get(i).position(), desired));
      }
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.divide(Vector.subtract(positions.get(i + 1), positions.get(i)), magnitude));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      Quaternion prev = chain.get(i).rotation().get();
      if(props_i._useConstraint)chain.get(i).rotate(deltaRotation);
      else{
        chain.get(i).setRotation(Quaternion.compose(chain.get(i).rotation(), deltaRotation));
      }
      //Get how much change was applied:
      change = Math.max(Quaternion.compose(prev.inverse(), chain.get(i).rotation()).angle(), change);
      orientation.compose(chain.get(i).rotation());
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.multiply(magnitude);
      constrained_pos.add(positions.get(i));
      //change += Vector.distance(_positions.get(i + 1), constrained_pos);
      positions.set(i + 1, constrained_pos);
    }
    return change;
  }

  //TODO : All this code is a mess, remove duplicate code later, this is jusst to prove ideas
  protected float _backwardReaching(ArrayList<? extends Frame> chain, Vector o) {
    float change = 0;
    Quaternion orientation;
    float magnitude;
    orientation = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
    magnitude = chain.get(0).reference() != null ? chain.get(0).reference().scaling() : 1;

    Vector o_hat = o;
    for (int i = 0; i < chain.size() - 1; i++) {
      if (_distances.get(i + 1) == 0) {
        _positions.set(i + 1, _positions.get(i));
        continue;
      }
      magnitude *= chain.get(i).scaling();
      //Find delta rotation
      Properties props_i = _properties.get(chain.get(i).id());
      Properties props_i1 = _properties.get(chain.get(i+1).id());
      //TODO : Is necessary to check children?
      //if(chain.get(i).children().size() < 2 && chain.get(i).constraint() != null && opt < 1){
      if(chain.get(i).constraint() != null && _keepDirection){
        Vector tr = Vector.subtract(_positions.get(i + 1), chain.get(i).position());
        Vector n_tr = Vector.subtract(_positions.get(i + 1), o_hat);
        Quaternion delta = new Quaternion(tr, n_tr);
        delta = new Quaternion(delta.axis(),rand ? (float)(Math.abs(r.nextGaussian())) : 1 * props_i1._directionWeight * delta.angle());
        Vector desired = delta.rotate(tr);
        o_hat = _positions.get(i + 1);
        _positions.set(i + 1, Vector.add(chain.get(i).position(), desired));
      }
      Vector newTranslation = Quaternion.compose(orientation, chain.get(i).rotation()).inverse().rotate(Vector.divide(Vector.subtract(_positions.get(i + 1), _positions.get(i)), magnitude));
      Quaternion deltaRotation = new Quaternion(chain.get(i + 1).translation(), newTranslation);
      //Apply delta rotation
      Quaternion prev = chain.get(i).rotation().get();
      if(props_i._useConstraint)chain.get(i).rotate(deltaRotation);
      else{
        chain.get(i).setRotation(Quaternion.compose(chain.get(i).rotation(), deltaRotation));
      }
      //Get how much change was applied:
      float jointChange = Quaternion.compose(prev.inverse(), chain.get(i).rotation()).angle();
      change = Math.max(jointChange, change);

      _jointChange.set(i, _jointChange.get(i) + jointChange);
      orientation.compose(chain.get(i).rotation());
      _orientations.set(i, orientation.get());
      Vector constrained_pos = orientation.rotate(chain.get(i + 1).translation().get());
      constrained_pos.multiply(magnitude);
      constrained_pos.add(_positions.get(i));
      //change += Vector.distance(_positions.get(i + 1), constrained_pos);
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
    Vector o = _positions.get(i);
    Vector p = _positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : _positions.get(i + 2);

    if(j.constraint() instanceof ConeConstraint){
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
      ConeConstraint constraint = (ConeConstraint) chain.get(i+1).constraint();
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

  public static Vector _constrainForwardReaching(ArrayList<? extends Frame> chain, ArrayList<Vector> positions, int i) {
    Frame j = chain.get(i + 1);
    Vector o = positions.get(i);
    Vector p = positions.get(i + 1);
    Vector q = i + 2 >= chain.size() ? null : positions.get(i + 2);

    if(j.constraint() instanceof ConeConstraint){
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
      ConeConstraint constraint = (ConeConstraint) chain.get(i+1).constraint();
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


  protected void _forwardTwist(ArrayList<Vector> positions, int idx,Vector o, Vector p, Vector q, Vector t){
    //1. Get The vector normal to Plane of interest
    Vector n = Vector.subtract(p, o);
    //2. Project Vector q,p into Plane
    Vector q_proj = Vector.projectVectorOnPlane(Vector.subtract(q,p), n);
    //3. Project Vector t,p into Plane
    Vector t_proj = Vector.projectVectorOnPlane(Vector.subtract(t,p), n);
    //4. Find angle between projected Vectors
    Quaternion theta = new Quaternion(q_proj, t_proj);
    //5. Apply rotation and update Positions from o to head
    _applyRotation(true, positions, idx, theta);

  }

  float min_v = 1e10f, min_u = 1e10f;

  protected void _applyTwistRotation(ArrayList<? extends Frame> chain, Vector t){
    //Change chain state in such a way that the target approach to chain
    Frame eff = chain.get(chain.size() - 1);
    for(int i = 0; i < chain.size() - 2; i++){
      Frame f_i = chain.get(i);
      //Project Vectors to Plane given by Twist Vector
      Vector twist = chain.get(i + 1).translation();
      //Get Vector from EFF to this Joint
      Vector localEff = f_i.location(eff.position());
      Vector v = Vector.projectVectorOnPlane(localEff, twist);
      //Get Vector from EFF to this Joint
      Vector u = Vector.projectVectorOnPlane(f_i.location(t), twist);
      //Perform this operation only when Projected Vectors have not a despicable length
      if(v.magnitude() > 0.1 * localEff.magnitude() && u.magnitude() > 0.1 * localEff.magnitude()) {
        Quaternion q = new Quaternion(v, u);
        //Perform this operation only when change is not despicable
        if(q.angle() > Math.toRadians(5)){
          f_i.rotate(q);
        }
      }
    }
  }

  protected void _applyRotation(boolean reverse, ArrayList<Vector> positions, int idx, Quaternion q){
    Vector v_i = positions.get(idx);
    int update = reverse ? -1 : 1;
    for(int i = idx + update; reverse ? i >=  0: i < positions.size(); i = i + update ){
      Vector v_j = Vector.subtract(positions.get(i), v_i);
      v_j = q.rotate(v_j);
      v_j.add(v_i);
      positions.set(i, v_j);
    }
  }


  protected float _distance(ArrayList<? extends Frame> chain) {
    float distance = 0.f;
    for (int i = 0; i < chain.size(); i++) {
      distance += Vector.distance(chain.get(i).position(), _positions.get(i));
    }
    return distance;
  }

  protected ArrayList<Frame> _copy(List<? extends Frame> chain, Frame reference) {
    ArrayList<Frame> copy = new ArrayList<Frame>();
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

  protected ArrayList<Frame> _copy(ArrayList<? extends Frame> chain) {
    Frame reference = chain.get(0).reference();
    if (reference != null) {
      reference = new Frame(reference.position().get(), reference.orientation().get(), 1);
    }
    return _copy(chain, reference);
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