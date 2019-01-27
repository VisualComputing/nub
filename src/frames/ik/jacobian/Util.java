package frames.ik.jacobian;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.linsol.svd.SolvePseudoInverseSvd_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

import static processing.core.PApplet.radians;
import static processing.core.PApplet.urlEncode;

public class Util {

    public static void multiplyTranspose(){

    }

    public static void updateChain(List<? extends Frame> chain, SimpleMatrix delta, Vector[] axes){
        Quaternion[] desiredOrientations = new Quaternion[chain.size()-1];
        for(int i = 0; i < delta.numRows(); i++){
            Frame frame = chain.get(i);
            Quaternion prev = frame.orientation();
            Quaternion orientation = prev.get();
            float angle = (float) delta.get(i,0);
            orientation.multiply(new Quaternion(axes[i], angle));
            desiredOrientations[i] = orientation;
        }
        //TODO: Consider to do for loop in reverse order
        for(int i = 0; i < chain.size()-1; i++) {
            chain.get(i).setOrientation(desiredOrientations[i]);
        }
    }

    //Here it is assumed a kinematic chain
    public static DMatrixRMaj jacobian(List<? extends Frame> chain, Frame endEffector, Vector target, Vector[] axes){
        if(chain == null) return null;
        int dim = endEffector.graph().is3D() ? 3 : 2;
        double [][] J = new double[dim][(chain.size()-1)];
        //TODO: Make it in an efficient way (calculate global transformations)
        Vector z = new Vector(0,0,1);
        Vector ef = endEffector.position();
        for(int j = 0; j < chain.size() - 1; j++){//Don't care about End Effector
            Vector joint = chain.get(j).position();
            Vector je = Vector.subtract(ef, joint);
            System.out.println("-- Joint to End Eff : \t " + je);
            //System.out.println("distt : " + r);
            Vector change_z =  Vector.cross(z, je, null);
            if(dim == 2){
                J[0][j] = change_z.x();
                J[1][j] = change_z.y();
            } else{
                Vector jt = Vector.subtract(target, joint);
                System.out.println("-- Joint to Target : \t " + jt);
                Vector axis = Vector.cross(je, jt, null);
                System.out.println("-- Axis : \t " + axis + " mag " + axis.magnitude());
                if(axis.magnitude() < 1E-2) axis = Vector.orthogonalVector(je);
                System.out.println("-- Axis Chang : \t " + axis);
                axis.normalize();
                System.out.println("-- Axis Norma : \t " + axis);
                axes[j] = axis;
                Vector change = Vector.cross(axis, je, null);
                System.out.println("-- change : \t " + change);
                J[0][j] = change.x();
                J[1][j] = change.y();
                J[2][j] = change.z();
            }
        }
        return new DMatrixRMaj(J);
    }


    //Here it is assumed a kinematic chain
    public static DMatrixRMaj numericalJacobian(List<? extends Frame> chain, Frame endEffector, Vector target, Vector[] axes){
        if(chain == null) return null;
        int dim = endEffector.graph().is3D() ? 3 : 2;
        double [][] J = new double[dim][(chain.size()-1)];
        //TODO: Make it in an efficient way (calculate global transformations)
        Vector z = new Vector(0,0,1);
        Vector ef = endEffector.position();
        for(int j = 0; j < chain.size() - 1; j++){//Don't care about End Effector
            Vector joint = chain.get(j).position();
            Vector je = Vector.subtract(ef, joint);
            //System.out.println("distt : " + r);
            Vector change_z =  numerical(z, je);
            if(dim == 2){
                J[0][j] = change_z.x();
                J[1][j] = change_z.y();
            } else{
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


    public static DMatrixRMaj solvePseudoinverse(DMatrixRMaj J, DMatrixRMaj error){
        LinearSolver<DMatrixRMaj, DMatrixRMaj> solver = LinearSolverFactory_DDRM.pseudoInverse(true);
        solver.setA(J);
        DMatrixRMaj delta = new DMatrixRMaj(new double[J.numCols]);
        solver.solve(error, delta);
        System.out.println("JJT : " + SimpleMatrix.wrap(J).mult(SimpleMatrix.wrap(J).transpose()));

        DMatrixRMaj pinv = new DMatrixRMaj(J.numCols, J.numRows);
        ((LinearSolverDense<DMatrixRMaj>) solver).invert(pinv);
        System.out.println("PINV: " + pinv);
        System.out.println("Delta a: " + SimpleMatrix.wrap(pinv).mult(SimpleMatrix.wrap(error)));
        System.out.println("Error a : " + SimpleMatrix.wrap(J).mult(SimpleMatrix.wrap(delta)));
        System.out.println("Quality : " + solver.quality());
        //System.out.println("Debug : " + SimpleMatrix.wrap(J).mult(SimpleMatrix.wrap(delta)));
        System.out.println("error : " + SimpleMatrix.wrap(error));

        System.out.println("sing values : " + ((SolvePseudoInverseSvd_DDRM) solver).getDecomposition().getSingularValues());
        //System.out.println("Delta: " + SimpleMatrix.wrap(delta));
        return delta;
    }

    public static DMatrixRMaj vectorToMatrix(Vector v, boolean is3D){
        return is3D ? new DMatrixRMaj(new double[]{v.x(), v.y(), v.z()}) : new DMatrixRMaj(new double[]{v.x(), v.y()});
    }

    public static Vector numerical(Vector x, Vector axis){
        float delta = (float) Math.toRadians(1);
        Quaternion q2 = new Quaternion(axis, delta);
        Quaternion q1 = new Quaternion(axis, -delta);
        Vector f2 = q2.rotate(x);
        Vector f1 = q1.rotate(x);
        return Vector.multiply(Vector.subtract(f2, f1),1/(2*delta));
    }
}
