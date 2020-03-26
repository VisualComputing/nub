package ik.trik;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Interpolator;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.TRIKTree;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MultipleLimbs extends PApplet {

    int numJoints = 10;
    float boneLength = 100;
    float radius = 10;
    Scene scene;
    Node target;
    Interpolator pathInterpolator;

    public void settings(){
        size(700, 700, P3D);
    }

    public void setup(){
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(boneLength * numJoints);
        scene.fit();
        //Create a kinematic chain
        createSkeleton(null, 0,255,0, radius, boneLength, numJoints);
    }

    public void draw(){
        background(0);
        lights();
        scene.render();
    }


    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
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
        PApplet.main(new String[]{"ik.trik.MultipleLimbs"});
    }


    public TRIKTree createSolver(Node root, float radius){
        TRIKTree solver = new TRIKTree(root, SimpleTRIK.HeuristicMode.FINAL);
        solver.setTimesPerFrame(10);
        solver.setMaxIterations(10);
        solver.setChainTimesPerFrame(15);
        solver.setChainMaxIterations(15);
        solver.setMaxError(20);
        for(Node node : Scene.branch(root)){
            if(node.children() == null || node.children().isEmpty()){
                node.enableTagging(false);
                //Add as target
                Node target = Util.createTarget(scene, radius * 1.2f);
                target.setPosition(node.position());
                target.setOrientation(node.orientation());
                solver.addTarget(node, target);
            }
        }

        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                solver.solve();
            }
        };
        task.run(40);
        return solver;
    }

    public HashMap<String, Node> createSkeleton(Node reference, int red, int green, int blue, float radius, float boneLength, int numJoints){
        HashMap<String, Node> skeleton = new HashMap<>();
        //Create root
        Joint root = new Joint(red, green, blue, radius);
        root.setReference(reference);
        root.setConstraint(new Constraint() {
            @Override
            public Vector constrainTranslation(Vector translation, Node node) {
                return new Vector();
            }

            @Override
            public Quaternion constrainRotation(Quaternion rotation, Node node) {
                return new Quaternion();
            }
        });
        root.setRoot(true);
        skeleton.put("ROOT", root);

        //create lower limbs
        createLimb("LOWER_LEFT_1", numJoints, red, green, blue, radius, boneLength,
                root, Vector.multiply(new Vector(-1,1,0),boneLength), new Vector(0,1,0), skeleton);
        createLimb("LOWER_RIGHT_1", numJoints, red, green, blue, radius, boneLength,
                root, Vector.multiply(new Vector(1,1,0),boneLength), new Vector(0,1,0), skeleton);

        createSolver(root, radius);

        return skeleton;
    }

    public void createLimb(String name, int joints, int red, int green, int blue, float radius, float boneLength, Node reference, Vector translation, Vector direction, HashMap<String, Node> skeleton){
        Vector v = direction.normalize(null);
        v.multiply(boneLength);

        Joint prev = new Joint(red, green, blue, radius);
        prev.setReference(reference);
        prev.translate(translation);
        skeleton.put(name , prev);

        for(int i = 0; i < joints; i++){
            Joint next = new Joint(red, green, blue, radius);
            next.setReference(prev);
            next.translate(v);
            skeleton.put(name + i , next);

            //add a constraint to prev node
            Hinge hinge = new Hinge((float) Math.toRadians(5), (float) Math.toRadians(50),
                    prev.rotation().get(), v, new Vector(1,0,0));
            prev.setConstraint(hinge);
            prev = next;
        }
    }

}
