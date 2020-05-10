package nub.ik.solver.trik.heuristic;

import nub.ik.solver.geometric.oldtrik.NodeInformation;
import nub.ik.solver.trik.Context;
import nub.primitives.Quaternion;
import nub.primitives.Vector;

public class BackwardTriangulation extends Heuristic {
  /**
   * The idea of this heuristics is similar to the Triangulation method. Here the motion is propagated from bottom to top
   * and as each step must keep the orientation of the joints and additional fix step is required.
   */

  public BackwardTriangulation(Context context) {
    super(context);
  }

  @Override
  public void prepare() {
    //Update cache of usable chain
    NodeInformation._updateCache(_context.usableChainInformation());
  }

  @Override
  public void applyActions(int i) {
    if (i == 0) return;

    NodeInformation j_0 = _context.usableChainInformation().get(0);
    NodeInformation j_i = _context.usableChainInformation().get(i);
    NodeInformation endEffector = _context.endEffectorInformation();
    Vector t = j_i.locationWithCache(_context.worldTarget().position());
    Vector e = j_i.locationWithCache(endEffector.positionCache());

    Vector b = j_i.locationWithCache(j_0);
    Vector c = Vector.subtract(t, b);
    //Project a on the plane given by vector b and t
    Vector axis = Vector.cross(b, t, null);
    Vector a = e;

    System.out.println("Joint : " + i);
    float a_mag = a.magnitude(), b_mag = b.magnitude(), c_mag = c.magnitude();
    System.out.println(" a " + a + " b " + b + " c " + c);
    System.out.println(" a mag" + a_mag + " b_mag " + b_mag + " c_mag " + c_mag);

    Quaternion delta;
    if (a_mag + b_mag <= c_mag) {
      //Chain must be extended as much as possible
      delta = new Quaternion(a, Vector.multiply(b, -1));
      System.out.println("Entra 1");
      System.out.println("    delta " + delta.axis() + Math.toDegrees(delta.angle()));
    } else if (c_mag < Math.abs(a_mag - b_mag)) {
      //Chain must be contracted as much as possible
      delta = new Quaternion(a, b);
      System.out.println("Entra 2");
      System.out.println("    delta " + delta.axis() + Math.toDegrees(delta.angle()));
    } else {
      //Apply law of cosines
      float angle = (float) (Math.acos(-(c_mag * c_mag - a_mag * a_mag - b_mag * b_mag) / (2f * a_mag * b_mag)));
      System.out.println("    delta " + angle);
      delta = new Quaternion(a, b);
      delta.compose(new Quaternion(axis, angle));
      System.out.println("Entra 3");
      System.out.println("    delta " + delta.axis() + Math.toDegrees(delta.angle()));
    }

    if (_smooth) delta = _clampRotation(delta, _smoothAngle);
    if (j_i.node().constraint() != null) {
      delta = j_i.node().constraint().constrainRotation(delta, j_i.node());
    }

    j_i.rotateAndUpdateCache(delta, false, endEffector); //Apply local rotation


    //Align base
    Vector p = j_0.locationWithCache(endEffector);
    Vector q = j_0.locationWithCache(_context.worldTarget().position());
    Quaternion alpha = new Quaternion(p, q);

    System.out.println("P : " + p);
    System.out.println("Q : " + q);
    System.out.println("Alpha --- " + alpha.axis() + Math.toDegrees(alpha.angle()));

        /*if(_smooth) alpha = _clampRotation(alpha, _smoothAngle);
        if(j_0.node().constraint() != null){
            alpha = j_0.node().constraint().constrainRotation(alpha, j_0.node());
        }*/
    j_0.rotateAndUpdateCache(alpha, false, endEffector, j_i);
    if (i > 1) {
      _context.usableChainInformation().get(i - 1).updateCacheUsingChild(j_i);
    }
  }

  @Override
  public NodeInformation[] nodesToModify(int i) {
    return new NodeInformation[0];
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
