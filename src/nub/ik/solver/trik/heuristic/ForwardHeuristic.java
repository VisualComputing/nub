package nub.ik.solver.trik.heuristic;

import nub.core.Node;
import nub.ik.solver.geometric.FABRIKSolver;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.State;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import nub.processing.Scene;

import java.util.ArrayList;
import java.util.List;

public class ForwardHeuristic extends Heuristic{
    /**
     * This heuristic requires to know the previous state of the chain, hence an auxiliary chain with this information is stored.
     * The idea of this heuristics is similar to FABRIK backward phase, where we want to reduce the distance between the goal and the end effector
     * hoping that the position of the joints doesn't change, while most of the motion is done by the joints near the head (base) of the chain.
    * */
    protected List<NodeInformation> _auxiliaryChainInformation;
    protected boolean _smooth = false; //smooth tries to reduce the movement done by each joint, such that the distance from initial one is reduced
    protected float _smoothAngle = (float) Math.toRadians(10);
    protected boolean  _enableTwist = true;
    protected float _maxTwistDirection = (float) Math.toRadians(15), _maxTwistCCD = (float) Math.toRadians(15);

    public ForwardHeuristic(Context context) {
        super(context);
        List<Node> auxiliarChain;
        if(context.debug() && context.chain().get(0).graph() instanceof Scene) {
            auxiliarChain = FABRIKSolver._copy(context.chain(), null, (Scene) context.chain().get(0).graph(), false);
        }
        else {
            auxiliarChain = FABRIKSolver._copy(context.chain(), false);
        }
        _auxiliaryChainInformation = NodeInformation._createInformationList(auxiliarChain, true);
    }

    public void enableSmooth(boolean value){
        _smooth = value;
    }

    public boolean smooth(){
        return _smooth;
    }


    @Override
    public void prepare() {
        int last = _context.last();
        List<NodeInformation> usableChainInformation = _context.usableChainInformation();
        //1. copy the state of the current chain into auxiliary chain
        _context.copyChainState(usableChainInformation, _auxiliaryChainInformation);
        //Step 2. Translate the auxiliary chain to the target position
        //2.1 Update auxiliary root and eff cache
        _auxiliaryChainInformation.get(0).setCache(usableChainInformation.get(0).positionCache(), usableChainInformation.get(0).orientationCache());
        _auxiliaryChainInformation.get(last).setCache(usableChainInformation.get(last).positionCache(), usableChainInformation.get(last).orientationCache());
        //2.2 Do alignment
        _alignToTarget(_auxiliaryChainInformation.get(0), _auxiliaryChainInformation.get(last), _context.worldTarget());
        //2.3 Update auxiliary cache
        NodeInformation._updateCache(_auxiliaryChainInformation);
    }

    protected static float _calculateWeight(float boneLength, float distToDesired){
        float dist = distToDesired - boneLength;
        dist = dist < 0 ? -1f / dist : dist;
        float d_i = dist / boneLength;
        return (float) Math.pow(1.5, -d_i);
    }

    @Override
    public void applyActions(int i) {
        NodeInformation j_i = _context.usableChainInformation().get(i); //must have a parent
        NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
        NodeInformation j_i1_hat = _auxiliaryChainInformation.get(i + 1);
        NodeInformation endEffector = _context.endEffectorInformation();

        Quaternion q1 = _findLocalRotation(j_i1, j_i1_hat, _context.enableWeight());
        if(_smooth) q1 = _clampRotation(q1, _smoothAngle);
        j_i.rotateAndUpdateCache(q1, true, endEffector); //Apply local rotation
        if(_enableTwist) {
            Quaternion q2;
            if(_smooth) q2 = _findTwist(j_i, j_i1, endEffector, _context.worldTarget(), _smoothAngle, _smoothAngle, _context.direction());
            else q2 = _findTwist(j_i, j_i1, endEffector, _context.worldTarget(), _maxTwistCCD, _maxTwistDirection, _context.direction());
            j_i.rotateAndUpdateCache(q2, true, endEffector); //Apply twist rotation
        }
    }



    @Override
    public NodeInformation[] nodesToModify(int i) {
        return new NodeInformation[]{_context.usableChainInformation().get(i)};
    }

    //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUMED THAT THEY ARE UPDATE
    protected Quaternion _findLocalRotation(NodeInformation j_i1, NodeInformation j_i1_hat, boolean enableWeight){
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
            if(_context.singleStep())System.out.println("weight : " + weight);
            delta = new Quaternion(delta.axis(), delta.angle() * weight);
        }
        return delta;
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

    protected void _alignToTarget(NodeInformation root, NodeInformation eff, Node target){ //root has to be updated
        if(_context.direction()) {
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

    protected void _translateToTarget(NodeInformation root, NodeInformation eff, Node target){
        //Find the distance from EFF current position and Target
        Vector diff = Vector.subtract(target.position(), eff.positionCache());
        //express diff w.r.t root reference
        Quaternion ref = Quaternion.compose(root.orientationCache(), root.node().rotation().inverse());
        diff = ref.inverseRotate(diff);
        //Move the root accordingly (disable constraints)
        root.translateAndUpdateCache(diff, false);
    }

    protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle){
        float angle = rotation.angle();
        if(Math.abs(angle) > maxAngle ){
            rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
        }
        return rotation;
    }

}


