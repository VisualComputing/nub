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
    public static class UniformMutation implements Operator{
        protected Random _random = new Random();
        protected float _delta = (float) Math.toRadians(5);
        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            //choose some position
            int index = _random.nextInt(individual.structure().size());
            Frame joint = individual.structure().get(index);
            //modify each Euler Angle
            //rotate
            float roll = 2*_delta * _random.nextFloat() - _delta;
            float pitch = 2*_delta * _random.nextFloat() - _delta;
            float yaw = 2*_delta * _random.nextFloat() - _delta;
            //rotate method consider constraints
            joint.rotate(new Quaternion(roll, pitch, yaw));
            return individual;
        }
    }

    public static class GaussianMutation implements Operator{
        protected Random _random = new Random();
        protected float _sigma = (float) Math.toRadians(5);
        @Override
        public Individual apply(Individual... individuals) {
            Individual individual = individuals[0].clone();
            //choose some position
            int index = _random.nextInt(individual.structure().size());
            Frame joint = individual.structure().get(index).get();
            //modify each Euler Angle
            //rotate
            float roll = (float) (_random.nextGaussian() * _sigma);
            float pitch = (float) (_random.nextGaussian() * _sigma);
            float yaw = (float) (_random.nextGaussian() * _sigma);
            //rotate method consider constraints
            joint.rotate(new Quaternion(roll, pitch, yaw));
            return individual;
        }
    }

    public static class ConvexCombination implements Operator{
        @Override
        public Individual apply(Individual... individuals) {
            Individual combination = individuals[0].clone();
            int n = individuals.length;
            for (int i = 0; i < individuals[0].structure().size(); i++){
                float roll = 0;
                float pitch = 0;
                float yaw = 0;
                for (Individual individual : individuals){
                    Vector euler = individual.structure().get(i).rotation().eulerAngles();
                    roll += euler.x();
                    pitch += euler.y();
                    yaw += euler.z();
                }
                combination.structure().get(i).setRotation(new Quaternion(roll/n, pitch/n, yaw/n ));
            }
            return combination;
        }
    }

}
