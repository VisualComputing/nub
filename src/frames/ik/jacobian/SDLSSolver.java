package frames.ik.jacobian;

import frames.core.Frame;
import frames.ik.Solver;
import frames.primitives.Vector;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import java.util.ArrayList;

//Look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/SdlsPaper.pdf
public class SDLSSolver extends Solver {
    //look at https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/iksurvey.pdf

    protected ArrayList<? extends Frame> _chain;
    protected Frame _target;
    protected Frame _previousTarget;
    protected SimpleMatrix _J;
    protected Vector[] _axes;
    protected SimpleMatrix _delta;
    protected int _dof; //TODO : Consider EF with ORIENTATIONAL DOF
    protected float _max_d, _max_change = (float) Math.toRadians(45);

    public SDLSSolver(ArrayList<? extends Frame> chain){
        super();
        this._chain = chain;
        for(Frame f : _chain){
            float d = Vector.distance(f.position(), f.reference() != null ? f.reference().position() : new Vector(0,0,0));
            _max_d = _max_d < d ? d : _max_d;
        }
        _axes = new Vector[_chain.size() - 1];
        _dof = chain.get(0).graph().is3D() ? 3 : 2;
    }

    public SDLSSolver(ArrayList<? extends Frame> chain, Frame target) {
        super();
        this._chain = chain;
        this._target = target;
        this._previousTarget =
                target == null ? null : new Frame(target.position().get(), target.orientation().get(), 1);
        _axes = new Vector[_chain.size() - 1];
        _dof = chain.get(0).graph().is3D() ? 3 : 2;
    }

    public ArrayList<? extends Frame> chain() {
        return _chain;
    }

    public Frame target() {
        return _target;
    }

    public void setTarget(Frame endEffector, Frame target) {
        this._target = target;
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

    //Adapted from https://www.math.ucsd.edu/~sbuss/ResearchWeb/ikmethods/
    @Override
    protected boolean _iterate() {
        //As no target is specified there is no need to perform an iteration
        if (_target == null || _chain.size() < 2) return true;
        //Clamp error
        Vector e = Vector.subtract(_target.position() , endEffector().position());
        if(e.magnitude() > _max_d){
            e.normalize();
            e.multiply(_max_d);
        }

        SimpleMatrix error = SimpleMatrix.wrap(
                Util.vectorToMatrix(e, head().graph().is3D()));

        _J = SimpleMatrix.wrap( Util.jacobian( _chain, endEffector() , _target.position(), _axes));
        _delta = SimpleMatrix.wrap(new DMatrixRMaj(new double[_J.numCols()]));

        //Get SVD
        SimpleSVD<SimpleMatrix> svd = _J.svd();
        SimpleMatrix U = svd.getU();
        SimpleMatrix V = svd.getV();
        SimpleMatrix w = svd.getW();

        double p_lj[][] = new double[_J.numRows()/_dof][_J.numCols()];
        for(int i = 0; i < _J.numCols(); i++) {
            for (int k = 0; k < _J.numRows()/_dof; k++) {
                for(int d = 0; d < _dof; d++) {
                    p_lj[k][i] += _J.get(_dof * k + d, i) * _J.get(_dof * k + d, i);
                }
                p_lj[k][i] = Math.sqrt(p_lj[k][i]);
            }
        }

        for(int i = 0; i < _J.numRows(); i++){
            if(Math.abs(w.get(i,i)) < 10e-6){
                continue;
            }
            double N_i = 0, alpha_i = 0;
            double w_inv = 1/w.get(i,i);
            //Get N_i and alpha_i
            for(int k = 0; k < _J.numRows()/_dof; k++) {
                double aux = 0;
                for(int d = 0; d < _dof; d++) {
                    alpha_i += U.get(_dof * k + d,i) * error.get(_dof * k + d, 0);
                    aux += U.get(_dof * k + d,i) * U.get(_dof * k + d,i);
                }
                N_i += Math.sqrt(aux);
            }
            //Calculate M_i
            double max_abs = 0, M_i = 0;
            double scale = w_inv * alpha_i;
            SimpleMatrix delta_i = SimpleMatrix.wrap(new DMatrixRMaj(new double[_J.numCols()]));
            for(int j = 0; j < _J.numCols(); j++){
                double p = 0;
                for(int k = 0; k < _J.numRows()/_dof; k++){
                    p += p_lj[k][j];
                }
                M_i += Math.abs(V.get(j,i)) * p;
                double d_i = V.get(j,i) * scale;
                delta_i.set(j, 0, d_i);
                max_abs = max_abs < Math.abs(d_i)? Math.abs(d_i) : max_abs;
            }
            M_i *= Math.abs(w_inv);
            double gamma_i = _max_change * Math.min(1, N_i / M_i);
            //Clamp max abs
            delta_i = delta_i.scale(gamma_i/(gamma_i + max_abs));
            _delta = _delta.plus(delta_i);
        }

        double max = 0;
        for(int i = 0; i < _delta.numRows(); i++ ){
            max = Math.abs(_delta.get(i,0)) > max ? Math.abs(_delta.get(i,0)) : max;
        }
        if(max > Math.toRadians(_max_change))_delta = _delta.scale(Math.toRadians(_max_change) / max);

        Util.updateChain(_chain, _delta, _axes);
        //Execute Until the distance between the end effector and the target is below a threshold
        if (Vector.distance(endEffector().position(), _target.position()) <= super.error) {
            return true;
        }
        //Check total rotation change
        //if (change <= minDistance) return true;
        return false;
    }

    //Update must be done at each iteration step (see line 83)
    @Override
    protected void _update() { }

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
        _previousTarget = _target == null ? null : new Frame(_target.position().get(), _target.orientation().get(), 1);
        _axes = new Vector[_chain.size() - 1];
        iterations = 0;
    }

    @Override
    public float error() {
        return Vector.distance(_target.position(), _chain.get(_chain.size()-1).position());
    }
}

