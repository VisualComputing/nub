package ik.constraintTest;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.constraint.BallAndSocket;
import frames.core.constraint.FixedConstraint;
import frames.core.constraint.Hinge;
import frames.core.constraint.PlanarPolygon;
import frames.ik.CCDSolver;
import frames.ik.ChainSolver;
import frames.ik.Solver;
import frames.ik.evolution.BioIk;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.timing.TimingTask;
import ik.basic.Util;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
public class Case1 extends PApplet {
    Scene scene;
    float boneLenght = 50;
    float targetRadius = 7;

    ArrayList<Frame>[] skeletons = new ArrayList[3];
    Frame[] targets = new Frame[3];
    Solver[] solvers = new Solver[3];

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.setRadius(boneLenght * 5);

        scene.fit(1);

        PShape redBall = createShape(SPHERE, targetRadius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        //create targets
        for(int i = 0; i < targets.length; i++) {
            Frame target = new Frame(scene, redBall);
            target.setPickingThreshold(0);
            targets[i] = target;
        }

        //create skeleton
        for(int i = 0; i < skeletons.length; i++) {
            skeletons[i] = generateSkeleton(new Vector(0.8f*(scene.radius()*i - scene.radius()), 0, 0));
            targets[i].setPosition(skeletons[i].get(skeletons[i].size() - 1).position().get());
        }

        //create solvers
        solvers[0] = new CCDSolver(skeletons[0]);
        solvers[1] = new ChainSolver(skeletons[1]);
        solvers[2] = new BioIk(skeletons[2], 10, 4);

        for(int i = 0; i < solvers.length; i++) {
            Solver solver = solvers[i];
            solver.setTarget(skeletons[i].get(skeletons[i].size() - 1) , targets[i]);
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    solver.solve();
                }
            };
            scene.registerTask(task);
            task.run(40);
        }
    }

    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        for(int i = 0; i < solvers.length; i++) {
            Util.printInfo(scene, solvers[i], skeletons[i].get(0).position());
        }
        scene.endHUD();

    }

    public ArrayList<Frame> generateSkeleton(Vector position){
        //3-Segment-Arm
        ArrayList<Frame> skeleton = new ArrayList<>();
        Joint j1 = new Joint(scene);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.translate(boneLenght, 0, 0);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        Vector v = new Vector(boneLenght, -boneLenght, 0);
        v.normalize();
        v.multiply(boneLenght);
        j3.translate(v);
        j1.setRoot(true);
        j1.translate(position);
        skeleton.add(j1);
        skeleton.add(j2);
        skeleton.add(j3);
        //Add constraints
        BallAndSocket c1 = new BallAndSocket(radians(45),radians(45));
        c1.setRestRotation(j1.rotation().get(), new Vector(0, 1, 0), new Vector(1, 0, 0));
        j1.setConstraint(c1);

        Hinge c2 = new Hinge(0,radians(120));
        c2.setRestRotation(j2.rotation().get());
        c2.setAxis(j2.rotation().get(), new Vector(0,0,-1));
        j2.setConstraint(c2);
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
            for(Frame target : targets){
                if(scene.trackedFrame() == target){
                    for(Frame t : targets) scene.translate(t);
                    return;
                }
            }
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
        Frame f = scene.trackedFrame();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.trackedFrame().rotate(new Quaternion(c.axis(), radians(5)));
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.constraintTest.Case1"});
    }
}