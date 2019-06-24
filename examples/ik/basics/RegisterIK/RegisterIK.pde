/**
 * Register IK
 * by Sebastian Chaparro.
 *
 * This example illustrates how to us an IK solver.
 * IK solver will be related  with a Y-Shape structure on the XY-Plane:
 *                            World
 *                              ^
 *                              |
 *                              0
 *                              ^
 *                             / \
 *                            1   2
 *                           /     \
 *                          3       4
 *                         /         \
 *                        5           6
 * As Nodes 5 and 6 are the End effectors of the structure (leaf nodes)
 * we will add a Target for each one of them.
 * Note: Nodes 1 and 2 lie on the same position (Visually it seems like a single Node).
 * This is done in order to make the motion of branches 1,3,5 and 2,4,6 independent.
 * So if Node 1 is rotated Nodes 2,4,6 will not move.
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.ik.solver.*;


int w = 1200;
int h = 1200;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

Scene scene;
float jointRadius = 5;
float length = 50;
//Skeleton structure defined above
Node[] skeleton = new Node[7];
Node leftTarget;
Node rightTarget;

void settings() {
    size(w, h, renderer);
}

void setup() {
    //Setting the scene
    scene = new Scene(this);
    if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
    scene.setRadius(200);
    scene.fit(1);
    //1. Create the Skeleton (Y-Shape described above)
    skeleton[0] = new Joint(scene,null, new Vector(0,-scene.radius()/2), jointRadius, false);
    skeleton[1] = new Joint(scene,skeleton[0], new Vector(0, length), jointRadius, true);
    skeleton[2] = new Joint(scene,skeleton[0], new Vector(0, length), jointRadius, true);
    skeleton[3] = new Joint(scene,skeleton[1], new Vector(-length, length), jointRadius, true);
    skeleton[4] = new Joint(scene,skeleton[2], new Vector(length, length), jointRadius, true);

    //Left End Effector
    skeleton[5] = new Joint(scene,skeleton[3], new Vector(-length, length), jointRadius, true);
    //Right End Effector
    skeleton[6] = new Joint(scene,skeleton[4], new Vector(length, length), jointRadius, true);

    //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
    skeleton[5].enableTracking(false);
    skeleton[6].enableTracking(false);

    //2. Lets create two Targets (a bit bigger than a Joint structure)
    leftTarget = new Target(scene, jointRadius * 1.3f);
    rightTarget = new Target(scene, jointRadius * 1.3f);

    //Locate the Targets on same spot of the end effectors
    leftTarget.setPosition(skeleton[5].position());
    rightTarget.setPosition(skeleton[6].position());

    //3. Relate the structure with a Solver. In this example we register a solver in the graph scene
    Solver solver = scene.registerTreeSolver(skeleton[0]);

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
    scene.addIKTarget(skeleton[5], leftTarget);
    scene.addIKTarget(skeleton[6], rightTarget);

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
    for (int i = 0; i < skeleton.length; i++) {
        if(i == 2) continue;
        //Print Node names
        Vector screenLocation = scene.screenLocation(skeleton[i].position());
        String s = "";
        if(i == 0){
          text("Basic Skeleton Structure", screenLocation.x(), screenLocation.y() - 50);
        }

        if(i == 1) {
          s += ", " + (i + 1);
        }
        if(i == 5 || i == 6){
            text("End Effector " + i, screenLocation.x(), screenLocation.y() + 30);
        } else{
            text("Node " + i + s, screenLocation.x(), screenLocation.y() + 30);
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
