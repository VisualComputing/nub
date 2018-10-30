package frames.ik.evolution;

import frames.core.Frame;
import frames.ik.Solver;
import frames.ik.evolution.operator.Operator;
import frames.ik.evolution.selection.Selection;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class GASolver extends Solver {
    protected Selection _selection;
    protected Operator _mutation;
    protected Operator _crossover;
    protected float _cross_probability = 0.5f;
    protected int _population_size = 10;

    protected Random _random = new Random();
    protected HashMap<Frame, Frame> _previousTarget;
    protected HashMap<Frame, Frame> _target;
    protected List<Frame> _structure;
    protected List<Individual> _population;
    protected Individual _best;


    public GASolver(List<Frame> structure, int population_size){
        this._structure = structure;
        this._population_size = population_size;
    }

    public List<Frame> structure(){
        return _structure;
    }

    public void setTarget(Frame endEffector, Frame target) {
        this._target.put(endEffector, target);
    }

    public double[] execute(){
        //double[] results = new double[maxIter];
        //1. Generate population
        _population = Util.generatePopulation(_structure, _population_size);
        //2. Update Fitness
        for(Individual individual : _population){
            individual.updateFitness(_target);
        }
        //3. TODO use a better Termination Condition
        int k = 0;
        while(k < maxIter){
            _iterate();
            k++;
        }
        //TODO update statistics
        //results[k] = _distanceToTarget(_x_i);
        return null;
    }

    @Override
    protected boolean _iterate() {
        //1. Select parents
        List<Individual> parents = _selection.choose(true, _population, _population_size * 2);
        //2. Generate children
        List<Individual> children = new ArrayList<>();
        for(int i = 0; i < parents.size(); i+=2){
            if(_random.nextFloat() < _cross_probability) {
                Individual child = _crossover.apply(parents.get(i), parents.get(i + 1));
                children.add(_mutation.apply(child));
            } else{
                children.add(parents.get(0));
            }
        }
        //3. Replacement


        //4. Find Best

        double distance = 0 ;
        return distance < minDistance;
    }


    public Frame head() {
        return _structure.get(0);
    }

    public Frame endEffector() {
        return _structure.get(_structure.size() - 1);
    }

    @Override
    protected void _update() {
        for(int i = 0; i < _structure.size(); i++){
            _structure.get(i).setRotation(_best.structure().get(i).rotation().get());
        }
    }

    @Override
    protected boolean _changed() {
        if (_target == null) {
            _previousTarget = null;
            return false;
        } else if (_previousTarget == null) {
            return true;
        }
        for(Frame endEffector : _target.keySet()){
            if(!(_previousTarget.get(endEffector).position().matches(_target.get(endEffector).position()) &&
                    _previousTarget.get(endEffector).orientation().matches(_target.get(endEffector).orientation()))){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void _reset() {
        iterations = 0;
        if(_target == null){
            _previousTarget = null;
            return;
        }
        for(Frame endEffector : _target.keySet()) {
            _previousTarget.put(endEffector, new Frame(_target.get(endEffector).position(), _target.get(endEffector).orientation()));
        }
    }
}

