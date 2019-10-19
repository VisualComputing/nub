package nub.ik.animation;

import java.util.HashMap;

/**
* An interesting event encapsulates key information that must be preserved in order to
 * visualize the execution of an IK Algorithm. Furthermore, Interesting events must provide
 * information about the flow of the animation Algorithm i.e when this interesting event
 * must be visualized and how much it takes to complete and how much time must still be visualized.
 *
 * An interesting event just by its own, does not provide any glimpse of how the information
 * preserved is visualized. To do so, it is required to couple it to a Visual Step via a Visualizer.
 * The intuition behind this is that an Event could have many different ways of be represented. We encourage to
 * use the predefined ones though.
* */

public class InterestingEvent{
    //This id will be used later identify and customize properly a visual event (Must be unique)
    protected String _name;
    //It is desirable to identify common behaviors across IK algorithms
    protected String _type;

    //Interesting Events, besides of their representation, must provide information about its initial time stamp
    //along with their execution rendering duration (discrete values)
    protected int _startingTime, _renderingDuration, _executionDuration;
    //each interesting event should contain a message that explains what is visually depicted
    //An event must contain a set of attributes that contains a key information to preserve
    protected HashMap<String, Object> _attributes;

    public InterestingEvent(String name, String type){
        _name = name;
        _type = type;
        _attributes = new HashMap<String, Object>();
    }

    public void addAttribute(String name, Object... values){
        if(values.length > 1 ) _attributes.put(name, values);
        else if(values.length == 1) _attributes.put(name, values[0]);
    }

    public Object getAttribute(String name){
        return _attributes.get(name);
    }

    public String type(){
        return _type;
    }

    public String name(){
        return _name;
    }

    public int renderingDuration(){
        return _renderingDuration;
    }

    public int executionDuration(){
        return _executionDuration;
    }

    public int startingTime(){
        return _startingTime;
    }

    public int finishingTime(){
        return _startingTime + _executionDuration;
    }

    //set starting time and duration
    public void startAt(int time){
        _startingTime = time;
    }

    public void startAfter(int wait, InterestingEvent previous){
        startAt(previous.finishingTime() + wait);
    }

    public void startAfter(InterestingEvent previous){
        startAfter(0, previous);
    }

    public void startWith(InterestingEvent event){
        startAt(event.startingTime());
    }

    public static void startAt(int time, InterestingEvent... events){
        for(InterestingEvent e : events){
            e.startAt(time);
        }
    }

    public static void startAfter(int time, int wait, InterestingEvent... events){
        startAt(time + wait, events);
    }

    public void setRenderingDuration(int renderingDuration){
        _renderingDuration = renderingDuration;
    }

    public void setExecutionDuration(int executionDuration){
        _executionDuration = executionDuration;
    }

    public void setDuration(int executionDuration, int renderingDuration){
        _executionDuration = executionDuration;
        _renderingDuration = renderingDuration;
    }
}
