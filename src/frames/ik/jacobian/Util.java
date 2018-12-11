package frames.ik.jacobian;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

import static processing.core.PApplet.radians;
import static processing.core.PApplet.urlEncode;

public class Util {

    public static void multiplyTranspose(){

    }

    public static void updateChain(List<? extends Frame> chain, SimpleMatrix delta){
        System.out.println("Entra ---- ");
        System.out.println("Cambio: ");
        System.out.println(delta.transpose().toString());

        Quaternion[] desiredOrientations = new Quaternion[chain.size()-1];

        for(int i = 0; i < delta.numRows(); i+=3){
            System.out.print("i : " + i);
            Frame frame = chain.get(i/3);
            /*Vector euler = frame.orientation().eulerAngles();
            euler.setX(euler.x() + (float) delta.get(i,0));
            euler.setY(euler.y() + (float) delta.get(i + 1,0));
            euler.setZ(euler.z() + (float) delta.get(i + 2,0));
            desiredOrientations[i/3] = new Quaternion(euler.x(), euler.y(), euler.z());*/

            Quaternion prev = frame.orientation();
            Quaternion orientation = prev.get();
            //After change in Z-Axis
            orientation.multiply(new Quaternion(new Vector(0,0,1), (float) delta.get(i + 2,0)));
            //After change in Y-Axis
            orientation.multiply(new Quaternion(new Vector(0,1,0), (float) delta.get(i + 1,0)));
            //chain.get(i/3).rotate(Quaternion.compose(prev.inverse(), orientation));
            //After change in X-Axis
            orientation.multiply(new Quaternion(new Vector(1,0,0), (float) delta.get(i,0)));
            desiredOrientations[i/3] = orientation;
        }
        //TODO: Consider to do for loop in reverse order
        for(int i = 0; i < chain.size()-1; i++) {
            chain.get(i).setOrientation(desiredOrientations[i]);
        }
    }


    //Here it is assumed a kinematic chain
    public static DMatrixRMaj jacobian(List<? extends Frame> chain, Frame endEffector, Vector target){
        if(chain == null) return null;
        int dim = endEffector.graph().is3D() ? 3 : 2;
        double [][] J = new double[dim][(chain.size()-1)*(dim == 2 ? 1 : 3)];
        //TODO: Make it in an efficient way (calculate global transformations)
        Vector x = new Vector(1,0,0);
        Vector y = new Vector(0,1,0);
        Vector z = new Vector(0,0,1);
        Vector t = target != null ?  target : endEffector.position();
        for(int j = 0; j < chain.size() - 1; j++){//Don't care about End Effector
            Vector r = Vector.subtract(t, chain.get(j).position());
            System.out.println("distt : " + r);
            Vector change_z =  Vector.cross(z, r, null);
            if(dim == 2){
                J[0][j] = change_z.x();
                J[1][j] = change_z.y();
            } else{
                Vector change_x =  Vector.cross(x, r, null);
                Vector change_y =  Vector.cross(y, r, null);
                System.out.println("- X : " + change_x);
                System.out.println("- X : " + change_y);
                System.out.println("- X : " + change_z);

                J[0][dim*j] = change_x.x();
                J[1][dim*j] = change_x.y();
                J[2][dim*j] = change_x.z();
                J[0][dim*j+1] = change_y.x();
                J[1][dim*j+1] = change_y.y();
                J[2][dim*j+1] = change_y.z();
                J[0][dim*j+2] = change_z.x();
                J[1][dim*j+2] = change_z.y();
                J[2][dim*j+2] = change_z.z();
            }
        }
        return new DMatrixRMaj(J);
    }


    //Here it is assumed a kinematic chain
    public static DMatrixRMaj numericalJacobian(List<? extends Frame> chain, Frame endEffector){
        if(chain == null) return null;
        int dim = endEffector.graph().is3D() ? 3 : 2;
        double [][] J = new double[dim][(chain.size()-1)*(dim == 2 ? 1 : 3)];
        //TODO: Make it in an efficient way (calculate global transformations)
        Vector x = new Vector(1,0,0);
        Vector y = new Vector(0,1,0);
        Vector z = new Vector(0,0,1);
        Vector t = endEffector.position();
        for(int j = 0; j < chain.size() - 1; j++){//Don't care about End Effector
            Vector r = Vector.subtract(t, chain.get(j).position());
            Vector change_z =  numerical(r, z);
            if(dim == 2){
                J[0][j] = change_z.x();
                J[1][j] = change_z.y();
            } else{
                Vector change_x =  numerical(r, x);
                Vector change_y =  numerical(r, y);
                System.out.println("- X : " + change_x);
                System.out.println("- X : " + change_y);
                System.out.println("- X : " + change_z);
                J[0][dim*j] = change_x.x();
                J[1][dim*j] = change_x.y();
                J[2][dim*j] = change_x.z();
                J[0][dim*j+1] = change_y.x();
                J[1][dim*j+1] = change_y.y();
                J[2][dim*j+1] = change_y.z();
                J[0][dim*j+2] = change_z.x();
                J[1][dim*j+2] = change_z.y();
                J[2][dim*j+2] = change_z.z();
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
        try{
            System.out.println("JJT : " + SimpleMatrix.wrap(J).mult(SimpleMatrix.wrap(J).transpose()).invert());
        }catch(Exception e){
            System.out.println("No inverse");
        }

        System.out.println("Debug : " + SimpleMatrix.wrap(J).mult(SimpleMatrix.wrap(delta)));
        System.out.println("error : " + SimpleMatrix.wrap(error));
        System.out.println("Delta: " + SimpleMatrix.wrap(delta));
        return delta;
    }

    public static DMatrixRMaj vectorToMatrix(Vector v, boolean is3D){
        return is3D ? new DMatrixRMaj(new double[]{v.x(), v.y(), v.z()}) : new DMatrixRMaj(new double[]{v.x(), v.y()});
    }

    public static Vector clampMagnitude(Vector v, float d){
        if(v.magnitude() > d){
            return Vector.multiply(v.normalize(null),d);
       }
        return v;
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
