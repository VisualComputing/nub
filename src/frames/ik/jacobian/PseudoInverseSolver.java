package frames.ik.jacobian;

import frames.core.Frame;
import frames.ik.Solver;
import frames.primitives.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class PseudoInverseSolver extends Solver{
    //look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/iksurvey.pdf

    protected ArrayList<? extends Frame> _chain;
    protected Frame _target;
    protected Frame _previousTarget;
    protected SimpleMatrix J;
    protected SimpleMatrix delta;
    protected float d_max;



    //protected List<Statistics> _statistics;
    protected boolean _debug = false;

    public PseudoInverseSolver(ArrayList<? extends Frame> chain){
        super();
        this._chain = chain;
        for(Frame f : _chain){
            float d = Vector.distance(f.position(), f.reference() != null ? f.reference().position() : new Vector(0,0,0));
            d_max = d_max < d ? d : d_max;
        }
    }

    public PseudoInverseSolver(ArrayList<? extends Frame> chain, Frame target) {
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
                Util.vectorToMatrix(Util.clampMagnitude(Vector.subtract(_target.position() , endEffector().position()), d_max/2.f), head().graph().is3D()));
        J = SimpleMatrix.wrap( Util.jacobian( _chain, endEffector() ) );
        System.out.println("J: " + J);

        delta = SimpleMatrix.wrap(Util.solvePseudoinverse(J.getDDRM(), error.getDDRM()));
        delta.scale(0.1f);
        System.out.println("delta: " + delta.transpose());

        System.out.println("cross: " + Vector.cross(new Vector(1,0,0), new Vector(0,0,1), null));


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

