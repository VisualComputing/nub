package ik.mocap;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.FABRIKSolver;
import frames.ik.Solver;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.FixedConstraint;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import ik.interactiveSkeleton.InteractiveJoint;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sebchaparr on 23/03/18.
 */
public class Viewer extends PApplet {
    Scene scene;
    Node eye;
    String path = "/testing/data/bvh/walk-03-sneak-yokoyama.bvh";
    BVHParser parser;
    HashMap<String,Node> originalLimbs = new HashMap<String, Node>();
    HashMap<String,Joint> limbs = new HashMap<String, Joint>();
    HashMap<String,Target> targets = new HashMap<String, Target>();
    Node root, rootIK;

    CCDSolver ccd_solver;
    ChainSolver chain_solver;
    Solver solver;
    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);

        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultGrabber(eye);
        scene.fitBallInterpolation();

        targets.put("LEFTHAND", new Target(scene));
        targets.put("RIGHTHAND", new Target(scene));
        targets.put("LEFTFOOT", new Target(scene));
        targets.put("RIGHTFOOT", new Target(scene));
        targets.put("HEAD", new Target(scene));


        parser = new BVHParser(Joint.class, sketchPath() + path, scene, null);
        root = parser.root();
        ((Joint) root).setRoot(true);

        BallAndSocket constraint = new BallAndSocket(radians(10), radians(10), radians(10), radians(10));
        Vector twist = root.children().get(0).translation().get();
        constraint.setRestRotation(root.rotation().get(), new Vector(0, 1, 0), twist);
        //root.setConstraint(constraint);

        parser.constraintJoints();
        //root.setConstraint(null);
        //root.setConstraint(constraint);


        //make a copy of the skeleton
        rootIK = (Joint) copy(scene.branch(root));
        //rootIK.translate(50, 50, 50);
        //rootIK.setConstraint(null);

        //Solver solver = scene.registerTreeSolver(rootIK);//limbs.get("RIGHTUPLEG"));
        //limbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());
        //originalLimbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());

        ArrayList<Node> list = scene.branch(limbs.get("RIGHTCOLLAR"));
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());

        //chain_solver = new ChainSolver(list);
        ArrayList<Node> list2 = scene.branch(originalLimbs.get("RIGHTCOLLAR"));
        list2.add(0, list2.get(0).reference());
        list2.add(0, list2.get(0).reference());
        //list2.add(0, list2.get(0).reference());
        solver = scene.registerTreeSolver(limbs.get("RIGHTUPLEG").reference());
        //Solver solver = scene.registerTreeSolver(limbs.get("RIGHTUPLEG").reference());

        chain_solver = new ChainSolver(list2);
        for(Frame f : rootIK.children()) {
            //f.setConstraint(new FixedConstraint());
            //limbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());
        }

        ((FABRIKSolver) solver).pg = scene.pApplet().getGraphics();
        chain_solver.pg = scene.pApplet().getGraphics();
        scene.unregisterTreeSolver(limbs.get("RIGHTUPLEG").reference());

        //ccd_solver = new CCDSolver(scene.branch(originalLimbs.get("RIGHTUPLEG")));
        ccd_solver = new CCDSolver(list);
        ccd_solver.timesPerFrame = 20;
        chain_solver.timesPerFrame = 20;
        chain_solver.error = 0.1f;
        solver.timesPerFrame = 0.5f;
        solver.error = 1f;
        ccd_solver.error = 0.1f;
        //scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        //chain_solver.setTarget(targets.get("RIGHTFOOT"));
        scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        //chain_solver.setTarget(targets.get("RIGHTFOOT"));
        chain_solver.setTarget(targets.get("RIGHTHAND"));
        ccd_solver.setTarget(targets.get("RIGHTHAND"));
        //scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));

    }

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        for (Node frame : scene.nodes()) {
            if (frame instanceof Shape) ((Shape) frame).draw();
        }
        if(read){
            parser.nextPose();
            //updateTargets();
        }
        parser.drawFeasibleRegion(this.getGraphics());
        //parser.drawConstraint(this.getGraphics());
        if(chain_solver != null) {
            if(show1) draw_pos(prev, color(0,255,0), 3);
            if(show2) draw_pos(chain_solver.get_p(), color(255,0,100), 3);
            if(show3) draw_pos(constr, color(100,100,0), 3);
        }

        if(solve) {
            ccd_solver.solve();
            chain_solver.solve();
        }
    }

    public Node copy(ArrayList<Node> branch) {
        ArrayList<Node> copy = new ArrayList<Node>();
        Node reference = branch.get(0).reference();
        HashMap<Node, Node> map = new HashMap<Node, Node>();
        map.put(branch.get(0), reference);
        for (Node joint : branch) {
            Joint newJoint = new Joint(scene);
            newJoint.setReference(map.get(joint));
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            //it's no too efficient but it is just executed once
            for (Node child : joint.children()) {
                if(joint.children().size() > 1) {
                    //add a new joint per child
                    Node dummy = new Node(scene);
                    dummy.setReference(newJoint);
                    dummy.setConstraint(newJoint.constraint());
                    dummy.setPosition(newJoint.position().get());
                    dummy.setOrientation(newJoint.orientation().get());
                    //scene.inputHandler().removeGrabber(dummy);
                    copy.add(dummy);
                    map.put(child, dummy);
                }else{
                    map.put(child, newJoint);
                }
            }
            if(parser._joint.get(joint)._name.equals("LEFTHAND")){
                originalLimbs.put("LEFTHAND", joint);
                limbs.put("LEFTHAND", newJoint);
                targets.get("LEFTHAND").setPosition(newJoint.position());
            } else if(parser._joint.get(joint)._name.equals("RIGHTHAND")){
                originalLimbs.put("RIGHTHAND", joint);
                limbs.put("RIGHTHAND", newJoint);
                targets.get("RIGHTHAND").setPosition(newJoint.position());
            } else if(parser._joint.get(joint)._name.equals("LEFTFOOT")){
                originalLimbs.put("LEFTFOOT", joint);
                limbs.put("LEFTFOOT", newJoint);
                targets.get("LEFTFOOT").setPosition(newJoint.position());
            } else if(parser._joint.get(joint)._name.equals("RIGHTFOOT")){
                originalLimbs.put("RIGHTFOOT", joint);
                limbs.put("RIGHTFOOT", newJoint);
                targets.get("RIGHTFOOT").setPosition(newJoint.position());
            } else if(parser._joint.get(joint)._name.equals("HEAD")){
                originalLimbs.put("HEAD", joint);
                limbs.put("HEAD", newJoint);
                targets.get("HEAD").setPosition(newJoint.position());
            }
            originalLimbs.put(parser._joint.get(joint)._name, joint);
            limbs.put(parser._joint.get(joint)._name, newJoint);
        }
        ((Joint) copy.get(0)).setRoot(true);
        return copy.get(0);
    }

    public void updateTargets() {
        rootIK.setPosition(root.position());
        for (Map.Entry<String, Node> entry : originalLimbs.entrySet()) {
            targets.get(entry.getKey()).setPosition(entry.getValue().position());
       }
    }

    boolean read = false;
    boolean solve = false;
    Node n = null;
    float d = 5;
    boolean show1 = true, show2 = true, show3 = true;

    ArrayList<Vector> prev = new ArrayList<Vector>();
    ArrayList<Vector> constr = new ArrayList<Vector>();

    public void keyPressed(){
        if(scene.mouse().trackedGrabber() != null) {
            n =  (Node) scene.mouse().trackedGrabber();
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
            //chain_solver.solve();
            //chain_solver._iterate(scene.frontBuffer());
        }
        if(key == 'j' || key == 'J'){
            chain_solver.forward();
            prev = copy_p(chain_solver.get_p());
            constr = copy_p(prev);
        }
        if(key == 'k' || key == 'K'){
            chain_solver.backward();
        }
        if(key == 'l' || key == 'L'){
            for (int i = 0; i < chain_solver.chain().size() - 1; i++) {
                constr.set(i+1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
            }
        }
        if(key == 'n' || key == 'N'){
            int i = 0;
            constr = copy_p(prev);
            constr.set(0,chain_solver.chain().get(0).position());
            constr.set(i+1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
        }
        if(key == 'm' || key == 'M'){
            int i = 1;
            constr.set(i+1, chain_solver._constrainBackwardReaching(chain_solver.chain(), i));
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

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.mocap.Viewer"});
    }

}