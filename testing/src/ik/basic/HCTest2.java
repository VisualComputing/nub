package ik.basic;

import frames.core.Frame;
import frames.core.constraint.BallAndSocket;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import processing.core.PApplet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class HCTest2{
    static int num_joints = 15;
    static float boneLength = 50;
    static int experiment = 0;
    //Methods
    static int num_solvers = 4;
    static ArrayList<Solver> solvers;

    public void setConstraint(float down, float up, float left, float right, Frame f, Vector twist, float boneLength){
        BallAndSocket constraint = new BallAndSocket(down, up, left, right);
        constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
        f.setConstraint(constraint);
    }

    public static ArrayList<Frame> generateChain(int num_joints, float boneLength) {
        ArrayList<Frame> chain = new ArrayList<Frame>();
        Frame prevJoint = null;
        for (int i = 0; i < num_joints; i++) {
            Frame joint;
            joint = new Frame();
            if (prevJoint != null) joint.setReference(prevJoint);
            float x = 0;
            float z = 1;
            float y = 0;
            Vector translate = new Vector(x,y,z);
            translate.normalize();
            translate.multiply(boneLength);
            joint.setTranslation(translate);
            joint.setPrecision(Frame.Precision.FIXED);
            prevJoint = joint;
            chain.add(joint);
        }
        return chain;
    }

    public static ArrayList<Frame> copy(List<? extends Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get());
        }
        for (Frame joint : chain) {
            Frame newJoint = new Frame();
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            reference = newJoint;
        }
        return copy;
    }


    public static Frame generateRandomReachablePosition(List<? extends Frame> original){
        ArrayList<? extends Frame> chain = copy(original);
        for(int i = 0; i < chain.size(); i++){
            float yaw = (float)(Math.random()*2*Math.PI - Math.PI);
            float pitch = (float)(Math.random()*2*Math.PI - Math.PI);
            float roll = (float)(Math.random()*2*Math.PI - Math.PI);
            chain.get(i).rotate(new Quaternion(roll, pitch, yaw));
        }
        return chain.get(chain.size()-1);
    }

    public static void generateExperiment(HillClimbingSolver method, int iterations, int times){
        String path = "/home/sebchaparr/Schap/Evolutivos/IK/";
        String name = "joints" + num_joints;
        name += method.powerLaw() ? "hill_power_law" : "hill_gaussian";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(path + name + experiment));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        Frame target = generateRandomReachablePosition(method.chain());
        method.setTarget(target);
        method.maxIter = iterations;
        for(int i = 0; i < times; i++) {
            double[] r = method.execute();
            for(int j = 0; j < r.length; j++){
                sb.append(r[j]);
                if(j != r.length - 1)
                    sb.append(",");
            }
            sb.append("\n");
        }
        experiment++;
        pw.write(sb.toString());
        pw.close();
    }

    public static void main(String args[]) {
        ArrayList<ArrayList<Frame>> structures = new ArrayList<>();

        for(int i = 0; i < num_solvers; i++){
            structures.add(generateChain(num_joints, boneLength));
        }

        solvers = new ArrayList<>();

        solvers.add(new HillClimbingSolver(PApplet.radians(3), structures.get(0)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(3), structures.get(1)));
        solvers.add(new HillClimbingSolver(PApplet.radians(5), structures.get(2)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(5), structures.get(3)));

        //generate experiments
        for(Solver solver : solvers) {
            generateExperiment((HillClimbingSolver) solver, 300, 30);
        }
        System.out.println("Finished...");
    }


}
