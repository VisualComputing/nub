package ik;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import remixlab.dandelion.core.Camera;
import remixlab.dandelion.core.GenericFrame;
import remixlab.dandelion.geom.Quat;
import remixlab.dandelion.geom.Vec;
import remixlab.dandelion.ik.Solver;
import remixlab.proscene.InteractiveFrame;
import remixlab.proscene.Scene;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 25/06/17.
 * A tree Structure with an IK Associated and a set of Targets (Red Spheres)
 * Each Branch of a sub-base (A frame with 2 or more Children) doesn't affect
 * the orientation of the other Branches
 */
public class MultipleEndEffectorIK3D extends PApplet {
    //Frames Hierarchy parameters
    int numSliblings = 6;
    int numJoints = 8;
    float distanceBtwnSliblings = 30.f;
    float boneLength = 20;

    //Solver Parameters
    int TimesPerFrame = 1;

    //Scene Parameters
    Scene scene;
    InteractiveFrame root;
    ArrayList<InteractiveFrame> targets = new ArrayList<InteractiveFrame>();//one target per Leaf


    public void settings() {
        size(500, 500, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setCameraType(Camera.Type.ORTHOGRAPHIC);
        scene.setAxesVisualHint(true);

        for(int i = 0; i < numSliblings; i++)
            targets.add(new InteractiveFrame(scene, "targetGraphics"));

        root = new InteractiveFrame(scene);
        ArrayList<InteractiveFrame> leaves;

        leaves = generateTree(root,  numSliblings, boneLength);
        int idx = leaves.size()/2;
        leaves = generateTree(leaves.get(idx),  numSliblings, boneLength);
        idx = leaves.size()/2;
        leaves = generateTree(leaves.get(idx),  numSliblings, boneLength);
        idx = leaves.size()/2;
        leaves = generateTree(leaves.get(idx),  numSliblings, boneLength);

        //Create IK Solver to the given Structure
        Solver solver = scene.setIKStructure(root);
        solver.setTIMESPERFRAME(TimesPerFrame);
        solver.setMINCHANGE(0.001f);

        //Create and associate Targets to each Leave at the last Level of the Structure
        for(int i = 0; i < leaves.size(); i++){
            targets.get(i).setPosition(leaves.get(i).position());
            targets.get(i).setOrientation(leaves.get(i).orientation());
            scene.addIKTarget(leaves.get(i), targets.get(i));
        }
    }

    public void frameGraphics(InteractiveFrame iFrame, PGraphics pg) {
        pg.pushStyle();
        scene.drawAxes(pg, 3);
        pg.fill(0, 255, 0);
        pg.strokeWeight(5);
        pg.stroke(0, 100, 100, 100);
        if (iFrame.referenceFrame() != null) {
            Vec v = iFrame.coordinatesOfFrom(new Vec(), iFrame.referenceFrame());
            if(pg.is2D())
                pg.line(0, 0, v.x(), v.y());
            else
                pg.line(0, 0, 0, v.x(), v.y(), v.z());

        }
        pg.popStyle();
    }

    public void targetGraphics(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(255, 0, 0, 200);
        if(pg.is2D())
            pg.ellipse(0, 0, 5, 5);
        else
            pg.sphere(5);
        pg.popStyle();
    }


    public void draw() {
        background(0);
        scene.drawFrames();
    }

    public ArrayList<InteractiveFrame> generateTree(GenericFrame root, int numSliblings, float boneLength){
        ArrayList<InteractiveFrame> frames = new ArrayList<InteractiveFrame>();
        //Generate children
        float step = PI/numSliblings;
        for(int j = 0; j < numSliblings; j++){
            InteractiveFrame dummy = new InteractiveFrame(scene, "frameGraphics");
            dummy.setReferenceFrame(root);
            Vec vec = new Vec(0,boneLength, 0);
            Quat q = new Quat(new Vec(0,0,1), step*j - PI);
            vec = q.multiply(vec);
            InteractiveFrame child = new InteractiveFrame(scene, "frameGraphics");
            child.setReferenceFrame(dummy);
            child.translate(vec);
            frames.add(child);
        }
        return frames;
    }


    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.MultipleEndEffectorIK3D"});
    }
}
