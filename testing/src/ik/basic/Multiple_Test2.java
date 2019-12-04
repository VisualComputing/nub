package ik.basic;

import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.TRIKTree;
import nub.ik.visual.Joint;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class Multiple_Test2 extends PApplet {
    Scene scene;
    List<Target> targets = new ArrayList<>();
    TRIKTree s;

    public void settings(){
        size(500,500,P3D);
    }

    public void setup(){
        Joint.axes = true;
        //Setting the scene
        scene = new Scene(this);
        if(scene.is3D()) scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRadius(100);
        scene.fit(1);

        Node root = generateStructure(scene);

        //Add IK
        s = new TRIKTree(root);
        //Add end effectors
        generateEFF(scene, s, root);


    }

    public void draw(){
        background(0);
        scene.render();
        s.solve();
    }

    public void generateEFF(Scene scene, TRIKTree s, Node root){
        if(root == null);
        if(root.children() == null || root.children().isEmpty()){
            root.enableTagging(false);
            Target target = new Target(scene, 3);
            target.setPosition(root.position().get());
            s.addTarget(root, target);
            targets.add(target);
        }

        for(Node child : root.children()){
            generateEFF(scene, s, child);
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

    public Node generateStructure(Scene scene){
        Joint root = new Joint(scene);
        Node n1 = new Joint(scene);
        n1.setReference(root);
        n1.setTranslation(-20, 0);
        BallAndSocket b1 = new BallAndSocket(radians(30), radians(30), radians(50), radians(60));
        b1.setTwistLimits(radians(3), radians(3));
        b1.setRestRotation(n1.rotation().get(), new Vector(1, 0, 0), new Vector(0,1,0));
        n1.setConstraint(b1);


        Node n2 = new Joint(scene);
        n2.setReference(n1);
        n2.setTranslation(0, 20);
        Hinge h3 = new Hinge(radians(5), radians(90), n2.rotation().get(), new Vector(0,1,0), new Vector(-1,0,0));
        n2.setConstraint(h3);

        Node n3 = new Joint(scene);
        n3.setReference(n2);
        n3.setTranslation(0, 20);
        Node n4 = new Joint(scene);
        n4.setReference(root);
        n4.setTranslation(20, 0);
        Node n5 = new Joint(scene);
        n5.setReference(n4);
        n5.setTranslation(0, 20);
        Node n6 = new Joint(scene);
        n6.setReference(n5);
        n6.setTranslation(0, 20);
        root.setRoot(true);
        return root;
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
            scene.translate(scene.mouseX() - scene.pmouseX(), scene.mouseY() - scene.pmouseY());
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
        s.solve();
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.Multiple_Test2"});
    }

}
