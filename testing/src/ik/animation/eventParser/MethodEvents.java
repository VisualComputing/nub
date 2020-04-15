package ik.animation.eventParser;

import nub.ik.visualization.InterestingEvent;
import java.util.ArrayList;
import java.util.List;

public class MethodEvents{
    protected String _name;
    protected List<InterestingEvent> _eventQueue = new ArrayList<InterestingEvent>();
    public MethodEvents(String name){
        _name = name;
    }

    public int finishingTime(){
        return !_eventQueue.isEmpty() ? _eventQueue.get(_eventQueue.size() - 1).finishingTime() : 0;
    }

    public long lastEvent(){
        return _eventQueue.size();
    }

    public InterestingEvent event(int id){
        return _eventQueue.get(id);
    }

    //Methods for facilitating event insertion on events queue
    public void addEvent(InterestingEvent event){
        //Insert the event at its corresponding position o(n)
        if(_eventQueue.isEmpty()){
            _eventQueue.add(event);
            return;
        }

        boolean added = false;
        for(int i = _eventQueue.size() -1; i >= 0; i--){
            InterestingEvent current = _eventQueue.get(i);
            if(event.startingTime() > current.startingTime() ||  (event.startingTime() == current.startingTime() && event.finishingTime() >= current.finishingTime())){
                if(i + 1 < _eventQueue.size()) _eventQueue.add(i + 1, event);
                else _eventQueue.add(event);
                added = true;
                break;
            }
        }
        if(!added) _eventQueue.add(0,event);
    }

    public InterestingEvent lastEventWithName(String name){
        for(int i = _eventQueue.size() - 1; i >= 0; i--){
            if(_eventQueue.get(i).name().equals(name)) return _eventQueue.get(i);
        }
        return null;
    }

    public InterestingEvent addEvent(String name, String type, int startingTime, int executionDuration, int renderingDuration){
        InterestingEvent event = new InterestingEvent(name, type);
        event.startAt(startingTime);
        event.setDuration(executionDuration, renderingDuration);
        addEvent(event);
        return event;
    }

    public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration, int renderingDuration, int wait){
        int start = previous.finishingTime() + wait;
        return  addEvent(name, type, start, executionDuration, renderingDuration);
    }

    public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration, int renderingDuration){
        return  addEventStartingAfter(previous, name, type, executionDuration, renderingDuration, 0);
    }

    public InterestingEvent addEventStartingAfter(InterestingEvent previous, String name, String type, int executionDuration){
        return  addEventStartingAfter(previous, name, type, executionDuration, executionDuration, 0);
    }

    //Starts the event right after the last event with the given name (if there's no one an error will be thrown)
    public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration, int renderingDuration, int wait){
        InterestingEvent event = lastEventWithName(previousEventName);
        if(!eventQueue().isEmpty())System.out.println(eventQueue().get(eventQueue().size()-1).name());
        if(event == null) throw new RuntimeException("There is no event with name " + previousEventName);
        return  addEventStartingAfter(event, name, type, executionDuration, renderingDuration, wait);
    }

    public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration, int renderingDuration){
        return  addEventStartingAfter(previousEventName, name, type, executionDuration, renderingDuration, 0);
    }

    public InterestingEvent addEventStartingAfter(String previousEventName, String name, String type, int executionDuration){
        return  addEventStartingAfter(previousEventName, name, type, executionDuration, executionDuration, 0);
    }

    public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration, int renderingDuration, int wait){
        return  addEvent(name, type, finishingTime() + wait,executionDuration, renderingDuration);
    }

    public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration, int renderingDuration){
        return  addEvent(name, type, finishingTime(),executionDuration, renderingDuration);
    }

    public void addEventStartingAfterLast(InterestingEvent event){
        event.startAt(finishingTime());
        addEvent(event);
    }

    public InterestingEvent addEventStartingAfterLast(String name, String type, int executionDuration){
        return  addEvent(name, type, finishingTime(),executionDuration, executionDuration);
    }

    public InterestingEvent addEventStartingWith(InterestingEvent event, String name, String type, int executionDuration, int renderingDuration){
        int start = event.startingTime();
        return  addEvent(name, type, start, executionDuration, renderingDuration);
    }

    public InterestingEvent addEventStartingWith(String concurrentEventName, String name, String type, int executionDuration, int renderingDuration){
        InterestingEvent event = lastEventWithName(concurrentEventName);
        if(event == null) throw new RuntimeException("There is no event with name " + concurrentEventName);
        return  addEventStartingWith(event, name, type, executionDuration, renderingDuration);
    }

    public InterestingEvent addEventStartingWith(String concurrentEventName, String name, String type, int executionDuration){
        return  addEventStartingWith(concurrentEventName, name, type, executionDuration, executionDuration);
    }

    public InterestingEvent addEventStartingWithLast(String name, String type, int executionDuration, int renderingDuration){
        if(_eventQueue.isEmpty()) return addEvent(name, type, finishingTime(), executionDuration, renderingDuration);
        return  addEventStartingWith(_eventQueue.get(_eventQueue.size() - 1), name, type, executionDuration, renderingDuration);
    }

    public InterestingEvent addEventStartingWithLast(String name, String type, int executionDuration){
        return  addEventStartingWithLast(name, type, executionDuration, executionDuration);
    }

    public List<InterestingEvent> eventQueue(){
        return _eventQueue;
    }


    public InterestingEvent findPrevious(InterestingEvent ev){
        for(int i = _eventQueue.size() - 1; i >= 0; i--){
            InterestingEvent other = _eventQueue.get(i);
            if(other != ev && other.startingTime() <= ev.startingTime() ){
                return other;
            }
        }
        return null;
    }


    @Override
    public String toString(){
        String s = "Method name : " + _name + "\n";
        for(InterestingEvent e : _eventQueue){
            s += "\tEvent name : " + e.name() + "\n";
            s += "\t\t start : " + e.startingTime() + " execution : " + e.executionDuration() + " rendering : "  + e.renderingDuration() + "\n";
        }
        return s;
    }
}

