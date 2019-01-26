package ik.basic;

import frames.core.Frame;
import frames.core.constraint.BallAndSocket;
import frames.ik.HAEASolver;
import frames.ik.evolution.GASolver;
import frames.ik.evolution.HillClimbingSolver;
import frames.ik.Solver;
import frames.ik.evolution.Statistics;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import processing.core.PApplet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class Benchmark {
    static Random random = new Random();
    static int num_joints = 15;
    static float boneLength = 50;
    static int experiment = 0;
    //Methods
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
            joint.setPickingThreshold(0.25f);
            prevJoint = joint;
            chain.add(joint);
        }
        return chain;
    }

    public static ArrayList<Frame> copy(List<? extends Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get(),1);
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
            chain.get(i).rotate(new Quaternion(Vector.random(), (float)(random.nextGaussian()*random.nextFloat()*Math.PI/2)));
        }
        return chain.get(chain.size()-1);
    }

    public static void generateExperiment(Solver solver, int iterations, int times){
        if(solver instanceof  HillClimbingSolver){
            generateExperiment((HillClimbingSolver) solver, iterations, times);
        } else if(solver instanceof  GASolver){
            generateExperiment((GASolver) solver, iterations, times);
        } else if(solver instanceof  HAEASolver){
            generateExperiment((HAEASolver) solver, iterations, times);
        }
    }

    public static void generateExperiment(HillClimbingSolver method, int iterations, int times){
        String path = "/home/sebchaparr/Schap/Evolutivos/IK/Hill/";
        String name = "joints" + num_joints;
        name += method.powerLaw() ? "hill_power_law" : "hill_gaussian";
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(path + name));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        method.maxIter = iterations;
        for(int i = 0; i < times; i++) {
            for(Frame f : method.chain()){
                f.setRotation(new Quaternion());
            }
            Frame target = generateRandomReachablePosition(method.chain());
            method.setTarget(target);

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

    public static void generateExperiment(GASolver method, int iterations, int times){
        String path = "/home/sebchaparr/Schap/Evolutivos/IK/GA/";
        String name = "joints" + num_joints;
        name += "GA_pop_" + method.populationSize() + "_prob_" + method.crossProbability();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(path + name));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        float[][] results = new float[6][iterations];

        StringBuilder sb = new StringBuilder();
        method.maxIter = iterations;
        for(int i = 0; i < times; i++) {
            for(Frame f : method.structure()){
                f.setRotation(new Quaternion());
            }
            Frame target = generateRandomReachablePosition(method.structure());
            method.setTarget(method.endEffector(), target);

            method.execute();
            for(int j  = 0; j < method.statistics().size(); j++){
                results[0][j] += method.statistics().get(j).best()/times;
                results[1][j] += method.statistics().get(j).worst()/times;
                results[2][j] += method.statistics().get(j).avg()/times;
                results[3][j] += method.statistics().get(j).median()/times;
                results[4][j] += method.statistics().get(j).stdAvg()/times;
                results[5][j] += method.statistics().get(j).stdMedian()/times;
            }
        }

        for(int i = 0; i < 6; i++){
            for(int j = 0; j < iterations; j++){
                sb.append(results[i][j]);
                if(j != iterations - 1)
                    sb.append(",");
            }
            sb.append("\n");
        }
        experiment++;
        pw.write(sb.toString());
        pw.close();
    }


    public static void generateExperiment(HAEASolver method, int iterations, int times){
        String path = "/home/sebchaparr/Schap/Evolutivos/IK/HAEA/";
        String name = "joints" + num_joints;
        name += "HAEA_pop_" + method.populationSize();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File(path + name));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        float[][] results = new float[6 + method.operators().size()][iterations];

        StringBuilder sb = new StringBuilder();
        method.maxIter = iterations;
        for(int i = 0; i < times; i++) {
            for(Frame f : method.structure()){
                f.setRotation(new Quaternion());
            }
            Frame target = generateRandomReachablePosition(method.structure());
            method.setTarget(method.endEffector(), target);

            method.execute();
            for(int j  = 0; j < method.statistics().size(); j++){
                results[0][j] += method.statistics().get(j).best()/times;
                results[1][j] += method.statistics().get(j).worst()/times;
                results[2][j] += method.statistics().get(j).avg()/times;
                results[3][j] += method.statistics().get(j).median()/times;
                results[4][j] += method.statistics().get(j).stdAvg()/times;
                results[5][j] += method.statistics().get(j).stdMedian()/times;
                for(int k = 0; k < method.operatorsValues().length; k++){
                    results[6 + k][j] += method.operatorsValues()[k][j]/times;
                }
            }
        }

        for(int i = 0; i < results.length; i++){
            for(int j = 0; j < iterations; j++){
                sb.append(results[i][j]);
                if(j != iterations - 1)
                    sb.append(",");
            }
            sb.append("\n");
        }
        experiment++;
        pw.write(sb.toString());
        pw.close();
    }


    public static void main(String args[]) {
        solvers = new ArrayList<>();
        /*
        //Hill Climbing
        solvers.add(new HillClimbingSolver(PApplet.radians(3), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(3), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(PApplet.radians(5), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(5), generateChain(num_joints, boneLength)));
        */
        //GA
        solvers.add(new GASolver(generateChain(num_joints, boneLength), 10));
        solvers.add(new GASolver(generateChain(num_joints, boneLength), 10));
        solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 10, true));
        solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 15, true));
        solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 20, true));

        //generate experiments
        int counter = 0;
        for(Solver solver : solvers) {
            System.out.println("On method : " + ++counter + " of " + solvers.size());
            generateExperiment(solver, 300, 30);
        }
        System.out.println("Finished...");
    }


}
