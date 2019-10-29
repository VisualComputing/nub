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

package nub.ik.solver.geometric;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.ik.animation.VisualizerMediator;
import nub.ik.solver.KinematicStructure;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.*;

public class ChainSolver extends FABRIKSolver {
  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)
  protected List<Node> _chain;
  protected List<? extends Node> _original;
  protected float _current = 10e10f, _best = 10e10f;
  protected Node _target;
  protected Node _prevTarget;
  protected boolean _is3D = true;

  //TODO : Check this ideas, clean and refine (*)
  //----------------------------------------------------
  protected Vector _targetDirection;
  protected ArrayList<Vector> _bestAvoidPosition, _afterAvoidPosition;
  protected ArrayList<ArrayList<Vector>> _avoidHistory, _divergeHistory;
  protected boolean _explore = true;
  protected float _explorationTimes = 0;
  protected float _lastError;
  //----------------------------------------------------

  public void explore(boolean explore) {
    _explore = explore;
  }

  public boolean explore() {
    return _explore;
  }

  public float explorationTimes() {
    return _explorationTimes;
  }

  public ArrayList<Vector> bestAvoidPosition() {
    return _bestAvoidPosition;
  }

  public ArrayList<Vector> afterAvoidPosition() {
    return _afterAvoidPosition;
  }

  public ArrayList<Vector> positions() {
    return _positions;
  }

  public ArrayList<ArrayList<Vector>> avoidHistory() {
    return _avoidHistory;
  }

  public ArrayList<ArrayList<Vector>> divergeHistory() {
    return _divergeHistory;
  }

  //TODO : clean
  public List<? extends Node> internalChain() {
    return _chain;
  }

  public List<? extends Node> chain() {
    return _original;
  }

  public Node target() {
    return _target;
  }

  public void setTarget(Node endEffector, Node target) {
    this._target = target;
  }

  public void setTargetDirection(Vector direction) { //TODO : clean
    _targetDirection = direction;
  }

  public void setTarget(Node target) {
    this._target = target;
  }

  public Node head() {
    return _original.get(0);
  }

  public Node endEffector() {
    return _original.get(_original.size() - 1);
  }

  public ChainSolver(List<? extends Node> chain) {
    this(chain, null);
  }

  public ChainSolver(List<? extends Node> chain, List<Node> copy, Node target) {
    super();
    this._original = chain;
    this._chain = copy == null ? _copy(chain) : copy;
    _positions = new ArrayList<Vector>();
    _distances = new ArrayList<Float>();
    _jointChange = new ArrayList<Float>();
    _orientations = new ArrayList<Quaternion>();
    Vector prevPosition = chain.get(0).reference() != null
        ? chain.get(0).reference().position().get() : new Vector(0, 0, 0);
    Quaternion prevOrientation = chain.get(0).reference() != null
        ? chain.get(0).reference().orientation().get() : new Quaternion();
    for (Node joint : _chain) {
      _properties.put(joint.id(), new Properties(false));
      Vector position = joint.position().get();
      Quaternion orientation = prevOrientation.get();
      orientation.compose(joint.rotation().get());
      _positions.add(position);
      float mag = Vector.subtract(position, prevPosition).magnitude();
      _distances.add(mag <= 10e-4 ? 0 : mag);
      _jointChange.add(0f);
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
    _is3D = chain.get(0).graph().is3D();
    if (!_is3D) _fixTwisting = false;
    _jointChange.remove(0);
    this._target = target;
    this._prevTarget =
        target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
    //TODO : REFINE
    //_generateGlobalConstraints();
  }

  public ChainSolver(List<? extends Node> chain, Node target) {
    this(chain, null, target);
  }

  /*Get maximum length of a given chain*/
  protected float _length() {
    float dist = 0;
    for (int i = 1; i < _chain.size(); i++) {
      dist += _chain.get(i).translation().magnitude() / _chain.get(i).magnitude();
    }
    return dist;
  }

  protected void _stretch(ArrayList<? extends Node> chain, Vector target) {
    for (int i = 0; i < chain.size() - 1; i++) {
      //Get the distance between Joint i and the Target
      Vector pos_i = _positions.get(i);
      float r_i = Vector.distance(pos_i, target);
      float dist_i = chain.get(i + 1).translation().magnitude() / chain.get(i + 1).magnitude();
      float lambda_i = dist_i / r_i;
      Vector new_pos = Vector.multiply(pos_i, 1.f - lambda_i);
      new_pos.add(Vector.multiply(target, lambda_i));
      _positions.set(i + 1, new_pos);
    }
  }

  /*
   * Performs a FABRIK ITERATION
   */
  @Override
  protected boolean _iterate() {
    //As no target is specified there is no need to perform FABRIK
    if (_target == null) return true;
    _current = 0;
    Node end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();
    //TODO: Check blocked and oscillation decisions based on "e"
    _lastError = _iterations % 2 == 0 ? Vector.distance(end.position(), _target.position()) : Math.max(Vector.distance(end.position(), _target.position()), _lastError);
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= _maxError) {
      return true;
    }
    //Get the distance between the Root and the End Effector
    //float length = _length();
    //Get the distance between the Root and the Target
    //float dist = Vector.distance(root.position(), target);
    //When Target is unreachable
    /*if(dist > length){
        stretchChain(chain, target);
        return true;
    }else{*/
    //Initial root position
    Vector initial = _chain.get(0).position().get();
    //Stage 1: Forward Reaching
    //TODO : Check for a better approach to include Twist (*)
    if (_fixTwisting && _iterations % 3 == 0) {
      _applyTwistRotation(_chain, target);
      //TODO : Do it efficiently
      for (int i = 0; i < _chain.size(); i++) {
        _positions.set(i, _chain.get(i).position());
      }
      //if(debug) addIterationRecord(_positions);
    }

    //TODO : Clean and consider twisting
    if (_targetDirection != null) {
      _applyTargetdirection();
    }
    _positions.set(_chain.size() - 1, target.get());

    //Animation
    _forwardReaching();
    //Animation stuff
    //if(debug) addIterationRecord(_positions);
    //Stage 2: Backward Reaching
    Vector o = _positions.get(0);
    _positions.set(0, initial);

    //Animation
    float change = _backwardReaching(o);
    //Save best solution
    float currentError = Vector.distance(end.position(), _target.position());
    if (_explore && currentError > _maxError) {
      if (debug)
        System.out.println("ne is greater than _maxError : " + " ne : " + currentError + " _maxError : " + _maxError);
      if (debug) System.out.println("change is : " + change);
      if (debug) System.out.println("e is : " + _lastError);

      if ((change < 10e-6) || (_iterations % 2 == 1 && currentError > _lastError)) {
        boolean avoid = currentError > _lastError;
        for (Float jc : _jointChange) {
          if (debug) System.out.print(jc + " , ");
        }
        if (debug) System.out.println();
        //avoid deadLock
        if (debug) System.out.println(avoid ? "AVOID" : "EXPLORE");
        _avoidDeadlock(avoid);
      }
    }
    _current = Vector.distance(target, _chain.get(_chain.size() - 1).position());
    _update();
    //if(debug) addIterationRecord(_positions);
    //Check total position change
    return false;
  }

  protected void _forwardReaching() {
    _forwardReaching(_chain);
  }

  protected float _backwardReaching(Vector o, int start) {
    return _backwardReaching(_chain, o, start);
  }


  protected float _backwardReaching(Vector o) {
    return _backwardReaching(_chain, o, 0);
  }

  @Override
  protected void _update() {
    if (_current < _best) {
      for (int i = 0; i < _original.size(); i++) {
        _original.get(i).setRotation(_chain.get(i).rotation().get());
      }
      _best = _current;
    }
  }

  @Override
  protected boolean _changed() {
    if (_target == null) {
      _prevTarget = null;
      return false;
    } else if (_prevTarget == null) {
      return true;
    }
    return !(_prevTarget.position().matches(_target.position()) && _prevTarget.orientation().matches(_target.orientation()));
  }

  @Override
  protected void _reset() {
    _prevTarget = _target == null ? null : new Node(_target.position().get(), _target.orientation().get(), 1);
    _iterations = 0;
    _explorationTimes = 0;
    //We know that State has change but not where, then it is better to reset Global Positions and Orientations
    _init();
    if (_target != null) {
      _best = Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
    } else {
      _best = 10e10f;
    }
  }

  @Override
  public float error() {
    return Vector.distance(_original.get(_original.size() - 1).position(), _target.position());
  }

  protected void _init() {
    //Initialize List with info about Positions and Orientations
    if (_original.get(0).reference() != null) {
      _chain.get(0).reference().setMagnitude(_original.get(0).reference().magnitude());
      _chain.get(0).reference().setOrientation(_original.get(0).reference().orientation().get());
      _chain.get(0).reference().setPosition(_original.get(0).reference().position().get());
    }

    for (int i = 0; i < _chain.size(); i++) {
      _chain.get(i).setScaling(_original.get(i).scaling());
      _chain.get(i).setRotation(_original.get(i).rotation().get());
      _chain.get(i).setTranslation(_original.get(i).translation().get());
    }
    _positions = new ArrayList<Vector>();
    _distances = new ArrayList<Float>();
    //TODO : rename and clean jointChange
    _jointChange = new ArrayList<Float>();
    _orientations = new ArrayList<Quaternion>();

    Vector prevPosition = _chain.get(0).reference() != null
        ? _chain.get(0).reference().position().get() : new Vector(0, 0, 0);
    Quaternion prevOrientation = _chain.get(0).reference() != null
        ? _chain.get(0).reference().orientation().get() : new Quaternion();
    for (Node node : _chain) {
      if (!_properties.containsKey(node.id())) {
        Properties props = new Properties(false); //TODO : CLEAN!!!
        _properties.put(node.id(), props);
      }
      Vector position = node.position().get();
      Quaternion orientation = prevOrientation.get();
      orientation.compose(node.rotation().get());
      _positions.add(position);
      float mag = Vector.subtract(position, prevPosition).magnitude();
      _distances.add(mag <= 10e-4 ? 0 : mag);
      _jointChange.add(0f);
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
    _jointChange.remove(0);
    _explorationTimes = 0;
    //TODO : REFINE & USE ONLY IN DEBUG MODE
    _avoidHistory = new ArrayList<>();
    _divergeHistory = new ArrayList<>();
    //TODO : REFINE
    //_generateGlobalConstraints();
  }

  protected void _applyTargetdirection() {
    //Use target orientation
    Vector o = _chain.get(_chain.size() - 2).position();
    Vector p = _chain.get(_chain.size() - 1).position();
    Vector op = Vector.subtract(o, p);
    Vector direction = _target.worldDisplacement(_targetDirection);
    direction.normalize();
    direction.multiply(-1);
    direction.multiply(op.magnitude());
    _positions.set(_chain.size() - 2, Vector.add(direction, p));
  }

  public ArrayList<Vector> dir_temp = new ArrayList<>();
  public ArrayList<Vector> dir_temp_i = new ArrayList<>();

  protected void _avoidDeadlock(boolean avoid) {
    if (_chain.size() <= 1) return;
    _explorationTimes++;
    float a;
    List<Node> b_copy = null;
    float b_d = Float.POSITIVE_INFINITY;
    Vector target = _target.position();

    //Sort Joints by change
    Integer[] indices = new Integer[_jointChange.size()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }

    Arrays.sort(indices, new Comparator<Integer>() {
      public int compare(Integer i1, Integer i2) {
        return avoid ? -_jointChange.get(i1).compareTo(_jointChange.get(i2)) : _jointChange.get(i1).compareTo(_jointChange.get(i2));
      }
    });

    //TODO : think about exploration strategies
    if (debug) System.out.println("sorted change : ");
    for (int idx : indices) {
      if (debug) System.out.print(_jointChange.get(idx) + " , ");
    }
    if (debug) System.out.println();

    for (int i = 0; i < 20; i++) {
      List<Node> copy = i < 10 ? _copy(_chain) : _copy(b_copy);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      for (Node f : copy) {
        copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
        if (j < _jointChange.size() && i < 10) {
          //focus on change joints who preserves direction
          a = 1 - i / (_jointChange.size() - 1);
          a = (float) (a * 180 * Math.PI / 180);
        } else {
          a = (float) (180 * Math.PI / 180);
        }
        if (j < _jointChange.size()) {
          float x = (float) (2 * Math.random() * a - a);
          float y = (float) (2 * Math.random() * a - a);
          float z = _is3D ? (float) (2 * Math.random() * a - a) : 0;
          f.rotate(new Quaternion(x, y, z));
        }
        copy_p.add(f.position().get());
      }

      /*copy_p.set(_chain.size() - 1, this._target.position().get());
      //if(_fixTwisting) _applyTwistRotation(copy, copy_p.get(copy_p.size() - 1));
      _forwardReaching(copy, copy_p, _distances, copy_props, _keepDirection);

      Vector o = copy_p.get(0);
      copy_p.set(0, _chain.get(0).position());
      _backwardReaching(copy, copy_p, _distances, copy_props , _keepDirection, o);*/

      float d = Vector.distance(copy_p.get(copy_p.size() - 1), target);
      if (d <= b_d) {
        b_d = d;
        b_copy = copy;
        _bestAvoidPosition = copy_p;
        if (avoid) _avoidHistory.add(_bestAvoidPosition);
        else _divergeHistory.add(_bestAvoidPosition);
        if (debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if (debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }

    for (int i = 0; i < 20; i++) {
      List<Node> copy = i < 10 ? _copy(b_copy) : _copy(_chain);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      int k = r.nextInt(_chain.size());

      if (i % 2 == 0 && _is3D) { // TODO : Clean!
        for (Node f : copy) {
          copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
          a = (float) (60 * Math.PI / 180);
          if (j < _chain.size())
            f.rotate(new Quaternion(copy.get(j).translation(), (float) (2 * Math.random() * a - a)));
          copy_p.add(f.position().get());
        }
      } else {
        for (Node f : copy) {
          copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
          copy_p.add(f.position().get());
        }
        //Use target orientation
        Vector o = _chain.get(_chain.size() - 3).position();
        Vector p = _chain.get(_chain.size() - 2).position();
        Vector q = _chain.get(_chain.size() - 1).position();

        Vector po = Vector.subtract(o, p);
        Vector qp = Vector.subtract(q, p);

        Vector axis = _is3D ? Vector.cross(po, qp, null) : new Vector(0, 0, 1);
        Quaternion quat = new Quaternion(axis, (float) (2 * Math.random() * Math.PI - Math.PI));
        qp = quat.rotate(qp);
        copy_p.set(_chain.size() - 2, Vector.add(qp, p));
        if (debug) {
          dir_temp_i.add(p.get());
          dir_temp.add(Vector.add(qp, p));
        }
      }

      copy_p.set(_chain.size() - 1, this._target.position().get());
      //if(_fixTwisting) _applyTwistRotation(copy, copy_p.get(copy_p.size() - 1));
      _forwardReaching(copy, copy_p, _distances, copy_props, _keepDirection);

      Vector o = copy_p.get(0);
      copy_p.set(0, _chain.get(0).position());
      _backwardReaching(copy, copy_p, _distances, copy_props, _keepDirection, o);

      float d = Vector.distance(copy_p.get(copy_p.size() - 1), target);
      if (d <= b_d) {
        b_d = d;
        b_copy = copy;
        _bestAvoidPosition = copy_p;
        if (avoid) _avoidHistory.add(_bestAvoidPosition);
        else _divergeHistory.add(_bestAvoidPosition);
        if (debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if (debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }

    for (int i = 0; i < _chain.size(); i++) {
      if (i < _jointChange.size()) _jointChange.set(i, 0f);
      _chain.get(i).setRotation(b_copy.get(i).rotation().get());
      _positions.set(i, b_copy.get(i).position().get());
    }
    _afterAvoidPosition = (ArrayList<Vector>) _positions.clone();
  }

  /*Original method is not accurate when joints are highly constrained, sometimes
  * we observe that actions on Forward Step makes the root of the structure move away
  * from initial position.
  * TODO: Rename ?
  */

  BallAndSocket[] _globalConstraints; //these constraint are useful only on froward step, hence we will reflect them

  public static class DebugGlobal{
      public Node n;
      public Vector axis;
      public Vector up_axis;
      public Vector right_axis;
      public List<Node>[] chain = new List[4];
  };

  public DebugGlobal[] dg;
  protected void _generateGlobalConstraints(){
    _globalConstraints = new BallAndSocket[_chain.size()];
    dg = new DebugGlobal[_chain.size()];
    Vector root_pos = _chain.get(0).position();
    System.out.println("Global Constraints Info");
    for(int i = 1; i < _chain.size(); i++){
      System.out.println("Joint i: " + i);
      Vector j_i_pos = _chain.get(i).position();
      Vector axis = Vector.subtract(j_i_pos, root_pos);
      Vector up_axis = Vector.orthogonalVector(axis);
      Vector right_axis = Vector.cross(axis, up_axis, null);
      Vector[] dirs = {up_axis, right_axis, Vector.multiply(up_axis, -1), Vector.multiply(right_axis, -1)};
      dg[i] = new DebugGlobal();
      dg[i].axis = axis;
      dg[i].up_axis = up_axis;
      dg[i].right_axis = right_axis;
      //TODO: Check avg vs Max / Min - Also, it is important to Take into account Twisting!
      //TODO: This will work better if we consider angles up to PI (current boundaries are at maximum PI/2)
      float[] angles = {0,0,0,0};
      for(int dir = 0; dir < 4; dir++) {
        List<Node> local_chain = _copy(_chain);
        while(local_chain.size() > i + 1) local_chain.remove(local_chain.size() - 1);

        for (int k = 0; k < i; k++) {
          System.out.println("|--- Joint K: " + k);
          Node j_k = local_chain.get(k);
          //get axis in terms of J_k
          Vector local_j_i = j_k.location(_chain.get(i));
          Vector local_j_i_hat = j_k.location(local_chain.get(i));
          Vector local_j_0 = j_k.location(root_pos);
          Vector local_dir = j_k.displacement(dirs[dir]);
          float desired_angle = _computeAngle(local_dir, local_j_0, local_j_i, local_j_i_hat);
          //rotate by desired
          if(j_k.constraint() instanceof BallAndSocket){
              //TODO : CLEAN THIS! Do not apply any twisting
              BallAndSocket c = ((BallAndSocket) j_k.constraint());
              float min = c.minTwistAngle();
              float max = c.maxTwistAngle();
              c.setTwistLimits(0,0);
              j_k.rotate(new Quaternion(local_dir, desired_angle));
              c.setTwistLimits(min,max);
          }else{
              j_k.rotate(new Quaternion(local_dir, desired_angle));
          }
        }
        //Find rotation between original and local
        System.out.println("original chain: " + _chain.get(0).location(_chain.get(i).position()));
        System.out.println("dir" + dirs[dir]);
        System.out.println("dir : " + dir +  " proj chain chain: " + _chain.get(0).location(local_chain.get(i).position()));

        dg[i].chain[dir] = local_chain;
        Quaternion q = new Quaternion(_chain.get(0).location(Vector.projectVectorOnPlane(_chain.get(i).position(), dirs[dir])), _chain.get(0).location(Vector.projectVectorOnPlane(local_chain.get(i).position(), dirs[dir])));
        float ang = Vector.angleBetween(Vector.subtract(_chain.get(i).position(), root_pos), Vector.subtract(local_chain.get(i).position(), root_pos));
        angles[dir] = ang;//q.angle();
        System.out.println(">>>>result: " + q.axis() + "  " + q.angle());
        //if(q.axis().dot(dirs[dir]) < 0)
        //  angles[dir] = (float)(2 * Math.PI - q.angle());
        System.out.println(">>>>result: " + q.axis() + "  " + Math.toDegrees(angles[dir]));

      }
      System.out.println("axis " + axis);
      System.out.println("up " + Math.toDegrees(angles[0]));
      System.out.println("right " + Math.toDegrees(angles[1]));
      System.out.println("down " + Math.toDegrees(angles[2]));
      System.out.println("left " + Math.toDegrees(angles[3]));
      //TODO : Generalize conic constraints
      //with the obtained information generate the global constraint for j_i
      if(angles[2] < Math.PI/2  && angles[0] < Math.PI/2  && angles[1] < Math.PI/2  && angles[3] < Math.PI/2 ) {
        _globalConstraints[i] = new BallAndSocket(angles[3], angles[1], angles[2], angles[0]);
        _globalConstraints[i].setRestRotation(new Quaternion(), up_axis, Vector.multiply(axis, 1));
      }
    }
  }

  protected float _computeAngle(Vector axis, Vector j0, Vector ji, Vector ji_hat){
    //All parameters are defined locally w.r.t j_k
    Vector ji_proj = Vector.projectVectorOnPlane(ji, axis);
    Vector A = Vector.projectVectorOnPlane(Vector.subtract(ji, j0), axis);
    System.out.println("  A : " + A);
    Vector B = Vector.projectVectorOnPlane(ji_hat, axis);
    System.out.println("  B : " + B);
    //Find the intersection between line defined by A and the the circle with radius B
    float radius = B.magnitude();
    float angle = Vector.angleBetween(A, ji_proj);
    System.out.println("  Angle : " + angle);
    float chord_length = (float)(2 * radius * Math.cos(angle));
    Vector C = Vector.add(ji_proj, Vector.multiply(A.normalize(null), - chord_length));
    System.out.println("  C : " + C);
    float desired_angle;
    if(radius  <= j0.magnitude()){
      //C =  Vector.add(Vector.multiply(Vector.subtract(C, ji_proj),0.5f), B).normalize(null);
      C =  Vector.cross(axis, A, null).normalize(null);
      C.multiply(radius);
      desired_angle = Vector.angleBetween(B,C);
      Vector cross = Vector.cross(B, C, null);
      if(Vector.dot(cross, axis) < 0){
        desired_angle = (float) Math.PI - desired_angle;
      }
    } else {
      desired_angle = Vector.angleBetween(B, C);
      Vector cross = Vector.cross(B, C, null);
      if (Vector.dot(cross, axis) < 0) {
        desired_angle = (float) (2 * Math.PI - desired_angle);
      }
    }
    System.out.println("ANG : " + Math.min(desired_angle, (float)(0.99 * Math.PI)));
    return Math.min(desired_angle, (float)(0.8 * Math.PI)); //Rotation near to PI generates Singularity problems
  }

//  protected float _computeAngle(Vector axis, Vector a, Vector b){
//    Vector v1 = Vector.projectVectorOnPlane(b, axis);
//    Vector v2 = Vector.projectVectorOnPlane(Vector.multiply(a,-1), axis);
//    float angle = Vector.angleBetween(v1,v2);
//    Vector cross = Vector.cross(v1, v2, null);
//    if(Vector.dot(cross, axis) < 0){
//      angle = (float) Math.PI * 2 - angle;
//    }
//    System.out.println("ANG : " + angle);
//    return Math.min(angle, (float) Math.PI * 0.99f); //Rotation near to PI generates Singularity problems
//  }

  protected float _getBound(Vector axis, Vector local_axis, float angle, Node node){
    Quaternion q = new Quaternion(local_axis, angle);
    Quaternion delta = node.constraint().constrainRotation(new Quaternion(local_axis, angle),node);
    System.out.println("----| cons: " + ((BallAndSocket)node.constraint()).orientation().axis() + Math.toDegrees(((BallAndSocket)node.constraint()).orientation().angle()));
      System.out.println("----| axis: " + axis);
    System.out.println("----| local axis: " + local_axis);
    System.out.println("----| desired quat : " + q.axis() + " angle (degrees) " + Math.toDegrees(q.angle()));
    System.out.println("----| constrained quat : " + delta.axis() + " angle (degrees) " + Math.toDegrees(delta.angle()));




      Quaternion rotor = node.orientation();
      System.out.println("----| rot orig computed : " + rotor.axis() + " angle (degrees) " + Math.toDegrees(rotor.angle()));

      //Keep only the component that we're interested on
      //Decompose in terms of twist and swing
      Vector rotationOAxis = new Vector(rotor._quaternion[0], rotor._quaternion[1], rotor._quaternion[2]);
      rotationOAxis = Vector.projectVectorOnAxis(rotationOAxis, axis);
      //Get rotation component on Axis direction
      Quaternion rotationOTwist = new Quaternion(rotationOAxis.x(), rotationOAxis.y(), rotationOAxis.z(), rotor.w());
      System.out.println("----| rot orig twist : " + rotationOTwist.axis() + " angle (degrees) " + Math.toDegrees(rotationOTwist.angle()));





    Quaternion rot = Quaternion.compose(node.orientation(),delta);
    System.out.println("----| rot computed : " + rot.axis() + " angle (degrees) " + Math.toDegrees(rot.angle()));

    //Keep only the component that we're interested on
    //Decompose in terms of twist and swing
    Vector rotationAxis = new Vector(rot._quaternion[0], rot._quaternion[1], rot._quaternion[2]);
    rotationAxis = Vector.projectVectorOnAxis(rotationAxis, node.orientation().inverseRotate(axis));
    System.out.println("rotAx : " +rotationAxis);
    //Get rotation component on Axis direction
    Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), rot.w());
    System.out.println("----| rot twist : " + rotationTwist.axis() + " angle (degrees) " + Math.toDegrees(rotationTwist.angle()));
    return rotationTwist.angle();
  }

  protected Quaternion applyGlobalConstraint(Vector j_hat, Vector j_root_hat,  Vector j_root, BallAndSocket b){
    if(b == null) return new Quaternion(); //no correction is done
    //Find angle in global space
    Quaternion rot = new Quaternion(Vector.subtract(j_root_hat, j_hat), Vector.subtract(j_root, j_hat));
    Vector axis = b.restRotation().rotate(new Vector(0,0,1));
    Vector target = rot.rotate(axis);
    //Apply constraint
    Vector result = b.apply(target,b.restRotation());
    //Apply inverse transformation
    return new Quaternion(result, target);
  }

  public BallAndSocket[] globalConstraints(){
      return _globalConstraints;
  }

  //Animation Stuff
  //TODO: Refactor, perhaps move to Solver class
  @Override
  public void registerStructure(VisualizerMediator mediator){
    mediator.registerStructure(_chain);
    mediator.registerStructure(_target);
  }

  @Override
  public Iterator<? extends Node> iterator(){
    return _chain.iterator();
  }
}