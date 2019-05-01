package ik.flock;


import nub.core.Node;
import nub.core.Interpolator;
import nub.ik.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import ik.common.Joint;
import ik.common.LinearBlendSkinning;
import ik.interactive.Target;
import processing.core.PApplet;
import processing.core.PShape;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Flock extends PApplet {
    Scene scene;
    //flock bounding box
    static int flockWidth = 1280;
    static int flockHeight = 720;
    static int flockDepth = 600;
    static boolean avoidWalls = true;

    String shapePath = "/testing/data/objs/";
    PShape pshape;
    Node objShape;

    int initBoidNum = 40, numFlocks = 8; // amount of boids to start the program with
    static ArrayList<ArrayList<Boid>> flocks = new ArrayList<>();
    static Node avatar;
    static boolean animate = true;
    ArrayList<LinearBlendSkinning> skinning = new ArrayList<>();

    public void settings() {
        size(1000, 800, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setFrustum(new Vector(0, 0, 0), new Vector(flockWidth, flockHeight, flockDepth));
        scene.setAnchor(scene.center());
        scene.setFOV(PI / 3);
        scene.fit();
        // create and fill the list of boids
        for(int k = 0; k < numFlocks; k++){
            ArrayList<Boid> flock = new ArrayList();
            generateFish("fish" + k % 4 + ".obj");
            for (int i = 0; i < initBoidNum; i++)
                flock.add(new Boid(scene, objShape, pshape, new Vector(flockWidth / 2, flockHeight / 2, flockDepth / 2), flock));
            flocks.add(flock);
        }
        frameRate(30);
    }

    public void draw() {
        background(0);
        ambientLight(128, 128, 128);
        directionalLight(255, 255, 255, 0, 1, -100);
        walls();
        if(act) {
            scene.render();
            updateAvatar();
            for (LinearBlendSkinning s : skinning) s.applyTransformations();
        }
    }

    public void updateAvatar() {
        // boid is the one picked with a 'mouseClicked'
        Node boid = scene.trackedNode("mouseClicked");
        if (boid != avatar) {
            avatar = boid;
            if (avatar != null)
                thirdPerson();
            else if (scene.eye().reference() != null)
                resetEye();
        }
    }

    // 'first-person' interaction
    public void mouseDragged() {
        if (scene.eye().reference() == null)
            if (mouseButton == LEFT)
                // same as: scene.spin(scene.eye());
                scene.spin();
            else if (mouseButton == RIGHT)
                // same as: scene.translate(scene.eye());
                scene.translate();
            else
                // same as: scene.zoom(mouseX - pmouseX, scene.eye());
                scene.scale(mouseX - pmouseX);
    }

    // highlighting and 'third-person' interaction
    public void mouseMoved(MouseEvent event) {
        // 1. highlighting
        scene.cast("mouseMoved", mouseX, mouseY);
        // 2. 'third-person interaction
        if (scene.eye().reference() != null)
            // press shift to move the mouse without looking around
            if (!event.isShiftDown())
                scene.lookAround();
    }

    public void mouseWheel(MouseEvent event) {
        // same as: scene.scale(event.getCount() * 20, scene.eye());
        scene.scale(event.getCount() * 20);
    }

    // picks up a boid avatar, may be null
    public void mouseClicked() {
        scene.cast("mouseClicked", mouseX, mouseY);
    }

    // Sets current avatar as the eye reference and interpolate the eye to it
    public void thirdPerson() {
        scene.eye().setReference(avatar);
        scene.fit(avatar, 0);
    }

    // Resets the eye
    public void resetEye() {
        // same as: scene.eye().setReference(null);
        scene.eye().resetReference();
        scene.lookAt(scene.center());
        scene.fit(1);
    }

    public void walls() {
        pushStyle();
        noFill();
        stroke(255);

        line(0, 0, 0, 0, flockHeight, 0);
        line(0, 0, flockDepth, 0, flockHeight, flockDepth);
        line(0, 0, 0, flockWidth, 0, 0);
        line(0, 0, flockDepth, flockWidth, 0, flockDepth);

        line(flockWidth, 0, 0, flockWidth, flockHeight, 0);
        line(flockWidth, 0, flockDepth, flockWidth, flockHeight, flockDepth);
        line(0, flockHeight, 0, flockWidth, flockHeight, 0);
        line(0, flockHeight, flockDepth, flockWidth, flockHeight, flockDepth);

        line(0, 0, 0, 0, 0, flockDepth);
        line(0, flockHeight, 0, 0, flockHeight, flockDepth);
        line(flockWidth, 0, 0, flockWidth, 0, flockDepth);
        line(flockWidth, flockHeight, 0, flockWidth, flockHeight, flockDepth);
        popStyle();
    }

    boolean act = false;
    public void keyPressed() {
        act = true;

        switch (key) {
            case 'a':
                animate = !animate;
                break;
            case 's':
                if (scene.eye().reference() == null)
                    scene.fit(1);
                break;
            case 't':
                scene.shiftTimers();
                break;
            case 'p':
                println("Frame rate: " + frameRate);
                break;
            case 'v':
                avoidWalls = !avoidWalls;
                break;
            case ' ':
                if (scene.eye().reference() != null)
                    resetEye();
                else if (avatar != null)
                    thirdPerson();
                break;
        }
    }

    public void generateFish(String name){
        pshape = loadShape(sketchPath() + shapePath + name);

        Vector[] box = getBoundingBox(pshape);
        //Scale model
        float max = max(abs(box[0].x() - box[1].x()), abs(box[0].y() - box[1].y()), abs(box[0].z() - box[1].z()));
        //model.scale(200.f*1.f/max);
        //Invert Y Axis and set Fill
        objShape = new Node(scene);

        objShape.rotate(new Quaternion(new Vector(0, 0, 1), PI));
        objShape.scale(200.f * 1.f / max);
        Node root = fishSkeleton(objShape);

        ArrayList<Node> skeleton = (ArrayList<Node>) scene.branch(root);
        objShape.scale(0.4f);
        objShape.rotate(new Quaternion(new Vector(0, 1, 0), -PI/2.f));
        //Uncomment to use Linear Blending Skinning with CPU
        LinearBlendSkinning s = new LinearBlendSkinning(objShape, pshape);
        s.setup(skeleton);
        skinning.add(s);
        //Adding IK behavior
        Node target = new Node(scene);
        target.setReference(objShape);
        target.setPosition(skeleton.get(skeleton.size() - 1).position());
        //Making a default Path that target must follow
        setupTargetInterpolator(objShape, target);
        Solver solver = scene.registerTreeSolver(root);
        solver.error = 0.1f;
        scene.addIKTarget(skeleton.get(skeleton.size() - 1), target);
    }

    public Node fishSkeleton(Node reference) {
        Node j1 = new Node(scene);
        j1.setReference(reference);
        j1.setPosition(0, 10.8f, 93);
        Node j2 = new Node(scene);
        j2.setReference(j1);
        j2.setPosition(0, 2.3f, 54.7f);
        Node j3 = new Node(scene);
        j3.setReference(j2);
        j3.setPosition(0, 0.4f, 22);
        Node j4 = new Node(scene);
        j4.setReference(j3);
        j4.setPosition(0, 0, -18);
        Node j5 = new Node(scene);
        j5.setReference(j4);
        j5.setPosition(0, 1.8f, -54);
        Node j6 = new Node(scene);
        j6.setReference(j5);
        j6.setPosition(0, -1.1f, -95);
        return j1;
    }

    public Interpolator setupTargetInterpolator(Node reference, Node target) {
        Interpolator targetInterpolator = new Interpolator(target);
        targetInterpolator.setLoop();
        targetInterpolator.setSpeed(8.2f);
        // Create an initial path
        int nbKeyFrames = 10;
        float step = 2.0f * PI / (nbKeyFrames - 1);
        for (int i = 0; i < nbKeyFrames; i++) {
            Node iFrame = new Node(scene);
            iFrame.setReference(reference);
            iFrame.setTranslation(new Vector(140 * sin(step * i), target.translation().y(), target.translation().z()));
            targetInterpolator.addKeyFrame(iFrame);
        }
        targetInterpolator.start();
        return targetInterpolator;
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

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.flock.Flock"});
    }
}
