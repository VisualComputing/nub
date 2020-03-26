package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class Individual {
  public enum FitnessFunction {
    POSITION, ORIENTATION, POSE
  }

  protected List<Node> _structure;
  protected HashMap<String, Float> _floatParams = new HashMap<String, Float>();
  protected HashMap<String, float[]> _arrayParams = new HashMap<String, float[]>();
  protected float _fitness;
  protected float _balanced_fitness;
  protected float _dr, _dt;
  protected FitnessFunction _fitness_function = FitnessFunction.POSITION;

  public Individual(List<Node> structure) {
    this(structure, false);
  }

  public Individual(List<Node> structure, boolean copy){
    if(!copy) _structure = structure;
    else _structure = _copy(structure);
    _fitness = Float.NaN;
    arrayParams().put("Evolution_Gradient", new float[structure.size() * 3]);
  }


  public List<Node> structure() {
    return _structure;
  }

  public void setStructure(List<Node> _structure) {
    this._structure = _structure;
  }

  public float fitness() {
    return _fitness;
  }

  public float balancedFitness() {
    return _balanced_fitness;
  }

  public HashMap<String, Float> floatParams() {
    return _floatParams;
  }

  public HashMap<String, float[]> arrayParams() {
    return _arrayParams;
  }

  public void setFitness(float fitness) {
    _fitness = fitness;
  }

  public void updateFitness(HashMap<Integer, Node> targets) {
    _dt = _dr = 0;
    //TODO : optimize calculations (cache positions and orientations)
    for (Integer index : targets.keySet()) {
      if (_fitness_function == FitnessFunction.POSITION || _fitness_function == FitnessFunction.POSE) {
        float dist = Vector.distance(structure().get(index).position(), targets.get(index).position());
        if (_fitness_function == FitnessFunction.POSE) {
          float l = 0;
          float d = Vector.distance(_structure.get(0).position(), _structure.get(index).position());
          Node prev = null;
          for (Node f : Node.path(_structure.get(0), _structure.get(index))) {
            if (prev != null) l += Vector.distance(f.position(), prev.position());
            prev = f;
          }
          _dt += (Math.PI * dist) / (Math.sqrt(l * d));
        } else {
          _dt += dist;
        }
      }
      if (_fitness_function == FitnessFunction.ORIENTATION || _fitness_function == FitnessFunction.POSE) {
        Quaternion q1 = structure().get(index).orientation();
        Quaternion q2 = targets.get(index).orientation();
        float q_dot = Quaternion.dot(q1, q2);
        _dr += 2 * Math.acos(q_dot);
      }
    }

    if (_fitness_function == FitnessFunction.POSITION) {
      _fitness = _dt / targets.size();
      _balanced_fitness = _dt / targets.size();
    } else if (_fitness_function == FitnessFunction.ORIENTATION) {
      _fitness = _dr / targets.size();
      _balanced_fitness = _dr / targets.size();
    } else {
      float w = (float) Math.random(); //Best solutions must adapt to different sort of weights.
      _fitness = (1 - w) * _dt / targets.size() + w * _dr / targets.size();
      _balanced_fitness = 0.5f * _dt / targets.size() + 0.5f * _dr / targets.size();
    }
  }

  public float getError() {
    float error = 0;
    switch (this._fitness_function) {
      case POSITION: {
        error = _dt;
        break;
      }
      case ORIENTATION: {
        error = _dr;
        break;
      }
      case POSE: {
        error = _dt + _dr;
        break;
      }
    }
    return error;
  }

  protected ArrayList<Node> _copy(List<Node> chain) {
    HashMap<Node, Node> map = new HashMap<>();
    ArrayList<Node> copy = new ArrayList<Node>();
    Node reference = chain.get(0).reference();
    if (reference != null) {
      reference = Node.detach(reference.position().get(), reference.orientation().get(), 1);
    }
    map.put(chain.get(0).reference(), reference);
    for (Node joint : chain) {
      Node newJoint = Node.detach(new Vector(), new Quaternion(), 1);
      newJoint.setReference(map.get(joint.reference()));
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      map.put(joint, newJoint);
    }
    return copy;
  }

  //here we assume that chain and copy has the same structure
  public void setChain(List<Node> chain) {
    for(int i = 0; i < chain.size(); i++){
      _structure.get(i).setTranslation(chain.get(i).translation().get());
      _structure.get(i).setRotation(chain.get(i).rotation().get());
    }
  }

  protected void _cloneAttributes(Individual individual){
    _fitness = individual._fitness;
    if (_floatParams != null) {
      for (String name : _floatParams.keySet()) {
        _floatParams.put(name, individual._floatParams.get(name).floatValue());
      }
    }
    if (_arrayParams != null) {
      for (String name : _arrayParams.keySet()) {
        _arrayParams.put(name, individual._arrayParams.get(name).clone());
      }
    }
  }

  public Individual clone() {
    Individual individual = new Individual(_copy(_structure));
    individual._cloneAttributes(this);
    return individual;
  }

  public void set(Individual individual){
    setChain(individual._structure);
    _cloneAttributes(individual);
  }

  public String toString() {
    String s = "";
    s += "Individual : [";
    for (Node frame : _structure) {
      s += frame.rotation().eulerAngles() + ", ";
    }
    s += "] Fitness " + this._fitness;
    return s;
  }
}
