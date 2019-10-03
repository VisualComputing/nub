package nub.ik.animation;

import nub.processing.Scene;

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
        _keepDrawing = true;
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

}
