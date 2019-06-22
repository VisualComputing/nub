package ik.collada.test;

import ik.basic.Util;
import nub.ik.loader.collada.URDFLoader;
import nub.ik.visual.Joint;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.numerical.SDLSSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.ik.loader.collada.data.Model;
import nub.timing.TimingTask;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 08/05/19.
 */
public class BenchmarkUR10 extends PApplet {
    Scene scene;
    String path = "/testing/data/dae/";
    String dae = "ur10_joint_limited_robot.dae";
    Model[] models = new Model[4];
    String solvers_type[] = {"FABRIK", "BIOIK", "CCD", "NUMERICAL"};
    List<Solver> solvers = new ArrayList<>();
    List<Vector> positions = new ArrayList<>();
    List<Target> targets = new ArrayList<>();
    int[] enable = {0,1,2};


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

        for(int k = 0; k < enable.length; k++){
            int i = enable[k];
            models[i] = URDFLoader.loadColladaModel(sketchPath() + path, dae, scene);
            Model model = models[i];

            model.printNames();
            Target target = new Target(scene, ((Joint) model.root()).radius());

            /*Adding constraints*/
            Node node_1 = model.skeleton().get("node1");
            Hinge hinge_1 = new Hinge(radians(180),radians(180), node_1.rotation(), new Vector(1,0,0), new Vector(0,0,1));
            node_1.setConstraint(hinge_1);


            Node node_3 = model.skeleton().get("node3");
            Hinge hinge_3 = new Hinge(radians(180),radians(180), node_3.rotation(), new Vector(1,0,0), new Vector(0,1,0));
            node_3.setConstraint(hinge_3);

            Node node_4 = model.skeleton().get("node4");
            Hinge hinge_4 = new Hinge(radians(180),radians(180), node_4.rotation(), new Vector(1,0,0), new Vector(0,0,1));
            node_4.setConstraint(hinge_4);

            Node node_6 = model.skeleton().get("node6");
            Hinge hinge_6 = new Hinge(radians(180),radians(180), node_6.rotation(), new Vector(0,0,1), new Vector(0,1,0));
            node_6.setConstraint(hinge_6);

            Node node_8 = model.skeleton().get("node8");
            Hinge hinge_8 = new Hinge(radians(180),radians(180), node_8.rotation(), new Vector(0,0,1), new Vector(0,1,0));
            node_8.setConstraint(hinge_8);

            Node node_9 = model.skeleton().get("node9");
            Hinge hinge_9 = new Hinge(radians(180),radians(180), node_9.rotation(), new Vector(1,0,0), new Vector(0,0,1));
            node_9.setConstraint(hinge_9);

            Node node_10 = model.skeleton().get("node10");
            Hinge hinge_10 = new Hinge(radians(180),radians(180), node_10.rotation(), new Vector(0,0,1), new Vector(0,1,0));
            node_10.setConstraint(hinge_10);

            //Cull unnecesary nodes
            model.skeleton().get("node2").cull();
            model.skeleton().get("node1").reference().translate(new Vector(i * scene.radius() * 2,i * scene.radius() * 2, 0));

            //Adding solver
            Solver solver;
            List<Node> branch = scene.path(model.skeleton().get("node1"), model.skeleton().get("node2"));

            switch (solvers_type[i]){
                case "FABRIK":{
                    solver = new ChainSolver( branch);
                    ((ChainSolver)solver).setKeepDirection(true);
                    ((ChainSolver)solver).setFixTwisting(true);
                    ((ChainSolver)solver).explore(true);
                    break;
                }
                case "BIOIK":{
                    solver = new BioIk(branch, 10, 4);
                    break;
                }
                case "CCD":{
                    solver = new CCDSolver( branch);
                    break;
                }
                case "NUMERICAL":{
                    solver = new SDLSSolver( branch);
                    break;
                }
                default:{
                    solver = new ChainSolver( branch);
                    ((ChainSolver)solver).setKeepDirection(true);
                    ((ChainSolver)solver).setFixTwisting(true);
                    break;
                }
            }

            solver.setTimesPerFrame(10);
            solver.setMaxIterations(50);
            solver.setMaxError(scene.radius() * 0.01f); solver.setMinDistance(scene.radius() * 0.01f);
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
            solvers.add(solver);
            positions.add(model.skeleton().get("node1").reference().translation());
            targets.add(target);
        }

        scene.eye().rotate(new Quaternion(new Vector(1,0,0), PI/2));
        scene.eye().rotate(new Quaternion(new Vector(0,0,1), PI));
        scene.setRadius(scene.radius() * enable.length * 3);
        scene.fit();
    }
    public void draw() {
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        scene.beginHUD();
        for (String s : models[0].skeleton().keySet()) {
            Node n = models[0].skeleton().get(s);
            if(n.isCulled() || !n.isTrackingEnabled()) continue;
            Vector sc = scene.screenLocation(new Vector(), n);
            text(s, sc.x(), sc.y());
        }

        for(int i = 0 ; i < solvers.size(); i++){
            Util.printInfo(scene, solvers.get(i), positions.get(i));
        }
        scene.endHUD();
    }

    @Override
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged() {
        if (mouseButton == LEFT){
            scene.spin();
        } else if (mouseButton == RIGHT) {
            if(targets.contains(scene.trackedNode())){
                for(Node target : targets) scene.translate(target);
            }else{
                scene.translate();
            }
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
        PApplet.main(new String[]{"ik.collada.test.BenchmarkUR10"});
    }
}