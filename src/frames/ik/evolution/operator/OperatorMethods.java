package frames.ik.evolution.operator;

import frames.core.Frame;
import frames.ik.evolution.Individual;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class OperatorMethods {
    public static class UniformMutation extends Operator{
        protected Random _random = new Random();
        protected float _delta = (float) Math.toRadians(30);
        protected float _rate = 1;

        public void setRate(Individual... individuals){
            _rate = 0;
            for(Individual individual : individuals) _rate += individual.extinction();
            _rate /= individuals.length;
        }

        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            int n = individual.structure().size();
            //Define how many genes mutate on average
            float alpha = (_rate * (n - 1) + 1) / n;
            float beta = _rate * _delta;

            for (int i = 0; i < individual.structure().size(); i++) {
                if (_random.nextFloat() > alpha) continue;
                Frame joint = individual.structure().get(i);
                //modify each Euler Angle
                //rotate
                float roll = 2 * beta * _random.nextFloat() - beta;
                float pitch = 2 * beta * _random.nextFloat() - beta;
                float yaw = 2 * beta * _random.nextFloat() - beta;
                //rotate method consider constraints
                joint.rotate(new Quaternion(roll, pitch, yaw));
            }
            return individual;
        }
    }

    public static class GaussianMutation extends Operator{
        protected Random _random = new Random();
        protected float _sigma = (float) Math.toRadians(30);
        protected float _rate = 1;

        public void setRate(Individual... individuals){
            _rate = 0;
            for(Individual individual : individuals) _rate += individual.extinction();
            _rate /= individuals.length;
        }

        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            int n = individual.structure().size();
            //Define how many genes mutate on average
            float alpha = (_rate * (n - 1) + 1) / n;
            float beta = _rate * _sigma;

            for (int i = 0; i < individual.structure().size(); i++) {
                if (_random.nextFloat() > alpha) continue;
                Frame joint = individual.structure().get(i);
                //modify each Euler Angle
                //rotate
                float roll = (float) (_random.nextGaussian() * beta);
                float pitch = (float) (_random.nextGaussian() * beta);
                float yaw = (float) (_random.nextGaussian() * beta);
                //rotate method consider constraints
                joint.rotate(new Quaternion(roll, pitch, yaw));
            }
            return individual;
        }
    }

    public static class ConvexCombination extends Operator{
        protected Random _random = new Random();
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
            for (int i = 0; i < individuals[0].structure().size(); i++){
                float roll = 0;
                float pitch = 0;
                float yaw = 0;
                int j = 0;
                float sum = 0;
                for (Individual individual : individuals){
                    float w = _weights != null ? _weights[j] : _randomWeights ? _random.nextFloat() : 1;
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
}
