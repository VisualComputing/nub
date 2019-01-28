package frames.ik.evolution.operator;

import frames.core.Frame;
import frames.ik.evolution.Individual;
import frames.ik.evolution.Util;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class OperatorMethods {
    //TODO : Keep cache of euler angles or do computations in terms of Quaternions
    public static class UniformMutation extends Operator{
        protected float _delta = (float) Math.toRadians(30);

        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            int n = individual.structure().size();
            //Define how many genes mutate on average
            float alpha = 1.0f / n;
            float beta = _delta;

            for (int i = 0; i < individual.structure().size(); i++) {
                if (Util.random.nextFloat() > alpha) continue;
                Frame joint = individual.structure().get(i);
                //modify each Euler Angle
                //rotate
                float roll = 2 * beta * Util.random.nextFloat() - beta;
                float pitch = 2 * beta * Util.random.nextFloat() - beta;
                float yaw = 2 * beta * Util.random.nextFloat() - beta;
                //rotate method consider constraints
                joint.rotate(new Quaternion(roll, pitch, yaw));
            }
            return individual;
        }
    }

    public static class GaussianMutation extends Operator{
        protected float _sigma = (float) Math.toRadians(30);

        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            int n = individual.structure().size();
            //Define how many genes mutate on average
            float alpha = 1.0f / n;
            float beta = _sigma;

            for (int i = 0; i < individual.structure().size(); i++) {
                if (Util.random.nextFloat() > alpha) continue;
                Frame joint = individual.structure().get(i);
                //modify each Euler Angle
                //rotate
                float roll = (float) (Util.random.nextGaussian() * beta);
                float pitch = (float) (Util.random.nextGaussian() * beta);
                float yaw = (float) (Util.random.nextGaussian() * beta);
                //rotate method consider constraints
                joint.rotate(new Quaternion(roll, pitch, yaw));
            }
            return individual;
        }
    }

    public static class ConvexCombination extends Operator{
        protected float[] _weights;
        protected boolean _randomWeights = true;

        public ConvexCombination(){
            _arity = 2;
        }

        public ConvexCombination(float[] weights){
            _arity = weights.length;
            _weights = weights;
        }

        @Override
        public Individual apply(Individual... individuals) {
            if(_weights != null){
                _weights = _weights.length != individuals.length ? null : _weights;
            }
            Individual combination = individuals[0].clone();

           // Set<String> params = individuals[0].parameters().keySet();

            for (int i = 0; i < individuals[0].structure().size(); i++){
                float roll = 0;
                float pitch = 0;
                float yaw = 0;
                int j = 0;
                float sum = 0;
                for (Individual individual : individuals){
                    float w = _weights != null ? _weights[j] : _randomWeights ? Util.random.nextFloat() : 1;
                    Vector euler = individual.structure().get(i).rotation().eulerAngles();
                    roll += w * euler.x();
                    pitch += w * euler.y();
                    yaw += w * euler.z();
                    sum += w;
                }
                combination.structure().get(i).setRotation(new Quaternion(roll/sum, pitch/sum, yaw/sum ));
            }
            return combination;
        }

        public void setWeights(float[] weights){
            if(weights == null){
                _weights = null;
                return;
            }
            //normalize
            float sum = 0;
            for(int i = 0; i < weights.length; i++) sum += weights[i];
            for(int i = 0; i < weights.length; i++) weights[i] /= sum;
            _weights = weights;
        }
    }

    //Operators based on Sebastian Stark's Thesis
    public static class Mutation extends Operator{
        protected float _extinction;

        public void setExtinction(Individual... individuals){
            _extinction = 0;
            for(Individual ind : individuals){
                _extinction += ind.floatParams().get("Extinction");
            }
            _extinction /= individuals.length;
        }

        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            int n = individual.structure().size();
            //Define how many genes mutate on average
            float alpha = (_extinction * (n - 1) + 1) / n;
            float beta = _extinction * (float) Math.PI;

            for (int i = 0; i < individual.structure().size(); i++) {
                if (Util.random.nextFloat() > alpha) continue;
                Frame joint = individual.structure().get(i);
                //modify each Euler Angle
                //rotate
                float roll = 2 * beta * Util.random.nextFloat() - beta;
                float pitch = 2 * beta * Util.random.nextFloat() - beta;
                float yaw = 2 * beta * Util.random.nextFloat() - beta;
                //rotate method consider constraints
                joint.rotate(new Quaternion(roll, pitch, yaw));
            }
            return individual;
        }
    }

    public static class Recombination extends Operator{
        public Recombination(){
            _arity = 2;
        }

        public Recombination(int arity){
            _arity = arity;
        }

        @Override
        public Individual apply(Individual... individuals) {
            Individual combination = individuals[0].clone();

            for (int i = 0; i < individuals[0].structure().size(); i++){
                float roll = 0;
                float pitch = 0;
                float yaw = 0;
                float sum = 0;
                for (Individual individual : individuals){
                    float w = Util.random.nextFloat();
                    Vector euler = individual.structure().get(i).rotation().eulerAngles();
                    roll += w * euler.x();
                    pitch += w * euler.y();
                    yaw += w * euler.z();
                    sum += w;
                }
                roll = roll/sum;
                pitch = pitch/sum;
                yaw = yaw/sum;

                for (Individual individual : individuals){
                        float[] g = individual.arrayParams().get("Evolution_Gradient");
                        if(g == null) continue;
                        float r = Util.random.nextFloat();
                        roll += r * g[3*i];
                        pitch += r * g[3*i + 1];
                        yaw += r * g[3*i + 2];
                }
                combination.structure().get(i).setRotation(new Quaternion(roll, pitch, yaw));
            }
            return combination;
        }
    }

    public static class Adoption extends Operator{
        protected Individual[] _parents;
        protected Individual _best;


        public Adoption(){
            _arity = 1;
        }

        public void setParents(Individual... individuals){
            _parents = individuals;
        }

        public void setBest(Individual best){
            _best = best;
        }

        @Override
        public Individual apply(Individual... individuals) {
            Individual combination = individuals[0].clone();
            Vector parents = new Vector();
            for (int i = 0; i < individuals[0].structure().size(); i++){
                for(Individual parent : _parents){
                    parents.add(parent.structure().get(i).rotation().eulerAngles());
                }
                parents.divide(_parents.length);
                Vector mine = individuals[0].structure().get(i).rotation().eulerAngles();
                Vector best = _best.structure().get(i).rotation().eulerAngles();

                Vector result = new Vector();

                float rp = Util.random.nextFloat();
                float rb = Util.random.nextFloat();

                for(int j = 0; j < 3; j++){
                    float wi = Util.random.nextFloat();
                    result._vector[j] = mine._vector[j] + wi*rp*(parents._vector[j] - mine._vector[j]) + (1-wi)*rb*(best._vector[i] - mine._vector[i]);
                }
                combination.structure().get(i).setRotation(new Quaternion(result.x(), result.y(), result.z()));
            }
            return combination;
        }
    }

    /*TODO : Greedy operator
    * Axis of Rotation is cross product defined by (EF - Joint) X (Target - Joint)
    * Find angle by means of dot product (EF - Joint) dot (Target - Joint).
    * Multiply angle by w ~ U(0,1) (Pitfall: Not consider propagation of the movement to each joint)
    * It'll perform similar to CCD but Order is not taken into account
    * Could perform badly when joint has constraints
    * */

    /*TODO : FABRIK operator
     * Options:
     * 1) Do a whole FABRIK Iteration (Stop if Error To Root is too big)
     * 2) a. Get randomly a Joint Ji, Translate Ji+1 by (Target - EF)
     *    b. Do a whole FABRIK Iteration From J0...Ji (Being Ji+1 the Target) (Doesn't seem promising)
     * Pitfalls : Costly operation
     * */


}
