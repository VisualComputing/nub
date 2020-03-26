package ik.obj;

import nub.ik.visual.Joint;
import nub.ik.skinning.GPULinearBlendSkinning;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.*;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 10/05/19.
 */
public class Hand extends PApplet {
    Scene scene;
    GPULinearBlendSkinning skinning;

    String shapePath = "/testing/data/objs/Rigged_Hand.obj";
    String texturePath = "/testing/data/objs/HAND_C.jpg";
    Node reference;


    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        //Joint.markers = true;
        Joint.depth = false;
        //1. Create and set the scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setRightHanded();
        scene.fit(1);
        //2. Define the Skeleton
        //2.1 Define a reference node to the skeleton and the mesh
        reference = new Node();
        reference.enableTagging(false); //disable interaction
        //2.2 Use SimpleBuilder example (or a Modelling Sw if desired) and locate each Joint accordingly to mesh
        //2.3 Create the Joints based on 2.2.
        List<Node> skeleton = buildSkeleton(reference);
        //3. Relate the shape with a skinning method (CPU or GPU)
        skinning = new GPULinearBlendSkinning(skeleton, this.g, sketchPath() + shapePath, sketchPath() + texturePath, scene.radius());

        //4. Adding IK behavior
        //4.1 Identify root and end effector(s)
        Node root = skeleton.get(0); //root is the fist joint of the structure
        List<Node> endEffectors = new ArrayList<>(); //Ende Effectors are leaf nodes (with no children)
        for(Node node : skeleton) {
            if (node.children().size() == 0) {
                endEffectors.add(node);
            }
        }

        //4.2 relate a skeleton with an IK Solver
        scene.enableTRIK(true);
        Solver solver = scene.registerTreeSolver(root);


        //Update params
        solver.setMaxError(1f);

        for(Node endEffector : endEffectors){
            //4.3 Create target(s) to relate with End Effector(s)
            Target target = new Target(scene, scene.radius() * 0.01f);
            target.setReference(reference); //Target also depends on reference
            target.setPosition(endEffector.position().get());
            //4.4 Relate target(s) with end effector(s)
            scene.addIKTarget(endEffector, target);
        }

    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        //Render mesh with respect to the node
        skinning.render(scene, reference);
        scene.render();
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
        Node f = scene.node();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.node().rotate(new Quaternion( c.orientation().rotate(new Vector(0,0,1)), radians(5)));
    }

    //Skeleton is founded by interacting with SimpleBuilder
    public List<Node> buildSkeleton(Node reference){
        Joint j1 = new Joint(scene.radius() * 0.01f);
        j1.setPickingThreshold(-0.01f);
        j1.setReference(reference);
        j1.setTranslation(-44.857143f ,-1.5283124E-6f ,4.8571386f);
        j1.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j2 = new Joint(scene.radius() * 0.01f);
        j2.setPickingThreshold(-0.01f);
        j2.setReference(j1);
        j2.setTranslation(23.144106f ,1.1564114E-6f ,0.5799241f);
        j2.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3 = new Joint(scene.radius() * 0.01f);
        j3.setPickingThreshold(-0.01f);
        j3.setReference(j2);
        j3.setTranslation(21.865131f ,5.1811577E-7f ,1.1588416f);
        j3.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j4 = new Joint(scene.radius() * 0.01f);
        j4.setPickingThreshold(-0.01f);
        j4.setReference(j3);
        j4.setTranslation(0.0f ,0.0f ,0.0f);
        j4.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j5 = new Joint( scene.radius() * 0.01f);
        j5.setPickingThreshold(-0.01f);
        j5.setReference(j4);
        j5.setTranslation(12.66872f ,2.2363186f ,10.542329f);
        j5.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j6 = new Joint(scene.radius() * 0.01f);
        j6.setPickingThreshold(-0.01f);
        j6.setReference(j5);
        j6.setTranslation(9.157889f ,-5.766021E-7f ,3.9953837f);
        j6.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j7 = new Joint(scene.radius() * 0.01f);
        j7.setPickingThreshold(-0.01f);
        j7.setReference(j6);
        j7.setTranslation(9.273516f ,-1.2207849f ,3.5199592f);
        j7.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j26 = new Joint(scene.radius() * 0.01f);
        j26.setPickingThreshold(-0.01f);
        j26.setReference(j3);
        j26.setTranslation(0.0f ,0.0f ,0.0f);
        j26.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j27 = new Joint(scene.radius() * 0.01f);
        j27.setPickingThreshold(-0.01f);
        j27.setReference(j26);
        j27.setTranslation(15.724657f ,4.0416408f ,3.2429714f);
        j27.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j28 = new Joint(scene.radius() * 0.01f);
        j28.setPickingThreshold(-0.01f);
        j28.setReference(j27);
        j28.setTranslation(12.553091f ,-1.595385f ,3.3422072f);
        j28.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j29 = new Joint(scene.radius() * 0.01f);
        j29.setPickingThreshold(-0.01f);
        j29.setReference(j28);
        j29.setTranslation(13.573793f ,-2.3576936E-7f ,0.9572305f);
        j29.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j136 = new Joint(scene.radius() * 0.01f);
        j136.setPickingThreshold(-0.01f);
        j136.setReference(j3);
        j136.setTranslation(0.0f ,0.0f ,0.0f);
        j136.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j137 = new Joint(scene.radius() * 0.01f);
        j137.setPickingThreshold(-0.01f);
        j137.setReference(j136);
        j137.setTranslation(19.469503f ,2.209004f ,-3.427169f);
        j137.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j138 = new Joint(scene.radius() * 0.01f);
        j138.setPickingThreshold(-0.01f);
        j138.setReference(j137);
        j138.setTranslation(10.016321f ,0.16893235f ,0.19314425f);
        j138.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j139 = new Joint(scene.radius() * 0.01f);
        j139.setPickingThreshold(-0.01f);
        j139.setReference(j138);
        j139.setTranslation(14.918277f ,1.391636E-8f ,-6.1522887E-6f);
        j139.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j686 = new Joint(scene.radius() * 0.01f);
        j686.setPickingThreshold(-0.01f);
        j686.setReference(j3);
        j686.setTranslation(0.0f ,0.0f ,0.0f);
        j686.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j687 = new Joint(scene.radius() * 0.01f);
        j687.setPickingThreshold(-0.01f);
        j687.setReference(j686);
        j687.setTranslation(20.563015f ,2.8993192f ,-13.581605f);
        j687.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j688 = new Joint(scene.radius() * 0.01f);
        j688.setPickingThreshold(-0.01f);
        j688.setReference(j687);
        j688.setTranslation(9.327533f ,1.41979E-7f ,-0.7453499f);
        j688.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j689 = new Joint(scene.radius() * 0.01f);
        j689.setPickingThreshold(-0.01f);
        j689.setReference(j688);
        j689.setTranslation(11.8942795f ,1.5528883E-7f ,-0.8063927f);
        j689.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3436 = new Joint(scene.radius() * 0.01f);
        j3436.setPickingThreshold(-0.01f);
        j3436.setReference(j3);
        j3436.setTranslation(0.0f ,0.0f ,0.0f);
        j3436.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3437 = new Joint(scene.radius() * 0.01f);
        j3437.setPickingThreshold(-0.01f);
        j3437.setReference(j3436);
        j3437.setTranslation(-0.40318406f ,2.9916523E-6f ,-16.732647f);
        j3437.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3438 = new Joint(scene.radius() * 0.01f);
        j3438.setPickingThreshold(-0.01f);
        j3438.setReference(j3437);
        j3438.setTranslation(4.489152f ,1.3270688E-6f ,-7.3980904f);
        j3438.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3439 = new Joint( scene.radius() * 0.01f);
        j3439.setPickingThreshold(-0.01f);
        j3439.setReference(j3438);
        j3439.setTranslation(4.031951f ,8.32879E-7f ,-4.6367645f);
        j3439.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3440 = new Joint(scene.radius() * 0.01f);
        j3440.setPickingThreshold(-0.01f);
        j3440.setReference(j3439);
        j3440.setTranslation(7.055936f ,4.7521172E-7f ,-2.6207702f);
        j3440.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        j1.setRoot(true);
        return scene.branch(j1);
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.obj.Hand"});
    }

}
