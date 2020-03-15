package ik.trik;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.FABRIKSolver;
import nub.ik.solver.geometric.MySolver;
import nub.ik.solver.geometric.oldtrik.TRIK;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NaiveLocomotion extends PApplet {
    public enum IKMode{ BIOIK, FABRIK, CCD, MYSOLVER, TRIK, SIMPLETRIK};
    Scene scene;
    float boneLength = 50;
    float radius = 10;
    int segments = 3;
    float stepHeight = boneLength/2 * segments/6f, stepWidth = boneLength * segments * 0.75f;
    static boolean solve = true;


    public void settings() {
        size(700, 700, P3D);
    }

    //--------------------
    ArrayList<Solver> solvers = new ArrayList<>();

    public void setup(){
        Joint.axes = true;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(segments * 4 * boneLength);
        scene.fit(1);

        createStructure(scene, segments, boneLength, radius, color(0,255,0), new Vector(-boneLength*3, 0,0), IKMode.SIMPLETRIK, 10 , 0);
    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
        } else {
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }

    public Solver addIKbehavior(Scene scene, ArrayList<Node> limb, Node target, IKMode mode){
        Solver solver;
        switch (mode){
            case CCD:{
                solver = new CCDSolver(limb,false);
                ((CCDSolver)solver).setTarget(target);
                break;
            }
            case FABRIK:{
                solver = new ChainSolver(limb);
                ((ChainSolver)solver).setTarget(target);
                ((ChainSolver)solver).setTargetDirection(new Vector(0, 0, 1));
                break;
            }
            case BIOIK:{
                solver = new BioIk(limb, 10,4);
                solver.setTarget(limb.get(limb.size() - 1), target);
                break;
            }
            case MYSOLVER:{
                solver = new MySolver(limb);
                ((MySolver)solver).setTarget(target);
                break;
            }

            case TRIK:{
                solver = new TRIK(limb);
                ((TRIK)solver).setTarget(target);
                ((TRIK)solver).setLookAhead(2);
                ((TRIK)solver).enableWeight(true);
                ((TRIK)solver).smooth(true);
                break;
            }

            case SIMPLETRIK:{
                solver = new SimpleTRIK(limb, SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL);
                ((SimpleTRIK) solver).context().setDirection(true);
                ((SimpleTRIK) solver).context().setSearchingAreaRadius(0.2f, true);
                ((SimpleTRIK) solver).context().setOrientationWeight(0.5f);
                ((SimpleTRIK) solver).enableDeadLockResolution(false);
                solver.setTarget(limb.get(limb.size() - 1), target);


                break;
            }

            default:{
                return null;
            }
        }

        solver.setTimesPerFrame(3);
        solver.setMaxIterations(3);
        solver.setMaxError(5f);

        target.setPosition(limb.get(limb.size() - 1).position());
        return solver;
    }

    public Node createTarget(Scene scene, float radius){
        PShape ball = createShape(SPHERE, radius);
        ball.setFill(color(255,0,0));
        ball.setStroke(false);
        return new Node(scene){
            @Override
            public void graphics(PGraphics pg){
                scene.drawAxes(pg, radius * 3);
                pg.shape(ball);
            }
        };
    }

    public void createStructure(Scene scene, int segments, float length, float radius, int color, Vector translation, IKMode mode, float min , float max){
        Node reference = new Node(scene);
        reference.translate(translation);

        //1. Create reference Frame
        Joint root = new Joint(scene, color(255,0,0), radius);
        root.setRoot(true);
        root.setReference(reference);

        //2. Create Targets, Limbs & Solvers
        Node target1 = createTarget(scene, radius*1.2f);
        Node target2 = createTarget(scene, radius*1.2f);

        solvers.add(createLimb(scene, segments, length, radius, color, root, target1, new Vector(-length,0,0), mode, min, max));
        solvers.add(createLimb(scene, segments, length, radius, color, root, target2, new Vector(length,0,0), mode, min, max));

        //3. Create walking cycle
        createBipedCycle(scene, root, solvers.get(solvers.size() - 1), solvers.get(solvers.size() - 2), target1, target2);
    }


    public Solver createLimb(Scene scene, int segments, float length, float radius, int color, Node reference, Node target, Vector translation, IKMode mode, float min, float max){
        target.setReference(reference.reference());
        ArrayList<Node> joints = new ArrayList<>();
        Joint root = new Joint(scene, color, radius);
        root.setReference(reference);
        joints.add(root);

        for(int i = 0; i < max(segments, 2); i++){
            Joint middle = new Joint(scene, color, radius);
            middle.setReference(joints.get(i));
            middle.translate(0, length, 0);
            if(i < max(segments, 2) - 1) {
                Hinge hinge = new Hinge(radians(min), radians(max), middle.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
                middle.setConstraint(hinge);
            }
            joints.add(middle);
        }
        BallAndSocket cone = new BallAndSocket(radians(50),radians(50));
        cone.setRestRotation(joints.get(joints.size() - 1).rotation().get(), new Vector(0,-1,0), new Vector(0,0,1));
        joints.get(joints.size() - 1).setConstraint(cone);

        Joint low = new Joint(scene, color, radius);
        low.setReference(joints.get(joints.size() - 1));
        low.translate(0,0,length);

        joints.add(low);
        root.translate(translation);
        root.setConstraint(new Hinge(radians(80), radians(80), root.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0)));
        return addIKbehavior(scene, joints, target, mode);
    }



    public void createBipedCycle(Scene scene, Node root, Solver leg1, Solver leg2, Node target1, Node target2){
        //Create Walking Cycle
        Cycle cycle = new Cycle(scene, leg1, leg2);
        Cycle.Step s1, s2, s3, s4, s5, s6;
        s1 = new LiftStep(scene, target1, 0.5f * stepWidth / 180);
        cycle.addStep(s1);
        cycle.initialStep(s1);
        s2 = new ForwardHipStep(scene, root, target1, 0.5f * stepWidth / 180);
        cycle.addStep(s2);
        s3 = new LiftStep(scene, target2, stepWidth / 180);
        cycle.addStep(s3);
        s4 = new ForwardHipStep(scene, root, target1, 0.5f * stepWidth / 180);
        cycle.addStep(s4);
        s5 = new LiftStep(scene, target1, stepWidth / 180);
        cycle.addStep(s5);
        s6 = new ForwardHipStep(scene, root, target1, 0.5f * stepWidth / 180);
        cycle.addStep(s6);

        cycle.addTransition(s1, s2);
        cycle.addTransition(s2, s3);
        cycle.addTransition(s3, s4);
        cycle.addTransition(s4, s5);
        cycle.addTransition(s5, s6);
        cycle.addTransition(s6, s3);

        final Vector center = root.translation();

        TimingTask task = new TimingTask(scene) {
            float ang = 0;

            @Override
            public void execute() {
                Vector v = root.translation().get();
                root.setTranslation(v.x(), center.y() + boneLength * 0.25f + stepHeight * 0.25f * (float) Math.sin(radians(ang)), v.z());
                ang += 10;
            }
        };

        task.run(17);

    }

    public static class Cycle{
        public static abstract class Step{
            public Step(Scene scene){
                _task = new TimingTask(scene) {
                    @Override
                    public void execute() {
                        Step.this.execute();
                    }
                };
            }
            TimingTask _task;
            public abstract boolean completed();
            public abstract void execute();
            public abstract void start();
        }
        Scene _scene;
        List<Step> _steps = new ArrayList<Step>();
        HashMap<Step, Step> _transition = new HashMap<>();
        Step _current = null;
        Solver _leg1, _leg2;

        public void run(){
            for(int i = 0; i < 10; i++) {
                if (_current == null) {
                    if (_transition.containsKey(_current)) {
                        _current = _transition.get(null);
                        _current.start();
                        //_current._task.run(10);
                    }
                } else if (_current.completed()) {
                    _current._task.stop();
                    if (_transition.containsKey(_current)) {
                        _current = _transition.get(_current);
                        _current.start();
                        //_current._task.run(10);
                    }
                }
                _current.execute();
                if (solve) {
                    _leg1.change(true);
                    _leg2.change(true);
                    _leg1.solve();
                    _leg2.solve();
                }
            }
        }

        public void addStep(Step step){
            _steps.add(step);
        }

        public void initialStep(Step step){
            addTransition(null, step);
        }

        public void addTransition(Step from, Step to){
            _transition.put(from, to);
        }

        public Cycle(Scene scene, Solver leg1, Solver leg2){
            _scene = scene;
            this._leg1 = leg1;
            this._leg2 = leg2;
            TimingTask task = new TimingTask(scene) {
                @Override
                public void execute() {
                    Cycle.this.run();
                }
            };
            task.run(20);
        }
    }

    class LiftStep extends Cycle.Step {
        float angle = 0;
        Vector _v;
        Node _target;
        float step = 1f;
        float _w;



        public LiftStep(Scene scene, Node target, float w) {
            super(scene);
            _target = target;
            _w = w;
        }

        @Override
        public boolean completed() {
            return angle > 180;
        }

        @Override
        public void execute() {
            float y = stepHeight * skewedSine(radians(angle), 10);
            _target.setTranslation(new Vector(_v.x(), _v.y() - y, _v.z() + _w * angle));
            angle +=  step;
        }

        @Override
        public void start() {
            angle = 0;
            _v = _target.translation().get();
        }
    }

    class ForwardHipStep extends Cycle.Step {
        float angle = 0;
        Vector _vh , _vt;
        Node _hip, _target;
        float step = 1f;
        float _w;



        public ForwardHipStep(Scene scene, Node hip, Node target, float w) {
            super(scene);
            _hip = hip;
            _target = target;
            _vt = _target.translation().get();
            _w = w;
        }

        @Override
        public boolean completed() {
            return angle > 180;
        }

        @Override
        public void execute() {
            _hip.setTranslation(new Vector(_hip.translation().x(), _hip.translation().y(), _vh.z() + _w * angle));
            angle +=  step;
        }

        @Override
        public void start() {
            angle = 0;
            _vh = _hip.translation().get();
        }
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.trik.NaiveLocomotion"});
    }


    //Based on this https://math.stackexchange.com/questions/2430564/equation-of-a-tilted-sine/2430662 post

    public static float skewedSine(float t, int n){
        int double_n = 2 * n;
        float res = 0;
        for(int k = 1; k <= n; k++){
            res += (float) comb(double_n, n - k) / comb(double_n, n) * Math.sin(k * t) / (float)(k);
        }
        return res;
    }

    public static float fact(int n){
        if(n == 0) return 1;
        float res = 1;
        for(int i = 2; i <= n; i++){
            res *= i;
        }
        return res;
    }

    public static float comb(int n, int k){
        return fact(n) / (fact(k) * fact(n - k));
    }
}
