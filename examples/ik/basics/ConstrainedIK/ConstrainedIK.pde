/*
 * Constrained IK
 * by Sebastian Chaparro Cuevas.
 *
 * In this example an IK solver will be related with a pretty simple chain structure on the XY-Plane,
 * furthermore each node has either 1-DOF (Hinge) or 3-DOF (Ball and Socket) constraint:
 *                         World
 *                           ^
 *                           | \
 *                           0 eye
 *                           ^
 *                           |
 *                           1
 *                           ^
 *                           |
 *                           2
 *                           ^
 *                           |
 *                           3
 *                           ^
 *                           |
 *                           4
 * As Node 4 is the End effector of the structure (leaf node) we will attach a Target to it.
 * Press 'S' to stop/restart IK Solver
 */


import nub.primitives.*;
import nub.core.*;
import nub.core.constraint.*;
import nub.processing.*;
import nub.ik.solver.geometric.*;
import nub.timing.*;

int w = 700;
int h = 700;

Scene scene;
float jointRadius = 5;
float length = 50;
//Skeleton structure defined above
ArrayList<Node> skeleton = new ArrayList<Node>();
boolean enableSolver = true;

void settings() {
    size(w, h, P3D);
}

void setup() {
    //Setting the scene
    scene = new Scene(this);
    if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(280);
    scene.fit(1);
    //1. Create the Skeleton (chain described above)
    Node node0 = new Joint(scene,null, new Vector(0, -scene.radius()/2), jointRadius, false);
    Node node1 = new Joint(scene,node0, new Vector(0, length), jointRadius, true);
    Node node2 = new Joint(scene,node1, new Vector(0, length), jointRadius, true);
    Node node3 = new Joint(scene,node2, new Vector(0, length), jointRadius, true);
    Node node4 = new Joint(scene,node3, new Vector(0, length), jointRadius, true);
    skeleton.add(node0);
    skeleton.add(node1);
    skeleton.add(node2);
    skeleton.add(node3);
    skeleton.add(node4);
    //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
    node4.enableTracking(false);
    //---------------------------------------------------
    //Apply constraints
    /*
     * When working with constraints it is important to take into account the rotation / orientation of the nodes.
     *
     * Here we are drawing the local axes of each node to know how to set properly a constraint.
     * Keep in mind that:
     *   X Axis is represented as a red line.
     *   Y Axis is represented as a green line.
     *   Z Axis is represented as a blue line.
     *
     * Locating a constraint requires to set an initial reference rotation, a Twist Vector and an Up Vector.
     * Once they are properly identified you could define the limits of the Hinge (1-DOF) or Ball and Socket (3-DOF)
     * constraints (see Constraint explanation on nub wikis).
     **/

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
    Node target = new Target(scene, jointRadius * 1.5f);
    
    //Locate the Target on same spot of the end effector
    target.setPosition(node4.position());
    
    //3. Relate the structure with a Solver. In this example we instantiate a solver
    //As we're dealing with a Chain Structure a Chain Solver is preferable
    //A Chain solver constructor receives an ArrayList containing the Skeleton structure
    final ChainSolver solver = new ChainSolver(skeleton);
    
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
    scene.registerTask(solverTask); //Add solverTask to the Graph scene
    solverTask.run(40); //Execute the solverTask each 40 ms
    
    //Define Text Properties
    textAlign(CENTER);
    textSize(24);
}

public void draw() {
    background(0);
    lights();
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



void mouseMoved() {
    scene.cast();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.spin();
    } else if (mouseButton == RIGHT) {
        scene.translate();
    } else {
        scene.scale(mouseX - pmouseX);
    }
}

void mouseWheel(MouseEvent event) {
    scene.scale(event.getCount() * 20);
}

void mouseClicked(MouseEvent event) {
    if (event.getCount() == 2)
        if (event.getButton() == LEFT)
            scene.focus();
        else
            scene.align();
}

void keyPressed(){
    if(key == 'S' || key == 's'){
        enableSolver = !enableSolver;
    }
}
