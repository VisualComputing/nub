package ik.basic;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.ik.solver.Solver;
import nub.ik.solver.evolutionary.BioIk;
import nub.ik.solver.evolutionary.GASolver;
import nub.ik.solver.evolutionary.HAEASolver;
import nub.ik.solver.evolutionary.HillClimbingSolver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

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
  static String dir = "C:/Users/usuario/Desktop/Sebas/Evolutivos/EvolutionaryClass/IK/Last/"; //"/home/sebchaparr/Schap/Evolutivos/IK/Hill/";

  public void setConstraint(float down, float up, float left, float right, Node f, Vector twist, float boneLength) {
    BallAndSocket constraint = new BallAndSocket(down, up, left, right);
    constraint.setRestRotation(f.rotation().get(), f.displacement(new Vector(0, 1, 0)), f.displacement(twist));
    f.setConstraint(constraint);
  }

  public static ArrayList<Node> generateChain(int num_joints, float boneLength) {
    ArrayList<Node> chain = new ArrayList<Node>();
    Node prevJoint = null;
    for (int i = 0; i < num_joints; i++) {
      Node joint;
      joint = Node.detach(new Vector(), new Quaternion(), 1f);
      if (prevJoint != null) joint.setReference(prevJoint);
      float x = 0;
      float z = 1;
      float y = 0;
      Vector translate = new Vector(x, y, z);
      translate.normalize();
      translate.multiply(boneLength);
      joint.setTranslation(translate);
      joint.setPickingThreshold(0.25f);
      prevJoint = joint;
      chain.add(joint);
    }
    return chain;
  }

  public static ArrayList<Node> copy(List<? extends Node> chain) {
    ArrayList<Node> copy = new ArrayList<Node>();
    Node reference = chain.get(0).reference();
    if (reference != null) {
      reference = Node.detach(reference.position().get(), reference.orientation().get(), 1);
    }
    for (Node joint : chain) {
      Node newJoint = Node.detach(new Vector(), new Quaternion(), 1f);
      newJoint.setReference(reference);
      newJoint.setPosition(joint.position().get());
      newJoint.setOrientation(joint.orientation().get());
      newJoint.setConstraint(joint.constraint());
      copy.add(newJoint);
      reference = newJoint;
    }
    return copy;
  }


  public static Node generateRandomReachablePosition(List<? extends Node> original) {
    ArrayList<? extends Node> chain = copy(original);
    for (int i = 0; i < chain.size(); i++) {
      chain.get(i).rotate(new Quaternion(Vector.random(), (float) (random.nextFloat() * Math.PI)));
    }
    return chain.get(chain.size() - 1);
  }

  public static void generateExperiment(Solver solver, int iterations, int times) {
    if (solver instanceof HillClimbingSolver) {
      generateExperiment((HillClimbingSolver) solver, iterations, times);
    } else if (solver instanceof GASolver) {
      generateExperiment((GASolver) solver, iterations, times);
    } else if (solver instanceof HAEASolver) {
      generateExperiment((HAEASolver) solver, iterations, times);
    } else if (solver instanceof BioIk) {
      generateExperiment((BioIk) solver, iterations, times);
    }
  }

  public static void generateExperiment(HillClimbingSolver method, int iterations, int times) {
    String path = dir + "Hill/";
    String name = "joints" + num_joints;
    name += method.powerLaw() ? "hill_power_law" : "hill_gaussian";
    PrintWriter pw = null;
    try {
      pw = new PrintWriter(new File(path + name));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    StringBuilder sb = new StringBuilder();
    method.setMaxIterations(iterations);
    double avg_time = 0;
    for (int i = 0; i < times; i++) {
      for (Node f : method.chain()) {
        f.setRotation(new Quaternion());
      }
      Node target = generateRandomReachablePosition(method.chain());
      method.setTarget(target);
      double time = System.currentTimeMillis();
      double[] r = method.execute();
      avg_time += System.currentTimeMillis() - time;
      for (int j = 0; j < r.length; j++) {
        sb.append(r[j]);
        if (j != r.length - 1)
          sb.append(",");
      }
      sb.append("\n");
    }
    avg_time /= times;
    System.out.println(" Avg Time : " + avg_time);

    experiment++;
    pw.write(sb.toString());
    pw.close();
  }

  public static void generateExperiment(GASolver method, int iterations, int times) {
    String path = dir + "GA/";
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
    method.setMaxIterations(iterations);
    double avg_time = 0;
    for (int i = 0; i < times; i++) {
      for (Node f : method.structure()) {
        f.setRotation(new Quaternion());
      }
      Node target = generateRandomReachablePosition(method.structure());
      method.setTarget(method.endEffector(), target);

      double time = System.currentTimeMillis();
      method.execute();
      avg_time += System.currentTimeMillis() - time;

      for (int j = 0; j < method.statistics().size(); j++) {
        results[0][j] += method.statistics().get(j).best() / times;
        results[1][j] += method.statistics().get(j).worst() / times;
        results[2][j] += method.statistics().get(j).avg() / times;
        results[3][j] += method.statistics().get(j).median() / times;
        results[4][j] += method.statistics().get(j).stdAvg() / times;
        results[5][j] += method.statistics().get(j).stdMedian() / times;
      }
    }
    avg_time /= times;
    System.out.println(" Avg Time : " + avg_time);


    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < iterations; j++) {
        sb.append(results[i][j]);
        if (j != iterations - 1)
          sb.append(",");
      }
      sb.append("\n");
    }
    experiment++;
    pw.write(sb.toString());
    pw.close();
  }


  public static void generateExperiment(HAEASolver method, int iterations, int times) {
    String path = dir + "HAEA/";
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
    method.setMaxIterations(iterations);

    double avg_time = 0;

    for (int i = 0; i < times; i++) {
      for (Node f : method.structure()) {
        f.setRotation(new Quaternion());
      }
      Node target = generateRandomReachablePosition(method.structure());
      method.setTarget(method.endEffector(), target);

      double time = System.currentTimeMillis();
      method.execute();
      avg_time += System.currentTimeMillis() - time;

      for (int j = 0; j < method.statistics().size(); j++) {
        results[0][j] += method.statistics().get(j).best() / times;
        results[1][j] += method.statistics().get(j).worst() / times;
        results[2][j] += method.statistics().get(j).avg() / times;
        results[3][j] += method.statistics().get(j).median() / times;
        results[4][j] += method.statistics().get(j).stdAvg() / times;
        results[5][j] += method.statistics().get(j).stdMedian() / times;
        for (int k = 0; k < method.operatorsValues().length; k++) {
          results[6 + k][j] += method.operatorsValues()[k][j] / times;
        }
      }
    }
    avg_time /= times;
    System.out.println(" Avg Time : " + avg_time);


    for (int i = 0; i < results.length; i++) {
      for (int j = 0; j < iterations; j++) {
        sb.append(results[i][j]);
        if (j != iterations - 1)
          sb.append(",");
      }
      sb.append("\n");
    }
    experiment++;
    pw.write(sb.toString());
    pw.close();
  }


  public static void generateExperiment(BioIk method, int iterations, int times) {
    String path = dir + "BioIk/";
    String name = "joints" + num_joints;
    name += "BIO_pop_" + method.populationSize() + "_elite_" + method.elitismSize();
    PrintWriter pw = null;
    try {
      pw = new PrintWriter(new File(path + name));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    float[][] results = new float[6][iterations];

    StringBuilder sb = new StringBuilder();
    method.setMaxIterations(iterations);

    double avg_time = 0;

    for (int i = 0; i < times; i++) {
      for (Node f : method.structure()) {
        f.setRotation(new Quaternion());
      }
      Node target = generateRandomReachablePosition(method.structure());
      method.setTarget(method.endEffector(), target);

      double time = System.currentTimeMillis();
      method.execute();
      avg_time += System.currentTimeMillis() - time;

      for (int j = 0; j < method.statistics().size(); j++) {
        results[0][j] += method.statistics().get(j).best() / times;
        results[1][j] += method.statistics().get(j).worst() / times;
        results[2][j] += method.statistics().get(j).avg() / times;
        results[3][j] += method.statistics().get(j).median() / times;
        results[4][j] += method.statistics().get(j).stdAvg() / times;
        results[5][j] += method.statistics().get(j).stdMedian() / times;
      }
    }
    avg_time /= times;
    System.out.println(" Avg Time : " + avg_time);

    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < iterations; j++) {
        sb.append(results[i][j]);
        if (j != iterations - 1)
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

    //Hill Climbing

        /*solvers.add(new HillClimbingSolver(PApplet.radians(3), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(3), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(PApplet.radians(5), generateChain(num_joints, boneLength)));
        solvers.add(new HillClimbingSolver(2.5, PApplet.radians(5), generateChain(num_joints, boneLength)));*/

    //GA

    solvers.add(new GASolver(generateChain(num_joints, boneLength), 10));
    solvers.add(new GASolver(generateChain(num_joints, boneLength), 20));
    solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 10, true));
    //solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 15, true));
    solvers.add(new HAEASolver(generateChain(num_joints, boneLength), 20, true));


    //BioIK
        /*solvers.add(new BioIk(generateChain(num_joints, boneLength), 10, 4));
        solvers.add(new BioIk(generateChain(num_joints, boneLength), 10, 6));
        solvers.add(new BioIk(generateChain(num_joints, boneLength), 20, 12));*/


    //generate experiments
    int counter = 0;
    for (Solver solver : solvers) {
      System.out.println("On method : " + ++counter + " of " + solvers.size());
      generateExperiment(solver, 300, 30);
    }
    System.out.println("Finished...");
  }


}
