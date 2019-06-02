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
import nub.ik.*;


int w = 1200;
int h = 1200;

//Choose FX2D, JAVA2D, P2D or P3D
String renderer = P3D;

Scene scene;
float jointRadius = 5;
float length = 50;
//Skeleton structure defined above
Node[] skeleton = new Node[7];


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
    skeleton[0] = createJoint(scene,null, new Vector(0, -scene.radius()/2), jointRadius, false);
    skeleton[1] = createJoint(scene,skeleton[0], new Vector(0, length), jointRadius, true);
    skeleton[2] = createJoint(scene,skeleton[0], new Vector(0, length), jointRadius, true);
    skeleton[3] = createJoint(scene,skeleton[1], new Vector(-length, length), jointRadius, true);
    skeleton[4] = createJoint(scene,skeleton[2], new Vector(length, length), jointRadius, true);

    //Left End Effector
    skeleton[5] = createJoint(scene,skeleton[3], new Vector(-length, length), jointRadius, true);
    //Right End Effector
    skeleton[6] = createJoint(scene,skeleton[4], new Vector(length, length), jointRadius, true);

    //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
    skeleton[5].enableTracking(false);
    skeleton[6].enableTracking(false);

    //2. Lets create two Targets (a bit bigger than a Joint structure)
    Node leftTarget = createTarget(scene, jointRadius * 1.1f);
    Node rightTarget = createTarget(scene, jointRadius * 1.1f);

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
    scene.beginHUD();
    for (int i = 0; i < skeleton.length; i++) {
        if(i == 2) continue;
        //Print Node names
        Vector screenLocation = scene.screenLocation(skeleton[i].position());
        String s = "";
        if(i == 1) {
            s += ", " + (i + 1);
        }
        text("Node " + i + s, screenLocation.x(), screenLocation.y());

    }
    scene.endHUD();
}


Node createTarget(Scene scene, float radius){
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

Node createJoint(Scene scene, Node node, Vector translation, final float radius, final boolean drawLine){
    /*
    * A Joint will be represented as a green ball
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