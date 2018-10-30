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

    public void setFitness(float fitness){
        _fitness = fitness;
    }

    public void putParameter(String name, float[] values){
        if(_parameters == null) _parameters = new HashMap<String, Parameter>();
        _parameters.put(name, new Parameter(values));
    }

    public float updateFitness(HashMap<Frame, Frame> targets){
        float dist = 0;
        for(Frame frame : targets.keySet()){
            dist += Vector.distance(frame.position(), targets.get(frame).position());
        }
        _fitness = dist;
        return dist;
    }

    public Individual clone(){
        List<Frame> structure = new ArrayList<>();
        for(int i = 0; i < _structure.size(); i++){
            structure.add(_structure.get(i).get());
        }
        Individual individual = new Individual(structure);
        individual._fitness = _fitness;
        return individual;
    }
}
