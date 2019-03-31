package ik.basic;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Constraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.*;
import frames.ik.evolution.BioIk;
import frames.ik.evolution.GASolver;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.jacobian.PseudoInverseSolver;
import frames.ik.jacobian.SDLSSolver;
import frames.ik.jacobian.TransposeSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class VisualBenchmarkMultipleEF  extends PApplet {
    enum ConstraintType{ NONE, HINGE, CONE_POLYGON, CONE_ELLIPSE, MIX }
    ConstraintType mode = ConstraintType.NONE; //Choose an option
    boolean is3D = true;
    int depth = 2;
    int repetitions = 3;
    float targetRadius = 30;
    float boneLength = 50;
    Random random = new Random();

    Scene scene;
    //Methods
    ArrayList<Solver> solvers;
    ArrayList<ArrayList<Frame>> structures = new ArrayList<>();
    ArrayList<HashMap<Frame,Frame>> targets = new ArrayList<>();

    public void settings() {
        size(1500, 800, is3D ? P3D : P2D);
    }

    public void setup() {
        scene = new Scene(this);
        if(is3D) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(2 * repetitions * depth * 1f * boneLength);
        scene.fit(1);
        scene.setRightHanded();

        float alpha = 1.f * width / height > 1.5f ? 0.5f * width / height : 0.5f;
        alpha *= depth/2f; //avoid undesirable overlapping

        int color = color(random(255), random(255), random(255));
        structures.add(generateYShape(depth, false, boneLength, repetitions, new Vector(), color));

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //BioIk solver = new BioIk(structures.get(0),10,4);

        //solver.maxIter = 200;
        //solver.timesPerFrame = 30.f;


        scene.registerTreeSolver(structures.get(0).get(0));
        for(Frame f : targets.get(0).keySet()){
            //solver.setTarget(f, targets.get(0).get(f));
            scene.addIKTarget(f, targets.get(0).get(f));
        }
        /*
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                solver.solve();
                //System.out.println("Error: " + solver.error());
            }
        };

        scene.registerTask(task);
        task.run(40);
        */
    }

    public void draw() {
        background(0);
        if(is3D) lights();
        //Draw Constraints
        scene.drawAxes();
        scene.render();
        scene.beginHUD();

        scene.endHUD();
    }

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
    }

    public Frame generateTarget(Frame frame){
        PShape redBall;
        if(is3D)  redBall = createShape(SPHERE, targetRadius);
        else  redBall = createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        Frame target = new Frame(scene){
            @Override
            public void visit() {
                scene.drawAxes(targetRadius * 2);
                if(scene.trackedFrame() == this){
                    redBall.setFill(color(0,255,0));
                }else{
                    redBall.setFill(color(255,0,0));
                }
                scene.pApplet().shape(redBall);
            }
        };
        target.setPickingThreshold(targetRadius);
        target.setHighlighting(Frame.Highlighting.FRONT);
        target.setPosition(frame.position().get());
        target.setOrientation(frame.orientation().get());
        return target;
    }

    public ArrayList<Frame> generateYShape(int depth, boolean independent, float boneLength, int repetitions, Vector translation, int color) {
        Joint reference = independent ? null : new Joint(scene, color, targetRadius * 0.3f);
        HashMap<Frame, Frame> target = new HashMap<>();
        Joint root = generateYShape(reference, independent, depth, repetitions, boneLength, color, target);
        root.setRoot(true);
        root.translate(translation);
        targets.add(target);
        return new ArrayList<Frame>(scene.branch(root));
    }

    public Joint generateYShape(Joint reference, boolean independent, int depth, int repetitions, float boneLength, int color, HashMap<Frame, Frame> targets) {
        if(depth < 1){
            targets.put(reference, generateTarget(reference));
            return null;
        }
        //create Y - Shape
        if(independent) {
            Joint j0 = new Joint(scene, color, targetRadius * 0.3f);
            j0.setReference(reference);
            reference = j0;
        }
        Joint prev = reference;
        Joint j1 = reference;
        for(int i = 0; i < repetitions; i++){
            j1 = new Joint(scene, color, targetRadius * 0.3f);
            j1.setReference(prev);
            j1.translate(0, boneLength, 0);
            prev = j1;
        }

        prev = j1;
        Joint j2 = j1;
        for(int i = 0; i < repetitions; i++){
            j2 = new Joint(scene, color, targetRadius * 0.3f);
            j2.setReference(prev);
            j2.translate(Vector.multiply(new Vector(1, 1, 0).normalize(null), boneLength));
            prev = j2;
        }

        prev = j1;
        Joint j3 = j1;
        for (int i = 0; i < repetitions; i++) {
            j3 = new Joint(scene, color, targetRadius * 0.3f);
            j3.setReference(prev);
            j3.translate(Vector.multiply(new Vector(-1, 1, 0).normalize(null), boneLength));
            prev = j3;
        }
        //Generate Y Shape on each Branch
        j2.rotate(new Quaternion(new Vector(0, 0, 1), radians(-45)));
        j3.rotate(new Quaternion(new Vector(0, 0, 1), radians(45)));
        generateYShape(j2, independent, depth - 1, repetitions, boneLength, color, targets);
        generateYShape(j3, independent, depth - 1, repetitions, boneLength, color, targets);
        return independent ? reference : j1;
    }

    public Frame generateRandomReachablePosition(List<? extends Frame> original){
        ArrayList<? extends Frame> chain = copy(original);
        for(int i = 0; i < chain.size(); i++){
            if(is3D)
                chain.get(i).rotate(new Quaternion(Vector.random(), (float)(random.nextGaussian()*random.nextFloat()*PI/2)));
            else
                chain.get(i).rotate(new Quaternion(new Vector(0,0,1), (float)(random.nextGaussian()*random.nextFloat()*PI/2)));
        }
        return chain.get(chain.size()-1);
    }

    public ArrayList<Frame> copy(List<? extends Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get(), 1);
        }
        for (Frame joint : chain) {
            Frame newJoint = new Frame();
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            reference = newJoint;
        }
        return copy;
    }


    boolean solve = false;
    public void keyPressed(){
        if(key == 'w' || key == 'W'){
            solve = !solve;
        }
        if(key == 's' || key == 'S'){
            Frame f = generateRandomReachablePosition(structures.get(0));
            //targets.get(0).setPosition(f.position());
        }
        if(key == 'd' || key == 'D'){
            for(List<Frame> structure : structures) {
                for (Frame f : structure) {
                    f.setRotation(new Quaternion());
                }
            }
        }

        // /* Uncomment this to debug a Specific Solver
        if(key == 'z' || key == 'Z'){
            solvers.get(solvers.size()-1).solve();
        }
        // /*
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


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.VisualBenchmarkMultipleEF"});
    }
}

