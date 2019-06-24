/*
 * Instantiate IK
 * by Sebastian Chaparro Cuevas.
 *
 * This example illustrates how to use an IK solver.
 * Here an IK solver will be related  with a pretty simple chain structure on the XY-Plane:
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
import nub.processing.*;
import nub.ik.solver.geometric.*;
import nub.timing.*;

int w = 700;
int h = 700;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

Scene scene;
float jointRadius = 5;
float length = 50;
//Skeleton structure defined above
ArrayList<Node> skeleton = new ArrayList<Node>();
boolean enableSolver = true;

void settings() {
    size(w, h, renderer);
}

void setup() {
    //Setting the scene
    scene = new Scene(this);
    if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(280);
    scene.fit(1);
    //1. Create the Skeleton (chain described above)
    skeleton.add(new Joint(scene,null, new Vector(0, -scene.radius()/2), jointRadius, false));
    skeleton.add(new Joint(scene,skeleton.get(0), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(1), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(2), new Vector(0, length), jointRadius, true));
    skeleton.add(new Joint(scene,skeleton.get(3), new Vector(0, length), jointRadius, true));
    //End Effector
    Node endEffector = new Joint(scene,skeleton.get(4), new Vector(0, length), jointRadius, true);
    skeleton.add(endEffector);
    //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
    endEffector.enableTracking(false);

    //2. Lets create a Target (a bit bigger than a Joint in the structure)
    Node target = new Target(scene, jointRadius * 1.5f);

    //Locate the Target on same spot of the end effectors
    target.setPosition(endEffector.position());

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
    solver.setTarget(endEffector, target);

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

void draw() {
    background(0);
    if(scene.is3D()) lights();
    scene.drawAxes();
    scene.render();
    noLights();
    scene.beginHUD();
    for (int i = 0; i < skeleton.size(); i++) {
        //Print Node names
        Vector screenLocation = scene.screenLocation(skeleton.get(i).position());
        if(i == 0){
          text("Chain Structure", screenLocation.x(), screenLocation.y() - 50);
        }
        if(i == skeleton.size() - 1){
            text("End Effector " + i, screenLocation.x(), screenLocation.y() + 30);
        } else{
            text("Node " + i, screenLocation.x(), screenLocation.y() + 30);
        }
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
