package nub.ik.animation;

import nub.core.Node;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SolverAnimation {
    public class StepGroup{
        boolean _init = false;
        protected List<Step> _steps = new ArrayList<Step>();
        public void add(Step step){
            _steps.add(step);
        }
        public boolean completed(){
            for(Step s : _steps){
                if(!s.completed()) return false;
            }
            return true;
        }

        public void init(){
            for(Step s : _steps){
                s.init();
            }
            _init = true;
        }

        public void execute(){
            for(Step s : _steps){
                s.execute();
            }
        }
    }

    public interface Step {
        void init();
        void reverse();
        void execute();
        void draw();
        boolean completed();
        void stopDrawing(boolean stop);
        boolean stopDrawing();
    }


    public void addStep(Step step){
        if(_sequential || _steps.isEmpty()) addStepGroup(step);
        else addNonSequentialStep(step);
    }

    public void addStepGroup(Step step){
        StepGroup group = new StepGroup();
        group.add(step);
        _steps.add(group);
    }

    public void addNonSequentialStep(Step step){
        _steps.get(_steps.size()-1).add(step);
    }


    public void enableSequential(boolean sequential){
        _sequential = sequential;
    }

    protected boolean _sequential;
    protected int _current = 0;
    protected long _period, _stepDuration;
    protected float _radius;
    protected List<StepGroup> _steps; //Execution flow
    protected List<Step> _drawingQueue; //Steps that must be drawn (even if they have no more updates)
    protected Scene _scene;


    protected HashMap<String, Joint> _structures;
    protected HashMap<Node, Joint> _nodeToJoint;

    public SolverAnimation(Scene scene, float radius, long period, long stepDuration){
        _scene = scene;
        _radius = radius;
        _period = period;
        _stepDuration = stepDuration;
        _steps = new ArrayList<StepGroup>();
        _drawingQueue = new ArrayList<Step>();
        _structures = new HashMap<String, Joint>();
        _nodeToJoint = new HashMap<Node, Joint>();
    }


    public void draw(){
        for(int i = _drawingQueue.size() - 1; i >= 0; i--){
            if(_drawingQueue.get(i).stopDrawing()) {
                _drawingQueue.remove(i);
            }else{
                _drawingQueue.get(i).draw();
            }
        }
    }

    public void executeGroup(){
        StepGroup group = _steps.get(_current);

        if(group._init == false){
            group.init();
            //add group steps to draw
            _drawingQueue.addAll(group._steps);
        }
        else if(!group.completed()) group.execute();
        else _current++;
    }

    public void reset(){
        _current = 0;
    }




    //TODO: ADD "SAME" OPTION i.e animate the same structure instead of create a new one
    //Register the structures to Animate
    public void registerStructure(String name, Node root){
        Joint joint = _generateBranch(root);
        _structures.put(name, joint);
    }

    public void registerStructure(String name, List<? extends Node> structure){
        Joint joint = _generateChain(structure);
        _structures.put(name, joint);
    }

    //TODO: Move this constructor
    protected Joint _generateBranch(Node root){
        Joint joint = new Joint(_scene);
        joint.setRoot(true);
        joint.setPosition(root.position().get());
        joint.setOrientation(root.orientation().get());
        joint.setConstraint(root.constraint());
        _nodeToJoint.put(root, joint);
        _addBranch(joint, root);
        return joint;
    }

    protected void _addBranch(Joint reference, Node node){
        //here we assume that node is attached to a graph
        for(Node child : node.children()){
            Joint joint = new Joint(_scene);
            child.setReference(reference);
            child.setTranslation(node.translation().get());
            child.setRotation(node.rotation().get());
            child.setConstraint(node.constraint());
            _nodeToJoint.put(child, joint);
            _addBranch(joint, child);
        }
    }

    protected Joint _generateChain(List<?  extends Node> chain){
        Node prev = chain.get(0);
        Joint root = new Joint(_scene, _radius);
        root.setPosition(prev.position().get());
        root.setOrientation(prev.orientation().get());
        root.setRoot(true);
        _nodeToJoint.put(prev, root);
        for(int i = 1; i < chain.size(); i++){
            Node current = chain.get(i);
            Joint joint = new Joint(_scene, _radius);
            Joint ref = _nodeToJoint.get(prev);
            joint.setReference(ref);
            joint.setPosition(current.position().get());
            joint.setOrientation(current.orientation().get());
            joint.setConstraint(current.constraint());
            _nodeToJoint.put(current, joint);
            prev = current;
        }
        return root;
    }


    //THESE METHODS MUST BE CALLED BY A CLIENT TO STACK THE STEP AND PROCESSS IT LATER
    //Clear last n steps
    public void clearStep(int num){
        Step[] steps = new Step[num];
        int c = 0;
        for(int sg = _steps.size()-1; sg >= 0; sg--){
            if(c == num) break;
            for(int st = _steps.get(sg)._steps.size()-1; st >=0; st--){
                if(c == num) break;
                steps[c] = _steps.get(sg)._steps.get(st);
                c++;
            }
        }
        ClearStep clearStep = new ClearStep(steps);
        addStep(clearStep);
    }

    public void addTrajectory(Node node, FollowTrajectoryStep.Line mode, Vector... vectors){
        FollowTrajectoryStep followStep = new FollowTrajectoryStep(_scene, _radius, _period, _stepDuration);
        followStep._mode = mode;
        followStep.setTrajectory(_nodeToJoint.get(node), vectors);
        addStep(followStep);
    }

    public void addTrajectory(Node node, Vector... vectors){
        addTrajectory(node, FollowTrajectoryStep.Line.V1_TO_V, vectors);
    }


    public void addRotateNode(Node node, Quaternion delta){
        RotateNode rotateStep = new RotateNode(_scene, _nodeToJoint.get(node), _radius, _period, _stepDuration);
        rotateStep.setRotation(delta);
        addStep(rotateStep);
    }

    public void addMessage(String message, float size, Vector start, Vector end){
        MessageStep messageStep = new MessageStep(_scene, message, _period, _stepDuration);
        messageStep.setLocation(start,end);
        messageStep.setSize(size);
        addStep(messageStep);
    }

    public void addMessage(String message, float size){
        MessageStep messageStep = new MessageStep(_scene, message, _period, _stepDuration);
        messageStep.setSize(size);
        addStep(messageStep);
    }


    //STEPS IMPLEMENTATION
    //Some important Steps
    public class ClearStep implements Step {
        List<Step> _steps;
        boolean _completed;

        public ClearStep(Step... steps){
            init();
            for(Step s : steps) _steps.add(s);
        }

        public void addStep(Step s){
            _steps.add(s);
            _completed = false;
        }

        @Override
        public void init() {
            _completed = false;
            _steps = _steps == null ? new ArrayList<Step>() : _steps;
        }

        @Override
        public void reverse(){
            for(Step s : _steps){
                s.init();
            }
            _completed = false;
        }


        @Override
        public void execute() {
            for(Step s : _steps){
                s.stopDrawing(true);
            }
            _completed = true;
        }

        @Override
        public void draw() {
            //Do nothing...
        }

        @Override
        public boolean completed() {
            return _completed;
        }

        @Override
        public void stopDrawing(boolean stop) {
            //do nothing
        }

        @Override
        public boolean stopDrawing() {
            return true;
        }
    }

    //Define some Steps
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

    public static class MessageStep implements Step{
        protected enum Location {TOP, BOTTOM, CUSTOM};
        protected boolean _completed, _stopDrawing;
        protected long _period, _duration;
        protected float _textSize = 28;
        protected int _color, _times, _totalTimes;
        protected String _message;
        protected Vector _start_location, _end_location;
        protected Location _mode = Location.BOTTOM;
        protected Scene _scene;

        public MessageStep(Scene scene, String message, long period, long duration) {
            _scene = scene;
            _color = _scene.pApplet().color(255);
            _message = message;
            _period = period;
            _duration = duration;
            init();
        }

        public void setLocation(Location location){
            _mode = location;
            switch (_mode){
                case TOP:{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 1.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  3.f * _scene.height() / 8.f);
                    break;
                }
                case BOTTOM:{
                    _start_location = new Vector(1.f *_scene.width() / 16.f, 6.f * _scene.height() / 8.f);
                    _end_location = new Vector(14.f *_scene.width() / 16.f,  _scene.height());
                    break;
                }
            }
        }

        public void setLocation(Vector start, Vector end){
            _mode = Location.CUSTOM;
            _start_location = start; //in terms of screen
            _end_location = end; //in terms of screen
            init();
        }

        public void setSize(float size){
            _textSize = size;
        }

        @Override
        public void init() {
            _completed = false;
            _times = 0;
            _totalTimes = (int) Math.ceil(_duration / _period) + 1;
            setLocation(_mode);
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if (_times++ >= _totalTimes) {
                //set rotation to fit exactly final rotation
                _completed = true;
                return;
            }
        }

        @Override
        public void draw() {
            PGraphics pg = _scene.context();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            pg.pushStyle();
            if (_message != null) {
                _scene.beginHUD();
                pg.stroke(_color);
                pg.fill(_color);
                pg.textSize(_textSize);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, _start_location.x(),_start_location.y(), _end_location.x(), _end_location.y());
                _scene.endHUD();
            }
            pg.popStyle();
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
        }

        @Override
        public boolean completed() {
            return _completed;
        }

        @Override
        public void stopDrawing(boolean stop) {
            _stopDrawing = stop;
        }

        @Override
        public boolean stopDrawing() {
            return _stopDrawing;
        }
    }


    public static class FollowTrajectoryStep implements Step {
        protected enum Line {V1_TO_V2, V_TO_V2, V1_TO_V}
        protected boolean _completed, _stopDrawing;
        protected long _edgePeriod, _period;
        protected Node _reference;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Vector[] _trajectory;
        protected Vector _current, _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected Line _mode = Line.V1_TO_V2;
        protected int _times, _totalTimes;
        protected Scene _scene;

        public FollowTrajectoryStep(Scene scene, float radius, long period, long edgePeriod) {
            _scene = scene;
            _radius = radius;
            //use some default colors
            _cv1 = _scene.pApplet().color(0, 0, 255, 200);
            _cv2 = _scene.pApplet().color(0, 0, 255, 200);
            _cv = _scene.pApplet().color(0, 0, 255, 200);
            _cline = _scene.pApplet().color(255, 0, 0, 200);
            _radius = radius;
            _period = period;
            _edgePeriod = edgePeriod;
        }

        public void setTrajectory(Node node, Vector... trajectory) {
            _reference = node;
            _trajectory = trajectory;
            init();
        }

        protected void _calculateSpeedPerIteration() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1];
            float inc = ((float) _period) / (_edgePeriod - _edgePeriod % _period);
            _delta = Vector.subtract(v2, v1);
            _delta.multiply(inc);
        }

        @Override
        public void init() {
            _idx = 0;
            _current = _trajectory[_idx].get();
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_edgePeriod / _period) + 1;
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if (_trajectory == null) return;
            Vector v2 = _trajectory[_idx + 1];
            if (_times++ >= _totalTimes) {
                _completed = true;
                return;
            }
            if (Vector.distance(_current, v2) < 0.1) {
                _idx++;
                if (_idx == _trajectory.length - 1) {
                    _idx--;
                    return;
                }
                _current = v2;
                _calculateSpeedPerIteration();
            } else {
                _current.add(_delta);
            }
        }

        @Override
        public void draw() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];
            switch (_mode) {
                case V1_TO_V: {
                    v2 = _current;
                    break;
                }
                case V_TO_V2: {
                    v1 = _current;
                    break;
                }
            }
            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.pushMatrix();
            if(_reference != null) _scene.applyWorldTransformation(_reference);
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if (_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                pg.fill(_cv2);
                pg.sphere(_radius);
                pg.popMatrix();
                pg.fill(_cline);
                if (Vector.distance(v1, v2) > 1.2f * _radius) _scene.drawArrow(v1, v2, _radius / 4f);
            } else {
                drawSegment2D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
                pg.stroke(_cv);
                pg.fill(_cv);
                pg.ellipse(_current.x(), _current.y(), _radius, _radius);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            pg.popMatrix();
            pg.popStyle();
        }

        @Override
        public boolean completed() {
            return _completed;
        }

        @Override
        public void stopDrawing(boolean stop) {
            _stopDrawing = stop;
        }

        @Override
        public boolean stopDrawing() {
            return _stopDrawing;
        }
    }


    public static class RotateNode implements Step {
        protected enum Rotate {HIGHLIGHT, NONE}
        protected long _duration, _period;
        protected boolean _completed, _stopDrawing;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Node _node;
        protected Quaternion _initial, _final, _deltaPerFrame, _delta;
        protected int _c;
        protected int _times, _totalTimes;
        protected Scene _scene;
        protected Rotate _mode = Rotate.HIGHLIGHT;

        public RotateNode(Scene scene, Node node, float radius, long period, long duration) {
            _scene = scene;
            //use some default colors
            _c = _scene.pApplet().color(0, 0, 255, 200);
            _radius = radius;
            _period = period;
            _duration = duration;
            _node = node;
        }

        public void setRotation(Quaternion delta) {
            _completed = false;
            _delta = delta;
            init();
        }

        protected void _calculateSpeedPerIteration() {
            float inc = ((float) _period) / (_duration - _duration % _period);
            _deltaPerFrame = new Quaternion(_delta.axis(), _delta.angle() * inc);
        }

        @Override
        public void init() {
            _initial = _node.rotation().get();
            _delta = _node.constraint() != null ? _node.constraint().constrainRotation(_delta, _node) : _delta;
            _final = Quaternion.compose(_initial, _delta);
            _completed = false;
            _times = 0;
            _calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_duration / _period) + 1;
        }

        @Override
        public void reverse() {

        }

        @Override
        public void execute() {
            if (_times++ >= _totalTimes) {
                //set rotation to fit exactly final rotation
                _node.setRotation(_final.get());
                _completed = true;
                return;
            }
            if (1 - Math.pow(Quaternion.dot(_node.rotation(), _final), 2) < 0.000001) {
                return;
            } else {
                _node.rotate(_deltaPerFrame);
            }
        }

        @Override
        public void draw() {
            if(_mode == Rotate.NONE) return;
            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            pg.pushMatrix();
            _scene.applyWorldTransformation(_node);
            pg.noStroke();
            pg.fill(_c);
            pg.sphere(_radius);
            pg.popMatrix();
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            pg.popStyle();
        }

        @Override
        public boolean completed() {
            return _completed;
        }

        @Override
        public void stopDrawing(boolean stop) {
            _stopDrawing = stop;
        }

        @Override
        public boolean stopDrawing() {
            return _stopDrawing;
        }
    }
}
