package ik.basic;

import common.InteractiveNode;
import ik.common.*;
import frames.core.Graph;
import frames.core.Node;
import frames.ik.Solver;
import frames.primitives.Vector;
import frames.processing.Scene;
import frames.processing.Shape;
import processing.core.PApplet;

import java.util.ArrayList;

/**
 * Created by sebchaparr on 23/02/18.
 * Inspired from
 * http://www.robosoup.com/2014/12/using-inverse-kinematics-to-develop-a-biped-robot-walking-gait-c.html
 */
public class BasicGait extends PApplet {
    Scene scene;
    Node eye;
    Target leftTarget, rightTarget;
    ArrayList<Node> leftLeg;
    ArrayList<Node> rightLeg;

    public static float boneLength = 15;

    public void settings() {
        size(700, 700, P3D);
    }

    public void setup() {
        scene = new Scene(this);
        scene.setType(Graph.Type.ORTHOGRAPHIC);
        eye = new InteractiveNode(scene);

        scene.setEye(eye);
        scene.setFieldOfView(PI / 3);
        scene.setDefaultGrabber(eye);
        scene.fitBallInterpolation();

        leftTarget = new Target(scene);
        rightTarget = new Target(scene);
        //Create the Structure of the left Leg
        leftLeg = generateChain(3, boneLength, new Vector(0, 0, boneLength * 2), 0);
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        leftLeg.get(0).setupHierarchy();
        leftTarget.setPosition(leftLeg.get(2).position());

        //Create the Structure of the right Leg
        rightLeg = generateChain(3, boneLength, new Vector(20, 0, boneLength * 2), 0);
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        rightLeg.get(0).setupHierarchy();
        rightTarget.setPosition(rightLeg.get(2).position());

        Solver solver = scene.registerTreeSolver(leftLeg.get(0));
        scene.addIKTarget(leftLeg.get(2), leftTarget);

        solver = scene.registerTreeSolver(rightLeg.get(0));
        scene.addIKTarget(rightLeg.get(2), rightTarget);
    }

    public ArrayList<Node> generateChain(int num_joints, float boneLength, Vector translation, float theta) {
        Joint prevFrame = null;
        Joint chainRoot = null;
        int color = color(random(0, 255), random(0, 255), random(0, 255), 100);
        for (int i = 0; i < num_joints; i++) {
            Joint iFrame;
            iFrame = new Joint(scene, color);
            if (i == 0)
                chainRoot = iFrame;
            if (prevFrame != null) iFrame.setReference(prevFrame);
            Vector translate = new Vector(sin(theta), 0, -cos(theta));
            translate.normalize();
            translate.multiply(boneLength);
            iFrame.setTranslation(translate);
            iFrame.setPrecision(Node.Precision.FIXED);
            prevFrame = iFrame;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        //chainRoot.setupHierarchy();
        return scene.branch(chainRoot);
    }

    int counter = 0;

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();

        for (Node frame : scene.nodes()) {
            if (frame instanceof Shape) ((Shape) frame).draw();
        }
        //target will follow a sin trajectory
        float theta = radians((counter * 2) % 360);
        rightLeg.get(0).translate(0, 0.1f, 0);
        leftLeg.get(0).translate(0, 0.1f, 0);
        if (theta < PI) {
            leftTarget.setPosition(0, counter * 0.1f, boneLength / 2.f * abs(sin(theta)));
        } else {
            rightTarget.setPosition(20, counter * 0.1f, boneLength / 2.f * abs(sin(theta)));
        }
        if (rightTarget.position().y() > 20) {
            leftLeg.get(0).setPosition(0, 0, boneLength * 2);
            rightLeg.get(0).setPosition(20, 0, boneLength * 2);
            leftTarget.setPosition(leftLeg.get(2).position());
            rightTarget.setPosition(rightLeg.get(2).position());
            counter = 0;
        }
        counter++;
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.BasicGait"});
    }

}