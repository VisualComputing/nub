package frames.ik.evolution;

import frames.core.Frame;
import frames.ik.Solver;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class HillClimbingSolver extends Solver {

    protected Random random = new Random();
    protected Frame _target;
    protected Frame _previousTarget;
    protected List<? extends Frame> _chain, _x_i;
    protected boolean _powerLaw;
    protected double _sigma;
    protected double _alpha = 2;

    public HillClimbingSolver(double sigma, List<? extends Frame> chain){
        this._powerLaw = false;
        this._sigma = sigma;
        this._chain = chain;
        _x_i = _copy(chain);
    }

    public HillClimbingSolver(double alpha, double sigma, List<? extends Frame> chain){
        this._powerLaw = true;
        this._alpha = alpha;
        this._sigma = sigma;
        this._chain = chain;
        _x_i = _copy(chain);
    }

    public double sigma(){ return _sigma; }

    public double alpha(){ return _alpha; }

    public boolean powerLaw(){
        return _powerLaw;
    }

    public double[] execute(){
        double[] results = new double[maxIter];
        int k = 0;
        _x_i = _copy(_chain);
        while(k < maxIter){
            _iterate();
            results[k] = _distanceToTarget(_x_i);
            k++;
        }
        return results;
    }

    public List<? extends Frame> chain(){
        return _chain;
    }

    public Frame target() {
        return _target;
    }

    public void setTarget(Frame target) {
        this._target = target;
    }

    protected double _distanceToTarget(List<? extends Frame> chain){
        return Vector.distance(chain.get(chain.size()-1).position(), _target.position());
    }

    public double distanceToTarget(){
        return _distanceToTarget(_chain);
    }

    protected double _powerLawGenerator(double x, double alpha){
        double coarse_alpha = 1.0/(1.0-alpha);
        return Math.pow(1.0 - x, coarse_alpha);
    }

    protected ArrayList<Frame> _copy(List<? extends Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get(), 1);
        }
        for (Frame joint : chain) {
            Frame newJoint = new Frame();
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            newJoint.setConstraint(joint.constraint());
            copy.add(newJoint);
            reference = newJoint;
        }
        return copy;
    }


    @Override
    protected boolean _iterate() {
        ArrayList<Frame> x_i1 = _copy(_x_i);
        for(int i = 0; i < _x_i.size(); i++) {
            int invert = random.nextDouble() >= 0.5 ? 1 : -1;
            //rotate
            float roll;
            float pitch;
            float yaw;
            if(_powerLaw){
                roll = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha)*_sigma);
                pitch = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha)*_sigma);
                yaw = (float) (invert * _powerLawGenerator(random.nextDouble(), _alpha)*_sigma);
            }else{
                roll = (float) (random.nextGaussian() * _sigma);
                pitch = (float) (random.nextGaussian() * _sigma);
                yaw = (float) (random.nextGaussian() * _sigma);
            }
            //rotate method consider constraints
            if(_chain.get(0).graph() == null)
                x_i1.get(i).rotate(new Quaternion(roll, pitch, yaw));
            else if(_chain.get(0).graph().is3D())
                x_i1.get(i).rotate(new Quaternion(roll, pitch, yaw));
            else
                x_i1.get(i).rotate(0,0,1, roll);
        }

        double d1 = _distanceToTarget(x_i1), d2 = _distanceToTarget(_x_i);
        if(d1 < d2) {
            _x_i = x_i1;
            d1 = d2;
        }
        return d1 < minDistance;
    }


    public Frame head() {
        return _chain.get(0);
    }

    public Frame endEffector() {
        return _chain.get(_chain.size() - 1);
    }

    @Override
    protected void _update() {
        for(int i = 0; i < _chain.size(); i++){
            _chain.get(i).setRotation(_x_i.get(i).rotation().get());
        }
    }

    @Override
    protected boolean _changed() {
        if (_target == null) {
            _previousTarget = null;
            return false;
        } else if (_previousTarget == null) {
            return true;
        }
        return !(_previousTarget.position().matches(_target.position()) && _previousTarget.orientation().matches(_target.orientation()));
    }

    @Override
    protected void _reset() {
        _x_i = _copy(_chain);
        _previousTarget = _target == null ? null : new Frame(_target.position().get(), _target.orientation().get(), 1);
        iterations = 0;
    }

    @Override
    public float error() {
        return Vector.distance(_target.position(), _chain.get(_chain.size()-1).position());
    }
}
