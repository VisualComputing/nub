package ik.constraintTest;

import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.core.Frame;
import frames.ik.evolution.BioIk;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.basic.Util;
import processing.core.PApplet;
import processing.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by sebchaparr on 8/07/18.
 */
public class ConeBall extends PApplet{
    int numJoints = 10;
    float targetRadius = 7;
    float boneLength = 50;
    boolean solve = false;

    //Scene Parameters
    Scene scene;
    //Benchmark Parameters
    Random random = new Random();
    ArrayList<Solver> solvers; //Will store Solvers
    ArrayList<ArrayList<Frame>> structures = new ArrayList<>(); //Keep Structures
    ArrayList<Frame> targets = new ArrayList<Frame>(); //Keep targets

    int randRotation = -1; //Set seed to generate initial random rotations, otherwise set to -1
    int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1
    int numSolvers = 6; //Set number of solvers

    public void settings() {
        size(1500, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength * 1.2f);
        scene.fit(1);
        scene.setRightHanded();

        //1. Create Targets
        targets = Util.createTargets(numSolvers, scene, targetRadius);
        float alpha = 1.f * width / height > 1.5f ? 0.5f * width / height : 0.5f;
        alpha *= numSolvers/4f; //avoid undesirable overlapping

        //2. Generate Structures
        for(int i = 0; i < numSolvers; i++){
            int color = color(random(255), random(255), random(255));
            structures.add(Util.generateChain(scene, numJoints, 0.3f* targetRadius, boneLength, new Vector(i * 2 * alpha * scene.radius()/(numSolvers - 1) - alpha * scene.radius(), 0, 0), color, randRotation, randLength));
        }

        //3. Apply constraints
        float down = radians(60);
        float up = radians(45);
        float left = radians(55);
        float right = radians(40);

        for(ArrayList<Frame> structure : structures){
            for (int i = 0; i < structure.size() - 1; i++) {
                Vector twist = structure.get(i + 1).translation().get();
                BallAndSocket constraint = new BallAndSocket(down, up, left, right);
                constraint.setRestRotation(structure.get(i).rotation().get(), new Vector(0, 1, 0), twist);
                structure.get(i).setConstraint(constraint);
            }
        }

        //4. Set eye scene
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //5. generate solvers
        solvers = new ArrayList<>();

        int i = 0;
        //CCD
        solvers.add(new CCDSolver(structures.get(i++)));
        //Standard FABRIK
        ChainSolver chainSolver;
        chainSolver = new ChainSolver(structures.get(i++));
        chainSolver.setKeepDirection(false);
        chainSolver.setFixTwisting(false);
        solvers.add(chainSolver);
        //FABRIK Keeping directions (H1)
        chainSolver = new ChainSolver(structures.get(i++));
        chainSolver.setFixTwisting(true);
        chainSolver.setKeepDirection(false);
        solvers.add(chainSolver);
        //FABRIK Fix Twisting (H2)
        chainSolver = new ChainSolver(structures.get(i++));
        chainSolver.setFixTwisting(false);
        chainSolver.setKeepDirection(true);
        solvers.add(chainSolver);
        //FABRIK Fix Twisting (H1 & H2)
        chainSolver = new ChainSolver(structures.get(i++));
        chainSolver.setFixTwisting(true);
        chainSolver.setKeepDirection(true);
        solvers.add(chainSolver);
        //BioIK
        solvers.add(new BioIk(structures.get(i++), 10, 4));

        for(i = 0; i < solvers.size(); i++){
            Solver solver = solvers.get(i);
            //6. Define solver parameters
            solver.error = 0.001f;
            solver.timesPerFrame = 5;
            solver.maxIter = 200;
            //7. Set targets
            solver.setTarget(structures.get(i).get(numJoints - 1), targets.get(i));
            targets.get(i).setPosition(structures.get(i).get(numJoints - 1).position());

            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    if(solve) {
                        solver.solve();
                    }
                }
            };
            scene.registerTask(task);
            task.run(40);
        }
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();

        scene.beginHUD();
        for(int  i = 0; i < solvers.size(); i++) {
            Util.printInfo(scene, solvers.get(i), structures.get(i).get(0).position());
        }
        scene.endHUD();
    }

    public void keyPressed(){
        if(key == 'w' || key == 'W'){
            solve = !solve;
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
            if(targets.contains(scene.trackedFrame())){
                for(Frame target : targets) scene.translate(target);
            }else{
                scene.translate();
            }
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
        PApplet.main(new String[]{"ik.constraintTest.ConeBall"});
    }
}
