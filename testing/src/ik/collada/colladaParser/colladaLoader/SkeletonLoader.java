package ik.collada.colladaParser.colladaLoader;

import frames.core.Frame;
import frames.primitives.Matrix;
import frames.primitives.Quaternion;
import ik.collada.animation.AnimatedModel;
import ik.collada.colladaParser.xmlParser.XmlNode;
import java.util.List;

public class SkeletonLoader {

    private XmlNode armatureData;
    private List<String> boneOrder;
    private int jointCount = 0;

    public SkeletonLoader(XmlNode visualSceneNode, List<String> boneOrder) {
        this.armatureData = visualSceneNode.getChild("visual_scene").getChildWithAttribute("node", "id", "Armature");
        this.boneOrder = boneOrder;
    }

    public void extractBoneData(AnimatedModel model){
        XmlNode headNode = armatureData.getChild("node");
        model.setRootJoint(loadJointData(headNode, model, null));
        model.setJointCount(jointCount);
    }

    private Frame loadJointData(XmlNode jointNode, AnimatedModel model, Frame reference){
        Frame joint = extractMainJointData(jointNode, model, reference);
        for(XmlNode childNode : jointNode.getChildren("node")){
            loadJointData(childNode, model, joint);
        }
        return joint;
    }

    private Frame extractMainJointData(XmlNode jointNode, AnimatedModel model, Frame reference){
        String nameId = jointNode.getAttribute("id");
        int index = boneOrder.indexOf(nameId);
        String[] matrixRawData = jointNode.getChild("matrix").getData().split(" ");
        float[] matrixData = convertData(matrixRawData);
        Matrix mat = new Matrix(matrixData);
        Frame frame = new Frame();
        frame.setReference(reference);
        frame.setTranslation(matrixData[3], matrixData[7], matrixData[11]);
        frame.setRotation(new Quaternion(mat));
        jointCount++;
        model.getJoints().put(nameId, frame);
        model.getIdxs().put(frame.id(), index);
        return frame;
    }

    private float[] convertData(String[] rawData){
        float[] matrixData = new float[16];
        for(int i=0;i<matrixData.length;i++){
            matrixData[i] = Float.parseFloat(rawData[i]);
        }
        return matrixData;
    }

}