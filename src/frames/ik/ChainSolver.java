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
import frames.core.constraint.Constraint;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.*;

public class ChainSolver extends FABRIKSolver {

  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)

  protected ArrayList<Frame> _chain;
  protected ArrayList<? extends Frame> _original;
  protected float _current = 10e10f, _best = 10e10f;

  //Animation Stuff
  protected ArrayList<ArrayList<Vector>> _iterationsHistory;

  protected Frame _target;
  protected Frame _prevTarget;

  public ArrayList<? extends Frame> chain() {
    return _original;
  }

  public Frame target() {
    return _target;
  }

  public void setTarget(Frame endEffector, Frame target) {
    this._target = target;
  }

  public void setTarget(Frame target) {
    this._target = target;
  }

  public Frame head() {
    return _original.get(0);
  }

  public Frame endEffector() {
    return _original.get(_original.size() - 1);
  }

  public ChainSolver(ArrayList<? extends Frame> chain) {
    this(chain, null);
  }

  public ChainSolver(ArrayList<? extends Frame> chain, ArrayList<Frame> copy, Frame target) {
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
    for (Frame joint : _chain) {
      _properties.put(joint.id(), new Properties());
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
            target == null ? null : new Frame(target.position().get(), target.orientation().get(), 1);
  }

  public ChainSolver(ArrayList<? extends Frame> chain, Frame target) {
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

  protected void _stretch(ArrayList<? extends Frame> chain, Vector target) {
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

  float e; //TODO : Rename or discard this param

  @Override
  protected boolean _iterate() {
    //As no target is specified there is no need to perform FABRIK
    if (_target == null) return true;
    _current = 0;
    Frame root = _chain.get(0);
    Frame end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();

    //TODO: Check blocked and oscillation decisions based on "e"
    e  = iterations % 2 == 0 ?  Vector.distance(end.position(), _target.position()) : Math.max(Vector.distance(end.position(), _target.position()), e);
    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= error) {
      return true;
    }

    //Get the distance between the Root and the End Effector
    float length = _length();
    //Get the distance between the Root and the Target
    float dist = Vector.distance(root.position(), target);
    //When Target is unreachable        //Debug methods
                    /*if(dist > length){
                    stretchChain(chain, target);
                    return true;
                }else{*/
    //Initial root position
    Vector initial = _chain.get(0).position().get();
    //Stage 1: Forward Reaching
    _positions.set(_chain.size() - 1, target.get());
    _forwardReaching();
    //Animation stuff
    addIterationRecord(_positions);
    //Stage 2: Backward Reaching
    Vector o = _positions.get(0);
    _positions.set(0, initial);
    float change = _backwardReaching(o);
    //Save best solution
    float ne  = Vector.distance(end.position(), _target.position());
    //TODO : clean code
//    System.out.println("ne is greater than error : " + " ne : " + ne + " error : " + error);
//    System.out.println("change is : " + change);
//    System.out.println("e is : " + e);
//    System.out.println("current is : " + _current);
//    System.out.println("best is : " + _best);

    if(ne > error) {
      if(debug) System.out.println("ne is greater than error : " + " ne : " + ne + " error : " + error);
      if(debug) System.out.println("change is : " + change);
      if(debug) System.out.println("e is : " + e);

      if ((change < 10e-6) || (iterations % 2 == 1 && ne > e)) {
        boolean x = ne > e;
        for(Float jc : _jointChange) {
          if (debug) System.out.print(jc + " , ");
        }
        if(debug) System.out.println();
        //avoid deadLock
        if(debug) System.out.println(x ? "AVOID" : "EXPLORE");
        _avoidDeadlock(x);
      }
    }
    _current = Vector.distance(target, _chain.get(_chain.size() - 1).position());
    _update();
    if(debug) addIterationRecord(_positions);
    //Check total position change
    return false;
  }

  Vector initial;
  Frame end;
  Vector target;

  public ArrayList<Vector> forward(){
    _init();
    //As no target is specified there is no need to perform FABRIK
    if (_target == null) return _positions;
    Frame root = _chain.get(0);
    end = _chain.get(_chain.size() - 1);
    target = this._target.position().get();

    //Execute Until the distance between the end effector and the target is below a threshold
    if (Vector.distance(end.position(), target) <= error) {
      return _positions;
    }

    //Get the distance between the Root and the End Effector
    float length = _length();
    //Get the distance between the Root and the Target
    float dist = Vector.distance(root.position(), target);
    //When Target is unreachable        //Debug methods
                    /*if(dist > length){
                    stretchChain(chain, target);
                    return true;
                }else{*/
    //Initial root position
    initial = _chain.get(0).position();
    //Stage 1: Forward Reaching
    _positions.set(_chain.size() - 1, target.get());
    _forwardReaching();
    return _positions;
  }

  public ArrayList<Vector> backward() {
    //Stage 2: Backward Reaching
    Vector o = _positions.get(0);
    _positions.set(0, initial);
    float change = _backwardReaching(o);
    //Save best solution
    _current = Vector.distance(target, _chain.get(_chain.size() - 1).position()); //TODO : Check on Tree Solver
    //Check total position change
    if (change <= 0.001f) return _positions;
    return _positions;
  }

  public ArrayList<Vector> get_p(){
    return _positions;
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
    _prevTarget = _target == null ? null : new Frame(_target.position().get(), _target.orientation().get(), 1);
    iterations = 0;
    exploration = 0;
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
    for (Frame joint : _chain) {
      _properties.put(joint.id(), new Properties());
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
    exploration = 0;
    avoid_pos_hist = new ArrayList<>();
    diverge_hist= new ArrayList<>();
  }


  //TODO : Check this ideas and refine (*)
  public ArrayList<Vector> avoid_pos, p_avoid_pos;
  public ArrayList<ArrayList<Vector>> avoid_pos_hist, diverge_hist;
  public float exploration = 0;
  protected void _avoidDeadlock(boolean x){
    if(_chain.size() <= 1) return;
    exploration++;
    float a = (float)(60 * Math.PI / 180);
    ArrayList<Frame> b_copy = null;
    float b_d = Float.POSITIVE_INFINITY;
    Vector target = _target.position();

    //Sort Joints by change
    Integer[] indices = new Integer[_jointChange.size()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }

    Arrays.sort(indices, new Comparator<Integer>() {
      public int compare(Integer i1, Integer i2) {
        return x ? -_jointChange.get(i1).compareTo(_jointChange.get(i2)) : _jointChange.get(i1).compareTo(_jointChange.get(i2));
      }
    });

    //TODO : think about exploration strategies
    if(debug) System.out.println("sorted change : ");
    for(int idx : indices) {
      if (debug) System.out.print(_jointChange.get(idx) + " , ");
    }
    if(debug) System.out.println();

    for(int i = 0; i < 20; i++) {
      ArrayList<Frame> copy = i < 10 ? _copy(_chain) : _copy(b_copy);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      for (Frame f : copy) {
        copy_props.put(f.id(), _properties.get(_chain.get(j++).id()));
        if(j < _jointChange.size() && i < 10) {
          //focus on change joints who preserves direction
          a = 1 - i/(_jointChange.size() - 1);
          a = (float)(a * 180 * Math.PI / 180);
        } else{
          a = (float)(180 * Math.PI / 180);
        }
        f.rotate(new Quaternion((float) (2 * Math.random() * a - a),
                (float) (2 * Math.random() * a - a),
                (float) (2 * Math.random() * a - a)));
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
        avoid_pos = copy_p;
        if(x)avoid_pos_hist.add(avoid_pos);
        else diverge_hist.add(avoid_pos);
        if(debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if(debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }


    for(int i = 0; i < 20; i++) {
      ArrayList<Frame> copy = i < 10 ? _copy(b_copy) : _copy(_original);
      ArrayList<Vector> copy_p = new ArrayList<>();
      HashMap<Integer, Properties> copy_props = new HashMap<Integer, Properties>();
      int j = 0;
      int k = r.nextInt(_chain.size());

      for (Frame f : copy) {
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
        avoid_pos = copy_p;
        if(x)avoid_pos_hist.add(avoid_pos);
        else diverge_hist.add(avoid_pos);
        if(debug) System.out.println("    BEST : " + b_d + " Dist : " + d);
        if(debug) System.out.println("    BEST Joint Change : " + b_d + " Dist : " + d);
      }
    }

    for(int i = 0; i < _chain.size(); i++){
      if(i < _jointChange.size()) _jointChange.set(i,0f);
      _chain.get(i).setRotation(b_copy.get(i).rotation().get());
      _positions.set(i,b_copy.get(i).position().get());
      p_avoid_pos = (ArrayList<Vector>) _positions.clone();
    }
  }

  //Animation Stuff
  public ArrayList<ArrayList<Vector>> iterationsHistory(){
    return _iterationsHistory;
  }

  public void addIterationRecord(ArrayList<Vector> iteration){
    _iterationsHistory.add(new ArrayList<>(iteration));
  }
}