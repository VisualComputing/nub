package frames.ik.evolution;

import frames.core.Frame;
import frames.ik.evolution.operator.Operator;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.*;

/**
 * Created by sebchaparr on 28/10/18.
 */
public class Util {
    public static Random random = new Random();

    public static List<Individual> sort(boolean inPlace, boolean reverse, List<Individual> population){
        final int sign = reverse ? -1 : 1;
        List<Individual> sorted = population;
        if(!inPlace) sorted = new ArrayList<Individual>(population);
        sorted.sort(new Comparator<Individual>() {
            @Override
            public int compare(Individual o1, Individual o2) {
                if(o1.fitness() < o2._fitness){
                    return -sign;
                } else if(o1.fitness() > o2.fitness()){
                    return sign;
                } else{
                    return 0;
                }
            }
        });
        if(inPlace) population = sorted;
        return sorted;
    }

    public static Individual generateIndividual(List<Frame> structure){
        return generateIndividual(new Individual(structure), (float) Math.toRadians(359));
    }

    public static Individual generateIndividual(List<Frame> structure, float max_angle){
        return generateIndividual(new Individual(structure), max_angle);
    }

    public static Individual generateIndividual(Individual original, float max_angle){
        Individual individual = original.clone();
        for(Frame frame : individual.structure()){
            float roll = 2 * max_angle * random.nextFloat() - max_angle;
            float pitch = 2 * max_angle * random.nextFloat() - max_angle;
            float yaw =  2 * max_angle * random.nextFloat() - max_angle;
            frame.rotate(new Quaternion(roll, pitch, yaw ));
        }
        individual.arrayParams().put("Evolution_Gradient", new float[individual.structure().size()*3]);
        return individual;
    }

    public static List<Individual> generatePopulation(List<Frame> structure, int n, float max_angle){
        List<Individual> population = new ArrayList<>();
        Individual original = new Individual(structure);
        for(int i = 0; i < n; i++){
            population.add(generateIndividual(original, max_angle));
        }
        population.add(original.clone());
        return population;
    }

    public static List<Individual> generatePopulation(List<Frame> structure, int n){
        return generatePopulation(structure, n, (float) Math.toRadians(30));
    }

    public static List<Individual> concatenate(List<Individual>... lists){
        List<Individual> concatenation = new ArrayList<>();
        for(List<Individual> list : lists){
            concatenation.addAll(list);
        }
        return concatenation;
    }

    public static void printList(List<Individual> list){
        System.out.print("[ ");
        for(Individual individual : list){
            System.out.print( individual.fitness() + ", ");
        }
        System.out.println(" ]");
    }

    public static void main(String[] args){
        //create _random Individuals
        Random r = new Random();
        List<Individual> l = new ArrayList<Individual>();
        for(int i = 0; i < 15; i++){
            Individual ind = new Individual(null);
            ind.setFitness(r.nextFloat()*100);
            l.add(ind);
        }
        printList(l);
        List<Individual> l2 = sort(true, true, l);
        printList(l);
        printList(l2);

    }
}
