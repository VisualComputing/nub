package ik.basic;

import nub.core.Node;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

/**
 * Created by sebchaparr on 01/06/19.
 */

public class RegisterIK extends PApplet {

    /*
    In this example an IK solver will be related
    with a Y-Shape structure on the XY-Plane:
                             World
                               ^
                               |\
                               0 eye
                               ^
                               |
                               1
                               ^
                              / \
                             2   3
                            /     \
                           4       5
    As Nodes 5 and 6 are the End effectors of the structure (leaf nodes)
    we will add a Target for each one of them.
    */

    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;
    float jointRadius = 5;
    float length = 50;
    //Skeleton structure defined above
    Node[] skeleton = new Node[6];


    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        //Setting the scene and loading mesh
        scene = new Scene(this);
        //scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(200);
        scene.fit(1);
        //1. Create the Skeleton (Y-Shape described above)
        skeleton[0] = createJoint(scene,null, new Vector(), jointRadius, false);
        skeleton[1] = createJoint(scene,skeleton[0], new Vector(0, length), jointRadius, true);
        skeleton[2] = createJoint(scene,skeleton[1], new Vector(-length, length), jointRadius, true);
        skeleton[3] = createJoint(scene,skeleton[1], new Vector(length, length), jointRadius, true);

        //Left End Effector
        skeleton[4] = createJoint(scene,skeleton[2], new Vector(-length, length), jointRadius, true);
        //Right End Effector
        skeleton[5] = createJoint(scene,skeleton[3], new Vector(length, length), jointRadius, true);

        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        skeleton[4].enableTracking(false);
        skeleton[5].enableTracking(false);

        //2. Lets create two Targets (a bit bigger than a Joint structure)
        Node leftTarget = createTarget(scene, jointRadius * 1.1f);
        Node rightTarget = createTarget(scene, jointRadius * 1.1f);

        //Locate the Targets on same spot of the end effectors
        leftTarget.setPosition(skeleton[4].position());
        rightTarget.setPosition(skeleton[5].position());

        //3. Relate the structure with a Solver. In this example we register a solver in the graph scene
        scene.registerTreeSolver(skeleton[0]);

        //4. relate targets with end effectors
        scene.addIKTarget(skeleton[4], leftTarget);
        scene.addIKTarget(skeleton[5], rightTarget);

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
        for (int i = 0; i < skeleton.length; i++) {
            //Print Node names
            Vector screenLocation = scene.screenLocation(skeleton[i].position());
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

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.RegisterIK"});
    }
}