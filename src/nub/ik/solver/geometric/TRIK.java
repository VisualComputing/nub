package nub.ik.solver.geometric;

import javafx.util.Pair;
import nub.core.Node;
import nub.core.constraint.Constraint;
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
    protected List<Node> _auxiliary_chain, _chain;
    protected Node _target;
    protected Node _previousTarget;
    protected boolean _direction = false;
    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;

    //TODO : REMOVE!
    protected boolean _debug = false;

    public TRIK(List<? extends Node> chain) {
        this(chain, null);
    }

    public TRIK(List<? extends Node> chain, Node target) {
        super();
        this._original = chain;
        if(_debug && _original.get(0).graph() instanceof Scene) {
            this._chain = FABRIKSolver._copy(chain, null, (Scene) _original.get(0).graph());
            this._auxiliary_chain = FABRIKSolver._copy(chain, null, (Scene) _original.get(0).graph(), false);
        }
        else {
            this._chain = FABRIKSolver._copy(chain);
            this._auxiliary_chain = FABRIKSolver._copy(chain, false);
        }
        this._target = target;
        this._previousTarget =
                target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
    }

    protected void _copyChainState(List<? extends Node> origin, List<? extends Node> dest){
       //Copy the content of the origin chain into dest
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
        root.translate(diff);
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

    //TODO REMOVE!
    public List<Pair<Vector, Vector>> sec = new ArrayList<>();
    public List<Pair<Vector, Vector>> main = new ArrayList<>();
    public List<Pair<Vector, Vector>> av = new ArrayList<>();

    protected Quaternion _applyLocalRotation(Node j_i1, Node j_i1_hat){
        Node j_i = j_i1.reference(); //must have a parent
        //Define how much rotate j_i in order to align j_i1 with j_i1_hat
        Vector p = j_i1.translation();
        //Find j_i1_hat w.r.t j_i
        Vector q = j_i.location(j_i1_hat);
        main.add(new Pair<>(j_i.worldLocation(q),j_i.position()));
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
        Vector tw = j_i1.translation(); // w.r.t j_i
        Vector rotationAxis = new Vector(delta._quaternion[0], delta._quaternion[1], delta._quaternion[2]);
        rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t j_i
        //Get rotation component on Axis direction
        Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), delta.w()); //w.r.t j_i
        Quaternion rotationSwing = Quaternion.compose(delta, rotationTwist.inverse()); //w.r.t idle
        //j_i.rotate(delta); //find a rotation from p to q
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


    protected Quaternion _applyTwisting(Node j_i, Node j_i1, Node eff, Node target){
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
        rotationTwistAngle = Math.abs(rotationTwistAngle) > Math.toRadians(15) ? (float) (Math.signum(rotationTwistAngle) * Math.toRadians(15)) : rotationTwistAngle;
        rotationTwist = new Quaternion(rotationTwist.axis(), rotationTwistAngle);
        //Apply twist to j_i
        return rotationTwist;
    }

    //Try to approach to target final position by means of twisting
    protected static Quaternion _applyCCDTwist(Node j_i, Node j_i1, Node eff, Node target){
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
        angle = Math.min(angle, (float) Math.toRadians(20));
        if(Vector.cross(j_i_to_eff_proj, j_i_to_target_proj, null).dot(tw) <0)
            angle *= -1;
        return new Quaternion(tw, angle);
    }


    protected Quaternion _bestLocalActions(Node j_i, Node j_i1, Node j_i1_hat, Node eff, Node target){
            //Step 3. Apply twisting to help reach desired rotation
            Quaternion t1, t2, twist, swing;
            t1 = t2 = _applyCCDTwist(j_i, j_i1, eff, target);
            if(_direction){
                t2 = _applyTwisting(j_i, j_i1, eff, target);
            }
            if(t1.axis().dot(t2.axis()) < 0){
                twist = new Quaternion(t1.axis(), 0.5f * t1.angle() - 0.5f * t2.angle());
            } else{
                twist = new Quaternion(t1.axis(), 0.5f * t1.angle() + 0.5f * t2.angle());
            }
            //Step 4. Apply local rotation to each joint of the chain
            swing = _applyLocalRotation(j_i1, j_i1_hat);
            return Quaternion.compose(twist, swing);
    }

    @Override
    protected boolean _iterate() {
        main.clear(); av.clear(); sec.clear();//TODO: Remove!
        //As no target is specified there is no need to solve IK
        if (_target == null) return true;
        _current = 10e10f; //Keep the current error

        //Step 1. make a deep copy of _chain state into _auxiliary_chain
        _copyChainState(_chain, _auxiliary_chain);
        //Step 2. Translate the auxiliary chain to the target position
        _alignToTarget(_auxiliary_chain.get(0), _auxiliary_chain.get(_auxiliary_chain.size() - 1), _target);
        for(int i = 1; i < _chain.size(); i++) {
            Quaternion delta;
            if(_lookAhead > 0 && i < _chain.size() - 2){
                delta = _lookAhead(i - 1, Math.min(_lookAhead, _chain.size() - 2 - i));
            } else {
                delta =_bestLocalActions(_chain.get(i - 1), _chain.get(i), _auxiliary_chain.get(i), _chain.get(_chain.size() - 1), _target);
            }
            _chain.get(i - 1).rotate(delta);
        }
        //Get the current error
        _current = _error(_chain);
        _update();

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
        //System.out.println("Current : " + _current + " best " + _best);
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

    //TODO: Move this class
    public class NodeState{
        Vector _translation;
        Quaternion _rotation;
        public NodeState(Node node){
            _translation = node.translation().get();
            _rotation = node.rotation().get();
        }

        public void apply(Node node){
            node.setTranslation(_translation);
            node.setRotation(_rotation);
        }
    }


    protected int _lookAhead = 0;

    protected Quaternion _lookAhead(int from, int times){
        if(times <= 0) return null;
        //save node states
        NodeState states[] = new NodeState[times + 1];
        Quaternion best = null;

        for(int i = from; i <= from + times; i++) {
            Node j_i = _chain.get(i);
            Node j_i1 = _chain.get(i + 1);
            Node j_i1_hat = _auxiliary_chain.get(i + 1);
            //save state prior to modification
            states[i - from] = new NodeState(j_i);
            Quaternion delta = _bestLocalActions(j_i, j_i1, j_i1_hat, _chain.get(_chain.size() -1), _target);
            j_i.rotate(delta);
            if(i == from) best = delta;
        }

        //Given configuration until now try to help the algorithm in a future
        //alpha w.r.t last
        Quaternion alpha = _applyLocalRotation(_chain.get(from + times),  _auxiliary_chain.get(from + times));
        //get alpha w.r.t from
        Quaternion fromToLast = Quaternion.compose(_chain.get(from).rotation().inverse(), _chain.get(from + times).rotation());
        Quaternion alpha_hat = Quaternion.compose(fromToLast, alpha);
        //TODO : Damp the rotation
        alpha_hat.compose(fromToLast.inverse());
        //revert actions
        for(int i = from; i < from + times + 1; i++) {
            states[i - from].apply(_chain.get(i));
        }
        //Return the best found action in addition to fixed action (alpha hat)
        return Quaternion.compose(best, alpha_hat);
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
            sec.add(new Pair<>(node.worldLocation(next).get(),_node.position()));
            float mag = vector.magnitude();
            //2. scale the vector
            vector.multiply(_g / mag);
            _influence.put(node, _g / mag);
            return vector;
        }
    }
}
