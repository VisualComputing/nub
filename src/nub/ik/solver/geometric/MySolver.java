package nub.ik.solver.geometric;

import nub.core.Node;
import nub.core.constraint.BallAndSocket;
import nub.core.constraint.Constraint;
import nub.ik.solver.Solver;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.ArrayList;
import java.util.List;

public class MySolver extends Solver{
    protected List<Node> _chain, _reversed;
    protected List<? extends Node> _original;
    protected BallAndSocket[] _globalConstraints;
    protected float _current = 10e10f, _best = 10e10f;
    protected Node _target;
    protected Node _prevTarget;

    public MySolver(List<? extends Node> chain, Node target){
        super();
        this._original = chain;
        this._chain = FABRIKSolver._copy(chain); //TODO: Move this method
        this._reversed = _reverseChain(_original);
        _generateGlobalConstraints();
    }


    @Override
    protected boolean _iterate() {
        if(_target == null) return true;


        return false;
    }

    @Override
    protected void _update() {

    }

    @Override
    protected boolean _changed() {
        return false;
    }

    @Override
    protected void _reset() {

    }

    @Override
    public float error() {
        return 0;
    }

    @Override
    public void setTarget(Node endEffector, Node target) {

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
    protected void applyLocalRotation(List<? extends Node> current, List<? extends Node> reversed, int i){
        int i_cur = current.size() - 1 - i;
        //Get how much to extend
        Vector j_i_j_0 = Vector.subtract(current.get(i_cur).position(), current.get(0).position());
        Vector j_i_hat_j_0 = Vector.subtract(reversed.get(i).position(), current.get(0).position());
        float desired_extension = j_i_j_0.magnitude();
        float current_extension = j_i_hat_j_0.magnitude();
        float difference = desired_extension - current_extension;
        //distribute the job between remaining Joints and current one
        //TODO: use constraints
        float remaining_posible_extension = 0;
        float local_posible_extension = 0;
        boolean extend = true;
        if(difference > 0){
            remaining_posible_extension = _posibleExtension(reversed, i + 1, reversed.size() - 1);
            local_posible_extension = _posibleExtension(reversed, i-1, i);
        }else{
            extend = false;
            remaining_posible_extension = _posibleContraction(reversed, i + 1, reversed.size() - 1);
            local_posible_extension = _posibleContraction(reversed, i-1, i);
        }
        float extension = local_posible_extension;
        extension *= _localExtension(remaining_posible_extension, local_posible_extension, desired_extension, i);
        //Apply defined extension



    }

    //TODO: Do the same calculation but with constrained structures
    protected float _posibleExtension(List<? extends Node> chain, int init, int end){
        float max = 0;
        for(int i = init + 1; i <= end; i++){
            Vector bone = Vector.subtract(chain.get(i).reference().position(), chain.get(i).position());
            max += bone.magnitude();
        }
        return max;
    }

    //TODO: Do the same calculation but with constrained structures
    protected float _posibleContraction(List<? extends Node> chain, int init, int end){
        return Vector.subtract(chain.get(init).position(), chain.get(end).position()).magnitude();
    }

    //define how to extend/contract the local joint. the idea is to try to make all joint to move the same amount
    protected float _localExtension(float remaining, float local, float desired, int n){
        float avg_extension = (remaining + local)/n;
        if(remaining + local < desired || remaining + avg_extension < desired || avg_extension > local){
            return 1;
        }
        return Math.max(avg_extension,Math.min(local, avg_extension * 1.1f)) / local;
    }

    protected void _applyExtension(List<? extends Node> reversed, int i, float extension){
        Node j_i = reversed.get(i);
        Node j_i1 = reversed.get(i + 1);
        //Do all w.r.t h_i1
        Vector dir = j_i1.location(j_i).get();
        dir.multiply(extension > 0 ? -1 : 1);
        Vector j_root = j_i1.location(reversed.get(0));
        float desired_theta = (float) Math.acos(extension / j_root.magnitude());
        float current_theta = Vector.angleBetween(j_root, dir);
        Vector axis = Vector.cross(j_root, dir, null);
        if(axis.magnitude() < 1e-5) j_root.orthogonalVector();
        float delta = desired_theta - current_theta;
        j_i1.rotate(axis, delta);
        return;
    }

    protected void _applyRotation(List<? extends Node> reversed, int i, Vector root){
        //apply a rotation that approach chain to original root
        Node j_i = reversed.get(i);
        Vector root_hat = j_i.location(reversed.get(0));
        Quaternion desired_rotation = new Quaternion(root_hat, root);
        j_i.rotate(desired_rotation);
    }

    protected static List<Node> _reverseChain(List<? extends Node> chain){
        List<Node> reversed = new ArrayList<>();
        Node reference = chain.get(0).reference();
        if (reference != null) {
            reference = new Node(reference.position().get(), reference.orientation().get(), 1);
        }
        for(int i = chain.size() -1; i >= 0; i--){
            Node joint = chain.get(i);
            Node newJoint = new Node();
            newJoint.setReference(reference);
            newJoint.setPosition(joint.position().get());
            newJoint.setOrientation(joint.orientation().get());
            //newJoint.setConstraint(joint.constraint()); TODO: Invert constraints
            reversed.add(newJoint);
            reference = newJoint;
        }
        return reversed;
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
