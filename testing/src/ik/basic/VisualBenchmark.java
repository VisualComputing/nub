package ik.basic;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.Constraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.HAEASolver;
import frames.ik.evolution.BioIk;
import frames.ik.evolution.GASolver;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.Solver;
import frames.ik.jacobian.PseudoInverseSolver;
import frames.ik.jacobian.SDLSSolver;
import frames.ik.jacobian.TransposeSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
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
    enum ConstraintType{ NONE, HINGE, CONE_POLYGON, CONE_ELLIPSE, MIX }

    ConstraintType mode = ConstraintType.MIX; //Choose an option
    boolean is3D = true;

    int num_joints = 15;
    float targetRadius = 30;
    float boneLength = 50;

    Random random = new Random();

    Scene scene;
    //Methods
    int num_solvers = 7;
    ArrayList<Solver> solvers;
    ArrayList<ArrayList<Frame>> structures = new ArrayList<>();
    ArrayList<Frame> targets = new ArrayList<Frame>();

    public void settings() {
        size(1500, 800, is3D ? P3D : P2D);
    }

    public void setup() {
        scene = new Scene(this);
        if(is3D) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(num_joints * 1f * boneLength);
        scene.fit(1);
        scene.setRightHanded();

        for(int i = 0; i < num_solvers; i++) {
            PShape redBall;
            if(is3D)
                 redBall = createShape(SPHERE, targetRadius);
            else
                redBall = createShape(ELLIPSE, 0,0, targetRadius, targetRadius);
            redBall.setStroke(false);
            redBall.setFill(color(255,0,0));
            Frame target = new Frame(scene, redBall);
            target.setPickingThreshold(0);
            targets.add(target);
        }

        float alpha = 1.f * width / height > 1.5f ? 0.5f * width / height : 0.5f;
        alpha *= num_solvers/4f; //avoid undesirable overlapping

        for(int i = 0; i < num_solvers; i++){
            int color = color(random(255), random(255), random(255));
            structures.add(generateChain(num_joints, boneLength, new Vector(i * 2 * alpha * scene.radius()/(num_solvers - 1) - alpha * scene.radius(), 0, 0), color));
        }

        for (int i = 0; i < num_joints - 1; i++) {
            Vector twist = structures.get(0).get(i + 1).translation().get();
            //Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(random(-90, 90)));
            Quaternion offset = Quaternion.random();
            Constraint constraint = null;
            ConstraintType mode = this.mode;
            if(this.mode == ConstraintType.MIX){
                int r = random.nextInt(ConstraintType.values().length);
                r = is3D ? r : r % 2;
                mode = ConstraintType.values()[r];
            }
            switch (mode){
                case NONE:{
                    break;
                }
                case CONE_ELLIPSE:{
                    if(!is3D) break;
                    float down = radians(random(10, 40));
                    float up = radians(random(10, 40));
                    float left = radians(random(10, 40));
                    float right = radians(random(10, 40));
                    constraint = new BallAndSocket(down, up, left, right);
                    Quaternion rest = Quaternion.compose(structures.get(0).get(i).rotation().get(), offset);
                    ((BallAndSocket) constraint).setRestRotation(rest, new Vector(0, 1, 0), twist);
                    break;
                }
                case CONE_POLYGON:{
                    if(!is3D) break;
                    ArrayList<Vector> vertices = new ArrayList<Vector>();
                    float v = 20;
                    float w = 20;

                    vertices.add(new Vector(-w, -v));
                    vertices.add(new Vector(w, -v));
                    vertices.add(new Vector(w, v));
                    vertices.add(new Vector(-w, v));

                    constraint = new PlanarPolygon(vertices);
                    Quaternion rest = Quaternion.compose(structures.get(0).get(i).rotation().get(), offset);
                    ((PlanarPolygon) constraint).setRestRotation(rest, new Vector(0, 1, 0), twist);
                    ((PlanarPolygon) constraint).setAngle(radians(random(30, 50)));
                    break;
                }
                case HINGE:{
                    constraint = new Hinge(radians(random(10, 170)), radians(random(10, 170)));
                    ((Hinge) constraint).setRestRotation(structures.get(0).get(i).rotation().get());
                    ((Hinge) constraint).setAxis(Vector.projectVectorOnPlane(Vector.random(), structures.get(0).get(i + 1).translation()));
                    if(Vector.squaredNorm(((Hinge) constraint).axis()) == 0) {
                        constraint = null;
                    }
                }
            }
            for(ArrayList<Frame> structure : structures){
               structure.get(i).setConstraint(constraint);
            }
        }


        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        solvers = new ArrayList<>();

        int idx = 0;
        solvers.add(new HillClimbingSolver(5, radians(5), structures.get(idx++)));
        solvers.add(new CCDSolver(structures.get(idx++)));
        solvers.add(new ChainSolver(structures.get(idx++)));
        solvers.add(new GASolver(structures.get(idx++), 12));
        //solvers.add(new HAEASolver(structures.get(4), 12, true));
        //solvers.add(new TransposeSolver(structures.get(5)));
        solvers.add(new PseudoInverseSolver(structures.get(idx++)));
        solvers.add(new SDLSSolver(structures.get(idx++)));
        solvers.add(new BioIk(structures.get(idx++),12, 4 ));

        for(int i = 0; i < solvers.size(); i++){
            solvers.get(i).error = 0.5f;
            solvers.get(i).timesPerFrame = 1;
            solvers.get(i).maxIter = 50;
            if(i != 0)targets.get(i).setReference(targets.get(0));
            if(solvers.get(i) instanceof HillClimbingSolver) {
                ((HillClimbingSolver) solvers.get(i)).setTarget(targets.get(i));
                targets.get(i).setPosition( ((HillClimbingSolver) solvers.get(i)).endEffector().position());
            }
            if(solvers.get(i) instanceof ChainSolver) {
                ((ChainSolver) solvers.get(i)).setTarget(targets.get(i));
                targets.get(i).setPosition( ((ChainSolver) solvers.get(i)).endEffector().position());
            }
            if(solvers.get(i) instanceof CCDSolver) {
                ((CCDSolver) solvers.get(i)).setTarget(targets.get(i));
                targets.get(i).setPosition( ((CCDSolver) solvers.get(i)).endEffector().position());
            }
            if(solvers.get(i) instanceof GASolver) {
                GASolver solver = (GASolver) solvers.get(i);
                solver.setTarget(solver.endEffector(), targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
            if(solvers.get(i) instanceof HAEASolver) {
                HAEASolver solver = (HAEASolver) solvers.get(i);
                solver.setTarget(solver.endEffector(), targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
            if(solvers.get(i) instanceof BioIk) {
                BioIk solver = (BioIk) solvers.get(i);
                solver.setTarget(solver.endEffector(), targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
            if(solvers.get(i) instanceof TransposeSolver) {
                TransposeSolver solver = (TransposeSolver) solvers.get(i);
                solver.setTarget(targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
            if(solvers.get(i) instanceof PseudoInverseSolver) {
                PseudoInverseSolver solver = (PseudoInverseSolver) solvers.get(i);
                solver.setTarget(targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
            if(solvers.get(i) instanceof SDLSSolver) {
                SDLSSolver solver = (SDLSSolver) solvers.get(i);
                solver.setTarget(targets.get(i));
                targets.get(i).setPosition(solver.endEffector().position());
            }
        }
    }

    public void draw() {
        background(0);
        if(is3D) lights();
        //Draw Constraints
        scene.drawAxes();
        if(solve) {
            for(Solver solver : solvers){
                //if(solver instanceof TransposeSolver)
                    solver.solve();
            }
        }
        scene.render();
        scene.beginHUD();
        for(Solver solver : solvers) {
            fill(255);
            textSize(10);
            if (solver instanceof HillClimbingSolver) {
                HillClimbingSolver s = (HillClimbingSolver) solver;
                Frame f = s.chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                if(s.powerLaw()){
                    text("Power Law  \n Sigma: " + String.format( "%.2f", s.sigma()) + "\n Alpha: " + String.format( "%.2f", s.alpha()) + "\n Error: " + String.format( "%.2f", s.distanceToTarget()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
                } else{
                    text("Gaussian  \n Sigma: " + String.format( "%.2f", s.sigma()) + "\n Error: " + String.format( "%.2f", s.distanceToTarget()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
                }
            } else if (solver instanceof ChainSolver) {
                Frame f = ((ChainSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("FABRIK", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  GASolver){
                Frame f = ((GASolver)solver).structure().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("Genetic \n Algorithm" + "\n Error: " + String.format( "%.2f", ((GASolver)solver).best()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  HAEASolver){
                Frame f = ((HAEASolver)solver).structure().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("HAEA \n Algorithm" + "\n Error: " + String.format( "%.2f", ((HAEASolver)solver).best()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  BioIk){
                Frame f = ((BioIk)solver).structure().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("BioIK \n Algorithm" + "\n Error: " + String.format( "%.2f", ((BioIk)solver).best()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  TransposeSolver){
                Frame f = ((TransposeSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("Transpose", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  PseudoInverseSolver){
                Frame f = ((PseudoInverseSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("PseudoInv", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  SDLSSolver){
                Frame f = ((SDLSSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("SDLS", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if (solver instanceof CCDSolver) {
                Frame f = ((CCDSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("CCD", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            }
        }
        scene.endHUD();
    }

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
    }

    public ArrayList<Frame> generateChain(int num_joints, float boneLength, Vector translation, int color) {
        Random rand = new Random(0);
        Joint prevJoint = null;
        Joint chainRoot = null;
        for (int i = 0; i < num_joints; i++) {
            Joint joint;
            joint = new Joint(scene, color, targetRadius*0.3f);
            if (i == 0)
                chainRoot = joint;
            if (prevJoint != null) joint.setReference(prevJoint);
            float x = 2*rand.nextFloat() - 1;
            float z = rand.nextFloat();
            float y = 2 * rand.nextFloat() - 1;
            Vector translate = new Vector(x,y,z);
            translate.normalize();
            translate.multiply(boneLength * (1 - 0.4f*rand.nextFloat()));
            joint.setTranslation(translate);
            prevJoint = joint;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        //chainRoot.setupHierarchy();
        chainRoot.setRoot(true);
        return (ArrayList) scene.branch(chainRoot);
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
            targets.get(0).setPosition(f.position());
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
        PApplet.main(new String[]{"ik.basic.VisualBenchmark"});
    }
}
