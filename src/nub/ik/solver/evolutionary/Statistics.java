package nub.ik.solver.evolutionary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by sebchaparr on 30/10/18.
 */
public class Statistics {
  protected float _best = Float.MAX_VALUE, _worst = Float.MIN_VALUE, _avg, _median, _std_avg, _std_median;

  public float median(List<Float> sorted) {
    int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
      return sorted.get(middle);
    } else {
      return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0f;
    }
  }

  public float best() {
    return _best;
  }

  public float worst() {
    return _worst;
  }

  public float median() {
    return _median;
  }

  public float avg() {
    return _avg;
  }

  public float stdAvg() {
    return _std_avg;
  }

  public float stdMedian() {
    return _std_median;
  }

  public float std(List<Float> population, float mean) {
    int sum = 0;
    for (Float individual : population) {
      sum += (individual - mean) * (individual - mean);
    }
    return (float) Math.sqrt(sum / population.size());
  }

  public Statistics(List population) {
    if (population.isEmpty()) return;
    List<Float> sorted;
    if (population.get(0) instanceof Individual) {
      sorted = new ArrayList<Float>();
      for (Object individual : population) {
        sorted.add(((Individual) individual).fitness());
      }
    } else sorted = new ArrayList<Float>(population);
    sorted.sort(new Comparator<Float>() {
      @Override
      public int compare(Float o1, Float o2) {
        if (o1 < o2) {
          return -1;
        } else if (o1 > o2) {
          return 1;
        } else {
          return 0;
        }
      }
    });
    for (Float individual : sorted) {
      _best = Math.min(individual, _best);
      _worst = Math.max(individual, _worst);
      _avg += individual;
    }
    _avg = _avg / sorted.size();
    _median = median(sorted);
    _std_avg = std(sorted, _avg);
    _std_median = std(sorted, _median);
  }

}
