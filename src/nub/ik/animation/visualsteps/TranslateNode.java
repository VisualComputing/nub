package nub.ik.animation.visualsteps;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.VisualStep;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

public class TranslateNode extends VisualStep {
    protected Node _node;
    protected boolean _enableConstraint = true, _modifyChildren = true, _useGlobalCoordinates = false;
    protected Vector _initial, _final, _deltaPerFrame, _delta;
    protected Constraint _constraint;

    public TranslateNode(Scene scene, Node node, long period, long duration, long renderingDuration) {
        super(scene, period, duration, renderingDuration);
        _node = node;
    }

    public void setTranslation(Vector delta) {
        _completed = false;
        _delta = delta;
        initialize();
    }

    public void enableConstraint(boolean enable){
        _enableConstraint = enable;
    }

    public void useGlobalCoordinates(boolean useGlobalCoordinates){
        _useGlobalCoordinates = useGlobalCoordinates;
    }

    public void modifyChildren(boolean modifyChildren){
        _modifyChildren = modifyChildren;
    }

    protected void _calculateSpeedPerIteration() {
        _deltaPerFrame = Vector.multiply(_delta, 1.f/Math.max(1.f,_totalTimes));
    }

    @Override
    public void _onInit() {
        _initial = _node.translation().get();
        if(!_enableConstraint){
            _constraint = _node.constraint();
        }
        if(_useGlobalCoordinates){
            _delta = _node.reference() != null ? _node.reference().displacement(_delta) : _delta;
        }
        _delta = _enableConstraint && _node.constraint() != null ? _node.constraint().constrainTranslation(_delta, _node) : _delta;
        _final = Vector.add(_initial, _delta);
        _calculateSpeedPerIteration();
    }

    @Override
    public void reverse() {

    }


    @Override
    protected void _onComplete(){
        //set translation to fit exactly final translation
        _applyTranslation(Vector.subtract(_final, _node.translation()));
    }

    @Override
    protected void _onRunning(){
        if (Vector.distance(_node.translation(), _final) >= 0.00001) {
            _applyTranslation(_deltaPerFrame);
        }
    }

    protected void _applyTranslation(Vector delta){
        if(!_enableConstraint) _node.setConstraint(null);
        _node.translate(delta);
        if(!_modifyChildren){
            for(Node child : _node.children()){
                Vector translation = Vector.multiply(delta, -1);
                translation = _node.rotation().inverseRotate(translation);
                child.translate(translation);
            }
        }
        if(!_enableConstraint) _node.setConstraint(_constraint);
    }

    @Override
    public void render() {
        if(!(boolean)_attributes.get("highlight")) return;
        PGraphics pg = _scene.context();
        pg.pushStyle();
        pg.hint(PConstants.DISABLE_DEPTH_TEST);
        pg.pushMatrix();
        _scene.applyWorldTransformation(_node);
        pg.noStroke();
        if(!_completed) pg.fill((int)_attributes.get("color"));
        else pg.fill((int)_attributes.get("color"), 150);
        pg.sphere((float)_attributes.get("radius"));
        pg.popMatrix();
        pg.hint(PConstants.ENABLE_DEPTH_TEST);
        pg.popStyle();
    }

    @Override
    protected void _defineAttributes(){
        _attributes.put("highlight", true);
        _attributes.put("color", _scene.pApplet().color(0, 0, 255));
        _attributes.put("radius", _scene.radius()*0.02f);
    }
}