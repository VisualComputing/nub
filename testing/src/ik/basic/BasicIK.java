package ik.basic;

import common.InteractiveNode;
import frames.processing.Shape;
import processing.core.PApplet;
import frames.core.Graph;
import frames.ik.Solver;
import frames.primitives.constraint.BallAndSocket;
import frames.primitives.constraint.PlanarPolygon;
import frames.primitives.constraint.SphericalPolygon;
import frames.processing.Scene;
import java.util.ArrayList;
import frames.core.*;
import frames.primitives.*;
import ik.common.*;
/*
* Tree different chains (With different kind of constraints) are pursuing the same Target
* */

public class BasicIK extends PApplet {
    Scene scene;
    Node eye;
    Target target;

    int num_joints = 8;

    //Ball and Socket
    public static float constraint_factor_x = 50;
    public static float constraint_factor_y = 50;

    public static float boneLength = 15;

    int TimesPerFrame = 1;


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

        target = new Target(scene);
        target.translate(0, 0,0 );

        //Four identical chains that will have different constraints
        ArrayList<Node> branchUnconstrained = generateChain(num_joints, boneLength, new Vector(-scene.radius(), -scene.radius(), 0));
        ArrayList<Node> branchEllipseConstraint = generateChain(num_joints, boneLength, new Vector(-scene.radius(), scene.radius(), 0));
        ArrayList<Node> branchPlanarConstraints = generateChain(num_joints, boneLength, new Vector(scene.radius(), -scene.radius(), 0));
        ArrayList<Node> branchSphericalConstraints = generateChain(num_joints, boneLength, new Vector(scene.radius(), scene.radius(), 0));

        //Apply Constraints
        //Spherical Ellipse Constraints
        for (int i = 1; i < branchEllipseConstraint.size()-1; i++) {
            BallAndSocket constraint = new BallAndSocket(radians(constraint_factor_y), radians(constraint_factor_y), radians(constraint_factor_x), radians(constraint_factor_x));
            constraint.setRestRotation(branchEllipseConstraint.get(i).rotation().get());
            branchEllipseConstraint.get(i).setConstraint(constraint);
        }

        //Sinus cone planar Polygon constraints

        //Define the Base (Any Polygon in clockwise or Counterclockwise order)
        ArrayList<Vector> vertices = new ArrayList<Vector>();
        vertices.add(new Vector(-10,-10));
        vertices.add(new Vector(10,-10));
        vertices.add(new Vector(10, 10));
        vertices.add(new Vector(-10,10));

        for (int i = 1; i < branchPlanarConstraints.size()-1; i++) {
            PlanarPolygon constraint = new PlanarPolygon(vertices);
            constraint.setHeight(boneLength/2.f);
            constraint.setRestRotation(branchPlanarConstraints.get(i).rotation().get());
            branchPlanarConstraints.get(i).setConstraint(constraint);
        }

        //Define the Base (Any Polygon in Counterclockwise order)
        ArrayList<Vector> verticesSpherical = new ArrayList<Vector>();
        int numVertices = 12;
        for(int i = 0; i < numVertices; i++){
            float step = i*(2*PI/(float)numVertices);
            verticesSpherical.add(new Vector(cos(step),sin(step), random(0,1)));
        }

        for (int i = 1; i < branchSphericalConstraints.size()-1; i++) {
            SphericalPolygon constraint = new SphericalPolygon(verticesSpherical);
            constraint.setRestRotation(branchSphericalConstraints.get(i).rotation().get());
            branchSphericalConstraints.get(i).setConstraint(constraint);
        }

        Solver solverUnconstrained = scene.setIKStructure(branchUnconstrained.get(0));
        scene.addIKTarget(branchUnconstrained.get(branchUnconstrained.size()-1), target);
        solverUnconstrained.setTIMESPERFRAME(TimesPerFrame);

        Solver solverEllipseConstraint = scene.setIKStructure(branchEllipseConstraint.get(0));
        scene.addIKTarget(branchEllipseConstraint.get(branchEllipseConstraint.size()-1), target);
        solverEllipseConstraint.setTIMESPERFRAME(TimesPerFrame);

        Solver solverPlanarConstraints = scene.setIKStructure(branchPlanarConstraints.get(0));
        scene.addIKTarget(branchPlanarConstraints.get(branchPlanarConstraints.size()-1), target);
        solverPlanarConstraints.setTIMESPERFRAME(TimesPerFrame);

        Solver solverSphericalConstraints = scene.setIKStructure(branchSphericalConstraints.get(0));
        scene.addIKTarget(branchSphericalConstraints.get(branchSphericalConstraints.size()-1), target);
        solverSphericalConstraints.setTIMESPERFRAME(TimesPerFrame);
    }

    public ArrayList<Node> generateChain(int num_joints, float boneLength, Vector translation){
        Joint prevFrame = null;
        Joint chainRoot = null;
        int color = color(random(0,255), random(0,255), random(0,255), 100);
        for (int i = 0; i < num_joints; i++) {
            Joint iFrame;
            iFrame = new Joint(scene, color);
            if (i == 0)
                chainRoot = iFrame;
            if (prevFrame != null) iFrame.setReference(prevFrame);
            Vector translate = new Vector(1, 1, 1);
            translate.normalize();
            translate.multiply(boneLength);
            iFrame.setTranslation(translate);
            iFrame.setPrecision(Node.Precision.FIXED);
            prevFrame = iFrame;
        }
        //Consider Standard Form: Parent Z Axis is Pointing at its Child
        chainRoot.setTranslation(translation);
        chainRoot.setupHierarchy();
        return scene.branch(chainRoot);
    }

    public void draw() {
        background(0);
        lights();
        //Draw Constraints
        scene.drawAxes();

        for(Node frame : scene.nodes()){
            if(frame instanceof Shape) ((Shape) frame).draw();
        }
    }

    public static void main(String args[]) {
        PApplet.main(new String[]{"ik.basic.BasicIK"});
    }

}