package nub.ik.solver.trik.heuristic;

import javafx.util.Pair;
import nub.core.constraint.Constraint;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.NodeState;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.HashMap;

public class FinalHeuristic extends Heuristic {
    /**
     * The idea of this heuristics is to apply and interchange popular CCD Step along with Triangulation step. Here most of the work is done by the first joints,
     * hence the obtained solution could not seem natural when working with unconstrained chains. For this purposes a smoothing stage is required in which each
     * joint will try to do and delegate work.
     */

    HashMap<String, Pair<Vector, Vector>> vectors = new HashMap<>();
    protected float _proximityWeight = 1f;
    protected float _lengthWeight = 1f;
    protected int _smoothingIterations = 10;
    protected Quaternion[] _initialRotations;
    protected boolean _checkHinge = true;

    public void checkHinge(boolean check){
        _checkHinge = check;
    }

    public FinalHeuristic(Context context) {
        super(context);
        _initialRotations = new Quaternion[_context.chain().size()];
        _log = false;
    }




    @Override
    public void prepare() {
        //Update cache of usable chain
        _smoothAngle = (float)Math.toRadians(40);
        NodeInformation._updateCache(_context.usableChainInformation());
        for(int i = 0; i < _initialRotations.length; i++){
            _initialRotations[i] = _context.usableChainInformation().get(i).node().rotation().get();
        }
    }

    protected boolean _log;
    protected int _times = 2;

    protected float _delegationParameter = 0;
    protected float _iterationParameter = 1;


    @Override
    public void applyActions(int i) {
        if(_context.debug()) vectors.clear();
        Vector target = _context.worldTarget().position();

        if(_smooth || _context.enableDelegation()){
            _context.usableChainInformation().get(i + 1).updateCacheUsingReference();
            //Apply delegation
            _delegationParameter = _context.delegationAtJoint(i);
            //If smooth is enable calculate smooth parameters
            int currentIteration = _context._currentIteration();
            if(_context.singleStep()){
                currentIteration = currentIteration / _context.chain().size();
            }

            _iterationParameter = 1f/Math.max(_smoothingIterations - currentIteration, 1);

            if(_log){
                System.out.println("Delegation parameter : " + _delegationParameter);
                System.out.println("Iteration parameter : " + _iterationParameter);
                System.out.println("t : " + target + "mag" + target.magnitude());
            }

            float k = Math.min(1,_delegationParameter);
            k += (1.f - k) * _iterationParameter;
            _delegationParameter = k;
            float remaining_work = 1 - k;
            if(_log)System.out.println("Final Delegation parameter : " + _delegationParameter);

            Vector t = Vector.subtract(target, _context.usableChainInformation().get(0).positionCache());
            Vector eff = Vector.subtract(_context.endEffectorInformation().positionCache(), _context.usableChainInformation().get(0).positionCache());
            float t_m = t.magnitude(), eff_m = eff.magnitude();

            float length_diff = t_m - eff_m;
            Quaternion rot_diff = new Quaternion(eff, t);

            float smooth_length = t_m - length_diff * remaining_work;
            Quaternion smooth_rot = new Quaternion(rot_diff.axis(), rot_diff.angle() * k);
            smooth_rot = Quaternion.slerp(new Quaternion(rot_diff.axis(), 0), rot_diff, k);



            Vector error = Vector.subtract(eff, t);
            Vector smooth_error = Vector.multiply(error,remaining_work);
            t.add(smooth_error);


            t = smooth_rot.rotate(eff);
            t.normalize();
            t.multiply(smooth_length);

            //Use TRIK heuristic
            /*Vector v = Vector.subtract(_context.usableChainInformation().get(i).positionCache(), _context.worldTarget().position());
            v.subtract(_context.usableChainInformation().get(i+1).positionCache());
            v.add(_context.endEffectorInformation().positionCache());
            v.normalize();
            v.multiply(-Vector.subtract(_context.usableChainInformation().get(i).positionCache(), _context.usableChainInformation().get(i + 1).positionCache()).magnitude());
            Vector t_des = Vector.subtract(_context.endEffectorInformation().positionCache(), _context.usableChainInformation().get(i + 1).positionCache());
            t_des.add(v);
            t_des.add(_context.usableChainInformation().get(i).positionCache());
            target = t_des;*/

            Vector eff_to_j_i1 = Vector.subtract(_context.endEffectorInformation().positionCache(), _context.usableChainInformation().get(i + 1).positionCache());
            float eff_to_j_i1_m = eff_to_j_i1.magnitude();
            Vector j_i1_proj = Vector.subtract(_context.worldTarget().position(), eff_to_j_i1);
            Vector j_i_proj = Vector.subtract(j_i1_proj, _context.usableChainInformation().get(i).positionCache());
            j_i_proj.normalize();
            j_i_proj.multiply(Vector.distance(_context.usableChainInformation().get(i).positionCache(), _context.usableChainInformation().get(i + 1).positionCache()));
            j_i_proj.add(_context.usableChainInformation().get(i).positionCache());

            Vector aux = Vector.subtract(target, j_i_proj);
            //aux = eff_to_j_i1;
            aux.normalize();
            aux.multiply(eff_to_j_i1_m);
            target = Vector.add(j_i_proj, aux);

            vectors.put("j_i_proj ", new Pair<>(_context.usableChainInformation().get(i).positionCache(), j_i_proj));
            vectors.put("j_i1_proj ", new Pair<>(_context.usableChainInformation().get(i).positionCache(), j_i1_proj));
            vectors.put("eff_ji1 ", new Pair<>(_context.usableChainInformation().get(i + 1).positionCache(), _context.endEffectorInformation().positionCache()));

            if(_log) {
                System.out.println(" t_m " + t_m + " p0" + _context.usableChainInformation().get(0).positionCache() + " rem " + remaining_work + " k " + k);
                System.out.println(" t : " + target + "mag" + target.magnitude());
                System.out.println("smooth t : " + target + "mag" + target.magnitude());
                System.out.println("J_i pos : " + _context.usableChainInformation().get(i).positionCache() + " vs " + _context.usableChainInformation().get(i).node().position());
                System.out.println("J_i1 pos : " + _context.usableChainInformation().get(i + 1).positionCache() + " vs " + _context.usableChainInformation().get(i + 1).node().position());
            }

            //target = Vector.add(t, _context.chainInformation().get(0).positionCache());


            if(_log) {
                System.out.println("delegation factor : " + k);
                System.out.println("Error : " + error);
                System.out.println("Error covered by current joint: " + smooth_error);
                System.out.println("smooth t : " + target + "mag" + target.magnitude());
                System.out.println("smooth t : " + target + "mag" + target.magnitude());
                System.out.println("Eff length " + eff_m + " target length " + t_m + " t des length " + smooth_length);
            }

            if(_context.debug()){
                vectors.put("st ", new Pair<>(target, _context.endEffectorInformation().positionCache()));
                vectors.put("t_des ", new Pair<>(_context.usableChainInformation().get(0).positionCache(), target));
            }
        }



        NodeInformation j_i = _context.usableChainInformation().get(i);
        Vector eff_wrt_j_i = j_i.locationWithCache(_context.endEffectorInformation().positionCache());
        Vector target_wrt_j_i = j_i.locationWithCache(target);

        if(_log) {
            System.out.println("*-*-*-*-*-*-*-*-*-*");
            System.out.println("On i : " + i);
        }

        if(i == _context.last() - 1){
            Quaternion q_i = applyCCD(i, j_i, eff_wrt_j_i, target_wrt_j_i, true);
            if(_smooth || _context.enableDelegation()) {
                q_i = new Quaternion(q_i.axis(), q_i.angle() * _delegationParameter);
                if(_smooth) q_i = _clampRotation(j_i.node().rotation(), _initialRotations[i], Quaternion.compose(j_i.node().rotation(), q_i), _smoothAngle); //clamp
            }
            j_i.rotateAndUpdateCache(q_i, false, _context.endEffectorInformation()); //Apply local rotation //TODO: Enable constraint?

            if(_context.direction()) {
                float posError = _context.positionError(_context.endEffectorInformation(), _context.worldTarget());
                float c_k = (float) Math.floor(posError / _context.positionWeight());
                float max_dist = (c_k + 1) * _context.positionWeight() - posError;
                float radius = Vector.distance(_context.worldTarget().position(), j_i.positionCache());
                //find max theta allowed
                float max_theta = (float) Math.acos(Math.max(Math.min(1 - (max_dist * max_dist) / (2 * radius * radius), 1), - 1)) * 0.8f;
                j_i.rotateAndUpdateCache(applyOrientationalCCD(i, max_theta), false, _context.endEffectorInformation());
            }

            return;
        }

        NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
        j_i1.updateCacheUsingReference();
        Vector eff_wrt_j_i1 = j_i1.locationWithCache(_context.endEffectorInformation().positionCache());
        Vector target_wrt_j_i1 = j_i1.locationWithCache(target);
        //Find the two solutions of the triangulation problem on joint j_i1
        Solution[] solutions;
        solutions = applyTriangulation(i, j_i, j_i1, eff_wrt_j_i1, target_wrt_j_i1, _checkHinge);

        //if(!_smooth) solutions = applyTriangulation(i, j_i, j_i1, eff_wrt_j_i1, target_wrt_j_i1, true);
        //else solutions = new Solution[]{new Solution(new Quaternion(), 0)};

        //Keep original State of J_i and J_i1
        NodeInformation endEffector =_context.endEffectorInformation();

        NodeState initial_j_i = new NodeState(j_i);
        NodeState initial_j_i1 = new NodeState(j_i1);
        NodeState initial_eff = new NodeState(endEffector);

        NodeState[] final_j_i = new NodeState[solutions.length];
        NodeState[] final_j_i1 = new NodeState[solutions.length];
        NodeState[] final_eff = new NodeState[solutions.length];

        int best = 0; // keep track of best solution
        float best_dist = Float.MAX_VALUE, best_angle = Float.MAX_VALUE;
        for(int s = 0; s < solutions.length; s++){
            float a  = 0; //amount or rotation applied
            j_i1.updateCacheUsingReference();
            //Apply solution find by triangulation
            j_i1.rotateAndUpdateCache(solutions[s].quaternion(), false, endEffector);

            if(!(j_i.node().constraint() instanceof Hinge)) {
                //Quaternion tw = applyCCDTwist(j_i, j_i1, j_i.locationWithCache(endEffector.positionCache()), j_i.locationWithCache(target), (float) Math.PI);
                //j_i.rotateAndUpdateCache(tw, false, endEffector);
            }


            //Apply CCD t times (best local action if joint rotation constraints are quite different)
            j_i.rotateAndUpdateCache(applyCCD(i,j_i,j_i.locationWithCache(endEffector.positionCache()), j_i.locationWithCache(target), true), true, endEffector);
            //Apply twisting if possible


            for(int t = 0; t < _times; t++){
                j_i1.updateCacheUsingReference();
                Quaternion q_i1 = applyCCD(i+1,j_i1,j_i1.locationWithCache(endEffector.positionCache()), j_i1.locationWithCache(target), true);
                j_i1.rotateAndUpdateCache(q_i1, false, endEffector);
                Quaternion q_i = applyCCD(i, j_i,j_i.locationWithCache(endEffector.positionCache()), j_i.locationWithCache(target), true);
                j_i.rotateAndUpdateCache(q_i, false, endEffector);
            }
            j_i1.updateCacheUsingReference();

            if(_context.debug()) {
                vectors.put("zi " + (s + 1), new Pair<>(j_i.positionCache().get(), j_i1.positionCache().get()));
                vectors.put("zf " + (s + 1), new Pair<>(j_i1.positionCache().get(), endEffector.positionCache().get()));
            }

            if(_context.direction()) {
                float posError = _context.positionError(endEffector, _context.worldTarget());
                float c_k = (float) Math.floor(posError / _context.positionWeight());
                float max_dist = (c_k + 1) * _context.positionWeight() - posError;
                float radius = Vector.distance(_context.worldTarget().position(), j_i1.positionCache());
                //find max theta allowed
                float max_theta = (float) Math.acos(Math.max(Math.min(1 - (max_dist * max_dist) / (2 * radius * radius), 1), - 1)) * 0.5f;

                j_i1.rotateAndUpdateCache(applyOrientationalCCD(i + 1, max_theta), false, endEffector);
            }

            //store state in final vector
            final_j_i[s] = new NodeState(j_i);
            final_j_i1[s] = new NodeState(j_i1);
            final_eff[s] = new NodeState(endEffector);

            a = 0.5f * Math.abs(_context.quaternionDistance(_initialRotations[i], final_j_i[s].rotation()) + _context.quaternionDistance(_initialRotations[i + 1], final_j_i1[s].rotation()));
            a += 0.5f * Math.abs(_context.quaternionDistance(_initialRotations[i], final_j_i[s].rotation()) - _context.quaternionDistance(_initialRotations[i + 1], final_j_i1[s].rotation()));

            if(_log) {
                System.out.println("---> a : " + a);
                System.out.println("initial j_i :" +  _initialRotations[i].axis() + "a " + _initialRotations[i].angle() + "final " + final_j_i[s].rotation().axis() + " a " + final_j_i[s].rotation().angle());
                System.out.println("initial j_i :" +  _initialRotations[i]._quaternion[0] + " , " + _initialRotations[i]._quaternion[1] + " , " + _initialRotations[i]._quaternion[2] + " , " + _initialRotations[i]._quaternion[3] );

                System.out.println("initial j_i1 :" +  _initialRotations[i+1].axis() + "a " + _initialRotations[i+1].angle()  + "final " + final_j_i1[s].rotation().axis() + " a " + final_j_i1[s].rotation().angle());

                System.out.println("---> sol : " + (s + 1) + "work by angle 1 " + _context.quaternionDistance(_initialRotations[i], final_j_i[s].rotation()));
                System.out.println("---> sol : " + (s + 1) + "work by angle 2 " + _context.quaternionDistance(_initialRotations[i + 1], final_j_i1[s].rotation()));
            }


            float dist = _context.error(endEffector, _context.worldTarget());


            float desired_length = Vector.distance(target, _context.usableChain().get(0).position());
            float current_length = Vector.distance(final_eff[s].position(), _context.usableChain().get(0).position());
            float length_distance = Math.abs(desired_length - current_length);
            length_distance = (float) Math.min(Math.pow(length_distance / (_context.avgLength() * _context.chain().size()), 2), 1); //distance normalized

            if(_log) {
                System.out.println("---> length distance : " + length_distance);
                System.out.println("---> dist : " + dist);
                System.out.println("---> constraint error : " + solutions[s].value());
            }

            if(_log)System.out.println("iteration_ param " + _iterationParameter);

            //if(_iterationParameter < 1) dist = solutions.get(s).getValue();
            dist =  dist + 0.1f*a;// + solutions[s].value();// + length_distance * _lengthWeight;
            //dist = _iterationParameter * dist + solutions[s].value() + a * 0.5f;// + length_distance * _lengthWeight;


            if(dist < best_dist){
                best_dist = dist;
                best = s;
            }
            //reset state
            j_i.setCache(initial_j_i.position().get(), initial_j_i.orientation().get());
            j_i.node().setRotation(initial_j_i.rotation().get());
            j_i1.setCache(initial_j_i1.position().get(), initial_j_i1.orientation().get());
            j_i1.node().setRotation(initial_j_i1.rotation().get());
            endEffector.setCache(initial_eff.position().get(), initial_eff.orientation().get());

            if(_context.debug()) {
                vectors.put("sol " + (s + 1), new Pair<>(final_j_i[s].position().get(), final_j_i1[s].position().get()));
                vectors.put("sol " + (s + 1) + " d : " + String.format("%.3f", dist), new Pair<>(final_j_i1[s].position().get(), final_eff[s].position().get()));
            }
        }

        if(_log){
            System.out.println("best i :" + final_j_i[best].rotation().axis() + " a : " + final_j_i[best].rotation().angle());
            System.out.println("best i + 1 :" + final_j_i1[best].rotation().axis() + " a : " + final_j_i1[best].rotation().angle());
            System.out.println("best i :" + j_i.node().rotation().axis() + " a : " + j_i.node().rotation().angle());
            System.out.println("best i + 1 :" + j_i1.node().rotation().axis() + " a : " + j_i1.node().rotation().angle());

        }

        //Apply best solution and smooth if required
        if(_smooth){
            applySmoothing(i, final_j_i[best].rotation(), final_j_i1[best].rotation());
        }else {
            j_i.setCache(final_j_i[best].position().get(), final_j_i[best].orientation().get());
            Constraint c_i = j_i.node().constraint();
            j_i.node().setConstraint(null);
            j_i.node().setRotation(final_j_i[best].rotation().get());
            j_i.node().setConstraint(c_i);
            j_i1.setCache(final_j_i1[best].position().get(), final_j_i1[best].orientation().get());
            Constraint c_i1 = j_i1.node().constraint();
            j_i1.node().setConstraint(null);
            j_i1.node().setRotation(final_j_i1[best].rotation().get());
            j_i1.node().setConstraint(c_i1);
            endEffector.setCache(final_eff[best].position().get(), final_eff[best].orientation().get());
        }
    }

    protected float smoothDistance(Vector v, Vector u){
        float dist = Vector.distance(u, v);
        dist = Math.min(dist / (_context.avgLength() * _context.chain().size()), 0.99f); //normalize distance
        return 1 - dist;
    }

    protected void applySmoothing(int i, Quaternion final_j_i, Quaternion final_j_i1){
        //Apply best solution and smooth if required
        if(_smooth){
            NodeInformation j_i = _context.usableChainInformation().get(i);
            NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
            NodeInformation endEffector = _context.endEffectorInformation();

            //Apply smoothing based on distance to target
            //float dist = Vector.distance(endEffector.positionCache(), _context.worldTarget().position());
            //dist = Math.min(dist / (_context.avgLength() * _context.chain().size()), 0.8f);
            //float w = 1 - dist;
            //w = 1;
            //Apply delegation
            //float k = _context.delegationAtJoint(i);
            //k = k/Math.max(1, _smoothingIterations - _context._currentIteration());
            //k += (_context._currentIteration())*(1.f - k)/_smoothingIterations;
            //k = Math.min(1,k);
            //w = w * k;

            //if(_log) System.out.println("i : " +  i +  " w " + w + " delegation factor " + k);

            //Quaternion q_i = Quaternion.compose(j_i.node().rotation().inverse(), final_j_i);
            //q_i = new Quaternion(q_i.axis(), q_i.angle() * w);
            //Quaternion q_i1 = Quaternion.compose(j_i1.node().rotation().inverse(), final_j_i1);
            //q_i1 = new Quaternion(q_i1.axis(), q_i1.angle() * w);
            //clamp
            System.out.println("ENTRA!!!!");
            Quaternion q_i = _clampRotation(j_i.node().rotation(), _initialRotations[i], final_j_i, _smoothAngle);
            Quaternion q_i1 = _clampRotation(j_i1.node().rotation(), _initialRotations[i + 1], final_j_i1, _smoothAngle);

            //q_i = Quaternion.compose(j_i.node().rotation().inverse(), q_i);
            //q_i.normalize();
            //q_i1 = Quaternion.compose(j_i1.node().rotation().inverse(), q_i1);
            //q_i1.normalize();

            j_i.rotateAndUpdateCache(q_i, true, endEffector);
            j_i1.updateCacheUsingReference();
            j_i1.rotateAndUpdateCache(q_i1, true, endEffector);
        }
    }



    protected class Solution{
        protected Quaternion _quaternion;
        protected float _value;

        protected Solution(Quaternion q, float v){
            _quaternion = q;
            _value = v;
        }

        protected void setQuaternion(Quaternion q){
            _quaternion = q;
        }

        protected void setValue(float v){
            _value = v;
        }

        protected Quaternion quaternion(){
            return _quaternion;
        }

        protected float value(){
            return _value;
        }
    }

    protected Solution[] applyTriangulation(int i , NodeInformation j_i, NodeInformation j_i1, Vector endEffector, Vector target, boolean checkHinge){
        //checkHinge = true;
        Hinge h_i1 = null;
        Vector v_i = j_i1.locationWithCache(j_i.positionCache());
        Vector normal;
        //In this case we apply triangulation over j_i1
        if(j_i1.node().constraint() instanceof Hinge){
            //Project endEffector to lie on the plane defined by the axis of rotation
            h_i1 = ((Hinge) j_i1.node().constraint());
            //1. find rotation axis
            normal = h_i1.orientation().rotate(new Vector(0,0,1));
            if(_log) System.out.println("normal " + normal);
            normal = j_i1.node().rotation().inverse().rotate(normal);
            normal.normalize();
            //2. project target and end effector
            v_i = Vector.projectVectorOnPlane(v_i, normal);
            endEffector = Vector.projectVectorOnPlane(endEffector, normal);
            target = Vector.projectVectorOnPlane(target, normal);
        } else{
            normal = Vector.cross(endEffector, target, null);
            if(normal.squaredNorm() < 0.0001f){
                normal = Vector.cross(target, v_i, null);
            }
            //pick any vector if all are collinear
            if(normal.squaredNorm() < 0.0001f){
                normal = target.orthogonalVector();
            }
            normal.normalize();
        }
        //Find the two solutions of the triangulation problem assuming no constraints
        Vector a = v_i;
        Vector a_neg = Vector.multiply(a, -1);
        Vector b = endEffector;
        Vector c = Vector.subtract(target, a);
        float a_mag = a.magnitude(), b_mag = b.magnitude(), c_mag = c.magnitude();

        if(_log) {
            System.out.println("Vec a : " + a + "mag" + a.magnitude());
            System.out.println("Vec b : " + b + "mag" + b.magnitude());
            System.out.println("Vec c : " + c + "mag" + c.magnitude());
        }

        float angle = Math.min(Math.max(Vector.dot(a, b) / (a_mag * b_mag), -1), 1);
        angle = (float) (Math.acos(angle));
        if(_log) {
            System.out.println("dot a ,b  : " + Vector.dot(a, b));
            System.out.println("a mag * b mag : " + (a_mag * b_mag));
            System.out.println("result : " + Vector.dot(a, b) / (a_mag * b_mag));
            System.out.println("angle : " + Math.toDegrees(angle));
            System.out.println("cross : " + Vector.cross(b, a_neg, null));
            System.out.println("Normal : " + normal);
            System.out.println("Dot : " + Vector.dot(Vector.cross(b, a_neg, null), normal));

        }

        float angle_1, angle_2;

        if(Vector.dot(Vector.cross(b, a_neg, null), normal) < 0){
            angle = -angle;
        }

        //Find limits centered at angle
        float max = (float)Math.PI;
        float min = -max;
        if(_log) {
            System.out.println("--- max limit : " + Math.toDegrees(max));
            System.out.println("--- min limit : " + Math.toDegrees(min));
        }

        if (a_mag + b_mag <= c_mag) {
            if(_log) System.out.println("---case1 : extend chain!!! ");
            //Chain must be extended as much as possible
            if(_log) {
                System.out.println("current : " + (Math.toDegrees(angle)));
                System.out.println("expected : " + (Math.toDegrees(Math.PI)));
            }

            if(angle == 0){
                angle_1 = (float) (Math.PI);
                angle_2 = -(float) (Math.PI);
            }else {
                angle_1 = Math.signum(angle) * (float) (Math.PI) - angle;
                angle_2 = (float)(-Math.signum(angle_1) * 2*Math.PI + angle_1);
            }
        } else if (c_mag < Math.abs(a_mag - b_mag)) {
            if(_log) System.out.println("---case2 : contract chain!!! on node : ");
            //Chain must be contracted as much as possible
            if(_log) {
                System.out.println("current : " + (Math.toDegrees(angle)));
                System.out.println("expected : " + 0);
            }

            angle_1 = -angle;
            angle_2 = (float)(-Math.signum(angle_1) * 2*Math.PI + angle_1);
        } else {
            //Apply law of cosines
            float current = angle;

            float expected = Math.min(Math.max(-(c_mag * c_mag - a_mag * a_mag - b_mag * b_mag) / (2f * a_mag * b_mag), -1), 1);
            expected = (float) (Math.acos(expected));

            if(_log) {
                System.out.println("current : " + Math.toDegrees(current));
                System.out.println("expected : " + Math.toDegrees(expected));
            }

            float sign = current != 0 ? Math.signum(current) : 1;
            angle_1 = sign * expected - current;
            if(Math.abs(current) < Math.abs(expected)){
                angle_2 = current - Math.signum(current) * expected;
            } else{
                angle_2 = sign * (float)(2 * Math.PI - expected) - current;
            }

            if(_log) {
                System.out.println("--> angle 1 : " + Math.toDegrees(angle_1));
                System.out.println("--> angle 2: " + Math.toDegrees(angle_2));
            }
        }
        if(_log) {
            System.out.println("--> angle 1 : " + Math.toDegrees(angle_1));
            System.out.println("--> angle 2 : " + Math.toDegrees(angle_2));
        }

        //Constraint the angles according to the joint limits (prefer the solution with least damping)
        float constrained_angle_1 = Math.min(max, Math.max(min,angle_1));
        float constrained_angle_2 = Math.min(max, Math.max(min, angle_2));;

        if(_log) {
            System.out.println("--> constrained angle 1 : " + Math.toDegrees(constrained_angle_1));
            System.out.println("--> constrained angle 2 : " + Math.toDegrees(constrained_angle_2));
            System.out.println("--> ratio angle 1 : " + (Math.abs(constrained_angle_1) + 1)/(Math.abs(angle_1) + 1));
            System.out.println("--> ration angle 2 : " + (Math.abs(constrained_angle_2) + 1)/(Math.abs(angle_2) + 1));

        }

        Solution[] deltas = new Solution[2];
        deltas[0] = new Solution(new Quaternion(normal, constrained_angle_1), 1 - (Math.abs(constrained_angle_1) + 1)/(Math.abs(angle_1) + 1));
        deltas[1] = new Solution(new Quaternion(normal, constrained_angle_2), 1 - (Math.abs(constrained_angle_2) + 1)/(Math.abs(angle_2) + 1));
        for(Solution delta : deltas){
            if(_smooth){
                //delta.setQuaternion(_clampRotation(j_i1.node().rotation(), _initialRotations[i + 1], Quaternion.compose(j_i1.node().rotation(),delta.quaternion()), _smoothAngle));
            }
            if(j_i1.node().constraint() != null){
                delta.setQuaternion(j_i1.node().constraint().constrainRotation(delta.quaternion(), j_i1.node()));
            }
            if(_log) {
                System.out.println("--> delta : " + delta.quaternion().axis() + " angle : "+ Math.toDegrees(delta.quaternion().angle()));
            }
            delta.quaternion().normalize();
        }
        return deltas;
    }

    //Try to approach to target final position by means of twisting
    protected Quaternion applyCCDTwist(NodeInformation j_i, NodeInformation j_i1, Vector endEffector, Vector target, float maxAngle){
        Vector j_i_to_eff_proj, j_i_to_target_proj;
        Vector tw = j_i1.node().translation(); // w.r.t j_i
        //Project the given vectors in the plane given by twist axis
        try {
            j_i_to_target_proj = Vector.projectVectorOnPlane(target, tw);
            j_i_to_eff_proj = Vector.projectVectorOnPlane(endEffector, tw);
        } catch(Exception e){
            return new Quaternion(tw, 0);
        }

        //Perform this operation only when Projected Vectors have not a despicable length
        if (j_i_to_target_proj.magnitude() < 0.3 * target.magnitude() && j_i_to_eff_proj.magnitude() < 0.3 * endEffector.magnitude()) {
            return new Quaternion(tw, 0);
        }

        //Find the angle between projected vectors
        float angle = Vector.angleBetween(j_i_to_eff_proj, j_i_to_target_proj);
        //clamp angle
        angle = Math.min(angle, maxAngle);
        if(Vector.cross(j_i_to_eff_proj, j_i_to_target_proj, null).dot(tw) <0)
            angle *= -1;

        Quaternion twist = new Quaternion(tw, angle);
        if(_smooth){
            twist = _clampRotation(twist, _smoothAngle);
        }
        twist.normalize();
        return twist;
    }


    protected Quaternion applyCCD(int i, NodeInformation j_i, Vector endEffector, Vector target, boolean checkHinge){
        Vector p = endEffector;
        Vector q = target;
        if(checkHinge && j_i.node().constraint() != null && j_i.node().constraint() instanceof Hinge) {
            Hinge h = (Hinge) j_i.node().constraint();
            Quaternion quat = Quaternion.compose(j_i.node().rotation().inverse(), h.idleRotation());
            Vector tw = h.restRotation().rotate(new Vector(0, 0, 1));
            tw = quat.rotate(tw);
            //Project b & c on the plane of rot
            p = Vector.projectVectorOnPlane(p, tw);
            q = Vector.projectVectorOnPlane(q, tw);
        }
        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);
        if(_smooth){
            //delta =_clampRotation(j_i.node().rotation(), _initialRotations[i], Quaternion.compose(j_i.node().rotation(), delta), _smoothAngle);
        }
        if(j_i.node().constraint() != null){
            delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
        }
        delta.normalize();
        return delta;
    }

    protected Quaternion applyOrientationalCCD(int i, float maxAngle){
        NodeInformation j_i = _context.usableChainInformation().get(i);
        Quaternion O_i = j_i.orientationCache();
        Quaternion O_i_inv = O_i.inverse();
        Quaternion O_eff = _context.usableChainInformation().get(_context.last()).orientationCache();
        Quaternion target = _context.worldTarget().orientation();
        Quaternion O_i1_to_eff = Quaternion.compose(O_i.inverse(), O_eff);
        O_i1_to_eff.normalize();
        Quaternion delta = Quaternion.compose(O_i_inv, target);
        delta.normalize();
        delta.compose(O_i1_to_eff.inverse());
        delta.normalize();
        if(j_i.node().constraint() != null){
            delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
            delta.normalize();
        }
        //clamp rotation
        delta = _clampRotation(delta, maxAngle);
        return delta;
    }



    @Override
    public NodeInformation[] nodesToModify(int i) {
        //return new NodeInformation[]{_context.usableChainInformation().get(i - 1), _context.usableChainInformation().get(i)};
        return null;
    }

    protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
        float angle = rotation.angle();
        float angleVal = Math.abs(angle);
        float angleSign = Math.signum(angle);
        Vector axis = rotation.axis();
        if(Math.abs(angle) > Math.PI){
            axis.multiply(-1);
            angle = angleSign * (float)(2 * Math.PI - angleVal);
        }
        if (Math.abs(angle) > maxAngle) {
            rotation = new Quaternion(axis, angleSign * maxAngle);
        }
        return rotation;
    }

    protected static Quaternion _clampRotation(Quaternion q_cur, Quaternion q_i, Quaternion q_f, float maxAngle) {
        Quaternion diff = Quaternion.compose(q_i.inverse(), q_f);
        diff.normalize();

        float angle = diff.angle();
        float angleVal = Math.abs(angle);
        float angleSign = Math.signum(angle);
        Vector axis = diff.axis();

        if(Math.abs(angle) > Math.PI){
            axis.multiply(-1);
            angle = angleSign * (float)(2 * Math.PI - angleVal);
        }

        if (Math.abs(angle) > maxAngle){
            diff = new Quaternion(axis, angleSign * maxAngle);
        }
        Quaternion delta = Quaternion.compose(q_cur.inverse(), q_i);
        delta.compose(diff);
        return delta;
    }


    public void drawVectors(Scene scene){
        if(!_context.debug()) return;
        PGraphics pg = scene.context();
        int i = 0;
        for(String key : vectors.keySet()){
            Pair<Vector, Vector> pair = vectors.get(key);
            if(key.equals("normal")){
                Vector n = Vector.subtract(pair.getValue(), pair.getKey());
                Vector a1 =  Vector.orthogonalVector(n);
                a1.normalize();
                a1.multiply(n.magnitude() * 9);
                Vector a2 =  Vector.cross(n, a1, null);
                a2.normalize();
                a2.multiply(n.magnitude() * 8);
                pg.fill(255,255,255, 50);
                pg.noStroke();
                pg.beginShape();
                    pg.vertex(a1.x() + a2.x() + pair.getKey().x(), a1.y() + a2.y() + pair.getKey().y(), a1.z() + a2.z() + pair.getKey().z());
                    pg.vertex(a1.x() - a2.x() + pair.getKey().x(), a1.y() - a2.y() + pair.getKey().y(), a1.z() - a2.z() + pair.getKey().z());
                    pg.vertex(-a1.x() -a2.x() + pair.getKey().x(), -a1.y() - a2.y() + pair.getKey().y(), -a1.z() - a2.z() + pair.getKey().z());
                    pg.vertex(-a1.x() + a2.x() + pair.getKey().x(), -a1.y() + a2.y() + pair.getKey().y(), -a1.z() + a2.z() + pair.getKey().z());
                pg.endShape();
            }


            Vector t = scene.screenLocation(pair.getValue());
            pg.noStroke();
            pg.fill(scene.pApplet().noise(1000 + 10*i) * 255, scene.pApplet().noise(80 + 10*i) * 255, scene.pApplet().noise(235 + 10*i) * 255);
            if(Vector.distance(pair.getKey(), pair.getValue()) > 1) {
                scene.drawArrow(pair.getKey(), pair.getValue(), 1.5f);
            }
            pg.noLights();
            scene.beginHUD();
            pg.fill(255,255,255);
            pg.text(key, t.x(), t.y());
            scene.endHUD();
            pg.lights();
            i++;

        }
    }

    public void drawPositionContourMap(Scene scene){
        PGraphics pg = scene.context();
        //Draw as much contours as the number of bones of the structure
        for(int i = 1; i < 5; i++){
            float r = _context.positionWeight() * i;
            pg.pushStyle();
            pg.noStroke();
            pg.noLights();

            pg.pushMatrix();
            scene.applyWorldTransformation(_context.worldTarget());
            pg.fill(255, 0, 0 , PApplet.map(i, 5, 1, 100, 30));
            pg.sphere(r);
            pg.popMatrix();
            pg.popStyle();
        }
    }
}
