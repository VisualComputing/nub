package ik.constraintTest;

import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.core.constraint.PlanarPolygon;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.basic.Util;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
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
    ArrayList<ArrayList<Frame>> structures = new ArrayList<>(); //Keep Structures
    ArrayList<Frame> targets = new ArrayList<Frame>(); //Keep targets

    int randRotation = -1; //Set seed to generate initial random rotations, otherwise set to -1
    int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1
    int numSolvers = 5; //Set number of solvers

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

        ArrayList<Frame> structure1;
        ArrayList<Frame> structure2;
        ArrayList<Frame> structure3;

        for(ArrayList<Frame> structure : structures){
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

        for(i = 0; i < solvers.size(); i++){
            Solver solver = solvers.get(i);
            if(solvers.get(i) instanceof ChainSolver){
                ((ChainSolver)solver).pg = scene.pApplet().getGraphics();
            }
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
        for(int  i = 0; i < solvers.size(); i++) {
            if (solvers.get(i) instanceof ChainSolver) {
                if (show1) draw_pos(prev, color(0, 255, 0), 3);
                if (show2) draw_pos(((ChainSolver) solvers.get(i)).get_p(), color(255, 0, 100), 3);
                if (show3) draw_pos(constr, color(100, 100, 0), 3);
            }
        }
        scene.beginHUD();
        for(int  i = 0; i < solvers.size(); i++) {
            Util.printInfo(scene, solvers.get(i), structures.get(i).get(0).position());
        }
        scene.endHUD();
    }

    Frame n = null;
    float d = 5;
    boolean show1 = true, show2 = true, show3 = true;

    ArrayList<Vector> prev = new ArrayList<Vector>();
    ArrayList<Vector> constr = new ArrayList<Vector>();

    public void keyPressed(){
        if(scene.trackedFrame() != null) {
            n =  scene.trackedFrame();
            if(n != null) {
                if (key == 'A' || key == 'a') {
                    d += 1;
                    System.out.println("Speed --- : " + d);
                }
                if (key == 's' || key == 'S') {
                    d *= -1;
                    System.out.println("Speed --- : " + d);
                }

                Frame ref = n.reference() != null ? n.reference() : new Frame();

                if (key == 'X' || key == 'x') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(1, 0, 0), radians(d))));
                }
                if (key == 'Y' || key == 'y') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(0, 1, 0), radians(d))));
                }
                if (key == 'Z' || key == 'z') {
                    n.rotate(Quaternion.multiply(ref.rotation().inverse(), new Quaternion(new Vector(0, 0, 1), radians(d))));
                }

                if (key == 'd' || key == 'D') {
                    n.reference().setConstraint(null);
                }
            }
        }

        if(key== 'Q' || key == 'q'){
            for(Solver s : solvers) {
                s.solve();
            }
        }
        if(key == 'j' || key == 'J'){
            for(Solver s: solvers) {
                if(s instanceof  ChainSolver) {
                    ((ChainSolver) s).forward();
                    prev = copy_p(((ChainSolver) s).get_p());
                    constr = copy_p(prev);
                }
            }
        }
        if(key == 'k' || key == 'K'){
            for(Solver s: solvers) {
                if(s instanceof  ChainSolver) {
                    ((ChainSolver) s).backward();
                }
            }
        }

        if(key == '1'){
            show1 = !show1;
        }
        if(key == '2'){
            show2 = !show2;
        }
        if(key == '3'){
            show3 = !show3;
        }

        if(key == 'w' || key == 'W'){
            solve = !solve;
        }

        if(key == '.'){
            //Apply random perturbation of 15 degrees
            ArrayList<? extends Frame> chain = structures.get(1);
            for(Frame f : chain){
                f.rotate(new Quaternion(Vector.random(), radians(random(0,15))));
            }
            solvers.get(1).iterations = 0;
        }
    }
    ArrayList<Vector> copy_p(ArrayList<Vector> _positions){
        ArrayList<Vector> copy = new ArrayList<Vector>();
        for(Vector p : _positions){
            copy.add(p.get());
        }
        return copy;
    }


    void draw_pos(ArrayList<Vector> _positions, int color, float str) {
        if(_positions == null) return;
        Vector prev = null;
        for(Vector p : _positions){
            pushMatrix();
            pushStyle();
            stroke(color);
            strokeWeight(str);
            if(prev != null) line(prev.x(),prev.y(),prev.z(), p.x(),p.y(),p.z());
            noStroke();
            fill(color, 100);
            translate(p.x(),p.y(),p.z());
            sphere(3);
            popStyle();
            popMatrix();
            prev = p;
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
        PApplet.main(new String[]{"ik.constraintTest.ConePolygon"});
    }

}
