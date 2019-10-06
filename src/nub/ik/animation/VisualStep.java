package nub.ik.animation;

import nub.processing.Scene;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Visual Step receive as input key information preserved by an interesting event and
 * gives then a visual representation.
 *
 * Visual steps handles the execution flow of an animated algorithm, hence, it is required
 * to distinguish among two vital tasks:
 *  a. execute a Step: which allows to performs smoothly transitions (deals with the dynamically behavior of an animation)
 *  b. render a Step: gives a visual representation of the information transformed smoothly previously
 * */

public abstract class VisualStep {
    protected boolean _initialized, _completed, _keepDrawing;
    protected long _period, _duration, _renderingDuration;
    protected int _times, _totalTimes, _totalRenderingTimes;
    protected Scene _scene;
    //This are the visual attributes of the Step, since this varies among concrete subclasses we use a HashMap
    protected HashMap<String, Object> _attributes;

    public abstract void initialize();
    public abstract void reverse();
    public abstract void execute();
    public abstract void render();

    public boolean completed(){
        return _completed;
    }

    public VisualStep(Scene scene, long period, long duration, long renderingDuration) {
        _scene = scene;
        _period = period;
        _duration = duration;
        _renderingDuration = renderingDuration;
        _completed = false;
        _keepDrawing = true;
        _attributes = new HashMap<>();
        _defineAttributes();
    }

    public boolean isInitialized(){
        return _initialized;
    }

    public boolean keepDrawing(){
        return _keepDrawing;
    }

    public void isCompleted(boolean completed){
        _completed = completed;
    }

    public void keepDrawing(boolean keep){
        _keepDrawing = keep;
    }

    public Set<String> getAttributes(){
        return _attributes.keySet();
    }

    public HashMap<String, Object> attributes(){
        return _attributes;
    }

    public void setAttributes(HashMap<String, Object> attributes){
        if(attributes == null) return;
        for(Map.Entry<String, Object> entry : attributes.entrySet()){
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    public void setAttribute(String name, Object value){
        if(_attributes.get(name) == null){
            throw new RuntimeException(name + "is not an attribute of the visual step. Please check the valid attrubtes calling at " +
                    "getAttributes method.");
        }
        _attributes.put(name, value);
    }

    protected abstract void _defineAttributes();
}
