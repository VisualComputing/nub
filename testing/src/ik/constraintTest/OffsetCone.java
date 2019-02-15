package ik.constraintTest;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.FixedConstraint;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.ik.TreeSolver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 15/02/19.
 */

public class OffsetCone extends PApplet {
    Scene scene;
    ArrayList<ChainSolver> chain_solvers = new ArrayList<ChainSolver>();
    ArrayList<Frame> targets = new ArrayList<Frame>();

    float radius = 10f;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(200);
        scene.fit();
        scene.setRightHanded();

        PShape redBall = createShape(SPHERE, radius*2);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        Frame target = new Frame(scene, redBall);
        target.setPickingThreshold(0);

        //Create a simple limb
        Joint j1 = new Joint(scene, radius);
        j1.setTranslation(0,90,0);
        Joint j2 = new Joint(scene, radius);
        j2.setReference(j1);
        j2.setTranslation(50,30,0);
        Joint j3 = new Joint(scene, radius);
        j3.setReference(j2);
        j3.setTranslation(50,-80,0);
        Joint j4 = new Joint(scene, radius);
        j4.setReference(j3);
        j4.setTranslation(0,-80,0);
        j1.setRoot(true);

        j1.setConstraint(new FixedConstraint());

        BallAndSocket c2 = new BallAndSocket(radians(89), radians(89), radians(89), radians(89));
        c2.setRestRotation(j1.rotation().get(),new Vector(0,1,0), new Vector(1,0,0), j3.translation().get());
        j2.setConstraint(c2);

        BallAndSocket c3 = new BallAndSocket(radians(10), radians(40), radians(40), radians(40));
        c3.setRestRotation(j1.rotation().get(),new Vector(0,0,1), new Vector(0,-1,0), j4.translation().get());
        j3.setConstraint(c3);


        target.setPosition(j4.position());
        ChainSolver solver = new ChainSolver(new ArrayList<>(scene.branch(j1)));
        //CCDSolver solver = new CCDSolver(new ArrayList<>(scene.branch(j1)));
        //TreeSolver solver = new TreeSolver(j1);
        solver.setTarget(target);
        //solver.addTarget(j4, target);
        TimingTask task = new TimingTask() {
            @Override
            public void execute() {
                solver.solve();
            }
        };
        scene.registerTask(task);
        task.run(40);

    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
    }

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
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
        PApplet.main(new String[]{"ik.constraintTest.OffsetCone"});
    }
}

