package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 01/06/19.
 */

public class InstantiateIK2 extends PApplet {

    /*
    In this example we will compare the performance of two common IK solver
    related with a pretty simple chain structure on the XY-Plane:
                                     World
                                       ^
                                       |
                                       0
                                       ^
                                       |
                                       1
                                       ^
                                       |
                                       2
                                       ^
                                       |
                                       3
                                       ^
                                       |
                                       4
    As Node 4 is the End effector of the structure (leaf node) we will attach a Target to it.
    */

    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;
    float jointRadius = 5;
    float length = 50;
    boolean enableSolver = true;
    //Skeleton structure defined above
    ArrayList<Node> skeleton1 = new ArrayList<Node>();
    ArrayList<Node> skeleton2 = new ArrayList<Node>();


    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(280);
        scene.fit(1);
        //1. Create the Skeletons (chain described above)
        skeleton1 = createSkeleton(new Vector(-scene.radius()/2 , -scene.radius()/2 , 0));
        skeleton2 = createSkeleton(new Vector( scene.radius()/2 , -scene.radius()/2 , 0));
        //2. Lets create a Target (a bit bigger than a Joint structure)
        Node target1 = createTarget(scene, jointRadius * 1.5f);
        Node target2 = createTarget(scene, jointRadius * 1.5f);

        //Locate the Target on same spot of the end effectors
        target1.setPosition(skeleton1.get(skeleton1.size()-1).position());
        target2.setPosition(skeleton2.get(skeleton2.size()-1).position());

        //3. Relate the structure with a Solver. In this example we instantiate a solver

        //A Chain solver constructor receives an ArrayList containing the Skeleton structure
        ChainSolver solver1 = new ChainSolver(skeleton1);

        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        solver1.setMaxError(1);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver1.setMaxIterations(15);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver1.setTimesPerFrame(5);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        solver1.setMinDistance(0.5f);

        //A CCD solver is another quite known solver, it is only allowed for chain structures
        //A CCD solver constructor receives an ArrayList containing the Skeleton structure
        CCDSolver solver2 = new CCDSolver(skeleton2);

        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        solver2.setMaxError(1);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver2.setMaxIterations(15);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver2.setTimesPerFrame(5);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        solver2.setMinDistance(0.5f);

        //4. relate targets with end effectors
        solver1.setTarget(skeleton1.get(skeleton1.size() - 1), target1);
        solver2.setTarget(skeleton2.get(skeleton2.size() - 1), target2);

        //5. Create a Timing Task for each solver such that the solver executes each amount of time
        TimingTask solverTask1 = new TimingTask(scene) {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                if(enableSolver){
                    solver1.solve();
                }
            }
        };

        TimingTask solverTask2 = new TimingTask(scene) {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                if(enableSolver){
                    solver2.solve();
                }
            }
        };

        solverTask1.run(40); //Execute the solverTask each 40 ms
        solverTask2.run(40); //Execute the solverTask each 40 ms

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

        for (int i = 0; i < skeleton1.size(); i++) {
            //Print Node names
            Vector screenLocation1 = scene.screenLocation(skeleton1.get(i).position());
            text("Node " + i, screenLocation1.x(), screenLocation1.y());
            Vector screenLocation2 = scene.screenLocation(skeleton2.get(i).position());
            text("Node " + i, screenLocation2.x(), screenLocation2.y());
            if(i == 0){
                text("Chain Solver", screenLocation1.x(), screenLocation1.y() - 50);
                text("CCD Solver", screenLocation2.x(), screenLocation2.y() - 50);
            }

        }
        scene.endHUD();
    }

    public ArrayList<Node> createSkeleton(Vector translation ){
        ArrayList<Node> skeleton = new ArrayList<Node>();
        skeleton.add(createJoint(scene,null, translation, jointRadius, false));
        skeleton.add(createJoint(scene,skeleton.get(0), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(1), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(2), new Vector(0, length), jointRadius, true));
        skeleton.add(createJoint(scene,skeleton.get(3), new Vector(0, length), jointRadius, true));
        //End Effector
        Node endEffector = createJoint(scene,skeleton.get(4), new Vector(0, length), jointRadius, true);
        skeleton.add(endEffector);
        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        endEffector.enableTracking(false);

        return skeleton;
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

        Node target = new Node(scene, redBall);
        //Exact picking precision
        target.setPickingThreshold(0);
        return target;
    }

    public Node createJoint(Scene scene, Node node, Vector translation, float radius, boolean drawLine){
        /*
         * A Joint will be represented as a ball
         * that is joined to its reference Node
         * */

        Node joint = new Node(scene){
            @Override
            public void graphics(PGraphics pg){
                Scene scene = (Scene) this._graph;
                pg.pushStyle();
                if (drawLine) {
                    pg.stroke(255);
                    Vector v = location(new Vector(), reference());
                    if (scene.is2D()) {
                        pg.line(0, 0, v.x(), v.y());
                    } else {
                        pg.line(0, 0, 0,  v.x(), v.y(), v.z());
                    }
                }
                pg.fill(color(0,255,0));
                pg.noStroke();
                if (scene.is2D()) pg.ellipse(0, 0, radius*2, radius*2);
                else pg.sphere(radius);
                pg.popStyle();
            }
        };
        joint.setReference(node);
        //Exact picking precision
        joint.setPickingThreshold(0);
        joint.setTranslation(translation);
        return joint;
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
        if(key == 'S'){
            enableSolver = !enableSolver;
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.InstantiateIK2"});
    }
}