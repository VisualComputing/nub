package ik.constraintTest;

import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.core.Frame;
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
 * Created by sebchaparr on 8/07/18.
 */
public class ConeBall extends PApplet{
    //TODO : Update
    int num_joints = 15;
    float targetRadius = 7;
    float boneLength = 50;

    Scene scene;
    CCDSolver ccd_solver;
    ArrayList<ChainSolver> chain_solvers = new ArrayList<ChainSolver>();
    ArrayList<Shape> targets = new ArrayList<Shape>();

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(num_joints * boneLength / 1.5f);
        scene.fit(1);
        scene.disableBackBuffer();


        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));
        targets.add(new Shape(scene, redBall));

        float down = PI/3;
        float up = PI/3;
        float left = PI/3;
        float right = PI/3;

        ArrayList<Frame> structure1;
        ArrayList<Frame> structure2;
        ArrayList<Frame> structure3;


        structure1 = generateChain(num_joints, boneLength, new Vector(-scene.radius()/2.f, 0, 0));
        structure2 = generateChain(num_joints, boneLength, new Vector(0, 0, 0));
        structure3 = generateChain(num_joints, boneLength, new Vector(scene.radius()/2.f, 0, 0));

        for (int i = 1; i < structure1.size() - 1; i++) {
            Vector twist = structure1.get(i + 1).translation().get();
            BallAndSocket constraint = new BallAndSocket(down, up, left, right);
            constraint.setRestRotation(structure1.get(i).rotation().get(), new Vector(0, 1, 0), twist);
            structure1.get(i).setConstraint(constraint);
            structure2.get(i).setConstraint(constraint);
            structure3.get(i).setConstraint(constraint);
        }



        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2.f));
        scene.eye().rotate(new Quaternion(new Vector(0,1,0), PI));

        //structure1 = generateStructure(boneLength,new Vector(-boneLength,0,0));
        chain_solvers.add(new ChainSolver(structure1));

        //structure2 = generateStructure(boneLength,new Vector(0,0,0));
        ccd_solver = new CCDSolver(structure2);

        //structure3 = generateStructure(boneLength,new Vector(boneLength,0,0));
        chain_solvers.add(new ChainSolver(structure3));
        chain_solvers.get(1).opt = 1;

        //((FABRIKSolver) solver).pg = scene.pApplet().getGraphics();
        int i = 0;
        for(ChainSolver s : chain_solvers){
            s.pg = scene.pApplet().getGraphics();
            //s.timesPerFrame = 20;
            s.error = 0.5f;
            s.timesPerFrame = 0.5f;
            s.maxIter = 30;
            //s.error = 1f;
            s.setTarget(targets.get(i));
            if(i != 0)targets.get(i).setReference(targets.get(0));
            targets.get(i++).setPosition(s.endEffector().position());
        }

        ccd_solver.timesPerFrame = 0.5f;
        ccd_solver.error = 0.5f;

        ccd_solver.setTarget(targets.get(targets.size()-1));
        //scene.addIKTarget(structure3.get(structure3.size()-1), targets.get(0));
        targets.get(targets.size()-1).setReference(targets.get(0));
        targets.get(targets.size()-1).setPosition(structure2.get(structure3.size()-1).position());
    }

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        for(ChainSolver chain_solver : chain_solvers){
            if(show1) draw_pos(prev, color(0,255,0), 3);
            if(show2) draw_pos(chain_solver.get_p(), color(255,0,100), 3);
            if(show3) draw_pos(constr, color(100,100,0), 3);
        }
        if(solve) {
            ccd_solver.solve();
            for(ChainSolver chain_solver : chain_solvers) chain_solver.solve();
        }
        scene.traverse();

    }

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
    }

    public ArrayList<Frame> generateStructure(float boneLength, Vector o){
        float down = PI/2;
        float up = PI/2;
        float left = PI/2;
        float right = PI/2;

        Joint prev = new Joint(scene);
        Joint current = prev;
        Joint root = current;
        root.setRoot(true);
        current.setRotation(Quaternion.random());
        current = new Joint(scene);
        current.setReference(prev);
        prev = current;
        current.setRotation(Quaternion.random());
        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength,0);
        setConstraint(down,up, left, right,current, new Vector(0,boneLength,0),boneLength);

        current = new Joint(scene);
        current.setReference(prev);
        prev = current;
        current.setRotation(Quaternion.random());
        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength*2,0);
        setConstraint(down,up, left, right,current, new Vector(0,0,1),boneLength);

        current = new Joint(scene);
        current.setReference(prev);
        current.setRotation(Quaternion.random());

        //current.setRotation(Quaternion.random());
        current.setPosition(0,boneLength*2,boneLength*2);
        setConstraint(down,up, left, right,current, new Vector(0,0,1),boneLength);

        root.setPosition(o);

        return (ArrayList) scene.branch(root);
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

    boolean read = false;
    boolean solve = false;
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
        if(key == ' ') {
            read = !read;
        }
        if(key== 'Q' || key == 'q'){
            ccd_solver.solve();
            for(Solver s : chain_solvers) {
                s.solve();
            }

            //chain_solver._iterate(scene.frontBuffer());
        }
        if(key == 'j' || key == 'J'){
            for(ChainSolver chain_solver : chain_solvers) {
                chain_solver.forward();
                prev = copy_p(chain_solver.get_p());
                constr = copy_p(prev);
            }
        }
        if(key == 'k' || key == 'K'){
            for(ChainSolver chain_solver : chain_solvers) chain_solver.backward();
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
        PApplet.main(new String[]{"ik.constraintTest.ConeBall"});
    }
}
