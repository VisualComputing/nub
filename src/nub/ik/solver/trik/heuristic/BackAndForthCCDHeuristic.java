package nub.ik.solver.trik.heuristic;

import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.ik.solver.trik.NodeState;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class BackAndForthCCDHeuristic extends Heuristic {
  /**
   * The idea of this solver is to work similar to Triangulation solver.
   * Triangulation performs well when j_i and j_{i+1} joints are not highly constrained, in other cases
   * (e.g. Hinge - Hinge) the solver is does not perform the best local action for a 3-Joint substructure.
   */

  protected int _times = 3;
  protected boolean _applyTriangulation, _topToBottom;


  public BackAndForthCCDHeuristic(Context context, boolean applyTriangulation, boolean topToBottom) {
    super(context);
    _applyTriangulation = applyTriangulation;
    _topToBottom = topToBottom;
  }

  public BackAndForthCCDHeuristic(Context context) {
    this(context, false, true);
  }


  @Override
  public void prepare() {
    //Update cache of usable chain
    NodeInformation._updateCache(_context.usableChainInformation());
  }

  @Override
  public void applyActions(int i) {
    //Save status of joint j_i and j_i1
    NodeInformation j_i = _context.usableChainInformation().get(i);
    NodeInformation endEffector = _context.endEffectorInformation();

    if (i == _context.last() - 1) {
      j_i.rotateAndUpdateCache(_applyAction(j_i), false, endEffector); //Apply local rotation
      return;
    }

    NodeInformation j_i1 = _context.usableChainInformation().get(i + 1);
    NodeState j_i_init = new NodeState(j_i);
    NodeState j_i1_init = new NodeState(j_i1);
    NodeState endEffector_init = new NodeState(endEffector);

    int k = 0;

    //Try to apply triangulation (modifying j_1 rotation) + Alignment
    if (_applyTriangulation) {
      _triangulation(j_i, j_i1);
      j_i.rotateAndUpdateCache(_applyAction(j_i), false, endEffector); //Apply local rotation
    }

    //Try common CCD
    while (k < _times) {
      j_i1.updateCacheUsingReference();
      Quaternion q1 = _applyAction(j_i1);
      //Apply rotation and update joint
      j_i1.rotateAndUpdateCache(q1, false, endEffector); //Apply local rotation
      //Do the same with previous joint
      Quaternion q = _applyAction(j_i);
      j_i.rotateAndUpdateCache(q, false, endEffector); //Apply local rotation
      k++;
    }
    //Define delta
    if (_topToBottom) {
      Quaternion delta = Quaternion.compose(j_i_init.rotation().inverse(), j_i.node().rotation());
      //Undo actions
      j_i.setCache(j_i_init.position().get(), j_i_init.orientation().get());
      j_i.node().setRotation(j_i_init.rotation());
      j_i1.setCache(j_i1_init.position().get(), j_i1_init.orientation().get());
      j_i1.node().setRotation(j_i1_init.rotation());
      endEffector.setCache(endEffector_init.position().get(), endEffector_init.orientation().get());
      j_i.rotateAndUpdateCache(delta, true, endEffector); //Apply local rotation
    }
  }

  @Override
  public NodeInformation[] nodesToModify(int i) {
    return new NodeInformation[]{_context.usableChainInformation().get(i - 1), _context.usableChainInformation().get(i)};
  }

  protected Quaternion _applyAction(NodeInformation j_i) {
    Vector p = j_i.locationWithCache(_context.endEffectorInformation().positionCache());
    Vector q = j_i.locationWithCache(_context.worldTarget().position());
    if (j_i.node().constraint() != null && j_i.node().constraint() instanceof Hinge) {
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
    float weight = 1;
    if (_context.enableWeight()) {
      weight = _calculateWeight(p.magnitude(), q.magnitude());
      if (_context.singleStep()) System.out.println("weight : " + weight);
      delta = new Quaternion(delta.axis(), delta.angle() * weight);
    }
    //_smoothAngle = (float) Math.pow((i + 1.f) / (_context.last()) , 1f / (_context.solver().iteration() + 1 ));

    if (_smooth) delta = _clampRotation(delta, _smoothAngle);
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
    }
    return delta;
  }

  protected void _triangulation(NodeInformation j_i, NodeInformation j_i1) {
    j_i1.updateCacheUsingReference();
    Vector a = j_i1.locationWithCache(j_i.positionCache());
    Vector b = j_i1.locationWithCache(_context.endEffectorInformation());
    Vector t = j_i1.locationWithCache(_context.worldTarget().position());
    Vector c = Vector.subtract(t, a);

    float a_mag = a.magnitude(), b_mag = b.magnitude(), c_mag = c.magnitude();
    Quaternion delta;
    if (a_mag + b_mag <= c_mag) {
      //Chain must be extended as much as possible
      delta = new Quaternion(b, Vector.multiply(a, -1));
    } else if (c_mag < Math.abs(a_mag - b_mag)) {
      //Chain must be contracted as much as possible
      delta = new Quaternion(b, a);
    } else {
      //Apply law of cosines
      float angle = (float) (Math.acos(Vector.dot(a, b) / (a_mag * b_mag)) - Math.acos(-(c_mag * c_mag - a_mag * a_mag - b_mag * b_mag) / (2f * a_mag * b_mag)));
      delta = new Quaternion(Vector.cross(t, a, null), angle);
    }
    if (_smooth) delta = _clampRotation(delta, _smoothAngle * delta.angle());
    if (j_i1.node().constraint() != null) {
      delta = j_i1.node().constraint().constrainRotation(delta, j_i1.node());
    }
    j_i1.rotateAndUpdateCache(delta, true, _context.endEffectorInformation()); //Apply local rotation

  }


  //IMPORTANT: THE FOLLOWING METHODS USE THE CACHE POSITION/ORIENTATION IT IS AND ASSUMED THAT THEY ARE UPDATE
  protected static float _calculateWeight(float boneLength, float distToDesired) {
    float dist = distToDesired - boneLength;
    dist = dist < 0 ? -1f / dist : dist;
    float d_i = dist / boneLength;
    return (float) Math.pow(1.5, -d_i);
  }

  protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
    float angle = rotation.angle();
    if (Math.abs(angle) > maxAngle) {
      rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
    }
    return rotation;
  }

}
