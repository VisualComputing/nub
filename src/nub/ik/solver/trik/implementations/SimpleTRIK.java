package nub.ik.solver.trik.implementations;

import nub.core.Node;
import nub.ik.solver.Solver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.heuristic.*;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.List;

public class SimpleTRIK extends Solver {

    public enum HeuristicMode{
        CCD, FORWARD, LOOK_AHEAD_FORWARD, BACKWARD, FORWARD_TRIANGULATION, BACKWARD_TRIANGULATION, FORWARD_CCD_DOUBLE_PASS, BACK_AND_FORTH,
        CCD_BACK_AND_FORTH, CCDT_BACK_AND_FORTH, BACK_AND_FORTH_T, FINAL, EXPRESSIVE_FINAL;
        ;
    }

    protected boolean _disable_order_swapping = true; //TODO : REMOVE!
    protected Context _context;
    protected HeuristicMode _heuristicMode;
    protected Heuristic _mainHeuristic, _secondaryHeuristic, _twistHeuristic;

    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;
    protected int _stepCounter;
    protected boolean  _enableTwist = true;
    protected float[] _delegationPerJoint;



    public SimpleTRIK(List<? extends Node> chain, HeuristicMode mode) {
        this(chain, null, mode);
    }

    public SimpleTRIK(List<? extends Node> chain, Node target, HeuristicMode mode) {
        super();
        this._context = new Context(chain, target, false);
        _context.setSolver(this);
        _setHeuristicMode(mode);
        this._twistHeuristic = new TwistHeuristic(_context);
        _enableTwist = false;
        enableSmooth(false);
        _context.setSingleStep(false);
    }

    protected void _setHeuristicMode(HeuristicMode mode){
        switch (mode){
            case FINAL:{
                _mainHeuristic = new FinalHeuristic(_context);
                _context.setTopToBottom(true);
                context().enableDelegation(false);
                _secondaryHeuristic = _mainHeuristic;
                break;
            }
            case EXPRESSIVE_FINAL:{
                _mainHeuristic = new FinalHeuristic(_context);
                _context.setTopToBottom(false);
                context().setDelegationFactor(0.5f);
                _secondaryHeuristic = _mainHeuristic;
                break;
            }

            case CCD:{
                _mainHeuristic = new CCDHeuristic(_context);
                _context.setTopToBottom(false);
                break;
            }

            case CCD_BACK_AND_FORTH:{
                _mainHeuristic = new BackAndForthCCDHeuristic(_context, false, false);
                _context.setTopToBottom(false);
                break;
            }
            case CCDT_BACK_AND_FORTH:{
                _mainHeuristic = new BackAndForthCCDHeuristic(_context, true, true);
                _context.setTopToBottom(true);
                break;
            }

            case LOOK_AHEAD_FORWARD:{
                _mainHeuristic = new LookAheadHeuristic(new ForwardHeuristic(_context));
                _context.setTopToBottom(true);
                break;
            }

            case FORWARD:{
                _mainHeuristic = new ForwardHeuristic(_context);
                _context.setTopToBottom(true);
                break;
            }
            case BACKWARD:{
                _mainHeuristic = new BackwardHeuristic(_context);
                _context.setTopToBottom(false);
                break;
            }
            case FORWARD_TRIANGULATION:{
                _mainHeuristic = new ForwardTriangulation(_context);
                _context.setTopToBottom(true);
                break;
            }
            case BACKWARD_TRIANGULATION:{
                _mainHeuristic = new BackwardTriangulation(_context);
                _context.setTopToBottom(false);
                break;
            }
            case BACK_AND_FORTH:{
                _context.setTopToBottom(true);
                //_mainHeuristic = new ForwardHeuristic(_context);
                //_secondaryHeuristic = new BackwardHeuristic(_context);
                _mainHeuristic = new BackAndForthCCDHeuristic(_context, false, true);
                _secondaryHeuristic = new BackAndForthCCDHeuristic(_context, false, false);
            }
            case BACK_AND_FORTH_T:{
                _context.setTopToBottom(false);
                //_mainHeuristic = new ForwardHeuristic(_context);
                //_secondaryHeuristic = new BackwardHeuristic(_context);
                _mainHeuristic = new BackAndForthCCDHeuristic(_context, true, true);
                _secondaryHeuristic = new BackAndForthCCDHeuristic(_context, true, false);
            }
        }
        _heuristicMode = mode;
    }
    public Context context(){
        return _context;
    }

    public boolean enableTwist(){
        return _enableTwist;
    }

    public void enableTwist(boolean enable){
        _enableTwist = enable;
    }

    public boolean direction(){
        return _context.direction();
    }

    public HeuristicMode mode(){
        return _heuristicMode;
    }

    public boolean enableSmooth(){
        return mainHeuristic().enableSmooth();
    }

    public void enableSmooth(boolean smooth){
        _mainHeuristic.enableSmooth(smooth);
        _twistHeuristic.enableSmooth(smooth);
    }

    protected boolean _iterateStepByStep(){
        System.out.println("On step " + _stepCounter);
        if(_stepCounter == 0) {
            if (_context.target() == null) return true; //As no target is specified there is no need to solve IK
            _current = 10e10f; //Keep the current error
            _mainHeuristic.prepare();
        } else if(_stepCounter < _context.chain().size()){
            int i = context().topToBottom() ? _stepCounter - 1 : _context.last() - _stepCounter;
            _mainHeuristic.applyActions(i);
            if(_enableTwist) _twistHeuristic.applyActions(i);
            if(context().topToBottom()) _context.usableChainInformation().get(i + 1).updateCacheUsingReference();

        } else{
            _current = _error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), _context.weightRatio(), 1);
            _update();
            _stepCounter = -1;

            if(!_disable_order_swapping && (_heuristicMode == HeuristicMode.BACK_AND_FORTH || _heuristicMode == HeuristicMode.FINAL || _heuristicMode == HeuristicMode.EXPRESSIVE_FINAL)){
                context().setTopToBottom(!context().topToBottom());
                Heuristic aux = _mainHeuristic;
                _mainHeuristic = _secondaryHeuristic;
                _secondaryHeuristic = aux;
            }

        }
        _stepCounter++;
        return  false;

    }

    @Override
    protected boolean _iterate() {
        if (_context.target() == null) return true;
        if(_context.singleStep()) return _iterateStepByStep();
        _current = 10e10f; //Keep the current error
        _mainHeuristic.prepare();
        if(context().topToBottom()){
            for(int i = 0; i < _context.chain().size() - 1; i++) {
                _mainHeuristic.applyActions(i);
                if(_enableTwist) _twistHeuristic.applyActions(i);
                //update next joint cache based on current one
                _context.usableChainInformation().get(i + 1).updateCacheUsingReference();
            }
        }else{
            for(int i = _context.chain().size() - 2; i >= 0; i--) {
                _mainHeuristic.applyActions(i);
                if(_enableTwist) _twistHeuristic.applyActions(i);
            }
        }

        if(!_disable_order_swapping && (_heuristicMode == HeuristicMode.BACK_AND_FORTH || _heuristicMode == HeuristicMode.FINAL || _heuristicMode == HeuristicMode.EXPRESSIVE_FINAL)){
            context().setTopToBottom(!context().topToBottom());
            Heuristic aux = _mainHeuristic;
            _mainHeuristic = _secondaryHeuristic;
            _secondaryHeuristic = aux;
        }

        //Obtain current error
        if(_context.debug()) System.out.println("Current error: ");
        //measure the error depending on position error
        _current = _error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), _context.weightRatio(), 1);
        if(_context.debug()) System.out.println("Current :" + _current + "Best error: " + _best);
        _update(); //update if required
        //if(_positionError(_context.chainInformation().get(_context.last()).positionCache(), _context.worldTarget().position()) < _maxError &&
                //_orientationError(_context.chainInformation().get(_context.last()).orientationCache(), _context.worldTarget().orientation(), true) < 1) {
        //if(_positionError(_context.chainInformation().get(_context.last()).positionCache(), _context.worldTarget().position()) <= _maxError){
        if(error() <= _maxError){
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

    public boolean changed(){
        return _changed();
    }

    public void reset(){
        _reset();
    }

    @Override
    protected void _reset() {
        _context.setPreviousTarget(_context.target() == null ? null : new Node(_context.target().position().get(), _context.target().orientation().get(), 1));
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

        if(!_disable_order_swapping && ((_heuristicMode == HeuristicMode.BACK_AND_FORTH || _heuristicMode == HeuristicMode.FINAL || _heuristicMode == HeuristicMode.EXPRESSIVE_FINAL) && context().topToBottom() == false)){
            context().setTopToBottom(true);
            Heuristic aux = _mainHeuristic;
            _mainHeuristic = _secondaryHeuristic;
            _secondaryHeuristic = aux;
        }

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

    public Node target(){
        return _context.target();
    }

    @Override
    public float error() {
        return Vector.distance(_context.chain().get(_context.chain().size() - 1).position(), target().position());
    }

    @Override
    public void setTarget(Node endEffector, Node target) {
        _context.setTarget(target);
    }

    public void setTarget(Node target){
        _context.setTarget(target);
    }

    public Heuristic mainHeuristic(){
        return _mainHeuristic;
    }
}
