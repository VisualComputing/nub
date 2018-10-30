package frames.ik.evolution.operator;

import frames.ik.evolution.Individual;

/**
 * Created by sebchaparr on 29/10/18.
 */
public interface Operator {
    Individual apply(Individual... individuals);
}
