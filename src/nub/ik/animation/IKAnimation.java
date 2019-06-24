package nub.ik.animation;

import nub.core.Node;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.AnimatorObject;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IKAnimation {
    //TODO : Remove this class
    //TODO : CCD Animation must be fixed
    //TODO : Perform operations using Frames
    public static abstract class Step extends AnimatorObject{
        protected boolean _completed = false;
        protected Scene _scene;

        public Step(Scene scene){
            super(scene.timingHandler());
            _scene = scene;
            setPeriod(40);
            stop();
        }

        public void animate(){
            if(_completed){
                this.stop();
                return;
            }
            update();
        }

        public abstract void update();
        public abstract void draw();
    }
    //Define some Steps
    public static class FollowTrajectoryStep extends Step{
        protected enum Line{ V1_TO_V2, V_TO_V2, V1_TO_V}

        protected long _edgePeriod;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Vector[] _trajectory;
        protected Vector _current, _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected Line _mode = Line.V1_TO_V2;
        protected String _message = null;
        protected int _times, _totalTimes;


        public FollowTrajectoryStep(Scene scene, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.context();
            _cv1 = pg.color(0,0,255, 200);
            _cv2 = pg.color(0,0,255, 200);
            _cv = pg.color(0,0,255, 200);
            _cline = pg.color(255,0,0, 200);
            _radius = radius;
            _edgePeriod = edgePeriod;
        }

        public void setTrajectory(Vector... trajectory){
            _trajectory = trajectory;
            _idx = 0;
            _current = trajectory[_idx].get();
            _completed = false;
            _times = 0;
            calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_edgePeriod / period()) + 1;
            this.start();
        }

        protected void calculateSpeedPerIteration(){
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1];
            float inc = ((float) period()) / (_edgePeriod - _edgePeriod % period());
            _delta = Vector.subtract(v2,v1);
            _delta.multiply(inc);
        }

        @Override
        public void update() {
            if(_trajectory == null) return;
            Vector v2 = _trajectory[_idx + 1];
            if(_times++ >= _totalTimes){
                _completed = true;
                return;
            }

            if(Vector.distance(_current, v2) < 0.1){
                _idx++;
                if(_idx == _trajectory.length - 1){
                    _idx--;
                    return;
                }
                _current = v2;
                calculateSpeedPerIteration();
            } else {
                _current.add(_delta);
            }
        }

        @Override
        public void draw() {
            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];

            switch (_mode){
                case V1_TO_V:{
                    v2 = _current;
                    break;
                }
                case V_TO_V2:{
                    v1 = _current;
                    break;
                }
            }

            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if(_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                pg.fill(_cv2);
                pg.sphere(_radius);
                pg.popMatrix();
                pg.fill(_cline);
                if(Vector.distance(v1,v2) > 1.2f*_radius) _scene.drawArrow(v1,v2, _radius/4f);
            } else{
                drawSegment2D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
                pg.stroke(_cv);
                pg.fill(_cv);
                pg.ellipse(_current.x(), _current.y(),_radius, _radius);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            if(_message != null){
                _scene.beginHUD();
                pg.fill(255);
                pg.textSize(28);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, pg.width*0.05f, pg.height*0.8f, pg.width*0.9f, pg.height);
                _scene.endHUD();
            }

            pg.popStyle();
        }
    }


    public static class FollowArc extends Step{
        protected enum Arc{ V1_TO_V2, V_TO_V2, V1_TO_V, V}
        protected long _edgePeriod;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Vector[] _trajectory;
        protected Vector _base, _current;
        protected Quaternion _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected String _message = null;
        protected Arc _mode = Arc.V1_TO_V2;
        protected int _times, _totalTimes;

        public FollowArc(Scene scene, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.context();
            _cv1 = pg.color(0,0,255, 200);
            _cv2 = pg.color(0,0,255, 200);
            _cv = pg.color(0,0,255, 200);
            _cline = pg.color(0,0,255, 200);
            _radius = radius;
            _edgePeriod = edgePeriod;
        }

        public void setTrajectory(Vector... trajectory){
            _trajectory = trajectory;
            _idx = 0;
            _base = trajectory[_idx++];
            _current = trajectory[_idx].get();
            _completed = false;
            _times = 0;
            calculateSpeedPerIteration();
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_edgePeriod / period()) + 3;
            this.start();
        }

        protected void calculateSpeedPerIteration(){
            Quaternion q = new Quaternion(Vector.subtract(_trajectory[_idx], _base), Vector.subtract(_trajectory[_idx + 1], _base));
            float inc = ((float) period()) / (_edgePeriod - _edgePeriod % period());
            _delta = new Quaternion(q.axis(), q.angle() * inc);
        }

        @Override
        public void update() {
            if(_trajectory == null) return;
            if(_times++ >= _totalTimes){
                _completed = true;
                return;
            }

            Vector v2 = _trajectory[_idx + 1];

            if(Math.abs(Vector.angleBetween(Vector.subtract(_current, _base), Vector.subtract(v2, _base))) < 0.001){
                _idx++;
                if(_idx == _trajectory.length - 1){
                    _idx--;
                    return;
                }
                _current = v2;
                calculateSpeedPerIteration();
            } else {
                _current = _delta.rotate(Vector.subtract(_current, _base));
                _current.add(_base);
            }
        }


        @Override
        public void draw() {
            if(_trajectory == null) return;

            Vector v1 = _trajectory[_idx];
            Vector v2 = _trajectory[_idx + 1 < _trajectory.length ? _idx + 1 : _idx];

            switch (_mode){
                case V1_TO_V:{
                    v2 = _current;
                    break;
                }
                case V_TO_V2:{
                    v1 = _current;
                    break;
                }
                case V:{
                    v1 = v2 = _current;
                }
            }

            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if(_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                pg.fill(_cv2);
                pg.sphere(_radius);
                pg.popMatrix();
                pg.fill(_cline);
                _scene.drawArrow(_base,_current, _radius/4f);
                _scene.drawArrow(_base,v2, _radius/4f);
                _scene.drawArrow(_base,v1, _radius/4f);
                //drawSegment3D(pg, _base, v1, _radius, _cline, _cv1, _cv2);
                //drawSegment3D(pg, _base, v2, _radius, _cline, _cv1, _cv2);
                //drawSegment3D(pg, _base, _current, _radius, _cv, _cv, _cv);

            } else{
                drawSegment2D(pg, _base, v1, _radius, _cline, _cv1, _cv2);
                drawSegment2D(pg, _base, v2, _radius, _cline, _cv1, _cv2);
                drawSegment2D(pg, _base, _current, _radius, _cv, _cv, _cv);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            if(_message != null){
                _scene.beginHUD();
                pg.fill(255);
                pg.textSize(28);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, pg.width*0.05f, pg.height*0.8f, pg.width*0.9f, pg.height);
                _scene.endHUD();
            }
            pg.popStyle();
        }
    }


    public static class RotateNode extends Step{
        protected enum Rotate{ V1_TO_V2, V_TO_V2, V1_TO_V, V}
        protected long _edgePeriod;
        protected boolean _updateNode = true;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Node _node;
        protected Quaternion _deltaLocal, _deltaGlobal;
        protected Vector _initial, _end, _current, _base;
        protected int _cline, _cv1, _cv2, _cv;
        protected int _times, _totalTimes;
        protected String _message = null;
        protected Rotate _mode = Rotate.V1_TO_V2;

        public RotateNode(Scene scene, Node node, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.context();
            _cv1 = pg.color(0,0,255, 200);
            _cv2 = pg.color(0,0,255, 200);
            _cv = pg.color(0,0,255, 200);
            _cline = pg.color(0,0,255, 200);
            _edgePeriod = edgePeriod;
            _node = node;
        }

        public void setTrajectory(Quaternion initial, Quaternion end, Vector vector){
            _node.setRotation(initial.get());
            _initial = _node.worldLocation(vector).get();
            _end = _node.worldLocation(Quaternion.compose(initial.inverse(), end).rotate(vector)).get();
            _current = _initial.get();
            _base = _node.position().get();
            _completed = false;
            calculateSpeedPerIteration();
            _deltaGlobal = new Quaternion(Quaternion.compose(initial.inverse(), end).axis(), _deltaLocal.angle());

            _times = 0;
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_edgePeriod / period()) + 1;
            this.start();
        }

        protected void calculateSpeedPerIteration(){
            Quaternion q = new Quaternion(Vector.subtract(_initial, _base), Vector.subtract(_end, _base));
            float inc = ((float) period()) / (_edgePeriod - _edgePeriod % period());
            _deltaLocal = new Quaternion(q.axis(), q.angle() * inc);
        }

        @Override
        public void update() {
            if(_initial == null || _end == null) return;
            if(_times++ >= _totalTimes){
                _completed = true;
                return;
            }

            if(Math.abs(Vector.angleBetween(Vector.subtract(_current, _base), Vector.subtract(_end, _base))) < 0.001){
                return;
            } else {
                if(_updateNode){
                    _node.rotate(_deltaGlobal);
                }
                _current = _deltaLocal.rotate(Vector.subtract(_current, _base));
                _current.add(_base);
            }
        }

        @Override
        public void draw() {
            Vector v1 = _initial;
            Vector v2 = _end;

            switch (_mode){
                case V1_TO_V:{
                    v2 = _current;
                    break;
                }
                case V_TO_V2:{
                    v1 = _current;
                    break;
                }
                case V:{
                    v1 = v2 = _current;
                }
            }

            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if(_scene.is3D()) {
                drawSegment3D(pg, _base, v1, _radius, _cline, _cv1, _cv2);
                drawSegment3D(pg, _base, v2, _radius, _cline, _cv1, _cv2);
                drawSegment3D(pg, _base, _current, _radius, _cv, _cv, _cv);
            } else{
                drawSegment2D(pg, _base, v1, _radius, _cline, _cv1, _cv2);
                drawSegment2D(pg, _base, v2, _radius, _cline, _cv1, _cv2);
                drawSegment2D(pg, _base, _current, _radius, _cv, _cv, _cv);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            if(_message != null){
                _scene.beginHUD();
                pg.fill(255);
                pg.textSize(28);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, pg.width*0.05f, pg.height*0.8f, pg.width*0.9f, pg.height);
                _scene.endHUD();
            }
            pg.popStyle();
        }
    }

    public static class TranslateNode extends Step{
        protected enum Translate{ V1_TO_V2, V_TO_V2, V1_TO_V, V}
        protected long _edgePeriod;
        protected boolean _fixChildren = true;
        protected boolean _updateNode = true;
        protected float _radius;//_iterations per edge is updated according to Animator period
        protected Node _node;
        protected Vector _delta;
        protected Vector _initial, _end, _current;
        protected int _cline, _cv1, _cv2, _cv;
        protected int _times, _totalTimes;
        protected String _message = null;
        protected Translate _mode = Translate.V1_TO_V2;

        public TranslateNode(Scene scene, Node node, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.context();
            _cv1 = pg.color(0,0,255, 200);
            _cv2 = pg.color(0,0,255, 200);
            _cv = pg.color(0,0,255, 200);
            _cline = pg.color(0,0,255, 200);
            _edgePeriod = edgePeriod;
            _node = node;
        }

        public void fixPosition(Node node, Vector newPosition){
            Vector diff = Vector.subtract(newPosition, node.position());
            node.setPosition(newPosition.get());
            if(!_fixChildren) return;

            for(Node child : node.children()){
                child.setPosition(Vector.subtract(child.position(), diff));
            }
        }

        public void setTrajectory(Vector initial, Vector end){
            fixPosition(_node, initial.get());
            _initial = initial;
            _end = end;
            _current = initial.get();
            _completed = false;
            calculateSpeedPerIteration();
            _times = 0;
            //how many times to execute?
            _totalTimes = (int) Math.ceil(_edgePeriod / period()) + 1;
            this.start();
        }


        protected void calculateSpeedPerIteration(){
            float inc = ((float) period()) / (_edgePeriod - _edgePeriod % period());
            _delta = Vector.subtract(_end,_initial);
            _delta.multiply(inc);
        }

        @Override
        public void update() {
            if(_initial == null || _end == null) return;
            if(_times++ >= _totalTimes){
                _completed = true;
                return;
            }

            if(Vector.distance(_current, _end) >= 0.1) {
                _current.add(_delta);
                if (_updateNode) {
                    fixPosition(_node, _current);
                }
            }
        }


        @Override
        public void draw() {
            Vector v1 = _initial;
            Vector v2 = _end;

            switch (_mode){
                case V1_TO_V:{
                    v2 = _current;
                    break;
                }
                case V_TO_V2:{
                    v1 = _current;
                    break;
                }
                case V:{
                    v1 = v2 = _current;
                }
            }

            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if(_scene.is3D()) {
                pg.noStroke();
                pg.pushMatrix();
                pg.translate(v1.x(), v1.y(), v1.z());
                pg.fill(_cv1);
                pg.sphere(_radius);
                pg.popMatrix();

                pg.noStroke();
                pg.pushMatrix();
                pg.translate(v2.x(), v2.y(), v2.z());
                pg.fill(_cv2);
                pg.sphere(_radius);
                pg.popMatrix();

                pg.fill(_cline);
                if(Vector.distance(v1,v2) > 1.2f*_radius) _scene.drawArrow(v1,v2, _radius/4f);
            } else{
                drawSegment2D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
                pg.stroke(_cv);
                pg.fill(_cv);
                pg.ellipse(_current.x(), _current.y(),_radius, _radius);
            }
            pg.hint(PConstants.ENABLE_DEPTH_TEST);
            if(_message != null){
                _scene.beginHUD();
                pg.fill(255);
                pg.textSize(28);
                pg.textAlign(PConstants.CENTER);
                pg.text(_message, pg.width*0.05f, pg.height*0.8f, pg.width*0.9f, pg.height);
                _scene.endHUD();
            }
            pg.popStyle();
        }
    }



    public static class CCDAnimation {
        protected CCDSolver _solver; //Keep hist of _iterations using CCD
        protected Scene _scene; //where to display animation
        protected int _current = 1; //current iteration
        protected int _step = 0, _iteration = 0; //useful variables to check which Joint is being animated
        protected int _state, _initial;
        protected Step[] _trajectory = new Step[4];
        protected HashMap<Integer, Node> _structure; //copy of CCDSolver structure
        protected Node _target;

        protected float _radius;
        protected long _period = 3000;
        protected String _message;

        public void setPeriod(long period){
            _period = period;
        }

        public CCDAnimation(Scene scene, CCDSolver solver, float radius){
            this(scene, solver, radius, -1);
        }

        public CCDAnimation(Scene scene, CCDSolver solver, float radius, int color) {
            _scene = scene;
            _solver = solver;
            _radius = radius;
            //Make a copy of the CCD Structure
            _structure = _copy(scene, solver.chain(), true);
            for(Node n : _structure.values()){
                if(color != -1) ((Joint) n).setColor(color);
                ((Joint) n).setRadius(_radius);
            }
            //Center the scene on the root node
            scene.setCenter(_structure.get(_solver.chain().get(0).id()).position());
            scene.fit();
            //Make a copy of target
            PShape redBall = scene.pApplet().createShape(PConstants.SPHERE, _radius * 1.2f);
            redBall.setStroke(false);
            redBall.setFill(scene.pApplet().color(255,0,0, 150));
            _target = new Node(_scene, redBall);
            _target.setPosition(_solver.target().position().get());
            setInitialConditions();
        }

        public void reset(){
            _state = 0;
            _step = 0;
            setInitialConditions();
        }

        public void animate() {
            //animate each step
            animateStep();
        }

        public void setInitialConditions(){
            if(_solver.history()._states.size() < 1) return;
            //Set structure to be on initial conditions
            while(_state < _solver.history()._states.size() && _solver.history()._states.get(_state)._name == "initialization"){
                NodeState state = _solver.history()._states.get(_state++);
                Node copy = _structure.get(state._node.id());
                copy.setRotation(state.quaternion());
                copy.setTranslation(state.vector());
            }
            //clear all
            for(int i = 0; i < _trajectory.length; i++){
                _scene.unregisterAnimator(_trajectory[i]);
                _trajectory[i] = null;
            }
            _target.setPosition(_solver.target().position());
            _initial = _state;
        }

        protected boolean _done(){
            if(_trajectory[0] == null && _state == _initial){
                return true;
            }
            //Check previous trajectory
            if(_step - 1 >= 0) return _trajectory[_step - 1]._completed;
            return _trajectory[_trajectory.length - 1]._completed;
        }



        public void animateStep() {
            if (_done() && _state < _solver.history()._states.size()) {
                //create next trajectory
                switch (_step) {
                    case 0: {
                        //clear all
                        for(int i = 0; i < _trajectory.length; i++){
                            if(_trajectory[i] == null) continue;
                            _scene.unregisterAnimator(_trajectory[i]);
                            _trajectory[i] = null;
                        }

                        NodeState state = _solver.history()._states.get(_state);
                        Node node = _structure.get(state.node().id());
                        Node eff = _structure.get(_solver.chain().get(_solver.chain().size() -1).id());
                        _trajectory[0] = new FollowTrajectoryStep(_scene, _radius * 0.8f, _period);
                        _trajectory[0].start();
                        ((FollowTrajectoryStep) _trajectory[0]).setTrajectory(node.position(), eff.position());
                        ((FollowTrajectoryStep) _trajectory[0])._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory[0])._cline = _scene.context().color(255,255,0);
                        ((FollowTrajectoryStep) _trajectory[0])._cv2 = _scene.context().color(255);
                        _message = "Step 1: Find the segment line defined by Joint " + ((Joint) node).name() + " and End Effector " + ((Joint) eff).name() + ") ";
                        break;
                    }
                    case 1: {
                        //clear all
                        NodeState state = _solver.history()._states.get(_state);
                        Node node = _structure.get(state.node().id());
                        _trajectory[1] = new FollowTrajectoryStep(_scene, _radius * 0.8f, _period);
                        _trajectory[1].start();
                        //Opaque previous segment
                        ((FollowTrajectoryStep) _trajectory[0])._cline = _scene.context().color(255,255,0, 100);
                        ((FollowTrajectoryStep) _trajectory[0])._cv2 = _scene.context().color(255, 100);
                        ((FollowTrajectoryStep) _trajectory[1]).setTrajectory(node.position(), _solver.target().position());
                        ((FollowTrajectoryStep) _trajectory[1])._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory[1])._cline = _scene.context().color(0,255,0);
                        ((FollowTrajectoryStep) _trajectory[1])._cv2 = _scene.context().color(255);
                        _message = "Step 2: Find the segment Line defined by Joint " + ((Joint) node).name() + " and Target (T)";
                        break;
                    }
                    case 2: {
                        NodeState state = _solver.history()._states.get(_state);
                        Node node = _structure.get(state.node().id());

                        //Opaque previous segment
                        ((FollowTrajectoryStep) _trajectory[1])._cline = _scene.context().color(0,255,0, 100);
                        ((FollowTrajectoryStep) _trajectory[1])._cv2 = _scene.context().color(255, 100);

                        _trajectory[3] = new RotateNode(_scene, node,_radius * 0.8f, _period);
                        Vector v = node.children().get(0).translation();
                        ((RotateNode) _trajectory[3]).setTrajectory(node.rotation(), state.quaternion(), v);
                        _trajectory[3].start();
                        ((RotateNode) _trajectory[3])._mode = RotateNode.Rotate.V;
                        ((RotateNode) _trajectory[3])._cv = _scene.context().color(255);
                        ((RotateNode) _trajectory[3])._cv1 = _scene.context().color(255);
                        ((RotateNode) _trajectory[3])._cv2 = _scene.context().color(255);
                        ((RotateNode) _trajectory[3])._cline = _scene.context().color(255);

                        //((RotateNode) _trajectory[2])._message = "Step 2: Rotate J_i to reach the previous segment line";

                        //Visualize line movement
                        _trajectory[2] = new FollowArc(_scene, _radius * 0.8f, _period);
                        _trajectory[2].start();
                        ((FollowArc) _trajectory[2])._mode = FollowArc.Arc.V;
                        ((FollowArc) _trajectory[2])._cline = _scene.context().color(255,255,0);
                        ((FollowArc) _trajectory[2])._cv2 = _scene.context().color(255);


                        Node eff =  _structure.get(_solver.chain().get(_solver.chain().size() -1).id());
                        Quaternion d = Quaternion.compose(node.rotation().inverse(), state.quaternion());
                        Vector end = d.rotate(node.location(eff.position()));
                        ((FollowArc) _trajectory[2]).setTrajectory(node.position().get(), eff.position().get(), node.worldLocation(end));
                        _message = "Step 3: Rotate Joint " + ((Joint) node).name() + " to reduce the distance from End Effector (" + ((Joint) eff).name() + ") " + "to Target (T)";

                        //update state
                        _state += 1;
                        break;
                    }
                    case 3: {
                        /*
                        Vector unconstrained = ((FollowArc)_trajectory)._current;
                        _previousSegment = Arrays.copyOfRange(((FollowArc) _trajectory)._trajectory, 1, ((FollowArc) _trajectory)._trajectory.length);
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowArc(_scene, _radius, _period);
                        ((FollowArc) _trajectory)._mode = FollowArc.Arc.V1_TO_V;
                        _trajectory.start();
                        ((FollowArc) _trajectory).setTrajectory(j_i, unconstrained, j_i1_hat);
                        ((FollowArc) _trajectory)._message = "Step 3: In case of constraints Move to a Feasible position";
                         */
                    }
                }
                _step = (_step + 1) % 3;
            }
        }

        public void draw() {
            if(_solver.history()._totalIterations < 2) return;
            animate();
            for(Step step : _trajectory){
                if(step != null) step.draw();
            }
            _scene.context().noLights();
            _scene.beginHUD();
            _scene.context().fill(255);
            _scene.context().textSize(28);
            _scene.context().textAlign(PConstants.CENTER);
            _scene.context().text("CCD Solver", _scene.context().width*0.05f, _scene.context().height*0.05f, _scene.context().width*0.9f, _scene.context().height);
            _scene.context().text(_message, _scene.context().width*0.05f, _scene.context().height*0.8f, _scene.context().width*0.9f, _scene.context().height);

            _scene.context().textSize(14);
            _scene.context().textAlign(PConstants.CENTER, PConstants.CENTER);
            //draw node names
            for(Node node : _structure.values()){
                String name = ((Joint) node).name();
                Vector scr = _scene.screenLocation(node.position());
                _scene.context().text(name, scr.x() - 15, scr.y());
            }
            Vector scr = _scene.screenLocation(_target.position());
            _scene.context().text("T", scr.x() + 15, scr.y());

            _scene.endHUD();
            _scene.context().lights();
        }
    }


    public static class FABRIKAnimation {
        //Keep hist of _iterations using FABRIK
        protected ChainSolver _solver;
        protected Scene _scene; //where to display animation
        protected int _current = 1; //current iteration
        protected int _step = 0; //useful variables to check which Joint is being animated
        protected int _state, _initial;
        protected List<Step> _steps;
        protected HashMap<Integer, Node> _structure; //copy of CCDSolver structure
        protected HashMap<Integer, Node> _previousStructure; //copy of CCDSolver structure

        protected Node _target;
        protected Vector _initialPosition;

        protected float _radius;
        protected long _period = 3000;
        protected String _message;

        public void setPeriod(long period){
            _period = period;
        }

        public FABRIKAnimation(Scene scene, ChainSolver solver, float radius) {
            this(scene, solver, radius, -1);
        }

        public FABRIKAnimation(Scene scene, ChainSolver solver, float radius, int color) {
            _scene = scene;
            _solver = solver;
            _radius = radius;

            //Make a copy of previous iteration
            _previousStructure = _copy(scene, solver.internalChain(), true);

            //Make a copy of the CCD Structure
            _structure = _copy(scene, solver.internalChain(), true);

            for(Node n : _structure.values()){
                if(color != -1) ((Joint) n).setColor(color);
                ((Joint) n).setRadius(_radius);
                n.setConstraint(null);
            }


            for(Node n : _previousStructure.values()){
                int c = _scene.pApplet().color(200, 200);
                if(color != -1) ((Joint) n).setColor(c); //opaque
                ((Joint) n).setRadius(_radius * 0.7f);
                n.setConstraint(null);
            }


            //Center the scene on the root node
            scene.setCenter(_structure.get(_solver.internalChain().get(0).id()).position());
            scene.fit();
            //Make a copy of target
            PShape redBall = scene.pApplet().createShape(PConstants.SPHERE, _radius * 1.2f);
            redBall.setStroke(false);
            redBall.setFill(scene.pApplet().color(255,0,0, 150));
            _target = new Node(_scene, redBall);
            _target.setPosition(_solver.target().position().get());
            setInitialConditions();
        }

        public void setInitialConditions(){
            if(_solver.history()._states.size() < 1) return;
            //Set structure to be on initial conditions
            while(_state < _solver.history()._states.size() && _solver.history()._states.get(_state)._name == "initialization"){
                NodeState state = _solver.history()._states.get(_state++);
                Node copy = _structure.get(state._node.id());
                copy.setRotation(state.quaternion());
                copy.setTranslation(state.vector());
            }
            _initialPosition = _structure.get(_solver.internalChain().get(0).id()).position().get();
            _target.setPosition(_solver.target().position());
            _initial = _state;
            if(_steps == null){
                _steps = new ArrayList<>();
            } else {
                //clear all
                for(Step s : _steps){
                    _scene.unregisterAnimator(s);
                }
                _steps.clear();
            }
        }

        public void reset(){
            _state = 0;
            setInitialConditions();
        }



        public void animate() {
            //animate each step
            animateStep();
        }

        protected void _updatePrevious(){
            for(Integer i : _structure.keySet()){
                _previousStructure.get(i).setTranslation(_structure.get(i).translation().get());
                _previousStructure.get(i).setRotation(_structure.get(i).rotation().get());
            }
        }



        protected boolean _done(){
            if(_steps.isEmpty() && _state == _initial){
                return true;
            }
            return _steps.get(_steps.size() - 1)._completed;
        }

        public void animateStep(){
            if (_done() && _state < _solver.history()._states.size()) {
                NodeState state = _solver.history()._states.get(_state);
                switch (state._name){
                    case "Effector to Target step":{
                        _updatePrevious();
                        animateEffToTarget();
                        break;
                    }
                    case "Backward step":{

                    }

                    case "Forward step":{
                        animateForwardStep();
                        break;
                    }
                    case "Head to Initial":{
                        animateHeadToInitial();
                        break;
                    }
                }
            }
        }

        public void animateEffToTarget(){
            //clear all
            for(Step s : _steps){
                _scene.unregisterAnimator(s);
            }
            _steps.clear();

            NodeState state = _solver.history()._states.get(_state);
            Node node = _structure.get(state.node().id());
            TranslateNode s = new TranslateNode(_scene, node, _radius * 0.8f, _period);
            s.start();
            s.setTrajectory(node.position(), state.vector());
            s._fixChildren = true;
            s._mode = TranslateNode.Translate.V1_TO_V;
            s._cline = _scene.context().color(255,0,0);
            s._cv1 = s._cv2 = _scene.context().color(255);
            _message = "Assume that End Effector (" + ((Joint) node).name() + ") reaches the Target (T)";
            _steps.add(s);
            _state += 1;
        }

        public void animateForwardStep(){
            //clear all
            for(Step s : _steps){
                _scene.unregisterAnimator(s);
            }
            _steps.clear();
            NodeState state = _solver.history()._states.get(_state);
            Node node = _structure.get(state.node().id());
            TranslateNode s = new TranslateNode(_scene, node,_radius * 0.8f, _period);
            s.start();
            s.setTrajectory(node.position(), state.vector());
            s._fixChildren = true;
            s._mode = TranslateNode.Translate.V1_TO_V;
            s._cline = _scene.context().color(255,255,0);
            s._cv1 = s._cv2 = _scene.context().color(255);
            _message = "Find the desired position of Joint " + ((Joint) node).name() + " by fixing the length of the bone";
            _steps.add(s);
            _state += 1;
        }

        public void animateHeadToInitial(){
            //clear all
            for(Step s : _steps){
                _scene.unregisterAnimator(s);
            }
            _steps.clear();

            NodeState state = _solver.history()._states.get(_state);
            Node node = _structure.get(state.node().id());
            TranslateNode s = new TranslateNode(_scene, node, _radius * 0.8f, _period);
            s.start();
            s.setTrajectory(node.position(), state.vector());
            s._fixChildren = true;
            s._mode = TranslateNode.Translate.V1_TO_V;
            s._cline = _scene.context().color(255,0,0);
            s._cv1 = s._cv2 = _scene.context().color(255);
            _message = "Assume that Joint " + ((Joint) node).name() + " reaches the root position (R)";
            _steps.add(s);
            _state += 1;
        }

        public void draw() {
            if (_solver.history()._totalIterations < 2) return;
            animate();
            //draw initial position
            _scene.context().pushStyle();
            _scene.context().noStroke();
            _scene.context().fill(0,255,0, 220);
            _scene.context().push();
            _scene.context().translate(_initialPosition.x(), _initialPosition.y(), _initialPosition.z());
            _scene.context().sphere(_radius * 1.2f);
            _scene.context().pop();
            _scene.context().popStyle();

            for (Step step : _steps) {
                if (step != null) step.draw();
            }

            _scene.context().noLights();
            _scene.beginHUD();
            _scene.context().fill(255);
            _scene.context().textSize(28);
            _scene.context().textAlign(PConstants.CENTER);
            _scene.context().text("FABRIK Solver", _scene.context().width * 0.05f, _scene.context().height * 0.05f, _scene.context().width * 0.9f, _scene.context().height);
            _scene.context().text(_message, _scene.context().width * 0.05f, _scene.context().height * 0.8f, _scene.context().width * 0.9f, _scene.context().height);

            _scene.context().textSize(14);
            _scene.context().textAlign(PConstants.CENTER, PConstants.CENTER);
            //draw node names
            for (Node node : _structure.values()) {
                String name = ((Joint) node).name();
                Vector scr = _scene.screenLocation(node.position());
                _scene.context().text(name, scr.x() - 15, scr.y());
            }
            Vector scr = _scene.screenLocation(_target.position());
            _scene.context().text("T", scr.x() + 15, scr.y());

            scr = _scene.screenLocation(_initialPosition);
            _scene.context().text("R", scr.x() + 15, scr.y());

            _scene.endHUD();
            _scene.context().lights();
        }
    }



    public static class NodeState{
        protected String _name;
        protected int _step, _iteration;
        protected Node _reference;
        protected Node _node;
        protected Vector _vector;
        protected Quaternion _quaternion;
        public NodeState(String name, Node node, Node reference, Vector vector, Quaternion quaternion, int step, int iteration){
            _name = name;
            _node = node;
            _reference = reference;
            _vector = vector;
            _quaternion = quaternion;
            _step = step;
            _iteration = iteration;
        }

        public Vector vector(){
            return _vector;
        }

        public Quaternion quaternion(){
            return _quaternion;
        }

        public Node node(){
            return _node;
        }

        public Node reference(){
            return _reference;
        }

        public int step(){
            return _step;
        }
    }

    public static class NodeStates{
        protected int _totalSteps;
        protected int _totalIterations;
        protected List<NodeState> _states;

        public NodeStates(){
            _totalSteps = 0;
            _totalIterations = 0;
            _states = new ArrayList<NodeState>();
        }

        public boolean addNodeState(String name, Node node, Node reference, Vector vector, Quaternion quaternion){
            return _states.add(new NodeState(name, node, reference, vector, quaternion, _totalSteps, _totalIterations));
        }

        public void incrementStep(){
            _totalSteps++;
        }

        public void incrementIteration(){
            _totalIterations++;
        }

        public void clear(){
            _totalSteps = 0;
            _totalIterations = 0;
            _states.clear();
        }
    }


    public static void drawSegment3D(PGraphics pg, Vector v1, Vector v2, float radius, int cline, int cv1, int cv2){
        pg.pushStyle();
        pg.strokeWeight(radius/2.f);
        pg.stroke(cline);
        pg.fill(cline);
        pg.line(v1.x(), v1.y(), v1.z(), v2.x(), v2.y(), v2.z());
        pg.strokeWeight(radius);
        pg.pushMatrix();
        pg.noStroke();
        pg.fill(cv1);
        pg.translate(v1.x(), v1.y(), v1.z());
        pg.sphere(radius);
        pg.popMatrix();
        pg.pushMatrix();
        pg.fill(cv2);
        pg.translate(v2.x(), v2.y(), v2.z());
        pg.sphere(radius);
        pg.popMatrix();
        pg.popStyle();
    }


    public static void drawSegment2D(PGraphics pg, Vector v1, Vector v2, float radius, int cline, int cv1, int cv2){
        pg.pushStyle();
        pg.strokeWeight(radius/2.f);
        pg.stroke(cline);
        pg.fill(cline);
        pg.line(v1.x(), v1.y(), v2.x(), v2.y());
        pg.strokeWeight(radius);
        pg.stroke(cv1);
        pg.fill(cv1);
        pg.ellipse(v1.x(), v1.y(),radius, radius);
        pg.stroke(cv2);
        pg.fill(cv2);
        pg.ellipse(v2.x(), v2.y(),radius, radius);
        pg.popStyle();
    }

    protected static HashMap<Integer, Node> _copy(Scene scene, List<? extends Node> chain, boolean asJoint) {
        HashMap<Integer, Node> copy = new HashMap<Integer, Node>();
        int idx = 0;
        for (Node node : chain) {
            //if reference is not contained clone it
            Node newNode;
            if(node.reference() == null || !copy.containsKey(node.reference().id())){
                newNode = _copyNode(scene, node, null, asJoint);
            } else {
                Node reference = copy.get(node.reference().id());
                newNode = _copyNode(scene, node, reference, asJoint);
            }

            if(asJoint){
                ((Joint) newNode).setName("" + idx++);
            }
            copy.put(node.id(), newNode);
        }

        if(asJoint){
            ((Joint)copy.get(chain.get(0).id())).setRoot(true);
        }

        return copy;
    }

    protected static Node _copyNode(Scene scene, Node node, Node reference, boolean asJoint){
        Node newNode;
        if(asJoint == false) {
            newNode = scene != null ? new Node(scene) : new Node();
        } else{
            newNode = new Joint(scene);
            if(node instanceof Joint){
                ((Joint) newNode).setColor(((Joint) node).color());
                ((Joint) newNode).setRadius(((Joint) node).radius());
            }
        }
        newNode.setReference(reference);
        newNode.setPosition(node.position().get());
        newNode.setOrientation(node.orientation().get());
        newNode.setConstraint(node.constraint());
        return newNode;
    }
}
