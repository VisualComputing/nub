package ik.interactiveSkeleton;

/**
 * Created by sebchaparr on 17/07/18.
 */

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.Node;
import frames.ik.CCDSolver;
import frames.ik.Solver;
import frames.primitives.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.primitives.constraint.Hinge;
import frames.primitives.constraint.PlanarPolygon;
import frames.processing.Scene;
import frames.processing.Shape;
import frames.timing.TimingTask;
import ik.common.Joint;
import ik.common.LinearBlendSkinningGPU;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class Puppet extends PApplet {
    Scene scene;
    Node eye;
    HashMap<String, ArrayList<Joint>> limbs;
    HashMap<String, Target> targets;
    String[] keys = {"LeftArm", "RightArm", "LeftFoot", "RightFoot"};
    float boneLenght = 50;


    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);
        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultGrabber(eye);
        scene.fitBallInterpolation();

        targets = new HashMap<>();
        limbs = new HashMap<>();

        for(String key : keys){
            targets.put(key, new Target(scene));
        }

        //right arm
        limbs.put("LeftArm", limb(new Vector(10,0,0), new Vector(1,0,0), boneLenght, new Vector(0,0,10)));
        limbs.put("RightArm", limb(new Vector(-10,0,0), new Vector(-1,0,0), boneLenght, new Vector(0,0,10)));
        limbs.put("LeftFoot", limb(new Vector(10,10,0), new Vector(0,1,0), boneLenght, new Vector(-10,0,0)));
        limbs.put("RightFoot", limb(new Vector(-10,10,0), new Vector(0,1,0), boneLenght, new Vector(-10,0,0)));

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
        for (Node frame : scene.nodes()) {
            if (frame instanceof Shape) ((Shape) frame).draw();
        }
    }


    public ArrayList<Joint> limb(Vector origin, Vector direction, float boneLength, Vector hinge) {
        ArrayList<Joint> skeleton = new ArrayList<Joint>();
        Vector bone = direction.normalize(null);
        bone.multiply(boneLength);
        Joint j1 = new Joint(scene);
        j1.setPosition(origin);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.translate(bone);
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
        c1.setHeight(boneLength / 2.f);
        c1.setAngle(radians(40));
        Vector twist = j2.translation().get();
        c1.setRestRotation(j2.rotation().get(), twist.orthogonalVector(), twist);
        j1.setConstraint(c1);

        float constraint_factor_x = 170;
        Hinge c2 = new Hinge(radians(0), radians(constraint_factor_x));
        c2.setRestRotation(j2.rotation().get());
        c2.setAxis(Vector.projectVectorOnPlane(hinge, j2.translation()));
        if(Vector.squaredNorm(c2.axis()) != 0) {
            j2.setConstraint(c2);
        }

        j1.setRoot(true);
        skeleton.add(j1);
        skeleton.add(j2);
        skeleton.add(j3);

        return skeleton;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.Puppet"});
    }
}