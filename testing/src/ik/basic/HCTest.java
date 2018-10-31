package ik.basic;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.PlanarPolygon;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.evolution.GASolver;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class HCTest extends PApplet {
    //TODO : Update
    int num_joints = 4;
    float targetRadius = 12;
    float boneLength = 50;

    Scene scene;
    //Methods
    int num_solvers = 6;
    ArrayList<Solver> solvers;
    ArrayList<Shape> targets = new ArrayList<Shape>();

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(num_joints * boneLength / 1.5f);
        scene.fitBallInterpolation();

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        for(int i = 0; i < num_solvers; i++) {
            targets.add(new Shape(scene, redBall));
        }

        float down = PI/2;
        float up = PI/2;
        float left = PI/2;
        float right = PI/2;

        ArrayList<ArrayList<Frame>> structures = new ArrayList<>();

        for(int i = 0; i < num_solvers; i++){
            structures.add(generateChain(num_joints, boneLength, new Vector(i*2*scene.radius()/num_solvers - 0.8f*scene.radius(), 0, 0)));
        }

        ArrayList<Vector> vertices = new ArrayList<Vector>();
        float v = 20;
        float w = 20;

        vertices.add(new Vector(-w, -v));
        vertices.add(new Vector(w, -v));
        vertices.add(new Vector(w, v));
        vertices.add(new Vector(-w, v));

        for (int i = 0; i < num_joints - 1; i++) {
            PlanarPolygon constraint = new PlanarPolygon(vertices);
            constraint.setHeight(boneLength / 2.f);
            Vector twist = structures.get(0).get(i + 1).translation().get();
            Quaternion offset = new Quaternion(new Vector(0, 1, 0), radians(40));
            offset = new Quaternion();
            Quaternion rest = Quaternion.compose(structures.get(0).get(i).rotation().get(), offset);
            constraint.setRestRotation(rest, new Vector(0, 1, 0), twist);
            constraint.setAngle(PI/3f);
            for(ArrayList<Frame> structure : structures){
                //structure.get(i).setConstraint(constraint);
            }
        }

        /*
        for (int i = 1; i < num_joints - 1; i++) {
            Vector twist = structures.get(0).get(i + 1).translation().get();
            BallAndSocket constraint = new BallAndSocket(down, up, left, right);
            constraint.setRestRotation(structures.get(0).get(i).rotation().get(), new Vector(0, 1, 0), twist);
            for(ArrayList<Frame> structure : structures){
                //structure.get(i).setConstraint(constraint);
            }
        }
        */

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        solvers = new ArrayList<>();

        solvers.add(new HillClimbingSolver(radians(3), structures.get(0)));
        solvers.add(new HillClimbingSolver(5, radians(3), structures.get(1)));
        solvers.add(new HillClimbingSolver(radians(5), structures.get(2)));
        solvers.add(new HillClimbingSolver(5, radians(5), structures.get(3)));
        solvers.add(new ChainSolver(structures.get(4)));
        solvers.add(new GASolver(structures.get(5), 10));
        //solvers.add(new CCDSolver(structures.get(2)));

        for(int i = 0; i < num_solvers; i++){
            solvers.get(i).error = 0.5f;
            solvers.get(i).timesPerFrame = 1;
            solvers.get(i).maxIter = 5;
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
        }
    }

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        if(solve) {
            for(Solver solver : solvers){
                solver.solve();
            }
        }
        scene.traverse();
        scene.beginHUD();
        for(Solver solver : solvers) {
            fill(255);
            textSize(12);
            if (solver instanceof HillClimbingSolver) {
                HillClimbingSolver s = (HillClimbingSolver) solver;
                Frame f = s.chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                if(s.powerLaw()){
                    text("Power Law  \n Sigma: " + String.format( "%.2f", s.sigma()) + "\n Alpha: " + String.format( "%.2f", s.alpha()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
                } else{
                    text("Gaussian  \n Sigma: " + String.format( "%.2f", s.sigma()), pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
                }
            } else if (solver instanceof ChainSolver) {
                Frame f = ((ChainSolver)solver).chain().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("FABRIK", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            } else if(solver instanceof  GASolver){
                Frame f = ((GASolver)solver).structure().get(0);
                Vector pos = scene.screenLocation(f.position());
                text("Genetic Algorithm", pos.x() - 30, pos.y() + 10, pos.x() + 30, pos.y() + 50);
            }
        }
        scene.endHUD();
    }

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
    }

    public ArrayList<Frame> generateChain(int num_joints, float boneLength, Vector translation) {
        Joint prevJoint = null;
        Joint chainRoot = null;
        for (int i = 0; i < num_joints; i++) {
            Joint joint;
            joint = new Joint(scene);
            if (i == 0)
                chainRoot = joint;
            if (prevJoint != null) joint.setReference(prevJoint);
            float x = 0;
            float z = 1;
            float y = 0;
            Vector translate = new Vector(x,y,z);
            translate.normalize();
            translate.multiply(boneLength);
            joint.setTranslation(translate);
            joint.setPrecision(Frame.Precision.FIXED);
            prevJoint = joint;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        //chainRoot.setupHierarchy();
        chainRoot.setRoot(true);
        return (ArrayList) scene.branch(chainRoot);
    }

    boolean solve = false;
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
            scene.translate();
        } else {
            scene.zoom(scene.mouseDX());
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
        PApplet.main(new String[]{"ik.basic.HCTest"});
    }
}
