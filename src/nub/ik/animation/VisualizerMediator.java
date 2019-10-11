package nub.ik.animation;

import nub.core.Node;
import nub.ik.solver.Solver;

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
    protected List<Visualizer> _visualizers = new ArrayList<>();
    protected Solver _solver;
    protected List<InterestingEvent> _eventQueue = new ArrayList<>();
    protected long _firstEvent = 0;
    protected final long MAX_EVENT_SIZE = 5000;


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
        long firstEvent = _firstEvent + _eventQueue.size() - MAX_EVENT_SIZE;
        //Make sure that each visualizer is updated to new first event
        for(Visualizer v : _visualizers){
            v.jumpTo(firstEvent);
        }
        _firstEvent = firstEvent;
        //remove first events
        while(_eventQueue.size() > MAX_EVENT_SIZE) _eventQueue.remove(0);
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
