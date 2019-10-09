package nub.ik.animation;


import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.List;

/**
 * Here you could find some common Visual Steps used in visualization of IK algorithms
 * */
public class VisualSteps {

    public static class TranslateNode extends VisualStep{
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
            float inc = ((float) _period) / (_duration - _duration % _period);
            _deltaPerFrame = Vector.multiply(_delta, inc);
        }

        @Override
        public void initialize() {
            _initial = _node.translation().get();
            if(!_enableConstraint){
                _constraint = _node.constraint();
            }
            if(_useGlobalCoordinates){
                _delta = _node.displacement(_delta);
            }

            _delta = _enableConstraint && _node.constraint() != null ? _node.constraint().constrainTranslation(_delta, _node) : _delta;
            _final = Vector.add(_initial, _delta);
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil( 1.0* _duration / _period);
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( 1.0* _renderingDuration / _period);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if (!_completed && _times >= _totalTimes) {
                //set translation to fit exactly final translation
                if(!_enableConstraint) _node.setConstraint(null);
                Vector v = Vector.subtract(_final, _node.translation());
                _node.translate(v);
                if(!_modifyChildren){
                    for(Node child : _node.children()){
                        Vector translation = Vector.multiply(v, -1);
                        translation = _node.rotation().inverseRotate(translation);
                        child.translate(translation);
                    }
                }
                if(!_enableConstraint) _node.setConstraint(_constraint);
                _completed = true;
            }else if (!_completed && Vector.distance(_node.translation(), _final) >= 0.00001) {
                if(!_enableConstraint) _node.setConstraint(null);
                _node.translate(_deltaPerFrame);
                if(!_modifyChildren){
                    for(Node child : _node.children()){
                        Vector translation = Vector.multiply(_deltaPerFrame, -1);
                        translation = _node.rotation().inverseRotate(translation);
                        child.translate(translation);
                    }
                }
                if(!_enableConstraint) _node.setConstraint(_constraint);
            }
            _times++;
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


    public static class RotateNode extends VisualStep{
        protected Node _node;
        protected boolean _enableConstraint = true, _modifyChildren = true;
        protected Quaternion _initial, _final, _deltaPerFrame, _delta;
        protected Constraint _constraint;

        public RotateNode(Scene scene, Node node, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
            _node = node;
        }

        public void setRotation(Quaternion delta) {
            _completed = false;
            _delta = delta;
            initialize();
        }

        public void enableConstraint(boolean enableConstraint){
            _enableConstraint = enableConstraint;
        }

        public void modifyChildren(boolean modifyChildren){
            _modifyChildren = modifyChildren;
        }

        protected void _calculateSpeedPerIteration() {
            float inc = ((float) _period) / (_duration - _duration % _period);
            _deltaPerFrame = new Quaternion(_delta.axis(), _delta.angle() * inc);
        }

        @Override
        public void initialize() {
            _initial = _node.rotation().get();
            if(!_enableConstraint){
                _constraint = _node.constraint();
            }
            _delta = _enableConstraint && _node.constraint() != null ? _node.constraint().constrainRotation(_delta, _node) : _delta;
            _final = Quaternion.compose(_initial, _delta);
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil( 1.0* _duration / _period);
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( 1.0* _renderingDuration / _period);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if (_times >= _totalTimes) {
                //set rotation to fit exactly final rotation
                if(!_enableConstraint) _node.setConstraint(null);
                _node.setRotation(_final.get());
                if(!_enableConstraint) _node.setConstraint(_constraint);
                _completed = true;
            }else if (1 - Math.pow(Quaternion.dot(_node.rotation(), _final), 2) >= 0.000001) {
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
            _times++;
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


    public static class MessageStep extends VisualStep {
        protected String _message;
        protected Vector _start_location, _end_location;

        public MessageStep(Scene scene, String message, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
            _message = message;
            initialize();
        }

        public void setLocation(){
            switch ((String) attributes().get("location")){
                case "TOP":{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 1.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  3.f * _scene.height() / 8.f);
                    break;
                }
                case "BOTTOM":{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 6.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  _scene.height());
                    break;
                }
                case "CUSTOM":{
                    _start_location = (Vector)_attributes.get("startLocation"); //in terms of screen
                    _end_location = (Vector) _attributes.get("endLocation");
                    break;
                }
                default:{
                    throw new RuntimeException("Location attribute must have either of the following values:" +
                            "TOP, BOTTOM or CUSTOM ");
                }
            }
        }

        @Override
        public void initialize() {
            _completed = false;
            _times = 0;
            setLocation();
            //how many times to execute?
            _totalTimes = (int) Math.ceil( 1.0* _duration / _period);
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( 1.0* _renderingDuration / _period);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if(_times >= _totalTimes) {
                _completed = true;
            }
            _times++;
        }

        @Override
        public void render() {
            PGraphics pg = _scene.context();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            pg.pushStyle();
            if (_message != null) {
                _scene.beginHUD();
                pg.stroke((int)_attributes.get("color"));
                pg.fill((int)_attributes.get("color"));
                pg.textSize((int)_attributes.get("textSize"));
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, _start_location.x(),_start_location.y(), _end_location.x(), _end_location.y());
                _scene.endHUD();
            }
            pg.popStyle();
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
        }

        @Override
        protected void _defineAttributes(){
            _attributes.put("location", "BOTTOM"); //Choose among the following modes: TOP, BOTTOM, CUSTOM
            _attributes.put("textSize", 24);
            _attributes.put("color", _scene.pApplet().color(255));
        }
    }

    public static class FollowTrajectoryStep extends VisualStep{
        protected Node _reference;
        protected Vector[] _trajectory;
        protected Vector _current, _delta;
        protected int _idx = 0;

        public FollowTrajectoryStep(Scene scene, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
        }

        public void setTrajectory(Node node, Vector... trajectory) {
            _reference = node;
            _trajectory = trajectory;
            initialize();
        }

        protected void _calculateSpeedPerIteration() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1];
            float inc = ((float) _period) / (_duration - _duration % _period);
            _delta = Vector.subtract(v2, v1);
            _delta.multiply(inc);
        }

        @Override
        public void initialize() {
            _idx = 0;
            _current = _trajectory[_idx].get();
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil( 1.0* _duration / _period);
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( 1.0* _renderingDuration / _period);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if(_times >= _totalTimes){
                _completed = true;
            } else {
                Vector v2 = _trajectory[_idx + 1];
                if (Vector.distance(_current, v2) < 0.1) {
                    _idx++;
                    if (_idx == _trajectory.length - 1) {
                        _idx--;
                    } else {
                        _current = v2;
                        _calculateSpeedPerIteration();
                    }
                } else {
                    _current.add(_delta);
                }
            }
            _times++;
        }

        @Override
        public void render() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];
            switch ((String) attributes().get("mode")) {
                case "V1_TO_V": {
                    v2 = _current;
                    break;
                }
                case "V_TO_V2": {
                    v1 = _current;
                    break;
                }
                case "V1_TO_V2":{
                    break;
                }
                default:{
                    throw new RuntimeException("mode attribute must have either of the following values:" +
                            "V1_TO_V, V_TO_V2 or V1_TO_V2 ");
                }
            }

            float _radius = (float)_attributes.get("radius");
            int cv = (int)_attributes.get("v_color");
            int cv1 = (int)_attributes.get("v1_color");
            int cv2 = (int)_attributes.get("v2_color");
            int cline = (int)_attributes.get("line_color");

            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.pushMatrix();
            if(_reference != null) _scene.applyWorldTransformation(_reference);
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if (_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                if(!_completed) pg.fill(cv2);
                else pg.fill(cv2, 150);
                pg.sphere(_radius);
                pg.popMatrix();
                pg.fill(cline);
                if (Vector.distance(v1, v2) > 1.2f * _radius) _scene.drawArrow(v1, v2, _radius / 4f);
            } else {
                drawSegment2D(pg, v1, v2, _radius, pg.color(cline, _completed ? 255 : 100), pg.color(cv1, _completed ? 255 : 100), pg.color(cv2, _completed ? 255 : 100));
                if(!_completed) pg.fill(cv);
                else pg.stroke(cv, 150);
                pg.ellipse(_current.x(), _current.y(), _radius, _radius);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            pg.popMatrix();
            pg.popStyle();
        }

        @Override
        protected void _defineAttributes(){
            _attributes.put("mode", "V1_TO_V"); //Choose among the following modes: V1_TO_V2, V_TO_V2, V1_TO_V
            _attributes.put("radius", _scene.radius()*0.02f);
            _attributes.put("line_color", _scene.pApplet().color(255,0,0));
            _attributes.put("v1_color", _scene.pApplet().color(0,0,255));
            _attributes.put("v2_color", _scene.pApplet().color(0,0,255));
            _attributes.put("v_color", _scene.pApplet().color(0,0,255));
        }
    }

    public static class UpdateStructure extends VisualStep {
        protected List<? extends Node> _structure;
        protected Vector[] _initialTranslations, _finalTranslations, _deltaPerFrameTranslations, _deltaTranslations;
        protected Quaternion[] _initialRotations, _finalRotations, _deltaPerFrameRotations, _deltaRotations;


        public UpdateStructure(Scene scene, List<? extends Node> structure, long period, long duration, long renderingDuration) {
            super(scene, period, duration, renderingDuration);
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

        protected void _calculateSpeedPerIteration() {
            float inc = ((float) _period) / (_duration - _duration % _period);
            //Calculate deltas per frame
            for(int i = 0; i < _structure.size(); i++){
                _deltaPerFrameRotations[i] = new Quaternion(_deltaRotations[i].axis(), _deltaRotations[i].angle() * inc);
                _deltaPerFrameTranslations[i] = Vector.multiply(_deltaTranslations[i], inc);
            }
        }


        @Override
        public void initialize() {
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
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil( 1.0* _duration / _period);
            //how many times to render?
            _totalRenderingTimes = (int) Math.ceil( 1.0* _renderingDuration / _period);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if(_completed && _times >= _totalRenderingTimes){
                _keepDrawing = false;
            } else if (_times >= _totalTimes) {
                //set rotation to fit exactly final rotation
                for(int i = 0; i < _structure.size(); i++){
                    _structure.get(i).setRotation(_finalRotations[i]);
                    _structure.get(i).setTranslation(_finalTranslations[i]);
                }
                _completed = true;
            }else{
                for(int i = 0; i < _structure.size(); i++) {
                    if (1 - Math.pow(Quaternion.dot(_structure.get(i).rotation(), _finalRotations[i]), 2) >= 0.000001) {
                        _structure.get(i).rotate(_deltaPerFrameRotations[i]);
                        _structure.get(i).translate(_deltaPerFrameTranslations[i]);
                    }
                }
            }
            _times++;
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

    //Some common rendering routines
    public static void drawSegment2D(PGraphics pg, Vector v1, Vector v2, float radius, int cline, int cv1, int cv2) {
        pg.pushStyle();
        pg.strokeWeight(radius / 2.f);
        pg.stroke(cline);
        pg.fill(cline);
        pg.line(v1.x(), v1.y(), v2.x(), v2.y());
        pg.strokeWeight(radius);
        pg.stroke(cv1);
        pg.fill(cv1);
        pg.ellipse(v1.x(), v1.y(), radius, radius);
        pg.stroke(cv2);
        pg.fill(cv2);
        pg.ellipse(v2.x(), v2.y(), radius, radius);
        pg.popStyle();
    }
}
