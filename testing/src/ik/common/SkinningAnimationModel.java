package ik.common;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
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

    public Quaternion[] boneQuat = new Quaternion[300];
    public Vector[] bonePos = new Vector[300];

    public float[] bonePositionOrig = new float[300];
    public float[] bonePosition = new float[300];
    float[] boneRotation = new float[300];

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
        Node[] skeleton = model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            if(skeleton[i] == null) continue;
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
        Node[] skeleton = model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            if(skeleton[i] == null) continue;
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
