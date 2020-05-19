package ik.trik;
import nub.core.Node;
import nub.core.constraint.*;
import nub.ik.animation.*;
import nub.primitives.*;
import nub.processing.*;
import processing.core.*;
import processing.event.MouseEvent;

public class TRIKConstrained extends PApplet {
    int w = 1200;
    int h = 1200;
    //Choose a renderer P2D or P3D
    String renderer = P2D;

    Scene scene;
    float length = 50;
    //Skeleton structure defined above
    Skeleton skeleton;

    public void settings() {
        size(w, h, renderer);
    }

    public void setup() {
        //Setting the scene
        scene = new Scene(this);
        scene.setRadius(200);
        scene.fit(1);
        //Create the Skeleton described above
        skeleton = new Skeleton(scene);
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
        //Apply a Ball & Socket constraint to node0:
        if(scene.is3D()) {
            BallAndSocket constraint0 = new BallAndSocket(radians(40), radians(60));
            constraint0.setRestRotation(joint0.rotation(), new Vector(1, 0, 0), new Vector(0, 1, 0));
            constraint0.setTwistLimits(radians(50), radians(50));
            joint0.setConstraint(constraint0);
        }

        //Apply a Ball & Socket constraint to node1:
        BallAndSocket constraint1 = new BallAndSocket(radians(60), radians(40));
        if(scene.is3D()) {
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

    public void draw() {
        background(0);
        if(scene.is3D()) lights();
        scene.drawAxes();
        scene.render();

        noLights();
        scene.beginHUD();
        text("Basic Skeleton Structure with constraints", width /2, 100);
        for(Node joint : skeleton.BFS()){
            Vector screenLocation = scene.screenLocation(joint.position());
            String s = !joint.children().isEmpty() ? "" : "End effector: ";
            s += skeleton.jointName(joint);
            text(s , screenLocation.x(), screenLocation.y() + 30);
        }
        scene.endHUD();

    }

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

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.trik.TRIKConstrained"});
    }
}


