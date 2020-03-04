package ik.basic;

import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.ik.solver.geometric.oldtrik.TRIKTree;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import nub.processing.TimingTask;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class TRIKTREETest extends PApplet{
    Scene scene;
    int branches = 2, num = 4, depth = 2;
    List<Target> targets = new ArrayList<>();
    boolean enableSolver = true;
    TRIKTree trik;

    public void settings(){
        size(700,700,P3D);
    }

    public void setup(){
        //TRIK._debug = true;
        Joint.axes = true;
        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(2000);
        scene.fit(1);

        Joint root = new Joint(scene);
        root.setRoot(true);
        generateTree(scene, root, branches, num, depth, 0, 0.5f * scene.radius()/((num + 1) * depth));

        //Add IK
        trik = new TRIKTree(root);
        trik.setTimesPerFrame(1);
        //Add end effectors
        generateEFF(trik, root);

        TimingTask solverTask = new TimingTask(scene) {
            @Override
            public void execute() {
                //a solver perform an iteration when solve method is called
                if(enableSolver){
                    trik.solve();
                }
            }
        };
        solverTask.run(40); //Execute the solverTask each 40 ms

    }

    public void draw(){
        background(0);
        scene.render();
    }

    public void generateEFF(TRIKTree solver, Node root){
        if(root == null);
        if(root.children() == null || root.children().isEmpty()){
            root.enableTagging(false);
            Target target = new Target(scene, 6);
            target.setPosition(root.position().get());
            solver.addTarget(root, target);
            targets.add(target);
        }

        for(Node child : root.children()){
            generateEFF(solver, child);
        }
    }


    public Node createTarget(Scene scene, float radius){
        /*
         * A target is a Node, we represent a Target as a
         * Red ball.
         * */
        PShape redBall;
        if (scene.is2D()) redBall = createShape(ELLIPSE,0, 0, radius*2, radius*2);
        else  redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));

        Node target = new Node(scene, redBall);
        //Exact picking precision
        target.setPickingThreshold(0);
        return target;
    }


    public Node generateTree(Scene scene, Node root, int branches, int num, int depth, int cur, float length){
        if(cur - depth == 0) return root;
        float step = radians(70)/ (branches / 2);
        float angle = 0;
        float l = (1f - (cur + 1f)/(depth + 1f)) * length;

        for(int i = 0; i < branches; i++){
            //Add joint
            Node child = new Joint(scene);
            child.setReference(root);
            child.rotate(new Vector(0,0,1), angle);
            child.translate(root.displacement(new Vector(0,l), child));
            for(int j = 0; j < num; j++){
                Node next = new Joint(scene);
                next.setReference(child);
                next.translate(new Vector(0,l));
                child = next;
            }
            angle = -angle;
            if(i % 2 == 0){
                angle -= step;
            }

            //recursive step:
            generateTree(scene, child, branches, num, depth, cur + 1, length);
        }
        return root;
    }

    @Override
    public void mouseMoved() {
        scene.mouseTag();
    }

    public void mouseDragged(MouseEvent event) {
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector vector = new Vector(scene.mouseX(), scene.mouseY());
            if(scene.node() != null)
                scene.node().interact("OnAdding", vector);
        } else if (mouseButton == LEFT) {
            scene.spin(scene.pmouseX(), scene.pmouseY(), scene.mouseX(), scene.mouseY());
        } else if (mouseButton == RIGHT) {
            scene.translate(scene.mouseX() - scene.pmouseX(), scene.mouseY() - scene.pmouseY(), 0);
            Target.multipleTranslate();
        } else if (mouseButton == CENTER){
            scene.scale(scene.mouseDX());
        } else if(scene.node() != null)
            scene.node().interact("Reset");
        if(!Target.selectedTargets().contains(scene.node())){
            Target.clearSelectedTargets();
        }

    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 1) {
            if (event.getButton() == LEFT) {
                if(event.isControlDown()){
                    if(scene.node() != null)
                        scene.node().interact("KeepSelected");
                }
            }

        } else if (event.getCount() == 2)
            if (event.getButton() == LEFT)
                scene.focus();
            else
                scene.align();
    }

    public void keyPressed(){
        if(key == 'A' || key == 'a'){
            enableSolver = false;
            trik.solve();
        }

        if(key == 'S' || key == 's'){
            enableSolver = !enableSolver;
        }

    }



    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.TRIKTREETest"});
    }

}
