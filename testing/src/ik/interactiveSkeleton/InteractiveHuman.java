package ik.interactiveSkeleton;

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import ik.common.LinearBlendSkinningGPU;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PShape;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 24/03/18.
 */
public class InteractiveHuman extends PApplet{
    //TODO : FIX SKINNING WITH ANOTHER ALGORITHM FOR WEIGHT CALCULATION
    Scene scene;
    Node eye;
    InteractiveShape shape;
    Joint root;
    //Uncomment to use Linear Blending Skinning with CPU
    LinearBlendSkinning skinning;
    //LinearBlendSkinningGPU skinning;
    HashMap<String, Target> targets;
    HashMap<String,Joint> limbs = new HashMap<String, Joint>();

    String shapePath = "/testing/data/objs/Female_low_poly.obj";

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

        targets = new HashMap<String, Target>();
        limbs = new HashMap<String, Joint>();

        targets.put("LEFTHAND", new Target(scene));
        targets.put("RIGHTHAND", new Target(scene));
        targets.put("LEFTFOOT", new Target(scene));
        targets.put("RIGHTFOOT", new Target(scene));
        targets.put("HEAD", new Target(scene));

        PShape model = loadShape(sketchPath() + shapePath);
        model.setFill(color(255,0,0, 50));

        Vector[] box = getBoundingBox(model);
        //Scale model
        float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
        //Invert Y Axis and set Fill
        shape = new InteractiveShape(scene, model);
        shape.setPrecision(Node.Precision.FIXED);
        shape.setPrecisionThreshold(1);
        shape.rotate(new Quaternion(new Vector(0,0,1), PI));
        shape.scale(200.f*1.f/max);
        root = humanSkeleton(shape);

        ArrayList<Node> skeleton = scene.branch(root);

        //Uncomment to use Linear Blending Skinning with CPU
        skinning = new LinearBlendSkinning(shape, model);
        skinning.setup(skeleton);
        //skinning = new LinearBlendSkinningGPU(shape, skeleton);
        //skinning.setSkinning(this, scene);
        //Adding IK behavior
        Solver solver = scene.registerTreeSolver(root);
        solver.timesPerFrame = 100;
        solver.error = 0.05f;
        scene.addIKTarget(limbs.get("LEFTHAND"), targets.get("LEFTHAND"));
        scene.addIKTarget(limbs.get("RIGHTHAND"), targets.get("RIGHTHAND"));
        scene.addIKTarget(limbs.get("LEFTFOOT"), targets.get("LEFTFOOT"));
        scene.addIKTarget(limbs.get("RIGHTFOOT"), targets.get("RIGHTFOOT"));
        scene.addIKTarget(limbs.get("HEAD"), targets.get("HEAD"));
    }

    public void draw(){
        //skinning.updateParams();
        background(0);
        lights();
        //Draw Constraints
        for(Node node : scene.nodes()){
            if(node instanceof InteractiveShape && node != shape)((InteractiveShape)node).draw();
        }
        //comment this line if you're using Linear Blending Skinning with CPU
        //shader(skinning.shader);
        shape.draw();
        //resetShader();
        //Uncomment to use Linear Blending Skinning with CPU
        skinning.applyTransformations();
    }

    public static  Vector[] getBoundingBox(PShape shape) {
        Vector v[] = new Vector[2];
        float minx = 999;  float miny = 999;
        float maxx = -999; float maxy = -999;
        float minz = 999;  float maxz = -999;
        for(int j = 0; j < shape.getChildCount(); j++){
            PShape aux = shape.getChild(j);
            for(int i = 0; i < aux.getVertexCount(); i++){
                float x = aux.getVertex(i).x;
                float y = aux.getVertex(i).y;
                float z = aux.getVertex(i).z;
                minx = minx > x ? x : minx;
                miny = miny > y ? y : miny;
                minz = minz > z ? z : minz;
                maxx = maxx < x ? x : maxx;
                maxy = maxy < y ? y : maxy;
                maxz = maxz < z ? z : maxz;
            }
        }

        v[0] = new Vector(minx,miny, minz);
        v[1] = new Vector(maxx,maxy, maxz);
        return v;
    }

    public Joint humanSkeleton(Node reference){
        //root
        Joint j1 = new Joint(scene);
        j1.setReference(reference);
        j1.setScaling(1.f/reference.scaling());
        j1.setPosition(-0.6260768f, -103.30267f, -7.696819E-6f);
        j1.setRoot(true);
        //Right leg
        Node j1_1 = new Node(scene);
        j1_1.setReference(j1);
        j1_1.setPosition(j1.position());
        scene.inputHandler().removeGrabber(j1_1);
        Joint j2 = new Joint(scene);
        j2.setReference(j1_1);
        j2.setPosition(11.2693815f, -103.30267f, -7.692007E-6f);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        j3.setPosition(10.643305f, -53.299656f, -4.9683833f);
        Joint j4 = new Joint(scene);
        j4.setReference(j3);
        j4.setPosition(10.480495f, -5.253174f, -4.9646983f);
        //set Target position
        limbs.put("RIGHTFOOT", j4);
        targets.get("RIGHTFOOT").setPosition(10.480495f, -5.253174f, -4.9646983f);
        //Left Leg
        Node j1_2 = new Node(scene);
        j1_2.setReference(j1);
        j1_2.setPosition(j1.position());
        scene.inputHandler().removeGrabber(j1_2);
        Joint j5 = new Joint(scene);
        j5.setReference(j1_2);
        j5.setPosition(-10.643305f, -103.30267f, -7.692444E-6f);
        Joint j6 = new Joint(scene);
        j6.setReference(j5);
        j6.setPosition(-10.643306f, -53.18076f, -4.952074f);
        Joint j7 = new Joint(scene);
        j7.setReference(j6);
        j7.setPosition(-10.802915f, -6.01915f, -4.9398084f);
        limbs.put("LEFTFOOT", j7);
        targets.get("LEFTFOOT").setPosition(-10.802915f, -6.01915f, -4.9398084f);
        //middle
        Node j1_3 = new Node(scene);
        j1_3.setReference(j1);
        j1_3.setPosition(j1.position());
        scene.inputHandler().removeGrabber(j1_3);
        Joint j8 = new Joint(scene);
        j8.setReference(j1_3);
        j8.setPosition(0.07123697f, -116.91087f, 0.48342294f);
        //middle
        Joint j9 = new Joint(scene);
        j9.setReference(j8);
        j9.setPosition(0.7575269f, -133.32848f, -2.5342464f);
        //neck
        Joint j10 = new Joint(scene);
        j10.setReference(j9);
        j10.setPosition(0.7575253f, -166.21028f, -2.5342464f);
        //head
        Joint j11 = new Joint(scene);
        j11.setReference(j10);
        j11.setPosition(0.13711494f, -184.20218f, -2.5342464f);
        limbs.put("HEAD", j11);
        targets.get("HEAD").setPosition(0.13711494f, -184.20218f, -2.5342464f);
        //right arm
        Joint j12 = new Joint(scene);
        j12.setReference(j9);
        j12.setPosition(8.822862f, -160.62653f, -2.5342464f);
        Joint j13 = new Joint(scene);
        j13.setReference(j12);
        j13.setPosition(20.09208f, -153.79529f, -9.227346f);
        Joint j14 = new Joint(scene);
        j14.setReference(j13);
        j14.setPosition(31.397337f, -126.518745f, -9.82052f);
        Joint j15 = new Joint(scene);
        j15.setReference(j14);
        j15.setPosition(42.670765f, -99.90062f, 0.21301739f);
        limbs.put("RIGHTHAND", j15);
        targets.get("RIGHTHAND").setPosition(42.670765f, -99.90062f, 0.21301739f);
        //left arm
        Joint j16 = new Joint(scene);
        j16.setReference(j9);
        j16.setPosition(-7.3889947f, -160.39792f, -1.9553403f);
        Joint j17 = new Joint(scene);
        j17.setReference(j16);
        j17.setPosition(-19.736198f, -154.72823f, -11.76652f);
        Joint j18 = new Joint(scene);
        j18.setReference(j17);
        j18.setPosition(-31.41613f, -127.28866f, -9.370963f);
        Joint j19 = new Joint(scene);
        j19.setReference(j18);
        j19.setPosition(-42.803642f, -98.48643f, 1.502241f);
        limbs.put("LEFTHAND", j19);
        targets.get("LEFTHAND").setPosition(-42.803642f, -98.48643f, 1.502241f);
        return j1;
    }
    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveHuman"});
    }

}

