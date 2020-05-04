package ik.interactiveSkeleton;

import nub.ik.skinning.GPULinearBlendSkinning;
import nub.core.Node;
import nub.core.Graph;
import nub.core.Interpolator;
import nub.ik.solver.geometric.oldtrik.TRIK;
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
    List<Node> skeleton2;

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
        reference = new Node();
        //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
        //2.3 Create the Joints based on 2.2.
        List<Node> skeleton = fishSkeleton(reference);

        skeleton2 = fishSkeleton(reference);
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
        target.setReference(skeleton2.get(0));
        //Make target to be on same position/orientation as endEffector
        target.setPosition(endEffector.position());

        //4.4 Relate target(s) with end effector(s)
        //scene.addIKTarget(endEffector, target);
        solver.setTarget(target);

        //Generates a default Path that target must follow
        targetInterpolator = setupTargetInterpolator(target);

        TimingTask solverTask = new TimingTask() {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                //solver.solve();
            }
        };
        solverTask.run(40); //Execute the solverTask each 40 ms
    }

    public Node createTarget(float radius){
        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        Node target = new Node(redBall);
        target.setPickingThreshold(0);
        return target;
    }


    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        //Render mesh with respect to the node
        skinning.render(scene, reference);
        if(showSkeleton) scene.render();

        scene.drawCatmullRom(targetInterpolator);
    }

    public List<Node> fishSkeleton(Node reference) {
        Joint j1 = new Joint();
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Joint j2 = new Joint();
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Joint j3 = new Joint();
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Joint j4 = new Joint();
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Joint j5 = new Joint();
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Joint j6 = new Joint();
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        j1.setRoot(true);
        return scene.branch(j1);
    }

    public Interpolator setupTargetInterpolator(Node target) {
        Node ref = skeleton2.get(2);
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.enableRecurrence();
        targetInterpolator.setSpeed(3.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2.0f * PI / (nbKeyFrames - 1);
        for (int i = 0; i < nbKeyFrames; i++) {
            Node iFrame = new Node();
            iFrame.setReference(ref);
            Vector v = iFrame.location(target);
            iFrame.setTranslation(new Vector(100 * sin(step * i) + v.x(), v.y(), v.z()));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.run();
        return targetInterpolator;
    }

    public void keyPressed() {
        if(key == 'S' || key == 's'){
            showSkeleton = !showSkeleton;
        }
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.mouseSpin();
        } else if (mouseButton == RIGHT) {
            scene.mouseTranslate();
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