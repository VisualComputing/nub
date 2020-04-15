package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.ik.visualization.InterestingEvent;
import nub.ik.visualization.VisualizerMediator;
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
      int lastTime = mediator().finishingTime();
      for(Individual individual : parents) {
        InterestingEvent event = mediator().addEvent("SELECT_PARENTS", "HighlightStructure", lastTime, 1, 1);
        event.addAttribute("structure", individual.structure());
        InterestingEvent event2 = mediator().addEvent("SELECT_PARENTS", "HideStructure", lastTime + 1, 1, 1);
        event2.addAttribute("structure", individual.structure());
      }
      InterestingEvent messageEvent = mediator().addEvent("SELECT_MESSAGE", "Message", lastTime, 0, 2);
      messageEvent.addAttribute("message", "Choose from population the structures that will be used as parents"); //Add the convenient attributes
    }


    //2. Generate children
    Individual worst = _best;
    Individual best = _best;
    for (int i = 0; i < parents.size(); i += 2) {
      if(_enableMediator){
        InterestingEvent event1 = mediator().addEventStartingAfterLast("SELECT_PARENT1", "HighlightStructure", 1, 1);
        event1.addAttribute("structure", parents.get(i).structure());
        InterestingEvent event2 = mediator().addEventStartingWithLast("SELECT_PARENT2", "HighlightStructure", 1, 1);
        event2.addAttribute("structure", parents.get(i+1).structure());
        InterestingEvent messageEvent = mediator().addEventStartingWithLast("SELECT_PAIR_MESSAGE", "Message", 0, 1);
        messageEvent.addAttribute("message", "Choose two parents to match"); //Add the convenient attributes
        event1 = mediator().addEventStartingAfterLast("SELECT_PARENT1", "HideStructure", 1, 1);
        event1.addAttribute("structure", parents.get(i).structure());
        event2 = mediator().addEventStartingWithLast("SELECT_PARENT2", "HideStructure", 1, 1);
        event2.addAttribute("structure", parents.get(i+1).structure());
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
          InterestingEvent event = mediator().addEventStartingAfterLast("CHILD1", "UpdateStructure", 0, 1);
          Vector[] translations = new Vector[_children.get(i).structure().size()];
          Quaternion[] rotations = new Quaternion[_children.get(i).structure().size()];
            for(int k = 0; k < _children.get(i).structure().size(); k++){
                translations[k] = _children.get(i).structure().get(k).translation().get();
                rotations[k] = _children.get(i).structure().get(k).rotation().get();
            }
          event.addAttribute("structure", _children.get(i).structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);

          InterestingEvent event2 = mediator().addEventStartingWithLast("CHILD1_h", "HighlightStructure",1, 1);
          event2.addAttribute("structure", _children.get(i).structure());

          event2 = mediator().addEventStartingAfterLast("CHILD1_h", "HideStructure",1, 1);
          event2.addAttribute("structure", _children.get(i).structure());
        }


        Individual child2 = _crossover.apply(parents.get(i), parents.get(i + 1));
        child2 = _mutation.apply(child2);
        child2.updateFitness(_targets);
        _children.get(i+1).set(child2);

        if(_enableMediator){
          InterestingEvent event = mediator().addEventStartingWith("CHILD1","CHILD2", "UpdateStructure", 0, 1);
          Vector[] translations = new Vector[_children.get(i+1).structure().size()];
          Quaternion[] rotations = new Quaternion[_children.get(i+1).structure().size()];
          for(int k = 0; k < _children.get(i+1).structure().size(); k++){
            translations[k] = _children.get(i+1).structure().get(k).translation().get();
            rotations[k] = _children.get(i+1).structure().get(k).rotation().get();
          }
          event.addAttribute("structure", _children.get(i+1).structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);

          InterestingEvent event2 = mediator().addEventStartingWithLast("CHILD2_h", "HighlightStructure", 1, 1);
          event2.addAttribute("structure", _children.get(i+1).structure());
          event2 = mediator().addEventStartingAfterLast("CHILD2_h", "HideStructure", 1, 1);
          event2.addAttribute("structure", _children.get(i+1).structure());

          InterestingEvent messageEvent = mediator().addEventStartingWith("CHILD1","CHILD1_MESSAGE", "Message", 0, 2);
          //Add the convenient attributes
          messageEvent.addAttribute("message", "Generate children from selected parents");
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
              if(i == 0){
                  InterestingEvent event = mediator().addEventStartingAfterLast("POPULATION", "HighlightStructure", 1, 1);
                  event.addAttribute("structure", _population.get(i).structure());
                  event = mediator().addEventStartingAfterLast("CHILDREN", "HighlightStructure", 1, 1);
                  event.addAttribute("structure", _children.get(i).structure());
              } else{
                  InterestingEvent event = mediator().addEventStartingWith("POPULATION","POPULATION", "HighlightStructure", 1, 1);
                  event.addAttribute("structure", _population.get(i).structure());
                  event = mediator().addEventStartingWith("CHILDREN", "CHILDREN", "HighlightStructure", 1, 1);
                  event.addAttribute("structure", _children.get(i).structure());
              }
          }
          InterestingEvent messageEvent = mediator().addEventStartingWith("POPULATION","Population_MESSAGE", "Message", 0, 1);
          messageEvent.addAttribute("message", "Previous population"); //Add the convenient attributes

          messageEvent = mediator().addEventStartingWith("CHILDREN","Children_MESSAGE", "Message", 0, 1);
          messageEvent.addAttribute("message", "new children"); //Add the convenient attributes
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
                  if(_population.contains(concatenation.get(i))){
                      InterestingEvent event = mediator().addEventStartingAfter("CHILDREN", "REMOVED_POPULATION", "HideStructure", 1, 1);
                      event.addAttribute("structure", concatenation.get(i).structure());
                  } else{
                      InterestingEvent event = mediator().addEventStartingAfter("CHILDREN", "NEW_POPULATION", "HideStructure", 1, 1, 2);
                      event.addAttribute("structure", concatenation.get(i).structure());
                  }
              }
              InterestingEvent messageEvent = mediator().addEventStartingWith("REMOVED_POPULATION", "Population_MESSAGE", "Message", 0, 2);
              messageEvent.addAttribute("message", "Keep best individuals form previous population and offspring"); //Add the convenient attributes

              InterestingEvent event = mediator().addEventStartingWithLast("Best", "UpdateStructure", 0, 1);
              Vector[] translations = new Vector[_best.structure().size()];
              Quaternion[] rotations = new Quaternion[_best.structure().size()];
              for(int k = 0; k < _best.structure().size(); k++){
                translations[k] = _best.structure().get(k).translation().get();
                rotations[k] = _best.structure().get(k).rotation().get();
              }
              event.addAttribute("structure", _best.structure());
              event.addAttribute("rotations", rotations);
              event.addAttribute("translations", translations);

              event = mediator().addEventStartingAfterLast("BEST_HIGHLIGHT", "HighlightStructure",1, 1, 1);
              event.addAttribute("structure", _best.structure());

              event = mediator().addEventStartingAfterLast("BEST_HIDE", "HideStructure",1, 1);
              event.addAttribute("structure", _best.structure());

              messageEvent = mediator().addEventStartingWith("BEST_HIGHLIGHT","SELECT_MESSAGE", "Message", 0, 2);
              messageEvent.addAttribute("message", "Best solution obtained so far."); //Add the convenient attributes
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
    InterestingEvent event = mediator().addEventStartingAfterLast("structure", "UpdateStructure",1, 1);
    Vector[] translations = new Vector[_structure.size()];
    Quaternion[] rotations = new Quaternion[_structure.size()];
    for(int k = 0; k < _structure.size(); k++){
      translations[k] = _structure.get(k).translation().get();
      rotations[k] = _structure.get(k).rotation().get();
    }
    event.addAttribute("structure", _structure);
    event.addAttribute("rotations", rotations);
    event.addAttribute("translations", translations);

    InterestingEvent messageEvent = mediator().addEventStartingWithLast("STRUCTURE_MESSAGE", "Message",0, 1);
    messageEvent.addAttribute("message", "Set structure state according with best solution"); //Add the convenient attributes
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
      _previousTarget.put(endEffector, Node.detach(_targets.get(endEffector).position(), _targets.get(endEffector).orientation(), 1));
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
          int lastTime = mediator().finishingTime();
          for(Individual individual : _population) {
              Vector[] translations = new Vector[individual.structure().size()];
              Quaternion[] rotations = new Quaternion[individual.structure().size()];
              for(int k = 0; k < individual.structure().size(); k++){
                  translations[k] = individual.structure().get(k).translation().get();
                  rotations[k] = individual.structure().get(k).rotation().get();
              }
              InterestingEvent update = mediator().addEvent("GENERATE_POPULATION", "UpdateStructure", lastTime, 1, 0);
              update.addAttribute("structure", individual.structure());
              update.addAttribute("rotations", rotations);
              update.addAttribute("translations", translations);
              InterestingEvent highlight = mediator().addEventStartingAfter(update,"GENERATE_POPULATION", "HighlightStructure", 1, 1);
              highlight.addAttribute("structure", individual.structure());
              InterestingEvent hide = mediator().addEventStartingAfter(highlight, "GENERATE_POPULATION", "HideStructure", 1, 0);
              hide.addAttribute("structure", individual.structure());
          }
          InterestingEvent messageEvent = mediator().addEvent("SELECT_MESSAGE", "Message", lastTime, 0, 1);
          messageEvent.addAttribute("message", "Generate an initial population"); //Add the convenient attributes

          InterestingEvent event =  mediator().addEventStartingAfterLast("Best", "UpdateStructure", 0, 1);
          Vector[] translations = new Vector[_best.structure().size()];
          Quaternion[] rotations = new Quaternion[_best.structure().size()];
          for(int k = 0; k < _best.structure().size(); k++){
            translations[k] = _best.structure().get(k).translation().get();
            rotations[k] = _best.structure().get(k).rotation().get();
          }
          event.addAttribute("structure", _best.structure());
          event.addAttribute("rotations", rotations);
          event.addAttribute("translations", translations);

          event = mediator().addEventStartingWithLast("BEST_HIGHLIGHT", "HighlightStructure", 1, 1);
          event.addAttribute("structure", _best.structure());

          event = mediator().addEventStartingAfterLast("BEST_HIDE", "HideStructure", 1, 1);
          event.addAttribute("structure", _best.structure());

          messageEvent = mediator().addEventStartingWith("BEST_HIGHLIGHT","SELECT_MESSAGE", "Message", 0, 1);
          messageEvent.addAttribute("message", "Best solution obtained so far."); //Add the convenient attributes
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
