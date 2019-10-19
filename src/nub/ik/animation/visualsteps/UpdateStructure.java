package nub.ik.animation.visualsteps;

import nub.core.Node;
import nub.ik.animation.VisualStep;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.List;

public class UpdateStructure extends VisualStep {
    protected List<? extends Node> _structure;
    protected Vector[] _initialTranslations, _finalTranslations, _deltaPerFrameTranslations, _deltaTranslations;
    protected Quaternion[] _initialRotations, _finalRotations, _deltaPerFrameRotations, _deltaRotations;


    public UpdateStructure(Scene scene, List<? extends Node> structure, long period, long stepDuration, long executionTimes, long renderingTimes) {
        super(scene, period, stepDuration, executionTimes, renderingTimes);
        _structure = structure;
    }

    public void setFinalState(Vector[] translations, Quaternion[] rotations){
        _finalTranslations = translations;
        _finalRotations = rotations;
    }

    public void setFinalTranslations(Vector[] translations){
        _finalTranslations = translations;
    }

    public void setFinalRotations(Quaternion[] rotations){
        _finalRotations = rotations;
    }

    @Override
    protected void _onTimeUpdate(int remainingTimes) {
        if(remainingTimes == 0) remainingTimes = 1;
        //Calculate deltas per frame
        for(int i = 0; i < _structure.size(); i++){
            //Given current status and remaining work find delta per frame
            Vector remainingTranslation = Vector.subtract(_finalTranslations[i], _structure.get(i).translation());
            Quaternion remainingRotation = Quaternion.compose(_structure.get(i).rotation().inverse(), _finalRotations[i]);
            _deltaPerFrameRotations[i] = new Quaternion(remainingRotation.axis(), remainingRotation.angle() / remainingTimes);
            _deltaPerFrameTranslations[i] = Vector.multiply(remainingTranslation, 1.f/remainingTimes);
        }
    }


    @Override
    public void _onInit() {
        int n =  _structure.size();
        _initialTranslations = new Vector[n];
        _deltaTranslations = new Vector[n];
        _deltaPerFrameTranslations = new Vector[n];
        _initialRotations = new Quaternion[n];
        _deltaRotations = new Quaternion[n];
        _deltaPerFrameRotations = new Quaternion[n];
        for(int i = 0; i < n; i++){
            Node node =  _structure.get(i);
            _initialTranslations[i] = node.translation().get();
            _initialRotations[i] = node.rotation().get();
            _deltaRotations[i] = Quaternion.compose(node.rotation().inverse(), _finalRotations[i]);
            _deltaTranslations[i] = Vector.subtract(_finalTranslations[i], node.translation());
        }
    }

    @Override
    public void reverse() {

    }

    @Override
    protected void _onComplete(){
        //set rotation to fit exactly final rotation
        for(int i = 0; i < _structure.size(); i++){
            _structure.get(i).setRotation(_finalRotations[i]);
            _structure.get(i).setTranslation(_finalTranslations[i]);
        }
    }

    @Override
    protected void _onRunning(){
        for(int i = 0; i < _structure.size(); i++) {
            if (1 - Math.pow(Quaternion.dot(_structure.get(i).rotation(), _finalRotations[i]), 2) >= 0.000001) {
                _structure.get(i).rotate(_deltaPerFrameRotations[i]);
                _structure.get(i).translate(_deltaPerFrameTranslations[i]);
            }
        }
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
