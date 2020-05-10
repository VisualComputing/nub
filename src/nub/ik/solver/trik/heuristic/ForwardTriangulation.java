package nub.ik.solver.trik.heuristic;

import nub.core.constraint.Hinge;
import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class ForwardTriangulation extends Heuristic {
  /**
   * The idea of this heuristics is to apply popular CCD Step. Here most of the work is done by the last joint and as them could move
   * that what is truly required the final pose of the chain will not be perceived as a natural pose.
   */
  public ForwardTriangulation(Context context) {
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
    Vector a = j_i1.node().translation();

    //TODO : Use triangulation with discounted actions - mid point

        /*if(i < _context.last() - 1){
            int middle = i + (int) Math.ceil((_context.usableChain().size() - i) / 2);
            List<NodeInformation> chain = _context.usableChainInformation();

            Quaternion o_prev = Quaternion.compose(chain.get(i + 1).orientationCache().get(), chain.get(i + 1).node().rotation().get().inverse());
            o_prev.normalize();
            Quaternion q = o_prev.inverse();
            Vector middle_pos = q.rotate(Vector.subtract(chain.get(middle).positionCache(), chain.get(i+1).positionCache()));
            middle_pos.add(chain.get(i+1).node().translation());
            a = middle_pos;
        }*/

    Vector b = j_i.locationWithCache(_context.endEffectorInformation());
    b.subtract(a);
    Vector c = j_i.locationWithCache(_context.worldTarget().position());

    if (j_i.node().constraint() != null && j_i.node().constraint() instanceof Hinge) {
      Hinge h = (Hinge) j_i.node().constraint();
      Quaternion quat = Quaternion.compose(j_i.node().rotation().inverse(), h.idleRotation());
      Vector tw = h.restRotation().rotate(new Vector(0, 0, 1));
      tw = quat.rotate(tw);
      //Project b & c on the plane of rot
      a = Vector.projectVectorOnPlane(a, tw);
      b = Vector.projectVectorOnPlane(b, tw);
      c = Vector.projectVectorOnPlane(c, tw);
    }


    float a_mag = a.magnitude(), b_mag = b.magnitude(), c_mag = c.magnitude();


    Quaternion delta;
    if (a_mag + b_mag <= c_mag) {
      //Chain must be extended as much as possible
      if (_context.debug()) System.out.println("Extend chain!");
      delta = new Quaternion(a, c);
    } else if (c_mag < Math.abs(a_mag - b_mag)) {
      //Chain must be contracted as much as possible
      if (_context.debug()) System.out.println("Contract chain!");
      delta = new Quaternion(a, Vector.multiply(c, -1));
    } else {
      //Apply law of cosines
      float angle = (float) (Math.acos(Vector.dot(a, c) / (a_mag * c_mag)) - Math.acos(-(b_mag * b_mag - a_mag * a_mag - c_mag * c_mag) / (2f * a_mag * c_mag)));
      delta = new Quaternion(Vector.cross(a, c, null), angle);
    }


    //float f = 1f / (2 + _context.usableChain().size() - 2 - i);
    //delta = _clampRotation(delta, f);

    //_smoothAngle = (float) Math.pow((i + 1.f) / (_context.last()) , 1f / (_context.solver().iteration() + 1 ));
    if (_context.debug()) {
      System.out.println("Tr. Vec a : " + a + "mag" + a.magnitude());
      System.out.println("Tr. Vec b : " + b + "mag" + b.magnitude());
      System.out.println("Tr. Vec c : " + c + "mag" + c.magnitude());
    }


    if (_smooth) delta = _clampRotation(delta, _smoothAngle * delta.angle());
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
    }
    if (_context.debug()) System.out.println("***** Tr. angle : " + Math.toDegrees(delta.angle()));

    j_i.rotateAndUpdateCache(delta, false, _context.endEffectorInformation()); //Apply local rotation
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

  protected static Quaternion _clampRotation(Quaternion rotation, float maxAngle) {
    float angle = rotation.angle();
    if (Math.abs(angle) > maxAngle) {
      rotation = new Quaternion(rotation.axis(), (Math.signum(angle) * maxAngle));
    }
    return rotation;
  }
}
