package nub.ik.solver.geometric.oldtrik;

import javafx.util.Pair;
import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.visualization.InterestingEvent;
import nub.ik.visualization.VisualizerMediator;
import nub.ik.solver.Solver;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.*;

public class TRIK extends Solver {
    protected List<? extends Node> _original;
    protected List<Node> _auxiliary_chain, _chain;
    protected List<NodeInformation> _original_info, _auxiliary_chain_info, _chain_info; //Keep position / orientation information
    protected Node _target, _worldTarget;
    protected Node _previousTarget;

    protected boolean _direction = false;
    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;

    protected boolean _enableWeight, _explore; //TODO : Refine
    protected int _lockTimes = 0, _lockTimesCriteria = 4;


    //Error attributes TODO: Refine
    protected boolean  _enableTwist = true;
    protected boolean _smooth = false; //smooth tries to reduce the movement done by each joint, such that the distance from initial one is reduced
    protected float _smoothAngle = (float) Math.toRadians(10);
    protected float _maxLength = 0;

    public void smooth(boolean value){
        _smooth = value;
    }


    public static boolean _debug = false; //TODO : REMOVE!
    protected Random _random = new Random(0); //TODO : REMOVE!
    protected float _weightRatio = 3f, _weightRatioNear = 1.2f; //how many times is position more important than orientation
    protected float _weightThreshold = 0.1f; //change error measurement when the chain is near the target

    protected int _last = -1;

    public void enableTwistHeuristics(boolean enable){
        _enableTwist = enable;
    }

    public boolean direction(){
        return _direction;
    }

    public void enableDirection(boolean direction){
        _direction = direction;
    }

    public void enableWeight(boolean enable){
        _enableWeight = enable;
    }

    public TRIK(List<? extends Node> chain) {
        this(chain, null);
    }

    public TRIK(List<? extends Node> chain, Node target) {
        super();
        this._original = chain;
        if(_debug && Graph.isReachable(_original.get(0))) {
            this._chain = Context._attachedCopy(chain, null);
            this._auxiliary_chain = Context._attachedCopy(chain, null, false);
        }
        else {
            this._chain = Context._detachedCopy(chain);
            this._auxiliary_chain = Context._detachedCopy(chain, false);
        }

        //create info list
        _original_info = NodeInformation._createInformationList(_original, true);
        _chain_info = NodeInformation._createInformationList(_chain, true);
        _auxiliary_chain_info = NodeInformation._createInformationList(_auxiliary_chain, true);

        this._target = target;
        this._previousTarget =
                target == null ? null : Node.detach(target.position().get(), target.orientation().get(), 1);

        this._worldTarget = target == null ? Node.detach(new Vector(), new Quaternion(), 1) : Node.detach(_target.position(), _target.orientation(), 1);
        this._last = _chain.size() - 1;
    }

    //TODO : remove this
    public static boolean _singleStep = false;
    public int _stepCounter = 0;

    protected boolean _iterateStepByStep() {
        int last = _auxiliary_chain_info.size() - 1;
        System.out.println("On step " + _stepCounter);
        if(_stepCounter == 0) {
            if (_target == null) return true; //As no target is specified there is no need to solve IK
            _current = 10e10f; //Keep the current error
            //Step 1. make a deep copy of _chain state into _auxiliary_chain
            //_copyChainState(_original, _chain);
            _copyChainState(_chain, _auxiliary_chain);
        } else if(_stepCounter == 1) {
            //Step 2. Translate the auxiliary chain to the target position
            //2.1 Update auxiliary root and eff cache
            _auxiliary_chain_info.get(0).setCache(_chain_info.get(0).positionCache(), _chain_info.get(0).orientationCache());
            _auxiliary_chain_info.get(last).setCache(_chain_info.get(last).positionCache(), _chain_info.get(last).orientationCache());
            //2.2 Do alignment
            _alignToTarget(_auxiliary_chain_info.get(0), _auxiliary_chain_info.get(last), _worldTarget);
            //2.3 Update auxiliary cache
            NodeInformation._updateCache(_auxiliary_chain_info);
        } else if(_stepCounter - 1 < _chain.size()){
            //for(int i = 1; i < _chain.size(); i++) {
            int i = _stepCounter - 1;
            if(_lookAhead > 0 && i < _chain.size() - 1){
                //Apply the best sequence
                List<Quaternion> rotations = _lookAhead(_chain_info, _auxiliary_chain_info, _worldTarget, _direction, _enableWeight, _explore, _enableTwist, _smooth, _smoothAngle, mode,i - 1, Math.min(_lookAhead, _chain.size() - i), 0, null, new ArrayList<>(),0);
                for(int k = 0; k < rotations.size(); k++){
                    Quaternion delta = rotations.get(k);
                    _chain_info.get(i - 1 + k).rotateAndUpdateCache(delta, false, _chain_info.get(last));
                    _chain_info.get(i + k).updateCacheUsingReference();
                }
            } else {
                applySwingTwist(_chain_info.get(i - 1), _chain_info.get(i), _auxiliary_chain_info.get(i), _chain_info.get(last), _worldTarget, (float) Math.toRadians(15), (float) Math.toRadians(15), _enableWeight, _direction, _enableTwist, _smooth, _smoothAngle);
                //update next joint cache based on current one
                _chain_info.get(i).updateCacheUsingReference();
            }
        } else{
            _current = _error(_chain_info.get(last), _worldTarget, _weightRatio, 1);
            _update();
            _stepCounter = -1;
        }
        _stepCounter++;
        return  false;
    }


    @Override
    protected boolean _iterate() {
        if(_singleStep) return _iterateStepByStep();
        //As no target is specified there is no need to solve IK
        if (_target == null) return true;
        int last = _auxiliary_chain_info.size() - 1;
        _explore = false; //Explore only when required
        //in case the error is not converging
        if(_lockTimes > _lockTimesCriteria) {
            //enable exploration
            //System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<Locked!!! " + _iterations);
            _explore = true;
            _lockTimes = 0;
        }

        _current = 10e10f; //Keep the current error
        //Step 1. make a deep copy of _chain state into _auxiliary_chain
        _copyChainState(_chain, _auxiliary_chain); //TODO change order of this and next
        //Step 2. Translate the auxiliary chain to the target position
        //2.1 Update auxiliary root and eff cache
        _auxiliary_chain_info.get(0).setCache(_chain_info.get(0).positionCache(), _chain_info.get(0).orientationCache());
        _auxiliary_chain_info.get(last).setCache(_chain_info.get(last).positionCache(), _chain_info.get(last).orientationCache());
        //2.2 Do alignment
        _alignToTarget(_auxiliary_chain_info.get(0), _auxiliary_chain_info.get(last), _worldTarget);
        //2.3 Update auxiliary cache
        NodeInformation._updateCache(_auxiliary_chain_info);
        //Step 3. Choose best local action
        float av = 0;
        for(int i = 1; i < _chain.size(); i++) {
            _action_counter = 0;
            if(_lookAhead > 0 && i < _chain.size() - 1){
                int la = _lookAhead;
                boolean explore = _explore;
                boolean smooth = _smooth;
                if(i == 1){
                    //la = Math.max(_chain.size(), _lookAhead); //Do more exploration on first action
                    //smooth = true;
                    explore = true;
                }

                List<Quaternion> rotations = _lookAhead(_chain_info, _auxiliary_chain_info, _worldTarget, _direction, _enableWeight, explore, _enableTwist, _smooth, _smoothAngle, mode,i - 1, Math.min(_lookAhead, _chain.size() - i - 1), 0, null, new ArrayList<>(),0);
                for(int k = 0; k < rotations.size(); k++){
                    Quaternion delta = rotations.get(k);
                    _chain_info.get(i - 1 + k).rotateAndUpdateCache(delta, false, _chain_info.get(last));
                    if(i + k < _chain.size() - 1)_chain_info.get(i + k).updateCacheUsingReference();
                }
            } else {
                //TODO: Use Twisting only when truly required
                applySwingTwist(_chain_info.get(i - 1), _chain_info.get(i), _auxiliary_chain_info.get(i), _chain_info.get(_chain.size() - 1), _worldTarget, (float) Math.toRadians(15), (float) Math.toRadians(15), _enableWeight, _direction, _enableTwist, _smooth, _smoothAngle);
                //update next joint cache based on current one
                _chain_info.get(i).updateCacheUsingReference();
                _action_counter += 1;
            }
            av += _action_counter;
        }
        av /= (_chain.size() - 1);
        _average_actions += av/_maxIterations;
        //_average_actions /= _maxIterations;
        //Obtain current error
        if(_debug) System.out.println("Current error: ");
        //measure the error depending on position error
        _current = _error(_chain_info.get(last), _worldTarget, _weightRatio, 1);
        if(_debug) System.out.println("Current :" + _current + "Best error: " + _best);

        if(_current >= _best){
            _lockTimes++;
        }

        _update(); //update if required

        if(_enableMediator){
            InterestingEvent event = mediator().addEventStartingAfterLast("UPDATE STRUCTURE", "UpdateStructure", 1, 1);
            int i = 0;
            Vector[] translations = new Vector[_chain.size()];
            Quaternion[] rotations = new Quaternion[_chain.size()];
            for (Node node : _chain) {
                translations[i] = node.translation().get();
                rotations[i++] = node.rotation().get();
            }
            event.addAttribute("structure", _chain);
            event.addAttribute("rotations", rotations);
            event.addAttribute("translations", translations);
            InterestingEvent messageEvent = mediator().addEventStartingWithLast("UPDATE MESSAGE", "Message", 0, 1);
            //Add the convenient attributes
            messageEvent.addAttribute("message", "Updating chain");
        }
        //Check total change
        //if (Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position()) <= _minDistance) return true;
        if(_positionError(_original_info.get(last).positionCache(), _worldTarget.position()) < _maxError && _orientationError(_original_info.get(last).orientationCache(), _worldTarget.orientation(), true) < 1) return true;
        return  false;
    }

    @Override
    protected void _update() {
        if(_singleStep) System.out.println("Current : " + _current + " best " + _best);
        if (_current < _best) {
            for (int i = 0; i < _original.size(); i++) {
                _original.get(i).setRotation(_chain.get(i).rotation().get());
            }
            NodeInformation._copyCache(_chain_info, _original_info);
            _best = _current;
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
/*
        for(GTarget gTarget : _gTargets){
            if(gTarget._prev == null){
                return true;
            }
            if(!(gTarget._node.position().matches(gTarget._prev))){
                return  true;
            }
        }
*/
        return !(_previousTarget.position().matches(_target.position()) && _previousTarget.orientation().matches(_target.orientation()));
    }

    @Override
    protected void _reset() {
        _average_actions = 0;
        if(_enableMediator) {
            InterestingEvent event1 = mediator().addEventStartingAfterLast("TRANSLATE TARGET", "NodeTranslation", 1, 1);
            event1.addAttribute("node", _target); //Add the convenient attributes
            event1.addAttribute("translation", Vector.subtract(_target.position(), _previousTarget != null ? _previousTarget.position() : new Vector()));
            event1.addAttribute("useGlobalCoordinates", true);

            InterestingEvent event2 = mediator().addEventStartingWithLast("ROTATE TARGET", "NodeRotation", 1, 1); //Create the event
            event2.addAttribute("node", _target); //Add the convenient attributes
            event2.addAttribute("rotation", Quaternion.compose(_previousTarget != null ? _previousTarget.rotation().inverse() : new Quaternion(), _target.rotation()));

            InterestingEvent message = mediator().addEventStartingWithLast("TRANSLATE MESSAGE", "Message", 0, 1);
            message.addAttribute("message", "Update target"); //Add the convenient attributes
        }


        if(_enableMediator){
            InterestingEvent event = mediator().addEventStartingAfterLast("UPDATE STRUCTURE", "UpdateStructure", 1, 1);
            int i = 0;
            Vector[] translations = new Vector[_chain.size()];
            Quaternion[] rotations = new Quaternion[_chain.size()];
            for (Node node : _chain) {
                translations[i] = node.translation().get();
                rotations[i++] = node.rotation().get();
            }
            event.addAttribute("structure", _chain);
            event.addAttribute("rotations", rotations);
            event.addAttribute("translations", translations);
            InterestingEvent messageEvent = mediator().addEventStartingWithLast("UPDATE MESSAGE", "Message", 0, 1);
            //Add the convenient attributes
            messageEvent.addAttribute("message", "Updating chain");
        }
        _previousTarget = _target == null ? null : Node.detach(_target.position().get(), _target.orientation().get(), 1);
        //Copy original state into chain
        _copyChainState(_original, _chain);
        //Update cache
        NodeInformation._updateCache(_original_info);
        NodeInformation._copyCache(_original_info, _chain_info);


        _iterations = 0;
        //Set values of worldTarget and worldEndEffector
        _worldTarget.setRotation( _target.orientation().get());
        _worldTarget.setPosition(_target.position().get());

        if (_target != null) {
            if(_debug)System.out.println("On Reset ------------");
            _best = _error(_original_info.get(_original_info.size() - 1), _target);
            if(_debug)System.out.println("          best " + _best);

        } else {
            _best = 10e10f;
        }
        _lockTimes = 0;
        _explore = false;

        //find maxLength
        _maxLength = 0;
        for(int i = 0; i < _original.size() - 1; i++){
            _maxLength += Vector.distance(_original_info.get(i).positionCache(), _original_info.get(i + 1).positionCache());
        }
        _avg_bone_length = _maxLength / _original.size();


        if(_singleStep) _stepCounter = 0;

    }

    public float positionError(){
        return _positionError(_original.get(_original.size() - 1).position(), _target.position());
    }

    protected float _positionError(Vector eff, Vector target){
        return Vector.distance(eff,target);
    }

    public float orientationError(){
        return _orientationError(_original.get(_original.size() - 1).orientation(), _target.orientation(), true);
    }

    protected float _orientationError(Quaternion eff, Quaternion target, boolean degrees){
        float dot = (float) Math.pow(Quaternion.dot(eff, target), 2);
        if(degrees) return (float) Math.toDegrees(Math.acos(2 * dot - 1));
        return (1 - dot);
    }

    protected float _error(Vector effPosition, Vector targetPosition, Quaternion effRotation, Quaternion targetRotation){
        float error = _positionError(effPosition, targetPosition);
        if(_direction){
            //float length = Vector.distance(chain.get(chain.size() - 1).position(), chain.get(0).position());
            float w1 = _weightRatio;
            error = error / _avg_bone_length;
            error *= error;

            //Add orientation error
            float orientationError = _orientationError(effRotation, targetRotation, false);
            //orientationError *= orientationError / 0.05f;

            if(_debug) System.out.println("error " + error + " ori" + orientationError);
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
        if(_direction){
            //float length = Vector.distance(chain.get(chain.size() - 1).position(), chain.get(0).position());
            error = error / _avg_bone_length;
            error *= error;
            //Add orientation error
            float orientationError = _orientationError(effRotation, targetRotation, false);
            //orientationError *= orientationError / 0.05f;
            if(_debug) System.out.println("error " + error + " ori" + orientationError);
            //error is the weighted sum
            error = w1 * error +  w2 * orientationError;
        }
        return error;
    }

    @Override
    public float error() {
        return _error(_original_info.get(_original_info.size() - 1), _worldTarget);
    }

    @Override
    public void setTarget(Node endEffector, Node target) {
        this._target = target;
        if(_enableMediator)_mediator.registerStructure(_target);
    }

    public void setTarget(Node target) {
        this._target = target;
        if(_enableMediator)_mediator.registerStructure(_target);
    }

    @Override
    public void registerStructure(VisualizerMediator mediator){
        mediator.registerStructure(_chain);
        mediator.registerStructure(_auxiliary_chain);
        //mediator.registerStructure(_target);
    }

    @Override
    public Iterator<? extends Node> iterator(){
        return _chain.iterator();
    }

    //TODO : remove this methods
    public List<? extends Node> chain(){
        return _original;
    }

    public List<Node> copyChain(){
        return _chain;
    }

    public List<Node> auxiliaryChain(){
        return _auxiliary_chain;
    }

    //TODO : Rename this methods
    public List<NodeInformation> chainInformation(){
        return _chain_info;
    }

    public Node target(){
        return _target;
    }

    public int last(){
        return _last;
    }

    //TODO : Look at this idea further

    /*Look ahead:
    * This is much more expensive since we're trying to look ahead n steps
    * based on what we think are the best actions and based on this advice an additional correction rotation
    * */


    protected int _lookAhead = 0;

    protected List<Quaternion> _findActions(List<NodeInformation> chain, List<NodeInformation> auxiliary_chain, Node target, boolean direction, boolean enableWeight, boolean enableTwist, int i, boolean choose, boolean explore, boolean smooth, float smoothAngle){
        List<Quaternion> actions = new ArrayList<Quaternion>();
        NodeInformation j_i = chain.get(i);
        NodeInformation j_i1 = chain.get(i + 1);
        NodeInformation j_i1_hat = auxiliary_chain.get(i + 1);
        NodeInformation eff = chain.get(chain.size() - 1);
        //Swing twist action
        Quaternion q1 = null;
        if(Math.random() < 0.8){
            //Keep previous state
            Quaternion j_i_rotation = j_i.node().rotation().get();
            Quaternion j_i_orientationCache = j_i.orientationCache().get();
            Vector j_i_positionCache = j_i.positionCache().get();
            Quaternion eff_orientationCache = eff.orientationCache().get();
            Vector eff_positionCache = eff.positionCache().get();
            applySwingTwist(j_i, j_i1, j_i1_hat, eff, target, (float) Math.toRadians(15), (float) Math.toRadians(15), enableWeight, direction, enableTwist, smooth, smoothAngle); //Apply swing twist
            q1 = Quaternion.compose(j_i_rotation.inverse(), j_i.node().rotation());
            q1.normalize();
            //Undo changes
            j_i.node().setRotation(j_i_rotation.get());
            j_i.setCache(j_i_positionCache, j_i_orientationCache);
            eff.setCache(eff_positionCache, eff_orientationCache);

            //Undo changes
            Constraint constraint = j_i.node().constraint();
            j_i.node().setConstraint(null);
            j_i.node().setRotation(j_i_rotation.get());
            j_i.node().setConstraint(constraint);
            j_i.setCache(j_i_positionCache, j_i_orientationCache);
            eff.setCache(eff_positionCache.get(), eff_orientationCache.get());
        } else{
            q1 = _findLocalRotation(j_i1, j_i1_hat, enableWeight); //Apply just swing
            if(smooth) q1 = _clampRotation(q1, smoothAngle);
        }
        actions.add(q1);

        if(direction){
            Quaternion q = _findLocalRotationDirection(j_i, eff, target);
            if(smooth) q = _clampRotation(q1, smoothAngle);
            actions.add(q);
        }

        if(choose){
            Vector basis1 = j_i1.node().translation().orthogonalVector();
            Vector basis2 = Vector.cross(j_i1.node().translation(), basis1, null);

            Quaternion q3 = new Quaternion(basis1, (float) Math.toRadians(Math.random() * 90 - 45));
            Quaternion q4 = new Quaternion(basis2, (float) Math.toRadians(Math.random() * 90 - 45));

            if(smooth){
                q3 = _clampRotation(q3, smoothAngle);
                q4 = _clampRotation(q4, smoothAngle);
            }

            actions.add(q3);
            actions.add(q4);
            actions.add(q3.inverse());
            actions.add(q4.inverse());

            if(enableTwist) {
                Quaternion t1 = _findCCDTwist(j_i, j_i1, eff, target, (float) Math.toRadians(40));
                Quaternion t2 = new Quaternion(j_i1.node().translation(), (float) Math.toRadians(Math.random() * 90 - 45));
                if(smooth){
                    t1 = _clampRotation(t1, smoothAngle);
                    t2 = _clampRotation(t2, smoothAngle);
                }
                actions.add(t1);
                actions.add(t2);
                actions.add(t1.inverse());
                actions.add(t2.inverse());

            }
        }

        //Explore when the chain has reach a dead lock status
        if(explore){
            if(j_i.node().constraint() == null || ! (j_i.node().constraint() instanceof Hinge)) {
                Vector x = new Vector(1, 0, 0);
                Vector y = new Vector(0, 1, 0);
                Vector z = new Vector(0, 0, 1);
                for (int c = 0; c < 0; c++) {
                    if (c % 3 == 0) actions.add(new Quaternion(x, (float) Math.toRadians(Math.random() * 90 - 45)));
                    else if (c % 3 == 1) actions.add(new Quaternion(y, (float) Math.toRadians(Math.random() * 90 - 45)));
                    else actions.add(new Quaternion(z, (float) Math.toRadians(Math.random() * 90 - 45)));
                }
            }
            else{
                Quaternion delta = Quaternion.compose(((Hinge)j_i.node().constraint()).idleRotation(), j_i.node().rotation().inverse());
                delta.compose(((Hinge)j_i.node().constraint()).restRotation());
                Vector axis = delta.multiply(new Vector(0,0,1));

                float max = ((Hinge)j_i.node().constraint()).maxAngle(), min = ((Hinge)j_i.node().constraint()).minAngle();

                for (int c = 0; c < 0; c++) {
                    Quaternion q = null;
                    if (c % 2 == 0) q = new Quaternion(axis, (float) Math.toRadians(Math.random() * Math.min(Math.toDegrees(max), 45)));
                    else q = new Quaternion(axis, (float) -Math.toRadians(Math.random() * Math.min(Math.toDegrees(min), 45)));
                    actions.add(Quaternion.compose(j_i.node().rotation().inverse(), q));
                }
            }

        }
        return actions;
    }

    //TODO: MOVE THIS
    public int _action_counter = 0;
    public float _average_actions = 0;

    enum LookAheadMode{ CHOOSE, CHOOSE_ALL}
    protected LookAheadMode mode = LookAheadMode.CHOOSE;
    //Mode: 0 - Look ahead is used only to apply correction
    //Mode: 1 - First iteration of look ahead choose action
    //Mode: 2 - choose at each step
    protected List<Quaternion> _lookAhead(List<NodeInformation> chain, List<NodeInformation> auxiliary_chain, Node target, boolean direction, boolean enableWeight, boolean explore, boolean enableTwist, boolean smooth, float smoothAngle, LookAheadMode mode, int from, int times, int depth, List<Quaternion> sequence, List<Pair<List<Quaternion>, Float>> actions, float movement){
        if(times < 0) return null;
        int last = chain.size() - 1;
        if(depth == times){
            //calculate error
            float e1;
            if(_debug){
                System.out.println("On joint : " + from);
                System.out.println("Eff position : " + chain.get(last).positionCache() + chain.get(last).node().position());
            }
            float w = _random.nextFloat() * (from + depth) / (chain.size() - 1f) * 0.5f;
            e1 = _error(chain.get(last), target,  w * _weightRatio, 1);
            //float e2 = Vector.distance(chain.get(from + times).position(), auxiliary_chain.get(from + times).position());
            //float e2 = movement;// * (float) (Math.PI / Math.sqrt(_maxLength * Vector.distance(chain.get(chain.size() - 1).position(), target.position()))); //quantity of movement
            float e2 = 0;
            if(_debug) System.out.println("e1 : " + Math.toDegrees(e1) + " e2: " + Math.toDegrees(e2) + "e_tot" +Math.toDegrees(e1 + e2 * 0.1f)  + "mov " + Math.toDegrees(movement));
            Pair<List<Quaternion>, Float> action = new Pair<List<Quaternion>, Float>(sequence, e1);
            actions.add(action);
            return null;
        }

        //System.out.println("    ----> on depth " + depth);
        if(depth == 0 || mode == LookAheadMode.CHOOSE_ALL){ //Visit each action (expand the tree)
            List<Quaternion> rotations = _findActions(chain, auxiliary_chain, target, direction, enableWeight, enableTwist, from + depth, true, depth == 0 && explore, smooth, smoothAngle);
            _action_counter += rotations.size();
            for (Quaternion quaternion : rotations){
                List<Quaternion> seq  = sequence == null ? new ArrayList<>() : new ArrayList<>(sequence);
                //System.out.println("    ----> Rot " + rotation.axis() + rotation.angle());
                NodeInformation j_i = chain.get(from + depth);
                //constraint rotation
                Constraint constraint = j_i.node().constraint();
                Quaternion rotation = constraint == null ? quaternion : j_i.node().constraint().constrainRotation(quaternion, j_i.node());
                seq.add(rotation);
                //Keep previous state
                Quaternion j_i_rotation = j_i.node().rotation().get();
                Quaternion j_i_orientationCache = j_i.orientationCache().get();
                Vector j_i_positionCache = j_i.positionCache().get();
                Quaternion eff_orientationCache = chain.get(last).orientationCache().get();
                Vector eff_positionCache = chain.get(last).positionCache().get();
                j_i.rotateAndUpdateCache(rotation, false, chain.get(last)); //Apply action
                //update next joint cache based on current one
                chain.get(from + depth + 1).updateCacheUsingReference();
                //Apply changes to end effector
                _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, enableTwist, smooth, smoothAngle, mode, from, times, depth + 1, seq, actions, movement + rotation.angle()); //look ahead
                //Undo changes
                j_i.node().setConstraint(null);
                j_i.node().setRotation(j_i_rotation.get());
                j_i.node().setConstraint(constraint);
                j_i.setCache(j_i_positionCache, j_i_orientationCache);
                chain.get(from + depth + 1).updateCacheUsingReference();
                chain.get(last).setCache(eff_positionCache.get(), eff_orientationCache.get());
            }
        } else{
            //Expand only the most promising action
            List<Quaternion> rotations = _findActions(chain, auxiliary_chain, target, direction, enableWeight, enableTwist,from + depth, true, false, smooth, smoothAngle);
            _action_counter += rotations.size();
            Quaternion best = null, prevBest = null;
            float error = 10e10f;
            NodeInformation j_i = chain.get(from + depth);
            //Keep previous state
            Quaternion j_i_rotation = j_i.node().rotation().get();
            Quaternion j_i_orientationCache = j_i.orientationCache().get();
            Vector j_i_positionCache = j_i.positionCache().get();
            Quaternion eff_orientationCache = chain.get(last).orientationCache().get();
            Vector eff_positionCache = chain.get(last).positionCache().get();

            //constraint rotation
            Constraint constraint = j_i.node().constraint();

            for (Quaternion quaternion : rotations) {
                Quaternion rotation = constraint == null ? quaternion : j_i.node().constraint().constrainRotation(quaternion, j_i.node());
                j_i.rotateAndUpdateCache(rotation, false, chain.get(last)); //Apply action
                float e;
                float w = _random.nextFloat() * (from + depth) / (chain.size() - 1f) * 0.5f;
                e = _error(chain.get(last), target, w * _weightRatio, 1);
                if (e < error) {
                    error = e;
                    prevBest = best == null ? rotation : best;
                    best = rotation;
                }
                //Undo changes
                j_i.node().setConstraint(null);
                j_i.node().setRotation(j_i_rotation.get());
                j_i.node().setConstraint(constraint);
                j_i.setCache(j_i_positionCache.get(), j_i_orientationCache.get());
                chain.get(from + depth + 1).updateCacheUsingReference();
                chain.get(last).setCache(eff_positionCache.get(), eff_orientationCache.get());
            }

            List<Quaternion> seq1  = sequence;
            List<Quaternion> seq2 = null, seq3 = null;
            if(from + depth <= Math.ceil(0.2f * chain.size()) || _iterations == 0) {
                seq2  = new ArrayList<>(sequence);
                seq3 = new ArrayList<>(sequence);
            }

            j_i.rotateAndUpdateCache(best, false, chain.get(last)); //Apply action
            chain.get(from + depth + 1).updateCacheUsingReference(); //update next joint cache based on current one
            seq1.add(best);
            _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, enableTwist, smooth, smoothAngle, mode, from, times, depth + 1, seq1, actions, movement + best.angle()); //look ahead
            //Undo changes
            j_i.node().setConstraint(null);
            j_i.node().setRotation(j_i_rotation.get());
            j_i.node().setConstraint(constraint);
            j_i.setCache(j_i_positionCache, j_i_orientationCache);
            chain.get(from + depth + 1).updateCacheUsingReference();
            chain.get(last).setCache(eff_positionCache, eff_orientationCache);

            //Do a kind of beam search when uncertainty is high (we consider that 20% of initial joints have uncertainty)
            if(seq2 != null && 1 < 1){

                j_i.rotateAndUpdateCache(prevBest, false, chain.get(last)); //Apply action
                chain.get(from + depth + 1).updateCacheUsingReference(); //update next joint cache based on current one
                seq2.add(prevBest);
                _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, enableTwist, smooth, smoothAngle, mode, from, times, depth + 1, seq2, actions, movement + best.angle()); //look ahead
                //Undo changes
                j_i.node().setConstraint(null);
                j_i.node().setRotation(j_i_rotation.get());
                j_i.node().setConstraint(constraint);
                j_i.setCache(j_i_positionCache.get(), j_i_orientationCache.get());
                chain.get(from + depth + 1).updateCacheUsingReference();
                chain.get(last).setCache(eff_positionCache.get(), eff_orientationCache.get());

                Quaternion randomQuaternion = rotations.get(_random.nextInt(rotations.size()));
                j_i.rotateAndUpdateCache(randomQuaternion, false, chain.get(last)); //Apply action
                chain.get(from + depth + 1).updateCacheUsingReference(); //update next joint cache based on current one

                seq3.add(randomQuaternion);
                _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, enableTwist, smooth, smoothAngle, mode, from, times, depth + 1, seq3, actions, movement + best.angle()); //look ahead
                //Undo changes
                j_i.node().setConstraint(null);
                j_i.node().setRotation(j_i_rotation.get());
                j_i.node().setConstraint(constraint);
                j_i.setCache(j_i_positionCache.get(), j_i_orientationCache.get());
                chain.get(from + depth + 1).updateCacheUsingReference();
                chain.get(last).setCache(eff_positionCache.get(), eff_orientationCache.get());
            }
        }

        if(depth == 0){
            Pair<List<Quaternion>, Float> best = actions.get(0);
            if(_debug) System.out.println("----Actions---");
            int i = 0, b = 0;
            for(Pair<List<Quaternion>, Float> action : actions){
                if(_debug) System.out.println("Action a " + i++ + "   " + action.getKey().get(0).axis() + action.getKey().get(0).angle() + " err " + Math.toDegrees(action.getValue()));
                //pick the action with the best value
                if(action.getValue() < best.getValue()){
                    best = action;
                    b = i - 1;
                }
            }
            if(_debug){
                System.out.println("BEST action : " + b);
                System.out.println("-----------");
                System.out.println("-----------");
            }
            return best.getKey();
        }
        return null;
    }

    public void setLookAhead(int n){
        _lookAhead = Math.max(n, 0);
    }


    //Secondary goals

    //TODO : Move this!
    //TODO : Fix error measure
    protected float _avg_bone_length;
    protected void _calculateAverageLength(){
        for(int i = 1; i < _original.size(); i++){
            _avg_bone_length += _original.get(i).translation().magnitude() * _original.get(i).magnitude();
        }
        _avg_bone_length /= _original.size();
    }

    protected Quaternion _twistToChangeBoundary(Node j_i, Node j_i1, Node j_i2, Node j_i2_hat, int detail){
        //Find the nearest axis
        Vector twist = j_i1.translation().get().normalize(null);
        Vector axis1 = twist.orthogonalVector();
        Vector axis2 = Vector.cross(twist, axis1, null);
        //Project j_2hat in plane normal to twist
        Vector desired = Vector.projectVectorOnPlane(j_i.location(j_i2_hat),twist);

        Vector proj1 = Vector.projectVectorOnAxis(desired, axis1);
        Vector proj2 = Vector.projectVectorOnAxis(desired, axis2);

        Vector axis = proj1.magnitude() > proj2.magnitude() ? axis1 : axis2;

/*
        System.out.println("axis " + axis);
        System.out.println("axis 1" + axis1);
        System.out.println("axis 2" + axis2);
        System.out.println("proj " + desired);
*/

        return  _twistAngleToSetBoundary(j_i, j_i1, j_i2, axis, detail);
    }

    //This method only make sense when j_i1 has constraints
    protected Quaternion _twistAngleToSetBoundary(Node j_i, Node j_i1, Node j_i2, Vector axis, int detail){
        if(j_i1.constraint() == null){
            return new Quaternion();
        }
        Vector twist = j_i1.translation();
        //System.out.println("  <<<<  tw   " + twist);

        float step = (float) (2 * Math.PI / detail);
        float bestAngle = 0, bestError = 10e10f;


        //axis w.r.t j_i ref
        Node ref = j_i.reference() != null ? j_i.reference() : Node.detach(new Vector(), new Quaternion(), 1);
        axis = ref.displacement(axis, j_i);

        for(float angle = 0; angle < (float)Math.PI; angle += step){
            for(int i = 0; i < 2; i++){
                Quaternion rotation = new Quaternion(twist, angle);

                Quaternion prev = j_i.rotation().get();

                j_i.rotate(rotation); //Apply action
                //System.out.println("ji : " + j_i.rotation().axis() + j_i.rotation().angle());
                //Try to align axis with j_i2
                Vector axis_wrt_j_i1 = j_i1.displacement(axis, ref);
                //System.out.println("  <<<<  axis_wrt_j_i1   " + axis_wrt_j_i1);

                Quaternion q = new Quaternion(j_i2.translation(), axis_wrt_j_i1);
                q = j_i1.constraint().constrainRotation(q, j_i1);
                Vector v = q.rotate(j_i2.translation());
                float dist = Vector.angleBetween(v, axis_wrt_j_i1);
                //System.out.println("------>    v" + v + " ax " + axis_wrt_j_i1 + " dist " + Math.toDegrees(dist));

                if (dist + 0.0001f < bestError) {
                    if(dist < 0.0001f) return new Quaternion(twist, angle);
                    bestAngle = angle;
                    bestError = dist;
                }

                j_i.setRotation(prev.get()); //Undo action

                angle = -angle;
            }
        }
        return new Quaternion(twist, bestAngle);
    }


    /*protected void _fixDeadLock(){
        //System.out.println("Fixing");
        //Step 1. make a deep copy of _chain state into _auxiliary_chain
        _copyChainState(_original, _dead_lock_chain);
        _copyChainState(_dead_lock_chain, _auxiliary_chain);
        //Step 2. Translate the auxiliary chain to the target position
        _alignToTarget(_auxiliary_chain.get(0), _auxiliary_chain.get(_auxiliary_chain.size() - 1), _target);
        //Step 3. Find the first joint whose boundary could be updated
        for(int i = _dead_lock_chain.size() - 3; i >= 0; i--) {
            Node j_i = _dead_lock_chain.get(i);
            Node j_i1 = _dead_lock_chain.get(i + 1);
            Node j_i2 = _dead_lock_chain.get(i + 2);
            Node j_i2_hat = _auxiliary_chain.get(i + 2);
            Quaternion q = _twistToChangeBoundary(j_i, j_i1, j_i2, j_i2_hat, 30);
            //System.out.println("Fix node : " + i + q.axis() + q.angle());
            if(q.angle() > Math.toRadians(10)){
                //System.out.println("Fix node : " + i);
                j_i.rotate(q);
                for(int k = i + 1; k < _auxiliary_chain.size() - 1; k++){
                    _dead_lock_chain.get(k).rotate(_findTwist(_dead_lock_chain.get(k), _dead_lock_chain.get(k + 1), _dead_lock_chain.get(_chain.size() - 1), _target, (float) Math.toRadians(20), (float) Math.toRadians(15), _enableWeight));
                    _dead_lock_chain.get(k).rotate(_findLocalRotation(_dead_lock_chain.get(k + 1), _auxiliary_chain.get(k + 1), _enableWeight));
                }
                //update if error is reduced
                float error = _error(_dead_lock_chain);
                if (_best < error) {
                    _copyChainState(_dead_lock_chain, _chain);
                    for (int c = 0; c < _original.size(); c++) {
                        _original.get(c).setRotation(_dead_lock_chain.get(c).rotation().get());
                    }
                    _best = error;
                }
                return;
            }
        }
    }*/

    //HERE ARE DEFINED THE TWIST/SWING ACTIONS
    protected void _copyChainState(List<? extends Node> origin, List<? extends Node> dest){
        //Copy the content of the origin chain into dest
        Node refDest = dest.get(0).reference();
        if(refDest != null){ //TODO: CHECK THIS!
            Constraint constraint = refDest.constraint();
            refDest.setConstraint(null);
            refDest.set(origin.get(0).reference());
            refDest.setConstraint(constraint);
        }

        for(int i = 0; i < origin.size(); i++) {
            Node node = origin.get(i);
            Constraint constraint = dest.get(i).constraint();
            Quaternion rotation = node.rotation().get();
            Vector translation = node.translation().get();

            dest.get(i).setConstraint(null);
            dest.get(i).setRotation(rotation);
            dest.get(i).setTranslation(translation);
            dest.get(i).setScaling(node.scaling());
            dest.get(i).setConstraint(constraint);
        }

        if(_enableMediator) {
            InterestingEvent event = mediator().addEventStartingAfterLast("RESET AUXILIARY STRUCTURE", "UpdateStructure", 1, 1);
            int i = 0;
            Vector[] translations = new Vector[dest.size()];
            Quaternion[] rotations = new Quaternion[dest.size()];
            for (Node node : dest) {
                translations[i] = node.translation().get();
                rotations[i++] = node.rotation().get();
            }
            event.addAttribute("structure", dest);
            event.addAttribute("rotations", rotations);
            event.addAttribute("translations", translations);
            InterestingEvent messageEvent = mediator().addEventStartingWithLast("RESET MESSAGE", "Message", 0, 1);
            //Add the convenient attributes
            messageEvent.addAttribute("message", "Updating auxiliary");
        }
    }

    protected void _translateToTarget(NodeInformation root, NodeInformation eff, Node target){
        //Find the distance from EFF current position and Target
        Vector diff = Vector.subtract(target.position(), eff.positionCache());
        //express diff w.r.t root reference
        Quaternion ref = Quaternion.compose(root.orientationCache(), root.node().rotation().inverse());
        diff = ref.inverseRotate(diff);
        //Move the root accordingly (disable constraints)
        root.translateAndUpdateCache(diff, false);

        if(_enableMediator) {
            InterestingEvent ev = mediator().addEventStartingAfterLast("TRANSLATE ROOT", "NodeTranslation", 1, 1);
            ev.addAttribute("node", root); //Add the convenient attributes
            ev.addAttribute("translation", diff);
            ev.addAttribute("enableConstraint", false);
            ev.addAttribute("modifyChildren", true);
            ev.addAttribute("useGlobalCoordinates", true);
            InterestingEvent message = mediator().addEventStartingWithLast("TRANSLATE MESSAGE", "Message", 0, 1);
            message.addAttribute("message", "Translate to target"); //Add the convenient attributes
        }
    }

    protected void _alignToTarget(NodeInformation root, NodeInformation eff, Node target){ //root has to be updated
        if(_direction) {
            Quaternion delta = Quaternion.compose(root.orientationCache().inverse(), target.orientation());
            delta.compose(eff.orientationCache().inverse());
            delta.compose(root.orientationCache());
            //clamp delta to a max of 20 degrees
            float angle = delta.angle();
            angle = Math.abs(angle) > Math.toRadians(20) ? (float) (Math.signum(angle) * Math.toRadians(20)) : angle;
            delta = new Quaternion(delta.axis(), angle);
            root.rotateAndUpdateCache(delta, false, eff);
        }
        //Move root such that eff is in the same position as target position
        _translateToTarget(root, eff, target);
    }


    //TODO : REFINE
    protected static float _calculateWeight(float boneLength, float distToDesired){
        float dist = distToDesired - boneLength;
        dist = dist < 0 ? -1f / dist : dist;
        float d_i = dist / boneLength;
        return (float) Math.pow(1.5, -d_i);
    }

    //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUME THAT THEY ARE UPDATE
    protected static Quaternion _findLocalRotation(NodeInformation j_i1, NodeInformation j_i1_hat, boolean enableWeight){
        NodeInformation j_i = j_i1.reference(); //must have a parent
        //Define how much rotate j_i in order to align j_i1 with j_i1_hat
        Vector p = j_i1.node().translation();
        //Find j_i1_hat w.r.t j_i
        Vector q = j_i.locationWithCache(j_i1_hat);
        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);
        float weight = 1;
        if(enableWeight) {
            weight = _calculateWeight(p.magnitude(), q.magnitude());
            if(_singleStep)System.out.println("weight : " + weight);
            delta = new Quaternion(delta.axis(), delta.angle() * weight);
        }
        return delta;
    }

    protected static Quaternion _findLocalRotationDirection(NodeInformation j_i, NodeInformation endEffector, Node target){
        //delta = O_i_inv * O_t * (O_i_inv * O_eff)^-1
        Quaternion delta = j_i.orientationCache().inverse();
        delta.compose(target.orientation());
        delta.compose(endEffector.orientationCache().inverse());
        delta.compose(j_i.orientationCache());
        return delta;
    }

    protected static Quaternion _findTwisting(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, float maxAngle){ //Math.toRadians(15)
        //Find twist that approach EFF to its desired orientation
        //q_delta = O_i_inv * O_t * (O_i_inv * O_eff)^-1
        Quaternion delta = j_i.orientationCache().inverse();
        delta.compose(target.orientation());
        delta.compose(endEffector.orientationCache().inverse());
        delta.compose(j_i.orientationCache());
        //get the twist component of the given quaternion
        Vector tw = j_i1.node().translation(); // w.r.t j_i
        //if delta is too short then avoid this operation
        if (Math.abs(delta.angle()) < Math.toRadians(5)) {
            return new Quaternion(tw, 0);
        }

        Vector rotationAxis = new Vector(delta._quaternion[0], delta._quaternion[1], delta._quaternion[2]);
        rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t j_i
        //Get rotation component on Axis direction
        Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), delta.w()); //w.r.t j_i
        //Quaternion rotationSwing = Quaternion.compose(delta, rotationTwist.inverse()); //w.r.t j_i
        //Clamp twist (Max 15 degrees)
        float rotationTwistAngle = rotationTwist.angle();
        rotationTwistAngle = Math.abs(rotationTwistAngle) > maxAngle ? (Math.signum(rotationTwistAngle) * maxAngle) : rotationTwistAngle;
        rotationTwist = new Quaternion(rotationTwist.axis(), rotationTwistAngle);
        //Apply twist to j_i
        return rotationTwist;
    }

    //Try to approach to target final position by means of twisting
    protected static Quaternion _findCCDTwist(NodeInformation j_i, NodeInformation j_i1, NodeInformation eff, Node target, float maxAngle){ //(float) Math.toRadians(20)
        Vector j_i_to_target = j_i.locationWithCache(target.position());
        Vector j_i_to_eff = j_i.locationWithCache(eff);
        Vector j_i_to_eff_proj, j_i_to_target_proj;
        Vector tw = j_i1.node().translation(); // w.r.t j_i
        //Project the given vectors in the plane given by twist axis
        try {
            j_i_to_target_proj = Vector.projectVectorOnPlane(j_i_to_target, tw);
            j_i_to_eff_proj = Vector.projectVectorOnPlane(j_i_to_eff, tw);
        } catch(Exception e){
            return new Quaternion(tw, 0);
        }

        //Perform this operation only when Projected Vectors have not a despicable length
        if (j_i_to_target_proj.magnitude() < 0.1*j_i_to_target.magnitude() && j_i_to_eff_proj.magnitude() < 0.1*j_i_to_eff.magnitude()) {
            return new Quaternion(tw, 0);
        }

        //Find the angle between projected vectors
        float angle = Vector.angleBetween(j_i_to_eff_proj, j_i_to_target_proj);
        //clamp angle
        angle = Math.min(angle, maxAngle);
        if(Vector.cross(j_i_to_eff_proj, j_i_to_target_proj, null).dot(tw) <0)
            angle *= -1;
        return new Quaternion(tw, angle);
    }


    protected static Quaternion _findTwist(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, float maxT1, float maxT2, boolean direction){
        //Step 3. Apply twisting to help reach desired rotation
        Quaternion t1, t2, twist;
        t1 = t2 = _findCCDTwist(j_i, j_i1, endEffector, target, maxT1);
        if(direction){
            t2 = _findTwisting(j_i, j_i1, endEffector, target, maxT2);
        }
        if(t1.axis().dot(t2.axis()) < 0){
            twist = new Quaternion(t1.axis(), 0.5f * t1.angle() - 0.5f * t2.angle());
        } else{
            twist = new Quaternion(t1.axis(), 0.5f * t1.angle() + 0.5f * t2.angle());
        }
        return twist;
    }

    protected static void applySwingTwist(NodeInformation j_i, NodeInformation j_i1, NodeInformation j_i1_hat, NodeInformation endEffector, Node target, float maxT1, float maxT2, boolean enableWeight, boolean direction, boolean enableTwist, boolean smooth, float smoothAngle){
        Quaternion q1 = _findLocalRotation(j_i1, j_i1_hat, enableWeight);
        if(smooth) q1 = _clampRotation(q1, smoothAngle);
        j_i.rotateAndUpdateCache(q1, true, endEffector); //Apply local rotation
        if(enableTwist) {
            Quaternion q2;
            if(smooth) q2 = _findTwist(j_i, j_i1, endEffector, target, smoothAngle, smoothAngle, direction);
            else q2 = _findTwist(j_i, j_i1, endEffector, target, maxT1, maxT2, direction);
            j_i.rotateAndUpdateCache(q2, true, endEffector); //Apply twist rotation
        }
    }

    protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle){
        float angle = rotation.angle();
        if(Math.abs(angle) > maxAngle ){
            rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
        }
        return rotation;
    }
}