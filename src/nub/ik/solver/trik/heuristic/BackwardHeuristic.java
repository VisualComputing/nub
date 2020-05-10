package nub.ik.solver.trik.heuristic;

import nub.core.Node;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class BackwardHeuristic extends Heuristic {
  /**
   * The idea of this heuristics is similar to FABRIK forward phase, where we want to reduce the distance between the goal and the end effector
   * hoping that the position of the joints doesn't change, while most of the motion is done by the joints near the  end effector of the chain.
   */
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
    if (_smooth) delta = _clampRotation(delta, _smoothAngle);
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
    }

    if (i == _context.last() - 1) {
      j_i.rotateAndUpdateCache(delta, false, endEffector); //Apply local rotation
      return;
    }

    //alpha = r_{i+1}^-1 q1^-1 r_{i+1}
    Quaternion alpha = Quaternion.compose(delta, j_i1.node().rotation()).inverse();
    alpha.compose(j_i1.node().rotation());

    if (j_i1.node().constraint() != null) {
      alpha = j_i1.node().constraint().constrainRotation(alpha, j_i1.node());
      delta = Quaternion.compose(j_i1.node().rotation(), alpha.inverse());
      delta.compose(j_i1.node().rotation().inverse());
    }
    j_i.rotateAndUpdateCache(delta, true, endEffector); //Apply local rotation
    j_i1.updateCacheUsingReference();
    j_i1.rotateAndUpdateCache(alpha, false, endEffector);
  }

  @Override
  public NodeInformation[] nodesToModify(int i) {
    return new NodeInformation[]{_context.usableChainInformation().get(i - 1), _context.usableChainInformation().get(i)};
  }


  //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUMED THAT THEY ARE UPDATE
  protected static float _calculateWeight(float boneLength, float distToDesired) {
    float dist = distToDesired - boneLength;
    dist = dist < 0 ? -1f / dist : dist;
    float d_i = dist / boneLength;
    return (float) Math.pow(1.5, -d_i);
  }

  protected Quaternion _findLocalRotation(NodeInformation j_i, NodeInformation j_i1, NodeInformation endEffector, Node target, boolean enableWeight) {
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
    if (enableWeight) {
      weight = _calculateWeight(p.magnitude(), q.magnitude());
      if (_context.singleStep()) System.out.println("weight : " + weight);
      delta = new Quaternion(delta.axis(), delta.angle() * weight);
    }
    return delta;
  }

  protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
    float angle = rotation.angle();
    if (Math.abs(angle) > maxAngle) {
      rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
    }
    return rotation;
  }
}
