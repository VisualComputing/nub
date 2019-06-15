package ik.constraintTest;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.CCDSolver;
import nub.ik.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.timing.TimingTask;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class UnconstrainedVsConstrained extends PApplet{
    Scene scene;
    //Set the scene as P3D or P2D
    String renderer = P3D;
    float jointRadius = 5;
    float length = 100;
    boolean enableSolver = true , enableRotation = false;
    //Skeleton structure defined above
    ArrayList<Node> skeleton = new ArrayList<Node>();
    Node target;


    public void settings() {
        size(700, 700, renderer);
    }

    public void setup() {
        //Setting the scene
        scene = new Scene(this);
        scene.setLeftHanded();
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(length * 2);
        scene.fit(1);
        //1. Create the Skeleton (chain described above)
        skeleton.add(createJoint(scene,null, new Vector(-scene.radius()/2, 0), jointRadius, false));
        skeleton.add(createJoint(scene,skeleton.get(0), new Vector(length/sqrt(2), length/sqrt(2)), jointRadius, true));

        //Add constraint
        Hinge c0 = new Hinge(radians(30), radians(30), skeleton.get(0).rotation().get(), skeleton.get(1).translation().get(), new Vector(0,0,1));
        skeleton.get(0).setConstraint(c0);

        //End Effector
        Node endEffector = createJoint(scene,skeleton.get(1), new Vector(length/sqrt(2), -length/sqrt(2)), jointRadius, true);
        skeleton.add(endEffector);
        //As targets and effectors lie on the same spot, is preferable to disable End Effectors tracking
        endEffector.enableTracking(false);
        //2. Lets create a Target (a bit bigger than a Joint in the structure)
        target = createTarget(scene, jointRadius * 1.5f);
        //Locate the Target on same spot of the end effectors
        target.setPosition(endEffector.position());

        //3. Relate the structure with a Solver. In this example we instantiate a solver
        //As we're dealing with a Chain Structure a Chain Solver is preferable
        //A Chain solver constructor receives an ArrayList containing the Skeleton structure
        CCDSolver solver = new CCDSolver(skeleton);

        //Optionally you could modify the following parameters of the Solver:
        //Maximum distance between end effector and target, If is below maxError, then we stop executing IK solver (Default value is 0.01)
        solver.setMaxError(1);
        //Number of iterations to perform in order to reach the target (Default value is 50)
        solver.setMaxIterations(15);
        //Times a solver will iterate on a single Frame (Default value is 5)
        solver.setTimesPerFrame(5);
        //Minimum distance between previous and current solution to consider that Solver converges (Default value is 0.01)
        solver.setMinDistance(0.5f);
        //solver.explore(false);

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

    public void draw() {
        background(0);
        if(scene.is3D()) lights();
        //scene.drawAxes();
        scene.render();

        /*
        if(enableRotation){
            Vector twist = skeleton.get(0).location(new Vector(), skeleton.get(skeleton.size() - 1));
            skeleton.get(0).rotate(new Quaternion(twist, radians(2)));
        }
        //Draw Twist Vector
        pushStyle();
        fill(43, 133, 229);
        noStroke();
        Vector end = Vector.add(skeleton.get(skeleton.size()-1).position(), skeleton.get(0).position());
        end.multiply(0.5f);
        scene.drawArrow(skeleton.get(0).position(), end, jointRadius / 2f);
        popStyle();

         */

        scene.beginHUD();
        text("IK with joint constraints", width/2, height/2 - 200);

        for (int i = 0; i < skeleton.size(); i++) {
            //Print Node names
            textAlign(CENTER, TOP);
            Vector screenLocation = scene.screenLocation(Vector.add(skeleton.get(i).position(), new Vector(0, jointRadius * 2)));
            text("Node " + i, screenLocation.x(), screenLocation.y());
        }
        //Print Target
        Vector screenLocation = scene.screenLocation(Vector.add(target.position(), new Vector(0, -jointRadius * 2)));
        textAlign(CENTER, BOTTOM);
        text("Target " , screenLocation.x(), screenLocation.y());

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

                if (constraint() != null) {
                    scene.drawConstraint(pg,this, 0.5f);
                }

                pg.hint(DISABLE_DEPTH_TEST);
                pg.strokeWeight(3);
                scene.drawAxes(pg,radius * 3);
                pg.hint(ENABLE_DEPTH_TEST);
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
        }if(key == 'R'){
            enableRotation = !enableRotation;
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.UnconstrainedVsConstrained"});
    }


}
