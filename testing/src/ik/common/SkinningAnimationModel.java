package ik.common;

import frames.core.Frame;
import frames.primitives.Quaternion;
import frames.primitives.Vector;
import ik.collada.animation.AnimatedModel;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PShader;

/**
 * Created by sebchaparr on 24/07/18.
 */
public class SkinningAnimationModel {
    public PShader shader;
    private AnimatedModel model;

    public Quaternion[] boneQuat = new Quaternion[120];
    public Vector[] bonePos = new Vector[120];

    public float[] bonePositionOrig = new float[120];
    public float[] bonePosition = new float[120];
    float[] boneRotation = new float[120];

    String fragmentPath = "/testing/src/ik/common/frag.glsl";
    String vertexPath = "/testing/src/ik/common/skinning.glsl";

    public SkinningAnimationModel(AnimatedModel model){
        this.model = model;
        PApplet pApplet = model.getScene().pApplet();
        shader = pApplet.loadShader(pApplet.sketchPath() + fragmentPath,
                pApplet.sketchPath() + vertexPath);
        initParams();
        updateParams();

    }

    public void initParams() {
        Frame[] skeleton = model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            //TODO : IF there is a reference frame not null
            Vector v = skeleton[i].position();
            Quaternion q = skeleton[i].orientation();
            boneQuat[i] = q;
            bonePos[i] = v.get();
            bonePositionOrig[i*3 + 0] =  v.x();
            bonePositionOrig[i*3 + 1] =  v.y();
            bonePositionOrig[i*3 + 2] =  v.z();
        }
        shader.set("bonePositionOrig", bonePositionOrig);
        shader.set("boneLength", skeleton.length);
    }

    public void updateParams() {
        //TODO: IT COULD BE DONE WITH LESS OPERATIONS
        Frame[] skeleton = model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            Vector v = Vector.subtract(skeleton[i].position(), bonePos[i]);
            Quaternion q = Quaternion.compose(skeleton[i].orientation(), boneQuat[i].inverse());
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
}
