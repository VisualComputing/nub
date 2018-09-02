package ik.mocap;

import frames.primitives.Quaternion;
import frames.primitives.Vector;
import java.util.*;

/**
 * Created by sebchaparr on 15/04/18.
 */
public class LearnConstraint {
    protected static ArrayList<Quaternion> performAdaptativeSampling(List<Quaternion> data, float step){
        if(data.isEmpty()) return null;
        ArrayList<Quaternion> new_data = new ArrayList<Quaternion>();
        float delta = 1;
        for(int i = 0; i < data.size() - 1; i++){
            Quaternion prev = data.get(i);
            new_data.add(data.get(i));
            float t = 0;
            while(t < 1) {
                Quaternion next = Quaternion.slerp(data.get(i), data.get(i+1), t);
                while (Vector.distance(prev.eulerAngles(), next.eulerAngles()) > step / 2.0) {
                    next = Quaternion.slerp(prev, next, 0.5f);
                    delta /= 2.0;
                }
                new_data.add(next);
                prev = next;
                t = Math.min(t + delta, 1);
            }
        }
        return new_data;
    }

    public static float[][][] getDistanceField(List<Quaternion> data, int I, int J, int K){
        if(data.isEmpty()) return null;
        float[][][] distance_field = new float[I][J][K];
        float step = (float) ((2*Math.PI) / Math.min(Math.min( I , J ) , K));
        List<Quaternion> new_data = performAdaptativeSampling(data, step);
        //Get a list of vectors
        ArrayList<Vector> vectors = new ArrayList<Vector>();
        for(Quaternion quaternion : new_data){
            Vector euler = quaternion.eulerAngles();
            if(euler.x() < 0)euler.setX((float)(euler.x() + 2*Math.PI));
            if(euler.y() < 0)euler.setY((float)(euler.y() + 2*Math.PI));
            if(euler.z() < 0)euler.setZ((float)(euler.z() + 2*Math.PI));
            vectors.add(euler);
        }
        for(int i = 0; i < I; i++){
            for(int j = 0; j < J; j++){
                for(int k = 0; k < K; k++){
                    float min = Float.MAX_VALUE;
                    Vector euler = new Vector((i + 0.5f) * 2 * (float)Math.PI / I,
                            (j + 0.5f) * 2 * (float)Math.PI / J,
                            (k + 0.5f) * 2 * (float)Math.PI / K);
                    int w = 0;
                    for(Vector vector : vectors){
                        float dx = (float) Math.min((2 * Math.PI) - Math.abs(euler.x() - vector.x()), Math.abs(euler.x() - vector.x()));
                        float dy = (float) Math.min((2 * Math.PI) - Math.abs(euler.y() - vector.y()), Math.abs(euler.y() - vector.y()));
                        float dz = (float) Math.min((2 * Math.PI) - Math.abs(euler.z() - vector.z()), Math.abs(euler.z() - vector.z()));
                        min = Math.min(min, (float) Math.sqrt(dx*dx + dy*dy + dz*dz));
                        //min = Math.min(min, Vector.distance(euler, vector));
                        w++;
                    }
                    distance_field[i][j][k] = min;
                }
            }
        }

        return distance_field;
    }
}
