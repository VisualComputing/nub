package ik.constraintTest;

import nub.core.Graph;
import nub.ik.CCDSolver;
import nub.ik.ChainSolver;
import nub.ik.Solver;
import nub.core.Node;
import nub.ik.evolution.BioIk;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.core.constraint.PlanarPolygon;
import nub.processing.Scene;
import nub.timing.TimingTask;
import ik.basic.Util;
import processing.core.PApplet;
import processing.event.MouseEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by sebchaparr on 8/07/18.
 */
public class ConePolygon extends PApplet{
    //TODO : Validate constraints in extreme cases (i.e Cone angle near to PI/2)
    int numJoints = 10;
    float targetRadius = 7;
    float boneLength = 50;
    boolean solve = false;

    //Scene Parameters
    Scene scene;
    //Benchmark Parameters
    Random random = new Random();
    ArrayList<Solver> solvers; //Will store Solvers
    ArrayList<ArrayList<Node>> structures = new ArrayList<>(); //Keep Structures
    ArrayList<Node> targets = new ArrayList<Node>(); //Keep targets

    int randRotation = -1; //Set seed to generate initial random rotations, otherwise set to -1
    int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1
    int numSolvers = 6; //Set number of solvers

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength);
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
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        float v = 20;
        float w = 20;

        vertices.add(new Vector(-w, -v));
        vertices.add(new Vector(w, -v));
        vertices.add(new Vector(w, v));
        vertices.add(new Vector(-w, v));

        ArrayList<Node> structure1;
        ArrayList<Node> structure2;
        ArrayList<Node> structure3;

        for(ArrayList<Node> structure : structures){
            for (int i = 0; i < structure.size() - 1; i++) {
                PlanarPolygon constraint = new PlanarPolygon(vertices);
                Vector twist = structure.get(i + 1).translation().get();
                Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(40));
                //offset = new Quaternion();
                Quaternion rest = Quaternion.compose(structure.get(i).rotation().get(), offset);
                constraint.setRestRotation(rest, new Vector(0, 1, 0), twist);
                constraint.setAngle(PI/3f);
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
            solver.setMaxError(0.001f);
            solver.setTimesPerFrame(5);
            solver.setMaxIterations(200);
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
        if(key== 'Q' || key == 'q'){
            for(Solver s : solvers) {
                s.solve();
            }
        }
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
            if(targets.contains(scene.trackedNode())){
                for(Node target : targets) scene.translate(target);
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
        PApplet.main(new String[]{"ik.constraintTest.ConePolygon"});
    }

}
