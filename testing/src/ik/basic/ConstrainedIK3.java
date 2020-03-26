package ik.basic;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
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

public class ConstrainedIK3 extends PApplet {

    /*
    In this example an IK solver will be related
    with a pretty simple chain structure on the XY-Plane,
    furthermore each node has a 3-DOF or 1-DOF constraint:
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
    float jointRadius = 5;
    float length = 50;
    boolean enableSolver = true;
    //Skeleton structure defined above
    ArrayList<Node> skeleton = new ArrayList<Node>();

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(280);
        scene.fit(1);
        //1. Create the Skeleton (chain described above)
        Node node0 = createJoint(scene,null, new Vector(0, -scene.radius()/2), jointRadius, false);
        Node node1 = createJoint(scene,node0, new Vector(0, length), jointRadius, true);
        Node node2 = createJoint(scene,node1, new Vector(0, length), jointRadius, true);
        Node node3 = createJoint(scene,node2, new Vector(0, length), jointRadius, true);
        Node node4 = createJoint(scene,node3, new Vector(0, length), jointRadius, true);
        skeleton.add(node0);
        skeleton.add(node1);
        skeleton.add(node2);
        skeleton.add(node3);
        skeleton.add(node4);
        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        node4.enableTagging(false);

        //---------------------------------------------------
        //Apply constraints
        /*
        * When working with constraints it is important to take into account the
        * rotation / orientation of the nodes.
        *
        * Here we are drawing local axes of each node to know how to set properly a constraint.
        * Keep in mind that:
        *   X Axis is represented as a red line.
        *   Y Axis is represented as a green line.
        *   Z Axis is represented as a blue line.
        *
        * Locating a constraint requires to set an initial reference rotation, a Twist Vector and an Up Vector.
        * Once they are properly identified you could define:
        *  1. Which is the maximum and minimum angle of a Hinge rotation:
        *   A Hinge rotation is a rotation whose Axis of rotation is the Twist Vector.
        *   There's a whole plane that is Orthogonal to Twist vector. You must define a
        *   Vector on this plane (Called up Vector) that will define the rest rotation.
        *
        *   It is allow to rotate clockwise of the rest rotation while the angle of the Up Vector and the
        *   new Up Vector is not larger than minimum angle.
        *
        *   Similarly, it is allow to rotate anti-clockwise of the rest rotation while the angle of the Up Vector and the
        *   new Up Vector is not larger than maximum angle.
        *
        *  2. Which is the maximum and minimum angle of a Ball and Socket rotation:
        *   A Ball and Socket rotation is a rotation whose Axis of rotation is the Twist Vector.
        *   There's a whole plane that is Orthogonal to Twist vector. You must define a
        *   Vector on this plane (Called up Vector) and a orthogonal Vector of Up and Twist (Called Left vector)
        *   that we will consider as the rest rotation.
        *
        * */


        //Apply a Ball & Socket constraint to node0:
        BallAndSocket constraint0 = new BallAndSocket(radians(40), radians(60));
        constraint0.setRestRotation(node0.rotation(), new Vector(1,0,0), new Vector(0,1,0));
        constraint0.setTwistLimits(radians(50), radians(50));
        node0.setConstraint(constraint0);

        //Apply a Ball & Socket constraint to node1:
        BallAndSocket constraint1 = new BallAndSocket(radians(60), radians(40));
        constraint1.setRestRotation(node1.rotation(), new Vector(1,0,0), new Vector(0,1,0));
        constraint1.setTwistLimits(radians(5), radians(5));
        node1.setConstraint(constraint1);

        //Apply a Hinge constraint to node2:
        Hinge constraint2 = new Hinge(radians(40), radians(60));
        constraint2.setRestRotation(node2.rotation(), new Vector(0,1,0), new Vector(1,0,0));
        node2.setConstraint(constraint2);

        //Apply a Hinge constraint to node3:
        Hinge constraint3 = new Hinge(radians(60), radians(40));
        constraint3.setRestRotation(node3.rotation(), new Vector(0,1,0), new Vector(0,0,1));
        node3.setConstraint(constraint3);
        //---------------------------------------------------

        //2. Lets create a Target (a bit bigger than a Joint in the structure)
        Node target = createTarget(scene, jointRadius * 1.5f);

        //Locate the Target on same spot of the end effectors
        target.setPosition(node4.position());

        //3. Relate the structure with a Solver. In this example we instantiate a solver
        //As we're dealing with a Chain Structure a Chain Solver is preferable
        //A Chain solver constructor receives an ArrayList containing the Skeleton structure
        ChainSolver solver = new ChainSolver(skeleton);

        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        solver.setMaxError(1);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver.setMaxIterations(15);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver.setTimesPerFrame(5);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        solver.setMinDistance(0.5f);

        //4. relate targets with end effectors
        solver.setTarget(node4, target);

        //5. Create a Timing Task such that the solver executes each amount of time
        TimingTask solverTask = new TimingTask() {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                if(enableSolver){
                    solver.solve();
                }
            }
        };
        solverTask.run(40); //Execute the solverTask each 40 ms

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
        for (int i = 0; i < skeleton.size(); i++) {
            //Print Node names
            Vector screenLocation = scene.screenLocation(skeleton.get(i).position());
            text("Node " + i, screenLocation.x(), screenLocation.y());
        }
        scene.endHUD();

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

        Node target = new Node(redBall);
        //Exact picking precision
        target.setPickingThreshold(0);
        return target;
    }

    public Node createJoint(Scene scene, Node node, Vector translation, float radius, boolean drawLine){
        /*
         * A Joint will be represented as a ball
         * that is joined to its reference Node
         * */

        Node joint = new Node(){
            @Override
            public void graphics(PGraphics pg){
                pg.pushStyle();
                if (drawLine) {
                    pg.stroke(255);
                    Vector v = location(new Vector(), reference());
                    if (pg.is2D()) {
                        pg.line(0, 0, v.x(), v.y());
                    } else {
                        pg.line(0, 0, 0,  v.x(), v.y(), v.z());
                    }
                }
                pg.fill(color(0,255,0));
                pg.noStroke();
                if (pg.is2D()) pg.ellipse(0, 0, radius*2, radius*2);
                else pg.sphere(radius);

                //Invoke drawConstraint method to draw the constraint related with the joint
                if (constraint() != null) {
                    Scene.drawConstraint(pg,this);
                }
                pg.strokeWeight(5);
                scene.drawAxes(radius*3);
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
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
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
        PApplet.main(new String[]{"ik.basic.ConstrainedIK3"});
    }
}