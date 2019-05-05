package ik.collada.test;

import ik.collada.colladaParser.colladaLoader.ColladaURDFLoader;
import ik.common.Joint;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.FixedConstraint;
import nub.ik.ChainSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.colladaLoader.ColladaBlenderLoader;
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
    String dae = "humanoid.dae";
    String tex = "texture.png";
    AnimatedModel model;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        randomSeed(14);
        this.g.textureMode(NORMAL);
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);

        model = ColladaURDFLoader.loadColladaModel("C:\\Users\\usuario\\Desktop\\Computer_Graphics_books\\Models\\collada-robots-collection-master\\kuka_kr16_2\\", "kuka_kr16_2.dae", scene);
        //model = ColladaURDFLoader.loadColladaModel("C:/Users/usuario/Desktop/Computer_Graphics_books/Models/collada-robots-collection-master/ur10_joint_limited_robot/", "ur10_joint_limited_robot.dae", scene);

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.fit();

        model.printNames();
        Target target = new Target(scene, ((Joint)model.getRootJoint()).radius()*1.2f);

        /*Chain solver*/
        List<Node> branch = scene.branch(model.getRootJoint());

        ChainSolver solver = new ChainSolver((ArrayList<? extends Node>) branch);
        solver.setKeepDirection(true);
        solver.setFixTwisting(true);

        solver.timesPerFrame = 5;
        solver.maxIter = 50;
        solver.error = solver.minDistance = scene.radius()*0.001f;
        solver.setTarget(branch.get(branch.size() - 1), target);
        target.setPosition(branch.get(branch.size() - 1).position());
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

    boolean constraint_hips = true;
    public void keyPressed(){
        if(key == 'h' || key == 'H'){
            if(constraint_hips) {
                System.out.println("Constraint Hips Enabled");
                model.getJoints().get("Torso").setConstraint(new FixedConstraint());
            } else {
                System.out.println("Constraint Hips Disabled");
                model.getJoints().get("Torso").setConstraint(null);
            }
            constraint_hips = !constraint_hips;
        }
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.collada.test.LoadURDF"});
    }
}