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
 * As Joint 4 is the End effector of the structure (leaf node) we will attach a Target to it.
 * Press 'S' to stop/restart IK Solver
 */

import nub.primitives.*;
import nub.core.*;
import nub.processing.*;
import nub.core.constraint.*;
//this packages are required for ik behavior
import nub.ik.animation.*;
import nub.ik.solver.*;

int w = 1200;
int h = 1200;

//Choose a renderer P2D or P3D
String renderer = P2D;

Scene scene;
float length = 50;
//Skeleton structure defined above
Skeleton skeleton;

void settings() {
    size(w, h, renderer);
}

void setup() {
    //Setting the scene
    scene = new Scene(this);
    scene.setRadius(200);
    scene.fit(1);
    //Create the Skeleton described above
    skeleton = new Skeleton(scene);
    /*
      A joint is a node with a predefined visual representation.
      To add a joint to the Skeleton you must use either the method skeleton.addJoint(name)
      or skeleton.addJoint(name, reference_name).
      Each joint has a unique name that will be use later by the IK solver.            
    */
    Joint joint0 = skeleton.addJoint("Joint 0");
    joint0.translate(new Vector(0,-scene.radius()/2));
    Joint joint1 = skeleton.addJoint("Joint 1", "Joint 0"); 
    joint1.translate(new Vector(0,length));
    Joint joint2 = skeleton.addJoint("Joint 2","Joint 1");
    joint2.translate(new Vector(0,length));
    Joint joint3 = skeleton.addJoint("Joint 3", "Joint 2");
    joint3.translate(new Vector(0,length));
    Joint joint4 = skeleton.addJoint("Joint 4", "Joint 3");
    joint4.translate(new Vector(0,length));

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
    if(scene.is3D()){
      BallAndSocket constraint0 = new BallAndSocket(radians(40), radians(60));
      constraint0.setRestRotation(joint0.rotation(), new Vector(1,0,0), new Vector(0,1,0));
      constraint0.setTwistLimits(radians(50), radians(50));
      joint0.setConstraint(constraint0);
      //Apply a Ball & Socket constraint to node1:
      BallAndSocket constraint1 = new BallAndSocket(radians(60), radians(40));
      constraint1.setRestRotation(joint1.rotation(), new Vector(1,0,0), new Vector(0,1,0));
      constraint1.setTwistLimits(radians(5), radians(5));
      joint1.setConstraint(constraint1);
    }

    //Apply a Hinge constraint to node2:
    Hinge constraint2 = new Hinge(radians(40), radians(60));
    Vector twist = scene.is3D() ? new Vector(1,0,0) : new Vector(0,0,1);
    constraint2.setRestRotation(joint2.rotation(), new Vector(0,1,0), twist);
    joint2.setConstraint(constraint2);

    //Apply a Hinge constraint to node3:
    Hinge constraint3 = new Hinge(radians(60), radians(40));
    constraint3.setRestRotation(joint4.rotation(), new Vector(0,1,0), new Vector(0,0,1));
    joint3.setConstraint(constraint3);
    //---------------------------------------------------


    //Enable IK functionallity
    skeleton.enableIK();
    //Lets create a Targets indicating the name of the leaf nodes. 
    skeleton.addTarget("Joint 4");
    //If desired you could set the target position and orientation to be the same as the leaves of the structure 
    skeleton.restoreTargetsState();
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
    text("Basic Skeleton Structure with constraints", width /2, 100);
    for(Node joint : skeleton.joints().values()){
        Vector screenLocation = scene.screenLocation(joint.position());
        String s = !joint.children().isEmpty() ? "" : "End effector: ";
        s += skeleton.jointName(joint);
        text(s , screenLocation.x(), screenLocation.y() + 30);
    }
    scene.endHUD();
    
}

void mouseMoved() {
    scene.mouseTag();
}

void mouseDragged() {
    if (mouseButton == LEFT){
        scene.mouseSpin();
    } else if (mouseButton == RIGHT) {
        scene.mouseTranslate();
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
