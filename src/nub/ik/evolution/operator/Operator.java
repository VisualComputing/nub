package nub.ik.evolution.operator;

import nub.ik.evolution.Individual;

import java.util.List;

/**
 * Created by sebchaparr on 29/10/18.
 */
public abstract class Operator {
    protected int _arity = 1;
    public int arity(){
        return _arity;
    }
    public void setArity(int arity){
        _arity = arity;
    }
    public abstract Individual apply(Individual... individuals);
}
