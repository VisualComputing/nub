package ik.collada.test;

import ik.collada.colladaParser.colladaLoader.ColladaURDFLoader;
import ik.common.Joint;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.collada.animation.AnimatedModel;
import nub.timing.TimingTask;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 23/07/18.
 */
public class LoadURDF extends PApplet {
    Scene scene;
    String path = "/testing/data/dae/";
    String[] daes = {"ur10_joint_limited_robot.dae", "kuka_kr16_2.dae", "nasa_valkyrie.dae"};
    int dae = 2; //choose between example models
    AnimatedModel model;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.axes = true;
        Joint.markers = true;
        randomSeed(14);
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);

        model = ColladaURDFLoader.loadColladaModel(sketchPath() + path, daes[dae], scene);

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fit();

        model.printNames();
        if(dae != 2) {
            Target target = new Target(scene, ((Joint) model.getRootJoint()).radius());
            /*Chain solver*/

            List<Node> branch = scene.path(model.getJoints().get("node1"), model.getJoints().get(dae == 0 ? "node10" : "node8"));

            ChainSolver solver = new ChainSolver((ArrayList<? extends Node>) branch);
            solver.setKeepDirection(true);
            solver.setFixTwisting(true);

            solver.setTimesPerFrame(5);
            solver.setMaxIterations(50);
            solver.setMaxError(scene.radius() * 0.001f);
            solver.setMinDistance(scene.radius() * 0.001f);
            solver.setTarget(branch.get(branch.size() - 1), target);
            target.setPosition(branch.get(branch.size() - 1).position().get());
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
        if(dae != 3) {
            scene.beginHUD();
            for (String s : model.getJoints().keySet()) {
                Node n = model.getJoints().get(s);
                Vector sc = scene.screenLocation(new Vector(), n);
                text(s, sc.x(), sc.y());
            }
            scene.endHUD();
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
        PApplet.main(new String[]{"ik.collada.test.LoadURDF"});
    }
}