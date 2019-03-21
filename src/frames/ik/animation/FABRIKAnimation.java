package frames.ik.animation;

import frames.ik.ChainSolver;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.AnimatorObject;
import processing.core.PConstants;
import processing.core.PGraphics;
import java.util.ArrayList;

public class FABRIKAnimation extends AnimatorObject {

    public static abstract class Step extends AnimatorObject{
        boolean _completed = false;
        Scene _scene;

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
            _cv1 = pg.color(255,0,0);
            _cv2 = pg.color(255,0,0);
            _cv = pg.color(0,0,255);
            _cline = pg.color(255,0,0);
            _radius = radius;
            _edgePeriod = edgePeriod;
        }

        public void setTrajectory(Vector... trajectory){
            _trajectory = trajectory;
            _idx = 0;
            _current = trajectory[_idx].get();
            _completed = false;
            calculateSpeedPerIteration();
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


    //Keep hist of iterations using FABRIK
    protected ChainSolver _solver;
    protected Scene _scene; //where to display animation
    protected int _current = 1; //current iteration
    protected int _step = 0, _j = 0; //useful variables to check which Joint is being animated
    protected FollowTrajectoryStep _trajectory;
    protected float _radius;
    protected int _cline, _cv1, _cv2, _cv;


    public FABRIKAnimation(Scene scene, ChainSolver solver, float radius) {
        super(scene.timingHandler());
        _scene = scene;
        _solver = solver;
        _j = solver.chain().size() - 1;
        _radius = radius;
        setPeriod(1000);
        stop();
        _trajectory = new FollowTrajectoryStep(scene,radius, period()*5);
        _trajectory.start();

        PGraphics pg = _scene.frontBuffer();

        _cv1 = pg.color(255,0,0, 100);
        _cv2 = pg.color(255,0,0, 100);
        _cv = pg.color(0,0,255, 100);
        _cline = pg.color(255,0,0, 100);
    }

    @Override
    public void animate(){
        animateForward();
    }

    public void animateForward(){
        ArrayList<Vector> prevStructure = _solver.iterationsHistory().get(_current - 1);
        ArrayList<Vector> nextStructure = _solver.iterationsHistory().get(_current);
        //Animate
        if(_step > 3){
            _step = 0;
            _j--;
        }
        if(_j <= 0) return;
        animateForwardStep(prevStructure.get(_j - 1), prevStructure.get(_j), nextStructure.get(_j - 1), nextStructure.get(_j));
    }

    public void animateForwardStep(Vector j_i, Vector j_i1, Vector j_i_hat, Vector j_i1_hat){
        if(_trajectory._completed || _trajectory._trajectory == null){
            //create next trajectory
            switch (_step){
                case 0:{
                    _trajectory.setTrajectory(j_i1, j_i1_hat);
                    _trajectory._message = "Step 1: Assume J_i reach its Target Position";
                    break;
                }
                case 1:{
                    _trajectory.setTrajectory(j_i1_hat, j_i);
                    _trajectory._message = "Step 2: Find the Segment Line defined by J_t and J_i";
                    break;
                }
                case 2:{
                    Vector v = Vector.subtract(j_i, j_i1_hat);
                    v.normalize();
                    v.multiply(Vector.distance(j_i1, j_i));
                    v.add(j_i1_hat);
                    _trajectory.setTrajectory(j_i1_hat, v);
                    _trajectory._message = "Step 3: Find the new joint's position by fixing the length of Segment Line";
                    break;
                }
                case 3:{
                    Vector v = Vector.subtract(j_i, j_i1_hat);
                    v.normalize();
                    v.multiply(Vector.distance(j_i1, j_i));
                    v.add(j_i1_hat);
                    _trajectory.setTrajectory(v, j_i_hat);
                    _trajectory._message = "Step 4: In case of constraints Move to a Feasible position";
                    break;
                }
            }
            _step++;
        }
    }

    public void draw(){
        //Draw previous iteration
        ArrayList<Vector> prev = _solver.iterationsHistory().get(_current - 1);
        PGraphics pg = _scene.frontBuffer();
        for(int i = 0; i < prev.size() - 1 ; i++){
            Vector v1 = prev.get(i);
            Vector v2 = prev.get(i+1);
            pg.pushStyle();
            if(_scene.is3D()) {
                drawSegment3D(pg, v1, v2, _radius, pg.color(255,255,0,100), pg.color(255,255,0,100), pg.color(255,255,0,100));
            } else{
                drawSegment2D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
            }
            pg.popStyle();
        }

        drawForward(pg);
    }

    public void drawForward(PGraphics pg){
        //Draw previous steps & previous state
        ArrayList<Vector> next = _solver.iterationsHistory().get(_current);
        for(int i = next.size() - 1; i > _j; i--){
            Vector v1 = next.get(i-1);
            Vector v2 = next.get(i);
            pg.pushStyle();
            if(_scene.is3D()) {
                drawSegment3D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
            } else{
                drawSegment2D(pg, v1, v2, _radius, _cline, _cv1, _cv2);
            }
            pg.popStyle();
        }

        //Animate current step
        if(_trajectory._trajectory != null)_trajectory.draw();
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
