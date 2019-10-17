package ik.constraintTest;


import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.core.constraint.Hinge;
import nub.processing.Scene;
import nub.processing.TimingTask;
import nub.ik.visual.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 8/07/18.
 */
public class HingeTest extends PApplet {
    int numJoints = 8;
    float min = 180;
    float max = 180;
    float targetRadius = 10;
    float boneLength = 50;
    boolean solve = true;

    Scene scene;
    ArrayList<CCDSolver> ccdSolvers = new ArrayList<CCDSolver>();
    ArrayList<ChainSolver> chainSolvers = new ArrayList<ChainSolver>();
    ArrayList<Node> targets = new ArrayList<Node>();

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(numJoints * boneLength);
        scene.fit(1);
        scene.setRightHanded();

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        for(int i = 0; i < 5; i++) {
            Node target = new Node(scene, redBall);
            target.setPickingThreshold(0);
            targets.add(target);
        }

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        ArrayList<Node> branchHingeConstraint = generateChain(numJoints, boneLength, new Vector(-scene.radius(), -scene.radius(), 0));
        ArrayList<Node> branchHingeConstraintCCD = generateChain(numJoints, boneLength, new Vector(-scene.radius(), -scene.radius(), 0));

        for (int i = 0; i < branchHingeConstraint.size() - 1; i++) {
            Vector vector = Vector.projectVectorOnPlane(new Vector(0, 1, 0), branchHingeConstraint.get(i + 1).translation());
            if(Vector.squaredNorm(vector) != 0) {
                Hinge constraint = new Hinge(radians(min), radians(max),branchHingeConstraint.get(i).rotation().get(), new Vector(0,1,0), vector);
                branchHingeConstraint.get(i).setConstraint(constraint);
                branchHingeConstraintCCD.get(i).setConstraint(constraint);
            }
        }

        ArrayList<Node> structure1 = generateStructure(boneLength,new Vector(-boneLength,0,0));
        chainSolvers.add(new ChainSolver(structure1));

        ArrayList<Node> structure2 = generateStructure(boneLength,new Vector(0,0,0));
        ccdSolvers.add(new CCDSolver(structure2));
        ccdSolvers.add(new CCDSolver(branchHingeConstraintCCD));

        ArrayList<Node> structure3 = generateStructure(boneLength,new Vector(boneLength,0,0));
        chainSolvers.add(new ChainSolver(structure3));
        chainSolvers.add(new ChainSolver(branchHingeConstraint));


        int i = 0;
        for(ChainSolver s : chainSolvers){
            //s._timesPerFrame = 20;
            s.setMaxError(0.5f);
            s.setTimesPerFrame(0.5f);
            s.setMaxIterations(30);
            //s.setMaxError(1f);
            s.setTarget(targets.get(i));
            if(i != 0)targets.get(i).setReference(targets.get(0));
            targets.get(i++).setPosition(s.endEffector().position());
            TimingTask task = new TimingTask(scene) {
                @Override
                public void execute() {
                    if(solve) s.solve();
                }
            };
            task.run(40);
        }

        for(CCDSolver s : ccdSolvers){
            s.setMaxError(0.5f);
            s.setTimesPerFrame(0.5f);
            s.setMaxIterations(30);
            s.setTarget(targets.get(i));
            targets.get(i).setReference(targets.get(0));
            targets.get(i++).setPosition(s.endEffector().position());
            TimingTask task = new TimingTask(scene) {
                @Override
                public void execute() {
                    if(solve) s.solve();
                }
            };
            task.run(40);
        }
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
    }

    public void setConstraint(Node f, Vector up, Vector twist){
        Hinge constraint = new Hinge(radians(45), radians(45),f.rotation().get(), up, twist.get());
        f.setConstraint(constraint);
    }

    public ArrayList<Node> generateStructure(float boneLength, Vector o){
        Joint prev = new Joint(scene);
        Joint current = prev;
        Joint root = current;
        root.setRoot(true);
        //current.setRotation(Quaternion._random());
        current = new Joint(scene);
        current.setReference(prev);
        prev = current;
        //current.setRotation(Quaternion._random());
        current.setPosition(0,boneLength,0);
        setConstraint(current, new Vector(0,1,0), new Vector(0,0,1));
        current = new Joint(scene);
        current.setReference(prev);
        prev = current;
        //current.setRotation(Quaternion._random());
        current.setPosition(0,boneLength*2,0);
        setConstraint(current, new Vector(0,0,1), new Vector(1,0,0));
        current = new Joint(scene);
        current.setReference(prev);
        //current.setRotation(Quaternion._random());
        current.setPosition(0,boneLength*2,boneLength*2);
        //setConstraint(current, new Vector(1,0,0));
        root.setPosition(o);
        return (ArrayList) scene.branch(root);
    }

    public ArrayList<Node> generateChain(int num_joints, float boneLength, Vector translation) {
        Joint prevJoint = null;
        Joint chainRoot = null;
        for (int i = 0; i < num_joints; i++) {
            Joint joint;
            joint = new Joint(scene);
            if (i == 0)
                chainRoot = joint;
            if (prevJoint != null) joint.setReference(prevJoint);
            float x = i % 2 == 0 ? 1 : 0;
            float z = (i + 1) % 2 == 0 ? 1 : 0;
            float y = 0;
            Vector translate = new Vector(x,y,z);
            translate.normalize();
            translate.multiply(boneLength);
            joint.setTranslation(translate);
            prevJoint = joint;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        //chainRoot.setupHierarchy();
        chainRoot.setRoot(true);
        return (ArrayList) scene.branch(chainRoot);
    }

    ArrayList<Vector> prev = new ArrayList<Vector>();
    ArrayList<Vector> constr = new ArrayList<Vector>();

    public void keyPressed(){
        if(key == 's' || key == 'S'){
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
        PApplet.main(new String[]{"ik.constraintTest.HingeTest"});
    }
}
