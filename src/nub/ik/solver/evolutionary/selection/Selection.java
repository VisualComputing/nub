package nub.ik.solver.evolutionary.selection;

import nub.ik.solver.evolutionary.Individual;

import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public interface Selection {
  List<Individual> choose(boolean replacement, List<Individual> population, int m);
}
