package nub.ik.visualization.visualsteps;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.visualization.VisualStep;
import nub.primitives.Quaternion;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

public class RotateNode extends VisualStep {
    protected Node _node;
    protected boolean _enableConstraint = true, _modifyChildren = true;
    protected Quaternion _initial, _final, _deltaPerFrame, _delta;
    protected Constraint _constraint;

    public RotateNode(Scene scene, Node node, long period, long stepDuration, long executionTimes, long renderingTimes) {
        super(scene, period, stepDuration, executionTimes, renderingTimes);
        _node = node;
    }

    public void setRotation(Quaternion delta) {
        _completed = false;
        _delta = delta;
    }

    public void enableConstraint(boolean enableConstraint){
        _enableConstraint = enableConstraint;
    }

    public void modifyChildren(boolean modifyChildren){
        _modifyChildren = modifyChildren;
    }

    @Override
    protected void _onTimeUpdate(int remainingTimes) {
        //Given current status and remaining work find delta per frame
        Quaternion remaining = Quaternion.compose(_node.rotation().inverse(), _final);
        if(remainingTimes == 0) remainingTimes = 1;
        _deltaPerFrame = new Quaternion(remaining.axis(), remaining.angle() / remainingTimes);
    }

    @Override
    public void _onInit() {
        _initial = _node.rotation().get();
        if(!_enableConstraint){
            _constraint = _node.constraint();
        }
        _delta = _enableConstraint && _node.constraint() != null ? _node.constraint().constrainRotation(_delta, _node) : _delta;
        _final = Quaternion.compose(_initial, _delta);
    }

    @Override
    public void reverse() {

    }

    @Override
    protected void _onComplete(){
        //set rotation to fit exactly final rotation
        if(!_enableConstraint) _node.setConstraint(null);
        _node.setRotation(_final.get());
        if(!_enableConstraint) _node.setConstraint(_constraint);
    }

    @Override
    public void _onRunning() {
        if (1 - Math.pow(Quaternion.dot(_node.rotation(), _final), 2) >= 0.000001) {
            if(!_enableConstraint) _node.setConstraint(null);
            _node.rotate(_deltaPerFrame);
            if(!_modifyChildren){
                for(Node child : _node.children()){
                    Quaternion rot = Quaternion.compose(_deltaPerFrame.inverse(), child.rotation());
                    child.setRotation(rot);
                }
            }
            if(!_enableConstraint) _node.setConstraint(_constraint);
        }
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