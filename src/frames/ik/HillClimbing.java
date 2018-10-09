package frames.ik;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sebchaparr on 8/10/18.
 */
public class HillClimbing extends Solver{

    protected Random random = new Random();
    protected Frame _target;
    protected Frame _previousTarget;
    protected List<? extends Frame> _chain, _x_i;
    protected boolean _powerLaw;
    protected double _sigma;

    public HillClimbing(boolean powerLaw, double sigma, List<? extends Frame> chain){
        this._powerLaw = powerLaw;
        this._sigma = sigma;
        this._chain = chain;
    }

    public List<? extends Frame> execute(List<? extends Frame> initial, double sigma, boolean powerLaw){
        int k = 0;
        ArrayList<Frame> x_i = _copy(initial);
        while(k < iterations){
            _iterate();
            k++;
        }
        return x_i;
    }

    protected double _distanceToTarget(List<? extends Frame> chain){
        return Vector.distance(chain.get(chain.size()-1).position(), _target.position());
    }

    protected double _powerLawGenerator(double x, double alpha){
        double coarse_alpha = 1.0/(1.0-alpha);
        return Math.pow(1.0 - x, coarse_alpha);
    }

    protected ArrayList<Frame> _copy(List<? extends Frame> chain) {
        ArrayList<Frame> copy = new ArrayList<Frame>();
        Frame reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Frame(reference.position().get(), reference.orientation().get());
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
                roll = (float) (invert * _powerLawGenerator(random.nextDouble(), 2)*_sigma);
                pitch = (float) (invert * _powerLawGenerator(random.nextDouble(), 2)*_sigma);
                yaw = (float) (invert * _powerLawGenerator(random.nextDouble(), 2)*_sigma);
            }else{
                roll = (float) (random.nextGaussian() * _sigma);
                pitch = (float) (random.nextGaussian() * _sigma);
                yaw = (float) (random.nextGaussian() * _sigma);
            }
            //rotate method consider constraints
            x_i1.get(i).rotate(new Quaternion(roll, pitch, yaw));
        }

        double d1 = _distanceToTarget(x_i1), d2 = _distanceToTarget(x_i1);
        if(d1 < d2) {
            _x_i = x_i1;
            d1 = d2;
        }
        return d1 < minDistance;
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
        _previousTarget = _target == null ? null : new Frame(_target.position().get(), _target.orientation().get());
        iterations = 0;
    }
}
