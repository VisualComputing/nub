package nub.ik.solver.geometric;

import javafx.util.Pair;
import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.animation.InterestingEvent;
import nub.ik.animation.VisualizerMediator;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class TRIK extends Solver {
    protected List<? extends Node> _original;
    protected List<Node> _auxiliary_chain, _chain, _dead_lock_chain; //TODO: CHECK REFERENCE NODE
    protected Node _target;
    protected Node _previousTarget;
    protected boolean _direction = false;
    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;

    protected boolean _enableWeight, _explore; //TODO : Refine
    protected int _lockTimes = 0, _lockTimesCriteria = 4;


    public static boolean _debug = false; //TODO : REMOVE!

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
        if(_debug && _original.get(0).graph() instanceof Scene) {
            this._chain = FABRIKSolver._copy(chain, null, (Scene) _original.get(0).graph());
            this._auxiliary_chain = FABRIKSolver._copy(chain, null, (Scene) _original.get(0).graph(), false);
            this._dead_lock_chain = FABRIKSolver._copy(chain);
        }
        else {
            this._chain = FABRIKSolver._copy(chain);
            this._auxiliary_chain = FABRIKSolver._copy(chain, false);
            this._dead_lock_chain = FABRIKSolver._copy(chain);
        }
        this._target = target;
        this._previousTarget =
                target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
    }

    //TODO : remove this
    public static boolean _singleStep = false;
    public int _stepCounter = 0;

    protected boolean _iterateStepByStep() {
        System.out.println("On step " + _stepCounter);
        if(_stepCounter == 0) {
            if (_target == null) return true; //As no target is specified there is no need to solve IK
            _current = 10e10f; //Keep the current error
            //Step 1. make a deep copy of _chain state into _auxiliary_chain
            _copyChainState(_original, _chain);
            _copyChainState(_chain, _auxiliary_chain);
        } else if(_stepCounter == 1) {
            //Step 2. Translate the auxiliary chain to the target position
            _alignToTarget(_auxiliary_chain.get(0), _auxiliary_chain.get(_auxiliary_chain.size() - 1), _target);
        } else if(_stepCounter - 1 < _chain.size()){
            //for(int i = 1; i < _chain.size(); i++) {
            int i = _stepCounter - 1;
            if(_lookAhead > 0 && i < _chain.size() - 1){
                Quaternion delta = _lookAhead(_chain, _auxiliary_chain, _target, _direction, _enableWeight, _explore, mode,i - 1, Math.min(_lookAhead, _chain.size() - i), 0, null, new ArrayList<>());
                _chain.get(i - 1).rotate(delta);
            } else {
                applySwingTwist(_chain.get(i - 1), _chain.get(i), _auxiliary_chain.get(i), _chain.get(_chain.size() - 1), _target, (float) Math.toRadians(15), (float) Math.toRadians(15), _enableWeight, _direction);
            }
        } else{
            _current = _error(_chain);
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
        _explore = false; //Explore only when required

        //in case the error is not converging
        if(_lockTimes > _lockTimesCriteria) {
            //enable exploration
            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<Locked!!! " + _iterations);
            _explore = true;
            _lockTimes = 0;
        }

        _current = 10e10f; //Keep the current error

        //Step 1. make a deep copy of _chain state into _auxiliary_chain
        _copyChainState(_original, _chain);
        _copyChainState(_chain, _auxiliary_chain);
        //Step 2. Translate the auxiliary chain to the target position
        _alignToTarget(_auxiliary_chain.get(0), _auxiliary_chain.get(_auxiliary_chain.size() - 1), _target);
        //Step 3. Choose best local action
        for(int i = 1; i < _chain.size(); i++) {
            if(_lookAhead > 0 && i < _chain.size() - 1){
                Quaternion delta = _lookAhead(_chain, _auxiliary_chain, _target, _direction, _enableWeight, _explore, mode,i - 1, Math.min(_lookAhead, _chain.size() - i), 0, null, new ArrayList<>());
                _chain.get(i - 1).rotate(delta);
            } else {
                applySwingTwist(_chain.get(i - 1), _chain.get(i), _auxiliary_chain.get(i), _chain.get(_chain.size() - 1), _target, (float) Math.toRadians(15), (float) Math.toRadians(15), _enableWeight, _direction);
            }
        }
        //Obtain current error
        _current = _error(_chain);
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
        return  false;
    }

    @Override
    protected void _update() {
        if(_singleStep) System.out.println("Current : " + _current + " best " + _best);
        if (_current < _best) {
            for (int i = 0; i < _original.size(); i++) {
                _original.get(i).setRotation(_chain.get(i).rotation().get());
            }
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
        _previousTarget = _target == null ? null : new Node(_target.position().get(), _target.orientation().get(), 1);
        for(GTarget gTarget : _gTargets){
            gTarget._prev = gTarget._node.position().get();
        }

        _copyChainState(_original, _chain);
        _iterations = 0;

        if (_target != null) {
            _best = _error(_original);
        } else {
            _best = 10e10f;
        }
        _lockTimes = 0;
        _explore = false;

        if(_singleStep) _stepCounter = 0;

    }

    protected float _error(List<? extends Node> chain){
        //TODO : Error must be defined as a weighted average between orientational error and translational error
        float distToTarget = Vector.distance(chain.get(chain.size() - 1).position(), _target.position());
        float distToGTargets = 0;
        for(int i = 0; i < chain.size(); i++){
            //dist of node to gtarget
            for(GTarget gTarget : _gTargets){
                if(gTarget._influence.get(_chain.get(i)) != null) {
                    float dist = Vector.distance(chain.get(i).position(), gTarget._node.position());
                    dist *= gTarget._influence.get(_chain.get(i)); //Low value compared with main error
                    distToGTargets +=1f/dist;
                }
            }
        }
        return distToTarget + distToGTargets;
    }

    @Override
    public float error() {
        return _error(_original);
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
    public List<Node> copyChain(){
        return _chain;
    }

    public List<Node> auxiliaryChain(){
        return _auxiliary_chain;
    }

    //TODO : Look at this idea further

    /*Look ahead:
    * This is much more expensive since we're trying to look ahead n steps
    * based on what we think are the best actions and based on this advice an additional correction rotation
    * */


    protected int _lookAhead = 0;

    protected List<Quaternion> _findActions(List<Node> chain, List<Node> auxiliary_chain, Node target, boolean direction, boolean enableWeight, int i, boolean choose, boolean explore){
        List<Quaternion> actions = new ArrayList<Quaternion>();
        Node j_i = chain.get(i);
        Node j_i1 = chain.get(i + 1);
        Node j_i1_hat = auxiliary_chain.get(i + 1);
        Node eff = chain.get(chain.size() - 1);
        //Swing twist action
        Quaternion original = j_i.rotation().get();
        Quaternion q1 = null;
        if(Math.random() < 0.8){
            applySwingTwist(j_i, j_i1, j_i1_hat, eff, target, (float) Math.toRadians(20), (float) Math.toRadians(20), enableWeight, direction); //Apply swing twist
            q1 = Quaternion.compose(original.inverse(), j_i.rotation());
            j_i.setRotation(original);
        } else{
            q1 = _findLocalRotation(j_i1, j_i1_hat, enableWeight); //Apply just swing
        }
        actions.add(q1);

        if(choose){
            Quaternion q2 = _findCCDTwist(j_i, j_i1, eff, target, (float) Math.toRadians(40));
            Quaternion q3 = new Quaternion(j_i1.translation().orthogonalVector(), (float) Math.toRadians(Math.random() * 90 - 45));
            Quaternion q4 = new Quaternion(Vector.cross(j_i1.translation(), j_i1.translation().orthogonalVector(), null), (float) Math.toRadians(Math.random() * 90 - 45));
            Quaternion q5 = new Quaternion(j_i1.translation(), (float) Math.toRadians(Math.random() * 90 - 45));
            actions.add(q2);
            actions.add(q3);
            actions.add(q4);
            actions.add(q5);
        }

        //Explore when the chain has reach a dead lock status
        if(explore){
            if(j_i.constraint() == null || ! (j_i.constraint() instanceof Hinge)) {
                Vector x = new Vector(1, 0, 0);
                Vector y = new Vector(0, 1, 0);
                Vector z = new Vector(0, 0, 1);
                for (int c = 0; c < 15; c++) {
                    if (c % 3 == 0) actions.add(new Quaternion(x, (float) Math.toRadians(Math.random() * 90 - 45)));
                    else if (c % 3 == 1) actions.add(new Quaternion(y, (float) Math.toRadians(Math.random() * 90 - 45)));
                    else actions.add(new Quaternion(z, (float) Math.toRadians(Math.random() * 90 - 45)));
                }
            }
            else{
                Quaternion delta = Quaternion.compose(((Hinge)j_i.constraint()).idleRotation(), j_i.rotation().inverse());
                delta.compose(((Hinge)j_i.constraint()).restRotation());
                Vector axis = delta.multiply(new Vector(0,0,1));

                float max = ((Hinge)j_i.constraint()).maxAngle(), min = ((Hinge)j_i.constraint()).minAngle();

                for (int c = 0; c < 15; c++) {
                    Quaternion q = null;
                    if (c % 2 == 0) q = new Quaternion(axis, (float) Math.toRadians(Math.random() * Math.min(Math.toDegrees(max), 45)));
                    else q = new Quaternion(axis, (float) -Math.toRadians(Math.random() * Math.min(Math.toDegrees(min), 45)));
                    actions.add(Quaternion.compose(j_i.rotation().inverse(), q));
                }
            }

        }
        return actions;
    }

    //TODO: MOVE THIS
    enum LookAheadMode{ CHOOSE, CHOOSE_ALL}
    protected LookAheadMode mode = LookAheadMode.CHOOSE;
    //Mode: 0 - Look ahead is used only to apply correction
    //Mode: 1 - First iteration of look ahead choose action
    //Mode: 2 - choose at each step
    protected Quaternion _lookAhead(List<Node> chain, List<Node> auxiliary_chain, Node target, boolean direction, boolean enableWeight, boolean explore, LookAheadMode mode, int from, int times, int depth, Quaternion initial, List<Pair<Quaternion, Float>> actions){
        if(times < 0) return null;
        if(depth == times){
            //Given configuration until now try to help the algorithm in a future
            //alpha w.r.t last
            Quaternion alpha = _findLocalRotation(chain.get(from + times),  auxiliary_chain.get(from + times), enableWeight);
            //get alpha w.r.t from
            Quaternion fromToLast = Quaternion.compose(chain.get(from).rotation().inverse(), chain.get(from + times).rotation());
            //Quaternion alpha_hat = Quaternion.compose(fromToLast, alpha);
            //TODO : Damp the rotation
            //alpha_hat.compose(fromToLast.inverse());
            //alpha_hat = Quaternion.slerp(new Quaternion(), alpha_hat, 0.25f);
            //alpha_hat = new Quaternion();
            //calculate error
            float e1 = Vector.distance(chain.get(chain.size() - 1).position(), target.position());
            float e2 = Vector.distance(chain.get(from + times).position(), auxiliary_chain.get(from + times).position());

            Pair<Quaternion, Float> action = new Pair<Quaternion, Float>(initial, e1 + e2 * 0.5f);
            actions.add(action);
            return null;
        }

        //System.out.println("    ----> on depth " + depth);
        if(depth == 0 || mode == LookAheadMode.CHOOSE_ALL){ //Visit each action (expand the tree)
            for (Quaternion rotation : _findActions(chain, auxiliary_chain, target, direction, enableWeight, from + depth, true, depth == 0 && explore)){
                if(depth == 0) initial = rotation;
                //System.out.println("    ----> Rot " + rotation.axis() + rotation.angle());
                Node j_i = chain.get(from + depth);
                Quaternion prev = j_i.rotation().get(); //Keep rotation
                j_i.rotate(rotation); //Apply action
                _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, mode, from, times, depth + 1, initial, actions); //look ahead
                j_i.setRotation(prev.get()); //Undo action
            }
        } else{
            //Expand only the most promising action
            List<Quaternion> rotations = _findActions(chain, auxiliary_chain, target, direction, enableWeight,from + depth, true, false);
            Quaternion best = null;
            float error = 10e10f;
            Node j_i = chain.get(from + depth);
            Quaternion prev = j_i.rotation().get(); //Keep rotation
            for (Quaternion rotation : rotations) {
                j_i.rotate(rotation); //Apply action
                float e = Vector.distance(chain.get(chain.size() - 1).position(), target.position());
                if(e < error){
                    error = e;
                    best = rotation;
                }
                j_i.setRotation(prev.get()); //Undo action
            }
            if(depth == 0) initial = best;
            j_i.rotate(best); //Apply action
            _lookAhead(chain, auxiliary_chain, target, direction, enableWeight, explore, mode, from, times, depth + 1, initial, actions); //look ahead
            j_i.setRotation(prev.get()); //Undo action
        }

        if(depth == 0){
            Pair<Quaternion, Float> best = actions.get(0);
            if(_debug) System.out.println("----Actions---");
            int i = 0, b = 0;
            for(Pair<Quaternion, Float> action : actions){
                if(_debug) System.out.println("Action a " + i++ + "   " + action.getKey().axis() + action.getKey().angle() + " err " + action.getValue());
                //pick the action with the best value
                if(action.getValue() < best.getValue()){
                    best = action;
                    b = i - 1;
                }
            }
            if(_debug) System.out.println("BEST action : " + b);
            if(_debug) System.out.println("-----------");
            if(_debug) System.out.println("-----------");
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
    protected float avg_bone_length;

    List<GTarget> _gTargets = new ArrayList<GTarget>();

    protected void _calculateAverageLength(){
        for(int i = 1; i < _original.size(); i++){
            avg_bone_length += _original.get(i).translation().magnitude() * _original.get(i).magnitude();
        }
        avg_bone_length /= _original.size();
    }

    public void addGTarget(Node target, float g){
        GTarget gTarget = new GTarget(target, g);
        //calculate force that must be applied to each joint
        _gTargets.add(gTarget);
    }

    class GTarget{
        protected float _g;
        protected Vector _prev;
        protected Node _node;
        protected HashMap<Node, Float> _influence;

        public GTarget(Node node, float g){
            this._node = node;
            this._g = g;
            _influence = new HashMap<Node, Float>();
        }

        public Vector pointVector(Node node, Vector next){
            //1. Find the Direction between this target and the given Node
            Vector vector = Vector.subtract(node.location(_node), next);
            float mag = vector.magnitude();
            //2. scale the vector
            vector.multiply(_g / mag);
            _influence.put(node, _g / mag);
            return vector;
        }
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
        Node ref = j_i.reference() != null ? j_i.reference() : new Node();
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


    protected void _fixDeadLock(){
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
    }


    //TODO : REFINE

    /*
    Spread:



     */

    //HERE ARE DEFINED THE AUXILIARY CLASSES




    //HERE ARE DEFINED THE TWIST/SWING ACTIONS
    protected void _copyChainState(List<? extends Node> origin, List<? extends Node> dest){
        //Copy the content of the origin chain into dest
        if(dest.get(0).reference() != null && origin.get(0).reference() != null){ //TODO: CHECK THIS!
            Constraint constraint = dest.get(0).reference().constraint();
            dest.get(0).reference().setConstraint(null);
            dest.get(0).reference().set(origin.get(0).reference());
            dest.get(0).reference().setConstraint(constraint);
        }

        for(int i = 0; i < origin.size(); i++){
            Constraint constraint = dest.get(i).constraint();
            dest.get(i).setConstraint(null);
            dest.get(i).set(origin.get(i));
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

    protected void _translateToTarget(Node root, Node eff, Node target){
        //Find the distance from EFF current position and Target
        Vector diff = Vector.subtract(target.position(), eff.position());
        //Move the root accordingly (disable constraints)
        Constraint constraint = root.constraint();
        root.setConstraint(null);
        //root.translate(diff);
        root.setPosition(Vector.add(root.position(), diff)); //TODO: CHECK THIS!
        root.setConstraint(constraint); // enable constraint

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

    protected void _alignToTarget(Node root, Node eff, Node target){
        if(_direction) {
            Quaternion delta = Quaternion.multiply(target.orientation(), eff.orientation().inverse());
            //clamp delta to a max of 20 degrees
            float angle = delta.angle();
            angle = Math.abs(angle) > Math.toRadians(20) ? (float) (Math.signum(angle) * Math.toRadians(20)) : angle;


            delta = new Quaternion(delta.axis(), angle);
            //Apply rotation to root
            Quaternion initial = root.rotation().get();
            root.setOrientation(Quaternion.compose(delta, root.orientation()));
            if(_enableMediator) {
                InterestingEvent event = mediator().addEventStartingAfterLast("ROOT ROTATION", "NodeRotation", 1, 1); //Create the event
                event.addAttribute("node", root); //Add the convenient attributes
                event.addAttribute("rotation", Quaternion.compose(initial.inverse(), root.rotation()));
                InterestingEvent event6 = mediator().addEventStartingWithLast("ROTATION MESSAGE", "Message", 0, 1); //Create the event
                event6.addAttribute("message", "Rotate to align target direction"); //Add the convenient attributes
            }
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

    protected static Quaternion _findLocalRotation(Node j_i1, Node j_i1_hat, boolean enableWeight){
        Node j_i = j_i1.reference(); //must have a parent
        //Define how much rotate j_i in order to align j_i1 with j_i1_hat
        Vector p = j_i1.translation();
        //Find j_i1_hat w.r.t j_i
        Vector q = j_i.location(j_i1_hat);
        //Find vector desired to accomplish secondary target
        /*Vector secondary = _calculateGTargetDirection(j_i, j_i1);
        if (secondary != null) {
            //Find weighted average
            secondary.normalize();
            q.normalize();
            float targetWeight = 0.8f;
            q = Vector.add(Vector.multiply(q, targetWeight), Vector.multiply(secondary, 1 - targetWeight));
            q.normalize();
            av.add(new Pair<>(j_i.worldLocation(Vector.multiply(q, 50)),j_i.position()));
        }*/
        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);
        float weight = 1;
        if(enableWeight) {
            weight = _calculateWeight(j_i1.translation().magnitude(), j_i.location(j_i1_hat).magnitude());
            if(_singleStep)System.out.println("weight : " + weight);
            delta = new Quaternion(delta.axis(), delta.angle() * weight);
        }
        return delta;
    }

    protected Vector _calculateGTargetDirection(Node j_i, Node j_i1){
        if(_gTargets == null || _gTargets.isEmpty()) return null;
        Vector secondaryDirection = new Vector();
        for(GTarget gTarget : _gTargets){
            Vector v = gTarget.pointVector(j_i, j_i1.translation());
            if(v != null) secondaryDirection.add(v);
        }
        secondaryDirection.multiply(1f/_gTargets.size());
        return secondaryDirection;
    }


    protected static Quaternion _findTwisting(Node j_i, Node j_i1, Node eff, Node target, float maxAngle){ //Math.toRadians(15)
        //Find twist that approach EFF to its desired orientation
        Quaternion O_i_inv = j_i.rotation().inverse();
        //q_delta = O_i_inv * O_t * (O_i_inv * O_eff)^-1
        Quaternion delta = Quaternion.compose(O_i_inv, target.orientation());
        delta.compose(Quaternion.compose(O_i_inv, eff.orientation()));
        //get the twist component of the given quaternion
        Vector tw = j_i1.translation(); // w.r.t j_i
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
    protected static Quaternion _findCCDTwist(Node j_i, Node j_i1, Node eff, Node target, float maxAngle){ //(float) Math.toRadians(20)
        Vector j_i_to_target = j_i.location(target);
        Vector j_i_to_eff = j_i.location(eff);
        Vector j_i_to_eff_proj, j_i_to_target_proj;
        Vector tw = j_i1.translation(); // w.r.t j_i
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


    protected static Quaternion _findTwist(Node j_i, Node j_i1, Node eff, Node target, float maxT1, float maxT2, boolean direction){
        //Step 3. Apply twisting to help reach desired rotation
        Quaternion t1, t2, twist;
        t1 = t2 = _findCCDTwist(j_i, j_i1, eff, target, maxT1);
        if(direction){
            t2 = _findTwisting(j_i, j_i1, eff, target, maxT2);
        }
        if(t1.axis().dot(t2.axis()) < 0){
            twist = new Quaternion(t1.axis(), 0.5f * t1.angle() - 0.5f * t2.angle());
        } else{
            twist = new Quaternion(t1.axis(), 0.5f * t1.angle() + 0.5f * t2.angle());
        }
        return twist;
    }

    protected static void applySwingTwist(Node j_i, Node j_i1, Node j_i1_hat, Node eff, Node target, float maxT1, float maxT2, boolean enableWeight, boolean direction){
        j_i.rotate(_findLocalRotation(j_i1, j_i1_hat, enableWeight)); //Apply swing
        j_i.rotate(_findTwist(j_i, j_i1, eff, target, maxT1, maxT2, direction)); //Apply twist
    }



}
