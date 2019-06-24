package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.operator.Operator;
import nub.ik.solver.evolutionary.operator.OperatorMethods;
import nub.ik.solver.evolutionary.selection.Selection;
import nub.ik.solver.evolutionary.selection.SelectionMethods;

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
  protected int _population_size;
  protected Replacement replacement = Replacement.ELITISM;

  protected Random _random = new Random();
  protected HashMap<Integer, Node> _previousTarget;
  protected HashMap<Integer, Node> _targets;
  protected List<Node> _structure;
  protected List<Individual> _population;
  protected Individual _best;


  protected List<Statistics> _statistics;

  protected boolean _debug = false;

  public GASolver(List<Node> structure, int population_size) {
    this._structure = structure;
    this._population_size = population_size;
    this._targets = new HashMap<Integer, Node>();
    this._previousTarget = new HashMap<Integer, Node>();
    _selection = new SelectionMethods.Tournament();
    _mutation = new OperatorMethods.UniformMutation();
    _crossover = new OperatorMethods.ConvexCombination();
  }

  public int populationSize() {
    return _population_size;
  }

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
    this._targets.put(structure().indexOf(endEffector), target);
  }

  public float execute() {
    this._best = null;
    this._statistics = new ArrayList<Statistics>();
    //1. Generate population
    _population = Util.generatePopulation(_structure, _population_size);
    //2. Update Fitness
    for (Individual individual : _population) {
      individual.updateFitness(_targets);
      _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
    }
    //3. Iterate a given number of times.
    int k = 0;
    while (k < _maxIterations) {
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
    List<Individual> parents = _selection.choose(true, _population, _population_size);
    if (_debug) {
      System.out.println("ITERATION : " + _iterations);
      System.out.println("Parents");
      for (Individual ind : parents) {
        System.out.println(ind);
      }
    }

    //2. Generate children
    List<Individual> children = new ArrayList<>();
    Individual worst = null;
    Individual best = _best;
    for (int i = 0; i < parents.size(); i += 2) {
      if (_random.nextFloat() < _cross_probability) {
        Individual child1 = _crossover.apply(parents.get(i), parents.get(i + 1));
        if (_debug) {
          System.out.println("\t Best " + _best);
          System.out.println("\t P1 " + parents.get(i));
          System.out.println("\t P2 " + parents.get(i + 1));
          child1.updateFitness(_targets);
          System.out.println("\t Child " + child1);
        }
        child1 = _mutation.apply(child1);
        child1.updateFitness(_targets);
        children.add(child1);

        Individual child2 = _crossover.apply(parents.get(i), parents.get(i + 1));
        child2 = _mutation.apply(child2);
        child2.updateFitness(_targets);
        children.add(child2);

        best = best == null ? child1 : best.fitness() > child1.fitness() ? child1 : best;
        worst = worst == null ? child1 : worst.fitness() < child1.fitness() ? child1 : worst;
        best = best == null ? child2 : best.fitness() > child1.fitness() ? child2 : best;
        worst = worst == null ? child2 : worst.fitness() < child1.fitness() ? child2 : worst;
      } else {
        Individual child1 = parents.get(i);
        Individual child2 = parents.get(i + 1);
        if (replacement != Replacement.ELITISM) {
          children.add(child1);
          children.add(child2);
        }
        best = best == null ? child1 : best.fitness() > child1.fitness() ? child1 : best;
        worst = worst == null ? child1 : worst.fitness() < child1.fitness() ? child1 : worst;
        best = best == null ? child2 : best.fitness() > child1.fitness() ? child2 : best;
        worst = worst == null ? child2 : worst.fitness() < child1.fitness() ? child2 : worst;
      }
    }
    if (_debug) {
      System.out.println("Children ");
      for (Individual ind : children) {
        System.out.println(ind);
      }
    }

    //3. Replacement
    switch (replacement) {
      case ELITISM: {
        //Keep best individuals
        _population = Util.concatenate(_population, children);
        _population = Util.sort(true, false, _population);
        _best = _population.get(0);
        List<Individual> elite = _population.subList(0, 2 * _population_size / 3);
        List<Individual> other = _population.subList(2 * _population_size / 3, _population.size());
        Collections.shuffle(other);
        _population = elite;
        _population.addAll(other.subList(0, _population_size - elite.size()));
        Collections.shuffle(_population);
        if (_debug) {
          System.out.println("Population ");
          for (Individual ind : _population) {
            System.out.println(ind);
          }
        }
        break;
      }
      case GENERATIONAL: {
        _population = children;
        _best = best;
        break;
      }
      //Steady State
      case KEEP_BEST: {
        _population = children;
        if (!_population.contains(best)) {
          _population.remove(worst);
          _population.add(best);
        }
        _best = best;
        break;
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
    for (int i = 0; i < _structure.size(); i++) {
      _structure.get(i).setRotation(_best.structure().get(i).rotation().get());
    }
  }

  @Override
  protected boolean _changed() {
    if (_targets == null) {
      _previousTarget = null;
      return false;
    } else if (_previousTarget == null) {
      return true;
    }
    for (Integer endEffector : _targets.keySet()) {
      if (_previousTarget.get(endEffector) == null) return true;
      if (!(_previousTarget.get(endEffector).position().matches(_targets.get(endEffector).position()) &&
          _previousTarget.get(endEffector).orientation().matches(_targets.get(endEffector).orientation()))) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void _reset() {
    _best = null;
    _iterations = 0;
    if (_targets == null) {
      _previousTarget = null;
      return;
    }
    for (Integer endEffector : _targets.keySet()) {
      _previousTarget.put(endEffector, new Node(_targets.get(endEffector).position(), _targets.get(endEffector).orientation(), 1));
    }
    //If there is no population then genereate one
    _population = Util.generatePopulation(_structure, _population_size);
    for (Individual individual : _population) {
      individual.updateFitness(_targets);
      _best = _best == null ? individual : _best.fitness() > individual.fitness() ? individual : _best;
    }
  }

  @Override
  public float error() {
    return _best != null ? _best.fitness() : Float.NaN;
  }
}
