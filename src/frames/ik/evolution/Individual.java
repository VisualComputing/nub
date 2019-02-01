package frames.ik.evolution;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class Individual{
    public enum FitnessFunction {
        POSITION, ORIENTATION, POSE
    }

    protected List<Frame> _structure;
    protected HashMap<String, Float> _floatParams;
    protected HashMap<String, float[]> _arrayParams;

    protected float _fitness;
    protected float _balanced_fitness;
    protected float _dr, _dt;

    protected FitnessFunction _fitness_function = FitnessFunction.POSITION;

    public Individual(List<Frame> structure){
        _structure = structure;
    }

    public List<Frame> structure() {
        return _structure;
    }

    public void setStructure(List<Frame> _structure) {
        this._structure = _structure;
    }

    public float fitness() {
        return _fitness;
    }

    public float balancedFitness() {
        return _balanced_fitness;
    }

    public HashMap<String, Float> floatParams(){
        return _floatParams;
    }

    public HashMap<String, float[]> arrayParams(){
        return _arrayParams;
    }

    public void setFitness(float fitness){
        _fitness = fitness;
    }

    public void updateFitness(HashMap<Integer, Frame> targets){
        //TODO : optimize calculations (cache positions and orientations)
        for(Integer index : targets.keySet()){
            if(_fitness_function == FitnessFunction.POSITION || _fitness_function == FitnessFunction.POSE) {
                float dist = Vector.distance(structure().get(index).position(), targets.get(index).position());
                if(_fitness_function == FitnessFunction.POSE) {
                    float l = 0;
                    float d = Vector.distance(_structure.get(0).position(), _structure.get(index).position());
                    Frame prev = null;
                    for (Frame f : _structure.get(0).graph().path(_structure.get(0), _structure.get(index))) {
                        if (prev != null) l += Vector.distance(f.position(), prev.position());
                        prev = f;
                    }
                    _dt += (Math.PI * dist) / (Math.sqrt(l * d));
                } else{
                    _dt += dist;
                }
            }
            if(_fitness_function == FitnessFunction.ORIENTATION || _fitness_function == FitnessFunction.POSE){
                Quaternion q1 = structure().get(index).orientation();
                Quaternion q2 = targets.get(index).orientation();
                float q_dot = Quaternion.dot(q1, q2);
                _dr += Math.acos((2 * q_dot * q_dot) / Math.sqrt(q1.dotProduct(q1) * q2.dotProduct(q2)));
            }
        }

        if(_fitness_function == FitnessFunction.POSITION){
            _fitness = _dt;
            _balanced_fitness = _dt;
        }

        if(_fitness_function == FitnessFunction.ORIENTATION){
            _fitness = _dr;
            _balanced_fitness = _dr;
        }

        float w = (float) Math.random() * 0.3f; //Best solutions must adapt to different sort of weights.
        _fitness = (1-w) * _dt + w * _dr;
        _balanced_fitness = 0.5f * _dt + 0.5f * _dr;
    }

    public float getError(){
        //TODO : Must be changed for Multiple end effectors
        float length = Vector.distance(structure().get(0).position(), structure().get(structure().size()).position());
        float error = 0;
        switch (this._fitness_function){
            case POSITION:{
                error = _dt / length;
                break;
            }
            case ORIENTATION:{
                error = _dr;
                break;
            }
            case POSE:{
                error = _dt / length + _dr;
                break;
            }
        }
        return error;
    }

    protected ArrayList<Frame> _copy(List<Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get(), 1);
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


    public Individual clone(){
        Individual individual = new Individual(_copy(_structure));
        individual._fitness = _fitness;
        if(_floatParams != null) {
            for (String name : _floatParams.keySet()) {
                individual._floatParams.put(name, _floatParams.get(name).floatValue());
            }
        }
        if(_arrayParams != null) {
            for (String name : _arrayParams.keySet()) {
                individual._arrayParams.put(name, _arrayParams.get(name).clone());
            }
        }
        return individual;
    }

    public String toString(){
        String s = "";
        s += "Individual : [";
        for(Frame frame : _structure){
            s += frame.rotation().eulerAngles() + ", ";
        }
        s+="] Fitness " + this._fitness;
        return s;
    }
}
