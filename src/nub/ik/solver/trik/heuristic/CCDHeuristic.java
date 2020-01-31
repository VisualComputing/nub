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
        }

        j_i.rotateAndUpdateCache(delta, false, endEffector); //Apply local rotation
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
}
