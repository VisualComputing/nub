package ik.interactiveSkeleton;

import common.InteractiveNode;
import common.InteractiveShape;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.Target;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 24/02/18.
 */

public class InteractiveSkeleton  extends PApplet {
    Scene scene;
    Node eye;
    InteractiveShape shape;
    Joint root;
    LinearBlendSkinning skinning;
    Target target;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        //hint(DISABLE_OPTIMIZED_STROKE);
        //hint(DISABLE_DEPTH_TEST);

        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);
        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultNode(eye);
        scene.fitBallInterpolation();

        target = new Target(scene);
        //Create an initial Joint at the center of the Shape
        //root = new InteractiveJoint(scene, true);
        //Create and load an InteractiveShape
        PShape model = loadShape("/home/sebchaparr/Processing/JS/framesjs/testing/data/objs/TropicalFish01.obj");
        model.setTexture(loadImage("/home/sebchaparr/Processing/JS/framesjs/testing/data/objs/TropicalFish01.jpg"));
        Vector[] box = getBoundingBox(model);
        //Scale model
        float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
        model.scale(200.f*1.f/max);
        //Invert Y Axis and set Fill
        //model.rotateZ(PI);
        //model.setFill(color(0,255,0, 100));
        shape = new InteractiveShape(scene, model);
        shape.setPrecision(Node.Precision.FIXED);
        shape.setPrecisionThreshold(1);
        shape.rotate(new Quaternion(new Vector(0,0,1), PI));
        root = fishSkeleton(shape);
        //Apply skinning
        skinning = new LinearBlendSkinning(shape, model);
        ArrayList<Node> skeleton = scene.branch(root);
        skinning.setup(skeleton);
        //Adding IK behavior
        target.setPosition(skeleton.get(skeleton.size()-1).position());
        Solver solver = scene.setIKStructure(root);
        scene.addIKTarget(skeleton.get(skeleton.size()-1), target);
        solver.setTIMESPERFRAME(1);

    }

    public void draw(){
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();

        for(Node frame : scene.nodes()){
            if(frame instanceof Shape) ((Shape) frame).draw();
        }
        skinning.applyTransformations();
    }

    public static  Vector[] getBoundingBox(PShape shape) {
        Vector v[] = new Vector[2];
        float minx = 999;  float miny = 999;
        float maxx = -999; float maxy = -999;
        float minz = 999;  float maxz = -999;
        for(int j = 0; j < shape.getChildCount(); j++){
            PShape aux = shape.getChild(j);
            for(int i = 0; i < aux.getVertexCount(); i++){
                float x = aux.getVertex(i).x;
                float y = aux.getVertex(i).y;
                float z = aux.getVertex(i).z;
                minx = minx > x ? x : minx;
                miny = miny > y ? y : miny;
                minz = minz > z ? z : minz;
                maxx = maxx < x ? x : maxx;
                maxy = maxy < y ? y : maxy;
                maxz = maxz < z ? z : maxz;
            }
        }

        v[0] = new Vector(minx,miny, minz);
        v[1] = new Vector(maxx,maxy, maxz);
        return v;
    }

    public Joint fishSkeleton(Node reference){
        Joint j1 = new Joint(scene, true);
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Joint j2 = new Joint(scene, false);
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Joint j3 = new Joint(scene, false);
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Joint j4 = new Joint(scene, false);
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Joint j5 = new Joint(scene, false);
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Joint j6 = new Joint(scene, false);
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        return j1;
    }

    public void printSkeleton(Node root){
        int i = 0;
        for(Node node : scene.branch(root)){
            System.out.println("Node " + i + " : " + node.position());
            i++;
        }
    }

    public void keyPressed(){
        if(key == ' '){
            printSkeleton(root);
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveSkeleton"});
    }
}
