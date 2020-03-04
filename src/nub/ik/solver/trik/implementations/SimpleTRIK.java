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

    protected boolean _disable_order_swapping = false; //TODO : REMOVE!
    protected Context _context;
    protected HeuristicMode _heuristicMode;
    protected Heuristic _mainHeuristic, _secondaryHeuristic, _twistHeuristic;

    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f, _previous = 10e10f;
    protected int _stepCounter;
    protected boolean  _enableTwist = true;


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
                _disable_order_swapping = true;
                _mainHeuristic = new FinalHeuristic(_context);
                _context.setTopToBottom(false);
                context().enableDelegation(false);
                ((FinalHeuristic)_mainHeuristic).checkHinge(false);
                _secondaryHeuristic = _mainHeuristic;
                break;
            }
            case EXPRESSIVE_FINAL:{
                _disable_order_swapping = true;
                _mainHeuristic = new FinalHeuristic(_context);
                _context.setTopToBottom(false);
                context().enableDelegation(true);
                context().setDelegationFactor(0.1f);
                ((FinalHeuristic)_mainHeuristic).checkHinge(false);
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
            _current = context().error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), 1, 1);
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
        _current = context().error(_context.usableChainInformation().get(_context.last()), _context.worldTarget(), 1, 1);
        if(_context.debug()) System.out.println("Current :" + _current + "Best error: " + _best);

        _update(); //update if required

        //Define dead lock if eff does not move to a better position
        if(Math.abs(_previous - _current) <= 0.001f){
            //context().incrementDeadlockCounter();
        }
        else {
            context().resetDeadlockCounter();
        }

        if(context().deadlockCounter() == 5){ //apply random perturbation
            for(int i = 0; i < _context.usableChainInformation().size() - 1; i++){
                NodeInformation j_i = _context.usableChainInformation().get(i);
                Quaternion q = Quaternion.random();
                if(j_i.node().constraint() != null) j_i.node().constraint().constrainRotation(q, j_i.node());
                j_i.node().rotate(q);
            }
            NodeInformation._updateCache(_context.usableChainInformation());
            context().resetDeadlockCounter();
            _current = 10e10f;
        }


        //if(_positionError(_context.chainInformation().get(_context.last()).positionCache(), _context.worldTarget().position()) < _maxError &&
                //_orientationError(_context.chainInformation().get(_context.last()).orientationCache(), _context.worldTarget().orientation(), true) < 1) {
        //if(_positionError(_context.chainInformation().get(_context.last()).positionCache(), _context.worldTarget().position()) <= _maxError){

        _previous = _current;

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
                _context.chain().get(i).rotation()._quaternion[0] = _context.usableChain().get(i).rotation()._quaternion[0];
                _context.chain().get(i).rotation()._quaternion[1] = _context.usableChain().get(i).rotation()._quaternion[1];
                _context.chain().get(i).rotation()._quaternion[2] = _context.usableChain().get(i).rotation()._quaternion[2];
                _context.chain().get(i).rotation()._quaternion[3] = _context.usableChain().get(i).rotation()._quaternion[3];
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

    public float bestError(){
        return _best;
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

        _context.update();

        if (_context.target() != null) {
            _best = context().error(_context.chainInformation().get(_context.last()), _context.target());
        } else {
            _best = 10e10f;
        }
        _previous = 10e10f;

        if(_context.singleStep()) _stepCounter = 0;

        if(!_disable_order_swapping && ((_heuristicMode == HeuristicMode.BACK_AND_FORTH || _heuristicMode == HeuristicMode.FINAL || _heuristicMode == HeuristicMode.EXPRESSIVE_FINAL) && context().topToBottom() == false)){
            context().setTopToBottom(true);
            Heuristic aux = _mainHeuristic;
            _mainHeuristic = _secondaryHeuristic;
            _secondaryHeuristic = aux;
        }
        context().resetDeadlockCounter();
    }

    public float positionError(){
        return _context.positionError(_context.chain().get(_context.last()).position(), _context.target().position());
    }

    public float orientationError(){
        return _context.orientationError(_context.chain().get(_context.last()).orientation(), _context.target().orientation(), true);
    }

    public Node target(){
        return _context.target();
    }

    @Override
    public float error() {
        return context().error(_context.chain().get(_context.last()).position(), _context.worldTarget().position(),
                _context.chain().get(_context.last()).orientation(), _context.worldTarget().orientation(), 1, 1);
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
