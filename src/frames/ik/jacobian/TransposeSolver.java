package frames.ik.jacobian;

import frames.core.Frame;
import frames.ik.Solver;
import frames.primitives.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class TransposeSolver extends Solver{
    //look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/iksurvey.pdf

    protected ArrayList<? extends Frame> _chain;
    protected Frame _target;
    protected Frame _previousTarget;
    protected SimpleMatrix J;
    protected SimpleMatrix delta;
    float _alpha = 0.00001f;

    //protected List<Statistics> _statistics;
    protected boolean _debug = false;

    public TransposeSolver(ArrayList<? extends Frame> chain){
        super();
        this._chain = chain;
    }

    public TransposeSolver(ArrayList<? extends Frame> chain, Frame target) {
        super();
        this._chain = chain;
        this._target = target;
        this._previousTarget =
                target == null ? null : new Frame(target.position().get(), target.orientation().get());
    }

    /*
    public List<Statistics> statistics(){
        return _statistics;
    }
    */

    public ArrayList<? extends Frame> chain() {
        return _chain;
    }

    public Frame target() {
        return _target;
    }

    public void setTarget(Frame target) {
        this._target = target;
    }

    public Frame head() {
        return _chain.get(0);
    }

    public Frame endEffector() {
        return _chain.get(_chain.size() - 1);
    }

    @Override
    protected boolean _iterate() {
        //As no target is specified there is no need to perform an iteration
        if (_target == null || _chain.size() < 2) return true;
        SimpleMatrix error = SimpleMatrix.wrap(
                Util.vectorToMatrix(Vector.subtract(_target.position() , endEffector().position()), head().graph().is3D()));
        J = SimpleMatrix.wrap( Util.jacobian( _chain, endEffector() ) );

        delta = J.transpose().mult(error);

        //choosing alpha according to error magnitude
        SimpleMatrix JJTe = J.mult(delta);
        delta = delta.scale(error.dot(JJTe)/JJTe.dot(JJTe));

        //Execute Until the distance between the end effector and the target is below a threshold
        if (Vector.distance(endEffector().position(), _target.position()) <= super.error) {
            return true;
        }
        //Check total rotation change
        //if (change <= minDistance) return true;
        return false;
    }

    @Override
    protected void _update() {
        Util.updateChain(_chain, delta);
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

