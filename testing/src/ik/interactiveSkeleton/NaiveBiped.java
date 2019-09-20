package ik.interactiveSkeleton;

import nub.core.Node;
import nub.core.Graph;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.FABRIKSolver;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.geometric.MySolver;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.TimingTask;
import ik.basic.Util;
import nub.ik.visual.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NaiveBiped extends PApplet {

    public enum IKMode{ BIOIK, FABRIK, CCD, MYSOLVER};

    Scene scene;
    float boneLength = 50;
    float radius = 10;

    int segments = 7;
    float stepHeight = boneLength/2 * segments/3f, stepWidth = boneLength * segments/3f;

    public void settings() {
        size(700, 700, P3D);
    }

    //DEBUGGING VARS
    boolean debug = false;
    boolean solve = !debug;
    boolean show[] = new boolean[4];
    //--------------------
    ArrayList<Solver> solvers = new ArrayList<>();

    public void setup(){
        if(debug){
            FABRIKSolver.debug = true;
            for(int i = 0; i < show.length; i++) show[i] = true;
        }

        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(segments * 2 * boneLength);
        scene.fit(1);

        if(!debug) {
            createStructure(scene, segments, boneLength, radius, color(255, 0, 0), new Vector(-boneLength * 3, 0, 0), IKMode.BIOIK);
            createStructure(scene, segments, boneLength, radius, color(0, 255, 0), new Vector(boneLength * 1, 0, 0), IKMode.CCD);
            //createStructure(scene, segments, boneLength, radius, color(0, 255, 0), new Vector(boneLength * 1, 0, 0), IKMode.FABRIK);
        }
        createStructure(scene, segments, boneLength, radius, color(0,0,255), new Vector(boneLength*5, 0,0), IKMode.FABRIK);
        //createStructure(scene, segments, boneLength, radius, color(0,0,255), new Vector(boneLength*5, 0,0), IKMode.MYSOLVER);

    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        if(debug){
            hint(DISABLE_DEPTH_TEST);
            for(Solver solver : solvers){
                if(solver instanceof MySolver){
                    scene.beginHUD();
                    Vector t = scene.screenLocation(((MySolver) solver).target());
                    text(" " + ((MySolver) solver).target().position(), t.x(), t.y());
                    scene.endHUD();
                }
                if(!(solver instanceof ChainSolver)) continue;
                ChainSolver s = (ChainSolver) solver;
                /*if(s.iterationsHistory() != null && !s.iterationsHistory().isEmpty() && show[0]) {
                    int last = s.iterationsHistory().size() - 1;
                    int prev1 = last > 0 ? last - 1 : 0;
                    int prev2 = last > 1 ? last - 2 : 0;
                    Util.drawPositions(scene.context(), s.iterationsHistory().get(prev2), color(255, 255, 255), 3);
                    Util.drawPositions(scene.context(), s.iterationsHistory().get(prev1), color(0, 255, 0), 3);
                    Util.drawPositions(scene.context(), s.iterationsHistory().get(last), color(255, 0, 0), 3);
                }*/
                if(s.divergeHistory() != null && !s.divergeHistory().isEmpty() && show[1]) {
                    for (ArrayList<Vector> l : s.divergeHistory()) {
                        Util.drawPositions(scene.context(), l, color(255, 255, 0, 50), 3);
                    }
                }
                if(s.avoidHistory() != null && !s.avoidHistory().isEmpty() && show[2]) {
                    for (ArrayList<Vector> l : s.avoidHistory()) {
                        Util.drawPositions(scene.context(), l, color(255, 0, 255, 50), 3);
                    }
                }
            }
            hint(ENABLE_DEPTH_TEST);
        }
    }

    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
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

    public void keyPressed(){
        if(key == 'S' || key == 's') {
            solve = !solve;
        }else if(key =='a' || key == 'A'){
            for(Solver s : solvers){
                s.solve();
            }
        }else {
            try {
                int i = Integer.parseInt("" + key) - 1;
                show[i] = !show[i];
            } catch (Exception e) {
            }
        }
    }

    public Solver addIKbehavior(Scene scene, ArrayList<Node> limb, Node target, IKMode mode){
        Solver solver;
        switch (mode){
            case CCD:{
                solver = new CCDSolver(limb,true);
                ((CCDSolver)solver).setTarget(target);
                break;
            }
            case FABRIK:{
                solver = new ChainSolver(limb);
                //chainSolver.setFixTwisting(false);
                //chainSolver.setMaxIterations(5);
                //chainSolver.setDirectionWeight(0.5f);
                //chainSolver.explore(false);
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

            default:{
                return null;
            }
        }

        solver.setMaxError(3f);
        if (!debug) solver.setTimesPerFrame(5);
        else solver.setTimesPerFrame(1f);
        target.setPosition(limb.get(limb.size() - 1).position());
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                if (solve) solver.solve();
            }
        };
        scene.registerTask(task);
        task.run(20);
        return solver;
    }

    public Node createTarget(Scene scene, float radius){
        PShape ball = createShape(SPHERE, radius);
        ball.setFill(color(255,0,0));
        ball.setStroke(false);
        return new Node(scene, ball);
    }

    public void createStructure(Scene scene, int segments, float length, float radius, int color, Vector translation, IKMode mode){
        Node reference = new Node(scene);
        reference.translate(translation);

        //1. Create reference Frame
        Joint root = new Joint(scene, color(255,0,0), radius);
        root.setRoot(true);
        root.setReference(reference);

        //2. Create Targets, Limbs & Solvers
        if(!debug){
            Node target1 = createTarget(scene, radius*1.2f);
            Node target2 = createTarget(scene, radius*1.2f);

            solvers.add(createLimb(scene, segments, length, radius, color, root, target1, new Vector(-length,0,0), mode));
            solvers.add(createLimb(scene, segments, length, radius, color, root, target2, new Vector(length,0,0), mode));

            //3. Create walking cycle
            createBipedCycle(scene, root, solvers.get(solvers.size() - 1), solvers.get(solvers.size() - 2), target1, target2);
        } else{
            Node target = createTarget(scene, radius*1.2f);
            solvers.add(createLimb(scene, segments, length, radius, color, root, target, new Vector(length,length,0), mode));
        }

    }


    public Solver createLimb(Scene scene, int segments, float length, float radius, int color, Node reference, Node target, Vector translation, IKMode mode){
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
                float max = 3;
                float min = 45;

                Hinge hinge = new Hinge(radians(min), radians(max), middle.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
                middle.setConstraint(hinge);
            }
            joints.add(middle);
        }
        BallAndSocket cone = new BallAndSocket(radians(20),radians(20));
        cone.setRestRotation(joints.get(joints.size() - 1).rotation().get(), new Vector(0,-1,0), new Vector(0,0,1));
        //joints.get(joints.size() - 1).setConstraint(cone);

        Joint low = new Joint(scene, color, radius);
        low.setReference(joints.get(joints.size() - 1));
        low.translate(0,0,length);

        joints.add(low);
        root.translate(translation);
        root.setConstraint(new Hinge(radians(60), radians(60), root.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0)));
        return addIKbehavior(scene, joints, target, mode);
    }



    public void createBipedCycle(Scene scene, Node root, Solver leg1, Solver leg2, Node target1, Node target2){
        //Create Walking Cycle
        Cycle cycle = new Cycle(scene);
        //Create Steps
        // First step
        Cycle.Step step1 = new Cycle.Step() {
            float angle = 0;
            float step = 3f;
            float w = 0.5f * stepWidth / 180;
            Vector initial;

            @Override
            public void start() {
                angle = 0;
                initial = target1.translation();
            }

            @Override
            public void execute(){
                //Follow a sin trajectory
                float y = stepHeight * sin(radians(angle));
                target1.setTranslation(new Vector(initial.x(), initial.y() - y, initial.z() + w*angle));
                angle +=  step;
            }

            @Override
            public boolean completed() {
                return angle > 180;
            }
        };
        cycle.addStep(step1);
        cycle.initialStep(step1);
        //Second step move root
        Cycle.Step step2 = new Cycle.Step() {
            float z = 0;
            float step = 0;
            float times = 15;
            Vector initial;

            @Override
            public void start() {
                z = 0;
                step = 0.5f * stepWidth/times;
                initial = root.translation().get();
            }

            @Override
            public void execute(){
                root.setTranslation(initial.x(), initial.y(), initial.z() + z);
                leg1.change(true);
                leg2.change(true);
                z += step;
            }

            @Override
            public boolean completed() {
                return z > 0.5f * stepWidth;
            }
        };
        cycle.addStep(step2);
        cycle.addTransition(step1, step2);
        // First step
        Cycle.Step step3 = new Cycle.Step() {
            float angle = 0;
            float step = 3f;
            float w = 0.5f * stepWidth / 180;
            Vector initial;

            @Override
            public void start() {
                angle = 0;
                initial = target2.translation().get();
            }

            @Override
            public void execute(){
                //Follow a sin trajectory
                float y = stepHeight * sin(radians(angle));
                target2.setTranslation(new Vector(initial.x(), initial.y() - y, initial.z() + w*angle));
                angle +=  step;
            }

            @Override
            public boolean completed() {
                return angle > 180;
            }
        };
        cycle.addStep(step3);
        cycle.addTransition(step2, step3);
        cycle.addTransition(step3, step1);
    }

    public static class Cycle{
        public static abstract class Step{
            TimingTask _task = new TimingTask() {
                @Override
                public void execute() {
                    Step.this.execute();
                }
            };
            public abstract boolean completed();
            public abstract void execute();
            public abstract void start();
        }
        Scene _scene;
        List<Step> _steps = new ArrayList<Step>();
        HashMap<Step, Step> _transition = new HashMap<>();
        Step _current = null;

        public void run(){
            if(_current == null){
                if(_transition.containsKey(_current)) {
                    _current = _transition.get(null);
                    _current.start();
                    _current._task.run(10);
                }
            }else if(_current.completed()){
                _current._task.stop();
                if(_transition.containsKey(_current)) {
                    _current = _transition.get(_current);
                    _current.start();
                    _current._task.run(10);
                }
            }
        }

        public void addStep(Step step){
            _steps.add(step);
            _scene.registerTask(step._task);
        }

        public void initialStep(Step step){
            addTransition(null, step);
        }

        public void addTransition(Step from, Step to){
            _transition.put(from, to);
        }

        public Cycle(Scene scene){
            _scene = scene;
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    Cycle.this.run();
                }
            };
            _scene.registerTask(task);
            task.run(5);
        }
    }
    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.NaiveBiped"});
    }

}
