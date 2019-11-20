package ik.mocap;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.loader.bvh.BVHLoader;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.trik.TRIK;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.core.constraint.BallAndSocket;
import nub.processing.Scene;
import nub.processing.TimingTask;
import nub.ik.visual.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sebchaparr on 23/03/18.
 */
public class Viewer extends PApplet{
    Scene scene;
    String path = "/testing/data/bvh/mocap.bvh";

    BVHLoader parser;
    HashMap<String, Node> originalLimbs = new HashMap<String, Node>();
    HashMap<String,Node> limbs = new HashMap<String, Node>();
    HashMap<String,Node> targets = new HashMap<String, Node>();

    Node root, rootIK;

    CCDSolver ccd_solver;
    ChainSolver chain_solver;
    Solver solver;

    ArrayList<Solver> chainsolvers = new ArrayList<>();
    float[] exploration;

    float targetRadius = 7;

    boolean read = false;
    boolean solve = false;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        //TRIK._debug = true;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(200);
        scene.eye().rotate(new Quaternion(0,0,PI));
        scene.fit(1);

        //Add a target per limb
        targets.put("LEFTHAND",  createTarget(targetRadius));
        targets.put("RIGHTHAND", createTarget(targetRadius));
        targets.put("LEFTFOOT",  createTarget(targetRadius));
        targets.put("RIGHTFOOT", createTarget(targetRadius));
        targets.put("HEAD",      createTarget(targetRadius));

        parser = new BVHLoader(sketchPath() + path, scene, null);
        root = parser.root();
        ((Joint) root).setRoot(true);

        //parser.constraintJoints();

        //make a copy of the skeleton
        rootIK = (Joint) copy(scene.branch(root));
        //rootIK.translate(50, 50, 50);
        //rootIK.setConstraint(null);

        //Solver solver = scene.registerTreeSolver(rootIK);

        //scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        //scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        //scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        //scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        //scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));

        //Solver solver = scene.registerTreeSolver(rootIK);//limbs.get("RIGHTUPLEG"));
        //limbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());
        //originalLimbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());

        /*ArrayList<Frame> list = (ArrayList<Frame>) scene.branch(limbs.get("RIGHTCOLLAR"));
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());
        list.add(0, list.get(0).reference());

        //chain_solver = new ClosedLoopChainSolver(list);
        ArrayList<Frame> list2 = (ArrayList<Frame>) scene.branch(originalLimbs.get("RIGHTCOLLAR"));
        list2.add(0, list2.get(0).reference());
        list2.add(0, list2.get(0).reference());
        //list2.add(0, list2.get(0).reference());
        //solver = scene.registerTreeSolver(limbs.get("RIGHTUPLEG").reference());
        //Solver solver = scene.registerTreeSolver(limbs.get("RIGHTUPLEG").reference());

        chain_solver = new ChainSolver(list2);
        for(Frame f : rootIK.children()) {
            //f.setConstraint(new FixedConstraint());
            //limbs.get("RIGHTUPLEG").reference().setConstraint(new FixedConstraint());
        }

        ((FABRIKSolver) solver).pg = scene.pApplet().getGraphics();
        chain_solver.pg = scene.pApplet().getGraphics();
        //scene.unregisterTreeSolver(limbs.get("RIGHTUPLEG").reference());

        //ccd_solver = new CCDSolver(scene.branch(originalLimbs.get("RIGHTUPLEG")));
        ccd_solver = new CCDSolver(list);
        ccd_solver._timesPerFrame = 20;
        chain_solver._timesPerFrame = 20;
        chain_solver._maxError = 0.1f;
        solver._timesPerFrame = 0.5f;
        solver.setMaxError(1f);
        ccd_solver._maxError = 0.1f;
        //scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        //chain_solver.setTarget(targets.get("RIGHTFOOT"));
        scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        //chain_solver.setTarget(targets.get("RIGHTFOOT"));
        chain_solver.setTarget(targets.get("RIGHTHAND"));
        ccd_solver.setTarget(targets.get("RIGHTHAND"));
        //scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));*/

        String[] target_names = {"RIGHTFOOT", "RIGHTHAND", "LEFTHAND", "LEFTFOOT", "HEAD"};
        String[] head_names = {"RIGHTUPLEG", "RIGHTUPARM", "LEFTUPARM", "LEFTUPLEG", "NECK"};

        //Adding constraints
        //CHEST  [ 45.6711, 116.072105, -16.3786 ] -> B & S - NECK  [ 46.18769, 145.8957, -9.860168 ]
        Node chest = limbs.get("CHEST");
        Node neck = limbs.get("NECK");
        BallAndSocket chestConstraint = new BallAndSocket(radians(45), radians(45));
        chestConstraint.setRestRotation(chest.rotation().get(), neck.translation().get(), new Vector(1,0,0));
        chest.setConstraint(chestConstraint);

        //LEFTUPARM  [ 29.100441, 145.6254, -13.055123 ]
        Node LUArm= limbs.get("LEFTUPARM");
        Node LLArm = limbs.get("LEFTLOWARM");
        BallAndSocket LUArmConstraint = new BallAndSocket(radians(85), radians(85));
        LUArmConstraint.setRestRotation(LUArm.rotation().get(), new Vector(0,1,0), new Vector(1,0,0),  LLArm.translation().get());
        LUArmConstraint.setTwistLimits(radians(70), radians(90));
        LUArm.setConstraint(LUArmConstraint);

        // LEFTLOWARM  [ 25.23188, 120.4344, -14.801453 ]
        Vector LLArmTwist = Vector.cross(limbs.get("LEFTHAND").translation(), new Vector(0,0,1), null);
        Hinge LLArmConstraint = new Hinge(radians(0), radians(178),LLArm.rotation().get(), limbs.get("LEFTHAND").translation() , LLArmTwist);
        LLArm.setConstraint(LLArmConstraint);

        //RIGTHUPARM  [ 29.100441, 145.6254, -13.055123 ]
        Node RUArm= limbs.get("RIGHTUPARM");
        Node RLArm = limbs.get("RIGHTLOWARM");
        BallAndSocket RUArmConstraint = new BallAndSocket(radians(85), radians(85));
        RUArmConstraint.setRestRotation(RUArm.rotation().get(), new Vector(0,1,0), new Vector(-1,0,0),  RLArm.translation().get());
        RUArmConstraint.setTwistLimits(radians(70), radians(90));

        RUArm.setConstraint(RUArmConstraint);
        // LEFTLOWARM  [ 25.23188, 120.4344, -14.801453 ]
        Vector RLArmTwist = Vector.cross(limbs.get("RIGHTHAND").translation(), new Vector(0,0,1), null);
        Hinge RLArmConstraint = new Hinge(radians(0), radians(178),RLArm.rotation().get(), limbs.get("RIGHTHAND").translation(), RLArmTwist);
        RLArm.setConstraint(RLArmConstraint);


        //LEFTUPLEG  [ 54.6711, 101.23, -16.3786 ] - > B&S
        Node LULeg = limbs.get("LEFTUPLEG");
        Node LLLeg = limbs.get("LEFTLOWLEG");
        BallAndSocket LULegConstraint = new BallAndSocket(radians(50), radians(50));
        LULegConstraint.setRestRotation(LULeg.rotation().get(), new Vector(1,0,0), LLLeg.translation().get());
        LULegConstraint.setTwistLimits(radians(10), radians(10));
        LULeg.setConstraint(LULegConstraint);
        //LEFTLOWLEG  [ 54.0961, 49.799, -12.47683 ] - > hinge
        Hinge LLLegConstraint = new Hinge(radians(0), radians(170), LLLeg.rotation().get(), new Vector(0,-1,0),new Vector(1,0,0));
        LLLeg.setConstraint(LLLegConstraint);


        //RIGHTUPLEG  [ 37.2461, 49.798904, -12.47683 ]
        Node RULeg = limbs.get("RIGHTUPLEG");
        Node RLLeg = limbs.get("RIGHTLOWLEG");
        BallAndSocket RULegConstraint = new BallAndSocket(radians(50), radians(50));
        RULegConstraint.setRestRotation(RULeg.rotation().get(), new Vector(1,0,0), RLLeg.translation().get());
        RULegConstraint.setTwistLimits(radians(10), radians(10));
        RULeg.setConstraint(RULegConstraint);
        //RIGHTLOWLEG  [ 37.2461, 49.798904, -12.47683 ]
        Hinge RLLegConstraint = new Hinge(radians(0), radians(170),RLLeg.rotation().get(), new Vector(0,-1,0), new Vector(1,0,0));
        RLLeg.setConstraint(RLLegConstraint);

        for(int i = 0; i < target_names.length; i++){
            List<Node> fr = new ArrayList<>();
            for(Node f : scene.branch(limbs.get(head_names[i]))){
                fr.add(f);
            }

            //BioIk chain = new BioIk(fr, 10, 4);
            //ChainSolver chain = new ChainSolver((ArrayList) fr);
            TRIK chain = new TRIK((ArrayList) fr);
            chain.setLookAhead(3);
            chain.enableWeight(true);
            //chain.setKeepDirection(true);
            //chain.setFixTwisting(true);

            //chain.explore(true);
            chain.setTimesPerFrame(10);
            chain.setMaxIterations(10);
            //chain.setMaxError(0.1f);
            //chain.setMinDistance(0.1f);
            chain.setTarget(limbs.get(target_names[i]), targets.get(target_names[i]));
            TimingTask task = new TimingTask(scene) {
                @Override
                public void execute() {
                    //if(solve) {
                    chain.solve();
                    //}
                }
            };
            task.run(40);
            chainsolvers.add(chain);
        }
        exploration = new float[chainsolvers.size()];

        //Solver solver = scene.registerTreeSolver(rootIK);

        //scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        //scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        //scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        //scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        //scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));
        //rootIK.cull(true);
        root.cull(true);
    }

    public Node createTarget(float radius){
        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        Node target = new Node(scene, redBall);
        target.setPickingThreshold(0);
        return target;
    }

    public void draw() {
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);
        shininess(10);
        //Draw Constraints
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
//        for(String k : limbs.keySet()){
//            Vector sc = scene.screenLocation(limbs.get(k).position());
//            fill(255);
//            text("" +  limbs.get(k).position().x() + "\n" + limbs.get(k).position().y() + "\n" + limbs.get(k).position().z(), sc.x(), sc.y());
//        }
//        for(String k : targets.keySet()){
//            Vector sc = scene.screenLocation(targets.get(k).position());
//            fill(255);
//            text("" +  targets.get(k).position().x() + "\n" + targets.get(k).position().y() + "\n" + targets.get(k).position().z(), sc.x(), sc.y());
//        }
        for(int i = 0; i < chainsolvers.size(); i++){
            //ChainSolver ch = chainsolvers.get(i);
            //Vector sc = scene.screenLocation(ch.target().position());
            //fill(255);
            //exploration[i] = ch.explorationTimes() > exploration[i] ? ch.explorationTimes() : exploration[i];
            //text("" +  exploration[i], sc.x(), sc.y());
        }

        scene.endHUD();

        if(read){
            parser.nextPose();
            updateTargets();
            rootIK.setRotation(root.rotation());
        }
        //parser.drawFeasibleRegion(this.getGraphics());
        //parser.drawConstraint(this.getGraphics());
        if(chain_solver != null) {
            //if(show1) draw_pos(prev, color(0,255,0), 3);
            //if(show2) draw_pos(chain_solver.get_p(), color(255,0,100), 3);
            //if(show3) draw_pos(constr, color(100,100,0), 3);
        }

        if(solve) {
            ccd_solver.solve();
            chain_solver.solve();
        }
    }

    public Node copy(List<Node> branch) {
        ArrayList<Node> copy = new ArrayList<Node>();
        Node reference = branch.get(0).reference();
        HashMap<Integer, Node> map = new HashMap<Integer, Node>();
        map.put(branch.get(0).id(), reference);
        for (Node joint : branch) {
            Joint newJoint = new Joint(scene);
            newJoint.setReference(map.get(joint.id()));
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
                    map.put(child.id(), dummy);
                }else{
                    map.put(child.id(), newJoint);
                }
            }
            if(parser.joint().get(joint.id()).name().equals("LEFTHAND")){
                originalLimbs.put("LEFTHAND", joint);
                limbs.put("LEFTHAND", newJoint);
                targets.get("LEFTHAND").setPosition(newJoint.position());
            } else if(parser.joint().get(joint.id()).name().equals("RIGHTHAND")){
                originalLimbs.put("RIGHTHAND", joint);
                limbs.put("RIGHTHAND", newJoint);
                targets.get("RIGHTHAND").setPosition(newJoint.position());
            } else if(parser.joint().get(joint.id()).name().equals("LEFTFOOT")){
                originalLimbs.put("LEFTFOOT", joint);
                limbs.put("LEFTFOOT", newJoint);
                targets.get("LEFTFOOT").setPosition(newJoint.position());
            } else if(parser.joint().get(joint.id()).name().equals("RIGHTFOOT")){
                originalLimbs.put("RIGHTFOOT", joint);
                limbs.put("RIGHTFOOT", newJoint);
                targets.get("RIGHTFOOT").setPosition(newJoint.position());
            } else if(parser.joint().get(joint.id()).name().equals("HEAD")){
                originalLimbs.put("HEAD", joint);
                limbs.put("HEAD", newJoint);
                targets.get("HEAD").setPosition(newJoint.position());
            }
            originalLimbs.put(parser.joint().get(joint.id()).name(), joint);
            limbs.put(parser.joint().get(joint.id()).name(), newJoint);
        }
        ((Joint) copy.get(0)).setRoot(true);
        return copy.get(0);
    }

    public void updateTargets() {
        rootIK.setPosition(root.position());
        for (Map.Entry<String, Node> entry : originalLimbs.entrySet()) {
            if(targets.containsKey(entry.getKey())) {
                targets.get(entry.getKey()).setPosition(entry.getValue().position());
            }
       }
    }

    public void keyPressed(){
        if(key == ' ') {
            read = !read;
        }
        if(key== 'Q' || key == 'q'){
            ccd_solver.solve();
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
        PApplet.main(new String[]{"ik.mocap.Viewer"});
    }

}