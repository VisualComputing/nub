package nub.ik.solver.trik.implementations;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.heuristic.BackwardHeuristic;
import nub.ik.solver.trik.heuristic.ForwardHeuristic;
import nub.ik.solver.trik.heuristic.Heuristic;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.List;

public class BackwardTRIK extends Solver {
    protected Context _context;
    protected Heuristic _backwardHeuristic;

    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;
    protected int _stepCounter;


    public BackwardTRIK(List<? extends Node> chain) {
        this(chain, null);
    }

    public BackwardTRIK(List<? extends Node> chain, Node target) {
        super();
        this._context = new Context(chain, target);
        this._backwardHeuristic = new BackwardHeuristic(_context);
    }

    protected boolean _iterateStepByStep(){
        System.out.println("On step " + _stepCounter);
        if(_stepCounter == 0) {
            if (_context.target() == null) return true; //As no target is specified there is no need to solve IK
            _current = 10e10f; //Keep the current error
            _backwardHeuristic.prepare();
        } else if(_stepCounter < _context.chain().size() - 1){
            int i = _context.last() - _stepCounter;
            _backwardHeuristic.applyActions(i);
        } else{
            _current = _error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), _context.weightRatio(), 1);
            _update();
            _stepCounter = -1;
        }
        _stepCounter++;
        return  false;

    }

    @Override
    protected boolean _iterate() {
        if (_context.target() == null) return true;
        if(_context.singleStep()) return _iterateStepByStep();

        _current = 10e10f; //Keep the current error
        _backwardHeuristic.prepare();
        for(int i = _context.chain().size() - 2; i >= 0; i--) {
            _backwardHeuristic.applyActions(i);
        }
        //Obtain current error
        if(_context.debug()) System.out.println("Current error: ");
        //measure the error depending on position error
        _current = _error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), _context.weightRatio(), 1);
        if(_context.debug()) System.out.println("Current :" + _current + "Best error: " + _best);
        _update(); //update if required
        if(_positionError(_context.chainInformation().get(_context.last()).positionCache(), _context.worldTarget().position()) < _maxError &&
                _orientationError(_context.chainInformation().get(_context.last()).orientationCache(), _context.worldTarget().orientation(), true) < 1) {
            return true;
        }
        return  false;
    }

    @Override
    protected void _update() {
        if(_context.singleStep()) System.out.println("Current : " + _current + " best " + _best);
        if (_current < _best) {
            for (int i = 0; i < _context.chain().size(); i++) {
                _context.chain().get(i).setRotation(_context.usableChain().get(i).rotation().get());
            }
            NodeInformation._copyCache(_context.usableChainInformation(), _context.chainInformation());
            _best = _current;
        }
    }

    @Override
    protected boolean _changed() {
        if (_context.target() == null) {
            _context.setPreviousTarget(null);
            return false;
        } else if (_context.previousTarget() == null) {
            return true;
        }
        return !(_context.previousTarget().position().matches(_context.target().position()) && _context.previousTarget().orientation().matches(_context.target().orientation()));
    }

    @Override
    protected void _reset() {
        _context.setPreviousTarget(_context.target() == null ? null : Node.detach(_context.target().position().get(), _context.target().orientation().get(), 1));
        //Copy original state into chain
        _context.copyChainState(_context.chainInformation(), _context.usableChainInformation());
        //Update cache
        NodeInformation._updateCache(_context.chainInformation());
        NodeInformation._copyCache(_context.chainInformation(), _context.usableChainInformation());

        _iterations = 0;
        //Set values of worldTarget and worldEndEffector
        _context.worldTarget().setRotation( _context.target().orientation().get());
        _context.worldTarget().setPosition(_context.target().position().get());

        if (_context.target() != null) {
            _best = _error(_context.chainInformation().get(_context.last()), _context.target());
        } else {
            _best = 10e10f;
        }

        //find maxLength
        float maxLength = 0;
        for(int i = 0; i < _context.chain().size() - 1; i++){
            maxLength += Vector.distance(_context.chainInformation().get(i).positionCache(), _context.chainInformation().get(i + 1).positionCache());
        }
        _context.setMaxLength(maxLength);
        _context.setAvgLength(maxLength / _context.chain().size());
        if(_context.singleStep()) _stepCounter = 0;
    }

    public float positionError(){
        return _positionError(_context.chain().get(_context.last()).position(), _context.target().position());
    }

    protected float _positionError(Vector eff, Vector target){
        return Vector.distance(eff,target);
    }

    public float orientationError(){
        return _orientationError(_context.chain().get(_context.last()).orientation(), _context.target().orientation(), true);
    }

    protected float _orientationError(Quaternion eff, Quaternion target, boolean degrees){
        float dot = (float) Math.pow(Quaternion.dot(eff, target), 2);
        if(degrees) return (float) Math.toDegrees(Math.acos(2 * dot - 1));
        return (1 - dot);
    }

    protected float _error(Vector effPosition, Vector targetPosition, Quaternion effRotation, Quaternion targetRotation){
        float error = _positionError(effPosition, targetPosition);
        if(_context.direction()){
            //float length = Vector.distance(chain.get(chain.size() - 1).position(), chain.get(0).position());
            float w1 = _context.weightRatio();
            error = error / _context.avgLength();
            error *= error;
            //Add orientation error
            float orientationError = _orientationError(effRotation, targetRotation, false);
            //orientationError *= orientationError / 0.05f;
            if(_context.debug()) System.out.println("error " + error + " ori" + orientationError);
            //error is the weighted sum
            error = w1 * error +  orientationError;
        }
        return error;
    }

    protected float _error(NodeInformation eff, Node target, float w1, float w2){
        return _error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation(), w1, w2);
    }

    protected float _error(NodeInformation eff, Node target){
        return _error(eff.positionCache(), target.position(), eff.orientationCache(), target.orientation());
    }

    protected float _error(Vector effPosition, Vector targetPosition, Quaternion effRotation, Quaternion targetRotation, float w1, float w2){
        float error = _positionError(effPosition, targetPosition);
        if(_context.direction()){
            //float length = Vector.distance(chain.get(chain.size() - 1).position(), chain.get(0).position());
            error = error / _context.avgLength();
            error *= error;
            //Add orientation error
            float orientationError = _orientationError(effRotation, targetRotation, false);
            //orientationError *= orientationError / 0.05f;
            if(_context.debug()) System.out.println("error " + error + " ori" + orientationError);
            //error is the weighted sum
            error = w1 * error +  w2 * orientationError;
        }
        return error;
    }


    @Override
    public float error() {
        return _error(_context.chainInformation().get(_context.last()), _context.worldTarget());
    }

    @Override
    public void setTarget(Node endEffector, Node target) {
        _context.setTarget(target);
    }
}
