package nub.ik.solver.evolutionary.operator;

import nub.ik.solver.evolutionary.Individual;

/**
 * Created by sebchaparr on 29/10/18.
 */
public abstract class Operator {
  protected int _arity = 1;

  public int arity() {
    return _arity;
  }

  public void setArity(int arity) {
    _arity = arity;
  }

  public abstract Individual apply(Individual... individuals);
}
