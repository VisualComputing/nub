package nub.ik.evolution;

import nub.core.Node;
import nub.ik.Solver;
import nub.ik.evolution.operator.Operator;
import nub.ik.evolution.operator.OperatorMethods;
import nub.ik.evolution.selection.Selection;
import nub.ik.evolution.selection.SelectionMethods;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.*;


/**
 * Created by sebchaparr on 11/01/19.
 */
public class BioIk extends Solver {
    public enum Replacement {
        GENERATIONAL, ELITISM, KEEP_BEST
    }

    protected Selection _selection = new SelectionMethods.Ranking(true);
    protected Operator _mutation = new OperatorMethods.Mutation();
    protected Operator _crossover = new OperatorMethods.Recombination();
    protected Operator _adoption = new OperatorMethods.Adoption();
    protected float _cross_probability = 1f;
    protected int _population_size;
    protected int _elitism_size;
    protected float _max_v = 0.0f;
    protected Replacement replacement = Replacement.ELITISM;

    protected Random _random = new Random();
    protected HashMap<Integer, Node> _previousTarget;
    protected HashMap<Integer, Node> _target;
    protected List<Node> _structure;
    protected HashMap<Integer, Float> _chain_length; //Key : Target
    protected List<HashMap<Integer, Float>> _chain_eff_length; // Key : Target

    protected List<Individual> _population, _sorted_population;
    protected Individual _best;


    protected List<Statistics> _statistics;

    protected boolean _debug = false;

    public BioIk(List<Node> structure, int population_size, int elitism_size) {
        this._structure = structure;
        this._population_size = population_size;
        this._elitism_size = elitism_size;
        this._target = new HashMap<Integer, Node>();
        this._previousTarget = new HashMap<Integer, Node>();
        _updateChainLength();
    }

    public BioIk(Node root, int population_size, int _elitism_size) {
        this._structure = root.graph().branch(root);
        this._population_size = population_size;
        this._target = new HashMap<Integer, Node>();
        this._previousTarget = new HashMap<Integer, Node>();
        _updateChainLength();
    }


    public int populationSize() {
        return _population_size;
    }

    public int elitismSize(){ return  _elitism_size; }

    public float crossProbability() {
        return _cross_probability;
    }

    public List<Statistics> statistics() {
        return _statistics;
    }

    public void setPopulationSize(int size) {
        _population_size = size;
    }

    public void setCrossProbability(float probability) {
        _cross_probability = probability;
    }

    public void setSelection(Selection selection) {
        _selection = selection;
    }

    public void setMutation(Operator mutation) {
        _mutation = mutation;
    }

    public void setCrossover(Operator crossover) {
        _crossover = crossover;
    }

    public List<Node> structure() {
        return _structure;
    }

    public float best() {
        return _best != null ? _best.fitness() : -1;
    }

    public void setTarget(Node endEffector, Node target) {
        this._target.put(structure().indexOf(endEffector), target);
    }

    public float execute() {
        _updateChainLength();
        this._best = null;
        this._statistics = new ArrayList<Statistics>();
        //1. Generate population
        _population = Util.generatePopulation(_structure, _population_size);
        //2. Update Fitness
        for (Individual individual : _population) {
            individual.updateFitness(_target);
            _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
        }
        _sorted_population = Util.sort(false, false, _population);
        _updateExtinction();
        //3. Iterate a given number of times.
        int k = 0;
        while (k < maxIter) {
            _iterate();
            ArrayList<Individual> st = new ArrayList<>(_sorted_population.subList(0, _elitism_size));
            st.add(_best);
            _statistics.add(new Statistics(st));
            k++;
        }
        //4. Keep statistics
        return _best.fitness();
    }

    @Override
    protected boolean _iterate() {
        //1. Exploit _elitism_size individuals
        _exploit(_sorted_population.subList(0, _elitism_size));
        if (_debug) System.out.println("------- Iteration: " + iterations);
        Individual best = _sorted_population.get(0), worst = _sorted_population.get(_sorted_population.size() - 1);
        //2. Mating pool is same as population
        List<Individual> pool = new ArrayList<Individual>(_sorted_population);
        //3. Get remaining offspring
        List<Individual> children = new ArrayList<>();
        for (int i = 0; i < _population.size() - _elitism_size; i++) {
            if (!pool.isEmpty()) {
                Individual child;
                //Choose 2 Parents
                List<Individual> parents = _selection.choose(true, pool, 2);
                //Generate an individual
                if (_random.nextFloat() < _cross_probability) {
                    Individual recombination = _crossover.apply(parents.get(0), parents.get(1));
                    if (_mutation instanceof OperatorMethods.Mutation) {
                        ((OperatorMethods.Mutation) _mutation).setExtinction(parents.get(0), parents.get(1));
                    }
                    Individual mutation = _mutation.apply(recombination);
                    if (_adoption instanceof OperatorMethods.Adoption) {
                        ((OperatorMethods.Adoption) _adoption).setParents(parents.get(0), parents.get(1));
                        ((OperatorMethods.Adoption) _adoption).setBest(_best);
                    }
                    child = _adoption.apply(mutation);
                    //Clipping is not necessary since solutions are kept in feasible regions (see Frame setRotation())
                    best = best == null ? child : best.fitness() > child.fitness() ? child : best;
                    worst = worst == null ? child : worst.fitness() < child.fitness() ? child : worst;
                    child.updateFitness(_target);
                    child.arrayParams().put("Evolution_Gradient", _calculateGradient(child, recombination));
                    children.add(child);
                } else {
                    child = parents.get(0);
                    children.add(child);
                    child.updateFitness(_target);
                    best = best == null ? child : best.fitness() > child.fitness() ? child : best;
                    worst = worst == null ? child : worst.fitness() < child.fitness() ? child : worst;
                }
                //Remove parents in case child performs better.
                if (parents.get(0).fitness() < child.fitness()) pool.remove(parents.get(0));
                if (parents.get(1).fitness() < child.fitness()) pool.remove(parents.get(1));
            } else {
                //Get a random individual
                Individual child = Util.generateIndividual(_structure);
                children.add(child);
                child.updateFitness(_target);
                best = best == null ? child : best.fitness() > child.fitness() ? child : best;
                worst = worst == null ? child : worst.fitness() < child.fitness() ? child : worst;
            }
        }

        for (Individual ind : _sorted_population) {
            if (_debug) System.out.println(ind);
        }
        _population = Util.concatenate(_sorted_population.subList(0, _elitism_size), children);


        //Sort population
        if (_debug) System.out.println("<><><><><><><><><><><><>");
        if (_debug) System.out.println("Elite");
        for (Individual ind : _sorted_population.subList(0, _elitism_size)) {
            if (_debug) System.out.println(ind);
        }
        if (_debug) System.out.println("<><><><><><><><><><><><>");
        if (_debug) System.out.println("Offspring");
        for (Individual ind : children) {
            if (_debug) System.out.println(ind);
        }

        _sorted_population = Util.sort(false, false, _population);
        //Execute Wipe - Reinitialize population
        if (_wipe(_best, best)) {
            _population = Util.generatePopulation(best.fitness() < _best.fitness() ? best.structure() : _best.structure(), _population_size);
            for (Individual individual : _population) {
                individual.updateFitness(_target);
                _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
            }
            _sorted_population = Util.sort(false, false, _population);
        }
        //Update extinction Factor
        _updateExtinction();

        _best = best.fitness() < _best.fitness() ? best : _best;
        return _best.fitness() < minDistance;
    }


    public Node head() {
        return _structure.get(0);
    }

    public Node endEffector() {
        return _structure.get(_structure.size() - 1);
    }

    @Override
    protected void _update() {
        if(_max_v != 0) {
            for (int i = 0; i < _structure.size(); i++) {
                Quaternion delta = Quaternion.compose(_structure.get(i).orientation().inverse(), _best.structure().get(i).orientation());
                float angle = delta.angle() > _max_v ? _max_v : delta.angle() < -_max_v ? -_max_v : delta.angle();
                delta = new Quaternion(delta.axis(), angle);
                _structure.get(i).setOrientation(Quaternion.compose(_structure.get(i).orientation(), delta));
            }
        } else{
            for (int i = 0; i < _structure.size(); i++) {
                _structure.get(i).setRotation(_best.structure().get(i).rotation().get());
            }
        }
    }

    protected void _updateExtinction() {
        float f_max = _sorted_population.get(_sorted_population.size() - 1).fitness();
        float f_min = _sorted_population.get(0).fitness();
        for (int i = 0; i < _population_size; i++) {
            Individual individual = _population.get(i);
            individual._floatParams.put("Extinction", (individual.fitness() + f_min * (i / (_population_size - 1.f) - 1.f)) / f_max);
        }
    }


    protected float[] _calculateGradient(Individual amr, Individual r) {
        float[] g = new float[amr.structure().size() * 3];
        for (int i = 0; i < amr.structure().size(); i++) {
            Vector v = Vector.subtract(amr.structure().get(i).rotation().eulerAngles(), r.structure().get(i).rotation().eulerAngles());
            //Simple gradient g = 0 or Keeping information of previous gradient (Momentum)
            g[3 * i] = v.x();
            g[3 * i + 1] = v.y();
            g[3 * i + 2] = v.z();
            //g[3 * i] = Util.random.nextFloat() * g[3 * i] + v.x();
            //g[3 * i + 1] = Util.random.nextFloat() * g[3 * i + 1] + v.y();
            //g[3 * i + 2] = Util.random.nextFloat() * g[3 * i + 2] + v.z();
        }
        return g;
    }

    protected void _exploit(List<Individual> population) {
        for (Individual individual : population) {
            if (_debug) System.out.println("<<<<<<< Exploitation >>>>>>>");
            if (_debug) System.out.println("Ind " + individual);
            float new_fitness = 0;
            int idx = 0;
            int f = 0;
            for (Node joint : individual.structure()) {
                if (_debug) System.out.println("<<<<<<< Joint >>>>>>>");
                for (int i = 0; i < 3; i++) {
                    individual.updateFitness(_target);
                    float fitness = individual.balancedFitness();
                    float error = 0;
                    for(int index : _chain_eff_length.get(f).keySet()){
                        float dist = Vector.distance(individual.structure().get(individual.structure().size() - 1).position(), _target.get(index).position());
                        float l = _chain_eff_length.get(f).get(index);
                        float d = Vector.distance(individual.structure().get(individual.structure().size() - 1).position(), individual.structure().get(f).position());
                        error += (Math.PI * dist) / (Math.sqrt(l * d));
                    }
                    Vector euler = joint.rotation().eulerAngles();
                    Quaternion q = joint.rotation().get();
                    Vector angles = euler.get();
                    float r = Util.random.nextFloat() * error;
                    float plus = (float) Math.min(r + euler._vector[i], Math.PI);
                    angles._vector[i] = plus;
                    Quaternion q_plus = new Quaternion(angles._vector[0], angles._vector[1], angles._vector[2]);
                    joint.setRotation(q_plus);
                    individual.updateFitness(_target);
                    if (_debug) System.out.println("plus " + individual);

                    float f_plus = individual.balancedFitness();
                    float minus = (float) Math.max(euler._vector[i] - r, -Math.PI);
                    angles._vector[i] = minus;
                    Quaternion q_minus = new Quaternion(angles._vector[0], angles._vector[1], angles._vector[2]);
                    joint.setRotation(q_minus);
                    individual.updateFitness(_target);

                    if (_debug) System.out.println("minus " + individual);
                    float f_minus = individual.balancedFitness();

                    if (f_plus < fitness && f_plus <= f_minus) {
                        if (_debug) System.out.println("Entra plus");
                        joint.setRotation(q_plus);
                        individual.arrayParams().get("Evolution_Gradient")[idx] = euler._vector[i] * Util.random.nextFloat() + plus - euler._vector[i];
                        individual.setFitness(f_plus);
                    } else if (f_minus < fitness && f_minus <= f_plus) {
                        if (_debug) System.out.println("Entra minus");
                        joint.setRotation(q_minus);
                        individual.arrayParams().get("Evolution_Gradient")[idx] = euler._vector[i] * Util.random.nextFloat() + minus - euler._vector[i];
                        individual.setFitness(f_minus);
                    } else {
                        if (_debug) System.out.println("Entra normal");
                        joint.setRotation(q);
                        individual.arrayParams().get("Evolution_Gradient")[idx] = euler._vector[i];
                        individual.setFitness(fitness);
                    }
                    if (_debug) System.out.println("final " + individual);
                    idx += 1;
                    new_fitness += individual.balancedFitness();
                    assert individual.fitness() - fitness < 1e-3  : String.format("new Individual must have a lower fitness than previous one: " + (individual.fitness() - fitness));
                }
                f++;
            }
            //individual.setFitness(new_fitness/individual.structure().size());
        }
    }

    protected boolean _wipe(Individual current, Individual next) {
        for (Node joint : next.structure()) {
            for (int i = 0; i < 3; i++) {
                Vector euler = joint.rotation().eulerAngles();
                Quaternion q = joint.rotation().get();
                Vector angles = euler.get();
                float r = Util.random.nextFloat() * next.fitness();
                float plus = (float) Math.min(r + euler._vector[i], Math.PI);
                angles._vector[i] = plus;
                Quaternion q_plus = new Quaternion(angles._vector[0], angles._vector[1], angles._vector[2]);
                joint.setRotation(q_plus);
                next.updateFitness(_target);
                float f_plus = next.balancedFitness();
                float minus = (float) Math.max(euler._vector[i] - r, -Math.PI);
                angles._vector[i] = minus;
                Quaternion q_minus = new Quaternion(angles._vector[0], angles._vector[1], angles._vector[2]);
                joint.setRotation(q_minus);
                next.updateFitness(_target);
                float f_minus = next.balancedFitness();

                if ((f_plus < next.fitness() && f_plus <= f_minus) || (f_minus < next.fitness() && f_minus <= f_plus)) {
                    joint.setRotation(q);
                    next.updateFitness(_target);
                    return false;
                }
                joint.setRotation(q);
                next.updateFitness(_target);
            }
        }
        return next.balancedFitness() < current.balancedFitness();
    }

    protected void _updateChainLength(){
        _chain_length = new HashMap<>();
        _chain_eff_length = new ArrayList<>(_structure.size());
        HashMap<Integer, Integer> joint = new HashMap<>();
        for(int i = 0; i < _structure.size(); i++){
            Node f = _structure.get(i);
            joint.put(f.id(), i);
            _chain_eff_length.add(new HashMap<>());
        }

        Node prev = null;
        for(Integer index : _target.keySet()) {
            float l = 0;
            for (Node f : Node.path(_structure.get(0), _structure.get(index))) {
                if (prev != null) l += Vector.distance(f.position(), prev.position());
                prev = f;
                _chain_eff_length.get(joint.get(f.id())).put(index, l);
            }
            _chain_length.put(index, l);
        }
    }


    @Override
    protected boolean _changed() {
        //TODO : Differentiate between Chain change and Target change
        if (_target == null) {
            _previousTarget = null;
            return false;
        } else if (_previousTarget == null) {
            return true;
        }
        for (Integer endEffector : _target.keySet()) {
            if (_previousTarget.get(endEffector) == null) return true;
            if (!(_previousTarget.get(endEffector).position().matches(_target.get(endEffector).position()) &&
                    _previousTarget.get(endEffector).orientation().matches(_target.get(endEffector).orientation()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void _reset() {
        _updateChainLength();
        _best = null;
        iterations = 0;
        if (_target == null) {
            _previousTarget = null;
            return;
        }
        for (Integer endEffector : _target.keySet()) {
            _previousTarget.put(endEffector, new Node(_target.get(endEffector).position(), _target.get(endEffector).orientation(), 1));
        }
        //If there is no population then generate one
        _population = Util.generatePopulation(_structure, _population_size);
        for (Individual individual : _population) {
            individual.updateFitness(_target);
            _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
        }

        _sorted_population = Util.sort(false, false, _population);
        _updateExtinction();
    }

    @Override
    public float error() {
        if(_best != null) _best.updateFitness(_target);
        return _best != null ? _best.fitness() : Float.NaN;
    }
}