package nub.ik.solver.trik.heuristic;

import nub.core.Graph;
import nub.core.Node;
import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

import java.util.List;

public class ForwardHeuristic extends Heuristic {
  /**
   * This heuristic requires to know the previous state of the chain, hence an auxiliary chain with this information is stored.
   * The idea of this heuristics is similar to FABRIK backward phase, where we want to reduce the distance between the goal and the end effector
   * hoping that the position of the joints doesn't change, while most of the motion is done by the joints near the head (base) of the chain.
   */
  protected List<NodeInformation> _auxiliaryChainInformation;
  protected boolean _discountedActions = true, _useMiddle = true;
  protected float _gamma = 0.5f;

  public ForwardHeuristic(Context context) {
    super(context);
    List<Node> auxiliarChain;
    if (context.debug() && Graph.isReachable(context.chain().get(0))) {
      auxiliarChain = Context._attachedCopy(context.chain(), null, false);
    } else {
      auxiliarChain = Context._detachedCopy(context.chain(), false);
    }
    _auxiliaryChainInformation = NodeInformation._createInformationList(auxiliarChain, true);
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

  protected static float _calculateWeight(float boneLength, float distToDesired) {
    float dist = distToDesired - boneLength;
    dist = dist < 0 ? -1f / dist : dist;
    float d_i = dist / boneLength;
    return (float) Math.pow(1.5, -d_i);
  }

  @Override
  public void applyActions(int i) {

    List<NodeInformation> chain = _context.usableChainInformation();
    NodeInformation j_i = chain.get(i); //must have a parent
    NodeInformation endEffector = _context.endEffectorInformation();

    Quaternion q1 = _findLocalRotation(j_i, chain.get(i + 1).node().translation(), _auxiliaryChainInformation.get(i + 1), _context.enableWeight());
    if (!_discountedActions) {
      if (_smooth) q1 = _clampRotation(q1, _smoothAngle);
      j_i.rotateAndUpdateCache(q1, true, endEffector); //Apply local rotation
      return;
    }
    //with discounting actions
    Vector des = new Vector();

    float sum = 0;
    float factor = 1;
    Vector v = chain.get(i + 1).node().translation().get();
    Vector v1 = q1.rotate(v);
    sum += factor;
    des.add(v1);

    if (_useMiddle && i < chain.size() - 2) {
      int middle = i + (int) Math.ceil((chain.size() - i) / 2);
      //Quaternion o_new = j_i.orientationCache().get();
      //Quaternion o_prev = Quaternion.compose(chain.get(i + 1).orientationCache().get(), chain.get(i + 1).node().rotation().get().inverse());
      //o_prev.normalize();
      //Quaternion delta = Quaternion.compose(o_new, o_prev.inverse());
      //delta.normalize();
      //Quaternion delta2 = Quaternion.compose(o_prev.inverse(), delta);
      //delta2.normalize();

      //Vector middle_pos = delta2.rotate(Vector.subtract(chain.get(middle).positionCache(), chain.get(i+1).positionCache()));
      //middle_pos.add(chain.get(i + 1).node().translation().get());


      Quaternion o_prev = Quaternion.compose(chain.get(i + 1).orientationCache().get(), chain.get(i + 1).node().rotation().get().inverse());
      o_prev.normalize();
      Quaternion delta = o_prev.inverse();
      Vector middle_pos = delta.rotate(Vector.subtract(chain.get(middle).positionCache(), chain.get(i + 1).positionCache()));
      middle_pos.add(chain.get(i + 1).node().translation());

      Quaternion q2 = _findLocalRotation(j_i, middle_pos, _auxiliaryChainInformation.get(middle), _context.enableWeight());
      Vector v2 = q2.rotate(v);
      factor *= _gamma;
      v2.multiply(factor);
      sum += factor;
      des.add(v2);
    }

    Quaternion q3 = _findLocalRotation(j_i, j_i.locationWithCache(chain.get(chain.size() - 1)), _auxiliaryChainInformation.get(chain.size() - 1), _context.enableWeight());
    Vector v3 = q3.rotate(v);
    factor *= _gamma;
    v3.multiply(factor);
    sum += factor;
    des.add(v3);

    //Get discount average
    des.multiply(1 / sum);
    q1 = new Quaternion(v, des);
    _smoothAngle = (float) Math.pow((i + 1.f) / (_context.last()), 1f / (0.2f * _context.solver().iteration() + 1));

    if (_smooth) q1 = _clampRotation(q1, _smoothAngle);
    j_i.rotateAndUpdateCache(q1, true, endEffector); //Apply local rotation
    //float f = 1f / (2 + _context.usableChain().size() - 2 - i);
    //q1 = _clampRotation(q1, f);
  }


  @Override
  public NodeInformation[] nodesToModify(int i) {
    return new NodeInformation[]{_context.usableChainInformation().get(i)};
  }

  //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUMED THAT THEY ARE UPDATE
  protected Quaternion _findLocalRotation(NodeInformation j_i, Vector j_i1, NodeInformation j_i1_hat, boolean enableWeight) {
    //Define how much rotate j_i in order to align j_i1 with j_i1_hat
    Vector p = j_i1;
    //Find j_i1_hat w.r.t j_i
    Vector q = j_i.locationWithCache(j_i1_hat);
    if (j_i.node().constraint() != null && j_i.node().constraint() instanceof Hinge) {
      Hinge h = (Hinge) j_i.node().constraint();
      Quaternion quat = Quaternion.compose(j_i.node().rotation().inverse(), h.idleRotation());
      Vector tw = h.restRotation().rotate(new Vector(0, 0, 1));
      tw = quat.rotate(tw);
      //Project p & q on the plane of rot
      p = Vector.projectVectorOnPlane(p, tw);
      q = Vector.projectVectorOnPlane(q, tw);
    }

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

  protected void _alignToTarget(NodeInformation root, NodeInformation eff, Node target) { //root has to be updated
    if (_context.direction()) {
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

  protected void _translateToTarget(NodeInformation root, NodeInformation eff, Node target) {
    //Find the distance from EFF current position and Target
    Vector diff = Vector.subtract(target.position(), eff.positionCache());
    //express diff w.r.t root reference
    Quaternion ref = Quaternion.compose(root.orientationCache(), root.node().rotation().inverse());
    diff = ref.inverseRotate(diff);
    //Move the root accordingly (disable constraints)
    root.translateAndUpdateCache(diff, false);
  }

  protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
    float angle = rotation.angle();
    if (Math.abs(angle) > maxAngle) {
      rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
    }
    return rotation;
  }

}


