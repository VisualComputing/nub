package ik.interactiveSkeleton;

import nub.ik.skinning.GPULinearBlendSkinning;
import nub.core.Node;
import nub.core.Graph;
import nub.core.Interpolator;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.TRIK;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.ik.visual.Joint;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;
import java.util.List;

/**
 * Created by sebchaparr on 11/03/18.
 */

// TODO: Check for flow field with noise and brownian motion
public class InteractiveFish extends PApplet {

    boolean showSkeleton = false;
    Scene scene;
    Node reference;

    GPULinearBlendSkinning skinning;

    Node target;
    Interpolator targetInterpolator;
    String shapePath = "/testing/data/objs/fish0.obj";
    String texturePath = "/testing/data/objs/fish0.jpg";

    float targetRadius = 7;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.axes = true;
        //1. Create and set the scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRightHanded();
        scene.setRadius(200);
        scene.fit(1);
        //2. Define the Skeleton
        //2.1 Define a reference node to the skeleton and the mesh
        reference = new Node(scene);
        //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
        //2.3 Create the Joints based on 2.2.
        List<Node> skeleton = fishSkeleton(reference);
        //3. Relate the shape with a skinning method (CPU or GPU)
        skinning = new GPULinearBlendSkinning(skeleton, this.g, sketchPath() + shapePath, sketchPath() + texturePath, 200, true);

        //4. Adding IK behavior
        //4.1 Identify root and end effector(s) (first and last joint on the skeleton)
        Node root = skeleton.get(0);
        Node endEffector = skeleton.get(skeleton.size()- 1);

        //4.2 relate a skeleton with an IK Solver
        TRIK solver = new TRIK(skeleton);
        //Update params
        solver.enableTwistHeuristics(false);
        //solver.smooth(true);
        solver.setTimesPerFrame(2);
        solver.setMaxIterations(2);
        //solver.setMaxError(1f);

        //4.3 Create target(s) to relate with End Effector(s)
        target = createTarget(targetRadius);
        //Target also depends on reference
        target.setReference(reference);
        //Make target to be on same position/orientation as endEffector
        target.setPosition(endEffector.position());

        //4.4 Relate target(s) with end effector(s)
        //scene.addIKTarget(endEffector, target);
        solver.setTarget(target);

        //Generates a default Path that target must follow
        targetInterpolator = setupTargetInterpolator(target);

        TimingTask solverTask = new TimingTask(scene) {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                solver.solve();
            }
        };
        solverTask.run(40); //Execute the solverTask each 40 ms

    }

    public Node createTarget(float radius){
        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        Node target = new Node(scene, redBall);
        target.setPickingThreshold(0);
        return target;
    }


    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        //Render mesh with respect to the node
        skinning.render(reference);
        if(showSkeleton) scene.render();
    }

    public List<Node> fishSkeleton(Node reference) {
        Joint j1 = new Joint(scene);
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Joint j4 = new Joint(scene);
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Joint j5 = new Joint(scene);
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Joint j6 = new Joint(scene);
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        j1.setRoot(true);
        return scene.branch(j1);
    }

    public Interpolator setupTargetInterpolator(Node target) {
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.setLoop();
        targetInterpolator.setSpeed(3.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2.0f * PI / (nbKeyFrames - 1);
        for (int i = 0; i < nbKeyFrames; i++) {
            Node iFrame = new Node(scene);
            iFrame.setReference(target.reference());
            iFrame.setTranslation(new Vector(100 * sin(step * i), target.translation().y(), target.translation().z()));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.start();
        return targetInterpolator;
    }

    public void keyPressed() {
        if(key == 'S' || key == 's'){
            showSkeleton = !showSkeleton;
        }
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
            scene.scale(scene.mouseDX());
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
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveFish"});
    }
}