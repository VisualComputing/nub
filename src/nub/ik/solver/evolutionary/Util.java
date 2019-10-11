package nub.ik.solver.evolutionary;

import nub.core.Node;
import nub.primitives.Quaternion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 28/10/18.
 */
public class Util {
  public static Random random = new Random();

  public static List<Individual> sort(boolean inPlace, boolean reverse, List<Individual> population) {
    final int sign = reverse ? -1 : 1;
    List<Individual> sorted = population;
    if (!inPlace) sorted = new ArrayList<Individual>(population);
    sorted.sort(new Comparator<Individual>() {
      @Override
      public int compare(Individual o1, Individual o2) {
        if (o1.fitness() < o2._fitness) {
          return -sign;
        } else if (o1.fitness() > o2.fitness()) {
          return sign;
        } else {
          return 0;
        }
      }
    });
    if (inPlace) population = sorted;
    return sorted;
  }

  public static Individual generateIndividual(List<Node> structure) {
    return generateIndividual(new Individual(structure), (float) Math.toRadians(359));
  }

  public static Individual generateIndividual(List<Node> structure, float max_angle) {
    return generateIndividual(new Individual(structure), max_angle);
  }

  public static Individual generateIndividual(Individual original, float max_angle) {
    Individual individual = original.clone();
    for (Node frame : individual.structure()) {
      float roll = 2 * max_angle * random.nextFloat() - max_angle;
      float pitch = 2 * max_angle * random.nextFloat() - max_angle;
      float yaw = 2 * max_angle * random.nextFloat() - max_angle;
      frame.rotate(new Quaternion(roll, pitch, yaw));
    }
    individual.arrayParams().put("Evolution_Gradient", new float[individual.structure().size() * 3]);
    return individual;
  }

  public static void setupIndividual(Individual individual, float max_angle) {
    for (Node frame : individual.structure()) {
      float roll = 2 * max_angle * random.nextFloat() - max_angle;
      float pitch = 2 * max_angle * random.nextFloat() - max_angle;
      float yaw = 2 * max_angle * random.nextFloat() - max_angle;
      frame.rotate(new Quaternion(roll, pitch, yaw));
    }
    individual.arrayParams().put("Evolution_Gradient", new float[individual.structure().size() * 3]);
  }


  public static List<Individual> generatePopulation(List<Node> structure, int n, float max_angle) {
    List<Individual> population = new ArrayList<>();
    Individual original = new Individual(structure);
    original.arrayParams().put("Evolution_Gradient", new float[original.structure().size() * 3]);
    for (int i = 0; i < n - 1; i++) {
      population.add(generateIndividual(original, max_angle));
    }
    population.add(original.clone());
    return population;
  }

  public static List<Individual> generatePopulation(List<Node> structure, int n) {
    return generatePopulation(structure, n, (float) Math.toRadians(60));
  }

  public static void setupPopulation(List<Node> structure, List<Individual> population){
    population.get(0).setChain(structure);
    population.get(0).arrayParams().put("Evolution_Gradient", new float[population.get(0).structure().size() * 3]);
    for (int i = 1; i < population.size(); i++) {
      population.get(i).setChain(structure);
      setupIndividual(population.get(i), (float) Math.toRadians(60));
    }
  }


  public static List<Individual> concatenate(List<Individual>... lists) {
    List<Individual> concatenation = new ArrayList<>();
    for (List<Individual> list : lists) {
      concatenation.addAll(list);
    }
    return concatenation;
  }

  public static void printList(List<Individual> list) {
    System.out.print("[ ");
    for (Individual individual : list) {
      System.out.print(individual.fitness() + ", ");
    }
    System.out.println(" ]");
  }

  public static void main(String[] args) {
    //create _random Individuals
    Random r = new Random();
    List<Individual> l = new ArrayList<Individual>();
    for (int i = 0; i < 15; i++) {
      Individual ind = new Individual(null);
      ind.setFitness(r.nextFloat() * 100);
      l.add(ind);
    }
    printList(l);
    List<Individual> l2 = sort(true, true, l);
    printList(l);
    printList(l2);

  }
}
