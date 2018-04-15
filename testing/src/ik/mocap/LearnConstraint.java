package ik.mocap;

import frames.primitives.Vector;

import java.util.List;

/**
 * Created by sebchaparr on 15/04/18.
 */
public class LearnConstraint {
    List<Vector> _feasibleRegion;
    Vector _rest;

    public LearnConstraint(List<Vector> feasibleRegion){
        _feasibleRegion = feasibleRegion;
        _rest = feasibleRegion.get(0);
    }
    public LearnConstraint(List<Vector> feasibleRegion, Vector rest){
        _feasibleRegion = feasibleRegion;
        _rest = rest;
    }
}
