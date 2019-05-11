package ik.obj;

import ik.interactive.FitCurve;
import ik.interactive.InteractiveJoint;
import ik.interactive.Target;
import nub.core.Node;
import nub.core.Graph;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.common.Joint;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 10/05/19.
 */

//Build easily a Skeleton to relate to a Mesh
public class SimpleBuilder extends PApplet{
    Scene scene;

    //Shape variables
    PShape model;

    //Set this path to load your objs
    String shapePath = "/testing/data/objs/Rigged_Hand.obj";
    String texturePath = "/testing/data/objs/HAND_C.jpg";

    float radius = 0;
    int w = 1000, h = 700;
    /*Create different skeletons to interact with*/
    String renderer = P3D;

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.obj.SimpleBuilder"});
    }

    public void settings() {
        size(w, h, renderer);
    }

    public void setup(){
        Joint.markers = true;
        //1. Create a scene
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        //2. Import model
        model = createShapeTri(loadShape(sketchPath() + shapePath), sketchPath() + texturePath, 100);
        //3. Scale scene
        float size = max(model.getHeight(), model.getWidth());
        scene.setRightHanded();
        scene.setRadius(size);
        scene.fit();
        //4. Create a Interactive Joint at the center of the scene
        radius = scene.radius() * 0.01f;
        InteractiveJoint initial = new InteractiveJoint(scene, radius);
        initial.setRoot(true);
        initial.setPickingThreshold(-0.01f);
    }

    public void draw() {
        background(0);
        ambientLight(102, 102, 102);
        lightSpecular(204, 204, 204);
        directionalLight(102, 102, 102, 0, 0, -1);
        specular(255, 255, 255);
        shininess(10);
        stroke(255);
        stroke(255,0,0);
        scene.drawAxes();
        shape(model);
        scene.render();
    }

    //mouse events
    public void mouseMoved() {
        scene.cast();
    }

    public void mouseDragged(MouseEvent event) {
        if (mouseButton == RIGHT && event.isControlDown()) {
            Vector vector = new Vector(scene.mouse().x(), scene.mouse().y());
            if(scene.trackedNode() != null)
                if(scene.trackedNode() instanceof  InteractiveJoint)
                    scene.trackedNode().interact("OnAdding", scene, vector);
                else
                    scene.trackedNode().interact("OnAdding", vector);
        } else if (mouseButton == LEFT) {
            scene.spin();
        } else if (mouseButton == RIGHT) {
            scene.translate();
        } else if (mouseButton == CENTER){
            scene.scale(scene.mouseDX());
        }
    }

    public void mouseReleased(MouseEvent event){
        Vector vector = new Vector(scene.mouse().x(), scene.mouse().y());
        if(scene.trackedNode() != null)
            if(scene.trackedNode() instanceof  InteractiveJoint)
                scene.trackedNode().interact("Add", scene, scene, vector);
    }

    public void mouseWheel(MouseEvent event) {
        scene.scale(event.getCount() * 20);
    }

    public void mouseClicked(MouseEvent event) {
        if (event.getCount() == 2) {
            if (event.getButton() == LEFT) {
                if (event.isShiftDown())
                    if(scene.trackedNode() != null)
                        scene.trackedNode().interact("Remove");
                    else
                        scene.focus();
            }
            else {
                scene.align();
            }
        }
    }

    public void keyPressed(){
        if(key == 'J' || key == 'j'){
            InteractiveJoint initial = new InteractiveJoint(scene, radius);
            initial.setRoot(true);
            initial.setPickingThreshold(-0.01f);
        }else if(key == 'P' || key == 'p'){
            printJoints(scene.trackedNode(), "reference", 1);
        }else if(key == 'A' || key == 'a'){
            Joint.axes = !Joint.axes;
        }else if(key == 'E' || key == 'e'){
            if(scene.trackedNode() != null){
                scene.trackedNode().setTranslation(new Vector());
                scene.trackedNode().enableTracking(false);
            }
        }
    }


    public void printTree(Node root, String sep){
        if(root == null) return;
        System.out.print(sep + "|-> Node ");
        System.out.println("translation: " + root.translation() + "rotation axis: " + root.rotation().axis() + "rotation angle : " + root.rotation().angle());
        for(Node child : root.children()){
            printTree(child, sep + "\t");
        }
    }

    public int printJoints(Node root, String reference, int i){
        if(root == null) return 0;
        System.out.println("Joint j" + i + " = new Joint(scene, scene.radius() * 0.01f);");
        System.out.println("j" + i + ".setPickingThreshold(-0.01f);");
        System.out.println("j" + i + ".setReference(" + reference + ");");
        System.out.println("j" + i + ".setTranslation(" + root.translation().x() + "f ," +
                root.translation().y() + "f ," + root.translation().z() + "f);");
        System.out.println("j" + i + ".setRotation( new Quaternion( new Vector (" +
                root.rotation().axis().x() + "f ," + root.rotation().axis().y() + "f ,"
                + root.rotation().axis().z() + "f), " + root.rotation().angle() + "f));");
        int idx = i;
        for(Node child : root.children()){
            idx = idx + printJoints(child, "j"+ i, idx + 1);
        }
        return idx;
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
}

