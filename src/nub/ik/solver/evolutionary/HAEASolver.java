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

package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.Individual;
import nub.ik.solver.evolutionary.Statistics;
import nub.ik.solver.evolutionary.Util;
import nub.ik.solver.evolutionary.operator.Operator;
import nub.ik.solver.evolutionary.operator.OperatorMethods;
import nub.ik.solver.evolutionary.selection.Selection;
import nub.ik.solver.evolutionary.selection.SelectionMethods;

import java.util.*;

/**
 * Created by sebchaparr on 2/11/18.
 */
public class HAEASolver  extends Solver {

    public static class HAEAIndividual extends Individual{
        protected float[] _operatorRates;

        public HAEAIndividual(Random random, List<Node> structure, List<Operator> operators) {
            super(structure);
            generateRates(random, operators);
        }

        public HAEAIndividual(Individual individual){
            super(individual.structure());
            this._fitness = individual.fitness();
            //this._parameters = individual.parameters();
        }

        public void generateRates(Random random, List<Operator> operators){
            _operatorRates = new float[operators.size()];
            for(int i = 0; i < operators.size(); i++){
                _operatorRates[i] = random.nextFloat();
            }
            normalizeRates();
        }


        public void normalizeRates(){
            float sum = 0;
            for(int i = 0; i < _operatorRates.length; i++) sum += _operatorRates[i];
            for(int i = 0; i < _operatorRates.length; i++) _operatorRates[i] /= sum;
        }

        protected int chooseOperator(Random random){
            float value = random.nextFloat();
            for(int i = 0; i < _operatorRates.length; i++){
                value -= _operatorRates[i];
                if(value < 0) return i;
            }
            return _operatorRates.length - 1;
        }
    }


    protected Selection _selection;
    protected List<Operator> _operators = new ArrayList<>();

    protected int _population_size = 10;
    protected Random _random = new Random();
    protected HashMap<Integer, Node> _previousTarget;
    protected HashMap<Integer, Node> _target;
    protected List<Node> _structure;
    protected List<Individual> _population;
    protected Individual _best;


    protected List<Statistics> _statistics;
    protected float[][] _operatorsValues;

    protected boolean _debug = false;

    public HAEASolver(List<Node> structure, int population_size, boolean convex){
        this._structure = structure;
        this._population_size = population_size;
        this._target = new HashMap<Integer, Node>();
        this._previousTarget = new HashMap<Integer, Node>();
        this._selection = new SelectionMethods.Tournament();
        //Default operators
        this._operators.add(new OperatorMethods.UniformMutation());
        if(convex) this._operators.add(new OperatorMethods.ConvexCombination());
        this._operators.add(new OperatorMethods.GaussianMutation());
    }

    public int populationSize(){
        return _population_size;
    }

    public List<Statistics> statistics(){
        return _statistics;
    }

    public void setPopulationSize(int size){
        _population_size = size;
    }

    public void addOperator(Operator operator){
        _operators.add(operator);
    }

    public List<Operator> operators(){
        return _operators;
    }

    public float[][] operatorsValues(){
        return _operatorsValues;
    }

    public void setSelection(Selection selection){
        _selection = selection;
    }

    public float best(){
        return _best != null ? _best.fitness() : -1;
    }

    public List<Node> structure(){
        return _structure;
    }

    public void setTarget(Node endEffector, Node target) {
        this._target.put(structure().indexOf(endEffector), target);
    }

    public float execute(){
        this._best = null;
        this._statistics = new ArrayList<Statistics>();
        _operatorsValues = new float[_operators.size()][_maxIterations];
        //1. Generate population
        _population = Util.generatePopulation(_structure, _population_size);
        //2. Update Fitness
        for (int i = 0; i < _population.size(); i++) {
            _population.get(i).updateFitness(_target);
            _population.set(i, new HAEAIndividual(_population.get(i)));
            ((HAEAIndividual)_population.get(i)).generateRates(_random, _operators);
            _best = _best == null ? _population.get(i) : _best.fitness() > _population.get(i).fitness() ? _population.get(i) : _best;
        }
        //3. Iterate a given number of times.
        int k = 0;
        while(k < _maxIterations){
            _iterate();
            _statistics.add(new Statistics(_population));
            for(int i = 0; i < _operators.size(); i++){
                for(int j = 0; j < _population_size; j++){
                    HAEAIndividual individual = (HAEAIndividual) _population.get(j);
                    _operatorsValues[i][k] += individual._operatorRates[i];
                }
                _operatorsValues[i][k] /= _population_size;
            }
            k++;
        }
        //4. Keep statistics
        return _best.fitness();
    }

    @Override
    protected boolean _iterate() {
        List<Individual> next = new ArrayList<>();

        for(Individual individual : _population){
            //learning rate
            float delta = _random.nextFloat()*0.5f;
            int index = ((HAEAIndividual) individual).chooseOperator(_random);
            Operator operator = _operators.get(index);
            List<Individual> children = new ArrayList<>();
            //TODO: Consider when output is more than 1 Individual and when operator requires selection
            if(_debug) {
                System.out.println("P1 : \n \t" + individual);
            }

            if(operator instanceof OperatorMethods.ConvexCombination){
                List<Individual> parents = _selection.choose(true, _population,1);
                //If individual has a worst fitness - it's better to choose the selected parent
                parents.add(individual);
                if(_debug) {
                    System.out.println("Cross ");
                    System.out.println("P2 : \n \t" + parents.get(0));
                }
                children.add(operator.apply(parents.toArray(new Individual[parents.size()])));
            } else{
                children.add(operator.apply(individual));
            }
            //Find best between children and individual
            Individual replacement = individual;
            boolean better = false;
            for(Individual child : children){
                child.updateFitness(_target);
                if(child.fitness() < replacement.fitness()){
                    replacement = child;
                    better = true;
                }
            }
            if(_debug) {
                System.out.println("Child : \n \t" + children.get(0));
            }

            if(better) {
                replacement = new HAEAIndividual(replacement);
                ((HAEAIndividual) replacement)._operatorRates = ((HAEAIndividual) individual)._operatorRates;
                //reward
                ((HAEAIndividual) replacement)._operatorRates[index] *= (1 + delta);
                ((HAEAIndividual) replacement)._operatorRates = ((HAEAIndividual) replacement)._operatorRates.clone();
            } else{
                //punish
                ((HAEAIndividual) replacement)._operatorRates[index] *= (1 - delta);
                ((HAEAIndividual) replacement)._operatorRates = ((HAEAIndividual) replacement)._operatorRates.clone();

            }

            if(_debug) {
                System.out.println("Replacement : \n \t" + replacement);
            }

            ((HAEAIndividual) replacement).normalizeRates();
            next.add(replacement);
            _best = _best == null ? replacement : _best.fitness() > replacement.fitness() ? replacement: _best;
        }

        _population = next;
        if(_debug) {
            System.out.println("Population ");
            for (Individual ind : _population) {
                System.out.print(ind);
                System.out.print(" Rates : ");
                for(int i = 0; i < ((HAEAIndividual) ind)._operatorRates.length; i++){
                    System.out.print(((HAEAIndividual) ind)._operatorRates[i] + " ");
                }
                System.out.println();
            }
        }
        return _best.fitness() < _minDistance;
    }

    public Node head() {
        return _structure.get(0);
    }

    public Node endEffector() {
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
        _iterations = 0;
        if(_target == null){
            _previousTarget = null;
            return;
        }
        for(Integer endEffector : _target.keySet()) {
            _previousTarget.put(endEffector, new Node(_target.get(endEffector).position(), _target.get(endEffector).orientation(), 1));
        }
        //If there is no population then genereate one
        _population = Util.generatePopulation(_structure, _population_size);
        for (int i = 0; i < _population.size(); i++) {
            _population.get(i).updateFitness(_target);
            _population.set(i, new HAEAIndividual(_population.get(i)));
            ((HAEAIndividual)_population.get(i)).generateRates(_random, _operators);
            _best = _best == null ? _population.get(i) : _best.fitness() > _population.get(i).fitness() ? _population.get(i) : _best;
        }
    }

    @Override
    public float error() {
        if(_best != null) _best.updateFitness(_target);
        return _best != null ? _best.fitness() : Float.NaN;
    }
}

