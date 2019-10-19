package nub.ik.animation.visualsteps;

import nub.core.Node;
import nub.ik.animation.VisualStep;
import nub.ik.visual.Joint;
import nub.processing.Scene;

import java.util.List;

//TODO: this works but must affect only joint transparency
public class HighLightStructure  extends VisualStep {
    protected List<? extends Node> _structure;
    protected int _init = 0, _end = 255;
    float _transparency, _delta;

    public HighLightStructure(Scene scene, List<? extends Node> structure, long period, long duration, long renderingDuration) {
        super(scene, period, duration, renderingDuration);
        _structure = structure;
    }

    public void setHighlight(int init, int end){
        _init = init;
        _end = end;
    }

    protected void _calculateSpeedPerIteration() {
        float inc = ((float) _period) / (_duration - _duration % _period);
        //Calculate deltas per frame
        _delta = (_end - _init)*inc;
    }


    @Override
    public void _onInit() {
        int n =  _structure.size();
        for(int i = 0; i < n; i++){
            if(_structure.get(i) instanceof Joint){
                ((Joint) _structure.get(i)).setAlpha((int)_transparency);
            }
        }
        _transparency = _init;
        _calculateSpeedPerIteration();
    }

    @Override
    public void reverse() {

    }

    @Override
    protected void _onComplete(){
        for(int i = 0; i < _structure.size(); i++){
            if(_structure.get(i) instanceof Joint) ((Joint) _structure.get(i)).setAlpha(_end);
        }
    }

    @Override
    protected void _onRunning(){
        for(int i = 0; i < _structure.size(); i++){
            if(_structure.get(i) instanceof Joint) ((Joint) _structure.get(i)).setAlpha((int)_transparency);
        }
        _transparency += _delta;
    }

    @Override
    public void render() {
        //Do nothing
    }

    @Override
    protected void _defineAttributes() {
        //Do nothing
    }
}
