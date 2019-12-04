package nub.ik.solver.trik.heuristic;

import nub.core.Node;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class BackwardHeuristic extends Heuristic{
    /**
     * The idea of this heuristics is similar to FABRIK forward phase, where we want to reduce the distance between the goal and the end effector
     * hoping that the position of the joints doesn't change, while most of the motion is done by the joints near the  end effector of the chain.
     * */
    protected boolean _smooth = false; //smooth tries to reduce the movement done by each joint, such that the distance from initial one is reduced
    protected float _smoothAngle = (float) Math.toRadians(10);
    protected boolean  _enableTwist = true;
    protected float _maxTwistDirection = (float) Math.toRadians(15), _maxTwistCCD = (float) Math.toRadians(15);

    public BackwardHeuristic(Context context) {
        super(context);
    }


    @Override
    public void prepare() {
        //Update cache of usable chain
        NodeInformation._updateCache(_context.usableChainInformation());
    }

    @Override
    public void applyActions(int i) {
        NodeInformation j_i = _context.usableChainInformation().get(i);
        NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
        NodeInformation endEffector = _context.endEffectorInformation();
        Quaternion delta = _findLocalRotation(j_i, j_i1, endEffector, _context.worldTarget(), _context.enableWeight());
        if(_smooth) delta = _clampRotation(delta, _smoothAngle);
        if(j_i.node().constraint() != null){
            delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
        }

        if(i == _context.last() - 1){
            j_i.rotateAndUpdateCache(delta, false, endEffector); //Apply local rotation
            return;
        }

        //alpha = r_{i+1}^-1 q1^-1 r_{i+1}
        Quaternion alpha = Quaternion.compose(delta, j_i1.node().rotation()).inverse();
        alpha.compose(j_i1.node().rotation());

        if(j_i1.node().constraint() != null){
            alpha = j_i.node().constraint().constrainRotation(alpha, j_i1.node());
            delta = Quaternion.compose(j_i1.node().rotation(), alpha.inverse());
            delta.compose(j_i1.node().rotation().inverse());
        }
        j_i.rotateAndUpdateCache(delta, false, endEffector); //Apply local rotation
        j_i1.updateCacheUsingReference();
        j_i1.rotateAndUpdateCache(alpha, false, endEffector);
        if(_enableTwist) {
            Quaternion q2;
            if(_smooth) q2 = _findTwist(j_i, j_i1, endEffector, _context.worldTarget(), _smoothAngle, _smoothAngle, _context.direction());
            else q2 = _findTwist(j_i, j_i1, endEffector, _context.worldTarget(), _maxTwistCCD, _maxTwistDirection, _context.direction());
            j_i.rotateAndUpdateCache(q2, true, endEffector, j_i1); //Apply twist rotation
        }
    }

    @Override
    public NodeInformation[] nodesToModify(int i) {
        return new NodeInformation[]{_context.usableChainInformation().get(i - 1), _context.usableChainInformation().get(i)};
    }


    //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUMED THAT THEY ARE UPDATE
    protected static float _calculateWeight(float boneLength, float distToDesired){
        float dist = distToDesired - boneLength;
        dist = dist < 0 ? -1f / dist : dist;
        float d_i = dist / boneLength;
        return (float) Math.pow(1.5, -d_i);
    }

    protected Quaternion _findLocalRotation(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, boolean enableWeight){
        //1. find the diff between end effector and target
        Vector difference = Vector.subtract(target.position(), endEffector.positionCache());
        //2. add the difference to j_i1
        Vector j_i1_hat = Vector.add(j_i1.positionCache(), difference);
        //Define how much rotate j_i in order to align j_i1 with j_i1_hat
        Vector p = j_i1.node().translation();
        //Find j_i1_hat w.r.t j_i
        Vector q = j_i.locationWithCache(j_i1_hat);
        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);
        float weight = 1;
        if(enableWeight) {
            weight = _calculateWeight(p.magnitude(), q.magnitude());
            if(_context.singleStep())System.out.println("weight : " + weight);
            delta = new Quaternion(delta.axis(), delta.angle() * weight);
        }
        return delta;
    }

    protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle){
        float angle = rotation.angle();
        if(Math.abs(angle) > maxAngle ){
            rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
        }
        return rotation;
    }


    protected Quaternion _findTwisting(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, float maxAngle){ //Math.toRadians(15)
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
    protected Quaternion _findCCDTwist(NodeInformation j_i, NodeInformation j_i1, NodeInformation eff, Node target, float maxAngle){ //(float) Math.toRadians(20)
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


    protected Quaternion _findTwist(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, float maxT1, float maxT2, boolean direction){
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

}
