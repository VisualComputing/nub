package nub.ik.solver.geometric;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.ConeConstraint;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.Solver;
import nub.ik.visual.Joint;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.List;

public class MySolver extends Solver{
    protected List<Node> _chain, _reversed;
    protected List<? extends Node> _original;
    protected BallAndSocket[] _globalConstraints;
    protected float _current = 10e10f, _best = 10e10f;
    protected Node _target;
    protected Node _prevTarget;
    protected boolean debug = true; //TODO: remove this

    public Node target(){
        return _target;
    }

    public MySolver(List<? extends Node> chain) {
        this(chain, null);
    }

    public MySolver(List<? extends Node> chain, Node target){
        super();
        this._original = chain;
        this._chain = FABRIKSolver._copy(chain); //TODO: Move this method
        this._reversed = _reverseChain(_original, (Scene)_original.get(0).graph());
        _generateGlobalConstraints();
    }

    int st = 0, cur = 0;
    boolean bkd = false;
    @Override
    protected boolean _iterate() {
        //As no target is specified there is no need to perform an iteration
        if (_target == null || _chain.size() < 2) return true;
        Node end = _chain.get(_chain.size() - 1);
        Vector target = this._target.position().get();
        //Execute Until the distance between the end effector and the target is below a threshold
        if (Vector.distance(end.position(), target) <= _maxError) {
            return true;
        }
        // /*NON DEBUG
        if(!debug) {
            st = 0;
            while (st < _original.size() - 1) {
                _applyStep(_reversed, st, _original.get(0).position());
                st++;
            }
            //Apply bkwd step
            if(_original.get(0).constraint() != null) {
                //2. Apply initial constraint
                Vector desired = Vector.subtract(_reversed.get(_reversed.size() - 2).position(), _reversed.get(_reversed.size() - 1).position());
                System.out.println("desired : " + desired);
                //w.r.t j0
                desired = _original.get(0).displacement(desired);
                System.out.println("desired : " + desired);
                //Find rotation
                Quaternion desired_delta = new Quaternion(_original.get(1).translation(), desired);
                System.out.println("original : " + _original.get(1).translation());
                //Find inv constrained rotation
                Quaternion constrained = _original.get(0).constraint().constrainRotation(desired_delta, _original.get(0));
                System.out.println("desired_de : " + desired_delta.axis() + Math.toDegrees(desired_delta.angle()));
                System.out.println("constr : " + constrained.axis() + Math.toDegrees(constrained.angle()));
                constrained.compose(desired_delta.inverse());
                System.out.println("constr diff: " + constrained.axis() + Math.toDegrees(constrained.angle()));

                //constrained = constrained.inverse();
                System.out.println("constr inv: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                //get constrained in terms of world
                Quaternion o = _original.get(0).reference() != null ? _original.get(0).reference().orientation() : new Quaternion();
                constrained = Quaternion.compose(o, constrained);
                constrained.compose(o.inverse());
                System.out.println("constr inv world: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                //get constrained in terms of r0
                o = _reversed.get(0).reference() != null ? _reversed.get(0).reference().orientation() : new Quaternion();
                constrained = Quaternion.compose(o.inverse(), constrained);
                constrained.compose(o);
                System.out.println("constr inv local: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                //Apply rotation to reversed
                _reversed.get(0).rotate(constrained);
                //translate reversed
            }
            translateChain(_reversed, Vector.add(_reversed.get(0).position(), Vector.subtract(_original.get(0).position(), _reversed.get(_reversed.size() - 1).position())));
            _updateReverse(_reversed, _original);
            st = 0;
            while (st < _original.size() - 1) {
                _applyStep(_original, st, _target.position());
                st++;
            }
            translateChain(_reversed, _target.position());
            _updateReverse(_original, _reversed);
            //_update(_original, _reversed);
            //_updateReverse(_original, _reversed);
        }
        else {
            /*DEBUG*/
            //st = 0;
            //while(st < _original.size() - 1) {
            if (st <= _original.size() - 2 && !bkd) {
                _applyStep(_reversed, st, _original.get(0).position());
            } else if(st == _original.size() - 1 && !bkd){
                bkd = true;
                //Apply bkwd step
                if(_original.get(0).constraint() != null) {
                    //2. Apply initial constraint
                    Vector desired = Vector.subtract(_reversed.get(_reversed.size() - 2).position(), _reversed.get(_reversed.size() - 1).position());
                    System.out.println("desired : " + desired);
                    //w.r.t j0
                    desired = _original.get(0).displacement(desired);
                    System.out.println("desired : " + desired);
                    //Find rotation
                    Quaternion desired_delta = new Quaternion(_original.get(1).translation(), desired);
                    System.out.println("original : " + _original.get(1).translation());
                    //Find inv constrained rotation
                    Quaternion constrained = _original.get(0).constraint().constrainRotation(desired_delta, _original.get(0));
                    System.out.println("desired_de : " + desired_delta.axis() + Math.toDegrees(desired_delta.angle()));
                    System.out.println("constr : " + constrained.axis() + Math.toDegrees(constrained.angle()));
                    constrained.compose(desired_delta.inverse());
                    System.out.println("constr diff: " + constrained.axis() + Math.toDegrees(constrained.angle()));

                    //constrained = constrained.inverse();
                    System.out.println("constr inv: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                    //get constrained in terms of world
                    Quaternion o = _original.get(0).reference() != null ? _original.get(0).reference().orientation() : new Quaternion();
                    constrained = Quaternion.compose(o, constrained);
                    constrained.compose(o.inverse());
                    System.out.println("constr inv world: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                    //get constrained in terms of r0
                    o = _reversed.get(0).reference() != null ? _reversed.get(0).reference().orientation() : new Quaternion();
                    constrained = Quaternion.compose(o.inverse(), constrained);
                    constrained.compose(o);
                    System.out.println("constr inv local: " + constrained.axis() + Math.toDegrees(constrained.angle()));
                    //Apply rotation to reversed
                    _reversed.get(0).rotate(constrained);
                    //translate reversed
                }
                translateChain(_reversed, Vector.add(_reversed.get(0).position(), Vector.subtract(_original.get(0).position(), _reversed.get(_reversed.size() - 1).position())));
                _updateReverse(_reversed, _original);
                st = 0;
            } else if(st <= _original.size() - 2 && bkd){
                _applyStep(_original, st, _target.position());
                //_update2(_original, _reversed);
            }else{
                translateChain(_reversed, _target.position());
                _updateReverse(_original, _reversed);
                st = 0;
                bkd = false;
            }
            //st++;
            //}
            //st = (st + 1) % (_original.size() - 1);
            /**/
            //Check total rotation change
        }
        return false;
    }

    protected void _update(List<? extends Node> chain, List<? extends Node> reversed){
        for(int i = 0; i < chain.size()-1; i++){
            Quaternion q = new Quaternion(chain.get(i).location(chain.get(i+1)), chain.get(i).location(reversed.get(reversed.size() - 2 - i)));
            chain.get(i).rotate(q);
        }
    }

    protected void _update2(List<? extends Node> chain, List<? extends Node> reversed){
        //1. translate reversed
        translateChain(reversed, Vector.add(reversed.get(0).position(), Vector.subtract(chain.get(0).position(), reversed.get(reversed.size() - 1).position())));
        //2. Apply initial constraint
        Vector desired = Vector.subtract(reversed.get(reversed.size() - 2).position(),reversed.get(reversed.size() - 1).position());
        //w.r.t j0
        desired = chain.get(0).displacement(desired);
        //Find rotation
        System.out.println("--rest " + chain.get(1).translation());
        System.out.println("--des " + desired);
        Quaternion desired_delta = new Quaternion(chain.get(1).translation(), desired);
        System.out.println("--des q" + desired_delta);
        //Find inv constrained rotation
        Quaternion constrained = chain.get(0).constraint().constrainRotation(desired_delta, chain.get(0));
        System.out.println("--const " + constrained);

        constrained.compose(desired_delta.inverse());
        constrained = constrained.inverse();
        //get constrained in terms of world
        Quaternion o = chain.get(0).reference() != null ? chain.get(0).reference().orientation() : new Quaternion();
        constrained = Quaternion.compose(o, constrained);
        constrained.compose(o.inverse());
        //get constrained in terms of r0
        o = reversed.get(0).reference() != null ? reversed.get(0).reference().orientation() : new Quaternion();
        constrained = Quaternion.compose(o.inverse(), constrained);
        constrained.compose(o);
        //Apply rotation to reversed
        reversed.get(0).rotate(constrained);
        _updateReverse(reversed, chain);
        int st = 0;
        while (st < _original.size() - 1) {
            _applyStep(_original, st, _target.position());
            st++;
        }
        translateChain(_reversed, _target.position());
    }

    protected void _updateReverse(List<? extends Node> chain, List<? extends Node> reversed){
        int n = reversed.size();
        for(int i = 0; i < n-1; i++){
            Vector v1 = Vector.subtract(reversed.get(i+1).position(), reversed.get(i).position());
            Vector v2 = Vector.subtract(chain.get(n - 2 - i).position(), chain.get(n - 1 - i).position());
            Quaternion q = new Quaternion(reversed.get(i).displacement(v1), reversed.get(i).displacement(v2));
            reversed.get(i).rotate(q);
        }
    }


    @Override
    protected void _update() {
        //if(st == 0) {
            //_update(_original, _reversed);
            //_updateReverse(_original, _reversed);
        //}
    }

    @Override
    protected boolean _changed() {
        if (_target == null) {
            _prevTarget = null;
            return false;
        } else if (_prevTarget == null) {
            return true;
        }
        return !(_prevTarget.position().matches(_target.position()) && _prevTarget.orientation().matches(_target.orientation()));
    }

    @Override
    protected void _reset() {
        _prevTarget = _target == null ? null : new Node(_target.position().get(), _target.orientation().get(), 1);
        _iterations = 0;
        st = 0; cur = 0; bkd = false;
        _updateReverse(_original, _reversed);
        translateChain(_reversed, _target.position());
    }

    @Override
    public float error() {
        return Vector.distance(_original.get(_original.size() - 1).position(), _target.position());
    }


    public void setTarget(Node endEffector, Node target) {
        this._target = target;
    }

    public void setTarget(Node target) {
        this._target = target;
    }

    //Step 1: Translate the whole chain to reach Target
    //TODO: Consider rotation and apply a global rotation ?
    protected void translateChain(List<? extends Node> reversed, Vector target){
        Vector endEffector = reversed.get(0).position();
        Vector difference = Vector.subtract(target, endEffector);
        Constraint constraint = reversed.get(0).constraint();
        reversed.get(0).setConstraint(null); //TODO : Disable/Enable constraint method  on Node ?
        reversed.get(0).setPosition(Vector.add(endEffector, difference));
        reversed.get(0).setConstraint(constraint);
    }

    //Step 2:
    //Goal: Decrease distance between j_0 and j_0_hat
    //Step 2.1 Stretch / Contract chain in order to to min | distance(j_i_hat, j_0_hat) - distance(j_i_hat, j_0) |
    //Step 2.2 Apply a rotation that approach root and projected root as much as possible
    protected void _applyStep(List<? extends Node> reversed, int i, Vector target){
        System.out.println("=========================");
        System.out.println("=========================");
        System.out.println("i :" + i + " cur : " + cur);
        //Get how much to extend
        Vector root_j_i_hat = Vector.subtract(reversed.get(i).position(), target);
        Vector root_hat_j_i_hat = Vector.subtract(reversed.get(i).position(), reversed.get(reversed.size() - 1).position());
        float desired_extension = root_j_i_hat.magnitude();
        float current_extension = root_hat_j_i_hat.magnitude();
        float difference = desired_extension - current_extension;
        //distribute the job between remaining Joints and current one
        //TODO: use constraints
        float remaining_possible_extension = 0;
        float local_possible_extension = 0;
        Vector dir = Vector.subtract(reversed.get(i + 1).position(), reversed.get(i).position());
        if(difference > 0){
            remaining_possible_extension = _possibleGlobalExtension2(reversed, i + 2, reversed.size() - 1);
            local_possible_extension = _possibleLocalExtension(reversed, i);
            System.out.println("Ext : " + remaining_possible_extension + " Local : " + local_possible_extension);
            if(remaining_possible_extension < 0){
                System.out.println("ERROR!!!!!!");
                throw new IllegalArgumentException("Max es menor!");
            }

        }else{
            //TODO: Check this with pre-calculations
            remaining_possible_extension = _possibleGlobalContraction2(reversed, i + 2, reversed.size() - 1);
            local_possible_extension = _possibleLocalContraction(reversed, i);
            System.out.println("Cont : " + remaining_possible_extension + " Local : " + local_possible_extension);
        }
        float extension = Math.signum(difference) * _localExtension(remaining_possible_extension, local_possible_extension, Math.abs(difference), Math.max(reversed.size() - i - 2, 1));
        System.out.println("Current ext : " + current_extension);
        System.out.println("Desired ext : " + desired_extension);
        System.out.println("Extension final : " + extension);
        //Apply defined extension
        //NON DEBUG
        if(!debug) {

            _applyExtension(reversed, i + 1, extension);
            _applyRotation(reversed, i, target);
        }
        else {
            /*DEBUG
             */
            if (cur == 0) {
                _applyExtension(reversed, i + 1, extension);
                cur++;
            } else if (cur == 1) {
                _applyRotation(reversed, i, target);
                cur = 0;
                st++;
            }
        }
        /**/
    }


    protected float _possibleLocalExtension(List<? extends Node> chain, int i){
        return _possibleGlobalExtension2(chain, i, i + 2);
    }

    //TODO: Do the same calculation but with constrained structures
    //How much could the chain extend given its current configuration
    protected float _possibleGlobalExtension(List<? extends Node> chain, int init, int end){
        if(init >= chain.size() || init >= end || end >= chain.size()) return 0;
        Vector current = Vector.subtract(chain.get(init).position(), chain.get(end).position());
        //Vector proj = Vector.projectVectorOnAxis(current, axis);
        float current_extension = current.magnitude();
        float max = 0;
        for(int i = init; i < end; i++){
            Vector bone = Vector.subtract(chain.get(i).position(), chain.get(i + 1).position());
            max += bone.magnitude();
        }
        System.out.println("°°°°°°°°°° MAX " + max);
        System.out.println("°°°°°°°°°° Curr " + current);
        System.out.println("°°°°°°°°°° Curr_ext " + current_extension);

        return max - current_extension;
    }

    protected float _possibleGlobalExtension2(List<? extends Node> chain, int init, int end){
        System.out.println("EXTENSION 2 -------------");
        if(init >= chain.size() || init >= end || end >= chain.size()) return 0;
        Vector current = Vector.subtract(chain.get(init).position(), chain.get(end).position());
        float current_extension = current.magnitude();
        Quaternion ref = chain.get(init).orientation().get();
        Vector last = chain.get(init + 1).position().get();
        for(int i = init + 1; i < end; i++){
            Node j_i = chain.get(i);
            ref.compose(j_i.rotation());

            Vector desired_direction = ref.inverseRotate(Vector.subtract(last, chain.get(init).position()));
            Vector current_direction = chain.get(i+1).translation();
            System.out.println("°°°°°°°°°° desired dir " + desired_direction);
            System.out.println("°°°°°°°°°° current dir " + current_direction);
            //Quaternion rotation = new Quaternion(current_direction, desired_direction);
            //Similar as Quaternion(current, desired) but considering singularities (rot of nearly PI) when dealing with Hinge
            Quaternion rotation;
            float fromSqNorm = current_direction.squaredNorm();
            float toSqNorm = desired_direction.squaredNorm();
            // Identity Quaternion when one vector is null
            if (fromSqNorm == 0 || toSqNorm == 0) {
                rotation = new Quaternion();
            } else {
                Vector axis = current_direction.cross(desired_direction);
                float axisSqNorm = axis.squaredNorm();
                // Aligned vectors, pick any axis, not aligned with from or to
                if(axis.magnitude() < 1e-4){ //If angle is nearly PI
                    axis = desired_direction.orthogonalVector();
                    if(j_i.constraint() instanceof Hinge){
                        //Project Hinge axis on plane defined by axis
                        axis = Vector.projectVectorOnPlane(((Hinge) j_i.constraint()).restRotation().rotate(new Vector(0,0,1)), desired_direction);
                    }
                }
                float angle = (float) Math.asin((float) Math.sqrt(axisSqNorm / (fromSqNorm * toSqNorm)));
                if (current_direction.dot(desired_direction) < 0.0)
                    angle = (float) Math.PI - angle;

                rotation = new Quaternion(axis, angle);
            }

            Quaternion constrained_rotation = j_i.constraint() != null ? j_i.constraint().constrainRotation(rotation, j_i) : rotation;
            //Pass to world coordinates
            ref.compose(constrained_rotation);
            last.add(ref.rotate(current_direction));
        }
        float max = Vector.subtract(last, chain.get(init).position()).magnitude();
        System.out.println("°°°°°°°°°° MAX " + max);
        System.out.println("°°°°°°°°°° Curr " + current);
        System.out.println("°°°°°°°°°° Curr_ext " + current_extension);
        return Math.max(max - current_extension, 0);
    }


    protected float _possibleGlobalContraction(List<? extends Node> chain, int init, int end){
        if(init >= chain.size() || init >= end || end >= chain.size()) return 0;
        return Vector.subtract(chain.get(init).position(), chain.get(end).position()).magnitude();
    }

    /*
    * Dummy approach, perhaps require further thinking
    *
    * */
    protected float _possibleGlobalContraction2(List<? extends Node> chain, int init, int end){
        System.out.println("Contraction 2 -------------");
        if(init >= chain.size() || init >= end || end >= chain.size()) return 0;
        Vector current = Vector.subtract(chain.get(init).position(), chain.get(end).position());
        float current_extension = current.magnitude();
        Quaternion ref = chain.get(init).orientation().get();
        Vector last = chain.get(init + 1).position().get();
        for(int i = init + 1; i < end; i++){
            Node j_i = chain.get(i);
            Vector desired_direction = ref.inverseRotate(Vector.subtract(chain.get(init).position(), last));
            Vector current_direction = chain.get(i+1).translation();
            System.out.println("°°°°°°°°°° desired dir " + desired_direction);
            System.out.println("°°°°°°°°°° current dir " + current_direction);
            System.out.println("°°°°°°°°°° axis  " + current_direction);

            //Similar as Quaternion(current, desired) but considering singularities (rot of nearly PI) when dealing with Hinge
            Quaternion rotation;
            float fromSqNorm = current_direction.squaredNorm();
            float toSqNorm = desired_direction.squaredNorm();
            // Identity Quaternion when one vector is null
            if (fromSqNorm == 0 || toSqNorm == 0) {
                rotation = new Quaternion();
            } else {
                Vector axis = current_direction.cross(desired_direction);
                float axisSqNorm = axis.squaredNorm();
                // Aligned vectors, pick any axis, not aligned with from or to
                if(axis.magnitude() < 1e-4){ //If angle is nearly PI
                    axis = desired_direction.orthogonalVector();
                    if(j_i.constraint() instanceof Hinge){
                        //Project Hinge axis on plane defined by axis
                        axis = Vector.projectVectorOnPlane(((Hinge) j_i.constraint()).restRotation().rotate(new Vector(0,0,1)), desired_direction);
                    }
                }
                float angle = (float) Math.asin((float) Math.sqrt(axisSqNorm / (fromSqNorm * toSqNorm)));
                if (current_direction.dot(desired_direction) < 0.0)
                    angle = (float) Math.PI - angle;

                rotation = new Quaternion(axis, angle);
            }
            Quaternion constrained_rotation = j_i.constraint() != null ? j_i.constraint().constrainRotation(rotation, j_i) : rotation;
            //Pass to world coordinates
            ref.compose(constrained_rotation);
            last.add(ref.rotate(current_direction));
        }
        Vector first = chain.get(init).position();
        float max = Vector.subtract(last, first).magnitude();
        System.out.println("°°°°°°°°°° MAX " + max);
        System.out.println("°°°°°°°°°° Curr " + current);
        System.out.println("°°°°°°°°°° Curr_ext " + current_extension);
        return Math.max(current_extension - max, 0);
    }


    //TODO: Do the same calculation but with constrained structures
    protected float _possibleLocalContraction(List<? extends Node> chain, int i){
        return _possibleGlobalContraction2(chain, i, i + 2);
    }

    //define how to extend/contract the local joint. the idea is to try to make all joint to move the same amount
    protected float _localExtension(float remaining, float local, float desired, int n){
        float avg_extension = desired/n;
        System.out.println("desired : " + desired);
        System.out.println("n : " + n);
        System.out.println("Avg : " + avg_extension);
        System.out.println("local : " + local);
        if(remaining + local < desired || avg_extension > local){
            return local;
        } else if(remaining + avg_extension < desired){
            return Math.min(Math.abs(desired - remaining), local);
        }
        return Math.max(avg_extension,Math.min(local, avg_extension * 1.1f));
    }

    protected void _applyExtension(List<? extends Node> reversed, int i, float extension){
        if(i >= reversed.size() - 1) return;
        Node j_i_prev = reversed.get(i - 1);
        Node j_i = reversed.get(i);
        Node j_i_next = reversed.get(i + 1);
        //Law of Cosine
        float a = Vector.subtract(j_i_prev.position(), j_i.position()).magnitude();
        float b = Vector.subtract(j_i_next.position(), j_i.position()).magnitude();
        float current_extension = Vector.subtract(j_i_prev.position(), j_i_next.position()).magnitude();
        System.out.println("a : " + a);
        System.out.println("b : " + b);
        System.out.println("---| cur extension : " + current_extension);
        System.out.println("---| extension : " + extension);

        float c = Math.max(Math.min(Math.abs(current_extension + extension), Math.abs(a + b)), Math.abs(a - b));
        float desired_theta = (float) Math.acos((-c*c + a*a + b*b)/(2*a*b));
        //Do all w.r.t j_i1
        System.out.println("---| desired_theta : " + Math.toDegrees(desired_theta));
        float current_theta = Vector.angleBetween(j_i.location(j_i_prev), j_i.location(j_i_next));
        System.out.println("---| current_theta : " + Math.toDegrees(current_theta));
        Vector axis = Vector.cross(j_i.location(j_i_prev), j_i.location(j_i_next), null);
        System.out.println(".....--| axis after cross : " + axis);
        if(axis.magnitude() < 1e-4){
            axis = j_i_next.translation().orthogonalVector();
            if(j_i.constraint() instanceof Hinge){
                //Project Hinge axis on plane defined by axis
                axis = Vector.projectVectorOnPlane(((Hinge) j_i.constraint()).restRotation().rotate(new Vector(0,0,1)), j_i.location(j_i_next));
            }
        }
        axis.normalize();
        float delta = desired_theta - current_theta;
        System.out.println(".....--| axis : " + axis);

        System.out.println(".....--| delta : " + Math.toDegrees(delta));
        delta = delta > Math.PI ? (float)(-2 *Math.PI + delta) : delta;
        delta = delta < -Math.PI ? (float)(2 *Math.PI + delta) : delta;

        if(j_i.constraint() != null){
            float delta2 = -desired_theta - current_theta;
            if(j_i.constraint() instanceof Hinge)System.out.println("Axis twist : " + ((Hinge) j_i.constraint()).restRotation().rotate(new Vector(0,0,1)));
            System.out.println(".....--| delta2 : " + Math.toDegrees(delta2));
            delta2 = delta2 > Math.PI ? (float)(-2 *Math.PI + delta2) : delta2;
            delta2 = delta2 < -Math.PI ? (float)(2 *Math.PI + delta2) : delta2;
            System.out.println(".....--| delta n : " + Math.toDegrees(delta));
            System.out.println(".....--| delta 2 n: " + Math.toDegrees(delta2));
            //If the joint has a constraint choose the best solution between theta and -theta
            Quaternion q1 = j_i.constraint().constrainRotation(new Quaternion(axis, delta), j_i);
            q1.normalize();
            Quaternion q2 = j_i.constraint().constrainRotation(new Quaternion(axis, delta2), j_i);
            q2.normalize();
            System.out.println(".....--| q1: " + q1.axis() + Math.toDegrees(q1.angle()));
            System.out.println(".....--| q1: " + q1.x() + ", " + q1.y() + ", " + q1.z() + ", " + q1.w() );
            System.out.println(".....--| q2: " + q2.axis() + Math.toDegrees(q2.angle()));

            //Get components of this rotation along axis
            Vector v1 = new Vector(q1.x(), q1.y(), q1.z());
            Vector v2 = new Vector(q2.x(), q2.y(), q2.z());
            Vector proj1 = Vector.projectVectorOnAxis(v1, axis);
            Vector proj2 = Vector.projectVectorOnAxis(v2, axis);
            Quaternion t1 = new Quaternion(proj1.x(), proj1.y(), proj1.z(), q1._quaternion[3]);
            Quaternion t2 = new Quaternion(proj2.x(), proj2.y(), proj2.z(), q2._quaternion[3]);

            //Keep the rotation whose angle is nearest to de desired one (delta)
            //TODO : Deal with singularity at PI
            System.out.println(".....--| t1: " + t1.axis() + Math.toDegrees(t1.angle()));
            System.out.println(".....--| t2: " + t2.axis() + Math.toDegrees(t2.angle()));

            float a1 = t1.axis().dot(axis) < 0 ? -t1.angle() : t1.angle();
            float a2 = t2.axis().dot(axis) < 0 ? -t2.angle() : t2.angle();

            System.out.println(".....--| a1: " + Math.toDegrees(a1));
            System.out.println(".....--| a2: " + Math.toDegrees(a2));

            if(Math.abs(delta - a1) > Math.abs(delta2 - a2)){
                delta = delta2;
            }
        }

        System.out.println("---| delta : " + Math.toDegrees(delta));
        System.out.println("---| axis : " + axis);
        j_i.rotate(axis, delta);
        System.out.println("---| final extension : " + Vector.subtract(j_i_prev.position(), j_i_next.position()).magnitude());
        return;
    }

    protected void _applyRotation(List<? extends Node> reversed, int i, Vector root){
        //apply a rotation that approach chain to original root
        Node j_i = reversed.get(i);
        root = j_i.location(root);
        Vector root_hat = j_i.location(reversed.get(reversed.size() - 1));
        Quaternion desired_rotation = new Quaternion(root_hat, root);
        j_i.rotate(desired_rotation);
        //j_i.rotate(desired_rotation.axis(), desired_rotation.angle()/(reversed.size() -1 - i));
    }

    protected static List<Node> _reverseChain(List<? extends Node> chain, Scene s){
        List<Node> reversed = new ArrayList<>();
        Node reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Node(reference.position().get(), reference.orientation().get(), 1);
        }
        for(int i = chain.size() -1; i >= 0; i--){
            Node joint = chain.get(i);
            Node newJoint = new Joint(s);
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            //newJoint.setConstraint(joint.constraint()); TODO: Invert constraints
            if(i > 0 && i < chain.size() -1) _reverseConstraint(chain.get(i - 1), joint,  chain.get(i + 1), newJoint);
            reversed.add(newJoint);
            reference = newJoint;
        }

        if(reversed.get(0) instanceof Joint){//TODO: Remove this
            ((Joint) reversed.get(0)).setRoot(true);
        }
        return reversed;
    }

    protected static void _reverseConstraint(Node j_i_prev, Node j_i, Node j_i_next, Node reversed_j_i){
        if(j_i.constraint() instanceof ConeConstraint){
            ConeConstraint constraint = (ConeConstraint) j_i.constraint();
            Quaternion q_i_prev =  j_i_prev != null ? j_i_prev.orientation() : new Quaternion();
            Quaternion rev_q_i_prev =  reversed_j_i.reference() != null ? reversed_j_i.reference().orientation() : new Quaternion();

            //Getting Reference Quaternion
            Quaternion local_ref = constraint.idleRotation(); //this reference is in terms of joint parent
            Quaternion global_ref = Quaternion.compose(q_i_prev,local_ref);
            //Align with previous segment
            Quaternion alignment = new Quaternion(Vector.subtract(j_i_next.position(), j_i.position()),
                    Vector.subtract(j_i.position(), j_i_prev != null ? j_i_prev.position() : new Vector()));
            global_ref = Quaternion.compose(alignment, global_ref);
            //Pass reference to reversed_joint space
            Quaternion reversed_local_ref = Quaternion.compose(rev_q_i_prev.inverse(), global_ref);

            //Getting up and twist vector
            Quaternion local_rest = constraint.restRotation();
            Vector up =local_rest.rotate(new Vector(0,1,0));
            //Reverse twist vector
            Vector twist = local_rest.rotate(new Vector(0,0,-1));
            //Attach new constraint
            if(j_i.constraint() instanceof BallAndSocket){
                BallAndSocket bs = (BallAndSocket) j_i.constraint();
                BallAndSocket rev_bs = new BallAndSocket(bs.down(), bs.up(), bs.right(), bs.left());
                rev_bs.setRestRotation(reversed_local_ref, up, twist);
                reversed_j_i.setConstraint(rev_bs);
            }
        } else if(j_i.constraint() instanceof Hinge){
            Hinge constraint = (Hinge) j_i.constraint();
            Quaternion q_i_prev = j_i_prev != null ? j_i_prev.orientation() : new Quaternion();
            Quaternion rev_q_i_prev = reversed_j_i.reference() != null ? reversed_j_i.reference().orientation() : new Quaternion();

            //Getting Reference quaternion
            Quaternion local_ref = constraint.idleRotation();
            Quaternion global_ref = Quaternion.compose(q_i_prev, local_ref);
            //Align with previous segment
            Quaternion alignment = new Quaternion(Vector.subtract(j_i_next.position(), j_i.position()),
                    Vector.subtract(j_i.position(), j_i_prev != null ? j_i_prev.position() : new Vector()));
            System.out.println("Alingmnet : " + alignment.axis() + " " + alignment.angle());
            global_ref = Quaternion.compose(alignment, global_ref);
            //Pass reference to reversed_joint space
            Quaternion reversed_local_ref = Quaternion.compose(rev_q_i_prev.inverse(), global_ref);

            //Getting up and twist vector
            Quaternion local_rest = constraint.restRotation();
            Vector up =local_rest.rotate(new Vector(0,-1,0));
            //Reverse twist vector
            Vector twist = local_rest.rotate(new Vector(0,0,1));
            //Attach new constraint
            Hinge rev_bs = new Hinge(constraint.maxAngle(), constraint.minAngle(), reversed_local_ref, up, twist);
            rev_bs.setRestRotation(reversed_local_ref, up, twist);
            reversed_j_i.setConstraint(rev_bs);
        }
    }




    //TODO: Remove this!
    public List<Node> reversed(){
        return _reversed;
    }

    protected void _generateGlobalConstraints(){
        _globalConstraints = new BallAndSocket[_chain.size()];
        Vector root_pos = _chain.get(0).position();
        System.out.println("Global Constraints Info");
        for(int i = 1; i < _chain.size(); i++){
            System.out.println("Joint i: " + i);
            Vector j_i_pos = _chain.get(i).position();
            Vector axis = Vector.subtract(j_i_pos, root_pos);
            Vector up_axis = Vector.orthogonalVector(axis);
            Vector right_axis = Vector.cross(axis, up_axis, null);
            Vector[] dirs = {up_axis, right_axis, Vector.multiply(up_axis, -1), Vector.multiply(right_axis, -1)};
            //TODO: Check avg vs Max / Min - Also, it is important to Take into account Twisting!
            //TODO: This will work better if we consider angles up to PI (current boundaries are at maximum PI/2)
            float[] angles = {0,0,0,0};
            for(int dir = 0; dir < 4; dir++) {
                List<Node> local_chain = FABRIKSolver._copy(_chain);
                while(local_chain.size() > i + 1) local_chain.remove(local_chain.size() - 1);

                for (int k = 0; k < i; k++) {
                    System.out.println("|--- Joint K: " + k);
                    Node j_k = local_chain.get(k);
                    //get axis in terms of J_k
                    Vector local_j_i = j_k.location(_chain.get(i));
                    Vector local_j_i_hat = j_k.location(local_chain.get(i));
                    Vector local_j_0 = j_k.location(root_pos);
                    Vector local_dir = j_k.displacement(dirs[dir]);
                    float desired_angle = _computeAngle(local_dir, local_j_0, local_j_i, local_j_i_hat);
                    //rotate by desired
                    if(j_k.constraint() instanceof BallAndSocket){
                        //TODO : CLEAN THIS! Do not apply any twisting
                        BallAndSocket c = ((BallAndSocket) j_k.constraint());
                        float min = c.minTwistAngle();
                        float max = c.maxTwistAngle();
                        c.setTwistLimits(0,0);
                        j_k.rotate(new Quaternion(local_dir, desired_angle));
                        c.setTwistLimits(min,max);
                    }else{
                        j_k.rotate(new Quaternion(local_dir, desired_angle));
                    }
                }
                //Find rotation between original and local
                System.out.println("original chain: " + _chain.get(0).location(_chain.get(i).position()));
                System.out.println("dir" + dirs[dir]);
                System.out.println("dir : " + dir +  " proj chain chain: " + _chain.get(0).location(local_chain.get(i).position()));

                Quaternion q = new Quaternion(_chain.get(0).location(Vector.projectVectorOnPlane(_chain.get(i).position(), dirs[dir])), _chain.get(0).location(Vector.projectVectorOnPlane(local_chain.get(i).position(), dirs[dir])));
                float ang = Vector.angleBetween(Vector.subtract(_chain.get(i).position(), root_pos), Vector.subtract(local_chain.get(i).position(), root_pos));
                angles[dir] = ang;//q.angle();
                System.out.println(">>>>result: " + q.axis() + "  " + q.angle());
                //if(q.axis().dot(dirs[dir]) < 0)
                //  angles[dir] = (float)(2 * Math.PI - q.angle());
                System.out.println(">>>>result: " + q.axis() + "  " + Math.toDegrees(angles[dir]));

            }
            System.out.println("axis " + axis);
            System.out.println("up " + Math.toDegrees(angles[0]));
            System.out.println("right " + Math.toDegrees(angles[1]));
            System.out.println("down " + Math.toDegrees(angles[2]));
            System.out.println("left " + Math.toDegrees(angles[3]));
            //TODO : Generalize conic constraints
            //with the obtained information generate the global constraint for j_i
            if(angles[2] < Math.PI/2  && angles[0] < Math.PI/2  && angles[1] < Math.PI/2  && angles[3] < Math.PI/2 ) {
                _globalConstraints[i] = new BallAndSocket(angles[3], angles[1], angles[2], angles[0]);
                _globalConstraints[i].setRestRotation(new Quaternion(), up_axis, Vector.multiply(axis, 1));
            }
        }
    }

    protected float _computeAngle(Vector axis, Vector j0, Vector ji, Vector ji_hat){
        //All parameters are defined locally w.r.t j_k
        Vector ji_proj = Vector.projectVectorOnPlane(ji, axis);
        Vector A = Vector.projectVectorOnPlane(Vector.subtract(ji, j0), axis);
        System.out.println("  A : " + A);
        Vector B = Vector.projectVectorOnPlane(ji_hat, axis);
        System.out.println("  B : " + B);
        //Find the intersection between line defined by A and the the circle with radius B
        float radius = B.magnitude();
        float angle = Vector.angleBetween(A, ji_proj);
        System.out.println("  Angle : " + angle);
        float chord_length = (float)(2 * radius * Math.cos(angle));
        Vector C = Vector.add(ji_proj, Vector.multiply(A.normalize(null), - chord_length));
        System.out.println("  C : " + C);
        float desired_angle;
        if(radius  <= j0.magnitude()){
            //C =  Vector.add(Vector.multiply(Vector.subtract(C, ji_proj),0.5f), B).normalize(null);
            C =  Vector.cross(axis, A, null).normalize(null);
            C.multiply(radius);
            desired_angle = Vector.angleBetween(B,C);
            Vector cross = Vector.cross(B, C, null);
            if(Vector.dot(cross, axis) < 0){
                desired_angle = (float) Math.PI - desired_angle;
            }
        } else {
            desired_angle = Vector.angleBetween(B, C);
            Vector cross = Vector.cross(B, C, null);
            if (Vector.dot(cross, axis) < 0) {
                desired_angle = (float) (2 * Math.PI - desired_angle);
            }
        }
        System.out.println("ANG : " + Math.min(desired_angle, (float)(0.99 * Math.PI)));
        return Math.min(desired_angle, (float)(0.8 * Math.PI)); //Rotation near to PI generates Singularity problems
    }
}
