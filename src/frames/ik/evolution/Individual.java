package frames.ik.evolution;

import frames.core.Frame;
import frames.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class Individual{

    public static class Parameter{
        protected float[] _values;

        public Parameter(int n){
            _values = new float[n];
        }

        public Parameter(float[] values){
            _values = values;
        }
    }

    protected List<Frame> _structure;
    protected HashMap<String, Parameter> _parameters;
    protected float _fitness;
    protected float _extinction;

    public Individual(List<Frame> structure){
        _structure = structure;
    }

    public List<Frame> structure() {
        return _structure;
    }

    public HashMap<String, Parameter> parameters(){
        return _parameters;
    }

    public void setStructure(List<Frame> _structure) {
        this._structure = _structure;
    }

    public float fitness() {
        return _fitness;
    }

    public float extinction(){
        return _extinction;
    }

    public void setExtinction(float extinction){
        _extinction = extinction;
    }

    public void setFitness(float fitness){
        _fitness = fitness;
    }

    public void putParameter(String name, float[] values){
        if(_parameters == null) _parameters = new HashMap<String, Parameter>();
        _parameters.put(name, new Parameter(values));
    }

    public float updateFitness(HashMap<Integer, Frame> targets){
        float dist = 0;
        for(Integer index : targets.keySet()){
            dist += Vector.distance(structure().get(index).position(), targets.get(index).position());
        }
        _fitness = dist;
        return dist;
    }

    protected ArrayList<Frame> _copy(List<Frame> chain) {
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


    public Individual clone(){
        List<Frame> structure = new ArrayList<>();
        for(int i = 0; i < _structure.size(); i++){
            //structure.add(_structure.get(i).get());
        }
        Individual individual = new Individual(_copy(_structure));
        individual._fitness = _fitness;
        individual._extinction = _extinction;
        if(_parameters != null) {
            for (String name : _parameters.keySet()) {
                individual.putParameter(name, _parameters.get(name)._values.clone());
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
