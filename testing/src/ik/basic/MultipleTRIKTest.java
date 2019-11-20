package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.geometric.trik.TRIK;
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

public class MultipleTRIKTest extends PApplet {
    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;
    float jointRadius = 5;
    float length = 50;
    boolean enableSolver = false;
    TRIK trik1, trik2, trik3;
    List<Node> skeleton_trik1, skeleton_trik2, skeleton_trik3;
    List<Node> targets = new ArrayList<Node>();


    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        //TRIK._debug = true;
        Joint.axes = true;
        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(280);
        scene.fit(1);
        //Create the Skeleton (chain described above)
        skeleton_trik1 = createSkeleton(Vector.multiply(scene.rightVector(), -scene.radius()/2f));
        skeleton_trik2 = createSkeleton(new Vector());
        skeleton_trik3 = createSkeleton(Vector.multiply(scene.rightVector(), scene.radius()/2f));
        //Create solver
        trik1 = createSolver(skeleton_trik1, 0);
        trik2 = createSolver(skeleton_trik2, 1);
        trik3 = createSolver(skeleton_trik3, 2);
        trik1.enableDirection(true);
        trik2.enableDirection(true);
        trik3.enableDirection(true);
        //trik1.enableTwistHeuristics(false);
        //trik2.enableTwistHeuristics(false);
        //trik3.enableTwistHeuristics(false);

        //trik2.enableWeight(true);
        trik3.enableWeight(true);
        trik2.setLookAhead(3);
        trik3.setLookAhead(3);
        trik3.smooth(true);

        //Define Text Properties
        textAlign(CENTER);
        textSize(24);
    }

    public void draw() {
        background(0);
        if(scene.is3D()) lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        //drawInfo(skeleton_trik1);
        //drawInfo(skeleton_trik2);
        //drawInfo(skeleton_trik3);
        scene.endHUD();
    }

    public List<Node> createSkeleton(Vector translation){
        ArrayList<Node> skeleton = new ArrayList<Node>();
        skeleton.add(createJoint(scene, null, new Vector(0, -scene.radius() / 2), jointRadius, false));
        int i;
        for(i = 0; i < 5; i++) {
            skeleton.add(createJoint(scene, skeleton.get(i), new Vector(0, length), jointRadius, true));
        }
        //End Effector
        Node endEffector = createJoint(scene,skeleton.get(i), new Vector(0, length), jointRadius, true);
        skeleton.add(endEffector);
        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        endEffector.enableTagging(false);
        skeleton.get(0).translate(translation);
        return skeleton;
    }

    public TRIK createSolver(List<Node> skeleton, int gTargets){
        Node endEffector = skeleton.get(skeleton.size() - 1);
        //2. Lets create a Target (a bit bigger than a Joint in the structure)
        Node target = createTarget(scene, jointRadius * 1.5f);
        //Locate the Target on same spot of the end effectors
        target.setPosition(endEffector.position());
        targets.add(target);
        //3. Relate the structure with a Solver. In this example we instantiate a solver
        TRIK solver = new TRIK(skeleton);

        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        //solver.setMaxError(0.0001f);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver.setMaxIterations(10);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver.setTimesPerFrame(TRIK._debug ? 1 : 10);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        //solver.setMinDistance(5f);
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
        return createTarget(scene, radius, color(255,0,0));
    }

    public Node createTarget(Scene scene, float radius, int col){
        /*
         * A target is a Node, we represent a Target as a
         * Red ball.
         * */
        PShape redBall;
        if (scene.is2D()) redBall = createShape(ELLIPSE,0, 0, radius*2, radius*2);
        else  redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(col);

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
                for(Node target : targets) scene.translateNode(target, scene.mouseDX(), scene.mouseDY());
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
        if(key == 'A' || key == 'a'){
            trik1.solve();
            trik2.solve();
            trik3.solve();
        }
        if(key == 'S' || key == 's'){
            enableSolver = !enableSolver;
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.MultipleTRIKTest"});
    }
}