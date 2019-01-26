package frames.ik.evolution;

import frames.core.Frame;
import frames.ik.Solver;
import frames.ik.evolution.operator.Operator;
import frames.ik.evolution.operator.OperatorMethods;
import frames.ik.evolution.selection.Selection;
import frames.ik.evolution.selection.SelectionMethods;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class GASolver extends Solver {
    public enum Replacement {
        GENERATIONAL, ELITISM, KEEP_BEST
    }

    protected Selection _selection;
    protected Operator _mutation;
    protected Operator _crossover;
    protected float _cross_probability = 1f;
    protected int _population_size = 10;
    protected Replacement replacement = Replacement.KEEP_BEST;

    protected Random _random = new Random();
    protected HashMap<Integer, Frame> _previousTarget;
    protected HashMap<Integer, Frame> _target;
    protected List<Frame> _structure;
    protected List<Individual> _population;
    protected Individual _best;


    protected List<Statistics> _statistics;

    protected boolean _debug = false;

    public GASolver(List<Frame> structure, int population_size){
        this._structure = structure;
        this._population_size = population_size;
        this._target = new HashMap<Integer, Frame>();
        this._previousTarget = new HashMap<Integer, Frame>();
        _selection = new SelectionMethods.Ranking();
        _mutation = new OperatorMethods.UniformMutation();
        _crossover = new OperatorMethods.ConvexCombination();
    }

    public int populationSize(){
        return _population_size;
    }

    public float crossProbability(){
        return _cross_probability;
    }

    public List<Statistics> statistics(){
        return _statistics;
    }

    public void setPopulationSize(int size){
        _population_size = size;
    }

    public void setCrossProbability(float probability){
        _cross_probability = probability;
    }

    public void setSelection(Selection selection){
        _selection = selection;
    }

    public void setMutation(Operator mutation){
        _mutation = mutation;
    }

    public void setCrossover(Operator crossover){
        _crossover = crossover;
    }

    public List<Frame> structure(){
        return _structure;
    }

    public float best(){
        return _best != null ? _best.fitness() : -1;
    }

    public void setTarget(Frame endEffector, Frame target) {
        this._target.put(structure().indexOf(endEffector), target);
    }

    public float execute(){
        this._best = null;
        this._statistics = new ArrayList<Statistics>();
        //1. Generate population
        _population = Util.generatePopulation(_structure, _population_size);
        //2. Update Fitness
        for(Individual individual : _population){
            individual.updateFitness(_target);
            _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
        }
        //3. Iterate a given number of times.
        int k = 0;
        while(k < maxIter){
            _iterate();
            _statistics.add(new Statistics(_population));
            k++;
        }
        //4. Keep statistics
        return _best.fitness();
    }

    @Override
    protected boolean _iterate() {
        //1. Select parents
        List<Individual> parents = _selection.choose(true, _population, _population_size * 2);
        if(_debug) {
            System.out.println("ITERATION : " + iterations);
            System.out.println("Parents");
            for(Individual ind : parents){
                System.out.println(ind);
            }
        }

        //2. Generate children
        List<Individual> children = new ArrayList<>();
        Individual worst = null;
        Individual best = _best;
        for(int i = 0; i < parents.size(); i+=2){
            if(_random.nextFloat() < _cross_probability) {
                Individual child = _crossover.apply(parents.get(i), parents.get(i + 1));
                if(_debug) {
                    System.out.println("\t Best " + _best);
                    System.out.println("\t P1 " + parents.get(i));
                    System.out.println("\t P2 " + parents.get(i + 1));
                    child.updateFitness(_target);
                    System.out.println("\t Child " + child);
                }
                child = _mutation.apply(child);
                child.updateFitness(_target);
                children.add(child);
                best = best == null ? child : best.fitness() > child.fitness() ? child : best;
                worst = worst == null ? child : worst.fitness() < child.fitness() ? child : worst;

            } else{
                Individual child = parents.get(i);
                children.add(child);
                best = best == null ? child : best.fitness() > child.fitness() ? child : best;
                worst = worst == null ? child : worst.fitness() < child.fitness() ? child : worst;
            }
        }
        if(_debug) {
            System.out.println("Children ");
            for (Individual ind : children) {
                System.out.println(ind);
            }
        }

        //3. Replacement
        switch (replacement){
            case ELITISM:{
                //Keep best individuals
                _population = Util.concatenate(parents, children);
                _population = Util.sort(true, false, _population);
                _best = _population.get(0);
                _population = _population.subList(0, _population_size);
                Collections.shuffle(_population);
                if(_debug) {
                    System.out.println("Population ");
                    for (Individual ind : _population) {
                        System.out.println(ind);
                    }
                }
                break;
            }
            case GENERATIONAL:{
                _population = children;
                _best = best;
                break;
            }
            //Steady State
            case KEEP_BEST:{
                _population = children;
                if(!_population.contains(best)) {
                    _population.remove(worst);
                    _population.add(best);
                }
                _best = best;
                break;
            }
        }
        return _best.fitness() < minDistance;
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
        for(Integer endEffector : _target.keySet()){
            if(_previousTarget.get(endEffector) == null) return true;
            if(!(_previousTarget.get(endEffector).position().matches(_target.get(endEffector).position()) &&
                    _previousTarget.get(endEffector).orientation().matches(_target.get(endEffector).orientation()))){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void _reset() {
        _best = null;
        iterations = 0;
        if(_target == null){
            _previousTarget = null;
            return;
        }
        for(Integer endEffector : _target.keySet()) {
            _previousTarget.put(endEffector, new Frame(_target.get(endEffector).position(), _target.get(endEffector).orientation(), 1));
        }
        //If there is no population then genereate one
        _population = Util.generatePopulation(_structure, _population_size);
        for (Individual individual : _population) {
            individual.updateFitness(_target);
            _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
        }
    }
}
