package ik.interactiveSkeleton;

import frames.core.Frame;
import frames.core.Graph;
import frames.core.Interpolator;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 11/03/18.
 */

// TODO: Check for flow field with noise and brownian motion
public class InteractiveFish extends PApplet {

    boolean showSkeleton = false;
    Scene scene;
    Shape shape;
    Frame root;
    PShape model;

    //Uncomment to use Linear Blending Skinning with CPU
    LinearBlendSkinning skinning;
    //LinearBlendSkinningGPU skinning;
    Frame target;
    Interpolator targetInterpolator;
    String shapePath = "/testing/data/objs/fish.obj";
    String texturePath = "/testing/data/objs/fish.jpg";

    float targetRadius = 7;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        scene.setFOV(PI / 3);
        scene.fit(1);
        target = createTarget(targetRadius);

        model = loadShape(sketchPath() + shapePath);
        model.setTexture(loadImage(sketchPath() + texturePath));

        Vector[] box = getBoundingBox(model);
        //Scale model
        float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
        //model.scale(200.f*1.f/max);
        //Invert Y Axis and set Fill
        shape = new Shape(scene, model);

        shape.rotate(new Quaternion(new Vector(0, 0, 1), PI));
        shape.scale(200.f * 1.f / max);
        root = fishSkeleton(shape);

        ArrayList<Frame> skeleton = (ArrayList<Frame>) scene.branch(root);
        shape.scale(0.25f);
        //Uncomment to use Linear Blending Skinning with CPU
        skinning = new LinearBlendSkinning(shape, model);
        skinning.setup(skeleton);
        //skinning = new LinearBlendSkinningGPU(model, skeleton);
        //skinning.initParams(this, scene);
        //Adding IK behavior
        target.setReference(shape);
        target.setPosition(skeleton.get(skeleton.size() - 1).position());
        //Making a default Path that target must follow
        targetInterpolator = setupTargetInterpolator(target);
        Solver solver = scene.registerTreeSolver(root);
        solver.error = 0.01f;
        scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);

    }

    public Shape createTarget(float radius){
        PShape redBall = createShape(SPHERE, radius);
        redBall.setStroke(false);
        redBall.setFill(color(255,0,0));
        return new Shape(scene, redBall);
    }


    public void draw() {
        //skinning.updateParams();
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();
        //comment this line if you're using Linear Blending Skinning with CPU
        //shader(skinning.shader);
        if(showSkeleton) scene.traverse();
        else{
            pushMatrix();
            scene.applyWorldTransformation(shape);
            shape(model);
            popMatrix();
        }
        //resetShader();
        //Uncomment to use Linear Blending Skinning with CPU
        skinning.applyTransformations();
    }

    public static Vector[] getBoundingBox(PShape shape) {
        Vector v[] = new Vector[2];
        float minx = 999;
        float miny = 999;
        float maxx = -999;
        float maxy = -999;
        float minz = 999;
        float maxz = -999;
        for (int j = 0; j < shape.getChildCount(); j++) {
            PShape aux = shape.getChild(j);
            for (int i = 0; i < aux.getVertexCount(); i++) {
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

        v[0] = new Vector(minx, miny, minz);
        v[1] = new Vector(maxx, maxy, maxz);
        return v;
    }

    public Joint fishSkeleton(Frame reference) {
        Joint j1 = new Joint(scene);
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Joint j2 = new Joint(scene);
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Joint j3 = new Joint(scene);
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Joint j4 = new Joint(scene);
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Joint j5 = new Joint(scene);
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Joint j6 = new Joint(scene);
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        j1.setRoot(true);
        return j1;
    }

    public Interpolator setupTargetInterpolator(Frame target) {
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.setLoop();
        targetInterpolator.setSpeed(5.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2.0f * PI / (nbKeyFrames - 1);
        for (int i = 0; i < nbKeyFrames; i++) {
            Frame iFrame = new Frame(scene);
            iFrame.setReference(shape);
            iFrame.setTranslation(new Vector(100 * sin(step * i), target.translation().y(), target.translation().z()));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.start();
        return targetInterpolator;
    }

    public void printSkeleton(Frame root) {
        int i = 0;
        for (Frame node : scene.branch(root)) {
            System.out.println("Node " + i + " : " + node.position());
            i++;
        }
    }

    public void keyPressed() {
        if (key == ' ') {
            printSkeleton(root);
        }
        if(key == 'S' || key == 's'){
            showSkeleton = !showSkeleton;
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
        PApplet.main(new String[]{"ik.interactiveSkeleton.InteractiveFish"});
    }
}