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

package nub.ik;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.*;

public class ChainSolver extends FABRIKSolver {

  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)
  protected ArrayList<Node> _chain;
  protected ArrayList<? extends Node> _original;
  protected float _current = 10e10f, _best = 10e10f;

  //Animation Stuff
  protected ArrayList<ArrayList<Vector>> _iterationsHistory;

  protected Node _target;
  protected Node _prevTarget;


  //TODO : Check this ideas, clean and refine (*)
  //----------------------------------------------------
  protected Vector _targetDirection;
  protected ArrayList<Vector> _bestAvoidPosition, _afterAvoidPosition;
  protected ArrayList<ArrayList<Vector>> _avoidHistory, _divergeHistory;
  protected boolean _explore = true;
  protected float _exploration = 0;
  protected float _lastError;
  //----------------------------------------------------

  public void explore(boolean explore){
    _explore = explore;
  }

  public float exploration(){
      return  _exploration;
  }

  public ArrayList<Vector> bestAvoidPosition(){
      return  _bestAvoidPosition;
  }

  public ArrayList<Vector> afterAvoidPosition(){
    return  _afterAvoidPosition;
  }

  public ArrayList<Vector> positions(){
    return  _positions;
  }

  public ArrayList<ArrayList<Vector>> avoidHistory(){
    return  _avoidHistory;
  }

  public ArrayList<ArrayList<Vector>> divergeHistory(){
    return  _divergeHistory;
  }


  public ArrayList<? extends Node> chain() {
    return _original;
  }

  public Node target() {
    return _target;
  }

  public void setTarget(Node endEffector, Node target) {
    this._target = target;
  }

  public void setTargetDirection(Vector direction){ //TODO : clean
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

  public ChainSolver(ArrayList<? extends Node> chain) {
    this(chain, null);
  }

  public ChainSolver(ArrayList<? extends Node> chain, ArrayList<Node> copy, Node target) {
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
      _distances.add(Vector.subtract(position, prevPosition).magnitude());
      _jointChange.add(0f);
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
    _jointChange.remove(0);
    this._target = target;
    this._prevTarget =
            target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
  }

  public ChainSolver(ArrayList<? extends Node> chain, Node target) {
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
    _lastError = iterations % 2 == 0 ?  Vector.distance(end.position(), _target.position()) : Math.max(Vector.distance(end.position(), _target.position()), _lastError);
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= error) {
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
    if(_fixTwisting && iterations % 3 == 0){
      _applyTwistRotation(_chain, target);
      //TODO : Do it efficiently
      for(int i = 0; i < _chain.size(); i++){
        _positions.set(i, _chain.get(i).position());
      }
    }

    //TODO : Clean and consider twisting
    if(_targetDirection != null){
      _applyTargetdirection();
    }
    _positions.set(_chain.size() - 1, target.get());
    _forwardReaching();
    //Animation stuff
    addIterationRecord(_positions);
    //Stage 2: Backward Reaching
    Vector o = _positions.get(0);
    _positions.set(0, initial);
    float change = _backwardReaching(o);
    //Save best solution
    float currentError  = Vector.distance(end.position(), _target.position());
    if(_explore && currentError > error) {
      if(debug) System.out.println("ne is greater than error : " + " ne : " + currentError + " error : " + error);
      if(debug) System.out.println("change is : " + change);
      if(debug) System.out.println("e is : " + _lastError);

      if ((change < 10e-6) || (iterations % 2 == 1 && currentError > _lastError)) {
        boolean avoid = currentError > _lastError;
        for(Float jc : _jointChange) {
          if (debug) System.out.print(jc + " , ");
        }
        if(debug) System.out.println();
        //avoid deadLock
        if(debug) System.out.println(avoid ? "AVOID" : "EXPLORE");
        _avoidDeadlock(avoid);
      }
    }
    _current = Vector.distance(target, _chain.get(_chain.size() - 1).position());
    _update();
    if(debug) addIterationRecord(_positions);
    //Check total position change
    return false;
  }

  protected void _forwardReaching() {
    _forwardReaching(_chain);
  }

  protected float _backwardReaching(Vector o) {
    return _backwardReaching(_chain, o);
  }

  @Override
  protected void _update() {
    if(_current < _best){
      for(int i = 0; i < _original.size(); i++){
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
    iterations = 0;
    _exploration = 0;
    //We know that State has change but not where, then it is better to reset Global Positions and Orientations
    _init();
    if(_target != null){
      _best = Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
    } else{
      _best = 10e10f;
    }
  }

  @Override
  public float error() {
    return Vector.distance(_original.get(_original.size() - 1).position(), _target.position());
  }

  protected void _init() {
    //Initialize List with info about Positions and Orientations
    if(_original.get(0).reference() != null) {
      _chain.get(0).reference().setMagnitude(_original.get(0).reference().magnitude());
      _chain.get(0).reference().setOrientation(_original.get(0).reference().orientation().get());
      _chain.get(0).reference().setPosition(_original.get(0).reference().position().get());
   }

    for(int i = 0; i < _chain.size(); i++){
      _chain.get(i).setScaling(_original.get(i).scaling());
      _chain.get(i).setRotation(_original.get(i).rotation().get());
      _chain.get(i).setTranslation(_original.get(i).translation().get());
    }
    _positions = new ArrayList<Vector>();
    _distances = new ArrayList<Float>();
    //TODO : rename and clean jointChange
    _jointChange = new ArrayList<Float>();
    _orientations = new ArrayList<Quaternion>();
    _iterationsHistory = new ArrayList<>();

    Vector prevPosition = _chain.get(0).reference() != null
        ? _chain.get(0).reference().position().get() : new Vector(0, 0, 0);
    Quaternion prevOrientation = _chain.get(0).reference() != null
        ? _chain.get(0).reference().orientation().get() : new Quaternion();
    for (Node joint : _chain) {
      if(!_properties.containsKey(joint.id())) {
        Properties props = new Properties(false); //TODO : CLEAN!!!
        _properties.put(joint.id(), props);
      }
      Vector position = joint.position().get();
      Quaternion orientation = prevOrientation.get();
      orientation.compose(joint.rotation().get());
      _positions.add(position);
      _distances.add(Vector.subtract(position, prevPosition).magnitude());
      _jointChange.add(0f);
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
    _jointChange.remove(0);
    addIterationRecord(_positions);
    _exploration = 0;
    //TODO : REFINE & USE ONLY IN DEBUG MODE
    _avoidHistory = new ArrayList<>();
    _divergeHistory = new ArrayList<>();
  }

  protected void _applyTargetdirection(){
    //Use target orientation
    Vector o = _chain.get(_chain.size() - 2).position();
    Vector p = _chain.get(_chain.size() - 1).position();
    Vector op = Vector.subtract(o,p);
    Vector direction = _target.worldDisplacement(_targetDirection);
    direction.normalize();
    direction.multiply(-1);
    direction.multiply(op.magnitude());
    _positions.set(_chain.size() - 2, Vector.add(direction, p));
  }


  protected void _avoidDeadlock(boolean avoid){
    if(_chain.size() <= 1) return;
    _exploration++;
    float a;
    ArrayList<Node> b_copy = null;
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
    if(debug) System.out.println("sorted change : ");
    for(int idx : indices) {
      if (debug) System.out.print(_jointChange.get(idx) + " , ");
    }
    if(debug) System.out.println();

    for(int i = 0; i < 20; i++) {
      ArrayList<Node> copy = i < 10 ? _copy(_chain) : _copy(b_copy);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      for (Node f : copy) {
        copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
        if(j < _jointChange.size() && i < 10) {
          //focus on change joints who preserves direction
          a = 1 - i/(_jointChange.size() - 1);
          a = (float)(a * 180 * Math.PI / 180);
        } else{
          a = (float)(180 * Math.PI / 180);
        }
        if(j < _jointChange.size()) {
          f.rotate(new Quaternion((float) (2 * Math.random() * a - a),
                  (float) (2 * Math.random() * a - a),
                  (float) (2 * Math.random() * a - a)));
        }
        copy_p.add(f.position().get());
      }

      /*copy_p.set(_chain.size() - 1, this._target.position().get());
      //if(_fixTwisting) _applyTwistRotation(copy, copy_p.get(copy_p.size() - 1));
      _forwardReaching(copy, copy_p, _distances, copy_props, _keepDirection);

      Vector o = copy_p.get(0);
      copy_p.set(0, _chain.get(0).position());
      _backwardReaching(copy, copy_p, _distances, copy_props , _keepDirection, o);*/

      float d = Vector.distance(copy_p.get(copy_p.size() -1), target);
      if(d <= b_d){
        b_d = d;
        b_copy = copy;
        _bestAvoidPosition = copy_p;
        if(avoid)_avoidHistory.add(_bestAvoidPosition);
        else _divergeHistory.add(_bestAvoidPosition);
        if(debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if(debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }


    for(int i = 0; i < 20; i++) {
      ArrayList<Node> copy = i < 10 ? _copy(b_copy) : _copy(_original);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      int k = r.nextInt(_chain.size());

      for (Node f : copy) {
        copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
        a = (float)(60 * Math.PI / 180);
        if(j < _chain.size())f.rotate(new Quaternion(copy.get(j).translation(), (float) (2 * Math.random() * a - a)));
        copy_p.add(f.position().get());
      }

      copy_p.set(_chain.size() - 1, this._target.position().get());
      //if(_fixTwisting) _applyTwistRotation(copy, copy_p.get(copy_p.size() - 1));
      _forwardReaching(copy, copy_p, _distances, copy_props, _keepDirection);

      Vector o = copy_p.get(0);
      copy_p.set(0, _chain.get(0).position());
      _backwardReaching(copy, copy_p, _distances, copy_props , _keepDirection, o);

      float d = Vector.distance(copy_p.get(copy_p.size() -1), target);
      if(d <= b_d){
        b_d = d;
        b_copy = copy;
         _bestAvoidPosition = copy_p;
        if(avoid)_avoidHistory.add(_bestAvoidPosition);
        else _divergeHistory.add(_bestAvoidPosition);
        if(debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if(debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }

    for(int i = 0; i < _chain.size(); i++){
      if(i < _jointChange.size()) _jointChange.set(i,0f);
      _chain.get(i).setRotation(b_copy.get(i).rotation().get());
      _positions.set(i,b_copy.get(i).position().get());
    }
    _afterAvoidPosition = (ArrayList<Vector>) _positions.clone();
  }

  //Animation Stuff
  public ArrayList<ArrayList<Vector>> iterationsHistory(){
    return _iterationsHistory;
  }

  public void addIterationRecord(ArrayList<Vector> iteration){
    _iterationsHistory.add(new ArrayList<>(iteration));
  }
}