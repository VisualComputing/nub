package frames.core.constraint;

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.core.Frame;

//TODO : Update

/**
 * Created by sebchaparr on 20/06/18.
 */
public class DistanceFieldConstraint extends Constraint {
    /*
    * With this Kind of Constraint no Translation is allowed
    * and the rotation depends on a Cone which base is a Spherical Polygon. This kind of constraint always
    * look for the reference frame (local constraint), if no initial position is
    * set a Quat() is assumed as rest position
    * */

    protected float[][][] _distance_field;
    protected float epsilon = 0.3f;

    public DistanceFieldConstraint(float[][][] distance_field) {
        this._distance_field = distance_field;
    }

    public float[][][] distance_field(){
        return _distance_field;
    }

    @Override
    public Quaternion constrainRotation(Quaternion rotation, Frame frame) {
        Quaternion desired = Quaternion.compose(frame.rotation(), rotation);
        return Quaternion.compose(frame.rotation().inverse(), apply(desired));
    }

    @Override
    public Vector constrainTranslation(Vector translation, Frame frame) {
        return new Vector(0, 0, 0);
    }

    public Quaternion apply(Quaternion desired){
        Vector euler = desired.eulerAngles();
        //Get distance in distance field
        if(euler.x() < 0)euler.setX((float)(euler.x() + 2*Math.PI));
        if(euler.y() < 0)euler.setY((float)(euler.y() + 2*Math.PI));
        if(euler.z() < 0)euler.setZ((float)(euler.z() + 2*Math.PI));


        int i = (int)(euler.x()/(2*Math.PI) * (_distance_field.length - 0.5));
        int j = (int)(euler.y()/(2*Math.PI) * (_distance_field[0].length - 0.5));
        int k = (int)(euler.z()/(2*Math.PI) * (_distance_field[0][0].length - 0.5));
        if(_distance_field[i][j][k] < epsilon){
            return desired;
        }
        //save as attributes
        float di = (float) (_distance_field.length / (2*Math.PI));
        float dj = (float) (_distance_field[0].length / (2*Math.PI));
        float dk = (float) (_distance_field[0][0].length / (2*Math.PI));
        //Apply twice
        //Vector projection = project(di,dj,dk,euler);//project(di,dj,dk,euler));
        Vector projection = project(di,dj,dk,project(di,dj,dk,euler));
        if(projection.x() < 0)projection.setX((float)(projection.x() + 2*Math.PI));
        if(projection.y() < 0)projection.setY((float)(projection.y() + 2*Math.PI));
        if(projection.z() < 0)projection.setZ((float)(projection.z() + 2*Math.PI));

        if(projection.x() > Math.PI)projection.setX((float)(projection.x() - 2*Math.PI));
        if(projection.y() > Math.PI)projection.setY((float)(projection.y() - 2*Math.PI));
        if(projection.z() > Math.PI)projection.setZ((float)(projection.z() - 2*Math.PI));


        return new Quaternion(projection.x(),projection.y(),projection.z());
    }

    protected Vector project(float di, float dj, float dk, Vector p){
        if(p.x() < 0)p.setX((float)(p.x() + 2*Math.PI));
        if(p.y() < 0)p.setY((float)(p.y() + 2*Math.PI));
        if(p.z() < 0)p.setZ((float)(p.z() + 2*Math.PI));

        int I = _distance_field.length;
        int J = _distance_field[0].length;
        int K = _distance_field[0][0].length;

        //System.out.println("----> p.x : " + p.x() + " p.y : " + p.y() + " p.z : " + p.z());
        int i = (int)(p.x()/(2*Math.PI) * (_distance_field.length - 0.5)) % I;
        int j = (int)(p.y()/(2*Math.PI) * (_distance_field[0].length - 0.5)) % J;
        int k = (int)(p.z()/(2*Math.PI) * (_distance_field[0][0].length - 0.5)) % K;

        Vector delta = new Vector();
        //Compute gradient
        //System.out.println("----> i : " + i + " j : " + j + " k : " + k);
        //System.out.println("----> I : " + I + " J : " + J + " K : " + K);

        //System.out.println("----> distance : " + _distance_field[i][j][k]);

        delta.setX((_distance_field[Math.min(i+1, I-1)][j][k] - _distance_field[Math.max(i-1,0)][j][k])/(2*di));
        delta.setY((_distance_field[i][Math.min(j+1, J-1)][k] - _distance_field[i][Math.max(j-1,0)][k])/(2*dj));
        delta.setZ((_distance_field[i][j][Math.min(k+1, K-1)] - _distance_field[i][j][Math.max(k-1,0)])/(2*dk));
        /*

        System.out.println("----> xi : " + (_distance_field[Math.max(i-1,0)][j][k]));
        System.out.println("----> yi : " + (_distance_field[i][Math.max(j-1,0)][k]));
        System.out.println("----> zi : " + (_distance_field[i][j][Math.max(k-1,0)]));

        System.out.println("----> xf : " + (_distance_field[Math.min(i+1, I-1)][j][k]));
        System.out.println("----> yf : " + (_distance_field[i][Math.min(j+1, J-1)][k]));
        System.out.println("----> zf : " + (_distance_field[i][j][Math.min(k+1, K-1)]));

        System.out.println("----> dx : " + (_distance_field[Math.min(i+1, I-1)][j][k] - _distance_field[Math.max(i-1,0)][j][k]));
        System.out.println("----> dy : " + (_distance_field[i][Math.min(j+1, J-1)][k] - _distance_field[i][Math.max(j-1,0)][k]));
        System.out.println("----> dz : " + (_distance_field[i][j][Math.min(k+1, K-1)] - _distance_field[i][j][Math.max(k-1,0)]));
        */
        delta.normalize();
        //System.out.println("----> delta : " + delta);
        //System.out.println("Res : " + Vector.subtract(p, Vector.multiply(delta, _distance_field[i][j][k])));

        return Vector.subtract(p, Vector.multiply(delta, _distance_field[i][j][k]));
    }
}