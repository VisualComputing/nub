/****************************************************************************************
 * nub
 * Copyright (c) 2019 National University of Colombia, https://visualcomputing.github.io/
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, https://github.com/VisualComputing
 *
 * All rights reserved. A 2D or 3D scene graph library providing eye, input and timing
 * handling to a third party (real or non-real time) renderer. Released under the terms
 * of the GPL v3.0 which is available at http://www.gnu.org/licenses/gpl.html
 ****************************************************************************************/

package nub.ik.solver.numerical;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

public class Util {

  //TODO : Allow this methods to work with multiple end effectors
  public static void updateChain(List<? extends Node> chain, SimpleMatrix delta, Vector[] axes) {
    //TODO: Keep Quaternion orientation is more efficient
    for (int i = 0; i < delta.numRows(); i++) {
      Node frame = chain.get(i);
      Vector axis = frame.displacement(axes[i]);
      float angle = (float) delta.get(i, 0);
      //orientation.multiply(new Quaternion(axes[i], angle));
      //desiredOrientations[i] = orientation;
      chain.get(i).rotate(new Quaternion(axis, angle));
    }
  }

  //Here it is assumed a kinematic chain
  public static DMatrixRMaj jacobian(List<? extends Node> chain, Node endEffector, Vector target, Vector[] axes) {
    if (chain == null) return null;
    int dim = endEffector.graph().is3D() ? 3 : 2;
    double[][] J = new double[dim][(chain.size() - 1)];
    //TODO: Make it in an efficient way (calculate global transformations)
    Vector z = new Vector(0, 0, 1);
    Vector ef = endEffector.position();
    for (int j = 0; j < chain.size() - 1; j++) {//Don't care about End Effector
      Vector joint = chain.get(j).position();
      Vector je = Vector.subtract(ef, joint);
      Vector change_z = Vector.cross(z, je, null);
      if (dim == 2) {
        J[0][j] = change_z.x();
        J[1][j] = change_z.y();
      } else {
        Vector jt = Vector.subtract(target, joint);
        Vector axis = Vector.cross(je, jt, null);
        if (axis.magnitude() < 1E-2) axis = Vector.orthogonalVector(je);
        axis.normalize();
        axes[j] = axis;
        Vector change = Vector.cross(axis, je, null);
        J[0][j] = change.x();
        J[1][j] = change.y();
        J[2][j] = change.z();
      }
    }
    return new DMatrixRMaj(J);
  }

  //Here it is assumed a kinematic chain
  public static DMatrixRMaj numericalJacobian(List<? extends Node> chain, Node endEffector, Vector target, Vector[] axes) {
    if (chain == null) return null;
    int dim = endEffector.graph().is3D() ? 3 : 2;
    double[][] J = new double[dim][(chain.size() - 1)];
    //TODO: Make it in an efficient way (calculate global transformations)
    Vector z = new Vector(0, 0, 1);
    Vector ef = endEffector.position();
    for (int j = 0; j < chain.size() - 1; j++) {//Don't care about End Effector
      Vector joint = chain.get(j).position();
      Vector je = Vector.subtract(ef, joint);
      Vector change_z = numerical(z, je);
      if (dim == 2) {
        J[0][j] = change_z.x();
        J[1][j] = change_z.y();
      } else {
        Vector jt = Vector.subtract(target, joint);
        Vector axis = Vector.cross(je, jt, null);
        axis.normalize();
        axes[j] = axis;
        Vector change = numerical(axis, je);
        J[0][j] = change.x();
        J[1][j] = change.y();
        J[2][j] = change.z();
      }
    }
    return new DMatrixRMaj(J);
  }

  public static DMatrixRMaj solvePseudoinverse(DMatrixRMaj J, DMatrixRMaj error) {
    LinearSolver<DMatrixRMaj, DMatrixRMaj> solver = LinearSolverFactory_DDRM.pseudoInverse(true);
    solver.setA(J);
    DMatrixRMaj delta = new DMatrixRMaj(new double[J.numCols]);
    solver.solve(error, delta);
    return delta;
  }

  public static DMatrixRMaj vectorToMatrix(Vector v, boolean is3D) {
    return is3D ? new DMatrixRMaj(new double[]{v.x(), v.y(), v.z()}) : new DMatrixRMaj(new double[]{v.x(), v.y()});
  }

  public static Vector numerical(Vector x, Vector axis) {
    float delta = (float) Math.toRadians(1);
    Quaternion q2 = new Quaternion(axis, delta);
    Quaternion q1 = new Quaternion(axis, -delta);
    Vector f2 = q2.rotate(x);
    Vector f1 = q1.rotate(x);
    return Vector.multiply(Vector.subtract(f2, f1), 1 / (2 * delta));
  }
}
