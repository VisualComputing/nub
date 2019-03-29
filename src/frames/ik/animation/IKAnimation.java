package frames.ik.animation;

import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.AnimatorObject;
import processing.core.PConstants;
import processing.core.PGraphics;
import java.util.ArrayList;
import java.util.Arrays;

public class IKAnimation {
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
            checkCompleted();
            if(_completed) return;
            update();
        }

        public abstract void update();
        public abstract void draw();
        public abstract void checkCompleted();
    }
    //Define some Steps
    public static class FollowTrajectoryStep extends Step{
        protected enum Line{ V1_TO_V2, V_TO_V2, V1_TO_V}

        protected long _edgePeriod;
        protected float _radius;//iterations per edge is updated according to Animator period
        protected Vector[] _trajectory;
        protected Vector _current, _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected Line _mode = Line.V1_TO_V2;
        protected String _message = null;

        public FollowTrajectoryStep(Scene scene, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.frontBuffer();
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
            _current = trajectory[_idx].get();
            _completed = false;
            calculateSpeedPerIteration();
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
            if(Vector.distance(_current, v2) < 0.1){
                _idx++;
                if(_idx == _trajectory.length - 1) return;
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

            PGraphics pg = _scene.frontBuffer();
            pg.pushStyle();
            pg.hint(PConstants.DISABLE_DEPTH_TEST);
            if(_scene.is3D()) {
                drawSegment3D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
                pg.noStroke();
                pg.fill(_cv);
                pg.pushMatrix();
                pg.translate(_current.x(), _current.y(), _current.z());
                pg.sphere(_radius);
                pg.popMatrix();
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

        @Override
        public void checkCompleted() {
            if(_trajectory == null){
                _completed = false;
            } else {
                _completed = _idx == _trajectory.length - 1;
            }
        }
    }


    public static class FollowArc extends Step{
        protected enum Arc{ V1_TO_V2, V_TO_V2, V1_TO_V}
        protected long _edgePeriod;
        protected float _radius;//iterations per edge is updated according to Animator period
        protected Vector[] _trajectory;
        protected Vector _base, _current;
        protected Quaternion _delta;
        protected int _idx = 0;
        protected int _cline, _cv1, _cv2, _cv;
        protected String _message = null;
        protected Arc _mode = Arc.V1_TO_V2;

        public FollowArc(Scene scene, float radius, long edgePeriod){
            super(scene);
            _radius = radius;
            //use some default colors
            PGraphics pg = _scene.frontBuffer();
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
            calculateSpeedPerIteration();
            this.start();
        }

        protected void calculateSpeedPerIteration(){
            Quaternion q = new Quaternion(Vector.subtract(_trajectory[_idx], _base), Vector.subtract(_trajectory[_idx + 1], _base));
            float inc = ((float) period()) / (_edgePeriod - _edgePeriod % period());
            _delta = new Quaternion(q.axis(), q.angle() * inc);
        }

        float min = 9999;
        @Override
        public void update() {
            if(_trajectory == null) return;
            Vector v2 = _trajectory[_idx + 1];
            if(Math.abs(Vector.angleBetween(Vector.subtract(_current, _base), Vector.subtract(v2, _base))) < 0.005){
                _idx++;
                if(_idx == _trajectory.length - 1) return;
                _current = v2;
                calculateSpeedPerIteration();
            } else {
                _current = _delta.rotate(Vector.subtract(_current, _base));
                _current.add(_base);
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

            PGraphics pg = _scene.frontBuffer();
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

        @Override
        public void checkCompleted() {
            if(_trajectory == null){
                _completed = false;
            } else {
                _completed = _idx == _trajectory.length - 1;
            }
        }
    }

    public static class FABRIKAnimation {
        //Keep hist of iterations using FABRIK
        protected ChainSolver _solver;
        protected Scene _scene; //where to display animation
        protected int _current = 1; //current iteration
        protected int _step = 0, _j = 0; //useful variables to check which Joint is being animated
        protected Step _trajectory;
        protected Vector[] _previousSegment;
        protected float _radius;
        protected boolean _forwardStage = true;
        protected long _period = 8000;


        public FABRIKAnimation(Scene scene, ChainSolver solver, float radius) {
            _scene = scene;
            _solver = solver;
            _j = solver.chain().size() - 1;
            _radius = radius;
        }

        public void animate() {
            if (_forwardStage) animateForward();
            else animateBackward();
        }

        public void animateForward() {
            ArrayList<Vector> prevStructure = _solver.iterationsHistory().get(_current - 1);
            ArrayList<Vector> nextStructure = _solver.iterationsHistory().get(_current);
            //Animate
            if (_step > 4 && _trajectory._completed) {
                _step = 0;
                _j--;
            }
            if (_j <= 0) {
                _current++;
                _j = 0;
                _forwardStage = false;
                _step = 0;
                return;
            }
            animateStep(prevStructure.get(_j - 1), prevStructure.get(_j), nextStructure.get(_j - 1), nextStructure.get(_j));
        }

        public void animateBackward() {
            ArrayList<Vector> prevStructure = _solver.iterationsHistory().get(_current - 1);
            ArrayList<Vector> nextStructure = _solver.iterationsHistory().get(_current);
            //Animate
            if (_step > 4 && _trajectory._completed) {
                _step = 0;
                _j++;
            }
            if (_j >= nextStructure.size() - 1) {
                _current++;
                _j = nextStructure.size() - 1;
                _step = 0;
                _forwardStage = true;
                return;
            }
            animateStep(prevStructure.get(_j + 1), prevStructure.get(_j), nextStructure.get(_j + 1), nextStructure.get(_j));
        }


        public void animateStep(Vector j_i, Vector j_i1, Vector j_i_hat, Vector j_i1_hat) {

            if (_trajectory == null || _trajectory._completed) {
                //create next trajectory
                switch (_step) {
                    case 0: {
                        _previousSegment = null;
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowTrajectoryStep(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowTrajectoryStep) _trajectory).setTrajectory(j_i1, j_i1_hat);
                        ((FollowTrajectoryStep) _trajectory)._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory)._message = "Step 1: Assume J_i reach its Target Position";
                        break;
                    }
                    case 1: {
                        _previousSegment = ((FollowTrajectoryStep) _trajectory)._trajectory.clone();
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowTrajectoryStep(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowTrajectoryStep) _trajectory).setTrajectory(j_i1_hat, j_i);
                        ((FollowTrajectoryStep) _trajectory)._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory)._message = "Step 2: Find the Segment Line defined by J_t and J_i";
                        break;
                    }
                    case 2: {
                        Vector v = Vector.subtract(j_i, j_i1_hat);
                        v.normalize();
                        v.multiply(Vector.distance(j_i1, j_i));
                        v.add(j_i1_hat);
                        _previousSegment = ((FollowTrajectoryStep) _trajectory)._trajectory.clone();
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowTrajectoryStep(_scene, _radius, _period);
                        ((FollowTrajectoryStep) _trajectory)._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        _trajectory.start();
                        ((FollowTrajectoryStep) _trajectory).setTrajectory(j_i1_hat, v);
                        ((FollowTrajectoryStep) _trajectory)._message = "Step 3: Find the new joint's position by fixing the length of Segment Line";
                        break;
                    }
                    case 3: {
                        Vector v = Vector.subtract(j_i, j_i1_hat);
                        v.normalize();
                        v.multiply(Vector.distance(j_i1, j_i));
                        v.add(j_i1_hat);
                        _previousSegment = ((FollowTrajectoryStep) _trajectory)._trajectory.clone();
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowArc(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowArc) _trajectory).setTrajectory(j_i1_hat, v, j_i_hat);
                        ((FollowArc) _trajectory)._mode = FollowArc.Arc.V1_TO_V;
                        ((FollowArc) _trajectory)._message = "Step 4: In case of constraints Move to a Feasible position";
                        break;
                    }
                    case 4: {
                        _previousSegment = Arrays.copyOfRange(((FollowArc) _trajectory)._trajectory, 1, ((FollowArc) _trajectory)._trajectory.length);
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowTrajectoryStep(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowTrajectoryStep) _trajectory)._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory).setTrajectory(j_i1_hat, j_i_hat);
                        ((FollowTrajectoryStep) _trajectory)._message = "Step 4: Connect new found position with target position";
                        break;
                    }

                }
                _step++;
            }
        }

        public void draw() {
            if(_solver.iterationsHistory().size() < 2) return;
            animate();
            //Draw previous iteration
            ArrayList<Vector> prev = _solver.iterationsHistory().get(_current - 1);
            PGraphics pg = _scene.frontBuffer();
            for (int i = 0; i < prev.size() - 1; i++) {
                Vector v1 = prev.get(i);
                Vector v2 = prev.get(i + 1);
                pg.pushStyle();
                if (_scene.is3D()) {
                    drawSegment3D(pg, v1, v2, _radius, pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100));
                } else {
                    drawSegment2D(pg, v1, v2, _radius, pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100));
                }
                pg.popStyle();
            }

            drawIteration(pg);
        }

        public void drawIteration(PGraphics pg) {
            if (_previousSegment != null) {
                if (_scene.is3D()) {
                    drawSegment3D(pg, _previousSegment[0], _previousSegment[1], _radius, pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100));
                } else {
                    drawSegment2D(pg, _previousSegment[0], _previousSegment[1], _radius, pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100));
                }
            }

            ArrayList<Vector> next = _solver.iterationsHistory().get(_current);
            int i = _forwardStage ? next.size() - 1 : 0;
            while (i != _j) {
                Vector v1 = _forwardStage ? next.get(i - 1) : next.get(i + 1);
                Vector v2 = next.get(i);
                pg.pushStyle();
                if (_scene.is3D()) {
                    drawSegment3D(pg, v1, v2, _radius, pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200));
                } else {
                    drawSegment2D(pg, v1, v2, _radius, pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200));
                }
                pg.popStyle();
                i = _forwardStage ? i - 1 : i + 1;
            }
            //Animate current step
            if (_trajectory != null) _trajectory.draw();
        }
    }

    public static class CCDAnimation {
        //Keep hist of iterations using FABRIK
        protected CCDSolver _solver;
        protected Scene _scene; //where to display animation
        protected int _current = 1; //current iteration
        protected int _step = 0, _j = 0; //useful variables to check which Joint is being animated
        protected Step _trajectory;
        protected Vector[] _previousSegment;
        protected float _radius;
        protected long _period = 8000;


        public CCDAnimation(Scene scene, CCDSolver solver, float radius) {
            _scene = scene;
            _solver = solver;
            _j = solver.chain().size() - 1;
            _radius = radius;
        }

        public void animate() {
            ArrayList<Vector> prevStructure = _solver.iterationsHistory().get(_current - 1);
            ArrayList<Vector> nextStructure = _solver.iterationsHistory().get(_current);
            //Animate
            if (_step > 2 && _trajectory._completed) {
                _step = 0;
                //Rotate previous found positions by same amount
                Quaternion rot = new Quaternion(Vector.subtract(prevStructure.get(_j), prevStructure.get(_j - 1)),Vector.subtract(nextStructure.get(_j), prevStructure.get(_j - 1)));
                for(int i = _j + 1; i < nextStructure.size(); i++){
                    Vector v = Vector.subtract(nextStructure.get(i), prevStructure.get(i - 1));
                    v = rot.rotate(v);
                    v.add(nextStructure.get(i - 1));
                    nextStructure.set(i, v);
                }
                _j--;
            }
            if (_j <= 0) {
                _current++;
                _j = nextStructure.size() - 1;
                _step = 0;
                return;
            }
            animateStep(prevStructure.get(_j - 1), prevStructure.get(_j), nextStructure.get(_j));
        }

        public void animateStep(Vector j_i, Vector j_i1, Vector j_i1_hat) {
            if (_trajectory == null || _trajectory._completed) {
                //create next trajectory
                switch (_step) {
                    case 0: {
                        _previousSegment = null;
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowTrajectoryStep(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowTrajectoryStep) _trajectory).setTrajectory(j_i, _solver.target().position());
                        ((FollowTrajectoryStep) _trajectory)._mode = FollowTrajectoryStep.Line.V1_TO_V;
                        ((FollowTrajectoryStep) _trajectory)._message = "Step 1: Find the segment Line defined by J_i and Target";
                        break;
                    }
                    case 1: {
                        _previousSegment = ((FollowTrajectoryStep) _trajectory)._trajectory.clone();
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowArc(_scene, _radius, _period);
                        _trajectory.start();
                        ((FollowArc) _trajectory).setTrajectory(j_i, j_i1, _solver.target().position());
                        ((FollowArc) _trajectory)._mode = FollowArc.Arc.V1_TO_V;
                        ((FollowArc) _trajectory)._message = "Step 2: Rotate J_i to reach the previous segment line";
                        break;
                    }
                    case 2: {
                        Vector unconstrained = ((FollowArc)_trajectory)._current;
                        _previousSegment = Arrays.copyOfRange(((FollowArc) _trajectory)._trajectory, 1, ((FollowArc) _trajectory)._trajectory.length);
                        _scene.unregisterAnimator(_trajectory);
                        _trajectory = new FollowArc(_scene, _radius, _period);
                        ((FollowArc) _trajectory)._mode = FollowArc.Arc.V1_TO_V;
                        _trajectory.start();
                        ((FollowArc) _trajectory).setTrajectory(j_i, unconstrained, j_i1_hat);
                        ((FollowArc) _trajectory)._message = "Step 3: In case of constraints Move to a Feasible position";
                    }
                }
                _step++;
            }
        }

        public void draw() {
            if(_solver.iterationsHistory().size() < 2) return;
            animate();
            //Draw previous iteration
            ArrayList<Vector> prev = _solver.iterationsHistory().get(_current - 1);
            PGraphics pg = _scene.frontBuffer();
            for (int i = 0; i < prev.size() - 1; i++) {
                Vector v1 = prev.get(i);
                Vector v2 = prev.get(i + 1);
                pg.pushStyle();
                if (_scene.is3D()) {
                    drawSegment3D(pg, v1, v2, _radius, pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100));
                } else {
                    drawSegment2D(pg, v1, v2, _radius, pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100), pg.color(255, 255, 0, 100));
                }
                pg.popStyle();
            }
            drawIteration(pg);
        }

        public void drawIteration(PGraphics pg) {
            if (_previousSegment != null) {
                if (_scene.is3D()) {
                    drawSegment3D(pg, _previousSegment[0], _previousSegment[1], _radius, pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100));
                } else {
                    drawSegment2D(pg, _previousSegment[0], _previousSegment[1], _radius, pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100), pg.color(255, 0, 0, 100));
                }
            }

            ArrayList<Vector> prev = _solver.iterationsHistory().get(_current - 1);
            ArrayList<Vector> next = _solver.iterationsHistory().get(_current);
            int i = next.size() - 1;
            while (i != _j) {
                Vector v1 = i == _j + 1 ? prev.get(i - 1) : next.get(i - 1);
                Vector v2 = next.get(i);
                pg.pushStyle();
                if (_scene.is3D()) {
                    drawSegment3D(pg, v1, v2, _radius, pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200));
                } else {
                    drawSegment2D(pg, v1, v2, _radius, pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200), pg.color(0, 255, 0, 200));
                }
                pg.popStyle();
                i--;
            }
            //Animate current step
            if (_trajectory != null) _trajectory.draw();
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

}
