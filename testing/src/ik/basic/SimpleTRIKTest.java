package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.TRIK;
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

public class SimpleTRIKTest extends PApplet {
    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;
    float jointRadius = 5;
    float length = 50;
    int numJoints = 8;
    boolean enableSolver = false;
    Solver trik, fabrik;
    List<Node> skeleton_trik, skeleton_fabrik;
    List<Node> targets = new ArrayList<Node>();

    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        Joint.axes = true;
        TRIK._debug = true;
        TRIK._singleStep = true;

        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(280);
        scene.fit(1);
        int color = color(random(255), random(255), random(255));
        skeleton_fabrik = Util.generateChain(scene, numJoints, jointRadius, length, Vector.multiply(scene.rightVector(), -scene.radius()/2f), color, -1, 0);
        color = color(random(255), random(255), random(255));
        skeleton_trik = Util.generateChain(scene, numJoints, jointRadius, length, Vector.multiply(scene.rightVector(), scene.radius()/2f), color, -1, 0);

        Util.generateConstraints(skeleton_fabrik, Util.ConstraintType.CONE_ELLIPSE, 0, scene.is3D());
        Util.generateConstraints(skeleton_trik, Util.ConstraintType.CONE_ELLIPSE, 0, scene.is3D());

        //skeleton_trik = createSkeleton(Vector.multiply(scene.rightVector(), -scene.radius()/2f));
        // skeleton_fabrik  = createSkeleton(Vector.multiply(scene.rightVector(), scene.radius()/2f));
        //Create solver
        trik = createSolver("TRIK", skeleton_trik);
        fabrik = createSolver("FABRIK", skeleton_fabrik);
        //Define Text Properties
        textAlign(CENTER);
        textSize(24);
    }

    public void draw() {
        if(((TRIK) trik)._singleStep && ((TRIK) trik)._stepCounter > 1){
            if(((TRIK) trik)._stepCounter > 2) ((Joint)(((TRIK) trik).copyChain().get(((TRIK) trik)._stepCounter - 3))).setColor(color(255,0,0));
            ((Joint)(((TRIK) trik).copyChain().get(((TRIK) trik)._stepCounter - 2))).setColor(255);
        }

        background(0);
        if(scene.is3D()) lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        //drawInfo(skeleton_trik);
        //drawInfo(skeleton_fabrik);
        scene.endHUD();
    }

    public List<Node> createSkeleton(Vector translation){
        ArrayList<Node> skeleton = new ArrayList<Node>();
        skeleton.add(createJoint(scene,null, new Vector(0, -scene.radius()/2), jointRadius, false));
        skeleton.add(createJoint(scene,skeleton.get(0), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(1), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(2), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(3), new Vector(0, length), jointRadius, true));
        //End Effector
        Node endEffector = createJoint(scene,skeleton.get(4), new Vector(0, length), jointRadius, true);
        skeleton.add(endEffector);
        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        endEffector.enableTracking(false);
        skeleton.get(0).translate(translation);
        return skeleton;
    }

    public Solver createSolver(String type, List<Node> skeleton){
        Solver solver;
        Node endEffector = skeleton.get(skeleton.size() - 1);
        //2. Lets create a Target (a bit bigger than a Joint in the structure)
        Node target = createTarget(scene, jointRadius * 1.5f);
        //Locate the Target on same spot of the end effectors
        target.setPosition(endEffector.position());
        targets.add(target);
        //3. Relate the structure with a Solver. In this example we instantiate a solver
        switch(type){
            case "TRIK":{
                TRIK trik = new TRIK(skeleton);
                trik.enableWeight(true);
                trik.setLookAhead(4);
                solver = trik;
                break;
            }
            case "FABRIK":{
                solver = new nub.ik.solver.geometric.ChainSolver(skeleton);
                break;
            }
            default:{
                solver = new TRIK(skeleton);
            }
        }
        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        solver.setMaxError(1);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver.setMaxIterations(2000);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver.setTimesPerFrame(1);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        solver.setMinDistance(0.5f);
        //4. relate targets with end effectors
        solver.setTarget(endEffector, target);
        //5. Create a Timing Task such that the solver executes each amount of time
        TimingTask solverTask = new TimingTask(scene) {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                if(enableSolver){
                    solver.solve();
                }
            }
        };
        solverTask.run(40); //Execute the solverTask each 40 ms
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
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            if(targets.contains(scene.trackedNode())){
                for(Node target : targets) scene.translate(target);
            }
            else{
                scene.translate();
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
        if(key == 'A' || key == 'a'){
            enableSolver = false;
            trik.solve();
            fabrik.solve();
        }

        if(key == 'S' || key == 's'){
            enableSolver = !enableSolver;
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.SimpleTRIKTest"});
    }
}