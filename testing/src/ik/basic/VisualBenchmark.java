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
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class VisualBenchmark extends PApplet {
    //Scene Parameters
    Scene scene;
    String renderer = P3D; //Define a 2D/3D renderer
    int numJoints = 8; //Define the number of joints that each chain will contain
    float targetRadius = 30; //Define size of target
    float boneLength = 50; //Define length of segments (bones)

    //Benchmark Parameters
    Util.ConstraintType constraintType = Util.ConstraintType.MIX; //Choose what kind of constraints apply to chain
    Random random = new Random();
    ArrayList<Solver> solvers; //Will store Solvers
    int randRotation = -1; //Set seed to generate initial random rotations, otherwise set to -1
    int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1


    Util.SolverType solversType [] = {Util.SolverType.SDLS, Util.SolverType.CCD, Util.SolverType.FABRIK, Util.SolverType.HGSA,
            Util.SolverType.FABRIK_H1, Util.SolverType.FABRIK_H2, Util.SolverType.FABRIK_H1_H2}; //Place Here Solvers that you want to compare
    ArrayList<ArrayList<Frame>> structures = new ArrayList<>(); //Keep Structures
    ArrayList<Frame> targets = new ArrayList<Frame>(); //Keep targets

    boolean solve = false;
    boolean show1 = false, show2 = false, show3 = false, show4 = false, show5 = false;

    public void settings() {
        size(1500, 800, renderer);
    }

    public void setup() {
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * 1f * boneLength);
        scene.fit(1);
        scene.setRightHanded();

        int numSolvers = solversType.length;
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
        for(ArrayList<Frame> structure : structures){
            Util.generateConstraints(structure, constraintType, 0, scene.is3D());
        }

        //4. Set eye scene
        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //5. generate solvers
        solvers = new ArrayList<>();
        for(int i = 0; i < numSolvers; i++){
            Solver solver = Util.createSolver(solversType[i], structures.get(i));
            solvers.add(solver);
            //6. Define solver parameters
            solvers.get(i).error = 0.001f;
            solvers.get(i).timesPerFrame = 1;
            solvers.get(i).maxIter = 30;
            solvers.get(i).minDistance = 0.001f;
            //7. Set targets
            solvers.get(i).setTarget(structures.get(i).get(numJoints - 1), targets.get(i));
            targets.get(i).setPosition(structures.get(i).get(numJoints - 1).position());
            //8. Register task
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    if(solve) solver.solve();
                }
            };
            scene.registerTask(task);
            task.run(40);
        }
    }

    public void draw() {
        background(0);
        if(scene.is3D()) lights();
        //Draw Constraints
        scene.drawAxes();

        //Debuggin exploration
        for(int  i = 0; i < solvers.size(); i++) {
            if (solvers.get(i) instanceof ChainSolver) {
                if (show2) Util.drawPositions(scene.frontBuffer(), ((ChainSolver) solvers.get(i)).positions(), color(255, 0, 100), 3);
                if (show4 && ((ChainSolver) solvers.get(i)).avoidHistory() != null) {
                    for(ArrayList<Vector> l : ((ChainSolver) solvers.get(i)).avoidHistory()){
                        Util.drawPositions(scene.frontBuffer(), l, color(255, 255, 0, 50), 3);
                    }
                }
                if (show5 && ((ChainSolver) solvers.get(i)).divergeHistory() != null) {
                    for(ArrayList<Vector> l : ((ChainSolver) solvers.get(i)).divergeHistory()){
                        Util.drawPositions(scene.frontBuffer(), l, color(255, 0, 0, 50), 3);
                    }
                }

                if(((ChainSolver) solvers.get(i)).bestAvoidPosition() != null && show1){
                    Util.drawPositions(scene.frontBuffer(),((ChainSolver) solvers.get(i)).bestAvoidPosition(), color(255, 0, 0), 6);
                }
                if(((ChainSolver) solvers.get(i)).afterAvoidPosition() != null && show1){
                    Util.drawPositions(scene.frontBuffer(),((ChainSolver) solvers.get(i)).afterAvoidPosition(), color(0, 255, 0), 6);
                }

            }
        }

        scene.render();
        scene.beginHUD();
        for(int  i = 0; i < solvers.size(); i++) {
            Util.printInfo(scene, solvers.get(i), structures.get(i).get(0).position());
        }
        scene.endHUD();
    }

    public Frame generateRandomReachablePosition(List<? extends Frame> original, boolean is3D){
        ArrayList<? extends Frame> chain = Util.copy(original);
        for(int i = 0; i < chain.size(); i++){
            if(is3D)
                chain.get(i).rotate(new Quaternion(Vector.random(), (float)(random.nextGaussian()*random.nextFloat()*PI/2)));
            else
                chain.get(i).rotate(new Quaternion(new Vector(0,0,1), (float)(random.nextGaussian()*random.nextFloat()*PI/2)));
        }
        return chain.get(chain.size()-1);
    }

    public void keyPressed(){
        if(key == 'w' || key == 'W'){
            solve = !solve;
        }
        if(key == 's' || key == 'S'){
            Frame f = generateRandomReachablePosition(structures.get(0), scene.is3D());
            Vector delta = Vector.subtract(f.position(), targets.get(0).position());
            for(Frame target : targets)
                target.setPosition(Vector.add(target.position(), delta));
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
            for(Solver s: solvers) s.solve();
        }
        // /*

        if(key == '1'){
            show1 = !show1;
        }
        if(key == '2'){
            show2 = !show2;
        }
        if(key == '3'){
            show3 = !show3;
        }
        if(key == '4'){
            show4 = !show4;
        }
        if(key == '5'){
            show5 = !show5;
        }
        if(key == '6'){
            FABRIKSolver.rand = !FABRIKSolver.rand;
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
        PApplet.main(new String[]{"ik.basic.VisualBenchmark"});
    }
}
