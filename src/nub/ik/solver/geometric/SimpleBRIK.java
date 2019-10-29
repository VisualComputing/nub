package nub.ik.solver.geometric;

import nub.core.Node;
import nub.core.constraint.Constraint;
import nub.ik.animation.InterestingEvent;
import nub.ik.animation.VisualizerMediator;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.Iterator;
import java.util.List;

public class SimpleBRIK extends Solver {
    protected List<? extends Node> _original;
    protected List<Node> _auxiliary_chain, _chain;
    protected Node _target;
    protected Node _previousTarget;
    protected boolean _direction = false;
    //Steady state algorithm
    protected float _current = 10e10f, _best = 10e10f;


    public SimpleBRIK(List<? extends Node> chain) {
        this(chain, null);
    }

    public SimpleBRIK(List<? extends Node> chain, Node target) {
        super();
        this._original = chain;
        this._chain = FABRIKSolver._copy(chain);
        this._auxiliary_chain = FABRIKSolver._copy(chain, false);
        this._target = target;
        this._previousTarget =
                target == null ? null : new Node(target.position().get(), target.orientation().get(), 1);
    }

    protected void _copyChainState(List<? extends Node> origin, List<? extends Node> dest){
       //Copy the content of the origin chain into dest
       for(int i = 0; i < origin.size(); i++){
           dest.get(i).set(origin.get(i));
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

    protected static void _applyLocalRotation(Node j_i1, Node j_i1_hat){
        Node j_i = j_i1.reference(); //must have a parent
        //Define how much rotate j_i in order to align j_i1 with j_i1_hat
        Vector p = j_i1.translation();
        //Find j_i1_hat w.r.t j_i
        Vector q = j_i.location(j_i1_hat);
        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);

        Vector tw = j_i1.translation(); // w.r.t j_i
        Vector rotationAxis = new Vector(delta._quaternion[0], delta._quaternion[1], delta._quaternion[2]);
        rotationAxis = Vector.projectVectorOnAxis(rotationAxis, tw); // w.r.t j_i
        //Get rotation component on Axis direction
        Quaternion rotationTwist = new Quaternion(rotationAxis.x(), rotationAxis.y(), rotationAxis.z(), delta.w()); //w.r.t j_i
        Quaternion rotationSwing = Quaternion.compose(delta, rotationTwist.inverse()); //w.r.t idle
        j_i.rotate(delta); //find a rotation from p to q
    }

    protected static Quaternion _applyTwisting(Node j_i, Node j_i1, Node eff, Node target){
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


    @Override
    protected boolean _iterate() {
        //As no target is specified there is no need to solve IK
        if (_target == null) return true;
        _current = 10e10f; //Keep the current error

        //Step 1. make a deep copy of _chain state into _auxiliary_chain
        _copyChainState(_chain, _auxiliary_chain);
        //Step 2. Translate the auxiliary chain to the target position
        _alignToTarget(_auxiliary_chain.get(0), _auxiliary_chain.get(_auxiliary_chain.size() - 1), _target);

        for(int i = 1; i < _chain.size(); i++) {
            //Step 3. Apply twisting to help reach desired rotation
            Quaternion t1, t2;
            t1 = t2 = _applyCCDTwist(_chain.get(i - 1), _chain.get(i), _chain.get(_chain.size() -1), _target);
            if(_direction){
                t2 = _applyTwisting(_chain.get(i - 1), _chain.get(i), _chain.get(_chain.size() -1), _target);
            }
            if(t1.axis().dot(t2.axis()) < 0){
                _chain.get(i - 1).rotate(t1.axis(), 0.5f * t1.angle() - 0.5f * t2.angle());
            } else{
                _chain.get(i - 1).rotate(t1.axis(), 0.5f * t1.angle() + 0.5f * t2.angle());
            }

            //Step 4. Apply local rotation to each joint of the chain
            _applyLocalRotation(_chain.get(i), _auxiliary_chain.get(i));
        }

        //Get the current error
        _current = Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position());
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
        if (Vector.distance(_chain.get(_chain.size() - 1).position(), _target.position()) <= _minDistance) return true;
        return  false;
    }

    @Override
    protected void _update() {
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
        _iterations = 0;

        if (_target != null) {
            _best = Vector.distance(_original.get(_original.size() - 1).position(), _target.position());
        } else {
            _best = 10e10f;
        }
    }

    @Override
    public float error() {
        //TODO : Error must be defined as a weighted average between orientational error and translational error
        return Vector.distance(_original.get(_original.size() - 1).position(), _target.position());
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

}
