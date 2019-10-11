package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.ik.animation.InterestingEvent;
import nub.ik.animation.VisualizerMediator;
import nub.ik.solver.KinematicStructure;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.operator.Operator;
import nub.ik.solver.evolutionary.operator.OperatorMethods;
import nub.ik.solver.evolutionary.selection.Selection;
import nub.ik.solver.evolutionary.selection.SelectionMethods;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

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
  protected List<Individual> _population, _children;
  protected Individual _best;
  protected List<Statistics> _statistics;
  protected boolean _debug = false;

  //Animation Stuff
  protected int _last_time_event = 0;

  public GASolver(List<Node> structure, int population_size) {
    this._structure = structure;
    this._population_size = population_size;
    this._targets = new HashMap<Integer, Node>();
    this._previousTarget = new HashMap<Integer, Node>();
    _population = _initList();
    _children = _initList();
    _best = new Individual(_structure, true);
    _selection = new SelectionMethods.Tournament();
    _mutation = new OperatorMethods.UniformMutation();
    _crossover = new OperatorMethods.ConvexCombination();
  }

  protected List<Individual> _initList(){
    List<Individual> list = new ArrayList<Individual>();
    for(int i = 0; i < _population_size; i++){
      list.add(new Individual(_structure, true));
    }
    return list;
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
    _population = _initList();
    _children = _initList();
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
    _best.updateFitness(_targets);
    this._statistics = new ArrayList<Statistics>();
    //1. Generate population
    Util.setupPopulation(_structure, _population);
    //2. Update Fitness
    Individual best = _best;
    for (int i = 0; i < _population.size(); i++) {
      _population.get(i).updateFitness(_targets);
      if(best.fitness() > _population.get(i).fitness()){
        best = _population.get(i);
      }
    }
    _best.set(best);
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

    if(_enableMediator){
      for(Individual individual : parents) {
        InterestingEvent event = new InterestingEvent("SELECT_PARENTS", "HighlightStructure", _last_time_event, 1, 1);
        event.addAttribute("structure", individual.structure());
        mediator().addEvent(event);

        InterestingEvent event2 = new InterestingEvent("SELECT_PARENTS", "HideStructure", _last_time_event + 1, 1, 1);
        event2.addAttribute("structure", individual.structure());
        mediator().addEvent(event2);
      }
      InterestingEvent messageEvent = new InterestingEvent("SELECT_MESSAGE", "Message", _last_time_event, 0, 2);
      //Add the convenient attributes
      messageEvent.addAttribute("message", "Choose from population the structures that will be used as parents");
      mediator().addEvent(messageEvent);
      _last_time_event+=2;
    }


    //2. Generate children
    Individual worst = _best;
    Individual best = _best;
    for (int i = 0; i < parents.size(); i += 2) {
      if(_enableMediator){
        InterestingEvent event1 = new InterestingEvent("SELECT_PARENT1", "HighlightStructure", _last_time_event, 1, 1);
        event1.addAttribute("structure", parents.get(i).structure());
        mediator().addEvent(event1);

        event1 = new InterestingEvent("SELECT_PARENT1", "HideStructure", _last_time_event + 2, 1, 1);
        event1.addAttribute("structure", parents.get(i).structure());
        mediator().addEvent(event1);

        InterestingEvent event2 = new InterestingEvent("SELECT_PARENT2", "HighlightStructure", _last_time_event, 1, 1);
        event2.addAttribute("structure", parents.get(i+1).structure());
        mediator().addEvent(event2);

        event2 = new InterestingEvent("SELECT_PARENT2", "HideStructure", _last_time_event + 2, 1, 1);
        event2.addAttribute("structure", parents.get(i+1).structure());
        mediator().addEvent(event2);

        InterestingEvent messageEvent = new InterestingEvent("SELECT_PAIR_MESSAGE", "Message", _last_time_event, 0, 1);
        //Add the convenient attributes
        messageEvent.addAttribute("message", "Choose two parents to match");
        mediator().addEvent(messageEvent);
        _last_time_event++;
      }

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
        _children.get(i).set(child1);

        if(_enableMediator){
          InterestingEvent event = new InterestingEvent("CHILD1", "UpdateStructure", _last_time_event, 0, 1);
          Vector[] translations = new Vector[_children.get(i).structure().size()];
          Quaternion[] rotations = new Quaternion[_children.get(i).structure().size()];
            for(int k = 0; k < _children.get(i).structure().size(); k++){
                translations[k] = _children.get(i).structure().get(k).translation().get();
                rotations[k] = _children.get(i).structure().get(k).rotation().get();
            }
          event.addAttribute("structure", _children.get(i).structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);
          mediator().addEvent(event);

          InterestingEvent event2 = new InterestingEvent("CHILD1_h", "HighlightStructure", _last_time_event, 1, 1);
          event2.addAttribute("structure", _children.get(i).structure());
          mediator().addEvent(event2);

          event2 = new InterestingEvent("CHILD1_h", "HideStructure", _last_time_event + 1, 1, 1);
          event2.addAttribute("structure", _children.get(i).structure());
          mediator().addEvent(event2);

        }


        Individual child2 = _crossover.apply(parents.get(i), parents.get(i + 1));
        child2 = _mutation.apply(child2);
        child2.updateFitness(_targets);
        _children.get(i+1).set(child2);

        if(_enableMediator){
          InterestingEvent event = new InterestingEvent("CHILD2", "UpdateStructure", _last_time_event, 0, 1);
          Vector[] translations = new Vector[_children.get(i+1).structure().size()];
          Quaternion[] rotations = new Quaternion[_children.get(i+1).structure().size()];
          for(int k = 0; k < _children.get(i+1).structure().size(); k++){
            translations[k] = _children.get(i+1).structure().get(k).translation().get();
            rotations[k] = _children.get(i+1).structure().get(k).rotation().get();
          }
          event.addAttribute("structure", _children.get(i+1).structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);
          mediator().addEvent(event);

          InterestingEvent event2 = new InterestingEvent("CHILD2_h", "HighlightStructure", _last_time_event, 1, 1);
          event2.addAttribute("structure", _children.get(i+1).structure());
          mediator().addEvent(event2);

            event2 = new InterestingEvent("CHILD2_h", "HideStructure", _last_time_event + 1, 1, 1);
            event2.addAttribute("structure", _children.get(i+1).structure());
            mediator().addEvent(event2);

          InterestingEvent messageEvent = new InterestingEvent("CHILD1_MESSAGE", "Message", _last_time_event, 0, 2);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "Generate children from selected parents");
          mediator().addEvent(messageEvent);
          _last_time_event+=2;
        }


        best = best.fitness() > child1.fitness() ? child1 : best;
        worst = worst.fitness() < child1.fitness() ? child1 : worst;
        best = best.fitness() > child1.fitness() ? child2 : best;
        worst = worst.fitness() < child1.fitness() ? child2 : worst;
      } else {
        Individual child1 = parents.get(i);
        Individual child2 = parents.get(i + 1);
        if (replacement != Replacement.ELITISM) {
          _children.get(i).set(child1);
          _children.get(i+1).set(child2);
        }
        best = best.fitness() > child1.fitness() ? child1 : best;
        worst = worst.fitness() < child1.fitness() ? child1 : worst;
        best = best.fitness() > child1.fitness() ? child2 : best;
        worst = worst.fitness() < child1.fitness() ? child2 : worst;
      }
    }
    if (_debug) {
      System.out.println("Children ");
      for (Individual ind : _children) {
        System.out.println(ind);
      }
    }

      if(_enableMediator){
          for(int i = 0; i < _population.size(); i++) {
              InterestingEvent event = new InterestingEvent("POPULATION", "HighlightStructure", _last_time_event, 1, 1);
              event.addAttribute("structure", _population.get(i).structure());
              mediator().addEvent(event);

              event = new InterestingEvent("POPULATION", "HighlightStructure", _last_time_event + 1, 1, 1);
              event.addAttribute("structure", _children.get(i).structure());
              mediator().addEvent(event);
          }
          InterestingEvent messageEvent = new InterestingEvent("Population_MESSAGE", "Message", _last_time_event, 0, 1);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "Previous population");
          mediator().addEvent(messageEvent);

          messageEvent = new InterestingEvent("Children_MESSAGE", "Message", _last_time_event + 1, 0, 1);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "new children");
          mediator().addEvent(messageEvent);
          _last_time_event+=2;
      }


    //3. Replacement
    switch (replacement) {
      case ELITISM: {
        //Keep best individuals
        List<Individual> concatenation = Util.concatenate(_population, _children);
        concatenation = Util.sort(true, false, concatenation);
        _best.set(concatenation.get(0));
        List<Individual> elite = concatenation.subList(0, 2 * _population_size / 3);
        List<Individual> other = concatenation.subList(2 * _population_size / 3, concatenation.size());
        Collections.shuffle(other);
        //children store the remaining ones
        _children.clear();
        _children.addAll(other.subList(_population_size - elite.size(), other.size()));
        _population.clear();
        _population.addAll(elite);
        _population.addAll(other.subList(0, _population_size - elite.size()));
        Collections.shuffle(_population);
        if (_debug) {
          System.out.println("Population ");
          for (Individual ind : _population) {
            System.out.println(ind);
          }
        }

          if(_enableMediator){
              for(int i = 0; i < concatenation.size(); i++) {
                  int inc = !_population.contains(concatenation.get(i)) ? 0 : 2;
                  InterestingEvent event = new InterestingEvent("population", "HideStructure", _last_time_event + inc, 1, 1);
                  event.addAttribute("structure", concatenation.get(i).structure());
                  mediator().addEvent(event);
              }
              InterestingEvent messageEvent = new InterestingEvent("Population_MESSAGE", "Message", _last_time_event, 0, 2);
              //Add the convenient attributes
              messageEvent.addAttribute("message", "Keep best individuals form previous population and offspring");
              mediator().addEvent(messageEvent);

              InterestingEvent event = new InterestingEvent("Best", "UpdateStructure", _last_time_event, 0, 1);
              Vector[] translations = new Vector[_best.structure().size()];
              Quaternion[] rotations = new Quaternion[_best.structure().size()];
              for(int k = 0; k < _best.structure().size(); k++){
                translations[k] = _best.structure().get(k).translation().get();
                rotations[k] = _best.structure().get(k).rotation().get();
              }
              event.addAttribute("structure", _best.structure());
              event.addAttribute("rotations", rotations);
              event.addAttribute("translations", translations);
              mediator().addEvent(event);


              event = new InterestingEvent("BEST_HIGHLIGHT", "HighlightStructure", _last_time_event + 2, 1, 1);
              event.addAttribute("structure", _best.structure());
              mediator().addEvent(event);

              event = new InterestingEvent("BEST_HIGHLIGHT", "HideStructure", _last_time_event + 3, 1, 1);
              event.addAttribute("structure", _best.structure());
              mediator().addEvent(event);


              messageEvent = new InterestingEvent("SELECT_MESSAGE", "Message", _last_time_event + 2, 0, 2);
              //Add the convenient attributes
              messageEvent.addAttribute("message", "Best solution obtained so far.");
              mediator().addEvent(messageEvent);
              _last_time_event+=4;

          }

        break;
      }
      case GENERATIONAL: {
        List<Individual> aux = _population;
        _population = _children;
        _children = aux;
        _best.set(best);
        break;
      }
      //Steady State
      case KEEP_BEST: {
        List<Individual> aux = _population;
        _population = _children;
        _children = aux;
        if (!_population.contains(best)) {
          if(_population.contains(worst))worst.set(best);
          else _population.get(0).set(best);
        }
        _best.set(best);
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
    InterestingEvent event = new InterestingEvent("structure", "UpdateStructure", _last_time_event, 1, 1);
    Vector[] translations = new Vector[_structure.size()];
    Quaternion[] rotations = new Quaternion[_structure.size()];
    for(int k = 0; k < _structure.size(); k++){
      translations[k] = _structure.get(k).translation().get();
      rotations[k] = _structure.get(k).rotation().get();
    }
    event.addAttribute("structure", _structure);
    event.addAttribute("rotations", rotations);
    event.addAttribute("translations", translations);
    mediator().addEvent(event);

    InterestingEvent messageEvent = new InterestingEvent("STRUCTURE_MESSAGE", "Message", _last_time_event, 0, 1);
    //Add the convenient attributes
    messageEvent.addAttribute("message", "Set structure state according with best solution");
    mediator().addEvent(messageEvent);
    _last_time_event ++;
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
      if (_previousTarget.get(endEffector) == null){
        return true;
      }
      if (!(_previousTarget.get(endEffector).position().matches(_targets.get(endEffector).position()) &&
          _previousTarget.get(endEffector).orientation().matches(_targets.get(endEffector).orientation()))) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void _reset() {
    _iterations = 0;
    if (_targets == null) {
      _previousTarget = null;
      return;
    }
    for (Integer endEffector : _targets.keySet()) {
      _previousTarget.put(endEffector, new Node(_targets.get(endEffector).position(), _targets.get(endEffector).orientation(), 1));
    }

    _best.setChain(_structure);
    _best.updateFitness(_targets);
    //1. Generate population
    Util.setupPopulation(_structure, _population);
    //2. Update Fitness
    Individual best = _best;
    for (int i = 0; i < _population.size(); i++) {
      _population.get(i).updateFitness(_targets);
      if(best.fitness() > _population.get(i).fitness()){
        best = _population.get(i);
      }
    }
      _best.set(best);

      if(_enableMediator){
          for(Individual individual : _population) {
              Vector[] translations = new Vector[individual.structure().size()];
              Quaternion[] rotations = new Quaternion[individual.structure().size()];
              for(int k = 0; k < individual.structure().size(); k++){
                  translations[k] = individual.structure().get(k).translation().get();
                  rotations[k] = individual.structure().get(k).rotation().get();
              }
              InterestingEvent event = new InterestingEvent("GENERATE_POPULATION", "UpdateStructure", _last_time_event, 1, 0);
              event.addAttribute("structure", individual.structure());
              event.addAttribute("rotations", rotations);
              event.addAttribute("translations", translations);
              mediator().addEvent(event);
              event = new InterestingEvent("GENERATE_POPULATION", "HighlightStructure", _last_time_event, 1, 1);
              event.addAttribute("structure", individual.structure());
              mediator().addEvent(event);
              InterestingEvent event2 = new InterestingEvent("GENERATE_POPULATION", "HideStructure", _last_time_event + 1, 1, 0);
              event2.addAttribute("structure", individual.structure());
              mediator().addEvent(event2);
          }
          InterestingEvent messageEvent = new InterestingEvent("SELECT_MESSAGE", "Message", _last_time_event, 0, 1);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "Generate an initial population");
          mediator().addEvent(messageEvent);
          _last_time_event++;

          InterestingEvent event = new InterestingEvent("Best", "UpdateStructure", _last_time_event, 0, 1);
          Vector[] translations = new Vector[_best.structure().size()];
          Quaternion[] rotations = new Quaternion[_best.structure().size()];
          for(int k = 0; k < _best.structure().size(); k++){
            translations[k] = _best.structure().get(k).translation().get();
            rotations[k] = _best.structure().get(k).rotation().get();
          }
          event.addAttribute("structure", _best.structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);
          mediator().addEvent(event);


          event = new InterestingEvent("BEST_HIGHLIGHT", "HighlightStructure", _last_time_event, 1, 1);
          event.addAttribute("structure", _best.structure());
          mediator().addEvent(event);

        event = new InterestingEvent("BEST_HIGHLIGHT", "HideStructure", _last_time_event + 1, 1, 1);
        event.addAttribute("structure", _best.structure());
        mediator().addEvent(event);

          messageEvent = new InterestingEvent("SELECT_MESSAGE", "Message", _last_time_event, 0, 1);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "Best solution obtained so far.");
          mediator().addEvent(messageEvent);
          _last_time_event++;
      }
  }

  @Override
  public float error() {
    return _best.fitness();
  }

    //Animation Stuff
    //TODO: Refactor, perhaps move to Solver class
    @Override
    public void registerStructure(VisualizerMediator mediator){
        mediator.registerStructure(_structure);
        mediator.registerStructure(_best._structure);

        for(Individual i : _population){
            mediator.registerStructure(i._structure);
        }
        for(Individual i : _children){
            mediator.registerStructure(i._structure);
        }
        for(Map.Entry<Integer, Node> entry : _targets.entrySet()){
            mediator.registerStructure(entry.getValue());
        }
    }

    @Override
    public Iterator<? extends Node> iterator(){
        return _structure.iterator();
    }

}
