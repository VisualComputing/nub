package ik.trik.mocap;

import ik.basic.Util;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.loader.bvh.BVHLoader;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.CCDSolver;
import nub.ik.solver.geometric.ChainSolver;
import nub.ik.solver.geometric.oldtrik.TRIKTree;
import nub.ik.solver.trik.implementations.SimpleTRIK;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Viewer extends PApplet{
    String path =  "/testing/data/bvh/mocap.bvh";
    Scene scene;
    BVHLoader parser;
    List<Skeleton> skeletons;

    boolean readNext = false;
    boolean solve = false;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.axes = true;
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(100);
        scene.eye().rotate(new Quaternion(0,0,PI));
        scene.fit(1);
        scene.setRadius(400);
        parser = new BVHLoader(sketchPath() + path, scene, null);
        parser.generateConstraints();

        skeletons = new ArrayList<Skeleton>();

        skeletons.add(new Skeleton(parser, SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL, scene, 0,255,0, scene.radius() * 0.01f));
        //skeletons.add(new Skeleton(parser, SimpleTRIK.HeuristicMode.EXPRESSIVE_FINAL, scene, color(255,0,0), scene.radius() * 0.01f));
        parser.root().cull(true);
        //skeleton._root.cull(true);
    }

    public void draw() {
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);
        shininess(10);
        //Draw Constraints
        scene.drawAxes();
        scene.render();
        //skeleton.renderNames();

        if(readNext) {
            readNextPose();
        }

    }

    public void readNextPose(){
        parser.nextPose();

        for(Skeleton skeleton: skeletons){
            Constraint c = skeleton._root.constraint();
            skeleton._root.setConstraint(null);
            skeleton._root.setPosition(parser.root().position().get());
            skeleton._root.setOrientation(parser.root().orientation().get());
            skeleton._root.setConstraint(c);
            //update targets
            for(Node joint : scene.branch(skeleton._root)){
                if (joint.children() == null || joint.children().isEmpty()) {
                    Node node = skeleton._jointToNode.get(joint);
                    Node target = skeleton._targets.get(parser.joint().get(node.id()).name());
                    target.setPosition(node.position().get());
                    target.setOrientation(node.orientation().get());
                    //modify end effector rotation
                    joint.setRotation(node.rotation());
                }
            }
        }
    }

    public void keyPressed(){
        if(key == 'W' || key == 'w'){
            readNext = !readNext;
        }
        if(key == 'S' || key == 's'){
            readNextPose();
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
        PApplet.main(new String[]{"ik.trik.mocap.Viewer"});
    }

    class Skeleton{
        Scene _scene;
        Node _reference;
        Joint _root;
        TRIKTree _solver;
        BVHLoader _loader;
        HashMap<Joint, Node> _jointToNode;
        HashMap<String, Joint> _structure;
        HashMap<String, Node> _targets;
        float _radius;

        public Skeleton(BVHLoader loader, SimpleTRIK.HeuristicMode mode, Scene scene, int red, int green, int blue, float radius){
            _scene = scene;
            _radius = radius;
            _reference = new Node();
            _reference.enableTagging(false);
            _createSkeleton(loader, red, green, blue, radius);
            _createSolver(mode);
        }

        protected void _createSkeleton(BVHLoader loader, int red, int green, int blue, float radius){
            HashMap<Node, Joint> pairs = new HashMap<>();
            _structure = new HashMap<>();
            _jointToNode = new HashMap<>();
            for(Node node : loader.branch()){
                Joint joint = new Joint(red, green, blue, radius);
                Joint reference = pairs.get(node.reference());
                if(reference != null){
                    joint.setReference(reference);
                }
                joint.setTranslation(node.translation().get());
                joint.setRotation(node.rotation().get());
                joint.setConstraint(node.constraint());
                pairs.put(node, joint);
                _structure.put(loader.joint().get(node.id()).name(),joint);

                if(node.reference() != null && node.reference().children().size() == 1){
                    //duplicateBone((Joint) joint.reference(), joint);
                }

                _jointToNode.put(joint, node);
            }
            _root = pairs.get(loader.root());
            _root.setReference(_reference);
            _root.setRoot(true);
        }


        protected void duplicateBone(Joint j_i, Joint j_i1){
            Joint j_mid = new Joint(j_i.red(), j_i.green(), j_i.blue(), j_i.radius());
            Vector v = j_i1.translation().get();
            j_mid.setReference(j_i);
            j_mid.setTranslation(Vector.multiply(v, 0.5f));
            BallAndSocket bs = new BallAndSocket(radians(20), radians(20));
            bs.setRestRotation(j_mid.rotation().get(), v.orthogonalVector(), v);
            j_mid.setConstraint(bs);
            //Set j_i1
            Constraint c_i1 = j_i1.constraint();
            Vector pos = j_i1.position();
            Quaternion or = j_i1.orientation();
            j_i1.setConstraint(null);
            j_i1.setReference(j_mid);
            j_i1.setPosition(pos);
            j_i1.setOrientation(or);
            j_i1.setConstraint(c_i1);

        }

        protected void _createSolver(SimpleTRIK.HeuristicMode mode){
            _solver = new TRIKTree(_root, mode);
            _solver.setMaxError(0.1f);
            _solver.setDirection(false);
            _solver.setSearchingAreaRadius(0.3f, true);
            _solver.setOrientationWeight(0.5f);

            _solver.setTimesPerFrame(10);
            _solver.setChainTimesPerFrame(20);
            _solver.setChainMaxIterations(20);
            _solver.setMaxIterations(10);

            //add task to scene
            TimingTask task = new TimingTask() {
                @Override
                public void execute() {
                    _solver.solve();
                }
            };
            task.run(40);

            _targets = new HashMap<>();

            //Add a target per leaf in the structure
            for(Map.Entry<String, Joint> entry : _structure.entrySet()){
                Node node = entry.getValue();
                if(node.children() == null || node.children().isEmpty()){
                    node.enableTagging(false);
                    Node target = Util.createTarget(scene, _radius);
                    target.setReference(_reference);
                    target.setPosition( node.position().get());
                    target.setOrientation( node.orientation().get());
                    _solver.addTarget(node, target);
                    _targets.put(entry.getKey(), target);
                }
            }
        }


        protected void renderNames(){
            _scene.beginHUD();
            PGraphics pg = _scene.context();
            pg.pushStyle();
            pg.noLights();
            pg.fill(46, 76, 125);
            for(Map.Entry<String, Joint> entry : _structure.entrySet()){
                Vector scrLocation = _scene.screenLocation(entry.getValue());
                pg.text(entry.getKey() + "\n" + String.format("x %.2f", entry.getValue().position().x())
                        + "\n" + String.format("y %.2f", entry.getValue().position().y())
                        + "\n" + String.format("z %.2f", entry.getValue().position().z()), scrLocation.x(), scrLocation.y());
            }

            for(Map.Entry<String, Node> entry : _targets.entrySet()){
                Vector scrLocation = _scene.screenLocation(entry.getValue());
                pg.text(entry.getKey() + "\n" + String.format("x %.2f", entry.getValue().position().x())
                        + "\n" + String.format("y %.2f", entry.getValue().position().y())
                        + "\n" + String.format("z %.2f", entry.getValue().position().z()), scrLocation.x(), scrLocation.y());
            }

            pg.popStyle();
            _scene.endHUD();
        }

    }


}
