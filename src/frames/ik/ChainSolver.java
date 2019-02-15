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
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;

public class ChainSolver extends FABRIKSolver {

  //TODO : Update
  //TODO: It will be useful that any Joint in the chain could have a Target ?
  //TODO: Enable Translation of Head (Skip Backward Step)

  protected ArrayList<Frame> _chain;
  protected ArrayList<? extends Frame> _original;
  protected float _current = 10e10f, _best = 10e10f;

  protected Frame _target;
  protected Frame _prevTarget;

  public ArrayList<? extends Frame> chain() {
    return _original;
  }

  public Frame target() {
    return _target;
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
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }
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
  @Override
  protected boolean _iterate() {
    //As no target is specified there is no need to perform FABRIK
    if (_target == null) return true;
    _current = 0;
    Frame root = _chain.get(0);
    Frame end = _chain.get(_chain.size() - 1);
    Vector target = this._target.position().get();

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
    //Stage 2: Backward Reaching
    Vector o = _positions.get(0);
    _positions.set(0, initial);
    float change = _backwardReaching(o);
    //Save best solution
    _current = Vector.distance(target, _chain.get(_chain.size() - 1).position());
    //Check total position change
    /*if (change <= minDistance){
      //avoid deadLock
      System.out.println("AVOID");
      _avoidDeadlock();
    }*/
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
    _current = Vector.distance(target, _original.get(_chain.size() - 1).position());
    //Check total position change
    if (change <= minDistance) return _positions;
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
    //We know that State has change but not where, then it is better to reset Global Positions and Orientations
    _init();
    if(target != null){
      _best = Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
    } else{
      _best = 10e10f;
    }
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
    _orientations = new ArrayList<Quaternion>();
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
      _orientations.add(orientation);
      prevPosition = position;
      prevOrientation = orientation.get();
    }

  }

  protected void _avoidDeadlock(){
    if(_chain.size() <= 1) return;
    Vector v = _chain.get(1).position();
    Vector w = Vector.subtract(_target.position(), head().position());
    Vector axis = w.cross(v);
    head().rotate(new Quaternion(axis, 0.1f));
  }
}