package frames.ik.evolution.selection;

import frames.ik.evolution.Individual;

import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public interface Selection {
    List<Individual> choose(boolean replacement, List<Individual> population, int m);
}
