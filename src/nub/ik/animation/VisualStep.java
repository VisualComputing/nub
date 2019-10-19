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
    protected long _period, _stepDuration, _executionTimes, _renderingTimes;
    protected int _times, _totalExecutionTimes, _totalRenderingTimes;
    protected Scene _scene;
    //This are the visual attributes of the Step, since this varies among concrete subclasses we use a HashMap
    protected HashMap<String, Object> _attributes;

    public VisualStep(Scene scene, long period, long stepDuration, long executionTimes, long renderingTimes) {
        _scene = scene;
        _period = period;
        _stepDuration = stepDuration;
        _executionTimes = executionTimes;
        _renderingTimes = renderingTimes;
        _completed = false;
        _keepDrawing = true;
        _attributes = new HashMap<>();
        _defineAttributes();
    }

    public abstract void reverse();
    protected abstract void _onInit();
    protected abstract void _onRunning();
    protected abstract void _onComplete();
    protected abstract void _onTimeUpdate(int remainingTimes); //Call it at initialization and at each time period or step duration is modified

    public abstract void render();

    public void initialize(){
        _completed = false;
        _times = 0;
        _onInit();
        _setDuration();
    }

    public void execute(){
        if(!_completed && _times < _totalExecutionTimes){
            _onRunning();
        }
        _times++;
        if(!_completed && _times >= _totalExecutionTimes){
            _onComplete();
            _completed = true;
        }
        if(_times >= _totalRenderingTimes){
            _keepDrawing = false;
        }
    }

    public void setStepDuration(long stepDuration){
        _stepDuration = stepDuration;
        _setDuration();
    }

    public void setPeriod(long period){
        _period = period;
        _setDuration();
    }

    protected void _setDuration(){
        float frac = (1.f * _times/_totalExecutionTimes);
        _totalExecutionTimes = (int) Math.ceil( 1.0 * _stepDuration * _executionTimes / _period); //how many times to execute?
        _totalRenderingTimes = (int) Math.ceil( 1.0 * _stepDuration * _renderingTimes / _period); //how many times to render?
        _times = (int) Math.floor(_totalExecutionTimes * frac);
        _onTimeUpdate(_totalExecutionTimes - _times);
    }

    public boolean completed(){
        return _completed;
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
            throw new RuntimeException(name + " is not an attribute of the visual step. Please check the valid attrubtes calling at " +
                    "getAttributes method.");
        }
        _attributes.put(name, value);
    }

    protected abstract void _defineAttributes();
}
