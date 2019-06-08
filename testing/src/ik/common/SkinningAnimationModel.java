package ik.common;

import nub.core.Node;
import nub.primitives.Quaternion;
import nub.primitives.Vector;
import ik.collada.animation.Model;
import processing.core.PApplet;
import processing.opengl.PShader;

/**
 * Created by sebchaparr on 24/07/18.
 */
public class SkinningAnimationModel {
    protected PShader _shader;
    protected Model _model;

    protected Quaternion[] _initialOrientations;
    public Vector[] _initialPositions;

    public float[] _initialPositionsArray;
    public float[] _positionsArray;
    float[] _orientationsArray;

    String fragmentPath = "/testing/src/ik/common/frag.glsl";
    String vertexPath = "/testing/src/ik/common/skinning.glsl";

    public SkinningAnimationModel(Model model){
        this._model = model;
        PApplet pApplet = model.getScene().pApplet();
        _shader = pApplet.loadShader(pApplet.sketchPath() + fragmentPath,
                pApplet.sketchPath() + vertexPath);

        int joints = _model.getJointTransforms().length;
        _initialOrientations = new Quaternion[joints];
        _initialPositions = new Vector[joints];
        _initialPositionsArray = new float[joints*3];
        _positionsArray = new float[joints*3];
        _orientationsArray = new float[joints*4];
        initParams();
        updateParams();
    }

    public PShader shader(){
        return _shader;
    }

    public void initParams() {
        Node[] skeleton = _model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            if(skeleton[i] == null) continue;
            Vector v = skeleton[i].position();
            Quaternion q = skeleton[i].orientation();
            _initialOrientations[i] = q;
            _initialPositions[i] = v.get();
            _initialPositionsArray[i*3 + 0] =  v.x();
            _initialPositionsArray[i*3 + 1] =  v.y();
            _initialPositionsArray[i*3 + 2] =  v.z();
        }
        _shader.set("bonePositionOrig", _initialPositionsArray);
        _shader.set("boneLength", skeleton.length);
    }

    public void updateParams() {
        //TODO: IT COULD BE DONE WITH LESS OPERATIONS
        Node[] skeleton = _model.getJointTransforms();
        for(int i = 0; i < skeleton.length; i++){
            if(skeleton[i] == null) continue;
            Vector v = Vector.subtract(skeleton[i].position(), _initialPositions[i]);
            Quaternion q = Quaternion.compose(skeleton[i].orientation(), _initialOrientations[i].inverse());
            _positionsArray[i*3 + 0] =  v.x();
            _positionsArray[i*3 + 1] =  v.y();
            _positionsArray[i*3 + 2] =  v.z();
            _orientationsArray[i*4 + 0] =  q.x();
            _orientationsArray[i*4 + 1] =  q.y();
            _orientationsArray[i*4 + 2] =  q.z();
            _orientationsArray[i*4 + 3] =  q.w();
        }
        _shader.set("bonePosition", _positionsArray);
        _shader.set("boneRotation", _orientationsArray);
    }
}
