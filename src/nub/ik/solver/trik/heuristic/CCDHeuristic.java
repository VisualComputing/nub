package nub.ik.solver.trik.heuristic;

import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class CCDHeuristic extends Heuristic{
    /**
     * The idea of this heuristics is to apply popular CCD Step. Here most of the work is done by the last joint and as them could move
     * that what is truly required the final pose of the chain will not be perceived as a natural pose.
     * */
    public CCDHeuristic(Context context) {
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
        NodeInformation endEffector = _context.endEffectorInformation();
        Vector p = j_i.locationWithCache(_context.endEffectorInformation().positionCache());
        Vector q = j_i.locationWithCache(_context.worldTarget().position());

        if(j_i.node().constraint() instanceof Hinge){
            Hinge h = (Hinge) j_i.node().constraint();
            Quaternion quat = Quaternion.compose(j_i.node().rotation().inverse(), h.idleRotation());
            Vector tw = h.restRotation().rotate(new Vector(0,0,1));
            tw = quat.rotate(tw);
            //Project b & c on the plane of rot
            p = Vector.projectVectorOnPlane(p, tw);
            q = Vector.projectVectorOnPlane(q, tw);
        }

        //Apply desired rotation removing twist component
        Quaternion delta = new Quaternion(p, q);
        delta.normalize();
        float weight = 1;
        /*if(_context.enableWeight()) {
            weight = _calculateWeight(p.magnitude(), q.magnitude());
            if(_context.singleStep())System.out.println("weight : " + weight);
            delta = new Quaternion(delta.axis(), delta.angle() * weight);
        }*/
        //_smoothAngle = (float) Math.pow((i + 1.f) / (_context.last()) , 1f / (_context.solver().iteration() + 1 ));
        if(_smooth) delta = _clampRotation(delta, _smoothAngle);

        if(j_i.node().constraint() != null){
            delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
            delta.normalize();
        }


        //if(_context.direction()) delta = Quaternion.slerp(delta, applyOrientationalCCD(i, (float) Math.toRadians(600)), 0.2f);
        j_i.rotateAndUpdateCache(delta, true, endEffector); //Apply local rotation

        if(_context.direction()) {
            float posError = _context.positionError(endEffector, _context.worldTarget());
            float c_k = (float) Math.floor(posError / _context.positionWeight());
            float max_dist = (c_k + 1) * _context.positionWeight() - posError;
            float radius = Vector.distance(_context.worldTarget().position(), j_i.positionCache());
            //find max theta allowed
            float max_theta = (float) Math.acos(Math.max(Math.min(1 - (max_dist * max_dist) / (2 * radius * radius), 1), - 1)) * 0.5f;
            j_i.rotateAndUpdateCache(applyOrientationalCCD(i, max_theta), false, endEffector);
        }

        //j_i.rotateAndUpdateCache(, false,  _context.endEffectorInformation());
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

    protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle){
        float angle = rotation.angle();
        if(Math.abs(angle) > maxAngle ){
            rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
        }
        return rotation;
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


}
