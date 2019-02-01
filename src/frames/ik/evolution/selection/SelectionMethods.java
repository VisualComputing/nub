package frames.ik.evolution.selection;

import frames.ik.evolution.Individual;
import frames.ik.evolution.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 29/10/18.
 */
public class SelectionMethods {

    public static class Uniform implements Selection {
        @Override
        public List<Individual> choose(boolean replacement, List<Individual> population, int m) {
            ArrayList<Individual> choosed = new ArrayList<Individual>();
            List<Individual> list = replacement ? population : new ArrayList<Individual>(population);
            for(int i = 0; i < m; i++){
                int index = Util.random.nextInt(list.size());
                choosed.add(list.get(index));
                if(!replacement) list.remove(index);
            }
            return choosed;
        }
    }

    public static class Roulette implements Selection {
        protected List<Float> _fitness;

        public void setFitness(List<Float> fitness){
            _fitness = fitness;
        }

        protected float fitness(List<Individual> population, int i){
            return _fitness == null ? population.get(i).fitness() : _fitness.get(i);
        }

        protected float _sumFitness(List<Individual> population){
            float sum = 0;
            for(int i = 0; i < population.size(); i++){
                sum += fitness(population,i);
            }
            return sum;
        }


        @Override
        public List<Individual> choose(boolean replacement, List<Individual> population, int m) {
            ArrayList<Individual> choosed = new ArrayList<Individual>();
            List<Individual> list = replacement ? population : new ArrayList<Individual>(population);
            int n = list.size();
            float sum = _sumFitness(population);
            for(int i = 0; i < m; i++){
                float value = Util.random.nextFloat() * sum;
                boolean selected = false;
                for(int j = 0; j < n; j++){
                    value = value - fitness(list, j);
                    if(value < 0){
                        choosed.add(list.get(j));
                        selected = true;
                        if(!replacement) {
                            if(_fitness != null) _fitness.remove(j);
                            list.remove(j);
                        }
                        break;
                    }
                }
                if(!selected){
                    choosed.add(list.get(n-1));
                    if(!replacement) {
                        if(_fitness != null) _fitness.remove(n-1);
                        list.remove(n-1);
                    }
                }

            }
            return choosed;
        }
    }

    public static class Ranking implements Selection {
        protected Roulette roulette = new Roulette();
        protected boolean _exponential = false;
        protected float _alpha = 2;
        protected boolean _minimization = true;
        protected boolean _sorted = false;

        public Ranking(){ }

        public Ranking(boolean sorted){
            _sorted = sorted;
        }

        @Override
        public List<Individual> choose(boolean replacement, List<Individual> population, int m) {
            List<Individual> list = _sorted ? population : Util.sort(false, _minimization, population);
            List<Float> rank = new ArrayList<Float>();
            float tie = 0;
            for(int i = 0; i < list.size(); i++){
                if(i > 0)
                    tie = (list.get(i-1).fitness() == list.get(i).fitness()) ? tie + 1 : 0;
                else
                    tie = 0;
                float value = i + 1 - tie;
                if(_exponential) value = (float) Math.pow(_alpha, value/list.size());
                rank.add(value);
            }
            //apply roulette based on rankings
            roulette.setFitness(rank);
            return roulette.choose(replacement,list,m);
        }
    }

    public static class Tournament implements Selection {
        protected Uniform uniform = new Uniform();
        protected int _size = 4;
        protected boolean _minimization = true;
        @Override
        public List<Individual> choose(boolean replacement, List<Individual> population, int m) {
            ArrayList<Individual> choosed = new ArrayList<Individual>();
            List<Individual> list = replacement ? population : new ArrayList<Individual>(population);
            for(int i = 0; i < m; i++){
                List<Individual> t_population = uniform.choose(replacement, list, _size);
                t_population = Util.sort(true, !_minimization, t_population);
                Individual individual = t_population.get(0);
                choosed.add(individual);
                if(!replacement) list.remove(individual);
            }
            return choosed;
        }
    }

    public static class Elitism implements Selection {
        protected Uniform uniform = new Uniform();
        protected boolean _minimization = true;
        @Override
        public List<Individual> choose(boolean replacement, List<Individual> population, int m) {
            List<Individual> choosed = new ArrayList<Individual>();
            List<Individual> list = Util.sort(false, !_minimization, population);
            int n = list.size();
            int index_best = (int)(0.1*n);
            int index_normal = (int)(0.8*n);

            choosed.addAll(uniform.choose(replacement, list.subList(0,index_best + 1), (int) Math.ceil(m/2.f)));
            choosed.addAll(uniform.choose(replacement, list.subList(index_best + 1, index_normal + 1), m/2));

            return choosed;
        }
    }

}
