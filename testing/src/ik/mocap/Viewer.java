package ik.mocap;

import common.InteractiveNode;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
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
        parser.constraintJoints();
        //make a copy of the skeleton
        //rootIK = (Joint) copy(scene.branch(root));
        //rootIK.translate(50, 50, 50);
        /*
        Solver solver = scene.registerTreeSolver(rootIK);
        solver.timesPerFrame = 100;
        solver.error = 0.05f;
        scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));
        */
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
            updateTargets();
        }
        parser.drawFeasibleRegion(this.getGraphics());
        parser.drawConstraint(this.getGraphics());
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
                    dummy.setPosition(newJoint.position());
                    scene.inputHandler().removeGrabber(dummy);
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
    Node n = null;
    public void keyPressed(){
        if(scene.mouse().trackedGrabber() != null) {
            n =  (Node) scene.mouse().trackedGrabber();
            float d = 5;
            if (key == 'X' || key == 'x') {
                n.rotate(Quaternion.multiply(n.reference().rotation().inverse(), new Quaternion(new Vector(1,0,0), radians(d))));
            }
            if (key == 'Y' || key == 'y') {
                n.rotate(Quaternion.multiply(n.reference().rotation().inverse(), new Quaternion(new Vector(0,1,0), radians(d))));
            }
            if (key == 'Z' || key == 'z') {
                n.rotate(Quaternion.multiply(n.reference().rotation().inverse(), new Quaternion(new Vector(0,0,1), radians(d))));
            }
        }
        //read = !read;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.mocap.Viewer"});
    }

}