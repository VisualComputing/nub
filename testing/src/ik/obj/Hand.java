package ik.obj;

import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import ik.interactive.Target;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.Solver;
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
    LinearBlendSkinning skinning;

    String shapePath = "/testing/data/objs/Rigged_Hand.obj";
    String texturePath = "/testing/data/objs/HAND_C.jpg";

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        Joint.markers = false;
        Joint.depth = false;

        //Setting the scene and loading mesh
        //1. Create the scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        //2. Import the model
        //2.1 create a root node which is parent of shape node
        Node root = new Node(scene);
        root.enableTracking(false);
        //2.2 Load the mesh
        PShape model = createShapeTri(loadShape(sketchPath() + shapePath), sketchPath() + texturePath, 100);
        Node shape = new Node(scene, model);
        shape.enableTracking(false); //use exact picking precision
        //2.3 set root as reference
        shape.setReference(root);
        //3. Scale scene
        float size = max(model.getHeight(), model.getWidth());
        scene.setRightHanded();
        scene.setRadius(size);
        scene.fit();
        //4. Adding the Skeleton
        //4.1 Use SimpleBuilder example (or a Modelling Sw if desired) and position each Joint accordingly to mesh
        //4.2 Create the Joints based on 1.
        ArrayList<Node> skeleton = (ArrayList<Node>) buildSkeleton(root);
        skinning = new LinearBlendSkinning(shape, model);
        skinning.setup(skeleton);

        //5. Adding IK behavior
        //5.1 register an IK Solver
        Solver solver = scene.registerTreeSolver(skeleton.get(0));
        //Update params
        solver.setMaxError(1f);

        //5.2 Identify end effectors (leaf nodes)
        List<Node> endEffectors = new ArrayList<>();
        List<Node> targets = new ArrayList<>();
        for(Node node : skeleton){
            if(node.children().size() == 0) {
                endEffectors.add(node);
                //5.3 Create targets
                Target target = new Target(scene, scene.radius() * 0.01f);
                target.setReference(root);
                target.setPosition(node.position().get());
                //add IK target to solver
                scene.addIKTarget(node, target);
            }
        }
    }

    public void draw(){
        background(0);
        lights();
        scene.drawAxes();
        scene.render();
        skinning.applyTransformations();
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
        Node f = scene.trackedNode();
        if(f == null) return;
        Hinge c = f.constraint() instanceof Hinge ? (Hinge) f.constraint() : null;
        if(c == null) return;
        scene.trackedNode().rotate(new Quaternion( c.orientation().rotate(new Vector(0,0,1)), radians(5)));
    }

    //Skeleton is founded by interacting with SimpleBuilder
    public List<Node> buildSkeleton(Node reference){
        Joint j1 = new Joint(scene, scene.radius() * 0.01f);
        j1.setPickingThreshold(-0.01f);
        j1.setReference(reference);
        j1.setTranslation(-44.857143f ,-1.5283124E-6f ,4.8571386f);
        j1.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j2 = new Joint(scene, scene.radius() * 0.01f);
        j2.setPickingThreshold(-0.01f);
        j2.setReference(j1);
        j2.setTranslation(23.144106f ,1.1564114E-6f ,0.5799241f);
        j2.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3 = new Joint(scene, scene.radius() * 0.01f);
        j3.setPickingThreshold(-0.01f);
        j3.setReference(j2);
        j3.setTranslation(21.865131f ,5.1811577E-7f ,1.1588416f);
        j3.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j4 = new Joint(scene, scene.radius() * 0.01f);
        j4.setPickingThreshold(-0.01f);
        j4.setReference(j3);
        j4.setTranslation(0.0f ,0.0f ,0.0f);
        j4.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j5 = new Joint(scene, scene.radius() * 0.01f);
        j5.setPickingThreshold(-0.01f);
        j5.setReference(j4);
        j5.setTranslation(12.66872f ,2.2363186f ,10.542329f);
        j5.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j6 = new Joint(scene, scene.radius() * 0.01f);
        j6.setPickingThreshold(-0.01f);
        j6.setReference(j5);
        j6.setTranslation(9.157889f ,-5.766021E-7f ,3.9953837f);
        j6.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j7 = new Joint(scene, scene.radius() * 0.01f);
        j7.setPickingThreshold(-0.01f);
        j7.setReference(j6);
        j7.setTranslation(9.273516f ,-1.2207849f ,3.5199592f);
        j7.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j26 = new Joint(scene, scene.radius() * 0.01f);
        j26.setPickingThreshold(-0.01f);
        j26.setReference(j3);
        j26.setTranslation(0.0f ,0.0f ,0.0f);
        j26.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j27 = new Joint(scene, scene.radius() * 0.01f);
        j27.setPickingThreshold(-0.01f);
        j27.setReference(j26);
        j27.setTranslation(15.724657f ,4.0416408f ,3.2429714f);
        j27.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j28 = new Joint(scene, scene.radius() * 0.01f);
        j28.setPickingThreshold(-0.01f);
        j28.setReference(j27);
        j28.setTranslation(12.553091f ,-1.595385f ,3.3422072f);
        j28.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j29 = new Joint(scene, scene.radius() * 0.01f);
        j29.setPickingThreshold(-0.01f);
        j29.setReference(j28);
        j29.setTranslation(13.573793f ,-2.3576936E-7f ,0.9572305f);
        j29.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j136 = new Joint(scene, scene.radius() * 0.01f);
        j136.setPickingThreshold(-0.01f);
        j136.setReference(j3);
        j136.setTranslation(0.0f ,0.0f ,0.0f);
        j136.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j137 = new Joint(scene, scene.radius() * 0.01f);
        j137.setPickingThreshold(-0.01f);
        j137.setReference(j136);
        j137.setTranslation(19.469503f ,2.209004f ,-3.427169f);
        j137.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j138 = new Joint(scene, scene.radius() * 0.01f);
        j138.setPickingThreshold(-0.01f);
        j138.setReference(j137);
        j138.setTranslation(10.016321f ,0.16893235f ,0.19314425f);
        j138.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j139 = new Joint(scene, scene.radius() * 0.01f);
        j139.setPickingThreshold(-0.01f);
        j139.setReference(j138);
        j139.setTranslation(14.918277f ,1.391636E-8f ,-6.1522887E-6f);
        j139.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j686 = new Joint(scene, scene.radius() * 0.01f);
        j686.setPickingThreshold(-0.01f);
        j686.setReference(j3);
        j686.setTranslation(0.0f ,0.0f ,0.0f);
        j686.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j687 = new Joint(scene, scene.radius() * 0.01f);
        j687.setPickingThreshold(-0.01f);
        j687.setReference(j686);
        j687.setTranslation(20.563015f ,2.8993192f ,-13.581605f);
        j687.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j688 = new Joint(scene, scene.radius() * 0.01f);
        j688.setPickingThreshold(-0.01f);
        j688.setReference(j687);
        j688.setTranslation(9.327533f ,1.41979E-7f ,-0.7453499f);
        j688.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j689 = new Joint(scene, scene.radius() * 0.01f);
        j689.setPickingThreshold(-0.01f);
        j689.setReference(j688);
        j689.setTranslation(11.8942795f ,1.5528883E-7f ,-0.8063927f);
        j689.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3436 = new Joint(scene, scene.radius() * 0.01f);
        j3436.setPickingThreshold(-0.01f);
        j3436.setReference(j3);
        j3436.setTranslation(0.0f ,0.0f ,0.0f);
        j3436.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3437 = new Joint(scene, scene.radius() * 0.01f);
        j3437.setPickingThreshold(-0.01f);
        j3437.setReference(j3436);
        j3437.setTranslation(-0.40318406f ,2.9916523E-6f ,-16.732647f);
        j3437.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3438 = new Joint(scene, scene.radius() * 0.01f);
        j3438.setPickingThreshold(-0.01f);
        j3438.setReference(j3437);
        j3438.setTranslation(4.489152f ,1.3270688E-6f ,-7.3980904f);
        j3438.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3439 = new Joint(scene, scene.radius() * 0.01f);
        j3439.setPickingThreshold(-0.01f);
        j3439.setReference(j3438);
        j3439.setTranslation(4.031951f ,8.32879E-7f ,-4.6367645f);
        j3439.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        Joint j3440 = new Joint(scene, scene.radius() * 0.01f);
        j3440.setPickingThreshold(-0.01f);
        j3440.setReference(j3439);
        j3440.setTranslation(7.055936f ,4.7521172E-7f ,-2.6207702f);
        j3440.setRotation( new Quaternion( new Vector (0.0f ,0.0f ,0.0f), 0.0f));
        j1.setRoot(true);
        return scene.branch(j1);
    }




    //Adapted from http://www.cutsquash.com/2015/04/better-obj-model-loading-in-processing/
    public PShape createShapeTri(PShape r, String texture, float size) {
        float scaleFactor = size / max(r.getWidth(), r.getHeight());
        PImage tex = loadImage(texture);
        PShape s = createShape();
        s.beginShape(TRIANGLES);
        s.noStroke();
        s.texture(tex);
        s.textureMode(NORMAL);
        for (int i=100; i<r.getChildCount (); i++) {
            if (r.getChild(i).getVertexCount() ==3) {
                for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);
                    s.vertex(p.x, p.y, p.z, u, v);
                }
            }
        }
        s.endShape();
        return s;
    }

    public PShape createShapeQuad(PShape r, String texture, float size) {
        float scaleFactor = size / max(r.getWidth(), r.getHeight());
        PImage tex = loadImage(texture);
        PShape s = createShape();
        s.beginShape(QUADS);
        s.noStroke();
        s.texture(tex);
        s.textureMode(NORMAL);
        for (int i=100; i<r.getChildCount (); i++) {
            if (r.getChild(i).getVertexCount() ==4) {
                for (int j=0; j<r.getChild (i).getVertexCount(); j++) {
                    PVector p = r.getChild(i).getVertex(j).mult(scaleFactor);
                    PVector n = r.getChild(i).getNormal(j);
                    float u = r.getChild(i).getTextureU(j);
                    float v = r.getChild(i).getTextureV(j);
                    s.normal(n.x, n.y, n.z);
                    s.vertex(p.x, p.y, p.z, u, v);
                }
            }
        }
        s.endShape();
        return s;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.obj.Hand"});
    }

}
