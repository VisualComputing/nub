package ik.common;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import frames.processing.Scene;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.opengl.PShader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by sebchaparr on 11/03/18.
 */
public class LinearBlendSkinningGPU {
    public Frame reference = new Frame();
    public PShape shape;
    public PShader shader;
    public Quaternion[] boneQuat = new Quaternion[120];
    public Vector[] bonePos = new Vector[120];
    public float[] bonePositionOrig = new float[120];
    public float[] bonePosition = new float[120];
    float[] boneRotation = new float[120];

    public ArrayList<Frame> skeleton;

    public LinearBlendSkinningGPU(PShape shape, ArrayList<Frame> skeleton) {
        this.shape = shape;
        this.skeleton = skeleton;
    }

    public void initParams(PApplet applet, Scene scene) {
        shader = applet.loadShader(applet.sketchPath() + "/testing/src/ik/common/frag.glsl",
                applet.sketchPath() + "/testing/src/ik/common/skinning.glsl");
        for(int i = 0; i < skeleton.size(); i++){
            Frame frame = skeleton.get(i);
            Vector position = frame.position();
            boneQuat[i] = frame.orientation();
            bonePos[i] = position;
            bonePositionOrig[i*3 + 0] = position.x();
            bonePositionOrig[i*3 + 1] = position.y();
            bonePositionOrig[i*3 + 2] = position.z();
        }
        shader.set("bonePositionOrig", bonePositionOrig);
        shader.set("boneLength", skeleton.size());
        setup(skeleton);
    }

    public void updateParams() {
        //TODO: IT COULD BE DONE WITH LESS OPERATIONS
        for(int i = 0; i < skeleton.size(); i++){
            Frame frame = skeleton.get(i);
            Vector v = Vector.subtract(frame.position(), bonePos[i]);
            Quaternion q = Quaternion.compose(frame.orientation(), boneQuat[i].inverse());
            bonePosition[i*3 + 0] =  v.x();
            bonePosition[i*3 + 1] =  v.y();
            bonePosition[i*3 + 2] =  v.z();
            boneRotation[i*4 + 0] =  q.x();
            boneRotation[i*4 + 1] =  q.y();
            boneRotation[i*4 + 2] =  q.z();
            boneRotation[i*4 + 3] =  q.w();
        }
        shader.set("bonePosition", bonePosition);
        shader.set("boneRotation", boneRotation);
    }

    public void setup(ArrayList<Frame> branch) {
        for (int i = 0; i < shape.getChildCount(); i++) {
            PShape child = shape.getChild(i);
            for (int j = 0; j < child.getVertexCount(); j++) {
                PVector vector = child.getVertex(j);
                Vector position = new Vector(vector.x, vector.y, vector.z);
                float total_dist = 0.f;
                int[] joints = new int[]{-1, -1, -1};
                float[] w = new float[]{0, 0, 0};
                float[] d = new float[]{9999, 9999, 9999};

                int k = 0;
                //Find the nearest 3 joints
                //TODO : Perhaps enable more joints - use QuickSort
                for (Frame joint : branch) {
                    if (joint.translation().magnitude() < Float.MIN_VALUE) continue;
                    float dist = getDistance(position, joint, reference);
                    dist = 1 / ((float) Math.pow(dist, 10));
                    if(dist <= d[0]){
                        //swap
                        for(int l = joints.length-1; l > 0; l--){
                            joints[l] = joints[l-1];
                            d[l] = d[l-1];
                        }
                        joints[0] = k;
                        d[0] = dist;
                    }
                    k++;
                }

                for(k = 0; k < joints.length; k++){
                    total_dist += 1.f/d[k];
                }

                for(k = 0; k < joints.length; k++){
                    w[k] += 1.f/d[k] / total_dist;
                }

                System.out.println("joints _ " + joints[0] + " " + joints[1] + " " + joints[2]);

                child.setAttrib("joints", j,joints[0]*1.f, joints[1]*1.f, joints[2]*1.f);
                child.setAttrib("weights", j, w[0], w[1], w[2]);
            }
        }
    }

    /*
     * Get the distance from vertex to line formed by frame and the reference frame of frame
     * Distance will be measure according to root coordinates.
     * In case of reference frame of frame is root, it will return distance from vertex to frame
     * */
    public static float getDistance(Vector vertex, Frame frame, Frame root) {
        if (frame == null) return 9999;
        Vector position = root.location(frame.position());
        Vector parentPosition = root.location(frame.reference().position());
        if (frame.reference() == root) {
            return Vector.distance(position, vertex);
        }
        //is the distance between line formed by b and its parent and v
        Vector line = Vector.subtract(position, parentPosition);
        Vector projection = Vector.subtract(vertex, parentPosition);
        float dot = Vector.dot(projection, line);
        float magnitude = line.magnitude();
        float u = dot * (float) 1. / (magnitude * magnitude);
        Vector distance = new Vector();
        if (u >= 0 && u <= 1) {
            distance = new Vector(parentPosition.x() + u * line.x(), parentPosition.y() + u * line.y(),
                    parentPosition.z() + u * line.z());
            distance = Vector.subtract(distance, vertex);
        }
        if (u < 0) {
            distance = Vector.subtract(position, vertex);
        }
        if (u > 1) {
            distance = Vector.subtract(position, vertex);
        }
        return distance.magnitude();
    }

}