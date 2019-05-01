package ik.interactiveSkeleton;

/**
 * Created by sebchaparr on 17/07/18.
 */

import nub.core.Graph;
import nub.core.Interpolator;
import nub.core.Node;
import nub.ik.CCDSolver;
import nub.ik.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.core.constraint.Hinge;
import nub.core.constraint.PlanarPolygon;
import nub.processing.Scene;
import nub.timing.TimingTask;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class Puppet extends PApplet {
    //TODO : Update
    Scene scene;
    HashMap<String, ArrayList<Joint>> limbs;
    HashMap<String, Node> targets;
    String[] keys = {"LeftArm", "RightArm", "LeftFoot", "RightFoot"};
    float boneLenght = 50;
    float targetRadius = 7;


    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(boneLenght * 5);
        scene.fit(1);
        //scene.disableBackBuffer();

        targets = new HashMap<>();
        limbs = new HashMap<>();

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        for(String key : keys){
            Node target = new Node(scene, redBall);
            target.setPickingThreshold(0);
            targets.put(key, target);
        }

        //right arm
        limbs.put("LeftArm", limb(new Vector(10,0,0), new Vector(1,0,0), new Vector(0,1,0), boneLenght, new Vector(10,0,0)));
        limbs.put("RightArm", limb(new Vector(-10,0,0), new Vector(-1,0,0), new Vector(0,1,0), boneLenght, new Vector(10,0,0)));
        limbs.put("LeftFoot", limb(new Vector(10,10,0), new Vector(0,1,0), new Vector(0,1,0), boneLenght, new Vector(-10,0,0)));
        limbs.put("RightFoot", limb(new Vector(-10,10,0), new Vector(0,1,0), new Vector(0,1,0), boneLenght, new Vector(-10,0,0)));

        for(String key : keys){
            ArrayList<Joint> skeleton = limbs.get(key);
            targets.get(key).setPosition(skeleton.get(skeleton.size()-1).position());
            //TESTING WITH FABRIK
            Solver solver = scene.registerTreeSolver(skeleton.get(0));
            solver.maxIter = 100;
            scene.addIKTarget(skeleton.get(skeleton.size() - 1), targets.get(key));
            //TESTING WITH CCD
            /*
            CCDSolver solver = new CCDSolver(skeleton);
            solver.setTarget(targets.get(key));
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    solver.solve();
                }
            };
            scene.registerTask(task);
            task.run(40);
            */
        }
    }
    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
    }


    public ArrayList<Joint> limb(Vector origin, Vector d1, Vector d2, float boneLength, Vector hinge) {
        ArrayList<Joint> skeleton = new ArrayList<Joint>();
        Vector bone = d1.normalize(null);
        bone.multiply(boneLength);
        Joint j1 = new Joint(scene);
        j1.setPosition(origin);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.translate(bone);
        bone = d2.normalize(null);
        bone.multiply(boneLength);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        j3.translate(bone);

        //APPLY CONSTRAINTS
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        int sides = 4;
        float radius = 40;
        float step = 2*PI/sides;
        for(int i = 0; i < sides; i++){
            float angle = i*step;
            vertices.add(new Vector(radius*cos(angle), radius*sin(angle)));
        }

        PlanarPolygon c1 = new PlanarPolygon(vertices);
        c1.setAngle(radians(40));
        Vector twist = j2.translation().get();
        c1.setRestRotation(j2.rotation().get(), twist.orthogonalVector(), twist);
        j1.setConstraint(c1);

        float constraint_factor_x = 170;
        Hinge c2 = new Hinge(radians(0), radians(constraint_factor_x),j2.rotation().get(), j3.translation(),hinge);
        //c2.setAxis(Vector.projectVectorOnPlane(hinge, j2.frame.translation()));
        //if(Vector.squaredNorm(c2.axis()) != 0) {
            System.out.println("axis : " + c2.orientation().rotate(new Vector(0,0,1)));
            j2.setConstraint(c2);
        //}

        j1.setRoot(true);
        skeleton.add(j1);
        skeleton.add(j2);
        skeleton.add(j3);

        return skeleton;
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
        Node f = scene.trackedNode();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.trackedNode().rotate(new Quaternion( c.orientation().rotate(new Vector(0,0,1)), radians(5)));

    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.Puppet"});
    }
}