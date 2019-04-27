package ik.interactiveSkeleton;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Hinge;
import frames.ik.ChainSolver;
import frames.ik.FABRIKSolver;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.basic.Util;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NaiveBiped extends PApplet {


    Scene scene;
    float boneLength = 50;
    float radius = 10;

    int segments = 5;
    float stepHeight = boneLength/2 * segments/3f, stepWidth = boneLength * segments/3f;

    public void settings() {
        size(700, 700, P3D);
    }
    Joint root;

    //DEBUGGING VARS
    boolean debug = false;
    boolean solve = !debug;
    boolean show[] = new boolean[4];
    //--------------------
    ArrayList<ChainSolver> solvers = new ArrayList<>();

    public void setup(){
        FABRIKSolver.debug = debug;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(segments * 2 * boneLength);
        scene.fit(1);

        Frame reference = new Frame(scene);

        //1. Create reference Frame
        root = new Joint(scene, color(255,0,0), radius);
        root.setRoot(true);
        root.setReference(reference);

        //2. Create Limbs & IK Solver
        solvers.add(createLimb(scene, segments, boneLength, radius, color(100), root, new Vector(-boneLength,0,0)));
        solvers.add(createLimb(scene, segments, boneLength, radius, color(100), root, new Vector(boneLength,0,0)));

        //3. Create walking cycle
        if(!debug)createBipedCycle(scene, root, solvers.get(0), solvers.get(1));
    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        if(debug){
            for(ChainSolver s : solvers){
                if(s.iterationsHistory() != null && !s.iterationsHistory().isEmpty() && show[0]) {
                    int last = s.iterationsHistory().size() - 1;
                    int prev = last > 0 ? last - 1 : 0;
                    Util.drawPositions(scene.frontBuffer(), s.iterationsHistory().get(prev), color(0, 255, 0), 3);
                    Util.drawPositions(scene.frontBuffer(), s.iterationsHistory().get(last), color(255, 0, 0), 3);
                }
                if(s.divergeHistory() != null && !s.divergeHistory().isEmpty() && show[1]) {
                    for (ArrayList<Vector> l : s.divergeHistory()) {
                        Util.drawPositions(scene.frontBuffer(), l, color(255, 255, 0, 50), 3);
                    }
                }
                if(s.avoidHistory() != null && !s.avoidHistory().isEmpty() && show[2]) {
                    for (ArrayList<Vector> l : s.avoidHistory()) {
                        Util.drawPositions(scene.frontBuffer(), l, color(255, 0, 255, 50), 3);
                    }
                }


            }
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
            for(ChainSolver s : solvers){
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

    public ChainSolver addIKbehavior(Scene scene, ArrayList<Frame> limb, Frame target){
        ChainSolver chainSolver = new ChainSolver(limb);
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                if(solve) chainSolver.solve();
            }
        };
        chainSolver.error = 1f;
        if(!debug) chainSolver.timesPerFrame = 5;
        else chainSolver.timesPerFrame = 1;
        //chainSolver.setFixTwisting(false);
        //chainSolver.maxIter = 5;
        //chainSolver.setDirectionWeight(0.5f);
        //chainSolver.explore(false);



        target.setPosition(limb.get(limb.size() - 1).position());

        chainSolver.setTarget(target);
        chainSolver.setTargetDirection(new Vector(0,0,1));

        scene.registerTask(task);
        task.run(20);
        return chainSolver;
    }


    public ChainSolver createLimb(Scene scene, int segments, float length, float radius, int color, Frame reference, Vector translation){
        PShape ball = createShape(SPHERE, radius);
        ball.setFill(color(255,0,0));
        ball.setStroke(false);
        Frame target = new Frame(scene, ball);
        target.setReference(reference.reference());

        ArrayList<Frame> joints = new ArrayList<>();
        Joint root = new Joint(scene, color, radius);
        root.setReference(reference);
        joints.add(root);

        for(int i = 0; i < max(segments, 2); i++){
            Joint middle = new Joint(scene, color, radius);
            middle.setReference(joints.get(i));
            middle.translate(0, length, 0);
            if(i < max(segments, 2) - 1) {
                float max = 10;
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
        return addIKbehavior(scene, joints, target);
    }



    public void createBipedCycle(Scene scene, Frame root, ChainSolver leg1, ChainSolver leg2){
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
                initial = leg1.target().translation().get();
            }

            @Override
            public void execute(){
                Frame target = leg1.target();
                //Follow a sin trajectory
                float y = stepHeight * sin(radians(angle));
                target.setTranslation(new Vector(initial.x(), initial.y() - y, initial.z() + w*angle));
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
                initial = leg2.target().translation().get();
            }

            @Override
            public void execute(){
                Frame target = leg2.target();
                //Follow a sin trajectory
                float y = stepHeight * sin(radians(angle));
                target.setTranslation(new Vector(initial.x(), initial.y() - y, initial.z() + w*angle));
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
