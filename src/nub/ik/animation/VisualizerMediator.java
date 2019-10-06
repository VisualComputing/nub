package nub.ik.animation;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.*;

/**
 * This class is responsible of deal with interactions between
 * a collection of visualizers and a solver.
 *
 * Usually we provide a visualizer with information from solver
 * i.e we add the interesting events occured during execution.
 *
 * Although it is less common, it is also possible to interact with
 * the visualizer and update the subscribed solver-
 * */

public class VisualizerMediator {
    protected class NodeState{
        protected Vector _position;
        protected Quaternion _orientation;
        protected NodeState(Node node){
            _position = node.position();
            _orientation = node.orientation();
        }
        protected Vector position(){
            return _position;
        }
        protected Quaternion orientation(){
            return _orientation;
        }
    }


    protected List<Visualizer> _visualizers = new ArrayList<>();
    protected Solver _solver;
    protected List<InterestingEvent> _eventQueue = new ArrayList<>();
    protected HashMap<Node, NodeState> _initialState = new HashMap<>();
    protected long _firstEvent = 0;
    protected final long MAX_EVENT_SIZE = 1000;


    public VisualizerMediator(Solver solver, Visualizer... visualizers){
        _solver = solver;
        _solver.setMediator(this);
        for(Visualizer v : visualizers){
            addVisualizer(v);
        }
    }

    public long lastEvent(){
        return _firstEvent + _eventQueue.size();
    }

    public InterestingEvent event(long id){
        return _eventQueue.get((int)(id - _firstEvent));
    }


    public List<InterestingEvent> eventQueue(){
        return _eventQueue;
    }

    public void addVisualizer(Visualizer v){
        _visualizers.add(v);
        v.setMediator(this);
        _solver.registerStructure(this);
        v._time = (_firstEvent + _eventQueue.size()) * v._period;
    }


    public void addEvent(InterestingEvent event){
        //Insert the event at its corresponding position o(n)
        if(_eventQueue.isEmpty()){
            _eventQueue.add(event);
            return;
        }
        for(int i = _eventQueue.size() -1; i >= 0; i--){
            if(event.startingTime() >= _eventQueue.get(i).startingTime()){
                if(i + 1 < _eventQueue.size()) _eventQueue.add(i + 1, event);
                else _eventQueue.add(event);
                break;
            }
        }
        if(_eventQueue.size() > MAX_EVENT_SIZE){
            _limitEventQueue();
        }
    }

    protected void _limitEventQueue(){
        //How many events must be removed ?
        _firstEvent += _eventQueue.size() - MAX_EVENT_SIZE;
        //Make sure that each visualizer is updated to new first event
        for(Visualizer v : _visualizers){
            v.jumpTo(_firstEvent);
        }
    }

    public void setInitialState(){
        Iterator<? extends Node> iterator = _solver.iterator();
        _initialState.clear();
        while(iterator.hasNext()){
            Node node = iterator.next();
            _initialState.put(node, new NodeState(node));
        }
    }

    public void sendInitialState(){
        for(Visualizer v : _visualizers){
            //reset all structures with the initial information
            _resetStructure(v);
        }
    }

    protected void _resetStructure(Visualizer v){
        for(Map.Entry<Node, Joint> entry : v._nodeToJoint.entrySet()){
            entry.getValue().setPosition(_initialState.get(entry).position());
            entry.getValue().setOrientation(_initialState.get(entry).orientation());
        }
    }

    public void registerStructure(Object structure){
        //Due to the flow of execution, the only visualizer that must be updated is the last one
        Visualizer v = _visualizers.get(_visualizers.size() -1);
        if(structure instanceof Node){
            v.registerStructure((Node) structure);
        } else if(structure instanceof List){
            v.registerStructure((List<? extends Node>) structure);
        }
    }
}
