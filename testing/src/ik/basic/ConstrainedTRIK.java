package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.MySolver;
import nub.ik.solver.geometric.oldtrik.TRIK;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 01/06/19.
 */

public class ConstrainedTRIK extends PApplet {
    public enum IKMode{ BIOIK, FABRIK, CCD, MYSOLVER, TRIK};
    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;

    float boneLength = 50;
    float radius = 10;
    int segments = 7;
    List<Node> targets = new ArrayList<Node>();
    boolean debug = true;
    boolean solve = !debug;
    Solver trik, fabrik;



    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        Joint.axes = true;

        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(280);
        scene.fit(1);
        //Create the Skeleton (chain described above)
        trik = createStructure(scene, segments, boneLength, radius, color(0,0,255), new Vector(boneLength*5, 0,0), IKMode.TRIK);
    }

    public void draw() {
        background(0);
        if(scene.is3D()) lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        scene.endHUD();
    }

    public Solver createStructure(Scene scene, int segments, float length, float radius, int color, Vector translation, IKMode mode){
        Node reference = new Node(scene);
        reference.translate(translation);

        //1. Create reference Frame
        Joint root = new Joint(scene, color(255,0,0), radius);
        root.setRoot(true);
        root.setReference(reference);

        //2. Create Targets, Limbs & Solvers
        Node target = createTarget(scene, radius*1.2f);
        return createLimb(scene, segments, length, radius, color, root, target, new Vector(length,length,0), mode);
    }

    public Solver createLimb(Scene scene, int segments, float length, float radius, int color, Node reference, Node target, Vector translation, IKMode mode){
        target.setReference(reference.reference());
        ArrayList<Node> joints = new ArrayList<>();
        Joint root = new Joint(scene, color, radius);
        root.setReference(reference);
        joints.add(root);

        for(int i = 0; i < max(segments, 2); i++){
            Joint middle = new Joint(scene, color, radius);
            middle.setReference(joints.get(i));
            middle.translate(0, length, 0);
            if(i < max(segments, 2) - 1) {
                float max = 3;
                float min = 45;

                Hinge hinge = new Hinge(radians(min), radians(max), middle.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
                middle.setConstraint(hinge);
            }
            joints.add(middle);
        }
        BallAndSocket cone = new BallAndSocket(radians(20),radians(20));
        cone.setRestRotation(joints.get(joints.size() - 1).rotation().get(), new Vector(0,-1,0), new Vector(0,0,1));
        //joints.get(joints.size() - 1).setConstraint(cone);

        Joint low = new Joint(scene, color, radius);
        low.setReference(joints.get(joints.size() - 1));
        low.translate(0,0,length);

        joints.add(low);
        root.translate(translation);
        root.setConstraint(new Hinge(radians(60), radians(60), root.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0)));
        return addIKbehavior(scene, joints, target, mode);
    }

    public Solver addIKbehavior(Scene scene, ArrayList<Node> limb, Node target, IKMode mode){
        Solver solver;
        switch (mode){
            case CCD:{
                solver = new CCDSolver(limb,true);
                ((CCDSolver)solver).setTarget(target);
                break;
            }
            case FABRIK:{
                solver = new ChainSolver(limb);
                ((ChainSolver)solver).setTarget(target);
                ((ChainSolver)solver).setTargetDirection(new Vector(0, 0, 1));
                break;
            }
            case BIOIK:{
                solver = new BioIk(limb, 10,4);
                solver.setTarget(limb.get(limb.size() - 1), target);
                break;
            }
            case MYSOLVER:{
                solver = new nub.ik.solver.geometric.MySolver(limb);
                ((MySolver)solver).setTarget(target);
                break;
            }

            case TRIK:{
                solver = new TRIK(limb);
                ((TRIK)solver).setTarget(target);
                break;
            }

            default:{
                return null;
            }
        }

        solver.setMaxError(3f);
        if (!debug) solver.setTimesPerFrame(5);
        else solver.setTimesPerFrame(5f);
        target.setPosition(limb.get(limb.size() - 1).position());
        TimingTask task = new TimingTask(scene) {
            @Override
            public void execute() {
                if (solve) solver.solve();
            }
        };
        task.run(20);
        return solver;
    }



    public Node createTarget(Scene scene, float radius){
        /*
         * A target is a Node, we represent a Target as a
         * Red ball.
         * */
        PShape redBall;
        if (scene.is2D()) redBall = createShape(ELLIPSE,0, 0, radius*2, radius*2);
        else  redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        Node target = new Node(scene){
            @Override
            public void graphics(PGraphics pGraphics){
                scene.drawAxes(pGraphics,radius *1.5f);
                pGraphics.shape(redBall);
            }
        };
        //Exact picking precision
        target.setPickingThreshold(0);
        return target;
    }

    public Node createJoint(Scene scene, Node node, Vector translation, float radius, boolean drawLine){
        /*
         * A Joint will be represented as a ball
         * that is joined to its reference Node
         * */
        Joint joint = new Joint(scene, radius);
        joint.setReference(node);
        //Exact picking precision
        joint.setPickingThreshold(0);
        joint.setTranslation(translation);
        if(!drawLine) joint.setRoot(true);
        return joint;
    }

    public void drawInfo(List<Node> skeleton){
        for (int i = 0; i < skeleton.size(); i++) {
            //Print Node names
            Vector screenLocation = scene.screenLocation(skeleton.get(i).position());
            text("Node " + i, screenLocation.x(), screenLocation.y());
        }
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            if(targets.contains(scene.node())){
                for(Node target : targets) scene.translateNode(target, scene.mouseDX(), scene.mouseDY(), 0);
            }
            else{
                scene.mouseTranslate();
            }

        } else {
            scene.scale(mouseX - pmouseX);
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

    public void keyPressed(){
        if(key == 'S' || key == 's') {
            solve = !solve;
        }else if(key =='a' || key == 'A'){
                trik.solve();
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.ConstrainedTRIK"});
    }
}