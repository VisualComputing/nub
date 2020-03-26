package ik.experimental;

import ik.basic.Util;
import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GeneralizedDistanceVSIterations {
    static float PI = (float) Math.PI;
    static Random random = new Random();
    static int seed = 0;
    static String dir = "C:/Users/olgaa/Desktop/Sebas/Thesis/Results/GeneralizedDistanceVSIterations/";

    //Benchmark Parameters
    static boolean continuousPath = false;
    static int numStructures = 100;
    static int numPostures = 1000; //Set the number of different postures to solve
    static int randRotation = -1; //Set seed to generate initial random rotations, otherwise set to -1
    static int randLength = 0; //Set seed to generate random segment lengths, otherwise set to -1
    static float boneLength = 50; //Define length of segments (bones)
    static int numJoints;


    static Util.ConstraintType constraintTypes[] = {Util.ConstraintType.NONE, Util.ConstraintType.HINGE, Util.ConstraintType.HINGE_ALIGNED, Util.ConstraintType.CONE_CIRCLE, Util.ConstraintType.CONE_ELLIPSE, Util.ConstraintType.MIX, Util.ConstraintType.MIX_CONSTRAINED}; //Choose what kind of constraints apply to chain
    static Util.SolverType solversType [] = {Util.SolverType.CCD_TRIK, Util.SolverType.FORWARD_TRIANGULATION_TRIK, Util.SolverType.FINAL_TRIK, Util.SolverType.EXPRESSIVE_FINAL_TRIK, Util.SolverType.FABRIK};
    static List<Vector> targetPositions;

    static HashMap<Util.SolverType, StatisticsPerIteration[]> _statisticsPerSolver = new HashMap<>();
    static HashMap<Util.SolverType, ArrayList<Float>> _histPerSolver = new HashMap<>();


    public static void generateExperiment(Util.SolverType type, StatisticsPerIteration[] stats, ArrayList<Float> hist, Util.ConstraintType constraintType, int iterations, int seed){
        //1. Generate structure
        List<Node> structure = Util.generateDetachedChain(numJoints, boneLength, randRotation, randLength);
        Node endEffector = structure.get(structure.size() - 1);
        //2. Apply constraints
        Util.generateConstraints(structure, constraintType, seed, true);
        //3. generate solver
        Solver solver = Util.createSolver(type, structure);
        //4. Define solver parameters
        solver.setMaxError(-1);
        solver.setMaxIterations(iterations);
        solver.setMinDistance(-1);
        //5. Set target
        Node target = new Node();
        target.setPosition(endEffector.position());
        target.setOrientation(endEffector.orientation());
        solver.setTarget(endEffector, target);
        int sample = 0;
        for(Vector t : targetPositions){
            if(sample % 100 == 0) System.out.println(type.name() + "On sample : " + sample);
            target.setPosition(t.get());
            for(int i = 0; i < iterations; i ++){
                solver.solve();
                stats[i].addValue(solver.error());
            }
            hist.add(solver.error());
            sample++;
        }
    }

    public static void generateRandomReachablePositions(int n, int seed, Util.ConstraintType constraintType){
        List<Node> chain = Util.generateDetachedChain(numJoints, boneLength, randRotation, randLength);
        Util.generateConstraints(chain, constraintType, seed, true);

        targetPositions = new ArrayList<Vector>();
        for(int t = 0; t < n; t++) {
            for (int i = 0; i < chain.size() - 1; i++) {
                if(random.nextFloat() > 0.4f) {
                    chain.get(i).rotate(new Quaternion(new Vector(0, 0, 1), random.nextFloat() * 2 * PI - PI));
                    chain.get(i).rotate(new Quaternion(new Vector(0, 1, 0), random.nextFloat() * 2 * PI - PI));
                    chain.get(i).rotate(new Quaternion(new Vector(1, 0, 0), random.nextFloat() * 2 * PI - PI));
                }
            }
            //save the position of the target
            targetPositions.add(chain.get(chain.size() - 1).position().get());
        }
    }

    public static void generateRandomPath(int n, int seed, Util.ConstraintType constraintType){
        PApplet pa = new PApplet();
        pa.randomSeed(0);
        List<Node> chain = Util.generateDetachedChain(numJoints, boneLength, randRotation, randLength);
        Util.generateConstraints(chain, constraintType, seed, true);

        targetPositions = new ArrayList<Vector>();
        float last = 0.005f * n;
        for(float t = 0; t < last ; t += 0.005f) {
            //Generate a random near pose
            for (int i = 0; i < chain.size(); i++) {
                float angle = 2 * PI * pa.noise(1000 * i + t) - PI;
                Vector dir = new Vector(pa.noise(10000 * i + t), pa.noise(20000 * i + t), pa.noise(30000 * i + t));
                chain.get(i).rotate(dir, angle);
            }
            targetPositions.add(chain.get(chain.size() - 1).position().get());
        }
    }

    public static void main(String args[]) {
        random.setSeed(seed);
        NodeInformation.disableCache = true; //disable cache for "highly" precision benchmarking
        int numSolvers = solversType.length;
        //prepare random path/targets
        JSONObject jsonExperiment = new JSONObject();
        JSONArray jsonSolverArray = new JSONArray();

        int iterations = 100;
        //Initialize statistics per solver
        for(int s = 0; s < numSolvers; s++) {
            StatisticsPerIteration[] stats = new StatisticsPerIteration[iterations];
            for (int i = 0; i < iterations; i++) {
                stats[i] = new StatisticsPerIteration();
            }
            _statisticsPerSolver.put(solversType[s], stats);
            _histPerSolver.put(solversType[s], new ArrayList<Float>());
        }


        int c_seed = 0;
        for(int s = 0; s < numStructures; s++) {
            numJoints = random.nextInt(16) + 4;
            Util.ConstraintType constraintType = constraintTypes[random.nextInt(constraintTypes.length)];
            System.out.println("On structure " + s + " joints : " + numJoints + " constraint : " + constraintType);

            for(int i = 0; i < numSolvers; i++){
                if (continuousPath) generateRandomPath(numPostures, c_seed, constraintType);
                else generateRandomReachablePositions(numPostures, c_seed, constraintType);
                generateExperiment(solversType[i], _statisticsPerSolver.get(solversType[i]), _histPerSolver.get(solversType[i]), constraintType, 100, c_seed);
            }
            c_seed = random.nextInt(100000);
        }

        //Save all info in a json
        for(int s = 0; s < numSolvers; s++) {
            JSONObject jsonSolver = new JSONObject();
            jsonSolver.setString("name", solversType[s].name());
            StatisticsPerIteration[] stats = _statisticsPerSolver.get(solversType[s]);
            ArrayList<Float> hist = _histPerSolver.get(solversType[s]);

            for(int i = 0; i < iterations; i ++){
                stats[i].updateStatistics();
            }
            //save the statistics in a convenient file
            JSONArray meanValues = new JSONArray();
            JSONArray stdValues = new JSONArray();
            JSONArray minValues = new JSONArray();
            JSONArray maxValues = new JSONArray();
            JSONArray histValues = new JSONArray();

            for(int i = 0; i < stats.length; i++){
                meanValues.append(stats[i].mean());
                stdValues.append(stats[i].std());
                maxValues.append(stats[i].maxError());
                minValues.append(stats[i].minError());
            }

            for(int i = 0; i < hist.size(); i++){
                histValues.append(hist.get(i));
            }

            jsonSolver.setJSONArray("mean", meanValues);
            jsonSolver.setJSONArray("std", stdValues);
            jsonSolver.setJSONArray("max", maxValues);
            jsonSolver.setJSONArray("min", minValues);
            jsonSolver.setJSONArray("hist", histValues);
            jsonSolverArray.append(jsonSolver);
        }
        jsonExperiment.setJSONArray("solvers", jsonSolverArray);
        //save json file
        String name = continuousPath ? "continuous_path" : "discontinuous_path";
        //add the parameters to the files name
        name += "_joints_" + numJoints + "_postures_" + numPostures + "_structures_" + numStructures + "_seed_" + seed;
        jsonExperiment.save(new File(dir + name + ".json"), null);
    }

    public static class StatisticsPerIteration {
        protected List<Double> _errorValues = new ArrayList<Double>();

        protected double _minError = Float.MAX_VALUE, _maxError = Float.MIN_VALUE, _mean, _std;

        public double minError() {
            return _minError;
        }

        public double maxError() {
            return _maxError;
        }

        public double mean() {
            return _mean;
        }

        public double std() {
            return _std;
        }

        public float std(double mean) {
            int sum = 0;
            for (Double value : _errorValues) {
                sum += (value - mean) * (value - mean);
            }
            return (float) Math.sqrt(sum / _errorValues.size());
        }

        public void addValue(double value){
            _errorValues.add(value);
        }

        public void updateStatistics(){
            for(Double value : _errorValues){
                _minError = Math.min(value, _minError);
                _maxError = Math.max(value, _maxError);
                _mean += value;
            }
            _mean = _mean / _errorValues.size();
            _std = std(_mean);
        }
    }
}
